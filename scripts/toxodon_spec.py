#!/usr/bin/env python3
"""Toxodon (notoungulate) model spec + forward-kinematics checker -- SOP Phase 1 / 1b / 2.

Replaces the 19-box / 128x128 rig that was forked off ModelBison and never re-derived
(ModelToxodon.java:244-246 admits its sit/sleep fold angles are inherited leftovers).

Authoring space is BLOCKBENCH project space: Y up, ground y=0, -Z = front (nose),
-X = left.  Group origins and cube from/to are ABSOLUTE project coordinates, exactly as
Blockbench stores them, so this table doubles as the data for the build script.

A group rotates everything inside it about its own origin:
    world(p) = A_root(A_child(... p))      A_g(p) = R_g @ (p - o_g) + o_g
Rotation order is Rz @ Ry @ Rx -- what Blockbench uses, and what MC's ModelPart uses
under the verified bbRot = [-rx, -ry, +rz] export flip.

Limbs, the neck chain and the tail are NOT hand-placed: they are described by WORLD
segment angles and lengths, the joint chain is walked, the pivot height is solved so the
sole lands exactly on y=0, and each part's LOCAL rotation is derived as
(world angle - parent world angle).  That is the equid lesson -- a local angle that reads
fine in isolation is several degrees off in world space once a parent tilt is inherited.

TOXODON SILHOUETTE (what the numbers below are trying to say):
  * high withers over a deep chest, topline sloping DOWN to a low rump -- the neural
    spines over the shoulder are the tallest point of the skeleton
  * short, very stout legs on broad three-toed feet; belly around 40% of shoulder height
  * a big, deep, low-carried head with a heavy jaw and a broad muzzle
  * high-set orbits and small high ears (the semi-aquatic hippo-like reading)
  * the diagnostic procumbent chisel incisor battery projecting past the muzzle
  * a short, thick, drooping tail

Two species share the one mesh (toxodon.json declares NO model flags), so
ModelToxodon.setupAnim reads getVariant() -- the ModelDeer / ModelEquid / ModelAntelope
pattern.  Both species use identical geometry and differ only by `scale` and skin.

Run:  python scripts/toxodon_spec.py            full report
      python scripts/toxodon_spec.py --uv       UV shelf pack only
      python scripts/toxodon_spec.py --zfight   z-fight sweep on the spec
      python scripts/toxodon_spec.py --pose     phase-7 sit/sleep solve
      python scripts/toxodon_spec.py --json     compact JSON for the Blockbench builder
"""
import math
import numpy as np

D = math.radians

# ---------------------------------------------------------------- contract
SPECIES = [
    # name,          variant, real shoulder height m
    ("toxodon",      0, 1.50),      # Toxodon platensis
    ("mixotoxodon",  1, 1.75),      # Mixotoxodon larensis -- the larger northern species
]
HITBOX = (1.7, 1.5)          # ModEntity: createEntity(EntityToxodon::new,"toxodon",1.7F,1.5F,..)

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
# Four big NESTED masses (SOP phase 2), each buried in the previous one so the topline
# reads as one continuous line rather than a stack of slabs.  Side planes are all
# distinct and >=0.3u apart so no two torso boxes can ever share one:
#     barrel 9.0  >  croup 8.4  >  withers 7.9  >  chest 7.4
# The withers box is TALL (14.1u) with its lower half buried inside the barrel, so only
# the shoulder hump emerges -- that is what makes the high-wither/low-rump topline read.
# HORIZONTAL planes have to be spaced as carefully as the side planes, and it is much
# easier to miss: two boxes with the same authored top or bottom y but different tilts
# still share that plane wherever the tilt has not yet separated them.  The first pass
# gave barrel and chest the same top (26.0) and the sweep found 36u2 of it.
#   tops     withers 27.6  >  barrel 26.0  >  chest 24.2  >  croup 24.0
#   bottoms  withers 15.0  >  croup 14.0  >  barrel 12.5  >  chest 10.8
# A TILTED box's horizontal planes have to clear its neighbour's along the WHOLE shared
# span, not just at the origin.  The chest's -6 lifts its top plane 0.84u over the 8u it
# shares with the barrel and the croup's +13 lifts its own by 1.35u, so a top merely
# "below" the barrel's still crosses it at the far end -- the first pass had 36u2 of
# exactly that, the single biggest coincident patch in the model.
add("body_barrel", None, (0, 20.0, 5.0), (0, 0, 0),
    (-9.0, 12.5, -5.0), (9.0, 26.0, 17.0), "body")
