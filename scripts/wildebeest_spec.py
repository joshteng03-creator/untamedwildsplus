#!/usr/bin/env python3
"""Wildebeest (alcelaphine) model spec + FK checker -- SOP Phase 1 / 1b / 2 (Phase 4.7, user-approved 2026-10-23).

A NEW type `wildebeest`: blue wildebeest, black wildebeest, Megalotragus priscus, hartebeest. Its own rig, not the
antelope's gazelle frame: high heavy forequarters, a back that SLOPES down to a lower, narrower rump, a long thick neck,
a long boxy face, a long tasselled tail. Tooling forked from caprine_spec.py.

Blockbench space: Y up, ground y=0, -Z = nose, -X = left. World-angle convention (positive rx turns a part's -Z end UP /
a hanging part's lower end FORWARD / an UPWARD part's top BACKWARD).

FOUR HORN FAMILIES, authored by WORLD orientation (W = Rz(splay) . Rx(pitch), the caprine ram-curl lesson):
  gnu       cow-like: out sideways, down, then up and in                       blue_wildebeest
  gnou      hooks: forward and down beside the face, then up                    black_wildebeest
  megalo    very long, sweeping out, up and back                                megalotragus
  hart      a bony PEDICLE on the skull top, then bracket horns (up, forward, sharply back)   hartebeest
plus per-species: MANE (blue, black, megalo), BEARD (blue, black), CHEST FRINGE + NOSE TUFT (black).

Run:  python scripts/wildebeest_spec.py [--uv | --zfight | --burial | --json | --pose]
"""
import math
import numpy as np

D = math.radians

SPECIES = [
    # name,              variant, real shoulder height m, horn family, mane, beard, black-wildebeest extras
    ("blue_wildebeest",   0, 1.30, "gnu",    True,  True,  False),   # Connochaetes taurinus
    ("black_wildebeest",  1, 1.15, "gnou",   True,  True,  True),    # Connochaetes gnou
    ("megalotragus",      2, 1.50, "megalo", True,  False, False),   # Megalotragus priscus -- extinct, Late Pleistocene
    ("hartebeest",        3, 1.25, "hart",   False, False, False),   # Alcelaphus buselaphus
]
HITBOX = (1.2, 1.5)

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


def leg(prefix, parent, pivot_z, y_auth, segs, parent_tilt):
    for side, s in (("left", -1.0), ("right", 1.0)):
        par, prev, y = parent, parent_tilt, y_auth
        for name, ang, L, outer, inner, fr, bk, ov in segs:
            add(f"{prefix}_{side}_{name}", par, (s * (outer + inner) / 2, y, pivot_z), (ang - prev, 0, 0),
                (s * outer, y - L, pivot_z - fr), (s * inner, y + ov, pivot_z + bk), "leg")
            par, prev, y = f"{prefix}_{side}_{name}", ang, y - L


def world_rot(name):
    """Composite world rotation matrix of a part (root first)."""
    M = np.eye(3)
    for _, _, o, r, *_ in reversed(ancestry(name)):
        M = M @ rmat(*r)
    return M


def euler_zyx(M):
    """Decompose M = Rz(c) Ry(b) Rx(a) (Blockbench's order) into degrees (a, b, c)."""
    b = math.asin(max(-1.0, min(1.0, -M[2][0])))
    a = math.atan2(M[2][1], M[2][2])
    c = math.atan2(M[1][0], M[0][0])
    return math.degrees(a), math.degrees(b), math.degrees(c)


