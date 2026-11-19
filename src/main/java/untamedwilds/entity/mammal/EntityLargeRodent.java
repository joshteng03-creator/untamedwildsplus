package untamedwilds.entity.mammal;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.*;
import untamedwilds.entity.ai.target.DefendHerdMateTarget;
import untamedwilds.entity.ai.target.SmartHurtByTargetGoal;
import untamedwilds.init.ModEntity;
import untamedwilds.util.EcologyTags;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Large rodents -- capybara, Neochoerus, Castoroides, North American beaver (Phase 4.6, 2026-10-10). Semi-aquatic, on the
 * hippo's ComplexMobAmphibious base: they feed ashore (capybaras graze by day, beavers come out by day and work the water
 * by night) and DIVE to escape.
 *
 *  * BIRD PASSENGER: a resting capybara now and then gets a small bird perched on its back (synced, saved); it flies off
 *    the moment the capybara moves fast, is hurt or swims.
 *  * TAIL-SLAP ALARM: a beaver that spots a predator within 14 blocks slaps its tail (ATTACK_SLAP) and every large rodent
 *    within 16 blocks that is ashore heads for the nearest water.
 *  * BEAVER DAMS: an adult beaver occasionally builds a small dam of logs and earth across SHALLOW water (a still source
 *    block with solid ground under it, next to a bank or an existing dam block), at most 16 blocks per beaver. Obeys the
 *    vanilla mobGriefing gamerule.
 *  * Prey for the jaguar, anaconda, wolves and the big cats (tag small_game; the two giants also grazer).
 */
public class EntityLargeRodent extends ComplexMobAmphibious implements INewSkins, ISpecies, IPackEntity, INeedsPostUpdate {

    public static final Animation ATTACK_BITE = Animation.create(12);
    public static final Animation ATTACK_SLAP = Animation.create(16);
    public static final Animation EAT = Animation.create(40);   // 2.8.0 idle pose (gen_keyframes.py)
    public static final Animation ALERT = Animation.create(40);   // 2.8.0 idle pose (gen_keyframes.py)

    private static final EntityDataAccessor<Boolean> BIRD = SynchedEntityData.defineId(EntityLargeRodent.class, EntityDataSerializers.BOOLEAN);

    private static final int CAPYBARA = 0;
    private static final int CASTOROIDES = 2;
    private static final int BEAVER = 3;
    private static final int MAX_DAM_BLOCKS = 16;

    private int slapCooldown = 0;
    private int damBlocks = 0;

