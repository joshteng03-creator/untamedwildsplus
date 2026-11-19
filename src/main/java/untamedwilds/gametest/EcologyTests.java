package untamedwilds.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigPerformance;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.HerdEntity;
import untamedwilds.entity.ai.SmartAvoidGoal;
import untamedwilds.entity.mammal.EntityDeer;
import untamedwilds.entity.mammal.EntityDireWolf;
import untamedwilds.init.ModEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** P2: behaviour-changing ecology fixes (parity, shared herd scan). */
@GameTestHolder(UntamedWilds.MOD_ID)
@PrefixGameTestTemplate(false)
public class EcologyTests {

    private static final BlockPos MID = new BlockPos(16, 3, 16);

    /**
     * The parity bug: a `tickCount % 10` gate inside a goal never fires for mobs whose goal-evaluation ticks
     * fall on odd tickCounts -- about half of them. Two deer spawned back to back have consecutive entity ids,
     * hence opposite evaluation parities; BOTH must now run their own predator scan ~every 10 ticks.
     */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 200, batch = "p2_parity")
    public static void parity_every_herbivore_scans_for_predators(GameTestHelper helper) {
        GTUtil.noon(helper);
        // Deer are crepuscular: at noon they would fall asleep near home and stop scanning, which is not the bug.
        EntityDeer a = GTUtil.awake(GTUtil.spawn(helper, ModEntity.DEER.get(), MID.west(4), 0));
        EntityDeer b = GTUtil.awake(GTUtil.spawn(helper, ModEntity.DEER.get(), MID.east(4), 0));
        GTUtil.check(Math.abs(a.getId() - b.getId()) == 1, "expected consecutive entity ids, got " + a.getId() + " / " + b.getId());
        int a0 = a.perfFleeScans, b0 = b.perfFleeScans;
        helper.runAfterDelay(120, () -> {
            int sa = a.perfFleeScans - a0, sb = b.perfFleeScans - b0;
            UntamedWilds.LOGGER.info("[gametest] flee scans in 120 ticks: deer {} = {}, deer {} = {}", a.getId(), sa, b.getId(), sb);
            GTUtil.check(sa >= 6, "deer " + a.getId() + " scanned only " + sa + " times in 120 ticks");
            GTUtil.check(sb >= 6, "deer " + b.getId() + " scanned only " + sb + " times in 120 ticks");
            GTUtil.done(helper);
        });
    }

    /**
     * The shared herd scan must find exactly what each member's own scan finds: same box, same selector, same
     * line of sight -- only the entity search is shared.
     */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 100, batch = "p2_shared_scan")
    public static void shared_herd_scan_matches_per_member_scans(GameTestHelper helper) {
        GTUtil.noon(helper);
        EntityDeer[] herd = new EntityDeer[6];
        for (int i = 0; i < herd.length; i++) {
            herd[i] = GTUtil.awake(GTUtil.spawn(helper, ModEntity.DEER.get(), new BlockPos(6 + i * 2, 3, 8 + (i % 2) * 3), 0));
            herd[i].setNoAi(true);
        }
        EntityDireWolf wolf = GTUtil.spawn(helper, ModEntity.DIRE_WOLF.get(), new BlockPos(20, 3, 22), 0);
        wolf.setNoAi(true);
        helper.runAfterDelay(5, () -> {
            for (int i = 1; i < herd.length; i++) {
                if (herd[0].herd != herd[i].herd && herd[0].herd != null && herd[i].herd != null) {
                    HerdEntity.combineHerds(herd[0].herd, herd[i].herd);
                }
            }
            GTUtil.check(herd[0].herd != null && herd[0].herd.creatureList.size() == herd.length,
                    "could not form a herd of " + herd.length);
            boolean was = ConfigPerformance.sharedHerdThreatScan.get();
            ConfigPerformance.sharedHerdThreatScan.set(true);
            try {
                int spotted = 0;
                for (EntityDeer deer : herd) {
                    SmartAvoidGoal<?> goal = findAvoidGoal(deer);
                    GTUtil.check(goal != null, "deer has no SmartAvoidGoal");
                    double detect = goal.currentDetectDistance();
                    Set<LivingEntity> own = new HashSet<>(goal.findThreatsUnshared(detect));
                    Set<LivingEntity> shared = new HashSet<>((List<? extends LivingEntity>) goal.findThreats(detect));
                    GTUtil.check(own.equals(shared), "deer " + deer.getId() + ": shared scan found " + shared.size()
                            + " threats, its own scan " + own.size());
                    if (!own.isEmpty()) {
                        spotted++;
                    }
                }
                UntamedWilds.LOGGER.info("[gametest] shared-scan equivalence: {} of {} deer see the wolf", spotted, herd.length);
                if (spotted == 0) {
                    for (EntityDeer deer : herd) {
                        SmartAvoidGoal<?> goal = findAvoidGoal(deer);
                        double detect = goal.currentDetectDistance();
                        int raw = helper.getLevel().getEntitiesOfClass(LivingEntity.class, deer.getBoundingBox().inflate(detect, 4, detect)).size();
                        net.minecraft.world.phys.BlockHitResult hit = helper.getLevel().clip(new net.minecraft.world.level.ClipContext(
                                deer.getEyePosition(), wolf.getEyePosition(), net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                net.minecraft.world.level.ClipContext.Fluid.NONE, deer));
                        UntamedWilds.LOGGER.info("[gametest]   deer {} eye {} -> wolf eye {}: sight line hits {} at {} ({})", deer.getId(),
                                deer.getEyePosition(), wolf.getEyePosition(), hit.getType(), hit.getBlockPos(),
                                helper.getLevel().getBlockState(hit.getBlockPos()));
                        UntamedWilds.LOGGER.info("[gametest]   deer {}: detect {} dist {} wolfPredator {} rawInBox {} los {} wolfAlive {} wolfPos {}",
                                deer.getId(), String.format("%.1f", detect), String.format("%.1f", deer.distanceTo(wolf)),
                                untamedwilds.util.EcologyTags.isPredator(wolf), raw, deer.hasLineOfSight(wolf), wolf.isAlive(), wolf.blockPosition());
                    }
                }
                GTUtil.check(spotted > 0, "test layout wrong: no deer can see the wolf");
            } finally {
                ConfigPerformance.sharedHerdThreatScan.set(was);
            }
            GTUtil.done(helper);
        });
    }

    private static SmartAvoidGoal<?> findAvoidGoal(ComplexMob mob) {
        for (WrappedGoal wrapped : mob.goalSelectorForTests().getAvailableGoals()) {
            if (wrapped.getGoal() instanceof SmartAvoidGoal<?> avoid) {
                return avoid;
            }
        }
        return null;
    }
}
