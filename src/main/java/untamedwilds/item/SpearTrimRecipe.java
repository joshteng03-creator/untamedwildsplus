package untamedwilds.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import untamedwilds.init.ModItems;
import untamedwilds.init.ModRecipes;

import javax.annotation.Nullable;

/**
 * Spear + shark tooth (or antler), shapeless, gives the same spear -- enchantments, damage and name
 * kept -- with that trim ({@link SpearItem#TRIM_TAG}). A special recipe like vanilla's shield
 * decoration, since the result depends on the input stack.
 */
public class SpearTrimRecipe extends CustomRecipe {

    public SpearTrimRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        return this.find(grid) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer grid) {
        ItemStack[] found = this.find(grid);
        if (found == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = found[0].copy();
        result.setCount(1);
        SpearItem.setTrim(result, trimOf(found[1]));
        return result;
    }

    /** {spear, trim material}, or null unless the grid holds exactly those two items. */
    @Nullable
    private ItemStack[] find(CraftingContainer grid) {
        ItemStack spear = ItemStack.EMPTY;
        ItemStack trim = ItemStack.EMPTY;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack stack = grid.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof SpearItem && spear.isEmpty()) {
                spear = stack;
            } else if (trimOf(stack) != null && trim.isEmpty()) {
                trim = stack;
            } else {
                return null;
            }
        }
        return spear.isEmpty() || trim.isEmpty() ? null : new ItemStack[]{spear, trim};
    }

    @Nullable
    private static String trimOf(ItemStack stack) {
        if (stack.is(ModItems.MATERIAL_SHARK_TOOTH.get())) {
            return SpearItem.TRIM_TOOTH;
        }
        if (stack.is(ModItems.MATERIAL_ANTLER.get())) {
            return SpearItem.TRIM_ANTLER;
        }
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SPEAR_TRIM.get();
    }
}
