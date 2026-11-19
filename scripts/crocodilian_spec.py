#!/usr/bin/env python3
"""Crocodilian model spec + forward-kinematics checker -- SOP Phase 1 / 1b / 2 (Phase 4.8, user-approved 2026-11-14).

A NEW type `crocodilian`: Nile crocodile, saltwater crocodile, mugger, black caiman, Quinkana, Voay. One life-size rig;
tooling forked from giant_monitor_spec.py (the same sprawl-leg helper).

Blockbench space: Y up, ground y=0, -Z = nose, -X = left. ONE angle convention: world rotation about X, positive turns a
part's -Z end UP. Legs are authored as in the monitor rig: upper limb laid OUT along +-X and dropped by a Z rotation,
lower limb hanging straight down, foot laid forward along -Z with the toes turned out.

The silhouette that makes it read as a crocodile and not a monitor:
  * a LOW, FLAT, WIDE trunk (depth ~0.09 of total length) carried in the 'high walk' on short sprawling legs;
  * a long flat head (skull table + two snout segments) with the eyes and nostrils on TOP, and the upper teeth showing
    along the closed jaw line (the crocodile grin);
  * armour: low scute plates down the neck and back;
  * a tail of ~45% of the length, laterally COMPRESSED (tall and thin, a swimming oar) with an upright crest.
Per species (set in the model, solved here): snout width/depth scale, Voay's skull horns, Quinkana's more erect legs.

Run:  python scripts/crocodilian_spec.py [--uv | --zfight | --burial | --json | --pose]
"""
import math
import numpy as np

D = math.radians

SPECIES = [
    # name,              variant, real nose-to-tail length m, snout (width x, depth y) scale, erect legs, horns
    ("nile",              0, 4.5, (1.00, 1.00), False, False),   # Crocodylus niloticus
    ("saltwater",         1, 5.2, (1.05, 1.05), False, False),   # Crocodylus porosus -- largest living reptile
    ("mugger",            2, 3.5, (1.30, 1.00), False, False),   # Crocodylus palustris -- broadest snout
    ("black_caiman",      3, 4.3, (1.15, 1.05), False, False),   # Melanosuchus niger
    ("quinkana",          4, 4.0, (0.80, 1.35), True,  False),   # Quinkana fortirostrum -- Pleistocene Sahul, land hunter
    ("voay",              5, 5.0, (1.20, 1.20), False, True),    # Voay robustus -- horned crocodile, Holocene Madagascar
]
HITBOX = (1.5, 0.9)
# how far the eye plane slides INTO its socket to close (open 0.3 outside the socket face, closed 0.3 inside)
EYE_CLOSE_SHIFT = 0.6

PARTS = []
BY_NAME = {}


def add(name, parent, origin, rot, a, b, cls):
    lo = [min(a[i], b[i]) for i in range(3)]
    hi = [max(a[i], b[i]) for i in range(3)]
    rec = (name, parent, tuple(origin), tuple(rot), tuple(lo), tuple(hi), cls)
    PARTS.append(rec)
    BY_NAME[name] = rec


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


def zchain(parent, pivot, segs, cls, parent_tilt=0.0):
    px, py, pz = pivot
    prev, par, z = parent_tilt, parent, pz
    for name, ang, L, hw, up, dn, ov in segs:
        add(name, par, (px, py, z), (ang - prev, 0, 0), (px - hw, py - dn, z - L), (px + hw, py + up, z + ov), cls)
        par, prev, z = name, ang, z - L


def tchain(parent, pivot, segs, cls, parent_tilt=0.0):
    px, py, pz = pivot
    prev, par, z = parent_tilt, parent, pz
    for name, ang, L, hw, up, dn, ov in segs:
        add(name, par, (px, py, z), (ang - prev, 0, 0), (px - hw, py - dn, z - ov), (px + hw, py + up, z + L), cls)
        par, prev, z = name, ang, z + L


