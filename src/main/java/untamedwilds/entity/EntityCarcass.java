package untamedwilds.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.network.NetworkHooks;
import untamedwilds.UntamedWilds;
import untamedwilds.config.ConfigAmbience;
import untamedwilds.init.ModEntity;
import untamedwilds.item.MountedSpecimenItem;
import untamedwilds.util.PerfCounters;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A carcass (2.8.0, P7): what an animal killed by a predator, or dead of natural causes, leaves behind.
 * <p>
 * Like the mounted specimen it is a plain, non-living entity holding the animal's saved data -- never a frozen
 * ComplexMob, so it runs no AI, hunger, herd or census code. It holds the food the kill did not use up (the killer
 * and its pack have already eaten their share, with the same caps as before), and {@code FeedOnCarcassGoal}
 * scavengers walk to it and feed over time, capped per scavenger so the payout ceiling is unchanged. It also holds the
 * animal's LOOT: a player can butcher it once with an axe, sword or spear (or anything tagged
 * {@code untamedwilds:butchering_tools}) for the normal drops, rolled through the animal's own loot table with full
 * player context, so looting, global loot modifiers and datapacks all still apply. Then only bones remain, and they
 * fade. Animals a player kills never leave a carcass -- they drop loot exactly as before.
 */
public class EntityCarcass extends Entity {

    public static final int STAGE_FRESH = 0;
    public static final int STAGE_PICKED = 1;
    public static final int STAGE_BONES = 2;

