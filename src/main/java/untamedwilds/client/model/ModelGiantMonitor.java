package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.reptile.EntityGiantMonitor;

/**
 * Giant monitor -- Megalania + Komodo dragon (Phase 4.3, 2026-10-10): 27 boxes on 256x128, authored from scratch in
 * scripts/giant_monitor_spec.py, ported VERBATIM by scripts/gen_model_giant_monitor.py, proved by verify_port.py.
 *
 * A deep, pot-bellied varanid carried low on SPRAWLING legs: each upper limb is laid out sideways and tilted down
 * (a Z rotation), the lower limb drops straight to the ground, the foot points forward with the toes turned out.
 * Because the legs sprawl, the walk swings them about the VERTICAL axis (rotateAngleY) in diagonal pairs -- left fore
 * with right hind -- while the body, neck and tail throw the lateral S-curve every lizard walks with; the tail's wave
 * lags further down each segment.
 * The forked tongue is hidden except during IDLE_TONGUE.
 */
public class ModelGiantMonitor extends AdvancedEntityModel<EntityGiantMonitor> {

    private final AdvancedModelBox body_belly;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox snout;
    private final AdvancedModelBox tongue;
    private final AdvancedModelBox jaw;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox arm_left_upper;
    private final AdvancedModelBox arm_left_lower;
    private final AdvancedModelBox arm_left_foot;
    private final AdvancedModelBox arm_right_upper;
    private final AdvancedModelBox arm_right_lower;
    private final AdvancedModelBox arm_right_foot;
    private final AdvancedModelBox body_hips;
    private final AdvancedModelBox tail_1;
    private final AdvancedModelBox tail_2;
    private final AdvancedModelBox tail_3;
    private final AdvancedModelBox tail_4;
    private final AdvancedModelBox tail_5;
    private final AdvancedModelBox leg_left_upper;
    private final AdvancedModelBox leg_left_lower;
    private final AdvancedModelBox leg_left_foot;
    private final AdvancedModelBox leg_right_upper;
    private final AdvancedModelBox leg_right_lower;
    private final AdvancedModelBox leg_right_foot;

    private final ModelAnimator animator;

