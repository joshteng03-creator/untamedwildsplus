#!/usr/bin/env python3
"""Diprotodont model spec + forward-kinematics checker -- SOP Phase 1 / 1b / 2.

A BRAND-NEW entity type (Phase 4.1, user-approved 2026-10-09). Forked from tapir_spec.py's
tooling (kinematics, UV pack, z-fight/burial checks, pose solve); the body is authored from
scratch.

Authoring space is BLOCKBENCH project space: Y up, ground y=0, -Z = front (nose), -X = left.
Group origins and cube from/to are ABSOLUTE project coordinates, exactly as Blockbench stores
them, so this table doubles as the data for the build script.

    world(p) = A_root(A_child(... p))      A_g(p) = R_g @ (p - o_g) + o_g

Rotation order is Rz @ Ry @ Rx. A POSITIVE rx raises a part's FRONT (-Z) end.

DIPROTODON SILHOUETTE -- a wombat the size of a rhinoceros:
  * a BROAD barrel body, as wide as it is deep, with a near-level back that rises a little
    over the shoulders and falls slightly to a rounded rump;
  * PILLAR legs, straight and columnar, belly at ~40% of the withers -- longer-legged than a
    wombat, shorter than a rhino;
  * PIGEON-TOED: broad plantigrade feet turned INWARD, fore and hind. This is the one cue no
    other rig in the mod has, and the thing that stops it reading as a rhino or a tapir;
  * a BIG blocky head carried low on a short thick neck, with wide cheeks (the flared
    zygomatic arches), a deep lower jaw, a broad fleshy nose pad and short forward-pointing
    incisors; small eyes set high; small rounded ears;
  * a stub tail.

FIVE species off one mesh (scales by shoulder height, 1 block = 1 m):
  * NASAL_BOSS is showModel-gated ON for zygomaturus alone (its raised nasal area);
  * the JOEY (a baby's head peeking from the REAR-facing pouch, as in wombats) is gated at
    runtime on a mother with a baby nearby -- never per species.

Run:  python scripts/diprotodont_spec.py            full report
      python scripts/diprotodont_spec.py --uv       UV shelf pack only
      python scripts/diprotodont_spec.py --zfight   z-fight sweep on the spec
      python scripts/diprotodont_spec.py --burial   volume-burial check
      python scripts/diprotodont_spec.py --pose     phase-7 sit/sleep solve
      python scripts/diprotodont_spec.py --json     compact JSON for the BB builder
"""
import math
import numpy as np

D = math.radians

# ---------------------------------------------------------------- contract
SPECIES = [
    # name,           variant, real shoulder height m, nasal boss?
    ("diprotodon",     0, 1.80, False),   # Diprotodon optatum -- the giant, ~2.7 t
    ("zygomaturus",    1, 1.10, True),    # Zygomaturus trilobus -- bull-sized, raised nasal area
    ("nototherium",    2, 1.20, False),   # Nototherium mitchelli -- stocky, short-faced
    ("hulitherium",    3, 0.80, False),   # Hulitherium tomasettii -- New Guinea, panda-like
    ("maokopia",       4, 0.60, False),   # Maokopia ronaldi -- smallest, New Guinea montane
]
HITBOX = (2.0, 2.0)      # proposed ModEntity.createEntity(EntityDiprotodont::new, "diprotodont", 2.0F, 2.0F, ..)

PARTS = []                    # (name, parent, origin, rot_deg, from, to, cls)
BY_NAME = {}


def add(name, parent, origin, rot, a, b, cls):
    lo = [min(a[i], b[i]) for i in range(3)]
    hi = [max(a[i], b[i]) for i in range(3)]
    rec = (name, parent, tuple(origin), tuple(rot), tuple(lo), tuple(hi), cls)
    PARTS.append(rec)
    BY_NAME[name] = rec


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


def local_of(name, pts):
    p = np.asarray(pts, float).reshape(-1, 3)
    for _, _, o, r, *_ in reversed(ancestry(name)):
        o = np.asarray(o, float)
        p = (rmat(*r).T @ (p - o).T).T + o
    return p


def corners(p):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    return np.array([[x, y, z] for x in (a[0], b[0]) for y in (a[1], b[1])
                     for z in (a[2], b[2])])


