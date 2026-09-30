#!/usr/bin/env python3
"""Parse a hand-written Citadel Model*.java into Blockbench project space.

Used to rebuild an ALREADY-SHIPPED model as a Blockbench rig so new geometry can be
sculpted against it and skins previewed in 3D. This is the REVERSE of the authoring
direction; for authoring, always build in Blockbench and let
export_model(codec_id:"modded_entity") do the Y-flip.

The transform is calibrated, not guessed (verified exact on ModelRhino, ModelBear,
ModelMammoth, ModelDireWolf, ModelBigCat by re-exporting and diffing):

    absolute pivot = sum of setRotationPoint down the parent chain
        (both engines apply rotation AFTER hierarchy offsets, so naive addition is right)
    bbOrigin = (-absX, 24 - absY, absZ)
    bbTo.x   = -(absX + boxX)          bbFrom.x = bbTo.x - width
    bbTo.y   = 24 - (absY + boxY)      bbFrom.y = bbTo.y - height
    bbFrom.z = absZ + boxZ             bbTo.z   = bbFrom.z + depth
    bbRot    = (-rxDeg, -ryDeg, +rzDeg)

ALWAYS prove it: after building, export_model again and diff every addBox / texOffs /
PartPose against the original .java. Do not paint against an unverified rig.

    python scripts/java2bb.py ModelAntelope > rig.json
"""
import json
import math
import pathlib
import re
import sys

SRC = pathlib.Path(__file__).resolve().parent.parent / "src/main/java/untamedwilds/client/model"
F = r"(-?[\d.]+)F?"

# Parts may be `this.<field>` OR a LOCAL `AdvancedModelBox x = new ...` -- ModelBear declares
# body_tail / head_teeth / ear_left / ear_right as locals, and a this.-only parse silently dropped
# them (the rig had 19 of 23 parts and the ears were never painted -- 2026-09-30).
RE_NEW = re.compile(r"(?:this\.|AdvancedModelBox\s+)(\w+)\s*=\s*new AdvancedModelBox\(this,\s*([\d.]+)\s*,\s*([\d.]+)\s*\)")
RE_RP = re.compile(r"(?:this\.)?(\w+)\.setRotationPoint\(\s*%s,\s*%s,\s*%s\s*\)" % (F, F, F))
RE_BOX = re.compile(r"(?:this\.)?(\w+)\.addBox\(\s*%s,\s*%s,\s*%s,\s*%s,\s*%s,\s*%s" % (F, F, F, F, F, F))
RE_ROT = re.compile(r"this\.setRotateAngle\(\s*(\w+)\s*,\s*%s,\s*%s,\s*%s\s*\)" % (F, F, F))
RE_CHILD = re.compile(r"(?:this\.)?(\w+)\.addChild\((?:this\.)?(\w+)\)")
RE_MIRROR = re.compile(r"(?:this\.)?(\w+)\.mirror\s*=\s*true")
RE_CANVAS = re.compile(r"texWidth\s*=\s*(\d+).*?texHeight\s*=\s*(\d+)", re.S)


def ctor_body(src, name):
    """Only the CONSTRUCTOR. Parsing the whole file is a trap: setupAnim re-issues
    setRotationPoint on the eye planes for the blink, and a file-wide regex keeps the LAST
    match, so the eyes come out at their hidden-inside-the-skull position instead of their
    real one. A round-trip diff will NOT catch it, because both sides of the comparison
    come from the same bad parse.
    """
    start = src.index("public %s()" % name)
    end = src.index("animator = ModelAnimator.create()", start)
    return src[start:end]


def parse(name):
    src = ctor_body((SRC / (name + ".java")).read_text(encoding="utf-8"), name)
    uv = {m.group(1): (int(float(m.group(2))), int(float(m.group(3))))
          for m in RE_NEW.finditer(src)}
    rp = {m.group(1): tuple(float(m.group(i)) for i in (2, 3, 4)) for m in RE_RP.finditer(src)}
    box = {m.group(1): tuple(float(m.group(i)) for i in range(2, 8)) for m in RE_BOX.finditer(src)}
    rot = {m.group(1): tuple(float(m.group(i)) for i in (2, 3, 4)) for m in RE_ROT.finditer(src)}
    parent = {c: p for p, c in RE_CHILD.findall(src)}
    order = [m.group(1) for m in RE_NEW.finditer(src)]
    # mirror flips the box UV horizontally in MC (Blockbench: mirror_uv). The old rigs
    # (bison/camel/boar) mirror 6-8 parts each; dropping it paints a mirrored part's
    # features onto the wrong end of its faces.
    mirror = set(RE_MIRROR.findall(src))
    return order, uv, rp, box, rot, parent, mirror


def canvas(name):
    m = RE_CANVAS.search((SRC / (name + ".java")).read_text(encoding="utf-8"))
    return [int(m.group(1)), int(m.group(2))] if m else [256, 128]


def convert(name):
    order, uv, rp, box, rot, parent, mirror = parse(name)

    absp = {}

    def apiv(n):
        if n in absp:
            return absp[n]
        x, y, z = rp[n]
        if parent.get(n):
            px, py, pz = apiv(parent[n])
            x, y, z = x + px, y + py, z + pz
        absp[n] = (x, y, z)
        return absp[n]

    # TOPOLOGICAL order, not declaration order. The builder creates groups in one forward
    # pass, so a part declared before its parent (ModelBison declares body_torso first,
    # whose parent body_main comes later) would find G[parent] undefined and silently
    # become a ROOT -- the rig then exports a flattened hierarchy that no longer matches
    # the Java, which is exactly what verify_port catches.
    seen, topo = set(), []

    def emit(n):
        if n in seen:
            return
        seen.add(n)
        if parent.get(n):
            emit(parent[n])
        topo.append(n)

    for n in order:
        emit(n)

    out = []
    for n in topo:
        ax, ay, az = apiv(n)
        bx, by, bz, w, h, d = box[n]
        rx, ry, rz = rot.get(n, (0.0, 0.0, 0.0))
        to_x = -(ax + bx)
        to_y = 24.0 - (ay + by)
        out.append({
            "n": n,
            "p": parent.get(n),
            "o": [round(-ax, 4), round(24.0 - ay, 4), round(az, 4)],
            "r": [round(-math.degrees(rx), 4), round(-math.degrees(ry), 4),
                  round(math.degrees(rz), 4)],
            "f": [round(to_x - w, 4), round(to_y - h, 4), round(az + bz, 4)],
            "t": [round(to_x, 4), round(to_y, 4), round(az + bz + d, 4)],
            "uv": list(uv[n]),
            "m": n in mirror,
            "c": "existing",
        })
    return out


if __name__ == "__main__":
    parts = convert(sys.argv[1])
    print(json.dumps({"canvas": canvas(sys.argv[1]), "parts": parts},
                     separators=(",", ":")))
