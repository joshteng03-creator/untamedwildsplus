#!/usr/bin/env python3
"""Offline preview of the 2.8.0 carcass look: each animal's own model rolled onto its side exactly as RendererCarcass
does it (roll 90 deg about the long axis, lifted by 0.42 x hitbox width, shifted by half the hitbox height), fresh and
decayed (light x0.55).  python scripts/carcass_sheet.py  ->  scratch/poses/sheet_carcass.png
"""
import math
import pathlib
import re

import numpy as np
from PIL import Image, ImageDraw

import soft_render as SR
from fk_rig import Rig

ROOT = pathlib.Path(__file__).resolve().parent.parent
MODENTITY = (ROOT / "src/main/java/untamedwilds/init/ModEntity.java").read_text(encoding="utf-8")
TYPES = [("bison", "ModelBison"), ("equid", "ModelEquid"), ("dire_wolf", "ModelDireWolf"), ("big_cat", "ModelBigCat"),
         ("giraffid", "ModelGiraffid"), ("rhino", "ModelRhino"), ("ground_sloth", "ModelGroundSloth"), ("bear", "ModelBear")]
RENDERER = (ROOT / "src/main/java/untamedwilds/client/render/RendererCarcass.java").read_text(encoding="utf-8")


def solved(typ):
    m = re.search(r'SIDE\.put\("' + typ + r'", new float\[\]\{([-\d.]+)F, ([-\d.]+)F\}\);', RENDERER)
    return (float(m.group(1)), float(m.group(2))) if m else None


def hitbox(typ):
    m = re.search(r'createEntity\(\w+::new, "' + typ + r'", ([\d.]+)F?, ([\d.]+)F?', MODENTITY)
    return float(m.group(1)), float(m.group(2))


class Rolled(Rig):
    """World coordinates in BLOCKS*16 (BB units) of the carcass: model rolled +90 deg about Z, then lifted/shifted."""

    def __init__(self, model, lift, shift):
        super().__init__(model)
        self.lift, self.shift = lift, shift

    def world(self, n, pts, over=None, move=None):
        p = super().world(n, pts, over, move)
        c, s = math.cos(math.pi / 2), math.sin(math.pi / 2)
        x, y = p[:, 0] * c - p[:, 1] * s, p[:, 0] * s + p[:, 1] * c
        q = np.stack([x + self.shift, y + self.lift, p[:, 2]], axis=1)
        return q


def main():
    cell_w, cell_h = 420, 260
    img = Image.new("RGBA", (cell_w * 2, cell_h * len(TYPES) + 24), (245, 242, 235, 255))
    ImageDraw.Draw(img).text((8, 4), "Carcass look (2.8.0): the species' own model on its side -- left fresh, right decayed "
                             "(55% light just before bones). Deer and proboscideans lie in their sleep pose instead (antlers / tusks).", fill=(0, 0, 0, 255))
    for i, (typ, model) in enumerate(TYPES):
        lift, shift = solved(typ)
        rig = Rolled(model, lift, shift)
        cols = SR.part_colours(rig, SR.skin_for(typ))
        hide = set(SR.T.get(typ, {}).get("hide", [])) if hasattr(SR, "T") else set()
        allc = np.vstack([rig.world(n, rig.corners(n)) for n in rig.order])
        ext = max(allc[:, 2].max() - allc[:, 2].min(), allc[:, 0].max() - allc[:, 0].min(), 1.0)
        scale = min(cell_w * 0.75 / ext, cell_h * 0.6 / max(1.0, allc[:, 1].max() + 4))
        oy = 24 + cell_h * i + cell_h * 0.78
        low = allc[:, 1].min()
        draw_dark = {k: (None if v is None else v * 0.55) for k, v in cols.items()}
        SR.draw_view(img, rig, {}, cols, hide, 135, 22, scale, (cell_w * 0.5, oy), None,
                     f"{typ}: lowest point {low / 16:+.2f} blocks (0 = resting on the ground)")
        SR.draw_view(img, rig, {}, draw_dark, hide, 135, 22, scale, (cell_w * 1.5, oy), None, "")
    out = ROOT / "scratch" / "poses" / "sheet_carcass.png"
    img.convert("RGB").save(out)
    print("wrote", out)


if __name__ == "__main__":
    main()
