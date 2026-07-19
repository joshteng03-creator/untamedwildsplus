package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelDireWolf;
import untamedwilds.client.model.ModelRhinoCalf;
import untamedwilds.entity.mammal.EntityDireWolf;

import javax.annotation.Nonnull;

public class RendererDireWolf extends MobRenderer<EntityDireWolf, EntityModel<EntityDireWolf>> {

    private static final ModelDireWolf WOLF_MODEL = new ModelDireWolf();

    public RendererDireWolf(EntityRendererProvider.Context renderManager) {
        super(renderManager, WOLF_MODEL, 1F);
    }

    /*@Override
    public void render(EntityDireWolf entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
        if (entityIn.isBaby()) {
            model = WOLF_MODEL;
        } else {
            model = WOLF_MODEL;
        }
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }*/

    protected void scale(EntityDireWolf entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.6F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityDireWolf entity) {
        return entity.getTexture();
    }
}