def vol(p, n=7):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    ax = [np.linspace(a[i], b[i], n) if b[i] - a[i] > 1e-6 else np.array([a[i]])
          for i in range(3)]
    return np.stack(np.meshgrid(*ax, indexing="ij"), -1).reshape(-1, 3)


# ================================================================ TORSO
# Four nested masses. Side planes step inboard from the barrel, the widest thing on the
# animal -- but only a little: a diprotodon is BROAD all the way along, not a wedge like the
# tapir.   barrel 8.8 > rump 8.4 > shoulders 8.2 > chest 7.4
add("body_barrel", None, (0, 16.0, 3.0), (0, 0, 0),
    (-8.8, 9.5, -8.0), (8.8, 22.6, 13.0), "body")
# The shoulder rise: a tall box tilted up at the front, so only its top emerges above the
# barrel as one continuous line (SOP massing rule: the topline box is TALL and nested).
add("body_shoulders", "body_barrel", (0, 16.0, -6.0), (6, 0, 0),
    (-8.2, 11.0, -14.0), (8.2, 23.4, 0.0), "body")
# Deep chest carrying the forelegs, tilted nose-down. Floor at 8.4, a deep brisket: at 9.2 the
# tilt brought its rear floor to 9.65 against the barrel floor at 9.5 and the sweep found 24u2.
add("body_chest", "body_barrel", (0, 15.0, -11.0), (-6, 0, 0),
    (-7.4, 8.4, -16.5), (7.4, 21.4, -7.0), "body")
# Rounded rump, origin at its own FRONT face so the tilt only drops the rear.
add("body_rump", "body_barrel", (0, 16.0, 9.5), (12, 0, 0),
    (-8.4, 10.5, 9.5), (8.4, 22.2, 18.5), "body")

TILT = {"body_chest": -6.0, "body_rump": 12.0}


# ================================================================ chain helpers
def zchain(parent, pivot, segs, cls, parent_tilt=0.0):
    """Chain authored lying forward along -Z from `pivot`; bent by WORLD angles.

    A POSITIVE angle raises the forward end. EVERY CHILD OF A SEGMENT MUST BE AUTHORED IN
    THIS REST FRAME, not at its apparent world position -- the chain is laid out colinear
    along -Z and only then rotated.
    """
    px, py, pz = pivot
    prev_ang = parent_tilt
    par = parent
    z = pz
    for name, ang, L, hw, up, dn, ov in segs:
        add(name, par, (px, py, z), (ang - prev_ang, 0, 0),
            (px - hw, py - dn, z - L), (px + hw, py + up, z + ov), cls)
        par, prev_ang, z = name, ang, z - L
    return par


def leg(prefix, parent, pivot_z, segs, parent_tilt, toe_in=0.0, _resolve=0.0):
    """Leg authored hanging straight down from a pivot whose HEIGHT IS SOLVED.

    The sole lands on y = 0 when the pivot's WORLD height equals sum(L*cos(angle)); the
    AUTHORED height is not that number, because the parent's own tilt drags the pivot off
    it. `toe_in` yaws the LAST segment (the foot) inward by that many degrees -- the
    pigeon-toed stance. A yaw about the foot's own vertical axis does not change its sole
    height, so the ground solve is unaffected.
    """
    drop = sum(L * math.cos(D(a)) for _, a, L, *_ in segs) + _resolve
    y_auth = drop
    for _ in range(12):
        y_auth += drop - world(parent, (0.0, y_auth, pivot_z))[0][1]
    for side, s in (("left", -1.0), ("right", 1.0)):
        par, prev_ang, y = parent, parent_tilt, y_auth
        for k, (name, ang, L, outer, inner, fr, bk, ov) in enumerate(segs):
            # R_y sends the toe (-Z) toward -x for a positive angle, so the LEFT foot (at -x)
            # turns IN under a NEGATIVE yaw and the right under a positive one.
            yaw = (s * toe_in) if k == len(segs) - 1 else 0.0
            add(f"{prefix}_{side}_{name}", par, (s * (outer + inner) / 2, y, pivot_z),
                (ang - prev_ang, yaw, 0),
                (s * outer, y - L, pivot_z - fr), (s * inner, y + ov, pivot_z + bk), "leg")
            par, prev_ang, y = f"{prefix}_{side}_{name}", ang, y - L
    # The toe-in yaw sits between two tilted frames, so the foot picks up a slight ROLL and
    # its sole corners dip (0.06-0.14u through the floor on the first pass). Measure the
    # real sole and re-solve the pivot height once with that error removed.
    if toe_in and not _resolve:
        foot = f"{prefix}_left_{segs[-1][0]}"
        low = world(foot, corners(BY_NAME[foot]))[:, 1].min()
        names = {f"{prefix}_{sd}_{sg[0]}" for sd in ("left", "right") for sg in segs}
        PARTS[:] = [q for q in PARTS if q[0] not in names]
        for n in names:
            del BY_NAME[n]
        return leg(prefix, parent, pivot_z, segs, parent_tilt, toe_in, _resolve=-low)
    return drop


