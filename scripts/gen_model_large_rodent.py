#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench export into ModelLargeRodent.java (Phase 4.6, 2026-10-10).

Literals VERBATIM from the modded_entity export; computed: the sit/sleep recumbency targets
(large_rodent_spec.pose_solve) and, per pose, the paddle tail's droop so it rests ON the ground instead of floating.

Usage:  python scripts/gen_model_large_rodent.py <export.java> <ModelLargeRodent.java>
"""
import math
import re
import sys
import pathlib
import io
import contextlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import large_rodent_spec as S                                      # noqa: E402

EXPORT = pathlib.Path(sys.argv[1])
OUT = pathlib.Path(sys.argv[2])
PART_RE = re.compile(
    r'PartDefinition (\w+) = (\w+)\.addOrReplaceChild\("(\w+)",\s*'
    r'CubeListBuilder\.create\(\)\.texOffs\((\d+), (\d+)\)'
    r'\.addBox\(([^)]*?), new CubeDeformation\(0\.0F\)\),\s*'
    r'PartPose\.(offset|offsetAndRotation)\(([^)]*)\)\);')
PADDLE = [v for _, v, _, t in S.SPECIES if t]


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


def tail_droop(over, dy):
    """Extra local droop on tail_base (Blockbench degrees) that lays the paddle on the ground in a rest pose."""
    rest = S.BY_NAME["tail_base"][3][0]
    for d in range(0, 90):
        o = dict(over)
        o["tail_base"] = (rest + d, 0, 0)
        low = min(S.world_mod(n, S.vol(S.BY_NAME[n]), o, dy)[:, 1].min() for n in ("tail_base", "tail_paddle"))
        if low <= 0.15:
            return rest + d
    return rest + 89


def main():
    parts = parse()
    assert len(parts) == len(S.PARTS) == 30, (len(parts), len(S.PARTS))
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    with contextlib.redirect_stdout(io.StringIO()):
        sol = S.pose_solve(verbose=False)
    root_y = by["body_barrel"]["off"][1]
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
              "untamedwilds.entity.mammal.EntityLargeRodent"):
        a(f"import {i};")
    a("""
