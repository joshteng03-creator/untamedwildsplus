package untamedwilds.entity.ai;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigAmbience;
import untamedwilds.config.ConfigPerformance;
import untamedwilds.entity.ComplexMob;
import untamedwilds.util.EcologyTags;
import untamedwilds.util.PerfCounters;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class SmartAvoidGoal <T extends LivingEntity> extends AvoidEntityGoal<T> {

    protected ComplexMob taskOwner;
    protected final float avoidDistance;
    protected final TargetingConditions builtTargetSelector;
    /* The parent keeps its own copies of these two private, and they have to be reachable to apply the
     * stamina/wound factor to them. */
    protected final double farSpeed;
    protected final double nearSpeed;

    /** Ceiling on any dynamic widening of the detection radius, and the range the conditions allow. */
    protected static final double MAX_DETECT_SCALE = 2.0D;

    public SmartAvoidGoal(ComplexMob entityIn, Class<T> classToAvoidIn, float avoidDistanceIn, double farSpeedIn, double nearSpeedIn, final Predicate<LivingEntity> targetSelector) {
        super(entityIn, classToAvoidIn, avoidDistanceIn, farSpeedIn, nearSpeedIn, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
        this.taskOwner = entityIn;
        this.avoidDistance = avoidDistanceIn;
        this.farSpeed = farSpeedIn;
        this.nearSpeed = nearSpeedIn;
        /* Built at the widest distance any subclass can ask for, because TargetingConditions.range is a
         * hard clamp and would silently cancel out the sentinel and dilution bonuses in
         * HerdFleeGoal.detectDistance(). The AABB in canUse() is what actually sets the radius. */
        this.builtTargetSelector = TargetingConditions.forCombat().range(avoidDistanceIn * MAX_DETECT_SCALE)
                .selector(candidate -> targetSelector.test(candidate) || this.fleesAsJuvenile(candidate));
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * How far this animal can spot something worth running from. Constant for a solitary animal;
     * {@link HerdFleeGoal} widens it for herd size and for tall sentinel species.
     */
    protected double detectDistance() {
        return this.avoidDistance;
    }

    /**
     * Extra things a juvenile runs from, on top of whatever its species' own predicate lists.
     * <p>
     * Those predicates are written for adults: bears and dire wolves flee only what out-scores them on
     * eco level, which a bison bull that has decided to gore a cub does not necessarily do. The clause
     * that matters is "anything currently targeting me" -- that is the elephant or the bison in the bug
     * report, neither of which is a predator by any tag.
     * <p>
     * Players are deliberately excluded: cubs are how bears and wolves are tamed, and a cub that bolts
     * on sight cannot be.
     */
    private boolean fleesAsJuvenile(LivingEntity candidate) {
        if (!this.taskOwner.isBaby() || candidate instanceof Player) {
            return false;
        }
        if (EcologyTags.isPredator(candidate)) {
            return true;
        }
        return candidate instanceof Mob hostile && hostile.getTarget() == this.taskOwner;
    }

    /** How often, in ticks, the mob looks around for something to avoid. Prey animals want a shorter
     *  interval than the predators this goal was originally written for. */
    protected int getScanInterval() {
        return 40;
    }

    /** 2.8.0: whether a threat seen just beyond flight range puts this animal on alert (herd prey) or is ignored. */
    protected boolean alertsInBand() {
        return false;
    }

    /** When true, an existing combat target does not block flight -- the mob drops it and runs. */
    protected boolean shouldAbandonTarget() {
        return false;
    }

    /** 2.8.0: parity-proof replacement for a tickCount % 40 gate (see GoalTimer). */
    private final untamedwilds.util.GoalTimer scanTimer = new untamedwilds.util.GoalTimer(40, true);

    @Override
    public boolean canUse() {
        /* A juvenile looks around four times as often and, crucially, is never blocked by holding a
         * combat target. The target goals hand cubs targets they are barred from acting on, and the
         * check below then read that as "busy fighting" and refused to let them run -- which is the
         * whole reason cubs stood still and died. See SmartHurtByTargetGoal. */
        boolean juvenile = this.taskOwner.isBaby();
        int interval = juvenile ? Math.min(10, this.getScanInterval()) : this.getScanInterval();
        if (!this.scanReady(interval)) {
            return false;
        }
        if ((this.taskOwner.getTarget() != null && !this.shouldAbandonTarget() && !juvenile) || this.taskOwner.isSleeping() || this.taskOwner.getCommandInt() != 0 || this.taskOwner.isTame()) {
            return false;
        }

        double detect = this.detectDistance();
        /* 2.8.0 vigilance: ONE search out to the alert band; inside flight range the animal flees exactly as before,
         * in the band beyond it the herd goes on alert (AlertGoal) instead of ignoring the predator. */
        boolean vigilant = this.alertsInBand() && ConfigAmbience.herbivoreAlert.get();
        List<T> list = this.findThreats(vigilant ? detect * ConfigAmbience.alertRangeScale.get() : detect);
        if (vigilant && !list.isEmpty()) {
            AABB fleeBox = this.taskOwner.getBoundingBox().inflate(detect, 4f, detect);
            List<T> close = new ArrayList<>();
            for (T threat : list) {
                if (threat.getBoundingBox().intersects(fleeBox)) {
                    close.add(threat);
                }
            }
            if (close.isEmpty()) {
                this.taskOwner.raiseAlert(list.get(0), true);
                return false;
            }
            list = close;
        }
        if (list.isEmpty()) {
            return false;
        } else {
            this.toAvoid = list.get(0);
            Vec3 vec3d = DefaultRandomPos.getPosAway(this.taskOwner, 16, 7, new Vec3(this.toAvoid.getX(), this.toAvoid.getY(), this.toAvoid.getZ()));
            if (vec3d == null) {
                return false;
            } else if (this.toAvoid.distanceToSqr(vec3d.x, vec3d.y, vec3d.z) < this.toAvoid.distanceToSqr(this.mob)) {
                return false;
            } else {
                this.path = this.pathNav.createPath(vec3d.x, vec3d.y, vec3d.z, 0);
                return this.path != null;
            }
        }
    }

    /** Herd-synchronised window used while the shared scan is on (see {@link #scanReady}). */
    private final untamedwilds.util.GoalTimer herdScanTimer = new untamedwilds.util.GoalTimer(40, false);

    private boolean sharesHerdScan() {
        return ConfigPerformance.sharedHerdThreatScan.get() && this.taskOwner.herd != null && this.taskOwner.herd.creatureList.size() > 1;
    }

    /**
     * Whether this evaluation should look around. With the shared herd scan on, every member of a herd uses the
     * same window (game time + the herd's key), so they all ask within the same two ticks and one entity search
     * serves the whole herd. Otherwise each animal keeps its own staggered window.
     */
    private boolean scanReady(int interval) {
        if (this.sharesHerdScan()) {
            return this.herdScanTimer.readyAt(this.taskOwner.level.getGameTime() + this.taskOwner.herd.threatScanKey(), interval);
        }
        return this.scanTimer.ready(this.taskOwner, interval);
    }

    /**
     * Everything worth running from within {@code detect}. Identical results with and without the shared herd
     * scan: the shared search only replaces WHERE the candidates come from; this animal's own box and its own
     * targeting conditions (range, line of sight, selector) are applied to them exactly as getNearbyEntities would.
     */
    public List<T> findThreats(double detect) {
        this.taskOwner.perfFleeScans++;
        AABB own = this.taskOwner.getBoundingBox().inflate(detect, 4f, detect);
        if (this.sharesHerdScan()) {
            List<T> shared = this.taskOwner.herd.sharedThreatCandidates(this.avoidClass, own, this.avoidDistance * MAX_DETECT_SCALE, this.taskOwner.level);
            if (shared != null) {
                List<T> out = new ArrayList<>();
                for (T candidate : shared) {
                    if (candidate.getBoundingBox().intersects(own) && this.builtTargetSelector.test(this.taskOwner, candidate)) {
                        out.add(candidate);
                    }
                }
                return out;
            }
        }
        PerfCounters.inc(PerfCounters.Counter.FLEE_SCAN);
        return this.taskOwner.level.getNearbyEntities(this.avoidClass, this.builtTargetSelector, this.taskOwner, own);
    }

    /** The per-member scan, regardless of the toggle (GameTest equivalence check). */
    public List<T> findThreatsUnshared(double detect) {
        return this.taskOwner.level.getNearbyEntities(this.avoidClass, this.builtTargetSelector, this.taskOwner,
                this.taskOwner.getBoundingBox().inflate(detect, 4f, detect));
    }

    public double currentDetectDistance() {
        return this.detectDistance();
    }

    /**
     * Flight speed, scaled by how much run the animal has left in it. This is the one place the factor
     * needs to be applied, since every flight goal in the mod derives from this class.
     * <p>
     * A fresh, healthy animal still flees at the full configured speed and outruns everything that hunts
     * it -- that part was never wrong. What was wrong is that it did so indefinitely, so no predator
     * could ever finish a chase and hunts ran until the prey healed. Prey now wins the sprint and loses
     * the marathon.
     */
    protected double fleeSpeed(double base) {
        return base * this.taskOwner.getFleeSpeedFactor();
    }

    @Override
    public void start() {
        this.pathNav.moveTo(this.path, this.fleeSpeed(this.farSpeed));
    }

    /* Mirrors AvoidEntityGoal.tick(), which switches to the sprint speed inside 7 blocks, but through
     * fleeSpeed() so exhaustion and injury are accounted for. */
    @Override
    public void tick() {
        if (this.mob.distanceToSqr(this.toAvoid) < 49.0D) {
            this.mob.getNavigation().setSpeedModifier(this.fleeSpeed(this.nearSpeed));
        } else {
            this.mob.getNavigation().setSpeedModifier(this.fleeSpeed(this.farSpeed));
        }
    }
}