package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityPinniped;

/**
 * Pinnipeds -- walrus, bearded seal, ringed seal, harp seal, Steller sea lion (Phase 4.4, 2026-10-10): 18 boxes on 256x128,
 * authored in scripts/pinniped_spec.py, ported VERBATIM by scripts/gen_model_pinniped.py, proved by verify_port.py.
 *
 * REST = hauled out lying prone: belly on the ground, head raised, broad fore paddles swept back with their tips on the
 * ground, hind fans side by side drooping to the ground behind.
 *   * Per species (getVariant()): whisker pad (walrus, bearded seal), tusks (walrus), ear flaps (sea lion).
 *   * The Steller sea lion stands in its SOLVED otariid stance on land -- chest raised 24 deg on fore flippers 1.45x
 *     longer (setScale on the flipper's long axis), belly counter-rotated flat -- and walks on all four flippers.
 *   * Seals GALUMPH on land (a model-only body wave and bounce, the way true seals hump along on their bellies) and
 *     SWIM with a lateral body wave driven by sweeping hind flippers, fore flippers tucked; the sea lion swims by
 *     flapping its fore flippers instead.
 * resetToDefaultPose does not restore scale, so the flipper scale is written on both branches every frame.
 */
public class ModelPinniped extends AdvancedEntityModel<EntityPinniped> {

    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox body_belly;
    private final AdvancedModelBox body_hips;
    private final AdvancedModelBox hind_left;
    private final AdvancedModelBox hind_right;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox muzzle;
    private final AdvancedModelBox nose;
    private final AdvancedModelBox whiskers;
    private final AdvancedModelBox tusk_left;
    private final AdvancedModelBox tusk_right;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox fore_left;
    private final AdvancedModelBox fore_right;

    private final ModelAnimator animator;

