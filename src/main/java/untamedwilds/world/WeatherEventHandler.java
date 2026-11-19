package untamedwilds.world;

import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ComplexMob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightning spooks herds (2026-10-06). When a bolt lands, every untamed, non-carnivore animal of the
 * mod within {@link #SPOOK_RADIUS} bolts away from the strike. A herd runs along ONE heading -- away
 * from the bolt through the herd's centre -- the same rule HerdFleeGoal uses for a predator, so a
 * strike scatters nothing into stragglers. Behaviour only, so it runs in Zoo mode too
 * ({@code weather_herd_behaviour}).
 * <p>
 * Not routed through HerdFleeGoal on purpose: that goal tracks a living threat to keep fleeing from,
 * and a bolt is gone the next tick. A single escape run is all a thunderclap is worth.
 */
@Mod.EventBusSubscriber(modid = UntamedWilds.MOD_ID)
public class WeatherEventHandler {

    private static final double SPOOK_RADIUS = 24.0D;
    private static final double SPOOK_SPEED = 1.5D;

    private WeatherEventHandler() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide() || !(event.getEntity() instanceof LightningBolt bolt) || !ConfigGamerules.weatherHerdBehaviour.get()) {
            return;
        }
        Vec3 strike = bolt.position();
        List<ComplexMob> nearby = event.getWorld().getEntitiesOfClass(ComplexMob.class, new AABB(strike, strike).inflate(SPOOK_RADIUS, 8.0D, SPOOK_RADIUS),
                mob -> mob.isAlive() && !mob.isTame() && !mob.isCarnivore() && mob.getTarget() == null);
        Map<Object, Vec3> headings = new HashMap<>();
        for (ComplexMob mob : nearby) {
            // One heading per herd, from the herd's centre; a lone animal uses its own position.
            Object key = mob.herd != null ? mob.herd : mob;
            Vec3 heading = headings.computeIfAbsent(key, k -> away(centre(mob), strike, mob));
            if (mob.isSleeping()) {
                mob.setSleeping(false);
            }
            Vec3 escape = DefaultRandomPos.getPosTowards(mob, 16, 7, mob.position().add(heading.scale(16.0D)), Math.PI / 2.0D);
            if (escape == null) {
                escape = DefaultRandomPos.getPosAway(mob, 16, 7, strike);
            }
            if (escape != null) {
                mob.getNavigation().moveTo(escape.x, escape.y, escape.z, SPOOK_SPEED * mob.getFleeSpeedFactor());
            }
        }
    }

    private static Vec3 centre(ComplexMob mob) {
        if (mob.herd == null) {
            return mob.position();
        }
        double x = 0, z = 0;
        int n = 0;
        for (ComplexMob member : mob.herd.creatureList) {
            if (member.isAlive()) {
                x += member.getX();
                z += member.getZ();
                n++;
            }
        }
        return n == 0 ? mob.position() : new Vec3(x / n, mob.getY(), z / n);
    }

    private static Vec3 away(Vec3 from, Vec3 strike, ComplexMob mob) {
        Vec3 v = new Vec3(from.x - strike.x, 0, from.z - strike.z);
        if (v.lengthSqr() < 1.0E-4D) {
            double a = mob.getRandom().nextDouble() * Math.PI * 2D;
            v = new Vec3(Math.cos(a), 0, Math.sin(a));
        }
        return v.normalize();
    }
}
