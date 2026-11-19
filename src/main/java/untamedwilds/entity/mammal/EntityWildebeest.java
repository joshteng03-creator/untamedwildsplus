package untamedwilds.entity.mammal;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import untamedwilds.UntamedWilds;
import untamedwilds.compat.CompatSereneSeasons;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.config.EcologyMode;
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
 * Wildebeest and hartebeest -- blue wildebeest, black wildebeest, Megalotragus priscus, hartebeest (Phase 4.7,
 * 2026-10-23). Africa's migratory grazers: prey for the lions, leopards, cheetahs, hyenas and wild dogs (tag grazer).
 *
 *  * GREAT MIGRATION: on top of the forage-driven range shifting every herd already does (HerdMigrationGoal), a wildebeest
 *    herd's leader periodically sets off on a long TREK -- gamerules.wildebeest_trek_distance blocks along a heading that
 *    turns 60-120 degrees each time, so successive treks loop round a circuit instead of drifting off in one direction.
 *    Without Serene Seasons the interval is gamerules.wildebeest_trek_interval ticks; with it, a trek starts whenever the
 *    season changes. The herd's split-off rule is suspended for the journey (HerdEntity.beginMigration), and hungry
 *    predators within gamerules.migration_predator_follow blocks are drawn along behind the moving herd.
 *  * TERRITORIAL BULLS: an adult male claims the spot where it settles while its herd is not migrating; a rival bull of
 *    its species inside that patch (5 blocks) sets off the alcelaphine KNEELING CLASH (ATTACK_CLASH) -- both drop to their
 *    knees and shove horns -- and the intruder then walks off. Cosmetic: no damage, no target. Treks clear territories.
 *  * Cornered, an adult lowers its head, paws the ground (ATTACK_THREATEN) and charges (MeleeAttackCharger), goring.
 */
public class EntityWildebeest extends ComplexMobTerrestrial implements INewSkins, ISpecies, IPackEntity, INeedsPostUpdate {

    public static final Animation ATTACK_GORE = Animation.create(14);
    public static final Animation ATTACK_THREATEN = Animation.create(50);
    public static final Animation EAT = Animation.create(40); // matches GrazeGoal's 40-tick eatingGrassTimer
    public static final Animation ATTACK_CLASH = Animation.create(40);
    public static final Animation DRINK = Animation.create(100);   // 2.8.0 idle pose (gen_keyframes.py)
    public static final Animation ALERT = Animation.create(40);   // 2.8.0 idle pose (gen_keyframes.py)

    /** Radius of a bull's territory, in blocks (a patch about 10 blocks across). */
    private static final double TERRITORY_RADIUS = 5.0D;

    int trekCooldown = -1;
    float trekHeading = Float.NaN;
    @Nullable
    private CompatSereneSeasons.Season lastSeason;
    @Nullable
    private BlockPos territory;
    private int clashCooldown = 600;

