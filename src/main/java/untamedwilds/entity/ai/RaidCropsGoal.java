package untamedwilds.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import untamedwilds.entity.ComplexMobTerrestrial;

import java.util.EnumSet;
import java.util.List;

public class RaidCropsGoal extends Goal {

    private BlockPos targetPos;
    private final ComplexMobTerrestrial taskOwner;
    private boolean continueTask;
    private int ticksRunning;
    // Give up on a crop the mob cannot reach instead of pathing at it forever.
    private static final int GIVE_UP_TICKS = 300;

    public RaidCropsGoal(ComplexMobTerrestrial entityIn) {
        this.taskOwner = entityIn;
        this.continueTask = true;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.taskOwner.isTame() || !net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.taskOwner.level, this.taskOwner)) {
            return false;
        }
        if (this.taskOwner.getHunger() > 80 || this.taskOwner.getTarget() != null) {
            return false;
        }
        if (this.taskOwner.getRandom().nextInt(120) != 0) {
            return false;
        }
        BlockPos pos = this.taskOwner.blockPosition();

        this.targetPos = getNearbyFarmland(pos);
        return this.targetPos != null;
    }

    @Override
    public void start() {
        // Reset per run: continueTask used to stay false after the first raid, so each mob could only
        // ever raid once in its lifetime.
        this.continueTask = true;
        this.ticksRunning = 0;
        this.taskOwner.getNavigation().moveTo((double)this.targetPos.getX() + 0.5D, this.targetPos.getY() + 1, (double)this.targetPos.getZ() + 0.5D, 1f);
    }

    @Override
    public void tick() {
        if (++this.ticksRunning > GIVE_UP_TICKS) {
            this.continueTask = false;
            return;
        }
        if (this.taskOwner.distanceToSqr(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D) < 4) {
            BlockState block = this.taskOwner.level.getBlockState(this.targetPos);
            if (block.getBlock() instanceof CropBlock) {
                // Block.getDrops(state, level, pos, blockEntity) supplies the ORIGIN and TOOL loot parameters.
                // The old hand-built LootContext left them out, so the block loot table threw
                // "Missing required parameters" the moment a mob reached a crop.
                List<ItemStack> drops = Block.getDrops(block, (ServerLevel) this.taskOwner.level, this.targetPos, null);
                if (!drops.isEmpty()) {
                    this.taskOwner.addHunger(Math.max(drops.size() * 10, 10));
                    this.taskOwner.level.destroyBlock(this.targetPos, false);
                    this.taskOwner.getNavigation().stop();
                }
            }
            this.continueTask = false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.taskOwner.getHunger() > 80 || this.taskOwner.level.isEmptyBlock(this.targetPos)) {
            return false;
        }
        return this.continueTask;
    }


    private BlockPos getNearbyFarmland(BlockPos roomCenter) {
        int X = 15;
        int Y = 3;
        //List<BlockPos> inventories = new ArrayList<>();
        for (BlockPos blockpos : BlockPos.betweenClosed(roomCenter.offset(-X, -Y, -X), roomCenter.offset(X, Y, X))) {
            if (this.taskOwner.level.getBlockState(blockpos).getBlock() instanceof FarmBlock) {
                if (this.hasPlantedCrop(this.taskOwner.level, blockpos)) {
                    return blockpos.above();
                }
            }
        }
        return null;
    }

    private boolean hasPlantedCrop(LevelReader worldIn, BlockPos pos) {
        BlockState block = worldIn.getBlockState(pos.above());
        return block.getBlock() instanceof CropBlock;
    }
}
