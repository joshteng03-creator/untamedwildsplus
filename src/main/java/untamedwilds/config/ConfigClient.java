package untamedwilds.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-only display settings (2.8.0): untamedwilds-client.toml. Registered as ModConfig.Type.CLIENT, so a server
 * never needs it and each player chooses their own.
 */
public final class ConfigClient {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue CARCASS_FLIES;
    public static final ForgeConfigSpec.IntValue MODEL_DETAIL_LOD_DISTANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Untamed Wilds client display settings");
        CARCASS_FLIES = builder.comment("Small specks drift over fresh carcasses.")
                .define("display.carcass_flies", true);
        MODEL_DETAIL_LOD_DISTANCE = builder.comment("Beyond this many blocks, tiny model details (eyes, teeth, claws, tufts) are not drawn. 0 = always draw everything.")
                .defineInRange("display.model_detail_lod_distance", 32, 0, 256);
        SPEC = builder.build();
    }

    private ConfigClient() {}

    public static boolean carcassFlies() {
        return !SPEC.isLoaded() || CARCASS_FLIES.get();
    }

    public static int modelDetailLodDistance() {
        return SPEC.isLoaded() ? MODEL_DETAIL_LOD_DISTANCE.get() : 0;
    }
}
