#!/usr/bin/env python3
"""Large rodent model spec + FK checker -- SOP Phase 1 / 1b / 2 (Phase 4.6, user-approved 2026-10-10).

A NEW semi-aquatic type `large_rodent`: capybara, Neochoerus (giant Pleistocene capybara), Castoroides (giant beaver) and
the North American beaver. Tooling forked from caprine_spec.py.

Blockbench space: Y up, ground y=0, -Z = nose, -X = left; positive rx turns a part's -Z end UP / a hanging part's lower
end FORWARD.

One mesh: a heavy barrel body on short legs (the hind longer), a long, blunt, boxy head with small high ears, high-set
eyes and orange incisors. Per species: the beavers' flat scaly PADDLE TAIL; per entity (runtime): the BIRD passenger
perched on a resting capybara's back.

Run:  python scripts/large_rodent_spec.py [--uv | --zfight | --burial | --json | --pose]
"""
import math
import numpy as np

D = math.radians

SPECIES = [
    # name,           variant, real shoulder height m, paddle tail
    ("capybara",        0, 0.58, False),   # Hydrochoerus hydrochaeris
    ("neochoerus",      1, 0.85, False),   # Neochoerus pinckneyi -- giant Pleistocene capybara
    ("castoroides",     2, 0.75, True),    # Castoroides ohioensis -- bear-sized giant beaver
    ("beaver",          3, 0.32, True),    # Castor canadensis -- North American beaver
]
HITBOX = (1.0, 0.9)

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


def build(hip_y, fore_y=None, hind_y=None, tail_droop=14.0):
    PARTS.clear()
    BY_NAME.clear()
    # ---- TORSO: a heavy barrel, a slightly narrower chest, a high rounded rump ----
    add("body_barrel", None, (0, hip_y, 2.0), (0, 0, 0), (-4.2, hip_y - 3.8, -5.5), (4.2, hip_y + 3.8, 8.5), "body")
    add("body_chest", "body_barrel", (0, hip_y, -4.0), (4, 0, 0), (-3.6, hip_y - 3.4, -9.0), (3.6, hip_y + 3.2, -4.0), "body")
    add("body_rump", "body_barrel", (0, hip_y, 7.5), (-4, 0, 0), (-3.9, hip_y - 3.2, 7.5), (3.9, hip_y + 4.4, 12.0), "body")
    # ---- HEAD: short thick neck, a long blunt boxy head, a blunt muzzle ----
    zchain("body_chest", (0, hip_y + 1.9, -8.5), [
        #  name      ang    L    hw   up   dn   ov
        ("neck",      10,  2.5, 3.0, 2.2, 2.8, 2.0),
        ("head",     -12,  5.6, 2.7, 2.4, 2.6, 1.4),
        ("muzzle",   -14,  2.0, 2.3, 1.9, 2.2, 1.0),
    ], "head", parent_tilt=4.0)
    hy = hip_y + 1.9
    # rest-frame anchors (about y = hy): neck z -11..-8.5  head -16.6..-11  muzzle -18.6..-16.6
    add("nose", "muzzle", (0, hy + 0.6, -18.6), (0, 0, 0), (-1.6, hy - 0.2, -19.0), (1.6, hy + 1.5, -18.3), "nose")
    add("incisors", "muzzle", (0, hy - 1.6, -18.4), (0, 0, 0), (-0.8, hy - 2.6, -18.8), (0.8, hy - 1.2, -18.0), "tooth")
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"ear_{side}", "head", (s * 2.0, hy + 2.3, -11.8), (0, 0, -s * 15),
            (s * 1.3, hy + 2.0, -12.4), (s * 2.7, hy + 3.3, -11.6), "ear")
        add(f"eye_{side}", "head", (s * 3.05, hy + 1.2, -13.4), (0, 0, 0),
            (s * 3.05, hy + 0.2, -14.4), (s * 3.05, hy + 2.2, -12.4), "eye")
    # ---- beaver tail: carried LEVEL behind the lower rump, the way a walking beaver holds it -- a thick short furred
    # ROOT, then a BROAD oval paddle nearly as wide as the rump. (v1 a steep plank to the ground; v2 a thin stick
    # angled down to the ground behind a tall animal -- both "look weird", user.) ----
    ty = hip_y + 0.4   # mid-rump: the tail continues from the back line (v3 at the rump base was "still too low", user)
    add("tail_base", "body_rump", (0, ty, 11.6), (10.0 + tail_droop * 0.0, 0, 0), (-1.3, ty - 0.9, 11.4), (1.3, ty + 0.8, 14.2), "tail")
    add("tail_paddle", "tail_base", (0, ty, 13.6), (2.0, 0, 0), (-2.6, ty - 0.55, 13.0), (2.6, ty + 0.55, 19.4), "tail")
    # ---- BIRD passenger: a small perched bird on the back (a cattle tyrant / jacana), runtime-gated ----
    add("bird_body", "body_barrel", (0, hip_y + 3.8, 2.4), (0, 0, 0), (-0.8, hip_y + 3.5, 1.2), (0.8, hip_y + 5.0, 3.8), "bird")
    add("bird_head", "bird_body", (0, hip_y + 5.0, 1.4), (0, 0, 0), (-0.55, hip_y + 4.7, 0.6), (0.55, hip_y + 5.8, 1.7), "bird")
    add("bird_beak", "bird_head", (0, hip_y + 5.2, 0.6), (0, 0, 0), (-0.2, hip_y + 5.05, -0.1), (0.2, hip_y + 5.35, 0.7), "bird")
    add("bird_tail", "bird_body", (0, hip_y + 4.4, 3.8), (-20, 0, 0), (-0.5, hip_y + 4.2, 3.6), (0.5, hip_y + 4.5, 5.6), "bird")
    # ---- LEGS: short and stout, the hind longer; broad flat feet (webbed on the beavers, in the skin) ----
    leg("fore", "body_chest", -6.5, fore_y if fore_y is not None else hip_y - 2.0, [
        #  name        ang   L    outer inner  fr   bk   ov
        ("upper",        8, 2.6, 4.5, 1.8, 1.6, 1.5, 1.6),   # outer 4.5: at 4.2 it was flush with the barrel
        ("lower",       -6, 2.2, 3.6, 2.2, 1.1, 1.0, 0.8),
        ("foot",         0, 0.9, 3.9, 2.0, 2.0, 0.8, 0.5),
    ], 4.0)
    leg("hind", "body_rump", 10.0, hind_y if hind_y is not None else hip_y - 2.0, [
        ("thigh",       25, 3.0, 4.5, 1.7, 2.0, 2.0, 1.8),
        ("shank",      -30, 2.6, 3.7, 2.1, 1.1, 1.1, 0.9),
        ("foot",         0, 0.9, 4.0, 1.8, 3.0, 0.8, 0.5),
    ], -4.0)


