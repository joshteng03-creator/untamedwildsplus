#!/usr/bin/env python3
"""Report overlapping UV footprints in a hand-written Citadel model.

Minecraft reserves 2*(w+d) wide by (h+d) tall from every box's texOffs, regardless of how small a
region the author meant to use (CLAUDE.md lesson 2). Two boxes whose footprints overlap will sample
each other's paint. Run this before and after any change to a box's WIDTH, HEIGHT or DEPTH -- the
count must not go up.

    python scripts/uv_footprint_check.py ModelMammoth ModelGroundSloth
"""
import itertools
import pathlib
import re
import sys

SRC = pathlib.Path(__file__).resolve().parent.parent / "src/main/java/untamedwilds/client/model"

F = r"(-?[\d.]+)F?"
RE_NEW = re.compile(r"this\.(\w+)\s*=\s*new AdvancedModelBox\(this,\s*([\d.]+)\s*,\s*([\d.]+)\s*\)")
RE_BOX = re.compile(r"this\.(\w+)\.addBox\(\s*%s,\s*%s,\s*%s,\s*%s,\s*%s,\s*%s" % (F, F, F, F, F, F))


def footprints(path):
    src = path.read_text(encoding="utf-8")
    offs = {m.group(1): (float(m.group(2)), float(m.group(3))) for m in RE_NEW.finditer(src)}
    out = {}
    for m in RE_BOX.finditer(src):
        name = m.group(1)
        if name not in offs:
            continue
        w, h, d = (float(m.group(5)), float(m.group(6)), float(m.group(7)))
        u, v = offs[name]
        out[name] = (u, v, u + 2 * (w + d), v + (h + d))
    return out


def main():
    names = sys.argv[1:] or sorted(p.stem for p in SRC.glob("Model*.java"))
    total = 0
    for name in names:
        fp = footprints(SRC / (name + ".java"))
        bad = []
        for a, b in itertools.combinations(sorted(fp), 2):
            ra, rb = fp[a], fp[b]
            ox = min(ra[2], rb[2]) - max(ra[0], rb[0])
            oy = min(ra[3], rb[3]) - max(ra[1], rb[1])
            if ox > 0.001 and oy > 0.001:
                bad.append((ox * oy, a, b, ra, rb))
        bad.sort(reverse=True)
        print("=== %s : %d box(es), %d footprint overlap(s) ===" % (name, len(fp), len(bad)))
        for area, a, b, ra, rb in bad:
            print("   %8.1f px2  %-22s %s  vs  %-22s %s" % (area, a, ra, b, rb))
        total += len(bad)
    print("TOTAL OVERLAPS: %d" % total)


if __name__ == "__main__":
    main()
