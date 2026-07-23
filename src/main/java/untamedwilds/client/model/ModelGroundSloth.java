package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityGroundSloth;

public class ModelGroundSloth extends AdvancedEntityModel<EntityGroundSloth> {

    private final AdvancedModelBox body_hips;
    private final AdvancedModelBox body_arch;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox snout;
    private final AdvancedModelBox jaw;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox arm_left_1;
    private final AdvancedModelBox arm_left_2;
    private final AdvancedModelBox hand_left;
    private final AdvancedModelBox claw_left_a;
    private final AdvancedModelBox claw_left_b;
    private final AdvancedModelBox claw_left_c;
    private final AdvancedModelBox arm_right_1;
    private final AdvancedModelBox arm_right_2;
    private final AdvancedModelBox hand_right;
    private final AdvancedModelBox claw_right_a;
    private final AdvancedModelBox claw_right_b;
    private final AdvancedModelBox claw_right_c;
    private final AdvancedModelBox leg_left_thigh;
    private final AdvancedModelBox leg_left_shank;
    private final AdvancedModelBox foot_left;
    private final AdvancedModelBox leg_right_thigh;
    private final AdvancedModelBox leg_right_shank;
    private final AdvancedModelBox foot_right;
    private final AdvancedModelBox tail_1;
    private final AdvancedModelBox tail_2;
    private final AdvancedModelBox tail_3;

    private final ModelAnimator animator;

    public ModelGroundSloth() {
        this.texWidth = 256;
        this.texHeight = 256;

        // Full Blockbench rebuild (32 boxes) of a Megatherium-type ground sloth. The torso is three
        // NESTED angled boxes, not a stack of flat-topped slabs: body_arch (+0.2793 rad) is deliberately
        // tall so its lower half is buried inside body_hips and only the top emerges, which makes the
        // hip-peaked arch read as one continuous topline falling to low shoulders -- the exact inverse of
        // the mammoth, which peaks at the shoulder. An earlier pass bolted haunch/shoulder fillets and a
        // fur overlay on top of this and it read as a pile of intersecting slabs; the shag is carried by
        // the SKIN instead.
        // Diagnostics: THREE recurved claws per hand on a knuckle-walking pad, pillar hind limbs with the
        // foot rolled onto its outer edge (Megatherium could not plant the pes flat because of the
        // claws), and a heavy 3-segment tail used as a tripod prop.
        // All coords/rotations/texOffs transcribed VERBATIM from the modded_entity export (CLAUDE.md
        // lesson 1 -- the Y-up/Y-down flip does not generalise and must never be re-derived by hand).
        this.body_hips = new AdvancedModelBox(this, 0, 0);
        this.body_hips.setRotationPoint(0.0F, 0.0F, 9.0F);
        this.body_hips.addBox(-11.5F, -7.0F, -9.0F, 23.0F, 18.0F, 20.0F, 0.0F);

        this.body_arch = new AdvancedModelBox(this, 86, 0);
        this.body_arch.setRotationPoint(0.0F, -7.0F, -1.0F);
        this.body_arch.addBox(-10.5F, -2.0F, -22.0F, 21.0F, 11.0F, 24.0F, 0.0F);
        this.setRotateAngle(body_arch, 0.2793F, 0.0F, 0.0F);

        this.body_chest = new AdvancedModelBox(this, 176, 0);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -9.0F);
        this.body_chest.addBox(-9.5F, -3.0F, -19.0F, 19.0F, 12.0F, 21.0F, 0.0F);
        this.setRotateAngle(body_chest, 0.2094F, 0.0F, 0.0F);

        this.neck = new AdvancedModelBox(this, 60, 64);
        this.neck.setRotationPoint(0.0F, -3.0F, -16.0F);
        this.neck.addBox(-6.5F, -2.5F, -7.0F, 13.0F, 8.5F, 9.0F, 0.0F);
        this.setRotateAngle(neck, 0.1047F, 0.0F, 0.0F);

