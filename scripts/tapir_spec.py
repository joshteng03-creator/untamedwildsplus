#!/usr/bin/env python3
"""Tapir model spec + forward-kinematics checker -- SOP Phase 1 / 1b / 2.

A BRAND-NEW entity type (Mechanism B): before this, the only occurrence of the string
"tapir" anywhere in the tree was a comment on ModelMacrauchenia.java:165.  There is no
donor rig, so this is authored from scratch rather than forked.

Authoring space is BLOCKBENCH project space: Y up, ground y=0, -Z = front (nose),
-X = left.  Group origins and cube from/to are ABSOLUTE project coordinates, exactly as
Blockbench stores them, so this table doubles as the data for the build script.

    world(p) = A_root(A_child(... p))      A_g(p) = R_g @ (p - o_g) + o_g

Rotation order is Rz @ Ry @ Rx -- Blockbench's, and MC's ModelPart's under the verified
bbRot = [-rx, -ry, +rz] export flip.

TAPIR SILHOUETTE -- deliberately the OPPOSITE animal to the macrauchenia sharing this
codebase, so the two can never read as one rig retuned:
  * SHORT-LEGGED and stocky.  ~10u of leg under a 20u withers, belly at ~47% of withers.
    (Macrauchenia: 23u of leg under 28u, belly 58%.)
  * NO NECK to speak of -- one short thick segment, where the litoptern has three long ones.
  * a WEDGE body: narrow and tapering at the chest (hw 6.4), broad at the barrel (8.5) and
    rump.  That taper is functional -- a tapir is shaped to drive through dense undergrowth
    -- and it is the most recognisable thing about the animal from above.
  * an ARCHED back rising to a HIGH ROUNDED RUMP that then falls away steeply, versus the
    litoptern's near-level topline.
  * a short deep head carrying the diagnostic prehensile PROBOSCIS (nose + upper lip).
  * small rounded ears, a stiff erect NECK CREST, and a stub tail.

SEVEN species off one mesh.  tapir.json declares no synced flag (and EntityTapir will not
add one), so everything routes through getVariant() -- the ModelDeer:509-524 pattern:
  * CREST is showModel-gated: lowland/mountain/bairds/vero/megatapirus carry the erect
    mane, malayan does not.
  * CLAWS are showModel-gated to palorchestes alone -- the marsupial "tapir", whose huge
    clawed forelimbs are the one thing a shared mesh would otherwise lose.  This is the
    thylacine-on-a-canid-rig precedent, done properly: the convergent body plan is shared,
    the divergent character is modelled.

Run:  python scripts/tapir_spec.py            full report
      python scripts/tapir_spec.py --uv       UV shelf pack only
      python scripts/tapir_spec.py --zfight   z-fight sweep on the spec
      python scripts/tapir_spec.py --burial   volume-burial check
      python scripts/tapir_spec.py --pose     phase-7 sit/sleep solve
      python scripts/tapir_spec.py --json     compact JSON for the BB builder
"""
import math
import numpy as np

D = math.radians

