#!/usr/bin/env python3
"""Proboscidean model spec + forward-kinematics checker -- SOP Phase 1 / 1b / 2. FULL REBUILD of the `mammoth` type
(user decision 2026-11-15, plan: docs/plans/2026-11-15-proboscidean-sloth-rebuild.md).

The July rig was sculpted by eye before the SOP. Its faults are fixed by construction here:
  * EARS were 1.1-thick slabs flat on the skull, 0.4u proud (2.5 cm) -- invisible. Now a hinged flap that YAWS off the
    skull, sized by earSize (0 extinct species, 1 Asian, 2 African at ~55% of shoulder height).
  * ONE mammoth body for 12 species. Now per-species FAMILIES (flags), every family a real animal's silhouette:
        head  DOME (mammoths) / TWIN (Asian) / SLOPE (African) / CREST (Palaeoloxodon) / FLAT (mastodons, gomphotheres)
        back  SLOPE (mammoth hump) / DIP (African) / ARCH (Asian) / LEVEL (Palaeoloxodon, mastodons)
        tusks SPIRAL / FORWARD / STRAIGHT / UPCURVE / TWIST / SMALL
  * BOXY: body width:length 0.60 -> ~0.45, tapered chest/hips, rounded rump; legs were 12-deep planks of three
    differently sized boxes -> near-square columns stepping monotonically inboard, a pad foot barely wider.
Trunk and tusk chains are authored by WORLD DIRECTION per segment (the caprine horn_world technique): local eulers are
derived as W_parent^T . W_k, so per-segment angles never accumulate into the 185-degree curl the old tusks had.

Blockbench space: Y up, ground y=0, -Z = trunk end, -X = left. Positive rx turns a part's -Z end UP.
Run:  python scripts/proboscidean_spec.py [--uv | --zfight | --burial | --json [species] | --families]
"""
import math
import os
import sys

import numpy as np

D = math.radians
CANVAS = (256, 256)
HITBOX = (2.6, 2.6)

HEAD = {"DOME": 0, "TWIN": 1, "SLOPE": 2, "CREST": 3, "FLAT": 4}
BACK = {"SLOPE": 0, "DIP": 1, "ARCH": 2, "LEVEL": 3}
TUSK = {"SPIRAL": 0, "FORWARD": 1, "STRAIGHT": 2, "UPCURVE": 3, "TWIST": 4, "SMALL": 5, "STEGO": 6}

SPECIES = [
    # name, variant, real shoulder m, head, back, tusks, large tusks, coat, earSize
    ("woolly",                 0, 3.0,  "DOME",  "SLOPE", "SPIRAL",   True,  True,  0),
    ("columbian",              1, 3.8,  "DOME",  "SLOPE", "SPIRAL",   True,  False, 0),
    ("steppe_mammoth",         2, 4.0,  "DOME",  "SLOPE", "SPIRAL",   True,  True,  0),
    ("mastodon",               3, 2.8,  "FLAT",  "LEVEL", "UPCURVE",  True,  False, 0),
    ("straight_tusked",        4, 4.0,  "CREST", "LEVEL", "STRAIGHT", True,  False, 1),
    ("cuvieronius",            5, 2.3,  "FLAT",  "LEVEL", "TWIST",    False, False, 0),
    ("african_elephant",       6, 3.2,  "SLOPE", "DIP",   "FORWARD",  False, False, 2),
    ("asian_elephant",         7, 2.75, "TWIN",  "ARCH",  "SMALL",    False, False, 1),
    ("notiomastodon",          8, 2.7,  "FLAT",  "LEVEL", "UPCURVE",  False, False, 0),
    ("paleoloxodon_namadicus", 9, 4.5,  "CREST", "LEVEL", "STRAIGHT", True,  False, 1),
    ("pygmy_mammoth",         10, 1.7,  "DOME",  "SLOPE", "SPIRAL",   False, False, 0),
    ("cretan_dwarf",          11, 1.0,  "CREST", "LEVEL", "STRAIGHT", False, False, 1),
    # 2.9.0: Stegodon (Stegodontidae) -- the STEGO tusk family; orientalis at Asian-elephant size (an estimate),
    # florensis as the Late Pleistocene dwarf S. f. insularis found with Homo floresiensis
    ("stegodon_orientalis",   12, 2.6,  "DOME",  "LEVEL", "STEGO",    True,  False, 1),
    ("stegodon_florensis",    13, 1.3,  "DOME",  "LEVEL", "STEGO",    False, False, 1),
]
LARGE_TUSK = 1.35

# ---- per-family settings applied in setupAnim (the Java port emits these tables) ----
# topline Y scales about each box's own pivot: (withers, back, croup)
BACK_SCALE = {"SLOPE": (1.0, 0.8, 0.55), "DIP": (1.0, 0.88, 0.96), "ARCH": (0.86, 1.55, 0.8), "LEVEL": (1.0, 1.22, 1.0)}
# extra head pitch / neck pitch (deg, world rx; + lifts the -Z end, i.e. tilts a face back / raises the head)
HEAD_PITCH = {"DOME": (0.0, 0.0), "TWIN": (0.0, -3.0), "SLOPE": (14.0, 2.0), "CREST": (0.0, 0.0), "FLAT": (8.0, -6.0)}
# ear: (yaw deg, Y scale, Z scale, pivot y, pivot z) by earSize
# The ear box is AUTHORED at the African size (23.8 x 18) and SHRUNK for smaller ears: setScale never touches UVs, so a
# mammoth-sized ear stretched 3.4x for the African showed texels 3.6x too big; shrinking a big ear only makes them finer.
EAR_H, EAR_L = 23.8, 18.0
EAR = {0: (24.0, 7.0 / EAR_H, 5.0 / EAR_L, 44.0, -34.5), 1: (26.0, 14.0 / EAR_H, 10.0 / EAR_L, 45.0, -34.0),
       2: (30.0, 1.0, 1.0, 47.5, -34.0)}

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
    """Decompose M = Rz(c) Ry(b) Rx(a) (Blockbench's order) into degrees (a, b, c)."""
    b = math.asin(max(-1.0, min(1.0, -M[2][0])))
    a = math.atan2(M[2][1], M[2][2])
    c = math.atan2(M[1][0], M[0][0])
    return math.degrees(a), math.degrees(b), math.degrees(c)


