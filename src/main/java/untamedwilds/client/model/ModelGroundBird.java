package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.bird.EntityGroundBird;

/**
 * Giant ground birds -- the giant moa, the elephant bird and Genyornis (2.9.0, NEW TYPE): 19 boxes on
 * 128x128, authored in scripts/ground_bird_spec.py, ported VERBATIM by scripts/gen_model_ground_bird.py,
 * proved by verify_port.py.
 *
 * A deep egg-shaped body, a long three-segment neck held up and forward, a small head and short beak, and the bird's
 * zig-zag leg authored by WORLD direction per segment (thigh forward-down inside the plumage, the long drumstick down
 * and BACK, the tarsus down-forward, a flat foot) on a hip height solved so the feet stand on the ground.
 *   * `deepBeak` (Genyornis): the beak is deepened with setScale -- written every frame (the model is shared).
 *   * The walk is FITTED (ground_bird_spec.walk_fit): no digging, the foot planted about half the cycle.
 *   * SIT and SLEEP are SOLVED: the bird rests on its hocks, tarsi flat forward along the ground under the belly;
 *     asleep the neck is laid forward along the ground with the head level.
 */
public class ModelGroundBird extends AdvancedEntityModel<EntityGroundBird> {

    private final AdvancedModelBox body_main;
    private final AdvancedModelBox body_breast;
    private final AdvancedModelBox body_rump;
    private final AdvancedModelBox tail;
    private final AdvancedModelBox neck_1;
    private final AdvancedModelBox neck_2;
    private final AdvancedModelBox neck_3;
    private final AdvancedModelBox head;
    private final AdvancedModelBox beak;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox leg_thigh_left;
    private final AdvancedModelBox leg_shin_left;
    private final AdvancedModelBox leg_tarsus_left;
    private final AdvancedModelBox foot_left;
    private final AdvancedModelBox leg_thigh_right;
    private final AdvancedModelBox leg_shin_right;
    private final AdvancedModelBox leg_tarsus_right;
    private final AdvancedModelBox foot_right;

    private final ModelAnimator animator;