    public ModelPinniped() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_chest = new AdvancedModelBox(this, 0, 0);
        this.body_chest.setRotationPoint(0.0F, 19.5000F, 0.0F);
        this.body_chest.addBox(-4.6000F, -4.5000F, -6.5000F, 9.2000F, 8.7000F, 10.5000F, 0.0F);
        this.body_belly = new AdvancedModelBox(this, 40, 0);
        this.body_belly.setRotationPoint(0.0F, 0.0F, 3.5000F);
        this.body_belly.addBox(-4.3000F, -3.9000F, -0.5000F, 8.6000F, 8.4000F, 8.5000F, 0.0F);
        this.body_hips = new AdvancedModelBox(this, 75, 0);
        this.body_hips.setRotationPoint(0.0F, 0.2000F, 7.5000F);
        this.body_hips.addBox(-3.4000F, -2.7000F, -0.5000F, 6.8000F, 6.4000F, 7.0F, 0.0F);
        this.hind_left = new AdvancedModelBox(this, 170, 0);
        this.hind_left.setRotationPoint(1.6500F, 2.1000F, 6.0F);
        this.hind_left.addBox(-1.5000F, -0.4500F, -0.8000F, 2.9500F, 0.9000F, 5.4000F, 0.0F);
        this.setRotateAngle(hind_left, -0.3863F, 0.0F, 0.0F);
        this.hind_right = new AdvancedModelBox(this, 187, 0);
        this.hind_right.setRotationPoint(-1.6500F, 2.1000F, 6.0F);
        this.hind_right.addBox(-1.4500F, -0.4500F, -0.8000F, 2.9500F, 0.9000F, 5.4000F, 0.0F);
        this.setRotateAngle(hind_right, -0.3863F, 0.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 103, 0);
        this.neck.setRotationPoint(0.0F, -1.0F, -6.0F);
        this.neck.addBox(-3.4000F, -3.0F, -3.6000F, 6.8000F, 6.2000F, 5.6000F, 0.0F);
        this.setRotateAngle(neck, -0.3840F, 0.0F, 0.0F);
        this.head = new AdvancedModelBox(this, 128, 0);
        this.head.setRotationPoint(0.0F, 0.0F, -3.6000F);
        this.head.addBox(-2.9000F, -2.4000F, -4.6000F, 5.8000F, 4.6000F, 6.0F, 0.0F);
        this.setRotateAngle(head, 0.2793F, 0.0F, 0.0F);
        this.muzzle = new AdvancedModelBox(this, 204, 0);
        this.muzzle.setRotationPoint(0.0F, 0.0F, -4.6000F);
        this.muzzle.addBox(-1.9000F, -1.4000F, -2.6000F, 3.8000F, 2.9000F, 3.6000F, 0.0F);
        this.setRotateAngle(muzzle, 0.1745F, 0.0F, 0.0F);
        this.nose = new AdvancedModelBox(this, 4, 20);
        this.nose.setRotationPoint(0.0F, -0.5000F, -2.7000F);
        this.nose.addBox(-1.1000F, -0.5000F, -0.4000F, 2.2000F, 1.0F, 0.9000F, 0.0F);
        this.whiskers = new AdvancedModelBox(this, 227, 0);
        this.whiskers.setRotationPoint(0.0F, 1.1000F, -1.4000F);
        this.whiskers.addBox(-2.5000F, -0.7000F, -1.6000F, 5.0F, 1.6000F, 2.4000F, 0.0F);
        this.tusk_left = new AdvancedModelBox(this, 219, 0);
        this.tusk_left.setRotationPoint(0.9500F, 1.2000F, -1.8000F);
        this.tusk_left.addBox(-0.4000F, -0.2000F, -0.4000F, 0.8000F, 5.4000F, 0.8000F, 0.0F);
        this.setRotateAngle(tusk_left, -0.2094F, 0.0F, 0.0F);
        this.tusk_right = new AdvancedModelBox(this, 223, 0);
        this.tusk_right.setRotationPoint(-0.9500F, 1.2000F, -1.8000F);
        this.tusk_right.addBox(-0.4000F, -0.2000F, -0.4000F, 0.8000F, 5.4000F, 0.8000F, 0.0F);
        this.setRotateAngle(tusk_right, -0.2094F, 0.0F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 250, 0);
        this.ear_left.setRotationPoint(2.9000F, -1.5000F, -1.0F);
        this.ear_left.addBox(-0.3000F, -1.4000F, -0.4000F, 1.0F, 1.8000F, 0.6000F, 0.0F);
        this.setRotateAngle(ear_left, 0.0F, 0.0F, 0.3491F);
        this.eye_left = new AdvancedModelBox(this, 242, 0);
        this.eye_left.setRotationPoint(3.2500F, -1.4000F, -2.2000F);
        this.eye_left.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.ear_right = new AdvancedModelBox(this, 0, 20);
        this.ear_right.setRotationPoint(-2.9000F, -1.5000F, -1.0F);
        this.ear_right.addBox(-0.7000F, -1.4000F, -0.4000F, 1.0F, 1.8000F, 0.6000F, 0.0F);
        this.setRotateAngle(ear_right, 0.0F, 0.0F, -0.3491F);
        this.eye_right = new AdvancedModelBox(this, 246, 0);
        this.eye_right.setRotationPoint(-3.2500F, -1.4000F, -2.2000F);
        this.eye_right.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.fore_left = new AdvancedModelBox(this, 152, 0);
        this.fore_left.setRotationPoint(4.4000F, 1.3000F, -3.4000F);
        this.fore_left.addBox(-0.3500F, -0.4000F, -1.6000F, 0.8000F, 4.5000F, 3.4000F, 0.0F);
        this.setRotateAngle(fore_left, 0.6981F, 0.0F, -0.7912F);
        this.fore_right = new AdvancedModelBox(this, 161, 0);
        this.fore_right.setRotationPoint(-4.4000F, 1.3000F, -3.4000F);
        this.fore_right.addBox(-0.4500F, -0.4000F, -1.6000F, 0.8000F, 4.5000F, 3.4000F, 0.0F);
        this.setRotateAngle(fore_right, 0.6981F, 0.0F, 0.7912F);

