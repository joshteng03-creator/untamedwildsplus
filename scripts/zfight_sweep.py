#!/usr/bin/env python3
"""Z-fighting sweep for the hand-written Citadel models in client/model/Model*.java.

This is Phase 4 of the remodelling SOP, as a reusable tool. It parses a Model*.java directly --
there are no .bbmodel files in this repo -- rebuilds the part hierarchy, and reports pairs of boxes
whose faces share a real coincident PATCH.

Why patch sampling and not a world-AABB test
--------------------------------------------
Both a box's world AABB and the test axes are world-axis-aligned, so for rotated parts an AABB
comparison invents pairs that are nowhere near each other: on the antelope it reported the horn base
"z-fighting" the neck at 0.044u when the horn actually sits well above and in front of it. Instead
each face rectangle is sampled on a 13x13 grid, every sample is transformed into the other box's
local space, and a sample counts only if it lands within PLANE_TOL of that face's plane AND inside
its rectangle. Shared area = hit fraction x face area, so genuine overlaps sort above slivers.

Chained joints deliberately overlap by ~1u (trunk, tail, tusk segments) and will show up; those are
harmless and are flagged as PARENT/CHILD. The pairs that matter are same-facing OUTER surfaces of
parts that are merely near each other.

Usage:
    python scripts/zfight_sweep.py                  # every model that has one
    python scripts/zfight_sweep.py ModelMammoth ModelGroundSloth
    python scripts/zfight_sweep.py --min-area 0.25  # loosen the reporting gate
"""
import argparse
import itertools
import math
import pathlib
import re
import sys

import numpy as np

SRC = pathlib.Path(__file__).resolve().parent.parent / "src/main/java/untamedwilds/client/model"

GRID = 13          # samples per axis across a face rectangle
PLANE_TOL = 0.15   # a sample this close to the other face's plane counts as coincident
ANGLE_TOL = 8.0    # degrees; faces less parallel than this cannot z-fight
MIN_AREA = 0.5     # u^2 of shared surface below which a hit is noise

# Faces that have been DELETED in the texture by clearing their UV rect to alpha 0. Entity models
# render with RenderType.entityCutoutNoCull, which discards fully transparent fragments, so such a
# face does not exist at draw time and cannot z-fight -- but nothing in the .java says so, and this
# script only reads geometry. Register them here or every run will re-report a fixed problem.
# Key: model name -> {part name: set of face keys}, where a face key is "<axis><+|->" naming the
# LOCAL face, e.g. "X-" for the face at the box's minimum x.
DELETED_FACES = {
    "ModelMammoth": {
        # Inner faces of the woolly skirt: they sit against the flank and are never legitimately
        # visible, but they crossed body_hips, body_rump, fur_chest and the forelegs (45u2 total).
        # Cleared in mammoth/*.png rather than moved, because every geometric position that resolved
        # one of those four neighbours grazed another.
        "fur_skirt_left": {"X-"},
        "fur_skirt_right": {"X+"},
    },
}

F = r"(-?[\d.]+)F?"
RE_NEW = re.compile(r"this\.(\w+)\s*=\s*new AdvancedModelBox\(this[^)]*\)")
RE_RP = re.compile(r"this\.(\w+)\.setRotationPoint\(\s*%s,\s*%s,\s*%s\s*\)" % (F, F, F))
RE_BOX = re.compile(r"this\.(\w+)\.addBox\(\s*%s,\s*%s,\s*%s,\s*%s,\s*%s,\s*%s(?:,\s*%s)?\s*\)"
                    % (F, F, F, F, F, F, F))
RE_ROT = re.compile(r"setRotateAngle\(\s*(\w+),\s*%s,\s*%s,\s*%s\s*\)" % (F, F, F))
RE_CHILD = re.compile(r"this\.(\w+)\.addChild\(this\.(\w+)\)")


class Part:
    __slots__ = ("name", "rp", "rot", "lo", "hi", "parent")

    def __init__(self, name):
        self.name = name
        self.rp = np.zeros(3)
        self.rot = np.zeros(3)
        self.lo = None
        self.hi = None
        self.parent = None


def parse(path):
    src = path.read_text(encoding="utf-8")
    parts = {}
    for m in RE_NEW.finditer(src):
        parts[m.group(1)] = Part(m.group(1))
    for m in RE_RP.finditer(src):
        if m.group(1) in parts:
            parts[m.group(1)].rp = np.array([float(x) for x in m.groups()[1:4]])
    for m in RE_BOX.finditer(src):
        name = m.group(1)
        if name not in parts:
            continue
        v = [float(x) for x in m.groups()[1:7]]
        delta = float(m.group(8)) if m.group(8) else 0.0
        lo = np.array(v[0:3]) - delta
        hi = np.array(v[0:3]) + np.array(v[3:6]) + delta
        parts[name].lo, parts[name].hi = lo, hi
    for m in RE_ROT.finditer(src):
        if m.group(1) in parts:
            parts[m.group(1)].rot = np.array([float(x) for x in m.groups()[1:4]])
    for m in RE_CHILD.finditer(src):
        parent, child = m.group(1), m.group(2)
        if parent in parts and child in parts:
            parts[child].parent = parent
    return parts


def local_matrix(p):
    """MC applies translate(rotationPoint) then Z, then Y, then X (ModelPart.translateAndRotate)."""
    rx, ry, rz = p.rot
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    R = Rz @ Ry @ Rx
    M = np.eye(4)
    M[:3, :3] = R
    M[:3, 3] = p.rp
    return M


