package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityMacrauchenia;

/**
 * Full Blockbench rebuild -- 35 boxes on 256x128, replacing the 26-box / 128x64 rig that
 * was the LAST un-remodelled ModelBison fork in the mod. The old one still declared bison
 * horns, beard, forelock and limb wool (hidden or scaled to zero at runtime) with a
 * two-cube proboscis bolted onto the snout. None of it survives.
 *
 * Sculpted from scripts/macrauchenia_spec.py, which carries the spec, the forward-
 * kinematics checker, the z-fight sweep, the volume-burial check, the UV shelf pack and
 * the phase-7 pose solver. Box coords + texOffs are transcribed VERBATIM from the
 * modded_entity export (the Y-down flip is the export's, never re-derived);
 * scripts/gen_model_macrauchenia.py does the transcription.
 *
 * Litoptern anatomy the numbers are expressing: LONG-LEGGED and cursorial (belly at 58%
 * of withers, against the bison fork's ~45%), a long S-curved neck carried high and
 * camelid-like, a small narrow head that pops off a hard-tapering neck (4.8 -> 4.0 -> 3.2
 * against a 6.5 chest), four-segment legs on broad splayed three-toed feet, and -- the
 * diagnostic character -- the RETRACTED NASAL OPENING domed on the skull roof between the
 * orbits (head_nasal). The short tapir-like proboscis is what that retraction implies,
 * not the character itself.
 *
 * macrauchenia.json declares NO model flags AND EntityMacrauchenia has no synced flag, so
 * anything species-specific would have to route through getVariant(). Both species share
 * one mesh and differ only by JSON `scale`, which getMobSize() already applies -- so there
 * is deliberately no getVariant() switch in here: adding one would be dead code. Scales
 * are calibrated 1 block = 1 metre at the withers (1.018 = 1.80 m Macrauchenia
 * patachonica, 0.876 = 1.55 m Xenorhinotherium bahiense).
 */
public class ModelMacrauchenia extends AdvancedEntityModel<EntityMacrauchenia> {

    private final AdvancedModelBox body_barrel;
    private final AdvancedModelBox body_withers;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck_base;
    private final AdvancedModelBox neck_mid;
    private final AdvancedModelBox neck_top;
    private final AdvancedModelBox head_skull;
    private final AdvancedModelBox head_muzzle;
    private final AdvancedModelBox proboscis_1;
    private final AdvancedModelBox proboscis_2;
    private final AdvancedModelBox head_jaw;
    private final AdvancedModelBox head_nasal;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
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
    private final AdvancedModelBox tail_tip;
    private final AdvancedModelBox hind_left_thigh;
    private final AdvancedModelBox hind_left_gaskin;
    private final AdvancedModelBox hind_left_cannon;
    private final AdvancedModelBox hind_left_foot;
    private final AdvancedModelBox hind_right_thigh;
    private final AdvancedModelBox hind_right_gaskin;
    private final AdvancedModelBox hind_right_cannon;
    private final AdvancedModelBox hind_right_foot;

    private final ModelAnimator animator;

