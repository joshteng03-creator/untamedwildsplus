#!/usr/bin/env python3
"""Antelope horn families + heavy build -- SOP phases 1/2/4/5 for the Track C EXTENSION.

ModelAntelope is EXTENDED, not rebuilt. The 36 existing boxes and their texOffs stay
byte-identical, so saiga.png / pronghorn.png / springbok.png keep working untouched. This
file adds only the new boxes and packs them into free UV space.

UV CONSTRAINT (the one that costs real rework if missed): the existing 36 footprints stop
at v = 52 and rows 53..127 are entirely free (19200 texels). Everything new is shelf-packed
at v >= 53, so no existing rect can move.

Authoring space is BLOCKBENCH project space: Y up, ground y=0, -Z = front (nose), -X = the
animal's LEFT. Coordinates are ABSOLUTE, matching what scripts/java2bb.py emits for the
existing rig, so both sets live in one coordinate system and can be swept together.

FOUR new horn families, all children of `head`, all hanging off the same pedicle spot the
existing gazelle horn uses (x +-1.05, y 38.0, z -13.6):

    GAZELLE  (existing)  horn_base + horn_tip            springbok, pronghorn(+prong)
    FORKED   (+2 boxes)  gazelle + a REAR prong          tetrameryx
    SPIRAL   (6 boxes)   3 chained, progressive yaw       giant_eland, blackbuck
    RAPIER   (4 boxes)   2 chained, near-vertical         gemsbok
    SCIMITAR (6 boxes)   3 chained, arcing back           sable, bluebuck

SEGMENT-TO-SEGMENT TWIST IS LOAD-BEARING, not decoration. A box's X-face normal is
untouched by rx, so a chain that only varies its BACKSWEEP leaves every segment's side
planes near-parallel and the sweep finds 1.0-1.4u2 at each joint. The shipped gazelle horn
does not flag at all, because base and tip differ by 20 degrees of rz. Consecutive segments
therefore need >8 degrees of ry or rz between them -- which is also exactly the twist that
makes a spiral read as a spiral rather than as a bent stick.

ROTATION SIGN, learned the hard way on the toxodon ears: in Blockbench space a POSITIVE rx
sends a point ABOVE the pivot toward +Z, i.e. BACKWARD. The shipped gazelle horn confirms
it -- horn_base carries rx +8 (swept back) and horn_tip rx -14 (hooking forward again),
which is exactly what a springbok horn does. Backswept scimitars therefore need POSITIVE
rx, not negative.

Run:  python scripts/antelope_horns_spec.py            full report
      python scripts/antelope_horns_spec.py --uv       UV pack (asserts v >= 53)
      python scripts/antelope_horns_spec.py --zfight   sweep NEW vs NEW and NEW vs EXISTING
      python scripts/antelope_horns_spec.py --json     compact JSON for the Blockbench builder
"""
import json
import math
import pathlib
import subprocess
import sys

import numpy as np

D = math.radians
HERE = pathlib.Path(__file__).resolve().parent

# ---------------------------------------------------------------- contract
# name, variant, horn family, build, real withers m
SPECIES = [
    ("saiga",       0, "NONE",     "light", 0.75),
    ("pronghorn",   1, "GAZELLE",  "light", 0.87),   # + the existing forward prong
    ("springbok",   2, "GAZELLE",  "light", 0.78),
    ("tetrameryx",  3, "FORKED",   "light", 0.85),
    ("blackbuck",   4, "SPIRAL",   "light", 0.79),
    ("giant_eland", 5, "SPIRAL",   "heavy", 1.60),   # + dewlap
    ("gemsbok",     6, "RAPIER",   "heavy", 1.20),
    ("sable",       7, "SCIMITAR", "heavy", 1.35),   # + neck mane
    ("bluebuck",    8, "SCIMITAR", "heavy", 1.05),
]
WITHERS_U = 26.44          # measured on the shipped rig; scale = withers_m * 16 / 26.44
HITBOX = (1.0, 1.2)
CANVAS = (256, 128)
V_FLOOR = 53               # nothing new may be packed above this row

PARTS = []                 # (name, parent, origin, rot_deg, from, to, cls)
BY_NAME = {}


def add(name, parent, origin, rot, a, b, cls):
    lo = [min(a[i], b[i]) for i in range(3)]
    hi = [max(a[i], b[i]) for i in range(3)]
    rec = (name, parent, tuple(origin), tuple(rot), tuple(lo), tuple(hi), cls)
    PARTS.append(rec)
    BY_NAME[name] = rec


