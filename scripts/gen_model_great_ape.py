#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench export into ModelGreatApe.java (2.9.0, NEW TYPE great_ape).

Box / pivot / texOffs literals VERBATIM from the modded_entity export (proved by verify_port.py). Computed numbers come
from great_ape_spec.py: the SOLVED sit / sleep poses (root drop + per-part deltas -> Java rest + delta) and the FITTED
knuckle-walk (scratch/great_ape_walk.json, one fit per limb pair, lateral-sequence phases). EAT / DRINK / ALERT are added
later by gen_keyframes.py into the `// <gen:idle-poses>` region right after animator.update().

Usage:  python scripts/gen_model_great_ape.py <export.java> <ModelGreatApe.java>
"""
import io
import math
import pathlib
import re
import sys
from contextlib import redirect_stdout

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import great_ape_spec as S                                           # noqa: E402

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
    return f"{x:.4f}F".replace(".0000F", ".0F").replace("-0.0F", "0.0F")


def jrot(r):
    return -math.radians(r[0]), -math.radians(r[1]), math.radians(r[2])


def pose_lines(sol, key, by):
    lines = []
    root = by["body_main"]["off"]
    lines.append(f"            this.progressPosition(body_main, ape.{key}, {f(root[0])}, {f(root[1] + sol['dy'])}, {f(root[2])}, 40);")
    for n in sorted(sol["over"]):
        rest = S.BY_NAME[n][3]
        d = sol["over"][n]
        x, y, z = jrot(tuple(float(rest[i]) + float(d[i]) for i in range(3)))
        lines.append(f"            this.progressRotation({n}, ape.{key}, {f(x)}, {f(y)}, {f(z)}, 40);")
    return lines


def main():
    parts = parse()
    assert len(parts) == len(S.PARTS), (len(parts), len(S.PARTS))
    assert S.WALK_FIT is not None, "run great_ape_spec.py --walk-fit first"
    names = [p["name"] for p in parts]
    by = {p["name"]: p for p in parts}
    eye = by["eye_left"]["off"]
    with redirect_stdout(io.StringIO()):
        sit = S.solve_rest(False)
        sleep = S.solve_rest(True)
    L = []
    a = L.append
    a("package untamedwilds.client.model;\n")
    for i in ("com.github.alexthe666.citadel.animation.IAnimatedEntity",
              "com.github.alexthe666.citadel.client.model.AdvancedEntityModel",
              "com.github.alexthe666.citadel.client.model.AdvancedModelBox",
              "com.github.alexthe666.citadel.client.model.ModelAnimator",
              "com.github.alexthe666.citadel.client.model.basic.BasicModelPart",
              "com.google.common.collect.ImmutableList",
              "untamedwilds.entity.mammal.EntityGreatApe"):
        a(f"import {i};")
    a(f"""
