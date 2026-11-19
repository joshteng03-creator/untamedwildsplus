#!/usr/bin/env python3
"""Mustelid model spec + forward-kinematics checker + pose solver -- SOP Phase 1 / 1b / 2 / 7 (2.9.0, group D,
NEW TYPE `mustelid`): the wolverine (Gulo gulo) and the honey badger (Mellivora capensis). Tooling forked from
pinniped_spec.py / orca_spec.py.

Blockbench space: Y up, ground y=0, -Z = nose, -X = left. Positive rx turns a part's -Z end UP (so it swings a
hanging leg's foot FORWARD and a trailing tail's tip DOWN).

BODY = long, low and heavy: a barrel arched over the middle (chest + hips + a raised back box), SHORT thick legs on
broad plantigrade paws, a broad heavy head on a thick neck with a short muzzle and a hinged lower jaw, small round ears,
a bushy tail. The two species share the mesh; the honey badger's identity is its skin (the grey-white mantle).
1 block = 1 m at the shoulder: the chest top is 8.4u, so scale = shoulder m * 16 / 8.4.

Poses (SOLVED here, emitted into the Java by gen_model_mustelid.py):
  * SIT  -- the body pitches up about the chest pivot until the rump rests on the ground; the forelegs are rotated back
            to vertical and the root drops until the fore paws are planted; hind legs fold forward under the belly.
  * SLEEP -- belly-down, legs tucked (fore forward, hind forward under), chin and tail resting on the ground.
Run:  python scripts/mustelid_spec.py [--uv | --zfight | --burial | --json | --pose | --walk]
"""
import math
import sys

import numpy as np

D = math.radians

SPECIES = [
    # name,          variant, real shoulder height m
    ("wolverine",      0, 0.42),   # Gulo gulo -- 9-25 kg, the largest land mustelid
    ("honey_badger",   1, 0.26),   # Mellivora capensis -- 9-16 kg, lower and flatter
]
HITBOX = (0.7, 0.6)
CANVAS = (128, 64)
SHOULDER_U = 8.4

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


def ancestry(name):
    out = []
    while name is not None:
        out.append(BY_NAME[name])
        name = BY_NAME[name][1]
    return out


def world(name, pts, over=None, dy=0.0):
    """over: {part: (drx, dry, drz)} BB degrees ADDED to the rest rotation; dy: the root moves DOWN by dy."""
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


