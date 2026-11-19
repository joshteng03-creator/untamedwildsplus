"""Register the 17 per-species hides + rugs (2026-10-06) and rewrite the bear/big_cat hide loot.

Follows the existing hide/rug pattern exactly: an Item per hide (ModItems), a CarpetBlock per rug
(ModBlock, whose createBlock also registers its BlockItem), item/block models, blockstate, a
self-dropping block loot table, a 2 hides -> 6 rugs recipe, forge:hide + forge:leather tags and
en_us names. Every hide (old and new) also gets a 1 hide -> 1 leather recipe.

Idempotent: re-running skips anything already present.
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
J = os.path.join(ROOT, 'src/main/java/untamedwilds/init')
A = os.path.join(ROOT, 'src/main/resources/assets/untamedwilds')
D = os.path.join(ROOT, 'src/main/resources/data')

# id suffix -> (display name, rug MaterialColor)
NEW = {
    'bear_cave': ('Cave Bear', 'COLOR_BROWN'),
    'bear_panda': ('Giant Panda', 'SNOW'),
    'bear_spectacled': ('Spectacled Bear', 'COLOR_BLACK'),
    'bear_sun': ('Sun Bear', 'COLOR_BLACK'),
    'bear_short_faced': ('Short-Faced Bear', 'TERRACOTTA_ORANGE'),
    'bear_arctotherium': ('Arctotherium', 'COLOR_BROWN'),
    'bigcat_cave_lion': ('Cave Lion', 'SAND'),
    'bigcat_american_lion': ('American Lion', 'COLOR_ORANGE'),
    'bigcat_barbary_lion': ('Barbary Lion', 'SAND'),
    'bigcat_homotherium': ('Scimitar Cat', 'SAND'),
    'bigcat_marsupial_lion': ('Marsupial Lion', 'COLOR_ORANGE'),
    'bigcat_sabertooth': ('Sabertooth', 'COLOR_GRAY'),
    'bigcat_cheetah': ('Cheetah', 'GOLD'),
    'bigcat_american_cheetah': ('American Cheetah', 'COLOR_LIGHT_GRAY'),
    'bigcat_caspian_tiger': ('Caspian Tiger', 'COLOR_ORANGE'),
    'bigcat_thylacosmilus': ('Thylacosmilus', 'COLOR_GRAY'),
    'bigcat_smilodon_fatalis': ('North American Sabertooth', 'TERRACOTTA_BROWN'),
}
OLD = ['bear_ashen', 'bear_black', 'bear_brown', 'bear_white', 'bigcat_jaguar', 'bigcat_leopard', 'bigcat_lion',
       'bigcat_panther', 'bigcat_puma', 'bigcat_snow_leopard', 'bigcat_tiger']


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path):
        return False
    with open(path, 'w', encoding='utf8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')
    return True


def edit_java(path, anchor_regex, lines):
    raw = open(path, encoding='utf8').read()
    nl = '\r\n' if '\r\n' in raw else '\n'
    s = raw.replace('\r\n', '\n')
    lines = [l for l in lines if l.split('"')[1] not in s]
    if not lines:
        return 0
    m = None
    for m in re.finditer(anchor_regex, s):
        pass
    assert m, anchor_regex
    end = s.index('\n', m.end()) + 1
    s = s[:end] + ''.join(l + '\n' for l in lines) + s[end:]
    open(path, 'w', encoding='utf8', newline='').write(s.replace('\n', nl))
    return len(lines)


def const(suffix):
    return suffix.upper()


def main():
    n_items = edit_java(os.path.join(J, 'ModItems.java'), r'public static RegistryObject<Item> HIDE_BIGCAT_TIGER = [^\n]*',
        ['    public static RegistryObject<Item> HIDE_%s = createItem("hide_%s", () -> new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items)));' % (const(k), k) for k in NEW])
    n_blocks = edit_java(os.path.join(J, 'ModBlock.java'), r'public static RegistryObject<Block> CARPET_BIGCAT_TIGER\s+= [^\n]*',
        ['    public static RegistryObject<Block> CARPET_%s  = createBlock("carpet_%s", () -> new CarpetBlock(Block.Properties.of(Material.CLOTH_DECORATION, MaterialColor.%s).destroyTime(0.1F).sound( SoundType.WOOL)), CreativeModeTab.TAB_DECORATIONS);' % (const(k), k, col) for k, (_, col) in NEW.items()])
    wrote = 0
    for k in NEW:
        tex_item = 'untamedwilds:item/hide/' + k.replace('bigcat_', 'big_cat_')
        tex_rug = 'untamedwilds:block/carpet/' + k
        wrote += write_json(os.path.join(A, 'models/item/hide_%s.json' % k), {"parent": "item/generated", "textures": {"layer0": tex_item}})
        wrote += write_json(os.path.join(A, 'models/item/carpet_%s.json' % k), {"parent": "untamedwilds:block/carpet_%s" % k})
        block_model = json.load(open(os.path.join(A, 'models/block/carpet_bear_black.json'), encoding='utf8'))
        block_model['textures'] = {"all": tex_rug, "particle": tex_rug}
        wrote += write_json(os.path.join(A, 'models/block/carpet_%s.json' % k), block_model)
        wrote += write_json(os.path.join(A, 'blockstates/carpet_%s.json' % k), {"variants": {"": {"model": "untamedwilds:block/carpet_%s" % k}}})
        wrote += write_json(os.path.join(D, 'untamedwilds/loot_tables/blocks/carpet_%s.json' % k),
                            {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "untamedwilds:carpet_%s" % k}],
                                                                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        wrote += write_json(os.path.join(D, 'untamedwilds/recipes/blocks/carpet_%s.json' % k),
                            {"type": "minecraft:crafting_shaped", "group": "carpet", "pattern": ["##"],
                             "key": {"#": {"item": "untamedwilds:hide_%s" % k}}, "result": {"item": "untamedwilds:carpet_%s" % k, "count": 6}})
    for k in list(NEW) + OLD:
        wrote += write_json(os.path.join(D, 'untamedwilds/recipes/items/leather_from_hide_%s.json' % k),
                            {"type": "minecraft:crafting_shapeless", "group": "leather",
                             "ingredients": [{"item": "untamedwilds:hide_%s" % k}], "result": {"item": "minecraft:leather"}})
    for tag in ('hide', 'leather'):
        p = os.path.join(D, 'forge/tags/items/%s.json' % tag)
        d = json.load(open(p, encoding='utf8'))
        for k in NEW:
            if 'untamedwilds:hide_' + k not in d['values']:
                d['values'].append('untamedwilds:hide_' + k)
        with open(p, 'w', encoding='utf8', newline='\n') as f:
            json.dump(d, f, indent=2)
            f.write('\n')
    p = os.path.join(A, 'lang/en_us.json')
    raw = open(p, encoding='utf8').read()
    add = ''
    for k, (name, _) in NEW.items():
        if '"item.untamedwilds.hide_%s"' % k not in raw:
            add += '  "item.untamedwilds.hide_%s": "%s Hide",\n' % (k, name)
            add += '  "block.untamedwilds.carpet_%s": "%s Rug",\n' % (k, name)
    if add:
        anchor = re.search(r'  "item\.untamedwilds\.hide_bigcat_tiger": "[^"]*",\n', raw)
        raw = raw[:anchor.end()] + add + raw[anchor.end():]
        json.loads(raw)
        open(p, 'w', encoding='utf8', newline='').write(raw)
    print('items +%d, blocks +%d, json files +%d, lang +%d keys' % (n_items, n_blocks, wrote, add.count('\n')))


if __name__ == '__main__':
    main()
