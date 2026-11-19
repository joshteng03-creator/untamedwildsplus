"""Register the antler decor blocks (2026-10-06): antler chandelier + antler wall trophy. ModBlock entries,
blockstates, item models, self loot, recipes, axe tag and en_us. Models + texture come from
`scripts/gen_antler_decor.py --install`. Idempotent.

    python scripts/register_antler_decor.py
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import register_gear_data as gd
from register_gear_data import A, D, NS, w, blockstate, self_loot, tag
from register_batch3 import edit, add_imports, after

PROPS = 'Block.Properties.of(Material.WOOD, MaterialColor.SAND).strength(1.0F).sound(SoundType.BONE_BLOCK).noOcclusion()'


def blocks(s):
    if '"antler_chandelier"' in s:
        return s
    s = add_imports(s, ['untamedwilds.block.AntlerChandelierBlock', 'untamedwilds.block.AntlerTrophyBlock'])
    lines = ('    public static RegistryObject<Block> ANTLER_CHANDELIER = createBlock("antler_chandelier", () -> new AntlerChandelierBlock(%s.lightLevel(state -> 15)), "cutout", CreativeModeTab.TAB_DECORATIONS);\n'
             '    public static RegistryObject<Block> ANTLER_TROPHY = createBlock("antler_trophy", () -> new AntlerTrophyBlock(%s), "cutout", CreativeModeTab.TAB_DECORATIONS);\n') % (PROPS, PROPS)
    return after(s, r'    public static RegistryObject<Block> DISPLAY_PLINTH = [^\n]*\n', lines)


def data():
    blockstate('antler_chandelier', {"variants": {"": {"model": NS + "block/antler_chandelier"}}})
    blockstate('antler_trophy', {"variants": {
        "facing=south": {"model": NS + "block/antler_trophy"},
        "facing=west": {"model": NS + "block/antler_trophy", "y": 90},
        "facing=north": {"model": NS + "block/antler_trophy", "y": 180},
        "facing=east": {"model": NS + "block/antler_trophy", "y": 270}}})
    # the chandelier hangs in the top half of its block: drop it into frame for inventory/hand/ground
    w(os.path.join(A, 'models/item/antler_chandelier.json'), {"parent": NS + "block/antler_chandelier", "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -3, 0], "scale": [0.7, 0.7, 0.7]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, -2, 0], "scale": [0.5, 0.5, 0.5]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 0, 2.5], "scale": [0.375, 0.375, 0.375]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -2, 0], "scale": [0.4, 0.4, 0.4]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, -2, 0], "scale": [0.4, 0.4, 0.4]}}})
    w(os.path.join(A, 'models/item/antler_trophy.json'), {"parent": NS + "block/antler_trophy"})
    for b in ('antler_chandelier', 'antler_trophy'):
        self_loot(b)
    w(os.path.join(D, 'untamedwilds/recipes/items/antler_chandelier.json'), {
        "type": "minecraft:crafting_shaped", "pattern": ["ATA", "ATA"],
        "key": {"A": {"item": NS + "material_antler"}, "T": {"item": "minecraft:torch"}}, "result": {"item": NS + "antler_chandelier"}})
    w(os.path.join(D, 'untamedwilds/recipes/items/antler_trophy.json'), {
        "type": "minecraft:crafting_shaped", "pattern": ["A A", " P "],
        "key": {"A": {"item": NS + "material_antler"}, "P": {"tag": "minecraft:planks"}}, "result": {"item": NS + "antler_trophy"}})
    tag('minecraft/tags/blocks/mineable/axe.json', [NS + 'antler_chandelier', NS + 'antler_trophy'])
    names = {'block.untamedwilds.antler_chandelier': 'Antler Chandelier', 'block.untamedwilds.antler_trophy': 'Antler Trophy'}
    p = os.path.join(A, 'lang/en_us.json')
    raw = open(p, encoding='utf8').read()
    add = ''.join('  %s: %s,\n' % (json.dumps(k), json.dumps(v)) for k, v in names.items() if json.dumps(k) not in raw)
    if add:
        m = re.search(r'  "item\.untamedwilds\.material_snake_skin": "[^"]*",\n', raw)
        raw = raw[:m.end()] + add + raw[m.end():]
        json.loads(raw)
        open(p, 'w', encoding='utf8', newline='').write(raw)
    print('json files +%d, en_us +%d' % (gd.n_files, add.count('\n')))


if __name__ == '__main__':
    edit('init/ModBlock.java', blocks)
    data()
