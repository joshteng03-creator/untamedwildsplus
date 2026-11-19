#!/usr/bin/env python3
"""Caprine (wild goats + sheep) model spec + FK checker -- SOP Phase 1 / 1b / 2 (Phase 4.5, user-approved 2026-10-10).

A NEW type `caprine`: Alpine ibex, Siberian ibex, argali, bighorn sheep, chamois, Harrington's mountain goat. Its own
rig, not the antelope's: the antelope is a slender gazelle frame, caprines are stocky, deep-chested, short-legged
climbers. Tooling forked from tapir_spec.py / diprotodont_spec.py.

Blockbench space: Y up, ground y=0, -Z = nose, -X = left. World-angle convention (positive rx turns a part's -Z end UP /
a hanging part's lower end FORWARD / an UPWARD part's top BACKWARD).

FOUR HORN FAMILIES, each its own chain of tapering segments on both sides of the skull, shown per species:
  ibex      long backswept scimitar (4 segments sweeping 25 -> 85 deg back)      alpine_ibex, siberian_ibex
  ram       thick spiral curl back, down and forward beside the face            argali, bighorn
  chamois   short vertical spike ending in a backward hook                       chamois
  daggers   short, slightly backswept black daggers                              harrington_goat
plus a BEARD (ibex + mountain goat).

Run:  python scripts/caprine_spec.py [--uv | --zfight | --burial | --json]
"""
import math
import numpy as np

D = math.radians

SPECIES = [
    # name,              variant, real shoulder height m, horn family, beard
    ("alpine_ibex",       0, 0.90, "ibex",    True),    # Capra ibex
    ("siberian_ibex",     1, 1.00, "ibex",    True),    # Capra sibirica
    ("argali",            2, 1.15, "ram",     False),   # Ovis ammon -- the largest wild sheep
    ("bighorn",           3, 0.95, "ram",     False),   # Ovis canadensis
    ("chamois",           4, 0.75, "chamois", False),   # Rupicapra rupicapra
    ("harrington_goat",   5, 0.80, "daggers", True),    # Oreamnos harringtoni -- extinct, Pleistocene
]
HITBOX = (1.0, 1.3)

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


def horn(prefix, parent, base, segs, parent_tilt, spread):
    """Horn chain authored UPWARD (+Y) from `base`; each segment's world rx tilts its top BACK, its world rz splays it
    OUT (positive = away from the midline on both sides). segs: (L, w, d, rx_world, rz_world)."""
    bx, by, bz = base
    for side, s in (("left", -1.0), ("right", 1.0)):
        par, prx, prz, y = parent, parent_tilt, 0.0, by
        for k, (L, w, d, rx, rz) in enumerate(segs):
            n = f"{prefix}_{side}_{k + 1}"
            zz = -s * (rz + spread)
            add(n, par, (s * bx, y, bz), (rx - prx, 0, zz - prz),
                (s * bx - w / 2, y - 0.4, bz - d / 2), (s * bx + w / 2, y + L, bz + d / 2), "horn")
            par, prx, prz, y = n, rx, zz, y + L - 0.4


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


def horn_world(prefix, parent, base, segs, root_tilt):
    """Horn chain authored by WORLD ORIENTATION: segment k's world rotation is Rx(rx_k) -- a curl in a VERTICAL plane --
    except the root, Rz(+-root_tilt) . Rx(rx_0), which carries the plane OUT beside the skull. Local rotations are
    derived from the matrices (local = W_parent^-1 . W_k), so the euler-composition cross-talk that swung the tip
    back into the neck cannot happen. segs: (L, w, d, rx_world)."""
    bx, by, bz = base
    for side, s in (("left", -1.0), ("right", 1.0)):
        par, y = parent, by
        Wp = world_rot(parent)
        for k, (L, w, d, rx) in enumerate(segs):
            n = f"{prefix}_{side}_{k + 1}"
            W = rmat(rx, 0, 0)
            if k == 0:
                W = rmat(0, 0, -s * root_tilt) @ W
            loc = euler_zyx(Wp.T @ W)
            add(n, par, (s * bx, y, bz), loc, (s * bx - w / 2, y - 0.4, bz - d / 2), (s * bx + w / 2, y + L, bz + d / 2), "horn")
            par, Wp, y = n, W, y + L - 0.4


