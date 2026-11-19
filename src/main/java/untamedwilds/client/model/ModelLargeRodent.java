package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityLargeRodent;

/**
 * Large rodents -- capybara, Neochoerus, Castoroides, North American beaver (Phase 4.6, 2026-10-10): 30 boxes on 256x128,
 * authored in scripts/large_rodent_spec.py, ported VERBATIM by scripts/gen_model_large_rodent.py, proved by
 * verify_port.py. A heavy barrel on short legs with a long blunt head and orange incisors.
 *   * The beavers' paddle tail (variant-gated) is carried level behind on land, lies on the ground at rest, sculls in
 *     water, and SLAPS the surface as an alarm (ATTACK_SLAP).
 *   * The bird passenger is shown per entity (EntityLargeRodent.hasBird(), synced) -- capybaras only.
 */
public class ModelLargeRodent extends AdvancedEntityModel<EntityLargeRodent> {

    private final AdvancedModelBox body_barrel;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox muzzle;
    private final AdvancedModelBox nose;
    private final AdvancedModelBox incisors;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox fore_left_upper;
    private final AdvancedModelBox fore_left_lower;
    private final AdvancedModelBox fore_left_foot;
    private final AdvancedModelBox fore_right_upper;
    private final AdvancedModelBox fore_right_lower;
    private final AdvancedModelBox fore_right_foot;
    private final AdvancedModelBox body_rump;
    private final AdvancedModelBox tail_base;
    private final AdvancedModelBox tail_paddle;
    private final AdvancedModelBox hind_left_thigh;
    private final AdvancedModelBox hind_left_shank;
    private final AdvancedModelBox hind_left_foot;
    private final AdvancedModelBox hind_right_thigh;
    private final AdvancedModelBox hind_right_shank;
    private final AdvancedModelBox hind_right_foot;
    private final AdvancedModelBox bird_body;
    private final AdvancedModelBox bird_head;
    private final AdvancedModelBox bird_beak;
    private final AdvancedModelBox bird_tail;

    private final ModelAnimator animator;

