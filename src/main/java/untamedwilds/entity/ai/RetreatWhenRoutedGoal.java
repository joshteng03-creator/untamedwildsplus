package untamedwilds.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import untamedwilds.entity.ComplexMob;

import java.util.EnumSet;

/**
 * Breaks off a fight once the animal is nearly dead, instead of letting every conflict run to a
 * corpse. Applies to territorial scraps, retaliation, defence of young and cross-species conflict.
 * <p>
 * A predator running a genuine hunt for food is exempt -- {@link ComplexMob#isRouted()} returns false
 * while {@code activeHunt} is set. Without that exemption a predator could never land a kill, never
 * reach {@code satiateFromKill}, and would starve, which is the opposite of what this goal is for.
 * <p>
 * This supersedes the "skirmish" retreat that was written into {@link SmartMeleeAttackGoal} and then
 * disabled behind an {@code if (false && ...)}; that version only reacted when the mob was below FULL
 * health, which fired constantly, and it lived in the attack goal, so it never covered the target
 * goals that keep re-acquiring the same enemy.
 */
public class RetreatWhenRoutedGoal extends Goal {

    private final ComplexMob taskOwner;
    private final double speed;
    private Path path;

    public RetreatWhenRoutedGoal(ComplexMob entityIn, double speedIn) {
        this.taskOwner = entityIn;
        this.speed = speedIn;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.shouldRun() || this.taskOwner.getCommandInt() != 0) {
            return false;
        }
        LivingEntity foe = this.getFoe();
        if (foe == null) {
            return false;
        }
        Vec3 escape = DefaultRandomPos.getPosAway(this.taskOwner, 16, 7, foe.position());
        if (escape == null) {
            return false;
        }
        this.path = this.taskOwner.getNavigation().createPath(escape.x, escape.y, escape.z, 0);
        return this.path != null;
    }

    /**
     * Whether this animal should be running right now: beaten in a fight, or a juvenile that has just
     * been hit.
     * <p>
     * A cub is never "routed" by health, because it usually dies at full health -- it cannot fight back
     * at all (every attack goal bails on isBaby()), so the fight is one-sided from the first blow. The
     * panic flag set by SmartHurtByTargetGoal / HurtPackByTargetGoal is what gets it moving on the tick
     * it is struck, instead of waiting up to a second for the next SmartAvoidGoal scan.
     */
    private boolean shouldRun() {
        return this.taskOwner.isRouted() || (this.taskOwner.isBaby() && this.taskOwner.fleeCooldown > 0);
    }

    /* Whatever this animal is currently fighting: its own target, or -- once that has been cleared --
     * the thing that last hit it, so the retreat survives the target being dropped. */
    private LivingEntity getFoe() {
        LivingEntity foe = this.taskOwner.getTarget() != null ? this.taskOwner.getTarget() : this.taskOwner.getLastHurtByMob();
        if (foe == null || !foe.isAlive() || this.taskOwner.distanceToSqr(foe) > 400D) {
            return null;
        }
        return foe;
    }

    @Override
    public void start() {
        this.taskOwner.routFromCombat();
        if (this.taskOwner.isSleeping()) {
            this.taskOwner.setSleeping(false);
        }
        this.taskOwner.getNavigation().moveTo(this.path, this.retreatSpeed());
    }

    /* An animal that has been beaten down to 10% health does not sprint away untouched -- see
     * ComplexMob.getFleeSpeedFactor(). Without this, breaking off a fight was a guaranteed escape, which
     * is a large part of why nothing ever died. */
    private double retreatSpeed() {
        return this.speed * this.taskOwner.getFleeSpeedFactor();
    }

    /* Deliberately NOT gated on the navigation still being busy: a short path finishes in a couple of
     * seconds, and ending the goal there would hand the mob straight back to its target goals while it
     * is still at 10% health. The cooldown is the timer; tick() re-paths whenever the path runs out. */
    @Override
    public boolean canContinueToUse() {
        return this.taskOwner.fleeCooldown > 0;
    }

    @Override
    public void tick() {
        // Target goals run on a separate selector and will happily re-acquire the enemy we are running
        // from, so the target has to be cleared every tick, not just once on start.
        if (this.taskOwner.getTarget() != null) {
            this.taskOwner.setTarget(null);
        }
        if (this.taskOwner.getNavigation().isDone()) {
            LivingEntity foe = this.getFoe();
            if (foe != null) {
                Vec3 escape = DefaultRandomPos.getPosAway(this.taskOwner, 16, 7, foe.position());
                if (escape != null) {
                    this.taskOwner.getNavigation().moveTo(escape.x, escape.y, escape.z, this.retreatSpeed());
                }
            }
        }
    }

    @Override
    public void stop() {
        this.path = null;
    }
}
