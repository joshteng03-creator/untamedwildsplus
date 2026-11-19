package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelOrca;
import untamedwilds.entity.mammal.EntityOrca;

/** Toothed whales (2.9.0). One model for adults and calves, scaled by species size like the shark. */
public class RendererOrca extends MobRenderer<EntityOrca, EntityModel<EntityOrca>> {

    private static final ModelOrca ORCA_MODEL = new ModelOrca();

    public RendererOrca(EntityRendererProvider.Context rendermanager) {
        super(rendermanager, ORCA_MODEL, 1.2F);
    }

    @Override
    protected void scale(EntityOrca entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.6F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityOrca entity) {
        return entity.getTexture();
    }
}
