#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench export into ModelOrca.java (2.9.0, NEW TYPE orca).

Box / pivot / texOffs literals VERBATIM from the modded_entity export (proved by verify_port.py). Nothing is solved:
the animal floats, so there is no ground contact to solve for (orca_spec.py keeps the fins above the hitbox floor for a
stranded animal).

Usage:  python scripts/gen_model_orca.py <export.java> <ModelOrca.java>
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import orca_spec as S                                               # noqa: E402

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
    names = [p["name"] for p in parts]
    by = {p["name"]: p for p in parts}
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
              "untamedwilds.entity.ComplexMob",
              "untamedwilds.entity.mammal.EntityOrca"):
        a(f"import {i};")
    a(f"""
/**
 * Toothed whales -- orca and false killer whale (2.9.0, NEW TYPE): {len(parts)} boxes on {S.CANVAS[0]}x{S.CANVAS[1]}, authored in
 * scripts/orca_spec.py, ported VERBATIM by scripts/gen_model_orca.py, proved by verify_port.py.
 *
 * A deep fusiform body round in section (core + back ridge + belly keel), a blunt head with a hinged jaw, a tail stock
 * into broad swept flukes, big paddle pectorals and a two-segment dorsal fin. Cetaceans swim with an UP-AND-DOWN fluke
 * stroke (chainWave on the tail chain, never a fish's side-to-side swing); turning bends the tail through the yaw ring
 * buffer, and the body pitches with the animal's dive angle.
 *   * dorsal height per animal: male orca 1.0, female 0.55, `finScale` (x1000) for species with a small fin -- setScale
 *     on dorsal_1 with its child, about its root on the back. Written every frame: the model is shared.
 */""")
    a("public class ModelOrca extends AdvancedEntityModel<EntityOrca> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;")
    a("    private final AdvancedModelBox[] tailParts;\n")
    a("    public ModelOrca() {")
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
    a("        this.dorsal_1.setShouldScaleChildren(true);")
    a("        this.tailParts = new AdvancedModelBox[]{body_rear, tail_1, tail_2};")
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
        // ATTACK_BITE (14t): the head draws back, then lunges with the jaw dropped open, and snaps shut
        animator.setAnimation(EntityOrca.ATTACK_BITE);
        animator.startKeyframe(4);
        this.rotate(animator, head, -8.0F, 0, 0);
        this.rotate(animator, jaw, 30.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, head, 6.0F, 0, 0);
        this.rotate(animator, jaw, 34.0F, 0, 0);
        animator.move(body_main, 0, 0, -2.0F);
        animator.endKeyframe();
        animator.startKeyframe(2);
        this.rotate(animator, head, 4.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityOrca orca, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(orca);

        // ---- dorsal fin height per animal (scale is not restored by resetToDefaultPose: write it every frame) ----
        int fin = ComplexMob.speciesFlag(orca, "finScale");
        float finScale = fin > 0 ? fin / 1000F : (orca.isMale() ? 1.0F : 0.55F);
        this.dorsal_1.setScale(1.0F, finScale, 1.0F);

        // ---- breathing ----
        float breath = (float) (Math.sin(ageInTicks / 24) * 0.025F);
        this.body_main.setScale(1.0F + breath, 1.0F + breath, 1.0F);

        boolean swimming = orca.isInWater();
        if (swimming) {
            this.body_main.rotateAngleX += Mth.clamp(orca.getXRot(), -35, 35) * ((float) Math.PI / 180F);
            // the fluke stroke: always on (a whale never stops sculling), stronger when it is going somewhere
            float amt = 0.35F + Math.min(0.65F, limbSwingAmount * 1.5F);
            float speed = orca.isAngry() ? 0.32F : 0.2F;
            this.chainWave(tailParts, speed, 0.16F * amt, -2.5, ageInTicks, 1.0F);
            this.body_main.rotateAngleX += Mth.cos(ageInTicks * speed) * 0.025F * amt;
            this.flap(fin_left, speed, 0.12F, false, 0.5F, 0F, ageInTicks, amt);
            this.flap(fin_right, speed, 0.12F, true, 0.5F, 0F, ageInTicks, amt);
            float partialTicks = ageInTicks - orca.tickCount;
            float renderYaw = (float) orca.getMovementOffsets(0, partialTicks)[0];
            this.tail_1.rotateAngleY += smartClamp((float) orca.getMovementOffsets(12, partialTicks)[0] - renderYaw, -30, 30) * ((float) Math.PI / 180F);
            this.tail_2.rotateAngleY += smartClamp((float) orca.getMovementOffsets(16, partialTicks)[0] - renderYaw, -30, 30) * ((float) Math.PI / 180F);
            this.body_main.rotateAngleZ += smartClamp((float) orca.getMovementOffsets(6, partialTicks)[0] - renderYaw, -15, 15) * ((float) Math.PI / 180F);
        }
        else {
            // stranded: the flukes lift and fall
            this.tail_1.rotateAngleX += Mth.cos(ageInTicks * 0.15F) * 0.12F;
        }
""")
    a("        // Blinking: bury the eye planes inside the head; y/z keep the constructor's values.")
    a("        if (!orca.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.5)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.5)}, {f(eye[1])}, {f(eye[2])});")
    a("        }")
    a("    }\n")
    a("""    // getMovementOffsets can return a large negative angle, which throws off a plain clamp
    public float smartClamp(float angle, int min, int max) {
        float val = Math.abs(angle);
        if (val > 180) {
            angle = 360 - val;
        }
        return Mth.clamp(angle, min, max);
    }
}""")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


if __name__ == "__main__":
    main()