# ---------------------------------------------------------------- existing rig
def load_existing():
    """The shipped 36 boxes, via java2bb, in the same absolute Blockbench space."""
    out = subprocess.run([sys.executable, str(HERE / "java2bb.py"), "ModelAntelope"],
                         capture_output=True, text=True, check=True).stdout
    return json.loads(out)["parts"]


EXISTING = load_existing()
for _p in EXISTING:
    BY_NAME[_p["n"]] = (_p["n"], _p["p"], tuple(_p["o"]), tuple(_p["r"]),
                        tuple(_p["f"]), tuple(_p["t"]), "existing")


# ================================================================ kinematics
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


def corners(p):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    return np.array([[x, y, z] for x in (a[0], b[0]) for y in (a[1], b[1])
                     for z in (a[2], b[2])])


# ================================================================ horn chains
PED = (1.05, 38.0, -13.6)      # the pedicle the shipped gazelle horn already uses


def ychain(fam, side, s, segs):
    """Chain stacked along +Y from the pedicle, each segment pivoting at its own base.

    segs: (suffix, base_y, top_y, halfw, z0, z1, rx, ry_s, rz_s)
    Each child's origin is the joint expressed in the PARENT'S UNROTATED frame -- the
    parent's own rotation carries it, so the joint can never crack open. Giving every
    segment the same pedicle origin instead is the cervid bug that left digitations
    floating 1.5-3.6u in mid air.
    """
    parent = "head"
    for suf, y0, y1, hw, z0, z1, rx, rys, rzs in segs:
        name = f"{fam}_{suf}_{side}"
        # rz SIGN: the shipped horn_base_left carries bb rz +8 and sits cleanly at world
        # x -2.30..-0.47. Authoring it as s*rzs gives -9 on the left and swings the horn
        # ACROSS the centreline to x +0.34 -- both horns end up leaning the same way, which
        # is the deer's "both antlers on one side of the skull" bug in a new costume.
        # Divergence needs -s, not s.
        add(name, parent,
            (s * PED[0], y0 + 0.8, PED[2]),
            (rx, s * rys, -s * rzs),
            (s * (PED[0] - hw), y0, z0), (s * (PED[0] + hw), y1, z1), fam)
        parent = name


for _side, _s in (("left", -1.0), ("right", 1.0)):
    # ---- SPIRAL: authored at GIANT ELAND -- long, open, swept back, progressively
    # yawed so the chain reads as a twist rather than a bent stick. Blackbuck reuses
    # these boxes with a tighter roll applied in setupAnim (a uniform scale can change
    # size but never shape, so the twist has to come from rotation, not scale).
    ychain("spiral", _side, _s, [
        # suf   y0    y1   hw     z0     z1    rx  ry_s rz_s
        ("1", 37.2, 43.6, 0.58, -14.35, -12.85, 12,  8,  12),
        ("2", 42.6, 48.4, 0.46, -14.20, -13.00,  8, 20,   6),
        ("3", 47.4, 52.6, 0.36, -14.10, -13.10,  6, 32,   5),
    ])
    # ---- RAPIER: gemsbok. Long and RULER-STRAIGHT is the whole identity, so only two
    # segments and almost no rotation between them.
    ychain("rapier", _side, _s, [
        ("1", 37.2, 47.0, 0.52, -14.30, -12.90,  6,  0,   7),
        # ry 12, not 0: rz on a CHILD is LOCAL, so 7 then -4 gives world 7 and world 3 --
        # only 4 degrees apart, still inside the near-parallel window, and the joint kept
        # flagging 0.89u2. A yaw is invisible on a ruler-straight horn and separates the
        # side planes properly.
        ("2", 46.2, 56.0, 0.40, -14.20, -13.00,  4, 12,  -4),
    ])
    # ---- SCIMITAR: authored at SABLE -- a strong rearward arc, cumulative rx
    # +16 -> +40 -> +70. Bluebuck reuses it shorter via setScale.
    ychain("scimitar", _side, _s, [
        ("1", 37.2, 43.4, 0.58, -14.35, -12.85, 16,  0,   9),
        ("2", 42.4, 48.2, 0.46, -14.20, -13.00, 24, 10,   5),
        ("3", 47.2, 52.4, 0.36, -14.10, -13.10, 30, 20,   4),
    ])
    # ---- FORKED: tetrameryx, the four-horned Pleistocene pronghorn. Reuses the whole
    # gazelle horn plus the existing forward prong, and adds a REAR prong so each horn
    # carries two. Yawed like the front prong so its x faces are never parallel to the
    # base's -- a spike buried in a thicker box shares a plane with it otherwise.
    add(f"horn_prong_rear_{_side}", f"horn_base_{_side}",
        (_s * 1.05, 40.6, -12.9), (-20, _s * 15, 0),
        (_s * 0.60, 39.7, -13.0), (_s * 1.50, 41.5, -10.6), "forked")

