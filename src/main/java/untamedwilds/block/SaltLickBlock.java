package untamedwilds.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import untamedwilds.block.blockentity.SaltLickBlockEntity;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.init.ModBlock;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Salt Lick (2026-10-08): enrichment for herbivores. Now and then a nearby plant-eater wanders over and
 * licks it (a little hunger, a sound and salt-dust particles). It wears down through four sizes
 * ({@link #WEAR}) over {@link #LICKS_PER_SIZE} licks each, then is gone.
 */
public class SaltLickBlock extends Block implements EntityBlock {

    public static final IntegerProperty WEAR = IntegerProperty.create("wear", 0, 3);
    public static final int LICKS_PER_SIZE = 8;
    private static final VoxelShape[] SHAPES = {
            Block.box(2, 0, 2, 14, 10, 14), Block.box(3, 0, 3, 13, 8, 13), Block.box(4, 0, 4, 12, 6, 12), Block.box(5, 0, 5, 11, 4, 11)};

    public SaltLickBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WEAR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WEAR);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(WEAR)];
    }

    /** Called by the block entity: one lick by {@code mob}. */
    public static void lick(Level level, BlockPos pos, BlockState state, ComplexMobTerrestrial mob, SaltLickBlockEntity lick) {
        mob.getLookControl().setLookAt(pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D);
        mob.addHunger(5);
        mob.playSound(SoundEvents.GENERIC_EAT, 0.6F, 1.4F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D, 5, 0.2D, 0.1D, 0.2D, 0.02D);
        }
        if (++lick.licks >= LICKS_PER_SIZE) {
            lick.licks = 0;
            int wear = state.getValue(WEAR);
            if (wear >= 3) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return;
            }
            level.setBlock(pos, state.setValue(WEAR, wear + 1), 3);
        }
        lick.setChanged();
    }

    public static List<ComplexMobTerrestrial> visitors(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(ComplexMobTerrestrial.class, new AABB(pos).inflate(8.0D),
                m -> m.isAlive() && !m.isCarnivore() && !m.isVehicle() && m.canMove() && !m.isBaby());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SaltLickBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != ModBlock.TILE_ENTITY_SALT_LICK.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<SaltLickBlockEntity>) SaltLickBlockEntity::serverTick;
    }
}
