package untamedwilds.util;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ComplexMob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the ecological role of an entity, so predators can prefer the prey they actually evolved
 * to hunt instead of the mod's single generic rule -- {@code getEcoLevel(prey) < getEcoLevel(predator)},
 * which appeared verbatim at eleven call sites and made a lion, an anaconda and a tarantula all pick
 * "nearest thing weaker than me".
 * <p>
 * An animal declares what it IS via {@code "ecology_tags"} and, if it hunts, what it EATS via
 * {@code "diet"}, both in {@code data/untamedwilds/entities/<type>.json} at type or species level.
 * Vanilla and modded mobs have no such JSON, so they fall back to {@link #VANILLA_TAGS} and then to a
 * size heuristic -- that way a datapack adding a new prey animal is huntable without touching this file.
 * <p>
 * Backwards compatible by construction: a predator that declares no diet keeps the old eco-level
 * behaviour exactly, so untagged types are unaffected.
 */
public class EcologyTags {

    public static final String SMALL_GAME  = "small_game";
    public static final String GRAZER      = "grazer";
    public static final String BROWSER     = "browser";
    public static final String MEGAFAUNA   = "megafauna";
    public static final String CARNIVORE   = "carnivore";
    public static final String AQUATIC     = "aquatic";
    public static final String PLAYER      = "player";

    /** Preference multiplier applied to a preferred prey's effective distance when ranking targets. */
    private static final double PREFERRED_BIAS = 3.0D;

    private static final Map<EntityType<?>, String> VANILLA_TAGS = new HashMap<>();
    static {
        VANILLA_TAGS.put(EntityType.COW, GRAZER);
        VANILLA_TAGS.put(EntityType.SHEEP, GRAZER);
        VANILLA_TAGS.put(EntityType.HORSE, GRAZER);
        VANILLA_TAGS.put(EntityType.DONKEY, GRAZER);
        VANILLA_TAGS.put(EntityType.MULE, GRAZER);
        VANILLA_TAGS.put(EntityType.LLAMA, BROWSER);
        VANILLA_TAGS.put(EntityType.GOAT, BROWSER);
        VANILLA_TAGS.put(EntityType.PIG, SMALL_GAME);
        VANILLA_TAGS.put(EntityType.CHICKEN, SMALL_GAME);
        VANILLA_TAGS.put(EntityType.RABBIT, SMALL_GAME);
        VANILLA_TAGS.put(EntityType.FOX, SMALL_GAME);
        VANILLA_TAGS.put(EntityType.CAT, SMALL_GAME);
        VANILLA_TAGS.put(EntityType.OCELOT, SMALL_GAME);
        VANILLA_TAGS.put(EntityType.WOLF, CARNIVORE);
        VANILLA_TAGS.put(EntityType.POLAR_BEAR, CARNIVORE);
        VANILLA_TAGS.put(EntityType.COD, AQUATIC);
        VANILLA_TAGS.put(EntityType.SALMON, AQUATIC);
        VANILLA_TAGS.put(EntityType.TROPICAL_FISH, AQUATIC);
        VANILLA_TAGS.put(EntityType.SQUID, AQUATIC);
        VANILLA_TAGS.put(EntityType.GLOW_SQUID, AQUATIC);
        VANILLA_TAGS.put(EntityType.TURTLE, SMALL_GAME);
    }

    /** What this entity IS, for diet matching. Never null; may be empty. */
    public static List<String> getTags(LivingEntity entity) {
        if (entity instanceof ComplexMob mob) {
            EntityDataHolder data = ComplexMob.getEntityData(mob.getType());
            if (data != null && mob.getVariant() < data.getSpeciesData().size()) {
                List<String> tags = data.getEcologyTags(mob.getVariant());
                if (!tags.isEmpty()) {
                    return tags;
                }
            }
        }
        String vanilla = VANILLA_TAGS.get(entity.getType());
        if (vanilla != null) {
            return List.of(vanilla);
        }
        // Players get a tag no diet lists, so tagging a predator never makes it seek players out. They
        // can still be attacked through the retaliation/territorial goals, which is where that belongs.
        if (entity instanceof Player) {
            return List.of(PLAYER);
        }
        // Unknown mob (another mod's, or an untagged vanilla one): infer from body size so it is still
        // huntable. Deliberately generous -- being wrong here only costs a slightly odd target choice.
        float bulk = entity.getBbWidth() * entity.getBbHeight();
        if (bulk < 0.7F) {
            return List.of(SMALL_GAME);
        }
        return bulk > 4.0F ? List.of(MEGAFAUNA) : List.of(GRAZER);
    }

    /** What this predator EATS. Empty means "no preference declared" -- keep the legacy behaviour. */
    public static List<String> getDiet(LivingEntity entity) {
        if (entity instanceof ComplexMob mob) {
            EntityDataHolder data = ComplexMob.getEntityData(mob.getType());
            if (data != null && mob.getVariant() < data.getSpeciesData().size()) {
                return data.getDiet(mob.getVariant());
            }
        }
        return List.of();
    }

    public static boolean hasDiet(LivingEntity predator) {
        return !getDiet(predator).isEmpty();
    }

    /**
     * True when the prey carries at least one tag the predator's diet asks for. Only meaningful when
     * {@link #hasDiet} is true; callers must fall back to the eco-level rule otherwise.
     */
    public static boolean isPreferredPrey(LivingEntity predator, LivingEntity prey) {
        List<String> diet = getDiet(predator);
        if (diet.isEmpty()) {
            return true;
        }
        for (String tag : getTags(prey)) {
            if (diet.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    /** Ceiling on the switching weight, so an abundant species cannot be preferred without limit. */
    private static final double MAX_ABUNDANCE_BIAS = 6.0D;

    /**
     * Ranking weight -- a preferred prey is treated as if it were {@value #PREFERRED_BIAS} times
     * closer, so a predator walks past an off-diet animal to reach the one it actually wants rather
     * than always taking whatever is nearest.
     * <p>
     * Multiplied by a <b>prey-switching</b> term: whichever of the predator's prey species is locally
     * abundant is preferred, and one that has been hunted down to a handful of animals is largely
     * passed over. This is the difference between a food web that persists and one that loses its prey
     * species one at a time -- an unweighted "nearest thing I can eat" rule keeps taking the same
     * animal until there are none of it left, then moves to the next. Only the RATIO between two
     * candidates affects the sort, so the absolute scale of the term does not matter.
     */
    public static double preference(LivingEntity predator, LivingEntity prey) {
        double bias = isPreferredPrey(predator, prey) ? PREFERRED_BIAS : 1.0D;
        return bias * abundanceBias(predator, prey);
    }

    private static double abundanceBias(LivingEntity predator, LivingEntity prey) {
        double strength = ConfigGamerules.preySwitchingStrength.get();
        if (strength <= 0 || !(predator instanceof ComplexMob hunter)) {
            return 1.0D;
        }
        int count = hunter.censusOf(prey.getType());
        if (count <= 1) {
            return 1.0D;
        }
        return Math.min(MAX_ABUNDANCE_BIAS, Math.pow(count, strength));
    }

    /**
     * True when the entity hunts living things at all -- it carries the {@link #CARNIVORE} tag, it
     * declares a diet, or it is a vanilla hostile. Used by the herbivore flight goals, which must key
     * off "is this a predator" and NOT off the old {@code getEcoLevel(other) > getEcoLevel(self)}
     * rule: eco-level counts herd size, so a neighbouring herbivore herd always out-scored a lone
     * animal and got treated as an enemy. That is what turned grazing herds into a massacre.
     */
    public static boolean isPredator(LivingEntity entity) {
        return entity instanceof Monster || getTags(entity).contains(CARNIVORE) || hasDiet(entity);
    }

    /**
     * True when {@code candidate} is something {@code self} has an actual reason to fight: a player,
     * a hostile, a carnivore, anything whose declared diet lists one of self's own tags, or anything
     * already targeting self. A fellow herbivore is never a threat, no matter how big its herd is.
     */
    public static boolean isThreatTo(LivingEntity self, LivingEntity candidate) {
        if (candidate == self) {
            return false;
        }
        if (candidate instanceof Player || candidate instanceof Monster) {
            return true;
        }
        if (candidate instanceof Mob mob && mob.getTarget() == self) {
            return true;
        }
        if (getTags(candidate).contains(CARNIVORE)) {
            return true;
        }
        List<String> diet = getDiet(candidate);
        if (diet.isEmpty()) {
            return false;
        }
        for (String tag : getTags(self)) {
            if (diet.contains(tag)) {
                return true;
            }
        }
        return false;
    }
}