# ---- heavy-build extras ------------------------------------------------------
# Eland dewlap: hangs off the front of neck_base, its back face buried 0.8u inside it.
add("dewlap", "neck_base", (0.0, 26.0, -14.5), (8, 0, 0),
    (-1.6, 19.5, -16.0), (1.6, 27.0, -13.8), "extra")

# Sable neck mane. Modelled rather than painted DESPITE the cervid note that "a mane box
# was the equid's worst z-fight source" -- the sweep is the arbiter, not the anecdote, and
# this one sits on the neck's +Z (dorsal) face with 0.6u of inset and 1.2u of clearance.
# On a LEANING part the dorsal face is +Z, not the local top: the equid mane was put on the
# local top and ended up on the muzzle.
add("neck_mane", "neck", (0.0, 32.0, -9.0), (0, 0, 0),
    (-0.75, 29.2, -9.7), (0.75, 35.2, -7.9), "extra")

NEW = [p for p in PARTS]
FAMILY = {}
for _p in NEW:
    FAMILY.setdefault(_p[6], []).append(_p[0])


# ================================================================ UV pack
def footprint(p):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    w, h, d = (abs(b[i] - a[i]) for i in range(3))
    return int(math.ceil(2 * (w + d))), int(math.ceil(h + d))


def uvpack():
    """Shelf-pack the NEW boxes only, starting at V_FLOOR, and assert they never collide
    with an existing footprint."""
    W, H = CANVAS
    items = sorted(((footprint(p), p[0]) for p in NEW), key=lambda it: (-it[0][1], -it[0][0]))
    placed, x, y, shelf_h = {}, 0, V_FLOOR, 0
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
                raise AssertionError(f"NEW/NEW UV overlap: {names[i]} vs {names[j]}")

    # and against every shipped rect
    for pe in EXISTING:
        a, b = np.asarray(pe["f"], float), np.asarray(pe["t"], float)
        w, h, d = (abs(b[i] - a[i]) for i in range(3))
        eu, ev = pe["uv"]
        ew, eh = 2 * (w + d), h + d
        for nm, (nx, ny, nw, nh) in placed.items():
            if nx < eu + ew and eu < nx + nw and ny < ev + eh and ev < ny + nh:
                raise AssertionError(f"NEW rect {nm} collides with SHIPPED rect {pe['n']}")

    print("=" * 78)
    print(f"UV PACK for {len(placed)} NEW boxes on {W}x{H}, floor v={V_FLOOR}")
    print("=" * 78)
    for p in NEW:
        u, v, fw, fh = placed[p[0]]
        print(f"  {p[0]:<26} texOffs({u:3d},{v:3d})   {fw:3d}x{fh:<3d}")
    assert used <= H, f"does not fit: needs {used} rows of {H}"
    print(f"  rows {V_FLOOR}..{used} used;  zero NEW/NEW and zero NEW/SHIPPED collisions")
    return placed


def emit_json():
    import io
    from contextlib import redirect_stdout
    with redirect_stdout(io.StringIO()):
        placed = uvpack()
    out = list(EXISTING)
    for name, parent, o, r, a, b, cls in NEW:
        u, v, _, _ = placed[name]
        out.append({"n": name, "p": parent, "o": [round(x, 4) for x in o],
                    "r": [round(x, 4) for x in r], "f": [round(x, 4) for x in a],
                    "t": [round(x, 4) for x in b], "uv": [u, v], "c": cls})
    print(json.dumps({"canvas": list(CANVAS), "parts": out}, separators=(",", ":")))


