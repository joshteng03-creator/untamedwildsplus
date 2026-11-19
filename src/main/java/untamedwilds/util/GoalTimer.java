package untamedwilds.util;

import net.minecraft.world.entity.Entity;

/**
 * Parity-proof replacement for {@code if (mob.tickCount % n == 0)} inside goal code (2.8.0).
 * <p>
 * Vanilla only evaluates goals ({@code canUse}, and {@code tick} unless the goal asks to update every tick) on
 * the server ticks where {@code (serverTick + entityId)} is even. A mob's tickCount advances in lockstep, so on
 * those ticks it always has the same parity -- and for roughly half of all mobs that parity is odd, so a
 * {@code tickCount % 10 == 0} check inside a goal NEVER fires. The herbivore flee scan was one of them: about
 * half the herd never looked for predators itself.
 * <p>
 * This fires on the FIRST call inside each {@code n}-tick window instead, so it fires once per window whatever
 * the call parity. With {@code stagger} the windows are offset by the entity id (independent checks spread
 * out instead of every animal loaded together firing in the same tick); without it the windows stay aligned
 * to tickCount, for checks that are paired with another {@code tickCount % n} window in the same goal.
 */
public final class GoalTimer {

    private final int interval;
    private final boolean stagger;
    private long lastWindow = Long.MIN_VALUE;
    private int lastInterval;

    public GoalTimer(int interval, boolean stagger) {
        this.interval = Math.max(1, interval);
        this.stagger = stagger;
    }

    public boolean ready(Entity entity) {
        return this.ready(entity, this.interval);
    }

    /** As {@link #ready(Entity)}, with a per-call interval (e.g. juveniles scanning more often). */
    public boolean ready(Entity entity, int interval) {
        return this.readyAt(entity.tickCount + (this.stagger ? (long) entity.getId() : 0L), interval);
    }

    /**
     * Windows keyed on an explicit clock. Herd members pass {@code gameTime + herdKey}, so a whole herd shares ONE
     * window and its single shared threat search serves every member (2.8.0 shared herd scan).
     */
    public boolean readyAt(long t, int interval) {
        int n = Math.max(1, interval);
        long window = Math.floorDiv(t, n);
        if (this.lastWindow == Long.MIN_VALUE) {
            this.lastWindow = window;
            this.lastInterval = n;
            return Math.floorMod(t, n) == 0;             // first ever call: only exactly on a boundary, as before
        }
        if (window != this.lastWindow || n != this.lastInterval) {   // a changed interval opens a new window
            this.lastWindow = window;
            this.lastInterval = n;
            return true;
        }
        return false;
    }
}
