package untamedwilds.entity.ai.target;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import untamedwilds.UntamedWilds;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.mammal.EntityCamel;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * A camel spitting at a carnivore that is threatening it or its herd. Purely defensive.
 * <p>
 * Formerly {@code BeAnAssTarget}, which was exactly what the name said: it picked anything under half
 * the camel's eco level (plus players) and spat at it roughly every ten seconds, forever. Three
 * separate things made that lethal rather than merely rude:
 * <ul>
 *   <li>the {@code nextInt(200)} roll runs every tick, so each camel spat about once per 10 s, and a
 *       herd is 8-14 animals;</li>
 *   <li>it set {@code huntingCooldown = 6000} on success but never read it back, so the five-minute
 *       cooldown it appeared to have did nothing at all;</li>
 *   <li>at 1 damage a spit is harmless once and fatal a hundred times over, so chickens, rabbits and
 *       calves were being ground down by camels that never even looked hostile.</li>
 * </ul>
 * The target list is now carnivores only -- see the predicate passed in {@link EntityCamel} -- and a
 * carnivore has to actually be threatening the herd before it is worth a mouthful. The projectile
 * itself deals no damage and skips {@code hurt()} entirely, so this cannot start a fight either.
 */
public class SpitAtPredatorTarget<T extends LivingEntity> extends TargetGoal {

    /** How close a predator has to come before it counts as a threat rather than as scenery. */
    private static final double THREAT_RANGE = 8D;
    /** Radius searched for herd-mates the predator might be after instead of this animal. */
    private static final double HERD_RADIUS = 16D;

    protected final Class<T> targetClass;
    protected final Sorter sorter;
    protected Predicate<? super T> targetEntitySelector;
    protected T targetEntity;

    public SpitAtPredatorTarget(ComplexMob creature, Class<T> classTarget, boolean checkSight, final Predicate<? super T> targetSelector) {
        super(creature, checkSight, true);
        this.targetClass = classTarget;
        this.sorter = new Sorter(creature);
        this.setFlags(EnumSet.of(Flag.TARGET));
        this.targetEntitySelector = (Predicate<T>) entity -> {
            if (targetSelector != null && !targetSelector.test(entity)) {
                return false;
            }
            if (!this.isThreatening(entity)) {
                return false;
            }
            return this.canAttack(entity, TargetingConditions.DEFAULT);
        };
    }

    /**
     * Whether this candidate is close enough and hostile enough to be worth spitting at. A predator
     * merely crossing the horizon is not: without this the goal fires on the first carnivore that
     * wanders into follow range, which is opportunistic, not defensive.
     */
    private boolean isThreatening(LivingEntity candidate) {
        if (this.mob.distanceToSqr(candidate) > THREAT_RANGE * THREAT_RANGE) {
            return false;
        }
        if (this.mob.getLastHurtByMob() == candidate) {
            return true;
        }
        if (!(candidate instanceof Mob hostile)) {
            return false;
        }
        LivingEntity prey = hostile.getTarget();
        if (prey == null) {
            return false;
        }
        if (prey == this.mob) {
            return true;
        }
        // Defence of the herd, not only of self -- a camel spits at the wolf on its neighbour too.
        return prey.getClass() == this.mob.getClass() && prey.distanceToSqr(this.mob) < HERD_RADIUS * HERD_RADIUS;
    }

    public boolean canUse() {
        if (!(this.mob instanceof EntityCamel)) {
            UntamedWilds.LOGGER.warn("Trying to run SpitAtPredatorTarget on a mob without a ranged attack");
            return false;
        }
        if (this.mob.isBaby()) {
            return false;
        }
        /* Honour the cooldown this goal has always set and never once read. That single omission is
         * what turned an occasional bit of flavour into a herd spitting several times a second. */
        if (this.mob instanceof ComplexMob complexMob) {
            if (complexMob.isTame() || complexMob.huntingCooldown != 0) {
                return false;
            }
        }
        /* Slower than the old 1-in-200 as well, so the wait INSIDE the cooldown is about half a minute
         * rather than the cooldown being the only brake on it. */
        if (this.mob.getRandom().nextInt(600) != 0) {
            return false;
        }
        List<T> list = this.mob.level.getEntitiesOfClass(this.targetClass, this.getTargetableArea(this.getFollowDistance()), this.targetEntitySelector);
        list.removeIf((Predicate<LivingEntity>) this::shouldRemoveTarget);

        if (list.isEmpty()) {
            return false;
        }
        else {
            list.sort(this.sorter);
            this.targetEntity = list.get(0);
            if (this.mob instanceof ComplexMob) {
                ((ComplexMob)this.mob).huntingCooldown = 6000;
            }
            return true;
        }
    }

    AABB getTargetableArea(double targetDistance) {
        return this.mob.getBoundingBox().inflate(targetDistance, 4.0D, targetDistance);
    }

    public void start() {
        this.mob.getLookControl().setLookAt(this.targetEntity);
        if (this.mob instanceof EntityCamel camel) {
            camel.performRangedAttack(this.targetEntity, 0.5F);
        }
        //this.mob.setTarget(this.targetEntity);
        super.start();
    }

    public boolean shouldRemoveTarget(LivingEntity entity) {
        if (entity instanceof Creeper) {
            return false; // Hardcoded Creepers out because they will absolutely destroy wildlife if targeted
        }
        /* Nothing helpless. A predator's cub is not a threat to a camel herd, and a half-dead one has
         * already lost whatever fight it picked -- both were part of the old attrition problem. */
        if (entity.isBaby() || entity.getHealth() < entity.getMaxHealth() / 2) {
            return true;
        }
        if (entity instanceof ComplexMob ctarget) {
            return (mob.getClass() == entity.getClass() && ((ComplexMob)mob).getVariant() == ctarget.getVariant()) || !ctarget.canBeTargeted();
        }
        return false;
    }

    public static class Sorter implements Comparator<Entity> {
        private final Entity entity;

        private Sorter(Entity entityIn)
        {
            this.entity = entityIn;
        }

        public int compare(Entity entity_1, Entity entity_2) {
            double dist_1 = this.entity.distanceToSqr(entity_1);
            double dist_2 = this.entity.distanceToSqr(entity_2);

            if (dist_1 < dist_2) {
                return -1;
            }
            else {
                return dist_1 > dist_2 ? 1 : 0;
            }
        }
    }
}
