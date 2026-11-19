#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench `modded_entity` export into ModelKangaroo.java (Phase 4.2, 2026-10-10).

Every setRotationPoint / addBox / texOffs literal is copied VERBATIM from the export. The only computed numbers are
the phase-7 pose targets -- sit, sleep and the pentapedal CRAWL -- grid-searched by kangaroo_spec.pose_solve()
against the real box geometry, and the sthenurine proportion constants that match the approved preview
(muzzle 0.55 long, arms 1.35 long).

Usage:  python scripts/gen_model_kangaroo.py <export.java> <ModelKangaroo.java>
"""
import math
import re
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import kangaroo_spec as S                                           # noqa: E402

EXPORT = pathlib.Path(sys.argv[1])
OUT = pathlib.Path(sys.argv[2])

PART_RE = re.compile(
    r'PartDefinition (\w+) = (\w+)\.addOrReplaceChild\("(\w+)",\s*'
    r'CubeListBuilder\.create\(\)\.texOffs\((\d+), (\d+)\)'
    r'\.addBox\(([^)]*?), new CubeDeformation\(0\.0F\)\),\s*'
    r'PartPose\.(offset|offsetAndRotation)\(([^)]*)\)\);')

STHEN = [v for _, v, _, st in S.SPECIES if st]
MUZZLE_LEN, ARM_LEN = 0.55, 1.35
MUZZLE_L = 3.4                     # the muzzle segment's length in the spec


def nums(s):
    return [float(x.strip().rstrip("Ff")) for x in s.split(",") if x.strip()]


def parse():
    src = EXPORT.read_text()
    out = []
    for m in PART_RE.finditer(src):
        box = nums(m.group(6))
        pose = nums(m.group(8))
        out.append(dict(name=m.group(3), parent=None if m.group(2) == "partdefinition" else m.group(2),
                        u=int(m.group(4)), v=int(m.group(5)), box=box[:3], size=box[3:6],
                        off=pose[:3], rot=pose[3:6] if len(pose) > 3 else [0.0, 0.0, 0.0]))
    return out


def f(x):
    return f"{x:.4f}F".replace(".0000F", ".0F")


def main():
    parts = parse()
    assert len(parts) == len(S.PARTS) == 31, (len(parts), len(S.PARTS))
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    sol = S.pose_solve(verbose=True)
    root_y = by["body_hips"]["off"][1]
    eye = by["eye_left"]["off"]

    def targets(pose):
        """part -> Java local rotateAngleX target (radians), plus the root's Java rotationPointY."""
        d = sol[pose]
        rot = {n: -math.radians(r[0]) for n, r in d["over"].items()}
        return rot, root_y - d["dy"]

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
              "untamedwilds.entity.mammal.EntityKangaroo"):
        a(f"import {i};")
    a("""
/**
 * Kangaroo -- the mod's first HOPPING body (Phase 4.2, 2026-10-10): 31 boxes on 256x128, authored from scratch in
 * scripts/kangaroo_spec.py (FK, ground/tail solves, z-fight, burial, UV pack, phase-7 pose solver), ported VERBATIM
 * from the modded_entity export by scripts/gen_model_kangaroo.py and proved by scripts/verify_port.py.
 *
 * REST pose = standing alert on the tripod: torso tipped up (hips 35, chest 50, neck 62 deg), the long feet flat,
 * the tail resting on the ground. Movement is MODEL-ONLY (user decision): the entity walks the ground like every
 * other mob and the model does the rest --
 *   * HOP (getCurrentSpeed() above SLOW_SPEED): the torso tips forward toward level, both hind legs swing together
 *     (push-off back, landing forward), the body rises over the airborne half of the cycle and the tail counter-swings;
 *   * CRAWL (slow, the pentapedal walk): blends toward a SOLVED crawl pose -- torso near level, forearms planted, tail
 *     on the ground -- while both hind legs swing forward together.
 * Gait phase is divided by the RENDER size so a 2 m Procoptodon and a 1.4 m grey cover the ground at the same rate.
 *
 * Per species (getVariant(), the ModelDeer:509-524 pattern): the sthenurines (Procoptodon, Sthenurus) get a SHORT FACE
 * (muzzle 0.55 long, nose pulled back with it) and LONG ARMS (each arm box 1.35 long with its child pivots re-stacked,
 * the ModelDireWolf stretchLimb method) plus claws. Citadel's resetToDefaultPose restores rotations and rotation points
 * but NOT scale, so every setScale is written on both branches every frame. The pouch joey follows the synced
 * EntityKangaroo.hasJoey() and ducks away whenever the mother sits or sleeps.
 */""")
    a("public class ModelKangaroo extends AdvancedEntityModel<EntityKangaroo> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;")
    a("    /** Blocks per tick: below this the animal crawls on five limbs, above it hops. */")
    a("    private static final float SLOW_SPEED = 0.08F;\n")
    a("    public ModelKangaroo() {")
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
    a("        return ImmutableList.of(body_hips);")
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

    a("""    /** Move a part's X angle a fraction t of the way from its rest angle to a solved target. */
    private static void blendX(AdvancedModelBox box, float target, float t) {
        box.rotateAngleX += (target - box.defaultRotationX) * t;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityKangaroo roo = (EntityKangaroo) entityIn;
        animator.update(roo);

        // ATTACK_KICK (18t) -- the kangaroo's real weapon: rock back onto the tail and strike forward with BOTH hind
        // feet at once. EntityKangaroo throws the target on the follow-through.
        animator.setAnimation(EntityKangaroo.ATTACK_KICK);
        animator.startKeyframe(7);
        this.rotate(animator, body_hips, -22.0F, 0, 0);
        this.rotate(animator, leg_left_thigh, -40.0F, 0, 0);
        this.rotate(animator, leg_right_thigh, -40.0F, 0, 0);
        this.rotate(animator, leg_left_shin, 35.0F, 0, 0);
        this.rotate(animator, leg_right_shin, 35.0F, 0, 0);
        this.rotate(animator, tail_1, 18.0F, 0, 0);
        this.rotate(animator, neck, 12.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, body_hips, -28.0F, 0, 0);
        this.rotate(animator, leg_left_thigh, -78.0F, 0, 0);
        this.rotate(animator, leg_right_thigh, -78.0F, 0, 0);
        this.rotate(animator, leg_left_shin, -5.0F, 0, 0);
        this.rotate(animator, leg_right_shin, -5.0F, 0, 0);
        this.rotate(animator, leg_left_foot, -25.0F, 0, 0);
        this.rotate(animator, leg_right_foot, -25.0F, 0, 0);
        this.rotate(animator, tail_1, 22.0F, 0, 0);
        this.rotate(animator, neck, 16.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(7);

        // ATTACK_BOX (44t) -- cosmetic sparring between males: stand tall on the tail and trade jabs, head back.
        animator.setAnimation(EntityKangaroo.ATTACK_BOX);
        animator.startKeyframe(6);
        this.rotate(animator, body_hips, -20.0F, 0, 0);
        this.rotate(animator, neck, 10.0F, 0, 0);
        this.rotate(animator, arm_left_upper, -55.0F, 0, 0);
        this.rotate(animator, arm_right_upper, -20.0F, 0, 0);
        animator.endKeyframe();
        for (int i = 0; i < 3; i++) {
            animator.startKeyframe(5);
            this.rotate(animator, body_hips, -20.0F, 0, 0);
            this.rotate(animator, neck, 10.0F, -8.0F, 0);
            this.rotate(animator, arm_left_upper, -20.0F, 0, 0);
            this.rotate(animator, arm_left_fore, 10.0F, 0, 0);
            this.rotate(animator, arm_right_upper, -70.0F, 0, 0);
            this.rotate(animator, arm_right_fore, -35.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(5);
            this.rotate(animator, body_hips, -20.0F, 0, 0);
            this.rotate(animator, neck, 10.0F, 8.0F, 0);
            this.rotate(animator, arm_left_upper, -70.0F, 0, 0);
            this.rotate(animator, arm_left_fore, -35.0F, 0, 0);
            this.rotate(animator, arm_right_upper, -20.0F, 0, 0);
            this.rotate(animator, arm_right_fore, 10.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_THREATEN (30t) -- stand up to full height on the toes and tail, arms raised, head high.
        animator.setAnimation(EntityKangaroo.ATTACK_THREATEN);
        animator.startKeyframe(8);
        this.rotate(animator, body_hips, -18.0F, 0, 0);
        this.rotate(animator, neck, 8.0F, 0, 0);
        this.rotate(animator, arm_left_upper, -45.0F, 0, 0);
        this.rotate(animator, arm_right_upper, -45.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(14);
        animator.resetKeyframe(8);
    }
""")

    a("    public void setupAnim(EntityKangaroo roo, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(roo);
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);
        float blend = Math.min(1.0F, limbSwingAmount / 0.3F);
        float gait = limbSwing * 0.55F / Math.max(0.3F, roo.getMobSize() * roo.getScale());
