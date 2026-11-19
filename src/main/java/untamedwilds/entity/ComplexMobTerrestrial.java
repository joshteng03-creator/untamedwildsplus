package untamedwilds.entity;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ai.control.look.SmartLandLookControl;
import untamedwilds.util.EntityUtils;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import java.util.List;
import net.minecraft.util.Mth;

public abstract class ComplexMobTerrestrial extends ComplexMob implements IAnimatedEntity, PlayerRideableJumping {

    public int sitProgress; // A counter which defines the progress towards the Sitting Poses
    public int ticksToSit;
    public int sleepProgress; // A counter which defines the progress towards the Sleeping Poses
    public int forceSleep; // Negative forceSleep keeps the mob awake, Positive forceSleep keeps the mob asleep
    protected int tiredCounter = 0;
    protected int buoyancy = 1;
    private int animationTick;
    private Animation currentAnimation;
    public float turn_speed = 0.2F;
    protected float swimSpeedMult = 1.0F;

    public ComplexMobTerrestrial(EntityType<? extends ComplexMob> type, Level worldIn){
        super(type, worldIn);
        this.moveControl = new MoveControl(this);
        this.lookControl = new SmartLandLookControl(this, 30);
        this.ticksToSit = 40;
    }

    public void aiStep() {
        if (!level.isClientSide) {
            this.cancelIdleAnimationIfBusy();
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
        if (!level.isClientSide) {
            if (this.forceSleep > 0) {
                this.forceSleep--;
            }
            else if (this.forceSleep < 0) {
                this.forceSleep++;
            }
            if (!this.getNavigation().isDone() && (this.isSitting() || this.isSleeping())) {
                this.setSitting(false);
                this.setSleeping(false);
            }

            if (this.getAirSupply() < 40 && this.tickCount % 10 == 0) { // TODO: There's probably a better place to dump this (mobs about to drown will go to the surface for air), but it refuses to work everywhere else
                this.getMoveControl().strafe(0.6F, 0);
                this.jumpFromGround();
            }

            if (!this.isSleeping() && this.forceSleep > 0) {
                this.setSleeping(true);
            }
            // 2.8.0: negative forceSleep means "keep awake" (an animal woken by a fight gets -300); this check ignored it,
            // so a woken animal could drop straight back to sleep at the next 200-tick mark. GotoSleepGoal already honours it.
            if (this.tickCount % 200 == 0 && this.forceSleep >= 0) {
                if (!this.isActive() && this.getNavigation().isDone()) {
                    this.tiredCounter++;
                    if (this.distanceToSqr(this.getHomeAsVec()) <= 6) {
                        this.setSleeping(true);
                        this.tiredCounter = 0;
                    }
                    else if (tiredCounter >= 3) {
                        this.setHome(BlockPos.ZERO);
                        this.tiredCounter = 0;
                    }
                    this.moveControl.setWantedPosition(this.getHome().getX(), this.getHome().getY(), this.getHome().getZ(), 1f);
                }
            }
        }
        if (this.isSitting() && this.sitProgress < this.ticksToSit) {
            this.sitProgress++;
        } else if (!this.isSitting() && this.sitProgress > 0) {
            this.sitProgress--;
        }
        if (this.isSleeping() && this.sleepProgress < 40) {
            this.sleepProgress++;
        } else if (!this.isSleeping() && this.sleepProgress > 0) {
            this.sleepProgress--;
        }
        super.aiStep();
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level.isClientSide()) {
            ItemStack itemstack = player.getItemInHand(hand);
            if (!this.isBaby() && this.isFood(itemstack) && !this.dead) {
                if (!this.level.isClientSide && !player.isCreative()) {
                    itemstack.shrink(1);
                }
                if (ConfigGamerules.playerBreeding.get() && this.age == 0) {
                    this.setInLove(player);
                    EntityUtils.spawnParticlesOnEntity(this.level, this, ParticleTypes.HEART, 7, 1);
                }
                this.tryStartAnimation(this.getAnimationEat());
                this.playSound(SoundEvents.GENERIC_EAT, 1F, 1);
                return InteractionResult.CONSUME;
            }
        }

        return super.mobInteract(player, hand);
    }

    protected ActivityType getActivityType() {
        return getEntityData(this.getType()).getActivityType(this.getVariant());
    }

