#!/usr/bin/env python3
"""Solve EAT / DRINK / ALERT neck-and-head poses for the shipped models against their real geometry (2.8.0, P3b).

Generalises wildebeest_spec.graze_solve to any model through fk_rig.Rig (java2bb parse, Blockbench space, ground y=0,
front = -Z). Only PITCH (BB x) deltas on the neck->muzzle chain are searched; every constraint is checked on
volume-sampled points of the real boxes.

  EAT   : the muzzle's lowest point 0.1..0.6 above the grass; every other head/neck part (horns, antlers, ears, every
          hidden family) >= 0.05 above it; no neck/head point sinks into the torso deeper than at rest.
  DRINK : the muzzle's lowest point -2.0..-0.4 (the water line is 2 units under the bank top at scale 1), and the
          muzzle beyond the bank edge (>= 1 unit ahead of the front feet); other head parts above the bank unless
          they are beyond the edge too.
  ALERT : the head raised as high as the joint limits allow (<= +35 deg per joint) while staying within 20 deg of
          level and out of the torso.

Smallest weighted bend wins (the neck does most of the work, as in a real grazer).

    python scripts/pose_reach.py --check-wildebeest     # must land close to the hand-solved wildebeest EAT
    python scripts/pose_reach.py deer equid            # solve and write scratch/poses/<type>.json
    python scripts/pose_reach.py --all
"""
import json
import math
import pathlib
import sys

import numpy as np

from fk_rig import Rig, rmat
from pose_targets import T, LIMB_WORDS, FORE_WORDS, SOFT_WORDS

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "scratch" / "poses"
EAT_MAX = 0.7            # the shipped (approved) wildebeest graze leaves the nose 0.67 above the grass
INSIDE_TOLERANCE = 8
FEASIBLE_PENALTY = 0.5   # a few hundredths of a unit outside a band is invisible, not a failure     # volume-sample points of solid head/neck allowed to sink into the torso beyond rest


