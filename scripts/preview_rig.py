#!/usr/bin/env python3
"""Screenshot skins on their real rigs in Blockbench (side + three-quarter), one row per skin, framed from
the rig's bounds. Each row gets a FRESH modded-entity project (a rig built into a Java Block tab loses all
group rotations), and load/shoot are separate evals (the rig builds asynchronously).

    python scripts/preview_rig.py out.png "label|ModelDeer|path/to/skin.png|--hide a,b --scale p:0.8" ...
"""
import json
import os
import pathlib
import shlex
import subprocess
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import java2bb

ROOT = pathlib.Path(__file__).resolve().parent.parent
SCR = ROOT / "scratch"
BB = [sys.executable, str(ROOT / "scripts/bb.py"), "eval"]


def run_js(text):
    p = SCR / "tmp_eval.js"
    p.write_text(text, encoding="utf-8")
    subprocess.run(BB + [str(p)], check=True, capture_output=True)


def shoot(label, model, skin, opts, k):
    run_js("(function () { newProject(Formats.modded_entity); Project.name = 'preview_%d'; return 1; })()" % k)
    rig = SCR / "rig_preview.js"
    subprocess.run([sys.executable, str(ROOT / "scripts/gen_bb_rig.py"), model, str(rig), "--skin", skin] + shlex.split(opts),
                   check=True, capture_output=True)
    subprocess.run(BB + [str(rig)], check=True, capture_output=True)
    parts = java2bb.convert(model)
    lo = [min(p["f"][i] for p in parts) for i in range(3)]
    hi = [max(p["t"][i] for p in parts) for i in range(3)]
    c = [(lo[i] + hi[i]) / 2 for i in range(3)]
    r = max(hi[i] - lo[i] for i in range(3)) * float(os.environ.get("PV_ZOOM", "1"))   # <1 = closer
    shots = [{"pos": [c[0] + r * 1.6, c[1] + r * 0.2, c[2]], "tgt": c, "file": "pv_%d_side.png" % k},
             {"pos": [c[0] - r * 1.1, c[1] + r * 0.6, c[2] - r * 1.1], "tgt": c, "file": "pv_%d_front.png" % k}]
    if os.environ.get("PV_FRONT"):   # straight head-on (-z) + straight side: for proportion checks
        shots = [{"pos": [c[0] + r * 1.8, c[1], c[2]], "tgt": c, "file": "pv_%d_side.png" % k},
                 {"pos": [c[0], c[1], c[2] - r * 1.8], "tgt": c, "file": "pv_%d_front.png" % k}]
    run_js("window.SHOTS = %s; window.OUT = %s;\n" % (json.dumps(shots), json.dumps(str(SCR).replace("\\", "/") + "/"))
           + (ROOT / "scripts/bb_shots.js").read_text(encoding="utf-8"))
    ims = [Image.open(SCR / s["file"]).convert("RGB") for s in shots]
    ims = [i.crop((int(i.width * .15), int(i.height * .08), int(i.width * .85), int(i.height * .92))) for i in ims]
    h = 260
    ims = [i.resize((int(i.width * h / i.height), h)) for i in ims]
    t = Image.new("RGB", (sum(i.width for i in ims), h + 20), (40, 40, 40))
    x = 0
    for i in ims:
        t.paste(i, (x, 20))
        x += i.width
    ImageDraw.Draw(t).text((6, 4), label, fill=(235, 235, 235))
    return t


def main():
    out = pathlib.Path(sys.argv[1])
    tiles = []
    for k, spec in enumerate(sys.argv[2:]):
        label, model, skin, opts = (spec.split("|") + [""])[:4]
        tiles.append(shoot(label, model, skin, opts, k))
    W = max(t.width for t in tiles)
    sheet = Image.new("RGB", (W, sum(t.height for t in tiles)), (40, 40, 40))
    y = 0
    for t in tiles:
        sheet.paste(t, (0, y))
        y += t.height
    sheet.save(out)
    print(out)


if __name__ == "__main__":
    main()