    public boolean isActive() {
        ActivityType type = this.getActivityType();
        Pair<Integer, Integer> times = type.getTimes();
        if ((this.isTame() && this.getCommandInt() != 0) || !ConfigGamerules.sleepBehaviour.get()) {
            return true;
        }
        if (type == ActivityType.CATHEMERAL) {
            return this.tickCount % 17000 < 3000;
        }
        long time = this.level.getDayTime();
        if (!times.getFirst().equals(times.getSecond())) {
            if (times.getFirst() > times.getSecond()) {
                return time > times.getFirst() || time < times.getSecond();
            }
            else {
                return time > times.getFirst() && time < times.getSecond();
            }
        }
        return this.forceSleep >= 0;
    }

    public enum ActivityType {
        DIURNAL         ("diurnal", 1000, 16000), // From 7 AM to 10 PM
        NOCTURNAL       ("nocturnal", 13000, 4000), // From 7 PM to 10 AM
        CREPUSCULAR     ("crepuscular", 8000, 23000), // From 14 PM to 5 AM ; 4000 - 19000 ???
        CATHEMERAL      ("cathemeral", -1, -1),      // Random naps throughout the day
        INSOMNIAC       ("insomniac", -1, -1);      // No sleep, redundant, should just not add the GoToSleepGoal

        public int wakeUp;
        public int sleep;
        public String name;
        public static final Codec<ActivityType> CODEC = Codec.STRING.comapFlatMap(ActivityType::getByName, ActivityType::toString).stable();

        ActivityType(String name, int wakeUp, int sleep) {
            this.wakeUp = wakeUp;
            this.sleep = sleep;
            this.name = name;
        }

        private static DataResult<ActivityType> getByName(String path) {
            return switch (path) {
                case "diurnal" -> DataResult.success(DIURNAL);
                case "nocturnal" -> DataResult.success(NOCTURNAL);
                case "crepuscular" -> DataResult.success(CREPUSCULAR);
                case "cathemeral" -> DataResult.success(CATHEMERAL);
                default -> DataResult.success(INSOMNIAC);
            };
        }

        public String toString() {
            return this.name;
        }

