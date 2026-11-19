package untamedwilds.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.config.EcologyMode;

import java.util.Random;

/**
 * Regrows grazed ground, on an ecological timescale rather than vanilla's.
 * <p>
 * This is the other half of {@code grazer_griefing}, and without it the whole forage carrying-capacity
 * mechanism is a one-way ratchet. {@code GrazeGoal} destroys the grazed plant and converts grass_block
 * to dirt; vanilla only ever puts either back through {@code SpreadingSnowyDirtBlock}'s random tick,
 * which is far slower than a herd eats. A world left running therefore converts permanently to bare
 * dirt, herds migrate away from ground that will never recover, and the carrying capacity of the whole
 * map only ever falls. The intended cycle is the real one: a herd eats a patch down, moves on, and the
 * old range grows back behind it.
 * <p>
 * Sampling follows players rather than walking the chunk map. Minecraft only ticks loaded chunks, so
 * player-adjacent ground is the only ground where an animal could have eaten anything -- and it keeps
 * the cost proportional to the number of players instead of to world size.
 */
@Mod.EventBusSubscriber(modid = UntamedWilds.MOD_ID)
public class ForageRegrowthHandler {

    /** Horizontal radius around a player that regrowth is sampled within. */
    private static final int SAMPLE_RADIUS = 96;
    /** Light level a block needs before anything will grow on it, matching vanilla grass spread. */
    private static final int MIN_LIGHT = 4;
    /** How far a bare column will look for a living neighbour to spread from. */
    private static final int SPREAD_SEARCH = 2;

    /** Soak hook: how often the forage report is logged (half an in-game day). */
    private static final long REPORT_TICKS = 12000L;
    private static long regrownGround, regrownPlants;

    private ForageRegrowthHandler() {}

    /** Centre of the dimension's force-loaded chunks, at its surface; null when nothing is force-loaded. */
    private static BlockPos forcedCentre(ServerLevel level) {
        it.unimi.dsi.fastutil.longs.LongSet forced = level.getForcedChunks();
        if (forced.isEmpty()) {
            return null;
        }
        long sx = 0, sz = 0;
        for (long c : forced) {
            sx += net.minecraft.world.level.ChunkPos.getX(c);
            sz += net.minecraft.world.level.ChunkPos.getZ(c);
        }
        int x = (int) (sx * 16 / forced.size()) + 8, z = (int) (sz * 16 / forced.size()) + 8;
        return new BlockPos(x, level.getSeaLevel(), z);
    }

