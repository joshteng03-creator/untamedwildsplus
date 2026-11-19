#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench export into ModelWildebeest.java (Phase 4.7, 2026-10-23).

Box/pivot/texOffs literals VERBATIM from the modded_entity export; the only computed numbers are the poses SOLVED by
wildebeest_spec.py: sit/sleep recumbency (pose_solve), the graze reach (graze_solve) and the kneeling clash (kneel_solve).

Usage:  python scripts/gen_model_wildebeest.py <export.java> <ModelWildebeest.java>
"""
import math
import re
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import wildebeest_spec as S                                        # noqa: E402

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


def deg(x):
    return f"{x:.1f}F"


def main():
    parts = parse()
    assert len(parts) == len(S.PARTS) == 67, (len(parts), len(S.PARTS))
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    sol = S.pose_solve(verbose=True)
    graze = S.graze_solve(verbose=True)
    kneel = S.kneel_solve(verbose=True)
    root_y = by["body_barrel"]["off"][1]
    eye = by["eye_left"]["off"]
    fam_vars = {fam: [v for _, v, _, fm, *_ in S.SPECIES if fm == fam] for fam in S.FAMILY}
    roots = {"gnu": ["gnu_left_1", "gnu_right_1"], "gnou": ["gnou_left_1", "gnou_right_1"],
             "megalo": ["megalo_left_1", "megalo_right_1"], "hart": ["hart_pedicle"]}
    mane_vars = [v for _, v, _, _, mane, _, _ in S.SPECIES if mane]
    beard_vars = [v for _, v, _, _, _, beard, _ in S.SPECIES if beard]
    black_vars = [v for _, v, _, _, _, _, extra in S.SPECIES if extra]

    def cond(vs):
        return " || ".join(f"variant == {v}" for v in vs) or "false"

    # kneel deltas: BB local change per part -> Java degrees (rotateAngleX = -radians(BB))
    kneel_rot = {}
    for prefix in ("fore", "hind"):
        for side in ("left", "right"):
            for nm, r in S._leg_rot_tilt(prefix, side, kneel[prefix], -kneel["pitch"]).items():
                kneel_rot[nm] = -(r[0] - S.BY_NAME[nm][3][0])

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
              "untamedwilds.entity.mammal.EntityWildebeest"):
        a(f"import {i};")
    a("""