# ================================================================ z-fight
def zfight():
    """Sweep the new boxes against each other AND against the shipped rig.

    Families that never render together are skipped: no species draws two horn families,
    so a shared face between a spiral and a scimitar is not a defect.
    """
    sys.path.insert(0, str(HERE))
    import zfight_sweep as Z

    allp = {p["n"]: (p["n"], p["p"], tuple(p["o"]), tuple(p["r"]),
                     tuple(p["f"]), tuple(p["t"]), "existing") for p in EXISTING}
    for p in NEW:
        allp[p[0]] = p

    parts = {}
    for name, (nm, parent, o, r, a, b, cls) in allp.items():
        if abs(a[0] - b[0]) < 1e-9:
            continue                       # zero-width eye planes
        q = Z.Part(name)
        po = np.asarray(allp[parent][2], float) if parent else np.zeros(3)
        q.rp = np.asarray(o, float) - po
        q.rot = np.array([D(r[0]), D(r[1]), D(r[2])])
        q.lo = np.asarray(a, float) - np.asarray(o, float)
        q.hi = np.asarray(b, float) - np.asarray(o, float)
        q.parent = parent
        parts[name] = q

    fam = {}
    for cls, names in FAMILY.items():
        if cls in ("spiral", "rapier", "scimitar"):
            for n in names:
                fam[n] = cls
    # the shipped gazelle horn is its own family too
    for n in ("horn_base_left", "horn_base_right", "horn_tip_left", "horn_tip_right",
              "horn_prong_left", "horn_prong_right"):
        fam[n] = "gazelle"
    for n in ("horn_prong_rear_left", "horn_prong_rear_right"):
        fam[n] = "gazelle"                 # FORKED renders alongside the gazelle horn

    newnames = {p[0] for p in NEW}
    print("=" * 78)
    print("Z-FIGHT SWEEP -- NEW vs NEW and NEW vs SHIPPED (>=0.5u2 coincident face)")
    print("=" * 78)
    shown = 0
    for area, na, nb, fa, fb, rel in Z.sweep(parts):
        if na not in newnames and nb not in newnames:
            continue                       # shipped-vs-shipped is not this change's problem
        if fam.get(na) and fam.get(nb) and fam[na] != fam[nb]:
            continue                       # two families never render together
        shown += 1
        print(f"  {area:6.2f}u2  {na:<24} {'XYZ'[fa[0]]}  <->  {nb:<24} "
              f"{'XYZ'[fb[0]]}{rel}")
    if not shown:
        print("  clean")


# ================================================================ report
def main():
    print("=" * 78)
    print(f"SPECIES / SCALE   scale = withers_m * 16 / {WITHERS_U}")
    print("=" * 78)
    print(f"{'species':<13}{'var':>4}{'family':>10}{'build':>7}{'withers m':>11}{'scale':>8}")
    for name, var, famname, build, real in SPECIES:
        print(f"{name:<13}{var:>4}{famname:>10}{build:>7}{real:>11.2f}"
              f"{real * 16.0 / WITHERS_U:>8.4f}")
    print(f"registered hitbox {HITBOX[0]} x {HITBOX[1]} blocks -- giant_eland (1.60) and "
          f"sable (1.35)\n  render taller than it; accepted and documented, as the dwarf "
          f"mammoths ship.")

    print("\n" + "=" * 78)
    print("HORN GEOMETRY  (world AABB of one side, and the tip reached)")
    print("=" * 78)
    for famname in ("spiral", "rapier", "scimitar"):
        segs = [n for n in FAMILY[famname] if n.endswith("_left")]
        pts = np.vstack([world(n, corners(BY_NAME[n])) for n in segs])
        tipn = segs[-1]
        tip = world(tipn, corners(BY_NAME[tipn]))
        skull_top = world("head", corners(BY_NAME["head"]))[:, 1].max()
        print(f"  {famname.upper():<9} x {pts[:,0].min():6.2f}..{pts[:,0].max():<6.2f} "
              f"y {pts[:,1].min():6.2f}..{pts[:,1].max():<6.2f} "
              f"z {pts[:,2].min():6.2f}..{pts[:,2].max():<6.2f}")
        print(f"  {'':9} rises {pts[:,1].max() - skull_top:5.2f}u above the skull, "
              f"tip centre z {tip[:,2].mean():6.2f}  (skull top y {skull_top:.2f})")
    for n in ("horn_prong_rear_left", "dewlap", "neck_mane"):
        w = world(n, corners(BY_NAME[n]))
        print(f"  {n:<20} x {w[:,0].min():6.2f}..{w[:,0].max():<6.2f} "
              f"y {w[:,1].min():6.2f}..{w[:,1].max():<6.2f} "
              f"z {w[:,2].min():6.2f}..{w[:,2].max():<6.2f}")

    tot = sum(fw * fh for fw, fh in (footprint(p) for p in NEW))
    print(f"\n{len(NEW)} new boxes, {tot} px of new footprint; "
          f"{(CANVAS[1] - V_FLOOR) * CANVAS[0]} px free below v={V_FLOOR}")


if __name__ == "__main__":
    if "--zfight" in sys.argv:
        zfight()
    elif "--uv" in sys.argv:
        uvpack()
    elif "--json" in sys.argv:
        emit_json()
    else:
        main()
