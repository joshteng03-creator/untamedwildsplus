package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import untamedwilds.entity.EntityCarcass;
import untamedwilds.item.MountedSpecimenItem;

/**
 * Draws a carcass (2.8.0): the animal's own model, un-ticked, rolled onto its side and darkening as it decays; once
 * only bones are left, a few vanilla bone items lying flat where it fell. No new art -- it reuses the species' model
 * and skin, exactly as a mounted specimen does. Deliberately does NOT use the living death pose (deathTime): vanilla
 * tints a dying model red.
 */
public class RendererCarcass extends EntityRenderer<EntityCarcass> {

    // <gen:carcass-poses> 2.8.0 scripts/gen_carcass_pose.py -- per type: {lift, shift} in model units for the
    // on-its-side roll, or SLEEP (lies in its solved sleep pose) when antlers / tusks would sink into the ground.
    private static final java.util.Map<String, float[]> SIDE = new java.util.HashMap<>();
    private static final java.util.Set<String> SLEEP = new java.util.HashSet<>();
    static {
        SIDE.put("aardvark", new float[]{4.55F, 5.39F});
        SIDE.put("antelope", new float[]{5.77F, 13.22F});
        SIDE.put("arowana", new float[]{3.43F, 2.75F});
        SIDE.put("baleen_whale", new float[]{30.36F, 7.79F});
        SIDE.put("bear", new float[]{9.48F, 10.65F});
        SIDE.put("big_cat", new float[]{5.75F, 8.94F});
        SIDE.put("bison", new float[]{7.0F, 11.26F});
        SIDE.put("boar", new float[]{5.23F, 7.72F});
        SIDE.put("camel", new float[]{6.8F, 18.88F});
        SIDE.put("caprine", new float[]{4.5F, 8.2F});
        SIDE.put("catfish", new float[]{7.91F, 2.44F});
        SIDE.put("crocodilian", new float[]{10.93F, 4.94F});
        SLEEP.add("deer");
        SIDE.put("diprotodont", new float[]{10.14F, 12.1F});
        SIDE.put("dire_wolf", new float[]{4.45F, 8.63F});
        SIDE.put("equid", new float[]{9.19F, 14.33F});
        SIDE.put("football_fish", new float[]{3.5F, 4.85F});
        SIDE.put("giant_clam", new float[]{9.01F, 4.74F});
        SIDE.put("giant_monitor", new float[]{11.4F, 5.21F});
        SIDE.put("giant_salamander", new float[]{6.32F, 1.66F});
        SIDE.put("giraffid", new float[]{8.2F, 19.68F});
        SIDE.put("glyptodont", new float[]{14.2F, 14.44F});
        SIDE.put("great_ape", new float[]{7.9F, 16.65F});
        SIDE.put("ground_bird", new float[]{5.6F, 13.99F});
        SIDE.put("ground_sloth", new float[]{13.22F, 16.02F});
        SIDE.put("hippo", new float[]{6.0F, 8.75F});
        SIDE.put("hyena", new float[]{4.55F, 8.62F});
        SIDE.put("kangaroo", new float[]{4.6F, 11.26F});
        SIDE.put("king_crab", new float[]{16.43F, 4.24F});
        SIDE.put("large_rodent", new float[]{4.5F, 6.9F});
        SIDE.put("macrauchenia", new float[]{8.6F, 15.05F});
        SLEEP.add("mammoth");
        SIDE.put("manatee", new float[]{12.18F, 9.35F});
        SIDE.put("monitor", new float[]{5.53F, 2.9F});
        SIDE.put("mustelid", new float[]{3.45F, 4.5F});
        SIDE.put("newt", new float[]{3.9F, 1.13F});
        SIDE.put("opossum", new float[]{4.4F, 5.39F});
        SIDE.put("orca", new float[]{13.95F, 12.19F});
        SIDE.put("pinniped", new float[]{7.68F, 4.5F});
        SIDE.put("rhino", new float[]{8.0F, 12.9F});
        SIDE.put("shark", new float[]{12.22F, 9.33F});
        SIDE.put("snake", new float[]{1.0F, 0.99F});
        SIDE.put("spadefish", new float[]{3.28F, 3.25F});
        SIDE.put("sunfish", new float[]{5.6F, 13.0F});
        SIDE.put("tapir", new float[]{9.0F, 10.59F});
        SIDE.put("tarantula", new float[]{9.78F, 2.68F});
        SIDE.put("tortoise", new float[]{5.06F, 4.84F});
        SIDE.put("toxodon", new float[]{9.6F, 13.8F});
        SIDE.put("trevally", new float[]{3.33F, 4.36F});
        SIDE.put("triggerfish", new float[]{2.79F, 4.09F});
        SIDE.put("whale_shark", new float[]{20.88F, 6.71F});
        SIDE.put("wildebeest", new float[]{4.7F, 9.77F});
    }
    // </gen:carcass-poses>

