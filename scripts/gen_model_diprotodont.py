#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench `modded_entity` export into ModelDiprotodont.java.

Never re-derive the Y-flip: every setRotationPoint / addBox / texOffs literal is copied
VERBATIM out of the export. The only numbers computed here are the phase-7 pose targets,
grid-searched by diprotodont_spec.pose_solve() against the real box geometry, and the neck
lift that keeps the jaw off the floor in those poses (solved the same way, see NECK_LIFT).

Usage:  python scripts/gen_model_diprotodont.py <export.java> <ModelDiprotodont.java>
"""
import math
import re
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import diprotodont_spec as S                                        # noqa: E402

EXPORT = pathlib.Path(sys.argv[1])
OUT = pathlib.Path(sys.argv[2])

PART_RE = re.compile(
    r'PartDefinition (\w+) = (\w+)\.addOrReplaceChild\("(\w+)",\s*'
    r'CubeListBuilder\.create\(\)\.texOffs\((\d+), (\d+)\)'
    r'\.addBox\(([^)]*?), new CubeDeformation\(0\.0F\)\),\s*'
    r'PartPose\.(offset|offsetAndRotation)\(([^)]*)\)\);')

BOSS = [v for _, v, _, b in S.SPECIES if b]
# Blockbench degrees the neck is lifted (front up) in each rest pose so the lowest head point
# clears y=0 once the body has dropped: solved 2 (sit) and 6 (sleep) as the minimum; sit
# carries the head higher still, which is how a resting animal that is awake holds it.
NECK_LIFT = {"sit": 10.0, "sleep": 6.0}


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
        d = out.setdefault(pose, {"dy": dy, "rot": {}})
        for side in ("left", "right"):
            for nm, r in S._leg_rot(prefix, side, angles).items():
                # Blockbench local degrees -> MC local radians (the verified flip).
                d["rot"][nm] = (-math.radians(r[0]), -math.radians(r[1]))
    return out


def main():
    parts = parse()
    assert len(parts) == len(S.PARTS) == 30, (len(parts), len(S.PARTS))
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    P = poses()
    barrel_y = by["body_barrel"]["off"][1]
    eye = by["eye_left"]["off"]
    neck_bb = S.BY_NAME["neck"][3][0]

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
              "untamedwilds.entity.mammal.EntityDiprotodont"):
        a(f"import {i};")
    a("""
