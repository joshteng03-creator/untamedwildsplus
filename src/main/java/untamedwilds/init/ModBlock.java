package untamedwilds.init;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import untamedwilds.UntamedWilds;
import untamedwilds.block.*;
import untamedwilds.block.blockentity.CageBlockEntity;
import untamedwilds.block.blockentity.CritterBurrowBlockEntity;
import untamedwilds.block.blockentity.EggBlockEntity;
import untamedwilds.block.blockentity.ReptileNestBlockEntity;
import untamedwilds.util.ModCreativeModeTab;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import untamedwilds.block.AntlerChandelierBlock;
import untamedwilds.block.AntlerTrophyBlock;
import untamedwilds.block.FeedingTroughBlock;
import untamedwilds.block.SaltLickBlock;
import untamedwilds.block.SpeciesPlacardBlock;
import untamedwilds.block.blockentity.FeedingTroughBlockEntity;
import untamedwilds.block.blockentity.SaltLickBlockEntity;
import untamedwilds.block.blockentity.SpeciesPlacardBlockEntity;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = UntamedWilds.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlock {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, UntamedWilds.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, UntamedWilds.MOD_ID);

    private final static List<Pair<RegistryObject<Block>, String>> RENDER_TYPE_DATA = Lists.newArrayList();

    // Carpets
    public static RegistryObject<Block> CARPET_STRAW  = createBlock("carpet_straw", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.SAND).destroyTime(0.1F).sound(SoundType.CROP)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BEAR_ASHEN  = createBlock("carpet_bear_ashen", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_GRAY).destroyTime(0.1F).sound(SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BEAR_BLACK  = createBlock("carpet_bear_black", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_BLACK).destroyTime(0.1F).sound(SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BEAR_BROWN  = createBlock("carpet_bear_brown", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_BROWN).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BEAR_WHITE  = createBlock("carpet_bear_white", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.SNOW).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_JAGUAR  = createBlock("carpet_bigcat_jaguar", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.GOLD).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_LEOPARD  = createBlock("carpet_bigcat_leopard", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.GOLD).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_LION  = createBlock("carpet_bigcat_lion", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_BROWN).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_PANTHER  = createBlock("carpet_bigcat_panther", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.SNOW).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_PUMA  = createBlock("carpet_bigcat_puma", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_GRAY).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_SNOW  = createBlock("carpet_bigcat_snow_leopard", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_BLACK).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_TIGER  = createBlock("carpet_bigcat_tiger", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_BROWN).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BEAR_CAVE  = createBlock("carpet_bear_cave", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_BROWN).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BEAR_PANDA  = createBlock("carpet_bear_panda", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.SNOW).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BEAR_SPECTACLED  = createBlock("carpet_bear_spectacled", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_BLACK).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BEAR_SUN  = createBlock("carpet_bear_sun", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_BLACK).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BEAR_SHORT_FACED  = createBlock("carpet_bear_short_faced", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.TERRACOTTA_ORANGE).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BEAR_ARCTOTHERIUM  = createBlock("carpet_bear_arctotherium", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_BROWN).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_CAVE_LION  = createBlock("carpet_bigcat_cave_lion", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.SAND).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_AMERICAN_LION  = createBlock("carpet_bigcat_american_lion", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_ORANGE).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_BARBARY_LION  = createBlock("carpet_bigcat_barbary_lion", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.SAND).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_HOMOTHERIUM  = createBlock("carpet_bigcat_homotherium", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.SAND).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_MARSUPIAL_LION  = createBlock("carpet_bigcat_marsupial_lion", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_ORANGE).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_SABERTOOTH  = createBlock("carpet_bigcat_sabertooth", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_GRAY).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_CHEETAH  = createBlock("carpet_bigcat_cheetah", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.GOLD).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_AMERICAN_CHEETAH  = createBlock("carpet_bigcat_american_cheetah", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_LIGHT_GRAY).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_CASPIAN_TIGER  = createBlock("carpet_bigcat_caspian_tiger", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_ORANGE).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_THYLACOSMILUS  = createBlock("carpet_bigcat_thylacosmilus", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.COLOR_GRAY).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CARPET_BIGCAT_SMILODON_FATALIS  = createBlock("carpet_bigcat_smilodon_fatalis", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.TERRACOTTA_BROWN).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);

    // Storage
    public static RegistryObject<Block> LARD_BLOCK  = createBlock("block_lard", () -> new LardBlock(Block.Properties.of(Material.CLAY, MaterialColor.COLOR_YELLOW).destroyTime(0.1F).sound(SoundType.SLIME_BLOCK)), CreativeModeTab.TAB_BUILDING_BLOCKS);
    public static RegistryObject<Block> PEARL_BLOCK  = createBlock("block_pearl", () -> new Block(Block.Properties.of(Material.STONE, MaterialColor.COLOR_CYAN).destroyTime(5.0F).sound(SoundType.STONE)), CreativeModeTab.TAB_BUILDING_BLOCKS);
    // Ivory set (2026-10-06): bone-block hardness and sound, quartz-style variants. Pillars like the bone block.
    public static RegistryObject<Block> IVORY_BLOCK = createBlock("ivory_block", () -> new RotatedPillarBlock(Block.Properties.of(Material.STONE, MaterialColor.SAND).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.BONE_BLOCK)), CreativeModeTab.TAB_BUILDING_BLOCKS);
    public static RegistryObject<Block> IVORY_SLAB = createBlock("ivory_slab", () -> new SlabBlock(Block.Properties.of(Material.STONE, MaterialColor.SAND).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.BONE_BLOCK)), CreativeModeTab.TAB_BUILDING_BLOCKS);
    public static RegistryObject<Block> IVORY_STAIRS = createBlock("ivory_stairs", () -> new StairBlock(() -> ModBlock.IVORY_BLOCK.get().defaultBlockState(), Block.Properties.of(Material.STONE, MaterialColor.SAND).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.BONE_BLOCK)), CreativeModeTab.TAB_BUILDING_BLOCKS);
    public static RegistryObject<Block> IVORY_WALL = createBlock("ivory_wall", () -> new WallBlock(Block.Properties.of(Material.STONE, MaterialColor.SAND).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.BONE_BLOCK)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CHISELED_IVORY = createBlock("chiseled_ivory", () -> new RotatedPillarBlock(Block.Properties.of(Material.STONE, MaterialColor.SAND).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.BONE_BLOCK)), CreativeModeTab.TAB_BUILDING_BLOCKS);
    public static RegistryObject<Block> DISPLAY_PLINTH = createBlock("display_plinth", () -> new Block(Block.Properties.of(Material.STONE, MaterialColor.STONE).requiresCorrectToolForDrops().strength(2.0F, 6.0F)), CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> ANTLER_CHANDELIER = createBlock("antler_chandelier", () -> new AntlerChandelierBlock(Block.Properties.of(Material.WOOD, MaterialColor.SAND).strength(1.0F).sound(SoundType.BONE_BLOCK).noOcclusion().lightLevel(state -> 15)), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> ANTLER_TROPHY = createBlock("antler_trophy", () -> new AntlerTrophyBlock(Block.Properties.of(Material.WOOD, MaterialColor.SAND).strength(1.0F).sound(SoundType.BONE_BLOCK).noOcclusion()), "cutout", CreativeModeTab.TAB_DECORATIONS);
    // Zoo blocks (2026-10-08)
    public static RegistryObject<Block> FEEDING_TROUGH = createBlock("feeding_trough", () -> new FeedingTroughBlock(Block.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion()), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> SPECIES_PLACARD = createBlock("species_placard", () -> new SpeciesPlacardBlock(Block.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion()), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> SALT_LICK = createBlock("salt_lick", () -> new SaltLickBlock(Block.Properties.of(Material.STONE, MaterialColor.QUARTZ).strength(0.8F).sound(SoundType.CALCITE).noOcclusion()), CreativeModeTab.TAB_DECORATIONS);

    // Machines
    public static RegistryObject<Block> TRAP_CAGE  = createBlock("trap_cage", () -> new CageBlock(Block.Properties.of(Material.WOOD, MaterialColor.WOOD).destroyTime(3.0F).sound(SoundType.WOOD)), CreativeModeTab.TAB_TRANSPORTATION);

    // Fauna
    public static RegistryObject<Block> ANEMONE_ROSE_BULB  = createBlock("anemone_rose_bulb", () -> new AnemoneBlock(Block.Properties.of(Material.WATER_PLANT, MaterialColor.COLOR_RED).destroyTime(0.1F).sound(SoundType.SLIME_BLOCK)), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> ANEMONE_SAND  = createBlock("anemone_sand", () -> new AnemoneBlock(Block.Properties.of(Material.WATER_PLANT, MaterialColor.COLOR_PINK).destroyTime(0.1F).sound(SoundType.SLIME_BLOCK)), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> ANEMONE_SEBAE  = createBlock("anemone_sebae", () -> new AnemoneBlock(Block.Properties.of(Material.WATER_PLANT, MaterialColor.TERRACOTTA_WHITE).destroyTime(0.1F).sound(SoundType.SLIME_BLOCK)), "cutout", CreativeModeTab.TAB_DECORATIONS);

    // Flora - Reeds
    public static RegistryObject<Block> COMMON_REED = createBlock("flora_common_reed", () -> new ReedBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(0.1F).sound(SoundType.VINE).noCollission()), "cutout", CreativeModeTab.TAB_DECORATIONS, 100);
    // Flora - Bushes
    public static RegistryObject<Block> BUSH_TEMPERATE = createBlock("flora_bush_temperate", () -> new UndergrowthBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(1.0F).sound(SoundType.AZALEA_LEAVES).noCollission()), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> BUSH_CREOSOTE = createBlock("flora_bush_creosote", () -> new UndergrowthBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(1.0F).sound(SoundType.AZALEA_LEAVES).noCollission()), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> ELEPHANT_EAR = createBlock("flora_elephant_ear", () -> new UndergrowthBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(1.0F).sound(SoundType.WET_GRASS).noCollission(), BlockBehaviour.OffsetType.XYZ), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> HEMLOCK = createBlock("flora_hemlock", () -> new UndergrowthPoisonousBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(0.1F).sound(SoundType.GRASS).noCollission(), BlockBehaviour.OffsetType.XYZ), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> YARROW = createBlock("flora_yarrow", () -> new CustomGrassBlock(MobEffects.REGENERATION, 4, Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(0.0F).sound(SoundType.GRASS).noCollission()), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> JUNEGRASS = createBlock("flora_junegrass", () -> new CustomGrassBlock(MobEffects.UNLUCK, 4, Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(0.0F).sound(SoundType.GRASS).noCollission()), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> CANOLA = createBlock("flora_canola", () -> new CustomGrassBlock(MobEffects.DAMAGE_BOOST, 4, Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(0.0F).sound(SoundType.GRASS).noCollission()), "cutout", CreativeModeTab.TAB_DECORATIONS);
    // Flora - Multistage
    public static RegistryObject<Block> ZIMBABWE_ALOE = createItemlessBlock("flora_zimbabwe_aloe", () -> new TallPlantBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(1.0F).sound(SoundType.WOOD).dynamicShape()), "cutout");
    public static RegistryObject<Block> PAMPAS_GRASS = createBlock("flora_pampas_grass", () -> new TallGrassBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(1.0F).sound(SoundType.GRASS).dynamicShape()), "cutout", CreativeModeTab.TAB_DECORATIONS);
    // Flora - Floating
    public static RegistryObject<Block> WATER_HYACINTH = createItemlessBlock("flora_water_hyacinth", () -> new FloatingPlantBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(0.0F).sound(SoundType.LILY_PAD).noCollission()), "cutout");
    // Flora - Algae
    public static RegistryObject<Block> AMAZON_SWORD = createBlock("flora_amazon_sword", () -> new AlgaeBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(0).sound(SoundType.WET_GRASS).noCollission()), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> EELGRASS = createBlock("flora_eelgrass", () -> new AlgaeBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(0).sound(SoundType.WET_GRASS).noCollission()), "cutout", CreativeModeTab.TAB_DECORATIONS);
    // Flora - Flowers
    public static RegistryObject<Block> ORCHID_MAGENTA = createBlock("flora_orchid_magenta", () -> new EpyphitePlantBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_MAGENTA).destroyTime(0.0F).sound(SoundType.VINE)), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> ORCHID_PURPLE = createBlock("flora_orchid_purple", () -> new EpyphitePlantBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_PURPLE).destroyTime(0.0F).sound(SoundType.VINE)), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> ORCHID_PINK = createBlock("flora_orchid_pink", () -> new EpyphitePlantBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_PINK).destroyTime(0.0F).sound(SoundType.VINE)), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> ORCHID_RED = createBlock("flora_orchid_red", () -> new EpyphitePlantBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_RED).destroyTime(0.0F).sound(SoundType.VINE)), "cutout", CreativeModeTab.TAB_DECORATIONS);
    public static RegistryObject<Block> TITAN_ARUM = createItemlessBlock("flora_titan_arum", () -> new TitanArumBlock(Block.Properties.of(Material.PLANT, MaterialColor.COLOR_GREEN).destroyTime(2.0F).sound(SoundType.WET_GRASS).noCollission().dynamicShape()), "cutout");

    // Nests
    public static RegistryObject<Block> NEST_REPTILE = createBlock("nest_reptile", () -> new NestReptileBlock(Block.Properties.of(Material.DIRT, MaterialColor.DIRT).destroyTime(1.0F).sound(SoundType.GRAVEL)),  "translucent", ModCreativeModeTab.untamedwilds_items);

    // Eggs
    public static RegistryObject<Block> EGG_SPITTER = createBlock("egg_spitter", () -> new StrangeEggBlock(Block.Properties.of(Material.DIRT, MaterialColor.DIRT).destroyTime(1.0F).sound(SoundType.SLIME_BLOCK)), "cutout", ModCreativeModeTab.untamedwilds_items);

    // Technical Blocks
    public static RegistryObject<Block> BURROW = createBlock("block_burrow", () -> new CritterBurrowBlock(Block.Properties.of(Material.DIRT, MaterialColor.DIRT).destroyTime(1.0F).sound(SoundType.GRAVEL).noCollission()),  "translucent", CreativeModeTab.TAB_DECORATIONS);

    // Block Entities
    public static final RegistryObject<BlockEntityType<CageBlockEntity>> TILE_ENTITY_CAGE = TILE_ENTITIES.register("trap_cage", () -> BlockEntityType.Builder.of(CageBlockEntity::new, ModBlock.TRAP_CAGE.get()).build(null));
    public static final RegistryObject<BlockEntityType<CritterBurrowBlockEntity>> TILE_ENTITY_BURROW = TILE_ENTITIES.register("critter_burrow", () -> BlockEntityType.Builder.of(CritterBurrowBlockEntity::new, ModBlock.BURROW.get()).build(null));
    public static final RegistryObject<BlockEntityType<ReptileNestBlockEntity>> TILE_ENTITY_NEST_REPTILE = TILE_ENTITIES.register("nest_reptile_block_entity", () -> BlockEntityType.Builder.of(ReptileNestBlockEntity::new, ModBlock.NEST_REPTILE.get()).build(null));
    public static final RegistryObject<BlockEntityType<EggBlockEntity>> TILE_ENTITY_EGG = TILE_ENTITIES.register("strange_egg", () -> BlockEntityType.Builder.of(EggBlockEntity::new, ModBlock.EGG_SPITTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<FeedingTroughBlockEntity>> TILE_ENTITY_TROUGH = TILE_ENTITIES.register("feeding_trough", () -> BlockEntityType.Builder.of(FeedingTroughBlockEntity::new, ModBlock.FEEDING_TROUGH.get()).build(null));
    public static final RegistryObject<BlockEntityType<SaltLickBlockEntity>> TILE_ENTITY_SALT_LICK = TILE_ENTITIES.register("salt_lick", () -> BlockEntityType.Builder.of(SaltLickBlockEntity::new, ModBlock.SALT_LICK.get()).build(null));
    public static final RegistryObject<BlockEntityType<SpeciesPlacardBlockEntity>> TILE_ENTITY_PLACARD = TILE_ENTITIES.register("species_placard", () -> BlockEntityType.Builder.of(SpeciesPlacardBlockEntity::new, ModBlock.SPECIES_PLACARD.get()).build(null));

    public static <B extends Block> RegistryObject<Block> createBlock(String name, Supplier<? extends B> supplier, CreativeModeTab group) {
        return createBlock(name, supplier, null, group, 0);
    }

    public static <B extends Block> RegistryObject<Block> createBlock(String name, Supplier<? extends B> supplier, @Nullable String renderType, CreativeModeTab group) {
        return createBlock(name, supplier, renderType, group, 0);
    }

    public static <B extends Block> RegistryObject<Block> createBlock(String name, Supplier<? extends B> supplier, @Nullable String renderType, CreativeModeTab group, int burnTime) {
        RegistryObject<Block> block = ModBlock.BLOCKS.register(name, supplier);
        if (renderType != null) {
            RENDER_TYPE_DATA.add(new Pair<>(block, renderType));
        }
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().tab(group)) {
            public int getBurnTime(ItemStack itemStack, RecipeType<?> recipeType) {
                return burnTime;
            }
        });
        return block;
    }

    public static <B extends Block> RegistryObject<Block> createItemlessBlock(String name, Supplier<? extends B> supplier) {
        return createItemlessBlock(name, supplier, null);
    }

    public static <B extends Block> RegistryObject<Block> createItemlessBlock(String name, Supplier<? extends B> supplier, @Nullable String renderType) {
        RegistryObject<Block> block = ModBlock.BLOCKS.register(name, supplier);
        if (renderType != null) {
            RENDER_TYPE_DATA.add(new Pair<>(block, renderType));
        }
        return block;
    }

    public static void registerRendering() {
        for (Pair<RegistryObject<Block>, String> i : RENDER_TYPE_DATA) {
            RenderType j = RenderType.cutout();
            if (Objects.equals(i.getSecond(), "cutout"))
                j = RenderType.cutout();
            else if (Objects.equals(i.getSecond(), "translucent"))
                j = RenderType.translucent();
            ItemBlockRenderTypes.setRenderLayer(i.getFirst().get(), j);
        }
    }

    public static void registerBlockColors(){
        BlockColors colors = Minecraft.getInstance().getBlockColors();
        final BlockColor grassColor = (state, worldIn, pos, tintIndex) -> worldIn != null && pos != null ? BiomeColors.getAverageGrassColor(worldIn, pos) : GrassColor.get(0.5D, 1.0D);

        colors.register(grassColor, ModBlock.YARROW.get());
        colors.register(grassColor, ModBlock.JUNEGRASS.get());
        colors.register(grassColor, ModBlock.CANOLA.get());
    }
}