def build():
    PARTS.clear()
    BY_NAME.clear()
    # ---- barrel: chest + hips, an arched back box over the middle ----
    add("body_chest", None, (0, 6.0, -2.5), (0, 0, 0), (-3.0, 3.0, -6.5), (3.0, 8.4, 0.5), "body")
    add("body_hips", "body_chest", (0, 6.0, 0.5), (0, 0, 0), (-3.2, 3.3, 0.0), (3.2, 8.0, 6.6), "body")
    add("body_back", "body_chest", (0, 8.0, -1.0), (0, 0, 0), (-2.3, 7.6, -4.5), (2.3, 9.0, 4.0), "body")
    # ---- thick neck, broad heavy head, short muzzle, hinged jaw ----
    # the head is carried low and level (a mustelid trots nose-down; +6 lifted it like a dog's)
    add("neck", "body_chest", (0, 6.8, -6.0), (0, 0, 0), (-2.2, 4.6, -8.8), (2.2, 8.0, -5.2), "head")
    add("head", "neck", (0, 6.6, -8.5), (0, 0, 0), (-2.5, 4.4, -12.0), (2.5, 8.4, -8.2), "head")
    add("snout", "head", (0, 5.6, -11.8), (0, 0, 0), (-1.35, 4.6, -14.2), (1.35, 6.75, -11.6), "head")
    add("nose", "snout", (0, 6.3, -14.2), (0, 0, 0), (-0.65, 5.85, -14.5), (0.65, 6.95, -14.0), "nose")
    add("jaw", "head", (0, 4.6, -11.0), (0, 0, 0), (-1.15, 4.0, -13.8), (1.15, 4.9, -10.8), "jaw")
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"ear_{side}", "head", (s * 1.9, 8.2, -9.2), (0, 0, s * 15), (s * 1.2, 8.0, -9.5), (s * 2.6, 9.3, -8.9), "ear")
        add(f"eye_{side}", "head", (s * 2.85, 7.0, -10.8), (0, 0, 0), (s * 2.85, 6.4, -11.6), (s * 2.85, 7.6, -10.0), "eye")
    # ---- legs: short and thick, the outer faces 0.25 proud of the barrel; broad plantigrade paws ----
    for side, s in (("left", -1.0), ("right", 1.0)):
        fx, hx = s * 2.15, s * 2.3
        add(f"leg_front_{side}_1", "body_chest", (fx, 5.2, -4.0), (0, 0, 0), (fx - 1.1, 1.6, -5.0), (fx + 1.1, 5.8, -3.0), "leg")
        add(f"leg_front_{side}_2", f"leg_front_{side}_1", (fx, 2.0, -4.0), (0, 0, 0), (fx - 0.85, 0.7, -4.7), (fx + 0.85, 2.3, -3.3), "leg")
        add(f"paw_front_{side}", f"leg_front_{side}_2", (fx, 0.8, -4.2), (0, 0, 0), (fx - 1.2, 0.0, -5.9), (fx + 1.2, 1.0, -2.8), "paw")
        add(f"leg_back_{side}_1", "body_hips", (hx, 5.8, 4.4), (0, 0, 0), (hx - 1.15, 1.9, 3.2), (hx + 1.15, 6.6, 5.7), "leg")
        add(f"leg_back_{side}_2", f"leg_back_{side}_1", (hx, 2.3, 4.9), (0, 0, 0), (hx - 0.9, 0.7, 4.1), (hx + 0.9, 2.6, 5.4), "leg")
        add(f"paw_back_{side}", f"leg_back_{side}_2", (hx, 0.8, 4.6), (0, 0, 0), (hx - 1.15, 0.0, 2.6), (hx + 1.15, 1.0, 5.8), "paw")
    # ---- bushy tail, carried low (positive rx droops a trailing part) ----
    add("tail_1", "body_hips", (0, 7.2, 6.4), (25, 0, 0), (-1.2, 6.1, 6.0), (1.2, 8.3, 9.4), "tail")
    add("tail_2", "tail_1", (0, 7.2, 9.1), (10, 0, 0), (-1.55, 5.8, 8.8), (1.55, 8.6, 12.6), "tail")


build()

FORE = ["leg_front_left_1", "leg_front_left_2", "paw_front_left"]
HIND = ["leg_back_left_1", "leg_back_left_2", "paw_back_left"]


def mirror(over):
    """left-side deltas -> both sides (rx is shared; ry / rz flip sign)."""
    out = dict(over)
    for n, (rx, ry, rz) in list(over.items()):
        if "_left" in n:
            out[n.replace("_left", "_right")] = (rx, -ry, -rz)
    return out


def low(names, over, dy):
    return min(world(n, vol(BY_NAME[n], 5), over, dy)[:, 1].min() for n in names)


# ================================================================ phase 7: SOLVED poses
def _limb_fit(over, dy, limb, target_low=0.05, bend=0.0004):
    """Random search of (upper, lower, paw) rx for one limb (left; mirrored): nothing below ground, the paw flat on it."""
    rng = np.random.default_rng(7 if limb is FORE else 11)
    names = limb
    def score(v):
        o = dict(over)
        for n, a in zip(names, v):
            base = o.get(n, (0, 0, 0))
            o[n] = (base[0] + a, base[1], base[2])
        pen = 0.0
        for n in names:
            l = low([n], o, dy)
            if l < 0:
                pen += l * l * 400
        paw = world(names[2], vol(BY_NAME[names[2]], 5), o, dy)
        pen += (paw[:, 1].min() - target_low) ** 2 * 4
        pen += 0.05 * (paw[:, 1].max() - paw[:, 1].min() - 1.0) ** 2     # sole level (the paw box is 1u tall)
        pen += bend * sum(x * x for x in v)
        return pen, o
    best = np.zeros(3)
    bs, _ = score(best)
    span = np.array([60.0, 60.0, 60.0])
    for _ in range(10):
        for c in best + rng.normal(0, 1, (400, 3)) * span:
            sc, _ = score(c)
            if sc < bs:
                bs, best = sc, c.copy()
        span *= 0.6
    _, o = score(best)
    return o, best, bs


