"""Register the approved material drops (2026-10-06): ivory, woolly fur, antler, scute, horn, shark
tooth, whale meat raw/cooked -- items, models, en_us names, fallback recipes, whale cooking, meat tags,
and the loot pools that drop them. Idempotent: anything already present is skipped.
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
J = os.path.join(ROOT, 'src/main/java/untamedwilds/init/ModItems.java')
A = os.path.join(ROOT, 'src/main/resources/assets/untamedwilds')
D = os.path.join(ROOT, 'src/main/resources/data')
LOOT = os.path.join(D, 'untamedwilds/loot_tables/entities')

PLAIN = 'new Item(new Item.Properties().tab(ModCreativeModeTab.untamedwilds_items))'
ITEMS = [  # (CONSTANT, id, constructor, texture, en_us name or None to keep existing)
    ('MATERIAL_IVORY', 'material_ivory', PLAIN, 'material/ivory', 'Ivory'),
    ('MATERIAL_WOOLLY_FUR', 'material_woolly_fur', PLAIN, 'material/woolly_fur', 'Woolly Fur'),
    ('MATERIAL_ANTLER', 'material_antler', PLAIN, 'material/antler', 'Antler'),
    ('MATERIAL_SCUTE', 'material_scute', PLAIN, 'material/scute', 'Glyptodont Scute'),
    ('MATERIAL_HORN', 'material_horn', PLAIN, 'material/horn', 'Horn'),
    ('MATERIAL_SHARK_TOOTH', 'material_shark_tooth', PLAIN, 'material/shark_tooth', 'Shark Tooth'),
    ('MEAT_CETACEAN_RAW', 'food_cetacean_raw',
     'new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).meat().build()).tab(CreativeModeTab.TAB_FOOD))',
     'food/meat_cetacean_raw', None),
    ('MEAT_CETACEAN_COOKED', 'food_cetacean_cooked',
     'new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).meat().build()).tab(CreativeModeTab.TAB_FOOD))',
     'food/meat_cetacean_cooked', None),
]


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path):
        return 0
    with open(path, 'w', encoding='utf8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')
    return 1


def register_items():
    raw = open(J, encoding='utf8').read()
    nl = '\r\n' if '\r\n' in raw else '\n'
    s = raw.replace('\r\n', '\n')
    lines = ['    public static RegistryObject<Item> %s = createItem("%s", () -> %s);' % (c, i, ctor)
             for c, i, ctor, _, _ in ITEMS if '"%s"' % i not in s]
    if lines:
        m = re.search(r'    public static RegistryObject<Item> MEAT_HIPPO_COOKED = [^\n]*\n', s)
        s = s[:m.end()] + ''.join(l + '\n' for l in lines) + s[m.end():]
        open(J, 'w', encoding='utf8', newline='').write(s.replace('\n', nl))
    return len(lines)


def pool(name, item, lo, hi, conditions=None, looting=(0, 1)):
    fns = [{"function": "minecraft:set_count", "count": {"min": lo, "max": hi}},
           {"function": "minecraft:looting_enchant", "count": {"min": looting[0], "max": looting[1]}}]
    entry = {"type": "minecraft:item", "name": item, "functions": fns}
    if conditions:
        entry["conditions"] = conditions
    return {"name": name, "rolls": 1, "entries": [entry]}


def nbt(tag):
    return {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"nbt": tag}}


def any_of(*tags):
    return [{"condition": "minecraft:alternative", "terms": [nbt(t) for t in tags]}]


def add_pools(table, pools, replace_item=None):
    p = os.path.join(LOOT, table + '.json')
    raw = open(p, encoding='utf8').read()
    if replace_item:
        raw = raw.replace('"%s"' % replace_item[0], '"%s"' % replace_item[1])
    d = json.loads(raw)
    have = {pl.get('name') for pl in d['pools']}
    added = [pl for pl in pools if pl['name'] not in have]
    d['pools'] += added
    open(p, 'w', encoding='utf8', newline='\n').write(json.dumps(d, indent=2) + '\n')
    return len(added)


def main():
    n_items = register_items()
    files = 0
    for _, i, _, tex, _ in ITEMS:
        files += write_json(os.path.join(A, 'models/item/%s.json' % i), {"parent": "item/generated", "textures": {"layer0": "untamedwilds:item/" + tex}})
    # en_us names
    p = os.path.join(A, 'lang/en_us.json')
    raw = open(p, encoding='utf8').read()
    add = ''.join('  "item.untamedwilds.%s": "%s",\n' % (i, name) for _, i, _, _, name in ITEMS
                  if name and '"item.untamedwilds.%s"' % i not in raw)
    if add:
        m = re.search(r'  "item\.untamedwilds\.material_snake_skin": "[^"]*",\n', raw)
        raw = raw[:m.end()] + add + raw[m.end():]
        json.loads(raw)
        open(p, 'w', encoding='utf8', newline='').write(raw)
    # recipes: bone-meal / wool fallbacks, whale cooking
    R = os.path.join(D, 'untamedwilds/recipes')
    for item, n in (('material_ivory', 3), ('material_antler', 2), ('material_horn', 2), ('material_shark_tooth', 1)):
        files += write_json(os.path.join(R, 'items/bone_meal_from_%s.json' % item.replace('material_', '')),
                            {"type": "minecraft:crafting_shapeless", "group": "bonemeal",
                             "ingredients": [{"item": "untamedwilds:" + item}], "result": {"item": "minecraft:bone_meal", "count": n}})
    files += write_json(os.path.join(R, 'items/white_wool_from_woolly_fur.json'),
                        {"type": "minecraft:crafting_shaped", "group": "wool", "pattern": ["##", "##"],
                         "key": {"#": {"item": "untamedwilds:material_woolly_fur"}}, "result": {"item": "minecraft:white_wool"}})
    for kind, suffix, t in (('minecraft:smelting', '', 200), ('minecraft:smoking', '_smoking', 100), ('minecraft:campfire_cooking', '_campfire', 600)):
        files += write_json(os.path.join(R, 'smelting/cooked_cetacean_meat%s.json' % suffix),
                            {"type": kind, "ingredient": {"item": "untamedwilds:food_cetacean_raw"},
                             "result": "untamedwilds:food_cetacean_cooked", "experience": 0.35, "cookingtime": t})
    # forge meat tags
    for tag, item in (('raw_meats', 'food_cetacean_raw'), ('cooked_meats', 'food_cetacean_cooked')):
        tp = os.path.join(D, 'forge/tags/items/%s.json' % tag)
        d = json.load(open(tp, encoding='utf8'))
        if 'untamedwilds:' + item not in d['values']:
            d['values'].insert(3, 'untamedwilds:' + item)
            open(tp, 'w', encoding='utf8', newline='\n').write(json.dumps(d, indent=2) + '\n')
    # loot
    pools = 0
    pools += add_pools('mammoth', [
        pool('mammoth_ivory', 'untamedwilds:material_ivory', 1, 2, [{"condition": "minecraft:inverted", "term": {"condition": "minecraft:alternative", "terms": [nbt('{Variant:10}'), nbt('{Variant:11}')]}}]),
        pool('mammoth_ivory_dwarf', 'untamedwilds:material_ivory', 0, 1, any_of('{Variant:10}', '{Variant:11}')),
        pool('mammoth_fur', 'untamedwilds:material_woolly_fur', 2, 4, any_of('{Variant:0}', '{Variant:2}')),
    ], replace_item=('minecraft:beef', 'untamedwilds:food_pachyderm_raw'))
    pools += add_pools('rhino', [
        pool('rhino_horn', 'untamedwilds:material_horn', 1, 1),
        pool('rhino_fur', 'untamedwilds:material_woolly_fur', 2, 4, any_of('{Variant:5}')),
    ])
    pools += add_pools('bison', [pool('bison_horn', 'untamedwilds:material_horn', 0, 2)])
    pools += add_pools('antelope', [pool('antelope_horn', 'untamedwilds:material_horn', 0, 2,
                                         [{"condition": "minecraft:inverted", "term": nbt('{Variant:0}')}])])
    pools += add_pools('deer', [pool('deer_antler', 'untamedwilds:material_antler', 0, 2, any_of('{Gender:0}', '{Variant:9}'))])
    pools += add_pools('glyptodont', [pool('glyptodont_scute', 'untamedwilds:material_scute', 2, 5)])
    pools += add_pools('shark', [pool('shark_tooth', 'untamedwilds:material_shark_tooth', 0, 2)])
    pools += add_pools('baleen_whale', [], replace_item=('untamedwilds:food_pachyderm_raw', 'untamedwilds:food_cetacean_raw'))
    print('items +%d, json files +%d, en_us +%d, loot pools +%d' % (n_items, files, add.count('\n'), pools))


if __name__ == '__main__':
    main()
