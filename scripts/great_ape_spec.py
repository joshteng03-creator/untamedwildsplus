#!/usr/bin/env python3
"""Great ape model spec + FK checker + pose / gait solver -- SOP Phase 1 / 1b / 2 / 7 (2.9.0, group D, NEW TYPE
`great_ape`): Gigantopithecus blacki and the Bornean orangutan (Pongo pygmaeus, its closest living relative).
Tooling forked from ground_bird_spec.py.

Blockbench space: Y up, ground y=0, -Z = face, -X = left. Positive rx turns a part's -Z end UP.

BODY = a knuckle-walking quadruped: LONG arms on fists, shorter legs on long plantigrade feet, so the torso is pitched
up from the hips to the shoulders (PITCH, solved from the two limb heights). Limbs are authored by WORLD direction per
segment under the pitched torso (local = W_parent^T . W_k), their joints placed by transforming the wanted WORLD joint
into the torso's rest frame. The head is counter-rotated level: heavy brow, a prognathous muzzle, a massive jaw (the
fossils are almost all jaws), small ears, and FORWARD-facing eye planes (an ape's eyes look forward, not sideways).
1 block = 1 m at the shoulder: the top of the back over the shoulders is BACK_U, scale = shoulder m * 16 / BACK_U.
Run:  python scripts/great_ape_spec.py [--uv | --zfight | --burial | --json | --pose | --walk | --walk-fit]
"""
import json
import math
import pathlib
import sys

import numpy as np

D = math.radians
ROOT = pathlib.Path(__file__).resolve().parent.parent

SPECIES = [
    # name,              variant, real shoulder height m (on all fours)
    ("gigantopithecus",    0, 1.80),   # Gigantopithecus blacki -- ~3 m standing, 200-300 kg (estimates from jaws/teeth)
    ("bornean_orangutan",  1, 0.90),   # Pongo pygmaeus -- males ~1.4 m standing, ~80 kg
]
HITBOX = (1.4, 2.0)
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


# ---- limbs (LEFT; x mirrored): (name, length, width, depth, overlap, world direction) ----
ARM = [("arm_upper", 11.0, 3.8, 4.0, 1.6, (0, -1.0, -0.08)), ("arm_fore", 11.0, 3.3, 3.5, 1.0, (0, -1.0, 0.10))]
HAND = (3.8, 2.8, 4.6)                 # the fist, knuckles down (deep enough that its z faces clear the forearm's)
LEG = [("leg_thigh", 7.0, 4.0, 4.2, 1.6, (0, -0.80, -0.60)), ("leg_shin", 7.0, 3.4, 3.6, 1.0, (0, -0.97, 0.24))]
FOOT = (3.9, 1.4, 6.8, 5.2)            # width, height, length, toes forward of the ankle
SH_X, SH_Z = 6.0, -6.0
HIP_X, HIP_Z = 4.3, 7.5   # (3.7 put the thighs' outer faces on the belly's / torso's sides: z-fight)


def drop(chain):
    return sum(L * abs(np.asarray(d, float)[1] / np.linalg.norm(d)) for _, L, _, _, _, d in chain)


SH_Y = HAND[1] + drop(ARM)
HIP_Y = FOOT[1] + drop(LEG)
PITCH = math.degrees(math.atan2(SH_Y - HIP_Y, HIP_Z - SH_Z))
ROOTP = np.array([0.0, HIP_Y + 1.8, HIP_Z])          # the torso pivots just above the hip joint


def to_local(Wroot, wpt):
    """a WORLD point -> the root's rest frame (the torso is the only rotated ancestor of limbs and head)."""
    return Wroot.T @ (np.asarray(wpt, float) - ROOTP) + ROOTP


