package untamedwilds.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.INeedsPostUpdate;

/**
 * Shared helpers for the headless GameTests (2.8.0 harness). Never shipped: build.gradle excludes the
 * whole {@code untamedwilds.gametest} package from the jar.
 */
public final class GTUtil {

    private GTUtil() {}

    /** 1.18.2's GameTestHelper has no assertTrue; this is it. Throwing fails the test with the message. */
    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    /**
     * Ends a test cleanly: finished test structures stay in the world, and their animals would otherwise keep
     * ticking -- and bump the global PerfCounters -- while later batches run.
     */
    public static void done(GameTestHelper helper) {
        helper.killAllEntities();
        helper.succeed();
    }

    /** Diagnostics: the goals an animal is running right now and the state flags that gate idle behaviour. */
    public static String describe(ComplexMob mob) {
        StringBuilder sb = new StringBuilder();
        mob.goalSelectorForTests().getRunningGoals().forEach(g ->
                sb.append(g.getGoal().getClass().getSimpleName()).append('@').append(g.getPriority()).append(' '));
        return "pos " + mob.blockPosition() + " goals [" + sb.toString().trim() + "] sit " + mob.isSitting()
                + " sleep " + mob.isSleeping() + " target " + (mob.getTarget() != null) + " flee " + mob.fleeCooldown
                + " alert " + mob.alertTicks + " hunger " + mob.getHunger() + " water " + mob.isInWater()
                + " nextDrink " + (mob.nextDrinkTime - mob.level.getGameTime()) + " drinks " + mob.drinkVisits;
    }

    /** Keeps an animal awake for the whole test (crepuscular/nocturnal species otherwise sleep at noon). */
    public static <T extends ComplexMob> T awake(T mob) {
        if (mob instanceof untamedwilds.entity.ComplexMobTerrestrial land) {
            land.forceSleep = -1_000_000;
            land.setSleeping(false);
        }
        return mob;
    }

    /** Noon, clear weather: deterministic lighting/sleep state for every behaviour test. */
    public static void noon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.setDayTime(6000L);
        level.setWeatherParameters(6000, 0, false, false);
    }

    /**
     * Spawns a mob the way worldgen does ({@code finalizeSpawn}: gender, size, age, herd) and then forces
     * the species, so a test never depends on the test world's biome.
     */
    public static <T extends ComplexMob> T spawn(GameTestHelper helper, EntityType<T> type, BlockPos rel, int variant) {
        ServerLevel level = helper.getLevel();
        T mob = type.create(level);
        if (mob == null) {
            throw new IllegalStateException("could not create " + type);
        }
        BlockPos abs = helper.absolutePos(rel);
        mob.moveTo(abs.getX() + 0.5D, abs.getY(), abs.getZ() + 0.5D, 0.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(abs), MobSpawnType.COMMAND, null, null);
        if (variant >= 0) {
            mob.setVariant(variant);
            mob.chooseSkinForSpecies(mob, false);
            if (mob instanceof INeedsPostUpdate post) {
                post.updateAttributes();
            }
            mob.setHealth(mob.getMaxHealth());
        }
        level.addFreshEntity(mob);
        return mob;
    }
}
