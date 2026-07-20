package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelEquid;
import untamedwilds.entity.mammal.EntityEquid;

public class RendererEquid extends MobRenderer<EntityEquid, EntityModel<EntityEquid>> {

    private static final ModelEquid MODEL = new ModelEquid();

    public RendererEquid(EntityRendererProvider.Context renderManager) {
        super(renderManager, MODEL, 1F);
    }

    protected void scale(EntityEquid entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.6F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityEquid entity) {
        return entity.getTexture();
    }
}
