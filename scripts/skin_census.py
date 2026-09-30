#!/usr/bin/env python3
"""HLS quantile census of every shipped skin for a type -- run BEFORE committing a new one.

Eyeballing a skin against its siblings does not work: Blockbench's viewport lighting reads
dark, and a full treatment change (agouti grizzle vs flat hide) barely moves a skin's mean
colour. Two near-collisions were caught this way that both looked fine in isolation.

Reports median hue / saturation / lightness over the OPAQUE texels of each PNG, plus the
closest sibling for every skin in a simple hue+lightness metric. Anything under ~10 apart
is the "same animal in a different colour" failure the CLAUDE.md skin-painting notes are
about.

    python scripts/skin_census.py bison
    python scripts/skin_census.py bison scratch/candidate.png
"""
import colorsys
import pathlib
import statistics
import sys

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/untamedwilds/textures/entity"


def hls(path):
    im = Image.open(path).convert("RGBA")
    h, l, s = [], [], []
    for r, g, b, a in im.getdata():
        if a < 128:
            continue
        hh, ll, ss = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
        h.append(hh * 360)
        l.append(ll)
        s.append(ss)
    if not h:
        return None
    l.sort()
    # Spread matters as much as the median. A PIEBALD skin is half dark and half cream, so
    # its median lands mid-range and collides with every plain mid-brown sibling -- while
    # in game it looks nothing like them. p90-p10 separates "patterned" from "plain", and
    # two skins only really collide if their medians AND their spreads are close.
    p10, p90 = l[len(l) // 10], l[len(l) * 9 // 10]
    return (statistics.median(h), statistics.median(l), statistics.median(s), len(h), p90 - p10)


def dist(a, b):
    dh = abs(a[0] - b[0])
    dh = min(dh, 360 - dh)
    # hue, lightness, lightness-SPREAD (plain vs patterned) and SATURATION. Saturation
    # earns its place: bison:giant_buffalo (sat 0.02) and bison:aurochs (sat 0.35) sit in
    # the same hue/lightness cell and are obviously different animals in game.
    return (dh ** 2
            + (abs(a[1] - b[1]) * 200) ** 2
            + (abs(a[4] - b[4]) * 120) ** 2
            + (abs(a[2] - b[2]) * 80) ** 2) ** 0.5


rows = {}
for p in sorted((TEX / sys.argv[1]).glob("*.png")):
    v = hls(p)
    if v:
        rows[p.stem] = v
for extra in sys.argv[2:]:
    p = pathlib.Path(extra)
    rows[p.stem + " (NEW)"] = hls(p)

print("%-28s %6s %6s %6s %6s %7s   %s" % ("skin", "hue", "light", "sat", "sprd", "px", "nearest sibling"))
for n, v in rows.items():
    near = min(((m, dist(v, w)) for m, w in rows.items() if m != n), key=lambda t: t[1])
    print("%-28s %6.1f %6.3f %6.3f %6.3f %7d   %s (%.1f)%s"
          % (n, v[0], v[1], v[2], v[4], v[3], near[0], near[1],
             "   <-- TOO CLOSE" if near[1] < 10 else ""))
