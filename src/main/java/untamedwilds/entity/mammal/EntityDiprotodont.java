package untamedwilds.entity.mammal;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import untamedwilds.UntamedWilds;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.*;
import untamedwilds.entity.ai.target.DefendHerdMateTarget;
import untamedwilds.entity.ai.target.SmartHurtByTargetGoal;
import untamedwilds.entity.ai.target.SmartOwnerHurtTargetGoal;
import untamedwilds.init.ModEntity;
import untamedwilds.util.EntityUtils;

import javax.annotation.Nullable;

/**
 * Diprotodont -- Sahul's megaherbivores (Phase 4.1, 2026-10-09): Diprotodon, Zygomaturus,
 * Nototherium, and New Guinea's Hulitherium and Maokopia. Forked from EntityTapir.
 *
 * What differs from the tapir:
 *  * ADULTS STAND THEIR GROUND. A healthy adult diprotodont is too big for anything in its range
 *    to take, so it does not register a flight goal at all: it turns and shoves (ATTACK_GORE throws
 *    the target, HoglinBase-style). Juveniles and the small New Guinea species are what Thylacoleo
 *    actually eats, and the herd's DefendHerdMateTarget is what makes taking one costly.
 *  * THE JOEY. Diprotodontids carried their young in a REAR-facing pouch, as wombats do. A synced
 *    flag, refreshed every 2 s on the server, is true for an adult female with a baby of her own
 *    type within 10 blocks; ModelDiprotodont shows a head peeking from the pouch while it is set.
 *  * RIDEABLE (the mounts system) -- but only the three large species. The hitbox is per TYPE, so
 *    the MountProfile.MIN_HEIGHT test cannot tell a 1.8 m Diprotodon from a 0.6 m Maokopia; the
 *    species scale decides instead, and the seat height follows the RENDERED size for the same
 *    reason.
 */
public class EntityDiprotodont extends ComplexMobTerrestrial implements INewSkins, ISpecies, IPackEntity, INeedsPostUpdate {

    public static final Animation ATTACK_THREATEN = Animation.create(50);
    public static final Animation ATTACK_GORE = Animation.create(14);
    public static final Animation EAT = Animation.create(40);   // 2.8.0 idle pose (gen_keyframes.py)
    public static final Animation DRINK = Animation.create(100);   // 2.8.0 idle pose (gen_keyframes.py)

    private static final EntityDataAccessor<Boolean> JOEY = SynchedEntityData.defineId(EntityDiprotodont.class, EntityDataSerializers.BOOLEAN);

    /** Species below this scale (Hulitherium 0.54, Maokopia 0.40) are too small to ride. */
    private static final float MIN_MOUNT_SCALE = 0.7F;
    /** Seat height in blocks per unit of render scale: the back top is 23.4u (1.46 blocks) on the mesh. */
    private static final double SEAT_PER_SCALE = 1.3D;

    public EntityDiprotodont(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.maxUpStep = 1F;
        this.turn_speed = 0.15F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(JOEY, false);
    }

    public boolean hasJoey() { return this.entityData.get(JOEY); }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new SmartSwimGoal_Land(this));
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.4D));
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.4D, false));
        this.goalSelector.addGoal(3, new SmartMateGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new GrazeGoal(this, 10));
        this.goalSelector.addGoal(3, new DrinkGoal(this));   // 2.8.0 cosmetic waterhole visits
        this.goalSelector.addGoal(3, new HerdMigrationGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new GotoSleepGoal(this, 1D));
        this.goalSelector.addGoal(5, new SmartWanderGoal(this, 1D, 120, 0, true));
        this.goalSelector.addGoal(6, new SmartLookAtGoal(this, LivingEntity.class, 10.0F));
        this.targetSelector.addGoal(1, new SmartHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new DefendHerdMateTarget(this));
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
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.FOLLOW_RANGE, 14.0D)
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.ARMOR, 4D);
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
            if ((this.level.getGameTime() + this.getId()) % 1000 == 0) { // staggered by id: every animal on the same tick was a sync-packet burst
                this.addHunger(-10);
                if (!this.isStarving()) {
                    this.heal(1.0F);
                }
            }
            if ((this.level.getGameTime() + this.getId()) % 40 == 0) {
                this.entityData.set(JOEY, this.carriesJoey());
            }
            int i = this.random.nextInt(3000);
            if (i == 13 && !this.isInWater() && this.getTarget() == null && this.isNotMoving() && this.canMove() && this.getAnimation() == NO_ANIMATION) {
                this.setSitting(true);
            }
            if (i == 14 && this.isSitting()) {
                this.setSitting(false);
            }
            this.setAngry(this.getTarget() != null);
        }
        else {
            if (this.getAnimation() == ATTACK_THREATEN) {
                this.setSprinting(this.getAnimationTick() % 18 < 6);
            }
        }
        super.aiStep();
    }

    /** An adult female with a baby of her own type close by -- the young one is "in the pouch". */
    private boolean carriesJoey() {
        if (this.isMale() || this.isBaby()) {
            return false;
        }
        return !this.level.getEntitiesOfClass(EntityDiprotodont.class, this.getBoundingBox().inflate(10.0D),
                e -> e != this && e.isBaby() && e.getVariant() == this.getVariant()).isEmpty();
    }

    public boolean doHurtTarget(Entity entityIn) {
        boolean flag = super.doHurtTarget(entityIn);
        if (flag && this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
            this.setAnimation(ATTACK_GORE);
            HoglinBase.hurtAndThrowTarget(this, (LivingEntity) entityIn);
        }
        return flag;
    }

    public boolean hurt(DamageSource damageSource, float amount) {
        performRetaliation(damageSource, this.getHealth(), amount, true);
        return super.hurt(damageSource, amount);
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playFootstep(SoundEvents.HOGLIN_STEP, 0.2F, 0.7F);   // 2.8.0 heavy steps (JSON "step")
    }

    @Nullable
    public EntityDiprotodont getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityDiprotodont(ModEntity.DIPROTODONT.get(), this.level));
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (hand == InteractionHand.MAIN_HAND && !this.level.isClientSide()) {
            if (!this.isTame() && this.isBaby() && EntityUtils.hasFullHealth(this) && this.isFood(itemstack)) {
                this.playSound(SoundEvents.HORSE_EAT, 1.5F, 0.7F);
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
    public MountProfile getMountProfile() {
        return this.getModelScale() >= MIN_MOUNT_SCALE ? MountProfile.DIPROTODONT : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return this.isSaddled() ? this.getMobSize() * this.getScale() * SEAT_PER_SCALE : super.getPassengersRidingOffset();
    }

    @Override
    public Animation getAnimationDrink() { return DRINK; }
    // 2.8.0 solved DRINK reach (gen_keyframes.py): muzzle 31.7 units in front of the centre at size 1
    @Override
    public double getDrinkReach() { return 1.980D * this.getMobSize(); }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_THREATEN, ATTACK_GORE, EAT, DRINK};
    }

    public Animation getAnimationEat() { return EAT; }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
    }
}