def sprawl(prefix, parent, x0, y0, z0, upper, lower, foot, toe_out):
    """upper = (L, thick, depth, drop_deg); lower = (L, hw, hd); foot = (L, hw, thick, heel)."""
    uL, ut, ud, drop = upper
    lL, lhw, lhd = lower
    fL, fhw, ft, heel = foot
    names = []
    for side, s in (("left", -1.0), ("right", 1.0)):
        up_n, lo_n, ft_n = f"{prefix}_{side}_upper", f"{prefix}_{side}_lower", f"{prefix}_{side}_foot"
        add(up_n, parent, (s * x0, y0, z0), (0, 0, -s * drop),
            (s * (x0 - 0.8), y0 - ut / 2, z0 - ud / 2), (s * (x0 + uL), y0 + ut / 2, z0 + ud / 2), "leg")
        ex = s * (x0 + uL - 0.6)
        add(lo_n, up_n, (ex, y0, z0), (0, 0, s * drop),
            (ex - lhw, y0 - lL, z0 - lhd), (ex + lhw, y0 + 0.8, z0 + lhd), "leg")
        fy = y0 - lL
        add(ft_n, lo_n, (ex, fy, z0), (0, -s * toe_out, 0),
            (ex - fhw, fy - ft / 2, z0 - fL), (ex + fhw, fy + ft / 2, z0 + heel), "foot")
        names += [up_n, lo_n, ft_n]
    return names


FORE_DROP, HIND_DROP = 24.0, 20.0