/**
 * Wildebeest and hartebeest -- blue + black wildebeest, Megalotragus, hartebeest (Phase 4.7, 2026-10-23): 67 boxes on
 * 256x128, authored in scripts/wildebeest_spec.py, ported VERBATIM by scripts/gen_model_wildebeest.py, proved by
 * verify_port.py. High heavy forequarters, a back sloping down to a lower rump, a long boxy face, a tasselled tail.
 *
 * FOUR HORN FAMILIES share the skull, one shown per species (showModel on each chain root hides its whole subtree; the
 * hartebeest's root is the bony PEDICLE its horns grow from). Calves show no horns. Mane / beard / chest fringe / nose
 * tuft are per species. Every pose is SOLVED against the geometry: sit + sleep (sternal recumbency), EAT (the nose to the
 * grass, horns clear) and ATTACK_CLASH (the alcelaphine fight on the carpal joints -- knees down, cannons folded back).
 */""")
    a("public class ModelWildebeest extends AdvancedEntityModel<EntityWildebeest> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelWildebeest() {")
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

    def graze_pose(nod=0.0):
        g = [f"        this.rotate(animator, neck_base, {deg(-graze[0])}, 0, 0);",
             f"        this.rotate(animator, neck, {deg(-graze[1])}, 0, 0);",
             f"        this.rotate(animator, head, {deg(-graze[2] + nod)}, 0, 0);"]
        return "\n".join(g)

    def kneel_pose(push=0.0):
        k = [f"        this.rotate(animator, body_barrel, {deg(kneel['pitch'])}, 0, 0);",
             f"        animator.move(body_barrel, 0, {f(kneel['drop'])}, {f(-push)});",
             f"        this.rotate(animator, neck_base, {deg(kneel['neck'])}, 0, 0);",
             f"        this.rotate(animator, head, {deg(8.0 + 4.0 * push)}, 0, 0);"]
        for nm, d in kneel_rot.items():
            k.append(f"        this.rotate(animator, {nm}, {deg(d)}, 0, 0);")
        return "\n".join(k)

    a("    private void animate(IAnimatedEntity entityIn) {")
    a("        EntityWildebeest wb = (EntityWildebeest) entityIn;")
    a("        animator.update(wb);\n")
    a("        // ATTACK_GORE (14t): the head drops, then hooks up through the target")
    a("        animator.setAnimation(EntityWildebeest.ATTACK_GORE);")
    a("        animator.startKeyframe(5);")
    a("        this.rotate(animator, neck_base, 22.0F, 0, 0);")
    a("        this.rotate(animator, head, 16.0F, 0, 0);")
    a("        animator.endKeyframe();")
    a("        animator.startKeyframe(4);")
    a("        animator.move(body_barrel, 0, 0, -1.5F);")
    a("        this.rotate(animator, neck_base, -12.0F, 0, 0);")
    a("        this.rotate(animator, head, -22.0F, 0, 0);")
    a("        animator.endKeyframe();")
    a("        animator.resetKeyframe(5);\n")
    a("        // ATTACK_THREATEN (50t): head down and horns presented, a foreleg pawing the ground, then a toss of the head")
    a("        animator.setAnimation(EntityWildebeest.ATTACK_THREATEN);")
    a("        animator.startKeyframe(10);")
    a("        this.rotate(animator, neck_base, 26.0F, 0, 0);")
    a("        this.rotate(animator, head, 10.0F, 0, 0);")
    a("        this.rotate(animator, fore_left_upper, -28.0F, 0, 0);")
    a("        this.rotate(animator, fore_left_forearm, 42.0F, 0, 0);")
    a("        animator.endKeyframe();")
    a("        animator.startKeyframe(8);")
    a("        this.rotate(animator, neck_base, 26.0F, 0, 0);")
    a("        this.rotate(animator, head, 10.0F, 0, 0);")
    a("        this.rotate(animator, fore_left_upper, 14.0F, 0, 0);")
    a("        this.rotate(animator, fore_left_forearm, 8.0F, 0, 0);")
    a("        animator.endKeyframe();")
    a("        animator.startKeyframe(8);")
    a("        this.rotate(animator, neck_base, 14.0F, 0, 0);")
    a("        this.rotate(animator, head, -22.0F, 0, 0);")
    a("        this.rotate(animator, fore_left_upper, -28.0F, 0, 0);")
    a("        this.rotate(animator, fore_left_forearm, 42.0F, 0, 0);")
    a("        animator.endKeyframe();")
    a("        animator.setStaticKeyframe(8);")
    a("        animator.resetKeyframe(16);\n")
    a(f"        // EAT (40t, GrazeGoal's timer): SOLVED graze reach -- neck_base {graze[0]:+d}, neck {graze[1]:+d}, head {graze[2]:+d}"
      " (BB deg), the nose just above the grass and every horn family clear of it; two cropping nods")
    a("        animator.setAnimation(EntityWildebeest.EAT);")
    a("        animator.startKeyframe(10);")
    a(graze_pose())
    a("        animator.endKeyframe();")
    a("        animator.startKeyframe(5);")
    a(graze_pose(nod=-6.0))
    a("        animator.endKeyframe();")
    a("        animator.startKeyframe(5);")
    a(graze_pose())
    a("        animator.endKeyframe();")
    a("        animator.setStaticKeyframe(10);")
    a("        animator.resetKeyframe(10);\n")
    a(f"        // ATTACK_CLASH (40t): SOLVED kneel -- barrel pitched {kneel['pitch']:.0f} deg nose-down and dropped "
      f"{kneel['drop']:.1f}u onto the knees, cannons folded back flat, hind legs standing; two shoves forward")
    a("        animator.setAnimation(EntityWildebeest.ATTACK_CLASH);")
    a("        animator.startKeyframe(10);")
    a(kneel_pose())
    a("        animator.endKeyframe();")
    a("        animator.setStaticKeyframe(6);")
    for push in (1.2, 0.0, 1.2):
        a("        animator.startKeyframe(4);")
        a(kneel_pose(push))
        a("        animator.endKeyframe();")
    a("        animator.setStaticKeyframe(4);")
    a("        animator.resetKeyframe(8);")
    a("    }\n")

    a("    public void setupAnim(EntityWildebeest wb, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(wb);
        limbSwingAmount = Math.min(0.6F, limbSwingAmount);
        float gait = limbSwing * 0.55F / Math.max(0.3F, wb.getMobSize() * wb.getScale());
""")
    a("        // ---- per-species parts (written EVERY frame: one model instance is shared by every wildebeest) ----")
    a("        int variant = wb.getVariant();")
    a("        boolean adult = !wb.isBaby();")
    for fam, rs in roots.items():
        a(f"        boolean {fam} = adult && ({cond(fam_vars[fam])});")
        for r in rs:
            a(f"        this.{r}.showModel = {fam};")
    a(f"        this.mane.showModel = {cond(mane_vars)};")
    a(f"        this.mane_base.showModel = {cond(mane_vars)};")
    a(f"        this.beard.showModel = adult && ({cond(beard_vars)});")
    a(f"        this.chest_fringe.showModel = adult && ({cond(black_vars)});")
    a(f"        this.nose_tuft.showModel = {cond(black_vars)};\n")
    a("""        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.03F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(ear_left, 0.2F, 0.18F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.2F, 0.18F, false, 1F, 0F, ageInTicks, 1);
        this.flap(tail, 0.15F, 0.25F, false, 0F, 0F, ageInTicks, 1);
        this.flap(tail_tassel, 0.15F, 0.2F, false, -0.8F, 0F, ageInTicks, 1);
        this.walk(beard, 0.12F, 0.06F, false, 0F, 0F, ageInTicks, 1);
""")
    a("        if (!wb.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        if (!wb.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        if (wb.isInWater() && !wb.isOnGround()) {
            gait = ageInTicks / 5;
            limbSwingAmount = 0.4F;
            this.setRotateAngle(neck_base, -0.45F, 0, 0);
        }

        if (wb.canMove()) {
            // 0.13 blocks/tick: wandering and grazing walk; fleeing, migrating at speed and charging gallop
            boolean running = wb.getCurrentSpeed() > 0.13F;
            if (!running) {
                // walk: a lateral-sequence four-beat walk, the head nodding with each forefoot
                float spd = 1.0F;
                bob(body_barrel, 2.0F * spd, 0.35F, true, gait, limbSwingAmount);
                walk(neck_base, 2.0F * spd, 0.05F, false, 0.5F, 0F, gait, limbSwingAmount);
                walk(fore_left_upper, spd, 0.55F, false, 0F, 0F, gait, limbSwingAmount);
                walk(fore_left_forearm, spd, 0.40F, false, -1.3F, 0.12F, gait, limbSwingAmount);
                walk(fore_left_cannon, spd, 0.30F, false, -1.3F, 0F, gait, limbSwingAmount);
                walk(fore_right_upper, spd, 0.55F, false, Mth.PI, 0F, gait, limbSwingAmount);
                walk(fore_right_forearm, spd, 0.40F, false, Mth.PI - 1.3F, 0.12F, gait, limbSwingAmount);
                walk(fore_right_cannon, spd, 0.30F, false, Mth.PI - 1.3F, 0F, gait, limbSwingAmount);
                walk(hind_left_thigh, spd, 0.50F, false, Mth.PI * 1.5F, 0F, gait, limbSwingAmount);
                walk(hind_left_gaskin, spd, 0.40F, true, Mth.PI * 1.5F - 1.2F, 0F, gait, limbSwingAmount);
                walk(hind_left_cannon, spd, 0.30F, false, Mth.PI * 1.5F - 1.2F, 0F, gait, limbSwingAmount);
                walk(hind_right_thigh, spd, 0.50F, false, Mth.PI * 0.5F, 0F, gait, limbSwingAmount);
                walk(hind_right_gaskin, spd, 0.40F, true, Mth.PI * 0.5F - 1.2F, 0F, gait, limbSwingAmount);
                walk(hind_right_cannon, spd, 0.30F, false, Mth.PI * 0.5F - 1.2F, 0F, gait, limbSwingAmount);
            } else {
                // gallop: a rotary gallop -- the hind pair lands, then the fore pair, the barrel rocking about its middle
                float spd = 0.7F;
                walk(body_barrel, spd, 0.08F, false, 0.6F, 0F, gait, limbSwingAmount);
                bob(body_barrel, spd, 1.1F, false, gait, limbSwingAmount);
                walk(neck_base, spd, 0.10F, true, 0.6F, 0.1F, gait, limbSwingAmount);
                walk(tail, spd, 0.25F, false, 1.0F, -0.35F, gait, limbSwingAmount);
                walk(fore_left_upper, spd, 0.85F, false, 0F, 0F, gait, limbSwingAmount);
                walk(fore_left_forearm, spd, 0.55F, false, -1.4F, 0.2F, gait, limbSwingAmount);
                walk(fore_left_cannon, spd, 0.45F, false, -1.4F, 0F, gait, limbSwingAmount);
                walk(fore_right_upper, spd, 0.85F, false, 0.5F, 0F, gait, limbSwingAmount);
                walk(fore_right_forearm, spd, 0.55F, false, 0.5F - 1.4F, 0.2F, gait, limbSwingAmount);
                walk(fore_right_cannon, spd, 0.45F, false, 0.5F - 1.4F, 0F, gait, limbSwingAmount);
                walk(hind_left_thigh, spd, 0.80F, false, Mth.PI + 0.3F, 0F, gait, limbSwingAmount);
                walk(hind_left_gaskin, spd, 0.55F, true, Mth.PI + 0.3F - 1.2F, 0F, gait, limbSwingAmount);
                walk(hind_left_cannon, spd, 0.45F, false, Mth.PI + 0.3F - 1.2F, 0F, gait, limbSwingAmount);
                walk(hind_right_thigh, spd, 0.80F, false, Mth.PI + 0.8F, 0F, gait, limbSwingAmount);
                walk(hind_right_gaskin, spd, 0.55F, true, Mth.PI + 0.8F - 1.2F, 0F, gait, limbSwingAmount);
                walk(hind_right_cannon, spd, 0.45F, false, Mth.PI + 0.8F - 1.2F, 0F, gait, limbSwingAmount);
            }
        }
