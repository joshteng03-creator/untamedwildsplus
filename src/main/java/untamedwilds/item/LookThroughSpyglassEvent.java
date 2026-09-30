package untamedwilds.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ISpecies;
import untamedwilds.entity.relict.EntitySpitter;
import untamedwilds.init.ModAdvancementTriggers;

@Mod.EventBusSubscriber(modid = UntamedWilds.MOD_ID)
public class LookThroughSpyglassEvent {

    @SubscribeEvent
    public static void lookAtEntityThroughSpyglassEvent(LivingEntityUseItemEvent event) {
        ItemStack usedItem = event.getItem();
        Entity entity = event.getEntity();
        if (ConfigGamerules.spyglassBehaviorChange.get() && !entity.getLevel().isClientSide && entity instanceof Player playerIn && playerIn.tickCount % 20 == 0 && usedItem.getItem().equals(Items.SPYGLASS)) {
            HitResult hitresult = raycast(playerIn, ConfigGamerules.spyglassCheckRange.get(), true);
            if (hitresult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHitResult = (EntityHitResult)hitresult;
                if (entityHitResult.getEntity() instanceof LivingEntity livingEntityHitResult) {
                    displayEntityData(livingEntityHitResult, playerIn, playerIn.getLevel());

                    // TODO: Hardcoded list of "observing" advancements
                    if (entityHitResult.getEntity() instanceof EntitySpitter)
                        ModAdvancementTriggers.DISCOVERED_SPITTER.trigger((ServerPlayer) playerIn);
                }
            }
        }
    }

    public static HitResult raycast(Entity origin, double maxDistance, boolean hitsEntities) {
        /* Level.clip reads block states along the whole ray, and on the server getBlockState loads -- or
         * generates -- any chunk it touches. At the default 5000-block spyglass range, one look across
         * open plains or ocean could generate hundreds of chunks on the main thread. Nothing past the view
         * distance is loaded or rendered for the player anyway. */
        if (origin.level.getServer() != null) {
            maxDistance = Math.min(maxDistance, origin.level.getServer().getPlayerList().getViewDistance() * 16.0D);
        }
        Vec3 startPos = origin.getEyePosition(1F);
        Vec3 rotation = origin.getViewVector(1F);
        Vec3 endPos = startPos.add(rotation.x * maxDistance, rotation.y * maxDistance, rotation.z * maxDistance);
        HitResult hitResult = origin.level.clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, origin));

        if(hitResult.getType() != HitResult.Type.MISS)
            endPos = hitResult.getLocation();

        /* getEntityHitResult's last argument is a SQUARED distance. It was passed range x 5, so entities were
         * only ever found within sqrt(5 x range) -- about 158 blocks at the default range -- while the search
         * box was sized to range x 5 (25,000 blocks) and swept every entity along it. Same reach, box sized
         * to it. */
        double entityReachSqr = maxDistance * 5;
        double entityReach = Math.sqrt(entityReachSqr);
        HitResult entityHitResult = ProjectileUtil.getEntityHitResult(origin, startPos, endPos, origin.getBoundingBox().expandTowards(rotation.scale(entityReach)).inflate(1.0D, 1.0D, 1.0D), entity -> !entity.isSpectator(), entityReachSqr);

        if(hitsEntities && entityHitResult != null)
            hitResult = entityHitResult;

        return hitResult;
    }

    private static void displayEntityData(LivingEntity target, Player playerIn, Level world) {
        TextComponent name = new TextComponent("");
        if (target instanceof ComplexMob entity) {
            String entityName = entity instanceof ISpecies ? ((ISpecies) entity).getSpeciesName() : entity.getName().getString();
            name.append((entity.isBaby() ? "Young " : "") + (ConfigGamerules.genderedBreeding.get() ? entity.getGenderString() + " " : "") + entityName + " ");

            if (ConfigGamerules.scientificNames.get()) {
                String useVarName = entity instanceof ISpecies ? "_" + ((ISpecies) entity).getRawSpeciesName(entity.getVariant()) : "";
                name.append("(");
                name.append(new TranslatableComponent(entity.getType().getDescriptionId() + useVarName + ".sciname").withStyle(ChatFormatting.ITALIC));
                name.append(") ");
            }
            if (!entity.isMale() && entity.getAge() > 0 && !ConfigGamerules.easyBreeding.get()) {
                name.append("This female is pregnant ");
            }
        }
        else {
            name.append(target.isBaby() ? "Young " : "" + target.getName().getString() + " ");
        }
        if (true) {
            int health = (int) (10 * target.getHealth() / target.getMaxHealth());
            MutableComponent state = getHealthState(health);
            name.append("(");
            name.append(state);
            name.append(") ");
        }
        if (true) {
            name.append("(");
            name.append(getThreatLevel(target, playerIn));
            name.append(")");
        }
        playerIn.displayClientMessage(name, true);
    }

    private static MutableComponent getHealthState(int health) {
        switch (health) {
            case 10, 9, 8 -> {
                return new TextComponent("Healthy").withStyle(ChatFormatting.GREEN);
            }
            case 7, 6, 5 -> {
                return new TextComponent("Injured").withStyle(ChatFormatting.YELLOW);
            }
            case 4, 3, 2 -> {
                return new TextComponent("Wounded").withStyle(ChatFormatting.RED);
            }
            case 1, 0 -> {
                return new TextComponent("Almost Dead").withStyle(ChatFormatting.DARK_RED);
            }
        }
        return new TextComponent("");
    }

    private static MutableComponent getThreatLevel(LivingEntity target, Player player) {
        int val = ComplexMob.getEcoLevel(player) - ComplexMob.getEcoLevel(target);
        if (val > 4)
            return new TextComponent("Harmless").withStyle(ChatFormatting.GREEN);
        else if (val > 2)
            return new TextComponent("Mild threat").withStyle(ChatFormatting.YELLOW);
        else if (val > 0)
            return new TextComponent("Caution").withStyle(ChatFormatting.YELLOW);
        else if (val > -4)
            return new TextComponent("Dangerous").withStyle(ChatFormatting.RED);
        else
            return new TextComponent("Deadly").withStyle(ChatFormatting.DARK_RED);
    }
}
