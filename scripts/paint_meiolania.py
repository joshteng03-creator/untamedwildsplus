#!/usr/bin/env python3
"""Meiolania platyceps skin (2.9.0) on ModelTortoise.

The body is the luminance-rank recolour of paint_recolour_b.py (sulcata base, so its scute pattern survives): a dull
olive-brown shell, darker on the back with paler worn scute edges, and grey-brown skin. The NEW parts (horns, armoured
tail, club and its spikes) have no texels in any shipped skin, so they are painted here, face by face, from each box's
box-UV layout (top / bottom row of height d, then the side row east / north / west / south of height h -- the side row
runs from the box's top, Java -y, downward):
  * horns: dark keratin sheath with paler worn tips (both end caps pale: the root cap is buried in the skull);
  * tail_1 / tail_2: bony rings -- a dark seam every other texel along the tail, a pale spike point on top of each ring;
  * club: pale bone with darker grooves between its segments; side spikes and the big rear pair pale at the tip.
Colours: scratch/new_species_research.json (a reconstruction).

    python scripts/paint_meiolania.py [--install]
"""
import pathlib
import shutil
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paint_recolour_b import paint, SHELL, OUT, TEX  # noqa: E402
from paint_renamed import hx  # noqa: E402

JOB = dict(model="ModelTortoise", base="tortoise/sulcata", keep=("eye",),
           ramp=["#24221d", "#3a3730", "#524d44", "#6a6458", "#847c6d", "#a39a88"],
           parts={SHELL: ["#211e14", "#353020", "#4b442e", "#635a3e", "#7f7452", "#a0946c"]})
KER, KER_DK, KER_TIP = hx("#6e6248"), hx("#4a412f"), hx("#b9aa86")
RING, SEAM, SPIKE = hx("#5c5340"), hx("#362f23"), hx("#a69676")
BONE, BONE_DK, BONE_TIP = hx("#a1916c"), hx("#6c5f45"), hx("#cbbd98")

# (u, v, w, h, d) per box -- must match ModelTortoise's texOffs and addBox sizes
BOX = {"horn": (46, 0, 3, 1, 1), "tail_1": (14, 25, 2, 2, 4), "tail_2": (26, 25, 1, 1, 3), "club": (34, 25, 3, 2, 3),
       "spike": (46, 25, 1, 1, 1), "club_horn": (50, 25, 1, 2, 1)}


def faces(u, v, w, h, d):
    """box-UV face rects (x0, y0, x1, y1), exclusive ends"""
    return {"top": (u + d, v, u + d + w, v + d), "bottom": (u + d + w, v, u + d + 2 * w, v + d),
            "east": (u, v + d, u + d, v + d + h), "north": (u + d, v + d, u + d + w, v + d + h),
            "west": (u + d + w, v + d, u + 2 * d + w, v + d + h), "south": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}


def fill(img, rect, fn):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), fn(x - x0, y - y0, x1 - x0, y1 - y0) + (255,))


def mul(c, k):
    return tuple(max(0, min(255, int(round(ch * k)))) for ch in c)


def main():
    img = paint(JOB)
    # horns: the long axis is u on top / bottom / north / south; the end caps (east, west) are d x h
    F = faces(*BOX["horn"])
    for f in ("top", "bottom", "north", "south"):
        fill(img, F[f], lambda i, j, W, H: mul(KER if j == 0 else KER_DK, 1.0 + 0.06 * ((i * 7) % 3 - 1)))
    for f in ("east", "west"):
        fill(img, F[f], lambda i, j, W, H: KER_TIP)
    # tail rings: the long axis (z, depth d) is v on top / bottom and u on east / west
    for key in ("tail_1", "tail_2"):
        F = faces(*BOX[key])
        fill(img, F["top"], lambda i, j, W, H: SPIKE if (j % 2 == 0 and i == W // 2) else (SEAM if j % 2 else RING))
        fill(img, F["bottom"], lambda i, j, W, H: mul(SEAM if j % 2 else RING, 0.85))   # open underneath: darker skin
        for f in ("east", "west"):
            fill(img, F[f], lambda i, j, W, H: SEAM if i % 2 else (SPIKE if j == 0 else RING))
        for f in ("north", "south"):
            fill(img, F[f], lambda i, j, W, H: RING)
    # club: pale bone, a darker groove between segments (every other texel along the tail), darker underneath
    F = faces(*BOX["club"])
    fill(img, F["top"], lambda i, j, W, H: BONE_DK if j == 1 else BONE)
    fill(img, F["bottom"], lambda i, j, W, H: mul(BONE_DK, 0.9))
    for f in ("east", "west"):
        fill(img, F[f], lambda i, j, W, H: BONE_DK if i == 1 else (BONE_TIP if j == 0 else BONE))
    fill(img, F["north"], lambda i, j, W, H: BONE)
    fill(img, F["south"], lambda i, j, W, H: BONE_TIP if j == 0 else BONE)
    # spikes: pale; the club's big rear pair pale at the tip (side row top = the box's -y end = its tip)
    for f, r in faces(*BOX["spike"]).items():
        fill(img, r, lambda i, j, W, H: BONE_TIP)
    for f, r in faces(*BOX["club_horn"]).items():
        fill(img, r, lambda i, j, W, H: BONE_TIP if (f in ("top",) or (f not in ("bottom",) and j == 0)) else BONE)
    OUT.mkdir(parents=True, exist_ok=True)
    dst = OUT / "tortoise_meiolania.png"
    img.save(dst)
    if "--install" in sys.argv:
        shutil.copy(dst, TEX / "tortoise/meiolania.png")
    print("meiolania ->", dst, "(installed)" if "--install" in sys.argv else "")


if __name__ == "__main__":
    main()
