package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityToxodon;

/**
 * Full Blockbench rebuild -- 34 boxes on 256x128, replacing the 19-box / 128x128 rig that
 * was forked off ModelBison. The old one had SINGLE-SEGMENT legs and, by its own comment,
 * sit/sleep fold angles inherited from that fork and never re-derived. None of it survives.
 *
 * Sculpted from scripts/toxodon_spec.py, which carries the spec, the forward-kinematics
 * checker, the z-fight sweep, the UV shelf pack and the phase-7 pose solver. Box coords +
 * texOffs are transcribed VERBATIM from the modded_entity export (the Y-down flip is the
 * export's, never re-derived); scripts/gen_model_toxodon.py does the transcription.
 *
 * Notoungulate anatomy the numbers are expressing: high withers over a deep chest with the
 * topline sloping DOWN to a low rump (withers 27.6u vs rump 21.7u in authoring space), a
 * big head carried LOW on a short thick neck, four-segment legs on broad three-toed feet
 * with the belly at ~36% of shoulder height, small high-set ears, high orbits, a heavy
 * mandible, and the diagnostic procumbent chisel incisor battery.
 *
 * toxodon.json declares NO model flags AND both species share one mesh -- Mixotoxodon
 * differs from Toxodon only in size, which getMobSize() already applies from the JSON
 * `scale`. So there is deliberately no getVariant() switch in here: adding one would be
 * dead code. Scales are calibrated 1 block = 1 metre at the shoulder (0.870 = 1.50 m,
 * 1.014 = 1.75 m), so Toxodon matches the registered 1.7x1.5 hitbox exactly.
 */
public class ModelToxodon extends AdvancedEntityModel<EntityToxodon> {

    private final AdvancedModelBox body_barrel;
    private final AdvancedModelBox body_withers;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head_skull;
    private final AdvancedModelBox head_muzzle;
    private final AdvancedModelBox incisor_lower;
    private final AdvancedModelBox incisor_upper;
    private final AdvancedModelBox head_jaw;
    private final AdvancedModelBox head_brow;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox throat;
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

