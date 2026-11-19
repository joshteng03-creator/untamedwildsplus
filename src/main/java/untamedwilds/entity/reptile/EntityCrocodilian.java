package untamedwilds.entity.reptile;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.config.EcologyMode;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.*;
import untamedwilds.entity.ai.target.HuntMobTarget;
import untamedwilds.entity.ai.target.SmartHurtByTargetGoal;
import untamedwilds.init.ModBlock;
import untamedwilds.init.ModEntity;
import untamedwilds.init.ModTags;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Crocodilians -- Nile crocodile, saltwater crocodile, mugger, black caiman, Quinkana, Voay (Phase 4.8, 2026-11-14).
 * The river ambush predator of every continent: amphibious, on the hippo / pinniped ComplexMobAmphibious base.
 *
 *  * RIVER AMBUSH: a hungry crocodile waits in the water and only takes prey that is IN or BESIDE the water -- an animal
 *    drinking at the bank, a herd crossing a river (the wildebeest treks), a capybara diving -- then LUNGES (a burst out of
 *    the water). It hunts through the standard HuntMobTarget machinery, so the hunt commitment, damage burst, kill
 *    cooldown, finite carcass and the prey-per-predator breeding gate all apply. Quinkana, a Pleistocene LAND crocodile,
 *    hunts on land like the giant monitor.
 *  * DEATH ROLL: a bite on prey in the water may become ATTACK_ROLL -- the crocodile spins, holding the victim under,
 *    for extra damage and a heavy slow.
 *  * BASKING: by day it hauls out to bask, lying with its mouth open (thermoregulation), and slides back into the water
 *    when something comes close or when it gets hungry.
 *  * NESTS: females lay their eggs in the mod's reptile nest (LayEggsOnNestGoal); the nest hatches hatchlings, which
 *    then grow up and are counted in the census. A mother does not lay again until her rearing interval has passed.
 *  * Ectotherm: hunger drains at half a mammal predator's rate, so a crocodile eats seldom.
 */
public class EntityCrocodilian extends ComplexMobAmphibious implements INewSkins, ISpecies, INeedsPostUpdate, INestingMob {

    public static final Animation ATTACK_BITE = Animation.create(12);
    public static final Animation ATTACK_ROLL = Animation.create(40);

    private static final EntityDataAccessor<Boolean> HAS_EGG = SynchedEntityData.defineId(EntityCrocodilian.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BASKING = SynchedEntityData.defineId(EntityCrocodilian.class, EntityDataSerializers.BOOLEAN);

    private static final int QUINKANA = 4;

    private int lungeCooldown;
    @Nullable
    private LivingEntity rollVictim;

    public EntityCrocodilian(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.maxUpStep = 1F;
        this.turn_speed = 0.12F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HAS_EGG, false);
        this.entityData.define(BASKING, false);
    }

    public boolean isBasking() { return this.entityData.get(BASKING); }

