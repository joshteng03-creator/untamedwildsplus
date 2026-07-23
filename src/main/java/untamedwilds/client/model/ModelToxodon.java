package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityToxodon;

public class ModelToxodon extends AdvancedEntityModel<EntityToxodon> {

    private final AdvancedModelBox body_main;
    private final AdvancedModelBox body_torso;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head_main;
    private final AdvancedModelBox muzzle;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox incisors;
    private final AdvancedModelBox leg_front_left;
    private final AdvancedModelBox foot_front_left;
    private final AdvancedModelBox leg_front_right;
    private final AdvancedModelBox foot_front_right;
    private final AdvancedModelBox leg_back_left;
    private final AdvancedModelBox foot_back_left;
    private final AdvancedModelBox leg_back_right;
    private final AdvancedModelBox foot_back_right;
    private final AdvancedModelBox tail;

    private final ModelAnimator animator;

    public ModelToxodon() {
        this.texWidth = 128;
        this.texHeight = 128;

        // Sculpted fresh in Blockbench: low-slung barrel-bodied notoungulate (bulky deep chest tapering to
        // a lower rear, short sturdy legs with broad feet), an elongated head with a high skull sloping to
        // a broad low muzzle, small high-set ears + eyes, and the diagnostic chisel incisors. Box coords +
        // texOffs transcribed VERBATIM from the modded_entity export (128x128, Y-down flip handled by it).
        this.body_main = new AdvancedModelBox(this, 0, 0);
        this.body_main.setRotationPoint(0.0F, 14.0F, 8.0F);
        this.body_main.addBox(-7.0F, -13.0F, -7.0F, 14.0F, 13.0F, 19.0F, 0.0F);

        this.body_torso = new AdvancedModelBox(this, 67, 0);
        this.body_torso.setRotationPoint(0.0F, 1.0F, -14.0F);
        this.body_torso.addBox(-8.0F, -16.0F, -7.0F, 16.0F, 16.0F, 14.0F, 0.0F);

        this.neck = new AdvancedModelBox(this, 93, 72);
        this.neck.setRotationPoint(0.0F, -9.0F, -7.0F);
        this.neck.addBox(-5.0F, -7.0F, -3.0F, 10.0F, 7.0F, 4.0F, 0.0F);

        this.head_main = new AdvancedModelBox(this, 0, 33);
        this.head_main.setRotationPoint(0.0F, 0.0F, -3.0F);
        this.head_main.addBox(-5.0F, -6.0F, -6.0F, 10.0F, 9.5F, 10.0F, 0.0F);

        this.muzzle = new AdvancedModelBox(this, 41, 33);
        this.muzzle.setRotationPoint(0.0F, 2.0F, -6.0F);
        this.muzzle.addBox(-5.5F, -4.5F, -9.0F, 11.0F, 8.0F, 9.0F, 0.0F);

        this.ear_left = new AdvancedModelBox(this, 0, 84);
        this.ear_left.setRotationPoint(3.5F, -6.0F, 1.5F);
        this.ear_left.addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F, 0.0F);

