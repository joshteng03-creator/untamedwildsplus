#!/usr/bin/env python3
"""Orca model spec + forward-kinematics checker -- SOP Phase 1 / 1b / 2 (2.9.0, group D, NEW TYPE `orca`).

Toothed whales on one body: the orca (Orcinus orca) and the false killer whale (Pseudorca crassidens). Tooling forked from
pinniped_spec.py.

Blockbench space: Y up, the hitbox floor y=0, -Z = nose, -X = left. Positive rx turns a part's -Z end UP.

BODY = fusiform and deep, round in section: a nested core (body_main) with a narrower back ridge and belly keel so the
section reads round rather than square; a blunt rounded head with no beak and a hinged lower jaw; the body tapering
through the rear and a laterally flattened tail stock into broad swept flukes. The dorsal fin is two stacked segments
(a wide base, a tall blade swept back) so it reads as a triangle; the pectoral fins are big rounded paddles.
Per-animal, in the model (no geometry change): dorsal height = setScale Y on dorsal_1 with its children (male orca 1.0,
female 0.55, false killer whale `finScale` 450 -> 0.45, which also reads as its small falcate fin).

1 block = 1 m along the body: 56u nose-to-fluke at scale 1.0 is 3.5 m, so scale = length m / 3.5.
Run:  python scripts/orca_spec.py [--uv | --zfight | --burial | --json]
"""
import math
import sys

import numpy as np

D = math.radians

SPECIES = [
    # name,               variant, real length m, dorsal fin scale (female orca 0.55 in the model)
    ("orca",                0, 7.0, 1.0),    # Orcinus orca -- adult male to ~8 m, females ~6 m
    ("false_killer_whale",  1, 5.0, 0.45),   # Pseudorca crassidens -- slender, all dark, small falcate fin
]
HITBOX = (2.0, 1.4)
CANVAS = (256, 128)

PARTS = []
BY_NAME = {}


def add(name, parent, origin, rot, a, b, cls):
    lo = [min(a[i], b[i]) for i in range(3)]
    hi = [max(a[i], b[i]) for i in range(3)]
    rec = (name, parent, tuple(float(x) for x in origin), tuple(float(x) for x in rot), tuple(lo), tuple(hi), cls)
    PARTS.append(rec)
    BY_NAME[name] = rec


def rmat(rx, ry, rz):
    cx, sx = math.cos(D(rx)), math.sin(D(rx))
    cy, sy = math.cos(D(ry)), math.sin(D(ry))
    cz, sz = math.cos(D(rz)), math.sin(D(rz))
    return (np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
            @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
            @ np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]))


def ancestry(name):
    out = []
    while name is not None:
        out.append(BY_NAME[name])
        name = BY_NAME[name][1]
    return out


def world(name, pts):
    p = np.asarray(pts, dtype=float).reshape(-1, 3)
    for _, _, o, r, *_ in ancestry(name):
        o = np.asarray(o, dtype=float)
        p = (rmat(*r) @ (p - o).T).T + o
    return p


def local_of(name, pts):
    p = np.asarray(pts, float).reshape(-1, 3)
    for _, _, o, r, *_ in reversed(ancestry(name)):
        o = np.asarray(o, float)
        p = (rmat(*r).T @ (p - o).T).T + o
    return p


def corners(p):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    return np.array([[x, y, z] for x in (a[0], b[0]) for y in (a[1], b[1]) for z in (a[2], b[2])])


def vol(p, n=7):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    ax = [np.linspace(a[i], b[i], n) if b[i] - a[i] > 1e-6 else np.array([a[i]]) for i in range(3)]
    return np.stack(np.meshgrid(*ax, indexing="ij"), -1).reshape(-1, 3)


