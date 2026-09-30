package untamedwilds.entity.ai.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ComplexMob;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class HuntWeakerTarget<T extends LivingEntity> extends HuntMobTarget<T> {

    private final int executionChance;

    public HuntWeakerTarget(ComplexMob creature, Class<T> classTarget, boolean checkSight) {
        this(creature, classTarget, 300, checkSight, null);
    }

    public HuntWeakerTarget(ComplexMob creature, Class<T> classTarget, int chance, boolean checkSight, final Predicate<LivingEntity> targetSelector) {
        super(creature, classTarget, checkSight, 30, false, targetSelector);
        this.executionChance = chance;
    }

    protected boolean isValidTarget(LivingEntity entity, @Nullable Predicate<LivingEntity> predicate) {
        if (entity instanceof Creeper || entity.equals(this.mob) || (!ConfigGamerules.attackUndead.get() && entity.getMobType() == MobType.UNDEAD) || entity.isVehicle() || (predicate != null && !predicate.test(entity)) || entity.getHealth() / entity.getMaxHealth() > 0.8) {
            return false;
        }
        // Same fix as ProtectChildrenTarget: the eco-level prefix made this guard skip exactly the case
        // it exists for -- two animals of the same species, which score identically.
        if (this.mob.getClass() == entity.getClass() && this.mob instanceof ComplexMob attacker && entity instanceof ComplexMob defender) {
            if (attacker.getVariant() == defender.getVariant()) {
                return false;
            }
        }
        return canAttack(entity, TargetingConditions.forCombat().range(getFollowDistance()));
    }

    /* This override replaces HuntMobTarget.canUse() wholesale, so it also dropped the hunger and
     * cooldown gates along with it -- which is why the shark, its only user, hunted without pause.
     * Both are re-applied here. */
    @Override
    public boolean canUse() {
        if (this.mob.isBaby() || this.mob.getRandom().nextInt(this.executionChance) != 0) {
            return false;
        }
        if (this.mob instanceof ComplexMob hunter) {
            if (hunter.huntingCooldown != 0 || hunter.isTame() || hunter.getHunger() > this.threshold) {
                return false;
            }
        }
        List<T> list = this.mob.level.getEntitiesOfClass(this.targetClass, this.mob.getBoundingBox().inflate(this.getFollowDistance(), 12.0D, this.getFollowDistance()), this.targetEntitySelector);
        if (list.isEmpty())
            return false;

        list.sort(this.sorter);
        this.targetMob = list.get(0);
        return true;
    }
}