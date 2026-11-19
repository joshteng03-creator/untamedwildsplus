#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench export into ModelGroundBird.java (2.9.0, NEW TYPE ground_bird).

Box / pivot / texOffs literals VERBATIM from the modded_entity export (proved by verify_port.py). Computed numbers come
from ground_bird_spec.py: the SOLVED sit / sleep poses (root drop + per-part rotation deltas, converted to Java axes as
rest + delta) and the FITTED biped walk (WALK_FIT, scratch/ground_bird_walk.json). EAT / DRINK / ALERT are added later
by gen_keyframes.py into the `// <gen:idle-poses>` region (2.8.0 pipeline), which lands right after animator.update().

Usage:  python scripts/gen_model_ground_bird.py <export.java> <ModelGroundBird.java>
"""
import io
import math
import pathlib
import re
import sys
from contextlib import redirect_stdout

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import ground_bird_spec as S                                         # noqa: E402

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
    lines.append(f"            this.progressPosition(body_main, bird.{key}, {f(root[0])}, {f(root[1] + sol['dy'])}, {f(root[2])}, 40);")
    for n in sorted(sol["over"]):
        rest = S.BY_NAME[n][3]
        d = sol["over"][n]
        x, y, z = jrot(tuple(float(rest[i]) + float(d[i]) for i in range(3)))
        lines.append(f"            this.progressRotation({n}, bird.{key}, {f(x)}, {f(y)}, {f(z)}, 40);")
    return lines


def main():
    parts = parse()
    assert len(parts) == len(S.PARTS), (len(parts), len(S.PARTS))
    assert S.WALK_FIT is not None, "run ground_bird_spec.py --walk-fit first"
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
              "net.minecraft.util.Mth",
              "untamedwilds.entity.ComplexMob",
              "untamedwilds.entity.bird.EntityGroundBird"):
        a(f"import {i};")
    a(f"""
/**
 * Giant ground birds -- the giant moa, the elephant bird and Genyornis (2.9.0, NEW TYPE): {len(parts)} boxes on
 * {S.CANVAS[0]}x{S.CANVAS[1]}, authored in scripts/ground_bird_spec.py, ported VERBATIM by scripts/gen_model_ground_bird.py,
 * proved by verify_port.py.
 *
 * A deep egg-shaped body, a long three-segment neck held up and forward, a small head and short beak, and the bird's
 * zig-zag leg authored by WORLD direction per segment (thigh forward-down inside the plumage, the long drumstick down
 * and BACK, the tarsus down-forward, a flat foot) on a hip height solved so the feet stand on the ground.
 *   * `deepBeak` (Genyornis): the beak is deepened with setScale -- written every frame (the model is shared).
 *   * The walk is FITTED (ground_bird_spec.walk_fit): no digging, the foot planted about half the cycle.
 *   * SIT and SLEEP are SOLVED: the bird rests on its hocks, tarsi flat forward along the ground under the belly;
 *     asleep the neck is laid forward along the ground with the head level.
 */""")
    a("public class ModelGroundBird extends AdvancedEntityModel<EntityGroundBird> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelGroundBird() {")
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
        // ATTACK_KICK (18t): the bird rocks back and drives one leg forward and down -- the cassowary's defence
        animator.setAnimation(EntityGroundBird.ATTACK_KICK);
        animator.startKeyframe(6);
        this.rotate(animator, body_main, -6.0F, 0, 0);
        this.rotate(animator, leg_thigh_right, -40.0F, 0, 0);
        this.rotate(animator, leg_shin_right, 50.0F, 0, 0);
        this.rotate(animator, leg_tarsus_right, -40.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, body_main, -4.0F, 0, 0);
        this.rotate(animator, leg_thigh_right, -70.0F, 0, 0);
        this.rotate(animator, leg_shin_right, 10.0F, 0, 0);
        this.rotate(animator, leg_tarsus_right, -10.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(8);
        // ATTACK_THREATEN (40t): neck reared up and back, the tail plume raised, a stamp (ModelAnimator has no scale())
        animator.setAnimation(EntityGroundBird.ATTACK_THREATEN);
        animator.startKeyframe(10);
        this.rotate(animator, neck_1, 20.0F, 0, 0);
        this.rotate(animator, neck_2, 10.0F, 0, 0);
        this.rotate(animator, tail, -20.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.startKeyframe(6);
        this.rotate(animator, neck_1, 20.0F, 0, 0);
        this.rotate(animator, leg_thigh_left, -25.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(14);
    }
""")
    a("    public void setupAnim(EntityGroundBird bird, float limbSwing, float limbSwingAmount, float ageInTicks, "
      "float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(bird);
        float rest = Math.max(bird.sitProgress, bird.sleepProgress) / 40F;
        limbSwingAmount = Math.min(1.0F, limbSwingAmount * 1.6F) * (1F - rest);
        float gait = limbSwing / Math.max(0.4F, bird.getMobSize() * bird.getScale());

        // per species: Genyornis' deep beak (scale is not restored by resetToDefaultPose: write it every frame)
        boolean deep = ComplexMob.speciesFlag(bird, "deepBeak") == 1;
        this.beak.setScale(deep ? 1.2F : 1.0F, deep ? 2.1F : 1.0F, deep ? 1.15F : 1.0F);

        float breath = (float) (Math.sin(ageInTicks / 22) * 0.025F);
        this.body_main.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(tail, 0.1F, 0.08F, false, 0F, 0F, ageInTicks, 1F - rest);
""")
    a("        // ---- walk: FITTED (ground_bird_spec.walk_fit), Java radians ----")
    for side in ("left", "right"):
        for n, deg, off, w in S.walk_terms(side):
            a(f"        this.walk({n}, {f(S.WALK_SPEED)}, {f(deg)}, false, {f(off)}, {f(w)}, gait, limbSwingAmount);")
    a("        // the head bobs with the stride, the body rocks over the planted foot")
    a(f"        this.walk(neck_1, {f(2 * S.WALK_SPEED)}, 0.06F, false, 0.5F, 0F, gait, limbSwingAmount);")
    a(f"        this.walk(head, {f(2 * S.WALK_SPEED)}, 0.06F, true, 0.5F, 0F, gait, limbSwingAmount);")
    a(f"        this.swing(body_main, {f(S.WALK_SPEED)}, 0.05F, false, 0F, 0F, gait, limbSwingAmount);")
    a(f"        this.bob(body_main, {f(2 * S.WALK_SPEED)}, 0.5F, false, gait, limbSwingAmount);\n")
    a("        // Blinking: bury the eye planes inside the head; y/z keep the constructor's values.")
    a("        if (!bird.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.5)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.5)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("        if (!bird.isSleeping()) {")
    a("            this.faceTarget(netHeadYaw, headPitch, 3, neck_2);")
    a("            this.faceTarget(netHeadYaw, headPitch, 2, head);")
    a("        }\n")
    a("        // ---- SOLVED rest poses (ground_bird_spec.solve_rest): target = rest + delta, Java axes ----")
    a("        if (bird.sitProgress > 0) {")
    for ln in pose_lines(sit, "sitProgress", by):
        a(ln)
    a("        }")
    a("        else if (bird.sleepProgress > 0) {")
    for ln in pose_lines(sleep, "sleepProgress", by):
        a(ln)
    a("        }")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


if __name__ == "__main__":
    main()
