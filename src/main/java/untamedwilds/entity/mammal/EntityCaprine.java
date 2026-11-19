package untamedwilds.entity.mammal;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import untamedwilds.UntamedWilds;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.*;
import untamedwilds.entity.ai.target.DefendHerdMateTarget;
import untamedwilds.entity.ai.target.SmartHurtByTargetGoal;
import untamedwilds.entity.ai.target.SmartOwnerHurtTargetGoal;
import untamedwilds.init.ModEntity;
import untamedwilds.util.EcologyTags;
import untamedwilds.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Caprines -- wild goats and sheep (Phase 4.5, 2026-10-10): Alpine ibex, Siberian ibex, argali, bighorn, chamois and the
 * extinct Harrington's mountain goat. Mountain prey for the snow leopard and wolves (tags grazer + browser).
 *
 *  * CLIFF CLIMBERS: step up 1.5 blocks, and an idle herd's wandering is pulled toward HIGH, ROCKY ground (ClimbWanderGoal
 *    samples nearby surface points and walks to the highest one standing on stone), so herds end up on ledges and crags.
 *  * HERDS flee predators; a cornered adult lowers its horns and BUTTS (ATTACK_BUTT), throwing the target.
 *  * RUT CLASHES: two idle adult males of the same species within 6 blocks occasionally rear up and smash horns
 *    (ATTACK_CLASH, 36 t). Cosmetic -- no damage, no target.
 *  * RIDEABLE: the two big wild sheep, argali and bighorn, join the mounts system -- sure-footed (1.5-block step),
 *    able to leap, and they ram what they run into. The seat follows the rendered size (the hitbox is per type).
 */
public class EntityCaprine extends ComplexMobTerrestrial implements INewSkins, ISpecies, IPackEntity, INeedsPostUpdate {

    public static final Animation ATTACK_BUTT = Animation.create(14);
    public static final Animation ATTACK_CLASH = Animation.create(36);
    public static final Animation EAT = Animation.create(40);   // 2.8.0 idle pose (gen_keyframes.py)
    public static final Animation DRINK = Animation.create(100);   // 2.8.0 idle pose (gen_keyframes.py)
    public static final Animation ALERT = Animation.create(40);   // 2.8.0 idle pose (gen_keyframes.py)

    private static final int ARGALI = 2;
    private static final int BIGHORN = 3;
    /** Seat height in blocks per unit of render scale: the back top is 16u (1.0 block) on the mesh. */
    private static final double SEAT_PER_SCALE = 0.95D;

    private int clashCooldown = 600;