def rot_from_down(d):
    """Minimal rotation taking the local DOWN axis (0,-1,0) onto direction d (Rodrigues)."""
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


def world_rot(name):
    R = np.eye(3)
    for rec in reversed(ancestry(name)):
        R = R @ rmat(*rec[3])
    return R


def world(name, pts, over=None):
    """over: {name: (rot override, scale (sx,sy,sz) or None, pivot override or None)} -- baked family settings."""
    over = over or {}
    p = np.asarray(pts, dtype=float).reshape(-1, 3)
    for nm, _, o, r, *_ in ancestry(name):
        ov = over.get(nm, {})
        if nm == name and "o" in ov:   # a moved rotation point carries its box (Java addBox is pivot-relative)
            p = p + (np.asarray(ov["o"], float) - np.asarray(o, float))
        o = np.asarray(ov.get("o", o), float)
        if nm == name and ov.get("s") is not None:
            p = (p - o) * np.asarray(ov["s"], float) + o
        p = (rmat(*ov.get("r", r)) @ (p - o).T).T + o
    return p


def local_of(name, pts, over=None):
    over = over or {}
    p = np.asarray(pts, float).reshape(-1, 3)
    for nm, _, o, r, *_ in reversed(ancestry(name)):
        ov = over.get(nm, {})
        o = np.asarray(ov.get("o", o), float)
        p = (rmat(*ov.get("r", r)).T @ (p - o).T).T + o
        if nm == name and ov.get("s") is not None:
            p = (p - o) / np.asarray(ov["s"], float) + o
    return p


def corners(p):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    return np.array([[x, y, z] for x in (a[0], b[0]) for y in (a[1], b[1]) for z in (a[2], b[2])])


def vol(p, n=7):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    ax = [np.linspace(a[i], b[i], n) if b[i] - a[i] > 1e-6 else np.array([a[i]]) for i in range(3)]
    return np.stack(np.meshgrid(*ax, indexing="ij"), -1).reshape(-1, 3)


def dchain(prefix, parent, base, segs, dirs, cls, mirror=True):
    """A chain hanging DOWN from `base`, authored by world direction per segment (relative to the parent's frame).
    segs: (L, w, d, ov); dirs: direction of each segment for the LEFT side (x mirrored for the right)."""
    sides = (("left", -1.0), ("right", 1.0)) if mirror else (("", 1.0),)
    for side, s in sides:
        bx, by, bz = base
        x0 = s * bx if mirror else bx
        par, y, Wp = parent, by, np.eye(3)
        for k, ((L, w, d, ov), dv) in enumerate(zip(segs, dirs)):
            n = f"{prefix}_{side}_{k + 1}" if mirror else f"{prefix}_{k + 1}"
            # dirs are authored for the LEFT side, whose outward is -x; the right side mirrors x
            dd = np.array([dv[0] if s < 0 else -dv[0], dv[1], dv[2]]) if mirror else np.asarray(dv, float)
            W = rot_from_down(dd)
            loc = euler_zyx(Wp.T @ W)
            add(n, par, (x0, y, bz), loc, (x0 - w / 2, y - L, bz - d / 2), (x0 + w / 2, y + ov, bz + d / 2), cls)
            par, Wp, y = n, W, y - L


# ================================================================ chain shapes (LEFT side, head frame)
# widths step >= 0.2 per side so nearly collinear segments (STRAIGHT) do not share a side plane at the joints
TUSK_SEGS = [(6.0, 2.9, 2.9, 0.8), (6.0, 2.5, 2.5, 0.9), (5.5, 2.1, 2.1, 0.9), (5.0, 1.8, 1.8, 0.9), (4.5, 1.5, 1.5, 0.8)]
TUSK_DIRS = {
    # out = -x for the left tusk; forward = -z; up = +y
    "SPIRAL":   [(-0.35, -0.85, -0.40), (-0.45, -0.40, -0.80), (-0.25, 0.25, -0.94), (0.30, 0.65, -0.70), (0.75, 0.62, -0.20)],
    "FORWARD":  [(-0.15, -0.90, -0.40), (-0.18, -0.62, -0.76), (-0.14, -0.25, -0.96), (-0.06, 0.10, -0.99), (0.03, 0.35, -0.94)],
    "STRAIGHT": [(-0.06, -0.78, -0.62), (-0.04, -0.66, -0.75), (-0.01, -0.55, -0.83), (0.02, -0.44, -0.90), (0.05, -0.30, -0.95)],
    "UPCURVE":  [(-0.14, -0.78, -0.61), (-0.14, -0.42, -0.90), (-0.11, -0.06, -0.99), (-0.06, 0.26, -0.96), (0.0, 0.48, -0.88)],
    "TWIST":    [(-0.22, -0.82, -0.53), (-0.42, -0.30, -0.86), (-0.30, 0.22, -0.93), (0.02, 0.48, -0.88), (0.30, 0.50, -0.81)],
    "SMALL":    [(-0.12, -0.92, -0.38), (-0.14, -0.60, -0.79), (-0.10, -0.25, -0.96), (-0.06, 0.10, -0.99), (0.03, 0.35, -0.94)],
    # Stegodon: long, nearly straight, forward, gently upcurved, CLOSE-SET. They run down past the hanging trunk and
    # converge only AHEAD of it (scripts/stegodon_tusk_design.py: trunk overlap <=2% at a 0.25u sway margin; inner gap
    # 4.0u at 1.0 and 0.9u at hasLargeTusks 1.35 -- nearly touching, never crossing; segments 3->4 turn 9 deg of yaw,
    # since a pitch-only bend there left their X side planes coplanar)
    "STEGO":    [(0.02, -0.74, -0.67), (0.14, -0.50, -0.85), (0.30, -0.22, -0.93), (0.14, 0.02, -0.99), (0.0, 0.20, -0.98)],
}
SMALL_SHOWN = 3                     # SMALL: Asian tusks -- only the first three segments are shown
TRUNK_SEGS = [(7.0, 9.2, 5.6, 1.6), (6.0, 7.8, 4.8, 1.2), (6.0, 7.2, 4.2, 1.1), (5.5, 6.2, 3.7, 1.0),
              (5.0, 5.2, 3.2, 1.0), (4.0, 4.4, 2.8, 0.9)]
