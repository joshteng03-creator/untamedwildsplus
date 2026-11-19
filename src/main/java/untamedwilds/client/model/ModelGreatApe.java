package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import untamedwilds.entity.mammal.EntityGreatApe;

/**
 * Great apes -- Gigantopithecus and the Bornean orangutan (2.9.0, NEW TYPE): 25 boxes on 128x128,
 * authored in scripts/great_ape_spec.py, ported VERBATIM by scripts/gen_model_great_ape.py, proved by verify_port.py.
 *
 * A knuckle-walker: LONG arms on fists and shorter legs on long plantigrade feet, so the torso is pitched up
 * 39 deg from hips to shoulders; a heavy-browed head with a prognathous muzzle and a massive jaw, counter-rotated
 * level; FORWARD-facing eye planes (blinking pushes them back into the skull, not inward).
 *   * The walk is FITTED per limb pair (great_ape_spec.walk_fit, lateral-sequence phases): no digging.
 *   * SIT is SOLVED (upright on the rump, fists down, feet forward); asleep the ape sits SLUMPED, chin on chest.
 */
public class ModelGreatApe extends AdvancedEntityModel<EntityGreatApe> {

    private final AdvancedModelBox body_main;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox body_belly;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox brow;
    private final AdvancedModelBox muzzle;
    private final AdvancedModelBox nose;
    private final AdvancedModelBox jaw;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox arm_upper_left;
    private final AdvancedModelBox arm_fore_left;
    private final AdvancedModelBox hand_left;
    private final AdvancedModelBox leg_thigh_left;
    private final AdvancedModelBox leg_shin_left;
    private final AdvancedModelBox foot_left;
    private final AdvancedModelBox arm_upper_right;
    private final AdvancedModelBox arm_fore_right;
    private final AdvancedModelBox hand_right;
    private final AdvancedModelBox leg_thigh_right;
    private final AdvancedModelBox leg_shin_right;
    private final AdvancedModelBox foot_right;

    private final ModelAnimator animator;

