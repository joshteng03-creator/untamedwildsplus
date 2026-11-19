package untamedwilds.util;

import java.util.EnumMap;
import java.util.Map;

/**
 * Server-thread operation counters for the 2.8.0 performance work: GameTests assert on them (a hungry wolf
 * with no prey must not search every other tick) and {@code /untamedwilds perf} dumps them for the soak
 * harness. An increment is a single array write, cheaper than reading the config switch would be, so the
 * counting is always on; {@code performance.debug_perf_counters} only enables the periodic CSV dump.
 * <p>
 * Call {@link #inc} from SERVER code only -- these are plain longs with no synchronisation.
 */
public final class PerfCounters {

    public enum Counter {
        HUNT_SCAN,
        HUNT_SCAN_EMPTY,
        FLEE_SCAN,
        SHARED_THREAT_SCAN,
        FLEE_PATH,
        MATE_SCAN,
        RELOCATE_SCAN,
        ANIM_PACKET,
        DRINK_SAMPLE,
        DRINK_VISIT,
        CARCASS_SPAWN,
        CARCASS_SCAN,
        CARCASS_BITE,
        ALERT_RAISED,
        CALL_PLAYED
    }

    private static final long[] COUNTS = new long[Counter.values().length];

    private PerfCounters() {}

    public static void inc(Counter counter) {
        COUNTS[counter.ordinal()]++;
    }

    public static long get(Counter counter) {
        return COUNTS[counter.ordinal()];
    }

    public static void reset() {
        java.util.Arrays.fill(COUNTS, 0L);
    }

    public static Map<Counter, Long> snapshot() {
        Map<Counter, Long> out = new EnumMap<>(Counter.class);
        for (Counter c : Counter.values()) {
            out.put(c, COUNTS[c.ordinal()]);
        }
        return out;
    }
}
