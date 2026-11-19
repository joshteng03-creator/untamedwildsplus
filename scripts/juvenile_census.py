#!/usr/bin/env python3
"""How much of each skin's JUVENILE UV region is painted (2026-10-07).

Juvenile models (cubs, calves, piglets) are separate Citadel models that read their own rects of the
adult PNG. Several painters only ever filled the adult rects, so those babies render see-through.
For each juvenile model this unions the MC box-UV face rects of every part and reports, per skin of
that type, the opaque fraction of those texels (and of the texels the ADULT model does not also use,
which is where the gaps live).

    python scripts/juvenile_census.py              # all juvenile models
    python scripts/juvenile_census.py big_cat      # one type
"""
import pathlib
import sys

from PIL import Image

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from java2bb import parse, canvas

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/untamedwilds/textures/entity"

# juvenile model -> (texture folder, adult model)
JUVENILES = {
    "ModelBigCatCub": ("big_cat", "ModelBigCat"),
    "ModelBearCub": ("bear", "ModelBear"),
    "ModelBoarPiglet": ("boar", "ModelBoar"),
    "ModelBisonCalf": ("bison", "ModelBison"),
    "ModelRhinoCalf": ("rhino", "ModelRhino"),
    "ModelCamelCalf": ("camel", "ModelCamel"),
    "ModelHippoCalf": ("hippo", "ModelHippo"),
}


def texels(model):
    """Every texel a model's box-UV faces sample, on its canvas."""
    order, uv, rp, box, rot, parent, mirror = parse(model)
    cw, ch = canvas(model)
    out = set()
    for n in order:
        if n not in box:
            continue
        u, v = uv[n]
        w, h, d = (abs(x) for x in box[n][3:6])
        rects = [(u + d, v, u + d + w, v + d), (u + d + w, v, u + d + 2 * w, v + d),   # top, bottom
                 (u, v + d, u + 2 * d + 2 * w, v + d + h)]                              # 4 sides
        for x0, y0, x1, y1 in rects:
            for y in range(int(y0), int(-(-y1 // 1))):
                for x in range(int(x0), int(-(-x1 // 1))):
                    if 0 <= x < cw and 0 <= y < ch:
                        out.add((x, y))
    return out, (cw, ch)


def census(model, folder, adult):
    juv, size = texels(model)
    try:
        ad, _ = texels(adult)
    except Exception:
        ad = set()
    only = juv - ad
    rows = []
    for png in sorted((TEX / folder).glob("*.png")):
        im = Image.open(png).convert("RGBA")
        if im.size != size:
            rows.append((png.name, None, None, im.size))
            continue
        a = im.load()
        cov = sum(1 for p in juv if a[p][3] > 0) / max(1, len(juv))
        cov_only = sum(1 for p in only if a[p][3] > 0) / max(1, len(only)) if only else 1.0
        rows.append((png.name, cov, cov_only, im.size))
    return len(juv), len(only), rows


if __name__ == "__main__":
    want = sys.argv[1:] or None
    for model, (folder, adult) in JUVENILES.items():
        if want and folder not in want:
            continue
        n, n_only, rows = census(model, folder, adult)
        print("== %s (%s): %d texels, %d not shared with %s" % (model, folder, n, n_only, adult))
        for name, cov, cov_only, size in rows:
            if cov is None:
                print("   %-34s size %s != model canvas" % (name, size))
            else:
                flag = "  <-- UNPAINTED" if cov_only < 0.5 else ("  <-- partial" if cov_only < 0.95 else "")
                print("   %-34s juvenile %5.1f%%   juvenile-only %5.1f%%%s" % (name, cov * 100, cov_only * 100, flag))