def build():
    PARTS.clear()
    BY_NAME.clear()
    # ---- core: deep fusiform body, nested ridge + keel round the section ----
    add("body_main", None, (0, 6.5, 0), (0, 0, 0), (-5.2, 1.0, -10.5), (5.2, 12.0, 8.5), "body")
    add("body_ridge", "body_main", (0, 11.5, 0), (0, 0, 0), (-3.8, 10.0, -9.0), (3.8, 12.9, 6.8), "body")
    add("body_keel", "body_main", (0, 1.5, 0), (0, 0, 0), (-3.9, 0.2, -8.8), (3.9, 2.6, 7.0), "body")
    # ---- head: blunt and rounded, a hinged lower jaw; the melon sits a step lower than the back ----
    add("head", "body_main", (0, 6.5, -10.0), (0, 0, 0), (-4.3, 2.4, -19.0), (4.3, 10.8, -9.5), "head")
    add("head_melon", "head", (0, 7.0, -19.0), (0, 0, 0), (-2.9, 3.4, -22.0), (2.9, 9.2, -18.6), "head")
    add("jaw", "head", (0, 3.0, -11.5), (0, 0, 0), (-3.3, 1.2, -21.4), (3.3, 3.1, -11.0), "jaw")
    for side, s in (("left", -1.0), ("right", 1.0)):
        # eye plane 0.35u off the head side, low on the face just above the gape (where the orca's eye is)
        add(f"eye_{side}", "head", (s * 4.65, 5.6, -15.2), (0, 0, 0), (s * 4.65, 5.1, -16.2), (s * 4.65, 6.1, -14.2), "eye")
    # ---- rear: taper through the tail stock into the flukes ----
    add("body_rear", "body_main", (0, 6.6, 8.0), (0, 0, 0), (-4.4, 1.6, 7.5), (4.4, 11.4, 18.0), "body")
    add("tail_1", "body_rear", (0, 6.8, 17.5), (0, 0, 0), (-2.4, 3.0, 17.0), (2.4, 10.0, 26.0), "tail")
    add("tail_2", "tail_1", (0, 6.8, 25.5), (0, 0, 0), (-1.5, 4.5, 25.0), (1.5, 8.8, 31.5), "tail")
    for side, s in (("left", -1.0), ("right", 1.0)):
        # flukes: broad thin plates swept back 22 deg; rooted on the tail-stock sides so the swept inner corners never cross the midline (they did from x 0.4)
        add(f"fluke_{side}", "tail_2", (s * 1.5, 6.6, 30.5), (0, -s * 22, 0),
            (s * 1.5, 6.15, 29.0), (s * 8.3, 7.05, 33.5), "fluke")
    # ---- pectoral paddles: big, rounded, angled out-down-back ----
    for side, s in (("left", -1.0), ("right", 1.0)):
        # (first pass rz +s*32 pointed the paddles UP: on the left side -x, a positive roll lifts the outer end)
        # droop 18 keeps the tips above the hitbox floor (32 sank them 2.1u -- into the sand on a stranded animal)
        add(f"fin_{side}", "body_main", (s * 4.9, 3.4, -6.0), (0, -s * 35, -s * 18),
            (s * 4.9, 3.0, -8.8), (s * 14.4, 3.8, -3.0), "fin")
    # ---- dorsal fin: wide base + tall swept blade (male orca ~1.5 m at scale 2) ----
    # positive rx swings an upright part's top toward +z, i.e. BACK (first pass used negative: the fin leaned forward)
    add("dorsal_1", "body_main", (0, 12.3, 1.5), (6, 0, 0), (-0.7, 11.0, -2.0), (0.7, 17.5, 5.0), "dorsal")
    add("dorsal_2", "dorsal_1", (0, 17.0, 2.0), (12, 0, 0), (-0.45, 16.6, -0.2), (0.45, 24.0, 3.4), "dorsal")


build()


def footprint(p):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    w, h, d = (abs(b[i] - a[i]) for i in range(3))
    if p[6] == "eye":
        w = 0.0
    return int(math.ceil(2 * (w + d))), int(math.ceil(h + d))


def uvpack():
    """Shelf pack of the TRUE box-UV footprints, tallest first; zero overlaps asserted."""
    W, H = CANVAS
    items = sorted(((footprint(p), p[0]) for p in PARTS), key=lambda it: (-it[0][1], -it[0][0]))
    placed, x, y, shelf_h = {}, 0, 0, 0
    for (fw, fh), name in items:
        if x + fw > W:
            x, y, shelf_h = 0, y + shelf_h, 0
        placed[name] = (x, y, fw, fh)
        x += fw
        shelf_h = max(shelf_h, fh)
    used = y + shelf_h
    names = sorted(placed)
    for i in range(len(names)):
        ax, ay, aw, ah = placed[names[i]]
        for j in range(i + 1, len(names)):
            bx, by, bw, bh = placed[names[j]]
            if ax < bx + bw and bx < ax + aw and ay < by + bh and by < ay + ah:
                raise AssertionError(f"UV overlap: {names[i]} vs {names[j]}")
    print(f"UV SHELF PACK on {W}x{H}: {len(placed)} boxes, {used} rows used, zero overlaps asserted")
    assert used <= H, "does not fit"
    return placed


def emit_json():
    import io
    import json
    from contextlib import redirect_stdout
    with redirect_stdout(io.StringIO()):
        placed = uvpack()
    out = []
    for name, parent, o, r, a, b, cls in PARTS:
        u, v, _, _ = placed[name]
        out.append({"n": name, "p": parent, "o": [round(x, 4) for x in o], "r": [round(x, 4) for x in r],
                    "f": [round(x, 4) for x in a], "t": [round(x, 4) for x in b], "uv": [u, v], "c": cls})
    print(json.dumps({"canvas": list(CANVAS), "parts": out}, separators=(",", ":")))


