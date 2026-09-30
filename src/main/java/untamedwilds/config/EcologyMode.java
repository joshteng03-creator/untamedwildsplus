package untamedwilds.config;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * The Ecosystem/Zoo switch, and the single place that knows how the two modes differ.
 * <p>
 * <b>Ecosystem</b> is a self-regulating predator/prey simulation: animals breed at the configured
 * rates, herds shift range when they eat one out, surplus young disperse, predators hunt, assess their
 * odds, lose fights and follow the herds. <b>Zoo</b> is an exhibit: reproduction is slow and
 * predictable, predators hunt only when genuinely starving, herds stay where they were put, and
 * nothing picks a fight with anything except a player who started it.
 * <p>
 * The live setting is a real vanilla gamerule, {@code untamedwildsEcosystemMode}, registered in the
 * {@link untamedwilds.UntamedWilds} constructor. That makes it per-world, saved in {@code level.dat},
 * and flippable mid-game with {@code /gamerule} -- which a config entry cannot be.
 * {@code gamerules.default_ecosystem_mode} supplies the default for new worlds and the fallback for
 * the handful of call sites that have no {@link Level} in hand.
 * <p>
 * Deliberately NOT a rewrite of every {@code ConfigGamerules.X.get()} call site. Only the numbers the
 * two modes actually disagree about are routed through here; everything else still reads the config
 * directly, so all existing tuning stays valid and the diff stays readable.
 */
public final class EcologyMode {

    /** Set by {@link #register()}. Null only if registration somehow did not run, hence the fallbacks. */
    @Nullable
    private static GameRules.Key<GameRules.BooleanValue> ECOSYSTEM_MODE;

    private EcologyMode() {}

    /**
     * Registers the gamerule. Must run during mod construction: vanilla builds a {@code GameRules}
     * instance per world from the type map, so anything registered after the first world loads is
     * missing from it.
     */
    public static void register() {
        if (ECOSYSTEM_MODE == null) {
            ECOSYSTEM_MODE = GameRules.register("untamedwildsEcosystemMode", GameRules.Category.MOBS,
                    GameRules.BooleanValue.create(ConfigGamerules.defaultEcosystemMode.get()));
        }
    }

    /** True when this world is running the full ecological simulation. */
    public static boolean isEcosystem(@Nullable Level level) {
        if (level == null || ECOSYSTEM_MODE == null) {
            return ConfigGamerules.defaultEcosystemMode.get();
        }
        return level.getGameRules().getBoolean(ECOSYSTEM_MODE);
    }

    /** Convenience for the common case of asking about the world an animal is standing in. */
    public static boolean isEcosystem(LivingEntity entity) {
        return isEcosystem(entity.level);
    }

    public static boolean isZoo(LivingEntity entity) {
        return !isEcosystem(entity);
    }

    // ---------------------------------------------------------------------------------------------
    // Reproduction. Zoo mode slows the whole cycle down rather than stopping it, so an exhibit still
    // produces the occasional calf without a herd doubling every few days.
    // ---------------------------------------------------------------------------------------------

    private static final double ZOO_GESTATION_SCALE = 2.5D;
    private static final double ZOO_MATURITY_SCALE = 2.0D;

    public static double gestationScale(LivingEntity entity) {
        return isEcosystem(entity) ? 1.0D : ZOO_GESTATION_SCALE;
    }

    public static double maturityScale(LivingEntity entity) {
        return isEcosystem(entity) ? 1.0D : ZOO_MATURITY_SCALE;
    }

    /** Zoo mode drops the carnivore litter bonus; a growing pride is an ecosystem problem, not a zoo one. */
    public static int litterBonus(LivingEntity entity) {
        return isEcosystem(entity) ? ConfigGamerules.predatorLitterBonus.get() : 0;
    }

    // ---------------------------------------------------------------------------------------------
    // Predation
    // ---------------------------------------------------------------------------------------------

    /** Hunger at or below which a predator will hunt. Zoo animals only hunt when genuinely starving. */
    private static final int ZOO_HUNT_THRESHOLD = 8;

