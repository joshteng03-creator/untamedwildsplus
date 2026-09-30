#!/usr/bin/env python3
"""Macrauchenia (litoptern) model spec + forward-kinematics checker -- SOP Phase 1/1b/2.

Replaces the RAW ModelBison FORK that macrauchenia has shipped with since day one: 26
boxes on 128x64 that still declare `head_horn_left/right`, `head_beard`, `head_hair`,
`body_hair` and `arm_*_fur` -- bison parts, hidden or not -- with a 2-cube proboscis
bolted onto the snout.  It is the last un-remodelled rig from the original new-type batch.

Authoring space is BLOCKBENCH project space: Y up, ground y=0, -Z = front (nose),
-X = left.  Group origins and cube from/to are ABSOLUTE project coordinates, exactly as
Blockbench stores them, so this table doubles as the data for the build script.

    world(p) = A_root(A_child(... p))      A_g(p) = R_g @ (p - o_g) + o_g

Rotation order is Rz @ Ry @ Rx -- Blockbench's, and MC's ModelPart's under the verified
bbRot = [-rx, -ry, +rz] export flip.

MACRAUCHENIA SILHOUETTE (what the numbers below are trying to say):
  * LONG-LEGGED.  This is the single thing the bison fork got most wrong.  Litopterns are
    cursorial: the belly sits ~58% of withers height, not a bison's ~45%.  ~23u of leg
    under a 28u withers.
  * a LONG neck carried high and S-curved -- steep at the base, arcing forward at the top,
    camelid-like.  It is what the genus is named for (makros auchen, "long neck").
  * a small, NARROW, long-muzzled head -- the neck tapers 4.8 -> 4.0 -> 3.2 against a 6.6
    chest, and the skull is only 3.6 half-wide.
  * the diagnostic RETRACTED NASAL OPENING high on the skull roof between the orbits
    (`head_nasal`), and the short tapir-like proboscis it implies.
  * three-toed, broad, splayed feet (mesaxonic, rhino-like) -- the feet flare back OUT.
  * a level topline with a modest wither, and a short tail.

Two species share the one mesh (macrauchenia.json declares NO model flags and
EntityMacrauchenia has no synced flag), so anything species-specific would have to route
through getVariant() -- the ModelDeer:509-524 pattern.  Both species here use identical
geometry and differ only by `scale` and skin, exactly as toxodon/mixotoxodon ship, so
ModelMacrauchenia deliberately gets NO variant switch.

Run:  python scripts/macrauchenia_spec.py            full report
      python scripts/macrauchenia_spec.py --uv       UV shelf pack only
      python scripts/macrauchenia_spec.py --zfight   z-fight sweep on the spec
      python scripts/macrauchenia_spec.py --pose     phase-7 sit/sleep solve
      python scripts/macrauchenia_spec.py --json     compact JSON for the BB builder
"""
import math
import numpy as np

D = math.radians

