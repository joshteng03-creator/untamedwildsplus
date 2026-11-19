#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench export into ModelPinniped.java (Phase 4.4, 2026-10-10).

Box/pivot/texOffs literals VERBATIM from the modded_entity export. Computed numbers: the Steller sea lion's SOLVED
land stance (pinniped_spec.stance_sealion: chest raised on lengthened fore flippers, belly flat) and the two rest
poses (pinniped_spec.rest_poses: the 'banana' sit, the flat sleep).

Usage:  python scripts/gen_model_pinniped.py <export.java> <ModelPinniped.java>
"""
import math
import re
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import pinniped_spec as S                                           # noqa: E402

EXPORT = pathlib.Path(sys.argv[1])
OUT = pathlib.Path(sys.argv[2])
PART_RE = re.compile(
    r'PartDefinition (\w+) = (\w+)\.addOrReplaceChild\("(\w+)",\s*'
    r'CubeListBuilder\.create\(\)\.texOffs\((\d+), (\d+)\)'
    r'\.addBox\(([^)]*?), new CubeDeformation\(0\.0F\)\),\s*'
    r'PartPose\.(offset|offsetAndRotation)\(([^)]*)\)\);')

WHISKERS = [v for _, v, _, w, _, _ in S.SPECIES if w]
TUSKS = [v for _, v, _, _, t, _ in S.SPECIES if t]
EARS = [v for _, v, _, _, _, e in S.SPECIES if e]


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


def jrot(r):
    """Blockbench local degrees -> MC radians (the verified flip: -x, -y, +z)."""
    return -math.radians(r[0]), -math.radians(r[1]), math.radians(r[2])


def or_list(vs):
    return " || ".join(f"variant == {v}" for v in vs) or "false"


def main():
    parts = parse()
    assert len(parts) == len(S.PARTS) == 18, (len(parts), len(S.PARTS))
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    root_y = by["body_chest"]["off"][1]
    eye = by["eye_left"]["off"]
    st = S.stance_sealion()
    _, st_over, st_dy, _, _, st_k = st
    rest = S.rest_poses()

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
              "untamedwilds.entity.mammal.EntityPinniped"):
        a(f"import {i};")
    a("""
/**
 * Pinnipeds -- walrus, bearded seal, ringed seal, harp seal, Steller sea lion (Phase 4.4, 2026-10-10): 18 boxes on 256x128,
 * authored in scripts/pinniped_spec.py, ported VERBATIM by scripts/gen_model_pinniped.py, proved by verify_port.py.
 *
 * REST = hauled out lying prone: belly on the ground, head raised, broad fore paddles swept back with their tips on the
 * ground, hind fans side by side drooping to the ground behind.
 *   * Per species (getVariant()): whisker pad (walrus, bearded seal), tusks (walrus), ear flaps (sea lion).
 *   * The Steller sea lion stands in its SOLVED otariid stance on land -- chest raised 24 deg on fore flippers 1.45x
 *     longer (setScale on the flipper's long axis), belly counter-rotated flat -- and walks on all four flippers.
 *   * Seals GALUMPH on land (a model-only body wave and bounce, the way true seals hump along on their bellies) and
 *     SWIM with a lateral body wave driven by sweeping hind flippers, fore flippers tucked; the sea lion swims by
 *     flapping its fore flippers instead.
 * resetToDefaultPose does not restore scale, so the flipper scale is written on both branches every frame.
 */""")
    a("public class ModelPinniped extends AdvancedEntityModel<EntityPinniped> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelPinniped() {")
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
        EntityPinniped seal = (EntityPinniped) entityIn;
        animator.update(seal);

        // ATTACK_BITE (12t): the head draws back, then lunges forward and down
        animator.setAnimation(EntityPinniped.ATTACK_BITE);
        animator.startKeyframe(5);
        this.rotate(animator, neck, -10.0F, 0, 0);
        this.rotate(animator, head, -6.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, 18.0F, 0, 0);
        this.rotate(animator, head, 8.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(3);

        // ATTACK_TUSK (16t): the walrus rears its head and chops DOWN with the tusks
        animator.setAnimation(EntityPinniped.ATTACK_TUSK);
        animator.startKeyframe(6);
        this.rotate(animator, neck, -22.0F, 0, 0);
        this.rotate(animator, head, -10.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, 30.0F, 0, 0);
        this.rotate(animator, head, 16.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(6);

        // ATTACK_JOUST (40t): cosmetic tusk-jousting between bull walruses -- chests raised, heads swung down in turn
        animator.setAnimation(EntityPinniped.ATTACK_JOUST);
        animator.startKeyframe(6);
        this.rotate(animator, body_chest, -14.0F, 0, 0);
        this.rotate(animator, neck, -18.0F, 0, 0);
        animator.endKeyframe();
        for (int i = 0; i < 3; i++) {
            animator.startKeyframe(5);
            this.rotate(animator, body_chest, -14.0F, 0, 0);
            this.rotate(animator, neck, 20.0F, 10.0F, 0);
            this.rotate(animator, head, 10.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(5);
            this.rotate(animator, body_chest, -14.0F, 0, 0);
            this.rotate(animator, neck, -18.0F, -10.0F, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(4);
    }
""")
    a("    public void setupAnim(EntityPinniped seal, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(seal);
        limbSwingAmount = Math.min(0.5F, limbSwingAmount);
        float amt = Math.min(1.0F, limbSwingAmount / 0.3F);
        float gait = limbSwing * 0.7F / Math.max(0.3F, seal.getMobSize() * seal.getScale());
        boolean swimming = seal.isInWater() && !seal.isOnGround();
""")
    a("        // ---- per-species parts ----")
    a("        int variant = seal.getVariant();")
    a(f"        this.whiskers.showModel = {or_list(WHISKERS)};")
    a(f"        boolean tusks = {or_list(TUSKS)};")
    a("        this.tusk_left.showModel = tusks;")
    a("        this.tusk_right.showModel = tusks;")
    a(f"        boolean otariid = {or_list(EARS)};")
    a("        this.ear_left.showModel = otariid;")
    a("        this.ear_right.showModel = otariid;")
    a(f"        this.fore_left.setScale(1.0F, otariid ? {f(st_k)} : 1.0F, 1.0F);")
    a(f"        this.fore_right.setScale(1.0F, otariid ? {f(st_k)} : 1.0F, 1.0F);")
    a("        if (otariid && !swimming && seal.sleepProgress == 0) {")
    a("            // the SOLVED otariid stance (pinniped_spec.stance_sealion): chest up on the fore flippers, belly flat")
    a(f"            this.body_chest.rotationPointY -= {f(st_dy)};")
    for n, r in st_over.items():
        x, y, z = jrot(r)
        a(f"            this.setRotateAngle({n}, {f(x)}, {f(y)}, {f(z)});")
    a("        }\n")
    a("""        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 22) * 0.03F);
        this.body_belly.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(hind_left, 0.08F, 0.08F, false, 0F, 0F, ageInTicks, 1);
        this.swing(hind_right, 0.08F, 0.08F, false, 1F, 0F, ageInTicks, 1);
