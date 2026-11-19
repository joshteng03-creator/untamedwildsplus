package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelGiantMonitor;
import untamedwilds.entity.reptile.EntityGiantMonitor;

public class RendererGiantMonitor extends MobRenderer<EntityGiantMonitor, EntityModel<EntityGiantMonitor>> {

    private static final ModelGiantMonitor MODEL = new ModelGiantMonitor();

    public RendererGiantMonitor(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityGiantMonitor entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.8F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityGiantMonitor entity) {
        return entity.getTexture();
    }
}
