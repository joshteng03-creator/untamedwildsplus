package untamedwilds.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.EntityMountedSpecimen;
import untamedwilds.init.ModItems;
import untamedwilds.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.List;

/**
 * "Mounted <species>" (taxidermy, 2026-10-06): what a Taxidermy Kit turns a kill into. Holds the
 * animal's saved data under {@code EntityTag} -- the key the cage and the mob items already use, so
 * {@link EntityUtils#buildTooltipData} reads gender and species from it -- with everything that
 * belongs to a living animal (position, health, hunger, home, owner, AI memory...) stripped. Placed on
 * a block it becomes an {@link EntityMountedSpecimen} facing the player; on a top face it sits on the
 * face's real height, so it stands on slabs and the Display Plinth.
 */
public class MountedSpecimenItem extends Item {

    public static final String ENTITY_TAG = "EntityTag";
    public static final String WIDTH_KEY = "MountWidth";
    public static final String HEIGHT_KEY = "MountHeight";
    private static final String[] LIVING_STATE = {
            "Pos", "Motion", "Rotation", "UUID", "FallDistance", "Fire", "Air", "OnGround", "PortalCooldown",
            "Invulnerable", "Passengers", "Tags", "Health", "HurtTime", "HurtByTimestamp", "DeathTime",
            "AbsorptionAmount", "Attributes", "ActiveEffects", "Brain", "FallFlying", "SleepingX", "SleepingY",
            "SleepingZ", "Leash", "Owner", "Sitting", "InLove", "LoveCause", "ForcedAge", "PersistenceRequired",
            "CanPickUpLoot", "LeftHanded", "ArmorItems", "HandItems", "ArmorDropChances", "HandDropChances",
            "DeathLootTable", "DeathLootTableSeed", "HomePosX", "HomePosY", "HomePosZ", "Command", "isAngry",
            "PeacefulTicks", "StarveTicks", "NextDrink"};
    // Hunger, Condition and LifeTicks stay: without them ComplexMob.readAdditionalSaveData derives
    // defaults from server-only species data, which a multiplayer client does not have.

    public MountedSpecimenItem(Properties properties) {
        super(properties);
    }

    /** The mount for a freshly killed animal. */
    public static ItemStack of(ComplexMob mob) {
        return withSpecimen(snapshot(mob));
    }

    /**
     * The animal's saved data minus its living state, plus its type id and hitbox: enough to rebuild an un-ticked
     * display copy on any client. Shared by mounted specimens and carcasses (2.8.0).
     */
    public static CompoundTag snapshot(ComplexMob mob) {
        CompoundTag data = mob.saveWithoutId(new CompoundTag());
        for (String key : LIVING_STATE) {
            data.remove(key);
        }
        data.putString("id", EntityType.getKey(mob.getType()).toString());
        data.putFloat(WIDTH_KEY, mob.getBbWidth());
        data.putFloat(HEIGHT_KEY, mob.getBbHeight());
        return data;
    }

    public static ItemStack withSpecimen(CompoundTag data) {
        ItemStack stack = new ItemStack(ModItems.MOUNTED_SPECIMEN.get());
        stack.getOrCreateTag().put(ENTITY_TAG, data);
        return stack;
    }

    @Nullable
    private static EntityType<?> typeOf(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ENTITY_TAG);
        return data == null ? null : EntityType.byString(data.getString("id")).orElse(null);
    }

    private static String variantName(ItemStack stack, EntityType<?> type) {
        return EntityUtils.getVariantName(type, stack.getTagElement(ENTITY_TAG).getInt("Variant"));
    }

    @Override
    public Component getName(ItemStack stack) {
        EntityType<?> type = typeOf(stack);
        if (type == null) {
            return super.getName(stack);
        }
        String variant = variantName(stack, type);
        String key = variant.isEmpty() ? type.getDescriptionId() : type.getDescriptionId() + "_" + variant;
        return new TranslatableComponent("item.untamedwilds.mounted_specimen.of", new TranslatableComponent(key));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        EntityType<?> type = typeOf(stack);
        if (type == null) {
            tooltip.add(new TranslatableComponent("item.untamedwilds.mounted_specimen.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        EntityUtils.buildTooltipData(stack, tooltip, type, variantName(stack, type));
        if (stack.getTagElement(ENTITY_TAG).getInt("Age") < 0) {
            tooltip.add(new TranslatableComponent("item.untamedwilds.mounted_specimen.juvenile").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (typeOf(stack) == null) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        Direction face = context.getClickedFace();
        BlockPos place = context.getClickedPos().relative(face);
        double y = face == Direction.UP ? context.getClickLocation().y : place.getY();
        Vec3 at = new Vec3(place.getX() + 0.5D, y, place.getZ() + 0.5D);
        if (!level.isClientSide) {
            // The armour stand's rule: snap to 45 degrees, facing the player.
            int facing = Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) & 7;
            CompoundTag data = stack.getTagElement(ENTITY_TAG).copy();
            EntityMountedSpecimen mount = new EntityMountedSpecimen(level, at.x, at.y, at.z, data, facing);
            if (data.contains("CustomName")) {
                mount.setCustomName(Component.Serializer.fromJson(data.getString("CustomName")));
            }
            if (stack.hasCustomHoverName()) {
                mount.setCustomName(stack.getHoverName());
            }
            level.addFreshEntity(mount);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