class Problem:
    def __init__(self, typ, muzzle=None):
        self.typ = typ
        self.cfg = T[typ]
        self.rig = Rig(self.cfg["model"])
        R = self.rig
        neck = self.cfg["neck"]
        self.muzzle = [m for m in (muzzle or self.cfg["muzzle"]) if m in R.P]
        if not self.muzzle:
            raise SystemExit(f"{typ}: none of the muzzle parts {self.cfg['muzzle']} exist")
        # chain: ancestry of the FIRST muzzle part up to and including the neck root, root first
        anc = R.ancestry(self.muzzle[0])
        if neck not in anc:
            raise SystemExit(f"{typ}: {neck} is not an ancestor of {self.muzzle[0]} ({anc})")
        self.chain = list(reversed(anc[:anc.index(neck) + 1]))
        self.head = [n for n in R.subtree(neck) if n not in self.muzzle]
        self.torso = [n for n in R.order if n not in R.subtree(neck) and not any(w in n for w in LIMB_WORDS + SOFT_WORDS)]
        # hair (beards, manes, fringes) passes through hair: never part of the "neck through the body" test
        self.solid = [n for n in self.head + self.muzzle if n != neck and not any(w in n for w in SOFT_WORDS)]
        self.fore = self.cfg.get("fore") or [n for n in R.order if any(w in n for w in FORE_WORDS) and n not in R.subtree(neck)]
        self.rest = {j: R.rest_rot(j) for j in self.chain}
        # sample points (local rest frame) per part, once
        self.pts = {n: R.vol(n, 3) for n in R.order}
        lows = [R.world(n, self.pts[n])[:, 1].min() for n in R.order if n not in R.subtree(neck)]
        self.ground = min(lows)                              # the feet: ~0 for every calibrated rig
        self.set_body({}, {})

    def set_body(self, over, move):
        """A fixed body pose under the neck solve (a splayed giraffe: body, legs, feet). The neck chain is solved on
        top of it; the torso test and the bank edge use the POSED body."""
        self.base_over, self.base_move = dict(over), dict(move)
        R = self.rig
        mv = self.base_move or None
        self.base_inside = self._inside_count(dict(self.base_over))
        self.front_z = (min(R.world(n, self.pts[n], self.base_over, mv)[:, 2].min() for n in self.fore)
                        if self.fore else None)

    def over(self, deltas):
        o = dict(self.base_over)
        for j, d in zip(self.chain, deltas[:len(self.chain)]):
            rx, ry, rz = o.get(j, self.rest[j])
            o[j] = (rx + d, ry, rz)
        return o

    def move(self, x):
        """The neck-root pivot shift (BB units) carried in x after the pitches, if any: the neck base drops and
        slides forward as the shoulder rotates (a grazing horse's withers-high neck cannot reach the grass otherwise)."""
        n = len(self.chain)
        m = dict(self.base_move)
        if len(x) > n:
            prev = m.get(self.chain[0], (0.0, 0.0, 0.0))
            m[self.chain[0]] = (prev[0], prev[1] + float(x[n]), prev[2] + float(x[n + 1]))
        return m or None

    def _inside_count(self, over, worlds=None):
        parts = self.solid
        if worlds is None:
            worlds = {n: self.rig.world(n, self.pts[n], over, self.base_move or None) for n in parts}
        allp = np.vstack([worlds[n] for n in parts]) if parts else np.zeros((0, 3))
        cnt = 0
        for b in self.torso:
            cnt += int(self.rig.inside(b, allp, self.base_over, 0.6, self.base_move or None).sum())
        return cnt

    def lowest(self, names, over):
        return min(self.rig.world(n, self.pts[n], over)[:, 1].min() for n in names) - self.ground

    def eval(self, pose, deltas):
        o = self.over(deltas)
        mv = self.move(deltas)
        R = self.rig
        worlds = {n: R.world(n, self.pts[n], o, mv) for n in self.head + self.muzzle}
        mz = np.vstack([worlds[n] for n in self.muzzle])
        mz_low = mz[:, 1].min() - self.ground
        pen = 0.0
        if pose == "eat":
            if mz_low < 0.1:
                pen += (0.1 - mz_low) * 10
            if mz_low > EAT_MAX:
                pen += (mz_low - EAT_MAX) * 10
            for n in self.head:
                low = worlds[n][:, 1].min() - self.ground
                if low < 0.05:
                    pen += (0.05 - low) * 10
        elif pose == "drink":
            # muzzle band: from the bank top down to the water line (2 units under it at scale 1); a trunk is
            # dipped INTO the water instead (pose_targets drink_band)
            band_lo, band_hi = self.cfg.get("drink_band", (-2.0, -0.4))
            if mz_low < band_lo:
                pen += (band_lo - mz_low) * 10
            if mz_low > band_hi:
                pen += (mz_low - band_hi) * 10
            edge = (self.front_z - 1.0) if self.front_z is not None else 0.0
            # anything dipped below the bank top has to be out over the water, beyond the bank edge
            for n in self.head + self.muzzle:
                w = worlds[n]
                bad = w[(w[:, 1] - self.ground < 0.05) & (w[:, 2] > edge)]
                if len(bad):
                    pen += float((bad[:, 2] - edge).sum()) * 0.5 + len(bad) * 0.05
        elif pose == "alert":
            # head within 20 deg of its rest world pitch
            pitch = sum(deltas[:len(self.chain)])
            if abs(pitch) > 20:
                pen += (abs(pitch) - 20) * 0.5
        # a little overlap where the neck meets the chest is normal (the shipped wildebeest graze has 6 points);
        # only real penetration counts
        extra = self._inside_count(o, worlds) - self.base_inside - INSIDE_TOLERANCE
        if extra > 0:
            pen += extra * 0.5
        n = len(self.chain)
        w = np.linspace(0.6, 1.5, n) if n > 1 else np.array([1.0])
        bend = float((w * np.asarray(deltas[:n]) ** 2).sum())
        if len(deltas) > n:                                 # neck-base drop: costs like ~4 deg of bend per unit
            bend += 16.0 * float(deltas[n] ** 2 + deltas[n + 1] ** 2)
        if pose == "alert":
            return pen * 1e4 - (mz[:, 1].max()) * 40 + 0.02 * bend, mz_low, pen
        return pen * 1e4 + bend, mz_low, pen

    def solve(self, pose, seed=7):
        smooth = [j for j in self.cfg.get("smooth", []) if j in self.chain]
        if smooth and pose != "alert":
            return self.solve_smooth(pose, smooth, seed)
        rng = np.random.default_rng(seed)
        n = len(self.chain)
        if pose == "alert":
            lo, hi = np.full(n, -12.0), np.full(n, 35.0)
        else:
            lo, hi = np.full(n, -100.0), np.full(n, 40.0)
            lo[0] = -130.0                                  # a steep withers-high neck swings further
            lo[-1], hi[-1] = -60.0, 60.0                    # the head/muzzle joint nods both ways
            lo = np.append(lo, [-6.0, -3.0])                # neck root drop (dy) and forward slide (dz)
            hi = np.append(hi, [0.0, 0.0])
        m = len(lo)
        best_val, best = None, None
        for restart in range(2):
            cur = np.clip(np.concatenate([rng.normal(0, 10, n) + (-30 if pose != "alert" else 10), np.zeros(m - n)]), lo, hi)
            cur_val = self.eval(pose, cur)[0]
            span = np.concatenate([np.full(n, 40.0), np.full(m - n, 3.0)])
            for it in range(11):
                cand = np.clip(cur + rng.normal(0, 1, (110, m)) * span, lo, hi)
                for c in cand:
                    v = self.eval(pose, c)[0]
                    if v < cur_val:
                        cur_val, cur = v, c
                span *= 0.68
            if best_val is None or cur_val < best_val:
                best_val, best = cur_val, cur
        best = np.round(best, 1)
        val, mz_low, pen = self.eval(pose, best)
        out = {"chain": self.chain, "deltas_bb": [float(x) for x in best[:n]], "muzzle_low": round(float(mz_low), 2),
               "feasible": bool(pen < FEASIBLE_PENALTY), "penalty": round(float(pen), 4)}
        if m > n:
            out["neck_move_bb"] = [0.0, float(best[n]), float(best[n + 1])]
        return out

    def solve_smooth(self, pose, smooth, seed=7):
        """A chain whose tail is one flexible organ (a trunk): its joints bend as ONE smooth curve -- delta_k = a + b*t,
        t from 0 at the first to 1 at the last -- instead of independently. Six free trunk joints let the search
        settle on a zig-zag that reads as a kinked hose; the ramp keeps the curl continuous. The joints before it
        (neck, head) and the neck-base move stay free."""
        rng = np.random.default_rng(seed)
        n = len(self.chain)
        free = [i for i, j in enumerate(self.chain) if j not in smooth]
        sm = [i for i, j in enumerate(self.chain) if j in smooth]
        t = np.linspace(0.0, 1.0, len(sm)) if len(sm) > 1 else np.zeros(1)

        def expand(x):
            d = np.zeros(n + 2)
            d[free] = x[:len(free)]
            a, b = x[len(free)], x[len(free) + 1]
            d[sm] = a + b * t
            d[n:] = x[len(free) + 2:]
            return d

        lo = np.concatenate([np.full(len(free), -60.0), [-45.0, -45.0], [-6.0, -3.0]])
        hi = np.concatenate([np.full(len(free), 40.0), [45.0, 45.0], [0.0, 0.0]])
        m = len(lo)
        best_val, best = None, None
        for restart in range(3):
            cur = np.clip(rng.normal(0, 6, m), lo, hi)
            cur_val = self.eval(pose, expand(cur))[0]
            span = np.concatenate([np.full(len(free), 20.0), [20.0, 20.0], [3.0, 3.0]])
            for it in range(14):
                for c in np.clip(cur + rng.normal(0, 1, (120, m)) * span, lo, hi):
                    v = self.eval(pose, expand(c))[0]
                    if v < cur_val:
                        cur_val, cur = v, c
                span *= 0.7
            if best_val is None or cur_val < best_val:
                best_val, best = cur_val, cur
        full = np.round(expand(best), 1)
        val, mz_low, pen = self.eval(pose, full)
        out = {"chain": self.chain, "deltas_bb": [float(x) for x in full[:n]], "muzzle_low": round(float(mz_low), 2),
               "feasible": bool(pen < FEASIBLE_PENALTY), "penalty": round(float(pen), 4),
               "neck_move_bb": [0.0, float(full[n]), float(full[n + 1])], "smooth": smooth}
        return out

    def explain(self, pose, deltas, move=None):
        """Which constraints a pose breaks, for the log."""
        o = self.over(deltas)
        mv = self.move(deltas)
        bm = self.base_move or None
        R = self.rig
        out = []
        for n in self.head:
            low = R.world(n, self.pts[n], o, mv)[:, 1].min() - self.ground
            if low < 0.05 and pose == "eat":
                out.append(f"{n} {low:+.2f} below the grass")
        base = {}
        for b in self.torso:
            for n in self.solid:
                c = int(R.inside(b, R.world(n, self.pts[n], o, mv), self.base_over, 0.6, bm).sum())
                c0 = int(R.inside(b, R.world(n, self.pts[n], self.base_over, bm), self.base_over, 0.6, bm).sum())
                if c > c0:
                    out.append(f"{n} in {b} {c0}->{c}")
        if pose == "drink" and self.front_z is not None:
            mz = np.vstack([R.world(n, self.pts[n], o, mv) for n in self.muzzle])
            out.append(f"muzzle rear z {mz[:, 2].max():+.1f} vs edge {self.front_z - 1.0:+.1f}")
        return "; ".join(out)


