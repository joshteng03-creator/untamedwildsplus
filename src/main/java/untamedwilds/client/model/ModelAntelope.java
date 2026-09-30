package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityAntelope;

/**
 * 56 boxes on 256x128. The original 36-box gazelle rebuild is UNCHANGED -- every one of its
 * texOffs, addBox and PartPose literals is byte-identical, which is what lets saiga.png,
 * pronghorn.png and springbok.png keep working untouched. The 20 new boxes are packed into
 * rows 53..65, which were empty (the shipped footprints stop at v=52).
 *
 * FIVE horn families now share the one skull, all hanging off the same pedicle:
 *     GAZELLE   horn_base + horn_tip                springbok, pronghorn (+ front prong)
 *     FORKED    gazelle + front AND rear prong      tetrameryx  (the 4-horned pronghorn)
 *     SPIRAL    3 chained, progressively yawed      giant_eland, blackbuck
 *     RAPIER    2 chained, ruler-straight           gemsbok
 *     SCIMITAR  3 chained, arcing back              sable, bluebuck
 *
 * Families are selected with showModel on the chain ROOT, not setScale(0): ModelPart.render
 * returns before recursing into children when visible is false, so hiding the root hides
 * the whole chain -- and a zero scale would have to be paired with setShouldScaleChildren,
 * which fights the per-species sizing below.
 *
 * Segment-to-segment TWIST is load-bearing, not decoration: a box's X-face normal is
 * untouched by rx, so a chain that only varies its backsweep leaves consecutive side planes
 * near-parallel and z-fighting. Every joint here turns >8 degrees of yaw or roll.
 *
 * antelope.json declares one flag (hasBulbousNose, saiga); everything else -- horn family,
 * build, size -- reads getVariant(), the ModelEquid / ModelDeer pattern. All nine scales are
 * withers_m * 16 / 26.44, so 1 block = 1 metre at the shoulder.
 */
public class ModelAntelope extends AdvancedEntityModel<EntityAntelope> {

    private static final int SAIGA = 0;
    private static final int PRONGHORN = 1;
    private static final int SPRINGBOK = 2;
    private static final int TETRAMERYX = 3;
    private static final int BLACKBUCK = 4;
    private static final int GIANT_ELAND = 5;
    private static final int GEMSBOK = 6;
    private static final int SABLE = 7;
    private static final int BLUEBUCK = 8;

    private final AdvancedModelBox body_barrel;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck_base;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox muzzle;
    private final AdvancedModelBox nose_bulb;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox horn_base_left;
    private final AdvancedModelBox horn_tip_left;
    private final AdvancedModelBox horn_prong_left;
    private final AdvancedModelBox horn_prong_rear_left;
    private final AdvancedModelBox horn_base_right;
    private final AdvancedModelBox horn_tip_right;
    private final AdvancedModelBox horn_prong_right;
    private final AdvancedModelBox horn_prong_rear_right;
    private final AdvancedModelBox spiral_1_left;
    private final AdvancedModelBox spiral_2_left;
    private final AdvancedModelBox spiral_3_left;
    private final AdvancedModelBox rapier_1_left;
    private final AdvancedModelBox rapier_2_left;
    private final AdvancedModelBox scimitar_1_left;
    private final AdvancedModelBox scimitar_2_left;
    private final AdvancedModelBox scimitar_3_left;
    private final AdvancedModelBox spiral_1_right;
    private final AdvancedModelBox spiral_2_right;
    private final AdvancedModelBox spiral_3_right;
    private final AdvancedModelBox rapier_1_right;
    private final AdvancedModelBox rapier_2_right;
    private final AdvancedModelBox scimitar_1_right;
    private final AdvancedModelBox scimitar_2_right;
    private final AdvancedModelBox scimitar_3_right;
    private final AdvancedModelBox neck_mane;
    private final AdvancedModelBox dewlap;
    private final AdvancedModelBox arm_left_1;
    private final AdvancedModelBox arm_left_2;
    private final AdvancedModelBox arm_left_3;
    private final AdvancedModelBox hoof_front_left;
    private final AdvancedModelBox arm_right_1;
    private final AdvancedModelBox arm_right_2;
    private final AdvancedModelBox arm_right_3;
    private final AdvancedModelBox hoof_front_right;
    private final AdvancedModelBox body_croup;
    private final AdvancedModelBox leg_left_1;
    private final AdvancedModelBox leg_left_2;
    private final AdvancedModelBox leg_left_3;
    private final AdvancedModelBox hoof_back_left;
    private final AdvancedModelBox leg_right_1;
    private final AdvancedModelBox leg_right_2;
    private final AdvancedModelBox leg_right_3;
    private final AdvancedModelBox hoof_back_right;
    private final AdvancedModelBox tail_dock;
    private final AdvancedModelBox tail_tuft;

    private final ModelAnimator animator;

