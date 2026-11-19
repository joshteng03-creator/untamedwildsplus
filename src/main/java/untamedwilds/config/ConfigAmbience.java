package untamedwilds.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * 2.8.0 ambience: vigilance, cosmetic waterhole visits, persistent carcasses, voices. All new keys, so their
 * defaults reach existing worlds; every feature can be switched off and then behaves exactly as 2.7.0 did.
 */
public class ConfigAmbience {

    // vigilance
    public static ForgeConfigSpec.BooleanValue herbivoreAlert;
    public static ForgeConfigSpec.DoubleValue alertRangeScale;
    public static ForgeConfigSpec.IntValue alertTicks;
    public static ForgeConfigSpec.IntValue alarmCallCooldownTicks;
    public static ForgeConfigSpec.IntValue alertMaxTicks;
    public static ForgeConfigSpec.IntValue alertRelaxTicks;
    // drinking
    public static ForgeConfigSpec.BooleanValue drinkVisits;
    public static ForgeConfigSpec.IntValue drinkIntervalTicks;
    public static ForgeConfigSpec.IntValue drinkSearchRadius;
    public static ForgeConfigSpec.DoubleValue drinkAfterWakeChance;
    // carcasses
    public static ForgeConfigSpec.BooleanValue carcasses;
    public static ForgeConfigSpec.BooleanValue carcassFromNaturalDeaths;
    public static ForgeConfigSpec.IntValue carcassLifetimeTicks;
    public static ForgeConfigSpec.IntValue carcassBonesTicks;
    public static ForgeConfigSpec.IntValue carcassAreaCap;
    public static ForgeConfigSpec.IntValue carcassAreaRadius;
    public static ForgeConfigSpec.IntValue carcassGlobalCap;
    public static ForgeConfigSpec.IntValue carcassMaxFeeders;
    public static ForgeConfigSpec.BooleanValue carcassMeatScalesWithFood;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> extraScavengers;
    // voices
    public static ForgeConfigSpec.BooleanValue longRangeCalls;
    public static ForgeConfigSpec.IntValue callIntervalTicks;
    public static ForgeConfigSpec.BooleanValue footstepSounds;
    public static ForgeConfigSpec.BooleanValue pitchBySize;

    ConfigAmbience(final ForgeConfigSpec.Builder builder) {
        builder.comment("Ambience (2.8.0): vigilance, waterholes, carcasses, voices");

        herbivoreAlert = builder.comment("Herbivores that spot a predator just beyond flight range stop, raise their heads and watch it (and call an alarm) instead of ignoring it until it is close enough to flee from.")
                .define("ambience.herbivore_alert", true);
        alertRangeScale = builder.comment("How far beyond its flight distance a herbivore notices a predator and goes on alert (1.5 = half as far again).")
                .defineInRange("ambience.alert_range_scale", 1.5D, 1.0D, 3.0D);
        alertTicks = builder.comment("How long an alert lasts after the last sighting, in ticks.")
                .defineInRange("ambience.alert_ticks", 60, 10, 600);
        alarmCallCooldownTicks = builder.comment("Minimum ticks between alarm calls from the same herd.")
                .defineInRange("ambience.alarm_call_cooldown_ticks", 300, 20, 6000);
        alertMaxTicks = builder.comment("Habituation: after this many ticks of unbroken alert (a predator lingering just beyond flight range) the animal relaxes and goes back to grazing. A hungry animal relaxes after 40% of this. Flight is unaffected: a predator that comes inside flight range is fled from as always.")
                .defineInRange("ambience.alert_max_ticks", 200, 40, 6000);
        alertRelaxTicks = builder.comment("How long a habituated animal ignores a predator that stays beyond flight range before it looks up again.")
                .defineInRange("ambience.alert_relax_ticks", 400, 0, 24000);

        drinkVisits = builder.comment("Herbivores periodically walk to nearby water and drink (cosmetic: there is no thirst stat and nothing bad happens if they cannot). Herds gather at a remembered waterhole.")
                .define("ambience.drink_visits", true);
        drinkIntervalTicks = builder.comment("Average ticks between drinking visits (24000 = once per in-game day). 0 disables visits.")
                .defineInRange("ambience.drink_interval_ticks", 24000, 0, 240000);
        drinkSearchRadius = builder.comment("How far (blocks) an animal looks for water to drink from. Searches are sampled, never a full block scan, and never load chunks.")
                .defineInRange("ambience.drink_search_radius", 24, 8, 64);
        drinkAfterWakeChance = builder.comment("Chance that an animal waking up goes for a drink soon after.")
                .defineInRange("ambience.drink_after_wake_chance", 0.5D, 0.0D, 1.0D);

        carcasses = builder.comment("Animals killed by predators, or dying naturally, leave a carcass that scavengers feed on over time and that fades to bones. Players can butcher a carcass once with an axe, sword or spear for the normal drops. Animals killed by players drop loot exactly as before. Ecosystem mode only; false restores the old instant feeding.")
                .define("ambience.carcasses", true);
        carcassFromNaturalDeaths = builder.comment("Natural deaths (starvation, old age) leave carcasses too, not only predator kills.")
                .define("ambience.carcass_from_natural_deaths", true);
        carcassLifetimeTicks = builder.comment("Ticks a fresh carcass lasts before only bones remain.")
                .defineInRange("ambience.carcass_lifetime_ticks", 12000, 600, 240000);
        carcassBonesTicks = builder.comment("Ticks the bones remain before they disappear.")
                .defineInRange("ambience.carcass_bones_ticks", 6000, 200, 240000);
        carcassAreaCap = builder.comment("Most carcasses allowed within carcass_area_radius; past that the oldest one turns straight to bones.")
                .defineInRange("ambience.carcass_area_cap", 6, 1, 64);
        carcassAreaRadius = builder.comment("Radius (blocks) of the area cap.")
                .defineInRange("ambience.carcass_area_radius", 48, 8, 256);
        carcassGlobalCap = builder.comment("Most carcasses per dimension. Above it, deaths fall back to the old instant drops and feeding.")
                .defineInRange("ambience.carcass_global_cap", 64, 1, 4096);
        carcassMaxFeeders = builder.comment("Most scavengers that can feed on one carcass at the same time.")
                .defineInRange("ambience.carcass_max_feeders", 3, 1, 16);
        carcassMeatScalesWithFood = builder.comment("Butchering a carcass scavengers have already fed on gives proportionally less meat (meat items only).")
                .define("ambience.carcass_meat_scales_with_food", true);
        extraScavengers = builder.comment("Extra entity types (registry ids) that also walk to carcasses and feed. Bears and hyenas always do.")
                .defineList("ambience.extra_scavengers", List.of(), o -> o instanceof String);

        longRangeCalls = builder.comment("Herd leaders and solitary animals occasionally make long-range calls (bugles, howls, trumpets) that carry 48-64 blocks.")
                .define("ambience.long_range_calls", true);
        callIntervalTicks = builder.comment("Average ticks between long-range calls from one animal.")
                .defineInRange("ambience.call_interval_ticks", 2400, 200, 72000);
        footstepSounds = builder.comment("Big animals have heavy, size-scaled footsteps.")
                .define("ambience.footstep_sounds", true);
        pitchBySize = builder.comment("Bigger animals of a type sound deeper and smaller ones higher (a dwarf mammoth is not voiced like a mammoth). Babies are still higher-pitched as in vanilla.")
                .define("ambience.pitch_by_size", true);
    }
}