    public static int huntHungerThreshold(LivingEntity entity, int configured) {
        return isEcosystem(entity) ? configured : Math.min(configured, ZOO_HUNT_THRESHOLD);
    }

    public static float huntRiskChance(LivingEntity entity) {
        return isEcosystem(entity) ? ConfigGamerules.predatorHuntRiskChance.get().floatValue() : 0F;
    }

    public static double hungerDrainScale(LivingEntity entity) {
        return isEcosystem(entity) ? ConfigGamerules.predatorHungerDrain.get() : ConfigGamerules.predatorHungerDrain.get() * 0.4D;
    }

    // ---------------------------------------------------------------------------------------------
    // Conflict
    // ---------------------------------------------------------------------------------------------

    /** How many herd-mates may engage one threat at once. Zoo herds put up a mother, and no more. */
    public static int defenderCap(LivingEntity entity) {
        return isEcosystem(entity) ? ConfigGamerules.herdDefenderCap.get() : 1;
    }

    /**
     * Whether this animal will pick a fight with something that is not a player.
     * <p>
     * Gates retaliation, pack call-for-help, territorial scraps and the angry-sleeper goal. Defence of
     * young is deliberately NOT gated -- a mother should still see off a wolf in a zoo -- it just runs
     * at a defender cap of one.
     */
    public static boolean allowsBrawls(LivingEntity entity) {
        return isEcosystem(entity);
    }

    /** True when {@code aggressor} may be fought despite zoo mode: a player started it. */
    public static boolean isFightableInZoo(@Nullable LivingEntity aggressor) {
        return aggressor instanceof Player;
    }

    // ---------------------------------------------------------------------------------------------
    // Movement
    // ---------------------------------------------------------------------------------------------

    /** Zoo exhibits stay where they were put; nothing migrates, disperses or relocates. */
    public static boolean allowsMigration(LivingEntity entity) {
        return isEcosystem(entity);
    }

    // ---------------------------------------------------------------------------------------------
    // Demography. Every gate below is off in Zoo mode, which is the whole point of the mode: a zoo
    // is stocked livestock, not a simulation, and nothing in it should die of anything the player did
    // not do. Ecosystem mode is where starvation, senescence and finite forage live.
    //
    // Note carefully that these gate CONSEQUENCES, not bookkeeping. ComplexMob still accrues condition
    // and lifeTicks in Zoo mode, because otherwise flipping the gamerule to true mid-world would hand
    // every existing animal a condition of 0 (instantly starving-weak) and a lifeTicks of 0 (a fresh
    // lifespan). Track the numbers always; act on them only here.
    // ---------------------------------------------------------------------------------------------

    /** Whether an animal can actually die of hunger. Zoo hunger still floors at 0 and still skips the heal. */
    public static boolean allowsStarvationDeath(LivingEntity entity) {
        return isEcosystem(entity);
    }

    /** Whether poor condition or old age slows an animal down. No zoo animal should look sickly. */
    public static boolean allowsConditionPenalty(LivingEntity entity) {
        return isEcosystem(entity);
    }

    /** Whether animals grow old: the senescent breeding stop, the speed penalty and death by old age. */
    public static boolean allowsSenescence(LivingEntity entity) {
        return isEcosystem(entity);
    }

    /** Whether forage is finite. Zoo grazing pays a flat forage_yield_max and the range never runs down. */
    public static boolean allowsForageDepletion(LivingEntity entity) {
        return isEcosystem(entity);
    }

    /**
     * Whether breeding consults sustained body condition and local forage.
     * <p>
     * When false the breeding predicates must fall back to the ORIGINAL ones -- {@code hunger >= 80}
     * for predators, {@code hunger >= 80 && !isRangeExhausted()} for herbivores -- and not simply skip
     * the check. A gate that returned true here would leave zoo animals breeding more freely than they
     * did before any of this existed.
     */
    public static boolean usesConditionForBreeding(LivingEntity entity) {
        return isEcosystem(entity);
    }

    /** Level-scoped form, for the forage regrowth handler, which has a Level and no entity. */
    public static boolean allowsForageDepletion(@Nullable Level level) {
        return isEcosystem(level);
    }
}
