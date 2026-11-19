package untamedwilds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import untamedwilds.UntamedWilds;

/**
 * Item renderer for the carapace shield: the vanilla shield model (same layer, same flip as
 * BlockEntityWithoutLevelRenderer's shield branch) with its own 64x64 texture bound directly
 * instead of through the banner atlas.
 */
public class CarapaceShieldRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(UntamedWilds.MOD_ID, "textures/entity/carapace_shield.png");
    private static CarapaceShieldRenderer instance;
    private final EntityModelSet models;
    private ShieldModel model;

    private CarapaceShieldRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
        this.models = models;
    }

    /** Created on first use: item construction runs before Minecraft's renderers exist. */
    public static CarapaceShieldRenderer get() {
        if (instance == null) {
            Minecraft mc = Minecraft.getInstance();
            instance = new CarapaceShieldRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemTransforms.TransformType transform, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (this.model == null) {
            this.model = new ShieldModel(this.models.bakeLayer(ModelLayers.SHIELD));
        }
        pose.pushPose();
        pose.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffer, this.model.renderType(TEXTURE), true, stack.hasFoil());
        this.model.renderToBuffer(pose, consumer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }
}
