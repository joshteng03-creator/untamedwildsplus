#!/usr/bin/env python3
"""Giant ground bird model spec + FK checker + pose solver -- SOP Phase 1 / 1b / 2 / 7 (2.9.0, group D, NEW TYPE
`ground_bird`): the South Island giant moa (Dinornis robustus), the elephant bird (Aepyornis maximus) and Genyornis
newtoni (a dromornithid "mihirung"). Tooling forked from mustelid_spec.py; the legs are authored by WORLD DIRECTION per
segment (proboscidean_spec.dchain), so the bird's zig-zag leg -- thigh forward-down, the long drumstick down-BACK, the
tarsus down-forward, a flat foot -- never accumulates per-segment angles.

Blockbench space: Y up, ground y=0, -Z = beak, -X = left. Positive rx turns a part's -Z end UP (so it lifts a forward
chain, droops a trailing one, and swings a hanging leg's foot FORWARD).

BODY = a deep egg-shaped body (core + lower breast + rump), a drooping tail plume, a long neck in three segments held up
and forward, a small head with a short beak (Genyornis: a deep beak, `deepBeak` flag -> setScale Y on the beak), and two
long legs that stand the body at the hip height SOLVED below (feet flat on the ground).
1 block = 1 m at the back: back top is BACK_U, so scale = back height m * 16 / BACK_U.
Run:  python scripts/ground_bird_spec.py [--uv | --zfight | --burial | --json | --pose | --walk]
"""
import math
import sys

import numpy as np

D = math.radians

SPECIES = [
    # name,            variant, real back height m, deep beak
    ("giant_moa",        0, 1.55, False),   # Dinornis robustus -- females to ~3.6 m with the neck raised, ~230 kg
    ("elephant_bird",    1, 1.75, False),   # Aepyornis maximus -- ~3 m tall, the heaviest bird known
    ("genyornis",        2, 1.25, True),    # Genyornis newtoni -- ~2.1 m tall, a huge deep beak
]
HITBOX = (1.0, 2.0)
CANVAS = (128, 128)

PARTS = []
BY_NAME = {}


def add(name, parent, origin, rot, a, b, cls):
    lo = [min(a[i], b[i]) for i in range(3)]
    hi = [max(a[i], b[i]) for i in range(3)]
    rec = (name, parent, tuple(float(x) for x in origin), tuple(float(x) for x in rot), tuple(lo), tuple(hi), cls)
    PARTS.append(rec)
    BY_NAME[name] = rec


def rmat(rx, ry, rz):
    cx, sx = math.cos(D(rx)), math.sin(D(rx))
    cy, sy = math.cos(D(ry)), math.sin(D(ry))
    cz, sz = math.cos(D(rz)), math.sin(D(rz))
    return (np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
            @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
            @ np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]))


def euler_zyx(M):
    b = math.asin(max(-1.0, min(1.0, -M[2][0])))
    a = math.atan2(M[2][1], M[2][2])
    c = math.atan2(M[1][0], M[0][0])
    return math.degrees(a), math.degrees(b), math.degrees(c)


def rot_from_down(d):
    a = np.array([0.0, -1.0, 0.0])
    b = np.asarray(d, float) / np.linalg.norm(d)
    v = np.cross(a, b)
    c = float(np.dot(a, b))
    if np.linalg.norm(v) < 1e-9:
        return np.eye(3) if c > 0 else rmat(180, 0, 0)
    K = np.array([[0, -v[2], v[1]], [v[2], 0, -v[0]], [-v[1], v[0], 0]])
    return np.eye(3) + K + K @ K * (1.0 / (1.0 + c))


def ancestry(name):
    out = []
    while name is not None:
        out.append(BY_NAME[name])
        name = BY_NAME[name][1]
    return out


def world(name, pts, over=None, dy=0.0):
    over = over or {}
    p = np.asarray(pts, dtype=float).reshape(-1, 3)
    for nm, _, o, r, *_ in ancestry(name):
        o = np.asarray(o, dtype=float)
        rr = np.asarray(r, float) + np.asarray(over.get(nm, (0, 0, 0)), float)
        p = (rmat(*rr) @ (p - o).T).T + o
    p[:, 1] -= dy
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