def world_matrices(parts):
    out, seen = {}, {}

    def resolve(name):
        if name in seen:
            return seen[name]
        p = parts[name]
        M = local_matrix(p)
        if p.parent:
            M = resolve(p.parent) @ M
        seen[name] = M
        return M

    for name in parts:
        out[name] = resolve(name)
    return out


def faces(p):
    """(axis, plane_coord, rect_lo, rect_hi, area) for each of the 6 faces, in part-local space."""
    res = []
    for axis in range(3):
        o1, o2 = [a for a in range(3) if a != axis]
        area = (p.hi[o1] - p.lo[o1]) * (p.hi[o2] - p.lo[o2])
        for coord in (p.lo[axis], p.hi[axis]):
            res.append((axis, coord, o1, o2, area))
    return res


def sample_face(p, face):
    axis, coord, o1, o2, _ = face
    a = np.linspace(p.lo[o1], p.hi[o1], GRID)
    b = np.linspace(p.lo[o2], p.hi[o2], GRID)
    A, B = np.meshgrid(a, b, indexing="ij")
    pts = np.empty((GRID * GRID, 3))
    pts[:, axis] = coord
    pts[:, o1] = A.ravel()
    pts[:, o2] = B.ravel()
    return pts


def sweep(parts, min_area=MIN_AREA, model=None):
    boxed = {n: p for n, p in parts.items() if p.lo is not None}
    mats = world_matrices(parts)

    cache = {}
    for n, p in boxed.items():
        M = mats[n]
        corners = np.array(list(itertools.product(*zip(p.lo, p.hi))))
        wc = (M[:3, :3] @ corners.T).T + M[:3, 3]
        cache[n] = (M, np.linalg.inv(M), wc.min(0), wc.max(0), faces(p))

    deleted = DELETED_FACES.get(model, {})

    def is_deleted(part_name, face, part):
        keys = deleted.get(part_name)
        if not keys:
            return False
        axis, coord = face[0], face[1]
        sign = "-" if abs(coord - part.lo[axis]) < 1e-6 else "+"
        return ("XYZ"[axis] + sign) in keys

    hits = []
    for na, nb in itertools.combinations(sorted(boxed), 2):
        Ma, _, loa, hia, fa = cache[na]
        Mb, Mbi, lob, hib, fb = cache[nb]
        if np.any(hia + 0.2 < lob) or np.any(hib + 0.2 < loa):
            continue
        pa, pb = boxed[na], boxed[nb]
        Ra, Rb = Ma[:3, :3], Mb[:3, :3]

        best = None
        for face_a in fa:
            if is_deleted(na, face_a, pa):
                continue
            na_world = Ra[:, face_a[0]]
            pts_l = sample_face(pa, face_a)
            pts_w = (Ra @ pts_l.T).T + Ma[:3, 3]
            pts_b = (Mbi[:3, :3] @ pts_w.T).T + Mbi[:3, 3]
            for face_b in fb:
                if is_deleted(nb, face_b, pb):
                    continue
                nb_world = Rb[:, face_b[0]]
                cosang = abs(float(np.dot(na_world, nb_world)))
                cosang = min(1.0, cosang)
                if math.degrees(math.acos(cosang)) > ANGLE_TOL:
                    continue
                axis, coord, o1, o2, _ = face_b
                near = np.abs(pts_b[:, axis] - coord) <= PLANE_TOL
                inside = ((pts_b[:, o1] >= pb.lo[o1]) & (pts_b[:, o1] <= pb.hi[o1]) &
                          (pts_b[:, o2] >= pb.lo[o2]) & (pts_b[:, o2] <= pb.hi[o2]))
                n_hit = int(np.count_nonzero(near & inside))
                if not n_hit:
                    continue
                shared = n_hit / (GRID * GRID) * face_a[4]
                if shared >= min_area and (best is None or shared > best[0]):
                    best = (shared, face_a, face_b)
        if best:
            rel = ""
            if pa.parent == nb or pb.parent == na:
                rel = " [PARENT/CHILD]"
            hits.append((best[0], na, nb, best[1], best[2], rel))

    hits.sort(reverse=True, key=lambda h: h[0])
    return hits


AX = "XYZ"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("models", nargs="*")
    ap.add_argument("--min-area", type=float, default=MIN_AREA)
    args = ap.parse_args()

    names = args.models or sorted(p.stem for p in SRC.glob("Model*.java"))
    total = 0
    for name in names:
        path = SRC / (name + ".java")
        if not path.exists():
            print("!! no such model: %s" % name)
            continue
        parts = parse(path)
        boxed = sum(1 for p in parts.values() if p.lo is not None)
        hits = sweep(parts, args.min_area, name)
        print("\n=== %s (%d boxes) : %d coincident patch(es) >= %.2fu2 ==="
              % (name, boxed, len(hits), args.min_area))
        for area, na, nb, fa, fb, rel in hits:
            print("  %7.2fu2  %-22s %s%+.2f   vs  %-22s %s%+.2f%s"
                  % (area, na, AX[fa[0]], fa[1], nb, AX[fb[0]], fb[1], rel))
        total += len(hits)
    print("\nTOTAL: %d" % total)
    return 0


if __name__ == "__main__":
    sys.exit(main())
