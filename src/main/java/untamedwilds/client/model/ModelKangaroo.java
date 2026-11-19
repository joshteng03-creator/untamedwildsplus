package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityKangaroo;

/**
 * Kangaroo -- the mod's first HOPPING body (Phase 4.2, 2026-10-10): 31 boxes on 256x128, authored from scratch in
 * scripts/kangaroo_spec.py (FK, ground/tail solves, z-fight, burial, UV pack, phase-7 pose solver), ported VERBATIM
 * from the modded_entity export by scripts/gen_model_kangaroo.py and proved by scripts/verify_port.py.
 *
 * REST pose = standing alert on the tripod: torso tipped up (hips 35, chest 50, neck 62 deg), the long feet flat,
 * the tail resting on the ground. Movement is MODEL-ONLY (user decision): the entity walks the ground like every
 * other mob and the model does the rest --
 *   * HOP (getCurrentSpeed() above SLOW_SPEED): the torso tips forward toward level, both hind legs swing together
 *     (push-off back, landing forward), the body rises over the airborne half of the cycle and the tail counter-swings;
 *   * CRAWL (slow, the pentapedal walk): blends toward a SOLVED crawl pose -- torso near level, forearms planted, tail
 *     on the ground -- while both hind legs swing forward together.
 * Gait phase is divided by the RENDER size so a 2 m Procoptodon and a 1.4 m grey cover the ground at the same rate.
 *
 * Per species (getVariant(), the ModelDeer:509-524 pattern): the sthenurines (Procoptodon, Sthenurus) get a SHORT FACE
 * (muzzle 0.55 long, nose pulled back with it) and LONG ARMS (each arm box 1.35 long with its child pivots re-stacked,
 * the ModelDireWolf stretchLimb method) plus claws. Citadel's resetToDefaultPose restores rotations and rotation points
 * but NOT scale, so every setScale is written on both branches every frame. The pouch joey follows the synced
 * EntityKangaroo.hasJoey() and ducks away whenever the mother sits or sleeps.
 */
public class ModelKangaroo extends AdvancedEntityModel<EntityKangaroo> {

    private final AdvancedModelBox body_hips;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox muzzle;
    private final AdvancedModelBox nose;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox arm_left_upper;
    private final AdvancedModelBox arm_left_fore;
    private final AdvancedModelBox arm_left_hand;
    private final AdvancedModelBox claw_left;
    private final AdvancedModelBox arm_right_upper;
    private final AdvancedModelBox arm_right_fore;
    private final AdvancedModelBox arm_right_hand;
    private final AdvancedModelBox claw_right;
    private final AdvancedModelBox joey_head;
    private final AdvancedModelBox joey_snout;
    private final AdvancedModelBox leg_left_thigh;
    private final AdvancedModelBox leg_left_shin;
    private final AdvancedModelBox leg_left_foot;
    private final AdvancedModelBox leg_left_toe;
    private final AdvancedModelBox leg_right_thigh;
    private final AdvancedModelBox leg_right_shin;
    private final AdvancedModelBox leg_right_foot;
    private final AdvancedModelBox leg_right_toe;
    private final AdvancedModelBox tail_1;
    private final AdvancedModelBox tail_2;
    private final AdvancedModelBox tail_3;

    private final ModelAnimator animator;
    /** Blocks per tick: below this the animal crawls on five limbs, above it hops. */
    private static final float SLOW_SPEED = 0.08F;

