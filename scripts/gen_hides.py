"""Generate per-species hide items + rug textures for bears and big cats (2026-10-06).

Each new hide starts from the closest EXISTING hand-made hide (its shape, outline and pattern --
spots, stripes or plain fur) and is recoloured by brightness into the species' own skin palette:
the template's dark / mid / light tones map onto the skin's dark / mid / light body tones. That keeps
the mod's hide art style and makes every hide read as its animal.

    python scripts/gen_hides.py            # writes candidates to scratch/hides/ + a review sheet
    python scripts/gen_hides.py --install  # copies approved candidates into assets/
"""
import os
import sys
import shutil
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, 'src/main/resources/assets/untamedwilds/textures')
OUT = os.path.join(ROOT, 'scratch/hides')

# species -> (type, item/carpet template, recolour mode)
NEW = {
    # bears (template: brown bear hide / rug)
    'bear_cave':            ('bear', 'bear_brown', 'ramp'),
    'bear_panda':           ('bear', 'bear_white', 'panda'),
    'bear_spectacled':      ('bear', 'bear_black', 'spectacled'),
    'bear_sun':             ('bear', 'bear_black', 'sun'),
    'bear_short_faced':     ('bear', 'bear_brown', 'ramp'),
    'bear_arctotherium':    ('bear', 'bear_brown', 'ramp'),
    # big cats: spotted -> leopard, striped -> tiger, plain -> lion / puma
    'bigcat_cave_lion':        ('big_cat', 'bigcat_lion', 'ramp'),
    'bigcat_american_lion':    ('big_cat', 'bigcat_lion', 'ramp'),
    'bigcat_barbary_lion':     ('big_cat', 'bigcat_lion', 'ramp'),
    'bigcat_homotherium':      ('big_cat', 'bigcat_lion', 'ramp'),
    'bigcat_marsupial_lion':   ('big_cat', 'bigcat_tiger', 'ramp'),
    'bigcat_sabertooth':       ('big_cat', 'bigcat_leopard', 'ramp'),
    'bigcat_cheetah':          ('big_cat', 'bigcat_lion', 'cheetah'),
    'bigcat_american_cheetah': ('big_cat', 'bigcat_puma', 'ramp'),
    'bigcat_caspian_tiger':    ('big_cat', 'bigcat_tiger', 'ramp'),
    'bigcat_thylacosmilus':    ('big_cat', 'bigcat_puma', 'ramp'),
    'bigcat_smilodon_fatalis': ('big_cat', 'bigcat_tiger', 'soft'),
}

# skin file per species (first common skin; dimorphic cats use the male)
SKIN = {
    'bear_cave': 'bear/cave', 'bear_panda': 'bear/panda', 'bear_spectacled': 'bear/spectacled',
    'bear_sun': 'bear/sun', 'bear_short_faced': 'bear/short_faced', 'bear_arctotherium': 'bear/arctotherium',
    'bigcat_cave_lion': 'big_cat/cave_lion_male', 'bigcat_american_lion': 'big_cat/american_lion_male',
    'bigcat_barbary_lion': 'big_cat/barbary_lion_male', 'bigcat_homotherium': 'big_cat/homotherium',
    'bigcat_marsupial_lion': 'big_cat/marsupial_lion', 'bigcat_sabertooth': 'big_cat/sabertooth',
    'bigcat_cheetah': 'big_cat/cheetah', 'bigcat_american_cheetah': 'big_cat/american_cheetah',
    'bigcat_caspian_tiger': 'big_cat/caspian_tiger', 'bigcat_thylacosmilus': 'big_cat/thylacosmilus',
    'bigcat_smilodon_fatalis': 'big_cat/smilodon_fatalis',
}


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def body_tones(skin_path):
    """Dark / mid / light tones of the skin's body fur: opaque, non-extreme pixels by brightness."""
    im = Image.open(skin_path).convert('RGBA')
    px = [p[:3] for p in im.getdata() if p[3] > 0]
    px = [p for p in px if 18 < lum(p) < 245]          # drop eye blacks and pure highlights
    px.sort(key=lum)
    n = len(px)

    def avg(a, b):
        seg = px[int(n * a):max(int(n * a) + 1, int(n * b))]
        return tuple(sum(c[i] for c in seg) / len(seg) for i in range(3))
    return avg(0.05, 0.25), avg(0.40, 0.60), avg(0.80, 0.95)


