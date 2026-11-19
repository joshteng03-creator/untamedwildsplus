package untamedwilds.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.network.NetworkEvent;
import untamedwilds.util.EntityDataHolder;
import untamedwilds.util.EntityDataListenerEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Sends one entity type's complete species data (the parsed {@code data/untamedwilds/entities/<type>.json})
 * to a client, encoded with {@link EntityDataHolder#CODEC} as NBT (2.8.0).
 * <p>
 * A dedicated-server client has no datapack, so before this packet it only ever had skin paths and species
 * names. Every client-side read of species data -- the per-species model flags in ModelBison/Deer/DireWolf/
 * Glyptodont, SaddleLayer's species lookup, EntityPinniped's pup texture -- then threw a NullPointerException
 * in multiplayer. With the whole holder synced, those code paths see exactly what they see in single player.
 * One packet per type keeps every payload far below the 2 MB NBT read limit.
 */
public class SyncEntityData {

    private final ResourceLocation type;
    private final CompoundTag data;

    public SyncEntityData(ResourceLocation type, CompoundTag data) {
        this.type = type;
        this.data = data;
    }

    public SyncEntityData(FriendlyByteBuf buf) {
        this.type = buf.readResourceLocation();
        this.data = buf.readNbt();
    }

    @Nullable
    public static SyncEntityData of(EntityType<?> type, EntityDataHolder holder) {
        return EntityDataHolder.CODEC.encodeStart(NbtOps.INSTANCE, holder).result()
                .filter(tag -> tag instanceof CompoundTag)
                .map(tag -> new SyncEntityData(type.getRegistryName(), (CompoundTag) tag))
                .orElse(null);
    }

    /** The encoded species data (GameTests decode it exactly as a client would). */
    public CompoundTag data() {
        return this.data;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.type);
        buf.writeNbt(this.data);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> EntityDataListenerEvent.installSyncedData(this.type, this.data));
        ctx.get().setPacketHandled(true);
        return true;
    }
}
