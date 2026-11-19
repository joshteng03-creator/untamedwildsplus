package untamedwilds.block.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import untamedwilds.init.ModBlock;

/**
 * What a Species Placard shows (2026-10-08): the studied animal's entity type, variant and species key,
 * copied from a Field Guide. Synced to clients for {@code SpeciesPlacardRenderer}.
 */
public class SpeciesPlacardBlockEntity extends BlockEntity {

    private String type = "";
    private String species = "";

    public SpeciesPlacardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlock.TILE_ENTITY_PLACARD.get(), pos, state);
    }

    public String getEntityType() {
        return this.type;
    }

    public String getSpecies() {
        return this.species;
    }

    public void setSpecies(String type, String species) {
        this.type = type;
        this.species = species;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("EntityType", this.type);
        tag.putString("Species", this.species);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.type = tag.getString("EntityType");
        this.species = tag.getString("Species");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