""")
    sn = " || ".join(f"variant == {v}" for v in STHEN)
    a("        // ---- per-species: sthenurine short face + long clawed arms ----")
    a("        int variant = roo.getVariant();")
    a(f"        boolean sthen = {sn};")
    a(f"        this.muzzle.setScale(1.0F, 1.0F, sthen ? {f(MUZZLE_LEN)} : 1.0F);")
    a("        this.claw_left.showModel = sthen;")
    a("        this.claw_right.showModel = sthen;")
    a("        for (AdvancedModelBox arm : new AdvancedModelBox[]{arm_left_upper, arm_left_fore, arm_left_hand, "
      "arm_right_upper, arm_right_fore, arm_right_hand}) {")
    a("            arm.setShouldScaleChildren(false);")
    a(f"            arm.setScale(1.0F, sthen ? {f(ARM_LEN)} : 1.0F, 1.0F);")
    a("        }")
    a("        if (sthen) {")
    a(f"            this.nose.rotationPointZ += {f((1 - MUZZLE_LEN) * MUZZLE_L)};   // follow the shortened muzzle back")
    a("            // re-close each arm chain: a child's pivot is an offset in its PARENT's frame, so it takes the parent's stretch")
    a("            for (AdvancedModelBox child : new AdvancedModelBox[]{arm_left_fore, arm_left_hand, claw_left, "
      "arm_right_fore, arm_right_hand, claw_right}) {")
    a(f"                child.rotationPointY *= {f(ARM_LEN)};")
    a("            }")
    a("        }")
    a("        this.joey_head.showModel = roo.hasJoey() && roo.sitProgress == 0 && roo.sleepProgress == 0;\n")

    a("""        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 18) * 0.035F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        walk(neck, 0.25f, 0.03f, false, 2.8F, 0.02F, ageInTicks / 20, 2);
        this.swing(ear_left, 0.22F, 0.20F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.22F, 0.20F, false, 1.3F, 0F, ageInTicks, 1);
        this.swing(tail_3, 0.08F, 0.10F, false, 0F, 0F, ageInTicks, 1);
        this.swing(joey_head, 0.10F, 0.25F, false, 0F, 0F, ageInTicks, 1);
