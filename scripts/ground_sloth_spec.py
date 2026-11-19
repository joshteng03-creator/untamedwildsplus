#!/usr/bin/env python3
"""Ground sloth model spec + forward-kinematics checker -- SOP Phase 1 / 1b / 2. FULL REBUILD of the `ground_sloth` type
(user decision 2026-11-15, plan: docs/plans/2026-11-15-proboscidean-sloth-rebuild.md).

The July rig was sculpted by eye before the SOP. Its faults, fixed by construction:
  * the body was three overlapping slabs at different tilts (hips 0, arch -16, chest -12) -> a stair-stepped outline that
    read as a crumpled crate. Now ONE long topline box, tall and buried in a nested barrel, runs from the high pelvis down
    to the lower shoulders (the real ground-sloth posture); barrel side planes step in monotonically from the wide pelvis;
  * ~2.7 m at the hips for a ~2 m animal (length:height 2.1, real ~3) -> calibrated by HIP HEIGHT, 1 block = 1 m;
  * a small bear-like head that vanished into the chest -> a head per FAMILY (flag headShape), carried forward and low:
        0 MEGATHERIID  tall narrow skull, very deep jaw, prehensile upper lip   (Megatherium, Eremotherium)
        1 MYLODONTID   broad square muzzle                                     (Glossotherium)
        2 MEGALONYCHID short blunt head                                        (Megalonyx, Megalocnus)
  * ivory "mitten" claws -> dark horn claws; the forefoot walks on its outer edge with the claws turned in along the ground,
    the hind foot stands on its outer edge with the sole turned in (pedolateral);
  * one shape for five species -> the head families above.

Blockbench space: Y up, ground y=0, -Z = head end, -X = left. Positive rx turns a part's -Z end UP.
Run:  python scripts/ground_sloth_spec.py [--uv | --zfight | --burial | --json [species] | --pose]
"""
import math
import os
import sys

import numpy as np

D = math.radians
CANVAS = (256, 256)

HEAD = {"MEGA": 0, "MYLO": 1, "MEGALONYCH": 2}
SPECIES = [
    # name, variant, real hip height m, head family
    ("megatherium",   0, 2.0,  "MEGA"),         # Megatherium americanum, ~6 m long
    ("eremotherium",  1, 2.1,  "MEGA"),         # Eremotherium laurillardi, as large
    ("megalonyx",     2, 1.2,  "MEGALONYCH"),   # Megalonyx jeffersonii, ~3 m long
    ("megalocnus",    3, 0.65, "MEGALONYCH"),   # Megalocnus rodens, Cuban island sloth
    ("glossotherium", 4, 1.4,  "MYLO"),         # Glossotherium robustum, ~4 m long
]
OLD_SCALE = {"megatherium": 1.4, "eremotherium": 1.35, "megalonyx": 1.1, "megalocnus": 0.4, "glossotherium": 1.15}
OLD_HIP_U = 33.0          # the July rig's hip height in model units

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


def rot_from(a, d):
    """Minimal rotation taking unit axis a onto direction d."""
    a = np.asarray(a, float) / np.linalg.norm(a)
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


def world(name, pts, over=None):
    over = over or {}
    p = np.asarray(pts, dtype=float).reshape(-1, 3)
    for nm, _, o, r, *_ in ancestry(name):
        ov = over.get(nm, {})
        o = np.asarray(o, float)
        p = (rmat(*ov.get("r", r)) @ (p - o).T).T + o
    return p


def local_of(name, pts):
    p = np.asarray(pts, float).reshape(-1, 3)
    for nm, _, o, r, *_ in reversed(ancestry(name)):
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


def world_rot(name):
    R = np.eye(3)
    for rec in reversed(ancestry(name)):
        R = R @ rmat(*rec[3])
    return R