TRUNK_DIRS = [(0, -0.84, -0.54), (0, -0.95, -0.30), (0, -1.0, -0.06), (0, -1.0, 0.08), (0, -0.97, 0.24), (0, -0.88, 0.47)]


def build():
    PARTS.clear()
    BY_NAME.clear()
    # ---- BARREL: nested, side planes stepping in from the widest middle (mid 11.0 > hips 10.4 > chest 10.0 > rump 9.0) ----
    add("body_mid", None, (0, 34, 0), (0, 0, 0), (-11.0, 21.5, -13.0), (11.0, 40.5, 12.0), "body")
    add("body_chest", "body_mid", (0, 34, -10), (0, 0, 0), (-10.0, 24.0, -28.0), (10.0, 41.5, -8.0), "body")
    add("body_hips", "body_mid", (0, 34, 9), (0, 0, 0), (-10.4, 25.0, 8.0), (10.4, 41.0, 21.0), "body")
    # the rump's front-top corner must stay UNDER every family's croup (it rose 1u above the Asian's and read as a bump)
    add("body_rump", "body_hips", (0, 43, 18), (24, 0, 0), (-9.0, 27.0, 17.0), (9.0, 43.3, 24.5), "body")
    # ---- TOPLINE: tall boxes buried in the barrel, only their tops emerge; heights set per back family. The barrel
    # tops sit at ~41 so THESE boxes alone make the silhouette (at 44-45 the barrel flattened every back family) ----
    add("body_withers", "body_chest", (0, 39, -17), (0, 0, 0), (-8.0, 34.0, -26.5), (8.0, 48.0, -7.5), "body")
    add("body_back", "body_mid", (0, 39, 0), (0, 0, 0), (-8.5, 34.0, -10.0), (8.5, 46.0, 9.4), "body")
    # the croup peaks at the hip point and FALLS toward the tail (tilted, shorter): as a flat block running to the rump
    # it read as a bump on the African's rear (user, 2026-11-15)
    add("body_croup", "body_hips", (0, 39, 12), (7, 0, 0), (-8.2, 34.0, 6.5), (8.2, 47.0, 18.5), "body")
    add("body_hump", "body_withers", (0, 45, -17), (12, 0, 0), (-7.2, 41.0, -25.0), (7.2, 52.5, -9.0), "hump")
    # ---- TAIL: hangs to the hocks, flat tuft ----
    add("tail", "body_rump", (0, 40.5, 24.8), (-8, 0, 0), (-1.2, 25.0, 24.0), (1.2, 41.0, 25.6), "tail")
    add("tail_tuft", "tail", (0, 25.5, 24.8), (0, 0, 0), (-2.0, 19.5, 24.3), (2.0, 26.0, 25.3), "tail")
    # ---- NECK + HEAD ----
    add("neck", "body_chest", (0, 38, -26), (0, 0, 0), (-7.0, 30.0, -32.5), (7.0, 44.0, -25.0), "head")
    add("head", "neck", (0, 40, -31), (0, 0, 0), (-7.5, 26.0, -42.0), (7.5, 46.0, -30.0), "head")
    add("head_dome", "head", (0, 45, -36), (-6, 0, 0), (-6.0, 42.5, -41.0), (6.0, 51.0, -31.0), "dome")
    add("head_dome_left", "head", (-3.4, 45, -36), (0, 0, 0), (-6.4, 43.0, -40.5), (-0.8, 49.5, -32.0), "twin")
    add("head_dome_right", "head", (3.4, 45, -36), (0, 0, 0), (0.8, 43.0, -40.5), (6.4, 49.5, -32.0), "twin")
    add("head_crest", "head", (0, 45, -38), (14, 0, 0), (-6.8, 41.5, -45.0), (6.8, 48.5, -35.0), "crest")
    add("jaw", "head", (0, 27.5, -34), (-12, 0, 0), (-3.4, 23.5, -40.5), (3.4, 27.8, -33.0), "head")
    for side, s in (("left", -1.0), ("right", 1.0)):
        # the fleshy bulge the tusk leaves the face through. Tusk root faces: x 4.85-7.75 at 1.0, 4.34-8.26 at 1.35;
        # trunk root 4.6, skull side 7.5 -- every pair >= 0.25 apart, sheath proud of the skull side and bottom
        add(f"tusk_sheath_{side}", "head", (s * 6.3, 29, -40), (0, 0, 0),
            (s * 4.05, 25.4, -43.5), (s * 8.55, 31.5, -38.0), "head")
        add(f"eye_{side}", "head", (s * 7.85, 37.5, -38), (0, 0, 0), (s * 7.85, 36.5, -39.5), (s * 7.85, 38.5, -36.5), "eye")
        ey = EAR[2]
        add(f"ear_{side}", "head", (s * 7.6, ey[3], ey[4]), (0, s * ey[0], 0),
            (s * 7.6, ey[3] - EAR_H, ey[4]), (s * 8.8, ey[3], ey[4] + EAR_L), "ear")
    # ---- TRUNK: hangs from the face, root flared, tip curling back; ~1u overlap at every joint ----
    dchain("trunk", "head", (0, 33.5, -43.0), TRUNK_SEGS, TRUNK_DIRS, "trunk", mirror=False)
    # ---- TUSKS: out of the sheaths, default shape FORWARD (families swap the local angles in setupAnim) ----
    dchain("tusk", "head", (6.3, 27.5, -42.0), TUSK_SEGS, TUSK_DIRS["FORWARD"], "tusk")
    # ---- LEGS: near-square columns, outer faces stepping inboard (chest 10.0 > 9.6 > 9.2; foot pad 9.4) ----
    for side, s in (("left", -1.0), ("right", 1.0)):
        fx = s * 5.8
        add(f"leg_front_{side}_1", "body_chest", (fx, 34, -19), (0, 0, 0), (fx - 3.8, 21.0, -23.2), (fx + 3.8, 37.0, -14.8), "leg")
        add(f"leg_front_{side}_2", f"leg_front_{side}_1", (fx, 21.5, -19), (0, 0, 0), (fx - 3.4, 5.0, -22.8), (fx + 3.4, 22.5, -15.2), "leg")
        add(f"foot_front_{side}", f"leg_front_{side}_2", (fx, 5.5, -19), (0, 0, 0), (fx - 3.6, 0.0, -23.2), (fx + 3.6, 6.0, -14.6), "foot")
        hx = s * 6.0
        add(f"leg_back_{side}_1", "body_hips", (hx, 35, 14), (0, 0, 0), (hx - 3.6, 20.0, 10.0), (hx + 3.6, 38.0, 18.0), "leg")
        add(f"leg_back_{side}_2", f"leg_back_{side}_1", (hx, 20.5, 14), (0, 0, 0), (hx - 3.2, 5.0, 10.4), (hx + 3.2, 21.8, 17.6), "leg")
        add(f"foot_back_{side}", f"leg_back_{side}_2", (hx, 5.5, 14), (0, 0, 0), (hx - 3.4, 0.0, 9.8), (hx + 3.4, 6.0, 18.4), "foot")
    # ---- COAT (woolly, steppe): ruff behind the head, mane over the hump and back, flank skirts, chest bib, rump ----
    add("fur_neck", "neck", (0, 38, -28), (0, 0, 0), (-8.8, 27.0, -30.6), (8.8, 47.5, -23.0), "fur")
    # long axis on local Y, rotated +90 more about X (local +y -> world +z, local -z -> world +y): box UV reserves
    # 2(w+d) x (h+d), and laid out naturally its 34-long depth cost 100x44 px; this way 51x44
    add("fur_mane", "body_withers", (0, 47, -18), (104, 0, 0), (-9.1, 39.0, -24.5), (9.1, 73.0, -15.0), "fur")
    add("fur_chest", "body_chest", (0, 26, -24), (0, 0, 0), (-7.2, 17.0, -28.8), (7.2, 26.6, -20.0), "fur")
    add("fur_rump", "body_rump", (0, 36, 22), (0, 0, 0), (-9.9, 28.0, 21.0), (9.9, 44.0, 26.8), "fur")
    for side, s in (("left", -1.0), ("right", 1.0)):
        # long axis on local X, yawed 90 so it runs nose-to-tail (local x -> world -z): footprint 86x21 instead of 86x62
        add(f"fur_skirt_{side}", "body_mid", (s * 11.8, 34, -3), (0, 90, s * 4), (s * 11.8 - 21.0, 16.0, -3.4), (s * 11.8 + 21.0, 36.0, -2.6), "fur")


