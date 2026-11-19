package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityWildebeest;

/**
 * Wildebeest and hartebeest -- blue + black wildebeest, Megalotragus, hartebeest (Phase 4.7, 2026-10-23): 67 boxes on
 * 256x128, authored in scripts/wildebeest_spec.py, ported VERBATIM by scripts/gen_model_wildebeest.py, proved by
 * verify_port.py. High heavy forequarters, a back sloping down to a lower rump, a long boxy face, a tasselled tail.
 *
 * FOUR HORN FAMILIES share the skull, one shown per species (showModel on each chain root hides its whole subtree; the
 * hartebeest's root is the bony PEDICLE its horns grow from). Calves show no horns. Mane / beard / chest fringe / nose
 * tuft are per species. Every pose is SOLVED against the geometry: sit + sleep (sternal recumbency), EAT (the nose to the
 * grass, horns clear) and ATTACK_CLASH (the alcelaphine fight on the carpal joints -- knees down, cannons folded back).
 */
public class ModelWildebeest extends AdvancedEntityModel<EntityWildebeest> {

    private final AdvancedModelBox body_barrel;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox body_withers;
    private final AdvancedModelBox neck_base;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox muzzle;
    private final AdvancedModelBox nose;
    private final AdvancedModelBox nose_tuft;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox gnu_left_1;
    private final AdvancedModelBox gnu_left_2;
    private final AdvancedModelBox gnu_left_3;
    private final AdvancedModelBox gnu_left_4;
    private final AdvancedModelBox gnu_right_1;
    private final AdvancedModelBox gnu_right_2;
    private final AdvancedModelBox gnu_right_3;
    private final AdvancedModelBox gnu_right_4;
    private final AdvancedModelBox gnou_left_1;
    private final AdvancedModelBox gnou_left_2;
    private final AdvancedModelBox gnou_left_3;
    private final AdvancedModelBox gnou_left_4;
    private final AdvancedModelBox gnou_right_1;
    private final AdvancedModelBox gnou_right_2;
    private final AdvancedModelBox gnou_right_3;
    private final AdvancedModelBox gnou_right_4;
    private final AdvancedModelBox megalo_left_1;
    private final AdvancedModelBox megalo_left_2;
    private final AdvancedModelBox megalo_left_3;
    private final AdvancedModelBox megalo_left_4;
    private final AdvancedModelBox megalo_right_1;
    private final AdvancedModelBox megalo_right_2;
    private final AdvancedModelBox megalo_right_3;
    private final AdvancedModelBox megalo_right_4;
    private final AdvancedModelBox hart_pedicle;
    private final AdvancedModelBox hart_left_1;
    private final AdvancedModelBox hart_left_2;
    private final AdvancedModelBox hart_left_3;
    private final AdvancedModelBox hart_right_1;
    private final AdvancedModelBox hart_right_2;
    private final AdvancedModelBox hart_right_3;
    private final AdvancedModelBox mane;
    private final AdvancedModelBox beard;
    private final AdvancedModelBox mane_base;
    private final AdvancedModelBox chest_fringe;
    private final AdvancedModelBox fore_left_upper;
    private final AdvancedModelBox fore_left_forearm;
    private final AdvancedModelBox fore_left_cannon;
    private final AdvancedModelBox fore_left_hoof;
    private final AdvancedModelBox fore_right_upper;
    private final AdvancedModelBox fore_right_forearm;
    private final AdvancedModelBox fore_right_cannon;
    private final AdvancedModelBox fore_right_hoof;
    private final AdvancedModelBox body_rump;
    private final AdvancedModelBox tail;
    private final AdvancedModelBox tail_tassel;
    private final AdvancedModelBox hind_left_thigh;
    private final AdvancedModelBox hind_left_gaskin;
    private final AdvancedModelBox hind_left_cannon;
    private final AdvancedModelBox hind_left_hoof;
    private final AdvancedModelBox hind_right_thigh;
    private final AdvancedModelBox hind_right_gaskin;
    private final AdvancedModelBox hind_right_cannon;
    private final AdvancedModelBox hind_right_hoof;

    private final ModelAnimator animator;