    private static final EntityDataAccessor<CompoundTag> SPECIMEN = SynchedEntityData.defineId(EntityCarcass.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(EntityCarcass.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DECAY = SynchedEntityData.defineId(EntityCarcass.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FACING = SynchedEntityData.defineId(EntityCarcass.class, EntityDataSerializers.INT);

    public static final TagKey<Item> BUTCHERING_TOOLS = TagKey.create(net.minecraft.core.Registry.ITEM_REGISTRY,
            new ResourceLocation(UntamedWilds.MOD_ID, "butchering_tools"));

    /** Live carcasses per dimension, kept by onAddedToWorld / onRemovedFromWorld (server only). */
    private static final Map<ResourceKey<Level>, Integer> COUNT = new HashMap<>();

    // server state
    private float food;
    private float foodMax;
    private int age;
    private int bonesAge;
    private boolean predated;
    private boolean butchered;
    private int biteCap;
    private final Set<UUID> excluded = new HashSet<>();
    private final Map<UUID, Integer> eatenBy = new HashMap<>();
    private final Set<UUID> feeders = new HashSet<>();
    private boolean counted;

    // client only
    @Nullable
    private Entity displayCopy;
    private boolean displayFailed;

    public EntityCarcass(EntityType<? extends EntityCarcass> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SPECIMEN, new CompoundTag());
        this.entityData.define(STAGE, STAGE_FRESH);
        this.entityData.define(DECAY, 0);
        this.entityData.define(FACING, 0);
    }

    // ------------------------------------------------------------------ creation

    /** Spawns the carcass of {@code dead} where it fell. {@code food} = what is left for scavengers. */
    public static EntityCarcass spawnFrom(ComplexMob dead, float food, boolean predated, int biteCap) {
        EntityCarcass carcass = new EntityCarcass(ModEntity.CARCASS.get(), dead.level);
        CompoundTag data = MountedSpecimenItem.snapshot(dead);
        carcass.entityData.set(SPECIMEN, data);
        carcass.entityData.set(FACING, Math.round(dead.yBodyRot) & 0x3FF);
        carcass.moveTo(dead.getX(), dead.getY(), dead.getZ(), dead.yBodyRot, 0.0F);
        carcass.refreshDimensions();
        carcass.food = food;
        carcass.foodMax = Math.max(1F, food);
        carcass.predated = predated;
        carcass.biteCap = biteCap;
        dead.level.addFreshEntity(carcass);
        PerfCounters.inc(PerfCounters.Counter.CARCASS_SPAWN);
        carcass.enforceAreaCap();
        return carcass;
    }

    public static int countIn(Level level) {
        return COUNT.getOrDefault(level.dimension(), 0);
    }

    /** Too many carcasses close together (a mass die-off): the oldest ones go straight to bones. */
    private void enforceAreaCap() {
        int cap = ConfigAmbience.carcassAreaCap.get();
        double r = ConfigAmbience.carcassAreaRadius.get();
        List<EntityCarcass> near = this.level.getEntitiesOfClass(EntityCarcass.class, this.getBoundingBox().inflate(r),
                c -> c.isAlive() && c.getStage() < STAGE_BONES);
        if (near.size() <= cap) {
            return;
        }
        near.sort((a, b) -> Integer.compare(b.age, a.age));        // oldest first
        for (int i = 0; i < near.size() - cap; i++) {
            near.get(i).toBones();
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.level.isClientSide && !this.counted) {
            this.counted = true;
            COUNT.merge(this.level.dimension(), 1, Integer::sum);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (!this.level.isClientSide && this.counted) {
            this.counted = false;
            COUNT.merge(this.level.dimension(), -1, Integer::sum);
        }
    }

    // ------------------------------------------------------------------ state

    public CompoundTag getSpecimen() {
        return this.entityData.get(SPECIMEN);
    }

    public int getStage() {
        return this.entityData.get(STAGE);
    }

    /** 0 = fresh .. 255 = fully decayed (drives the renderer's darkening). */
    public int getDecay() {
        return this.entityData.get(DECAY);
    }

    public float getFacingYaw() {
        return this.entityData.get(FACING);
    }

    public float getFood() {
        return this.food;
    }

    public float getFoodMax() {
        return this.foodMax;
    }

    public void setFood(float food) {
        this.food = Math.max(0F, food);
        this.updateStage();
    }

    public boolean isButchered() {
        return this.butchered;
    }

    public int getAge() {
        return this.age;
    }

    /** GameTests: jump the clock. */
    public void setAgeForTests(int age) {
        this.age = age;
        this.updateStage();
    }

    public void exclude(Entity eater) {
        this.excluded.add(eater.getUUID());
    }

    public int feederCount() {
        return this.feeders.size();
    }

    private void toBones() {
        if (this.getStage() != STAGE_BONES) {
            this.entityData.set(STAGE, STAGE_BONES);
            this.food = 0F;
            this.bonesAge = 0;
            this.feeders.clear();
        }
    }

    private void updateStage() {
        if (this.level.isClientSide) {
            return;
        }
        int lifetime = Math.max(1, ConfigAmbience.carcassLifetimeTicks.get());
        if (this.butchered || this.age >= lifetime || this.food < 1F) {
            this.toBones();
        } else if (this.getStage() == STAGE_FRESH && this.food < this.foodMax * 0.5F) {
            this.entityData.set(STAGE, STAGE_PICKED);
        }
        this.entityData.set(DECAY, Math.min(255, this.age * 255 / lifetime));
    }

    // ------------------------------------------------------------------ scavenging

    /** Whether {@code eater} may feed here: food left, not the pack that already ate, not its own kind, a slot free. */
    public boolean canFeed(ComplexMob eater) {
        if (!this.isAlive() || this.getStage() >= STAGE_BONES || this.food < 1F || this.excluded.contains(eater.getUUID())) {
            return false;
        }
        if (this.eatenBy.getOrDefault(eater.getUUID(), 0) >= this.biteCap) {
            return false;
        }
        String id = this.getSpecimen().getString("id");
        if (id.equals(EntityType.getKey(eater.getType()).toString())) {
            return false;         // nothing eats its own kind here, as in feedScavengers
        }
        return this.feeders.contains(eater.getUUID()) || this.feeders.size() < ConfigAmbience.carcassMaxFeeders.get();
    }

    public boolean claimFeeder(ComplexMob eater) {
        if (!this.canFeed(eater)) {
            return false;
        }
        this.feeders.add(eater.getUUID());
        return true;
    }

    public void releaseFeeder(ComplexMob eater) {
        this.feeders.remove(eater.getUUID());
    }

    /** One bite: up to {@code max} food, never past this eater's per-carcass cap or what is left. */
    public int bite(ComplexMob eater, int max) {
        if (!this.canFeed(eater)) {
            return 0;
        }
        int eaten = this.eatenBy.getOrDefault(eater.getUUID(), 0);
        int amount = (int) Math.min(Math.min(max, this.biteCap - eaten), this.food);
        if (amount <= 0) {
            return 0;
        }
        this.food -= amount;
        this.eatenBy.put(eater.getUUID(), eaten + amount);
        PerfCounters.inc(PerfCounters.Counter.CARCASS_BITE);
        this.updateStage();
        return amount;
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            if (this.getStage() < STAGE_BONES && this.random.nextInt(12) == 0 && untamedwilds.config.ConfigClient.carcassFlies()) {
                this.level.addParticle(ParticleTypes.MYCELIUM, this.getRandomX(0.6D), this.getY() + this.getBbHeight() * (0.3D + this.random.nextDouble() * 0.7D),
                        this.getRandomZ(0.6D), 0.0D, 0.0D, 0.0D);
            }
            return;
        }
        // no physics: a carcass lies where the animal fell
        if (this.getStage() == STAGE_BONES) {
            if (++this.bonesAge >= ConfigAmbience.carcassBonesTicks.get()) {
                this.discard();
            }
            return;
        }
        this.age++;
        if (this.age % 20 == 0) {
            this.updateStage();
        }
    }

    // ------------------------------------------------------------------ butchering

    public static boolean isButcheringTool(ItemStack stack) {
        return stack.getItem() instanceof AxeItem || stack.getItem() instanceof SwordItem || stack.is(BUTCHERING_TOOLS);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || this.butchered || this.getStage() >= STAGE_BONES || !isButcheringTool(held)) {
            return InteractionResult.PASS;
        }
        if (!this.level.isClientSide && this.level instanceof ServerLevel server) {
            this.butcher(server, player, held);
        }
        return InteractionResult.sidedSuccess(this.level.isClientSide);
    }

    /** Rolls the animal's own loot table as a player kill, once. */
    public void butcher(ServerLevel server, Player player, ItemStack tool) {
        if (this.butchered) {
            return;
        }
        this.butchered = true;
        if (server.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            for (ItemStack drop : this.rollLoot(server, player)) {
                if (!drop.isEmpty()) {
                    this.spawnAtLocation(drop, 0.3F);
                }
            }
        }
        server.playSound(null, this.getX(), this.getY(), this.getZ(), untamedwilds.init.ModSounds.ENTITY_CARCASS_BUTCHER, SoundSource.PLAYERS, 0.9F, 1.0F);
        if (!player.getAbilities().instabuild) {
            tool.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
        this.toBones();
    }

    private List<ItemStack> rollLoot(ServerLevel server, Player player) {
        Optional<Entity> made;
        try {
            made = EntityType.create(this.getSpecimen().copy(), server);
        } catch (RuntimeException e) {
            made = Optional.empty();
        }
        if (made.isEmpty() || !(made.get() instanceof Mob copy)) {
            return List.of();
        }
        copy.setPos(this.getX(), this.getY(), this.getZ());
        ResourceLocation tableId = copy.getLootTable();
        LootTable table = server.getServer().getLootTables().get(tableId);
        DamageSource source = DamageSource.playerAttack(player);
        LootContext.Builder builder = new LootContext.Builder(server)
                .withRandom(this.random)
                .withParameter(LootContextParams.THIS_ENTITY, copy)
                .withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.KILLER_ENTITY, player)
                .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, player)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                .withLuck(player.getLuck());
        List<ItemStack> drops = table.getRandomItems(builder.create(LootContextParamSets.ENTITY));
        if (ConfigAmbience.carcassMeatScalesWithFood.get() && this.foodMax > 0F) {
            float left = this.predated ? Math.min(1F, this.food / this.foodMax) : 1F;
            for (ItemStack stack : drops) {
                if (stack.isEdible() && stack.getItem().getFoodProperties() != null && stack.getItem().getFoodProperties().isMeat()) {
                    stack.setCount(Math.max(this.food >= 1F ? 1 : 0, Math.round(stack.getCount() * left)));
                }
            }
        }
        return drops;
    }

