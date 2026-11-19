#!/usr/bin/env python3
"""Pinniped model spec + forward-kinematics checker -- SOP Phase 1 / 1b / 2 (Phase 4.4, user-approved 2026-10-10).

A NEW amphibious type `pinniped`: walrus, bearded seal, ringed seal, harp seal, Steller sea lion. Tooling forked from
giant_monitor_spec.py.

Blockbench space: Y up, ground y=0, -Z = nose, -X = left. World-angle convention as the other new specs (positive rx
turns a part's -Z end UP / a hanging part's lower end FORWARD).

REST POSE = hauled out, lying PRONE: the belly on the ground, the head raised on a short thick neck, the fore flippers
splayed out to the sides with their tips on the ground, the hind flippers trailing together behind. A fusiform body
(chest > belly > hips), a round head, a short muzzle.
Per-species parts, gated in the model:
  * whisker pad (walrus, bearded seal)  * tusks (walrus)  * ear flaps (Steller sea lion)
The sea lion's upright, propped-on-the-fore-flippers stance is a SOLVED per-species pose applied in setupAnim.

Run:  python scripts/pinniped_spec.py [--uv | --zfight | --burial | --json]
"""
import math
import numpy as np

D = math.radians

SPECIES = [
    # name,           variant, real length m (nose to hind-flipper tip), whiskers, tusks, ears
    ("walrus",          0, 3.2, True,  True,  False),   # Odobenus rosmarus
    ("bearded_seal",    1, 2.4, True,  False, False),   # Erignathus barbatus
    ("ringed_seal",     2, 1.4, False, False, False),   # Pusa hispida -- the polar bear's main prey
    ("harp_seal",       3, 1.8, False, False, False),   # Pagophilus groenlandicus -- white-coat pups
    ("steller_sea_lion", 4, 3.0, False, False, True),   # Eumetopias jubatus
]
HITBOX = (1.6, 1.0)

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


def build(splay, droop=20.0):
    PARTS.clear()
    BY_NAME.clear()
    # ---- fusiform body lying prone: chest > belly > hips, the belly on the ground ----
    add("body_chest", None, (0, 4.5, 0.0), (0, 0, 0), (-4.6, 0.3, -6.5), (4.6, 9.0, 4.0), "body")
    add("body_belly", "body_chest", (0, 4.5, 3.5), (0, 0, 0), (-4.3, 0.0, 3.0), (4.3, 8.4, 11.5), "body")
    add("body_hips", "body_belly", (0, 4.3, 11.0), (0, 0, 0), (-3.4, 0.6, 10.5), (3.4, 7.0, 17.5), "body")
    # hind flippers: two BROAD fans lying side by side behind the hips (0.3u apart, parallel, so their faces never
    # overlap), drooping until the tips rest on the ground. (v1: thin planks raised in a V -- "look weird", user.)
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"hind_{side}", "body_hips", (s * 1.65, 2.2, 17.0), (droop, 0, 0),
            (s * 0.15, 1.75, 16.2), (s * 3.1, 2.65, 21.6), "flipper")
    # ---- neck + head, raised ----
    zchain("body_chest", (0, 5.5, -6.0), [
        #  name      ang    L    hw   up   dn   ov
        ("neck",      22,  3.6, 3.4, 3.0, 3.2, 2.0),
        ("head",       6,  4.6, 2.9, 2.4, 2.2, 1.4),
        ("muzzle",    -4,  2.6, 1.9, 1.4, 1.5, 1.0),
    ], "head")
    # rest-frame anchors (about y 5.5): neck z -9.6..-6   head -14.2..-9.6   muzzle -16.8..-14.2
    add("nose", "muzzle", (0, 6.0, -16.9), (0, 0, 0), (-1.1, 5.5, -17.3), (1.1, 6.5, -16.4), "nose")
    add("whiskers", "muzzle", (0, 4.4, -15.6), (0, 0, 0), (-2.5, 3.5, -17.2), (2.5, 5.1, -14.8), "whisker")
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"tusk_{side}", "muzzle", (s * 0.95, 4.3, -16.0), (12, 0, 0),
            (s * 0.55, -0.9, -16.4), (s * 1.35, 4.5, -15.6), "tusk")
        add(f"ear_{side}", "head", (s * 2.9, 7.0, -10.6), (0, 0, -s * 20),
            (s * 2.6, 6.6, -11.0), (s * 3.6, 8.4, -10.4), "ear")
        add(f"eye_{side}", "head", (s * 3.25, 6.9, -11.8), (0, 0, 0),
            (s * 3.25, 5.9, -12.8), (s * 3.25, 7.9, -10.8), "eye")
    # ---- fore flippers: BROAD short paddles held close to the flank, swept BACK (rx -40), splayed out only until the
    # tip rests on the ground. (v1: long thin planks splayed sideways at ground level -- "far too low", user.) ----
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"fore_{side}", "body_chest", (s * 4.4, 3.2, -3.4), (-40, 0, s * splay),
            (s * 4.05, -0.9, -5.0), (s * 4.85, 3.6, -1.6), "flipper")


def solve():
    # fore splay: the smallest outward swing that keeps the paddle tip on (not through) the ground
    lo, hi = 0.0, 89.0
    for _ in range(40):
        mid = (lo + hi) / 2
        build(mid)
        if lowest(["fore_left"]) < 0.02:
            lo = mid
        else:
            hi = mid
    splay = hi
    # hind droop: the fans tip down until their trailing edge rests on the ground
    lo, hi = 0.0, 60.0
    for _ in range(40):
        mid = (lo + hi) / 2
        build(splay, mid)
        if lowest(["hind_left"]) > 0.05:
            lo = mid
        else:
            hi = mid
    build(splay, (lo + hi) / 2)
    return splay, (lo + hi) / 2


