package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityDireWolf;

public class ModelDireWolf extends AdvancedEntityModel<EntityDireWolf> {

    public AdvancedModelBox body_main;
    public AdvancedModelBox head_neck;
    public AdvancedModelBox arm_left_upper;
    public AdvancedModelBox leg_left_upper;
    public AdvancedModelBox tail_1;
    public AdvancedModelBox arm_right_upper;
    public AdvancedModelBox leg_right_upper;
    public AdvancedModelBox head_main;
    public AdvancedModelBox hair;
    public AdvancedModelBox eye_right;
    public AdvancedModelBox eye_left;
    public AdvancedModelBox head_snout;
    public AdvancedModelBox head_jaw;
    public AdvancedModelBox ear_right;
    public AdvancedModelBox ear_left;
    public AdvancedModelBox arm_left_lower;
    public AdvancedModelBox leg_left_lower;
    public AdvancedModelBox arm_right_lower;
    public AdvancedModelBox leg_right_lower;

    private final ModelAnimator animator;
    private float tailX = -0.35F;

    public ModelDireWolf() {
        this.texWidth = 128;
        this.texHeight = 64;

        // Sculpted fresh in Blockbench as a VANILLA-WOLF-shaped dire wolf: blocky head, heavy shoulder
        // ruff (hair), level back, even robust 2-segment legs and a thick bushy tail. Box coords +
        // texOffs transcribed VERBATIM from the modded_entity export (128x64, Y-down flip handled by
        // it). Every part overlaps its neighbour by 0.5 so no two faces are coplanar (coplanar faces
        // z-fight and render as black streaks).
        this.body_main = new AdvancedModelBox(this, 0, 0);
        this.body_main.setRotationPoint(0.0F, 14.0F, 1.0F);
        this.body_main.addBox(-3.5F, -3.5F, -5.5F, 7.0F, 7.0F, 11.0F, 0.0F);

        this.hair = new AdvancedModelBox(this, 36, 0);
        this.hair.setRotationPoint(0.0F, -1.5F, -3.5F);
        this.hair.addBox(-4.0F, -3.5F, -4.0F, 8.0F, 8.0F, 8.0F, 0.0F);

        this.head_neck = new AdvancedModelBox(this, 0, 18);
        this.head_neck.setRotationPoint(0.0F, -2.5F, -5.5F);
        this.head_neck.addBox(-2.5F, -2.0F, -4.0F, 5.0F, 4.5F, 4.5F, 0.0F);

        this.head_main = new AdvancedModelBox(this, 68, 0);
        this.head_main.setRotationPoint(0.0F, -0.5F, -4.5F);
        this.head_main.addBox(-3.5F, -3.5F, -4.5F, 7.0F, 7.0F, 5.5F, 0.0F);

        this.head_snout = new AdvancedModelBox(this, 20, 18);
        this.head_snout.setRotationPoint(0.0F, 0.0F, -4.5F);
        this.head_snout.addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 4.5F, 0.0F);

        this.head_jaw = new AdvancedModelBox(this, 24, 28);
        this.head_jaw.setRotationPoint(0.0F, 1.5F, -4.5F);
        this.head_jaw.addBox(-1.8F, 0.0F, -3.5F, 3.6F, 1.5F, 4.5F, 0.0F);

        this.ear_left = new AdvancedModelBox(this, 41, 28);
        this.ear_left.setRotationPoint(-2.2F, -3.0F, -1.0F);
        this.ear_left.addBox(-1.25F, -2.5F, -0.5F, 2.5F, 3.0F, 1.0F, 0.0F);

        this.ear_right = new AdvancedModelBox(this, 49, 28);
        this.ear_right.setRotationPoint(2.2F, -3.0F, -1.0F);
        this.ear_right.addBox(-1.25F, -2.5F, -0.5F, 2.5F, 3.0F, 1.0F, 0.0F);

