package untamedwilds.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigAmbience;
import untamedwilds.entity.mammal.EntityBison;
import untamedwilds.entity.mammal.EntityDeer;
import untamedwilds.entity.mammal.EntityDireWolf;
import untamedwilds.init.ModEntity;

/** P4 vigilance and P5 waterhole visits. One batch per test. */
@GameTestHolder(UntamedWilds.MOD_ID)
@PrefixGameTestTemplate(false)
public class AmbienceTests {

    private static final int Y = UWGameTests.FEET_Y;

    /**
     * A predator just beyond flight range: every deer goes on alert (stops, faces it) and NONE flees. Once the
     * predator is inside flight range, they all flee and the alert clears.
     */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 260, batch = "p4_alert")
    public static void alert_comes_before_flight(GameTestHelper helper) {
        GTUtil.noon(helper);
        EntityDeer[] deer = new EntityDeer[3];
        for (int i = 0; i < deer.length; i++) {
            deer[i] = GTUtil.awake(GTUtil.spawn(helper, ModEntity.DEER.get(), new BlockPos(3, Y, 12 + i * 3), 0));
        }
        // flight range for a lone deer is ~20.6 blocks; alert band reaches 1.5x that. Put the wolf at ~25.
        EntityDireWolf wolf = GTUtil.spawn(helper, ModEntity.DIRE_WOLF.get(), new BlockPos(28, Y, 15), 0);
        wolf.setNoAi(true);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (EntityDeer d : deer) {
                        GTUtil.check(d.isAlert(), "deer " + d.getId() + " never went on alert (dist " + String.format("%.1f", d.distanceTo(wolf)) + ")");
                        GTUtil.check(d.fleeCooldown <= 0, "deer " + d.getId() + " fled from a predator beyond flight range");
                    }
                })
                .thenExecuteAfter(25, () -> {
                    for (EntityDeer d : deer) {
                        double want = Mth.atan2(wolf.getZ() - d.getZ(), wolf.getX() - d.getX()) * (180D / Math.PI) - 90D;
                        double off = Math.abs(Mth.wrapDegrees(want - d.getYRot()));
                        GTUtil.check(off <= 30.0D, "alert deer " + d.getId() + " is not facing the predator (" + Math.round(off) + " deg off)");
                    }
                    // now bring the predator inside flight range
                    Vec3 near = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(10, Y, 15)));
                    wolf.teleportTo(near.x, near.y, near.z);
                })
                .thenWaitUntil(() -> {
                    for (EntityDeer d : deer) {
                        GTUtil.check(d.fleeCooldown > 0, "deer " + d.getId() + " did not flee once the predator came close");
                        GTUtil.check(!d.isAlert(), "deer " + d.getId() + " is still on alert while fleeing");
                    }
                })
                .thenExecute(() -> GTUtil.done(helper));
    }

    /**
     * Habituation (found by the S2 ecology soak: a moose and a tetrameryx starved with predators resting just beyond
     * flight range, because the alert was re-raised on every scan and an alert animal never grazes). A HUNGRY deer
     * watching a predator that does not come closer must relax within ~80 ticks and then actually feed.
     */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 700, batch = "p4_habituation")
    public static void alert_habituates_so_prey_can_feed(GameTestHelper helper) {
        GTUtil.noon(helper);
        EntityDeer deer = GTUtil.awake(GTUtil.spawn(helper, ModEntity.DEER.get(), new BlockPos(3, Y, 15), 0));
        deer.addHunger(15 - deer.getHunger());       // hungry (< 40): habituates after ~80 ticks
        int start = deer.getHunger();
        EntityDireWolf wolf = GTUtil.spawn(helper, ModEntity.DIRE_WOLF.get(), new BlockPos(28, Y, 15), 0);
        wolf.setNoAi(true);
        helper.startSequence()
                .thenWaitUntil(() -> GTUtil.check(deer.isAlert(), "the deer never went on alert: " + GTUtil.describe(deer)))
                .thenWaitUntil(() -> GTUtil.check(!deer.isAlert() && deer.isRelaxedVigilance(),
                        "the deer stayed on alert (streak " + deer.alertStreak + "): " + GTUtil.describe(deer)))
                .thenWaitUntil(() -> GTUtil.check(deer.getHunger() > start, "the relaxed deer never fed (hunger "
                        + deer.getHunger() + "): " + GTUtil.describe(deer)))
                .thenExecute(() -> {
                    GTUtil.check(deer.fleeCooldown <= 0, "the deer fled from a predator that never came inside flight range");
                    GTUtil.done(helper);
                });
    }

    /** A bison due a drink walks to the pond, stops on the bank facing the water, drinks, and reschedules. */
    @GameTest(template = UWGameTests.WATERHOLE, timeoutTicks = 520, batch = "p5_drink")
    public static void drink_visit_reaches_the_bank(GameTestHelper helper) {
        GTUtil.noon(helper);
        EntityBison bison = GTUtil.awake(GTUtil.spawn(helper, ModEntity.BISON.get(), new BlockPos(16, Y, 4), 0));
        bison.addHunger(200);                                  // full: it should not wander off to graze instead
        long start = helper.getLevel().getGameTime();
        bison.nextDrinkTime = start;                           // due now
        BlockPos pondMin = helper.absolutePos(new BlockPos(13, Y - 1, 13));
        BlockPos pondMax = helper.absolutePos(new BlockPos(18, Y - 1, 18));
        {
            net.minecraft.server.level.ServerLevel level = helper.getLevel();
            BlockPos centre = helper.absolutePos(new BlockPos(15, Y - 1, 15));
            BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, centre);
            StringBuilder col = new StringBuilder();
            for (int dy = -3; dy <= 3; dy++) {
                BlockPos p = centre.above(dy);
                col.append(dy).append(':').append(level.getBlockState(p).getBlock().getName().getString())
                        .append(level.getFluidState(p).isEmpty() ? "" : "(fluid " + level.getFluidState(p).isSource() + ")").append(' ');
            }
            untamedwilds.util.WaterFinder.Spot spot = untamedwilds.util.WaterFinder.findBank(level, bison.blockPosition(), 24, 3, new java.util.Random(1));
            UntamedWilds.LOGGER.info("[gametest] pond centre {} heightmap top {} column [{}] findBank -> {}", centre, top, col.toString().trim(),
                    spot == null ? "null" : spot.water + " bank " + spot.bank);
        }
        for (int t = 20; t <= 400; t += 60) {
            helper.runAfterDelay(t, () -> UntamedWilds.LOGGER.info("[gametest] drink bison: {}", GTUtil.describe(bison)));
        }
        helper.startSequence()
                .thenWaitUntil(() -> GTUtil.check(bison.drinkVisits >= 1, "the bison never drank: " + GTUtil.describe(bison)))
                .thenExecute(() -> {
                    double cx = Mth.clamp(bison.getX(), pondMin.getX(), pondMax.getX() + 1);
                    double cz = Mth.clamp(bison.getZ(), pondMin.getZ(), pondMax.getZ() + 1);
                    double dist = Math.sqrt((bison.getX() - cx) * (bison.getX() - cx) + (bison.getZ() - cz) * (bison.getZ() - cz));
                    // its muzzle reaches getDrinkReach() blocks in front of its centre (solved DRINK pose x size)
                    double reach = bison.getDrinkReach();
                    GTUtil.check(dist <= reach + 0.9D, "drinking " + String.format("%.2f", dist) + " blocks from the water with a "
                            + String.format("%.2f", reach) + "-block reach");
                    GTUtil.check(!bison.isInWater(), "the bison walked into the pond to drink");
                })
                .thenWaitUntil(() -> GTUtil.check(bison.nextDrinkTime - helper.getLevel().getGameTime()
                        >= (long) (0.7D * ConfigAmbience.drinkIntervalTicks.get()), "next visit not rescheduled a day out"))
                .thenExecute(() -> GTUtil.done(helper));
    }

    /**
     * The widest animal in the mod (2.6-block hitbox) drinks with its TRUNK: it has to stop ~3.6 blocks back from the
     * water -- its solved trunk reach -- which is exactly where a wide animal's path used to end short and freeze.
     */
    @GameTest(template = UWGameTests.WATERHOLE, timeoutTicks = 560, batch = "p5_drink_mammoth")
    public static void mammoth_drinks_with_its_trunk(GameTestHelper helper) {
        GTUtil.noon(helper);
        untamedwilds.entity.mammal.EntityMammoth mammoth = GTUtil.awake(GTUtil.spawn(helper, ModEntity.MAMMOTH.get(), new BlockPos(16, Y, 3), 0));
        mammoth.addHunger(200);
        mammoth.nextDrinkTime = helper.getLevel().getGameTime();
        BlockPos pondMin = helper.absolutePos(new BlockPos(13, Y - 1, 13));
        BlockPos pondMax = helper.absolutePos(new BlockPos(18, Y - 1, 18));
        helper.startSequence()
                .thenWaitUntil(() -> GTUtil.check(mammoth.drinkVisits >= 1, "the mammoth never drank: " + GTUtil.describe(mammoth)))
                .thenExecute(() -> {
                    double cx = Mth.clamp(mammoth.getX(), pondMin.getX(), pondMax.getX() + 1);
                    double cz = Mth.clamp(mammoth.getZ(), pondMin.getZ(), pondMax.getZ() + 1);
                    double dist = Math.sqrt((mammoth.getX() - cx) * (mammoth.getX() - cx) + (mammoth.getZ() - cz) * (mammoth.getZ() - cz));
                    double reach = mammoth.getDrinkReach();
                    UntamedWilds.LOGGER.info("[gametest] mammoth drinks {} blocks from the water, trunk reach {}", String.format("%.2f", dist), String.format("%.2f", reach));
                    // the trunk tip must be OVER the water: stood no further back than its reach (+ a little slack)
                    GTUtil.check(dist <= reach + 0.15D, "drinking " + String.format("%.2f", dist) + " blocks from the water with a "
                            + String.format("%.2f", reach) + "-block trunk reach: the trunk tip is on the bank");
                    GTUtil.check(dist >= reach - 1.5D, "walked " + String.format("%.2f", reach - dist) + " blocks closer than its trunk needs");
                    GTUtil.check(!mammoth.isInWater(), "the mammoth walked into the pond to drink");
                    GTUtil.check(mammoth.getAnimation() == untamedwilds.entity.mammal.EntityMammoth.DRINK, "not playing DRINK while drinking");
                })
                .thenExecute(() -> GTUtil.done(helper));
    }

    /** With no water in reach the goal never starts, and the next attempt is scheduled for later. */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 120, batch = "p5_no_water")
    public static void drink_without_water_just_retries(GameTestHelper helper) {
        GTUtil.noon(helper);
        EntityBison bison = GTUtil.awake(GTUtil.spawn(helper, ModEntity.BISON.get(), new BlockPos(16, Y, 16), 0));
        bison.addHunger(200);                                  // full: grazing would hold MOVE at the same priority
        // keep the search on the dry platform (the test world around the template is not ours)
        int radius = ConfigAmbience.drinkSearchRadius.get();
        ConfigAmbience.drinkSearchRadius.set(8);
        long start = helper.getLevel().getGameTime();
        bison.nextDrinkTime = start;
        helper.runAfterDelay(60, () -> {
            ConfigAmbience.drinkSearchRadius.set(radius);
            GTUtil.check(bison.drinkVisits == 0, "drank with no water anywhere");
            GTUtil.check(bison.nextDrinkTime >= start + 2000, "no retry scheduled: " + GTUtil.describe(bison));
            GTUtil.done(helper);
        });
    }
}
