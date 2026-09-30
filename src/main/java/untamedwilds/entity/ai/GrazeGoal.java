package untamedwilds.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.init.ModTags;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.EnumSet;

/**
 * Eats a grazeable block in front of the animal, and -- when there is nothing to eat where it stands --
 * walks it to the nearest patch there is.
 * <p>
 * The walking half used to be dead code, which is why herbivores dropped onto sand or into a
 * grazed-out clearing simply stood there until they starved. {@code locateGrazeables} tested
 * {@code testpos}, the fixed spot in front of the animal, instead of the candidate position it had
 * just generated, so it could only ever succeed when the animal did not need it; its second clause
 * demanded {@code getWalkTargetValue < 0}, which is a *penalised* block and essentially never true;
 * and {@code canUse} issued a {@code moveTo} and then returned false, so the goal never held the MOVE
 * flag and SmartWanderGoal re-pathed the animal on the very next tick.
 * <p>
 * The goal now runs in two phases. SEEKING owns MOVE while it walks to a located patch; EATING is the
 * original in-place behaviour, unchanged, and is still what happens whenever there is already food in
 * front of the animal.
 */
public class GrazeGoal extends Goal {

    /** Half-extent of the search box. Small enough to stay cheap, wide enough to leave a bare patch. */
    private static final int SEARCH_RADIUS = 16;
    private static final int SEARCH_HEIGHT = 4;
    private static final int SEARCH_ATTEMPTS = 24;
    /** Surface columns probed to judge how much food is left within reach. See {@link #sampleForageDensity}. */
    private static final int FORAGE_SAMPLES = 16;
    /** Ticks allowed to reach a located patch before giving up, so an unreachable one cannot pin the goal. */
    private static final int TRAVEL_BUDGET = 400;
    /** Distance at which the animal is close enough to start eating, squared. */
    private static final double ARRIVAL_DIST_SQR = 4.0D;

    private final ComplexMobTerrestrial taskOwner;
    public final Level entityWorld;
    public BlockPos testpos;
    /** Non-null only while SEEKING: the patch being walked to. Null means EATING. */
    @Nullable
    private BlockPos targetPos;
    private int travelTicks;
    private int eatingGrassTimer;
    private final int executionChance;
    /** Grazeable surface columns counted when the current mouthful began. Decides what that mouthful is worth. */
    private int forageDensity = FORAGE_SAMPLES;