def build(hip_y, fore_y=None, hind_y=None):
    PARTS.clear()
    BY_NAME.clear()
    # ---- TORSO: deep chest, barrel, rump; nested, side planes stepping in from the barrel ----
    add("body_barrel", None, (0, hip_y, 2.0), (0, 0, 0), (-4.2, hip_y - 3.6, -6.0), (4.2, hip_y + 3.6, 9.0), "body")
    add("body_chest", "body_barrel", (0, hip_y, -4.0), (6, 0, 0), (-3.8, hip_y - 4.2, -10.0), (3.8, hip_y + 4.4, -3.0), "body")
    add("body_rump", "body_barrel", (0, hip_y, 7.0), (8, 0, 0), (-3.9, hip_y - 2.8, 7.0), (3.9, hip_y + 3.0, 12.6), "body")
    add("tail", "body_rump", (0, hip_y + 2.6, 12.4), (-35, 0, 0), (-0.9, hip_y + 0.8, 11.9), (0.9, hip_y + 3.0, 14.2), "tail")
    # ---- NECK + HEAD: short thick neck carried UP, head angled down to a short muzzle ----
    zchain("body_chest", (0, hip_y + 2.6, -9.0), [
        #  name      ang    L    hw   up   dn   ov
        ("neck",      38,  4.2, 2.3, 2.1, 2.5, 2.2),
        ("head",     -28,  3.8, 2.0, 1.9, 1.7, 1.4),
        ("muzzle",   -40,  2.6, 1.4, 1.2, 1.3, 1.0),
    ], "head", parent_tilt=6.0)
    hy = hip_y + 2.6
    # rest-frame anchors (about y = hy): neck z -13.2..-9  head -17.0..-13.2  muzzle -19.6..-17.0
    add("nose", "muzzle", (0, hy, -19.6), (0, 0, 0), (-1.0, hy - 0.6, -20.0), (1.0, hy + 0.9, -19.3), "nose")
    add("beard", "muzzle", (0, hy - 1.2, -18.0), (-15, 0, 0), (-0.7, hy - 3.6, -18.6), (0.7, hy - 1.0, -17.4), "beard")
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"ear_{side}", "head", (s * 1.9, hy + 1.2, -14.0), (0, s * -10, -s * 60),
            (s * 1.9, hy + 0.8, -14.6), (s * 4.3, hy + 1.6, -13.4), "ear")
        add(f"eye_{side}", "head", (s * 2.35, hy + 0.6, -15.2), (0, 0, 0),
            (s * 2.35, hy - 0.4, -16.2), (s * 2.35, hy + 1.6, -14.2), "eye")
    # ---- HORNS: four families on the skull top, rest-frame base (+-0.9, hy+1.6, -14.6) ----
    base = (0.9, hy + 1.5, -14.8)
    head_w = -28.0
    horn("ibex", "head", base, [(3.2, 1.3, 1.6, -10, 0), (3.0, 1.1, 1.4, 15, 9), (2.8, 0.9, 1.2, 40, 18),
                                (2.4, 0.7, 0.9, 62, 27)], head_w, 2)
    # RAM CURL: ONLY the root tilts out (rz 40); segment 2 straightens again (rz 0), so the rest curls back, down and
    # forward in a VERTICAL plane offset outside the skull. A tilted curl axis sweeps the tip back inward as it bends
    # down (pass 3: still 98% buried). Adding splay at every segment as well -- the first two passes -- interacts with the >90 deg bend and swung
    # the tip back in through the neck (98% buried). Widths taper 0.45 per segment so the joints' side faces clear.
    horn_world("ram", "head", (1.9, base[1], base[2]), [(2.6, 2.0, 2.0, 25), (2.6, 1.55, 1.7, 85),
                                                        (2.4, 1.1, 1.4, 145), (2.2, 0.65, 1.1, 205)], 40.0)
    horn("chamois", "head", base, [(2.4, 0.6, 0.7, -20, 2), (1.4, 0.5, 0.5, 110, 2)], head_w, 2)
    horn("dagger", "head", base, [(2.4, 0.8, 0.8, 5, 3), (1.8, 0.6, 0.6, 22, 3)], head_w, 3)
    # ---- LEGS: short and sturdy, four segments, outer/inner planes stepping inboard ----
    # lateral planes step >= 0.2 between neighbours and clear the chest (3.8) / rump (3.9) sides; the hoof's heel
    # (bk 0.5) sits well clear of the cannon's (0.9)
    leg("fore", "body_chest", -6.6, fore_y if fore_y is not None else hip_y - 1.0, [
        #  name        ang   L    outer inner  fr   bk   ov
        ("upper",       10, 3.6, 4.4, 1.6, 1.8, 1.7, 1.6),
        ("forearm",     -6, 3.0, 3.5, 2.0, 1.3, 1.2, 0.9),
        ("cannon",       2, 2.6, 3.1, 2.3, 0.9, 0.9, 0.8),
        ("hoof",         0, 1.2, 3.35, 2.1, 1.3, 0.5, 0.5),
    ], 6.0)
    leg("hind", "body_rump", 10.4, hind_y if hind_y is not None else hip_y - 1.0, [
        ("thigh",       20, 3.8, 4.5, 1.5, 2.0, 2.0, 1.8),
        ("gaskin",     -28, 3.2, 3.6, 1.9, 1.3, 1.3, 1.0),
        ("cannon",      10, 2.8, 3.2, 2.2, 0.9, 0.9, 0.8),
        ("hoof",         0, 1.2, 3.45, 2.0, 1.3, 0.5, 0.5),
    ], 8.0)