def limb(prefix_parts, side, s, joint_world, chain, Wroot, end_box, end_name, end_offset):
    jl = to_local(Wroot, joint_world)
    par, x0, y, z0, Wp = "body_main", jl[0], jl[1], jl[2], Wroot
    for nm, L, w, d, ov, dv in chain:
        n = f"{nm}_{side}"
        W = rot_from_down(dv)
        add(n, par, (x0, y, z0), euler_zyx(Wp.T @ W), (x0 - w / 2, y - L, z0 - d / 2), (x0 + w / 2, y + ov, z0 + d / 2), "limb")
        par, Wp, y = n, W, y - L
    ew, eh, el, fwd = end_box
    # the end box reaches 0.6 up into the last segment: flush, its top face lay on the forearm's end face (z-fight)
    add(f"{end_name}_{side}", par, (x0, y, z0), euler_zyx(Wp.T), (x0 - ew / 2, y - eh, z0 - fwd), (x0 + ew / 2, y + 0.6, z0 - fwd + el), end_name)


def build():
    PARTS.clear()
    BY_NAME.clear()
    Wr = rmat(PITCH, 0, 0)
    px, py, pz = ROOTP
    # ---- torso (rest frame along -Z from the hip pivot, pitched up by PITCH) ----
    add("body_main", None, tuple(ROOTP), (PITCH, 0, 0), (-6.0, py - 6.8, pz - 19.5), (6.0, py + 5.0, pz + 3.0), "body")
    add("body_chest", "body_main", (0, py + 1.0, pz - 14.0), (0, 0, 0), (-7.2, py - 4.2, pz - 21.0), (7.2, py + 5.8, pz - 9.5), "body")
    add("body_belly", "body_main", (0, py - 4.0, pz - 5.0), (0, 0, 0), (-5.6, py - 8.6, pz - 11.0), (5.6, py - 1.0, pz + 1.0), "body")
    # ---- head: placed in WORLD, counter-rotated to face slightly down ----
    # the head is the NECK's child (the 2.8.0 eat/drink solver moves the neck's whole subtree as "the head"): its
    # pivot is the wanted WORLD point carried into the neck's rest frame, its local pitch the world pitch minus both
    npiv = to_local(Wr, (0, SH_Y + 0.5, SH_Z - 2.5))
    neck_rx = -PITCH * 0.5
    add("neck", "body_main", tuple(npiv), (neck_rx, 0, 0), tuple(npiv + np.array([-3.4, -3.0, -4.0])),
        tuple(npiv + np.array([3.4, 3.4, 2.0])), "head")
    hw = np.array([0.0, SH_Y + 1.6, SH_Z - 5.5])
    hl = rmat(neck_rx, 0, 0).T @ (to_local(Wr, hw) - npiv) + npiv
    hx, hy, hz = hl
    add("head", "neck", (hx, hy, hz), (-PITCH - 6.0 - neck_rx, 0, 0), (hx - 4.3, hy - 3.6, hz - 6.6), (hx + 4.3, hy + 4.6, hz + 0.6), "head")
    add("brow", "head", (hx, hy + 3.1, hz - 6.0), (0, 0, 0), (hx - 4.5, hy + 2.4, hz - 7.1), (hx + 4.5, hy + 3.9, hz - 4.6), "head")
    add("muzzle", "head", (hx, hy - 1.2, hz - 6.6), (0, 0, 0), (hx - 3.0, hy - 3.2, hz - 9.2), (hx + 3.0, hy + 0.8, hz - 6.0), "head")
    add("nose", "muzzle", (hx, hy + 0.1, hz - 9.2), (0, 0, 0), (hx - 1.2, hy - 0.4, hz - 9.55), (hx + 1.2, hy + 0.5, hz - 9.0), "nose")
    add("jaw", "head", (hx, hy - 3.3, hz - 2.5), (0, 0, 0), (hx - 3.3, hy - 4.8, hz - 8.8), (hx + 3.3, hy - 2.7, hz - 2.6), "jaw")
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"ear_{side}", "head", (s * 4.4, hy + 1.0, hz - 2.6), (0, 0, 0),
            (s * 4.0, hy + 0.2, hz - 3.2), (s * 4.9, hy + 1.8, hz - 2.0), "ear")   # inner face 0.3 inside the skull
        # forward-facing eye plane under the brow, 0.35 in front of the face
        add(f"eye_{side}", "head", (s * 1.9, hy + 1.5, hz - 6.95), (0, 0, 0),
            (s * 1.2, hy + 0.9, hz - 6.95), (s * 2.6, hy + 2.1, hz - 6.95), "eyef")
    # ---- limbs ----
    for side, s in (("left", -1.0), ("right", 1.0)):
        limb(None, side, s, (s * SH_X, SH_Y, SH_Z), ARM, Wr, (HAND[0], HAND[1], HAND[2], HAND[2] / 2 + 0.3), "hand", 0)
        limb(None, side, s, (s * HIP_X, HIP_Y, HIP_Z), LEG, Wr, FOOT, "foot", 0)


