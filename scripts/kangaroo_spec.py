#!/usr/bin/env python3
"""Kangaroo model spec + forward-kinematics checker -- SOP Phase 1 / 1b / 2 (Phase 4.2, user-approved 2026-10-09).

The mod's FIRST HOPPING BODY. Tooling forked from diprotodont_spec.py; the body is authored from scratch.

Authoring space is BLOCKBENCH project space: Y up, ground y=0, -Z = front (nose), -X = left. Group origins and
cube from/to are ABSOLUTE project coordinates, exactly as Blockbench stores them.

ONE ANGLE CONVENTION everywhere: every chain angle is a WORLD rotation about X in Blockbench's sense, and a
POSITIVE angle turns a part's -Z end UP / its +Z end DOWN / the bottom of a hanging part FORWARD.
  * zchain  -- segments laid FORWARD along -Z (torso, neck, head)
  * tchain  -- segments laid BACKWARD along +Z (tail): a positive angle droops the tail
  * hang    -- segments hanging DOWN along -Y (legs, arms): a positive angle swings the lower end forward
Each segment's LOCAL rotation is (its world angle - its parent's world angle), so a child authored in the rest
frame lands at its intended world angle whatever the parent does.

KANGAROO SILHOUETTE (the REST pose -- standing, alert, between hops):
  * the torso tipped UP: haunches 35 deg, chest 50 deg, neck 62 deg -- the head carried well above the hips;
  * HUGE hind legs: a deep haunch, a long shin angled back to the heel, and a very long foot lying FLAT on the
    ground with the big fourth toe beyond it (the animal stands plantigrade at rest, on its toes only in a hop);
  * a thick tapering TAIL running back and down to rest ON THE GROUND (the third leg of the tripod);
  * small forearms held in front of the chest; a deer-like head with tall upright ears.
FIVE species off one mesh, sized by STANDING HEIGHT (top of the head at rest, 1 block = 1 m):
  * the sthenurines (Procoptodon, Sthenurus) get a SHORT FACE and LONG CLAWED ARMS (variant-gated setScale on
    the muzzle and arm chains, claws shown only for them);
  * the forward-facing POUCH JOEY is gated at runtime (mother with a baby nearby), never per species.

Run:  python scripts/kangaroo_spec.py            full report
      python scripts/kangaroo_spec.py --uv       UV shelf pack only
      python scripts/kangaroo_spec.py --zfight   z-fight sweep on the spec
      python scripts/kangaroo_spec.py --burial   volume-burial check
      python scripts/kangaroo_spec.py --json     compact JSON for the BB builder
"""
import math
import numpy as np

D = math.radians

SPECIES = [
    # name,          variant, real standing height m, sthenurine?
    ("red",           0, 1.50, False),   # Osphranter rufus
    ("eastern_grey",  1, 1.40, False),   # Macropus giganteus
    ("procoptodon",   2, 2.00, True),    # Procoptodon goliah -- giant short-faced kangaroo
    ("sthenurus",     3, 1.60, True),    # Sthenurus stirlingi
    ("protemnodon",   4, 1.40, False),   # Protemnodon anak -- heavy giant wallaby
]
HITBOX = (0.9, 1.8)

PARTS = []                    # (name, parent, origin, rot_deg, from, to, cls)
BY_NAME = {}


def add(name, parent, origin, rot, a, b, cls):
    lo = [min(a[i], b[i]) for i in range(3)]
    hi = [max(a[i], b[i]) for i in range(3)]
    rec = (name, parent, tuple(origin), tuple(rot), tuple(lo), tuple(hi), cls)
    PARTS.append(rec)
    BY_NAME[name] = rec


def drop_parts(names):
    PARTS[:] = [q for q in PARTS if q[0] not in names]
    for n in names:
        BY_NAME.pop(n, None)


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
    return np.array([[x, y, z] for x in (a[0], b[0]) for y in (a[1], b[1]) for z in (a[2], b[2])])


def vol(p, n=7):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    ax = [np.linspace(a[i], b[i], n) if b[i] - a[i] > 1e-6 else np.array([a[i]]) for i in range(3)]
    return np.stack(np.meshgrid(*ax, indexing="ij"), -1).reshape(-1, 3)


def lowest(names):
    return min(world(n, corners(BY_NAME[n]))[:, 1].min() for n in names)