    public ModelAntelope() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_barrel = new AdvancedModelBox(this, 45, 0);
        this.body_barrel.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.body_barrel.addBox(-4.7500F, -4.4000F, -7.5000F, 9.5000F, 9.8000F, 15.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 0, 0);
        this.body_chest.setRotationPoint(0.0F, -3.0F, -6.0F);
        this.body_chest.addBox(-4.1500F, -3.0F, -8.5000F, 8.3000F, 12.6000F, 13.5000F, 0.0F);
        this.setRotateAngle(body_chest, -0.0524F, 0.0F, 0.0F);
        this.neck_base = new AdvancedModelBox(this, 228, 0);
        this.neck_base.setRotationPoint(0.0F, 0.0F, -5.5000F);
        this.neck_base.addBox(-2.7000F, -6.5000F, -3.1000F, 5.4000F, 8.0F, 6.2000F, 0.0F);
        this.setRotateAngle(neck_base, 0.4887F, 0.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 122, 28);
        this.neck.setRotationPoint(0.0F, -6.0F, 0.0F);
        this.neck.addBox(-1.6000F, -6.5000F, -2.4000F, 3.2000F, 7.0F, 4.8000F, 0.0F);
        this.setRotateAngle(neck, 0.1745F, 0.0F, 0.0F);
        this.head = new AdvancedModelBox(this, 0, 28);
        this.head.setRotationPoint(0.0F, -6.0F, -0.5000F);
        this.head.addBox(-2.0F, -3.6000F, -7.0F, 4.0F, 6.4000F, 8.0F, 0.0F);
        this.setRotateAngle(head, -0.5236F, 0.0F, 0.0F);
        this.muzzle = new AdvancedModelBox(this, 139, 28);
        this.muzzle.setRotationPoint(0.0F, 0.5000F, -7.0F);
        this.muzzle.addBox(-1.3000F, -1.9000F, -4.8000F, 2.6000F, 3.8000F, 6.2000F, 0.0F);
        this.nose_bulb = new AdvancedModelBox(this, 103, 28);
        this.nose_bulb.setRotationPoint(0.0F, -0.5000F, -2.0F);
        this.nose_bulb.addBox(-1.7000F, -3.4000F, -4.4000F, 3.4000F, 6.2000F, 5.6000F, 0.0F);
        this.setRotateAngle(nose_bulb, 0.2094F, 0.0F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 237, 28);
        this.ear_left.setRotationPoint(2.0F, -2.6000F, -0.5000F);
        this.ear_left.addBox(-0.7000F, -5.0F, -1.8000F, 1.4000F, 5.4000F, 3.6000F, 0.0F);
        this.setRotateAngle(ear_left, 0.1745F, 0.4363F, 0.7330F);
        this.ear_right = new AdvancedModelBox(this, 225, 28);
        this.ear_right.setRotationPoint(-2.0F, -2.6000F, -0.5000F);
        this.ear_right.addBox(-0.7000F, -5.0F, -1.8000F, 1.4000F, 5.4000F, 3.6000F, 0.0F);
        this.setRotateAngle(ear_right, 0.1745F, -0.4363F, -0.7330F);
        this.eye_left = new AdvancedModelBox(this, 79, 44);
        this.eye_left.setRotationPoint(2.6000F, -1.0F, -3.2000F);
        this.eye_left.addBox(0.0F, -1.5000F, -1.5000F, 0.0F, 3.0F, 3.0F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 72, 44);
        this.eye_right.setRotationPoint(-2.6000F, -1.0F, -3.2000F);
        this.eye_right.addBox(0.0F, -1.5000F, -1.5000F, 0.0F, 3.0F, 3.0F, 0.0F);
        this.horn_base_left = new AdvancedModelBox(this, 0, 44);
        this.horn_base_left.setRotationPoint(1.0500F, -3.0F, -1.6000F);
        this.horn_base_left.addBox(-0.4500F, -5.4000F, -0.8000F, 0.9500F, 6.3000F, 1.5000F, 0.0F);
        this.setRotateAngle(horn_base_left, -0.1396F, 0.0F, 0.1396F);
        this.horn_tip_left = new AdvancedModelBox(this, 67, 44);
        this.horn_tip_left.setRotationPoint(-0.0300F, -5.2000F, -0.1500F);
        this.horn_tip_left.addBox(-0.4000F, -4.6000F, -0.4500F, 0.8000F, 5.4000F, 0.9000F, 0.0F);
        this.setRotateAngle(horn_tip_left, 0.2443F, 0.0F, -0.2094F);
        this.horn_prong_left = new AdvancedModelBox(this, 94, 44);
        this.horn_prong_left.setRotationPoint(0.0F, -3.5000F, -0.7000F);
        this.horn_prong_left.addBox(-0.4500F, -0.9000F, -2.3000F, 0.9000F, 1.8000F, 2.4000F, 0.0F);
        this.setRotateAngle(horn_prong_left, -0.4363F, 0.2618F, 0.0F);
        this.horn_prong_rear_left = new AdvancedModelBox(this, 96, 53);
        this.horn_prong_rear_left.setRotationPoint(0.0F, -2.6000F, 0.7000F);
        this.horn_prong_rear_left.addBox(-0.4500F, -0.9000F, -0.1000F, 0.9000F, 1.8000F, 2.4000F, 0.0F);
        this.setRotateAngle(horn_prong_rear_left, 0.3491F, 0.2618F, 0.0F);
        this.horn_base_right = new AdvancedModelBox(this, 249, 28);
        this.horn_base_right.setRotationPoint(-1.0500F, -3.0F, -1.6000F);
        this.horn_base_right.addBox(-0.5000F, -5.4000F, -0.8000F, 0.9500F, 6.3000F, 1.5000F, 0.0F);
        this.setRotateAngle(horn_base_right, -0.1396F, 0.0F, -0.1396F);
        this.horn_tip_right = new AdvancedModelBox(this, 62, 44);
        this.horn_tip_right.setRotationPoint(0.0300F, -5.2000F, -0.1500F);
        this.horn_tip_right.addBox(-0.4000F, -4.6000F, -0.4500F, 0.8000F, 5.4000F, 0.9000F, 0.0F);
        this.setRotateAngle(horn_tip_right, 0.2443F, 0.0F, 0.2094F);
        this.horn_prong_right = new AdvancedModelBox(this, 86, 44);
        this.horn_prong_right.setRotationPoint(0.0F, -3.5000F, -0.7000F);
        this.horn_prong_right.addBox(-0.4500F, -0.9000F, -2.3000F, 0.9000F, 1.8000F, 2.4000F, 0.0F);
        this.setRotateAngle(horn_prong_right, -0.4363F, -0.2618F, 0.0F);
        this.horn_prong_rear_right = new AdvancedModelBox(this, 103, 53);
        this.horn_prong_rear_right.setRotationPoint(0.0F, -2.6000F, 0.7000F);
        this.horn_prong_rear_right.addBox(-0.4500F, -0.9000F, -0.1000F, 0.9000F, 1.8000F, 2.4000F, 0.0F);
        this.setRotateAngle(horn_prong_rear_right, 0.3491F, -0.2618F, 0.0F);
        this.spiral_1_left = new AdvancedModelBox(this, 36, 53);
        this.spiral_1_left.setRotationPoint(1.0500F, -3.0F, -1.6000F);
        this.spiral_1_left.addBox(-0.5800F, -5.6000F, -0.7500F, 1.1600F, 6.4000F, 1.5000F, 0.0F);
        this.setRotateAngle(spiral_1_left, -0.2094F, 0.1396F, 0.2094F);
        this.spiral_2_left = new AdvancedModelBox(this, 70, 53);
        this.spiral_2_left.setRotationPoint(0.0F, -5.4000F, 0.0F);
        this.spiral_2_left.addBox(-0.4600F, -5.0F, -0.6000F, 0.9200F, 5.8000F, 1.2000F, 0.0F);
        this.setRotateAngle(spiral_2_left, -0.1396F, 0.3491F, 0.1047F);
        this.spiral_3_left = new AdvancedModelBox(this, 80, 53);
        this.spiral_3_left.setRotationPoint(0.0F, -4.8000F, 0.0F);
        this.spiral_3_left.addBox(-0.3600F, -4.4000F, -0.5000F, 0.7200F, 5.2000F, 1.0F, 0.0F);
        this.setRotateAngle(spiral_3_left, -0.1047F, 0.5585F, 0.0873F);
        this.rapier_1_left = new AdvancedModelBox(this, 0, 53);
        this.rapier_1_left.setRotationPoint(1.0500F, -3.0F, -1.6000F);
        this.rapier_1_left.addBox(-0.5200F, -9.0F, -0.7000F, 1.0400F, 9.8000F, 1.4000F, 0.0F);
        this.setRotateAngle(rapier_1_left, -0.1047F, 0.0F, 0.1222F);
        this.rapier_2_left = new AdvancedModelBox(this, 10, 53);
        this.rapier_2_left.setRotationPoint(0.0F, -9.0F, 0.0F);
        this.rapier_2_left.addBox(-0.4000F, -9.0F, -0.6000F, 0.8000F, 9.8000F, 1.2000F, 0.0F);
        this.setRotateAngle(rapier_2_left, -0.0698F, 0.2094F, -0.0698F);
        this.scimitar_1_left = new AdvancedModelBox(this, 42, 53);
        this.scimitar_1_left.setRotationPoint(1.0500F, -3.0F, -1.6000F);
        this.scimitar_1_left.addBox(-0.5800F, -5.4000F, -0.7500F, 1.1600F, 6.2000F, 1.5000F, 0.0F);
        this.setRotateAngle(scimitar_1_left, -0.2793F, 0.0F, 0.1571F);
        this.scimitar_2_left = new AdvancedModelBox(this, 60, 53);
        this.scimitar_2_left.setRotationPoint(0.0F, -5.2000F, 0.0F);
        this.scimitar_2_left.addBox(-0.4600F, -5.0F, -0.6000F, 0.9200F, 5.8000F, 1.2000F, 0.0F);
        this.setRotateAngle(scimitar_2_left, -0.4189F, 0.1745F, 0.0873F);
        this.scimitar_3_left = new AdvancedModelBox(this, 84, 53);
        this.scimitar_3_left.setRotationPoint(0.0F, -4.8000F, 0.0F);
        this.scimitar_3_left.addBox(-0.3600F, -4.4000F, -0.5000F, 0.7200F, 5.2000F, 1.0F, 0.0F);
        this.setRotateAngle(scimitar_3_left, -0.5236F, 0.3491F, 0.0698F);
        this.spiral_1_right = new AdvancedModelBox(this, 48, 53);
        this.spiral_1_right.setRotationPoint(-1.0500F, -3.0F, -1.6000F);
        this.spiral_1_right.addBox(-0.5800F, -5.6000F, -0.7500F, 1.1600F, 6.4000F, 1.5000F, 0.0F);
        this.setRotateAngle(spiral_1_right, -0.2094F, -0.1396F, -0.2094F);
        this.spiral_2_right = new AdvancedModelBox(this, 75, 53);
        this.spiral_2_right.setRotationPoint(0.0F, -5.4000F, 0.0F);
        this.spiral_2_right.addBox(-0.4600F, -5.0F, -0.6000F, 0.9200F, 5.8000F, 1.2000F, 0.0F);
        this.setRotateAngle(spiral_2_right, -0.1396F, -0.3491F, -0.1047F);
        this.spiral_3_right = new AdvancedModelBox(this, 88, 53);
        this.spiral_3_right.setRotationPoint(0.0F, -4.8000F, 0.0F);
        this.spiral_3_right.addBox(-0.3600F, -4.4000F, -0.5000F, 0.7200F, 5.2000F, 1.0F, 0.0F);
        this.setRotateAngle(spiral_3_right, -0.1047F, -0.5585F, -0.0873F);
        this.rapier_1_right = new AdvancedModelBox(this, 5, 53);
        this.rapier_1_right.setRotationPoint(-1.0500F, -3.0F, -1.6000F);
        this.rapier_1_right.addBox(-0.5200F, -9.0F, -0.7000F, 1.0400F, 9.8000F, 1.4000F, 0.0F);
        this.setRotateAngle(rapier_1_right, -0.1047F, 0.0F, -0.1222F);
        this.rapier_2_right = new AdvancedModelBox(this, 14, 53);
        this.rapier_2_right.setRotationPoint(0.0F, -9.0F, 0.0F);
        this.rapier_2_right.addBox(-0.4000F, -9.0F, -0.6000F, 0.8000F, 9.8000F, 1.2000F, 0.0F);
        this.setRotateAngle(rapier_2_right, -0.0698F, -0.2094F, 0.0698F);
        this.scimitar_1_right = new AdvancedModelBox(this, 54, 53);
        this.scimitar_1_right.setRotationPoint(-1.0500F, -3.0F, -1.6000F);
        this.scimitar_1_right.addBox(-0.5800F, -5.4000F, -0.7500F, 1.1600F, 6.2000F, 1.5000F, 0.0F);
        this.setRotateAngle(scimitar_1_right, -0.2793F, 0.0F, -0.1571F);
        this.scimitar_2_right = new AdvancedModelBox(this, 65, 53);
        this.scimitar_2_right.setRotationPoint(0.0F, -5.2000F, 0.0F);
        this.scimitar_2_right.addBox(-0.4600F, -5.0F, -0.6000F, 0.9200F, 5.8000F, 1.2000F, 0.0F);
        this.setRotateAngle(scimitar_2_right, -0.4189F, -0.1745F, -0.0873F);
        this.scimitar_3_right = new AdvancedModelBox(this, 92, 53);
        this.scimitar_3_right.setRotationPoint(0.0F, -4.8000F, 0.0F);
        this.scimitar_3_right.addBox(-0.3600F, -4.4000F, -0.5000F, 0.7200F, 5.2000F, 1.0F, 0.0F);
        this.setRotateAngle(scimitar_3_right, -0.5236F, -0.3491F, -0.0698F);
        this.neck_mane = new AdvancedModelBox(this, 29, 53);
        this.neck_mane.setRotationPoint(0.0F, -3.0F, 2.5000F);
        this.neck_mane.addBox(-0.7500F, -3.2000F, -0.7000F, 1.5000F, 6.0F, 1.8000F, 0.0F);
        this.dewlap = new AdvancedModelBox(this, 18, 53);
        this.dewlap.setRotationPoint(0.0F, -3.0F, -3.0F);
        this.dewlap.addBox(-1.6000F, -1.0F, -1.5000F, 3.2000F, 7.5000F, 2.2000F, 0.0F);
        this.setRotateAngle(dewlap, -0.1396F, 0.0F, 0.0F);
        this.arm_left_1 = new AdvancedModelBox(this, 206, 0);
        this.arm_left_1.setRotationPoint(3.1000F, -0.4000F, 0.6000F);
        this.arm_left_1.addBox(-1.9000F, -0.4000F, -3.2000F, 3.8000F, 9.6000F, 6.4000F, 0.0F);
        this.setRotateAngle(arm_left_1, 0.0524F, 0.0F, -0.0349F);
        this.arm_left_2 = new AdvancedModelBox(this, 84, 28);
        this.arm_left_2.setRotationPoint(0.0F, 8.5000F, 0.2000F);
        this.arm_left_2.addBox(-1.6000F, -0.3000F, -2.7000F, 3.2000F, 7.8000F, 5.4000F, 0.0F);
        this.setRotateAngle(arm_left_2, -0.0698F, 0.0F, 0.0F);
        this.arm_left_3 = new AdvancedModelBox(this, 197, 28);
        this.arm_left_3.setRotationPoint(0.0F, 6.9000F, 0.0F);
        this.arm_left_3.addBox(-1.2000F, -0.4000F, -1.7000F, 2.4000F, 6.1000F, 3.4000F, 0.0F);
        this.setRotateAngle(arm_left_3, 0.0349F, 0.0F, 0.0F);
        this.hoof_front_left = new AdvancedModelBox(this, 18, 44);
        this.hoof_front_left.setRotationPoint(0.0F, 5.1400F, -0.4000F);
        this.hoof_front_left.addBox(-0.9000F, -0.2600F, -2.1000F, 1.8000F, 3.1000F, 3.4000F, 0.0F);
        this.setRotateAngle(hoof_front_left, 0.0349F, 0.0F, 0.0F);
        this.arm_right_1 = new AdvancedModelBox(this, 184, 0);
        this.arm_right_1.setRotationPoint(-3.1000F, -0.4000F, 0.6000F);
        this.arm_right_1.addBox(-1.9000F, -0.4000F, -3.2000F, 3.8000F, 9.6000F, 6.4000F, 0.0F);
        this.setRotateAngle(arm_right_1, 0.0524F, 0.0F, 0.0349F);
        this.arm_right_2 = new AdvancedModelBox(this, 65, 28);
        this.arm_right_2.setRotationPoint(0.0F, 8.5000F, 0.2000F);
        this.arm_right_2.addBox(-1.6000F, -0.3000F, -2.7000F, 3.2000F, 7.8000F, 5.4000F, 0.0F);
        this.setRotateAngle(arm_right_2, -0.0698F, 0.0F, 0.0F);
        this.arm_right_3 = new AdvancedModelBox(this, 184, 28);
        this.arm_right_3.setRotationPoint(0.0F, 6.9000F, 0.0F);
        this.arm_right_3.addBox(-1.2000F, -0.4000F, -1.7000F, 2.4000F, 6.1000F, 3.4000F, 0.0F);
        this.setRotateAngle(arm_right_3, 0.0349F, 0.0F, 0.0F);
        this.hoof_front_right = new AdvancedModelBox(this, 6, 44);
        this.hoof_front_right.setRotationPoint(0.0F, 5.1400F, -0.4000F);
        this.hoof_front_right.addBox(-0.9000F, -0.2600F, -2.1000F, 1.8000F, 3.1000F, 3.4000F, 0.0F);
        this.setRotateAngle(hoof_front_right, 0.0349F, 0.0F, 0.0F);
        this.body_croup = new AdvancedModelBox(this, 95, 0);
        this.body_croup.setRotationPoint(0.0F, -1.0F, 5.0F);
        this.body_croup.addBox(-4.4500F, -4.6000F, -4.5000F, 8.9000F, 10.2000F, 10.0F, 0.0F);
        this.setRotateAngle(body_croup, -0.1222F, 0.0F, 0.0F);
        this.leg_left_1 = new AdvancedModelBox(this, 159, 0);
        this.leg_left_1.setRotationPoint(3.2000F, -2.6000F, 0.5000F);
        this.leg_left_1.addBox(-2.2000F, -0.7000F, -4.0F, 4.4000F, 11.1000F, 7.6000F, 0.0F);
        this.setRotateAngle(leg_left_1, -0.1047F, 0.0F, -0.0349F);
        this.leg_left_2 = new AdvancedModelBox(this, 45, 28);
        this.leg_left_2.setRotationPoint(0.0F, 9.4000F, 0.0F);
        this.leg_left_2.addBox(-1.8000F, -1.0F, -2.9000F, 3.6000F, 9.2000F, 5.8000F, 0.0F);
        this.setRotateAngle(leg_left_2, 0.6807F, 0.0F, 0.0F);
        this.leg_left_3 = new AdvancedModelBox(this, 171, 28);
        this.leg_left_3.setRotationPoint(0.0F, 7.6000F, 0.0F);
        this.leg_left_3.addBox(-1.2000F, -1.0F, -1.8000F, 2.4000F, 6.4000F, 3.4000F, 0.0F);
        this.setRotateAngle(leg_left_3, -0.5760F, 0.0F, 0.0F);
        this.hoof_back_left = new AdvancedModelBox(this, 42, 44);
        this.hoof_back_left.setRotationPoint(0.0F, 5.0F, 0.0F);
        this.hoof_back_left.addBox(-0.9000F, -0.8500F, -2.5000F, 1.8000F, 3.4000F, 3.4000F, 0.0F);
        this.setRotateAngle(hoof_back_left, 0.1222F, 0.0F, 0.0F);
        this.leg_right_1 = new AdvancedModelBox(this, 134, 0);
        this.leg_right_1.setRotationPoint(-3.2000F, -2.6000F, 0.5000F);
        this.leg_right_1.addBox(-2.2000F, -0.7000F, -4.0F, 4.4000F, 11.1000F, 7.6000F, 0.0F);
        this.setRotateAngle(leg_right_1, -0.1047F, 0.0F, 0.0349F);
        this.leg_right_2 = new AdvancedModelBox(this, 25, 28);
        this.leg_right_2.setRotationPoint(0.0F, 9.4000F, 0.0F);
        this.leg_right_2.addBox(-1.8000F, -1.0F, -2.9000F, 3.6000F, 9.2000F, 5.8000F, 0.0F);
        this.setRotateAngle(leg_right_2, 0.6807F, 0.0F, 0.0F);
        this.leg_right_3 = new AdvancedModelBox(this, 158, 28);
        this.leg_right_3.setRotationPoint(0.0F, 7.6000F, 0.0F);
        this.leg_right_3.addBox(-1.2000F, -1.0F, -1.8000F, 2.4000F, 6.4000F, 3.4000F, 0.0F);
        this.setRotateAngle(leg_right_3, -0.5760F, 0.0F, 0.0F);
        this.hoof_back_right = new AdvancedModelBox(this, 30, 44);
        this.hoof_back_right.setRotationPoint(0.0F, 5.0F, 0.0F);
        this.hoof_back_right.addBox(-0.9000F, -0.8500F, -2.5000F, 1.8000F, 3.4000F, 3.4000F, 0.0F);
        this.setRotateAngle(hoof_back_right, 0.1222F, 0.0F, 0.0F);
        this.tail_dock = new AdvancedModelBox(this, 54, 44);
        this.tail_dock.setRotationPoint(0.0F, -3.0F, 5.4000F);
        this.tail_dock.addBox(-0.6000F, -0.6000F, -1.0F, 1.2000F, 4.6000F, 2.2000F, 0.0F);
        this.setRotateAngle(tail_dock, -0.1745F, 0.0F, 0.0F);
        this.tail_tuft = new AdvancedModelBox(this, 210, 28);
        this.tail_tuft.setRotationPoint(0.0F, 3.5000F, 0.1000F);
        this.tail_tuft.addBox(-1.5500F, -0.5000F, -0.9000F, 3.1000F, 5.0F, 3.8000F, 0.0F);
        this.setRotateAngle(tail_tuft, -0.1047F, 0.0F, 0.0F);

