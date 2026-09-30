#!/usr/bin/env python3
"""Deer (cervid) model spec + forward-kinematics checker -- SOP Phase 1 / 1b / 2.

Authoring space is BLOCKBENCH project space: Y up, ground y=0, -Z = front (nose),
-X = left.  Group origins and cube from/to are ABSOLUTE project coordinates, exactly as
Blockbench stores them, so this table doubles as the data for the build script.

A group rotates everything inside it about its own origin:
    world(p) = A_root(A_child(... p))      A_g(p) = R_g @ (p - o_g) + o_g
Rotation order is Rz @ Ry @ Rx -- what Blockbench uses, and what MC's ModelPart uses
under the verified bbRot = [-rx, -ry, +rz] export flip.

Limbs and the neck are NOT hand-placed: they are described by WORLD segment angles and
lengths, the joint chain is walked, the pivot height is solved so the hoof lands exactly
on y=0, and each part's LOCAL rotation is derived as (world angle - parent world angle).
That is what stops the equid bug where a local angle that read fine in isolation was
11 degrees further forward in world space because of the croup's tilt.

One mesh serves five species (deer.json), which declares NO model flags, so
ModelDeer.setupAnim reads getVariant() -- the ModelEquid / ModelAntelope pattern.
Two antler families are built and toggled:
    PALMATE  -> megaloceros, stag_moose, moose
    BRANCHED -> wapiti, red_deer

Run:  python scripts/deer_spec.py
"""
import math
import numpy as np

D = math.radians

# ---------------------------------------------------------------- contract
SPECIES = [
    # name,         variant, real withers m, antler family, real antler span m
    ("megaloceros", 0, 2.10, "palmate",  3.50),
    ("stag_moose",  1, 2.00, "palmate",  2.00),
    ("moose",       2, 1.90, "palmate",  1.80),
    ("wapiti",      3, 1.45, "branched", 1.25),
    ("red_deer",    4, 1.20, "branched", 0.85),
]
HITBOX = (1.4, 1.8)          # ModEntity: createEntity(EntityDeer::new,"deer",1.4F,1.8F,..)

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


# ================================================================ TORSO
# Side planes are all distinct so no two torso boxes share one: barrel 6.5,
# croup 6.3, withers 6.1, chest 5.9.  Each box is TALL and buried in the previous
# one (SOP phase 2) so the topline reads as a single line, not a stack of slabs.
add("body_barrel", None, (0, 21.0, 5.0), (0, 0, 0),
    (-6.5, 15.0, -6.0), (6.5, 25.2, 15.0), "body")
add("body_withers", "body_barrel", (0, 20.0, 2.0), (0, 0, 0),
    (-6.1, 17.5, -4.0), (6.1, 28.4, 7.5), "body")
add("body_chest", "body_barrel", (0, 21.0, -5.0), (5, 0, 0),
    (-5.5, 13.9, -12.5), (5.5, 24.2, 1.0), "body")
add("body_croup", "body_barrel", (0, 22.5, 13.0), (-8, 0, 0),
    (-6.3, 15.6, 9.0), (6.3, 26.8, 22.0), "body")

TILT = {"body_chest": 5.0, "body_croup": -8.0}   # inherited world tilt of a limb root


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


