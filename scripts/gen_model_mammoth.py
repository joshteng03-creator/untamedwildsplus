#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench export of the REBUILT proboscidean rig into ModelMammoth.java (2026-11-15).

Box / pivot / texOffs literals are VERBATIM from the modded_entity export (proved by verify_port.py). The only computed
numbers come from proboscidean_spec.py:
  * the per-family tables -- tusk segment angles for each tusk shape (authored by world direction), topline scales per
    back shape, head/neck pitch per head shape, ear yaw/scale/pivot per earSize -- converted to Java axes
    (rotation (-x, -y, +z) radians; child offsets (-dx, -dy, +dz));
  * the SOLVED rest / sleep poses (pose_solve): body drop, leg folds, tail swing, coat squash, head lift per head shape.

Usage:  python scripts/gen_model_mammoth.py <export.java> <ModelMammoth.java>
"""
import io
import math
import pathlib
import re
import sys
from contextlib import redirect_stdout

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import proboscidean_spec as S                                       # noqa: E402

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


def jr(deg_xyz):
    """Blockbench euler (deg) -> Java rotateAngle (rad): (-x, -y, +z)."""
    a, b, c = deg_xyz
    return (-math.radians(a), -math.radians(b), math.radians(c))


def arr(xs):
    return "{" + ", ".join(f(x) for x in xs) + "}"


def main():
    parts = parse()
    assert len(parts) == len(S.PARTS), (len(parts), len(S.PARTS))
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    with redirect_stdout(io.StringIO()):
        sol = S.pose_solve(verbose=False)
    print(f"pose: drop {sol['dy']:+.2f}  tail {sol['tail']:.0f}  fur {sol['fur']}  lifts {sol['lifts']}")

    # ---- family tables ----
    tusk_tab = []
    for fam in sorted(S.TUSK, key=S.TUSK.get):
        sp = ("x", 0, 1.0, "DOME", "SLOPE", fam, False, False, 0)
        over, _ = S.config(sp)
        sides = []
        for side in ("left", "right"):
            sides.append([jr(over[f"tusk_{side}_{k}"]["r"]) for k in range(1, 6)])
        tusk_tab.append((fam, sides))
    back_tab = [(fam, S.BACK_SCALE[fam]) for fam in sorted(S.BACK, key=S.BACK.get)]
    head_tab = [(fam, -math.radians(S.HEAD_PITCH[fam][0]), -math.radians(S.HEAD_PITCH[fam][1]))
                for fam in sorted(S.HEAD, key=S.HEAD.get)]
    head_o = S.BY_NAME["head"][2]
    ear_tab = []
    for size in (0, 1, 2):
        yaw, ys, zs, py, pz = S.EAR[size]
        ear_tab.append((size, math.radians(yaw), ys, zs, -(py - head_o[1]), pz - head_o[2]))
    ear_x = 7.6
    # the default ear pivot must match the export (the rig is built at the unscaled ear size, earSize 2)
    base = next(e for e in ear_tab if abs(e[2] - 1.0) < 1e-9 and abs(e[3] - 1.0) < 1e-9)
    assert abs(by["ear_left"]["off"][1] - base[4]) < 1e-3 and abs(by["ear_left"]["off"][2] - base[5]) < 1e-3, \
        (by["ear_left"]["off"], base)
    lifts = {pose: [sol["lifts"][pose].get(i, max(sol["lifts"][pose].values())) for i in range(5)] for pose in ("sit", "sleep")}
    root_y = by["body_mid"]["off"][1]

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
              "untamedwilds.entity.mammal.EntityMammoth"):
        a(f"import {i};")
    a("""
