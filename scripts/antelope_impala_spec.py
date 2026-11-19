#!/usr/bin/env python3
"""Impala LYRE horn family -- SOP phases 1/2/4/5 for the Phase 4.7 antelope EXTENSION (2026-10-23).

ModelAntelope is EXTENDED again, not rebuilt: the 56 shipped boxes (the 36-box gazelle rebuild + the 20-box horn-family
extension) stay byte-identical, so all nine shipped antelope skins keep working. This file adds only the six LYRE boxes
and packs them into UV space no shipped rect touches (an occupancy raster of all 56 footprints, not a v-floor: the earlier
extension already used rows 53..65).

Same authoring space and pedicle as antelope_horns_spec.py: Blockbench project space, Y up, ground y=0, -Z = nose, -X =
the animal's left, absolute coordinates as java2bb emits them; every new segment is chained along +Y from the pedicle
(x +-1.05, y 38.0, z -13.6), each child's origin the joint in its parent's unrotated frame.

LYRE (impala rams; ewes are hornless): from the side the horn sweeps up and back, then rises, the tip hooking forward;
from the front it bows OUT through the middle and turns back IN at the tips -- the lyre. Every joint turns > 8 degrees of
yaw or roll (the load-bearing twist rule: rx alone leaves consecutive side planes parallel and z-fighting).

Run:  python scripts/antelope_impala_spec.py [--uv | --zfight | --json]
"""
import json
import math
import pathlib
import sys

import numpy as np

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import antelope_horns_spec as H                                       # noqa: E402  (kinematics + the shipped rig)

D = math.radians
CANVAS = (256, 128)
IMPALA = ("impala", 9, "LYRE", "light", 0.85)                         # Aepyceros melampus
EXISTING = H.EXISTING                                                 # the 56 shipped boxes, via java2bb
assert len(EXISTING) == 56, len(EXISTING)

PARTS = []
BY_NAME = H.BY_NAME                                                   # shared, so world() resolves new + shipped


def add(name, parent, origin, rot, a, b, cls):
    lo = [min(a[i], b[i]) for i in range(3)]
    hi = [max(a[i], b[i]) for i in range(3)]
    rec = (name, parent, tuple(origin), tuple(rot), tuple(lo), tuple(hi), cls)
    PARTS.append(rec)
    BY_NAME[name] = rec


PED = H.PED
for side, s in (("left", -1.0), ("right", 1.0)):
    parent = "head"
    for suf, y0, y1, hw, z0, z1, rx, rys, rzs in [
        # suf  y0    y1    hw     z0      z1     rx   ry_s  rz_s
        ("1", 37.2, 43.2, 0.56, -14.36, -12.84, 32,   0,   16),     # up and back, bowing out
        ("2", 42.4, 47.8, 0.45, -14.20, -13.00, -16, 12,   10),     # rising, still opening
        ("3", 47.0, 51.4, 0.34, -14.10, -13.10, -24, 18,  -42),     # the tip hooks forward and turns back in
    ]:
        name = f"lyre_{suf}_{side}"
        add(name, parent, (s * PED[0], y0 + 0.8, PED[2]), (rx, s * rys, -s * rzs),
            (s * (PED[0] - hw), y0, z0), (s * (PED[0] + hw), y1, z1), "lyre")
        parent = name
NEW = list(PARTS)


def footprint(p):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    w, h, d = (abs(b[i] - a[i]) for i in range(3))
    return int(math.ceil(2 * (w + d))), int(math.ceil(h + d))


def occupancy():
    occ = np.zeros((CANVAS[1], CANVAS[0]), bool)
    for pe in EXISTING:
        a, b = np.asarray(pe["f"], float), np.asarray(pe["t"], float)
        w, h, d = (abs(b[i] - a[i]) for i in range(3))
        u, v = pe["uv"]
        occ[v:min(CANVAS[1], v + int(math.ceil(h + d))), u:min(CANVAS[0], u + int(math.ceil(2 * (w + d))))] = True
    return occ


