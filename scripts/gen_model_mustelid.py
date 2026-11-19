#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench export into ModelMustelid.java (2.9.0, NEW TYPE mustelid).

Box / pivot / texOffs literals VERBATIM from the modded_entity export (proved by verify_port.py). Computed numbers come
from mustelid_spec.py: the SOLVED sit / sleep poses (root drop + per-part rotation deltas, converted to Java axes as
rest + delta) and the walk terms its walk_check() measured (no digging, paws planted ~half the cycle).

Usage:  python scripts/gen_model_mustelid.py <export.java> <ModelMustelid.java>
"""
import io
import math
import pathlib
import re
import sys
from contextlib import redirect_stdout

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import mustelid_spec as S                                            # noqa: E402

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
    """Blockbench local degrees -> MC radians (the verified flip: -x, -y, +z)."""
    return -math.radians(r[0]), -math.radians(r[1]), math.radians(r[2])


def pose_lines(sol, key, by):
    lines = []
    root = by["body_chest"]["off"]
    lines.append(f"            this.progressPosition(body_chest, mustelid.{key}, {f(root[0])}, {f(root[1] + sol['dy'])}, {f(root[2])}, 40);")
    for n in sorted(sol["over"]):
        rest = S.BY_NAME[n][3]
        d = sol["over"][n]
        tot = tuple(float(rest[i]) + float(d[i]) for i in range(3))
        x, y, z = jrot(tot)
        lines.append(f"            this.progressRotation({n}, mustelid.{key}, {f(x)}, {f(y)}, {f(z)}, 40);")
    return lines


def main():
    parts = parse()
    assert len(parts) == len(S.PARTS), (len(parts), len(S.PARTS))
    names = [p["name"] for p in parts]
    by = {p["name"]: p for p in parts}
    eye = by["eye_left"]["off"]
    with redirect_stdout(io.StringIO()):
        sleep = S.solve_sleep()
        sit = S.solve_sit()
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
              "untamedwilds.entity.mammal.EntityMustelid"):
        a(f"import {i};")
    a(f"""
/**
 * Mustelids -- wolverine and honey badger (2.9.0, NEW TYPE): {len(parts)} boxes on {S.CANVAS[0]}x{S.CANVAS[1]}, authored in
 * scripts/mustelid_spec.py, ported VERBATIM by scripts/gen_model_mustelid.py, proved by verify_port.py.
 *
 * A long low barrel arched over the middle, short thick legs on broad plantigrade paws, a broad heavy head carried low
 * on a thick neck, small round ears, a bushy tail. Both species share the mesh (the honey badger's mantle is its skin).
 *   * SIT and SLEEP are SOLVED (mustelid_spec.solve_sit / solve_sleep): sitting, the body pitches up {sit['theta']:.0f} deg until
 *     the rump rests on the ground with the fore paws planted; asleep it lies belly-down, legs tucked, chin and tail down.
 *   * The walk is the four-term cycle mustelid_spec.walk_check() measured: no digging, each paw planted about half the time.
 */""")
    a("public class ModelMustelid extends AdvancedEntityModel<EntityMustelid> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelMustelid() {")
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
    a("        return ImmutableList.of(body_chest);")
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
        // ATTACK_BITE (12t): the head draws back, then lunges with the jaw open and snaps
        animator.setAnimation(EntityMustelid.ATTACK_BITE);
        animator.startKeyframe(4);
        this.rotate(animator, neck, 10.0F, 0, 0);
        this.rotate(animator, jaw, 25.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(3);
        this.rotate(animator, neck, -8.0F, 0, 0);
        this.rotate(animator, jaw, 30.0F, 0, 0);
        animator.move(body_chest, 0, 0, -1.5F);
        animator.endKeyframe();
        animator.resetKeyframe(5);
        // IDLE_TALK (24t): a snarl -- head up a little, the jaw drops twice
        animator.setAnimation(EntityMustelid.IDLE_TALK);
        animator.startKeyframe(5);
        this.rotate(animator, neck, -8.0F, 0, 0);
        this.rotate(animator, jaw, 20.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, -8.0F, 0, 0);
        this.rotate(animator, jaw, 6.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, -8.0F, 0, 0);
        this.rotate(animator, jaw, 22.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(10);
    }
""")
    a("    public void setupAnim(EntityMustelid mustelid, float limbSwing, float limbSwingAmount, float ageInTicks, "
      "float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(mustelid);
        float rest = Math.max(mustelid.sitProgress, mustelid.sleepProgress) / 40F;
        limbSwingAmount = Math.min(0.6F, limbSwingAmount) * (1F - rest);
        // a planted paw keeps pace whatever the species' size (the dire wolf gait rule)
        float gait = limbSwing / Math.max(0.35F, mustelid.getMobSize() * mustelid.getScale());

        // breathing + idle tail
        float breath = (float) (Math.sin(ageInTicks / 18) * 0.03F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(tail_1, 0.12F, 0.12F, false, 0F, 0F, ageInTicks, 1F - rest);
        this.swing(tail_2, 0.12F, 0.16F, false, 1F, 0F, ageInTicks, 1F - rest);
""")
    a("        // ---- walk (mustelid_spec.walk_terms, Java radians) ----")
    for limb in ("hind", "fore"):
        for side in ("left", "right"):
            for n, deg, off, w in S.walk_terms(limb, side):
                a(f"        this.walk({n}, {f(S.WALK_SPEED)}, {f(deg)}, false, {f(off)}, {f(w)}, gait, limbSwingAmount);")
    a("        this.bob(body_chest, 2 * " + f(S.WALK_SPEED) + ", 0.4F, false, gait, limbSwingAmount);")
    a("        this.swing(body_chest, " + f(S.WALK_SPEED) + ", 0.05F, false, 0F, 0F, gait, limbSwingAmount);\n")
    a("        // Blinking: bury the eye planes inside the head; y/z keep the constructor's values.")
    a("        if (!mustelid.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.5)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.5)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("        if (!mustelid.isSleeping()) {")
    a("            this.faceTarget(netHeadYaw, headPitch, 3, neck);")
    a("            this.faceTarget(netHeadYaw, headPitch, 2, head);")
    a("        }\n")
    a("        // ---- SOLVED rest poses (mustelid_spec.solve_sit / solve_sleep): target = rest + delta, Java axes ----")
    a("        if (mustelid.sitProgress > 0) {")
    for ln in pose_lines(sit, "sitProgress", by):
        a(ln)
    a("        }")
    a("        else if (mustelid.sleepProgress > 0) {")
    for ln in pose_lines(sleep, "sleepProgress", by):
        a(ln)
    a("        }")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes; sit pitch {sit['theta']:.1f}, sleep drop {sleep['dy']:.2f})")


if __name__ == "__main__":
    main()