def solve():
    y, fy, hy = 8.0, 6.0, 6.0
    for _ in range(40):
        build(y, fy, hy)
        fy -= lowest(["fore_left_foot"])
        hy -= lowest(["hind_left_foot"])
    return y, fy, hy, 0.0


HIP_Y, FORE_Y, HIND_Y, TAIL_DROOP = solve()

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
        print(f"  {area:6.2f}u2  {na:<18} {'XYZ'[fa[0]]}  <->  {nb:<18} {'XYZ'[fb[0]]}{rel}")
    print("  z-fight: " + ("clean" if not shown else f"{shown} pair(s)"))


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
    for frac, name, cls in sorted(out, reverse=True)[:14]:
        flag = "   <== FEATURE MOSTLY BURIED" if frac > 0.55 and cls in ("tooth", "ear", "nose", "tail", "bird") else ""
        print(f"  {frac:6.1%}  {name}{flag}")


def main():
    wc = {p[0]: world(p[0], corners(p)) for p in PARTS}
    lo = {k: v.min(0) for k, v in wc.items()}
    hi = {k: v.max(0) for k, v in wc.items()}
    floor = min(v[1] for v in lo.values())
    wp = BY_NAME["body_chest"]
    withers = world("body_chest", [(0.0, wp[5][1], -6.5)])[0][1]
    print(f"hip pivot {HIP_Y:.2f}u; lowest {floor:+.3f}" + ("  OK" if floor > -0.02 else "  <== FLOOR"))
    for n in ("fore_left_foot", "hind_left_foot", "tail_base", "tail_paddle"):
        print(f"  {n:<16} lowest {lo[n][1]:+.3f}")
    print(f"  withers {withers:.2f}u  rump top {hi['body_rump'][1]:.2f}u  belly {lo['body_barrel'][1]:.2f}u "
          f"({lo['body_barrel'][1] / withers:.0%})  head top {hi['head'][1]:.2f}u  nose z {lo['nose'][2]:.1f}  "
          f"tail tip z {hi['tail_paddle'][2]:.1f}")
    for name, var, real, paddle in SPECIES:
        s = real * 16.0 / withers
        print(f"  {name:<12} {real:.2f} m -> scale {s:.3f}{'   paddle tail' if paddle else ''}")
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


LEGS = {"fore": ("body_chest", 4.0, ["upper", "lower", "foot"]),
        "hind": ("body_rump", -4.0, ["thigh", "shank", "foot"])}


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
        if k >= 1:
            pen += (low - 0.12) ** 2 * 3
    for a in angles[1:]:
        pen += (math.cos(D(a)) ** 2) * 4
    return pen


def pose_solve(verbose=True):
    rng = np.random.default_rng(7)
    body_lo = world("body_barrel", corners(BY_NAME["body_barrel"]))[:, 1].min()
    out = {}
    for pose, belly in (("sit", 1.0), ("sleep", 0.4)):
        dy = belly - body_lo
        for prefix in ("fore", "hind"):
            best = np.array([60.0, 95.0, 90.0] if prefix == "fore" else [80.0, -95.0, -90.0])
            bs = _leg_score(prefix, best, dy)
            span = np.array([45.0, 45.0, 30.0])
            for _ in range(9):
                cand = best + rng.normal(0, 1, (1500, 3)) * span
                for c in cand:
                    sc = _leg_score(prefix, c, dy)
                    if sc < bs:
                        bs, best = sc, c.copy()
                span *= 0.62
            out[(pose, prefix)] = (best, dy)
            if verbose:
                print(f"{pose:<5} {prefix:<4} dy {dy:+.2f}  angles " + " ".join(f"{a:7.1f}" for a in best) + f"  pen {bs:.4f}")
        # neck: lift until every head part (incl. the lowest-hanging horn family and the beard) clears the floor
        over = {}
        for prefix in ("fore", "hind"):
            for side in ("left", "right"):
                over.update(_leg_rot(prefix, side, out[(pose, prefix)][0]))
        nrest = BY_NAME["neck"][3]
        heads = [q[0] for q in PARTS if q[6] in ("head", "tooth", "nose", "ear")]
        lift = 0.0
        for lift in range(0, 60, 2):
            over["neck"] = (nrest[0] + lift, 0, 0)
            if min(world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min() for n in heads) >= 0.15:
                break
        out[(pose, "neck")] = (lift, dy)
        if verbose:
            print(f"{pose:<5} neck lift {lift} deg")
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
