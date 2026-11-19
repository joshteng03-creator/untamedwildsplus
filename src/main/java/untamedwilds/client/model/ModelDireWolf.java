package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.mammal.EntityDireWolf;

public class ModelDireWolf extends AdvancedEntityModel<EntityDireWolf> {

    /** Measured distance from the foreleg pivot to the bottom of the paw, used to lift the
     *  body when legLength stretches the limbs so the paws still land on the ground. */
    private static final float LEG_DROP = 13.18F;
    public AdvancedModelBox body_main;
    public AdvancedModelBox body_chest;
    public AdvancedModelBox arm_left_upper;
    public AdvancedModelBox arm_left_lower;
    public AdvancedModelBox arm_left_foot;
    public AdvancedModelBox arm_left_paw;
    public AdvancedModelBox arm_right_upper;
    public AdvancedModelBox arm_right_lower;
    public AdvancedModelBox arm_right_foot;
    public AdvancedModelBox arm_right_paw;
    public AdvancedModelBox body_loin;
    public AdvancedModelBox body_croup;
    public AdvancedModelBox tail_1;
    public AdvancedModelBox tail_2;
    public AdvancedModelBox tail_3;
    public AdvancedModelBox leg_left_upper;
    public AdvancedModelBox leg_left_lower;
    public AdvancedModelBox leg_left_foot;
    public AdvancedModelBox leg_left_paw;
    public AdvancedModelBox leg_right_upper;
    public AdvancedModelBox leg_right_lower;
    public AdvancedModelBox leg_right_foot;
    public AdvancedModelBox leg_right_paw;
    public AdvancedModelBox hair;
    public AdvancedModelBox head_neck;
    public AdvancedModelBox head_main;
    public AdvancedModelBox head_face;
    public AdvancedModelBox head_snout;
    public AdvancedModelBox nose;
    public AdvancedModelBox head_jaw;
    public AdvancedModelBox cheek_left;
    public AdvancedModelBox cheek_right;
    public AdvancedModelBox ear_left;
    public AdvancedModelBox ear_left_tip;
    public AdvancedModelBox ear_right;
    public AdvancedModelBox ear_right_tip;
    public AdvancedModelBox eye_left;
    public AdvancedModelBox eye_right;
    public AdvancedModelBox neck_ruff;

    private final ModelAnimator animator;
    private float tailX = -0.35F;

    public ModelDireWolf() {
        this.texWidth = 128;
        this.texHeight = 128;

        // Sculpted from scratch in Blockbench against scripts/dire_wolf_spec.py, which
        // solves the joint chain, checks ground contact, and sweeps for coincident faces
        // BEFORE anything is built. Every literal below is transcribed VERBATIM from the
        // modded_entity export -- the Blockbench viewport is Y-up and MC is Y-down, and
        // the export is the only thing that performs that flip correctly.
        //
        // The old 19-box rig read derpy for measurable reasons, all fixed here: a 7x7x5.5
        // CUBE skull (now 5.2 wide over a 9.2 long head = 0.56, the real canid ratio), an
        // 8x8x8 ruff wider than the torso it sat on, a 4x4 square muzzle, two equal 4u leg
        // stumps identical front and back (now four segments each with a real hock), a
        // single 7x7x11 torso slab (now chest + tuck-up + croup), 1u paper ears and a
        // straight tail board.

        this.body_main = new AdvancedModelBox(this, 38, 0);
        this.body_main.setRotationPoint(0.0F, 11.0F, 0.0F);
        this.body_main.addBox(-3.6200F, -3.0F, -5.5000F, 7.2400F, 7.4000F, 8.0F, 0.0F);

        this.body_chest = new AdvancedModelBox(this, 96, 0);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -5.0F);
        this.body_chest.addBox(-3.0500F, -2.0500F, -3.6000F, 6.1000F, 6.8500F, 5.6000F, 0.0F);
        this.setRotateAngle(body_chest, -0.0873F, 0.0F, 0.0F);

        this.arm_left_upper = new AdvancedModelBox(this, 77, 18);
        this.arm_left_upper.setRotationPoint(2.4900F, -0.1826F, -1.1000F);
        this.arm_left_upper.addBox(-1.5400F, -1.2000F, -1.6500F, 3.0800F, 6.6000F, 4.0F, 0.0F);
        this.setRotateAngle(arm_left_upper, -0.0524F, 0.0F, 0.0F);

        this.arm_left_lower = new AdvancedModelBox(this, 87, 31);
        this.arm_left_lower.setRotationPoint(-0.3900F, 5.4000F, 0.0F);
        this.arm_left_lower.addBox(-0.7500F, -1.2000F, -1.6500F, 1.5000F, 5.6000F, 3.5500F, 0.0F);
        this.setRotateAngle(arm_left_lower, 0.2094F, 0.0F, 0.0F);

        this.arm_left_foot = new AdvancedModelBox(this, 90, 41);
        this.arm_left_foot.setRotationPoint(-0.6250F, 4.4000F, 0.0F);
        this.arm_left_foot.addBox(-0.9250F, -1.0F, -1.1500F, 1.8500F, 3.3500F, 2.2000F, 0.0F);
        this.setRotateAngle(arm_left_foot, -0.1571F, 0.0F, 0.0F);

