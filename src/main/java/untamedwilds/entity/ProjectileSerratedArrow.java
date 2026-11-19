package untamedwilds.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import untamedwilds.init.ModEntity;
import untamedwilds.init.ModItems;

/**
 * Serrated arrow (2026-10-06): a shark-tooth-tipped arrow. Vanilla arrow damage, plus a short bleed
 * on hit -- Wither I for two seconds, on the scale of a tipped arrow's effect -- so it is a sidegrade,
 * not a better arrow. Picked back up as itself.
 */
public class ProjectileSerratedArrow extends AbstractArrow {

    private static final int BLEED_TICKS = 40;

    public ProjectileSerratedArrow(EntityType<? extends ProjectileSerratedArrow> type, Level level) {
        super(type, level);
    }

    public ProjectileSerratedArrow(Level level, LivingEntity shooter) {
        super(ModEntity.SERRATED_ARROW.get(), shooter, level);
    }

    public ProjectileSerratedArrow(Level level, double x, double y, double z) {
        super(ModEntity.SERRATED_ARROW.get(), x, y, z, level);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, BLEED_TICKS, 0), this.getEffectSource());
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.SERRATED_ARROW.get());
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