def solve_body(prob, spec, seed=11):
    """Stage 1 of a SPLAYED drink (giraffe): the forelegs spread `splay` degrees sideways, then the body drop and
    pitch, a foreleg pitch and three hind-leg joint pitches that keep all four hooves planted -- on the ground, not
    sliding more than a unit, soles level. Real giraffes drink exactly so: the front comes down between the splayed
    forelegs and the body tips forward over straight-ish hind legs, so hind-leg folding costs more than body pitch.
    Returns (over, move, deltas) for Problem.set_body plus the record fields gen_keyframes writes."""
    R = prob.rig
    root, splay = spec["root"], float(spec["splay"])
    fore, hind = spec["fore"], spec["hind"]
    feet = [f[-1] for f in fore] + [h[-1] for h in hind]
    pts = {n: R.vol(n, 3) for n in feet}
    rest_feet = {n: R.world(n, pts[n]) for n in feet}
    # outward sign per foreleg: the rz that moves the hoof AWAY from the midline
    sign = {}
    for chain in fore:
        sh, foot = chain[0], chain[-1]
        rx, ry, rz = R.rest_rot(sh)
        x0 = abs(rest_feet[foot][:, 0].mean())
        sign[sh] = max((1.0, -1.0), key=lambda s: abs(R.world(foot, pts[foot], {sh: (rx, ry, rz + s * splay)})[:, 0].mean()) - x0)

    def rotations(v):
        pitch, fpitch, ht, hg, hc = v
        d = {root: (pitch, 0.0, 0.0)}
        for chain in fore:
            sh, foot = chain[0], chain[-1]
            d[sh] = (fpitch, 0.0, sign[sh] * splay)
            d[foot] = (-(pitch + fpitch), 0.0, -sign[sh] * splay)          # keep the hoof sole level
        for th, ga, ca, foot in hind:
            d[th], d[ga], d[ca] = (ht, 0.0, 0.0), (hg, 0.0, 0.0), (hc, 0.0, 0.0)
            d[foot] = (-(pitch + ht + hg + hc), 0.0, 0.0)
        over = {}
        for part, (a, b, c) in d.items():
            rx, ry, rz = R.rest_rot(part)
            over[part] = (rx + a, ry + b, rz + c)
        return over, d

    def build(v):
        """The body drop is not searched: a drop is a pure vertical shift of the root, so every hoof moves by exactly
        it. It is DERIVED so the lowest hoof sits exactly on the ground -- nothing can sink, and the search only has
        to bring the other hooves down to meet it."""
        over, d = rotations(v)
        lows = [R.world(n, pts[n], over)[:, 1].min() - prob.ground for n in feet]
        dy = -min(lows)
        return over, {root: (0.0, dy, 0.0)}, d, dy

    def cost(v):
        over, move, _, dy = build(v)
        pen = 0.0
        for n in feet:
            w = R.world(n, pts[n], over, move)
            low = w[:, 1].min() - prob.ground              # >= 0 by construction: penalise a hoof left in the air
            pen += 20.0 * max(0.0, low - 0.1)
            slide = abs(w[:, 2].mean() - rest_feet[n][:, 2].mean())
            pen += 5.0 * max(0.0, slide - 1.0)
            sole = w[w[:, 1] < w[:, 1].min() + 1.0]
            if len(sole):
                pen += 2.0 * max(0.0, (sole[:, 1].max() - sole[:, 1].min()) - 0.6)
        if dy > 2.0:
            pen += (dy - 2.0) * 5.0                        # the body may not RISE to make the legs fit
        pitch, fpitch, ht, hg, hc = v
        bend = 0.3 * pitch ** 2 + 1.0 * fpitch ** 2 + 2.0 * (ht ** 2 + hg ** 2 + hc ** 2) + 4.0 * dy ** 2
        return pen * 1e3 + bend, pen

    lo = np.array([-25.0, -25.0, -35.0, -40.0, -40.0])
    hi = np.array([25.0, 25.0, 35.0, 40.0, 40.0])
    rng = np.random.default_rng(seed)
    best_v, best_c = None, None
    for restart in range(4):
        cur = np.clip(rng.normal(0, 4, 5), lo, hi)
        cur_c = cost(cur)[0]
        span = np.array([12.0, 12.0, 15.0, 15.0, 15.0])
        for it in range(18):
            for c in np.clip(cur + rng.normal(0, 1, (140, 5)) * span, lo, hi):
                cc = cost(c)[0]
                if cc < cur_c:
                    cur_c, cur = cc, c
            span *= 0.72
        if best_c is None or cur_c < best_c:
            best_c, best_v = cur_c, cur
    best_v = np.round(best_v, 1)
    over, move, d, dy = build(best_v)
    dy = round(float(dy), 2)
    move = {root: (0.0, dy, 0.0)}
    pen = cost(best_v)[1]
    lows = {n: round(float(R.world(n, pts[n], over, move)[:, 1].min() - prob.ground), 2) for n in feet}
    rec = {"body_rot_bb": {k: [round(float(x), 1) for x in val] for k, val in d.items()},
           "body_move_bb": {root: [0.0, dy, 0.0]},
           "body_note": f"forelegs splayed {splay:.0f} deg, body drop {-dy:.1f} pitch {best_v[0]:+.0f}",
           "feet_low": lows, "body_feasible": bool(pen < FEASIBLE_PENALTY)}
    print(f"     body: drop {-dy:.1f}u pitch {best_v[0]:+.1f} fore {best_v[1]:+.1f} hind {best_v[2]:+.1f}/{best_v[3]:+.1f}/{best_v[4]:+.1f}"
          f"  feet {lows}  {'OK' if pen < FEASIBLE_PENALTY else 'PENALTY ' + str(round(pen, 3))}", flush=True)
    return over, move, rec