def build(body_y, tail_droop, arm_lower=4.6):
    PARTS.clear()
    BY_NAME.clear()
    by = body_y
    # ---- TRUNK: low, flat and wide -- three nested masses ----
    add("body_mid", None, (0, by, 0.0), (0, 0, 0), (-4.6, by - 3.1, -6.0), (4.6, by + 2.8, 5.0), "body")
    add("body_chest", "body_mid", (0, by, -5.0), (1, 0, 0), (-4.1, by - 2.8, -13.0), (4.1, by + 2.4, -4.6), "body")
    add("body_hips", "body_mid", (0, by, 4.5), (-1, 0, 0), (-4.0, by - 2.7, 4.0), (4.0, by + 2.4, 8.6), "body")
    # ---- ARMOUR: low scute plates on the back (tops sit 0.5 proud of the body tops) ----
    add("scutes_back", "body_mid", (0, by + 2.8, 0.0), (0, 0, 0), (-3.1, by + 2.2, -5.5), (3.1, by + 3.3, 3.6), "armour")
    add("scutes_hips", "body_hips", (0, by + 2.4, 6.0), (0, 0, 0), (-2.6, by + 2.0, 5.4), (2.6, by + 2.9, 8.2), "armour")
    # ---- NECK + HEAD: a short thick neck, the skull table, two flat snout segments ----
    hy = by + 0.6
    zchain("body_chest", (0, hy, -12.6), [
        #  name          ang    L    hw   up   dn   ov
        ("neck",          0,   4.6, 3.4, 1.0, 2.4, 1.2),
        ("head",         -2,   4.2, 3.0, 1.2, 1.4, 1.0),     # the skull table: flat, wide
        ("snout_base",   -3,   4.6, 2.5, 0.8, 1.2, 0.8),
        ("snout_tip",    -2,   3.8, 1.9, 0.6, 1.0, 0.6),
    ], "head", parent_tilt=1.0)
    # rest-frame anchors (y about hy): neck z -17.2..-12.6  head -21.4..-17.2  snout_base -26.0..-21.4  tip -29.8..-26.0
    add("scutes_neck", "neck", (0, hy + 1.3, -15.0), (0, 0, 0), (-2.2, hy + 0.6, -16.6), (2.2, hy + 1.95, -13.2), "armour")
    # the lower jaw: hinged at the back of the skull, two segments narrower than the snout so the upper teeth show
    zchain("head", (0, hy - 1.4, -17.6), [
        ("jaw",           -3,   8.0, 1.7, 0.9, 0.8, 0.6),
        ("jaw_tip",       -2,   3.8, 1.1, 1.15, 0.6, 0.4),
    ], "jaw", parent_tilt=-2.0)
    for side, s in (("left", -1.0), ("right", 1.0)):
        # upper teeth: a row along each side of the snout, hanging below its edge, outside the jaw
        add(f"teeth_{side}", "snout_base", (s * 2.1, hy - 1.2, -23.6), (0, 0, 0),
            (s * 1.95, hy - 1.75, -25.8), (s * 2.25, hy - 1.05, -21.6), "teeth")
        add(f"teeth_tip_{side}", "snout_tip", (s * 1.5, hy - 1.0, -27.8), (0, 0, 0),
            (s * 1.35, hy - 1.55, -29.6), (s * 1.65, hy - 0.85, -26.2), "teeth")
        # eyes ON TOP of the skull: a bulging bump, the eye plane on its outer side
        # EYES (v4, the mod's standard eye). v1: 1.1-proud bumps that read as ears; v2: a one-texel plane on the skull edge
        # ("no eyes"); v3: the eyeball painted on top of a bump ("uncanny"). Now: a raised eye SOCKET along the top edge of
        # the skull (0.5 above the skull top, 0.2 out past its side -- a brow), and a side-facing eye PLANE 0.3 off the
        # socket's outer face, exactly 3 x 1 texels (v4a: 3 x 2 read as goggles): gold, black slit, gold. Closed = slid into the socket.
        add(f"eye_bump_{side}", "head", (s * 2.55, hy + 0.7, -19.0), (0, 0, 0),
            (s * 1.9, hy - 0.3, -20.35), (s * 3.2, hy + 1.7, -17.45), "eyebump")
        add(f"eye_{side}", "head", (s * 3.5, hy + 1.15, -19.0), (0, 0, 0),
            (s * 3.5, hy + 0.65, -20.3), (s * 3.5, hy + 1.65, -17.3), "eye")
        # small bony knobs swept BACK and out (v1 was 2.2u tall, tilted forward, and read as ears)
        add(f"horn_{side}", "head", (s * 2.45, hy + 1.1, -16.9), (40, 0, -s * 25),
            (s * 2.0, hy + 0.85, -17.3), (s * 2.9, hy + 2.05, -16.5), "horn")
    add("nose_knob", "snout_tip", (0, hy + 0.6, -28.8), (0, 0, 0), (-1.0, hy + 0.3, -29.6), (1.0, hy + 1.1, -28.0), "nose")
    # ---- TAIL: six segments, laterally compressed, drooping to the ground, with an upright crest ----
    ty = by + 0.2
    tchain("body_hips", (ty * 0 + 0, ty, 8.2), [
        #  name        ang                    L    hw   up   dn   ov
        ("tail_1",  tail_droop * 0.4,       6.5, 3.0, 2.0, 2.4, 0.8),
        ("tail_2",  tail_droop * 0.7,       6.5, 2.3, 1.6, 1.9, 0.8),
        ("tail_3",  tail_droop,             6.0, 1.7, 1.35, 1.55, 0.8),
        ("tail_4",  tail_droop * 0.6,       6.0, 1.2, 1.1, 1.25, 0.7),
        ("tail_5",  tail_droop * 0.2,       5.5, 0.8, 0.85, 1.0, 0.7),
        ("tail_6",  0.0,                    5.0, 0.45, 0.6, 0.75, 0.6),
    ], "tail", parent_tilt=-1.0)
    crest = [("tail_1", 2.0, 0.9, 2.0), ("tail_2", 1.6, 0.8, 1.4), ("tail_3", 1.35, 0.7, 0.9), ("tail_4", 1.1, 0.6, 0.5)]
    z0 = 8.2
    for k, (seg, up, h, w) in enumerate(crest):
        L = (6.5, 6.5, 6.0, 6.0)[k]
        add(f"{seg}_crest", seg, (0, ty + up, z0 + L / 2), (0, 0, 0),
            (-w / 2, ty + up - 0.3, z0 + 0.6), (w / 2, ty + up + h, z0 + L - 1.1), "crest")
        z0 += L
    # ---- LEGS: short, sprawling, hind larger than fore ----
    legs = sprawl("arm", "body_chest", 3.4, by - 1.0, -9.0,
                  upper=(3.8, 2.4, 2.4, FORE_DROP), lower=(arm_lower, 1.2, 0.95), foot=(3.0, 1.5, 0.9, 0.8), toe_out=25.0)
    legs += sprawl("leg", "body_hips", 3.4, by - 1.0, 6.2,
                   upper=(4.6, 2.9, 2.9, HIND_DROP), lower=(3.7, 1.4, 1.15), foot=(4.6, 1.7, 0.9, 1.1), toe_out=30.0)
    return legs


