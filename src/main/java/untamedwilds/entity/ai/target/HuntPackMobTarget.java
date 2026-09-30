package untamedwilds.entity.ai.target;

import net.minecraft.world.entity.LivingEntity;
import untamedwilds.entity.ComplexMob;

import java.util.function.Predicate;

public class HuntPackMobTarget<T extends LivingEntity> extends HuntMobTarget<T> {

    /* targetSelector was being dropped on the floor here -- super was called with a hardcoded null, so
     * the caller's predicate (for dire wolves and hyenas, "only things weaker than me") never applied
     * and pack hunters would pick a target regardless of how badly outmatched they were. Passed through
     * now, which also lets the ecology diet filter in HuntMobTarget.isValidTarget see it. */
    public HuntPackMobTarget(ComplexMob creature, Class<T> classTarget, boolean checkSight, int hungerThreshold, boolean onlyNearby, final Predicate<LivingEntity> targetSelector) {
        super(creature, classTarget, checkSight, hungerThreshold, false, targetSelector);
    }

    public void start() {
        this.mob.setTarget(this.targetMob);
        if (this.mob instanceof ComplexMob mob && mob.herd != null) {
            for (ComplexMob creature : mob.herd.creatureList) {
                creature.setTarget(this.targetMob);
                /* Pack-mates were handed the target but not the hunt. Their own copy of this goal is
                 * independently gated on hunger and cooldown, so a wolf that was fed, or still on
                 * cooldown, fought with no commitment at all and broke off the moment the prey dropped
                 * below 10% -- one member pressing the kill while seven others walked away from it. */
                creature.commitToHunt(this.targetMob);
            }
        }
        super.start();
    }
}
