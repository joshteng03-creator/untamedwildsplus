#!/usr/bin/env python3
"""Dire wolf (Aenocyon dirus) model spec + FK checker -- SOP Phase 1 / 1b / 2.

Authoring space is BLOCKBENCH project space: Y up, ground y=0, -Z = front (nose),
-X = left.  Group origins and cube from/to are ABSOLUTE project coordinates, exactly as
Blockbench stores them, so this table doubles as the data for the build script.

Replaces the 19-box "vanilla wolf" rig, which read derpy for seven measurable reasons:
a 7x7x5.5 CUBE skull (canid skulls are ~0.55 as wide as long), an 8x8x8 ruff wider AND
taller than the 7x7 torso it sat on, a 4x4 square blunt muzzle, two equal 4u leg
segments with no paws and no fore/hind differentiation, a single 7x7x11 torso slab with
no chest depth and no tuck-up, 1u-thick paper ears, and a straight 3x3x8 tail board.

The rig keeps EVERY part name ModelDireWolf's animations touch -- body_main, head_neck,
head_main, head_snout, head_jaw, ear_*, eye_*, hair, arm_*_upper, arm_*_lower,
leg_*_upper, leg_*_lower, tail_1 -- so IDLE_TALK / ATTACK_BITE / ATTACK_POUNCE and the
walk/run cycles carry over unchanged.  New parts are all leaves or extra distal chain
segments, which the existing walk() calls drive for free.

Limbs, neck and tail are NOT hand-placed: they are described by WORLD segment angles and
lengths, the joint chain is walked, the pivot height is solved so the paw lands exactly
on y=0, and each part's LOCAL rotation is derived as (world angle - parent world angle).

Run:  python scripts/dire_wolf_spec.py [--zfight|--uv|--json|--pose]
"""
import math
import numpy as np

D = math.radians

# ---------------------------------------------------------------- contract
SPECIES = [
    # name,               variant, real withers m
    ("dire_wolf",         0, 0.90),   # Aenocyon dirus -- heavier build than a gray wolf
    ("pleistocene_wolf",  1, 0.82),   # Beringian Canis lupus
]
HITBOX = (1.0, 1.0)     # ModEntity: createEntity(EntityDireWolf::new,"dire_wolf",1F,1F,..)

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
    """Transform world points into `name`'s own local (authoring) frame."""
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


