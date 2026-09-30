#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench `modded_entity` export into ModelDireWolf.java.

Never re-derive the Y-flip: every setRotationPoint / addBox / texOffs literal emitted
below is copied VERBATIM out of the export. The only numbers this script computes itself
are the phase-7 pose targets, and those come from dire_wolf_spec.pose_solve() -- solved
against real box geometry, never hand-guessed.

The animation bodies (IDLE_TALK / ATTACK_BITE / ATTACK_POUNCE and the walk/run/water
handling) are carried over UNCHANGED from the existing ModelDireWolf, because the rebuild
deliberately kept every part name those keyframes touch.

Usage:  python scripts/gen_model_dire_wolf.py <export.java> <ModelDireWolf.java>
"""
import math
import re
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import dire_wolf_spec as S                                          # noqa: E402

EXPORT = pathlib.Path(sys.argv[1])
OUT = pathlib.Path(sys.argv[2])

PART_RE = re.compile(
    r'PartDefinition (\w+) = (\w+)\.addOrReplaceChild\("(\w+)",\s*'
    r'CubeListBuilder\.create\(\)\.texOffs\((\d+), (\d+)\)'
    r'\.addBox\(([^)]*?), new CubeDeformation\(0\.0F\)\),\s*'
    r'PartPose\.(offset|offsetAndRotation)\(([^)]*)\)\);')


def nums(s):
    return [float(x.strip().rstrip("Ff")) for x in s.split(",") if x.strip()]


def parse():
    src = EXPORT.read_text()
    out = []
    for m in PART_RE.finditer(src):
        parent = m.group(2)
        box = nums(m.group(6))
        pose = nums(m.group(8))
        out.append(dict(name=m.group(3),
                        parent=None if parent == "partdefinition" else parent,
                        u=int(m.group(4)), v=int(m.group(5)),
                        box=box[:3], size=box[3:6],
                        off=pose[:3],
                        rot=pose[3:6] if len(pose) > 3 else [0.0, 0.0, 0.0]))
    return out


def f(x):
    return f"{x:.4f}F".replace(".0000F", ".0F")


# The four leg roots the pose solver drives, mapped onto the rig's part names.
POSE_PARTS = [f"{p}_{s}_{seg}"
              for p in ("arm", "leg") for s in ("left", "right")
              for seg in ("upper", "lower", "foot", "paw")]


def poses():
    """Absolute LOCAL rotations per part, in MC radians, for sit and sleep."""
    assert S.pose_verify(), "cached poses no longer fit the geometry; re-run --pose --resolve"
    solved = S.POSE_CACHE
    out = {}
    for (pose, prefix), (angles, dy, pitch) in solved.items():
        d = out.setdefault(pose, {"dy": dy, "pitch": pitch, "rot": {}})
        for side in ("left", "right"):
            for nm, r in S._leg_rot(prefix, side, angles, pitch).items():
                # Blockbench local X degrees -> MC local X radians (the verified flip).
                d["rot"][nm] = -math.radians(r[0])
    return out


HEADER = '''package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityDireWolf;

public class ModelDireWolf extends AdvancedEntityModel<EntityDireWolf> {

'''


def main():
    parts = parse()
    assert len(parts) == 39, f"expected 39 parts, export has {len(parts)}"
    by = {p["name"]: p for p in parts}
    for nm in POSE_PARTS:
        assert nm in by, f"pose solver drives {nm}, which the export does not contain"

    L = []
    L.append(HEADER.rstrip("\n"))
    for p in parts:
        L.append(f"    public AdvancedModelBox {p['name']};")
    L.append("")
    L.append("    private final ModelAnimator animator;")
    L.append("    private float tailX = -0.35F;")
    L.append("")
    L.append("    public ModelDireWolf() {")
    L.append("        this.texWidth = 128;")
    L.append("        this.texHeight = 128;")
    L.append("")
    L.append("        // Sculpted from scratch in Blockbench against scripts/dire_wolf_spec.py, which")
    L.append("        // solves the joint chain, checks ground contact, and sweeps for coincident faces")
    L.append("        // BEFORE anything is built. Every literal below is transcribed VERBATIM from the")
    L.append("        // modded_entity export -- the Blockbench viewport is Y-up and MC is Y-down, and")
    L.append("        // the export is the only thing that performs that flip correctly.")
    L.append("        //")
    L.append("        // The old 19-box rig read derpy for measurable reasons, all fixed here: a 7x7x5.5")
    L.append("        // CUBE skull (now 5.2 wide over a 9.2 long head = 0.56, the real canid ratio), an")
    L.append("        // 8x8x8 ruff wider than the torso it sat on, a 4x4 square muzzle, two equal 4u leg")
    L.append("        // stumps identical front and back (now four segments each with a real hock), a")
    L.append("        // single 7x7x11 torso slab (now chest + tuck-up + croup), 1u paper ears and a")
    L.append("        // straight tail board.")

    for p in parts:
        L.append("")
        L.append(f"        this.{p['name']} = new AdvancedModelBox(this, {p['u']}, {p['v']});")
        L.append(f"        this.{p['name']}.setRotationPoint("
                 f"{f(p['off'][0])}, {f(p['off'][1])}, {f(p['off'][2])});")
        L.append(f"        this.{p['name']}.addBox("
                 f"{f(p['box'][0])}, {f(p['box'][1])}, {f(p['box'][2])}, "
                 f"{f(p['size'][0])}, {f(p['size'][1])}, {f(p['size'][2])}, 0.0F);")
        if any(abs(v) > 1e-9 for v in p["rot"]):
            L.append(f"        this.setRotateAngle({p['name']}, "
                     f"{f(p['rot'][0])}, {f(p['rot'][1])}, {f(p['rot'][2])});")

    L.append("")
    for p in parts:
        if p["parent"]:
            L.append(f"        this.{p['parent']}.addChild(this.{p['name']});")

    L.append("")
    L.append("        animator = ModelAnimator.create();")
    L.append("        updateDefaultPose();")
    L.append("    }")
    L.append("")
    L.append("    @Override")
    L.append("    public Iterable<BasicModelPart> parts() {")
    L.append("        return ImmutableList.of(this.body_main);")
    L.append("    }")
    L.append("")
    L.append("    @Override")
    L.append("    public Iterable<AdvancedModelBox> getAllParts() {")
    names = [p["name"] for p in parts]
    L.append("        return ImmutableList.of(")
    for i in range(0, len(names), 5):
        chunk = ", ".join(names[i:i + 5])
        L.append(f"            {chunk}" + ("," if i + 5 < len(names) else ");"))
    L.append("    }")

    L.append(ANIMATE)

    P = poses()
    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        L.append(f"        // {pose.title()}ing Animation -- SOLVED by "
                 f"dire_wolf_spec.pose_solve() against the real box geometry, not guessed:")
        L.append(f"        // every segment is constrained above the floor and the foot + paw onto it.")
        L.append(f"        if (wolf.{key} > 0) {{")
        # Body drop: dy is a Blockbench +Y-up shift, MC rotationPointY grows DOWNWARD.
        dy = P[pose]["dy"]
        pitch = P[pose]["pitch"]
        body_y = 24.0 - (13.0 + dy)
        L.append(f"            this.progressPosition(body_main, wolf.{key}, "
                 f"0.0F, {f(body_y)}, 0.0F, 40);")
        if pose == "sleep":
            L.append(f"            this.progressRotation(body_main, wolf.{key}, "
                     "0.0F, 0.0F, -1.50255395F, 40);")
            L.append(f"            this.progressRotation(head_neck, wolf.{key}, "
                     "0.27314402F, 0.22759093F, 0.0F, 40);")
            L.append(f"            this.progressRotation(head_main, wolf.{key}, "
                     "0.45535640F, 0.0F, 0.0F, 40);")
        else:
            # Nose-up body pitch: a canid sits on its haunches with the chest UP, and the
            # leg angles below were solved WITH this pitch applied, so it is not optional.
            L.append(f"            this.progressRotation(body_main, wolf.{key}, "
                     f"{f(-math.radians(pitch))}, 0.0F, 0.0F, 40);")
            L.append(f"            this.progressRotation(head_neck, wolf.{key}, "
                     "-0.30F, 0.0F, 0.0F, 40);")
        for nm in POSE_PARTS:
            L.append(f"            this.progressRotation({nm}, wolf.{key}, "
                     f"{f(P[pose]['rot'][nm])}, 0.0F, 0.0F, 40);")
        L.append(f"            this.progressRotation(tail_1, wolf.{key}, "
                 + ("-0.9F" if pose == "sit" else "0.5F") + ", 0.0F, 0.0F, 40);")
        L.append("        }")
        L.append("")
    L.append("    }")
    L.append("}")

    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(parts)} parts, {len(L)} lines)")


# The animation + setupAnim bodies are carried over from the previous ModelDireWolf
# unchanged. The rebuild kept every part name these keyframes touch, which is the whole
# reason the rig was designed around the old names in the first place.
ANIMATE = r'''
    private void animate(IAnimatedEntity entityIn) {
        EntityDireWolf wolf = (EntityDireWolf) entityIn;
        animator.update(wolf);

        animator.setAnimation(EntityDireWolf.IDLE_TALK);
        animator.startKeyframe(10);
        this.rotate(animator, head_neck, -26.09F, 0, 0);
        this.rotate(animator, head_jaw, 26.09F, 0, 0);
        this.rotate(animator, head_main, -26.09F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(10);

        animator.setAnimation(EntityDireWolf.ATTACK_BITE);
        animator.startKeyframe(5);
        this.rotate(animator, head_main, -10.43F, 15.65F, -20.87F);
        this.rotate(animator, head_jaw, 57.39F, 0, 0);
        this.rotate(animator, head_neck, 44.35F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, head_main, -18.26F, -5.22F, 10.43F);
        this.rotate(animator, head_jaw, 57.39F, 0, 0);
        this.rotate(animator, head_neck, 20.87F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(5);

        animator.setAnimation(EntityDireWolf.ATTACK_POUNCE);
        animator.startKeyframe(12);
        this.rotate(animator, body_main, -18.26F, 0, 0);
        animator.move(body_main, 0, -2, 0);
        this.rotate(animator, head_jaw, 52.17F, 0, 0);
        this.rotate(animator, arm_left_upper, -20.87F, 0, -20.87F);
        this.rotate(animator, arm_left_lower, -60F, 0, 7.83F);
        this.rotate(animator, arm_right_upper, -20.87F, 0, 20.87F);
        this.rotate(animator, arm_right_lower, -60F, 0, -7.83F);
        this.rotate(animator, leg_left_upper, 15.65F, 0, -2.61F);
        animator.move(leg_left_lower, 0, -1.5F, 0);
        this.rotate(animator, leg_right_upper, 15.65F, 0, 2.61F);
        animator.move(leg_right_lower, 0, -1.5F, 0);
        this.rotate(animator, tail_1, -15.65F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(6);
        this.rotate(animator, body_main, 2.61F, 0, 0);
        this.rotate(animator, head_neck, -10.43F, -5.22F, 7.83F);
        this.rotate(animator, head_main, 0, 2.61F, -18.26F);
        this.rotate(animator, head_jaw, 52.17F, 0, 0);
        this.rotate(animator, arm_left_upper, -52.17F, -18.26F, -10.44F);
        this.rotate(animator, arm_left_lower, -60F, 0, 7.83F);
        this.rotate(animator, arm_right_upper, -52.17F, 18.26F, 10.44F);
        this.rotate(animator, arm_right_lower, -60F, 0, -7.83F);
        this.rotate(animator, leg_left_upper, 62.61F, 15.65F, -2.61F);
        this.rotate(animator, leg_right_upper, 62.61F, -15.65F, 2.61F);
        this.rotate(animator, tail_1, -15.65F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(10);
        this.rotate(animator, body_main, 23.48F, 0, 0);
        this.rotate(animator, head_main, 0, 2.61F, -18.26F);
        this.rotate(animator, arm_left_upper, 5.22F, 0, -15.65F);
        this.rotate(animator, arm_left_lower, -60F, 0, 23.48F);
        animator.move(arm_left_upper, 0, 1F, 0);
        this.rotate(animator, arm_right_upper, 5.22F, 0, 15.65F);
        this.rotate(animator, arm_right_lower, -60F, 0, -23.48F);
        animator.move(arm_right_upper, 0, 1F, 0);
        this.rotate(animator, leg_left_upper, 2.61F, -5.22F, -10.43F);
        this.rotate(animator, leg_right_upper, 2.61F, 5.22F, 10.43F);
        this.rotate(animator, tail_1, -15.65F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(6);
        this.rotate(animator, body_main, 7.83F, 0, 0);
        animator.move(body_main, 0, 2, 0);
        this.rotate(animator, head_main, 2.61F, 7.83F, -2.61F);
        this.rotate(animator, arm_left_upper, 39.13F, 0, -15.65F);
        animator.move(arm_left_upper, 0, 3, 0);
        this.rotate(animator, arm_left_lower, -60F, 0, 23.48F);
        this.rotate(animator, arm_right_upper, 39.13F, 0, 15.65F);
        animator.move(arm_right_upper, 0, 3, 0);
        this.rotate(animator, arm_right_lower, -60F, 0, -23.48F);
        this.rotate(animator, leg_left_upper, -5.22F, 0, -2.61F);
        this.rotate(animator, leg_right_upper, -5.22F, 0, 2.61F);
        this.rotate(animator, tail_1, -15.65F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(8);
    }

    public void setupAnim(EntityDireWolf wolf, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(wolf);
        float globalSpeed = 2.8f;
        float globalDegree = 1f;
        limbSwingAmount = Math.min(0.6F, limbSwingAmount * 2);
        limbSwing *= 0.5F;

        // Breathing Animation
        final double scaleX = Math.sin(ageInTicks * 1 / 20F);
        final double scaleY = Math.sin(ageInTicks / 16);
        this.body_main.setScale((float) (1F + scaleX * 0.08F), (float) (1F + scaleY * 0.06F), 1.0F);
        bob(body_main, 0.4F * globalSpeed, 0.03F, false, ageInTicks / 20, 2);
        bob(arm_right_upper, 0.4F * globalSpeed, 0.03F, false, -ageInTicks / 20, 2);
        bob(arm_left_upper, 0.4F * globalSpeed, 0.03F, false, -ageInTicks / 20, 2);
        bob(leg_right_upper, 0.4F * globalSpeed, 0.03F, false, -ageInTicks / 20, 2);
        bob(leg_left_upper, 0.4F * globalSpeed, 0.03F, false, -ageInTicks / 20, 2);

        // Blinking Animation - retract both eye planes into head_main so they are occluded.
        // The eye sits 0.35 PROUD of the skull side (|2.95| vs |2.60|), so pulling it to
        // |1.6| buries it a full unit deep and no sliver can poke through at any head
        // angle. NOTE the export flips X: eye_LEFT carries the POSITIVE offset in Java.
        if (!wolf.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(1.6F, -1.1F, -2.8F);
            this.eye_right.setRotationPoint(-1.6F, -1.1F, -2.8F);
        }

        // Head Tracking Animation
        if (!wolf.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, head_neck);
            this.faceTarget(netHeadYaw, headPitch, 3, head_main);
        }

        // Pitch/Yaw handler
        if (wolf.isInWater() && !wolf.isOnGround()) {
            limbSwing = ageInTicks / 3;
            limbSwingAmount = 0.5f;
            this.body_main.rotationPointY += 4; // Model offset to make the wolf "sink" in water (while not drowning)
            this.setRotateAngle(head_neck, -0.18203784098300857F, 0.0F, 0.0F);
            float pitch = Mth.clamp(wolf.getXRot() - 10, -25F, 25.0F);
            this.setRotateAngle(body_main, (float) (pitch * Math.PI / 180F), 0, 0);
        }

        // Movement Animation
        float newZ = Mth.lerp(0.4F, this.tailX, this.tail_1.defaultRotationX + (float)wolf.getCurrentSpeed() * 2);
        this.tail_1.rotateAngleX = newZ;
        this.tailX = newZ;
        if (wolf.canMove()) {
            if (wolf.getCurrentSpeed() > 0.06f || wolf.isAngry()) { // Running animation
                bob(body_main, 0.3F * globalSpeed, 0.5F, false, limbSwing, limbSwingAmount);
                walk(body_main, 0.3f * globalSpeed, 0.5f * globalDegree, true, 0.5F, 0f, limbSwing, limbSwingAmount);
                walk(head_neck, 0.3f * globalSpeed, -0.5f * globalDegree, true, 0.5F, 0f, limbSwing, limbSwingAmount);
                bob(arm_right_upper, 0.3F * globalSpeed, 0.8F, false, limbSwing, limbSwingAmount);
                walk(arm_right_upper, 0.3f * globalSpeed, globalDegree, true, 0F, 0f, limbSwing, limbSwingAmount);
                walk(arm_right_lower, 0.3f * globalSpeed, 0.6f * globalDegree, true, 0.2F, 0.2f, limbSwing, limbSwingAmount);
                walk(arm_right_foot, 0.3f * globalSpeed, 0.4f * globalDegree, true, 0.4F, 0.2f, limbSwing, limbSwingAmount);
                bob(arm_left_upper, 0.3F * globalSpeed, 0.8F, false, limbSwing, limbSwingAmount);
                walk(arm_left_upper, 0.3f * globalSpeed, globalDegree, true, 0.6F, 0f, limbSwing, limbSwingAmount);
                walk(arm_left_lower, 0.3f * globalSpeed, 0.6f * globalDegree, true, 0.8F, 0.2f, limbSwing, limbSwingAmount);
                walk(arm_left_foot, 0.3f * globalSpeed, 0.4f * globalDegree, true, 1.0F, 0.2f, limbSwing, limbSwingAmount);
                bob(leg_right_upper, 0.3F * globalSpeed, 0.8F, false, limbSwing, limbSwingAmount);
                walk(leg_right_upper, 0.3f * globalSpeed, globalDegree, true, 1.4F, 0f, limbSwing, limbSwingAmount);
                walk(leg_right_lower, 0.3f * globalSpeed, 0.6f * globalDegree, true, 1.6F, 0.2f, limbSwing, limbSwingAmount);
                walk(leg_right_foot, 0.3f * globalSpeed, 0.4f * globalDegree, true, 1.8F, 0.2f, limbSwing, limbSwingAmount);
                bob(leg_left_upper, 0.3F * globalSpeed, 0.8F, false, limbSwing, limbSwingAmount);
                walk(leg_left_upper, 0.3f * globalSpeed, globalDegree, true, 2F, 0f, limbSwing, limbSwingAmount);
                walk(leg_left_lower, 0.3f * globalSpeed, 0.6f * globalDegree, true, 2.2F, 0.2f, limbSwing, limbSwingAmount);
                walk(leg_left_foot, 0.3f * globalSpeed, 0.4f * globalDegree, true, 2.4F, 0.2f, limbSwing, limbSwingAmount);
            } else { // Walking Animation
                bob(body_main, 0.5F * globalSpeed, 0.2F, false, limbSwing, limbSwingAmount);
                walk(body_main, 0.5f * globalSpeed, 0.2f * globalDegree, true, 0.5F, 0f, limbSwing, limbSwingAmount);
                walk(head_neck, 0.5f * globalSpeed, -0.2f * globalDegree, true, 0.5F, 0f, limbSwing, limbSwingAmount);

                walk(arm_right_upper, 0.5f * globalSpeed, 0.6f * globalDegree, false, 2F, 0f, limbSwing, limbSwingAmount);
                walk(arm_right_lower, 0.5f * globalSpeed, 0.6f * globalDegree, false, 0F, -0.8f, limbSwing, limbSwingAmount * 1.2f);
                walk(arm_right_foot, 0.5f * globalSpeed, 0.35f * globalDegree, false, 5.4F, 0f, limbSwing, limbSwingAmount);
                walk(arm_left_upper, 0.5f * globalSpeed, 0.6f * globalDegree, false, 5.0F, 0f, limbSwing, limbSwingAmount);
                walk(arm_left_lower, 0.5f * globalSpeed, 0.6f * globalDegree, false, 3F, -0.8f, limbSwing, limbSwingAmount * 1.2f);
                walk(arm_left_foot, 0.5f * globalSpeed, 0.35f * globalDegree, false, 2.2F, 0f, limbSwing, limbSwingAmount);
                bob(leg_right_upper, 0.5F * globalSpeed, 0.6F, false, limbSwing, limbSwingAmount);
                walk(leg_right_upper, 0.5f * globalSpeed, 0.8F * globalDegree, true, 1F, 0f, limbSwing, limbSwingAmount);
                walk(leg_right_lower, 0.5f * globalSpeed, 0.4f * globalDegree, true, 1.2F, 0.2f, limbSwing, limbSwingAmount);
                walk(leg_right_foot, 0.5f * globalSpeed, 0.3f * globalDegree, true, 1.4F, 0.1f, limbSwing, limbSwingAmount);
                bob(leg_left_upper, 0.5F * globalSpeed, 0.6F, false, limbSwing, limbSwingAmount);
                walk(leg_left_upper, 0.5f * globalSpeed, 0.8F * globalDegree, true, 3.4F, 0f, limbSwing, limbSwingAmount);
                walk(leg_left_lower, 0.5f * globalSpeed, 0.4f * globalDegree, true, 3.6F, 0.2f, limbSwing, limbSwingAmount);
                walk(leg_left_foot, 0.5f * globalSpeed, 0.3f * globalDegree, true, 3.8F, 0.1f, limbSwing, limbSwingAmount);
            }
        }

'''


if __name__ == "__main__":
    main()
