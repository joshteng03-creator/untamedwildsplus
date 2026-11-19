package untamedwilds.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.config.EcologyMode;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.entity.HerdEntity;
import untamedwilds.util.EcologyTags;
import untamedwilds.util.PerfCounters;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Moves a hungry predator, and its pack, to where the prey actually is.
 * <p>
 * This is not an optional flourish -- it is what keeps predators coupled to prey once herds start
 * shifting range ({@link HerdMigrationGoal}). If the herds move and the packs do not, the packs simply
 * starve on ground that no longer holds anything to eat, and the predator extinction this whole pass
 * exists to fix comes back by a different route.
 * <p>
 * Runs on the pack leader only, for the same reason the herd version does: {@link SmartWanderGoal}
 * already paths every other member to within 7 blocks of the leader.
 */
public class PredatorRelocateGoal extends Goal {

    private static final int SEARCH_ATTEMPTS = 10;
    /* 2.8.0: each candidate's census is a 64-block entity sweep; up to ten of them ran in one tick. The
     * attempts still skip unloaded / water candidates, but at most four candidates are actually censused. */
    private static final int MAX_CENSUS_SCANS = 4;
    private static final int TRAVEL_BUDGET = 2400;
    private static final double ARRIVAL_DIST_SQR = 64D;
    /** How far a predator casts about for a new hunting ground. */
    private static final int MIN_DISTANCE = 48;
    private static final int MAX_DISTANCE = 96;
    /** Radius the predator sweeps for prey before deciding this ground is empty. */
    private static final double PREY_SCAN_RADIUS = 32D;

    private final ComplexMobTerrestrial taskOwner;
    private final double speed;
    /** Ticks this animal has been hungry with nothing to hunt in range. */
    private int droughtTicks;
    @Nullable
    private BlockPos destination;
    private int travelTicks;