        this.ear_right = new AdvancedModelBox(this, 13, 84);
        this.ear_right.setRotationPoint(-3.5F, -6.0F, 1.5F);
        this.ear_right.addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F, 0.0F);

        this.eye_left = new AdvancedModelBox(this, 26, 84);
        this.eye_left.setRotationPoint(5.05F, -1.75F, -3.75F);
        this.eye_left.addBox(0.0F, -1.25F, -1.25F, 0.0F, 2.5F, 2.5F, 0.0F);

        this.eye_right = new AdvancedModelBox(this, 33, 84);
        this.eye_right.setRotationPoint(-5.05F, -1.75F, -3.75F);
        this.eye_right.addBox(0.0F, -1.25F, -1.25F, 0.0F, 2.5F, 2.5F, 0.0F);

        this.incisors = new AdvancedModelBox(this, 40, 84);
        this.incisors.setRotationPoint(0.0F, 5.0F, -15.0F);
        this.incisors.addBox(-3.0F, -1.5F, -1.0F, 6.0F, 3.0F, 2.0F, 0.0F);

        this.leg_front_left = new AdvancedModelBox(this, 82, 33);
        this.leg_front_left.setRotationPoint(4.5F, -0.5F, -17.0F);
        this.leg_front_left.addBox(-3.0F, 0.0F, -3.0F, 6.0F, 10.5F, 6.0F, 0.0F);

        this.foot_front_left = new AdvancedModelBox(this, 92, 54);
        this.foot_front_left.setRotationPoint(0.0F, 8.0F, 0.0F);
        this.foot_front_left.addBox(-3.5F, 0.0F, -4.0F, 7.0F, 2.5F, 8.0F, 0.0F);

        this.leg_front_right = new AdvancedModelBox(this, 0, 54);
        this.leg_front_right.setRotationPoint(-4.5F, -0.5F, -17.0F);
        this.leg_front_right.addBox(-3.0F, 0.0F, -3.0F, 6.0F, 10.5F, 6.0F, 0.0F);

        this.foot_front_right = new AdvancedModelBox(this, 0, 72);
        this.foot_front_right.setRotationPoint(0.0F, 8.0F, 0.0F);
        this.foot_front_right.addBox(-3.5F, 0.0F, -4.0F, 7.0F, 2.5F, 8.0F, 0.0F);

        this.leg_back_left = new AdvancedModelBox(this, 25, 54);
        this.leg_back_left.setRotationPoint(4.0F, -0.5F, 5.0F);
        this.leg_back_left.addBox(-3.0F, 0.0F, -3.0F, 6.0F, 10.5F, 6.0F, 0.0F);

        this.foot_back_left = new AdvancedModelBox(this, 31, 72);
        this.foot_back_left.setRotationPoint(0.0F, 8.0F, 0.0F);
        this.foot_back_left.addBox(-3.5F, 0.0F, -4.0F, 7.0F, 2.5F, 8.0F, 0.0F);

        this.leg_back_right = new AdvancedModelBox(this, 50, 54);
        this.leg_back_right.setRotationPoint(-4.0F, -0.5F, 5.0F);
        this.leg_back_right.addBox(-3.0F, 0.0F, -3.0F, 6.0F, 10.5F, 6.0F, 0.0F);

        this.foot_back_right = new AdvancedModelBox(this, 62, 72);
        this.foot_back_right.setRotationPoint(0.0F, 8.0F, 0.0F);
        this.foot_back_right.addBox(-3.5F, 0.0F, -4.0F, 7.0F, 2.5F, 8.0F, 0.0F);

        this.tail = new AdvancedModelBox(this, 75, 54);
        this.tail.setRotationPoint(0.0F, -12.0F, 11.0F);
        this.tail.addBox(-1.5F, 0.0F, 0.0F, 3.0F, 7.0F, 5.0F, 0.0F);

        this.body_main.addChild(this.body_torso);
        this.body_torso.addChild(this.neck);
        this.neck.addChild(this.head_main);
        this.head_main.addChild(this.muzzle);
        this.head_main.addChild(this.ear_left);
        this.head_main.addChild(this.ear_right);
        this.head_main.addChild(this.eye_left);
        this.head_main.addChild(this.eye_right);
        this.head_main.addChild(this.incisors);
        this.body_main.addChild(this.leg_front_left);
        this.leg_front_left.addChild(this.foot_front_left);
        this.body_main.addChild(this.leg_front_right);
        this.leg_front_right.addChild(this.foot_front_right);
        this.body_main.addChild(this.leg_back_left);
        this.leg_back_left.addChild(this.foot_back_left);
        this.body_main.addChild(this.leg_back_right);
        this.leg_back_right.addChild(this.foot_back_right);
        this.body_main.addChild(this.tail);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_main);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(
            body_main, body_torso, neck, head_main, muzzle,
            ear_left, ear_right, eye_left, eye_right, incisors,
            leg_front_left, foot_front_left, leg_front_right, foot_front_right,
            leg_back_left, foot_back_left, leg_back_right, foot_back_right,
            tail
        );
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityToxodon toxodon = (EntityToxodon) entityIn;
        animator.update(toxodon);

        animator.setAnimation(EntityToxodon.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_torso, 0, 0, 7.83F);
            this.rotate(animator, head_main, 7.83F, 0, -13.04F);
            animator.move(leg_front_left, 0, -0.6F, 0);
            this.rotate(animator, leg_front_left, 0, 0, -7.83F);
            animator.move(leg_front_right, 0, -0.5F, 0);
            this.rotate(animator, leg_front_right, -20F, 0, -5.21F);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_torso, 0, 0, -13.05F);
            this.rotate(animator, head_main, 7.83F, 0, 26.08F);
            animator.move(leg_front_left, 0, 0.5F, 0);
            this.rotate(animator, leg_front_left, 0, 0, 13.04F);
            animator.move(leg_front_right, 0, 0.5F, 0);
            this.rotate(animator, leg_front_right, 24.79F, 0, 10.43F);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        animator.setAnimation(EntityToxodon.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, head_main, 31.31F, 0, 26.08F);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, head_main, -26.08F, 0, -46.96F);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityToxodon toxodon, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(toxodon);
        float globalSpeed = 1.5f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // Breathing Animation
        this.body_main.setScale((float) (1.0F + Math.sin(ageInTicks / 20) * 0.08F), (float) (1.0F + Math.sin(ageInTicks / 16) * 0.08F), 1.0F);
        this.body_torso.setScale((float) (1.0F + Math.sin(ageInTicks / 20) * 0.08F), (float) (1.0F + Math.sin(ageInTicks / 16) * 0.08F), 1.0F);
        bob(body_main, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        bob(leg_front_left, 0.4F * globalSpeed, 0.1F, false, -ageInTicks / 20, 2);
        bob(leg_front_right, 0.4F * globalSpeed, 0.1F, false, -ageInTicks / 20, 2);
        bob(leg_back_left, 0.4F * globalSpeed, 0.1F, false, -ageInTicks / 20, 2);
        bob(leg_back_right, 0.4F * globalSpeed, 0.1F, false, -ageInTicks / 20, 2);
        walk(head_main, 0.4f * globalSpeed, 0.03f, false, 2.8F, 0.06F, ageInTicks / 20, 2);

        // Blinking Animation: tuck the eye planes just inside the head faces to hide them.
        if (!toxodon.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(4.5F, -1.75F, -3.75F);
            this.eye_right.setRotationPoint(-4.5F, -1.75F, -3.75F);
        }

        // Head Tracking Animation
        if (!toxodon.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, head_main);
        }

        // Pitch/Yaw handler
        if (toxodon.isInWater()) {
            this.setRotateAngle(head_main, -0.18203784098300857F, 0.0F, 0.0F);
            if (!toxodon.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(toxodon.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_main, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // Movement Animation
        if (toxodon.canMove()) {
            bob(body_main, 0.8f * globalSpeed, 0.6f * globalDegree, true, f, limbSwingAmount);
            walk(head_main, 0.8f * globalSpeed, 0.15f * globalDegree, true, 0, 0, f, limbSwingAmount);
            walk(leg_front_left, -0.8f * globalSpeed, 1.0f * globalDegree, true, 0F, 1.0f, f, limbSwingAmount);
            walk(leg_front_right, -0.8f * globalSpeed, 1.0f * globalDegree, true, 2F, 1.0f, f, limbSwingAmount);
            walk(leg_back_left, 0.8f * globalSpeed, 1.0f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(leg_back_right, 0.8f * globalSpeed, 1.0f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
        }

        // Sitting / Sleeping: lower the body and fold the legs under. NOTE: leg-fold poses are inherited
        // from the old fork geometry and need in-game re-derivation for this new sculpt (deferred, like the
        // mammoth sleep pose) — walk/idle are relative so they read fine; only these rare poses may look off.
        if (toxodon.sitProgress > 0) {
            this.progressPosition(body_main, toxodon.sitProgress, 0.0F, 12.0F, 0.0F, 40);
            this.progressRotation(head_main, toxodon.sitProgress, (float) Math.toRadians(-20F), 0, 0, 40);
            this.progressRotation(leg_front_left, toxodon.sitProgress, (float) Math.toRadians(-55F), 0, 0, 40);
            this.progressRotation(leg_front_right, toxodon.sitProgress, (float) Math.toRadians(-55F), 0, 0, 40);
            this.progressRotation(leg_back_left, toxodon.sitProgress, (float) Math.toRadians(-70F), (float) Math.toRadians(12F), 0, 40);
            this.progressRotation(leg_back_right, toxodon.sitProgress, (float) Math.toRadians(-70F), (float) Math.toRadians(-12F), 0, 40);
        }
        else if (toxodon.sleepProgress > 0) {
            this.progressPosition(body_main, toxodon.sleepProgress, 0.0F, 12.0F, 0.0F, 40);
            this.progressRotation(leg_front_left, toxodon.sleepProgress, (float) Math.toRadians(-55F), 0, 0, 40);
            this.progressRotation(leg_front_right, toxodon.sleepProgress, (float) Math.toRadians(-55F), 0, 0, 40);
            this.progressRotation(leg_back_left, toxodon.sleepProgress, (float) Math.toRadians(-70F), (float) Math.toRadians(12F), 0, 40);
            this.progressRotation(leg_back_right, toxodon.sleepProgress, (float) Math.toRadians(-70F), (float) Math.toRadians(-12F), 0, 40);
        }
    }
}