        this.eye_left = new AdvancedModelBox(this, 57, 28);
        this.eye_left.setRotationPoint(-2.6F, -1.5F, -4.4F);
        this.eye_left.addBox(-1.0F, -0.5F, -0.5F, 1.5F, 1.5F, 1.0F, 0.0F);

        this.eye_right = new AdvancedModelBox(this, 63, 28);
        this.eye_right.setRotationPoint(2.6F, -1.5F, -4.4F);
        this.eye_right.addBox(-0.5F, -0.5F, -0.5F, 1.5F, 1.5F, 1.0F, 0.0F);

        this.arm_left_upper = new AdvancedModelBox(this, 38, 18);
        this.arm_left_upper.setRotationPoint(-2.3F, 2.5F, -3.0F);
        this.arm_left_upper.addBox(-1.75F, 0.0F, -1.75F, 3.5F, 4.0F, 3.5F, 0.0F);

        this.arm_left_lower = new AdvancedModelBox(this, 98, 18);
        this.arm_left_lower.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.arm_left_lower.addBox(-1.25F, -0.5F, -1.5F, 2.5F, 4.0F, 3.0F, 0.0F);

        this.arm_right_upper = new AdvancedModelBox(this, 53, 18);
        this.arm_right_upper.setRotationPoint(2.3F, 2.5F, -3.0F);
        this.arm_right_upper.addBox(-1.75F, 0.0F, -1.75F, 3.5F, 4.0F, 3.5F, 0.0F);

        this.arm_right_lower = new AdvancedModelBox(this, 110, 18);
        this.arm_right_lower.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.arm_right_lower.addBox(-1.25F, -0.5F, -1.5F, 2.5F, 4.0F, 3.0F, 0.0F);

        this.leg_left_upper = new AdvancedModelBox(this, 68, 18);
        this.leg_left_upper.setRotationPoint(-2.3F, 2.5F, 4.5F);
        this.leg_left_upper.addBox(-1.75F, 0.0F, -1.75F, 3.5F, 4.0F, 3.5F, 0.0F);

        this.leg_left_lower = new AdvancedModelBox(this, 0, 28);
        this.leg_left_lower.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.leg_left_lower.addBox(-1.25F, -0.5F, -1.5F, 2.5F, 4.0F, 3.0F, 0.0F);

        this.leg_right_upper = new AdvancedModelBox(this, 83, 18);
        this.leg_right_upper.setRotationPoint(2.3F, 2.5F, 4.5F);
        this.leg_right_upper.addBox(-1.75F, 0.0F, -1.75F, 3.5F, 4.0F, 3.5F, 0.0F);

        this.leg_right_lower = new AdvancedModelBox(this, 12, 28);
        this.leg_right_lower.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.leg_right_lower.addBox(-1.25F, -0.5F, -1.5F, 2.5F, 4.0F, 3.0F, 0.0F);

        this.tail_1 = new AdvancedModelBox(this, 94, 0);
        this.tail_1.setRotationPoint(0.0F, -2.0F, 5.0F);
        this.tail_1.addBox(-1.5F, -1.0F, -0.5F, 3.0F, 3.0F, 8.0F, 0.0F);
        // Slight resting droop; setupAnim lerps toward defaultRotationX + speed, so the tail lifts as
        // the wolf picks up pace (positive rotateAngleX raises the tail: +Y is down in model space).
        this.setRotateAngle(tail_1, -0.35F, 0.0F, 0.0F);