/**
 * Proboscideans -- mammoths, mastodons, gomphotheres and elephants. FULL REBUILD (2026-11-15) of the July rig, which was
 * sculpted by eye before the remodelling SOP: its ears were flat slabs 2.5 cm proud of the skull, every species wore the
 * mammoth's domed head, hump and 185-degree spiral tusks, the body was 0.60 as wide as long and the legs were stacked planks.
 *
 * 57 boxes on 256x256, authored in scripts/proboscidean_spec.py, ported VERBATIM by scripts/gen_model_mammoth.py and proved
 * by verify_port.py. One mesh serves every species through FAMILIES read from the species flags (0 = the mammoth form, so a
 * species without the new flags is a mammoth):
 *   headShape 0 DOME / 1 TWIN (Asian) / 2 SLOPE (African) / 3 CREST (Palaeoloxodon) / 4 FLAT (mastodons, gomphotheres)
 *   backShape 0 SLOPE (mammoth hump) / 1 DIP (African) / 2 ARCH (Asian) / 3 LEVEL
 *   tuskShape 0 SPIRAL / 1 FORWARD / 2 STRAIGHT / 3 UPCURVE / 4 TWIST / 5 SMALL (first three segments only) / 6 STEGO
 *   earSize 0 / 1 / 2 (hinged flaps yawed off the skull), hasLargeTusks (x1.35 whole chain), hasWoollyCoat.
 * The model instance is SHARED by every proboscidean, so every family setting is written every frame, and before the
 * keyframe animations, which add on top of it.
 */""")
    a("public class ModelMammoth extends AdvancedEntityModel<EntityMammoth> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    // ---- family tables (generated; Java axes) ----")
    a("    // TUSK_ROT[tuskShape][side 0 left / 1 right][segment][x, y, z] -- each family authored by world direction per segment")
    a("    private static final float[][][][] TUSK_ROT = {")
    for fam, sides in tusk_tab:
        a(f"        {{   // {fam}")
        for k, segs in enumerate(sides):
            a("            {" + ", ".join(arr(r) for r in segs) + "}" + ("," if k == 0 else ""))
        a("        },")
    a("    };")
    a("    // BACK_SCALE[backShape] = Y scale of {body_withers, body_back, body_croup} about each box's own pivot")
    a("    private static final float[][] BACK_SCALE = {" + ", ".join(arr(sc) for _, sc in back_tab) + "};   // "
      + " / ".join(fam for fam, _ in back_tab))
    a("    // extra pitch per headShape (rad): head tilts the face, neck raises or lowers the carriage")
    a("    private static final float[] HEAD_PITCH = " + arr([h for _, h, _ in head_tab]) + ";   // "
      + " / ".join(fam for fam, _, _ in head_tab))
    a("    private static final float[] NECK_PITCH = " + arr([n for _, _, n in head_tab]) + ";")
    a("    // EAR[earSize] = {yaw off the skull (rad), Y scale, Z scale, pivot y, pivot z}; the hinge is the ear's top front corner")
    a("    private static final float[][] EAR = {" + ", ".join(arr(e[1:]) for e in ear_tab) + "};")
    a("    // SOLVED rest-pose head lift per headShape (rad, split 60% neck / 40% head): the trunk and tusks clear the ground")
    a("    // resting, and rest on it asleep")
    a("    private static final float[] SIT_LIFT = " + arr([math.radians(x) for x in lifts["sit"]]) + ";")
    a("    private static final float[] SLEEP_LIFT = " + arr([math.radians(x) for x in lifts["sleep"]]) + ";\n")
    a("    public ModelMammoth() {")
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
        // hasLargeTusks scales a whole 5-box chain about its root, so the scale must reach the children
        this.tusk_left_1.setShouldScaleChildren(true);
        this.tusk_right_1.setShouldScaleChildren(true);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_mid);
    }

    /* Built once: Citadel asks for this every frame for every animal. */
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
        EntityMammoth mammoth = (EntityMammoth) entityIn;
        animator.update(mammoth);

        // Threat display: trumpet (head and trunk thrown up, ears flared, a forefoot raised), then stomp. Twice.
        animator.setAnimation(EntityMammoth.ATTACK_THREATEN);
        for (int beat = 0; beat < 2; beat++) {
            animator.startKeyframe(beat == 0 ? 12 : 10);
            this.rotate(animator, neck, -18.0F, 0, 0);
            this.rotate(animator, head, -12.0F, 0, 0);
            this.rotate(animator, ear_left, 0, 35.0F, 0);
            this.rotate(animator, ear_right, 0, -35.0F, 0);
            this.rotate(animator, leg_front_right_1, -30.0F, 0, 0);
            this.rotate(animator, leg_front_right_2, 40.0F, 0, 0);
            this.rotate(animator, foot_front_right, -10.0F, 0, 0);
            this.rotate(animator, trunk_1, -20.0F, 0, 0);
            this.rotate(animator, trunk_2, -30.0F, 0, 0);
            this.rotate(animator, trunk_3, -35.0F, 0, 0);
            this.rotate(animator, trunk_4, -35.0F, 0, 0);
            this.rotate(animator, trunk_5, -30.0F, 0, 0);
            this.rotate(animator, trunk_6, -25.0F, 0, 0);
            animator.endKeyframe();
            if (beat == 0) {
                animator.setStaticKeyframe(6);
            }
            animator.startKeyframe(7);
            this.rotate(animator, neck, 8.0F, 0, 6.0F);
            this.rotate(animator, head, 6.0F, 0, 4.0F);
            this.rotate(animator, ear_left, 0, 35.0F, 0);
            this.rotate(animator, ear_right, 0, -35.0F, 0);
            this.rotate(animator, trunk_1, -8.0F, 0, 0);
            this.rotate(animator, trunk_2, -6.0F, 0, 0);
            this.rotate(animator, trunk_3, 6.0F, 0, 0);
            this.rotate(animator, trunk_4, 10.0F, 0, 0);
            this.rotate(animator, trunk_5, 12.0F, 0, 0);
            this.rotate(animator, trunk_6, 14.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // Gore: head down with the trunk curled under out of harm's way, then a lunge that tosses the tusks up and
        // across. Shallower than the July rig's: the long straight and upcurved tusks reach far lower than its spirals.
        animator.setAnimation(EntityMammoth.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck, 12.0F, 0, 10.0F);
        this.rotate(animator, head, 8.0F, 0, 8.0F);
        this.rotate(animator, ear_left, 0, 25.0F, 0);
        this.rotate(animator, ear_right, 0, -25.0F, 0);
        this.rotate(animator, trunk_2, 26.0F, 0, 0);
        this.rotate(animator, trunk_3, 32.0F, 0, 0);
        this.rotate(animator, trunk_4, 28.0F, 0, 0);
        this.rotate(animator, trunk_5, 20.0F, 0, 0);
        this.rotate(animator, trunk_6, 14.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, -21.0F, 0, -26.0F);
        this.rotate(animator, head, -16.0F, 0, -18.0F);
        this.rotate(animator, ear_left, 0, 25.0F, 0);
        this.rotate(animator, ear_right, 0, -25.0F, 0);
        this.rotate(animator, trunk_2, 26.0F, 0, 0);
        this.rotate(animator, trunk_3, 32.0F, 0, 0);
        this.rotate(animator, trunk_4, 28.0F, 0, 0);
        this.rotate(animator, trunk_5, 20.0F, 0, 0);
        this.rotate(animator, trunk_6, 14.0F, 0, 0);
        animator.move(body_mid, 0, 0, -2.0F);
        animator.endKeyframe();
        animator.resetKeyframe(4);

        // Graze the way elephants do: the head dips only a little, the trunk reaches forward along the ground, the tip
        // curls round a mouthful, then the whole trunk coils back under the skull to the mouth.
        animator.setAnimation(EntityMammoth.EAT);
        animator.startKeyframe(10);
        this.rotate(animator, neck, 8.0F, 0, 0);
        this.rotate(animator, head, 4.0F, 0, 0);
        this.rotate(animator, trunk_1, -6.0F, 0, 0);
        this.rotate(animator, trunk_2, -10.0F, 0, 0);
        this.rotate(animator, trunk_3, -6.0F, 0, 0);
        this.rotate(animator, trunk_4, 10.0F, 0, 0);
        this.rotate(animator, trunk_5, 22.0F, 0, 0);
        this.rotate(animator, trunk_6, 30.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(6);
        this.rotate(animator, neck, 8.0F, 0, 0);
        this.rotate(animator, head, 4.0F, 0, 0);
        this.rotate(animator, trunk_1, -6.0F, 0, 0);
        this.rotate(animator, trunk_2, -10.0F, 0, 0);
        this.rotate(animator, trunk_3, -6.0F, 0, 0);
        this.rotate(animator, trunk_4, 22.0F, 0, 0);
        this.rotate(animator, trunk_5, 38.0F, 0, 0);
        this.rotate(animator, trunk_6, 50.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(10);
        this.rotate(animator, neck, -4.0F, 0, 0);
        this.rotate(animator, trunk_1, -11.0F, 0, 0);
        this.rotate(animator, trunk_2, 39.0F, 0, 0);
        this.rotate(animator, trunk_3, 63.0F, 0, 0);
        this.rotate(animator, trunk_4, 68.0F, 0, 0);
        this.rotate(animator, trunk_5, 59.0F, 0, 0);
        this.rotate(animator, trunk_6, 50.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(6);
        animator.resetKeyframe(8);
    }

    public void setupAnim(EntityMammoth mammoth, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();

        // ---- FAMILIES (every frame: one model instance serves every proboscidean) ----
        // (locals are *F: a local named `head` would shadow the head part)
        int headF = Mth.clamp(mammoth.getHeadShape(), 0, 4);
        int backF = Mth.clamp(mammoth.getBackShape(), 0, 3);
        int tuskF = Mth.clamp(mammoth.getTuskShape(), 0, TUSK_ROT.length - 1);
        int earF = Mth.clamp(mammoth.getEarSize(), 0, 2);
        boolean coat = mammoth.hasWoollyCoat();
        this.head_dome.showModel = headF == 0;
        this.head_dome_left.showModel = headF == 1;
        this.head_dome_right.showModel = headF == 1;
        this.head_crest.showModel = headF == 3;
        this.body_hump.showModel = backF == 0;
        this.fur_neck.showModel = coat;
        this.fur_mane.showModel = coat;
        this.fur_chest.showModel = coat;
        this.fur_rump.showModel = coat;
        this.fur_skirt_left.showModel = coat;
        this.fur_skirt_right.showModel = coat;
        this.body_withers.setScale(1.0F, BACK_SCALE[backF][0], 1.0F);
        this.body_back.setScale(1.0F, BACK_SCALE[backF][1], 1.0F);
        this.body_croup.setScale(1.0F, BACK_SCALE[backF][2], 1.0F);
        this.head.rotateAngleX += HEAD_PITCH[headF];
        this.neck.rotateAngleX += NECK_PITCH[headF];
        float[] e = EAR[earF];""")
    a(f"        this.ear_left.setRotationPoint({f(ear_x)}, e[3], e[4]);")
    a(f"        this.ear_right.setRotationPoint({f(-ear_x)}, e[3], e[4]);")
    a("""        this.setRotateAngle(ear_left, 0.0F, e[0], 0.0F);
        this.setRotateAngle(ear_right, 0.0F, -e[0], 0.0F);
        this.ear_left.setScale(1.0F, e[1], e[2]);
        this.ear_right.setScale(1.0F, e[1], e[2]);
        AdvancedModelBox[] tl = {tusk_left_1, tusk_left_2, tusk_left_3, tusk_left_4, tusk_left_5};
        AdvancedModelBox[] tr = {tusk_right_1, tusk_right_2, tusk_right_3, tusk_right_4, tusk_right_5};
        for (int k = 0; k < 5; k++) {
            float[] l = TUSK_ROT[tuskF][0][k], r = TUSK_ROT[tuskF][1][k];
            this.setRotateAngle(tl[k], l[0], l[1], l[2]);
            this.setRotateAngle(tr[k], r[0], r[1], r[2]);
        }
        boolean longTusk = tuskF != 5;   // SMALL: only the first three segments
        this.tusk_left_4.showModel = longTusk;
        this.tusk_right_4.showModel = longTusk;
        float tuskScale = mammoth.hasLargeTusks() ? 1.35F : 1.0F;
        this.tusk_left_1.setScale(tuskScale, tuskScale, tuskScale);
        this.tusk_right_1.setScale(tuskScale, tuskScale, tuskScale);

        animate(mammoth);
        float globalSpeed = 1.1F;
        float globalDegree = 1.0F;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- idle: the trunk hangs and sways, amplitude growing toward the tip (cut to 30% lying down, where it rests
        // on the ground); the ears fan (a swing about the hinge -- how an elephant actually flaps them); tail swings ----
        float rest = Math.max(mammoth.sitProgress, mammoth.sleepProgress) / 40F;
        float sway = 1.0F - 0.7F * rest;
        this.walk(trunk_1, 0.10F, 0.035F * sway, false, 0.0F, 0F, ageInTicks, 1);
        this.walk(trunk_2, 0.10F, 0.05F * sway, false, 0.6F, 0F, ageInTicks, 1);
        this.walk(trunk_3, 0.10F, 0.07F * sway, false, 1.2F, 0F, ageInTicks, 1);
        this.walk(trunk_4, 0.10F, 0.09F * sway, false, 1.8F, 0F, ageInTicks, 1);
        this.walk(trunk_5, 0.10F, 0.11F * sway, false, 2.4F, 0F, ageInTicks, 1);
        this.walk(trunk_6, 0.10F, 0.14F * sway, false, 3.0F, 0F, ageInTicks, 1);
        this.swing(trunk_2, 0.07F, 0.04F * sway, false, 1.0F, 0F, ageInTicks, 1);
        this.swing(trunk_4, 0.07F, 0.07F * sway, false, 2.0F, 0F, ageInTicks, 1);
        this.swing(trunk_6, 0.07F, 0.10F * sway, false, 3.0F, 0F, ageInTicks, 1);
        float earFan = earF == 2 ? 0.10F : earF == 1 ? 0.08F : 0.05F;
        this.swing(ear_left, 0.09F, earFan, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.09F, earFan, true, 0F, 0F, ageInTicks, 1);
        this.swing(tail, 0.10F, 0.16F, false, 0F, 0F, ageInTicks, 1);
        this.walk(tail_tuft, 0.10F, 0.10F, false, 1F, 0F, ageInTicks, 1);

        // breathing
        float bx = (float) (1.0F + Math.sin(ageInTicks / 20) * 0.04F), by = (float) (1.0F + Math.sin(ageInTicks / 16) * 0.04F);
        this.body_mid.setScale(bx, by, 1.0F);
        this.body_chest.setScale(bx, by, 1.0F);
        this.body_hips.setScale(bx, by, 1.0F);

        // Blinking / asleep: squash the eye planes to 20% height about their centre -- a shut dark slit
        float eyeOpen = mammoth.shouldRenderEyes() ? 1.0F : 0.2F;
        this.eye_left.setScale(1.0F, eyeOpen, 1.0F);
        this.eye_right.setScale(1.0F, eyeOpen, 1.0F);

        if (!mammoth.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 3, head);
        }

        if (mammoth.isInWater()) {
            this.head.rotateAngleX -= 0.18F;   // trunk up as a snorkel
            if (!mammoth.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5F;
                float pitch = Mth.clamp(mammoth.getXRot() - 10, -25F, 25.0F);
                this.body_mid.rotateAngleX += (float) (pitch * Math.PI / 180F);
            }
        }

        // ---- walk: lateral-sequence (LH, LF, RH, RF a quarter-cycle apart), the elephant gait ----
        if (mammoth.canMove()) {
            bob(body_mid, 0.8F * globalSpeed, 0.3F * globalDegree, true, f, limbSwingAmount);
            flap(body_mid, 0.8F * globalSpeed, 0.06F * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(neck, 0.8F * globalSpeed, 0.12F * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head, 0.8F * globalSpeed, 0.08F * globalDegree, true, 0, 0, f, limbSwingAmount);""")
    a("            // (generated by the walk fit in proboscidean_spec.py --walk)")
    for line in WALK_LINES:
        a("            " + line)
    a("""            walk(trunk_1, 0.8F * globalSpeed, 0.18F * globalDegree, false, 0F, 0, f, limbSwingAmount);
            walk(trunk_2, 0.8F * globalSpeed, 0.22F * globalDegree, false, -0.6F, 0, f, limbSwingAmount);
            walk(trunk_3, 0.8F * globalSpeed, 0.26F * globalDegree, false, -1.2F, 0, f, limbSwingAmount);
            walk(trunk_4, 0.8F * globalSpeed, 0.30F * globalDegree, false, -1.8F, 0, f, limbSwingAmount);
            walk(trunk_5, 0.8F * globalSpeed, 0.34F * globalDegree, false, -2.4F, 0, f, limbSwingAmount);
            walk(trunk_6, 0.8F * globalSpeed, 0.38F * globalDegree, false, -3.0F, 0, f, limbSwingAmount);
            swing(tail, 0.8F * globalSpeed, 0.35F * globalDegree, false, 0F, 0, f, limbSwingAmount);
        }
""")
    fur = sol["fur"]
    a("        // ---- rest / sleep: SOLVED sternal recumbency (proboscidean_spec.py --pose). The coat squashes instead of")
    a("        // passing through the ground; written every frame (1.0 standing) because the model is shared.")
    a("        float furRest = mammoth.sitProgress > 0 ? mammoth.sitProgress / 40F : mammoth.sleepProgress / 40F;")
    for n in ("fur_skirt_left", "fur_skirt_right", "fur_chest", "fur_rump", "fur_neck"):
        k = fur.get(n, 1.0)
        a(f"        this.{n}.setScale(1.0F, 1.0F - {f(1.0 - k)} * furRest, 1.0F);")
    a("        if (mammoth.sitProgress > 0) {")
    a("            applyRestingPose(mammoth.sitProgress, SIT_LIFT[headF], false);")
    a("        }")
    a("        else if (mammoth.sleepProgress > 0) {")
    a("            applyRestingPose(mammoth.sleepProgress, SLEEP_LIFT[headF], true);")
    a("        }")
    a("    }\n")
    a("    private void applyRestingPose(float progress, float lift, boolean asleep) {")
    a(f"        // the belly sinks to {S.BELLY_CLEAR}u off the ground; forelegs stretched forward, hind legs straight back")
    a(f"        this.progressPosition(body_mid, progress, 0.0F, {f(root_y - sol['dy'])}, 0.0F, 40);")
    for n, r in sol["legs"].items():
        jx, jy, jz = jr(r)
        dx, dy_, dz = by[n]["rot"]
        a(f"        this.progressRotation({n}, progress, {f(dx + jx)}, {f(dy_ + jy)}, {f(dz + jz)}, 40);")
    tx = by["tail"]["rot"][0] - math.radians(sol["tail"])
    a(f"        this.progressRotation(tail, progress, {f(tx)}, 0.0F, 0.0F, 40);   // lies back along the ground")
    a("        // head lift per head shape keeps the trunk and tusks clear (rest) / resting on the ground (sleep); added on")
    a("        // top of the family pitch, which progressRotation leaves in place (it moves toward target - default)")
    a(f"        this.progressRotation(neck, progress, {f(by['neck']['rot'][0])} - lift * 0.6F, 0.0F, 0.0F, 40);")
    a(f"        this.progressRotation(head, progress, {f(by['head']['rot'][0])} - lift * 0.4F, 0.0F, 0.0F, 40);")
    for pose, cond in (("sit", "if (!asleep)"), ("sleep", "else")):
        a(f"        {cond} {{")
        for k, n in enumerate(S.TRUNK):
            jx = -math.radians(S.TRUNK_CURL[pose][k])
            d = by[n]["rot"]
            a(f"            this.progressRotation({n}, progress, {f(d[0] + jx)}, {f(d[1])}, {f(d[2])}, 40);")
        a("        }")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes)")


WALK_LINES = []

if __name__ == "__main__":
    import json
    wf = pathlib.Path(__file__).parent.parent / "scratch" / "probo_walk.json"
    if wf.exists():
        WALK_LINES = json.loads(wf.read_text())
    else:
        sys.exit("run proboscidean_spec.py --walk first (writes scratch/probo_walk.json)")
    main()
