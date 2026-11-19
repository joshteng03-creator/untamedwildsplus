package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelGreatApe;
import untamedwilds.entity.mammal.EntityGreatApe;

/** Great apes (2.9.0). One model for adults and infants, scaled by species size. */
public class RendererGreatApe extends MobRenderer<EntityGreatApe, EntityModel<EntityGreatApe>> {

    private static final ModelGreatApe APE_MODEL = new ModelGreatApe();

    public RendererGreatApe(EntityRendererProvider.Context rendermanager) {
        super(rendermanager, APE_MODEL, 0.7F);
    }

    @Override
    protected void scale(EntityGreatApe entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.7F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityGreatApe entity) {
        return entity.getTexture();
    }
}
