package untamedwilds.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import untamedwilds.UntamedWilds;
import untamedwilds.entity.ComplexMob;
import untamedwilds.init.ModItems;

/**
 * Hunting-gear hooks (2026-10-06).
 * <ul>
 * <li>A trimmed spear's melee bonus against the mod's animals. Thrown spears add it themselves
 * (ProjectileThrownSpear), so only a direct player hit is handled here.</li>
 * <li>Taxidermy: kill one of the mod's animals with a Taxidermy Kit in the off hand and the kit is
 * used up to drop a Mounted Specimen of that animal, beside its normal loot.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = UntamedWilds.MOD_ID)
public class HuntingEvents {

    private HuntingEvents() {}

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getDirectEntity() instanceof Player player && event.getSource().getEntity() == player) {
            float bonus = SpearItem.trimBonus(player.getMainHandItem(), event.getEntityLiving());
            if (bonus > 0.0F) {
                event.setAmount(event.getAmount() + bonus);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntityLiving().level.isClientSide || !(event.getEntityLiving() instanceof ComplexMob mob)
                || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        ItemStack kit = player.getOffhandItem();
        if (!kit.is(ModItems.TAXIDERMY_KIT.get())) {
            return;
        }
        mob.spawnAtLocation(MountedSpecimenItem.of(mob));
        if (!player.getAbilities().instabuild) {
            kit.shrink(1);
        }
        mob.level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
