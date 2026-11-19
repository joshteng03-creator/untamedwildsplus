package untamedwilds.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.config.ConfigMobControl;
import untamedwilds.config.EcologyMode;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.INeedsPostUpdate;
import untamedwilds.entity.ISpecies;
import untamedwilds.util.EntityUtils;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Slow immigration (2026-10-24): now and then a small group of a native species walks in near a player when
 * that species is locally absent, so a local extinction is not permanent and predator/prey cycles can restart.
 * <p>
 * The mod only ever spawned at world generation, so the loaded area around a player was a closed system:
 * once a species was eaten out of it, it was gone for good unless a herd happened to migrate back in. Real
 * populations are open -- animals disperse in from the surrounding range -- and that rescue effect is what
 * keeps a metapopulation alive through local crashes.
 * <p>
 * Deliberately conservative: one attempt per player per {@code immigration_interval}, a group of two to four
 * (a male/female pair for predators), only for a species with NO adults within 128 blocks, only predators
 * where there is enough prey per predator for them to breed, never past {@code immigration_local_cap}
 * animals, and Ecosystem mode only. Spawns go through {@code finalizeSpawn}, so the species is the biome's
 * own, and size, gender and age are set exactly as at world generation.
 */
@Mod.EventBusSubscriber(modid = UntamedWilds.MOD_ID)
public class ImmigrationHandler {

    private static final int MIN_DISTANCE = 64;
    private static final int MAX_DISTANCE = 128;
    private static final double ABSENCE_RADIUS = 128D;

    private ImmigrationHandler() {}

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END || !(event.world instanceof ServerLevel level)) {
            return;
        }
        if (!EcologyMode.allowsImmigration(level)
                || ConfigMobControl.dimensionBlacklist.get().contains(level.dimension().location().toString())) {
            return;
        }
        int interval = ConfigGamerules.immigrationInterval.get();
        List<ServerPlayer> players = level.players();
        for (int p = 0; p < players.size(); p++) {
            // Staggered per player, so several players never trigger on the same tick.
            if ((level.getGameTime() + p * 977L) % interval == 0) {
                tryImmigrate(level, players.get(p));
            }
        }
    }

    private static void tryImmigrate(ServerLevel level, ServerPlayer player) {
        Random random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2D;
        int dist = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);
        BlockPos column = player.blockPosition().offset(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
        if (!level.isLoaded(column)) {
            return;
        }
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        if (!level.getFluidState(pos.below()).isEmpty() || !level.getFluidState(pos).isEmpty()) {
            return;
        }
        AABB area = new AABB(pos).inflate(ABSENCE_RADIUS, 64D, ABSENCE_RADIUS);
        if (level.getEntitiesOfClass(ComplexMob.class, area).size() > ConfigGamerules.immigrationLocalCap.get()) {
            return;
        }
        boolean predator = random.nextDouble() < ConfigGamerules.immigrationPredatorShare.get();
        Optional<FaunaHandler.SpawnListEntry> entry = WeightedRandom.getRandomItem(random,
                FaunaHandler.getSpawnableList(predator ? FaunaHandler.animalType.APEX_PRED : FaunaHandler.animalType.LARGE_HERB));
        if (entry.isEmpty() || entry.get().entityType == null) {
            return;
        }
        EntityType<?> type = entry.get().entityType;
        ComplexMob first = create(level, type, pos);
        if (first == null) {
            return;
        }
        int variant = first.getVariant();
        if (variant == 99 || !ComplexMob.ENTITY_DATA_HASH.containsKey(type)) {
            return;       // the biome has no species of this type
        }
        // Only a species that is locally ABSENT immigrates.
        if (!level.getEntitiesOfClass(ComplexMob.class, area,
                m -> m.isAlive() && !m.isBaby() && m.getType() == type && m.getVariant() == variant).isEmpty()) {
            return;
        }
        int group = predator ? 2 : 2 + random.nextInt(3);
        group = Math.min(group, Math.max(2, EntityUtils.getPackSize(type, variant)));
        if (predator) {
            float[] census = ComplexMob.countPreyAndPredators(first, pos, ConfigGamerules.ratioRadius.get());
            // census[1] counts only predators already here (the newcomers are not in the world yet).
            if (census[0] / (census[1] + group) < ConfigGamerules.predatorBreedRatio.get()) {
                return;
            }
        }
        int firstGender = first.getGender();
        level.addFreshEntityWithPassengers(first);
        for (int i = 1; i < group; i++) {
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3));
            ComplexMob mate = create(level, type, at);
            if (mate == null) {
                continue;
            }
            mate.setVariant(variant);
            if (i == 1) {
                mate.setGender(1 - firstGender);     // always a breeding pair
            }
            mate.chooseSkinForSpecies(mate, false);
            if (mate instanceof INeedsPostUpdate post) {
                post.updateAttributes();
            }
            level.addFreshEntityWithPassengers(mate);
        }
        UntamedWilds.LOGGER.info("Immigration: {} x{} ({}) arrived at {}", first instanceof ISpecies s ? s.getRawSpeciesName(variant)
                : type.getRegistryName(), group, predator ? "predator" : "herbivore", pos);
    }

    /** One adult of {@code type} at {@code pos}, through finalizeSpawn, or null. Not yet added to the world. */
    private static ComplexMob create(ServerLevel level, EntityType<?> type, BlockPos pos) {
        Entity entity;
        try {
            entity = type.create(level);
        } catch (Exception e) {
            return null;
        }
        if (!(entity instanceof ComplexMob mob)) {
            if (entity != null) {
                entity.discard();
            }
            return null;
        }
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360F, 0F);
        if (!level.noCollision(mob)) {
            return null;
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
        if (!mob.isAlive()) {
            return null;
        }
        if (mob.isBaby()) {
            mob.setAge(0);   // immigrants are dispersing adults
        }
        return mob;
    }
}