SPLAY, DROOP = solve()

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
        flag = "   <== FEATURE MOSTLY BURIED" if frac > 0.55 and cls in ("whisker", "tusk", "ear", "nose", "flipper") else ""
        print(f"  {frac:6.1%}  {name}{flag}")


def main():
    wc = {p[0]: world(p[0], corners(p)) for p in PARTS}
    lo = {k: v.min(0) for k, v in wc.items()}
    hi = {k: v.max(0) for k, v in wc.items()}
    floor = min(v[1] for k, v in lo.items() if BY_NAME[k][6] != "tusk")
    nose = lo["nose"][2]
    tip = max(hi["hind_left"][2], hi["hind_right"][2])
    length = tip - nose
    print(f"fore-flipper splay {SPLAY:.1f} deg, hind droop {DROOP:.1f} deg; lowest point (tusks aside) {floor:+.3f}" + ("  OK" if floor > -0.02 else "  <== FLOOR"))
    print(f"  tusk tips y {lo['tusk_left'][1]:+.2f}  (walrus: they may touch the ground when the head drops)")
    print(f"  belly {lo['body_belly'][1]:+.2f}  back {hi['body_chest'][1]:.2f}u  head top {hi['head'][1]:.2f}u  "
          f"nose-to-flipper-tip {length:.1f}u")
    for name, var, real, *_ in SPECIES:
        s = real * 16.0 / length
        print(f"  {name:<17} {real:.1f} m -> scale {s:.3f}   back {hi['body_chest'][1] * s / 16:.2f} m")
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


def low_of(names, over, dy=0.0):
    return min(world_mod(n, vol(BY_NAME[n]), over, dy)[:, 1].min() for n in names)


def _fore(over, side, s, splay, rx):
    over[f"fore_{side}"] = (rx, 0.0, s * splay)


def _scaled_fore(k):
    """Temporarily stretch both fore flippers to k x length about their pivots (Java: setScale(1, k, 1))."""
    saved = {}
    for side in ("left", "right"):
        n = f"fore_{side}"
        rec = BY_NAME[n]
        saved[n] = rec
        oy = rec[2][1]
        lo, hi = list(rec[4]), list(rec[5])
        lo[1] = oy + (lo[1] - oy) * k
        hi[1] = oy + (hi[1] - oy) * k
        BY_NAME[n] = (rec[0], rec[1], rec[2], rec[3], tuple(lo), tuple(hi), rec[6])
    return saved


def stance_sealion():
    """The otariid stance: chest raised on long straightened fore flippers, belly and hips flat on the ground.
    Sea lions' fore flippers are far bigger than a phocid's, so the search includes their LENGTH (k, a Java setScale on
    the flipper's long axis): at seal length the tips could not reach the ground from a raised chest (0.2u short at the
    swing limit). Chest pitch fixed at 24 deg; belly counter-rotated flat; the drop puts the belly on the ground."""
    best = None
    for k in np.linspace(1.0, 1.6, 13):
        saved = _scaled_fore(k)
        for splay in np.linspace(5, 60, 23):
            for rx in np.linspace(-60, 40, 41):
                over = {"body_chest": (24.0, 0, 0), "body_belly": (-24.0, 0, 0)}
                for side, sg in (("left", -1.0), ("right", 1.0)):
                    _fore(over, side, sg, splay, rx)
                dy = -low_of(["body_belly", "body_hips"], over)
                tip = low_of(["fore_left"], over, dy)
                if tip < -0.02:
                    continue
                sc = (tip - 0.05) ** 2 + 0.0004 * (splay - 15) ** 2 + 0.0002 * rx ** 2 + 0.02 * (k - 1.0)
                if best is None or sc < best[0]:
                    best = (sc, over, dy, splay, rx, k)
        BY_NAME.update(saved)
    return best


def rest_poses():
    """sit = the 'banana': head and hind flippers lifted; sleep = flat, head down on the ground."""
    out = {}
    sit = {"neck": (34.0, 0, 0), "head": (-6.0, 0, 0)}
    for side, sg in (("left", -1.0), ("right", 1.0)):
        sit[f"hind_{side}"] = (DROOP - 42.0, 0.0, 0.0)     # lifted ~20 deg ABOVE level (rest droops DROOP below)
    out["sit"] = (sit, 0.0)
    sleep = {"neck": (-6.0, 0, 0), "head": (-10.0, 0, 0)}
    dy = 0.0
    lowhead = low_of(["muzzle", "nose", "whiskers"], sleep)
    if lowhead < 0.15:                       # keep the chin on, not through, the ground
        sleep["neck"] = (-6.0 + 2.0 * (0.15 - lowhead), 0, 0)
    out["sleep"] = (sleep, dy)
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
        b = stance_sealion()
        print(f"sea lion stance: drop {b[2]:+.2f}u splay {b[3]:.1f} rx {b[4]:.1f} flipper x{b[5]:.2f}  score {b[0]:.4f}")
        for k, (ov, dy) in rest_poses().items():
            allp = [q[0] for q in PARTS if q[6] not in ("eye", "tusk")]
            print(k, "lowest", round(min(low_of([n], ov, dy) for n in allp), 3), {n: v[0] for n, v in ov.items()})
    else:
        main()
