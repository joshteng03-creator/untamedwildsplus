package untamedwilds.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Antler chandelier (2026-10-06): four antlers around four candles on a dark-oak plate, hung from a
 * chain. Placed only under a block that can hold it (like a hanging lantern) and drops when that block
 * goes. Light 15; the candle flames are particles, as on vanilla candles. No collision, so it never
 * catches a player's head in a low room.
 */
public class AntlerChandelierBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(1.0D, 8.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    // Wick tops of the four candles, in block pixels (see scripts/gen_antler_decor.py).
    private static final double[][] FLAMES = {{5.5, 5.5}, {10.5, 5.5}, {5.5, 10.5}, {10.5, 10.5}};

    public AntlerChandelierBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return direction == Direction.UP && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, Random random) {
        for (double[] flame : FLAMES) {
            if (random.nextFloat() < 0.6F) {
                double x = pos.getX() + flame[0] / 16.0D;
                double y = pos.getY() + 16.5D / 16.0D;
                double z = pos.getZ() + flame[1] / 16.0D;
                level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
                if (random.nextFloat() < 0.15F) {
                    level.addParticle(ParticleTypes.SMOKE, x, y + 0.05D, z, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }
}