# ---------------------------------------------------------------- contract
SPECIES = [
    # name,                variant, real shoulder height m
    ("macrauchenia",       0, 1.80),   # Macrauchenia patachonica -- Patagonian, the big one
    ("xenorhinotherium",   1, 1.55),   # Xenorhinotherium bahiense -- NE Brazilian, gracile
]
HITBOX = (1.3, 1.9)   # ModEntity: createEntity(EntityMacrauchenia::new,"macrauchenia",1.3F,1.9F,..)

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
# Four big NESTED masses (SOP phase 2), each buried in the previous so the topline reads
# as one continuous line rather than a stack of slabs.  Distinct side planes, >=0.4u
# apart, so no two torso boxes can share one:
#     barrel 8.0  >  croup 7.4  >  withers 6.9  >  chest 6.5
# The withers box is TALL (9.5u) with its lower half buried in the barrel, so only the
# shoulder rise emerges -- that is what makes the wither read as part of one line.
# HORIZONTAL planes are spaced with the same care, and a TILTED box's must clear its
# neighbour's along the WHOLE shared span, not just at the origin (the toxodon's chest
# tilt lifted its top plane 0.84u over 8u of shared span and produced the single biggest
# coincident patch in that model):
#     tops     withers 28.0  >  barrel 26.6  >  croup 26.2  >  chest 25.2
#     bottoms  withers 18.5  >  croup 17.6  >  barrel 16.4  >  chest 14.8
#
# THE TOPLINE IS THE SURFACE THIS MODEL IS JUDGED ON, and getting it flat-safe is not the
# same as getting it to READ.  The first build cleared the sweep with a flat-topped
# withers ending at z=4 and a croup dropped to 25.5, and the grey render came back with
# TWO hard horizontal ledges across the back -- a stack of slabs, precisely the phase-2
# failure ("bolting boxes on top produces a pile, not an animal").  Both are fixed by
# geometry, not by hiding them:
#
#  1. The WITHERS IS TILTED +13 rather than flat.  Its top now peaks 29.5 at the front
#     (z=-8.5, right behind the neck base -- anatomically where the neural spines
#     actually peak) and slopes back to 26.50 at z=5, which is 0.10 under the barrel's
#     26.6: visually a seamless blend.  0.10 would be a guaranteed z-fight between
#     PARALLEL planes, but these two are 13 degrees apart, well outside the sweep's
#     8-degree near-parallel window, so it is safe as well as invisible.
#  2. The CROUP'S SHARED SPAN WITH THE BARREL IS SHORT ON PURPOSE (z 13..16, not 5..14).
#     A tilted rear box cannot both stay under a flat barrel top across a long shared
#     span AND come close to it at the barrel's rear end -- the tilt that drops its rear
#     is the same tilt that lifts its front through the barrel's top plane.  Every
#     attempt to close the rear ledge by raising the croup put its FRONT 0.2-0.8u above
#     the barrel, i.e. a hump over the loin and the 18.75u2 z-fight back again.  Starting
#     the croup at z=13 leaves only 3u of overlap, so its top can sit 0.45u under the
#     barrel where they meet and still fall away to 24.9 at the buttock: the remaining
#     step is 0.92u and it sits at the rear of the animal, where it reads as the rump
#     turning down rather than as a ledge across the back.
add("body_barrel", None, (0, 21.5, 5.0), (0, 0, 0),
    (-8.0, 16.4, -4.0), (8.0, 26.6, 16.0), "body")
# Carries the wither and the shoulder mass.  TALL (9.5u) with its lower half buried in the
# barrel so only the crest emerges -- that is what makes a rise read as one continuous
# line instead of an applied lump -- and burying it deep also keeps it clear of the
# forearm's top face.
# Front face at -11.0, not -8.5: at -8.5 the crest peaked BEHIND the neck base's rear
# face and the grey render showed a V-notch in the topline between the two.  Pushing it
# forward buries the crest's leading edge inside the neck (52 degrees apart, so nothing
# the sweep can object to) and the topline runs unbroken from poll to rump.
add("body_withers", "body_barrel", (0, 22.0, -1.0), (13, 0, 0),
    (-6.9, 18.5, -11.0), (6.9, 28.0, 5.0), "body")
# Chest tilts the front DOWN (-5).  Modest: a litoptern carries a level topline, so the
# shoulder mass only just slopes forward into the base of a neck that then rises steeply.
add("body_chest", "body_barrel", (0, 22.0, -4.0), (-5, 0, 0),
    (-6.5, 14.8, -12.5), (6.5, 25.2, 2.5), "body")
# Croup tilts the rear DOWN (+9): positive rx sends a +z point downward.  Its origin sits
# at its own FRONT face (z=13), so the tilt only ever drops the rear and never lifts the
# front into the barrel's top plane -- which is the whole reason the rear ledge can be
# closed to 0.92u.  9 degrees, not more: Macrauchenia's rump is nearly level with its
# wither, unlike the notoungulates, whose steeply dropped rump is diagnostic.  Any more
# and it reads as a toxodon.
add("body_croup", "body_barrel", (0, 22.5, 13.0), (9, 0, 0),
    (-7.4, 17.6, 13.0), (7.4, 26.2, 21.0), "body")