def chain(prefix, parent, base, segs, dirs, axis, cls, mirror=True, names=None, world_frame=False):
    """A chain authored by WORLD DIRECTION per segment (relative to the parent's frame): each segment runs along `axis`
    (local, unit) from its pivot. segs: (L, w, h, ov) -- w across x, h across the other axis; dirs: LEFT side (x mirrored).
    Local eulers are derived as W_parent^T . W_k (the horn_world technique), so angles never accumulate by accident."""
    ax = np.asarray(axis, float)
    sides = (("left", -1.0), ("right", 1.0)) if mirror else (("", 1.0),)
    for side, s in sides:
        bx, by, bz = base
        x0 = s * abs(bx) if mirror else bx
        # world_frame: dirs are WORLD directions (the tail hangs from a rump already tilted 38 deg -- read in the parent
        # frame, that tilt stacked onto the droop and put the tip 6u underground)
        par, cur, Wp = parent, np.array([x0, by, bz], float), (world_rot(parent) if world_frame else np.eye(3))
        for k, ((L, w, h, ov), dv) in enumerate(zip(segs, dirs)):
            n = (names[k] if names else f"{prefix}_{k + 1}") if not mirror else (f"{prefix}_{side}_{names[k]}" if names else f"{prefix}_{side}_{k + 1}")
            dd = np.array([dv[0] if s < 0 else -dv[0], dv[1], dv[2]]) if mirror else np.asarray(dv, float)
            W = rot_from(ax, dd)
            loc = euler_zyx(Wp.T @ W)
            # box along the local axis from -ov to L, cross-section w x h
            if abs(ax[1]) > 0.5:          # along y
                sgn = np.sign(ax[1])
                y0, y1 = sorted((cur[1] - sgn * ov, cur[1] + sgn * L))
                a, b = (cur[0] - w / 2, y0, cur[2] - h / 2), (cur[0] + w / 2, y1, cur[2] + h / 2)
            else:                         # along z
                sgn = np.sign(ax[2])
                z0, z1 = sorted((cur[2] - sgn * ov, cur[2] + sgn * L))
                a, b = (cur[0] - w / 2, cur[1] - h / 2, z0), (cur[0] + w / 2, cur[1] + h / 2, z1)
            add(n, par, tuple(cur), loc, a, b, cls)
            par, Wp = n, W
            cur = cur + ax * L


# ================================================================ chain shapes (LEFT side; inward = +x)
# long and carried fairly level (first pass drooped 20-44 deg: 2.42 length:hip and the tip 5.5u underground)
TAIL_SEGS = [(10.0, 11.0, 10.0, 1.5), (9.0, 8.4, 7.6, 1.2), (8.0, 6.0, 5.4, 1.0), (6.0, 4.0, 3.6, 0.9)]
TAIL_PITCH = [12.0, 18.0, 24.0, 30.0]           # world droop per segment (deg, + = tip down)
CLAW_SEGS = [(3.4, 1.5, 1.6, 0.6), (3.0, 1.1, 1.2, 0.6)]
# forefoot claws lie along the ground pointing IN and then curl back: the hand walks on its outer edge
CLAW_DIRS = {"big": [(0.62, -0.18, -0.76), (0.55, -0.10, 0.83)], "small": [(0.70, -0.20, -0.68), (0.62, -0.12, 0.78)]}


