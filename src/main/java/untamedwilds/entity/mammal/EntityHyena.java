package untamedwilds.entity.mammal;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.*;
import untamedwilds.entity.ai.target.*;
import untamedwilds.init.ModEntity;
import untamedwilds.init.ModSounds;
import untamedwilds.util.EntityUtils;

import javax.annotation.Nullable;
import untamedwilds.entity.ai.RetreatWhenRoutedGoal;
import untamedwilds.entity.ai.MeleeAttackCircleHerd;

public class EntityHyena extends ComplexMobTerrestrial implements INewSkins, ISpecies, IPackEntity, INeedsPostUpdate {

    public static final Animation ATTACK_POUNCE = Animation.create(42);
    public static final Animation IDLE_TALK = Animation.create(20);
    public static final Animation ATTACK_BITE = Animation.create(15);

    public EntityHyena(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.maxUpStep = 1F;
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
         * the goal's direct-pursuit branch, so no pack-size gate is needed here. */
        this.goalSelector.addGoal(2, new MeleeAttackCircleHerd(this, 1.6D, false, 1, false));
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
        this.targetSelector.addGoal(1, new HurtPackByTargetGoal(this).setAlertOthers(EntityHyena.class));
        this.targetSelector.addGoal(2, new ProtectChildrenTarget<>(this, LivingEntity.class, true, input -> !(input instanceof EntityHyena)));
        this.targetSelector.addGoal(3, new HuntPackMobTarget<>(this, LivingEntity.class, true, 30, false, input -> getEcoLevelAsPrey(input) < getEcoLevel(this)));
        this.targetSelector.addGoal(4, new AngrySleeperTarget<>(this, LivingEntity.class, true));
    }

    @Override
    protected void reassessTameGoals() {
        if (this.isTame()) {
            if (UntamedWilds.DEBUG) {
                UntamedWilds.LOGGER.info("Updating AI tasks for tamed mob");
            }
            this.goalSelector.addGoal(3, new SmartFollowOwnerGoal(this, 1.3D, 12.0F, 3.0F));
            this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new SmartOwnerHurtTargetGoal(this));
        }
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.4D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    public boolean wantsToBreed() {
        // Same rule as every land predator; see ComplexMob.wantsToBreedAsLandPredator.
        return this.wantsToBreedAsLandPredator();
    }