build()
FAMILY_PARTS = {"dome": "DOME", "twin": "TWIN", "crest": "CREST", "hump": "SLOPE"}


def config(sp):
    """Baked overrides + hidden parts for one species: exactly what setupAnim will do in game."""
    name, var, real, head, back, tusk, large, coat, ear = sp
    hide = set()
    for p in PARTS:
        if p[6] == "dome" and head != "DOME":
            hide.add(p[0])
        if p[6] == "twin" and head != "TWIN":
            hide.add(p[0])
        if p[6] == "crest" and head != "CREST":
            hide.add(p[0])
        if p[6] == "hump" and back != "SLOPE":
            hide.add(p[0])
        if p[6] == "fur" and not coat:
            hide.add(p[0])
    if tusk == "SMALL":
        for side in ("left", "right"):
            for k in range(SMALL_SHOWN + 1, 6):
                hide.add(f"tusk_{side}_{k}")
    over = {}
    sw, sb, sc = BACK_SCALE[back]
    over["body_withers"] = {"s": (1, sw, 1)}
    over["body_back"] = {"s": (1, sb, 1)}
    over["body_croup"] = {"s": (1, sc, 1)}
    hp, np_ = HEAD_PITCH[head]
    over["head"] = {"r": (BY_NAME["head"][3][0] + hp, 0, 0)}
    over["neck"] = {"r": (BY_NAME["neck"][3][0] + np_, 0, 0)}
    yaw, ys, zs, py, pz = EAR[ear]
    for side, s in (("left", -1.0), ("right", 1.0)):
        over[f"ear_{side}"] = {"r": (0, s * yaw, 0), "s": (1, ys, zs), "o": (s * 7.6, py, pz)}
        Wp = np.eye(3)
        for k, dv in enumerate(TUSK_DIRS[tusk]):
            dd = np.array([dv[0] if s < 0 else -dv[0], dv[1], dv[2]])
            W = rot_from_down(dd)
            n = f"tusk_{side}_{k + 1}"
            over[n] = {"r": euler_zyx(Wp.T @ W)}
            Wp = W
        if large:
            over[f"tusk_{side}_1"]["chain_scale"] = LARGE_TUSK
    return over, hide


def chain_scale_world(name, pts, over):
    """world() plus the hasLargeTusks chain scale about tusk_*_1's pivot (setShouldScaleChildren)."""
    p = np.asarray(pts, float).reshape(-1, 3)
    anc = ancestry(name)
    for nm, _, o, r, *_ in anc:
        ov = over.get(nm, {})
        if nm == name and "o" in ov:
            p = p + (np.asarray(ov["o"], float) - np.asarray(o, float))
        o = np.asarray(ov.get("o", o), float)
        if nm == name and ov.get("s") is not None:
            p = (p - o) * np.asarray(ov["s"], float) + o
        if ov.get("chain_scale"):
            p = (p - o) * ov["chain_scale"] + o
        p = (rmat(*ov.get("r", r)) @ (p - o).T).T + o
    return p


def shown_parts(sp):
    over, hide = config(sp)
    return [p for p in PARTS if p[0] not in hide], over


