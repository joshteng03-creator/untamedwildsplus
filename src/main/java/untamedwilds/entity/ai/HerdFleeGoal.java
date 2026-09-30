package untamedwilds.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ComplexMob;
import untamedwilds.util.EcologyTags;

import java.util.function.Predicate;

/**
 * Flight for herd prey. {@link SmartAvoidGoal} makes ONE animal back away from a threat, which is what
 * a solitary predator wants; a herd needs the opposite -- the whole group has to break at once, and it
 * has to break instead of turning to fight.
 * <p>
 * Two behaviours on top of the parent goal:
 * <ul>
 *   <li><b>The herd panics together.</b> On {@code start()} every member of {@code herd.creatureList}
 *       is given a flee cooldown, has its combat target cleared and is sent running in the same
 *       direction. This mirrors {@link untamedwilds.entity.ai.target.HuntPackMobTarget}, which already
 *       shares a target across a pack so it hunts as one animal.</li>
 *   <li><b>Running beats fighting.</b> {@code shouldAbandonTarget()} is true, so an antelope that has
 *       been bitten drops the wolf as a target and runs rather than squaring up to it -- unless there
 *       is nowhere to run, in which case no escape path is found, the goal declines, and the animal
 *       fights at bay with whatever goals remain.</li>
 * </ul>
 */
public class HerdFleeGoal<T extends LivingEntity> extends SmartAvoidGoal<T> {

    private static final int PANIC_TICKS = 200;
    /** Speed a panicking animal re-paths at, before its own stamina/wound factor is applied. */
    private static final double PANIC_SPEED = 1.6D;

    /** Extra detection range this animal gets for being a tall-standing sentinel species. */
    private final double detectBonus;

    public HerdFleeGoal(ComplexMob entityIn, Class<T> classToAvoidIn, float avoidDistanceIn, double farSpeedIn, double nearSpeedIn, final Predicate<LivingEntity> targetSelector) {
        this(entityIn, classToAvoidIn, avoidDistanceIn, farSpeedIn, nearSpeedIn, targetSelector, 1.0D);
    }

    /**
     * @param detectBonusIn multiplier on the detection distance. Giraffids pass
     *                      {@code sentinel_detect_bonus}: a browser that stands five metres up sees a
     *                      lion long before a grazer with its head down does, and via the alarm network
     *                      below the whole plains gets to hear about it.
     */
    public HerdFleeGoal(ComplexMob entityIn, Class<T> classToAvoidIn, float avoidDistanceIn, double farSpeedIn, double nearSpeedIn, final Predicate<LivingEntity> targetSelector, double detectBonusIn) {
        super(entityIn, classToAvoidIn, avoidDistanceIn, farSpeedIn, nearSpeedIn, targetSelector);
        this.detectBonus = detectBonusIn;
    }

    /**
     * Many eyes see further. A big herd notices a predator sooner than a lone animal does, which is
     * most of what safety in numbers actually consists of -- together with the straggler bias in
     * {@code HuntMobTarget.Sorter}, it is what lets herd size protect a herd <b>without</b> the herd
     * having to mob the predator to death.
     */
    @Override
    protected double detectDistance() {
        double scale = this.detectBonus;
        if (this.taskOwner.herd != null) {
            scale += Math.min(0.5D, this.taskOwner.herd.creatureList.size() * 0.03D);
        }
        return this.avoidDistance * Math.min(MAX_DETECT_SCALE, scale);
    }

    /* Prey cannot afford to check for predators only every two seconds; a dire wolf covers most of the
     * avoid radius in that time. */
    @Override
    protected int getScanInterval() {
        return 10;
    }

    /**
     * The animals the herd has committed to the fight are exempt from all of this.
     * <p>
     * Flight and defence were previously mutually exclusive by accident: {@code panic()} clears every
     * member's target and {@code canUse()} re-clears it for the whole {@link #PANIC_TICKS} window, rescanned
     * every ten ticks. Since a predator is *seen* before it bites, the panic always came first, so a
     * defender's target was wiped within ten ticks of it claiming a slot and the herd's group defence
     * could never actually engage. The defender cap already bounds how many animals this can be.
     */
    private static boolean isDefending(ComplexMob animal) {
        return animal.herd != null && animal.herd.isDefender(animal);
    }

    @Override
    protected boolean shouldAbandonTarget() {
        return !isDefending(this.taskOwner);
    }

