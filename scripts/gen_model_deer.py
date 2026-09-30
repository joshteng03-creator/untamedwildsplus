#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench `modded_entity` export into ModelDeer.java.

Never re-derive the Y-flip: every setRotationPoint / addBox / texOffs literal below is
copied VERBATIM out of the export. The only numbers this script computes itself are the
phase-7 pose targets, and those come from deer_spec.pose_solve() -- solved against real
box geometry, never hand-guessed.
"""
import math
import re
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import deer_spec as S                                             # noqa: E402

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
        name, parent = m.group(3), m.group(2)
        box = nums(m.group(6))
        pose = nums(m.group(8))
        out.append(dict(name=name,
                        parent=None if parent == "partdefinition" else parent,
                        u=int(m.group(4)), v=int(m.group(5)),
                        box=box[:3], size=box[3:6],
                        off=pose[:3], rot=pose[3:6] if len(pose) > 3 else [0.0, 0.0, 0.0]))
    return out


def f(x):
    return f"{x:.4f}F".replace(".0000F", ".0F")


def poses():
    """Absolute LOCAL rotations per part, in MC radians, for sit and sleep."""
    solved = S.pose_solve()
    out = {}
    for (pose, prefix), (angles, dy) in solved.items():
        d = out.setdefault(pose, {"dy": dy, "rot": {}})
        for side in ("left", "right"):
            for nm, r in S._leg_rot(prefix, side, angles).items():
                # Blockbench local X degrees -> MC local X radians (the verified flip).
                d["rot"][nm] = -math.radians(r[0])
    return out


def main():
    import io
    from contextlib import redirect_stdout
    with redirect_stdout(io.StringIO()):
        S.main()                      # fills S.ANTLER_SCALE via the size calibration
    parts = parse()
    assert len(parts) == 59, len(parts)
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    P = poses()
    barrel_y = by["body_barrel"]["off"][1]
    eye = by["eye_left"]["off"]

    L = []
    a = L.append
    a("package untamedwilds.client.model;\n")
    for i in ("com.github.alexthe666.citadel.animation.IAnimatedEntity",
              "com.github.alexthe666.citadel.client.model.AdvancedEntityModel",
              "com.github.alexthe666.citadel.client.model.AdvancedModelBox",
              "com.github.alexthe666.citadel.client.model.ModelAnimator",
              "com.github.alexthe666.citadel.client.model.basic.BasicModelPart",
              "com.google.common.collect.ImmutableList",
              "net.minecraft.util.Mth",
              "untamedwilds.entity.mammal.EntityDeer"):
        a(f"import {i};")
    a("""
