package untamedwilds.entity.ai.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import untamedwilds.config.EcologyMode;
import untamedwilds.entity.ComplexMob;

import java.util.EnumSet;

// Only difference between SmartHurtByTargetGoal and HurtByTargetGoal is that this class
// automatically alerts others when the hurt mod is a baby
public class SmartHurtByTargetGoal extends HurtByTargetGoal {

    /** Last hit for which the pack/herd has already been called, so the alert fires once per blow. */
    private int lastAlertTimestamp;

    public SmartHurtByTargetGoal(PathfinderMob p_26039_, Class<?>... p_26040_) {
        super(p_26039_, p_26040_);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    /**
     * A juvenile shouts for help and runs; it never takes the target itself.
     * <p>
     * Every attack goal in the mod already refuses to run for a baby -- SmartMeleeAttackGoal,
     * MeleeAttackCircleHerd and MeleeAttackCharger all bail on isBaby() -- so a cub that holds a
     * target cannot do anything with it. Worse, holding one switches its flight off:
     * SmartAvoidGoal.canUse() returns early whenever getTarget() is non-null. The result was cubs
     * standing motionless while a bison or an elephant beat them to death.
     */
    public boolean canUse() {
        if (!super.canUse()) {
            return false;
        }
        // Zoo animals do not pick fights with each other. A player who starts one still gets a fight.
        if (!EcologyMode.allowsBrawls(this.mob) && !EcologyMode.isFightableInZoo(this.mob.getLastHurtByMob())) {
            return false;
        }
        /* A hunting predator that is bitten by something other than its quarry counts it as pressure to
         * leave, not as a new enemy. Turning on every defender is how a hunt became a brawl with a whole
         * herd -- see the longer note in HurtPackByTargetGoal. */
        if (this.mob instanceof ComplexMob fighter && fighter.isHunting()
                && this.mob.getLastHurtByMob() != fighter.huntVictim) {
            fighter.reportDefenderPressure(1);
            return false;
        }
        if (this.mob.isBaby()) {
            int stamp = this.mob.getLastHurtByMobTimestamp();
            if (stamp != this.lastAlertTimestamp) {
                this.lastAlertTimestamp = stamp;
                this.alertOthers();
            }
            /* Puts the cub into the panic state the flight goals read, so it bolts on the tick it is
             * hit rather than waiting for the next avoid scan. getLastHurtByMob() is left set, which
             * is what RetreatWhenRoutedGoal resolves its foe from when there is no target. */
            if (this.mob instanceof ComplexMob cub) {
                cub.fleeCooldown = Math.max(cub.fleeCooldown, 200);
            }
            return false;
        }
        return true;
    }

    /* Do not hand a target to somebody else's cub either -- the alert is meant to bring the ADULTS. */
    @Override
    protected void alertOther(Mob p_26049_, LivingEntity p_26050_) {
        if (p_26049_.isBaby()) {
            return;
        }
        super.alertOther(p_26049_, p_26050_);
    }

    /* Retaliation stops being worth it once either side is nearly dead. Without this a mob that was
     * hit once kept re-acquiring its attacker until one of them was a corpse, which is how a single
     * accidental blow escalated into a herd wiping itself out. ComplexMob.tryBreakOff is the single
     * source of truth for that rule, and for the one exception to it: a predator committed to a food
     * hunt presses on regardless of how nearly dead its prey is. */
    public boolean canContinueToUse() {
        if (this.mob instanceof ComplexMob fighter && ComplexMob.tryBreakOff(fighter, fighter.getTarget())) {
            return false;
        }
        return super.canContinueToUse();
    }
}
