package untamedwilds.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Starts (or stops, index -1) a Citadel animation on the client -- 2.8.0.
 * <p>
 * Citadel's own start message goes to EVERY player on the server ({@code Citadel.sendMSGToAll}), whether or
 * not they can see the animal. {@link untamedwilds.util.AnimationNetworkEvents} cancels that for Untamed
 * Wilds mobs and sends this packet to the players tracking the entity instead. It also carries the current
 * animation tick, so a player who starts tracking an animal mid-animation joins it at the right frame
 * (Citadel's message always restarts at tick 0). The client handler does what Citadel's does, plus a bounds
 * check on the index.
 */
public class AnimationStartPacket {

    public final int entityId;
    public final int index;
    public final int tick;

    public AnimationStartPacket(int entityId, int index, int tick) {
        this.entityId = entityId;
        this.index = index;
        this.tick = tick;
    }

    public AnimationStartPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.index = buf.readVarInt() - 1;   // -1 (stop) travels as 0
        this.tick = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.index + 1);
        buf.writeVarInt(Math.max(0, this.tick));
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> untamedwilds.client.ClientPacketHandlers.handleAnimationStart(this)));
        ctx.get().setPacketHandled(true);
        return true;
    }
}