/**
 * Large rodents -- capybara, Neochoerus, Castoroides, North American beaver (Phase 4.6, 2026-10-10): 30 boxes on 256x128,
 * authored in scripts/large_rodent_spec.py, ported VERBATIM by scripts/gen_model_large_rodent.py, proved by
 * verify_port.py. A heavy barrel on short legs with a long blunt head and orange incisors.
 *   * The beavers' paddle tail (variant-gated) is carried level behind on land, lies on the ground at rest, sculls in
 *     water, and SLAPS the surface as an alarm (ATTACK_SLAP).
 *   * The bird passenger is shown per entity (EntityLargeRodent.hasBird(), synced) -- capybaras only.
 */""")
    a("public class ModelLargeRodent extends AdvancedEntityModel<EntityLargeRodent> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelLargeRodent() {")
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
        EntityLargeRodent rodent = (EntityLargeRodent) entityIn;
        animator.update(rodent);

        // ATTACK_BITE (12t): head drops and lunges, incisors first
        animator.setAnimation(EntityLargeRodent.ATTACK_BITE);
        animator.startKeyframe(5);
        this.rotate(animator, neck, -12.0F, 0, 0);
        this.rotate(animator, head, -8.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        animator.move(body_barrel, 0, 0, -1.2F);
        this.rotate(animator, neck, 16.0F, 0, 0);
        this.rotate(animator, head, 10.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(3);

        // ATTACK_SLAP (16t): the beaver alarm -- tail raised high, then slammed down flat
        animator.setAnimation(EntityLargeRodent.ATTACK_SLAP);
        animator.startKeyframe(7);
        this.rotate(animator, tail_base, -40.0F, 0, 0);
        this.rotate(animator, tail_paddle, -15.0F, 0, 0);
        this.rotate(animator, body_barrel, 4.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(3);
        this.rotate(animator, tail_base, 35.0F, 0, 0);
        this.rotate(animator, tail_paddle, 10.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(6);
    }
""")
    a("    public void setupAnim(EntityLargeRodent rodent, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(rodent);
        limbSwingAmount = Math.min(0.5F, limbSwingAmount);
        float gait = limbSwing * 0.8F / Math.max(0.3F, rodent.getMobSize() * rodent.getScale());
        boolean swimming = rodent.isInWater() && !rodent.isOnGround();
""")
    a("        int variant = rodent.getVariant();")
    cond = " || ".join(f"variant == {v}" for v in PADDLE)
    a(f"        this.tail_base.showModel = {cond};")
    a("        this.bird_body.showModel = rodent.hasBird();\n")
    a("""        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.03F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(ear_left, 0.18F, 0.15F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.18F, 0.15F, false, 1F, 0F, ageInTicks, 1);
        this.walk(bird_head, 0.35F, 0.25F, false, 0F, 0F, ageInTicks, 1);
        this.swing(bird_head, 0.12F, 0.5F, false, 1F, 0F, ageInTicks, 1);
""")
    a("        if (!rodent.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        if (!rodent.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        if (swimming) {
            // swimming low with only the head up: legs paddle, the paddle tail sculls up and down
            float sw = ageInTicks * 0.3F;
            this.neck.rotateAngleX -= 0.25F;
            this.fore_left_upper.rotateAngleX += Mth.cos(sw) * 0.6F;
            this.fore_right_upper.rotateAngleX += Mth.cos(sw + Mth.PI) * 0.6F;
            this.hind_left_thigh.rotateAngleX += Mth.cos(sw + Mth.PI) * 0.7F;
            this.hind_right_thigh.rotateAngleX += Mth.cos(sw) * 0.7F;
            this.tail_base.rotateAngleX += Mth.cos(sw * 0.7F) * 0.2F;
            this.tail_paddle.rotateAngleX += Mth.cos(sw * 0.7F - 0.8F) * 0.25F;
        }
        else if (rodent.canMove()) {
            // a short-stepping diagonal walk, the body rolling slightly over the stubby legs
            bob(body_barrel, 2.0F, 0.4F, true, gait, limbSwingAmount);
            walk(fore_left_upper, 1.0F, 0.7F, false, 0F, 0F, gait, limbSwingAmount);
            walk(fore_left_lower, 1.0F, 0.4F, false, -1.3F, 0.1F, gait, limbSwingAmount);
            walk(fore_right_upper, 1.0F, 0.7F, false, Mth.PI, 0F, gait, limbSwingAmount);
            walk(fore_right_lower, 1.0F, 0.4F, false, Mth.PI - 1.3F, 0.1F, gait, limbSwingAmount);
            walk(hind_left_thigh, 1.0F, 0.65F, false, Mth.PI, 0F, gait, limbSwingAmount);
            walk(hind_left_shank, 1.0F, 0.45F, true, Mth.PI - 1.2F, 0F, gait, limbSwingAmount);
            walk(hind_right_thigh, 1.0F, 0.65F, false, 0F, 0F, gait, limbSwingAmount);
            walk(hind_right_shank, 1.0F, 0.45F, true, -1.2F, 0F, gait, limbSwingAmount);
            swing(body_barrel, 1.0F, 0.05F, false, 0F, 0F, gait, limbSwingAmount);
            walk(tail_base, 2.0F, 0.06F, false, 0F, 0F, gait, limbSwingAmount);
        }
""")
    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        head = "        if (rodent.sitProgress > 0) {" if pose == "sit" else "        else if (rodent.sleepProgress > 0) {"
        a(head)
        dy = sol[(pose, "fore")][1]
        over = {}
        for prefix in ("fore", "hind"):
            for side in ("left", "right"):
                over.update(S._leg_rot(prefix, side, sol[(pose, prefix)][0]))
        a("            // SOLVED (large_rodent_spec.py --pose): belly down, legs folded, the paddle tail laid on the ground")
        a(f"            this.progressPosition(body_barrel, rodent.{key}, 0.0F, {f(root_y - dy)}, 0.0F, 40);")
        for nm, r in over.items():
            a(f"            this.progressRotation({nm}, rodent.{key}, {f(-math.radians(r[0]))}, 0, 0, 40);")
        td = tail_droop(over, dy)
        a(f"            this.progressRotation(tail_base, rodent.{key}, {f(-math.radians(td))}, 0, 0, 40);")
        nrest = S.BY_NAME["neck"][3][0]
        extra = 6.0 if pose == "sit" else -8.0
        a(f"            this.progressRotation(neck, rodent.{key}, {f(-math.radians(nrest + extra))}, 0, 0, 40);")
        a("        }")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes, paddle={PADDLE})")


if __name__ == "__main__":
    main()