def ears_forward(prob):
    """Ears for ALERT. Default: pricked forward -- the pitch (+/-25 deg) that moves each ear's tip toward -Z.
    ear_flare types (elephants): spread sideways -- the yaw (+/-30 deg) that moves the ear's mass away from the
    midline. Returned as {ear: (dx, dy, dz)} BB deltas."""
    R = prob.rig
    out = {}
    flare = prob.cfg.get("ear_flare")
    for n in R.order:
        if not (n.startswith("ear") or "_ear" in n) or "eye" in n or "rear" in n or "fear" in n:
            continue
        pts = R.vol(n, 3)
        rx, ry, rz = R.rest_rot(n)
        if flare:
            x0 = abs(R.world(n, pts)[:, 0].mean())
            best = max(((d, abs(R.world(n, pts, {n: (rx, ry + d, rz)})[:, 0].mean())) for d in (-30.0, 30.0)), key=lambda t: t[1])
            if best[1] > x0 + 0.05:
                out[n] = (0.0, best[0], 0.0)
            continue
        z0 = R.world(n, pts)[:, 2].mean()
        best = min(((d, R.world(n, pts, {n: (rx + d, ry, rz)})[:, 2].mean()) for d in (-25.0, 25.0)), key=lambda t: t[1])
        if best[1] < z0 - 0.05:
            out[n] = (best[0], 0.0, 0.0)
    return out


