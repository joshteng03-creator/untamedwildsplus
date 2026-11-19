package untamedwilds.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import untamedwilds.UntamedWilds;
import untamedwilds.entity.mammal.EntityBison;
import untamedwilds.init.ModEntity;

/**
 * Headless GameTests: {@code ./gradlew runGameTestServer}. Behaviour tests each get their own batch so a
 * predator in one test can never be seen by prey in a neighbouring one.
 */
@GameTestHolder(UntamedWilds.MOD_ID)
@PrefixGameTestTemplate(false)
public class UWGameTests {

    /** Forge prefixes the holder namespace itself: this resolves to untamedwilds:gametest/platform. */
    public static final String PLATFORM = "gametest/platform";
    public static final String WATERHOLE = "gametest/waterhole";

    /**
     * Test-relative coordinates sit one block BELOW the template's own: the grass layer (template y 1) is helper
     * y 2, so animals stand at helper y 3. Every test assumes this; it is asserted here so a framework change shows
     * up as one clear failure instead of animals spawning inside the ground.
     */
    public static final int FEET_Y = 3;

    /** Proves the harness itself: the floor is where the tests think, a mob spawns through finalizeSpawn and survives. */
    @GameTest(template = PLATFORM, timeoutTicks = 100, batch = "harness")
    public static void harness_spawns_bison(GameTestHelper helper) {
        GTUtil.noon(helper);
        GTUtil.check(helper.getBlockState(new BlockPos(16, FEET_Y - 1, 16)).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK),
                "expected grass at helper y " + (FEET_Y - 1) + ", found " + helper.getBlockState(new BlockPos(16, FEET_Y - 1, 16)));
        GTUtil.check(helper.getBlockState(new BlockPos(16, FEET_Y, 16)).isAir(),
                "expected air at helper y " + FEET_Y + ", found " + helper.getBlockState(new BlockPos(16, FEET_Y, 16)));
        EntityBison bison = GTUtil.spawn(helper, ModEntity.BISON.get(), new BlockPos(16, FEET_Y, 16), 0);
        helper.runAfterDelay(40, () -> {
            GTUtil.check(bison.isAlive(), "bison died within 40 ticks");
            GTUtil.done(helper);
        });
    }
}
