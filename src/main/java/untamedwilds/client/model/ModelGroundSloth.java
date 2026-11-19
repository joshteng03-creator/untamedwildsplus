package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityGroundSloth;

/**
 * Ground sloths -- Megatherium, Eremotherium, Megalonyx, Megalocnus, Glossotherium. FULL REBUILD (2026-11-16) of the July rig,
 * which was sculpted by eye before the remodelling SOP: three body slabs at different tilts read as a crumpled crate, it
 * stood ~2.7 m at the hips for a ~2 m animal, its small bear head vanished into the chest and its claws were ivory mittens.
 *
 * 43 boxes on 256x256, authored in scripts/ground_sloth_spec.py, ported VERBATIM by scripts/gen_model_ground_sloth.py and
 * proved by verify_port.py. One long sloping topline over a nested barrel (high wide pelvis, lower shoulders), the forefoot
 * walking on its outer edge with dark claws turned in, the hind foot on its outer edge with the sole turned in, a long
 * thick tail. headShape 0 MEGATHERIID / 1 MYLODONTID / 2 MEGALONYCHID picks the head; written every frame (shared model).
 * ATTACK_THREATEN is the rear-up: the whole animal pitches up about the HIP JOINT onto its hind feet and tail (solved:
 * the hip stays put, the feet stay planted, the tail tip rests on the ground), forelimbs raised and raking.
 */
public class ModelGroundSloth extends AdvancedEntityModel<EntityGroundSloth> {

    private final AdvancedModelBox body_mid;
    private final AdvancedModelBox body_hips;
    private final AdvancedModelBox body_rump;
    private final AdvancedModelBox tail_1;
    private final AdvancedModelBox tail_2;
    private final AdvancedModelBox tail_3;
    private final AdvancedModelBox tail_4;
    private final AdvancedModelBox leg_left_thigh;
    private final AdvancedModelBox leg_left_shank;
    private final AdvancedModelBox foot_left;
    private final AdvancedModelBox leg_right_thigh;
    private final AdvancedModelBox leg_right_shank;
    private final AdvancedModelBox foot_right;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox snout_mega;
    private final AdvancedModelBox lip_mega;
    private final AdvancedModelBox jaw_mega;
    private final AdvancedModelBox cheek_mylo;
    private final AdvancedModelBox snout_mylo;
    private final AdvancedModelBox jaw_mylo;
    private final AdvancedModelBox snout_short;
    private final AdvancedModelBox jaw_short;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox arm_left_1;
    private final AdvancedModelBox arm_left_2;
    private final AdvancedModelBox hand_left;
    private final AdvancedModelBox claw_left_a1;
    private final AdvancedModelBox claw_left_a2;
    private final AdvancedModelBox claw_left_b1;
    private final AdvancedModelBox claw_left_b2;
    private final AdvancedModelBox arm_right_1;
    private final AdvancedModelBox arm_right_2;
    private final AdvancedModelBox hand_right;
    private final AdvancedModelBox claw_right_a1;
    private final AdvancedModelBox claw_right_a2;
    private final AdvancedModelBox claw_right_b1;
    private final AdvancedModelBox claw_right_b2;
    private final AdvancedModelBox body_back;

    private final ModelAnimator animator;

    // SOLVED rest-pose neck lift per headShape (rad, 60% neck / 40% head): the head clears the ground resting and lies
    // on it asleep
    private static final float[] SIT_LIFT = {0.6458F, 0.4712F, 0.5236F};
    private static final float[] SLEEP_LIFT = {0.5061F, 0.3142F, 0.3578F};
    // the rear-up (solved): pitch about the hip, the root move that keeps the hip joint fixed, the tail lift
    private static final float REAR_PITCH = 0.7679F;
    private static final float REAR_MOVE_Y = -8.7968F;
    private static final float REAR_MOVE_Z = 3.6509F;
    private static final float REAR_TAIL = 0.6632F;