# Bottom at 15.0, not 13.5: at 13.5 it sat 0.12u off the forearm's top face and the two
# shimmered across 14.6u2.  The withers box only exists to carry the shoulder hump, so
# burying its underside deeper in the barrel costs nothing.  Its rear stops at 5.0 so it
# never shares a span with the croup at all.
add("body_withers", "body_barrel", (0, 20.0, -1.0), (0, 0, 0),
    (-7.9, 15.0, -10.0), (7.9, 27.6, 5.0), "body")
# Chest tilts the FRONT DOWN (-6): the toxodon carries its head low, so the shoulder
# mass has to slope forward-and-down into the neck instead of rearing up like a cervid's.
add("body_chest", "body_barrel", (0, 21.0, -5.0), (-6, 0, 0),
    (-7.4, 10.8, -14.0), (7.4, 24.2, 3.0), "body")
# Croup tilts the REAR DOWN (+13): positive rx sends a +z point downward, which is the
# whole point -- rump below withers is the diagnostic notoungulate topline.  Raised from
# +9 because at +9 the rump read as a flat vertical wall in the grey render.
add("body_croup", "body_barrel", (0, 22.0, 12.0), (13, 0, 0),
    (-8.4, 14.0, 6.0), (8.4, 24.0, 22.0), "body")

TILT = {"body_chest": -6.0, "body_croup": 13.0}   # inherited world tilt of a limb root


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
    pivot off it -- here the chest's -6 and the croup's +9 both move it by most of a
    unit.  Solve for the authored value instead of assuming.
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
# Short thick neck carrying a big head LOW -- the cumulative world angle runs
# -6 (chest) -> -5 -> -11 -> -17, i.e. every segment adds only ~6 degrees.  Kept
# deliberately shallow because -38 degrees of accumulated chest+neck+head once put a
# cervid snout through the floor.  A first pass at -10/-16/-22 dropped the muzzle to 13.5u
# on a 27.6u animal -- a permanently-grazing head; this lands it near 16u (0.87 m), level
# with the chest, which is where a toxodon actually carries it, and the walk cycle's head
# bob takes it down from there.
# Half-widths taper 6.2 -> 5.4 -> 4.6 so no two chain segments share a side plane.
# THE FRONT END IS THE PART THAT WENT WRONG FIRST.  A 6.0/8.0/5.0 chain at -5/-11/-17
# put head+neck across 21.6u of a 52u animal -- 42% of its length -- carried at shoulder
# height and shoulder width, and the flat-grey render came back as ONE continuous slab
# from rump to nose with no head on it at all.  A real Toxodon skull is ~0.65 m on a
# 2.7 m body, i.e. 22%.  Two things fix it together and neither works alone:
#   1. LENGTH.  4.5 + 6.5 + 4.5 = 15.5u of head+neck (skull+muzzle 11u = 22% of the body).
#   2. DROP.  -12 / -20 / -26 instead of -5 / -11 / -17, so the skull roof clears the
#      withers by ~6u and a real notch opens between shoulder and head.
# The chain also NARROWS hard (5.2 -> 5.0 -> 4.2 against a 7.4 chest), which is what makes
# the neck read as a neck rather than as the front of the barrel.
# Cumulative world angle still only reaches -26, well inside the range that once put a
# cervid snout through the floor at -38.
zchain("body_chest", (0, 22.5, -11.0), [
    #  name          ang    L    hw   up   dn   ov
    ("neck",         -12,  4.5, 5.2, 4.4, 5.0, 1.6),
    ("head_skull",   -20,  6.5, 5.0, 3.6, 4.4, 1.6),
    ("head_muzzle",  -26,  4.5, 4.2, 3.0, 3.4, 1.6),
], "head", parent_tilt=TILT["body_chest"])

