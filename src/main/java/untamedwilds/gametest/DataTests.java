package untamedwilds.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.RegistryObject;
import untamedwilds.UntamedWilds;
import untamedwilds.entity.ComplexMob;
import untamedwilds.init.ModEntity;
import untamedwilds.network.SyncEntityData;
import untamedwilds.util.EntityDataHolder;
import untamedwilds.util.EntityDataListenerEvent;
import untamedwilds.util.SpeciesDataHolder;

/** Data-layer tests: every type's species data reaches a dedicated-server client intact. */
@GameTestHolder(UntamedWilds.MOD_ID)
@PrefixGameTestTemplate(false)
public class DataTests {

    /**
     * Citadel syncs animations by INDEX into getAnimations(): every type's array must be duplicate-free, short
     * enough for the packet, and contain every animation its eat / drink / alert hooks return.
     */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 100, batch = "p3_anim_index")
    public static void animation_tables_are_consistent(GameTestHelper helper) {
        int checked = 0;
        for (RegistryObject<EntityType<?>> entry : ModEntity.ENTITIES.getEntries()) {
            net.minecraft.world.entity.Entity e = entry.get().create(helper.getLevel());
            if (!(e instanceof com.github.alexthe666.citadel.animation.IAnimatedEntity animated)) {
                continue;
            }
            com.github.alexthe666.citadel.animation.Animation[] all = animated.getAnimations();
            java.util.Set<Object> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (com.github.alexthe666.citadel.animation.Animation a : all) {
                GTUtil.check(seen.add(a), e.getType().getRegistryName() + " declares an animation twice");
            }
            GTUtil.check(all.length < 127, e.getType().getRegistryName() + " has " + all.length + " animations");
            if (e instanceof untamedwilds.entity.ComplexMobTerrestrial land) {
                for (com.github.alexthe666.citadel.animation.Animation hook : new com.github.alexthe666.citadel.animation.Animation[]{
                        land.getAnimationEat(), land.getAnimationDrink(), land.getAnimationAlert()}) {
                    GTUtil.check(hook == com.github.alexthe666.citadel.animation.IAnimatedEntity.NO_ANIMATION || seen.contains(hook),
                            e.getType().getRegistryName() + " returns an idle animation missing from getAnimations()");
                }
            }
            e.discard();
            checked++;
        }
        UntamedWilds.LOGGER.info("[gametest] animation tables consistent for {} types", checked);
        GTUtil.done(helper);
    }

    /**
     * The dedicated-server crash: only 32 of 51 types were registered eagerly, and a client only ever received
     * skins and names. Every type with a JSON must now be registered, and its full holder must survive the
     * NBT round trip a client performs -- same species, same names, same skins, same per-species flags.
     */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 100, batch = "p1_data")
    public static void data_every_type_syncs_to_clients(GameTestHelper helper) {
        EntityDataListenerEvent.registerData();
        int checked = 0;
        for (RegistryObject<EntityType<?>> entry : ModEntity.ENTITIES.getEntries()) {
            EntityType<?> type = entry.get();
            ResourceLocation id = type.getRegistryName();
            if (id == null || EntityDataListenerEvent.ENTITY_DATA_HOLDERS.getData(id) == null) {
                continue;   // projectiles and the mounted specimen have no species data
            }
            EntityDataHolder data = ComplexMob.ENTITY_DATA_HASH.get(type);
            GTUtil.check(data != null, "no species data registered for " + id);
            SyncEntityData packet = SyncEntityData.of(type, data);
            GTUtil.check(packet != null, "species data for " + id + " does not encode");
            EntityDataHolder back = EntityDataHolder.CODEC.parse(NbtOps.INSTANCE, packet.data()).result().orElse(null);
            GTUtil.check(back != null, "species data for " + id + " does not decode");
            GTUtil.check(back.getSpeciesData().size() == data.getSpeciesData().size(), id + ": species count changed in sync");
            for (int i = 0; i < data.getSpeciesData().size(); i++) {
                SpeciesDataHolder a = data.getSpeciesData().get(i);
                SpeciesDataHolder b = back.getSpeciesData().get(i);
                GTUtil.check(a.getName().equals(b.getName()), id + " #" + i + ": name changed in sync");
                GTUtil.check(a.getSkins() == b.getSkins(), id + " " + a.getName() + ": skins changed in sync");
                GTUtil.check(a.getFlags().equals(b.getFlags()), id + " " + a.getName() + ": flags changed in sync");
            }
            checked++;
        }
        GTUtil.check(checked >= 50, "only " + checked + " entity types had species data");
        UntamedWilds.LOGGER.info("[gametest] species data round-trips for {} types", checked);
        GTUtil.done(helper);
    }
}
