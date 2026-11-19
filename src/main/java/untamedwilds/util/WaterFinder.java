package untamedwilds.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Finds a drinkable bank: a WATER SOURCE block with open air above it, beside a solid block an animal can stand on
 * (sturdy top, no fluid, two blocks of air above) at the same height (2.8.0, P5).
 * <p>
 * Cost is bounded and never touches an unloaded chunk: {@code n} random surface columns, each one heightmap read and a
 * handful of block-state reads. Never a cube scan -- this runs for herds, and herds are many.
 */
public final class WaterFinder {

    private WaterFinder() {}

    /** A bank to drink from: the water block, the block the animal stands ON, and the direction from it to the water. */
    public static final class Spot {
        public final BlockPos water;
        public final BlockPos bank;
        public final Direction toWater;

        Spot(BlockPos water, BlockPos bank, Direction toWater) {
            this.water = water;
            this.bank = bank;
            this.toWater = toWater;
        }
    }

    @Nullable
    public static Spot sampleBank(Level level, BlockPos origin, int radius, int samples, Random random) {
        for (int i = 0; i < samples; i++) {
            PerfCounters.inc(PerfCounters.Counter.DRINK_SAMPLE);
            BlockPos column = origin.offset(random.nextInt(radius * 2 + 1) - radius, 0, random.nextInt(radius * 2 + 1) - radius);
            if (!level.isLoaded(column)) {
                continue;
            }
            BlockPos water = surfaceWater(level, column);
            if (water == null) {
                continue;
            }
            Spot spot = bankAt(level, water, random);
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }

    /**
     * The nearest drinkable bank within {@code radius}, probing a {@code step}-spaced grid of surface columns (one
     * heightmap read each; ~289 for radius 24 / step 3). Random samples miss a 6x6 pond most of the time; a grid with
     * step 3 cannot miss water three blocks wide. Water found away from any bank (mid-lake) is followed back toward
     * the animal to its shore. Bounded, and never touches an unloaded chunk.
     */
    @Nullable
    public static Spot findBank(Level level, BlockPos origin, int radius, int step, Random random) {
        Spot best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                double d = (double) dx * dx + (double) dz * dz;
                if (d > (double) radius * radius || d >= bestDist) {
                    continue;
                }
                BlockPos column = origin.offset(dx, 0, dz);
                if (!level.isLoaded(column)) {
                    continue;
                }
                PerfCounters.inc(PerfCounters.Counter.DRINK_SAMPLE);
                BlockPos top = surfaceWater(level, column);
                if (top == null) {
                    continue;
                }
                Spot spot = bankAt(level, top, random);
                if (spot == null) {
                    spot = shoreToward(level, top, origin, random);
                }
                if (spot != null) {
                    double sd = spot.bank.distSqr(origin);
                    if (sd < bestDist) {
                        bestDist = sd;
                        best = spot;
                    }
                }
            }
        }
        return best;
    }

    /**
     * Open water in this column NEAR THE ANIMAL'S OWN HEIGHT (two above to five below its feet): the top block of a
     * water body with air over it. Deliberately not a heightmap read -- water 20 blocks down a cliff or in a ravine is
     * not somewhere a grazer can drink anyway, and heightmaps can lag behind placed blocks (they do in the GameTest
     * world). Eight block reads.
     */
    @Nullable
    private static BlockPos surfaceWater(Level level, BlockPos column) {
        for (int dy = 2; dy >= -5; dy--) {
            BlockPos p = column.above(dy);
            if (level.getFluidState(p).is(FluidTags.WATER)) {
                return level.getBlockState(p.above()).isAir() ? p : null;
            }
        }
        return null;
    }

    /** From open water, walk toward {@code origin} (up to 12 blocks) until the water meets a bank. */
    @Nullable
    private static Spot shoreToward(Level level, BlockPos water, BlockPos origin, Random random) {
        BlockPos at = water;
        for (int i = 0; i < 12; i++) {
            int sx = Integer.signum(origin.getX() - at.getX()), sz = Integer.signum(origin.getZ() - at.getZ());
            if (sx == 0 && sz == 0) {
                return null;
            }
            BlockPos next = Math.abs(origin.getX() - at.getX()) >= Math.abs(origin.getZ() - at.getZ()) ? at.offset(sx, 0, 0) : at.offset(0, 0, sz);
            if (!level.isLoaded(next)) {
                return null;
            }
            if (!level.getFluidState(next).is(FluidTags.WATER)) {
                return bankAt(level, at, random);
            }
            at = next;
        }
        return null;
    }

    /** The bank beside {@code water}, if it is a drinkable source block; null otherwise. */
    @Nullable
    public static Spot bankAt(Level level, BlockPos water, Random random) {
        if (!level.isLoaded(water)) {
            return null;
        }
        FluidState fluid = level.getFluidState(water);
        if (!fluid.is(FluidTags.WATER) || !fluid.isSource() || !level.getBlockState(water.above()).isAir()) {
            return null;
        }
        int start = random.nextInt(4);
        for (int k = 0; k < 4; k++) {
            Direction dir = Direction.from2DDataValue((start + k) & 3);
            BlockPos bank = water.relative(dir);
            if (!level.isLoaded(bank)) {
                continue;
            }
            if (level.getBlockState(bank).isFaceSturdy(level, bank, Direction.UP) && level.getFluidState(bank).isEmpty()
                    && level.getBlockState(bank.above()).isAir() && level.getBlockState(bank.above(2)).isAir()) {
                return new Spot(water, bank, dir.getOpposite());
            }
        }
        return null;
    }
}
