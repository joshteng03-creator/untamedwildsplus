package untamedwilds.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * The ecological profile of a type or species, read from an {@code "ecology"} block in
 * {@code data/untamedwilds/entities/<type>.json}:
 *
 * <pre>
 * "ecology": {
 *   "speed": 0.23,                    // real-world-calibrated MOVEMENT_SPEED; omit to inherit
 *   "tags": ["grazer", "megafauna"],  // what this animal IS
 *   "diet": ["small_game", "grazer"]  // what it EATS (predators only)
 * }
 * </pre>
 *
 * These three live in one nested object rather than as three top-level fields for a hard reason:
 * Mojang's {@code RecordCodecBuilder.group()} accepts at most 16 arguments, and SpeciesDataHolder was
 * already at 15. Nesting keeps both holders inside the limit and leaves room to add further ecological
 * traits later without another refactor.
 */
public class EcologyDataHolder {

    public static final EcologyDataHolder EMPTY = new EcologyDataHolder(-1F, new ArrayList<>(), new ArrayList<>());

    public static final Codec<EcologyDataHolder> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Codec.FLOAT.fieldOf("speed").orElse(-1F).forGetter((holder) -> holder.speed),
            Codec.STRING.listOf().fieldOf("tags").orElse(new ArrayList<>()).forGetter((holder) -> holder.tags),
            Codec.STRING.listOf().fieldOf("diet").orElse(new ArrayList<>()).forGetter((holder) -> holder.diet))
            .apply(instance, EcologyDataHolder::new));

    private final float speed;
    private final List<String> tags;
    private final List<String> diet;

    public EcologyDataHolder(float speed, List<String> tags, List<String> diet) {
        this.speed = speed;
        this.tags = tags;
        this.diet = diet;
    }

    /** -1 means "not set here"; callers fall back to the type, then to registerAttributes(). */
    public float getSpeed() {
        return this.speed;
    }

    public List<String> getTags() {
        return this.tags;
    }

    public List<String> getDiet() {
        return this.diet;
    }
}
