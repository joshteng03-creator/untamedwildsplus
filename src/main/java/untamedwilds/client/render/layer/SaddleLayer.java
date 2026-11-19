package untamedwilds.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import untamedwilds.UntamedWilds;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ISpecies;

import java.util.HashMap;
import java.util.Map;

/**
 * Saddle overlay (2026-10-08): when the animal is saddled, the model is drawn a second time with a mostly
 * transparent texture on the type's own UV layout that paints the saddle, blanket and girth onto the body,
 * the way vanilla draws horse markings. {@code textures/entity/<type>/saddle_<species>.png} wins when it
 * exists (camelids: each species' visible humps decide where the seat can go), else {@code saddle.png}.
 * Painted by scripts/gen_saddles.py in model space so the drape is continuous across stacked boxes.
 */
public class SaddleLayer<T extends ComplexMob, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private final String type;
    private final ResourceLocation fallback;
    private final Map<String, ResourceLocation> bySpecies = new HashMap<>();

    public SaddleLayer(RenderLayerParent<T, M> parent, String type) {
        super(parent);
        this.type = type;
        this.fallback = new ResourceLocation(UntamedWilds.MOD_ID, "textures/entity/" + type + "/saddle.png");
    }

    private ResourceLocation textureFor(T entity) {
        if (!(entity instanceof ISpecies species)) {
            return this.fallback;
        }
        String name = species.getRawSpeciesName(entity.getVariant());
        return this.bySpecies.computeIfAbsent(name, n -> {
            ResourceLocation own = new ResourceLocation(UntamedWilds.MOD_ID, "textures/entity/" + this.type + "/saddle_" + n + ".png");
            return Minecraft.getInstance().getResourceManager().hasResource(own) ? own : this.fallback;
        });
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.isSaddled() || entity.isInvisible()) {
            return;
        }
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(this.textureFor(entity)));
        this.getParentModel().renderToBuffer(pose, vc, light, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