    public EntityLargeRodent(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.turn_speed = 0.25F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BIRD, false);
    }

    public boolean hasBird() { return this.entityData.get(BIRD); }

    private boolean isBeaver() { return this.getVariant() == CASTOROIDES || this.getVariant() == BEAVER; }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.6D));
        this.goalSelector.addGoal(2, new HerdFleeGoal<>(this, LivingEntity.class, 16, 1.5D, 1.9D, EcologyTags::isPredator));
        this.goalSelector.addGoal(3, new AlertGoal(this));   // 2.8.0 vigilance (below flight, never equal)
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.3D, false));
        this.goalSelector.addGoal(3, new SmartMateGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new GrazeGoal(this, 10));
        this.goalSelector.addGoal(4, new AmphibiousTransition(this, 1.1D));
        this.goalSelector.addGoal(4, new DamGoal(this));
        this.goalSelector.addGoal(4, new GotoSleepGoal(this, 1D));
        this.goalSelector.addGoal(5, new SmartWanderGoal(this, 1D, 120, 0, false));
        this.goalSelector.addGoal(5, new AmphibiousRandomSwimGoal(this, 1, 120));
        this.goalSelector.addGoal(6, new SmartLookAtGoal(this, LivingEntity.class, 10.0F));
        this.targetSelector.addGoal(1, new SmartHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new DefendHerdMateTarget(this));
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.4D)
                .add(Attributes.MOVEMENT_SPEED, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.ARMOR, 1D);
    }

    /* Capybaras keep to the bank and take to the water when threatened; beavers are ashore by day, in the water by night. */
    @Override
    public boolean wantsToBeOnLand() {
        boolean threatened = this.getLastHurtByMob() != null && this.tickCount - this.getLastHurtByMobTimestamp() < 300;
        if (threatened) {
            return false;
        }
        return !this.isBeaver() || this.level.isDay();
    }

    @Override
    public boolean wantsToBeInWater() {
        return !this.wantsToBeOnLand();
    }

    public boolean wantsToBreed() {
        return this.wantsToBreedAsHerbivore();
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
            if ((this.level.getGameTime() + this.getId()) % 1000 == 0) {
                this.addHunger(-10);
                if (!this.isStarving()) {
                    this.heal(1.0F);
                }
            }
            if (this.slapCooldown > 0) {
                this.slapCooldown--;
            }
            this.tickBird();
            this.tickAlarm();
            this.setAngry(this.getTarget() != null);
        }
        super.aiStep();
    }

    private void tickBird() {
        if (this.getVariant() != CAPYBARA || this.isBaby()) {
            if (this.hasBird()) this.entityData.set(BIRD, false);
            return;
        }
        if (this.hasBird() && (this.getCurrentSpeed() > 0.12D || this.hurtTime > 0 || this.isInWater())) {
            this.entityData.set(BIRD, false);
        }
        else if (!this.hasBird() && (this.level.getGameTime() + this.getId()) % 200 == 0 && this.isNotMoving()
                && !this.isInWater() && this.level.isDay() && this.random.nextInt(4) == 0) {
            this.entityData.set(BIRD, true);
        }
    }

    /** The beaver tail-slap: a predator nearby sends every large rodent ashore toward the nearest water. */
    private void tickAlarm() {
        if (!this.isBeaver() || this.isBaby() || this.slapCooldown > 0 || (this.tickCount + this.getId()) % 20 != 0) {
            return;
        }
        boolean danger = !this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(14.0D),
                e -> e != this && EcologyTags.isPredator(e) && !(e instanceof EntityLargeRodent)).isEmpty();
        if (!danger) {
            return;
        }
        this.slapCooldown = 400;
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ATTACK_SLAP);
        }
        this.playSound(SoundEvents.GENERIC_SPLASH, 1.2F, 0.8F);
        for (EntityLargeRodent r : this.level.getEntitiesOfClass(EntityLargeRodent.class, this.getBoundingBox().inflate(16.0D))) {
            if (!r.isInWater()) {
                BlockPos water = r.nearestWater(10);
                if (water != null) {
                    r.getNavigation().moveTo(water.getX() + 0.5D, water.getY(), water.getZ() + 0.5D, 1.8D);
                }
            }
        }
    }

    @Nullable
    BlockPos nearestWater(int radius) {
        BlockPos me = this.blockPosition();
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-radius, -3, -radius), me.offset(radius, 2, radius))) {
            if (this.level.getFluidState(p).is(Fluids.WATER)) {
                double d = p.distSqr(me);
                if (d < bd) {
                    bd = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    public boolean doHurtTarget(Entity entityIn) {
        boolean flag = super.doHurtTarget(entityIn);
        if (flag && this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
            this.setAnimation(ATTACK_BITE);
        }
        return flag;
    }

    public boolean hurt(DamageSource damageSource, float amount) {
        performRetaliation(damageSource, this.getHealth(), amount, true);
        return super.hurt(damageSource, amount);
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.PIG_STEP, 0.15F, 0.8F);
    }

    @Nullable
    public EntityLargeRodent getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityLargeRodent(ModEntity.LARGE_RODENT.get(), this.level));
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("HasBird", this.hasBird());
        compound.putInt("DamBlocks", this.damBlocks);
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(BIRD, compound.getBoolean("HasBird"));
        this.damBlocks = compound.getInt("DamBlocks");
    }

    @Override
    public Animation getAnimationAlert() { return ALERT; }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_BITE, ATTACK_SLAP, EAT, ALERT};
    }

    public Animation getAnimationEat() { return EAT; }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
    }

    /**
     * Beaver dam building. Rarely (about once every two minutes per idle adult beaver) picks a SHALLOW still-water block --
     * a water source with a solid block beneath and air above, next to a bank or an existing dam block -- walks to it and
     * fills it with a horizontal log or packed earth (alternating, the stick-and-mud of a real dam). Capped at
     * MAX_DAM_BLOCKS per beaver; nothing happens when mobGriefing is off.
     */
    static class DamGoal extends Goal {
        private final EntityLargeRodent beaver;
        private BlockPos target;
        private int timeout;

        DamGoal(EntityLargeRodent beaver) {
            this.beaver = beaver;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.beaver.isBeaver() || this.beaver.isBaby() || this.beaver.damBlocks >= MAX_DAM_BLOCKS
                    || this.beaver.getTarget() != null || !this.beaver.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                    || this.beaver.getRandom().nextInt(2400) != 0) {
                return false;
            }
            this.target = this.findSpot();
            return this.target != null;
        }

        @Nullable
        private BlockPos findSpot() {
            Level lvl = this.beaver.level;
            BlockPos me = this.beaver.blockPosition();
            for (int k = 0; k < 40; k++) {
                BlockPos p = me.offset(this.beaver.getRandom().nextInt(17) - 8, this.beaver.getRandom().nextInt(5) - 2, this.beaver.getRandom().nextInt(17) - 8);
                if (!lvl.getFluidState(p).isSource() || !lvl.getFluidState(p).is(Fluids.WATER)) continue;
                if (!lvl.getBlockState(p.above()).isAir()) continue;
                if (!lvl.getBlockState(p.below()).getMaterial().isSolid()) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockState n = lvl.getBlockState(p.relative(d));
                    if (n.getMaterial().isSolid() && !n.is(Blocks.WATER)) {
                        return p;
                    }
                }
            }
            return null;
        }

        @Override
        public void start() {
            this.timeout = 400;
            this.beaver.getNavigation().moveTo(this.target.getX() + 0.5D, this.target.getY(), this.target.getZ() + 0.5D, 1.0D);
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.timeout > 0 && this.beaver.getTarget() == null;
        }

        @Override
        public void tick() {
            this.timeout--;
            if (this.beaver.distanceToSqr(this.target.getX() + 0.5D, this.target.getY(), this.target.getZ() + 0.5D) < 6.5D) {
                Level lvl = this.beaver.level;
                if (lvl.getFluidState(this.target).isSource()) {
                    BlockState block = (this.beaver.damBlocks % 2 == 0)
                            ? Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                                this.beaver.getRandom().nextBoolean() ? Direction.Axis.X : Direction.Axis.Z)
                            : Blocks.COARSE_DIRT.defaultBlockState();
                    lvl.setBlockAndUpdate(this.target, block);
                    this.beaver.playSound(SoundEvents.WOOD_PLACE, 0.8F, 0.9F);
                    this.beaver.damBlocks++;
                }
                this.target = null;
            }
        }

        @Override
        public void stop() {
            this.target = null;
        }
    }
}
