package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.mammal.EntityOrca;

/**
 * Toothed whales -- orca and false killer whale (2.9.0, NEW TYPE): 17 boxes on 256x128, authored in
 * scripts/orca_spec.py, ported VERBATIM by scripts/gen_model_orca.py, proved by verify_port.py.
 *
 * A deep fusiform body round in section (core + back ridge + belly keel), a blunt head with a hinged jaw, a tail stock
 * into broad swept flukes, big paddle pectorals and a two-segment dorsal fin. Cetaceans swim with an UP-AND-DOWN fluke
 * stroke (chainWave on the tail chain, never a fish's side-to-side swing); turning bends the tail through the yaw ring
 * buffer, and the body pitches with the animal's dive angle.
 *   * dorsal height per animal: male orca 1.0, female 0.55, `finScale` (x1000) for species with a small fin -- setScale
 *     on dorsal_1 with its child, about its root on the back. Written every frame: the model is shared.
 */
public class ModelOrca extends AdvancedEntityModel<EntityOrca> {

    private final AdvancedModelBox body_main;
    private final AdvancedModelBox body_ridge;
    private final AdvancedModelBox body_keel;
    private final AdvancedModelBox head;
    private final AdvancedModelBox head_melon;
    private final AdvancedModelBox jaw;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox body_rear;
    private final AdvancedModelBox tail_1;
    private final AdvancedModelBox tail_2;
    private final AdvancedModelBox fluke_left;
    private final AdvancedModelBox fluke_right;
    private final AdvancedModelBox fin_left;
    private final AdvancedModelBox fin_right;
    private final AdvancedModelBox dorsal_1;
    private final AdvancedModelBox dorsal_2;

    private final ModelAnimator animator;
    private final AdvancedModelBox[] tailParts;

