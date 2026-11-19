package untamedwilds.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigAmbience;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.config.EcologyMode;
import untamedwilds.entity.EntityCarcass;
import untamedwilds.entity.mammal.EntityDeer;
import untamedwilds.entity.mammal.EntityDireWolf;
import untamedwilds.entity.mammal.EntityHyena;
import untamedwilds.init.ModEntity;

import java.util.List;

/** P7 persistent carcasses. One batch per test; each sets the ecosystem gamerule it needs. */
@GameTestHolder(UntamedWilds.MOD_ID)
@PrefixGameTestTemplate(false)
public class CarcassTests {

    private static final int Y = UWGameTests.FEET_Y;
    private static final BlockPos MID = new BlockPos(16, Y, 16);

    private static List<EntityCarcass> carcasses(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(EntityCarcass.class, area(helper));
    }

    private static List<ItemEntity> items(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area(helper));
    }

    private static AABB area(GameTestHelper helper) {
        BlockPos a = helper.absolutePos(new BlockPos(0, 0, 0));
        return new AABB(a, a.offset(32, 8, 32));
    }

    private static EntityDeer deer(GameTestHelper helper) {
        EntityDeer d = GTUtil.awake(GTUtil.spawn(helper, ModEntity.DEER.get(), MID, 0));
        d.setNoAi(true);
        return d;
    }

    /** A natural death leaves a carcass that holds the loot (nothing drops) and the carrion pool. */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 100, batch = "p7_natural")
    public static void carcass_natural_death_keeps_the_loot(GameTestHelper helper) {
        EcologyMode.setEcosystem(helper.getLevel(), true);
        EntityDeer d = deer(helper);
        float max = d.getMaxHealth();
        helper.runAfterDelay(5, () -> {
            d.hurt(DamageSource.STARVE, 10000F);
        });
        helper.runAfterDelay(15, () -> {
            List<EntityCarcass> c = carcasses(helper);
            GTUtil.check(c.size() == 1, "expected one carcass, found " + c.size());
            GTUtil.check(items(helper).isEmpty(), "loot dropped although the carcass should hold it");
            float want = max * ConfigGamerules.carrionFoodPerHealth.get();
            GTUtil.check(Math.abs(c.get(0).getFood() - want) < 0.5F, "carcass food " + c.get(0).getFood() + ", expected " + want);
            GTUtil.done(helper);
        });
    }

    /** A player's kill is unchanged: loot drops, no carcass. */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 100, batch = "p7_player")
    public static void player_kill_drops_loot_as_before(GameTestHelper helper) {
        EcologyMode.setEcosystem(helper.getLevel(), true);
        EntityDeer d = deer(helper);
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        helper.runAfterDelay(5, () -> d.hurt(DamageSource.playerAttack(player), 10000F));
        helper.runAfterDelay(15, () -> {
            GTUtil.check(carcasses(helper).isEmpty(), "a player's kill left a carcass");
            GTUtil.check(!items(helper).isEmpty(), "a player's kill dropped nothing");
            GTUtil.done(helper);
        });
    }

    /** /kill leaves nothing behind (and Zoo mode keeps the old drops). */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 100, batch = "p7_zoo")
    public static void zoo_mode_and_kill_command_leave_no_carcass(GameTestHelper helper) {
        EcologyMode.setEcosystem(helper.getLevel(), false);
        EntityDeer zoo = deer(helper);
        EntityDeer killed = GTUtil.awake(GTUtil.spawn(helper, ModEntity.DEER.get(), MID.east(6), 0));
        killed.setNoAi(true);
        helper.runAfterDelay(5, () -> zoo.hurt(DamageSource.STARVE, 10000F));
        helper.runAfterDelay(8, () -> {
            EcologyMode.setEcosystem(helper.getLevel(), true);
            killed.kill();
        });
        helper.runAfterDelay(20, () -> {
            GTUtil.check(carcasses(helper).isEmpty(), "a zoo death or /kill left a carcass");
            GTUtil.check(!items(helper).isEmpty(), "the zoo death dropped no loot");
            GTUtil.done(helper);
        });
    }

    /** A predator's kill: the killer eats its share and the rest stays in the carcass. */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 200, batch = "p7_predator")
    public static void predator_kill_leaves_the_remainder(GameTestHelper helper) {
        EcologyMode.setEcosystem(helper.getLevel(), true);
        EntityDeer d = deer(helper);
        EntityDireWolf wolf = GTUtil.spawn(helper, ModEntity.DIRE_WOLF.get(), MID.west(2), 0);
        wolf.setNoAi(true);
        wolf.addHunger(10 - wolf.getHunger());
        float pool = d.getMaxHealth() * ConfigGamerules.carcassFoodPerHealth.get();
        helper.runAfterDelay(5, () -> {
            d.setHealth(1.0F);
            wolf.commitToHunt(d);
            wolf.doHurtTarget(d);
        });
        helper.runAfterDelay(15, () -> {
            GTUtil.check(!d.isAlive(), "the wolf did not kill the deer");
            List<EntityCarcass> c = carcasses(helper);
            GTUtil.check(c.size() == 1, "expected one carcass, found " + c.size());
            GTUtil.check(wolf.getHunger() > 10, "the killer did not eat");
            float left = c.get(0).getFood();
            GTUtil.check(left < pool && left > 0F, "carcass food " + left + " of pool " + pool + " (killer share not taken, or nothing left)");
            GTUtil.check(!c.get(0).canFeed(wolf), "the killer may feed again from its own kill");
            GTUtil.done(helper);
        });
    }

    /** A hungry hyena walks over and feeds; it takes at most its per-scavenger cap. */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 600, batch = "p7_scavenger")
    public static void scavenger_walks_over_and_feeds(GameTestHelper helper) {
        EcologyMode.setEcosystem(helper.getLevel(), true);
        GTUtil.noon(helper);
        EntityDeer d = deer(helper);
        EntityHyena hyena = GTUtil.awake(GTUtil.spawn(helper, ModEntity.HYENA.get(), new BlockPos(4, Y, 4), 0));
        hyena.addHunger(40 - hyena.getHunger());
        float[] start = new float[1];
        helper.runAfterDelay(5, () -> d.hurt(DamageSource.STARVE, 10000F));
        helper.runAfterDelay(10, () -> {
            List<EntityCarcass> c = carcasses(helper);
            GTUtil.check(c.size() == 1, "no carcass to scavenge");
            start[0] = c.get(0).getFood();
        });
        helper.startSequence().thenIdle(12).thenWaitUntil(() -> {
            List<EntityCarcass> c = carcasses(helper);
            GTUtil.check(!c.isEmpty(), "carcass vanished");
            GTUtil.check(c.get(0).getFood() < start[0], "the hyena has not fed yet (hunger " + hyena.getHunger() + ")");
        }).thenExecute(() -> {
            EntityCarcass c = carcasses(helper).get(0);
            GTUtil.check(start[0] - c.getFood() <= Math.max(1, ConfigGamerules.carrionHunger.get()) + 0.5F, "a scavenger took more than its cap");
            GTUtil.check(c.feederCount() <= ConfigAmbience.carcassMaxFeeders.get(), "too many feeders");
            GTUtil.done(helper);
        });
    }

    /** Butchered once with an axe: loot drops, the axe takes 1 damage, then it is bones and does nothing more. */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 100, batch = "p7_butcher")
    public static void butcher_once_with_an_axe(GameTestHelper helper) {
        EcologyMode.setEcosystem(helper.getLevel(), true);
        EntityDeer d = deer(helper);
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        helper.runAfterDelay(5, () -> d.hurt(DamageSource.STARVE, 10000F));
        helper.runAfterDelay(12, () -> {
            List<EntityCarcass> c = carcasses(helper);
            GTUtil.check(c.size() == 1, "no carcass");
            EntityCarcass carcass = c.get(0);
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            player.setItemInHand(InteractionHand.MAIN_HAND, axe);
            GTUtil.check(carcass.interact(player, InteractionHand.MAIN_HAND).consumesAction(), "butchering refused");
            GTUtil.check(carcass.isButchered() && carcass.getStage() == EntityCarcass.STAGE_BONES, "not bones after butchering");
            GTUtil.check(axe.getDamageValue() == 1, "axe damage " + axe.getDamageValue() + ", expected 1");
            int drops = items(helper).size();
            GTUtil.check(drops >= 1, "butchering dropped nothing");
            GTUtil.check(!carcass.interact(player, InteractionHand.MAIN_HAND).consumesAction(), "a second butchering was allowed");
            GTUtil.check(items(helper).size() == drops, "the second use dropped more loot");
            GTUtil.done(helper);
        });
    }

    /** Decay: past its lifetime a carcass is bones, and it keeps its NBT through a save/load. */
    @GameTest(template = UWGameTests.PLATFORM, timeoutTicks = 100, batch = "p7_decay")
    public static void decay_to_bones_and_nbt_round_trip(GameTestHelper helper) {
        EcologyMode.setEcosystem(helper.getLevel(), true);
        EntityDeer d = deer(helper);
        helper.runAfterDelay(5, () -> d.hurt(DamageSource.STARVE, 10000F));
        helper.runAfterDelay(12, () -> {
            EntityCarcass carcass = carcasses(helper).get(0);
            net.minecraft.nbt.CompoundTag tag = carcass.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            EntityCarcass copy = new EntityCarcass(ModEntity.CARCASS.get(), helper.getLevel());
            copy.load(tag);
            GTUtil.check(Math.abs(copy.getFood() - carcass.getFood()) < 0.01F && copy.getAge() == carcass.getAge(), "NBT round trip lost state");
            carcass.setAgeForTests(ConfigAmbience.carcassLifetimeTicks.get());
            GTUtil.check(carcass.getStage() == EntityCarcass.STAGE_BONES, "still flesh past its lifetime (stage " + carcass.getStage() + ")");
            GTUtil.done(helper);
        });
    }
}
