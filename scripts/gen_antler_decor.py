"""Antler decor blocks (2026-10-06): the antler chandelier (hanging light) and the antler wall trophy.

One element list per model drives everything: the Minecraft block-model JSON, and the Blockbench preview
(which loads that same JSON), so what is approved in Blockbench is byte-for-byte what ships.

The antler texture reuses the antler item's palette (scripts/gen_materials.py): dark burr at the bottom
rows, mid beam, pale tips at the top -- each element picks its tone by the UV rows it samples.
Everything else is vanilla (chain, candle, dark oak) so the blocks sit in a vanilla build.

    python scripts/gen_antler_decor.py <vanilla_textures_dir>            # scratch pack + Blockbench JS
    python scripts/gen_antler_decor.py <vanilla_textures_dir> --install  # models + texture into assets/
"""
import json
import os
import random
import shutil
import sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'src/main/resources/assets')
PACK = os.path.join(ROOT, 'scratch/decor_pack/assets')
NS = 'untamedwilds:'

# The antler texture (16x16). Columns 0-7: one continuous bone gradient, ivory tips at row 0 down to the
# knobbly burr at rows 13-15, with faint lengthwise grooves. An element samples from its START ROW for its
# own face height, so an upright tine runs pale-at-top to darker-at-base by itself and neighbouring
# segments step only a few shades. Columns 8-15: bleached skull bone for the trophy (sockets + nostrils).
GRADIENT = ['#f0e8d6', '#ebe1cc', '#e4d8bf', '#dccfb3', '#d4c5a6', '#cbba99', '#c2ae8c', '#b8a280',
            '#ae9775', '#a38b6a', '#977f60', '#8b7356', '#80694e', '#7a6249', '#6e5841', '#644f3a']
TIP, UPPER, MID, BEAM, LOWER, BURR = 0, 2, 5, 7, 9, 13      # start rows
BONE = '#e6dcc6'