    public ModelOrca() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_main = new AdvancedModelBox(this, 0, 0);
        this.body_main.setRotationPoint(0.0F, 17.5000F, 0.0F);
        this.body_main.addBox(-5.2000F, -5.5000F, -10.5000F, 10.4000F, 11.0F, 19.0F, 0.0F);
        this.body_ridge = new AdvancedModelBox(this, 146, 0);
        this.body_ridge.setRotationPoint(0.0F, -5.0F, 0.0F);
        this.body_ridge.addBox(-3.8000F, -1.4000F, -9.0F, 7.6000F, 2.9000F, 15.8000F, 0.0F);
        this.body_keel = new AdvancedModelBox(this, 98, 0);
        this.body_keel.setRotationPoint(0.0F, 5.0F, 0.0F);
        this.body_keel.addBox(-3.9000F, -1.1000F, -8.8000F, 7.8000F, 2.4000F, 15.8000F, 0.0F);
        this.head = new AdvancedModelBox(this, 193, 0);
        this.head.setRotationPoint(0.0F, 0.0F, -10.0F);
        this.head.addBox(-4.3000F, -4.3000F, -9.0F, 8.6000F, 8.4000F, 9.5000F, 0.0F);
        this.head_melon = new AdvancedModelBox(this, 107, 30);
        this.head_melon.setRotationPoint(0.0F, -0.5000F, -9.0F);
        this.head_melon.addBox(-2.9000F, -2.2000F, -3.0F, 5.8000F, 5.8000F, 3.4000F, 0.0F);
        this.jaw = new AdvancedModelBox(this, 45, 30);
        this.jaw.setRotationPoint(0.0F, 3.5000F, -1.5000F);
        this.jaw.addBox(-3.3000F, -0.1000F, -9.9000F, 6.6000F, 1.9000F, 10.4000F, 0.0F);
        this.eye_left = new AdvancedModelBox(this, 234, 30);
        this.eye_left.setRotationPoint(4.6500F, 0.9000F, -5.2000F);
        this.eye_left.addBox(0.0F, -0.5000F, -1.0F, 0.0F, 1.0F, 2.0F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 238, 30);
        this.eye_right.setRotationPoint(-4.6500F, 0.9000F, -5.2000F);
        this.eye_right.addBox(0.0F, -0.5000F, -1.0F, 0.0F, 1.0F, 2.0F, 0.0F);
        this.body_rear = new AdvancedModelBox(this, 59, 0);
        this.body_rear.setRotationPoint(0.0F, -0.1000F, 8.0F);
        this.body_rear.addBox(-4.4000F, -4.8000F, -0.5000F, 8.8000F, 9.8000F, 10.5000F, 0.0F);
        this.tail_1 = new AdvancedModelBox(this, 0, 30);
        this.tail_1.setRotationPoint(0.0F, -0.2000F, 9.5000F);
        this.tail_1.addBox(-2.4000F, -3.2000F, -0.5000F, 4.8000F, 7.0F, 9.0F, 0.0F);
        this.tail_2 = new AdvancedModelBox(this, 79, 30);
        this.tail_2.setRotationPoint(0.0F, 0.0F, 8.0F);
        this.tail_2.addBox(-1.5000F, -2.0F, -0.5000F, 3.0F, 4.3000F, 6.5000F, 0.0F);
        this.fluke_left = new AdvancedModelBox(this, 188, 30);
        this.fluke_left.setRotationPoint(1.5000F, 0.2000F, 5.0F);
        this.fluke_left.addBox(0.0F, -0.4500F, -1.5000F, 6.8000F, 0.9000F, 4.5000F, 0.0F);
        this.setRotateAngle(fluke_left, 0.0F, -0.3840F, 0.0F);
        this.fluke_right = new AdvancedModelBox(this, 211, 30);
        this.fluke_right.setRotationPoint(-1.5000F, 0.2000F, 5.0F);
        this.fluke_right.addBox(-6.8000F, -0.4500F, -1.5000F, 6.8000F, 0.9000F, 4.5000F, 0.0F);
        this.setRotateAngle(fluke_right, 0.0F, 0.3840F, 0.0F);
        this.fin_left = new AdvancedModelBox(this, 126, 30);
        this.fin_left.setRotationPoint(4.9000F, 3.1000F, -6.0F);
        this.fin_left.addBox(0.0F, -0.4000F, -2.8000F, 9.5000F, 0.8000F, 5.8000F, 0.0F);
        this.setRotateAngle(fin_left, 0.0F, -0.6109F, 0.3142F);
        this.fin_right = new AdvancedModelBox(this, 157, 30);
        this.fin_right.setRotationPoint(-4.9000F, 3.1000F, -6.0F);
        this.fin_right.addBox(-9.5000F, -0.4000F, -2.8000F, 9.5000F, 0.8000F, 5.8000F, 0.0F);
        this.setRotateAngle(fin_right, 0.0F, 0.6109F, -0.3142F);
        this.dorsal_1 = new AdvancedModelBox(this, 28, 30);
        this.dorsal_1.setRotationPoint(0.0F, -5.8000F, 1.5000F);
        this.dorsal_1.addBox(-0.7000F, -5.2000F, -3.5000F, 1.4000F, 6.5000F, 7.0F, 0.0F);
        this.setRotateAngle(dorsal_1, -0.1047F, 0.0F, 0.0F);
        this.dorsal_2 = new AdvancedModelBox(this, 98, 30);
        this.dorsal_2.setRotationPoint(0.0F, -4.7000F, 0.5000F);
        this.dorsal_2.addBox(-0.4500F, -7.0F, -2.2000F, 0.9000F, 7.4000F, 3.6000F, 0.0F);
        this.setRotateAngle(dorsal_2, -0.2094F, 0.0F, 0.0F);

