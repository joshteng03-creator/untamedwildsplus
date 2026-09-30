#!/usr/bin/env python3
"""Giraffid model spec + forward-kinematics checker -- SOP Phase 1 / 1b / 2.

`giraffid` is one of the last raw ModelBison forks left in the tree.  The shipped
ModelGiraffid still declares bison horns (and hides them with setScale(0,0,0) every
frame), a bison beard, a bison forelock and bison limb wool, with two 1x3x1 ossicone
sticks bolted onto a bison skull -- i.e. the single most neck-defined animal on earth is
currently rendered on a body plan chosen for a buffalo.  This replaces it wholesale.

Authoring space is BLOCKBENCH project space: Y up, ground y=0, -Z = front (nose),
-X = left.  Group origins and cube from/to are ABSOLUTE project coordinates, exactly as
Blockbench stores them, so this table doubles as the data for the build script.

    world(p) = A_root(A_child(... p))      A_g(p) = R_g @ (p - o_g) + o_g

Rotation order is Rz @ Ry @ Rx -- Blockbench's, and MC's ModelPart's under the verified
bbRot = [-rx, -ry, +rz] export flip.

GIRAFFID SILHOUETTE -- three things carry the whole family and nothing else matters much:
  * THE NECK, four segments rising at 70/74/76/58 degrees and curving forward into the
    head at the top (the nuchal curve).  25.5u of rise on a 40u withers.
  * THE DOWNHILL TOPLINE.  A giraffe's withers stand ~7u above its croup and the back
    falls away continuously to a low-set tail.  This is the exact INVERSE of the tapir
    (high rounded rump) and it is what stops the two rigs reading as one retuned body.
  * LONG STRAIGHT LEGS.  Belly at 60% of withers, against the tapir's 47% and the
    macrauchenia's 58%.  Fore drop > hind drop, which is why the topline tips downhill in
    the first place.

SEVEN species off one mesh.  giraffid.json declares no synced flag beyond groupCount (and
EntityGiraffid will not add one), so everything routes through getVariant() -- the
ModelDeer:509-524 / ModelTapir pattern:

  * NECK LENGTH is per-species, and it is the reason this rig can carry an okapi and a
    giraffe at all.  Each neck segment is scaled along its OWN local Z (its length axis)
    and every child's rotation point is scaled with it, so the chain shortens without
    shear.  Scaling only Z and moving the pivots by hand -- rather than
    setShouldScaleChildren on the chain root -- is deliberate: a non-uniform scale
    propagated through children that are themselves rotated shears every segment
    diagonally, and the render still looks vaguely neck-shaped while being wrong.
    A matching XY THICKEN goes the other way (a short giraffid neck is a bull neck).
  * OSSICONE FAMILY, showModel-gated, four families plus hornless.  This is the antelope
    horn-family mechanism: showModel on the chain ROOT, because ModelPart.render returns
    before recursing into children when `visible` is false.

Run:  python scripts/giraffid_spec.py            full report
      python scripts/giraffid_spec.py --uv       UV shelf pack only
      python scripts/giraffid_spec.py --zfight   z-fight sweep on the spec
      python scripts/giraffid_spec.py --burial   volume-burial check
      python scripts/giraffid_spec.py --species  per-species neck sweep
      python scripts/giraffid_spec.py --pose     phase-7 sit/sleep solve
      python scripts/giraffid_spec.py --json     compact JSON for the BB builder
"""
import math
import os

import numpy as np

D = math.radians

# ---------------------------------------------------------------------------
# NECK-FACTOR PREVIEW HOOK
# ---------------------------------------------------------------------------
# Set GIRAFFID_NF=<factor> to emit the rig with a SHORTENED neck, exactly as the Java applies
# it at runtime. This exists because Blockbench never runs setupAnim: every species previews
# with the giraffe's full-length neck, so the one genuinely novel mechanism on this rig -- the
# per-species neck -- is the one thing a screenshot cannot check. Rebuilding from the spec with
# the factor baked in is the only honest way to see the short-necked silhouette before
# runClient, and it reproduces the runtime transform rather than approximating it: segment
# lengths scale by nf along the chain axis, and the cross-section thickens by the same
# 1 + (1-nf)*0.45 the model uses.
#
# For SILHOUETTE checks only. It changes every box size, so the UV pack differs from the
# shipped one and a painted skin will not line up -- preview it in flat grey.
NF = float(os.environ.get("GIRAFFID_NF", "1.0"))
NTHICK = 1.0 + (1.0 - NF) * 0.45

# ---------------------------------------------------------------- contract
# name, variant, real withers m, neck factor, ossicone family, ossicone scale
#
# NECK FACTOR is a RATIO OF RATIOS, not a guess: (species neck / species withers) divided
# by the same quotient for the giraffe.  Species `scale` already carries absolute size, so
# what the mesh has to express is how long the neck is *for an animal that height* --
# which is the only part of giraffid proportion that species scale cannot reach.
#     giraffe        2.20 m neck / 3.30 withers = 0.667  -> 1.00
#     helladotherium 1.20 / 2.30 = 0.522                 -> 0.78
#     samotherium    1.00 / 1.90 = 0.526                 -> 0.79
#     palaeotragus   0.90 / 2.00 = 0.450                 -> 0.68
#     okapi          0.65 / 1.50 = 0.433                 -> 0.65
#     bramatherium   0.70 / 1.80 = 0.389                 -> 0.58
#     sivatherium    0.80 / 2.20 = 0.364                 -> 0.55
SPECIES = [
    ("giraffe",        0, 3.30, 1.00, "TAPERED", 1.00),   # Giraffa camelopardalis
    ("sivatherium",    1, 2.20, 0.55, "PALMATE", 1.00),   # Sivatherium giganteum
    ("okapi",          2, 1.50, 0.65, "SPIKE",   0.50),   # Okapia johnstoni -- forest, striped
    ("samotherium",    3, 1.90, 0.79, "SPIKE",   1.00),   # Samotherium boissieri
    ("bramatherium",   4, 1.80, 0.58, "QUAD",    1.00),   # Bramatherium perimense
    ("helladotherium", 5, 2.30, 0.78, "NONE",    0.00),   # Helladotherium duvernoyi
    ("palaeotragus",   6, 2.00, 0.68, "SPIKE",   0.75),   # Palaeotragus rouenii
]
# WIDTH STAYS AT THE SHIPPED 1.6; ONLY THE HEIGHT MOVES, 2.4 -> 3.4.
# The widest species measures 1.49 blocks, so 1.6 already fits and changing it would perturb
# pathing for no gain. 2.4 does not: the giraffe stands 3.30 at the withers and 5.96 to the
# ossicones, so a 2.4 box covered under half the animal. 3.4 covers the body and the base of
# the neck. It deliberately does NOT cover the whole neck -- a ~6-block hitbox would leave a
# giraffe unable to path anywhere, and would be grotesque on the okapi, which shares the
# per-TYPE box at 1.50m. That mismatch is the documented dwarf-mammoth tradeoff: getMobSize()
# scales the rendered model only and EntityGiraffid has no getScale() override.
HITBOX = (1.6, 3.4)      # ModEntity.createEntity(...,"giraffid", 1.6F, 3.4F, ...)