# ---------------------------------------------------------------- contract
SPECIES = [
    # name,           variant, real shoulder height m, crest?, claws?
    ("lowland",        0, 1.05, True,  False),   # Tapirus terrestris
    ("malayan",        1, 1.08, False, False),   # Tapirus indicus -- the saddled one
    ("bairds",         2, 1.10, True,  False),   # Tapirus bairdii
    ("mountain",       3, 0.80, True,  False),   # Tapirus pinchaque -- woolly, smallest
    ("megatapirus",    4, 1.30, True,  False),   # Megatapirus augustus -- extinct giant
    ("vero_tapir",     5, 1.00, True,  False),   # Tapirus veroensis -- extinct N. American
    ("palorchestes",   6, 1.35, False, True),    # Palorchestes azael -- marsupial, clawed
]
HITBOX = (1.3, 1.2)      # proposed ModEntity.createEntity(EntityTapir::new,"tapir",1.3F,1.2F,..)

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
# Four nested masses.  Side planes step monotonically inboard from the barrel, which is
# the widest thing on the animal:  barrel 8.5 > croup 8.0 > withers 7.6 > chest 6.4.
# That 8.5 -> 6.4 taper IS the wedge -- it is not incidental spacing, it is the shape.
#
# THE RUMP SITS ABOVE THE BACK, AND THAT IS ALLOWED.  On the macrauchenia the croup had to
# be kept strictly UNDER the barrel's top, because a tilted rear box whose top CROSSED the
# barrel's produced 18.75u2 of coincident face along the topline.  Crossing is the problem,
# not height: here the croup's top stays 0.8-1.1u ABOVE the barrel's across the whole 3u
# they share and never meets it, so the high rounded rump reads exactly as a tapir's does
# and the sweep has nothing to report.
#     tops     croup 21.5(front, world 21.0)  >  withers 20.0  >  barrel 19.5  >  chest 18.5
#     bottoms  withers 11.5  >  croup 10.0  >  barrel 9.5  >  chest 8.0
add("body_barrel", None, (0, 15.0, 3.5), (0, 0, 0),
    (-8.5, 9.5, -5.0), (8.5, 19.5, 12.0), "body")
# Modest shoulder rise -- on a tapir the crest (the erect mane) does the visual work at the
# front, not the skeleton, and the high point of the animal is the RUMP.
# Top 18.8, NOT 20.0. At 20.0 the +7 tilt put this plane 0.71u ABOVE the barrel's at the
# front of the 8.5u they share and 0.33u BELOW at the rear -- i.e. the two crossed, and the
# sweep found 37.62u2 of coincident face, the largest patch in the model and sitting right
# along the topline. 18.8 keeps it 0.49-1.52u under the barrel everywhere they overlap,
# while its front (z=-9, forward of the barrel's face) still rises to 19.5 to fill the gap
# between the barrel and the neck. Being a PARENT/CHILD pair does not make a shared plane
# harmless -- that tag only means the overlap was deliberate, not that it cannot shimmer.
add("body_withers", "body_barrel", (0, 15.0, -3.0), (7, 0, 0),
    (-7.6, 11.5, -9.0), (7.6, 18.8, 3.5), "body")
# The WEDGE. Narrow (6.4 against the barrel's 8.5) and tilted nose-down: a tapir's chest
# tapers hard so the animal can drive through undergrowth head-first.
# Floor at 8.6, not 8.0: at 8.0 it passed 0.08u from the forearm's top face and the two are
# EXACTLY parallel (the forearm's world angle is -8, the same as the chest's tilt), so the
# sweep found 19.69u2 on each side. On a short-legged animal the chest floor and the leg
# segments are close together by construction, so this pair has to be checked deliberately.
# FRONT AT -8.0, NOT -14.0. This is the change that finally made the head read. At -14
# the chest was 13u long and reached almost to the base of the neck, so the muzzle
# projected barely 9u past it on a 47u animal: the CHEST was the front of the animal and
# the head rendered as facets of it. Two passes (compressing the whole body, then
# dropping and narrowing the head chain) both failed because both treated the head as
# the problem. Cutting the chest to 7u gives the head+proboscis ~17u of clear projection.
add("body_chest", "body_barrel", (0, 15.0, -5.0), (-8, 0, 0),
    (-6.4, 8.6, -8.0), (6.4, 18.5, -1.0), "body")
# High rounded rump, origin at its own FRONT face so the +18 tilt only ever drops the rear.
add("body_croup", "body_barrel", (0, 15.0, 9.5), (18, 0, 0),
    (-8.0, 10.0, 9.5), (8.0, 21.5, 17.5), "body")

TILT = {"body_chest": -8.0, "body_croup": 18.0}