    @Override
    public boolean canUse() {
        // Keep running while the panic is still on, even after the predator drops out of sight.
        if (this.taskOwner.fleeCooldown > 0 && this.taskOwner.getTarget() != null && !isDefending(this.taskOwner)) {
            this.taskOwner.setTarget(null);
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        /* An animal that took a defender slot mid-flight turns and holds. Without this the goal would
         * keep running on fleeCooldown alone -- canUse() is not consulted once a goal has started -- and
         * would drag the defender away from the fight it had just been committed to. */
        if (isDefending(this.taskOwner)) {
            return false;
        }
        return this.taskOwner.fleeCooldown > 0 || super.canContinueToUse();
    }

    /* AvoidEntityGoal.tick() only adjusts speed; if the escape path runs out while the herd is still
     * panicking the animal would simply stand there. Re-path for as long as the panic lasts. */
    @Override
    public void tick() {
        if (this.taskOwner.getNavigation().isDone() && this.toAvoid != null && this.taskOwner.fleeCooldown > 0) {
            Vec3 escape = escapeAlong(this.taskOwner, this.taskOwner.fleeHeading, this.toAvoid);
            if (escape != null) {
                this.taskOwner.getNavigation().moveTo(escape.x, escape.y, escape.z, this.fleeSpeed(PANIC_SPEED));
            }
            return;
        }
        super.tick();
    }

    @Override
    public void start() {
        /* Only the animal that STARTS a panic raises the alarm. Every herd-mate that then sees the same
         * predator on its own 10-tick scan used to re-alert the whole herd and the alarm network, and
         * panic() re-pathed everyone each time: in a 20-strong herd with neighbours that was over a
         * thousand pathfinds within half a second, repeated for as long as the threat stayed in view. */
        boolean alreadyPanicking = this.taskOwner.fleeCooldown > 0;
        this.taskOwner.setTarget(null);
        this.taskOwner.fleeCooldown = PANIC_TICKS;
        if (!alreadyPanicking) {
            this.alertHerd();
        }
        super.start();
        /* The spotter runs the herd's way too, not only the way AvoidEntityGoal picked for it alone. */
        if (this.taskOwner.fleeHeading != null && this.toAvoid != null) {
            Vec3 escape = escapeAlong(this.taskOwner, this.taskOwner.fleeHeading, this.toAvoid);
            if (escape != null) {
                this.taskOwner.getNavigation().moveTo(escape.x, escape.y, escape.z, this.fleeSpeed(PANIC_SPEED));
            }
        }
    }

    /* The animal that spotted the predator drags the rest of the herd with it. Herd-mates are pathed
     * directly rather than being left to notice the threat on their own -- otherwise the herd peels
     * away one animal at a time and the stragglers read as if they had not seen anything. */
    private void alertHerd() {
        if (this.toAvoid == null) {
            return;
        }
        /* One heading for the whole herd: away from the threat, measured from the herd's centre. Every
         * animal used to run to its own random point away from the predator, which scattered the herd --
         * and a scattered herd is a row of stragglers, exactly what straggler_bias then picks off. Running
         * the same way keeps the herd together, which is what real herds do and what makes them safe. */
        Vec3 heading = this.taskOwner.herd != null ? awayFrom(herdCentre(this.taskOwner), this.toAvoid) : awayFrom(this.taskOwner.position(), this.toAvoid);
        this.taskOwner.fleeHeading = heading;
        if (this.taskOwner.herd != null) {
            for (ComplexMob member : this.taskOwner.herd.creatureList) {
                if (member == this.taskOwner) {
                    continue;
                }
                this.panic(member, heading);
            }
        }
        this.alertNeighbours();
    }

    /**
     * The mixed-species alarm network. A zebra that bolts takes the wildebeest and the impala with it,
     * because a predator is a predator whoever spotted it first -- and because a warning is worth
     * nothing if only your own species can hear it.
     * <p>
     * Only herbivores are alerted; a predator standing nearby has no reason to run from another
     * predator's approach, and telling it to would hand prey a free escape from anything hunting it.
     */
    private void alertNeighbours() {
        double radius = ConfigGamerules.alarmNetworkRadius.get();
        if (radius <= 0 || this.toAvoid == null) {
            return;
        }
        for (ComplexMob neighbour : this.taskOwner.level.getEntitiesOfClass(ComplexMob.class,
                this.taskOwner.getBoundingBox().inflate(radius, 8D, radius))) {
            if (neighbour == this.taskOwner || EcologyTags.isPredator(neighbour)
                    || (this.taskOwner.herd != null && this.taskOwner.herd.containsCreature(neighbour))) {
                continue;
            }
            // A neighbour of another species runs away from the threat from where IT stands.
            this.panic(neighbour, awayFrom(neighbour.position(), this.toAvoid));
        }
    }

    private static Vec3 herdCentre(ComplexMob animal) {
        double x = 0, y = 0, z = 0;
        int n = 0;
        for (ComplexMob member : animal.herd.creatureList) {
            if (member.isAlive()) {
                x += member.getX();
                y += member.getY();
                z += member.getZ();
                n++;
            }
        }
        return n == 0 ? animal.position() : new Vec3(x / n, y / n, z / n);
    }

    /** Horizontal unit vector pointing from the threat through {@code from}. */
    private static Vec3 awayFrom(Vec3 from, LivingEntity threat) {
        Vec3 away = new Vec3(from.x - threat.getX(), 0, from.z - threat.getZ());
        if (away.lengthSqr() < 1.0E-4D) {
            double angle = threat.getRandom().nextDouble() * Math.PI * 2D;
            away = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        }
        return away.normalize();
    }

    /** A reachable point about 16 blocks along {@code heading}; falls back to plain "away from the threat" if the heading is blocked. */
    private static Vec3 escapeAlong(ComplexMob animal, Vec3 heading, LivingEntity threat) {
        Vec3 escape = heading == null ? null
                : DefaultRandomPos.getPosTowards(animal, 16, 7, animal.position().add(heading.scale(16D)), Math.PI / 2D);
        return escape != null ? escape : DefaultRandomPos.getPosAway(animal, 16, 7, threat.position());
    }

    private void panic(ComplexMob animal, Vec3 heading) {
        // A herd-mate holding the line is not swept up in the stampede; see isDefending.
        if (!animal.isAlive() || animal.isTame() || this.toAvoid == null || isDefending(animal)) {
            return;
        }
        // Already running from this panic: keep the escape path it has rather than recomputing one.
        if (animal.fleeCooldown > 0 && !animal.getNavigation().isDone()) {
            return;
        }
        animal.setTarget(null);
        animal.fleeCooldown = PANIC_TICKS;
        if (animal.isSleeping()) {
            animal.setSleeping(false);
        }
        animal.fleeHeading = heading;
        Vec3 escape = escapeAlong(animal, heading, this.toAvoid);
        if (escape != null) {
            // Each animal runs at its OWN condition, so the blown and the wounded fall behind -- which
            // is how a pack picks which animal it is going to get.
            animal.getNavigation().moveTo(escape.x, escape.y, escape.z, PANIC_SPEED * animal.getFleeSpeedFactor());
        }
    }
}
