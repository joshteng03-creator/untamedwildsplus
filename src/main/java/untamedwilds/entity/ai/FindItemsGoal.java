package untamedwilds.entity.ai;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import untamedwilds.entity.ComplexMobTerrestrial;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class FindItemsGoal extends Goal {

    private final ComplexMobTerrestrial taskOwner;
    private final ClosestSorter sorter;
    private final int distance;
    private ItemEntity targetItem;
    private FoodProperties targetItemStack;
    private final int executionChance;
    private final boolean hyperCarnivore;
    private final boolean hyperHerbivore;

    public FindItemsGoal(ComplexMobTerrestrial entityIn, int distance) {
        this(entityIn, distance, 100, false, false);
    }

    /** Hunger at or below which a carnivore bothers with dropped food (2026-10-24; everything else: 80). */
    private static final int CARNIVORE_HUNGER_LIMIT = 60;
    /** Meat younger than this is a fresh kill's drop, already paid for by the carcass pool. */
    private static final int FRESH_KILL_DROP_TICKS = 600;

    public FindItemsGoal(ComplexMobTerrestrial entityIn, int distance, boolean hyperCarnivore) {
        this(entityIn, distance, 100, hyperCarnivore, false);
    }

    public FindItemsGoal(ComplexMobTerrestrial entityIn, int distance, int chance, boolean hyperCarnivore, boolean hyperHerbivore) {
        this.taskOwner = entityIn;
        this.sorter = new ClosestSorter(entityIn);
        this.distance = distance;
        this.executionChance = chance;
        this.hyperCarnivore = hyperCarnivore;
        this.hyperHerbivore = hyperHerbivore;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Flag.TARGET));
    }

    public boolean canUse() {
        /* Carnivores (2026-10-24): only when properly hungry, and never the meat their own kills just dropped --
         * that food is already in the carcass pool, and bears used to eat it twice over. */
        boolean carnivore = this.taskOwner.isCarnivore();
        if (this.taskOwner.getHunger() > (carnivore ? CARNIVORE_HUNGER_LIMIT : 80) || this.taskOwner.isBaby() || this.taskOwner.isSleeping()) {
            return false;
        }
        if (this.taskOwner.getRandom().nextInt(this.executionChance) != 0) {
            return false;
        }
        List<ItemEntity> list = this.taskOwner.level.getEntitiesOfClass(ItemEntity.class, this.getTargettableArea(distance));

        list.removeIf((ItemEntity item) -> !item.getItem().isEdible());
        if (carnivore) {
            list.removeIf((ItemEntity item) -> item.getAge() < FRESH_KILL_DROP_TICKS && isMeat(item.getItem().getItem()));
        }
        if (!list.isEmpty()) {
            if (this.hyperCarnivore) {
                list.removeIf((ItemEntity item) -> !isMeat(item.getItem().getItem()));
            } else if (this.hyperHerbivore) {
                list.removeIf((ItemEntity item) -> isMeat(item.getItem().getItem()));
            }
            if (!list.isEmpty()) {
                list.sort(this.sorter);
                this.targetItem = list.get(0);
                this.targetItemStack = this.targetItem.getItem().getItem().getFoodProperties();
                return true;
            }
        }
        return false;
    }

    private AABB getTargettableArea(double targetDistance) {
        return this.taskOwner.getBoundingBox().inflate(targetDistance, 4.0D, targetDistance);
    }

    private boolean isMeat(Item item) {
        if (item.getDefaultInstance().getItem().getFoodProperties() == null) {
            return false;
        }
        return item.getDefaultInstance().getItem().getFoodProperties().isMeat() || item.getDefaultInstance().is(ItemTags.FISHES);
    }

    @Override
    public void start() {
        this.taskOwner.getNavigation().moveTo(this.targetItem.getX(), this.targetItem.getY(), this.targetItem.getZ(), 1D);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetItem == null || !this.targetItem.isAlive()) {
            return false;
        } else if (!this.taskOwner.canMove()) {
            return false;
        }
        return !this.taskOwner.isPathFinding();
    }

    @Override
    public void tick() {
        double distance = Math.sqrt(Math.pow(this.taskOwner.getX() - this.targetItem.getX(), 2.0D) + Math.pow(this.taskOwner.getZ() - this.targetItem.getZ(), 2.0D));
        if (distance < 1.5D) {
            int perNutrition = this.taskOwner.isCarnivore() ? 5 : 10;   // scraps, not a meal, for a predator
            this.taskOwner.addHunger(Math.max(targetItemStack.getNutrition() * perNutrition, 10));
            this.targetItem.getItem().shrink(1);
            this.taskOwner.setAnimation(taskOwner.getAnimationEat());
        }
        if(this.taskOwner.getNavigation().isDone()){
            stop();
        }
        this.taskOwner.getNavigation().moveTo(this.targetItem.getX(), this.targetItem.getY(), this.targetItem.getZ(), 1D);
    }

    public static class ClosestSorter implements Comparator<Entity> {
        private final Entity entity;

        private ClosestSorter(Entity entityIn)
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
