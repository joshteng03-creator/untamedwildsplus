package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelGiraffid;
import untamedwilds.entity.mammal.EntityGiraffid;

public class RendererGiraffid extends MobRenderer<EntityGiraffid, EntityModel<EntityGiraffid>> {

    private static final ModelGiraffid MODEL = new ModelGiraffid();

    public RendererGiraffid(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityGiraffid entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.6F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityGiraffid entity) {
        return entity.getTexture();
    }
}
