package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityGlyptodont;

public class ModelGlyptodont extends AdvancedEntityModel<EntityGlyptodont> {

    private final AdvancedModelBox body_core;
    private final AdvancedModelBox shell_crown;
    private final AdvancedModelBox shell_front;
    private final AdvancedModelBox shell_rear;
    private final AdvancedModelBox shell_flank_left;
    private final AdvancedModelBox shell_flank_right;
    private final AdvancedModelBox head_neck;
    private final AdvancedModelBox head_main;
    private final AdvancedModelBox head_cap;
    private final AdvancedModelBox snout;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox arm_left_1;
    private final AdvancedModelBox arm_left_2;
    private final AdvancedModelBox arm_right_1;
    private final AdvancedModelBox arm_right_2;
    private final AdvancedModelBox leg_left_1;
    private final AdvancedModelBox leg_left_2;
    private final AdvancedModelBox leg_right_1;
    private final AdvancedModelBox leg_right_2;
    private final AdvancedModelBox tail_1;
    private final AdvancedModelBox tail_2;
    private final AdvancedModelBox tail_3;
    private final AdvancedModelBox tail_club;
    private final AdvancedModelBox club_spike_left;
    private final AdvancedModelBox club_spike_right;
    private final AdvancedModelBox club_spike_top;
    private final AdvancedModelBox club_spike_rear;

    private final ModelAnimator animator;

