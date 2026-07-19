package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelMammoth;
import untamedwilds.entity.mammal.EntityMammoth;

public class RendererMammoth extends MobRenderer<EntityMammoth, EntityModel<EntityMammoth>> {

    private static final ModelMammoth MAMMOTH_MODEL = new ModelMammoth();

    public RendererMammoth(EntityRendererProvider.Context renderManager) {
        super(renderManager, MAMMOTH_MODEL, 1F);
    }

    protected void scale(EntityMammoth entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.7F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityMammoth entity) {
        return entity.getTexture();
    }
}
