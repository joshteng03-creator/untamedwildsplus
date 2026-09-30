#!/usr/bin/env python3
"""Diff the LIVE Blockbench rig against a <animal>_spec.py, part by part.

Run this whenever the user has hand-edited the rig in Blockbench, BEFORE running
gen_bb_build.py (which wipes the outliner).  Anything it prints has to be folded back into
the spec by hand, because the spec -- not the Blockbench project -- is the source of truth
that every later stage reads: the builder, the UV packer, the Java port and the pose solver.

    python scripts/read_back.py tapir_spec

Prints the exact `add(...)` line for every part that differs, ready to paste into the spec.
"""
import io
import json
import pathlib
import subprocess
import sys
from contextlib import redirect_stdout

MOD = sys.argv[1]
HERE = pathlib.Path(__file__).parent
OUT = HERE.parent / "scratch" / "rig_live.json"

DUMP_JS = """
(function () {
  var out = {};
  Cube.all.forEach(function (c) {
    var g = c.parent;
    out[c.name] = {
      f: c.from.map(function (v) { return +v.toFixed(4); }),
      t: c.to.map(function (v) { return +v.toFixed(4); }),
      uv: c.uv_offset,
      o: (g && g.origin ? g.origin : [0,0,0]).map(function (v) { return +v.toFixed(4); }),
      r: (g && g.rotation ? g.rotation : [0,0,0]).map(function (v) { return +v.toFixed(4); }),
      p: (g && g.parent && g.parent.name) ? g.parent.name : null
    };
  });
  require('fs').writeFileSync(window.OUT + 'rig_live.json', JSON.stringify(out));
  return Object.keys(out).length + ' cubes dumped';
})()
"""

js = HERE.parent / "scratch" / "_dump_rig.js"
js.parent.mkdir(exist_ok=True)
js.write_text(DUMP_JS, encoding="utf-8")
print(subprocess.run([sys.executable, str(HERE / "bb.py"), "eval", str(js)],
                     capture_output=True, text=True).stdout.strip())

sys.path.insert(0, str(HERE))
S = __import__(MOD)
buf = io.StringIO()
with redirect_stdout(buf):
    S.emit_json()
spec = {p["n"]: p for p in json.loads(buf.getvalue())["parts"]}
live = json.loads(OUT.read_text())

diffs = []
for n in sorted(set(spec) | set(live)):
    if n not in spec:
        diffs.append((n, "ONLY IN BLOCKBENCH -- a part the user added", live[n]))
        continue
    if n not in live:
        diffs.append((n, "ONLY IN SPEC -- deleted in Blockbench", None))
        continue
    a, b = spec[n], live[n]
    bad = []
    for k in ("f", "t", "o", "r"):
        if any(abs(x - y) > 1e-3 for x, y in zip(a[k], b[k])):
            bad.append(f"{k}: spec={a[k]} live={b[k]}")
    if list(a["uv"]) != list(b["uv"]):
        bad.append(f"uv: spec={a['uv']} live={b['uv']}")
    if a["p"] != b["p"]:
        bad.append(f"parent: spec={a['p']} live={b['p']}")
    if bad:
        diffs.append((n, "; ".join(bad), b))

print("=" * 78)
print(f"LIVE RIG vs {MOD}      {len(spec)} spec parts, {len(live)} live parts")
print("=" * 78)
if not diffs:
    print("  identical -- nothing to fold back")
    raise SystemExit(0)
for n, why, b in diffs:
    print(f"\n  {n}: {why}")
    if b:
        print(f'    add("{n}", "{b["p"]}", {tuple(b["o"])}, {tuple(b["r"])},')
        print(f'        {tuple(b["f"])}, {tuple(b["t"])}, "<cls>")')
print(f"\n{len(diffs)} part(s) differ -- fold these into scripts/{MOD}.py BEFORE rebuilding.")
