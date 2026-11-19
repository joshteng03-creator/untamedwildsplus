package untamedwilds.entity.bird;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.*;
import untamedwilds.entity.ai.target.DefendHerdMateTarget;
import untamedwilds.entity.ai.target.ProtectChildrenTarget;
import untamedwilds.entity.ai.target.SmartHurtByTargetGoal;
import untamedwilds.init.ModEntity;
import untamedwilds.util.EcologyTags;

import javax.annotation.Nullable;

/**
 * Giant ground birds (2.9.0, group D, NEW TYPE): the giant moa, the elephant bird and Genyornis. Large flightless
 * BROWSING birds in small groups; they run from predators (HerdFleeGoal), stand and kick when cornered or defending
 * their chicks (the cassowary's weapon: ATTACK_KICK throws), and visit water and graze like every other herbivore.
 * <p>
 * Built from EntityTapir (the lean herd herbivore). Chicks are born live, as the giant monitor's are: an egg item and a
 * nest would be new assets.
 * Model: client/model/ModelGroundBird (scripts/ground_bird_spec.py -- fitted biped walk, solved sit / sleep); EAT /
 * DRINK / ALERT are solved against it by scripts/pose_reach.py and written by scripts/gen_keyframes.py.
 */
public class EntityGroundBird extends ComplexMobTerrestrial implements INewSkins, ISpecies, IPackEntity, INeedsPostUpdate {

    public static final Animation ATTACK_KICK = Animation.create(18);
    public static final Animation ATTACK_THREATEN = Animation.create(40);
    public static final Animation EAT = Animation.create(40);   // 2.8.0 idle pose (gen_keyframes.py)
    public static final Animation DRINK = Animation.create(100);   // 2.8.0 idle pose (gen_keyframes.py)
    public static final Animation ALERT = Animation.create(40);   // 2.8.0 idle pose (gen_keyframes.py)

    public EntityGroundBird(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.maxUpStep = 1.25F;
        this.turn_speed = 0.25F;
    }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new SmartSwimGoal_Land(this));
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new HerdFleeGoal<>(this, LivingEntity.class, 20, 1.6D, 2.0D, EcologyTags::isPredator));
        this.goalSelector.addGoal(3, new AlertGoal(this));   // 2.8.0 vigilance (below flight, never equal)
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.5D, false));
        this.goalSelector.addGoal(3, new SmartMateGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new GrazeGoal(this, 10));
        this.goalSelector.addGoal(3, new DrinkGoal(this));   // 2.8.0 cosmetic waterhole visits
        this.goalSelector.addGoal(3, new HerdMigrationGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new GotoSleepGoal(this, 1D));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(5, new SmartWanderGoal(this, 1D, 120, 0, true));
        this.goalSelector.addGoal(6, new SmartLookAtGoal(this, LivingEntity.class, 10.0F));
        this.targetSelector.addGoal(1, new SmartHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new ProtectChildrenTarget<>(this, LivingEntity.class, true, input -> !(input instanceof EntityGroundBird)));
        this.targetSelector.addGoal(3, new DefendHerdMateTarget(this));
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    public boolean wantsToBreed() {
        return this.wantsToBreedAsHerbivore();
    }

    @Override
    public void aiStep() {
        if (!this.level.isClientSide) {
            if (this.herd == null) {
                IPackEntity.initPack(this);
            } else {
                this.herd.tick();
            }
            if ((this.level.getGameTime() + this.getId()) % 1000 == 0) {
                this.addHunger(-10);
                if (!this.isStarving()) {
                    this.heal(1.0F);
                }
            }
            int i = this.random.nextInt(3000);
            if (i == 13 && !this.isInWater() && this.getTarget() == null && this.isNotMoving() && this.canMove() && this.getAnimation() == NO_ANIMATION) {
                this.setSitting(true);
            }
            if (i == 14 && this.isSitting()) {
                this.setSitting(false);
            }
            this.setAngry(this.getTarget() != null);
            if (this.getTarget() != null && this.tickCount % 120 == 0 && this.getAnimation() == NO_ANIMATION
                    && this.distanceToSqr(this.getTarget()) > 16) {
                this.setAnimation(ATTACK_THREATEN);
            }
        }
        if (this.getAnimation() == ATTACK_THREATEN && this.getAnimationTick() == 2 && this.getThreatSound() != null) {
            this.playSound(this.getThreatSound(), this.getSoundVolume(), this.getVoicePitch());
        }
        super.aiStep();
    }

    public boolean doHurtTarget(Entity entityIn) {
        boolean flag = super.doHurtTarget(entityIn);
        if (flag && this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
            this.setAnimation(ATTACK_KICK);
            if (entityIn instanceof LivingEntity living) {
                HoglinBase.hurtAndThrowTarget(this, living);
            }
        }
        return flag;
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.4F, 0.6F);
    }

    @Nullable
    public EntityGroundBird getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityGroundBird(ModEntity.GROUND_BIRD.get(), this.level));
    }

    // gen_keyframes declares EAT but leaves its getter to the entity (every herbivore defines it itself)
    public Animation getAnimationEat() { return EAT; }
    @Override
    public Animation getAnimationDrink() { return DRINK; }
    @Override
    public Animation getAnimationAlert() { return ALERT; }
    // 2.8.0 solved DRINK reach (gen_keyframes.py): muzzle 21.0 units in front of the centre at size 1
    @Override
    public double getDrinkReach() { return 1.313D * this.getMobSize(); }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_KICK, ATTACK_THREATEN, EAT, DRINK, ALERT};
    }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
    }
}