        this.arm_left_paw = new AdvancedModelBox(this, 108, 41);
        this.arm_left_paw.setRotationPoint(-0.0400F, 2.3500F, 0.0F);
        this.arm_left_paw.addBox(-1.2850F, -0.6000F, -2.3000F, 2.5700F, 1.8000F, 3.1500F, 0.0F);
        this.setRotateAngle(arm_left_paw, 0.0873F, 0.0F, 0.0F);

        this.arm_right_upper = new AdvancedModelBox(this, 92, 18);
        this.arm_right_upper.setRotationPoint(-2.4900F, -0.1826F, -1.1000F);
        this.arm_right_upper.addBox(-1.5400F, -1.2000F, -1.6500F, 3.0800F, 6.6000F, 4.0F, 0.0F);
        this.setRotateAngle(arm_right_upper, -0.0524F, 0.0F, 0.0F);

        this.arm_right_lower = new AdvancedModelBox(this, 98, 31);
        this.arm_right_lower.setRotationPoint(0.3900F, 5.4000F, 0.0F);
        this.arm_right_lower.addBox(-0.7500F, -1.2000F, -1.6500F, 1.5000F, 5.6000F, 3.5500F, 0.0F);
        this.setRotateAngle(arm_right_lower, 0.2094F, 0.0F, 0.0F);

        this.arm_right_foot = new AdvancedModelBox(this, 99, 41);
        this.arm_right_foot.setRotationPoint(0.6250F, 4.4000F, 0.0F);
        this.arm_right_foot.addBox(-0.9250F, -1.0F, -1.1500F, 1.8500F, 3.3500F, 2.2000F, 0.0F);
        this.setRotateAngle(arm_right_foot, -0.1571F, 0.0F, 0.0F);

        this.arm_right_paw = new AdvancedModelBox(this, 0, 49);
        this.arm_right_paw.setRotationPoint(0.0400F, 2.3500F, 0.0F);
        this.arm_right_paw.addBox(-1.2850F, -0.6000F, -2.3000F, 2.5700F, 1.8000F, 3.1500F, 0.0F);
        this.setRotateAngle(arm_right_paw, 0.0873F, 0.0F, 0.0F);

        this.body_loin = new AdvancedModelBox(this, 54, 18);
        this.body_loin.setRotationPoint(0.0F, 0.0F, 2.0F);
        this.body_loin.addBox(-3.0400F, -2.7000F, -0.8000F, 6.0800F, 5.3000F, 5.4000F, 0.0F);
        this.setRotateAngle(body_loin, 0.0524F, 0.0F, 0.0F);

        this.body_croup = new AdvancedModelBox(this, 30, 18);
        this.body_croup.setRotationPoint(0.0F, -0.5000F, 4.4000F);
        this.body_croup.addBox(-3.4800F, -2.4000F, -1.2000F, 6.9600F, 6.1000F, 4.8000F, 0.0F);
        this.setRotateAngle(body_croup, 0.1396F, 0.0F, 0.0F);

        this.tail_1 = new AdvancedModelBox(this, 109, 31);
        this.tail_1.setRotationPoint(0.0F, -1.4000F, 3.0F);
        this.tail_1.addBox(-1.3500F, -1.3000F, -1.0F, 2.7000F, 2.7500F, 4.4000F, 0.0F);
        this.setRotateAngle(tail_1, -0.5760F, 0.0F, 0.0F);

        this.tail_2 = new AdvancedModelBox(this, 0, 41);
        this.tail_2.setRotationPoint(0.0F, 0.0F, 3.4000F);
        this.tail_2.addBox(-1.6000F, -1.4500F, -1.1000F, 3.2000F, 3.0F, 4.3000F, 0.0F);
        this.setRotateAngle(tail_2, -0.2793F, 0.0F, 0.0F);

        this.tail_3 = new AdvancedModelBox(this, 59, 41);
        this.tail_3.setRotationPoint(0.0F, 0.0F, 3.2000F);
        this.tail_3.addBox(-1.1500F, -1.0500F, -0.9000F, 2.3000F, 2.1500F, 3.8000F, 0.0F);
        this.setRotateAngle(tail_3, -0.2443F, 0.0F, 0.0F);

        this.leg_left_upper = new AdvancedModelBox(this, 0, 18);
        this.leg_left_upper.setRotationPoint(2.9400F, 0.4714F, 1.7000F);
        this.leg_left_upper.addBox(-1.0900F, -1.4000F, -2.5500F, 2.1800F, 6.9500F, 5.3000F, 0.0F);
        this.setRotateAngle(leg_left_upper, -0.6458F, 0.0F, 0.0F);

        this.leg_left_lower = new AdvancedModelBox(this, 63, 31);
        this.leg_left_lower.setRotationPoint(-0.7400F, 5.5500F, 0.0F);
        this.leg_left_lower.addBox(-0.8000F, -0.8000F, -1.9500F, 1.6000F, 5.9000F, 4.0500F, 0.0F);
        this.setRotateAngle(leg_left_lower, 1.1519F, 0.0F, 0.0F);

