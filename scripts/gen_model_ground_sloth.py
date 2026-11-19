#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench export of the REBUILT ground sloth rig into ModelGroundSloth.java (2026-11-16).

Box / pivot / texOffs literals are VERBATIM from the modded_entity export (proved by verify_port.py). Computed numbers come
from ground_sloth_spec.py (head families; the SOLVED rest / sleep poses and the rear-up threat) and from
ground_sloth_walk_fit.py (scratch/sloth_walk.json). BB -> Java: rotation (-x, -y, +z) radians; offsets (-dx, -dy, +dz).

Usage:  python scripts/gen_model_ground_sloth.py <export.java> <ModelGroundSloth.java>
"""
import io
import json
import math
import pathlib
import re
import sys
from contextlib import redirect_stdout

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import ground_sloth_spec as S                                       # noqa: E402

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


def main():
    parts = parse()
    assert len(parts) == len(S.PARTS), (len(parts), len(S.PARTS))
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    with redirect_stdout(io.StringIO()):
        sol = S.pose_solve(verbose=False)
    walk = json.loads((pathlib.Path(__file__).resolve().parent.parent / "scratch" / "sloth_walk.json").read_text())
    rear = sol["rear"]
    root_y = by["body_mid"]["off"][1]
    fam = {"mega": [n for n in names if S.BY_NAME[n][6] == "mega"],
           "mylo": [n for n in names if S.BY_NAME[n][6] == "mylo"],
           "megalonych": [n for n in names if S.BY_NAME[n][6] == "megalonych"]}
    sit = [math.radians(sol["sit"].get(i, 30.0)) for i in range(3)]
    slp = [math.radians(sol["sleep"].get(i, 20.0)) for i in range(3)]
    print(f"pose: drop {sol['dy']:+.2f} tail {sol['tail']:.0f} rest lifts {sol['sit']} sleep {sol['sleep']} rear {rear}")

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
              "untamedwilds.entity.mammal.EntityGroundSloth"):
        a(f"import {i};")
    a("""
