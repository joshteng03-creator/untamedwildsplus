package untamedwilds.init;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import untamedwilds.UntamedWilds;
import untamedwilds.item.*;
import untamedwilds.item.debug.*;
import untamedwilds.util.ModCreativeModeTab;

import java.util.function.Supplier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import untamedwilds.item.ModArmorMaterials;
import untamedwilds.item.SerratedArrowItem;
import untamedwilds.item.HerdingHornItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.ChatFormatting;
import untamedwilds.item.SpearItem;
import untamedwilds.item.CarapaceShieldItem;
import untamedwilds.item.MountedSpecimenItem;
import java.util.List;
import untamedwilds.item.FieldGuideItem;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = UntamedWilds.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, UntamedWilds.MOD_ID);
    // Wild Level Item instances

    // Debug Tools
    public static RegistryObject<Item> LOGO = createItem("logo", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static RegistryObject<Item> OWNERSHIP_DEED = createItem("ownership_deed", () -> new OwnershipDeedItem(new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> DEBUG_ERASER = createItem("debug_eraser", () -> new EraserItem(new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.untamedwilds_items).rarity(Rarity.EPIC)));
    public static RegistryObject<Item> DEBUG_ANALYZER = createItem("debug_analyzer", () -> new AnalyzerItem(new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.untamedwilds_items).rarity(Rarity.EPIC)));
    public static RegistryObject<Item> DEBUG_IPECAC = createItem("debug_ipecac", () -> new IpecacItem(new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.untamedwilds_items).rarity(Rarity.EPIC)));
    public static RegistryObject<Item> DEBUG_LOVE_POTION = createItem("debug_love_potion", () -> new LovePotionItem(new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.untamedwilds_items).rarity(Rarity.EPIC)));
    public static RegistryObject<Item> DEBUG_GROWTH_TONIC = createItem("debug_growth_tonic", () -> new GrowthTonicItem(new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.untamedwilds_items).rarity(Rarity.EPIC)));
    public static RegistryObject<Item> DEBUG_HIGHLIGHTER = createItem("debug_highlighter", () -> new HighlighterItem(new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.untamedwilds_items).rarity(Rarity.EPIC)));

    // Materials
    public static RegistryObject<Item> MATERIAL_FAT = createItem("material_fat", () -> new LardItem(new Item.Properties().food((new FoodProperties.Builder()).nutrition(1).saturationMod(1F).meat().build()).tab(CreativeModeTab.TAB_FOOD)));
    public static RegistryObject<Item> MATERIAL_BLUBBER = createItem("material_blubber", () -> new LardItem(new Item.Properties().food((new FoodProperties.Builder()).nutrition(1).saturationMod(1F).meat().build()).tab(CreativeModeTab.TAB_FOOD)){
        public int getBurnTime(ItemStack itemStack, RecipeType<?> recipeType) {
            return 1200;
        }
    });
    public static RegistryObject<Item> MATERIAL_PEARL = createItem("material_pearl", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> RARE_GIANT_PEARL = createItem("material_giant_pearl", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> MATERIAL_SNAKE_SKIN = createItem("material_snake_skin", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> CHUM = createItem("chum", () -> new ChumItem(new Item.Properties().food((new FoodProperties.Builder()).nutrition(1).saturationMod(0.1F).effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 1200, 0), 1F).meat().build()).tab(CreativeModeTab.TAB_FOOD)));

    // Food
    public static RegistryObject<Item> MEAT_BEAR_RAW = createItem("food_bear_raw", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.6F).meat().build()).tab(CreativeModeTab.TAB_FOOD)));
    public static RegistryObject<Item> MEAT_BEAR_COOKED = createItem("food_bear_cooked", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(7).saturationMod(1F).meat().build()).tab(CreativeModeTab.TAB_FOOD)));
    public static RegistryObject<Item> MEAT_TURTLE_RAW = createItem("food_turtle_raw", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(2).saturationMod(0.3F).meat().build()).tab(CreativeModeTab.TAB_FOOD)));
    public static RegistryObject<Item> MEAT_TURTLE_COOKED = createItem("food_turtle_cooked", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(6).saturationMod(0.6F).meat().build()).tab(CreativeModeTab.TAB_FOOD)));
    public static RegistryObject<Item> MEAT_HIPPO_RAW = createItem("food_pachyderm_raw", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.7F).meat().build()).tab(CreativeModeTab.TAB_FOOD)));
    public static RegistryObject<Item> MEAT_HIPPO_COOKED = createItem("food_pachyderm_cooked", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(7).saturationMod(1.1F).meat().build()).tab(CreativeModeTab.TAB_FOOD)));
    public static RegistryObject<Item> MATERIAL_IVORY = createItem("material_ivory", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> MATERIAL_WOOLLY_FUR = createItem("material_woolly_fur", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> MATERIAL_ANTLER = createItem("material_antler", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> MATERIAL_SCUTE = createItem("material_scute", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> MATERIAL_HORN = createItem("material_horn", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> MATERIAL_SHARK_TOOTH = createItem("material_shark_tooth", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> MEAT_CETACEAN_RAW = createItem("food_cetacean_raw", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).meat().build()).tab(CreativeModeTab.TAB_FOOD)));
    public static RegistryObject<Item> MEAT_CETACEAN_COOKED = createItem("food_cetacean_cooked", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).meat().build()).tab(CreativeModeTab.TAB_FOOD)));
    public static RegistryObject<Item> FUR_HELMET = createItem("fur_helmet", () -> new ArmorItem(ModArmorMaterials.FUR, EquipmentSlot.HEAD, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> FUR_CHESTPLATE = createItem("fur_chestplate", () -> new ArmorItem(ModArmorMaterials.FUR, EquipmentSlot.CHEST, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> FUR_LEGGINGS = createItem("fur_leggings", () -> new ArmorItem(ModArmorMaterials.FUR, EquipmentSlot.LEGS, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> FUR_BOOTS = createItem("fur_boots", () -> new ArmorItem(ModArmorMaterials.FUR, EquipmentSlot.FEET, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> CARAPACE_HELMET = createItem("carapace_helmet", () -> new ArmorItem(ModArmorMaterials.CARAPACE, EquipmentSlot.HEAD, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> CARAPACE_CHESTPLATE = createItem("carapace_chestplate", () -> new ArmorItem(ModArmorMaterials.CARAPACE, EquipmentSlot.CHEST, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> CARAPACE_LEGGINGS = createItem("carapace_leggings", () -> new ArmorItem(ModArmorMaterials.CARAPACE, EquipmentSlot.LEGS, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> CARAPACE_BOOTS = createItem("carapace_boots", () -> new ArmorItem(ModArmorMaterials.CARAPACE, EquipmentSlot.FEET, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> SERRATED_ARROW = createItem("serrated_arrow", () -> new SerratedArrowItem(new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> HERDING_HORN = createItem("herding_horn", () -> new HerdingHornItem(new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> WOODEN_SPEAR = createItem("wooden_spear", () -> new SpearItem(Tiers.WOOD, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> STONE_SPEAR = createItem("stone_spear", () -> new SpearItem(Tiers.STONE, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> IRON_SPEAR = createItem("iron_spear", () -> new SpearItem(Tiers.IRON, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> GOLDEN_SPEAR = createItem("golden_spear", () -> new SpearItem(Tiers.GOLD, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> DIAMOND_SPEAR = createItem("diamond_spear", () -> new SpearItem(Tiers.DIAMOND, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> NETHERITE_SPEAR = createItem("netherite_spear", () -> new SpearItem(Tiers.NETHERITE, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).fireResistant()));
    public static RegistryObject<Item> CARAPACE_SHIELD = createItem("carapace_shield", () -> new CarapaceShieldItem(new Item.Properties().durability(504).tab(CreativeModeTab.TAB_COMBAT)));
    public static RegistryObject<Item> TAXIDERMY_KIT = createItem("taxidermy_kit", () -> new Item(new Item.Properties().stacksTo(16).tab(ModCreativeModeTab.untamedwilds_items)) {
        public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(new TranslatableComponent("item.untamedwilds.taxidermy_kit.desc").withStyle(ChatFormatting.GRAY));
        }
    });
    public static RegistryObject<Item> MOUNTED_SPECIMEN = createItem("mounted_specimen", () -> new MountedSpecimenItem(new Item.Properties().stacksTo(1)));
    public static RegistryObject<Item> FIELD_GUIDE = createItem("field_guide", () -> new FieldGuideItem(new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> FOOD_TURTLE_SOUP = createItem("food_turtle_soup", () -> new BowlFoodItem(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.6F).build()).tab(CreativeModeTab.TAB_FOOD).stacksTo(1)));
    public static RegistryObject<Item> FOOD_PEMMICAN = createItem("food_pemmican", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(6).saturationMod(1.0F).build()).tab(CreativeModeTab.TAB_FOOD)));
    public static RegistryObject<Item> VEGETABLE_AARDVARK_CUCUMBER = createItem("food_aardvark_cucumber", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.2F).build()).tab(CreativeModeTab.TAB_FOOD)));
    public static RegistryObject<Item> FOOD_HEMLOCK_STEW = createItem("food_hemlock_stew", () -> new BowlFoodItem(new Item.Properties().food((new FoodProperties.Builder()).nutrition(6).saturationMod(0.1F).effect(() -> new MobEffectInstance(MobEffects.POISON, 1200, 3), 1.0F).alwaysEat().build()).tab(CreativeModeTab.TAB_FOOD).stacksTo(1)));

    // Hides
    public static RegistryObject<Item> HIDE_BEAR_ASHEN = createItem("hide_bear_ashen", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BEAR_BLACK = createItem("hide_bear_black", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BEAR_BROWN = createItem("hide_bear_brown", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BEAR_WHITE = createItem("hide_bear_white", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_JAGUAR = createItem("hide_bigcat_jaguar", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_LEOPARD = createItem("hide_bigcat_leopard", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_LION = createItem("hide_bigcat_lion", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_PANTHER = createItem("hide_bigcat_panther", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_PUMA = createItem("hide_bigcat_puma", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_SNOW_LEOPARD = createItem("hide_bigcat_snow_leopard", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_TIGER = createItem("hide_bigcat_tiger", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BEAR_CAVE = createItem("hide_bear_cave", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BEAR_PANDA = createItem("hide_bear_panda", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BEAR_SPECTACLED = createItem("hide_bear_spectacled", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BEAR_SUN = createItem("hide_bear_sun", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BEAR_SHORT_FACED = createItem("hide_bear_short_faced", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BEAR_ARCTOTHERIUM = createItem("hide_bear_arctotherium", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_CAVE_LION = createItem("hide_bigcat_cave_lion", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_AMERICAN_LION = createItem("hide_bigcat_american_lion", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_BARBARY_LION = createItem("hide_bigcat_barbary_lion", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_HOMOTHERIUM = createItem("hide_bigcat_homotherium", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_MARSUPIAL_LION = createItem("hide_bigcat_marsupial_lion", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_SABERTOOTH = createItem("hide_bigcat_sabertooth", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_CHEETAH = createItem("hide_bigcat_cheetah", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_AMERICAN_CHEETAH = createItem("hide_bigcat_american_cheetah", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_CASPIAN_TIGER = createItem("hide_bigcat_caspian_tiger", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_THYLACOSMILUS = createItem("hide_bigcat_thylacosmilus", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
    public static RegistryObject<Item> HIDE_BIGCAT_SMILODON_FATALIS = createItem("hide_bigcat_smilodon_fatalis", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

    // Item Blocks
    public static RegistryObject<Item> SEED_TITAN_ARUM = createItem("flora_titan_arum_corm", () -> new ItemNameBlockItem(ModBlock.TITAN_ARUM.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
    public static RegistryObject<Item> SEED_ZIMBABWE_ALOE = createItem("flora_zimbabwe_aloe_sapling", () -> new ItemNameBlockItem(ModBlock.ZIMBABWE_ALOE.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
    public static RegistryObject<Item> WATER_HYACINTH_BLOCK = createItem("flora_water_hyacinth_item", () -> new WaterLilyBlockItem(ModBlock.WATER_HYACINTH.get(), new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

    public static <I extends Item> RegistryObject<I> createItem(String name, Supplier<? extends I> supplier) {
        return ModItems.ITEMS.register(name, supplier);
    }

    public static void registerSpawnItems() {
        // These items have no associated objects, as they are not supposed to be accessed, and I do not want to register each variant
        // Tarantula Items
        ModItems.ITEMS.register("egg_tarantula", () -> new MobEggItem(ModEntity.TARANTULA, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("bottle_tarantula", () -> new MobBottledItem(ModEntity.TARANTULA, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Small Snake Items
        ModItems.ITEMS.register("egg_snake", () -> new MobEggItem(ModEntity.SNAKE, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("spawn_snake", () -> new MobSpawnItem(ModEntity.SNAKE, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Softshell Turtle Items
        ModItems.ITEMS.register("egg_softshell_turtle", () -> new MobEggItem(ModEntity.SOFTSHELL_TURTLE, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("spawn_softshell_turtle", () -> new MobSpawnItem(ModEntity.SOFTSHELL_TURTLE, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Giant Clam Items
        ModItems.ITEMS.register("egg_giant_clam", () -> new MobEggItem(ModEntity.GIANT_CLAM, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("spawn_giant_clam", () -> new MobSpawnItem(ModEntity.GIANT_CLAM, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Sunfish Items
        ModItems.ITEMS.register("egg_sunfish", () -> new MobEggItem(ModEntity.SUNFISH, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Trevally Items
        ModItems.ITEMS.register("egg_trevally", () -> new MobEggItem(ModEntity.TREVALLY, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("bucket_trevally", () -> new MobBucketedItem(ModEntity.TREVALLY, Fluids.WATER, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Arowana Items
        ModItems.ITEMS.register("egg_arowana", () -> new MobEggItem(ModEntity.AROWANA, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("bucket_arowana", () -> new MobBucketedItem(ModEntity.AROWANA, Fluids.WATER, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Football Fish Items
        ModItems.ITEMS.register("egg_football_fish", () -> new MobEggItem(ModEntity.FOOTBALL_FISH, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("bucket_football_fish", () -> new MobBucketedItem(ModEntity.FOOTBALL_FISH, Fluids.WATER, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Giant Salamander Items
        ModItems.ITEMS.register("egg_giant_salamander", () -> new MobEggItem(ModEntity.GIANT_SALAMANDER, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("bucket_giant_salamander", () -> new MobBucketedItem(ModEntity.GIANT_SALAMANDER, Fluids.WATER, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Newt Items
        ModItems.ITEMS.register("egg_newt", () -> new MobEggItem(ModEntity.NEWT, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("bucket_newt", () -> new MobBucketedItem(ModEntity.NEWT, Fluids.WATER, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Tortoise Items
        ModItems.ITEMS.register("egg_tortoise", () -> new MobEggItem(ModEntity.TORTOISE, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("spawn_tortoise", () -> new MobSpawnItem(ModEntity.TORTOISE, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Large Snake Items
        ModItems.ITEMS.register("egg_large_snake", () -> new MobEggItem(ModEntity.ANACONDA, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Triggerfish Items
        ModItems.ITEMS.register("egg_triggerfish", () -> new MobEggItem(ModEntity.TRIGGERFISH, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("bucket_triggerfish", () -> new MobBucketedItem(ModEntity.TRIGGERFISH, Fluids.WATER, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Catfish Items
        ModItems.ITEMS.register("egg_catfish", () -> new MobEggItem(ModEntity.CATFISH, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("bucket_catfish", () -> new MobBucketedItem(ModEntity.CATFISH, Fluids.WATER, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // King Crab Items
        ModItems.ITEMS.register("egg_king_crab", () -> new MobEggItem(ModEntity.KING_CRAB, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("bucket_king_crab", () -> new MobBucketedItem(ModEntity.KING_CRAB, Fluids.WATER, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Monitor Items
        ModItems.ITEMS.register("egg_monitor", () -> new MobEggItem(ModEntity.MONITOR, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

        // Spadefish Items
        ModItems.ITEMS.register("egg_spadefish", () -> new MobEggItem(ModEntity.SPADEFISH, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));
        ModItems.ITEMS.register("bucket_spadefish", () -> new MobBucketedItem(ModEntity.SPADEFISH, Fluids.WATER, new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));

    }
}
