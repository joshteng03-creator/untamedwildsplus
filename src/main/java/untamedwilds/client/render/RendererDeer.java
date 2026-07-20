package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelDeer;
import untamedwilds.entity.mammal.EntityDeer;

public class RendererDeer extends MobRenderer<EntityDeer, EntityModel<EntityDeer>> {

    private static final ModelDeer MODEL = new ModelDeer();

    public RendererDeer(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityDeer entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.6F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityDeer entity) {
        return entity.getTexture();
    }
}
