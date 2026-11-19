package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityMustelid;

/**
 * Mustelids -- wolverine and honey badger (2.9.0, NEW TYPE): 26 boxes on 128x64, authored in
 * scripts/mustelid_spec.py, ported VERBATIM by scripts/gen_model_mustelid.py, proved by verify_port.py.
 *
 * A long low barrel arched over the middle, short thick legs on broad plantigrade paws, a broad heavy head carried low
 * on a thick neck, small round ears, a bushy tail. Both species share the mesh (the honey badger's mantle is its skin).
 *   * SIT and SLEEP are SOLVED (mustelid_spec.solve_sit / solve_sleep): sitting, the body pitches up 18 deg until
 *     the rump rests on the ground with the fore paws planted; asleep it lies belly-down, legs tucked, chin and tail down.
 *   * The walk is the four-term cycle mustelid_spec.walk_check() measured: no digging, each paw planted about half the time.
 */
public class ModelMustelid extends AdvancedEntityModel<EntityMustelid> {

    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox body_hips;
    private final AdvancedModelBox leg_back_left_1;
    private final AdvancedModelBox leg_back_left_2;
    private final AdvancedModelBox paw_back_left;
    private final AdvancedModelBox leg_back_right_1;
    private final AdvancedModelBox leg_back_right_2;
    private final AdvancedModelBox paw_back_right;
    private final AdvancedModelBox tail_1;
    private final AdvancedModelBox tail_2;
    private final AdvancedModelBox body_back;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox snout;
    private final AdvancedModelBox nose;
    private final AdvancedModelBox jaw;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox leg_front_left_1;
    private final AdvancedModelBox leg_front_left_2;
    private final AdvancedModelBox paw_front_left;
    private final AdvancedModelBox leg_front_right_1;
    private final AdvancedModelBox leg_front_right_2;
    private final AdvancedModelBox paw_front_right;

    private final ModelAnimator animator;

