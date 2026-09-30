package untamedwilds.entity.ai.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ComplexMob;
import untamedwilds.util.EcologyTags;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class ProtectChildrenTarget<T extends LivingEntity> extends HuntMobTarget<T> {

    /**
     * How close a threat has to be to the calf before its mother will take it on.
     * <p>
     * This used to be the mother's whole follow range, which for a herd animal meant every adult
     * within 8 blocks of any calf piled onto the same predator at once -- a dozen bison, several of
     * them also charging via MeleeAttackCharger at 7 attack apiece. A bear cannot survive that and a
     * bear does not deserve to die for standing near a herd. Defending a calf now means defending the
     * ground the calf is actually on.
     */
    private static final double DEFEND_RADIUS = 8D;

    private Mob protectTarget;

    public ProtectChildrenTarget(ComplexMob creature, Class<T> classTarget, boolean checkSight, final Predicate<LivingEntity> targetSelector) {
        super(creature, classTarget, checkSight,200, false, targetSelector);
    }

    @Override
    protected boolean isFoodHunt() {
        return false;
    }

    protected boolean isValidTarget(LivingEntity entity, @Nullable Predicate<LivingEntity> predicate) {
        if (entity instanceof Creeper || entity.equals(this.mob) || (!ConfigGamerules.attackUndead.get() && entity.getMobType() == MobType.UNDEAD) || (predicate != null && !predicate.test(entity))) {
            return false;
        }
        /* The same-species exemption used to be gated behind `getEcoLevel(entity) < getEcoLevel(mob)`,
         * which inverted its own purpose: two animals of the same species and variant have the SAME
         * eco level, so the `<` was false, the guard was skipped, and a mother would happily gore the
         * herd-mate standing next to her calf. The strength comparison never belonged here -- a member
         * of your own species is never the thing you are defending the calf from. */
        if (this.mob.getClass() == entity.getClass() && this.mob instanceof ComplexMob attacker && entity instanceof ComplexMob defender) {
            if (attacker.getVariant() == defender.getVariant()) {
                return false;
            }
        }
        /* Nor is any other herbivore. Callers used to pass `getEcoLevel(input) > getEcoLevel(this)`,
         * and because eco level adds herd size, a neighbouring grazing herd always out-scored a lone
         * cow and was attacked on sight. Defence of young is now strictly about actual threats. */
        /* Anything actually going for the young is a threat whatever it is -- this is what still lets a
         * lioness see off a wolf that is after her cub, now that predators no longer count as threats to
         * each other merely for standing nearby. */
        boolean huntingTheYoung = entity instanceof Mob attacker && this.protectTarget != null && attacker.getTarget() == this.protectTarget;
        if (!huntingTheYoung && !EcologyTags.isThreatTo(this.mob, entity)) {
            return false;
        }
        return canAttack(entity, TargetingConditions.forCombat().range(getFollowDistance()));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isBaby() || (this.mob instanceof TamableAnimal tamable && tamable.isTame()))
            return false;
        /* Every adult ran a calf scan plus, with a calf about, a threat scan and a census-weighted sort,
         * every other tick. A ~10-tick random interval is vanilla's cadence for target goals and still
         * answers a threat to a calf within half a second. */
        if (this.mob.getRandom().nextInt(reducedTickDelay(10)) != 0)
            return false;

        if (this.mob instanceof ComplexMob temp) {

            for (Mob child : this.mob.level.getEntitiesOfClass(this.mob.getClass(), mob.getBoundingBox().inflate(8.0D, 4.0D, 8.0D))) {
                if (child.isBaby() && ((ComplexMob)child).getVariant() == temp.getVariant()) {
                    this.protectTarget = child;
                    List<T> list = this.mob.level.getEntitiesOfClass(this.targetClass, this.getTargettableArea(this.getFollowDistance()), this.targetEntitySelector);
                    // Only threats standing over the calf, not everything the mother can see. See DEFEND_RADIUS.
                    list.removeIf(threat -> threat.distanceToSqr(child) > DEFEND_RADIUS * DEFEND_RADIUS);

                    if (list.isEmpty()) {
                        return false;
                    }

                    T threat = this.sorter.best(list);
                    /* Bounded group defence. Every adult runs this goal independently, so before this
                     * check a twenty-strong herd put a dozen mothers onto the same wolf at once and
                     * killed it in about two seconds no matter how strong it was -- the reason
                     * predators could not survive near large herds. The herd now fields a front of
                     * herd_defender_cap animals and the rest bunch and back off, which is both what
                     * real herd defence looks like and the only way predation can happen at all.
                     *
                     * Nothing else is needed to make the non-defenders retreat: the HEAVY herbivores
                     * all carry a SmartAvoidGoal keyed on predators that is suppressed only while they
                     * hold a combat target, so refusing the target IS the backing away. */
                    if (temp.herd != null && !temp.herd.tryClaimDefenderSlot(temp, threat)) {
                        return false;
                    }
                    this.targetMob = threat;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canContinueToUse() {
        /* Defending a calf is not a fight to the death for either side. Whichever of the two drops
         * below ROUT_THRESHOLD ends it -- the mother because she has driven the predator off far
         * enough, the predator because this meal is not worth dying for. The one exception, handled
         * inside tryBreakOff: a predator whose own cub strayed near its committed prey must not have
         * this goal hand the kill back, which it did whenever isThreatTo matched the animal it was
         * already hunting. */
        if (this.mob instanceof ComplexMob defender && ComplexMob.tryBreakOff(defender, this.targetMob != null ? this.targetMob : defender.getTarget())) {
            this.targetMob = null;
            return false;
        }
        /* Once the threat has been driven off the calf the job is done -- chasing it across the map is
         * how a defence turned into the herd running a predator to death. Slack of 4 blocks over
         * DEFEND_RADIUS so the mother does not disengage the instant the fight drifts a step. */
        if (this.targetMob != null && this.targetMob.distanceToSqr(this.protectTarget) > (DEFEND_RADIUS + 4D) * (DEFEND_RADIUS + 4D)) {
            this.mob.setTarget(null);
            this.targetMob = null;
            return false;
        }
        if (this.protectTarget.distanceTo(this.mob) > 12) {
            this.mob.setTarget(null);
            this.targetMob = null;
            this.mob.getNavigation().moveTo(this.protectTarget, 1);
            return false;
        }
        return super.canContinueToUse();
    }

    /* Frees the defender slot for a herd-mate and starts this animal's own re-engage cooldown, so one
     * fight cannot simply cycle a fresh trio of defenders through the cap. HerdEntity re-validates the
     * slots on its own tick as well, because a defender that dies or loses its target never gets here. */
    @Override
    public void stop() {
        if (this.mob instanceof ComplexMob defender && defender.herd != null) {
            defender.herd.releaseDefenderSlot(defender);
        }
        this.protectTarget = null;
        super.stop();
    }

    @Override
    protected double getFollowDistance() {
        return super.getFollowDistance() * 0.5D;
    }
}