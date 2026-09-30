#!/usr/bin/env python3
"""SOP phase 6 -- transcribe the Blockbench `modded_entity` export into ModelTapir.java.

Never re-derive the Y-flip: every setRotationPoint / addBox / texOffs literal is copied
VERBATIM out of the export.  The only numbers computed here are the phase-7 pose targets,
grid-searched by tapir_spec.pose_solve() against the real box geometry.  There is no donor
rig to inherit fold angles from -- tapir is a brand-new type -- so they are solved from
scratch, which is what the SOP asks for anyway.

Usage:  python scripts/gen_model_tapir.py <export.java> <ModelTapir.java>
"""
import math
import re
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import tapir_spec as S                                              # noqa: E402

EXPORT = pathlib.Path(sys.argv[1])
OUT = pathlib.Path(sys.argv[2])

PART_RE = re.compile(
    r'PartDefinition (\w+) = (\w+)\.addOrReplaceChild\("(\w+)",\s*'
    r'CubeListBuilder\.create\(\)\.texOffs\((\d+), (\d+)\)'
    r'\.addBox\(([^)]*?), new CubeDeformation\(0\.0F\)\),\s*'
    r'PartPose\.(offset|offsetAndRotation)\(([^)]*)\)\);')

CREST = [v for _, v, _, c, _ in S.SPECIES if c]
CLAWS = [v for _, v, _, _, k in S.SPECIES if k]


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
                # Blockbench local X degrees -> MC local X radians (the verified flip).
                d["rot"][nm] = -math.radians(r[0])
    return out


