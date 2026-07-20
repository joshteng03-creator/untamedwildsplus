package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelAntelope;
import untamedwilds.entity.mammal.EntityAntelope;

public class RendererAntelope extends MobRenderer<EntityAntelope, EntityModel<EntityAntelope>> {

    private static final ModelAntelope MODEL = new ModelAntelope();

    public RendererAntelope(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityAntelope entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.6F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityAntelope entity) {
        return entity.getTexture();
    }
}
