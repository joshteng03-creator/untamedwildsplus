package untamedwilds.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import untamedwilds.compat.CompatSereneSeasons;
import untamedwilds.compat.CompatSereneSeasons.Season;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ComplexMob;
import untamedwilds.util.SpeciesDataHolder;

/**
 * Climate and weather as the animals feel them: Minecraft's own weather everywhere, plus Serene
 * Seasons' seasons when that mod is present (2026-10-06).
 * <p>
 * Deliberately NOT a breeding gate -- breeding runs all year (see {@link CompatSereneSeasons}). What
 * the climate changes is food and cost: how fast grazed ground grows back, and how much extra hunger
 * an animal that is not built for the cold burns through a cold snap.
 * <p>
 * Seasons only touch temperate biomes. Serene Seasons itself gives hot biomes a wet/dry cycle rather
 * than a winter, and a savanna herd starving through "winter" would be wrong, so anything at or above
 * {@link #TROPICAL_TEMPERATURE} ignores the temperate season.
 */
public final class ClimateHelper {

    /** Base temperature from which a biome is treated as tropical (no temperate winter). */
    public static final float TROPICAL_TEMPERATURE = 0.8F;

    private ClimateHelper() {}

    /** Precipitation is falling as snow on this exact spot (open sky, cold enough). */
    public static boolean isSnowingAt(Level level, BlockPos pos) {
        if (!level.isRaining() || !level.canSeeSky(pos)) {
            return false;
        }
        return !level.getBiome(pos).value().warmEnoughToRain(pos);
    }

    /** Rain (not snow) is falling on this exact spot. */
    public static boolean isRainingAt(Level level, BlockPos pos) {
        return level.isRainingAt(pos);
    }

    /** The temperate season at this spot, or NONE in tropical biomes and without Serene Seasons. */
    public static Season seasonAt(Level level, BlockPos pos) {
        Season season = CompatSereneSeasons.season(level);
        if (season == Season.NONE) {
            return Season.NONE;
        }
        Biome biome = level.getBiome(pos).value();
        return biome.getBaseTemperature() >= TROPICAL_TEMPERATURE ? Season.NONE : season;
    }

    /**
     * Multiplier on forage regrowth at this spot. Rain speeds regrowth, snow stops it, and with Serene
     * Seasons spring flushes, autumn slows and winter halves it (temperate biomes only).
     */
    public static double forageRegrowthFactor(Level level, BlockPos pos) {
        if (!ConfigGamerules.climateEffects.get()) {
            return 1.0D;
        }
        if (isSnowingAt(level, pos)) {
            return 0.0D;
        }
        double factor = isRainingAt(level, pos) ? ConfigGamerules.rainRegrowthBoost.get() : 1.0D;
        switch (seasonAt(level, pos)) {
            case SPRING -> factor *= ConfigGamerules.springRegrowthBoost.get();
            case AUTUMN -> factor *= 0.75D;
            case WINTER -> factor *= ConfigGamerules.winterRegrowthFactor.get();
            default -> { }
        }
        return factor;
    }

    /**
     * Whether this animal is under cold stress right now: snow falling on it, or a temperate winter
     * (Serene Seasons). Cold-adapted species never are.
     */
    public static boolean isColdStressed(ComplexMob mob) {
        if (isColdAdapted(mob)) {
            return false;
        }
        BlockPos pos = mob.blockPosition();
        return isSnowingAt(mob.level, pos) || seasonAt(mob.level, pos) == Season.WINTER;
    }

    /** Cold-adapted per species: see {@link SpeciesDataHolder#isColdAdapted()}. */
    public static boolean isColdAdapted(ComplexMob mob) {
        var data = ComplexMob.getEntityData(mob.getType());
        if (data == null || mob.getVariant() < 0 || mob.getVariant() >= data.getSpeciesData().size()) {
            return false;
        }
        return data.getSpeciesData().get(mob.getVariant()).isColdAdapted();
    }
}