def solve_sleep():
    dy = min(low([n], {}, 0) for n in ("body_chest", "body_hips")) - 0.3        # belly 0.3 off the ground
    over = {}
    # fore legs fold FORWARD (+rx), hind legs fold forward under the belly (+rx): seeded, then fitted
    over["leg_front_left_1"] = (80, 0, 0)
    over["leg_back_left_1"] = (70, 0, 0)
    over = mirror(over)
    over, fv, fs = _limb_fit(over, dy, FORE)
    over, hv, hs = _limb_fit(over, dy, HIND)
    over = mirror({k: v for k, v in over.items() if "_left" in k} | {k: v for k, v in over.items() if "_left" not in k and "_right" not in k})
    # chin down: the smallest neck/head lower that rests the jaw 0.15 above the ground
    for a in np.arange(0, -60, -0.5):
        o = dict(over)
        o["neck"] = (a * 0.6, 0, 0)
        o["head"] = (a * 0.4, 0, 0)
        if low(["jaw", "snout", "nose", "head"], o, dy) <= 0.15:
            over = o
            break
    # tail droops onto the ground
    for a in np.arange(0, 80, 0.5):
        o = dict(over)
        o["tail_1"] = (a, 0, 0)
        if low(["tail_1", "tail_2"], o, dy) <= 0.1:
            over = o
            break
    return dict(dy=dy, over=over, fit=(fs, hs))


def solve_sit():
    """theta = root pitch (front up). For each theta the fore legs are counter-rotated to vertical and the root
    dropped until the fore paws touch; pick the theta that rests the rump ~0.15 off the ground."""
    best = None
    for th in np.arange(4, 60, 0.5):
        over = mirror({"body_chest": (th, 0, 0), "leg_front_left_1": (-th, 0, 0)})
        over["body_chest"] = (th, 0, 0)
        dy = low(["paw_front_left"], over, 0) - 0.02
        rump = low(["body_hips"], over, dy)
        if best is None or abs(rump - 0.15) < abs(best[2] - 0.15):
            best = (th, dy, rump, over)
    th, dy, rump, over = best
    over, hv, hs = _limb_fit(over, dy, HIND, target_low=0.05)
    over = mirror({k: v for k, v in over.items() if "_left" in k}) | {k: v for k, v in over.items() if "_left" not in k and "_right" not in k}
    # head level again (the pitch tilted it up): counter part of it on the neck
    over["neck"] = (-th * 0.6, 0, 0)
    # the pitch swings the tail INTO the ground: LIFT it (negative rx) until it just rests on the ground
    for a in np.arange(0, -90, -0.5):
        o = dict(over)
        o["tail_1"] = (a, 0, 0)
        if low(["tail_1", "tail_2"], o, dy) >= 0.05:
            over = o
            break
    return dict(theta=th, dy=dy, rump=rump, over=over, fit=hs)


def pose_report():
    for name, sol in (("SLEEP", solve_sleep()), ("SIT", solve_sit())):
        over, dy = sol["over"], sol["dy"]
        lows = {n: low([n], over, dy) for n in BY_NAME if BY_NAME[n][6] != "eye"}
        worst = min(lows.items(), key=lambda kv: kv[1])
        print(f"{name}: root drop {dy:+.2f}u" + (f", pitch {sol['theta']:.1f} deg, rump {sol['rump']:+.2f}" if "theta" in sol else "")
              + f"; lowest part {worst[0]} {worst[1]:+.2f}" + ("  OK" if worst[1] > -0.06 else "  <== THROUGH THE GROUND"))
        for n in sorted(over):
            if "_right" in n:
                continue
            print(f"    {n:<18} {tuple(round(x, 1) for x in over[n])}")