def bounds(sp):
    parts, over = shown_parts(sp)
    pts = {p[0]: chain_scale_world(p[0], vol(p, 5), over) for p in parts}
    return pts, over


def withers_height(sp):
    pts, _ = bounds(sp)
    names = ["body_withers"] + (["body_hump"] if sp[4] == "SLOPE" else [])
    return max(pts[n][:, 1].max() for n in names if n in pts)


# ================================================================ reports
def main():
    print(f"{len(PARTS)} boxes")
    pts, _ = bounds(SPECIES[6])
    allp = np.vstack(list(pts.values()))
    lo, hi = allp.min(0), allp.max(0)
    L = max(p[:, 2].max() for n, p in pts.items() if n.startswith("body")) - min(p[:, 2].min() for n, p in pts.items() if n.startswith("body"))
    W = max(p[:, 0].max() for n, p in pts.items() if n.startswith("body_mid"))
    print(f"body length (chest front to rump) {L:.1f}u, width {2 * W:.1f}u -> width:length {2 * W / L:.2f} (elephants ~0.40-0.45; old rig 0.60)")
    for n in ("foot_front_left", "foot_back_left"):
        print(f"  {n:<18} lowest y {pts[n][:, 1].min():+.2f}")
    print(f"  belly {pts['body_mid'][:, 1].min():.1f}u, chest bottom {pts['body_chest'][:, 1].min():.1f}u")
    print()
    print(f"{'species':<24}{'withers u':>10}{'scale':>8}{'height m':>9}{'old m':>7}  topline (withers/back/croup)  "
          f"head top  tusk tip y  trunk tip y  lowest")
    OLD = {"woolly": 1.0, "columbian": 1.278, "steppe_mammoth": 1.381, "mastodon": 1.0, "straight_tusked": 1.312,
           "cuvieronius": 0.8, "african_elephant": 1.05, "asian_elephant": 0.9, "notiomastodon": 0.95,
           "paleoloxodon_namadicus": 1.554, "pygmy_mammoth": 0.55, "cretan_dwarf": 0.45}
    for sp in SPECIES:
        pts, over = bounds(sp)
        wh = withers_height(sp)
        s = sp[2] * 16.0 / wh
        tb = [pts[n][:, 1].max() for n in ("body_withers", "body_back", "body_croup")]
        headtop = max(pts[n][:, 1].max() for n in pts if BY_NAME[n][6] in ("head", "dome", "twin", "crest") and n != "jaw")
        tusk = min(pts[n][:, 1].min() for n in pts if n.startswith("tusk_left"))
        trunk = pts["trunk_6"][:, 1].min()
        low = min(p[:, 1].min() for p in pts.values())
        old_m = OLD.get(sp[0], 0.0) * 45.0 / 16.0
        flag = "  <== SHRINKS" if sp[2] < old_m - 0.05 else ""
        print(f"{sp[0]:<24}{wh:>10.1f}{s:>8.3f}{sp[2]:>9.2f}{old_m:>7.2f}  {tb[0]:5.1f}/{tb[1]:5.1f}/{tb[2]:5.1f}"
              f"{'':12}{headtop:6.1f}  {tusk:9.1f}  {trunk:10.1f}  {low:+.2f}{flag}")
    print()
    ear_report()
    eye_report()


def ear_report():
    print("EARS (stand-off from the skull/neck/chest; inner face x vs the widest nearby box):")
    for size in (0, 1, 2):
        sp = next(s for s in SPECIES if s[8] == size)
        pts, over = bounds(sp)
        e = pts["ear_left"]
        h = e[:, 1].max() - e[:, 1].min()
        wh = withers_height(sp)
        back = e[np.argmax(e[:, 2])]
        print(f"  earSize {size} ({sp[0]}): {h:.1f}u tall = {h / wh:.0%} of withers, back edge at x {back[0]:+.1f} "
              f"(head side -7.5), lowest y {e[:, 1].min():.1f}")


def eye_report():
    c = np.mean([np.asarray(BY_NAME["eye_left"][4]), np.asarray(BY_NAME["eye_left"][5])], axis=0)
    for anc in ("head", "neck", "tusk_sheath_left", "ear_left"):
        p = BY_NAME[anc]
        l = local_of(anc, c)[0]
        a, b = np.asarray(p[4], float), np.asarray(p[5], float)
        gap = max(max(a[i] - l[i], l[i] - b[i]) for i in range(3))
        print(f"  eye_left centre vs {anc:<18} outside by {gap:+.2f}" + ("  <== INSIDE/TOO CLOSE" if gap < 0.3 else ""))


def footprint(p):
    a, b = np.asarray(p[4], float), np.asarray(p[5], float)
    w, h, d = (abs(b[i] - a[i]) for i in range(3))
    return int(math.ceil(2 * (w + d))), int(math.ceil(h + d))


