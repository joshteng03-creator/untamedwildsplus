package untamedwilds.item;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import untamedwilds.entity.ComplexMob;

import java.util.List;

/**
 * Herding Horn (2026-10-06): blow it and your tamed animals within {@link #RANGE} blocks switch to
 * Follow; sneak while blowing to make them Sit. The same command states the empty-hand right-click
 * cycles through ({@code ComplexMob.setCommandInt}: 0 wander, 1 follow, 2 sit), just broadcast. A
 * 7-second cooldown, like a goat horn, and no durability.
 */
public class HerdingHornItem extends Item {

    private static final double RANGE = 32.0D;
    private static final int COOLDOWN = 140;
    private static final int FOLLOW = 1;
    private static final int SIT = 2;

    public HerdingHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean sit = player.isShiftKeyDown();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_DIDGERIDOO, SoundSource.PLAYERS, 2.0F, sit ? 0.6F : 0.8F);
        if (!level.isClientSide) {
            List<ComplexMob> pets = level.getEntitiesOfClass(ComplexMob.class, new AABB(player.blockPosition()).inflate(RANGE),
                    mob -> mob.isAlive() && mob.isTame() && mob.getOwner() == player);
            for (ComplexMob pet : pets) {
                pet.setCommandInt(sit ? SIT : FOLLOW);
                if (sit) {
                    pet.getNavigation().stop();
                    pet.setSitting(true);
                } else if (pet.isSitting()) {
                    pet.setSitting(false);
                }
            }
            player.displayClientMessage(new TranslatableComponent(sit ? "item.untamedwilds.herding_horn.sit" : "item.untamedwilds.herding_horn.follow", pets.size()), true);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
