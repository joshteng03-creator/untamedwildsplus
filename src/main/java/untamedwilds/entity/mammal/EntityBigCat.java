package untamedwilds.entity.mammal;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.*;
import untamedwilds.entity.ai.target.*;
import untamedwilds.init.ModEntity;
import untamedwilds.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.List;
import untamedwilds.entity.ai.RetreatWhenRoutedGoal;
import untamedwilds.entity.ai.MeleeAttackCircleHerd;

public class EntityBigCat extends ComplexMobTerrestrial implements ISpecies, INewSkins, INeedsPostUpdate, IPackEntity {

    private static final EntityDataAccessor<Boolean> DIMORPHISM = SynchedEntityData.defineId(EntityBigCat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLUFFY_TAIL = SynchedEntityData.defineId(EntityBigCat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SABRE_FANGS = SynchedEntityData.defineId(EntityBigCat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHORT_SABRES = SynchedEntityData.defineId(EntityBigCat.class, EntityDataSerializers.BOOLEAN);

    public static final Animation ATTACK_MAUL = Animation.create(22);
    public static final Animation ATTACK_POUNCE = Animation.create(42);
    public static Animation ANIMATION_ROAR;
    public static Animation ANIMATION_EAT;
    public static final Animation IDLE_TALK = Animation.create(20);
    public static final Animation IDLE_STRETCH = Animation.create(110);
    public int aggroProgress;
    /* The victim this cat has already spent its ambush on. Deliberately not persisted: the same reasoning
     * as huntCommitTicks, which is also transient -- a reload ends the hunt anyway, and the worst case is
     * that one stalk gets its opening strike back. Cleared in aiStep once the hunt is over, so breaking
     * off and stalking the same animal again re-arms it, which is what an ambush predator actually does. */
    @Nullable
    private LivingEntity ambushedVictim;

    public EntityBigCat(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.entityData.define(DIMORPHISM, false);
        this.entityData.define(FLUFFY_TAIL, false);
        this.entityData.define(SABRE_FANGS, false);
        this.entityData.define(SHORT_SABRES, false);
        this.maxUpStep = 1;
        this.turn_speed = 0.1F;
    }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new SmartSwimGoal_Land(this));
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new FindItemsGoal(this, 12, true));
        /* Pack predators surround their prey instead of queueing up single-file behind it.
         * MeleeAttackCircleHerd has been in the repo unused since the "// TODO: Have Lions use
         * the MeleeAttackCircleHerd goal" note; combined with HuntPackMobTarget, which already
         * gives the whole pack one shared target, this is what lets a pack actually bring down
         * an animal much larger than any individual member. Solitary variants fall through to
         * the goal's direct-pursuit branch, so no pack-size gate is needed here.
         * The HuntPackMobTarget half of that pairing was missing until now: this goal was registered
         * while the target goals below stayed on the SOLITARY HuntMobTarget/SmartHurtByTargetGoal, so a
         * pride of eight acquired eight separate targets, each lion committed alone, and the other seven
         * orbited at six blocks contributing nothing to the fight the first one was losing -- and nothing
         * came when one of them was attacked. Both are now the pack versions, matching EntityHyena. */
        this.goalSelector.addGoal(2, new MeleeAttackCircleHerd(this, 1.4D, false, 1, true));
        this.goalSelector.addGoal(3, new SmartAvoidGoal<>(this, LivingEntity.class, 16, 1.2D, 1.6D, input -> getEcoLevel(input) > getEcoLevel(this)));
        this.goalSelector.addGoal(4, new SmartMateGoal(this, 1D));
        this.goalSelector.addGoal(4, new GotoSleepGoal(this, 1D));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(5, new SmartWanderGoal(this, 1D, true));
        /* Follows the herds. Once prey starts shifting range (HerdMigrationGoal), a pack that
         * stays put simply starves on empty ground -- so this is not a flourish, it is what keeps
         * predator and prey coupled. Runs on the pack leader only; the rest follow it. */
        this.goalSelector.addGoal(5, new PredatorRelocateGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new SmartLookAtGoal(this, LivingEntity.class, 10.0F));
        this.targetSelector.addGoal(1, new HurtPackByTargetGoal(this).setAlertOthers(EntityBigCat.class));
        this.targetSelector.addGoal(2, new AngrySleeperTarget<>(this, LivingEntity.class, true));
        this.targetSelector.addGoal(3, new ProtectChildrenTarget<>(this, LivingEntity.class, true, input -> !(input instanceof EntityBigCat)));
        this.targetSelector.addGoal(4, new HuntPackMobTarget<>(this, LivingEntity.class, true, 30, false, input -> getEcoLevel(input) < getEcoLevel(this)));
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.8D)
                .add(Attributes.MOVEMENT_SPEED, 0.16D)
                .add(Attributes.FOLLOW_RANGE, 32D)
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.ARMOR, 0D);
    }

    /* The hardcoreBreeding overcrowding clause was removed -- see the note in EntityBear. It counted
     * every nearby LivingEntity including pride-mates, so a lion with the new groupCount of 8 could
     * never have bred. */
    /**
     * The classic case: a lion loses its kill to a hyena clan, or to a bear. The cat is the better
     * hunter and the worse owner of what it has caught, which is precisely why the two coexist.
     */
    @Override
    public boolean yieldsCarcassTo(ComplexMob rival) {
        return rival instanceof EntityHyena || rival instanceof EntityBear;
    }

    public boolean wantsToBreed() {
        if (super.wantsToBreed()) {
            // wantsToBreedAsPredator carries the hunger >= 80 check, plus the sustained-condition and
            // territoriality terms that stop a single carcass triggering a birth pulse.
            return !this.isSleeping() && this.getAge() == 0 && EntityUtils.hasHealthFraction(this, 0.6F) && this.wantsToBreedAsPredator();
        }
        return false;
    }

    @Nullable
    public EntityBigCat getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityBigCat(ModEntity.BIG_CAT.get(), this.level));
    }

    public boolean isPushedByFluid() {
        return false;
    }

    protected int calculateFallDamage(float distance, float damageMultiplier) {
        return Mth.ceil((distance * 0.5F - 3.0F) * damageMultiplier);
    }

    public void aiStep() {
        if (!this.level.isClientSide) {
            if (this.herd == null && EntityUtils.getPackSize(this.getType(), this.getVariant()) > 1) {
                IPackEntity.initPack(this);
            }
            else if (EntityUtils.getPackSize(this.getType(), this.getVariant()) > 1) {
                this.herd.tick();
            }
            if ((this.level.getGameTime() + this.getId()) % 1000 == 0) { // staggered by id: every animal on the same tick was a sync-packet burst
                this.drainHuntingHunger(2);
                if (!this.isStarving()) {
                    // A fed predator recovers between hunts; a hungry one does not. Regeneration used to be a flat
                    // trickle, which combined with the old full-health breeding gate to lock predators out of breeding.
                    this.heal(this.getHunger() >= 120 ? 6.0F : 2.0F);
                }
            }
            // The stalk is over, so the next one gets a fresh opening strike.
            if (this.ambushedVictim != null && !this.isHunting()) {
                this.ambushedVictim = null;
            }

            // Random idle animations
            if (this.getAnimation() == NO_ANIMATION && this.getTarget() == null && !this.isSleeping()) {
                if (this.getCommandInt() == 0) {
                    int i = this.random.nextInt(3000);
                    if (i == 0 && !this.isInWater() && this.isNotMoving() && this.canMove() && this.isActive()) {
                        this.getNavigation().stop();
                        this.setSitting(true);
                    }
                    if ((i == 1 || this.isInWater()) && this.isSitting() && this.getCommandInt() < 2) {
                        this.setSitting(false);
                    }
                    if (i == 2 && this.canMove() && !this.isInWater() && !this.isBaby()) {
                        this.getNavigation().stop();
                        this.setAnimation(IDLE_STRETCH);
                    }
                    if (i > 2980 && !this.isInWater() && !this.isBaby()) {
                        this.setAnimation(IDLE_TALK);
                    }
                }
            }
            if (this.tickCount % 80 == 2 && this.getTarget() != null && this.getAnimation() == NO_ANIMATION) {
                this.setAnimation(ANIMATION_ROAR);
            }
            if (this.getAnimation() == ATTACK_POUNCE && this.getAnimationTick() == 10) {
                this.getMoveControl().strafe(2F, 0);
                this.getJumpControl().jump();
            }
            this.setAngry(this.getTarget() != null);
        }
        if (this.getAnimation() == ANIMATION_EAT && (this.getAnimationTick() == 10 || this.getAnimationTick() == 20 || this.getAnimationTick() == 30)) {
            this.playSound(SoundEvents.HORSE_EAT,1.5F, 0.8F);
        }
        if (this.getAnimation() == ATTACK_MAUL && this.getAnimationTick() == 10) {
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP,1.5F, 0.8F);
        }
        if (this.getAnimation() == IDLE_TALK && this.getAnimationTick() == 1 && this.getAmbientSound() != null) {
            this.playSound(this.getAmbientSound(), this.getSoundVolume(), this.getVoicePitch());
        }
        if (this.level.isClientSide && this.isAngry() && this.aggroProgress < 40) {
            this.aggroProgress++;
        } else if (this.level.isClientSide && !this.isAngry() && this.aggroProgress > 0) {
            this.aggroProgress--;
        }
        super.aiStep();
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.WOLF_STEP, 0.15F, 1.0F);
    }

    @Override
    protected void reassessTameGoals() {
        if (this.isTame()) {
            if (UntamedWilds.DEBUG) {
                UntamedWilds.LOGGER.info("Updating AI tasks for tamed mob");
            }
            this.goalSelector.addGoal(3, new SmartFollowOwnerGoal(this, 2.3D, 12.0F, 3.0F));
            this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new SmartOwnerHurtTargetGoal(this));
        }
    }

    protected SoundEvent getAmbientSound() {
        return this.isBaby() ? SoundEvents.OCELOT_AMBIENT : super.getAmbientSound();
    }

    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return this.isBaby() ?  SoundEvents.OCELOT_HURT : super.getHurtSound(source);
    }

    protected SoundEvent getDeathSound() {
        return this.isBaby() ? SoundEvents.OCELOT_DEATH : super.getDeathSound();
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (hand == InteractionHand.MAIN_HAND && !this.level.isClientSide()) {

            if (!this.isTame() && this.isBaby() && EntityUtils.hasFullHealth(this) && this.isFood(itemstack)) {
                this.playSound(SoundEvents.HORSE_EAT, 1.5F, 0.8F);
                if (this.getRandom().nextInt(3) == 0) {
                    this.tame(player);
                    EntityUtils.spawnParticlesOnEntity(this.level, this, ParticleTypes.HEART, 3, 6);
                } else {
                    EntityUtils.spawnParticlesOnEntity(this.level, this, ParticleTypes.SMOKE, 3, 3);
                }
            }
        }

        return super.mobInteract(player, hand);
    }

    /**
     * True when this blow is the opening strike of a stalk rather than a swing in an ongoing fight.
     * <p>
     * All four clauses matter. The commitment restricts it to hunts for food, so a cat gets no burst in a
     * scrap over cubs. The victim check spends it once per hunt rather than once per bite -- and it cannot
     * be replaced by {@code getLastHurtByMob}, because in a pride that field is whichever lion bit last,
     * so every member would read every blow as its own first. The last two are the ambush itself: quarry
     * that has already been bitten by this cat, or that is already coming for it, has not been ambushed.
     */
    private boolean isAmbushStrike(Entity target) {
        if (!(target instanceof LivingEntity victim) || this.isBaby()) {
            return false;
        }
        return this.isCommittedTo(victim)
                && this.ambushedVictim != victim
                && victim.getLastHurtByMob() != this
                && !(victim instanceof Mob mob && mob.getTarget() == this);
    }

    public boolean doHurtTarget(Entity entityIn) {
        boolean ambush = this.isAmbushStrike(entityIn);
        /* Summed, not multiplied: both are amounts on one MULTIPLY_TOTAL modifier, so a committed lion
         * landing an ambush is x(1 + 1.00 + 0.75). Sabre-toothed species drive their canines into the
         * throat of an animal they have already pinned, which is worth something only on that first blow. */
        double bonus = this.getHuntDamageBonus(entityIn);
        if (ambush) {
            bonus += ConfigGamerules.bigCatAmbushMultiplier.get() - 1.0D;
            if (this.hasSabreFangs() || this.hasShortSabres()) {
                bonus += 0.5D;
            }
        }
        this.applyAttackBonus(bonus);
        boolean flag;
        try {
            flag = super.doHurtTarget(entityIn);
        } finally {
            this.clearAttackBonus();
        }
        if (flag) {
            // Spent only on a landed blow, so a strike the quarry shrugs off does not waste the stalk.
            if (ambush && entityIn instanceof LivingEntity victim) {
                this.ambushedVictim = victim;
                this.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.2F, 0.7F);
                EntityUtils.spawnParticlesOnEntity(this.level, victim, ParticleTypes.CRIT, 4, 6);
            }
            this.satiateFromKill(entityIn);
            if (this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
                Animation anim = chooseAttackAnimation(entityIn);
                this.setAnimation(anim);
                this.setAnimationTick(0);
            }
        }
        return flag;
    }

    /**
     * A cornered cat is famously hard to finish. The mirror of the bear's Bearserk: a wounded bear becomes
     * more dangerous, a wounded cat becomes harder to kill.
     * <p>
     * Damage reduction rather than a Strength-style buff on purpose. {@code MobEffects.DAMAGE_BOOST} is
     * attribute-backed, so it raises ATTACK_DAMAGE and with it this animal's eco level mid-fight, changing
     * what it hunts and what runs from it. (That is a real quirk of Bearserk today, and is left alone.)
     */
    @Override
    protected float getIncomingDamageFactor(DamageSource source) {
        return this.getHealth() < this.getMaxHealth() / 2
                ? 1F - ConfigGamerules.bigCatCorneredReduction.get().floatValue() : 1F;
    }

    public boolean hurt(DamageSource damageSource, float amount) {
        // Retaliate I: Mob will strike back when attacked by its current target
        float f = this.getHealth();
        if (!this.level.isClientSide && !this.isNoAi() && this.getTarget() == damageSource.getEntity() && amount < f && (damageSource.getEntity() != null || damageSource.getDirectEntity() != null) && damageSource.getEntity() instanceof LivingEntity && (damageSource.getEntity() instanceof TamableAnimal tamable && tamable.getOwner() != null)) {
            if (this.hasLineOfSight(damageSource.getEntity()) && !damageSource.getEntity().isInvulnerable() && this.getAnimation() == NO_ANIMATION) {
                this.doHurtTarget(damageSource.getEntity());
            }
        }
        return super.hurt(damageSource, amount);
    }

    private Animation chooseAttackAnimation(Entity target) {
        if (target.getBbHeight() < this.getBbHeight()) {
            return ATTACK_MAUL;
        }
        return ATTACK_POUNCE;
    }

    //public Animation getAnimationEat() { return ANIMATION_EAT; }
    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_POUNCE, ATTACK_MAUL, IDLE_TALK, IDLE_STRETCH};
    }

    protected float getStandingEyeHeight(Pose poseIn, EntityDimensions sizeIn) {
        return sizeIn.height * 0.9F;
    }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(getEntityData(this.getType()).getSpeciesData().get(this.getVariant()).getAttack());
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(getEntityData(this.getType()).getSpeciesData().get(this.getVariant()).getHealth());
        this.setHealth(this.getMaxHealth());
        this.setDimorphism(getEntityData(this.getType()).getFlags(this.getVariant(), "dimorphism") == 1);
        this.setFluffyTail(getEntityData(this.getType()).getFlags(this.getVariant(), "fluffyTail") == 1);
        this.setSabreFangs(getEntityData(this.getType()).getFlags(this.getVariant(), "hasSabreFangs") == 1);
        this.setShortSabres(getEntityData(this.getType()).getFlags(this.getVariant(), "shortSabres") == 1);
    }

    public boolean hasDimorphism(){ return (this.entityData.get(DIMORPHISM)); }
    private void setDimorphism(boolean dimorphism){ this.entityData.set(DIMORPHISM, dimorphism); }

    public boolean hasFluffyTail(){ return (this.entityData.get(FLUFFY_TAIL)); }
    private void setFluffyTail(boolean fluffy_tail){ this.entityData.set(FLUFFY_TAIL, fluffy_tail); }

    public boolean hasSabreFangs(){ return (this.entityData.get(SABRE_FANGS)); }
    private void setSabreFangs(boolean sabre_fangs){ this.entityData.set(SABRE_FANGS, sabre_fangs); }

    public boolean hasShortSabres(){ return (this.entityData.get(SHORT_SABRES)); }
    private void setShortSabres(boolean short_sabres){ this.entityData.set(SHORT_SABRES, short_sabres); }

    public void addAdditionalSaveData(CompoundTag compound){
        super.addAdditionalSaveData(compound);
        compound.putBoolean("hasDimorphism", this.hasDimorphism());
        compound.putBoolean("fluffy", this.hasFluffyTail());
        compound.putBoolean("hasSabreFangs", this.hasSabreFangs());
        compound.putBoolean("shortSabres", this.hasShortSabres());
    }

    public void readAdditionalSaveData(CompoundTag compound){
        super.readAdditionalSaveData(compound);
        this.setDimorphism(compound.getBoolean("hasDimorphism"));
        this.setFluffyTail(compound.getBoolean("fluffy"));
        this.setSabreFangs(compound.getBoolean("hasSabreFangs"));
        this.setShortSabres(compound.getBoolean("shortSabres"));
    }

    public ResourceLocation getTexture() {
        ResourceLocation texture_path = EntityUtils.getSkinFromEntity(this);
        if (this.hasDimorphism()) {
            String trimmed_path = texture_path.getPath().substring(0, texture_path.getPath().lastIndexOf('.'));
            return new ResourceLocation(UntamedWilds.MOD_ID, trimmed_path + "_" + this.getGenderString() + ".png");
        }
        return texture_path;
    }
}
