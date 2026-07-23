package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityEquid;

public class ModelEquid extends AdvancedEntityModel<EntityEquid> {

    private final AdvancedModelBox body_barrel;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox mane;
    private final AdvancedModelBox head;
    private final AdvancedModelBox muzzle;
    private final AdvancedModelBox forelock;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
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
    private final AdvancedModelBox tail_hair_1;
    private final AdvancedModelBox tail_hair_2;

    private final ModelAnimator animator;

    public ModelEquid() {
        this.texWidth = 256;
        this.texHeight = 128;

        // Full Blockbench rebuild (31 boxes, 256x128). The old model was a BISON FORK -- 24 boxes on
        // 128x64 that still carried head_horn_left/right, head_beard and body_hair, with the horns
        // scaled to zero at runtime to hide them. None of that geometry survives.
        // Anatomy -- a WILD horse, not a riding horse: deep chest and level back with the withers just
        // above the croup; a neck rising forward-and-up 40 deg carrying an ERECT crest mane (tarpan and
        // Przewalski-type equids have a stiff upright mane, and all three species here are wild); a long
        // head tapering to a narrow muzzle; and FOUR-segment legs -- shoulder/forearm/cannon/hoof in
        // front, thigh/gaskin/cannon/hoof behind with the hock angled so the hind leg makes the horse's
        // characteristic zigzag. The thin cannons are what read as "horse" rather than as one more
        // pillar-legged herbivore.
        // Outer faces step monotonically inboard down each foreleg (barrel 8.0 > chest 7.5 > shoulder
        // 7.0 > forearm 6.5 > cannon 6.0) so no two same-facing surfaces ever share a plane; the hind
        // thigh deliberately bulges 0.8 outside the barrel, as a horse's hindquarters do. A coplanar
        // sweep over all 465 box pairs reports one hit -- neck against muzzle, both at |x| 3.0 -- and
        // sampling that shared patch shows it is 100% enclosed by the head, which is wider (4.0) than
        // either, so nothing renders there and it cannot shimmer. No other pair comes within 0.2u.
        // All coords/rotations/texOffs transcribed VERBATIM from the modded_entity export.
        this.body_barrel = new AdvancedModelBox(this, 0, 0);
        this.body_barrel.setRotationPoint(0.0F, 2.0F, 0.0F);
        this.body_barrel.addBox(-8.0F, -5.5F, -11.0F, 16.0F, 12.0F, 22.0F, 0.0F);

        this.body_chest = new AdvancedModelBox(this, 76, 0);
        this.body_chest.setRotationPoint(0.0F, -2.0F, -9.0F);
        this.body_chest.addBox(-7.5F, -4.5F, -8.0F, 15.0F, 13.5F, 10.0F, 0.0F);
        this.setRotateAngle(body_chest, 0.0873F, 0.0F, 0.0F);

        // 6 WIDE, not 9. A horse's neck is narrow laterally and deep dorsoventrally, and it must come
        // out NARROWER than the skull is across the cheeks (8) -- at 9 it was wider than the head, which
        // is what read as fat. It also sat 0.07 units off the eye planes, so the neck's own cheek
        // rendered in front of the eyes and clipped them; at 6 the eyes (|x| 4.3) clear it by 1.3.
        this.neck = new AdvancedModelBox(this, 176, 0);
        this.neck.setRotationPoint(0.0F, -2.0F, -3.0F);
        this.neck.addBox(-3.0F, -13.0F, -5.0F, 6.0F, 14.0F, 8.0F, 0.0F);
        this.setRotateAngle(neck, 0.6981F, 0.0F, 0.0F);

        // The crest rides the neck's LOCAL +Z face, not its local top: the neck leans 40 deg, which
        // makes local +Z the dorsal side. It runs the full withers-to-poll length and is buried 1u so
        // no face of it is ever coplanar with the neck.
        this.mane = new AdvancedModelBox(this, 116, 34);
        this.mane.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.mane.addBox(-2.0F, -14.0F, 2.0F, 4.0F, 14.0F, 4.5F, 0.0F);

        // 9 deep, not 10. Nose-to-poll is 14, so 9 gives the ~1.6:1 horse head; at 10 the jaw hung
        // 2.5 below the muzzle and that exposed front face read as a squared-off box under the nose.
        // The unit came off the BOTTOM (the box still starts at y -2), so every UV rect keeps its
        // origin and simply loses its last row -- no texture rework.
        this.head = new AdvancedModelBox(this, 82, 34);
        this.head.setRotationPoint(0.0F, -12.0F, -2.0F);
        this.head.addBox(-4.0F, -2.0F, -6.0F, 8.0F, 9.0F, 9.0F, 0.0F);
        this.setRotateAngle(head, -0.384F, 0.0F, 0.0F);

        // RAISED 3.5 (rotation point y 5.0 -> 1.5) and de-rotated (was -0.1047). A horse's
        // poll -> forehead -> nasal bridge is one straight line: the skull's dorsal slope and the
        // nasal bridge now both run at -23 deg, break 0.00 deg, with the muzzle buried exactly 1.0
        // below the skull's topline instead of stepping 4.5 off it. The head loses depth off its
        // UNDERSIDE -- deep jowl at the back, shallow nose at the front -- not off the topline.
        // Dimensions are untouched, so the painted nostrils and mouth carry over as they are.
        this.muzzle = new AdvancedModelBox(this, 134, 34);
        this.muzzle.setRotationPoint(0.0F, 1.5F, -4.0F);
        this.muzzle.addBox(-3.0F, -2.5F, -7.0F, 6.0F, 6.5F, 8.0F, 0.0F);

        this.forelock = new AdvancedModelBox(this, 108, 55);
        this.forelock.setRotationPoint(0.0F, -1.5F, -2.0F);
        this.forelock.addBox(-2.5F, -2.5F, -3.0F, 5.0F, 3.5F, 5.0F, 0.0F);
        this.setRotateAngle(forelock, 0.1396F, 0.0F, 0.0F);

        this.ear_left = new AdvancedModelBox(this, 196, 55);
        this.ear_left.setRotationPoint(2.6F, -1.5F, 1.0F);
        this.ear_left.addBox(-1.2F, -3.5F, -1.2F, 2.4F, 4.0F, 2.4F, 0.0F);
        this.setRotateAngle(ear_left, 0.0F, -0.1745F, -0.1745F);

        this.ear_right = new AdvancedModelBox(this, 208, 55);
        this.ear_right.setRotationPoint(-2.6F, -1.5F, 1.0F);
        this.ear_right.addBox(-1.2F, -3.5F, -1.2F, 2.4F, 4.0F, 2.4F, 0.0F);
        this.setRotateAngle(ear_right, 0.0F, 0.1745F, 0.1745F);

        // Eye planes sit 0.3 clear of the cheek (|x| 4.0) so they cannot z-fight it, and are 3x3 --
        // at 2px there is no room for a pupil to read. They no longer have to be jammed forward to
        // escape the neck: at |x| 4.3 against the neck's 3.0 they cannot be occluded in ANY pose,
        // because every head animation is a pure X rotation and so never changes their x. That frees
        // them to sit where a horse's orbit actually is -- high on the skull, about a third of the
        // head's length back from the poll, over the front of the cheek.
        this.eye_left = new AdvancedModelBox(this, 220, 55);
        this.eye_left.setRotationPoint(4.3F, 1.0F, -2.0F);
        this.eye_left.addBox(0.0F, -1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 0.0F);

        this.eye_right = new AdvancedModelBox(this, 226, 55);
        this.eye_right.setRotationPoint(-4.3F, 1.0F, -2.0F);
        this.eye_right.addBox(0.0F, -1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 0.0F);

        this.arm_left_1 = new AdvancedModelBox(this, 30, 34);
        this.arm_left_1.setRotationPoint(5.0F, -1.0F, -1.0F);
        this.arm_left_1.addBox(-2.5F, -0.5F, -4.0F, 4.5F, 11.5F, 7.5F, 0.0F);
        this.setRotateAngle(arm_left_1, -0.0524F, 0.0F, -0.0349F);

        this.arm_left_2 = new AdvancedModelBox(this, 232, 34);
        this.arm_left_2.setRotationPoint(0.0F, 9.5F, 0.0F);
        this.arm_left_2.addBox(-2.2F, -0.5F, -3.0F, 3.7F, 7.5F, 6.0F, 0.0F);
        this.setRotateAngle(arm_left_2, 0.0524F, 0.0F, 0.0F);

        this.arm_left_3 = new AdvancedModelBox(this, 38, 55);
        this.arm_left_3.setRotationPoint(0.0F, 7.0F, 0.0F);
        this.arm_left_3.addBox(-1.0F, -0.5F, -2.0F, 2.0F, 6.2F, 4.0F, 0.0F);

        this.hoof_front_left = new AdvancedModelBox(this, 164, 55);
        this.hoof_front_left.setRotationPoint(0.0F, 5.7F, 0.0F);
        this.hoof_front_left.addBox(-1.4F, -0.4F, -2.4F, 2.8F, 2.92F, 4.8F, 0.0F);

        this.arm_right_1 = new AdvancedModelBox(this, 56, 34);
        this.arm_right_1.setRotationPoint(-5.0F, -1.0F, -1.0F);
        this.arm_right_1.addBox(-2.0F, -0.5F, -4.0F, 4.5F, 11.5F, 7.5F, 0.0F);
        this.setRotateAngle(arm_right_1, -0.0524F, 0.0F, 0.0349F);

        this.arm_right_2 = new AdvancedModelBox(this, 0, 55);
        this.arm_right_2.setRotationPoint(0.0F, 9.5F, 0.0F);
        this.arm_right_2.addBox(-1.5F, -0.5F, -3.0F, 3.7F, 7.5F, 6.0F, 0.0F);
        this.setRotateAngle(arm_right_2, 0.0524F, 0.0F, 0.0F);

        this.arm_right_3 = new AdvancedModelBox(this, 50, 55);
        this.arm_right_3.setRotationPoint(0.0F, 7.0F, 0.0F);
        this.arm_right_3.addBox(-1.0F, -0.5F, -2.0F, 2.0F, 6.2F, 4.0F, 0.0F);

        this.hoof_front_right = new AdvancedModelBox(this, 180, 55);
        this.hoof_front_right.setRotationPoint(0.0F, 5.7F, 0.0F);
        this.hoof_front_right.addBox(-1.4F, -0.4F, -2.4F, 2.8F, 2.92F, 4.8F, 0.0F);

        this.body_croup = new AdvancedModelBox(this, 126, 0);
        this.body_croup.setRotationPoint(0.0F, -3.0F, 7.0F);
        this.body_croup.addBox(-7.5F, -2.5F, -1.0F, 15.0F, 11.5F, 10.0F, 0.0F);
        this.setRotateAngle(body_croup, -0.192F, 0.0F, 0.0F);

        // HIND STANCE -- rotations only, no box was moved or resized. The old angles (thigh -10, gaskin
        // +20, cannon -12) stacked on top of the croup's own -11 tilt, so the whole limb leaned 21 deg
        // forward in world space and planted the hind hoof at z 2.1 -- 5.9 units AHEAD of the hip, under
        // the belly, as if the horse were permanently camped under itself. Re-solved by forward kinematics
        // against the real boxes: world angles are now thigh -14 / gaskin +15 / cannon +1, which puts the
        // hock (z 7.00) a clear 1.7 behind the stifle (z 5.32) for the horse zigzag, drops the cannon to
        // vertical, and lands the hoof at z 6.6 -- directly under the hip at z 7.98. Ground contact is
        // preserved exactly: the lowest corner of the hind hoof sits at y 24.00, matching the fore hoof.
        this.leg_left_1 = new AdvancedModelBox(this, 210, 0);
        this.leg_left_1.setRotationPoint(5.5F, 0.0F, 1.0F);
        this.leg_left_1.addBox(-2.5F, -1.0F, -5.0F, 5.8F, 12.0F, 9.0F, 0.0F);
        this.setRotateAngle(leg_left_1, -0.0524F, 0.0F, -0.0349F);

        this.leg_left_2 = new AdvancedModelBox(this, 184, 34);
        this.leg_left_2.setRotationPoint(0.0F, 11.0F, 0.0F);
        this.leg_left_2.addBox(-2.1F, -0.5F, -4.0F, 4.2F, 7.0F, 7.0F, 0.0F);
        this.setRotateAngle(leg_left_2, 0.5061F, 0.0F, 0.0F);

        this.leg_left_3 = new AdvancedModelBox(this, 80, 55);
        this.leg_left_3.setRotationPoint(0.0F, 6.5F, 0.0F);
        this.leg_left_3.addBox(-1.1F, -0.5F, -2.5F, 2.2F, 5.2F, 4.0F, 0.0F);
        this.setRotateAngle(leg_left_3, -0.2443F, 0.0F, 0.0F);

        this.hoof_back_left = new AdvancedModelBox(this, 128, 55);
        this.hoof_back_left.setRotationPoint(0.0F, 4.7F, 0.0F);
        this.hoof_back_left.addBox(-1.7F, -0.4F, -2.9F, 3.4F, 3.46F, 4.8F, 0.0F);

        this.leg_right_1 = new AdvancedModelBox(this, 0, 34);
        this.leg_right_1.setRotationPoint(-5.5F, 0.0F, 1.0F);
        this.leg_right_1.addBox(-3.3F, -1.0F, -5.0F, 5.8F, 12.0F, 9.0F, 0.0F);
        this.setRotateAngle(leg_right_1, -0.0524F, 0.0F, 0.0349F);

        this.leg_right_2 = new AdvancedModelBox(this, 208, 34);
        this.leg_right_2.setRotationPoint(0.0F, 11.0F, 0.0F);
        this.leg_right_2.addBox(-2.1F, -0.5F, -4.0F, 4.2F, 7.0F, 7.0F, 0.0F);
        this.setRotateAngle(leg_right_2, 0.5061F, 0.0F, 0.0F);

        this.leg_right_3 = new AdvancedModelBox(this, 94, 55);
        this.leg_right_3.setRotationPoint(0.0F, 6.5F, 0.0F);
        this.leg_right_3.addBox(-1.1F, -0.5F, -2.5F, 2.2F, 5.2F, 4.0F, 0.0F);
        this.setRotateAngle(leg_right_3, -0.2443F, 0.0F, 0.0F);

        this.hoof_back_right = new AdvancedModelBox(this, 146, 55);
        this.hoof_back_right.setRotationPoint(0.0F, 4.7F, 0.0F);
        this.hoof_back_right.addBox(-1.7F, -0.4F, -2.9F, 3.4F, 3.46F, 4.8F, 0.0F);

        // TAIL -- rotations only, no box was moved or resized. Every segment used to carry a NEGATIVE X
        // rotation, and negative X swings a part's distal end FORWARD; stacked on the croup's -11 that
        // put the dock at -39, the hair at -55 and the tip at -65 world, so the tail swept forward and
        // ended up hanging under the barrel (its z reached 1.0, i.e. mid-belly) instead of behind the
        // horse. Now the three segments read +1 / +3 / +3 in world space: the tail falls straight down
        // off the point of the croup with a 3 deg rearward lean, spanning z 13.5-21.5 (entirely behind
        // the rump, whose rear face is at z 14.1-16.3) and ending at y 17.8 -- just below the hocks at
        // y 16, which is where a horse's tail actually reaches. Still 6.2 units clear of the ground.
        this.tail_dock = new AdvancedModelBox(this, 62, 55);
        this.tail_dock.setRotationPoint(0.0F, -1.0F, 7.0F);
        this.tail_dock.addBox(-2.0F, -1.0F, -0.5F, 4.0F, 4.5F, 4.5F, 0.0F);
        this.setRotateAngle(tail_dock, 0.2094F, 0.0F, 0.0F);

        this.tail_hair_1 = new AdvancedModelBox(this, 162, 34);
        this.tail_hair_1.setRotationPoint(0.0F, 2.0F, 3.0F);
        this.tail_hair_1.addBox(-2.8F, -0.5F, -1.0F, 5.6F, 9.5F, 4.5F, 0.0F);
        this.setRotateAngle(tail_hair_1, 0.0349F, 0.0F, 0.0F);

        this.tail_hair_2 = new AdvancedModelBox(this, 20, 55);
        this.tail_hair_2.setRotationPoint(0.0F, 8.0F, 1.0F);
        this.tail_hair_2.addBox(-2.4F, -0.5F, -1.0F, 4.8F, 9.0F, 3.5F, 0.0F);
        this.setRotateAngle(tail_hair_2, 0.0F, 0.0F, 0.0F);

        this.body_barrel.addChild(this.body_chest);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.mane);
        this.neck.addChild(this.head);
        this.head.addChild(this.muzzle);
        this.head.addChild(this.forelock);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.ear_right);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.eye_right);
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
        this.tail_dock.addChild(this.tail_hair_1);
        this.tail_hair_1.addChild(this.tail_hair_2);

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
            body_barrel, body_chest, body_croup,
            neck, mane, head, muzzle, forelock, ear_left, ear_right, eye_left, eye_right,
            arm_left_1, arm_left_2, arm_left_3, hoof_front_left,
            arm_right_1, arm_right_2, arm_right_3, hoof_front_right,
            leg_left_1, leg_left_2, leg_left_3, hoof_back_left,
            leg_right_1, leg_right_2, leg_right_3, hoof_back_right,
            tail_dock, tail_hair_1, tail_hair_2
        );
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityEquid equid = (EntityEquid) entityIn;
        animator.update(equid);

        // Threat: REARING. This is what a horse actually does when it squares up -- rock back onto the
        // hindquarters, haul both forelegs up and paw the air, ears pinned back. Negative X on the
        // barrel pitches the front of the body up; negative X on a limb swings its distal end forward.
        animator.setAnimation(EntityEquid.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_barrel, -34F, 0, 0);
            animator.move(body_barrel, 0, -3.5F, 0);
            this.rotate(animator, neck, 12F, 0, 0);
            this.rotate(animator, head, 10F, 0, 0);
            this.rotate(animator, ear_left, 0, 0, 30F);
            this.rotate(animator, ear_right, 0, 0, -30F);
            this.rotate(animator, arm_left_1, -62F, 0, 0);
            this.rotate(animator, arm_left_2, 74F, 0, 0);
            this.rotate(animator, arm_right_1, -40F, 0, 0);
            this.rotate(animator, arm_right_2, 58F, 0, 0);
            this.rotate(animator, leg_left_1, 16F, 0, 0);
            this.rotate(animator, leg_right_1, 16F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_barrel, -26F, 0, 0);
            animator.move(body_barrel, 0, -2.5F, 0);
            this.rotate(animator, neck, 8F, 0, 0);
            this.rotate(animator, head, 6F, 0, 0);
            this.rotate(animator, ear_left, 0, 0, 30F);
            this.rotate(animator, ear_right, 0, 0, -30F);
            this.rotate(animator, arm_left_1, -34F, 0, 0);
            this.rotate(animator, arm_left_2, 52F, 0, 0);
            this.rotate(animator, arm_right_1, -66F, 0, 0);
            this.rotate(animator, arm_right_2, 78F, 0, 0);
            this.rotate(animator, leg_left_1, 12F, 0, 0);
            this.rotate(animator, leg_right_1, 12F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // Gore: a striking bite. The neck coils back, then the whole head-and-neck snaps forward and
        // down -- horses fight by biting far more often than by butting.
        animator.setAnimation(EntityEquid.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck, 22F, 0, 0);
        this.rotate(animator, head, 14F, 0, 0);
        this.rotate(animator, ear_left, 0, 0, 26F);
        this.rotate(animator, ear_right, 0, 0, -26F);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, -30F, 0, 0);
        this.rotate(animator, head, -26F, 0, 0);
        this.rotate(animator, muzzle, -16F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityEquid equid, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(equid);
        float globalSpeed = 1.4f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // Per-species BUILD. equid.json carries no flags, so one mesh serves all three; the only lever
        // is the torso, and scaling it does NOT reach the legs (Citadel leaves shouldScaleChildren off),
        // which is exactly what is wanted -- a heavier body on unchanged legs reads as stocky.
        // Hippidion was a short-legged, heavy-bodied South American equid with famously enlarged nasal
        // bones; the tarpan was a light, compact wild horse. Written on EVERY branch every frame,
        // because the model instance is shared across all equids.
        int variant = equid.getVariant();
        float girth = variant == 1 ? 1.08F : (variant == 2 ? 0.97F : 1.0F);
        float depth = variant == 1 ? 1.05F : 1.0F;
        this.body_chest.setScale(girth, depth, 1.0F);
        this.body_croup.setScale(girth, depth, 1.0F);
        this.muzzle.setScale(variant == 1 ? 1.12F : 1.0F, 1.0F, variant == 1 ? 1.1F : 1.0F);

        // Breathing, layered on top of the per-species girth rather than replacing it.
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.05F);
        this.body_barrel.setScale(girth + breath, depth + breath, 1.0F);
        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        walk(neck, 0.35f * globalSpeed, 0.035f, false, 2.8F, 0.05F, ageInTicks / 20, 2);
        this.swing(tail_dock, 0.09F, 0.10F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_hair_1, 0.09F, 0.15F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail_hair_2, 0.09F, 0.20F, false, 2F, 0F, ageInTicks, 1);

        // Blinking: bury the eye planes inside the skull (|x| 4.3 -> 3.2). y/z MUST track the
        // constructor's rotation point or the eye jumps across the face on every blink.
        if (!equid.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(3.2F, 1.0F, -2.0F);
            this.eye_right.setRotationPoint(-3.2F, 1.0F, -2.0F);
        }

        // Head Tracking. A horse has a long, very mobile neck, so it turns freely.
        if (!equid.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 2, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        // Pitch/Yaw handler
        if (equid.isInWater()) {
            this.setRotateAngle(head, -0.18203784098300857F, 0.0F, 0.0F);
            if (!equid.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(equid.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // Walk. Four-beat diagonal gait with a long stride -- a horse swings from the shoulder and hip,
        // and the joints below trail it. Each segment gets a progressively later phase offset and a
        // smaller amplitude, so the knee and hock flex out of the stride instead of the leg staying a
        // rigid stick. The neck nods with the beat; the crest mane is stiff and deliberately does not.
        if (equid.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.5f * globalDegree, true, f, limbSwingAmount);
            walk(neck, 0.8f * globalSpeed, 0.18f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head, 0.8f * globalSpeed, 0.14f * globalDegree, true, 0, 0, f, limbSwingAmount);

            walk(arm_left_1, -0.8f * globalSpeed, 1.15f * globalDegree, true, 2F, 0.6f, f, limbSwingAmount);
            walk(arm_left_2, -0.8f * globalSpeed, 0.80f * globalDegree, false, 1.2F, 0.6f, f, limbSwingAmount * 1.2f);
            walk(arm_left_3, -0.8f * globalSpeed, 0.40f * globalDegree, false, 0.6F, 0.2f, f, limbSwingAmount * 1.2f);
            walk(arm_right_1, -0.8f * globalSpeed, 1.15f * globalDegree, true, 0F, 0.6f, f, limbSwingAmount);
            walk(arm_right_2, -0.8f * globalSpeed, 0.80f * globalDegree, false, -0.8F, 0.6f, f, limbSwingAmount * 1.2f);
            walk(arm_right_3, -0.8f * globalSpeed, 0.40f * globalDegree, false, -1.4F, 0.2f, f, limbSwingAmount * 1.2f);

            walk(leg_left_1, 0.8f * globalSpeed, 1.05f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(leg_left_2, 0.8f * globalSpeed, 0.75f * globalDegree, true, 0.2F, 0, f, limbSwingAmount * 1.2f);
            walk(leg_left_3, 0.8f * globalSpeed, 0.35f * globalDegree, false, -0.4F, 0, f, limbSwingAmount * 1.2f);
            walk(leg_right_1, 0.8f * globalSpeed, 1.05f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(leg_right_2, 0.8f * globalSpeed, 0.75f * globalDegree, true, 2.2F, 0, f, limbSwingAmount * 1.2f);
            walk(leg_right_3, 0.8f * globalSpeed, 0.35f * globalDegree, false, 1.6F, 0, f, limbSwingAmount * 1.2f);

            swing(tail_dock, 0.8f * globalSpeed, 0.25f * globalDegree, false, 0F, 0, f, limbSwingAmount);
            swing(tail_hair_1, 0.8f * globalSpeed, 0.32f * globalDegree, false, -1F, 0, f, limbSwingAmount);
            swing(tail_hair_2, 0.8f * globalSpeed, 0.40f * globalDegree, false, -2F, 0, f, limbSwingAmount);
        }

        if (equid.sitProgress > 0) {
            applyRestingPose(equid.sitProgress);
        }
        else if (equid.sleepProgress > 0) {
            applyRestingPose(equid.sleepProgress);
        }
    }

    /**
     * Sternal recumbency -- how a horse actually lies down. It folds onto its brisket rather than its
     * side: the barrel sinks 12 units, each foreleg folds at the knee with the cannon tucked back
     * underneath, and the hind legs fold FORWARD and splay 26 degrees outward so they lie alongside the
     * barrel instead of colliding with the forelegs (which is what they do if kept tucked under -- the
     * two cannons overlapped by 1.6 units before the splay was added). The neck stays up: horses rest
     * with the head held clear of the ground.
     * <p>
     * Solved by grid search against the real box geometry, not by eye. In the final pose the lowest
     * point of the whole model is the left gaskin at -0.14, and the tail is swung forward to -40 at the
     * dock because leaving it in its standing hang would put the hair 2.1 units through the floor.
     * <p>
     * Every target below is an ABSOLUTE angle -- {@code progressRotation}/{@code progressPosition} both
     * resolve to the literal passed in once progress reaches maxProgress, and {@code ticksToSit} and the
     * sleep counter are both 40 -- so this pose is unaffected by the standing defaults in the ctor.
     */
    private void applyRestingPose(float progress) {
        this.progressPosition(body_barrel, progress, 0.0F, 14.0F, 0.0F, 40);

        this.progressRotation(arm_left_1, progress, (float) Math.toRadians(-42F), 0, (float) Math.toRadians(-2F), 40);
        this.progressRotation(arm_left_2, progress, (float) Math.toRadians(106F), 0, 0, 40);
        this.progressRotation(arm_left_3, progress, (float) Math.toRadians(20F), 0, 0, 40);
        this.progressRotation(arm_right_1, progress, (float) Math.toRadians(-42F), 0, (float) Math.toRadians(2F), 40);
        this.progressRotation(arm_right_2, progress, (float) Math.toRadians(106F), 0, 0, 40);
        this.progressRotation(arm_right_3, progress, (float) Math.toRadians(20F), 0, 0, 40);

        this.progressRotation(leg_left_1, progress, (float) Math.toRadians(-9F), 0, (float) Math.toRadians(-26F), 40);
        this.progressRotation(leg_left_2, progress, (float) Math.toRadians(-68F), 0, 0, 40);
        this.progressRotation(leg_left_3, progress, (float) Math.toRadians(6F), 0, 0, 40);
        this.progressRotation(leg_right_1, progress, (float) Math.toRadians(-9F), 0, (float) Math.toRadians(26F), 40);
        this.progressRotation(leg_right_2, progress, (float) Math.toRadians(-68F), 0, 0, 40);
        this.progressRotation(leg_right_3, progress, (float) Math.toRadians(6F), 0, 0, 40);

        this.progressRotation(tail_dock, progress, (float) Math.toRadians(-40F), 0, 0, 40);
        this.progressRotation(tail_hair_1, progress, (float) Math.toRadians(-20F), 0, 0, 40);
        this.progressRotation(tail_hair_2, progress, 0, 0, 0, 40);

        this.progressRotation(neck, progress, (float) Math.toRadians(40F), 0, 0, 40);
        this.progressRotation(head, progress, (float) Math.toRadians(-22F), 0, 0, 40);
    }
}
