package untamedwilds.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.config.EcologyMode;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.entity.HerdEntity;
import untamedwilds.init.ModTags;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Moves a herd off a range it has eaten out, or out of a neighbourhood that is too crowded, onto
 * better ground.
 * <p>
 * This is how density dependence is expressed in this mod, and it is deliberately <b>not</b> a cap on
 * births. A birth cap stops a population growing at all; what real ungulate populations do when the
 * ground will not carry them is <b>leave</b> -- herds shift range, and surplus young adults disperse
 * to found herds elsewhere. Growth continues; it just spreads out instead of stacking up. Reduced
 * fecundity is the last resort and only applies when a migration has already failed to find anywhere
 * better ({@link ComplexMob#isRangeExhausted()}).
 * <p>
 * Two structural facts make this cheap:
 * <ul>
 *   <li>{@link SmartWanderGoal#getPosition()} already paths every non-leader herd member to within
 *       7 blocks of {@code herd.getLeader()}. <b>Move the leader and the herd follows</b>, so the goal
 *       only ever runs on one animal per herd.</li>
 *   <li>{@link HerdEntity#effectiveSplitOffDistance()} suspends the 32-block split-off rule for the
 *       duration. Without that the herd sheds every straggler mid-journey and arrives as one animal.</li>
 * </ul>
 * <p>
 * <b>Engine constraint, not a design choice:</b> Minecraft only ticks entities in loaded chunks, so
 * this is range <i>shifting</i> bounded by simulation distance (~160 blocks by default), not
 * continental migration. A herd nobody is near does not move.
 */
public class HerdMigrationGoal extends Goal {

    /** Candidate destinations sampled per attempt. */
    private static final int SEARCH_ATTEMPTS = 12;
    /** Half-extent of the box each candidate is scored over, and how many blocks in it are sampled. */
    private static final int SCORE_RADIUS = 6;
    private static final int SCORE_SAMPLES = 24;
    /** Ticks allowed to reach the destination before the herd gives up and settles where it is. */
    private static final int TRAVEL_BUDGET = 2400;
    /** Close enough to count as arrived, squared. */
    private static final double ARRIVAL_DIST_SQR = 36D;
    /** How much better a candidate must score than home before it is worth uprooting the herd. */
    private static final int MIN_IMPROVEMENT = 3;

    private final ComplexMobTerrestrial taskOwner;
    private final double speed;
    @Nullable
    private BlockPos destination;
    private int travelTicks;

    public HerdMigrationGoal(ComplexMobTerrestrial entityIn, double speedIn) {
        this.taskOwner = entityIn;
        this.speed = speedIn;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    /** How often the (expensive) range assessment is allowed to run. */
    private static final int ASSESS_INTERVAL = 100;

    @Override
    public boolean canUse() {
        /* Throttled FIRST and hard. GoalSelector calls canUse every tick on every goal that is not
         * running, and both halves of the decision below scan a large box -- the crowding check sweeps
         * 96x32x96 for entities and the range score samples blocks. At once every five seconds this is
         * negligible; at once a tick it would be the most expensive thing in the mod. */
        if (this.taskOwner.tickCount % ASSESS_INTERVAL != 0) {
            return false;
        }
        HerdEntity herd = this.taskOwner.herd;
        if (herd == null || herd.getLeader() != this.taskOwner || !this.taskOwner.canMove()) {
            return false;
        }
        // Never uproot a herd that is in the middle of something. A stampede or a defence is not the
        // moment to start a hundred-block walk, and the flight goals own MOVE anyway.
        if (this.taskOwner.getTarget() != null || this.taskOwner.fleeCooldown > 0 || this.taskOwner.isTame()) {
            return false;
        }
        if (!herd.wantsToMigrate()) {
            /* Not moving the whole herd, but possibly shedding one animal. Dispersal is checked here
             * because this goal already runs exactly once per herd, on the leader -- putting it on its
             * own goal would mean every member re-deriving the same answer. */
            this.tryDisperse(herd);
            return false;
        }
        this.destination = this.findBetterRange();
        if (this.destination == null) {
            // Nowhere better within reach. THIS is what eventually suppresses breeding, and nothing
            // else does -- see ComplexMob.isRangeExhausted().
            herd.endMigration(null);
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        HerdEntity herd = this.taskOwner.herd;
        if (herd != null) {
            herd.beginMigration();
        }
        this.travelTicks = 0;
        this.pathToDestination();
        if (UntamedWilds.DEBUG && this.destination != null) {
            UntamedWilds.LOGGER.info("{} is leading its herd to a new range at {}", this.taskOwner.getName().getString(), this.destination);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.destination != null && this.travelTicks < TRAVEL_BUDGET
                && this.taskOwner.herd != null && this.taskOwner.getTarget() == null;
    }

    @Override
    public void tick() {
        this.travelTicks++;
        if (this.destination == null) {
            return;
        }
        if (this.taskOwner.distanceToSqr(this.destination.getX() + 0.5D, this.destination.getY(), this.destination.getZ() + 0.5D) < ARRIVAL_DIST_SQR) {
            this.arrive();
            return;
        }
        /* Re-issue rather than trust one moveTo to carry a hundred blocks: long paths are truncated by
         * the navigator, and a path that ends short would leave the herd standing until the budget ran
         * out. Same reason GrazeGoal.tickSeeking re-paths. */
        if (this.taskOwner.getNavigation().isDone()) {
            this.pathToDestination();
        }
    }

    @Override
    public void stop() {
        HerdEntity herd = this.taskOwner.herd;
        if (herd != null && herd.isMigrating()) {
            /* Ran out of budget or was interrupted. Settling where the herd actually stands is right --
             * it has already left the range it was starving on, and the cooldown stops it immediately
             * trying again. */
            herd.endMigration(this.taskOwner.blockPosition());
        }
        this.destination = null;
        this.travelTicks = 0;
    }

    private void arrive() {
        HerdEntity herd = this.taskOwner.herd;
        if (herd != null) {
            herd.endMigration(this.destination);
        }
        this.destination = null;
    }

    private void pathToDestination() {
        if (this.destination != null) {
            this.taskOwner.getNavigation().moveTo(this.destination.getX() + 0.5D, this.destination.getY(), this.destination.getZ() + 0.5D, this.speed);
        }
    }

    /**
     * Picks the best range within reach, or null when nothing beats where the herd already is.
     * <p>
     * Candidates are drawn from an annulus so the herd cannot "migrate" ten blocks and end up grazing
     * the same patch it just stripped, and are scored on food present minus conspecifics already there
     * -- an untouched meadow with a rival herd standing on it is not an improvement.
     */
    @Nullable
    private BlockPos findBetterRange() {
        int min = ConfigGamerules.migrationMinDistance.get();
        int max = Math.max(min + 1, ConfigGamerules.migrationMaxDistance.get());
        BlockPos origin = this.taskOwner.blockPosition();
        int homeScore = this.scoreRange(origin);

        BlockPos best = null;
        int bestScore = homeScore + MIN_IMPROVEMENT;
        for (int i = 0; i < SEARCH_ATTEMPTS; i++) {
            double angle = this.taskOwner.getRandom().nextDouble() * Math.PI * 2D;
            double dist = min + this.taskOwner.getRandom().nextInt(max - min);
            BlockPos candidate = this.groundAt(origin.offset(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            if (candidate == null) {
                continue;
            }
            int score = this.scoreRange(candidate);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /** Food available around {@code pos}, less the animals of this species already eating it. */
    private int scoreRange(BlockPos pos) {
        Level level = this.taskOwner.level;
        int food = 0;
        for (int i = 0; i < SCORE_SAMPLES; i++) {
            BlockPos sample = pos.offset(
                    this.taskOwner.getRandom().nextInt(SCORE_RADIUS * 2) - SCORE_RADIUS,
                    this.taskOwner.getRandom().nextInt(4) - 2,
                    this.taskOwner.getRandom().nextInt(SCORE_RADIUS * 2) - SCORE_RADIUS);
            // Unloaded chunks read as air, which would score every distant candidate at zero. Skipping
            // them means an unloaded direction is simply never chosen, which is the correct outcome.
            if (!level.isLoaded(sample)) {
                return Integer.MIN_VALUE;
            }
            if (level.getBlockState(sample).is(ModTags.ModBlockTags.GRAZEABLE_BLOCKS)
                    || level.getBlockState(sample.below()).getBlock() == Blocks.GRASS_BLOCK) {
                food++;
            }
        }
        int competitors = 0;
        for (ComplexMob other : level.getEntitiesOfClass(ComplexMob.class, new AABB(pos).inflate(SCORE_RADIUS * 2D))) {
            if (other.getType() == this.taskOwner.getType() && other.getVariant() == this.taskOwner.getVariant()
                    && !other.isBaby() && (this.taskOwner.herd == null || !this.taskOwner.herd.containsCreature(other))) {
                competitors++;
            }
        }
        return food - competitors * 2;
    }

    /** Drops a candidate onto the surface, or null when there is nowhere to stand. */
    @Nullable
    private BlockPos groundAt(BlockPos pos) {
        Level level = this.taskOwner.level;
        if (!level.isLoaded(pos)) {
            return null;
        }
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
        return level.getFluidState(surface.below()).isEmpty() ? surface : null;
    }

    /**
     * Sends one young adult out of a full, crowded herd to found its own elsewhere.
     * <p>
     * This is the valve that keeps a population growing while local density self-limits. It is also the
     * only way this mod has of colonising new ground once natural spawning has done its initial pass.
     */
    private void tryDisperse(HerdEntity herd) {
        if (!EcologyMode.allowsMigration(this.taskOwner)) {
            return;
        }
        ComplexMob disperser = herd.pickDisperser();
        if (disperser == null) {
            return;
        }
        BlockPos target = this.findBetterRange();
        herd.detachDisperser(disperser);
        if (target != null) {
            disperser.setHome(target);
            disperser.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, this.speed);
        }
        if (UntamedWilds.DEBUG) {
            UntamedWilds.LOGGER.info("{} dispersed from a full herd toward {}", disperser.getName().getString(), target);
        }
    }
}
