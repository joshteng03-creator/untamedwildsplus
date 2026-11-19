package untamedwilds.entity.ai.unique;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import untamedwilds.entity.ComplexMob;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class TortoiseHideInShellGoal<T extends LivingEntity> extends Goal {

    protected final Class<T> classToAvoid;
    protected T avoidTarget;
    protected ComplexMob taskOwner;
    protected final float avoidDistance;
    private final TargetingConditions builtTargetSelector;

    public TortoiseHideInShellGoal(ComplexMob entityIn, Class<T> classToAvoidIn, float avoidDistanceIn, final Predicate<LivingEntity> targetSelector) {
        this.taskOwner = entityIn;
        this.classToAvoid = classToAvoidIn;
        this.avoidDistance = avoidDistanceIn;
        this.builtTargetSelector = TargetingConditions.forCombat().range(avoidDistanceIn).selector(targetSelector);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    /** 2.8.0: parity-proof replacement for a tickCount % 40 gate (see GoalTimer). */
    private final untamedwilds.util.GoalTimer checkTimer = new untamedwilds.util.GoalTimer(40, true);

    @Override
    public boolean canUse() {
        if (!this.checkTimer.ready(this.taskOwner)) {
            return false;
        }
        if (this.taskOwner.getTarget() != null || this.taskOwner.getCommandInt() != 0 || this.taskOwner.isTame()) {
            return false;
        }

        List<T> list = this.taskOwner.level.getNearbyEntities(classToAvoid, this.builtTargetSelector, this.taskOwner, this.taskOwner.getBoundingBox().inflate(avoidDistance, 4f, avoidDistance));
        if (list.isEmpty()) {
            this.taskOwner.setSitting(false);
            return false;
        }
        this.avoidTarget = list.get(0);
        return true;
    }

    public void start() {
        super.start();
        this.taskOwner.getNavigation().stop();
        this.taskOwner.setSitting(true);
        // 2.8.0: pulling into the shell forces the air out -- the hiss a real tortoise makes (its "threat" sound)
        net.minecraft.sounds.SoundEvent hiss = untamedwilds.util.EntityUtils.getSound(this.taskOwner.getType(), this.taskOwner.getVariant(), "threat");
        if (hiss != null) {
            this.taskOwner.playSound(hiss, 0.8F, this.taskOwner.voicePitch());
        }
    }

    public void stop() {
        super.stop();
    }

    public void tick() {
        if (this.taskOwner.getRandom().nextInt(40) == 0) {
            List<T> list = this.taskOwner.level.getNearbyEntities(classToAvoid, this.builtTargetSelector, this.taskOwner, this.taskOwner.getBoundingBox().inflate(avoidDistance, 4f, avoidDistance));
            if (list.isEmpty()) {
                this.taskOwner.setSitting(false);
            }
        }
        if (this.taskOwner.distanceTo(this.avoidTarget) > 10) {
            this.taskOwner.setSitting(false);
        }
        super.tick();
    }

    public boolean canContinueToUse() {
        return !this.taskOwner.isSitting();
    }
}