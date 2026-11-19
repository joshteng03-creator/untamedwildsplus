package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelDiprotodont;
import untamedwilds.entity.mammal.EntityDiprotodont;
import untamedwilds.client.render.layer.SaddleLayer;

public class RendererDiprotodont extends MobRenderer<EntityDiprotodont, EntityModel<EntityDiprotodont>> {

    private static final ModelDiprotodont MODEL = new ModelDiprotodont();

    public RendererDiprotodont(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
        this.addLayer(new SaddleLayer<>(this, "diprotodont"));
    }

    protected void scale(EntityDiprotodont entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.7F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityDiprotodont entity) {
        return entity.getTexture();
    }
}