        this.head = new AdvancedModelBox(this, 172, 64);
        this.head.setRotationPoint(0.0F, 1.0F, -6.0F);
        this.head.addBox(-5.5F, -3.0F, -7.0F, 11.0F, 9.0F, 8.0F, 0.0F);
        this.setRotateAngle(head, 0.0873F, 0.0F, 0.0F);

        this.snout = new AdvancedModelBox(this, 24, 100);
        this.snout.setRotationPoint(0.0F, 1.5F, -6.0F);
        this.snout.addBox(-4.0F, -2.0F, -4.0F, 8.0F, 5.5F, 5.0F, 0.0F);
        this.setRotateAngle(snout, 0.0349F, 0.0F, 0.0F);

        this.jaw = new AdvancedModelBox(this, 50, 100);
        this.jaw.setRotationPoint(0.0F, 4.5F, -5.0F);
        this.jaw.addBox(-4.0F, -0.5F, -4.0F, 8.0F, 4.5F, 5.0F, 0.0F);
        this.setRotateAngle(jaw, -0.0524F, 0.0F, 0.0F);

        this.ear_left = new AdvancedModelBox(this, 76, 100);
        this.ear_left.setRotationPoint(5.5F, -1.0F, -2.0F);
        this.ear_left.addBox(-0.2F, -2.5F, -1.5F, 1.0F, 3.5F, 3.0F, 0.0F);
        this.setRotateAngle(ear_left, 0.0F, -0.2094F, 0.0F);

        this.ear_right = new AdvancedModelBox(this, 84, 100);
        this.ear_right.setRotationPoint(-5.5F, -1.0F, -2.0F);
        this.ear_right.addBox(-0.8F, -2.5F, -1.5F, 1.0F, 3.5F, 3.0F, 0.0F);
        this.setRotateAngle(ear_right, 0.0F, 0.2094F, 0.0F);

        // Eye planes sit 0.35 clear of the skull face (|x| 5.5) so they cannot z-fight the cheek, and
        // are 3x3 rather than 2x2 -- at 2px there is no room for a pupil to read against dark fur.
        this.eye_left = new AdvancedModelBox(this, 92, 100);
        this.eye_left.setRotationPoint(5.85F, -0.5F, -6.0F);
        this.eye_left.addBox(0.0F, -1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 0.0F);

        this.eye_right = new AdvancedModelBox(this, 98, 100);
        this.eye_right.setRotationPoint(-5.85F, -0.5F, -6.0F);
        this.eye_right.addBox(0.0F, -1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 0.0F);

        this.arm_left_1 = new AdvancedModelBox(this, 104, 64);
        this.arm_left_1.setRotationPoint(8.0F, 0.0F, -12.0F);
        this.arm_left_1.addBox(-3.5F, -0.5F, -5.0F, 7.0F, 7.35F, 10.0F, 0.0F);
        this.setRotateAngle(arm_left_1, -0.2094F, 0.0F, -0.0873F);

        this.arm_left_2 = new AdvancedModelBox(this, 0, 64);
        this.arm_left_2.setRotationPoint(0.0F, 6.85F, 0.0F);
        this.arm_left_2.addBox(-3.0F, -0.5F, -4.0F, 6.0F, 9.8F, 9.0F, 0.0F);
        this.setRotateAngle(arm_left_2, 0.3142F, 0.0F, 0.0F);

        this.hand_left = new AdvancedModelBox(this, 66, 83);
        this.hand_left.setRotationPoint(0.0F, 9.3F, 0.0F);
        this.hand_left.addBox(-2.5F, -0.5F, -3.0F, 5.0F, 5.7F, 7.0F, 0.0F);
        this.setRotateAngle(hand_left, -0.1222F, 0.0F, 0.0F);