""")
    a("        // Blinking: bury the eye planes inside the skull; y/z keep the constructor's values.")
    a("        if (!seal.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        if (!seal.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        if (swimming) {
            float sw = ageInTicks * 0.25F;
            float pitch = Mth.clamp(seal.getXRot(), -30F, 30F);
            this.body_chest.rotateAngleX += (float) Math.toRadians(pitch);
            if (otariid) {
                // sea lions fly through the water on their fore flippers
                this.fore_left.rotateAngleZ += Mth.sin(sw) * 0.6F;
                this.fore_right.rotateAngleZ -= Mth.sin(sw) * 0.6F;
                this.body_chest.rotateAngleY += Mth.cos(sw) * 0.04F;
            }
            else {
                // true seals scull with the hind flippers: a lateral wave down the body, fore flippers tucked
                this.fore_left.rotateAngleZ += 0.5F;
                this.fore_right.rotateAngleZ -= 0.5F;
                this.body_chest.rotateAngleY += Mth.cos(sw) * 0.06F;
                this.body_belly.rotateAngleY += Mth.cos(sw - 0.8F) * 0.10F;
                this.body_hips.rotateAngleY += Mth.cos(sw - 1.6F) * 0.16F;
                this.hind_left.rotateAngleY += Mth.cos(sw - 2.4F) * 0.45F;
                this.hind_right.rotateAngleY += Mth.cos(sw - 2.4F) * 0.45F;
            }
        }
        else if (seal.canMove() && limbSwingAmount > 0.02F) {
            if (otariid) {
                // sea lions walk on all four flippers
                this.fore_left.rotateAngleX += Mth.cos(gait) * 0.5F * amt;
                this.fore_right.rotateAngleX += Mth.cos(gait + Mth.PI) * 0.5F * amt;
                this.hind_left.rotateAngleX += Mth.cos(gait + Mth.PI) * 0.3F * amt;
                this.hind_right.rotateAngleX += Mth.cos(gait) * 0.3F * amt;
                this.body_chest.rotateAngleY += Mth.cos(gait) * 0.05F * amt;
            }
            else {
                // GALUMPH: the chest heaves up and forward, the wave rolls back down the body, the flippers push
                float sn = Mth.sin(gait);
                this.body_chest.rotateAngleX -= sn * 0.10F * amt;
                this.body_belly.rotateAngleX += sn * 0.08F * amt;
                this.body_hips.rotateAngleX += Mth.sin(gait - 1.0F) * 0.10F * amt;
                this.body_chest.rotationPointY -= Math.max(0, sn) * 1.2F * amt;
                this.fore_left.rotateAngleX += Mth.cos(gait) * 0.5F * amt;
                this.fore_right.rotateAngleX += Mth.cos(gait) * 0.5F * amt;
                this.hind_left.rotateAngleX -= sn * 0.3F * amt;
                this.hind_right.rotateAngleX -= sn * 0.3F * amt;
            }
        }
""")
    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        over, dy = rest[pose]
        head = "        if (seal.sitProgress > 0) {" if pose == "sit" else "        else if (seal.sleepProgress > 0) {"
        a(head)
        a("            // pinniped_spec.rest_poses: " + ("the 'banana' -- head and hind flippers lifted" if pose == "sit"
                                                      else "lying flat, chin down on the ground"))
        for n, r in over.items():
            x, y, z = jrot(r)
            a(f"            this.progressRotation({n}, seal.{key}, {f(x)}, {f(y)}, {f(z)}, 40);")
        a("        }")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes, whiskers={WHISKERS} tusks={TUSKS} ears={EARS})")


if __name__ == "__main__":
    main()
