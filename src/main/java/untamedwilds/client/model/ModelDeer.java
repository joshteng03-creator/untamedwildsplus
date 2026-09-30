package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.mammal.EntityDeer;

/**
 * Full Blockbench rebuild -- 59 boxes on 256x128. The old model was a BISON FORK (28 boxes
 * on 128x64) that still carried head_horn_left/right, head_beard and body_hair with the
 * horns scaled to zero at runtime. None of that geometry survives.
 *
 * Every per-species difference is driven off deer.json FLAGS -- the ModelBison pattern
 * (palmate / throatBell / antlerScale), not off getVariant() indices. The index form was
 * replaced when the second wave of species landed: caribou and fallow are palmate at
 * variants 9 and 11, so a `variant <= 2` test silently gave them beam antlers.
 *     palmate 1 -> megaloceros, stag_moose, moose, caribou, fallow
 *     palmate 0 -> wapiti, red_deer and the South American guild
 * Both racks hang off a shared pedicle, so ONE setScale + setShouldScaleChildren(true) on
 * pedicle_left/right sizes each family. A uniform scale changes size but never shape, so
 * PALMATE is authored at Megaloceros proportions and BRANCHED at wapiti proportions --
 * the biggest species of each family has to be the one drawn correctly.
 *
 * The wapiti / red-deer neck mane is PAINTED, not modelled: a mane box was the equid's
 * worst z-fight source. throat_bell (the moose dewlap) shows on stag_moose + moose only.
 */
public class ModelDeer extends AdvancedEntityModel<EntityDeer> {

    private final AdvancedModelBox body_barrel;
    private final AdvancedModelBox body_withers;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck_lower;
    private final AdvancedModelBox neck_upper;
    private final AdvancedModelBox head_skull;
    private final AdvancedModelBox head_muzzle;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox pedicle_left;
    private final AdvancedModelBox palm_beam_left;
    private final AdvancedModelBox palm_lower_left;
    private final AdvancedModelBox palm_upper_left;
    private final AdvancedModelBox palm_digA_left;
    private final AdvancedModelBox palm_digB_left;
    private final AdvancedModelBox palm_digC_left;
    private final AdvancedModelBox palm_brow_left;
    private final AdvancedModelBox beam_low_left;
    private final AdvancedModelBox beam_up_left;
    private final AdvancedModelBox br_crownA_left;
    private final AdvancedModelBox br_crownB_left;
    private final AdvancedModelBox br_brow_left;
    private final AdvancedModelBox br_trez_left;
    private final AdvancedModelBox pedicle_right;
    private final AdvancedModelBox palm_beam_right;
    private final AdvancedModelBox palm_lower_right;
    private final AdvancedModelBox palm_upper_right;
    private final AdvancedModelBox palm_digA_right;
    private final AdvancedModelBox palm_digB_right;
    private final AdvancedModelBox palm_digC_right;
    private final AdvancedModelBox palm_brow_right;
    private final AdvancedModelBox beam_low_right;
    private final AdvancedModelBox beam_up_right;
    private final AdvancedModelBox br_crownA_right;
    private final AdvancedModelBox br_crownB_right;
    private final AdvancedModelBox br_brow_right;
    private final AdvancedModelBox br_trez_right;
    private final AdvancedModelBox throat_bell;
    private final AdvancedModelBox fore_left_shoulder;
    private final AdvancedModelBox fore_left_forearm;
    private final AdvancedModelBox fore_left_cannon;
    private final AdvancedModelBox fore_left_hoof;
    private final AdvancedModelBox fore_right_shoulder;
    private final AdvancedModelBox fore_right_forearm;
    private final AdvancedModelBox fore_right_cannon;
    private final AdvancedModelBox fore_right_hoof;
    private final AdvancedModelBox body_croup;
    private final AdvancedModelBox tail_dock;
    private final AdvancedModelBox tail_tip;
    private final AdvancedModelBox hind_left_thigh;
    private final AdvancedModelBox hind_left_gaskin;
    private final AdvancedModelBox hind_left_cannon;
    private final AdvancedModelBox hind_left_hoof;
    private final AdvancedModelBox hind_right_thigh;
    private final AdvancedModelBox hind_right_gaskin;
    private final AdvancedModelBox hind_right_cannon;
    private final AdvancedModelBox hind_right_hoof;

    private final ModelAnimator animator;