build()
FORE = ["arm_upper_left", "arm_fore_left", "hand_left"]
HIND = ["leg_thigh_left", "leg_shin_left", "foot_left"]


def mirror(over):
    out = dict(over)
    for n, (rx, ry, rz) in list(over.items()):
        if "_left" in n:
            out[n.replace("_left", "_right")] = (rx, -ry, -rz)
    return out


def low(names, over, dy):
    return min(world(n, vol(BY_NAME[n], 5), over, dy)[:, 1].min() for n in names)


def back_top():
    return max(world(n, corners(BY_NAME[n]))[:, 1].max() for n in ("body_main", "body_chest"))


# ================================================================ phase 7: SOLVED rest poses
def _fit(over, dy, names, end, end_h, seed, guess, extra=None):
    rng = np.random.default_rng(seed)
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
        e = world(end, vol(BY_NAME[end], 5), o, dy)
        pen += (e[:, 1].min() - 0.05) ** 2 * 6
        pen += 1.0 * (e[:, 1].max() - e[:, 1].min() - end_h) ** 2
        if extra:
            pen += extra(o)
        pen += 0.0002 * sum(x * x for x in v)
        return pen, o
    best = np.asarray(guess, float)
    bs, _ = score(best)
    span = np.full(len(names), 50.0)
    for _ in range(11):
        for c in best + rng.normal(0, 1, (400, len(names))) * span:
            sc, _ = score(c)
            if sc < bs:
                bs, best = sc, c.copy()
        span *= 0.6
    _, o = score(best)
    return o, bs


def solve_rest(sleep):
    if sleep:
        # Asleep the ape sits SLUMPED: the solved sit with the head dropped chin-to-chest. (A belly-down solve put the
        # jaw 2.4u through the ground and folded the forearms 160 deg: the pitched skull points the face down once the
        # torso is level.) Apes resting on the ground commonly sleep sitting up.
        sol = solve_rest(False)
        o = dict(sol["over"])
        h = o.get("head", (0, 0, 0))
        o["head"] = (h[0] - 24.0, h[1], h[2])
        n = o.get("neck", (0, 0, 0))
        o["neck"] = (n[0] - 10.0, n[1], n[2])
        return dict(dy=sol["dy"], over=o, fit=sol["fit"])
    else:
        # sitting up on the rump: the torso raised near upright, the root dropped onto the rump, legs forward, fists down
        over = {"body_main": (40.0, 0, 0)}
        dy = low(["body_main", "body_belly"], over, 0) - 0.2
        over["head"] = (-40.0, 0, 0)
        o, fs = _fit(over, dy, FORE, "hand_left", HAND[1], 3, [-30.0, 0.0, 0.0])
        o, hs = _fit(o, dy, HIND, "foot_left", FOOT[1], 5, [-60.0, 40.0, 30.0],
                     extra=lambda o: 0.2 * max(0.0, world("foot_left", corners(BY_NAME["foot_left"]), o, dy)[:, 2].mean() + 4.0) ** 2)
    o = mirror({k: v for k, v in o.items() if "_left" in k}) | {k: v for k, v in o.items() if "_left" not in k and "_right" not in k}
    return dict(dy=dy, over=o, fit=(fs, hs))


