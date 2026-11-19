package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelPinniped;
import untamedwilds.entity.mammal.EntityPinniped;

public class RendererPinniped extends MobRenderer<EntityPinniped, EntityModel<EntityPinniped>> {

    private static final ModelPinniped MODEL = new ModelPinniped();

    public RendererPinniped(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityPinniped entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.7F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityPinniped entity) {
        return entity.getTexture();
    }
}
