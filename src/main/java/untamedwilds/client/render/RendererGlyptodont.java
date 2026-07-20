package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelGlyptodont;
import untamedwilds.entity.mammal.EntityGlyptodont;

public class RendererGlyptodont extends MobRenderer<EntityGlyptodont, EntityModel<EntityGlyptodont>> {

    private static final ModelGlyptodont MODEL = new ModelGlyptodont();

    public RendererGlyptodont(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityGlyptodont entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.6F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityGlyptodont entity) {
        return entity.getTexture();
    }
}