# ================================================================ chain helpers
def zchain(parent, pivot, segs, cls, parent_tilt=0.0):
    """Forward along -Z. segs: (name, ang, L, hw, up, dn, ov). Returns the rest-frame z of each segment's front."""
    px, py, pz = pivot
    prev, par, z = parent_tilt, parent, pz
    fronts = {}
    for name, ang, L, hw, up, dn, ov in segs:
        add(name, par, (px, py, z), (ang - prev, 0, 0), (px - hw, py - dn, z - L), (px + hw, py + up, z + ov), cls)
        fronts[name] = (z, z - L)
        par, prev, z = name, ang, z - L
    return fronts


def tchain(parent, pivot, segs, cls, parent_tilt=0.0):
    """Backward along +Z. A positive WORLD angle droops the rear end. segs: (name, ang, L, hw, up, dn, ov)."""
    px, py, pz = pivot
    prev, par, z = parent_tilt, parent, pz
    for name, ang, L, hw, up, dn, ov in segs:
        add(name, par, (px, py, z), (ang - prev, 0, 0), (px - hw, py - dn, z - ov), (px + hw, py + up, z + L), cls)
        par, prev, z = name, ang, z + L


def hang(prefix, parent, pivot, segs, parent_tilt, cls):
    """Both sides of a limb hanging along -Y from pivot (x is the RIGHT-side |x| of the joint centre).
    segs: (name, ang, L, outer, inner, fr, bk, ov) -- outer/inner are |x| of the box faces."""
    _, py, pz = pivot
    names = []
    for side, s in (("left", -1.0), ("right", 1.0)):
        par, prev, y = parent, parent_tilt, py
        for name, ang, L, outer, inner, fr, bk, ov in segs:
            n = f"{prefix}_{side}_{name}"
            add(n, par, (s * (outer + inner) / 2, y, pz), (ang - prev, 0, 0),
                (s * outer, y - L, pz - fr), (s * inner, y + ov, pz + bk), cls)
            names.append(n)
            par, prev, y = n, ang, y - L
    return names