    public PredatorRelocateGoal(ComplexMobTerrestrial entityIn, double speedIn) {
        this.taskOwner = entityIn;
        this.speed = speedIn;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    /** 2.8.0: parity-proof replacement for a tickCount % 40 gate (see GoalTimer). */
    private final untamedwilds.util.GoalTimer checkTimer = new untamedwilds.util.GoalTimer(40, true);

    @Override
    public boolean canUse() {
        int limit = ConfigGamerules.predatorRelocateTicks.get();
        if (limit <= 0 || !EcologyMode.allowsMigration(this.taskOwner) || !this.taskOwner.canMove()
                || this.taskOwner.isTame() || this.taskOwner.getTarget() != null) {
            return false;
        }
        HerdEntity herd = this.taskOwner.herd;
        if (herd != null && herd.getLeader() != this.taskOwner) {
            return false;
        }
        // Cheap: a check every couple of seconds, not every tick.
        if (!this.checkTimer.ready(this.taskOwner)) {
            return false;
        }
        /* Dispersal at maturity (2026-10-24): a solitary predator that has just come of age leaves home once,
         * for the best prey-per-predator ground it can find, instead of setting up beside its mother. */
        if (this.taskOwner.pendingDispersal) {
            this.taskOwner.pendingDispersal = false;
            this.dispersing = true;
            this.destination = this.findHuntingGround(DISPERSAL_MIN, DISPERSAL_MAX, true);
            return this.destination != null;
        }
        this.dispersing = false;
        // Only a predator that is actually going hungry has a reason to leave. A fed one stays put.
        if (this.taskOwner.getHunger() > 60) {
            this.droughtTicks = 0;
            return false;
        }
        /* 2026-10-24: leave when the local prey per predator stays LOW, not only when there is no prey at all.
         * Waiting for zero meant predators ate a range out before they moved; the ratio is the same number the
         * breeding gate reads, so a crowded range both stops breeding and sheds hunters. */
        boolean preyHere = this.hasPreyNearby();    // 2.8.0: one scan per check (was two)
        if (preyHere && this.taskOwner.preyPerPredator() >= ConfigGamerules.predatorRelocateRatio.get()) {
            this.droughtTicks = 0;
            return false;
        }
        this.droughtTicks += 40;
        if (this.droughtTicks < limit) {
            return false;
        }
        this.destination = this.findHuntingGround(MIN_DISTANCE, MAX_DISTANCE, !preyHere);
        if (this.destination == null) {
            this.droughtTicks = 0;      // nothing better within reach: stay, and look again later
        }
        return this.destination != null;
    }

    /** Dispersal range for a predator leaving home at maturity. */
    private static final int DISPERSAL_MIN = 64;
    private static final int DISPERSAL_MAX = 128;
    private boolean dispersing;

    @Override
    public void start() {
        this.travelTicks = 0;
        this.droughtTicks = 0;
        this.pathToDestination();
        if (UntamedWilds.DEBUG && this.destination != null) {
            UntamedWilds.LOGGER.info("{} relocated its pack toward prey at {}", this.taskOwner.getName().getString(), this.destination);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.destination != null && this.travelTicks < TRAVEL_BUDGET && this.taskOwner.getTarget() == null;
    }

    @Override
    public void tick() {
        this.travelTicks++;
        if (this.destination == null) {
            return;
        }
        if (this.taskOwner.distanceToSqr(this.destination.getX() + 0.5D, this.destination.getY(), this.destination.getZ() + 0.5D) < ARRIVAL_DIST_SQR) {
            this.settle();
            return;
        }
        if (this.taskOwner.getNavigation().isDone()) {
            this.pathToDestination();
        }
    }

    @Override
    public void stop() {
        // Settle wherever the journey ended. Even an interrupted move has taken the pack off empty
        // ground, and home has to follow or GotoSleepGoal walks it all the way back overnight.
        this.settle();
        this.travelTicks = 0;
    }

    private void settle() {
        BlockPos here = this.destination != null && this.taskOwner.distanceToSqr(this.destination.getX() + 0.5D, this.destination.getY(), this.destination.getZ() + 0.5D) < ARRIVAL_DIST_SQR
                ? this.destination : this.taskOwner.blockPosition();
        HerdEntity herd = this.taskOwner.herd;
        if (herd != null) {
            for (ComplexMob member : herd.creatureList) {
                member.setHome(here);
            }
        } else {
            this.taskOwner.setHome(here);
        }
        this.destination = null;
    }

    private void pathToDestination() {
        if (this.destination != null) {
            this.taskOwner.getNavigation().moveTo(this.destination.getX() + 0.5D, this.destination.getY(), this.destination.getZ() + 0.5D, this.speed);
        }
    }

    /** Anything this predator's diet actually lists, within scanning range. */
    private boolean hasPreyNearby() {
        return !this.nearbyPrey(this.taskOwner.blockPosition(), PREY_SCAN_RADIUS).isEmpty();
    }

    private List<LivingEntity> nearbyPrey(BlockPos around, double radius) {
        /* Prey this predator could actually take, by the same eco test the hunt goals use. Checking the
         * diet alone meant a starving tiger standing beside a deer herd it was not allowed to hunt saw
         * "prey nearby", never relocated, and starved where it stood. */
        int ownLevel = ComplexMob.getEcoLevel(this.taskOwner);
        return this.taskOwner.level.getEntitiesOfClass(LivingEntity.class, new AABB(around).inflate(radius, 16D, radius),
                candidate -> candidate != this.taskOwner && candidate.isAlive() && !EcologyTags.isPredator(candidate)
                        && EcologyTags.isPreferredPrey(this.taskOwner, candidate)
                        && ComplexMob.getEcoLevelAsPrey(candidate) < ownLevel);
    }

    /**
     * Heads toward the densest concentration of prey it can find. Falls back to any walkable ground at
     * range when nothing is detected, because a predator with no information should still move rather
     * than starve in place -- an empty search is exactly the situation this goal exists for.
     */
    /**
     * Heads for the ground with the most prey PER PREDATOR (2026-10-24; was: the most prey, which sent every
     * pack to the same herd). Only moves when a candidate beats the ratio here; {@code allowFallback} -- no
     * prey here at all, or a dispersing youngster -- takes any walkable ground at range when nothing scores,
     * because a predator with no information should still move rather than starve in place.
     */
    @Nullable
    private BlockPos findHuntingGround(int minDist, int maxDist, boolean allowFallback) {
        BlockPos origin = this.taskOwner.blockPosition();
        BlockPos best = null;
        double bestScore = this.dispersing ? 0D : this.taskOwner.preyPerPredator();
        BlockPos fallback = null;
        int censuses = 0;
        for (int i = 0; i < SEARCH_ATTEMPTS && censuses < MAX_CENSUS_SCANS; i++) {
            double angle = this.taskOwner.getRandom().nextDouble() * Math.PI * 2D;
            double dist = minDist + this.taskOwner.getRandom().nextInt(Math.max(1, maxDist - minDist));
            BlockPos candidate = this.groundAt(origin.offset(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            if (candidate == null) {
                continue;
            }
            if (fallback == null) {
                fallback = candidate;
            }
            censuses++;
            PerfCounters.inc(PerfCounters.Counter.RELOCATE_SCAN);
            float[] census = ComplexMob.countPreyAndPredators(this.taskOwner, candidate, PREY_SCAN_RADIUS);
            double score = census[0] / (census[1] + 1D);     // +1: this predator arriving
            if (census[0] > 0 && score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best != null ? best : (allowFallback ? fallback : null);
    }

    @Nullable
    private BlockPos groundAt(BlockPos pos) {
        Level level = this.taskOwner.level;
        if (!level.isLoaded(pos)) {
            return null;
        }
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
        return level.getFluidState(surface.below()).isEmpty() ? surface : null;
    }
}
