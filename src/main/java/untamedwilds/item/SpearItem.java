package untamedwilds.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ProjectileThrownSpear;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Hunting spear (2026-10-06), one per vanilla tool tier. A sword sidegrade: one point less damage
 * and a slower swing (1.2) than the same tier's sword, no sweep, but one block more reach, and it
 * can be thrown like a trident (hold to charge, release) for its melee damage. A thrown spear sticks
 * and is picked back up; each throw costs 1 durability.
 * <p>
 * Extends SwordItem on purpose: sword enchantments (Sharpness, Smite, Looting, Fire Aspect...) and
 * tier repair then work at the table and the anvil with no extra code, since vanilla tests the
 * WEAPON category with {@code instanceof SwordItem}.
 * <p>
 * Trims: crafting a spear with a shark tooth or an antler ({@link SpearTrimRecipe}) stores
 * {@link #TRIM_TAG}; a trimmed spear does +{@link #TRIM_BONUS} damage to the mod's animals, melee
 * (HuntingEvents) and thrown. A new trim replaces the old one.
 */
public class SpearItem extends SwordItem {

    public static final String TRIM_TAG = "SpearTrim";
    public static final String TRIM_TOOTH = "tooth";
    public static final String TRIM_ANTLER = "antler";
    public static final float TRIM_BONUS = 2.0F;
    private static final UUID ATTACK_RANGE_UUID = UUID.fromString("6f3b2a54-1d8e-4c9a-b2f1-7a5e0c3d9b41");
    private static final int THROW_THRESHOLD = 10;
    private static final float THROW_POWER = 2.0F;

    // Built on first use: Forge's attributes register after items, so ATTACK_RANGE is not resolvable
    // in the constructor.
    private Multimap<Attribute, AttributeModifier> mainhandModifiers;

    public SpearItem(Tier tier, Properties properties) {
        super(tier, 2, -2.8F, properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }
        if (this.mainhandModifiers == null) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(super.getDefaultAttributeModifiers(slot));
            builder.put(ForgeMod.ATTACK_RANGE.get(), new AttributeModifier(ATTACK_RANGE_UUID, "Spear reach", 1.0D, AttributeModifier.Operation.ADDITION));
            this.mainhandModifiers = builder.build();
        }
        return this.mainhandModifiers;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return action != ToolActions.SWORD_SWEEP && super.canPerformAction(stack, action);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment != Enchantments.SWEEPING_EDGE && super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player) || this.getUseDuration(stack) - timeLeft < THROW_THRESHOLD) {
            return;
        }
        if (!level.isClientSide) {
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(user.getUsedItemHand()));
            ProjectileThrownSpear spear = new ProjectileThrownSpear(level, player, stack);
            spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_POWER, 1.0F);
            if (player.getAbilities().instabuild) {
                spear.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            level.addFreshEntity(spear);
            level.playSound(null, spear, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                player.getInventory().removeItem(stack);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String trim = getTrim(stack);
        if (!trim.isEmpty()) {
            tooltip.add(new TranslatableComponent("item.untamedwilds.spear.trim_" + trim).withStyle(ChatFormatting.GRAY));
            tooltip.add(new TranslatableComponent("item.untamedwilds.spear.trim_bonus", (int) TRIM_BONUS).withStyle(ChatFormatting.BLUE));
        }
    }

    public static String getTrim(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getString(TRIM_TAG) : "";
    }

    public static void setTrim(ItemStack stack, String trim) {
        stack.getOrCreateTag().putString(TRIM_TAG, trim);
    }

    /** The trim's extra damage against {@code target}: only the mod's animals, only when trimmed. */
    public static float trimBonus(ItemStack stack, Entity target) {
        return stack.getItem() instanceof SpearItem && target instanceof ComplexMob && !getTrim(stack).isEmpty() ? TRIM_BONUS : 0.0F;
    }

    /** Item-model predicate "untamedwilds:trim": 0 untrimmed, 0.5 tooth, 1 antler. */
    public static float trimModelValue(ItemStack stack) {
        return switch (getTrim(stack)) {
            case TRIM_TOOTH -> 0.5F;
            case TRIM_ANTLER -> 1.0F;
            default -> 0.0F;
        };
    }
}
