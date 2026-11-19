package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import untamedwilds.entity.EntityMountedSpecimen;

/**
 * Draws a mounted specimen by handing its un-ticked animal copy to the species' own renderer, with
 * partial ticks 0 so nothing interpolates: the copy's walk speed, age-in-ticks and rotations are all
 * fixed, which freezes the model in whatever pose {@link EntityMountedSpecimen} set. The mount's own
 * name tag shows when looked at, like a mob's.
 */
public class RendererMountedSpecimen extends EntityRenderer<EntityMountedSpecimen> {

    public RendererMountedSpecimen(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityMountedSpecimen mount, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light) {
        Entity copy = mount.getDisplayCopy();
        if (copy != null) {
            this.entityRenderDispatcher.render(copy, 0.0D, 0.0D, 0.0D, mount.getFacingYaw(), 0.0F, pose, buffer, light);
        }
        super.render(mount, yaw, partialTicks, pose, buffer, light);
    }

    @Override
    protected boolean shouldShowName(EntityMountedSpecimen mount) {
        return mount.hasCustomName() && mount == this.entityRenderDispatcher.crosshairPickEntity;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityMountedSpecimen mount) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
