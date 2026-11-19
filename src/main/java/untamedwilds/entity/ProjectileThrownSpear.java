package untamedwilds.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import untamedwilds.init.ModEntity;
import untamedwilds.init.ModItems;
import untamedwilds.item.SpearItem;

import javax.annotation.Nullable;

/**
 * A thrown hunting spear (2026-10-06), modelled on vanilla's ThrownTrident: hits once for the spear's
 * melee damage (+ Sharpness/Smite/Bane, + the trim bonus against the mod's animals, + Fire Aspect and
 * Knockback, which a trident lacks but a spear's sword enchantments promise), then drops away and
 * sticks where it lands to be picked back up. The spear's stack rides along in synched data so the
 * client draws the right tier and trim.
 */
public class ProjectileThrownSpear extends AbstractArrow {

    private static final EntityDataAccessor<ItemStack> SPEAR = SynchedEntityData.defineId(ProjectileThrownSpear.class, EntityDataSerializers.ITEM_STACK);
    private boolean dealtDamage;

    public ProjectileThrownSpear(EntityType<? extends ProjectileThrownSpear> type, Level level) {
        super(type, level);
    }

    public ProjectileThrownSpear(Level level, LivingEntity thrower, ItemStack spear) {
        super(ModEntity.THROWN_SPEAR.get(), thrower, level);
        this.entityData.set(SPEAR, spear.copy());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SPEAR, ItemStack.EMPTY);
    }

    public ItemStack getSpear() {
        ItemStack spear = this.entityData.get(SPEAR);
        return spear.isEmpty() ? new ItemStack(ModItems.WOODEN_SPEAR.get()) : spear;
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }
        super.tick();
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        return this.dealtDamage ? null : super.findHitEntity(from, to);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        ItemStack spear = this.getSpear();
        float damage = spear.getItem() instanceof SwordItem sword ? sword.getDamage() + 1.0F : 1.0F;
        if (target instanceof LivingEntity living) {
            damage += EnchantmentHelper.getDamageBonus(spear, living.getMobType());
        }
        damage += SpearItem.trimBonus(spear, target);
        Entity owner = this.getOwner();
        DamageSource source = new IndirectEntityDamageSource("untamedwilds.spear", this, owner == null ? this : owner).setProjectile();
        this.dealtDamage = true;
        int fire = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, spear);
        if (fire > 0 && target.getType() != EntityType.ENDERMAN) {
            target.setSecondsOnFire(fire * 4);
        }
        if (target.hurt(source, damage)) {
            if (target.getType() == EntityType.ENDERMAN) {
                return;
            }
            if (target instanceof LivingEntity living) {
                int knockback = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, spear);
                if (knockback > 0) {
                    Vec3 push = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize().scale(knockback * 0.6D);
                    if (push.lengthSqr() > 0.0D) {
                        living.push(push.x, 0.1D, push.z);
                    }
                }
                if (owner instanceof LivingEntity thrower) {
                    EnchantmentHelper.doPostHurtEffects(living, thrower);
                    EnchantmentHelper.doPostDamageEffects(thrower, living);
                }
                this.doPostHurtEffects(living);
            }
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    @Override
    protected ItemStack getPickupItem() {
        return this.getSpear().copy();
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Spear", this.getSpear().save(new CompoundTag()));
        tag.putBoolean("DealtDamage", this.dealtDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Spear", 10)) {
            this.entityData.set(SPEAR, ItemStack.of(tag.getCompound("Spear")));
        }
        this.dealtDamage = tag.getBoolean("DealtDamage");
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
