package untamedwilds.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import untamedwilds.compat.CompatPatchouli;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.entity.ISpecies;
import untamedwilds.init.ModAdvancementTriggers;
import untamedwilds.util.TimeUtils;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Field Guide (2026-10-08): the survival naturalist's notebook. Use it on one of the mod's animals to read
 * a short field card (species, sex, age stage, health, how well fed it is, breeding state, owner/saddle),
 * the player-facing counterpart of the debug Analyzer. Studying an animal counts as observing it for the
 * discovery advancements and remembers the species ({@link #STUDIED}) for the Species Placard. Sneak-use
 * opens that species' Encyclopedia of Life entry when Patchouli is installed.
 */
public class FieldGuideItem extends Item {

    public static final String STUDIED = "Studied";

    public FieldGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof ComplexMob mob)) {
            return InteractionResult.PASS;
        }
        String variant = mob instanceof ISpecies sp ? sp.getRawSpeciesName(mob.getVariant()) : "";
        String type = EntityType.getKey(mob.getType()).getPath();
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer sp && ModList.get().isLoaded("patchouli")) {
                CompatPatchouli.openEntry(sp, type, variant);
            }
            return InteractionResult.sidedSuccess(player.level.isClientSide);
        }
        if (player.level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        // remember the species for the placard (on the stack in hand, not the copy Minecraft passes in creative)
        ItemStack held = player.getItemInHand(hand);
        CompoundTag studied = new CompoundTag();
        studied.putString("type", EntityType.getKey(mob.getType()).toString());
        studied.putInt("variant", mob.getVariant());
        studied.putString("species", variant);
        held.getOrCreateTag().put(STUDIED, studied);

        for (Component line : card(mob, variant, player.level)) {
            player.sendMessage(line, player.getUUID());
        }
        player.playNotifySound(SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        if (player instanceof ServerPlayer sp) {
            ModAdvancementTriggers.DISCOVERED_ANIMAL.trigger(sp, mob.getType());
        }
        return InteractionResult.SUCCESS;
    }

    private static List<Component> card(ComplexMob mob, String variant, Level level) {
        String key = mob.getType().getDescriptionId() + (variant.isEmpty() ? "" : "_" + variant);
        MutableComponent title = new TranslatableComponent(key).withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD);
        TranslatableComponent sci = new TranslatableComponent(key + ".sciname");
        if (!sci.getString().equals(key + ".sciname")) {
            title.append(new TextComponent(" ").append(sci.withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)));
        }
        MutableComponent about = new TextComponent("");
        if (ConfigGamerules.genderedBreeding.get()) {
            about.append(new TranslatableComponent(mob.isMale() ? "item.untamedwilds.field_guide.male" : "item.untamedwilds.field_guide.female")).append(", ");
        }
        if (mob.isBaby()) {
            about.append(new TranslatableComponent("item.untamedwilds.field_guide.juvenile", TimeUtils.convertTicksToDays(level, -mob.getAge())));
        } else {
            about.append(new TranslatableComponent(mob.isSenescent() ? "item.untamedwilds.field_guide.elderly" : "item.untamedwilds.field_guide.adult"));
        }
        MutableComponent health = new TranslatableComponent("item.untamedwilds.field_guide.health", Math.round(mob.getHealth()), Math.round(mob.getMaxHealth()));
        if (mob instanceof ComplexMobTerrestrial) {
            String fed = mob.starveTicks > 0 ? "starving" : mob.getHunger() < 25 ? "hungry" : mob.getHunger() < 60 ? "fed" : "well_fed";
            health.append(" · ").append(new TranslatableComponent("item.untamedwilds.field_guide." + fed));
        }
        List<Component> out = new java.util.ArrayList<>(List.of(title, about.withStyle(ChatFormatting.GRAY), health.withStyle(ChatFormatting.GRAY)));
        if (!mob.isMale() && mob.getAge() > 0 && !ConfigGamerules.easyBreeding.get()) {
            out.add(new TranslatableComponent("item.untamedwilds.field_guide.pregnant", TimeUtils.convertTicksToDays(level, mob.getAge())).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (mob.wantsToBreed()) {
            out.add(new TranslatableComponent("item.untamedwilds.field_guide.seeking_mate").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (mob.isTame() && mob.getOwner() != null) {
            MutableComponent tame = new TranslatableComponent("item.untamedwilds.field_guide.owner", mob.getOwner().getDisplayName());
            if (mob.isSaddled()) {
                tame.append(" · ").append(new TranslatableComponent("item.untamedwilds.field_guide.saddled"));
            }
            out.add(tame.withStyle(ChatFormatting.GOLD));
        }
        return out;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(new TranslatableComponent("item.untamedwilds.field_guide.desc").withStyle(ChatFormatting.GRAY));
        CompoundTag studied = stack.getTagElement(STUDIED);
        if (studied != null) {
            EntityType.byString(studied.getString("type")).ifPresent(type -> {
                String sp = studied.getString("species");
                tooltip.add(new TranslatableComponent("item.untamedwilds.field_guide.last",
                        new TranslatableComponent(type.getDescriptionId() + (sp.isEmpty() ? "" : "_" + sp))).withStyle(ChatFormatting.DARK_GREEN));
            });
        }
    }
}
