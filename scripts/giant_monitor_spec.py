#!/usr/bin/env python3
"""Giant monitor model spec + forward-kinematics checker -- SOP Phase 1 / 1b / 2 (Phase 4.3, user-approved 2026-10-10).

A NEW type, `giant_monitor` (Megalania + Komodo dragon), with its own life-size rig: the legacy 20-box monitor runs a
64x32 texture and a 1.3 x 0.6 hitbox, both far too small for a 5 m lizard. Tooling forked from kangaroo_spec.py.

Blockbench space: Y up, ground y=0, -Z = nose, -X = left. ONE angle convention: world rotation about X, positive turns a
part's -Z end UP. The legs SPRAWL, which X rotations cannot express, so they are authored directly:
  * upper limb (humerus / femur) laid OUT along +-X from the shoulder/hip and tilted down by a Z rotation
    (rz = -side * drop: a positive drop lowers the outer end on both sides);
  * lower limb hanging straight DOWN from the elbow/knee (its local Z rotation cancels the upper limb's tilt);
  * foot laid FORWARD along -Z, toes turned OUT by a Y rotation.
The body is low-slung (belly ~4u up), the neck long, the head long and flat with a tapered snout; the tail -- more than
half the animal -- tapers through five segments and droops until its tip just touches the ground.

Run:  python scripts/giant_monitor_spec.py [--uv | --zfight | --burial | --json]
"""
import math
import numpy as np

D = math.radians

SPECIES = [
    # name,       variant, real nose-to-tail length m
    ("megalania",  0, 5.0),    # Varanus priscus -- Pleistocene Australia (modern estimates ~4.5-5.5 m)
    ("komodo",     1, 2.6),    # Varanus komodoensis -- living, Indonesia
]
HITBOX = (1.4, 1.0)

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
        ex = s * (x0 + uL - 0.6)                       # elbow / knee, 0.6 inside the upper limb's end
        add(lo_n, up_n, (ex, y0, z0), (0, 0, s * drop),
            (ex - lhw, y0 - lL, z0 - lhd), (ex + lhw, y0 + 0.8, z0 + lhd), "leg")
        fy = y0 - lL
        add(ft_n, lo_n, (ex, fy, z0), (0, -s * toe_out, 0),
            (ex - fhw, fy - ft / 2, z0 - fL), (ex + fhw, fy + ft / 2, z0 + heel), "foot")
        names += [up_n, lo_n, ft_n]
    return names


def build(body_y, tail_droop, arm_lower=5.5):
    PARTS.clear()
    BY_NAME.clear()
    # ---- TORSO: three nested masses, widest at mid-belly (the pot-bellied varanid shape) ----
    # First grey pass read as "a long table": a 5.4u-deep slab on straight posts. A varanid is DEEP-bodied and
    # pot-bellied (depth ~0.11 of total length on a Komodo), carried low on bent sprawling legs.
    add("body_belly", None, (0, body_y, 2.0), (0, 0, 0),
        (-5.2, body_y - 3.8, -2.0), (5.2, body_y + 3.8, 10.5), "body")
    add("body_chest", "body_belly", (0, body_y, -1.0), (2, 0, 0),
        (-4.6, body_y - 3.4, -9.5), (4.6, body_y + 3.2, 0.0), "body")
    add("body_hips", "body_belly", (0, body_y, 10.0), (-2, 0, 0),
        (-4.2, body_y - 3.2, 9.0), (4.2, body_y + 3.3, 14.5), "body")
    # ---- NECK + HEAD: long neck rising slightly, long flat head, tapered snout ----
    zchain("body_chest", (0, body_y + 0.6, -9.0), [
        #  name        ang    L    hw   up   dn   ov
        ("neck",        8,   7.0, 2.9, 2.0, 2.6, 1.6),   # top 0.6 under the chest top (0.2 apart z-fought)
        ("head",        2,   6.5, 2.7, 1.6, 1.6, 1.2),   # flat head, top 0.4 under the neck top
        ("snout",      -2,   5.0, 1.9, 1.3, 1.2, 1.0),
    ], "head", parent_tilt=2.0)
    # rest-frame anchors (about y = body_y + 0.6): neck z -16..-9  head -22.5..-16  snout -27.5..-22.5
    hy = body_y + 0.6
    add("jaw", "head", (0, hy - 1.2, -17.0), (0, 0, 0), (-2.3, hy - 2.3, -26.6), (2.3, hy - 0.8, -16.6), "head")
    add("tongue", "snout", (0, hy - 0.6, -27.2), (0, 0, 0), (-0.4, hy - 0.8, -30.2), (0.4, hy - 0.5, -27.0), "tongue")
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"eye_{side}", "head", (s * 3.05, hy + 0.6, -19.0), (0, 0, 0),
            (s * 3.05, hy - 0.4, -20.0), (s * 3.05, hy + 1.6, -18.0), "eye")
    # ---- TAIL: five tapering segments, drooping until the tip just touches the ground ----
    tchain("body_hips", (0, body_y + 0.4, 14.0), [
        #  name        ang                    L    hw   up   dn   ov
        ("tail_1",  tail_droop * 0.55,      9.0, 3.5, 2.6, 2.8, 1.2),
        ("tail_2",  tail_droop * 0.85,      8.5, 2.7, 2.0, 2.2, 1.0),
        ("tail_3",  tail_droop,             8.5, 2.0, 1.5, 1.6, 1.0),
        ("tail_4",  tail_droop * 0.7,       8.0, 1.3, 1.0, 1.1, 0.8),
        ("tail_5",  tail_droop * 0.3,       7.5, 0.7, 0.6, 0.6, 0.8),
    ], "tail", parent_tilt=-2.0)
    # ---- LEGS: sprawling ----
    legs = sprawl("arm", "body_chest", 4.0, body_y - 0.8, -5.5,
                  upper=(4.6, 2.6, 2.6, 14.0), lower=(arm_lower, 1.3, 1.1), foot=(3.8, 1.6, 1.0, 1.0), toe_out=22.0)
    legs += sprawl("leg", "body_hips", 3.6, body_y - 0.8, 11.5,
                   upper=(5.0, 3.0, 3.0, 16.0), lower=(4.0, 1.5, 1.25), foot=(4.4, 1.7, 1.0, 1.2), toe_out=28.0)
    return legs