# Every ossicone box, keyed by family. showModel is driven off these lists, and the
# generator reads them so the Java can never disagree with the spec about which box
# belongs to which family.
FAMILIES = {
    "TAPERED": ["oss_taper_left", "oss_taper_right", "oss_boss"],
    "PALMATE": ["oss_palm_base_left", "oss_palm_base_right",
                "oss_palm_left", "oss_palm_right"],
    "SPIKE":   ["oss_spike_left", "oss_spike_right"],
    "QUAD":    ["oss_quad_left", "oss_quad_right",
                "oss_quad_front_left", "oss_quad_front_right"],
    "NONE":    [],
}
# Roots that carry a child, so showModel on the root is enough (and setScale needs
# setShouldScaleChildren on these and only these).
FAMILY_ROOTS = {"oss_palm_base_left": "oss_palm_left",
                "oss_palm_base_right": "oss_palm_right"}

NECK_SEGS = ["neck_1", "neck_2", "neck_3", "neck_4"]

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
# Four nested masses.  Side planes step monotonically inboard from the barrel:
#     barrel 7.6 > croup 7.2 > withers 7.0 > chest 6.2
# A giraffe is DEEP and NARROW -- laterally compressed, unlike every other big herbivore
# on this rig list -- so the whole torso is ~1u/side slimmer than the tapir's at nearly
# twice the height.
#
# THE TOPLINE FALLS FROM FRONT TO BACK, and that is the whole point.  On the tapir the
# croup deliberately peaks ABOVE the withers; here the relationship is inverted and the
# main() report asserts the sign, because getting it backwards produces a perfectly
# plausible-looking grey render of an animal that is not a giraffid.
#     tops     withers 39.6(front) > barrel 36.0 > croup 33.2(front, 30.4 rear)
#     bottoms  withers 27.0 > croup 25.0 > barrel 23.5 > chest 24.6
#
# The barrel runs to z=15 and the croup to 23, giving a fore->hind stance of 0.58 x withers.
# At the first pass the barrel stopped at 13 and the hind pivot sat at 15, which put the
# feet only 0.46 x withers apart -- a giraffe's back IS short, but not that short, and the
# animal read as though it were standing on tiptoe with all four feet under one point.
add("body_barrel", None, (0, 31.0, 4.0), (0, 0, 0),
    (-7.6, 23.5, -6.0), (7.6, 36.0, 15.0), "body")
# The withers hump: TALL, so its lower 9u hide inside the barrel and only the top emerges.
# That is what makes the shoulder rise read as one continuous line into the neck instead of
# a step.  Origin sits at the REAR of the box and rx is POSITIVE, so the rotation lifts the
# FRONT and leaves the rear tucked under the barrel's top -- the two never cross.
# It reaches back to z=7, not 3.5, and its top is 37.4, not 38.6. The first build put the
# withers' rear top at 38.6 against a barrel top of 36.0 and the croup's front top at 33.1,
# so the topline came down two hard 2.6u and 2.9u shelves and the grey render showed a
# staircase where a giraffe has the smoothest back line of any large mammal. Reaching
# further back and dropping the rear brings the withers out at 36.85 -- 0.85 above the
# barrel, a step small enough to read as one line, and still 0.85 clear of the sweep.
add("body_withers", "body_barrel", (0, 31.0, 3.5), (8, 0, 0),
    (-7.0, 27.0, -11.0), (7.0, 37.4, 7.0), "body")
# Deep narrow brisket, tilted nose-down. On a giraffe the chest is the deepest part of the
# animal and the neck launches off the top of it, so unlike the tapir there is no risk of
# the chest becoming "the front of the animal" -- the head is 25u above it.
add("body_chest", "body_barrel", (0, 31.0, -5.0), (-8, 0, 0),
    (-6.2, 24.6, -12.0), (6.2, 36.6, -1.0), "body")
# Croup: LOW and falling away. Origin at its own FRONT face so the +16 only ever drops the
# rear, and its unrotated front top (33.2) is already 6.7u below the withers.
# Front top RAISED to 35.0 (was 33.2) for the same reason: it now sits 1.0u under the
# barrel instead of 2.9u, closing the second shelf. The +16 tilt still drops the rear to
# 32.1, so the rump falls 6.4u below the withers -- the fall is delivered by the TILT, not
# by starting the box low, which is what lets the topline stay continuous.
add("body_croup", "body_barrel", (0, 31.0, 13.0), (16, 0, 0),
    (-7.2, 25.0, 13.0), (7.2, 35.0, 23.0), "body")

TILT = {"body_chest": -8.0, "body_croup": 16.0, "body_withers": 8.0}


