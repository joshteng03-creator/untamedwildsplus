package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelMacrauchenia;
import untamedwilds.entity.mammal.EntityMacrauchenia;

public class RendererMacrauchenia extends MobRenderer<EntityMacrauchenia, EntityModel<EntityMacrauchenia>> {

    private static final ModelMacrauchenia MODEL = new ModelMacrauchenia();

    public RendererMacrauchenia(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityMacrauchenia entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.6F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityMacrauchenia entity) {
        return entity.getTexture();
    }
}