def solve():
    """The body pivot is fixed; each leg pair's authored pivot height is solved so its hoof soles land on y=0 (a single
    shared pivot height left the fore hooves 0.38u up and the hind 0.38u down)."""
    y, fy, hy = 11.4, 10.4, 10.4
    for _ in range(40):
        build(y, fy, hy)
        fy -= lowest(["fore_left_hoof"])
        hy -= lowest(["hind_left_hoof"])
    build(y, fy, hy)
    return y, fy, hy


HIP_Y, FORE_Y, HIND_Y = solve()

CANVAS = (256, 128)
FAMILY = {"ibex": "ibex", "ram": "ram", "chamois": "chamois", "daggers": "dagger"}


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
    for frac, name, cls in sorted(out, reverse=True)[:14]:
        flag = "   <== FEATURE MOSTLY BURIED" if frac > 0.55 and cls in ("horn", "beard", "ear", "nose", "tail") else ""
        print(f"  {frac:6.1%}  {name}{flag}")


def main():
    wc = {p[0]: world(p[0], corners(p)) for p in PARTS}
    lo = {k: v.min(0) for k, v in wc.items()}
    hi = {k: v.max(0) for k, v in wc.items()}
    floor = min(v[1] for v in lo.values())
    wp = BY_NAME["body_chest"]
    withers = world("body_chest", [(0.0, wp[5][1], -6.6)])[0][1]
    print(f"hip pivot {HIP_Y:.2f}u; lowest {floor:+.3f}" + ("  OK" if floor > -0.02 else "  <== FLOOR"))
    for n in ("fore_left_hoof", "hind_left_hoof"):
        print(f"  {n:<16} sole {lo[n][1]:+.3f}")
    print(f"  withers {withers:.2f}u  belly {lo['body_barrel'][1]:.2f}u ({lo['body_barrel'][1] / withers:.0%})  "
          f"head top {hi['head'][1]:.2f}u  nose z {lo['nose'][2]:.1f}  rump z {hi['body_rump'][2]:.1f}")
    for fam in FAMILY.values():
        hs = [n for n in wc if n.startswith(fam + "_left")]
        top = max(hi[n][1] for n in hs)
        back = max(hi[n][2] for n in hs)
        out = min(lo[n][0] for n in hs)
        print(f"  horns {fam:<8} top y {top:5.2f}  rearmost z {back:5.1f}  outermost x {out:5.2f}")
    for name, var, real, fam, beard in SPECIES:
        s = real * 16.0 / withers
        print(f"  {name:<16} {real:.2f} m -> scale {s:.3f}   horns {fam}{'  + beard' if beard else ''}")
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


LEGS = {"fore": ("body_chest", 6.0, ["upper", "forearm", "cannon", "hoof"]),
        "hind": ("body_rump", 8.0, ["thigh", "gaskin", "cannon", "hoof"])}


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
        # neck: lift until every head part (incl. the lowest-hanging horn family and the beard) clears the floor
        over = {}
        for prefix in ("fore", "hind"):
            for side in ("left", "right"):
                over.update(_leg_rot(prefix, side, out[(pose, prefix)][0]))
        nrest = BY_NAME["neck"][3]
        heads = [q[0] for q in PARTS if q[6] in ("head", "horn", "beard", "nose", "ear")]
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