# ================================================================ chain helpers
def zchain(parent, pivot, segs, cls, parent_tilt=0.0):
    """Chain authored lying forward along -Z from `pivot`; bent by WORLD angles.

    A POSITIVE angle raises the forward end.  EVERY CHILD OF A SEGMENT MUST BE AUTHORED IN
    THIS REST FRAME, not at its apparent world position -- the chain is laid out colinear
    along -Z and only then rotated.  For a near-vertical giraffe neck the rest frame and
    the world frame are ~70 degrees apart, so this trap is far sharper here than on any
    previous animal: the mane belongs on each segment's local +Y face, which after the
    rotation points BACKWARD in world -- which is exactly where a giraffe's mane is.
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

    The sole lands on y = 0 when the pivot's WORLD height equals sum(L*cos(angle)); the
    AUTHORED height is not that number, because the parent's own tilt drags the pivot off
    it.  Solve rather than assume -- and on this animal the legs are 30u long, so a 1u
    error in the pivot is a hoof visibly buried in or floating over the ground.
    """
    drop = sum(L * math.cos(D(a)) for _, a, L, *_ in segs)
    y_auth = drop
    for _ in range(24):
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
# FOUR segments at 70/74/76/58 world degrees. The chain rises 25.5u off a 37u shoulder and
# then curves FORWARD at the top -- the nuchal curve is what makes it read as a neck under
# tension rather than a flagpole.
#
# The 58 on neck_4 is doing real work in two places. It bends the top of the neck forward
# into the head, and it keeps the neck_4 -> head_skull joint at 28 degrees rather than the
# 45 it would be at if neck_4 stayed near 76. Chained joints crack open a wedge gap the
# moment they animate, and 45 degrees of relative bend on a 3u-half-width box needs more
# overlap than the box is long.
#
# Rest-frame anchors for anything parented into this chain -- author children HERE, not at
# their apparent world position. All at about y 37, x 0:
#     neck_1  z -16.0..-6.4    neck_2  z -23.0..-13.6    neck_3  z -29.8..-20.6
#     neck_4  z -36.4..-27.4   head_skull z -44.4..-34.2  head_muzzle z -49.4..-42.6
#
# HALF-WIDTHS STEP MONOTONICALLY INBOARD DOWN THE WHOLE CHAIN, and here that is not a
# nicety, it is the only available fix. A box's X-face normal is UNTOUCHED by rx, so the
# 28-degree pitch break between neck_4 and head_skull leaves their side planes exactly
# parallel: at 3.3 half-width each they were coincident and the sweep found 13.25u2 running
# the length of the skull. Same mechanism gave head_jaw/head_muzzle 13.65u2 at 2.3 vs 2.4.
# Every step below is >= 0.4, comfortably past the sweep's 0.15u plane tolerance:
#     4.6 > 4.1 > 3.7 > 3.4 > 2.9 (skull) > 2.4 (muzzle) > 1.9 (jaw)
#
# Segment lengths total 25.8u, not the first pass's 27.4. At 27.4 the giraffe topped out at
# 6.03 blocks against a real bull's ~5.5m; trimming 1.6u of neck brings it to 5.6 while
# leaving neck/withers at 0.65, still inside the 0.65-0.70 band a giraffe actually occupies.
#
# THE PER-SEGMENT STEPS ARE 0.25/0.2, NOT 0.5/0.3. The taper still has to step -- rx does
# not touch an X-face normal, and consecutive segments sit 2-4 degrees apart, so a shared
# half-width z-fights down the length of the neck exactly as the mane's did. But at 0.5 per
# joint the grey render came back BANDED: four visible ledges where each segment emerged
# from the last, so the most important shape on the animal read as a stack of blocks rather
# than a taper. 0.25 is still 0.10 clear of the sweep's 0.15u plane tolerance while halving
# the visible ledge, and the total 4.60 -> 3.85 taper across four joints is unchanged in
# aggregate. The real narrowing the eye reads is the 3.85 -> 2.90 drop into the skull.
#
# THE ANGLES ARE 64/73/82/62 AND THE SPACING BETWEEN THEM IS DELIBERATE. Narrowing the
# up/dn steps to 0.2 alongside the half-widths immediately produced 11.02 and 10.19u2
# between neck_1/neck_2 and neck_2/neck_3 -- their Y faces. A 0.2 gap is nominally clear of
# the 0.15u tolerance, but the segments were only 4 and 2 degrees apart, and 2 degrees over
# a 6.6u segment walks a plane 0.23u: the gap closes to zero somewhere inside the overlap.
# Thickening the steps back up would have restored the banding this pass exists to remove,
# so the fix is the other variable -- every joint now turns >= 9 degrees, clear of the
# sweep's 8-degree ANGLE_TOL, which makes the whole question moot. It also buys the profile
# a gentle backward curve as it rises before the head comes forward, which is what a
# giraffe's neck actually does and what a constant-angle pole never reads as.
NECK_PIVOT = (0, 36.6, -9.0)
NECK_SPEC = [
    #  name       ang    L         hw            up          dn          ov
    ("neck_1",     64,  6.6 * NF, 4.60 * NTHICK, 3.6 * NTHICK, 3.4 * NTHICK, 2.6 * NF),
    ("neck_2",     73,  6.6 * NF, 4.35 * NTHICK, 3.4 * NTHICK, 3.2 * NTHICK, 2.4 * NF),
    ("neck_3",     82,  6.4 * NF, 4.10 * NTHICK, 3.2 * NTHICK, 3.0 * NTHICK, 2.4 * NF),
    ("neck_4",     62,  6.2 * NF, 3.85 * NTHICK, 3.0 * NTHICK, 2.8 * NTHICK, 2.4 * NF),
]
# neck_4 62 / skull 36 / muzzle 24, not 58/30/16. The chain's cumulative bend is the same
# but it is distributed: at 58/30 the neck_4 -> skull joint took 28 degrees in one step and
# the profile showed a distinct elbow just below the ears. 62/36/24 spreads it 14/26/12 and
# lifts the muzzle to the angle a browsing giraffe actually carries it.
HEAD_SPEC = [
    ("head_skull", 36,  8.2, 2.90, 3.4, 3.0, 2.4),
    ("head_muzzle", 24, 5.0, 2.40, 2.4, 2.5, 1.8),
]
zchain("body_chest", NECK_PIVOT, NECK_SPEC + HEAD_SPEC, "head",
       parent_tilt=TILT["body_chest"])

# Long slender mandible, child of head_skull so the muzzle's extra 14 degrees cannot crack
# a gap open along the tooth row. Stops 1.6u behind the muzzle's own front face: two
# near-parallel front planes a few tenths apart and DIVERGING is a guaranteed shimmer.
add("head_jaw", "head_skull", (0, 35.2, -38.0), (0, 0, 0),
    (-1.9, 33.0, -46.6), (1.9, 36.4, -35.0), "head")

# Large ears, set high and swept up and OUT. A giraffe's ears are conspicuous and they are
# most of what gives the head its width from the front.
# rz SIGN: R_z sends a point at +x toward +y, so a tip authored at -x (the LEFT ear) only
# rises under a NEGATIVE rz. Getting this backwards rolls both ear tips DOWN into the skull
# and the render still looks plausible -- it cost a full round trip on the toxodon.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"ear_{side}", "head_skull", (s * 2.6, 39.2, -36.0), (10, s * -20, s * 42),
        (s * 2.6, 38.0, -37.6), (s * 7.4, 40.6, -34.4), "ear")

# Eyes: whole-unit 2.0 x 2.0 zero-width planes, blinked by retracting them INTO the skull.
# Whole-unit and not fractional so the outward and inward halves land on separate texel
# rects -- at a fractional depth alpha-0ing one half eats a column of the other and the two
# sides end up with visibly different eyes from symmetric geometry.
# Set HIGH and FORWARD on the skull, which is where a giraffe's eyes actually are (they are
# famously large and placed for all-round vision) and which also keeps them clear of the
# jaw. Unlike the equid there is no wider ancestor to hide inside -- the neck is 20u below.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"eye_{side}", "head_skull", (s * 3.4, 38.0, -40.6), (0, 0, 0),
        (s * 3.4, 37.0, -41.6), (s * 3.4, 39.0, -39.6), "eye")

# ================================================================ MANE
# One blade per neck segment, authored in that segment's OWN rest frame on its local +Y
# face with rotation ZERO. Both halves of that sentence are load-bearing:
#   * rot 0, because a part that should simply follow its parent must not repeat the
#     parent's angle -- the equid's mane was given the neck's -40 a second time on top of
#     inheriting it, which threw the crest onto the muzzle, and the screenshot still looked
#     plausible because the neck's own top edge read as a crest.
#   * local +Y, because at a 70-degree lean local "up" is NOT the dorsal face. Here local
#     +Y maps to world backward-and-slightly-up, which is precisely where the dorsal crest
#     of a vertical neck is.
# Per segment, so it bends with the neck instead of shearing off it. Its base is buried 1.2u
# inside the neck so no gap can open where it meets the shoulder.
#
# THE HALF-WIDTHS TAPER 1.10 > 0.85 > 0.62 > 0.40, and the z overlap between consecutive
# blades is cut to 0.15u. Both are forced by the same mechanism as the skull: consecutive
# neck segments are only 2-4 degrees apart, and rx does not touch an X-face normal at all,
# so four blades at a common 0.8 half-width had exactly coincident sides -- 6.11 + 6.05 +
# 1.91u2 of shimmer running the length of the mane. A mane that narrows toward the head is
# what a giraffe actually has, so the fix costs nothing.
# The blade STANDS 3.4u proud at the withers and tapers to 2.2 at the head, on 0.9u of
# burial rather than 1.2. At 1.2/2.4 the burial check put mane_1 at 70% and mane_2 at 55%
# swallowed -- the base segment emerges right where the withers hump is, so it loses depth
# to the withers box as well as to its own neck segment, and only ~1u of a 3.6u blade
# actually showed. A mane that is tallest over the withers is also what a giraffe has.
MANE_HW = [1.10, 0.85, 0.62, 0.40]
MANE_UP = [3.4, 3.0, 2.6, 2.2]
for (nm, _, L, hw, up, dn, ov), mhw, mup in zip(NECK_SPEC, MANE_HW, MANE_UP):
    o = BY_NAME[nm][2]
    add(f"mane_{nm[-1]}", nm, (o[0], o[1] + up, o[2] - L / 2), (0, 0, 0),
        (-mhw, o[1] + up - 0.9, o[2] - L - 0.5 * NF),
        (mhw, o[1] + up + mup, o[2] + 0.5 * NF), "mane")
