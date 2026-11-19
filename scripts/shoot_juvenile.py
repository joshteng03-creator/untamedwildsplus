#!/usr/bin/env python3
"""Screenshot every juvenile draft on its juvenile rig in Blockbench, one contact sheet per type.

    python scripts/shoot_juvenile.py            # all drafts in scratch/juvenile
    python scripts/shoot_juvenile.py big_cat    # one type
"""
import json
import pathlib
import subprocess
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paint_juvenile import PLAN, MODEL

ROOT = pathlib.Path(__file__).resolve().parent.parent
SCR = ROOT / "scratch"
DRAFTS = SCR / "juvenile"
# camera per juvenile model: (position, target) pairs in Blockbench space; side view, then 3/4 front
VIEWS = {
    "ModelBigCatCub": [([46, 11, -4], [0, 7, -4]), ([-28, 17, -40], [0, 7, -4])],
    "ModelBearCub": [([48, 11, -3], [0, 7, -3]), ([-30, 18, -42], [0, 7, -3])],
    "ModelBoarPiglet": [([34, 8, -1], [0, 5, -1]), ([-21, 12, -31], [0, 5, -1])],
    "ModelBisonCalf": [([66, 17, 0], [0, 12, 0]), ([-42, 26, -58], [0, 12, 0])],
    "ModelRhinoCalf": [([68, 15, 0], [0, 10, 0]), ([-44, 24, -60], [0, 10, 0])],
}


def shoot(folder, skin):
    model = MODEL[folder]
    rig = SCR / "rig_juv.js"
    subprocess.run([sys.executable, str(ROOT / "scripts/gen_bb_rig.py"), model, str(rig), "--skin",
                    str(DRAFTS / folder / (skin + ".png"))], check=True, capture_output=True)
    shots = [{"pos": p, "tgt": t, "file": "juv_%s_%s_%d.png" % (folder, skin, i)} for i, (p, t) in enumerate(VIEWS[model])]
    js = SCR / "shoot_juv.js"
    # A fresh modded-entity project first: a rig built into a Java Block tab silently loses every
    # group rotation. Then two evals: the rig builds its meshes and loads the skin asynchronously, so
    # a screenshot in the same call renders an empty scene.
    newp = SCR / "new_entity_project.js"
    newp.write_text("(function () { newProject(Formats.modded_entity); Project.name = 'preview_rig'; return Format.id; })()", encoding="utf-8")
    subprocess.run([sys.executable, str(ROOT / "scripts/bb.py"), "eval", str(newp)], check=True, capture_output=True)
    subprocess.run([sys.executable, str(ROOT / "scripts/bb.py"), "eval", str(rig)], check=True, capture_output=True)
    js.write_text("window.SHOTS = %s; window.OUT = %s;\n" % (json.dumps(shots), json.dumps(str(SCR).replace("\\", "/") + "/"))
                  + (ROOT / "scripts/bb_shots.js").read_text(encoding="utf-8"), encoding="utf-8")
    subprocess.run([sys.executable, str(ROOT / "scripts/bb.py"), "eval", str(js)], check=True, capture_output=True)
    return [SCR / s["file"] for s in shots]


def sheet(folder, skins):
    tiles = []
    for skin in skins:
        ims = [Image.open(f).convert("RGB") for f in shoot(folder, skin)]
        ims = [i.crop((int(i.width * .18), int(i.height * .12), int(i.width * .82), int(i.height * .88))) for i in ims]
        h = 230
        ims = [i.resize((int(i.width * h / i.height), h)) for i in ims]
        tile = Image.new("RGB", (sum(i.width for i in ims), h + 22), (30, 30, 30))
        x = 0
        for i in ims:
            tile.paste(i, (x, 22))
            x += i.width
        ImageDraw.Draw(tile).text((6, 5), skin, fill=(235, 235, 235))
        tiles.append(tile)
    cols = 2
    tw, th = tiles[0].size
    rows = -(-len(tiles) // cols)
    out = Image.new("RGB", (tw * cols, th * rows), (30, 30, 30))
    for k, t in enumerate(tiles):
        out.paste(t, ((k % cols) * tw, (k // cols) * th))
    path = SCR / ("juvenile_sheet_%s.png" % folder)
    out.save(path)
    print(path)


if __name__ == "__main__":
    for folder, plan in PLAN.items():
        if sys.argv[1:] and folder not in sys.argv[1:]:
            continue
        sheet(folder, list(plan))