def build():
    PARTS.clear()
    BY_NAME.clear()
    # ---- BARREL: nested; side planes step in from the wide pelvis (hips 11.2 > mid 10.5 > rump 9.6 > chest 8.8) ----
    add("body_mid", None, (0, 22, 0), (0, 0, 0), (-10.5, 14.0, -11.0), (10.5, 27.5, 11.0), "body")
    add("body_hips", "body_mid", (0, 24, 12), (8, 0, 0), (-11.2, 13.5, 4.0), (11.2, 29.0, 21.0), "body")
    add("body_rump", "body_hips", (0, 27, 20), (30, 0, 0), (-9.6, 15.0, 17.0), (9.6, 29.0, 26.0), "body")
    # chest bottom raised (11 -> 13.5): hung that deep it left the long, powerful forelimbs looking like stubs
    add("body_chest", "body_mid", (0, 20, -12), (0, 0, 0), (-8.8, 13.5, -24.5), (8.8, 25.0, -6.0), "body")
    # ---- TOPLINE: one long tall box, buried in the barrel, high over the pelvis and falling to the shoulders ----
    add("body_back", "body_mid", (0, 26, -2), (-10, 0, 0), (-8.2, 18.0, -24.0), (8.2, 29.5, 14.0), "body")
    # ---- NECK + HEAD: carried forward and LOW ----
    add("neck", "body_chest", (0, 21, -22), (-10, 0, 0), (-5.6, 15.5, -34.0), (5.6, 25.5, -20.0), "head")
    add("head", "neck", (0, 20.5, -29.5), (10, 0, 0), (-3.6, 15.5, -37.0), (3.6, 23.0, -28.5), "head")
    # family boxes (children of the cranium): MEGATHERIID
    add("snout_mega", "head", (0, 18, -37), (0, 0, 0), (-2.6, 15.0, -43.0), (2.6, 20.5, -36.5), "mega")
    add("jaw_mega", "head", (0, 16, -31), (0, 0, 0), (-3.0, 11.0, -40.0), (3.0, 16.5, -30.0), "mega")
    add("lip_mega", "snout_mega", (0, 17, -43), (0, 0, 0), (-1.8, 15.6, -45.0), (1.8, 18.6, -42.5), "mega")
    # MYLODONTID: broad cheeks and a square muzzle
    add("cheek_mylo", "head", (0, 18, -31), (0, 0, 0), (-5.2, 15.0, -34.0), (5.2, 21.5, -29.5), "mylo")
    add("snout_mylo", "head", (0, 17, -37), (0, 0, 0), (-4.3, 14.0, -42.0), (4.3, 19.5, -36.0), "mylo")
    add("jaw_mylo", "head", (0, 14, -31), (0, 0, 0), (-4.0, 12.5, -41.5), (4.0, 15.5, -31.0), "mylo")
    # MEGALONYCHID: short and blunt
    add("snout_short", "head", (0, 17, -37), (0, 0, 0), (-3.1, 14.5, -40.0), (3.1, 20.0, -36.0), "megalonych")
    add("jaw_short", "head", (0, 14.5, -31), (0, 0, 0), (-3.3, 12.5, -38.5), (3.3, 16.0, -30.0), "megalonych")
    for side, s in (("left", -1.0), ("right", 1.0)):
        add(f"eye_{side}", "head", (s * 3.95, 20.25, -35.6), (0, 0, 0), (s * 3.95, 19.5, -36.6), (s * 3.95, 21.0, -34.6), "eye")
        add(f"ear_{side}", "head", (s * 3.6, 22, -30.5), (0, 0, 0), (s * 3.3, 21.9, -31.4), (s * 5.6, 24.6, -29.6), "ear")
    # ---- TAIL: thick, drooping toward the ground (the third leg of the rearing tripod) ----
    dirs = [(0, -math.sin(D(a)), math.cos(D(a))) for a in TAIL_PITCH]
    chain("tail", "body_rump", (0, 24.5, 23.0), TAIL_SEGS, dirs, (0, 0, 1), "tail", mirror=False, world_frame=True)
    # ---- FORELIMBS: long and powerful; the hand walks on its outer edge, claws turned in along the ground ----
    for side, s in (("left", -1.0), ("right", 1.0)):
        fx = s * 6.6
        add(f"arm_{side}_1", "body_chest", (fx, 19, -16), (8, 0, 0), (fx - 2.8, 9.0, -19.2), (fx + 2.8, 21.5, -12.8), "leg")
        add(f"arm_{side}_2", f"arm_{side}_1", (fx, 9.5, -16), (-14, 0, 0), (fx - 2.5, 2.4, -18.6), (fx + 2.5, 10.6, -13.4), "leg")
        add(f"hand_{side}", f"arm_{side}_2", (fx, 2.8, -16), (6, 0, -s * 10), (fx - 2.7, 0.3, -19.5), (fx + 2.7, 3.7, -13.0), "foot")
    chain("claw", "hand_left", (6.6 - 1.4, 0.75, -19.0), CLAW_SEGS, CLAW_DIRS["big"], (0, -1, 0), "claw", mirror=True,
          names=["a1", "a2"])
    chain("claw", "hand_left", (6.6 - 1.6, 0.75, -16.1), CLAW_SEGS, CLAW_DIRS["small"], (0, -1, 0), "claw", mirror=True,
          names=["b1", "b2"])
    # the mirrored claw chains were built on hand_left: re-parent the right ones
    for n in [p[0] for p in PARTS if p[0].startswith("claw_right_")]:
        rec = BY_NAME[n]
        if rec[1] == "hand_left":
            new = (rec[0], "hand_right") + rec[2:]
            PARTS[PARTS.index(rec)] = new
            BY_NAME[n] = new
    # ---- HIND LIMBS: massive thigh, short shank, the foot on its outer edge with the sole turned in ----
    for side, s in (("left", -1.0), ("right", 1.0)):
        hx = s * 8.0
        add(f"leg_{side}_thigh", "body_hips", (hx, 22, 13), (0, 0, 0), (hx - 3.6, 9.0, 9.0), (hx + 3.6, 24.0, 17.0), "leg")
        add(f"leg_{side}_shank", f"leg_{side}_thigh", (hx, 9.6, 13), (0, 0, 0), (hx - 3.0, 3.2, 9.8), (hx + 3.0, 10.6, 16.2), "leg")
        add(f"foot_{side}", f"leg_{side}_shank", (s * 8.6, 3.6, 13), (0, 0, s * 15), (s * 8.6 - 1.8, 1.0, 7.5), (s * 8.6 + 1.8, 7.2, 18.5), "foot")



