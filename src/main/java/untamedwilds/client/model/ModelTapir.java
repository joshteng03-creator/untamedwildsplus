package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityTapir;

/**
 * Tapir -- 34 boxes on 256x128, authored from scratch. There was no donor rig: before this
 * class the only occurrence of "tapir" in the whole tree was a comment on
 * ModelMacrauchenia.java:165.
 *
 * Sculpted from scripts/tapir_spec.py, which carries the spec, the forward-kinematics
 * checker, the z-fight sweep, the volume-burial check, the UV shelf pack and the phase-7
 * pose solver. Box coords + texOffs are transcribed VERBATIM from the modded_entity export
 * (the Y-down flip is the export's, never re-derived); scripts/gen_model_tapir.py does the
 * transcription and scripts/verify_port.py proves it.
 *
 * Built as the deliberate OPPOSITE of ModelMacrauchenia so the two can never read as one
 * rig retuned: short-legged (belly at 50% of withers against the litoptern's 58%), ONE
 * short neck segment against three long ones, a wedge body that tapers hard to the chest
 * (hw 6.4 against the barrel's 8.5 -- a tapir is shaped to drive through undergrowth
 * head-first), a high rounded rump peaking 2.29u ABOVE the withers, and the head carried
 * LOW on a chain running -12/-18/-26 with the diagnostic prehensile proboscis drooping off
 * the muzzle.
 *
 * SEVEN species share this one mesh. tapir.json declares no synced flag and EntityTapir
 * adds none, so both toggles read getVariant() -- the ModelDeer:509-524 pattern. showModel
 * is applied to the chain ROOT, because ModelPart.render returns before recursing into
 * children when `visible` is false, so hiding a root hides its whole subtree:
 *   * CREST (the erect neck mane) is hidden on malayan and palorchestes.
 *   * CLAWS are shown ONLY on palorchestes. Palorchestes azael is a diprotodontid
 *     marsupial, not a perissodactyl -- it shares the tapir's body plan by convergence and
 *     is grouped here the way thylacine is grouped under dire_wolf. Its huge clawed
 *     forelimbs are the one character a shared mesh would otherwise erase, so they are
 *     modelled rather than left for the skin to fake.
 */
public class ModelTapir extends AdvancedEntityModel<EntityTapir> {

    private final AdvancedModelBox body_barrel;
    private final AdvancedModelBox body_withers;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head_skull;
    private final AdvancedModelBox head_muzzle;
    private final AdvancedModelBox proboscis_1;
    private final AdvancedModelBox proboscis_2;
    private final AdvancedModelBox head_jaw;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox crest;
    private final AdvancedModelBox fore_left_shoulder;
    private final AdvancedModelBox fore_left_forearm;
    private final AdvancedModelBox fore_left_cannon;
    private final AdvancedModelBox fore_left_foot;
    private final AdvancedModelBox claw_left;
    private final AdvancedModelBox fore_right_shoulder;
    private final AdvancedModelBox fore_right_forearm;
    private final AdvancedModelBox fore_right_cannon;
    private final AdvancedModelBox fore_right_foot;
    private final AdvancedModelBox claw_right;
    private final AdvancedModelBox body_croup;
    private final AdvancedModelBox tail;
    private final AdvancedModelBox hind_left_thigh;
    private final AdvancedModelBox hind_left_gaskin;
    private final AdvancedModelBox hind_left_cannon;
    private final AdvancedModelBox hind_left_foot;
    private final AdvancedModelBox hind_right_thigh;
    private final AdvancedModelBox hind_right_gaskin;
    private final AdvancedModelBox hind_right_cannon;
    private final AdvancedModelBox hind_right_foot;

    private final ModelAnimator animator;

