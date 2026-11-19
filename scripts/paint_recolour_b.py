#!/usr/bin/env python3
"""2.9.0 group B: new species whose body exists but whose type has no painter (hippo, equid, tortoise), made by the
luminance-rank recolour of paint_renamed.py -- every painted shade, countershade and marking of the base skin survives,
only the palette changes -- with PER-PART ramps (a tortoise's shell and its skin are different materials). Eyes, teeth,
hooves are copied unchanged. Colours follow scratch/new_species_research.json (each one placed apart from the shipped
siblings: the research measured their mean colours).

    python scripts/paint_recolour_b.py            # drafts -> scratch/recolour_b/
    python scripts/paint_recolour_b.py --install
"""
import bisect
import pathlib
import shutil
import sys

from PIL import Image

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paint_renamed import hx, lum, part_texels, ramp_at, TEX  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "scratch/recolour_b"

SHELL = ("main_body", "shape8", "body_main")
JOBS = {
    # Hippopotamus antiquus: cooler, greyer slate than the common hippo, pinkish-brown low on the flanks
    "hippo/european": dict(model="ModelHippo", base="hippo/common_1", keep=("eye", "tooth"),
                           ramp=["#232223", "#36322f", "#48413d", "#5c524b", "#76665b", "#977f71"]),   # (first ramp read light pink)
    # Choeropsis liberiensis: glossy greenish-black above, creamy grey below
    "hippo/pygmy": dict(model="ModelHippo", base="hippo/common_1", keep=("eye", "tooth"),
                        ramp=["#121614", "#202624", "#313834", "#4a504a", "#7a7a70", "#bab3a4"]),
    # Hippopotamus lemerlei: lighter and pinker than the common hippo, so it never reads as a calf
    "hippo/malagasy_dwarf": dict(model="ModelHippo", base="hippo/common_1", keep=("eye", "tooth"),
                                 ramp=["#3e3532", "#5c4c46", "#7a635a", "#98786c", "#b78f82", "#d6b1a2"]),
    # Equus kiang: rich chestnut, chocolate dorsal stripe and mane, bright white legs / belly / muzzle
    "equid/kiang": dict(model="ModelEquid", base="equid/onager", keep=("eye", "hoof"),
                        # (first ramp mapped the onager's dithered belly fringe to blotchy chestnut-white: more white stops)
                        ramp=["#24140c", "#4e2a18", "#77401f", "#97552c", "#e6d6c2", "#f4eee4", "#f7f2eb"]),
    # Haringtonhippus francisci: cool greyish mouse dun (between the pale European wild ass and the tarpan)
    "equid/haringtonhippus": dict(model="ModelEquid", base="equid/european_wild_ass", keep=("eye", "hoof"),
                                  ramp=["#24201c", "#48423a", "#686054", "#837a6c", "#a39a8a", "#d5cfc0"]),
    # Aldabrachelys gigantea: dark grey-brown dome with paler tan rings; grey-brown skin
    "tortoise/aldabra": dict(model="ModelTortoise", base="tortoise/sulcata", keep=("eye",),
                             ramp=["#2a2622", "#433d37", "#5c544c", "#766c62", "#918678", "#b0a596"],
                             parts={SHELL: ["#1c1814", "#2e2620", "#43382e", "#5a4a3c", "#7a6650", "#9c8668"]}),
    # Chelonoidis niger complex: near-uniform charcoal to slate-black shell, dark grey skin
    "tortoise/galapagos": dict(model="ModelTortoise", base="tortoise/sulcata", keep=("eye",),
                               ramp=["#1e1d1c", "#2f2d2b", "#42403d", "#57534f", "#6e6964", "#8a847d"],
                               parts={SHELL: ["#121212", "#1e1e1e", "#2c2c2b", "#3c3b39", "#52504c", "#6e6b66"]}),
}


def paint(job):
    base = Image.open(TEX / (job["base"] + ".png")).convert("RGBA")
    keep = part_texels(job["model"], job["keep"])
    groups = [(part_texels(job["model"], names), [hx(c) for c in ramp]) for names, ramp in (job.get("parts") or {}).items()]
    default = [hx(c) for c in job["ramp"]]
    owner = {}
    for i, (tx, _) in enumerate(groups):
        for p in tx:
            owner.setdefault(p, i)
    # luminance ranks are taken WITHIN each group, so a dark shell and a pale skin each span their own ramp
    pools = {}
    for y in range(base.height):
        for x in range(base.width):
            c = base.getpixel((x, y))
            if c[3] == 0 or (x, y) in keep:
                continue
            pools.setdefault(owner.get((x, y), -1), []).append(lum(c))
    for k in pools:
        pools[k].sort()
    out = base.copy()
    for y in range(base.height):
        for x in range(base.width):
            c = base.getpixel((x, y))
            if c[3] == 0 or (x, y) in keep:
                continue
            g = owner.get((x, y), -1)
            pool = pools[g]
            stops = groups[g][1] if g >= 0 else default
            q = bisect.bisect_left(pool, lum(c)) / max(1, len(pool) - 1)
            out.putpixel((x, y), ramp_at(stops, q) + (c[3],))
    return out


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for key, job in JOBS.items():
        img = paint(job)
        dst = OUT / (key.replace("/", "_") + ".png")
        img.save(dst)
        if "--install" in sys.argv:
            shutil.copy(dst, TEX / (key + ".png"))
        print(key, "->", dst, "(installed)" if "--install" in sys.argv else "")


if __name__ == "__main__":
    main()
