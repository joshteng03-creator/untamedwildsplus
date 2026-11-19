#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench export into ModelCaprine.java (Phase 4.5, 2026-10-10).

Box/pivot/texOffs literals VERBATIM from the modded_entity export; the only computed numbers are the sit/sleep
recumbency targets solved by caprine_spec.pose_solve().

Usage:  python scripts/gen_model_caprine.py <export.java> <ModelCaprine.java>
"""
import math
import re
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import caprine_spec as S                                           # noqa: E402

EXPORT = pathlib.Path(sys.argv[1])
OUT = pathlib.Path(sys.argv[2])
PART_RE = re.compile(
    r'PartDefinition (\w+) = (\w+)\.addOrReplaceChild\("(\w+)",\s*'
    r'CubeListBuilder\.create\(\)\.texOffs\((\d+), (\d+)\)'
    r'\.addBox\(([^)]*?), new CubeDeformation\(0\.0F\)\),\s*'
    r'PartPose\.(offset|offsetAndRotation)\(([^)]*)\)\);')
FAMILY = {"ibex": "ibex", "ram": "ram", "chamois": "chamois", "daggers": "dagger"}


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
    assert len(parts) == len(S.PARTS) == 53, (len(parts), len(S.PARTS))
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    sol = S.pose_solve(verbose=True)
    root_y = by["body_barrel"]["off"][1]
    eye = by["eye_left"]["off"]
    roots = {fam: [n for n in names if n.startswith(fam + "_") and n.endswith("_1")] for fam in FAMILY.values()}
    fam_vars = {fam: [v for _, v, _, fm, _ in S.SPECIES if FAMILY[fm] == fam] for fam in FAMILY.values()}
    beard_vars = [v for _, v, _, _, b in S.SPECIES if b]

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
              "untamedwilds.entity.mammal.EntityCaprine"):
        a(f"import {i};")
    a("""