def uvpack():
    """Skyline bottom-left pack of the TRUE box-UV footprints (MC reserves 2(w+d) x (h+d) from texOffs regardless of
    intent), tallest first; zero overlaps asserted. A shelf pack wasted ~25% on this mix of big body boxes and slivers."""
    W, H = CANVAS
    items = sorted(((footprint(p), p[0]) for p in PARTS), key=lambda it: (-it[0][1], -it[0][0]))
    sky = [0] * W
    placed = {}
    for (fw, fh), name in items:
        best = None
        for x in range(0, W - fw + 1):
            y = max(sky[x:x + fw])
            if best is None or y < best[1] or (y == best[1] and x < best[0]):
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
    """Default: the full rig (every family box) for the UV/paint project. With a species, its in-game configuration
    BAKED into plain geometry (hidden parts dropped, family angles / scales / ear pivot applied) for grey previews."""
    import io
    import json
    from contextlib import redirect_stdout
    species = species or os.environ.get("PROBO_SPECIES")
    with redirect_stdout(io.StringIO()):
        placed = uvpack()
    out = []
    if species:
        sp = next(s for s in SPECIES if s[0] == species)
        over, hide = config(sp)
    else:
        over, hide = {}, set()
    for name, parent, o, r, a, b, cls in PARTS:
        if name in hide:
            continue
        ov = over.get(name, {})
        o2 = ov.get("o", o)
        r2 = ov.get("r", r)
        a2, b2 = np.asarray(a, float), np.asarray(b, float)
        if ov.get("s") is not None:
            sc = np.asarray(ov["s"], float)
            oo = np.asarray(o2, float)
            if "o" in ov:   # box authored about the default pivot: move it with the pivot first
                d = oo - np.asarray(o, float)
                a2, b2 = a2 + d, b2 + d
            a2, b2 = (a2 - oo) * sc + oo, (b2 - oo) * sc + oo
            a2, b2 = np.minimum(a2, b2), np.maximum(a2, b2)
        u, v, _, _ = placed[name]
        out.append({"n": name, "p": parent, "o": [round(float(x), 4) for x in o2], "r": [round(float(x), 4) for x in r2],
                    "f": [round(float(x), 4) for x in a2], "t": [round(float(x), 4) for x in b2], "uv": [u, v], "c": cls})
    if species:
        # hasLargeTusks: bake the chain scale about each tusk root's pivot into every tusk box below it (rest frame)
        for side in ("left", "right"):
            root = over.get(f"tusk_{side}_1", {})
            cs = root.get("chain_scale")
            if not cs:
                continue
            piv = np.asarray(BY_NAME[f"tusk_{side}_1"][2], float)
            for rec in out:
                if rec["n"].startswith(f"tusk_{side}_"):
                    for key in ("o", "f", "t"):
                        if key == "o" and rec["n"] == f"tusk_{side}_1":
                            continue
                        rec[key] = [round(float(piv[i] + (rec[key][i] - piv[i]) * cs), 4) for i in range(3)]
    print(json.dumps({"canvas": list(CANVAS), "parts": out}, separators=(",", ":")))


# ================================================================ phase 7: SOLVED rest / sleep (sternal recumbency)
# The body sinks until the belly is BELLY_CLEAR off the ground. At that height a folded knee no longer fits, so the
# legs take the posture the July rig settled on and the user approved: forelegs stretched forward, hind legs straight
# back, every foot within ~0.1u of the ground; the tail lies back along the ground. The legs and tail are identical on
# every species, so they are solved ONCE. The head is not: the tusk shape and size decide how far the head must lift
# to keep the tusks out of the ground (rest) or let them just rest on it (sleep), so that lift is solved per species
# and folded into a table keyed by (tuskShape, large) -- the model reads the flags, not the variant.
BELLY_CLEAR = 0.8
LEG_NAMES = {"fore": ["leg_front_left_1", "leg_front_left_2", "foot_front_left"],
             "hind": ["leg_back_left_1", "leg_back_left_2", "foot_back_left"]}
TRUNK = [f"trunk_{k}" for k in range(1, 7)]
# trunk curl in each pose (BB local rx deltas added to the standing chain, segments 1..6): resting awake it reaches
# the ground and curls forward; asleep it lies forward along the ground
TRUNK_CURL = {"sit": (0.0, 0.0, 35.0, 18.0, 15.0, 15.0), "sleep": (0.0, 30.0, 25.0, 5.0, 0.0, 0.0)}
HEAD_GOAL = {"sit": (2.0, None), "sleep": (0.15, 0.6)}     # (lowest tusk/trunk clearance, max gap for sleep)


def body_drop():
    sp = SPECIES[6]
    pts, _ = bounds(sp)
    return BELLY_CLEAR - min(pts[n][:, 1].min() for n in ("body_mid", "body_chest", "body_hips"))


def _posed(sp, rest, dy, names=None):
    over, hide = config(sp)
    over = {k: dict(v) for k, v in over.items()}
    for n, r in rest.items():
        cur = over.get(n, {})
        base = cur.get("r", BY_NAME[n][3])
        cur["r"] = (base[0] + r[0], base[1] + r[1], base[2] + r[2])
        over[n] = cur
    out = {}
    for p in PARTS:
        if p[0] in hide or (names and p[0] not in names):
            continue
        q = chain_scale_world(p[0], vol(p, 6), over)
        q[:, 1] += dy
        out[p[0]] = q
    return out


def _leg_score(v, dy):
    rest = {}
    for side in ("left", "right"):
        rest[f"leg_front_{side}_1"] = (v[0], 0, 0)
        rest[f"leg_front_{side}_2"] = (v[1], 0, 0)
        rest[f"foot_front_{side}"] = (v[2], 0, 0)
        rest[f"leg_back_{side}_1"] = (v[3], 0, 0)
        rest[f"leg_back_{side}_2"] = (v[4], 0, 0)
        rest[f"foot_back_{side}"] = (v[5], 0, 0)
    pts = _posed(SPECIES[6], rest, dy, set(LEG_NAMES["fore"] + LEG_NAMES["hind"]))
    pen = 0.0
    for limb in ("fore", "hind"):
        for n in LEG_NAMES[limb]:
            low = pts[n][:, 1].min()
            if low < 0:
                pen += low * low * 400
        pen += (pts[LEG_NAMES[limb][2]][:, 1].min() - 0.05) ** 2 * 4
        # the foot lies flat: its sole (lowest face) level
        f = pts[LEG_NAMES[limb][2]]
        pen += 0.02 * (f[:, 1].max() - f[:, 1].min() - 6.0) ** 2
    pen += 0.0002 * sum(x * x for x in (v[1], v[2], v[4], v[5]))
    return pen, rest


def solve_legs(verbose=True):
    rng = np.random.default_rng(11)
    dy = body_drop()
    best = np.array([85.0, -10.0, -5.0, -85.0, 10.0, 5.0])
    bs, _ = _leg_score(best, dy)
    span = np.array([30.0, 30.0, 30.0, 30.0, 30.0, 30.0])
    for _ in range(10):
        for c in best + rng.normal(0, 1, (500, 6)) * span:
            sc, _ = _leg_score(c, dy)
            if sc < bs:
                bs, best = sc, c.copy()
        span *= 0.6
    _, rest = _leg_score(best, dy)
    if verbose:
        print(f"LEGS  body drop {dy:+.2f}u  fore {best[0]:6.1f} {best[1]:6.1f} {best[2]:6.1f}  hind {best[3]:6.1f} "
              f"{best[4]:6.1f} {best[5]:6.1f}  penalty {bs:.4f}")
    return dy, rest