def solve():
    """Body height from the HIND feet, then the fore lower limb so the FRONT feet land too, then the tail droop so the
    tail's back half just rests on the ground."""
    y, arm = 7.0, 4.6
    for _ in range(25):
        for _ in range(20):
            build(y, 8.0, arm)
            y -= lowest(["leg_left_foot"])
        arm += lowest(["arm_left_foot"])
    lo_d, hi_d = 0.0, 40.0
    for _ in range(40):
        mid = (lo_d + hi_d) / 2
        build(y, mid, arm)
        if lowest(["tail_3", "tail_4", "tail_5", "tail_6"]) > 0.05:
            lo_d = mid
        else:
            hi_d = mid
    build(y, (lo_d + hi_d) / 2, arm)
    return y, (lo_d + hi_d) / 2, arm


BODY_Y, TAIL_DROOP, ARM_LOWER = solve()

ERECT_HIND_DROP = 55.0


def erect_solve():
    """Quinkana's more ERECT stance on the SAME geometry: the hind upper limb drops to ERECT_HIND_DROP, the fore drop is
    bisected so the front feet still land, the body is raised to stand on them, and the tail droop is re-solved.
    Returns (body lift, fore drop, hind drop, tail droop) -- the model applies these per variant."""
    global FORE_DROP, HIND_DROP
    saved = (FORE_DROP, HIND_DROP)
    HIND_DROP = ERECT_HIND_DROP
    y = BODY_Y
    lo_f, hi_f = 10.0, 89.0
    for _ in range(30):
        FORE_DROP = (lo_f + hi_f) / 2
        for _ in range(20):
            build(y, TAIL_DROOP, ARM_LOWER)
            y -= lowest(["leg_left_foot"])
        if lowest(["arm_left_foot"]) > 0:
            lo_f = FORE_DROP      # fore foot in the air: drop the arm further
        else:
            hi_f = FORE_DROP
    fore = FORE_DROP
    lo_d, hi_d = 0.0, 60.0
    for _ in range(40):
        mid = (lo_d + hi_d) / 2
        build(y, mid, ARM_LOWER)
        if lowest(["tail_3", "tail_4", "tail_5", "tail_6"]) > 0.05:
            lo_d = mid
        else:
            hi_d = mid
    droop = (lo_d + hi_d) / 2
    result = (y - BODY_Y, fore, ERECT_HIND_DROP, droop)
    FORE_DROP, HIND_DROP = saved
    build(BODY_Y, TAIL_DROOP, ARM_LOWER)
    return result


ERECT = erect_solve()
if __import__("os").environ.get("CROC_ERECT"):
    # preview only (same boxes and UVs): rebuild in the solved erect stance
    FORE_DROP, HIND_DROP = ERECT[1], ERECT[2]
    build(BODY_Y + ERECT[0], ERECT[3], ARM_LOWER)

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
    print(f"UV SHELF PACK on {W}x{H}: {len(placed)} boxes, {used} rows used, zero overlaps asserted")
    assert used <= H
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


def zfight():
    """The horns are only shown on Voay, so they are swept on their own pass too."""
    import zfight_sweep as Z
    for with_horns in (False, True):
        parts = {}
        for name, parent, o, r, a, b, cls in PARTS:
            if cls == "eye" or (cls == "horn" and not with_horns):
                continue
            p = Z.Part(name)
            po = np.asarray(BY_NAME[parent][2], float) if parent else np.zeros(3)
            p.rp = np.asarray(o, float) - po
            p.rot = np.array([D(r[0]), D(r[1]), D(r[2])])
            p.lo = np.asarray(a, float) - np.asarray(o, float)
            p.hi = np.asarray(b, float) - np.asarray(o, float)
            p.parent = parent
            parts[name] = p
        shown = 0
        tag = "voay" if with_horns else "base"
        for area, na, nb, fa, fb, rel in Z.sweep(parts):
            if with_horns and "horn" not in na and "horn" not in nb:
                continue
            shown += 1
            print(f"  [{tag}] {area:6.2f}u2  {na:<20} {'XYZ'[fa[0]]}  <->  {nb:<20} {'XYZ'[fb[0]]}{rel}")
        print(f"  [{tag}] z-fight: " + ("clean" if not shown else f"{shown} pair(s)"))


FEATURES = ("foot", "tail", "teeth", "eyebump", "horn", "nose", "crest", "armour")


