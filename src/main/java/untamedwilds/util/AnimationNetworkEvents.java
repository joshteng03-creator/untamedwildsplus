package untamedwilds.util;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationEvent;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.ArrayUtils;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigPerformance;
import untamedwilds.entity.ComplexMob;
import untamedwilds.network.AnimationStartPacket;
import untamedwilds.network.UntamedInstance;

/**
 * Sends Untamed Wilds animation starts only to the players tracking the animal (2.8.0).
 * <p>
 * Citadel posts a cancellable {@link AnimationEvent.Start} on the Forge bus at tick 0 of every animation and,
 * unless it is cancelled, broadcasts a start message to every player on the server. With eat, drink and alert
 * animations on every herbivore that is a lot of traffic to players who cannot even see the animal. This
 * listener runs LAST (so any other mod's listener sees the event first), touches only our own mobs on the
 * server, and replaces the broadcast with a tracking-only packet. The animation tick still advances: Citadel
 * only skips its send when the event is cancelled.
 * <p>
 * Guarded by {@code isCancelable()} in case a modpack ships a Citadel build where the event is not
 * cancellable -- then Citadel's own broadcast simply stays in place.
 */
@Mod.EventBusSubscriber(modid = UntamedWilds.MOD_ID)
public final class AnimationNetworkEvents {

    private AnimationNetworkEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAnimationStart(AnimationEvent.Start<?> event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.level.isClientSide || !(entity instanceof ComplexMob)
                || !event.isCancelable() || event.isCanceled() || !ConfigPerformance.trackingOnlyAnimationPackets.get()) {
            return;
        }
        IAnimatedEntity animated = (IAnimatedEntity) entity;
        Animation animation = event.getAnimation();
        // What Citadel's sendAnimationMessage would have done on the server before broadcasting.
        animated.setAnimation(animation);
        event.setCanceled(true);
        sendToTrackers(entity, animated, animation, 0);
    }

    /** Sends an animation (or a stop, when NO_ANIMATION) to every player tracking {@code entity}. */
    public static void sendToTrackers(Entity entity, IAnimatedEntity animated, Animation animation, int tick) {
        int index = animation == IAnimatedEntity.NO_ANIMATION ? -1 : ArrayUtils.indexOf(animated.getAnimations(), animation);
        if (index < 0 && animation != IAnimatedEntity.NO_ANIMATION) {
            return;   // not declared in getAnimations(): Citadel could not have synced it either
        }
        PerfCounters.inc(PerfCounters.Counter.ANIM_PACKET);
        if (entity instanceof ComplexMob mob) {
            mob.perfAnimStarts++;
        }
        UntamedInstance.sendToTracking(new AnimationStartPacket(entity.getId(), index, tick), entity);
    }
}
