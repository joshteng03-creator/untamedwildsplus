package untamedwilds.entity.ai.unique;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ai.target.HuntMobTarget;

import java.util.function.Predicate;

public class HippoTerritoryTargetGoal<T extends LivingEntity> extends HuntMobTarget<T>  {

    public HippoTerritoryTargetGoal(ComplexMob creature, Class<T> classTarget, boolean checkSight, boolean onlyNearby, final Predicate<? super T > targetSelector) {
        super(creature, classTarget, checkSight,200, false, ((Predicate<LivingEntity>)null));
        this.targetEntitySelector = (Predicate<T>) entity -> {
            if (targetSelector != null && !targetSelector.test(entity)) {
                return false;
            }
            else {
                return TargetingConditions.forCombat().test(creature, entity) && this.canAttack(entity, TargetingConditions.DEFAULT);
            }
        };
    }

    /* Chasing an intruder out of the pool is not feeding, so it must not burn the hunting cooldown
     * and must not claim the fight-to-the-death exemption a real hunt gets. */
    @Override
    protected boolean isFoodHunt() {
        return false;
    }

    /* Still cooldown-gated, though: without it a hippo re-acquires every intruder on the next tick. */
    @Override
    protected boolean usesHuntCooldown() {
        return true;
    }

    public boolean canUse() {
        if (mob.isBaby() || !mob.isInWater()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Driving an intruder out of the water is a fight, not a hunt -- see ComplexMob.tryBreakOff().
        if (this.mob instanceof ComplexMob fighter && ComplexMob.tryBreakOff(fighter, fighter.getTarget())) {
            return false;
        }
        return super.canContinueToUse();
    }
}