    public ModelGroundSloth() {
        this.texWidth = 256;
        this.texHeight = 256;

        this.body_mid = new AdvancedModelBox(this, 109, 0);
        this.body_mid.setRotationPoint(0.0F, 2.0F, 0.0F);
        this.body_mid.addBox(-10.5000F, -5.5000F, -11.0F, 21.0F, 13.5000F, 22.0F, 0.0F);
        this.body_hips = new AdvancedModelBox(this, 109, 36);
        this.body_hips.setRotationPoint(0.0F, -2.0F, 12.0F);
        this.body_hips.addBox(-11.2000F, -5.0F, -8.0F, 22.4000F, 15.5000F, 17.0F, 0.0F);
        this.setRotateAngle(body_hips, -0.1396F, 0.0F, 0.0F);
        this.body_rump = new AdvancedModelBox(this, 195, 24);
        this.body_rump.setRotationPoint(0.0F, -3.0F, 8.0F);
        this.body_rump.addBox(-9.6000F, -2.0F, -3.0F, 19.2000F, 14.0F, 9.0F, 0.0F);
        this.setRotateAngle(body_rump, -0.5236F, 0.0F, 0.0F);
        this.tail_1 = new AdvancedModelBox(this, 73, 69);
        this.tail_1.setRotationPoint(0.0F, 2.5000F, 3.0F);
        this.tail_1.addBox(-5.5000F, -5.0F, -1.5000F, 11.0F, 10.0F, 11.5000F, 0.0F);
        this.setRotateAngle(tail_1, 0.4538F, 0.0F, 0.0F);
        this.tail_2 = new AdvancedModelBox(this, 0, 80);
        this.tail_2.setRotationPoint(0.0F, 0.0F, 10.0F);
        this.tail_2.addBox(-4.2000F, -3.8000F, -1.2000F, 8.4000F, 7.6000F, 10.2000F, 0.0F);
        this.setRotateAngle(tail_2, -0.1047F, 0.0F, 0.0F);
        this.tail_3 = new AdvancedModelBox(this, 30, 98);
        this.tail_3.setRotationPoint(0.0F, 0.0F, 9.0F);
        this.tail_3.addBox(-3.0F, -2.7000F, -1.0F, 6.0F, 5.4000F, 9.0F, 0.0F);
        this.setRotateAngle(tail_3, -0.1047F, 0.0F, 0.0F);
        this.tail_4 = new AdvancedModelBox(this, 110, 107);
        this.tail_4.setRotationPoint(0.0F, 0.0F, 8.0F);
        this.tail_4.addBox(-2.0F, -1.8000F, -0.9000F, 4.0F, 3.6000F, 6.9000F, 0.0F);
        this.setRotateAngle(tail_4, -0.1047F, 0.0F, 0.0F);
        this.leg_left_thigh = new AdvancedModelBox(this, 188, 47);
        this.leg_left_thigh.setRotationPoint(8.0F, 2.0F, 1.0F);
        this.leg_left_thigh.addBox(-3.6000F, -2.0F, -4.0F, 7.2000F, 15.0F, 8.0F, 0.0F);
        this.leg_left_shank = new AdvancedModelBox(this, 60, 106);
        this.leg_left_shank.setRotationPoint(0.0F, 12.4000F, 0.0F);
        this.leg_left_shank.addBox(-3.0F, -1.0F, -3.2000F, 6.0F, 7.4000F, 6.4000F, 0.0F);
        this.foot_left = new AdvancedModelBox(this, 38, 80);
        this.foot_left.setRotationPoint(0.6000F, 6.0F, 0.0F);
        this.foot_left.addBox(-1.8000F, -3.6000F, -5.5000F, 3.6000F, 6.2000F, 11.0F, 0.0F);
        this.setRotateAngle(foot_left, 0.0F, 0.0F, -0.2618F);
        this.leg_right_thigh = new AdvancedModelBox(this, 219, 47);
        this.leg_right_thigh.setRotationPoint(-8.0F, 2.0F, 1.0F);
        this.leg_right_thigh.addBox(-3.6000F, -2.0F, -4.0F, 7.2000F, 15.0F, 8.0F, 0.0F);
        this.leg_right_shank = new AdvancedModelBox(this, 85, 106);
        this.leg_right_shank.setRotationPoint(0.0F, 12.4000F, 0.0F);
        this.leg_right_shank.addBox(-3.0F, -1.0F, -3.2000F, 6.0F, 7.4000F, 6.4000F, 0.0F);
        this.foot_right = new AdvancedModelBox(this, 118, 89);
        this.foot_right.setRotationPoint(-0.6000F, 6.0F, 0.0F);
        this.foot_right.addBox(-1.8000F, -3.6000F, -5.5000F, 3.6000F, 6.2000F, 11.0F, 0.0F);
        this.setRotateAngle(foot_right, 0.0F, 0.0F, 0.2618F);
        this.body_chest = new AdvancedModelBox(this, 0, 50);
        this.body_chest.setRotationPoint(0.0F, 2.0F, -12.0F);
        this.body_chest.addBox(-8.8000F, -5.0F, -12.5000F, 17.6000F, 11.5000F, 18.5000F, 0.0F);
        this.neck = new AdvancedModelBox(this, 195, 0);
        this.neck.setRotationPoint(0.0F, -1.0F, -10.0F);
        this.neck.addBox(-5.6000F, -4.5000F, -12.0F, 11.2000F, 10.0F, 14.0F, 0.0F);
        this.setRotateAngle(neck, 0.1745F, 0.0F, 0.0F);
        this.head = new AdvancedModelBox(this, 118, 69);
        this.head.setRotationPoint(0.0F, 0.5000F, -10.5000F);
        this.head.addBox(-4.5000F, -3.1250F, -9.3750F, 9.0F, 9.3750F, 10.6250F, 0.0F);
        this.setRotateAngle(head, -0.1745F, 0.0F, 0.0F);
        this.snout_mega = new AdvancedModelBox(this, 0, 98);
        this.snout_mega.setRotationPoint(0.0F, 3.1250F, -9.3750F);
        this.snout_mega.addBox(-3.2500F, -3.1250F, -7.5000F, 6.5000F, 6.8750F, 8.1250F, 0.0F);
        this.lip_mega = new AdvancedModelBox(this, 132, 107);
        this.lip_mega.setRotationPoint(0.0F, 1.2500F, -7.5000F);
        this.lip_mega.addBox(-2.2500F, -2.0F, -2.5000F, 4.5000F, 3.7500F, 3.1250F, 0.0F);
        this.jaw_mega = new AdvancedModelBox(this, 158, 70);
        this.jaw_mega.setRotationPoint(0.0F, 5.6250F, -1.8750F);
        this.jaw_mega.addBox(-3.7500F, -0.6250F, -11.2500F, 7.5000F, 6.8750F, 12.5000F, 0.0F);
        this.cheek_mylo = new AdvancedModelBox(this, 148, 105);
        this.cheek_mylo.setRotationPoint(0.0F, 3.1250F, -1.8750F);
        this.cheek_mylo.addBox(-6.5000F, -4.3750F, -3.7500F, 13.0F, 8.1250F, 5.6250F, 0.0F);
        this.snout_mylo = new AdvancedModelBox(this, 68, 91);
        this.snout_mylo.setRotationPoint(0.0F, 4.3750F, -9.3750F);
        this.snout_mylo.addBox(-5.3750F, -3.1250F, -6.2500F, 10.7500F, 6.8750F, 7.5000F, 0.0F);
        this.jaw_mylo = new AdvancedModelBox(this, 198, 89);
        this.jaw_mylo.setRotationPoint(0.0F, 8.1250F, -1.8750F);
        this.jaw_mylo.addBox(-5.0F, -1.8750F, -13.1250F, 10.0F, 3.7500F, 13.1250F, 0.0F);
        this.snout_short = new AdvancedModelBox(this, 228, 106);
        this.snout_short.setRotationPoint(0.0F, 4.3750F, -9.3750F);
        this.snout_short.addBox(-3.8750F, -3.7500F, -3.7500F, 7.7500F, 6.8750F, 5.0F, 0.0F);
        this.jaw_short = new AdvancedModelBox(this, 148, 90);
        this.jaw_short.setRotationPoint(0.0F, 7.5000F, -1.8750F);
        this.jaw_short.addBox(-4.1250F, -1.8750F, -9.3750F, 8.2500F, 4.3750F, 10.6250F, 0.0F);
        this.eye_left = new AdvancedModelBox(this, 143, 114);
        this.eye_left.setRotationPoint(4.9375F, 0.3125F, -7.6250F);
        this.eye_left.addBox(0.0F, -0.9375F, -1.2500F, 0.0F, 1.8750F, 2.5000F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 48, 113);
        this.ear_left.setRotationPoint(4.5000F, -1.8750F, -1.2500F);
        this.ear_left.addBox(-0.3750F, -3.2500F, -1.1250F, 2.8750F, 3.3750F, 2.2500F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 235, 118);
        this.eye_right.setRotationPoint(-4.9375F, 0.3125F, -7.6250F);
        this.eye_right.addBox(0.0F, -0.9375F, -1.2500F, 0.0F, 1.8750F, 2.5000F, 0.0F);
        this.ear_right = new AdvancedModelBox(this, 132, 114);
        this.ear_right.setRotationPoint(-4.5000F, -1.8750F, -1.2500F);
        this.ear_right.addBox(-2.5000F, -3.2500F, -1.1250F, 2.8750F, 3.3750F, 2.2500F, 0.0F);
        this.arm_left_1 = new AdvancedModelBox(this, 198, 70);
        this.arm_left_1.setRotationPoint(6.6000F, 1.0F, -4.0F);
        this.arm_left_1.addBox(-2.8000F, -2.5000F, -3.2000F, 5.6000F, 12.5000F, 6.4000F, 0.0F);
        this.setRotateAngle(arm_left_1, -0.1396F, 0.0F, 0.0F);
        this.arm_left_2 = new AdvancedModelBox(this, 186, 106);
        this.arm_left_2.setRotationPoint(0.0F, 9.5000F, 0.0F);
        this.arm_left_2.addBox(-2.5000F, -1.1000F, -2.6000F, 5.0F, 8.2000F, 5.2000F, 0.0F);
        this.setRotateAngle(arm_left_2, 0.2443F, 0.0F, 0.0F);
        this.hand_left = new AdvancedModelBox(this, 0, 113);
        this.hand_left.setRotationPoint(0.0F, 6.7000F, 0.0F);
        this.hand_left.addBox(-2.7000F, -0.9000F, -3.5000F, 5.4000F, 3.4000F, 6.5000F, 0.0F);
        this.setRotateAngle(hand_left, -0.1047F, 0.0F, 0.1745F);
        this.claw_left_a1 = new AdvancedModelBox(this, 110, 118);
        this.claw_left_a1.setRotationPoint(-1.4000F, 2.0500F, -3.0F);
        this.claw_left_a1.addBox(-0.7500F, -0.6000F, -0.8000F, 1.5000F, 4.0F, 1.6000F, 0.0F);
        this.setRotateAngle(claw_left_a1, -0.9829F, 0.4130F, 0.7462F);
        this.claw_left_a2 = new AdvancedModelBox(this, 240, 118);
        this.claw_left_a2.setRotationPoint(0.0F, 3.4000F, 0.0F);
        this.claw_left_a2.addBox(-0.5500F, -0.6000F, -0.6000F, 1.1000F, 3.6000F, 1.2000F, 0.0F);
        this.setRotateAngle(claw_left_a2, 2.2735F, -0.3450F, -0.7887F);
        this.claw_left_b1 = new AdvancedModelBox(this, 124, 118);
        this.claw_left_b1.setRotationPoint(-1.6000F, 2.0500F, -0.1000F);
        this.claw_left_b1.addBox(-0.7500F, -0.6000F, -0.8000F, 1.5000F, 4.0F, 1.6000F, 0.0F);
        this.setRotateAngle(claw_left_b1, -0.8399F, 0.4109F, 0.8733F);
        this.claw_left_b2 = new AdvancedModelBox(this, 250, 118);
        this.claw_left_b2.setRotationPoint(0.0F, 3.4000F, 0.0F);
        this.claw_left_b2.addBox(-0.5500F, -0.6000F, -0.6000F, 1.1000F, 3.6000F, 1.2000F, 0.0F);
        this.setRotateAngle(claw_left_b2, 2.0522F, -0.4344F, -0.7706F);
        this.arm_right_1 = new AdvancedModelBox(this, 222, 70);
        this.arm_right_1.setRotationPoint(-6.6000F, 1.0F, -4.0F);
        this.arm_right_1.addBox(-2.8000F, -2.5000F, -3.2000F, 5.6000F, 12.5000F, 6.4000F, 0.0F);
        this.setRotateAngle(arm_right_1, -0.1396F, 0.0F, 0.0F);
        this.arm_right_2 = new AdvancedModelBox(this, 207, 106);
        this.arm_right_2.setRotationPoint(0.0F, 9.5000F, 0.0F);
        this.arm_right_2.addBox(-2.5000F, -1.1000F, -2.6000F, 5.0F, 8.2000F, 5.2000F, 0.0F);
        this.setRotateAngle(arm_right_2, 0.2443F, 0.0F, 0.0F);
        this.hand_right = new AdvancedModelBox(this, 24, 113);
        this.hand_right.setRotationPoint(0.0F, 6.7000F, 0.0F);
        this.hand_right.addBox(-2.7000F, -0.9000F, -3.5000F, 5.4000F, 3.4000F, 6.5000F, 0.0F);
        this.setRotateAngle(hand_right, -0.1047F, 0.0F, -0.1745F);
        this.claw_right_a1 = new AdvancedModelBox(this, 117, 118);
        this.claw_right_a1.setRotationPoint(1.4000F, 2.0500F, -3.0F);
        this.claw_right_a1.addBox(-0.7500F, -0.6000F, -0.8000F, 1.5000F, 4.0F, 1.6000F, 0.0F);
        this.setRotateAngle(claw_right_a1, -0.9829F, -0.4130F, -0.7462F);
        this.claw_right_a2 = new AdvancedModelBox(this, 245, 118);
        this.claw_right_a2.setRotationPoint(0.0F, 3.4000F, 0.0F);
        this.claw_right_a2.addBox(-0.5500F, -0.6000F, -0.6000F, 1.1000F, 3.6000F, 1.2000F, 0.0F);
        this.setRotateAngle(claw_right_a2, 2.2735F, 0.3450F, 0.7887F);
        this.claw_right_b1 = new AdvancedModelBox(this, 228, 118);
        this.claw_right_b1.setRotationPoint(1.6000F, 2.0500F, -0.1000F);
        this.claw_right_b1.addBox(-0.7500F, -0.6000F, -0.8000F, 1.5000F, 4.0F, 1.6000F, 0.0F);
        this.setRotateAngle(claw_right_b1, -0.8399F, -0.4109F, -0.8733F);
        this.claw_right_b2 = new AdvancedModelBox(this, 48, 119);
        this.claw_right_b2.setRotationPoint(0.0F, 3.4000F, 0.0F);
        this.claw_right_b2.addBox(-0.5500F, -0.6000F, -0.6000F, 1.1000F, 3.6000F, 1.2000F, 0.0F);
        this.setRotateAngle(claw_right_b2, 2.0522F, 0.4344F, 0.7706F);
        this.body_back = new AdvancedModelBox(this, 0, 0);
        this.body_back.setRotationPoint(0.0F, -4.0F, -2.0F);
        this.body_back.addBox(-8.2000F, -3.5000F, -22.0F, 16.4000F, 11.5000F, 38.0F, 0.0F);
        this.setRotateAngle(body_back, 0.1745F, 0.0F, 0.0F);

        this.body_mid.addChild(this.body_hips);
        this.body_hips.addChild(this.body_rump);
        this.body_rump.addChild(this.tail_1);
        this.tail_1.addChild(this.tail_2);
        this.tail_2.addChild(this.tail_3);
        this.tail_3.addChild(this.tail_4);
        this.body_hips.addChild(this.leg_left_thigh);
        this.leg_left_thigh.addChild(this.leg_left_shank);
        this.leg_left_shank.addChild(this.foot_left);
        this.body_hips.addChild(this.leg_right_thigh);
        this.leg_right_thigh.addChild(this.leg_right_shank);
        this.leg_right_shank.addChild(this.foot_right);
        this.body_mid.addChild(this.body_chest);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.snout_mega);
        this.snout_mega.addChild(this.lip_mega);
        this.head.addChild(this.jaw_mega);
        this.head.addChild(this.cheek_mylo);
        this.head.addChild(this.snout_mylo);
        this.head.addChild(this.jaw_mylo);
        this.head.addChild(this.snout_short);
        this.head.addChild(this.jaw_short);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.eye_right);
        this.head.addChild(this.ear_right);
        this.body_chest.addChild(this.arm_left_1);
        this.arm_left_1.addChild(this.arm_left_2);
        this.arm_left_2.addChild(this.hand_left);
        this.hand_left.addChild(this.claw_left_a1);
        this.claw_left_a1.addChild(this.claw_left_a2);
        this.hand_left.addChild(this.claw_left_b1);
        this.claw_left_b1.addChild(this.claw_left_b2);
        this.body_chest.addChild(this.arm_right_1);
        this.arm_right_1.addChild(this.arm_right_2);
        this.arm_right_2.addChild(this.hand_right);
        this.hand_right.addChild(this.claw_right_a1);
        this.claw_right_a1.addChild(this.claw_right_a2);
        this.hand_right.addChild(this.claw_right_b1);
        this.claw_right_b1.addChild(this.claw_right_b2);
        this.body_mid.addChild(this.body_back);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_mid);
    }

    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
                body_mid,
                body_hips,
                body_rump,
                tail_1,
                tail_2,
                tail_3,
                tail_4,
                leg_left_thigh,
                leg_left_shank,
                foot_left,
                leg_right_thigh,
                leg_right_shank,
                foot_right,
                body_chest,
                neck,
                head,
                snout_mega,
                lip_mega,
                jaw_mega,
                cheek_mylo,
                snout_mylo,
                jaw_mylo,
                snout_short,
                jaw_short,
                eye_left,
                ear_left,
                eye_right,
                ear_right,
                arm_left_1,
                arm_left_2,
                hand_left,
                claw_left_a1,
                claw_left_a2,
                claw_left_b1,
                claw_left_b2,
                arm_right_1,
                arm_right_2,
                hand_right,
                claw_right_a1,
                claw_right_a2,
                claw_right_b1,
                claw_right_b2,
                body_back
            );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityGroundSloth sloth = (EntityGroundSloth) entityIn;
        animator.update(sloth);
        // <gen:idle-poses> 2.8.0: SOLVED by scripts/pose_reach.py against this model's own boxes (muzzle to the grass / the
        // water line beyond the bank, every head part and horn family clear of the ground, nothing through the torso).
        // Regenerate, never hand-edit: python scripts/gen_keyframes.py ground_sloth
        // EAT (40t, GrazeGoal's mouthful): head down to the grass, a cropping lift, back down
        animator.setAnimation(EntityGroundSloth.EAT);
        animator.startKeyframe(10);
        this.rotate(animator, neck, 19.8F, 0.0F, 0.0F);
        this.rotate(animator, head, 2.2F, 0.0F, 0.0F);
        this.rotate(animator, snout_mega, 0.9F, 0.0F, 0.0F);
        this.rotate(animator, lip_mega, 0.9F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 0.1F, -1.2F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 19.8F, 0.0F, 0.0F);
        this.rotate(animator, head, -3.8F, 0.0F, 0.0F);
        this.rotate(animator, snout_mega, 0.9F, 0.0F, 0.0F);
        this.rotate(animator, lip_mega, 0.9F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 0.1F, -1.2F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 19.8F, 0.0F, 0.0F);
        this.rotate(animator, head, 2.2F, 0.0F, 0.0F);
        this.rotate(animator, snout_mega, 0.9F, 0.0F, 0.0F);
        this.rotate(animator, lip_mega, 0.9F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 0.1F, -1.2F);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.resetKeyframe(10);
        // DRINK (100t, DrinkGoal): muzzle to the water past the bank edge, two swallows lifting the head
        animator.setAnimation(EntityGroundSloth.DRINK);
        animator.startKeyframe(15);
        this.rotate(animator, neck, 17.4F, 0.0F, 0.0F);
        this.rotate(animator, head, 6.0F, 0.0F, 0.0F);
        this.rotate(animator, snout_mega, 1.3F, 0.0F, 0.0F);
        this.rotate(animator, lip_mega, 1.2F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 1.2F, -0.9F);
        animator.endKeyframe();
        animator.setStaticKeyframe(20);
        animator.startKeyframe(5);
        this.rotate(animator, neck, 17.4F, 0.0F, 0.0F);
        this.rotate(animator, head, 1.0F, 0.0F, 0.0F);
        this.rotate(animator, snout_mega, 1.3F, 0.0F, 0.0F);
        this.rotate(animator, lip_mega, 1.2F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 1.2F, -0.9F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 17.4F, 0.0F, 0.0F);
        this.rotate(animator, head, 6.0F, 0.0F, 0.0F);
        this.rotate(animator, snout_mega, 1.3F, 0.0F, 0.0F);
        this.rotate(animator, lip_mega, 1.2F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 1.2F, -0.9F);
        animator.endKeyframe();
        animator.setStaticKeyframe(20);
        animator.startKeyframe(5);
        this.rotate(animator, neck, 17.4F, 0.0F, 0.0F);
        this.rotate(animator, head, 1.0F, 0.0F, 0.0F);
        this.rotate(animator, snout_mega, 1.3F, 0.0F, 0.0F);
        this.rotate(animator, lip_mega, 1.2F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 1.2F, -0.9F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 17.4F, 0.0F, 0.0F);
        this.rotate(animator, head, 6.0F, 0.0F, 0.0F);
        this.rotate(animator, snout_mega, 1.3F, 0.0F, 0.0F);
        this.rotate(animator, lip_mega, 1.2F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 1.2F, -0.9F);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.resetKeyframe(15);
        // </gen:idle-poses>

        // Threat: the rear-up itself is procedural in setupAnim (exact hip-fixed pitch); these keyframes rake the raised
        // forelimbs -- two swipes, the claws flexing on each.
        animator.setAnimation(EntityGroundSloth.ATTACK_THREATEN);
        animator.startKeyframe(12);
        this.rotate(animator, arm_left_1, -40F, 0, -12F);
        this.rotate(animator, arm_right_1, -36F, 0, 12F);
        this.rotate(animator, arm_left_2, -20F, 0, 0);
        this.rotate(animator, arm_right_2, -18F, 0, 0);
        animator.endKeyframe();
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(6);
            this.rotate(animator, arm_left_1, -55F, 0, -14F);
            this.rotate(animator, arm_right_1, -20F, 0, 6F);
            this.rotate(animator, arm_left_2, -30F, 0, 0);
            this.rotate(animator, arm_right_2, -10F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(6);
            this.rotate(animator, arm_left_1, -20F, 0, -6F);
            this.rotate(animator, arm_right_1, -55F, 0, 14F);
            this.rotate(animator, arm_left_2, -10F, 0, 0);
            this.rotate(animator, arm_right_2, -30F, 0, 0);
            animator.endKeyframe();
        }
        animator.setStaticKeyframe(6);
        animator.resetKeyframe(8);

        // Gore: a raking downward swipe with the right forelimb, the claws opening then hooking through.
        animator.setAnimation(EntityGroundSloth.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, body_chest, -8F, 0, 0);
        this.rotate(animator, arm_right_1, -60F, 0, 16F);
        this.rotate(animator, arm_right_2, -30F, 0, 0);
        this.rotate(animator, claw_right_a1, -25F, 0, 0);
        this.rotate(animator, claw_right_b1, -25F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, body_chest, 6F, 0, 0);
        this.rotate(animator, arm_right_1, 30F, 0, -14F);
        this.rotate(animator, arm_right_2, 15F, 0, 0);
        this.rotate(animator, claw_right_a1, 20F, 0, 0);
        this.rotate(animator, claw_right_b1, 20F, 0, 0);
        animator.move(body_mid, 0, 0, -1.5F);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    // 2.8.0 review: a barrel, not a plank (Megatherium's pelvis was enormously wide), and a jaw that does not hang
    private static final float WIDE_HIPS = 1.18F, WIDE_MID = 1.16F, WIDE_CHEST = 1.12F, WIDE_BACK = 1.14F;
    private static final float JAW_MEGA_DEPTH = 0.55F;

    public void setupAnim(EntityGroundSloth sloth, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();

        // ---- head family (every frame: one model instance serves every sloth) ----
        int headF = Mth.clamp(sloth.getHeadShape(), 0, 2);
        this.snout_mega.showModel = headF == 0;
        this.jaw_mega.showModel = headF == 0;
        this.cheek_mylo.showModel = headF == 1;
        this.snout_mylo.showModel = headF == 1;
        this.jaw_mylo.showModel = headF == 1;
        this.snout_short.showModel = headF == 2;
        this.jaw_short.showModel = headF == 2;

        // ---- 2.8.0 review (user, 2026-11-18): "far too thin" and "the sag at the bottom of the face looks weird" ----
        // UV-safe: per-box setScale (scaleChildren off) plus the limb roots pushed out by the same factor, so no skin texel
        // moves. The megatheriid jaw keeps its depth from its top pivot, so it tucks up under the skull instead of hanging.
        // KEEP IN SYNC with scripts/pose_targets.py STATICS["ModelGroundSloth"]: the pose / carcass solvers must see the same.
        this.jaw_mega.setScale(1.0F, JAW_MEGA_DEPTH, 1.0F);
        this.body_rump.setScale(WIDE_HIPS, 1.0F, 1.0F);
        this.body_back.setScale(WIDE_BACK, 1.0F, 1.0F);
        this.leg_left_thigh.rotationPointX *= WIDE_HIPS;
        this.leg_right_thigh.rotationPointX *= WIDE_HIPS;
        this.arm_left_1.rotationPointX *= WIDE_CHEST;
        this.arm_right_1.rotationPointX *= WIDE_CHEST;

        animate(sloth);
        float globalSpeed = 0.9F;
        float globalDegree = 1.0F;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- idle: slow breathing, a heavy tail sway ----
        float bx = (float) (1.0F + Math.sin(ageInTicks / 22) * 0.04F), by = (float) (1.0F + Math.sin(ageInTicks / 18) * 0.04F);
        this.body_mid.setScale(bx * WIDE_MID, by, 1.0F);
        this.body_chest.setScale(bx * WIDE_CHEST, by, 1.0F);
        this.body_hips.setScale(bx * WIDE_HIPS, by, 1.0F);
        this.swing(tail_1, 0.08F, 0.08F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_2, 0.08F, 0.10F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail_3, 0.08F, 0.12F, false, 2F, 0F, ageInTicks, 1);
        this.walk(neck, 0.06F, 0.03F, false, 0F, 0F, ageInTicks, 1);

        // eyes: squash the planes to a slit when shut (written every frame: the model is shared)
        float eyeOpen = sloth.shouldRenderEyes() ? 1.0F : 0.2F;
        this.eye_left.setScale(1.0F, eyeOpen, 1.0F);
        this.eye_right.setScale(1.0F, eyeOpen, 1.0F);

        if (!sloth.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 3, head);
        }

        // ---- the rear-up (ATTACK_THREATEN): rise over 12 ticks, hold, settle over the last 10. The animal pitches up about
        // the hip joint -- the root rotation plus the translation that keeps that joint fixed -- the thighs counter-rotate so
        // the hind feet stay planted, and the tail drops onto the ground as the third leg.
        if (sloth.getAnimation() == EntityGroundSloth.ATTACK_THREATEN) {
            float t = sloth.getAnimationTick() + (ageInTicks - (int) ageInTicks);
            float d = EntityGroundSloth.ATTACK_THREATEN.getDuration();
            float p = t < 12 ? t / 12F : t > d - 10 ? Math.max(0F, (d - t) / 10F) : 1F;
            p = p * p * (3 - 2 * p);
            this.body_mid.rotateAngleX -= REAR_PITCH * p;
            this.body_mid.rotationPointY += REAR_MOVE_Y * p;
            this.body_mid.rotationPointZ += REAR_MOVE_Z * p;
            this.leg_left_thigh.rotateAngleX += REAR_PITCH * p;
            this.leg_right_thigh.rotateAngleX += REAR_PITCH * p;
            this.tail_1.rotateAngleX += REAR_TAIL * p;
            this.neck.rotateAngleX += REAR_PITCH * 0.35F * p;    // keep the face looking forward, not at the sky
            this.head.rotateAngleX += REAR_PITCH * 0.25F * p;
        }

        // ---- walk: lateral sequence, FITTED to the box geometry (ground_sloth_walk_fit.py): no planted-foot slide ----
        if (sloth.canMove()) {
            bob(body_mid, 0.8F * globalSpeed, 0.25F * globalDegree, true, f, limbSwingAmount);
            flap(body_mid, 0.8F * globalSpeed, 0.05F * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(neck, 0.8F * globalSpeed, 0.08F * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(leg_left_thigh, 0.8F * globalSpeed, 0.548F, false, 0.000F, 0.000F, f, limbSwingAmount);
            walk(leg_left_shank, 0.8F * globalSpeed, 1.235F, false, -0.480F, 1.297F, f, limbSwingAmount);
            walk(foot_left, 0.8F * globalSpeed, -0.548F, false, 0.000F, 0.000F, f, limbSwingAmount);
            walk(foot_left, 0.8F * globalSpeed, -1.235F, false, -0.480F, -1.297F, f, limbSwingAmount);
            walk(leg_right_thigh, 0.8F * globalSpeed, 0.548F, false, 3.142F, 0.000F, f, limbSwingAmount);
            walk(leg_right_shank, 0.8F * globalSpeed, 1.235F, false, 2.662F, 1.297F, f, limbSwingAmount);
            walk(foot_right, 0.8F * globalSpeed, -0.548F, false, 3.142F, 0.000F, f, limbSwingAmount);
            walk(foot_right, 0.8F * globalSpeed, -1.235F, false, 2.662F, -1.297F, f, limbSwingAmount);
            walk(arm_left_1, 0.8F * globalSpeed, 0.452F, false, -1.571F, 0.000F, f, limbSwingAmount);
            walk(arm_left_2, 0.8F * globalSpeed, 0.953F, false, -2.468F, 0.774F, f, limbSwingAmount);
            walk(hand_left, 0.8F * globalSpeed, -0.452F, false, -1.571F, 0.000F, f, limbSwingAmount);
            walk(hand_left, 0.8F * globalSpeed, -0.953F, false, -2.468F, -0.774F, f, limbSwingAmount);
            walk(arm_right_1, 0.8F * globalSpeed, 0.452F, false, 1.571F, 0.000F, f, limbSwingAmount);
            walk(arm_right_2, 0.8F * globalSpeed, 0.953F, false, 0.674F, 0.774F, f, limbSwingAmount);
            walk(hand_right, 0.8F * globalSpeed, -0.452F, false, 1.571F, 0.000F, f, limbSwingAmount);
            walk(hand_right, 0.8F * globalSpeed, -0.953F, false, 0.674F, -0.774F, f, limbSwingAmount);
            swing(tail_1, 0.8F * globalSpeed, 0.15F * globalDegree, false, 0F, 0, f, limbSwingAmount);
            swing(tail_2, 0.8F * globalSpeed, 0.18F * globalDegree, false, -0.6F, 0, f, limbSwingAmount);
        }

        // ---- rest / sleep: SOLVED sternal recumbency (ground_sloth_spec.py --pose) ----
        if (sloth.sitProgress > 0) {
            applyRestingPose(sloth.sitProgress, SIT_LIFT[headF]);
        }
        else if (sloth.sleepProgress > 0) {
            applyRestingPose(sloth.sleepProgress, SLEEP_LIFT[headF]);
        }
    }

    private void applyRestingPose(float progress, float lift) {
        this.progressPosition(body_mid, progress, 0.0F, 13.5496F, 0.0F, 40);
        this.progressRotation(arm_left_1, progress, -1.8218F, 0.0F, 0.0F, 40);
        this.progressRotation(arm_left_2, progress, 1.3047F, 0.0F, 0.0F, 40);
        this.progressRotation(hand_left, progress, 0.0638F, 0.0F, 0.1745F, 40);
        this.progressRotation(leg_left_thigh, progress, 1.6519F, 0.0F, 0.0F, 40);
        this.progressRotation(leg_left_shank, progress, -1.1674F, 0.0F, 0.0F, 40);
        this.progressRotation(foot_left, progress, -0.1772F, 0.0F, -0.2618F, 40);
        this.progressRotation(arm_right_1, progress, -1.8218F, 0.0F, 0.0F, 40);
        this.progressRotation(arm_right_2, progress, 1.3047F, 0.0F, 0.0F, 40);
        this.progressRotation(hand_right, progress, 0.0638F, 0.0F, -0.1745F, 40);
        this.progressRotation(leg_right_thigh, progress, 1.6519F, 0.0F, 0.0F, 40);
        this.progressRotation(leg_right_shank, progress, -1.1674F, 0.0F, 0.0F, 40);
        this.progressRotation(foot_right, progress, -0.1772F, 0.0F, 0.2618F, 40);
        this.progressRotation(tail_1, progress, 0.5411F, 0.0F, 0.0F, 40);
        this.progressRotation(neck, progress, 0.1745F - lift * 0.6F, 0.0F, 0.0F, 40);
        this.progressRotation(head, progress, -0.1745F - lift * 0.4F, 0.0F, 0.0F, 40);
    }
}