    public EntityCaprine(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.maxUpStep = 1.5F;
        this.turn_speed = 0.3F;
    }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new SmartSwimGoal_Land(this));
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.8D));
        this.goalSelector.addGoal(2, new HerdFleeGoal<>(this, LivingEntity.class, 20, 1.6D, 2.0D, EcologyTags::isPredator));
        this.goalSelector.addGoal(3, new AlertGoal(this));   // 2.8.0 vigilance (below flight, never equal)
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.6D, false));
        this.goalSelector.addGoal(3, new SmartMateGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new GrazeGoal(this, 10));
        this.goalSelector.addGoal(3, new DrinkGoal(this));   // 2.8.0 cosmetic waterhole visits
        this.goalSelector.addGoal(3, new HerdMigrationGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new ClashGoal(this));
        this.goalSelector.addGoal(4, new GotoSleepGoal(this, 1D));
        this.goalSelector.addGoal(5, new ClimbWanderGoal(this, 1.0D));
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
            this.goalSelector.addGoal(3, new SmartFollowOwnerGoal(this, 1.4D, 12.0F, 3.0F));
            this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new SmartOwnerHurtTargetGoal(this));
        }
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 18.0D)
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
                .add(Attributes.ARMOR, 1D);
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
            if (this.clashCooldown > 0) {
                this.clashCooldown--;
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
        super.aiStep();
    }

    boolean canClash() {
        return this.isMale() && !this.isBaby() && this.clashCooldown == 0 && this.getTarget() == null && this.canMove()
                && !this.isSleeping() && !this.isSitting() && !this.isInWater() && !this.isVehicle() && this.getAnimation() == NO_ANIMATION;
    }

    public boolean doHurtTarget(Entity entityIn) {
        boolean flag = super.doHurtTarget(entityIn);
        if (flag && this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
            this.setAnimation(ATTACK_BUTT);
            HoglinBase.hurtAndThrowTarget(this, (LivingEntity) entityIn);
        }
        return flag;
    }

    public boolean hurt(DamageSource damageSource, float amount) {
        performRetaliation(damageSource, this.getHealth(), amount, true);
        return super.hurt(damageSource, amount);
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.GOAT_STEP, 0.15F, 1.0F);
    }

    @Nullable
    public EntityCaprine getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityCaprine(ModEntity.CAPRINE.get(), this.level));
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (hand == InteractionHand.MAIN_HAND && !this.level.isClientSide()) {
            if (!this.isTame() && this.isBaby() && EntityUtils.hasFullHealth(this) && this.isFood(itemstack)) {
                this.playSound(SoundEvents.GOAT_EAT, 1.0F, 1.0F);
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
        return (this.getVariant() == ARGALI || this.getVariant() == BIGHORN) ? MountProfile.CAPRINE : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return this.isSaddled() ? this.getMobSize() * this.getScale() * SEAT_PER_SCALE : super.getPassengersRidingOffset();
    }

    @Override
    public Animation getAnimationDrink() { return DRINK; }
    @Override
    public Animation getAnimationAlert() { return ALERT; }
    // 2.8.0 solved DRINK reach (gen_keyframes.py): muzzle 14.7 units in front of the centre at size 1
    @Override
    public double getDrinkReach() { return 0.917D * this.getMobSize(); }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_BUTT, ATTACK_CLASH, EAT, DRINK, ALERT};
    }

    public Animation getAnimationEat() { return EAT; }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
    }

    /** Idle wandering pulled toward high, rocky ground: sample nearby surface points, walk to the highest on stone. */
    static class ClimbWanderGoal extends Goal {
        private final EntityCaprine mob;
        private final double speed;
        private BlockPos target;

        ClimbWanderGoal(EntityCaprine mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.mob.canMove() || this.mob.isVehicle() || this.mob.getTarget() != null || this.mob.isInWater()
                    || this.mob.getRandom().nextInt(240) != 0) {
                return false;
            }
            BlockPos best = null;
            int bestY = this.mob.blockPosition().getY();
            for (int k = 0; k < 14; k++) {
                int x = this.mob.blockPosition().getX() + this.mob.getRandom().nextInt(25) - 12;
                int z = this.mob.blockPosition().getZ() + this.mob.getRandom().nextInt(25) - 12;
                int y = this.mob.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos below = new BlockPos(x, y - 1, z);
                if (y > bestY && this.mob.level.getBlockState(below).is(BlockTags.BASE_STONE_OVERWORLD)) {
                    bestY = y;
                    best = new BlockPos(x, y, z);
                }
            }
            this.target = best;
            return best != null;
        }

        @Override
        public void start() {
            this.mob.getNavigation().moveTo(this.target.getX() + 0.5D, this.target.getY(), this.target.getZ() + 0.5D, this.speed);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.mob.getNavigation().isDone() && this.mob.getTarget() == null;
        }
    }

    /** Cosmetic rut clash between two idle males (the kangaroo SparGoal pattern). */
    static class ClashGoal extends Goal {
        private final EntityCaprine self;
        private EntityCaprine partner;
        private int timer;

        ClashGoal(EntityCaprine self) {
            this.self = self;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.self.canClash() || this.self.getRandom().nextInt(400) != 0) {
                return false;
            }
            List<EntityCaprine> near = this.self.level.getEntitiesOfClass(EntityCaprine.class, this.self.getBoundingBox().inflate(6.0D),
                    e -> e != this.self && e.getVariant() == this.self.getVariant() && e.canClash());
            if (near.isEmpty()) {
                return false;
            }
            this.partner = near.get(0);
            return true;
        }

        @Override
        public void start() {
            this.timer = ATTACK_CLASH.getDuration();
            for (EntityCaprine c : new EntityCaprine[]{this.self, this.partner}) {
                c.getNavigation().stop();
                c.setAnimation(ATTACK_CLASH);
                c.clashCooldown = 1200 + c.getRandom().nextInt(1200);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.timer > 0 && this.partner != null && this.partner.isAlive() && this.self.getTarget() == null
                    && this.self.distanceToSqr(this.partner) < 64.0D;
        }

        @Override
        public void tick() {
            this.timer--;
            this.self.getLookControl().setLookAt(this.partner, 30.0F, 30.0F);
            this.partner.getLookControl().setLookAt(this.self, 30.0F, 30.0F);
            this.self.getNavigation().stop();
        }

        @Override
        public void stop() {
            this.partner = null;
        }
    }
}
