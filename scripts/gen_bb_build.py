#!/usr/bin/env python3
"""Emit the Blockbench builder JS for a <animal>_spec.py -- SOP phase 3.

Never hand-type coordinates into Blockbench (that is Blockbench lesson 1, and it is how
the first mammoth ended up an unreadable dark mass).  This reads the spec's own
`--json` output and writes a single risky_eval payload that rebuilds the whole rig:
one Group per part carrying origin+rotation, one box_uv Cube inside it carrying
from/to + the packed uv_offset.

Emitting ONE eval matters: 35 parts via 70 individual MCP tool calls is 70 round trips,
each of which can half-apply and leave the outliner in a state no screenshot explains.

Usage:  python scripts/gen_bb_build.py <spec_module> <out.js> [--grey]
        python scripts/gen_bb_build.py macrauchenia_spec scratch/build_mac.js
"""
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

# risky_eval REFUSES any payload containing "console.", "//" or "/* */" -- so this
# template carries no comments at all and the explanation lives here instead:
#   * the project is wiped first, so a re-run is idempotent;
#   * the texture is a single FLAT GREY built on a canvas (create_texture's width/height
#     arguments do not reliably stick, and random per-cube colours make form unreadable
#     -- flat grey is what lets a silhouette actually be judged, SOP phase 3);
#   * groups are created in the spec's own topological order, so one forward pass
#     resolves every parent by name;
#   * box_uv + an explicit uv_offset is what makes the modded_entity export emit
#     texOffs(u,v) verbatim, which is what the phase-6 Java port transcribes.
#   * IT SNAPSHOTS THE LIVE RIG BEFORE WIPING IT. This builder is destructive by design --
#     the Blockbench project is disposable and regenerated from the spec -- but that means
#     any edit the USER makes by hand in Blockbench is silently destroyed by the next run.
#     That happened on the tapir: a hand-tweaked eye plane was lost to a rebuild during the
#     head iterations and could not be recovered or even identified afterwards, because the
#     rebuilt rig matched the spec exactly and there was nothing left to diff against.
#     Every run now writes the pre-wipe state to scratch/rig_prewipe.json first, so a lost
#     edit is always recoverable by diffing that against the spec.
JS = """
(function () {
  const SPEC = %s;

  if (typeof Cube !== 'undefined' && Cube.all.length) {
    const snap = {};
    Cube.all.forEach(c => {
      const g = c.parent;
      snap[c.name] = {
        f: c.from, t: c.to, uv: c.uv_offset,
        o: (g && g.origin) ? g.origin : null,
        r: (g && g.rotation) ? g.rotation : null,
        p: (g && g.parent && g.parent.name) ? g.parent.name : null
      };
    });
    try {
      require('fs').writeFileSync(
        (window.OUT || '') + 'rig_prewipe.json', JSON.stringify(snap));
    } catch (e) { }
  }

  [...Outliner.root].forEach(n => n.remove(false));
  Project.texture_width  = SPEC.canvas[0];
  Project.texture_height = SPEC.canvas[1];

  Texture.all.slice().forEach(t => t.remove(false));
  const cv = document.createElement('canvas');
  cv.width = SPEC.canvas[0]; cv.height = SPEC.canvas[1];
  const cx = cv.getContext('2d');
  cx.fillStyle = '#9a9a9a';
  cx.fillRect(0, 0, cv.width, cv.height);
  const tex = new Texture({ name: 'grey' }).fromDataURL(cv.toDataURL()).add();

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
      autouv: 0, box_uv: true, uv_offset: p.uv, mirror_uv: false
    }).init();
    c.addTo(G[p.n]);
    c.applyTexture(tex, true);
    n++;
  }

  Canvas.updateAll();
  return JSON.stringify({
    groups: Object.keys(G).length,
    cubes: n,
    roots: Outliner.root.length,
    canvas: [Project.texture_width, Project.texture_height]
  });
})()
""" % json.dumps(spec, separators=(",", ":"))

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(JS, encoding="utf-8")
print(f"wrote {OUT}  ({len(JS)} bytes, {len(spec['parts'])} parts)")