        this.body_chest.addChild(this.body_belly);
        this.body_belly.addChild(this.body_hips);
        this.body_hips.addChild(this.hind_left);
        this.body_hips.addChild(this.hind_right);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.muzzle);
        this.muzzle.addChild(this.nose);
        this.muzzle.addChild(this.whiskers);
        this.muzzle.addChild(this.tusk_left);
        this.muzzle.addChild(this.tusk_right);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.ear_right);
        this.head.addChild(this.eye_right);
        this.body_chest.addChild(this.fore_left);
        this.body_chest.addChild(this.fore_right);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_chest);
    }

    // Built once: getAllParts() is called every frame, and a fresh ImmutableList per animal per frame
    // was pure garbage (2.8.0, same as ModelBison).
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
                body_chest,
                body_belly,
                body_hips,
                hind_left,
                hind_right,
                neck,
                head,
                muzzle,
                nose,
                whiskers,
                tusk_left,
                tusk_right,
                ear_left,
                eye_left,
                ear_right,
                eye_right,
                fore_left,
                fore_right
        );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityPinniped seal = (EntityPinniped) entityIn;
        animator.update(seal);

        // ATTACK_BITE (12t): the head draws back, then lunges forward and down
        animator.setAnimation(EntityPinniped.ATTACK_BITE);
        animator.startKeyframe(5);
        this.rotate(animator, neck, -10.0F, 0, 0);
        this.rotate(animator, head, -6.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, 18.0F, 0, 0);
        this.rotate(animator, head, 8.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(3);

        // ATTACK_TUSK (16t): the walrus rears its head and chops DOWN with the tusks
        animator.setAnimation(EntityPinniped.ATTACK_TUSK);
        animator.startKeyframe(6);
        this.rotate(animator, neck, -22.0F, 0, 0);
        this.rotate(animator, head, -10.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, 30.0F, 0, 0);
        this.rotate(animator, head, 16.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(6);

        // ATTACK_JOUST (40t): cosmetic tusk-jousting between bull walruses -- chests raised, heads swung down in turn
        animator.setAnimation(EntityPinniped.ATTACK_JOUST);
        animator.startKeyframe(6);
        this.rotate(animator, body_chest, -14.0F, 0, 0);
        this.rotate(animator, neck, -18.0F, 0, 0);
        animator.endKeyframe();
        for (int i = 0; i < 3; i++) {
            animator.startKeyframe(5);
            this.rotate(animator, body_chest, -14.0F, 0, 0);
            this.rotate(animator, neck, 20.0F, 10.0F, 0);
            this.rotate(animator, head, 10.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(5);
            this.rotate(animator, body_chest, -14.0F, 0, 0);
            this.rotate(animator, neck, -18.0F, -10.0F, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityPinniped seal, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(seal);
        limbSwingAmount = Math.min(0.5F, limbSwingAmount);
        float amt = Math.min(1.0F, limbSwingAmount / 0.3F);
        float gait = limbSwing * 0.7F / Math.max(0.3F, seal.getMobSize() * seal.getScale());
        boolean swimming = seal.isInWater() && !seal.isOnGround();

        // ---- per-species parts ----
        int variant = seal.getVariant();
        this.whiskers.showModel = variant == 0 || variant == 1;
        boolean tusks = variant == 0;
        this.tusk_left.showModel = tusks;
        this.tusk_right.showModel = tusks;
        boolean otariid = variant == 4;
        this.ear_left.showModel = otariid;
        this.ear_right.showModel = otariid;
        this.fore_left.setScale(1.0F, otariid ? 1.4500F : 1.0F, 1.0F);
        this.fore_right.setScale(1.0F, otariid ? 1.4500F : 1.0F, 1.0F);
        if (otariid && !swimming && seal.sleepProgress == 0) {
            // the SOLVED otariid stance (pinniped_spec.stance_sealion): chest up on the fore flippers, belly flat
            this.body_chest.rotationPointY -= 1.4236F;
            this.setRotateAngle(body_chest, -0.4189F, -0.0F, 0.0F);
            this.setRotateAngle(body_belly, 0.4189F, -0.0F, 0.0F);
            this.setRotateAngle(fore_left, -0.0F, -0.0F, -0.2618F);
            this.setRotateAngle(fore_right, -0.0F, -0.0F, 0.2618F);
        }

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 22) * 0.03F);
        this.body_belly.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(hind_left, 0.08F, 0.08F, false, 0F, 0F, ageInTicks, 1);
        this.swing(hind_right, 0.08F, 0.08F, false, 1F, 0F, ageInTicks, 1);

        // Blinking: bury the eye planes inside the skull; y/z keep the constructor's values.
        if (!seal.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(1.7875F, -1.4000F, -2.2000F);
            this.eye_right.setRotationPoint(-1.7875F, -1.4000F, -2.2000F);
        }

        if (!seal.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        if (swimming) {
            float sw = ageInTicks * 0.25F;
            float pitch = Mth.clamp(seal.getXRot(), -30F, 30F);
            this.body_chest.rotateAngleX += (float) Math.toRadians(pitch);
            if (otariid) {
                // sea lions fly through the water on their fore flippers
                this.fore_left.rotateAngleZ += Mth.sin(sw) * 0.6F;
                this.fore_right.rotateAngleZ -= Mth.sin(sw) * 0.6F;
                this.body_chest.rotateAngleY += Mth.cos(sw) * 0.04F;
            }
            else {
                // true seals scull with the hind flippers: a lateral wave down the body, fore flippers tucked
                this.fore_left.rotateAngleZ += 0.5F;
                this.fore_right.rotateAngleZ -= 0.5F;
                this.body_chest.rotateAngleY += Mth.cos(sw) * 0.06F;
                this.body_belly.rotateAngleY += Mth.cos(sw - 0.8F) * 0.10F;
                this.body_hips.rotateAngleY += Mth.cos(sw - 1.6F) * 0.16F;
                this.hind_left.rotateAngleY += Mth.cos(sw - 2.4F) * 0.45F;
                this.hind_right.rotateAngleY += Mth.cos(sw - 2.4F) * 0.45F;
            }
        }
        else if (seal.canMove() && limbSwingAmount > 0.02F) {
            if (otariid) {
                // sea lions walk on all four flippers
                this.fore_left.rotateAngleX += Mth.cos(gait) * 0.5F * amt;
                this.fore_right.rotateAngleX += Mth.cos(gait + Mth.PI) * 0.5F * amt;
                this.hind_left.rotateAngleX += Mth.cos(gait + Mth.PI) * 0.3F * amt;
                this.hind_right.rotateAngleX += Mth.cos(gait) * 0.3F * amt;
                this.body_chest.rotateAngleY += Mth.cos(gait) * 0.05F * amt;
            }
            else {
                // GALUMPH: the chest heaves up and forward, the wave rolls back down the body, the flippers push
                float sn = Mth.sin(gait);
                this.body_chest.rotateAngleX -= sn * 0.10F * amt;
                this.body_belly.rotateAngleX += sn * 0.08F * amt;
                this.body_hips.rotateAngleX += Mth.sin(gait - 1.0F) * 0.10F * amt;
                this.body_chest.rotationPointY -= Math.max(0, sn) * 1.2F * amt;
                this.fore_left.rotateAngleX += Mth.cos(gait) * 0.5F * amt;
                this.fore_right.rotateAngleX += Mth.cos(gait) * 0.5F * amt;
                this.hind_left.rotateAngleX -= sn * 0.3F * amt;
                this.hind_right.rotateAngleX -= sn * 0.3F * amt;
            }
        }

        if (seal.sitProgress > 0) {
            // pinniped_spec.rest_poses: the 'banana' -- head and hind flippers lifted
            this.progressRotation(neck, seal.sitProgress, -0.5934F, -0.0F, 0.0F, 40);
            this.progressRotation(head, seal.sitProgress, 0.1047F, -0.0F, 0.0F, 40);
            this.progressRotation(hind_left, seal.sitProgress, 0.3467F, -0.0F, 0.0F, 40);
            this.progressRotation(hind_right, seal.sitProgress, 0.3467F, -0.0F, 0.0F, 40);
        }
        else if (seal.sleepProgress > 0) {
            // pinniped_spec.rest_poses: lying flat, chin down on the ground
            this.progressRotation(neck, seal.sleepProgress, 0.1047F, -0.0F, 0.0F, 40);
            this.progressRotation(head, seal.sleepProgress, 0.1745F, -0.0F, 0.0F, 40);
        }
    }
}