# ================================================================ chain helpers
def zchain(parent, pivot, segs, cls, parent_tilt=0.0):
    """Chain authored lying forward along -Z from `pivot`; bent by world angles.

    segs: (name, world_angle_deg_above_horizontal, length, halfwidth, up, down, overlap)
    Rest positions are colinear along -Z, so a rotation about each pivot lands the
    joints exactly where the world angles say they should be.
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


def tailchain(parent, pivot, segs, cls, parent_tilt=0.0):
    """Chain authored lying BACKWARD along +Z from `pivot`; bent by world droop.

    segs: (name, droop_deg_below_horizontal, length, halfwidth, up, down, overlap)
    zchain() builds forward along -Z, so feeding it a tail makes the "droop" swing the
    dock forward INTO the rump instead of behind it.  For a box extending to +Z,
    R_x(theta) sends the far tip to y = -sin(theta)*L, so world droop == theta directly
    and the local rotation is (droop_i - droop_{i-1}).
    """
    px, py, pz = pivot
    prev, par, z = parent_tilt, parent, pz
    for name, droop, L, hw, up, dn, ov in segs:
        add(name, par, (px, py, z), (droop - prev, 0, 0),
            (px - hw, py - dn, z - ov), (px + hw, py + up, z + L), cls)
        par, prev, z = name, droop, z + L
    return par


def leg(prefix, parent, pivot_z, segs, parent_tilt):
    """Leg authored hanging straight down from a pivot whose HEIGHT IS SOLVED.

    segs: (name, world_angle_deg (+ swings the distal end forward), length,
           outer|x|, inner|x|, front, back, overlap)

    The sole lands on y = 0 when the pivot's WORLD height equals sum(L*cos(angle)).
    The authored height is not that number, because the parent's own tilt drags the
    pivot off it -- solve for the authored value instead of assuming.
    """
    drop = sum(L * math.cos(D(a)) for _, a, L, *_ in segs)
    y_auth = drop
    for _ in range(8):
        y_auth += drop - world(parent, (0.0, y_auth, pivot_z))[0][1]
    for side, s in (("left", -1.0), ("right", 1.0)):
        par, prev_ang, y = parent, parent_tilt, y_auth
        for name, ang, L, outer, inner, fr, bk, ov in segs:
            add(f"{prefix}_{side}_{name}", par, (s * (outer + inner) / 2, y, pivot_z),
                (ang - prev_ang, 0, 0),
                (s * outer, y - L, pivot_z - fr), (s * inner, y + ov, pivot_z + bk),
                "paw" if name == "paw" else "leg")
            par, prev_ang, y = f"{prefix}_{side}_{name}", ang, y - L
    return drop


# ================================================================ TORSO
# LATERAL LADDER -- every |x| plane in the rig is distinct, and neighbours that share
# any y/z footprint are >=0.4 apart, so no two boxes can share a side plane:
#   hair 4.45 > arm_upper/thigh 4.03 > neck_ruff 3.95 > body_main 3.62 > cheek 3.50 >
#   croup 3.48 > head_neck 3.45 > paws 3.25/3.35 > loin 3.04 > chest/head_main 3.05 >
#   gaskin 3.00 > feet 2.80/2.90 > arm_lower 2.60.
# Values repeat only between boxes that share NO y/z footprint (arm_upper vs thigh,
# chest vs head_main); every pair that does overlap is >=0.40 apart, which is well
# outside the 0.15u plane tolerance the sweep gates on.
#
# The topline is carried by TALL boxes whose lower halves are buried in their parent
# (SOP phase 2), so the back reads as one line, not a stack of slabs.  A wolf's back is
# LEVEL -- the 13-21 degree mass tilts that suit a bison/mammoth hump are wrong here;
# the angled masses on a canid are the croup, the neck and the leg segments.
add("body_main", None, (0, 13.0, 0.0), (0, 0, 0),
    (-3.62, 8.6, -5.5), (3.62, 16.0, 2.5), "body")

# Deep narrow ribcage front + brisket.  Chest depth (16.0 - 8.2 = 7.8u) is very close to
# the foreleg length (8.3u), which is the canid proportion; the old rig was 7.0 deep on
# an 8.0 leg and still read squat because the torso had no separate chest mass at all.
add("body_chest", "body_main", (0, 13.0, -5.0), (5, 0, 0),
    (-3.05, 8.2, -8.6), (3.05, 15.05, -3.0), "body")

# Loin: narrower AND with its floor 2.0u above the brisket -- that gap IS the tuck-up.
add("body_loin", "body_main", (0, 13.0, 2.0), (-3, 0, 0),
    (-3.04, 10.4, 1.2), (3.04, 15.7, 6.6), "body")

add("body_croup", "body_loin", (0, 13.5, 6.4), (-8, 0, 0),
    (-3.48, 9.8, 5.2), (3.48, 15.9, 10.0), "body")

# Shoulder mantle.  0.73u proud of the ribcage on each side and 1.0u above the withers;
# its floor (10.0) is buried inside body_main (8.6) so only the crest and flanks emerge.
add("hair", "body_main", (0, 13.0, -3.0), (0, 0, 0),
    (-4.45, 10.0, -8.2), (4.45, 17.0, 1.9), "ruff")

TILT = {"body_chest": 5.0, "body_loin": -3.0, "body_croup": -11.0}


# ================================================================ NECK + HEAD
# These are WORLD angles, and they accumulate: a first pass at -16/-18/-26/-38 read as
# a plausible set of local bends and drove the nose to y 7.6, pointing at the floor.
# The neck alone descends; the skull then lifts BACK to near-level (local +14) and the
# muzzle drops only gently, so the head is carried low and forward -- prowling wolf --
# with the muzzle level.  That head carriage is most of the "wolf" read.
zchain("body_main", (0, 14.2, -4.8), [
    #  name          ang    L    hw    up   dn   ov
    ("head_neck",     -8,  5.2, 3.35, 2.40, 3.15, 1.5),
    ("head_main",      2,  3.7, 2.60, 2.45, 2.20, 1.0),   # braincase + zygomatics
    ("head_face",      0,  1.3, 1.98, 1.85, 1.75, 0.8),   # frontal / the STOP
    ("head_snout",    -7,  3.0, 1.52, 1.18, 1.38, 1.1),   # long tapering muzzle
], "head")

# The zchain lays every head segment out colinear along -Z at the pivot's y, so the
# absolute coordinates below are read straight off that layout.  RE-DERIVE THEM after any
# change to the chain's lengths: shrinking the head from 12.6u to 9.2u left the nose
# floating 3u in front of the muzzle and both eyes hanging in mid-air ahead of the skull,
# and the outline render still looked plausible.
#   head_neck  z -10.0..-3.3   y 11.05..16.60   hw 3.35
#   head_main  z -13.7..-9.0   y 12.00..16.65   hw 2.60
#   head_face  z -15.0..-12.9  y 12.35..16.05   hw 1.98
#   head_snout z -18.0..-13.9  y 12.82..15.38   hw 1.52

# Fur collar framing the face -- wide, short, and sitting at the HEAD end of the neck so
# it does not duplicate the shoulder mantle.  This is the box that says "dire".
add("neck_ruff", "head_neck", (0, 14.2, -4.8), (0, 0, 0),
    (-3.70, 12.15, -11.4), (3.70, 17.6, -7.0), "ruff")

# Nose leather on the upper front of the muzzle.
add("nose", "head_snout", (0, 14.2, -18.0), (0, 0, 0),
    (-1.05, 13.75, -18.7), (1.05, 15.15, -17.3), "nose")
# Mandible: narrower than the maxilla (1.10 vs 1.52) and stopping SHORT of it, because
# a canid's upper jaw overhangs the lower.
add("head_jaw", "head_main", (0, 13.0, -10.0), (-7, 0, 0),
    (-1.10, 11.60, -17.6), (1.10, 13.80, -10.6), "jaw")

# Cheek jowls: proud of the skull (2.60) at 3.15, and kept BELOW eye level -- a first
# pass ran them up past the eye line and swallowed the rear half of the eye plane, which
# is the equid bug (an eye that clears the skull but is buried in a wider SIBLING).
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"cheek_{side}", "head_main", (s * 2.6, 13.1, -11.4), (0, 0, s * -9),
        (s * 2.50, 12.10, -13.3), (s * 3.15, 13.95, -9.7), "ruff")

# Ears: SMALL, THICK (2.0u front-to-back, not the old 1.0u paper), rounded-triangular,
# set wide and canted out+forward.  Two segments so the ear tapers to its tip.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"ear_{side}", "head_main", (s * 1.70, 16.2, -11.2), (-8, 0, s * -15),
        (s * 0.55, 16.20, -12.3), (s * 2.60, 18.60, -10.1), "ear")
    add(f"ear_{side}_tip", f"ear_{side}", (s * 1.70, 18.3, -11.2), (0, 0, s * -11),
        (s * 0.85, 18.30, -12.1), (s * 2.30, 20.50, -10.3), "ear")

# Eyes: zero-width plane on its own UV footprint, 0.35u proud of the skull side, in the
# upper third of the braincase and just behind the stop -- which is where a wolf's eye
# actually sits AND the only place clear of every wider box.  Kept inside head_main's
# own z span so it never floats over the narrower head_face with a gap behind it.
# The eye is 1.0 DEEP x 2.0 TALL, and both numbers are WHOLE UNITS on purpose.
#
# A zero-width box draws two coplanar quads that sample opposite halves of its UV
# footprint (2*(0+d) wide). At a fractional depth those two halves round to OVERLAPPING
# texel rects -- at d=1.5 the outward face landed on x[74,76) and the inward on x[73,75).
# The inward half has to be cleared to alpha 0 (otherwise the two quads z-fight), and
# that clear also wiped a column of the outward eye. Worse, the two sides paint in
# opposite order, so the LEFT eye kept 2px and the RIGHT eye kept 1px: visibly different
# eyes on the two sides of one face, from geometry that is perfectly symmetric.
#
# At whole-unit depth the halves land on exact texel boundaries (u0..u0+1 and u0+1..u0+2)
# and cannot overlap, so both eyes render identically at 1 x 2 px.
# Kept clear of cheek_* (top 13.95) as well as of head_main.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"eye_{side}", "head_main", (s * 2.95, 15.3, -12.80), (0, 0, 0),
        (s * 2.95, 14.30, -13.30), (s * 2.95, 16.30, -12.30), "eye")


# ================================================================ TAIL
# Narrow dock (1.35) so it threads between the thighs (inner face 1.85, 0.5 clear),
# swelling to 1.60 for the brush once it is past them, then tapering to the tip.
tailchain("body_croup", (0, 14.9, 9.4), [
    ("tail_1", 22, 3.4, 1.35, 1.30, 1.45, 1.0),
    ("tail_2", 38, 3.2, 1.60, 1.45, 1.55, 1.1),
    ("tail_3", 52, 2.9, 1.15, 1.05, 1.10, 0.9),
], "tail", parent_tilt=TILT["body_croup"])


# ================================================================ LEGS
# Four segments each, fore and hind DIFFERENT: the fore leg is near-vertical (scapula
# +8, forearm -4) and the hind carries the canid zig-zag (femur +26 forward, tibia -40
# back to the hock, metatarsus +10 vertical).  The old rig used one identical 2-segment
# stump for all four corners, which is why it stood like a table.
FORE_DROP = leg("arm", "body_chest", -6.1, [
    #  name        ang   L    outer inner  fr   bk   ov
    ("upper",        8, 5.40, 4.03, 0.95, 1.65, 2.35, 1.2),
    ("lower",       -4, 4.40, 2.85, 1.35, 1.65, 1.90, 1.2),
    ("foot",         5, 2.35, 2.40, 0.55, 1.15, 1.05, 1.0),
    ("paw",          0, 1.20, 2.72, 0.15, 2.30, 0.85, 0.6),
], TILT["body_chest"])

HIND_DROP = leg("leg", "body_croup", 8.1, [
    ("upper",       26, 5.55, 4.03, 1.85, 2.55, 2.75, 1.4),
    ("lower",      -40, 5.10, 3.00, 1.40, 1.95, 2.10, 0.8),
    ("foot",        10, 3.55, 2.50, 1.00, 1.10, 1.15, 1.0),
    ("paw",          0, 1.20, 2.82, 0.65, 2.20, 0.85, 0.6),
], TILT["body_croup"])


# ================================================================ UV
CANVAS = (128, 128)


def footprint(p):
    """The region MC reserves from texOffs: 2*(w+d) x (h+d), in texels.  CEIL."""
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    w, h, d = (abs(b[i] - a[i]) for i in range(3))
    if p[6] == "eye":
        w = 0.0
    return int(math.ceil(2 * (w + d))), int(math.ceil(h + d))


def uvpack():
    """Shelf-pack every box's true footprint, then ASSERT zero overlap."""
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
        print(f"  {p[0]:<22} texOffs({u:3d},{v:3d})   footprint {fw:3d}x{fh:<3d}")
    print(f"  zero overlaps asserted;  {used}/{H} rows used")
    return placed


