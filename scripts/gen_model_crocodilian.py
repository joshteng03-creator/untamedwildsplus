#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench export into ModelCrocodilian.java (Phase 4.8, 2026-11-14).

Box/pivot/texOffs literals VERBATIM from the modded_entity export. The only computed numbers are SOLVED by
crocodilian_spec.py: the belly-down sit and sleep poses and the basking gape (pose_solve), and Quinkana's erect stance
(ERECT: body lift, fore/hind limb drop, tail droop).

Usage:  python scripts/gen_model_crocodilian.py <export.java> <ModelCrocodilian.java>
"""
import math
import os
import re
import sys
import pathlib

os.environ.pop("CROC_ERECT", None)
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import crocodilian_spec as S                                         # noqa: E402

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
    out = []
    for m in PART_RE.finditer(EXPORT.read_text()):
        box, pose = nums(m.group(6)), nums(m.group(8))
        out.append(dict(name=m.group(3), parent=None if m.group(2) == "partdefinition" else m.group(2),
                        u=int(m.group(4)), v=int(m.group(5)), box=box[:3], size=box[3:6],
                        off=pose[:3], rot=pose[3:6] if len(pose) > 3 else [0.0, 0.0, 0.0]))
    return out


def f(x):
    return f"{x:.4f}F".replace(".0000F", ".0F")


def main():
    parts = parse()
    assert len(parts) == len(S.PARTS) == 45, (len(parts), len(S.PARTS))
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    sol = S.pose_solve(verbose=True)
    root_y = by["body_mid"]["off"][1]
    eye = by["eye_left"]["off"]
    lift, e_fore, e_hind, e_droop = S.ERECT
    print(f"erect: lift {lift:.2f} fore {e_fore:.1f} hind {e_hind:.1f} droop {e_droop:.1f}")

    # Quinkana's erect stance as Java deltas from the default pose (BB rz -> Java +rz, BB rx -> Java -rx)
    erect = []
    for prefix, d0, d1 in (("arm", S.FORE_DROP, e_fore), ("leg", S.HIND_DROP, e_hind)):
        for side, s in (("left", -1.0), ("right", 1.0)):
            erect.append((f"{prefix}_{side}_upper", "Z", math.radians(-s * d1) - math.radians(-s * d0)))
            erect.append((f"{prefix}_{side}_lower", "Z", math.radians(s * d1) - math.radians(s * d0)))
    prev0 = prev1 = S.HIPS_TILT
    for k, n in enumerate(S.TAILS):
        a0, a1 = S.TAIL_DROOP * S.TAIL_SHAPE[k], e_droop * S.TAIL_SHAPE[k]
        erect.append((n, "X", -math.radians(a1 - prev1) + math.radians(a0 - prev0)))
        prev0, prev1 = a0, a1
    species = S.SPECIES

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
              "untamedwilds.entity.reptile.EntityCrocodilian"):
        a(f"import {i};")
    a("""