    public EntityWildebeest(EntityType<? extends ComplexMob> type, Level worldIn) {
        super(type, worldIn);
        this.maxUpStep = 1F;
        this.turn_speed = 0.2F;
    }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new SmartSwimGoal_Land(this));
        this.goalSelector.addGoal(1, new RetreatWhenRoutedGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new HerdFleeGoal<>(this, LivingEntity.class, 20, 1.6D, 2.0D, EcologyTags::isPredator));
        this.goalSelector.addGoal(3, new AlertGoal(this));   // 2.8.0 vigilance (below flight, never equal)
        this.goalSelector.addGoal(2, new MeleeAttackCharger(this, 1.4F, 3));
        this.goalSelector.addGoal(3, new SmartMateGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new GrazeGoal(this, 10));
        this.goalSelector.addGoal(3, new DrinkGoal(this));   // 2.8.0 cosmetic waterhole visits
        this.goalSelector.addGoal(3, new GreatMigrationGoal(this));
        this.goalSelector.addGoal(3, new HerdMigrationGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new TerritoryClashGoal(this));
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
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
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
            if (this.trekCooldown > 0) {
                this.trekCooldown--;
            }
            if (this.clashCooldown > 0) {
                this.clashCooldown--;
            }
            this.tickTerritory();
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

    // ------------------------------------------------------------------------------------------------ territorial bulls

    private boolean isBull() {
        return this.isMale() && !this.isBaby() && !this.isTame();
    }

    /** A settled bull claims the spot it stands on; a migrating herd leaves its territories behind. */
    private void tickTerritory() {
        if (!this.isBull()) {
            this.territory = null;
            return;
        }
        if (this.herd != null && this.herd.isMigrating()) {
            this.territory = null;
        }
        else if (this.territory == null && (this.tickCount + this.getId()) % 100 == 0 && this.isNotMoving() && this.getTarget() == null) {
            this.territory = this.blockPosition();
        }
    }

    boolean canClash() {
        return this.isBull() && this.clashCooldown == 0 && this.getTarget() == null && this.canMove() && !this.isSleeping()
                && !this.isSitting() && !this.isInWater() && !this.isVehicle() && this.getAnimation() == NO_ANIMATION;
    }

    // ------------------------------------------------------------------------------------------------ great migration

    /** True when this herd leader should set off on a trek: on a season change with Serene Seasons, else on the timer. */
    boolean trekDue() {
        int interval = ConfigGamerules.wildebeestTrekInterval.get();
        if (interval <= 0) {
            return false;
        }
        CompatSereneSeasons.Season season = CompatSereneSeasons.season(this.level);
        if (season != CompatSereneSeasons.Season.NONE) {
            if (this.lastSeason == null) {
                this.lastSeason = season;
                return false;
            }
            if (season != this.lastSeason) {
                this.lastSeason = season;
                return true;
            }
            return false;
        }
        if (this.trekCooldown < 0) {                      // first sight of this leader: stagger herds across the interval
            this.trekCooldown = this.random.nextInt(interval);
        }
        return this.trekCooldown == 0;
    }

    void finishTrek() {
        int interval = ConfigGamerules.wildebeestTrekInterval.get();
        this.trekCooldown = interval + this.random.nextInt(Math.max(1, interval / 3));
        this.trekHeading += 60 + this.random.nextInt(61);
    }

    /** The trek destination: along the herd's heading, shortened until it lands on loaded, dry ground. */
    @Nullable
    BlockPos pickTrekDestination() {
        if (Float.isNaN(this.trekHeading)) {
            this.trekHeading = this.random.nextFloat() * 360F;
        }
        double ang = Math.toRadians(this.trekHeading);
        BlockPos origin = this.blockPosition();
        for (int dist = ConfigGamerules.wildebeestTrekDistance.get(); dist >= 48; dist = (int) (dist * 0.75F)) {
            BlockPos p = origin.offset(Math.cos(ang) * dist, 0, Math.sin(ang) * dist);
            if (!this.level.isLoaded(p)) {
                continue;
            }
            BlockPos surface = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p);
            if (this.level.getFluidState(surface.below()).isEmpty()) {
                return surface;
            }
        }
        this.trekHeading += 90F;                          // blocked (sea, unloaded): try another way next time
        return null;
    }

    /** Hungry predators near a trekking herd are drawn along behind it, to about 14 blocks off its flank. */
    void drawPredators() {
        int r = ConfigGamerules.migrationPredatorFollow.get();
        if (r <= 0) {
            return;
        }
        List<ComplexMob> near = this.level.getEntitiesOfClass(ComplexMob.class, this.getBoundingBox().inflate(r),
                e -> e.isCarnivore() && !e.isTame() && e.getTarget() == null && e.canMove() && !e.isSleeping()
                        && e.getHunger() <= 60);   // genuinely hungry ones (the relocate goal's threshold), not nearly all of them
        for (ComplexMob p : near) {
            if (p.distanceToSqr(this) > 324.0D) {
                Vec3 away = p.position().subtract(this.position()).normalize().scale(14.0D);
                Vec3 to = this.position().add(away);
                p.getNavigation().moveTo(to.x, to.y, to.z, 1.0D);
                p.setHome(new BlockPos(to));   // or the nightly walk back to its sleeping spot undoes the follow
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ combat

    public boolean doHurtTarget(Entity entityIn) {
        boolean flag = super.doHurtTarget(entityIn);
        if (flag && this.getAnimation() == NO_ANIMATION && !this.isBaby()) {
            this.setAnimation(ATTACK_GORE);
            HoglinBase.hurtAndThrowTarget(this, (LivingEntity) entityIn);
        }
        return flag;
    }

    public boolean hurt(DamageSource damageSource, float amount) {
        performRetaliation(damageSource, this.getHealth(), amount, true);
        return super.hurt(damageSource, amount);
    }

    @Override
    public Animation getChargeAnimation() {
        return ATTACK_THREATEN;
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.COW_STEP, 0.15F, 0.9F);
    }

    @Nullable
    public EntityWildebeest getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
        return create_offspring(new EntityWildebeest(ModEntity.WILDEBEEST.get(), this.level));
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (hand == InteractionHand.MAIN_HAND && !this.level.isClientSide()) {
            if (!this.isTame() && this.isBaby() && EntityUtils.hasFullHealth(this) && this.isFood(itemstack)) {
                this.playSound(SoundEvents.COW_MILK, 1.0F, 1.2F);
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

    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("TrekCooldown", this.trekCooldown);
        if (!Float.isNaN(this.trekHeading)) {
            compound.putFloat("TrekHeading", this.trekHeading);
        }
        if (this.territory != null) {
            compound.putLong("Territory", this.territory.asLong());
        }
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("TrekCooldown")) {
            this.trekCooldown = compound.getInt("TrekCooldown");
        }
        if (compound.contains("TrekHeading")) {
            this.trekHeading = compound.getFloat("TrekHeading");
        }
        if (compound.contains("Territory")) {
            this.territory = BlockPos.of(compound.getLong("Territory"));
        }
    }

    @Override
    public Animation getAnimationDrink() { return DRINK; }
    @Override
    public Animation getAnimationAlert() { return ALERT; }
    // 2.8.0 solved DRINK reach (gen_keyframes.py): muzzle 20.3 units in front of the centre at size 1
    @Override
    public double getDrinkReach() { return 1.271D * this.getMobSize(); }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ATTACK_GORE, ATTACK_THREATEN, EAT, ATTACK_CLASH, DRINK, ALERT};
    }

    public Animation getAnimationEat() { return EAT; }

    @Override
    public void updateAttributes() {
        this.applySpeciesSpeed();
        this.applySpeciesHealthAndAttack();
        this.setHealth(this.getMaxHealth());
    }

    /** Runs on the herd LEADER only: moving the leader moves the herd (SmartWanderGoal paths the rest after it). */
    static class GreatMigrationGoal extends Goal {
        private static final int TRAVEL_BUDGET = 6000;
        private final EntityWildebeest mob;
        @Nullable
        private BlockPos destination;
        private int travelTicks;

        GreatMigrationGoal(EntityWildebeest mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        /** 2.8.0: parity-proof replacement for a tickCount % 100 gate (see GoalTimer). */
        private final untamedwilds.util.GoalTimer checkTimer = new untamedwilds.util.GoalTimer(100, true);

        @Override
        public boolean canUse() {
            if (!this.checkTimer.ready(this.mob)) {
                return false;
            }
            HerdEntity herd = this.mob.herd;
            if (herd == null || herd.getLeader() != this.mob || herd.isMigrating() || !this.mob.canMove() || this.mob.isTame()
                    || this.mob.getTarget() != null || this.mob.fleeCooldown > 0 || !EcologyMode.allowsMigration(this.mob)) {
                return false;
            }
            if (!this.mob.trekDue()) {
                return false;
            }
            this.destination = this.mob.pickTrekDestination();
            if (this.destination == null) {
                this.mob.trekCooldown = 1200;
                return false;
            }
            return true;
        }

        @Override
        public void start() {
            if (this.mob.herd != null) {
                this.mob.herd.beginMigration();
            }
            this.travelTicks = 0;
            this.path();
            if (UntamedWilds.DEBUG && this.destination != null) {
                UntamedWilds.LOGGER.info("{} is leading its herd on a trek to {}", this.mob.getName().getString(), this.destination);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.destination != null && this.travelTicks < TRAVEL_BUDGET && this.mob.herd != null && this.mob.getTarget() == null;
        }

        @Override
        public void tick() {
            this.travelTicks++;
            if (this.destination == null) {
                return;
            }
            if (this.mob.distanceToSqr(this.destination.getX() + 0.5D, this.destination.getY(), this.destination.getZ() + 0.5D) < 64D) {
                this.destination = null;
                return;
            }
            if (this.mob.getNavigation().isDone()) {
                this.path();
            }
            if (this.travelTicks % 200 == 0) {
                this.mob.drawPredators();
            }
        }

        @Override
        public void stop() {
            HerdEntity herd = this.mob.herd;
            if (herd != null && herd.isMigrating()) {
                herd.endMigration(this.mob.blockPosition());
            }
            this.destination = null;
            this.travelTicks = 0;
            this.mob.finishTrek();
        }

        private void path() {
            if (this.destination != null) {
                this.mob.getNavigation().moveTo(this.destination.getX() + 0.5D, this.destination.getY(), this.destination.getZ() + 0.5D, 1.1D);
            }
        }
    }

    /** A rival bull inside a territory: both kneel and shove horns (ATTACK_CLASH), then the intruder walks away. */
    static class TerritoryClashGoal extends Goal {
        private final EntityWildebeest self;
        @Nullable
        private EntityWildebeest rival;
        private int timer;

        TerritoryClashGoal(EntityWildebeest self) {
            this.self = self;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.self.territory == null || !this.self.canClash() || this.self.getRandom().nextInt(200) != 0) {
                return false;
            }
            List<EntityWildebeest> near = this.self.level.getEntitiesOfClass(EntityWildebeest.class,
                    new AABB(this.self.territory).inflate(TERRITORY_RADIUS),
                    e -> e != this.self && e.getVariant() == this.self.getVariant() && e.canClash());
            if (near.isEmpty()) {
                return false;
            }
            this.rival = near.get(0);
            return true;
        }

        @Override
        public void start() {
            this.timer = ATTACK_CLASH.getDuration();
            for (EntityWildebeest w : new EntityWildebeest[]{this.self, this.rival}) {
                w.getNavigation().stop();
                w.setAnimation(ATTACK_CLASH);
                w.clashCooldown = 1200 + w.getRandom().nextInt(1200);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.timer > 0 && this.rival != null && this.rival.isAlive() && this.self.getTarget() == null
                    && this.self.distanceToSqr(this.rival) < 64.0D;
        }

        @Override
        public void tick() {
            this.timer--;
            this.self.getLookControl().setLookAt(this.rival, 30.0F, 30.0F);
            this.rival.getLookControl().setLookAt(this.self, 30.0F, 30.0F);
            this.self.getNavigation().stop();
            this.rival.getNavigation().stop();
        }

        @Override
        public void stop() {
            if (this.rival != null && this.rival.isAlive() && this.self.territory != null) {
                // the loser leaves: walk the intruder 12 blocks out from the territory centre
                Vec3 centre = Vec3.atCenterOf(this.self.territory);
                Vec3 dir = this.rival.position().subtract(centre);
                dir = dir.lengthSqr() < 0.01D ? new Vec3(1, 0, 0) : dir.normalize();
                Vec3 to = centre.add(dir.scale(12.0D));
                this.rival.getNavigation().moveTo(to.x, to.y, to.z, 1.1D);
                this.rival.territory = null;
            }
            this.rival = null;
        }
    }
}