        this.body_main.addChild(this.hair);
        this.body_main.addChild(this.head_neck);
        this.head_neck.addChild(this.head_main);
        this.head_main.addChild(this.head_snout);
        this.head_main.addChild(this.head_jaw);
        this.head_main.addChild(this.ear_left);
        this.head_main.addChild(this.ear_right);
        this.head_main.addChild(this.eye_left);
        this.head_main.addChild(this.eye_right);
        this.body_main.addChild(this.arm_left_upper);
        this.arm_left_upper.addChild(this.arm_left_lower);
        this.body_main.addChild(this.arm_right_upper);
        this.arm_right_upper.addChild(this.arm_right_lower);
        this.body_main.addChild(this.leg_left_upper);
        this.leg_left_upper.addChild(this.leg_left_lower);
        this.body_main.addChild(this.leg_right_upper);
        this.leg_right_upper.addChild(this.leg_right_lower);
        this.body_main.addChild(this.tail_1);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(this.body_main);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of( body_main, head_neck, arm_left_upper, leg_left_upper, tail_1, arm_right_upper, leg_right_upper,
            head_main, hair, eye_right, eye_left, head_snout, head_jaw, ear_right, ear_left, arm_left_lower,
            leg_left_lower, arm_right_lower, leg_right_lower);
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityDireWolf wolf = (EntityDireWolf) entityIn;
        animator.update(wolf);