/**
 * Full Blockbench rebuild -- 59 boxes on 256x128. The old model was a BISON FORK (28 boxes
 * on 128x64) that still carried head_horn_left/right, head_beard and body_hair with the
 * horns scaled to zero at runtime. None of that geometry survives.
 *
 * deer.json declares NO model flags, so every per-species difference is driven off
 * getVariant() -- the ModelEquid / ModelAntelope pattern:
 *     0 megaloceros  1 stag_moose  2 moose   -> PALMATE antlers
 *     3 wapiti       4 red_deer              -> BRANCHED antlers
 * Both racks hang off a shared pedicle, so ONE setScale + setShouldScaleChildren(true) on
 * pedicle_left/right sizes each family. A uniform scale changes size but never shape, so
 * PALMATE is authored at Megaloceros proportions and BRANCHED at wapiti proportions --
 * the biggest species of each family has to be the one drawn correctly.
 *
 * The wapiti / red-deer neck mane is PAINTED, not modelled: a mane box was the equid's
 * worst z-fight source. throat_bell (the moose dewlap) shows on stag_moose + moose only.
 */""")
    a("public class ModelDeer extends AdvancedEntityModel<EntityDeer> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelDeer() {")
    a("        this.texWidth = 256;")
    a("        this.texHeight = 128;\n")
    for p in parts:
        n = p["name"]
        a(f"        this.{n} = new AdvancedModelBox(this, {p['u']}, {p['v']});")
        a(f"        this.{n}.setRotationPoint({f(p['off'][0])}, {f(p['off'][1])}, {f(p['off'][2])});")
        a(f"        this.{n}.addBox({f(p['box'][0])}, {f(p['box'][1])}, {f(p['box'][2])}, "
          f"{f(p['size'][0])}, {f(p['size'][1])}, {f(p['size'][2])}, 0.0F);")
        if any(abs(r) > 1e-6 for r in p["rot"]):
            a(f"        this.setRotateAngle({n}, {f(p['rot'][0])}, {f(p['rot'][1])}, {f(p['rot'][2])});")
    a("")
    for p in parts:
        if p["parent"]:
            a(f"        this.{p['parent']}.addChild(this.{p['name']});")
    a("")
    a("        animator = ModelAnimator.create();")
    a("        updateDefaultPose();")
    a("    }\n")
    a("    @Override")
    a("    public Iterable<BasicModelPart> parts() {")
    a("        return ImmutableList.of(body_barrel);")
    a("    }\n")
    a("    @Override")
    a("    public Iterable<AdvancedModelBox> getAllParts() {")
    a("        return ImmutableList.of(")
    a(",\n".join(f"            {n}" for n in names))
    a("        );")
    a("    }\n")

    a("""    private void animate(IAnimatedEntity entityIn) {
        EntityDeer deer = (EntityDeer) entityIn;
        animator.update(deer);

        // ATTACK_THREATEN (50t) -- head lowered, rack presented, weight rocking side to side.
        animator.setAnimation(EntityDeer.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_withers, 0, 0, 7.83F);
            this.rotate(animator, neck_lower, 24.0F, 0, -10.0F);
            this.rotate(animator, neck_upper, 14.0F, 0, 0);
            this.rotate(animator, head_skull, 10.0F, 0, 0);
            this.rotate(animator, fore_left_shoulder, -22.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, 18.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_withers, 0, 0, -13.05F);
            this.rotate(animator, neck_lower, 24.0F, 0, 20.0F);
            this.rotate(animator, neck_upper, 14.0F, 0, 0);
            this.rotate(animator, head_skull, 10.0F, 0, 0);
            this.rotate(animator, fore_left_shoulder, 18.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, -22.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_GORE (14t) -- rear back, then drive the rack down and forward.
        animator.setAnimation(EntityDeer.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck_lower, -26.0F, 0, 0);
        this.rotate(animator, neck_upper, -18.0F, 0, 0);
        this.rotate(animator, head_skull, -14.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck_lower, 42.0F, 0, 0);
        this.rotate(animator, neck_upper, 30.0F, 0, 0);
        this.rotate(animator, head_skull, 22.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }
""")

    a("    public void setupAnim(EntityDeer deer, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(deer);
        float globalSpeed = 1.5f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- per-species toggles (deer.json has no flags, so this reads getVariant()) ----
        int variant = deer.getVariant();
        boolean palmate = variant <= 2;
        // showModel, NOT setScale: a scale of 0 does not reach children unless
        // setShouldScaleChildren(true) is set, which would fight the antler sizing below.
        this.palm_beam_left.showModel = palmate;
        this.palm_beam_right.showModel = palmate;
        this.beam_low_left.showModel = !palmate;
        this.beam_low_right.showModel = !palmate;
        this.throat_bell.showModel = (variant == 1 || variant == 2);

        // One scale on the shared pedicle drives the whole rack. Solved from each species'
        // real antler span: 3.50 / 2.00 / 1.80 / 1.25 / 0.85 m.
        float rack = ANTLER_SCALE[Mth.clamp(variant, 0, ANTLER_SCALE.length - 1)];
        this.pedicle_left.setShouldScaleChildren(true);
        this.pedicle_right.setShouldScaleChildren(true);
        this.pedicle_left.setScale(rack, rack, rack);
        this.pedicle_right.setScale(rack, rack, rack);

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.05F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        walk(neck_lower, 0.35f * globalSpeed, 0.035f, false, 2.8F, 0.05F, ageInTicks / 20, 2);
        this.swing(tail_dock, 0.10F, 0.12F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_tip, 0.10F, 0.18F, false, 1F, 0F, ageInTicks, 1);
""")
    a("        // Blinking: bury the eye planes inside the skull. y/z MUST keep the")
    a("        // constructor's values or the eye jumps across the face on every blink.")
    a("        if (!deer.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.62)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.62)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        // Head tracking -- a cervid neck is long and mobile, so it turns freely.
        if (!deer.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck_lower);
            this.faceTarget(netHeadYaw, headPitch, 3, neck_upper);
            this.faceTarget(netHeadYaw, headPitch, 3, head_skull);
        }

        if (deer.isInWater()) {
            this.setRotateAngle(head_skull, -0.1820F, 0.0F, 0.0F);
            if (!deer.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(deer.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // ---- gait ----
        if (deer.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.6f * globalDegree, true, f, limbSwingAmount);
            walk(neck_lower, 0.8f * globalSpeed, 0.2f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head_skull, 0.8f * globalSpeed, 0.15f * globalDegree, true, 0, 0, f, limbSwingAmount);
            walk(fore_right_shoulder, -0.8f * globalSpeed, 1.2f * globalDegree, true, 0F, 1.2f, f, limbSwingAmount);
            walk(fore_right_forearm, -0.8f * globalSpeed, 1.2f * globalDegree, false, -1F, 1.2f, f, limbSwingAmount * 1.2f);
            walk(fore_right_cannon, -0.8f * globalSpeed, 0.8f * globalDegree, false, -2F, 0.8f, f, limbSwingAmount);
            walk(fore_left_shoulder, -0.8f * globalSpeed, 1.2f * globalDegree, true, 2F, 1.2f, f, limbSwingAmount);
            walk(fore_left_forearm, -0.8f * globalSpeed, 1.2f * globalDegree, false, 1F, 1.2f, f, limbSwingAmount * 1.2f);
            walk(fore_left_cannon, -0.8f * globalSpeed, 0.8f * globalDegree, false, 0F, 0.8f, f, limbSwingAmount);
            walk(hind_right_thigh, 0.8f * globalSpeed, 1.2f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(hind_right_gaskin, 0.8f * globalSpeed, 1.2f * globalDegree, true, 1.8F, 0, f, limbSwingAmount);
            walk(hind_right_cannon, 0.8f * globalSpeed, 0.8f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(hind_left_thigh, 0.8f * globalSpeed, 1.2f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(hind_left_gaskin, 0.8f * globalSpeed, 1.2f * globalDegree, true, -0.2F, 0, f, limbSwingAmount);
            walk(hind_left_cannon, 0.8f * globalSpeed, 0.8f * globalDegree, false, -1.2F, 0, f, limbSwingAmount);
        }
""")

    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        d = P[pose]
        head = ("        if (deer.sitProgress > 0) {" if pose == "sit"
                else "        else if (deer.sleepProgress > 0) {")
        a(head)
        a(f"            // Solved by grid search against real box geometry (deer_spec.py --pose):")
        a(f"            // belly drops {abs(d['dy']):.2f}u, every leg box stays above y=0 and the")
        a(f"            // cannon + hoof of all four limbs rest ON the floor.")
        a(f"            this.progressPosition(body_barrel, deer.{key}, 0.0F, "
          f"{f(barrel_y - d['dy'])}, 0.0F, 40);")
        for n in names:
            if n in d["rot"]:
                a(f"            this.progressRotation({n}, deer.{key}, {f(d['rot'][n])}, 0, 0, 40);")
        if pose == "sleep":
            a("            this.progressRotation(neck_lower, deer.sleepProgress, 0.6981F, 0, 0, 40);")
            a("            this.progressRotation(neck_upper, deer.sleepProgress, -0.5236F, 0, 0, 40);")
            a("            this.progressRotation(head_skull, deer.sleepProgress, -0.6109F, 0, 0, 40);")
        a("        }\n")
    a("    }\n")
    a("    // megaloceros / stag_moose / moose / wapiti / red_deer")
    a("    private static final float[] ANTLER_SCALE = {"
      + ", ".join(f"{S.ANTLER_SCALE[n]}F" for n, *_ in S.SPECIES) + "};")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


if __name__ == "__main__":
    main()
