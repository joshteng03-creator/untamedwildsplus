#!/usr/bin/env python3
"""Zoo-side art drafts (2026-10-08): Field Guide icon, Feeding Trough (3 fill states), Species Placard,
Salt Lick (4 wear sizes). Each block is ONE element list that writes both the shipped block-model JSON and
the scratch resource pack Blockbench previews, so the approved preview is exactly what ships.

    python scripts/gen_zoo_art.py <vanilla_textures_dir>            # scratch pack + sheet
    python scripts/gen_zoo_art.py <vanilla_textures_dir> --install  # models, blockstates, textures into assets/
"""
import json
import os
import random
import shutil
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'src/main/resources/assets')
PACK = os.path.join(ROOT, 'scratch/zoo_pack/assets')
NS = 'untamedwilds:'


def hx(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


# ------------------------------------------------------------------ textures
def field_guide(V):
    """Vanilla book recoloured to moss-green leather, a tan pawprint on the cover, a dark elastic strap."""
    book = Image.open(os.path.join(V, 'item/book.png')).convert('RGBA')
    out = book.copy()
    ramp = [hx('#1f3324'), hx('#2f4d34'), hx('#3f6644'), hx('#5a8a5c')]
    for y in range(16):
        for x in range(16):
            r, g, b, a = book.getpixel((x, y))
            if a and r > g + 25 and r > b + 25:                # the red-brown cover leather
                L = lum((r, g, b))
                out.putpixel((x, y), ramp[min(3, int(L / 64))] + (255,))
    paw = {(6, 6): '#d8c08e', (8, 6): '#d8c08e', (5, 7): '#d8c08e', (9, 7): '#d8c08e', (6, 8): '#c9ad78', (7, 8): '#d8c08e', (8, 8): '#c9ad78', (7, 9): '#c9ad78'}
    for (x, y), c in paw.items():
        if out.getpixel((x, y))[3]:
            out.putpixel((x, y), hx(c) + (255,))
    for y in range(16):
        x = 11
        if out.getpixel((x, y))[3] and lum(out.getpixel((x, y))[:3]) < 140:
            out.putpixel((x, y), hx('#141c16') + (255,))
    return out


def trough_meat():
    rnd = random.Random(3)
    img = Image.new('RGBA', (16, 16), hx('#8e2f2a') + (255,))
    for _ in range(9):
        cx, cy = rnd.randrange(1, 15), rnd.randrange(1, 15)
        for dx in range(-2, 3):
            for dy in range(-1, 2):
                x, y = cx + dx, cy + dy
                if 0 <= x < 16 and 0 <= y < 16 and abs(dx) + abs(dy) < 3:
                    img.putpixel((x, y), hx(rnd.choice(['#c4504a', '#b2443e', '#d8706a'])) + (255,))
        if 0 <= cx < 16 and 0 <= cy < 16:
            img.putpixel((cx, cy), hx('#efd6c8') + (255,))          # fat marbling
    for y in range(16):
        for x in range(16):
            if rnd.random() < 0.08:
                img.putpixel((x, y), hx('#6e221e') + (255,))
    return img


def placard_face():
    rnd = random.Random(5)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            v = 236 + rnd.randint(-6, 4)
            img.putpixel((x, y), (v, v - 6, v - 22, 255))          # warm parchment
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(182, 160, 118, 255))     # printed border line
    return img


def salt_lick():
    rnd = random.Random(9)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            base = rnd.choice(['#efe2dc', '#e8d4cc', '#f4ece6', '#e2c6bc', '#ead8d0'])
            img.putpixel((x, y), hx(base) + (255,))
    for _ in range(6):
        x, y = rnd.randrange(16), rnd.randrange(16)
        img.putpixel((x, y), hx('#d4a89c') + (255,))               # pink mineral flecks
    return img


# ------------------------------------------------------------------ models
def box(name, frm, to, tex, uvs=None, faces=('north', 'south', 'east', 'west', 'up', 'down')):
    e = {"name": name, "from": frm, "to": to, "faces": {}}
    for f in faces:
        sx, sy, sz = (to[i] - frm[i] for i in range(3))
        x0, y0, z0 = frm
        auto = {'north': [16 - to[0], 16 - to[1], 16 - frm[0], 16 - frm[1]], 'south': [x0, 16 - to[1], to[0], 16 - y0],
                'east': [16 - to[2], 16 - to[1], 16 - z0, 16 - y0], 'west': [z0, 16 - to[1], to[2], 16 - y0],
                'up': [x0, z0, to[0], to[2]], 'down': [x0, 16 - to[2], to[0], 16 - z0]}[f]
        e["faces"][f] = {"uv": (uvs or {}).get(f, auto), "texture": tex}
    return e


