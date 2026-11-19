package untamedwilds.entity;

/**
 * How a tamed, saddled animal rides (2026-10-08). Returned by {@link ComplexMob#getMountProfile()}; a null
 * profile means the type cannot be ridden.
 *
 * @param speed       multiplier on the animal's own movement speed while ridden
 * @param jump        jump strength for the jump bar (0 = no jump bar); vanilla horses use 0.4-1.0
 * @param seats       riders carried (the first one steers)
 * @param stepHeight  blocks the animal steps up while ridden
 * @param charge      a fast-moving mount rams living things in its path (attack damage + knockback)
 * @param sandRunner  extra speed on sand and snow
 * @param riderHeight rider seat height as a fraction of the animal's hitbox height
 */
public record MountProfile(float speed, float jump, int seats, float stepHeight, boolean charge, boolean sandRunner, float riderHeight) {

    public static final MountProfile EQUID = new MountProfile(1.0F, 0.75F, 1, 1.0F, false, false, 0.75F);
    public static final MountProfile CAMELID = new MountProfile(0.95F, 0.0F, 1, 1.0F, false, true, 0.9F);
    public static final MountProfile PROBOSCIDEAN = new MountProfile(0.75F, 0.0F, 2, 1.5F, false, false, 0.95F);
    public static final MountProfile BOVID = new MountProfile(0.95F, 0.0F, 1, 1.0F, true, false, 0.8F);
    public static final MountProfile DEER = new MountProfile(1.05F, 0.0F, 1, 1.0F, true, false, 0.75F);
    /** Slow and sure-footed, one rider, no jump; EntityDiprotodont overrides the seat height (per-species size). */
    /** Argali + bighorn: sure-footed (1.5-block step), a modest leap, and they ram what they run into. */
    public static final MountProfile CAPRINE = new MountProfile(1.0F, 0.55F, 1, 1.5F, true, false, 0.85F);
    public static final MountProfile DIPROTODONT = new MountProfile(0.85F, 0.0F, 1, 1.0F, false, false, 0.9F);

    /** Below this hitbox height an animal is too small to ride (pudu, vicuna, dwarf forms). */
    public static final float MIN_HEIGHT = 1.2F;
}
