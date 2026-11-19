"""Register the zoo-side items and blocks (2026-10-08): Field Guide now; Feeding Trough, Species Placard and
Salt Lick are appended as they are built. Java registry edits + models, recipes, loot, tags, en_us. Idempotent.

    python scripts/register_zoo.py
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import register_gear_data as gd
from register_gear_data import A, D, NS, w, item_model
from register_batch3 import edit, add_imports, after


def items(s):
    if '"field_guide"' in s:
        return s
    s = add_imports(s, ['untamedwilds.item.FieldGuideItem'])
    return after(s, r'    public static RegistryObject<Item> MOUNTED_SPECIMEN = [^\n]*\n',
                 '    public static RegistryObject<Item> FIELD_GUIDE = createItem("field_guide", () -> new FieldGuideItem(new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.untamedwilds_items)));\n')


NAMES = {
    'item.untamedwilds.field_guide': 'Field Guide',
    'item.untamedwilds.field_guide.desc': 'Use on an animal to study it; sneak-use to open its encyclopedia page',
    'item.untamedwilds.field_guide.last': 'Last studied: %s',
    'item.untamedwilds.field_guide.male': 'Male',
    'item.untamedwilds.field_guide.female': 'Female',
    'item.untamedwilds.field_guide.juvenile': 'juvenile, grown in %s',
    'item.untamedwilds.field_guide.adult': 'adult',
    'item.untamedwilds.field_guide.elderly': 'elderly',
    'item.untamedwilds.field_guide.health': 'Health %s/%s',
    'item.untamedwilds.field_guide.well_fed': 'well fed',
    'item.untamedwilds.field_guide.fed': 'fed',
    'item.untamedwilds.field_guide.hungry': 'hungry',
    'item.untamedwilds.field_guide.starving': 'starving',
    'item.untamedwilds.field_guide.pregnant': 'Expecting young in %s',
    'item.untamedwilds.field_guide.seeking_mate': 'Looking for a mate',
    'item.untamedwilds.field_guide.owner': 'Tamed by %s',
    'item.untamedwilds.field_guide.saddled': 'saddled',
}


def lang(names):
    p = os.path.join(A, 'lang/en_us.json')
    raw = open(p, encoding='utf8').read()
    add = ''.join('  %s: %s,\n' % (json.dumps(k), json.dumps(v)) for k, v in names.items() if json.dumps(k) not in raw)
    if add:
        m = re.search(r'  "item\.untamedwilds\.material_snake_skin": "[^"]*",\n', raw)
        raw = raw[:m.end()] + add + raw[m.end():]
        json.loads(raw)
        open(p, 'w', encoding='utf8', newline='').write(raw)
    return add.count('\n')


BLOCK_NAMES = {
    'block.untamedwilds.feeding_trough': 'Feeding Trough',
    'block.untamedwilds.species_placard': 'Species Placard',
    'block.untamedwilds.species_placard.study_first': 'Study an animal with this Field Guide first',
    'block.untamedwilds.salt_lick': 'Salt Lick',
}


def blocks_data():
    from register_gear_data import self_loot, tag
    for b in ('feeding_trough', 'species_placard', 'salt_lick'):
        self_loot(b)
    w(os.path.join(D, 'untamedwilds/recipes/items/feeding_trough.json'), {
        "type": "minecraft:crafting_shaped", "pattern": ["P P", "SSS"],
        "key": {"P": {"tag": "minecraft:planks"}, "S": {"tag": "minecraft:wooden_slabs"}}, "result": {"item": NS + "feeding_trough"}})
    w(os.path.join(D, 'untamedwilds/recipes/items/species_placard.json'), {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"tag": "minecraft:signs"}, {"item": "minecraft:paper"}, {"item": "minecraft:ink_sac"}],
        "result": {"item": NS + "species_placard"}})
    w(os.path.join(D, 'untamedwilds/recipes/items/salt_lick.json'), {
        "type": "minecraft:crafting_shaped", "pattern": ["CC", "CC"],
        "key": {"C": {"item": "minecraft:calcite"}}, "result": {"item": NS + "salt_lick"}})
    tag('minecraft/tags/blocks/mineable/axe.json', [NS + 'feeding_trough', NS + 'species_placard'])
    tag('minecraft/tags/blocks/mineable/pickaxe.json', [NS + 'salt_lick'])


if __name__ == '__main__':
    if '--blocks' in sys.argv:
        blocks_data()
        print('blocks: json files +%d, en_us +%d' % (gd.n_files, lang(BLOCK_NAMES)))
        sys.exit()
    edit('init/ModItems.java', items)
    item_model('field_guide')
    w(os.path.join(D, 'untamedwilds/recipes/items/field_guide.json'), {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": "minecraft:book"}, {"item": "minecraft:feather"}, {"item": "minecraft:ink_sac"}],
        "result": {"item": NS + "field_guide"}})
    print('json files +%d, en_us +%d' % (gd.n_files, lang(NAMES)))