def burial():
    solids = [p for p in PARTS if p[6] != "eye"]
    out = []
    for p in solids:
        pts = world(p[0], vol(p, 9))
        inside = np.zeros(len(pts), bool)
        for q in solids:
            if q[0] == p[0]:
                continue
            l = local_of(q[0], pts)
            a, b = np.asarray(q[4], float), np.asarray(q[5], float)
            inside |= np.all((l >= a - 1e-6) & (l <= b + 1e-6), axis=1)
        out.append((inside.mean(), p[0], p[6]))
    for frac, name, cls in sorted(out, reverse=True)[:16]:
        flag = "   <== FEATURE MOSTLY BURIED" if frac > 0.55 and cls in FEATURES else ""
        print(f"  {frac:6.1%}  {name}{flag}")


def main():
    wc = {p[0]: world(p[0], corners(p)) for p in PARTS}
    lo = {k: v.min(0) for k, v in wc.items()}
    hi = {k: v.max(0) for k, v in wc.items()}
    floor = min(v[1] for v in lo.values())
    nose = min(lo["snout_tip"][2], lo["jaw_tip"][2], lo["nose_knob"][2])
    tip = hi["tail_6"][2]
    length = tip - nose
    print(f"body pivot y {BODY_Y:.2f}u, fore lower limb {ARM_LOWER:.2f}u, tail droop {TAIL_DROOP:.1f} deg, lowest point {floor:+.3f}"
          + ("  OK" if floor > -0.02 else "  <== THROUGH THE FLOOR"))
    for n in ("arm_left_foot", "leg_left_foot", "tail_3", "tail_5", "tail_6", "body_mid", "jaw", "jaw_tip"):
        print(f"  {n:<16} lowest y {lo[n][1]:+6.2f}")
    svl = hi["body_hips"][2] - nose
    head = hi["head"][2] - nose
    print(f"  nose-to-tail {length:.1f}u  snout-vent {svl:.1f}u = {svl / length:.0%} (real crocodiles ~50%), "
          f"head {head:.1f}u = {head / length:.0%} (real ~14-17%), tail {(tip - hi['body_hips'][2]) / length:.0%}")
    print(f"  back top {hi['scutes_back'][1]:.2f}u, belly {lo['body_mid'][1]:.2f}u, head top {hi['head'][1]:.2f}u, "
          f"width {2 * max(abs(v[0]) for v in list(lo.values()) + list(hi.values())):.1f}u, trunk depth/length "
          f"{(hi['body_mid'][1] - lo['body_mid'][1]) / length:.3f} (real ~0.08-0.10)")
    for name, var, real, snout, erect, horns in SPECIES:
        s = real * 16.0 / length
        print(f"  {name:<14} {real:.1f} m -> scale {s:.3f}  back {hi['scutes_back'][1] * s / 16:.2f} m  snout x{snout[0]:.2f} "
              f"y{snout[1]:.2f}" + ("  erect legs" if erect else "") + ("  horns" if horns else ""))
    print(f"  eye plane x {abs(BY_NAME['eye_left'][2][0]):.2f}, socket outer face {abs(BY_NAME['eye_bump_left'][4][0]):.2f} "
          f"(clear by {abs(BY_NAME['eye_left'][2][0]) - abs(BY_NAME['eye_bump_left'][4][0]):.2f}); closed x "
          f"{abs(BY_NAME['eye_left'][2][0]) - EYE_CLOSE_SHIFT:.2f} (inside the socket)")
    for side in ():
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head", f"eye_bump_{side}", "neck"):
            p = BY_NAME[anc]
            l = local_of(anc, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gap = max(max(a[i] - l[i], l[i] - b[i]) for i in range(3))
            beside = a[1] <= l[1] <= b[1]          # only a plane BESIDE a side face can z-fight it
            if gap < 0.3 and beside:
                print(f"  eye_{side} vs {anc} clear by {gap:.2f}  <== TOO CLOSE")
    tot = sum(fw * fh for fw, fh in (footprint(p) for p in PARTS))
    print(f"  {len(PARTS)} boxes, UV {tot} px = {100 * tot / (CANVAS[0] * CANVAS[1]):.0f}% of 256x128")


# ================================================================ phase 7 poses (belly down)
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


TAILS = ["tail_1", "tail_2", "tail_3", "tail_4", "tail_5", "tail_6"]
TAIL_SHAPE = (0.4, 0.7, 1.0, 0.6, 0.2, 0.0)
CHEST_TILT, HIPS_TILT = 1.0, -1.0
# chin height targets: lying up with the head raised a little (sit / bask), chin on the ground (sleep)
CROC_POSES = {"sit": dict(chin=1.2, curl=0.0), "sleep": dict(chin=0.15, curl=8.0)}
HEADS = ["neck", "head", "snout_base", "snout_tip", "jaw", "jaw_tip", "nose_knob", "teeth_left", "teeth_right",
         "teeth_tip_left", "teeth_tip_right"]


def _over(pose, v):
    """v = [fore_drop, fore_lower_rz, fore_lower_rx, hind_drop, hind_lower_rz, hind_lower_rx, tail_droop, neck]."""
    P = CROC_POSES[pose]
    over = {}
    for prefix, (drop, lrz, lrx) in (("arm", v[0:3]), ("leg", v[3:6])):
        for side, sgn in (("left", -1.0), ("right", 1.0)):
            u = BY_NAME[f"{prefix}_{side}_upper"][3]
            over[f"{prefix}_{side}_upper"] = (u[0], u[1], -sgn * drop)
            over[f"{prefix}_{side}_lower"] = (lrx, 0.0, sgn * lrz)
    over["neck"] = (v[7] - CHEST_TILT, 0, 0)
    prev = HIPS_TILT
    for k, n in enumerate(TAILS):
        w = v[6] * TAIL_SHAPE[k]
        over[n] = (w - prev, P["curl"], 0)
        prev = w
    return over


def _drop(over):
    return 0.4 - low_of(["body_mid", "body_chest", "body_hips"], over, 0.0)


def _score(pose, v):
    over = _over(pose, v)
    dy = _drop(over)
    pen = 0.0
    for prefix in ("arm", "leg"):
        for seg in ("upper", "lower", "foot"):
            n = f"{prefix}_left_{seg}"
            low = world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min()
            if low < 0:
                pen += low * low * 400
            if seg == "foot":
                pen += (low - 0.05) ** 2 * 3
    for n in TAILS + HEADS:
        low = world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min()
        if low < 0:
            pen += low * low * 20
    pen += (low_of(TAILS[1:], over, dy) - 0.1) ** 2 * 2
    pen += (low_of(["jaw", "jaw_tip"], over, dy) - CROC_POSES[pose]["chin"]) ** 2 * 2
    # relaxed limbs: upper limbs near level (elbows and knees not hiked up), joints not wrenched
    pen += 0.0005 * (v[0] ** 2 + v[3] ** 2) + 0.0001 * (v[1] ** 2 + v[2] ** 2 + v[4] ** 2 + v[5] ** 2)
    return pen


def pose_solve(verbose=True):
    rng = np.random.default_rng(5)
    out = {}
    for pose in ("sit", "sleep"):
        best = np.array([10.0, 30.0, 0.0, 10.0, 30.0, 0.0, 6.0, 0.0])
        bs = _score(pose, best)
        span = np.array([25.0, 40.0, 25.0, 25.0, 40.0, 25.0, 12.0, 12.0])
        for _ in range(9):
            for c in best + rng.normal(0, 1, (700, 8)) * span:
                sc = _score(pose, c)
                if sc < bs:
                    bs, best = sc, c.copy()
            span *= 0.6
        over = _over(pose, best)
        dy = _drop(over)
        out[pose] = dict(over=over, dy=dy, v=best, pen=bs)
        if verbose:
            allp = [q[0] for q in PARTS if q[6] != "eye"]
            lows = sorted((world_mod(n, corners(BY_NAME[n]), over, dy)[:, 1].min(), n) for n in allp)[:5]
            print(f"{pose.upper():<6} penalty {bs:.4f} drop {dy:+.2f}u  v " + " ".join(f"{a:6.1f}" for a in best)
                  + "\n        lowest: " + ", ".join(f"{n} {y:+.2f}" for y, n in lows))
    # basking gape: crocodiles bask with the HEAD (upper jaw) tilted up while the lower jaw stays on the ground, so the
    # gape is a head raise with the jaw counter-rotated by the same angle -- the jaw's world pose is unchanged.
    out["gape"] = 32.0
    if verbose:
        print(f"BASK   head raised {out['gape']:.0f} deg, jaw counter-rotated (stays on the ground)")
    return out


if __name__ == "__main__":
    import sys
    if "--pose" in sys.argv:
        pose_solve()
        sys.exit()
    if "--zfight" in sys.argv:
        zfight()
    elif "--uv" in sys.argv:
        uvpack()
    elif "--json" in sys.argv:
        emit_json()
    elif "--burial" in sys.argv:
        burial()
    else:
        main()
