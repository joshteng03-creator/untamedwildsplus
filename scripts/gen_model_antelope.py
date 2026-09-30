#!/usr/bin/env python3
"""SOP phase 6 -- regenerate ModelAntelope.java from the Blockbench export.

ModelAntelope is EXTENDED, not rebuilt: the 36 shipped boxes must come out byte-identical
so saiga.png / pronghorn.png / springbok.png keep working. main() asserts that against the
file currently on disk before writing anything.

Every setRotationPoint / addBox / texOffs literal is copied VERBATIM from the export -- the
Y-flip is the export's, never re-derived. animate() and applyRestingPose() are reproduced
unchanged; only setupAnim() gains the per-species family switch and the heavy build.

Usage:  python scripts/gen_model_antelope.py <export.java> <ModelAntelope.java>
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from java2bb import parse                                          # noqa: E402
import antelope_horns_spec as S                                    # noqa: E402

EXPORT = pathlib.Path(sys.argv[1])
OUT = pathlib.Path(sys.argv[2])

PART_RE = re.compile(
    r'PartDefinition (\w+) = (\w+)\.addOrReplaceChild\("(\w+)",\s*'
    r'CubeListBuilder\.create\(\)\.texOffs\((\d+), (\d+)\)'
    r'\.addBox\(([^)]*?), new CubeDeformation\(0\.0F\)\),\s*'
    r'PartPose\.(offset|offsetAndRotation)\(([^)]*)\)\);')

SHIPPED = 36


def nums(s):
    return [float(x.strip().rstrip("Ff")) for x in s.split(",") if x.strip()]


def f(x):
    return f"{x:.4f}F".replace(".0000F", ".0F")


def parse_export():
    src = EXPORT.read_text()
    out = []
    for m in PART_RE.finditer(src):
        box, pose = nums(m.group(6)), nums(m.group(8))
        out.append(dict(name=m.group(3),
                        parent=None if m.group(2) == "partdefinition" else m.group(2),
                        u=int(m.group(4)), v=int(m.group(5)),
                        box=box[:3], size=box[3:6],
                        off=pose[:3], rot=pose[3:6] if len(pose) > 3 else [0.0, 0.0, 0.0]))
    return out


def assert_shipped_unchanged(parts):
    """The whole point of Track C: the shipped rects and boxes must not move."""
    order, uv, rp, box, rot, parent = parse("ModelAntelope")
    by = {p["name"]: p for p in parts}
    bad = []
    for n in order:
        p = by.get(n)
        if p is None:
            bad.append(f"{n}: dropped from the export")
            continue
        if (p["u"], p["v"]) != uv[n]:
            bad.append(f"{n}: texOffs MOVED {uv[n]} -> {(p['u'], p['v'])}")
        if any(abs(a - b) > 1e-3 for a, b in zip(p["box"] + p["size"], box[n])):
            bad.append(f"{n}: addBox changed")
        if any(abs(a - b) > 1e-3 for a, b in zip(p["off"], rp[n])):
            bad.append(f"{n}: rotationPoint changed")
        if any(abs(a - b) > 1e-3 for a, b in zip(p["rot"], rot.get(n, (0, 0, 0)))):
            bad.append(f"{n}: rotation changed")
        if p["parent"] != parent.get(n):
            bad.append(f"{n}: parent changed")
    if bad:
        raise SystemExit("SHIPPED GEOMETRY CHANGED -- the 3 existing skins would break:\n  "
                         + "\n  ".join(bad))
    print(f"  verified: all {len(order)} shipped boxes unchanged "
          f"(texOffs, addBox, rotationPoint, rotation, parent)")
    return order


def main():
    parts = parse_export()
    assert len(parts) == SHIPPED + 20, len(parts)
    shipped = assert_shipped_unchanged(parts)
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    new = [n for n in names if n not in set(shipped)]
    print(f"  {len(shipped)} shipped + {len(new)} new = {len(names)} boxes")

    # Blackbuck's tighter corkscrew is an ADDITIONAL yaw on the authored one, so the
    # literals come from the export and cannot drift out of sync with the geometry.
    tw = {n: by[n]["rot"][1] for n in
          ("spiral_2_left", "spiral_3_left", "spiral_2_right", "spiral_3_right")}

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
              "untamedwilds.entity.mammal.EntityAntelope"):
        a(f"import {i};")
    a("""