# Heavy jowl/throat mass slung under the neck.  Its top is buried 2u inside the neck;
# only the underside emerges.  Child of `neck`, so it follows head yaw but not head pitch.
# WIDER than the neck it hangs from (5.6 vs 5.2) -- jowls bulge, and it also keeps the
# throat's side planes clear of both the neck's and the jaw's.
add("throat", "neck", (0, 19.5, -13.0), (0, 0, 0),
    (-5.6, 15.0, -17.0), (5.6, 19.5, -10.0), "head")

# Deep lower jaw.  Child of head_skull (NOT head_muzzle) so it stays one rigid mandible;
# its top at 21.5 is 3.4u inside the skull and 2.4u inside the muzzle, so the 6 degrees
# of muzzle tilt it does not inherit can never crack a gap open along the tooth row.
# Its front stops at -26.5, a clear 1.5u behind the muzzle's own front face.  At -27.5 the
# two front planes were 0.5u apart but diverging by the muzzle's extra 6 degrees, and the
# sweep found 8.8u2 where they crossed.  The incisors are anchored in the MUZZLE, not the
# jaw, so nothing needs the jaw to reach them.
add("head_jaw", "head_skull", (0, 18.0, -19.0), (0, 0, 0),
    (-4.6, 14.5, -24.5), (4.6, 20.0, -13.0), "head")

# Frontal boss: a narrow raised crest over the orbits.  Bottom buried in the skull roof,
# only the top ~1.5u shows.  Narrow (4.2) so it can never swallow the eye planes at 5.8.
# Its underside sits at 26.0, NOT flush with the muzzle's 26.9 top -- the two overlap in
# both x and z, so a 0.1u gap there is a guaranteed shimmering coincident patch.
add("head_brow", "head_skull", (0, 25.4, -18.5), (0, 0, 0),
    (-3.8, 24.0, -22.5), (3.8, 26.8, -14.5), "head")

# The diagnostic chisel incisor battery, projecting past the muzzle.  Each keeps ~1u
# inside the muzzle so no bite/graze animation can open a wedge gap at the join.
add("incisor_lower", "head_muzzle", (0, 20.5, -26.0), (0, 0, 0),
    (-2.8, 19.6, -29.0), (2.8, 21.4, -25.5), "tooth")
add("incisor_upper", "head_muzzle", (0, 22.5, -26.0), (0, 0, 0),
    (-2.4, 21.8, -28.5), (2.4, 23.2, -25.5), "tooth")

# Small, high-set, rounded ears swept up and out.
# rz SIGN: R_z sends a point at +x toward +y, so a tip authored at -x (the LEFT ear) only
# rises under a NEGATIVE rz.  The first pass used s * -48 and rolled both ear tips
# DOWNWARD into the skull -- the world AABB showed ear top 27.13 against a skull top of
# 27.06, i.e. the ears had all but vanished, while the render would still have looked
# plausible.  s * +45 is the correct sign.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"ear_{side}", "head_skull", (s * 3.2, 25.6, -15.0), (10, s * -20, s * 45),
        (s * 3.2, 25.0, -16.2), (s * 6.8, 26.4, -13.4), "ear")

# Eyes: the ModelEquid/ModelDeer design -- a 3x3 zero-width plane on its own 6x6 UV
# footprint, blinked by retracting it INTO the skull rather than by swapping art.
# Set HIGH and well FORWARD so they clear the neck (hw 6.2) as well as the skull (5.4);
# on the equid, eyes that cleared the skull were still buried in the neck and rendered
# as a triangular sliver.  High orbits are correct toxodon anatomy anyway.
for side, s in (("left", -1.0), ("right", 1.0)):
    add(f"eye_{side}", "head_skull", (s * 5.4, 24.0, -19.5), (0, 0, 0),
        (s * 5.4, 22.5, -21.0), (s * 5.4, 25.5, -18.0), "eye")

