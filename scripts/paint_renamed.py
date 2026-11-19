#!/usr/bin/env python3
"""Repaint the two species renamed in place (2026-10-08, roadmap Phase 3.4), which still wore their old
animals' skins:

  deer/eucladoceros    -> Schomburgk's deer (Rucervus schomburgki): warm chestnut, pale cream belly
  mammoth/cretan_dwarf -> Sicilian dwarf elephant (Palaeoloxodon falconeri): from its ancestor, the
                          straight-tusked elephant, in a lighter warmer grey-brown

Method: luminance-rank recolour of a painted base skin into a target colour ramp, so all the painted
shading, countershading and markings survive and only the palette changes. Antlers, tusks, eyes, nose,
hooves and toenails are excluded by part (their texels are copied unchanged), as is anything transparent.

    python scripts/paint_renamed.py            # drafts -> scratch/renamed/
    python scripts/paint_renamed.py --install  # over the shipped PNGs
"""
import bisect
import pathlib
import shutil
import sys

from PIL import Image

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from juvenile_census import texels as model_texels  # noqa: F401  (kept for parity with other painters)
from java2bb import parse

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/untamedwilds/textures/entity"
OUT = ROOT / "scratch/renamed"

JOBS = {
    "deer/eucladoceros": dict(model="ModelDeer", base="deer/eucladoceros",
                              keep=("pedicle", "beam", "tine", "palm", "br_", "antler", "hoof", "eye", "nose"),
                              ramp=["#3e2416", "#63391f", "#86512c", "#a26a3c", "#be8c5c", "#dcc29a"]),
    "mammoth/cretan_dwarf": dict(model="ModelMammoth", base="mammoth/straight_tusked",
                                 keep=("tusk", "eye", "toe", "nail"),
                                 ramp=["#3e3732", "#5c544c", "#766d64", "#8e857a", "#a69c90", "#c0b7aa"]),
}


def hx(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def part_texels(model, names):
    order, uv, rp, box, rot, par, mirror = parse(model)
    out = set()
    for n in order:
        if n not in box or not any(k in n for k in names):
            continue
        u, v = uv[n]
        w, h, d = (abs(x) for x in box[n][3:6])
        for y in range(int(v), int(-(-(v + d + h) // 1))):
            for x in range(int(u), int(-(-(u + 2 * d + 2 * w) // 1))):
                out.add((x, y))
    return out


def ramp_at(stops, q):
    q = min(max(q, 0.0), 1.0) * (len(stops) - 1)
    i = min(int(q), len(stops) - 2)
    t = q - i
    a, b = stops[i], stops[i + 1]
    return tuple(int(a[k] + (b[k] - a[k]) * t) for k in range(3))


def paint(job):
    base = Image.open(TEX / (job["base"] + ".png")).convert("RGBA")
    keep = part_texels(job["model"], job["keep"])
    stops = [hx(c) for c in job["ramp"]]
    hide = sorted(lum(base.getpixel((x, y))) for y in range(base.height) for x in range(base.width)
                  if base.getpixel((x, y))[3] > 0 and (x, y) not in keep)
    out = base.copy()
    for y in range(base.height):
        for x in range(base.width):
            c = base.getpixel((x, y))
            if c[3] == 0 or (x, y) in keep:
                continue
            q = bisect.bisect_left(hide, lum(c)) / max(1, len(hide) - 1)
            out.putpixel((x, y), ramp_at(stops, q) + (c[3],))
    return out


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for key, job in JOBS.items():
        img = paint(job)
        dst = OUT / (key.replace("/", "_") + ".png")
        img.save(dst)
        if "--install" in sys.argv:
            shutil.copy(dst, TEX / (key + ".png"))
        print(key, "->", dst, "(installed)" if "--install" in sys.argv else "")


if __name__ == "__main__":
    main()
