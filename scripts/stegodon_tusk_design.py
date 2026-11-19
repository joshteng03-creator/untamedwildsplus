#!/usr/bin/env python3
"""Stegodon tusk family design check (2.9.0) on the rebuilt proboscidean rig.

Stegodon's upper tusks were long, nearly straight, pointing forward and curving gently upward, set parallel and so close
together that they nearly touched (the trunk rested on or beside them, not between). In this rig the trunk hangs from the
face midline, so the tusks may only converge AHEAD of it. For a candidate STEGO direction set this reports, at tusk scale
1.0 and 1.35 (hasLargeTusks):
  * the fraction of each tusk segment's volume inside any trunk segment (standing pose), and the closest x clearance;
  * the gap between the two tusks' inner faces at each segment end (they must not touch or cross: > 0.3u);
  * the lowest tusk point (the ground is y 0; the standing head carriage must keep it well clear).

    python scripts/stegodon_tusk_design.py
"""
import pathlib
import sys

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import proboscidean_spec as S  # noqa: E402

# LEFT tusk (out = -x, so +x is INWARD), head frame, one direction per segment
STEGO = S.TUSK_DIRS["STEGO"]     # the shipped set (first try here was converged too little: 6.2u apart)


def check(dirs, large, head="DOME", back="LEVEL", verbose=True):
    S.TUSK_DIRS["STEGO"] = dirs
    S.TUSK.setdefault("STEGO", 6)
    sp = ("stego", 99, 2.6, head, back, "STEGO", large, False, 1)
    over, hide = S.config(sp)
    trunk = [f"trunk_{k}" for k in range(1, 7)]
    res = {"inside": 0.0, "gap": 99.0, "low": 99.0, "worst": ""}
    for side in ("left", "right"):
        for k in range(1, 6):
            n = f"tusk_{side}_{k}"
            pts = S.chain_scale_world(n, S.vol(S.BY_NAME[n], 7), over)
            res["low"] = min(res["low"], pts[:, 1].min())
            inside = np.zeros(len(pts), bool)
            for t in trunk:
                l = S.local_of(t, pts, over)
                a, b = np.asarray(S.BY_NAME[t][4]), np.asarray(S.BY_NAME[t][5])
                inside |= np.all((l >= a - 0.25) & (l <= b + 0.25), axis=1)   # 0.25u margin = clearance for the sway
            if inside.mean() > res["inside"]:
                res["inside"], res["worst"] = inside.mean(), n
    # inner-face gap: left tusk's max x vs right tusk's min x, per segment
    for k in range(1, 6):
        L = S.chain_scale_world(f"tusk_left_{k}", S.vol(S.BY_NAME[f"tusk_left_{k}"], 7), over)
        R = S.chain_scale_world(f"tusk_right_{k}", S.vol(S.BY_NAME[f"tusk_right_{k}"], 7), over)
        res["gap"] = min(res["gap"], R[:, 0].min() - L[:, 0].max())
        if verbose:
            tip = L[np.argmin(L[:, 2])]
            print(f"    seg {k}: inner gap {R[:, 0].min() - L[:, 0].max():5.2f}u   tip (x {tip[0]:+.1f}, y {tip[1]:.1f}, z {tip[2]:.1f})")
    return res


if __name__ == "__main__":
    for large in (False, True):
        print(f"STEGO tusks, scale {'1.35' if large else '1.0'}:")
        r = check(STEGO, large)
        print(f"  trunk overlap (0.25u margin) worst {r['inside']:.0%} ({r['worst']}), min inner gap {r['gap']:.2f}u, "
              f"lowest tusk y {r['low']:.1f}")
    # the shipped families, for comparison
    for fam in ("STRAIGHT", "FORWARD", "UPCURVE"):
        r = check(S.TUSK_DIRS[fam], False, verbose=False)
        print(f"{fam:<9} 1.0: trunk overlap {r['inside']:.0%}, inner gap {r['gap']:.2f}u, lowest {r['low']:.1f}")
