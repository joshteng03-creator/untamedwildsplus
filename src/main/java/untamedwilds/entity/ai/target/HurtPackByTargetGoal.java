package untamedwilds.entity.ai.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import untamedwilds.config.EcologyMode;
import untamedwilds.entity.ComplexMob;

import java.util.EnumSet;

public class HurtPackByTargetGoal extends TargetGoal {
    private static final TargetingConditions field_220795_a = (TargetingConditions.DEFAULT);
    private boolean entityCallsForHelp;
    private int revengeTimerOld;
    private final Class<?>[] excludedReinforcementTypes;
    private Class<?>[] reinforcementTypes;

    public HurtPackByTargetGoal(PathfinderMob creatureIn, Class<?>... excludeReinforcementTypes) {
        super(creatureIn, true);
        this.excludedReinforcementTypes = excludeReinforcementTypes;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    public boolean canUse() {
        int i = this.mob.getLastHurtByMobTimestamp();
        LivingEntity livingentity = this.mob.getLastHurtByMob();
        if (i != this.revengeTimerOld && livingentity != null) {
            for(Class<?> oclass : this.excludedReinforcementTypes) {
                if (oclass.isAssignableFrom(livingentity.getClass())) {
                    return false;
                }
            }
            // Zoo animals do not pick fights. A player who starts one still gets what they asked for.
            if (!EcologyMode.allowsBrawls(this.mob) && !EcologyMode.isFightableInZoo(livingentity)) {
                this.revengeTimerOld = i;
                return false;
            }
            /* A hunting predator gored by a DEFENDER does not turn on the defender: it counts the blow
             * as a reason to leave, and either presses the animal it actually came for or gives up.
             *
             * This is the single inversion that made hunting inside a herd survivable. Before it, one
             * bison goring one wolf handed the ENTIRE pack that bison as a target -- the hunt was
             * dropped and a pack-versus-herd brawl started in its place, which the pack always lost
             * because the herd out-numbered it and neither side would disengage. Escalation was the
             * default response to being defended against. */
            if (this.mob instanceof ComplexMob fighter && fighter.isHunting() && livingentity != fighter.huntVictim) {
                this.revengeTimerOld = i;
                fighter.reportDefenderPressure(1);
                return false;
            }
            /* A pup calls the pack and runs -- it never takes the target. Same reasoning as
             * SmartHurtByTargetGoal: a juvenile is barred from every attack goal, so a target it
             * cannot act on only serves to switch its flight off in SmartAvoidGoal. */
            if (this.mob.isBaby()) {
                this.revengeTimerOld = i;
                if (this.entityCallsForHelp) {
                    this.alertOthers();
                }
                if (this.mob instanceof ComplexMob pup) {
                    pup.fleeCooldown = Math.max(pup.fleeCooldown, 200);
                }
                return false;
            }
            return this.canAttack(livingentity, field_220795_a);
        } else {
            return false;
        }
    }

    public HurtPackByTargetGoal setAlertOthers(Class<?>... reinforcementTypes) {
        this.entityCallsForHelp = true;
        this.reinforcementTypes = reinforcementTypes;
        return this;
    }

    public void start() {
        this.mob.setTarget(this.mob.getLastHurtByMob());
        this.targetMob = this.mob.getTarget();
        this.revengeTimerOld = this.mob.getLastHurtByMobTimestamp();
        this.unseenMemoryTicks = 300;
        if (this.entityCallsForHelp) {
            this.alertOthers();
        }

        super.start();
    }

    /* Pack retaliation is a fight, not a hunt, so it ends when either side is nearly dead rather than
     * running until one of them is a corpse. See ComplexMob.tryBreakOff(). This goal sits at priority 1
     * on every pack predator, above the hunt goal, so it is the one that used to spare wounded prey the
     * instant the prey bit back -- tryBreakOff now checks the hunt commitment before applying the rule. */
    public boolean canContinueToUse() {
        if (this.mob instanceof ComplexMob fighter && ComplexMob.tryBreakOff(fighter, fighter.getTarget())) {
            return false;
        }
        return super.canContinueToUse();
    }

    /** How far a call for help carries. The pack that is present comes; the pack across the valley does not. */
    private static final double ALERT_RADIUS = 12D;

    protected void alertOthers() {
        if (this.mob instanceof ComplexMob) {
            ComplexMob mob = (ComplexMob)this.mob;
            if (mob.herd != null) {
                for (ComplexMob creature : mob.herd.creatureList) {
                    // The call for help brings the adults. A pup handed a target just stops running.
                    if (creature.isBaby()) {
                        continue;
                    }
                    /* Never off a pack-mate's live hunt. The alert used to overwrite the target of
                     * every member unconditionally, so a single blow from a defender pulled animals off
                     * the quarry they had already run down and put them into a fight nobody chose. */
                    if (creature.isHunting()) {
                        continue;
                    }
                    if (creature != this.mob && creature.distanceToSqr(this.mob) > ALERT_RADIUS * ALERT_RADIUS) {
                        continue;
                    }
                    creature.setTarget(this.mob.getLastHurtByMob());
                }
            }
        }
    }
}