def pose_report():
    for name, sol in (("SIT", solve_rest(False)), ("SLEEP", solve_rest(True))):
        over, dy = sol["over"], sol["dy"]
        lows = {n: low([n], over, dy) for n in BY_NAME if not BY_NAME[n][6].startswith("eye")}
        worst = min(lows.items(), key=lambda kv: kv[1])
        print(f"{name}: root drop {dy:+.2f}u, fit {sol['fit'][0]:.3f}/{sol['fit'][1]:.3f}; lowest part {worst[0]} {worst[1]:+.2f}"
              + ("  OK" if worst[1] > -0.06 else "  <== THROUGH THE GROUND"))
        for n in sorted(over):
            if "_right" not in n:
                print(f"    {n:<16} {tuple(round(float(x), 1) for x in over[n])}")


# ================================================================ gait (quadruped knuckle-walk) -- FITTED per limb
WALK_SPEED = 0.8
PHASE = {("hind", "left"): 0.0, ("fore", "left"): -math.pi / 2, ("hind", "right"): math.pi, ("fore", "right"): math.pi / 2}
LIMBS = {"fore": ["arm_upper", "arm_fore", "hand"], "hind": ["leg_thigh", "leg_shin", "foot"]}
END_H = {"fore": HAND[1], "hind": FOOT[1]}


def walk_terms(limb, side, fit):
    off = PHASE[(limb, side)]
    return [(f"{j}_{side}", fit[3 * k], off + fit[3 * k + 1], fit[3 * k + 2]) for k, j in enumerate(LIMBS[limb])]


def _cycle(limb, fit, steps=36):
    terms = walk_terms(limb, "left", fit)
    end = LIMBS[limb][2] + "_left"
    lows, zs, flat = [], [], []
    for t in np.linspace(0, 2 * math.pi, steps + 1)[:-1]:
        over = {}
        for n, deg, off, w in terms:
            d = deg * math.cos(t + off) + w
            cur = over.get(n, (0.0, 0, 0))
            over[n] = (cur[0] - math.degrees(d), 0, 0)
        e = world(end, corners(BY_NAME[end]), over)
        lows.append(min(e[:, 1].min(), *(world(n, corners(BY_NAME[n]), over)[:, 1].min() for n, *_ in terms)))
        zs.append(e[:, 2].mean())
        flat.append(e[:, 1].max() - e[:, 1].min() - END_H[limb])
    return np.array(lows), np.array(zs), np.array(flat)


def _score(limb, fit):
    lows, zs, flat = _cycle(limb, fit)
    st = lows < 0.35
    dz = np.diff(np.r_[zs, zs[0]])
    pen = 300 * np.sum(np.minimum(0, lows + 0.02) ** 2)
    pen += 20 * (st.mean() - 0.62) ** 2
    pen += 2.0 * np.sum(np.minimum(0, dz[st]) ** 2)
    pen += 0.05 * ((zs.max() - zs.min()) - 9.0) ** 2
    lift = lows.max()
    pen += 0.5 * max(0, 1.2 - lift) ** 2 + 0.5 * max(0, lift - 3.0) ** 2
    pen += 1.0 * np.sum(flat[st] ** 2) / max(1, st.sum())
    return pen


def walk_fit(limb, rounds=14, n=420, seed=3):
    rng = np.random.default_rng(seed)
    best = np.array([0.4, 0.0, 0.0, 0.35, -1.57, 0.3, -0.4, 0.0, 0.0])
    bs = _score(limb, best)
    span = np.array([0.3, 1.5, 0.3] * 3)
    for _ in range(rounds):
        for c in best + rng.normal(0, 1, (n, 9)) * span:
            c[0::3] = np.clip(c[0::3], -1.2, 1.2)
            s = _score(limb, c)
            if s < bs:
                bs, best = s, c.copy()
        span *= 0.65
    return best, bs