# The 0.5 z overhang at each end is a MINIMUM, not a style choice. At 0.15 the mane's front
# plane sat exactly 0.15u past its own neck segment's front plane -- the sweep's PLANE_TOL to
# the digit -- and mane_3/neck_3 and mane_4/neck_4 came back at 1.03 and 0.89u2. It also
# gives consecutive blades a 1.0u overlap, which is what a chained joint needs anyway.

# ================================================================ TAIL
# Long thin dock + the diagnostic black tassel. Set on LOW off the falling croup.
# Authored as explicit boxes straddling their pivots with a NEGATIVE rx, never via zchain:
# zchain lays a segment forward along -Z and then rotates, so a tail needs an angle past
# -90 to end up behind its pivot and sweeps through the rump on the way. Built that way the
# z-fight sweep reports NOTHING while most of the tail sits buried inside the croup -- 64%
# of the toxodon's dock was lost exactly like that. Use --burial, not --zfight, to check.
# The tassel's relative angle is -14, not -8. At -8 the two Z faces sat exactly 8.0 degrees
# apart -- the sweep's ANGLE_TOL to the digit -- and it reported 2.29u2. A droopier tassel
# is also simply more correct.
add("tail_dock", "body_croup", (0, 31.4, 21.8), (-24, 0, 0),
    (-1.2, 20.0, 21.0), (1.2, 32.4, 23.4), "tail")
add("tail_tassel", "tail_dock", (0, 21.6, 22.2), (-14, 0, 0),
    (-1.7, 14.6, 21.4), (1.7, 22.4, 23.0), "tail")

# ================================================================ LEGS
# LONG and straight -- 32u of foreleg under a 40u withers, belly at 60%. The giraffe is the
# longest-legged animal in the mod by a wide margin and the joints are deliberately subtle:
# a giraffe's foreleg reads as one nearly straight column, so the angles stay under 10
# degrees per joint at the front and the visible bend is all at the hind stifle and hock.
#
# FORE DROP > HIND DROP by ~4u, and that is not a rounding artefact -- it is the mechanical
# reason the topline tips downhill, and it must survive any later tuning.
#
# Lateral planes step monotonically inboard so no two boxes that overlap in projection come
# within 0.2u of sharing one. INNER faces need the same treatment as outer ones -- two
# segments sharing an inner plane z-fight exactly as readily, and the tail dock threading
# between the thighs is the pair that bites:
#   fore  outer 8.2 / 7.0 / 5.8 / 6.4   inner 2.2 / 2.9 / 3.6 / 3.0
#   hind  outer 8.2 / 6.9 / 5.6 / 6.2   inner 1.8 / 2.6 / 3.4 / 2.8
# `fr`/`bk` are stepped too: every segment of a leg is authored around the SAME pivot_z, so
# equal `bk` values put two segments' rear planes at an identical z BY CONSTRUCTION.
# Both limb roots sit PROUD of the barrel (8.2 vs 7.6): a shoulder tucked just inside the
# torso plane is the pairing that produced 20-38u2 of shared face on the deer.
FORE_PIVOT_Z = -5.0          # also where the withers height is measured (see main())
FORE_DROP = leg("fore", "body_chest", FORE_PIVOT_Z, [
    #  name        ang    L    outer inner  fr   bk   ov
    ("shoulder",     7, 11.0, 8.2, 2.2, 5.5, 4.5, 1.6),
    ("forearm",     -7,  9.0, 7.0, 2.9, 4.4, 3.6, 1.4),
    ("cannon",       3,  9.2, 5.8, 3.6, 3.2, 2.6, 1.2),
    ("foot",         0,  3.0, 6.4, 3.0, 4.6, 1.4, 0.9),
], TILT["body_chest"])

# THE THIGH'S `fr` AND `bk` BOTH HAD TO MOVE, and only the second attempt found the real
# pair. At bk 5.6 the thigh's REAR plane landed at z 23.1 against the croup's own rear face
# at 23.0 -- 31.44u2 per side, the largest patch in the model. Cutting bk to 3.4 cleared
# that and the sweep still reported 31.44u2, because the pivot sits at 18.0 and fr was 5.0,
# which put the thigh's FRONT plane at exactly 13.0 -- the croup's front face, to the digit.
# The two planes are 2 degrees apart in world (croup 16, thigh 18), so both were genuine.
# fr 6.5 sends the thigh 1.5u FORWARD of the croup instead, tucking it under the barrel,
# which is where a giraffe's upper thigh actually sits. Being a parent/child pair does not
# make a shared plane harmless; that tag only records that the overlap was deliberate.
HIND_DROP = leg("hind", "body_croup", 18.0, [
    ("thigh",       18, 10.0, 8.2, 1.8, 6.5, 3.4, 1.8),
    ("gaskin",     -24,  8.4, 6.9, 2.6, 4.2, 4.0, 1.5),
    ("cannon",       9,  8.6, 5.6, 3.4, 3.0, 2.6, 1.2),
    ("foot",         0,  2.8, 6.2, 2.8, 4.4, 1.4, 0.9),
], TILT["body_croup"])

# ================================================================ OSSICONES
# Four families on one skull, plus hornless, selected by showModel on the chain ROOT.
# This is the ModelAntelope horn-family mechanism, and the reason it works is that
# ModelPart.render returns before recursing into children when `visible` is false, so
# hiding a root hides its whole subtree.
#
# SEGMENT-TO-SEGMENT TWIST IS LOAD-BEARING. A box's X-face normal is untouched by rx, so a
# pair of boxes that differ only in backsweep leaves consecutive side planes near-parallel,
# and the antelope sweep found 1.0-1.4u2 at every such joint. Every joint below turns >8
# degrees of yaw or roll as well as pitch.
#
# THE rz SIGN CONVENTION IS THE OPPOSITE OF THE EAR'S, AND GETTING IT WRONG COST A FULL
# PASS HERE. The rule in the SOP -- "a tip authored at -x rises under a NEGATIVE rz" -- is
# about a part that EXTENDS ALONG X, like an ear. An ossicone extends along +Y, and for a
# part standing up from its pivot R_z sends (0, +h) to x' = -h*sin(rz): so the LEFT
# ossicone (which must fan toward -x) needs a POSITIVE rz. Written as `-s * angle`.
# With the sign inverted, every family's pair leaned INWARD across the midline and
# interpenetrated: oss_taper_left reached x +0.53 while oss_taper_right reached -0.53, and
# the two palms overlapped each other through 4.5u of the animal's centre line. The
# z-fight sweep did NOT report it -- interpenetration without a coincident face is
# invisible to it -- and a grey render would have shown a plausible pair of horns.
#
# All authored in head_skull's REST FRAME (skull z -44.4..-34.2 at about y 37), on top of
# the skull, projecting up.

