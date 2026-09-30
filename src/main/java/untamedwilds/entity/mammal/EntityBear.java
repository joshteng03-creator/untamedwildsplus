package untamedwilds.entity.mammal;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.*;
import untamedwilds.entity.ai.target.*;
import untamedwilds.init.ModEntity;
import untamedwilds.init.ModSounds;
import untamedwilds.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.List;
import untamedwilds.entity.ai.RetreatWhenRoutedGoal;

public class EntityBear extends ComplexMobTerrestrial implements ISpecies, INewSkins, INeedsPostUpdate {

    private static final EntityDataAccessor<Boolean> SHORT_SNOUT = SynchedEntityData.defineId(EntityBear.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BACK_HUMP = SynchedEntityData.defineId(EntityBear.class, EntityDataSerializers.BOOLEAN);

    public static final Animation ATTACK_BITE = Animation.create(18);
    public static final Animation ATTACK_MAUL = Animation.create(76);
    public static final Animation ATTACK_SWIPE = Animation.create(26);
    public static final Animation ATTACK_POUND = Animation.create(28);
    public static final Animation ANIMATION_ROAR = Animation.create(50);
    public static final Animation IDLE_STAND = Animation.create(148);
    public static final Animation IDLE_TALK = Animation.create(20);
    public static final Animation ANIMATION_EAT = Animation.create(104);

    public EntityBear(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.entityData.define(SHORT_SNOUT, false);
        this.entityData.define(BACK_HUMP, false);
        this.maxUpStep = 1;
        this.turn_speed = 0.3F;
    }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new SmartSwimGoal_Land(this));
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new FindItemsGoal(this, 12));
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.6D, false, 1));
        this.goalSelector.addGoal(3, new SmartAvoidGoal<>(this, LivingEntity.class, 16, 1.2D, 1.6D, input -> getEcoLevel(input) > getEcoLevel(this)));
        this.goalSelector.addGoal(4, new SmartMateGoal(this, 1D));
        this.goalSelector.addGoal(4, new GotoSleepGoal(this, 1D));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25D));
        //this.goalSelector.addGoal(5, new BearRaidChestsGoal(this, 120));
        this.goalSelector.addGoal(6, new SmartWanderGoal(this, 1D, true));
        /* Follows the herds. Once prey starts shifting range (HerdMigrationGoal), a pack that
         * stays put simply starves on empty ground -- so this is not a flourish, it is what keeps
         * predator and prey coupled. Runs on the pack leader only; the rest follow it. */
        this.goalSelector.addGoal(5, new PredatorRelocateGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new SmartLookAtGoal(this, LivingEntity.class, 10.0F));
        this.targetSelector.addGoal(1, new SmartHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new ProtectChildrenTarget<>(this, LivingEntity.class, true, input -> !(input instanceof EntityBear)));
        this.targetSelector.addGoal(3, new HuntMobTarget<>(this, LivingEntity.class, true, 30, false, input -> getEcoLevelAsPrey(input) < getEcoLevel(this)));
        this.targetSelector.addGoal(4, new AngrySleeperTarget<>(this, LivingEntity.class, true));
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.FOLLOW_RANGE, 24D)
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1D)
                .add(Attributes.ARMOR, 4D);
    }

    /* The hardcoreBreeding overcrowding clause was removed. It counted EVERY LivingEntity within 6
     * blocks -- the animal's own mate, its own cubs, and any passing chicken -- and refused to breed
     * at 3 or more. With herd/pack sizes now raised to realistic numbers a social animal is
     * permanently over that limit and could never breed again, which is the opposite of the intent.
     * Population pressure is handled by gestation and maturity timers instead (see
     * ComplexMob.getPregnancyTime / getAdulthoodTime). */
    /**
     * A bear takes a carcass off anything, on its own. Brown bears displace wolf packs from kills as a
     * matter of routine, and it is a large part of how an omnivore that hunts badly eats well.
     */
    @Override
    public int displacementGroupSize() {
        return ConfigGamerules.kleptoparasitismClanSize.get() > 0 ? 1 : 0;
    }

    @Override
    public boolean canScavengeCarcass() {
        return true;
    }

    public boolean wantsToBreed() {
        // Same rule as every land predator; see ComplexMob.wantsToBreedAsLandPredator.
        return this.wantsToBreedAsLandPredator();
    }

    @Nullable
    public EntityBear getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityBear(ModEntity.BEAR.get(), this.level));
    }

    public boolean isPushedByFluid() {
        return false;
    }

    public void aiStep() {
        if (!this.level.isClientSide) {
            if (this.tickCount % 1000 == 0) {
                this.drainHuntingHunger(2);
                if (!this.isStarving()) {
                    // A fed predator recovers between hunts; a hungry one does not. Regeneration used to be a flat
                    // trickle, which combined with the old full-health breeding gate to lock predators out of breeding.
                    this.heal(this.getHunger() >= 120 ? 6.0F : 2.0F);
                }
            }
            // Bearserk
            if (this.getHealth() < this.getMaxHealth() / 2) {
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0, true, true));
                this.forceSleep = -1200;
            }

            this.setAngry(this.getTarget() != null);
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
                        if (!this.isSitting()) {
                            this.setAnimation(IDLE_STAND);
                        }
                    }
                    if (i > 2980 && !this.isInWater() && !this.isBaby()) {
                        this.setAnimation(IDLE_TALK);
                    }
                }
            }
        }
        if (this.tickCount % 100 == 0 && this.getTarget() != null && this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_ROAR);
        }
        if (this.getAnimation() != NO_ANIMATION) {
            if (this.getAnimation() == ATTACK_BITE && this.getAnimationTick() == 1) {
                this.playSound(ModSounds.ENTITY_ATTACK_BITE, 1.5F, 0.8F);
            }
            if (this.getAnimation() == ATTACK_SWIPE && this.getAnimationTick() == 8) {
                this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.8F);
            }
            if (this.getAnimation() == ATTACK_POUND && this.getAnimationTick() == 10) {
                this.getMoveControl().strafe(2F, 0);
                this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.8F);
            }
            if (this.getAnimation() == ATTACK_MAUL) {
                if (this.getAnimationTick() == 10) {
                    this.playSound(this.getThreatSound(), this.getSoundVolume(), this.getVoicePitch());
                }
                if (this.getAnimationTick() == 20) {
                    this.playSound(SoundEvents.PLAYER_SMALL_FALL, 1.5F, 0.8F);
                    if (!this.level.isClientSide()) {
                        BlockPos pos = this.blockPosition().below();
                        BlockState state = this.level.getBlockState(pos);
                        if (!state.addLandingEffects((ServerLevel) this.level, pos, state, this, 40))
                            ((ServerLevel)this.level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), this.getX(), this.getY(), this.getZ(), 40, 0.0D, 0.0D, 0.0D, 99.3F);
                    }
                }
            }
            if (this.getAnimation() == ANIMATION_ROAR && this.getAnimationTick() == 7) {
                this.playSound(this.getThreatSound(), this.getSoundVolume(), this.getVoicePitch());
            }
            if (this.getAnimation() == IDLE_TALK && this.getAnimationTick() == 1) {
                this.playSound(ModSounds.ENTITY_BEAR_AMBIENT, this.getSoundVolume(), this.getVoicePitch());
            }
            if (this.getAnimation() == ANIMATION_EAT && (this.getAnimationTick() == 10 || this.getAnimationTick() == 20 || this.getAnimationTick() == 30)) {
                this.playSound(SoundEvents.HORSE_EAT, 1.5F, 0.8F);
            }
        }
        super.aiStep();
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

    public double getMountedYOffset() { return getModelScale() + 0.5f * this.getMobSize(); }

    protected SoundEvent getAmbientSound() {
        return !this.isBaby() ? super.getAmbientSound() : ModSounds.ENTITY_BEAR_BABY_AMBIENT;
    }

    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return !this.isBaby() ? super.getHurtSound(source) : ModSounds.ENTITY_BEAR_BABY_CRY;
    }

    protected SoundEvent getDeathSound() { return super.getDeathSound(); }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.POLAR_BEAR_STEP, 0.15F, 1.0F);
    }

    public boolean doHurtTarget(Entity entityIn) {
        this.applyAttackBonus(this.getHuntDamageBonus(entityIn));
        boolean flag;
        try {
            flag = super.doHurtTarget(entityIn);
        } finally {
            this.clearAttackBonus();
        }
        if (flag) {
            /* Stagger. Bears are the slowest predators in the mod -- 0.15 to 0.24 -- so everything they
             * pick a fight with simply walks away from them, and Bearserk only helps in the fights they
             * manage to hold. The answer is not to make them faster, which would make them something other
             * than a bear; it is to make them impossible to disengage from, alongside the ATTACK_KNOCKBACK
             * of 1.0 they already carry. Deliberately NOT gated on which attack animation came up:
             * chooseAttackAnimation() is rolled below, after the hit, and only when the bear is not already
             * mid-animation, so gating on it would drop the stagger from most blows at random. */
            if (entityIn instanceof LivingEntity victim && !this.isBaby()) {
                int stagger = ConfigGamerules.bearStaggerTicks.get();
                if (stagger > 0) {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stagger, 0, true, true));
                }
            }
            this.satiateFromKill(entityIn);
            if (this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
                Animation anim = chooseAttackAnimation();
                this.setAnimation(anim);
                this.setAnimationTick(0);
            }
        }
        return flag;
    }

    public boolean hurt(DamageSource damageSource, float amount) {
        // Retaliate II: Mob will strike back when attacked by any mob
        performRetaliation(damageSource, this.getHealth(), amount, false);
        return super.hurt(damageSource, amount);
    }

    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source) || source == DamageSource.SWEET_BERRY_BUSH;
    }

    private Animation chooseAttackAnimation() {
        return switch (this.random.nextInt(4)) {
            case 0 -> ATTACK_SWIPE;
            case 1 -> ATTACK_BITE;
            case 2 -> ATTACK_MAUL;
            default -> ATTACK_POUND;
        };
    }

    protected float getStandingEyeHeight(Pose poseIn, EntityDimensions sizeIn) {
        return sizeIn.height * 0.85F;
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

    public Animation getAnimationEat() { return ANIMATION_EAT; }
    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ANIMATION_ROAR, IDLE_STAND, IDLE_TALK, ANIMATION_EAT, ATTACK_MAUL, ATTACK_BITE, ATTACK_SWIPE, ATTACK_POUND};
    }

    // Flags Parameters
    public boolean isPanda() {
        return getEntityData(this.getType()).getFlags(this.getVariant(), "isPanda") == 1;
    }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(getEntityData(this.getType()).getSpeciesData().get(this.getVariant()).getAttack());
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(getEntityData(this.getType()).getSpeciesData().get(this.getVariant()).getHealth());
        this.setHealth(this.getMaxHealth());
        this.setShortSnout(getEntityData(this.getType()).getFlags(this.getVariant(), "hasShortSnout") == 1);
        this.setHump(getEntityData(this.getType()).getFlags(this.getVariant(), "hasHump") == 1);
    }

    public boolean hasShortSnout(){ return (this.entityData.get(SHORT_SNOUT)); }
    private void setShortSnout(boolean short_snout){ this.entityData.set(SHORT_SNOUT, short_snout); }
    public boolean hasHump(){ return (this.entityData.get(BACK_HUMP)); }
    private void setHump(boolean hump){ this.entityData.set(BACK_HUMP, hump); }

    // TODO: Very shitty hardcoded solution for Blind Cave Bears to keep spawning
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
        if (this.getY() < 53) {
            this.setVariant(1);
        }
        return data;
    }

    public void addAdditionalSaveData(CompoundTag compound){
        super.addAdditionalSaveData(compound);
        compound.putBoolean("hasShortSnout", this.hasShortSnout());
        compound.putBoolean("hasHump", this.hasHump());
    }

    public void readAdditionalSaveData(CompoundTag compound){
        super.readAdditionalSaveData(compound);
        this.setShortSnout(compound.getBoolean("hasShortSnout"));
        this.setHump(compound.getBoolean("hasHump"));
    }
}
