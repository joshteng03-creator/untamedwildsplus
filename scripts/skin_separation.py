#!/usr/bin/env python3
"""Pairwise skin separation for a set of species skins sharing one rig -- SOP phase 8.

WHY THIS EXISTS. On a shared mesh the only thing distinguishing two species is the paint, and
"I gave it a completely different treatment" is NOT evidence that it looks like a different
animal. The tapir batch measured megatapirus at 16.8 from lowland -- squarely inside the
10-20 band that reads as a plain hue shift -- despite a full agouti-grizzle treatment no other
species used. Grizzling adds per-texel VARIANCE; it does not move the MEAN. Only the ground
colour does.

So every new skin is measured against ALL shipped siblings, not just the previous one, and the
report is what decides whether a repaint is needed.

Metric: mean CIELAB of the OPAQUE texels, then CIE76 dE between species means. Bands:
    < 10   the same animal in two files          -- must repaint
    10-20  reads as a hue shift of a sibling     -- repaint or go structural
    20-35  distinguishable
    > 35   clearly different animals

dE is computed on the mean, so it is deliberately blind to structure. A pair that is close on
dE but differs in STRUCTURE -- stripes vs patches vs a two-tone split -- is genuinely separable
and the report says so rather than demanding a recolour; that is exactly the route
palaeotragus and okapi take here. The structural note is data supplied by the caller, not
something the metric can see.

Usage:  python scripts/skin_separation.py <dir> [name ...]
"""
import itertools
import pathlib
import sys

import numpy as np
from PIL import Image

# Species that carry a STRUCTURAL marking, i.e. one whose identity does not live in the mean.
# Keyed by skin stem. Used only to annotate the report.
STRUCTURAL = {
    "giraffe": "bold reticulated patch network",
    "okapi": "white horizontal band field on haunch + limbs",
    "samotherium": "fine soft dappling + dark dorsal stripe",
    "palaeotragus": "hard two-tone fore/hind split",
    "sivatherium": "wrinkle folds + pale face mask",
    "helladotherium": "desaturated cool + dark crown on a hornless skull",
    "bramatherium": "coarse clumped hair + dark neck cape",
}


def srgb_to_lab(rgb):
    """rgb in 0..255 -> CIELAB (D65). Vectorised over the leading axis."""
    c = np.asarray(rgb, float) / 255.0
    c = np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)
    m = np.array([[0.4124564, 0.3575761, 0.1804375],
                  [0.2126729, 0.7151522, 0.0721750],
                  [0.0193339, 0.1191920, 0.9503041]])
    xyz = c @ m.T
    white = np.array([0.95047, 1.00000, 1.08883])
    t = xyz / white
    d = 6.0 / 29.0
    ft = np.where(t > d ** 3, np.cbrt(t), t / (3 * d * d) + 4.0 / 29.0)
    return np.stack([116 * ft[..., 1] - 16,
                     500 * (ft[..., 0] - ft[..., 1]),
                     200 * (ft[..., 1] - ft[..., 2])], -1)


def load(p):
    a = np.array(Image.open(p).convert("RGBA"), dtype=np.uint8)
    op = a[..., 3] > 8
    px = a[..., :3][op]
    lab = srgb_to_lab(px)
    return dict(name=p.stem, n=int(op.sum()), rgb=px.mean(0), lab=lab.mean(0),
                labsd=lab.std(0), size=a.shape[1::-1])


def main():
    d = pathlib.Path(sys.argv[1])
    want = sys.argv[2:]
    files = sorted(d.glob("*.png"))
    if want:
        files = [f for f in files if f.stem in want]
    S = [load(f) for f in files]
    if len(S) < 2:
        print("need at least 2 skins")
        return

    print("=" * 88)
    print(f"SKIN SEPARATION   {len(S)} skins from {d}")
    print("=" * 88)
    print(f"{'species':<17}{'size':>10}{'opaque':>9}{'L*':>7}{'a*':>7}{'b*':>7}"
          f"{'sd(L)':>7}   structure")
    for s in S:
        print(f"{s['name']:<17}{str(s['size'][0])+'x'+str(s['size'][1]):>10}{s['n']:>9}"
              f"{s['lab'][0]:7.1f}{s['lab'][1]:7.1f}{s['lab'][2]:7.1f}{s['labsd'][0]:7.1f}"
              f"   {STRUCTURAL.get(s['name'], '-')}")

    rows = []
    for a, b in itertools.combinations(S, 2):
        de = float(np.linalg.norm(a["lab"] - b["lab"]))
        rows.append((de, a["name"], b["name"]))
    rows.sort()

    print("\n" + "=" * 88)
    print("PAIRWISE dE (CIE76 on the mean)   <10 must repaint / 10-20 reads as a hue shift")
    print("=" * 88)
    bad = warn = 0
    for de, a, b in rows:
        flag = ""
        if de < 10:
            flag = "   <== TOO CLOSE, REPAINT"
            bad += 1
        elif de < 20:
            sa, sb = STRUCTURAL.get(a), STRUCTURAL.get(b)
            if sa and sb:
                flag = f"   (close on mean, but SEPARATED BY STRUCTURE: {sa} vs {sb})"
            else:
                flag = "   <== HUE-SHIFT BAND"
                warn += 1
        print(f"  {de:6.1f}   {a:<17} vs {b:<17}{flag}")
    print(f"\n  {len(rows)} pairs;  {bad} must repaint, {warn} in the hue-shift band"
          + ("   -- all clear" if not (bad or warn) else ""))


if __name__ == "__main__":
    main()
