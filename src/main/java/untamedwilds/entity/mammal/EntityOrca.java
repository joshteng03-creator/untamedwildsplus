package untamedwilds.entity.mammal;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreathAirGoal;
import net.minecraft.world.level.Level;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.FishReturnToSchoolGoal;
import untamedwilds.entity.ai.FishWanderAsSchoolGoal;
import untamedwilds.entity.ai.MeleeAttackCircle;
import untamedwilds.entity.ai.SmartMateGoal;
import untamedwilds.entity.ai.target.HuntPackMobTarget;
import untamedwilds.entity.ai.target.HurtPackByTargetGoal;
import untamedwilds.entity.ai.unique.WhaleBreachGoal;
import untamedwilds.init.ModEntity;
import untamedwilds.init.ModSounds;
import untamedwilds.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Toothed whales (2.9.0, group D, NEW TYPE): the orca and the false killer whale. A POD predator of the open ocean --
 * the herd system is the pod, HuntPackMobTarget gives the whole pod one quarry (the way orcas work a seal or a whale
 * together) and MeleeAttackCircle closes on it from all sides. What it may take is the usual pair of filters: the
 * species diet (aquatic -- fish, sharks, pinnipeds, whales) and the eco-level comparison, with the pod's weight behind it.
 * <p>
 * Built from the shark (aquatic hunter, ring buffer for the body bend) and the baleen whale (surfacing, breaching).
 * Like the whales it breathes through ComplexMobAquatic, so BreathAirGoal is a cosmetic trip to the surface.
 * Model: client/model/ModelOrca (scripts/orca_spec.py); the dorsal fin height is per animal (male orca tall, female
 * shorter, false killer whale small via the `finScale` flag).
 */
public class EntityOrca extends ComplexMobAquatic implements ISpecies, IAnimatedEntity, INewSkins, INeedsPostUpdate, IPackEntity {

    public static final Animation ATTACK_BITE = Animation.create(14);
    private int animationTick;
    private Animation currentAnimation;
    public int ringBufferIndex = -1;
    public final double[][] ringBuffer = new double[64][3];