def zfight():
    import zfight_sweep as Z
    parts = {}
    for name, parent, o, r, a, b, cls in PARTS:
        if cls == "eye":
            continue
        p = Z.Part(name)
        po = np.asarray(BY_NAME[parent][2], float) if parent else np.zeros(3)
        p.rp = np.asarray(o, float) - po
        p.rot = np.array([D(r[0]), D(r[1]), D(r[2])])
        p.lo = np.asarray(a, float) - np.asarray(o, float)
        p.hi = np.asarray(b, float) - np.asarray(o, float)
        p.parent = parent
        parts[name] = p
    shown = 0
    for area, na, nb, fa, fb, rel in Z.sweep(parts):
        shown += 1
        print(f"  {area:6.2f}u2  {na:<14} {'XYZ'[fa[0]]}  <->  {nb:<14} {'XYZ'[fb[0]]}{rel}")
    print("  z-fight: clean" if not shown else f"  z-fight: {shown} pair(s)")


def burial():
    solids = [p for p in PARTS if p[6] != "eye"]
    out = []
    for p in solids:
        pts = world(p[0], vol(p, 9))
        inside = np.zeros(len(pts), bool)
        for q in solids:
            if q[0] == p[0]:
                continue
            l = local_of(q[0], pts)
            a, b = np.asarray(q[4], float), np.asarray(q[5], float)
            inside |= np.all((l >= a - 1e-6) & (l <= b + 1e-6), axis=1)
        out.append((inside.mean(), p[0], p[6]))
    for frac, name, cls in sorted(out, reverse=True):
        flag = "   <== FEATURE MOSTLY BURIED" if frac > 0.55 and cls in ("fin", "fluke", "dorsal", "jaw", "tail") else ""
        print(f"  {frac:6.1%}  {name}{flag}")


def main():
    wc = {p[0]: world(p[0], corners(p)) for p in PARTS}
    lo = {k: v.min(0) for k, v in wc.items()}
    hi = {k: v.max(0) for k, v in wc.items()}
    nose = min(lo[n][2] for n in ("head_melon", "jaw"))
    tip = max(hi["fluke_left"][2], hi["fluke_right"][2])
    length = tip - nose
    span = hi["fluke_right"][0] - lo["fluke_left"][0]
    print(f"nose-to-fluke {length:.1f}u, fluke span {span:.1f}u ({span / length:.0%} of length; orca ~25%), "
          f"body depth {hi['body_ridge'][1] - lo['body_keel'][1]:.1f}u ({(hi['body_ridge'][1] - lo['body_keel'][1]) / length:.0%}; orca ~20%)")
    print(f"  dorsal tip {hi['dorsal_2'][1]:.1f}u = {hi['dorsal_2'][1] - hi['body_ridge'][1]:.1f}u above the back "
          f"({(hi['dorsal_2'][1] - hi['body_ridge'][1]) / length:.0%} of length; male orca up to ~22%)")
    print(f"  pectoral span tip-to-tip {hi['fin_right'][0] - lo['fin_left'][0]:.1f}u, lowest point {min(v[1] for v in lo.values()):+.2f}")
    for name, var, real, fin in SPECIES:
        s = real * 16.0 / length
        print(f"  {name:<19} {real:.1f} m -> scale {s:.3f}   width {(hi['body_main'][0] - lo['body_main'][0]) * s / 16:.2f} m"
              f"  depth {(hi['body_ridge'][1] - lo['body_keel'][1]) * s / 16:.2f} m  (hitbox {HITBOX[0]} x {HITBOX[1]})")
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head", "jaw", "head_melon"):
            p = BY_NAME[anc]
            l = local_of(anc, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gap = max(max(a[i] - l[i], l[i] - b[i]) for i in range(3))
            if gap < 0.3:
                print(f"  eye_{side} vs {anc} clear by {gap:.2f}  <== TOO CLOSE")
    tot = sum(fw * fh for fw, fh in (footprint(p) for p in PARTS))
    print(f"  {len(PARTS)} boxes, UV {tot} px = {100 * tot / (CANVAS[0] * CANVAS[1]):.0f}% of {CANVAS[0]}x{CANVAS[1]}")


if __name__ == "__main__":
    if "--zfight" in sys.argv:
        zfight()
    elif "--uv" in sys.argv:
        uvpack()
    elif "--json" in sys.argv:
        emit_json()
    elif "--burial" in sys.argv:
        burial()
    else:
        main()
