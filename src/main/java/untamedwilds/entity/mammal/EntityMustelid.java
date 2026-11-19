package untamedwilds.entity.mammal;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.*;
import untamedwilds.entity.ai.target.*;
import untamedwilds.init.ModEntity;
import untamedwilds.init.ModSounds;

import javax.annotation.Nullable;

/**
 * Mustelids (2.9.0, group D, NEW TYPE): the wolverine and the honey badger. A small, SOLITARY, fearless predator and
 * scavenger, wired like the bear (the other solitary predator that scavenges): HuntMobTarget filtered by the species
 * diet and the eco level, FeedOnCarcassGoal on other predators' kills, the shared land-predator breeding gate.
 * <p>
 * Deliberately NOT given the bear's kill-stealing (displacementGroupSize): a 15 kg animal taking every kill off a wolf
 * pack would starve the packs. It scavenges what is left instead (canScavengeCarcass) -- which is most of how a
 * wolverine really eats in winter.
 * Model: client/model/ModelMustelid (scripts/mustelid_spec.py, solved sit / sleep).
 */
public class EntityMustelid extends ComplexMobTerrestrial implements ISpecies, INewSkins, INeedsPostUpdate {

    public static final Animation ATTACK_BITE = Animation.create(12);
    public static final Animation IDLE_TALK = Animation.create(24);

    public EntityMustelid(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.maxUpStep = 1F;
        this.turn_speed = 0.3F;
    }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new SmartSwimGoal_Land(this));
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.5D, false, 1));
        this.goalSelector.addGoal(3, new SmartAvoidGoal<>(this, LivingEntity.class, 12, 1.2D, 1.6D, input -> getEcoLevel(input) > getEcoLevel(this) + 4));
        this.goalSelector.addGoal(4, new FeedOnCarcassGoal(this));
        this.goalSelector.addGoal(4, new SmartMateGoal(this, 1D));
        this.goalSelector.addGoal(4, new GotoSleepGoal(this, 1D));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(5, new PredatorRelocateGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new SmartWanderGoal(this, 1D, true));
        this.goalSelector.addGoal(7, new SmartLookAtGoal(this, LivingEntity.class, 10.0F));
        this.targetSelector.addGoal(1, new SmartHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new ProtectChildrenTarget<>(this, LivingEntity.class, true, input -> !(input instanceof EntityMustelid)));
        this.targetSelector.addGoal(3, new HuntMobTarget<>(this, LivingEntity.class, true, 30, false, input -> getEcoLevelAsPrey(input) < getEcoLevel(this)));
        this.targetSelector.addGoal(4, new AngrySleeperTarget<>(this, LivingEntity.class, true));
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.3D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 20D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
                .add(Attributes.ARMOR, 2D);
    }

    @Override
    public boolean canScavengeCarcass() {
        return true;
    }

    public boolean wantsToBreed() {
        // Same rule as every land predator; see ComplexMob.wantsToBreedAsLandPredator.
        return this.wantsToBreedAsLandPredator();
    }

    @Nullable
    public EntityMustelid getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityMustelid(ModEntity.MUSTELID.get(), this.level));
    }

    public void aiStep() {
        if (!this.level.isClientSide) {
            if ((this.level.getGameTime() + this.getId()) % 1000 == 0) {
                this.drainHuntingHunger(3);
                if (!this.isStarving()) {
                    this.heal(this.getHunger() >= 120 ? 4.0F : 1.0F);
                }
            }
            this.setAngry(this.getTarget() != null);
            if (this.getAnimation() == NO_ANIMATION && this.getTarget() == null && !this.isSleeping()) {
                if (this.getCommandInt() == 0) {
                    int i = this.random.nextInt(3000);
                    if (i == 0 && !this.isInWater() && this.isNotMoving() && this.canMove() && this.isActive()) {
                        this.getNavigation().stop();
                        this.setSitting(true);
                    }
                    if ((i == 1 || this.isInWater()) && this.isSitting() && this.getCommandInt() < 2) {
                        this.setSitting(false);
                    }
                    if (i > 2985 && !this.isInWater() && !this.isBaby()) {
                        this.setAnimation(IDLE_TALK);
                    }
                }
            }
            if (this.getTarget() != null && this.tickCount % 100 == 0 && this.getAnimation() == NO_ANIMATION) {
                this.setAnimation(IDLE_TALK);
            }
        }
        if (this.getAnimation() == ATTACK_BITE && this.getAnimationTick() == 4) {
            this.playSound(ModSounds.ENTITY_ATTACK_BITE, 1.2F, 1.1F);
        }
        if (this.getAnimation() == IDLE_TALK && this.getAnimationTick() == 1 && this.getThreatSound() != null) {
            this.playSound(this.getThreatSound(), this.getSoundVolume(), this.getVoicePitch());
        }
        super.aiStep();
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

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.WOLF_STEP, 0.12F, 1.2F);   // vanilla 1.18 has no fox step
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_BITE, IDLE_TALK};
    }

    public Animation getAnimationEat() { return NO_ANIMATION; }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
    }
}