def hx(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def antler_texture():
    rnd = random.Random(7)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(8):
            c = hx(GRADIENT[y])
            f = 1.0 + rnd.uniform(-0.035, 0.035)
            if 4 <= y <= 12 and x in (2, 5):
                f -= 0.09                                   # lengthwise grooves of the beam
            if y >= 13:                                     # burr: pearled knobs and pits
                f = rnd.choice([1.25, 1.1, 1.0, 0.85, 0.8])
            img.putpixel((x, y), shade(c, f))
        for x in range(8, 16):
            img.putpixel((x, y), shade(hx(BONE), 1.0 + rnd.uniform(-0.04, 0.03) - 0.012 * max(0, y - 8)))
    # Deer skull, front view (anatomy, not a face): the orbits sit on the SIDES of the skull, so the dark
    # sockets only show on the orbit elements' front-outer and outer faces; the long snout carries just
    # shading, and the nose tip a faint nasal shadow -- no dark "mouth".
    for x in range(8, 11):                                  # cranium front 3x2: pale dome
        img.putpixel((x, 0), hx('#f3ecde'))
        img.putpixel((x, 1), hx('#e9e0cc'))
    img.putpixel((11, 0), hx('#3b2f24'))                    # orbit front: socket over a bone rim
    img.putpixel((11, 1), hx('#d2c6ac'))
    img.putpixel((12, 0), hx('#3b2f24'))                    # orbit outer face: socket + rim
    img.putpixel((13, 0), hx('#cbbfa4'))
    img.putpixel((12, 1), hx('#d8ccb2'))
    img.putpixel((13, 1), hx('#cbbfa4'))
    for y, (l, r) in enumerate((('#ebe3d0', '#e4dac4'), ('#e6ddc8', '#dcd1b9'), ('#ddd2bb', '#d3c7ad'), ('#d4c8ae', '#c9bca1'))):
        img.putpixel((8, 3 + y), hx(l))                     # snout front 2x4: lit left, shaded right, darkening down
        img.putpixel((9, 3 + y), hx(r))
    img.putpixel((10, 3), hx('#cfc3a8'))                    # nose tip front: faint nasal shadow on the right half
    img.putpixel((11, 3), hx('#a8987c'))
    return img


FACE_SHIFT = {'north': 0, 'south': 2, 'east': 1, 'west': 3, 'up': 4, 'down': 5}


def face_uv(frm, to, face, row):
    """A UV rect sized like the face (never squashed), starting at texture row `row` of the gradient."""
    sx, sy, sz = (to[i] - frm[i] for i in range(3))
    w = {'north': sx, 'south': sx, 'east': sz, 'west': sz, 'up': sx, 'down': sx}[face]
    h = {'north': sy, 'south': sy, 'east': sy, 'west': sy, 'up': sz, 'down': sz}[face]
    w, h = max(min(w, 8), 0.5), max(min(h, 16 - row), 0.5)
    x0 = min(FACE_SHIFT[face], 8 - w)
    return [x0, row, x0 + w, row + h]


def el(name, frm, to, tex, rows=None, rot=None, uvs=None, faces=None, shade=True):
    e = {"name": name, "from": frm, "to": to, "faces": {}}
    if rot:
        axis, angle, origin = rot
        e["rotation"] = {"angle": angle, "axis": axis, "origin": origin}
    for f in faces or ('north', 'south', 'east', 'west', 'up', 'down'):
        uv = (uvs or {}).get(f) or face_uv(frm, to, f, BEAM if rows is None else rows)
        e["faces"][f] = {"uv": uv, "texture": tex}
    if not shade:
        e["shade"] = False
    return e


def chandelier():
    els = []
    # chain from the ceiling: vanilla chain, two crossed planes like the lantern's
    els.append(el('chain_a', [6.5, 11, 8], [9.5, 16, 8], '#chain', uvs={'north': [0, 0, 3, 5], 'south': [0, 0, 3, 5]}, faces=('north', 'south'),
                  rot=('y', 45, [8, 8, 8])))
    els.append(el('chain_b', [8, 11, 6.5], [8, 16, 9.5], '#chain', uvs={'east': [3, 0, 6, 5], 'west': [3, 0, 6, 5]}, faces=('east', 'west'),
                  rot=('y', 45, [8, 8, 8])))
    # dark-oak ring plate the antlers and candles hang from
    plate_uv = {f: [4, 4, 12, 12] for f in ('up', 'down')}
    plate_uv.update({f: [4, 7, 12, 8] for f in ('north', 'south', 'east', 'west')})
    els.append(el('plate', [4, 10, 4], [12, 11, 12], '#wood', uvs=plate_uv))
    els.append(el('hub', [6.5, 8.5, 6.5], [9.5, 10, 9.5], '#wood', uvs={f: [6, 6, 9, 7.5] for f in ('north', 'south', 'east', 'west')} | {'up': [6, 6, 9, 9], 'down': [6, 6, 9, 9]}))
    # four vanilla candles on the plate (candle_lit texture, vanilla UVs); the flames are particles in game
    side = {f: [0, 8, 2, 12] for f in ('north', 'south', 'east', 'west')} | {'up': [0, 6, 2, 8]}
    for i, (cx, cz) in enumerate(((5.5, 5.5), (10.5, 5.5), (5.5, 10.5), (10.5, 10.5))):
        els.append(el('candle_%d' % i, [cx - 1, 11, cz - 1], [cx + 1, 15, cz + 1], '#candle', uvs=side,
                      faces=('north', 'south', 'east', 'west', 'up')))
        for k, ang in (('a', 45), ('b', -45)):
            els.append(el('wick_%d%s' % (i, k), [cx - 0.5, 15, cz], [cx + 0.5, 16, cz], '#candle',
                          uvs={'north': [0, 5, 1, 6], 'south': [0, 5, 1, 6]}, faces=('north', 'south'), rot=('y', ang, [cx, 15, cz])))
    # four antlers, bases under the plate edge, beams out and curving up, one tine each.
    # Built for the east arm (+x) and mirrored by swapping axes; rotations stay on one axis per element.
    for arm, (sx, sz) in (('e', (1, 0)), ('w', (-1, 0)), ('s', (0, 1)), ('n', (0, -1))):
        axis = 'z' if sx else 'x'
        sign = (1 if sx > 0 else -1) if sx else (-1 if sz > 0 else 1)   # positive = tilts the outward end up

        def box(r0, r1, y0, y1, half):
            """radial span r0..r1 from the block centre, height y0..y1, half-thickness `half`."""
            if sx:
                a, b = sorted((8 + sx * r0, 8 + sx * r1))
                return [a, y0, 8 - half], [b, y1, 8 + half]
            a, b = sorted((8 + sz * r0, 8 + sz * r1))
            return [8 - half, y0, a], [8 + half, y1, b]

        def origin(r, y):
            return [8 + sx * r, y, 8 + sz * r]

        f, t = box(1.5, 3.5, 8, 9.5, 1.0)
        els.append(el('burr_' + arm, f, t, '#antler', BURR))
        f, t = box(3, 7.75, 8.25, 9.5, 0.75)
        els.append(el('beam_' + arm, f, t, '#antler', BEAM, rot=(axis, 22.5 * sign, origin(3, 8.75))))
        f, t = box(6.75, 8.25, 9.75, 14.5, 0.75)
        els.append(el('tip_' + arm, f, t, '#antler', TIP, rot=(axis, -22.5 * sign, origin(7.5, 9.75))))
        f, t = box(5, 6, 9.25, 12.5, 0.5)
        els.append(el('tine_' + arm, f, t, '#antler', UPPER, rot=(axis, -22.5 * sign, origin(5.5, 9.25))))
    return {"parent": "block/block", "ambientocclusion": False,
            "textures": {"particle": NS + "block/antler", "antler": NS + "block/antler", "wood": "minecraft:block/dark_oak_planks",
                         "chain": "minecraft:block/chain", "candle": "minecraft:block/candle_lit"},
            "elements": els}


def trophy():
    """Facing south: plaque on the north side of the block space (z 0-1), antlers spreading toward the viewer."""
    els = []
    wood_face = {f: [3, 2, 13, 14] for f in ('north', 'south')}
    wood_face.update({'east': [0, 2, 1, 14], 'west': [15, 2, 16, 14], 'up': [3, 0, 13, 1], 'down': [3, 15, 13, 16]})
    els.append(el('plaque', [3, 2, 0], [13, 14, 1], '#wood', uvs=wood_face))
    els.append(el('plaque_face', [4, 3, 1], [12, 13, 1.5], '#wood', uvs={f: [4, 3, 12, 13] for f in ('north', 'south')} |
                  {'east': [4, 3, 4.5, 13], 'west': [11.5, 3, 12, 13], 'up': [4, 3, 12, 3.5], 'down': [4, 12.5, 12, 13]}))
    bone = {'north': [8, 9, 11, 11], 'east': [12, 9, 14, 11], 'west': [12, 9, 14, 11], 'up': [8, 12, 11, 14], 'down': [8, 13, 11, 15]}
    els.append(el('cranium', [6.5, 9.5, 1.5], [9.5, 11.5, 3.5], '#antler', uvs=bone | {'south': [8, 0, 11, 2]}))
    els.append(el('orbit_l', [5.75, 9, 1.75], [6.5, 10.25, 3], '#antler', uvs=bone | {'south': [11, 0, 11.75, 1.25], 'west': [12, 0, 13.25, 1.25]}))
    els.append(el('orbit_r', [9.5, 9, 1.75], [10.25, 10.25, 3], '#antler', uvs=bone | {'south': [11, 0, 11.75, 1.25], 'east': [12, 0, 13.25, 1.25]}))
    els.append(el('snout', [7, 6, 1.5], [9, 9.5, 3], '#antler', uvs=bone | {'south': [8, 3, 10, 6.5]}))
    els.append(el('nose', [7.25, 5, 1.5], [8.75, 6, 2.75], '#antler', uvs=bone | {'south': [10, 3, 11.5, 4]}))
    for side, s in (('l', -1), ('r', 1)):          # +x is the viewer's right when facing the plaque
        def xs(a, b):
            lo, hi = sorted((8 + s * a, 8 + s * b))
            return lo, hi
        a, b = xs(1, 2.5)
        els.append(el('burr_' + side, [a, 10, 2], [b, 11.5, 3.5], '#antler', BURR))
        a, b = xs(1.5, 3)
        els.append(el('beam1_' + side, [a, 11, 2.25], [b, 15, 3.25], '#antler', LOWER, rot=('z', -22.5 * s, [8 + s * 2.25, 11, 2.75])))
        a, b = xs(3.5, 5)
        els.append(el('beam2_' + side, [a, 13.5, 2.25], [b, 17, 3.25], '#antler', MID, rot=('z', -45 * s, [8 + s * 4.25, 13.5, 2.75])))
        a, b = xs(6, 7)
        els.append(el('tip_' + side, [a, 15.5, 2.5], [b, 19, 3.25], '#antler', TIP, rot=('z', -22.5 * s, [8 + s * 6.5, 15.5, 2.75])))
        a, b = xs(2, 3)
        els.append(el('brow_' + side, [a, 11.5, 3], [b, 12.5, 6], '#antler', UPPER, rot=('x', -22.5, [8 + s * 2.5, 12, 3])))
        a, b = xs(4.5, 5.5)
        els.append(el('tine_' + side, [a, 15.5, 2.5], [b, 18.5, 3.25], '#antler', 1, rot=('z', 22.5 * s, [8 + s * 5, 15.5, 2.75])))
    return {"parent": "block/block",
            "textures": {"particle": "minecraft:block/dark_oak_planks", "antler": NS + "block/antler", "wood": "minecraft:block/dark_oak_planks"},
            "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, -1, 0], "scale": [0.625, 0.625, 0.625]}},
            "elements": els}


def write(base, vanilla):
    models = os.path.join(base, 'untamedwilds/models/block')
    os.makedirs(models, exist_ok=True)
    os.makedirs(os.path.join(base, 'untamedwilds/textures/block'), exist_ok=True)
    antler_texture().save(os.path.join(base, 'untamedwilds/textures/block/antler.png'))
    for name, model in (('antler_chandelier', chandelier()), ('antler_trophy', trophy())):
        with open(os.path.join(models, name + '.json'), 'w', encoding='utf8', newline='\n') as f:
            json.dump(model, f, indent=2)
            f.write('\n')
    if vanilla:
        vb = os.path.join(base, 'minecraft/textures/block')
        os.makedirs(vb, exist_ok=True)
        for t in ('chain', 'candle_lit', 'dark_oak_planks'):
            shutil.copy(os.path.join(vanilla, 'block', t + '.png'), vb)


if __name__ == '__main__':
    if '--install' in sys.argv:
        write(ASSETS, None)
        print('installed models + texture')
    else:
        write(PACK, sys.argv[1])
        print('scratch pack written to', PACK)
