package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityGiraffid;

/**
 * Giraffid -- 50 boxes on 256x128, replacing a raw ModelBison fork.
 *
 * The class this supersedes was the bison body with two 1x3x1 sticks bolted to the skull as
 * ossicones; it still declared the bison's horns (hidden with setScale(0,0,0) every frame),
 * beard, forelock and limb wool, and its sit/sleep fold angles were ModelBison's applied
 * unchanged to an animal whose legs are three times as long.
 *
 * Sculpted from scripts/giraffid_spec.py, which carries the spec, the forward-kinematics
 * checker, the z-fight sweep, the volume-burial check, a midline-crossing check, the UV
 * shelf pack and the phase-7 pose solver. Box coords + texOffs are transcribed VERBATIM
 * from the modded_entity export (the Y-down flip is the export's, never re-derived);
 * scripts/gen_model_giraffid.py does the transcription and scripts/verify_port.py proves it.
 *
 * Three things carry the family and the rig is built around them:
 *   * THE NECK -- four segments at 64/73/82/62 world degrees, curving back as it rises and
 *     then forward into the head. 25.8u of neck on a 38.5u withers.
 *   * THE DOWNHILL TOPLINE -- the withers stands 6.43u above the rump and the back falls
 *     away continuously. This is the exact inverse of ModelTapir's high rounded croup, and
 *     it is what stops the two rigs reading as one body retuned.
 *   * LONG STRAIGHT LEGS -- belly at 61% of withers against the tapir's 47%, and the fore
 *     drop deliberately exceeds the hind by 3.6u, which is mechanically WHY the back slopes.
 *
 * SEVEN species share this one mesh. giraffid.json declares no synced flag beyond
 * groupCount and EntityGiraffid adds none, so both mechanisms read getVariant().
 */
public class ModelGiraffid extends AdvancedEntityModel<EntityGiraffid> {

    private final AdvancedModelBox body_barrel;
    private final AdvancedModelBox body_withers;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck_1;
    private final AdvancedModelBox neck_2;
    private final AdvancedModelBox neck_3;
    private final AdvancedModelBox neck_4;
    private final AdvancedModelBox head_skull;
    private final AdvancedModelBox head_muzzle;
    private final AdvancedModelBox head_jaw;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox oss_taper_left;
    private final AdvancedModelBox oss_taper_right;
    private final AdvancedModelBox oss_boss;
    private final AdvancedModelBox oss_palm_base_left;
    private final AdvancedModelBox oss_palm_left;
    private final AdvancedModelBox oss_palm_base_right;
    private final AdvancedModelBox oss_palm_right;
    private final AdvancedModelBox oss_spike_left;
    private final AdvancedModelBox oss_spike_right;
    private final AdvancedModelBox oss_quad_left;
    private final AdvancedModelBox oss_quad_front_left;
    private final AdvancedModelBox oss_quad_right;
    private final AdvancedModelBox oss_quad_front_right;
    private final AdvancedModelBox mane_4;
    private final AdvancedModelBox mane_3;
    private final AdvancedModelBox mane_2;
    private final AdvancedModelBox mane_1;
    private final AdvancedModelBox fore_left_shoulder;
    private final AdvancedModelBox fore_left_forearm;
    private final AdvancedModelBox fore_left_cannon;
    private final AdvancedModelBox fore_left_foot;
    private final AdvancedModelBox fore_right_shoulder;
    private final AdvancedModelBox fore_right_forearm;
    private final AdvancedModelBox fore_right_cannon;
    private final AdvancedModelBox fore_right_foot;
    private final AdvancedModelBox body_croup;
    private final AdvancedModelBox tail_dock;
    private final AdvancedModelBox tail_tassel;
    private final AdvancedModelBox hind_left_thigh;
    private final AdvancedModelBox hind_left_gaskin;
    private final AdvancedModelBox hind_left_cannon;
    private final AdvancedModelBox hind_left_foot;
    private final AdvancedModelBox hind_right_thigh;
    private final AdvancedModelBox hind_right_gaskin;
    private final AdvancedModelBox hind_right_cannon;
    private final AdvancedModelBox hind_right_foot;

    private final ModelAnimator animator;

