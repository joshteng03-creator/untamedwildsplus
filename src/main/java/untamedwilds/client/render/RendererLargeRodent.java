package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelLargeRodent;
import untamedwilds.entity.mammal.EntityLargeRodent;

public class RendererLargeRodent extends MobRenderer<EntityLargeRodent, EntityModel<EntityLargeRodent>> {

    private static final ModelLargeRodent MODEL = new ModelLargeRodent();

    public RendererLargeRodent(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityLargeRodent entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.5F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityLargeRodent entity) {
        return entity.getTexture();
    }
}