        this.leg_left_foot = new AdvancedModelBox(this, 30, 41);
        this.leg_left_foot.setRotationPoint(-0.4500F, 5.1000F, 0.0F);
        this.leg_left_foot.addBox(-0.7500F, -1.0F, -1.1000F, 1.5000F, 4.5500F, 2.2500F, 0.0F);
        this.setRotateAngle(leg_left_foot, -0.8727F, 0.0F, 0.0F);

        this.leg_left_paw = new AdvancedModelBox(this, 12, 49);
        this.leg_left_paw.setRotationPoint(-0.0150F, 3.5500F, 0.0F);
        this.leg_left_paw.addBox(-1.0850F, -0.6000F, -2.2000F, 2.1700F, 1.8000F, 3.0500F, 0.0F);
        this.setRotateAngle(leg_left_paw, 0.1745F, 0.0F, 0.0F);

        this.leg_right_upper = new AdvancedModelBox(this, 15, 18);
        this.leg_right_upper.setRotationPoint(-2.9400F, 0.4714F, 1.7000F);
        this.leg_right_upper.addBox(-1.0900F, -1.4000F, -2.5500F, 2.1800F, 6.9500F, 5.3000F, 0.0F);
        this.setRotateAngle(leg_right_upper, -0.6458F, 0.0F, 0.0F);

        this.leg_right_lower = new AdvancedModelBox(this, 75, 31);
        this.leg_right_lower.setRotationPoint(0.7400F, 5.5500F, 0.0F);
        this.leg_right_lower.addBox(-0.8000F, -0.8000F, -1.9500F, 1.6000F, 5.9000F, 4.0500F, 0.0F);
        this.setRotateAngle(leg_right_lower, 1.1519F, 0.0F, 0.0F);

        this.leg_right_foot = new AdvancedModelBox(this, 38, 41);
        this.leg_right_foot.setRotationPoint(0.4500F, 5.1000F, 0.0F);
        this.leg_right_foot.addBox(-0.7500F, -1.0F, -1.1000F, 1.5000F, 4.5500F, 2.2500F, 0.0F);
        this.setRotateAngle(leg_right_foot, -0.8727F, 0.0F, 0.0F);

        this.leg_right_paw = new AdvancedModelBox(this, 23, 49);
        this.leg_right_paw.setRotationPoint(0.0150F, 3.5500F, 0.0F);
        this.leg_right_paw.addBox(-1.0850F, -0.6000F, -2.2000F, 2.1700F, 1.8000F, 3.0500F, 0.0F);
        this.setRotateAngle(leg_right_paw, 0.1745F, 0.0F, 0.0F);

        this.hair = new AdvancedModelBox(this, 0, 0);
        this.hair.setRotationPoint(0.0F, 0.0F, -3.0F);
        this.hair.addBox(-4.4500F, -4.0F, -5.2000F, 8.9000F, 7.0F, 10.1000F, 0.0F);

        this.head_neck = new AdvancedModelBox(this, 69, 0);
        this.head_neck.setRotationPoint(0.0F, -1.2000F, -4.8000F);
        this.head_neck.addBox(-3.3500F, -2.4000F, -5.2000F, 6.7000F, 5.5500F, 6.7000F, 0.0F);
        this.setRotateAngle(head_neck, 0.1396F, 0.0F, 0.0F);

        this.head_main = new AdvancedModelBox(this, 24, 31);
        this.head_main.setRotationPoint(0.0F, 0.0F, -5.2000F);
        this.head_main.addBox(-2.6000F, -2.4500F, -3.7000F, 5.2000F, 4.6500F, 4.7000F, 0.0F);
        this.setRotateAngle(head_main, -0.1745F, 0.0F, 0.0F);

        this.head_face = new AdvancedModelBox(this, 46, 41);
        this.head_face.setRotationPoint(0.0F, 0.0F, -3.7000F);
        this.head_face.addBox(-1.9800F, -1.8500F, -1.3000F, 3.9600F, 3.6000F, 2.1000F, 0.0F);
        this.setRotateAngle(head_face, 0.0349F, 0.0F, 0.0F);

        this.head_snout = new AdvancedModelBox(this, 15, 41);
        this.head_snout.setRotationPoint(0.0F, 0.0F, -1.3000F);
        this.head_snout.addBox(-1.5200F, -1.1800F, -3.0F, 3.0400F, 2.5600F, 4.1000F, 0.0F);
        this.setRotateAngle(head_snout, 0.1222F, 0.0F, 0.0F);

        this.nose = new AdvancedModelBox(this, 66, 49);
        this.nose.setRotationPoint(0.0F, 0.0F, -3.0F);
        this.nose.addBox(-1.0500F, -0.9500F, -0.7000F, 2.1000F, 1.4000F, 1.4000F, 0.0F);

        this.head_jaw = new AdvancedModelBox(this, 44, 31);
        this.head_jaw.setRotationPoint(0.0F, 1.2000F, 0.0F);
        this.head_jaw.addBox(-1.1000F, -0.8000F, -7.6000F, 2.2000F, 2.2000F, 7.0F, 0.0F);
        this.setRotateAngle(head_jaw, 0.1222F, 0.0F, 0.0F);

        this.cheek_left = new AdvancedModelBox(this, 72, 41);
        this.cheek_left.setRotationPoint(2.6000F, 1.1000F, -1.4000F);
        this.cheek_left.addBox(-0.1000F, -0.8500F, -1.9000F, 0.6500F, 1.8500F, 3.6000F, 0.0F);
        this.setRotateAngle(cheek_left, 0.0F, 0.0F, 0.1571F);

