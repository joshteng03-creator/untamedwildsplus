#!/usr/bin/env python3
"""Fit the ground sloth's walk (SOP phase 7b) against the rebuilt rig, the proboscidean_walk_fit method.

Lateral-sequence walk (LH, LF, RH, RF a quarter-cycle apart). Per limb, Citadel walk() terms
(rotateAngleX += f1*(deg*cos(t+off) + weight)) on the upper segment (swing), the middle segment (flex, biased so it is
mostly in the swing phase) and the foot/hand (counter-rotating both). Fitted to: zero forward slide of a planted foot,
no digging, a visible lift, a stride of at least ~9u. Writes scratch/sloth_walk.json for gen_model_ground_sloth.py.
"""
import json
import math
import pathlib
import sys

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import ground_sloth_spec as S   # noqa: E402

WALK_AMT = 0.4
PHASE = {("hind", "left"): 0.0, ("fore", "left"): -math.pi / 2, ("hind", "right"): math.pi, ("fore", "right"): math.pi / 2}
NAMES = {"fore": ("arm_{s}_1", "arm_{s}_2", "hand_{s}"), "hind": ("leg_{s}_thigh", "leg_{s}_shank", "foot_{s}")}
T = np.linspace(0, 2 * math.pi, 49)[:-1]


def terms(limb, side, A, Dd, dphi, wf):
    o1 = PHASE[(limb, side)]
    o2 = o1 - math.pi / 2 + dphi
    up, mid, foot = (n.format(s=side) for n in NAMES[limb])
    return [(up, A, o1, 0.0), (mid, Dd, o2, Dd * wf), (foot, -A, o1, 0.0), (foot, -Dd, o2, -Dd * wf)]


def run(limb, A, Dd, dphi, wf):
    tt = terms(limb, "left", A, Dd, dphi, wf)
    names = {t[0] for t in tt}
    foot = NAMES[limb][2].format(s="left")
    lows, zs = [], []
    for t in T:
        rot = {}
        for nm, deg, off, w in tt:
            d = WALK_AMT * (deg * math.cos(t + off) + w)        # Java radians; Java +x -> BB -x
            cur = rot.get(nm, (0.0, 0, 0))
            rot[nm] = (cur[0] - math.degrees(d), 0, 0)
        pts = S.posed(S.SPECIES[0], rot, names=names)
        lows.append(pts[foot][:, 1].min())
        zs.append(pts[foot][:, 2].mean())
    lows, zs = np.array(lows), np.array(zs)
    dz = np.diff(np.r_[zs, zs[0]])
    planted = lows < 0.35
    return dict(slide=float(np.sum(np.maximum(0, -dz)[planted])), dig=float(max(0, -lows.min())),
                lift=float(lows.max()), stride=float(zs.max() - zs.min()), planted=float(planted.mean()))


def score(r):
    # planted weighted hard: the first fit bought zero slide with a permanently bent knee and feet on the ground only
    # 17% of the cycle (a hovering gait)
    return r["slide"] * 4 + r["dig"] * 40 + max(0, 1.5 - r["lift"]) * 3 + max(0, 9 - r["stride"]) + abs(r["planted"] - 0.45) * 12


def main():
    rng = np.random.default_rng(5)
    best_all = {}
    for limb in ("fore", "hind"):
        best = np.array([0.45, 0.6, 0.0, 1.0])
        bs = score(run(limb, *best))
        span = np.array([0.15, 0.25, 0.8, 0.4])
        for _ in range(7):
            for c in best + rng.normal(0, 1, (50, 4)) * span:
                c[0] = max(0.15, c[0])
                c[1] = max(0.0, c[1])
                c[3] = min(1.05, max(0.6, c[3]))   # flex bias <= ~1: the knee straightens at mid-stance (no permanent bend)
                sc = score(run(limb, *c))
                if sc < bs:
                    bs, best = sc, c.copy()
            span *= 0.6
        best_all[limb] = best
        print(limb, "A %.3f D %.3f dphi %.3f wf %.3f" % tuple(best), {k: round(v, 3) for k, v in run(limb, *best).items()})
    lines = []
    for limb in ("hind", "fore"):
        for side in ("left", "right"):
            for nm, deg, off, w in terms(limb, side, *best_all[limb]):
                lines.append(f"walk({nm}, 0.8F * globalSpeed, {deg:.3f}F, false, {off:.3f}F, {w:.3f}F, f, limbSwingAmount);")
    out = pathlib.Path(__file__).resolve().parent.parent / "scratch" / "sloth_walk.json"
    out.write_text(json.dumps(lines, indent=1))
    print("wrote", out)


if __name__ == "__main__":
    main()
