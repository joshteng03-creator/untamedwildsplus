package untamedwilds.entity.ai.target;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.config.EcologyMode;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.HerdEntity;
import untamedwilds.util.EcologyTags;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class HuntMobTarget<T extends LivingEntity> extends TargetGoal {
    /** How long a hunter keeps chasing something it cannot currently see. */
    private static final int UNSEEN_MEMORY_TICKS = 200;
    /** Hard leash on a committed chase, in blocks; the commitment timer is what normally ends it. */
    private static final double CHASE_RANGE = 48D;
    protected final Class<T> targetClass;
    protected final Sorter sorter;
    protected Predicate<? super T> targetEntitySelector;
    protected final int threshold;
    private final boolean isCannibal;

    /* Default threshold is 30 ("only hunt when hungry"), NOT the old 200 ("always hunt"). Every call
     * site that wanted gated hunting had to remember to pass 30 explicitly, and the three that forgot
     * -- shark, football fish and giant salamander -- hunted around the clock. Making the safe value
     * the default means the next predator added cannot reintroduce that by omission. */
    public HuntMobTarget(ComplexMob creature, Class<T> classTarget, boolean checkSight, boolean isCannibal, final Predicate<LivingEntity> targetSelector) {
        this(creature, classTarget, checkSight, 30, isCannibal, targetSelector);
    }

    public HuntMobTarget(ComplexMob creature, Class<T> classTarget, boolean checkSight, int hungerThreshold, boolean isCannibal, final Predicate<LivingEntity> targetSelector) {
        super(creature, checkSight, true);
        this.targetClass = classTarget;
        this.sorter = new Sorter(creature);
        this.setFlags(EnumSet.of(Flag.TARGET));
        this.threshold = hungerThreshold;
        this.isCannibal = isCannibal;
        this.targetEntitySelector = entity -> isValidTarget(entity, targetSelector);
    }

    protected boolean isValidTarget(LivingEntity entity, @Nullable Predicate<LivingEntity> predicate) {
        /* Cheapest test first. The scan box is a cuboid, so its corners hold candidates that canAttack's
         * range check below rejects anyway -- after the diet lookup, the herd walk and the rout memory had
         * already been paid for. Never stricter than canAttack, whose range is this times visibility. */
        double range = this.getFollowDistance();
        if (this.mob.distanceToSqr(entity) > range * range) {
            return false;
        }
        if (entity instanceof Creeper || entity.equals(this.mob) || (!ConfigGamerules.attackUndead.get() && entity.getMobType() == MobType.UNDEAD) || (entity instanceof ComplexMob cmob && !cmob.canBeTargeted()) || (predicate != null && !predicate.test(entity))) {
            return false;
        }
        if (!this.isCannibal && this.mob.getClass() == entity.getClass() && this.mob instanceof ComplexMob attacker && entity instanceof ComplexMob defender) {
            if (attacker.getVariant() == defender.getVariant()) {
                return false;
            }
        }
        // Diet filter. Only applies when the predator's JSON declares one -- an undeclared predator
        // keeps the legacy "anything weaker than me" behaviour supplied via the targetSelector
        // predicate, so this rolls out per species without touching the untagged ones.
        if (EcologyTags.hasDiet(this.mob) && !EcologyTags.isPreferredPrey(this.mob, entity)) {
            return false;
        }
        if (this.isFoodHunt() && isHerdTooSmallToHunt(entity)) {
            return false;
        }
        /* A herd that has already routed this predator is left alone for a while. Without the memory a
         * pack that fled re-acquired the same herd within seconds and walked straight back into the
         * fight it had just lost -- the same lesson as the hunt commitment, that a decision living only
         * on a goal does not survive the goal being stopped. */
        if (this.mob instanceof ComplexMob hunter && hunter.isAvoiding(entity)) {
            return false;
        }
        return canAttack(entity, TargetingConditions.forCombat().range(getFollowDistance()));
    }

    /* Hard floor against predation locally wiping a species out. Now that hunts actually end in kills,
     * the hunger gate and the post-kill cooldown bound the RATE of predation but nothing bounds the
     * final few animals: a herd of two, with no third member left to breed with, is one bad afternoon
     * from gone. A starving predator ignores the floor -- it is a balance rule, not physics, and an
     * animal about to die of hunger should still take the shot. Solitary prey has no herd at all and
     * stays huntable, which is correct. */
    private boolean isHerdTooSmallToHunt(LivingEntity entity) {
        int floor = ConfigGamerules.predatorHerdFloor.get();
        if (floor <= 0 || !(entity instanceof ComplexMob prey) || prey.herd == null) {
            return false;
        }
        if (this.mob instanceof ComplexMob hunter && hunter.isStarving()) {
            return false;
        }
        /* The floor protects breeding stock. A calf is not breeding stock -- it is the part of the herd
         * predation is supposed to take -- so shielding it here removed the only pressure the population
         * was ever under. Its parents are still counted, so the herd itself keeps the protection. */
        if (entity.isBaby()) {
            return false;
        }
        /* A species that is solitary BY DESIGN is not a herd being wiped out; it never had a herd to
         * lose. Every IPackEntity gets a herd object even when its groupCount is 1, so ground_sloth (1)
         * and glyptodont (2) sat permanently at or under the floor and could not be hunted by anything
         * at all. Compare against the species' configured maximum, not the current membership, or a
         * genuine herd that has been thinned would lose its protection at exactly the wrong moment. */
        if (prey.herd.getMaxSize() <= floor) {
            return false;
        }
        int living = 0;
        for (ComplexMob member : prey.herd.creatureList) {
            if (member.isAlive()) {
                living++;
            }
        }
        return living <= floor;
    }

    public boolean canUse() {
        if (this.mob.isBaby() || this.mob.getHealth() < this.mob.getMaxHealth() / 3) {
            return false;
        }
        // Gate on ComplexMob, not ComplexMobTerrestrial: hunger now lives on the base class, so this
        // finally covers ComplexMobAquatic (shark, football fish) and ComplexMobAmphibious (giant
        // salamander), which previously fell straight through this check and hunted continuously.
        if (this.mob instanceof ComplexMob hunter) {
            // Zoo mode pulls the threshold down to "starving only" rather than removing predation, so
            // the hunger and feeding systems still mean something in an exhibit.
            if (hunter.isTame() || hunter.getHunger() > EcologyMode.huntHungerThreshold(hunter, this.threshold))
                return false;
            /* Re-acquire the animal this predator is still committed to, ignoring the cooldown. A
             * retaliation goal outranks this one and takes the target slot the moment the prey bites
             * back; when that goal later expires it nulls the target, and the cooldown charged at hunt
             * start then blocked this goal from picking the chase back up. The predator would stand
             * there, still hungry, while the animal it had brought to 10% health walked off and healed.
             * The commitment timer -- not the cooldown -- is what decides when a hunt is over. */
            if (this.isFoodHunt() && hunter.huntCommitTicks > 0 && hunter.huntVictim != null && hunter.huntVictim.isAlive()
                    && this.mob.distanceToSqr(hunter.huntVictim) < CHASE_RANGE * CHASE_RANGE) {
                this.targetMob = hunter.huntVictim;
                return true;
            }
            if (hunter.huntingCooldown != 0)
                return false;
            /* A fresh search is a follow-range entity scan plus the odds check's own scan, and nothing here
             * throttled it: every hungry predator paid both every other tick. Vanilla's target goals roll a
             * ~10-tick random interval; re-acquiring a committed victim (above) stays immediate. */
            if (this.mob.getRandom().nextInt(reducedTickDelay(10)) != 0)
                return false;
            /* Do not START a fight the group cannot win. Checked here and NOT in canContinueToUse: a
             * predator already committed would otherwise abandon its quarry the instant the first
             * defender turned up, which is every hunt. Once committed, the commitment timer and the
             * pack's morale decide when to leave. */
            if (this.isFoodHunt() && !hunter.oddsAreAcceptable()) {
                return false;
            }
        }

        List<T> list = this.mob.level.getEntitiesOfClass(this.targetClass, this.getTargettableArea(this.getFollowDistance()), this.targetEntitySelector);
        if (list.isEmpty())
            return false;

        this.targetMob = this.sorter.best(list);
        return true;
    }

    AABB getTargettableArea(double targetDistance) {
        return this.mob.getBoundingBox().inflate(targetDistance, 4.0D, targetDistance);
    }

    /* Whether running this goal counts as hunting for food. ProtectChildrenTarget inherits from this
     * class purely to reuse the target scan -- a mother driving a wolf off her calf is not feeding
     * herself, so it must neither burn her hunting cooldown nor claim the fight-to-the-death exemption
     * that activeHunt grants. */
    protected boolean isFoodHunt() {
        return true;
    }

    /* Whether running this goal should burn the 6000-tick cooldown that stops a predator hunting
     * again. Kept separate from isFoodHunt() because the two subclasses want different answers:
     * HippoTerritoryTargetGoal is not feeding but DOES need the cooldown, or a hippo re-targets every
     * intruder continuously; ProtectChildrenTarget wants neither, since a predator mother who chased
     * something off her cubs was being locked out of hunting for five minutes as a side effect. */
    protected boolean usesHuntCooldown() {
        return this.isFoodHunt();
    }

    public void start() {
        if (!this.mob.getNavigation().isDone())
            this.mob.getNavigation().stop();
        if (this.mob instanceof ComplexMob hunter && this.usesHuntCooldown()) {
            /* A food hunt pays only the short failed-attempt cooldown up front; the full lockout is
             * charged in ComplexMob.satiateFromKill, on the kill itself. Charging 6000 here meant a
             * single interruption -- and something interrupts nearly every hunt, since the retaliation
             * goals outrank this one -- locked the predator out for five minutes, so it could never come
             * back to finish the animal it had already brought to 10% health. Non-food users
             * (HippoTerritoryTargetGoal) keep the long cooldown, which is what stops a hippo
             * re-targeting every intruder continuously. */
            hunter.huntingCooldown = this.isFoodHunt() ? ConfigGamerules.predatorFailedHuntCooldown.get() : ConfigGamerules.predatorKillCooldown.get();
        }
        if (this.mob instanceof ComplexMob hunter && this.isFoodHunt()) {
            /* Marks this as a hunt for food, which exempts the predator from the low-health break-off
             * in ComplexMob.isRouted(). Every other kind of fight ends with both parties walking away;
             * if hunts did too, a predator could never land a kill, never call satiateFromKill, and
             * would starve -- which is the opposite of what the break-off is meant to achieve. */
            hunter.activeHunt++;
            /* The commitment is the part that actually survives contact. activeHunt only lasts as long
             * as THIS goal owns the target slot, and every retaliation goal outranks it, so the
             * exemption used to vanish the moment the prey fought back. */
            hunter.commitToHunt(this.targetMob);
            /* Prey that breaks line of sight for one tick used to end the hunt outright, because
             * TargetGoal defaults unseenMemoryTicks to 0 and this goal is built with mustSee. A predator
             * remembers where its quarry went. */
            this.unseenMemoryTicks = UNSEEN_MEMORY_TICKS;
        }
        this.mob.setTarget(this.targetMob);
        super.start();
    }

    public void stop() {
        if (this.mob instanceof ComplexMob hunter && this.isFoodHunt() && hunter.activeHunt > 0) {
            hunter.activeHunt--;
        }
        super.stop();
    }

    /* Drop the chase once the predator is no longer hungry. Without this a predator that fed mid-hunt
     * (a pack-mate's kill, or scavenging) still ran its current victim down, so the "one kill per
     * hunger cycle" rule leaked an extra death every time. */
    public boolean canContinueToUse() {
        if (this.mob instanceof ComplexMob hunter && hunter.getHunger() > EcologyMode.huntHungerThreshold(hunter, this.threshold)) {
            return false;
        }
        /* While committed, hold the target past FOLLOW_RANGE and through broken line of sight. TargetGoal
         * drops a target the moment it leaves follow range (12 blocks for several predators) and prey
         * flees 16 blocks at a time, so the parent check ended almost every chase within a second or two
         * of it starting. The commitment timer is the real leash, and CHASE_RANGE only stops a predator
         * tracking something across a continent. */
        if (this.mob instanceof ComplexMob hunter && hunter.isCommittedTo(this.targetMob)
                && this.targetMob.isAlive() && this.mob.distanceToSqr(this.targetMob) < CHASE_RANGE * CHASE_RANGE) {
            this.mob.setTarget(this.targetMob);
            return true;
        }
        return super.canContinueToUse();
    }

    /* Ranks candidates by distance WEIGHTED by dietary preference, local abundance and condition,
     * rather than by raw distance. Three separate ecological ideas share this one comparator:
     *
     *  - DIET: a predator walks past an off-diet animal to reach the prey it actually hunts.
     *  - SWITCHING: it prefers whichever of its prey species is locally COMMON. Without this, "take
     *    whatever is nearest that I can eat" removes prey species one at a time until only the most
     *    abundant survives; with it, a species hunted down to a few animals is largely ignored and
     *    recovers. This is the single most important stabiliser in the whole system.
     *  - CONDITION: wounded, blown and separated animals are taken first. That is what makes a large
     *    herd protective WITHOUT the herd having to fight the predator off -- the ecological answer to
     *    predators being mobbed to death.
     *
     * With no diet declared, prey_switching_strength 0 and straggler_bias 0, every weight is 1.0 and
     * this degrades exactly to the original distance sort. */
    public static class Sorter implements Comparator<Entity> {
        private final Entity entity;

        private Sorter(Entity entityIn) {
            this.entity = entityIn;
        }

        private double weightedDistance(Entity target) {
            double dist = this.entity.distanceToSqr(target);
            if (this.entity instanceof LivingEntity predator && target instanceof LivingEntity prey) {
                return dist / (EcologyTags.preference(predator, prey) * conditionBias(prey));
            }
            return dist;
        }

        /* Treats an animal that is already losing as if it were closer. Weighted by straggler_bias, and
         * multiplicative so a wounded animal that is ALSO cut off from its herd is the obvious choice. */
        private static double conditionBias(LivingEntity prey) {
            double bias = ConfigGamerules.stragglerBias.get();
            if (bias <= 0) {
                return 1.0D;
            }
            double weight = 1.0D;
            if (prey.getHealth() < prey.getMaxHealth() * ComplexMob.WOUNDED_THRESHOLD) {
                weight *= bias;
            }
            if (prey instanceof ComplexMob animal) {
                // Blown: it has spent its sprint and cannot outrun anything any more.
                if (animal.fleeStamina <= 0) {
                    weight *= bias;
                }
                /* Separated from the group. The single animal at the edge of a herd is the one that
                 * gets taken, and this is what a herd's safety-in-numbers actually consists of. */
                HerdEntity herd = animal.herd;
                if (herd != null && herd.getLeader() != null && herd.creatureList.size() > 1
                        && animal.distanceToSqr(herd.getLeader()) > herd.getRadius() * herd.getRadius()) {
                    weight *= bias;
                }
                /* Past its prime. Predators taking the old is the other half of a working age structure:
                 * senescence on its own only adds deaths, whereas this turns those animals into food and
                 * takes the pressure off breeding-age adults. Expressed here rather than as an attribute
                 * penalty on purpose -- lowering an old animal's MAX_HEALTH would drag it through
                 * getEcoLevel and silently re-sort every targeting predicate in the mod. */
                if (animal.isSenescent()) {
                    weight *= bias;
                }
            }
            return weight;
        }

        public int compare(Entity entity_1, Entity entity_2) {
            return Double.compare(this.weightedDistance(entity_1), this.weightedDistance(entity_2));
        }

        /* The lowest-weighted candidate in one pass: the first element list.sort(this) would have put first,
         * but each weight -- a census lookup and a herd walk -- is computed once instead of twice per
         * comparison. */
        public <E extends Entity> E best(List<E> candidates) {
            E best = null;
            double bestWeight = Double.MAX_VALUE;
            for (E candidate : candidates) {
                double weight = this.weightedDistance(candidate);
                if (best == null || weight < bestWeight) {
                    best = candidate;
                    bestWeight = weight;
                }
            }
            return best;
        }
    }
}