# ================================================================ TORSO (solved height)
# The whole animal is built off one hip pivot whose HEIGHT is solved so that the hind soles land exactly on y=0;
# the tail is then solved separately so its tip RESTS on the ground (the tripod stance).
def build(hip_y, tail_droop):
    PARTS.clear()
    BY_NAME.clear()
    # Torso chain from the hip, tipped UP. The haunch box carries a deep rear overlap (ov 3.5): that is the rump.
    zchain(None, (0, hip_y, 0), [
        #  name          ang    L    hw   up   dn   ov
        ("body_hips",     35,  7.0, 4.2, 4.0, 4.4, 3.5),
        ("body_chest",    50,  6.5, 3.4, 3.4, 3.6, 2.0),
        ("neck",          62,  4.0, 1.9, 1.8, 1.9, 1.6),
        ("head",          -5,  4.4, 2.3, 2.0, 2.0, 1.4),
        ("muzzle",       -18,  3.4, 1.5, 1.4, 1.4, 1.0),
    ], "body")
    # rest-frame anchors (all about y = hip_y):  hips z -7..+3.5  chest -13.5..-5  neck -17.5..-11.9
    #                                            head -21.9..-16.1  muzzle -25.3..-20.9
    TILT = {"body_hips": 35.0, "body_chest": 50.0, "neck": 62.0, "head": -5.0, "muzzle": -18.0}

    add("nose", "muzzle", (0, hip_y + 0.2, -25.4), (0, 0, 0),
        (-1.1, hip_y - 0.6, -26.2), (1.1, hip_y + 1.1, -24.9), "nose")
    # tall upright ears on the back of the head, rolled slightly out
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"ear_{side}", "head", (s * 1.4, hip_y + 1.8, -17.6), (-8, s * -14, s * -14),   # roll -s: +s leaned the tips IN to an "A" (grey render)
            (s * 0.6, hip_y + 1.4, -18.0), (s * 2.2, hip_y + 6.2, -17.2), "ear")
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"eye_{side}", "head", (s * 2.6, hip_y + 0.4, -19.4), (0, 0, 0),   # 2x2 (was 1x1: no room for a lid row)
            (s * 2.6, hip_y - 0.6, -20.4), (s * 2.6, hip_y + 1.4, -18.4), "eye")

    # ---- forearms, hanging from the chest front, held in front of the body ----
    # rooted at the chest FRONT and below it: the first pass hung the upper arms 72% inside the chest
    hang("arm", "body_chest", (2.0, hip_y - 3.2, -12.8), [
        #  name      ang    L    outer inner  fr   bk   ov
        ("upper",     20,  3.0, 3.6, 1.0, 1.2, 1.1, 0.8),   # outer 3.6: at 3.4 it was coplanar with the chest side
        ("fore",      55,  2.8, 3.05, 1.35, 1.0, 0.9, 0.7),
        ("hand",      75,  1.6, 3.3, 1.1, 0.8, 0.7, 0.5),
    ], TILT["body_chest"], "arm")
    # User 2026-10-09: the first arms "look like noodles" -- 1.2-1.6u thick, 3.2-3.4u long and hanging straight
    # down. Now muscular (2.6 / 1.7 / 2.2 across, 2.3 / 1.9 / 1.5 deep), shorter, the forearm bent well forward and
    # a broad paw held in front of the chest, the way a resting kangaroo carries them. Lateral faces still step
    # >= 0.25u between neighbours (3.6 / 3.05 / 3.3 out, 1.0 / 1.35 / 1.1 in).
    # sthenurine claws: two long curved claws past each hand (variant-gated)
    for side, s in (("left", -1.0), ("right", 1.0)):
        hand = BY_NAME[f"arm_{side}_hand"]
        hy = hand[2][1] - 1.6
        add(f"claw_{side}", f"arm_{side}_hand", (s * 2.2, hy, -12.8), (20, 0, 0),
            (s * 2.7, hy - 1.6, -13.3), (s * 1.7, hy + 0.3, -12.6), "claw")

    # ---- the pouch joey: a head peeking from the forward-facing pouch on the lower belly ----
    # The first pass sat INSIDE the haunch box (100% buried). The belly face of the haunch is at y = hip - 4.4 in
    # the rest frame, so the head now hangs 1.4u below it with 0.5u seated in the pouch, snout forward.
    add("joey_head", "body_hips", (0, hip_y - 4.4, -5.2), (0, 0, 0),
        (-0.95, hip_y - 5.8, -6.4), (0.95, hip_y - 3.9, -4.0), "joey")   # hw 0.95: at 1.2 its flanks sat 0.1u off the inner thighs
    add("joey_snout", "joey_head", (0, hip_y - 5.2, -6.4), (0, 0, 0),
        (-0.6, hip_y - 5.7, -7.4), (0.6, hip_y - 4.6, -6.2), "joey")

    # ---- hind legs: deep haunch, long shin angled back to the heel, very long flat foot, big fourth toe ----
    legs = hang("leg", "body_hips", (2.9, hip_y - 0.6, -0.6), [
        #  name      ang    L    outer inner  fr   bk   ov
        ("thigh",     55,  7.0, 4.6, 1.3, 3.0, 3.4, 2.2),
        ("shin",     -42,  8.4, 3.6, 2.1, 1.1, 0.9, 1.2),   # bk 0.9: at 1.2 the heel corner poked 0.14u through the floor
        ("foot",      90,  6.0, 3.4, 1.9, 0.9, 0.6, 0.6),
        ("toe",       92,  3.0, 3.2, 2.1, 0.5, 0.9, 0.8),   # toe pad drops 0.3 below the foot sole: equal soles z-fought
    ], TILT["body_hips"], "leg")

    # ---- tail: thick at the base, tapering, running back and DOWN to rest on the ground ----
    tchain("body_hips", (0, hip_y - 1.2, 3.0), [
        #  name      ang           L    hw   up   dn   ov
        ("tail_1",  tail_droop,    6.0, 1.8, 1.9, 1.8, 1.6),
        ("tail_2",  tail_droop * 0.45, 6.0, 1.3, 1.2, 1.3, 1.0),
        ("tail_3",  tail_droop * 0.05, 5.5, 0.8, 0.8, 0.8, 0.8),
    ], "tail", parent_tilt=TILT["body_hips"])
    return legs


