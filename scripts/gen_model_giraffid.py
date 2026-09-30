#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench `modded_entity` export into ModelGiraffid.java.

Never re-derive the Y-flip: every setRotationPoint / addBox / texOffs literal is copied
VERBATIM out of the export.  The only numbers computed here are

  * the phase-7 pose targets, grid-searched by giraffid_spec.pose_solve() against the real
    box geometry (including the giraffe's head-on-the-rump sleeping coil), and
  * the per-species neck factors, which are applied to the EXPORTED offsets rather than to
    anything re-measured -- see the long comment on the neck block below.

Both are read from the spec at generation time, so a later geometry tweak cannot leave
stale numbers behind in the Java.

Usage:  python scripts/gen_model_giraffid.py <export.java> <ModelGiraffid.java>
"""
import math
import re
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import giraffid_spec as S                                            # noqa: E402

EXPORT = pathlib.Path(sys.argv[1])
OUT = pathlib.Path(sys.argv[2])

PART_RE = re.compile(
    r'PartDefinition (\w+) = (\w+)\.addOrReplaceChild\("(\w+)",\s*'
    r'CubeListBuilder\.create\(\)\.texOffs\((\d+), (\d+)\)'
    r'\.addBox\(([^)]*?), new CubeDeformation\(0\.0F\)\),\s*'
    r'PartPose\.(offset|offsetAndRotation)\(([^)]*)\)\);')

# Boxes whose scale must NOT be written directly, because their parent is a family root
# carrying setShouldScaleChildren(true) and would otherwise scale them a second time.
INHERIT_SCALE = set(S.FAMILY_ROOTS.values())


def nums(s):
    return [float(x.strip().rstrip("Ff")) for x in s.split(",") if x.strip()]


def parse():
    src = EXPORT.read_text()
    out = []
    for m in PART_RE.finditer(src):
        box = nums(m.group(6))
        pose = nums(m.group(8))
        out.append(dict(name=m.group(3),
                        parent=None if m.group(2) == "partdefinition" else m.group(2),
                        u=int(m.group(4)), v=int(m.group(5)),
                        box=box[:3], size=box[3:6],
                        off=pose[:3], rot=pose[3:6] if len(pose) > 3 else [0.0, 0.0, 0.0]))
    return out


def f(x):
    return f"{x:.4f}F".replace(".0000F", ".0F")


def poses():
    solved = S.pose_solve()
    out = {}
    for (pose, prefix), (angles, dy) in solved.items():
        d = out.setdefault(pose, {"dy": dy, "rot": {}, "coil": {}})
        if prefix == "coil":
            # Blockbench local X degrees -> MC local X radians (the verified flip).
            d["coil"] = {nm: -math.radians(a) for nm, a in angles.items()}
            continue
        d["dy"] = dy
        for side in ("left", "right"):
            for nm, r in S._leg_rot(prefix, side, angles).items():
                d["rot"][nm] = -math.radians(r[0])
    return out


def main():
    parts = parse()
    assert len(parts) == len(S.PARTS), f"{len(parts)} exported vs {len(S.PARTS)} in spec"
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    P = poses()
    barrel_y = by["body_barrel"]["off"][1]
    eye = by["eye_left"]["off"]

    # Children of each neck segment, with their exported offsets -- this is what the
    # per-species neck factor is applied to.
    neck_children = [(p["name"], p["parent"], p["off"])
                     for p in parts if p["parent"] in S.NECK_SEGS]

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
              "untamedwilds.entity.mammal.EntityGiraffid"):
        a(f"import {i};")
    a("""
/**
 * Giraffid -- 50 boxes on 256x128, replacing a raw ModelBison fork.
 *
 * The class this supersedes was the bison body with two 1x3x1 sticks bolted to the skull as
 * ossicones; it still declared the bison's horns (hidden with setScale(0,0,0) every frame),
 * beard, forelock and limb wool, and its sit/sleep fold angles were ModelBison's applied
 * unchanged to an animal whose legs are three times as long.
 *
 * Sculpted from scripts/giraffid_spec.py, which carries the spec, the forward-kinematics
 * checker, the z-fight sweep, the volume-burial check, a midline-crossing check, the UV
 * shelf pack and the phase-7 pose solver. Box coords + texOffs are transcribed VERBATIM
 * from the modded_entity export (the Y-down flip is the export's, never re-derived);
 * scripts/gen_model_giraffid.py does the transcription and scripts/verify_port.py proves it.
 *
 * Three things carry the family and the rig is built around them:
 *   * THE NECK -- four segments at 64/73/82/62 world degrees, curving back as it rises and
 *     then forward into the head. 25.8u of neck on a 38.5u withers.
 *   * THE DOWNHILL TOPLINE -- the withers stands 6.43u above the rump and the back falls
 *     away continuously. This is the exact inverse of ModelTapir's high rounded croup, and
 *     it is what stops the two rigs reading as one body retuned.
 *   * LONG STRAIGHT LEGS -- belly at 61% of withers against the tapir's 47%, and the fore
 *     drop deliberately exceeds the hind by 3.6u, which is mechanically WHY the back slopes.
 *
 * SEVEN species share this one mesh. giraffid.json declares no synced flag beyond
 * groupCount and EntityGiraffid adds none, so both mechanisms read getVariant().
 */""")
    a("public class ModelGiraffid extends AdvancedEntityModel<EntityGiraffid> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelGiraffid() {")
    a(f"        this.texWidth = {S.CANVAS[0]};")
    a(f"        this.texHeight = {S.CANVAS[1]};\n")
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
    a("        // The palmate ossicone is a base + palm CHAIN, so one setScale on the base has")
    a("        // to carry the palm with it. AdvancedModelBox.render divides the scale back out")
    a("        // before recursing into children unless this flag is set, which is why the")
    a("        // mammoth's hasLargeTusks chain needed it too.")
    for root in sorted(S.FAMILY_ROOTS):
        a(f"        this.{root}.setShouldScaleChildren(true);")
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

    # ------------------------------------------------------------------ animate
    a("""    private void animate(IAnimatedEntity entityIn) {
        EntityGiraffid giraffid = (EntityGiraffid) entityIn;
        animator.update(giraffid);

        // ATTACK_THREATEN (50t) -- the broadside display. A giraffid does not lower its head
        // to threaten; it does the opposite and stands as TALL as it can, stretching the neck
        // to full height and swaying it slowly while stiffening the forelegs. Height is the
        // whole point of the animal, so height is the display.
        animator.setAnimation(EntityGiraffid.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, neck_1, -9.0F, -11.0F, 0);
            this.rotate(animator, neck_2, -6.0F, -9.0F, 0);
            this.rotate(animator, neck_3, -4.0F, -7.0F, 0);
            this.rotate(animator, head_skull, -8.0F, -9.0F, 0);
            this.rotate(animator, fore_left_shoulder, -7.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, 5.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, neck_1, -9.0F, 11.0F, 0);
            this.rotate(animator, neck_2, -6.0F, 9.0F, 0);
            this.rotate(animator, neck_3, -4.0F, 7.0F, 0);
            this.rotate(animator, head_skull, -8.0F, 9.0F, 0);
            this.rotate(animator, fore_left_shoulder, 5.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, -7.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_GORE (14t) -- NECKING, which is what the ossicones are actually for. A
        // giraffid fights by swinging its whole neck like a sledgehammer and landing the
        // ossicones as the head comes round; bulls knock each other down with it. The
        // wind-up rears the neck back and up, the strike drives it down and across. Rolling
        // the segments (rz) as well as pitching them is what makes it read as a swing rather
        // than a nod, and it is also the only motion on the rig that uses the ossicones as a
        // weapon -- which matters most for sivatherium and bramatherium, whose whole
        // identity is the size of them.
        animator.setAnimation(EntityGiraffid.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck_1, -14.0F, 0, -22.0F);
        this.rotate(animator, neck_2, -10.0F, 0, -18.0F);
        this.rotate(animator, neck_3, -8.0F, 0, -14.0F);
        this.rotate(animator, neck_4, -6.0F, 0, -10.0F);
        this.rotate(animator, head_skull, -18.0F, 0, -12.0F);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck_1, 20.0F, 0, 30.0F);
        this.rotate(animator, neck_2, 16.0F, 0, 26.0F);
        this.rotate(animator, neck_3, 13.0F, 0, 21.0F);
        this.rotate(animator, neck_4, 10.0F, 0, 16.0F);
        this.rotate(animator, head_skull, 26.0F, 0, 18.0F);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }
""")

    # ------------------------------------------------------------------ setupAnim
    a("    public void setupAnim(EntityGiraffid giraffid, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(giraffid);
        float globalSpeed = 1.5f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        int variant = giraffid.getVariant();
""")

    # ---- ossicone families ---------------------------------------------
    a("        // ================= OSSICONE FAMILY (showModel on the chain ROOT) =========")
    a("        // ModelPart.render returns before recursing into children when `visible` is")
    a("        // false, so hiding a root hides its whole subtree -- the ModelAntelope")
    a("        // horn-family mechanism. Four families plus hornless:")
    for fam, boxes in S.FAMILIES.items():
        sp = [n for n, _, _, _, fm, _ in S.SPECIES if fm == fam]
        a(f"        //   {fam:<8} {', '.join(sp) if sp else '(none)'}")
    for fam in ("TAPERED", "PALMATE", "SPIKE", "QUAD"):
        vs = [v for _, v, _, _, fm, _ in S.SPECIES if fm == fam]
        if not vs:
            continue
        a(f"        boolean oss{fam.capitalize()} = "
          + " || ".join(f"variant == {v}" for v in vs) + ";")
    a("")
    for fam in ("TAPERED", "PALMATE", "SPIKE", "QUAD"):
        for n in S.FAMILIES[fam]:
            a(f"        this.{n}.showModel = oss{fam.capitalize()};")
    a("")
    a("        // Per-species ossicone SIZE. Written for every family's boxes unconditionally,")
    a("        // not just the visible one, and that is deliberate -- see the neck block below")
    a("        // for why an unwritten scale is a bug rather than a no-op.")
    a("        float oss;")
    a("        switch (variant) {")
    seen = {}
    for name, v, _, _, fam, sc in S.SPECIES:
        seen.setdefault(sc, []).append((v, name))
    for sc, group in sorted(seen.items(), reverse=True):
        for v, name in group[:-1]:
            a(f"            case {v}:   // {name}")
        v, name = group[-1]
        a(f"            case {v}: oss = {f(sc if sc > 0 else 1.0)}; break;   // {name}")
    a("            default: oss = 1.0F; break;")
    a("        }")
    for fam in ("TAPERED", "PALMATE", "SPIKE", "QUAD"):
        for n in S.FAMILIES[fam]:
            if n in INHERIT_SCALE:
                a(f"        // {n} inherits its scale from its base (setShouldScaleChildren)")
                continue
            a(f"        this.{n}.setScale(oss, oss, oss);")
    a("")

    # ---- neck length ---------------------------------------------------
    a("""        // ================= PER-SPECIES NECK LENGTH ==============================
        // This is the mechanism that lets one mesh carry a 1.5m okapi and a 3.3m giraffe.
        // Species `scale` in giraffid.json already handles absolute size; what it cannot
        // express is how long the neck is FOR an animal that height, which is the only
        // proportion that actually separates the giraffids. nf is a ratio of ratios:
        // (species neck / species withers) over the same quotient for the giraffe.
        //
        // Each segment is scaled along its OWN local Z -- its length axis -- and every child
        // of a segment has its rotation point moved with it. AdvancedModelBox.translateAndRotate
        // applies translate(rotationPoint) -> rotZ -> rotY -> rotX -> scale, so the scale acts
        // in the part's own post-rotation frame: scaling Z shortens the segment along its
        // length with NO shear, whatever angle it sits at. render() then divides the scale
        // back out before recursing into children (scaleChildren is false here), which is
        // exactly what makes moving the child pivots by hand the correct thing to do.
        //
        // Using setShouldScaleChildren on the chain root instead would look tidier and be
        // wrong: a non-uniform scale propagated into children that are themselves rotated
        // shears every segment diagonally, and the result still reads as vaguely neck-shaped.
        //
        // EVERY VALUE BELOW IS WRITTEN UNCONDITIONALLY, INCLUDING nf == 1.0 FOR THE GIRAFFE.
        // resetToDefaultPose() restores rotateAngle* and rotationPoint* and does NOT touch
        // scaleX/Y/Z, and one ModelGiraffid instance renders every giraffid in the world --
        // so a `if (nf != 1.0F)` guard would leave a giraffe wearing the okapi's neck for as
        // long as it was rendered after one.
        //
        // The XY thicken runs the other way from nf, because a short giraffid neck is a bull
        // neck rather than a thin one. The mane's pivot is thickened with it so the blade
        // stays on the surface it sits on.""")
    a("        float nf;")
    seen = {}
    for name, v, _, nf, _, _ in S.SPECIES:
        seen.setdefault(nf, []).append((v, name))
    a("        switch (variant) {")
    for nf, group in sorted(seen.items(), reverse=True):
        for v, name in group[:-1]:
            a(f"            case {v}:   // {name}")
        v, name = group[-1]
        a(f"            case {v}: nf = {f(nf)}; break;   // {name}")
    a("            default: nf = 1.0F; break;")
    a("        }")
    a("        float nthick = 1.0F + (1.0F - nf) * 0.45F;")
    for nm in S.NECK_SEGS:
        a(f"        this.{nm}.setScale(nthick, nthick, nf);")
    for nm, par, off in neck_children:
        oy = "0.0F" if abs(off[1]) < 1e-6 else f"{f(off[1])} * nthick"
        oz = "0.0F" if abs(off[2]) < 1e-6 else f"{f(off[2])} * nf"
        a(f"        this.{nm}.setRotationPoint({f(off[0])}, {oy}, {oz});")
    for nm, par, off in neck_children:
        if nm.startswith("mane_"):
            a(f"        this.{nm}.setScale(1.0F, 1.0F, nf);")
    a("")
    a("        // A SHORT-NECKED GIRAFFID MUST ALSO CARRY ITS HEAD LOWER, or it reads as")
    a("        // craning. Shortening the chain alone leaves the head at the giraffe's +36")
    a("        // degree world pitch, which on a 14u neck puts the muzzle sticking straight")
    a("        // out horizontally at shoulder height -- caught by rebuilding the rig at")
    a("        // nf 0.55 (GIRAFFID_NF in giraffid_spec.py) and looking, because Blockbench")
    a("        // never runs setupAnim and every other preview shows the giraffe's neck.")
    a("        // MC rotateAngleX is the NEGATED Blockbench angle under the export flip, so")
    a("        // ADDING here lowers the nose: up to 9 degrees at nf 0.55, nothing at 1.0.")
    a("        // Placed before the gait and head-tracking calls, which are additive on top.")
    a("        this.head_skull.rotateAngleX += (1.0F - nf) * 0.35F;")
    a("")

    # ---- idle ----------------------------------------------------------
    a("""        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.05F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        // The neck is a 26u lever, so even a small sway at the base is a large motion at the
        // head. Amplitudes fall as they go up the chain rather than accumulating.
        walk(neck_1, 0.22f * globalSpeed, 0.030f, false, 2.8F, 0.02F, ageInTicks / 20, 2);
        walk(neck_2, 0.22f * globalSpeed, 0.022f, false, 2.4F, 0.015F, ageInTicks / 20, 2);
        walk(neck_3, 0.22f * globalSpeed, 0.016f, false, 2.0F, 0.010F, ageInTicks / 20, 2);
        this.swing(ear_left, 0.16F, 0.26F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.16F, 0.26F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail_dock, 0.11F, 0.22F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_tassel, 0.11F, 0.34F, false, 1F, 0F, ageInTicks, 1);
""")
    a("        // Blinking: bury the eye planes inside the skull. y/z MUST keep the")
    a("        // constructor's values or the eye jumps across the face on every blink.")
    a("        if (!giraffid.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.5)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.5)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        // Head tracking, SHARED DOWN THE CHAIN. A giraffe turns to look by rotating the
        // whole neck, not by cranking the skull -- and dividing the turn four ways also keeps
        // any single joint from opening the 2.4u overlap into a visible wedge.
        if (!giraffid.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 8, neck_1);
            this.faceTarget(netHeadYaw, headPitch, 6, neck_2);
            this.faceTarget(netHeadYaw, headPitch, 5, neck_3);
            this.faceTarget(netHeadYaw, headPitch, 4, neck_4);
            this.faceTarget(netHeadYaw, headPitch, 3, head_skull);
        }

        if (giraffid.isInWater()) {
            this.setRotateAngle(head_skull, -0.1820F, 0.0F, 0.0F);
            if (!giraffid.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(giraffid.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // ---- gait: THE GIRAFFE PACES ----
        // A giraffe is one of the few large mammals that walks a PACE rather than a diagonal
        // walk: both legs on the same side swing together. It is as diagnostic as the neck
        // and it costs nothing to express, so fore_left shares hind_left's phase offset
        // (2F) and fore_right shares hind_right's (0F). Every other quadruped on this
        // codebase offsets the hind legs against the fore, which is the trot pattern -- do
        // not "fix" these to match them.
        if (giraffid.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.6f * globalDegree, true, f, limbSwingAmount);
            walk(neck_1, 0.8f * globalSpeed, 0.10f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(neck_4, 0.8f * globalSpeed, 0.08f * globalDegree, true, 1F, 0, f, limbSwingAmount);
            walk(head_skull, 0.8f * globalSpeed, 0.09f * globalDegree, true, 0, 0, f, limbSwingAmount);
            walk(fore_right_shoulder, -0.8f * globalSpeed, 1.1f * globalDegree, true, 0F, 1.1f, f, limbSwingAmount);
            walk(fore_right_forearm, -0.8f * globalSpeed, 1.1f * globalDegree, false, -1F, 1.1f, f, limbSwingAmount * 1.2f);
            walk(fore_right_cannon, -0.8f * globalSpeed, 0.7f * globalDegree, false, -2F, 0.7f, f, limbSwingAmount);
            walk(fore_left_shoulder, -0.8f * globalSpeed, 1.1f * globalDegree, true, 2F, 1.1f, f, limbSwingAmount);
            walk(fore_left_forearm, -0.8f * globalSpeed, 1.1f * globalDegree, false, 1F, 1.1f, f, limbSwingAmount * 1.2f);
            walk(fore_left_cannon, -0.8f * globalSpeed, 0.7f * globalDegree, false, 0F, 0.7f, f, limbSwingAmount);
            walk(hind_right_thigh, 0.8f * globalSpeed, 1.1f * globalDegree, false, 0F, 0, f, limbSwingAmount);
            walk(hind_right_gaskin, 0.8f * globalSpeed, 1.1f * globalDegree, true, -1F, 0, f, limbSwingAmount);
            walk(hind_right_cannon, 0.8f * globalSpeed, 0.7f * globalDegree, false, -2F, 0, f, limbSwingAmount);
            walk(hind_left_thigh, 0.8f * globalSpeed, 1.1f * globalDegree, false, 2F, 0, f, limbSwingAmount);
            walk(hind_left_gaskin, 0.8f * globalSpeed, 1.1f * globalDegree, true, 1F, 0, f, limbSwingAmount);
            walk(hind_left_cannon, 0.8f * globalSpeed, 0.7f * globalDegree, false, 0F, 0, f, limbSwingAmount);
        }
""")

    # ---- poses ---------------------------------------------------------
    coil = P["sleep"]["coil"]
    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        d = P[pose]
        head = ("        if (giraffid.sitProgress > 0) {" if pose == "sit"
                else "        else if (giraffid.sleepProgress > 0) {")
        a(head)
        a("            // SOLVED by grid search against real box geometry"
          " (giraffid_spec.py --pose).")
        a("            // The class this replaces used ModelBison's fold angles verbatim on legs")
        a("            // three times as long, which is the exact failure the SOP's phase 7 exists")
        a(f"            // to stop. Belly drops {abs(d['dy']):.2f}u; every leg box stays above y=0 and")
        a("            // the cannon + foot of all four limbs rest ON the floor.")
        a(f"            this.progressPosition(body_barrel, giraffid.{key}, 0.0F, "
          f"{f(barrel_y - d['dy'])}, 0.0F, 40);")
        for n in names:
            if n in d["rot"]:
                a(f"            this.progressRotation({n}, giraffid.{key}, "
                  f"{f(d['rot'][n])}, 0, 0, 40);")
        if pose == "sit":
            a(f"            this.progressRotation(neck_1, giraffid.{key}, -0.1396F, 0, 0, 40);")
            a(f"            this.progressRotation(head_skull, giraffid.{key}, 0.1745F, 0, 0, 40);")
        else:
            a("")
            a("            // THE SLEEPING COIL, and the giraffe alone can do it. A resting")
            a("            // giraffe folds its neck right round and lays its head on its own")
            a("            // rump; it is the animal's most recognisable posture. The angles are")
            a("            // solved, not drawn: the objective lands the muzzle tip ON the croup's")
            a("            // top surface while keeping every neck segment out of the barrel.")
            a("            // Gated on nf, because at 0.78 the neck is ~6u short of reaching and")
            a("            // the head would hang in mid air -- so every other species gets a")
            a("            // plain head-forward-and-down sleep instead.")
            a("            if (nf >= 0.95F) {")
            for nm in S.NECK_SEGS + ["head_skull", "head_muzzle"]:
                if nm in coil:
                    a(f"                this.progressRotation({nm}, giraffid.{key}, "
                      f"{f(coil[nm])}, 0, 0, 40);")
            a("            } else {")
            a(f"                this.progressRotation(neck_1, giraffid.{key}, 0.5236F, 0, 0, 40);")
            a(f"                this.progressRotation(neck_2, giraffid.{key}, 0.3491F, 0, 0, 40);")
            a(f"                this.progressRotation(neck_3, giraffid.{key}, 0.2618F, 0, 0, 40);")
            a(f"                this.progressRotation(head_skull, giraffid.{key}, 0.3491F, 0, 0, 40);")
            a("            }")
        a("        }\n")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    fams = {f: [n for n, _, _, _, fm, _ in S.SPECIES if fm == f] for f in S.FAMILIES}
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")
    for f_, sp in fams.items():
        print(f"  {f_:<8} {sp}")


if __name__ == "__main__":
    main()
