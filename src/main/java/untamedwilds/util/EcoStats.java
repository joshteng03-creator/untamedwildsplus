package untamedwilds.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ISpecies;

import java.util.HashMap;
import java.util.Map;

/**
 * Births and deaths by cause, per species, between census snapshots (2026-10-24). The census used to report
 * only head counts, so a soak test could see a population fall but not WHY -- predation, starvation or old
 * age -- which is the one thing needed to tune the ecosystem. Server-thread only; drained by CensusCommand.
 */
public final class EcoStats {

    public static final int BIRTHS = 0;
    public static final int PREDATION = 1;
    public static final int STARVATION = 2;
    public static final int OLD_AGE = 3;
    public static final int OTHER = 4;
    private static final int SLOTS = 5;

    private static final Map<String, int[]> COUNTS = new HashMap<>();

    private EcoStats() {}

    /** The census row key, "type,species" -- identical to CensusCommand's grouping key. */
    public static String key(ComplexMob mob) {
        String type = EntityType.getKey(mob.getType()).getPath();
        String species = mob instanceof ISpecies s && ComplexMob.ENTITY_DATA_HASH.containsKey(mob.getType())
                ? s.getRawSpeciesName(mob.getVariant()) : "-";
        return type + "," + species;
    }

    public static void recordBirth(ComplexMob child) {
        if (!child.level.isClientSide) {
            COUNTS.computeIfAbsent(key(child), k -> new int[SLOTS])[BIRTHS]++;
        }
    }

    public static void recordDeath(ComplexMob mob, DamageSource cause, boolean predated) {
        if (mob.level.isClientSide) {
            return;
        }
        int slot = predated ? PREDATION : cause == DamageSource.STARVE ? STARVATION : mob.oldAgeDeath ? OLD_AGE : OTHER;
        COUNTS.computeIfAbsent(key(mob), k -> new int[SLOTS])[slot]++;
    }

    /** Counts since the last call, which resets them. */
    public static Map<String, int[]> drain() {
        Map<String, int[]> out = new HashMap<>(COUNTS);
        COUNTS.clear();
        return out;
    }
}
