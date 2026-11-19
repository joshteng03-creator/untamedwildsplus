package untamedwilds.entity.ai;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import untamedwilds.entity.ComplexMobTerrestrial;

import java.util.EnumSet;

/**
 * Vigilance (2.8.0): a herbivore that has spotted a predator just beyond flight range stops what it is doing,
 * turns to face it, raises its head (the type's ALERT animation) and watches -- the stare every real prey animal
 * gives a predator it is not yet running from. {@link SmartAvoidGoal} raises the alert; flight takes over the
 * moment the predator comes inside flight range (fleeCooldown), and the alert simply times out if it leaves.
 * <p>
 * Registered at the priority just BELOW the flight goal, never equal: two goals of the same priority cannot
 * interrupt each other, and an alert that held MOVE at flight priority would stop the animal ever fleeing.
 * GrazeGoal, DrinkGoal and SmartWanderGoal stand down while {@code isAlert()}.
 */
public class AlertGoal extends Goal {

    private final ComplexMobTerrestrial mob;

    public AlertGoal(ComplexMobTerrestrial mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity source = this.mob.alertSource;
        return this.mob.isAlert() && source != null && source.isAlive() && this.mob.fleeCooldown <= 0
                && this.mob.getTarget() == null && !this.mob.isSleeping() && this.mob.canMove() && !this.mob.isTame()
                && !this.mob.isVehicle();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
        this.mob.tryStartAnimation(this.mob.getAnimationAlert());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity source = this.mob.alertSource;
        if (source == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(source, 30.0F, 30.0F);
        // turn the whole body to face it, not just the head
        double dx = source.getX() - this.mob.getX();
        double dz = source.getZ() - this.mob.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        this.mob.setYRot(Mth.approachDegrees(this.mob.getYRot(), yaw, 8.0F));
        this.mob.yBodyRot = this.mob.getYRot();
        if (this.mob.getAnimation() == IAnimatedEntity.NO_ANIMATION) {
            this.mob.tryStartAnimation(this.mob.getAnimationAlert());   // repeat the stare for as long as it lasts
        }
    }

    @Override
    public void stop() {
        this.mob.stopAnimation(this.mob.getAnimationAlert());
    }
}