WALK_FILE = ROOT / "scratch/great_ape_walk.json"
WALK_FIT = json.loads(WALK_FILE.read_text()) if WALK_FILE.exists() else None


def walk_check(fits=None):
    fits = fits or WALK_FIT
    for limb in ("fore", "hind"):
        lows, zs, flat = _cycle(limb, fits[limb], 72)
        st = lows < 0.35
        print(f"WALK {limb}: lowest {lows.min():+.2f} (dig), lift {lows.max():.2f}, stride {zs.max() - zs.min():.1f}u, "
              f"planted {st.mean():.0%}, end tilt while planted <= {flat[st].max() if st.any() else 0:.2f}u")


# ================================================================ UV / export / checks
def footprint(p):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    w, h, d = (abs(b[i] - a[i]) for i in range(3))
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
    from contextlib import redirect_stdout
    with redirect_stdout(io.StringIO()):
        placed = uvpack()
    out = []
    for name, parent, o, r, a, b, cls in PARTS:
        u, v, _, _ = placed[name]
        out.append({"n": name, "p": parent, "o": [round(float(x), 4) for x in o], "r": [round(float(x), 4) for x in r],
                    "f": [round(float(x), 4) for x in a], "t": [round(float(x), 4) for x in b], "uv": [u, v], "c": cls})
    print(json.dumps({"canvas": list(CANVAS), "parts": out}, separators=(",", ":")))


def zfight():
    import zfight_sweep as Z
    parts = {}
    for name, parent, o, r, a, b, cls in PARTS:
        if cls.startswith("eye"):
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
    solids = [p for p in PARTS if not p[6].startswith("eye")]
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
        flag = "   <== FEATURE MOSTLY BURIED" if frac > 0.55 and cls in ("ear", "nose", "jaw", "hand", "foot") else ""
        print(f"  {frac:6.1%}  {name}{flag}")


def main():
    wc = {p[0]: world(p[0], corners(p)) for p in PARTS}
    lo = {k: v.min(0) for k, v in wc.items()}
    hi = {k: v.max(0) for k, v in wc.items()}
    floor = min(v[1] for v in lo.values())
    bt = back_top()
    print(f"shoulder joint {SH_Y:.2f}u, hip joint {HIP_Y:.2f}u, torso pitch {PITCH:.1f} deg; lowest point {floor:+.3f}"
          + ("  OK" if floor > -0.02 else "  <== FLOOR") + f"; back top {bt:.1f}u, head top {hi['head'][1]:.1f}u")
    print(f"  hands z {lo['hand_left'][2]:.1f}..{hi['hand_left'][2]:.1f}, feet z {lo['foot_left'][2]:.1f}..{hi['foot_left'][2]:.1f}, "
          f"face front z {min(lo['muzzle'][2], lo['nose'][2]):.1f}, rump z {hi['body_main'][2]:.1f}")
    for name, var, real in SPECIES:
        s = real * 16.0 / bt
        print(f"  {name:<18} shoulder {real:.2f} m -> scale {s:.3f}   length {(hi['body_main'][2] - min(lo['nose'][2], lo['muzzle'][2])) * s / 16:.2f} m")
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head", "brow", "muzzle"):
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
        fits = {}
        for limb in ("fore", "hind"):
            best = None
            for seed in (3, 11):
                f, s = walk_fit(limb, seed=seed)
                if best is None or s < best[1]:
                    best = (f, s)
            fits[limb] = [round(float(x), 4) for x in best[0]]
            print(f"{limb}: score {best[1]:.3f}")
        WALK_FILE.write_text(json.dumps(fits))
        walk_check(fits)
    elif "--walk" in sys.argv:
        walk_check()
    else:
        main()
