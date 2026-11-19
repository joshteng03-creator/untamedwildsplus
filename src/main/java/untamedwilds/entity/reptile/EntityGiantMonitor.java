package untamedwilds.entity.reptile;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import untamedwilds.UntamedWilds;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.*;
import untamedwilds.entity.ai.target.*;
import untamedwilds.init.ModEntity;

import javax.annotation.Nullable;

/**
 * Giant monitors -- Megalania (Varanus priscus) and the Komodo dragon (Phase 4.3, 2026-10-10). A NEW type rather than
 * monitor species: the legacy monitor's hitbox (1.3 x 0.6, per type) and 64x32 rig cannot carry a 5 m lizard.
 *
 *  * SOLITARY APEX AMBUSH PREDATOR, wired like the bear: HuntMobTarget by diet (grazer / browser / small_game -- the
 *    kangaroos, small diprotodonts and the young of the big ones; adult Diprotodon out-ranks it on eco level), hunger
 *    drained between kills (drainHuntingHunger, the call the legacy monitor never makes) and refilled by kills.
 *  * VENOMOUS BITE: Poison II for 5 s on every hit -- varanid venom stops clotting and drops blood pressure, which is
 *    why a bitten animal that escapes still weakens. Attacks alternate a BITE and a head-shaking THRASH.
 *  * LIVE YOUNG for now: monitors lay eggs, but the legacy egg item hatches the small monitor type, and a giant-monitor
 *    egg would be a new visible asset (needs the user's approval first).
 */
public class EntityGiantMonitor extends ComplexMobTerrestrial implements INewSkins, ISpecies, INeedsPostUpdate {

    public static final Animation IDLE_TONGUE = Animation.create(20);
    public static final Animation ATTACK_BITE = Animation.create(14);
    public static final Animation ATTACK_THRASH = Animation.create(30);

    public EntityGiantMonitor(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.maxUpStep = 1F;
        this.turn_speed = 0.12F;
    }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new SmartSwimGoal_Land(this));
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.3D));
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.6D, false, 1));
        this.goalSelector.addGoal(4, new SmartMateGoal(this, 1D));
        this.goalSelector.addGoal(4, new GotoSleepGoal(this, 1D));
        this.goalSelector.addGoal(5, new PredatorRelocateGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new SmartWanderGoal(this, 1D, true));
        this.goalSelector.addGoal(7, new SmartLookAtGoal(this, LivingEntity.class, 10.0F));
        this.targetSelector.addGoal(1, new SmartHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new HuntMobTarget<>(this, LivingEntity.class, true, 30, false, input -> getEcoLevelAsPrey(input) < getEcoLevel(this)));
    }

    @Override
    protected void reassessTameGoals() {
        if (this.isTame()) {
            if (UntamedWilds.DEBUG) {
                UntamedWilds.LOGGER.info("Updating AI tasks for tamed mob");
            }
            this.goalSelector.addGoal(3, new SmartFollowOwnerGoal(this, 1.6D, 12.0F, 3.0F));
            this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new SmartOwnerHurtTargetGoal(this));
        }
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.ARMOR, 4D);
    }

    public boolean wantsToBreed() {
        return this.wantsToBreedAsLandPredator();
    }

    @Override
    public void aiStep() {
        if (!this.level.isClientSide) {
            if (this.tickCount % 1000 == 0) {
                this.drainHuntingHunger(2);
                if (!this.isStarving()) {
                    this.heal(this.getHunger() >= 120 ? 5.0F : 1.5F);
                }
            }
            if (this.getAnimation() == NO_ANIMATION && this.getTarget() == null && !this.isSleeping()) {
                int i = this.random.nextInt(2400);
                if (i <= 8 && !this.isInWater()) {
                    this.getNavigation().stop();
                    this.setSitting(true);
                }
                else if (i == 9 && this.isSitting()) {
                    this.setSitting(false);
                }
                else if (i > 2370) {
                    this.setAnimation(IDLE_TONGUE);
                }
            }
            this.setAngry(this.getTarget() != null);
        }
        super.aiStep();
    }

    public boolean doHurtTarget(Entity entityIn) {
        boolean flag = super.doHurtTarget(entityIn);
        if (flag) {
            if (entityIn instanceof LivingEntity victim) {
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            }
            this.satiateFromKill(entityIn);
            if (this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
                this.setAnimation(this.random.nextInt(3) == 0 ? ATTACK_THRASH : ATTACK_BITE);
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
        this.playSound(SoundEvents.TURTLE_SHAMBLE, 0.3F, 0.6F);
    }

    @Nullable
    public EntityGiantMonitor getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityGiantMonitor(ModEntity.GIANT_MONITOR.get(), this.level));
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, IDLE_TONGUE, ATTACK_BITE, ATTACK_THRASH};
    }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
    }
}
