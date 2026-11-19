package untamedwilds.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import untamedwilds.config.ConfigAmbience;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.entity.HerdEntity;
import untamedwilds.util.EntityUtils;
import untamedwilds.util.PerfCounters;
import untamedwilds.util.WaterFinder;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Cosmetic waterhole visits (2.8.0, P5). Every so often (about once a day, jittered) an animal walks to water it can
 * reach from a bank, faces it, lowers its head (the type's DRINK animation) and drinks. There is NO thirst stat: an
 * animal that finds no water simply tries again later, and nothing about its health or hunger changes.
 * <p>
 * Herds remember the waterhole they last drank at, and when the leader goes to drink the rest follow shortly after,
 * spread along the bank -- that gathering at the water is most of what makes it read as a waterhole. All the
 * searching is bounded ({@link WaterFinder}) and gated on a simple timer, so an animal that is not due to drink costs
 * one comparison per goal evaluation.
 */
public class DrinkGoal extends Goal {

    private static final int TRAVEL_BUDGET = 600;
    private static final int DRINK_TICKS = 100;
    /** Within this many blocks of the stand point the animal steers straight at it instead of pathing. */
    private static final double FINAL_APPROACH = 2.5D;
    /** How far past the bank edge the muzzle (or trunk tip) is aimed. */
    private static final double MUZZLE_OVER_WATER = 0.35D;
    private int approachTicks;
    private int repathCooldown;

    private final ComplexMobTerrestrial mob;
    @Nullable
    private WaterFinder.Spot spot;
    @Nullable
    private Vec3 standAt;
    private int ticks;
    private boolean drinking;

    public DrinkGoal(ComplexMobTerrestrial mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /** Ticks until this animal's next visit, jittered x0.75 .. x1.5 so a herd does not run on one clock. */
    public static long nextInterval(java.util.Random random) {
        int interval = ConfigAmbience.drinkIntervalTicks.get();
        return (long) (interval * (0.75D + random.nextDouble() * 0.75D));
    }

    private boolean busy() {
        return this.mob.isInWater() || this.mob.isSleeping() || this.mob.getTarget() != null || this.mob.fleeCooldown > 0
                || this.mob.isAlert() || this.mob.getCommandInt() != 0 || this.mob.isLeashed() || this.mob.isVehicle()
                || this.mob.isPassenger() || this.mob.isNursing() || !this.mob.canMove();
    }

    @Override
    public boolean canUse() {
        if (!ConfigAmbience.drinkVisits.get() || ConfigAmbience.drinkIntervalTicks.get() <= 0) {
            return false;
        }
        long now = this.mob.level.getGameTime();
        if (this.mob.nextDrinkTime < 0L) {
            // first sight of this animal (new, or loaded from a world saved before 2.8.0): a random point in the cycle,
            // so an updated world does not send every animal to the water at once
            this.mob.nextDrinkTime = now + this.mob.getRandom().nextInt(Math.max(1, ConfigAmbience.drinkIntervalTicks.get()));
            return false;
        }
        if (now < this.mob.nextDrinkTime || this.busy()) {
            return false;
        }
        this.spot = this.findWater();
        if (this.spot == null) {
            this.mob.nextDrinkTime = now + 2400 + this.mob.getRandom().nextInt(2400);
            HerdEntity herd = this.mob.herd;
            if (herd != null && herd.waterhole != null && ++herd.waterholeMisses >= 3) {
                herd.waterhole = null;        // the pond dried up, was built over, or is out of reach: forget it
                herd.waterholeMisses = 0;
            }
            return false;
        }
        this.standAt = standPoint(this.spot, this.mob);
        return true;
    }

    @Nullable
    private WaterFinder.Spot findWater() {
        HerdEntity herd = this.mob.herd;
        if (herd != null && herd.waterhole != null && herd.waterhole.distSqr(this.mob.blockPosition()) < 64 * 64) {
            // the remembered waterhole: random banks around it, so the herd spreads out along the shore
            WaterFinder.Spot near = WaterFinder.sampleBank(this.mob.level, herd.waterhole, 6, 12, this.mob.getRandom());
            if (near == null) {
                near = WaterFinder.bankAt(this.mob.level, herd.waterhole, this.mob.getRandom());
            }
            if (near != null) {
                return near;
            }
        }
        return WaterFinder.findBank(this.mob.level, this.mob.blockPosition(), ConfigAmbience.drinkSearchRadius.get(), 3, this.mob.getRandom());
    }

    /**
     * Where the animal's centre should stand: on the bank, backed away from the water so that its muzzle -- the
     * solved DRINK reach, scaled to this animal -- lands over the water rather than its whole body walking in.
     */
    public static Vec3 standPoint(WaterFinder.Spot spot, ComplexMobTerrestrial mob) {
        double reach = mob.getDrinkReach();
        Vec3 water = Vec3.atBottomCenterOf(spot.water.above());
        Vec3 toWater = new Vec3(spot.toWater.getStepX(), 0, spot.toWater.getStepZ());
        // the muzzle lands MUZZLE_OVER_WATER past the bank edge (the water block's near face is 0.5 from its centre),
        // so the arrival tolerance can never leave it drinking from the grass
        Vec3 p = water.subtract(toWater.scale(0.5D - MUZZLE_OVER_WATER + reach));
        return new Vec3(p.x, spot.bank.getY() + 1, p.z);
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.approachTicks = 0;
        this.repathCooldown = 20;
        this.drinking = false;
        this.pathToStand();
    }

    private void pathToStand() {
        if (this.standAt != null) {
            this.mob.getNavigation().moveTo(this.standAt.x, this.standAt.y, this.standAt.z, 1.0D);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.spot == null || this.busy()) {
            return false;
        }
        return this.drinking ? this.ticks < DRINK_TICKS : this.ticks < TRAVEL_BUDGET;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.spot == null || this.standAt == null) {
            return;
        }
        Vec3 water = Vec3.atCenterOf(this.spot.water);
        if (!this.drinking) {
            double dx = this.standAt.x - this.mob.getX(), dz = this.standAt.z - this.mob.getZ();
            double d2 = dx * dx + dz * dz;
            // tight: the stand point already backs the animal off by its solved muzzle reach, so stopping a block short
            // would leave it drinking from the grass
            double arrive = 0.3D;           // reachable: the last stretch is steered straight at the point
            if (d2 < arrive * arrive) {
                this.beginDrinking();
            } else if (d2 < FINAL_APPROACH * FINAL_APPROACH) {
                // The last stretch is walked STRAIGHT, not pathed. moveTo() plans to block accuracy 1 and a wide
                // animal counts a waypoint reached within half its width, so a bison's path ends up to ~1.8 blocks
                // short; re-pathing from there returns a path that is already "done" and the animal froze in place.
                this.mob.getNavigation().stop();
                this.mob.getMoveControl().setWantedPosition(this.standAt.x, this.standAt.y, this.standAt.z, 1.0D);
                if (++this.approachTicks > 60) {
                    if (d2 < 1.2D * 1.2D) {
                        this.beginDrinking();   // as close as the bank lets it get: still within reach of the water
                    } else {
                        this.spot = null;       // blocked short of the bank: give up, stop() schedules a retry
                    }
                }
            } else if (this.mob.getNavigation().isDone() && --this.repathCooldown <= 0) {
                this.repathCooldown = 20;      // never re-path every tick: a failed path is just as failed next tick
                this.pathToStand();
            }
            return;
        }
        this.faceWater(water);
        if (this.ticks == 30 || this.ticks == 50 || this.ticks == 70) {
            SoundEvent sound = this.mob.getDrinkSound();
            if (sound != null) {
                this.mob.playSound(sound, 0.6F, this.mob.voicePitch());
            }
            if (this.mob.level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SPLASH, water.x, this.spot.water.getY() + 0.95D, water.z, 2, 0.25D, 0.0D, 0.25D, 0.0D);
            }
        }
    }

    private void beginDrinking() {
        this.drinking = true;
        this.ticks = 0;
        this.mob.getNavigation().stop();
        this.faceWater(Vec3.atCenterOf(this.spot.water));
        this.mob.tryStartAnimation(this.mob.getAnimationDrink());
        PerfCounters.inc(PerfCounters.Counter.DRINK_VISIT);
        this.mob.drinkVisits++;
        HerdEntity herd = this.mob.herd;
        if (herd != null) {
            herd.waterhole = this.spot.water;
            herd.waterholeMisses = 0;
            // the leader going down to the water brings the herd: each member is due within ~10 seconds
            if (herd.getLeader() == this.mob) {
                long now = this.mob.level.getGameTime();
                for (ComplexMob member : herd.creatureList) {
                    if (member != this.mob && member.isAlive() && member.nextDrinkTime > now + 200) {
                        member.nextDrinkTime = now + 20 + this.mob.getRandom().nextInt(180);
                    }
                }
            }
        }
    }

    private void faceWater(Vec3 water) {
        // body yaw only: the DRINK keyframes lower the head; head tracking on top (a look target down at the water)
        // would bend the neck twice
        float yaw = (float) (Mth.atan2(water.z - this.mob.getZ(), water.x - this.mob.getX()) * (180D / Math.PI)) - 90.0F;
        this.mob.setYRot(Mth.approachDegrees(this.mob.getYRot(), yaw, 10.0F));
        this.mob.yBodyRot = this.mob.getYRot();
    }

    @Override
    public void stop() {
        this.mob.stopAnimation(this.mob.getAnimationDrink());
        long now = this.mob.level.getGameTime();
        if (this.drinking) {
            this.mob.nextDrinkTime = now + nextInterval(this.mob.getRandom());
        } else {
            this.mob.nextDrinkTime = now + 1200 + this.mob.getRandom().nextInt(1200);   // never got there: try again soon
        }
        this.spot = null;
        this.standAt = null;
        this.drinking = false;
    }
}
