"""Draft batch 3 art (2026-10-06): carapace shield, hunting spears (6 vanilla tiers + 2 trims),
taxidermy kit, mounted specimen. Vanilla-derived where vanilla has the shape (shield texture layout,
sword-blade tier palettes, stick colours).

    python scripts/gen_batch3.py <vanilla_textures_dir>            # drafts + review sheet
    python scripts/gen_batch3.py <vanilla_textures_dir> --install  # copy approved drafts into assets/
"""
import os
import sys
import shutil
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_gear import scutes, hx, lum   # same overlapping-scute treatment as the carapace armour

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, 'src/main/resources/assets/untamedwilds/textures')
OUT = os.path.join(ROOT, 'scratch/batch3')

SPEAR = [
    "..............oo",
    ".............oLo",
    "............oLMo",
    "...........oLMMo",
    "..........oLMMDo",
    ".........oLMMDo.",
    "........obMDDo..",
    ".......sbbooo...",
    "......sS........",
    ".....sS.........",
    "....sS..........",
    "...sS...........",
    "..sS............",
    ".sS.............",
    "sS..............",
    "................",
]
TRIM_TOOTH = {(9, 8): '#d8d0bc', (10, 8): '#f0eadc', (9, 9): '#b8b09c', (10, 9): '#e0d8c6', (9, 10): '#8e8676'}
TRIM_ANTLER = {(7, 6): '#7a6248', (6, 5): '#a68c6a', (6, 4): '#c4ac88', (6, 3): '#dccaa8', (5, 5): '#c4ac88', (4, 5): '#dccaa8'}

KIT = [  # a rolled leather taxidermy kit tied with string, a needle through it
    "................",
    "................",
    "...........o....",
    "..........oNo...",
    ".........oNo....",
    "...oooooonoooo..",
    "..oLLLLLMMMMDo..",
    "..oLMMMMMMMMDo..",
    "..owwwwwwwwwwo..",
    "..oLMMMMMMMMDo..",
    "..oMMMMMMMMDDo..",
    "...oooooooooo...",
    "................",
    "................",
    "................",
    "................",
]
SPECIMEN = [  # a standing mounted quadruped (ears, head, back, legs, tail) on an oak display base
    "................",
    "................",
    "..o.o...........",
    ".oLoLo..........",
    ".oLLLLoooooooo..",
    ".oaLLLLLLLLLLLo.",
    "..oLMMMMMMMMMDoo",
    "...oMMMMMMMMMDo.",
    "...oMMMMMMMMDDo.",
    "...oMo.oo.oMDo..",
    "...oMo....oDo...",
    "..oooooooooooooo",
    ".obbbbbbbbbbbbbo",
    ".oBBBBBBBBBBBBBo",
    "..ooooooooooooo.",
    "................",
]


def ramp_from(img):
    """4 tones (outline, dark, mid, light) from an image's opaque texels, by brightness."""
    px = sorted([p[:3] for p in img.convert('RGBA').getdata() if p[3]], key=lum)
    n = len(px)
    pick = lambda f: px[min(n - 1, int(n * f))]
    return pick(0.04), pick(0.30), pick(0.62), pick(0.92)


def blade(sword):
    """The blade texels of a vanilla sword: everything that isn't the brown wooden handle (bottom-left)."""
    out = []
    for y in range(16):
        for x in range(16):
            r, g, b, a = sword.getpixel((x, y))
            if a and x + (15 - y) > 12:             # upper-right half of the diagonal = blade
                out.append((r, g, b))
    img = Image.new('RGBA', (len(out), 1))
    for i, c in enumerate(out):
        img.putpixel((i, 0), c + (255,))
    return img


def draw(rows, pal):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), pal[ch] + (255,))
    return img


