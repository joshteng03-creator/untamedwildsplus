package untamedwilds.entity.mammal;

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
 * Great apes (2.9.0, group D, NEW TYPE): Gigantopithecus blacki and the Bornean orangutan. Fruit- and bamboo-eating
 * browsers of tropical forest, alone or in small family groups. They run only from a predator that outclasses them (the
 * orangutan from a tiger; Gigantopithecus from almost nothing), threaten before they fight (ATTACK_THREATEN, rearing up
 * off the fists), and fight with a heavy arm swipe.
 * <p>
 * Built from EntityTapir (the lean herbivore). Model: client/model/ModelGreatApe (scripts/great_ape_spec.py -- fitted
 * knuckle-walk, solved sit, slumped sleep); ALERT solved by pose_reach.py and written by gen_keyframes.py; EAT is a
 * hand-made hand-to-mouth (the mouth cannot reach the ground), and there is no DRINK (no DrinkGoal).
 */
public class EntityGreatApe extends ComplexMobTerrestrial implements INewSkins, ISpecies, IPackEntity, INeedsPostUpdate {

    public static final Animation ATTACK_SWIPE = Animation.create(16);
    public static final Animation ATTACK_THREATEN = Animation.create(40);
    public static final Animation EAT = Animation.create(40);   // hand-to-mouth, hand-made in ModelGreatApe (apes feed with their hands)
    public static final Animation ALERT = Animation.create(40);   // 2.8.0 idle pose (gen_keyframes.py)

    public EntityGreatApe(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.maxUpStep = 1.25F;
        this.turn_speed = 0.25F;
    }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new SmartSwimGoal_Land(this));
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new HerdFleeGoal<>(this, LivingEntity.class, 16, 1.5D, 1.9D,
                input -> EcologyTags.isPredator(input) && getEcoLevel(input) > getEcoLevel(this)));
        this.goalSelector.addGoal(3, new AlertGoal(this));   // 2.8.0 vigilance (below flight, never equal)
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.4D, false));
        this.goalSelector.addGoal(3, new SmartMateGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new GrazeGoal(this, 10));
        this.goalSelector.addGoal(3, new HerdMigrationGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new GotoSleepGoal(this, 1D));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(5, new SmartWanderGoal(this, 1D, 120, 0, true));
        this.goalSelector.addGoal(6, new SmartLookAtGoal(this, LivingEntity.class, 10.0F));
        this.targetSelector.addGoal(1, new SmartHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new ProtectChildrenTarget<>(this, LivingEntity.class, true, input -> !(input instanceof EntityGreatApe)));
        this.targetSelector.addGoal(3, new DefendHerdMateTarget(this));
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D);
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
            if (this.getTarget() != null && this.tickCount % 140 == 0 && this.getAnimation() == NO_ANIMATION
                    && this.distanceToSqr(this.getTarget()) > 16) {
                this.setAnimation(ATTACK_THREATEN);
            }
        }
        if (this.getAnimation() == ATTACK_THREATEN && this.getAnimationTick() == 4 && this.getThreatSound() != null) {
            this.playSound(this.getThreatSound(), this.getSoundVolume(), this.getVoicePitch());
        }
        super.aiStep();
    }

    public boolean doHurtTarget(Entity entityIn) {
        boolean flag = super.doHurtTarget(entityIn);
        if (flag && this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
            this.setAnimation(ATTACK_SWIPE);
        }
        return flag;
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.POLAR_BEAR_STEP, 0.25F, 1.0F);
    }

    @Nullable
    public EntityGreatApe getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityGreatApe(ModEntity.GREAT_APE.get(), this.level));
    }

    public Animation getAnimationEat() { return EAT; }
    @Override
    public Animation getAnimationAlert() { return ALERT; }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_SWIPE, ATTACK_THREATEN, EAT, ALERT};
    }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
    }
}
