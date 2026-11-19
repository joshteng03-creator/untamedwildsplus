package untamedwilds;

import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import untamedwilds.block.CageBlock;
import untamedwilds.compat.CompatBridge;
import untamedwilds.config.ConfigBase;
import untamedwilds.config.EcologyMode;
import untamedwilds.init.*;
import untamedwilds.network.UntamedInstance;
import untamedwilds.world.UntamedWildsGenerator;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import untamedwilds.entity.ProjectileSerratedArrow;
import untamedwilds.init.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import untamedwilds.init.ModRecipes;
import untamedwilds.item.SpearItem;

@Mod(value = UntamedWilds.MOD_ID)
public class UntamedWilds {

    // TODO: Abstract Herd logic to be functional with any LivingEntity (instead of being limited to IPackEntity ComplexMob)
    // TODO: Store the children's UUID in their mother's NBT, to allow checking for Children without constant AABB checking
    // TODO: Have carnivorous mobs gain hunger when attacking
    // TODO: Move Variants from Int to String to prevent shuffling?
    // TODO: Gillie suit bauble, to prevent animals from spotting you

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "untamedwilds";
    public static final boolean DEBUG = false;

    public UntamedWilds() {
        final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigBase.common_config);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, untamedwilds.config.ConfigClient.SPEC);
        ConfigBase.loadConfig(ConfigBase.common_config, FMLPaths.CONFIGDIR.get().resolve("untamedwilds-common.toml").toString());
        /* Registered HERE and not in setupCommon: vanilla builds each world's GameRules instance from
         * the static type map, so a rule added after the first world has loaded is simply absent from
         * it. The config load above has to come first, since the rule's default is read from it. */
        EcologyMode.register();
        eventBus.addListener(this::setupCommon);
        eventBus.addListener(this::setupClient);
        ModBlock.BLOCKS.register(eventBus);
        ModBlock.TILE_ENTITIES.register(eventBus);
        ModItems.ITEMS.register(eventBus);
        ModEntity.ENTITIES.register(eventBus);
        ModRecipes.SERIALIZERS.register(eventBus);
        ModItems.registerSpawnItems();
        ModSounds.SOUNDS.register(eventBus);
        ModAdvancementTriggers.register();
        UntamedWildsGenerator.FEATURES.register(eventBus);
        UntamedWildsGenerator.CONFIGURED_FEATURES.register(eventBus);
        UntamedWildsGenerator.PLACED_FEATURES.register(eventBus);
        CompatBridge.RegisterCompat();
        UntamedWildsGenerator.readBioDiversityLevels();
    }

    private void setupCommon(final FMLCommonSetupEvent event) {
        UntamedInstance.registerMessages();
        DispenserBlock.registerBehavior(ModBlock.TRAP_CAGE.get().asItem(), new CageBlock.DispenserBehaviorTrapCage());
        // Dispensers fire serrated arrows like vanilla arrows.
        DispenserBlock.registerBehavior(ModItems.SERRATED_ARROW.get(), new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level level, Position pos, ItemStack stack) {
                ProjectileSerratedArrow arrow = new ProjectileSerratedArrow(level, pos.x(), pos.y(), pos.z());
                arrow.pickup = AbstractArrow.Pickup.ALLOWED;
                return arrow;
            }
        });
    }

    private void setupClient(final FMLClientSetupEvent event) {
        ModBlock.registerRendering();
        ModBlock.registerBlockColors();
        event.enqueueWork(() -> {
            // Spear trims swap the item model; the carapace shield needs vanilla's "blocking" predicate,
            // which vanilla only registers for its own shield.
            for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
                if (item.get() instanceof SpearItem) {
                    ItemProperties.register(item.get(), new ResourceLocation(MOD_ID, "trim"), (stack, level, entity, seed) -> SpearItem.trimModelValue(stack));
                }
            }
            ItemProperties.register(ModItems.CARAPACE_SHIELD.get(), new ResourceLocation("blocking"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });

        //ModBlock.registerBlockColors();
        //ModParticles.registerParticles(); Handled through events
    }
}