/**
 * Caprines -- Alpine + Siberian ibex, argali, bighorn, chamois, Harrington's mountain goat (Phase 4.5, 2026-10-10): 53
 * boxes on 256x128, authored in scripts/caprine_spec.py, ported VERBATIM by scripts/gen_model_caprine.py, proved by
 * verify_port.py. A stocky, deep-chested, short-legged climber -- deliberately NOT the slender antelope frame.
 *
 * FOUR HORN FAMILIES share the skull, one shown per species (showModel on each chain root hides its whole subtree):
 * ibex scimitars, ram curls (authored by world orientation so the curl lies in a vertical plane outside the skull),
 * chamois hooks and mountain-goat daggers; plus a beard (ibex + mountain goat).
 * ATTACK_BUTT is the head-down charge-and-butt every caprine fights with; ATTACK_CLASH is the cosmetic rut clash between
 * males -- rear up on the hind legs, then drop and smash horns.
 */""")
    a("public class ModelCaprine extends AdvancedEntityModel<EntityCaprine> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelCaprine() {")
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
    a("""    private void animate(IAnimatedEntity entityIn) {
        EntityCaprine goat = (EntityCaprine) entityIn;
        animator.update(goat);

        // ATTACK_BUTT (14t): head drops, horns forward, then the butt drives up and through
        animator.setAnimation(EntityCaprine.ATTACK_BUTT);
        animator.startKeyframe(6);
        this.rotate(animator, neck, 30.0F, 0, 0);
        this.rotate(animator, head, 20.0F, 0, 0);
        this.rotate(animator, body_chest, 6.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        animator.move(body_barrel, 0, 0, -1.5F);
        this.rotate(animator, neck, -10.0F, 0, 0);
        this.rotate(animator, head, -12.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);

        // ATTACK_CLASH (36t): the rut clash -- rear up on the hind legs, hold, drop and smash horns forward
        animator.setAnimation(EntityCaprine.ATTACK_CLASH);
        animator.startKeyframe(10);
        this.rotate(animator, body_barrel, -32.0F, 0, 0);
        animator.move(body_barrel, 0, -3.0F, 2.0F);
        this.rotate(animator, hind_left_thigh, 30.0F, 0, 0);
        this.rotate(animator, hind_right_thigh, 30.0F, 0, 0);
        this.rotate(animator, fore_left_upper, -35.0F, 0, 0);
        this.rotate(animator, fore_right_upper, -35.0F, 0, 0);
        this.rotate(animator, neck, 10.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(6);
        animator.startKeyframe(5);
        animator.move(body_barrel, 0, 0, -2.0F);
        this.rotate(animator, neck, 34.0F, 0, 0);
        this.rotate(animator, head, 22.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(5);
        animator.resetKeyframe(10);
    }
""")
    a("    public void setupAnim(EntityCaprine goat, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(goat);
        limbSwingAmount = Math.min(0.5F, limbSwingAmount);
        float gait = limbSwing * 0.7F / Math.max(0.3F, goat.getMobSize() * goat.getScale());
""")
    a("        // ---- per-species horns + beard ----")
    a("        int variant = goat.getVariant();")
    for fam, rs in roots.items():
        cond = " || ".join(f"variant == {v}" for v in fam_vars[fam]) or "false"
        a(f"        boolean {fam} = {cond};")
        for r in rs:
            a(f"        this.{r}.showModel = {fam};")
    a("        this.beard.showModel = " + (" || ".join(f"variant == {v}" for v in beard_vars) or "false") + ";\n")
    a("""        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.03F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(ear_left, 0.2F, 0.18F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.2F, 0.18F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail, 0.25F, 0.2F, false, 0F, 0F, ageInTicks, 1);
        this.walk(beard, 0.12F, 0.08F, false, 0F, 0F, ageInTicks, 1);
""")
    a("        if (!goat.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        if (!goat.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        if (goat.isInWater() && !goat.isOnGround()) {
            gait = ageInTicks / 5;
            limbSwingAmount = 0.4F;
            this.setRotateAngle(neck, -0.3F, 0, 0);
        }

        // ---- gait: a quick, short-strided diagonal walk/trot, the body bobbing over stiff sure-footed legs ----
        if (goat.canMove()) {
            float spd = 1.0F;
            bob(body_barrel, 2.0F * spd, 0.5F, true, gait, limbSwingAmount);
            walk(neck, spd, 0.08F, false, 0, 0, gait, limbSwingAmount);
            walk(fore_left_upper, spd, 0.75F, false, 0F, 0F, gait, limbSwingAmount);
            walk(fore_left_forearm, spd, 0.45F, false, -1.3F, 0.15F, gait, limbSwingAmount);
            walk(fore_left_cannon, spd, 0.30F, false, -1.3F, 0F, gait, limbSwingAmount);
            walk(fore_right_upper, spd, 0.75F, false, Mth.PI, 0F, gait, limbSwingAmount);
            walk(fore_right_forearm, spd, 0.45F, false, Mth.PI - 1.3F, 0.15F, gait, limbSwingAmount);
            walk(fore_right_cannon, spd, 0.30F, false, Mth.PI - 1.3F, 0F, gait, limbSwingAmount);
            walk(hind_left_thigh, spd, 0.70F, false, Mth.PI, 0F, gait, limbSwingAmount);
            walk(hind_left_gaskin, spd, 0.50F, true, Mth.PI - 1.2F, 0F, gait, limbSwingAmount);
            walk(hind_left_cannon, spd, 0.30F, false, Mth.PI - 1.2F, 0F, gait, limbSwingAmount);
            walk(hind_right_thigh, spd, 0.70F, false, 0F, 0F, gait, limbSwingAmount);
            walk(hind_right_gaskin, spd, 0.50F, true, -1.2F, 0F, gait, limbSwingAmount);
            walk(hind_right_cannon, spd, 0.30F, false, -1.2F, 0F, gait, limbSwingAmount);
        }
""")
    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        head = "        if (goat.sitProgress > 0) {" if pose == "sit" else "        else if (goat.sleepProgress > 0) {"
        a(head)
        dy = sol[(pose, "fore")][1]
        a(f"            // SOLVED (caprine_spec.py --pose): sternal recumbency, every leg box above the floor")
        a(f"            this.progressPosition(body_barrel, goat.{key}, 0.0F, {f(root_y - dy)}, 0.0F, 40);")
        for prefix in ("fore", "hind"):
            ang = sol[(pose, prefix)][0]
            for side in ("left", "right"):
                for nm, r in S._leg_rot(prefix, side, ang).items():
                    a(f"            this.progressRotation({nm}, goat.{key}, {f(-math.radians(r[0]))}, 0, 0, 40);")
        lift = sol[(pose, "neck")][0]
        nrest = S.BY_NAME["neck"][3][0]
        extra = 8.0 if pose == "sit" else -6.0                 # sit: head up and alert; sleep: head lowered
        a(f"            this.progressRotation(neck, goat.{key}, {f(-math.radians(nrest + lift + extra))}, 0, 0, 40);")
        a("        }")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


if __name__ == "__main__":
    main()