    @Override
    public void aiStep() {
        if (!this.level.isClientSide) {
            if (this.herd == null) {
                IPackEntity.initPack(this);
            }
            else {
                this.herd.tick();
            }
            if ((this.level.getGameTime() + this.getId()) % 1000 == 0) { // staggered by id: every animal on the same tick was a sync-packet burst
                this.drainHuntingHunger(4);
                if (!this.isStarving()) {
                    // See EntityBear: canids healed 1 HP per 1000 ticks, slower than anything they hunt.
                    this.heal(this.getHunger() >= 120 ? 5.0F : 2.0F);
                }
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
                    if (i > 2980 && !this.isInWater() && !this.isBaby()) {
                        this.setAnimation(IDLE_TALK);
                    }
                }
            }
            this.setAngry(this.getTarget() != null);
            if (this.getAnimation() == ATTACK_POUNCE && this.getAnimationTick() == 10) {
                this.getMoveControl().strafe(2F, 0);
                this.getJumpControl().jump();
            }
            if (this.getAnimation() == IDLE_TALK && this.getAnimationTick() == 1 && this.getAmbientSound() != null) {
                this.playSound(this.getAmbientSound(), this.getSoundVolume(), this.getVoicePitch());
            }
            if (this.getTarget() != null && this.tickCount % 120 == 0) {
                this.playSound(this.getThreatSound(), this.getSoundVolume(), this.getVoicePitch());
            }
        }
        if (this.getAnimation() != NO_ANIMATION) {
            if (this.getAnimation() == ATTACK_BITE && this.getAnimationTick() == 6) {
                this.playSound(ModSounds.ENTITY_ATTACK_BITE, 1.5F, 0.8F);
            }
        }
        super.aiStep();
    }

    /** Ceiling on the pack bonus, so a clan of twenty cannot simply multiply itself into a boss fight. */
    private static final double PACK_BONUS_CAP = 0.5D;

    /**
     * Extra damage for each clan-mate working the same animal.
     * <p>
     * The hyena signature. A spotted hyena is attack 5, the weakest large carnivore in the mod, and is
     * meant to be nothing on its own and inevitable in a clan -- which is the entire reason it ships with
     * groupCount 20. Scaling damage by the mob rather than raising the stat line keeps that shape, and
     * keeps its eco level untouched.
     * <p>
     * Self-limiting by construction: it counts only clan-mates that hold the same target AND are within 8
     * blocks of it, and MeleeAttackCircleHerd orbits most of the clan at six blocks most of the time, so
     * the cap is reached only in the moments the clan has actually piled on.
     */
    private double packBonus(Entity target) {
        if (this.herd == null || target == null) {
            return 0D;
        }
        double step = ConfigGamerules.hyenaPackDamageStep.get();
        if (step <= 0) {
            return 0D;
        }
        int engaged = 0;
        for (ComplexMob member : this.herd.creatureList) {
            if (member != this && member.isAlive() && !member.isBaby()
                    && member.getTarget() == target && member.distanceToSqr(target) < 64D) {
                engaged++;
            }
        }
        return Math.min(engaged * step, PACK_BONUS_CAP);
    }

    public boolean doHurtTarget(Entity entityIn) {
        // Summed, not multiplied -- both are amounts on the same MULTIPLY_TOTAL modifier.
        this.applyAttackBonus(this.getHuntDamageBonus(entityIn) + this.packBonus(entityIn));
        boolean flag;
        try {
            flag = super.doHurtTarget(entityIn);
        } finally {
            this.clearAttackBonus();
        }
        if (flag) {
            this.satiateFromKill(entityIn);
            if (this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
                Animation anim = chooseAttackAnimation();
                this.setAnimation(anim);
            }
        }
        return flag;
    }

    /**
     * Kleptoparasitism: a hyena eats from any carcass nearby, including -- especially -- one a lion or a
     * wolf pack brought down. The feeding itself is driven from the killer's side in
     * {@code ComplexMob.shareCarcassWithScavengers}; see the note there for why it cannot live here.
     */
    @Override
    public boolean canScavengeCarcass() {
        return true;
    }

    /**
     * A clan large enough drives the animal that made the kill off it outright, rather than waiting for
     * scraps. This is the difference between scavenging and kleptoparasitism proper, and it is the whole
     * of the hyena's ecological character: a clan that eats other predators' kills passes the hunger gate
     * less often, so it hunts less often, so it is exposed less often to the one thing that kills a
     * predator for hunting.
     */
    @Override
    public int displacementGroupSize() {
        return ConfigGamerules.kleptoparasitismClanSize.get();
    }

    /** Hyenas concede to a bear, and to nothing else. */
    @Override
    public boolean yieldsCarcassTo(ComplexMob rival) {
        return rival instanceof EntityBear;
    }

    /**
     * See {@code EntityDireWolf.getIncomingDamageFactor}. A spotted hyena is 20 HP against a wooly rhino's
     * 10 attack, so it died in two bites; reduction rather than health keeps its eco level, and therefore
     * the whole clan's, exactly where it is.
     */
    @Override
    protected float getIncomingDamageFactor(DamageSource source) {
        return appliesPackHunterReduction(source) ? 1F - ConfigGamerules.packHunterDamageReduction.get().floatValue() : 1F;
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.WOLF_STEP, 0.15F, 1.0F);
    }

    private Animation chooseAttackAnimation() {
        return switch (this.random.nextInt(4)) {
            case 0 -> ATTACK_POUNCE;
            case 1 -> ATTACK_POUNCE;
            default -> ATTACK_BITE;
        };
    }

    @Nullable
    public EntityHyena getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityHyena(ModEntity.HYENA.get(), this.level));
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

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_POUNCE, ATTACK_BITE, IDLE_TALK};
    }

    public Animation getAnimationEat() { return NO_ANIMATION; }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(getEntityData(this.getType()).getSpeciesData().get(this.getVariant()).getAttack());
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(getEntityData(this.getType()).getSpeciesData().get(this.getVariant()).getHealth());
        this.setHealth(this.getMaxHealth());
    }
}