""")
    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        head = "        if (wb.sitProgress > 0) {" if pose == "sit" else "        else if (wb.sleepProgress > 0) {"
        a(head)
        dy = sol[(pose, "fore")][1]
        a(f"            // SOLVED (wildebeest_spec.py --pose): sternal recumbency, every leg box above the floor")
        a(f"            this.progressPosition(body_barrel, wb.{key}, 0.0F, {f(root_y - dy)}, 0.0F, 40);")
        for prefix in ("fore", "hind"):
            ang = sol[(pose, prefix)][0]
            for side in ("left", "right"):
                for nm, r in S._leg_rot(prefix, side, ang).items():
                    a(f"            this.progressRotation({nm}, wb.{key}, {f(-math.radians(r[0]))}, 0, 0, 40);")
        lift = sol[(pose, "neck")][0]
        nrest = S.BY_NAME["neck_base"][3][0]
        extra = 6.0 if pose == "sit" else -10.0                # sit: head up and alert; sleep: head lowered
        a(f"            this.progressRotation(neck_base, wb.{key}, {f(-math.radians(nrest + lift + extra))}, 0, 0, 40);")
        tl, troll = sol[(pose, "tail")]
        trest = S.BY_NAME["tail"][3][0]
        a(f"            // the tail DRAPES to one side, its tassel lying on the ground (solved)")
        a(f"            this.progressRotation(tail, wb.{key}, {f(-math.radians(trest - tl))}, 0, {f(math.radians(troll))}, 40);")
        a("        }")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


if __name__ == "__main__":
    main()