    public ModelKangaroo() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_hips = new AdvancedModelBox(this, 0, 0);
        this.body_hips.setRotationPoint(0.0F, 12.6678F, 0.0F);
        this.body_hips.addBox(-4.2000F, -4.0F, -7.0F, 8.4000F, 8.4000F, 10.5000F, 0.0F);
        this.setRotateAngle(body_hips, -0.6109F, 0.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 38, 0);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -7.0F);
        this.body_chest.addBox(-3.4000F, -3.4000F, -6.5000F, 6.8000F, 7.0F, 8.5000F, 0.0F);
        this.setRotateAngle(body_chest, -0.2618F, 0.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 187, 0);
        this.neck.setRotationPoint(0.0F, 0.0F, -6.5000F);
        this.neck.addBox(-1.9000F, -1.8000F, -4.0F, 3.8000F, 3.7000F, 5.6000F, 0.0F);
        this.setRotateAngle(neck, -0.2094F, 0.0F, 0.0F);
        this.head = new AdvancedModelBox(this, 146, 0);
        this.head.setRotationPoint(0.0F, 0.0F, -4.0F);
        this.head.addBox(-2.3000F, -2.0F, -4.4000F, 4.6000F, 4.0F, 5.8000F, 0.0F);
        this.setRotateAngle(head, 1.1694F, 0.0F, 0.0F);
        this.muzzle = new AdvancedModelBox(this, 234, 0);
        this.muzzle.setRotationPoint(0.0F, 0.0F, -4.4000F);
        this.muzzle.addBox(-1.5000F, -1.4000F, -3.4000F, 3.0F, 2.8000F, 4.4000F, 0.0F);
        this.setRotateAngle(muzzle, 0.2269F, 0.0F, 0.0F);
        this.nose = new AdvancedModelBox(this, 89, 19);
        this.nose.setRotationPoint(0.0F, -0.2000F, -3.5000F);
        this.nose.addBox(-1.1000F, -0.9000F, -0.8000F, 2.2000F, 1.7000F, 1.3000F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 36, 19);
        this.ear_left.setRotationPoint(1.4000F, -1.8000F, -0.1000F);
        this.ear_left.addBox(-0.8000F, -4.4000F, -0.4000F, 1.6000F, 4.8000F, 0.8000F, 0.0F);
        this.setRotateAngle(ear_left, 0.1396F, -0.2443F, 0.2443F);
        this.ear_right = new AdvancedModelBox(this, 41, 19);
        this.ear_right.setRotationPoint(-1.4000F, -1.8000F, -0.1000F);
        this.ear_right.addBox(-0.8000F, -4.4000F, -0.4000F, 1.6000F, 4.8000F, 0.8000F, 0.0F);
        this.setRotateAngle(ear_right, 0.1396F, 0.2443F, -0.2443F);
        this.eye_left = new AdvancedModelBox(this, 81, 19);
        this.eye_left.setRotationPoint(2.6000F, -0.4000F, -1.9000F);
        this.eye_left.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 85, 19);
        this.eye_right.setRotationPoint(-2.6000F, -0.4000F, -1.9000F);
        this.eye_right.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.arm_left_upper = new AdvancedModelBox(this, 0, 19);
        this.arm_left_upper.setRotationPoint(2.3000F, 3.2000F, -5.8000F);
        this.arm_left_upper.addBox(-1.3000F, -0.8000F, -1.2000F, 2.6000F, 3.8000F, 2.3000F, 0.0F);
        this.setRotateAngle(arm_left_upper, 0.5236F, 0.0F, 0.0F);
        this.arm_left_fore = new AdvancedModelBox(this, 20, 19);
        this.arm_left_fore.setRotationPoint(-0.1000F, 3.0F, 0.0F);
        this.arm_left_fore.addBox(-0.8500F, -0.7000F, -1.0F, 1.7000F, 3.5000F, 1.9000F, 0.0F);
        this.setRotateAngle(arm_left_fore, -0.6109F, 0.0F, 0.0F);
        this.arm_left_hand = new AdvancedModelBox(this, 65, 19);
        this.arm_left_hand.setRotationPoint(0.0F, 2.8000F, 0.0F);
        this.arm_left_hand.addBox(-1.1000F, -0.5000F, -0.8000F, 2.2000F, 2.1000F, 1.5000F, 0.0F);
        this.setRotateAngle(arm_left_hand, -0.3491F, 0.0F, 0.0F);
        this.claw_left = new AdvancedModelBox(this, 102, 19);
        this.claw_left.setRotationPoint(0.0F, 1.6000F, 0.0F);
        this.claw_left.addBox(-0.5000F, -0.3000F, -0.5000F, 1.0F, 1.9000F, 0.7000F, 0.0F);
        this.setRotateAngle(claw_left, -0.3491F, 0.0F, 0.0F);
        this.arm_right_upper = new AdvancedModelBox(this, 10, 19);
        this.arm_right_upper.setRotationPoint(-2.3000F, 3.2000F, -5.8000F);
        this.arm_right_upper.addBox(-1.3000F, -0.8000F, -1.2000F, 2.6000F, 3.8000F, 2.3000F, 0.0F);
        this.setRotateAngle(arm_right_upper, 0.5236F, 0.0F, 0.0F);
        this.arm_right_fore = new AdvancedModelBox(this, 28, 19);
        this.arm_right_fore.setRotationPoint(0.1000F, 3.0F, 0.0F);
        this.arm_right_fore.addBox(-0.8500F, -0.7000F, -1.0F, 1.7000F, 3.5000F, 1.9000F, 0.0F);
        this.setRotateAngle(arm_right_fore, -0.6109F, 0.0F, 0.0F);
        this.arm_right_hand = new AdvancedModelBox(this, 73, 19);
        this.arm_right_hand.setRotationPoint(0.0F, 2.8000F, 0.0F);
        this.arm_right_hand.addBox(-1.1000F, -0.5000F, -0.8000F, 2.2000F, 2.1000F, 1.5000F, 0.0F);
        this.setRotateAngle(arm_right_hand, -0.3491F, 0.0F, 0.0F);
        this.claw_right = new AdvancedModelBox(this, 106, 19);
        this.claw_right.setRotationPoint(0.0F, 1.6000F, 0.0F);
        this.claw_right.addBox(-0.5000F, -0.3000F, -0.5000F, 1.0F, 1.9000F, 0.7000F, 0.0F);
        this.setRotateAngle(claw_right, -0.3491F, 0.0F, 0.0F);
        this.joey_head = new AdvancedModelBox(this, 56, 19);
        this.joey_head.setRotationPoint(0.0F, 4.4000F, -5.2000F);
        this.joey_head.addBox(-0.9500F, -0.5000F, -1.2000F, 1.9000F, 1.9000F, 2.4000F, 0.0F);
        this.joey_snout = new AdvancedModelBox(this, 97, 19);
        this.joey_snout.setRotationPoint(0.0F, 0.8000F, -1.2000F);
        this.joey_snout.addBox(-0.6000F, -0.6000F, -1.0F, 1.2000F, 1.1000F, 1.2000F, 0.0F);
        this.leg_left_thigh = new AdvancedModelBox(this, 69, 0);
        this.leg_left_thigh.setRotationPoint(2.9500F, 0.6000F, -0.6000F);
        this.leg_left_thigh.addBox(-1.6500F, -2.2000F, -3.0F, 3.3000F, 9.2000F, 6.4000F, 0.0F);
        this.setRotateAngle(leg_left_thigh, -0.3491F, 0.0F, 0.0F);
        this.leg_left_shin = new AdvancedModelBox(this, 132, 0);
        this.leg_left_shin.setRotationPoint(-0.1000F, 7.0F, 0.0F);
        this.leg_left_shin.addBox(-0.7500F, -1.2000F, -1.1000F, 1.5000F, 9.6000F, 2.0F, 0.0F);
        this.setRotateAngle(leg_left_shin, 1.6930F, 0.0F, 0.0F);
        this.leg_left_foot = new AdvancedModelBox(this, 206, 0);
        this.leg_left_foot.setRotationPoint(-0.2000F, 8.4000F, 0.0F);
        this.leg_left_foot.addBox(-0.7500F, -0.6000F, -0.9000F, 1.5000F, 6.6000F, 1.5000F, 0.0F);
        this.setRotateAngle(leg_left_foot, -2.3038F, 0.0F, 0.0F);
        this.leg_left_toe = new AdvancedModelBox(this, 46, 19);
        this.leg_left_toe.setRotationPoint(0.0F, 6.0F, 0.0F);
        this.leg_left_toe.addBox(-0.5500F, -0.8000F, -0.5000F, 1.1000F, 3.8000F, 1.4000F, 0.0F);
        this.setRotateAngle(leg_left_toe, -0.0349F, 0.0F, 0.0F);
        this.leg_right_thigh = new AdvancedModelBox(this, 89, 0);
        this.leg_right_thigh.setRotationPoint(-2.9500F, 0.6000F, -0.6000F);
        this.leg_right_thigh.addBox(-1.6500F, -2.2000F, -3.0F, 3.3000F, 9.2000F, 6.4000F, 0.0F);
        this.setRotateAngle(leg_right_thigh, -0.3491F, 0.0F, 0.0F);
        this.leg_right_shin = new AdvancedModelBox(this, 139, 0);
        this.leg_right_shin.setRotationPoint(0.1000F, 7.0F, 0.0F);
        this.leg_right_shin.addBox(-0.7500F, -1.2000F, -1.1000F, 1.5000F, 9.6000F, 2.0F, 0.0F);
        this.setRotateAngle(leg_right_shin, 1.6930F, 0.0F, 0.0F);
        this.leg_right_foot = new AdvancedModelBox(this, 212, 0);
        this.leg_right_foot.setRotationPoint(0.2000F, 8.4000F, 0.0F);
        this.leg_right_foot.addBox(-0.7500F, -0.6000F, -0.9000F, 1.5000F, 6.6000F, 1.5000F, 0.0F);
        this.setRotateAngle(leg_right_foot, -2.3038F, 0.0F, 0.0F);
        this.leg_right_toe = new AdvancedModelBox(this, 51, 19);
        this.leg_right_toe.setRotationPoint(0.0F, 6.0F, 0.0F);
        this.leg_right_toe.addBox(-0.5500F, -0.8000F, -0.5000F, 1.1000F, 3.8000F, 1.4000F, 0.0F);
        this.setRotateAngle(leg_right_toe, -0.0349F, 0.0F, 0.0F);
        this.tail_1 = new AdvancedModelBox(this, 109, 0);
        this.tail_1.setRotationPoint(0.0F, 1.2000F, 3.0F);
        this.tail_1.addBox(-1.8000F, -1.9000F, -1.6000F, 3.6000F, 3.7000F, 7.6000F, 0.0F);
        this.setRotateAngle(tail_1, -0.3439F, 0.0F, 0.0F);
        this.tail_2 = new AdvancedModelBox(this, 167, 0);
        this.tail_2.setRotationPoint(0.0F, 0.0F, 6.0F);
        this.tail_2.addBox(-1.3000F, -1.2000F, -1.0F, 2.6000F, 2.5000F, 7.0F, 0.0F);
        this.setRotateAngle(tail_2, 0.5251F, 0.0F, 0.0F);
        this.tail_3 = new AdvancedModelBox(this, 218, 0);
        this.tail_3.setRotationPoint(0.0F, 0.0F, 6.0F);
        this.tail_3.addBox(-0.8000F, -0.8000F, -0.8000F, 1.6000F, 1.6000F, 6.3000F, 0.0F);
        this.setRotateAngle(tail_3, 0.3819F, 0.0F, 0.0F);

        this.body_hips.addChild(this.body_chest);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.muzzle);
        this.muzzle.addChild(this.nose);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.ear_right);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.eye_right);
        this.body_chest.addChild(this.arm_left_upper);
        this.arm_left_upper.addChild(this.arm_left_fore);
        this.arm_left_fore.addChild(this.arm_left_hand);
        this.arm_left_hand.addChild(this.claw_left);
        this.body_chest.addChild(this.arm_right_upper);
        this.arm_right_upper.addChild(this.arm_right_fore);
        this.arm_right_fore.addChild(this.arm_right_hand);
        this.arm_right_hand.addChild(this.claw_right);
        this.body_hips.addChild(this.joey_head);
        this.joey_head.addChild(this.joey_snout);
        this.body_hips.addChild(this.leg_left_thigh);
        this.leg_left_thigh.addChild(this.leg_left_shin);
        this.leg_left_shin.addChild(this.leg_left_foot);
        this.leg_left_foot.addChild(this.leg_left_toe);
        this.body_hips.addChild(this.leg_right_thigh);
        this.leg_right_thigh.addChild(this.leg_right_shin);
        this.leg_right_shin.addChild(this.leg_right_foot);
        this.leg_right_foot.addChild(this.leg_right_toe);
        this.body_hips.addChild(this.tail_1);
        this.tail_1.addChild(this.tail_2);
        this.tail_2.addChild(this.tail_3);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_hips);
    }

    // Built once: getAllParts() is called every frame, and a fresh ImmutableList per animal per frame
    // was pure garbage (2.8.0, same as ModelBison).
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
                body_hips,
                body_chest,
                neck,
                head,
                muzzle,
                nose,
                ear_left,
                ear_right,
                eye_left,
                eye_right,
                arm_left_upper,
                arm_left_fore,
                arm_left_hand,
                claw_left,
                arm_right_upper,
                arm_right_fore,
                arm_right_hand,
                claw_right,
                joey_head,
                joey_snout,
                leg_left_thigh,
                leg_left_shin,
                leg_left_foot,
                leg_left_toe,
                leg_right_thigh,
                leg_right_shin,
                leg_right_foot,
                leg_right_toe,
                tail_1,
                tail_2,
                tail_3
        );
        }
        return this.allParts;
    }

    /** Move a part's X angle a fraction t of the way from its rest angle to a solved target. */
    private static void blendX(AdvancedModelBox box, float target, float t) {
        box.rotateAngleX += (target - box.defaultRotationX) * t;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityKangaroo roo = (EntityKangaroo) entityIn;
        animator.update(roo);
        // <gen:idle-poses> 2.8.0: SOLVED by scripts/pose_reach.py against this model's own boxes (muzzle to the grass / the
        // water line beyond the bank, every head part and horn family clear of the ground, nothing through the torso).
        // Regenerate, never hand-edit: python scripts/gen_keyframes.py kangaroo
        // ALERT (40t, AlertGoal): head raised, ears up, staring at the threat; repeats while the alert lasts
        animator.setAnimation(EntityKangaroo.ALERT);
        animator.startKeyframe(8);
        this.rotate(animator, neck, -27.3F, 0.0F, 0.0F);
        this.rotate(animator, head, -16.6F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 12.0F, 0.0F, 0.0F);
        this.rotate(animator, nose, 12.0F, 0.0F, 0.0F);
        this.rotate(animator, ear_left, 25.0F, 0.0F, 0.0F);
        this.rotate(animator, ear_right, 25.0F, 0.0F, 0.0F);
        animator.endKeyframe();
        animator.setStaticKeyframe(24);
        animator.resetKeyframe(8);
        // </gen:idle-poses>

        // ATTACK_KICK (18t) -- the kangaroo's real weapon: rock back onto the tail and strike forward with BOTH hind
        // feet at once. EntityKangaroo throws the target on the follow-through.
        animator.setAnimation(EntityKangaroo.ATTACK_KICK);
        animator.startKeyframe(7);
        this.rotate(animator, body_hips, -22.0F, 0, 0);
        this.rotate(animator, leg_left_thigh, -40.0F, 0, 0);
        this.rotate(animator, leg_right_thigh, -40.0F, 0, 0);
        this.rotate(animator, leg_left_shin, 35.0F, 0, 0);
        this.rotate(animator, leg_right_shin, 35.0F, 0, 0);
        this.rotate(animator, tail_1, 18.0F, 0, 0);
        this.rotate(animator, neck, 12.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, body_hips, -28.0F, 0, 0);
        this.rotate(animator, leg_left_thigh, -78.0F, 0, 0);
        this.rotate(animator, leg_right_thigh, -78.0F, 0, 0);
        this.rotate(animator, leg_left_shin, -5.0F, 0, 0);
        this.rotate(animator, leg_right_shin, -5.0F, 0, 0);
        this.rotate(animator, leg_left_foot, -25.0F, 0, 0);
        this.rotate(animator, leg_right_foot, -25.0F, 0, 0);
        this.rotate(animator, tail_1, 22.0F, 0, 0);
        this.rotate(animator, neck, 16.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(7);

        // ATTACK_BOX (44t) -- cosmetic sparring between males: stand tall on the tail and trade jabs, head back.
        animator.setAnimation(EntityKangaroo.ATTACK_BOX);
        animator.startKeyframe(6);
        this.rotate(animator, body_hips, -20.0F, 0, 0);
        this.rotate(animator, neck, 10.0F, 0, 0);
        this.rotate(animator, arm_left_upper, -55.0F, 0, 0);
        this.rotate(animator, arm_right_upper, -20.0F, 0, 0);
        animator.endKeyframe();
        for (int i = 0; i < 3; i++) {
            animator.startKeyframe(5);
            this.rotate(animator, body_hips, -20.0F, 0, 0);
            this.rotate(animator, neck, 10.0F, -8.0F, 0);
            this.rotate(animator, arm_left_upper, -20.0F, 0, 0);
            this.rotate(animator, arm_left_fore, 10.0F, 0, 0);
            this.rotate(animator, arm_right_upper, -70.0F, 0, 0);
            this.rotate(animator, arm_right_fore, -35.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(5);
            this.rotate(animator, body_hips, -20.0F, 0, 0);
            this.rotate(animator, neck, 10.0F, 8.0F, 0);
            this.rotate(animator, arm_left_upper, -70.0F, 0, 0);
            this.rotate(animator, arm_left_fore, -35.0F, 0, 0);
            this.rotate(animator, arm_right_upper, -20.0F, 0, 0);
            this.rotate(animator, arm_right_fore, 10.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_THREATEN (30t) -- stand up to full height on the toes and tail, arms raised, head high.
        animator.setAnimation(EntityKangaroo.ATTACK_THREATEN);
        animator.startKeyframe(8);
        this.rotate(animator, body_hips, -18.0F, 0, 0);
        this.rotate(animator, neck, 8.0F, 0, 0);
        this.rotate(animator, arm_left_upper, -45.0F, 0, 0);
        this.rotate(animator, arm_right_upper, -45.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(14);
        animator.resetKeyframe(8);
    }

    public void setupAnim(EntityKangaroo roo, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(roo);
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);
        float blend = Math.min(1.0F, limbSwingAmount / 0.3F);
        float gait = limbSwing * 0.55F / Math.max(0.3F, roo.getMobSize() * roo.getScale());

        // ---- per-species: sthenurine short face + long clawed arms ----
        int variant = roo.getVariant();
        boolean sthen = variant == 2 || variant == 3;
        this.muzzle.setScale(1.0F, 1.0F, sthen ? 0.5500F : 1.0F);
        this.claw_left.showModel = sthen;
        this.claw_right.showModel = sthen;
        for (AdvancedModelBox arm : new AdvancedModelBox[]{arm_left_upper, arm_left_fore, arm_left_hand, arm_right_upper, arm_right_fore, arm_right_hand}) {
            arm.setShouldScaleChildren(false);
            arm.setScale(1.0F, sthen ? 1.3500F : 1.0F, 1.0F);
        }
        if (sthen) {
            this.nose.rotationPointZ += 1.5300F;   // follow the shortened muzzle back
            // re-close each arm chain: a child's pivot is an offset in its PARENT's frame, so it takes the parent's stretch
            for (AdvancedModelBox child : new AdvancedModelBox[]{arm_left_fore, arm_left_hand, claw_left, arm_right_fore, arm_right_hand, claw_right}) {
                child.rotationPointY *= 1.3500F;
            }
        }
        this.joey_head.showModel = roo.hasJoey() && roo.sitProgress == 0 && roo.sleepProgress == 0;

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 18) * 0.035F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        walk(neck, 0.25f, 0.03f, false, 2.8F, 0.02F, ageInTicks / 20, 2);
        this.swing(ear_left, 0.22F, 0.20F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.22F, 0.20F, false, 1.3F, 0F, ageInTicks, 1);
        this.swing(tail_3, 0.08F, 0.10F, false, 0F, 0F, ageInTicks, 1);
        this.swing(joey_head, 0.10F, 0.25F, false, 0F, 0F, ageInTicks, 1);

        // Blinking: bury the eye planes inside the skull; y/z keep the constructor's values.
        if (!roo.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(1.4300F, -0.4000F, -1.9000F);
            this.eye_right.setRotationPoint(-1.4300F, -0.4000F, -1.9000F);
        }

        if (!roo.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        if (roo.isInWater() && !roo.isOnGround()) {
            // kangaroos swim with the body level, kicking the hind legs alternately
            this.body_hips.rotateAngleX += 0.55F;
            this.leg_left_thigh.rotateAngleX -= 0.55F;
            this.leg_right_thigh.rotateAngleX -= 0.55F;
            walk(leg_left_thigh, 0.35F, 0.5F, false, 0F, 0F, ageInTicks, 1);
            walk(leg_right_thigh, 0.35F, 0.5F, true, 0F, 0F, ageInTicks, 1);
        }
        else if (roo.canMove() && limbSwingAmount > 0.02F) {
            if (roo.getCurrentSpeed() < SLOW_SPEED) {
                // ---- CRAWL (pentapedal walk): blend toward the solved crawl pose, then swing both hind legs
                // forward together while the forearms and tail carry the weight ----
                this.body_hips.rotationPointY += (16.0582F - this.body_hips.defaultPositionY) * blend;
                blendX(body_hips, -0.1047F, blend);
                blendX(neck, -0.2967F, blend);
                blendX(head, 0.8029F, blend);
                blendX(arm_left_upper, 0.3119F, blend);
                blendX(arm_left_fore, 0.0779F, blend);
                blendX(arm_left_hand, -1.0032F, blend);
                blendX(arm_right_upper, 0.3119F, blend);
                blendX(arm_right_fore, 0.0779F, blend);
                blendX(arm_right_hand, -1.0032F, blend);
                blendX(leg_left_thigh, -1.3976F, blend);
                blendX(leg_left_shin, 2.4143F, blend);
                blendX(leg_left_foot, -2.3332F, blend);
                blendX(leg_left_toe, -0.1685F, blend);
                blendX(leg_right_thigh, -1.3976F, blend);
                blendX(leg_right_shin, 2.4143F, blend);
                blendX(leg_right_foot, -2.3332F, blend);
                blendX(leg_right_toe, -0.1685F, blend);
                blendX(tail_1, -0.2696F, blend);
                blendX(tail_2, -0.1675F, blend);
                blendX(tail_3, 1.0489F, blend);
                float swingLegs = Mth.sin(gait * 0.9F) * 0.45F * blend;
                this.leg_left_thigh.rotateAngleX += swingLegs;
                this.leg_right_thigh.rotateAngleX += swingLegs;
                this.leg_left_foot.rotateAngleX -= swingLegs * 0.6F;
                this.leg_right_foot.rotateAngleX -= swingLegs * 0.6F;
                this.body_hips.rotationPointY -= Math.max(0, Mth.sin(gait * 0.9F)) * 1.2F * blend;
            }
            else {
                // ---- HOP: torso tips forward, legs counter-rotated to stay under it, then both legs swing together:
                // push-off BACK at phase 0, airborne over the first half, landing FORWARD at pi ----
                float ph = gait * 1.1F;
                float pitch = 0.45F * blend;
                this.body_hips.rotateAngleX += pitch;
                this.neck.rotateAngleX -= pitch * 0.6F;
                this.leg_left_thigh.rotateAngleX -= pitch;
                this.leg_right_thigh.rotateAngleX -= pitch;
                float sw = Mth.cos(ph) * blend;
                this.leg_left_thigh.rotateAngleX += sw * 0.65F;
                this.leg_right_thigh.rotateAngleX += sw * 0.65F;
                this.leg_left_shin.rotateAngleX -= sw * 0.45F;
                this.leg_right_shin.rotateAngleX -= sw * 0.45F;
                this.leg_left_foot.rotateAngleX += sw * 0.55F;
                this.leg_right_foot.rotateAngleX += sw * 0.55F;
                this.leg_left_toe.rotateAngleX += sw * 0.30F;
                this.leg_right_toe.rotateAngleX += sw * 0.30F;
                this.tail_1.rotateAngleX += sw * 0.20F;
                this.tail_2.rotateAngleX += sw * 0.15F;
                this.arm_left_upper.rotateAngleX -= 0.25F * blend;
                this.arm_right_upper.rotateAngleX -= 0.25F * blend;
                this.body_hips.rotationPointY -= Math.max(0, Mth.sin(ph)) * 3.0F * blend;
            }
        }

        if (roo.sitProgress > 0) {
            // SOLVED (kangaroo_spec.py --pose): every box above y=0, feet and tail resting ON the floor
            this.progressPosition(body_hips, roo.sitProgress, 0.0F, 15.6678F, 0.0F, 40);
            this.progressRotation(body_hips, roo.sitProgress, -0.7854F, 0, 0, 40);
            this.progressRotation(neck, roo.sitProgress, -0.1745F, 0, 0, 40);
            this.progressRotation(head, roo.sitProgress, 1.3090F, 0, 0, 40);
            this.progressRotation(arm_left_upper, roo.sitProgress, 0.4371F, 0, 0, 40);
            this.progressRotation(arm_left_fore, roo.sitProgress, -0.2357F, 0, 0, 40);
            this.progressRotation(arm_left_hand, roo.sitProgress, -0.5125F, 0, 0, 40);
            this.progressRotation(arm_right_upper, roo.sitProgress, 0.4371F, 0, 0, 40);
            this.progressRotation(arm_right_fore, roo.sitProgress, -0.2357F, 0, 0, 40);
            this.progressRotation(arm_right_hand, roo.sitProgress, -0.5125F, 0, 0, 40);
            this.progressRotation(leg_left_thigh, roo.sitProgress, -0.5000F, 0, 0, 40);
            this.progressRotation(leg_left_shin, roo.sitProgress, 2.1309F, 0, 0, 40);
            this.progressRotation(leg_left_foot, roo.sitProgress, -2.4622F, 0, 0, 40);
            this.progressRotation(leg_left_toe, roo.sitProgress, 0.0843F, 0, 0, 40);
            this.progressRotation(leg_right_thigh, roo.sitProgress, -0.5000F, 0, 0, 40);
            this.progressRotation(leg_right_shin, roo.sitProgress, 2.1309F, 0, 0, 40);
            this.progressRotation(leg_right_foot, roo.sitProgress, -2.4622F, 0, 0, 40);
            this.progressRotation(leg_right_toe, roo.sitProgress, 0.0843F, 0, 0, 40);
            this.progressRotation(tail_1, roo.sitProgress, 0.2695F, 0, 0, 40);
            this.progressRotation(tail_2, roo.sitProgress, 0.3690F, 0, 0, 40);
            this.progressRotation(tail_3, roo.sitProgress, 0.0315F, 0, 0, 40);
        }
        else if (roo.sleepProgress > 0) {
            // SOLVED (kangaroo_spec.py --pose): every box above y=0, feet and tail resting ON the floor, paws on it
            this.progressPosition(body_hips, roo.sleepProgress, 0.0F, 18.8057F, 0.0F, 40);
            this.progressRotation(body_hips, roo.sleepProgress, -0.1396F, 0, 0, 40);
            this.progressRotation(neck, roo.sleepProgress, 0.0175F, 0, 0, 40);
            this.progressRotation(head, roo.sleepProgress, 0.5934F, 0, 0, 40);
            this.progressRotation(arm_left_upper, roo.sleepProgress, 0.0283F, 0, 0, 40);
            this.progressRotation(arm_left_fore, roo.sleepProgress, -0.5050F, 0, 0, 40);
            this.progressRotation(arm_left_hand, roo.sleepProgress, -0.9340F, 0, 0, 40);
            this.progressRotation(arm_right_upper, roo.sleepProgress, 0.0283F, 0, 0, 40);
            this.progressRotation(arm_right_fore, roo.sleepProgress, -0.5050F, 0, 0, 40);
            this.progressRotation(arm_right_hand, roo.sleepProgress, -0.9340F, 0, 0, 40);
            this.progressRotation(leg_left_thigh, roo.sleepProgress, -1.2533F, 0, 0, 40);
            this.progressRotation(leg_left_shin, roo.sleepProgress, 2.6762F, 0, 0, 40);
            this.progressRotation(leg_left_foot, roo.sleepProgress, -2.8332F, 0, 0, 40);
            this.progressRotation(leg_left_toe, roo.sleepProgress, -0.1091F, 0, 0, 40);
            this.progressRotation(leg_right_thigh, roo.sleepProgress, -1.2533F, 0, 0, 40);
            this.progressRotation(leg_right_shin, roo.sleepProgress, 2.6762F, 0, 0, 40);
            this.progressRotation(leg_right_foot, roo.sleepProgress, -2.8332F, 0, 0, 40);
            this.progressRotation(leg_right_toe, roo.sleepProgress, -0.1091F, 0, 0, 40);
            this.progressRotation(tail_1, roo.sleepProgress, 0.1277F, 0, 0, 40);
            this.progressRotation(tail_2, roo.sleepProgress, -0.0913F, 0, 0, 40);
            this.progressRotation(tail_3, roo.sleepProgress, -0.2710F, 0, 0, 40);
        }
    }
}
