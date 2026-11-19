package untamedwilds.client.render;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import untamedwilds.UntamedWilds;
import untamedwilds.entity.ProjectileSerratedArrow;

public class RendererSerratedArrow extends ArrowRenderer<ProjectileSerratedArrow> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(UntamedWilds.MOD_ID, "textures/entity/projectiles/serrated_arrow.png");

    public RendererSerratedArrow(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ProjectileSerratedArrow entity) {
        return TEXTURE;
    }
}