def solve():
    # body height from the HIND feet, then the fore lower-limb length so the FRONT feet land too (they floated 0.48u
    # when both limbs shared one length), then the tail droop so its tip just touches the ground
    y, arm = 8.0, 5.5
    for _ in range(25):
        for _ in range(20):
            build(y, 8.0, arm)
            y -= lowest(["leg_left_foot"])
        arm += lowest(["arm_left_foot"])
    lo_d, hi_d = 0.0, 40.0
    for _ in range(40):
        mid = (lo_d + hi_d) / 2
        build(y, mid, arm)
        if lowest(["tail_3", "tail_4", "tail_5"]) > 0.05:
            lo_d = mid
        else:
            hi_d = mid
    build(y, (lo_d + hi_d) / 2, arm)
    return y, (lo_d + hi_d) / 2, arm


BODY_Y, TAIL_DROOP, ARM_LOWER = solve()

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
    shown = 0
    for area, na, nb, fa, fb, rel in Z.sweep(parts):
        shown += 1
        print(f"  {area:6.2f}u2  {na:<20} {'XYZ'[fa[0]]}  <->  {nb:<20} {'XYZ'[fb[0]]}{rel}")
    print("  z-fight: clean" if not shown else f"  z-fight: {shown} pair(s)")


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
    for frac, name, cls in sorted(out, reverse=True)[:12]:
        flag = "   <== FEATURE MOSTLY BURIED" if frac > 0.55 and cls in ("foot", "tongue", "tail") else ""
        print(f"  {frac:6.1%}  {name}{flag}")


