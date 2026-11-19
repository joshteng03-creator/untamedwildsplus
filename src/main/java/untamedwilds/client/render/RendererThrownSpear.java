package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import untamedwilds.entity.ProjectileThrownSpear;

/**
 * Draws a thrown spear as its own item sprite laid along the flight axis, twice (two crossed planes,
 * like an arrow) so it reads from every side. The arrow renderer's rotations put local +X on the
 * flight direction; the spear icon points up-right, so a -45 degree roll lines the tip up with it.
 */
public class RendererThrownSpear extends EntityRenderer<ProjectileThrownSpear> {

    private static final float SCALE = 1.2F;
    private final ItemRenderer itemRenderer;

    public RendererThrownSpear(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ProjectileThrownSpear spear, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.mulPose(Vector3f.YP.rotationDegrees(Mth.lerp(partialTicks, spear.yRotO, spear.getYRot()) - 90.0F));
        pose.mulPose(Vector3f.ZP.rotationDegrees(Mth.lerp(partialTicks, spear.xRotO, spear.getXRot())));
        float shake = (float) spear.shakeTime - partialTicks;
        if (shake > 0.0F) {
            pose.mulPose(Vector3f.ZP.rotationDegrees(-Mth.sin(shake * 3.0F) * shake));
        }
        // Pull the spear back along its axis so a stuck spear buries only its point.
        pose.translate(-0.35D, 0.0D, 0.0D);
        pose.mulPose(Vector3f.ZP.rotationDegrees(-45.0F));
        pose.scale(SCALE, SCALE, SCALE);
        ItemStack stack = spear.getSpear();
        this.itemRenderer.renderStatic(stack, ItemTransforms.TransformType.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffer, spear.getId());
        Vector3f shaft = new Vector3f(1.0F, 1.0F, 0.0F);
        shaft.normalize();
        pose.mulPose(shaft.rotationDegrees(90.0F));
        this.itemRenderer.renderStatic(stack, ItemTransforms.TransformType.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffer, spear.getId());
        pose.popPose();
        super.render(spear, yaw, partialTicks, pose, buffer, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ProjectileThrownSpear spear) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