    public ModelGiraffid() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_barrel = new AdvancedModelBox(this, 0, 0);
        this.body_barrel.setRotationPoint(0.0F, -7.0F, 4.0F);
        this.body_barrel.addBox(-7.6000F, -5.0F, -10.0F, 15.2000F, 12.5000F, 21.0F, 0.0F);
        this.body_withers = new AdvancedModelBox(this, 73, 0);
        this.body_withers.setRotationPoint(0.0F, 0.0F, -0.5000F);
        this.body_withers.addBox(-7.0F, -6.4000F, -14.5000F, 14.0F, 10.4000F, 18.0F, 0.0F);
        this.setRotateAngle(body_withers, -0.1396F, 0.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 137, 0);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -9.0F);
        this.body_chest.addBox(-6.2000F, -5.6000F, -7.0F, 12.4000F, 12.0F, 11.0F, 0.0F);
        this.setRotateAngle(body_chest, 0.1396F, 0.0F, 0.0F);
        this.neck_1 = new AdvancedModelBox(this, 215, 34);
        this.neck_1.setRotationPoint(0.0F, -5.6000F, -4.0F);
        this.neck_1.addBox(-4.6000F, -3.6000F, -6.6000F, 9.2000F, 7.0F, 9.2000F, 0.0F);
        this.setRotateAngle(neck_1, -1.2566F, 0.0F, 0.0F);
        this.neck_2 = new AdvancedModelBox(this, 65, 56);
        this.neck_2.setRotationPoint(0.0F, 0.0F, -6.6000F);
        this.neck_2.addBox(-4.3500F, -3.4000F, -6.6000F, 8.7000F, 6.6000F, 9.0F, 0.0F);
        this.setRotateAngle(neck_2, -0.1571F, 0.0F, 0.0F);
        this.neck_3 = new AdvancedModelBox(this, 101, 56);
        this.neck_3.setRotationPoint(0.0F, 0.0F, -6.6000F);
        this.neck_3.addBox(-4.1000F, -3.2000F, -6.4000F, 8.2000F, 6.2000F, 8.8000F, 0.0F);
        this.setRotateAngle(neck_3, -0.1571F, 0.0F, 0.0F);
        this.neck_4 = new AdvancedModelBox(this, 167, 56);
        this.neck_4.setRotationPoint(0.0F, 0.0F, -6.4000F);
        this.neck_4.addBox(-3.8500F, -3.0F, -6.2000F, 7.7000F, 5.8000F, 8.6000F, 0.0F);
        this.setRotateAngle(neck_4, 0.3491F, 0.0F, 0.0F);
        this.head_skull = new AdvancedModelBox(this, 0, 56);
        this.head_skull.setRotationPoint(0.0F, 0.0F, -6.2000F);
        this.head_skull.addBox(-2.9000F, -3.4000F, -8.2000F, 5.8000F, 6.4000F, 10.6000F, 0.0F);
        this.setRotateAngle(head_skull, 0.4538F, 0.0F, 0.0F);
        this.head_muzzle = new AdvancedModelBox(this, 36, 73);
        this.head_muzzle.setRotationPoint(0.0F, 0.0F, -8.2000F);
        this.head_muzzle.addBox(-2.4000F, -2.4000F, -5.0F, 4.8000F, 4.9000F, 6.8000F, 0.0F);
        this.setRotateAngle(head_muzzle, 0.2094F, 0.0F, 0.0F);
        this.head_jaw = new AdvancedModelBox(this, 200, 56);
        this.head_jaw.setRotationPoint(0.0F, 1.4000F, -3.2000F);
        this.head_jaw.addBox(-1.9000F, -1.2000F, -8.6000F, 3.8000F, 3.4000F, 11.6000F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 82, 87);
        this.ear_left.setRotationPoint(2.6000F, -2.6000F, -1.2000F);
        this.ear_left.addBox(0.0F, -1.4000F, -1.6000F, 4.8000F, 2.6000F, 3.2000F, 0.0F);
        this.setRotateAngle(ear_left, -0.1745F, -0.3491F, -0.7330F);
        this.ear_right = new AdvancedModelBox(this, 99, 87);
        this.ear_right.setRotationPoint(-2.6000F, -2.6000F, -1.2000F);
        this.ear_right.addBox(-4.8000F, -1.4000F, -1.6000F, 4.8000F, 2.6000F, 3.2000F, 0.0F);
        this.setRotateAngle(ear_right, -0.1745F, 0.3491F, 0.7330F);
        this.eye_left = new AdvancedModelBox(this, 132, 87);
        this.eye_left.setRotationPoint(3.4000F, -1.4000F, -5.8000F);
        this.eye_left.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 136, 87);
        this.eye_right.setRotationPoint(-3.4000F, -1.4000F, -5.8000F);
        this.eye_right.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.oss_taper_left = new AdvancedModelBox(this, 60, 87);
        this.oss_taper_left.setRotationPoint(1.7000F, -3.4000F, -3.6000F);
        this.oss_taper_left.addBox(-1.2000F, -3.6000F, -1.4000F, 2.4000F, 4.2000F, 2.8000F, 0.0F);
        this.setRotateAngle(oss_taper_left, 0.1571F, -0.1047F, 0.1396F);
        this.oss_taper_right = new AdvancedModelBox(this, 71, 87);
        this.oss_taper_right.setRotationPoint(-1.7000F, -3.4000F, -3.6000F);
        this.oss_taper_right.addBox(-1.2000F, -3.6000F, -1.4000F, 2.4000F, 4.2000F, 2.8000F, 0.0F);
        this.setRotateAngle(oss_taper_right, 0.1571F, 0.1047F, -0.1396F);
        this.oss_boss = new AdvancedModelBox(this, 44, 87);
        this.oss_boss.setRotationPoint(0.0F, -3.0F, -7.6000F);
        this.oss_boss.addBox(-2.2000F, -2.2000F, -1.6000F, 4.4000F, 3.0F, 3.2000F, 0.0F);
        this.setRotateAngle(oss_boss, 0.2793F, 0.0F, 0.0F);
        this.oss_palm_base_left = new AdvancedModelBox(this, 0, 87);
        this.oss_palm_base_left.setRotationPoint(2.2000F, -3.2000F, -2.6000F);
        this.oss_palm_base_left.addBox(-1.3000F, -3.4000F, -1.6000F, 2.8000F, 4.2000F, 3.2000F, 0.0F);
        this.setRotateAngle(oss_palm_base_left, 0.0698F, -0.1396F, 0.3491F);
        this.oss_palm_left = new AdvancedModelBox(this, 0, 73);
        this.oss_palm_left.setRotationPoint(0.7000F, -3.0F, 0.0F);
        this.oss_palm_left.addBox(-0.7000F, -5.6000F, -3.6000F, 1.4000F, 6.2000F, 7.2000F, 0.0F);
        this.setRotateAngle(oss_palm_left, 0.1396F, 0.2443F, 0.3840F);
        this.oss_palm_base_right = new AdvancedModelBox(this, 13, 87);
        this.oss_palm_base_right.setRotationPoint(-2.2000F, -3.2000F, -2.6000F);
        this.oss_palm_base_right.addBox(-1.5000F, -3.4000F, -1.6000F, 2.8000F, 4.2000F, 3.2000F, 0.0F);
        this.setRotateAngle(oss_palm_base_right, 0.0698F, 0.1396F, -0.3491F);
        this.oss_palm_right = new AdvancedModelBox(this, 18, 73);
        this.oss_palm_right.setRotationPoint(-0.7000F, -3.0F, 0.0F);
        this.oss_palm_right.addBox(-0.7000F, -5.6000F, -3.6000F, 1.4000F, 6.2000F, 7.2000F, 0.0F);
        this.setRotateAngle(oss_palm_right, 0.1396F, -0.2443F, -0.3840F);
        this.oss_spike_left = new AdvancedModelBox(this, 26, 87);
        this.oss_spike_left.setRotationPoint(2.4000F, -3.2000F, -5.4000F);
        this.oss_spike_left.addBox(-1.2000F, -4.6000F, -1.2000F, 2.0F, 5.2000F, 2.4000F, 0.0F);
        this.setRotateAngle(oss_spike_left, -0.4189F, -0.1047F, 0.2094F);
        this.oss_spike_right = new AdvancedModelBox(this, 35, 87);
        this.oss_spike_right.setRotationPoint(-2.4000F, -3.2000F, -5.4000F);
        this.oss_spike_right.addBox(-0.8000F, -4.6000F, -1.2000F, 2.0F, 5.2000F, 2.4000F, 0.0F);
        this.setRotateAngle(oss_spike_right, -0.4189F, 0.1047F, -0.2094F);
        this.oss_quad_left = new AdvancedModelBox(this, 221, 73);
        this.oss_quad_left.setRotationPoint(1.6000F, -3.2000F, -2.8000F);
        this.oss_quad_left.addBox(-1.1000F, -4.4000F, -1.6000F, 2.8000F, 5.2000F, 3.2000F, 0.0F);
        this.setRotateAngle(oss_quad_left, 0.1047F, -0.1745F, 0.4189F);
        this.oss_quad_front_left = new AdvancedModelBox(this, 116, 87);
        this.oss_quad_front_left.setRotationPoint(2.0F, -3.0F, -6.6000F);
        this.oss_quad_front_left.addBox(-1.1000F, -2.6000F, -1.0F, 1.8000F, 3.2000F, 2.2000F, 0.0F);
        this.setRotateAngle(oss_quad_front_left, -0.2793F, -0.1047F, 0.2443F);
        this.oss_quad_right = new AdvancedModelBox(this, 234, 73);
        this.oss_quad_right.setRotationPoint(-1.6000F, -3.2000F, -2.8000F);
        this.oss_quad_right.addBox(-1.7000F, -4.4000F, -1.6000F, 2.8000F, 5.2000F, 3.2000F, 0.0F);
        this.setRotateAngle(oss_quad_right, 0.1047F, 0.1745F, -0.4189F);
        this.oss_quad_front_right = new AdvancedModelBox(this, 124, 87);
        this.oss_quad_front_right.setRotationPoint(-2.0F, -3.0F, -6.6000F);
        this.oss_quad_front_right.addBox(-0.7000F, -2.6000F, -1.0F, 1.8000F, 3.2000F, 2.2000F, 0.0F);
        this.setRotateAngle(oss_quad_front_right, -0.2793F, 0.1047F, -0.2443F);
        this.mane_4 = new AdvancedModelBox(this, 117, 73);
        this.mane_4.setRotationPoint(0.0F, -3.0F, -3.1000F);
        this.mane_4.addBox(-0.4000F, -2.2000F, -3.6000F, 0.8000F, 3.1000F, 7.2000F, 0.0F);
        this.mane_3 = new AdvancedModelBox(this, 99, 73);
        this.mane_3.setRotationPoint(0.0F, -3.2000F, -3.2000F);
        this.mane_3.addBox(-0.6200F, -2.6000F, -3.7000F, 1.2400F, 3.5000F, 7.4000F, 0.0F);
        this.mane_2 = new AdvancedModelBox(this, 80, 73);
        this.mane_2.setRotationPoint(0.0F, -3.4000F, -3.3000F);
        this.mane_2.addBox(-0.8500F, -3.0F, -3.8000F, 1.7000F, 3.9000F, 7.6000F, 0.0F);
        this.mane_1 = new AdvancedModelBox(this, 60, 73);
        this.mane_1.setRotationPoint(0.0F, -3.6000F, -3.3000F);
        this.mane_1.addBox(-1.1000F, -3.4000F, -3.8000F, 2.2000F, 4.3000F, 7.6000F, 0.0F);
        this.fore_left_shoulder = new AdvancedModelBox(this, 184, 0);
        this.fore_left_shoulder.setRotationPoint(5.2000F, -1.0485F, 0.0F);
        this.fore_left_shoulder.addBox(-3.0F, -1.6000F, -5.5000F, 6.0F, 12.6000F, 10.0F, 0.0F);
        this.setRotateAngle(fore_left_shoulder, -0.2618F, 0.0F, 0.0F);
        this.fore_left_forearm = new AdvancedModelBox(this, 115, 34);
        this.fore_left_forearm.setRotationPoint(-0.2500F, 11.0F, 0.0F);
        this.fore_left_forearm.addBox(-2.0500F, -1.4000F, -4.4000F, 4.1000F, 10.4000F, 8.0F, 0.0F);
        this.setRotateAngle(fore_left_forearm, 0.2443F, 0.0F, 0.0F);
        this.fore_left_cannon = new AdvancedModelBox(this, 33, 56);
        this.fore_left_cannon.setRotationPoint(-0.2500F, 9.0F, 0.0F);
        this.fore_left_cannon.addBox(-1.1000F, -1.2000F, -3.2000F, 2.2000F, 10.4000F, 5.8000F, 0.0F);
        this.setRotateAngle(fore_left_cannon, -0.1745F, 0.0F, 0.0F);
        this.fore_left_foot = new AdvancedModelBox(this, 134, 73);
        this.fore_left_foot.setRotationPoint(0.0F, 9.2000F, 0.0F);
        this.fore_left_foot.addBox(-1.7000F, -0.9000F, -4.6000F, 3.4000F, 3.9000F, 6.0F, 0.0F);
        this.setRotateAngle(fore_left_foot, 0.0524F, 0.0F, 0.0F);
        this.fore_right_shoulder = new AdvancedModelBox(this, 216, 0);
        this.fore_right_shoulder.setRotationPoint(-5.2000F, -1.0485F, 0.0F);
        this.fore_right_shoulder.addBox(-3.0F, -1.6000F, -5.5000F, 6.0F, 12.6000F, 10.0F, 0.0F);
        this.setRotateAngle(fore_right_shoulder, -0.2618F, 0.0F, 0.0F);
        this.fore_right_forearm = new AdvancedModelBox(this, 140, 34);
        this.fore_right_forearm.setRotationPoint(0.2500F, 11.0F, 0.0F);
        this.fore_right_forearm.addBox(-2.0500F, -1.4000F, -4.4000F, 4.1000F, 10.4000F, 8.0F, 0.0F);
        this.setRotateAngle(fore_right_forearm, 0.2443F, 0.0F, 0.0F);
        this.fore_right_cannon = new AdvancedModelBox(this, 49, 56);
        this.fore_right_cannon.setRotationPoint(0.2500F, 9.0F, 0.0F);
        this.fore_right_cannon.addBox(-1.1000F, -1.2000F, -3.2000F, 2.2000F, 10.4000F, 5.8000F, 0.0F);
        this.setRotateAngle(fore_right_cannon, -0.1745F, 0.0F, 0.0F);
        this.fore_right_foot = new AdvancedModelBox(this, 153, 73);
        this.fore_right_foot.setRotationPoint(0.0F, 9.2000F, 0.0F);
        this.fore_right_foot.addBox(-1.7000F, -0.9000F, -4.6000F, 3.4000F, 3.9000F, 6.0F, 0.0F);
        this.setRotateAngle(fore_right_foot, 0.0524F, 0.0F, 0.0F);
        this.body_croup = new AdvancedModelBox(this, 66, 34);
        this.body_croup.setRotationPoint(0.0F, 0.0F, 9.0F);
        this.body_croup.addBox(-7.2000F, -4.0F, 0.0F, 14.4000F, 10.0F, 10.0F, 0.0F);
        this.setRotateAngle(body_croup, -0.2793F, 0.0F, 0.0F);
        this.tail_dock = new AdvancedModelBox(this, 231, 56);
        this.tail_dock.setRotationPoint(0.0F, -0.4000F, 8.8000F);
        this.tail_dock.addBox(-1.2000F, -1.0F, -0.8000F, 2.4000F, 12.4000F, 2.4000F, 0.0F);
        this.setRotateAngle(tail_dock, 0.4189F, 0.0F, 0.0F);
        this.tail_tassel = new AdvancedModelBox(this, 210, 73);
        this.tail_tassel.setRotationPoint(0.0F, 9.8000F, 0.4000F);
        this.tail_tassel.addBox(-1.7000F, -0.8000F, -0.8000F, 3.4000F, 7.8000F, 1.6000F, 0.0F);
        this.setRotateAngle(tail_tassel, 0.2443F, 0.0F, 0.0F);
        this.hind_left_thigh = new AdvancedModelBox(this, 0, 34);
        this.hind_left_thigh.setRotationPoint(5.0F, 1.1894F, 5.0F);
        this.hind_left_thigh.addBox(-3.2000F, -1.8000F, -6.5000F, 6.4000F, 11.8000F, 9.9000F, 0.0F);
        this.setRotateAngle(hind_left_thigh, -0.0349F, 0.0F, 0.0F);
        this.hind_left_gaskin = new AdvancedModelBox(this, 165, 34);
        this.hind_left_gaskin.setRotationPoint(-0.2500F, 10.0F, 0.0F);
        this.hind_left_gaskin.addBox(-2.1500F, -1.5000F, -4.2000F, 4.3000F, 9.9000F, 8.2000F, 0.0F);
        this.setRotateAngle(hind_left_gaskin, 0.7330F, 0.0F, 0.0F);
        this.hind_left_cannon = new AdvancedModelBox(this, 135, 56);
        this.hind_left_cannon.setRotationPoint(-0.2500F, 8.4000F, 0.0F);
        this.hind_left_cannon.addBox(-1.1000F, -1.2000F, -3.0F, 2.2000F, 9.8000F, 5.6000F, 0.0F);
        this.setRotateAngle(hind_left_cannon, -0.5760F, 0.0F, 0.0F);
        this.hind_left_foot = new AdvancedModelBox(this, 172, 73);
        this.hind_left_foot.setRotationPoint(0.0F, 8.6000F, 0.0F);
        this.hind_left_foot.addBox(-1.7000F, -0.9000F, -4.4000F, 3.4000F, 3.7000F, 5.8000F, 0.0F);
        this.setRotateAngle(hind_left_foot, 0.1571F, 0.0F, 0.0F);
        this.hind_right_thigh = new AdvancedModelBox(this, 33, 34);
        this.hind_right_thigh.setRotationPoint(-5.0F, 1.1894F, 5.0F);
        this.hind_right_thigh.addBox(-3.2000F, -1.8000F, -6.5000F, 6.4000F, 11.8000F, 9.9000F, 0.0F);
        this.setRotateAngle(hind_right_thigh, -0.0349F, 0.0F, 0.0F);
        this.hind_right_gaskin = new AdvancedModelBox(this, 190, 34);
        this.hind_right_gaskin.setRotationPoint(0.2500F, 10.0F, 0.0F);
        this.hind_right_gaskin.addBox(-2.1500F, -1.5000F, -4.2000F, 4.3000F, 9.9000F, 8.2000F, 0.0F);
        this.setRotateAngle(hind_right_gaskin, 0.7330F, 0.0F, 0.0F);
        this.hind_right_cannon = new AdvancedModelBox(this, 151, 56);
        this.hind_right_cannon.setRotationPoint(0.2500F, 8.4000F, 0.0F);
        this.hind_right_cannon.addBox(-1.1000F, -1.2000F, -3.0F, 2.2000F, 9.8000F, 5.6000F, 0.0F);
        this.setRotateAngle(hind_right_cannon, -0.5760F, 0.0F, 0.0F);
        this.hind_right_foot = new AdvancedModelBox(this, 191, 73);
        this.hind_right_foot.setRotationPoint(0.0F, 8.6000F, 0.0F);
        this.hind_right_foot.addBox(-1.7000F, -0.9000F, -4.4000F, 3.4000F, 3.7000F, 5.8000F, 0.0F);
        this.setRotateAngle(hind_right_foot, 0.1571F, 0.0F, 0.0F);

        this.body_barrel.addChild(this.body_withers);
        this.body_barrel.addChild(this.body_chest);
        this.body_chest.addChild(this.neck_1);
        this.neck_1.addChild(this.neck_2);
        this.neck_2.addChild(this.neck_3);
        this.neck_3.addChild(this.neck_4);
        this.neck_4.addChild(this.head_skull);
        this.head_skull.addChild(this.head_muzzle);
        this.head_skull.addChild(this.head_jaw);
        this.head_skull.addChild(this.ear_left);
        this.head_skull.addChild(this.ear_right);
        this.head_skull.addChild(this.eye_left);
        this.head_skull.addChild(this.eye_right);
        this.head_skull.addChild(this.oss_taper_left);
        this.head_skull.addChild(this.oss_taper_right);
        this.head_skull.addChild(this.oss_boss);
        this.head_skull.addChild(this.oss_palm_base_left);
        this.oss_palm_base_left.addChild(this.oss_palm_left);
        this.head_skull.addChild(this.oss_palm_base_right);
        this.oss_palm_base_right.addChild(this.oss_palm_right);
        this.head_skull.addChild(this.oss_spike_left);
        this.head_skull.addChild(this.oss_spike_right);
        this.head_skull.addChild(this.oss_quad_left);
        this.head_skull.addChild(this.oss_quad_front_left);
        this.head_skull.addChild(this.oss_quad_right);
        this.head_skull.addChild(this.oss_quad_front_right);
        this.neck_4.addChild(this.mane_4);
        this.neck_3.addChild(this.mane_3);
        this.neck_2.addChild(this.mane_2);
        this.neck_1.addChild(this.mane_1);
        this.body_chest.addChild(this.fore_left_shoulder);
        this.fore_left_shoulder.addChild(this.fore_left_forearm);
        this.fore_left_forearm.addChild(this.fore_left_cannon);
        this.fore_left_cannon.addChild(this.fore_left_foot);
        this.body_chest.addChild(this.fore_right_shoulder);
        this.fore_right_shoulder.addChild(this.fore_right_forearm);
        this.fore_right_forearm.addChild(this.fore_right_cannon);
        this.fore_right_cannon.addChild(this.fore_right_foot);
        this.body_barrel.addChild(this.body_croup);
        this.body_croup.addChild(this.tail_dock);
        this.tail_dock.addChild(this.tail_tassel);
        this.body_croup.addChild(this.hind_left_thigh);
        this.hind_left_thigh.addChild(this.hind_left_gaskin);
        this.hind_left_gaskin.addChild(this.hind_left_cannon);
        this.hind_left_cannon.addChild(this.hind_left_foot);
        this.body_croup.addChild(this.hind_right_thigh);
        this.hind_right_thigh.addChild(this.hind_right_gaskin);
        this.hind_right_gaskin.addChild(this.hind_right_cannon);
        this.hind_right_cannon.addChild(this.hind_right_foot);

        // The palmate ossicone is a base + palm CHAIN, so one setScale on the base has
        // to carry the palm with it. AdvancedModelBox.render divides the scale back out
        // before recursing into children unless this flag is set, which is why the
        // mammoth's hasLargeTusks chain needed it too.
        this.oss_palm_base_left.setShouldScaleChildren(true);
        this.oss_palm_base_right.setShouldScaleChildren(true);

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
            body_withers,
            body_chest,
            neck_1,
            neck_2,
            neck_3,
            neck_4,
            head_skull,
            head_muzzle,
            head_jaw,
            ear_left,
            ear_right,
            eye_left,
            eye_right,
            oss_taper_left,
            oss_taper_right,
            oss_boss,
            oss_palm_base_left,
            oss_palm_left,
            oss_palm_base_right,
            oss_palm_right,
            oss_spike_left,
            oss_spike_right,
            oss_quad_left,
            oss_quad_front_left,
            oss_quad_right,
            oss_quad_front_right,
            mane_4,
            mane_3,
            mane_2,
            mane_1,
            fore_left_shoulder,
            fore_left_forearm,
            fore_left_cannon,
            fore_left_foot,
            fore_right_shoulder,
            fore_right_forearm,
            fore_right_cannon,
            fore_right_foot,
            body_croup,
            tail_dock,
            tail_tassel,
            hind_left_thigh,
            hind_left_gaskin,
            hind_left_cannon,
            hind_left_foot,
            hind_right_thigh,
            hind_right_gaskin,
            hind_right_cannon,
            hind_right_foot
        );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityGiraffid giraffid = (EntityGiraffid) entityIn;
        animator.update(giraffid);

        // ATTACK_THREATEN (50t) -- the broadside display. A giraffid does not lower its head
        // to threaten; it does the opposite and stands as TALL as it can, stretching the neck
        // to full height and swaying it slowly while stiffening the forelegs. Height is the
        // whole point of the animal, so height is the display.
        animator.setAnimation(EntityGiraffid.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, neck_1, -9.0F, -11.0F, 0);
            this.rotate(animator, neck_2, -6.0F, -9.0F, 0);
            this.rotate(animator, neck_3, -4.0F, -7.0F, 0);
            this.rotate(animator, head_skull, -8.0F, -9.0F, 0);
            this.rotate(animator, fore_left_shoulder, -7.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, 5.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, neck_1, -9.0F, 11.0F, 0);
            this.rotate(animator, neck_2, -6.0F, 9.0F, 0);
            this.rotate(animator, neck_3, -4.0F, 7.0F, 0);
            this.rotate(animator, head_skull, -8.0F, 9.0F, 0);
            this.rotate(animator, fore_left_shoulder, 5.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, -7.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_GORE (14t) -- NECKING, which is what the ossicones are actually for. A
        // giraffid fights by swinging its whole neck like a sledgehammer and landing the
        // ossicones as the head comes round; bulls knock each other down with it. The
        // wind-up rears the neck back and up, the strike drives it down and across. Rolling
        // the segments (rz) as well as pitching them is what makes it read as a swing rather
        // than a nod, and it is also the only motion on the rig that uses the ossicones as a
        // weapon -- which matters most for sivatherium and bramatherium, whose whole
        // identity is the size of them.
        animator.setAnimation(EntityGiraffid.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck_1, -14.0F, 0, -22.0F);
        this.rotate(animator, neck_2, -10.0F, 0, -18.0F);
        this.rotate(animator, neck_3, -8.0F, 0, -14.0F);
        this.rotate(animator, neck_4, -6.0F, 0, -10.0F);
        this.rotate(animator, head_skull, -18.0F, 0, -12.0F);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck_1, 20.0F, 0, 30.0F);
        this.rotate(animator, neck_2, 16.0F, 0, 26.0F);
        this.rotate(animator, neck_3, 13.0F, 0, 21.0F);
        this.rotate(animator, neck_4, 10.0F, 0, 16.0F);
        this.rotate(animator, head_skull, 26.0F, 0, 18.0F);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityGiraffid giraffid, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(giraffid);
        float globalSpeed = 1.5f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        int variant = giraffid.getVariant();

        // ================= OSSICONE FAMILY (showModel on the chain ROOT) =========
        // ModelPart.render returns before recursing into children when `visible` is
        // false, so hiding a root hides its whole subtree -- the ModelAntelope
        // horn-family mechanism. Four families plus hornless:
        //   TAPERED  giraffe
        //   PALMATE  sivatherium
        //   SPIKE    okapi, samotherium, palaeotragus
        //   QUAD     bramatherium
        //   NONE     helladotherium
        boolean ossTapered = variant == 0;
        boolean ossPalmate = variant == 1;
        boolean ossSpike = variant == 2 || variant == 3 || variant == 6;
        boolean ossQuad = variant == 4;

        this.oss_taper_left.showModel = ossTapered;
        this.oss_taper_right.showModel = ossTapered;
        this.oss_boss.showModel = ossTapered;
        this.oss_palm_base_left.showModel = ossPalmate;
        this.oss_palm_base_right.showModel = ossPalmate;
        this.oss_palm_left.showModel = ossPalmate;
        this.oss_palm_right.showModel = ossPalmate;
        this.oss_spike_left.showModel = ossSpike;
        this.oss_spike_right.showModel = ossSpike;
        this.oss_quad_left.showModel = ossQuad;
        this.oss_quad_right.showModel = ossQuad;
        this.oss_quad_front_left.showModel = ossQuad;
        this.oss_quad_front_right.showModel = ossQuad;

        // Per-species ossicone SIZE. Written for every family's boxes unconditionally,
        // not just the visible one, and that is deliberate -- see the neck block below
        // for why an unwritten scale is a bug rather than a no-op.
        float oss;
        switch (variant) {
            case 0:   // giraffe
            case 1:   // sivatherium
            case 3:   // samotherium
            case 4: oss = 1.0F; break;   // bramatherium
            case 6: oss = 0.7500F; break;   // palaeotragus
            case 2: oss = 0.5000F; break;   // okapi
            case 5: oss = 1.0F; break;   // helladotherium
            default: oss = 1.0F; break;
        }
        this.oss_taper_left.setScale(oss, oss, oss);
        this.oss_taper_right.setScale(oss, oss, oss);
        this.oss_boss.setScale(oss, oss, oss);
        this.oss_palm_base_left.setScale(oss, oss, oss);
        this.oss_palm_base_right.setScale(oss, oss, oss);
        // oss_palm_left inherits its scale from its base (setShouldScaleChildren)
        // oss_palm_right inherits its scale from its base (setShouldScaleChildren)
        this.oss_spike_left.setScale(oss, oss, oss);
        this.oss_spike_right.setScale(oss, oss, oss);
        this.oss_quad_left.setScale(oss, oss, oss);
        this.oss_quad_right.setScale(oss, oss, oss);
        this.oss_quad_front_left.setScale(oss, oss, oss);
        this.oss_quad_front_right.setScale(oss, oss, oss);

        // ================= PER-SPECIES NECK LENGTH ==============================
        // This is the mechanism that lets one mesh carry a 1.5m okapi and a 3.3m giraffe.
        // Species `scale` in giraffid.json already handles absolute size; what it cannot
        // express is how long the neck is FOR an animal that height, which is the only
        // proportion that actually separates the giraffids. nf is a ratio of ratios:
        // (species neck / species withers) over the same quotient for the giraffe.
        //
        // Each segment is scaled along its OWN local Z -- its length axis -- and every child
        // of a segment has its rotation point moved with it. AdvancedModelBox.translateAndRotate
        // applies translate(rotationPoint) -> rotZ -> rotY -> rotX -> scale, so the scale acts
        // in the part's own post-rotation frame: scaling Z shortens the segment along its
        // length with NO shear, whatever angle it sits at. render() then divides the scale
        // back out before recursing into children (scaleChildren is false here), which is
        // exactly what makes moving the child pivots by hand the correct thing to do.
        //
        // Using setShouldScaleChildren on the chain root instead would look tidier and be
        // wrong: a non-uniform scale propagated into children that are themselves rotated
        // shears every segment diagonally, and the result still reads as vaguely neck-shaped.
        //
        // EVERY VALUE BELOW IS WRITTEN UNCONDITIONALLY, INCLUDING nf == 1.0 FOR THE GIRAFFE.
        // resetToDefaultPose() restores rotateAngle* and rotationPoint* and does NOT touch
        // scaleX/Y/Z, and one ModelGiraffid instance renders every giraffid in the world --
        // so a `if (nf != 1.0F)` guard would leave a giraffe wearing the okapi's neck for as
        // long as it was rendered after one.
        //
        // The XY thicken runs the other way from nf, because a short giraffid neck is a bull
        // neck rather than a thin one. The mane's pivot is thickened with it so the blade
        // stays on the surface it sits on.
        float nf;
        switch (variant) {
            case 0: nf = 1.0F; break;   // giraffe
            case 3: nf = 0.7900F; break;   // samotherium
            case 5: nf = 0.7800F; break;   // helladotherium
            case 6: nf = 0.6800F; break;   // palaeotragus
            case 2: nf = 0.6500F; break;   // okapi
            case 4: nf = 0.5800F; break;   // bramatherium
            case 1: nf = 0.5500F; break;   // sivatherium
            default: nf = 1.0F; break;
        }
        float nthick = 1.0F + (1.0F - nf) * 0.45F;
        this.neck_1.setScale(nthick, nthick, nf);
        this.neck_2.setScale(nthick, nthick, nf);
        this.neck_3.setScale(nthick, nthick, nf);
        this.neck_4.setScale(nthick, nthick, nf);
        this.neck_2.setRotationPoint(0.0F, 0.0F, -6.6000F * nf);
        this.neck_3.setRotationPoint(0.0F, 0.0F, -6.6000F * nf);
        this.neck_4.setRotationPoint(0.0F, 0.0F, -6.4000F * nf);
        this.head_skull.setRotationPoint(0.0F, 0.0F, -6.2000F * nf);
        this.mane_4.setRotationPoint(0.0F, -3.0F * nthick, -3.1000F * nf);
        this.mane_3.setRotationPoint(0.0F, -3.2000F * nthick, -3.2000F * nf);
        this.mane_2.setRotationPoint(0.0F, -3.4000F * nthick, -3.3000F * nf);
        this.mane_1.setRotationPoint(0.0F, -3.6000F * nthick, -3.3000F * nf);
        this.mane_4.setScale(1.0F, 1.0F, nf);
        this.mane_3.setScale(1.0F, 1.0F, nf);
        this.mane_2.setScale(1.0F, 1.0F, nf);
        this.mane_1.setScale(1.0F, 1.0F, nf);

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.05F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        // The neck is a 26u lever, so even a small sway at the base is a large motion at the
        // head. Amplitudes fall as they go up the chain rather than accumulating.
        walk(neck_1, 0.22f * globalSpeed, 0.030f, false, 2.8F, 0.02F, ageInTicks / 20, 2);
        walk(neck_2, 0.22f * globalSpeed, 0.022f, false, 2.4F, 0.015F, ageInTicks / 20, 2);
        walk(neck_3, 0.22f * globalSpeed, 0.016f, false, 2.0F, 0.010F, ageInTicks / 20, 2);
        this.swing(ear_left, 0.16F, 0.26F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.16F, 0.26F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail_dock, 0.11F, 0.22F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_tassel, 0.11F, 0.34F, false, 1F, 0F, ageInTicks, 1);

        // Blinking: bury the eye planes inside the skull. y/z MUST keep the
        // constructor's values or the eye jumps across the face on every blink.
        if (!giraffid.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(1.7000F, -1.4000F, -5.8000F);
            this.eye_right.setRotationPoint(-1.7000F, -1.4000F, -5.8000F);
        }

        // Head tracking, SHARED DOWN THE CHAIN. A giraffe turns to look by rotating the
        // whole neck, not by cranking the skull -- and dividing the turn four ways also keeps
        // any single joint from opening the 2.4u overlap into a visible wedge.
        if (!giraffid.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 8, neck_1);
            this.faceTarget(netHeadYaw, headPitch, 6, neck_2);
            this.faceTarget(netHeadYaw, headPitch, 5, neck_3);
            this.faceTarget(netHeadYaw, headPitch, 4, neck_4);
            this.faceTarget(netHeadYaw, headPitch, 3, head_skull);
        }

        if (giraffid.isInWater()) {
            this.setRotateAngle(head_skull, -0.1820F, 0.0F, 0.0F);
            if (!giraffid.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(giraffid.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // ---- gait: THE GIRAFFE PACES ----
        // A giraffe is one of the few large mammals that walks a PACE rather than a diagonal
        // walk: both legs on the same side swing together. It is as diagnostic as the neck
        // and it costs nothing to express, so fore_left shares hind_left's phase offset
        // (2F) and fore_right shares hind_right's (0F). Every other quadruped on this
        // codebase offsets the hind legs against the fore, which is the trot pattern -- do
        // not "fix" these to match them.
        if (giraffid.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.6f * globalDegree, true, f, limbSwingAmount);
            walk(neck_1, 0.8f * globalSpeed, 0.10f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(neck_4, 0.8f * globalSpeed, 0.08f * globalDegree, true, 1F, 0, f, limbSwingAmount);
            walk(head_skull, 0.8f * globalSpeed, 0.09f * globalDegree, true, 0, 0, f, limbSwingAmount);
            walk(fore_right_shoulder, -0.8f * globalSpeed, 1.1f * globalDegree, true, 0F, 1.1f, f, limbSwingAmount);
            walk(fore_right_forearm, -0.8f * globalSpeed, 1.1f * globalDegree, false, -1F, 1.1f, f, limbSwingAmount * 1.2f);
            walk(fore_right_cannon, -0.8f * globalSpeed, 0.7f * globalDegree, false, -2F, 0.7f, f, limbSwingAmount);
            walk(fore_left_shoulder, -0.8f * globalSpeed, 1.1f * globalDegree, true, 2F, 1.1f, f, limbSwingAmount);
            walk(fore_left_forearm, -0.8f * globalSpeed, 1.1f * globalDegree, false, 1F, 1.1f, f, limbSwingAmount * 1.2f);
            walk(fore_left_cannon, -0.8f * globalSpeed, 0.7f * globalDegree, false, 0F, 0.7f, f, limbSwingAmount);
            walk(hind_right_thigh, 0.8f * globalSpeed, 1.1f * globalDegree, false, 0F, 0, f, limbSwingAmount);
            walk(hind_right_gaskin, 0.8f * globalSpeed, 1.1f * globalDegree, true, -1F, 0, f, limbSwingAmount);
            walk(hind_right_cannon, 0.8f * globalSpeed, 0.7f * globalDegree, false, -2F, 0, f, limbSwingAmount);
            walk(hind_left_thigh, 0.8f * globalSpeed, 1.1f * globalDegree, false, 2F, 0, f, limbSwingAmount);
            walk(hind_left_gaskin, 0.8f * globalSpeed, 1.1f * globalDegree, true, 1F, 0, f, limbSwingAmount);
            walk(hind_left_cannon, 0.8f * globalSpeed, 0.7f * globalDegree, false, 0F, 0, f, limbSwingAmount);
        }

        if (giraffid.sitProgress > 0) {
            // SOLVED by grid search against real box geometry (giraffid_spec.py --pose).
            // The class this replaces used ModelBison's fold angles verbatim on legs
            // three times as long, which is the exact failure the SOP's phase 7 exists
            // to stop. Belly drops 20.50u; every leg box stays above y=0 and
            // the cannon + foot of all four limbs rest ON the floor.
            this.progressPosition(body_barrel, giraffid.sitProgress, 0.0F, 13.5000F, 0.0F, 40);
            this.progressRotation(fore_left_shoulder, giraffid.sitProgress, -1.4962F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, giraffid.sitProgress, 2.2455F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, giraffid.sitProgress, -2.3559F, 0, 0, 40);
            this.progressRotation(fore_left_foot, giraffid.sitProgress, 0.2228F, 0, 0, 40);
            this.progressRotation(fore_right_shoulder, giraffid.sitProgress, -1.4962F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, giraffid.sitProgress, 2.2455F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, giraffid.sitProgress, -2.3559F, 0, 0, 40);
            this.progressRotation(fore_right_foot, giraffid.sitProgress, 0.2228F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, giraffid.sitProgress, -1.0520F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, giraffid.sitProgress, 2.7243F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, giraffid.sitProgress, -2.7808F, 0, 0, 40);
            this.progressRotation(hind_left_foot, giraffid.sitProgress, 0.1397F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, giraffid.sitProgress, -1.0520F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, giraffid.sitProgress, 2.7243F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, giraffid.sitProgress, -2.7808F, 0, 0, 40);
            this.progressRotation(hind_right_foot, giraffid.sitProgress, 0.1397F, 0, 0, 40);
            this.progressRotation(neck_1, giraffid.sitProgress, -0.1396F, 0, 0, 40);
            this.progressRotation(head_skull, giraffid.sitProgress, 0.1745F, 0, 0, 40);
        }

        else if (giraffid.sleepProgress > 0) {
            // SOLVED by grid search against real box geometry (giraffid_spec.py --pose).
            // The class this replaces used ModelBison's fold angles verbatim on legs
            // three times as long, which is the exact failure the SOP's phase 7 exists
            // to stop. Belly drops 21.70u; every leg box stays above y=0 and
            // the cannon + foot of all four limbs rest ON the floor.
            this.progressPosition(body_barrel, giraffid.sleepProgress, 0.0F, 14.7000F, 0.0F, 40);
            this.progressRotation(fore_left_shoulder, giraffid.sleepProgress, -1.7974F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, giraffid.sleepProgress, 2.1074F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, giraffid.sleepProgress, -1.9552F, 0, 0, 40);
            this.progressRotation(fore_left_foot, giraffid.sleepProgress, 0.2566F, 0, 0, 40);
            this.progressRotation(fore_right_shoulder, giraffid.sleepProgress, -1.7974F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, giraffid.sleepProgress, 2.1074F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, giraffid.sleepProgress, -1.9552F, 0, 0, 40);
            this.progressRotation(fore_right_foot, giraffid.sleepProgress, 0.2566F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, giraffid.sleepProgress, -1.7017F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, giraffid.sleepProgress, 2.3342F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, giraffid.sleepProgress, -1.8886F, 0, 0, 40);
            this.progressRotation(hind_left_foot, giraffid.sleepProgress, 0.3399F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, giraffid.sleepProgress, -1.7017F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, giraffid.sleepProgress, 2.3342F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, giraffid.sleepProgress, -1.8886F, 0, 0, 40);
            this.progressRotation(hind_right_foot, giraffid.sleepProgress, 0.3399F, 0, 0, 40);

            // THE SLEEPING COIL, and the giraffe alone can do it. A resting
            // giraffe folds its neck right round and lays its head on its own
            // rump; it is the animal's most recognisable posture. The angles are
            // solved, not drawn: the objective lands the muzzle tip ON the croup's
            // top surface while keeping every neck segment out of the barrel.
            // Gated on nf, because at 0.78 the neck is ~6u short of reaching and
            // the head would hang in mid air -- so every other species gets a
            // plain head-forward-and-down sleep instead.
            if (nf >= 0.95F) {
                this.progressRotation(neck_1, giraffid.sleepProgress, -2.1689F, 0, 0, 40);
                this.progressRotation(neck_2, giraffid.sleepProgress, -0.5966F, 0, 0, 40);
                this.progressRotation(neck_3, giraffid.sleepProgress, -0.5089F, 0, 0, 40);
                this.progressRotation(neck_4, giraffid.sleepProgress, -0.3759F, 0, 0, 40);
                this.progressRotation(head_skull, giraffid.sleepProgress, 0.0072F, 0, 0, 40);
                this.progressRotation(head_muzzle, giraffid.sleepProgress, -1.1820F, 0, 0, 40);
            } else {
                this.progressRotation(neck_1, giraffid.sleepProgress, 0.5236F, 0, 0, 40);
                this.progressRotation(neck_2, giraffid.sleepProgress, 0.3491F, 0, 0, 40);
                this.progressRotation(neck_3, giraffid.sleepProgress, 0.2618F, 0, 0, 40);
                this.progressRotation(head_skull, giraffid.sleepProgress, 0.3491F, 0, 0, 40);
            }
        }

    }
}
