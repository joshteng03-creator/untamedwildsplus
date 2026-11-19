package untamedwilds.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import untamedwilds.block.blockentity.FeedingTroughBlockEntity;
import untamedwilds.init.ModBlock;

import javax.annotation.Nullable;

/**
 * Feeding Trough (2026-10-08): right-click with food to fill it (one stack), with an empty hand to empty it.
 * Hungry animals nearby come and eat ({@link FeedingTroughBlockEntity}). {@link #FILL} shows what is in it.
 */
public class FeedingTroughBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public enum Fill implements StringRepresentable {
        EMPTY, PLANT, MEAT;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }

    public static final EnumProperty<Fill> FILL = EnumProperty.create("fill", Fill.class);
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D);

    public FeedingTroughBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILL, Fill.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILL);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (hand != InteractionHand.MAIN_HAND || trough.getFood().isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                player.getInventory().placeItemBackInInventory(trough.takeAll());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!FeedingTroughBlockEntity.isTroughFood(held)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack rest = trough.insert(held);
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, rest);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), trough.getFood());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FeedingTroughBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != ModBlock.TILE_ENTITY_TROUGH.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<FeedingTroughBlockEntity>) FeedingTroughBlockEntity::serverTick;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