/**
 * Crocodilians -- Nile, saltwater, mugger, black caiman, Quinkana, Voay (Phase 4.8, 2026-11-14): 45 boxes on 256x128,
 * authored in scripts/crocodilian_spec.py, ported VERBATIM by scripts/gen_model_crocodilian.py, proved by verify_port.py.
 *
 * A low, flat, wide trunk on short sprawling legs; a flat head with the eyes on raised sockets, the upper teeth along the
 * closed jaw; back armour; a tall, thin, crested tail. Per species, written EVERY frame (one model instance is shared):
 * snout width/depth (setScale on the snout and jaw chains, children included), Voay's horns, and Quinkana's ERECT stance
 * (solved: limbs tucked under, body raised, tail re-drooped), faded out as it lies down because progressRotation adds
 * to whatever pose is already set. Sit and sleep are SOLVED belly-down poses; basking lifts the head (upper jaw) while
 * the lower jaw stays on the ground. ATTACK_ROLL is the death roll: the whole animal spins about its long axis.
 */""")
    a("public class ModelCrocodilian extends AdvancedEntityModel<EntityCrocodilian> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelCrocodilian() {")
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
    a("        return ImmutableList.of(body_mid);")
    a("    }\n")
    a("    // Built once: getAllParts() runs every frame (2.8.0).")
    a("    private ImmutableList<AdvancedModelBox> allParts;")
    a("")
    a("    @Override")
    a("    public Iterable<AdvancedModelBox> getAllParts() {")
    a("        if (this.allParts == null) {")
    a("            this.allParts = ImmutableList.of(")
    a(",\n".join(f"                {n}" for n in names))
    a("            );")
    a("        }")
    a("        return this.allParts;")
    a("    }\n")
    a("""    private void animate(IAnimatedEntity entityIn) {
        EntityCrocodilian croc = (EntityCrocodilian) entityIn;
        animator.update(croc);

        // ATTACK_BITE (12t): the head lifts and the jaw gapes, then it snaps shut and the head drives down
        animator.setAnimation(EntityCrocodilian.ATTACK_BITE);
        animator.startKeyframe(5);
        this.rotate(animator, neck, -8.0F, 0, 0);
        this.rotate(animator, head, -16.0F, 0, 0);
        this.rotate(animator, jaw, 34.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(3);
        this.rotate(animator, neck, 6.0F, 0, 0);
        this.rotate(animator, head, 4.0F, 0, 0);
        this.rotate(animator, jaw, 0, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }
""")
    a("    public void setupAnim(EntityCrocodilian croc, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(croc);
        limbSwingAmount = Math.min(0.5F, limbSwingAmount);
        float amt = Math.min(1.0F, limbSwingAmount / 0.35F);
        float gait = limbSwing * 0.55F / Math.max(0.3F, croc.getMobSize() * croc.getScale());
        int variant = croc.getVariant();
""")
    a("        // ---- per species (every frame: one model instance is shared by every crocodilian) ----")
    a("        float sx, sy;")
    a("        switch (variant) {")
    for name, var, real, (sx, sy), erect_v, horns in species:
        a(f"            case {var} -> {{ sx = {f(sx)}; sy = {f(sy)}; }}   // {name}")
    a("            default -> { sx = 1.0F; sy = 1.0F; }")
    a("        }")
    a("        this.snout_base.setShouldScaleChildren(true);")
    a("        this.snout_base.setScale(sx, sy, 1.0F);")
    a("        this.jaw.setShouldScaleChildren(true);")
    a("        this.jaw.setScale(sx, sy, 1.0F);")
    horn_vars = [v for _, v, _, _, _, h in species if h]
    erect_vars = [v for _, v, _, _, e, _ in species if e]
    a(f"        boolean horns = {' || '.join(f'variant == {v}' for v in horn_vars)};")
    a("        this.horn_left.showModel = horns;")
    a("        this.horn_right.showModel = horns;\n")
    a("        // QUINKANA's erect stance (solved), faded out as it lies down -- progressRotation adds to the current pose")
    a(f"        if ({' || '.join(f'variant == {v}' for v in erect_vars)}) {{")
    a("            float stand = 1.0F - Math.max(croc.sitProgress, croc.sleepProgress) / 40.0F;")
    a(f"            this.body_mid.rotationPointY -= {f(lift)} * stand;")
    for n, axis, d in erect:
        a(f"            this.{n}.rotateAngle{axis} += {f(d)} * stand;")
    a("        }\n")
    a("""        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 26) * 0.025F);
        this.body_mid.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(tail_5, 0.05F, 0.08F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_6, 0.05F, 0.12F, false, -0.8F, 0F, ageInTicks, 1);
""")
    a(f"        // Closing the eyes: slide the eye planes {S.EYE_CLOSE_SHIFT} into their sockets; y/z keep the constructor's values.")
    a("        if (!croc.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] - math.copysign(S.EYE_CLOSE_SHIFT, eye[0]))}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] + math.copysign(S.EYE_CLOSE_SHIFT, eye[0]))}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        if (!croc.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        // ATTACK_ROLL -- the death roll: the whole animal spins twice about its long axis (it only fires in water)
        if (croc.getAnimation() == EntityCrocodilian.ATTACK_ROLL) {
            float t = (croc.getAnimationTick() + (ageInTicks - (int) ageInTicks)) / EntityCrocodilian.ATTACK_ROLL.getDuration();
            this.body_mid.rotateAngleZ += t * Mth.TWO_PI * 2.0F;
            this.neck.rotateAngleY += Mth.sin(t * Mth.TWO_PI * 4.0F) * 0.25F;
            this.tail_1.rotateAngleY += Mth.sin(t * Mth.TWO_PI * 4.0F) * 0.3F;
        }

        if (croc.isInWater() && !croc.isOnGround()) {
            // swimming: legs pressed back along the flanks, the tail sculls from side to side
            float sw = ageInTicks * 0.25F;
            this.arm_left_upper.rotateAngleY += 1.0F;
            this.arm_right_upper.rotateAngleY -= 1.0F;
            this.leg_left_upper.rotateAngleY += 1.0F;
            this.leg_right_upper.rotateAngleY -= 1.0F;
            this.tail_1.rotateAngleY += Mth.cos(sw) * 0.18F;
            this.tail_2.rotateAngleY += Mth.cos(sw - 0.7F) * 0.26F;
            this.tail_3.rotateAngleY += Mth.cos(sw - 1.4F) * 0.32F;
            this.tail_4.rotateAngleY += Mth.cos(sw - 2.1F) * 0.36F;
            this.tail_5.rotateAngleY += Mth.cos(sw - 2.8F) * 0.40F;
            this.tail_6.rotateAngleY += Mth.cos(sw - 3.5F) * 0.44F;
            this.body_mid.rotateAngleY += Mth.cos(sw + 0.7F) * 0.06F;
        }
        else if (croc.canMove()) {
            // ---- sprawling 'high walk': diagonal pairs swing about the vertical axis, the trunk and tail undulate ----
            float c = Mth.cos(gait) * amt, sn = Mth.sin(gait);
            this.arm_left_upper.rotateAngleY += c * 0.5F;
            this.arm_right_upper.rotateAngleY += c * 0.5F;
            this.leg_left_upper.rotateAngleY -= c * 0.5F;
            this.leg_right_upper.rotateAngleY -= c * 0.5F;
            this.arm_left_upper.rotateAngleZ -= Math.max(0, sn) * 0.25F * amt;
            this.leg_right_upper.rotateAngleZ += Math.max(0, sn) * 0.25F * amt;
            this.arm_right_upper.rotateAngleZ += Math.max(0, -sn) * 0.25F * amt;
            this.leg_left_upper.rotateAngleZ -= Math.max(0, -sn) * 0.25F * amt;
            this.body_mid.rotateAngleY += c * 0.07F;
            this.neck.rotateAngleY -= c * 0.08F;
            this.tail_1.rotateAngleY -= Mth.cos(gait - 0.6F) * 0.10F * amt;
            this.tail_2.rotateAngleY -= Mth.cos(gait - 1.2F) * 0.13F * amt;
            this.tail_3.rotateAngleY -= Mth.cos(gait - 1.8F) * 0.16F * amt;
            this.tail_4.rotateAngleY -= Mth.cos(gait - 2.4F) * 0.18F * amt;
            this.tail_5.rotateAngleY -= Mth.cos(gait - 3.0F) * 0.20F * amt;
            this.tail_6.rotateAngleY -= Mth.cos(gait - 3.6F) * 0.22F * amt;
        }
""")
    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        d = sol[pose]
        head = "        if (croc.sitProgress > 0) {" if pose == "sit" else "        else if (croc.sleepProgress > 0) {"
        a(head)
        a("            // SOLVED (crocodilian_spec.py --pose): belly on the ground, legs splayed with the feet flat, tail resting"
          + (", chin down, tail curled" if pose == "sleep" else ", head up"))
        a(f"            this.progressPosition(body_mid, croc.{key}, 0.0F, {f(root_y - d['dy'])}, 0.0F, 40);")
        for n in names:
            if n in d["over"]:
                rx, ry, rz = d["over"][n]
                a(f"            this.progressRotation({n}, croc.{key}, {f(-math.radians(rx))}, {f(-math.radians(ry))}, "
                  f"{f(math.radians(rz))}, 40);")
        if pose == "sit":
            g = math.radians(sol["gape"])
            a("            // BASKING: the head (upper jaw) lifts and the jaw counter-rotates, so the lower jaw stays on the ground")
            a("            if (croc.isBasking()) {")
            a(f"                this.head.rotateAngleX -= {f(g)} * croc.sitProgress / 40.0F;")
            a(f"                this.jaw.rotateAngleX += {f(g)} * croc.sitProgress / 40.0F;")
            a("            }")
        a("        }")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


if __name__ == "__main__":
    main()
