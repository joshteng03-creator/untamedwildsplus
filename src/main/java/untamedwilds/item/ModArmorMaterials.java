package untamedwilds.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import untamedwilds.UntamedWilds;
import untamedwilds.init.ModItems;

import java.util.function.Supplier;

/**
 * Armour made from animal materials (2026-10-06), balanced against vanilla's own tiers so neither
 * set competes with diamond or netherite:
 * <ul>
 * <li><b>FUR</b> (woolly fur) -- leather's exact stats. Its identity is warmth: every piece is in
 *     {@code minecraft:freeze_immune_wearables}, as leather is, so it stops powder-snow freezing.</li>
 * <li><b>CARAPACE</b> (glyptodont scutes) -- iron's defence and durability plus 1 toughness, and
 *     0.05 knockback resistance per piece (0.2 for the full set) -- a nod to an animal built like a
 *     tank, at a quarter of netherite's per-piece knockback resistance.</li>
 * </ul>
 * Texture lookup follows vanilla: material name {@code untamedwilds:fur} resolves to
 * {@code untamedwilds:textures/models/armor/fur_layer_1.png} / {@code _layer_2.png}.
 */
public enum ModArmorMaterials implements ArmorMaterial {
    FUR("fur", 5, new int[]{1, 2, 3, 1}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.of(ModItems.MATERIAL_WOOLLY_FUR.get())),
    CARAPACE("carapace", 15, new int[]{2, 5, 6, 2}, 9, SoundEvents.ARMOR_EQUIP_TURTLE, 1.0F, 0.05F, () -> Ingredient.of(ModItems.MATERIAL_SCUTE.get()));

    // Vanilla's per-slot durability base, in EquipmentSlot armour index order (feet, legs, chest, head).
    private static final int[] HEALTH_PER_SLOT = new int[]{13, 15, 16, 11};

    private final String name;
    private final int durabilityMultiplier;
    private final int[] slotProtections;
    private final int enchantmentValue;
    private final SoundEvent sound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    ModArmorMaterials(String name, int durabilityMultiplier, int[] slotProtections, int enchantmentValue, SoundEvent sound,
                      float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.slotProtections = slotProtections;
        this.enchantmentValue = enchantmentValue;
        this.sound = sound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurabilityForSlot(EquipmentSlot slot) {
        return HEALTH_PER_SLOT[slot.getIndex()] * this.durabilityMultiplier;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot slot) {
        return this.slotProtections[slot.getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.sound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String getName() {
        return UntamedWilds.MOD_ID + ":" + this.name;
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