def tones_of(img):
    px = sorted([p[:3] for p in img.getdata() if p[3] > 0], key=lum)
    return lum(px[int(len(px) * 0.15)]), lum(px[len(px) // 2]), lum(px[int(len(px) * 0.88)])


def ramp_recolour(template, tones):
    """Map the template's brightness onto the species' dark/mid/light tones, keeping its alpha."""
    t_lo, t_mid, t_hi = tones_of(template)
    dark, mid, light = tones
    out = Image.new('RGBA', template.size)
    for y in range(template.height):
        for x in range(template.width):
            r, g, b, a = template.getpixel((x, y))
            if a == 0:
                continue
            L = lum((r, g, b))
            if L <= t_mid:
                t = 0.0 if t_mid == t_lo else max(0.0, (L - t_lo) / (t_mid - t_lo))
                c = [dark[i] + (mid[i] - dark[i]) * min(1.0, t) for i in range(3)]
                if L < t_lo:                               # template's deepest shadow stays darker
                    c = [v * max(0.45, L / max(1.0, t_lo)) for v in c]
            else:
                t = 1.0 if t_hi == t_mid else min(1.0, (L - t_mid) / (t_hi - t_mid))
                c = [mid[i] + (light[i] - mid[i]) * t for i in range(3)]
                if L > t_hi:
                    c = [min(255, v * (L / max(1.0, t_hi))) for v in c]
            out.putpixel((x, y), tuple(int(max(0, min(255, v))) for v in c) + (a,))
    return out


# Bear pelt layout (16x16 item, read off bear_black/bear_white): head rows 0-2 (snout tip row 0,
# cols 6-8), forelegs out to the edges on rows 3-5, body rows 6-12, hind legs in the bottom corners,
# tail bottom-centre on row 15.
PANDA_BLACK = set()
for y in range(3, 6):
    for x in range(16):
        if x <= 3 or x >= 12:
            PANDA_BLACK.add((x, y))                     # forelegs
for y in range(4, 7):
    for x in range(3, 13):
        PANDA_BLACK.add((x, y))                         # shoulder saddle joining the forelegs
for y in range(12, 16):
    for x in range(16):
        if x <= 4 or x >= 11:
            PANDA_BLACK.add((x, y))                     # hind legs
PANDA_BLACK |= {(3, 2), (4, 2), (11, 2), (12, 2)}      # ears
PANDA_BLACK |= {(6, 1), (8, 1)}                        # eye patches
SUN_CRESCENT = {(4, 5), (5, 6), (6, 7), (7, 7), (8, 7), (9, 7), (10, 6), (11, 5)}
SPECT_RINGS = {(6, 0), (7, 0), (8, 0), (5, 1), (9, 1), (5, 2), (6, 2), (8, 2), (9, 2)}  # cream muzzle + rings round the eyes
SPECT_THROAT = {(6, 4), (7, 4), (8, 4), (7, 5), (8, 5), (7, 6)}  # cream throat blaze


def shade(col, L):
    """A flat marking colour, shaded by the template texel's own brightness so the fur texture shows."""
    k = 0.75 + 0.5 * (L / 255.0)
    return tuple(int(max(0, min(255, c * k))) for c in col)


def mark(img, template, cells, col):
    out = img.copy()
    for (x, y) in cells:
        r, g, b, a = template.getpixel((x, y))
        if a:
            out.putpixel((x, y), shade(col, lum((r, g, b))) + (a,))
    return out


def panda_recolour(template):
    """Giant panda pelt: white body and head, black legs, shoulder saddle, ears and eye patches."""
    out = template.copy().convert('RGBA')
    return mark(out, template, PANDA_BLACK, (34, 32, 34))


def panda_rug(template):
    """Panda rug: the white fur with irregular, organic black blotches. Built from periodic noise over
    the 16-px tile so the pattern tiles seamlessly across rug blocks (no grid, no seams)."""
    import math
    out = template.copy().convert('RGBA')
    for y in range(16):
        for x in range(16):
            u, v = 2 * math.pi * x / 16, 2 * math.pi * y / 16
            n = (math.sin(u + 0.7) * math.cos(v * 1.0 + 1.3) + 0.6 * math.sin(2 * u + 2 * v + 0.4)
                 + 0.45 * math.cos(3 * u - v + 2.1) + 0.35 * math.sin(u - 2 * v + 0.9))
            if n > 0.55:
                r, g, b, a = out.getpixel((x, y))
                out.putpixel((x, y), shade((34, 32, 34), lum((r, g, b))) + (a,))
    return out


def tint(img, col, amt):
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a:
                out.putpixel((x, y), tuple(int(c + (t - c) * amt) for c, t in zip((r, g, b), col)) + (a,))
    return out


def flecks(img, col, every):
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            if (x * 7 + y * 13 + (x * y) % 5) % every == 0:
                r, g, b, a = img.getpixel((x, y))
                if a:
                    out.putpixel((x, y), shade(col, lum((r, g, b))) + (a,))
    return out


def soften(img, amt):
    """Pull every texel's colour toward the image's mean by amt -- faint barring instead of bold stripes."""
    px = [p for p in img.getdata() if p[3]]
    mean = [sum(p[i] for p in px) / len(px) for i in range(3)]
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a:
                out.putpixel((x, y), tuple(int(c + (m - c) * amt) for c, m in zip((r, g, b), mean)) + (a,))
    return out


def cheetah_spots(img, template, col):
    """Solid round dark spots (a cheetah's, not a leopard's open rosettes): one texel each on a
    staggered 4-px grid -- 4 divides 16, so the rug tiles without seams."""
    cells = set()
    for y in range(16):
        for x in range(16):
            if y % 4 == 1 and x % 4 == 1:
                cells.add((x, y))
            elif y % 4 == 3 and x % 4 == 3:
                cells.add((x, y))
    return mark(img, template, cells, col)


def build():
    os.makedirs(OUT, exist_ok=True)
    rows = []
    for key, (typ, tmpl, mode) in NEW.items():
        item_t = Image.open(os.path.join(TEX, 'item/hide', tmpl.replace('bigcat_', 'big_cat_') + '.png')).convert('RGBA')
        rug_t = Image.open(os.path.join(TEX, 'block/carpet', tmpl + '.png')).convert('RGBA')
        skin_path = os.path.join(TEX, 'entity', SKIN[key] + '.png')
        if mode == 'panda':
            item, rug = panda_recolour(item_t), panda_rug(rug_t)
        else:
            tones = body_tones(skin_path)
            item, rug = ramp_recolour(item_t, tones), ramp_recolour(rug_t, tones)
            if mode == 'sun':
                # the sun bear's gold-cream chest crescent + a tan muzzle; rug a touch warmer
                item = mark(item, item_t, SUN_CRESCENT, (226, 172, 74))
                item = mark(item, item_t, {(6, 0), (7, 0), (8, 0)}, (168, 132, 92))
                rug = tint(rug, (92, 60, 30), 0.18)
            elif mode == 'soft':
                # Smilodon fatalis: its skin carries only faint broken barring, so the tiger stripes are
                # pulled 55% toward the coat's mean colour
                item, rug = soften(item, 0.55), soften(rug, 0.55)
            elif mode == 'cheetah':
                spot = tuple(int(c * 0.35) for c in tones[0])
                item = cheetah_spots(item, item_t, spot)
                rug = cheetah_spots(rug, rug_t, spot)
            elif mode == 'spectacled':
                # the spectacled bear's cream "spectacles" round the eyes, muzzle and throat blaze;
                # rug cool black with sparse cream flecks
                item = mark(item, item_t, SPECT_RINGS | SPECT_THROAT, (232, 220, 190))
                rug = flecks(rug, (220, 206, 176), 11)
        item.save(os.path.join(OUT, 'item_' + key + '.png'))
        rug.save(os.path.join(OUT, 'rug_' + key + '.png'))
        rows.append((key, skin_path, item, rug))
    # review sheet: skin thumbnail | hide x8 | rug x8 (2x2 tiled, so seams show) | label
    S = 8
    cell = 16 * S
    sheet = Image.new('RGBA', (cell * 4 + 260, (cell + 10) * len(rows) + 10), (236, 232, 224, 255))
    d = ImageDraw.Draw(sheet)
    for i, (key, skin_path, item, rug) in enumerate(rows):
        y = 10 + i * (cell + 10)
        sk = Image.open(skin_path).convert('RGBA')
        sk.thumbnail((cell, cell))
        sheet.alpha_composite(sk, (10, y))
        sheet.alpha_composite(item.resize((cell, cell), Image.NEAREST), (cell + 20, y))
        tile = Image.new('RGBA', (32, 32))
        for tx in (0, 16):
            for ty in (0, 16):
                tile.alpha_composite(rug, (tx, ty))
        sheet.alpha_composite(tile.resize((cell * 2, cell * 2), Image.NEAREST).crop((0, 0, cell * 2, cell)), (cell * 2 + 30, y))
        d.text((cell * 4 + 40, y + cell // 2 - 6), key.replace('bigcat_', '').replace('bear_', '') + (' (bear)' if key.startswith('bear') else ''), fill=(30, 30, 30, 255))
    sheet.save(os.path.join(ROOT, 'scratch/hides_sheet.png'))
    print('wrote', len(rows), 'hides + rugs ->', OUT, 'and scratch/hides_sheet.png')


def install():
    for key in NEW:
        shutil.copy(os.path.join(OUT, 'item_' + key + '.png'),
                    os.path.join(TEX, 'item/hide', key.replace('bigcat_', 'big_cat_') + '.png'))
        shutil.copy(os.path.join(OUT, 'rug_' + key + '.png'), os.path.join(TEX, 'block/carpet', key + '.png'))
    print('installed', len(NEW))


if __name__ == '__main__':
    install() if '--install' in sys.argv else build()