# --- TAPERED: the giraffe. Two upright skin-covered ossicones, blunt and tufted, plus the
# median frontal boss that bull giraffes develop with age (a third "horn" between the eyes,
# and the single easiest way to tell a giraffe skull from any other giraffid's).
# Near-vertical with only 8 degrees of outward lean -- a giraffe's ossicones really are
# almost parallel, which is what distinguishes them from every other family here.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"oss_taper_{side}", "head_skull", (s * 1.7, 40.0, -38.4), (-9, s * -6, -s * 8),
        (s * 0.5, 39.4, -39.8), (s * 2.9, 43.6, -37.0), "ossicone")
add("oss_boss", "head_skull", (0, 39.6, -42.4), (-16, 0, 0),
    (-2.2, 38.8, -44.0), (2.2, 41.8, -40.8), "ossicone")

# --- PALMATE: Sivatherium. Huge flattened palms on short thick bases, swept up and out
# like a moose's -- the most extreme ossicones any giraffid ever grew, and the entire
# reason this species cannot be left to the skin. Base and palm are a parent/child pair so
# ONE showModel and ONE setScale (with setShouldScaleChildren) drive both.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"oss_palm_base_{side}", "head_skull", (s * 2.2, 39.8, -37.4), (-4, s * -8, -s * 20),
        (s * 0.9, 39.0, -39.0), (s * 3.7, 43.2, -35.8), "ossicone")
    # The palm: 7.2 x 6.2 and only 1.4 thick, twisted a further 22 degrees of roll and 14
    # of yaw off its base so the flat face turns outward and the pair reads as a span
    # rather than as two slabs edge-on to the camera.
    add(f"oss_palm_{side}", f"oss_palm_base_{side}", (s * 2.9, 42.8, -37.4),
        (-8, s * 14, -s * 22),
        (s * 2.2, 42.2, -41.0), (s * 3.6, 48.4, -33.8), "ossicone")

# --- SPIKE: Samotherium / Palaeotragus / okapi. Straight conical ossicones set OVER THE
# EYES and swept back -- the primitive giraffid condition, and structurally a different
# animal from the giraffe's blunt vertical pair. Scaled down to 0.50 for the okapi (whose
# ossicones are short, skin-covered and male-only) and 0.75 for Palaeotragus.
# Bases moved out to s*2.4 (from s*1.9): the pair was crossing the midline and the sweep
# found 2.56u2 where the two spikes' rear faces met each other over the skull's centre line.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"oss_spike_{side}", "head_skull", (s * 2.4, 39.8, -40.2), (24, s * -6, -s * 12),
        (s * 1.2, 39.2, -41.4), (s * 3.2, 44.4, -39.0), "ossicone")

# --- QUAD: Bramatherium. Four ossicones -- a massive pair set close together on the
# midline and angled hard outward, plus a small supraorbital pair in front. Nothing else on
# this rig has four, so the head silhouette alone identifies the species.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"oss_quad_{side}", "head_skull", (s * 1.6, 39.8, -37.6), (-6, s * -10, -s * 24),
        (s * 0.5, 39.0, -39.2), (s * 3.3, 44.2, -36.0), "ossicone")
    add(f"oss_quad_front_{side}", "head_skull", (s * 2.0, 39.6, -41.4), (16, s * -6, -s * 14),
        (s * 0.9, 39.0, -42.4), (s * 2.7, 42.2, -40.2), "ossicone")

# ================================================================ UV pack
CANVAS = (256, 128)


def footprint(p):
    """The region MC reserves from texOffs: 2*(w+d) x (h+d), in texels.

    CEIL, not round. The Cube constructor maps UVs from the RAW FLOAT sizes passed to
    addBox and these boxes are not integer-sized, so rounding down by half a texel lets a
    face sample its neighbour's paint -- and leaves the outer fraction of every edge on an
    unpainted, fully transparent texel that entityCutoutNoCull then DISCARDS.
    """
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    w, h, d = (abs(b[i] - a[i]) for i in range(3))
    if p[6] == "eye":
        w = 0.0
    return int(math.ceil(2 * (w + d))), int(math.ceil(h + d))