def zchain(parent, pivot, segs, cls, parent_tilt=0.0):
    """A chain laid FORWARD along -Z; ang = WORLD pitch of each segment (local = ang - previous)."""
    px, py, pz = pivot
    prev, par, z = parent_tilt, parent, pz
    for name, ang, L, hw, up, dn, ov in segs:
        add(name, par, (px, py, z), (ang - prev, 0, 0), (px - hw, py - dn, z - L), (px + hw, py + up, z + ov), cls)
        par, prev, z = name, ang, z - L


# ---- the leg, LEFT side (x mirrored): (name, length, width, depth, overlap, world direction) ----
# (first build: shin 2.6 / tarsus 1.8 wide read as a crane's legs; moa and elephant-bird legs were massive)
LEG = [
    ("thigh", 6.0, 3.6, 3.8, 1.4, (0, -0.55, -0.83)),   # forward-down, mostly inside the plumage
    ("shin",  10.0, 3.2, 3.4, 1.0, (0, -0.92, 0.39)),   # the long drumstick, down and BACK
    ("tarsus", 8.0, 2.4, 2.4, 0.8, (0, -0.97, -0.24)),  # down and a little forward
]
FOOT = (4.4, 0.9, 5.4, 4.2)   # width, height, length, how far the toes reach in front of the ankle
HIP_X, HIP_Z = 3.1, 1.0


def hip_height():
    drop = sum(L * abs(np.asarray(d, float)[1] / np.linalg.norm(d)) for _, L, _, _, _, d in LEG)
    return FOOT[1] + drop


H = hip_height()


def build():
    PARTS.clear()
    BY_NAME.clear()
    h = H
    # ---- body: deep and egg-shaped; the breast slung lower in front, the rump rising behind ----
    add("body_main", None, (0, h + 1.5, HIP_Z), (0, 0, 0), (-5.6, h - 5.2, -8.0), (5.6, h + 6.8, 9.5), "body")
    # (+-4.5 put its sides 0.1 from the drumsticks' outer faces: both rotate about X only, so the planes z-fought)
    add("body_breast", "body_main", (0, h + 0.5, -6.0), (-12, 0, 0), (-4.0, h - 6.0, -10.5), (4.0, h + 4.2, -3.5), "body")
    add("body_rump", "body_main", (0, h + 2.5, 8.5), (14, 0, 0), (-4.7, h - 3.0, 7.5), (4.7, h + 6.2, 13.0), "body")
    add("tail", "body_rump", (0, h + 3.0, 12.5), (38, 0, 0), (-3.6, h - 1.2, 12.0), (3.6, h + 4.6, 17.0), "plume")
    # ---- neck + head: held up and forward ----
    zchain("body_main", (0, h + 4.5, -8.5), [
        #  name     ang   L    hw   up   dn   ov
        ("neck_1",  48,  6.5, 2.4, 2.3, 2.3, 1.8),
        ("neck_2",  62,  6.0, 1.95, 1.95, 1.95, 1.0),
        ("neck_3",  40,  5.5, 1.6, 1.7, 1.7, 0.9),
        ("head",    -4,  4.4, 1.85, 1.8, 1.5, 0.8),
        ("beak",   -14,  3.4, 0.85, 0.7, 0.6, 0.4),
    ], "neck")
    # head pivot frame (local, unrotated) -- the chain is laid out along -z at y = h + 4.5
    hz = -8.5 - 6.5 - 6.0 - 5.5          # head pivot z
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"eye_{side}", "head", (s * 2.2, h + 5.1, hz - 2.0), (0, 0, 0),
            (s * 2.2, h + 4.55, hz - 2.8), (s * 2.2, h + 5.65, hz - 1.4), "eye")
    # ---- legs (world-direction chain; the foot counter-rotated flat) ----
    for side, s in (("left", -1.0), ("right", 1.0)):
        x0 = s * HIP_X
        par, y, Wp = "body_main", h, np.eye(3)
        for nm, L, w, d, ov, dv in LEG:
            n = f"leg_{nm}_{side}"
            W = rot_from_down(dv)
            add(n, par, (x0, y, HIP_Z), euler_zyx(Wp.T @ W), (x0 - w / 2, y - L, HIP_Z - d / 2), (x0 + w / 2, y + ov, HIP_Z + d / 2), "leg")
            par, Wp, y = n, W, y - L
        fw, fh, fl, fwd = FOOT
        add(f"foot_{side}", par, (x0, y, HIP_Z), euler_zyx(Wp.T), (x0 - fw / 2, y - fh, HIP_Z - fwd), (x0 + fw / 2, y, HIP_Z - fwd + fl), "foot")