def horn_w(prefix, parent, base, segs):
    """Horn chain authored UPWARD (+Y) from `base`, every segment placed by its WORLD orientation W = Rz(+-splay) . Rx(pitch):
    pitch tilts the segment's top BACK (negative = forward), splay rolls it OUT from the midline (both sides mirrored).
    Local rotations are derived from the matrices (local = W_parent^-1 . W), so a pitched head cannot twist the chain.
    segs: (L, w, d, pitch_world, splay_world)."""
    bx, by, bz = base
    for side, s in (("left", -1.0), ("right", 1.0)):
        par, y = parent, by
        Wp = world_rot(parent)
        for k, (L, w, d, rx, rz) in enumerate(segs):
            n = f"{prefix}_{side}_{k + 1}"
            W = rmat(0, 0, -s * rz) @ rmat(rx, 0, 0)
            loc = euler_zyx(Wp.T @ W)
            d = d * (0.86 if k % 2 else 1.0)
            add(n, par, (s * bx, y, bz), loc, (s * bx - w / 2, y - 0.4, bz - d / 2), (s * bx + w / 2, y + L, bz + d / 2), "horn")
            par, Wp, y = n, W, y + L - 0.4


def build(hip_y, fore_y=None, hind_y=None):
    PARTS.clear()
    BY_NAME.clear()
    # ---- TORSO: barrel tilted nose-up (the sloping back), a deep high chest, a lower narrower rump ----
    add("body_barrel", None, (0, hip_y, 0.0), (7, 0, 0), (-4.3, hip_y - 3.7, -6.0), (4.3, hip_y + 3.6, 4.6), "body")
    add("body_chest", "body_barrel", (0, hip_y, -5.0), (-3, 0, 0), (-4.0, hip_y - 4.4, -11.0), (4.0, hip_y + 4.7, -3.0), "body")
    add("body_withers", "body_chest", (0, hip_y + 4.0, -6.5), (0, 0, 0), (-2.1, hip_y + 3.1, -10.0), (2.1, hip_y + 5.4, -3.8), "body")
    add("body_rump", "body_barrel", (0, hip_y, 4.0), (4, 0, 0), (-3.6, hip_y - 2.8, 3.4), (3.6, hip_y + 2.7, 8.8), "body")
    # ---- TAIL: hangs to the hocks, a long tassel of hair below a short dock ----
    ty = hip_y + 2.0
    add("tail", "body_rump", (0, ty, 9.4), (-14, 0, 0), (-0.6, ty - 5.2, 8.9), (0.6, ty + 0.4, 9.9), "tail")
    add("tail_tassel", "tail", (0, ty - 5.0, 9.4), (-4, 0, 0), (-1.0, ty - 9.8, 8.6), (1.0, ty - 4.6, 10.2), "tail")
    # ---- NECK + HEAD: thick neck carried forward and up, a long boxy face hanging steeply ----
    zchain("body_chest", (0, hip_y + 2.6, -10.0), [
        #  name        ang    L    hw   up   dn   ov
        ("neck_base",   18,  4.4, 2.5, 2.7, 3.0, 2.4),
        ("neck",        26,  4.0, 2.3, 2.2, 2.5, 1.4),
        ("head",       -40,  3.6, 2.0, 2.0, 2.0, 1.4),
        ("muzzle",     -58,  4.6, 1.6, 1.6, 1.7, 1.4),
    ], "head", parent_tilt=2.0)
    py = hip_y + 2.6
    # rest-frame anchors (about y = py): neck_base z -14.4..-10  neck -18.4..-14.4  head -22.0..-18.4  muzzle -26.6..-22.0
    add("nose", "muzzle", (0, py, -26.6), (0, 0, 0), (-1.9, py - 1.9, -27.2), (1.9, py + 1.0, -26.0), "nose")
    add("nose_tuft", "muzzle", (0, py + 1.6, -25.0), (-30, 0, 0), (-0.9, py + 1.2, -26.0), (0.9, py + 3.0, -24.2), "tuft")
    add("mane_base", "neck_base", (0, py + 2.8, -12.0), (0, 0, 0), (-0.45, py + 2.0, -14.2), (0.45, py + 4.4, -10.0), "mane")
    add("mane", "neck", (0, py + 2.3, -16.4), (0, 0, 0), (-0.3, py + 1.9, -18.2), (0.3, py + 3.7, -14.4), "mane")
    add("beard", "neck", (0, py - 2.7, -16.6), (-10, 0, 0), (-0.7, py - 5.4, -18.0), (0.7, py - 2.3, -14.8), "beard")
    add("chest_fringe", "body_chest", (0, hip_y - 4.4, -9.0), (0, 0, 0), (-1.4, hip_y - 6.4, -10.6), (1.4, hip_y - 4.0, -6.4), "fringe")
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"ear_{side}", "head", (s * 1.9, py + 1.0, -19.4), (0, s * -15, -s * 25),
            (s * 1.9, py + 0.6, -20.1), (s * 4.5, py + 1.4, -18.7), "ear")
        add(f"eye_{side}", "head", (s * 2.35, py + 0.5, -20.6), (0, 0, 0),
            (s * 2.35, py - 0.5, -21.6), (s * 2.35, py + 1.5, -19.6), "eye")
    # ---- HORNS: rest-frame skull top (head up = py + 2.0), just ahead of the ears ----
    hb = (1.2, py + 1.7, -19.6)
    horn_w("gnu", "head", hb, [(2.4, 1.6, 1.6, 10, 80), (2.4, 1.3, 1.3, 15, 125), (2.2, 1.0, 1.0, 20, 55),
                               (2.0, 0.7, 0.7, 20, -10)])
    horn_w("gnou", "head", (1.6, hb[1], hb[2]), [(2.2, 1.6, 1.6, -125, -50), (2.0, 1.3, 1.3, -95, -40), (2.0, 0.85, 1.0, -40, 40),
                                (1.6, 0.7, 0.7, 0, 20)])
    horn_w("megalo", "head", hb, [(3.0, 1.7, 1.7, 30, 55), (3.0, 1.4, 1.4, 50, 67), (2.8, 1.1, 1.1, 65, 55),
                                  (2.4, 0.8, 0.8, 70, 25)])
    add("hart_pedicle", "head", (0, py + 1.6, -19.8), (35, 0, 0), (-1.1, py + 1.2, -20.5), (1.1, py + 3.8, -19.1), "horn")
    horn_w("hart", "hart_pedicle", (0.7, py + 3.7, -19.8), [(2.0, 1.0, 1.0, -5, 45), (1.9, 0.85, 0.85, -30, 18),
                                                           (2.2, 0.65, 0.65, 100, 10)])
    # ---- LEGS: long and slender, four segments, outer/inner planes stepping inboard ----
    leg("fore", "body_chest", -7.0, fore_y if fore_y is not None else hip_y - 1.0, [
        #  name        ang   L    outer inner  fr   bk   ov
        ("upper",        8, 4.0, 4.5, 1.8, 2.0, 1.9, 1.8),
        ("forearm",     -4, 4.3, 3.6, 2.1, 1.3, 1.2, 0.9),
        ("cannon",       2, 3.8, 3.2, 2.4, 0.8, 0.8, 0.8),
        ("hoof",         0, 1.2, 3.4, 2.2, 1.2, 0.5, 0.5),
    ], 4.0)
    leg("hind", "body_rump", 6.6, hind_y if hind_y is not None else hip_y - 1.0, [
        ("thigh",       18, 4.2, 4.7, 1.6, 2.2, 1.7, 1.4),
        ("gaskin",     -26, 4.2, 3.4, 1.9, 1.3, 1.3, 1.0),
        ("cannon",       8, 4.0, 3.1, 2.3, 0.8, 0.8, 0.8),
        ("hoof",         0, 1.2, 3.3, 2.1, 1.2, 0.5, 0.5),
    ], 11.0)