def transform_subtree(root, k, off):
    """Scale a part and its descendants by k about the root's pivot, then translate by off (rest frame)."""
    P = np.asarray(BY_NAME[root][2], float)
    off = np.asarray(off, float)
    todo, names = [root], []
    while todo:
        n = todo.pop()
        names.append(n)
        todo += [q[0] for q in PARTS if q[1] == n]
    for n in names:
        rec = BY_NAME[n]
        f = lambda v: tuple(P + (np.asarray(v, float) - P) * k + off)
        new = (rec[0], rec[1], f(rec[2]), rec[3], f(rec[4]), f(rec[5]), rec[6])
        PARTS[PARTS.index(rec)] = new
        BY_NAME[n] = new


def build_all():
    build()
    # the head was small and tucked against the chest, so the three head families barely showed: 25% larger, carried
    # 3u further forward on the lengthened neck
    transform_subtree("head", 1.25, (0, 0, -3.0))


build_all()


def config(sp):
    name, var, real, head = sp
    hide = set()
    fam = {"MEGA": "mega", "MYLO": "mylo", "MEGALONYCH": "megalonych"}[head]
    for p in PARTS:
        if p[6] in ("mega", "mylo", "megalonych") and p[6] != fam:
            hide.add(p[0])
    return {}, hide


def shown_parts(sp):
    over, hide = config(sp)
    return [p for p in PARTS if p[0] not in hide], over


def hip_height(sp=None):
    pts = world("body_back", vol(BY_NAME["body_back"], 9))
    m = pts[(pts[:, 2] > 6) & (pts[:, 2] < 18)]
    return float(max(m[:, 1].max(), world("body_hips", corners(BY_NAME["body_hips"]))[:, 1].max()))


def main():
    sp = SPECIES[0]
    parts, over = shown_parts(sp)
    pts = {p[0]: world(p[0], vol(p, 6)) for p in parts}
    allp = np.vstack(list(pts.values()))
    hh = hip_height()
    nose = allp[:, 2].min()
    tip = pts["tail_4"][:, 2].max()
    L = tip - nose
    print(f"{len(PARTS)} boxes; hip height {hh:.1f}u; nose {nose:.1f} .. tail tip {tip:.1f} = {L:.1f}u; length:hip-height {L / hh:.2f} (real ~3)")
    sh = max(p[:, 1].max() for n, p in pts.items() if n in ("body_back",) and False) if False else None
    front = pts["body_back"][pts["body_back"][:, 2] < -18]
    print(f"  shoulder (back top over the forelegs) {front[:, 1].max():.1f}u; belly {pts['body_mid'][:, 1].min():.1f}u; "
          f"hips width {2 * 11.2:.1f}u, chest width {2 * 8.8:.1f}u")
    for n in ("hand_left", "foot_left", "claw_left_a2", "claw_left_b2", "tail_4", "jaw_mega", "lip_mega"):
        if n in pts:
            print(f"  {n:<16} lowest y {pts[n][:, 1].min():+6.2f}")
    print()
    print(f"{'species':<14}{'real hip m':>11}{'scale':>8}{'new length m':>13}{'old hip m':>10}")
    for sp in SPECIES:
        s = sp[2] * 16.0 / hh
        old = OLD_SCALE[sp[0]] * OLD_HIP_U / 16.0
        print(f"{sp[0]:<14}{sp[2]:>11.2f}{s:>8.3f}{L * s / 16:>13.2f}{old:>10.2f}" + ("  <== shrinks" if sp[2] < old - 0.05 else ""))
    eye_report()