build()
LIMB = ["leg_thigh_left", "leg_shin_left", "leg_tarsus_left", "foot_left"]


def mirror(over):
    out = dict(over)
    for n, (rx, ry, rz) in list(over.items()):
        if "_left" in n:
            out[n.replace("_left", "_right")] = (rx, -ry, -rz)
    return out


def low(names, over, dy):
    return min(world(n, vol(BY_NAME[n], 5), over, dy)[:, 1].min() for n in names)


def back_top():
    return max(world(n, corners(BY_NAME[n]))[:, 1].max() for n in ("body_main", "body_rump"))


# ================================================================ phase 7: SOLVED rest poses
def _leg_fit(over, dy):
    """(thigh, shin, tarsus, foot) rx for the folded, sitting leg: nothing below ground, the tarsus and foot lying on it."""
    rng = np.random.default_rng(5)
    def score(v):
        o = dict(over)
        for n, a in zip(LIMB, v):
            base = o.get(n, (0, 0, 0))
            o[n] = (base[0] + a, base[1], base[2])
        pen = 0.0
        for n in LIMB:
            l = low([n], o, dy)
            if l < 0:
                pen += l * l * 400
        # A sitting ratite rests on its HOCKS: the hock at the rear under the hip, the tarsus lying flat FORWARD along
        # the ground under the belly, the foot flat under the breast. Two failed fits bracket it: the feet trailing out
        # behind the body, then the tarsus thrown out in front with the foot standing up.
        tar = world("leg_tarsus_left", vol(BY_NAME["leg_tarsus_left"], 5), o, dy)
        pen += (tar[:, 1].min() - 0.05) ** 2 * 6
        pen += 0.4 * (tar[:, 1].max() - tar[:, 1].min() - 2.4) ** 2        # the tarsus lies along the ground
        foot = world("foot_left", vol(BY_NAME["foot_left"], 5), o, dy)
        pen += (foot[:, 1].min() - 0.05) ** 2 * 4
        pen += 2.0 * (foot[:, 1].max() - foot[:, 1].min() - FOOT[1]) ** 2   # the foot flat
        pen += 0.3 * (foot[:, 2].mean() + 4.0) ** 2                          # under the breast, not out in front
        hock = tar[np.argmax(tar[:, 2])]
        pen += 0.2 * max(0.0, 4.0 - hock[2]) ** 2                            # the hock at the rear
        pen += 0.0003 * sum(x * x for x in v)
        return pen, o
    # seed: thigh as it stands, drumstick swung back-up (-50), tarsus forward flat (+130), foot counter-rotated (-80)
    best = np.array([0.0, -50.0, 130.0, -80.0])
    bs, _ = score(best)
    span = np.array([50.0, 50.0, 50.0, 50.0])
    for _ in range(12):
        for c in best + rng.normal(0, 1, (500, 4)) * span:
            sc, _ = score(c)
            if sc < bs:
                bs, best = sc, c.copy()
        span *= 0.62
    _, o = score(best)
    return o, best, bs


def solve_rest(sleep):
    dy = low(["body_main", "body_breast"], {}, 0) - 0.4                   # the belly 0.4 off the ground
    over, v, s = _leg_fit({}, dy)
    over = mirror({k: val for k, val in over.items() if "_left" in k})
    if sleep:
        # sleep: the neck laid FORWARD along the ground, head level, chin resting on it. (First try bent the whole
        # chain down in proportion: the neck arched and the head hung beak-down like an anteater's.) The upper neck
        # flattens to about -8/-10 deg world, the head is counter-rotated level, and neck_1 lowers until the chin rests.
        for d1 in np.arange(-20, -85, -0.5):
            o = dict(over)
            o["neck_1"] = (d1, 0, 0)
            o["neck_2"] = (-17.0, 0, 0)
            o["neck_3"] = (20.0, 0, 0)
            o["head"] = (1.0 - d1, 0, 0)
            if low(["head", "beak", "neck_3"], o, dy) <= 0.3:
                over = o
                break
    else:
        # sitting alert: the neck stays up; the body is lower, so nothing else changes
        pass
    for a in np.arange(0, 60, 0.5):
        o = dict(over)
        o["tail"] = (a, 0, 0)
        if low(["tail"], o, dy) <= 0.3:
            over = o
            break
    return dict(dy=dy, over=over, fit=s)