def uvpack():
    """Shelf-pack every box's true footprint, then ASSERT zero overlap.

    Every part gets its OWN region -- no mirror_uv sharing between left/right. It avoids
    the side1/side2-runs-backwards class of bug, and it is REQUIRED for the eyes, whose
    anti-flicker fix clears OPPOSITE halves on eye_left and eye_right.
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
    """SOP phase 4 against the SPEC, before anything is built.

    zfight_sweep.sweep() is frame-agnostic -- it only needs parts carrying
    (rotationPoint, local box, radian rotation, parent), so the spec's absolute origins
    convert as rp = origin - parent_origin and box = corners - origin.

    Ossicone families are swept ONE AT A TIME: only one family is ever visible in game, so
    a taper/palm/spike/quad pair sharing a plane is not a defect and reporting it just
    buries the real hits under noise.
    """
    import zfight_sweep as Z

    allfam = {n for v in FAMILIES.values() for n in v}
    for fam in ("TAPERED", "PALMATE", "SPIKE", "QUAD"):
        keep = set(FAMILIES[fam])
        parts = {}
        for name, parent, o, r, a, b, cls in PARTS:
            if cls == "eye":
                continue          # zero-width planes: handled by the alpha-0 UV trick
            if name in allfam and name not in keep:
                continue
            p = Z.Part(name)
            po = np.asarray(BY_NAME[parent][2], float) if parent else np.zeros(3)
            p.rp = np.asarray(o, float) - po
            p.rot = np.array([D(r[0]), D(r[1]), D(r[2])])
            p.lo = np.asarray(a, float) - np.asarray(o, float)
            p.hi = np.asarray(b, float) - np.asarray(o, float)
            p.parent = parent
            parts[name] = p

        print("=" * 80)
        print(f"Z-FIGHT SWEEP ON THE SPEC -- ossicone family {fam}"
              "   (>=0.5u2 coincident face)")
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
    """Fraction of each part's VOLUME inside another box.

    The z-fight sweep is structurally blind to this: a part completely swallowed by a
    neighbour shares NO coincident face and is reported clean while being invisible in
    game. Nested core masses reading 65-80% are the intended massing; a FEATURE cube
    (mane, ossicone, tail, ear) reading high is the defect.
    """
    print("=" * 80)
    print("VOLUME BURIAL   (fraction of each part's volume inside some OTHER box)")
    print("=" * 80)
    allfam = {n for v in FAMILIES.values() for n in v}
    solids = [p for p in PARTS if p[6] != "eye"]
    worst = []
    for p in solids:
        pts = world(p[0], vol(p, 9))
        inside = np.zeros(len(pts), bool)
        for q in solids:
            if q[0] == p[0]:
                continue
            # An ossicone hidden inside a DIFFERENT family's ossicone is not burial --
            # the two are never visible at the same time.
            if p[0] in allfam and q[0] in allfam:
                same = any(p[0] in v and q[0] in v for v in FAMILIES.values())
                if not same:
                    continue
            l = local_of(q[0], pts)
            a, b = np.asarray(q[4], float), np.asarray(q[5], float)
            inside |= np.all((l >= a - 1e-6) & (l <= b + 1e-6), axis=1)
        worst.append((inside.mean(), p[0], p[6]))
    for frac, name, cls in sorted(worst, reverse=True):
        flag = ""
        if frac > 0.55:
            flag = ("   <== FEATURE MOSTLY BURIED"
                    if cls in ("mane", "ossicone", "tail", "ear")
                    else "   <== mostly buried (nested core mass, expected)")
        print(f"  {frac:6.1%}  {name}{flag}")


# ================================================================ per-species neck
def species_sweep():
    """The per-species neck factor, checked as GEOMETRY rather than trusted.

    Applying a factor nf to every neck segment means: scale each segment along its own
    local Z by nf, and multiply every child's rotation-point Z offset by nf.  Since the
    chain is authored colinear along -Z, each child's offset from its parent is purely
    (0, 0, -L) -- which is what makes the whole trick safe, and it is worth re-deriving
    rather than assuming: if any neck child ever acquires a nonzero X or Y offset, scaling
    Z alone stops being equivalent to shortening the chain and this must be revisited.

    A matching XY thicken of 1 + (1-nf)*0.45 goes the other way, because a short giraffid
    neck is a bull neck. That thickening is checked against the withers here -- the phase-4
    sweep only ever sees the nominal (giraffe) geometry.
    """
    # neck_1 is deliberately EXCLUDED. Its offset from body_chest is (0, +5.6, -4.0) -- the
    # step up from the shoulder onto the base of the neck -- and that offset must NOT be
    # scaled: it positions where the neck starts, not how long it is. Only the offsets
    # BETWEEN chain links carry length, and those are the ones that have to be pure -Z.
    for nm in NECK_SEGS[1:] + ["head_skull"]:
        rec = BY_NAME[nm]
        par = BY_NAME[rec[1]]
        off = np.asarray(rec[2], float) - np.asarray(par[2], float)
        assert abs(off[0]) < 1e-9 and abs(off[1]) < 1e-9, \
            f"{nm} offset {off} is not pure -Z; the neck-scale trick is invalid"

    print("=" * 80)
    print("PER-SPECIES NECK   (scale each segment's local Z by nf, pivots with it)")
    print("=" * 80)
    wp = BY_NAME["body_withers"]
    withers = world("body_withers", [(0.0, wp[5][1], FORE_PIVOT_Z)])[0][1]
    wid = abs(wp[5][0])

    print(f"{'species':<15}{'nf':>6}{'thick':>7}{'neck u':>8}{'head y':>8}"
          f"{'top u':>7}{'top m':>7}{'neck/wth':>10}  clearance")
    for name, var, real, nf, fam, oss in SPECIES:
        thick = 1.0 + (1.0 - nf) * 0.45
        # Rebuild the chain's world transform with the segments shortened.
        px, py, pz = NECK_PIVOT
        prev, z, ang_abs = TILT["body_chest"], pz, None
        pts = {}
        par_frames = []
        for nm, ang, L, hw, up, dn, ov in NECK_SPEC + HEAD_SPEC:
            k = nf if nm in NECK_SEGS else 1.0
            par_frames.append((nm, ang, L * k, hw, up, dn, ov, k))
        # World position of each joint, walking the shortened chain from the pivot.
        p0 = world("body_chest", (px, py, pz))[0]
        # Direction of a segment at world angle `ang`: forward -Z rotated by +ang about X,
        # then carried through body_chest's own tilt (already folded into `ang`).
        cur = p0.copy()
        head_y = None
        neck_len = 0.0
        for nm, ang, L, hw, up, dn, ov, k in par_frames:
            d = np.array([0.0, math.sin(D(ang)), -math.cos(D(ang))])
            nxt = cur + d * L
            pts[nm] = (cur.copy(), nxt.copy())
            if nm in NECK_SEGS:
                neck_len += L
            if nm == "head_skull":
                head_y = cur[1]
            cur = nxt
        top = max(v[1][1] for v in pts.values()) + 3.4
        s = real * 16.0 / withers
        # Thickened neck half-width vs the withers box it launches from.
        clear = wid - max(hw * thick for _, _, _, hw, _, _, _, _ in par_frames[:1])
        print(f"{name:<15}{nf:6.2f}{thick:7.2f}{neck_len:8.1f}{head_y:8.1f}"
              f"{top:7.1f}{top*s/16:7.2f}{neck_len/withers:10.2f}"
              f"  neck hw {4.6*thick:.2f} vs withers hw {wid:.1f}"
              + ("" if clear > 0 else "  <== NECK WIDER THAN WITHERS"))
    print("\n  neck/withers: giraffe should be ~0.65-0.70, okapi ~0.42-0.45")


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
    print(f"fore pivot height {FORE_DROP:6.2f}   hind pivot height {HIND_DROP:6.2f}"
          f"   (fore MUST exceed hind: it is why the topline falls)")
    for n in sorted(wc):
        if n.endswith("_foot"):
            print(f"  {n:<24} sole y {lo[n][1]:+6.3f}   z {lo[n][2]:6.2f}..{hi[n][2]:.2f}")
    floor = min(v[1] for v in lo.values())
    print(f"  lowest point of the whole model: {floor:+.3f}"
          + ("   OK" if floor > -0.02 else "   <== THROUGH THE FLOOR"))

    # WITHERS = the topline height ABOVE THE FORELEG, never the withers box's world-AABB
    # max. The box is tilted and runs forward, so its AABB max is a front-top corner over
    # the base of the neck -- on the macrauchenia that read 1.8u high and put a 6% error
    # straight into every species' `scale`.
    wp = BY_NAME["body_withers"]
    withers = world("body_withers", [(0.0, wp[5][1], FORE_PIVOT_Z)])[0][1]
    cr = BY_NAME["body_croup"]
    rump_front = world("body_croup", [(0.0, cr[5][1], cr[4][2])])[0][1]
    rump_rear = world("body_croup", [(0.0, cr[5][1], cr[5][2])])[0][1]
    belly = lo["body_barrel"][1]
    stance = ((lo["hind_left_foot"][2] + hi["hind_left_foot"][2]) / 2
              - (lo["fore_left_foot"][2] + hi["fore_left_foot"][2]) / 2)
    print(f"  fore->hind stance {stance:.2f}u = {stance/withers:.2f} x withers"
          f"   (a long-legged browser is ~0.55-0.70)")
    print(f"  withers {withers:.2f}u   croup front {rump_front:.2f}u"
          f"   croup rear {rump_rear:.2f}u")
    # Thresholded on the croup's REAR, not its front. Raising the front top to close the
    # topline shelf deliberately reduces withers->croup-front, so testing that number would
    # now punish the smoothing fix; what "downhill" actually means is where the RUMP ends up.
    print(f"  topline shelf withers->croup front {withers - rump_front:+.2f}u"
          f"   (small is good: it is a step in the back line)")
    drop = withers - rump_rear
    print(f"  TOPLINE FALL withers->rump rear {drop:+.2f}u"
          + ("   OK (downhill, as a giraffid must be)" if drop > 5.0
             else "   <== NOT DOWNHILL ENOUGH"))
    print(f"  belly {belly:.2f}u = {belly/withers:.0%} of withers   "
          f"(LONG-LEGGED browser ~58-62%; the tapir is 47%)")

    # ---- the neck --------------------------------------------------------
    print("\n" + "=" * 80)
    print("THE NECK   (the diagnostic -- everything else is a deer)")
    print("=" * 80)
    nl = sum(L for _, _, L, *_ in NECK_SPEC)
    neck_top = hi["neck_4"][1]
    skull_top = hi["head_skull"][1]
    nose = lo["head_muzzle"][2]
    print(f"  4 segments, {nl:.1f}u of neck, world angles "
          + "/".join(str(a) for _, a, *_ in NECK_SPEC))
    print(f"  neck base y {world('body_chest', NECK_PIVOT)[0][1]:.2f}"
          f"  ->  neck top y {neck_top:.2f}   rise {neck_top - withers:+.2f}u over withers")
    print(f"  skull top {skull_top:.2f}u   nose reach z {nose:.2f}")
    print(f"  neck/withers {nl/withers:.2f}   (a giraffe is ~0.65-0.70)")
    for i in range(len(NECK_SPEC + HEAD_SPEC) - 1):
        chain = NECK_SPEC + HEAD_SPEC
        bend = abs(chain[i + 1][1] - chain[i][1])
        ov = chain[i + 1][6]
        print(f"    {chain[i][0]:<12} -> {chain[i+1][0]:<12} bend {bend:4.0f} deg"
              f"   overlap {ov:.1f}u"
              + ("" if bend < 32 else "   <== BEND TOO SHARP FOR THE JOINT"))

    # ---- eye clearance (check EVERY wider ancestor) ----------------------
    print("\n" + "=" * 80)
    print("EYE CLEARANCE  (must sit outside every ancestor box, not just the skull)")
    print("=" * 80)
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for other in ("head_skull", "neck_4", "head_jaw", "head_muzzle",
                      f"ear_{side}", "oss_boss", f"oss_spike_{side}"):
            p = BY_NAME[other]
            l = local_of(other, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gaps = [max(a[i] - l[i], l[i] - b[i]) for i in range(3)]
            gap = max(gaps)
            print(f"  eye_{side:<6} vs {other:<18} clear by {gap:6.2f} on "
                  f"{'xyz'[int(np.argmax(gaps))]}"
                  + ("" if gap > 0.3 else "   <== TOO CLOSE"))

    # ---- midline crossing ------------------------------------------------
    # THE Z-FIGHT SWEEP IS STRUCTURALLY BLIND TO THIS and so is a grey render. Two mirrored
    # parts that lean inward past x=0 interpenetrate without ever sharing a coincident face,
    # so the sweep reports clean; and from any camera angle a pair of crossed ossicones
    # still looks like a pair of ossicones. The first pass had the rz sign inverted on all
    # four families and every single pair was crossed -- oss_palm_left reached x +3.97 while
    # oss_palm_right reached -3.97, i.e. 8u of mutual interpenetration through the skull's
    # centre line. This check is the only thing in the toolchain that catches it.
    print("\n" + "=" * 80)
    print("MIDLINE CROSSING   (a *_left part must stay at x <= 0, *_right at x >= 0)")
    print("=" * 80)
    bad = 0
    for p in PARTS:
        n = p[0]
        if n.endswith("_left") or "_left_" in n:
            over = hi[n][0]
            if over > 0.02:
                bad += 1
                print(f"  {n:<24} reaches x {over:+6.2f}   <== CROSSES THE MIDLINE")
        elif n.endswith("_right") or "_right_" in n:
            over = lo[n][0]
            if over < -0.02:
                bad += 1
                print(f"  {n:<24} reaches x {over:+6.2f}   <== CROSSES THE MIDLINE")
    if not bad:
        print("  clean -- every mirrored part stays on its own side")

    # ---- size calibration ------------------------------------------------
    print("\n" + "=" * 80)
    print(f"SIZE CALIBRATION   mesh withers = {withers:.2f}u,  scale = m*16/withers")
    print("=" * 80)
    rear = hi["body_croup"][2]
    width = 2 * max(abs(v[0]) for v in list(lo.values()) + list(hi.values()))
    print(f"{'species':<15}{'m':>6}{'scale':>8}{'withers':>9}{'top':>7}"
          f"{'nose-rump':>11}{'width':>8}  ossicones")
    for name, var, real, nf, fam, oss in SPECIES:
        s = real * 16.0 / withers
        # TOP IS PER-SPECIES ON BOTH COUNTS. Only this species' own ossicone family exists
        # in game, so measuring against max(taper, palm, skull) charged the giraffe for
        # Sivatherium's palms and reported it 0.3 blocks taller than it renders. And a
        # shorter neck lowers everything above it, so the neck rise is re-summed at nf.
        own = [hi[n][1] * oss + hi["head_skull"][1] * (1 - oss)
               for n in FAMILIES[fam]] or [hi["head_skull"][1]]
        rise = sum(L * nf * math.sin(D(a)) for _, a, L, *_ in NECK_SPEC)
        rise0 = sum(L * math.sin(D(a)) for _, a, L, *_ in NECK_SPEC)
        t = max(max(own), hi["head_skull"][1]) - (rise0 - rise)
        print(f"{name:<15}{real:6.2f}{s:8.3f}{withers*s/16:9.2f}{t*s/16:7.2f}"
              f"{(rear-nose)*s/16:11.2f}{width*s/16:8.2f}  {fam}"
              + (f" @{oss:.2f}" if fam != "NONE" else ""))
    print(f"proposed hitbox {HITBOX[0]} x {HITBOX[1]} blocks"
          "   (per-TYPE, so the biggest species should sit close to it)")

    # ---- silhouette ------------------------------------------------------
    # n=15, not the default 7: a sparser grid leaves gaps wider than a render cell and
    # reads as a speckled cloud rather than a silhouette.
    skip = {n for f, v in FAMILIES.items() if f != "TAPERED" for n in v}
    pts = np.vstack([world(p[0], vol(p, 15)) for p in PARTS
                     if p[6] != "eye" and p[0] not in skip])
    print("\n" + "=" * 80)
    print("SIDE SILHOUETTE  (giraffe: TAPERED ossicones)      <- nose      tail ->")
    print("=" * 80)
    zmin, zmax, ymax = pts[:, 2].min(), pts[:, 2].max(), pts[:, 1].max()
    W, H = 76, 34
    g = [[" "] * W for _ in range(H)]
    for _, y, z in pts:
        c = int((z - zmin) / (zmax - zmin) * (W - 1))
        r = int((ymax - y) / ymax * (H - 1))
        if 0 <= r < H:
            g[r][c] = "#"
    for r, row in enumerate(g):
        print(f"{ymax - r*ymax/(H-1):5.1f} |" + "".join(row))
    print(f"      z {zmin:.1f} .. {zmax:.1f}")

    # ---- UV budget -------------------------------------------------------
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
    "fore": ("body_chest", TILT["body_chest"],
             [("shoulder", 11.0), ("forearm", 9.0), ("cannon", 9.2), ("foot", 3.0)]),
    "hind": ("body_croup", TILT["body_croup"],
             [("thigh", 10.0), ("gaskin", 8.4), ("cannon", 8.6), ("foot", 2.8)]),
}


def _leg_rot(prefix, side, angles):
    _, tilt, segs = LEGS[prefix]
    over, prev = {}, tilt
    for (nm, _), a in zip(segs, angles):
        over[f"{prefix}_{side}_{nm}"] = (a - prev, 0.0, 0.0)
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
        if k >= 2:                                        # cannon + foot must lie ON it
            pen += (low - 0.12) ** 2 * 3.0
    for a in angles[2:]:
        pen += (math.cos(D(a)) ** 2) * 4.0
    return pen


def _coil_score(angles, dy):
    """Head-on-the-rump coil, scored against the real croup box.

    A resting giraffe folds its neck back and lays the head on its own flank or rump. It is
    the animal's single most recognisable pose and it is worth solving properly, but only
    the giraffe can reach: at nf 0.78 the neck is 6u short and the head would hang in mid
    air, so the generator gates the coil on nf >= 0.95 and gives every other species a
    plain head-forward sleep.

    Scored on three things, all against geometry rather than by eye: the muzzle tip must
    land ON the croup's top surface, the whole chain must stay above the body, and no neck
    segment may pass THROUGH the barrel on the way round.
    """
    chain = NECK_SPEC + HEAD_SPEC
    over, prev = {}, TILT["body_chest"]
    for (nm, *_), a in zip(chain, angles):
        over[nm] = (a - prev, 0.0, 0.0)
        prev = a
    cr = BY_NAME["body_croup"]
    target = world("body_croup", [(0.0, cr[5][1], (cr[4][2] + cr[5][2]) / 2)])[0]
    target = target + np.array([0.0, dy + 1.6, 0.0])

    tip = world_mod("head_muzzle", [(0.0, BY_NAME["head_muzzle"][2][1],
                                     BY_NAME["head_muzzle"][4][2])], over, dy)[0]
    pen = float(np.sum((tip - target) ** 2)) * 1.0

    barrel = world("body_barrel", vol(BY_NAME["body_barrel"], 5)) + np.array([0, dy, 0])
    bl, bh = barrel.min(0), barrel.max(0)
    for nm, *_ in chain:
        w = world_mod(nm, vol(BY_NAME[nm], 5), over, dy)
        # above the body, not through it
        inside = np.all((w >= bl + 0.4) & (w <= bh - 0.4), axis=1)
        pen += inside.mean() * 60.0
        pen += max(0.0, 1.0 - w[:, 1].min()) ** 2 * 8.0
    return pen


def pose_solve():
    """SOP phase 7: SOLVE the folds, never hand-guess them.

    Hand-guessed poses have failed repeatedly on this codebase -- claws swung 6.3u through
    the floor once a hand folded, and the shipped ModelToxodon carried bison fold angles it
    admitted in a comment were never re-derived. The rig being replaced here is worse than
    that: ModelGiraffid's sit/sleep block is ModelBison's, unchanged, applied to an animal
    whose legs are 3x as long.
    """
    rng = np.random.default_rng(11)
    print("=" * 80)
    print("PHASE 7 -- POSES SOLVED AGAINST REAL BOX GEOMETRY")
    print("=" * 80)
    body_lo = world("body_barrel", corners(BY_NAME["body_barrel"]))[:, 1].min()
    out = {}
    for pose, belly in (("sit", 3.0), ("sleep", 1.8)):
        dy = belly - body_lo
        print(f"\n{pose.upper()}   body drop dy = {dy:+.2f}u  (belly rests at y={belly})")
        for prefix in ("fore", "hind"):
            base = np.array([44.0, -74.0, 96.0, 96.0] if prefix == "fore"
                            else [72.0, -96.0, 84.0, 84.0])
            best, bs = base.copy(), _leg_score(prefix, base, dy)
            span = np.array([55.0, 55.0, 45.0, 45.0])
            for _ in range(9):
                cand = best + rng.normal(0, 1, (4000, 4)) * span
                cand[:, 3] = cand[:, 2] + rng.normal(0, 6, 4000)
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
                print(f"      {nm:<26} localRotX {rot[0]:+8.3f} deg"
                      f"   y {w[:,1].min():6.2f}..{w[:,1].max():6.2f}")

    # ---- the coil, solved for the giraffe alone --------------------------
    dy = 1.8 - body_lo
    chain = NECK_SPEC + HEAD_SPEC
    base = np.array([70.0, 110.0, 150.0, 185.0, 205.0, 215.0])
    best, bs = base.copy(), _coil_score(base, dy)
    span = np.full(6, 40.0)
    for _ in range(11):
        cand = best + rng.normal(0, 1, (3000, 6)) * span
        for c in cand:
            s = _coil_score(c, dy)
            if s < bs:
                bs, best = s, c.copy()
        span *= 0.66
    print(f"\nSLEEP COIL (giraffe only, nf >= 0.95)   penalty {bs:.3f}")
    over, prev = {}, TILT["body_chest"]
    for (nm, *_), a in zip(chain, best):
        over[nm] = (a - prev, 0.0, 0.0)
        prev = a
    tip = world_mod("head_muzzle", [(0.0, BY_NAME["head_muzzle"][2][1],
                                     BY_NAME["head_muzzle"][4][2])], over, dy)[0]
    print(f"  muzzle tip lands at y {tip[1]:.2f}  z {tip[2]:.2f}"
          f"   (croup top is y {world('body_croup', [(0,BY_NAME['body_croup'][5][1], 15.0)])[0][1] + dy:.2f})")
    for nm, *_ in chain:
        w = world_mod(nm, vol(BY_NAME[nm]), over, dy)
        print(f"      {nm:<14} localRotX {over[nm][0]:+8.2f} deg"
              f"   y {w[:,1].min():6.2f}..{w[:,1].max():6.2f}"
              f"   z {w[:,2].min():7.2f}..{w[:,2].max():7.2f}")
    out[("sleep", "coil")] = ({nm: over[nm][0] for nm, *_ in chain}, dy)
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
    elif "--species" in sys.argv:
        species_sweep()
    else:
        main()
