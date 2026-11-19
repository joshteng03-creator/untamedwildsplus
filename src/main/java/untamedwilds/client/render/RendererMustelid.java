package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.client.model.ModelMustelid;
import untamedwilds.entity.mammal.EntityMustelid;

/** Mustelids (2.9.0). One model for adults and kits, scaled by species size. */
public class RendererMustelid extends MobRenderer<EntityMustelid, EntityModel<EntityMustelid>> {

    private static final ModelMustelid MUSTELID_MODEL = new ModelMustelid();

    public RendererMustelid(EntityRendererProvider.Context rendermanager) {
        super(rendermanager, MUSTELID_MODEL, 0.4F);
    }

    @Override
    protected void scale(EntityMustelid entity, PoseStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getScale();
        matrixStackIn.scale(f, f, f);
        this.shadowRadius = f * 0.45F;
    }

    public @NotNull ResourceLocation getTextureLocation(EntityMustelid entity) {
        return entity.getTexture();
    }
}