def eye_report():
    for sp in SPECIES:
        parts, _ = shown_parts(sp)
        names = {p[0] for p in parts}
        c = np.mean([np.asarray(BY_NAME["eye_left"][4]), np.asarray(BY_NAME["eye_left"][5])], axis=0)
        worst = None
        for p in parts:
            if p[6] in ("eye",) or p[0] == "eye_left":
                continue
            l = local_of(p[0], c)[0]
            a, b = np.asarray(p[4], float), np.asarray(p[5], float)
            gap = max(max(a[i] - l[i], l[i] - b[i]) for i in range(3))
            if worst is None or gap < worst[0]:
                worst = (gap, p[0])
        print(f"  eye ({sp[0]}): nearest box {worst[1]} outside by {worst[0]:+.2f}" + ("  <== TOO CLOSE" if worst[0] < 0.3 else ""))


def footprint(p):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    w, h, d = (abs(b[i] - a[i]) for i in range(3))
    return int(math.ceil(2 * (w + d))), int(math.ceil(h + d))


def uvpack():
    W, H = CANVAS
    items = sorted(((footprint(p), p[0]) for p in PARTS), key=lambda it: (-it[0][1], -it[0][0]))
    sky = [0] * W
    placed = {}
    for (fw, fh), name in items:
        best = None
        for x in range(0, W - fw + 1):
            y = max(sky[x:x + fw])
            if best is None or y < best[1]:
                best = (x, y)
        x, y = best
        placed[name] = (x, y, fw, fh)
        for i in range(x, x + fw):
            sky[i] = y + fh
    used = max(sky)
    names = sorted(placed)
    for i in range(len(names)):
        ax, ay, aw, ah = placed[names[i]]
        for j in range(i + 1, len(names)):
            bx, by, bw, bh = placed[names[j]]
            if ax < bx + bw and bx < ax + aw and ay < by + bh and by < ay + ah:
                raise AssertionError(f"UV overlap: {names[i]} vs {names[j]}")
    tot = sum(fw * fh for fw, fh in (footprint(p) for p in PARTS))
    print(f"UV SKYLINE PACK on {W}x{H}: {len(placed)} boxes, {used} rows used, {tot} px ({100 * tot / (W * H):.0f}%), zero overlaps")
    assert used <= H, "does not fit"
    return placed


def emit_json(species=None):
    import io
    import json
    from contextlib import redirect_stdout
    species = species or os.environ.get("SLOTH_SPECIES")
    with redirect_stdout(io.StringIO()):
        placed = uvpack()
    hide = set()
    if species:
        sp = next(s for s in SPECIES if s[0] == species)
        _, hide = config(sp)
    out = []
    for name, parent, o, r, a, b, cls in PARTS:
        if name in hide:
            continue
        u, v, _, _ = placed[name]
        out.append({"n": name, "p": parent, "o": [round(x, 4) for x in o], "r": [round(x, 4) for x in r],
                    "f": [round(x, 4) for x in a], "t": [round(x, 4) for x in b], "uv": [u, v], "c": cls})
    print(json.dumps({"canvas": list(CANVAS), "parts": out}, separators=(",", ":")))


def zfight(species_list=None):
    import zfight_sweep as Z
    for sp in [s for s in SPECIES if not species_list or s[0] in species_list]:
        parts, _ = shown_parts(sp)
        zp = {}
        for name, parent, o, r, a, b, cls in parts:
            if cls == "eye":
                continue
            z = Z.Part(name)
            po = np.asarray(BY_NAME[parent][2], float) if parent else np.zeros(3)
            z.rp = np.asarray(o, float) - po
            z.rot = np.array([D(r[0]), D(r[1]), D(r[2])])
            z.lo = np.asarray(a, float) - np.asarray(o, float)
            z.hi = np.asarray(b, float) - np.asarray(o, float)
            z.parent = parent
            zp[name] = z
        hits = list(Z.sweep(zp))
        for area, na, nb, fa, fb, rel in hits:
            print(f"  [{sp[0]}] {area:6.2f}u2  {na:<18} {'XYZ'[fa[0]]}  <->  {nb:<18} {'XYZ'[fb[0]]}{rel}")
        print(f"  [{sp[0]}] z-fight: " + ("clean" if not hits else f"{len(hits)} pair(s)"))


FEATURES = ("ear", "claw", "tail", "mega", "mylo", "megalonych", "foot")


