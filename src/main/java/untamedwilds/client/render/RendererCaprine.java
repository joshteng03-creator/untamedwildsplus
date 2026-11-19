package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelCaprine;
import untamedwilds.entity.mammal.EntityCaprine;
import untamedwilds.client.render.layer.SaddleLayer;

public class RendererCaprine extends MobRenderer<EntityCaprine, EntityModel<EntityCaprine>> {

    private static final ModelCaprine MODEL = new ModelCaprine();

    public RendererCaprine(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
        this.addLayer(new SaddleLayer<>(this, "caprine"));
    }

    protected void scale(EntityCaprine entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.5F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityCaprine entity) {
        return entity.getTexture();
    }
}
