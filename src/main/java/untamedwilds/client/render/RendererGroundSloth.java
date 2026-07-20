package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelGroundSloth;
import untamedwilds.entity.mammal.EntityGroundSloth;

public class RendererGroundSloth extends MobRenderer<EntityGroundSloth, EntityModel<EntityGroundSloth>> {

    private static final ModelGroundSloth MODEL = new ModelGroundSloth();

    public RendererGroundSloth(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityGroundSloth entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.6F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityGroundSloth entity) {
        return entity.getTexture();
    }
}