def solve():
    # hip height: the lowest of shin / foot / toe touches y=0 exactly
    hip = 11.0
    for _ in range(30):
        build(hip, 30.0)
        hip -= lowest(["leg_left_shin", "leg_left_foot", "leg_left_toe"])
    # tail droop: the tip must rest ON the ground (0.0 .. 0.1)
    lo_d, hi_d = 0.0, 80.0
    for _ in range(40):
        mid = (lo_d + hi_d) / 2
        build(hip, mid)
        if lowest(["tail_2", "tail_3"]) > 0.05:   # tail_2 sags below the tip if only tail_3 is checked
            lo_d = mid
        else:
            hi_d = mid
    build(hip, (lo_d + hi_d) / 2)
    return hip, (lo_d + hi_d) / 2


HIP_Y, TAIL_DROOP = solve()

# ================================================================ UV pack
CANVAS = (256, 128)


def footprint(p):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    w, h, d = (abs(b[i] - a[i]) for i in range(3))
    if p[6] == "eye":
        w = 0.0
    return int(math.ceil(2 * (w + d))), int(math.ceil(h + d))


def uvpack():
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
        out.append({"n": name, "p": parent, "o": [round(x, 4) for x in o], "r": [round(x, 4) for x in r],
                    "f": [round(x, 4) for x in a], "t": [round(x, 4) for x in b], "uv": [u, v], "c": cls})
    print(json.dumps({"canvas": list(CANVAS), "parts": out}, separators=(",", ":")))


# ================================================================ z-fight sweep / burial
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
    print("=" * 80)
    print("Z-FIGHT SWEEP ON THE SPEC   (>=0.5u2 of genuinely coincident face)")
    print("=" * 80)
    shown = 0
    for area, na, nb, fa, fb, rel in Z.sweep(parts):
        shown += 1
        print(f"  {area:6.2f}u2  {na:<22} {'XYZ'[fa[0]]}  <->  {nb:<22} {'XYZ'[fb[0]]}{rel}")
    if not shown:
        print("  clean")


