#!/usr/bin/env python3
"""Render several species of one model side by side at their real relative in-game sizes (one Blockbench
scene, each rig offset along X and uniformly scaled by its species scale), side and front views.

    python scripts/lineup.py out.png ModelMammoth "woolly|mammoth/woolly.png|1.0|--hide ..." ...
"""
import json
import pathlib
import re
import shlex
import subprocess
import sys

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
SCR = ROOT / "scratch"
BB = [sys.executable, str(ROOT / "scripts/bb.py"), "eval"]


def run_js(text, name="tmp_eval.js"):
    p = SCR / name
    p.write_text(text, encoding="utf-8")
    subprocess.run(BB + [str(p)], check=True, capture_output=True, timeout=180)


def main():
    out, model, axis, specs = pathlib.Path(sys.argv[1]), sys.argv[2], sys.argv[3], sys.argv[4:]   # axis: x = front view row, z = side view row
    ai = 0 if axis == "x" else 2
    run_js("(function(){ newProject(Formats.modded_entity); Project.name='lineup'; return 1; })()")
    x = 0.0
    labels = []
    for k, spec in enumerate(specs):
        label, skin, scale, opts = (spec.split("|") + [""])[:4]
        scale = float(scale)
        rig = SCR / ("lineup_%d.js" % k)
        subprocess.run([sys.executable, str(ROOT / "scripts/gen_bb_rig.py"), model, str(rig), "--skin", skin] + shlex.split(opts),
                       check=True, capture_output=True)
        js = rig.read_text(encoding="utf-8")
        run_js(js, "lineup_build.js")
        # move + scale the rig just built: every root group not yet tagged
        run_js("""(function(){
          var s = %f, dx = %f, n = 0, AI = %d;
          Group.all.forEach(function (g) {
            if (g.parent === 'root' && !g.lineupDone) {
              g.lineupDone = true; n++;
            }
          });
          Outliner.elements.forEach(function (e) {
            if (e.lineupDone) return; e.lineupDone = true;
            e.from = e.from.map(function (v, i) { return v * s + (i === AI ? dx : 0); });
            e.to = e.to.map(function (v, i) { return v * s + (i === AI ? dx : 0); });
            e.origin = e.origin.map(function (v, i) { return v * s + (i === AI ? dx : 0); });
          });
          Group.all.forEach(function (g) {
            if (g.lineupMoved) return; g.lineupMoved = true;
            g.origin = g.origin.map(function (v, i) { return v * s + (i === AI ? dx : 0); });
          });
          Canvas.updateAll();
          return n;
        })()""" % (scale, x, ai))
        labels.append(label)
        x += 34.0 if ai == 0 else 58.0
    mid = x / 2 - (17 if ai == 0 else 29)
    r = x * 0.95
    if ai == 0:   # row along x, camera in front (-z): the head-on widths side by side
        shots = [{"pos": [mid, 26, -r], "tgt": [mid, 22, 0], "file": out.name}]
    else:         # row along z, camera at +x: side profiles
        shots = [{"pos": [r, 26, mid], "tgt": [0, 22, mid], "file": out.name}]
    run_js("window.SHOTS = %s; window.OUT = %s;\n" % (json.dumps(shots), json.dumps(str(SCR).replace(chr(92), "/") + "/"))
           + (ROOT / "scripts/bb_shots.js").read_text(encoding="utf-8"))
    print(labels)


if __name__ == "__main__":
    main()