        this.cheek_right = new AdvancedModelBox(this, 81, 41);
        this.cheek_right.setRotationPoint(-2.6000F, 1.1000F, -1.4000F);
        this.cheek_right.addBox(-0.5500F, -0.8500F, -1.9000F, 0.6500F, 1.8500F, 3.6000F, 0.0F);
        this.setRotateAngle(cheek_right, 0.0F, 0.0F, -0.1571F);

        this.ear_left = new AdvancedModelBox(this, 34, 49);
        this.ear_left.setRotationPoint(1.7000F, -2.0F, -1.2000F);
        this.ear_left.addBox(-1.1500F, -2.4000F, -1.1000F, 2.0500F, 2.4000F, 2.2000F, 0.0F);
        this.setRotateAngle(ear_left, 0.1396F, 0.0F, 0.2618F);

        this.ear_left_tip = new AdvancedModelBox(this, 52, 49);
        this.ear_left_tip.setRotationPoint(0.0F, -2.1000F, 0.0F);
        this.ear_left_tip.addBox(-0.8500F, -2.2000F, -0.9000F, 1.4500F, 2.2000F, 1.8000F, 0.0F);
        this.setRotateAngle(ear_left_tip, 0.0F, 0.0F, 0.1920F);

        this.ear_right = new AdvancedModelBox(this, 43, 49);
        this.ear_right.setRotationPoint(-1.7000F, -2.0F, -1.2000F);
        this.ear_right.addBox(-0.9000F, -2.4000F, -1.1000F, 2.0500F, 2.4000F, 2.2000F, 0.0F);
        this.setRotateAngle(ear_right, 0.1396F, 0.0F, -0.2618F);

        this.ear_right_tip = new AdvancedModelBox(this, 59, 49);
        this.ear_right_tip.setRotationPoint(0.0F, -2.1000F, 0.0F);
        this.ear_right_tip.addBox(-0.6000F, -2.2000F, -0.9000F, 1.4500F, 2.2000F, 1.8000F, 0.0F);
        this.setRotateAngle(ear_right_tip, 0.0F, 0.0F, -0.1920F);

        this.eye_left = new AdvancedModelBox(this, 73, 49);
        this.eye_left.setRotationPoint(2.9500F, -1.1000F, -2.8000F);
        this.eye_left.addBox(0.0F, -1.0F, -0.5000F, 0.0F, 2.0F, 1.0F, 0.0F);

        this.eye_right = new AdvancedModelBox(this, 75, 49);
        this.eye_right.setRotationPoint(-2.9500F, -1.1000F, -2.8000F);
        this.eye_right.addBox(0.0F, -1.0F, -0.5000F, 0.0F, 2.0F, 1.0F, 0.0F);

        this.neck_ruff = new AdvancedModelBox(this, 0, 31);
        this.neck_ruff.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.neck_ruff.addBox(-3.7000F, -3.4000F, -6.6000F, 7.4000F, 5.4500F, 4.4000F, 0.0F);

