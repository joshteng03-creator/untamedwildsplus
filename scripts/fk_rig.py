#!/usr/bin/env python3
"""Generic forward kinematics for ANY shipped Citadel model (2.8.0 pose solving).

Reads the model through java2bb.convert() (constructor-only parse, Blockbench space: Y up, 1 unit = 1/16 block at
scale 1, ground at y = 0, the animal's FRONT is -Z) and composes rotations exactly as the per-animal spec scripts do:
for each ancestor, self first, p = R(r) (p - o) + o with R = Rz Ry Rx in Blockbench degrees.

Rotation overrides are ABSOLUTE Blockbench rotations per part ({name: (rx, ry, rz)}). A Citadel keyframe adds a
delta to the default pose, so `rest + delta` is what the game renders; Blockbench deltas convert to the Java
keyframe as (x, y, z)_java = (-dx, -dy, +dz) in degrees (the models' rotate(animator, part, deg...) helper).

    python scripts/fk_rig.py --selftest          # must reproduce wildebeest_spec.world() for every part
"""
import math
import sys

import numpy as np

import java2bb


def rmat(rx, ry, rz):
    cx, sx = math.cos(math.radians(rx)), math.sin(math.radians(rx))
    cy, sy = math.cos(math.radians(ry)), math.sin(math.radians(ry))
    cz, sz = math.cos(math.radians(rz)), math.sin(math.radians(rz))
    return (np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
            @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
            @ np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]))


def _statics(model):
    """Fixed per-frame geometry a model's setupAnim applies to EVERY animal (pose_targets.STATICS): per-part setScale
    (scaleChildren off, about the part's pivot) and limb roots pushed out along x. The solvers must see what renders."""
    try:
        from pose_targets import STATICS
    except ImportError:
        return {}
    return STATICS.get(model, {})


class Rig:
    def __init__(self, model):
        self.model = model
        parts = java2bb.convert(model)
        self.P = {p["n"]: p for p in parts}
        self.order = [p["n"] for p in parts]
        self.kids = {n: [] for n in self.order}
        for p in parts:
            if p["p"]:
                self.kids[p["p"]].append(p["n"])
        self._anc = {}
        st = _statics(model)
        self.scale = {k: np.asarray(v, float) for k, v in st.get("scale", {}).items() if k in self.P}
        # rotationPointX *= f in Java = the part's pivot (and everything below it) shifted by x*(f-1); valid because every
        # ancestor of these parts sits on the midline (x = 0), which _check_statics asserts
        self.static_move = {}
        for k, f in st.get("pivot_x", {}).items():
            if k in self.P:
                assert all(abs(self.P[a]["o"][0]) < 1e-6 for a in self.ancestry(k)[1:]), f"{k}: an ancestor is off the midline"
                self.static_move[k] = (self.P[k]["o"][0] * (f - 1.0), 0.0, 0.0)

    def _moves(self, move):
        if not self.static_move:
            return move
        m = dict(self.static_move)
        for k, v in (move or {}).items():
            m[k] = tuple(a + b for a, b in zip(m.get(k, (0.0, 0.0, 0.0)), v))
        return m

    # ---- structure
    def ancestry(self, n):
        if n not in self._anc:
            out, k = [], n
            while k is not None:
                out.append(k)
                k = self.P[k]["p"]
            self._anc[n] = out
        return self._anc[n]

    def subtree(self, n):
        out, stack = [], [n]
        while stack:
            k = stack.pop()
            out.append(k)
            stack.extend(self.kids[k])
        return out

    def rest_rot(self, n):
        return tuple(self.P[n]["r"])

    # ---- geometry
    def corners(self, n):
        a, b = self.P[n]["f"], self.P[n]["t"]
        return np.array([[x, y, z] for x in (a[0], b[0]) for y in (a[1], b[1]) for z in (a[2], b[2])], float)

    def vol(self, n, k=5):
        a, b = np.asarray(self.P[n]["f"], float), np.asarray(self.P[n]["t"], float)
        ax = [np.linspace(a[i], b[i], k) if b[i] - a[i] > 1e-6 else np.array([a[i]]) for i in range(3)]
        return np.stack(np.meshgrid(*ax, indexing="ij"), -1).reshape(-1, 3)

    def world(self, n, pts, over=None, move=None):
        """World positions of local-rest points of part n. `over`: absolute BB rotations; `move`: {part: (dx,dy,dz)}
        translations of a part's pivot (Citadel animator.move, BB axes), applied with the part's own transform."""
        p = np.asarray(pts, float).reshape(-1, 3)
        move = self._moves(move)
        if n in self.scale:                    # the part's own setScale (children unaffected), about its pivot
            o = np.asarray(self.P[n]["o"], float)
            p = o + (p - o) * self.scale[n]
        for k in self.ancestry(n):
            r = (over or {}).get(k, self.P[k]["r"])
            o = np.asarray(self.P[k]["o"], float)
            p = (rmat(*r) @ (p - o).T).T + o
            if move and k in move:
                # a move shifts this part's pivot, i.e. everything at and below it, in its PARENT's frame;
                # applied here before the parent's rotation (the next loop iteration) -- matches Citadel's translate-then-rotate.
                p = p + np.asarray(move[k], float)
        return p

    def lowest(self, names, over=None, move=None, k=3):
        return min(self.world(n, self.vol(n, k), over, move)[:, 1].min() for n in names)

    def local_of(self, n, wpts, over=None, move=None):
        """Inverse of world(): world points into part n's local rest frame."""
        p = np.asarray(wpts, float).reshape(-1, 3)
        move = self._moves(move)
        for k in reversed(self.ancestry(n)):
            if move and k in move:
                p = p - np.asarray(move[k], float)
            r = (over or {}).get(k, self.P[k]["r"])
            o = np.asarray(self.P[k]["o"], float)
            p = (rmat(*r).T @ (p - o).T).T + o
        if n in self.scale:
            o = np.asarray(self.P[n]["o"], float)
            p = o + (p - o) / self.scale[n]
        return p

    def inside(self, n, wpts, over=None, margin=0.4, move=None):
        """Boolean mask: which world points sit inside part n's box (shrunk by `margin`)."""
        q = self.local_of(n, wpts, over, move)
        a = np.minimum(self.P[n]["f"], self.P[n]["t"]) + margin
        b = np.maximum(self.P[n]["f"], self.P[n]["t"]) - margin
        return np.all((q > a) & (q < b), axis=1)