    public RendererCarcass(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(EntityCarcass carcass, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light) {
        // darken with decay: full light when fresh, ~55% when only just short of bones
        float dim = 1.0F - 0.45F * (carcass.getDecay() / 255.0F);
        int block = (int) (LightTexture.block(light) * dim);
        int sky = (int) (LightTexture.sky(light) * dim);
        int dimmed = LightTexture.pack(block, sky);
        pose.pushPose();
        pose.mulPose(Vector3f.YP.rotationDegrees(180.0F - carcass.getFacingYaw()));
        if (carcass.getStage() >= EntityCarcass.STAGE_BONES) {
            this.renderBones(carcass, pose, buffer, dimmed);
        } else {
            Entity copy = carcass.getDisplayCopy();
            if (copy != null) {
                String type = net.minecraft.world.entity.EntityType.getKey(copy.getType()).getPath();
                float size = copy instanceof untamedwilds.entity.ComplexMob mob ? Math.max(0.1F, mob.getMobSize()) : 1.0F;
                if (SLEEP.contains(type) && copy instanceof untamedwilds.entity.ComplexMobTerrestrial land) {
                    // antlers / tusks would go through the ground on its side: it lies in its solved sleep pose
                    if (!land.isSleeping()) {
                        land.setSleeping(true);
                    }
                    land.sleepProgress = 40;
                } else {
                    // roll 90 degrees onto its side about its long axis, then the SOLVED lift (torso and legs resting on
                    // the ground) and shift (body centred); model units x the animal's size, /16 for blocks
                    float[] p = SIDE.get(type);
                    if (p != null) {
                        pose.translate(p[1] * size / 16.0F, p[0] * size / 16.0F, 0.0D);
                    } else {
                        float w = carcass.getSpecimen().getFloat(MountedSpecimenItem.WIDTH_KEY);
                        float h = carcass.getSpecimen().getFloat(MountedSpecimenItem.HEIGHT_KEY);
                        pose.translate(h * 0.5F, w * 0.42F, 0.0D);
                    }
                    pose.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
                }
                this.entityRenderDispatcher.render(copy, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, pose, buffer, dimmed);
            }
        }
        pose.popPose();
        super.render(carcass, yaw, partialTicks, pose, buffer, light);
    }

    private void renderBones(EntityCarcass carcass, PoseStack pose, MultiBufferSource buffer, int light) {
        ItemStack bone = new ItemStack(Items.BONE);
        float w = Math.max(0.6F, carcass.getSpecimen().getFloat(MountedSpecimenItem.WIDTH_KEY));
        int n = 3 + (carcass.getId() & 1) + (w > 1.6F ? 1 : 0);
        long seed = carcass.getUUID().getLeastSignificantBits();
        java.util.Random rand = new java.util.Random(seed);
        for (int i = 0; i < n; i++) {
            pose.pushPose();
            pose.translate((rand.nextFloat() - 0.5F) * w, 0.03D + i * 0.004D, (rand.nextFloat() - 0.5F) * w * 1.4F);
            pose.mulPose(Vector3f.YP.rotationDegrees(rand.nextFloat() * 360.0F));
            pose.mulPose(Vector3f.XP.rotationDegrees(90.0F));
            pose.scale(0.9F, 0.9F, 0.9F);
            Minecraft.getInstance().getItemRenderer().renderStatic(bone, ItemTransforms.TransformType.GROUND, light,
                    OverlayTexture.NO_OVERLAY, pose, buffer, (int) seed + i);
            pose.popPose();
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityCarcass carcass) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