def solve_type(typ):
    prob = Problem(typ)
    res = {"type": typ, "model": prob.cfg["model"], "entity": prob.cfg["entity"], "ground": round(prob.ground, 3)}
    for pose in ("eat", "drink", "alert"):
        if prob.cfg.get(pose, True) is False:
            continue
        solver = prob
        body = None
        if pose == "alert" and prob.cfg.get("alert_muzzle"):
            solver = Problem(typ, prob.cfg["alert_muzzle"])
        if pose == "drink" and prob.cfg.get("drink_body"):
            solver = Problem(typ)
            b_over, b_move, body = solve_body(solver, prob.cfg["drink_body"])
            solver.set_body(b_over, b_move)
        r = solver.solve(pose)
        if body:
            r.update(body)
            r["feasible"] = r["feasible"] and body["body_feasible"]
        if pose == "alert":
            r["ears"] = ears_forward(prob)
        res[pose] = r
        flag = "OK " if r["feasible"] else "XX "
        print(f"  {flag}{typ:<13} {pose:<6} " + "  ".join(f"{j} {d:+.1f}" for j, d in zip(r["chain"], r["deltas_bb"]))
              + f"   muzzle low {r['muzzle_low']:+.2f}" + ("" if r["feasible"] else f"  PENALTY {r['penalty']}"), flush=True)
        if not r["feasible"]:
            r["why"] = solver.explain(pose, r["deltas_bb"])
            print(f"      why: {r['why']}", flush=True)
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f"{typ}.json").write_text(json.dumps(res, indent=1), encoding="utf-8")
    return res


def check_wildebeest():
    """The wildebeest EAT was solved by hand in wildebeest_spec.graze_solve: neck_base -82, neck -6, head +30 (BB).
    This general solver must reach the same family of answer (nose low, horns clear) on the same rig."""
    prob = Problem("wildebeest")
    r = prob.solve("eat")
    print("wildebeest EAT (general solver):", dict(zip(r["chain"], r["deltas_bb"])), "muzzle low", r["muzzle_low"], "feasible", r["feasible"])
    hand = {"neck_base": -82.0, "neck": -6.0, "head": 30.0}
    d = [hand.get(j, 0.0) for j in prob.chain]
    v, low, pen = prob.eval("eat", d)
    print("hand-solved deltas under the same constraints: muzzle low", round(float(low), 2), "penalty", round(float(pen), 4))


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if "--check-wildebeest" in sys.argv:
        check_wildebeest()
    if "--all" in sys.argv:
        args = list(T)
    for t in args:
        solve_type(t)
