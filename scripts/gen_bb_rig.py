#!/usr/bin/env python3
"""Rebuild an ALREADY-SHIPPED Model*.java as a live Blockbench rig, for skin work.

This is the counterpart of gen_bb_build.py: that one builds a NEW animal from its
<animal>_spec.py, this one reconstructs an existing Java model so a skin can be painted
and previewed in 3D (CLAUDE.md Blockbench lesson 9). Geometry comes from java2bb.py,
which is calibrated, not guessed -- never hand-type coordinates.

It optionally loads the species' shipped PNG instead of flat grey, which is how the
reference conventions (eye halves, alpha-0'd parts, mouth colour) get read off the real
art rather than invented.

    python scripts/gen_bb_rig.py ModelBison scratch/rig_bison.js
    python scripts/gen_bb_rig.py ModelBison scratch/rig_bison.js --skin bison/plains.png
    python scripts/bb.py eval scratch/rig_bison.js

risky_eval refuses any payload containing "console.", "//" or "/* */", so the emitted JS
carries no comments at all -- the explanation stays here.
"""
import base64
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import java2bb  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/untamedwilds/textures/entity"

model = sys.argv[1]
out = pathlib.Path(sys.argv[2])
skin = None
if "--skin" in sys.argv:
    arg = sys.argv[sys.argv.index("--skin") + 1]
    skin = pathlib.Path(arg) if pathlib.Path(arg).exists() else TEX / arg

spec = {"canvas": java2bb.canvas(model), "parts": java2bb.convert(model)}

# Blockbench never runs setupAnim, so a species' showModel toggles do not exist in the
# preview (CLAUDE.md lesson 5). Bake them in by dropping the parts the species hides --
# otherwise a lessHair bovid is previewed wearing the bison's shoulder wool. Children of
# a hidden part go with it, exactly as ModelPart.render skips a hidden subtree.
# Bake a Java `setScale(s,s,s)` + `setShouldScaleChildren(true)` into the geometry, about the
# scaled part's own rotation point -- that is what ModelDeer does to size a whole antler rack
# from one call on the shared pedicle. Applying it to the live THREE mesh instead does not
# survive Canvas.updateAll(), which rebuilds meshes from the model.
#   --scale pedicle_left:0.372,pedicle_right:0.372
if "--scale" in sys.argv:
    for item in sys.argv[sys.argv.index("--scale") + 1].split(","):
        root, s = item.split(":")
        s = float(s)
        byname = {p["n"]: p for p in spec["parts"]}
        if root not in byname:
            raise SystemExit("--scale: no such part " + root)
        ox, oy, oz = byname[root]["o"]

        def sub(n):
            out = [n]
            for p in spec["parts"]:
                if p["p"] == n:
                    out += sub(p["n"])
            return out

        for n in sub(root):
            p = byname[n]
            for key in ("o", "f", "t"):
                x, y, z = p[key]
                p[key] = [ox + (x - ox) * s, oy + (y - oy) * s, oz + (z - oz) * s]
    print("scaled: %s" % sys.argv[sys.argv.index("--scale") + 1])

# Bake a Java `rotationPointY -= n` body lift into the preview. Blockbench is Y-UP and MC
# is Y-DOWN, so a Java lift of n becomes +n here. Pairs with --scale: stretching the limbs
# alone previews the animal buried in the floor, which is not what the game will draw.
#   --lift body_main:5.93
# Preview ModelDireWolf's per-segment limb stretch: each box scaled thin on x/z and long
# on y in its OWN frame, with the child pivots rescaled so the chain stays closed. The
# rig JSON is in absolute coordinates, so this applies the factors about each part's own
# origin and then re-stacks the chain -- an approximation only in that it ignores each
# segment's small X rotation (<=12 deg), which is fine for judging proportion.
#   --limb arm_left_upper,arm_left_lower,arm_left_foot,arm_left_paw:1.35:0.64
if "--limb" in sys.argv:
    for item in sys.argv[sys.argv.index("--limb") + 1].split(";"):
        names, longer, thinner = item.split(":")
        longer, thinner = float(longer), float(thinner)
        chain = names.split(",")
        byname = {p["n"]: p for p in spec["parts"]}
        base = byname[chain[0]]["o"][:]
        shift = [0.0, 0.0, 0.0]
        for idx, n in enumerate(chain):
            p = byname[n]
            ox, oy, oz = p["o"]
            if idx > 0:
                # the pivot offset from the parent grows with the parent's factors
                par = byname[chain[idx - 1]]["o"]
                shift[0] += (ox - par[0]) * (thinner - 1)
                shift[1] += (oy - par[1]) * (longer - 1)
                shift[2] += (oz - par[2]) * (thinner - 1)
            no = [ox + shift[0], oy + shift[1], oz + shift[2]]
            for key in ("f", "t"):
                x, y, z = p[key]
                p[key] = [no[0] + (x - ox) * thinner,
                          no[1] + (y - oy) * longer,
                          no[2] + (z - oz) * thinner]
            p["o"] = no
    print("limb-stretched: %s" % sys.argv[sys.argv.index("--limb") + 1])