def leg(prefix, parent, pivot_z, segs, parent_tilt):
    """Leg authored hanging straight down from a pivot whose HEIGHT IS SOLVED.

    segs: (name, world_angle_deg (+ swings the distal end forward), length,
           outer|x|, inner|x|, front, back, overlap)

    The sole lands on y = 0 when the pivot's WORLD height equals sum(L*cos(angle)).
    The authored height is not that number, because the parent's own tilt drags the
    pivot off it -- the chest's +5 costs 0.11u and the croup's -8 gains 0.59u. Solve
    for the authored value instead of assuming, which is the equid lesson applied to
    positions rather than angles.
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
                (s * outer, y - L, pivot_z - fr), (s * inner, y + ov, pivot_z + bk), "leg")
            par, prev_ang, y = f"{prefix}_{side}_{name}", ang, y - L
    return drop


# ================================================================ NECK + HEAD
# Rise from the chest top to the poll: base y 24, poll ~35, reach 16 forward.
# Cumulative angle is kept shallow (34 -> 40 -> -6 -> -14) precisely because
# -38 degrees of accumulated chest+neck+head once put a snout through the floor.
zchain("body_chest", (0, 24.0, -6.0), [
    #  name          ang    L    hw   up   dn   ov
    ("neck_lower",    34, 10.5, 4.2, 3.6, 4.2, 1.2),
    ("neck_upper",    40,  8.5, 3.0, 3.2, 3.4, 1.2),
    ("head_skull",    -6,  7.0, 3.4, 3.4, 3.2, 1.2),
    ("head_muzzle",  -14,  8.2, 2.4, 2.4, 2.7, 1.2),
], "head", parent_tilt=TILT["body_chest"])

# Moose / stag-moose dewlap ("bell"): hidden on the other three by setScale(0,0,0).
add("throat_bell", "neck_upper", (0, 21.0, -21.0), (0, 0, 0),
    (-1.7, 14.0, -22.6), (1.7, 21.4, -18.6), "bell")

# Big leaf ears, low on the skull and swept back-and-out.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"ear_{side}", "head_skull", (s * 3.0, 26.4, -25.4), (10, s * -34, s * -40),
        (s * 3.0, 25.6, -26.8), (s * 10.2, 26.6, -22.4), "ear")

# Eyes: the ModelEquid design -- a 3x3 zero-width plane on its own 6x6 UV footprint,
# blinked by retracting it INTO the skull rather than by swapping art. It must clear
# every WIDER ancestor, not just the skull, so it sits high and well forward.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"eye_{side}", "head_skull", (s * 3.75, 26.2, -27.4), (0, 0, 0),
        (s * 3.75, 24.7, -28.9), (s * 3.75, 27.7, -25.9), "eye")

# ================================================================ TAIL
# Cervid tails are short. The dock threads between the thighs (inner face 1.6),
# so its half-width stays at 1.2 -- 0.4 clear, well outside the 0.15u plane tolerance.
#
# zchain lays segments forward along -Z, so a tail needs a world angle PAST -90 to hang
# behind its pivot. The original -62/-78 from (25.4, 20.0) swung the whole tail down and
# FORWARD into the rump: 100% of both boxes sat inside body_croup and no tail rendered
# (the latent bug the toxodon pass flagged). Re-solved 2026-09-30 by grid search
# (pivot y/z x both angles) against croup + thighs: pivot moved onto the rear face,
# dock hangs at -110 with the tip 5 degrees further under. 12% of the dock stays inside
# the croup as the anchor, the tip is 0% buried, nothing touches a thigh. Box sizes and
# texOffs are unchanged, so the shipped skins still map -- and past -90 the dock's
# dorsal (+Y) face turns to face backwards, which is anatomically where it belongs.
zchain("body_croup", (0, 25.5, 22.5), [
    ("tail_dock", -110, 4.4, 1.2, 1.0, 1.4, 1.0),
    ("tail_tip",  -115, 4.0, 0.9, 0.8, 1.1, 0.9),
], "tail", parent_tilt=TILT["body_croup"])

# ================================================================ LEGS
# Outer faces step monotonically INBOARD down each limb and inner faces step
# monotonically OUTBOARD, so no two segments can ever share a side plane. The outer
# faces also have to clear the TORSO planes (barrel 6.5, croup 6.3, withers 6.1,
# chest 5.5): a first pass put the shoulder at 6.2 against the withers' 6.1 and the
# thigh at 6.6 against the barrel's 6.5, which the sweep found as 20u2 and 38u2 of
# shared surface. Both limb roots now sit clearly PROUD of the torso instead.
FORE_DROP = leg("fore", "body_chest", -5.0, [
    #  name        ang   L   outer inner  fr   bk   ov
    ("shoulder",     7, 9.4, 6.8, 2.2, 3.4, 2.6, 1.6),
    ("forearm",     -5, 7.8, 5.0, 2.5, 2.5, 2.1, 1.2),
    ("cannon",       0, 6.6, 4.6, 2.8, 1.5, 1.3, 1.0),
    ("hoof",         0, 2.6, 4.4, 3.0, 2.2, 1.2, 0.6),
], TILT["body_chest"])

HIND_DROP = leg("hind", "body_croup", 17.5, [
    ("thigh",       30, 9.5, 7.0, 1.6, 3.8, 3.2, 1.8),
    ("gaskin",     -40, 9.0, 5.9, 2.1, 2.6, 2.4, 1.4),
    ("cannon",      10, 8.0, 5.3, 3.0, 1.5, 1.3, 1.0),
    ("hoof",         0, 2.6, 5.1, 3.3, 2.2, 1.2, 0.6),
], TILT["body_croup"])

# ================================================================ ANTLERS
# Both families hang off a shared pedicle, so ModelDeer scales the whole chain with a
# single setScale + setShouldScaleChildren(true) on the pedicle.
# Local rz > 0 sends a part's distal end toward -x (the LEFT side); mirrored for right.
# PALMATE is authored at MEGALOCEROS proportions and scaled DOWN for the other two,
# because the three palmate species differ mostly in size, not in shape -- a uniform
# scale on the chain root can change size but never shape, so the biggest one has to
# be the one that is drawn correctly.
#
# EVERY antler part carries its OWN pivot at its base joint. The first pass shared one
# origin (the pedicle top) across the whole rack, so rotating a tine whose base sat 20
# units away swung the base clean off its parent -- the digitations floated 1.5-3.6u in
# mid air. A pivot buried inside the parent/child overlap makes the joint rotation-proof:
# both boxes straddle the pivot, so no angle can open a gap.
#
# The palm is TWO stepped panels, not one slab. A single 16x17 blade was the whole rack,
# which read as a flat sheet of cardboard; a narrower proximal panel that flares into a
# broader, thinner distal one gives the palm a taper and a silhouette, and the rooted
# digitations along its trailing edge are what actually say "Megaloceros".
PALMATE = [
    # name,      parent,       pivot dy from ped,  rot(x,y,z),    box authored in the
    #                                                             pedicle frame (x =
    #            (pivot offsets and box are both measured from    lateral thickness, y =
    #             `ped` in the PARENT's frame)                    out along the beam,
    #                                                             z = fore-aft breadth)
    ("palm_beam",  "pedicle",    (0, 0.0, 0.0),   (-8, 0, 58),
     (-1.9, 0.0, -2.1), (1.9, 8.0, 2.1)),
    ("palm_lower", "palm_beam",  (0, 6.8, 0.0),   (-4, -6, 12),
     (-1.5, 5.6, -5.2), (1.5, 14.5, 5.8)),
    ("palm_upper", "palm_lower", (0, 13.0, 0.0),  (0, -4, 10),
     (-1.1, 11.8, -8.4), (1.1, 21.0, 9.6)),
    ("palm_brow",  "palm_beam",  (0, 1.2, 0.0),   (-64, 14, 20),
     (-1.1, 0.4, -1.6), (1.1, 9.5, 1.6)),
    # digitations: three points off the trailing/top edge of the upper palm, each with
    # its base ~3.5u INSIDE the panel and its pivot inside that overlap.
    ("palm_digA",  "palm_upper", (0, 19.0, -6.0), (-12, 14, 4),
     (-0.9, 17.0, -7.8), (0.9, 25.5, -4.6)),
    ("palm_digB",  "palm_upper", (0, 19.5, 0.5),  (0, 0, 6),
     # 0.7, not 0.9: the middle digitation runs up the panel's own centreline, so at
     # 0.9 its side faces tipped to within 0.15u of the palm's and the sweep found
     # 4.7u2 of coincident face. The outer two are angled away and never get close.
     (-0.7, 17.5, -0.8), (0.7, 27.0, 2.4)),
    ("palm_digC",  "palm_upper", (0, 19.0, 6.5),  (10, -12, 8),
     (-0.9, 17.0, 5.2), (0.9, 25.0, 8.4)),
]
# BRANCHED is authored at WAPITI proportions: a swept beam with brow / trez tines and
# a terminal fork, towering well above the poll the way a bull elk's rack does. Same
# per-joint pivot rule -- the crown tines used to float 3.0-3.6u off the upper beam.
BRANCHED = [
    ("beam_low",   "pedicle",   (0, 0.0, 0.0),  (6, 0, 13),
     (-1.2, 0.0, -1.2), (1.2, 7.6, 1.2)),
    ("beam_up",    "beam_low",  (0, 6.4, 0.0),  (24, 0, 0),
     (-1.0, 5.6, -1.0), (1.0, 18.5, 1.0)),
    ("br_brow",    "beam_low",  (0, 1.2, 0.0),  (-72, 6, 10),
     (-0.8, 0.2, -0.8), (0.8, 7.4, 0.8)),
    ("br_trez",    "beam_low",  (0, 4.6, 0.0),  (-54, 8, 12),
     (-0.8, 3.2, -0.8), (0.8, 11.0, 0.8)),
    ("br_crownA",  "beam_up",   (0, 17.0, 0.0), (-40, 8, 4),
     (-0.7, 15.0, -0.7), (0.7, 22.5, 0.7)),
    ("br_crownB",  "beam_up",   (0, 16.5, 0.0), (30, -8, 6),
     (-0.7, 14.5, -0.7), (0.7, 21.5, 0.7)),
]
PED_TOP = None
for side, s in (("left", -1.0), ("right", 1.0)):
    # Pedicle: short stub on the skull roof, just behind the brow.
    # The pedicle also CANCELS the inherited head tilt. Everything above it is authored
    # straight up, but the chain reaching the skull sums to -16 degrees world
    # (chest +5, neck +29, +6, skull -46), which leant the whole rack out over the
    # muzzle. +10 here brings the rack back to +4, i.e. very slightly swept back.
    # Mirroring flips ry and rz, and the sign is -s, NOT s: rz > 0 swings a part's
    # distal end toward -x, which is the LEFT side, and left is s = -1. Getting this
    # backwards built both antlers on the same side of the skull -- palm_blade_left
    # measured x +3.47..+22.12 -- while every size metric still looked correct.
    add(f"pedicle_{side}", "head_skull", (s * 2.3, 27.0, -23.4), (10, 0, -s * 14),
        (s * 1.0, 26.2, -24.6), (s * 3.6, 30.2, -22.2), "antler")
    ped = (s * 2.3, 30.0, -23.4)         # antler chains start at the pedicle TOP
    for fam in (PALMATE, BRANCHED):
        for name, par, piv, rot, a, b in fam:
            parent = f"pedicle_{side}" if par == "pedicle" else f"{par}_{side}"
            # box coords are authored in a left-handed local frame with +y up and
            # x measured outward; mirror x for the right side.
            add(f"{name}_{side}", parent,
                (ped[0] + s * piv[0], ped[1] + piv[1], ped[2] + piv[2]),
                (rot[0], -s * rot[1], -s * rot[2]),
                (ped[0] + s * a[0], ped[1] + a[1], ped[2] + a[2]),
                (ped[0] + s * b[0], ped[1] + b[1], ped[2] + b[2]), "antler")

FAMILY = {"palmate": [n for n, *_ in PALMATE], "branched": [n for n, *_ in BRANCHED]}

# ================================================================ UV pack
# The raw footprint sum (16.3k px) suggested 256x128 would be too tight, but the shelf
# pack only needs 81 rows -- so the deer sits on the same canvas as the equid and the
# antelope. Trust the pack, not the fill-ratio estimate.
CANVAS = (256, 128)
ANTLER_SCALE = {}          # filled by main()'s size calibration


def footprint(p):
    """The region MC reserves from texOffs: 2*(w+d) x (h+d), in texels.

    CEIL, not round. The Cube constructor maps UVs from the raw float sizes passed to
    addBox, and these boxes are not integer-sized (4.6, 9.4, ...), so rounding down by
    half a texel lets a face sample its neighbour's paint.
    """
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    w, h, d = (abs(b[i] - a[i]) for i in range(3))
    if p[6] == "eye":
        w = 0.0
    return int(math.ceil(2 * (w + d))), int(math.ceil(h + d))


def uvpack():
    """Shelf-pack every box's true footprint, then ASSERT zero overlap.

    Every part gets its OWN region -- no mirror_uv sharing between left/right. There is
    room for it (25% raw fill), it avoids the side1/side2-runs-backwards class of bug,
    and it is REQUIRED for the eyes: the anti-flicker fix clears opposite UV halves on
    eye_left and eye_right, which is impossible if they share one rect.
    """
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
    """Dump the spec + UV pack as compact JSON for the Blockbench builder.

    Blockbench space IS the authoring space (Y-up, ground y=0, front -Z, left -X) and
    Blockbench groups/cubes rotate in ZYX order, which is exactly rmat()'s Rz@Ry@Rx and
    also exactly Minecraft's ModelPart order -- so nothing is reordered anywhere.
    """
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
    print(json.dumps({"canvas": list(CANVAS), "parts": out,
                      "family": FAMILY, "antler_scale": ANTLER_SCALE},
                     separators=(",", ":")))


# ================================================================ z-fight sweep
def zfight():
    """Run SOP phase 4 against the SPEC, before anything is built.

    scripts/zfight_sweep.py parses Model*.java, but its sweep() is frame-agnostic --
    it only needs parts carrying (rotationPoint, local box, radian rotation, parent).
    The spec stores absolute origins and absolute box corners, so the conversion is
    rp = origin - parent_origin and box = corners - origin. Catching a shared face
    here costs one script run; catching it after the Blockbench build and the Java
    port costs a full round trip through both.
    """
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

    # The two antler families never render together, so a shared face between a
    # palmate part and a branched part is not a defect -- no species draws both.
    fam = {n: "p" for n in FAMILY["palmate"]}
    fam.update({n: "b" for n in FAMILY["branched"]})

    def family(name):
        return fam.get(name.rsplit("_", 1)[0])

    print("=" * 80)
    print("Z-FIGHT SWEEP ON THE SPEC   (>=0.5u2 of genuinely coincident face)")
    print("=" * 80)
    shown = 0
    for area, na, nb, fa, fb, rel in Z.sweep(parts):
        if family(na) and family(nb) and family(na) != family(nb):
            continue
        shown += 1
        print(f"  {area:6.2f}u2  {na:<22} {'XYZ'[fa[0]]}  <->  {nb:<22} "
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
        print(f"{n:<22}{lo[n][0]:7.2f}..{hi[n][0]:<7.2f}"
              f"{lo[n][1]:7.2f}..{hi[n][1]:<7.2f}{lo[n][2]:7.2f}..{hi[n][2]:<7.2f}")

    print("\n" + "=" * 80)
    print("GROUND CONTACT + JOINT CHAIN")
    print("=" * 80)
    print(f"fore pivot height {FORE_DROP:6.2f}   hind pivot height {HIND_DROP:6.2f}")
    for n in sorted(wc):
        if "hoof" in n:
            print(f"  {n:<24} sole y {lo[n][1]:+6.3f}   z {lo[n][2]:6.2f}..{hi[n][2]:.2f}")
    floor = min(v[1] for v in lo.values())
    print(f"  lowest point of the whole model: {floor:+.3f}"
          + ("   OK" if floor > -0.02 else "   <== THROUGH THE FLOOR"))
    stance = ((lo["hind_left_hoof"][2] + hi["hind_left_hoof"][2]) / 2
              - (lo["fore_left_hoof"][2] + hi["fore_left_hoof"][2]) / 2)
    withers = hi["body_withers"][1]
    print(f"  fore->hind stance {stance:.2f}u = {stance/withers:.2f} x withers"
          f"   (a square-standing deer is ~0.8)")
    print(f"  hind zigzag: stifle z {(lo['hind_left_gaskin'][2]+hi['hind_left_gaskin'][2])/2:.2f}"
          f"  hock z {(lo['hind_left_cannon'][2]+hi['hind_left_cannon'][2])/2:.2f}")

    # ---- eye clearance (SOP phase 4 -- check EVERY wider ancestor) ----------
    print("\n" + "=" * 80)
    print("EYE CLEARANCE  (must sit outside every ancestor box, not just the skull)")
    print("=" * 80)
    for side in ("left", "right"):
        eye = wc[f"eye_{side}"]
        c = eye.mean(0)
        for anc in ("head_skull", "neck_upper", "neck_lower", "body_chest"):
            p = BY_NAME[anc]
            l = local_of(anc, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gap = max(max(a[i] - l[i], l[i] - b[i]) for i in range(3))
            axis = "xyz"[int(np.argmax([max(a[i] - l[i], l[i] - b[i]) for i in range(3)]))]
            print(f"  eye_{side:<6} vs {anc:<12} clear by {gap:6.2f} on {axis}"
                  + ("" if gap > 0.3 else "   <== TOO CLOSE"))

    # ---- size calibration --------------------------------------------------
    print("\n" + "=" * 80)
    print(f"SIZE CALIBRATION   mesh withers = {withers:.2f}u,  scale = m*16/withers")
    print("=" * 80)
    head_top = max(hi["head_skull"][1], hi["ear_left"][1])
    nose = lo["head_muzzle"][2]
    rear = max(hi["body_croup"][2], hi["tail_dock"][2])
    def rack(fam, asc):
        """World AABB of one antler chain under setScale(asc) on the pedicle.

        AdvancedModelBox builds translate(rp) . rotate . scale, so the scale acts in
        the part's own pre-rotation frame about its rotation point. Reproduce that
        exactly -- pulling the points into the pedicle's authoring frame, scaling
        about its origin, and pushing them back out -- rather than scaling world
        coordinates about a guessed centre.
        """
        o = np.asarray(BY_NAME["pedicle_left"][2], float)
        out = []
        for n in [f"{k}_left" for k in FAMILY[fam]] + ["pedicle_left"]:
            p = local_of("pedicle_left", wc[n])
            out.append(world("pedicle_left", o + asc * (p - o)))
        return np.vstack(out)

    print(f"{'species':<13}{'m':>6}{'scale':>7}{'withers':>9}{'headtop':>9}"
          f"{'antlerScale':>12}{'antler W':>10}{'antler top':>11}{'length':>8}")
    ANTLER_SCALE.clear()
    for name, var, real, fam, span_m in SPECIES:
        s = real * 16.0 / withers
        # Solve setScale for the real antler span; span(asc) is affine in asc
        # because the chain scales about a pedicle that is off the centreline.
        w0 = 2 * abs(rack(fam, 0.0)[:, 0]).max() * s / 16
        w1 = 2 * abs(rack(fam, 1.0)[:, 0]).max() * s / 16
        asc = round((span_m - w0) / (w1 - w0), 3)
        ANTLER_SCALE[name] = asc
        r = rack(fam, asc)
        print(f"{name:<13}{real:6.2f}{s:7.3f}{withers*s/16:9.2f}{head_top*s/16:9.2f}"
              f"{asc:12.3f}{2*abs(r[:, 0]).max()*s/16:10.2f}"
              f"{r[:, 1].max()*s/16:11.2f}{(rear-nose)*s/16:8.2f}")
    print(f"registered hitbox {HITBOX[0]} x {HITBOX[1]} blocks"
          "   (per-TYPE, so the biggest species should sit close to it)")

    # ---- silhouette --------------------------------------------------------
    for label, drop in (("PALMATE  (megaloceros / stag_moose / moose)", FAMILY["branched"]),
                        ("BRANCHED (wapiti / red_deer)", FAMILY["palmate"])):
        pts = np.vstack([world(p[0], vol(p)) for p in PARTS
                         if p[6] != "eye"
                         and p[0].rsplit("_", 1)[0] not in drop])
        print("\n" + "=" * 80)
        print(f"SIDE SILHOUETTE -- {label}      <- nose      tail ->")
        print("=" * 80)
        zmin, zmax, ymax = pts[:, 2].min(), pts[:, 2].max(), pts[:, 1].max()
        W, H = 76, 30
        g = [[" "] * W for _ in range(H)]
        for _, y, z in pts:
            c = int((z - zmin) / (zmax - zmin) * (W - 1))
            r = int((ymax - y) / ymax * (H - 1))
            if 0 <= r < H:
                g[r][c] = "#"
        for r, row in enumerate(g):
            print(f"{ymax - r*ymax/(H-1):5.1f} |" + "".join(row))
        print(f"      z {zmin:.1f} .. {zmax:.1f}")

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
    for cw, ch in ((256, 128), (256, 256), (512, 256)):
        print(f"  {cw}x{ch} = {cw*ch:>6} px -> "
              f"{'FITS' if tot < cw*ch*0.72 else 'TOO TIGHT'} ({100*tot/(cw*ch):.0f}% raw)")


if __name__ == "__main__":
    import sys
    if "--zfight" in sys.argv:
        zfight()
    elif "--uv" in sys.argv:
        uvpack()
    elif "--json" in sys.argv:
        emit_json()
    else:
        main()


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
    "fore": ("body_chest", TILT["body_chest"],
             [("shoulder", 9.4), ("forearm", 7.8), ("cannon", 6.6), ("hoof", 2.6)]),
    "hind": ("body_croup", TILT["body_croup"],
             [("thigh", 9.5), ("gaskin", 9.0), ("cannon", 8.0), ("hoof", 2.6)]),
}


def _leg_rot(prefix, side, angles):
    """World angles -> the local rotation each segment must carry."""
    _, tilt, segs = LEGS[prefix]
    over, prev = {}, tilt
    for (nm, _), a in zip(segs, angles):
        over[f"{prefix}_{side}_{nm}"] = (a - prev, 0.0, 0.0)
        prev = a
    return over


def _leg_score(prefix, angles, dy):
    """Penalty for a folded leg: nothing through the floor, distal segments ON it."""
    over = _leg_rot(prefix, "left", angles)
    pen = 0.0
    _, _, segs = LEGS[prefix]
    for k, (nm, _) in enumerate(segs):
        full = f"{prefix}_left_{nm}"
        w = world_mod(full, vol(BY_NAME[full]), over, dy)
        lo = w[:, 1].min()
        if lo < 0.0:
            pen += (-lo) ** 2 * 12.0                      # never through the floor
        if k >= 2:                                        # cannon + hoof must lie ON it
            pen += (lo - 0.15) ** 2 * 3.0
    # distal segments want to be roughly horizontal, i.e. |cos(world angle)| small
    for a in angles[2:]:
        pen += (math.cos(D(a)) ** 2) * 4.0
    return pen


def pose_solve():
    rng = np.random.default_rng(7)
    print("=" * 80)
    print("PHASE 7 -- POSES SOLVED AGAINST REAL BOX GEOMETRY")
    print("=" * 80)
    body_lo = world("body_barrel", corners(BY_NAME["body_barrel"]))[:, 1].min()
    out = {}
    for pose, belly in (("sit", 1.6), ("sleep", 0.7)):
        dy = belly - body_lo
        print(f"\n{pose.upper()}   body drop dy = {dy:+.2f}u  (belly rests at y={belly})")
        for prefix in ("fore", "hind"):
            base = np.array([40.0, -70.0, 95.0, 95.0] if prefix == "fore"
                            else [70.0, -95.0, 80.0, 80.0])
            best, bs = base.copy(), _leg_score(prefix, base, dy)
            span = np.array([55.0, 55.0, 45.0, 45.0])
            for it in range(9):
                cand = best + rng.normal(0, 1, (4000, 4)) * span
                cand[:, 3] = cand[:, 2] + rng.normal(0, 6, 4000)   # hoof follows cannon
                for c in cand:
                    s = _leg_score(prefix, c, dy)
                    if s < bs:
                        bs, best = s, c.copy()
                span *= 0.62
            out[(pose, prefix)] = (best, dy)
            print(f"  {prefix:<5} world angles " +
                  " ".join(f"{a:7.2f}" for a in best) + f"   penalty {bs:.4f}")
            for side in ("left", "right"):
                over = _leg_rot(prefix, side, best)
                for nm, rot in over.items():
                    w = world_mod(nm, vol(BY_NAME[nm]), over, dy)
                    print(f"      {nm:<24} localRotX {rot[0]:+8.3f} deg"
                          f"   y {w[:,1].min():6.2f}..{w[:,1].max():6.2f}")
                break
    return out