    public ModelToxodon() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_barrel = new AdvancedModelBox(this, 0, 0);
        this.body_barrel.setRotationPoint(0.0F, 4.0F, 5.0F);
        this.body_barrel.addBox(-9.0F, -6.0F, -10.0F, 18.0F, 13.5000F, 22.0F, 0.0F);
        this.body_withers = new AdvancedModelBox(this, 144, 0);
        this.body_withers.setRotationPoint(0.0F, 0.0F, -6.0F);
        this.body_withers.addBox(-7.9000F, -7.6000F, -9.0F, 15.8000F, 12.6000F, 15.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 80, 0);
        this.body_chest.setRotationPoint(0.0F, -1.0F, -10.0F);
        this.body_chest.addBox(-7.4000F, -3.2000F, -9.0F, 14.8000F, 13.4000F, 17.0F, 0.0F);
        this.setRotateAngle(body_chest, 0.1047F, 0.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 31, 62);
        this.neck.setRotationPoint(0.0F, -1.5000F, -6.0F);
        this.neck.addBox(-5.2000F, -4.4000F, -4.5000F, 10.4000F, 9.4000F, 6.1000F, 0.0F);
        this.setRotateAngle(neck, 0.1047F, 0.0F, 0.0F);
        this.head_skull = new AdvancedModelBox(this, 176, 36);
        this.head_skull.setRotationPoint(0.0F, 0.0F, -4.5000F);
        this.head_skull.addBox(-5.0F, -3.6000F, -6.5000F, 10.0F, 8.0F, 8.1000F, 0.0F);
        this.setRotateAngle(head_skull, 0.1396F, 0.0F, 0.0F);
        this.head_muzzle = new AdvancedModelBox(this, 158, 62);
        this.head_muzzle.setRotationPoint(0.0F, 0.0F, -6.5000F);
        this.head_muzzle.addBox(-4.2000F, -3.0F, -4.5000F, 8.4000F, 6.4000F, 6.1000F, 0.0F);
        this.setRotateAngle(head_muzzle, 0.1047F, 0.0F, 0.0F);
        this.incisor_lower = new AdvancedModelBox(this, 208, 79);
        this.incisor_lower.setRotationPoint(0.0F, 2.0F, -4.0F);
        this.incisor_lower.addBox(-2.8000F, -0.9000F, -3.0F, 5.6000F, 1.8000F, 3.5000F, 0.0F);
        this.incisor_upper = new AdvancedModelBox(this, 239, 79);
        this.incisor_upper.setRotationPoint(0.0F, 0.0F, -4.0F);
        this.incisor_upper.addBox(-2.4000F, -0.7000F, -2.5000F, 4.8000F, 1.4000F, 3.0F, 0.0F);
        this.head_jaw = new AdvancedModelBox(this, 134, 36);
        this.head_jaw.setRotationPoint(0.0F, 4.5000F, -3.5000F);
        this.head_jaw.addBox(-4.6000F, -2.0F, -5.5000F, 9.2000F, 5.5000F, 11.5000F, 0.0F);
        this.head_brow = new AdvancedModelBox(this, 0, 79);
        this.head_brow.setRotationPoint(0.0F, -2.9000F, -3.0F);
        this.head_brow.addBox(-3.8000F, -1.4000F, -4.0F, 7.6000F, 2.8000F, 8.0F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 0, 90);
        this.ear_left.setRotationPoint(3.2000F, -3.1000F, 0.5000F);
        this.ear_left.addBox(0.0F, -0.8000F, -1.2000F, 3.6000F, 1.4000F, 2.8000F, 0.0F);
        this.setRotateAngle(ear_left, -0.1745F, -0.3491F, -0.7854F);
        this.ear_right = new AdvancedModelBox(this, 13, 90);
        this.ear_right.setRotationPoint(-3.2000F, -3.1000F, 0.5000F);
        this.ear_right.addBox(-3.6000F, -0.8000F, -1.2000F, 3.6000F, 1.4000F, 2.8000F, 0.0F);
        this.setRotateAngle(ear_right, -0.1745F, 0.3491F, 0.7854F);
        this.eye_left = new AdvancedModelBox(this, 227, 79);
        this.eye_left.setRotationPoint(5.4000F, -1.5000F, -4.0F);
        this.eye_left.addBox(0.0F, -1.5000F, -1.5000F, 0.0F, 3.0F, 3.0F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 233, 79);
        this.eye_right.setRotationPoint(-5.4000F, -1.5000F, -4.0F);
        this.eye_right.addBox(0.0F, -1.5000F, -1.5000F, 0.0F, 3.0F, 3.0F, 0.0F);
        this.throat = new AdvancedModelBox(this, 188, 62);
        this.throat.setRotationPoint(0.0F, 3.0F, -2.0F);
        this.throat.addBox(-5.6000F, 0.0F, -4.0F, 11.2000F, 4.5000F, 7.0F, 0.0F);
        this.fore_left_shoulder = new AdvancedModelBox(this, 213, 36);
        this.fore_left_shoulder.setRotationPoint(6.0F, 2.2949F, 0.0F);
        this.fore_left_shoulder.addBox(-3.6000F, -1.8000F, -4.5000F, 7.2000F, 8.3000F, 8.0F, 0.0F);
        this.setRotateAngle(fore_left_shoulder, -0.2094F, 0.0F, 0.0F);
        this.fore_left_forearm = new AdvancedModelBox(this, 112, 62);
        this.fore_left_forearm.setRotationPoint(-0.4000F, 6.5000F, 0.0F);
        this.fore_left_forearm.addBox(-2.6000F, -2.0F, -3.2000F, 5.2000F, 7.5000F, 6.0F, 0.0F);
        this.setRotateAngle(fore_left_forearm, 0.2269F, 0.0F, 0.0F);
        this.fore_left_cannon = new AdvancedModelBox(this, 166, 79);
        this.fore_left_cannon.setRotationPoint(-0.2000F, 5.5000F, 0.0F);
        this.fore_left_cannon.addBox(-1.6000F, -1.2000F, -2.4000F, 3.2000F, 5.4000F, 4.6000F, 0.0F);
        this.setRotateAngle(fore_left_cannon, -0.1745F, 0.0F, 0.0F);
        this.fore_left_foot = new AdvancedModelBox(this, 84, 79);
        this.fore_left_foot.setRotationPoint(0.1000F, 4.2000F, 0.0F);
        this.fore_left_foot.addBox(-2.9000F, -0.8000F, -4.2000F, 5.8000F, 3.4000F, 6.2000F, 0.0F);
        this.setRotateAngle(fore_left_foot, 0.0524F, 0.0F, 0.0F);
        this.fore_right_shoulder = new AdvancedModelBox(this, 0, 62);
        this.fore_right_shoulder.setRotationPoint(-6.0F, 2.2949F, 0.0F);
        this.fore_right_shoulder.addBox(-3.6000F, -1.8000F, -4.5000F, 7.2000F, 8.3000F, 8.0F, 0.0F);
        this.setRotateAngle(fore_right_shoulder, -0.2094F, 0.0F, 0.0F);
        this.fore_right_forearm = new AdvancedModelBox(this, 135, 62);
        this.fore_right_forearm.setRotationPoint(0.4000F, 6.5000F, 0.0F);
        this.fore_right_forearm.addBox(-2.6000F, -2.0F, -3.2000F, 5.2000F, 7.5000F, 6.0F, 0.0F);
        this.setRotateAngle(fore_right_forearm, 0.2269F, 0.0F, 0.0F);
        this.fore_right_cannon = new AdvancedModelBox(this, 182, 79);
        this.fore_right_cannon.setRotationPoint(0.2000F, 5.5000F, 0.0F);
        this.fore_right_cannon.addBox(-1.6000F, -1.2000F, -2.4000F, 3.2000F, 5.4000F, 4.6000F, 0.0F);
        this.setRotateAngle(fore_right_cannon, -0.1745F, 0.0F, 0.0F);
        this.fore_right_foot = new AdvancedModelBox(this, 108, 79);
        this.fore_right_foot.setRotationPoint(-0.1000F, 4.2000F, 0.0F);
        this.fore_right_foot.addBox(-2.9000F, -0.8000F, -4.2000F, 5.8000F, 3.4000F, 6.2000F, 0.0F);
        this.setRotateAngle(fore_right_foot, 0.0524F, 0.0F, 0.0F);
        this.body_croup = new AdvancedModelBox(this, 0, 36);
        this.body_croup.setRotationPoint(0.0F, -2.0F, 7.0F);
        this.body_croup.addBox(-8.4000F, -2.0F, -6.0F, 16.8000F, 10.0F, 16.0F, 0.0F);
        this.setRotateAngle(body_croup, -0.2269F, 0.0F, 0.0F);
        this.tail_dock = new AdvancedModelBox(this, 225, 62);
        this.tail_dock.setRotationPoint(0.0F, -0.5000F, 9.0F);
        this.tail_dock.addBox(-1.4000F, -1.0F, -0.8000F, 2.8000F, 8.0F, 3.6000F, 0.0F);
        this.setRotateAngle(tail_dock, 0.4363F, 0.0F, 0.0F);
        this.tail_tip = new AdvancedModelBox(this, 198, 79);
        this.tail_tip.setRotationPoint(0.0F, 6.5000F, 1.5000F);
        this.tail_tip.addBox(-1.0F, -0.6000F, -0.9000F, 2.0F, 5.6000F, 2.8000F, 0.0F);
        this.setRotateAngle(tail_tip, 0.2618F, 0.0F, 0.0F);
        this.hind_left_thigh = new AdvancedModelBox(this, 66, 36);
        this.hind_left_thigh.setRotationPoint(5.7000F, 3.1008F, 4.0F);
        this.hind_left_thigh.addBox(-3.9000F, -2.0F, -5.0F, 7.8000F, 8.8000F, 9.2000F, 0.0F);
        this.setRotateAngle(hind_left_thigh, -0.1920F, 0.0F, 0.0F);
        this.hind_left_gaskin = new AdvancedModelBox(this, 64, 62);
        this.hind_left_gaskin.setRotationPoint(-0.5000F, 6.8000F, 0.0F);
        this.hind_left_gaskin.addBox(-2.6000F, -1.6000F, -3.4000F, 5.2000F, 8.0F, 6.4000F, 0.0F);
        this.setRotateAngle(hind_left_gaskin, 0.9774F, 0.0F, 0.0F);
        this.hind_left_cannon = new AdvancedModelBox(this, 132, 79);
        this.hind_left_cannon.setRotationPoint(0.0F, 6.4000F, 0.0F);
        this.hind_left_cannon.addBox(-1.8000F, -1.2000F, -2.4000F, 3.6000F, 5.2000F, 4.6000F, 0.0F);
        this.setRotateAngle(hind_left_cannon, -0.7330F, 0.0F, 0.0F);
        this.hind_left_foot = new AdvancedModelBox(this, 32, 79);
        this.hind_left_foot.setRotationPoint(0.3000F, 4.0F, 0.0F);
        this.hind_left_foot.addBox(-3.3000F, -0.8000F, -4.0F, 6.6000F, 3.3000F, 6.2000F, 0.0F);
        this.setRotateAngle(hind_left_foot, 0.1745F, 0.0F, 0.0F);
        this.hind_right_thigh = new AdvancedModelBox(this, 100, 36);
        this.hind_right_thigh.setRotationPoint(-5.7000F, 3.1008F, 4.0F);
        this.hind_right_thigh.addBox(-3.9000F, -2.0F, -5.0F, 7.8000F, 8.8000F, 9.2000F, 0.0F);
        this.setRotateAngle(hind_right_thigh, -0.1920F, 0.0F, 0.0F);
        this.hind_right_gaskin = new AdvancedModelBox(this, 88, 62);
        this.hind_right_gaskin.setRotationPoint(0.5000F, 6.8000F, 0.0F);
        this.hind_right_gaskin.addBox(-2.6000F, -1.6000F, -3.4000F, 5.2000F, 8.0F, 6.4000F, 0.0F);
        this.setRotateAngle(hind_right_gaskin, 0.9774F, 0.0F, 0.0F);
        this.hind_right_cannon = new AdvancedModelBox(this, 149, 79);
        this.hind_right_cannon.setRotationPoint(0.0F, 6.4000F, 0.0F);
        this.hind_right_cannon.addBox(-1.8000F, -1.2000F, -2.4000F, 3.6000F, 5.2000F, 4.6000F, 0.0F);
        this.setRotateAngle(hind_right_cannon, -0.7330F, 0.0F, 0.0F);
        this.hind_right_foot = new AdvancedModelBox(this, 58, 79);
        this.hind_right_foot.setRotationPoint(-0.3000F, 4.0F, 0.0F);
        this.hind_right_foot.addBox(-3.3000F, -0.8000F, -4.0F, 6.6000F, 3.3000F, 6.2000F, 0.0F);
        this.setRotateAngle(hind_right_foot, 0.1745F, 0.0F, 0.0F);

