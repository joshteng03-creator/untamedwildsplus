#!/usr/bin/env python3
"""Verify a phase-6 Java port against the Blockbench export it came from.

Parses the `modded_entity` export and the generated Model*.java independently and
asserts that every part's texOffs, addBox origin+size, rotationPoint, rotation and PARENT
match.  This is the check that catches a stale port after a late geometry tweak -- fixing
the equid's eyes after its port left the Java holding the old rotation points, and one
diff found it in a single line.

CRITICAL: only the CONSTRUCTOR is parsed.  A file-wide regex keeps the LAST
setRotationPoint per part, and setupAnim re-issues one on both eye planes for the blink,
so a whole-file parse silently reads the eyes at their hidden-inside-the-skull position
and still reports a match.  That exact bug made scripts/java2bb.py claim "EXACT MATCH"
while eye_left sat at x 1.0 instead of 2.6.

Usage:  python scripts/verify_port.py <export.java> <Model*.java>
"""
import pathlib
import re
import sys

EXPORT = pathlib.Path(sys.argv[1]).read_text()
JAVA = pathlib.Path(sys.argv[2]).read_text()

EXPORT_RE = re.compile(
    r'PartDefinition (\w+) = (\w+)\.addOrReplaceChild\("(\w+)",\s*'
    r'CubeListBuilder\.create\(\)\.texOffs\((\d+), (\d+)\)'
    # a mirrored cube exports as .texOffs(..).mirror().addBox(..).mirror(false)
    r'(?:\.mirror\(\))?'
    r'\.addBox\(([^)]*?), new CubeDeformation\(0\.0F\)\)(?:\.mirror\(false\))?,\s*'
    r'PartPose\.(?:offset|offsetAndRotation)\(([^)]*)\)\);')


def nums(s):
    return [round(float(x.strip().rstrip("Ff")), 4) for x in s.split(",") if x.strip()]


def from_export():
    out = {}
    for m in EXPORT_RE.finditer(EXPORT):
        box, pose = nums(m.group(6)), nums(m.group(7))
        out[m.group(3)] = {
            "uv": (int(m.group(4)), int(m.group(5))),
            "box": box[:6],
            "rp": pose[:3],
            "rot": pose[3:6] if len(pose) > 3 else [0.0, 0.0, 0.0],
            "parent": None if m.group(2) == "partdefinition" else m.group(2),
        }
    return out


def from_java():
    # Slice out the constructor ONLY -- see the module docstring.
    start = re.search(r"public Model\w+\(\)", JAVA).start()
    end = JAVA.index("updateDefaultPose();", start)
    ctor = JAVA[start:end]

    out = {}
    for m in re.finditer(r"this\.(\w+) = new AdvancedModelBox\(this, (\d+), (\d+)\);", ctor):
        out[m.group(1)] = {"uv": (int(m.group(2)), int(m.group(3))),
                           "rot": [0.0, 0.0, 0.0], "parent": None}
    for m in re.finditer(r"this\.(\w+)\.setRotationPoint\(([^)]*)\);", ctor):
        out[m.group(1)]["rp"] = nums(m.group(2))
    for m in re.finditer(r"this\.(\w+)\.addBox\(([^)]*)\);", ctor):
        out[m.group(1)]["box"] = nums(m.group(2))[:6]
    for m in re.finditer(r"this\.setRotateAngle\((\w+), ([^)]*)\);", ctor):
        out[m.group(1)]["rot"] = nums(m.group(2))
    for m in re.finditer(r"this\.(\w+)\.addChild\(this\.(\w+)\);", ctor):
        out[m.group(2)]["parent"] = m.group(1)
    return out


E, J = from_export(), from_java()
bad = []

if set(E) != set(J):
    bad.append(f"part sets differ: only-export={sorted(set(E)-set(J))} "
               f"only-java={sorted(set(J)-set(E))}")

for n in sorted(set(E) & set(J)):
    for k in ("uv", "box", "rp", "rot", "parent"):
        ev, jv = E[n][k], J[n][k]
        if isinstance(ev, list):
            ok = len(ev) == len(jv) and all(abs(x - y) < 1e-3 for x, y in zip(ev, jv))
        else:
            ok = ev == jv
        if not ok:
            bad.append(f"{n}.{k}: export={ev}  java={jv}")

print("=" * 78)
print(f"PORT VERIFY   {len(E)} parts in export, {len(J)} in Java")
print("=" * 78)
if bad:
    for b in bad:
        print("  MISMATCH  " + b)
    raise SystemExit(f"\n{len(bad)} mismatches -- the port is NOT faithful")
print(f"  EXACT MATCH on all {len(E)} parts "
      "(texOffs, addBox origin+size, rotationPoint, rotation, parent)")