TILT = {"body_chest": -5.0, "body_croup": 9.0}   # inherited world tilt of a limb root


# ================================================================ chain helpers
def zchain(parent, pivot, segs, cls, parent_tilt=0.0):
    """Chain authored lying forward along -Z from `pivot`; bent by WORLD angles.

    segs: (name, world_angle_deg_above_horizontal, length, halfwidth, up, down, overlap)
    Rest positions are colinear along -Z, so a rotation about each pivot lands the joints
    exactly where the world angles say they should be.  A POSITIVE angle raises the
    forward end: R_x sends a point at -z toward +y.
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

    The sole lands on y = 0 when the pivot's WORLD height equals sum(L*cos(angle)).  The
    AUTHORED height is not that number, because the parent's own tilt drags the pivot off
    it -- the chest's -5 and the croup's +8 each move it by most of a unit.  Solve for the
    authored value instead of assuming.
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
# THE defining feature.  Long, S-curved and carried high: steep at the base (+56), then
# arcing forward over the top (+47, +31) so the head is carried well ahead of the
# shoulder rather than straight above it.  A single-angle neck reads as a flagpole.
#
# Each segment turns only 9-16 degrees against the one below it, which matters twice
# over: chained joints crack a visible wedge open the moment they animate (every segment
# carries ~1.6u of overlap for that), and a shallow per-joint turn is what makes the
# curve read as a curve instead of a hinge.
#
# The chain NARROWS hard -- 4.8 -> 4.0 -> 3.2 against a 6.5 chest -- which is what makes
# the neck read as a neck rather than as the front of the barrel.  The skull then widens
# back to 3.6 so the head "pops" off the end of the neck instead of continuing it.
#
# Cumulative world angle never goes NEGATIVE past the skull, so no part of this chain can
# reach the floor -- the failure mode that once put a cervid snout through it at -38.
# EVERY CHILD OF A CHAIN SEGMENT IS AUTHORED IN THE CHAIN'S REST FRAME, NOT IN WORLD
# SPACE.  zchain lays the whole chain out colinear along -Z and only then rotates it, so
# head_skull's box actually rests at z -32.1..-24.5 around y 23.5 -- nowhere near where
# the head ends up on screen.  Authoring the jaw/nasal/ears/eyes at their apparent world
# position instead put all five of them ~15u behind the skull, hanging in mid-air off the
# middle of the neck.  Rest-frame anchors, for reference when editing anything below:
#     neck_base  z -15.5..-7.9    neck_mid  z -21.1..-13.9   neck_top  z -26.1..-19.5
#     head_skull z -32.1..-24.5   head_muzzle z -36.7..-30.5   all about y 23.5
zchain("body_chest", (0, 23.5, -9.5), [
    #  name           ang    L    hw   up   dn   ov
    ("neck_base",     52,  6.0, 4.8, 3.4, 4.2, 1.6),
    ("neck_mid",      44,  5.6, 4.0, 3.0, 3.6, 1.6),
    ("neck_top",      32,  5.0, 3.2, 2.6, 3.0, 1.6),
    ("head_skull",    -8,  6.0, 3.6, 3.0, 3.2, 1.6),
    ("head_muzzle",  -22,  4.6, 2.6, 2.2, 2.4, 1.6),
], "head", parent_tilt=TILT["body_chest"])

# Slender mandible.  Child of head_skull (NOT head_muzzle) so it stays one rigid bone;
# its top is 2.3u inside the skull, so the 14 degrees of muzzle tilt it does not inherit
# can never crack a gap open along the tooth row.  Its front stops 1.4u behind the
# muzzle's own front face: two near-parallel front planes a few tenths apart and
# DIVERGING is a guaranteed shimmering patch (8.8u2 of exactly that on the toxodon).
# The bottom is at 18.6, a full 1.7u below the skull's 20.3 -- at 19.6 the mandible was
# 74% buried by volume and only 0.7u of it was ever visible, which the z-fight sweep is
# structurally blind to (burial shares no coincident face).  Run --burial, not --zfight,
# to catch this class of defect.
add("head_jaw", "head_skull", (0, 21.0, -28.5), (0, 0, 0),
    (-2.9, 18.6, -35.3), (2.9, 22.0, -24.8), "head")

# THE DIAGNOSTIC FEATURE.  Macrauchenia's nasal opening is retracted to the top of the
# skull, BETWEEN the orbits -- that, not the trunk itself, is the character the trunk is
# inferred from.  A raised dome on the skull roof is how it reads at 16px.
# Narrow (2.4) so it can never swallow the eye planes at 4.0, and its underside is buried
# 1.1u in the skull roof rather than sitting flush -- the two overlap in x and z, so a
# 0.1u gap there would be a guaranteed coincident patch.
add("head_nasal", "head_skull", (0, 26.0, -28.0), (0, 0, 0),
    (-2.4, 25.4, -31.0), (2.4, 27.6, -25.5), "head")

# Short tapir-like proboscis, hanging forward and DOWN off the muzzle front.  Two
# segments, each keeping >=2.2u inside its parent so a graze/bite animation cannot open a
# wedge gap at the joint -- chained segments need ~1u of overlap MINIMUM and these two
# also carry 24-30 degrees of relative bend, which needs more.
add("proboscis_1", "head_muzzle", (0, 23.0, -35.6), (-30, 0, 0),
    (-1.9, 21.4, -39.4), (1.9, 24.4, -34.5), "nose")
add("proboscis_2", "proboscis_1", (0, 23.0, -38.3), (-24, 0, 0),
    (-1.4, 21.6, -41.6), (1.4, 24.2, -37.2), "nose")

# Tall, narrow, mobile ears set well back on the skull and swept up and out.
# rz SIGN: R_z sends a point at +x toward +y, so a tip authored at -x (the LEFT ear) only
# rises under a NEGATIVE rz.  Getting this backwards rolls both ear tips DOWN into the
# skull and the render still looks plausible -- it cost a full round trip on the toxodon.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"ear_{side}", "head_skull", (s * 2.4, 25.8, -26.0), (8, s * -16, s * 40),
        (s * 2.4, 25.2, -27.2), (s * 6.4, 26.8, -24.8), "ear")

# Eyes: the ModelEquid/ModelDeer design -- a zero-width plane on its own UV footprint,
# blinked by retracting it INTO the skull rather than by swapping art.
# WHOLE-UNIT 3x3, never fractional: at a fractional depth the outward and inward halves
# land on OVERLAPPING texel rects, so alpha-0ing the inward half eats a column of the
# outward eye -- and because the two sides paint in opposite order, that gives one face
# visibly different eyes from perfectly symmetric geometry.
# Set high and forward so they clear neck_top (3.2) as well as the skull (3.6); on the
# equid, eyes that cleared the skull were still buried in the neck and rendered as a
# triangular sliver.  High, rearward orbits are correct Macrauchenia anatomy anyway --
# they are what the retracted nasal pushes back.
# 2.0 x 2.0, NOT the 3.0 x 3.0 the toxodon uses.  Copying that size onto a smaller skull
# put a 3u eye on a 7.6u-deep, 6.2u-tall head, and the painted middle row rendered as a
# solid black BAR across a third of the face -- the plane was simply too big for the
# animal.  2.0 is the smallest whole-unit size that still reads (below ~1.0x2.0 each
# surviving face gets about one texel and no eye can be drawn at all).
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"eye_{side}", "head_skull", (s * 4.0, 24.5, -29.0), (0, 0, 0),
        (s * 4.0, 23.5, -30.0), (s * 4.0, 25.5, -28.0), "eye")

# ================================================================ TAIL
# Short and tapering, hung off the croup's rear-top and swinging DOWN AND BACK.
#
# Authored as EXPLICIT boxes straddling their pivot with a NEGATIVE rx, never via zchain.
# zchain lays a segment forward along -Z and then rotates it, so a tail needs an angle
# past -90 to end up behind its pivot, and even then it sweeps through the rump on the
# way.  Built that way the z-fight sweep says NOTHING -- burial is not a coincident face
# -- while most of the dock sits inside body_croup and the rear render shows no tail.
# 64% of the toxodon's dock was buried exactly like that.
# Half-width 1.2 threads between the thighs (inner face 2.0) with 0.8 to spare.
add("tail_dock", "body_croup", (0, 24.6, 19.8), (-28, 0, 0),
    (-1.2, 18.3, 19.0), (1.2, 25.5, 22.4), "tail")
add("tail_tip", "tail_dock", (0, 18.9, 21.1), (-16, 0, 0),
    (-0.85, 13.5, 20.3), (0.85, 19.5, 22.9), "tail")

# ================================================================ LEGS
# LONG and slender -- ~23u of leg under a 28u withers, against the bison fork's ~17u.
# This is the proportion that makes the animal read as a cursorial litoptern and not as a
# short-legged bovid, and it is the single biggest change from the shipped rig.
#
# Lateral planes step MONOTONICALLY INBOARD down each limb so no two boxes that overlap
# in projection come within 0.2u of sharing one.  The INNER faces need the same treatment
# as the outer ones -- two segments sharing an inner plane z-fight exactly as readily,
# and the tail dock threading between the thighs is the pair that bites:
#   fore  outer 8.6 / 7.1 / 5.9 / 6.5   inner 2.6 / 3.2 / 3.9 / 3.1
#   hind  outer 8.6 / 6.9 / 5.7 / 6.3   inner 2.0 / 2.7 / 3.5 / 2.7
# The feet flare back OUT (6.5 / 6.3) rather than continuing inboard: a broad splayed
# three-toed foot is the diagnostic litoptern foot, and the flare is safe because each
# foot clears the segment above it by >1.4u and shares no vertical range with anything
# higher up the chain.
# Both limb roots sit PROUD of the barrel (8.6 vs 8.0) -- a shoulder tucked just inside
# the torso plane is the pairing that produced 20-38u2 of shared face on the deer.
#
# `fr`/`bk` ALSO have to be stepped, not just the lateral faces.  Every segment of a leg
# is authored around the SAME pivot_z, so equal `bk` values put two segments' rear planes
# at the identical z by construction -- and consecutive segments are only a few degrees
# apart, well inside the sweep's 8-degree near-parallel window.  Both feet originally
# shared their rear plane with the cannon above them (fore: exactly coincident, 2.28u2
# each side; hind: 0.1u apart and only escaping the report because of a 12-degree turn).
# Rear planes now step monotonically:  fore  0.1 / -0.5 / -1.3 / -2.0
#                                      hind 17.7 / 16.6 / 15.7 / 15.0
FORE_PIVOT_Z = -3.0      # also where the withers height is measured (see main())
FORE_DROP = leg("fore", "body_chest", FORE_PIVOT_Z, [
    #  name        ang   L    outer inner  fr   bk   ov
    ("shoulder",     9, 8.0, 8.6, 2.6, 3.9, 3.1, 1.8),
    ("forearm",     -9, 7.0, 7.1, 3.2, 2.9, 2.5, 2.0),
    ("cannon",       3, 6.0, 5.9, 3.9, 1.9, 1.7, 1.4),
    ("foot",         0, 2.4, 6.5, 3.1, 3.2, 1.0, 0.8),
], TILT["body_chest"])

HIND_DROP = leg("hind", "body_croup", 15.5, [
    ("thigh",       22, 8.5, 8.6, 2.0, 4.4, 3.7, 2.0),
    ("gaskin",     -30, 7.5, 6.9, 2.7, 3.0, 2.6, 1.8),
    ("cannon",      12, 5.5, 5.7, 3.5, 1.9, 1.7, 1.4),
    ("foot",         0, 2.3, 6.3, 2.7, 3.4, 1.0, 0.8),
], TILT["body_croup"])

# ================================================================ UV pack
CANVAS = (256, 128)


def footprint(p):
    """The region MC reserves from texOffs: 2*(w+d) x (h+d), in texels.

    CEIL, not round.  The Cube constructor maps UVs from the RAW FLOAT sizes passed to
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

    Every part gets its OWN region -- no mirror_uv sharing between left/right.  It avoids
    the side1/side2-runs-backwards class of bug, and it is REQUIRED for the eyes: the
    anti-flicker fix clears OPPOSITE UV halves on eye_left and eye_right, which is
    impossible if the two share one rect.
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
    out = []
    for name, parent, o, r, a, b, cls in PARTS:
        u, v, _, _ = placed[name]
        out.append({"n": name, "p": parent, "o": [round(x, 4) for x in o],
                    "r": [round(x, 4) for x in r], "f": [round(x, 4) for x in a],
                    "t": [round(x, 4) for x in b], "uv": [u, v], "c": cls})
    print(json.dumps({"canvas": list(CANVAS), "parts": out}, separators=(",", ":")))


