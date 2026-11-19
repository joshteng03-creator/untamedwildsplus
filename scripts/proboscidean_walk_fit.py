import sys, math, pathlib, io, contextlib
import numpy as np
sys.path.insert(0, r"C:/Users/josht/OneDrive/Desktop/untamed wilds plus/untamedwildsplus/scripts")
import proboscidean_spec as S
T = np.linspace(0, 2 * math.pi, 49)[:-1]
def run(limb, A, D, dphi, wf):
    side = "left"; o1 = S.PHASE[(limb, side)]; o2 = o1 - math.pi / 2 + dphi
    n = ("leg_front_" if limb == "fore" else "leg_back_") + side
    foot = ("foot_front_" if limb == "fore" else "foot_back_") + side
    terms = [(f"{n}_1", A, o1, 0.0), (f"{n}_2", D, o2, D * wf), (foot, -A, o1, 0.0), (foot, -D, o2, -D * wf)]
    lows, zs = [], []
    for t in T:
        rest = {}
        for nm, deg, off, w in terms:
            d = S.WALK_AMT * (deg * math.cos(t + off) + w)
            cur = rest.get(nm, (0.0, 0, 0)); rest[nm] = (cur[0] - math.degrees(d), 0, 0)
        pts = S._posed(S.SPECIES[6], rest, 0.0, {f"{n}_1", f"{n}_2", foot})
        lows.append(pts[foot][:, 1].min()); zs.append(pts[foot][:, 2].mean())
    lows, zs = np.array(lows), np.array(zs)
    dz = np.diff(np.r_[zs, zs[0]])
    planted = lows < 0.15
    slide = float(np.sum(np.maximum(0, -dz)[planted]))
    dig = float(max(0, -lows.min()))
    return dict(slide=slide, dig=dig, lift=float(lows.max()), stride=float(zs.max() - zs.min()), planted=float(planted.mean()), terms=terms)
def score(r):
    return r["slide"] * 4 + r["dig"] * 40 + max(0, 1.8 - r["lift"]) * 3 + max(0, 11 - r["stride"]) + abs(r["planted"] - 0.5) * 3
rng = np.random.default_rng(3)
best_all = {}
for limb in ("fore", "hind"):
    best = np.array([0.5, 0.6, 0.0, 1.0]); bs = score(run(limb, *best))
    span = np.array([0.15, 0.25, 0.8, 0.4])
    for it in range(7):
        for c in best + rng.normal(0, 1, (60, 4)) * span:
            c[0] = max(0.2, c[0]); c[1] = max(0.0, c[1])
            sc = score(run(limb, *c))
            if sc < bs: bs, best = sc, c.copy()
        span *= 0.6
    r = run(limb, *best)
    best_all[limb] = best
    print(limb, "A %.3f D %.3f dphi %.3f wf %.3f" % tuple(best), {k: round(v, 3) for k, v in r.items() if k != "terms"})
import json
lines = []
for limb in ("hind", "fore"):
    A, D, dphi, wf = best_all[limb]
    for side in ("left", "right"):
        o1 = S.PHASE[(limb, side)]; o2 = o1 - math.pi / 2 + dphi
        n = ("leg_front_" if limb == "fore" else "leg_back_") + side
        foot = ("foot_front_" if limb == "fore" else "foot_back_") + side
        for nm, deg, off, w in [(f"{n}_1", A, o1, 0.0), (f"{n}_2", D, o2, D * wf), (foot, -A, o1, 0.0), (foot, -D, o2, -D * wf)]:
            lines.append(f"walk({nm}, 0.8F * globalSpeed, {deg:.3f}F, false, {off:.3f}F, {w:.3f}F, f, limbSwingAmount);")
(S.pathlib_root() / "scratch" / "probo_walk.json").write_text(json.dumps(lines, indent=1))
print("wrote walk lines", len(lines))