/**
 * 56 boxes on 256x128. The original 36-box gazelle rebuild is UNCHANGED -- every one of its
 * texOffs, addBox and PartPose literals is byte-identical, which is what lets saiga.png,
 * pronghorn.png and springbok.png keep working untouched. The 20 new boxes are packed into
 * rows 53..65, which were empty (the shipped footprints stop at v=52).
 *
 * FIVE horn families now share the one skull, all hanging off the same pedicle:
 *     GAZELLE   horn_base + horn_tip                springbok, pronghorn (+ front prong)
 *     FORKED    gazelle + front AND rear prong      tetrameryx  (the 4-horned pronghorn)
 *     SPIRAL    3 chained, progressively yawed      giant_eland, blackbuck
 *     RAPIER    2 chained, ruler-straight           gemsbok
 *     SCIMITAR  3 chained, arcing back              sable, bluebuck
 *
 * Families are selected with showModel on the chain ROOT, not setScale(0): ModelPart.render
 * returns before recursing into children when visible is false, so hiding the root hides
 * the whole chain -- and a zero scale would have to be paired with setShouldScaleChildren,
 * which fights the per-species sizing below.
 *
 * Segment-to-segment TWIST is load-bearing, not decoration: a box's X-face normal is
 * untouched by rx, so a chain that only varies its backsweep leaves consecutive side planes
 * near-parallel and z-fighting. Every joint here turns >8 degrees of yaw or roll.
 *
 * antelope.json declares one flag (hasBulbousNose, saiga); everything else -- horn family,
 * build, size -- reads getVariant(), the ModelEquid / ModelDeer pattern. All nine scales are
 * withers_m * 16 / 26.44, so 1 block = 1 metre at the shoulder.
 */""")
    a("public class ModelAntelope extends AdvancedEntityModel<EntityAntelope> {\n")
    for nm, var, fam, build, real in S.SPECIES:
        a(f"    private static final int {nm.upper()} = {var};")
    a("")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelAntelope() {")
    a("        this.texWidth = 256;")
    a("        this.texHeight = 128;\n")
    for p in parts:
        n = p["name"]
        a(f"        this.{n} = new AdvancedModelBox(this, {p['u']}, {p['v']});")
        a(f"        this.{n}.setRotationPoint({f(p['off'][0])}, {f(p['off'][1])}, {f(p['off'][2])});")
        a(f"        this.{n}.addBox({f(p['box'][0])}, {f(p['box'][1])}, {f(p['box'][2])}, "
          f"{f(p['size'][0])}, {f(p['size'][1])}, {f(p['size'][2])}, 0.0F);")
        if any(abs(r) > 1e-6 for r in p["rot"]):
            a(f"        this.setRotateAngle({n}, {f(p['rot'][0])}, {f(p['rot'][1])}, "
              f"{f(p['rot'][2])});")
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
    a("    @Override")
    a("    public Iterable<AdvancedModelBox> getAllParts() {")
    a("        return ImmutableList.of(")
    a(",\n".join(f"            {n}" for n in names))
    a("        );")
    a("    }\n")

    a("""    private void animate(IAnimatedEntity entityIn) {
        EntityAntelope antelope = (EntityAntelope) entityIn;
        animator.update(antelope);

        // Threat display: an antelope does not rear, it drops the neck to present the horns
        // and swings the head laterally while stamping a forefoot -- the "broadside" display.
        animator.setAnimation(EntityAntelope.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, neck_base, 26.09F, 0, -10.43F);
            this.rotate(animator, neck, 7.83F, 0, -7.83F);
            this.rotate(animator, head, -13.04F, 0, 0);
            this.rotate(animator, ear_left, 0, 0, -20.87F);
            this.rotate(animator, ear_right, 0, 0, 20.87F);
            animator.move(arm_right_1, 0, -0.8F, 0);
            this.rotate(animator, arm_right_1, -31.31F, 0, 0);
            this.rotate(animator, arm_right_2, 41.74F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, neck_base, 26.09F, 0, 13.04F);
            this.rotate(animator, neck, 7.83F, 0, 10.43F);
            this.rotate(animator, head, -13.04F, 0, 0);
            this.rotate(animator, ear_left, 0, 0, -20.87F);
            this.rotate(animator, ear_right, 0, 0, 20.87F);
            this.rotate(animator, arm_right_1, 5.21F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // Gore: coil the neck back, then hook the horns forward and UP through the target.
        animator.setAnimation(EntityAntelope.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck_base, -20.87F, 0, 0);
        this.rotate(animator, neck, -13.04F, 0, 0);
        this.rotate(animator, head, 20.87F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck_base, 41.74F, 0, 0);
        this.rotate(animator, neck, 20.87F, 0, 0);
        this.rotate(animator, head, -36.53F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }
""")

    a("    public void setupAnim(EntityAntelope antelope, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(antelope);
        float globalSpeed = 1.7f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- per-species parts -------------------------------------------------------
        // Written on EVERY branch every frame: one ModelAntelope instance is shared by every
        // antelope in the world, so a toggle left unset leaks the previous entity's species.
        int variant = antelope.getVariant();
        boolean gazelle  = variant == PRONGHORN || variant == SPRINGBOK || variant == TETRAMERYX;
        boolean frontPr  = variant == PRONGHORN || variant == TETRAMERYX;
        boolean rearPr   = variant == TETRAMERYX;
        boolean spiral   = variant == BLACKBUCK || variant == GIANT_ELAND;
        boolean rapier   = variant == GEMSBOK;
        boolean scimitar = variant == SABLE || variant == BLUEBUCK;
        boolean heavy    = variant == GIANT_ELAND || variant == GEMSBOK
                        || variant == SABLE || variant == BLUEBUCK;

        this.horn_base_left.showModel = gazelle;
        this.horn_base_right.showModel = gazelle;
        this.horn_prong_left.showModel = frontPr;
        this.horn_prong_right.showModel = frontPr;
        this.horn_prong_rear_left.showModel = rearPr;
        this.horn_prong_rear_right.showModel = rearPr;
        this.spiral_1_left.showModel = spiral;
        this.spiral_1_right.showModel = spiral;
        this.rapier_1_left.showModel = rapier;
        this.rapier_1_right.showModel = rapier;
        this.scimitar_1_left.showModel = scimitar;
        this.scimitar_1_right.showModel = scimitar;
        this.dewlap.showModel = variant == GIANT_ELAND;
        this.neck_mane.showModel = variant == GIANT_ELAND || variant == SABLE;

        // SPIRAL is authored at giant-eland proportions and SCIMITAR at sable's, because a
        // uniform scale can change SIZE but never SHAPE -- so the biggest member of each
        // family has to be the one actually drawn.
        float sp = variant == BLACKBUCK ? 0.82F : 1.0F;
        this.spiral_1_left.setShouldScaleChildren(true);
        this.spiral_1_right.setShouldScaleChildren(true);
        this.spiral_1_left.setScale(sp, sp, sp);
        this.spiral_1_right.setScale(sp, sp, sp);
        float sc = variant == BLUEBUCK ? 0.70F : 1.0F;
        this.scimitar_1_left.setShouldScaleChildren(true);
        this.scimitar_1_right.setShouldScaleChildren(true);
        this.scimitar_1_left.setScale(sc, sc, sc);
        this.scimitar_1_right.setScale(sc, sc, sc);
""")
    a("        // A blackbuck's corkscrew is far tighter than a giant eland's open twist, and")
    a("        // scale cannot express that -- so the extra turn is added to the AUTHORED yaw,")
    a("        // in the same direction (+ on the left, - on the right) so it tightens rather")
    a("        // than unwinds. The base literals come straight from the export.")
    a("        if (variant == BLACKBUCK) {")
    for n in ("spiral_2_left", "spiral_3_left"):
        a(f"            this.{n}.rotateAngleY = {f(tw[n] + 0.2618)};")
    for n in ("spiral_2_right", "spiral_3_right"):
        a(f"            this.{n}.rotateAngleY = {f(tw[n] - 0.2618)};")
    a("        }\n")
    a("""        // Heavy build for the hippotragines and tragelaphines. The torso is scaled in X
        // and Y, and the limb ROOTS are pushed outboard by hand to match: AdvancedModelBox
        // does not scale a child's rotation point, so a wider chest alone would swallow the
        // legs (chest 4.15 -> 4.65 against a shoulder whose outer face sits at 5.00). With
        // the roots at 1.16x the shoulder clears the barrel by 0.37 and the thigh by 0.78.
        float hw = heavy ? 1.08F : 1.0F;      // barrel width
        float cw = heavy ? 1.12F : 1.0F;      // chest / croup width
        float hh = heavy ? 1.10F : 1.0F;      // height
        float lx = heavy ? 1.16F : 1.0F;      // limb root lateral offset
        this.arm_left_1.setRotationPoint(3.1F * lx, -0.4F, 0.6F);
        this.arm_right_1.setRotationPoint(-3.1F * lx, -0.4F, 0.6F);
        this.leg_left_1.setRotationPoint(3.2F * lx, -2.6F, 0.5F);
        this.leg_right_1.setRotationPoint(-3.2F * lx, -2.6F, 0.5F);
        this.neck_base.setScale(heavy ? 1.22F : 1.0F, 1.0F, heavy ? 1.12F : 1.0F);
        this.body_croup.setScale(cw, hh, 1.0F);

        // Breathing Animation -- folded into the heavy-build scale rather than issued
        // separately, because setScale OVERWRITES; two calls would drop whichever came first.
        float bx = (float) (1.0F + Math.sin(ageInTicks / 20) * 0.06F);
        float by = (float) (1.0F + Math.sin(ageInTicks / 16) * 0.06F);
        this.body_barrel.setScale(bx * hw, by * hh, 1.0F);
        this.body_chest.setScale((float) (1.0F + Math.sin(ageInTicks / 20) * 0.05F) * cw,
                                 (float) (1.0F + Math.sin(ageInTicks / 16) * 0.05F) * hh, 1.0F);
        float nose = antelope.hasBulbousNose() ? 1F : 0F;
        this.nose_bulb.setScale(nose, nose, nose);

        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        walk(neck_base, 0.4f * globalSpeed, 0.03f, false, 2.8F, 0.05F, ageInTicks / 20, 2);
        walk(ear_left, 0.3f * globalSpeed, 0.09f, false, 1.4F, 0F, ageInTicks / 20, 2);
        walk(ear_right, 0.3f * globalSpeed, 0.09f, true, 1.4F, 0F, ageInTicks / 20, 2);
        walk(tail_dock, 0.3f * globalSpeed, 0.12f, false, 0F, 0F, ageInTicks / 20, 2);

        // Blinking Animation -- pull the eye planes inside the 4.0-wide skull (|x| 2.6 -> 1.0)
        // so they are occluded by the cheek rather than floating off it.
        if (!antelope.shouldRenderEyes()) {
            this.eye_right.setRotationPoint(-1F, -1.0F, -3.2F);
            this.eye_left.setRotationPoint(1F, -1.0F, -3.2F);
        }

        // Head Tracking Animation
        if (!antelope.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck_base);
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 3, head);
        }

        // Pitch/Yaw handler
        if (antelope.isInWater()) {
            this.setRotateAngle(head, -0.1745F, 0.0F, 0.0F);
            if (!antelope.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(antelope.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // Movement Animation -- four-beat walk with each segment phase-lagged behind the one
        // above it and at a lower amplitude, which is what keeps the thin cannons from
        // looking like rigid stilts.
        if (antelope.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.5f * globalDegree, true, f, limbSwingAmount);
            walk(neck_base, 0.8f * globalSpeed, 0.16f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head, 0.8f * globalSpeed, 0.12f * globalDegree, true, 0, 0, f, limbSwingAmount);
            walk(arm_right_1, -0.8f * globalSpeed, 1.2f * globalDegree, true, 0F, 1.2f, f, limbSwingAmount);
            walk(arm_right_2, -0.8f * globalSpeed, 1.0f * globalDegree, false, -1F, 1.2f, f, limbSwingAmount * 1.2f);
            walk(arm_right_3, -0.8f * globalSpeed, 0.6f * globalDegree, false, -2F, 0.6f, f, limbSwingAmount * 1.2f);
            walk(arm_left_1, -0.8f * globalSpeed, 1.2f * globalDegree, true, 2F, 1.2f, f, limbSwingAmount);
            walk(arm_left_2, -0.8f * globalSpeed, 1.0f * globalDegree, false, 1F, 1.2f, f, limbSwingAmount * 1.2f);
            walk(arm_left_3, -0.8f * globalSpeed, 0.6f * globalDegree, false, 0F, 0.6f, f, limbSwingAmount * 1.2f);
            walk(leg_right_1, 0.8f * globalSpeed, 1.2f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(leg_right_2, 0.8f * globalSpeed, 1.0f * globalDegree, true, 1.8F, 0, f, limbSwingAmount);
            walk(leg_right_3, 0.8f * globalSpeed, 0.6f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(leg_left_1, 0.8f * globalSpeed, 1.2f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(leg_left_2, 0.8f * globalSpeed, 1.0f * globalDegree, true, -0.2F, 0, f, limbSwingAmount);
            walk(leg_left_3, 0.8f * globalSpeed, 0.6f * globalDegree, false, -1.2F, 0, f, limbSwingAmount);
            walk(tail_dock, 0.8f * globalSpeed, 0.3f * globalDegree, false, 0F, 0F, f, limbSwingAmount);
        }

        // ---- resting -----------------------------------------------------------------
        // STERNAL RECUMBENCY, solved by search against the real boxes rather than guessed:
        // the body drops 12.82 units, the foreleg Z-folds (humerus back to world -22, radius
        // forward to world +90 putting the CARPUS on the ground, cannon folded back 177 deg
        // to lie along it), and the hind leg Z-folds the other way (femur forward, tibia back
        // to put the HOCK down, cannon lying forward) with a 30 deg outward splay -- folded
        // straight under, the hind cannon drove through the foreleg. Verified: lowest point
        // of the whole model is -0.01, the belly floats 1.78 clear of the folded limbs, and
        // fore and hind cannons clear each other by 5.15 units.
        // Every target below is ABSOLUTE (ticksToSit and the sleep counter both equal the
        // maxProgress of 40), so this pose is immune to changes in the standing defaults.
        if (antelope.sitProgress > 0) {
            applyRestingPose(antelope.sitProgress);
        }
        else if (antelope.sleepProgress > 0) {
            applyRestingPose(antelope.sleepProgress);
            // Asleep the neck relaxes forward and the ears drop; awake-but-sitting it stays alert.
            this.progressRotation(neck_base, antelope.sleepProgress, 0.7854F, 0, 0, 40);
            this.progressRotation(head, antelope.sleepProgress, -0.7854F, 0, 0, 40);
            this.progressRotation(ear_left, antelope.sleepProgress, 0.1745F, 0.4363F, 1.0472F, 40);
            this.progressRotation(ear_right, antelope.sleepProgress, 0.1745F, -0.4363F, -1.0472F, 40);
        }
    }

    private void applyRestingPose(float progress) {
        this.progressPosition(body_barrel, progress, 0.0F, 16.82F, 0.0F, 40);
        this.progressRotation(arm_right_1, progress, 0.4393F, 0, 0.0349F, 40);
        this.progressRotation(arm_right_2, progress, -1.9570F, 0, 0, 40);
        this.progressRotation(arm_right_3, progress, 3.0961F, 0, 0, 40);
        this.progressRotation(hoof_front_right, progress, 0.0449F, 0, 0, 40);
        this.progressRotation(arm_left_1, progress, 0.4393F, 0, -0.0349F, 40);
        this.progressRotation(arm_left_2, progress, -1.9570F, 0, 0, 40);
        this.progressRotation(arm_left_3, progress, 3.0961F, 0, 0, 40);
        this.progressRotation(hoof_front_left, progress, 0.0449F, 0, 0, 40);
        this.progressRotation(leg_right_1, progress, -0.4948F, 0.5236F, 0.0349F, 40);
        this.progressRotation(leg_right_2, progress, 2.2043F, 0, 0, 40);
        this.progressRotation(leg_right_3, progress, -2.8565F, 0, 0, 40);
        this.progressRotation(hoof_back_right, progress, -0.2141F, 0, 0, 40);
        this.progressRotation(leg_left_1, progress, -0.4948F, -0.5236F, -0.0349F, 40);
        this.progressRotation(leg_left_2, progress, 2.2043F, 0, 0, 40);
        this.progressRotation(leg_left_3, progress, -2.8565F, 0, 0, 40);
        this.progressRotation(hoof_back_left, progress, -0.2141F, 0, 0, 40);
    }
}""")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


if __name__ == "__main__":
    main()
