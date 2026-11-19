#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench export into ModelGiantMonitor.java (Phase 4.3, 2026-10-10).

Box/pivot/texOffs literals VERBATIM from the modded_entity export; the only computed numbers are the two belly-down
rest poses solved by giant_monitor_spec.gm_pose_solve().

Usage:  python scripts/gen_model_giant_monitor.py <export.java> <ModelGiantMonitor.java>
"""
import math
import re
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import giant_monitor_spec as S                                      # noqa: E402

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
    assert len(parts) == len(S.PARTS) == 27, (len(parts), len(S.PARTS))
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    sol = S.gm_pose_solve(verbose=True)
    root_y = by["body_belly"]["off"][1]
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
              "untamedwilds.entity.reptile.EntityGiantMonitor"):
        a(f"import {i};")
    a("""
/**
 * Giant monitor -- Megalania + Komodo dragon (Phase 4.3, 2026-10-10): 27 boxes on 256x128, authored from scratch in
 * scripts/giant_monitor_spec.py, ported VERBATIM by scripts/gen_model_giant_monitor.py, proved by verify_port.py.
 *
 * A deep, pot-bellied varanid carried low on SPRAWLING legs: each upper limb is laid out sideways and tilted down
 * (a Z rotation), the lower limb drops straight to the ground, the foot points forward with the toes turned out.
 * Because the legs sprawl, the walk swings them about the VERTICAL axis (rotateAngleY) in diagonal pairs -- left fore
 * with right hind -- while the body, neck and tail throw the lateral S-curve every lizard walks with; the tail's wave
 * lags further down each segment.
 * The forked tongue is hidden except during IDLE_TONGUE.
 */""")
    a("public class ModelGiantMonitor extends AdvancedEntityModel<EntityGiantMonitor> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelGiantMonitor() {")
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
    a("        return ImmutableList.of(body_belly);")
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
        EntityGiantMonitor lizard = (EntityGiantMonitor) entityIn;
        animator.update(lizard);

        // IDLE_TONGUE (20t): two quick flicks of the forked tongue out of the closed mouth
        animator.setAnimation(EntityGiantMonitor.IDLE_TONGUE);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(4);
            animator.move(tongue, 0, 0, -2.6F);
            this.rotate(animator, tongue, 6.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(4);
            animator.move(tongue, 0, 0, -0.6F);
            animator.endKeyframe();
        }
        animator.resetKeyframe(4);

        // ATTACK_BITE (14t): the head draws back and up, the jaw gapes, then the strike drives forward and down
        animator.setAnimation(EntityGiantMonitor.ATTACK_BITE);
        animator.startKeyframe(6);
        this.rotate(animator, neck, -14.0F, 0, 0);
        this.rotate(animator, head, -8.0F, 0, 0);
        this.rotate(animator, jaw, 30.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, 14.0F, 0, 0);
        this.rotate(animator, head, 6.0F, 0, 0);
        this.rotate(animator, jaw, 0, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);

        // ATTACK_THRASH (30t): jaws clamped, the head and neck whip side to side to tear
        animator.setAnimation(EntityGiantMonitor.ATTACK_THRASH);
        for (int i = 0; i < 3; i++) {
            animator.startKeyframe(4);
            this.rotate(animator, neck, 0, 28.0F, 0);
            this.rotate(animator, head, 0, 12.0F, 0);
            this.rotate(animator, body_belly, 0, -8.0F, 0);
            animator.endKeyframe();
            animator.startKeyframe(4);
            this.rotate(animator, neck, 0, -28.0F, 0);
            this.rotate(animator, head, 0, -12.0F, 0);
            this.rotate(animator, body_belly, 0, 8.0F, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(6);
    }
""")
    a("    public void setupAnim(EntityGiantMonitor lizard, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(lizard);
        limbSwingAmount = Math.min(0.5F, limbSwingAmount);
        float amt = Math.min(1.0F, limbSwingAmount / 0.35F);
        float gait = limbSwing * 0.6F / Math.max(0.3F, lizard.getMobSize() * lizard.getScale());
        this.tongue.showModel = lizard.getAnimation() == EntityGiantMonitor.IDLE_TONGUE;

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 22) * 0.03F);
        this.body_belly.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(tail_4, 0.06F, 0.10F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_5, 0.06F, 0.16F, false, -0.8F, 0F, ageInTicks, 1);