    public ModelGlyptodont() {
        this.texWidth = 256;
        this.texHeight = 256;

        // Full Blockbench rebuild (28 boxes, 256x256 -- the old model was 23 boxes on 256x128 and the
        // carapace needed the extra texture space). The shell is ONE core box with five angled panels
        // NESTED into it rather than bolted on top: a flat crown, front and rear rims at +/-26 deg
        // giving the overhanging lip a glyptodont carapace actually has, and two flanks rolled inward
        // at the bottom on Z (+/-0.2793 rad). That inward roll is what turns a box into a dome -- without
        // it the shell reads as a crate.
        // Anatomy: the carapace IS the animal; a small head pokes out under the shell front carrying a
        // bony cephalic shield; legs are short two-segment pillars with almost no bend, mostly hidden
        // under the shell rim; the tail is sheathed in armoured rings.
        // All coords/rotations/texOffs transcribed VERBATIM from the modded_entity export.
        this.body_core = new AdvancedModelBox(this, 0, 0);
        this.body_core.setRotationPoint(0.0F, 4.0F, -2.0F);
        this.body_core.addBox(-12.5F, -6.0F, -14.0F, 25.0F, 15.0F, 29.0F, 0.0F);

        this.shell_crown = new AdvancedModelBox(this, 0, 44);
        this.shell_crown.setRotationPoint(0.0F, -5.0F, -1.0F);
        this.shell_crown.addBox(-11.0F, -4.0F, -10.0F, 22.0F, 5.0F, 22.0F, 0.0F);

        this.shell_front = new AdvancedModelBox(this, 88, 44);
        this.shell_front.setRotationPoint(0.0F, -5.0F, -11.0F);
        this.shell_front.addBox(-11.5F, -2.0F, -8.0F, 23.0F, 10.0F, 10.0F, 0.0F);
        this.setRotateAngle(shell_front, 0.4538F, 0.0F, 0.0F);

        this.shell_rear = new AdvancedModelBox(this, 154, 44);
        this.shell_rear.setRotationPoint(0.0F, -5.0F, 10.0F);
        this.shell_rear.addBox(-11.5F, -2.0F, -1.0F, 23.0F, 10.0F, 9.0F, 0.0F);
        this.setRotateAngle(shell_rear, -0.4538F, 0.0F, 0.0F);

        this.shell_flank_left = new AdvancedModelBox(this, 108, 0);
        this.shell_flank_left.setRotationPoint(12.0F, -4.0F, 0.0F);
        this.shell_flank_left.addBox(-1.0F, -1.0F, -13.0F, 3.0F, 13.0F, 27.0F, 0.0F);
        this.setRotateAngle(shell_flank_left, 0.0F, 0.0F, 0.2793F);

        this.shell_flank_right = new AdvancedModelBox(this, 168, 0);
        this.shell_flank_right.setRotationPoint(-12.0F, -4.0F, 0.0F);
        this.shell_flank_right.addBox(-2.0F, -1.0F, -13.0F, 3.0F, 13.0F, 27.0F, 0.0F);
        this.setRotateAngle(shell_flank_right, 0.0F, 0.0F, -0.2793F);

        this.head_neck = new AdvancedModelBox(this, 98, 71);
        this.head_neck.setRotationPoint(0.0F, 3.0F, -13.0F);
        this.head_neck.addBox(-5.5F, -3.5F, -6.0F, 11.0F, 7.5F, 7.0F, 0.0F);
        this.setRotateAngle(head_neck, 0.1047F, 0.0F, 0.0F);

        this.head_main = new AdvancedModelBox(this, 134, 71);
        this.head_main.setRotationPoint(0.0F, 0.0F, -5.0F);
        this.head_main.addBox(-5.5F, -3.0F, -5.5F, 11.0F, 7.5F, 6.5F, 0.0F);
        this.setRotateAngle(head_main, 0.0873F, 0.0F, 0.0F);

        this.head_cap = new AdvancedModelBox(this, 208, 90);
        this.head_cap.setRotationPoint(0.0F, -3.0F, -2.0F);
        this.head_cap.addBox(-5.0F, -1.8F, -3.0F, 10.0F, 2.3F, 5.5F, 0.0F);
        this.setRotateAngle(head_cap, 0.0524F, 0.0F, 0.0F);

        this.snout = new AdvancedModelBox(this, 186, 90);
        this.snout.setRotationPoint(0.0F, 1.5F, -5.0F);
        this.snout.addBox(-3.5F, -2.5F, -3.5F, 7.0F, 5.5F, 4.0F, 0.0F);
        this.setRotateAngle(snout, 0.1222F, 0.0F, 0.0F);

        // Eye planes sit 0.35 clear of the skull face (|x| 5.5) so they cannot z-fight the cheek, and
        // are 3x3 -- at 2px there is no room for a pupil to read.
        this.eye_left = new AdvancedModelBox(this, 72, 104);
        this.eye_left.setRotationPoint(5.85F, -0.5F, -5.0F);
        this.eye_left.addBox(0.0F, -1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 0.0F);

        this.eye_right = new AdvancedModelBox(this, 78, 104);
        this.eye_right.setRotationPoint(-5.85F, -0.5F, -5.0F);
        this.eye_right.addBox(0.0F, -1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 0.0F);

        this.arm_left_1 = new AdvancedModelBox(this, 170, 71);
        this.arm_left_1.setRotationPoint(8.0F, 7.0F, -8.0F);
        this.arm_left_1.addBox(-3.0F, -0.5F, -3.5F, 6.0F, 8.0F, 7.0F, 0.0F);
        this.setRotateAngle(arm_left_1, -0.0698F, 0.0F, -0.0698F);

        this.arm_left_2 = new AdvancedModelBox(this, 114, 90);
        this.arm_left_2.setRotationPoint(0.0F, 7.5F, 0.0F);
        this.arm_left_2.addBox(-2.5F, -0.5F, -3.0F, 5.0F, 6.0F, 6.0F, 0.0F);
        this.setRotateAngle(arm_left_2, 0.0698F, 0.0F, 0.0F);

        this.arm_right_1 = new AdvancedModelBox(this, 196, 71);
        this.arm_right_1.setRotationPoint(-8.0F, 7.0F, -8.0F);
        this.arm_right_1.addBox(-3.0F, -0.5F, -3.5F, 6.0F, 8.0F, 7.0F, 0.0F);
        this.setRotateAngle(arm_right_1, -0.0698F, 0.0F, 0.0698F);

        this.arm_right_2 = new AdvancedModelBox(this, 136, 90);
        this.arm_right_2.setRotationPoint(0.0F, 7.5F, 0.0F);
        this.arm_right_2.addBox(-2.5F, -0.5F, -3.0F, 5.0F, 6.0F, 6.0F, 0.0F);
        this.setRotateAngle(arm_right_2, 0.0698F, 0.0F, 0.0F);

        this.leg_left_1 = new AdvancedModelBox(this, 42, 71);
        this.leg_left_1.setRotationPoint(8.5F, 7.0F, 8.0F);
        this.leg_left_1.addBox(-3.0F, -0.5F, -4.0F, 6.0F, 8.0F, 8.0F, 0.0F);
        this.setRotateAngle(leg_left_1, 0.0698F, 0.0F, -0.0698F);

        this.leg_left_2 = new AdvancedModelBox(this, 36, 90);
        this.leg_left_2.setRotationPoint(0.0F, 7.5F, 0.0F);
        this.leg_left_2.addBox(-2.5F, -0.5F, -3.5F, 5.0F, 6.0F, 7.0F, 0.0F);
        this.setRotateAngle(leg_left_2, -0.0698F, 0.0F, 0.0F);

        this.leg_right_1 = new AdvancedModelBox(this, 70, 71);
        this.leg_right_1.setRotationPoint(-8.5F, 7.0F, 8.0F);
        this.leg_right_1.addBox(-3.0F, -0.5F, -4.0F, 6.0F, 8.0F, 8.0F, 0.0F);
        this.setRotateAngle(leg_right_1, 0.0698F, 0.0F, 0.0698F);

        this.leg_right_2 = new AdvancedModelBox(this, 60, 90);
        this.leg_right_2.setRotationPoint(0.0F, 7.5F, 0.0F);
        this.leg_right_2.addBox(-2.5F, -0.5F, -3.5F, 5.0F, 6.0F, 7.0F, 0.0F);
        this.setRotateAngle(leg_right_2, -0.0698F, 0.0F, 0.0F);

        this.tail_1 = new AdvancedModelBox(this, 0, 90);
        this.tail_1.setRotationPoint(0.0F, 1.0F, 14.0F);
        this.tail_1.addBox(-5.5F, -2.0F, -1.0F, 11.0F, 7.0F, 6.5F, 0.0F);
        this.setRotateAngle(tail_1, -0.1222F, 0.0F, 0.0F);

        this.tail_2 = new AdvancedModelBox(this, 84, 90);
        this.tail_2.setRotationPoint(0.0F, 1.0F, 4.5F);
        this.tail_2.addBox(-4.5F, -1.5F, -1.0F, 9.0F, 6.0F, 6.0F, 0.0F);
        this.setRotateAngle(tail_2, -0.1396F, 0.0F, 0.0F);

        this.tail_3 = new AdvancedModelBox(this, 158, 90);
        this.tail_3.setRotationPoint(0.0F, 1.5F, 4.0F);
        this.tail_3.addBox(-3.6F, -1.3F, -1.0F, 7.2F, 4.8F, 5.5F, 0.0F);
        this.setRotateAngle(tail_3, -0.1396F, 0.0F, 0.0F);

        // Doedicurus tail club + its four spikes. Toggled with showModel, NOT setScale: setScale does
        // not reach children unless setShouldScaleChildren(true), so scale-0 on the club (what the old
        // model did) would leave four spikes floating in mid-air on every glyptodon.
        this.tail_club = new AdvancedModelBox(this, 0, 71);
        this.tail_club.setRotationPoint(0.0F, 1.0F, 3.5F);
        this.tail_club.addBox(-6.5F, -5.5F, -0.5F, 13.0F, 10.5F, 7.5F, 0.0F);
        this.setRotateAngle(tail_club, -0.0524F, 0.0F, 0.0F);

        this.club_spike_left = new AdvancedModelBox(this, 40, 104);
        this.club_spike_left.setRotationPoint(6.5F, -0.5F, 3.0F);
        this.club_spike_left.addBox(-0.5F, -1.5F, -1.5F, 3.5F, 3.0F, 4.0F, 0.0F);
        this.setRotateAngle(club_spike_left, 0.0F, 0.0F, -0.3142F);

        this.club_spike_right = new AdvancedModelBox(this, 56, 104);
        this.club_spike_right.setRotationPoint(-6.5F, -0.5F, 3.0F);
        this.club_spike_right.addBox(-3.0F, -1.5F, -1.5F, 3.5F, 3.0F, 4.0F, 0.0F);
        this.setRotateAngle(club_spike_right, 0.0F, 0.0F, 0.3142F);

        this.club_spike_top = new AdvancedModelBox(this, 0, 104);
        this.club_spike_top.setRotationPoint(0.0F, -5.5F, 3.0F);
        this.club_spike_top.addBox(-3.0F, -3.0F, -1.5F, 6.0F, 3.5F, 4.0F, 0.0F);
        this.setRotateAngle(club_spike_top, 0.2094F, 0.0F, 0.0F);

        this.club_spike_rear = new AdvancedModelBox(this, 20, 104);
        this.club_spike_rear.setRotationPoint(0.0F, 0.0F, 7.0F);
        this.club_spike_rear.addBox(-3.0F, -2.0F, -0.5F, 6.0F, 4.0F, 3.5F, 0.0F);

        this.body_core.addChild(this.shell_crown);
        this.body_core.addChild(this.shell_front);
        this.body_core.addChild(this.shell_rear);
        this.body_core.addChild(this.shell_flank_left);
        this.body_core.addChild(this.shell_flank_right);
        this.body_core.addChild(this.head_neck);
        this.head_neck.addChild(this.head_main);
        this.head_main.addChild(this.head_cap);
        this.head_main.addChild(this.snout);
        this.head_main.addChild(this.eye_left);
        this.head_main.addChild(this.eye_right);
        this.body_core.addChild(this.arm_left_1);
        this.arm_left_1.addChild(this.arm_left_2);
        this.body_core.addChild(this.arm_right_1);
        this.arm_right_1.addChild(this.arm_right_2);
        this.body_core.addChild(this.leg_left_1);
        this.leg_left_1.addChild(this.leg_left_2);
        this.body_core.addChild(this.leg_right_1);
        this.leg_right_1.addChild(this.leg_right_2);
        this.body_core.addChild(this.tail_1);
        this.tail_1.addChild(this.tail_2);
        this.tail_2.addChild(this.tail_3);
        this.tail_3.addChild(this.tail_club);
        this.tail_club.addChild(this.club_spike_left);
        this.tail_club.addChild(this.club_spike_right);
        this.tail_club.addChild(this.club_spike_top);
        this.tail_club.addChild(this.club_spike_rear);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_core);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(
            body_core, shell_crown, shell_front, shell_rear, shell_flank_left, shell_flank_right,
            head_neck, head_main, head_cap, snout, eye_left, eye_right,
            arm_left_1, arm_left_2, arm_right_1, arm_right_2,
            leg_left_1, leg_left_2, leg_right_1, leg_right_2,
            tail_1, tail_2, tail_3, tail_club,
            club_spike_left, club_spike_right, club_spike_top, club_spike_rear
        );
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityGlyptodont glyptodont = (EntityGlyptodont) entityIn;
        animator.update(glyptodont);

