#!/usr/bin/env python3
"""Paint the missing JUVENILE regions of shipped skins (2026-10-07).

Cubs, calves and piglets are separate models reading their own rects of the adult PNG (see
scripts/juvenile_census.py); 24 live skins left those rects empty, so their babies render see-through.

Method: take a REFERENCE sibling of the same type whose juvenile region is painted (nearest relative,
or the sibling with the right juvenile markings) and transfer it into the TARGET species' colours by
luminance quantile: every reference juvenile texel's brightness rank within the reference ADULT palette
picks the colour at the same rank of the target's adult palette. The reference's juvenile markings
(lion-cub spots, piglet stripes, calf shading) survive; the colours, contrast range and tone are the
target's own. Reference alpha is kept, so alpha-0 conventions (eyes, inner faces) carry over, and the
reference's EYE texels are copied verbatim (an eye is not a hide colour). Only texels the target has
not painted are written -- an adult rect the calf shares is never touched.

    python scripts/paint_juvenile.py              # drafts -> scratch/juvenile/<type>/<skin>.png
    python scripts/paint_juvenile.py --install    # copy drafts over the shipped skins
"""
import colorsys
import pathlib
import shutil
import sys

from PIL import Image

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from juvenile_census import texels, JUVENILES

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/untamedwilds/textures/entity"
OUT = ROOT / "scratch/juvenile"

# type -> {target skin: reference skin}
PLAN = {
    "big_cat": {
        "american_cheetah": "leopard_1", "cheetah": "leopard_1",
        "barbary_lion_female": "lion_1_female", "barbary_lion_male": "lion_1_male",
        "caspian_tiger": "tiger", "homotherium": "lion_1_female",
        "smilodon_fatalis": "lion_1_female", "sabertooth": "lion_1_female",
        "thylacosmilus": "marsupial_lion",
        "cave_lion_male": "lion_1_female", "cave_lion_female": "lion_1_female",   # 2026-10-09 repaint
        "puma": "lion_1_female",   # 2026-11-15 repaint: spotted kittens, as real puma kittens are
        "clouded_leopard": "leopard_1",   # 2.9.0 new species: spotted cubs
    },
    "bear": {"arctotherium": "spectacled", "short_faced": "brown_1", "cave": "brown_1"},   # cave: 2026-10-09 repaint
    "boar": {
        "babirusa": "peccary", "platygonus": "peccary", "giant_warthog": "warthog",
        "giant_forest_hog": "red_river", "kubanochoerus": "warthog",
        "warthog": "giant_warthog",   # 2026-11-15 repaint: its piglet came from the original warthog piglet
    },
    "bison": {s: "wisent" for s in ("african_buffalo", "banteng", "gaur", "long_horned", "pelorovis",
                                    "steppe", "water_buffalo", "wild_yak", "giant_buffalo",   # giant_buffalo: 2026-10-09 repaint
                                    "bootherium", "antiquus")},   # 2.9.0 new species
    "rhino": {"mercks": "sumatran", "wooly": "sumatran"},   # wooly: 2026-10-09 repaint
}
MODEL = {folder: model for model, (folder, _) in JUVENILES.items()}
# Contrast squeeze toward the palette median for targets whose adult palette is too high-contrast to map
# cleanly (a quantile map then turns soft piglet shading into camouflage blotches). 1.0 = untouched.
CONTRAST = {"giant_warthog": 0.45}


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def is_eye(c):
    """Eye texels: saturated or near-black small features that must not be recoloured as hide."""
    r, g, b = (v / 255 for v in c[:3])
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    return l < 0.09 or (s > 0.55 and l > 0.25 and not (0.03 < h < 0.14))


def adult_palette(im, exclude):
    """Opaque adult texels sorted by luminance (the colour ramp)."""
    a = im.load()
    px = [a[x, y] for y in range(im.height) for x in range(im.width) if a[x, y][3] > 0 and (x, y) not in exclude]
    px.sort(key=lum)
    return px


def ramp_at(palette, q, window=0.015):
    """Average colour of the palette around luminance quantile q (smooths single-texel noise)."""
    n = len(palette)
    i0, i1 = max(0, int((q - window) * n)), min(n, int((q + window) * n) + 1)
    chunk = palette[i0:i1] or [palette[min(n - 1, int(q * n))]]
    return tuple(int(sum(c[k] for c in chunk) / len(chunk)) for k in range(3))


def paint(folder, target, reference, contrast=1.0):
    juv, size = texels(MODEL[folder])
    tgt = Image.open(TEX / folder / (target + ".png")).convert("RGBA")
    ref = Image.open(TEX / folder / (reference + ".png")).convert("RGBA")
    assert tgt.size == ref.size == size, (target, tgt.size, ref.size, size)
    ref_pal = adult_palette(ref, juv)
    ref_lums = [lum(c) for c in ref_pal]
    tgt_pal = adult_palette(tgt, juv)
    t, r = tgt.load(), ref.load()
    out = tgt.copy()
    o = out.load()
    written = 0
    import bisect
    for p in juv:
        if t[p][3] > 0 or r[p][3] == 0:
            continue                                         # already painted, or meant to be empty
        c = r[p]
        if is_eye(c):
            o[p] = c
        else:
            q = bisect.bisect_left(ref_lums, lum(c)) / max(1, len(ref_lums) - 1)
            q = 0.5 + (q - 0.5) * contrast
            o[p] = ramp_at(tgt_pal, min(1.0, q)) + (c[3],)
        written += 1
    cov = sum(1 for p in juv if o[p][3] > 0) / len(juv)
    ref_cov = sum(1 for p in juv if r[p][3] > 0) / len(juv)
    return out, written, cov, ref_cov


def main():
    install = "--install" in sys.argv
    for folder, plan in PLAN.items():
        for target, reference in plan.items():
            out, written, cov, ref_cov = paint(folder, target, reference, CONTRAST.get(target, 1.0))
            dst = OUT / folder / (target + ".png")
            dst.parent.mkdir(parents=True, exist_ok=True)
            out.save(dst)
            print("%-8s %-22s <- %-14s wrote %4d texels, juvenile coverage %5.1f%% (reference %5.1f%%)"
                  % (folder, target, reference, written, cov * 100, ref_cov * 100))
            if install:
                shutil.copy(dst, TEX / folder / (target + ".png"))


if __name__ == "__main__":
    main()