if "--lift" in sys.argv:
    for item in sys.argv[sys.argv.index("--lift") + 1].split(","):
        root, dy = item.split(":")
        dy = float(dy)
        byname = {p["n"]: p for p in spec["parts"]}

        def sub2(n):
            out = [n]
            for p in spec["parts"]:
                if p["p"] == n:
                    out += sub2(p["n"])
            return out

        for n in sub2(root):
            p = byname[n]
            for key in ("o", "f", "t"):
                p[key][1] += dy
    print("lifted: %s" % sys.argv[sys.argv.index("--lift") + 1])

if "--hide" in sys.argv:
    hide = set(sys.argv[sys.argv.index("--hide") + 1].split(","))
    grew = True
    while grew:
        grew = False
        for p in spec["parts"]:
            if p["p"] in hide and p["n"] not in hide:
                hide.add(p["n"])
                grew = True
    spec["parts"] = [p for p in spec["parts"] if p["n"] not in hide]
    print("hidden: %s" % ",".join(sorted(hide)))

# The texture is replaced by REMOVING every existing Texture first: adding a new one and
# reassigning cube.faces[f].texture leaves the viewport rendering the old image, which
# once made a UV checker run come back looking clean and prove nothing.
# risky_eval rejects any payload containing "//", and a base64 blob hits that pair
# constantly, so '/' is escaped to '_' here and restored in the JS.
src = base64.b64encode(skin.read_bytes()).decode().replace("/", "_") if skin else None

js = """(function () {
  const SPEC = %s;
  const SKIN = %s;
  [...Outliner.root].forEach(n => n.remove(false));
  Project.texture_width  = SPEC.canvas[0];
  Project.texture_height = SPEC.canvas[1];
  Texture.all.slice().forEach(t => t.remove(false));

  let tex;
  if (SKIN) {
    const url = 'data:image' + String.fromCharCode(47) + 'png;base64,'
      + SKIN.split('_').join(String.fromCharCode(47));
    tex = new Texture({ name: 'skin' }).fromDataURL(url).add();
  } else {
    const cv = document.createElement('canvas');
    cv.width = SPEC.canvas[0]; cv.height = SPEC.canvas[1];
    const cx = cv.getContext('2d');
    cx.fillStyle = '#9a9a9a';
    cx.fillRect(0, 0, cv.width, cv.height);
    tex = new Texture({ name: 'grey' }).fromDataURL(cv.toDataURL()).add();
  }

  const G = {};
  for (const p of SPEC.parts) {
    const g = new Group({ name: p.n, origin: p.o, rotation: p.r }).init();
    if (p.p) g.addTo(G[p.p]); else g.addTo();
    G[p.n] = g;
  }
  let n = 0;
  for (const p of SPEC.parts) {
    const c = new Cube({
      name: p.n, from: p.f, to: p.t,
      autouv: 0, box_uv: true, uv_offset: p.uv, mirror_uv: !!p.m
    }).init();
    c.addTo(G[p.n]);
    c.applyTexture(tex, true);
    n++;
  }
  Canvas.updateAll();
  return JSON.stringify({
    groups: Object.keys(G).length, cubes: n,
    mirrored: SPEC.parts.filter(p => p.m).length,
    canvas: [Project.texture_width, Project.texture_height]
  });
})()""" % (json.dumps(spec, separators=(",", ":")),
           json.dumps(src) if src else "null")

out.write_text(js, encoding="utf-8")
print("%s -> %s  (%d parts, canvas %s, skin %s)"
      % (model, out, len(spec["parts"]), spec["canvas"], skin.name if skin else "grey"))
