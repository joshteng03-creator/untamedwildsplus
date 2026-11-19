#!/usr/bin/env python3
"""Does a texture fit a model's CURRENT UV layout? (2.9.0, for wiring up old upstream art.)

For every texel the model's box-UV faces sample, count how many are opaque in the PNG, and compare with the
type's shipped skins. Art painted for an older layout shows up as a much lower coverage (faces sampling blank
canvas = see-through holes in game) or as paint sitting outside the faces the model reads.

    python scripts/uv_fit.py ModelCatfish catfish/shovelnose.png --ref catfish/blue.png catfish/channel.png
"""
import pathlib
import sys

from PIL import Image

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from juvenile_census import texels, TEX  # noqa: E402


def fit(model, png):
    want, (cw, ch) = texels(model)
    im = Image.open(TEX / png).convert("RGBA")
    if im.size != (cw, ch):
        return {"png": png, "size": im.size, "canvas": (cw, ch), "covered": None, "outside": None}
    px = im.load()
    covered = sum(1 for x, y in want if px[x, y][3] > 0)
    painted = sum(1 for y in range(ch) for x in range(cw) if px[x, y][3] > 0)
    return {"png": png, "size": im.size, "canvas": (cw, ch), "covered": covered / max(1, len(want)),
            "outside": (painted - covered) / max(1, painted)}


if __name__ == "__main__":
    model = sys.argv[1]
    args = sys.argv[2:]
    refs = args[args.index("--ref") + 1:] if "--ref" in args else []
    cands = args[:args.index("--ref")] if "--ref" in args else args
    for kind, lst in (("ref ", refs), ("cand", cands)):
        for png in lst:
            r = fit(model, png)
            if r["covered"] is None:
                print(f"{kind} {png:<28} SIZE MISMATCH {r['size']} vs canvas {r['canvas']}")
            else:
                print(f"{kind} {png:<28} faces covered {100 * r['covered']:5.1f}%   paint outside faces {100 * r['outside']:5.1f}%")