def trough(fill):
    els = [
        box('foot_w', [1, 0, 4], [3, 1, 12], '#wood'), box('foot_e', [13, 0, 4], [15, 1, 12], '#wood'),
        box('floor', [1, 1, 3], [15, 2, 13], '#wood'),
        box('wall_n', [1, 2, 3], [15, 8, 4], '#wood'), box('wall_s', [1, 2, 12], [15, 8, 13], '#wood'),
        box('end_w', [1, 2, 4], [2, 8, 12], '#wood'), box('end_e', [14, 2, 4], [15, 8, 12], '#wood'),
        box('rim_n', [0.5, 8, 2.5], [15.5, 9, 4], '#log'), box('rim_s', [0.5, 8, 12], [15.5, 9, 13.5], '#log'),
    ]
    tex = {"particle": "minecraft:block/spruce_planks", "wood": "minecraft:block/spruce_planks", "log": "minecraft:block/stripped_spruce_log"}
    if fill != 'empty':
        els.append(box('fill', [2, 2, 4], [14, 6.5, 12], '#fill', faces=('up', 'north', 'south', 'east', 'west')))
        tex["fill"] = "minecraft:block/hay_block_top" if fill == 'plant' else NS + "block/trough_meat"
    return {"parent": "block/block", "textures": tex, "elements": els}


def placard():
    return {"parent": "block/block",
            "textures": {"particle": "minecraft:block/dark_oak_planks", "frame": "minecraft:block/dark_oak_planks", "face": NS + "block/placard_face"},
            "elements": [box('frame', [1, 3, 0], [15, 13, 1], '#frame'),
                         box('face', [2, 4, 1], [14, 12, 1.5], '#face', uvs={'south': [0, 0, 16, 16]})]}


SALT = [[2, 0, 2, 14, 10, 14], [3, 0, 3, 13, 8, 13], [4, 0, 4, 12, 6, 12], [5, 0, 5, 11, 4, 11]]


def salt(wear):
    x0, y0, z0, x1, y1, z1 = SALT[wear]
    els = [box('lick', [x0, y0, z0], [x1, y1, z1], '#salt')]
    if wear < 3:
        els.append(box('crown', [x0 + 1, y1, z0 + 1], [x1 - 1, y1 + 1, z1 - 1], '#salt'))
    return {"parent": "block/block", "textures": {"particle": NS + "block/salt_lick", "salt": NS + "block/salt_lick"}, "elements": els}


def write(base, vanilla, install):
    tdir = os.path.join(base, 'untamedwilds/textures')
    mdir = os.path.join(base, 'untamedwilds/models/block')
    for d in (os.path.join(tdir, 'item'), os.path.join(tdir, 'block'), mdir):
        os.makedirs(d, exist_ok=True)
    field_guide(vanilla).save(os.path.join(tdir, 'item/field_guide.png'))
    trough_meat().save(os.path.join(tdir, 'block/trough_meat.png'))
    placard_face().save(os.path.join(tdir, 'block/placard_face.png'))
    salt_lick().save(os.path.join(tdir, 'block/salt_lick.png'))
    models = {'feeding_trough_' + f: trough(f) for f in ('empty', 'plant', 'meat')}
    models['species_placard'] = placard()
    models.update({'salt_lick_%d' % w: salt(w) for w in range(4)})
    for name, m in models.items():
        with open(os.path.join(mdir, name + '.json'), 'w', encoding='utf8', newline='\n') as f:
            json.dump(m, f, indent=2)
            f.write('\n')
    if install:
        bs = os.path.join(base, 'untamedwilds/blockstates')
        rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
        variants = {}
        for fill in ('empty', 'plant', 'meat'):
            for facing, y in rot.items():
                v = {"model": NS + "block/feeding_trough_" + fill}
                if y:
                    v["y"] = y
                variants["facing=%s,fill=%s" % (facing, fill)] = v
        json.dump({"variants": variants}, open(os.path.join(bs, 'feeding_trough.json'), 'w'), indent=2)
        json.dump({"variants": {"facing=south": {"model": NS + "block/species_placard"},
                                "facing=west": {"model": NS + "block/species_placard", "y": 90},
                                "facing=north": {"model": NS + "block/species_placard", "y": 180},
                                "facing=east": {"model": NS + "block/species_placard", "y": 270}}},
                  open(os.path.join(bs, 'species_placard.json'), 'w'), indent=2)
        json.dump({"variants": {"wear=%d" % w: {"model": NS + "block/salt_lick_%d" % w} for w in range(4)}},
                  open(os.path.join(bs, 'salt_lick.json'), 'w'), indent=2)
        im = os.path.join(base, 'untamedwilds/models/item')
        for item, parent in (('feeding_trough', 'feeding_trough_plant'), ('species_placard', 'species_placard'), ('salt_lick', 'salt_lick_0')):
            json.dump({"parent": NS + "block/" + parent}, open(os.path.join(im, item + '.json'), 'w'), indent=2)
    else:
        vb = os.path.join(base, 'minecraft/textures/block')
        os.makedirs(vb, exist_ok=True)
        for t in ('spruce_planks', 'stripped_spruce_log', 'hay_block_top', 'dark_oak_planks'):
            shutil.copy(os.path.join(vanilla, 'block', t + '.png'), vb)


if __name__ == '__main__':
    install = '--install' in sys.argv
    write(ASSETS if install else PACK, sys.argv[1], install)
    print('installed' if install else 'scratch pack written to ' + PACK)