    public ModelWildebeest() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_barrel = new AdvancedModelBox(this, 0, 0);
        this.body_barrel.setRotationPoint(0.0F, 10.8000F, 0.0F);
        this.body_barrel.addBox(-4.3000F, -3.6000F, -6.0F, 8.6000F, 7.3000F, 10.6000F, 0.0F);
        this.setRotateAngle(body_barrel, -0.1222F, 0.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 39, 0);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -5.0F);
        this.body_chest.addBox(-4.0F, -4.7000F, -6.0F, 8.0F, 9.1000F, 8.0F, 0.0F);
        this.setRotateAngle(body_chest, 0.0524F, 0.0F, 0.0F);
        this.body_withers = new AdvancedModelBox(this, 216, 0);
        this.body_withers.setRotationPoint(0.0F, -4.0F, -1.5000F);
        this.body_withers.addBox(-2.1000F, -1.4000F, -3.5000F, 4.2000F, 2.3000F, 6.2000F, 0.0F);
        this.neck_base = new AdvancedModelBox(this, 71, 0);
        this.neck_base.setRotationPoint(0.0F, -2.6000F, -5.0F);
        this.neck_base.addBox(-2.5000F, -2.7000F, -4.4000F, 5.0F, 5.7000F, 6.8000F, 0.0F);
        this.setRotateAngle(neck_base, -0.2793F, 0.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 121, 0);
        this.neck.setRotationPoint(0.0F, 0.0F, -4.4000F);
        this.neck.addBox(-2.3000F, -2.2000F, -4.0F, 4.6000F, 4.7000F, 5.4000F, 0.0F);
        this.setRotateAngle(neck, -0.1396F, 0.0F, 0.0F);
        this.head = new AdvancedModelBox(this, 237, 0);
        this.head.setRotationPoint(0.0F, 0.0F, -4.0F);
        this.head.addBox(-2.0F, -2.0F, -3.6000F, 4.0F, 4.0F, 5.0F, 0.0F);
        this.setRotateAngle(head, 1.1519F, 0.0F, 0.0F);
        this.muzzle = new AdvancedModelBox(this, 141, 0);
        this.muzzle.setRotationPoint(0.0F, 0.0F, -3.6000F);
        this.muzzle.addBox(-1.6000F, -1.6000F, -4.6000F, 3.2000F, 3.3000F, 6.0F, 0.0F);
        this.setRotateAngle(muzzle, 0.3142F, 0.0F, 0.0F);
        this.nose = new AdvancedModelBox(this, 127, 18);
        this.nose.setRotationPoint(0.0F, 0.0F, -4.6000F);
        this.nose.addBox(-1.9000F, -1.0F, -0.6000F, 3.8000F, 2.9000F, 1.2000F, 0.0F);
        this.nose_tuft = new AdvancedModelBox(this, 187, 18);
        this.nose_tuft.setRotationPoint(0.0F, -1.6000F, -3.0F);
        this.nose_tuft.addBox(-0.9000F, -1.4000F, -1.0F, 1.8000F, 1.8000F, 1.8000F, 0.0F);
        this.setRotateAngle(nose_tuft, 0.5236F, 0.0F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 50, 26);
        this.ear_left.setRotationPoint(1.9000F, -1.0F, -1.0F);
        this.ear_left.addBox(0.0F, -0.4000F, -0.7000F, 2.6000F, 0.8000F, 1.4000F, 0.0F);
        this.setRotateAngle(ear_left, 0.0F, -0.2618F, 0.4363F);
        this.eye_left = new AdvancedModelBox(this, 247, 18);
        this.eye_left.setRotationPoint(2.3500F, -0.5000F, -2.2000F);
        this.eye_left.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.ear_right = new AdvancedModelBox(this, 59, 26);
        this.ear_right.setRotationPoint(-1.9000F, -1.0F, -1.0F);
        this.ear_right.addBox(-2.6000F, -0.4000F, -0.7000F, 2.6000F, 0.8000F, 1.4000F, 0.0F);
        this.setRotateAngle(ear_right, 0.0F, 0.2618F, -0.4363F);
        this.eye_right = new AdvancedModelBox(this, 251, 18);
        this.eye_right.setRotationPoint(-2.3500F, -0.5000F, -2.2000F);
        this.eye_right.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.gnu_left_1 = new AdvancedModelBox(this, 137, 18);
        this.gnu_left_1.setRotationPoint(1.2000F, -1.7000F, -1.2000F);
        this.gnu_left_1.addBox(-0.8000F, -2.4000F, -0.8000F, 1.6000F, 2.8000F, 1.6000F, 0.0F);
        this.setRotateAngle(gnu_left_1, -0.3094F, 0.6514F, 1.3507F);
        this.gnu_left_2 = new AdvancedModelBox(this, 227, 18);
        this.gnu_left_2.setRotationPoint(0.0F, -2.0F, 0.0F);
        this.gnu_left_2.addBox(-0.6500F, -2.4000F, -0.5590F, 1.3000F, 2.8000F, 1.1180F, 0.0F);
        this.setRotateAngle(gnu_left_2, -0.1378F, -0.1231F, 0.7777F);
        this.gnu_left_3 = new AdvancedModelBox(this, 0, 26);
        this.gnu_left_3.setRotationPoint(0.0F, -2.0F, 0.0F);
        this.gnu_left_3.addBox(-0.5000F, -2.2000F, -0.5000F, 1.0F, 2.6000F, 1.0F, 0.0F);
        this.setRotateAngle(gnu_left_3, -0.2577F, 0.2457F, -1.2104F);
        this.gnu_left_4 = new AdvancedModelBox(this, 32, 26);
        this.gnu_left_4.setRotationPoint(0.0F, -1.8000F, 0.0F);
        this.gnu_left_4.addBox(-0.3500F, -2.0F, -0.3010F, 0.7000F, 2.4000F, 0.6020F, 0.0F);
        this.setRotateAngle(gnu_left_4, -0.1964F, 0.3152F, -1.1102F);
        this.gnu_right_1 = new AdvancedModelBox(this, 144, 18);
        this.gnu_right_1.setRotationPoint(-1.2000F, -1.7000F, -1.2000F);
        this.gnu_right_1.addBox(-0.8000F, -2.4000F, -0.8000F, 1.6000F, 2.8000F, 1.6000F, 0.0F);
        this.setRotateAngle(gnu_right_1, -0.3094F, -0.6514F, -1.3507F);
        this.gnu_right_2 = new AdvancedModelBox(this, 232, 18);
        this.gnu_right_2.setRotationPoint(0.0F, -2.0F, 0.0F);
        this.gnu_right_2.addBox(-0.6500F, -2.4000F, -0.5590F, 1.3000F, 2.8000F, 1.1180F, 0.0F);
        this.setRotateAngle(gnu_right_2, -0.1378F, 0.1231F, -0.7777F);
        this.gnu_right_3 = new AdvancedModelBox(this, 4, 26);
        this.gnu_right_3.setRotationPoint(0.0F, -2.0F, 0.0F);
        this.gnu_right_3.addBox(-0.5000F, -2.2000F, -0.5000F, 1.0F, 2.6000F, 1.0F, 0.0F);
        this.setRotateAngle(gnu_right_3, -0.2577F, -0.2457F, 1.2104F);
        this.gnu_right_4 = new AdvancedModelBox(this, 35, 26);
        this.gnu_right_4.setRotationPoint(0.0F, -1.8000F, 0.0F);
        this.gnu_right_4.addBox(-0.3500F, -2.0F, -0.3010F, 0.7000F, 2.4000F, 0.6020F, 0.0F);
        this.setRotateAngle(gnu_right_4, -0.1964F, -0.3152F, 1.1102F);
        this.gnou_left_1 = new AdvancedModelBox(this, 151, 18);
        this.gnou_left_1.setRotationPoint(1.6000F, -1.7000F, -1.2000F);
        this.gnou_left_1.addBox(-0.8000F, -2.2000F, -0.8000F, 1.6000F, 2.6000F, 1.6000F, 0.0F);
        this.setRotateAngle(gnou_left_1, 1.7163F, -0.4911F, -0.7540F);
        this.gnou_left_2 = new AdvancedModelBox(this, 237, 18);
        this.gnou_left_2.setRotationPoint(0.0F, -1.8000F, 0.0F);
        this.gnou_left_2.addBox(-0.6500F, -2.0F, -0.5590F, 1.3000F, 2.4000F, 1.1180F, 0.0F);
        this.setRotateAngle(gnou_left_2, -0.5308F, 0.1427F, -0.1008F);
        this.gnou_left_3 = new AdvancedModelBox(this, 8, 26);
        this.gnou_left_3.setRotationPoint(0.0F, -1.6000F, 0.0F);
        this.gnou_left_3.addBox(-0.4250F, -2.0F, -0.5000F, 0.8500F, 2.4000F, 1.0F, 0.0F);
        this.setRotateAngle(gnou_left_3, -1.3394F, 1.3759F, -0.4591F);
        this.gnou_left_4 = new AdvancedModelBox(this, 68, 26);
        this.gnou_left_4.setRotationPoint(0.0F, -1.6000F, 0.0F);
        this.gnou_left_4.addBox(-0.3500F, -1.6000F, -0.3010F, 0.7000F, 2.0F, 0.6020F, 0.0F);
        this.setRotateAngle(gnou_left_4, -0.6677F, -0.2217F, -0.2719F);
        this.gnou_right_1 = new AdvancedModelBox(this, 158, 18);
        this.gnou_right_1.setRotationPoint(-1.6000F, -1.7000F, -1.2000F);
        this.gnou_right_1.addBox(-0.8000F, -2.2000F, -0.8000F, 1.6000F, 2.6000F, 1.6000F, 0.0F);
        this.setRotateAngle(gnou_right_1, 1.7163F, 0.4911F, 0.7540F);
        this.gnou_right_2 = new AdvancedModelBox(this, 242, 18);
        this.gnou_right_2.setRotationPoint(0.0F, -1.8000F, 0.0F);
        this.gnou_right_2.addBox(-0.6500F, -2.0F, -0.5590F, 1.3000F, 2.4000F, 1.1180F, 0.0F);
        this.setRotateAngle(gnou_right_2, -0.5308F, -0.1427F, 0.1008F);
        this.gnou_right_3 = new AdvancedModelBox(this, 12, 26);
        this.gnou_right_3.setRotationPoint(0.0F, -1.6000F, 0.0F);
        this.gnou_right_3.addBox(-0.4250F, -2.0F, -0.5000F, 0.8500F, 2.4000F, 1.0F, 0.0F);
        this.setRotateAngle(gnou_right_3, -1.3394F, -1.3759F, 0.4591F);
        this.gnou_right_4 = new AdvancedModelBox(this, 71, 26);
        this.gnou_right_4.setRotationPoint(0.0F, -1.6000F, 0.0F);
        this.gnou_right_4.addBox(-0.3500F, -1.6000F, -0.3010F, 0.7000F, 2.0F, 0.6020F, 0.0F);
        this.setRotateAngle(gnou_right_4, -0.6677F, 0.2217F, 0.2719F);
        this.megalo_left_1 = new AdvancedModelBox(this, 113, 18);
        this.megalo_left_1.setRotationPoint(1.2000F, -1.7000F, -1.2000F);
        this.megalo_left_1.addBox(-0.8500F, -3.0F, -0.8500F, 1.7000F, 3.4000F, 1.7000F, 0.0F);
        this.setRotateAngle(megalo_left_1, -0.9449F, 0.5286F, 0.8443F);
        this.megalo_left_2 = new AdvancedModelBox(this, 165, 18);
        this.megalo_left_2.setRotationPoint(0.0F, -2.6000F, 0.0F);
        this.megalo_left_2.addBox(-0.7000F, -3.0F, -0.6020F, 1.4000F, 3.4000F, 1.2040F, 0.0F);
        this.setRotateAngle(megalo_left_2, -0.3586F, -0.1041F, 0.1820F);
        this.megalo_left_3 = new AdvancedModelBox(this, 177, 18);
        this.megalo_left_3.setRotationPoint(0.0F, -2.6000F, 0.0F);
        this.megalo_left_3.addBox(-0.5500F, -2.8000F, -0.5500F, 1.1000F, 3.2000F, 1.1000F, 0.0F);
        this.setRotateAngle(megalo_left_3, -0.2727F, 0.1600F, -0.1358F);
        this.megalo_left_4 = new AdvancedModelBox(this, 38, 26);
        this.megalo_left_4.setRotationPoint(0.0F, -2.4000F, 0.0F);
        this.megalo_left_4.addBox(-0.4000F, -2.4000F, -0.3440F, 0.8000F, 2.8000F, 0.6880F, 0.0F);
        this.setRotateAngle(megalo_left_4, -0.1449F, 0.4703F, -0.2393F);
        this.megalo_right_1 = new AdvancedModelBox(this, 120, 18);
        this.megalo_right_1.setRotationPoint(-1.2000F, -1.7000F, -1.2000F);
        this.megalo_right_1.addBox(-0.8500F, -3.0F, -0.8500F, 1.7000F, 3.4000F, 1.7000F, 0.0F);
        this.setRotateAngle(megalo_right_1, -0.9449F, -0.5286F, -0.8443F);
        this.megalo_right_2 = new AdvancedModelBox(this, 171, 18);
        this.megalo_right_2.setRotationPoint(0.0F, -2.6000F, 0.0F);
        this.megalo_right_2.addBox(-0.7000F, -3.0F, -0.6020F, 1.4000F, 3.4000F, 1.2040F, 0.0F);
        this.setRotateAngle(megalo_right_2, -0.3586F, 0.1041F, -0.1820F);
        this.megalo_right_3 = new AdvancedModelBox(this, 182, 18);
        this.megalo_right_3.setRotationPoint(0.0F, -2.6000F, 0.0F);
        this.megalo_right_3.addBox(-0.5500F, -2.8000F, -0.5500F, 1.1000F, 3.2000F, 1.1000F, 0.0F);
        this.setRotateAngle(megalo_right_3, -0.2727F, -0.1600F, 0.1358F);
        this.megalo_right_4 = new AdvancedModelBox(this, 41, 26);
        this.megalo_right_4.setRotationPoint(0.0F, -2.4000F, 0.0F);
        this.megalo_right_4.addBox(-0.4000F, -2.4000F, -0.3440F, 0.8000F, 2.8000F, 0.6880F, 0.0F);
        this.setRotateAngle(megalo_right_4, -0.1449F, -0.4703F, 0.2393F);
        this.hart_pedicle = new AdvancedModelBox(this, 195, 18);
        this.hart_pedicle.setRotationPoint(0.0F, -1.6000F, -1.4000F);
        this.hart_pedicle.addBox(-1.1000F, -2.2000F, -0.7000F, 2.2000F, 2.6000F, 1.4000F, 0.0F);
        this.setRotateAngle(hart_pedicle, -0.6109F, 0.0F, 0.0F);
        this.hart_left_1 = new AdvancedModelBox(this, 16, 26);
        this.hart_left_1.setRotationPoint(0.7000F, -2.1000F, 0.0F);
        this.hart_left_1.addBox(-0.5000F, -2.0F, -0.5000F, 1.0F, 2.4000F, 1.0F, 0.0F);
        this.setRotateAngle(hart_left_1, 0.0502F, 0.0370F, 0.7847F);
        this.hart_left_2 = new AdvancedModelBox(this, 20, 26);
        this.hart_left_2.setRotationPoint(0.0F, -1.6000F, 0.0F);
        this.hart_left_2.addBox(-0.4250F, -1.9000F, -0.3655F, 0.8500F, 2.3000F, 0.7310F, 0.0F);
        this.setRotateAngle(hart_left_2, 0.4458F, -0.0396F, -0.4697F);
        this.hart_left_3 = new AdvancedModelBox(this, 44, 26);
        this.hart_left_3.setRotationPoint(0.0F, -1.5000F, 0.0F);
        this.hart_left_3.addBox(-0.3250F, -2.2000F, -0.3250F, 0.6500F, 2.6000F, 0.6500F, 0.0F);
        this.setRotateAngle(hart_left_3, -2.2647F, -0.0696F, -0.1211F);
        this.hart_right_1 = new AdvancedModelBox(this, 24, 26);
        this.hart_right_1.setRotationPoint(-0.7000F, -2.1000F, 0.0F);
        this.hart_right_1.addBox(-0.5000F, -2.0F, -0.5000F, 1.0F, 2.4000F, 1.0F, 0.0F);
        this.setRotateAngle(hart_right_1, 0.0502F, -0.0370F, -0.7847F);
        this.hart_right_2 = new AdvancedModelBox(this, 28, 26);
        this.hart_right_2.setRotationPoint(0.0F, -1.6000F, 0.0F);
        this.hart_right_2.addBox(-0.4250F, -1.9000F, -0.3655F, 0.8500F, 2.3000F, 0.7310F, 0.0F);
        this.setRotateAngle(hart_right_2, 0.4458F, 0.0396F, 0.4697F);
        this.hart_right_3 = new AdvancedModelBox(this, 47, 26);
        this.hart_right_3.setRotationPoint(0.0F, -1.5000F, 0.0F);
        this.hart_right_3.addBox(-0.3250F, -2.2000F, -0.3250F, 0.6500F, 2.6000F, 0.6500F, 0.0F);
        this.setRotateAngle(hart_right_3, -2.2647F, 0.0696F, 0.1211F);
        this.mane = new AdvancedModelBox(this, 104, 18);
        this.mane.setRotationPoint(0.0F, -2.3000F, -2.0F);
        this.mane.addBox(-0.3000F, -1.4000F, -1.8000F, 0.6000F, 1.8000F, 3.8000F, 0.0F);
        this.beard = new AdvancedModelBox(this, 61, 18);
        this.beard.setRotationPoint(0.0F, 2.7000F, -2.2000F);
        this.beard.addBox(-0.7000F, -0.4000F, -1.4000F, 1.4000F, 3.1000F, 3.2000F, 0.0F);
        this.setRotateAngle(beard, 0.1745F, 0.0F, 0.0F);
        this.mane_base = new AdvancedModelBox(this, 50, 18);
        this.mane_base.setRotationPoint(0.0F, -2.8000F, -2.0F);
        this.mane_base.addBox(-0.4500F, -1.6000F, -2.2000F, 0.9000F, 2.4000F, 4.2000F, 0.0F);
        this.chest_fringe = new AdvancedModelBox(this, 36, 18);
        this.chest_fringe.setRotationPoint(0.0F, 4.4000F, -4.0F);
        this.chest_fringe.addBox(-1.4000F, -0.4000F, -1.6000F, 2.8000F, 2.4000F, 4.2000F, 0.0F);
        this.fore_left_upper = new AdvancedModelBox(this, 160, 0);
        this.fore_left_upper.setRotationPoint(3.1500F, 0.7023F, -2.0F);
        this.fore_left_upper.addBox(-1.3500F, -1.8000F, -2.0F, 2.7000F, 5.8000F, 3.9000F, 0.0F);
        this.setRotateAngle(fore_left_upper, -0.0698F, 0.0F, 0.0F);
        this.fore_left_forearm = new AdvancedModelBox(this, 0, 18);
        this.fore_left_forearm.setRotationPoint(-0.3000F, 4.0F, 0.0F);
        this.fore_left_forearm.addBox(-0.7500F, -0.9000F, -1.3000F, 1.5000F, 5.2000F, 2.5000F, 0.0F);
        this.setRotateAngle(fore_left_forearm, 0.2094F, 0.0F, 0.0F);
        this.fore_left_cannon = new AdvancedModelBox(this, 84, 18);
        this.fore_left_cannon.setRotationPoint(-0.0500F, 4.3000F, 0.0F);
        this.fore_left_cannon.addBox(-0.4000F, -0.8000F, -0.8000F, 0.8000F, 4.6000F, 1.6000F, 0.0F);
        this.setRotateAngle(fore_left_cannon, -0.1047F, 0.0F, 0.0F);
        this.fore_left_hoof = new AdvancedModelBox(this, 203, 18);
        this.fore_left_hoof.setRotationPoint(0.0F, 3.8000F, 0.0F);
        this.fore_left_hoof.addBox(-0.6000F, -0.5000F, -1.2000F, 1.2000F, 1.7000F, 1.7000F, 0.0F);
        this.setRotateAngle(fore_left_hoof, 0.0349F, 0.0F, 0.0F);
        this.fore_right_upper = new AdvancedModelBox(this, 174, 0);
        this.fore_right_upper.setRotationPoint(-3.1500F, 0.7023F, -2.0F);
        this.fore_right_upper.addBox(-1.3500F, -1.8000F, -2.0F, 2.7000F, 5.8000F, 3.9000F, 0.0F);
        this.setRotateAngle(fore_right_upper, -0.0698F, 0.0F, 0.0F);
        this.fore_right_forearm = new AdvancedModelBox(this, 9, 18);
        this.fore_right_forearm.setRotationPoint(0.3000F, 4.0F, 0.0F);
        this.fore_right_forearm.addBox(-0.7500F, -0.9000F, -1.3000F, 1.5000F, 5.2000F, 2.5000F, 0.0F);
        this.setRotateAngle(fore_right_forearm, 0.2094F, 0.0F, 0.0F);
        this.fore_right_cannon = new AdvancedModelBox(this, 89, 18);
        this.fore_right_cannon.setRotationPoint(0.0500F, 4.3000F, 0.0F);
        this.fore_right_cannon.addBox(-0.4000F, -0.8000F, -0.8000F, 0.8000F, 4.6000F, 1.6000F, 0.0F);
        this.setRotateAngle(fore_right_cannon, -0.1047F, 0.0F, 0.0F);
        this.fore_right_hoof = new AdvancedModelBox(this, 209, 18);
        this.fore_right_hoof.setRotationPoint(0.0F, 3.8000F, 0.0F);
        this.fore_right_hoof.addBox(-0.6000F, -0.5000F, -1.2000F, 1.2000F, 1.7000F, 1.7000F, 0.0F);
        this.setRotateAngle(fore_right_hoof, 0.0349F, 0.0F, 0.0F);
        this.body_rump = new AdvancedModelBox(this, 95, 0);
        this.body_rump.setRotationPoint(0.0F, 0.0F, 4.0F);
        this.body_rump.addBox(-3.6000F, -2.7000F, -0.6000F, 7.2000F, 5.5000F, 5.4000F, 0.0F);
        this.setRotateAngle(body_rump, -0.0698F, 0.0F, 0.0F);
        this.tail = new AdvancedModelBox(this, 79, 18);
        this.tail.setRotationPoint(0.0F, -2.0F, 5.4000F);
        this.tail.addBox(-0.6000F, -0.4000F, -0.5000F, 1.2000F, 5.6000F, 1.0F, 0.0F);
        this.setRotateAngle(tail, 0.2443F, 0.0F, 0.0F);
        this.tail_tassel = new AdvancedModelBox(this, 71, 18);
        this.tail_tassel.setRotationPoint(0.0F, 5.0F, 0.0F);
        this.tail_tassel.addBox(-1.0F, -0.4000F, -0.8000F, 2.0F, 5.2000F, 1.6000F, 0.0F);
        this.setRotateAngle(tail_tassel, 0.0698F, 0.0F, 0.0F);
        this.hind_left_thigh = new AdvancedModelBox(this, 188, 0);
        this.hind_left_thigh.setRotationPoint(3.1500F, -0.7274F, 2.6000F);
        this.hind_left_thigh.addBox(-1.5500F, -1.4000F, -2.2000F, 3.1000F, 5.6000F, 3.9000F, 0.0F);
        this.setRotateAngle(hind_left_thigh, -0.1222F, 0.0F, 0.0F);
        this.hind_left_gaskin = new AdvancedModelBox(this, 18, 18);
        this.hind_left_gaskin.setRotationPoint(-0.5000F, 4.2000F, 0.0F);
        this.hind_left_gaskin.addBox(-0.7500F, -1.0F, -1.3000F, 1.5000F, 5.2000F, 2.6000F, 0.0F);
        this.setRotateAngle(hind_left_gaskin, 0.7679F, 0.0F, 0.0F);
        this.hind_left_cannon = new AdvancedModelBox(this, 94, 18);
        this.hind_left_cannon.setRotationPoint(0.0500F, 4.2000F, 0.0F);
        this.hind_left_cannon.addBox(-0.4000F, -0.8000F, -0.8000F, 0.8000F, 4.8000F, 1.6000F, 0.0F);
        this.setRotateAngle(hind_left_cannon, -0.5934F, 0.0F, 0.0F);
        this.hind_left_hoof = new AdvancedModelBox(this, 215, 18);
        this.hind_left_hoof.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.hind_left_hoof.addBox(-0.6000F, -0.5000F, -1.2000F, 1.2000F, 1.7000F, 1.7000F, 0.0F);
        this.setRotateAngle(hind_left_hoof, 0.1396F, 0.0F, 0.0F);
        this.hind_right_thigh = new AdvancedModelBox(this, 202, 0);
        this.hind_right_thigh.setRotationPoint(-3.1500F, -0.7274F, 2.6000F);
        this.hind_right_thigh.addBox(-1.5500F, -1.4000F, -2.2000F, 3.1000F, 5.6000F, 3.9000F, 0.0F);
        this.setRotateAngle(hind_right_thigh, -0.1222F, 0.0F, 0.0F);
        this.hind_right_gaskin = new AdvancedModelBox(this, 27, 18);
        this.hind_right_gaskin.setRotationPoint(0.5000F, 4.2000F, 0.0F);
        this.hind_right_gaskin.addBox(-0.7500F, -1.0F, -1.3000F, 1.5000F, 5.2000F, 2.6000F, 0.0F);
        this.setRotateAngle(hind_right_gaskin, 0.7679F, 0.0F, 0.0F);
        this.hind_right_cannon = new AdvancedModelBox(this, 99, 18);
        this.hind_right_cannon.setRotationPoint(-0.0500F, 4.2000F, 0.0F);
        this.hind_right_cannon.addBox(-0.4000F, -0.8000F, -0.8000F, 0.8000F, 4.8000F, 1.6000F, 0.0F);
        this.setRotateAngle(hind_right_cannon, -0.5934F, 0.0F, 0.0F);
        this.hind_right_hoof = new AdvancedModelBox(this, 221, 18);
        this.hind_right_hoof.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.hind_right_hoof.addBox(-0.6000F, -0.5000F, -1.2000F, 1.2000F, 1.7000F, 1.7000F, 0.0F);
        this.setRotateAngle(hind_right_hoof, 0.1396F, 0.0F, 0.0F);

        this.body_barrel.addChild(this.body_chest);
        this.body_chest.addChild(this.body_withers);
        this.body_chest.addChild(this.neck_base);
        this.neck_base.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.muzzle);
        this.muzzle.addChild(this.nose);
        this.muzzle.addChild(this.nose_tuft);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.ear_right);
        this.head.addChild(this.eye_right);
        this.head.addChild(this.gnu_left_1);
        this.gnu_left_1.addChild(this.gnu_left_2);
        this.gnu_left_2.addChild(this.gnu_left_3);
        this.gnu_left_3.addChild(this.gnu_left_4);
        this.head.addChild(this.gnu_right_1);
        this.gnu_right_1.addChild(this.gnu_right_2);
        this.gnu_right_2.addChild(this.gnu_right_3);
        this.gnu_right_3.addChild(this.gnu_right_4);
        this.head.addChild(this.gnou_left_1);
        this.gnou_left_1.addChild(this.gnou_left_2);
        this.gnou_left_2.addChild(this.gnou_left_3);
        this.gnou_left_3.addChild(this.gnou_left_4);
        this.head.addChild(this.gnou_right_1);
        this.gnou_right_1.addChild(this.gnou_right_2);
        this.gnou_right_2.addChild(this.gnou_right_3);
        this.gnou_right_3.addChild(this.gnou_right_4);
        this.head.addChild(this.megalo_left_1);
        this.megalo_left_1.addChild(this.megalo_left_2);
        this.megalo_left_2.addChild(this.megalo_left_3);
        this.megalo_left_3.addChild(this.megalo_left_4);
        this.head.addChild(this.megalo_right_1);
        this.megalo_right_1.addChild(this.megalo_right_2);
        this.megalo_right_2.addChild(this.megalo_right_3);
        this.megalo_right_3.addChild(this.megalo_right_4);
        this.head.addChild(this.hart_pedicle);
        this.hart_pedicle.addChild(this.hart_left_1);
        this.hart_left_1.addChild(this.hart_left_2);
        this.hart_left_2.addChild(this.hart_left_3);
        this.hart_pedicle.addChild(this.hart_right_1);
        this.hart_right_1.addChild(this.hart_right_2);
        this.hart_right_2.addChild(this.hart_right_3);
        this.neck.addChild(this.mane);
        this.neck.addChild(this.beard);
        this.neck_base.addChild(this.mane_base);
        this.body_chest.addChild(this.chest_fringe);
        this.body_chest.addChild(this.fore_left_upper);
        this.fore_left_upper.addChild(this.fore_left_forearm);
        this.fore_left_forearm.addChild(this.fore_left_cannon);
        this.fore_left_cannon.addChild(this.fore_left_hoof);
        this.body_chest.addChild(this.fore_right_upper);
        this.fore_right_upper.addChild(this.fore_right_forearm);
        this.fore_right_forearm.addChild(this.fore_right_cannon);
        this.fore_right_cannon.addChild(this.fore_right_hoof);
        this.body_barrel.addChild(this.body_rump);
        this.body_rump.addChild(this.tail);
        this.tail.addChild(this.tail_tassel);
        this.body_rump.addChild(this.hind_left_thigh);
        this.hind_left_thigh.addChild(this.hind_left_gaskin);
        this.hind_left_gaskin.addChild(this.hind_left_cannon);
        this.hind_left_cannon.addChild(this.hind_left_hoof);
        this.body_rump.addChild(this.hind_right_thigh);
        this.hind_right_thigh.addChild(this.hind_right_gaskin);
        this.hind_right_gaskin.addChild(this.hind_right_cannon);
        this.hind_right_cannon.addChild(this.hind_right_hoof);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_barrel);
    }

    // Built once: getAllParts() is called every frame, and a fresh ImmutableList per animal per frame
    // was pure garbage (2.8.0, same as ModelBison).
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
                body_barrel,
                body_chest,
                body_withers,
                neck_base,
                neck,
                head,
                muzzle,
                nose,
                nose_tuft,
                ear_left,
                eye_left,
                ear_right,
                eye_right,
                gnu_left_1,
                gnu_left_2,
                gnu_left_3,
                gnu_left_4,
                gnu_right_1,
                gnu_right_2,
                gnu_right_3,
                gnu_right_4,
                gnou_left_1,
                gnou_left_2,
                gnou_left_3,
                gnou_left_4,
                gnou_right_1,
                gnou_right_2,
                gnou_right_3,
                gnou_right_4,
                megalo_left_1,
                megalo_left_2,
                megalo_left_3,
                megalo_left_4,
                megalo_right_1,
                megalo_right_2,
                megalo_right_3,
                megalo_right_4,
                hart_pedicle,
                hart_left_1,
                hart_left_2,
                hart_left_3,
                hart_right_1,
                hart_right_2,
                hart_right_3,
                mane,
                beard,
                mane_base,
                chest_fringe,
                fore_left_upper,
                fore_left_forearm,
                fore_left_cannon,
                fore_left_hoof,
                fore_right_upper,
                fore_right_forearm,
                fore_right_cannon,
                fore_right_hoof,
                body_rump,
                tail,
                tail_tassel,
                hind_left_thigh,
                hind_left_gaskin,
                hind_left_cannon,
                hind_left_hoof,
                hind_right_thigh,
                hind_right_gaskin,
                hind_right_cannon,
                hind_right_hoof
        );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityWildebeest wb = (EntityWildebeest) entityIn;
        animator.update(wb);
        // <gen:idle-poses> 2.8.0: SOLVED by scripts/pose_reach.py against this model's own boxes (muzzle to the grass / the
        // water line beyond the bank, every head part and horn family clear of the ground, nothing through the torso).
        // Regenerate, never hand-edit: python scripts/gen_keyframes.py wildebeest
        // DRINK (100t, DrinkGoal): muzzle to the water past the bank edge, two swallows lifting the head
        animator.setAnimation(EntityWildebeest.DRINK);
        animator.startKeyframe(15);
        this.rotate(animator, neck_base, 34.8F, 0.0F, 0.0F);
        this.rotate(animator, neck, 12.9F, 0.0F, 0.0F);
        this.rotate(animator, head, -0.1F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, -1.2F, 0.0F, 0.0F);
        this.rotate(animator, nose, -1.2F, 0.0F, 0.0F);
        animator.move(neck_base, 0.0F, 5.9F, 0.0F);
        animator.endKeyframe();
        animator.setStaticKeyframe(20);
        animator.startKeyframe(5);
        this.rotate(animator, neck_base, 34.8F, 0.0F, 0.0F);
        this.rotate(animator, neck, 12.9F, 0.0F, 0.0F);
        this.rotate(animator, head, -5.1F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, -1.2F, 0.0F, 0.0F);
        this.rotate(animator, nose, -1.2F, 0.0F, 0.0F);
        animator.move(neck_base, 0.0F, 5.9F, 0.0F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck_base, 34.8F, 0.0F, 0.0F);
        this.rotate(animator, neck, 12.9F, 0.0F, 0.0F);
        this.rotate(animator, head, -0.1F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, -1.2F, 0.0F, 0.0F);
        this.rotate(animator, nose, -1.2F, 0.0F, 0.0F);
        animator.move(neck_base, 0.0F, 5.9F, 0.0F);
        animator.endKeyframe();
        animator.setStaticKeyframe(20);
        animator.startKeyframe(5);
        this.rotate(animator, neck_base, 34.8F, 0.0F, 0.0F);
        this.rotate(animator, neck, 12.9F, 0.0F, 0.0F);
        this.rotate(animator, head, -5.1F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, -1.2F, 0.0F, 0.0F);
        this.rotate(animator, nose, -1.2F, 0.0F, 0.0F);
        animator.move(neck_base, 0.0F, 5.9F, 0.0F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck_base, 34.8F, 0.0F, 0.0F);
        this.rotate(animator, neck, 12.9F, 0.0F, 0.0F);
        this.rotate(animator, head, -0.1F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, -1.2F, 0.0F, 0.0F);
        this.rotate(animator, nose, -1.2F, 0.0F, 0.0F);
        animator.move(neck_base, 0.0F, 5.9F, 0.0F);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.resetKeyframe(15);
        // ALERT (40t, AlertGoal): head raised, ears up, staring at the threat; repeats while the alert lasts
        animator.setAnimation(EntityWildebeest.ALERT);
        animator.startKeyframe(8);
        this.rotate(animator, neck_base, -35.0F, 0.0F, 0.0F);
        this.rotate(animator, neck, -13.1F, 0.0F, 0.0F);
        this.rotate(animator, head, 4.2F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 12.0F, 0.0F, 0.0F);
        this.rotate(animator, nose, 12.0F, 0.0F, 0.0F);
        animator.endKeyframe();
        animator.setStaticKeyframe(24);
        animator.resetKeyframe(8);
        // </gen:idle-poses>

        // ATTACK_GORE (14t): the head drops, then hooks up through the target
        animator.setAnimation(EntityWildebeest.ATTACK_GORE);
        animator.startKeyframe(5);
        this.rotate(animator, neck_base, 22.0F, 0, 0);
        this.rotate(animator, head, 16.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        animator.move(body_barrel, 0, 0, -1.5F);
        this.rotate(animator, neck_base, -12.0F, 0, 0);
        this.rotate(animator, head, -22.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(5);

        // ATTACK_THREATEN (50t): head down and horns presented, a foreleg pawing the ground, then a toss of the head
        animator.setAnimation(EntityWildebeest.ATTACK_THREATEN);
        animator.startKeyframe(10);
        this.rotate(animator, neck_base, 26.0F, 0, 0);
        this.rotate(animator, head, 10.0F, 0, 0);
        this.rotate(animator, fore_left_upper, -28.0F, 0, 0);
        this.rotate(animator, fore_left_forearm, 42.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(8);
        this.rotate(animator, neck_base, 26.0F, 0, 0);
        this.rotate(animator, head, 10.0F, 0, 0);
        this.rotate(animator, fore_left_upper, 14.0F, 0, 0);
        this.rotate(animator, fore_left_forearm, 8.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(8);
        this.rotate(animator, neck_base, 14.0F, 0, 0);
        this.rotate(animator, head, -22.0F, 0, 0);
        this.rotate(animator, fore_left_upper, -28.0F, 0, 0);
        this.rotate(animator, fore_left_forearm, 42.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(8);
        animator.resetKeyframe(16);

        // EAT (40t, GrazeGoal's timer): SOLVED graze reach -- neck_base -82, neck -6, head +30 (BB deg), the nose just above the grass and every horn family clear of it; two cropping nods
        animator.setAnimation(EntityWildebeest.EAT);
        animator.startKeyframe(10);
        this.rotate(animator, neck_base, 82.0F, 0, 0);
        this.rotate(animator, neck, 6.0F, 0, 0);
        this.rotate(animator, head, -30.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck_base, 82.0F, 0, 0);
        this.rotate(animator, neck, 6.0F, 0, 0);
        this.rotate(animator, head, -36.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck_base, 82.0F, 0, 0);
        this.rotate(animator, neck, 6.0F, 0, 0);
        this.rotate(animator, head, -30.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.resetKeyframe(10);

        // ATTACK_CLASH (40t): SOLVED kneel -- barrel pitched 12 deg nose-down and dropped 3.5u onto the knees, cannons folded back flat, hind legs standing; two shoves forward
        animator.setAnimation(EntityWildebeest.ATTACK_CLASH);
        animator.startKeyframe(10);
        this.rotate(animator, body_barrel, 12.0F, 0, 0);
        animator.move(body_barrel, 0, 3.5000F, -0.0F);
        this.rotate(animator, neck_base, 10.0F, 0, 0);
        this.rotate(animator, head, 8.0F, 0, 0);
        this.rotate(animator, fore_left_upper, -34.4F, 0, 0);
        this.rotate(animator, fore_left_forearm, 37.7F, 0, 0);
        this.rotate(animator, fore_left_cannon, -103.4F, 0, 0);
        this.rotate(animator, fore_left_hoof, 2.1F, 0, 0);
        this.rotate(animator, fore_right_upper, -34.4F, 0, 0);
        this.rotate(animator, fore_right_forearm, 37.7F, 0, 0);
        this.rotate(animator, fore_right_cannon, -103.4F, 0, 0);
        this.rotate(animator, fore_right_hoof, 2.1F, 0, 0);
        this.rotate(animator, hind_left_thigh, -22.8F, 0, 0);
        this.rotate(animator, hind_left_gaskin, 45.4F, 0, 0);
        this.rotate(animator, hind_left_cannon, -40.9F, 0, 0);
        this.rotate(animator, hind_left_hoof, 5.9F, 0, 0);
        this.rotate(animator, hind_right_thigh, -22.8F, 0, 0);
        this.rotate(animator, hind_right_gaskin, 45.4F, 0, 0);
        this.rotate(animator, hind_right_cannon, -40.9F, 0, 0);
        this.rotate(animator, hind_right_hoof, 5.9F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(6);
        animator.startKeyframe(4);
        this.rotate(animator, body_barrel, 12.0F, 0, 0);
        animator.move(body_barrel, 0, 3.5000F, -1.2000F);
        this.rotate(animator, neck_base, 10.0F, 0, 0);
        this.rotate(animator, head, 12.8F, 0, 0);
        this.rotate(animator, fore_left_upper, -34.4F, 0, 0);
        this.rotate(animator, fore_left_forearm, 37.7F, 0, 0);
        this.rotate(animator, fore_left_cannon, -103.4F, 0, 0);
        this.rotate(animator, fore_left_hoof, 2.1F, 0, 0);
        this.rotate(animator, fore_right_upper, -34.4F, 0, 0);
        this.rotate(animator, fore_right_forearm, 37.7F, 0, 0);
        this.rotate(animator, fore_right_cannon, -103.4F, 0, 0);
        this.rotate(animator, fore_right_hoof, 2.1F, 0, 0);
        this.rotate(animator, hind_left_thigh, -22.8F, 0, 0);
        this.rotate(animator, hind_left_gaskin, 45.4F, 0, 0);
        this.rotate(animator, hind_left_cannon, -40.9F, 0, 0);
        this.rotate(animator, hind_left_hoof, 5.9F, 0, 0);
        this.rotate(animator, hind_right_thigh, -22.8F, 0, 0);
        this.rotate(animator, hind_right_gaskin, 45.4F, 0, 0);
        this.rotate(animator, hind_right_cannon, -40.9F, 0, 0);
        this.rotate(animator, hind_right_hoof, 5.9F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, body_barrel, 12.0F, 0, 0);
        animator.move(body_barrel, 0, 3.5000F, -0.0F);
        this.rotate(animator, neck_base, 10.0F, 0, 0);
        this.rotate(animator, head, 8.0F, 0, 0);
        this.rotate(animator, fore_left_upper, -34.4F, 0, 0);
        this.rotate(animator, fore_left_forearm, 37.7F, 0, 0);
        this.rotate(animator, fore_left_cannon, -103.4F, 0, 0);
        this.rotate(animator, fore_left_hoof, 2.1F, 0, 0);
        this.rotate(animator, fore_right_upper, -34.4F, 0, 0);
        this.rotate(animator, fore_right_forearm, 37.7F, 0, 0);
        this.rotate(animator, fore_right_cannon, -103.4F, 0, 0);
        this.rotate(animator, fore_right_hoof, 2.1F, 0, 0);
        this.rotate(animator, hind_left_thigh, -22.8F, 0, 0);
        this.rotate(animator, hind_left_gaskin, 45.4F, 0, 0);
        this.rotate(animator, hind_left_cannon, -40.9F, 0, 0);
        this.rotate(animator, hind_left_hoof, 5.9F, 0, 0);
        this.rotate(animator, hind_right_thigh, -22.8F, 0, 0);
        this.rotate(animator, hind_right_gaskin, 45.4F, 0, 0);
        this.rotate(animator, hind_right_cannon, -40.9F, 0, 0);
        this.rotate(animator, hind_right_hoof, 5.9F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, body_barrel, 12.0F, 0, 0);
        animator.move(body_barrel, 0, 3.5000F, -1.2000F);
        this.rotate(animator, neck_base, 10.0F, 0, 0);
        this.rotate(animator, head, 12.8F, 0, 0);
        this.rotate(animator, fore_left_upper, -34.4F, 0, 0);
        this.rotate(animator, fore_left_forearm, 37.7F, 0, 0);
        this.rotate(animator, fore_left_cannon, -103.4F, 0, 0);
        this.rotate(animator, fore_left_hoof, 2.1F, 0, 0);
        this.rotate(animator, fore_right_upper, -34.4F, 0, 0);
        this.rotate(animator, fore_right_forearm, 37.7F, 0, 0);
        this.rotate(animator, fore_right_cannon, -103.4F, 0, 0);
        this.rotate(animator, fore_right_hoof, 2.1F, 0, 0);
        this.rotate(animator, hind_left_thigh, -22.8F, 0, 0);
        this.rotate(animator, hind_left_gaskin, 45.4F, 0, 0);
        this.rotate(animator, hind_left_cannon, -40.9F, 0, 0);
        this.rotate(animator, hind_left_hoof, 5.9F, 0, 0);
        this.rotate(animator, hind_right_thigh, -22.8F, 0, 0);
        this.rotate(animator, hind_right_gaskin, 45.4F, 0, 0);
        this.rotate(animator, hind_right_cannon, -40.9F, 0, 0);
        this.rotate(animator, hind_right_hoof, 5.9F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(4);
        animator.resetKeyframe(8);
    }

    public void setupAnim(EntityWildebeest wb, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(wb);
        limbSwingAmount = Math.min(0.6F, limbSwingAmount);
        float gait = limbSwing * 0.55F / Math.max(0.3F, wb.getMobSize() * wb.getScale());

        // ---- per-species parts (written EVERY frame: one model instance is shared by every wildebeest) ----
        int variant = wb.getVariant();
        boolean adult = !wb.isBaby();
        boolean gnu = adult && (variant == 0);
        this.gnu_left_1.showModel = gnu;
        this.gnu_right_1.showModel = gnu;
        boolean gnou = adult && (variant == 1);
        this.gnou_left_1.showModel = gnou;
        this.gnou_right_1.showModel = gnou;
        boolean megalo = adult && (variant == 2);
        this.megalo_left_1.showModel = megalo;
        this.megalo_right_1.showModel = megalo;
        boolean hart = adult && (variant == 3);
        this.hart_pedicle.showModel = hart;
        this.mane.showModel = variant == 0 || variant == 1 || variant == 2;
        this.mane_base.showModel = variant == 0 || variant == 1 || variant == 2;
        this.beard.showModel = adult && (variant == 0 || variant == 1);
        this.chest_fringe.showModel = adult && (variant == 1);
        this.nose_tuft.showModel = variant == 1;

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.03F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(ear_left, 0.2F, 0.18F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.2F, 0.18F, false, 1F, 0F, ageInTicks, 1);
        this.flap(tail, 0.15F, 0.25F, false, 0F, 0F, ageInTicks, 1);
        this.flap(tail_tassel, 0.15F, 0.2F, false, -0.8F, 0F, ageInTicks, 1);
        this.walk(beard, 0.12F, 0.06F, false, 0F, 0F, ageInTicks, 1);

        if (!wb.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(1.2925F, -0.5000F, -2.2000F);
            this.eye_right.setRotationPoint(-1.2925F, -0.5000F, -2.2000F);
        }

        if (!wb.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        if (wb.isInWater() && !wb.isOnGround()) {
            gait = ageInTicks / 5;
            limbSwingAmount = 0.4F;
            this.setRotateAngle(neck_base, -0.45F, 0, 0);
        }

        if (wb.canMove()) {
            // 0.13 blocks/tick: wandering and grazing walk; fleeing, migrating at speed and charging gallop
            boolean running = wb.getCurrentSpeed() > 0.13F;
            if (!running) {
                // walk: a lateral-sequence four-beat walk, the head nodding with each forefoot
                float spd = 1.0F;
                bob(body_barrel, 2.0F * spd, 0.35F, true, gait, limbSwingAmount);
                walk(neck_base, 2.0F * spd, 0.05F, false, 0.5F, 0F, gait, limbSwingAmount);
                walk(fore_left_upper, spd, 0.55F, false, 0F, 0F, gait, limbSwingAmount);
                walk(fore_left_forearm, spd, 0.40F, false, -1.3F, 0.12F, gait, limbSwingAmount);
                walk(fore_left_cannon, spd, 0.30F, false, -1.3F, 0F, gait, limbSwingAmount);
                walk(fore_right_upper, spd, 0.55F, false, Mth.PI, 0F, gait, limbSwingAmount);
                walk(fore_right_forearm, spd, 0.40F, false, Mth.PI - 1.3F, 0.12F, gait, limbSwingAmount);
                walk(fore_right_cannon, spd, 0.30F, false, Mth.PI - 1.3F, 0F, gait, limbSwingAmount);
                walk(hind_left_thigh, spd, 0.50F, false, Mth.PI * 1.5F, 0F, gait, limbSwingAmount);
                walk(hind_left_gaskin, spd, 0.40F, true, Mth.PI * 1.5F - 1.2F, 0F, gait, limbSwingAmount);
                walk(hind_left_cannon, spd, 0.30F, false, Mth.PI * 1.5F - 1.2F, 0F, gait, limbSwingAmount);
                walk(hind_right_thigh, spd, 0.50F, false, Mth.PI * 0.5F, 0F, gait, limbSwingAmount);
                walk(hind_right_gaskin, spd, 0.40F, true, Mth.PI * 0.5F - 1.2F, 0F, gait, limbSwingAmount);
                walk(hind_right_cannon, spd, 0.30F, false, Mth.PI * 0.5F - 1.2F, 0F, gait, limbSwingAmount);
            } else {
                // gallop: a rotary gallop -- the hind pair lands, then the fore pair, the barrel rocking about its middle
                float spd = 0.7F;
                walk(body_barrel, spd, 0.08F, false, 0.6F, 0F, gait, limbSwingAmount);
                bob(body_barrel, spd, 1.1F, false, gait, limbSwingAmount);
                walk(neck_base, spd, 0.10F, true, 0.6F, 0.1F, gait, limbSwingAmount);
                walk(tail, spd, 0.25F, false, 1.0F, -0.35F, gait, limbSwingAmount);
                walk(fore_left_upper, spd, 0.85F, false, 0F, 0F, gait, limbSwingAmount);
                walk(fore_left_forearm, spd, 0.55F, false, -1.4F, 0.2F, gait, limbSwingAmount);
                walk(fore_left_cannon, spd, 0.45F, false, -1.4F, 0F, gait, limbSwingAmount);
                walk(fore_right_upper, spd, 0.85F, false, 0.5F, 0F, gait, limbSwingAmount);
                walk(fore_right_forearm, spd, 0.55F, false, 0.5F - 1.4F, 0.2F, gait, limbSwingAmount);
                walk(fore_right_cannon, spd, 0.45F, false, 0.5F - 1.4F, 0F, gait, limbSwingAmount);
                walk(hind_left_thigh, spd, 0.80F, false, Mth.PI + 0.3F, 0F, gait, limbSwingAmount);
                walk(hind_left_gaskin, spd, 0.55F, true, Mth.PI + 0.3F - 1.2F, 0F, gait, limbSwingAmount);
                walk(hind_left_cannon, spd, 0.45F, false, Mth.PI + 0.3F - 1.2F, 0F, gait, limbSwingAmount);
                walk(hind_right_thigh, spd, 0.80F, false, Mth.PI + 0.8F, 0F, gait, limbSwingAmount);
                walk(hind_right_gaskin, spd, 0.55F, true, Mth.PI + 0.8F - 1.2F, 0F, gait, limbSwingAmount);
                walk(hind_right_cannon, spd, 0.45F, false, Mth.PI + 0.8F - 1.2F, 0F, gait, limbSwingAmount);
            }
        }

        if (wb.sitProgress > 0) {
            // SOLVED (wildebeest_spec.py --pose): sternal recumbency, every leg box above the floor
            this.progressPosition(body_barrel, wb.sitProgress, 0.0F, 18.3670F, 0.0F, 40);
            this.progressRotation(fore_left_upper, wb.sitProgress, -1.2912F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, wb.sitProgress, 1.7410F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, wb.sitProgress, -1.9421F, 0, 0, 40);
            this.progressRotation(fore_left_hoof, wb.sitProgress, 0.0823F, 0, 0, 40);
            this.progressRotation(fore_right_upper, wb.sitProgress, -1.2912F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, wb.sitProgress, 1.7410F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, wb.sitProgress, -1.9421F, 0, 0, 40);
            this.progressRotation(fore_right_hoof, wb.sitProgress, 0.0823F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, wb.sitProgress, -1.2029F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, wb.sitProgress, 1.8552F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, wb.sitProgress, -2.0189F, 0, 0, 40);
            this.progressRotation(hind_left_hoof, wb.sitProgress, 0.0671F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, wb.sitProgress, -1.2029F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, wb.sitProgress, 1.8552F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, wb.sitProgress, -2.0189F, 0, 0, 40);
            this.progressRotation(hind_right_hoof, wb.sitProgress, 0.0671F, 0, 0, 40);
            this.progressRotation(neck_base, wb.sitProgress, -0.3840F, 0, 0, 40);
            // the tail DRAPES to one side, its tassel lying on the ground (solved)
            this.progressRotation(tail, wb.sitProgress, 0.2443F, 0, 1.0472F, 40);
        }
        else if (wb.sleepProgress > 0) {
            // SOLVED (wildebeest_spec.py --pose): sternal recumbency, every leg box above the floor
            this.progressPosition(body_barrel, wb.sleepProgress, 0.0F, 19.1670F, 0.0F, 40);
            this.progressRotation(fore_left_upper, wb.sleepProgress, -1.4113F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, wb.sleepProgress, 2.0161F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, wb.sleepProgress, -2.1024F, 0, 0, 40);
            this.progressRotation(fore_left_hoof, wb.sleepProgress, 0.0797F, 0, 0, 40);
            this.progressRotation(fore_right_upper, wb.sleepProgress, -1.4113F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, wb.sleepProgress, 2.0161F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, wb.sleepProgress, -2.1024F, 0, 0, 40);
            this.progressRotation(fore_right_hoof, wb.sleepProgress, 0.0797F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, wb.sleepProgress, -1.2978F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, wb.sleepProgress, 2.1220F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, wb.sleepProgress, -2.2000F, 0, 0, 40);
            this.progressRotation(hind_left_hoof, wb.sleepProgress, 0.0899F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, wb.sleepProgress, -1.2978F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, wb.sleepProgress, 2.1220F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, wb.sleepProgress, -2.2000F, 0, 0, 40);
            this.progressRotation(hind_right_hoof, wb.sleepProgress, 0.0899F, 0, 0, 40);
            this.progressRotation(neck_base, wb.sleepProgress, -0.1047F, 0, 0, 40);
            // the tail DRAPES to one side, its tassel lying on the ground (solved)
            this.progressRotation(tail, wb.sleepProgress, 0.2443F, 0, 1.2217F, 40);
        }
    }
}