# ================================================================ TAIL
# Short and thick, hung from the croup's rear-top and swinging DOWN AND BACK.
#
# Authored with explicit boxes, NOT zchain.  zchain lays a segment forward along -Z and
# then rotates it, so a tail needs an angle past -90 to end up behind its pivot -- and
# even then the box sweeps through the rump on the way.  Built that way the sweep said
# nothing (no coincident faces) while 64% of the dock's volume and 26% of the tip's sat
# INSIDE body_croup and the rear render showed no tail at all.  The cervid rig has the
# same latent bug at -62/-78.
#
# Here each segment straddles its pivot and carries a negative rx, which sends everything
# below the pivot backwards: the dock's lower end lands ~2u clear of the croup's rear
# face.  Half-width 1.4 threads between the thighs (inner face 1.8) with 0.4 to spare.
# The 25 degrees also puts these boxes well outside the sweep's 8-degree near-parallel
# window relative to the rump, so nothing here can share a plane with it.
add("tail_dock", "body_croup", (0, 22.5, 21.0), (-25, 0, 0),
    (-1.4, 15.5, 20.2), (1.4, 23.5, 23.8), "tail")
add("tail_tip", "tail_dock", (0, 16.0, 22.5), (-15, 0, 0),
    (-1.0, 11.0, 21.6), (1.0, 16.6, 24.4), "tail")

# ================================================================ LEGS
# Short and stout: ~18.5u of leg under a 27.6u withers.  Lateral planes are spaced so no
# two boxes that overlap in projection come within 0.2u of sharing one:
#   fore  outer 9.6 / 8.2 / 7.0 / 8.4   inner 2.4 / 3.0 / 3.8 / 2.6
#   hind  outer 9.6 / 7.8 / 7.0 / 8.8   inner 1.8 / 2.6 / 3.4 / 2.2
# The feet flare back OUT (8.4 / 8.8) rather than continuing inboard -- a broad splayed
# three-toed foot is diagnostic, and the flare is safe because the foot clears the
# segment above it by >1.4u and shares no vertical range with anything higher.
# Both limb roots sit PROUD of the barrel (9.6 vs 9.0): a shoulder tucked just inside the
# torso plane is the pairing that produced 20-38u2 of shared face on the deer.
FORE_DROP = leg("fore", "body_chest", -5.0, [
    #  name        ang   L   outer inner  fr   bk   ov
    ("shoulder",     6, 6.5, 9.6, 2.4, 4.5, 3.5, 1.8),
    ("forearm",     -7, 5.5, 8.2, 3.0, 3.2, 2.8, 2.0),
    ("cannon",       3, 4.2, 7.0, 3.8, 2.4, 2.2, 1.2),
    ("foot",         0, 2.6, 8.4, 2.6, 4.2, 2.0, 0.8),
], TILT["body_chest"])

HIND_DROP = leg("hind", "body_croup", 16.0, [
    ("thigh",       24, 6.8, 9.6, 1.8, 5.0, 4.2, 2.0),
    ("gaskin",     -32, 6.4, 7.8, 2.6, 3.4, 3.0, 1.6),
    ("cannon",      10, 4.0, 7.0, 3.4, 2.4, 2.2, 1.2),
    ("foot",         0, 2.5, 8.8, 2.2, 4.0, 2.2, 0.8),
], TILT["body_croup"])

# ================================================================ UV pack
CANVAS = (256, 128)