        this.body_barrel.addChild(this.body_withers);
        this.body_barrel.addChild(this.body_chest);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head_skull);
        this.head_skull.addChild(this.head_muzzle);
        this.head_muzzle.addChild(this.incisor_lower);
        this.head_muzzle.addChild(this.incisor_upper);
        this.head_skull.addChild(this.head_jaw);
        this.head_skull.addChild(this.head_brow);
        this.head_skull.addChild(this.ear_left);
        this.head_skull.addChild(this.ear_right);
        this.head_skull.addChild(this.eye_left);
        this.head_skull.addChild(this.eye_right);
        this.neck.addChild(this.throat);
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
            neck,
            head_skull,
            head_muzzle,
            incisor_lower,
            incisor_upper,
            head_jaw,
            head_brow,
            ear_left,
            ear_right,
            eye_left,
            eye_right,
            throat,
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
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityToxodon toxodon = (EntityToxodon) entityIn;
        animator.update(toxodon);

        // ATTACK_THREATEN (50t) -- head swung low and side to side, weight rocking between
        // the forelegs. A toxodon has no horns, so the threat display is the broad head and
        // the incisor battery, not a rack: the head drops rather than lifting.
        animator.setAnimation(EntityToxodon.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_chest, 0, 0, 6.5F);
            this.rotate(animator, neck, 12.0F, -14.0F, -8.0F);
            this.rotate(animator, head_skull, 8.0F, -10.0F, 0);
            this.rotate(animator, fore_left_shoulder, -18.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, 15.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_chest, 0, 0, -11.0F);
            this.rotate(animator, neck, 12.0F, 14.0F, 8.0F);
            this.rotate(animator, head_skull, 8.0F, 10.0F, 0);
            this.rotate(animator, fore_left_shoulder, 15.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, -18.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_GORE (14t) -- rear the head back, then drive the incisors up and forward.
        // The upward finish is the point: the lower incisors are procumbent, so the animal
        // hooks with them from below rather than stabbing downward.
        animator.setAnimation(EntityToxodon.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck, -20.0F, 0, 0);
        this.rotate(animator, head_skull, -16.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, 34.0F, 0, 0);
        this.rotate(animator, head_skull, 26.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityToxodon toxodon, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(toxodon);
        float globalSpeed = 1.5f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.05F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        walk(neck, 0.35f * globalSpeed, 0.03f, false, 2.8F, 0.04F, ageInTicks / 20, 2);
        this.swing(tail_dock, 0.10F, 0.14F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_tip, 0.10F, 0.20F, false, 1F, 0F, ageInTicks, 1);

        // Blinking: bury the eye planes inside the skull. y/z MUST keep the
        // constructor's values or the eye jumps across the face on every blink.
        if (!toxodon.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(3.3480F, -1.5000F, -4.0F);
            this.eye_right.setRotationPoint(-3.3480F, -1.5000F, -4.0F);
        }

        // Head tracking. The neck is short and heavily muscled, so it contributes less
        // than a cervid's -- most of the turn happens at the skull.
        if (!toxodon.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 4, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head_skull);
        }

        if (toxodon.isInWater()) {
            this.setRotateAngle(head_skull, -0.1820F, 0.0F, 0.0F);
            if (!toxodon.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(toxodon.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // ---- gait: a heavy, short-strided walk, so the swing amplitudes stay low ----
        if (toxodon.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.6f * globalDegree, true, f, limbSwingAmount);
            walk(neck, 0.8f * globalSpeed, 0.16f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head_skull, 0.8f * globalSpeed, 0.12f * globalDegree, true, 0, 0, f, limbSwingAmount);
            walk(fore_right_shoulder, -0.8f * globalSpeed, 1.0f * globalDegree, true, 0F, 1.0f, f, limbSwingAmount);
            walk(fore_right_forearm, -0.8f * globalSpeed, 1.0f * globalDegree, false, -1F, 1.0f, f, limbSwingAmount * 1.2f);
            walk(fore_right_cannon, -0.8f * globalSpeed, 0.7f * globalDegree, false, -2F, 0.7f, f, limbSwingAmount);
            walk(fore_left_shoulder, -0.8f * globalSpeed, 1.0f * globalDegree, true, 2F, 1.0f, f, limbSwingAmount);
            walk(fore_left_forearm, -0.8f * globalSpeed, 1.0f * globalDegree, false, 1F, 1.0f, f, limbSwingAmount * 1.2f);
            walk(fore_left_cannon, -0.8f * globalSpeed, 0.7f * globalDegree, false, 0F, 0.7f, f, limbSwingAmount);
            walk(hind_right_thigh, 0.8f * globalSpeed, 1.0f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(hind_right_gaskin, 0.8f * globalSpeed, 1.0f * globalDegree, true, 1.8F, 0, f, limbSwingAmount);
            walk(hind_right_cannon, 0.8f * globalSpeed, 0.7f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(hind_left_thigh, 0.8f * globalSpeed, 1.0f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(hind_left_gaskin, 0.8f * globalSpeed, 1.0f * globalDegree, true, -0.2F, 0, f, limbSwingAmount);
            walk(hind_left_cannon, 0.8f * globalSpeed, 0.7f * globalDegree, false, -1.2F, 0, f, limbSwingAmount);
        }

        if (toxodon.sitProgress > 0) {
            // SOLVED by grid search against real box geometry (toxodon_spec.py --pose),
            // NOT inherited from the old bison fork the way the previous rig's were:
            // belly drops 10.70u, every leg box stays above y=0 and the
            // cannon + foot of all four limbs rest ON the floor.
            this.progressPosition(body_barrel, toxodon.sitProgress, 0.0F, 14.7000F, 0.0F, 40);
            this.progressRotation(fore_left_shoulder, toxodon.sitProgress, -1.6177F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, toxodon.sitProgress, 1.2287F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, toxodon.sitProgress, -1.2755F, 0, 0, 40);
            this.progressRotation(fore_left_foot, toxodon.sitProgress, 0.0448F, 0, 0, 40);
            this.progressRotation(fore_right_shoulder, toxodon.sitProgress, -1.6177F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, toxodon.sitProgress, 1.2287F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, toxodon.sitProgress, -1.2755F, 0, 0, 40);
            this.progressRotation(fore_right_foot, toxodon.sitProgress, 0.0448F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, toxodon.sitProgress, 1.9727F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, toxodon.sitProgress, -1.5028F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, toxodon.sitProgress, -1.8136F, 0, 0, 40);
            this.progressRotation(hind_left_foot, toxodon.sitProgress, -0.0012F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, toxodon.sitProgress, 1.9727F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, toxodon.sitProgress, -1.5028F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, toxodon.sitProgress, -1.8136F, 0, 0, 40);
            this.progressRotation(hind_right_foot, toxodon.sitProgress, -0.0012F, 0, 0, 40);
            this.progressRotation(neck, toxodon.sitProgress, -0.2618F, 0, 0, 40);
            this.progressRotation(head_skull, toxodon.sitProgress, -0.1745F, 0, 0, 40);
        }

        else if (toxodon.sleepProgress > 0) {
            // SOLVED by grid search against real box geometry (toxodon_spec.py --pose),
            // NOT inherited from the old bison fork the way the previous rig's were:
            // belly drops 11.70u, every leg box stays above y=0 and the
            // cannon + foot of all four limbs rest ON the floor.
            this.progressPosition(body_barrel, toxodon.sleepProgress, 0.0F, 15.7000F, 0.0F, 40);
            this.progressRotation(fore_left_shoulder, toxodon.sleepProgress, -1.7149F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, toxodon.sleepProgress, 2.0717F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, toxodon.sleepProgress, -2.0253F, 0, 0, 40);
            this.progressRotation(fore_left_foot, toxodon.sleepProgress, 0.0504F, 0, 0, 40);
            this.progressRotation(fore_right_shoulder, toxodon.sleepProgress, -1.7149F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, toxodon.sleepProgress, 2.0717F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, toxodon.sleepProgress, -2.0253F, 0, 0, 40);
            this.progressRotation(fore_right_foot, toxodon.sleepProgress, 0.0504F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, toxodon.sleepProgress, -1.6399F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, toxodon.sleepProgress, 1.5096F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, toxodon.sleepProgress, -1.2087F, 0, 0, 40);
            this.progressRotation(hind_left_foot, toxodon.sleepProgress, -0.0060F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, toxodon.sleepProgress, -1.6399F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, toxodon.sleepProgress, 1.5096F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, toxodon.sleepProgress, -1.2087F, 0, 0, 40);
            this.progressRotation(hind_right_foot, toxodon.sleepProgress, -0.0060F, 0, 0, 40);
            this.progressRotation(neck, toxodon.sleepProgress, 0.5236F, 0, 0, 40);
            this.progressRotation(head_skull, toxodon.sleepProgress, -0.6981F, 0, 0, 40);
        }

    }
}