    public ModelMustelid() {
        this.texWidth = 128;
        this.texHeight = 64;

        this.body_chest = new AdvancedModelBox(this, 0, 0);
        this.body_chest.setRotationPoint(0.0F, 18.0F, -2.5000F);
        this.body_chest.addBox(-3.0F, -2.4000F, -4.0F, 6.0F, 5.4000F, 7.0F, 0.0F);
        this.body_hips = new AdvancedModelBox(this, 26, 0);
        this.body_hips.setRotationPoint(0.0F, 0.0F, 3.0F);
        this.body_hips.addBox(-3.2000F, -2.0F, -0.5000F, 6.4000F, 4.7000F, 6.6000F, 0.0F);
        this.leg_back_left_1 = new AdvancedModelBox(this, 113, 0);
        this.leg_back_left_1.setRotationPoint(2.3000F, 0.2000F, 3.9000F);
        this.leg_back_left_1.addBox(-1.1500F, -0.8000F, -1.2000F, 2.3000F, 4.7000F, 2.5000F, 0.0F);
        this.leg_back_left_2 = new AdvancedModelBox(this, 120, 13);
        this.leg_back_left_2.setRotationPoint(0.0F, 3.5000F, 0.5000F);
        this.leg_back_left_2.addBox(-0.9000F, -0.3000F, -0.8000F, 1.8000F, 1.9000F, 1.3000F, 0.0F);
        this.paw_back_left = new AdvancedModelBox(this, 76, 13);
        this.paw_back_left.setRotationPoint(0.0F, 1.5000F, -0.3000F);
        this.paw_back_left.addBox(-1.1500F, -0.2000F, -2.0F, 2.3000F, 1.0F, 3.2000F, 0.0F);
        this.leg_back_right_1 = new AdvancedModelBox(this, 0, 13);
        this.leg_back_right_1.setRotationPoint(-2.3000F, 0.2000F, 3.9000F);
        this.leg_back_right_1.addBox(-1.1500F, -0.8000F, -1.2000F, 2.3000F, 4.7000F, 2.5000F, 0.0F);
        this.leg_back_right_2 = new AdvancedModelBox(this, 0, 21);
        this.leg_back_right_2.setRotationPoint(0.0F, 3.5000F, 0.5000F);
        this.leg_back_right_2.addBox(-0.9000F, -0.3000F, -0.8000F, 1.8000F, 1.9000F, 1.3000F, 0.0F);
        this.paw_back_right = new AdvancedModelBox(this, 98, 13);
        this.paw_back_right.setRotationPoint(0.0F, 1.5000F, -0.3000F);
        this.paw_back_right.addBox(-1.1500F, -0.2000F, -2.0F, 2.3000F, 1.0F, 3.2000F, 0.0F);
        this.tail_1 = new AdvancedModelBox(this, 42, 13);
        this.tail_1.setRotationPoint(0.0F, -1.2000F, 5.9000F);
        this.tail_1.addBox(-1.2000F, -1.1000F, -0.4000F, 2.4000F, 2.2000F, 3.4000F, 0.0F);
        this.setRotateAngle(tail_1, -0.4363F, 0.0F, 0.0F);
        this.tail_2 = new AdvancedModelBox(this, 10, 13);
        this.tail_2.setRotationPoint(0.0F, 0.0F, 2.7000F);
        this.tail_2.addBox(-1.5500F, -1.4000F, -0.3000F, 3.1000F, 2.8000F, 3.8000F, 0.0F);
        this.setRotateAngle(tail_2, -0.1745F, 0.0F, 0.0F);
        this.body_back = new AdvancedModelBox(this, 52, 0);
        this.body_back.setRotationPoint(0.0F, -2.0F, 1.5000F);
        this.body_back.addBox(-2.3000F, -1.0F, -3.5000F, 4.6000F, 1.4000F, 8.5000F, 0.0F);
        this.neck = new AdvancedModelBox(this, 97, 0);
        this.neck.setRotationPoint(0.0F, -0.8000F, -3.5000F);
        this.neck.addBox(-2.2000F, -1.2000F, -2.8000F, 4.4000F, 3.4000F, 3.6000F, 0.0F);
        this.head = new AdvancedModelBox(this, 79, 0);
        this.head.setRotationPoint(0.0F, 0.2000F, -2.5000F);
        this.head.addBox(-2.5000F, -1.8000F, -3.5000F, 5.0F, 4.0F, 3.8000F, 0.0F);
        this.snout = new AdvancedModelBox(this, 54, 13);
        this.snout.setRotationPoint(0.0F, 1.0F, -3.3000F);
        this.snout.addBox(-1.3500F, -1.1500F, -2.4000F, 2.7000F, 2.1500F, 2.6000F, 0.0F);
        this.nose = new AdvancedModelBox(this, 29, 21);
        this.nose.setRotationPoint(0.0F, -0.7000F, -2.4000F);
        this.nose.addBox(-0.6500F, -0.6500F, -0.3000F, 1.3000F, 1.1000F, 0.5000F, 0.0F);
        this.jaw = new AdvancedModelBox(this, 109, 13);
        this.jaw.setRotationPoint(0.0F, 2.0F, -2.5000F);
        this.jaw.addBox(-1.1500F, -0.3000F, -2.8000F, 2.3000F, 0.9000F, 3.0F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 33, 21);
        this.ear_left.setRotationPoint(1.9000F, -1.6000F, -0.7000F);
        this.ear_left.addBox(-0.7000F, -1.1000F, -0.3000F, 1.4000F, 1.3000F, 0.6000F, 0.0F);
        this.setRotateAngle(ear_left, 0.0F, 0.0F, -0.2618F);
        this.eye_left = new AdvancedModelBox(this, 21, 21);
        this.eye_left.setRotationPoint(2.8500F, -0.4000F, -2.3000F);
        this.eye_left.addBox(0.0F, -0.6000F, -0.8000F, 0.0F, 1.2000F, 1.6000F, 0.0F);
        this.ear_right = new AdvancedModelBox(this, 37, 21);
        this.ear_right.setRotationPoint(-1.9000F, -1.6000F, -0.7000F);
        this.ear_right.addBox(-0.7000F, -1.1000F, -0.3000F, 1.4000F, 1.3000F, 0.6000F, 0.0F);
        this.setRotateAngle(ear_right, 0.0F, 0.0F, 0.2618F);
        this.eye_right = new AdvancedModelBox(this, 25, 21);
        this.eye_right.setRotationPoint(-2.8500F, -0.4000F, -2.3000F);
        this.eye_right.addBox(0.0F, -0.6000F, -0.8000F, 0.0F, 1.2000F, 1.6000F, 0.0F);
        this.leg_front_left_1 = new AdvancedModelBox(this, 24, 13);
        this.leg_front_left_1.setRotationPoint(2.1500F, 0.8000F, -1.5000F);
        this.leg_front_left_1.addBox(-1.1000F, -0.6000F, -1.0F, 2.2000F, 4.2000F, 2.0F, 0.0F);
        this.leg_front_left_2 = new AdvancedModelBox(this, 7, 21);
        this.leg_front_left_2.setRotationPoint(0.0F, 3.2000F, 0.0F);
        this.leg_front_left_2.addBox(-0.8500F, -0.3000F, -0.7000F, 1.7000F, 1.6000F, 1.4000F, 0.0F);
        this.paw_front_left = new AdvancedModelBox(this, 65, 13);
        this.paw_front_left.setRotationPoint(0.0F, 1.2000F, -0.2000F);
        this.paw_front_left.addBox(-1.2000F, -0.2000F, -1.7000F, 2.4000F, 1.0F, 3.1000F, 0.0F);
        this.leg_front_right_1 = new AdvancedModelBox(this, 33, 13);
        this.leg_front_right_1.setRotationPoint(-2.1500F, 0.8000F, -1.5000F);
        this.leg_front_right_1.addBox(-1.1000F, -0.6000F, -1.0F, 2.2000F, 4.2000F, 2.0F, 0.0F);
        this.leg_front_right_2 = new AdvancedModelBox(this, 14, 21);
        this.leg_front_right_2.setRotationPoint(0.0F, 3.2000F, 0.0F);
        this.leg_front_right_2.addBox(-0.8500F, -0.3000F, -0.7000F, 1.7000F, 1.6000F, 1.4000F, 0.0F);
        this.paw_front_right = new AdvancedModelBox(this, 87, 13);
        this.paw_front_right.setRotationPoint(0.0F, 1.2000F, -0.2000F);
        this.paw_front_right.addBox(-1.2000F, -0.2000F, -1.7000F, 2.4000F, 1.0F, 3.1000F, 0.0F);

        this.body_chest.addChild(this.body_hips);
        this.body_hips.addChild(this.leg_back_left_1);
        this.leg_back_left_1.addChild(this.leg_back_left_2);
        this.leg_back_left_2.addChild(this.paw_back_left);
        this.body_hips.addChild(this.leg_back_right_1);
        this.leg_back_right_1.addChild(this.leg_back_right_2);
        this.leg_back_right_2.addChild(this.paw_back_right);
        this.body_hips.addChild(this.tail_1);
        this.tail_1.addChild(this.tail_2);
        this.body_chest.addChild(this.body_back);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.snout);
        this.snout.addChild(this.nose);
        this.head.addChild(this.jaw);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.ear_right);
        this.head.addChild(this.eye_right);
        this.body_chest.addChild(this.leg_front_left_1);
        this.leg_front_left_1.addChild(this.leg_front_left_2);
        this.leg_front_left_2.addChild(this.paw_front_left);
        this.body_chest.addChild(this.leg_front_right_1);
        this.leg_front_right_1.addChild(this.leg_front_right_2);
        this.leg_front_right_2.addChild(this.paw_front_right);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_chest);
    }

    // Built once: getAllParts() runs every frame (2.8.0).
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
                body_chest,
                body_hips,
                leg_back_left_1,
                leg_back_left_2,
                paw_back_left,
                leg_back_right_1,
                leg_back_right_2,
                paw_back_right,
                tail_1,
                tail_2,
                body_back,
                neck,
                head,
                snout,
                nose,
                jaw,
                ear_left,
                eye_left,
                ear_right,
                eye_right,
                leg_front_left_1,
                leg_front_left_2,
                paw_front_left,
                leg_front_right_1,
                leg_front_right_2,
                paw_front_right
            );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        animator.update(entityIn);
        // ATTACK_BITE (12t): the head draws back, then lunges with the jaw open and snaps
        animator.setAnimation(EntityMustelid.ATTACK_BITE);
        animator.startKeyframe(4);
        this.rotate(animator, neck, 10.0F, 0, 0);
        this.rotate(animator, jaw, 25.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(3);
        this.rotate(animator, neck, -8.0F, 0, 0);
        this.rotate(animator, jaw, 30.0F, 0, 0);
        animator.move(body_chest, 0, 0, -1.5F);
        animator.endKeyframe();
        animator.resetKeyframe(5);
        // IDLE_TALK (24t): a snarl -- head up a little, the jaw drops twice
        animator.setAnimation(EntityMustelid.IDLE_TALK);
        animator.startKeyframe(5);
        this.rotate(animator, neck, -8.0F, 0, 0);
        this.rotate(animator, jaw, 20.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, -8.0F, 0, 0);
        this.rotate(animator, jaw, 6.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, -8.0F, 0, 0);
        this.rotate(animator, jaw, 22.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(10);
    }

    public void setupAnim(EntityMustelid mustelid, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(mustelid);
        float rest = Math.max(mustelid.sitProgress, mustelid.sleepProgress) / 40F;
        limbSwingAmount = Math.min(0.6F, limbSwingAmount) * (1F - rest);
        // a planted paw keeps pace whatever the species' size (the dire wolf gait rule)
        float gait = limbSwing / Math.max(0.35F, mustelid.getMobSize() * mustelid.getScale());

        // breathing + idle tail
        float breath = (float) (Math.sin(ageInTicks / 18) * 0.03F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(tail_1, 0.12F, 0.12F, false, 0F, 0F, ageInTicks, 1F - rest);
        this.swing(tail_2, 0.12F, 0.16F, false, 1F, 0F, ageInTicks, 1F - rest);

        // ---- walk (mustelid_spec.walk_terms, Java radians) ----
        this.walk(leg_back_left_1, 0.9000F, 0.5500F, false, 0.0F, 0.0F, gait, limbSwingAmount);
        this.walk(leg_back_left_2, 0.9000F, 0.4500F, false, -1.5708F, 0.4500F, gait, limbSwingAmount);
        this.walk(paw_back_left, 0.9000F, -0.5500F, false, 0.0F, 0.0F, gait, limbSwingAmount);
        this.walk(paw_back_left, 0.9000F, -0.4500F, false, -1.5708F, -0.4500F, gait, limbSwingAmount);
        this.walk(leg_back_right_1, 0.9000F, 0.5500F, false, 3.1416F, 0.0F, gait, limbSwingAmount);
        this.walk(leg_back_right_2, 0.9000F, 0.4500F, false, 1.5708F, 0.4500F, gait, limbSwingAmount);
        this.walk(paw_back_right, 0.9000F, -0.5500F, false, 3.1416F, 0.0F, gait, limbSwingAmount);
        this.walk(paw_back_right, 0.9000F, -0.4500F, false, 1.5708F, -0.4500F, gait, limbSwingAmount);
        this.walk(leg_front_left_1, 0.9000F, 0.5500F, false, -1.5708F, 0.0F, gait, limbSwingAmount);
        this.walk(leg_front_left_2, 0.9000F, 0.4500F, false, -3.1416F, 0.4500F, gait, limbSwingAmount);
        this.walk(paw_front_left, 0.9000F, -0.5500F, false, -1.5708F, 0.0F, gait, limbSwingAmount);
        this.walk(paw_front_left, 0.9000F, -0.4500F, false, -3.1416F, -0.4500F, gait, limbSwingAmount);
        this.walk(leg_front_right_1, 0.9000F, 0.5500F, false, 1.5708F, 0.0F, gait, limbSwingAmount);
        this.walk(leg_front_right_2, 0.9000F, 0.4500F, false, 0.0F, 0.4500F, gait, limbSwingAmount);
        this.walk(paw_front_right, 0.9000F, -0.5500F, false, 1.5708F, 0.0F, gait, limbSwingAmount);
        this.walk(paw_front_right, 0.9000F, -0.4500F, false, 0.0F, -0.4500F, gait, limbSwingAmount);
        this.bob(body_chest, 2 * 0.9000F, 0.4F, false, gait, limbSwingAmount);
        this.swing(body_chest, 0.9000F, 0.05F, false, 0F, 0F, gait, limbSwingAmount);

        // Blinking: bury the eye planes inside the head; y/z keep the constructor's values.
        if (!mustelid.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(1.4250F, -0.4000F, -2.3000F);
            this.eye_right.setRotationPoint(-1.4250F, -0.4000F, -2.3000F);
        }

        if (!mustelid.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        // ---- SOLVED rest poses (mustelid_spec.solve_sit / solve_sleep): target = rest + delta, Java axes ----
        if (mustelid.sitProgress > 0) {
            this.progressPosition(body_chest, mustelid.sitProgress, 0.0F, 18.4827F, -2.5000F, 40);
            this.progressRotation(body_chest, mustelid.sitProgress, -0.3142F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_back_left_1, mustelid.sitProgress, -0.7737F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_back_left_2, mustelid.sitProgress, -0.3395F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_back_right_1, mustelid.sitProgress, -0.7737F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_back_right_2, mustelid.sitProgress, -0.3395F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_front_left_1, mustelid.sitProgress, 0.3142F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_front_right_1, mustelid.sitProgress, 0.3142F, 0.0F, 0.0F, 40);
            this.progressRotation(neck, mustelid.sitProgress, 0.1885F, 0.0F, 0.0F, 40);
            this.progressRotation(paw_back_left, mustelid.sitProgress, -0.1416F, 0.0F, 0.0F, 40);
            this.progressRotation(paw_back_right, mustelid.sitProgress, -0.1416F, 0.0F, 0.0F, 40);
            this.progressRotation(tail_1, mustelid.sitProgress, -0.0262F, 0.0F, 0.0F, 40);
        }
        else if (mustelid.sleepProgress > 0) {
            this.progressPosition(body_chest, mustelid.sleepProgress, 0.0F, 20.7000F, -2.5000F, 40);
            this.progressRotation(head, mustelid.sleepProgress, 0.0733F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_back_left_1, mustelid.sleepProgress, -1.2618F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_back_left_2, mustelid.sleepProgress, -0.0205F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_back_right_1, mustelid.sleepProgress, -1.2618F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_back_right_2, mustelid.sleepProgress, -0.0205F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_front_left_1, mustelid.sleepProgress, -1.3251F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_front_left_2, mustelid.sleepProgress, 0.0130F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_front_right_1, mustelid.sleepProgress, -1.3251F, 0.0F, 0.0F, 40);
            this.progressRotation(leg_front_right_2, mustelid.sleepProgress, 0.0130F, 0.0F, 0.0F, 40);
            this.progressRotation(neck, mustelid.sleepProgress, 0.1100F, 0.0F, 0.0F, 40);
            this.progressRotation(paw_back_left, mustelid.sleepProgress, -0.0116F, 0.0F, 0.0F, 40);
            this.progressRotation(paw_back_right, mustelid.sleepProgress, -0.0116F, 0.0F, 0.0F, 40);
            this.progressRotation(paw_front_left, mustelid.sleepProgress, -0.0108F, 0.0F, 0.0F, 40);
            this.progressRotation(paw_front_right, mustelid.sleepProgress, -0.0108F, 0.0F, 0.0F, 40);
            this.progressRotation(tail_1, mustelid.sleepProgress, -0.4625F, 0.0F, 0.0F, 40);
        }
    }
}