    public ModelGroundBird() {
        this.texWidth = 128;
        this.texHeight = 128;

        this.body_main = new AdvancedModelBox(this, 0, 0);
        this.body_main.setRotationPoint(0.0F, 1.3130F, 1.0F);
        this.body_main.addBox(-5.6000F, -5.3000F, -9.0F, 11.2000F, 12.0F, 17.5000F, 0.0F);
        this.body_breast = new AdvancedModelBox(this, 58, 0);
        this.body_breast.setRotationPoint(0.0F, 1.0F, -7.0F);
        this.body_breast.addBox(-4.0F, -3.7000F, -4.5000F, 8.0F, 10.2000F, 7.0F, 0.0F);
        this.setRotateAngle(body_breast, 0.2094F, 0.0F, 0.0F);
        this.body_rump = new AdvancedModelBox(this, 88, 0);
        this.body_rump.setRotationPoint(0.0F, -1.0F, 7.5000F);
        this.body_rump.addBox(-4.7000F, -3.7000F, -1.0F, 9.4000F, 9.2000F, 5.5000F, 0.0F);
        this.setRotateAngle(body_rump, -0.2443F, 0.0F, 0.0F);
        this.tail = new AdvancedModelBox(this, 0, 45);
        this.tail.setRotationPoint(0.0F, -0.5000F, 4.0F);
        this.tail.addBox(-3.6000F, -1.6000F, -0.5000F, 7.2000F, 5.8000F, 5.0F, 0.0F);
        this.setRotateAngle(tail, -0.6632F, 0.0F, 0.0F);
        this.neck_1 = new AdvancedModelBox(this, 28, 30);
        this.neck_1.setRotationPoint(0.0F, -3.0F, -9.5000F);
        this.neck_1.addBox(-2.4000F, -2.3000F, -6.5000F, 4.8000F, 4.6000F, 8.3000F, 0.0F);
        this.setRotateAngle(neck_1, -0.8378F, 0.0F, 0.0F);
        this.neck_2 = new AdvancedModelBox(this, 25, 45);
        this.neck_2.setRotationPoint(0.0F, 0.0F, -6.5000F);
        this.neck_2.addBox(-1.9500F, -1.9500F, -6.0F, 3.9000F, 3.9000F, 7.0F, 0.0F);
        this.setRotateAngle(neck_2, -0.2443F, 0.0F, 0.0F);
        this.neck_3 = new AdvancedModelBox(this, 47, 45);
        this.neck_3.setRotationPoint(0.0F, 0.0F, -6.0F);
        this.neck_3.addBox(-1.6000F, -1.7000F, -5.5000F, 3.2000F, 3.4000F, 6.4000F, 0.0F);
        this.setRotateAngle(neck_3, 0.3840F, 0.0F, 0.0F);
        this.head = new AdvancedModelBox(this, 67, 45);
        this.head.setRotationPoint(0.0F, 0.0F, -5.5000F);
        this.head.addBox(-1.8500F, -1.8000F, -4.4000F, 3.7000F, 3.3000F, 5.2000F, 0.0F);
        this.setRotateAngle(head, 0.7679F, 0.0F, 0.0F);
        this.beak = new AdvancedModelBox(this, 0, 56);
        this.beak.setRotationPoint(0.0F, 0.0F, -4.4000F);
        this.beak.addBox(-0.8500F, -0.7000F, -3.4000F, 1.7000F, 1.3000F, 3.8000F, 0.0F);
        this.setRotateAngle(beak, 0.1745F, 0.0F, 0.0F);
        this.eye_left = new AdvancedModelBox(this, 11, 56);
        this.eye_left.setRotationPoint(2.2000F, -0.6000F, -2.0F);
        this.eye_left.addBox(0.0F, -0.5500F, -0.8000F, 0.0F, 1.1000F, 1.4000F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 14, 56);
        this.eye_right.setRotationPoint(-2.2000F, -0.6000F, -2.0F);
        this.eye_right.addBox(0.0F, -0.5500F, -0.8000F, 0.0F, 1.1000F, 1.4000F, 0.0F);
        this.leg_thigh_left = new AdvancedModelBox(this, 55, 30);
        this.leg_thigh_left.setRotationPoint(3.1000F, 1.5000F, 0.0F);
        this.leg_thigh_left.addBox(-1.8000F, -1.4000F, -1.9000F, 3.6000F, 7.4000F, 3.8000F, 0.0F);
        this.setRotateAngle(leg_thigh_left, -0.9856F, 0.0F, 0.0F);
        this.leg_shin_left = new AdvancedModelBox(this, 0, 30);
        this.leg_shin_left.setRotationPoint(0.0F, 6.0F, 0.0F);
        this.leg_shin_left.addBox(-1.6000F, -1.0F, -1.7000F, 3.2000F, 11.0F, 3.4000F, 0.0F);
        this.setRotateAngle(leg_shin_left, 1.3865F, 0.0F, 0.0F);
        this.leg_tarsus_left = new AdvancedModelBox(this, 85, 30);
        this.leg_tarsus_left.setRotationPoint(0.0F, 10.0F, 0.0F);
        this.leg_tarsus_left.addBox(-1.2000F, -0.8000F, -1.2000F, 2.4000F, 8.8000F, 2.4000F, 0.0F);
        this.setRotateAngle(leg_tarsus_left, -0.6435F, 0.0F, 0.0F);
        this.foot_left = new AdvancedModelBox(this, 85, 45);
        this.foot_left.setRotationPoint(0.0F, 8.0F, 0.0F);
        this.foot_left.addBox(-2.2000F, 0.0F, -4.2000F, 4.4000F, 0.9000F, 5.4000F, 0.0F);
        this.setRotateAngle(foot_left, 0.2426F, 0.0F, 0.0F);
        this.leg_thigh_right = new AdvancedModelBox(this, 70, 30);
        this.leg_thigh_right.setRotationPoint(-3.1000F, 1.5000F, 0.0F);
        this.leg_thigh_right.addBox(-1.8000F, -1.4000F, -1.9000F, 3.6000F, 7.4000F, 3.8000F, 0.0F);
        this.setRotateAngle(leg_thigh_right, -0.9856F, 0.0F, 0.0F);
        this.leg_shin_right = new AdvancedModelBox(this, 14, 30);
        this.leg_shin_right.setRotationPoint(0.0F, 6.0F, 0.0F);
        this.leg_shin_right.addBox(-1.6000F, -1.0F, -1.7000F, 3.2000F, 11.0F, 3.4000F, 0.0F);
        this.setRotateAngle(leg_shin_right, 1.3865F, 0.0F, 0.0F);
        this.leg_tarsus_right = new AdvancedModelBox(this, 95, 30);
        this.leg_tarsus_right.setRotationPoint(0.0F, 10.0F, 0.0F);
        this.leg_tarsus_right.addBox(-1.2000F, -0.8000F, -1.2000F, 2.4000F, 8.8000F, 2.4000F, 0.0F);
        this.setRotateAngle(leg_tarsus_right, -0.6435F, 0.0F, 0.0F);
        this.foot_right = new AdvancedModelBox(this, 105, 45);
        this.foot_right.setRotationPoint(0.0F, 8.0F, 0.0F);
        this.foot_right.addBox(-2.2000F, 0.0F, -4.2000F, 4.4000F, 0.9000F, 5.4000F, 0.0F);
        this.setRotateAngle(foot_right, 0.2426F, 0.0F, 0.0F);

        this.body_main.addChild(this.body_breast);
        this.body_main.addChild(this.body_rump);
        this.body_rump.addChild(this.tail);
        this.body_main.addChild(this.neck_1);
        this.neck_1.addChild(this.neck_2);
        this.neck_2.addChild(this.neck_3);
        this.neck_3.addChild(this.head);
        this.head.addChild(this.beak);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.eye_right);
        this.body_main.addChild(this.leg_thigh_left);
        this.leg_thigh_left.addChild(this.leg_shin_left);
        this.leg_shin_left.addChild(this.leg_tarsus_left);
        this.leg_tarsus_left.addChild(this.foot_left);
        this.body_main.addChild(this.leg_thigh_right);
        this.leg_thigh_right.addChild(this.leg_shin_right);
        this.leg_shin_right.addChild(this.leg_tarsus_right);
        this.leg_tarsus_right.addChild(this.foot_right);

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
                body_breast,
                body_rump,
                tail,
                neck_1,
                neck_2,
                neck_3,
                head,
                beak,
                eye_left,
                eye_right,
                leg_thigh_left,
                leg_shin_left,
                leg_tarsus_left,
                foot_left,
                leg_thigh_right,
                leg_shin_right,
                leg_tarsus_right,
                foot_right
            );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        animator.update(entityIn);
        // <gen:idle-poses> 2.8.0: SOLVED by scripts/pose_reach.py against this model's own boxes (muzzle to the grass / the
        // water line beyond the bank, every head part and horn family clear of the ground, nothing through the torso).
        // Regenerate, never hand-edit: python scripts/gen_keyframes.py ground_bird
        // EAT (40t, GrazeGoal's mouthful): head down to the grass, a cropping lift, back down
        animator.setAnimation(EntityGroundBird.EAT);
        animator.startKeyframe(10);
        this.rotate(animator, neck_1, 72.7F, 0.0F, 0.0F);
        this.rotate(animator, neck_2, 26.7F, 0.0F, 0.0F);
        this.rotate(animator, neck_3, 5.0F, 0.0F, 0.0F);
        this.rotate(animator, head, -3.9F, 0.0F, 0.0F);
        this.rotate(animator, beak, -3.7F, 0.0F, 0.0F);
        animator.move(neck_1, 0.0F, 6.0F, -0.6F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck_1, 72.7F, 0.0F, 0.0F);
        this.rotate(animator, neck_2, 26.7F, 0.0F, 0.0F);
        this.rotate(animator, neck_3, 5.0F, 0.0F, 0.0F);
        this.rotate(animator, head, -9.9F, 0.0F, 0.0F);
        this.rotate(animator, beak, -3.7F, 0.0F, 0.0F);
        animator.move(neck_1, 0.0F, 6.0F, -0.6F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck_1, 72.7F, 0.0F, 0.0F);
        this.rotate(animator, neck_2, 26.7F, 0.0F, 0.0F);
        this.rotate(animator, neck_3, 5.0F, 0.0F, 0.0F);
        this.rotate(animator, head, -3.9F, 0.0F, 0.0F);
        this.rotate(animator, beak, -3.7F, 0.0F, 0.0F);
        animator.move(neck_1, 0.0F, 6.0F, -0.6F);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.resetKeyframe(10);
        // DRINK (100t, DrinkGoal): muzzle to the water past the bank edge, two swallows lifting the head
        animator.setAnimation(EntityGroundBird.DRINK);
        animator.startKeyframe(15);
        this.rotate(animator, neck_1, 77.2F, 0.0F, 0.0F);
        this.rotate(animator, neck_2, 29.2F, 0.0F, 0.0F);
        this.rotate(animator, neck_3, -1.2F, 0.0F, 0.0F);
        this.rotate(animator, head, -6.7F, 0.0F, 0.0F);
        this.rotate(animator, beak, -2.0F, 0.0F, 0.0F);
        animator.move(neck_1, 0.0F, 6.0F, -0.4F);
        animator.endKeyframe();
        animator.setStaticKeyframe(20);
        animator.startKeyframe(5);
        this.rotate(animator, neck_1, 77.2F, 0.0F, 0.0F);
        this.rotate(animator, neck_2, 29.2F, 0.0F, 0.0F);
        this.rotate(animator, neck_3, -1.2F, 0.0F, 0.0F);
        this.rotate(animator, head, -11.7F, 0.0F, 0.0F);
        this.rotate(animator, beak, -2.0F, 0.0F, 0.0F);
        animator.move(neck_1, 0.0F, 6.0F, -0.4F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck_1, 77.2F, 0.0F, 0.0F);
        this.rotate(animator, neck_2, 29.2F, 0.0F, 0.0F);
        this.rotate(animator, neck_3, -1.2F, 0.0F, 0.0F);
        this.rotate(animator, head, -6.7F, 0.0F, 0.0F);
        this.rotate(animator, beak, -2.0F, 0.0F, 0.0F);
        animator.move(neck_1, 0.0F, 6.0F, -0.4F);
        animator.endKeyframe();
        animator.setStaticKeyframe(20);
        animator.startKeyframe(5);
        this.rotate(animator, neck_1, 77.2F, 0.0F, 0.0F);
        this.rotate(animator, neck_2, 29.2F, 0.0F, 0.0F);
        this.rotate(animator, neck_3, -1.2F, 0.0F, 0.0F);
        this.rotate(animator, head, -11.7F, 0.0F, 0.0F);
        this.rotate(animator, beak, -2.0F, 0.0F, 0.0F);
        animator.move(neck_1, 0.0F, 6.0F, -0.4F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck_1, 77.2F, 0.0F, 0.0F);
        this.rotate(animator, neck_2, 29.2F, 0.0F, 0.0F);
        this.rotate(animator, neck_3, -1.2F, 0.0F, 0.0F);
        this.rotate(animator, head, -6.7F, 0.0F, 0.0F);
        this.rotate(animator, beak, -2.0F, 0.0F, 0.0F);
        animator.move(neck_1, 0.0F, 6.0F, -0.4F);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.resetKeyframe(15);
        // ALERT (40t, AlertGoal): head raised, ears up, staring at the threat; repeats while the alert lasts
        animator.setAnimation(EntityGroundBird.ALERT);
        animator.startKeyframe(8);
        this.rotate(animator, neck_1, -31.4F, 0.0F, 0.0F);
        this.rotate(animator, neck_2, -3.0F, 0.0F, 0.0F);
        this.rotate(animator, neck_3, -3.9F, 0.0F, 0.0F);
        this.rotate(animator, head, 6.3F, 0.0F, 0.0F);
        this.rotate(animator, beak, 12.0F, 0.0F, 0.0F);
        animator.endKeyframe();
        animator.setStaticKeyframe(24);
        animator.resetKeyframe(8);
        // </gen:idle-poses>
        // ATTACK_KICK (18t): the bird rocks back and drives one leg forward and down -- the cassowary's defence
        animator.setAnimation(EntityGroundBird.ATTACK_KICK);
        animator.startKeyframe(6);
        this.rotate(animator, body_main, -6.0F, 0, 0);
        this.rotate(animator, leg_thigh_right, -40.0F, 0, 0);
        this.rotate(animator, leg_shin_right, 50.0F, 0, 0);
        this.rotate(animator, leg_tarsus_right, -40.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, body_main, -4.0F, 0, 0);
        this.rotate(animator, leg_thigh_right, -70.0F, 0, 0);
        this.rotate(animator, leg_shin_right, 10.0F, 0, 0);
        this.rotate(animator, leg_tarsus_right, -10.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(8);
        // ATTACK_THREATEN (40t): neck reared up and back, the tail plume raised, a stamp (ModelAnimator has no scale())
        animator.setAnimation(EntityGroundBird.ATTACK_THREATEN);
        animator.startKeyframe(10);
        this.rotate(animator, neck_1, 20.0F, 0, 0);
        this.rotate(animator, neck_2, 10.0F, 0, 0);
        this.rotate(animator, tail, -20.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.startKeyframe(6);
        this.rotate(animator, neck_1, 20.0F, 0, 0);
        this.rotate(animator, leg_thigh_left, -25.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(14);
    }

    public void setupAnim(EntityGroundBird bird, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(bird);
        float rest = Math.max(bird.sitProgress, bird.sleepProgress) / 40F;
        limbSwingAmount = Math.min(1.0F, limbSwingAmount * 1.6F) * (1F - rest);
        float gait = limbSwing / Math.max(0.4F, bird.getMobSize() * bird.getScale());

        // per species: Genyornis' deep beak (scale is not restored by resetToDefaultPose: write it every frame)
        boolean deep = ComplexMob.speciesFlag(bird, "deepBeak") == 1;
        this.beak.setScale(deep ? 1.2F : 1.0F, deep ? 2.1F : 1.0F, deep ? 1.15F : 1.0F);

        float breath = (float) (Math.sin(ageInTicks / 22) * 0.025F);
        this.body_main.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(tail, 0.1F, 0.08F, false, 0F, 0F, ageInTicks, 1F - rest);

        // ---- walk: FITTED (ground_bird_spec.walk_fit), Java radians ----
        this.walk(leg_thigh_left, 0.7000F, -0.0970F, false, -2.0255F, 0.0169F, gait, limbSwingAmount);
        this.walk(leg_shin_left, 0.7000F, -0.2071F, false, -3.4466F, 0.0372F, gait, limbSwingAmount);
        this.walk(leg_tarsus_left, 0.7000F, -0.0230F, false, 1.0480F, -0.1668F, gait, limbSwingAmount);
        this.walk(foot_left, 0.7000F, 0.0308F, false, 1.5788F, 0.0682F, gait, limbSwingAmount);
        this.walk(leg_thigh_right, 0.7000F, -0.0970F, false, 1.1161F, 0.0169F, gait, limbSwingAmount);
        this.walk(leg_shin_right, 0.7000F, -0.2071F, false, -0.3050F, 0.0372F, gait, limbSwingAmount);
        this.walk(leg_tarsus_right, 0.7000F, -0.0230F, false, 4.1896F, -0.1668F, gait, limbSwingAmount);
        this.walk(foot_right, 0.7000F, 0.0308F, false, 4.7204F, 0.0682F, gait, limbSwingAmount);
        // the head bobs with the stride, the body rocks over the planted foot
        this.walk(neck_1, 1.4000F, 0.06F, false, 0.5F, 0F, gait, limbSwingAmount);
        this.walk(head, 1.4000F, 0.06F, true, 0.5F, 0F, gait, limbSwingAmount);
        this.swing(body_main, 0.7000F, 0.05F, false, 0F, 0F, gait, limbSwingAmount);
        this.bob(body_main, 1.4000F, 0.5F, false, gait, limbSwingAmount);

        // Blinking: bury the eye planes inside the head; y/z keep the constructor's values.
        if (!bird.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(1.1000F, -0.6000F, -2.0F);
            this.eye_right.setRotationPoint(-1.1000F, -0.6000F, -2.0F);
        }

        if (!bird.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck_2);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        // ---- SOLVED rest poses (ground_bird_spec.solve_rest): target = rest + delta, Java axes ----
        if (bird.sitProgress > 0) {
            this.progressPosition(body_main, bird.sitProgress, 0.0F, 15.3065F, 1.0F, 40);
            this.progressRotation(foot_left, bird.sitProgress, 1.4768F, 0.0F, 0.0F, 40);
            this.progressRotation(foot_right, bird.sitProgress, 1.4768F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_shin_left, bird.sitProgress, 2.4271F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_shin_right, bird.sitProgress, 2.4271F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_tarsus_left, bird.sitProgress, -2.7445F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_tarsus_right, bird.sitProgress, -2.7445F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_thigh_left, bird.sitProgress, -1.1938F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_thigh_right, bird.sitProgress, -1.1938F, 0.0F, 0.0F, 40);
        }
        else if (bird.sleepProgress > 0) {
            this.progressPosition(body_main, bird.sleepProgress, 0.0F, 15.3065F, 1.0F, 40);
            this.progressRotation(foot_left, bird.sleepProgress, 1.4768F, 0.0F, 0.0F, 40);
            this.progressRotation(foot_right, bird.sleepProgress, 1.4768F, 0.0F, 0.0F, 40);
            this.progressRotation(head, bird.sleepProgress, -0.6283F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_shin_left, bird.sleepProgress, 2.4271F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_shin_right, bird.sleepProgress, 2.4271F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_tarsus_left, bird.sleepProgress, -2.7445F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_tarsus_right, bird.sleepProgress, -2.7445F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_thigh_left, bird.sleepProgress, -1.1938F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_thigh_right, bird.sleepProgress, -1.1938F, 0.0F, 0.0F, 40);
            this.progressRotation(neck_1, bird.sleepProgress, 0.5411F, 0.0F, 0.0F, 40);
            this.progressRotation(neck_2, bird.sleepProgress, 0.0524F, 0.0F, 0.0F, 40);
            this.progressRotation(neck_3, bird.sleepProgress, 0.0349F, 0.0F, 0.0F, 40);
        }
    }
}