        this.claw_left_a = new AdvancedModelBox(this, 114, 83);
        this.claw_left_a.setRotationPoint(1.7F, -0.8F, -2.0F);
        this.claw_left_a.addBox(-1.3F, -0.5F, -8.0F, 2.6F, 2.7F, 8.5F, 0.0F);
        this.setRotateAngle(claw_left_a, 0.2443F, 0.0F, -0.1222F);

        this.claw_left_b = new AdvancedModelBox(this, 138, 83);
        this.claw_left_b.setRotationPoint(0.0F, -0.8F, -2.0F);
        this.claw_left_b.addBox(-1.3F, -0.5F, -8.5F, 2.6F, 3.0F, 9.0F, 0.0F);
        this.setRotateAngle(claw_left_b, 0.2967F, 0.0F, 0.0F);

        this.claw_left_c = new AdvancedModelBox(this, 162, 83);
        this.claw_left_c.setRotationPoint(-1.7F, -0.8F, -2.0F);
        this.claw_left_c.addBox(-1.3F, -0.5F, -8.0F, 2.6F, 2.7F, 8.5F, 0.0F);
        this.setRotateAngle(claw_left_c, 0.2443F, 0.0F, 0.1222F);

        this.arm_right_1 = new AdvancedModelBox(this, 138, 64);
        this.arm_right_1.setRotationPoint(-8.0F, 0.0F, -12.0F);
        this.arm_right_1.addBox(-3.5F, -0.5F, -5.0F, 7.0F, 7.35F, 10.0F, 0.0F);
        this.setRotateAngle(arm_right_1, -0.2094F, 0.0F, 0.0873F);

        this.arm_right_2 = new AdvancedModelBox(this, 30, 64);
        this.arm_right_2.setRotationPoint(0.0F, 6.85F, 0.0F);
        this.arm_right_2.addBox(-3.0F, -0.5F, -4.0F, 6.0F, 9.8F, 9.0F, 0.0F);
        this.setRotateAngle(arm_right_2, 0.3142F, 0.0F, 0.0F);

        this.hand_right = new AdvancedModelBox(this, 90, 83);
        this.hand_right.setRotationPoint(0.0F, 9.3F, 0.0F);
        this.hand_right.addBox(-2.5F, -0.5F, -3.0F, 5.0F, 5.7F, 7.0F, 0.0F);
        this.setRotateAngle(hand_right, -0.1222F, 0.0F, 0.0F);

        this.claw_right_a = new AdvancedModelBox(this, 186, 83);
        this.claw_right_a.setRotationPoint(-1.7F, -0.8F, -2.0F);
        this.claw_right_a.addBox(-1.3F, -0.5F, -8.0F, 2.6F, 2.7F, 8.5F, 0.0F);
        this.setRotateAngle(claw_right_a, 0.2443F, 0.0F, 0.1222F);

        this.claw_right_b = new AdvancedModelBox(this, 210, 83);
        this.claw_right_b.setRotationPoint(0.0F, -0.8F, -2.0F);
        this.claw_right_b.addBox(-1.3F, -0.5F, -8.5F, 2.6F, 3.0F, 9.0F, 0.0F);
        this.setRotateAngle(claw_right_b, 0.2967F, 0.0F, 0.0F);

        this.claw_right_c = new AdvancedModelBox(this, 0, 100);
        this.claw_right_c.setRotationPoint(1.7F, -0.8F, -2.0F);
        this.claw_right_c.addBox(-1.3F, -0.5F, -8.0F, 2.6F, 2.7F, 8.5F, 0.0F);
        this.setRotateAngle(claw_right_c, 0.2443F, 0.0F, -0.1222F);

        this.leg_left_thigh = new AdvancedModelBox(this, 0, 38);
        this.leg_left_thigh.setRotationPoint(8.0F, 0.0F, 1.0F);
        this.leg_left_thigh.addBox(-4.0F, -1.0F, -7.0F, 8.0F, 12.0F, 14.0F, 0.0F);
        this.setRotateAngle(leg_left_thigh, -0.1396F, 0.0F, -0.0698F);