    public GrazeGoal(ComplexMobTerrestrial entityIn, int chance) {
        this.taskOwner = entityIn;
        this.entityWorld = entityIn.level;
        this.executionChance = chance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!this.taskOwner.canMove() || this.taskOwner.isBaby() || this.taskOwner.getHunger() > 100 || this.taskOwner.getTarget() != null || this.taskOwner.getRandom().nextInt(executionChance) != 0) {
            return false;
        }
        this.testpos = this.taskOwner.blockPosition().offset(Math.cos(Math.toRadians(this.taskOwner.getYRot()+ 90)) * 1.2, 0, Math.sin(Math.toRadians(this.taskOwner.getYRot() + 90)) * 1.2);
        if (this.isGrazeable(this.testpos)) {
            this.targetPos = null;
            return true;
        }
        // Only go looking once actually hungry -- a well-fed animal grazes opportunistically where it
        // happens to be standing, and should not march across the map for a better patch.
        if (this.taskOwner.getHunger() < 40) {
            BlockPos pos = this.locateGrazeables();
            if (pos != null) {
                this.targetPos = pos;
                this.travelTicks = 0;
                return true;
            }
            /* Hungry, and nothing edible within sixteen blocks. This is the signal that the range is
             * spent, and enough of it sends the whole herd looking for new ground (HerdMigrationGoal).
             * The step is the goal's own execution chance, so it accrues at roughly one per attempt
             * rather than one per tick. */
            this.taskOwner.addForageStress(this.executionChance * 4);
        }
        // Nothing within reach and nothing found: fall through so the wander goals keep the animal
        // moving. Standing still is what made this starve animals in the first place.
        return false;
    }

    @Override
    public void start() {
        if (this.targetPos == null) {
            this.beginEating();
        }
        else {
            this.pathToTarget();
        }
    }

    @Override
    public void stop() {
        this.eatingGrassTimer = 0;
        this.targetPos = null;
        this.travelTicks = 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPos != null) {
            return this.travelTicks < TRAVEL_BUDGET;
        }
        return this.eatingGrassTimer > 0;
    }

    @Override
    public void tick() {
        if (this.targetPos != null) {
            this.tickSeeking();
            return;
        }
        this.eatingGrassTimer = Math.max(0, this.eatingGrassTimer - 1);
        if (this.eatingGrassTimer == 4) {
            if (this.entityWorld.getBlockState(this.testpos).is(ModTags.ModBlockTags.GRAZEABLE_BLOCKS)) {
                if (net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.entityWorld, this.taskOwner) && ConfigGamerules.grazerGriefing.get()) {
                    this.entityWorld.destroyBlock(this.testpos, false);
                }
                this.taskOwner.addHunger(this.taskOwner.getForageYield(this.forageDensity));
                // A mouthful is evidence the range still feeds this animal.
                this.taskOwner.addForageStress(-200);
                this.taskOwner.ate();
            } else {
                BlockPos blockpos1 = this.testpos.below();
                if (this.entityWorld.getBlockState(blockpos1).getBlock() == Blocks.GRASS_BLOCK) {
                    if (net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.entityWorld, this.taskOwner)) {
                        this.entityWorld.globalLevelEvent(2001, blockpos1, Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
                        if (ConfigGamerules.grazerGriefing.get()) {
                            this.entityWorld.setBlock(blockpos1, Blocks.DIRT.defaultBlockState(), 2);
                        }
                    }
                    this.taskOwner.addHunger(16);
                    // A mouthful is evidence the range still feeds this animal.
                    this.taskOwner.addForageStress(-200);
                    this.taskOwner.ate();
                }
            }
        }
    }

    /** Walks toward the located patch, and switches to EATING on arrival. */
    private void tickSeeking() {
        this.travelTicks++;
        BlockPos target = this.targetPos;
        // Someone else got there first, or the block was destroyed en route.
        if (!this.isGrazeable(target)) {
            this.targetPos = null;
            return;
        }
        this.taskOwner.getLookControl().setLookAt(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        if (this.taskOwner.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) < ARRIVAL_DIST_SQR) {
            this.testpos = target;
            this.targetPos = null;
            this.beginEating();
            return;
        }
        // Re-issue rather than assume one moveTo carries the whole way: long paths get truncated, and a
        // path that finishes short of the patch would otherwise leave the animal stalled until the
        // travel budget ran out.
        if (this.taskOwner.getNavigation().isDone()) {
            this.pathToTarget();
        }
    }

    private void pathToTarget() {
        if (this.targetPos != null) {
            this.taskOwner.getNavigation().moveTo(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D, 1);
        }
    }

    private void beginEating() {
        this.forageDensity = this.sampleForageDensity();
        this.eatingGrassTimer = 40;
        this.entityWorld.broadcastEntityEvent(this.taskOwner, (byte)10);
        this.taskOwner.getNavigation().stop();
        this.taskOwner.setAnimation(this.taskOwner.getAnimationEat());
    }

    /**
     * How many of {@link #FORAGE_SAMPLES} surface columns within the search radius still hold something
     * edible. This is the carrying capacity measurement: a mouthful taken on rich ground is worth
     * {@code forage_yield_max}, one taken on ground the herd has already eaten down is worth
     * {@code forage_yield_min}, and the gap between them is what finally makes forage a finite resource.
     * <p>
     * Grazing previously paid a flat 16 anywhere at all, so one mouthful per ~1600 ticks sustained an
     * animal no matter how bare its range was -- herds had no carrying capacity, ate the landscape to
     * dirt, and went on breeding on it.
     * <p>
     * Samples the HEIGHTMAP column rather than a random point in a box: a naive 3D probe mostly lands in
     * open air or inside the ground and would read even intact grassland as barren.
     */
    private int sampleForageDensity() {
        Random random = this.taskOwner.getRandom();
        BlockPos origin = this.taskOwner.blockPosition();
        int found = 0;
        for (int i = 0; i < FORAGE_SAMPLES; ++i) {
            BlockPos column = origin.offset(random.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS, 0, random.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS);
            BlockPos surface = this.entityWorld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (this.isGrazeable(surface)) {
                found++;
            }
        }
        return found;
    }

    /** Nearest grazeable position within the search box, or null if there is nothing to walk to. */
    @Nullable
    private BlockPos locateGrazeables() {
        Random random = this.taskOwner.getRandom();
        BlockPos blockpos = this.taskOwner.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for(int i = 0; i < SEARCH_ATTEMPTS; ++i) {
            BlockPos blockpos1 = blockpos.offset(random.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS, random.nextInt(SEARCH_HEIGHT * 2) - SEARCH_HEIGHT, random.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS);
            // Test the CANDIDATE, not testpos. Testing testpos here meant this could only ever return a
            // position when the animal was already standing on food, and returned null in exactly the
            // situation the search exists for.
            if (this.isGrazeable(blockpos1)) {
                double dist = blockpos.distSqr(blockpos1);
                if (dist < bestDist) {
                    best = blockpos1;
                    bestDist = dist;
                }
            }
        }

        return best;
    }

    /**
     * Whether there is something to eat at {@code pos}. Override this, and not the no-argument form, to
     * give a species a different diet -- the position search calls this one.
     */
    public boolean isGrazeable(BlockPos pos) {
        return (this.entityWorld.getBlockState(pos).is(ModTags.ModBlockTags.GRAZEABLE_BLOCKS) || this.entityWorld.getBlockState(pos.below()).getBlock() == Blocks.GRASS_BLOCK);
    }

    public boolean isGrazeable() {
        return this.isGrazeable(this.testpos);
    }
}
