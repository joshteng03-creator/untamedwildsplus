package untamedwilds.entity;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import untamedwilds.init.ModEntity;
import untamedwilds.item.MountedSpecimenItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A mounted specimen (taxidermy, 2026-10-06): a museum display piece made from a killed animal.
 * <p>
 * Deliberately NOT a frozen ComplexMob: every one of those runs hunger, herd, ecology and census code
 * from its own tick. This is a plain, non-living entity holding the animal's saved data
 * ({@link #SPECIMEN}). On the client it builds an un-ticked copy of the animal from that data and
 * {@code RendererMountedSpecimen} draws the copy through the species' own renderer and model. The copy
 * never ticks, so its pose never moves: no AI, no sound, no ecology, never counted.
 * <p>
 * Right-click cycles the pose (stand, sit, sleep, then each of the species' animations held at its
 * midpoint), sneak-right-click turns it 45 degrees, a named name tag names it. Only a player breaks
 * it -- two quick hits, like an armour stand, or one in creative -- and it drops itself back as the
 * item. Explosions, fire, mobs and projectiles without a player owner do nothing.
 */
public class EntityMountedSpecimen extends Entity {

    private static final EntityDataAccessor<CompoundTag> SPECIMEN = SynchedEntityData.defineId(EntityMountedSpecimen.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> POSE_INDEX = SynchedEntityData.defineId(EntityMountedSpecimen.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FACING = SynchedEntityData.defineId(EntityMountedSpecimen.class, EntityDataSerializers.INT);
    private static final int BREAK_WINDOW = 5;

    // Client only: the un-ticked animal the renderer draws. Rebuilt whenever the data or pose changes.
    @Nullable
    private Entity displayCopy;
    private boolean displayFailed;
    private long lastHitTime;

    public EntityMountedSpecimen(EntityType<? extends EntityMountedSpecimen> type, Level level) {
        super(type, level);
    }

    public EntityMountedSpecimen(Level level, double x, double y, double z, CompoundTag specimen, int facing) {
        this(ModEntity.MOUNTED_SPECIMEN.get(), level);
        this.entityData.set(SPECIMEN, specimen);
        this.entityData.set(FACING, facing & 7);
        this.setPos(x, y, z);
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SPECIMEN, new CompoundTag());
        this.entityData.define(POSE_INDEX, 0);
        this.entityData.define(FACING, 0);
    }

    public CompoundTag getSpecimen() {
        return this.entityData.get(SPECIMEN);
    }

    public int getPoseIndex() {
        return this.entityData.get(POSE_INDEX);
    }

    public float getFacingYaw() {
        return this.entityData.get(FACING) * 45.0F;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SPECIMEN.equals(key)) {
            this.refreshDimensions();
            this.displayFailed = false;
        }
        if (SPECIMEN.equals(key) || POSE_INDEX.equals(key)) {
            this.displayCopy = null;
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        CompoundTag specimen = this.getSpecimen();
        if (specimen.contains(MountedSpecimenItem.WIDTH_KEY)) {
            return EntityDimensions.scalable(specimen.getFloat(MountedSpecimenItem.WIDTH_KEY), specimen.getFloat(MountedSpecimenItem.HEIGHT_KEY));
        }
        return super.getDimensions(pose);
    }

    @Override
    public void tick() {
        // A display piece: no movement, no gravity, nothing to update.
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level.isClientSide || this.isRemoved() || !(source.getEntity() instanceof Player player) || !player.getAbilities().mayBuild) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            long now = this.level.getGameTime();
            if (now - this.lastHitTime > BREAK_WINDOW) {
                this.lastHitTime = now;
                this.playSound(SoundEvents.ARMOR_STAND_HIT, 1.0F, 1.0F);
                return true;
            }
            this.spawnAtLocation(this.toItem());
        }
        this.playSound(SoundEvents.ARMOR_STAND_BREAK, 1.0F, 1.0F);
        this.discard();
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.NAME_TAG) && held.hasCustomHoverName()) {
            if (!this.level.isClientSide) {
                this.setCustomName(held.getHoverName());
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        }
        if (!this.level.isClientSide) {
            if (player.isShiftKeyDown()) {
                this.entityData.set(FACING, (this.entityData.get(FACING) + 1) & 7);
            } else {
                // Wraps on the client, where the species' pose count is known (see applyPose).
                this.entityData.set(POSE_INDEX, (this.getPoseIndex() + 1) & 1023);
            }
            this.playSound(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(this.level.isClientSide);
    }

    public ItemStack toItem() {
        CompoundTag specimen = this.getSpecimen().copy();
        Component name = this.getCustomName();
        if (name != null) {
            specimen.putString("CustomName", Component.Serializer.toJson(name));
        }
        ItemStack stack = MountedSpecimenItem.withSpecimen(specimen);
        return stack;
    }

    @Override
    public ItemStack getPickResult() {
        return this.toItem();
    }

    /**
     * Client only. The un-ticked animal to draw, placed and turned to match this mount; null if the
     * stored data no longer names a loadable entity (a species removed by an update, say).
     */
    @Nullable
    public Entity getDisplayCopy() {
        if (this.displayCopy == null && !this.displayFailed) {
            CompoundTag data = this.getSpecimen().copy();
            data.remove("CustomName");
            Optional<Entity> made;
            try {
                made = EntityType.create(data, this.level);
            } catch (RuntimeException e) {
                made = Optional.empty();
            }
            if (made.isEmpty()) {
                this.displayFailed = true;
                return null;
            }
            this.displayCopy = made.get();
            applyPose(this.displayCopy, this.getPoseIndex());
        }
        if (this.displayCopy != null) {
            float yaw = this.getFacingYaw();
            this.displayCopy.setPos(this.getX(), this.getY(), this.getZ());
            this.displayCopy.setYRot(yaw);
            this.displayCopy.yRotO = yaw;
            this.displayCopy.setXRot(0.0F);
            this.displayCopy.xRotO = 0.0F;
            if (this.displayCopy instanceof LivingEntity living) {
                living.yBodyRot = living.yBodyRotO = yaw;
                living.yHeadRot = living.yHeadRotO = yaw;
            }
        }
        return this.displayCopy;
    }

    /** Stand, sit and sleep for land animals, then every animation the species has, at its midpoint. */
    private static void applyPose(Entity copy, int index) {
        List<Runnable> poses = new ArrayList<>();
        poses.add(() -> {});
        if (copy instanceof ComplexMobTerrestrial land) {
            poses.add(() -> {
                land.setSitting(true);
                land.sitProgress = land.ticksToSit;
            });
            poses.add(() -> {
                land.setSleeping(true);
                land.sleepProgress = 40;
            });
        }
        if (copy instanceof IAnimatedEntity animated) {
            for (Animation animation : animated.getAnimations()) {
                if (animation != null && animation != IAnimatedEntity.NO_ANIMATION && animation.getDuration() > 0) {
                    poses.add(() -> {
                        animated.setAnimation(animation);
                        animated.setAnimationTick(animation.getDuration() / 2);
                    });
                }
            }
        }
        poses.get(Math.floorMod(index, poses.size())).run();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Specimen", this.getSpecimen());
        tag.putInt("PoseIndex", this.getPoseIndex());
        tag.putInt("Facing", this.entityData.get(FACING));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(SPECIMEN, tag.getCompound("Specimen"));
        this.entityData.set(POSE_INDEX, tag.getInt("PoseIndex"));
        this.entityData.set(FACING, tag.getInt("Facing") & 7);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