# ================================================================ NECK + HEAD
# A short thick neck and a BIG head carried low. Rest-frame anchors for anything parented
# into this chain (author children HERE, not at their apparent world position):
#     neck        z -19.5..-13.0      head_skull  z -27.0..-17.5
#     head_muzzle z -31.2..-25.4      all about y 17.0
# Length budget: a real Diprotodon is ~2.1x its shoulder height nose to rump. The first pass
# (neck 4.5 / skull 8.5 / muzzle 5.0 on a 39u trunk) came out at 2.56x.
zchain("body_chest", (0, 17.0, -16.0), [
    #  name           ang    L    hw   up   dn   ov
    ("neck",          -8,  3.5, 6.0, 4.0, 4.6, 3.0),
    ("head_skull",   -14,  7.5, 5.4, 4.2, 4.0, 2.0),
    ("head_muzzle",  -22,  4.2, 3.9, 3.0, 3.0, 1.6),
], "head", parent_tilt=TILT["body_chest"])

# Broad fleshy nose pad, proud of the muzzle front.
add("nose", "head_muzzle", (0, 17.6, -30.8), (0, 0, 0),
    (-3.2, 16.2, -32.6), (3.2, 19.4, -30.2), "nose")
# Zygomaturus' raised nasal area -- showModel-gated to that species alone. Base buried in
# the muzzle's top so no gap opens when the head moves.
add("nasal_boss", "head_muzzle", (0, 19.6, -28.3), (0, 0, 0),
    (-2.4, 19.3, -30.0), (2.4, 20.9, -26.4), "boss")

# Deep lower jaw. Child of head_skull, NOT head_muzzle, so it stays one rigid bone. Its
# front stops well behind the muzzle's so the two near-parallel front planes never meet.
add("head_jaw", "head_skull", (0, 14.0, -21.0), (0, 0, 0),
    (-4.2, 10.8, -29.0), (4.2, 14.6, -18.5), "head")
# Short forward-pointing lower incisors at the jaw tip.
add("incisors", "head_jaw", (0, 13.6, -29.0), (0, 0, 0),
    (-1.2, 12.8, -31.2), (1.2, 14.2, -28.6), "tooth")

# Small rounded ears, set high and back, swept up and out. rz SIGN: a tip authored at -x
# (the LEFT ear) only rises under a NEGATIVE rz.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"ear_{side}", "head_skull", (s * 3.8, 20.6, -19.6), (6, s * -14, s * 34),
        (s * 3.6, 19.8, -20.6), (s * 6.4, 22.2, -18.6), "ear")

# Eyes: whole-unit 2.0 x 2.0 zero-width planes, set HIGH on the skull and 0.4 outside it.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"eye_{side}", "head_skull", (s * 5.8, 19.2, -23.5), (0, 0, 0),
        (s * 5.8, 18.2, -24.5), (s * 5.8, 20.2, -22.5), "eye")

# ================================================================ TAIL
# A stub. Authored as an explicit box straddling its pivot with a NEGATIVE rx, never via
# zchain (see tapir_spec: a zchain tail sweeps through the rump and gets buried).
add("tail", "body_rump", (0, 18.0, 18.0), (-30, 0, 0),
    (-1.8, 15.0, 17.4), (1.8, 19.4, 21.0), "tail")

# ================================================================ JOEY
# A baby's head peeking out of the pouch. Diprotodontid pouches, like a wombat's, open to
# the REAR, so the joey hangs under the back of the belly looking backward. Gated at
# runtime only (mother with a baby nearby).
add("joey_head", "body_barrel", (0, 9.4, 9.0), (0, 0, 0),
    (-1.7, 7.2, 8.6), (1.7, 10.4, 12.2), "joey")
