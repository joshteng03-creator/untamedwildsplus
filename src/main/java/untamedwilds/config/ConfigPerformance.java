package untamedwilds.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Performance switches (2.8.0). Every entry either changes nothing a player can observe, or -- where it
 * could -- defaults to the old behaviour. They double as A/B switches for the soak harness
 * ({@code scripts/soak}), which is why even the "obviously better" ones can be turned off.
 */
public class ConfigPerformance {

    public static ForgeConfigSpec.BooleanValue trackingOnlyAnimationPackets;
    public static ForgeConfigSpec.BooleanValue sharedHerdThreatScan;
    public static ForgeConfigSpec.IntValue panicPathSpreadTicks;
    public static ForgeConfigSpec.IntValue huntScanBackoffMaxTicks;
    public static ForgeConfigSpec.BooleanValue countTowardMobCap;
    public static ForgeConfigSpec.DoubleValue trackingRangeMultiplier;
    public static ForgeConfigSpec.BooleanValue debugPerfCounters;
    public static ForgeConfigSpec.BooleanValue debugRegrowForcedChunks;

    ConfigPerformance(final ForgeConfigSpec.Builder builder) {
        builder.comment("Performance");

        trackingOnlyAnimationPackets = builder.comment("Send Untamed Wilds animation-start packets only to players who can see the animal, instead of Citadel's default of every player on the server. Identical in game; less network traffic on busy servers.")
                .define("performance.tracking_only_animation_packets", true);
        sharedHerdThreatScan = builder.comment("Herds look for predators with ONE entity search per herd every 10 ticks instead of one per member. Each animal still applies its own range and line-of-sight test, so who spots what is unchanged.")
                .define("performance.shared_herd_threat_scan", false);
        panicPathSpreadTicks = builder.comment("When a herd panics, spread its members' flee pathfinding over this many ticks instead of computing every path in the same tick. Every animal still turns and starts running at once. 0 = the old behaviour.")
                .defineInRange("performance.panic_path_spread_ticks", 8, 0, 40);
        huntScanBackoffMaxTicks = builder.comment("A hungry predator that found no prey waits longer before each new search (10, 20, 40 ... up to this many ticks). The wait resets after a kill or when it starts starving.")
                .defineInRange("performance.hunt_scan_backoff_max_ticks", 200, 10, 2400);
        countTowardMobCap = builder.comment("Whether Untamed Wilds animals count toward the vanilla CREATURE mob cap. true = unchanged behaviour. false = they no longer suppress vanilla/other mods' natural creature spawning (and skip vanilla's per-tick despawn player search; they never despawn either way).")
                .define("performance.count_toward_vanilla_mob_cap", true);
        trackingRangeMultiplier = builder.comment("Multiplies how far away clients are sent each animal (by size class: tiny 6 chunks, fish 4, land 10, huge 14 -- vanilla animals use 10). Raise it on servers with a large view distance if animals should stay visible further out. Needs a restart.")
                .defineInRange("performance.tracking_range_multiplier", 1.0D, 0.25D, 8.0D);
        debugPerfCounters = builder.comment("Debug: count expensive operations (entity searches, pathfinds, animation packets) for /untamedwilds perf. Off costs one boolean check per site.")
                .define("performance.debug_perf_counters", false);
        debugRegrowForcedChunks = builder.comment("TEST HOOK for headless soak servers -- leave false in real worlds. Forage regrowth follows players, so a server with nobody online slowly eats its force-loaded area bare and herbivores starve whatever the AI does. With this on and no player in a dimension, the force-loaded area stands in for one player standing at its centre (same radius and rate), and a forage report is logged every half day.")
                .define("performance.debug_regrow_forced_chunks", false);
    }
}
