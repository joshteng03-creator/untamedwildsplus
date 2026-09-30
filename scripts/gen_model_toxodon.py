#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench `modded_entity` export into ModelToxodon.java.

Never re-derive the Y-flip: every setRotationPoint / addBox / texOffs literal below is
copied VERBATIM out of the export. The only numbers this script computes itself are the
phase-7 pose targets, and those come from toxodon_spec.pose_solve() -- solved by grid
search against real box geometry, never hand-guessed. That is the specific defect being
fixed here: the shipped ModelToxodon carries fold angles inherited from the old bison
fork, and says so at lines 244-246.

Usage:  python scripts/gen_model_toxodon.py <export.java> <ModelToxodon.java>
"""
import math
import re
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import toxodon_spec as S                                            # noqa: E402

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
    """Absolute LOCAL rotations per leg part, in MC radians, for sit and sleep."""
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
    parts = parse()
    assert len(parts) == 34, len(parts)
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
              "untamedwilds.entity.mammal.EntityToxodon"):
        a(f"import {i};")
    a("""
/**
 * Full Blockbench rebuild -- 34 boxes on 256x128, replacing the 19-box / 128x128 rig that
 * was forked off ModelBison. The old one had SINGLE-SEGMENT legs and, by its own comment,
 * sit/sleep fold angles inherited from that fork and never re-derived. None of it survives.
 *
 * Sculpted from scripts/toxodon_spec.py, which carries the spec, the forward-kinematics
 * checker, the z-fight sweep, the UV shelf pack and the phase-7 pose solver. Box coords +
 * texOffs are transcribed VERBATIM from the modded_entity export (the Y-down flip is the
 * export's, never re-derived); scripts/gen_model_toxodon.py does the transcription.
 *
 * Notoungulate anatomy the numbers are expressing: high withers over a deep chest with the
 * topline sloping DOWN to a low rump (withers 27.6u vs rump 21.7u in authoring space), a
 * big head carried LOW on a short thick neck, four-segment legs on broad three-toed feet
 * with the belly at ~36% of shoulder height, small high-set ears, high orbits, a heavy
 * mandible, and the diagnostic procumbent chisel incisor battery.
 *
 * toxodon.json declares NO model flags AND both species share one mesh -- Mixotoxodon
 * differs from Toxodon only in size, which getMobSize() already applies from the JSON
 * `scale`. So there is deliberately no getVariant() switch in here: adding one would be
 * dead code. Scales are calibrated 1 block = 1 metre at the shoulder (0.870 = 1.50 m,
 * 1.014 = 1.75 m), so Toxodon matches the registered 1.7x1.5 hitbox exactly.
 */""")
    a("public class ModelToxodon extends AdvancedEntityModel<EntityToxodon> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelToxodon() {")
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
        EntityToxodon toxodon = (EntityToxodon) entityIn;
        animator.update(toxodon);

        // ATTACK_THREATEN (50t) -- head swung low and side to side, weight rocking between
        // the forelegs. A toxodon has no horns, so the threat display is the broad head and
        // the incisor battery, not a rack: the head drops rather than lifting.
        animator.setAnimation(EntityToxodon.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_chest, 0, 0, 6.5F);
            this.rotate(animator, neck, 12.0F, -14.0F, -8.0F);
            this.rotate(animator, head_skull, 8.0F, -10.0F, 0);
            this.rotate(animator, fore_left_shoulder, -18.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, 15.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_chest, 0, 0, -11.0F);
            this.rotate(animator, neck, 12.0F, 14.0F, 8.0F);
            this.rotate(animator, head_skull, 8.0F, 10.0F, 0);
            this.rotate(animator, fore_left_shoulder, 15.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, -18.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_GORE (14t) -- rear the head back, then drive the incisors up and forward.
        // The upward finish is the point: the lower incisors are procumbent, so the animal
        // hooks with them from below rather than stabbing downward.
        animator.setAnimation(EntityToxodon.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck, -20.0F, 0, 0);
        this.rotate(animator, head_skull, -16.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, 34.0F, 0, 0);
        this.rotate(animator, head_skull, 26.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }
""")

    a("    public void setupAnim(EntityToxodon toxodon, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(toxodon);
        float globalSpeed = 1.5f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.05F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        walk(neck, 0.35f * globalSpeed, 0.03f, false, 2.8F, 0.04F, ageInTicks / 20, 2);
        this.swing(tail_dock, 0.10F, 0.14F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_tip, 0.10F, 0.20F, false, 1F, 0F, ageInTicks, 1);
""")
    a("        // Blinking: bury the eye planes inside the skull. y/z MUST keep the")
    a("        // constructor's values or the eye jumps across the face on every blink.")
    a("        if (!toxodon.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.62)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.62)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        // Head tracking. The neck is short and heavily muscled, so it contributes less
        // than a cervid's -- most of the turn happens at the skull.
        if (!toxodon.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 4, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head_skull);
        }

        if (toxodon.isInWater()) {
            this.setRotateAngle(head_skull, -0.1820F, 0.0F, 0.0F);
            if (!toxodon.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(toxodon.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // ---- gait: a heavy, short-strided walk, so the swing amplitudes stay low ----
        if (toxodon.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.6f * globalDegree, true, f, limbSwingAmount);
            walk(neck, 0.8f * globalSpeed, 0.16f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head_skull, 0.8f * globalSpeed, 0.12f * globalDegree, true, 0, 0, f, limbSwingAmount);
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
""")

    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        d = P[pose]
        head = ("        if (toxodon.sitProgress > 0) {" if pose == "sit"
                else "        else if (toxodon.sleepProgress > 0) {")
        a(head)
        a("            // SOLVED by grid search against real box geometry"
          " (toxodon_spec.py --pose),")
        a("            // NOT inherited from the old bison fork the way the previous rig's were:")
        a(f"            // belly drops {abs(d['dy']):.2f}u, every leg box stays above y=0 and the")
        a("            // cannon + foot of all four limbs rest ON the floor.")
        a(f"            this.progressPosition(body_barrel, toxodon.{key}, 0.0F, "
          f"{f(barrel_y - d['dy'])}, 0.0F, 40);")
        for n in names:
            if n in d["rot"]:
                a(f"            this.progressRotation({n}, toxodon.{key}, "
                  f"{f(d['rot'][n])}, 0, 0, 40);")
        if pose == "sit":
            a("            this.progressRotation(neck, toxodon.sitProgress, -0.2618F, 0, 0, 40);")
            a("            this.progressRotation(head_skull, toxodon.sitProgress, -0.1745F, 0, 0, 40);")
        else:
            # Sleeping: the head folds back and down onto the chest, which is what a
            # sternally-recumbent grazer actually does; the old rig left it upright.
            a("            this.progressRotation(neck, toxodon.sleepProgress, 0.5236F, 0, 0, 40);")
            a("            this.progressRotation(head_skull, toxodon.sleepProgress, -0.6981F, 0, 0, 40);")
        a("        }\n")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


if __name__ == "__main__":
    main()