def footprint(p):
    """The region MC reserves from texOffs: 2*(w+d) x (h+d), in texels.

    CEIL, not round.  The Cube constructor maps UVs from the raw float sizes passed to
    addBox, and these boxes are not integer-sized, so rounding down by half a texel lets
    a face sample its neighbour's paint.
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
    anti-flicker fix clears opposite UV halves on eye_left and eye_right, which is
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
    withers = hi["body_withers"][1]
    # The croup is TILTED, so its world-AABB max y is its FRONT top corner and says
    # nothing about the rump.  Measure the actual rear-top corner instead, or the
    # topline-drop check silently passes on a model whose rump is not dropped at all.
    cr = BY_NAME["body_croup"]
    rump = world("body_croup", [(0.0, cr[5][1], cr[5][2])])[0][1]
    belly = min(lo["body_barrel"][1], lo["body_chest"][1])
    print(f"  fore->hind stance {stance:.2f}u = {stance/withers:.2f} x withers"
          f"   (a compact bulky notoungulate is ~0.65-0.75)")
    print(f"  withers {withers:.2f}u   rump {rump:.2f}u   "
          f"topline drop {withers-rump:+.2f}u   (must be POSITIVE: high wither, low rump)")
    print(f"  belly {belly:.2f}u = {belly/withers:.0%} of withers   "
          f"(short-legged grazer is ~40%)")
    print(f"  hind zigzag: stifle z "
          f"{(lo['hind_left_gaskin'][2]+hi['hind_left_gaskin'][2])/2:.2f}"
          f"  hock z {(lo['hind_left_cannon'][2]+hi['hind_left_cannon'][2])/2:.2f}")

    # ---- eye clearance (SOP phase 4 -- check EVERY wider ancestor) ----------
    print("\n" + "=" * 80)
    print("EYE CLEARANCE  (must sit outside every ancestor box, not just the skull)")
    print("=" * 80)
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head_skull", "neck", "body_chest", "body_barrel"):
            p = BY_NAME[anc]
            l = local_of(anc, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gaps = [max(a[i] - l[i], l[i] - b[i]) for i in range(3)]
            gap = max(gaps)
            print(f"  eye_{side:<6} vs {anc:<12} clear by {gap:6.2f} on "
                  f"{'xyz'[int(np.argmax(gaps))]}"
                  + ("" if gap > 0.3 else "   <== TOO CLOSE"))
        # the brow and jaw are siblings, not ancestors, but are the two boxes near
        # enough to swallow an orbit if either is ever widened
        for sib in ("head_brow", "head_jaw", "head_muzzle"):
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
    head_top = max(hi["head_brow"][1], hi["ear_left"][1], hi["head_skull"][1])
    nose = min(lo["head_muzzle"][2], lo["incisor_lower"][2], lo["incisor_upper"][2])
    # head-BODY length, i.e. nose to rump, EXCLUDING the tail -- that is the dimension
    # the 2.7 m figure for Toxodon platensis is actually quoted at.
    rear = hi["body_croup"][2]
    width = 2 * max(abs(v[0]) for v in list(lo.values()) + list(hi.values()))
    print(f"{'species':<14}{'m':>6}{'scale':>8}{'withers':>9}{'headtop':>9}"
          f"{'hd-body':>9}{'width':>8}")
    for name, var, real in SPECIES:
        s = real * 16.0 / withers
        print(f"{name:<14}{real:6.2f}{s:8.3f}{withers*s/16:9.2f}{head_top*s/16:9.2f}"
              f"{(rear-nose)*s/16:9.2f}{width*s/16:8.2f}")
    print(f"registered hitbox {HITBOX[0]} x {HITBOX[1]} blocks"
          "   (per-TYPE, so the biggest species should sit close to it)")
    print(f"  real Toxodon platensis is ~2.7 m long at 1.5 m tall -> head-body/withers"
          f" 1.80;  mesh gives {(rear-nose)/withers:.2f}")

    # ---- silhouette --------------------------------------------------------
    # n=15, not the default 7: these boxes are up to 24u across, and a 7-sample grid
    # leaves gaps wider than a cell of the 76x30 render, which reads as a speckled cloud
    # rather than a silhouette -- exactly the "truthful silhouette" the SOP asks for,
    # made unreadable.
    pts = np.vstack([world(p[0], vol(p, 15)) for p in PARTS if p[6] != "eye"])
    print("\n" + "=" * 80)
    print("SIDE SILHOUETTE      <- nose      tail ->")
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
    tot = sum(fw * fh for fw, fh in (footprint(p) for p in PARTS))
    print(f"{len(PARTS)} boxes, {tot} px")
    for cw, ch in ((256, 128), (256, 256), (512, 256)):
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
             [("shoulder", 6.5), ("forearm", 5.5), ("cannon", 4.2), ("foot", 2.6)]),
    "hind": ("body_croup", TILT["body_croup"],
             [("thigh", 6.8), ("gaskin", 6.4), ("cannon", 4.0), ("foot", 2.5)]),
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

    This is the specific defect being fixed -- the shipped ModelToxodon carries fold
    angles inherited from the bison fork that were never re-derived for its own geometry.
    """
    rng = np.random.default_rng(7)
    print("=" * 80)
    print("PHASE 7 -- POSES SOLVED AGAINST REAL BOX GEOMETRY")
    print("=" * 80)
    body_lo = world("body_barrel", corners(BY_NAME["body_barrel"]))[:, 1].min()
    out = {}
    for pose, belly in (("sit", 1.8), ("sleep", 0.8)):
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
    else:
        main()