add("joey_snout", "joey_head", (0, 8.4, 12.0), (0, 0, 0),
    (-1.0, 7.4, 11.8), (1.0, 9.2, 13.6), "joey")

# ================================================================ LEGS
# PILLARS: straight, near-vertical segments. Lateral planes step so no two overlapping
# boxes share a plane:
#   fore  outer 9.8 / 9.0 / 9.6   inner 2.6 / 3.0 / 2.8
#   hind  outer 10.0 / 9.0 / 9.6  inner 2.2 / 2.8 / 2.5
# Both limb roots sit PROUD of the barrel (9.8/10.0 vs 8.8). Thickened after the first grey
# render: at ~5u across the lower legs read as sticks under that body, not pillars. The feet are broad and long and
# turned in (toe_in): the pigeon-toed diprotodont stance.
FORE_PIVOT_Z = -10.0         # also where the withers height is measured (see main())
FORE_DROP = leg("fore", "body_chest", FORE_PIVOT_Z, [
    #  name        ang   L    outer inner  fr   bk   ov
    ("upper",        6, 6.0, 9.8, 2.6, 3.8, 3.6, 1.4),
    ("forearm",     -4, 5.4, 9.0, 3.0, 3.6, 3.2, 2.0),   # ov 2.0: top inside the chest (at 1.0 it met the chest floor, 2.5u2)
    ("foot",         0, 2.2, 9.6, 2.8, 4.8, 2.4, 0.8),
], TILT["body_chest"], toe_in=16.0)

HIND_DROP = leg("hind", "body_rump", 14.0, [
    ("thigh",       14, 6.4, 10.0, 2.2, 3.8, 4.0, 1.6),
    ("shank",      -12, 5.0, 9.0, 2.8, 3.4, 3.2, 1.0),
    ("foot",         0, 2.0, 9.6, 2.5, 5.0, 2.2, 0.8),
], TILT["body_rump"], toe_in=12.0)

# ================================================================ UV pack
CANVAS = (256, 128)


def footprint(p):
    """The region MC reserves from texOffs: 2*(w+d) x (h+d), in texels (CEIL, see tapir_spec)."""
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    w, h, d = (abs(b[i] - a[i]) for i in range(3))
    if p[6] == "eye":
        w = 0.0
    return int(math.ceil(2 * (w + d))), int(math.ceil(h + d))


def uvpack():
    """Shelf-pack every box's true footprint, then ASSERT zero overlap. Every part gets its OWN
    region -- no mirror_uv sharing (the eyes' anti-flicker fix clears OPPOSITE halves)."""
    W, H = CANVAS
    items = sorted(((footprint(p), p[0]) for p in PARTS),
                   key=lambda it: (-it[0][1], -it[0][0]))
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

    print("=" * 80)
    print(f"UV SHELF PACK on {W}x{H}   ({len(placed)} boxes, {used} rows used)")
    print("=" * 80)
    assert used <= H, f"does not fit: needs {used} rows of {H}"
    for p in PARTS:
        u, v, fw, fh = placed[p[0]]
        print(f"  {p[0]:<24} texOffs({u:3d},{v:3d})   footprint {fw:3d}x{fh:<3d}")
    print(f"  zero overlaps asserted;  {used}/{H} rows used")
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
        out.append({"n": name, "p": parent, "o": [round(x, 4) for x in o],
                    "r": [round(x, 4) for x in r], "f": [round(x, 4) for x in a],
                    "t": [round(x, 4) for x in b], "uv": [u, v], "c": cls})
    print(json.dumps({"canvas": list(CANVAS), "parts": out}, separators=(",", ":")))


# ================================================================ z-fight sweep
def zfight():
    import zfight_sweep as Z

    parts = {}
    for name, parent, o, r, a, b, cls in PARTS:
        if cls == "eye":
            continue          # zero-width planes: handled by the alpha-0 UV trick
        p = Z.Part(name)
        po = np.asarray(BY_NAME[parent][2], float) if parent else np.zeros(3)
        p.rp = np.asarray(o, float) - po
        p.rot = np.array([D(r[0]), D(r[1]), D(r[2])])
        p.lo = np.asarray(a, float) - np.asarray(o, float)
        p.hi = np.asarray(b, float) - np.asarray(o, float)
        p.parent = parent
        parts[name] = p

    print("=" * 80)
    print("Z-FIGHT SWEEP ON THE SPEC   (>=0.5u2 of genuinely coincident face)")
    print("=" * 80)
    shown = 0
    for area, na, nb, fa, fb, rel in Z.sweep(parts):
        shown += 1
        print(f"  {area:6.2f}u2  {na:<22} {'XYZ'[fa[0]]}  <->  {nb:<22} "
              f"{'XYZ'[fb[0]]}{rel}")
    if not shown:
        print("  clean")