/**
 * Diprotodont -- 30 boxes on 256x128, authored from scratch (Phase 4.1, 2026-10-09). There is no
 * donor rig: before this class nothing in the tree modelled a diprotodontid body.
 *
 * Sculpted from scripts/diprotodont_spec.py, which carries the spec, the forward-kinematics
 * checker, the z-fight sweep, the volume-burial check, the UV shelf pack and the phase-7 pose
 * solver. Box coords + texOffs are transcribed VERBATIM from the modded_entity export (the Y-down
 * flip is the export's, never re-derived); scripts/gen_model_diprotodont.py does the
 * transcription and scripts/verify_port.py proves it.
 *
 * A wombat the size of a rhinoceros: a broad barrel (width/depth 1.34) with a slight shoulder rise
 * and a rounded rump, pillar legs with the belly at 40% of the withers, broad plantigrade feet
 * turned INWARD (the pigeon-toed diprotodont stance -- 16 deg fore, 12 hind, a Y rotation on each
 * foot), and a big low-carried head with wide cheeks, a deep jaw, a fleshy nose pad and short
 * forward-pointing incisors.
 *
 * FIVE species share this one mesh, sized by species `scale` at 1 block = 1 m at the shoulder.
 *   * NASAL_BOSS is shown for zygomaturus alone (getVariant(), the ModelDeer:509-524 pattern).
 *   * The JOEY (a baby's head peeking from the rear-facing pouch) follows the synced
 *     EntityDiprotodont.hasJoey() flag, and is tucked away whenever the mother sits or sleeps.
 * showModel is applied to each chain ROOT: ModelPart.render returns before recursing into
 * children when `visible` is false, so hiding a root hides its whole subtree.
 */""")
    a("public class ModelDiprotodont extends AdvancedEntityModel<EntityDiprotodont> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelDiprotodont() {")
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
        EntityDiprotodont dip = (EntityDiprotodont) entityIn;
        animator.update(dip);

        // ATTACK_THREATEN (50t) -- no horns and no tusks, so the display is mass: the head drops
        // and swings side to side while the animal shifts its weight from one forefoot to the
        // other, the way a wombat squares up before it shoves.
        animator.setAnimation(EntityDiprotodont.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_chest, 0, 0, 5.0F);
            this.rotate(animator, neck, 8.0F, -12.0F, -5.0F);
            this.rotate(animator, head_skull, 6.0F, -10.0F, 0);
            this.rotate(animator, fore_left_upper, -10.0F, 0, 0);
            this.rotate(animator, fore_right_upper, 8.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_chest, 0, 0, -8.0F);
            this.rotate(animator, neck, 8.0F, 12.0F, 5.0F);
            this.rotate(animator, head_skull, 6.0F, 10.0F, 0);
            this.rotate(animator, fore_left_upper, 8.0F, 0, 0);
            this.rotate(animator, fore_right_upper, -10.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_GORE (14t) -- misnamed by the shared Animation field: a diprotodont SHOVES. The
        // whole body sinks back onto the haunches, then drives forward with the head low and the
        // jaw opening for a bite on the follow-through (EntityDiprotodont throws the target).
        animator.setAnimation(EntityDiprotodont.ATTACK_GORE);
        animator.startKeyframe(6);
        animator.move(body_barrel, 0, 0.6F, 2.0F);
        this.rotate(animator, body_barrel, -4.0F, 0, 0);
        this.rotate(animator, neck, -10.0F, 0, 0);
        this.rotate(animator, head_skull, -8.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        animator.move(body_barrel, 0, 0, -3.0F);
        this.rotate(animator, body_barrel, 3.0F, 0, 0);
        this.rotate(animator, neck, 14.0F, 0, 0);
        this.rotate(animator, head_skull, 12.0F, 0, 0);
        this.rotate(animator, head_jaw, 22.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }
""")

    a("    public void setupAnim(EntityDiprotodont dip, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(dip);
        float globalDegree = 1f;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);
        // Gait phase divided by the RENDER size: a 1.2-scale Diprotodon and a 0.4-scale Maokopia
        // cover very different ground per leg cycle, so one fixed rate would slide the big one's
        // feet and make the small one shuffle (the ModelDireWolf fix).
        float gait = limbSwing * 0.55F / Math.max(0.3F, dip.getMobSize() * dip.getScale());
""")
    a("        // ---- per-species toggles ----")
    a("        int variant = dip.getVariant();")
    a(f"        // nasal boss ON for {BOSS} (zygomaturus) ALONE")
    a("        this.nasal_boss.showModel = " + " || ".join(f"variant == {v}" for v in BOSS) + ";")
    a("        // joey: only while the mother is up -- lying down, it ducks back into the pouch")
    a("        this.joey_head.showModel = dip.hasJoey() && dip.sitProgress == 0 && dip.sleepProgress == 0;\n")

    a("""        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 24) * 0.04F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        bob(body_barrel, 0.3F, 0.08F, false, ageInTicks / 24, 2);
        walk(neck, 0.3f, 0.04f, false, 2.8F, 0.03F, ageInTicks / 24, 2);
        this.swing(ear_left, 0.16F, 0.18F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.16F, 0.18F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail, 0.10F, 0.12F, false, 0F, 0F, ageInTicks, 1);
        this.swing(joey_head, 0.08F, 0.20F, false, 0F, 0F, ageInTicks, 1);
