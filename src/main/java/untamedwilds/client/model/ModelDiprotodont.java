package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityDiprotodont;

/**
 * Diprotodont -- 30 boxes on 256x128, authored from scratch (Phase 4.1, 2026-10-09). There is no
 * donor rig: before this class nothing in the tree modelled a diprotodontid body.
 *
 * Sculpted from scripts/diprotodont_spec.py, which carries the spec, the forward-kinematics
 * checker, the z-fight sweep, the volume-burial check, the UV shelf pack and the phase-7 pose
 * solver. Box coords + texOffs are transcribed VERBATIM from the modded_entity export (the Y-down
 * flip is the export's, never re-derived); scripts/gen_model_diprotodont.py does the
 * transcription and scripts/verify_port.py proves it.
 *
 * A wombat the size of a rhinoceros: a broad barrel (width/depth 1.34) with a slight shoulder rise
 * and a rounded rump, pillar legs with the belly at 40% of the withers, broad plantigrade feet
 * turned INWARD (the pigeon-toed diprotodont stance -- 16 deg fore, 12 hind, a Y rotation on each
 * foot), and a big low-carried head with wide cheeks, a deep jaw, a fleshy nose pad and short
 * forward-pointing incisors.
 *
 * FIVE species share this one mesh, sized by species `scale` at 1 block = 1 m at the shoulder.
 *   * NASAL_BOSS is shown for zygomaturus alone (getVariant(), the ModelDeer:509-524 pattern).
 *   * The JOEY (a baby's head peeking from the rear-facing pouch) follows the synced
 *     EntityDiprotodont.hasJoey() flag, and is tucked away whenever the mother sits or sleeps.
 * showModel is applied to each chain ROOT: ModelPart.render returns before recursing into
 * children when `visible` is false, so hiding a root hides its whole subtree.
 */
public class ModelDiprotodont extends AdvancedEntityModel<EntityDiprotodont> {

    private final AdvancedModelBox body_barrel;
    private final AdvancedModelBox body_shoulders;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head_skull;
    private final AdvancedModelBox head_muzzle;
    private final AdvancedModelBox nose;
    private final AdvancedModelBox nasal_boss;
    private final AdvancedModelBox head_jaw;
    private final AdvancedModelBox incisors;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox fore_left_upper;
    private final AdvancedModelBox fore_left_forearm;
    private final AdvancedModelBox fore_left_foot;
    private final AdvancedModelBox fore_right_upper;
    private final AdvancedModelBox fore_right_forearm;
    private final AdvancedModelBox fore_right_foot;
    private final AdvancedModelBox body_rump;
    private final AdvancedModelBox tail;
    private final AdvancedModelBox hind_left_thigh;
    private final AdvancedModelBox hind_left_shank;
    private final AdvancedModelBox hind_left_foot;
    private final AdvancedModelBox hind_right_thigh;
    private final AdvancedModelBox hind_right_shank;
    private final AdvancedModelBox hind_right_foot;
    private final AdvancedModelBox joey_head;
    private final AdvancedModelBox joey_snout;

    private final ModelAnimator animator;

