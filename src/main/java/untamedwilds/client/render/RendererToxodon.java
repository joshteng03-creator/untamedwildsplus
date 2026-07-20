package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelToxodon;
import untamedwilds.entity.mammal.EntityToxodon;

public class RendererToxodon extends MobRenderer<EntityToxodon, EntityModel<EntityToxodon>> {

    private static final ModelToxodon MODEL = new ModelToxodon();

    public RendererToxodon(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityToxodon entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.6F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityToxodon entity) {
        return entity.getTexture();
    }
}