""")
    a("        // Blinking: bury the eye planes inside the skull; y/z keep the constructor's values.")
    a("        if (!lizard.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        if (!lizard.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        if (lizard.isInWater() && !lizard.isOnGround()) {
            // swimming: legs pressed back along the flanks, the tail does the work
            float sw = ageInTicks * 0.3F;
            this.arm_left_upper.rotateAngleY += 0.9F;
            this.arm_right_upper.rotateAngleY -= 0.9F;
            this.leg_left_upper.rotateAngleY += 0.9F;
            this.leg_right_upper.rotateAngleY -= 0.9F;
            this.tail_1.rotateAngleY += Mth.cos(sw) * 0.20F;
            this.tail_2.rotateAngleY += Mth.cos(sw - 0.7F) * 0.30F;
            this.tail_3.rotateAngleY += Mth.cos(sw - 1.4F) * 0.35F;
            this.tail_4.rotateAngleY += Mth.cos(sw - 2.1F) * 0.40F;
            this.tail_5.rotateAngleY += Mth.cos(sw - 2.8F) * 0.45F;
            this.body_belly.rotateAngleY += Mth.cos(sw + 0.7F) * 0.08F;
        }
        else if (lizard.canMove()) {
            // ---- sprawling walk: diagonal pairs swing about the vertical axis ----
            // A limb laid along +x and one along -x rotated by the SAME rotateAngleY move in opposite directions, so the
            // two forelegs (and the two hind legs) alternate on equal values; hind = fore + pi gives the diagonal pairs.
            float c = Mth.cos(gait) * amt, sn = Mth.sin(gait);
            this.arm_left_upper.rotateAngleY += c * 0.55F;
            this.arm_right_upper.rotateAngleY += c * 0.55F;
            this.leg_left_upper.rotateAngleY -= c * 0.55F;
            this.leg_right_upper.rotateAngleY -= c * 0.55F;
            // lift the limb that is swinging forward (left-side outer ends rise on -Z, right-side on +Z)
            this.arm_left_upper.rotateAngleZ -= Math.max(0, sn) * 0.30F * amt;
            this.leg_right_upper.rotateAngleZ += Math.max(0, sn) * 0.30F * amt;
            this.arm_right_upper.rotateAngleZ += Math.max(0, -sn) * 0.30F * amt;
            this.leg_left_upper.rotateAngleZ -= Math.max(0, -sn) * 0.30F * amt;
            // the lateral S-curve: trunk one way, neck and tail the other, the tail wave lagging down its length
            this.body_belly.rotateAngleY += c * 0.10F;
            this.neck.rotateAngleY -= c * 0.12F;
            this.tail_1.rotateAngleY -= Mth.cos(gait - 0.6F) * 0.14F * amt;
            this.tail_2.rotateAngleY -= Mth.cos(gait - 1.2F) * 0.18F * amt;
            this.tail_3.rotateAngleY -= Mth.cos(gait - 1.8F) * 0.22F * amt;
            this.tail_4.rotateAngleY -= Mth.cos(gait - 2.4F) * 0.24F * amt;
            this.tail_5.rotateAngleY -= Mth.cos(gait - 3.0F) * 0.26F * amt;
        }
""")
    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        d = sol[pose]
        head = "        if (lizard.sitProgress > 0) {" if pose == "sit" else "        else if (lizard.sleepProgress > 0) {"
        a(head)
        a("            // SOLVED (giant_monitor_spec.py --pose): belly on the ground, legs splayed with the feet flat, tail resting"
          + (", chin down, tail curled" if pose == "sleep" else ""))
        a(f"            this.progressPosition(body_belly, lizard.{key}, 0.0F, {f(root_y - d['dy'])}, 0.0F, 40);")
        for n in names:
            if n in d["over"]:
                rx, ry, rz = d["over"][n]
                a(f"            this.progressRotation({n}, lizard.{key}, {f(-math.radians(rx))}, {f(-math.radians(ry))}, "
                  f"{f(math.radians(rz))}, 40);")
        a("        }")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


if __name__ == "__main__":
    main()