def solve():
    """The body pivot is fixed; each leg pair's authored pivot height is solved so its hoof soles land on y=0."""
    y, fy, hy = 13.2, 12.0, 12.0
    for _ in range(40):
        build(y, fy, hy)
        fy -= lowest(["fore_left_hoof"])
        hy -= lowest(["hind_left_hoof"])
    build(y, fy, hy)
    return y, fy, hy


HIP_Y, FORE_Y, HIND_Y = solve()

CANVAS = (256, 128)
FAMILY = {"gnu": "gnu", "gnou": "gnou", "megalo": "megalo", "hart": "hart"}


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


def zfight(family=None):
    """Sweep the body + ONE horn family at a time (the families are never shown together)."""
    import zfight_sweep as Z
    fams = [family] if family else list(FAMILY.values())
    for fam in fams:
        parts = {}
        for name, parent, o, r, a, b, cls in PARTS:
            if cls == "eye" or (cls == "horn" and not name.startswith(fam + "_")):
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
            print(f"  [{fam}] {area:6.2f}u2  {na:<18} {'XYZ'[fa[0]]}  <->  {nb:<18} {'XYZ'[fb[0]]}{rel}")
        print(f"  [{fam}] z-fight: " + ("clean" if not shown else f"{shown} pair(s)"))