        this.leg_left_shank = new AdvancedModelBox(this, 132, 38);
        this.leg_left_shank.setRotationPoint(0.0F, 11.0F, 0.0F);
        this.leg_left_shank.addBox(-3.5F, -0.5F, -6.0F, 7.0F, 8.0F, 11.0F, 0.0F);
        this.setRotateAngle(leg_left_shank, 0.2443F, 0.0F, 0.0F);

        this.foot_left = new AdvancedModelBox(this, 210, 64);
        this.foot_left.setRotationPoint(0.0F, 7.5F, 0.0F);
        this.foot_left.addBox(-3.5F, -0.5F, -9.0F, 7.0F, 5.0F, 12.0F, 0.0F);
        this.setRotateAngle(foot_left, 0.0F, 0.2443F, 0.2793F);

        this.leg_right_thigh = new AdvancedModelBox(this, 44, 38);
        this.leg_right_thigh.setRotationPoint(-8.0F, 0.0F, 1.0F);
        this.leg_right_thigh.addBox(-4.0F, -1.0F, -7.0F, 8.0F, 12.0F, 14.0F, 0.0F);
        this.setRotateAngle(leg_right_thigh, -0.1396F, 0.0F, 0.0698F);

        this.leg_right_shank = new AdvancedModelBox(this, 168, 38);
        this.leg_right_shank.setRotationPoint(0.0F, 11.0F, 0.0F);
        this.leg_right_shank.addBox(-3.5F, -0.5F, -6.0F, 7.0F, 8.0F, 11.0F, 0.0F);
        this.setRotateAngle(leg_right_shank, 0.2443F, 0.0F, 0.0F);

        this.foot_right = new AdvancedModelBox(this, 0, 83);
        this.foot_right.setRotationPoint(0.0F, 7.5F, 0.0F);
        this.foot_right.addBox(-3.5F, -0.5F, -9.0F, 7.0F, 5.0F, 12.0F, 0.0F);
        this.setRotateAngle(foot_right, 0.0F, -0.2443F, -0.2793F);

        this.tail_1 = new AdvancedModelBox(this, 88, 38);
        this.tail_1.setRotationPoint(0.0F, -2.0F, 9.0F);
        this.tail_1.addBox(-6.5F, -1.0F, -2.0F, 13.0F, 13.0F, 9.0F, 0.0F);
        this.setRotateAngle(tail_1, -0.2793F, 0.0F, 0.0F);

        this.tail_2 = new AdvancedModelBox(this, 204, 38);
        this.tail_2.setRotationPoint(0.0F, 2.0F, 6.0F);
        this.tail_2.addBox(-5.0F, -1.0F, -1.0F, 10.0F, 11.0F, 8.0F, 0.0F);
        this.setRotateAngle(tail_2, -0.2618F, 0.0F, 0.0F);

        this.tail_3 = new AdvancedModelBox(this, 38, 83);
        this.tail_3.setRotationPoint(0.0F, 1.5F, 6.0F);
        this.tail_3.addBox(-3.5F, -0.5F, -1.0F, 7.0F, 8.0F, 7.0F, 0.0F);
        this.setRotateAngle(tail_3, -0.2618F, 0.0F, 0.0F);