# ================================================================ z-fight sweep
def zfight():
    """Run SOP phase 4 against the SPEC, before anything is built.

    scripts/zfight_sweep.py parses Model*.java, but its sweep() is frame-agnostic -- it
    only needs parts carrying (rotationPoint, local box, radian rotation, parent).  The
    spec stores absolute origins and absolute box corners, so the conversion is
    rp = origin - parent_origin and box = corners - origin.  Catching a shared face here
    costs one script run; catching it after the Blockbench build and the Java port costs
    a full round trip through both.
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
    """How much of each part's VOLUME sits inside another box.

    The z-fight sweep is blind to this: a part completely swallowed by a neighbour shares
    NO coincident face and is reported clean while being invisible in game.  That is
    exactly how the toxodon shipped with 64% of its tail dock inside body_croup and no
    tail at all in the rear render.  Anything over ~55% is worth a second look; a feature
    cube (nasal, wither) is buried ON PURPOSE and should read high here.
    """
    print("=" * 80)
    print("VOLUME BURIAL   (fraction of each part's volume inside some OTHER box)")
    print("=" * 80)
    solids = [p for p in PARTS if p[6] != "eye"]
    worst = []
    for p in solids:
        pts = world(p[0], vol(p, 9))
        inside = np.zeros(len(pts), bool)
        for q in solids:
            if q[0] == p[0]:
                continue
            l = local_of(q[0], pts)
            a, b = np.asarray(q[4], float), np.asarray(q[5], float)
            inside |= np.all((l >= a - 1e-6) & (l <= b + 1e-6), axis=1)
        worst.append((inside.mean(), p[0]))
    for frac, name in sorted(worst, reverse=True):
        flag = "   <== MOSTLY BURIED" if frac > 0.55 else ""
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
    print("GROUND CONTACT + JOINT CHAIN")
    print("=" * 80)
    print(f"fore pivot height {FORE_DROP:6.2f}   hind pivot height {HIND_DROP:6.2f}")
    for n in sorted(wc):
        if n.endswith("_foot"):
            print(f"  {n:<24} sole y {lo[n][1]:+6.3f}   z {lo[n][2]:6.2f}..{hi[n][2]:.2f}")
    floor = min(v[1] for v in lo.values())
    print(f"  lowest point of the whole model: {floor:+.3f}"
          + ("   OK" if floor > -0.02 else "   <== THROUGH THE FLOOR"))
    stance = ((lo["hind_left_foot"][2] + hi["hind_left_foot"][2]) / 2
              - (lo["fore_left_foot"][2] + hi["fore_left_foot"][2]) / 2)
    # WITHERS = the topline height ABOVE THE FORELEG, not the world-AABB max of the
    # withers box.  The box is tilted +13 and now runs forward to z=-11.5, so its AABB
    # max is its front-top corner -- a point over the base of the NECK, 1.8u higher than
    # the shoulder.  Taking the AABB reported a withers of 30.10 for an animal whose
    # shoulder is at 28.30, which is a 6% error straight into every species' `scale`.
    # Sample the tilted top plane at the foreleg pivot z instead.
    wp = BY_NAME["body_withers"]
    withers = world("body_withers", [(0.0, wp[5][1], FORE_PIVOT_Z)])[0][1]
    # The croup is TILTED, so its world-AABB max y is its FRONT top corner and says
    # nothing about the rump.  Measure the actual rear-top corner instead, or the
    # topline check silently passes on a model whose rump is not where it claims.
    cr = BY_NAME["body_croup"]
    rump = world("body_croup", [(0.0, cr[5][1], cr[5][2])])[0][1]
    # Report the BELLY LINE (the barrel's floor) and the BRISKET (the chest's floor)
    # separately.  Lumping them under one min() reports the brisket -- which hangs lower
    # by design, between the forelegs -- and makes a correctly long-legged animal look
    # 9 points shorter in the leg than it is.
    belly = lo["body_barrel"][1]
    brisket = lo["body_chest"][1]
    head_top = max(hi["head_nasal"][1], hi["ear_left"][1], hi["head_skull"][1])
    print(f"  fore->hind stance {stance:.2f}u = {stance/withers:.2f} x withers"
          f"   (a long-legged cursorial ungulate is ~0.60-0.72)")
    print(f"  withers {withers:.2f}u   rump {rump:.2f}u   "
          f"topline drop {withers-rump:+.2f}u   (small POSITIVE: near-level litoptern back)")
    print(f"  belly {belly:.2f}u = {belly/withers:.0%} of withers   "
          f"(LONG-LEGGED cursor is ~55-60%; the bison fork was ~45%)")
    print(f"  brisket {brisket:.2f}u = {brisket/withers:.0%} of withers   "
          f"(chest floor between the forelegs; always lower than the belly line)")
    print(f"  head top {head_top:.2f}u = {head_top/withers:.2f} x withers   "
          f"(alert camelid-necked animal is ~1.35-1.45)")
    print(f"  hind zigzag: stifle z "
          f"{(lo['hind_left_gaskin'][2]+hi['hind_left_gaskin'][2])/2:.2f}"
          f"  hock z {(lo['hind_left_cannon'][2]+hi['hind_left_cannon'][2])/2:.2f}")

    # ---- eye clearance (SOP phase 4 -- check EVERY wider ancestor) ----------
    print("\n" + "=" * 80)
    print("EYE CLEARANCE  (must sit outside every ancestor box, not just the skull)")
    print("=" * 80)
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head_skull", "neck_top", "neck_mid", "body_chest"):
            p = BY_NAME[anc]
            l = local_of(anc, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gaps = [max(a[i] - l[i], l[i] - b[i]) for i in range(3)]
            gap = max(gaps)
            print(f"  eye_{side:<6} vs {anc:<12} clear by {gap:6.2f} on "
                  f"{'xyz'[int(np.argmax(gaps))]}"
                  + ("" if gap > 0.3 else "   <== TOO CLOSE"))
        # siblings, not ancestors, but the boxes near enough to swallow an orbit
        for sib in ("head_nasal", "head_jaw", "head_muzzle"):
            p = BY_NAME[sib]
            l = local_of(sib, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gaps = [max(a[i] - l[i], l[i] - b[i]) for i in range(3)]
            gap = max(gaps)
            print(f"  eye_{side:<6} vs {sib:<12} clear by {gap:6.2f} on "
                  f"{'xyz'[int(np.argmax(gaps))]}"
                  + ("" if gap > 0.3 else "   <== TOO CLOSE"))

    # ---- size calibration --------------------------------------------------
    print("\n" + "=" * 80)
    print(f"SIZE CALIBRATION   mesh withers = {withers:.2f}u,  scale = m*16/withers")
    print("=" * 80)
    nose = min(lo["head_muzzle"][2], lo["proboscis_1"][2], lo["proboscis_2"][2])
    rear = hi["body_croup"][2]
    width = 2 * max(abs(v[0]) for v in list(lo.values()) + list(hi.values()))
    print(f"{'species':<20}{'m':>6}{'scale':>8}{'withers':>9}{'headtop':>9}"
          f"{'nose-rump':>11}{'width':>8}")
    for name, var, real in SPECIES:
        s = real * 16.0 / withers
        print(f"{name:<20}{real:6.2f}{s:8.3f}{withers*s/16:9.2f}{head_top*s/16:9.2f}"
              f"{(rear-nose)*s/16:11.2f}{width*s/16:8.2f}")
    print(f"registered hitbox {HITBOX[0]} x {HITBOX[1]} blocks"
          "   (per-TYPE, so the biggest species should sit close to it)")
    print("  NB nose-rump is the WORLD z span, and this animal carries its head UP, so it")
    print("     is much less than the ~3.0 m head-body length quoted for M. patachonica.")

    # ---- silhouette --------------------------------------------------------
    # n=15, not the default 7: a 7-sample grid leaves gaps wider than a cell of the 76x30
    # render, which reads as a speckled cloud rather than a silhouette -- exactly the
    # "truthful silhouette" the SOP asks for, made unreadable.
    pts = np.vstack([world(p[0], vol(p, 15)) for p in PARTS if p[6] != "eye"])
    print("\n" + "=" * 80)
    print("SIDE SILHOUETTE      <- nose      tail ->")
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

    # ---- UV budget ---------------------------------------------------------
    print("\n" + "=" * 80)
    print("UV FOOTPRINT BUDGET   (MC reserves 2*(w+d) x (h+d) from every texOffs)")
    print("=" * 80)
    tot = sum(fw * fh for fw, fh in (footprint(p) for p in PARTS))
    print(f"{len(PARTS)} boxes, {tot} px")
    for cw, ch in ((128, 64), (256, 128), (256, 256), (512, 256)):
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
    "fore": ("body_chest", TILT["body_chest"],
             [("shoulder", 8.0), ("forearm", 7.0), ("cannon", 6.0), ("foot", 2.4)]),
    "hind": ("body_croup", TILT["body_croup"],
             [("thigh", 8.5), ("gaskin", 7.5), ("cannon", 5.5), ("foot", 2.3)]),
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
        low = w[:, 1].min()
        if low < 0.0:
            pen += (-low) ** 2 * 12.0                     # never through the floor
        if k >= 2:                                        # cannon + foot must lie ON it
            pen += (low - 0.15) ** 2 * 3.0
    # distal segments want to be roughly horizontal, i.e. |cos(world angle)| small
    for a in angles[2:]:
        pen += (math.cos(D(a)) ** 2) * 4.0
    return pen


def pose_solve():
    """SOP phase 7: SOLVE sternal recumbency, never hand-guess it.

    The shipped ModelMacrauchenia inherits its sit/sleep fold angles wholesale from
    ModelBison -- a rig with legs 6u shorter than these.  Hand-guessed folds have failed
    repeatedly on this codebase (claws swung 6.3u through the floor once a hand folded),
    so the angles are grid-searched against the real box geometry instead.
    """
    rng = np.random.default_rng(7)
    print("=" * 80)
    print("PHASE 7 -- POSES SOLVED AGAINST REAL BOX GEOMETRY")
    print("=" * 80)
    body_lo = world("body_barrel", corners(BY_NAME["body_barrel"]))[:, 1].min()
    out = {}
    for pose, belly in (("sit", 2.2), ("sleep", 1.0)):
        dy = belly - body_lo
        print(f"\n{pose.upper()}   body drop dy = {dy:+.2f}u  (belly rests at y={belly})")
        for prefix in ("fore", "hind"):
            base = np.array([40.0, -70.0, 95.0, 95.0] if prefix == "fore"
                            else [70.0, -95.0, 80.0, 80.0])
            best, bs = base.copy(), _leg_score(prefix, base, dy)
            span = np.array([55.0, 55.0, 45.0, 45.0])
            for _ in range(9):
                cand = best + rng.normal(0, 1, (4000, 4)) * span
                cand[:, 3] = cand[:, 2] + rng.normal(0, 6, 4000)   # foot follows cannon
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
