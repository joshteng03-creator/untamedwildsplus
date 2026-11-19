package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import untamedwilds.UntamedWilds;
import untamedwilds.block.blockentity.SpeciesPlacardBlockEntity;

/**
 * Draws a Species Placard's face: the species' 16x16 encyclopedia icon at the top, the common name below
 * it, the scientific name in italics under that -- like a zoo enclosure sign. Text is centred and shrunk to
 * fit the plaque. The plaque model's front face is at z = 1.5/16 when the block faces south.
 */
public class SpeciesPlacardRenderer implements BlockEntityRenderer<SpeciesPlacardBlockEntity> {

    private static final float FACE_Z = 1.5F / 16.0F + 0.002F;
    private static final float TEXT_WIDTH = 12.0F / 16.0F;
    private final Font font;

    public SpeciesPlacardRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(SpeciesPlacardBlockEntity placard, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        String type = placard.getEntityType();
        if (type.isEmpty()) {
            return;
        }
        String path = new ResourceLocation(type).getPath();
        String species = placard.getSpecies();
        float yaw = placard.getBlockState().getValue(HorizontalDirectionalBlock.FACING).toYRot();
        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        pose.mulPose(Vector3f.YP.rotationDegrees(-yaw));
        pose.translate(0.0D, 0.0D, -0.5D + FACE_Z);

        // icon: 5/16 square, upper part of the plaque
        ResourceLocation icon = new ResourceLocation(UntamedWilds.MOD_ID, "textures/icon/" + path + "/" + (species.isEmpty() ? "category" : species) + ".png");
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(icon));
        float s = 2.5F / 16.0F, top = 4.2F / 16.0F;
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        quad(vc, m, n, -s, top - 2 * s, s, top, light);

        String key = "entity.untamedwilds." + path + (species.isEmpty() ? "" : "_" + species);
        Component name = new TranslatableComponent(key);
        TranslatableComponent sci = new TranslatableComponent(key + ".sciname");
        text(pose, buffer, name.getVisualOrderText(), -0.2F / 16.0F, 0.0105F, 0xFF2A2018, light);
        if (!sci.getString().equals(key + ".sciname")) {
            text(pose, buffer, sci.withStyle(ChatFormatting.ITALIC).getVisualOrderText(), -2.2F / 16.0F, 0.0075F, 0xFF4A3C2C, light);
        }
        pose.popPose();
    }

    private void text(PoseStack pose, MultiBufferSource buffer, FormattedCharSequence line, float y, float scale, int colour, int light) {
        int width = this.font.width(line);
        float fit = Math.min(scale, TEXT_WIDTH / Math.max(1, width));
        pose.pushPose();
        pose.translate(0.0D, y, 0.001D);
        pose.scale(fit, -fit, fit);
        this.font.drawInBatch(line, -width / 2.0F, -this.font.lineHeight / 2.0F, colour, false, pose.last().pose(), buffer, false, 0, light);
        pose.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, float x0, float y0, float x1, float y1, int light) {
        vc.vertex(m, x0, y0, 0).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, x1, y0, 0).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, x1, y1, 0).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, x0, y1, 0).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
    }
}