def pose_over_move(rig, r, ears=True, body=True):
    """One solved pose record (scratch/poses/<type>.json[pose]) -> (over, move) for Rig.world. The ONE place that
    knows the record's fields, so the solver, the review renderer and the keyframe generator cannot disagree:
      chain + deltas_bb   pitch deltas on the neck->muzzle chain
      neck_move_bb        pivot shift of the chain root
      ears                {ear: (dx, dy, dz)}
      body_rot_bb         {part: (dx, dy, dz)} (splayed drinkers: body, legs, feet)
      body_move_bb        {part: (dx, dy, dz)} pivot shifts (the body drop)"""
    over, move = {}, {}
    if body:
        for part, d in (r.get("body_rot_bb") or {}).items():
            rx, ry, rz = rig.rest_rot(part)
            over[part] = (rx + d[0], ry + d[1], rz + d[2])
        for part, d in (r.get("body_move_bb") or {}).items():
            move[part] = tuple(d)
    for j, d in zip(r.get("chain", []), r.get("deltas_bb", [])):
        rx, ry, rz = over.get(j, rig.rest_rot(j))
        over[j] = (rx + d, ry, rz)
    mv = r.get("neck_move_bb")
    if mv and any(abs(v) > 1e-6 for v in mv):
        c = r["chain"][0]
        prev = move.get(c, (0.0, 0.0, 0.0))
        move[c] = tuple(a + b for a, b in zip(prev, mv))
    if ears:
        for ear, d in (r.get("ears") or {}).items():
            rx, ry, rz = over.get(ear, rig.rest_rot(ear))
            dx, dy, dz = d if isinstance(d, (list, tuple)) else (d, 0.0, 0.0)
            over[ear] = (rx + dx, ry + dy, rz + dz)
    return over, (move or None)


def selftest():
    import wildebeest_spec as W
    rig = Rig("ModelWildebeest")
    worst = 0.0
    for rec in W.PARTS:
        n = rec[0]
        if n not in rig.P:
            raise SystemExit(f"part {n} missing from the parsed Java")
        a = W.world(n, W.corners(rec))
        b = rig.world(n, rig.corners(n))
        # same corner set, possibly different order: compare sorted rows
        d = np.abs(np.sort(a, axis=0) - np.sort(b, axis=0)).max()
        worst = max(worst, d)
    print(f"fk_rig self-test: {len(W.PARTS)} parts, worst corner error {worst:.2e}")
    if worst > 1e-3:
        raise SystemExit("FAIL: fk_rig does not reproduce wildebeest_spec.world()")
    print("PASS")


if __name__ == "__main__" and "--selftest" in sys.argv:
    selftest()
