"""Draft the new material item icons (2026-10-06): ivory, woolly fur, antler, scute, cooked whale meat.

Hand-placed 16x16 pixel art in the mod's existing item style (soft dark-brown outline, 3-4 tones lit
from the top-left, diagonal pose -- read off horn.png, fat.png, meat_*.png). Cooked whale meat is the
shipped meat_cetacean_raw.png with its red flesh re-toned to the mod's cooked-meat browns.

    python scripts/gen_materials.py            # drafts -> scratch/materials/ + review sheet
    python scripts/gen_materials.py --install  # copy approved drafts into assets/
"""
import os
import sys
import shutil
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM = os.path.join(ROOT, 'src/main/resources/assets/untamedwilds/textures/item')
OUT = os.path.join(ROOT, 'scratch/materials')


def hx(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


ART = {
    # mammoth / elephant tusk: cream ivory with a brown root, curving up to a point
    'material/ivory': ({'o': '#6b5a44', 'd': '#b8a888', 'm': '#e2d6bc', 'l': '#f8f2e4', 'b': '#8e7658'}, [
        "................",
        "...........ooo..",
        "..........ollmo.",
        "..........olmmdo",
        "...........olmdo",
        "...........olmdo",
        "..........olmmdo",
        ".........olmmdo.",
        "........olmmddo.",
        ".......olmmmdo..",
        ".....oolmmmddo..",
        "....olmmmmddo...",
        "...obbmmmddo....",
        "...obbbdddo.....",
        "...oobbbbo......",
        ".....oooo.......",
    ]),
    # a lock of woolly mammoth hair: wide at the root, hanging in separate strands
    'material/woolly_fur': ({'o': '#2e1c10', 'd': '#5a3820', 'm': '#7c5232', 'l': '#a8784c'}, [
        "................",
        "....oooooo......",
        "...olllmmmoo....",
        "..olmmlmmmmdo...",
        "..olmmmlmmmdo...",
        "..omlmmmmlmdo...",
        "..ommlmmmmddo...",
        "...omlmmldddo...",
        "...ommmlmddo....",
        "...omdmmddmo....",
        "....odmdmddo....",
        "....omdodmdo....",
        "....od.odo.do...",
        "....o..od..o....",
        ".......o........",
        "................",
    ]),
    # a shed deer antler: beam from the burr with three upturned tines, pale tips
    'material/antler': ({'o': '#4a3a28', 'd': '#7a6248', 'm': '#a68c6a', 'l': '#dccaa8', 'b': '#5c4632'}, [
        "................",
        "..........o.....",
        ".........olo....",
        "....o....olo..o.",
        "...olo...olo.olo",
        "...olo..olmo.olo",
        "....olo.olmoolmo",
        "....olmoolmolmo.",
        ".....olmmmmmmo..",
        "......olmmmdo...",
        ".....olmmddo....",
        "....olmddo......",
        "...olmddo.......",
        "..oddddo........",
        "..obbbo.........",
        "...ooo..........",
    ]),
    # a glyptodont scute: rounded bony plate with the raised rosette stud of the Glyptotherium skin
    'material/scute': ({'o': '#4c392a', 'd': '#7b5a3a', 'm': '#9c7850', 'l': '#d4b286', 'c': '#6a4c30'}, [
        "................",
        "................",
        ".....oooooo.....",
        "....odmmmmdo....",
        "...odmmmmmmdo...",
        "..odmmccccmmdo..",
        "..odmcllllcmdo..",
        "..odmcllllcmdo..",
        "..odmcllllcmdo..",
        "..odmmccccmmdo..",
        "..oddmmmmmmddo..",
        "...oddmmmmddo...",
        "....oddddddo....",
        ".....oooooo.....",
        "................",
        "................",
    ]),
}


def draw(palette, rows):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), hx(palette[ch]))
    return img


def cooked_whale():
    """meat_cetacean_raw.png with the red flesh re-toned to cooked browns; skin band left as is."""
    raw = Image.open(os.path.join(ITEM, 'food/meat_cetacean_raw.png')).convert('RGBA')
    ramp = [hx('#3a1e12'), hx('#5c321e'), hx('#7e4a2c'), hx('#a4683e'), hx('#c48a56')]
    out = raw.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = raw.getpixel((x, y))
            if a and r > g + 30 and r > b + 30:          # flesh (red), not the grey-blue skin
                L = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                t = min(0.999, max(0.0, (L - 0.12) / 0.55)) * (len(ramp) - 1)
                i = int(t)
                c = [ramp[i][k] + (ramp[min(i + 1, 4)][k] - ramp[i][k]) * (t - i) for k in range(3)]
                out.putpixel((x, y), tuple(int(v) for v in c) + (a,))
    return out


def build():
    os.makedirs(os.path.join(OUT, 'material'), exist_ok=True)
    os.makedirs(os.path.join(OUT, 'food'), exist_ok=True)
    made = {k: draw(*v) for k, v in ART.items()}
    made['food/meat_cetacean_cooked'] = cooked_whale()
    for k, img in made.items():
        img.save(os.path.join(OUT, k + '.png'))
    # review sheet: new drafts, then the three reused shipped textures, all at x10
    reuse = ['material/horn', 'material/shark_tooth', 'food/meat_cetacean_raw']
    names = list(made) + reuse
    S = 10
    sheet = Image.new('RGBA', (len(names) * (16 * S + 14) + 14, 16 * S + 46), (236, 232, 224, 255))
    d = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        img = made[n] if n in made else Image.open(os.path.join(ITEM, n + '.png')).convert('RGBA')
        x = 14 + i * (16 * S + 14)
        sheet.alpha_composite(img.resize((16 * S, 16 * S), Image.NEAREST), (x, 10))
        d.text((x, 16 * S + 16), n.split('/')[1] + ('  (NEW)' if n in made else '  (existing)'), fill=(30, 30, 30, 255))
    sheet.save(os.path.join(ROOT, 'scratch/materials_sheet.png'))
    print('drafted', len(made), '-> scratch/materials_sheet.png')


def install():
    for root, _, files in os.walk(OUT):
        for f in files:
            rel = os.path.relpath(os.path.join(root, f), OUT)
            dst = os.path.join(ITEM, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy(os.path.join(root, f), dst)
            print('installed', rel)


if __name__ == '__main__':
    install() if '--install' in sys.argv else build()