    public ModelDiprotodont() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_barrel = new AdvancedModelBox(this, 0, 0);
        this.body_barrel.setRotationPoint(0.0F, 8.0F, 3.0F);
        this.body_barrel.addBox(-8.8000F, -6.6000F, -11.0F, 17.6000F, 13.1000F, 21.0F, 0.0F);
        this.body_shoulders = new AdvancedModelBox(this, 78, 0);
        this.body_shoulders.setRotationPoint(0.0F, 0.0F, -9.0F);
        this.body_shoulders.addBox(-8.2000F, -7.4000F, -8.0F, 16.4000F, 12.4000F, 14.0F, 0.0F);
        this.setRotateAngle(body_shoulders, -0.1047F, 0.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 139, 0);
        this.body_chest.setRotationPoint(0.0F, 1.0F, -14.0F);
        this.body_chest.addBox(-7.4000F, -6.4000F, -5.5000F, 14.8000F, 13.0F, 9.5000F, 0.0F);
        this.setRotateAngle(body_chest, 0.1047F, 0.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 41, 35);
        this.neck.setRotationPoint(0.0F, -2.0F, -5.0F);
        this.neck.addBox(-6.0F, -4.0F, -3.5000F, 12.0F, 8.6000F, 6.5000F, 0.0F);
        this.setRotateAngle(neck, 0.0349F, 0.0F, 0.0F);
        this.head_skull = new AdvancedModelBox(this, 0, 35);
        this.head_skull.setRotationPoint(0.0F, 0.0F, -3.5000F);
        this.head_skull.addBox(-5.4000F, -4.2000F, -7.5000F, 10.8000F, 8.2000F, 9.5000F, 0.0F);
        this.setRotateAngle(head_skull, 0.1047F, 0.0F, 0.0F);
        this.head_muzzle = new AdvancedModelBox(this, 104, 53);
        this.head_muzzle.setRotationPoint(0.0F, 0.0F, -7.5000F);
        this.head_muzzle.addBox(-3.9000F, -3.0F, -4.2000F, 7.8000F, 6.0F, 5.8000F, 0.0F);
        this.setRotateAngle(head_muzzle, 0.1396F, 0.0F, 0.0F);
        this.nose = new AdvancedModelBox(this, 29, 68);
        this.nose.setRotationPoint(0.0F, -0.6000F, -3.8000F);
        this.nose.addBox(-3.2000F, -1.8000F, -1.8000F, 6.4000F, 3.2000F, 2.4000F, 0.0F);
        this.nasal_boss = new AdvancedModelBox(this, 47, 68);
        this.nasal_boss.setRotationPoint(0.0F, -2.6000F, -1.3000F);
        this.nasal_boss.addBox(-2.4000F, -1.3000F, -1.7000F, 4.8000F, 1.6000F, 3.6000F, 0.0F);
        this.head_jaw = new AdvancedModelBox(this, 142, 35);
        this.head_jaw.setRotationPoint(0.0F, 3.0F, -1.5000F);
        this.head_jaw.addBox(-4.2000F, -0.6000F, -8.0F, 8.4000F, 3.8000F, 10.5000F, 0.0F);
        this.incisors = new AdvancedModelBox(this, 84, 68);
        this.incisors.setRotationPoint(0.0F, 0.4000F, -8.0F);
        this.incisors.addBox(-1.2000F, -0.6000F, -2.2000F, 2.4000F, 1.4000F, 2.6000F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 64, 68);
        this.ear_left.setRotationPoint(3.8000F, -3.6000F, -0.1000F);
        this.ear_left.addBox(-0.2000F, -1.6000F, -1.0F, 2.8000F, 2.4000F, 2.0F, 0.0F);
        this.setRotateAngle(ear_left, -0.1047F, -0.2443F, -0.5934F);
        this.ear_right = new AdvancedModelBox(this, 74, 68);
        this.ear_right.setRotationPoint(-3.8000F, -3.6000F, -0.1000F);
        this.ear_right.addBox(-2.6000F, -1.6000F, -1.0F, 2.8000F, 2.4000F, 2.0F, 0.0F);
        this.setRotateAngle(ear_right, -0.1047F, 0.2443F, 0.5934F);
        this.eye_left = new AdvancedModelBox(this, 102, 68);
        this.eye_left.setRotationPoint(5.8000F, -2.2000F, -4.0F);
        this.eye_left.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 106, 68);
        this.eye_right.setRotationPoint(-5.8000F, -2.2000F, -4.0F);
        this.eye_right.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.fore_left_upper = new AdvancedModelBox(this, 180, 35);
        this.fore_left_upper.setRotationPoint(6.2000F, 1.4873F, 1.0F);
        this.fore_left_upper.addBox(-3.6000F, -1.4000F, -3.8000F, 7.2000F, 7.4000F, 7.4000F, 0.0F);
        this.setRotateAngle(fore_left_upper, -0.2094F, 0.0F, 0.0F);
        this.fore_left_forearm = new AdvancedModelBox(this, 0, 53);
        this.fore_left_forearm.setRotationPoint(-0.2000F, 6.0F, 0.0F);
        this.fore_left_forearm.addBox(-3.0F, -2.0F, -3.6000F, 6.0F, 7.4000F, 6.8000F, 0.0F);
        this.setRotateAngle(fore_left_forearm, 0.1745F, 0.0F, 0.0F);
        this.fore_left_foot = new AdvancedModelBox(this, 132, 53);
        this.fore_left_foot.setRotationPoint(0.2000F, 5.4000F, 0.0F);
        this.fore_left_foot.addBox(-3.4000F, -0.8000F, -4.8000F, 6.8000F, 3.0F, 7.2000F, 0.0F);
        this.setRotateAngle(fore_left_foot, -0.0698F, 0.2793F, 0.0F);
        this.fore_right_upper = new AdvancedModelBox(this, 210, 35);
        this.fore_right_upper.setRotationPoint(-6.2000F, 1.4873F, 1.0F);
        this.fore_right_upper.addBox(-3.6000F, -1.4000F, -3.8000F, 7.2000F, 7.4000F, 7.4000F, 0.0F);
        this.setRotateAngle(fore_right_upper, -0.2094F, 0.0F, 0.0F);
        this.fore_right_forearm = new AdvancedModelBox(this, 26, 53);
        this.fore_right_forearm.setRotationPoint(0.2000F, 6.0F, 0.0F);
        this.fore_right_forearm.addBox(-3.0F, -2.0F, -3.6000F, 6.0F, 7.4000F, 6.8000F, 0.0F);
        this.setRotateAngle(fore_right_forearm, 0.1745F, 0.0F, 0.0F);
        this.fore_right_foot = new AdvancedModelBox(this, 160, 53);
        this.fore_right_foot.setRotationPoint(-0.2000F, 5.4000F, 0.0F);
        this.fore_right_foot.addBox(-3.4000F, -0.8000F, -4.8000F, 6.8000F, 3.0F, 7.2000F, 0.0F);
        this.setRotateAngle(fore_right_foot, -0.0698F, -0.2793F, 0.0F);
        this.body_rump = new AdvancedModelBox(this, 188, 0);
        this.body_rump.setRotationPoint(0.0F, 0.0F, 6.5000F);
        this.body_rump.addBox(-8.4000F, -6.2000F, 0.0F, 16.8000F, 11.7000F, 9.0F, 0.0F);
        this.setRotateAngle(body_rump, -0.2094F, 0.0F, 0.0F);
        this.tail = new AdvancedModelBox(this, 0, 68);
        this.tail.setRotationPoint(0.0F, -2.0F, 8.5000F);
        this.tail.addBox(-1.8000F, -1.4000F, -0.6000F, 3.6000F, 4.4000F, 3.6000F, 0.0F);
        this.setRotateAngle(tail, 0.5236F, 0.0F, 0.0F);
        this.hind_left_thigh = new AdvancedModelBox(this, 78, 35);
        this.hind_left_thigh.setRotationPoint(6.1000F, 1.8427F, 4.5000F);
        this.hind_left_thigh.addBox(-3.9000F, -1.6000F, -3.8000F, 7.8000F, 8.0F, 7.8000F, 0.0F);
        this.setRotateAngle(hind_left_thigh, -0.0349F, 0.0F, 0.0F);
        this.hind_left_shank = new AdvancedModelBox(this, 52, 53);
        this.hind_left_shank.setRotationPoint(-0.2000F, 6.4000F, 0.0F);
        this.hind_left_shank.addBox(-3.1000F, -1.0F, -3.4000F, 6.2000F, 6.0F, 6.6000F, 0.0F);
        this.setRotateAngle(hind_left_shank, 0.4538F, 0.0F, 0.0F);
        this.hind_left_foot = new AdvancedModelBox(this, 188, 53);
        this.hind_left_foot.setRotationPoint(0.1500F, 5.0F, 0.0F);
        this.hind_left_foot.addBox(-3.5500F, -0.8000F, -5.0F, 7.1000F, 2.8000F, 7.2000F, 0.0F);
        this.setRotateAngle(hind_left_foot, -0.2094F, 0.2094F, 0.0F);
        this.hind_right_thigh = new AdvancedModelBox(this, 110, 35);
        this.hind_right_thigh.setRotationPoint(-6.1000F, 1.8427F, 4.5000F);
        this.hind_right_thigh.addBox(-3.9000F, -1.6000F, -3.8000F, 7.8000F, 8.0F, 7.8000F, 0.0F);
        this.setRotateAngle(hind_right_thigh, -0.0349F, 0.0F, 0.0F);
        this.hind_right_shank = new AdvancedModelBox(this, 78, 53);
        this.hind_right_shank.setRotationPoint(0.2000F, 6.4000F, 0.0F);
        this.hind_right_shank.addBox(-3.1000F, -1.0F, -3.4000F, 6.2000F, 6.0F, 6.6000F, 0.0F);
        this.setRotateAngle(hind_right_shank, 0.4538F, 0.0F, 0.0F);
        this.hind_right_foot = new AdvancedModelBox(this, 217, 53);
        this.hind_right_foot.setRotationPoint(-0.1500F, 5.0F, 0.0F);
        this.hind_right_foot.addBox(-3.5500F, -0.8000F, -5.0F, 7.1000F, 2.8000F, 7.2000F, 0.0F);
        this.setRotateAngle(hind_right_foot, -0.2094F, -0.2094F, 0.0F);
        this.joey_head = new AdvancedModelBox(this, 15, 68);
        this.joey_head.setRotationPoint(0.0F, 6.6000F, 6.0F);
        this.joey_head.addBox(-1.7000F, -1.0F, -0.4000F, 3.4000F, 3.2000F, 3.6000F, 0.0F);
        this.joey_snout = new AdvancedModelBox(this, 94, 68);
        this.joey_snout.setRotationPoint(0.0F, 1.0F, 3.0F);
        this.joey_snout.addBox(-1.0F, -0.8000F, -0.2000F, 2.0F, 1.8000F, 1.8000F, 0.0F);

        this.body_barrel.addChild(this.body_shoulders);
        this.body_barrel.addChild(this.body_chest);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head_skull);
        this.head_skull.addChild(this.head_muzzle);
        this.head_muzzle.addChild(this.nose);
        this.head_muzzle.addChild(this.nasal_boss);
        this.head_skull.addChild(this.head_jaw);
        this.head_jaw.addChild(this.incisors);
        this.head_skull.addChild(this.ear_left);
        this.head_skull.addChild(this.ear_right);
        this.head_skull.addChild(this.eye_left);
        this.head_skull.addChild(this.eye_right);
        this.body_chest.addChild(this.fore_left_upper);
        this.fore_left_upper.addChild(this.fore_left_forearm);
        this.fore_left_forearm.addChild(this.fore_left_foot);
        this.body_chest.addChild(this.fore_right_upper);
        this.fore_right_upper.addChild(this.fore_right_forearm);
        this.fore_right_forearm.addChild(this.fore_right_foot);
        this.body_barrel.addChild(this.body_rump);
        this.body_rump.addChild(this.tail);
        this.body_rump.addChild(this.hind_left_thigh);
        this.hind_left_thigh.addChild(this.hind_left_shank);
        this.hind_left_shank.addChild(this.hind_left_foot);
        this.body_rump.addChild(this.hind_right_thigh);
        this.hind_right_thigh.addChild(this.hind_right_shank);
        this.hind_right_shank.addChild(this.hind_right_foot);
        this.body_barrel.addChild(this.joey_head);
        this.joey_head.addChild(this.joey_snout);

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
                body_shoulders,
                body_chest,
                neck,
                head_skull,
                head_muzzle,
                nose,
                nasal_boss,
                head_jaw,
                incisors,
                ear_left,
                ear_right,
                eye_left,
                eye_right,
                fore_left_upper,
                fore_left_forearm,
                fore_left_foot,
                fore_right_upper,
                fore_right_forearm,
                fore_right_foot,
                body_rump,
                tail,
                hind_left_thigh,
                hind_left_shank,
                hind_left_foot,
                hind_right_thigh,
                hind_right_shank,
                hind_right_foot,
                joey_head,
                joey_snout
        );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityDiprotodont dip = (EntityDiprotodont) entityIn;
        animator.update(dip);
        // <gen:idle-poses> 2.8.0: SOLVED by scripts/pose_reach.py against this model's own boxes (muzzle to the grass / the
        // water line beyond the bank, every head part and horn family clear of the ground, nothing through the torso).
        // Regenerate, never hand-edit: python scripts/gen_keyframes.py diprotodont
        // EAT (40t, GrazeGoal's mouthful): head down to the grass, a cropping lift, back down
        animator.setAnimation(EntityDiprotodont.EAT);
        animator.startKeyframe(10);
        this.rotate(animator, neck, 27.4F, 0.0F, 0.0F);
        this.rotate(animator, head_skull, 13.3F, 0.0F, 0.0F);
        this.rotate(animator, head_muzzle, -0.5F, 0.0F, 0.0F);
        this.rotate(animator, nose, -0.7F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 2.1F, -1.6F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 27.4F, 0.0F, 0.0F);
        this.rotate(animator, head_skull, 7.3F, 0.0F, 0.0F);
        this.rotate(animator, head_muzzle, -0.5F, 0.0F, 0.0F);
        this.rotate(animator, nose, -0.7F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 2.1F, -1.6F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 27.4F, 0.0F, 0.0F);
        this.rotate(animator, head_skull, 13.3F, 0.0F, 0.0F);
        this.rotate(animator, head_muzzle, -0.5F, 0.0F, 0.0F);
        this.rotate(animator, nose, -0.7F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 2.1F, -1.6F);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.resetKeyframe(10);
        // DRINK (100t, DrinkGoal): muzzle to the water past the bank edge, two swallows lifting the head
        animator.setAnimation(EntityDiprotodont.DRINK);
        animator.startKeyframe(15);
        this.rotate(animator, neck, 14.2F, 0.0F, 0.0F);
        this.rotate(animator, head_skull, 6.8F, 0.0F, 0.0F);
        this.rotate(animator, head_muzzle, -0.3F, 0.0F, 0.0F);
        this.rotate(animator, nose, -0.7F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 5.0F, -0.9F);
        animator.endKeyframe();
        animator.setStaticKeyframe(20);
        animator.startKeyframe(5);
        this.rotate(animator, neck, 14.2F, 0.0F, 0.0F);
        this.rotate(animator, head_skull, 1.8F, 0.0F, 0.0F);
        this.rotate(animator, head_muzzle, -0.3F, 0.0F, 0.0F);
        this.rotate(animator, nose, -0.7F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 5.0F, -0.9F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 14.2F, 0.0F, 0.0F);
        this.rotate(animator, head_skull, 6.8F, 0.0F, 0.0F);
        this.rotate(animator, head_muzzle, -0.3F, 0.0F, 0.0F);
        this.rotate(animator, nose, -0.7F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 5.0F, -0.9F);
        animator.endKeyframe();
        animator.setStaticKeyframe(20);
        animator.startKeyframe(5);
        this.rotate(animator, neck, 14.2F, 0.0F, 0.0F);
        this.rotate(animator, head_skull, 1.8F, 0.0F, 0.0F);
        this.rotate(animator, head_muzzle, -0.3F, 0.0F, 0.0F);
        this.rotate(animator, nose, -0.7F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 5.0F, -0.9F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 14.2F, 0.0F, 0.0F);
        this.rotate(animator, head_skull, 6.8F, 0.0F, 0.0F);
        this.rotate(animator, head_muzzle, -0.3F, 0.0F, 0.0F);
        this.rotate(animator, nose, -0.7F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 5.0F, -0.9F);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.resetKeyframe(15);
        // </gen:idle-poses>

        // ATTACK_THREATEN (50t) -- no horns and no tusks, so the display is mass: the head drops
        // and swings side to side while the animal shifts its weight from one forefoot to the
        // other, the way a wombat squares up before it shoves.
        animator.setAnimation(EntityDiprotodont.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_chest, 0, 0, 5.0F);
            this.rotate(animator, neck, 8.0F, -12.0F, -5.0F);
            this.rotate(animator, head_skull, 6.0F, -10.0F, 0);
            this.rotate(animator, fore_left_upper, -10.0F, 0, 0);
            this.rotate(animator, fore_right_upper, 8.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_chest, 0, 0, -8.0F);
            this.rotate(animator, neck, 8.0F, 12.0F, 5.0F);
            this.rotate(animator, head_skull, 6.0F, 10.0F, 0);
            this.rotate(animator, fore_left_upper, 8.0F, 0, 0);
            this.rotate(animator, fore_right_upper, -10.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_GORE (14t) -- misnamed by the shared Animation field: a diprotodont SHOVES. The
        // whole body sinks back onto the haunches, then drives forward with the head low and the
        // jaw opening for a bite on the follow-through (EntityDiprotodont throws the target).
        animator.setAnimation(EntityDiprotodont.ATTACK_GORE);
        animator.startKeyframe(6);
        animator.move(body_barrel, 0, 0.6F, 2.0F);
        this.rotate(animator, body_barrel, -4.0F, 0, 0);
        this.rotate(animator, neck, -10.0F, 0, 0);
        this.rotate(animator, head_skull, -8.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        animator.move(body_barrel, 0, 0, -3.0F);
        this.rotate(animator, body_barrel, 3.0F, 0, 0);
        this.rotate(animator, neck, 14.0F, 0, 0);
        this.rotate(animator, head_skull, 12.0F, 0, 0);
        this.rotate(animator, head_jaw, 22.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityDiprotodont dip, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(dip);
        float globalDegree = 1f;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);
        // Gait phase divided by the RENDER size: a 1.2-scale Diprotodon and a 0.4-scale Maokopia
        // cover very different ground per leg cycle, so one fixed rate would slide the big one's
        // feet and make the small one shuffle (the ModelDireWolf fix).
        float gait = limbSwing * 0.55F / Math.max(0.3F, dip.getMobSize() * dip.getScale());

        // ---- per-species toggles ----
        int variant = dip.getVariant();
        // nasal boss ON for [1] (zygomaturus) ALONE
        this.nasal_boss.showModel = variant == 1;
        // joey: only while the mother is up -- lying down, it ducks back into the pouch
        this.joey_head.showModel = dip.hasJoey() && dip.sitProgress == 0 && dip.sleepProgress == 0;

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 24) * 0.04F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        bob(body_barrel, 0.3F, 0.08F, false, ageInTicks / 24, 2);
        walk(neck, 0.3f, 0.04f, false, 2.8F, 0.03F, ageInTicks / 24, 2);
        this.swing(ear_left, 0.16F, 0.18F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.16F, 0.18F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail, 0.10F, 0.12F, false, 0F, 0F, ageInTicks, 1);
        this.swing(joey_head, 0.08F, 0.20F, false, 0F, 0F, ageInTicks, 1);

        // Blinking: bury the eye planes inside the skull. y/z MUST keep the
        // constructor's values or the eye jumps across the face on every blink.
        if (!dip.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(3.1900F, -2.2000F, -4.0F);
            this.eye_right.setRotationPoint(-3.1900F, -2.2000F, -4.0F);
        }

        // Head tracking: a short thick neck, so most of the turn happens at the skull.
        if (!dip.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 4, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head_skull);
        }

        if (dip.isInWater() && !dip.isOnGround()) {
            this.setRotateAngle(head_skull, -0.1820F, 0.0F, 0.0F);
            gait = ageInTicks / 6;
            limbSwingAmount = 0.5f;
            float pitch = Mth.clamp(dip.getXRot() - 10, -25F, 25.0F);
            this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
        }

        // ---- gait: pillar legs swing from the shoulder and hip with a small bend below; a lateral
        // sequence walk (hind leads its same-side fore by a quarter cycle), amplitudes kept LOW --
        // a two-tonne graviportal animal does not stride like a horse ----
        if (dip.canMove()) {
            bob(body_barrel, 2.0f, 0.5f * globalDegree, true, gait, limbSwingAmount);
            walk(neck, 1.0f, 0.10f * globalDegree, false, 0, 0, gait, limbSwingAmount);
            walk(fore_right_upper, 1.0f, 0.75f * globalDegree, false, 0F, 0F, gait, limbSwingAmount);
            walk(fore_right_forearm, 1.0f, 0.45f * globalDegree, false, -1.4F, 0.15F, gait, limbSwingAmount);
            walk(fore_right_foot, 1.0f, 0.35f * globalDegree, true, -1.4F, 0F, gait, limbSwingAmount);
            walk(fore_left_upper, 1.0f, 0.75f * globalDegree, false, 3.1416F, 0F, gait, limbSwingAmount);
            walk(fore_left_forearm, 1.0f, 0.45f * globalDegree, false, 1.7416F, 0.15F, gait, limbSwingAmount);
            walk(fore_left_foot, 1.0f, 0.35f * globalDegree, true, 1.7416F, 0F, gait, limbSwingAmount);
            walk(hind_right_thigh, 1.0f, 0.70f * globalDegree, false, 1.5708F, 0F, gait, limbSwingAmount);
            walk(hind_right_shank, 1.0f, 0.45f * globalDegree, true, 0.1708F, 0F, gait, limbSwingAmount);
            walk(hind_right_foot, 1.0f, 0.30f * globalDegree, false, 0.1708F, 0F, gait, limbSwingAmount);
            walk(hind_left_thigh, 1.0f, 0.70f * globalDegree, false, 4.7124F, 0F, gait, limbSwingAmount);
            walk(hind_left_shank, 1.0f, 0.45f * globalDegree, true, 3.3124F, 0F, gait, limbSwingAmount);
            walk(hind_left_foot, 1.0f, 0.30f * globalDegree, false, 3.3124F, 0F, gait, limbSwingAmount);
        }

        if (dip.sitProgress > 0) {
            // SOLVED by grid search against real box geometry (diprotodont_spec.py --pose):
            // the belly drops 7.90u, every leg box stays above y=0, the lower
            // segments rest ON the floor, and the feet keep their toe-in (rotY) throughout.
            this.progressPosition(body_barrel, dip.sitProgress, 0.0F, 15.9000F, 0.0F, 40);
            this.progressRotation(fore_left_upper, dip.sitProgress, -1.2656F, -0.0F, 0, 40);
            this.progressRotation(fore_left_forearm, dip.sitProgress, -0.4187F, -0.0F, 0, 40);
            this.progressRotation(fore_left_foot, dip.sitProgress, 0.0168F, 0.2793F, 0, 40);
            this.progressRotation(fore_right_upper, dip.sitProgress, -1.2656F, -0.0F, 0, 40);
            this.progressRotation(fore_right_forearm, dip.sitProgress, -0.4187F, -0.0F, 0, 40);
            this.progressRotation(fore_right_foot, dip.sitProgress, 0.0168F, -0.2793F, 0, 40);
            this.progressRotation(hind_left_thigh, dip.sitProgress, -1.1342F, -0.0F, 0, 40);
            this.progressRotation(hind_left_shank, dip.sitProgress, 3.2699F, -0.0F, 0, 40);
            this.progressRotation(hind_left_foot, dip.sitProgress, -0.3031F, 0.2094F, 0, 40);
            this.progressRotation(hind_right_thigh, dip.sitProgress, -1.1342F, -0.0F, 0, 40);
            this.progressRotation(hind_right_shank, dip.sitProgress, 3.2699F, -0.0F, 0, 40);
            this.progressRotation(hind_right_foot, dip.sitProgress, -0.3031F, -0.2094F, 0, 40);
            // neck lifted 10 deg (solved minimum 2) so the jaw clears the floor
            this.progressRotation(neck, dip.sitProgress, -0.1396F, 0, 0, 40);
        }

        else if (dip.sleepProgress > 0) {
            // SOLVED by grid search against real box geometry (diprotodont_spec.py --pose):
            // the belly drops 8.70u, every leg box stays above y=0, the lower
            // segments rest ON the floor, and the feet keep their toe-in (rotY) throughout.
            this.progressPosition(body_barrel, dip.sleepProgress, 0.0F, 16.7000F, 0.0F, 40);
            this.progressRotation(fore_left_upper, dip.sleepProgress, -1.4342F, -0.0F, 0, 40);
            this.progressRotation(fore_left_forearm, dip.sleepProgress, -0.2128F, -0.0F, 0, 40);
            this.progressRotation(fore_left_foot, dip.sleepProgress, -0.0292F, 0.2793F, 0, 40);
            this.progressRotation(fore_right_upper, dip.sleepProgress, -1.4342F, -0.0F, 0, 40);
            this.progressRotation(fore_right_forearm, dip.sleepProgress, -0.2128F, -0.0F, 0, 40);
            this.progressRotation(fore_right_foot, dip.sleepProgress, -0.0292F, -0.2793F, 0, 40);
            this.progressRotation(hind_left_thigh, dip.sleepProgress, -1.2604F, -0.0F, 0, 40);
            this.progressRotation(hind_left_shank, dip.sleepProgress, 3.3903F, -0.0F, 0, 40);
            this.progressRotation(hind_left_foot, dip.sleepProgress, -0.3248F, 0.2094F, 0, 40);
            this.progressRotation(hind_right_thigh, dip.sleepProgress, -1.2604F, -0.0F, 0, 40);
            this.progressRotation(hind_right_shank, dip.sleepProgress, 3.3903F, -0.0F, 0, 40);
            this.progressRotation(hind_right_foot, dip.sleepProgress, -0.3248F, -0.2094F, 0, 40);
            // neck lifted 6 deg (solved minimum 6) so the jaw clears the floor
            this.progressRotation(neck, dip.sleepProgress, -0.0698F, 0, 0, 40);
            this.progressRotation(head_skull, dip.sleepProgress, 0.1047F, 0, 0, 40);
        }

    }
}