/**
 * Ground sloths -- Megatherium, Eremotherium, Megalonyx, Megalocnus, Glossotherium. FULL REBUILD (2026-11-16) of the July rig,
 * which was sculpted by eye before the remodelling SOP: three body slabs at different tilts read as a crumpled crate, it
 * stood ~2.7 m at the hips for a ~2 m animal, its small bear head vanished into the chest and its claws were ivory mittens.
 *
 * 43 boxes on 256x256, authored in scripts/ground_sloth_spec.py, ported VERBATIM by scripts/gen_model_ground_sloth.py and
 * proved by verify_port.py. One long sloping topline over a nested barrel (high wide pelvis, lower shoulders), the forefoot
 * walking on its outer edge with dark claws turned in, the hind foot on its outer edge with the sole turned in, a long
 * thick tail. headShape 0 MEGATHERIID / 1 MYLODONTID / 2 MEGALONYCHID picks the head; written every frame (shared model).
 * ATTACK_THREATEN is the rear-up: the whole animal pitches up about the HIP JOINT onto its hind feet and tail (solved:
 * the hip stays put, the feet stay planted, the tail tip rests on the ground), forelimbs raised and raking.
 */""")
    a("public class ModelGroundSloth extends AdvancedEntityModel<EntityGroundSloth> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    // SOLVED rest-pose neck lift per headShape (rad, 60% neck / 40% head): the head clears the ground resting and lies")
    a("    // on it asleep")
    a("    private static final float[] SIT_LIFT = {" + ", ".join(f(x) for x in sit) + "};")
    a("    private static final float[] SLEEP_LIFT = {" + ", ".join(f(x) for x in slp) + "};")
    a(f"    // the rear-up (solved): pitch about the hip, the root move that keeps the hip joint fixed, the tail lift")
    a(f"    private static final float REAR_PITCH = {f(math.radians(rear['theta']))};")
    a(f"    private static final float REAR_MOVE_Y = {f(-rear['move'][1])};")
    a(f"    private static final float REAR_MOVE_Z = {f(rear['move'][2])};")
    a(f"    private static final float REAR_TAIL = {f(-math.radians(rear['tail']))};\n")
    a("    public ModelGroundSloth() {")
    a("        this.texWidth = 256;")
    a("        this.texHeight = 256;\n")
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
    a("""
        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_mid);
    }

    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(""")
    a(",\n".join(f"                {n}" for n in names))
    a("""            );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityGroundSloth sloth = (EntityGroundSloth) entityIn;
        animator.update(sloth);

        // Threat: the rear-up itself is procedural in setupAnim (exact hip-fixed pitch); these keyframes rake the raised
        // forelimbs -- two swipes, the claws flexing on each.
        animator.setAnimation(EntityGroundSloth.ATTACK_THREATEN);
        animator.startKeyframe(12);
        this.rotate(animator, arm_left_1, -40F, 0, -12F);
        this.rotate(animator, arm_right_1, -36F, 0, 12F);
        this.rotate(animator, arm_left_2, -20F, 0, 0);
        this.rotate(animator, arm_right_2, -18F, 0, 0);
        animator.endKeyframe();
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(6);
            this.rotate(animator, arm_left_1, -55F, 0, -14F);
            this.rotate(animator, arm_right_1, -20F, 0, 6F);
            this.rotate(animator, arm_left_2, -30F, 0, 0);
            this.rotate(animator, arm_right_2, -10F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(6);
            this.rotate(animator, arm_left_1, -20F, 0, -6F);
            this.rotate(animator, arm_right_1, -55F, 0, 14F);
            this.rotate(animator, arm_left_2, -10F, 0, 0);
            this.rotate(animator, arm_right_2, -30F, 0, 0);
            animator.endKeyframe();
        }
        animator.setStaticKeyframe(6);
        animator.resetKeyframe(8);

        // Gore: a raking downward swipe with the right forelimb, the claws opening then hooking through.
        animator.setAnimation(EntityGroundSloth.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, body_chest, -8F, 0, 0);
        this.rotate(animator, arm_right_1, -60F, 0, 16F);
        this.rotate(animator, arm_right_2, -30F, 0, 0);
        this.rotate(animator, claw_right_a1, -25F, 0, 0);
        this.rotate(animator, claw_right_b1, -25F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, body_chest, 6F, 0, 0);
        this.rotate(animator, arm_right_1, 30F, 0, -14F);
        this.rotate(animator, arm_right_2, 15F, 0, 0);
        this.rotate(animator, claw_right_a1, 20F, 0, 0);
        this.rotate(animator, claw_right_b1, 20F, 0, 0);
        animator.move(body_mid, 0, 0, -1.5F);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityGroundSloth sloth, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();

        // ---- head family (every frame: one model instance serves every sloth) ----
        int headF = Mth.clamp(sloth.getHeadShape(), 0, 2);""")
    for key, code in (("mega", 0), ("mylo", 1), ("megalonych", 2)):
        for n in fam[key]:
            if S.BY_NAME[n][1] in fam[key]:
                continue          # children of a family box go with it
            a(f"        this.{n}.showModel = headF == {code};")
    a("""
        animate(sloth);
        float globalSpeed = 0.9F;
        float globalDegree = 1.0F;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- idle: slow breathing, a heavy tail sway ----
        float bx = (float) (1.0F + Math.sin(ageInTicks / 22) * 0.04F), by = (float) (1.0F + Math.sin(ageInTicks / 18) * 0.04F);
        this.body_mid.setScale(bx, by, 1.0F);
        this.body_chest.setScale(bx, by, 1.0F);
        this.body_hips.setScale(bx, by, 1.0F);
        this.swing(tail_1, 0.08F, 0.08F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail_2, 0.08F, 0.10F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail_3, 0.08F, 0.12F, false, 2F, 0F, ageInTicks, 1);
        this.walk(neck, 0.06F, 0.03F, false, 0F, 0F, ageInTicks, 1);

        // eyes: squash the planes to a slit when shut (written every frame: the model is shared)
        float eyeOpen = sloth.shouldRenderEyes() ? 1.0F : 0.2F;
        this.eye_left.setScale(1.0F, eyeOpen, 1.0F);
        this.eye_right.setScale(1.0F, eyeOpen, 1.0F);

        if (!sloth.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 3, head);
        }

        // ---- the rear-up (ATTACK_THREATEN): rise over 12 ticks, hold, settle over the last 10. The animal pitches up about
        // the hip joint -- the root rotation plus the translation that keeps that joint fixed -- the thighs counter-rotate so
        // the hind feet stay planted, and the tail drops onto the ground as the third leg.
        if (sloth.getAnimation() == EntityGroundSloth.ATTACK_THREATEN) {
            float t = sloth.getAnimationTick() + (ageInTicks - (int) ageInTicks);
            float d = EntityGroundSloth.ATTACK_THREATEN.getDuration();
            float p = t < 12 ? t / 12F : t > d - 10 ? Math.max(0F, (d - t) / 10F) : 1F;
            p = p * p * (3 - 2 * p);
            this.body_mid.rotateAngleX -= REAR_PITCH * p;
            this.body_mid.rotationPointY += REAR_MOVE_Y * p;
            this.body_mid.rotationPointZ += REAR_MOVE_Z * p;
            this.leg_left_thigh.rotateAngleX += REAR_PITCH * p;
            this.leg_right_thigh.rotateAngleX += REAR_PITCH * p;
            this.tail_1.rotateAngleX += REAR_TAIL * p;
            this.neck.rotateAngleX += REAR_PITCH * 0.35F * p;    // keep the face looking forward, not at the sky
            this.head.rotateAngleX += REAR_PITCH * 0.25F * p;
        }

        // ---- walk: lateral sequence, FITTED to the box geometry (ground_sloth_walk_fit.py): no planted-foot slide ----
        if (sloth.canMove()) {
            bob(body_mid, 0.8F * globalSpeed, 0.25F * globalDegree, true, f, limbSwingAmount);
            flap(body_mid, 0.8F * globalSpeed, 0.05F * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(neck, 0.8F * globalSpeed, 0.08F * globalDegree, false, 0, 0, f, limbSwingAmount);""")
    for line in walk:
        a("            " + line)
    a("""            swing(tail_1, 0.8F * globalSpeed, 0.15F * globalDegree, false, 0F, 0, f, limbSwingAmount);
            swing(tail_2, 0.8F * globalSpeed, 0.18F * globalDegree, false, -0.6F, 0, f, limbSwingAmount);
        }

        // ---- rest / sleep: SOLVED sternal recumbency (ground_sloth_spec.py --pose) ----
        if (sloth.sitProgress > 0) {
            applyRestingPose(sloth.sitProgress, SIT_LIFT[headF]);
        }
        else if (sloth.sleepProgress > 0) {
            applyRestingPose(sloth.sleepProgress, SLEEP_LIFT[headF]);
        }
    }

    private void applyRestingPose(float progress, float lift) {""")
    a(f"        this.progressPosition(body_mid, progress, 0.0F, {f(root_y - sol['dy'])}, 0.0F, 40);")
    for n, r in sol["rot"].items():
        d = by[n]["rot"]
        a(f"        this.progressRotation({n}, progress, {f(d[0] - math.radians(r[0]))}, {f(d[1] - math.radians(r[1]))}, "
          f"{f(d[2] + math.radians(r[2]))}, 40);")
    a(f"        this.progressRotation(tail_1, progress, {f(by['tail_1']['rot'][0] - math.radians(sol['tail']))}, "
      f"{f(by['tail_1']['rot'][1])}, {f(by['tail_1']['rot'][2])}, 40);")
    a(f"        this.progressRotation(neck, progress, {f(by['neck']['rot'][0])} - lift * 0.6F, 0.0F, 0.0F, 40);")
    a(f"        this.progressRotation(head, progress, {f(by['head']['rot'][0])} - lift * 0.4F, 0.0F, 0.0F, 40);")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


if __name__ == "__main__":
    main()