        animator.setAnimation(EntityDireWolf.IDLE_TALK);
        animator.startKeyframe(10);
        this.rotate(animator, head_neck, -26.09F, 0, 0);
        this.rotate(animator, head_jaw, 26.09F, 0, 0);
        this.rotate(animator, head_main, -26.09F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(10);

        animator.setAnimation(EntityDireWolf.ATTACK_BITE);
        animator.startKeyframe(5);
        this.rotate(animator, head_main, -10.43F, 15.65F, -20.87F);
        this.rotate(animator, head_jaw, 57.39F, 0, 0);
        this.rotate(animator, head_neck, 44.35F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, head_main, -18.26F, -5.22F, 10.43F);
        this.rotate(animator, head_jaw, 57.39F, 0, 0);
        this.rotate(animator, head_neck, 20.87F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(5);

        animator.setAnimation(EntityDireWolf.ATTACK_POUNCE);
        animator.startKeyframe(12);
        this.rotate(animator, body_main, -18.26F, 0, 0);
        animator.move(body_main, 0, -2, 0);
        this.rotate(animator, head_jaw, 52.17F, 0, 0);
        this.rotate(animator, arm_left_upper, -20.87F, 0, -20.87F);
        this.rotate(animator, arm_left_lower, -60F, 0, 7.83F);
        this.rotate(animator, arm_right_upper, -20.87F, 0, 20.87F);
        this.rotate(animator, arm_right_lower, -60F, 0, -7.83F);
        this.rotate(animator, leg_left_upper, 15.65F, 0, -2.61F);
        animator.move(leg_left_lower, 0, -1.5F, 0);
        this.rotate(animator, leg_right_upper, 15.65F, 0, 2.61F);
        animator.move(leg_right_lower, 0, -1.5F, 0);
        this.rotate(animator, tail_1, -15.65F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(6);
        this.rotate(animator, body_main, 2.61F, 0, 0);
        this.rotate(animator, head_neck, -10.43F, -5.22F, 7.83F);
        this.rotate(animator, head_main, 0, 2.61F, -18.26F);
        this.rotate(animator, head_jaw, 52.17F, 0, 0);
        this.rotate(animator, arm_left_upper, -52.17F, -18.26F, -10.44F);
        this.rotate(animator, arm_left_lower, -60F, 0, 7.83F);
        this.rotate(animator, arm_right_upper, -52.17F, 18.26F, 10.44F);
        this.rotate(animator, arm_right_lower, -60F, 0, -7.83F);
        this.rotate(animator, leg_left_upper, 62.61F, 15.65F, -2.61F);
        this.rotate(animator, leg_right_upper, 62.61F, -15.65F, 2.61F);
        this.rotate(animator, tail_1, -15.65F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(10);
        this.rotate(animator, body_main, 23.48F, 0, 0);
        this.rotate(animator, head_main, 0, 2.61F, -18.26F);
        this.rotate(animator, arm_left_upper, 5.22F, 0, -15.65F);
        this.rotate(animator, arm_left_lower, -60F, 0, 23.48F);
        animator.move(arm_left_upper, 0, 1F, 0);
        this.rotate(animator, arm_right_upper, 5.22F, 0, 15.65F);
        this.rotate(animator, arm_right_lower, -60F, 0, -23.48F);
        animator.move(arm_right_upper, 0, 1F, 0);
        this.rotate(animator, leg_left_upper, 2.61F, -5.22F, -10.43F);
        this.rotate(animator, leg_right_upper, 2.61F, 5.22F, 10.43F);
        this.rotate(animator, tail_1, -15.65F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(6);
        this.rotate(animator, body_main, 7.83F, 0, 0);
        animator.move(body_main, 0, 2, 0);
        this.rotate(animator, head_main, 2.61F, 7.83F, -2.61F);
        this.rotate(animator, arm_left_upper, 39.13F, 0, -15.65F);
        animator.move(arm_left_upper, 0, 3, 0);
        this.rotate(animator, arm_left_lower, -60F, 0, 23.48F);
        this.rotate(animator, arm_right_upper, 39.13F, 0, 15.65F);
        animator.move(arm_right_upper, 0, 3, 0);
        this.rotate(animator, arm_right_lower, -60F, 0, -23.48F);
        this.rotate(animator, leg_left_upper, -5.22F, 0, -2.61F);
        this.rotate(animator, leg_right_upper, -5.22F, 0, 2.61F);
        this.rotate(animator, tail_1, -15.65F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(8);
    }

    public void setupAnim(EntityDireWolf wolf, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(wolf);
        float globalSpeed = 2.8f;
        float globalDegree = 1f;
        limbSwingAmount = Math.min(0.6F, limbSwingAmount * 2);
        limbSwing *= 0.5F;

        //globalSpeed = 1;
        //limbSwing = big_cat.tickCount;
        //limbSwingAmount = 0.5F;

        // Breathing Animation
        final double scaleX = Math.sin(ageInTicks * 1 / 20F);
        final double scaleY = Math.sin(ageInTicks / 16);
        this.body_main.setScale((float) (1F + scaleX * 0.08F), (float) (1F + scaleY * 0.06F), 1.0F);
        bob(body_main, 0.4F * globalSpeed, 0.03F, false, ageInTicks / 20, 2);
        bob(arm_right_upper, 0.4F * globalSpeed, 0.03F, false, -ageInTicks / 20, 2);
        bob(arm_left_upper, 0.4F * globalSpeed, 0.03F, false, -ageInTicks / 20, 2);
        bob(leg_right_upper, 0.4F * globalSpeed, 0.03F, false, -ageInTicks / 20, 2);
        bob(leg_left_upper, 0.4F * globalSpeed, 0.03F, false, -ageInTicks / 20, 2);

        // Blinking Animation - tuck both eye cubes fully inside head_main so they are occluded
        if (!wolf.shouldRenderEyes()) {
            this.eye_right.setRotationPoint(0.6F, -1.5F, -2.0F);
            this.eye_left.setRotationPoint(-0.6F, -1.5F, -2.0F);
        }

        // Head Tracking Animation
        if (!wolf.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, head_neck);
            this.faceTarget(netHeadYaw, headPitch, 3, head_main);
        }

        // Pitch/Yaw handler
        if (wolf.isInWater() && !wolf.isOnGround()) {
            limbSwing = ageInTicks / 3;
            limbSwingAmount = 0.5f;
            this.body_main.rotationPointY += 4; // Model offset to make the wolf "sink" in water (while not drowning)
            this.setRotateAngle(head_neck, -0.18203784098300857F, 0.0F, 0.0F);
            float pitch = Mth.clamp(wolf.getXRot() - 10, -25F, 25.0F);
            this.setRotateAngle(body_main, (float) (pitch * Math.PI / 180F), 0, 0);
        }

        // Movement Animation
        float newZ = Mth.lerp(0.4F, this.tailX, this.tail_1.defaultRotationX + (float)wolf.getCurrentSpeed() * 2);
        this.tail_1.rotateAngleX = newZ;
        this.tailX = newZ;
        if (wolf.canMove()) {
            if (wolf.getCurrentSpeed() > 0.06f || wolf.isAngry()) { // Running animation
                bob(body_main, 0.3F * globalSpeed, 0.5F, false, limbSwing, limbSwingAmount);
                walk(body_main, 0.3f * globalSpeed, 0.5f * globalDegree, true, 0.5F, 0f, limbSwing, limbSwingAmount);
                walk(head_neck, 0.3f * globalSpeed, -0.5f * globalDegree, true, 0.5F, 0f, limbSwing, limbSwingAmount);
                bob(arm_right_upper, 0.3F * globalSpeed, 0.8F, false, limbSwing, limbSwingAmount);
                walk(arm_right_upper, 0.3f * globalSpeed, globalDegree, true, 0F, 0f, limbSwing, limbSwingAmount);
                walk(arm_right_lower, 0.3f * globalSpeed, 0.6f * globalDegree, true, 0.2F, 0.2f, limbSwing, limbSwingAmount);
                bob(arm_left_upper, 0.3F * globalSpeed, 0.8F, false, limbSwing, limbSwingAmount);
                walk(arm_left_upper, 0.3f * globalSpeed, globalDegree, true, 0.6F, 0f, limbSwing, limbSwingAmount);
                walk(arm_left_lower, 0.3f * globalSpeed, 0.6f * globalDegree, true, 0.8F, 0.2f, limbSwing, limbSwingAmount);
                bob(leg_right_upper, 0.3F * globalSpeed, 0.8F, false, limbSwing, limbSwingAmount);
                walk(leg_right_upper, 0.3f * globalSpeed, globalDegree, true, 1.4F, 0f, limbSwing, limbSwingAmount);
                walk(leg_right_lower, 0.3f * globalSpeed, 0.6f * globalDegree, true, 1.6F, 0.2f, limbSwing, limbSwingAmount);
                bob(leg_left_upper, 0.3F * globalSpeed, 0.8F, false, limbSwing, limbSwingAmount);
                walk(leg_left_upper, 0.3f * globalSpeed, globalDegree, true, 2F, 0f, limbSwing, limbSwingAmount);
                walk(leg_left_lower, 0.3f * globalSpeed, 0.6f * globalDegree, true, 2.2F, 0.2f, limbSwing, limbSwingAmount);
            } else { // Walking Animation
                bob(body_main, 0.5F * globalSpeed, 0.2F, false, limbSwing, limbSwingAmount);
                walk(body_main, 0.5f * globalSpeed, 0.2f * globalDegree, true, 0.5F, 0f, limbSwing, limbSwingAmount);
                walk(head_neck, 0.5f * globalSpeed, -0.2f * globalDegree, true, 0.5F, 0f, limbSwing, limbSwingAmount);

                walk(arm_right_upper, 0.5f * globalSpeed, 0.6f * globalDegree, false, 2F, 0f, limbSwing, limbSwingAmount);
                walk(arm_right_lower, 0.5f * globalSpeed, 0.6f * globalDegree, false, 0F, -0.8f, limbSwing, limbSwingAmount * 1.2f);
                walk(arm_left_upper, 0.5f * globalSpeed, 0.6f * globalDegree, false, 5.0F, 0f, limbSwing, limbSwingAmount);
                walk(arm_left_lower, 0.5f * globalSpeed, 0.6f * globalDegree, false, 3F, -0.8f, limbSwing, limbSwingAmount * 1.2f);
                bob(leg_right_upper, 0.5F * globalSpeed, 0.6F, false, limbSwing, limbSwingAmount);
                walk(leg_right_upper, 0.5f * globalSpeed, 0.8F * globalDegree, true, 1F, 0f, limbSwing, limbSwingAmount);
                walk(leg_right_lower, 0.5f * globalSpeed, 0.4f * globalDegree, true, 1.2F, 0.2f, limbSwing, limbSwingAmount);
                bob(leg_left_upper, 0.5F * globalSpeed, 0.6F, false, limbSwing, limbSwingAmount);
                walk(leg_left_upper, 0.5f * globalSpeed, 0.8F * globalDegree, true, 3.4F, 0f, limbSwing, limbSwingAmount);
                walk(leg_left_lower, 0.5f * globalSpeed, 0.4f * globalDegree, true, 3.6F, 0.2f, limbSwing, limbSwingAmount);
            }
        }

        // Sitting Animation (positions follow the new rig: body_main sits at y 14, left parts are -X)
        if (wolf.sitProgress > 0) {
            this.progressPosition(body_main, wolf.sitProgress, 0.0F, 20.4F, 1.0F, 40);
            this.progressPosition(leg_left_upper, wolf.sitProgress, -2.3F, 2.7F, 6.1F, 40);
            this.progressPosition(leg_right_upper, wolf.sitProgress, 2.3F, 2.7F, 6.1F, 40);
            this.progressRotation(body_main, wolf.sitProgress, 0.0F, 0.0F, 0.0F, 40);
            this.progressRotation(head_neck, wolf.sitProgress, -0.8F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_left_upper, wolf.sitProgress, -0.27314402793711257F, -0.0F, -0.045553093477052F, 40);
            this.progressRotation(leg_left_lower, wolf.sitProgress, -1.2292353921796064F, -0.22759093446006054F, 0.045553093477052F, 40);
            this.progressRotation(arm_left_upper, wolf.sitProgress, 0.27314402793711207F, -6.200655107570901E-17F, -0.24361070658773F, 40);
            this.progressRotation(arm_left_lower, wolf.sitProgress, -1.8668041679331349F, 0.0F, 0.10803588069844901F, 40);
            this.progressRotation(leg_right_upper, wolf.sitProgress, -0.27314402793711257F, -0.0F, 0.045553093477052F, 40);
            this.progressRotation(leg_right_lower, wolf.sitProgress, -1.2292353921796064F, 0.22759093446006054F, -0.045553093477052F, 40);
            this.progressRotation(arm_right_upper, wolf.sitProgress, 0.27314402793711207F, 6.200655107570901E-17F, 0.24361070658773F, 40);
            this.progressRotation(arm_right_lower, wolf.sitProgress, -1.8668041679331349F, 0.0F, -0.10803588069844901F, 40);
            this.progressRotation(tail_1, wolf.sitProgress, -1.0471975511965976F, 0.0F, 0.0F, 40);
        }

        // Sleeping Animation
        if (wolf.sleepProgress > 0) {
            this.progressPosition(body_main, wolf.sleepProgress, -1.0F, 17.9F, 0.0F, 40);
            this.progressRotation(body_main, wolf.sleepProgress, 0, 0.0F, -1.50255395F, 40);
            this.progressRotation(leg_right_upper, wolf.sleepProgress, -0.500909495F , -0.0F, 0.045553093477052F, 40);
            this.progressRotation(leg_left_lower, wolf.sleepProgress, -0.13665928F, 0, 0.77405352F, 40);
            this.progressRotation(head_neck, wolf.sleepProgress, 0.27314402793711207F, 0.22759093446006054F, 0.0F, 40);
            this.progressRotation(head_main, wolf.sleepProgress, 0.4553564F, 0, 0.0F, 40);
            this.progressRotation(arm_right_upper, wolf.sleepProgress, -0.27314402793711207F, 0, 0.09110619F, 40);
            this.progressRotation(arm_left_lower, wolf.sleepProgress, -0.5009095F, -0.09110619F, 1.0472F, 40);
        }
    }
}