# ================================================================ burial check
def burial():
    """Fraction of each part's VOLUME inside another box (the z-fight sweep is blind to a
    part swallowed whole). Nested core masses reading high are intended; FEATURES are not."""
    print("=" * 80)
    print("VOLUME BURIAL   (fraction of each part's volume inside some OTHER box)")
    print("=" * 80)
    solids = [p for p in PARTS if p[6] != "eye"]
    worst = []
    for p in solids:
        pts = world(p[0], vol(p, 9))
        inside = np.zeros(len(pts), bool)
        for q in solids:
            if q[0] == p[0] or (q[6] == "joey") != (p[6] == "joey") and q[6] == "joey":
                continue
            l = local_of(q[0], pts)
            a, b = np.asarray(q[4], float), np.asarray(q[5], float)
            inside |= np.all((l >= a - 1e-6) & (l <= b + 1e-6), axis=1)
        worst.append((inside.mean(), p[0], p[6]))
    for frac, name, cls in sorted(worst, reverse=True):
        flag = ""
        if frac > 0.55:
            flag = ("   <== FEATURE MOSTLY BURIED"
                    if cls in ("boss", "tooth", "tail", "nose", "ear", "joey")
                    else "   <== mostly buried (nested core mass, expected)")
        print(f"  {frac:6.1%}  {name}{flag}")