        public Pair<Integer, Integer> getTimes() {
            return new Pair<>(this.wakeUp, this.sleep);
        }
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this.isSitting()) {
            this.setSitting(false);
        }
        if (this.isSleeping() && this.forceSleep <= 0) {
            this.setSleeping(false);
            this.forceSleep = -4000;
        }
        return super.hurt(source, amount);
    }

    public void die(DamageSource p_70645_1_) {
        if (!this.level.isClientSide && !ConfigGamerules.hardcoreDeath.get() && this.getHome() != BlockPos.ZERO && this.isTame() && this.getHunger() != 0) {
            this.addEffect(new MobEffectInstance(MobEffects.GLOWING, 800, 0));
            this.setHealth(0.5F);
            this.setHunger(0);
            if (!this.randomTeleport(this.getHome().getX(), this.getHome().getY(), this.getHome().getZ(), true)){
                super.die(p_70645_1_);
            }
        }
        else {
            super.die(p_70645_1_);
        }
    }

    // ---- Riding (2026-10-08), after vanilla AbstractHorse.travel ----------------------------------------
    private float mountJumpPending;
    private int chargeCooldown;
    private float restingStepHeight = -1F;

    @Override
    public void onPlayerJump(int power) {
        if (this.canJump()) {
            this.mountJumpPending = power >= 90 ? 1.0F : 0.4F + 0.4F * Math.max(0, power) / 90.0F;
        }
    }

    @Override
    public boolean canJump() {
        MountProfile mount = this.getMountProfile();
        return mount != null && mount.jump() > 0 && this.isSaddled();
    }

    @Override
    public void handleStartJump(int power) {}

    @Override
    public void handleStopJump() {}

    private boolean onSandOrSnow() {
        BlockState below = this.level.getBlockState(this.blockPosition().below());
        BlockState at = this.level.getBlockState(this.blockPosition());
        return below.is(BlockTags.SAND) || below.is(BlockTags.SNOW) || at.is(BlockTags.SNOW);
    }

    /** A charging mount moving at a run rams what is in front of it: attack damage + knockback, 2 s cooldown. */
    private void tryCharge(LivingEntity rider) {
        if (this.chargeCooldown > 0) {
            this.chargeCooldown--;
            return;
        }
        double moved = Math.sqrt((this.getX() - this.xo) * (this.getX() - this.xo) + (this.getZ() - this.zo) * (this.getZ() - this.zo));
        if (moved < 0.25D) {
            return;
        }
        Vec3 ahead = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        List<LivingEntity> hit = this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().move(ahead.scale(0.6D)).inflate(0.2D),
                e -> e != this && e != rider && !this.hasPassenger(e) && e.isAlive()
                        && !(e instanceof ComplexMob cm && cm.isTame() && cm.getOwner() == rider));
        if (hit.isEmpty()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity e : hit) {
            if (e.hurt(DamageSource.mobAttack(this), damage)) {
                e.knockback(1.2D, -ahead.x, -ahead.z);
            }
        }
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 1.0F);
        this.chargeCooldown = 40;
    }

    public void travel(Vec3 p_30218_) {
        MountProfile mount = this.getMountProfile();
        if (this.isAlive() && mount != null && this.isVehicle() && this.getControllingPassenger() instanceof LivingEntity rider) {
            if (this.restingStepHeight < 0) {
                this.restingStepHeight = this.maxUpStep;
            }
            this.maxUpStep = mount.stepHeight();
            this.setYRot(rider.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(rider.getXRot() * 0.5F);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.yBodyRot;
            float strafe = rider.xxa * 0.5F;
            float forward = rider.zza;
            if (forward <= 0.0F) {
                forward *= 0.25F;
            }
            if (this.mountJumpPending > 0.0F && this.isOnGround()) {
                double up = mount.jump() * this.mountJumpPending * this.getBlockJumpFactor() + this.getJumpBoostPower();
                Vec3 v = this.getDeltaMovement();
                this.setDeltaMovement(v.x, up, v.z);
                this.hasImpulse = true;
                ForgeHooks.onLivingJump(this);
                if (forward > 0.0F) {
                    float sin = Mth.sin(this.getYRot() * ((float) Math.PI / 180F));
                    float cos = Mth.cos(this.getYRot() * ((float) Math.PI / 180F));
                    this.setDeltaMovement(this.getDeltaMovement().add(-0.4F * sin * this.mountJumpPending, 0.0D, 0.4F * cos * this.mountJumpPending));
                }
                this.mountJumpPending = 0.0F;
            }
            this.flyingSpeed = this.getSpeed() * 0.1F;
            if (this.isControlledByLocalInstance()) {
                float speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * mount.speed();
                if (mount.sandRunner() && this.onSandOrSnow()) {
                    speed *= 1.2F;
                }
                this.setSpeed(speed);
                super.travel(new Vec3(strafe, p_30218_.y, forward));
            } else if (rider instanceof Player) {
                this.setDeltaMovement(Vec3.ZERO);
            }
            if (this.isOnGround()) {
                this.mountJumpPending = 0.0F;
            }
            this.calculateEntityAnimation(this, false);
            this.tryCheckInsideBlocks();
            if (mount.charge() && !this.level.isClientSide) {
                this.tryCharge(rider);
            }
            return;
        }
        if (this.restingStepHeight >= 0) {
            this.maxUpStep = this.restingStepHeight;
            this.restingStepHeight = -1F;
        }
        this.flyingSpeed = 0.02F;
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(0.025F, p_30218_);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(p_30218_);
        }
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }
    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }
    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }
    @Override
    public void setAnimation(Animation animation) {
        /* 2.8.0: switching to a DIFFERENT animation restarts it. Before, the tick carried over from whatever was
         * playing, so the new animation started part-way through, Citadel never saw tick 0, never posted its
         * start event and never told any client -- a mid-animation switch simply did not sync. Re-setting the
         * animation that is already playing still continues it. */
        if (animation != currentAnimation) {
            animationTick = 0;
        }
        currentAnimation = animation;
    }
    @Override
    public Animation[] getAnimations() {
        return new Animation[]{};
    }

    public Animation getAnimationEat() { return this.NO_ANIMATION; }

    // ---------------------------------------------------------------------------------------------
    // 2.8.0 idle animations (eat / drink / alert) and the priority rule that keeps them from getting in the way
    // ---------------------------------------------------------------------------------------------

    /** Head-down drink at a waterhole (DrinkGoal). NO_ANIMATION = this type has none. */
    public Animation getAnimationDrink() { return this.NO_ANIMATION; }

    /**
     * How far in front of its centre (blocks) this animal's muzzle reaches while drinking. DrinkGoal backs it off the
     * water by this much so the muzzle, not the whole animal, ends up over the water. Types override it with their
     * solved DRINK reach (scaled by getMobSize); the default is half the body width plus a little.
     */
    public double getDrinkReach() { return this.getBbWidth() * 0.5D + 0.4D; }

    /** Head-up vigilance stare at a distant threat (AlertGoal). NO_ANIMATION = this type has none. */
    public Animation getAnimationAlert() { return this.NO_ANIMATION; }

    public static final int ANIM_PRIORITY_IDLE = 1;       // eat, drink
    public static final int ANIM_PRIORITY_ALERT = 2;
    public static final int ANIM_PRIORITY_ACTION = 4;     // attacks, threats, anything else the type declares

    public int getAnimationPriority(Animation animation) {
        if (animation == null || animation == NO_ANIMATION) {
            return 0;
        }
        if (animation == this.getAnimationEat() || animation == this.getAnimationDrink()) {
            return ANIM_PRIORITY_IDLE;
        }
        if (animation == this.getAnimationAlert()) {
            return ANIM_PRIORITY_ALERT;
        }
        return ANIM_PRIORITY_ACTION;
    }

    public boolean isPlayingIdleAnimation() {
        Animation current = this.getAnimation();
        return current != null && current != NO_ANIMATION && this.getAnimationPriority(current) < ANIM_PRIORITY_ACTION;
    }

    private static final java.util.Set<String> WARNED_UNDECLARED = new java.util.HashSet<>();

    /**
     * Starts an animation unless something at least as important is already playing. Refuses NO_ANIMATION and
     * anything missing from {@link #getAnimations()} (Citadel syncs by index, so an undeclared animation would
     * only ever play on the server). Re-requesting the animation that is already playing just lets it continue.
     */
    public boolean tryStartAnimation(Animation animation) {
        if (animation == null || animation == NO_ANIMATION) {
            return false;
        }
        if (!org.apache.commons.lang3.ArrayUtils.contains(this.getAnimations(), animation)) {
            String key = this.getClass().getSimpleName() + "@" + System.identityHashCode(animation);
            if (WARNED_UNDECLARED.add(key)) {
                untamedwilds.UntamedWilds.LOGGER.warn("{} tried to play an animation missing from getAnimations(); ignored", this.getClass().getSimpleName());
            }
            return false;
        }
        Animation current = this.getAnimation();
        if (current == animation) {
            return true;
        }
        if (current != null && current != NO_ANIMATION && this.getAnimationPriority(current) >= this.getAnimationPriority(animation)) {
            return false;
        }
        this.setAnimation(animation);
        return true;
    }

    /** Stops {@code onlyIf} if it is what is playing, and tells the clients. */
    public void stopAnimation(Animation onlyIf) {
        if (onlyIf == null || onlyIf == NO_ANIMATION || this.getAnimation() != onlyIf) {
            return;
        }
        this.setAnimation(NO_ANIMATION);
        this.setAnimationTick(0);
        if (!this.level.isClientSide) {
            untamedwilds.util.AnimationNetworkEvents.sendToTrackers(this, this, NO_ANIMATION, 0);
        }
    }

    /**
     * An eating, drinking or watchful animal that is attacked, picks a fight or starts to flee drops the idle
     * animation at once. Every attack/threat in the mod starts only when nothing is playing, so without this a
     * grazing bison could not threaten a wolf until its 40-tick mouthful had finished.
     */
    protected void cancelIdleAnimationIfBusy() {
        if (this.isPlayingIdleAnimation() && (this.getTarget() != null || this.fleeCooldown > 0 || this.hurtTime > 0)) {
            this.stopAnimation(this.getAnimation());
        }
    }

    /**
     * Wind-up animation played by {@link untamedwilds.entity.ai.MeleeAttackCharger} while an animal
     * gathers itself for a charge. Overridden to ATTACK_THREATEN by every species that can charge; the
     * default is a no-op so a type without the goal never needs to care.
     */
    public Animation getChargeAnimation() { return this.NO_ANIMATION; }

    public void addAdditionalSaveData(CompoundTag compound){
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Sleeping", this.isSleeping());
        compound.putInt("SleepingTicks", this.forceSleep);
        compound.putBoolean("Sitting", this.isSitting());
    }

    public void readAdditionalSaveData(CompoundTag compound){
        super.readAdditionalSaveData(compound);
        this.setSleeping(compound.getBoolean("Sleeping"));
        this.forceSleep = compound.getInt("SleepingTicks");
        this.setSitting(compound.getBoolean("Sitting"));
    }

    /*static class MoveHelperController extends MoveControl {
        private final ComplexMobTerrestrial entity;

        MoveHelperController(ComplexMobTerrestrial turtleIn) {
            super(turtleIn);
            this.entity = turtleIn;
        }

        @Override
        public void tick() {
            if (this.action == MovementController.Action.STRAFE) {
                float f = (float)this.entity.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
                float f1 = (float)this.speed * f;
                float f2 = this.moveForward;
                float f3 = this.moveStrafe;
                float f4 = Mth.sqrt(f2 * f2 + f3 * f3);
                if (f4 < 1.0F) {
                    f4 = 1.0F;
                }

                f4 = f1 / f4;
                f2 = f2 * f4;
                f3 = f3 * f4;
                float f5 = Mth.sin(this.entity.rotationYaw * ((float)Math.PI / 180F));
                float f6 = Mth.cos(this.entity.rotationYaw * ((float)Math.PI / 180F));
                float f7 = f2 * f6 - f3 * f5;
                float f8 = f3 * f6 + f2 * f5;
                PathNavigator pathnavigator = this.entity.getNavigation();
                NodeProcessor nodeprocessor = pathnavigator.getNodeProcessor();
                if (nodeprocessor.getPathNodeType(this.entity.world, Mth.floor(this.entity.getX() + (double) f7), Mth.floor(this.entity.getY()), Mth.floor(this.entity.getZ() + (double) f8)) != PathNodeType.WALKABLE) {
                    this.moveForward = (float) this.getSpeed(); // EDITED: was hardcoded to 1.0F
                    this.moveStrafe = 0.0F;
                    f1 = f;
                }

                this.entity.setAIMoveSpeed(f1);
                this.entity.setMoveForward(this.moveForward);
                this.entity.setMoveStrafing(this.moveStrafe);
                this.action = MovementController.Action.WAIT;

            } else if (this.action == MovementController.Action.MOVE_TO) {
                this.action = MovementController.Action.WAIT;

                double d0 = this.posX - this.entity.getX();
                double d1 = this.posZ - this.entity.getZ();
                double d2 = this.posY - this.entity.getY();
                double d3 = d0 * d0 + d2 * d2 + d1 * d1;
                if (d3 < (double)2.5000003E-7F) {
                    this.entity.setMoveForward(0.0F);
                    return;
                }
                float f = (float)(Mth.atan2(d1, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                this.entity.rotationYaw = this.limitAngle(this.entity.rotationYaw, f, this.entity.turn_speed * 100);
                BlockPos blockpos = this.entity.getPosition();
                BlockState blockstate = this.entity.world.getBlockState(blockpos);
                Block block = blockstate.getBlock();
                VoxelShape voxelshape = blockstate.getCollisionShape(this.entity.world, blockpos);

                if (this.entity.isInWater()) {
                    if (this.entity.collidedHorizontally && this.entity.tickCount % 10 == 0) {
                        this.entity.jump();
                    }
                    //this.entity.setDeltaMovement(this.entity.getDeltaMovement().scale(1.1));
                    float f2 = -((float)(Mth.atan2(d1, Mth.sqrt(d0 * d0 + d2 * d2)) * (double)(180F / (float)Math.PI)));
                    f2 = Mth.clamp(Mth.wrapDegrees(f2), -85.0F, 85.0F);
                    this.entity.getXRot() = this.limitAngle(this.entity.getXRot(), f2, 1.0F);
                    float f4 = Mth.sin(this.entity.getXRot() * ((float)Math.PI / 180F));
                    this.entity.moveVertical = -f4 *  (float)this.entity.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
                    if (this.entity.getTarget() == null) {
                        this.entity.setDeltaMovement(this.entity.getDeltaMovement().add(0.0D, this.entity.buoyancy - 1, 0.0D));
                    }
                    //this.entity.getXRot() = this.limitAngle(this.entity.getXRot(), (float) (this.entity.getDeltaMovement().getY() * -40), 0.1F);
                }

                //((ServerWorld)this.entity.world).spawnParticle(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0.15F);
                float f1 = (float)(this.getSpeed() * this.entity.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
                this.entity.setAIMoveSpeed(Mth.lerp(0.125F, this.entity.getAIMoveSpeed(), f1));

                if (d2 > (double)this.entity.maxUpStep && d0 * d0 + d1 * d1 < (double)Math.max(1.0F, this.entity.getBbWidth()) || !voxelshape.isEmpty() && this.entity.getY() < voxelshape.getEnd(Direction.Axis.Y) + (double)blockpos.getY() && !block.isIn(BlockTags.DOORS) && !block.isIn(BlockTags.FENCES)) {
                    this.entity.getJumpControl().jump();
                    this.action = MovementController.Action.JUMPING;
                }
            } else if (this.action == MovementController.Action.JUMPING) {
                this.entity.setAIMoveSpeed((float)(this.speed * this.entity.getAttribute(Attributes.MOVEMENT_SPEED).getValue()));
                if (this.entity.onGround) {
                    this.action = MovementController.Action.WAIT;
                }
            } else {
                if (this.entity.isInWater() && this.entity.getTarget() == null && !this.entity.collidedHorizontally) {
                    this.entity.setDeltaMovement(this.entity.getDeltaMovement().add(0.0D, this.entity.buoyancy - 1, 0.0D));
                }
                this.entity.setMoveForward(0.0F);
            }
        }
    }*/
}