def pose_report():
    for name, sol in (("SIT", solve_rest(False)), ("SLEEP", solve_rest(True))):
        over, dy = sol["over"], sol["dy"]
        lows = {n: low([n], over, dy) for n in BY_NAME if BY_NAME[n][6] != "eye"}
        worst = min(lows.items(), key=lambda kv: kv[1])
        print(f"{name}: root drop {dy:+.2f}u, fit {sol['fit']:.4f}; lowest part {worst[0]} {worst[1]:+.2f}"
              + ("  OK" if worst[1] > -0.06 else "  <== THROUGH THE GROUND"))
        for n in sorted(over):
            if "_right" not in n:
                print(f"    {n:<18} {tuple(round(float(x), 1) for x in over[n])}")


# ================================================================ walk (biped, Citadel walk() terms, Java radians) -- FITTED
# Each joint = one Citadel walk() term: rotateAngleX += degree * cos(t + offset) + weight. A hand-set first try dug the
# foot 0.96u into the ground, lifted it 5u and kept it planted 24% of the cycle; walk_fit() searches the 4 joints'
# (degree, offset, weight) for: no digging, ~60% stance, the planted foot sliding BACKWARD (the body passes over it),
# a ~10u stride, a 1.5-3u swing lift, and the foot flat while planted. The fitted set is WALK_FIT.
WALK_SPEED = 0.7
JOINTS = ["leg_thigh", "leg_shin", "leg_tarsus", "foot"]
WALK_FIT = None     # filled by walk_fit() / loaded from scratch/ground_bird_walk.json


def walk_terms(side, fit=None):
    fit = WALK_FIT if fit is None else [float(x) for x in fit]
    off = 0.0 if side == "left" else math.pi
    return [(f"{j}_{side}", fit[3 * k], off + fit[3 * k + 1], fit[3 * k + 2]) for k, j in enumerate(JOINTS)]


def _cycle(fit, steps=36):
    terms = walk_terms("left", fit)
    lows, zs, flat = [], [], []
    for t in np.linspace(0, 2 * math.pi, steps + 1)[:-1]:
        over = {}
        for n, deg, off, w in terms:
            d = deg * math.cos(t + off) + w
            cur = over.get(n, (0.0, 0, 0))
            over[n] = (cur[0] - math.degrees(d), 0, 0)
        fp = world("foot_left", corners(BY_NAME["foot_left"]), over)
        lw = min(fp[:, 1].min(), world("leg_tarsus_left", corners(BY_NAME["leg_tarsus_left"]), over)[:, 1].min())
        lows.append(lw)
        zs.append(fp[:, 2].mean())
        flat.append(fp[:, 1].max() - fp[:, 1].min() - FOOT[1])
    return np.array(lows), np.array(zs), np.array(flat)


def _walk_score(fit):
    lows, zs, flat = _cycle(fit)
    st = lows < 0.35
    dz = np.diff(np.r_[zs, zs[0]])
    pen = 300 * np.sum(np.minimum(0, lows + 0.02) ** 2)
    pen += 20 * (st.mean() - 0.6) ** 2
    pen += 2.0 * np.sum(np.minimum(0, dz[st]) ** 2)                  # planted foot must not slide forward
    pen += 0.05 * ((zs.max() - zs.min()) - 10.0) ** 2
    lift = lows.max()
    pen += 0.5 * max(0, 1.5 - lift) ** 2 + 0.5 * max(0, lift - 3.0) ** 2
    pen += 1.0 * np.sum(flat[st] ** 2) / max(1, st.sum())
    return pen


def walk_fit(rounds=10, n=260, seed=3):
    rng = np.random.default_rng(seed)
    best = np.array([0.45, 0.0, 0.0, 0.4, -1.57, 0.3, 0.4, 1.57, -0.2, 0.45, 0.0, 0.0])
    bs = _walk_score(best)
    span = np.array([0.3, 1.5, 0.3] * 4)
    for _ in range(rounds):
        for c in best + rng.normal(0, 1, (n, 12)) * span:
            c[0::3] = np.clip(c[0::3], -1.2, 1.2)
            s = _walk_score(c)
            if s < bs:
                bs, best = s, c.copy()
        span *= 0.65
    return best, bs