        this.body_main.addChild(this.body_ridge);
        this.body_main.addChild(this.body_keel);
        this.body_main.addChild(this.head);
        this.head.addChild(this.head_melon);
        this.head.addChild(this.jaw);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.eye_right);
        this.body_main.addChild(this.body_rear);
        this.body_rear.addChild(this.tail_1);
        this.tail_1.addChild(this.tail_2);
        this.tail_2.addChild(this.fluke_left);
        this.tail_2.addChild(this.fluke_right);
        this.body_main.addChild(this.fin_left);
        this.body_main.addChild(this.fin_right);
        this.body_main.addChild(this.dorsal_1);
        this.dorsal_1.addChild(this.dorsal_2);

        this.dorsal_1.setShouldScaleChildren(true);
        this.tailParts = new AdvancedModelBox[]{body_rear, tail_1, tail_2};
        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_main);
    }

    // Built once: getAllParts() runs every frame (2.8.0).
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
                body_main,
                body_ridge,
                body_keel,
                head,
                head_melon,
                jaw,
                eye_left,
                eye_right,
                body_rear,
                tail_1,
                tail_2,
                fluke_left,
                fluke_right,
                fin_left,
                fin_right,
                dorsal_1,
                dorsal_2
            );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        animator.update(entityIn);
        // ATTACK_BITE (14t): the head draws back, then lunges with the jaw dropped open, and snaps shut
        animator.setAnimation(EntityOrca.ATTACK_BITE);
        animator.startKeyframe(4);
        this.rotate(animator, head, -8.0F, 0, 0);
        this.rotate(animator, jaw, 30.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, head, 6.0F, 0, 0);
        this.rotate(animator, jaw, 34.0F, 0, 0);
        animator.move(body_main, 0, 0, -2.0F);
        animator.endKeyframe();
        animator.startKeyframe(2);
        this.rotate(animator, head, 4.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityOrca orca, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(orca);

        // ---- dorsal fin height per animal (scale is not restored by resetToDefaultPose: write it every frame) ----
        int fin = ComplexMob.speciesFlag(orca, "finScale");
        float finScale = fin > 0 ? fin / 1000F : (orca.isMale() ? 1.0F : 0.55F);
        this.dorsal_1.setScale(1.0F, finScale, 1.0F);

        // ---- breathing ----
        float breath = (float) (Math.sin(ageInTicks / 24) * 0.025F);
        this.body_main.setScale(1.0F + breath, 1.0F + breath, 1.0F);

        boolean swimming = orca.isInWater();
        if (swimming) {
            this.body_main.rotateAngleX += Mth.clamp(orca.getXRot(), -35, 35) * ((float) Math.PI / 180F);
            // the fluke stroke: always on (a whale never stops sculling), stronger when it is going somewhere
            float amt = 0.35F + Math.min(0.65F, limbSwingAmount * 1.5F);
            float speed = orca.isAngry() ? 0.32F : 0.2F;
            this.chainWave(tailParts, speed, 0.16F * amt, -2.5, ageInTicks, 1.0F);
            this.body_main.rotateAngleX += Mth.cos(ageInTicks * speed) * 0.025F * amt;
            this.flap(fin_left, speed, 0.12F, false, 0.5F, 0F, ageInTicks, amt);
            this.flap(fin_right, speed, 0.12F, true, 0.5F, 0F, ageInTicks, amt);
            float partialTicks = ageInTicks - orca.tickCount;
            float renderYaw = (float) orca.getMovementOffsets(0, partialTicks)[0];
            this.tail_1.rotateAngleY += smartClamp((float) orca.getMovementOffsets(12, partialTicks)[0] - renderYaw, -30, 30) * ((float) Math.PI / 180F);
            this.tail_2.rotateAngleY += smartClamp((float) orca.getMovementOffsets(16, partialTicks)[0] - renderYaw, -30, 30) * ((float) Math.PI / 180F);
            this.body_main.rotateAngleZ += smartClamp((float) orca.getMovementOffsets(6, partialTicks)[0] - renderYaw, -15, 15) * ((float) Math.PI / 180F);
        }
        else {
            // stranded: the flukes lift and fall
            this.tail_1.rotateAngleX += Mth.cos(ageInTicks * 0.15F) * 0.12F;
        }

        // Blinking: bury the eye planes inside the head; y/z keep the constructor's values.
        if (!orca.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(2.3250F, 0.9000F, -5.2000F);
            this.eye_right.setRotationPoint(-2.3250F, 0.9000F, -5.2000F);
        }
    }

    // getMovementOffsets can return a large negative angle, which throws off a plain clamp
    public float smartClamp(float angle, int min, int max) {
        float val = Math.abs(angle);
        if (val > 180) {
            angle = 360 - val;
        }
        return Mth.clamp(angle, min, max);
    }
}
