package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.reptile.EntityCrocodilian;

/**
 * Crocodilians -- Nile, saltwater, mugger, black caiman, Quinkana, Voay (Phase 4.8, 2026-11-14): 45 boxes on 256x128,
 * authored in scripts/crocodilian_spec.py, ported VERBATIM by scripts/gen_model_crocodilian.py, proved by verify_port.py.
 *
 * A low, flat, wide trunk on short sprawling legs; a flat head with the eyes on raised sockets, the upper teeth along the
 * closed jaw; back armour; a tall, thin, crested tail. Per species, written EVERY frame (one model instance is shared):
 * snout width/depth (setScale on the snout and jaw chains, children included), Voay's horns, and Quinkana's ERECT stance
 * (solved: limbs tucked under, body raised, tail re-drooped), faded out as it lies down because progressRotation adds
 * to whatever pose is already set. Sit and sleep are SOLVED belly-down poses; basking lifts the head (upper jaw) while
 * the lower jaw stays on the ground. ATTACK_ROLL is the death roll: the whole animal spins about its long axis.
 */
public class ModelCrocodilian extends AdvancedEntityModel<EntityCrocodilian> {

    private final AdvancedModelBox body_mid;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox snout_base;
    private final AdvancedModelBox snout_tip;
    private final AdvancedModelBox teeth_tip_left;
    private final AdvancedModelBox teeth_tip_right;
    private final AdvancedModelBox nose_knob;
    private final AdvancedModelBox teeth_left;
    private final AdvancedModelBox teeth_right;
    private final AdvancedModelBox jaw;
    private final AdvancedModelBox jaw_tip;
    private final AdvancedModelBox eye_bump_left;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox horn_left;
    private final AdvancedModelBox eye_bump_right;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox horn_right;
    private final AdvancedModelBox scutes_neck;
    private final AdvancedModelBox arm_left_upper;
    private final AdvancedModelBox arm_left_lower;
    private final AdvancedModelBox arm_left_foot;
    private final AdvancedModelBox arm_right_upper;
    private final AdvancedModelBox arm_right_lower;
    private final AdvancedModelBox arm_right_foot;
    private final AdvancedModelBox body_hips;
    private final AdvancedModelBox scutes_hips;
    private final AdvancedModelBox tail_1;
    private final AdvancedModelBox tail_2;
    private final AdvancedModelBox tail_3;
    private final AdvancedModelBox tail_4;
    private final AdvancedModelBox tail_5;
    private final AdvancedModelBox tail_6;
    private final AdvancedModelBox tail_4_crest;
    private final AdvancedModelBox tail_3_crest;
    private final AdvancedModelBox tail_2_crest;
    private final AdvancedModelBox tail_1_crest;
    private final AdvancedModelBox leg_left_upper;
    private final AdvancedModelBox leg_left_lower;
    private final AdvancedModelBox leg_left_foot;
    private final AdvancedModelBox leg_right_upper;
    private final AdvancedModelBox leg_right_lower;
    private final AdvancedModelBox leg_right_foot;
    private final AdvancedModelBox scutes_back;

    private final ModelAnimator animator;

