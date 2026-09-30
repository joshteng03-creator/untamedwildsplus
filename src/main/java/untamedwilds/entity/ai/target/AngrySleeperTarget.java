package untamedwilds.entity.ai.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.config.EcologyMode;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.entity.ISpecies;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class AngrySleeperTarget<T extends LivingEntity> extends TargetGoal {
    protected final Class<T> targetClass;
    protected final int targetChance;
    protected LivingEntity target;
    protected ComplexMobTerrestrial taskOwner;
    protected Predicate<T> targetEntitySelector;
    private int runningTicks;

    public AngrySleeperTarget(ComplexMobTerrestrial entityIn, Class<T> targetClassIn, boolean checkSight) {
        this(entityIn, targetClassIn, checkSight, false);
    }

    public AngrySleeperTarget(ComplexMobTerrestrial entityIn, Class<T> targetClassIn, boolean checkSight, boolean nearbyOnlyIn) {
        this(entityIn, targetClassIn, 4, checkSight, nearbyOnlyIn);
    }

    public AngrySleeperTarget(ComplexMobTerrestrial entityIn, Class<T> targetClassIn, int targetChanceIn, boolean checkSight, boolean nearbyOnlyIn) {
        super(entityIn, checkSight, nearbyOnlyIn);
        this.targetClass = targetClassIn;
        this.targetChance = targetChanceIn;
        this.setFlags(EnumSet.of(Flag.TARGET));
        this.runningTicks = 1000;
        this.taskOwner = entityIn;
        this.targetEntitySelector = entity -> {
            if (entity instanceof Creeper || ComplexMob.getEcoLevel(this.taskOwner) > ComplexMob.getEcoLevel(entity) * 2) {
                return false;
            }
            /* A sleeping predator does not wake up and pick a fight with another predator just for walking
             * past -- only with one that is coming for it. This was a main route by which wolf packs
             * (eco 13-16) killed passing big cats and bears. */
            if (this.taskOwner.isCarnivore() && entity instanceof ComplexMob other && other.isCarnivore() && other.getTarget() != this.taskOwner) {
                return false;
            }
            if (this.taskOwner.getClass() == entity.getClass()) {
                if (this.taskOwner instanceof ISpecies && entity instanceof ISpecies) {
                    ComplexMob attacker = this.taskOwner;
                    ComplexMob defender = ((ComplexMob)entity);
                    if (attacker.getVariant() == defender.getVariant()) {
                        return false;
                    }
                }
            }
            if (entity instanceof Player player) {
                if (player.isSteppingCarefully() || player.isCreative() || player.isSpectator())
                    return false;
            }
            return TargetingConditions.forCombat().test(this.taskOwner, entity) && this.canAttack(entity, TargetingConditions.DEFAULT);
        };
    }

    public boolean canUse() {
        // Zoo mode forces angry_sleepers off: an exhibit animal woken up does not attack the visitor.
        if (!ConfigGamerules.angrySleepers.get() || !EcologyMode.allowsBrawls(this.taskOwner)
                || this.taskOwner.isBaby() || !this.taskOwner.isSleeping() || this.taskOwner.isTame() || this.taskOwner.forceSleep != 0) {
            return false;
        }
        /* Used to return true for as long as the animal slept, whether or not anything was nearby, and to
         * keep whatever it had found on some earlier night -- so start() could aim a freshly woken animal
         * at a stale entity, and a sleeper held the target slot all night regardless. */
        this.target = null;
        List<LivingEntity> list = this.mob.level.getEntitiesOfClass(LivingEntity.class, this.mob.getBoundingBox().inflate(6.0D, 4.0D, 6.0D), (input) -> this.targetEntitySelector.test((T) input));
        if (list.isEmpty()) {
            return false;
        }
        this.taskOwner.setSleeping(false);
        this.target = list.get(0);
        return true;
    }

    public void start() {
        // Per fight: this was only ever set in the constructor, so after an animal had spent 1000 ticks
        // in angry-sleeper fights over its whole life, every later one ended the tick it began.
        this.runningTicks = 1000;
        this.taskOwner.setTarget(this.target);
        this.taskOwner.forceSleep = -300;
        super.start();
    }

    public boolean canContinueToUse() {
        // Break off once either side is nearly dead -- see ComplexMob.tryBreakOff().
        if (this.mob instanceof ComplexMob fighter && ComplexMob.tryBreakOff(fighter, fighter.getTarget())) {
            return false;
        }
        this.runningTicks--;
        if (this.runningTicks < 1)
            return false;
        return super.canContinueToUse();
    }
}