    public ModelTapir() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_barrel = new AdvancedModelBox(this, 0, 0);
        this.body_barrel.setRotationPoint(0.0F, 9.0F, 3.5000F);
        this.body_barrel.addBox(-8.5000F, -4.5000F, -8.5000F, 17.0F, 10.0F, 17.0F, 0.0F);
        this.body_withers = new AdvancedModelBox(this, 68, 0);
        this.body_withers.setRotationPoint(0.0F, 0.0F, -6.5000F);
        this.body_withers.addBox(-7.6000F, -3.8000F, -6.0F, 15.2000F, 7.3000F, 12.5000F, 0.0F);
        this.setRotateAngle(body_withers, -0.1222F, 0.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 172, 0);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -8.5000F);
        this.body_chest.addBox(-6.4000F, -3.5000F, -3.0F, 12.8000F, 9.9000F, 7.0F, 0.0F);
        this.setRotateAngle(body_chest, 0.1396F, 0.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 212, 0);
        this.neck.setRotationPoint(0.0F, -0.5000F, -3.0F);
        this.neck.addBox(-4.6000F, -3.2000F, -4.6000F, 9.2000F, 7.2000F, 7.6000F, 0.0F);
        this.setRotateAngle(neck, 0.0698F, 0.0F, 0.0F);
        this.head_skull = new AdvancedModelBox(this, 0, 27);
        this.head_skull.setRotationPoint(0.0F, 0.0F, -4.6000F);
        this.head_skull.addBox(-3.8000F, -2.8000F, -6.4000F, 7.6000F, 6.2000F, 8.0F, 0.0F);
        this.setRotateAngle(head_skull, 0.1047F, 0.0F, 0.0F);
        this.head_muzzle = new AdvancedModelBox(this, 189, 27);
        this.head_muzzle.setRotationPoint(0.0F, 0.0F, -6.4000F);
        this.head_muzzle.addBox(-2.9000F, -2.4000F, -4.2000F, 5.8000F, 5.2000F, 5.8000F, 0.0F);
        this.setRotateAngle(head_muzzle, 0.1396F, 0.0F, 0.0F);
        this.proboscis_1 = new AdvancedModelBox(this, 38, 42);
        this.proboscis_1.setRotationPoint(0.0F, 0.7000F, -3.4000F);
        this.proboscis_1.addBox(-2.0F, -1.6000F, -4.2000F, 4.0F, 3.4000F, 4.8000F, 0.0F);
        this.setRotateAngle(proboscis_1, 0.7330F, 0.0F, 0.0F);
        this.proboscis_2 = new AdvancedModelBox(this, 56, 42);
        this.proboscis_2.setRotationPoint(0.0F, 0.8000F, -2.8000F);
        this.proboscis_2.addBox(-1.5000F, -1.6000F, -3.2000F, 3.0F, 3.4000F, 4.4000F, 0.0F);
        this.setRotateAngle(proboscis_2, 0.6283F, 0.0F, 0.0F);
        this.head_jaw = new AdvancedModelBox(this, 32, 27);
        this.head_jaw.setRotationPoint(0.0F, 2.5000F, -2.4000F);
        this.head_jaw.addBox(-2.5000F, -1.6000F, -6.0F, 5.0F, 3.8000F, 9.5000F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 223, 42);
        this.ear_left.setRotationPoint(2.4000F, -2.1000F, -0.4000F);
        this.ear_left.addBox(0.0F, -1.2000F, -1.2000F, 3.2000F, 2.0F, 2.6000F, 0.0F);
        this.setRotateAngle(ear_left, -0.1047F, -0.2443F, -0.6632F);
        this.ear_right = new AdvancedModelBox(this, 235, 42);
        this.ear_right.setRotationPoint(-2.4000F, -2.1000F, -0.4000F);
        this.ear_right.addBox(-3.2000F, -1.2000F, -1.2000F, 3.2000F, 2.0F, 2.6000F, 0.0F);
        this.setRotateAngle(ear_right, -0.1047F, 0.2443F, 0.6632F);
        this.eye_left = new AdvancedModelBox(this, 247, 42);
        this.eye_left.setRotationPoint(4.2000F, -1.3000F, -3.9000F);
        this.eye_left.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 251, 42);
        this.eye_right.setRotationPoint(-4.2000F, -1.3000F, -3.9000F);
        this.eye_right.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.crest = new AdvancedModelBox(this, 61, 27);
        this.crest.setRotationPoint(0.0F, -2.9000F, -1.5000F);
        this.crest.addBox(-1.1000F, -3.0F, -3.7000F, 2.2000F, 4.2000F, 8.7000F, 0.0F);
        this.fore_left_shoulder = new AdvancedModelBox(this, 137, 27);
        this.fore_left_shoulder.setRotationPoint(5.7000F, 4.0518F, 1.0F);
        this.fore_left_shoulder.addBox(-3.3000F, -1.2000F, -3.4000F, 6.6000F, 5.6000F, 6.2000F, 0.0F);
        this.setRotateAngle(fore_left_shoulder, -0.2793F, 0.0F, 0.0F);
        this.fore_left_forearm = new AdvancedModelBox(this, 0, 42);
        this.fore_left_forearm.setRotationPoint(-0.5000F, 4.4000F, 0.0F);
        this.fore_left_forearm.addBox(-2.2000F, -0.9000F, -2.6000F, 4.4000F, 3.7000F, 4.8000F, 0.0F);
        this.setRotateAngle(fore_left_forearm, 0.2793F, 0.0F, 0.0F);
        this.fore_left_cannon = new AdvancedModelBox(this, 115, 42);
        this.fore_left_cannon.setRotationPoint(-0.3000F, 2.8000F, 0.0F);
        this.fore_left_cannon.addBox(-1.1000F, -0.9000F, -1.9000F, 2.2000F, 3.2000F, 3.5000F, 0.0F);
        this.setRotateAngle(fore_left_cannon, -0.1920F, 0.0F, 0.0F);
        this.fore_left_foot = new AdvancedModelBox(this, 83, 42);
        this.fore_left_foot.setRotationPoint(0.0F, 2.3000F, 0.0F);
        this.fore_left_foot.addBox(-1.9000F, -0.7000F, -2.8000F, 3.8000F, 2.4000F, 3.8000F, 0.0F);
        this.setRotateAngle(fore_left_foot, 0.0524F, 0.0F, 0.0F);
        this.claw_left = new AdvancedModelBox(this, 195, 42);
        this.claw_left.setRotationPoint(0.0F, 0.4482F, -2.8000F);
        this.claw_left.addBox(-1.7000F, -0.9000F, -2.8000F, 3.4000F, 1.9000F, 3.4000F, 0.0F);
        this.fore_right_shoulder = new AdvancedModelBox(this, 163, 27);
        this.fore_right_shoulder.setRotationPoint(-5.7000F, 4.0518F, 1.0F);
        this.fore_right_shoulder.addBox(-3.3000F, -1.2000F, -3.4000F, 6.6000F, 5.6000F, 6.2000F, 0.0F);
        this.setRotateAngle(fore_right_shoulder, -0.2793F, 0.0F, 0.0F);
        this.fore_right_forearm = new AdvancedModelBox(this, 19, 42);
        this.fore_right_forearm.setRotationPoint(0.5000F, 4.4000F, 0.0F);
        this.fore_right_forearm.addBox(-2.2000F, -0.9000F, -2.6000F, 4.4000F, 3.7000F, 4.8000F, 0.0F);
        this.setRotateAngle(fore_right_forearm, 0.2793F, 0.0F, 0.0F);
        this.fore_right_cannon = new AdvancedModelBox(this, 127, 42);
        this.fore_right_cannon.setRotationPoint(0.3000F, 2.8000F, 0.0F);
        this.fore_right_cannon.addBox(-1.1000F, -0.9000F, -1.9000F, 2.2000F, 3.2000F, 3.5000F, 0.0F);
        this.setRotateAngle(fore_right_cannon, -0.1920F, 0.0F, 0.0F);
        this.fore_right_foot = new AdvancedModelBox(this, 99, 42);
        this.fore_right_foot.setRotationPoint(0.0F, 2.3000F, 0.0F);
        this.fore_right_foot.addBox(-1.9000F, -0.7000F, -2.8000F, 3.8000F, 2.4000F, 3.8000F, 0.0F);
        this.setRotateAngle(fore_right_foot, 0.0524F, 0.0F, 0.0F);
        this.claw_right = new AdvancedModelBox(this, 209, 42);
        this.claw_right.setRotationPoint(0.0F, 0.4482F, -2.8000F);
        this.claw_right.addBox(-1.7000F, -0.9000F, -2.8000F, 3.4000F, 1.9000F, 3.4000F, 0.0F);
        this.body_croup = new AdvancedModelBox(this, 124, 0);
        this.body_croup.setRotationPoint(0.0F, 0.0F, 6.0F);
        this.body_croup.addBox(-8.0F, -6.5000F, 0.0F, 16.0F, 11.5000F, 8.0F, 0.0F);
        this.setRotateAngle(body_croup, -0.3142F, 0.0F, 0.0F);
        this.tail = new AdvancedModelBox(this, 71, 42);
        this.tail.setRotationPoint(0.0F, -4.6000F, 7.1000F);
        this.tail.addBox(-1.5000F, -0.8000F, -0.7000F, 3.0F, 4.8000F, 2.7000F, 0.0F);
        this.setRotateAngle(tail, 0.5236F, 0.0F, 0.0F);
        this.hind_left_thigh = new AdvancedModelBox(this, 83, 27);
        this.hind_left_thigh.setRotationPoint(5.4000F, 3.6568F, 3.7000F);
        this.hind_left_thigh.addBox(-3.6000F, -1.4000F, -2.9000F, 7.2000F, 5.4000F, 6.1000F, 0.0F);
        this.setRotateAngle(hind_left_thigh, -0.0698F, 0.0F, 0.0F);
        this.hind_left_gaskin = new AdvancedModelBox(this, 213, 27);
        this.hind_left_gaskin.setRotationPoint(-0.5500F, 4.0F, 0.0F);
        this.hind_left_gaskin.addBox(-2.3500F, -1.2000F, -2.8000F, 4.7000F, 4.6000F, 5.2000F, 0.0F);
        this.setRotateAngle(hind_left_gaskin, 0.8727F, 0.0F, 0.0F);
        this.hind_left_cannon = new AdvancedModelBox(this, 139, 42);
        this.hind_left_cannon.setRotationPoint(-0.2500F, 3.4000F, 0.0F);
        this.hind_left_cannon.addBox(-1.2000F, -0.9000F, -1.9000F, 2.4000F, 3.0F, 3.5000F, 0.0F);
        this.setRotateAngle(hind_left_cannon, -0.6632F, 0.0F, 0.0F);
        this.hind_left_foot = new AdvancedModelBox(this, 163, 42);
        this.hind_left_foot.setRotationPoint(0.0F, 2.1000F, 0.0F);
        this.hind_left_foot.addBox(-2.0F, -0.7000F, -2.6000F, 4.0F, 2.3000F, 3.6000F, 0.0F);
        this.setRotateAngle(hind_left_foot, 0.1745F, 0.0F, 0.0F);
        this.hind_right_thigh = new AdvancedModelBox(this, 110, 27);
        this.hind_right_thigh.setRotationPoint(-5.4000F, 3.6568F, 3.7000F);
        this.hind_right_thigh.addBox(-3.6000F, -1.4000F, -2.9000F, 7.2000F, 5.4000F, 6.1000F, 0.0F);
        this.setRotateAngle(hind_right_thigh, -0.0698F, 0.0F, 0.0F);
        this.hind_right_gaskin = new AdvancedModelBox(this, 233, 27);
        this.hind_right_gaskin.setRotationPoint(0.5500F, 4.0F, 0.0F);
        this.hind_right_gaskin.addBox(-2.3500F, -1.2000F, -2.8000F, 4.7000F, 4.6000F, 5.2000F, 0.0F);
        this.setRotateAngle(hind_right_gaskin, 0.8727F, 0.0F, 0.0F);
        this.hind_right_cannon = new AdvancedModelBox(this, 151, 42);
        this.hind_right_cannon.setRotationPoint(0.2500F, 3.4000F, 0.0F);
        this.hind_right_cannon.addBox(-1.2000F, -0.9000F, -1.9000F, 2.4000F, 3.0F, 3.5000F, 0.0F);
        this.setRotateAngle(hind_right_cannon, -0.6632F, 0.0F, 0.0F);
        this.hind_right_foot = new AdvancedModelBox(this, 179, 42);
        this.hind_right_foot.setRotationPoint(0.0F, 2.1000F, 0.0F);
        this.hind_right_foot.addBox(-2.0F, -0.7000F, -2.6000F, 4.0F, 2.3000F, 3.6000F, 0.0F);
        this.setRotateAngle(hind_right_foot, 0.1745F, 0.0F, 0.0F);

        this.body_barrel.addChild(this.body_withers);
        this.body_barrel.addChild(this.body_chest);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head_skull);
        this.head_skull.addChild(this.head_muzzle);
        this.head_muzzle.addChild(this.proboscis_1);
        this.proboscis_1.addChild(this.proboscis_2);
        this.head_skull.addChild(this.head_jaw);
        this.head_skull.addChild(this.ear_left);
        this.head_skull.addChild(this.ear_right);
        this.head_skull.addChild(this.eye_left);
        this.head_skull.addChild(this.eye_right);
        this.neck.addChild(this.crest);
        this.body_chest.addChild(this.fore_left_shoulder);
        this.fore_left_shoulder.addChild(this.fore_left_forearm);
        this.fore_left_forearm.addChild(this.fore_left_cannon);
        this.fore_left_cannon.addChild(this.fore_left_foot);
        this.fore_left_foot.addChild(this.claw_left);
        this.body_chest.addChild(this.fore_right_shoulder);
        this.fore_right_shoulder.addChild(this.fore_right_forearm);
        this.fore_right_forearm.addChild(this.fore_right_cannon);
        this.fore_right_cannon.addChild(this.fore_right_foot);
        this.fore_right_foot.addChild(this.claw_right);
        this.body_barrel.addChild(this.body_croup);
        this.body_croup.addChild(this.tail);
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
            proboscis_1,
            proboscis_2,
            head_jaw,
            ear_left,
            ear_right,
            eye_left,
            eye_right,
            crest,
            fore_left_shoulder,
            fore_left_forearm,
            fore_left_cannon,
            fore_left_foot,
            claw_left,
            fore_right_shoulder,
            fore_right_forearm,
            fore_right_cannon,
            fore_right_foot,
            claw_right,
            body_croup,
            tail,
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
        EntityTapir tapir = (EntityTapir) entityIn;
        animator.update(tapir);

        // ATTACK_THREATEN (50t) -- a tapir has no horns, no tusks and no reach, so the
        // display is bulk and noise: the head drops, the forehand swings side to side and
        // the animal rocks its weight between the forelegs. The head goes DOWN rather than
        // up, which is the opposite of the macrauchenia's stacked-neck camelid display.
        animator.setAnimation(EntityTapir.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_chest, 0, 0, 6.0F);
            this.rotate(animator, neck, 10.0F, -14.0F, -6.0F);
            this.rotate(animator, head_skull, 6.0F, -12.0F, 0);
            this.rotate(animator, fore_left_shoulder, -16.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, 13.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_chest, 0, 0, -10.0F);
            this.rotate(animator, neck, 10.0F, 14.0F, 6.0F);
            this.rotate(animator, head_skull, 6.0F, 12.0F, 0);
            this.rotate(animator, fore_left_shoulder, 13.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, -16.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_GORE (14t) -- misnamed by the shared Animation field. A tapir's actual
        // weapon is its BITE: it has a full set of incisors and a canine on each jaw, and
        // real tapirs bite hard enough to take fingers off. So this rears the head back and
        // drives it forward and down, with the jaw dropping open on the wind-up and
        // snapping shut on the strike.
        animator.setAnimation(EntityTapir.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck, -18.0F, 0, 0);
        this.rotate(animator, head_skull, -22.0F, 0, 0);
        this.rotate(animator, head_jaw, 26.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, 26.0F, 0, 0);
        this.rotate(animator, head_skull, 30.0F, 0, 0);
        this.rotate(animator, head_jaw, -4.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityTapir tapir, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(tapir);
        float globalSpeed = 1.5f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- per-species toggles (getVariant(), the ModelDeer:509-524 pattern) ----
        // showModel on the chain ROOT: ModelPart.render returns before recursing
        // into children when `visible` is false, so this hides the whole subtree.
        int variant = tapir.getVariant();
        // crest ON for [0, 2, 3, 4, 5]; malayan and palorchestes carry no erect mane
        this.crest.showModel = variant == 0 || variant == 2 || variant == 3 || variant == 4 || variant == 5;
        // claws ON for [6] (palorchestes) ALONE
        boolean clawed = variant == 6;
        this.claw_left.showModel = clawed;
        this.claw_right.showModel = clawed;

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.05F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        walk(neck, 0.32f * globalSpeed, 0.04f, false, 2.8F, 0.03F, ageInTicks / 20, 2);
        // The proboscis is soft tissue and constantly in motion -- a tapir uses it the way
        // an elephant uses a trunk. It swings faster and wider than anything bony.
        this.swing(proboscis_1, 0.26F, 0.20F, false, 0F, 0F, ageInTicks, 1);
        this.swing(proboscis_2, 0.26F, 0.32F, false, 1F, 0F, ageInTicks, 1);
        this.swing(ear_left, 0.18F, 0.24F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.18F, 0.24F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail, 0.12F, 0.16F, false, 0F, 0F, ageInTicks, 1);

        // Blinking: bury the eye planes inside the skull. y/z MUST keep the
        // constructor's values or the eye jumps across the face on every blink.
        if (!tapir.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(2.3100F, -1.3000F, -3.9000F);
            this.eye_right.setRotationPoint(-2.3100F, -1.3000F, -3.9000F);
        }

        // Head tracking. The neck is one short heavily-muscled segment, so it
        // contributes little and most of the turn happens at the skull -- the opposite of
        // the macrauchenia, which shares its yaw down a long four-link chain.
        if (!tapir.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 4, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head_skull);
        }

        // Tapirs are strong swimmers and dive to feed, so this branch matters more here
        // than on most of the herd herbivores.
        if (tapir.isInWater()) {
            this.setRotateAngle(head_skull, -0.1820F, 0.0F, 0.0F);
            if (!tapir.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(tapir.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // ---- gait: short-legged and short-strided, so the amplitudes stay LOW ----
        if (tapir.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.6f * globalDegree, true, f, limbSwingAmount);
            walk(neck, 0.8f * globalSpeed, 0.14f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head_skull, 0.8f * globalSpeed, 0.10f * globalDegree, true, 0, 0, f, limbSwingAmount);
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

        if (tapir.sitProgress > 0) {
            // SOLVED by grid search against real box geometry (tapir_spec.py --pose).
            // Nothing to inherit here -- tapir is a new type with no donor rig --
            // which is exactly how the SOP wants poses derived anyway.
            // Belly drops 7.90u; every leg box stays above y=0 and the
            // cannon + foot of all four limbs rest ON the floor.
            this.progressPosition(body_barrel, tapir.sitProgress, 0.0F, 16.9000F, 0.0F, 40);
            this.progressRotation(fore_left_shoulder, tapir.sitProgress, -1.7754F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, tapir.sitProgress, 0.8180F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, tapir.sitProgress, -0.7480F, 0, 0, 40);
            this.progressRotation(fore_left_foot, tapir.sitProgress, 0.1836F, 0, 0, 40);
            this.progressRotation(fore_right_shoulder, tapir.sitProgress, -1.7754F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, tapir.sitProgress, 0.8180F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, tapir.sitProgress, -0.7480F, 0, 0, 40);
            this.progressRotation(fore_right_foot, tapir.sitProgress, 0.1836F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, tapir.sitProgress, -3.2964F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, tapir.sitProgress, 3.6160F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, tapir.sitProgress, -1.0914F, 0, 0, 40);
            this.progressRotation(hind_left_foot, tapir.sitProgress, -0.2761F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, tapir.sitProgress, -3.2964F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, tapir.sitProgress, 3.6160F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, tapir.sitProgress, -1.0914F, 0, 0, 40);
            this.progressRotation(hind_right_foot, tapir.sitProgress, -0.2761F, 0, 0, 40);
            this.progressRotation(neck, tapir.sitProgress, -0.1745F, 0, 0, 40);
            this.progressRotation(head_skull, tapir.sitProgress, -0.1047F, 0, 0, 40);
        }

        else if (tapir.sleepProgress > 0) {
            // SOLVED by grid search against real box geometry (tapir_spec.py --pose).
            // Nothing to inherit here -- tapir is a new type with no donor rig --
            // which is exactly how the SOP wants poses derived anyway.
            // Belly drops 8.70u; every leg box stays above y=0 and the
            // cannon + foot of all four limbs rest ON the floor.
            this.progressPosition(body_barrel, tapir.sleepProgress, 0.0F, 17.7000F, 0.0F, 40);
            this.progressRotation(fore_left_shoulder, tapir.sleepProgress, -1.7154F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, tapir.sleepProgress, 3.1461F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, tapir.sleepProgress, -2.8048F, 0, 0, 40);
            this.progressRotation(fore_left_foot, tapir.sleepProgress, -0.1064F, 0, 0, 40);
            this.progressRotation(fore_right_shoulder, tapir.sleepProgress, -1.7154F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, tapir.sleepProgress, 3.1461F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, tapir.sleepProgress, -2.8048F, 0, 0, 40);
            this.progressRotation(fore_right_foot, tapir.sleepProgress, -0.1064F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, tapir.sleepProgress, -2.9278F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, tapir.sleepProgress, 3.2498F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, tapir.sleepProgress, -1.2746F, 0, 0, 40);
            this.progressRotation(hind_left_foot, tapir.sleepProgress, -0.0897F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, tapir.sleepProgress, -2.9278F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, tapir.sleepProgress, 3.2498F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, tapir.sleepProgress, -1.2746F, 0, 0, 40);
            this.progressRotation(hind_right_foot, tapir.sleepProgress, -0.0897F, 0, 0, 40);
            this.progressRotation(neck, tapir.sleepProgress, 0.3491F, 0, 0, 40);
            this.progressRotation(head_skull, tapir.sleepProgress, 0.4363F, 0, 0, 40);
            this.progressRotation(proboscis_1, tapir.sleepProgress, 0.2618F, 0, 0, 40);
        }

    }
}