    public ModelDeer() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_barrel = new AdvancedModelBox(this, 0, 0);
        this.body_barrel.setRotationPoint(0.0F, 3.0F, 5.0F);
        this.body_barrel.addBox(-6.5000F, -4.2000F, -11.0F, 13.0F, 10.2000F, 21.0F, 0.0F);
        this.body_withers = new AdvancedModelBox(this, 0, 32);
        this.body_withers.setRotationPoint(0.0F, 1.0F, -3.0F);
        this.body_withers.addBox(-6.1000F, -8.4000F, -6.0F, 12.2000F, 10.9000F, 11.5000F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 202, 0);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -10.0F);
        this.body_chest.addBox(-5.5000F, -3.2000F, -7.5000F, 11.0F, 10.3000F, 13.5000F, 0.0F);
        this.setRotateAngle(body_chest, -0.0873F, 0.0F, 0.0F);
        this.neck_lower = new AdvancedModelBox(this, 48, 32);
        this.neck_lower.setRotationPoint(0.0F, -3.0F, -1.0F);
        this.neck_lower.addBox(-4.2000F, -3.6000F, -10.5000F, 8.4000F, 7.8000F, 11.7000F, 0.0F);
        this.setRotateAngle(neck_lower, -0.5061F, 0.0F, 0.0F);
        this.neck_upper = new AdvancedModelBox(this, 195, 32);
        this.neck_upper.setRotationPoint(0.0F, 0.0F, -10.5000F);
        this.neck_upper.addBox(-3.0F, -3.2000F, -8.5000F, 6.0F, 6.6000F, 9.7000F, 0.0F);
        this.setRotateAngle(neck_upper, -0.1047F, 0.0F, 0.0F);
        this.head_skull = new AdvancedModelBox(this, 58, 55);
        this.head_skull.setRotationPoint(0.0F, 0.0F, -8.5000F);
        this.head_skull.addBox(-3.4000F, -3.4000F, -7.0F, 6.8000F, 6.6000F, 8.2000F, 0.0F);
        this.setRotateAngle(head_skull, 0.8029F, 0.0F, 0.0F);
        this.head_muzzle = new AdvancedModelBox(this, 88, 55);
        this.head_muzzle.setRotationPoint(0.0F, 0.0F, -7.0F);
        this.head_muzzle.addBox(-2.4000F, -2.4000F, -8.2000F, 4.8000F, 5.1000F, 9.4000F, 0.0F);
        this.setRotateAngle(head_muzzle, 0.1396F, 0.0F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 0, 84);
        this.ear_left.setRotationPoint(3.0F, -2.4000F, -0.4000F);
        this.ear_left.addBox(0.0F, -0.2000F, -1.4000F, 7.2000F, 1.0F, 4.4000F, 0.0F);
        this.setRotateAngle(ear_left, -0.1745F, -0.5934F, 0.6981F);
        this.ear_right = new AdvancedModelBox(this, 24, 84);
        this.ear_right.setRotationPoint(-3.0F, -2.4000F, -0.4000F);
        this.ear_right.addBox(-7.2000F, -0.2000F, -1.4000F, 7.2000F, 1.0F, 4.4000F, 0.0F);
        this.setRotateAngle(ear_right, -0.1745F, 0.5934F, -0.6981F);
        this.eye_left = new AdvancedModelBox(this, 48, 84);
        this.eye_left.setRotationPoint(3.7500F, -2.2000F, -2.4000F);
        this.eye_left.addBox(0.0F, -1.5000F, -1.5000F, 0.0F, 3.0F, 3.0F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 54, 84);
        this.eye_right.setRotationPoint(-3.7500F, -2.2000F, -2.4000F);
        this.eye_right.addBox(0.0F, -1.5000F, -1.5000F, 0.0F, 3.0F, 3.0F, 0.0F);
        this.pedicle_left = new AdvancedModelBox(this, 208, 72);
        this.pedicle_left.setRotationPoint(2.3000F, -3.0F, 1.6000F);
        this.pedicle_left.addBox(-1.3000F, -3.2000F, -1.2000F, 2.6000F, 4.0F, 2.4000F, 0.0F);
        this.setRotateAngle(pedicle_left, -0.1745F, 0.0F, 0.2443F);
        this.palm_beam_left = new AdvancedModelBox(this, 163, 55);
        this.palm_beam_left.setRotationPoint(0.0F, -3.0F, 0.0F);
        this.palm_beam_left.addBox(-1.9000F, -8.0F, -2.1000F, 3.8000F, 8.0F, 4.2000F, 0.0F);
        this.setRotateAngle(palm_beam_left, 0.1396F, 0.0F, 1.0123F);
        this.palm_lower_left = new AdvancedModelBox(this, 89, 32);
        this.palm_lower_left.setRotationPoint(0.0F, -6.8000F, 0.0F);
        this.palm_lower_left.addBox(-1.5000F, -7.7000F, -5.2000F, 3.0F, 8.9000F, 11.0F, 0.0F);
        this.setRotateAngle(palm_lower_left, 0.0698F, 0.1047F, 0.2094F);
        this.palm_upper_left = new AdvancedModelBox(this, 68, 0);
        this.palm_upper_left.setRotationPoint(0.0F, -6.2000F, 0.0F);
        this.palm_upper_left.addBox(-1.1000F, -8.0F, -8.4000F, 2.2000F, 9.2000F, 18.0F, 0.0F);
        this.setRotateAngle(palm_upper_left, 0.0F, 0.0698F, 0.1745F);
        this.palm_digA_left = new AdvancedModelBox(this, 44, 72);
        this.palm_digA_left.setRotationPoint(0.0F, -6.0F, -6.0F);
        this.palm_digA_left.addBox(-0.9000F, -6.5000F, -1.8000F, 1.8000F, 8.5000F, 3.2000F, 0.0F);
        this.setRotateAngle(palm_digA_left, 0.2094F, -0.2443F, 0.0698F);
        this.palm_digB_left = new AdvancedModelBox(this, 219, 55);
        this.palm_digB_left.setRotationPoint(0.0F, -6.5000F, 0.5000F);
        this.palm_digB_left.addBox(-0.7000F, -7.5000F, -1.3000F, 1.4000F, 9.5000F, 3.2000F, 0.0F);
        this.setRotateAngle(palm_digB_left, 0.0F, 0.0F, 0.1047F);
        this.palm_digC_left = new AdvancedModelBox(this, 22, 72);
        this.palm_digC_left.setRotationPoint(0.0F, -6.0F, 6.5000F);
        this.palm_digC_left.addBox(-0.9000F, -6.0F, -1.3000F, 1.8000F, 8.0F, 3.2000F, 0.0F);
        this.setRotateAngle(palm_digC_left, -0.1745F, 0.2094F, 0.1396F);
        this.palm_brow_left = new AdvancedModelBox(this, 197, 55);
        this.palm_brow_left.setRotationPoint(0.0F, -1.2000F, 0.0F);
        this.palm_brow_left.addBox(-1.1000F, -8.3000F, -1.6000F, 2.2000F, 9.1000F, 3.2000F, 0.0F);
        this.setRotateAngle(palm_brow_left, 1.1170F, -0.2443F, 0.3491F);
        this.beam_low_left = new AdvancedModelBox(this, 84, 72);
        this.beam_low_left.setRotationPoint(0.0F, -3.0F, 0.0F);
        this.beam_low_left.addBox(-1.2000F, -7.6000F, -1.2000F, 2.4000F, 7.6000F, 2.4000F, 0.0F);
        this.setRotateAngle(beam_low_left, -0.1047F, 0.0F, 0.2269F);
        this.beam_up_left = new AdvancedModelBox(this, 117, 55);
        this.beam_up_left.setRotationPoint(0.0F, -6.4000F, 0.0F);
        this.beam_up_left.addBox(-1.0F, -12.1000F, -1.0F, 2.0F, 12.9000F, 2.0F, 0.0F);
        this.setRotateAngle(beam_up_left, -0.4189F, 0.0F, 0.0F);
        this.br_crownA_left = new AdvancedModelBox(this, 132, 72);
        this.br_crownA_left.setRotationPoint(0.0F, -10.6000F, 0.0F);
        this.br_crownA_left.addBox(-0.7000F, -5.5000F, -0.7000F, 1.4000F, 7.5000F, 1.4000F, 0.0F);
        this.setRotateAngle(br_crownA_left, 0.6981F, -0.1396F, 0.0698F);
        this.br_crownB_left = new AdvancedModelBox(this, 138, 72);
        this.br_crownB_left.setRotationPoint(0.0F, -10.1000F, 0.0F);
        this.br_crownB_left.addBox(-0.7000F, -5.0F, -0.7000F, 1.4000F, 7.0F, 1.4000F, 0.0F);
        this.setRotateAngle(br_crownB_left, -0.5236F, 0.1396F, 0.1047F);
        this.br_brow_left = new AdvancedModelBox(this, 118, 72);
        this.br_brow_left.setRotationPoint(0.0F, -1.2000F, 0.0F);
        this.br_brow_left.addBox(-0.8000F, -6.2000F, -0.8000F, 1.6000F, 7.2000F, 1.6000F, 0.0F);
        this.setRotateAngle(br_brow_left, 1.2566F, -0.1047F, 0.1745F);
        this.br_trez_left = new AdvancedModelBox(this, 104, 72);
        this.br_trez_left.setRotationPoint(0.0F, -4.6000F, 0.0F);
        this.br_trez_left.addBox(-0.8000F, -6.4000F, -0.8000F, 1.6000F, 7.8000F, 1.6000F, 0.0F);
        this.setRotateAngle(br_trez_left, 0.9425F, -0.1396F, 0.2094F);
        this.pedicle_right = new AdvancedModelBox(this, 219, 72);
        this.pedicle_right.setRotationPoint(-2.3000F, -3.0F, 1.6000F);
        this.pedicle_right.addBox(-1.3000F, -3.2000F, -1.2000F, 2.6000F, 4.0F, 2.4000F, 0.0F);
        this.setRotateAngle(pedicle_right, -0.1745F, 0.0F, -0.2443F);
        this.palm_beam_right = new AdvancedModelBox(this, 180, 55);
        this.palm_beam_right.setRotationPoint(0.0F, -3.0F, 0.0F);
        this.palm_beam_right.addBox(-1.9000F, -8.0F, -2.1000F, 3.8000F, 8.0F, 4.2000F, 0.0F);
        this.setRotateAngle(palm_beam_right, 0.1396F, 0.0F, -1.0123F);
        this.palm_lower_right = new AdvancedModelBox(this, 117, 32);
        this.palm_lower_right.setRotationPoint(0.0F, -6.8000F, 0.0F);
        this.palm_lower_right.addBox(-1.5000F, -7.7000F, -5.2000F, 3.0F, 8.9000F, 11.0F, 0.0F);
        this.setRotateAngle(palm_lower_right, 0.0698F, -0.1047F, -0.2094F);
        this.palm_upper_right = new AdvancedModelBox(this, 109, 0);
        this.palm_upper_right.setRotationPoint(0.0F, -6.2000F, 0.0F);
        this.palm_upper_right.addBox(-1.1000F, -8.0F, -8.4000F, 2.2000F, 9.2000F, 18.0F, 0.0F);
        this.setRotateAngle(palm_upper_right, 0.0F, -0.0698F, -0.1745F);
        this.palm_digA_right = new AdvancedModelBox(this, 54, 72);
        this.palm_digA_right.setRotationPoint(0.0F, -6.0F, -6.0F);
        this.palm_digA_right.addBox(-0.9000F, -6.5000F, -1.8000F, 1.8000F, 8.5000F, 3.2000F, 0.0F);
        this.setRotateAngle(palm_digA_right, 0.2094F, 0.2443F, -0.0698F);
        this.palm_digB_right = new AdvancedModelBox(this, 229, 55);
        this.palm_digB_right.setRotationPoint(0.0F, -6.5000F, 0.5000F);
        this.palm_digB_right.addBox(-0.7000F, -7.5000F, -1.3000F, 1.4000F, 9.5000F, 3.2000F, 0.0F);
        this.setRotateAngle(palm_digB_right, 0.0F, 0.0F, -0.1047F);
        this.palm_digC_right = new AdvancedModelBox(this, 33, 72);
        this.palm_digC_right.setRotationPoint(0.0F, -6.0F, 6.5000F);
        this.palm_digC_right.addBox(-0.9000F, -6.0F, -1.3000F, 1.8000F, 8.0F, 3.2000F, 0.0F);
        this.setRotateAngle(palm_digC_right, -0.1745F, -0.2094F, -0.1396F);
        this.palm_brow_right = new AdvancedModelBox(this, 208, 55);
        this.palm_brow_right.setRotationPoint(0.0F, -1.2000F, 0.0F);
        this.palm_brow_right.addBox(-1.1000F, -8.3000F, -1.6000F, 2.2000F, 9.1000F, 3.2000F, 0.0F);
        this.setRotateAngle(palm_brow_right, 1.1170F, 0.2443F, -0.3491F);
        this.beam_low_right = new AdvancedModelBox(this, 94, 72);
        this.beam_low_right.setRotationPoint(0.0F, -3.0F, 0.0F);
        this.beam_low_right.addBox(-1.2000F, -7.6000F, -1.2000F, 2.4000F, 7.6000F, 2.4000F, 0.0F);
        this.setRotateAngle(beam_low_right, -0.1047F, 0.0F, -0.2269F);
        this.beam_up_right = new AdvancedModelBox(this, 125, 55);
        this.beam_up_right.setRotationPoint(0.0F, -6.4000F, 0.0F);
        this.beam_up_right.addBox(-1.0F, -12.1000F, -1.0F, 2.0F, 12.9000F, 2.0F, 0.0F);
        this.setRotateAngle(beam_up_right, -0.4189F, 0.0F, 0.0F);
        this.br_crownA_right = new AdvancedModelBox(this, 144, 72);
        this.br_crownA_right.setRotationPoint(0.0F, -10.6000F, 0.0F);
        this.br_crownA_right.addBox(-0.7000F, -5.5000F, -0.7000F, 1.4000F, 7.5000F, 1.4000F, 0.0F);
        this.setRotateAngle(br_crownA_right, 0.6981F, 0.1396F, -0.0698F);
        this.br_crownB_right = new AdvancedModelBox(this, 150, 72);
        this.br_crownB_right.setRotationPoint(0.0F, -10.1000F, 0.0F);
        this.br_crownB_right.addBox(-0.7000F, -5.0F, -0.7000F, 1.4000F, 7.0F, 1.4000F, 0.0F);
        this.setRotateAngle(br_crownB_right, -0.5236F, -0.1396F, -0.1047F);
        this.br_brow_right = new AdvancedModelBox(this, 125, 72);
        this.br_brow_right.setRotationPoint(0.0F, -1.2000F, 0.0F);
        this.br_brow_right.addBox(-0.8000F, -6.2000F, -0.8000F, 1.6000F, 7.2000F, 1.6000F, 0.0F);
        this.setRotateAngle(br_brow_right, 1.2566F, 0.1047F, -0.1745F);
        this.br_trez_right = new AdvancedModelBox(this, 111, 72);
        this.br_trez_right.setRotationPoint(0.0F, -4.6000F, 0.0F);
        this.br_trez_right.addBox(-0.8000F, -6.4000F, -0.8000F, 1.6000F, 7.8000F, 1.6000F, 0.0F);
        this.setRotateAngle(br_trez_right, 0.9425F, 0.1396F, -0.2094F);
        this.throat_bell = new AdvancedModelBox(this, 239, 55);
        this.throat_bell.setRotationPoint(0.0F, 3.0F, -4.5000F);
        this.throat_bell.addBox(-1.7000F, -0.4000F, -1.6000F, 3.4000F, 7.4000F, 4.0F, 0.0F);
        this.fore_left_shoulder = new AdvancedModelBox(this, 227, 32);
        this.fore_left_shoulder.setRotationPoint(4.5000F, -5.3205F, 0.0F);
        this.fore_left_shoulder.addBox(-2.3000F, -1.6000F, -3.4000F, 4.6000F, 11.0F, 6.0F, 0.0F);
        this.setRotateAngle(fore_left_shoulder, -0.0349F, 0.0F, 0.0F);
        this.fore_left_forearm = new AdvancedModelBox(this, 133, 55);
        this.fore_left_forearm.setRotationPoint(-0.7500F, 9.4000F, 0.0F);
        this.fore_left_forearm.addBox(-1.2500F, -1.2000F, -2.5000F, 2.5000F, 9.0F, 4.6000F, 0.0F);
        this.setRotateAngle(fore_left_forearm, 0.2094F, 0.0F, 0.0F);
        this.fore_left_cannon = new AdvancedModelBox(this, 64, 72);
        this.fore_left_cannon.setRotationPoint(-0.0500F, 7.8000F, 0.0F);
        this.fore_left_cannon.addBox(-0.9000F, -1.0F, -1.5000F, 1.8000F, 7.6000F, 2.8000F, 0.0F);
        this.setRotateAngle(fore_left_cannon, -0.0873F, 0.0F, 0.0F);
        this.fore_left_hoof = new AdvancedModelBox(this, 230, 72);
        this.fore_left_hoof.setRotationPoint(0.0F, 6.6000F, 0.0F);
        this.fore_left_hoof.addBox(-0.7000F, -0.6000F, -2.2000F, 1.4000F, 3.2000F, 3.4000F, 0.0F);
        this.fore_right_shoulder = new AdvancedModelBox(this, 0, 55);
        this.fore_right_shoulder.setRotationPoint(-4.5000F, -5.3205F, 0.0F);
        this.fore_right_shoulder.addBox(-2.3000F, -1.6000F, -3.4000F, 4.6000F, 11.0F, 6.0F, 0.0F);
        this.setRotateAngle(fore_right_shoulder, -0.0349F, 0.0F, 0.0F);
        this.fore_right_forearm = new AdvancedModelBox(this, 148, 55);
        this.fore_right_forearm.setRotationPoint(0.7500F, 9.4000F, 0.0F);
        this.fore_right_forearm.addBox(-1.2500F, -1.2000F, -2.5000F, 2.5000F, 9.0F, 4.6000F, 0.0F);
        this.setRotateAngle(fore_right_forearm, 0.2094F, 0.0F, 0.0F);
        this.fore_right_cannon = new AdvancedModelBox(this, 74, 72);
        this.fore_right_cannon.setRotationPoint(0.0500F, 7.8000F, 0.0F);
        this.fore_right_cannon.addBox(-0.9000F, -1.0F, -1.5000F, 1.8000F, 7.6000F, 2.8000F, 0.0F);
        this.setRotateAngle(fore_right_cannon, -0.0873F, 0.0F, 0.0F);
        this.fore_right_hoof = new AdvancedModelBox(this, 240, 72);
        this.fore_right_hoof.setRotationPoint(0.0F, 6.6000F, 0.0F);
        this.fore_right_hoof.addBox(-0.7000F, -0.6000F, -2.2000F, 1.4000F, 3.2000F, 3.4000F, 0.0F);
        this.body_croup = new AdvancedModelBox(this, 150, 0);
        this.body_croup.setRotationPoint(0.0F, -1.5000F, 8.0F);
        this.body_croup.addBox(-6.3000F, -4.3000F, -4.0F, 12.6000F, 11.2000F, 13.0F, 0.0F);
        this.setRotateAngle(body_croup, 0.1396F, 0.0F, 0.0F);
        this.tail_dock = new AdvancedModelBox(this, 156, 72);
        this.tail_dock.setRotationPoint(0.0F, -3.0000F, 9.5000F);
        this.tail_dock.addBox(-1.2000F, -1.0F, -4.4000F, 2.4000F, 2.4000F, 5.4000F, 0.0F);
        this.setRotateAngle(tail_dock, 1.7802F, 0.0F, 0.0F);
        this.tail_tip = new AdvancedModelBox(this, 172, 72);
        this.tail_tip.setRotationPoint(0.0F, 0.0F, -4.4000F);
        this.tail_tip.addBox(-0.9000F, -0.8000F, -4.0F, 1.8000F, 1.9000F, 4.9000F, 0.0F);
        this.setRotateAngle(tail_tip, 0.0873F, 0.0F, 0.0F);
        this.hind_left_thigh = new AdvancedModelBox(this, 145, 32);
        this.hind_left_thigh.setRotationPoint(4.3000F, -2.4981F, 4.5000F);
        this.hind_left_thigh.addBox(-2.7000F, -1.8000F, -3.8000F, 5.4000F, 11.3000F, 7.0F, 0.0F);
        this.setRotateAngle(hind_left_thigh, -0.6632F, 0.0F, 0.0F);
        this.hind_left_gaskin = new AdvancedModelBox(this, 22, 55);
        this.hind_left_gaskin.setRotationPoint(-0.3000F, 9.5000F, 0.0F);
        this.hind_left_gaskin.addBox(-1.9000F, -1.4000F, -2.6000F, 3.8000F, 10.4000F, 5.0F, 0.0F);
        this.setRotateAngle(hind_left_gaskin, 1.2217F, 0.0F, 0.0F);
        this.hind_left_cannon = new AdvancedModelBox(this, 0, 72);
        this.hind_left_cannon.setRotationPoint(0.1500F, 9.0F, 0.0F);
        this.hind_left_cannon.addBox(-1.1500F, -1.0F, -1.5000F, 2.3000F, 9.0F, 2.8000F, 0.0F);
        this.setRotateAngle(hind_left_cannon, -0.8727F, 0.0F, 0.0F);
        this.hind_left_hoof = new AdvancedModelBox(this, 186, 72);
        this.hind_left_hoof.setRotationPoint(0.0500F, 8.0F, 0.0F);
        this.hind_left_hoof.addBox(-0.9000F, -0.6000F, -2.2000F, 1.8000F, 3.2000F, 3.4000F, 0.0F);
        this.setRotateAngle(hind_left_hoof, 0.1745F, 0.0F, 0.0F);
        this.hind_right_thigh = new AdvancedModelBox(this, 170, 32);
        this.hind_right_thigh.setRotationPoint(-4.3000F, -2.4981F, 4.5000F);
        this.hind_right_thigh.addBox(-2.7000F, -1.8000F, -3.8000F, 5.4000F, 11.3000F, 7.0F, 0.0F);
        this.setRotateAngle(hind_right_thigh, -0.6632F, 0.0F, 0.0F);
        this.hind_right_gaskin = new AdvancedModelBox(this, 40, 55);
        this.hind_right_gaskin.setRotationPoint(0.3000F, 9.5000F, 0.0F);
        this.hind_right_gaskin.addBox(-1.9000F, -1.4000F, -2.6000F, 3.8000F, 10.4000F, 5.0F, 0.0F);
        this.setRotateAngle(hind_right_gaskin, 1.2217F, 0.0F, 0.0F);
        this.hind_right_cannon = new AdvancedModelBox(this, 11, 72);
        this.hind_right_cannon.setRotationPoint(-0.1500F, 9.0F, 0.0F);
        this.hind_right_cannon.addBox(-1.1500F, -1.0F, -1.5000F, 2.3000F, 9.0F, 2.8000F, 0.0F);
        this.setRotateAngle(hind_right_cannon, -0.8727F, 0.0F, 0.0F);
        this.hind_right_hoof = new AdvancedModelBox(this, 197, 72);
        this.hind_right_hoof.setRotationPoint(-0.0500F, 8.0F, 0.0F);
        this.hind_right_hoof.addBox(-0.9000F, -0.6000F, -2.2000F, 1.8000F, 3.2000F, 3.4000F, 0.0F);
        this.setRotateAngle(hind_right_hoof, 0.1745F, 0.0F, 0.0F);

        this.body_barrel.addChild(this.body_withers);
        this.body_barrel.addChild(this.body_chest);
        this.body_chest.addChild(this.neck_lower);
        this.neck_lower.addChild(this.neck_upper);
        this.neck_upper.addChild(this.head_skull);
        this.head_skull.addChild(this.head_muzzle);
        this.head_skull.addChild(this.ear_left);
        this.head_skull.addChild(this.ear_right);
        this.head_skull.addChild(this.eye_left);
        this.head_skull.addChild(this.eye_right);
        this.head_skull.addChild(this.pedicle_left);
        this.pedicle_left.addChild(this.palm_beam_left);
        this.palm_beam_left.addChild(this.palm_lower_left);
        this.palm_lower_left.addChild(this.palm_upper_left);
        this.palm_upper_left.addChild(this.palm_digA_left);
        this.palm_upper_left.addChild(this.palm_digB_left);
        this.palm_upper_left.addChild(this.palm_digC_left);
        this.palm_beam_left.addChild(this.palm_brow_left);
        this.pedicle_left.addChild(this.beam_low_left);
        this.beam_low_left.addChild(this.beam_up_left);
        this.beam_up_left.addChild(this.br_crownA_left);
        this.beam_up_left.addChild(this.br_crownB_left);
        this.beam_low_left.addChild(this.br_brow_left);
        this.beam_low_left.addChild(this.br_trez_left);
        this.head_skull.addChild(this.pedicle_right);
        this.pedicle_right.addChild(this.palm_beam_right);
        this.palm_beam_right.addChild(this.palm_lower_right);
        this.palm_lower_right.addChild(this.palm_upper_right);
        this.palm_upper_right.addChild(this.palm_digA_right);
        this.palm_upper_right.addChild(this.palm_digB_right);
        this.palm_upper_right.addChild(this.palm_digC_right);
        this.palm_beam_right.addChild(this.palm_brow_right);
        this.pedicle_right.addChild(this.beam_low_right);
        this.beam_low_right.addChild(this.beam_up_right);
        this.beam_up_right.addChild(this.br_crownA_right);
        this.beam_up_right.addChild(this.br_crownB_right);
        this.beam_low_right.addChild(this.br_brow_right);
        this.beam_low_right.addChild(this.br_trez_right);
        this.neck_upper.addChild(this.throat_bell);
        this.body_chest.addChild(this.fore_left_shoulder);
        this.fore_left_shoulder.addChild(this.fore_left_forearm);
        this.fore_left_forearm.addChild(this.fore_left_cannon);
        this.fore_left_cannon.addChild(this.fore_left_hoof);
        this.body_chest.addChild(this.fore_right_shoulder);
        this.fore_right_shoulder.addChild(this.fore_right_forearm);
        this.fore_right_forearm.addChild(this.fore_right_cannon);
        this.fore_right_cannon.addChild(this.fore_right_hoof);
        this.body_barrel.addChild(this.body_croup);
        this.body_croup.addChild(this.tail_dock);
        this.tail_dock.addChild(this.tail_tip);
        this.body_croup.addChild(this.hind_left_thigh);
        this.hind_left_thigh.addChild(this.hind_left_gaskin);
        this.hind_left_gaskin.addChild(this.hind_left_cannon);
        this.hind_left_cannon.addChild(this.hind_left_hoof);
        this.body_croup.addChild(this.hind_right_thigh);
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

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(
            body_barrel,
            body_withers,
            body_chest,
            neck_lower,
            neck_upper,
            head_skull,
            head_muzzle,
            ear_left,
            ear_right,
            eye_left,
            eye_right,
            pedicle_left,
            palm_beam_left,
            palm_lower_left,
            palm_upper_left,
            palm_digA_left,
            palm_digB_left,
            palm_digC_left,
            palm_brow_left,
            beam_low_left,
            beam_up_left,
            br_crownA_left,
            br_crownB_left,
            br_brow_left,
            br_trez_left,
            pedicle_right,
            palm_beam_right,
            palm_lower_right,
            palm_upper_right,
            palm_digA_right,
            palm_digB_right,
            palm_digC_right,
            palm_brow_right,
            beam_low_right,
            beam_up_right,
            br_crownA_right,
            br_crownB_right,
            br_brow_right,
            br_trez_right,
            throat_bell,
            fore_left_shoulder,
            fore_left_forearm,
            fore_left_cannon,
            fore_left_hoof,
            fore_right_shoulder,
            fore_right_forearm,
            fore_right_cannon,
            fore_right_hoof,
            body_croup,
            tail_dock,
            tail_tip,
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

    private void animate(IAnimatedEntity entityIn) {
        EntityDeer deer = (EntityDeer) entityIn;
        animator.update(deer);

        // ATTACK_THREATEN (50t) -- head lowered, rack presented, weight rocking side to side.
        animator.setAnimation(EntityDeer.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_withers, 0, 0, 7.83F);
            this.rotate(animator, neck_lower, 24.0F, 0, -10.0F);
            this.rotate(animator, neck_upper, 14.0F, 0, 0);
            this.rotate(animator, head_skull, 10.0F, 0, 0);
            this.rotate(animator, fore_left_shoulder, -22.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, 18.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_withers, 0, 0, -13.05F);
            this.rotate(animator, neck_lower, 24.0F, 0, 20.0F);
            this.rotate(animator, neck_upper, 14.0F, 0, 0);
            this.rotate(animator, head_skull, 10.0F, 0, 0);
            this.rotate(animator, fore_left_shoulder, 18.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, -22.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_GORE (14t) -- rear back, then drive the rack down and forward.
        animator.setAnimation(EntityDeer.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck_lower, -26.0F, 0, 0);
        this.rotate(animator, neck_upper, -18.0F, 0, 0);
        this.rotate(animator, head_skull, -14.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck_lower, 42.0F, 0, 0);
        this.rotate(animator, neck_upper, 30.0F, 0, 0);
        this.rotate(animator, head_skull, 22.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityDeer deer, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(deer);
        float globalSpeed = 1.5f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- per-species toggles, read straight from the species data (ModelBison pattern) ----
        int variant = deer.getVariant();
        boolean palmate = ComplexMob.getEntityData(deer.getType()).getFlags(variant, "palmate") == 1;
        // showModel, NOT setScale: a scale of 0 does not reach children unless
        // setShouldScaleChildren(true) is set, which would fight the antler sizing below.
        this.palm_beam_left.showModel = palmate;
        this.palm_beam_right.showModel = palmate;
        this.beam_low_left.showModel = !palmate;
        this.beam_low_right.showModel = !palmate;
        this.throat_bell.showModel = ComplexMob.getEntityData(deer.getType()).getFlags(variant, "throatBell") == 1;

        // One scale on the shared pedicle drives the whole rack. Each value is span /
        // (1.725 * species scale), calibrated off wapiti (0.887 @ scale 0.817 = a 1.25 m span).
        // getFlags returns an Integer -- there is no float flag path -- so antlerScale is
        // authored in PER-MILLE, which the shipped values (0.434, 0.407, ...) need to survive
        // the round trip exactly.
        float rack = ComplexMob.getEntityData(deer.getType()).getFlags(variant, "antlerScale") / 1000F;
        this.pedicle_left.setShouldScaleChildren(true);
        this.pedicle_right.setShouldScaleChildren(true);
        this.pedicle_left.setScale(rack, rack, rack);
        this.pedicle_right.setScale(rack, rack, rack);

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.05F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        walk(neck_lower, 0.35f * globalSpeed, 0.035f, false, 2.8F, 0.05F, ageInTicks / 20, 2);
        this.swing(tail_dock, 0.10F, 0.12F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_tip, 0.10F, 0.18F, false, 1F, 0F, ageInTicks, 1);

        // Blinking: bury the eye planes inside the skull. y/z MUST keep the
        // constructor's values or the eye jumps across the face on every blink.
        if (!deer.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(2.3250F, -2.2000F, -2.4000F);
            this.eye_right.setRotationPoint(-2.3250F, -2.2000F, -2.4000F);
        }

        // Head tracking -- a cervid neck is long and mobile, so it turns freely.
        if (!deer.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck_lower);
            this.faceTarget(netHeadYaw, headPitch, 3, neck_upper);
            this.faceTarget(netHeadYaw, headPitch, 3, head_skull);
        }

        if (deer.isInWater()) {
            this.setRotateAngle(head_skull, -0.1820F, 0.0F, 0.0F);
            if (!deer.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(deer.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // ---- gait ----
        if (deer.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.6f * globalDegree, true, f, limbSwingAmount);
            walk(neck_lower, 0.8f * globalSpeed, 0.2f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head_skull, 0.8f * globalSpeed, 0.15f * globalDegree, true, 0, 0, f, limbSwingAmount);
            walk(fore_right_shoulder, -0.8f * globalSpeed, 1.2f * globalDegree, true, 0F, 1.2f, f, limbSwingAmount);
            walk(fore_right_forearm, -0.8f * globalSpeed, 1.2f * globalDegree, false, -1F, 1.2f, f, limbSwingAmount * 1.2f);
            walk(fore_right_cannon, -0.8f * globalSpeed, 0.8f * globalDegree, false, -2F, 0.8f, f, limbSwingAmount);
            walk(fore_left_shoulder, -0.8f * globalSpeed, 1.2f * globalDegree, true, 2F, 1.2f, f, limbSwingAmount);
            walk(fore_left_forearm, -0.8f * globalSpeed, 1.2f * globalDegree, false, 1F, 1.2f, f, limbSwingAmount * 1.2f);
            walk(fore_left_cannon, -0.8f * globalSpeed, 0.8f * globalDegree, false, 0F, 0.8f, f, limbSwingAmount);
            walk(hind_right_thigh, 0.8f * globalSpeed, 1.2f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(hind_right_gaskin, 0.8f * globalSpeed, 1.2f * globalDegree, true, 1.8F, 0, f, limbSwingAmount);
            walk(hind_right_cannon, 0.8f * globalSpeed, 0.8f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(hind_left_thigh, 0.8f * globalSpeed, 1.2f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(hind_left_gaskin, 0.8f * globalSpeed, 1.2f * globalDegree, true, -0.2F, 0, f, limbSwingAmount);
            walk(hind_left_cannon, 0.8f * globalSpeed, 0.8f * globalDegree, false, -1.2F, 0, f, limbSwingAmount);
        }

        if (deer.sitProgress > 0) {
            // Solved by grid search against real box geometry (deer_spec.py --pose):
            // belly drops 13.40u, every leg box stays above y=0 and the
            // cannon + hoof of all four limbs rest ON the floor.
            this.progressPosition(body_barrel, deer.sitProgress, 0.0F, 16.4000F, 0.0F, 40);
            this.progressRotation(fore_left_shoulder, deer.sitProgress, 1.2540F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, deer.sitProgress, -1.0519F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, deer.sitProgress, -1.6819F, 0, 0, 40);
            this.progressRotation(fore_left_hoof, deer.sitProgress, 0.0194F, 0, 0, 40);
            this.progressRotation(fore_right_shoulder, deer.sitProgress, 1.2540F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, deer.sitProgress, -1.0519F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, deer.sitProgress, -1.6819F, 0, 0, 40);
            this.progressRotation(fore_right_hoof, deer.sitProgress, 0.0194F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, deer.sitProgress, -1.4732F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, deer.sitProgress, 0.9849F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, deer.sitProgress, -1.2127F, 0, 0, 40);
            this.progressRotation(hind_left_hoof, deer.sitProgress, 0.0195F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, deer.sitProgress, -1.4732F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, deer.sitProgress, 0.9849F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, deer.sitProgress, -1.2127F, 0, 0, 40);
            this.progressRotation(hind_right_hoof, deer.sitProgress, 0.0195F, 0, 0, 40);
        }

        else if (deer.sleepProgress > 0) {
            // Solved by grid search against real box geometry (deer_spec.py --pose):
            // belly drops 14.30u, every leg box stays above y=0 and the
            // cannon + hoof of all four limbs rest ON the floor.
            this.progressPosition(body_barrel, deer.sleepProgress, 0.0F, 17.3000F, 0.0F, 40);
            this.progressRotation(fore_left_shoulder, deer.sleepProgress, -1.0359F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, deer.sleepProgress, 1.7183F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, deer.sleepProgress, -2.1611F, 0, 0, 40);
            this.progressRotation(fore_left_hoof, deer.sleepProgress, 0.0235F, 0, 0, 40);
            this.progressRotation(fore_right_shoulder, deer.sleepProgress, -1.0359F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, deer.sleepProgress, 1.7183F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, deer.sleepProgress, -2.1611F, 0, 0, 40);
            this.progressRotation(fore_right_hoof, deer.sleepProgress, 0.0235F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, deer.sleepProgress, -1.6251F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, deer.sleepProgress, 1.4488F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, deer.sleepProgress, -1.5283F, 0, 0, 40);
            this.progressRotation(hind_left_hoof, deer.sleepProgress, 0.0263F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, deer.sleepProgress, -1.6251F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, deer.sleepProgress, 1.4488F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, deer.sleepProgress, -1.5283F, 0, 0, 40);
            this.progressRotation(hind_right_hoof, deer.sleepProgress, 0.0263F, 0, 0, 40);
            this.progressRotation(neck_lower, deer.sleepProgress, 0.6981F, 0, 0, 40);
            this.progressRotation(neck_upper, deer.sleepProgress, -0.5236F, 0, 0, 40);
            this.progressRotation(head_skull, deer.sleepProgress, -0.6109F, 0, 0, 40);
        }

    }

}
