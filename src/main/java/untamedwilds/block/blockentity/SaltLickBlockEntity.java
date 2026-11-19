package untamedwilds.block.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import untamedwilds.block.SaltLickBlock;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.init.ModBlock;

import java.util.List;

/**
 * Salt Lick visits (2026-10-08). Every 5 s one herbivore within 8 blocks may take an interest (30%); while
 * one is interested it walks over, and licks once it is beside the block. Only one visitor at a time.
 */
public class SaltLickBlockEntity extends BlockEntity {

    private static final int INTERVAL = 100;
    private static final float INTEREST = 0.3F;

    public int licks;
    private int visitorId = -1;

    public SaltLickBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlock.TILE_ENTITY_SALT_LICK.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SaltLickBlockEntity lick) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        double cx = pos.getX() + 0.5D, cz = pos.getZ() + 0.5D;
        ComplexMobTerrestrial visitor = lick.visitorId >= 0 && level.getEntity(lick.visitorId) instanceof ComplexMobTerrestrial m && m.isAlive() && m.canMove() ? m : null;
        if (visitor == null) {
            lick.visitorId = -1;
            if (level.getGameTime() % INTERVAL != 0 || level.random.nextFloat() > INTEREST) {
                return;
            }
            List<ComplexMobTerrestrial> near = SaltLickBlock.visitors(level, pos);
            if (near.isEmpty()) {
                return;
            }
            visitor = near.get(level.random.nextInt(near.size()));
            lick.visitorId = visitor.getId();
        }
        double reach = 1.4D + visitor.getBbWidth() / 2.0D;
        if (visitor.distanceToSqr(cx, visitor.getY(), cz) <= reach * reach) {
            SaltLickBlock.lick(level, pos, state, visitor, lick);
            lick.visitorId = -1;
        } else if (visitor.getNavigation().isDone()) {
            visitor.getNavigation().moveTo(cx, pos.getY(), cz, 0.9D);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Licks", this.licks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.licks = tag.getInt("Licks");
    }
}