# ================================================================ walk check (Citadel walk() terms, Java radians)
WALK_SPEED = 0.9
WALK = {"fore": (0.55, 0.45), "hind": (0.55, 0.45)}   # (upper swing, lower flex) degree


def walk_terms(limb, side):
    off = {("hind", "left"): 0.0, ("fore", "left"): -math.pi / 2, ("hind", "right"): math.pi, ("fore", "right"): math.pi / 2}[(limb, side)]
    A, F = WALK[limb]
    n = ("leg_front_" if limb == "fore" else "leg_back_") + side
    paw = ("paw_front_" if limb == "fore" else "paw_back_") + side
    # upper swings; lower flexes only in the swing (weight = degree), the paw counter-rotates both to stay flat
    return [(f"{n}_1", A, off, 0.0), (f"{n}_2", F, off - math.pi / 2, F), (paw, -A, off, 0.0), (paw, -F, off - math.pi / 2, -F)]


def walk_check():
    amt = 1.0
    for limb in ("fore", "hind"):
        terms = walk_terms(limb, "left")
        lows = []
        for t in np.linspace(0, 2 * math.pi, 73)[:-1]:
            over = {}
            for n, deg, off, w in terms:
                d = amt * (deg * math.cos(t + off) + w)
                cur = over.get(n, (0.0, 0, 0))
                over[n] = (cur[0] - math.degrees(d), 0, 0)      # Java +x -> BB -x
            lows.append(min(world(n, vol(BY_NAME[n], 5), over)[:, 1].min() for n in {x[0] for x in terms}))
        lows = np.array(lows)
        print(f"WALK {limb}: lowest {lows.min():+.2f} (dig), highest lowest-point {lows.max():.2f} (lift), "
              f"planted {np.mean(lows < 0.3):.0%} of the cycle")


# ================================================================ UV / export / checks
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
    assert used <= H, "does not fit"
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
        print(f"  {area:6.2f}u2  {na:<18} {'XYZ'[fa[0]]}  <->  {nb:<18} {'XYZ'[fb[0]]}{rel}")
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
        flag = "   <== FEATURE MOSTLY BURIED" if frac > 0.55 and cls in ("ear", "nose", "jaw", "tail", "paw") else ""
        print(f"  {frac:6.1%}  {name}{flag}")


def main():
    wc = {p[0]: world(p[0], corners(p)) for p in PARTS}
    lo = {k: v.min(0) for k, v in wc.items()}
    hi = {k: v.max(0) for k, v in wc.items()}
    floor = min(v[1] for v in lo.values())
    nose = lo["nose"][2]
    rump = hi["body_hips"][2]
    tail = hi["tail_2"][2]
    print(f"lowest point {floor:+.3f}" + ("  OK" if floor > -0.02 else "  <== FLOOR")
          + f"; shoulder {hi['body_chest'][1]:.1f}u, back {hi['body_back'][1]:.1f}u, belly {lo['body_chest'][1]:.1f}u")
    print(f"  nose-to-rump {rump - nose:.1f}u ({(rump - nose) / hi['body_chest'][1]:.2f}x shoulder; wolverine ~2.1x), "
          f"tail beyond rump {tail - rump:.1f}u, head {hi['head'][2] - lo['nose'][2]:.1f}u long")
    for name, var, real in SPECIES:
        s = real * 16.0 / SHOULDER_U
        print(f"  {name:<14} shoulder {real:.2f} m -> scale {s:.3f}   nose-to-rump {(rump - nose) * s / 16:.2f} m")
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head", "snout", "neck", f"ear_{side}"):
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
    elif "--walk" in sys.argv:
        walk_check()
    else:
        main()