    public ModelGiantMonitor() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_belly = new AdvancedModelBox(this, 0, 0);
        this.body_belly.setRotationPoint(0.0F, 17.3801F, 2.0F);
        this.body_belly.addBox(-5.2000F, -3.8000F, -4.0F, 10.4000F, 7.6000F, 12.5000F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 46, 0);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -3.0F);
        this.body_chest.addBox(-4.6000F, -3.2000F, -8.5000F, 9.2000F, 6.6000F, 9.5000F, 0.0F);
        this.setRotateAngle(body_chest, -0.0349F, 0.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 149, 0);
        this.neck.setRotationPoint(0.0F, -0.6000F, -8.0F);
        this.neck.addBox(-2.9000F, -2.0F, -7.0F, 5.8000F, 4.6000F, 8.6000F, 0.0F);
        this.setRotateAngle(neck, -0.1047F, 0.0F, 0.0F);
        this.head = new AdvancedModelBox(this, 28, 21);
        this.head.setRotationPoint(0.0F, 0.0F, -7.0F);
        this.head.addBox(-2.7000F, -1.6000F, -6.5000F, 5.4000F, 3.2000F, 7.7000F, 0.0F);
        this.setRotateAngle(head, 0.1047F, 0.0F, 0.0F);
        this.snout = new AdvancedModelBox(this, 98, 21);
        this.snout.setRotationPoint(0.0F, 0.0F, -6.5000F);
        this.snout.addBox(-1.9000F, -1.3000F, -5.0F, 3.8000F, 2.5000F, 6.0F, 0.0F);
        this.setRotateAngle(snout, 0.0698F, 0.0F, 0.0F);
        this.tongue = new AdvancedModelBox(this, 48, 33);
        this.tongue.setRotationPoint(0.0F, 0.6000F, -4.7000F);
        this.tongue.addBox(-0.4000F, -0.1000F, -3.0F, 0.8000F, 0.3000F, 3.2000F, 0.0F);
        this.jaw = new AdvancedModelBox(this, 205, 0);
        this.jaw.setRotationPoint(0.0F, 1.2000F, -1.0F);
        this.jaw.addBox(-2.3000F, -0.4000F, -9.6000F, 4.6000F, 1.5000F, 10.0F, 0.0F);
        this.eye_left = new AdvancedModelBox(this, 56, 33);
        this.eye_left.setRotationPoint(3.0500F, -0.6000F, -3.0F);
        this.eye_left.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 60, 33);
        this.eye_right.setRotationPoint(-3.0500F, -0.6000F, -3.0F);
        this.eye_right.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.arm_left_upper = new AdvancedModelBox(this, 232, 21);
        this.arm_left_upper.setRotationPoint(4.0F, 0.8000F, -4.5000F);
        this.arm_left_upper.addBox(-0.8000F, -1.3000F, -1.3000F, 5.4000F, 2.6000F, 2.6000F, 0.0F);
        this.setRotateAngle(arm_left_upper, 0.0F, 0.0F, 0.2443F);
        this.arm_left_lower = new AdvancedModelBox(this, 140, 21);
        this.arm_left_lower.setRotationPoint(4.0F, 0.0F, 0.0F);
        this.arm_left_lower.addBox(-1.3000F, -0.8000F, -1.1000F, 2.6000F, 5.2601F, 2.2000F, 0.0F);
        this.setRotateAngle(arm_left_lower, 0.0F, 0.0F, -0.2443F);
        this.arm_left_foot = new AdvancedModelBox(this, 0, 33);
        this.arm_left_foot.setRotationPoint(0.0F, 4.4601F, 0.0F);
        this.arm_left_foot.addBox(-1.6000F, -0.5000F, -3.8000F, 3.2000F, 1.0F, 4.8000F, 0.0F);
        this.setRotateAngle(arm_left_foot, 0.0F, -0.3840F, 0.0F);
        this.arm_right_upper = new AdvancedModelBox(this, 16, 33);
        this.arm_right_upper.setRotationPoint(-4.0F, 0.8000F, -4.5000F);
        this.arm_right_upper.addBox(-4.6000F, -1.3000F, -1.3000F, 5.4000F, 2.6000F, 2.6000F, 0.0F);
        this.setRotateAngle(arm_right_upper, 0.0F, 0.0F, -0.2443F);
        this.arm_right_lower = new AdvancedModelBox(this, 150, 21);
        this.arm_right_lower.setRotationPoint(-4.0F, 0.0F, 0.0F);
        this.arm_right_lower.addBox(-1.3000F, -0.8000F, -1.1000F, 2.6000F, 5.2601F, 2.2000F, 0.0F);
        this.setRotateAngle(arm_right_lower, 0.0F, 0.0F, 0.2443F);
        this.arm_right_foot = new AdvancedModelBox(this, 32, 33);
        this.arm_right_foot.setRotationPoint(0.0F, 4.4601F, 0.0F);
        this.arm_right_foot.addBox(-1.6000F, -0.5000F, -3.8000F, 3.2000F, 1.0F, 4.8000F, 0.0F);
        this.setRotateAngle(arm_right_foot, 0.0F, 0.3840F, 0.0F);
        this.body_hips = new AdvancedModelBox(this, 0, 21);
        this.body_hips.setRotationPoint(0.0F, 0.0F, 8.0F);
        this.body_hips.addBox(-4.2000F, -3.3000F, -1.0F, 8.4000F, 6.5000F, 5.5000F, 0.0F);
        this.setRotateAngle(body_hips, 0.0349F, 0.0F, 0.0F);
        this.tail_1 = new AdvancedModelBox(this, 84, 0);
        this.tail_1.setRotationPoint(0.0F, -0.4000F, 4.0F);
        this.tail_1.addBox(-3.5000F, -2.6000F, -1.2000F, 7.0F, 5.4000F, 10.2000F, 0.0F);
        this.setRotateAngle(tail_1, -0.1611F, 0.0F, 0.0F);
        this.tail_2 = new AdvancedModelBox(this, 119, 0);
        this.tail_2.setRotationPoint(0.0F, 0.0F, 9.0F);
        this.tail_2.addBox(-2.7000F, -2.0F, -1.0F, 5.4000F, 4.2000F, 9.5000F, 0.0F);
        this.setRotateAngle(tail_2, -0.0689F, 0.0F, 0.0F);
        this.tail_3 = new AdvancedModelBox(this, 178, 0);
        this.tail_3.setRotationPoint(0.0F, 0.0F, 8.5000F);
        this.tail_3.addBox(-2.0F, -1.5000F, -1.0F, 4.0F, 3.1000F, 9.5000F, 0.0F);
        this.setRotateAngle(tail_3, -0.0344F, 0.0F, 0.0F);
        this.tail_4 = new AdvancedModelBox(this, 55, 21);
        this.tail_4.setRotationPoint(0.0F, 0.0F, 8.5000F);
        this.tail_4.addBox(-1.3000F, -1.0F, -0.8000F, 2.6000F, 2.1000F, 8.8000F, 0.0F);
        this.setRotateAngle(tail_4, 0.0689F, 0.0F, 0.0F);
        this.tail_5 = new AdvancedModelBox(this, 78, 21);
        this.tail_5.setRotationPoint(0.0F, 0.0F, 8.0F);
        this.tail_5.addBox(-0.7000F, -0.6000F, -0.8000F, 1.4000F, 1.2000F, 8.3000F, 0.0F);
        this.setRotateAngle(tail_5, 0.0918F, 0.0F, 0.0F);
        this.leg_left_upper = new AdvancedModelBox(this, 196, 21);
        this.leg_left_upper.setRotationPoint(3.6000F, 0.8000F, 1.5000F);
        this.leg_left_upper.addBox(-0.8000F, -1.5000F, -1.5000F, 5.8000F, 3.0F, 3.0F, 0.0F);
        this.setRotateAngle(leg_left_upper, 0.0F, 0.0F, 0.2793F);
        this.leg_left_lower = new AdvancedModelBox(this, 118, 21);
        this.leg_left_lower.setRotationPoint(4.4000F, 0.0F, 0.0F);
        this.leg_left_lower.addBox(-1.5000F, -0.8000F, -1.2500F, 3.0F, 4.8000F, 2.5000F, 0.0F);
        this.setRotateAngle(leg_left_lower, 0.0F, 0.0F, -0.2793F);
        this.leg_left_foot = new AdvancedModelBox(this, 160, 21);
        this.leg_left_foot.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.leg_left_foot.addBox(-1.7000F, -0.5000F, -4.4000F, 3.4000F, 1.0F, 5.6000F, 0.0F);
        this.setRotateAngle(leg_left_foot, 0.0F, -0.4887F, 0.0F);
        this.leg_right_upper = new AdvancedModelBox(this, 214, 21);
        this.leg_right_upper.setRotationPoint(-3.6000F, 0.8000F, 1.5000F);
        this.leg_right_upper.addBox(-5.0F, -1.5000F, -1.5000F, 5.8000F, 3.0F, 3.0F, 0.0F);
        this.setRotateAngle(leg_right_upper, 0.0F, 0.0F, -0.2793F);
        this.leg_right_lower = new AdvancedModelBox(this, 129, 21);
        this.leg_right_lower.setRotationPoint(-4.4000F, 0.0F, 0.0F);
        this.leg_right_lower.addBox(-1.5000F, -0.8000F, -1.2500F, 3.0F, 4.8000F, 2.5000F, 0.0F);
        this.setRotateAngle(leg_right_lower, 0.0F, 0.0F, 0.2793F);
        this.leg_right_foot = new AdvancedModelBox(this, 178, 21);
        this.leg_right_foot.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.leg_right_foot.addBox(-1.7000F, -0.5000F, -4.4000F, 3.4000F, 1.0F, 5.6000F, 0.0F);
        this.setRotateAngle(leg_right_foot, 0.0F, 0.4887F, 0.0F);

        this.body_belly.addChild(this.body_chest);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.snout);
        this.snout.addChild(this.tongue);
        this.head.addChild(this.jaw);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.eye_right);
        this.body_chest.addChild(this.arm_left_upper);
        this.arm_left_upper.addChild(this.arm_left_lower);
        this.arm_left_lower.addChild(this.arm_left_foot);
        this.body_chest.addChild(this.arm_right_upper);
        this.arm_right_upper.addChild(this.arm_right_lower);
        this.arm_right_lower.addChild(this.arm_right_foot);
        this.body_belly.addChild(this.body_hips);
        this.body_hips.addChild(this.tail_1);
        this.tail_1.addChild(this.tail_2);
        this.tail_2.addChild(this.tail_3);
        this.tail_3.addChild(this.tail_4);
        this.tail_4.addChild(this.tail_5);
        this.body_hips.addChild(this.leg_left_upper);
        this.leg_left_upper.addChild(this.leg_left_lower);
        this.leg_left_lower.addChild(this.leg_left_foot);
        this.body_hips.addChild(this.leg_right_upper);
        this.leg_right_upper.addChild(this.leg_right_lower);
        this.leg_right_lower.addChild(this.leg_right_foot);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_belly);
    }

    // Built once: getAllParts() is called every frame, and a fresh ImmutableList per animal per frame
    // was pure garbage (2.8.0, same as ModelBison).
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
                body_belly,
                body_chest,
                neck,
                head,
                snout,
                tongue,
                jaw,
                eye_left,
                eye_right,
                arm_left_upper,
                arm_left_lower,
                arm_left_foot,
                arm_right_upper,
                arm_right_lower,
                arm_right_foot,
                body_hips,
                tail_1,
                tail_2,
                tail_3,
                tail_4,
                tail_5,
                leg_left_upper,
                leg_left_lower,
                leg_left_foot,
                leg_right_upper,
                leg_right_lower,
                leg_right_foot
        );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityGiantMonitor lizard = (EntityGiantMonitor) entityIn;
        animator.update(lizard);

        // IDLE_TONGUE (20t): two quick flicks of the forked tongue out of the closed mouth
        animator.setAnimation(EntityGiantMonitor.IDLE_TONGUE);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(4);
            animator.move(tongue, 0, 0, -2.6F);
            this.rotate(animator, tongue, 6.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(4);
            animator.move(tongue, 0, 0, -0.6F);
            animator.endKeyframe();
        }
        animator.resetKeyframe(4);

        // ATTACK_BITE (14t): the head draws back and up, the jaw gapes, then the strike drives forward and down
        animator.setAnimation(EntityGiantMonitor.ATTACK_BITE);
        animator.startKeyframe(6);
        this.rotate(animator, neck, -14.0F, 0, 0);
        this.rotate(animator, head, -8.0F, 0, 0);
        this.rotate(animator, jaw, 30.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, 14.0F, 0, 0);
        this.rotate(animator, head, 6.0F, 0, 0);
        this.rotate(animator, jaw, 0, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);

        // ATTACK_THRASH (30t): jaws clamped, the head and neck whip side to side to tear
        animator.setAnimation(EntityGiantMonitor.ATTACK_THRASH);
        for (int i = 0; i < 3; i++) {
            animator.startKeyframe(4);
            this.rotate(animator, neck, 0, 28.0F, 0);
            this.rotate(animator, head, 0, 12.0F, 0);
            this.rotate(animator, body_belly, 0, -8.0F, 0);
            animator.endKeyframe();
            animator.startKeyframe(4);
            this.rotate(animator, neck, 0, -28.0F, 0);
            this.rotate(animator, head, 0, -12.0F, 0);
            this.rotate(animator, body_belly, 0, 8.0F, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(6);
    }

    public void setupAnim(EntityGiantMonitor lizard, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(lizard);
        limbSwingAmount = Math.min(0.5F, limbSwingAmount);
        float amt = Math.min(1.0F, limbSwingAmount / 0.35F);
        float gait = limbSwing * 0.6F / Math.max(0.3F, lizard.getMobSize() * lizard.getScale());
        this.tongue.showModel = lizard.getAnimation() == EntityGiantMonitor.IDLE_TONGUE;

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 22) * 0.03F);
        this.body_belly.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(tail_4, 0.06F, 0.10F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_5, 0.06F, 0.16F, false, -0.8F, 0F, ageInTicks, 1);

        // Blinking: bury the eye planes inside the skull; y/z keep the constructor's values.
        if (!lizard.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(1.6775F, -0.6000F, -3.0F);
            this.eye_right.setRotationPoint(-1.6775F, -0.6000F, -3.0F);
        }

        if (!lizard.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        if (lizard.isInWater() && !lizard.isOnGround()) {
            // swimming: legs pressed back along the flanks, the tail does the work
            float sw = ageInTicks * 0.3F;
            this.arm_left_upper.rotateAngleY += 0.9F;
            this.arm_right_upper.rotateAngleY -= 0.9F;
            this.leg_left_upper.rotateAngleY += 0.9F;
            this.leg_right_upper.rotateAngleY -= 0.9F;
            this.tail_1.rotateAngleY += Mth.cos(sw) * 0.20F;
            this.tail_2.rotateAngleY += Mth.cos(sw - 0.7F) * 0.30F;
            this.tail_3.rotateAngleY += Mth.cos(sw - 1.4F) * 0.35F;
            this.tail_4.rotateAngleY += Mth.cos(sw - 2.1F) * 0.40F;
            this.tail_5.rotateAngleY += Mth.cos(sw - 2.8F) * 0.45F;
            this.body_belly.rotateAngleY += Mth.cos(sw + 0.7F) * 0.08F;
        }
        else if (lizard.canMove()) {
            // ---- sprawling walk: diagonal pairs swing about the vertical axis ----
            // A limb laid along +x and one along -x rotated by the SAME rotateAngleY move in opposite directions, so the
            // two forelegs (and the two hind legs) alternate on equal values; hind = fore + pi gives the diagonal pairs.
            float c = Mth.cos(gait) * amt, sn = Mth.sin(gait);
            this.arm_left_upper.rotateAngleY += c * 0.55F;
            this.arm_right_upper.rotateAngleY += c * 0.55F;
            this.leg_left_upper.rotateAngleY -= c * 0.55F;
            this.leg_right_upper.rotateAngleY -= c * 0.55F;
            // lift the limb that is swinging forward (left-side outer ends rise on -Z, right-side on +Z)
            this.arm_left_upper.rotateAngleZ -= Math.max(0, sn) * 0.30F * amt;
            this.leg_right_upper.rotateAngleZ += Math.max(0, sn) * 0.30F * amt;
            this.arm_right_upper.rotateAngleZ += Math.max(0, -sn) * 0.30F * amt;
            this.leg_left_upper.rotateAngleZ -= Math.max(0, -sn) * 0.30F * amt;
            // the lateral S-curve: trunk one way, neck and tail the other, the tail wave lagging down its length
            this.body_belly.rotateAngleY += c * 0.10F;
            this.neck.rotateAngleY -= c * 0.12F;
            this.tail_1.rotateAngleY -= Mth.cos(gait - 0.6F) * 0.14F * amt;
            this.tail_2.rotateAngleY -= Mth.cos(gait - 1.2F) * 0.18F * amt;
            this.tail_3.rotateAngleY -= Mth.cos(gait - 1.8F) * 0.22F * amt;
            this.tail_4.rotateAngleY -= Mth.cos(gait - 2.4F) * 0.24F * amt;
            this.tail_5.rotateAngleY -= Mth.cos(gait - 3.0F) * 0.26F * amt;
        }

        if (lizard.sitProgress > 0) {
            // SOLVED (giant_monitor_spec.py --pose): belly on the ground, legs splayed with the feet flat, tail resting
            this.progressPosition(body_belly, lizard.sitProgress, 0.0F, 19.8000F, 0.0F, 40);
            this.progressRotation(neck, lizard.sitProgress, -0.1396F, -0.0F, 0.0F, 40);
            this.progressRotation(head, lizard.sitProgress, 0.1396F, -0.0F, 0.0F, 40);
            this.progressRotation(arm_left_upper, lizard.sitProgress, -0.0F, -0.0F, 0.0267F, 40);
            this.progressRotation(arm_left_lower, lizard.sitProgress, 0.9779F, -0.0F, -1.0392F, 40);
            this.progressRotation(arm_right_upper, lizard.sitProgress, -0.0F, -0.0F, -0.0267F, 40);
            this.progressRotation(arm_right_lower, lizard.sitProgress, 0.9779F, -0.0F, 1.0392F, 40);
            this.progressRotation(tail_1, lizard.sitProgress, -0.1094F, -0.0F, 0.0F, 40);
            this.progressRotation(tail_2, lizard.sitProgress, -0.0407F, -0.0F, 0.0F, 40);
            this.progressRotation(tail_3, lizard.sitProgress, -0.0203F, -0.0F, 0.0F, 40);
            this.progressRotation(tail_4, lizard.sitProgress, 0.0407F, -0.0F, 0.0F, 40);
            this.progressRotation(tail_5, lizard.sitProgress, 0.0542F, -0.0F, 0.0F, 40);
            this.progressRotation(leg_left_upper, lizard.sitProgress, -0.0F, -0.0F, -0.4987F, 40);
            this.progressRotation(leg_left_lower, lizard.sitProgress, 0.3812F, -0.0F, 0.1692F, 40);
            this.progressRotation(leg_right_upper, lizard.sitProgress, -0.0F, -0.0F, 0.4987F, 40);
            this.progressRotation(leg_right_lower, lizard.sitProgress, 0.3812F, -0.0F, -0.1692F, 40);
        }
        else if (lizard.sleepProgress > 0) {
            // SOLVED (giant_monitor_spec.py --pose): belly on the ground, legs splayed with the feet flat, tail resting, chin down, tail curled
            this.progressPosition(body_belly, lizard.sleepProgress, 0.0F, 19.8000F, 0.0F, 40);
            this.progressRotation(neck, lizard.sleepProgress, 0.2443F, -0.0F, 0.0F, 40);
            this.progressRotation(head, lizard.sleepProgress, -0.1222F, -0.0F, 0.0F, 40);
            this.progressRotation(arm_left_upper, lizard.sleepProgress, -0.0F, -0.0F, -0.2855F, 40);
            this.progressRotation(arm_left_lower, lizard.sleepProgress, 0.5121F, -0.0F, -0.4684F, 40);
            this.progressRotation(arm_right_upper, lizard.sleepProgress, -0.0F, -0.0F, 0.2855F, 40);
            this.progressRotation(arm_right_lower, lizard.sleepProgress, 0.5121F, -0.0F, 0.4684F, 40);
            this.progressRotation(tail_1, lizard.sleepProgress, -0.1097F, -0.1571F, 0.0F, 40);
            this.progressRotation(tail_2, lizard.sleepProgress, -0.0408F, -0.1571F, 0.0F, 40);
            this.progressRotation(tail_3, lizard.sleepProgress, -0.0204F, -0.1571F, 0.0F, 40);
            this.progressRotation(tail_4, lizard.sleepProgress, 0.0408F, -0.1571F, 0.0F, 40);
            this.progressRotation(tail_5, lizard.sleepProgress, 0.0544F, -0.1571F, 0.0F, 40);
            this.progressRotation(leg_left_upper, lizard.sleepProgress, -0.0F, -0.0F, 0.2690F, 40);
            this.progressRotation(leg_left_lower, lizard.sleepProgress, -0.2870F, -0.0F, -1.8056F, 40);
            this.progressRotation(leg_right_upper, lizard.sleepProgress, -0.0F, -0.0F, -0.2690F, 40);
            this.progressRotation(leg_right_lower, lizard.sleepProgress, -0.2870F, -0.0F, 1.8056F, 40);
        }
    }
}