        this.body_hips.addChild(this.body_arch);
        this.body_hips.addChild(this.body_chest);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.snout);
        this.head.addChild(this.jaw);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.ear_right);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.eye_right);
        this.body_chest.addChild(this.arm_left_1);
        this.arm_left_1.addChild(this.arm_left_2);
        this.arm_left_2.addChild(this.hand_left);
        this.hand_left.addChild(this.claw_left_a);
        this.hand_left.addChild(this.claw_left_b);
        this.hand_left.addChild(this.claw_left_c);
        this.body_chest.addChild(this.arm_right_1);
        this.arm_right_1.addChild(this.arm_right_2);
        this.arm_right_2.addChild(this.hand_right);
        this.hand_right.addChild(this.claw_right_a);
        this.hand_right.addChild(this.claw_right_b);
        this.hand_right.addChild(this.claw_right_c);
        this.body_hips.addChild(this.leg_left_thigh);
        this.leg_left_thigh.addChild(this.leg_left_shank);
        this.leg_left_shank.addChild(this.foot_left);
        this.body_hips.addChild(this.leg_right_thigh);
        this.leg_right_thigh.addChild(this.leg_right_shank);
        this.leg_right_shank.addChild(this.foot_right);
        this.body_hips.addChild(this.tail_1);
        this.tail_1.addChild(this.tail_2);
        this.tail_2.addChild(this.tail_3);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_hips);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(
            body_hips, body_arch, body_chest, neck, head, snout, jaw,
            ear_left, ear_right, eye_left, eye_right,
            arm_left_1, arm_left_2, hand_left, claw_left_a, claw_left_b, claw_left_c,
            arm_right_1, arm_right_2, hand_right, claw_right_a, claw_right_b, claw_right_c,
            leg_left_thigh, leg_left_shank, foot_left,
            leg_right_thigh, leg_right_shank, foot_right,
            tail_1, tail_2, tail_3
        );
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityGroundSloth sloth = (EntityGroundSloth) entityIn;
        animator.update(sloth);

        // Threat: rear back on the hind limbs and tail, both clawed forelimbs raised wide.
        animator.setAnimation(EntityGroundSloth.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_chest, -20F, 0, 0);
            this.rotate(animator, neck, -10F, 0, 7.8F);
            animator.move(arm_left_1, 0, -1.0F, 0);
            this.rotate(animator, arm_left_1, -55F, 0, -12F);
            this.rotate(animator, arm_left_2, -30F, 0, 0);
            animator.move(arm_right_1, 0, -1.0F, 0);
            this.rotate(animator, arm_right_1, -48F, 0, 12F);
            this.rotate(animator, arm_right_2, -26F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_chest, -10F, 0, 0);
            this.rotate(animator, neck, -4F, 0, -7.8F);
            animator.move(arm_left_1, 0, 0.5F, 0);
            this.rotate(animator, arm_left_1, -28F, 0, 6F);
            this.rotate(animator, arm_left_2, -14F, 0, 0);
            animator.move(arm_right_1, 0, 0.5F, 0);
            this.rotate(animator, arm_right_1, -32F, 0, -6F);
            this.rotate(animator, arm_right_2, -16F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // Gore: a raking downward swipe with the claws -- the sloth's actual weapon.
        animator.setAnimation(EntityGroundSloth.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, body_chest, -14F, 0, 0);
        this.rotate(animator, arm_right_1, -70F, 0, 18F);
        this.rotate(animator, arm_right_2, -35F, 0, 0);
        this.rotate(animator, claw_right_a, -25F, 0, 0);
        this.rotate(animator, claw_right_b, -25F, 0, 0);
        this.rotate(animator, claw_right_c, -25F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, body_chest, 8F, 0, 0);
        this.rotate(animator, arm_right_1, 40F, 0, -20F);
        this.rotate(animator, arm_right_2, 20F, 0, 0);
        this.rotate(animator, claw_right_a, 20F, 0, 0);
        this.rotate(animator, claw_right_b, 20F, 0, 0);
        this.rotate(animator, claw_right_c, 20F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityGroundSloth sloth, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(sloth);
        float globalSpeed = 0.9f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // Idle: slow breathing and a heavy tail sway. Ground sloths are ponderous, so everything here
        // runs slower than the bison/rhino baseline.
        this.body_hips.setScale((float) (1.0F + Math.sin(ageInTicks / 22) * 0.05F), (float) (1.0F + Math.sin(ageInTicks / 18) * 0.05F), 1.0F);
        this.body_chest.setScale((float) (1.0F + Math.sin(ageInTicks / 22) * 0.05F), (float) (1.0F + Math.sin(ageInTicks / 18) * 0.05F), 1.0F);
        bob(body_hips, 0.32F * globalSpeed, 0.1F, false, ageInTicks / 22, 2);
        bob(arm_left_1, 0.32F * globalSpeed, 0.1F, false, -ageInTicks / 22, 2);
        bob(arm_right_1, 0.32F * globalSpeed, 0.1F, false, -ageInTicks / 22, 2);
        bob(leg_left_thigh, 0.32F * globalSpeed, 0.1F, false, -ageInTicks / 22, 2);
        bob(leg_right_thigh, 0.32F * globalSpeed, 0.1F, false, -ageInTicks / 22, 2);
        walk(neck, 0.32f * globalSpeed, 0.03f, false, 2.8F, 0.05F, ageInTicks / 22, 2);
        this.swing(tail_1, 0.08F, 0.10F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_2, 0.08F, 0.14F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail_3, 0.08F, 0.18F, false, 2F, 0F, ageInTicks, 1);

        // Blinking: bury the eye planes inside the skull (|x| 5.85 -> 4.6) so they stop rendering.
        if (!sloth.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(4.6F, -0.5F, -6.0F);
            this.eye_right.setRotationPoint(-4.6F, -0.5F, -6.0F);
        }

        // Head Tracking
        if (!sloth.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 3, head);
        }

        // Pitch/Yaw handler
        if (sloth.isInWater()) {
            this.setRotateAngle(head, -0.18203784098300857F, 0.0F, 0.0F);
            if (!sloth.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(sloth.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_hips, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // Walk: three-segment limbs, upper leading and lower following a step out of phase so the
        // elbow/hock flexes through the stride. Amplitude is low and speed slow -- a ground sloth
        // shuffles on the knuckles of its forefeet, it does not stride.
        if (sloth.canMove()) {
            bob(body_hips, 0.7f * globalSpeed, 0.4f * globalDegree, true, f, limbSwingAmount);
            walk(neck, 0.7f * globalSpeed, 0.16f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head, 0.7f * globalSpeed, 0.10f * globalDegree, true, 0, 0, f, limbSwingAmount);

            walk(arm_right_1, -0.7f * globalSpeed, 0.85f * globalDegree, true, 0F, 0.8f, f, limbSwingAmount);
            walk(arm_right_2, -0.7f * globalSpeed, 0.85f * globalDegree, false, -1F, 0.8f, f, limbSwingAmount * 1.2f);
            walk(hand_right, -0.7f * globalSpeed, 0.45f * globalDegree, true, -2F, 0, f, limbSwingAmount);
            walk(arm_left_1, -0.7f * globalSpeed, 0.85f * globalDegree, true, 2F, 0.8f, f, limbSwingAmount);
            walk(arm_left_2, -0.7f * globalSpeed, 0.85f * globalDegree, false, 1F, 0.8f, f, limbSwingAmount * 1.2f);
            walk(hand_left, -0.7f * globalSpeed, 0.45f * globalDegree, true, 0F, 0, f, limbSwingAmount);

            walk(leg_right_thigh, 0.7f * globalSpeed, 0.85f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(leg_right_shank, 0.7f * globalSpeed, 0.85f * globalDegree, true, 1.8F, 0, f, limbSwingAmount);
            walk(foot_right, 0.7f * globalSpeed, 0.40f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(leg_left_thigh, 0.7f * globalSpeed, 0.85f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(leg_left_shank, 0.7f * globalSpeed, 0.85f * globalDegree, true, -0.2F, 0, f, limbSwingAmount);
            walk(foot_left, 0.7f * globalSpeed, 0.40f * globalDegree, false, -1.2F, 0, f, limbSwingAmount);

            swing(tail_1, 0.7f * globalSpeed, 0.28f * globalDegree, false, 0F, 0, f, limbSwingAmount);
            swing(tail_2, 0.7f * globalSpeed, 0.34f * globalDegree, false, -1F, 0, f, limbSwingAmount);
            swing(tail_3, 0.7f * globalSpeed, 0.40f * globalDegree, false, -2F, 0, f, limbSwingAmount);
        }

        // Rest / sleep: one pose for both. The body sinks 11 units so the belly settles just off the
        // ground, each limb folds against itself, and the tail flattens out behind. The fold angles were
        // solved against the actual box geometry -- notably the claws need +104/+108 deg, because once
        // the hand folds back they otherwise swing 6.3 units THROUGH the floor, and the tail's standing
        // 16 deg droop has to flatten to 4 deg or it clips once the body drops.
        if (sloth.sitProgress > 0) {
            applyRestingPose(sloth.sitProgress);
        }
        else if (sloth.sleepProgress > 0) {
            applyRestingPose(sloth.sleepProgress);
        }
    }

    private void applyRestingPose(float progress) {
        this.progressPosition(body_hips, progress, 0.0F, 11.0F, 9.0F, 40);

        this.progressRotation(arm_left_1, progress, (float) Math.toRadians(-20F), 0, (float) Math.toRadians(-5F), 40);
        this.progressRotation(arm_left_2, progress, (float) Math.toRadians(100F), 0, 0, 40);
        this.progressRotation(hand_left, progress, (float) Math.toRadians(-10F), 0, 0, 40);
        this.progressRotation(claw_left_a, progress, (float) Math.toRadians(-104F), 0, (float) Math.toRadians(-7F), 40);
        this.progressRotation(claw_left_b, progress, (float) Math.toRadians(-108F), 0, 0, 40);
        this.progressRotation(claw_left_c, progress, (float) Math.toRadians(-104F), 0, (float) Math.toRadians(7F), 40);
        this.progressRotation(arm_right_1, progress, (float) Math.toRadians(-20F), 0, (float) Math.toRadians(5F), 40);
        this.progressRotation(arm_right_2, progress, (float) Math.toRadians(100F), 0, 0, 40);
        this.progressRotation(hand_right, progress, (float) Math.toRadians(-10F), 0, 0, 40);
        this.progressRotation(claw_right_a, progress, (float) Math.toRadians(-104F), 0, (float) Math.toRadians(7F), 40);
        this.progressRotation(claw_right_b, progress, (float) Math.toRadians(-108F), 0, 0, 40);
        this.progressRotation(claw_right_c, progress, (float) Math.toRadians(-104F), 0, (float) Math.toRadians(-7F), 40);

        this.progressRotation(leg_left_thigh, progress, (float) Math.toRadians(40F), 0, (float) Math.toRadians(-4F), 40);
        this.progressRotation(leg_left_shank, progress, (float) Math.toRadians(-130F), 0, 0, 40);
        this.progressRotation(foot_left, progress, (float) Math.toRadians(4F), (float) Math.toRadians(14F), (float) Math.toRadians(16F), 40);
        this.progressRotation(leg_right_thigh, progress, (float) Math.toRadians(40F), 0, (float) Math.toRadians(4F), 40);
        this.progressRotation(leg_right_shank, progress, (float) Math.toRadians(-130F), 0, 0, 40);
        this.progressRotation(foot_right, progress, (float) Math.toRadians(4F), (float) Math.toRadians(-14F), (float) Math.toRadians(-16F), 40);

        this.progressRotation(neck, progress, (float) Math.toRadians(-4F), 0, 0, 40);
        this.progressRotation(head, progress, (float) Math.toRadians(-2F), 0, 0, 40);

        this.progressRotation(tail_1, progress, (float) Math.toRadians(-4F), 0, 0, 40);
        this.progressRotation(tail_2, progress, (float) Math.toRadians(-4F), 0, 0, 40);
        this.progressRotation(tail_3, progress, (float) Math.toRadians(-4F), 0, 0, 40);
    }
}