    /**
     * Soak hook: blocks regrown since the last report, and a sample of the force-loaded area's ground cover -- grass with a
     * plant on it (forage), bare grass, dirt -- so a soak log shows directly whether the herds are eating it bare.
     */
    private static void report(ServerLevel level, Random random) {
        BlockPos c = forcedCentre(level);
        if (c == null) {
            return;
        }
        int plant = 0, bare = 0, dirt = 0, other = 0;
        for (int i = 0; i < 400; i++) {
            BlockPos column = c.offset(random.nextInt(SAMPLE_RADIUS * 2) - SAMPLE_RADIUS, 0, random.nextInt(SAMPLE_RADIUS * 2) - SAMPLE_RADIUS);
            if (!level.isLoaded(column)) {
                continue;
            }
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            BlockState g = level.getBlockState(surface.below()), top = level.getBlockState(surface);
            if (g.is(Blocks.GRASS_BLOCK)) {
                if (top.is(Blocks.GRASS) || top.is(Blocks.FERN) || top.is(Blocks.TALL_GRASS) || top.is(Blocks.LARGE_FERN)) {
                    plant++;
                } else {
                    bare++;
                }
            } else if (g.is(Blocks.DIRT) || g.is(Blocks.COARSE_DIRT)) {
                dirt++;
            } else {
                other++;
            }
        }
        UntamedWilds.LOGGER.info("[soak] forage day {}: regrown {} grass + {} plants since last report; ground cover plant {} / bare grass {} / dirt {} / other {}",
                String.format("%.1f", level.getGameTime() / 24000.0), regrownGround, regrownPlants, plant, bare, dirt, other);
        regrownGround = 0;
        regrownPlants = 0;
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.world instanceof ServerLevel level)) {
            return;
        }
        // soak hook report: checked before the per-tick early returns, which skip most ticks
        if (untamedwilds.config.ConfigPerformance.debugRegrowForcedChunks.get() && level.getGameTime() % REPORT_TICKS == 0
                && level.players().isEmpty()) {
            report(level, level.getRandom());
        }
        int interval = ConfigGamerules.forageRegrowthInterval.get();
        int attempts = ConfigGamerules.forageRegrowthAttempts.get();
        if (interval <= 0 || attempts <= 0) {
            return;
        }
        /* Spread each interval's attempts evenly across its ticks instead of firing all of them on one:
         * 32 attempts per player at up to ~150 block reads each was a ~5000-lookup spike every 200 ticks.
         * The per-interval total is unchanged. */
        long phase = level.getGameTime() % interval;
        int thisTick = (int) ((phase + 1) * attempts / interval - phase * attempts / interval);
        if (thisTick <= 0) {
            return;
        }
        /* Both gates are deliberate. Zoo mode has no forage depletion to undo, and if grazing is not
         * allowed to destroy blocks then nothing has been eaten and there is nothing to grow back --
         * regrowing grass a herd never removed would just be the mod terraforming the player's world. */
        if (!EcologyMode.allowsForageDepletion(level) || !ConfigGamerules.grazerGriefing.get()) {
            return;
        }
        Random random = level.getRandom();
        java.util.List<BlockPos> origins = new java.util.ArrayList<>();
        for (int p = 0; p < level.players().size(); p++) {
            origins.add(level.players().get(p).blockPosition());
        }
        if (origins.isEmpty() && untamedwilds.config.ConfigPerformance.debugRegrowForcedChunks.get()) {
            BlockPos stand = forcedCentre(level);
            if (stand != null) {
                origins.add(stand);           // soak hook: one virtual player at the centre of the force-loaded area
            }
        }
        for (BlockPos origin : origins) {
            /* Climate scales the local rate (ClimateHelper): rain speeds regrowth, snow stops it, and
             * with Serene Seasons spring flushes and winter halves it. The fractional part is rolled so
             * a 0.5x winter really is half the attempts on average, not zero. */
            double want = thisTick * ClimateHelper.forageRegrowthFactor(level, origin);
            int n = (int) want + (random.nextDouble() < want - (int) want ? 1 : 0);
            for (int i = 0; i < n; i++) {
                tryRegrow(level, random, origin);
            }
        }
    }

    /** One attempt: pick a surface column near {@code origin} and put back whatever is missing. */
    private static void tryRegrow(ServerLevel level, Random random, BlockPos origin) {
        BlockPos column = origin.offset(random.nextInt(SAMPLE_RADIUS * 2) - SAMPLE_RADIUS, 0, random.nextInt(SAMPLE_RADIUS * 2) - SAMPLE_RADIUS);
        // Only ever touches ground a player is actually near enough to have loaded.
        if (!level.isLoaded(column)) {
            return;
        }
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        BlockPos ground = surface.below();
        if (level.getMaxLocalRawBrightness(surface) < MIN_LIGHT) {
            return;
        }
        BlockState groundState = level.getBlockState(ground);
        if (groundState.is(Blocks.DIRT) || groundState.is(Blocks.COARSE_DIRT)) {
            // Grass creeps back from living ground, exactly as vanilla spread does -- an isolated dirt
            // patch in a desert must not spontaneously turn green.
            if (hasNeighbour(level, ground, Blocks.GRASS_BLOCK)) {
                level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                regrownGround++;
            }
            return;
        }
        if (groundState.is(Blocks.GRASS_BLOCK) && level.getBlockState(surface).isAir()) {
            // The plant layer is what GrazeGoal actually eats most of the time, so it is what most
            // needs putting back. Ferns only where ferns already grow, so biome character is preserved.
            if (hasNeighbour(level, surface, Blocks.FERN)) {
                level.setBlock(surface, Blocks.FERN.defaultBlockState(), 2);
                regrownPlants++;
            } else if (hasNeighbour(level, surface, Blocks.GRASS)) {
                level.setBlock(surface, Blocks.GRASS.defaultBlockState(), 2);
                regrownPlants++;
            }
        }
    }

    /** Whether {@code block} occurs within {@link #SPREAD_SEARCH} of {@code pos}, at the same level or one either side. */
    private static boolean hasNeighbour(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        /* The column itself was checked, but a column at the edge of the loaded area has neighbours in
         * a chunk that is not, and getBlockState on the server loads it synchronously. */
        if (!level.hasChunksAt(pos.getX() - SPREAD_SEARCH, pos.getZ() - SPREAD_SEARCH, pos.getX() + SPREAD_SEARCH, pos.getZ() + SPREAD_SEARCH)) {
            return false;
        }
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-SPREAD_SEARCH, -1, -SPREAD_SEARCH), pos.offset(SPREAD_SEARCH, 1, SPREAD_SEARCH))) {
            if (level.getBlockState(candidate).is(block)) {
                return true;
            }
        }
        return false;
    }
}
