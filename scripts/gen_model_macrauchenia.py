#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench `modded_entity` export into ModelMacrauchenia.

Never re-derive the Y-flip: every setRotationPoint / addBox / texOffs literal below is
copied VERBATIM out of the export.  The only numbers this script computes itself are the
phase-7 pose targets, and those come from macrauchenia_spec.pose_solve() -- grid-searched
against real box geometry, never hand-guessed.  That matters here specifically: the rig
being replaced is a raw ModelBison fork whose sit/sleep fold angles belong to an animal
with legs 6u shorter than these.

Usage:  python scripts/gen_model_macrauchenia.py <export.java> <ModelMacrauchenia.java>
"""
import math
import re
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import macrauchenia_spec as S                                       # noqa: E402

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
    assert len(parts) == 35, len(parts)
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
              "untamedwilds.entity.mammal.EntityMacrauchenia"):
        a(f"import {i};")
    a("""
/**
 * Full Blockbench rebuild -- 35 boxes on 256x128, replacing the 26-box / 128x64 rig that
 * was the LAST un-remodelled ModelBison fork in the mod. The old one still declared bison
 * horns, beard, forelock and limb wool (hidden or scaled to zero at runtime) with a
 * two-cube proboscis bolted onto the snout. None of it survives.
 *
 * Sculpted from scripts/macrauchenia_spec.py, which carries the spec, the forward-
 * kinematics checker, the z-fight sweep, the volume-burial check, the UV shelf pack and
 * the phase-7 pose solver. Box coords + texOffs are transcribed VERBATIM from the
 * modded_entity export (the Y-down flip is the export's, never re-derived);
 * scripts/gen_model_macrauchenia.py does the transcription.
 *
 * Litoptern anatomy the numbers are expressing: LONG-LEGGED and cursorial (belly at 58%
 * of withers, against the bison fork's ~45%), a long S-curved neck carried high and
 * camelid-like, a small narrow head that pops off a hard-tapering neck (4.8 -> 4.0 -> 3.2
 * against a 6.5 chest), four-segment legs on broad splayed three-toed feet, and -- the
 * diagnostic character -- the RETRACTED NASAL OPENING domed on the skull roof between the
 * orbits (head_nasal). The short tapir-like proboscis is what that retraction implies,
 * not the character itself.
 *
 * macrauchenia.json declares NO model flags AND EntityMacrauchenia has no synced flag, so
 * anything species-specific would have to route through getVariant(). Both species share
 * one mesh and differ only by JSON `scale`, which getMobSize() already applies -- so there
 * is deliberately no getVariant() switch in here: adding one would be dead code. Scales
 * are calibrated 1 block = 1 metre at the withers (1.018 = 1.80 m Macrauchenia
 * patachonica, 0.876 = 1.55 m Xenorhinotherium bahiense).
 */""")
    a("public class ModelMacrauchenia extends AdvancedEntityModel<EntityMacrauchenia> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelMacrauchenia() {")
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
        EntityMacrauchenia macrauchenia = (EntityMacrauchenia) entityIn;
        animator.update(macrauchenia);

        // ATTACK_THREATEN (50t) -- a camelid display, not a bovid one. Macrauchenia has no
        // horns and no incisor battery, so the threat is made of HEIGHT: the neck stacks
        // up and the head swings side to side above the aggressor, weight rocking between
        // the forelegs. The neck lifts rather than dropping, which is the opposite of the
        // toxodon's low broad-headed display and is the whole point of the silhouette.
        animator.setAnimation(EntityMacrauchenia.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_chest, 0, 0, 5.0F);
            this.rotate(animator, neck_base, -10.0F, -10.0F, -5.0F);
            this.rotate(animator, neck_mid, -8.0F, -12.0F, 0);
            this.rotate(animator, neck_top, -6.0F, -12.0F, 0);
            this.rotate(animator, head_skull, 6.0F, -14.0F, 0);
            this.rotate(animator, fore_left_shoulder, -14.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, 12.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_chest, 0, 0, -9.0F);
            this.rotate(animator, neck_base, -10.0F, 10.0F, 5.0F);
            this.rotate(animator, neck_mid, -8.0F, 12.0F, 0);
            this.rotate(animator, neck_top, -6.0F, 12.0F, 0);
            this.rotate(animator, head_skull, 6.0F, 14.0F, 0);
            this.rotate(animator, fore_left_shoulder, 12.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, -14.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_GORE (14t) -- misnamed by the shared Animation field, and deliberately
        // not a gore. Nothing on this skull can gore: no horn, no tusk, no procumbent
        // incisor. A litoptern strikes the way a camelid or an equid does, so this rears
        // the forehand and drives BOTH forefeet down and forward. EntityMacrauchenia
        // follows the hit with HoglinBase.hurtAndThrowTarget, and a descending forefoot
        // strike is the motion that actually explains a target being thrown.
        animator.setAnimation(EntityMacrauchenia.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, body_chest, -26.0F, 0, 0);
        this.rotate(animator, neck_base, 14.0F, 0, 0);
        this.rotate(animator, head_skull, 10.0F, 0, 0);
        this.rotate(animator, fore_left_shoulder, -60.0F, 0, 0);
        this.rotate(animator, fore_right_shoulder, -54.0F, 0, 0);
        this.rotate(animator, fore_left_forearm, -50.0F, 0, 0);
        this.rotate(animator, fore_right_forearm, -44.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, body_chest, 12.0F, 0, 0);
        this.rotate(animator, neck_base, -16.0F, 0, 0);
        this.rotate(animator, head_skull, -12.0F, 0, 0);
        this.rotate(animator, fore_left_shoulder, 34.0F, 0, 0);
        this.rotate(animator, fore_right_shoulder, 30.0F, 0, 0);
        this.rotate(animator, fore_left_forearm, 20.0F, 0, 0);
        this.rotate(animator, fore_right_forearm, 18.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }
""")

    a("    public void setupAnim(EntityMacrauchenia macrauchenia, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(macrauchenia);
        float globalSpeed = 1.5f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.05F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        // A long neck idles visibly. Amplitude decreases up the chain so the head rides
        // the sway instead of leading it -- the reverse reads as a bobbing puppet.
        walk(neck_base, 0.32f * globalSpeed, 0.05f, false, 2.8F, 0.04F, ageInTicks / 20, 2);
        walk(neck_mid, 0.32f * globalSpeed, 0.035f, false, 2.4F, 0.03F, ageInTicks / 20, 2);
        walk(neck_top, 0.32f * globalSpeed, 0.025f, false, 2.0F, 0.02F, ageInTicks / 20, 2);
        // The proboscis is soft tissue: it swings faster and wider than anything bony.
        this.swing(proboscis_1, 0.22F, 0.16F, false, 0F, 0F, ageInTicks, 1);
        this.swing(proboscis_2, 0.22F, 0.26F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail_dock, 0.10F, 0.14F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_tip, 0.10F, 0.20F, false, 1F, 0F, ageInTicks, 1);
        this.swing(ear_left, 0.16F, 0.20F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.16F, 0.20F, false, 1F, 0F, ageInTicks, 1);
""")
    a("        // Blinking: bury the eye planes inside the skull. y/z MUST keep the")
    a("        // constructor's values or the eye jumps across the face on every blink.")
    a("        if (!macrauchenia.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        // Head tracking. A long flexible neck turns along its whole length, so the
        // yaw is SHARED down the chain rather than dumped into the skull -- the opposite
        // of the short-necked toxodon. Larger divisors = smaller share.
        if (!macrauchenia.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 6, neck_base);
            this.faceTarget(netHeadYaw, headPitch, 5, neck_mid);
            this.faceTarget(netHeadYaw, headPitch, 4, neck_top);
            this.faceTarget(netHeadYaw, headPitch, 3, head_skull);
        }

        if (macrauchenia.isInWater()) {
            this.setRotateAngle(head_skull, -0.1820F, 0.0F, 0.0F);
            if (!macrauchenia.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(macrauchenia.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // ---- gait: long-legged and long-strided, so the swing amplitudes run HIGH ----
        if (macrauchenia.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.6f * globalDegree, true, f, limbSwingAmount);
            walk(neck_base, 0.8f * globalSpeed, 0.14f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(neck_mid, 0.8f * globalSpeed, 0.10f * globalDegree, false, 0.4F, 0, f, limbSwingAmount);
            walk(neck_top, 0.8f * globalSpeed, 0.08f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(head_skull, 0.8f * globalSpeed, 0.12f * globalDegree, true, 0, 0, f, limbSwingAmount);
            walk(fore_right_shoulder, -0.8f * globalSpeed, 1.4f * globalDegree, true, 0F, 1.4f, f, limbSwingAmount);
            walk(fore_right_forearm, -0.8f * globalSpeed, 1.4f * globalDegree, false, -1F, 1.4f, f, limbSwingAmount * 1.2f);
            walk(fore_right_cannon, -0.8f * globalSpeed, 0.9f * globalDegree, false, -2F, 0.9f, f, limbSwingAmount);
            walk(fore_left_shoulder, -0.8f * globalSpeed, 1.4f * globalDegree, true, 2F, 1.4f, f, limbSwingAmount);
            walk(fore_left_forearm, -0.8f * globalSpeed, 1.4f * globalDegree, false, 1F, 1.4f, f, limbSwingAmount * 1.2f);
            walk(fore_left_cannon, -0.8f * globalSpeed, 0.9f * globalDegree, false, 0F, 0.9f, f, limbSwingAmount);
            walk(hind_right_thigh, 0.8f * globalSpeed, 1.4f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(hind_right_gaskin, 0.8f * globalSpeed, 1.4f * globalDegree, true, 1.8F, 0, f, limbSwingAmount);
            walk(hind_right_cannon, 0.8f * globalSpeed, 0.9f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(hind_left_thigh, 0.8f * globalSpeed, 1.4f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(hind_left_gaskin, 0.8f * globalSpeed, 1.4f * globalDegree, true, -0.2F, 0, f, limbSwingAmount);
            walk(hind_left_cannon, 0.8f * globalSpeed, 0.9f * globalDegree, false, -1.2F, 0, f, limbSwingAmount);
        }
""")

    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        d = P[pose]
        head = ("        if (macrauchenia.sitProgress > 0) {" if pose == "sit"
                else "        else if (macrauchenia.sleepProgress > 0) {")
        a(head)
        a("            // SOLVED by grid search against real box geometry"
          " (macrauchenia_spec.py --pose),")
        a("            // NOT inherited from ModelBison the way the previous rig's were -- those")
        a("            // belong to an animal whose legs are 6u shorter than these.")
        a(f"            // Belly drops {abs(d['dy']):.2f}u; every leg box stays above y=0 and the")
        a("            // cannon + foot of all four limbs rest ON the floor.")
        a(f"            this.progressPosition(body_barrel, macrauchenia.{key}, 0.0F, "
          f"{f(barrel_y - d['dy'])}, 0.0F, 40);")
        for n in names:
            if n in d["rot"]:
                a(f"            this.progressRotation({n}, macrauchenia.{key}, "
                  f"{f(d['rot'][n])}, 0, 0, 40);")
        if pose == "sit":
            # Couched but alert: the neck stays up, which is exactly how camelids sit.
            a(f"            this.progressRotation(neck_base, macrauchenia.{key}, -0.1745F, 0, 0, 40);")
            a(f"            this.progressRotation(head_skull, macrauchenia.{key}, -0.1222F, 0, 0, 40);")
        else:
            # Asleep: the long neck folds back and the head comes down onto the flank,
            # which is what a sternally-recumbent long-necked ungulate actually does.
            a(f"            this.progressRotation(neck_base, macrauchenia.{key}, 0.4363F, 0, 0, 40);")
            a(f"            this.progressRotation(neck_mid, macrauchenia.{key}, 0.5236F, 0, 0, 40);")
            a(f"            this.progressRotation(neck_top, macrauchenia.{key}, 0.6109F, 0, 0, 40);")
            a(f"            this.progressRotation(head_skull, macrauchenia.{key}, -0.9599F, 0, 0, 40);")
        a("        }\n")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


if __name__ == "__main__":
    main()
