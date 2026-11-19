package untamedwilds.entity.mammal;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import untamedwilds.UntamedWilds;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.*;
import untamedwilds.entity.ai.target.DefendHerdMateTarget;
import untamedwilds.entity.ai.target.SmartHurtByTargetGoal;
import untamedwilds.init.ModEntity;
import untamedwilds.util.EcologyTags;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Pinnipeds -- walrus, bearded seal, ringed seal, harp seal, Steller sea lion (Phase 4.4, 2026-10-10). Amphibious, on
 * the hippo's ComplexMobAmphibious base.
 *
 *  * HAUL-OUT COLONIES: by day a pinniped wants the land (ice, beach, shore) where the colony rests and sleeps; at night,
 *    or whenever it is hungry, it wants the water, and FEEDS there -- hunger refills while it swims (fish are abstracted,
 *    the mod's fish are not its prey list). AmphibiousTransition moves it between the two.
 *  * PREY for the polar bear (tags `aquatic` + `small_game` on the seals) and the big sharks. Seals flee predators toward
 *    nothing in particular -- the herd scatters -- while WALRUS stand and fight with their tusks (ATTACK_TUSK) and the
 *    herd defends its members; an adult walrus out-ranks a polar bear on eco level.
 *  * BULL WALRUSES JOUST: two idle adult males within 6 blocks occasionally rear up and swing their tusks at each
 *    other (ATTACK_JOUST, 40 t). Cosmetic -- no damage, no target.
 *  * WHITE-COAT PUPS: harp and ringed seal babies wear <species>_pup.png (the lanugo coat) instead of the adult skin.
 */
public class EntityPinniped extends ComplexMobAmphibious implements INewSkins, ISpecies, IPackEntity, INeedsPostUpdate {

    public static final Animation ATTACK_BITE = Animation.create(12);
    public static final Animation ATTACK_TUSK = Animation.create(16);
    public static final Animation ATTACK_JOUST = Animation.create(40);

    private static final int WALRUS = 0;
    private static final int RINGED = 2;
    private static final int HARP = 3;

    private int joustCooldown = 600;

    public EntityPinniped(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.turn_speed = 0.2F;
    }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.4D));
        this.goalSelector.addGoal(2, new HerdFleeGoal<>(this, LivingEntity.class, 16, 1.3D, 1.6D,
                input -> this.getVariant() != WALRUS && EcologyTags.isPredator(input)));
        this.goalSelector.addGoal(3, new AlertGoal(this));   // 2.8.0 vigilance (below flight, never equal)
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.3D, false));
        this.goalSelector.addGoal(3, new SmartMateGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new AmphibiousTransition(this, 1.1D));
        this.goalSelector.addGoal(4, new JoustGoal(this));
        this.goalSelector.addGoal(4, new GotoSleepGoal(this, 1D));
        this.goalSelector.addGoal(5, new SmartWanderGoal(this, 1D, 120, 0, false));
        this.goalSelector.addGoal(5, new AmphibiousRandomSwimGoal(this, 1, 120));
        this.goalSelector.addGoal(6, new SmartLookAtGoal(this, LivingEntity.class, 10.0F));
        this.targetSelector.addGoal(1, new SmartHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new DefendHerdMateTarget(this));
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.8D)
                .add(Attributes.MOVEMENT_SPEED, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.ARMOR, 2D);
    }

    /* Haul-out: rest ashore by day, go to sea at night or when hungry. */
    @Override
    public boolean wantsToBeOnLand() {
        return this.level.isDay() && this.getHunger() > 60;
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
            // feeding at sea: fish are abstracted -- a pinniped swimming refills its hunger
            if (this.isInWater() && (this.level.getGameTime() + this.getId()) % 100 == 0) {
                this.addHunger(4);
            }
            if (this.joustCooldown > 0) {
                this.joustCooldown--;
            }
            this.setAngry(this.getTarget() != null);
        }
        super.aiStep();
    }

    boolean canJoust() {
        return this.getVariant() == WALRUS && this.isMale() && !this.isBaby() && this.joustCooldown == 0
                && this.getTarget() == null && !this.isInWater() && !this.isSleeping() && this.getAnimation() == NO_ANIMATION;
    }

    @Override
    public ResourceLocation getTexture() {
        if (this.isBaby() && (this.getVariant() == HARP || this.getVariant() == RINGED)) {
            return new ResourceLocation(UntamedWilds.MOD_ID, "textures/entity/pinniped/" + this.getRawSpeciesName(this.getVariant()) + "_pup.png");
        }
        return super.getTexture();
    }

    public boolean doHurtTarget(Entity entityIn) {
        boolean flag = super.doHurtTarget(entityIn);
        if (flag && this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
            this.setAnimation(this.getVariant() == WALRUS ? ATTACK_TUSK : ATTACK_BITE);
        }
        return flag;
    }

    public boolean hurt(DamageSource damageSource, float amount) {
        performRetaliation(damageSource, this.getHealth(), amount, this.getVariant() == WALRUS);
        return super.hurt(damageSource, amount);
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.SLIME_SQUISH, 0.2F, 0.6F);
    }

    @Nullable
    public EntityPinniped getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityPinniped(ModEntity.PINNIPED.get(), this.level));
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_BITE, ATTACK_TUSK, ATTACK_JOUST};
    }

    public Animation getAnimationEat() { return NO_ANIMATION; }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
    }

    /** Cosmetic tusk-jousting between two idle bull walruses (the kangaroo SparGoal pattern). */
    static class JoustGoal extends Goal {
        private final EntityPinniped self;
        private EntityPinniped partner;
        private int timer;

        JoustGoal(EntityPinniped self) {
            this.self = self;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.self.canJoust() || this.self.getRandom().nextInt(400) != 0) {
                return false;
            }
            List<EntityPinniped> near = this.self.level.getEntitiesOfClass(EntityPinniped.class,
                    this.self.getBoundingBox().inflate(6.0D), e -> e != this.self && e.canJoust());
            if (near.isEmpty()) {
                return false;
            }
            this.partner = near.get(0);
            return true;
        }

        @Override
        public void start() {
            this.timer = ATTACK_JOUST.getDuration();
            for (EntityPinniped p : new EntityPinniped[]{this.self, this.partner}) {
                p.getNavigation().stop();
                p.setAnimation(ATTACK_JOUST);
                p.joustCooldown = 1200 + p.getRandom().nextInt(1200);
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
