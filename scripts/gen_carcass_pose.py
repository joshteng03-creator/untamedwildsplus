#!/usr/bin/env python3
"""Solve how each type's carcass lies (2.8.0, P7): roll the shipped model 90 deg onto its side (as RendererCarcass
does), then lift it so its torso and legs REST on the ground and shift it so the body is centred. If the head --
antlers, tusks, a long neck -- would still sink into the ground by more than 0.25 blocks, that type lies in its solved
SLEEP pose instead of on its side (a carcass with its antlers through the turf reads worse than one lying chin-down).

Writes the // <gen:carcass-poses> table into client/render/RendererCarcass.java.
    python scripts/gen_carcass_pose.py
"""
import math
import pathlib
import re

import numpy as np

import java2bb
from fk_rig import Rig
from pose_targets import T, HIDE, LIMB_WORDS

ROOT = pathlib.Path(__file__).resolve().parent.parent
MODELS = ROOT / "src/main/java/untamedwilds/client/model"
RENDERER = ROOT / "src/main/java/untamedwilds/client/render/RendererCarcass.java"
ENT_DIR = ROOT / "src/main/resources/data/untamedwilds/entities"
HEAD_WORDS = ("head", "neck", "antler", "horn", "tusk", "trunk", "jaw", "snout", "muzzle", "nose", "ear", "oss", "palm",
              "beam", "br_", "pedicle", "proboscis", "beard", "eye", "teeth", "lip", "skull", "cheek", "brow", "crest", "mane")


def model_for(typ):
    name = "Model" + "".join(p.capitalize() for p in typ.split("_"))
    return name if (MODELS / f"{name}.java").exists() else None


def rolled(rig, n):
    p = rig.world(n, rig.vol(n, 3))
    return np.stack([-p[:, 1], p[:, 0], p[:, 2]], axis=1)     # +90 deg about Z: (x, y) -> (-y, x)


def solve(typ, model):
    try:
        rig = Rig(model)
    except Exception:
        return None
    neck = T.get(typ, {}).get("neck")
    head = set(rig.subtree(neck)) if neck and neck in rig.P else {n for n in rig.order if any(w in n for w in HEAD_WORDS)}
    hide = set(T.get(typ, {}).get("hide", [])) | set(HIDE.get(typ, []))   # HIDE also covers types with no T entry
    body = [n for n in rig.order if n not in head and n not in hide]
    if not body:
        return None
    bpts = np.vstack([rolled(rig, n) for n in body])
    hpts = np.vstack([rolled(rig, n) for n in head if n not in hide]) if head - hide else bpts[:1]
    lift = -bpts[:, 1].min()
    shift = -(bpts[:, 0].min() + bpts[:, 0].max()) / 2
    sink = -(hpts[:, 1].min() + lift)                         # how far the head goes below the ground, units
    return round(float(lift), 2), round(float(shift), 2), round(float(sink / 16.0), 3)


def main():
    rows = []
    for f in sorted(ENT_DIR.glob("*.json")):
        typ = f.stem
        model = model_for(typ)
        if not model:
            continue
        r = solve(typ, model)
        if r is None:
            continue
        lift, shift, sink = r
        side = sink <= 0.25
        rows.append((typ, lift, shift, side, sink))
        print(f"  {typ:<14} lift {lift:6.2f}u shift {shift:6.2f}u head sinks {sink:+.2f} blocks -> {'on its side' if side else 'SLEEP pose'}")
    L = ["    // <gen:carcass-poses> 2.8.0 scripts/gen_carcass_pose.py -- per type: {lift, shift} in model units for the",
         "    // on-its-side roll, or SLEEP (lies in its solved sleep pose) when antlers / tusks would sink into the ground.",
         "    private static final java.util.Map<String, float[]> SIDE = new java.util.HashMap<>();",
         "    private static final java.util.Set<String> SLEEP = new java.util.HashSet<>();",
         "    static {"]
    for typ, lift, shift, side, _ in rows:
        if side:
            L.append(f'        SIDE.put("{typ}", new float[]{{{lift}F, {shift}F}});')
        else:
            L.append(f'        SLEEP.add("{typ}");')
    L += ["    }", "    // </gen:carcass-poses>"]
    s = RENDERER.read_text(encoding="utf-8")
    block = "\n".join(L)
    if "// <gen:carcass-poses>" in s:
        s = re.sub(r"[ \t]*// <gen:carcass-poses>.*?// </gen:carcass-poses>", block, s, flags=re.S)
    else:
        anchor = "    public RendererCarcass(EntityRendererProvider.Context context) {"
        s = s.replace(anchor, block + "\n\n" + anchor, 1)
    RENDERER.write_text(s, encoding="utf-8")
    print(f"wrote {len(rows)} carcass poses into RendererCarcass.java")


if __name__ == "__main__":
    main()