""")
    a("        // Blinking: bury the eye planes inside the skull. y/z MUST keep the")
    a("        // constructor's values or the eye jumps across the face on every blink.")
    a("        if (!dip.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        // Head tracking: a short thick neck, so most of the turn happens at the skull.
        if (!dip.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 4, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head_skull);
        }

        if (dip.isInWater() && !dip.isOnGround()) {
            this.setRotateAngle(head_skull, -0.1820F, 0.0F, 0.0F);
            gait = ageInTicks / 6;
            limbSwingAmount = 0.5f;
            float pitch = Mth.clamp(dip.getXRot() - 10, -25F, 25.0F);
            this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
        }

        // ---- gait: pillar legs swing from the shoulder and hip with a small bend below; a lateral
        // sequence walk (hind leads its same-side fore by a quarter cycle), amplitudes kept LOW --
        // a two-tonne graviportal animal does not stride like a horse ----
        if (dip.canMove()) {
            bob(body_barrel, 2.0f, 0.5f * globalDegree, true, gait, limbSwingAmount);
            walk(neck, 1.0f, 0.10f * globalDegree, false, 0, 0, gait, limbSwingAmount);
            walk(fore_right_upper, 1.0f, 0.75f * globalDegree, false, 0F, 0F, gait, limbSwingAmount);
            walk(fore_right_forearm, 1.0f, 0.45f * globalDegree, false, -1.4F, 0.15F, gait, limbSwingAmount);
            walk(fore_right_foot, 1.0f, 0.35f * globalDegree, true, -1.4F, 0F, gait, limbSwingAmount);
            walk(fore_left_upper, 1.0f, 0.75f * globalDegree, false, 3.1416F, 0F, gait, limbSwingAmount);
            walk(fore_left_forearm, 1.0f, 0.45f * globalDegree, false, 1.7416F, 0.15F, gait, limbSwingAmount);
            walk(fore_left_foot, 1.0f, 0.35f * globalDegree, true, 1.7416F, 0F, gait, limbSwingAmount);
            walk(hind_right_thigh, 1.0f, 0.70f * globalDegree, false, 1.5708F, 0F, gait, limbSwingAmount);
            walk(hind_right_shank, 1.0f, 0.45f * globalDegree, true, 0.1708F, 0F, gait, limbSwingAmount);
            walk(hind_right_foot, 1.0f, 0.30f * globalDegree, false, 0.1708F, 0F, gait, limbSwingAmount);
            walk(hind_left_thigh, 1.0f, 0.70f * globalDegree, false, 4.7124F, 0F, gait, limbSwingAmount);
            walk(hind_left_shank, 1.0f, 0.45f * globalDegree, true, 3.3124F, 0F, gait, limbSwingAmount);
            walk(hind_left_foot, 1.0f, 0.30f * globalDegree, false, 3.3124F, 0F, gait, limbSwingAmount);
        }
""")

    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        d = P[pose]
        head = ("        if (dip.sitProgress > 0) {" if pose == "sit"
                else "        else if (dip.sleepProgress > 0) {")
        a(head)
        a("            // SOLVED by grid search against real box geometry (diprotodont_spec.py --pose):")
        a(f"            // the belly drops {abs(d['dy']):.2f}u, every leg box stays above y=0, the lower")
        a("            // segments rest ON the floor, and the feet keep their toe-in (rotY) throughout.")
        a(f"            this.progressPosition(body_barrel, dip.{key}, 0.0F, "
          f"{f(barrel_y - d['dy'])}, 0.0F, 40);")
        for n in names:
            if n in d["rot"]:
                rx, ry = d["rot"][n]
                a(f"            this.progressRotation({n}, dip.{key}, {f(rx)}, {f(ry)}, 0, 40);")
        lift = NECK_LIFT[pose]
        a(f"            // neck lifted {lift:.0f} deg (solved minimum {'2' if pose == 'sit' else '6'}) so the jaw clears the floor")
        a(f"            this.progressRotation(neck, dip.{key}, {f(-math.radians(neck_bb + lift))}, 0, 0, 40);")
        if pose == "sleep":
            a(f"            this.progressRotation(head_skull, dip.{key}, {f(by['head_skull']['rot'][0])}, 0, 0, 40);")
        a("        }\n")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes, boss={BOSS})")


if __name__ == "__main__":
    main()