    // ------------------------------------------------------------------ entity plumbing

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        CompoundTag specimen = this.getSpecimen();
        if (specimen.contains(MountedSpecimenItem.WIDTH_KEY)) {
            // lying on its side: as long as the animal is wide, as tall as half its height
            float w = specimen.getFloat(MountedSpecimenItem.WIDTH_KEY);
            float h = specimen.getFloat(MountedSpecimenItem.HEIGHT_KEY);
            return EntityDimensions.scalable(Math.max(w, h * 0.8F), Math.max(0.3F, w * 0.6F));
        }
        return super.getDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SPECIMEN.equals(key)) {
            this.refreshDimensions();
            this.displayCopy = null;
            this.displayFailed = false;
        }
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
        // only a creative player can clear one by hand; nothing else damages it
        if (!this.level.isClientSide && source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
            this.discard();
            return true;
        }
        return false;
    }

    /** Client only: the un-ticked animal to draw lying on its side; null if the stored data no longer loads. */
    @Nullable
    public Entity getDisplayCopy() {
        if (this.displayCopy == null && !this.displayFailed) {
            CompoundTag data = this.getSpecimen().copy();
            data.remove("CustomName");
            try {
                this.displayCopy = EntityType.create(data, this.level).orElse(null);
            } catch (RuntimeException e) {
                this.displayCopy = null;
            }
            if (this.displayCopy == null) {
                this.displayFailed = true;
            } else if (this.displayCopy instanceof LivingEntity living) {
                living.yBodyRot = living.yBodyRotO = 0F;
                living.yHeadRot = living.yHeadRotO = 0F;
                living.setYRot(0F);
                living.yRotO = 0F;
            }
        }
        if (this.displayCopy != null) {
            this.displayCopy.setPos(this.getX(), this.getY(), this.getZ());
        }
        return this.displayCopy;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Specimen", this.getSpecimen());
        tag.putInt("Stage", this.getStage());
        tag.putInt("Facing", this.entityData.get(FACING));
        tag.putFloat("Food", this.food);
        tag.putFloat("FoodMax", this.foodMax);
        tag.putInt("Age", this.age);
        tag.putInt("BonesAge", this.bonesAge);
        tag.putBoolean("Predated", this.predated);
        tag.putBoolean("Butchered", this.butchered);
        tag.putInt("BiteCap", this.biteCap);
        ListTag ex = new ListTag();
        for (UUID id : this.excluded) {
            ex.add(StringTag.valueOf(id.toString()));
        }
        tag.put("Excluded", ex);
        CompoundTag eaten = new CompoundTag();
        this.eatenBy.forEach((id, n) -> eaten.putInt(id.toString(), n));
        tag.put("EatenBy", eaten);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(SPECIMEN, tag.getCompound("Specimen"));
        this.entityData.set(STAGE, tag.getInt("Stage"));
        this.entityData.set(FACING, tag.getInt("Facing"));
        this.food = tag.getFloat("Food");
        this.foodMax = Math.max(1F, tag.getFloat("FoodMax"));
        this.age = tag.getInt("Age");
        this.bonesAge = tag.getInt("BonesAge");
        this.predated = tag.getBoolean("Predated");
        this.butchered = tag.getBoolean("Butchered");
        this.biteCap = tag.getInt("BiteCap");
        this.excluded.clear();
        for (net.minecraft.nbt.Tag t : tag.getList("Excluded", 8)) {
            try {
                this.excluded.add(UUID.fromString(t.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        this.eatenBy.clear();
        CompoundTag eaten = tag.getCompound("EatenBy");
        for (String key : eaten.getAllKeys()) {
            try {
                this.eatenBy.put(UUID.fromString(key), eaten.getInt(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
        this.refreshDimensions();
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