def solve_tail(dy):
    """Tail swung BACK (negative rx lifts its tip toward +z) just far enough that the tuft lies 0.1 off the ground --
    hanging straight, the 21u tail would end 1.2u underground once the body has dropped."""
    for a in np.arange(0, -100, -1.0):
        pts = _posed(SPECIES[6], {"tail": (a, 0, 0)}, dy, {"tail", "tail_tuft"})
        if min(q[:, 1].min() for q in pts.values()) >= 0.1:
            return float(a)
    return -90.0


def solve_fur(dy):
    """Y squash of each coat panel that would reach below ground lying down (setScale about its own pivot)."""
    sp = SPECIES[0]
    out = {}
    for n in ("fur_skirt_left", "fur_chest", "fur_rump", "fur_neck"):
        for k in np.arange(1.0, 0.2, -0.01):
            over, _ = config(sp)
            over = {kk: dict(v) for kk, v in over.items()}
            cur = over.get(n, {})
            cur["s"] = (1, k, 1)
            over[n] = cur
            q = chain_scale_world(n, vol(BY_NAME[n], 6), over)
            if q[:, 1].min() + dy >= 0.1:
                out[n] = round(float(k), 2)
                break
    out["fur_skirt_right"] = out["fur_skirt_left"]
    return out


def solve_head(sp, pose, dy, legs):
    """Smallest neck lift (deg, split 60/40 neck/head) meeting the pose goal for this species."""
    clear, gap = HEAD_GOAL[pose]
    names = set(TRUNK) | {n for n in BY_NAME if n.startswith("tusk_") or n in ("jaw",)}
    for lift in np.arange(-30.0, 60.0, 0.5):
        rest = dict(legs)
        rest["neck"] = (lift * 0.6, 0, 0)
        rest["head"] = (lift * 0.4, 0, 0)
        for k, n in enumerate(TRUNK):
            rest[n] = (TRUNK_CURL[pose][k], 0, 0)
        pts = _posed(sp, rest, dy, names)
        tusk_low = min((q[:, 1].min() for n, q in pts.items() if n.startswith("tusk_")), default=99)
        trunk_low = min(pts[n][:, 1].min() for n in TRUNK if n in pts)
        low = min(tusk_low, trunk_low)
        if pose == "sit" and low >= clear:
            return lift, low
        if pose == "sleep" and low >= clear:
            return lift, low
    return None, None


def pose_solve(verbose=True):
    dy, legs = solve_legs(verbose)
    tail = solve_tail(dy)
    if verbose:
        print(f"TAIL  swung back {tail:.0f} deg")
    fur = solve_fur(dy)
    if verbose:
        print("FUR   squash " + ", ".join(f"{k} {v:.2f}" for k, v in fur.items()))
    # the trunk, not the tusks, limits the head: the lift tracks each HEAD family's own carriage, so it is keyed by
    # headShape (max over the species sharing it) -- tusk shape and size barely move it
    lifts = {}
    for pose in ("sit", "sleep"):
        lifts[pose] = {}
        for sp in SPECIES:
            lift, low = solve_head(sp, pose, dy, legs)
            key = HEAD[sp[3]]
            lifts[pose][key] = max(lifts[pose].get(key, -99), lift)
            if verbose:
                print(f"  {pose:<5} {sp[0]:<24} head {sp[3]:<6} tusks {sp[5]:<8}  lift {lift:6.1f}  lowest {low:+.2f}")
    return dict(dy=dy, legs=legs, tail=tail, fur=fur, lifts=lifts)


# ================================================================ phase 7b: the walk, checked numerically
# Lateral-sequence walk (LH, LF, RH, RF a quarter-cycle apart), Citadel walk(): rotateAngleX += f1*(deg*cos(t+off) + weight).
#   upper  swings +-A about the shoulder/hip (stride ~13u);
#   lower  flexes ONLY in the swing phase: weight = deg makes it deg*f1*(cos+1), zero at mid-stance, peak at mid-swing;
#   foot   counter-rotates both, so the sole stays level.
WALK_SPEED, WALK_AMT = 0.88, 0.4
WALK = {"fore": dict(A=0.50, D=0.60), "hind": dict(A=0.48, D=0.55)}
PHASE = {("hind", "left"): 0.0, ("fore", "left"): -math.pi / 2, ("hind", "right"): math.pi, ("fore", "right"): math.pi / 2}


def walk_terms(limb, side):
    o1 = PHASE[(limb, side)]
    o2 = o1 - math.pi / 2
    A, Dd = WALK[limb]["A"], WALK[limb]["D"]
    n = ("leg_front_" if limb == "fore" else "leg_back_") + side
    foot = ("foot_front_" if limb == "fore" else "foot_back_") + side
    return [(f"{n}_1", A, o1, 0.0), (f"{n}_2", Dd, o2, Dd), (foot, -A, o1, 0.0), (foot, -Dd, o2, -Dd)]