def build(V):
    for d in ('item', 'entity'):
        os.makedirs(os.path.join(OUT, d), exist_ok=True)
    made = {}
    stick = ramp_from(Image.open(os.path.join(V, 'item/stick.png')))
    bind = (hx('#5c3a1e'), hx('#8a5a32'))
    for tier in ('wooden', 'stone', 'iron', 'golden', 'diamond', 'netherite'):
        o, d, m, l = ramp_from(blade(Image.open(os.path.join(V, 'item/%s_sword.png' % tier)).convert('RGBA')))
        pal = {'o': o, 'D': d, 'M': m, 'L': l, 's': stick[2], 'S': stick[1], 'b': bind[1]}
        made['item/%s_spear' % tier] = draw(SPEAR, pal)
    for name, trim in (('spear_trim_tooth', TRIM_TOOTH), ('spear_trim_antler', TRIM_ANTLER)):
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for (x, y), c in trim.items():
            img.putpixel((x, y), hx(c) + (255,))
        made['item/' + name] = img
    # taxidermy kit: vanilla leather, a steel needle through it trailing white thread
    kit = Image.open(os.path.join(V, 'item/leather.png')).convert('RGBA')
    for (x, y), c in {(12, 2): '#e4e8ec', (11, 3): '#c8ccd0', (10, 4): '#a8acb2', (9, 5): '#8a8e94', (8, 6): '#8a8e94',
                      (13, 1): '#f4f6f8', (7, 7): '#ece6d6', (6, 8): '#ece6d6', (6, 9): '#d8d0bc', (7, 10): '#ece6d6',
                      (8, 11): '#d8d0bc'}.items():
        kit.putpixel((x, y), hx(c) + (255,))
    made['item/taxidermy_kit'] = kit
    made['item/mounted_specimen'] = draw(SPECIMEN, {'o': hx('#2c1e12'), 'L': hx('#a8784c'), 'l': hx('#c49a6a'), 'M': hx('#7c5232'),
                                                    'D': hx('#5a3820'), 'a': hx('#1a1a1a'), 'b': hx('#b8945a'), 'B': hx('#8a6a3c')})
    # carapace shield: the vanilla shield texture with its wooden face turned into scute plates; the
    # iron rim (grey texels) is kept
    shield = Image.open(os.path.join(V, 'entity/shield_base_nopattern.png')).convert('RGBA')
    wood = Image.new('RGBA', shield.size, (0, 0, 0, 0))
    rim = shield.copy()
    for y in range(shield.height):
        for x in range(shield.width):
            r, g, b, a = shield.getpixel((x, y))
            if a and r > b + 18:                     # brown wood, not the grey iron rim
                wood.putpixel((x, y), (r, g, b, a))
                rim.putpixel((x, y), (0, 0, 0, 0))
    plates = scutes(wood, hx('#c8a272'), hx('#94704a'), hx('#6a4c30'), hx('#2c1e12'))
    rim.alpha_composite(plates)
    made['entity/carapace_shield'] = rim
    for k, img in made.items():
        img.save(os.path.join(OUT, k + '.png'))
    # review sheet
    S = 8
    items = [k for k in made if made[k].size == (16, 16)]
    sheet = Image.new('RGBA', (len(items) * (16 * S + 10) + 10 + 64 * 4 + 20, max(16 * S + 30, 64 * 4 + 30)), (236, 232, 224, 255))
    dr = ImageDraw.Draw(sheet)
    for i, k in enumerate(items):
        x = 10 + i * (16 * S + 10)
        img = made[k]
        if 'trim' in k:                              # show trims on top of the iron spear
            base = made['item/iron_spear'].copy()
            base.alpha_composite(img)
            img = base
        sheet.alpha_composite(img.resize((16 * S, 16 * S), Image.NEAREST), (x, 8))
        dr.text((x, 16 * S + 12), k.split('/')[1], fill=(30, 30, 30, 255))
    x = 10 + len(items) * (16 * S + 10)
    sheet.alpha_composite(made['entity/carapace_shield'].resize((64 * 4, 64 * 4), Image.NEAREST), (x, 8))
    dr.text((x, 64 * 4 + 12), 'carapace_shield (texture)', fill=(30, 30, 30, 255))
    sheet.save(os.path.join(ROOT, 'scratch/batch3_sheet.png'))
    print('drafted', len(made))


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