def burial():
    print("=" * 80)
    print("VOLUME BURIAL   (fraction of each part's volume inside some OTHER box)")
    print("=" * 80)
    solids = [p for p in PARTS if p[6] != "eye"]
    out = []
    for p in solids:
        pts = world(p[0], vol(p, 9))
        inside = np.zeros(len(pts), bool)
        for q in solids:
            if q[0] == p[0] or (q[6] == "joey" and p[6] != "joey"):
                continue
            l = local_of(q[0], pts)
            a, b = np.asarray(q[4], float), np.asarray(q[5], float)
            inside |= np.all((l >= a - 1e-6) & (l <= b + 1e-6), axis=1)
        out.append((inside.mean(), p[0], p[6]))
    for frac, name, cls in sorted(out, reverse=True):
        flag = ""
        if frac > 0.55:
            flag = ("   <== FEATURE MOSTLY BURIED" if cls in ("ear", "nose", "tail", "claw", "joey")
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
        print(f"{n:<22}{lo[n][0]:7.2f}..{hi[n][0]:<7.2f}{lo[n][1]:7.2f}..{hi[n][1]:<7.2f}{lo[n][2]:7.2f}..{hi[n][2]:<7.2f}")

    print("\n" + "=" * 80)
    print("GROUND CONTACT + PROPORTION")
    print("=" * 80)
    print(f"  solved hip height {HIP_Y:.2f}u   tail droop {TAIL_DROOP:.1f} deg")
    for n in ("leg_left_foot", "leg_left_toe", "tail_3", "tail_2"):
        print(f"  {n:<20} lowest y {lo[n][1]:+6.3f}")
    floor = min(v[1] for v in lo.values())
    print(f"  lowest point of the whole model: {floor:+.3f}" + ("   OK" if floor > -0.02 else "   <== THROUGH THE FLOOR"))
    head_top = hi["head"][1]
    ear_top = max(hi["ear_left"][1], hi["ear_right"][1])
    print(f"  head top {head_top:.2f}u   ear tips {ear_top:.2f}u   hip {HIP_Y:.2f}u")
    print(f"  foot+toe length {hi['leg_left_foot'][2] - lo['leg_left_toe'][2]:.2f}u   tail reach "
          f"{hi['tail_3'][2]:.2f}u behind the hip, nose {lo['nose'][2]:.2f}u ahead")
    # tripod: the tail tip and the toes bracket the centre of mass
    print(f"  tripod: toes z {lo['leg_left_toe'][2]:.1f} .. tail tip z {hi['tail_3'][2]:.1f}")

    print("\n" + "=" * 80)
    print("EYE CLEARANCE")
    print("=" * 80)
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head", "neck", "muzzle", f"ear_{side}"):
            p = BY_NAME[anc]
            l = local_of(anc, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gaps = [max(a[i] - l[i], l[i] - b[i]) for i in range(3)]
            gap = max(gaps)
            print(f"  eye_{side:<6} vs {anc:<10} clear by {gap:6.2f} on {'xyz'[int(np.argmax(gaps))]}"
                  + ("" if gap > 0.3 else "   <== TOO CLOSE"))

    print("\n" + "=" * 80)
    print(f"SIZE CALIBRATION   mesh standing height (head top) = {head_top:.2f}u,  scale = m*16/height")
    print("=" * 80)
    length = hi["tail_3"][2] - lo["nose"][2]
    print(f"{'species':<14}{'m':>6}{'scale':>8}{'head':>7}{'ears':>7}{'nose-tail':>11}")
    for name, var, real, sth in SPECIES:
        s = real * 16.0 / head_top
        print(f"{name:<14}{real:6.2f}{s:8.3f}{head_top * s / 16:7.2f}{ear_top * s / 16:7.2f}{length * s / 16:11.2f}"
              f"  {'sthenurine' if sth else ''}")

    pts = np.vstack([world(p[0], vol(p, 13)) for p in PARTS if p[6] not in ("eye", "joey", "claw")])
    print("\n" + "=" * 80)
    print("SIDE SILHOUETTE  (joey + claws hidden)      <- nose      tail ->")
    print("=" * 80)
    zmin, zmax, ymax = pts[:, 2].min(), pts[:, 2].max(), pts[:, 1].max()
    W, H = 72, 28
    g = [[" "] * W for _ in range(H)]
    for _, y, z in pts:
        c = int((z - zmin) / (zmax - zmin) * (W - 1))
        r = int((ymax - y) / ymax * (H - 1))
        if 0 <= r < H:
            g[r][c] = "#"
    for r, row in enumerate(g):
        print(f"{ymax - r * ymax / (H - 1):5.1f} |" + "".join(row))
    print(f"      z {zmin:.1f} .. {zmax:.1f}")
    tot = sum(fw * fh for fw, fh in (footprint(p) for p in PARTS))
    print(f"\n{len(PARTS)} boxes, UV {tot} px = {100 * tot / (CANVAS[0] * CANVAS[1]):.0f}% of {CANVAS[0]}x{CANVAS[1]}")


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


def low_of(names, over, dy):
    return min(world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min() for n in names)


LEG = ["thigh", "shin", "foot", "toe"]
# Rest-pose WORLD angles, read back off the spec (local rot accumulates down each chain).
REST_WORLD = {"body_hips": 35.0, "body_chest": 50.0, "neck": 62.0, "head": -5.0, "muzzle": -18.0,
              "thigh": 55.0, "shin": -42.0, "foot": 90.0, "toe": 92.0}
POSES = {
    # sit: sunk onto the haunches, torso a little MORE upright, head up
    "sit": dict(hips=45.0, belly=None, drop=-3.0, neck=70.0, head=-5.0),
    # sleep: belly down (the way wallabies rest), torso near level, legs folded under, head low and resting
    "sleep": dict(hips=8.0, belly=0.35, drop=None, neck=22.0, head=-12.0),
    # crawl: the slow pentapedal gait's planted phase -- torso near level, paws and tail on the ground
    "crawl": dict(hips=6.0, belly=3.2, drop=None, neck=38.0, head=-8.0),
}


ARM = ["upper", "fore", "hand"]
ARM_REST = (20.0, 55.0, 75.0)


def _over(pose, legs, tail, arms=ARM_REST):
    P = POSES[pose]
    over = {"body_hips": (P["hips"], 0, 0)}
    # chest / muzzle keep their rest LOCAL bends; the neck and head get the pose's world angles
    chest_w = P["hips"] + (REST_WORLD["body_chest"] - REST_WORLD["body_hips"])
    over["neck"] = (P["neck"] - chest_w, 0, 0)
    over["head"] = (P["head"] - P["neck"], 0, 0)
    for side in ("left", "right"):
        prev = P["hips"]
        for seg, a in zip(LEG, legs):
            over[f"leg_{side}_{seg}"] = (a - prev, 0, 0)
            prev = a
    for side in ("left", "right"):
        prev = chest_w
        for seg, a in zip(ARM, arms):
            over[f"arm_{side}_{seg}"] = (a - prev, 0, 0)
            prev = a
    t1, t2, t3 = tail
    over["tail_1"] = (t1 - P["hips"], 0, 0)
    over["tail_2"] = (t2 - t1, 0, 0)
    over["tail_3"] = (t3 - t2, 0, 0)
    return over


def _drop(pose, over):
    P = POSES[pose]
    if P["drop"] is not None:
        return P["drop"]
    return P["belly"] - low_of(["body_hips", "body_chest"], over, 0.0)


def _score(pose, legs, tail, arms=ARM_REST):
    over = _over(pose, legs, tail, arms)
    dy = _drop(pose, over)
    pen = 0.0
    names = [f"leg_left_{s}" for s in LEG]
    for k, n in enumerate(names):
        low = world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min()
        if low < 0:
            pen += low * low * 20
        if k >= 2:                                   # foot + toe lie ON the floor
            pen += (low - 0.05) ** 2 * 4
    for n in ("tail_1", "tail_2", "tail_3"):
        low = world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min()
        if low < 0:
            pen += low * low * 20
    pen += (low_of(["tail_3"], over, dy) - 0.1) ** 2 * 2     # the tail rests on the ground
    for k, seg in enumerate(ARM):                       # arms: never through the floor; asleep, the paws rest ON it
        n = f"arm_left_{seg}"
        low = world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min()
        if low < 0:
            pen += low * low * 20
        if pose in ("sleep", "crawl") and k == 2:
            pen += (low - 0.1) ** 2 * 2
    for n in ("claw_left",):
        low = world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min()
        if low < 0:
            pen += low * low * 5
    for n in ("head", "muzzle", "nose", "neck"):
        low = world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min()
        if low < 0.1:
            pen += (0.1 - low) ** 2 * 20
    pen += 0.0005 * sum((a - b) ** 2 for a, b in zip(legs, (55, -42, 90, 92)))   # stay near the rest pose
    return pen


def pose_solve(verbose=True):
    rng = np.random.default_rng(11)
    out = {}
    for pose, guess in (("sit", (66.5, -56.2, 91.3, 88.9, 32.9, 5.8, 6.2, 20.0, 55.0, 75.0)),
                        ("sleep", (94.8, -58.6, 93.1, 113.7, 17.1, -0.7, 10.7, 80.0, 95.0, 95.0)),
                        ("crawl", (80.0, -60.0, 90.0, 92.0, 12.0, 4.0, 1.0, -10.0, 10.0, 80.0))):
        best = np.array(guess)
        bs = _score(pose, best[:4], best[4:7], best[7:])
        span = np.array([30.0, 40.0, 20.0, 20.0, 20.0, 15.0, 12.0, 40.0, 40.0, 40.0])
        for _ in range(10):
            cand = best + rng.normal(0, 1, (900, 10)) * span
            for c in cand:
                sc = _score(pose, c[:4], c[4:7], c[7:])
                if sc < bs:
                    bs, best = sc, c.copy()
            span *= 0.6
        over = _over(pose, best[:4], best[4:7], best[7:])
        dy = _drop(pose, over)
        out[pose] = dict(over=over, dy=dy, legs=best[:4], tail=best[4:7], arms=best[7:], pen=bs)
        if verbose:
            print(f"{pose.upper():<6} penalty {bs:.4f}  drop {dy:+.2f}u  legs " + " ".join(f"{a:7.1f}" for a in best[:4])
                  + "  tail " + " ".join(f"{a:6.1f}" for a in best[4:7]) + "  arms " + " ".join(f"{a:6.1f}" for a in best[7:]))
            allp = [p[0] for p in PARTS if p[6] not in ("eye",)]
            lows = sorted((world_mod(n, corners(BY_NAME[n]), over, dy)[:, 1].min(), n) for n in allp)[:6]
            print("        lowest: " + ", ".join(f"{n} {y:+.2f}" for y, n in lows))
    return out


if __name__ == "__main__":
    import sys
    if "--zfight" in sys.argv:
        zfight()
    elif "--uv" in sys.argv:
        uvpack()
    elif "--json" in sys.argv:
        emit_json()
    elif "--burial" in sys.argv:
        burial()
    elif "--pose" in sys.argv:
        pose_solve()
    else:
        main()