# ================================================================ report
def main():
    wc = {p[0]: world(p[0], corners(p)) for p in PARTS}
    lo = {k: v.min(0) for k, v in wc.items()}
    hi = {k: v.max(0) for k, v in wc.items()}

    print("=" * 80)
    print("WORLD AABBs   (Blockbench space: y up, ground 0, -Z = nose, -X = left)")
    print("=" * 80)
    for p in PARTS:
        n = p[0]
        print(f"{n:<22}{lo[n][0]:7.2f}..{hi[n][0]:<7.2f}"
              f"{lo[n][1]:7.2f}..{hi[n][1]:<7.2f}{lo[n][2]:7.2f}..{hi[n][2]:<7.2f}")

    print("\n" + "=" * 80)
    print("GROUND CONTACT + PROPORTION")
    print("=" * 80)
    print(f"fore pivot height {FORE_DROP:6.2f}   hind pivot height {HIND_DROP:6.2f}")
    for n in sorted(wc):
        if n.endswith("_foot"):
            print(f"  {n:<24} sole y {lo[n][1]:+6.3f}   z {lo[n][2]:6.2f}..{hi[n][2]:.2f}")
    floor = min(v[1] for v in lo.values())
    print(f"  lowest point of the whole model: {floor:+.3f}"
          + ("   OK" if floor > -0.02 else "   <== THROUGH THE FLOOR"))

    # WITHERS = the topline height ABOVE THE FORELEG, never the box's world-AABB max.
    wp = BY_NAME["body_shoulders"]
    withers = world("body_shoulders", [(0.0, wp[5][1], FORE_PIVOT_Z)])[0][1]
    rp = BY_NAME["body_rump"]
    rump = world("body_rump", [(0.0, rp[5][1], rp[5][2])])[0][1]
    belly = lo["body_barrel"][1]
    stance = ((lo["hind_left_foot"][2] + hi["hind_left_foot"][2]) / 2
              - (lo["fore_left_foot"][2] + hi["fore_left_foot"][2]) / 2)
    print(f"  fore->hind stance {stance:.2f}u = {stance/withers:.2f} x withers")
    print(f"  withers {withers:.2f}u   barrel top {hi['body_barrel'][1]:.2f}u   rump rear {rump:.2f}u")
    print(f"  belly {belly:.2f}u = {belly/withers:.0%} of withers   (pillar-legged diprotodont ~38-42%)")
    head_top = hi["head_skull"][1]
    print(f"  head top {head_top:.2f}u ({head_top/withers:.0%} of withers -- carried LOW, at or below the back)")

    print("\n" + "=" * 80)
    print("BREADTH   (a diprotodont is broad all the way along -- NOT a wedge)")
    print("=" * 80)
    for n in ("body_chest", "body_shoulders", "body_rump", "body_barrel"):
        print(f"  {n:<16} half-width {abs(BY_NAME[n][5][0]):5.2f}")
    print(f"  barrel width / depth = {2*8.8/(22.6-9.5):.2f}   (wombat-like ~1.2-1.4)")
    print(f"  nose-rump / withers = {(hi['body_rump'][2]-min(lo['nose'][2], lo['incisors'][2]))/withers:.2f}   (real Diprotodon ~2.1)")

    print("\n" + "=" * 80)
    print("EYE CLEARANCE  (must sit outside every ancestor box, not just the skull)")
    print("=" * 80)
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head_skull", "neck", "body_chest", "head_jaw", "head_muzzle", f"ear_{side}"):
            p = BY_NAME[anc]
            l = local_of(anc, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gaps = [max(a[i] - l[i], l[i] - b[i]) for i in range(3)]
            gap = max(gaps)
            print(f"  eye_{side:<6} vs {anc:<12} clear by {gap:6.2f} on "
                  f"{'xyz'[int(np.argmax(gaps))]}"
                  + ("" if gap > 0.3 else "   <== TOO CLOSE"))

    print("\n" + "=" * 80)
    print(f"SIZE CALIBRATION   mesh withers = {withers:.2f}u,  scale = m*16/withers")
    print("=" * 80)
    nose = min(lo["nose"][2], lo["head_muzzle"][2], lo["incisors"][2])
    rear = hi["body_rump"][2]
    width = 2 * max(abs(v[0]) for v in list(lo.values()) + list(hi.values()))
    top = max(v[1] for v in hi.values())
    print(f"{'species':<14}{'m':>6}{'scale':>8}{'withers':>9}{'top':>7}"
          f"{'nose-rump':>11}{'width':>8}  flags")
    for name, var, real, boss in SPECIES:
        s = real * 16.0 / withers
        print(f"{name:<14}{real:6.2f}{s:8.3f}{withers*s/16:9.2f}{top*s/16:7.2f}"
              f"{(rear-nose)*s/16:11.2f}{width*s/16:8.2f}  {'nasal_boss' if boss else ''}")
    print(f"proposed hitbox {HITBOX[0]} x {HITBOX[1]} blocks"
          "   (per-TYPE, so the biggest species should sit close to it)")

    pts = np.vstack([world(p[0], vol(p, 15)) for p in PARTS
                     if p[6] not in ("eye", "joey", "boss")])
    print("\n" + "=" * 80)
    print("SIDE SILHOUETTE  (joey + nasal boss hidden)      <- nose      tail ->")
    print("=" * 80)
    zmin, zmax, ymax = pts[:, 2].min(), pts[:, 2].max(), pts[:, 1].max()
    W, H = 76, 24
    g = [[" "] * W for _ in range(H)]
    for _, y, z in pts:
        c = int((z - zmin) / (zmax - zmin) * (W - 1))
        r = int((ymax - y) / ymax * (H - 1))
        if 0 <= r < H:
            g[r][c] = "#"
    for r, row in enumerate(g):
        print(f"{ymax - r*ymax/(H-1):5.1f} |" + "".join(row))
    print(f"      z {zmin:.1f} .. {zmax:.1f}")

    print("\n" + "=" * 80)
    print("FRONT SILHOUETTE  (looking at the nose; the pigeon-toed feet show here)")
    print("=" * 80)
    xmin, xmax = pts[:, 0].min(), pts[:, 0].max()
    W2 = 44
    g = [[" "] * W2 for _ in range(H)]
    for x, y, _ in pts:
        c = int((x - xmin) / (xmax - xmin) * (W2 - 1))
        r = int((ymax - y) / ymax * (H - 1))
        if 0 <= r < H:
            g[r][c] = "#"
    for r, row in enumerate(g):
        print(f"{ymax - r*ymax/(H-1):5.1f} |" + "".join(row))

    print("\n" + "=" * 80)
    print("UV FOOTPRINT BUDGET   (MC reserves 2*(w+d) x (h+d) from every texOffs)")
    print("=" * 80)
    tot = sum(fw * fh for fw, fh in (footprint(p) for p in PARTS))
    print(f"{len(PARTS)} boxes, {tot} px")
    for cw, ch in ((128, 64), (256, 128), (256, 256)):
        print(f"  {cw}x{ch} = {cw*ch:>6} px -> "
              f"{'FITS' if tot < cw*ch*0.72 else 'TOO TIGHT'} ({100*tot/(cw*ch):.0f}% raw)")


# ================================================================ phase 7 poses
def world_mod(name, pts, rot_over=None, dy=0.0):
    p = np.asarray(pts, float).reshape(-1, 3)
    for nm, _, o, r, *_ in ancestry(name):
        r = (rot_over or {}).get(nm, r)
        o = np.asarray(o, float)
        p = (rmat(*r) @ (p - o).T).T + o
    p = p.copy()
    p[:, 1] += dy
    return p


LEGS = {
    "fore": ("body_chest", TILT["body_chest"], [("upper", 6.0), ("forearm", 5.4), ("foot", 2.2)]),
    "hind": ("body_rump", TILT["body_rump"], [("thigh", 6.4), ("shank", 5.0), ("foot", 2.0)]),
}


def _leg_rot(prefix, side, angles):
    _, tilt, segs = LEGS[prefix]
    over, prev = {}, tilt
    for (nm, _), a in zip(segs, angles):
        full = f"{prefix}_{side}_{nm}"
        over[full] = (a - prev, BY_NAME[full][3][1], 0.0)    # keep the authored toe-in yaw
        prev = a
    return over


def _leg_score(prefix, angles, dy):
    over = _leg_rot(prefix, "left", angles)
    pen = 0.0
    _, _, segs = LEGS[prefix]
    for k, (nm, _) in enumerate(segs):
        full = f"{prefix}_left_{nm}"
        w = world_mod(full, vol(BY_NAME[full]), over, dy)
        low = w[:, 1].min()
        if low < 0.0:
            pen += (-low) ** 2 * 12.0                     # never through the floor
        if k >= 1:                                        # forearm/shank + foot lie ON it
            pen += (low - 0.12) ** 2 * 3.0
    for a in angles[1:]:
        pen += (math.cos(D(a)) ** 2) * 4.0
    return pen


def pose_solve():
    """SOP phase 7: SOLVE sternal recumbency against the real box geometry, never hand-guess."""
    rng = np.random.default_rng(7)
    print("=" * 80)
    print("PHASE 7 -- POSES SOLVED AGAINST REAL BOX GEOMETRY")
    print("=" * 80)
    body_lo = world("body_barrel", corners(BY_NAME["body_barrel"]))[:, 1].min()
    out = {}
    for pose, belly in (("sit", 1.6), ("sleep", 0.8)):
        dy = belly - body_lo
        print(f"\n{pose.upper()}   body drop dy = {dy:+.2f}u  (belly rests at y={belly})")
        for prefix in ("fore", "hind"):
            base = np.array([40.0, 95.0, 95.0] if prefix == "fore" else [70.0, -95.0, -95.0])
            best, bs = base.copy(), _leg_score(prefix, base, dy)
            span = np.array([55.0, 45.0, 45.0])
            for _ in range(9):
                cand = best + rng.normal(0, 1, (4000, 3)) * span
                cand[:, 2] = cand[:, 1] + rng.normal(0, 6, 4000)
                for c in cand:
                    s = _leg_score(prefix, c, dy)
                    if s < bs:
                        bs, best = s, c.copy()
                span *= 0.62
            out[(pose, prefix)] = (best, dy)
            print(f"  {prefix:<5} world angles " +
                  " ".join(f"{a:7.2f}" for a in best) + f"   penalty {bs:.4f}")
            over = _leg_rot(prefix, "left", best)
            for nm, rot in over.items():
                w = world_mod(nm, vol(BY_NAME[nm]), over, dy)
                print(f"      {nm:<24} localRotX {rot[0]:+8.3f} deg"
                      f"   y {w[:,1].min():6.2f}..{w[:,1].max():6.2f}")
    return out


if __name__ == "__main__":
    import sys
    if "--zfight" in sys.argv:
        zfight()
    elif "--uv" in sys.argv:
        uvpack()
    elif "--json" in sys.argv:
        emit_json()
    elif "--pose" in sys.argv:
        pose_solve()
    elif "--burial" in sys.argv:
        burial()
    else:
        main()