""")
    a("        // Blinking: bury the eye planes inside the skull; y/z keep the constructor's values.")
    a("        if (!roo.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        if (!roo.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        if (roo.isInWater() && !roo.isOnGround()) {
            // kangaroos swim with the body level, kicking the hind legs alternately
            this.body_hips.rotateAngleX += 0.55F;
            this.leg_left_thigh.rotateAngleX -= 0.55F;
            this.leg_right_thigh.rotateAngleX -= 0.55F;
            walk(leg_left_thigh, 0.35F, 0.5F, false, 0F, 0F, ageInTicks, 1);
            walk(leg_right_thigh, 0.35F, 0.5F, true, 0F, 0F, ageInTicks, 1);
        }
        else if (roo.canMove() && limbSwingAmount > 0.02F) {
            if (roo.getCurrentSpeed() < SLOW_SPEED) {""")
    crot, croot = targets("crawl")
    a("                // ---- CRAWL (pentapedal walk): blend toward the solved crawl pose, then swing both hind legs")
    a("                // forward together while the forearms and tail carry the weight ----")
    a(f"                this.body_hips.rotationPointY += ({f(croot)} - this.body_hips.defaultPositionY) * blend;")
    for n in names:
        if n in crot and not n.startswith("claw"):
            a(f"                blendX({n}, {f(crot[n])}, blend);")
    a("""                float swingLegs = Mth.sin(gait * 0.9F) * 0.45F * blend;
                this.leg_left_thigh.rotateAngleX += swingLegs;
                this.leg_right_thigh.rotateAngleX += swingLegs;
                this.leg_left_foot.rotateAngleX -= swingLegs * 0.6F;
                this.leg_right_foot.rotateAngleX -= swingLegs * 0.6F;
                this.body_hips.rotationPointY -= Math.max(0, Mth.sin(gait * 0.9F)) * 1.2F * blend;
            }
            else {
                // ---- HOP: torso tips forward, legs counter-rotated to stay under it, then both legs swing together:
                // push-off BACK at phase 0, airborne over the first half, landing FORWARD at pi ----
                float ph = gait * 1.1F;
                float pitch = 0.45F * blend;
                this.body_hips.rotateAngleX += pitch;
                this.neck.rotateAngleX -= pitch * 0.6F;
                this.leg_left_thigh.rotateAngleX -= pitch;
                this.leg_right_thigh.rotateAngleX -= pitch;
                float sw = Mth.cos(ph) * blend;
                this.leg_left_thigh.rotateAngleX += sw * 0.65F;
                this.leg_right_thigh.rotateAngleX += sw * 0.65F;
                this.leg_left_shin.rotateAngleX -= sw * 0.45F;
                this.leg_right_shin.rotateAngleX -= sw * 0.45F;
                this.leg_left_foot.rotateAngleX += sw * 0.55F;
                this.leg_right_foot.rotateAngleX += sw * 0.55F;
                this.leg_left_toe.rotateAngleX += sw * 0.30F;
                this.leg_right_toe.rotateAngleX += sw * 0.30F;
                this.tail_1.rotateAngleX += sw * 0.20F;
                this.tail_2.rotateAngleX += sw * 0.15F;
                this.arm_left_upper.rotateAngleX -= 0.25F * blend;
                this.arm_right_upper.rotateAngleX -= 0.25F * blend;
                this.body_hips.rotationPointY -= Math.max(0, Mth.sin(ph)) * 3.0F * blend;
            }
        }
""")
    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        rot, ry = targets(pose)
        head = "        if (roo.sitProgress > 0) {" if pose == "sit" else "        else if (roo.sleepProgress > 0) {"
        a(head)
        a(f"            // SOLVED (kangaroo_spec.py --pose): every box above y=0, feet and tail resting ON the floor"
          + (", paws on it" if pose == "sleep" else ""))
        a(f"            this.progressPosition(body_hips, roo.{key}, 0.0F, {f(ry)}, 0.0F, 40);")
        for n in names:
            if n in rot:
                a(f"            this.progressRotation({n}, roo.{key}, {f(rot[n])}, 0, 0, 40);")
        a("        }")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes, sthenurines={STHEN})")


if __name__ == "__main__":
    main()
