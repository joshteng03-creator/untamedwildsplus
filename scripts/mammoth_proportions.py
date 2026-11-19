#!/usr/bin/env python3
"""Mammoth-type proportions audit (2026-10-09; user: "the mammoth model looks too wide horizontally in some
scaled models"). Analysis only -- changes nothing.

For every species, bakes the per-species toggles ModelMammoth.setupAnim applies (wool shown/hidden, flatBack
hump 0.55 / dome 0.35 in Y, ear Y/Z scale, tusk subtree 1.35), measures the silhouette in model units with
exact forward kinematics, and reports:
  body width   widest torso/hump/fur box, x            shoulder height  top of the hump/back above the ground
  body w/h     the "how wide does it read" number       overall width    including tusks and ears
and compares to real-animal body-width : shoulder-height ratios (~0.45-0.55 for living elephants).

    python scripts/mammoth_proportions.py
"""
import json
import math
import pathlib
import sys

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from java2bb import parse

ROOT = pathlib.Path(__file__).resolve().parent.parent
ORDER, UV, RP, BOX, ROT, PAR, MIRROR = parse("ModelMammoth")
FUR = ["fur_mane", "fur_neck", "fur_chest", "fur_rump", "fur_skirt_left", "fur_skirt_right"]
TORSO = ["body_hips", "body_chest", "body_hump", "body_rump"]


def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    return (np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]) @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
            @ np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]))


def world_points(part, scale):
    """Box corners in model space (Y DOWN, ground y=24), applying per-part scales (with subtree scaling for tusks)."""
    chain = [part]
    while PAR.get(chain[-1]):
        chain.append(PAR[chain[-1]])
    M = np.eye(4)
    for n in reversed(chain):
        T = np.eye(4)
        T[:3, :3] = rot(*(ROT.get(n) or (0.0, 0.0, 0.0)))
        T[:3, 3] = RP[n]
        S = np.eye(4)
        sc = scale.get(n)
        if sc and (n == part or n in ("tusk_left_1", "tusk_right_1")):
            S[0, 0], S[1, 1], S[2, 2] = sc
        M = M @ T @ S
    bx, by, bz, w, h, d = BOX[part]
    return np.array([(M @ np.array([x, y, z, 1.0]))[:3] for x in (bx, bx + w) for y in (by, by + h) for z in (bz, bz + d)])


def measure(flags):
    wool = flags.get("hasWoollyCoat", 0) == 1
    flat = flags.get("flatBack", 0) == 1
    ear = flags.get("earSize", 0)
    tusk = 1.35 if flags.get("hasLargeTusks", 0) == 1 else 1.0
    scale = {"head_dome": (1, 0.35 if flat else 1, 1), "body_hump": (1, 0.55 if flat else 1, 1),
             "ear_left": (1, {2: 2.2, 1: 1.5}.get(ear, 1.0), {2: 1.9, 1: 1.35}.get(ear, 1.0)),
             "ear_right": (1, {2: 2.2, 1: 1.5}.get(ear, 1.0), {2: 1.9, 1: 1.35}.get(ear, 1.0)),
             "tusk_left_1": (tusk,) * 3, "tusk_right_1": (tusk,) * 3}
    visible = [n for n in ORDER if n in BOX and (wool or n not in FUR)]
    pts = {n: world_points(n, scale) for n in visible}
    body = np.vstack([pts[n] for n in TORSO + (FUR if wool else []) if n in pts])
    allp = np.vstack(list(pts.values()))
    ground = allp[:, 1].max()
    body_w = body[:, 0].max() - body[:, 0].min()
    sh = ground - body[:, 1].min()
    return body_w, sh, allp[:, 0].max() - allp[:, 0].min()


def main():
    data = json.loads((ROOT / "src/main/resources/data/untamedwilds/entities/mammoth.json").read_text(encoding="utf-8-sig"))
    top = data.get("flags", {})
    print("%-24s %5s %8s %8s %8s %9s  %s" % ("species", "scale", "body w", "should h", "body w/h", "overall w", "toggles"))
    for s in data["species"]:
        flags = dict(top)
        flags.update(s.get("flags", {}))
        bw, sh, ow = measure(flags)
        sc = s.get("scale", 1.0)
        tog = ",".join(k for k in ("hasWoollyCoat", "flatBack", "hasLargeTusks") if flags.get(k) == 1) + (",ears%d" % flags.get("earSize", 0) if flags.get("earSize") else "")
        print("%-24s %5.2f %8.1f %8.1f %8.2f %9.1f  %s" % (s["name"], sc, bw * sc, sh * sc, bw / sh, ow * sc, tog))


if __name__ == "__main__":
    main()