def burial():
    solids = [p for p in PARTS if p[6] != "eye"]
    out = []
    for p in solids:
        pts = world(p[0], vol(p, 9))
        inside = np.zeros(len(pts), bool)
        for q in solids:
            if q[0] == p[0] or (q[6] == "horn" and p[6] == "horn" and q[0].split("_")[0] != p[0].split("_")[0]):
                continue
            l = local_of(q[0], pts)
            a, b = np.asarray(q[4], float), np.asarray(q[5], float)
            inside |= np.all((l >= a - 1e-6) & (l <= b + 1e-6), axis=1)
        out.append((inside.mean(), p[0], p[6]))
    for frac, name, cls in sorted(out, reverse=True)[:18]:
        flag = "   <== FEATURE MOSTLY BURIED" if frac > 0.55 and cls in ("horn", "beard", "ear", "nose", "tail", "mane",
                                                                          "tuft", "fringe") else ""
        print(f"  {frac:6.1%}  {name}{flag}")


def main():
    wc = {p[0]: world(p[0], corners(p)) for p in PARTS}
    lo = {k: v.min(0) for k, v in wc.items()}
    hi = {k: v.max(0) for k, v in wc.items()}
    floor = min(v[1] for v in lo.values())
    withers = hi["body_withers"][1]
    print(f"hip pivot {HIP_Y:.2f}u (fore {FORE_Y:.2f} hind {HIND_Y:.2f}); lowest {floor:+.3f}" + ("  OK" if floor > -0.02 else "  <== FLOOR"))
    for n in ("fore_left_hoof", "hind_left_hoof"):
        print(f"  {n:<16} sole {lo[n][1]:+.3f}")
    print(f"  withers {withers:.2f}u  croup {hi['body_rump'][1]:.2f}u  belly {lo['body_barrel'][1]:.2f}u "
          f"({lo['body_barrel'][1] / withers:.0%})  head top {hi['head'][1]:.2f}u  nose y {lo['nose'][1]:.2f} z {lo['nose'][2]:.1f}  "
          f"rump z {hi['body_rump'][2]:.1f}  tail tip y {lo['tail_tassel'][1]:.2f}  hock y {lo['hind_left_gaskin'][1]:.2f}")
    print(f"  length nose->rump {hi['body_rump'][2] - lo['nose'][2]:.1f}u  body {hi['body_rump'][2] - lo['body_chest'][2]:.1f}u")
    for fam in FAMILY.values():
        hs = [n for n in wc if n.startswith(fam + "_left") or n == fam + "_pedicle"]
        top = max(hi[n][1] for n in hs)
        back = max(hi[n][2] for n in hs)
        front = min(lo[n][2] for n in hs)
        low = min(lo[n][1] for n in hs)
        out = min(lo[n][0] for n in hs)
        print(f"  horns {fam:<7} y {low:5.2f}..{top:5.2f}  z {front:5.1f}..{back:5.1f}  outermost x {out:5.2f}")
    for name, var, real, fam, mane, beard, extra in SPECIES:
        s = real * 16.0 / withers
        print(f"  {name:<17} {real:.2f} m -> scale {s:.3f}   horns {fam}")
    for side in ("left", "right"):
        c = wc[f"eye_{side}"].mean(0)
        for anc in ("head", "neck", "muzzle", f"ear_{side}"):
            p = BY_NAME[anc]
            l = local_of(anc, c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gap = max(max(a[i] - l[i], l[i] - b[i]) for i in range(3))
            if gap < 0.3:
                print(f"  eye_{side} vs {anc} clear by {gap:.2f}  <== TOO CLOSE")
    tot = sum(fw * fh for fw, fh in (footprint(p) for p in PARTS))
    print(f"  {len(PARTS)} boxes, UV {tot} px = {100 * tot / (CANVAS[0] * CANVAS[1]):.0f}% of 256x128")


# ================================================================ phase 7 poses (sternal recumbency)
def world_mod(name, pts, rot_over=None, dy=0.0):
    p = np.asarray(pts, float).reshape(-1, 3)
    for nm, _, o, r, *_ in ancestry(name):
        r = (rot_over or {}).get(nm, r)
        o = np.asarray(o, float)
        p = (rmat(*r) @ (p - o).T).T + o
    p = p.copy()
    p[:, 1] += dy
    return p


LEGS = {"fore": ("body_chest", 4.0, ["upper", "forearm", "cannon", "hoof"]),
        "hind": ("body_rump", 11.0, ["thigh", "gaskin", "cannon", "hoof"])}


def _leg_rot(prefix, side, angles):
    _, tilt, segs = LEGS[prefix]
    over, prev = {}, tilt
    for nm, a in zip(segs, angles):
        over[f"{prefix}_{side}_{nm}"] = (a - prev, 0.0, 0.0)
        prev = a
    return over


def _leg_score(prefix, angles, dy):
    over = _leg_rot(prefix, "left", angles)
    pen = 0.0
    for k, nm in enumerate(LEGS[prefix][2]):
        full = f"{prefix}_left_{nm}"
        low = world_mod(full, vol(BY_NAME[full]), over, dy)[:, 1].min()
        if low < 0:
            pen += low * low * 12
        if k >= 2:
            pen += (low - 0.12) ** 2 * 3
    for a in angles[2:]:
        pen += (math.cos(D(a)) ** 2) * 4
    return pen


def pose_solve(verbose=True):
    rng = np.random.default_rng(7)
    body_lo = world("body_barrel", corners(BY_NAME["body_barrel"]))[:, 1].min()
    out = {}
    for pose, belly in (("sit", 1.4), ("sleep", 0.6)):
        dy = belly - body_lo
        for prefix in ("fore", "hind"):
            best = np.array([40.0, -70.0, 95.0, 95.0] if prefix == "fore" else [70.0, -95.0, 80.0, 80.0])
            bs = _leg_score(prefix, best, dy)
            span = np.array([55.0, 55.0, 45.0, 45.0])
            for _ in range(9):
                cand = best + rng.normal(0, 1, (1500, 4)) * span
                cand[:, 3] = cand[:, 2] + rng.normal(0, 6, 1500)
                for c in cand:
                    sc = _leg_score(prefix, c, dy)
                    if sc < bs:
                        bs, best = sc, c.copy()
                span *= 0.62
            out[(pose, prefix)] = (best, dy)
            if verbose:
                print(f"{pose:<5} {prefix:<4} dy {dy:+.2f}  angles " + " ".join(f"{a:7.1f}" for a in best) + f"  pen {bs:.4f}")
        over = {}
        for prefix in ("fore", "hind"):
            for side in ("left", "right"):
                over.update(_leg_rot(prefix, side, out[(pose, prefix)][0]))
        # tail: DRAPED, not swung out -- a lying wildebeest's tail falls to one side and the tassel rests on the ground.
        # Search a backward swing (tl) and a sideways roll (rz) for the least bend that keeps every tail point above
        # the floor while the tassel still lies ON it. (v1 swung it straight back until it cleared: a stiff pole.)
        trest = BY_NAME["tail"][3]
        best = None
        for tl in range(0, 61, 3):
            for rz in range(0, 101, 5):
                over["tail"] = (trest[0] - tl, 0, rz)
                lo_t = world_mod("tail", vol(BY_NAME["tail"]), over, dy)[:, 1].min()
                lo_s = world_mod("tail_tassel", vol(BY_NAME["tail_tassel"]), over, dy)[:, 1].min()
                if min(lo_t, lo_s) < 0.05 or lo_s > 0.8:
                    continue
                cost = tl + 0.3 * rz
                if best is None or cost < best[0]:
                    best = (cost, tl, rz)
        tl, rz = (best[1], best[2]) if best else (54, 0)
        over["tail"] = (trest[0] - tl, 0, rz)
        out[(pose, "tail")] = (tl, rz)
        nrest = BY_NAME["neck_base"][3]
        heads = [q[0] for q in PARTS if q[6] in ("head", "horn", "beard", "nose", "ear", "mane", "tuft")]
        lift = 0.0
        for lift in range(0, 60, 2):
            over["neck_base"] = (nrest[0] + lift, 0, 0)
            if min(world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min() for n in heads) >= 0.15:
                break
        out[(pose, "neck")] = (lift, dy)
        if verbose:
            print(f"{pose:<5} neck lift {lift} deg, tail back {out[(pose, 'tail')][0]} deg roll {out[(pose, 'tail')][1]} deg")
    return out


HEAD_PARTS = None


def _head_parts():
    return [q[0] for q in PARTS if q[6] in ("head", "horn", "beard", "nose", "ear", "mane", "tuft")]


def graze_solve(verbose=True):
    """GRAZE: extra neck_base / neck / head rotations (BB degrees, negative = lower the -Z end) that put the nose's lowest
    corner 0.2..0.7 above the floor with every head part -- the lowest-hanging horn family included -- clear of it.
    The smallest total bend wins (the neck does most of the work, as in a real grazing antelope)."""
    nb0, n0, h0 = (BY_NAME[k][3][0] for k in ("neck_base", "neck", "head"))
    heads = _head_parts()
    best = None
    for a in range(0, -92, -2):
        for b in range(0, -64, -3):
            over = {"neck_base": (nb0 + a, 0, 0), "neck": (n0 + b, 0, 0)}
            for c in range(-30, 34, 4):
                over["head"] = (h0 + c, 0, 0)
                nose = world_mod("nose", corners(BY_NAME["nose"]), over)[:, 1].min()
                if not 0.2 <= nose <= 0.7:
                    continue
                if min(world_mod(n, corners(BY_NAME[n]), over)[:, 1].min() for n in heads) < 0.1:
                    continue
                cost = 0.6 * a * a + b * b + 1.5 * c * c
                if best is None or cost < best[0]:
                    best = (cost, a, b, c, nose)
    if verbose:
        print(f"graze  neck_base {best[1]:+d}  neck {best[2]:+d}  head {best[3]:+d}  nose low {best[4]:.2f}")
    return best[1:4]


def _leg_rot_tilt(prefix, side, angles, extra):
    _, tilt, segs = LEGS[prefix]
    over, prev = {}, tilt + extra
    for nm, a in zip(segs, angles):
        over[f"{prefix}_{side}_{nm}"] = (a - prev, 0.0, 0.0)
        prev = a
    return over


def kneel_solve(verbose=True, pitch=12.0):
    """CLASH: the alcelaphine fighting pose -- the bull drops onto its CARPAL joints (knees) with the forearms near vertical
    and the cannons folded back along the ground, the hindquarters standing. The barrel pitches nose-down by `pitch` and
    drops by `drop`; the fore legs are solved for knee (forearm bottom) height 0.3 with the cannon and hoof lying flat
    above the floor, the hind legs for hoof soles on the floor, standing upright."""
    rng = np.random.default_rng(11)
    b0 = BY_NAME["body_barrel"][3][0]

    def score(prefix, ang, drop):
        over = {"body_barrel": (b0 - pitch, 0, 0)}
        over.update(_leg_rot_tilt(prefix, "left", ang, -pitch))
        pen = 0.0
        segs = LEGS[prefix][2]
        for k, nm in enumerate(segs):
            full = f"{prefix}_left_{nm}"
            low = world_mod(full, vol(BY_NAME[full], 5), over, -drop)[:, 1].min()
            if low < 0:
                pen += low * low * 12
            if prefix == "fore" and k == 1:
                pen += (low - 0.3) ** 2 * 10                     # the knee rests on the ground
            if prefix == "fore" and k >= 2:
                pen += (low - 0.15) ** 2 * 2 + math.cos(D(ang[k])) ** 2 * 3   # cannon + hoof folded back flat
            if prefix == "hind" and k == 3:
                pen += low * low * 10 + math.sin(D(ang[k])) ** 2 * 2         # hoof flat on the floor
        if prefix == "fore":
            pen += math.sin(D(ang[1])) ** 2 * 1.5                          # forearm near vertical
        return pen

    best_all = None
    for drop in np.arange(2.0, 7.01, 0.5):
        res = {}
        tot = 0.0
        for prefix in ("fore", "hind"):
            best = np.array([20.0, 0.0, 95.0, 95.0] if prefix == "fore" else [30.0, -20.0, 10.0, 0.0])
            bs = score(prefix, best, drop)
            span = np.array([40.0, 40.0, 40.0, 40.0])
            for _ in range(7):
                cand = best + rng.normal(0, 1, (700, 4)) * span
                for cc in cand:
                    sc = score(prefix, cc, drop)
                    if sc < bs:
                        bs, best = sc, cc.copy()
                span *= 0.6
            res[prefix] = (best, bs)
            tot += bs
        if best_all is None or tot < best_all[0]:
            best_all = (tot, drop, res)
    tot, drop, res = best_all
    if verbose:
        for prefix in ("fore", "hind"):
            print(f"kneel {prefix:<4} drop {drop:.1f} pitch {pitch:.0f}  angles " + " ".join(f"{a:7.1f}" for a in res[prefix][0])
                  + f"  pen {res[prefix][1]:.4f}")
    # horns forward: lower the neck until the horn tips sit ahead of the muzzle at clash height, head parts clear
    over = {"body_barrel": (b0 - pitch, 0, 0)}
    for prefix in ("fore", "hind"):
        for side in ("left", "right"):
            over.update(_leg_rot_tilt(prefix, side, res[prefix][0], -pitch))
    nb0 = BY_NAME["neck_base"][3][0]
    lower = 0
    for lower in range(0, 50, 2):
        over["neck_base"] = (nb0 - lower, 0, 0)
        nose = world_mod("nose", corners(BY_NAME["nose"]), over, -drop)[:, 1].min()
        if nose < 3.0:
            break
    heads = _head_parts()
    floor = min(world_mod(n, corners(BY_NAME[n]), over, -drop)[:, 1].min() for n in heads)
    if verbose:
        print(f"kneel neck lowered {lower} deg, nose {nose:.2f}, lowest head part {floor:.2f}")
    return {"drop": drop, "pitch": pitch, "fore": res["fore"][0], "hind": res["hind"][0], "neck": lower}


if __name__ == "__main__":
    import sys
    if "--graze" in sys.argv:
        graze_solve()
        sys.exit()
    if "--kneel" in sys.argv:
        kneel_solve()
        sys.exit()
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
