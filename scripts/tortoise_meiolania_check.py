#!/usr/bin/env python3
"""Meiolania parts on ModelTortoise (2.9.0) -- SOP phases 1/4/7 in one checker, run on the java2bb rig so it measures
the shipped Java, not a separate spec.

  * standing: lowest point of every part (ground = 0) and how far the horns stand off the skull / neck / shell;
  * the SIT pose (hide in shell: setupAnim moves body_main down 2.6) -- the tail lift (Java rad, added to tail_1)
    that keeps the club on, not in, the ground;
  * z-fight sweep over the Meiolania configuration (old stub tail hidden).

    python scripts/gen_bb_rig.py ModelTortoise scratch/rig_tortoise.js
    python scripts/tortoise_meiolania_check.py scratch/rig_tortoise.js
"""
import json
import math
import pathlib
import sys

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import zfight_sweep as Z  # noqa: E402

D = math.radians
txt = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")
i = txt.index("const SPEC = ") + len("const SPEC = ")
SPEC = {p["n"]: p for p in json.loads(txt[i:txt.index("};", i) + 1])["parts"]}
NEW = ["horn_left", "horn_right", "tail_1", "tail_2", "tail_club", "club_spike_left", "club_spike_right",
       "club_horn_left", "club_horn_right"]
SHOWN = [n for n in SPEC if n != "body_tail"]


def rmat(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(D(rx)), math.sin(D(rx)), math.cos(D(ry)), math.sin(D(ry)), math.cos(D(rz)), math.sin(D(rz))
    return (np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]) @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
            @ np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]))


def chain(n):
    out = []
    while n:
        out.append(SPEC[n])
        n = SPEC[n]["p"]
    return out


def world(n, extra=None, drop=0.0):
    """extra: {part: (drx, dry, drz) BB degrees} added to the rest rotation; drop: BB units the root moves DOWN."""
    extra = extra or {}
    p = SPEC[n]
    a, b = np.asarray(p["f"], float), np.asarray(p["t"], float)
    pts = np.array([[x, y, z] for x in np.linspace(a[0], b[0], 4) for y in np.linspace(a[1], b[1], 4)
                    for z in np.linspace(a[2], b[2], 4)])
    for q in chain(n):
        o = np.asarray(q["o"], float)
        r = np.asarray(q["r"], float) + np.asarray(extra.get(q["n"], (0, 0, 0)), float)
        pts = (rmat(*r) @ (pts - o).T).T + o
    pts[:, 1] -= drop
    return pts


print("STANDING (ground y = 0):")
for n in NEW + ["leg_left", "hand_left", "body_main"]:
    print(f"  {n:<18} lowest {world(n)[:, 1].min():+.2f}")
# the sit pose: progressPosition(body_main ... 22.6) from a rest y of 20 -> 2.6 down
print("SIT (body 2.6 down): tail_1 lift (Java rad) for tail_2 + club to rest just off the ground:")
best = None
for lift_deg in np.arange(0, 60, 0.5):
    # Java +x rotation lifts a +z-pointing chain; BB rx = -Java rx
    low = min(world(n, {"tail_1": (-lift_deg, 0, 0)}, 2.6)[:, 1].min() for n in NEW[3:])   # tail_1's root is at body height
    if low >= 0.05:
        best = (lift_deg, low)
        break
print(f"  lift {best[0]:.1f} deg = {math.radians(best[0]):.4f} rad, club lowest {best[1]:+.2f}" if best else "  none found")
sit_low = {n: world(n, None, 2.6)[:, 1].min() for n in ("horn_left", "head", "neck")}
print("  head parts sitting: " + ", ".join(f"{k} {v:+.2f}" for k, v in sit_low.items()))

parts = {}
for n in SHOWN:
    q = SPEC[n]
    z = Z.Part(n)
    po = np.asarray(SPEC[q["p"]]["o"], float) if q["p"] else np.zeros(3)
    z.rp = np.asarray(q["o"], float) - po
    z.rot = np.array([D(q["r"][0]), D(q["r"][1]), D(q["r"][2])])
    z.lo = np.asarray(q["f"], float) - np.asarray(q["o"], float)
    z.hi = np.asarray(q["t"], float) - np.asarray(q["o"], float)
    z.parent = q["p"]
    parts[n] = z
hits = [h for h in Z.sweep(parts) if h[1] in NEW or h[2] in NEW]
for area, na, nb, fa, fb, rel in hits:
    print(f"  z-fight {area:5.2f}u2 {na} <-> {nb}{rel}")
print("z-fight (new parts): " + ("clean" if not hits else f"{len(hits)} pair(s)"))
