package untamedwilds.gametest;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import untamedwilds.UntamedWilds;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.arthropod.EntityTarantula;
import untamedwilds.entity.mammal.EntityBison;
import untamedwilds.entity.mammal.EntityDireWolf;
import untamedwilds.init.ModBlock;
import untamedwilds.init.ModEntity;
import untamedwilds.util.PerfCounters;

/** P1: crash fixes and behaviour-preserving performance work. One batch per test (no cross-talk). */
@GameTestHolder(UntamedWilds.MOD_ID)
@PrefixGameTestTemplate(false)
public class PerfTests {

    private static final BlockPos MID = new BlockPos(16, 3, 16);

    /**
     * An animal whose home is far away (every migrated herd) used to look its home block entity up every tick
     * with no player near -- and getBlockEntity() on an unloaded chunk loads it. The home chunk must stay
     * unloaded for the whole run. Checked every tick: a chunk loaded briefly unloads again soon after.
     */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 260, batch = "p1_despawn")
    public static void despawn_never_loads_far_home_chunk(GameTestHelper helper) {
        GTUtil.noon(helper);
        ServerLevel level = helper.getLevel();
        EntityBison bison = GTUtil.spawn(helper, ModEntity.BISON.get(), MID, 0);
        BlockPos far = helper.absolutePos(MID).offset(2048, 0, 2048);
        int cx = far.getX() >> 4, cz = far.getZ() >> 4;
        GTUtil.check(!level.getChunkSource().hasChunk(cx, cz), "the far chunk was already loaded before the test");
        bison.setHome(far);
        helper.startSequence()
                .thenExecuteFor(200, () -> GTUtil.check(!level.getChunkSource().hasChunk(cx, cz),
                        "the despawn check loaded the far-away home chunk"))
                .thenExecute(() -> GTUtil.check(bison.isAlive(), "bison died"))
                .thenExecute(() -> GTUtil.done(helper));
    }

    /** Control for the throttled despawn check: a critter with a real burrow still goes home. */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 200, batch = "p1_burrow")
    public static void burrow_critter_still_enters_its_burrow(GameTestHelper helper) {
        GTUtil.noon(helper);
        BlockPos burrowRel = MID;
        helper.setBlock(burrowRel, ModBlock.BURROW.get());
        EntityTarantula spider = GTUtil.spawn(helper, ModEntity.TARANTULA.get(), MID.east(2), 0);
        spider.setHome(helper.absolutePos(burrowRel));
        helper.succeedWhen(() -> GTUtil.check(spider.isRemoved(), "tarantula has not entered its burrow"));
    }

    /**
     * CLAUDE.md's top in-game check, automated: a predator's eco level right after a hunting blow must equal its
     * value at rest. The transient attack bonus must not survive the blow, and the new per-tick eco memo must
     * never hand out a value computed while the bonus was applied.
     */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 100, batch = "p1_eco")
    public static void eco_level_unchanged_after_hunting_blow(GameTestHelper helper) {
        GTUtil.noon(helper);
        EntityDireWolf wolf = GTUtil.spawn(helper, ModEntity.DIRE_WOLF.get(), MID.west(2), 0);
        EntityBison prey = GTUtil.spawn(helper, ModEntity.BISON.get(), MID.east(1), 0);
        wolf.setNoAi(true);
        prey.setNoAi(true);
        helper.runAfterDelay(5, () -> {
            wolf.invalidateEcoCache();
            int atRest = ComplexMob.getEcoLevel(wolf);
            int cachedSameTick = ComplexMob.getEcoLevel(wolf);
            GTUtil.check(atRest == cachedSameTick, "eco memo returned " + cachedSameTick + " vs computed " + atRest);
            wolf.commitToHunt(prey);
            wolf.doHurtTarget(prey);
            GTUtil.check(!wolf.hasAttackBonus(), "the hunting attack bonus survived the blow");
            int memoAfter = ComplexMob.getEcoLevel(wolf);
            wolf.invalidateEcoCache();
            int freshAfter = ComplexMob.getEcoLevel(wolf);
            GTUtil.check(freshAfter == atRest, "eco level moved across a hunting blow: " + atRest + " -> " + freshAfter);
            GTUtil.check(memoAfter == atRest, "eco memo served a stale/boosted value after the blow: " + memoAfter);
            GTUtil.done(helper);
        });
    }

    /** A hungry predator on empty ground must back its searches off instead of scanning every ~10 ticks. */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 700, batch = "p1_hunt")
    public static void hunt_search_backs_off_without_prey(GameTestHelper helper) {
        GTUtil.noon(helper);
        EntityDireWolf wolf = GTUtil.spawn(helper, ModEntity.DIRE_WOLF.get(), MID, 0);
        wolf.addHunger(20 - wolf.getHunger());          // hungry (threshold 30), not starving
        int start = wolf.perfHuntScans;                 // per-animal: worldgen fauna share the global counter
        helper.runAfterDelay(600, () -> {
            long scans = wolf.perfHuntScans - start;
            UntamedWilds.LOGGER.info("[gametest] hunt scans in 600 ticks without prey: {}", scans);
            GTUtil.check(scans >= 1, "the hungry wolf never searched at all");
            GTUtil.check(scans <= 12, "the hungry wolf searched " + scans + " times in 600 ticks with no prey around");
            GTUtil.done(helper);
        });
    }

    /**
     * The Citadel start event is cancelled for our mobs and replaced by ONE tracking-only packet -- and the
     * animation must still run to completion on the server (Citadel advances the tick regardless).
     */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 100, batch = "p1_anim")
    public static void animation_start_is_tracking_only_and_completes(GameTestHelper helper) {
        GTUtil.noon(helper);
        EntityBison bison = GTUtil.spawn(helper, ModEntity.BISON.get(), MID, 0);
        bison.setNoAi(true);
        int[] before = new int[1];
        helper.runAfterDelay(5, () -> {
            before[0] = bison.perfAnimStarts;               // per-animal: worldgen fauna share the global counter
            bison.setAnimation(EntityBison.ATTACK_GORE);    // 14 ticks
        });
        helper.runAfterDelay(9, () -> GTUtil.check(bison.getAnimation() == EntityBison.ATTACK_GORE && bison.getAnimationTick() > 0,
                "GORE is not advancing (tick " + bison.getAnimationTick() + ")"));
        helper.runAfterDelay(40, () -> {
            GTUtil.check(bison.getAnimation() == IAnimatedEntity.NO_ANIMATION, "GORE never finished");
            long sent = bison.perfAnimStarts - before[0];
            GTUtil.check(sent == 1, "expected exactly one tracking-only start packet, got " + sent);
            GTUtil.done(helper);
        });
    }
}