def burial(species_list=None):
    for sp in [s for s in SPECIES if not species_list or s[0] in species_list]:
        parts, _ = shown_parts(sp)
        out = []
        for p in parts:
            if p[6] not in FEATURES:
                continue
            pts = world(p[0], vol(p, 9))
            inside = np.zeros(len(pts), bool)
            for q in parts:
                if q[0] == p[0] or q[6] == "eye":
                    continue
                l = local_of(q[0], pts)
                a, b = np.asarray(q[4], float), np.asarray(q[5], float)
                inside |= np.all((l >= a + 1e-6) & (l <= b - 1e-6), axis=1)
            out.append((inside.mean(), p[0]))
        worst = [f"{n} {f:.0%}" for f, n in sorted(out, reverse=True) if f > 0.45]
        print(f"  [{sp[0]}] features >45% buried: " + (", ".join(worst) if worst else "none"))


# ================================================================ phase 7: SOLVED poses + the rear-up
def posed(sp, dz_rot, root=None, names=None):
    """World points for one species with BB rotation DELTAS added to the default rotations (dz_rot: name -> (dx,dy,dz)).
    root: (theta_x_deg, (tx,ty,tz)) -- an extra rotation of the whole animal about the root pivot, then a translation."""
    _, hide = config(sp)
    over = {}
    for n, d in dz_rot.items():
        r = BY_NAME[n][3]
        over[n] = {"r": (r[0] + d[0], r[1] + d[1], r[2] + d[2])}
    out = {}
    for p in PARTS:
        if p[0] in hide or (names and p[0] not in names) or p[6] == "eye":
            continue
        q = world(p[0], vol(p, 6), over)
        if root:
            th, t = root
            o = np.asarray(BY_NAME["body_mid"][2], float)
            q = (rmat(th, 0, 0) @ (q - o).T).T + o + np.asarray(t, float)
        out[p[0]] = q
    return out


LIMBS = {"fore": ["arm_left_1", "arm_left_2", "hand_left"], "hind": ["leg_left_thigh", "leg_left_shank", "foot_left"]}
BELLY_CLEAR = 0.8
TAIL = ["tail_1", "tail_2", "tail_3", "tail_4"]
HEAD_CLS = {"MEGA": "mega", "MYLO": "mylo", "MEGALONYCH": "megalonych"}


def body_drop():
    pts = posed(SPECIES[0], {}, names={"body_mid", "body_chest", "body_hips"})
    return BELLY_CLEAR - min(q[:, 1].min() for q in pts.values())


def _limb_rot(v):
    rot = {}
    for side in ("left", "right"):
        rot[f"arm_{side}_1"] = (v[0], 0, 0)
        rot[f"arm_{side}_2"] = (v[1], 0, 0)
        rot[f"hand_{side}"] = (v[2], 0, 0)
        rot[f"leg_{side}_thigh"] = (v[3], 0, 0)
        rot[f"leg_{side}_shank"] = (v[4], 0, 0)
        rot[f"foot_{side}"] = (v[5], 0, 0)
    return rot


def _rest_score(v, dy):
    rot = _limb_rot(v)
    names = set(LIMBS["fore"] + LIMBS["hind"]) | {n for n in BY_NAME if n.startswith("claw_left")}
    pts = posed(SPECIES[0], rot, root=(0.0, (0, dy, 0)), names=names)
    pen = 0.0
    for n, q in pts.items():
        low = q[:, 1].min()
        if low < 0:
            pen += low * low * 400
    for limb in ("fore", "hind"):
        pen += (pts[LIMBS[limb][2]][:, 1].min() - 0.1) ** 2 * 4
    pen += 0.0002 * sum(x * x for x in v)
    return pen, rot


def solve_rest(verbose=True):
    rng = np.random.default_rng(13)
    dy = body_drop()
    best = np.array([70.0, -60.0, 0.0, -70.0, 60.0, 0.0])
    bs, _ = _rest_score(best, dy)
    span = np.array([35.0, 35.0, 25.0, 35.0, 35.0, 25.0])
    for _ in range(10):
        for c in best + rng.normal(0, 1, (400, 6)) * span:
            sc, _ = _rest_score(c, dy)
            if sc < bs:
                bs, best = sc, c.copy()
        span *= 0.6
    _, rot = _rest_score(best, dy)
    if verbose:
        print(f"REST  drop {dy:+.2f}  fore {best[0]:6.1f} {best[1]:6.1f} {best[2]:6.1f}  hind {best[3]:6.1f} {best[4]:6.1f} "
              f"{best[5]:6.1f}  pen {bs:.4f}")
    return dy, rot