    public ModelMacrauchenia() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_barrel = new AdvancedModelBox(this, 0, 0);
        this.body_barrel.setRotationPoint(0.0F, 2.5000F, 5.0F);
        this.body_barrel.addBox(-8.0F, -5.1000F, -9.0F, 16.0F, 10.2000F, 20.0F, 0.0F);
        this.body_withers = new AdvancedModelBox(this, 72, 0);
        this.body_withers.setRotationPoint(0.0F, -0.5000F, -6.0F);
        this.body_withers.addBox(-6.9000F, -6.0F, -10.0F, 13.8000F, 9.5000F, 16.0F, 0.0F);
        this.setRotateAngle(body_withers, -0.2269F, 0.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 132, 0);
        this.body_chest.setRotationPoint(0.0F, -0.5000F, -9.0F);
        this.body_chest.addBox(-6.5000F, -3.2000F, -8.5000F, 13.0F, 10.4000F, 15.0F, 0.0F);
        this.setRotateAngle(body_chest, 0.0873F, 0.0F, 0.0F);
        this.neck_base = new AdvancedModelBox(this, 98, 31);
        this.neck_base.setRotationPoint(0.0F, -1.5000F, -5.5000F);
        this.neck_base.addBox(-4.8000F, -3.4000F, -6.0F, 9.6000F, 7.6000F, 7.6000F, 0.0F);
        this.setRotateAngle(neck_base, -0.9948F, 0.0F, 0.0F);
        this.neck_mid = new AdvancedModelBox(this, 0, 48);
        this.neck_mid.setRotationPoint(0.0F, 0.0F, -6.0F);
        this.neck_mid.addBox(-4.0F, -3.0F, -5.6000F, 8.0F, 6.6000F, 7.2000F, 0.0F);
        this.setRotateAngle(neck_mid, 0.1396F, 0.0F, 0.0F);
        this.neck_top = new AdvancedModelBox(this, 61, 48);
        this.neck_top.setRotationPoint(0.0F, 0.0F, -5.6000F);
        this.neck_top.addBox(-3.2000F, -2.6000F, -5.0F, 6.4000F, 5.6000F, 6.6000F, 0.0F);
        this.setRotateAngle(neck_top, 0.2094F, 0.0F, 0.0F);
        this.head_skull = new AdvancedModelBox(this, 31, 48);
        this.head_skull.setRotationPoint(0.0F, 0.0F, -5.0F);
        this.head_skull.addBox(-3.6000F, -3.0F, -6.0F, 7.2000F, 6.2000F, 7.6000F, 0.0F);
        this.setRotateAngle(head_skull, 0.6981F, 0.0F, 0.0F);
        this.head_muzzle = new AdvancedModelBox(this, 88, 48);
        this.head_muzzle.setRotationPoint(0.0F, 0.0F, -6.0F);
        this.head_muzzle.addBox(-2.6000F, -2.2000F, -4.6000F, 5.2000F, 4.6000F, 6.2000F, 0.0F);
        this.setRotateAngle(head_muzzle, 0.2443F, 0.0F, 0.0F);
        this.proboscis_1 = new AdvancedModelBox(this, 201, 48);
        this.proboscis_1.setRotationPoint(0.0F, 0.5000F, -3.5000F);
        this.proboscis_1.addBox(-1.9000F, -1.4000F, -3.8000F, 3.8000F, 3.0F, 4.9000F, 0.0F);
        this.setRotateAngle(proboscis_1, 0.5236F, 0.0F, 0.0F);
        this.proboscis_2 = new AdvancedModelBox(this, 32, 62);
        this.proboscis_2.setRotationPoint(0.0F, 0.0F, -2.7000F);
        this.proboscis_2.addBox(-1.4000F, -1.2000F, -3.3000F, 2.8000F, 2.6000F, 4.4000F, 0.0F);
        this.setRotateAngle(proboscis_2, 0.4189F, 0.0F, 0.0F);
        this.head_jaw = new AdvancedModelBox(this, 211, 31);
        this.head_jaw.setRotationPoint(0.0F, 2.5000F, -2.4000F);
        this.head_jaw.addBox(-2.9000F, -1.0F, -6.8000F, 5.8000F, 3.4000F, 10.5000F, 0.0F);
        this.head_nasal = new AdvancedModelBox(this, 180, 48);
        this.head_nasal.setRotationPoint(0.0F, -2.5000F, -1.9000F);
        this.head_nasal.addBox(-2.4000F, -1.6000F, -3.0F, 4.8000F, 2.2000F, 5.5000F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 47, 62);
        this.ear_left.setRotationPoint(2.4000F, -2.3000F, 0.1000F);
        this.ear_left.addBox(0.0F, -1.0F, -1.2000F, 4.0F, 1.6000F, 2.4000F, 0.0F);
        this.setRotateAngle(ear_left, -0.1396F, -0.2793F, -0.6981F);
        this.ear_right = new AdvancedModelBox(this, 60, 62);
        this.ear_right.setRotationPoint(-2.4000F, -2.3000F, 0.1000F);
        this.ear_right.addBox(-4.0F, -1.0F, -1.2000F, 4.0F, 1.6000F, 2.4000F, 0.0F);
        this.setRotateAngle(ear_right, -0.1396F, 0.2793F, 0.6981F);
        this.eye_left = new AdvancedModelBox(this, 73, 62);
        this.eye_left.setRotationPoint(4.0F, -1.0F, -2.9000F);
        this.eye_left.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 77, 62);
        this.eye_right.setRotationPoint(-4.0F, -1.0F, -2.9000F);
        this.eye_right.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.fore_left_shoulder = new AdvancedModelBox(this, 46, 31);
        this.fore_left_shoulder.setRotationPoint(5.6000F, -1.1242F, 1.0F);
        this.fore_left_shoulder.addBox(-3.0F, -1.8000F, -3.9000F, 6.0F, 9.8000F, 7.0F, 0.0F);
        this.setRotateAngle(fore_left_shoulder, -0.2443F, 0.0F, 0.0F);
        this.fore_left_forearm = new AdvancedModelBox(this, 173, 31);
        this.fore_left_forearm.setRotationPoint(-0.4500F, 8.0F, 0.0F);
        this.fore_left_forearm.addBox(-1.9500F, -2.0F, -2.9000F, 3.9000F, 9.0F, 5.4000F, 0.0F);
        this.setRotateAngle(fore_left_forearm, 0.3142F, 0.0F, 0.0F);
        this.fore_left_cannon = new AdvancedModelBox(this, 123, 48);
        this.fore_left_cannon.setRotationPoint(-0.2500F, 7.0F, 0.0F);
        this.fore_left_cannon.addBox(-1.0F, -1.4000F, -1.9000F, 2.0F, 7.4000F, 3.6000F, 0.0F);
        this.setRotateAngle(fore_left_cannon, -0.2094F, 0.0F, 0.0F);
        this.fore_left_foot = new AdvancedModelBox(this, 219, 48);
        this.fore_left_foot.setRotationPoint(-0.1000F, 6.0F, 0.0F);
        this.fore_left_foot.addBox(-1.7000F, -0.8000F, -3.2000F, 3.4000F, 3.2000F, 4.2000F, 0.0F);
        this.setRotateAngle(fore_left_foot, 0.0524F, 0.0F, 0.0F);
        this.fore_right_shoulder = new AdvancedModelBox(this, 72, 31);
        this.fore_right_shoulder.setRotationPoint(-5.6000F, -1.1242F, 1.0F);
        this.fore_right_shoulder.addBox(-3.0F, -1.8000F, -3.9000F, 6.0F, 9.8000F, 7.0F, 0.0F);
        this.setRotateAngle(fore_right_shoulder, -0.2443F, 0.0F, 0.0F);
        this.fore_right_forearm = new AdvancedModelBox(this, 192, 31);
        this.fore_right_forearm.setRotationPoint(0.4500F, 8.0F, 0.0F);
        this.fore_right_forearm.addBox(-1.9500F, -2.0F, -2.9000F, 3.9000F, 9.0F, 5.4000F, 0.0F);
        this.setRotateAngle(fore_right_forearm, 0.3142F, 0.0F, 0.0F);
        this.fore_right_cannon = new AdvancedModelBox(this, 135, 48);
        this.fore_right_cannon.setRotationPoint(0.2500F, 7.0F, 0.0F);
        this.fore_right_cannon.addBox(-1.0F, -1.4000F, -1.9000F, 2.0F, 7.4000F, 3.6000F, 0.0F);
        this.setRotateAngle(fore_right_cannon, -0.2094F, 0.0F, 0.0F);
        this.fore_right_foot = new AdvancedModelBox(this, 235, 48);
        this.fore_right_foot.setRotationPoint(0.1000F, 6.0F, 0.0F);
        this.fore_right_foot.addBox(-1.7000F, -0.8000F, -3.2000F, 3.4000F, 3.2000F, 4.2000F, 0.0F);
        this.setRotateAngle(fore_right_foot, 0.0524F, 0.0F, 0.0F);
        this.body_croup = new AdvancedModelBox(this, 0, 31);
        this.body_croup.setRotationPoint(0.0F, -1.0F, 8.0F);
        this.body_croup.addBox(-7.4000F, -3.7000F, 0.0F, 14.8000F, 8.6000F, 8.0F, 0.0F);
        this.setRotateAngle(body_croup, -0.1571F, 0.0F, 0.0F);
        this.tail_dock = new AdvancedModelBox(this, 111, 48);
        this.tail_dock.setRotationPoint(0.0F, -2.1000F, 6.8000F);
        this.tail_dock.addBox(-1.2000F, -0.9000F, -0.8000F, 2.4000F, 7.2000F, 3.4000F, 0.0F);
        this.setRotateAngle(tail_dock, 0.4887F, 0.0F, 0.0F);
        this.tail_tip = new AdvancedModelBox(this, 171, 48);
        this.tail_tip.setRotationPoint(0.0F, 5.7000F, 1.3000F);
        this.tail_tip.addBox(-0.8500F, -0.6000F, -0.8000F, 1.7000F, 6.0F, 2.6000F, 0.0F);
        this.setRotateAngle(tail_tip, 0.2793F, 0.0F, 0.0F);
        this.hind_left_thigh = new AdvancedModelBox(this, 188, 0);
        this.hind_left_thigh.setRotationPoint(5.3000F, 0.0535F, 2.5000F);
        this.hind_left_thigh.addBox(-3.3000F, -2.0F, -4.4000F, 6.6000F, 10.5000F, 8.1000F, 0.0F);
        this.setRotateAngle(hind_left_thigh, -0.2269F, 0.0F, 0.0F);
        this.hind_left_gaskin = new AdvancedModelBox(this, 133, 31);
        this.hind_left_gaskin.setRotationPoint(-0.5000F, 8.5000F, 0.0F);
        this.hind_left_gaskin.addBox(-2.1000F, -1.8000F, -3.0F, 4.2000F, 9.3000F, 5.6000F, 0.0F);
        this.setRotateAngle(hind_left_gaskin, 0.9076F, 0.0F, 0.0F);
        this.hind_left_cannon = new AdvancedModelBox(this, 147, 48);
        this.hind_left_cannon.setRotationPoint(-0.2000F, 7.5000F, 0.0F);
        this.hind_left_cannon.addBox(-1.1000F, -1.4000F, -1.9000F, 2.2000F, 6.9000F, 3.6000F, 0.0F);
        this.setRotateAngle(hind_left_cannon, -0.7330F, 0.0F, 0.0F);
        this.hind_left_foot = new AdvancedModelBox(this, 0, 62);
        this.hind_left_foot.setRotationPoint(-0.1000F, 5.5000F, 0.0F);
        this.hind_left_foot.addBox(-1.8000F, -0.8000F, -3.4000F, 3.6000F, 3.1000F, 4.4000F, 0.0F);
        this.setRotateAngle(hind_left_foot, 0.2094F, 0.0F, 0.0F);
        this.hind_right_thigh = new AdvancedModelBox(this, 218, 0);
        this.hind_right_thigh.setRotationPoint(-5.3000F, 0.0535F, 2.5000F);
        this.hind_right_thigh.addBox(-3.3000F, -2.0F, -4.4000F, 6.6000F, 10.5000F, 8.1000F, 0.0F);
        this.setRotateAngle(hind_right_thigh, -0.2269F, 0.0F, 0.0F);
        this.hind_right_gaskin = new AdvancedModelBox(this, 153, 31);
        this.hind_right_gaskin.setRotationPoint(0.5000F, 8.5000F, 0.0F);
        this.hind_right_gaskin.addBox(-2.1000F, -1.8000F, -3.0F, 4.2000F, 9.3000F, 5.6000F, 0.0F);
        this.setRotateAngle(hind_right_gaskin, 0.9076F, 0.0F, 0.0F);
        this.hind_right_cannon = new AdvancedModelBox(this, 159, 48);
        this.hind_right_cannon.setRotationPoint(0.2000F, 7.5000F, 0.0F);
        this.hind_right_cannon.addBox(-1.1000F, -1.4000F, -1.9000F, 2.2000F, 6.9000F, 3.6000F, 0.0F);
        this.setRotateAngle(hind_right_cannon, -0.7330F, 0.0F, 0.0F);
        this.hind_right_foot = new AdvancedModelBox(this, 16, 62);
        this.hind_right_foot.setRotationPoint(0.1000F, 5.5000F, 0.0F);
        this.hind_right_foot.addBox(-1.8000F, -0.8000F, -3.4000F, 3.6000F, 3.1000F, 4.4000F, 0.0F);
        this.setRotateAngle(hind_right_foot, 0.2094F, 0.0F, 0.0F);

        this.body_barrel.addChild(this.body_withers);
        this.body_barrel.addChild(this.body_chest);
        this.body_chest.addChild(this.neck_base);
        this.neck_base.addChild(this.neck_mid);
        this.neck_mid.addChild(this.neck_top);
        this.neck_top.addChild(this.head_skull);
        this.head_skull.addChild(this.head_muzzle);
        this.head_muzzle.addChild(this.proboscis_1);
        this.proboscis_1.addChild(this.proboscis_2);
        this.head_skull.addChild(this.head_jaw);
        this.head_skull.addChild(this.head_nasal);
        this.head_skull.addChild(this.ear_left);
        this.head_skull.addChild(this.ear_right);
        this.head_skull.addChild(this.eye_left);
        this.head_skull.addChild(this.eye_right);
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
        this.tail_dock.addChild(this.tail_tip);
        this.body_croup.addChild(this.hind_left_thigh);
        this.hind_left_thigh.addChild(this.hind_left_gaskin);
        this.hind_left_gaskin.addChild(this.hind_left_cannon);
        this.hind_left_cannon.addChild(this.hind_left_foot);
        this.body_croup.addChild(this.hind_right_thigh);
        this.hind_right_thigh.addChild(this.hind_right_gaskin);
        this.hind_right_gaskin.addChild(this.hind_right_cannon);
        this.hind_right_cannon.addChild(this.hind_right_foot);

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
            neck_base,
            neck_mid,
            neck_top,
            head_skull,
            head_muzzle,
            proboscis_1,
            proboscis_2,
            head_jaw,
            head_nasal,
            ear_left,
            ear_right,
            eye_left,
            eye_right,
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
            tail_tip,
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

    private void animate(IAnimatedEntity entityIn) {
        EntityMacrauchenia macrauchenia = (EntityMacrauchenia) entityIn;
        animator.update(macrauchenia);

        // ATTACK_THREATEN (50t) -- a camelid display, not a bovid one. Macrauchenia has no
        // horns and no incisor battery, so the threat is made of HEIGHT: the neck stacks
        // up and the head swings side to side above the aggressor, weight rocking between
        // the forelegs. The neck lifts rather than dropping, which is the opposite of the
        // toxodon's low broad-headed display and is the whole point of the silhouette.
        animator.setAnimation(EntityMacrauchenia.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_chest, 0, 0, 5.0F);
            this.rotate(animator, neck_base, -10.0F, -10.0F, -5.0F);
            this.rotate(animator, neck_mid, -8.0F, -12.0F, 0);
            this.rotate(animator, neck_top, -6.0F, -12.0F, 0);
            this.rotate(animator, head_skull, 6.0F, -14.0F, 0);
            this.rotate(animator, fore_left_shoulder, -14.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, 12.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_chest, 0, 0, -9.0F);
            this.rotate(animator, neck_base, -10.0F, 10.0F, 5.0F);
            this.rotate(animator, neck_mid, -8.0F, 12.0F, 0);
            this.rotate(animator, neck_top, -6.0F, 12.0F, 0);
            this.rotate(animator, head_skull, 6.0F, 14.0F, 0);
            this.rotate(animator, fore_left_shoulder, 12.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, -14.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_GORE (14t) -- misnamed by the shared Animation field, and deliberately
        // not a gore. Nothing on this skull can gore: no horn, no tusk, no procumbent
        // incisor. A litoptern strikes the way a camelid or an equid does, so this rears
        // the forehand and drives BOTH forefeet down and forward. EntityMacrauchenia
        // follows the hit with HoglinBase.hurtAndThrowTarget, and a descending forefoot
        // strike is the motion that actually explains a target being thrown.
        animator.setAnimation(EntityMacrauchenia.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, body_chest, -26.0F, 0, 0);
        this.rotate(animator, neck_base, 14.0F, 0, 0);
        this.rotate(animator, head_skull, 10.0F, 0, 0);
        this.rotate(animator, fore_left_shoulder, -60.0F, 0, 0);
        this.rotate(animator, fore_right_shoulder, -54.0F, 0, 0);
        this.rotate(animator, fore_left_forearm, -50.0F, 0, 0);
        this.rotate(animator, fore_right_forearm, -44.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, body_chest, 12.0F, 0, 0);
        this.rotate(animator, neck_base, -16.0F, 0, 0);
        this.rotate(animator, head_skull, -12.0F, 0, 0);
        this.rotate(animator, fore_left_shoulder, 34.0F, 0, 0);
        this.rotate(animator, fore_right_shoulder, 30.0F, 0, 0);
        this.rotate(animator, fore_left_forearm, 20.0F, 0, 0);
        this.rotate(animator, fore_right_forearm, 18.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityMacrauchenia macrauchenia, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(macrauchenia);
        float globalSpeed = 1.5f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.05F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        // A long neck idles visibly. Amplitude decreases up the chain so the head rides
        // the sway instead of leading it -- the reverse reads as a bobbing puppet.
        walk(neck_base, 0.32f * globalSpeed, 0.05f, false, 2.8F, 0.04F, ageInTicks / 20, 2);
        walk(neck_mid, 0.32f * globalSpeed, 0.035f, false, 2.4F, 0.03F, ageInTicks / 20, 2);
        walk(neck_top, 0.32f * globalSpeed, 0.025f, false, 2.0F, 0.02F, ageInTicks / 20, 2);
        // The proboscis is soft tissue: it swings faster and wider than anything bony.
        this.swing(proboscis_1, 0.22F, 0.16F, false, 0F, 0F, ageInTicks, 1);
        this.swing(proboscis_2, 0.22F, 0.26F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail_dock, 0.10F, 0.14F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_tip, 0.10F, 0.20F, false, 1F, 0F, ageInTicks, 1);
        this.swing(ear_left, 0.16F, 0.20F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.16F, 0.20F, false, 1F, 0F, ageInTicks, 1);

        // Blinking: bury the eye planes inside the skull. y/z MUST keep the
        // constructor's values or the eye jumps across the face on every blink.
        if (!macrauchenia.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(2.2000F, -1.0F, -2.9000F);
            this.eye_right.setRotationPoint(-2.2000F, -1.0F, -2.9000F);
        }

        // Head tracking. A long flexible neck turns along its whole length, so the
        // yaw is SHARED down the chain rather than dumped into the skull -- the opposite
        // of the short-necked toxodon. Larger divisors = smaller share.
        if (!macrauchenia.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 6, neck_base);
            this.faceTarget(netHeadYaw, headPitch, 5, neck_mid);
            this.faceTarget(netHeadYaw, headPitch, 4, neck_top);
            this.faceTarget(netHeadYaw, headPitch, 3, head_skull);
        }

        if (macrauchenia.isInWater()) {
            this.setRotateAngle(head_skull, -0.1820F, 0.0F, 0.0F);
            if (!macrauchenia.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(macrauchenia.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // ---- gait: long-legged and long-strided, so the swing amplitudes run HIGH ----
        if (macrauchenia.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.6f * globalDegree, true, f, limbSwingAmount);
            walk(neck_base, 0.8f * globalSpeed, 0.14f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(neck_mid, 0.8f * globalSpeed, 0.10f * globalDegree, false, 0.4F, 0, f, limbSwingAmount);
            walk(neck_top, 0.8f * globalSpeed, 0.08f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(head_skull, 0.8f * globalSpeed, 0.12f * globalDegree, true, 0, 0, f, limbSwingAmount);
            walk(fore_right_shoulder, -0.8f * globalSpeed, 1.4f * globalDegree, true, 0F, 1.4f, f, limbSwingAmount);
            walk(fore_right_forearm, -0.8f * globalSpeed, 1.4f * globalDegree, false, -1F, 1.4f, f, limbSwingAmount * 1.2f);
            walk(fore_right_cannon, -0.8f * globalSpeed, 0.9f * globalDegree, false, -2F, 0.9f, f, limbSwingAmount);
            walk(fore_left_shoulder, -0.8f * globalSpeed, 1.4f * globalDegree, true, 2F, 1.4f, f, limbSwingAmount);
            walk(fore_left_forearm, -0.8f * globalSpeed, 1.4f * globalDegree, false, 1F, 1.4f, f, limbSwingAmount * 1.2f);
            walk(fore_left_cannon, -0.8f * globalSpeed, 0.9f * globalDegree, false, 0F, 0.9f, f, limbSwingAmount);
            walk(hind_right_thigh, 0.8f * globalSpeed, 1.4f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(hind_right_gaskin, 0.8f * globalSpeed, 1.4f * globalDegree, true, 1.8F, 0, f, limbSwingAmount);
            walk(hind_right_cannon, 0.8f * globalSpeed, 0.9f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(hind_left_thigh, 0.8f * globalSpeed, 1.4f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(hind_left_gaskin, 0.8f * globalSpeed, 1.4f * globalDegree, true, -0.2F, 0, f, limbSwingAmount);
            walk(hind_left_cannon, 0.8f * globalSpeed, 0.9f * globalDegree, false, -1.2F, 0, f, limbSwingAmount);
        }

        if (macrauchenia.sitProgress > 0) {
            // SOLVED by grid search against real box geometry (macrauchenia_spec.py --pose),
            // NOT inherited from ModelBison the way the previous rig's were -- those
            // belong to an animal whose legs are 6u shorter than these.
            // Belly drops 14.20u; every leg box stays above y=0 and the
            // cannon + foot of all four limbs rest ON the floor.
            this.progressPosition(body_barrel, macrauchenia.sitProgress, 0.0F, 16.7000F, 0.0F, 40);
            this.progressRotation(fore_left_shoulder, macrauchenia.sitProgress, -1.4697F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, macrauchenia.sitProgress, 0.7669F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, macrauchenia.sitProgress, -0.9436F, 0, 0, 40);
            this.progressRotation(fore_left_foot, macrauchenia.sitProgress, 0.1824F, 0, 0, 40);
            this.progressRotation(fore_right_shoulder, macrauchenia.sitProgress, -1.4697F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, macrauchenia.sitProgress, 0.7669F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, macrauchenia.sitProgress, -0.9436F, 0, 0, 40);
            this.progressRotation(fore_right_foot, macrauchenia.sitProgress, 0.1824F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, macrauchenia.sitProgress, -1.3767F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, macrauchenia.sitProgress, 0.8495F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, macrauchenia.sitProgress, -0.8840F, 0, 0, 40);
            this.progressRotation(hind_left_foot, macrauchenia.sitProgress, 0.1914F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, macrauchenia.sitProgress, -1.3767F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, macrauchenia.sitProgress, 0.8495F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, macrauchenia.sitProgress, -0.8840F, 0, 0, 40);
            this.progressRotation(hind_right_foot, macrauchenia.sitProgress, 0.1914F, 0, 0, 40);
            this.progressRotation(neck_base, macrauchenia.sitProgress, -0.1745F, 0, 0, 40);
            this.progressRotation(head_skull, macrauchenia.sitProgress, -0.1222F, 0, 0, 40);
        }

        else if (macrauchenia.sleepProgress > 0) {
            // SOLVED by grid search against real box geometry (macrauchenia_spec.py --pose),
            // NOT inherited from ModelBison the way the previous rig's were -- those
            // belong to an animal whose legs are 6u shorter than these.
            // Belly drops 15.40u; every leg box stays above y=0 and the
            // cannon + foot of all four limbs rest ON the floor.
            this.progressPosition(body_barrel, macrauchenia.sleepProgress, 0.0F, 17.9000F, 0.0F, 40);
            this.progressRotation(fore_left_shoulder, macrauchenia.sleepProgress, -1.5305F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, macrauchenia.sleepProgress, 2.3022F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, macrauchenia.sleepProgress, -2.3505F, 0, 0, 40);
            this.progressRotation(fore_left_foot, macrauchenia.sleepProgress, 0.1142F, 0, 0, 40);
            this.progressRotation(fore_right_shoulder, macrauchenia.sleepProgress, -1.5305F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, macrauchenia.sleepProgress, 2.3022F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, macrauchenia.sleepProgress, -2.3505F, 0, 0, 40);
            this.progressRotation(fore_right_foot, macrauchenia.sleepProgress, 0.1142F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, macrauchenia.sleepProgress, -1.5693F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, macrauchenia.sleepProgress, 2.3202F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, macrauchenia.sleepProgress, -2.1601F, 0, 0, 40);
            this.progressRotation(hind_left_foot, macrauchenia.sleepProgress, 0.2016F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, macrauchenia.sleepProgress, -1.5693F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, macrauchenia.sleepProgress, 2.3202F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, macrauchenia.sleepProgress, -2.1601F, 0, 0, 40);
            this.progressRotation(hind_right_foot, macrauchenia.sleepProgress, 0.2016F, 0, 0, 40);
            this.progressRotation(neck_base, macrauchenia.sleepProgress, 0.4363F, 0, 0, 40);
            this.progressRotation(neck_mid, macrauchenia.sleepProgress, 0.5236F, 0, 0, 40);
            this.progressRotation(neck_top, macrauchenia.sleepProgress, 0.6109F, 0, 0, 40);
            this.progressRotation(head_skull, macrauchenia.sleepProgress, -0.9599F, 0, 0, 40);
        }

    }
}