def walk_check(fit=None):
    lows, zs, flat = _cycle(WALK_FIT if fit is None else fit, 72)
    st = lows < 0.35
    print(f"WALK: foot lowest {lows.min():+.2f} (dig), highest {lows.max():.2f} (lift), stride {zs.max() - zs.min():.1f}u, "
          f"planted {st.mean():.0%} of the cycle, foot tilt while planted <= {flat[st].max() if st.any() else 0:.2f}u")


def _load_fit():
    import json
    import pathlib
    p = pathlib.Path(__file__).resolve().parent.parent / "scratch/ground_bird_walk.json"
    return json.loads(p.read_text())["fit"] if p.exists() else None


WALK_FIT = _load_fit()


# ================================================================ UV / export / checks
def footprint(p):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    w, h, d = (abs(b[i] - a[i]) for i in range(3))
    if p[6] == "eye":
        w = 0.0
    return int(math.ceil(2 * (w + d))), int(math.ceil(h + d))


def uvpack():
    W, Hh = CANVAS
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
    print(f"UV SHELF PACK on {W}x{Hh}: {len(placed)} boxes, {used} rows used, zero overlaps asserted")
    assert used <= Hh, "does not fit"
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
        print(f"  {area:6.2f}u2  {na:<16} {'XYZ'[fa[0]]}  <->  {nb:<16} {'XYZ'[fb[0]]}{rel}")
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
        flag = "   <== FEATURE MOSTLY BURIED" if frac > 0.55 and cls in ("plume", "foot", "neck") else ""
        print(f"  {frac:6.1%}  {name}{flag}")


def main():
    wc = {p[0]: world(p[0], corners(p)) for p in PARTS}
    lo = {k: v.min(0) for k, v in wc.items()}
    hi = {k: v.max(0) for k, v in wc.items()}
    floor = min(v[1] for v in lo.values())
    bt = back_top()
    head_top = hi["head"][1]
    print(f"hip height {H:.2f}u; lowest point {floor:+.3f}" + ("  OK" if floor > -0.02 else "  <== FLOOR")
          + f"; back {bt:.1f}u, head top {head_top:.1f}u ({head_top / bt:.2f}x back), belly {lo['body_main'][1]:.1f}u")
    foot = wc["foot_left"]
    print(f"  foot under the body: toes z {foot[:, 2].min():.1f}..{foot[:, 2].max():.1f} (body {lo['body_main'][2]:.1f}..{hi['body_main'][2]:.1f})")
    for name, var, real, deep in SPECIES:
        s = real * 16.0 / bt
        print(f"  {name:<14} back {real:.2f} m -> scale {s:.3f}   head up {head_top * s / 16:.2f} m  (hitbox {HITBOX[0]} x {HITBOX[1]})")
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head", "beak", "neck_3"):
            p = BY_NAME[anc]
            l = local_of(anc, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gap = max(max(a[i] - l[i], l[i] - b[i]) for i in range(3))
            if gap < 0.3:
                print(f"  eye_{side} vs {anc} clear by {gap:.2f}  <== TOO CLOSE")
    tot = sum(fw * fh for fw, fh in (footprint(p) for p in PARTS))
    print(f"  {len(PARTS)} boxes, UV {tot} px = {100 * tot / (CANVAS[0] * CANVAS[1]):.0f}% of {CANVAS[0]}x{CANVAS[1]}")


if __name__ == "__main__":
    if "--zfight" in sys.argv:
        zfight()
    elif "--uv" in sys.argv:
        uvpack()
    elif "--json" in sys.argv:
        emit_json()
    elif "--burial" in sys.argv:
        burial()
    elif "--pose" in sys.argv:
        pose_report()
    elif "--walk-fit" in sys.argv:
        import json
        import pathlib
        fit, score = walk_fit()
        p = pathlib.Path(__file__).resolve().parent.parent / "scratch/ground_bird_walk.json"
        p.write_text(json.dumps({"fit": [round(float(x), 4) for x in fit], "score": float(score)}))
        print(f"fitted (score {score:.3f}) -> {p}")
        walk_check(list(fit))
    elif "--walk" in sys.argv:
        walk_check()
    else:
        main()
