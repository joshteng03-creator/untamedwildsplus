"""Draft the crafted-gear art (2026-10-06): fur armour, carapace chestplate, serrated arrow, herding
horn, ivory blocks. Vanilla principle: gear keeps vanilla's silhouettes, so every design is derived
from the matching vanilla 1.18.2 texture (extracted to the scratchpad for reference) recoloured into
the material's palette, plus one identifying detail each.

    python scripts/gen_gear.py <vanilla_textures_dir>            # drafts + review sheet
    python scripts/gen_gear.py <vanilla_textures_dir> --install  # copy approved drafts into assets/
"""
import os
import sys
import shutil
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, 'src/main/resources/assets/untamedwilds/textures')
OUT = os.path.join(ROOT, 'scratch/gear')


def hx(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def ramp_map(img, ramp, only=None):
    """Recolour by brightness through a dark->light ramp. `only(r,g,b)` limits which texels change."""
    out = img.copy().convert('RGBA')
    px = [p for p in out.getdata() if p[3]]
    if not px:
        return out
    ls = sorted(lum(p) for p in px)
    lo, hi = ls[int(len(ls) * 0.03)], ls[int(len(ls) * 0.97)]
    n = len(ramp) - 1
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            if not a or (only and not only(r, g, b)):
                continue
            t = min(0.999, max(0.0, (lum((r, g, b)) - lo) / max(1.0, hi - lo))) * n
            i = int(t)
            c = [ramp[i][k] + (ramp[min(i + 1, n)][k] - ramp[i][k]) * (t - i) for k in range(3)]
            out.putpixel((x, y), tuple(int(v) for v in c) + (a,))
    return out


FUR = [hx('#120a05'), hx('#26170c'), hx('#3a2414'), hx('#52341e'), hx('#6c4628')]
SCUTE = [hx('#2e2116'), hx('#563d27'), hx('#755636'), hx('#94704a'), hx('#b48c60')]
IVORY_TOP = [hx('#b4a482'), hx('#cfc3a4'), hx('#e2d8c0'), hx('#f0e8d6'), hx('#faf6ea')]
IVORY = [hx('#8c7c62'), hx('#c2b494'), hx('#ddd2b6'), hx('#ece4d0'), hx('#faf6ea')]


def fringe(img, rng_seed=3):
    """Shaggy hair: under every bottom-edge texel of the silhouette hang a 1-2 px strand on alternate
    columns, darker than the fur above it -- fur reads as hair, not as a flat leather panel."""
    out = img.copy()
    W, H = out.size
    for x in range(W):
        col = [y for y in range(H) if out.getpixel((x, y))[3]]
        if not col:
            continue
        y = col[-1]
        if (x + rng_seed) % 2 == 0 and y + 1 < H:
            c = out.getpixel((x, y))
            dark = tuple(int(v * 0.7) for v in c[:3]) + (255,)
            out.putpixel((x, y + 1), dark)
            if (x + rng_seed) % 4 == 0 and y + 2 < H:
                out.putpixel((x, y + 2), tuple(int(v * 0.55) for v in c[:3]) + (255,))
    return out


def studs(img, color, step=4, phase=1):
    """Scute studs on the carapace chestplate: a lighter texel on a staggered grid inside the silhouette."""
    out = img.copy()
    for y in range(out.height):
        for x in range(out.width):
            if out.getpixel((x, y))[3] and (y % step == phase) and ((x + (y // step) * 2) % step == phase):
                out.putpixel((x, y), color + (255,))
    return out


def _h(x, y, k):
    v = (x * 374761393 + y * 668265263 + k * 2147483647) & 0xffffffff
    v = (v ^ (v >> 13)) * 1274126177 & 0xffffffff
    return (v & 0xffff) / 65535.0


def furry(img, seed=0):
    """Fur, not leather: streaked hair strands (each column gets runs of different tone, staggered
    so the strands read vertical) and a few pale tips, on top of the brightness ramp."""
    out = img.copy()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            if not a:
                continue
            run = (y + int(3 * _h(x, 0, seed))) // 2
            k = 0.72 + 0.56 * _h(x, run, seed + 1)
            if _h(x, y, seed + 2) > 0.93:
                k = 1.45                                   # pale hair tip
            out.putpixel((x, y), tuple(int(max(0, min(255, c * k))) for c in (r, g, b)) + (a,))
    return out


def shaggy(img, seed=1):
    """Ragged hair hanging off the bottom of each column (1-3 px) and tufts off the side edges."""
    out = img.copy()
    W, H = out.size
    src = img.copy()
    for x in range(W):
        col = [y for y in range(H) if src.getpixel((x, y))[3]]
        if not col:
            continue
        y = col[-1]
        n = int(_h(x, 7, seed) * 3.2)
        c = src.getpixel((x, y))
        for i in range(1, n + 1):
            if y + i < H:
                out.putpixel((x, y + i), tuple(int(v * (0.8 - 0.15 * i)) for v in c[:3]) + (255,))
    for y in range(H):
        row = [x for x in range(W) if src.getpixel((x, y))[3]]
        if not row:
            continue
        for x, dx in ((row[0], -1), (row[-1], 1)):
            if _h(y, x, seed + 5) > 0.55 and 0 <= x + dx < W and not src.getpixel((x + dx, y))[3]:
                c = src.getpixel((x, y))
                out.putpixel((x + dx, y), tuple(int(v * 0.7) for v in c[:3]) + (255,))
    return out


def scutes(img, light, mid, dark, seam):
    """Overlapping scute plates (pangolin / armadillo bands): staggered rows of rounded plates, each with
    a lit top edge, a dark shadow line where the row below overlaps it, and dark seams between plates.
    Only texels inside the silhouette change, so the outline and the UV layout are kept."""
    out = img.copy()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            if not a:
                continue
            row = y // 3
            off = 2 if row % 2 else 0
            lx, ly = (x + off) % 4, y % 3
            # one scute = a 4x3 cell, lit along its top, shading down to its bottom, with dark notches
            # only at the bottom corners where it meets its neighbours -- so each row reads as a band of
            # rounded plates tucked UNDER the lit top of the row below (no straight mortar lines).
            TILE = [[mid, light, light, mid],
                    [mid, mid, mid, mid],
                    [seam, dark, dark, seam]]
            c = TILE[ly][lx]
            # keep the original silhouette's outline darkness
            if lum((r, g, b)) < 60:
                c = tuple(int(v * 0.6) for v in dark)
            out.putpixel((x, y), c + (a,))
    return out


def with_overlay(V, base_rel):
    """Vanilla leather pieces are a dyed base + an undyed overlay (the helmet's centre, the laces);
    composite them so the recolour sees the whole piece."""
    base = Image.open(os.path.join(V, base_rel + '.png')).convert('RGBA')
    ov = os.path.join(V, base_rel + '_overlay.png')
    if os.path.exists(ov):
        base.alpha_composite(Image.open(ov).convert('RGBA'))
    return base


def build(V):
    os.makedirs(OUT, exist_ok=True)
    made = {}
    # fur armour icons (leather shapes, fur ramp, hair fringe) + worn layers (no fringe on the UV map)
    for piece in ('helmet', 'chestplate', 'leggings', 'boots'):
        base = with_overlay(V, 'item/leather_%s' % piece)
        made['item/fur_%s' % piece] = shaggy(furry(ramp_map(base, FUR), seed=len(piece)))
    for layer in ('1', '2'):
        made['models/armor/fur_layer_%s' % layer] = furry(ramp_map(with_overlay(V, 'models/armor/leather_layer_%s' % layer), FUR), seed=int(layer))
    # carapace chestplate: iron chestplate shape in the scute palette, with studs
    plate = (hx('#c8a272'), hx('#94704a'), hx('#6a4c30'), hx('#2c1e12'))
    # the full carapace set (user, 2026-10-06): vanilla iron silhouettes, overlapping scute plates
    for piece in ('helmet', 'chestplate', 'leggings', 'boots'):
        made['item/carapace_%s' % piece] = scutes(Image.open(os.path.join(V, 'item/iron_%s.png' % piece)).convert('RGBA'), *plate)
    for layer in ('1', '2'):
        made['models/armor/carapace_layer_%s' % layer] = scutes(Image.open(os.path.join(V, 'models/armor/iron_layer_%s.png' % layer)).convert('RGBA'), *plate)
    # serrated arrow: vanilla arrow, flint head re-toned to shark-tooth cream
    is_head = lambda r, g, b: abs(r - g) < 14 and abs(g - b) < 14   # the grey flint head (shaft/fletching are coloured)
    made['item/serrated_arrow'] = ramp_map(Image.open(os.path.join(V, 'item/arrow.png')), [hx('#6e6658'), hx('#b8b09c'), hx('#ece6d6')], only=is_head)
    made['entity/projectiles/serrated_arrow'] = ramp_map(Image.open(os.path.join(V, 'entity/projectiles/arrow.png')), [hx('#6e6658'), hx('#b8b09c'), hx('#ece6d6')], only=is_head)
    # herding horn: the mod's horn with a copper band + mouthpiece at the narrow end
    horn = Image.open(os.path.join(TEX, 'item/material/horn.png')).convert('RGBA')
    hh = horn.copy()
    for (x, y) in [(2, 4), (3, 4), (2, 5), (3, 5), (3, 6), (4, 6), (2, 3), (3, 3)]:
        if hh.getpixel((x, y))[3]:
            c = (0xd8, 0x84, 0x52) if (x + y) % 2 else (0xa8, 0x5a, 0x34)
            hh.putpixel((x, y), c + (255,))
    made['item/herding_horn'] = hh
    # ivory blocks: bone block grain re-toned to ivory; chiseled from chiseled quartz
    made['block/ivory_block_side'] = ramp_map(Image.open(os.path.join(V, 'block/bone_block_side.png')), IVORY)
    made['block/ivory_block_top'] = ramp_map(Image.open(os.path.join(V, 'block/bone_block_top.png')), IVORY_TOP)
    made['block/chiseled_ivory'] = ramp_map(Image.open(os.path.join(V, 'block/chiseled_quartz_block.png')), IVORY)
    made['block/chiseled_ivory_top'] = ramp_map(Image.open(os.path.join(V, 'block/chiseled_quartz_block_top.png')), IVORY)
    for k, img in made.items():
        p = os.path.join(OUT, k + '.png')
        os.makedirs(os.path.dirname(p), exist_ok=True)
        img.save(p)
    # review sheet: icons/blocks at x8; armour layers + arrow projectile at x4
    small = [k for k in made if made[k].size == (16, 16)]
    big = [k for k in made if made[k].size != (16, 16)]
    S = 8
    W = max(len(small) * (16 * S + 12), sum(made[k].width * 4 + 12 for k in big)) + 12
    sheet = Image.new('RGBA', (W, 16 * S + 40 + 32 * 4 + 40), (236, 232, 224, 255))
    d = ImageDraw.Draw(sheet)
    for i, k in enumerate(small):
        x = 12 + i * (16 * S + 12)
        sheet.alpha_composite(made[k].resize((16 * S, 16 * S), Image.NEAREST), (x, 8))
        d.text((x, 16 * S + 14), k.split('/')[-1], fill=(30, 30, 30, 255))
    x = 12
    y0 = 16 * S + 36
    for k in big:
        im = made[k].resize((made[k].width * 4, made[k].height * 4), Image.NEAREST)
        sheet.alpha_composite(im, (x, y0))
        d.text((x, y0 + im.height + 4), k.split('/')[-1], fill=(30, 30, 30, 255))
        x += im.width + 12
    sheet = sheet.crop((0, 0, W, y0 + 32 * 4 + 24))
    sheet.save(os.path.join(ROOT, 'scratch/gear_sheet.png'))
    print('drafted', len(made), '->', OUT)


def install():
    for root, _, files in os.walk(OUT):
        for f in files:
            rel = os.path.relpath(os.path.join(root, f), OUT)
            dst = os.path.join(TEX, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy(os.path.join(root, f), dst)
            print('installed', rel)


if __name__ == '__main__':
    install() if '--install' in sys.argv else build(sys.argv[1])
