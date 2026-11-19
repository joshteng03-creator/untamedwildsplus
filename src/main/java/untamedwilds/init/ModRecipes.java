package untamedwilds.init;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import untamedwilds.UntamedWilds;
import untamedwilds.item.SpearTrimRecipe;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, UntamedWilds.MOD_ID);

    public static final RegistryObject<SimpleRecipeSerializer<SpearTrimRecipe>> SPEAR_TRIM = SERIALIZERS.register("spear_trim", () -> new SimpleRecipeSerializer<>(SpearTrimRecipe::new));
}