def walk_check(verbose=True):
    worst, lines = {}, []
    for limb in ("fore", "hind"):
        side = "left"
        terms = walk_terms(limb, side)
        names = sorted({t[0] for t in terms}, key=lambda n: ["_1", "_2", "foot"].index(next(x for x in ["_1", "_2", "foot"] if x in n)))
        lows, zs = [], []
        for t in np.linspace(0, 2 * math.pi, 73)[:-1]:
            rest = {}
            for n, deg, off, w in terms:
                d = WALK_AMT * (deg * math.cos(t + off) + w)       # Java radians
                cur = rest.get(n, (0.0, 0, 0))
                rest[n] = (cur[0] - math.degrees(d), 0, 0)        # Java +x -> BB -x
            pts = _posed(SPECIES[6], rest, 0.0, set(names))
            foot = names[-1]
            lows.append(pts[foot][:, 1].min())
            zs.append(pts[foot][:, 2].mean())
        lows, zs = np.array(lows), np.array(zs)
        stance = lows < 0.6
        dz = np.diff(np.r_[zs, zs[0]])
        back = np.mean(dz[stance] > 0) if stance.any() else 0
        worst[limb] = (lows.min(), lows.max(), zs.max() - zs.min(), back)
        if verbose:
            print(f"WALK {limb}: foot lowest {lows.min():+.2f} (dig), highest {lows.max():.2f} (lift), stride {zs.max() - zs.min():.1f}u, "
                  f"planted {stance.mean():.0%} of the cycle, moving BACKWARD {back:.0%} of the planted time")
    for limb in ("hind", "fore"):
        for side in ("left", "right"):
            for n, deg, off, w in walk_terms(limb, side):
                lines.append(f"walk({n}, 0.8F * globalSpeed, {deg:.3f}F, false, {off:.3f}F, {w:.3f}F, f, limbSwingAmount);")
    return worst, lines


def pathlib_root():
    import pathlib
    return pathlib.Path(__file__).resolve().parent.parent


def baked(species):
    """One species' in-game geometry as plain parts (the --json bake), keyed by name."""
    import io
    import json
    from contextlib import redirect_stdout
    buf = io.StringIO()
    with redirect_stdout(buf):
        emit_json(species)
    return {q["n"]: q for q in json.loads(buf.getvalue())["parts"]}


def zfight(species_list=None):
    """SOP phase 4, per species: families change which boxes are shown and how they sit, so every species is swept
    on its own baked geometry. Eyes are excluded (their planes are checked by eye_report)."""
    import zfight_sweep as Z
    for sp in species_list or [s[0] for s in SPECIES]:
        B = baked(sp)
        parts = {}
        for n, q in B.items():
            if q["c"] == "eye":
                continue
            z = Z.Part(n)
            po = np.asarray(B[q["p"]]["o"], float) if q["p"] else np.zeros(3)
            z.rp = np.asarray(q["o"], float) - po
            z.rot = np.array([D(q["r"][0]), D(q["r"][1]), D(q["r"][2])])
            z.lo = np.asarray(q["f"], float) - np.asarray(q["o"], float)
            z.hi = np.asarray(q["t"], float) - np.asarray(q["o"], float)
            z.parent = q["p"]
            parts[n] = z
        hits = list(Z.sweep(parts))
        for area, na, nb, fa, fb, rel in hits:
            print(f"  [{sp}] {area:6.2f}u2  {na:<20} {'XYZ'[fa[0]]}  <->  {nb:<20} {'XYZ'[fb[0]]}{rel}")
        print(f"  [{sp}] z-fight: " + ("clean" if not hits else f"{len(hits)} pair(s)"))


FEATURES = ("ear", "tusk", "trunk", "tail", "dome", "twin", "crest", "hump", "foot")


def burial(species_list=None):
    """Volume fraction of each FEATURE box hidden inside other shown boxes, per species (baked geometry)."""
    for sp in species_list or [s[0] for s in SPECIES]:
        B = baked(sp)
        world_pts, frames = {}, {}
        def wmat(n):
            R, t = np.eye(3), np.zeros(3)
            chain = []
            while n is not None:
                chain.append(B[n]); n = B[n]["p"]
            M = np.eye(4)
            for q in reversed(chain):
                o = np.asarray(q["o"], float)
                T = np.eye(4); T[:3, 3] = o
                Rm = np.eye(4); Rm[:3, :3] = rmat(*q["r"])
                Ti = np.eye(4); Ti[:3, 3] = -o
                M = M @ T @ Rm @ Ti
            return M
        mats = {n: wmat(n) for n in B}
        solids = [n for n in B if B[n]["c"] != "eye"]
        out = []
        for n in solids:
            if B[n]["c"] not in FEATURES:
                continue
            a, b = np.asarray(B[n]["f"], float), np.asarray(B[n]["t"], float)
            ax = [np.linspace(a[i], b[i], 9) for i in range(3)]
            pts = np.stack(np.meshgrid(*ax, indexing="ij"), -1).reshape(-1, 3)
            wp = (mats[n] @ np.c_[pts, np.ones(len(pts))].T).T[:, :3]
            inside = np.zeros(len(wp), bool)
            for m in solids:
                if m == n or m.startswith(n.rsplit("_", 1)[0] + "_") and B[m]["c"] == B[n]["c"]:
                    continue
                Mi = np.linalg.inv(mats[m])
                l = (Mi @ np.c_[wp, np.ones(len(wp))].T).T[:, :3]
                qa, qb = np.asarray(B[m]["f"], float), np.asarray(B[m]["t"], float)
                inside |= np.all((l >= qa + 1e-6) & (l <= qb - 1e-6), axis=1)
            out.append((inside.mean(), n))
        worst = [f"{n} {f:.0%}" for f, n in sorted(out, reverse=True) if f > 0.45]
        print(f"  [{sp}] features >45% buried: " + (", ".join(worst) if worst else "none"))


if __name__ == "__main__":
    if "--uv" in sys.argv:
        uvpack()
    elif "--zfight" in sys.argv:
        zfight(sys.argv[sys.argv.index("--zfight") + 1:] or None)
    elif "--walk" in sys.argv:
        import json
        w, lines = walk_check()
        # REPORT ONLY: the shipped coefficients come from scripts/proboscidean_walk_fit.py (zero planted-foot slide), and
        # writing these unfitted defaults over them once put the slide back into the port
        print("report only; coefficients are fitted by scripts/proboscidean_walk_fit.py -> scratch/probo_walk.json")
    elif "--pose" in sys.argv:
        pose_solve()
    elif "--burial" in sys.argv:
        burial(sys.argv[sys.argv.index("--burial") + 1:] or None)
    elif "--json" in sys.argv:
        i = sys.argv.index("--json")
        emit_json(sys.argv[i + 1] if len(sys.argv) > i + 1 else None)
    else:
        main()
