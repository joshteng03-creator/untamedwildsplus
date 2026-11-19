"""Register batch 3 (2026-10-06): hunting spears (6 tiers + tooth/antler trims), carapace shield,
taxidermy kit, mounted specimen (item + entity), display plinth. Java registry edits + item/block
models, blockstate, loot, recipes, tags and en_us. Idempotent.

    python scripts/register_batch3.py
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import register_gear_data as gd
from register_gear_data import A, D, NS, w, item_model, block_model, blockstate, self_loot, shaped, tag

ROOT = gd.ROOT
J = os.path.join(ROOT, 'src/main/java/untamedwilds')
TIERS = [('wooden', 'WOOD', '#minecraft:planks', 'Wooden'), ('stone', 'STONE', '#minecraft:stone_tool_materials', 'Stone'),
         ('iron', 'IRON', 'minecraft:iron_ingot', 'Iron'), ('golden', 'GOLD', 'minecraft:gold_ingot', 'Golden'),
         ('diamond', 'DIAMOND', 'minecraft:diamond', 'Diamond'), ('netherite', 'NETHERITE', None, 'Netherite')]


def edit(rel, fn):
    path = os.path.join(J, rel)
    raw = open(path, encoding='utf8').read()
    nl = '\r\n' if '\r\n' in raw else '\n'
    s = fn(raw.replace('\r\n', '\n'))
    open(path, 'w', encoding='utf8', newline='').write(s.replace('\n', nl))


def add_imports(s, imports):
    m = list(re.finditer(r'^import [^\n]+;\n', s, re.M))[-1]
    new = ''.join('import %s;\n' % i for i in imports if 'import %s;' % i not in s)
    return s[:m.end()] + new + s[m.end():]


def after(s, pattern, text):
    m = re.search(pattern, s)
    assert m, pattern
    return s[:m.end()] + text + s[m.end():]


# ---------------------------------------------------------------- Java
def items(s):
    if '"wooden_spear"' in s:
        return s
    s = add_imports(s, ['net.minecraft.world.item.Tiers', 'net.minecraft.world.item.TooltipFlag', 'net.minecraft.world.level.Level',
                        'net.minecraft.network.chat.Component', 'net.minecraft.network.chat.TranslatableComponent', 'net.minecraft.ChatFormatting',
                        'untamedwilds.item.SpearItem', 'untamedwilds.item.CarapaceShieldItem', 'untamedwilds.item.MountedSpecimenItem',
                        'java.util.List'])
    lines = []
    for name, tier, _, _ in TIERS:
        props = 'new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)' + ('.fireResistant()' if name == 'netherite' else '')
        lines.append('    public static RegistryObject<Item> %s_SPEAR = createItem("%s_spear", () -> new SpearItem(Tiers.%s, %s));' % (name.upper(), name, tier, props))
    lines += [
        '    public static RegistryObject<Item> CARAPACE_SHIELD = createItem("carapace_shield", () -> new CarapaceShieldItem(new Item.Properties().durability(504).tab(CreativeModeTab.TAB_COMBAT)));',
        '    public static RegistryObject<Item> TAXIDERMY_KIT = createItem("taxidermy_kit", () -> new Item(new Item.Properties().stacksTo(16).tab(ModCreativeModeTab.untamedwilds_items)) {',
        '        public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {',
        '            tooltip.add(new TranslatableComponent("item.untamedwilds.taxidermy_kit.desc").withStyle(ChatFormatting.GRAY));',
        '        }',
        '    });',
        '    public static RegistryObject<Item> MOUNTED_SPECIMEN = createItem("mounted_specimen", () -> new MountedSpecimenItem(new Item.Properties().stacksTo(1)));',
    ]
    return after(s, r'    public static RegistryObject<Item> HERDING_HORN = [^\n]*\n', '\n'.join(lines) + '\n')


def blocks(s):
    if '"display_plinth"' in s:
        return s
    line = ('    public static RegistryObject<Block> DISPLAY_PLINTH = createBlock("display_plinth", () -> new Block(Block.Properties.of(Material.STONE, MaterialColor.STONE)'
            '.requiresCorrectToolForDrops().strength(2.0F, 6.0F)), CreativeModeTab.TAB_DECORATIONS);\n')
    return after(s, r'    public static RegistryObject<Block> CHISELED_IVORY = [^\n]*\n', line)


def entity(s):
    if 'THROWN_SPEAR' in s:
        return s
    s = add_imports(s, ['untamedwilds.entity.ProjectileThrownSpear', 'untamedwilds.entity.EntityMountedSpecimen',
                        'untamedwilds.client.render.RendererThrownSpear', 'untamedwilds.client.render.RendererMountedSpecimen'])
    s = after(s, r'    public static RegistryObject<EntityType<ProjectileSerratedArrow>> SERRATED_ARROW = [^\n]*\n',
              '    public static RegistryObject<EntityType<ProjectileThrownSpear>> THROWN_SPEAR = createProjectile(ProjectileThrownSpear::new, "thrown_spear", 4, 20, true, 0.5F, 0.5F);\n'
              '    // Taxidermy display piece: a plain entity, not a mob, so it has no spawn egg and no attributes.\n'
              '    public static RegistryObject<EntityType<EntityMountedSpecimen>> MOUNTED_SPECIMEN = ENTITIES.register("mounted_specimen", () -> EntityType.Builder.<EntityMountedSpecimen>of(EntityMountedSpecimen::new, MobCategory.MISC)\n'
              '            .sized(0.5F, 0.5F).fireImmune().clientTrackingRange(10).updateInterval(20).build("mounted_specimen"));\n')
    return after(s, r'        event.registerEntityRenderer\(ModEntity.SERRATED_ARROW.get\(\), RendererSerratedArrow::new\);\n',
                 '        event.registerEntityRenderer(ModEntity.THROWN_SPEAR.get(), RendererThrownSpear::new);\n'
                 '        event.registerEntityRenderer(ModEntity.MOUNTED_SPECIMEN.get(), RendererMountedSpecimen::new);\n')


CLIENT = '''        event.enqueueWork(() -> {
            // Spear trims swap the item model; the carapace shield needs vanilla's "blocking" predicate,
            // which vanilla only registers for its own shield.
            for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
                if (item.get() instanceof SpearItem) {
                    ItemProperties.register(item.get(), new ResourceLocation(MOD_ID, "trim"), (stack, level, entity, seed) -> SpearItem.trimModelValue(stack));
                }
            }
            ItemProperties.register(ModItems.CARAPACE_SHIELD.get(), new ResourceLocation("blocking"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
'''


def setup(s):
    if 'ModRecipes' in s:
        return s
    s = add_imports(s, ['net.minecraft.client.renderer.item.ItemProperties', 'net.minecraft.resources.ResourceLocation',
                        'net.minecraft.world.item.Item', 'net.minecraftforge.registries.RegistryObject',
                        'untamedwilds.init.ModRecipes', 'untamedwilds.item.SpearItem'])
    s = after(s, r'        ModEntity.ENTITIES.register\(eventBus\);\n', '        ModRecipes.SERIALIZERS.register(eventBus);\n')
    return after(s, r'        ModBlock.registerBlockColors\(\);\n', CLIENT)


# ---------------------------------------------------------------- data
def recipe(name, obj):
    w(os.path.join(D, 'untamedwilds/recipes/items/%s.json' % name), obj)


def data():
    for name, _, mat, _ in TIERS:
        spear = '%s_spear' % name
        overrides = []
        for trim, value in (('tooth', 0.5), ('antler', 1.0)):
            w(os.path.join(A, 'models/item/%s_%s.json' % (spear, trim)),
              {"parent": "item/handheld", "textures": {"layer0": NS + 'item/' + spear, "layer1": NS + 'item/spear_trim_' + trim}})
            overrides.append({"predicate": {NS + "trim": value}, "model": NS + 'item/%s_%s' % (spear, trim)})
        w(os.path.join(A, 'models/item/%s.json' % spear),
          {"parent": "item/handheld", "textures": {"layer0": NS + 'item/' + spear}, "overrides": overrides})
        if mat:
            key = {"tag": mat[1:]} if mat.startswith('#') else {"item": mat}
            recipe(spear, {"type": "minecraft:crafting_shaped", "group": "untamedwilds:spear", "pattern": ["  M", " S ", "S  "],
                           "key": {"M": key, "S": {"item": "minecraft:stick"}}, "result": {"item": NS + spear}})
    recipe('netherite_spear_smithing', {"type": "minecraft:smithing", "base": {"item": NS + "diamond_spear"},
                                        "addition": {"item": "minecraft:netherite_ingot"}, "result": {"item": NS + "netherite_spear"}})
    w(os.path.join(D, 'untamedwilds/recipes/items/spear_trim.json'), {"type": NS + "spear_trim"})

    # carapace shield: vanilla shield models, the scute as particle
    vanilla = json.load(open(os.path.join(os.environ.get('VANILLA_MODELS', ''), 'shield.json'))) if os.environ.get('VANILLA_MODELS') else None
    if vanilla:
        blocking = json.load(open(os.path.join(os.environ['VANILLA_MODELS'], 'shield_blocking.json')))
        for obj in (vanilla, blocking):
            obj['textures'] = {"particle": NS + "item/material_scute"}
        vanilla['overrides'] = [{"predicate": {"blocking": 1}, "model": NS + "item/carapace_shield_blocking"}]
        w(os.path.join(A, 'models/item/carapace_shield.json'), vanilla)
        w(os.path.join(A, 'models/item/carapace_shield_blocking.json'), blocking)
    else:
        assert os.path.exists(os.path.join(A, 'models/item/carapace_shield.json')), 'set VANILLA_MODELS to the vanilla models/item dir'
    recipe('carapace_shield', {"type": "minecraft:crafting_shapeless", "ingredients": [
        {"item": "minecraft:shield"}, {"item": NS + "material_scute"}, {"item": NS + "material_scute"}], "result": {"item": NS + "carapace_shield"}})

    item_model('taxidermy_kit')
    item_model('mounted_specimen')
    recipe('taxidermy_kit', {"type": "minecraft:crafting_shapeless", "ingredients": [
        {"item": "minecraft:leather"}, {"item": "minecraft:string"}, {"item": "minecraft:white_wool"}, {"item": "minecraft:iron_nugget"}],
        "result": {"item": NS + "taxidermy_kit"}})

    block_model('display_plinth', 'minecraft:block/cube_bottom_top',
                {"top": "minecraft:block/oak_planks", "bottom": "minecraft:block/smooth_stone", "side": "minecraft:block/smooth_stone_slab_side"})
    blockstate('display_plinth', {"variants": {"": {"model": NS + "block/display_plinth"}}})
    item_model('display_plinth', parent=NS + 'block/display_plinth')
    self_loot('display_plinth')
    shaped('display_plinth', ["P", "S"], {"P": "minecraft:oak_slab", "S": "minecraft:smooth_stone"}, NS + 'display_plinth')

    tag('minecraft/tags/blocks/mineable/pickaxe.json', [NS + 'display_plinth'])
    tag('minecraft/tags/items/piglin_loved.json', [NS + 'golden_spear'])

    names = {'item.untamedwilds.%s_spear' % n: '%s Spear' % label for n, _, _, label in TIERS}
    names.update({
        'item.untamedwilds.spear.trim_tooth': 'Shark Tooth Trim',
        'item.untamedwilds.spear.trim_antler': 'Antler Trim',
        'item.untamedwilds.spear.trim_bonus': '+%s damage to wild animals',
        'entity.untamedwilds.thrown_spear': 'Spear',
        'death.attack.untamedwilds.spear': '%1$s was skewered by %2$s',
        'death.attack.untamedwilds.spear.item': '%1$s was skewered by %2$s using %3$s',
        'item.untamedwilds.carapace_shield': 'Carapace Shield',
        'item.untamedwilds.taxidermy_kit': 'Taxidermy Kit',
        'item.untamedwilds.taxidermy_kit.desc': 'Hold in your off hand while hunting to mount your kill',
        'item.untamedwilds.mounted_specimen': 'Mounted Specimen',
        'item.untamedwilds.mounted_specimen.of': 'Mounted %s',
        'item.untamedwilds.mounted_specimen.empty': 'Made with a Taxidermy Kit',
        'item.untamedwilds.mounted_specimen.juvenile': 'Juvenile',
        'entity.untamedwilds.mounted_specimen': 'Mounted Specimen',
        'block.untamedwilds.display_plinth': 'Display Plinth',
    })
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
    edit('init/ModItems.java', items)
    edit('init/ModBlock.java', blocks)
    edit('init/ModEntity.java', entity)
    edit('UntamedWilds.java', setup)
    data()