/**
 * Great apes -- Gigantopithecus and the Bornean orangutan (2.9.0, NEW TYPE): {len(parts)} boxes on {S.CANVAS[0]}x{S.CANVAS[1]},
 * authored in scripts/great_ape_spec.py, ported VERBATIM by scripts/gen_model_great_ape.py, proved by verify_port.py.
 *
 * A knuckle-walker: LONG arms on fists and shorter legs on long plantigrade feet, so the torso is pitched up
 * {S.PITCH:.0f} deg from hips to shoulders; a heavy-browed head with a prognathous muzzle and a massive jaw, counter-rotated
 * level; FORWARD-facing eye planes (blinking pushes them back into the skull, not inward).
 *   * The walk is FITTED per limb pair (great_ape_spec.walk_fit, lateral-sequence phases): no digging.
 *   * SIT is SOLVED (upright on the rump, fists down, feet forward); asleep the ape sits SLUMPED, chin on chest.
 */""")
    a("public class ModelGreatApe extends AdvancedEntityModel<EntityGreatApe> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelGreatApe() {")
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
    a("        animator = ModelAnimator.create();")
    a("        updateDefaultPose();")
    a("    }\n")
    a("    @Override")
    a("    public Iterable<BasicModelPart> parts() {")
    a("        return ImmutableList.of(body_main);")
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
        animator.update(entityIn);
        // ATTACK_SWIPE (16t): weight onto the left fist, the right arm swings up, forward and across
        animator.setAnimation(EntityGreatApe.ATTACK_SWIPE);
        animator.startKeyframe(6);
        this.rotate(animator, body_main, -6.0F, 0, 0);
        this.rotate(animator, arm_upper_right, -70.0F, 0, -20.0F);
        this.rotate(animator, arm_fore_right, -30.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, arm_upper_right, -20.0F, 30.0F, 10.0F);
        this.rotate(animator, arm_fore_right, -10.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(6);
        // ATTACK_THREATEN (40t): the ape rears up off its fists, arms flung up and out, and drops back with a slam
        animator.setAnimation(EntityGreatApe.ATTACK_THREATEN);
        animator.startKeyframe(10);
        this.rotate(animator, body_main, -30.0F, 0, 0);
        this.rotate(animator, head, 25.0F, 0, 0);
        this.rotate(animator, arm_upper_left, -60.0F, 0, 25.0F);
        this.rotate(animator, arm_upper_right, -60.0F, 0, -25.0F);
        this.rotate(animator, leg_thigh_left, 30.0F, 0, 0);
        this.rotate(animator, leg_thigh_right, 30.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(14);
        animator.resetKeyframe(8);
        animator.setStaticKeyframe(8);
        // EAT (40t), hand-made: an ape feeds with its HANDS (a knuckle-walker's mouth cannot reach the ground) --
        // the right arm reaches forward and down, brings the food up to the mouth, the head bows to meet it, it chews
        animator.setAnimation(EntityGreatApe.EAT);
        animator.startKeyframe(8);
        this.rotate(animator, arm_upper_right, -25.0F, 0, 0);
        this.rotate(animator, arm_fore_right, -20.0F, 0, 0);
        this.rotate(animator, head, 10.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(10);
        this.rotate(animator, arm_upper_right, -75.0F, 0, -15.0F);
        this.rotate(animator, arm_fore_right, -80.0F, 0, 0);
        this.rotate(animator, head, 8.0F, 0, 0);
        animator.endKeyframe();
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(4);
            this.rotate(animator, arm_upper_right, -75.0F, 0, -15.0F);
            this.rotate(animator, arm_fore_right, -80.0F, 0, 0);
            this.rotate(animator, head, 8.0F, 0, 0);
            this.rotate(animator, jaw, 12.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(3);
            this.rotate(animator, arm_upper_right, -75.0F, 0, -15.0F);
            this.rotate(animator, arm_fore_right, -80.0F, 0, 0);
            this.rotate(animator, head, 8.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);
    }
""")
    a("    public void setupAnim(EntityGreatApe ape, float limbSwing, float limbSwingAmount, float ageInTicks, "
      "float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(ape);
        float rest = Math.max(ape.sitProgress, ape.sleepProgress) / 40F;
        limbSwingAmount = Math.min(1.0F, limbSwingAmount * 1.6F) * (1F - rest);
        float gait = limbSwing / Math.max(0.4F, ape.getMobSize() * ape.getScale());

        float breath = (float) (Math.sin(ageInTicks / 20) * 0.03F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
""")
    a("        // ---- knuckle-walk: FITTED per limb pair (great_ape_spec.walk_fit), Java radians ----")
    for limb in ("hind", "fore"):
        for side in ("left", "right"):
            for n, deg, off, w in S.walk_terms(limb, side, S.WALK_FIT[limb]):
                a(f"        this.walk({n}, {f(S.WALK_SPEED)}, {f(deg)}, false, {f(off)}, {f(w)}, gait, limbSwingAmount);")
    a(f"        this.swing(body_main, {f(S.WALK_SPEED)}, 0.06F, false, 0F, 0F, gait, limbSwingAmount);")
    a(f"        this.bob(body_main, {f(2 * S.WALK_SPEED)}, 0.4F, false, gait, limbSwingAmount);\n")
    a("        // Blinking: the forward-facing eye planes are pushed BACK into the skull; x/y keep the constructor's values.")
    a("        if (!ape.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0])}, {f(eye[1])}, {f(eye[2] + 1.2)});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0])}, {f(eye[1])}, {f(eye[2] + 1.2)});")
    a("        }\n")
    a("        if (!ape.isSleeping()) {")
    a("            this.faceTarget(netHeadYaw, headPitch, 2, neck);")
    a("            this.faceTarget(netHeadYaw, headPitch, 2, head);")
    a("        }\n")
    a("        // ---- SOLVED rest poses (great_ape_spec.solve_rest): target = rest + delta, Java axes ----")
    a("        if (ape.sitProgress > 0) {")
    for ln in pose_lines(sit, "sitProgress", by):
        a(ln)
    a("        }")
    a("        else if (ape.sleepProgress > 0) {")
    for ln in pose_lines(sleep, "sleepProgress", by):
        a(ln)
    a("        }")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


if __name__ == "__main__":
    main()
