package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelCrocodilian;
import untamedwilds.entity.reptile.EntityCrocodilian;

public class RendererCrocodilian extends MobRenderer<EntityCrocodilian, EntityModel<EntityCrocodilian>> {

    private static final ModelCrocodilian MODEL = new ModelCrocodilian();

    public RendererCrocodilian(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityCrocodilian entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.8F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityCrocodilian entity) {
        return entity.getTexture();
    }
}