    public ModelCrocodilian() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_mid = new AdvancedModelBox(this, 0, 0);
        this.body_mid.setRotationPoint(0.0F, 17.4282F, 0.0F);
        this.body_mid.addBox(-4.6000F, -2.8000F, -6.0F, 9.2000F, 5.9000F, 11.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 41, 0);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -5.0F);
        this.body_chest.addBox(-4.1000F, -2.4000F, -8.0F, 8.2000F, 5.2000F, 8.4000F, 0.0F);
        this.setRotateAngle(body_chest, -0.0175F, 0.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 208, 0);
        this.neck.setRotationPoint(0.0F, -0.6000F, -7.6000F);
        this.neck.addBox(-3.4000F, -1.0F, -4.6000F, 6.8000F, 3.4000F, 5.8000F, 0.0F);
        this.setRotateAngle(neck, 0.0175F, 0.0F, 0.0F);
        this.head = new AdvancedModelBox(this, 35, 17);
        this.head.setRotationPoint(0.0F, 0.0F, -4.6000F);
        this.head.addBox(-3.0F, -1.2000F, -4.2000F, 6.0F, 2.6000F, 5.2000F, 0.0F);
        this.setRotateAngle(head, 0.0349F, 0.0F, 0.0F);
        this.snout_base = new AdvancedModelBox(this, 58, 17);
        this.snout_base.setRotationPoint(0.0F, 0.0F, -4.2000F);
        this.snout_base.addBox(-2.5000F, -0.8000F, -4.6000F, 5.0F, 2.0F, 5.4000F, 0.0F);
        this.setRotateAngle(snout_base, 0.0175F, 0.0F, 0.0F);
        this.snout_tip = new AdvancedModelBox(this, 117, 17);
        this.snout_tip.setRotationPoint(0.0F, 0.0F, -4.6000F);
        this.snout_tip.addBox(-1.9000F, -0.6000F, -3.8000F, 3.8000F, 1.6000F, 4.4000F, 0.0F);
        this.setRotateAngle(snout_tip, -0.0175F, 0.0F, 0.0F);
        this.teeth_tip_left = new AdvancedModelBox(this, 142, 27);
        this.teeth_tip_left.setRotationPoint(1.5000F, 1.0F, -1.8000F);
        this.teeth_tip_left.addBox(-0.1500F, -0.1500F, -1.8000F, 0.3000F, 0.7000F, 3.4000F, 0.0F);
        this.teeth_tip_right = new AdvancedModelBox(this, 150, 27);
        this.teeth_tip_right.setRotationPoint(-1.5000F, 1.0F, -1.8000F);
        this.teeth_tip_right.addBox(-0.1500F, -0.1500F, -1.8000F, 0.3000F, 0.7000F, 3.4000F, 0.0F);
        this.nose_knob = new AdvancedModelBox(this, 186, 27);
        this.nose_knob.setRotationPoint(0.0F, -0.6000F, -2.8000F);
        this.nose_knob.addBox(-1.0F, -0.5000F, -0.8000F, 2.0F, 0.8000F, 1.6000F, 0.0F);
        this.teeth_left = new AdvancedModelBox(this, 106, 27);
        this.teeth_left.setRotationPoint(2.1000F, 1.2000F, -2.2000F);
        this.teeth_left.addBox(-0.1500F, -0.1500F, -2.2000F, 0.3000F, 0.7000F, 4.2000F, 0.0F);
        this.teeth_right = new AdvancedModelBox(this, 124, 27);
        this.teeth_right.setRotationPoint(-2.1000F, 1.2000F, -2.2000F);
        this.teeth_right.addBox(-0.1500F, -0.1500F, -2.2000F, 0.3000F, 0.7000F, 4.2000F, 0.0F);
        this.jaw = new AdvancedModelBox(this, 133, 0);
        this.jaw.setRotationPoint(0.0F, 1.4000F, -0.4000F);
        this.jaw.addBox(-1.7000F, -0.9000F, -8.0F, 3.4000F, 1.7000F, 8.6000F, 0.0F);
        this.setRotateAngle(jaw, 0.0175F, 0.0F, 0.0F);
        this.jaw_tip = new AdvancedModelBox(this, 236, 17);
        this.jaw_tip.setRotationPoint(0.0F, 0.0F, -8.0F);
        this.jaw_tip.addBox(-1.1000F, -1.1500F, -3.8000F, 2.2000F, 1.7500F, 4.2000F, 0.0F);
        this.setRotateAngle(jaw_tip, -0.0175F, 0.0F, 0.0F);
        this.eye_bump_left = new AdvancedModelBox(this, 115, 27);
        this.eye_bump_left.setRotationPoint(2.5500F, -0.7000F, -1.8000F);
        this.eye_bump_left.addBox(-0.6500F, -1.0F, -1.3500F, 1.3000F, 2.0F, 2.9000F, 0.0F);
        this.eye_left = new AdvancedModelBox(this, 174, 27);
        this.eye_left.setRotationPoint(3.5000F, -1.1500F, -1.8000F);
        this.eye_left.addBox(0.0F, -0.5000F, -1.3000F, 0.0F, 1.0F, 3.0F, 0.0F);
        this.horn_left = new AdvancedModelBox(this, 194, 27);
        this.horn_left.setRotationPoint(2.4500F, -1.1000F, 0.3000F);
        this.horn_left.addBox(-0.4500F, -0.9500F, -0.4000F, 0.9000F, 1.2000F, 0.8000F, 0.0F);
        this.setRotateAngle(horn_left, -0.6981F, 0.0F, 0.4363F);
        this.eye_bump_right = new AdvancedModelBox(this, 133, 27);
        this.eye_bump_right.setRotationPoint(-2.5500F, -0.7000F, -1.8000F);
        this.eye_bump_right.addBox(-0.6500F, -1.0F, -1.3500F, 1.3000F, 2.0F, 2.9000F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 180, 27);
        this.eye_right.setRotationPoint(-3.5000F, -1.1500F, -1.8000F);
        this.eye_right.addBox(0.0F, -0.5000F, -1.3000F, 0.0F, 1.0F, 3.0F, 0.0F);
        this.horn_right = new AdvancedModelBox(this, 198, 27);
        this.horn_right.setRotationPoint(-2.4500F, -1.1000F, 0.3000F);
        this.horn_right.addBox(-0.4500F, -0.9500F, -0.4000F, 0.9000F, 1.2000F, 0.8000F, 0.0F);
        this.setRotateAngle(horn_right, -0.6981F, 0.0F, -0.4363F);
        this.scutes_neck = new AdvancedModelBox(this, 34, 27);
        this.scutes_neck.setRotationPoint(0.0F, -1.3000F, -2.4000F);
        this.scutes_neck.addBox(-2.2000F, -0.6500F, -1.6000F, 4.4000F, 1.3500F, 3.4000F, 0.0F);
        this.arm_left_upper = new AdvancedModelBox(this, 50, 27);
        this.arm_left_upper.setRotationPoint(3.4000F, 1.0F, -4.0F);
        this.arm_left_upper.addBox(-0.8000F, -1.2000F, -1.2000F, 4.6000F, 2.4000F, 2.4000F, 0.0F);
        this.setRotateAngle(arm_left_upper, 0.0F, 0.0F, 0.4189F);
        this.arm_left_lower = new AdvancedModelBox(this, 184, 17);
        this.arm_left_lower.setRotationPoint(3.2000F, 0.0F, 0.0F);
        this.arm_left_lower.addBox(-1.2000F, -0.8000F, -0.9500F, 2.4000F, 4.6673F, 1.9000F, 0.0F);
        this.setRotateAngle(arm_left_lower, 0.0F, 0.0F, -0.4189F);
        this.arm_left_foot = new AdvancedModelBox(this, 64, 27);
        this.arm_left_foot.setRotationPoint(0.0F, 3.8673F, 0.0F);
        this.arm_left_foot.addBox(-1.5000F, -0.4500F, -3.0F, 3.0F, 0.9000F, 3.8000F, 0.0F);
        this.setRotateAngle(arm_left_foot, 0.0F, -0.4363F, 0.0F);
        this.arm_right_upper = new AdvancedModelBox(this, 78, 27);
        this.arm_right_upper.setRotationPoint(-3.4000F, 1.0F, -4.0F);
        this.arm_right_upper.addBox(-3.8000F, -1.2000F, -1.2000F, 4.6000F, 2.4000F, 2.4000F, 0.0F);
        this.setRotateAngle(arm_right_upper, 0.0F, 0.0F, -0.4189F);
        this.arm_right_lower = new AdvancedModelBox(this, 193, 17);
        this.arm_right_lower.setRotationPoint(-3.2000F, 0.0F, 0.0F);
        this.arm_right_lower.addBox(-1.2000F, -0.8000F, -0.9500F, 2.4000F, 4.6673F, 1.9000F, 0.0F);
        this.setRotateAngle(arm_right_lower, 0.0F, 0.0F, 0.4189F);
        this.arm_right_foot = new AdvancedModelBox(this, 92, 27);
        this.arm_right_foot.setRotationPoint(0.0F, 3.8673F, 0.0F);
        this.arm_right_foot.addBox(-1.5000F, -0.4500F, -3.0F, 3.0F, 0.9000F, 3.8000F, 0.0F);
        this.setRotateAngle(arm_right_foot, 0.0F, 0.4363F, 0.0F);
        this.body_hips = new AdvancedModelBox(this, 182, 0);
        this.body_hips.setRotationPoint(0.0F, 0.0F, 4.5000F);
        this.body_hips.addBox(-4.0F, -2.4000F, -0.5000F, 8.0F, 5.1000F, 4.6000F, 0.0F);
        this.setRotateAngle(body_hips, 0.0175F, 0.0F, 0.0F);
        this.scutes_hips = new AdvancedModelBox(this, 158, 27);
        this.scutes_hips.setRotationPoint(0.0F, -2.4000F, 1.5000F);
        this.scutes_hips.addBox(-2.6000F, -0.5000F, -0.6000F, 5.2000F, 0.9000F, 2.8000F, 0.0F);
        this.tail_1 = new AdvancedModelBox(this, 75, 0);
        this.tail_1.setRotationPoint(0.0F, -0.2000F, 3.7000F);
        this.tail_1.addBox(-3.0F, -2.0F, -0.8000F, 6.0F, 4.4000F, 7.3000F, 0.0F);
        this.setRotateAngle(tail_1, -0.1485F, 0.0F, 0.0F);
        this.tail_2 = new AdvancedModelBox(this, 158, 0);
        this.tail_2.setRotationPoint(0.0F, 0.0F, 6.5000F);
        this.tail_2.addBox(-2.3000F, -1.6000F, -0.8000F, 4.6000F, 3.5000F, 7.3000F, 0.0F);
        this.setRotateAngle(tail_2, -0.0983F, 0.0F, 0.0F);
        this.tail_3 = new AdvancedModelBox(this, 234, 0);
        this.tail_3.setRotationPoint(0.0F, 0.0F, 6.5000F);
        this.tail_3.addBox(-1.7000F, -1.3500F, -0.8000F, 3.4000F, 2.9000F, 6.8000F, 0.0F);
        this.setRotateAngle(tail_3, -0.0983F, 0.0F, 0.0F);
        this.tail_4 = new AdvancedModelBox(this, 0, 17);
        this.tail_4.setRotationPoint(0.0F, 0.0F, 6.0F);
        this.tail_4.addBox(-1.2000F, -1.1000F, -0.7000F, 2.4000F, 2.3500F, 6.7000F, 0.0F);
        this.setRotateAngle(tail_4, 0.1310F, 0.0F, 0.0F);
        this.tail_5 = new AdvancedModelBox(this, 19, 17);
        this.tail_5.setRotationPoint(0.0F, 0.0F, 6.0F);
        this.tail_5.addBox(-0.8000F, -0.8500F, -0.7000F, 1.6000F, 1.8500F, 6.2000F, 0.0F);
        this.setRotateAngle(tail_5, 0.1310F, 0.0F, 0.0F);
        this.tail_6 = new AdvancedModelBox(this, 134, 17);
        this.tail_6.setRotationPoint(0.0F, 0.0F, 5.5000F);
        this.tail_6.addBox(-0.4500F, -0.6000F, -0.6000F, 0.9000F, 1.3500F, 5.6000F, 0.0F);
        this.setRotateAngle(tail_6, 0.0655F, 0.0F, 0.0F);
        this.tail_4_crest = new AdvancedModelBox(this, 24, 27);
        this.tail_4_crest.setRotationPoint(0.0F, -1.1000F, 3.0F);
        this.tail_4_crest.addBox(-0.2500F, -0.6000F, -2.4000F, 0.5000F, 0.9000F, 4.3000F, 0.0F);
        this.tail_3_crest = new AdvancedModelBox(this, 13, 27);
        this.tail_3_crest.setRotationPoint(0.0F, -1.3500F, 3.0F);
        this.tail_3_crest.addBox(-0.4500F, -0.7000F, -2.4000F, 0.9000F, 1.0F, 4.3000F, 0.0F);
        this.tail_2_crest = new AdvancedModelBox(this, 0, 27);
        this.tail_2_crest.setRotationPoint(0.0F, -1.6000F, 3.2500F);
        this.tail_2_crest.addBox(-0.7000F, -0.8000F, -2.6500F, 1.4000F, 1.1000F, 4.8000F, 0.0F);
        this.tail_1_crest = new AdvancedModelBox(this, 148, 17);
        this.tail_1_crest.setRotationPoint(0.0F, -2.0F, 3.2500F);
        this.tail_1_crest.addBox(-1.0F, -0.9000F, -2.6500F, 2.0F, 1.2000F, 4.8000F, 0.0F);
        this.leg_left_upper = new AdvancedModelBox(this, 202, 17);
        this.leg_left_upper.setRotationPoint(3.4000F, 1.0F, 1.7000F);
        this.leg_left_upper.addBox(-0.8000F, -1.4500F, -1.4500F, 5.4000F, 2.9000F, 2.9000F, 0.0F);
        this.setRotateAngle(leg_left_upper, 0.0F, 0.0F, 0.3491F);
        this.leg_left_lower = new AdvancedModelBox(this, 162, 17);
        this.leg_left_lower.setRotationPoint(4.0F, 0.0F, 0.0F);
        this.leg_left_lower.addBox(-1.4000F, -0.8000F, -1.1500F, 2.8000F, 4.5000F, 2.3000F, 0.0F);
        this.setRotateAngle(leg_left_lower, 0.0F, 0.0F, -0.3491F);
        this.leg_left_foot = new AdvancedModelBox(this, 79, 17);
        this.leg_left_foot.setRotationPoint(0.0F, 3.7000F, 0.0F);
        this.leg_left_foot.addBox(-1.7000F, -0.4500F, -4.6000F, 3.4000F, 0.9000F, 5.7000F, 0.0F);
        this.setRotateAngle(leg_left_foot, 0.0F, -0.5236F, 0.0F);
        this.leg_right_upper = new AdvancedModelBox(this, 219, 17);
        this.leg_right_upper.setRotationPoint(-3.4000F, 1.0F, 1.7000F);
        this.leg_right_upper.addBox(-4.6000F, -1.4500F, -1.4500F, 5.4000F, 2.9000F, 2.9000F, 0.0F);
        this.setRotateAngle(leg_right_upper, 0.0F, 0.0F, -0.3491F);
        this.leg_right_lower = new AdvancedModelBox(this, 173, 17);
        this.leg_right_lower.setRotationPoint(-4.0F, 0.0F, 0.0F);
        this.leg_right_lower.addBox(-1.4000F, -0.8000F, -1.1500F, 2.8000F, 4.5000F, 2.3000F, 0.0F);
        this.setRotateAngle(leg_right_lower, 0.0F, 0.0F, 0.3491F);
        this.leg_right_foot = new AdvancedModelBox(this, 98, 17);
        this.leg_right_foot.setRotationPoint(0.0F, 3.7000F, 0.0F);
        this.leg_right_foot.addBox(-1.7000F, -0.4500F, -4.6000F, 3.4000F, 0.9000F, 5.7000F, 0.0F);
        this.setRotateAngle(leg_right_foot, 0.0F, 0.5236F, 0.0F);
        this.scutes_back = new AdvancedModelBox(this, 102, 0);
        this.scutes_back.setRotationPoint(0.0F, -2.8000F, 0.0F);
        this.scutes_back.addBox(-3.1000F, -0.5000F, -5.5000F, 6.2000F, 1.1000F, 9.1000F, 0.0F);

        this.body_mid.addChild(this.body_chest);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.snout_base);
        this.snout_base.addChild(this.snout_tip);
        this.snout_tip.addChild(this.teeth_tip_left);
        this.snout_tip.addChild(this.teeth_tip_right);
        this.snout_tip.addChild(this.nose_knob);
        this.snout_base.addChild(this.teeth_left);
        this.snout_base.addChild(this.teeth_right);
        this.head.addChild(this.jaw);
        this.jaw.addChild(this.jaw_tip);
        this.head.addChild(this.eye_bump_left);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.horn_left);
        this.head.addChild(this.eye_bump_right);
        this.head.addChild(this.eye_right);
        this.head.addChild(this.horn_right);
        this.neck.addChild(this.scutes_neck);
        this.body_chest.addChild(this.arm_left_upper);
        this.arm_left_upper.addChild(this.arm_left_lower);
        this.arm_left_lower.addChild(this.arm_left_foot);
        this.body_chest.addChild(this.arm_right_upper);
        this.arm_right_upper.addChild(this.arm_right_lower);
        this.arm_right_lower.addChild(this.arm_right_foot);
        this.body_mid.addChild(this.body_hips);
        this.body_hips.addChild(this.scutes_hips);
        this.body_hips.addChild(this.tail_1);
        this.tail_1.addChild(this.tail_2);
        this.tail_2.addChild(this.tail_3);
        this.tail_3.addChild(this.tail_4);
        this.tail_4.addChild(this.tail_5);
        this.tail_5.addChild(this.tail_6);
        this.tail_4.addChild(this.tail_4_crest);
        this.tail_3.addChild(this.tail_3_crest);
        this.tail_2.addChild(this.tail_2_crest);
        this.tail_1.addChild(this.tail_1_crest);
        this.body_hips.addChild(this.leg_left_upper);
        this.leg_left_upper.addChild(this.leg_left_lower);
        this.leg_left_lower.addChild(this.leg_left_foot);
        this.body_hips.addChild(this.leg_right_upper);
        this.leg_right_upper.addChild(this.leg_right_lower);
        this.leg_right_lower.addChild(this.leg_right_foot);
        this.body_mid.addChild(this.scutes_back);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_mid);
    }

    // Built once: getAllParts() is called every frame, and a fresh ImmutableList per animal per frame
    // was pure garbage (2.8.0, same as ModelBison).
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
                body_mid,
                body_chest,
                neck,
                head,
                snout_base,
                snout_tip,
                teeth_tip_left,
                teeth_tip_right,
                nose_knob,
                teeth_left,
                teeth_right,
                jaw,
                jaw_tip,
                eye_bump_left,
                eye_left,
                horn_left,
                eye_bump_right,
                eye_right,
                horn_right,
                scutes_neck,
                arm_left_upper,
                arm_left_lower,
                arm_left_foot,
                arm_right_upper,
                arm_right_lower,
                arm_right_foot,
                body_hips,
                scutes_hips,
                tail_1,
                tail_2,
                tail_3,
                tail_4,
                tail_5,
                tail_6,
                tail_4_crest,
                tail_3_crest,
                tail_2_crest,
                tail_1_crest,
                leg_left_upper,
                leg_left_lower,
                leg_left_foot,
                leg_right_upper,
                leg_right_lower,
                leg_right_foot,
                scutes_back
        );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityCrocodilian croc = (EntityCrocodilian) entityIn;
        animator.update(croc);

        // ATTACK_BITE (12t): the head lifts and the jaw gapes, then it snaps shut and the head drives down
        animator.setAnimation(EntityCrocodilian.ATTACK_BITE);
        animator.startKeyframe(5);
        this.rotate(animator, neck, -8.0F, 0, 0);
        this.rotate(animator, head, -16.0F, 0, 0);
        this.rotate(animator, jaw, 34.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(3);
        this.rotate(animator, neck, 6.0F, 0, 0);
        this.rotate(animator, head, 4.0F, 0, 0);
        this.rotate(animator, jaw, 0, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityCrocodilian croc, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(croc);
        limbSwingAmount = Math.min(0.5F, limbSwingAmount);
        float amt = Math.min(1.0F, limbSwingAmount / 0.35F);
        float gait = limbSwing * 0.55F / Math.max(0.3F, croc.getMobSize() * croc.getScale());
        int variant = croc.getVariant();

        // ---- per species (every frame: one model instance is shared by every crocodilian) ----
        float sx, sy;
        switch (variant) {
            case 0 -> { sx = 1.0F; sy = 1.0F; }   // nile
            case 1 -> { sx = 1.0500F; sy = 1.0500F; }   // saltwater
            case 2 -> { sx = 1.3000F; sy = 1.0F; }   // mugger
            case 3 -> { sx = 1.1500F; sy = 1.0500F; }   // black_caiman
            case 4 -> { sx = 0.8000F; sy = 1.3500F; }   // quinkana
            case 5 -> { sx = 1.2000F; sy = 1.2000F; }   // voay
            default -> { sx = 1.0F; sy = 1.0F; }
        }
        this.snout_base.setShouldScaleChildren(true);
        this.snout_base.setScale(sx, sy, 1.0F);
        this.jaw.setShouldScaleChildren(true);
        this.jaw.setScale(sx, sy, 1.0F);
        boolean horns = variant == 5;
        this.horn_left.showModel = horns;
        this.horn_right.showModel = horns;

        // QUINKANA's erect stance (solved), faded out as it lies down -- progressRotation adds to the current pose
        if (variant == 4) {
            float stand = 1.0F - Math.max(croc.sitProgress, croc.sleepProgress) / 40.0F;
            this.body_mid.rotationPointY -= 1.9082F * stand;
            this.arm_left_upper.rotateAngleZ += 1.1345F * stand;
            this.arm_left_lower.rotateAngleZ += -1.1345F * stand;
            this.arm_right_upper.rotateAngleZ += -1.1345F * stand;
            this.arm_right_lower.rotateAngleZ += 1.1345F * stand;
            this.leg_left_upper.rotateAngleZ += 0.6109F * stand;
            this.leg_left_lower.rotateAngleZ += -0.6109F * stand;
            this.leg_right_upper.rotateAngleZ += -0.6109F * stand;
            this.leg_right_lower.rotateAngleZ += 0.6109F * stand;
            this.tail_1.rotateAngleX += -0.0446F * stand;
            this.tail_2.rotateAngleX += -0.0335F * stand;
            this.tail_3.rotateAngleX += -0.0335F * stand;
            this.tail_4.rotateAngleX += 0.0446F * stand;
            this.tail_5.rotateAngleX += 0.0446F * stand;
            this.tail_6.rotateAngleX += 0.0223F * stand;
        }

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 26) * 0.025F);
        this.body_mid.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(tail_5, 0.05F, 0.08F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_6, 0.05F, 0.12F, false, -0.8F, 0F, ageInTicks, 1);

        // Closing the eyes: slide the eye planes 0.6 into their sockets; y/z keep the constructor's values.
        if (!croc.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(2.9000F, -1.1500F, -1.8000F);
            this.eye_right.setRotationPoint(-2.9000F, -1.1500F, -1.8000F);
        }

        if (!croc.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        // ATTACK_ROLL -- the death roll: the whole animal spins twice about its long axis (it only fires in water)
        if (croc.getAnimation() == EntityCrocodilian.ATTACK_ROLL) {
            float t = (croc.getAnimationTick() + (ageInTicks - (int) ageInTicks)) / EntityCrocodilian.ATTACK_ROLL.getDuration();
            this.body_mid.rotateAngleZ += t * Mth.TWO_PI * 2.0F;
            this.neck.rotateAngleY += Mth.sin(t * Mth.TWO_PI * 4.0F) * 0.25F;
            this.tail_1.rotateAngleY += Mth.sin(t * Mth.TWO_PI * 4.0F) * 0.3F;
        }

        if (croc.isInWater() && !croc.isOnGround()) {
            // swimming: legs pressed back along the flanks, the tail sculls from side to side
            float sw = ageInTicks * 0.25F;
            this.arm_left_upper.rotateAngleY += 1.0F;
            this.arm_right_upper.rotateAngleY -= 1.0F;
            this.leg_left_upper.rotateAngleY += 1.0F;
            this.leg_right_upper.rotateAngleY -= 1.0F;
            this.tail_1.rotateAngleY += Mth.cos(sw) * 0.18F;
            this.tail_2.rotateAngleY += Mth.cos(sw - 0.7F) * 0.26F;
            this.tail_3.rotateAngleY += Mth.cos(sw - 1.4F) * 0.32F;
            this.tail_4.rotateAngleY += Mth.cos(sw - 2.1F) * 0.36F;
            this.tail_5.rotateAngleY += Mth.cos(sw - 2.8F) * 0.40F;
            this.tail_6.rotateAngleY += Mth.cos(sw - 3.5F) * 0.44F;
            this.body_mid.rotateAngleY += Mth.cos(sw + 0.7F) * 0.06F;
        }
        else if (croc.canMove()) {
            // ---- sprawling 'high walk': diagonal pairs swing about the vertical axis, the trunk and tail undulate ----
            float c = Mth.cos(gait) * amt, sn = Mth.sin(gait);
            this.arm_left_upper.rotateAngleY += c * 0.5F;
            this.arm_right_upper.rotateAngleY += c * 0.5F;
            this.leg_left_upper.rotateAngleY -= c * 0.5F;
            this.leg_right_upper.rotateAngleY -= c * 0.5F;
            this.arm_left_upper.rotateAngleZ -= Math.max(0, sn) * 0.25F * amt;
            this.leg_right_upper.rotateAngleZ += Math.max(0, sn) * 0.25F * amt;
            this.arm_right_upper.rotateAngleZ += Math.max(0, -sn) * 0.25F * amt;
            this.leg_left_upper.rotateAngleZ -= Math.max(0, -sn) * 0.25F * amt;
            this.body_mid.rotateAngleY += c * 0.07F;
            this.neck.rotateAngleY -= c * 0.08F;
            this.tail_1.rotateAngleY -= Mth.cos(gait - 0.6F) * 0.10F * amt;
            this.tail_2.rotateAngleY -= Mth.cos(gait - 1.2F) * 0.13F * amt;
            this.tail_3.rotateAngleY -= Mth.cos(gait - 1.8F) * 0.16F * amt;
            this.tail_4.rotateAngleY -= Mth.cos(gait - 2.4F) * 0.18F * amt;
            this.tail_5.rotateAngleY -= Mth.cos(gait - 3.0F) * 0.20F * amt;
            this.tail_6.rotateAngleY -= Mth.cos(gait - 3.6F) * 0.22F * amt;
        }

        if (croc.sitProgress > 0) {
            // SOLVED (crocodilian_spec.py --pose): belly on the ground, legs splayed with the feet flat, tail resting, head up
            this.progressPosition(body_mid, croc.sitProgress, 0.0F, 20.5000F, 0.0F, 40);
            this.progressRotation(neck, croc.sitProgress, 0.0448F, -0.0F, 0.0F, 40);
            this.progressRotation(arm_left_upper, croc.sitProgress, -0.0F, -0.0F, -0.5518F, 40);
            this.progressRotation(arm_left_lower, croc.sitProgress, -0.3972F, -0.0F, 0.3593F, 40);
            this.progressRotation(arm_right_upper, croc.sitProgress, -0.0F, -0.0F, 0.5518F, 40);
            this.progressRotation(arm_right_lower, croc.sitProgress, -0.3972F, -0.0F, -0.3593F, 40);
            this.progressRotation(tail_1, croc.sitProgress, -0.0750F, -0.0F, 0.0F, 40);
            this.progressRotation(tail_2, croc.sitProgress, -0.0431F, -0.0F, 0.0F, 40);
            this.progressRotation(tail_3, croc.sitProgress, -0.0431F, -0.0F, 0.0F, 40);
            this.progressRotation(tail_4, croc.sitProgress, 0.0575F, -0.0F, 0.0F, 40);
            this.progressRotation(tail_5, croc.sitProgress, 0.0575F, -0.0F, 0.0F, 40);
            this.progressRotation(tail_6, croc.sitProgress, 0.0288F, -0.0F, 0.0F, 40);
            this.progressRotation(leg_left_upper, croc.sitProgress, -0.0F, -0.0F, -0.3384F, 40);
            this.progressRotation(leg_left_lower, croc.sitProgress, -0.0560F, -0.0F, -0.6988F, 40);
            this.progressRotation(leg_right_upper, croc.sitProgress, -0.0F, -0.0F, 0.3384F, 40);
            this.progressRotation(leg_right_lower, croc.sitProgress, -0.0560F, -0.0F, 0.6988F, 40);
            // BASKING: the head (upper jaw) lifts and the jaw counter-rotates, so the lower jaw stays on the ground
            if (croc.isBasking()) {
                this.head.rotateAngleX -= 0.5585F * croc.sitProgress / 40.0F;
                this.jaw.rotateAngleX += 0.5585F * croc.sitProgress / 40.0F;
            }
        }
        else if (croc.sleepProgress > 0) {
            // SOLVED (crocodilian_spec.py --pose): belly on the ground, legs splayed with the feet flat, tail resting, chin down, tail curled
            this.progressPosition(body_mid, croc.sleepProgress, 0.0F, 20.5000F, 0.0F, 40);
            this.progressRotation(neck, croc.sleepProgress, 0.1094F, -0.0F, 0.0F, 40);
            this.progressRotation(arm_left_upper, croc.sleepProgress, -0.0F, -0.0F, -0.1214F, 40);
            this.progressRotation(arm_left_lower, croc.sleepProgress, -0.6848F, -0.0F, -1.0134F, 40);
            this.progressRotation(arm_right_upper, croc.sleepProgress, -0.0F, -0.0F, 0.1214F, 40);
            this.progressRotation(arm_right_lower, croc.sleepProgress, -0.6848F, -0.0F, 1.0134F, 40);
            this.progressRotation(tail_1, croc.sleepProgress, -0.0746F, -0.1396F, 0.0F, 40);
            this.progressRotation(tail_2, croc.sleepProgress, -0.0429F, -0.1396F, 0.0F, 40);
            this.progressRotation(tail_3, croc.sleepProgress, -0.0429F, -0.1396F, 0.0F, 40);
            this.progressRotation(tail_4, croc.sleepProgress, 0.0572F, -0.1396F, 0.0F, 40);
            this.progressRotation(tail_5, croc.sleepProgress, 0.0572F, -0.1396F, 0.0F, 40);
            this.progressRotation(tail_6, croc.sleepProgress, 0.0286F, -0.1396F, 0.0F, 40);
            this.progressRotation(leg_left_upper, croc.sleepProgress, -0.0F, -0.0F, -0.5218F, 40);
            this.progressRotation(leg_left_lower, croc.sleepProgress, 0.1307F, -0.0F, 0.2234F, 40);
            this.progressRotation(leg_right_upper, croc.sleepProgress, -0.0F, -0.0F, 0.5218F, 40);
            this.progressRotation(leg_right_lower, croc.sleepProgress, 0.1307F, -0.0F, -0.2234F, 40);
        }
    }
}
