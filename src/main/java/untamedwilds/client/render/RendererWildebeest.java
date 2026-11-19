package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelWildebeest;
import untamedwilds.entity.mammal.EntityWildebeest;

public class RendererWildebeest extends MobRenderer<EntityWildebeest, EntityModel<EntityWildebeest>> {

    private static final ModelWildebeest MODEL = new ModelWildebeest();

    public RendererWildebeest(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityWildebeest entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.6F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityWildebeest entity) {
        return entity.getTexture();
    }
}
