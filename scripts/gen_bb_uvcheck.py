#!/usr/bin/env python3
"""Emit the SOP phase-5 flat-colour UV checker for a <animal>_spec.py.

Fills each part's TRUE footprint rect -- 2*(w+d) x (h+d) ceil'd, the region MC reserves
from texOffs whatever the author intended -- with one distinct hue, then the rig is
screenshotted.  Every box must read as ONE clean colour.  A box showing two colours is
sampling a neighbour's rect; a box showing BLACK/transparent is sampling unpainted
canvas.  Both are packing bugs and both are far cheaper to catch here than after the
Java port.

Hues are assigned so that parts ADJACENT in the UV atlas get widely separated colours
(golden-angle stepping by pack order), which is what makes a one-texel bleed visible
instead of blending into its neighbour.

Usage:  python scripts/gen_bb_uvcheck.py <spec_module> <out.js>
"""
import colorsys
import importlib
import io
import json
import pathlib
import sys
from contextlib import redirect_stdout

MOD = sys.argv[1]
OUT = pathlib.Path(sys.argv[2])

sys.path.insert(0, str(pathlib.Path(__file__).parent))
S = importlib.import_module(MOD)

buf = io.StringIO()
with redirect_stdout(buf):
    S.emit_json()
spec = json.loads(buf.getvalue())

# footprint sizes come from the spec's own footprint(), so the checker paints exactly
# the region the packer reserved -- not a re-derivation that could disagree with it
foot = {p[0]: S.footprint(p) for p in S.PARTS}

rects = []
for i, p in enumerate(spec["parts"]):
    fw, fh = foot[p["n"]]
    h = (i * 0.381966) % 1.0                       # golden angle -> max separation
    r, g, b = colorsys.hsv_to_rgb(h, 0.72, 0.98 if i % 2 else 0.72)
    rects.append({"n": p["n"], "u": p["uv"][0], "v": p["uv"][1], "w": fw, "h": fh,
                  "c": "#%02x%02x%02x" % (int(r * 255), int(g * 255), int(b * 255))})

# risky_eval bans "console.", "//" and "/* */", so the payload carries no comments.
#
# THE OLD TEXTURE MUST BE REMOVED, NOT JUST SUPERSEDED.  Adding a new Texture and
# reassigning every `cube.faces[f].texture` uuid -- the documented reload recipe -- left
# the viewport still rendering the flat grey from the build pass, so the first checker
# run came back looking like a clean grey model and proved nothing at all.  Dropping
# every pre-existing Texture first and then using applyTexture (which is what the build
# script does, and which does stick) is what actually rebinds the material.
JS = """
(function () {
  const R = %s;
  const W = %d, H = %d;
  const cv = document.createElement('canvas');
  cv.width = W; cv.height = H;
  const cx = cv.getContext('2d');
  cx.clearRect(0, 0, W, H);
  for (const r of R) { cx.fillStyle = r.c; cx.fillRect(r.u, r.v, r.w, r.h); }

  Texture.all.slice().forEach(t => t.remove(false));
  const tex = new Texture({ name: 'uvcheck' }).fromDataURL(cv.toDataURL()).add();
  Cube.all.forEach(c => c.applyTexture(tex, true));
  Canvas.updateAll();
  Canvas.updateAllFaces();

  const px = cx.getImageData(0, 0, W, H).data;
  let painted = 0;
  for (let i = 3; i < px.length; i += 4) if (px[i] > 0) painted++;
  return JSON.stringify({ rects: R.length, paintedTexels: painted,
                          textures: Texture.all.length, canvas: [W, H] });
})()
""" % (json.dumps(rects, separators=(",", ":")), spec["canvas"][0], spec["canvas"][1])

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(JS, encoding="utf-8")
print(f"wrote {OUT}  ({len(rects)} rects)")
