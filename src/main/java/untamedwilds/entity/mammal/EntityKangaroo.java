package untamedwilds.entity.mammal;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.entity.ai.goal.Goal;
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
import untamedwilds.util.EcologyTags;
import untamedwilds.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Kangaroos -- the mod's first hopping animals (Phase 4.2, 2026-10-10): red, eastern grey, and the extinct giants
 * Procoptodon, Sthenurus and Protemnodon. Forked from EntityDiprotodont.
 *
 *  * HOPPING IS MODEL-ONLY (user decision): the entity moves along the ground like every other mob, so pathfinding
 *    and the herd AI are untouched; ModelKangaroo draws the hop at speed and a five-limbed crawl when slow.
 *  * Mobs FLEE FAST (HerdFleeGoal at x2.2): unlike the diprotodont, a kangaroo's defence is distance. Cornered, an
 *    adult KICKS -- rocks back on the tail and strikes with both hind feet (ATTACK_KICK), throwing the target.
 *  * MALES SPAR: two idle adult males of the same species within 6 blocks occasionally face each other and box for
 *    44 ticks (ATTACK_BOX). Purely cosmetic -- no damage, no target set, and either one breaks off when disturbed.
 *  * THE JOEY: a synced flag, refreshed every 2 s on the server, is true for an adult female with a baby of her own
 *    species within 10 blocks; ModelKangaroo shows a head peeking from the forward-facing pouch.
 *  * Not rideable.
 */
public class EntityKangaroo extends ComplexMobTerrestrial implements INewSkins, ISpecies, IPackEntity, INeedsPostUpdate {

    public static final Animation ATTACK_KICK = Animation.create(18);
    public static final Animation ATTACK_BOX = Animation.create(44);
    public static final Animation ATTACK_THREATEN = Animation.create(30);
    public static final Animation ALERT = Animation.create(40);   // 2.8.0 idle pose (gen_keyframes.py)

    private static final EntityDataAccessor<Boolean> JOEY = SynchedEntityData.defineId(EntityKangaroo.class, EntityDataSerializers.BOOLEAN);

    private int sparCooldown = 600;

    public EntityKangaroo(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.maxUpStep = 1F;
        this.turn_speed = 0.25F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(JOEY, false);
    }

    public boolean hasJoey() { return this.entityData.get(JOEY); }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new SmartSwimGoal_Land(this));
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 2.0D));
        this.goalSelector.addGoal(2, new HerdFleeGoal<>(this, LivingEntity.class, 20, 1.8D, 2.2D, EcologyTags::isPredator));
        this.goalSelector.addGoal(3, new AlertGoal(this));   // 2.8.0 vigilance (below flight, never equal)
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.6D, false));
        this.goalSelector.addGoal(3, new SmartMateGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new GrazeGoal(this, 10));
        this.goalSelector.addGoal(3, new HerdMigrationGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new SparGoal(this));
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
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
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
            if ((this.level.getGameTime() + this.getId()) % 1000 == 0) { // staggered by id: every animal on the same tick was a sync-packet burst
                this.addHunger(-10);
                if (!this.isStarving()) {
                    this.heal(1.0F);
                }
            }
            if ((this.level.getGameTime() + this.getId()) % 40 == 0) {
                this.entityData.set(JOEY, this.carriesJoey());
            }
            if (this.sparCooldown > 0) {
                this.sparCooldown--;
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

    /** An adult female with a baby of her own species close by -- the young one is "in the pouch". */
    private boolean carriesJoey() {
        if (this.isMale() || this.isBaby()) {
            return false;
        }
        return !this.level.getEntitiesOfClass(EntityKangaroo.class, this.getBoundingBox().inflate(10.0D),
                e -> e != this && e.isBaby() && e.getVariant() == this.getVariant()).isEmpty();
    }

    /** Idle adult male, free to spar. */
    boolean canSpar() {
        return this.isMale() && !this.isBaby() && this.sparCooldown == 0 && this.getTarget() == null && this.canMove()
                && !this.isSleeping() && !this.isSitting() && !this.isInWater() && this.getAnimation() == NO_ANIMATION;
    }

    /** 2.8.0: a kangaroo squaring up to fight (retaliation, defending the mob) gives its growl-cough first. */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        this.threatCallOnNewTarget(target);
        super.setTarget(target);
    }

    public boolean doHurtTarget(Entity entityIn) {
        boolean flag = super.doHurtTarget(entityIn);
        if (flag && this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
            this.setAnimation(ATTACK_KICK);
            HoglinBase.hurtAndThrowTarget(this, (LivingEntity) entityIn);
        }
        return flag;
    }

    public boolean hurt(DamageSource damageSource, float amount) {
        performRetaliation(damageSource, this.getHealth(), amount, true);
        return super.hurt(damageSource, amount);
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.RABBIT_JUMP, 0.3F, 0.6F);
    }

    @Nullable
    public EntityKangaroo getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityKangaroo(ModEntity.KANGAROO.get(), this.level));
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (hand == InteractionHand.MAIN_HAND && !this.level.isClientSide()) {
            if (!this.isTame() && this.isBaby() && EntityUtils.hasFullHealth(this) && this.isFood(itemstack)) {
                this.playSound(SoundEvents.HORSE_EAT, 1.0F, 1.3F);
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
    public Animation getAnimationAlert() { return ALERT; }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_KICK, ATTACK_BOX, ATTACK_THREATEN, ALERT};
    }

    public Animation getAnimationEat() { return NO_ANIMATION; }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
    }

    /**
     * Cosmetic sparring between two adult males. Checked rarely (1 in 400 ticks per candidate), so it is an occasional
     * sight rather than a constant one; both animals stop, face each other and play ATTACK_BOX, then each waits
     * 1-2 minutes before it can spar again. No damage and no target, so it never turns into a real fight.
     */
    static class SparGoal extends Goal {
        private final EntityKangaroo self;
        private EntityKangaroo partner;
        private int timer;

        SparGoal(EntityKangaroo self) {
            this.self = self;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.self.canSpar() || this.self.getRandom().nextInt(400) != 0) {
                return false;
            }
            List<EntityKangaroo> near = this.self.level.getEntitiesOfClass(EntityKangaroo.class,
                    this.self.getBoundingBox().inflate(6.0D),
                    e -> e != this.self && e.getVariant() == this.self.getVariant() && e.canSpar());
            if (near.isEmpty()) {
                return false;
            }
            this.partner = near.get(0);
            return true;
        }

        @Override
        public void start() {
            this.timer = ATTACK_BOX.getDuration();
            for (EntityKangaroo k : new EntityKangaroo[]{this.self, this.partner}) {
                k.getNavigation().stop();
                k.setAnimation(ATTACK_BOX);
                k.sparCooldown = 1200 + k.getRandom().nextInt(1200);
            }
            // the bout opens with the challenger's growl-cough (2.8.0)
            SoundEvent growl = this.self.getThreatSound();
            if (growl != null) {
                this.self.playSound(growl, this.self.getSoundVolume() * 0.8F, this.self.voicePitch());
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
