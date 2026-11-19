package untamedwilds.util;

import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigAmbience;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.entity.ai.FeedOnCarcassGoal;

/**
 * {@code ambience.extra_scavengers}: Untamed Wilds types a pack maker wants feeding on carcasses besides bears and
 * hyenas (which register the goal themselves). Added when the animal joins the world, at the same priority bears use.
 */
@Mod.EventBusSubscriber(modid = UntamedWilds.MOD_ID)
public final class CarcassEvents {

    private CarcassEvents() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide || !(event.getEntity() instanceof ComplexMobTerrestrial mob) || mob.canScavengeCarcass()) {
            return;
        }
        var extra = ConfigAmbience.extraScavengers.get();
        if (extra.isEmpty() || !extra.contains(EntityType.getKey(mob.getType()).toString())) {
            return;
        }
        boolean has = mob.goalSelectorForTests().getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof FeedOnCarcassGoal);
        if (!has) {
            mob.goalSelectorForTests().addGoal(4, new FeedOnCarcassGoal(mob));
        }
    }
}