    public ModelGreatApe() {
        this.texWidth = 128;
        this.texHeight = 128;

        this.body_main = new AdvancedModelBox(this, 0, 0);
        this.body_main.setRotationPoint(0.0F, 8.4049F, 7.5000F);
        this.body_main.addBox(-6.0F, -5.0F, -19.5000F, 12.0F, 11.8000F, 22.5000F, 0.0F);
        this.setRotateAngle(body_main, -0.6799F, 0.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 69, 0);
        this.body_chest.setRotationPoint(0.0F, -1.0F, -14.0F);
        this.body_chest.addBox(-7.2000F, -4.8000F, -7.0F, 14.4000F, 10.0F, 11.5000F, 0.0F);
        this.body_belly = new AdvancedModelBox(this, 0, 35);
        this.body_belly.setRotationPoint(0.0F, 4.0F, -5.0F);
        this.body_belly.addBox(-5.6000F, -3.0F, -6.0F, 11.2000F, 7.6000F, 12.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 14, 55);
        this.neck.setRotationPoint(0.0F, 2.5827F, -18.4874F);
        this.neck.addBox(-3.4000F, -3.4000F, -4.0F, 6.8000F, 6.4000F, 6.0F, 0.0F);
        this.setRotateAngle(neck, 0.3400F, 0.0F, 0.0F);
        this.head = new AdvancedModelBox(this, 79, 35);
        this.head.setRotationPoint(0.0F, -0.0366F, -3.1951F);
        this.head.addBox(-4.3000F, -4.6000F, -6.6000F, 8.6000F, 8.2000F, 7.2000F, 0.0F);
        this.setRotateAngle(head, 0.4447F, 0.0F, 0.0F);
        this.brow = new AdvancedModelBox(this, 0, 80);
        this.brow.setRotationPoint(0.0F, -3.1000F, -6.0F);
        this.brow.addBox(-4.5000F, -0.8000F, -1.1000F, 9.0F, 1.5000F, 2.5000F, 0.0F);
        this.muzzle = new AdvancedModelBox(this, 70, 71);
        this.muzzle.setRotationPoint(0.0F, 1.2000F, -6.6000F);
        this.muzzle.addBox(-3.0F, -2.0F, -2.6000F, 6.0F, 4.0F, 3.2000F, 0.0F);
        this.nose = new AdvancedModelBox(this, 33, 80);
        this.nose.setRotationPoint(0.0F, -1.3000F, -2.6000F);
        this.nose.addBox(-1.2000F, -0.4000F, -0.3500F, 2.4000F, 0.9000F, 0.5500F, 0.0F);
        this.jaw = new AdvancedModelBox(this, 0, 71);
        this.jaw.setRotationPoint(0.0F, 3.3000F, -2.5000F);
        this.jaw.addBox(-3.3000F, -0.6000F, -6.3000F, 6.6000F, 2.1000F, 6.2000F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 23, 80);
        this.ear_left.setRotationPoint(4.4000F, -1.0F, -2.6000F);
        this.ear_left.addBox(-0.4000F, -0.8000F, -0.6000F, 0.9000F, 1.6000F, 1.2000F, 0.0F);
        this.eye_left = new AdvancedModelBox(this, 39, 80);
        this.eye_left.setRotationPoint(1.9000F, -1.5000F, -6.9500F);
        this.eye_left.addBox(-0.7000F, -0.6000F, 0.0F, 1.4000F, 1.2000F, 0.0F, 0.0F);
        this.ear_right = new AdvancedModelBox(this, 28, 80);
        this.ear_right.setRotationPoint(-4.4000F, -1.0F, -2.6000F);
        this.ear_right.addBox(-0.5000F, -0.8000F, -0.6000F, 0.9000F, 1.6000F, 1.2000F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 42, 80);
        this.eye_right.setRotationPoint(-1.9000F, -1.5000F, -6.9500F);
        this.eye_right.addBox(-0.7000F, -0.6000F, 0.0F, 1.4000F, 1.2000F, 0.0F, 0.0F);
        this.arm_upper_left = new AdvancedModelBox(this, 47, 35);
        this.arm_upper_left.setRotationPoint(6.0F, 1.3997F, -16.2290F);
        this.arm_upper_left.addBox(-1.9000F, -1.6000F, -2.0F, 3.8000F, 12.6000F, 4.0F, 0.0F);
        this.setRotateAngle(arm_upper_left, 0.6001F, 0.0F, 0.0F);
        this.arm_fore_left = new AdvancedModelBox(this, 111, 35);
        this.arm_fore_left.setRotationPoint(0.0F, 11.0F, 0.0F);
        this.arm_fore_left.addBox(-1.6500F, -1.0F, -1.7500F, 3.3000F, 12.0F, 3.5000F, 0.0F);
        this.setRotateAngle(arm_fore_left, 0.1795F, 0.0F, 0.0F);
        this.hand_left = new AdvancedModelBox(this, 89, 71);
        this.hand_left.setRotationPoint(0.0F, 11.0F, 0.0F);
        this.hand_left.addBox(-1.9000F, -0.6000F, -2.6000F, 3.8000F, 3.4000F, 4.6000F, 0.0F);
        this.setRotateAngle(hand_left, -0.0997F, 0.0F, 0.0F);
        this.leg_thigh_left = new AdvancedModelBox(this, 40, 55);
        this.leg_thigh_left.setRotationPoint(4.3000F, 1.3997F, 1.1317F);
        this.leg_thigh_left.addBox(-2.0F, -1.6000F, -2.1000F, 4.0F, 8.6000F, 4.2000F, 0.0F);
        this.setRotateAngle(leg_thigh_left, 0.0364F, 0.0F, 0.0F);
        this.leg_shin_left = new AdvancedModelBox(this, 74, 55);
        this.leg_shin_left.setRotationPoint(0.0F, 7.0F, 0.0F);
        this.leg_shin_left.addBox(-1.7000F, -1.0F, -1.8000F, 3.4000F, 8.0F, 3.6000F, 0.0F);
        this.setRotateAngle(leg_shin_left, 0.8861F, 0.0F, 0.0F);
        this.foot_left = new AdvancedModelBox(this, 26, 71);
        this.foot_left.setRotationPoint(0.0F, 7.0F, 0.0F);
        this.foot_left.addBox(-1.9500F, -0.6000F, -5.2000F, 3.9000F, 2.0F, 6.8000F, 0.0F);
        this.setRotateAngle(foot_left, -0.2426F, 0.0F, 0.0F);
        this.arm_upper_right = new AdvancedModelBox(this, 63, 35);
        this.arm_upper_right.setRotationPoint(-6.0F, 1.3997F, -16.2290F);
        this.arm_upper_right.addBox(-1.9000F, -1.6000F, -2.0F, 3.8000F, 12.6000F, 4.0F, 0.0F);
        this.setRotateAngle(arm_upper_right, 0.6001F, 0.0F, 0.0F);
        this.arm_fore_right = new AdvancedModelBox(this, 0, 55);
        this.arm_fore_right.setRotationPoint(0.0F, 11.0F, 0.0F);
        this.arm_fore_right.addBox(-1.6500F, -1.0F, -1.7500F, 3.3000F, 12.0F, 3.5000F, 0.0F);
        this.setRotateAngle(arm_fore_right, 0.1795F, 0.0F, 0.0F);
        this.hand_right = new AdvancedModelBox(this, 106, 71);
        this.hand_right.setRotationPoint(0.0F, 11.0F, 0.0F);
        this.hand_right.addBox(-1.9000F, -0.6000F, -2.6000F, 3.8000F, 3.4000F, 4.6000F, 0.0F);
        this.setRotateAngle(hand_right, -0.0997F, 0.0F, 0.0F);
        this.leg_thigh_right = new AdvancedModelBox(this, 57, 55);
        this.leg_thigh_right.setRotationPoint(-4.3000F, 1.3997F, 1.1317F);
        this.leg_thigh_right.addBox(-2.0F, -1.6000F, -2.1000F, 4.0F, 8.6000F, 4.2000F, 0.0F);
        this.setRotateAngle(leg_thigh_right, 0.0364F, 0.0F, 0.0F);
        this.leg_shin_right = new AdvancedModelBox(this, 89, 55);
        this.leg_shin_right.setRotationPoint(0.0F, 7.0F, 0.0F);
        this.leg_shin_right.addBox(-1.7000F, -1.0F, -1.8000F, 3.4000F, 8.0F, 3.6000F, 0.0F);
        this.setRotateAngle(leg_shin_right, 0.8861F, 0.0F, 0.0F);
        this.foot_right = new AdvancedModelBox(this, 48, 71);
        this.foot_right.setRotationPoint(0.0F, 7.0F, 0.0F);
        this.foot_right.addBox(-1.9500F, -0.6000F, -5.2000F, 3.9000F, 2.0F, 6.8000F, 0.0F);
        this.setRotateAngle(foot_right, -0.2426F, 0.0F, 0.0F);

        this.body_main.addChild(this.body_chest);
        this.body_main.addChild(this.body_belly);
        this.body_main.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.brow);
        this.head.addChild(this.muzzle);
        this.muzzle.addChild(this.nose);
        this.head.addChild(this.jaw);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.ear_right);
        this.head.addChild(this.eye_right);
        this.body_main.addChild(this.arm_upper_left);
        this.arm_upper_left.addChild(this.arm_fore_left);
        this.arm_fore_left.addChild(this.hand_left);
        this.body_main.addChild(this.leg_thigh_left);
        this.leg_thigh_left.addChild(this.leg_shin_left);
        this.leg_shin_left.addChild(this.foot_left);
        this.body_main.addChild(this.arm_upper_right);
        this.arm_upper_right.addChild(this.arm_fore_right);
        this.arm_fore_right.addChild(this.hand_right);
        this.body_main.addChild(this.leg_thigh_right);
        this.leg_thigh_right.addChild(this.leg_shin_right);
        this.leg_shin_right.addChild(this.foot_right);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_main);
    }

    // Built once: getAllParts() runs every frame (2.8.0).
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
                body_main,
                body_chest,
                body_belly,
                neck,
                head,
                brow,
                muzzle,
                nose,
                jaw,
                ear_left,
                eye_left,
                ear_right,
                eye_right,
                arm_upper_left,
                arm_fore_left,
                hand_left,
                leg_thigh_left,
                leg_shin_left,
                foot_left,
                arm_upper_right,
                arm_fore_right,
                hand_right,
                leg_thigh_right,
                leg_shin_right,
                foot_right
            );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        animator.update(entityIn);
        // <gen:idle-poses> 2.8.0: SOLVED by scripts/pose_reach.py against this model's own boxes (muzzle to the grass / the
        // water line beyond the bank, every head part and horn family clear of the ground, nothing through the torso).
        // Regenerate, never hand-edit: python scripts/gen_keyframes.py great_ape
        // ALERT (40t, AlertGoal): head raised, ears up, staring at the threat; repeats while the alert lasts
        animator.setAnimation(EntityGreatApe.ALERT);
        animator.startKeyframe(8);
        this.rotate(animator, neck, -35.0F, 0.0F, 0.0F);
        this.rotate(animator, head, 3.0F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 12.0F, 0.0F, 0.0F);
        animator.endKeyframe();
        animator.setStaticKeyframe(24);
        animator.resetKeyframe(8);
        // </gen:idle-poses>
        // ATTACK_SWIPE (16t): weight onto the left fist, the right arm swings up, forward and across
        animator.setAnimation(EntityGreatApe.ATTACK_SWIPE);
        animator.startKeyframe(6);
        this.rotate(animator, body_main, -6.0F, 0, 0);
        this.rotate(animator, arm_upper_right, -70.0F, 0, -20.0F);
        this.rotate(animator, arm_fore_right, -30.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, arm_upper_right, -20.0F, 30.0F, 10.0F);
        this.rotate(animator, arm_fore_right, -10.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(6);
        // ATTACK_THREATEN (40t): the ape rears up off its fists, arms flung up and out, and drops back with a slam
        animator.setAnimation(EntityGreatApe.ATTACK_THREATEN);
        animator.startKeyframe(10);
        this.rotate(animator, body_main, -30.0F, 0, 0);
        this.rotate(animator, head, 25.0F, 0, 0);
        this.rotate(animator, arm_upper_left, -60.0F, 0, 25.0F);
        this.rotate(animator, arm_upper_right, -60.0F, 0, -25.0F);
        this.rotate(animator, leg_thigh_left, 30.0F, 0, 0);
        this.rotate(animator, leg_thigh_right, 30.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(14);
        animator.resetKeyframe(8);
        animator.setStaticKeyframe(8);
        // EAT (40t), hand-made: an ape feeds with its HANDS (a knuckle-walker's mouth cannot reach the ground) --
        // the right arm reaches forward and down, brings the food up to the mouth, the head bows to meet it, it chews
        animator.setAnimation(EntityGreatApe.EAT);
        animator.startKeyframe(8);
        this.rotate(animator, arm_upper_right, -25.0F, 0, 0);
        this.rotate(animator, arm_fore_right, -20.0F, 0, 0);
        this.rotate(animator, head, 10.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(10);
        this.rotate(animator, arm_upper_right, -75.0F, 0, -15.0F);
        this.rotate(animator, arm_fore_right, -80.0F, 0, 0);
        this.rotate(animator, head, 8.0F, 0, 0);
        animator.endKeyframe();
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(4);
            this.rotate(animator, arm_upper_right, -75.0F, 0, -15.0F);
            this.rotate(animator, arm_fore_right, -80.0F, 0, 0);
            this.rotate(animator, head, 8.0F, 0, 0);
            this.rotate(animator, jaw, 12.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(3);
            this.rotate(animator, arm_upper_right, -75.0F, 0, -15.0F);
            this.rotate(animator, arm_fore_right, -80.0F, 0, 0);
            this.rotate(animator, head, 8.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);
    }

    public void setupAnim(EntityGreatApe ape, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(ape);
        float rest = Math.max(ape.sitProgress, ape.sleepProgress) / 40F;
        limbSwingAmount = Math.min(1.0F, limbSwingAmount * 1.6F) * (1F - rest);
        float gait = limbSwing / Math.max(0.4F, ape.getMobSize() * ape.getScale());

        float breath = (float) (Math.sin(ageInTicks / 20) * 0.03F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);

        // ---- knuckle-walk: FITTED per limb pair (great_ape_spec.walk_fit), Java radians ----
        this.walk(leg_thigh_left, 0.8000F, 0.2570F, false, -1.2606F, 0.3140F, gait, limbSwingAmount);
        this.walk(leg_shin_left, 0.8000F, -0.0616F, false, -0.4496F, 0.1252F, gait, limbSwingAmount);
        this.walk(foot_left, 0.8000F, 0.1962F, false, 1.6306F, -0.4257F, gait, limbSwingAmount);
        this.walk(leg_thigh_right, 0.8000F, 0.2570F, false, 1.8810F, 0.3140F, gait, limbSwingAmount);
        this.walk(leg_shin_right, 0.8000F, -0.0616F, false, 2.6920F, 0.1252F, gait, limbSwingAmount);
        this.walk(foot_right, 0.8000F, 0.1962F, false, 4.7722F, -0.4257F, gait, limbSwingAmount);
        this.walk(arm_upper_left, 0.8000F, -0.1435F, false, 0.6693F, -0.0992F, gait, limbSwingAmount);
        this.walk(arm_fore_left, 0.8000F, -0.2616F, false, -0.9210F, 0.2406F, gait, limbSwingAmount);
        this.walk(hand_left, 0.8000F, -0.3969F, false, -3.6773F, -0.2366F, gait, limbSwingAmount);
        this.walk(arm_upper_right, 0.8000F, -0.1435F, false, 3.8109F, -0.0992F, gait, limbSwingAmount);
        this.walk(arm_fore_right, 0.8000F, -0.2616F, false, 2.2206F, 0.2406F, gait, limbSwingAmount);
        this.walk(hand_right, 0.8000F, -0.3969F, false, -0.5357F, -0.2366F, gait, limbSwingAmount);
        this.swing(body_main, 0.8000F, 0.06F, false, 0F, 0F, gait, limbSwingAmount);
        this.bob(body_main, 1.6000F, 0.4F, false, gait, limbSwingAmount);

        // Blinking: the forward-facing eye planes are pushed BACK into the skull; x/y keep the constructor's values.
        if (!ape.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(1.9000F, -1.5000F, -5.7500F);
            this.eye_right.setRotationPoint(-1.9000F, -1.5000F, -5.7500F);
        }

        if (!ape.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 2, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        // ---- SOLVED rest poses (great_ape_spec.solve_rest): target = rest + delta, Java axes ----
        if (ape.sitProgress > 0) {
            this.progressPosition(body_main, ape.sitProgress, 0.0F, 19.5530F, 7.5000F, 40);
            this.progressRotation(arm_fore_left, ape.sitProgress, 0.9129F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_fore_right, ape.sitProgress, 0.9129F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_upper_left, ape.sitProgress, 0.4125F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_upper_right, ape.sitProgress, 0.4125F, 0.0F, 0.0F, 40);
            this.progressRotation(body_main, ape.sitProgress, -1.3781F, 0.0F, 0.0F, 40);
            this.progressRotation(foot_left, ape.sitProgress, -2.0478F, 0.0F, 0.0F, 40);
            this.progressRotation(foot_right, ape.sitProgress, -2.0478F, 0.0F, 0.0F, 40);
            this.progressRotation(hand_left, ape.sitProgress, 0.0527F, 0.0F, 0.0F, 40);
            this.progressRotation(hand_right, ape.sitProgress, 0.0527F, 0.0F, 0.0F, 40);
            this.progressRotation(head, ape.sitProgress, 1.1428F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_shin_left, ape.sitProgress, 0.4917F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_shin_right, ape.sitProgress, 0.4917F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_thigh_left, ape.sitProgress, -0.3368F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_thigh_right, ape.sitProgress, -0.3368F, 0.0F, 0.0F, 40);
        }
        else if (ape.sleepProgress > 0) {
            this.progressPosition(body_main, ape.sleepProgress, 0.0F, 19.5530F, 7.5000F, 40);
            this.progressRotation(arm_fore_left, ape.sleepProgress, 0.9129F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_fore_right, ape.sleepProgress, 0.9129F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_upper_left, ape.sleepProgress, 0.4125F, 0.0F, 0.0F, 40);
            this.progressRotation(arm_upper_right, ape.sleepProgress, 0.4125F, 0.0F, 0.0F, 40);
            this.progressRotation(body_main, ape.sleepProgress, -1.3781F, 0.0F, 0.0F, 40);
            this.progressRotation(foot_left, ape.sleepProgress, -2.0478F, 0.0F, 0.0F, 40);
            this.progressRotation(foot_right, ape.sleepProgress, -2.0478F, 0.0F, 0.0F, 40);
            this.progressRotation(hand_left, ape.sleepProgress, 0.0527F, 0.0F, 0.0F, 40);
            this.progressRotation(hand_right, ape.sleepProgress, 0.0527F, 0.0F, 0.0F, 40);
            this.progressRotation(head, ape.sleepProgress, 1.5617F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_shin_left, ape.sleepProgress, 0.4917F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_shin_right, ape.sleepProgress, 0.4917F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_thigh_left, ape.sleepProgress, -0.3368F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_thigh_right, ape.sleepProgress, -0.3368F, 0.0F, 0.0F, 40);
            this.progressRotation(neck, ape.sleepProgress, 0.5145F, 0.0F, 0.0F, 40);
        }
    }
}