        this.body_main.addChild(this.body_chest);
        this.body_chest.addChild(this.arm_left_upper);
        this.arm_left_upper.addChild(this.arm_left_lower);
        this.arm_left_lower.addChild(this.arm_left_foot);
        this.arm_left_foot.addChild(this.arm_left_paw);
        this.body_chest.addChild(this.arm_right_upper);
        this.arm_right_upper.addChild(this.arm_right_lower);
        this.arm_right_lower.addChild(this.arm_right_foot);
        this.arm_right_foot.addChild(this.arm_right_paw);
        this.body_main.addChild(this.body_loin);
        this.body_loin.addChild(this.body_croup);
        this.body_croup.addChild(this.tail_1);
        this.tail_1.addChild(this.tail_2);
        this.tail_2.addChild(this.tail_3);
        this.body_croup.addChild(this.leg_left_upper);
        this.leg_left_upper.addChild(this.leg_left_lower);
        this.leg_left_lower.addChild(this.leg_left_foot);
        this.leg_left_foot.addChild(this.leg_left_paw);
        this.body_croup.addChild(this.leg_right_upper);
        this.leg_right_upper.addChild(this.leg_right_lower);
        this.leg_right_lower.addChild(this.leg_right_foot);
        this.leg_right_foot.addChild(this.leg_right_paw);
        this.body_main.addChild(this.hair);
        this.body_main.addChild(this.head_neck);
        this.head_neck.addChild(this.head_main);
        this.head_main.addChild(this.head_face);
        this.head_face.addChild(this.head_snout);
        this.head_snout.addChild(this.nose);
        this.head_main.addChild(this.head_jaw);
        this.head_main.addChild(this.cheek_left);
        this.head_main.addChild(this.cheek_right);
        this.head_main.addChild(this.ear_left);
        this.ear_left.addChild(this.ear_left_tip);
        this.head_main.addChild(this.ear_right);
        this.ear_right.addChild(this.ear_right_tip);
        this.head_main.addChild(this.eye_left);
        this.head_main.addChild(this.eye_right);
        this.head_neck.addChild(this.neck_ruff);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(this.body_main);
    }

    /* Built once: Citadel asks for this every frame for every animal, and ImmutableList.of over
     * dozens of parts was a fresh allocation each time. Lazy because the parts only exist once
     * the constructor has built them. */
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
            body_main, body_chest, arm_left_upper, arm_left_lower, arm_left_foot,
            arm_left_paw, arm_right_upper, arm_right_lower, arm_right_foot, arm_right_paw,
            body_loin, body_croup, tail_1, tail_2, tail_3,
            leg_left_upper, leg_left_lower, leg_left_foot, leg_left_paw, leg_right_upper,
            leg_right_lower, leg_right_foot, leg_right_paw, hair, head_neck,
            head_main, head_face, head_snout, nose, head_jaw,
            cheek_left, cheek_right, ear_left, ear_left_tip, ear_right,
            ear_right_tip, eye_left, eye_right, neck_ruff);
        }
        return this.allParts;
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

    /**
     * Stretch one limb chain to `longer` x length and `thinner` x cross-section.
     * Each segment is scaled on its own (scaleChildren stays OFF) so the scale lands in
     * that box's own rotated frame and nothing shears; the child rotation points are then
     * rescaled by hand because they are offsets expressed in the PARENT's frame.
     * resetToDefaultPose() restores the authored rotation points every frame, so these
     * multiplications never accumulate.
     */
    private void stretchLimb(AdvancedModelBox upper, AdvancedModelBox lower,
                             AdvancedModelBox foot, AdvancedModelBox paw,
                             float longer, float thinner) {
        AdvancedModelBox[] chain = {upper, lower, foot, paw};
        for (AdvancedModelBox part : chain) {
            part.setShouldScaleChildren(false);
            part.setScale(thinner, longer, thinner);
        }
        // Every segment INCLUDING the paw scales in length. Exempting the paw would be
        // more anatomical but it breaks the ground-contact arithmetic: LEG_DROP is the
        // whole pivot-to-sole distance, so the lift below is only exact while the entire
        // chain scales by the same factor.
        for (AdvancedModelBox part : new AdvancedModelBox[]{lower, foot, paw}) {
            part.rotationPointX *= thinner;
            part.rotationPointY *= longer;
            part.rotationPointZ *= thinner;
        }
    }

    public void setupAnim(EntityDireWolf wolf, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(wolf);
        float globalSpeed = 2.8f;
        limbSwingAmount = Math.min(0.6F, limbSwingAmount * 2);
        limbSwing *= 0.5F;

        // Breathing Animation
        final double scaleX = Math.sin(ageInTicks * 1 / 20F);
        final double scaleY = Math.sin(ageInTicks / 16);
        this.body_main.setScale((float) (1F + scaleX * 0.08F), (float) (1F + scaleY * 0.06F), 1.0F);
        bob(body_main, 0.4F * globalSpeed, 0.03F, false, ageInTicks / 20, 2);
        bob(arm_right_upper, 0.4F * globalSpeed, 0.03F, false, -ageInTicks / 20, 2);
        bob(arm_left_upper, 0.4F * globalSpeed, 0.03F, false, -ageInTicks / 20, 2);
        bob(leg_right_upper, 0.4F * globalSpeed, 0.03F, false, -ageInTicks / 20, 2);
        bob(leg_left_upper, 0.4F * globalSpeed, 0.03F, false, -ageInTicks / 20, 2);

        // Blinking Animation - retract both eye planes into head_main so they are occluded.
        // The eye sits 0.35 PROUD of the skull side (|2.95| vs |2.60|), so pulling it to
        // |1.6| buries it a full unit deep and no sliver can poke through at any head
        // angle. NOTE the export flips X: eye_LEFT carries the POSITIVE offset in Java.
        if (!wolf.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(1.6F, -1.1F, -2.8F);
            this.eye_right.setRotationPoint(-1.6F, -1.1F, -2.8F);
        }

        // Head Tracking Animation
        if (!wolf.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, head_neck);
            this.faceTarget(netHeadYaw, headPitch, 3, head_main);
        }

        // ---- per-species leg proportions (maned wolf) ----
        // Chrysocyon is defined by long, THIN stilt legs, and a skin cannot express that.
        //
        // Scaling the limb ROOT with setShouldScaleChildren(true) is the obvious approach
        // and it is wrong here: that pushes ONE matrix down the whole chain, so the scale
        // has to be uniform (long legs come out equally fat), and any non-uniform version
        // shears the lower segments, which each carry their own X rotation.
        // Instead every segment is scaled SEPARATELY with scaleChildren OFF. Citadel
        // applies the scale after the part's own rotation, so each box is scaled in its
        // OWN frame -- narrow on x/z, long on y, with no shear at any joint. The chain is
        // then re-closed by hand: a child's rotation point is an offset in its PARENT's
        // local frame, so it takes the parent's own factors, or the segments detach.
        float legLong = ComplexMob.speciesFlag(wolf, "legLength") / 1000F;
        float legThin = ComplexMob.speciesFlag(wolf, "legThickness") / 1000F;
        if (legLong != 1.0F || legThin != 1.0F) {
            stretchLimb(this.arm_left_upper, this.arm_left_lower, this.arm_left_foot,
                    this.arm_left_paw, legLong, legThin);
            stretchLimb(this.arm_right_upper, this.arm_right_lower, this.arm_right_foot,
                    this.arm_right_paw, legLong, legThin);
            stretchLimb(this.leg_left_upper, this.leg_left_lower, this.leg_left_foot,
                    this.leg_left_paw, legLong, legThin);
            stretchLimb(this.leg_right_upper, this.leg_right_lower, this.leg_right_foot,
                    this.leg_right_paw, legLong, legThin);
            // Lengthening from the pivots pushes the paws THROUGH the ground, so lift the
            // body by the extra length. LEG_DROP is the measured foreleg pivot-to-sole
            // distance (13.18); the hind chain is 13.03, so lifting by the foreleg figure
            // leaves the hind paws 0.05 clear rather than buried. ADDITIVE, like the
            // water-sink offset below, so it composes with whatever animate() did.
            this.body_main.rotationPointY -= (legLong - 1.0F) * LEG_DROP;
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
        // Per-species: stiffTail (thylacine, user 2026-10-09). A thylacine's tail is thick where it leaves
        // the body, tapers EVENLY (3.25 -> 2.7 -> 2.2 wide) and is carried out low and nearly rigid -- not a
        // canid's hanging brush. Box scales (scaleChildren off, UV-safe), tail_2/tail_3 pivots pushed back
        // so the tail is ~10% longer, a slight downward curve, and only a small lift with speed. The first
        // pass (base 1.6x, tip 0.7x) read as a bee stinger. Written on BOTH branches: resetToDefaultPose
        // does not restore scale and the model is shared.
        boolean stiff = ComplexMob.speciesFlag(wolf, "stiffTail") == 1;
        this.tail_1.setScale(stiff ? 1.2F : 1.0F, stiff ? 1.2F : 1.0F, stiff ? 1.1F : 1.0F);
        this.tail_2.setScale(stiff ? 0.85F : 1.0F, stiff ? 0.85F : 1.0F, stiff ? 1.15F : 1.0F);
        this.tail_3.setScale(stiff ? 0.95F : 1.0F, stiff ? 0.95F : 1.0F, stiff ? 1.3F : 1.0F);
        float tailBase = this.tail_1.defaultRotationX;
        float tailLift = 2.0F;
        if (stiff) {
            this.tail_2.rotationPointZ += 0.5F;
            this.tail_3.rotationPointZ += 0.6F;
            this.tail_2.rotateAngleX += -0.1F - this.tail_2.defaultRotationX;
            this.tail_3.rotateAngleX += -0.1F - this.tail_3.defaultRotationX;
            tailBase = -0.4F;
            tailLift = 0.5F;
        }
        float newZ = Mth.lerp(0.4F, this.tailX, tailBase + (float)wolf.getCurrentSpeed() * tailLift);
        this.tail_1.rotateAngleX = newZ;
        this.tailX = newZ;
        if (wolf.canMove()) {
            // Gait FITTED to the rebuilt four-segment legs by scripts/wolf_gait.py (2026-10-07); the old
            // calls were the hyena's cycle and dug the paws in while sliding them ~2.4x too far per step.
            // Each joint is two walk() terms (cycle + second harmonic), which is what lets a paw stay flat
            // on the floor through the stance. Walk = lateral sequence (LH, LF, RH, RF); run = rotary
            // gallop (hind pair, then fore pair). The phase is divided by the RENDER scale so a planted paw
            // keeps pace with the ground for every species size (maned wolf 0.55 .. dire wolf 0.9) and for
            // pups. The body settles a little while moving: the near-straight rest-pose legs cannot reach
            // the floor at the ends of a stride otherwise.
            float gait = limbSwing * 2.0F / Math.max(0.2F, wolf.getMobSize() * wolf.getScale());
            float stride = Math.min(1.0F, limbSwingAmount / 0.6F);
            // 0.1 blocks/tick of tracked speed: wandering (x1.0) stays below it and walks; following,
            // fleeing and hunting (x1.25..x1.6) run. The old 0.06 sent plain wandering into the run cycle.
            boolean running = wolf.getCurrentSpeed() > 0.1F || wolf.isAngry();
            if (!running) {
                // walk: fitted by scripts/wolf_gait.py (K=2.10 units/rad, duty 0.62)
                this.body_main.rotationPointY += (0.850F + 0.200F * Mth.sin(gait * 3.8095F + 0.0000F)) * stride;
                walk(arm_left_upper, 1.9048F, 0.3500F, false, 0.0998F, -0.2014F, gait, stride);
                walk(arm_left_upper, 3.8095F, 0.0907F, false, 0.4066F, 0F, gait, stride);
                walk(arm_left_lower, 1.9048F, 0.4000F, false, 1.9427F, 0.2968F, gait, stride);
                walk(arm_left_lower, 3.8095F, -0.0440F, false, 4.2900F, 0F, gait, stride);
                walk(arm_left_foot, 1.9048F, 0.8500F, false, 2.9324F, -0.3000F, gait, stride);
                walk(arm_left_foot, 3.8095F, 0.3000F, false, 2.8074F, 0F, gait, stride);
                walk(arm_left_paw, 1.9048F, 0.3000F, false, 4.2799F, -0.3000F, gait, stride);
                walk(arm_left_paw, 3.8095F, 0.1500F, false, 2.3789F, 0F, gait, stride);
                walk(arm_right_upper, 1.9048F, 0.3500F, false, 3.2413F, -0.2014F, gait, stride);
                walk(arm_right_upper, 3.8095F, 0.0907F, false, 0.4065F, 0F, gait, stride);
                walk(arm_right_lower, 1.9048F, 0.4000F, false, 5.0843F, 0.2968F, gait, stride);
                walk(arm_right_lower, 3.8095F, -0.0440F, false, 4.2902F, 0F, gait, stride);
                walk(arm_right_foot, 1.9048F, 0.8500F, false, 6.0739F, -0.3000F, gait, stride);
                walk(arm_right_foot, 3.8095F, 0.3000F, false, 2.8076F, 0F, gait, stride);
                walk(arm_right_paw, 1.9048F, 0.3000F, false, 1.1384F, -0.3000F, gait, stride);
                walk(arm_right_paw, 3.8095F, 0.1500F, false, 2.3786F, 0F, gait, stride);
                walk(leg_left_upper, 1.9048F, 0.3408F, false, 2.6050F, -0.3000F, gait, stride);
                walk(leg_left_upper, 3.8095F, -0.0445F, false, 1.7907F, 0F, gait, stride);
                walk(leg_left_lower, 1.9048F, 0.2641F, false, 1.8974F, 0.2412F, gait, stride);
                walk(leg_left_lower, 3.8095F, -0.1500F, false, 5.2881F, 0F, gait, stride);
                walk(leg_left_foot, 1.9048F, 0.7976F, false, 5.0734F, 0.1875F, gait, stride);
                walk(leg_left_foot, 3.8095F, 0.3000F, false, 4.6416F, 0F, gait, stride);
                walk(leg_left_paw, 1.9048F, 0.3000F, false, 1.4245F, -0.1563F, gait, stride);
                walk(leg_left_paw, 3.8095F, -0.1500F, false, 4.2632F, 0F, gait, stride);
                walk(leg_right_upper, 1.9048F, 0.3408F, false, 5.7466F, -0.3000F, gait, stride);
                walk(leg_right_upper, 3.8095F, -0.0445F, false, 1.7907F, 0F, gait, stride);
                walk(leg_right_lower, 1.9048F, 0.2641F, false, 5.0390F, 0.2412F, gait, stride);
                walk(leg_right_lower, 3.8095F, -0.1500F, false, 5.2881F, 0F, gait, stride);
                walk(leg_right_foot, 1.9048F, 0.7976F, false, 1.9318F, 0.1875F, gait, stride);
                walk(leg_right_foot, 3.8095F, 0.3000F, false, 4.6416F, 0F, gait, stride);
                walk(leg_right_paw, 1.9048F, 0.3000F, false, 4.5661F, -0.1563F, gait, stride);
                walk(leg_right_paw, 3.8095F, -0.1500F, false, 4.2632F, 0F, gait, stride);
            } else {
                // run: fitted by scripts/wolf_gait.py (K=6.00 units/rad, duty 0.36)
                this.body_main.rotationPointY += (1.700F + 0.700F * Mth.sin(gait * 0.9467F + 0.0000F)) * stride;
                walk(body_main, 0.9467F, 0.0873F, false, 1.2000F, 0F, gait, stride);
                walk(arm_left_upper, 0.9467F, 0.6000F, false, 5.6774F, -0.2361F, gait, stride);
                walk(arm_left_upper, 1.8933F, -0.2000F, false, 2.2275F, 0F, gait, stride);
                walk(arm_left_lower, 0.9467F, 0.6000F, false, 1.1761F, 0.1323F, gait, stride);
                walk(arm_left_lower, 1.8933F, -0.2500F, false, 4.1569F, 0F, gait, stride);
                walk(arm_left_foot, 0.9467F, 0.8000F, false, 2.2535F, -0.3500F, gait, stride);
                walk(arm_left_foot, 1.8933F, -0.3000F, false, 5.2464F, 0F, gait, stride);
                walk(arm_left_paw, 0.9467F, 0.4000F, false, 2.8968F, -0.3500F, gait, stride);
                walk(arm_left_paw, 1.8933F, 0.2000F, false, 5.7891F, 0F, gait, stride);
                walk(arm_right_upper, 0.9467F, 0.6000F, false, 0.1836F, -0.1788F, gait, stride);
                walk(arm_right_upper, 1.8933F, 0.2000F, false, 0.5725F, 0F, gait, stride);
                walk(arm_right_lower, 0.9467F, 0.6000F, false, 2.1073F, 0.0404F, gait, stride);
                walk(arm_right_lower, 1.8933F, 0.2500F, false, 2.5903F, 0F, gait, stride);
                walk(arm_right_foot, 0.9467F, -0.8000F, false, 6.0490F, -0.3500F, gait, stride);
                walk(arm_right_foot, 1.8933F, 0.3000F, false, 3.6680F, 0F, gait, stride);
                walk(arm_right_paw, 0.9467F, -0.4000F, false, 0.6208F, -0.3500F, gait, stride);
                walk(arm_right_paw, 1.8933F, 0.2000F, false, 0.8487F, 0F, gait, stride);
                walk(leg_left_upper, 0.9467F, 0.4075F, false, 1.9680F, -0.3124F, gait, stride);
                walk(leg_left_upper, 1.8933F, 0.1936F, false, 5.9487F, 0F, gait, stride);
                walk(leg_left_lower, 0.9467F, 0.6000F, false, 3.1871F, 0.2300F, gait, stride);
                walk(leg_left_lower, 1.8933F, 0.2500F, false, 2.7883F, 0F, gait, stride);
                walk(leg_left_foot, 0.9467F, 0.8000F, false, 4.4937F, -0.3500F, gait, stride);
                walk(leg_left_foot, 1.8933F, 0.0858F, false, 4.8658F, 0F, gait, stride);
                walk(leg_left_paw, 0.9467F, -0.4000F, false, 3.2106F, 0.0956F, gait, stride);
                walk(leg_left_paw, 1.8933F, 0.2000F, false, 4.8328F, 0F, gait, stride);
                walk(leg_right_upper, 0.9467F, 0.4216F, false, 2.8722F, -0.2431F, gait, stride);
                walk(leg_right_upper, 1.8933F, -0.1895F, false, 4.3486F, 0F, gait, stride);
                walk(leg_right_lower, 0.9467F, 0.6000F, false, 3.7688F, 0.3500F, gait, stride);
                walk(leg_right_lower, 1.8933F, 0.2500F, false, 4.3266F, 0F, gait, stride);
                walk(leg_right_foot, 0.9467F, 0.8000F, false, 5.2593F, -0.3500F, gait, stride);
                walk(leg_right_foot, 1.8933F, -0.0959F, false, 3.1953F, 0F, gait, stride);
                walk(leg_right_paw, 0.9467F, 0.4000F, false, 0.7301F, -0.0723F, gait, stride);
                walk(leg_right_paw, 1.8933F, -0.2000F, false, 3.3259F, 0F, gait, stride);
            }
        }


        // Siting Animation -- SOLVED by dire_wolf_spec.pose_solve() against the real box geometry, not guessed:
        // every segment is constrained above the floor and the foot + paw onto it.
        if (wolf.sitProgress > 0) {
            this.progressPosition(body_main, wolf.sitProgress, 0.0F, 13.1000F, 0.0F, 40);
            this.progressRotation(body_main, wolf.sitProgress, -0.3840F, 0.0F, 0.0F, 40);
            this.progressRotation(head_neck, wolf.sitProgress, -0.30F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_left_upper, wolf.sitProgress, 0.4712F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_left_lower, wolf.sitProgress, -0.0F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_left_foot, wolf.sitProgress, -0.0F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_left_paw, wolf.sitProgress, -0.0F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_right_upper, wolf.sitProgress, 0.4712F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_right_lower, wolf.sitProgress, -0.0F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_right_foot, wolf.sitProgress, -0.0F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_right_paw, wolf.sitProgress, -0.0F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_left_upper, wolf.sitProgress, -0.8341F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_left_lower, wolf.sitProgress, 0.4683F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_left_foot, wolf.sitProgress, -1.0048F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_left_paw, wolf.sitProgress, 0.0756F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_right_upper, wolf.sitProgress, -0.8341F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_right_lower, wolf.sitProgress, 0.4683F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_right_foot, wolf.sitProgress, -1.0048F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_right_paw, wolf.sitProgress, 0.0756F, 0.0F, 0.0F, 40);
            this.progressRotation(tail_1, wolf.sitProgress, -0.9F, 0.0F, 0.0F, 40);
        }

        // Sleeping Animation -- SOLVED by dire_wolf_spec.pose_solve() against the real box geometry, not guessed:
        // every segment is constrained above the floor and the foot + paw onto it.
        if (wolf.sleepProgress > 0) {
            this.progressPosition(body_main, wolf.sleepProgress, 0.0F, 18.2000F, 0.0F, 40);
            this.progressRotation(body_main, wolf.sleepProgress, 0.0F, 0.0F, -1.50255395F, 40);
            this.progressRotation(head_neck, wolf.sleepProgress, 0.27314402F, 0.22759093F, 0.0F, 40);
            this.progressRotation(head_main, wolf.sleepProgress, 0.45535640F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_left_upper, wolf.sleepProgress, -1.3600F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_left_lower, wolf.sleepProgress, 1.6572F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_left_foot, wolf.sleepProgress, -1.7780F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_left_paw, wolf.sleepProgress, 0.0527F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_right_upper, wolf.sleepProgress, -1.3600F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_right_lower, wolf.sleepProgress, 1.6572F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_right_foot, wolf.sleepProgress, -1.7780F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_right_paw, wolf.sleepProgress, 0.0527F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_left_upper, wolf.sleepProgress, -1.6663F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_left_lower, wolf.sleepProgress, 1.9022F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_left_foot, wolf.sleepProgress, -1.9851F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_left_paw, wolf.sleepProgress, 0.0744F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_right_upper, wolf.sleepProgress, -1.6663F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_right_lower, wolf.sleepProgress, 1.9022F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_right_foot, wolf.sleepProgress, -1.9851F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_right_paw, wolf.sleepProgress, 0.0744F, 0.0F, 0.0F, 40);
            this.progressRotation(tail_1, wolf.sleepProgress, 0.5F, 0.0F, 0.0F, 40);
        }

    }
}