# ================================================================ chain helpers
def zchain(parent, pivot, segs, cls, parent_tilt=0.0):
    """Chain authored lying forward along -Z from `pivot`; bent by WORLD angles.

    A POSITIVE angle raises the forward end.  EVERY CHILD OF A SEGMENT MUST BE AUTHORED IN
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


def leg(prefix, parent, pivot_z, segs, parent_tilt):
    """Leg authored hanging straight down from a pivot whose HEIGHT IS SOLVED.

    The sole lands on y = 0 when the pivot's WORLD height equals sum(L*cos(angle)); the
    AUTHORED height is not that number, because the parent's own tilt drags the pivot off
    it.  Solve rather than assume -- the croup's +18 moves it by well over a unit.
    """
    drop = sum(L * math.cos(D(a)) for _, a, L, *_ in segs)
    y_auth = drop
    for _ in range(12):
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
# ONE short thick segment. This is the single clearest difference from the macrauchenia
# rig (three long ones) and it is what stops a viewer reading the two as the same animal.
#
# THE CHAIN GOES DOWN, NOT UP. The first pass ran it at +14/+2/-14 with half-widths
# 5.4/4.4/3.4 against a 6.4 chest, and the grey render came back with NO HEAD AT ALL --
# the front of the animal was one continuous wedge from rump to nose. That is exactly the
# toxodon's fourth defect, and its lesson is that fixing it needs a LENGTH cut, a WIDTH cut
# and a real DROP together; no one of the three works alone. Here:
#   * DROP: -12/-18/-26. A tapir carries its head LOW, at or below shoulder level, so the
#     skull ends up around y 13-17 against a 19.5 barrel top and a 21.8 rump -- which opens
#     a genuine notch between the shoulder and the head.
#   * WIDTH: 4.6 -> 3.6 -> 2.8 against the chest's 6.4 (was only 5.4, i.e. barely tapered).
# Cumulative world angle reaches -26, nowhere near the -38 that once put a cervid snout
# through the floor.
# Rest-frame anchors for anything parented into this chain -- author children HERE, not at
# their apparent world position:
#     neck        z -12.6..-5.0       head_skull  z -19.0..-11.0
#     head_muzzle z -23.2..-17.4      all about y 15.5
# The neck's REAR OVERLAP is 3.0, not the usual 1.6: the chain now starts at the chest's
# own front face, so without the deeper seat the neck box would begin in mid-air with a
# visible gap behind it.
# head_skull's `up` is 2.8 against the neck's 3.2 -- at 3.2 both tops sat at y 18.7, only
# 6 degrees apart, and the sweep found 11.30u2. A tapir's skull sits below its crest line
# anyway.
zchain("body_chest", (0, 15.5, -8.0), [
    #  name           ang    L    hw   up   dn   ov
    ("neck",         -12,  4.6, 4.6, 3.2, 4.0, 3.0),
    ("head_skull",   -18,  6.4, 3.8, 2.8, 3.4, 1.6),
    ("head_muzzle",  -26,  4.2, 2.9, 2.4, 2.8, 1.6),
], "head", parent_tilt=TILT["body_chest"])

# Deep short mandible. Child of head_skull, NOT head_muzzle, so it stays one rigid bone and
# the muzzle's extra 16 degrees cannot crack a gap open along the tooth row. Front stops
# 1.4u behind the muzzle's own face: two near-parallel front planes a few tenths apart and
# DIVERGING is a guaranteed shimmering patch.
# Front at -21.0, a full 2.2u behind the muzzle's -23.2: at -21.8 the jaw's front face
# crossed the drooping proboscis tip and the sweep found 2.36u2 there.
add("head_jaw", "head_skull", (0, 13.0, -15.0), (0, 0, 0),
    (-2.5, 10.8, -21.0), (2.5, 14.6, -11.5), "head")

# THE DIAGNOSTIC FEATURE: the prehensile proboscis (fused nose + upper lip), hanging
# forward and DOWN off the muzzle. Two segments, each keeping >=2u inside its parent --
# chained joints need ~1u minimum and these carry 22-28 degrees of relative bend, which
# needs more or the joint cracks open the moment it animates.
add("proboscis_1", "head_muzzle", (0, 14.8, -22.4), (-42, 0, 0),
    (-2.0, 13.0, -26.6), (2.0, 16.4, -21.8), "nose")
add("proboscis_2", "proboscis_1", (0, 14.0, -25.2), (-36, 0, 0),
    (-1.5, 12.2, -28.4), (1.5, 15.6, -24.0), "nose")

# Small rounded ears, set high and back, swept up and out.
# rz SIGN: R_z sends a point at +x toward +y, so a tip authored at -x (the LEFT ear) only
# rises under a NEGATIVE rz. Getting this backwards rolls both ear tips DOWN into the skull
# and the render still looks plausible -- it cost a full round trip on the toxodon.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"ear_{side}", "head_skull", (s * 2.4, 17.6, -13.0), (6, s * -14, s * 38),
        (s * 2.4, 16.8, -14.2), (s * 5.6, 18.8, -11.6), "ear")

# Eyes: whole-unit 2.0 x 2.0 zero-width planes, blinked by retracting them INTO the skull.
# 2.0, NOT the toxodon's 3.0 -- a 3u plane on a head this size renders the painted row as a
# black BAR across a third of the face. At a FRACTIONAL depth the outward and inward halves
# would land on overlapping texel rects and alpha-0ing one would eat a column of the other,
# giving the two sides visibly different eyes from symmetric geometry.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"eye_{side}", "head_skull", (s * 4.2, 16.8, -16.5), (0, 0, 0),
        (s * 4.2, 15.8, -17.5), (s * 4.2, 17.8, -15.5), "eye")

# ================================================================ NECK CREST
# The stiff erect mane. showModel-gated OFF for malayan and palorchestes.
# Authored in the NECK's rest frame (see the anchors above), running along its top and
# rising into a blade. Half-width 1.1 keeps it clear of everything; its base is buried 1.5u
# inside the neck so no gap can open where it meets the shoulder.
add("crest", "neck", (0, 18.4, -9.5), (0, 0, 0),
    (-1.1, 17.2, -13.2), (1.1, 21.4, -4.5), "crest")

# ================================================================ TAIL
# A stub -- a tapir's tail is almost vestigial, and that is itself diagnostic.
# Authored as an explicit box straddling its pivot with a NEGATIVE rx, never via zchain:
# zchain lays a segment forward along -Z and then rotates, so a tail needs an angle past
# -90 to end up behind its pivot and sweeps through the rump on the way. Built that way the
# z-fight sweep reports NOTHING while most of the tail sits buried inside the croup -- 64%
# of the toxodon's dock was lost exactly like that. Use --burial, not --zfight, to check.
add("tail", "body_croup", (0, 19.6, 16.6), (-30, 0, 0),
    (-1.5, 15.6, 15.9), (1.5, 20.4, 18.6), "tail")

# ================================================================ LEGS
# SHORT and stout: ~10u of leg under a 20u withers, against the macrauchenia's 23u under 28.
# Lateral planes step monotonically inboard so no two boxes that overlap in projection come
# within 0.2u of sharing one. INNER faces need the same treatment as outer ones -- two
# segments sharing an inner plane z-fight exactly as readily:
#   fore  outer 9.0 / 7.4 / 6.0 / 6.8   inner 2.4 / 3.0 / 3.8 / 3.0
#   hind  outer 9.0 / 7.2 / 5.8 / 6.6   inner 1.8 / 2.5 / 3.4 / 2.6
# `fr`/`bk` are stepped too: every segment of a leg is authored around the SAME pivot_z, so
# equal `bk` values put two segments' rear planes at an identical z BY CONSTRUCTION, and
# consecutive segments sit only a few degrees apart -- well inside the 8-degree window.
# Both limb roots sit PROUD of the barrel (9.0 vs 8.5): a shoulder tucked just inside the
# torso plane is the pairing that produced 20-38u2 of shared face on the deer.
FORE_PIVOT_Z = -4.0          # also where the withers height is measured (see main())
FORE_DROP = leg("fore", "body_chest", FORE_PIVOT_Z, [
    #  name        ang   L    outer inner  fr   bk   ov
    ("shoulder",     8, 4.4, 9.0, 2.4, 3.4, 2.8, 1.2),
    ("forearm",     -8, 2.8, 7.4, 3.0, 2.6, 2.2, 0.9),
    ("cannon",       3, 2.3, 6.0, 3.8, 1.9, 1.6, 0.9),
    ("foot",         0, 1.7, 6.8, 3.0, 2.8, 1.0, 0.7),
], TILT["body_chest"])

HIND_DROP = leg("hind", "body_croup", 13.2, [
    ("thigh",       22, 4.0, 9.0, 1.8, 2.9, 3.2, 1.4),
    ("gaskin",     -28, 3.4, 7.2, 2.5, 2.8, 2.4, 1.2),
    ("cannon",      10, 2.1, 5.8, 3.4, 1.9, 1.6, 0.9),
    ("foot",         0, 1.6, 6.6, 2.6, 2.6, 1.0, 0.7),
], TILT["body_croup"])

# ================================================================ PALORCHESTES CLAWS
# showModel-gated ON for palorchestes alone. Palorchestes azael is a diprotodontid
# marsupial whose huge clawed forelimbs are its single most distinctive character -- the
# thing a shared tapir mesh would otherwise erase. Modelling it (rather than letting the
# skin try to carry it) is what keeps the species from reading as a recoloured tapir.
# Authored in each fore foot's own frame, projecting forward and down past the toe.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"claw_{side}", f"fore_{side}_foot", (s * 4.9, 1.0, -6.8), (0, 0, 0),
        (s * 3.2, 0.0, -9.6), (s * 6.6, 1.9, -6.2), "claw")

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
    """Fraction of each part's VOLUME inside another box.

    The z-fight sweep is structurally blind to this: a part completely swallowed by a
    neighbour shares NO coincident face and is reported clean while being invisible in
    game. Nested core masses reading 65-80% are the intended massing; a FEATURE cube
    (crest, claw, tail, proboscis) reading high is the defect.
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
        worst.append((inside.mean(), p[0], p[6]))
    for frac, name, cls in sorted(worst, reverse=True):
        flag = ""
        if frac > 0.55:
            flag = ("   <== FEATURE MOSTLY BURIED" if cls in ("crest", "claw", "tail", "nose", "ear")
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

    # WITHERS = the topline height ABOVE THE FORELEG, never the withers box's world-AABB
    # max. The box is tilted and runs forward, so its AABB max is a front-top corner over
    # the base of the neck -- on the macrauchenia that read 1.8u high and put a 6% error
    # straight into every species' `scale`.
    wp = BY_NAME["body_withers"]
    withers = world("body_withers", [(0.0, wp[5][1], FORE_PIVOT_Z)])[0][1]
    cr = BY_NAME["body_croup"]
    rump = world("body_croup", [(0.0, cr[5][1], cr[5][2])])[0][1]
    rump_front = world("body_croup", [(0.0, cr[5][1], cr[4][2])])[0][1]
    belly = lo["body_barrel"][1]
    stance = ((lo["hind_left_foot"][2] + hi["hind_left_foot"][2]) / 2
              - (lo["fore_left_foot"][2] + hi["fore_left_foot"][2]) / 2)
    print(f"  fore->hind stance {stance:.2f}u = {stance/withers:.2f} x withers"
          f"   (a compact short-legged browser is ~0.80-1.00)")
    print(f"  withers {withers:.2f}u   rump PEAK {rump_front:.2f}u   rump rear {rump:.2f}u")
    print(f"  rump rise over withers {rump_front-withers:+.2f}u"
          f"   (must be POSITIVE: the high rounded tapir rump)")
    print(f"  belly {belly:.2f}u = {belly/withers:.0%} of withers   "
          f"(SHORT-LEGGED browser ~45-50%; the macrauchenia is 58%)")

    # ---- the wedge -------------------------------------------------------
    print("\n" + "=" * 80)
    print("BODY WEDGE   (the diagnostic taper -- chest must be much narrower than barrel)")
    print("=" * 80)
    for n in ("body_chest", "body_withers", "body_croup", "body_barrel"):
        print(f"  {n:<16} half-width {abs(BY_NAME[n][5][0]):5.2f}")
    taper = abs(BY_NAME['body_chest'][5][0]) / abs(BY_NAME['body_barrel'][5][0])
    print(f"  chest/barrel = {taper:.2f}   (a tapir is ~0.72-0.80; 1.0 would be a barrel)")

    # ---- eye clearance (check EVERY wider ancestor) ----------------------
    print("\n" + "=" * 80)
    print("EYE CLEARANCE  (must sit outside every ancestor box, not just the skull)")
    print("=" * 80)
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head_skull", "neck", "body_chest"):
            p = BY_NAME[anc]
            l = local_of(anc, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gaps = [max(a[i] - l[i], l[i] - b[i]) for i in range(3)]
            gap = max(gaps)
            print(f"  eye_{side:<6} vs {anc:<12} clear by {gap:6.2f} on "
                  f"{'xyz'[int(np.argmax(gaps))]}"
                  + ("" if gap > 0.3 else "   <== TOO CLOSE"))
        for sib in ("head_jaw", "head_muzzle", "crest"):
            p = BY_NAME[sib]
            l = local_of(sib, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gaps = [max(a[i] - l[i], l[i] - b[i]) for i in range(3)]
            gap = max(gaps)
            print(f"  eye_{side:<6} vs {sib:<12} clear by {gap:6.2f} on "
                  f"{'xyz'[int(np.argmax(gaps))]}"
                  + ("" if gap > 0.3 else "   <== TOO CLOSE"))

    # ---- size calibration ------------------------------------------------
    print("\n" + "=" * 80)
    print(f"SIZE CALIBRATION   mesh withers = {withers:.2f}u,  scale = m*16/withers")
    print("=" * 80)
    nose = min(lo["head_muzzle"][2], lo["proboscis_1"][2], lo["proboscis_2"][2])
    rear = hi["body_croup"][2]
    width = 2 * max(abs(v[0]) for v in list(lo.values()) + list(hi.values()))
    top = max(hi["crest"][1], hi["body_croup"][1], hi["ear_left"][1])
    print(f"{'species':<15}{'m':>6}{'scale':>8}{'withers':>9}{'top':>7}"
          f"{'nose-rump':>11}{'width':>8}  flags")
    for name, var, real, crest, claws in SPECIES:
        s = real * 16.0 / withers
        fl = ("crest " if crest else "") + ("CLAWS" if claws else "")
        print(f"{name:<15}{real:6.2f}{s:8.3f}{withers*s/16:9.2f}{top*s/16:7.2f}"
              f"{(rear-nose)*s/16:11.2f}{width*s/16:8.2f}  {fl}")
    print(f"proposed hitbox {HITBOX[0]} x {HITBOX[1]} blocks"
          "   (per-TYPE, so the biggest species should sit close to it)")

    # ---- silhouette ------------------------------------------------------
    # n=15, not the default 7: a sparser grid leaves gaps wider than a render cell and
    # reads as a speckled cloud rather than a silhouette.
    pts = np.vstack([world(p[0], vol(p, 15)) for p in PARTS
                     if p[6] not in ("eye", "claw")])
    print("\n" + "=" * 80)
    print("SIDE SILHOUETTE  (crest shown, claws hidden)      <- nose      tail ->")
    print("=" * 80)
    zmin, zmax, ymax = pts[:, 2].min(), pts[:, 2].max(), pts[:, 1].max()
    W, H = 76, 26
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
             [("shoulder", 4.4), ("forearm", 2.8), ("cannon", 2.3), ("foot", 1.7)]),
    "hind": ("body_croup", TILT["body_croup"],
             [("thigh", 4.0), ("gaskin", 3.4), ("cannon", 2.1), ("foot", 1.6)]),
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


def pose_solve():
    """SOP phase 7: SOLVE sternal recumbency, never hand-guess it.

    Hand-guessed folds have failed repeatedly on this codebase -- claws swung 6.3u through
    the floor once a hand folded, and the shipped ModelToxodon carried bison fold angles it
    admitted in a comment were never re-derived. There is no donor rig here to inherit from
    at all, so these are solved from scratch against the real box geometry.
    """
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
            base = np.array([40.0, -70.0, 95.0, 95.0] if prefix == "fore"
                            else [70.0, -95.0, 80.0, 80.0])
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