    private boolean isLandHunter() { return this.getVariant() == QUINKANA; }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.3D));
        this.goalSelector.addGoal(2, new LungeGoal(this));
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.5D, false, 1));
        this.goalSelector.addGoal(3, new SmartMateGoal(this, 1D));
        this.goalSelector.addGoal(3, new LayEggsOnNestGoal(this));
        this.goalSelector.addGoal(4, new AmphibiousTransition(this, 1D));
        this.goalSelector.addGoal(4, new GotoSleepGoal(this, 1D));
        this.goalSelector.addGoal(5, new AmphibiousRandomSwimGoal(this, 0.8D, 160));
        this.goalSelector.addGoal(5, new SmartWanderGoal(this, 1D, 160, 0, false));
        this.goalSelector.addGoal(6, new SmartLookAtGoal(this, LivingEntity.class, 10.0F));
        this.targetSelector.addGoal(1, new SmartHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new HuntMobTarget<>(this, LivingEntity.class, true, 30, false,
                input -> getEcoLevelAsPrey(input) < getEcoLevel(this) && this.canAmbush(input)));
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.3D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.ARMOR, 6D);
    }

    // ------------------------------------------------------------------------------------------------ ambush

    /** Prey a waiting crocodile can take: in the water or at its edge, and only while the crocodile is itself in the water.
     *  Quinkana hunts on land. */
    boolean canAmbush(LivingEntity prey) {
        if (this.isLandHunter()) {
            return true;
        }
        return this.isInWater() && (prey.isInWater() || isBesideWater(prey.blockPosition()));
    }

    private boolean isBesideWater(BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 0, 2))) {
            if (this.level.getFluidState(p).is(Fluids.WATER)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHungry() {
        return this.getHunger() <= EcologyMode.huntHungerThreshold(this, 30);
    }

    /* By day it basks on the bank; at night, when hungry, when hurt or when nesting is not due it waits in the water.
     * Quinkana keeps to the land. */
    @Override
    public boolean wantsToBeOnLand() {
        if (this.isLandHunter() || this.wantsToLayEggs()) {
            return true;
        }
        boolean threatened = this.getLastHurtByMob() != null && this.tickCount - this.getLastHurtByMobTimestamp() < 300;
        if (threatened || this.isHungry()) {
            return false;
        }
        long t = this.level.getDayTime() % 24000L;
        return t > 1000 && t < 9000;
    }

    @Override
    public boolean wantsToBeInWater() {
        return !this.wantsToBeOnLand();
    }

    public boolean wantsToBreed() {
        return this.wantsToBreedAsLandPredator();
    }

    @Override
    public void aiStep() {
        if (!this.level.isClientSide) {
            if ((this.tickCount + this.getId()) % 1000 == 0) {
                this.drainHuntingHunger(1);           // ectotherm: half a mammal predator's drain
                if (!this.isStarving()) {
                    this.heal(this.getHunger() >= 120 ? 4.0F : 1.0F);
                }
            }
            if (this.lungeCooldown > 0) {
                this.lungeCooldown--;
            }
            this.tickBasking();
            this.tickDeathRoll();
            this.setAngry(this.getTarget() != null);
        }
        super.aiStep();
    }

    /** Hauled out by day with nothing to do: lie up and bask, mouth open. Anything coming close sends it back in. */
    private void tickBasking() {
        boolean day = this.level.isDay();
        if (this.isSitting()) {
            // 2.8.0: the disturbance sweep ran every tick for every basking crocodile; every 10 ticks (staggered)
            // still sends it into the water within half a second of something approaching.
            boolean disturbed = (this.tickCount + this.getId()) % 10 == 0
                    && !this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(6.0D),
                    e -> e != this && !(e instanceof EntityCrocodilian) && e.isAlive()
                            && (e instanceof net.minecraft.world.entity.player.Player || untamedwilds.util.EcologyTags.isPredator(e))).isEmpty();
            if (this.isInWater() || this.getTarget() != null || !this.wantsToBeOnLand() || disturbed || !day) {
                this.setSitting(false);
            }
        }
        else if (day && !this.isInWater() && this.wantsToBeOnLand() && this.getTarget() == null && this.isNotMoving()
                && this.canMove() && this.getAnimation() == NO_ANIMATION && this.random.nextInt(400) == 0) {
            this.getNavigation().stop();
            this.setSitting(true);
        }
        boolean basking = this.isSitting() && day && !this.isInWater();
        if (basking != this.isBasking()) {
            this.entityData.set(BASKING, basking);
        }
    }

    /** During ATTACK_ROLL the victim is held under: pulled to the jaws, slowed, and bitten every 10 ticks. */
    private void tickDeathRoll() {
        if (this.getAnimation() != ATTACK_ROLL || this.rollVictim == null) {
            this.rollVictim = null;
            return;
        }
        LivingEntity v = this.rollVictim;
        if (!v.isAlive() || v.distanceToSqr(this) > 25.0D) {
            this.rollVictim = null;
            return;
        }
        Vec3 jaws = this.position().add(this.getLookAngle().scale(this.getBbWidth() * 0.9D));
        v.setDeltaMovement(jaws.subtract(v.position()).scale(0.3D));
        v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
        if (this.getAnimationTick() % 10 == 5) {
            v.hurt(DamageSource.mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4F);
            this.satiateFromKill(v);
        }
    }

    public boolean doHurtTarget(Entity entityIn) {
        boolean flag = super.doHurtTarget(entityIn);
        if (flag) {
            this.satiateFromKill(entityIn);
            if (this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
                if (this.isInWater() && entityIn instanceof LivingEntity victim && victim.isAlive() && this.random.nextInt(2) == 0) {
                    this.rollVictim = victim;
                    this.setAnimation(ATTACK_ROLL);
                    this.playSound(SoundEvents.GENERIC_SPLASH, 1.4F, 0.7F);
                } else {
                    this.setAnimation(ATTACK_BITE);
                }
                this.setAnimationTick(0);
            }
        }
        return flag;
    }

    public boolean hurt(DamageSource damageSource, float amount) {
        performRetaliation(damageSource, this.getHealth(), amount, false);
        return super.hurt(damageSource, amount);
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.TURTLE_SHAMBLE, 0.3F, 0.5F);
    }

    @Nullable
    public EntityCrocodilian getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityCrocodilian(ModEntity.CROCODILIAN.get(), this.level));
    }

    // ------------------------------------------------------------------------------------------------ nesting

    @Override
    public boolean wantsToLayEggs() {
        return this.entityData.get(HAS_EGG);
    }

    @Override
    public void setEggStatus(boolean status) {
        /* Eggs laid: the rearing interval starts here, since an egg-layer never runs breed() (2026-11-14). */
        if (!status && this.wantsToLayEggs() && EcologyMode.usesConditionForBreeding(this)) {
            this.rearingTicks = (int) (this.getAdulthoodTime() * ConfigGamerules.predatorRearingFraction.get());
        }
        this.entityData.set(HAS_EGG, status);
    }

    @Override
    public Block getNestType() {
        return ModBlock.NEST_REPTILE.get();
    }

    @Override
    public boolean isValidNestBlock(BlockPos pos) {
        return this.level.isEmptyBlock(pos) && this.level.getBlockState(pos.below()).is(ModTags.ModBlockTags.VALID_REPTILE_NEST)
                && this.getNestType().defaultBlockState().canSurvive(this.level, pos);
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("has_egg", this.wantsToLayEggs());
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(HAS_EGG, compound.getBoolean("has_egg"));
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_BITE, ATTACK_ROLL};
    }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
    }

    /** The ambush strike: a hunting crocodile in the water bursts at a target within 6 blocks. */
    static class LungeGoal extends Goal {
        private final EntityCrocodilian croc;

        LungeGoal(EntityCrocodilian croc) {
            this.croc = croc;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = this.croc.getTarget();
            return t != null && t.isAlive() && this.croc.lungeCooldown == 0 && !this.croc.isBaby()
                    && (this.croc.isInWater() || this.croc.isLandHunter()) && this.croc.distanceToSqr(t) < 36.0D
                    && this.croc.distanceToSqr(t) > 4.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity t = this.croc.getTarget();
            if (t == null) {
                return;
            }
            Vec3 dir = t.position().subtract(this.croc.position()).normalize();
            this.croc.setDeltaMovement(dir.x * 0.9D, 0.25D, dir.z * 0.9D);
            this.croc.lookAt(t, 30F, 30F);
            this.croc.lungeCooldown = 80;
            this.croc.playSound(this.croc.isInWater() ? SoundEvents.GENERIC_SPLASH : SoundEvents.RAVAGER_ATTACK, 1.0F, 0.8F);
        }
    }
}
