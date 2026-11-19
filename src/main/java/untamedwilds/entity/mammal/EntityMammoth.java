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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.*;
import untamedwilds.entity.ai.*;
import untamedwilds.entity.ai.target.ProtectChildrenTarget;
import untamedwilds.entity.ai.target.SmartOwnerHurtTargetGoal;
import untamedwilds.init.ModEntity;
import untamedwilds.util.EntityUtils;

import javax.annotation.Nullable;
import untamedwilds.entity.ai.HerdFleeGoal;
import untamedwilds.entity.ai.SmartAvoidGoal;
import untamedwilds.entity.ai.MeleeAttackCircleHerd;
import untamedwilds.util.EcologyTags;
import untamedwilds.entity.ai.target.DefendHerdMateTarget;
import untamedwilds.entity.ai.target.SmartHurtByTargetGoal;
import untamedwilds.entity.ai.RetreatWhenRoutedGoal;
import untamedwilds.entity.MountProfile;

public class EntityMammoth extends ComplexMobTerrestrial implements INewSkins, ISpecies, IPackEntity, INeedsPostUpdate {

    private static final EntityDataAccessor<Boolean> WOOLLY_COAT = SynchedEntityData.defineId(EntityMammoth.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LARGE_TUSKS = SynchedEntityData.defineId(EntityMammoth.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLAT_BACK = SynchedEntityData.defineId(EntityMammoth.class, EntityDataSerializers.BOOLEAN);
    /* An INT rather than the boolean the other toggles use, because the three ear sizes are genuinely
     * three: the extinct proboscideans keep a small ear, Elephas maximus has a medium one, and
     * Loxodonta africana's is the huge fan the genus is known for. */
    private static final EntityDataAccessor<Integer> EAR_SIZE = SynchedEntityData.defineId(EntityMammoth.class, EntityDataSerializers.INT);
    // Rebuilt rig (2026-11-15): per-species FAMILIES. 0 is always the mammoth form, so a species without the flag is a mammoth.
    //   headShape 0 DOME / 1 TWIN / 2 SLOPE / 3 CREST / 4 FLAT;  backShape 0 SLOPE / 1 DIP / 2 ARCH / 3 LEVEL;
    //   tuskShape 0 SPIRAL / 1 FORWARD / 2 STRAIGHT / 3 UPCURVE / 4 TWIST / 5 SMALL
    private static final EntityDataAccessor<Integer> HEAD_SHAPE = SynchedEntityData.defineId(EntityMammoth.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BACK_SHAPE = SynchedEntityData.defineId(EntityMammoth.class, EntityDataSerializers.INT);
    // species scales of the July mesh, by variant -- only for rescaling pre-rebuild saves (see readAdditionalSaveData)
    private static final float[] PRE_REBUILD_SCALE = {1.0F, 1.278F, 1.381F, 1.0F, 1.312F, 0.8F, 1.05F, 0.9F, 0.95F, 1.554F, 0.55F, 0.45F};
    private static final EntityDataAccessor<Integer> TUSK_SHAPE = SynchedEntityData.defineId(EntityMammoth.class, EntityDataSerializers.INT);

    public static final Animation ATTACK_THREATEN = Animation.create(50);
    public static final Animation ATTACK_GORE = Animation.create(14);
    public static final Animation EAT = Animation.create(40); // matches GrazeGoal's 40-tick eatingGrassTimer
    public static final Animation DRINK = Animation.create(100);   // 2.8.0 idle pose (gen_keyframes.py)

    public EntityMammoth(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.maxUpStep = 1F;
        this.turn_speed = 0.2F;
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WOOLLY_COAT, false);
        this.entityData.define(LARGE_TUSKS, false);
        this.entityData.define(FLAT_BACK, false);
        this.entityData.define(EAR_SIZE, 0);
        this.entityData.define(HEAD_SHAPE, 0);
        this.entityData.define(BACK_SHAPE, 0);
        this.entityData.define(TUSK_SHAPE, 0);
    }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new SmartSwimGoal_Land(this));
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.5D));
        // Backs off from carnivores when there is no calf to defend; ProtectChildrenTarget
        // takes over when there is.
        this.goalSelector.addGoal(2, new SmartAvoidGoal<>(this, LivingEntity.class, 12, 1.1D, 1.4D, EcologyTags::isPredator));
        this.goalSelector.addGoal(2, new MeleeAttackCharger(this, 1.4F, 3));
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.6D, false));
        this.goalSelector.addGoal(3, new SmartMateGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new GrazeGoal(this, 10));
        this.goalSelector.addGoal(3, new DrinkGoal(this));   // 2.8.0 cosmetic waterhole visits (trunk drink)
        /* Density dependence, expressed as MOVEMENT rather than as a cap on births: a herd that
         * has eaten its range out, or that is standing in an overcrowded neighbourhood, shifts
         * ground, and surplus young adults disperse to found herds elsewhere. Runs on the herd
         * LEADER only -- SmartWanderGoal already paths every other member to within 7 blocks of
         * it, so moving the leader moves the herd. */
        this.goalSelector.addGoal(3, new HerdMigrationGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new GotoSleepGoal(this, 1D));
        this.goalSelector.addGoal(5, new SmartWanderGoal(this, 1D, 120, 0, true));
        this.goalSelector.addGoal(6, new SmartLookAtGoal(this, LivingEntity.class, 10.0F));
        this.targetSelector.addGoal(1, new SmartHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new DefendHerdMateTarget(this));
        this.targetSelector.addGoal(2, new ProtectChildrenTarget<>(this, LivingEntity.class, true, input -> EcologyTags.isThreatTo(this, input)));
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
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.19D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MAX_HEALTH, 90.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1D)
                .add(Attributes.ARMOR, 4D);
    }

    public boolean wantsToBreed() {
        /* Forage stress -- hungry with nothing edible within reach -- is the carrying capacity signal,
         * and it now suppresses births on its own. Animals still migrate first; they simply do not
         * calve on ground that cannot feed a calf. Falls back to the old predicate in Zoo mode. */
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

    public boolean doHurtTarget(Entity entityIn) {
        boolean flag = super.doHurtTarget(entityIn);
        if (flag && this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
            Animation anim = chooseAttackAnimation();
            this.setAnimation(anim);
            this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, this.getVoicePitch());
            HoglinBase.hurtAndThrowTarget(this, (LivingEntity) entityIn);
        }
        return flag;
    }

    public boolean hurt(DamageSource damageSource, float amount) {
        performRetaliation(damageSource, this.getHealth(), amount, true);
        return super.hurt(damageSource, amount);
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playFootstep(SoundEvents.RAVAGER_STEP, 0.15F, 1.0F);   // 2.8.0 heavy steps (JSON "step")
    }

    private Animation chooseAttackAnimation() {
        return ATTACK_GORE;
    }

    /* The mammoth is the reason this hook exists: MeleeAttackCharger used to pick the wind-up with two
     * hardcoded instanceof checks for bison and rhino, so the heaviest charger in the mod had no tell. */
    @Override
    public Animation getChargeAnimation() {
        return ATTACK_THREATEN;
    }

    @Nullable
    public EntityMammoth getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityMammoth(ModEntity.MAMMOTH.get(), this.level));
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (hand == InteractionHand.MAIN_HAND && !this.level.isClientSide()) {
            if (!this.isTame() && this.isBaby() && EntityUtils.hasFullHealth(this) && this.isFood(itemstack)) {
                this.playSound(SoundEvents.HORSE_EAT, 1.5F, 0.8F);
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
    public Animation getAnimationDrink() { return DRINK; }
    // 2.8.0 solved DRINK reach (gen_keyframes.py): muzzle 44.1 units in front of the centre at size 1
    @Override
    public double getDrinkReach() { return 2.755D * this.getMobSize(); }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_THREATEN, ATTACK_GORE, EAT, DRINK};
    }

    public Animation getAnimationEat() { return EAT; }

    public boolean hasWoollyCoat(){ return (this.entityData.get(WOOLLY_COAT)); }
    private void setWoollyCoat(boolean woolly){ this.entityData.set(WOOLLY_COAT, woolly); }
    public boolean hasLargeTusks(){ return (this.entityData.get(LARGE_TUSKS)); }
    private void setLargeTusks(boolean large){ this.entityData.set(LARGE_TUSKS, large); }
    public int getEarSize(){ return (this.entityData.get(EAR_SIZE)); }
    private void setEarSize(int size){ this.entityData.set(EAR_SIZE, size); }
    public boolean hasFlatBack(){ return (this.entityData.get(FLAT_BACK)); }
    public int getHeadShape(){ return this.entityData.get(HEAD_SHAPE); }
    public int getBackShape(){ return this.entityData.get(BACK_SHAPE); }
    public int getTuskShape(){ return this.entityData.get(TUSK_SHAPE); }
    private void setFlatBack(boolean flat){ this.entityData.set(FLAT_BACK, flat); }

    public void addAdditionalSaveData(CompoundTag compound){
        super.addAdditionalSaveData(compound);
        compound.putBoolean("hasWoollyCoat", this.hasWoollyCoat());
        compound.putBoolean("hasLargeTusks", this.hasLargeTusks());
        compound.putBoolean("flatBack", this.hasFlatBack());
        compound.putInt("earSize", this.getEarSize());
        compound.putInt("headShape", this.getHeadShape());
        compound.putInt("backShape", this.getBackShape());
        compound.putInt("tuskShape", this.getTuskShape());
    }

    public void readAdditionalSaveData(CompoundTag compound){
        super.readAdditionalSaveData(compound);
        this.setWoollyCoat(compound.getBoolean("hasWoollyCoat"));
        this.setLargeTusks(compound.getBoolean("hasLargeTusks"));
        this.setFlatBack(compound.getBoolean("flatBack"));
        this.setEarSize(compound.getInt("earSize"));
        // A save from before the 2026-11-15 rebuild has no shape tags. Its Size was rolled against the July mesh, which was
        // 45u tall where the new one is 48-54u, so carried over unchanged every old mammoth would render ~12% oversized.
        // Rescale by new/old species scale; the individual's own variation survives.
        if (!compound.contains("headShape") && getEntityData(this.getType()) != null) {
            int v = this.getVariant();
            if (v >= 0 && v < PRE_REBUILD_SCALE.length) {
                this.setMobSize(this.getMobSize() * this.getModelScale() / PRE_REBUILD_SCALE[v]);
            }
        }
        // Shapes are species constants, so they come from the species data -- an elephant saved before the rebuild has no
        // shape tags and would otherwise load as a mammoth. The ear size is refreshed for the same reason (the rebuild
        // gave the Palaeoloxodon species medium ears).
        this.applyShapeFlags();
    }

    private void applyShapeFlags() {
        if (getEntityData(this.getType()) == null) {
            return;
        }
        this.entityData.set(HEAD_SHAPE, getEntityData(this.getType()).getFlags(this.getVariant(), "headShape"));
        this.entityData.set(BACK_SHAPE, getEntityData(this.getType()).getFlags(this.getVariant(), "backShape"));
        this.entityData.set(TUSK_SHAPE, getEntityData(this.getType()).getFlags(this.getVariant(), "tuskShape"));
        this.setEarSize(getEntityData(this.getType()).getFlags(this.getVariant(), "earSize"));
        this.setLargeTusks(getEntityData(this.getType()).getFlags(this.getVariant(), "hasLargeTusks") == 1);
    }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
        this.setWoollyCoat(getEntityData(this.getType()).getFlags(this.getVariant(), "hasWoollyCoat") == 1);
        this.setLargeTusks(getEntityData(this.getType()).getFlags(this.getVariant(), "hasLargeTusks") == 1);
        this.setFlatBack(getEntityData(this.getType()).getFlags(this.getVariant(), "flatBack") == 1);
        this.setEarSize(getEntityData(this.getType()).getFlags(this.getVariant(), "earSize"));
        this.applyShapeFlags();
    }

    // Seat height (2026-11-15 rebuild): the saddle lies on the mid-back box, whose top depends on the back shape (model units,
    // measured in proboscidean_spec.py); a woolly coat's mane lifts it 5.2u. The rider sits at 0.89 of that, the diprotodont
    // convention. The fixed 2.6-block hitbox put every rider at ~2.5 blocks, from the 1.7 m pygmy to the 4.5 m namadicus.
    private static final float[] SEAT_TOP = {44.6F, 45.2F, 49.9F, 47.5F};   // SLOPE, DIP, ARCH, LEVEL
    private static final float COAT_SEAT = 5.2F;
    /** Below this species scale (the 1.0 m Sicilian dwarf, 0.33) a proboscidean is too small to ride. */
    private static final float MIN_MOUNT_SCALE = 0.45F;

    @Override
    public MountProfile getMountProfile() {
        return this.getModelScale() >= MIN_MOUNT_SCALE ? MountProfile.PROBOSCIDEAN : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        if (!this.isSaddled()) {
            return super.getPassengersRidingOffset();
        }
        int back = Math.max(0, Math.min(3, this.getBackShape()));
        float top = SEAT_TOP[back] + (this.hasWoollyCoat() ? COAT_SEAT : 0.0F);
        return this.getMobSize() * this.getScale() * top / 16.0F * 0.89F;
    }
}