def emit_json():
    import io
    import json
    from contextlib import redirect_stdout
    with redirect_stdout(io.StringIO()):
        placed = uvpack()
        main()
    out = []
    for name, parent, o, r, a, b, cls in PARTS:
        u, v, _, _ = placed[name]
        out.append({"n": name, "p": parent, "o": [round(x, 4) for x in o],
                    "r": [round(x, 4) for x in r], "f": [round(x, 4) for x in a],
                    "t": [round(x, 4) for x in b], "uv": [u, v], "c": cls})
    print(json.dumps({"canvas": list(CANVAS), "parts": out}, separators=(",", ":")))


# ================================================================ z-fight sweep
def zfight():
    import sys
    import os
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
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
        print(f"  {area:6.2f}u2  {na:<20} {'XYZ'[fa[0]]}  <->  {nb:<20} "
              f"{'XYZ'[fb[0]]}{rel}")
    if not shown:
        print("  clean")


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
        print(f"{n:<20}{lo[n][0]:7.2f}..{hi[n][0]:<7.2f}"
              f"{lo[n][1]:7.2f}..{hi[n][1]:<7.2f}{lo[n][2]:7.2f}..{hi[n][2]:<7.2f}")

    print("\n" + "=" * 80)
    print("GROUND CONTACT + JOINT CHAIN")
    print("=" * 80)
    print(f"fore pivot drop {FORE_DROP:6.2f}   hind pivot drop {HIND_DROP:6.2f}")
    for n in sorted(wc):
        if n.endswith("_paw"):
            print(f"  {n:<22} sole y {lo[n][1]:+6.3f}   z {lo[n][2]:6.2f}..{hi[n][2]:.2f}")
    floor = min(v[1] for v in lo.values())
    print(f"  lowest point of the whole model: {floor:+.3f}"
          + ("   OK" if floor > -0.02 else "   <== THROUGH THE FLOOR"))
    withers = hi["body_main"][1]
    stance = ((lo["leg_left_paw"][2] + hi["leg_left_paw"][2]) / 2
              - (lo["arm_left_paw"][2] + hi["arm_left_paw"][2]) / 2)
    print(f"  fore->hind stance {stance:.2f}u = {stance/withers:.2f} x withers"
          f"   (a square-standing canid is ~0.95)")
    print(f"  elbow y {lo['arm_left_upper'][1]:.2f}   brisket y {lo['body_chest'][1]:.2f}"
          f"   (elbow should sit at/just below the brisket)")
    print(f"  stifle z {(lo['leg_left_lower'][2]+hi['leg_left_lower'][2])/2:.2f}"
          f"   hock z {(lo['leg_left_foot'][2]+hi['leg_left_foot'][2])/2:.2f}"
          f"   (stifle FORWARD of the hock = canid zig-zag)")
    print(f"  chest depth {withers - lo['body_chest'][1]:.2f}u vs ground-to-brisket "
          f"{lo['body_chest'][1]:.2f}u   (canids are ~1:1 -- leggy, not squat)")
    print(f"  head centre y {(lo['head_main'][1]+hi['head_main'][1])/2:.2f} = "
          f"{(lo['head_main'][1]+hi['head_main'][1])/2/withers:.2f} x withers"
          f"   nose y {(lo['nose'][1]+hi['nose'][1])/2:.2f}")
    print(f"  tail tip ({hi['tail_3'][2]:.1f}z, {lo['tail_3'][1]:.1f}y) -- must be "
          f"BEHIND the croup ({hi['body_croup'][2]:.1f}z) and above the floor")
    print(f"  tuck-up: brisket {lo['body_chest'][1]:.2f} -> belly "
          f"{lo['body_loin'][1]:.2f}  (+{lo['body_loin'][1]-lo['body_chest'][1]:.2f})")

    # ---- head proportions (the actual derp fix) ----------------------------
    print("\n" + "=" * 80)
    print("HEAD PROPORTIONS   (canid skull: width ~0.5-0.6 x length)")
    print("=" * 80)
    skull_w = hi["head_main"][0] - lo["head_main"][0]
    skull_l = hi["head_main"][2] - lo["head_snout"][2]
    muzzle_w = hi["head_snout"][0] - lo["head_snout"][0]
    muzzle_h = hi["head_snout"][1] - lo["head_snout"][1]
    print(f"  skull width {skull_w:.2f} / occiput-to-nose {skull_l:.2f} "
          f"= {skull_w/skull_l:.2f}      (old rig: 7.00 / 10.00 = 0.70)")
    print(f"  muzzle {muzzle_w:.2f}w x {muzzle_h:.2f}h x "
          f"{hi['head_snout'][2]-lo['head_snout'][2]:.2f}d"
          f"   (old rig: 4.00 x 4.00 x 4.50 -- a square stub)")
    print(f"  ear thickness {hi['ear_left'][2]-lo['ear_left'][2]:.2f}u   "
          f"(old rig: 1.00u paper)")
    print(f"  ruff proud of ribcage by {hi['hair'][0]-hi['body_main'][0]:.2f} on x, "
          f"{hi['hair'][1]-hi['body_main'][1]:.2f} on y   "
          f"(old rig: +0.50 x, +1.50 y on a box 1u WIDER than the body)")

    # ---- eye clearance (SOP phase 4 -- check EVERY wider box) --------------
    print("\n" + "=" * 80)
    print("EYE CLEARANCE  (must sit outside every wider ancestor AND the ruffs)")
    print("=" * 80)
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head_main", "head_neck", "body_main", "neck_ruff",
                    f"cheek_{side}", "hair"):
            p = BY_NAME[anc]
            l = local_of(anc, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gaps = [max(a[i] - l[i], l[i] - b[i]) for i in range(3)]
            gap = max(gaps)
            print(f"  eye_{side:<6} vs {anc:<12} clear by {gap:6.2f} on "
                  f"{'xyz'[int(np.argmax(gaps))]}"
                  + ("" if gap > 0.3 else "   <== TOO CLOSE"))

    # ---- size calibration --------------------------------------------------
    print("\n" + "=" * 80)
    print(f"SIZE CALIBRATION   mesh withers = {withers:.2f}u,  scale = m*16/withers")
    print("=" * 80)
    ruff_top = hi["hair"][1]
    ear_top = hi["ear_left_tip"][1]
    nose_z = lo["head_snout"][2]
    tail_z = hi["tail_3"][2]
    print(f"{'species':<19}{'m':>6}{'scale':>7}{'withers':>9}{'ruff':>7}{'eartop':>8}"
          f"{'nose->rump':>12}{'+tail':>8}")
    for name, var, real in SPECIES:
        s = real * 16.0 / withers
        print(f"{name:<19}{real:6.2f}{s:7.3f}{withers*s/16:9.2f}{ruff_top*s/16:7.2f}"
              f"{ear_top*s/16:8.2f}{(hi['body_croup'][2]-nose_z)*s/16:12.2f}"
              f"{(tail_z-nose_z)*s/16:8.2f}")
    print(f"registered hitbox {HITBOX[0]} x {HITBOX[1]} blocks"
          "   (per-TYPE, so the biggest species should sit close to it)")
    print(f"  body length / withers = "
          f"{(hi['body_croup'][2]-nose_z)/withers:.2f}   (real canid ~1.8-2.0)")

    # ---- silhouette --------------------------------------------------------
    pts = np.vstack([world(p[0], vol(p)) for p in PARTS if p[6] != "eye"])
    print("\n" + "=" * 80)
    print("SIDE SILHOUETTE      <- nose      tail ->")
    print("=" * 80)
    zmin, zmax, ymax = pts[:, 2].min(), pts[:, 2].max(), pts[:, 1].max()
    W, H = 78, 26
    g = [[" "] * W for _ in range(H)]
    for _, y, z in pts:
        c = int((z - zmin) / (zmax - zmin) * (W - 1))
        r = int((ymax - y) / ymax * (H - 1))
        if 0 <= r < H:
            g[r][c] = "#"
    for r, row in enumerate(g):
        print(f"{ymax - r*ymax/(H-1):5.1f} |" + "".join(row))
    print(f"      z {zmin:.1f} .. {zmax:.1f}")

    # top view, to prove the body is deep-and-narrow rather than a slab
    print("\n" + "=" * 80)
    print("TOP-DOWN SILHOUETTE      <- nose      tail ->")
    print("=" * 80)
    xmax = max(abs(pts[:, 0]).max(), 1e-6)
    H2 = 15
    g = [[" "] * W for _ in range(H2)]
    for x, _, z in pts:
        c = int((z - zmin) / (zmax - zmin) * (W - 1))
        r = int((x + xmax) / (2 * xmax) * (H2 - 1))
        if 0 <= r < H2:
            g[r][c] = "#"
    for r, row in enumerate(g):
        print(f"{-xmax + r*2*xmax/(H2-1):5.1f} |" + "".join(row))

    # ---- UV budget ---------------------------------------------------------
    print("\n" + "=" * 80)
    print("UV FOOTPRINT BUDGET   (MC reserves 2*(w+d) x (h+d) from every texOffs)")
    print("=" * 80)
    tot = 0
    for p in PARTS:
        a, b = np.asarray(p[4], float), np.asarray(p[5], float)
        w, h, d = [max(1, round(abs(b[i] - a[i]))) for i in range(3)]
        if p[6] == "eye":
            w = 0
        tot += 2 * (w + d) * (h + d)
    print(f"{len(PARTS)} boxes, {tot} px")
    for cw, ch in ((128, 64), (128, 128), (256, 128)):
        print(f"  {cw}x{ch} = {cw*ch:>6} px -> "
              f"{'FITS' if tot < cw*ch*0.72 else 'TOO TIGHT'} ({100*tot/(cw*ch):.0f}% raw)")


# ================================================================ phase 7 poses
def world_mod(name, pts, rot_over=None, dy=0.0):
    """FK with per-part rotation overrides and a global vertical shift."""
    p = np.asarray(pts, float).reshape(-1, 3)
    for nm, _, o, r, *_ in ancestry(name):
        r = (rot_over or {}).get(nm, r)
        o = np.asarray(o, float)
        p = (rmat(*r) @ (p - o).T).T + o
    p = p.copy()
    p[:, 1] += dy
    return p


LEGS = {
    "arm": ("body_chest", TILT["body_chest"],
            [("upper", 5.10), ("lower", 4.70), ("foot", 2.35), ("paw", 1.20)]),
    "leg": ("body_croup", TILT["body_croup"],
            [("upper", 5.55), ("lower", 5.10), ("foot", 3.55), ("paw", 1.20)]),
}


def _leg_rot(prefix, side, angles, pitch=0.0):
    """World angles -> the local rotation each segment must carry.

    `pitch` is a nose-up rotation applied to body_main.  It is part of the world tilt the
    limb root inherits, so it has to enter here or every segment ends up `pitch` degrees
    away from the angle that was solved for -- the equid bug, in the pose solver.
    """
    _, tilt, segs = LEGS[prefix]
    over, prev = {}, tilt + pitch
    for (nm, _), a in zip(segs, angles):
        over[f"{prefix}_{side}_{nm}"] = (a - prev, 0.0, 0.0)
        prev = a
    return over


def _leg_score(prefix, angles, dy, mode="folded", pitch=0.0):
    """Penalty for a posed leg.  Both modes forbid anything through the floor.

    "folded"  -- the metatarsus/metacarpus and the paw lie FLAT on the ground, which is
                 the sternal-recumbency / lying pose.
    "upright" -- only the PAW touches; every segment stays near-vertical.  A canid sits
                 on its haunches with the forelegs propped straight, so scoring a sitting
                 dog's front legs as "folded" produces a lying-down dog with its chest on
                 the floor.  That is what the first solve returned (fore -57/+29/+90/+86)
                 and it is wrong for this animal even though the penalty was tiny.
    """
    over = _leg_rot(prefix, "left", angles, pitch)
    if pitch:
        over["body_main"] = (pitch, 0.0, 0.0)
    pen = 0.0
    _, _, segs = LEGS[prefix]
    for k, (nm, _) in enumerate(segs):
        full = f"{prefix}_left_{nm}"
        w = world_mod(full, vol(BY_NAME[full]), over, dy)
        lo = w[:, 1].min()
        if lo < 0.0:
            pen += (-lo) ** 2 * 12.0
        if mode == "folded":
            if k >= 2:
                pen += (lo - 0.12) ** 2 * 3.0
        else:
            if k == len(segs) - 1:                     # the paw, and only the paw
                pen += (lo - 0.10) ** 2 * 6.0
    if mode == "folded":
        for a in angles[2:]:                           # distal segments horizontal
            pen += (math.cos(D(a)) ** 2) * 4.0
    else:
        for a in angles:                               # whole column vertical
            pen += (math.sin(D(a)) ** 2) * 3.0
    return pen


# A wolf sits upright on its haunches: hind legs folded under, forelegs propped straight.
# It sleeps curled on its side with everything folded.
POSE_MODE = {("sit", "arm"): "upright", ("sit", "leg"): "folded",
             ("sleep", "arm"): "folded", ("sleep", "leg"): "folded"}


# The converged result of pose_solve(), cached so the Java port does not pay for a
# four-minute random search on every run.  Regenerate with `--pose --resolve` and paste
# the printed numbers back here; `--pose` alone re-verifies the CACHE against the real box
# geometry, which is the check that actually matters (a stale cache after a geometry edit
# would put a paw through the floor, and the verifier prints exactly that).
#   (pose, prefix) -> (world segment angles, body dy, body nose-up pitch)
POSE_CACHE = {
    ("sit",   "arm"): ([  0.00,   0.00,  0.00,  0.00], -2.10, 22.0),
    ("sit",   "leg"): ([ 58.79,  31.96, 89.53, 85.20], -2.10, 22.0),
    ("sleep", "arm"): ([ 82.92, -12.03, 89.84, 86.82], -7.20,  0.0),
    ("sleep", "leg"): ([ 84.47, -24.52, 89.22, 84.96], -7.20,  0.0),
}


def pose_verify():
    """Re-score the cached poses against the CURRENT geometry.  Cheap, and it is the
    thing that breaks silently: any later box edit invalidates the cached angles."""
    print("=" * 80)
    print("PHASE 7 -- CACHED POSES RE-VERIFIED AGAINST THE CURRENT BOX GEOMETRY")
    print("=" * 80)
    bad = 0
    for (pose, prefix), (angles, dy, pitch) in sorted(POSE_CACHE.items()):
        mode = POSE_MODE[(pose, prefix)]
        pen = _leg_score(prefix, np.array(angles, float), dy, mode, pitch)
        over = _leg_rot(prefix, "left", angles, pitch)
        if pitch:
            over["body_main"] = (pitch, 0.0, 0.0)
        floor = min(world_mod(nm, vol(BY_NAME[nm]), over, dy)[:, 1].min()
                    for nm in over if nm != "body_main")
        toe = world_mod(f"{prefix}_left_paw", vol(BY_NAME[f"{prefix}_left_paw"]),
                        over, dy)[:, 1].min()
        ok = floor > -0.05 and toe < 0.45
        bad += not ok
        print(f"  {pose:<6} {prefix:<4} [{mode:<7}] penalty {pen:7.4f}   "
              f"lowest {floor:+6.3f}   paw sole {toe:+6.3f}"
              + ("" if ok else "   <== STALE, re-run --pose --resolve"))
    print("  all poses valid" if not bad else f"  {bad} pose(s) need re-solving")
    return not bad


def pose_solve():
    rng = np.random.default_rng(11)
    print("=" * 80)
    print("PHASE 7 -- POSES SOLVED AGAINST REAL BOX GEOMETRY")
    print("=" * 80)
    body_lo = world("body_main", corners(BY_NAME["body_main"]))[:, 1].min()
    out = {}

    def solve_leg(prefix, dy, mode, pitch, n=2500, rounds=9):
        """Shrinking random search.  `n`/`rounds` are lowered for the grid probe and
        raised again for the final solve, so the joint (pitch, drop) search stays a
        couple of minutes instead of hours."""
        base = (np.array([0.0, 0.0, 0.0, 0.0]) if mode == "upright"
                else np.array([40.0, -70.0, 95.0, 95.0] if prefix == "arm"
                              else [70.0, -95.0, 80.0, 80.0]))
        best, bs = base.copy(), _leg_score(prefix, base, dy, mode, pitch)
        span = np.array([55.0, 55.0, 45.0, 45.0])
        for _ in range(rounds):
            cand = best + rng.normal(0, 1, (n, 4)) * span
            cand[:, 3] = cand[:, 2] + rng.normal(0, 6, n)
            for c in cand:
                s = _leg_score(prefix, c, dy, mode, pitch)
                if s < bs:
                    bs, best = s, c.copy()
            span *= 0.62
        return best, bs

    # SIT: a canid does NOT drop its whole barrel to the floor -- it pitches nose-up and
    # sits on its haunches with the forelegs propped straight. Dropping the body uniformly
    # (the first attempt) forced the shoulder 65 deg backwards just to reach the ground,
    # for a penalty of 2.48. Pitch and drop are therefore solved TOGETHER, not guessed.
    print("\nSIT   searching (body pitch, body drop) jointly")
    best_sit, best_cost = None, 1e18
    for pitch in (6, 10, 14, 18, 22, 26, 30):
        for belly in (3.5, 4.5, 5.5, 6.5, 7.5):
            dy = belly - body_lo
            _, ca = solve_leg("arm", dy, "upright", pitch, n=350, rounds=6)
            _, cl = solve_leg("leg", dy, "folded", pitch, n=350, rounds=6)
            if ca + cl < best_cost:
                best_cost, best_sit = ca + cl, (pitch, dy, belly)
    pitch, dy, belly = best_sit
    print(f"  pitch {pitch:+.0f} deg nose-up, dy {dy:+.2f}u (belly y={belly:.1f}), "
          f"cost {best_cost:.4f}")
    for prefix in ("arm", "leg"):
        mode = POSE_MODE[("sit", prefix)]
        ang, c = solve_leg(prefix, dy, mode, pitch)
        out[("sit", prefix)] = (ang, dy, pitch)
        print(f"  {prefix:<5} [{mode:<7}] world angles " +
              " ".join(f"{a:7.2f}" for a in ang) + f"   penalty {c:.4f}")
        over = _leg_rot(prefix, "left", ang, pitch)
        over["body_main"] = (pitch, 0.0, 0.0)
        for nm in [k for k in over if k != "body_main"]:
            w = world_mod(nm, vol(BY_NAME[nm]), over, dy)
            print(f"      {nm:<22} localRotX {over[nm][0]:+8.3f} deg"
                  f"   y {w[:,1].min():6.2f}..{w[:,1].max():6.2f}")

    # SLEEP: curled on the ground, everything folded, no pitch.
    dy = 1.4 - body_lo
    print(f"\nSLEEP   body drop dy = {dy:+.2f}u  (belly rests at y=1.4)")
    for prefix in ("arm", "leg"):
        ang, c = solve_leg(prefix, dy, "folded", 0.0)
        out[("sleep", prefix)] = (ang, dy, 0.0)
        print(f"  {prefix:<5} [folded ] world angles " +
              " ".join(f"{a:7.2f}" for a in ang) + f"   penalty {c:.4f}")
        over = _leg_rot(prefix, "left", ang, 0.0)
        for nm, rot in over.items():
            w = world_mod(nm, vol(BY_NAME[nm]), over, dy)
            print(f"      {nm:<22} localRotX {rot[0]:+8.3f} deg"
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
    elif "--pose" in sys.argv and "--resolve" not in sys.argv:
        pose_verify()
    elif "--pose" in sys.argv:
        pose_solve()
    else:
        main()
