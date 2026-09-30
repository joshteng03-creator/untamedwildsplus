package untamedwilds.entity.ai.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.config.EcologyMode;
import untamedwilds.entity.ComplexMob;
import untamedwilds.util.EcologyTags;

import java.util.EnumSet;

/**
 * A herd animal comes to the aid of a herd-mate under attack.
 * <p>
 * This is the group defence a herd did not have. {@code ProtectChildrenTarget} is the only other one,
 * and it requires a <b>same-variant baby within 8 blocks</b> of the defender -- so a herd with no calf
 * nearby had no group defence whatsoever, and an adult being killed among twenty herd-mates was ignored
 * by every one of them. Predators have had the equivalent all along: {@code EntityDireWolf} registers
 * {@code HurtPackByTargetGoal.setAlertOthers(...)}, while every herbivore registers a bare
 * {@code SmartHurtByTargetGoal}, which never enables vanilla's {@code alertSameType} and so never calls
 * the herd at all.
 * <p>
 * <b>The cap is the whole design.</b> Slots are claimed from {@link untamedwilds.entity.HerdEntity}'s
 * existing defender pool, so this inherits {@code herd_defender_cap}, the re-engage cooldown,
 * {@code pruneDefenders} and the group-rout machinery unchanged. It therefore cannot recreate the
 * unbounded mobbing that killed predators before the defender cap existed: it changes <i>what</i> brings
 * a herd's defenders out, never <i>how many</i> of them there are.
 * <p>
 * The trigger needs no new bookkeeping. {@code getLastHurtByMob}/{@code getLastHurtByMobTimestamp} are
 * maintained by vanilla {@code LivingEntity.hurt} and are exactly what {@code HurtByTargetGoal} reads.
 */
public class DefendHerdMateTarget extends TargetGoal {

    /** How recently the herd-mate must have been hit for the call to still be answered. */
    private static final int ALERT_MEMORY_TICKS = 100;
    /** Slack over the defend radius before a defender gives up and goes back to the herd. */
    private static final double DISENGAGE_SLACK = 8D;

    private final ComplexMob defender;
    private LivingEntity threat;

    public DefendHerdMateTarget(ComplexMob creature) {
        super(creature, false);
        this.defender = creature;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        double radius = ConfigGamerules.herdDefendRadius.get();
        if (radius <= 0 || this.defender.isBaby() || this.defender.isTame() || this.defender.herd == null) {
            return false;
        }
        // A defender that has just stood down is on its re-engage cooldown; do not even scan.
        if (this.defender.defendCooldown > 0) {
            return false;
        }
        for (ComplexMob member : this.defender.herd.creatureList) {
            if (member == this.defender || !member.isAlive()
                    || member.distanceToSqr(this.defender) > radius * radius) {
                continue;
            }
            LivingEntity aggressor = member.getLastHurtByMob();
            if (aggressor == null || !aggressor.isAlive() || aggressor == this.defender
                    || member.tickCount - member.getLastHurtByMobTimestamp() > ALERT_MEMORY_TICKS) {
                continue;
            }
            /* Never answer a call against our own herd -- a scuffle between two members of the same herd
             * must not pull the other eighteen in. isThreatTo already refuses a fellow herbivore no
             * matter how big its herd is, but a herd can hold more than one species after a merge. */
            if (aggressor instanceof ComplexMob mate && this.defender.herd.containsCreature(mate)) {
                continue;
            }
            if (!EcologyTags.isThreatTo(this.defender, aggressor)) {
                continue;
            }
            /* Zoo herds do not rally. This is a call-for-help, which the zoo contract gates alongside
             * retaliation and territorial scraps -- unlike defence of young, which always runs. A fight
             * a player started is still answered. */
            if (!EcologyMode.allowsBrawls(this.defender) && !EcologyMode.isFightableInZoo(aggressor)) {
                continue;
            }
            if (!this.canAttack(aggressor, TargetingConditions.forCombat())) {
                continue;
            }
            if (!this.defender.herd.tryClaimDefenderSlot(this.defender, aggressor)) {
                // The front is full. Backing off is what the flight goals already do for free.
                return false;
            }
            this.threat = aggressor;
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.threat);
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        /* Single source of truth for "this fight is over". Also carries the one exception: a predator
         * committed to a food hunt is not spared for being nearly dead. */
        if (ComplexMob.tryBreakOff(this.defender, this.defender.getTarget())) {
            return false;
        }
        if (this.threat == null || !this.threat.isAlive()) {
            return false;
        }
        /* Driving the threat off is the job; running it down across the map is how a defence turned into
         * a herd chasing a predator to death, which is the behaviour the defender cap exists to stop. */
        double reach = ConfigGamerules.herdDefendRadius.get() + DISENGAGE_SLACK;
        if (this.threat.distanceToSqr(this.defender) > reach * reach) {
            return false;
        }
        return super.canContinueToUse();
    }

    /* Frees the slot for a herd-mate and starts this animal's own re-engage cooldown, so one long fight
     * cannot cycle a fresh set of defenders through the cap and land the same total damage anyway. */
    @Override
    public void stop() {
        if (this.defender.herd != null) {
            this.defender.herd.releaseDefenderSlot(this.defender);
        }
        this.threat = null;
        super.stop();
    }
}
