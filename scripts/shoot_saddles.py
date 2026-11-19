#!/usr/bin/env python3
"""Preview each saddle overlay on its model in Blockbench (base skin + overlay composited), side and
three-quarter-from-above views, framed from the rig's own bounds. -> scratch/saddle_sheet.png"""
import json
import pathlib
import subprocess
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import java2bb
from gen_saddles import SPEC, OUT, EXTRA_PREVIEWS, HIDE

ROOT = pathlib.Path(__file__).resolve().parent.parent
SCR = ROOT / "scratch"
BB = [sys.executable, str(ROOT / "scripts/bb.py"), "eval"]


def bounds(model):
    parts = java2bb.convert(model)
    lo = [min(p["f"][i] for p in parts) for i in range(3)]
    hi = [max(p["t"][i] for p in parts) for i in range(3)]
    return lo, hi


def run_js(text):
    p = SCR / "tmp_eval.js"
    p.write_text(text, encoding="utf-8")
    subprocess.run(BB + [str(p)], check=True, capture_output=True)


def main():
    tiles = []
    only = sys.argv[1:]
    jobs = []
    for typ, spec in SPEC.items():
        if only and typ not in only:
            continue
        for key in [spec["preview"]] + [p for t, p, _ in EXTRA_PREVIEWS if t == typ]:
            jobs.append((typ, spec["model"], key, OUT / typ / ("preview_%s.png" % key.split("/")[1])))
    for typ, model, preview, skin in jobs:
        run_js("(function () { newProject(Formats.modded_entity); Project.name = 'saddle_%s'; return 1; })()" % typ)
        rig = SCR / "rig_saddle.js"
        cmd = [sys.executable, str(ROOT / "scripts/gen_bb_rig.py"), model, str(rig), "--skin", str(skin)]
        if HIDE.get(preview):
            cmd += ["--hide", ",".join(HIDE[preview])]
        subprocess.run(cmd, check=True, capture_output=True)
        subprocess.run(BB + [str(rig)], check=True, capture_output=True)
        lo, hi = bounds(model)
        c = [(lo[i] + hi[i]) / 2 for i in range(3)]
        r = max(hi[i] - lo[i] for i in range(3))
        shots = [{"pos": [c[0] + r * 1.5, c[1] + r * 0.25, c[2]], "tgt": c, "file": "saddle_%s_%s_side.png" % (typ, preview.split("/")[1])},
                 {"pos": [c[0] - r * 1.0, c[1] + r * 1.1, c[2] + r * 1.0], "tgt": c, "file": "saddle_%s_%s_top.png" % (typ, preview.split("/")[1])}]
        run_js("window.SHOTS = %s; window.OUT = %s;\n" % (json.dumps(shots), json.dumps(str(SCR).replace("\\", "/") + "/"))
               + (ROOT / "scripts/bb_shots.js").read_text(encoding="utf-8"))
        ims = [Image.open(SCR / s["file"]).convert("RGB") for s in shots]
        ims = [i.crop((int(i.width * .15), int(i.height * .1), int(i.width * .85), int(i.height * .9))) for i in ims]
        h = 240
        ims = [i.resize((int(i.width * h / i.height), h)) for i in ims]
        t = Image.new("RGB", (sum(i.width for i in ims), h + 20), (40, 40, 40))
        x = 0
        for i in ims:
            t.paste(i, (x, 20))
            x += i.width
        ImageDraw.Draw(t).text((6, 4), "%s  (on %s)" % (typ, preview.split("/")[1]), fill=(235, 235, 235))
        tiles.append(t)
    W = max(t.width for t in tiles)
    sheet = Image.new("RGB", (W, sum(t.height for t in tiles)), (40, 40, 40))
    y = 0
    for t in tiles:
        sheet.paste(t, (0, y))
        y += t.height
    sheet.save(SCR / "saddle_sheet.png")
    print(SCR / "saddle_sheet.png")


if __name__ == "__main__":
    main()
