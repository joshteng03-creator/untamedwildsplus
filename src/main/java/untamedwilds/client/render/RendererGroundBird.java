package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelGroundBird;
import untamedwilds.entity.bird.EntityGroundBird;

/** Giant ground birds (2.9.0). One model for adults and chicks, scaled by species size. */
public class RendererGroundBird extends MobRenderer<EntityGroundBird, EntityModel<EntityGroundBird>> {

    private static final ModelGroundBird BIRD_MODEL = new ModelGroundBird();

    public RendererGroundBird(EntityRendererProvider.Context rendermanager) {
        super(rendermanager, BIRD_MODEL, 0.6F);
    }

    @Override
    protected void scale(EntityGroundBird entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.55F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityGroundBird entity) {
        return entity.getTexture();
    }
}