def main():
    wc = {p[0]: world(p[0], corners(p)) for p in PARTS}
    lo = {k: v.min(0) for k, v in wc.items()}
    hi = {k: v.max(0) for k, v in wc.items()}
    floor = min(v[1] for v in lo.values())
    nose = min(lo["snout"][2], lo["jaw"][2])
    tip = hi["tail_5"][2]
    length = tip - nose
    print(f"body pivot y {BODY_Y:.2f}u, fore lower limb {ARM_LOWER:.2f}u, tail droop {TAIL_DROOP:.1f} deg, lowest point {floor:+.3f}"
          + ("  OK" if floor > -0.02 else "  <== THROUGH THE FLOOR"))
    for n in ("arm_left_foot", "leg_left_foot", "tail_3", "tail_4", "tail_5", "body_belly", "jaw"):
        print(f"  {n:<16} lowest y {lo[n][1]:+6.2f}")
    print(f"  nose-to-tail {length:.1f}u  (snout-vent {hi['body_hips'][2] - nose:.1f}u = "
          f"{(hi['body_hips'][2] - nose) / length:.0%}, real varanids ~40-45%)")
    print(f"  back height {hi['body_belly'][1]:.2f}u, head top {hi['head'][1]:.2f}u, belly {lo['body_belly'][1]:.2f}u, "
          f"width {2 * max(abs(v[0]) for v in list(lo.values()) + list(hi.values())):.1f}u")
    for name, var, real in SPECIES:
        s = real * 16.0 / length
        print(f"  {name:<10} {real:.1f} m -> scale {s:.3f}  back {hi['body_belly'][1] * s / 16:.2f} m")
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head", "neck", "jaw"):
            p = BY_NAME[anc]
            l = local_of(anc, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gap = max(max(a[i] - l[i], l[i] - b[i]) for i in range(3))
            if gap < 0.3:
                print(f"  eye_{side} vs {anc} clear by {gap:.2f}  <== TOO CLOSE")
    tot = sum(fw * fh for fw, fh in (footprint(p) for p in PARTS))
    print(f"  {len(PARTS)} boxes, UV {tot} px = {100 * tot / (CANVAS[0] * CANVAS[1]):.0f}% of 256x128")


# ================================================================ phase 7 poses (both belly-down)
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


TAILS = ["tail_1", "tail_2", "tail_3", "tail_4", "tail_5"]
GM_POSES = {
    "sit": dict(neck=10.0, head=2.0, curl=0.0),      # lying up, head raised, tail straight
    "sleep": dict(neck=-12.0, head=-5.0, curl=9.0),  # chin down on the ground, tail curled round to one side
}


def _gm_over(pose, v):
    """v = [fore_drop, fore_lower_rz, fore_lower_rx, hind_drop, hind_lower_rz, hind_lower_rx, tail_droop]."""
    P = GM_POSES[pose]
    over = {}
    for prefix, (drop, lrz, lrx) in (("arm", v[0:3]), ("leg", v[3:6])):
        for side, sgn in (("left", -1.0), ("right", 1.0)):
            u = BY_NAME[f"{prefix}_{side}_upper"][3]
            over[f"{prefix}_{side}_upper"] = (u[0], u[1], -sgn * drop)
            over[f"{prefix}_{side}_lower"] = (lrx, 0.0, sgn * lrz)
    over["neck"] = (P["neck"] - 2.0, 0, 0)
    over["head"] = (P["head"] - P["neck"], 0, 0)
    prev = -2.0
    for k, n in enumerate(TAILS):
        w = v[6] * (0.55, 0.85, 1.0, 0.7, 0.3)[k]
        over[n] = (w - prev, P["curl"], 0)
        prev = w
    return over


def _gm_drop(over):
    return 0.4 - low_of(["body_belly", "body_chest", "body_hips"], over, 0.0)


def _gm_score(pose, v):
    over = _gm_over(pose, v)
    dy = _gm_drop(over)
    pen = 0.0
    for prefix in ("arm", "leg"):
        for seg in ("upper", "lower", "foot"):
            n = f"{prefix}_left_{seg}"
            low = world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min()
            if low < 0:
                pen += low * low * 20
            if seg == "foot":
                pen += (low - 0.05) ** 2 * 3               # the foot rests ON the ground
    for n in TAILS + ["jaw", "head", "snout", "neck"]:
        low = world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min()
        if low < 0:
            pen += low * low * 20
    pen += (low_of(["tail_2", "tail_3", "tail_4", "tail_5"], over, dy) - 0.1) ** 2 * 2
    if pose == "sleep":
        pen += (low_of(["jaw"], over, dy) - 0.2) ** 2 * 2   # chin resting on the ground
    return pen


def gm_pose_solve(verbose=True):
    rng = np.random.default_rng(5)
    out = {}
    for pose in ("sit", "sleep"):
        best = np.array([14.0, 40.0, 0.0, 16.0, 40.0, 0.0, 4.0])
        bs = _gm_score(pose, best)
        span = np.array([25.0, 40.0, 25.0, 25.0, 40.0, 25.0, 12.0])
        for _ in range(9):
            for c in best + rng.normal(0, 1, (700, 7)) * span:
                sc = _gm_score(pose, c)
                if sc < bs:
                    bs, best = sc, c.copy()
            span *= 0.6
        over = _gm_over(pose, best)
        dy = _gm_drop(over)
        out[pose] = dict(over=over, dy=dy, v=best, pen=bs)
        if verbose:
            allp = [q[0] for q in PARTS if q[6] != "eye"]
            lows = sorted((world_mod(n, corners(BY_NAME[n]), over, dy)[:, 1].min(), n) for n in allp)[:5]
            print(f"{pose.upper():<6} penalty {bs:.4f} drop {dy:+.2f}u  v " + " ".join(f"{a:6.1f}" for a in best)
                  + "\n        lowest: " + ", ".join(f"{n} {y:+.2f}" for y, n in lows))
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
        gm_pose_solve()
    else:
        main()