def solve_tail_rest(dy):
    """Tail lifted (negative rx raises a +z segment's tip) just enough that it lies along the ground."""
    for a in np.arange(0, -60, -1.0):
        pts = posed(SPECIES[0], {"tail_1": (a, 0, 0)}, root=(0.0, (0, dy, 0)), names=set(TAIL))
        if min(q[:, 1].min() for q in pts.values()) >= 0.1:
            return float(a)
    return -60.0


def solve_head_rest(dy, rot, goal):
    """Per head family: the smallest neck lift (deg, split 60/40 neck/head) keeping the head >= goal off the ground."""
    out = {}
    for fam, code in HEAD.items():
        sp = next(s for s in SPECIES if s[3] == fam)
        names = {p[0] for p in PARTS if p[6] in ("head", HEAD_CLS[fam])}
        for lift in np.arange(-40.0, 60.0, 0.5):
            r = dict(rot)
            r["neck"] = (lift * 0.6, 0, 0)
            r["head"] = (lift * 0.4, 0, 0)
            pts = posed(sp, r, root=(0.0, (0, dy, 0)), names=names)
            low = min(q[:, 1].min() for q in pts.values())
            if low >= goal:
                out[code] = float(lift)
                break
    return out


def solve_rear(verbose=True):
    """The threat display: the sloth rears onto its hind feet and tail. The whole animal pitches up by theta about the
    HIP JOINT (root rotation + the translation that keeps the hip joint where it was), the hind legs counter-rotate to stay
    planted, and the tail drops until its tip rests on the ground -- the tripod."""
    hip = world("leg_left_thigh", np.asarray([BY_NAME["leg_left_thigh"][2]]))[0]
    o = np.asarray(BY_NAME["body_mid"][2], float)
    best = None
    for th in np.arange(20, 75, 1.0):
        R = rmat(th, 0, 0)
        t = hip - ((R @ (hip - o)) + o)
        rot = {f"leg_{s}_thigh": (-th, 0, 0) for s in ("left", "right")}
        # the tail angle (negative rx LIFTS a +z segment's tip) that leaves the tip resting 0.1-0.4 off the ground: pitching
        # up drops the rump, so it usually has to come UP (searched both ways; the first pass only dropped it, 11u under)
        a, tl, pts = 0.0, 99.0, None
        for a in sorted(np.arange(-90, 80, 1.0), key=abs):
            r2 = dict(rot)
            r2["tail_1"] = (a, 0, 0)
            pts = posed(SPECIES[0], r2, root=(th, t), names=set(TAIL) | {"foot_left", "leg_left_shank"})
            tl = min(pts[n][:, 1].min() for n in TAIL)
            if 0.1 <= tl <= 0.4:
                break
        feet = pts["foot_left"][:, 1].min()
        head = posed(SPECIES[0], {}, root=(th, t), names={"head"})["head"][:, 1].max()
        best = (th, t, a, feet, tl, head)
        if head / 16.0 >= 3.7:       # Megatherium reared to ~3.5-4 m
            break
    th, t, a, feet, tl, head = best
    if verbose:
        print(f"REAR  pitch {th:.0f} deg, root move {np.round(t, 2)}, tail drop {a:.0f} deg, feet {feet:+.2f}, tail tip "
              f"{tl:+.2f}, head top {head:.1f}u = {head / 16:.2f} m at scale 1")
    return dict(theta=float(th), move=[float(x) for x in t], tail=float(a))


def pose_solve(verbose=True):
    dy, rot = solve_rest(verbose)
    tail = solve_tail_rest(dy)
    sit = solve_head_rest(dy, rot, 2.0)
    sleep = solve_head_rest(dy, rot, 0.15)
    rear = solve_rear(verbose)
    if verbose:
        print(f"TAIL  rest lift {tail:.0f}; HEAD lift per family  rest {sit}  sleep {sleep}")
    return dict(dy=dy, rot=rot, tail=tail, sit=sit, sleep=sleep, rear=rear)


if __name__ == "__main__":
    if "--uv" in sys.argv:
        uvpack()
    elif "--json" in sys.argv:
        i = sys.argv.index("--json")
        emit_json(sys.argv[i + 1] if len(sys.argv) > i + 1 else None)
    elif "--zfight" in sys.argv:
        zfight(sys.argv[sys.argv.index("--zfight") + 1:] or None)
    elif "--pose" in sys.argv:
        pose_solve()
    elif "--burial" in sys.argv:
        burial(sys.argv[sys.argv.index("--burial") + 1:] or None)
    else:
        main()
