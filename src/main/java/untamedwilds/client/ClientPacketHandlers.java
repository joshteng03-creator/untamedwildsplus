package untamedwilds.client;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import untamedwilds.network.AnimationStartPacket;

/**
 * Client-only halves of the mod's packets. Kept in its own class and reached through DistExecutor, so a
 * dedicated server never loads a class that references {@link Minecraft}.
 */
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {}

    public static void handleAnimationStart(AnimationStartPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(packet.entityId);
        if (!(entity instanceof IAnimatedEntity animated)) {
            return;
        }
        if (packet.index < 0) {
            animated.setAnimation(IAnimatedEntity.NO_ANIMATION);
            animated.setAnimationTick(0);
            return;
        }
        Animation[] animations = animated.getAnimations();
        // Citadel's handler indexes this array unchecked; a server/client mismatch would throw on the render thread.
        if (animations == null || packet.index >= animations.length) {
            return;
        }
        animated.setAnimation(animations[packet.index]);
        animated.setAnimationTick(Math.min(packet.tick, Math.max(0, animations[packet.index].getDuration() - 1)));
    }
}