def uvpack():
    occ = occupancy()
    placed = {}
    for p in sorted(NEW, key=lambda q: -footprint(q)[1]):
        fw, fh = footprint(p)
        spot = None
        for v in range(CANVAS[1] - fh + 1):
            for u in range(CANVAS[0] - fw + 1):
                if not occ[v:v + fh, u:u + fw].any():
                    spot = (u, v)
                    break
            if spot:
                break
        assert spot, f"no free UV space for {p[0]}"
        u, v = spot
        occ[v:v + fh, u:u + fw] = True
        placed[p[0]] = (u, v, fw, fh)
    print(f"UV: {len(placed)} lyre boxes placed in free space, none touching the 56 shipped rects")
    for n, (u, v, fw, fh) in placed.items():
        print(f"  {n:<14} texOffs({u:3d},{v:3d})  {fw}x{fh}")
    return placed


def emit_json():
    import io
    from contextlib import redirect_stdout
    with redirect_stdout(io.StringIO()):
        placed = uvpack()
    out = list(EXISTING)
    for name, parent, o, r, a, b, cls in NEW:
        u, v, _, _ = placed[name]
        out.append({"n": name, "p": parent, "o": [round(x, 4) for x in o], "r": [round(x, 4) for x in r],
                    "f": [round(x, 4) for x in a], "t": [round(x, 4) for x in b], "uv": [u, v], "c": cls})
    print(json.dumps({"canvas": list(CANVAS), "parts": out}, separators=(",", ":")))


OTHER_HORNS = ("horn_", "spiral_", "rapier_", "scimitar_")


def zfight():
    import zfight_sweep as Z
    allp = {p["n"]: (p["n"], p["p"], tuple(p["o"]), tuple(p["r"]), tuple(p["f"]), tuple(p["t"]), "existing") for p in EXISTING}
    for p in NEW:
        allp[p[0]] = p
    parts = {}
    for name, (nm, parent, o, r, a, b, cls) in allp.items():
        if abs(a[0] - b[0]) < 1e-9 or name.startswith(OTHER_HORNS):
            continue                     # eye planes; and the other families never render with the lyre
        q = Z.Part(name)
        po = np.asarray(allp[parent][2], float) if parent else np.zeros(3)
        q.rp = np.asarray(o, float) - po
        q.rot = np.array([D(r[0]), D(r[1]), D(r[2])])
        q.lo = np.asarray(a, float) - np.asarray(o, float)
        q.hi = np.asarray(b, float) - np.asarray(o, float)
        q.parent = parent
        parts[name] = q
    new = {p[0] for p in NEW}
    shown = 0
    for area, na, nb, fa, fb, rel in Z.sweep(parts):
        if na in new or nb in new:
            shown += 1
            print(f"  {area:6.2f}u2  {na:<16} {'XYZ'[fa[0]]}  <->  {nb:<16} {'XYZ'[fb[0]]}{rel}")
    print("  lyre z-fight: " + ("clean" if not shown else f"{shown} pair(s)"))


def main():
    skull_top = H.world("head", H.corners(BY_NAME["head"]))[:, 1].max()
    segs = [p[0] for p in NEW if p[0].endswith("_left")]
    pts = np.vstack([H.world(n, H.corners(BY_NAME[n])) for n in segs])
    print(f"LYRE (left)  x {pts[:, 0].min():6.2f}..{pts[:, 0].max():6.2f}  y {pts[:, 1].min():6.2f}..{pts[:, 1].max():6.2f}"
          f"  z {pts[:, 2].min():6.2f}..{pts[:, 2].max():6.2f};  rises {pts[:, 1].max() - skull_top:.2f}u above the skull")
    for n in segs:
        c = H.world(n, H.corners(BY_NAME[n])).mean(0)
        print(f"  {n:<14} centre x {c[0]:6.2f} y {c[1]:6.2f} z {c[2]:6.2f}")
    print(f"impala scale = {IMPALA[4]} * 16 / {H.WITHERS_U} = {IMPALA[4] * 16 / H.WITHERS_U:.4f}")


if __name__ == "__main__":
    if "--zfight" in sys.argv:
        zfight()
    elif "--uv" in sys.argv:
        uvpack()
    elif "--json" in sys.argv:
        emit_json()
    else:
        main()
