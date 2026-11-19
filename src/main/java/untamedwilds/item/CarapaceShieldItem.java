package untamedwilds.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.client.IItemRenderProperties;
import untamedwilds.client.render.CarapaceShieldRenderer;
import untamedwilds.init.ModItems;

import java.util.function.Consumer;

/**
 * Carapace shield (2026-10-06): a shield faced with glyptodont scutes. Blocks exactly like a shield,
 * with 1.5x its durability (504) and repaired with scutes; takes no banner patterns (vanilla's
 * decoration recipe only accepts the plain shield). Drawn by {@link CarapaceShieldRenderer}, since
 * the vanilla shield model is hard-wired to the vanilla item.
 */
public class CarapaceShieldItem extends ShieldItem {

    public CarapaceShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.MATERIAL_SCUTE.get());
    }

    @Override
    public void initializeClient(Consumer<IItemRenderProperties> consumer) {
        consumer.accept(new IItemRenderProperties() {
            @Override
            public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
                return CarapaceShieldRenderer.get();
            }
        });
    }
}
