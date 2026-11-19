package untamedwilds.compat;

import net.minecraft.world.level.Level;
import untamedwilds.UntamedWilds;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Serene Seasons bridge, by reflection -- there is no compile-time dependency, so the mod builds and
 * runs without Serene Seasons and simply reports "no season" when it is absent or its API moved.
 * <p>
 * The upstream version of this class was written against the 1.16 API and left commented out, with
 * {@link #isCurrentSeason} hard-wired to {@code true}. That stub is now INTENTIONAL and stays:
 * breeding runs all year regardless of seasons (user decision 2026-10-06), because a seasonal breeding
 * gate can push the reproduction rate low enough to crash populations. Seasons only drive the climate
 * effects in {@code untamedwilds.world.ClimateHelper} (forage regrowth, cold stress).
 */
public class CompatSereneSeasons {

    /** The four temperate seasons, plus NONE when Serene Seasons is absent or unreadable. */
    public enum Season { SPRING, SUMMER, AUTUMN, WINTER, NONE }

    private static boolean resolved = false;
    private static boolean broken = false;
    private static Method getSeasonState;
    private static Method getSeason;
    private static Method getSubSeason;

    /** Breeding is never gated by season -- see the class comment. The {@code breeding} field is unused. */
    public static boolean isCurrentSeason(Level world, String string) {
        return true;
    }

    /** The current temperate season, or {@link Season#NONE} without Serene Seasons. */
    public static Season season(@Nullable Level level) {
        Object state = state(level);
        if (state == null) {
            return Season.NONE;
        }
        try {
            String name = String.valueOf(getSeason.invoke(state));
            return Season.valueOf(name);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            fail(e);
            return Season.NONE;
        }
    }

    /** The current sub-season name (e.g. {@code EARLY_WINTER}), or null without Serene Seasons. */
    @Nullable
    public static String subSeason(@Nullable Level level) {
        Object state = state(level);
        if (state == null) {
            return null;
        }
        try {
            return String.valueOf(getSubSeason.invoke(state));
        } catch (ReflectiveOperationException e) {
            fail(e);
            return null;
        }
    }

    @Nullable
    private static Object state(@Nullable Level level) {
        if (level == null || !CompatBridge.SereneSeasons || broken) {
            return null;
        }
        if (!resolved) {
            resolve();
        }
        if (broken) {
            return null;
        }
        try {
            return getSeasonState.invoke(null, level);
        } catch (ReflectiveOperationException e) {
            fail(e);
            return null;
        }
    }

    private static void resolve() {
        resolved = true;
        try {
            Class<?> helper = Class.forName("sereneseasons.api.season.SeasonHelper");
            getSeasonState = helper.getMethod("getSeasonState", Level.class);
            Class<?> stateClass = getSeasonState.getReturnType();
            getSeason = stateClass.getMethod("getSeason");
            getSubSeason = stateClass.getMethod("getSubSeason");
            UntamedWilds.LOGGER.info("Serene Seasons climate bridge active");
        } catch (ReflectiveOperationException | LinkageError e) {
            fail(e);
        }
    }

    private static void fail(Throwable e) {
        if (!broken) {
            broken = true;
            UntamedWilds.LOGGER.warn("Serene Seasons is loaded but its season API could not be read ({}); seasonal climate effects are off", e.toString());
        }
    }
}
