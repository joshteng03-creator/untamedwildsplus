package untamedwilds.block.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import untamedwilds.block.FeedingTroughBlock;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.init.ModBlock;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Feeding Trough contents and feeding logic (2026-10-08). Holds one stack of food. Every
 * {@link #INTERVAL} ticks it looks for the nearest hungry land animal within {@link #RANGE} blocks that
 * eats what is in it: one standing at the trough is fed a serving, otherwise it is called over. No AI goal
 * is added to any species -- the trough drives the animal's own navigation, so every type works with it.
 * <p>
 * What an animal accepts: its favourite food always; meat for carnivores; plant food for everything else
 * (anything edible that is not meat, plus wheat, hay bales, sugar cane, seagrass and kelp). A serving is
 * worth the item's own nutrition x10 hunger, the same as feeding by hand; a hay bale is nine servings.
 */
public class FeedingTroughBlockEntity extends BlockEntity {

    public static final int INTERVAL = 40;
    public static final double RANGE = 10.0D;
    private static final int HUNGRY_BELOW = 60;
    private static final int HAY_SERVINGS = 9;

    private ItemStack food = ItemStack.EMPTY;
    private int servingsLeft;

    public FeedingTroughBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlock.TILE_ENTITY_TROUGH.get(), pos, state);
    }

    public ItemStack getFood() {
        return this.food;
    }

    /** Puts as much of {@code stack} in as fits; returns what is left over. */
    public ItemStack insert(ItemStack stack) {
        if (!isTroughFood(stack) || !(this.food.isEmpty() || ItemStack.isSameItemSameTags(this.food, stack))) {
            return stack;
        }
        int room = stack.getMaxStackSize() - this.food.getCount();
        int moved = Math.min(room, stack.getCount());
        if (moved <= 0) {
            return stack;
        }
        if (this.food.isEmpty()) {
            this.food = stack.copy();
            this.food.setCount(moved);
        } else {
            this.food.grow(moved);
        }
        ItemStack rest = stack.copy();
        rest.shrink(moved);
        this.contentsChanged();
        return rest;
    }

    public ItemStack takeAll() {
        ItemStack out = this.food;
        this.food = ItemStack.EMPTY;
        this.servingsLeft = 0;
        this.contentsChanged();
        return out;
    }

    private void contentsChanged() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            FeedingTroughBlock.Fill fill = this.food.isEmpty() ? FeedingTroughBlock.Fill.EMPTY : isMeat(this.food) ? FeedingTroughBlock.Fill.MEAT : FeedingTroughBlock.Fill.PLANT;
            if (state.getValue(FeedingTroughBlock.FILL) != fill) {
                this.level.setBlock(this.worldPosition, state.setValue(FeedingTroughBlock.FILL, fill), 3);
            }
        }
    }

    public static boolean isTroughFood(ItemStack stack) {
        return !stack.isEmpty() && (stack.isEdible() || plantValue(stack) > 0);
    }

    private static boolean isMeat(ItemStack stack) {
        FoodProperties f = stack.getItem().getFoodProperties();
        return f != null && f.isMeat();
    }

    private static int plantValue(ItemStack stack) {
        if (stack.is(Items.WHEAT) || stack.is(Items.HAY_BLOCK)) return 20;
        if (stack.is(Items.SUGAR_CANE) || stack.is(Items.SEAGRASS) || stack.is(Items.KELP)) return 15;
        return 0;
    }

    /** Hunger a serving of {@code stack} gives {@code mob}; 0 if it will not eat it. */
    public static int servingValue(ComplexMob mob, ItemStack stack) {
        FoodProperties f = stack.getItem().getFoodProperties();
        int edible = f != null ? f.getNutrition() * 10 : 0;
        if (mob.isFood(stack)) {
            return Math.max(edible, Math.max(plantValue(stack), 30));
        }
        if (mob.isCarnivore()) {
            return f != null && f.isMeat() ? edible : 0;
        }
        return f != null ? (f.isMeat() ? 0 : edible) : plantValue(stack);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FeedingTroughBlockEntity trough) {
        if (level.getGameTime() % INTERVAL != 0 || trough.food.isEmpty()) {
            return;
        }
        double cx = pos.getX() + 0.5D, cy = pos.getY() + 0.5D, cz = pos.getZ() + 0.5D;
        List<ComplexMobTerrestrial> hungry = level.getEntitiesOfClass(ComplexMobTerrestrial.class, new AABB(pos).inflate(RANGE),
                m -> m.isAlive() && !m.isVehicle() && m.getHunger() < HUNGRY_BELOW && servingValue(m, trough.food) > 0);
        Optional<ComplexMobTerrestrial> nearest = hungry.stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(cx, cy, cz)));
        if (nearest.isEmpty()) {
            return;
        }
        ComplexMobTerrestrial mob = nearest.get();
        double reach = 1.6D + mob.getBbWidth() / 2.0D;
        if (mob.distanceToSqr(cx, mob.getY(), cz) <= reach * reach) {
            mob.getLookControl().setLookAt(cx, cy, cz);
            mob.addHunger(servingValue(mob, trough.food));
            mob.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
            if (level instanceof ServerLevel server) {
                server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, trough.food), cx, cy + 0.3D, cz, 6, 0.2D, 0.1D, 0.2D, 0.05D);
            }
            if (trough.food.is(Items.HAY_BLOCK)) {
                if (trough.servingsLeft <= 0) {
                    trough.servingsLeft = HAY_SERVINGS;
                }
                if (--trough.servingsLeft > 0) {
                    trough.setChanged();
                    return;
                }
            }
            trough.food.shrink(1);
            if (trough.food.isEmpty()) {
                trough.food = ItemStack.EMPTY;
            }
            trough.contentsChanged();
        } else if (mob.canMove() && mob.getNavigation().isDone()) {
            mob.getNavigation().moveTo(cx, pos.getY(), cz, 1.0D);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Food", this.food.save(new CompoundTag()));
        tag.putInt("Servings", this.servingsLeft);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.food = ItemStack.of(tag.getCompound("Food"));
        this.servingsLeft = tag.getInt("Servings");
    }
}