        // Threat: hunker down, pull the head back under the shell rim and cock the tail to one side --
        // a glyptodont's defence is its carapace, and (for doedicurus) winding up the club.
        animator.setAnimation(EntityGlyptodont.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            animator.move(body_core, 0, 1.5F, 0);
            this.rotate(animator, head_neck, 14F, 0, 0);
            this.rotate(animator, head_main, 10F, 0, 0);
            this.rotate(animator, tail_1, 0, 26F, 0);
            this.rotate(animator, tail_2, 0, 20F, 0);
            this.rotate(animator, tail_3, 0, 16F, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            animator.move(body_core, 0, 1.0F, 0);
            this.rotate(animator, head_neck, 8F, 0, 0);
            this.rotate(animator, head_main, 6F, 0, 0);
            this.rotate(animator, tail_1, 0, -26F, 0);
            this.rotate(animator, tail_2, 0, -20F, 0);
            this.rotate(animator, tail_3, 0, -16F, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // Gore: the tail-club swing. Wind up to one side, then whip across.
        animator.setAnimation(EntityGlyptodont.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, body_core, 0, -14F, 0);
        this.rotate(animator, tail_1, 0, -42F, 0);
        this.rotate(animator, tail_2, 0, -34F, 0);
        this.rotate(animator, tail_3, 0, -28F, 0);
        this.rotate(animator, tail_club, 0, -18F, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, body_core, 0, 16F, 0);
        this.rotate(animator, tail_1, 0, 52F, 0);
        this.rotate(animator, tail_2, 0, 44F, 0);
        this.rotate(animator, tail_3, 0, 36F, 0);
        this.rotate(animator, tail_club, 0, 24F, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityGlyptodont glyptodont, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(glyptodont);
        float globalSpeed = 1.0f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // Per-species: doedicurus carries the tail club, glyptodon's tail just tapers out. showModel
        // rather than setScale, so the four spikes (children of the club) vanish with it. Written on
        // BOTH branches every frame -- the model instance is shared across all glyptodonts.
        boolean club = glyptodont.hasTailClub();
        this.tail_club.showModel = club;
        this.club_spike_left.showModel = club;
        this.club_spike_right.showModel = club;
        this.club_spike_top.showModel = club;
        this.club_spike_rear.showModel = club;

        // Idle: the shell barely moves -- only a slow breath and a lazy tail sway.
        this.body_core.setScale((float) (1.0F + Math.sin(ageInTicks / 24) * 0.035F), (float) (1.0F + Math.sin(ageInTicks / 20) * 0.035F), 1.0F);
        bob(body_core, 0.3F * globalSpeed, 0.08F, false, ageInTicks / 24, 2);
        walk(head_neck, 0.3f * globalSpeed, 0.04f, false, 2.8F, 0.05F, ageInTicks / 24, 2);
        this.swing(tail_1, 0.07F, 0.09F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_2, 0.07F, 0.13F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail_3, 0.07F, 0.17F, false, 2F, 0F, ageInTicks, 1);

        // Blinking: bury the eye planes inside the skull (|x| 5.85 -> 4.6).
        if (!glyptodont.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(4.6F, -0.5F, -5.0F);
            this.eye_right.setRotationPoint(-4.6F, -0.5F, -5.0F);
        }

        // Head Tracking. The head is short and buried under the shell rim, so it turns less than most.
        if (!glyptodont.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 4, head_neck);
            this.faceTarget(netHeadYaw, headPitch, 4, head_main);
        }

        // Pitch/Yaw handler
        if (glyptodont.isInWater()) {
            this.setRotateAngle(head_main, -0.18203784098300857F, 0.0F, 0.0F);
            if (!glyptodont.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(glyptodont.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_core, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // Walk: short two-segment pillars with a small stride and a heavy body bob. The carapace is
        // rigid, so unlike the other megafauna nothing in the torso flexes -- all the motion is in the
        // legs, plus the tail swinging behind.
        if (glyptodont.canMove()) {
            bob(body_core, 0.8f * globalSpeed, 0.35f * globalDegree, true, f, limbSwingAmount);
            walk(head_neck, 0.8f * globalSpeed, 0.12f * globalDegree, false, 0, 0, f, limbSwingAmount);

            walk(arm_right_1, -0.8f * globalSpeed, 0.75f * globalDegree, true, 0F, 0.6f, f, limbSwingAmount);
            walk(arm_right_2, -0.8f * globalSpeed, 0.75f * globalDegree, false, -1F, 0.6f, f, limbSwingAmount * 1.2f);
            walk(arm_left_1, -0.8f * globalSpeed, 0.75f * globalDegree, true, 2F, 0.6f, f, limbSwingAmount);
            walk(arm_left_2, -0.8f * globalSpeed, 0.75f * globalDegree, false, 1F, 0.6f, f, limbSwingAmount * 1.2f);
            walk(leg_right_1, 0.8f * globalSpeed, 0.75f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(leg_right_2, 0.8f * globalSpeed, 0.75f * globalDegree, true, 1.8F, 0, f, limbSwingAmount);
            walk(leg_left_1, 0.8f * globalSpeed, 0.75f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(leg_left_2, 0.8f * globalSpeed, 0.75f * globalDegree, true, -0.2F, 0, f, limbSwingAmount);

            swing(tail_1, 0.8f * globalSpeed, 0.22f * globalDegree, false, 0F, 0, f, limbSwingAmount);
            swing(tail_2, 0.8f * globalSpeed, 0.28f * globalDegree, false, -1F, 0, f, limbSwingAmount);
            swing(tail_3, 0.8f * globalSpeed, 0.34f * globalDegree, false, -2F, 0, f, limbSwingAmount);
        }

        // Rest / sleep: the animal simply settles, which is what an armoured tank does -- the shell
        // sinks 7 units until its rim is just off the ground and the short legs fold flat underneath
        // it. The tail also has to FLATTEN (its standing droop would otherwise put the club 1.9 units
        // through the floor once the body drops). Solved against real box geometry: nothing clips.
        if (glyptodont.sitProgress > 0) {
            applyRestingPose(glyptodont.sitProgress);
        }
        else if (glyptodont.sleepProgress > 0) {
            applyRestingPose(glyptodont.sleepProgress);
        }
    }

    private void applyRestingPose(float progress) {
        this.progressPosition(body_core, progress, 0.0F, 11.0F, -2.0F, 40);

        this.progressRotation(arm_left_1, progress, (float) Math.toRadians(75F), 0, (float) Math.toRadians(-4F), 40);
        this.progressRotation(arm_left_2, progress, 0, 0, 0, 40);
        this.progressRotation(arm_right_1, progress, (float) Math.toRadians(75F), 0, (float) Math.toRadians(4F), 40);
        this.progressRotation(arm_right_2, progress, 0, 0, 0, 40);
        this.progressRotation(leg_left_1, progress, (float) Math.toRadians(80F), 0, (float) Math.toRadians(-4F), 40);
        this.progressRotation(leg_left_2, progress, 0, 0, 0, 40);
        this.progressRotation(leg_right_1, progress, (float) Math.toRadians(80F), 0, (float) Math.toRadians(4F), 40);
        this.progressRotation(leg_right_2, progress, 0, 0, 0, 40);

        this.progressRotation(tail_1, progress, (float) Math.toRadians(2F), 0, 0, 40);
        this.progressRotation(tail_2, progress, (float) Math.toRadians(2F), 0, 0, 40);
        this.progressRotation(tail_3, progress, (float) Math.toRadians(2F), 0, 0, 40);
        this.progressRotation(tail_club, progress, (float) Math.toRadians(6F), 0, 0, 40);

        this.progressRotation(head_neck, progress, (float) Math.toRadians(2F), 0, 0, 40);
        this.progressRotation(head_main, progress, (float) Math.toRadians(2F), 0, 0, 40);
    }
}