        this.body_barrel.addChild(this.body_chest);
        this.body_chest.addChild(this.neck_base);
        this.neck_base.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.muzzle);
        this.muzzle.addChild(this.nose_bulb);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.ear_right);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.eye_right);
        this.head.addChild(this.horn_base_left);
        this.horn_base_left.addChild(this.horn_tip_left);
        this.horn_base_left.addChild(this.horn_prong_left);
        this.horn_base_left.addChild(this.horn_prong_rear_left);
        this.head.addChild(this.horn_base_right);
        this.horn_base_right.addChild(this.horn_tip_right);
        this.horn_base_right.addChild(this.horn_prong_right);
        this.horn_base_right.addChild(this.horn_prong_rear_right);
        this.head.addChild(this.spiral_1_left);
        this.spiral_1_left.addChild(this.spiral_2_left);
        this.spiral_2_left.addChild(this.spiral_3_left);
        this.head.addChild(this.rapier_1_left);
        this.rapier_1_left.addChild(this.rapier_2_left);
        this.head.addChild(this.scimitar_1_left);
        this.scimitar_1_left.addChild(this.scimitar_2_left);
        this.scimitar_2_left.addChild(this.scimitar_3_left);
        this.head.addChild(this.spiral_1_right);
        this.spiral_1_right.addChild(this.spiral_2_right);
        this.spiral_2_right.addChild(this.spiral_3_right);
        this.head.addChild(this.rapier_1_right);
        this.rapier_1_right.addChild(this.rapier_2_right);
        this.head.addChild(this.scimitar_1_right);
        this.scimitar_1_right.addChild(this.scimitar_2_right);
        this.scimitar_2_right.addChild(this.scimitar_3_right);
        this.neck.addChild(this.neck_mane);
        this.neck_base.addChild(this.dewlap);
        this.body_chest.addChild(this.arm_left_1);
        this.arm_left_1.addChild(this.arm_left_2);
        this.arm_left_2.addChild(this.arm_left_3);
        this.arm_left_3.addChild(this.hoof_front_left);
        this.body_chest.addChild(this.arm_right_1);
        this.arm_right_1.addChild(this.arm_right_2);
        this.arm_right_2.addChild(this.arm_right_3);
        this.arm_right_3.addChild(this.hoof_front_right);
        this.body_barrel.addChild(this.body_croup);
        this.body_croup.addChild(this.leg_left_1);
        this.leg_left_1.addChild(this.leg_left_2);
        this.leg_left_2.addChild(this.leg_left_3);
        this.leg_left_3.addChild(this.hoof_back_left);
        this.body_croup.addChild(this.leg_right_1);
        this.leg_right_1.addChild(this.leg_right_2);
        this.leg_right_2.addChild(this.leg_right_3);
        this.leg_right_3.addChild(this.hoof_back_right);
        this.body_croup.addChild(this.tail_dock);
        this.tail_dock.addChild(this.tail_tuft);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_barrel);
    }

    /* Built once: Citadel asks for this every frame for every animal, and ImmutableList.of over
     * dozens of parts was a fresh allocation each time. Lazy because the parts only exist once
     * the constructor has built them. */
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
            body_barrel,
            body_chest,
            neck_base,
            neck,
            head,
            muzzle,
            nose_bulb,
            ear_left,
            ear_right,
            eye_left,
            eye_right,
            horn_base_left,
            horn_tip_left,
            horn_prong_left,
            horn_prong_rear_left,
            horn_base_right,
            horn_tip_right,
            horn_prong_right,
            horn_prong_rear_right,
            spiral_1_left,
            spiral_2_left,
            spiral_3_left,
            rapier_1_left,
            rapier_2_left,
            scimitar_1_left,
            scimitar_2_left,
            scimitar_3_left,
            spiral_1_right,
            spiral_2_right,
            spiral_3_right,
            rapier_1_right,
            rapier_2_right,
            scimitar_1_right,
            scimitar_2_right,
            scimitar_3_right,
            neck_mane,
            dewlap,
            arm_left_1,
            arm_left_2,
            arm_left_3,
            hoof_front_left,
            arm_right_1,
            arm_right_2,
            arm_right_3,
            hoof_front_right,
            body_croup,
            leg_left_1,
            leg_left_2,
            leg_left_3,
            hoof_back_left,
            leg_right_1,
            leg_right_2,
            leg_right_3,
            hoof_back_right,
            tail_dock,
            tail_tuft
        );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityAntelope antelope = (EntityAntelope) entityIn;
        animator.update(antelope);

        // Threat display: an antelope does not rear, it drops the neck to present the horns
        // and swings the head laterally while stamping a forefoot -- the "broadside" display.
        animator.setAnimation(EntityAntelope.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, neck_base, 26.09F, 0, -10.43F);
            this.rotate(animator, neck, 7.83F, 0, -7.83F);
            this.rotate(animator, head, -13.04F, 0, 0);
            this.rotate(animator, ear_left, 0, 0, -20.87F);
            this.rotate(animator, ear_right, 0, 0, 20.87F);
            animator.move(arm_right_1, 0, -0.8F, 0);
            this.rotate(animator, arm_right_1, -31.31F, 0, 0);
            this.rotate(animator, arm_right_2, 41.74F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, neck_base, 26.09F, 0, 13.04F);
            this.rotate(animator, neck, 7.83F, 0, 10.43F);
            this.rotate(animator, head, -13.04F, 0, 0);
            this.rotate(animator, ear_left, 0, 0, -20.87F);
            this.rotate(animator, ear_right, 0, 0, 20.87F);
            this.rotate(animator, arm_right_1, 5.21F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // Gore: coil the neck back, then hook the horns forward and UP through the target.
        animator.setAnimation(EntityAntelope.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck_base, -20.87F, 0, 0);
        this.rotate(animator, neck, -13.04F, 0, 0);
        this.rotate(animator, head, 20.87F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck_base, 41.74F, 0, 0);
        this.rotate(animator, neck, 20.87F, 0, 0);
        this.rotate(animator, head, -36.53F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityAntelope antelope, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(antelope);
        float globalSpeed = 1.7f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- per-species parts -------------------------------------------------------
        // Written on EVERY branch every frame: one ModelAntelope instance is shared by every
        // antelope in the world, so a toggle left unset leaks the previous entity's species.
        int variant = antelope.getVariant();
        boolean gazelle  = variant == PRONGHORN || variant == SPRINGBOK || variant == TETRAMERYX;
        boolean frontPr  = variant == PRONGHORN || variant == TETRAMERYX;
        boolean rearPr   = variant == TETRAMERYX;
        boolean spiral   = variant == BLACKBUCK || variant == GIANT_ELAND;
        boolean rapier   = variant == GEMSBOK;
        boolean scimitar = variant == SABLE || variant == BLUEBUCK;
        boolean heavy    = variant == GIANT_ELAND || variant == GEMSBOK
                        || variant == SABLE || variant == BLUEBUCK;

        this.horn_base_left.showModel = gazelle;
        this.horn_base_right.showModel = gazelle;
        this.horn_prong_left.showModel = frontPr;
        this.horn_prong_right.showModel = frontPr;
        this.horn_prong_rear_left.showModel = rearPr;
        this.horn_prong_rear_right.showModel = rearPr;
        this.spiral_1_left.showModel = spiral;
        this.spiral_1_right.showModel = spiral;
        this.rapier_1_left.showModel = rapier;
        this.rapier_1_right.showModel = rapier;
        this.scimitar_1_left.showModel = scimitar;
        this.scimitar_1_right.showModel = scimitar;
        this.dewlap.showModel = variant == GIANT_ELAND;
        this.neck_mane.showModel = variant == GIANT_ELAND || variant == SABLE;

        // SPIRAL is authored at giant-eland proportions and SCIMITAR at sable's, because a
        // uniform scale can change SIZE but never SHAPE -- so the biggest member of each
        // family has to be the one actually drawn.
        float sp = variant == BLACKBUCK ? 0.82F : 1.0F;
        this.spiral_1_left.setShouldScaleChildren(true);
        this.spiral_1_right.setShouldScaleChildren(true);
        this.spiral_1_left.setScale(sp, sp, sp);
        this.spiral_1_right.setScale(sp, sp, sp);
        float sc = variant == BLUEBUCK ? 0.70F : 1.0F;
        this.scimitar_1_left.setShouldScaleChildren(true);
        this.scimitar_1_right.setShouldScaleChildren(true);
        this.scimitar_1_left.setScale(sc, sc, sc);
        this.scimitar_1_right.setScale(sc, sc, sc);

        // A blackbuck's corkscrew is far tighter than a giant eland's open twist, and
        // scale cannot express that -- so the extra turn is added to the AUTHORED yaw,
        // in the same direction (+ on the left, - on the right) so it tightens rather
        // than unwinds. The base literals come straight from the export.
        if (variant == BLACKBUCK) {
            this.spiral_2_left.rotateAngleY = 0.6109F;
            this.spiral_3_left.rotateAngleY = 0.8203F;
            this.spiral_2_right.rotateAngleY = -0.6109F;
            this.spiral_3_right.rotateAngleY = -0.8203F;
        }

        // Heavy build for the hippotragines and tragelaphines. The torso is scaled in X
        // and Y, and the limb ROOTS are pushed outboard by hand to match: AdvancedModelBox
        // does not scale a child's rotation point, so a wider chest alone would swallow the
        // legs (chest 4.15 -> 4.65 against a shoulder whose outer face sits at 5.00). With
        // the roots at 1.16x the shoulder clears the barrel by 0.37 and the thigh by 0.78.
        float hw = heavy ? 1.08F : 1.0F;      // barrel width
        float cw = heavy ? 1.12F : 1.0F;      // chest / croup width
        float hh = heavy ? 1.10F : 1.0F;      // height
        float lx = heavy ? 1.16F : 1.0F;      // limb root lateral offset
        this.arm_left_1.setRotationPoint(3.1F * lx, -0.4F, 0.6F);
        this.arm_right_1.setRotationPoint(-3.1F * lx, -0.4F, 0.6F);
        this.leg_left_1.setRotationPoint(3.2F * lx, -2.6F, 0.5F);
        this.leg_right_1.setRotationPoint(-3.2F * lx, -2.6F, 0.5F);
        this.neck_base.setScale(heavy ? 1.22F : 1.0F, 1.0F, heavy ? 1.12F : 1.0F);
        this.body_croup.setScale(cw, hh, 1.0F);

        // Breathing Animation -- folded into the heavy-build scale rather than issued
        // separately, because setScale OVERWRITES; two calls would drop whichever came first.
        float bx = (float) (1.0F + Math.sin(ageInTicks / 20) * 0.06F);
        float by = (float) (1.0F + Math.sin(ageInTicks / 16) * 0.06F);
        this.body_barrel.setScale(bx * hw, by * hh, 1.0F);
        this.body_chest.setScale((float) (1.0F + Math.sin(ageInTicks / 20) * 0.05F) * cw,
                                 (float) (1.0F + Math.sin(ageInTicks / 16) * 0.05F) * hh, 1.0F);
        float nose = antelope.hasBulbousNose() ? 1F : 0F;
        this.nose_bulb.setScale(nose, nose, nose);

        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        walk(neck_base, 0.4f * globalSpeed, 0.03f, false, 2.8F, 0.05F, ageInTicks / 20, 2);
        walk(ear_left, 0.3f * globalSpeed, 0.09f, false, 1.4F, 0F, ageInTicks / 20, 2);
        walk(ear_right, 0.3f * globalSpeed, 0.09f, true, 1.4F, 0F, ageInTicks / 20, 2);
        walk(tail_dock, 0.3f * globalSpeed, 0.12f, false, 0F, 0F, ageInTicks / 20, 2);

        // Blinking Animation -- pull the eye planes inside the 4.0-wide skull (|x| 2.6 -> 1.0)
        // so they are occluded by the cheek rather than floating off it.
        if (!antelope.shouldRenderEyes()) {
            this.eye_right.setRotationPoint(-1F, -1.0F, -3.2F);
            this.eye_left.setRotationPoint(1F, -1.0F, -3.2F);
        }

        // Head Tracking Animation
        if (!antelope.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck_base);
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 3, head);
        }

        // Pitch/Yaw handler
        if (antelope.isInWater()) {
            this.setRotateAngle(head, -0.1745F, 0.0F, 0.0F);
            if (!antelope.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(antelope.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // Movement Animation -- four-beat walk with each segment phase-lagged behind the one
        // above it and at a lower amplitude, which is what keeps the thin cannons from
        // looking like rigid stilts.
        if (antelope.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.5f * globalDegree, true, f, limbSwingAmount);
            walk(neck_base, 0.8f * globalSpeed, 0.16f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head, 0.8f * globalSpeed, 0.12f * globalDegree, true, 0, 0, f, limbSwingAmount);
            walk(arm_right_1, -0.8f * globalSpeed, 1.2f * globalDegree, true, 0F, 1.2f, f, limbSwingAmount);
            walk(arm_right_2, -0.8f * globalSpeed, 1.0f * globalDegree, false, -1F, 1.2f, f, limbSwingAmount * 1.2f);
            walk(arm_right_3, -0.8f * globalSpeed, 0.6f * globalDegree, false, -2F, 0.6f, f, limbSwingAmount * 1.2f);
            walk(arm_left_1, -0.8f * globalSpeed, 1.2f * globalDegree, true, 2F, 1.2f, f, limbSwingAmount);
            walk(arm_left_2, -0.8f * globalSpeed, 1.0f * globalDegree, false, 1F, 1.2f, f, limbSwingAmount * 1.2f);
            walk(arm_left_3, -0.8f * globalSpeed, 0.6f * globalDegree, false, 0F, 0.6f, f, limbSwingAmount * 1.2f);
            walk(leg_right_1, 0.8f * globalSpeed, 1.2f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(leg_right_2, 0.8f * globalSpeed, 1.0f * globalDegree, true, 1.8F, 0, f, limbSwingAmount);
            walk(leg_right_3, 0.8f * globalSpeed, 0.6f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(leg_left_1, 0.8f * globalSpeed, 1.2f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(leg_left_2, 0.8f * globalSpeed, 1.0f * globalDegree, true, -0.2F, 0, f, limbSwingAmount);
            walk(leg_left_3, 0.8f * globalSpeed, 0.6f * globalDegree, false, -1.2F, 0, f, limbSwingAmount);
            walk(tail_dock, 0.8f * globalSpeed, 0.3f * globalDegree, false, 0F, 0F, f, limbSwingAmount);
        }

        // ---- resting -----------------------------------------------------------------
        // STERNAL RECUMBENCY, solved by search against the real boxes rather than guessed:
        // the body drops 12.82 units, the foreleg Z-folds (humerus back to world -22, radius
        // forward to world +90 putting the CARPUS on the ground, cannon folded back 177 deg
        // to lie along it), and the hind leg Z-folds the other way (femur forward, tibia back
        // to put the HOCK down, cannon lying forward) with a 30 deg outward splay -- folded
        // straight under, the hind cannon drove through the foreleg. Verified: lowest point
        // of the whole model is -0.01, the belly floats 1.78 clear of the folded limbs, and
        // fore and hind cannons clear each other by 5.15 units.
        // Every target below is ABSOLUTE (ticksToSit and the sleep counter both equal the
        // maxProgress of 40), so this pose is immune to changes in the standing defaults.
        if (antelope.sitProgress > 0) {
            applyRestingPose(antelope.sitProgress);
        }
        else if (antelope.sleepProgress > 0) {
            applyRestingPose(antelope.sleepProgress);
            // Asleep the neck relaxes forward and the ears drop; awake-but-sitting it stays alert.
            this.progressRotation(neck_base, antelope.sleepProgress, 0.7854F, 0, 0, 40);
            this.progressRotation(head, antelope.sleepProgress, -0.7854F, 0, 0, 40);
            this.progressRotation(ear_left, antelope.sleepProgress, 0.1745F, 0.4363F, 1.0472F, 40);
            this.progressRotation(ear_right, antelope.sleepProgress, 0.1745F, -0.4363F, -1.0472F, 40);
        }
    }

    private void applyRestingPose(float progress) {
        this.progressPosition(body_barrel, progress, 0.0F, 16.82F, 0.0F, 40);
        this.progressRotation(arm_right_1, progress, 0.4393F, 0, 0.0349F, 40);
        this.progressRotation(arm_right_2, progress, -1.9570F, 0, 0, 40);
        this.progressRotation(arm_right_3, progress, 3.0961F, 0, 0, 40);
        this.progressRotation(hoof_front_right, progress, 0.0449F, 0, 0, 40);
        this.progressRotation(arm_left_1, progress, 0.4393F, 0, -0.0349F, 40);
        this.progressRotation(arm_left_2, progress, -1.9570F, 0, 0, 40);
        this.progressRotation(arm_left_3, progress, 3.0961F, 0, 0, 40);
        this.progressRotation(hoof_front_left, progress, 0.0449F, 0, 0, 40);
        this.progressRotation(leg_right_1, progress, -0.4948F, 0.5236F, 0.0349F, 40);
        this.progressRotation(leg_right_2, progress, 2.2043F, 0, 0, 40);
        this.progressRotation(leg_right_3, progress, -2.8565F, 0, 0, 40);
        this.progressRotation(hoof_back_right, progress, -0.2141F, 0, 0, 40);
        this.progressRotation(leg_left_1, progress, -0.4948F, -0.5236F, -0.0349F, 40);
        this.progressRotation(leg_left_2, progress, 2.2043F, 0, 0, 40);
        this.progressRotation(leg_left_3, progress, -2.8565F, 0, 0, 40);
        this.progressRotation(hoof_back_left, progress, -0.2141F, 0, 0, 40);
    }
}
