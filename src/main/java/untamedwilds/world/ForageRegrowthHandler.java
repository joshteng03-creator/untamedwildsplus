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

    private ForageRegrowthHandler() {}

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.world instanceof ServerLevel level)) {
            return;
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
        for (int p = 0; p < level.players().size(); p++) {
            BlockPos origin = level.players().get(p).blockPosition();
            for (int i = 0; i < thisTick; i++) {
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
            }
            return;
        }
        if (groundState.is(Blocks.GRASS_BLOCK) && level.getBlockState(surface).isAir()) {
            // The plant layer is what GrazeGoal actually eats most of the time, so it is what most
            // needs putting back. Ferns only where ferns already grow, so biome character is preserved.
            if (hasNeighbour(level, surface, Blocks.FERN)) {
                level.setBlock(surface, Blocks.FERN.defaultBlockState(), 2);
            } else if (hasNeighbour(level, surface, Blocks.GRASS)) {
                level.setBlock(surface, Blocks.GRASS.defaultBlockState(), 2);
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