    public ModelLargeRodent() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_barrel = new AdvancedModelBox(this, 0, 0);
        this.body_barrel.setRotationPoint(0.0F, 16.0F, 2.0F);
        this.body_barrel.addBox(-4.2000F, -3.8000F, -7.5000F, 8.4000F, 7.6000F, 14.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 95, 0);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -6.0F);
        this.body_chest.addBox(-3.6000F, -3.2000F, -5.0F, 7.2000F, 6.6000F, 5.0F, 0.0F);
        this.setRotateAngle(body_chest, -0.0698F, 0.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 120, 0);
        this.neck.setRotationPoint(0.0F, -1.9000F, -4.5000F);
        this.neck.addBox(-3.0F, -2.2000F, -2.5000F, 6.0F, 5.0F, 4.5000F, 0.0F);
        this.setRotateAngle(neck, -0.1047F, 0.0F, 0.0F);
        this.head = new AdvancedModelBox(this, 70, 0);
        this.head.setRotationPoint(0.0F, 0.0F, -2.5000F);
        this.head.addBox(-2.7000F, -2.4000F, -5.6000F, 5.4000F, 5.0F, 7.0F, 0.0F);
        this.setRotateAngle(head, 0.3840F, 0.0F, 0.0F);
        this.muzzle = new AdvancedModelBox(this, 193, 0);
        this.muzzle.setRotationPoint(0.0F, 0.0F, -5.6000F);
        this.muzzle.addBox(-2.3000F, -1.9000F, -2.0F, 4.6000F, 4.1000F, 3.0F, 0.0F);
        this.setRotateAngle(muzzle, 0.0349F, 0.0F, 0.0F);
        this.nose = new AdvancedModelBox(this, 91, 22);
        this.nose.setRotationPoint(0.0F, -0.6000F, -2.0F);
        this.nose.addBox(-1.6000F, -0.9000F, -0.4000F, 3.2000F, 1.7000F, 0.7000F, 0.0F);
        this.incisors = new AdvancedModelBox(this, 105, 22);
        this.incisors.setRotationPoint(0.0F, 1.6000F, -1.8000F);
        this.incisors.addBox(-0.8000F, -0.4000F, -0.4000F, 1.6000F, 1.4000F, 0.8000F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 110, 22);
        this.ear_left.setRotationPoint(2.0F, -2.3000F, -0.8000F);
        this.ear_left.addBox(-0.7000F, -1.0F, -0.6000F, 1.4000F, 1.3000F, 0.8000F, 0.0F);
        this.setRotateAngle(ear_left, 0.0F, 0.0F, 0.2618F);
        this.eye_left = new AdvancedModelBox(this, 83, 22);
        this.eye_left.setRotationPoint(3.0500F, -1.2000F, -2.4000F);
        this.eye_left.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.ear_right = new AdvancedModelBox(this, 115, 22);
        this.ear_right.setRotationPoint(-2.0F, -2.3000F, -0.8000F);
        this.ear_right.addBox(-0.7000F, -1.0F, -0.6000F, 1.4000F, 1.3000F, 0.8000F, 0.0F);
        this.setRotateAngle(ear_right, 0.0F, 0.0F, -0.2618F);
        this.eye_right = new AdvancedModelBox(this, 87, 22);
        this.eye_right.setRotationPoint(-3.0500F, -1.2000F, -2.4000F);
        this.eye_right.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.fore_left_upper = new AdvancedModelBox(this, 209, 0);
        this.fore_left_upper.setRotationPoint(3.1500F, 2.5179F, -2.5000F);
        this.fore_left_upper.addBox(-1.3500F, -1.6000F, -1.6000F, 2.7000F, 4.2000F, 3.1000F, 0.0F);
        this.setRotateAngle(fore_left_upper, -0.0698F, 0.0F, 0.0F);
        this.fore_left_lower = new AdvancedModelBox(this, 29, 22);
        this.fore_left_lower.setRotationPoint(-0.2500F, 2.6000F, 0.0F);
        this.fore_left_lower.addBox(-0.7000F, -0.8000F, -1.1000F, 1.4000F, 3.0F, 2.1000F, 0.0F);
        this.setRotateAngle(fore_left_lower, 0.2443F, 0.0F, 0.0F);
        this.fore_left_foot = new AdvancedModelBox(this, 54, 22);
        this.fore_left_foot.setRotationPoint(0.0500F, 2.2000F, 0.0F);
        this.fore_left_foot.addBox(-0.9500F, -0.5000F, -2.0F, 1.9000F, 1.4000F, 2.8000F, 0.0F);
        this.setRotateAngle(fore_left_foot, -0.1047F, 0.0F, 0.0F);
        this.fore_right_upper = new AdvancedModelBox(this, 221, 0);
        this.fore_right_upper.setRotationPoint(-3.1500F, 2.5179F, -2.5000F);
        this.fore_right_upper.addBox(-1.3500F, -1.6000F, -1.6000F, 2.7000F, 4.2000F, 3.1000F, 0.0F);
        this.setRotateAngle(fore_right_upper, -0.0698F, 0.0F, 0.0F);
        this.fore_right_lower = new AdvancedModelBox(this, 36, 22);
        this.fore_right_lower.setRotationPoint(0.2500F, 2.6000F, 0.0F);
        this.fore_right_lower.addBox(-0.7000F, -0.8000F, -1.1000F, 1.4000F, 3.0F, 2.1000F, 0.0F);
        this.setRotateAngle(fore_right_lower, 0.2443F, 0.0F, 0.0F);
        this.fore_right_foot = new AdvancedModelBox(this, 64, 22);
        this.fore_right_foot.setRotationPoint(-0.0500F, 2.2000F, 0.0F);
        this.fore_right_foot.addBox(-0.9500F, -0.5000F, -2.0F, 1.9000F, 1.4000F, 2.8000F, 0.0F);
        this.setRotateAngle(fore_right_foot, -0.1047F, 0.0F, 0.0F);
        this.body_rump = new AdvancedModelBox(this, 45, 0);
        this.body_rump.setRotationPoint(0.0F, 0.0F, 5.5000F);
        this.body_rump.addBox(-3.9000F, -4.4000F, 0.0F, 7.8000F, 7.6000F, 4.5000F, 0.0F);
        this.setRotateAngle(body_rump, 0.0698F, 0.0F, 0.0F);
        this.tail_base = new AdvancedModelBox(this, 43, 22);
        this.tail_base.setRotationPoint(0.0F, -0.4000F, 4.1000F);
        this.tail_base.addBox(-1.3000F, -0.8000F, -0.2000F, 2.6000F, 1.7000F, 2.8000F, 0.0F);
        this.setRotateAngle(tail_base, -0.1745F, 0.0F, 0.0F);
        this.tail_paddle = new AdvancedModelBox(this, 169, 0);
        this.tail_paddle.setRotationPoint(0.0F, 0.0F, 2.0F);
        this.tail_paddle.addBox(-2.6000F, -0.5500F, -0.6000F, 5.2000F, 1.1000F, 6.4000F, 0.0F);
        this.setRotateAngle(tail_paddle, -0.0349F, 0.0F, 0.0F);
        this.hind_left_thigh = new AdvancedModelBox(this, 141, 0);
        this.hind_left_thigh.setRotationPoint(3.1000F, 2.3094F, 2.5000F);
        this.hind_left_thigh.addBox(-1.4000F, -1.8000F, -2.0F, 2.8000F, 4.8000F, 4.0F, 0.0F);
        this.setRotateAngle(hind_left_thigh, -0.5061F, 0.0F, 0.0F);
        this.hind_left_shank = new AdvancedModelBox(this, 13, 22);
        this.hind_left_shank.setRotationPoint(-0.2000F, 3.0F, 0.0F);
        this.hind_left_shank.addBox(-0.8000F, -0.9000F, -1.1000F, 1.6000F, 3.5000F, 2.2000F, 0.0F);
        this.setRotateAngle(hind_left_shank, 0.9599F, 0.0F, 0.0F);
        this.hind_left_foot = new AdvancedModelBox(this, 233, 0);
        this.hind_left_foot.setRotationPoint(0.0F, 2.6000F, 0.0F);
        this.hind_left_foot.addBox(-1.1000F, -0.5000F, -3.0F, 2.2000F, 1.4000F, 3.8000F, 0.0F);
        this.setRotateAngle(hind_left_foot, -0.5236F, 0.0F, 0.0F);
        this.hind_right_thigh = new AdvancedModelBox(this, 155, 0);
        this.hind_right_thigh.setRotationPoint(-3.1000F, 2.3094F, 2.5000F);
        this.hind_right_thigh.addBox(-1.4000F, -1.8000F, -2.0F, 2.8000F, 4.8000F, 4.0F, 0.0F);
        this.setRotateAngle(hind_right_thigh, -0.5061F, 0.0F, 0.0F);
        this.hind_right_shank = new AdvancedModelBox(this, 21, 22);
        this.hind_right_shank.setRotationPoint(0.2000F, 3.0F, 0.0F);
        this.hind_right_shank.addBox(-0.8000F, -0.9000F, -1.1000F, 1.6000F, 3.5000F, 2.2000F, 0.0F);
        this.setRotateAngle(hind_right_shank, 0.9599F, 0.0F, 0.0F);
        this.hind_right_foot = new AdvancedModelBox(this, 0, 22);
        this.hind_right_foot.setRotationPoint(0.0F, 2.6000F, 0.0F);
        this.hind_right_foot.addBox(-1.1000F, -0.5000F, -3.0F, 2.2000F, 1.4000F, 3.8000F, 0.0F);
        this.setRotateAngle(hind_right_foot, -0.5236F, 0.0F, 0.0F);
        this.bird_body = new AdvancedModelBox(this, 74, 22);
        this.bird_body.setRotationPoint(0.0F, -3.8000F, 0.4000F);
        this.bird_body.addBox(-0.8000F, -1.2000F, -1.2000F, 1.6000F, 1.5000F, 2.6000F, 0.0F);
        this.bird_head = new AdvancedModelBox(this, 120, 22);
        this.bird_head.setRotationPoint(0.0F, -1.2000F, -1.0F);
        this.bird_head.addBox(-0.5500F, -0.8000F, -0.8000F, 1.1000F, 1.1000F, 1.1000F, 0.0F);
        this.bird_beak = new AdvancedModelBox(this, 125, 22);
        this.bird_beak.setRotationPoint(0.0F, -0.2000F, -0.8000F);
        this.bird_beak.addBox(-0.2000F, -0.1500F, -0.7000F, 0.4000F, 0.3000F, 0.8000F, 0.0F);
        this.bird_tail = new AdvancedModelBox(this, 99, 22);
        this.bird_tail.setRotationPoint(0.0F, -0.6000F, 1.4000F);
        this.bird_tail.addBox(-0.5000F, -0.1000F, -0.2000F, 1.0F, 0.3000F, 2.0F, 0.0F);
        this.setRotateAngle(bird_tail, 0.3491F, 0.0F, 0.0F);

        this.body_barrel.addChild(this.body_chest);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.muzzle);
        this.muzzle.addChild(this.nose);
        this.muzzle.addChild(this.incisors);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.ear_right);
        this.head.addChild(this.eye_right);
        this.body_chest.addChild(this.fore_left_upper);
        this.fore_left_upper.addChild(this.fore_left_lower);
        this.fore_left_lower.addChild(this.fore_left_foot);
        this.body_chest.addChild(this.fore_right_upper);
        this.fore_right_upper.addChild(this.fore_right_lower);
        this.fore_right_lower.addChild(this.fore_right_foot);
        this.body_barrel.addChild(this.body_rump);
        this.body_rump.addChild(this.tail_base);
        this.tail_base.addChild(this.tail_paddle);
        this.body_rump.addChild(this.hind_left_thigh);
        this.hind_left_thigh.addChild(this.hind_left_shank);
        this.hind_left_shank.addChild(this.hind_left_foot);
        this.body_rump.addChild(this.hind_right_thigh);
        this.hind_right_thigh.addChild(this.hind_right_shank);
        this.hind_right_shank.addChild(this.hind_right_foot);
        this.body_barrel.addChild(this.bird_body);
        this.bird_body.addChild(this.bird_head);
        this.bird_head.addChild(this.bird_beak);
        this.bird_body.addChild(this.bird_tail);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_barrel);
    }

    // Built once: getAllParts() is called every frame, and a fresh ImmutableList per animal per frame
    // was pure garbage (2.8.0, same as ModelBison).
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
                body_barrel,
                body_chest,
                neck,
                head,
                muzzle,
                nose,
                incisors,
                ear_left,
                eye_left,
                ear_right,
                eye_right,
                fore_left_upper,
                fore_left_lower,
                fore_left_foot,
                fore_right_upper,
                fore_right_lower,
                fore_right_foot,
                body_rump,
                tail_base,
                tail_paddle,
                hind_left_thigh,
                hind_left_shank,
                hind_left_foot,
                hind_right_thigh,
                hind_right_shank,
                hind_right_foot,
                bird_body,
                bird_head,
                bird_beak,
                bird_tail
        );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityLargeRodent rodent = (EntityLargeRodent) entityIn;
        animator.update(rodent);
        // <gen:idle-poses> 2.8.0: SOLVED by scripts/pose_reach.py against this model's own boxes (muzzle to the grass / the
        // water line beyond the bank, every head part and horn family clear of the ground, nothing through the torso).
        // Regenerate, never hand-edit: python scripts/gen_keyframes.py large_rodent
        // EAT (40t, GrazeGoal's mouthful): head down to the grass, a cropping lift, back down
        animator.setAnimation(EntityLargeRodent.EAT);
        animator.startKeyframe(10);
        this.rotate(animator, neck, 18.2F, 0.0F, 0.0F);
        this.rotate(animator, head, 7.3F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 1.3F, 0.0F, 0.0F);
        this.rotate(animator, nose, 0.2F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 2.9F, -1.3F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 18.2F, 0.0F, 0.0F);
        this.rotate(animator, head, 1.3F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 1.3F, 0.0F, 0.0F);
        this.rotate(animator, nose, 0.2F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 2.9F, -1.3F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 18.2F, 0.0F, 0.0F);
        this.rotate(animator, head, 7.3F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 1.3F, 0.0F, 0.0F);
        this.rotate(animator, nose, 0.2F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 2.9F, -1.3F);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.resetKeyframe(10);
        // ALERT (40t, AlertGoal): head raised, ears up, staring at the threat; repeats while the alert lasts
        animator.setAnimation(EntityLargeRodent.ALERT);
        animator.startKeyframe(8);
        this.rotate(animator, neck, -35.0F, 0.0F, 0.0F);
        this.rotate(animator, head, -9.0F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 12.0F, 0.0F, 0.0F);
        this.rotate(animator, nose, 12.0F, 0.0F, 0.0F);
        this.rotate(animator, ear_left, 25.0F, 0.0F, 0.0F);
        this.rotate(animator, ear_right, 25.0F, 0.0F, 0.0F);
        animator.endKeyframe();
        animator.setStaticKeyframe(24);
        animator.resetKeyframe(8);
        // </gen:idle-poses>

        // ATTACK_BITE (12t): head drops and lunges, incisors first
        animator.setAnimation(EntityLargeRodent.ATTACK_BITE);
        animator.startKeyframe(5);
        this.rotate(animator, neck, -12.0F, 0, 0);
        this.rotate(animator, head, -8.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        animator.move(body_barrel, 0, 0, -1.2F);
        this.rotate(animator, neck, 16.0F, 0, 0);
        this.rotate(animator, head, 10.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(3);

        // ATTACK_SLAP (16t): the beaver alarm -- tail raised high, then slammed down flat
        animator.setAnimation(EntityLargeRodent.ATTACK_SLAP);
        animator.startKeyframe(7);
        this.rotate(animator, tail_base, -40.0F, 0, 0);
        this.rotate(animator, tail_paddle, -15.0F, 0, 0);
        this.rotate(animator, body_barrel, 4.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(3);
        this.rotate(animator, tail_base, 35.0F, 0, 0);
        this.rotate(animator, tail_paddle, 10.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(6);
    }

    public void setupAnim(EntityLargeRodent rodent, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(rodent);
        limbSwingAmount = Math.min(0.5F, limbSwingAmount);
        float gait = limbSwing * 0.8F / Math.max(0.3F, rodent.getMobSize() * rodent.getScale());
        boolean swimming = rodent.isInWater() && !rodent.isOnGround();

        int variant = rodent.getVariant();
        this.tail_base.showModel = variant == 2 || variant == 3;
        this.bird_body.showModel = rodent.hasBird();

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.03F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(ear_left, 0.18F, 0.15F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.18F, 0.15F, false, 1F, 0F, ageInTicks, 1);
        this.walk(bird_head, 0.35F, 0.25F, false, 0F, 0F, ageInTicks, 1);
        this.swing(bird_head, 0.12F, 0.5F, false, 1F, 0F, ageInTicks, 1);

        if (!rodent.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(1.6775F, -1.2000F, -2.4000F);
            this.eye_right.setRotationPoint(-1.6775F, -1.2000F, -2.4000F);
        }

        if (!rodent.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        if (swimming) {
            // swimming low with only the head up: legs paddle, the paddle tail sculls up and down
            float sw = ageInTicks * 0.3F;
            this.neck.rotateAngleX -= 0.25F;
            this.fore_left_upper.rotateAngleX += Mth.cos(sw) * 0.6F;
            this.fore_right_upper.rotateAngleX += Mth.cos(sw + Mth.PI) * 0.6F;
            this.hind_left_thigh.rotateAngleX += Mth.cos(sw + Mth.PI) * 0.7F;
            this.hind_right_thigh.rotateAngleX += Mth.cos(sw) * 0.7F;
            this.tail_base.rotateAngleX += Mth.cos(sw * 0.7F) * 0.2F;
            this.tail_paddle.rotateAngleX += Mth.cos(sw * 0.7F - 0.8F) * 0.25F;
        }
        else if (rodent.canMove()) {
            // a short-stepping diagonal walk, the body rolling slightly over the stubby legs
            bob(body_barrel, 2.0F, 0.4F, true, gait, limbSwingAmount);
            walk(fore_left_upper, 1.0F, 0.7F, false, 0F, 0F, gait, limbSwingAmount);
            walk(fore_left_lower, 1.0F, 0.4F, false, -1.3F, 0.1F, gait, limbSwingAmount);
            walk(fore_right_upper, 1.0F, 0.7F, false, Mth.PI, 0F, gait, limbSwingAmount);
            walk(fore_right_lower, 1.0F, 0.4F, false, Mth.PI - 1.3F, 0.1F, gait, limbSwingAmount);
            walk(hind_left_thigh, 1.0F, 0.65F, false, Mth.PI, 0F, gait, limbSwingAmount);
            walk(hind_left_shank, 1.0F, 0.45F, true, Mth.PI - 1.2F, 0F, gait, limbSwingAmount);
            walk(hind_right_thigh, 1.0F, 0.65F, false, 0F, 0F, gait, limbSwingAmount);
            walk(hind_right_shank, 1.0F, 0.45F, true, -1.2F, 0F, gait, limbSwingAmount);
            swing(body_barrel, 1.0F, 0.05F, false, 0F, 0F, gait, limbSwingAmount);
            walk(tail_base, 2.0F, 0.06F, false, 0F, 0F, gait, limbSwingAmount);
        }

        if (rodent.sitProgress > 0) {
            // SOLVED (large_rodent_spec.py --pose): belly down, legs folded, the paddle tail laid on the ground
            this.progressPosition(body_barrel, rodent.sitProgress, 0.0F, 19.2000F, 0.0F, 40);
            this.progressRotation(fore_left_upper, rodent.sitProgress, -1.0480F, 0, 0, 40);
            this.progressRotation(fore_left_lower, rodent.sitProgress, -0.3424F, 0, 0, 40);
            this.progressRotation(fore_left_foot, rodent.sitProgress, -0.0446F, 0, 0, 40);
            this.progressRotation(fore_right_upper, rodent.sitProgress, -1.0480F, 0, 0, 40);
            this.progressRotation(fore_right_lower, rodent.sitProgress, -0.3424F, 0, 0, 40);
            this.progressRotation(fore_right_foot, rodent.sitProgress, -0.0446F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, rodent.sitProgress, -1.3676F, 0, 0, 40);
            this.progressRotation(hind_left_shank, rodent.sitProgress, 3.3171F, 0, 0, 40);
            this.progressRotation(hind_left_foot, rodent.sitProgress, -0.4473F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, rodent.sitProgress, -1.3676F, 0, 0, 40);
            this.progressRotation(hind_right_shank, rodent.sitProgress, 3.3171F, 0, 0, 40);
            this.progressRotation(hind_right_foot, rodent.sitProgress, -0.4473F, 0, 0, 40);
            this.progressRotation(tail_base, rodent.sitProgress, -0.7330F, 0, 0, 40);
            this.progressRotation(neck, rodent.sitProgress, -0.2094F, 0, 0, 40);
        }
        else if (rodent.sleepProgress > 0) {
            // SOLVED (large_rodent_spec.py --pose): belly down, legs folded, the paddle tail laid on the ground
            this.progressPosition(body_barrel, rodent.sleepProgress, 0.0F, 19.8000F, 0.0F, 40);
            this.progressRotation(fore_left_upper, rodent.sleepProgress, -1.3433F, 0, 0, 40);
            this.progressRotation(fore_left_lower, rodent.sleepProgress, 0.0066F, 0, 0, 40);
            this.progressRotation(fore_left_foot, rodent.sleepProgress, -0.0899F, 0, 0, 40);
            this.progressRotation(fore_right_upper, rodent.sleepProgress, -1.3433F, 0, 0, 40);
            this.progressRotation(fore_right_lower, rodent.sleepProgress, 0.0066F, 0, 0, 40);
            this.progressRotation(fore_right_foot, rodent.sleepProgress, -0.0899F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, rodent.sleepProgress, -1.5907F, 0, 0, 40);
            this.progressRotation(hind_left_shank, rodent.sleepProgress, 3.5180F, 0, 0, 40);
            this.progressRotation(hind_left_foot, rodent.sleepProgress, -0.4260F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, rodent.sleepProgress, -1.5907F, 0, 0, 40);
            this.progressRotation(hind_right_shank, rodent.sleepProgress, 3.5180F, 0, 0, 40);
            this.progressRotation(hind_right_foot, rodent.sleepProgress, -0.4260F, 0, 0, 40);
            this.progressRotation(tail_base, rodent.sleepProgress, -0.6283F, 0, 0, 40);
            this.progressRotation(neck, rodent.sleepProgress, 0.0349F, 0, 0, 40);
        }
    }
}