    public EntityOrca(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.turn_speed = 0.12F;
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2D)
                .add(Attributes.MOVEMENT_SPEED, 0.9D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1D)
                .add(Attributes.MAX_HEALTH, 80.0D);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new BreathAirGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackCircle(this, 1.6D, false, 2.5F));
        this.goalSelector.addGoal(3, new SmartMateGoal(this, 1D));
        this.goalSelector.addGoal(4, new FishWanderAsSchoolGoal(this));
        this.goalSelector.addGoal(4, new FishReturnToSchoolGoal(this));
        this.goalSelector.addGoal(5, new WhaleBreachGoal(this, 40));
        this.targetSelector.addGoal(1, new HurtPackByTargetGoal(this).setAlertOthers(EntityOrca.class));
        this.targetSelector.addGoal(3, new HuntPackMobTarget<>(this, LivingEntity.class, true, 30, false,
                input -> !(input instanceof EntityOrca) && getEcoLevelAsPrey(input) < getEcoLevel(this)));
    }

    public void aiStep() {
        super.aiStep();
        AnimationHandler.INSTANCE.updateAnimations(this);
        if (!this.level.isClientSide) {
            if (this.herd == null) {
                IPackEntity.initPack(this);
            } else {
                this.herd.tick();
            }
            this.setAngry(this.getTarget() != null);
            if ((this.level.getGameTime() + this.getId()) % 1000 == 0) {
                this.drainHuntingHunger(3);
                if (this.wantsToBreed() && !this.isMale()) {
                    this.setAge(this.getPregnancyTime());
                }
                if (!this.isStarving()) {
                    this.heal(this.getHunger() >= 120 ? 4.0F : 1.0F);
                }
            }
            if (this.getTarget() != null && this.tickCount % 160 == 0 && this.getThreatSound() != null) {
                this.playSound(this.getThreatSound(), this.getSoundVolume(), this.getVoicePitch());
            }
        }
        if (this.getAnimation() == ATTACK_BITE && this.getAnimationTick() == 5) {
            this.playSound(ModSounds.ENTITY_ATTACK_BITE, 1.5F, 0.6F);
        }

        if (!this.isNoAi() && !this.isBaby()) {
            if (this.ringBufferIndex < 0) {
                for (int i = 0; i < this.ringBuffer.length; ++i) {
                    this.ringBuffer[i][0] = this.getYRot();
                    this.ringBuffer[i][1] = this.getY();
                }
            }
            this.ringBufferIndex++;
            if (this.ringBufferIndex == this.ringBuffer.length) {
                this.ringBufferIndex = 0;
            }
            this.ringBuffer[this.ringBufferIndex][0] = this.yRotO + 0.5F * Mth.wrapDegrees(this.getYRot() - this.yRotO);
            this.ringBuffer[ringBufferIndex][1] = this.getY();
        }
    }

    public double[] getMovementOffsets(int offset, float partialTicks) {
        if (this.isDeadOrDying()) {
            partialTicks = 0.0F;
        }
        partialTicks = 1.0F - partialTicks;
        int i = this.ringBufferIndex - offset & 63;
        int j = this.ringBufferIndex - offset - 1 & 63;
        double[] adouble = new double[3];
        double d0 = this.ringBuffer[i][0];
        double d1 = this.ringBuffer[j][0] - d0;
        adouble[0] = d0 + d1 * (double) partialTicks;
        d0 = this.ringBuffer[i][1];
        d1 = this.ringBuffer[j][1] - d0;
        adouble[1] = d0 + d1 * (double) partialTicks;
        adouble[2] = Mth.lerp(partialTicks, this.ringBuffer[i][2], this.ringBuffer[j][2]);
        return adouble;
    }

    /* Breeding: the shared predator gate (sustained condition, not locally overcrowded -- never seasonal) and a
     * nearby animal of the other sex and the same species. */
    public boolean wantsToBreed() {
        if (ConfigGamerules.naturalBreeding.get() && this.getAge() == 0 && EntityUtils.hasFullHealth(this) && this.wantsToBreedAsPredator()) {
            List<EntityOrca> list = this.level.getEntitiesOfClass(EntityOrca.class, this.getBoundingBox().inflate(16.0D, 8.0D, 16.0D));
            list.removeIf(input -> EntityUtils.isInvalidPartner(this, input, false));
            if (list.size() >= 1) {
                this.setAge(this.getPregnancyTime());
                list.get(0).setAge(this.getPregnancyTime());
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverWorld, AgeableMob ageableEntity) {
        return create_offspring(new EntityOrca(ModEntity.ORCA.get(), this.level));
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.GUARDIAN_FLOP;
    }

    public boolean doHurtTarget(Entity entityIn) {
        this.applyAttackBonus(this.getHuntDamageBonus(entityIn));
        boolean flag;
        try {
            flag = super.doHurtTarget(entityIn);
        } finally {
            this.clearAttackBonus();
        }
        if (flag) {
            this.satiateFromKill(entityIn);
            if (this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
                this.setAnimation(ATTACK_BITE);
            }
        }
        return flag;
    }

    /** A pod hunter, so the same reduction as the canids and hyenas (prey / defenders / players only). */
    @Override
    protected float getIncomingDamageFactor(DamageSource source) {
        return appliesPackHunterReduction(source) ? 1F - ConfigGamerules.packHunterDamageReduction.get().floatValue() : 1F;
    }

    public boolean shouldLeavePack() {
        return this.random.nextInt(4000) == 0;
    }

    public int getAnimationTick() { return animationTick; }
    public void setAnimationTick(int tick) { animationTick = tick; }
    public Animation getAnimation() { return currentAnimation; }
    public void setAnimation(Animation animation) {
        if (animation != currentAnimation) {
            animationTick = 0;
        }
        currentAnimation = animation;
    }
    public Animation[] getAnimations() { return new Animation[]{NO_ANIMATION, ATTACK_BITE}; }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
    }
}