def main():
    parts = parse()
    assert len(parts) == 34, len(parts)
    by = {p["name"]: p for p in parts}
    names = [p["name"] for p in parts]
    P = poses()
    barrel_y = by["body_barrel"]["off"][1]
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
              "untamedwilds.entity.mammal.EntityTapir"):
        a(f"import {i};")
    a("""
/**
 * Tapir -- 34 boxes on 256x128, authored from scratch. There was no donor rig: before this
 * class the only occurrence of "tapir" in the whole tree was a comment on
 * ModelMacrauchenia.java:165.
 *
 * Sculpted from scripts/tapir_spec.py, which carries the spec, the forward-kinematics
 * checker, the z-fight sweep, the volume-burial check, the UV shelf pack and the phase-7
 * pose solver. Box coords + texOffs are transcribed VERBATIM from the modded_entity export
 * (the Y-down flip is the export's, never re-derived); scripts/gen_model_tapir.py does the
 * transcription and scripts/verify_port.py proves it.
 *
 * Built as the deliberate OPPOSITE of ModelMacrauchenia so the two can never read as one
 * rig retuned: short-legged (belly at 50% of withers against the litoptern's 58%), ONE
 * short neck segment against three long ones, a wedge body that tapers hard to the chest
 * (hw 6.4 against the barrel's 8.5 -- a tapir is shaped to drive through undergrowth
 * head-first), a high rounded rump peaking 2.29u ABOVE the withers, and the head carried
 * LOW on a chain running -12/-18/-26 with the diagnostic prehensile proboscis drooping off
 * the muzzle.
 *
 * SEVEN species share this one mesh. tapir.json declares no synced flag and EntityTapir
 * adds none, so both toggles read getVariant() -- the ModelDeer:509-524 pattern. showModel
 * is applied to the chain ROOT, because ModelPart.render returns before recursing into
 * children when `visible` is false, so hiding a root hides its whole subtree:
 *   * CREST (the erect neck mane) is hidden on malayan and palorchestes.
 *   * CLAWS are shown ONLY on palorchestes. Palorchestes azael is a diprotodontid
 *     marsupial, not a perissodactyl -- it shares the tapir's body plan by convergence and
 *     is grouped here the way thylacine is grouped under dire_wolf. Its huge clawed
 *     forelimbs are the one character a shared mesh would otherwise erase, so they are
 *     modelled rather than left for the skin to fake.
 */""")
    a("public class ModelTapir extends AdvancedEntityModel<EntityTapir> {\n")
    for n in names:
        a(f"    private final AdvancedModelBox {n};")
    a("\n    private final ModelAnimator animator;\n")
    a("    public ModelTapir() {")
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
    a("    @Override")
    a("    public Iterable<AdvancedModelBox> getAllParts() {")
    a("        return ImmutableList.of(")
    a(",\n".join(f"            {n}" for n in names))
    a("        );")
    a("    }\n")

    a("""    private void animate(IAnimatedEntity entityIn) {
        EntityTapir tapir = (EntityTapir) entityIn;
        animator.update(tapir);

        // ATTACK_THREATEN (50t) -- a tapir has no horns, no tusks and no reach, so the
        // display is bulk and noise: the head drops, the forehand swings side to side and
        // the animal rocks its weight between the forelegs. The head goes DOWN rather than
        // up, which is the opposite of the macrauchenia's stacked-neck camelid display.
        animator.setAnimation(EntityTapir.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_chest, 0, 0, 6.0F);
            this.rotate(animator, neck, 10.0F, -14.0F, -6.0F);
            this.rotate(animator, head_skull, 6.0F, -12.0F, 0);
            this.rotate(animator, fore_left_shoulder, -16.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, 13.0F, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_chest, 0, 0, -10.0F);
            this.rotate(animator, neck, 10.0F, 14.0F, 6.0F);
            this.rotate(animator, head_skull, 6.0F, 12.0F, 0);
            this.rotate(animator, fore_left_shoulder, 13.0F, 0, 0);
            this.rotate(animator, fore_right_shoulder, -16.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // ATTACK_GORE (14t) -- misnamed by the shared Animation field. A tapir's actual
        // weapon is its BITE: it has a full set of incisors and a canine on each jaw, and
        // real tapirs bite hard enough to take fingers off. So this rears the head back and
        // drives it forward and down, with the jaw dropping open on the wind-up and
        // snapping shut on the strike.
        animator.setAnimation(EntityTapir.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck, -18.0F, 0, 0);
        this.rotate(animator, head_skull, -22.0F, 0, 0);
        this.rotate(animator, head_jaw, 26.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, 26.0F, 0, 0);
        this.rotate(animator, head_skull, 30.0F, 0, 0);
        this.rotate(animator, head_jaw, -4.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }
""")

    a("    public void setupAnim(EntityTapir tapir, float limbSwing, float limbSwingAmount, "
      "float ageInTicks, float netHeadYaw, float headPitch) {")
    a("""        this.resetToDefaultPose();
        animate(tapir);
        float globalSpeed = 1.5f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);
""")

    # ---- variant gates -------------------------------------------------
    a("        // ---- per-species toggles (getVariant(), the ModelDeer:509-524 pattern) ----")
    a("        // showModel on the chain ROOT: ModelPart.render returns before recursing")
    a("        // into children when `visible` is false, so this hides the whole subtree.")
    a("        int variant = tapir.getVariant();")
    a(f"        // crest ON for {CREST}; malayan and palorchestes carry no erect mane")
    a("        this.crest.showModel = " +
      " || ".join(f"variant == {v}" for v in CREST) + ";")
    a(f"        // claws ON for {CLAWS} (palorchestes) ALONE")
    a("        boolean clawed = " + " || ".join(f"variant == {v}" for v in CLAWS) + ";")
    a("        this.claw_left.showModel = clawed;")
    a("        this.claw_right.showModel = clawed;\n")

    a("""        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.05F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.body_chest.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        bob(body_barrel, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        walk(neck, 0.32f * globalSpeed, 0.04f, false, 2.8F, 0.03F, ageInTicks / 20, 2);
        // The proboscis is soft tissue and constantly in motion -- a tapir uses it the way
        // an elephant uses a trunk. It swings faster and wider than anything bony.
        this.swing(proboscis_1, 0.26F, 0.20F, false, 0F, 0F, ageInTicks, 1);
        this.swing(proboscis_2, 0.26F, 0.32F, false, 1F, 0F, ageInTicks, 1);
        this.swing(ear_left, 0.18F, 0.24F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.18F, 0.24F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail, 0.12F, 0.16F, false, 0F, 0F, ageInTicks, 1);
""")
    a("        // Blinking: bury the eye planes inside the skull. y/z MUST keep the")
    a("        // constructor's values or the eye jumps across the face on every blink.")
    a("        if (!tapir.shouldRenderEyes()) {")
    a(f"            this.eye_left.setRotationPoint({f(eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a(f"            this.eye_right.setRotationPoint({f(-eye[0] * 0.55)}, {f(eye[1])}, {f(eye[2])});")
    a("        }\n")
    a("""        // Head tracking. The neck is one short heavily-muscled segment, so it
        // contributes little and most of the turn happens at the skull -- the opposite of
        // the macrauchenia, which shares its yaw down a long four-link chain.
        if (!tapir.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 4, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head_skull);
        }

        // Tapirs are strong swimmers and dive to feed, so this branch matters more here
        // than on most of the herd herbivores.
        if (tapir.isInWater()) {
            this.setRotateAngle(head_skull, -0.1820F, 0.0F, 0.0F);
            if (!tapir.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(tapir.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_barrel, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // ---- gait: short-legged and short-strided, so the amplitudes stay LOW ----
        if (tapir.canMove()) {
            bob(body_barrel, 0.8f * globalSpeed, 0.6f * globalDegree, true, f, limbSwingAmount);
            walk(neck, 0.8f * globalSpeed, 0.14f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head_skull, 0.8f * globalSpeed, 0.10f * globalDegree, true, 0, 0, f, limbSwingAmount);
            walk(fore_right_shoulder, -0.8f * globalSpeed, 1.0f * globalDegree, true, 0F, 1.0f, f, limbSwingAmount);
            walk(fore_right_forearm, -0.8f * globalSpeed, 1.0f * globalDegree, false, -1F, 1.0f, f, limbSwingAmount * 1.2f);
            walk(fore_right_cannon, -0.8f * globalSpeed, 0.7f * globalDegree, false, -2F, 0.7f, f, limbSwingAmount);
            walk(fore_left_shoulder, -0.8f * globalSpeed, 1.0f * globalDegree, true, 2F, 1.0f, f, limbSwingAmount);
            walk(fore_left_forearm, -0.8f * globalSpeed, 1.0f * globalDegree, false, 1F, 1.0f, f, limbSwingAmount * 1.2f);
            walk(fore_left_cannon, -0.8f * globalSpeed, 0.7f * globalDegree, false, 0F, 0.7f, f, limbSwingAmount);
            walk(hind_right_thigh, 0.8f * globalSpeed, 1.0f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(hind_right_gaskin, 0.8f * globalSpeed, 1.0f * globalDegree, true, 1.8F, 0, f, limbSwingAmount);
            walk(hind_right_cannon, 0.8f * globalSpeed, 0.7f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(hind_left_thigh, 0.8f * globalSpeed, 1.0f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(hind_left_gaskin, 0.8f * globalSpeed, 1.0f * globalDegree, true, -0.2F, 0, f, limbSwingAmount);
            walk(hind_left_cannon, 0.8f * globalSpeed, 0.7f * globalDegree, false, -1.2F, 0, f, limbSwingAmount);
        }
""")

    for pose, key in (("sit", "sitProgress"), ("sleep", "sleepProgress")):
        d = P[pose]
        head = ("        if (tapir.sitProgress > 0) {" if pose == "sit"
                else "        else if (tapir.sleepProgress > 0) {")
        a(head)
        a("            // SOLVED by grid search against real box geometry"
          " (tapir_spec.py --pose).")
        a("            // Nothing to inherit here -- tapir is a new type with no donor rig --")
        a("            // which is exactly how the SOP wants poses derived anyway.")
        a(f"            // Belly drops {abs(d['dy']):.2f}u; every leg box stays above y=0 and the")
        a("            // cannon + foot of all four limbs rest ON the floor.")
        a(f"            this.progressPosition(body_barrel, tapir.{key}, 0.0F, "
          f"{f(barrel_y - d['dy'])}, 0.0F, 40);")
        for n in names:
            if n in d["rot"]:
                a(f"            this.progressRotation({n}, tapir.{key}, "
                  f"{f(d['rot'][n])}, 0, 0, 40);")
        if pose == "sit":
            a(f"            this.progressRotation(neck, tapir.{key}, -0.1745F, 0, 0, 40);")
            a(f"            this.progressRotation(head_skull, tapir.{key}, -0.1047F, 0, 0, 40);")
        else:
            # Asleep: a tapir lies sternally with the head down and the trunk slack.
            a(f"            this.progressRotation(neck, tapir.{key}, 0.3491F, 0, 0, 40);")
            a(f"            this.progressRotation(head_skull, tapir.{key}, 0.4363F, 0, 0, 40);")
            a(f"            this.progressRotation(proboscis_1, tapir.{key}, 0.2618F, 0, 0, 40);")
        a("        }\n")
    a("    }")
    a("}")
    OUT.write_text("\n".join(L) + "\n")
    print(f"wrote {OUT}  ({len(L)} lines, {len(parts)} boxes, "
          f"crest={CREST} claws={CLAWS})")


if __name__ == "__main__":
    main()
