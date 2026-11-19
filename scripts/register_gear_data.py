"""Data + assets for the crafted gear (2026-10-06): fur armour, carapace armour, serrated arrow,
herding horn, and the ivory block set. Item/block models, blockstates, block loot, crafting +
stonecutting recipes, vanilla/forge tags and en_us names. Idempotent.
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, 'src/main/resources/assets/untamedwilds')
D = os.path.join(ROOT, 'src/main/resources/data')
NS = 'untamedwilds:'
n_files = 0


def w(path, obj):
    global n_files
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if not os.path.exists(path):
        with open(path, 'w', encoding='utf8', newline='\n') as f:
            json.dump(obj, f, indent=2)
            f.write('\n')
        n_files += 1


def item_model(name, texture=None, parent=None):
    w(os.path.join(A, 'models/item/%s.json' % name),
      {"parent": parent} if parent else {"parent": "item/generated", "textures": {"layer0": NS + 'item/' + (texture or name)}})


def block_model(name, parent, textures):
    w(os.path.join(A, 'models/block/%s.json' % name), {"parent": parent, "textures": textures})


def blockstate(name, obj):
    w(os.path.join(A, 'blockstates/%s.json' % name), obj)


def self_loot(block, slab=False):
    entry = {"type": "minecraft:item", "name": NS + block}
    if slab:
        entry["functions"] = [{"function": "minecraft:set_count", "add": False, "count": 2,
                               "conditions": [{"condition": "minecraft:block_state_property", "block": NS + block,
                                               "properties": {"type": "double"}}]},
                              {"function": "minecraft:explosion_decay"}]
    w(os.path.join(D, 'untamedwilds/loot_tables/blocks/%s.json' % block),
      {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def shaped(name, pattern, key, result, count=1, group=None):
    obj = {"type": "minecraft:crafting_shaped", "pattern": pattern,
           "key": {k: {"item": v} for k, v in key.items()}, "result": {"item": result, "count": count}}
    if group:
        obj["group"] = group
    w(os.path.join(D, 'untamedwilds/recipes/items/%s.json' % name), obj)


def stonecut(name, ingredient, result, count=1):
    w(os.path.join(D, 'untamedwilds/recipes/items/%s.json' % name),
      {"type": "minecraft:stonecutting", "ingredient": {"item": ingredient}, "result": result, "count": count})


def tag(path, values):
    p = os.path.join(D, path)
    d = json.load(open(p, encoding='utf8')) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d['values']:
            d['values'].append(v)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf8', newline='\n') as f:
        json.dump(d, f, indent=2)
        f.write('\n')


def pillar(name, end, side):
    block_model(name, 'minecraft:block/cube_column', {"end": NS + 'block/' + end, "side": NS + 'block/' + side})
    block_model(name + '_horizontal', 'minecraft:block/cube_column_horizontal', {"end": NS + 'block/' + end, "side": NS + 'block/' + side})
    blockstate(name, {"variants": {
        "axis=y": {"model": NS + 'block/' + name},
        "axis=z": {"model": NS + 'block/' + name + '_horizontal', "x": 90},
        "axis=x": {"model": NS + 'block/' + name + '_horizontal', "x": 90, "y": 90}}})
    item_model(name, parent=NS + 'block/' + name)
    self_loot(name)


def stairs_state(name):
    m = {s: NS + 'block/' + name + suf for s, suf in (('straight', ''), ('inner', '_inner'), ('outer', '_outer'))}
    variants = {}
    facing_y = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    for facing, fy in facing_y.items():
        for half in ('bottom', 'top'):
            for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                kind = 'straight' if shape == 'straight' else shape.split('_')[0]
                y = fy
                if half == 'bottom' and shape.endswith('left'):
                    y = (fy + 270) % 360
                if half == 'top' and shape.endswith('right'):
                    y = (fy + 90) % 360
                v = {"model": m[kind]}
                if half == 'top':
                    v["x"] = 180
                if y:
                    v["y"] = y
                if half == 'top' or y:
                    v["uvlock"] = True
                variants['facing=%s,half=%s,shape=%s' % (facing, half, shape)] = v
    return {"variants": variants}


def wall_state(name):
    post, side, tall = (NS + 'block/' + name + s for s in ('_post', '_side', '_side_tall'))
    parts = [{"when": {"up": "true"}, "apply": {"model": post}}]
    for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for h, model in (('low', side), ('tall', tall)):
            a = {"model": model, "uvlock": True}
            if y:
                a["y"] = y
            parts.append({"when": {d: h}, "apply": a})
    return {"multipart": parts}


def main():
    # ---------------- armour + arrow + horn items ----------------
    for pre in ('fur', 'carapace'):
        for piece in ('helmet', 'chestplate', 'leggings', 'boots'):
            item_model('%s_%s' % (pre, piece))
    item_model('serrated_arrow')
    item_model('herding_horn')
    for mat, item in (('fur', 'material_woolly_fur'), ('carapace', 'material_scute')):
        shaped('%s_helmet' % mat, ["###", "# #"], {"#": NS + item}, NS + mat + '_helmet')
        shaped('%s_chestplate' % mat, ["# #", "###", "###"], {"#": NS + item}, NS + mat + '_chestplate')
        shaped('%s_leggings' % mat, ["###", "# #", "# #"], {"#": NS + item}, NS + mat + '_leggings')
        shaped('%s_boots' % mat, ["# #", "# #"], {"#": NS + item}, NS + mat + '_boots')
    shaped('serrated_arrow', ["X", "#", "Y"], {"X": NS + 'material_shark_tooth', "#": 'minecraft:stick', "Y": 'minecraft:feather'}, NS + 'serrated_arrow', 4)
    shaped('herding_horn', ["CHC"], {"C": 'minecraft:copper_ingot', "H": NS + 'material_horn'}, NS + 'herding_horn')
    tag('minecraft/tags/items/freeze_immune_wearables.json', [NS + 'fur_' + p for p in ('helmet', 'chestplate', 'leggings', 'boots')])
    tag('minecraft/tags/items/arrows.json', [NS + 'serrated_arrow'])
    # ---------------- ivory set ----------------
    pillar('ivory_block', 'ivory_block_top', 'ivory_block_side')
    pillar('chiseled_ivory', 'chiseled_ivory_top', 'chiseled_ivory')
    tex = {"bottom": NS + 'block/ivory_block_top', "top": NS + 'block/ivory_block_top', "side": NS + 'block/ivory_block_side'}
    block_model('ivory_slab', 'minecraft:block/slab', tex)
    block_model('ivory_slab_top', 'minecraft:block/slab_top', tex)
    blockstate('ivory_slab', {"variants": {"type=bottom": {"model": NS + 'block/ivory_slab'}, "type=top": {"model": NS + 'block/ivory_slab_top'},
                                           "type=double": {"model": NS + 'block/ivory_block'}}})
    item_model('ivory_slab', parent=NS + 'block/ivory_slab')
    self_loot('ivory_slab', slab=True)
    for suf, parent in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
        block_model('ivory_stairs' + suf, 'minecraft:block/' + parent, tex)
    blockstate('ivory_stairs', stairs_state('ivory_stairs'))
    item_model('ivory_stairs', parent=NS + 'block/ivory_stairs')
    self_loot('ivory_stairs')
    wall_tex = {"wall": NS + 'block/ivory_block_side'}
    for suf, parent in (('_post', 'template_wall_post'), ('_side', 'template_wall_side'), ('_side_tall', 'template_wall_side_tall'), ('_inventory', 'wall_inventory')):
        block_model('ivory_wall' + suf, 'minecraft:block/' + parent, wall_tex)
    blockstate('ivory_wall', wall_state('ivory_wall'))
    item_model('ivory_wall', parent=NS + 'block/ivory_wall_inventory')
    self_loot('ivory_wall')
    shaped('ivory_block', ["##", "##"], {"#": NS + 'material_ivory'}, NS + 'ivory_block')
    shaped('ivory_slab', ["###"], {"#": NS + 'ivory_block'}, NS + 'ivory_slab', 6)
    shaped('ivory_stairs', ["#  ", "## ", "###"], {"#": NS + 'ivory_block'}, NS + 'ivory_stairs', 4)
    shaped('ivory_wall', ["###", "###"], {"#": NS + 'ivory_block'}, NS + 'ivory_wall', 6)
    shaped('chiseled_ivory', ["#", "#"], {"#": NS + 'ivory_slab'}, NS + 'chiseled_ivory')
    stonecut('ivory_slab_from_stonecutting', NS + 'ivory_block', NS + 'ivory_slab', 2)
    stonecut('ivory_stairs_from_stonecutting', NS + 'ivory_block', NS + 'ivory_stairs')
    stonecut('ivory_wall_from_stonecutting', NS + 'ivory_block', NS + 'ivory_wall')
    stonecut('chiseled_ivory_from_stonecutting', NS + 'ivory_block', NS + 'chiseled_ivory')
    ivory = [NS + b for b in ('ivory_block', 'ivory_slab', 'ivory_stairs', 'ivory_wall', 'chiseled_ivory')]
    tag('minecraft/tags/blocks/mineable/pickaxe.json', ivory)
    tag('minecraft/tags/blocks/walls.json', [NS + 'ivory_wall'])
    tag('minecraft/tags/blocks/stairs.json', [NS + 'ivory_stairs'])
    tag('minecraft/tags/blocks/slabs.json', [NS + 'ivory_slab'])
    tag('minecraft/tags/items/walls.json', [NS + 'ivory_wall'])
    tag('minecraft/tags/items/stairs.json', [NS + 'ivory_stairs'])
    tag('minecraft/tags/items/slabs.json', [NS + 'ivory_slab'])
    # ---------------- names ----------------
    names = {}
    for pre, label in (('fur', 'Fur'), ('carapace', 'Carapace')):
        for piece, pl in (('helmet', 'Helmet' if pre == 'carapace' else 'Hood'), ('chestplate', 'Chestplate' if pre == 'carapace' else 'Coat'),
                          ('leggings', 'Leggings'), ('boots', 'Boots')):
            names['item.untamedwilds.%s_%s' % (pre, piece)] = '%s %s' % (label, pl)
    names.update({
        'item.untamedwilds.serrated_arrow': 'Serrated Arrow',
        'item.untamedwilds.herding_horn': 'Herding Horn',
        'item.untamedwilds.herding_horn.follow': '%s animals are following you',
        'item.untamedwilds.herding_horn.sit': '%s animals sit and stay',
        'entity.untamedwilds.serrated_arrow': 'Serrated Arrow',
        'block.untamedwilds.ivory_block': 'Block of Ivory',
        'block.untamedwilds.ivory_slab': 'Ivory Slab',
        'block.untamedwilds.ivory_stairs': 'Ivory Stairs',
        'block.untamedwilds.ivory_wall': 'Ivory Wall',
        'block.untamedwilds.chiseled_ivory': 'Chiseled Ivory',
    })
    p = os.path.join(A, 'lang/en_us.json')
    raw = open(p, encoding='utf8').read()
    add = ''.join('  "%s": "%s",\n' % (k, v) for k, v in names.items() if '"%s"' % k not in raw)
    if add:
        m = re.search(r'  "item\.untamedwilds\.material_snake_skin": "[^"]*",\n', raw)
        raw = raw[:m.end()] + add + raw[m.end():]
        json.loads(raw)
        open(p, 'w', encoding='utf8', newline='').write(raw)
    print('json files +%d, en_us +%d' % (n_files, add.count('\n')))


if __name__ == '__main__':
    main()
