#!/usr/bin/env python3
"""Audit skins painted on RESIZED rigs before the 2026-09-30 gen_bb_rig UV fix (2026-10-07).

Blockbench recomputed a box-UV cube's face rects from its SCALED size, so the painter wrote each resized
part into rects sized for the scaled box; the game samples rects sized for the ORIGINAL addBox. Per part
this compares the two rect sets on the shipped PNG:

  holes  -- texels the game samples that are transparent (see-through in game)
  stray  -- texels the game samples that the painter never wrote (they keep whatever the base skin had);
            reported as their share of the part and their colour distance from the painted texels
  spill  -- texels the painter wrote OUTSIDE the true rects (may sit on a neighbouring part's rect)

Blockbench's box UV may floor sizes; both the float and floored layouts are checked and the worse kept.

    python scripts/uv_resize_audit.py
"""
import math
import pathlib
import sys

from PIL import Image

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from java2bb import parse

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/untamedwilds/textures/entity"

LEGS = [[side + "_" + seg for seg in ("upper", "lower", "foot", "paw")]
        for side in ("arm_left", "arm_right", "leg_left", "leg_right")]
# skin -> (model, {part: (sx, sy, sz)})
CASES = {}


def subtree(parent, roots):
    out = []
    for r in roots:
        out.append(r)
        out += subtree(parent, [c for c, p in parent.items() if p == r])
    return out


def deer_case(skin, s, palmate=False):
    """The whole rack scales about the pedicles; ModelDeer hides the palm subtree unless palmate, and
    the plain lower beams when it is (showModel hides children too)."""
    order, uv, rp, box, rot, par, mirror = parse("ModelDeer")
    hidden = set(subtree(par, ["beam_low_left", "beam_low_right"] if palmate else ["palm_beam_left", "palm_beam_right"]))
    return "deer/" + skin, ("ModelDeer", {n: (s, s, s) for n in subtree(par, ["pedicle_left", "pedicle_right"]) if n not in hidden})


def canid_case(skin, longer, thinner):
    order, uv, rp, box, rot, par, mirror = parse("ModelDireWolf")
    parts = {}
    for chain in LEGS:
        for n in chain:
            if n in box:
                parts[n] = (thinner, longer, thinner)
    return "dire_wolf/" + skin, ("ModelDireWolf", parts)


def rects(u, v, w, h, d):
    return [(u + d, v, u + d + w, v + d), (u + d + w, v, u + d + 2 * w, v + d), (u, v + d, u + 2 * d + 2 * w, v + d + h)]


def texset(rs):
    out = set()
    for x0, y0, x1, y1 in rs:
        for y in range(int(math.floor(y0)), int(math.ceil(y1))):
            for x in range(int(math.floor(x0)), int(math.ceil(x1))):
                out.add((x, y))
    return out


def audit(skin, model, parts):
    order, uv, rp, box, rot, par, mirror = parse(model)
    im = Image.open(TEX / (skin + ".png")).convert("RGBA")
    a = im.load()
    W, H = im.size
    rows = []
    for n, (sx, sy, sz) in parts.items():
        if n not in box:
            continue
        u, v = uv[n]
        w, h, d = (abs(x) for x in box[n][3:6])
        true = {p for p in texset(rects(u, v, w, h, d)) if p[0] < W and p[1] < H}
        worst = None
        for floor in (False, True):
            pw, ph, pd = w * sx, h * sy, d * sz
            if floor:
                pw, ph, pd = math.floor(pw), math.floor(ph), math.floor(pd)
            painted = {p for p in texset(rects(u, v, pw, ph, pd)) if p[0] < W and p[1] < H}
            holes = sum(1 for p in true if a[p][3] == 0) / max(1, len(true))
            stray = true - painted
            spill = painted - true
            inner = [a[p] for p in true & painted if a[p][3] > 0]
            outer = [a[p] for p in stray if a[p][3] > 0]
            dist = 0.0
            if inner and outer:
                mi = [sum(c[k] for c in inner) / len(inner) for k in range(3)]
                mo = [sum(c[k] for c in outer) / len(outer) for k in range(3)]
                dist = math.dist(mi, mo)
            r = (holes, len(stray) / max(1, len(true)), dist, len(spill))
            worst = r if worst is None or (r[0], r[1] * r[2]) > (worst[0], worst[1] * worst[2]) else worst
        rows.append((n, len(true)) + worst)
    return rows


if __name__ == "__main__":
    cases = dict([deer_case("caribou", 1.193), deer_case("white_tailed", 0.514), deer_case("fallow", 0.654),
                  deer_case("sika", 0.474), deer_case("eucladoceros", 0.8),
                  canid_case("maned_wolf", 1.35, 0.64), canid_case("bush_dog", 0.72, 1.1)])
    for skin, (model, parts) in cases.items():
        rows = audit(skin, model, parts)
        bad = [r for r in rows if r[2] > 0.02 or (r[3] > 0.15 and r[4] > 30)]
        tot = sum(r[1] for r in rows)
        holes = sum(r[1] * r[2] for r in rows) / max(1, tot)
        print("== %-24s %2d parts, %4d texels sampled, holes %4.1f%%, %d parts flagged" % (skin, len(rows), tot, holes * 100, len(bad)))
        for n, t, h, s, dist, spill in sorted(bad, key=lambda r: -(r[2] * 1000 + r[3] * r[4]))[:8]:
            print("     %-22s holes %5.1f%%  stray %5.1f%% (colour off by %5.1f)  spill %d" % (n, h * 100, s * 100, dist, spill))
