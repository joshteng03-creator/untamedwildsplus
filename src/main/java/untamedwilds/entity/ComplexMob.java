package untamedwilds.entity;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.util.Mth;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import untamedwilds.UntamedWilds;
import untamedwilds.block.blockentity.CritterBurrowBlockEntity;
import untamedwilds.compat.CompatBridge;
import untamedwilds.compat.CompatSereneSeasons;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.config.ConfigMobControl;
import untamedwilds.config.EcologyMode;
import untamedwilds.init.ModAdvancementTriggers;
import untamedwilds.util.EcologyTags;
import untamedwilds.util.EntityDataHolder;
import untamedwilds.util.EntityDataHolderClient;
import untamedwilds.util.EntityDataListenerEvent;
import untamedwilds.util.EntityUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class ComplexMob extends TamableAnimal {

    public static HashMap<String, HashMap<Integer, ArrayList<ResourceLocation>>> TEXTURES_COMMON = new HashMap<>();
    public static HashMap<String, HashMap<Integer, ArrayList<ResourceLocation>>> TEXTURES_RARE = new HashMap<>();

    private static final EntityDataAccessor<BlockPos> HOME_POS = SynchedEntityData.defineId(ComplexMob.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(ComplexMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(ComplexMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(ComplexMob.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> GENDER = SynchedEntityData.defineId(ComplexMob.class, EntityDataSerializers.INT); // 0 - Male, 1 - Female
    private static final EntityDataAccessor<Boolean> IS_ANGRY = SynchedEntityData.defineId(ComplexMob.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> COMMAND = SynchedEntityData.defineId(ComplexMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(ComplexMob.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(ComplexMob.class, EntityDataSerializers.BOOLEAN);
    /* Moved up from ComplexMobTerrestrial. HuntMobTarget.canUse() gates hunting on hunger behind an
     * `instanceof ComplexMobTerrestrial` check, so while HUNGER lived on that subclass every aquatic
     * and amphibious predator -- shark, football fish, giant salamander -- skipped the gate entirely
     * and hunted without pause. Same NBT key ("Hunger"), so existing worlds load unchanged. */
    private static final EntityDataAccessor<Integer> HUNGER = SynchedEntityData.defineId(ComplexMob.class, EntityDataSerializers.INT);
    public HerdEntity herd = null;
    public float turn_speed = 1F;
    public int huntingCooldown;
    public int retaliationCooldown;
    /* Ticks of blind panic left. Set on every member of a herd when one of them spots a predator, so
     * the whole group stampedes together instead of each animal noticing the wolf independently. */
    public int fleeCooldown;
    /* Non-zero only while a hunt-for-food goal is actually running. huntingCooldown cannot be used for
     * this -- it stays at 6000 for five minutes AFTER the hunt ends. The low-health break-off in
     * isRouted() consults this so a predator presses a genuine hunt to the kill, while every other
     * kind of fight (territorial, retaliation, defence of young) is allowed to end with both parties
     * walking away. */
    public int activeHunt;
    /* Ticks left in the current committed hunt, and the animal it is committed against. This exists
     * because activeHunt could not do the job on its own: every break-off target goal is registered at a
     * HIGHER priority than the hunt goal, so the moment the prey hits back (or a cub strays within eight
     * blocks) the GoalSelector calls stop() on the hunt, activeHunt falls to 0, and the mercy clause
     * released the prey at exactly 10% health -- the bug this whole mechanism exists to prevent. The
     * commitment is a timer on the animal instead of a flag on a goal, so it survives being preempted.
     * Deliberately not persisted: neither huntingCooldown nor activeHunt are either, and the saved
     * Hunger value is what actually gates re-hunting across a reload. */
    public int huntCommitTicks;
    @Nullable
    public LivingEntity huntVictim;
    /* Whether the current commitment is a hunt the quarry will not let its attacker walk away from.
     * Rolled once per hunt in commitToHunt so the answer is stable for the whole fight rather than
     * flickering tick to tick, and read only by tryBreakOff. Not persisted, for the same reason
     * huntCommitTicks is not: the window is 600 ticks and a reload ends the hunt anyway. */
    public boolean huntIsRisky;
    /* Ticks of flat-out flight left before the animal is blown. Fleeing prey used to be strictly faster
     * than every predator chasing it -- a deer flees at 0.25*2.0 = 0.50 while a dire wolf pursues at
     * 0.24*1.6 = 0.384 -- so the gap never closed and no hunt could ever end in a kill. Stamina is what
     * makes a chase winnable without making prey slower in the first place: prey outruns the initial
     * rush, then tires. */
    public int fleeStamina = 200;
    /* Ticks before this animal may take a herd defender slot again. Without it a fight simply cycles a
     * fresh trio of defenders through the cap and the total damage a predator takes is unchanged. */
    public int defendCooldown;
    /* Ticks a dispersing young adult stays closed to rejoining a herd, so it does not merge straight
     * back into the one it just left. */
    public int dispersalLockout;
    /* How long this animal has been unable to find anything to eat. Driven by GrazeGoal: up when its
     * search finds nothing within 16 blocks, down on every successful mouthful. This is habitat
     * quality, and it is what tells a herd its range is spent. */
    public int forageStress;
    /* The herd that beat this predator, and how long it will keep away from it. A rout that is not
     * remembered is cosmetic -- HuntMobTarget re-acquires the same herd within seconds. */
    @Nullable
    public HerdEntity avoidedHerd;
    public int avoidHerdUntil;
    /* Body condition: a slow exponential average of hunger, 0-1. Hunger itself is far too twitchy to
     * breed off -- a single carcass pays +120 to the killer and +90 to every pack-mate within 16
     * blocks, so one kill used to put an entire eight-wolf pack over the hunger>=80 threshold at the
     * same instant. Averaging over condition_halflife_ticks means breeding tracks the RATE at which an
     * animal acquires food rather than whether it happens to be full right now, which is what couples
     * predator births to prey density. That lag is not a side effect: it is what turns exponential
     * predator growth into a bounded predator/prey oscillation.
     * Persisted, and accrued even in Zoo mode -- see EcologyMode's demography note. */
    public float condition = 0.79F;
    /* Consecutive ticks spent at zero hunger. Reset the moment anything is eaten, so an animal that
     * finds one mouthful in time pays nothing. */
    public int starveTicks;
    /* How long this animal has been alive, in ticks. The mod had no age structure at all beyond the
     * vanilla baby/adult flag, so any cohort that reached adulthood was immortal and populations could
     * only ever climb. Compared against getLifespan(). Persisted. */
    public int lifeTicks;
    public static HashMap<EntityType<?>, EntityDataHolder> ENTITY_DATA_HASH = new HashMap<>();
    public static HashMap<EntityType<?>, EntityDataHolderClient> CLIENT_DATA_HASH = new HashMap<>();

    public ComplexMob(EntityType<? extends ComplexMob> type, Level worldIn){
        super(type, worldIn);
        this.moveControl = new MoveControl(this);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HOME_POS, BlockPos.ZERO);
        this.entityData.define(VARIANT, 0);
        this.entityData.define(SKIN, 0);
        this.entityData.define(SIZE, 1F);
        this.entityData.define(GENDER, 0);
        this.entityData.define(IS_ANGRY, false);
        this.entityData.define(COMMAND, 0);
        this.entityData.define(SLEEPING, false);
        this.entityData.define(SITTING, false);
        this.entityData.define(HUNGER, 79); // One point less than the breeding threshold
    }

    public void aiStep() {
        super.aiStep();
        if (!this.level.isClientSide) {
            if (this.huntingCooldown > 0)
                this.huntingCooldown--;
            if (this.retaliationCooldown > 0)
                this.retaliationCooldown--;
            if (this.fleeCooldown > 0)
                this.fleeCooldown--;
            if (this.defendCooldown > 0)
                this.defendCooldown--;
            if (this.dispersalLockout > 0) {
                this.dispersalLockout--;
                if (this.dispersalLockout == 0 && this.herd != null) {
                    // Free to settle: re-open so it can absorb, or be absorbed by, whatever is here now.
                    this.herd.setOpenToCombine(true);
                }
            }
            if (this.avoidHerdUntil > 0 && --this.avoidHerdUntil == 0)
                this.avoidedHerd = null;
            this.tickHuntCommitment();
            this.tickHuntAbandon();
            this.tickFleeStamina();
            this.tickSurvival();
            if (this.tickCount % 600 == 0 && this.wantsToBreed()) {
                this.setInLove(null);
            }
        }
    }

    /**
     * Wrapper method to access EntityDataHolder objects, contains safeguards against accessing data before its initialization
     * @param typeIn The EntityType to access in ENTITY_DATA_HASH, or initialize it if needed
     */
    public static EntityDataHolder getEntityData(EntityType<?> typeIn) {
        if (!ENTITY_DATA_HASH.containsKey(typeIn)) {
            EntityDataListenerEvent.registerEntityData(typeIn);
        }
        return ENTITY_DATA_HASH.get(typeIn);
    }

    protected SoundEvent getAmbientSound() {
        return EntityUtils.getSound(this.getType(), this.getVariant(), "ambient");
    }

    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return EntityUtils.getSound(this.getType(), this.getVariant(), "hurt", SoundEvents.GENERIC_HURT);
    }

    protected SoundEvent getDeathSound() {
        return EntityUtils.getSound(this.getType(), this.getVariant(), "death", SoundEvents.GENERIC_DEATH);
    }

    protected SoundEvent getThreatSound() {
        return EntityUtils.getSound(this.getType(), this.getVariant(), "threat");
    }

    public boolean checkSpawnRules(LevelAccessor worldIn, MobSpawnType spawnReasonIn) {
        return true;
    }

    // Why is a method called 'checkSpawnObstruction()' also checking for Water?
    @Override
    public boolean checkSpawnObstruction(LevelReader worldIn) {
        //return !p_21433_.containsAnyLiquid(this.getBoundingBox()) && p_21433_.isUnobstructed(this);
        return worldIn.isUnobstructed(this);
    }

    @Override
    public boolean canBeLeashed(Player player) {
        if (player.isCreative()) {
            return !this.isLeashed();
        }
        return (!this.isLeashed() && this.isTame());
    }

    @Override
    public boolean removeWhenFarAway(double p_21542_) {
        return false;
    }

    public void setSleeping(boolean sleeping){ this.entityData.set(SLEEPING, sleeping); }
    public boolean isSleeping(){ return (this.entityData.get(SLEEPING)); }

    public void setSitting(boolean sitting){ this.entityData.set(SITTING, sitting); }
    public boolean isSitting(){ return (this.entityData.get(SITTING)); }

    protected void setHunger(int hunger){
        this.entityData.set(HUNGER, hunger);
    }
    public int getHunger(){
        return (this.entityData.get(HUNGER));
    }
    public boolean isStarving() { return this.getHunger() <= 0; }
    public void addHunger(int change) {
        int i = this.getHunger() + change;
        this.setHunger((i > 200) ? 200 : (Math.max(i, 0)));
    }

    /**
     * Periodic hunger decay for a carnivore, scaled by the predator_hunger_drain gamerule.
     * <p>
     * Carnivores route their decay through here and herbivores do not, because the two are not the same
     * clock: a grazer's hunger is refilled continuously by GrazeGoal, so its decay rate barely matters,
     * while a predator's decides how often it hunts and therefore how many animals a region loses per
     * day. Canids in particular shared the herbivore rate of -10, which had a pack killing roughly twice
     * an in-game day -- faster than any herd could breed back.
     * <p>
     * Pass the species' base rate as a positive number; the floor of 1 keeps the gamerule from stalling
     * hunger entirely and starving the hunt gate of its input.
     */
    public void drainHuntingHunger(int base) {
        this.addHunger(-Math.max(1, Math.round(base * (float) EcologyMode.hungerDrainScale(this))));
    }

    /* Restores hunger when a predator lands the killing blow, so HuntMobTarget.canUse() then blocks new hunts
     * until hunger decays again. Prevents predators from thinning entire herds one animal per cooldown. */
    public void satiateFromKill(Entity target) {
        if (target instanceof LivingEntity le && (le.isDeadOrDying() || le.getHealth() <= 0)) {
            /* Somebody stronger, or simply more numerous, may take this off us before we have finished
             * it. Resolved here, at the kill, rather than by polling for "is anyone feeding" every
             * tick -- the carcass is not an entity in this mod, so the kill IS the meal. */
            ComplexMob thief = this.findCarcassThief();
            if (thief != null) {
                this.loseCarcassTo(thief, target);
                return;
            }
            /* The carcass is a fixed amount of food, split between everyone eating from it. It used to
             * pay +120 to the killer and +90 to EVERY pack-mate within 16 blocks however many there were,
             * so pack size cost nothing: one kill fed a pack of twelve as fully as a pack of three, the
             * whole pack stayed in breeding condition, and packs grew without limit. The caps keep a lone
             * hunter or a small pack on decent prey exactly where it was. */
            List<ComplexMob> eaters = new ArrayList<>();
            if (this.herd != null) {
                for (ComplexMob member : this.herd.creatureList) {
                    if (member != this && member.isAlive() && member.distanceToSqr(target) < 256D) {
                        eaters.add(member);
                    }
                }
            }
            int perHealth = ConfigGamerules.carcassFoodPerHealth.get();
            float share = perHealth <= 0 ? Float.MAX_VALUE : le.getMaxHealth() * perHealth / (1 + eaters.size());
            this.addHunger((int) Math.min(120F, share));
            /* The full lockout is paid HERE, on the kill, and no longer when the hunt starts. Charging it
             * at the start meant any interruption -- prey breaking line of sight, a pack-mate's fight
             * stealing the target slot -- locked the predator out for five minutes while the animal it
             * had nearly killed healed back to full. A failed attempt now costs only
             * predator_failed_hunt_cooldown; a successful one costs the full price. */
            this.huntingCooldown = ConfigGamerules.predatorKillCooldown.get();
            this.endHuntCommitment();
            /* A pack shares the carcass. Only the animal that landed the killing blow used to be fed,
             * so seven of a dire wolf pack's eight members got nothing out of a hunt they all ran --
             * they stayed permanently below the hunger>=80 breeding threshold while the herds they
             * hunted grazed back to full in seconds. Pack-mates near the kill now eat too, which is
             * both how real pack predators work and what makes their populations sustainable. */
            for (ComplexMob member : eaters) {
                member.addHunger((int) Math.min(90F, share));
                // Fed off this carcass, so the hunt is over for them too -- otherwise a pack-mate
                // that ate but never committed rolls straight into a second animal.
                member.huntingCooldown = ConfigGamerules.predatorKillCooldown.get();
                member.endHuntCommitment();
            }
            this.shareCarcassWithScavengers(target);
            if (UntamedWilds.DEBUG) {
                UntamedWilds.LOGGER.info("{} killed {}, hunger now {}, locked out for {} ticks", this.getName().getString(), target.getName().getString(), this.getHunger(), this.huntingCooldown);
            }
        }
    }
    /* Fraction of max health below which an animal gives up a fight and runs. Fights used to only ever
     * end in a death, which is what made a single scuffle wipe out both herds. */
    public static final float ROUT_THRESHOLD = 0.1F;
    /** Fraction of max health below which an animal is hurt badly enough that it cannot run flat out. */
    public static final float WOUNDED_THRESHOLD = 0.25F;
    /** No amount of exhaustion and injury slows an animal below this fraction of its flight speed. */
    private static final float MIN_FLEE_FACTOR = 0.45F;
    /** Share of the adult ecological level a juvenile is worth. See getEcoLevel. */
    private static final float BABY_ECO_FACTOR = 0.3F;
    /* Identity of the one-attack-long ATTACK_DAMAGE modifier used by every predator damage bonus. A
     * fixed UUID, because removeModifier keys off it and a fresh one per attack would leak. */
    private static final UUID ATTACK_BONUS_UUID = UUID.fromString("7a5f3c11-2b64-4d0e-9c7a-1f8e05d3b642");

    /**
     * Damage bonus for pressing a hunt this animal is committed to. Zero in a brawl, in defence of young,
     * and in retaliation -- the predator gets stronger at the one thing it was failing at, and nothing
     * that was not already its prey has any new reason to fear it.
     * <p>
     * The counterpart of {@code predator_chase_burst}, which does exactly this for pursuit speed.
     * <p>
     * The burst is SHARED across the pack-mates actually in the fight, because it was granted to each
     * hunter independently while {@code HuntPackMobTarget} gives the whole pack one shared victim -- so a
     * pack of eight delivered eight times it, 8 x (9 x 2.0) = 144 damage per second, against a herd
     * capped at {@code herd_defender_cap} x 7 = 21. A lone hunter still gets the full multiplier: a
     * solitary cat or bear was never the problem, and sharing must not punish it for the pack's sins.
     */
    protected double getHuntDamageBonus(@Nullable Entity target) {
        if (!(target instanceof LivingEntity victim) || !this.isCommittedTo(victim)) {
            return 0.0D;
        }
        double raw = ConfigGamerules.predatorHuntDamageBurst.get() - 1.0D;
        double sharing = ConfigGamerules.packBurstSharing.get();
        if (sharing <= 0.0D || raw <= 0.0D) {
            return raw;
        }
        return raw / Math.pow(this.countPackCommittedTo(victim), sharing);
    }

    /**
     * Pack-mates -- this animal included -- committed to the same victim and close enough to be landing
     * blows on it. Always at least 1.
     * <p>
     * Deliberately measured against the VICTIM rather than against this animal, unlike
     * {@link #countNearbyPack()}: what the shared burst has to divide is the number of mouths on the one
     * carcass, and two wolves twelve blocks apart can both be biting the animal standing between them.
     */
    public int countPackCommittedTo(LivingEntity victim) {
        if (this.herd == null) {
            return 1;
        }
        int count = 0;
        for (ComplexMob member : this.herd.creatureList) {
            if (member.isAlive() && !member.isBaby() && member.isCommittedTo(victim)
                    && member.distanceToSqr(victim) < ENGAGEMENT_RADIUS * ENGAGEMENT_RADIUS) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    /**
     * Adds every damage bonus this attack has earned, as a single transient modifier.
     * <p>
     * ALWAYS pair this with {@link #clearAttackBonus()} in a finally block. The bonus must not outlive
     * the attack that created it: {@link #getEcoLevel} reads the ATTACK_DAMAGE attribute directly, so a
     * modifier left in place would silently raise this animal's ecological level and change both what it
     * hunts and what runs from it -- the one thing the whole predator rebalance is built to avoid.
     * <p>
     * Bonuses are summed by the caller rather than multiplied because MULTIPLY_TOTAL adds its amounts:
     * a committed lion landing an ambush is x(1 + 1.00 + 0.75), not x1.75 x 2.00.
     */
    protected void applyAttackBonus(double bonus) {
        if (bonus <= 0) {
            return;
        }
        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.addTransientModifier(new AttributeModifier(ATTACK_BONUS_UUID, "Predator attack bonus",
                    bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    /** Tears down {@link #applyAttackBonus}. Safe to call when no bonus was applied. */
    protected void clearAttackBonus() {
        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(ATTACK_BONUS_UUID);
        }
    }

    /**
     * Lands a charge, with {@code herbivore_charge_multiplier} applied. The herbivore counterpart of
     * {@code predator_hunt_damage_burst}, and the answer to it: a bison telegraphs a charge for 50 ticks
     * and then landed a bare 7 damage, against a dire wolf's 18-damage bite with no wind-up at all.
     * <p>
     * Lives here rather than in {@link untamedwilds.entity.ai.MeleeAttackCharger} so that
     * {@link #applyAttackBonus}/{@link #clearAttackBonus} stay package-private to the entity classes --
     * the pairing is the one part of this that must never be got wrong, and every other call site of it
     * is an entity's own {@code doHurtTarget}. The {@code finally} is mandatory: a modifier that outlives
     * the attack raises ATTACK_DAMAGE, which {@link #getEcoLevel} reads, silently changing both what
     * hunts this animal and what runs from it.
     */
    public boolean doChargeHurtTarget(Entity target) {
        this.applyAttackBonus(ConfigGamerules.herbivoreChargeMultiplier.get() - 1.0D);
        try {
            return this.doHurtTarget(target);
        } finally {
            this.clearAttackBonus();
        }
    }

    /**
     * Multiplier applied to all incoming damage. 1.0 is no reduction.
     * <p>
     * This exists instead of simply giving the thin-skinned predators more health, because MAX_HEALTH is
     * an input to {@link #getEcoLevel} and damage reduction is not. Raising a dire wolf from 34 to 55 HP
     * lifts its pack's eco level from 15.0 to 16.9, which brings seven prey species into range past the
     * diet filter -- giraffe and cuvieronius among them. Reduction buys the identical survivability and
     * re-sorts nothing.
     * <p>
     * ARMOR is not an alternative either: MC reduces by {@code max(armor/5, armor - damage/2)/25}, so the
     * bear's 4 armor against an 8-damage bite is worth 3.2%.
     */
    protected float getIncomingDamageFactor(DamageSource source) {
        return 1F;
    }

    /* Applied here on the base class so every animal inherits it exactly once. ComplexMob did not
     * previously override hurt(); ComplexMobTerrestrial does, and every subclass that overrides it chains
     * to super, so this sits at the bottom of the chain and cannot be applied twice. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        float factor = this.getIncomingDamageFactor(source);
        /* A solitary hunter faces the herd's whole defender allowance alone -- three bison, not three
         * split across eight wolves -- which is why the predators that kept dying were the lone cats and
         * bears. It gets the pack's protection while it is actually hunting, and never more than the best
         * single reduction on offer. */
        if (this.isCarnivore() && this.huntCommitTicks > 0 && isPreyOrDefenderDamage(source) && this.countNearbyPack() <= 1) {
            factor = Math.min(factor, 1F - ConfigGamerules.loneHunterDamageReduction.get().floatValue());
        }
        return super.hurt(source, factor >= 1F ? amount : amount * factor);
    }

    /**
     * Damage from something a hunter is up against: an animal that is not itself a carnivore. Excludes
     * players, other predators, and everything with no attacker at all (starvation, falls, fire).
     */
    protected static boolean isPreyOrDefenderDamage(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player)
                && !(attacker instanceof ComplexMob animal && animal.isCarnivore());
    }

    /**
     * Where a pack hunter's thick-skin reduction applies: against prey and defenders, and against players
     * as before. Not in fights with other predators -- the flat reduction made packs win every predator
     * brawl -- and not against damage with no attacker, where it let packs outlast every famine.
     */
    protected static boolean appliesPackHunterReduction(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof ComplexMob animal && animal.isCarnivore());
    }

    /**
     * Too badly hurt to keep fighting.
     * <p>
     * A predator on a hunt used to be exempt from this outright, which had three consequences that
     * together wiped whole predator species out. It could never break off, so it fought every hunt to
     * the death. Its quarry could never see it as routed either -- {@code isRouted(foe)} was the prey's
     * mercy clause -- so nothing ever spared a beaten hunter. And {@link #isPressingRiskyHunt} was
     * therefore unreachable: it is only consulted after {@code isRouted(foe)} passes, which requires the
     * hunter NOT to be hunting, while {@code isCommittedTo} requires that it is. The two conditions are
     * mutually exclusive, so {@code predator_hunt_risk_chance} did nothing whatsoever and every single
     * hunt behaved as maximally risky.
     * <p>
     * The exemption is now conditional on the risk roll, which is what that roll was always for. A
     * hunter on an ordinary hunt gives up at {@link #getHunterBreakThreshold()} -- higher than
     * {@link #ROUT_THRESHOLD}, so it walks away wounded rather than at death's door -- and only a hunt
     * rolled risky is still pressed to the kill regardless.
     */
    public boolean isRouted() {
        if (this.isHunting()) {
            return !this.huntIsRisky && this.getHealth() < this.getMaxHealth() * getHunterBreakThreshold();
        }
        return this.getHealth() < this.getMaxHealth() * ROUT_THRESHOLD;
    }

    /** Fraction of max health at which a predator abandons a hunt it was not committed to dying for. */
    public static float getHunterBreakThreshold() {
        return ConfigGamerules.predatorHuntAbandonHealth.get().floatValue();
    }

    /** Static form, so the same rule can be applied to a non-ComplexMob opponent. */
    public static boolean isRouted(LivingEntity entity) {
        if (entity instanceof ComplexMob mob) {
            return mob.isRouted();
        }
        // Players are never routed; they decide for themselves when to disengage.
        return !(entity instanceof Player) && entity.getHealth() < entity.getMaxHealth() * ROUT_THRESHOLD;
    }

    /** Breaks off the current fight and sets the animal running. */
    public void routFromCombat() {
        this.setTarget(null);
        this.setLastHurtByMob(null);
        this.fleeCooldown = 200;
    }

    /** Running a hunt for food, either as the goal that owns the target slot or as a live commitment. */
    public boolean isHunting() {
        return this.activeHunt > 0 || this.huntCommitTicks > 0;
    }

    /** Committed to killing this particular animal, as opposed to merely fighting it. */
    public boolean isCommittedTo(@Nullable LivingEntity foe) {
        return foe != null && this.huntCommitTicks > 0 && foe == this.huntVictim;
    }

    /**
     * Starts the commitment window against one victim. Called by the hunt target goals.
     * <p>
     * An existing commitment against the same animal is deliberately NOT extended: a hunt goal that is
     * preempted and then re-acquires its victim would otherwise refresh the timer on every handover and
     * chase forever, which is precisely the unbounded pursuit this window exists to prevent.
     */
    public void commitToHunt(@Nullable LivingEntity victim) {
        if (victim == null || this.isCommittedTo(victim)) {
            return;
        }
        this.huntVictim = victim;
        this.huntCommitTicks = ConfigGamerules.predatorHuntCommitTicks.get();
        /* Decide up front whether this is a hunt the quarry fights to finish. Rolling once, here, rather
         * than each time tryBreakOff runs keeps a single fight from flickering between mercy and none;
         * megafauna are twice as likely to be the hunt that goes wrong, which is what makes taking on a
         * mammoth a different proposition from taking on a deer. */
        float risk = EcologyMode.huntRiskChance(this);
        if (EcologyTags.getTags(victim).contains(EcologyTags.MEGAFAUNA)) {
            risk *= 2F;
        }
        this.huntIsRisky = this.random.nextFloat() < risk;
        if (UntamedWilds.DEBUG) {
            UntamedWilds.LOGGER.info("{} committed to hunting {} for {} ticks{}", this.getName().getString(), victim.getName().getString(), this.huntCommitTicks, this.huntIsRisky ? " (RISKY - quarry will not spare it)" : "");
        }
    }

    /** Ends the commitment without charging a failure cooldown -- the hunt concluded on its own terms. */
    public void endHuntCommitment() {
        this.huntCommitTicks = 0;
        this.huntVictim = null;
        this.huntIsRisky = false;
    }

    /* The commitment is a wall clock, so a predator cannot chase one animal forever: 30 seconds of
     * pressing the kill, then it gives up and pays the (much shorter) failed-hunt cooldown. That bound is
     * what keeps "predators actually finish their prey" from turning into "nothing ever escapes". */
    private void tickHuntCommitment() {
        if (this.huntCommitTicks <= 0) {
            return;
        }
        if (this.huntVictim != null && (!this.huntVictim.isAlive() || this.huntVictim.isRemoved())) {
            this.endHuntCommitment();
            return;
        }
        if (--this.huntCommitTicks <= 0) {
            if (UntamedWilds.DEBUG) {
                UntamedWilds.LOGGER.info("{} gave up its hunt, waiting {} ticks", this.getName().getString(), ConfigGamerules.predatorFailedHuntCooldown.get());
            }
            this.endHuntCommitment();
            this.huntingCooldown = Math.max(this.huntingCooldown, ConfigGamerules.predatorFailedHuntCooldown.get());
        }
    }

    /**
     * Gives up a hunt the predator is losing, and makes the abandonment stick.
     * <p>
     * Letting {@link #isRouted()} return true is not on its own enough to end a hunt:
     * {@code HuntMobTarget.canContinueToUse} re-acquires {@code huntVictim} for as long as the
     * commitment is live and ignores both the cooldown and follow range while doing it, so a predator
     * that ran away would turn straight back round on the next tick. The commitment has to be torn
     * down explicitly.
     * <p>
     * This lives on the mob rather than in {@code RetreatWhenRoutedGoal} so that it covers every
     * predator in the mod, not only the ones that happen to register that goal.
     */
    private void tickHuntAbandon() {
        if (!this.isHunting() || this.huntIsRisky || this.fleeCooldown > 0) {
            return;
        }
        if (this.getHealth() >= this.getMaxHealth() * getHunterBreakThreshold()) {
            return;
        }
        if (UntamedWilds.DEBUG) {
            UntamedWilds.LOGGER.info("{} broke off its hunt at {}/{} health and fled", this.getName().getString(), this.getHealth(), this.getMaxHealth());
        }
        this.endHuntCommitment();
        this.huntingCooldown = Math.max(this.huntingCooldown, ConfigGamerules.predatorFailedHuntCooldown.get());
        this.routFromCombat();
    }

    /* Burned while the animal is panicking, recovered at half rate while it is not. */
    private void tickFleeStamina() {
        int max = ConfigGamerules.preyFleeStaminaTicks.get();
        if (this.fleeCooldown > 0) {
            if (this.fleeStamina > 0) {
                this.fleeStamina--;
            }
        } else if (this.fleeStamina < max && this.tickCount % 2 == 0) {
            this.fleeStamina++;
        }
        if (this.fleeStamina > max) {
            this.fleeStamina = max;
        }
    }

    /**
     * Multiplier applied to every flight speed this animal is given. A fresh, healthy animal runs at
     * full speed and outruns anything hunting it; a blown or badly wounded one does not. Applied in
     * {@link untamedwilds.entity.ai.SmartAvoidGoal}, so every flight goal inherits it.
     */
    public float getFleeSpeedFactor() {
        float factor = 1F;
        if (ConfigGamerules.preyFleeStaminaTicks.get() > 0 && this.fleeStamina <= 0) {
            factor *= ConfigGamerules.preyExhaustedFleeFactor.get().floatValue();
        }
        if (this.getHealth() < this.getMaxHealth() * WOUNDED_THRESHOLD) {
            factor *= ConfigGamerules.preyWoundedFleeFactor.get().floatValue();
        }
        return Math.max(factor, MIN_FLEE_FACTOR);
    }

    /**
     * The single break-off rule, shared by every non-hunt target goal. Returns true when the fight
     * should end here, having already sent the animal running.
     * <p>
     * A predator committed to a food hunt never breaks off -- neither because it is hurt nor, crucially,
     * because its prey is. Six goals each carried their own copy of this check, and every one of them
     * spared the prey at 10% health the instant a higher-priority goal preempted the hunt, which is why
     * predators hunted endlessly and never killed anything.
     */
    public static boolean tryBreakOff(ComplexMob fighter, @Nullable LivingEntity foe) {
        if (fighter.isCommittedTo(foe)) {
            return false;
        }
        if (fighter.isRouted()) {
            fighter.routFromCombat();
            return true;
        }
        if (foe != null && !fighter.isHunting() && isRouted(foe) && !isPressingRiskyHunt(foe, fighter)) {
            fighter.routFromCombat();
            return true;
        }
        return false;
    }

    /**
     * True when {@code foe} is a predator mid-commitment against {@code fighter} on a hunt that was
     * rolled risky, and so must not be spared at low health.
     * <p>
     * This is the only route by which a predator dies of hunting. A hunter is already exempt from being
     * routed itself while committed, so it never disengages -- but its quarry always did, the instant the
     * hunter dropped below the rout threshold. The result was that hunting carried no risk whatsoever: a
     * predator could lose every fight it ever picked and still walk away from all of them.
     * <p>
     * The share of hunts this affects is predator_hunt_risk_chance; the share that actually kill the
     * predator is much smaller, since it still has to lose the fight inside the commitment window.
     */
    private static boolean isPressingRiskyHunt(LivingEntity foe, ComplexMob fighter) {
        return foe instanceof ComplexMob hunter && hunter.huntIsRisky && hunter.isCommittedTo(fighter);
    }

    // ---------------------------------------------------------------------------------------------
    // Assessing the odds. A predator that only ever asks "how hurt am I?" walks into fights it cannot
    // win and stays in them; these are the questions it should be asking instead.
    // ---------------------------------------------------------------------------------------------

    /** How far around itself an animal looks when counting who is in the fight. */
    private static final double ENGAGEMENT_RADIUS = 12D;

    /**
     * Adults currently squared up against this animal or one of its pack-mates.
     * <p>
     * This is the number that was killing predators. A herd defends with everything it has within reach
     * of a calf, and a dire wolf is 34 HP against bison at attack 7 -- ten defenders land roughly fifty
     * damage per round. Counting them lets a predator decline the fight before it starts.
     */
    public int countEngagedDefenders() {
        int count = 0;
        for (Mob candidate : this.level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(ENGAGEMENT_RADIUS))) {
            if (candidate == this || candidate.isBaby() || !candidate.isAlive()) {
                continue;
            }
            LivingEntity theirTarget = candidate.getTarget();
            if (theirTarget == this || (theirTarget instanceof ComplexMob mate && this.isPackMate(mate))) {
                count++;
            }
        }
        return count;
    }

    /** Pack-mates close enough to actually be in the same fight. Always at least this animal itself. */
    public int countNearbyPack() {
        if (this.herd == null) {
            return 1;
        }
        int count = 0;
        for (ComplexMob member : this.herd.creatureList) {
            if (member.isAlive() && !member.isBaby() && member.distanceToSqr(this) < ENGAGEMENT_RADIUS * ENGAGEMENT_RADIUS) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    public boolean isPackMate(ComplexMob other) {
        return this.herd != null && other.herd == this.herd;
    }

    /**
     * Whether the odds are good enough to start a hunt here. Consulted only when a hunt BEGINS -- an
     * animal already committed is governed by the commitment timer and by its herd's morale, or a
     * predator would abandon its quarry the instant the first defender turned up, which is every hunt.
     */
    public boolean oddsAreAcceptable() {
        double ratio = ConfigGamerules.predatorOddsRatio.get();
        if (ratio <= 0) {
            return true;
        }
        return this.countEngagedDefenders() <= this.countNearbyPack() * ratio;
    }

    /** Records that {@code foe}'s herd beat this animal, so it leaves that herd alone for a while. */
    public void rememberDefeat(@Nullable LivingEntity foe) {
        int ticks = ConfigGamerules.predatorRoutAvoidTicks.get();
        if (ticks <= 0 || !(foe instanceof ComplexMob beaten) || beaten.herd == null) {
            return;
        }
        this.avoidedHerd = beaten.herd;
        this.avoidHerdUntil = ticks;
    }

    /** True while this animal is still steering clear of the herd that last drove it off. */
    public boolean isAvoiding(LivingEntity candidate) {
        return this.avoidHerdUntil > 0 && this.avoidedHerd != null
                && candidate instanceof ComplexMob prey && prey.herd == this.avoidedHerd;
    }

    /* One tick's worth of "how many of each animal are around me", so the target sorter can prefer
     * whichever prey species is locally common without re-scanning the world for every comparison.
     * Rebuilt at most once per tick, and only ever on demand. */
    private final HashMap<EntityType<?>, Integer> preyCensus = new HashMap<>();
    private int preyCensusTick = -1;
    /** Radius over which prey abundance is judged when a predator picks what to hunt. */
    private static final double CENSUS_RADIUS = 32D;

    /** How many living adults of {@code type} are near this animal right now. */
    public int censusOf(EntityType<?> type) {
        if (this.tickCount != this.preyCensusTick) {
            this.preyCensusTick = this.tickCount;
            this.preyCensus.clear();
            for (LivingEntity nearby : this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(CENSUS_RADIUS, 16D, CENSUS_RADIUS))) {
                if (nearby.isAlive() && !nearby.isBaby()) {
                    this.preyCensus.merge(nearby.getType(), 1, Integer::sum);
                }
            }
        }
        return this.preyCensus.getOrDefault(type, 0);
    }

    /** Damage taken from something this animal is not hunting -- a defender. Feeds the group's morale. */
    public void reportDefenderPressure(int amount) {
        if (this.herd != null) {
            this.herd.addPressure(amount);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Range quality. Crowding and starvation are reasons to MOVE, not reasons to stop breeding -- see
    // HerdEntity.wantsToMigrate(). Only isRangeExhausted() ever touches reproduction.
    // ---------------------------------------------------------------------------------------------

    public void addForageStress(int amount) {
        this.forageStress = Mth.clamp(this.forageStress + amount, 0, ConfigGamerules.forageStressTicks.get() * 2);
    }

    public void clearForageStress() {
        this.forageStress = 0;
    }

    /** Has gone hungry long enough that the ground it is standing on is not worth staying on. */
    public boolean isForageStressed() {
        int limit = ConfigGamerules.forageStressTicks.get();
        return limit > 0 && this.forageStress >= limit;
    }

    /**
     * More of this animal's own species nearby than the range will carry.
     * <p>
     * Scoped to type AND variant, which is what the old {@code hardcoreBreeding} overcrowding clause got
     * wrong -- it counted every LivingEntity within 6 blocks, so a herd standing next to a flock of
     * chickens counted as overcrowded and the clause had to be deleted.
     */
    public boolean isLocallyOvercrowded() {
        if (this.herd == null || !EcologyMode.allowsMigration(this)) {
            return false;
        }
        /* A crowding_radius box (97 blocks wide at the default 48) scanned from wantsToMigrate, pickDisperser,
         * the predator breeding gate and twice per candidate in SmartMateGoal. Local density does not change
         * on a tick timescale, so reuse the answer for OVERCROWDING_CACHE_TICKS. */
        long now = this.level.getGameTime();
        if (now - this.overcrowdedCheckedAt < OVERCROWDING_CACHE_TICKS) {
            return this.overcrowdedCached;
        }
        this.overcrowdedCheckedAt = now;
        this.overcrowdedCached = this.countOvercrowded();
        return this.overcrowdedCached;
    }

    private static final int OVERCROWDING_CACHE_TICKS = 100;
    private long overcrowdedCheckedAt = Long.MIN_VALUE / 2;
    private boolean overcrowdedCached;

    private boolean countOvercrowded() {
        double radius = ConfigGamerules.crowdingRadius.get();
        int capacity = (int) Math.ceil(this.herd.getMaxSize() * ConfigGamerules.localCapacityFactor.get());
        int neighbours = 0;
        /* Carnivore young count against capacity: a cub eats as much of the range's prey as the adult it is
         * about to become, and counting adults only let a pack sit at capacity with a dozen pups on the way.
         * Herbivore calves still do not, because for herds crowding means MOVE, not stop breeding. */
        boolean countYoung = this.isCarnivore();
        for (ComplexMob other : this.level.getEntitiesOfClass(ComplexMob.class, this.getBoundingBox().inflate(radius, 16D, radius))) {
            if (other.getType() == this.getType() && other.getVariant() == this.getVariant() && (countYoung || !other.isBaby()) && other.isAlive()) {
                neighbours++;
                if (neighbours > capacity) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Starving AND with nowhere better to go. The single, last-resort clause that suppresses breeding.
     * <p>
     * Ordinary crowding deliberately does not appear here. Capping births would stop the population
     * growing at all; what real ungulates do is leave, which is what {@code HerdMigrationGoal} and herd
     * dispersal are for. Reduced fecundity is what happens only when emigration has already failed.
     */
    public boolean isRangeExhausted() {
        if (!EcologyMode.allowsMigration(this)) {
            return false;
        }
        return this.isForageStressed() && this.herd != null && this.herd.migrationFailed;
    }

    // ---------------------------------------------------------------------------------------------
    // Demography: condition, starvation and senescence.
    //
    // Everything above this point models behaviour. None of it could bound a population, because the
    // mod had no death that was not inflicted by another animal: isStarving() was read in some
    // twenty-five places and its only consequence anywhere was skipping a heal, removeWhenFarAway()
    // returns false, and nothing aged. A wild adult could only ever be removed from the world by a
    // player. This section is the density-dependent mortality and the demographic turnover that
    // makes an equilibrium possible at all.
    //
    // Gated by EcologyMode, but note the split: the COUNTERS run in both modes and the CONSEQUENCES
    // do not. A Zoo world that is flipped to Ecosystem must not discover that every animal alive has
    // a condition of 0 and a brand new lifespan.
    // ---------------------------------------------------------------------------------------------

    /** How often the condition average and the survival penalties are re-evaluated. */
    private static final int CONDITION_INTERVAL = 20;
    /** Hunger that counts as a full belly for condition purposes. Hunger itself caps at 200. */
    private static final float CONDITION_FULL_HUNGER = 100F;
    /* Identity of the MOVEMENT_SPEED penalty carried by a starving or elderly animal. Fixed, because
     * removeModifier keys off it. Speed is used deliberately: MAX_HEALTH and ATTACK_DAMAGE are the two
     * inputs to getEcoLevel, so expressing weakness through either would silently re-sort every
     * targeting predicate in the mod. See the note on applyAttackBonus. */
    private static final UUID CONDITION_PENALTY_UUID = UUID.fromString("d41c9b07-58e6-4a92-b0f4-2c6d8e1a37b5");

    /**
     * One tick of the demographic clocks. Called from {@link #aiStep()}, which every one of the mod's
     * entity classes reaches through {@code super.aiStep()}, so this is the one hook that covers the
     * whole roster.
     */
    private void tickSurvival() {
        this.lifeTicks++;
        if (this.getHunger() > 0) {
            this.starveTicks = 0;
        } else {
            this.starveTicks++;
        }
        if (this.tickCount % CONDITION_INTERVAL == 0) {
            this.tickCondition();
            this.tickConditionPenalty();
            this.tickStarvation();
        }
        /* Deliberately on the mob's own tickCount and not on level.getGameTime(), which is what the
         * per-species hunger blocks use: a world-time modulo fires every animal in the world on the
         * same tick, and this roll would land on top of that spike. */
        if (this.tickCount % 1000 == 0) {
            this.tickSenescence();
        }
    }

    /**
     * Moves body condition toward current hunger. An exponential average, so
     * {@code condition_halflife_ticks} is literally the time it takes to cover half the gap.
     */
    private void tickCondition() {
        float target = Mth.clamp(this.getHunger() / CONDITION_FULL_HUNGER, 0F, 1F);
        int halflife = ConfigGamerules.conditionHalflifeTicks.get();
        float alpha = halflife <= 0 ? 1F : 1F - (float) Math.pow(0.5D, (double) CONDITION_INTERVAL / halflife);
        this.condition = Mth.clamp(this.condition + (target - this.condition) * alpha, 0F, 1F);
    }

    /** Adds or removes the movement penalty for an animal in poor condition or past its prime. */
    private void tickConditionPenalty() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean wanted = EcologyMode.allowsConditionPenalty(this) && !this.isTame()
                && (this.condition < ConfigGamerules.conditionWeakThreshold.get() || this.isSenescent());
        boolean present = speed.getModifier(CONDITION_PENALTY_UUID) != null;
        if (wanted && !present) {
            speed.addTransientModifier(new AttributeModifier(CONDITION_PENALTY_UUID, "Poor condition",
                    ConfigGamerules.starvedSpeedFactor.get() - 1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!wanted && present) {
            speed.removeModifier(CONDITION_PENALTY_UUID);
        }
    }

    /**
     * Death by hunger, after a grace period. This is the density-dependent mortality the whole
     * simulation was missing, and the direct answer to pack hunters that accumulated forever.
     * <p>
     * Babies are exempt because they physically cannot feed themselves: {@code GrazeGoal.canUse()}
     * refuses to run for {@code isBaby()}, so without this every calf in the world would starve. Treat
     * them as nursing. Tamed animals are exempt so that nobody's pet dies quietly while they are away.
     * <p>
     * The damage is ordinary damage, so it lowers health, which lowers {@link #getEcoLevel} and feeds
     * the straggler bias in {@code HuntMobTarget.Sorter} -- animals in poor condition get taken first,
     * through machinery that already existed. That is intended, not a side effect.
     */
    private void tickStarvation() {
        if (!EcologyMode.allowsStarvationDeath(this) || this.isBaby() || this.isTame() || this.isDeadOrDying()) {
            return;
        }
        int interval = ConfigGamerules.starvationDamageInterval.get();
        float damage = ConfigGamerules.starvationDamage.get().floatValue();
        if (damage <= 0 || this.starveTicks <= ConfigGamerules.starvationGraceTicks.get()) {
            return;
        }
        if ((this.starveTicks - ConfigGamerules.starvationGraceTicks.get()) % interval < CONDITION_INTERVAL) {
            this.hurt(DamageSource.STARVE, damage);
            if (UntamedWilds.DEBUG) {
                UntamedWilds.LOGGER.info("{} is starving, {} ticks at zero hunger, {}/{} health", this.getName().getString(), this.starveTicks, this.getHealth(), this.getMaxHealth());
            }
        }
    }

    /**
     * Natural lifespan, derived from the time this species takes to reach adulthood rather than
     * authored per species.
     * <p>
     * That keeps the maturity-to-lifespan ratio identical for every animal in the mod and makes the
     * value inherit {@code cycle_length}, {@code maturity_multiplier} and the carnivore
     * {@code predator_maturity_multiplier} for free -- so none of the ~40 entity JSONs need a new field,
     * and a world that has retuned its breeding rates gets a consistent lifespan without further work.
     */
    public int getLifespan() {
        return (int) Math.min(Integer.MAX_VALUE, (long) (this.getAdulthoodTime() * ConfigGamerules.lifespanMultiplier.get()));
    }

    /**
     * Past its prime: too old to breed, slower than it was, and preferentially taken by predators
     * through {@code HuntMobTarget.Sorter.conditionBias}.
     * <p>
     * Returns false outright in Zoo mode and for tamed animals, so every caller inherits both
     * exemptions without repeating them.
     */
    public boolean isSenescent() {
        if (!EcologyMode.allowsSenescence(this) || this.isTame() || this.isBaby()) {
            return false;
        }
        return this.lifeTicks >= this.getLifespan() * ConfigGamerules.senescenceFraction.get();
    }

    /**
     * Death by old age, rolled once per 1000 ticks and ramping from zero at the full lifespan to the
     * configured chance at one and a half times it, so a cohort spread over a range of ages does not
     * all drop at once.
     * <p>
     * Kills by dealing damage rather than by discarding the entity, so {@link #die} runs and the
     * carcass pays out to scavengers -- which is exactly the case the comment on {@link #feedScavengers}
     * anticipated and nothing could previously produce.
     */
    private void tickSenescence() {
        if (!EcologyMode.allowsSenescence(this) || this.isTame() || this.isBaby() || this.isDeadOrDying()) {
            return;
        }
        int lifespan = this.getLifespan();
        if (lifespan <= 0 || this.lifeTicks < lifespan) {
            return;
        }
        double excess = Mth.clamp((this.lifeTicks - lifespan) / (lifespan * 0.5D), 0D, 1D);
        double chance = ConfigGamerules.oldAgeMortalityChance.get() * excess;
        if (chance > 0 && this.random.nextDouble() < chance) {
            if (UntamedWilds.DEBUG) {
                UntamedWilds.LOGGER.info("{} died of old age at {} ticks (lifespan {})", this.getName().getString(), this.lifeTicks, lifespan);
            }
            this.hurt(DamageSource.GENERIC, this.getMaxHealth() * 4F);
        }
    }

    /** Has been well fed for long enough to raise young, rather than merely having just eaten. */
    public boolean isInGoodCondition() {
        return this.condition >= ConfigGamerules.breedingConditionThreshold.get();
    }

    /**
     * The shared carnivore breeding rule.
     * <p>
     * The old predicate was a bare {@code getHunger() >= 80}, which is why predator populations ran
     * away: a kill pays +120 to the killer and +90 to every pack-mate within 16 blocks, so a single
     * carcass flipped an entire pack into breeding simultaneously. Births tracked kill EVENTS instead
     * of prey DENSITY, which is exponential growth by construction. Gating on sustained condition makes
     * the response lag the food supply, and the crowding term supplies the intraspecific competition
     * that actually bounds carnivore density in the field -- note the deliberate asymmetry with
     * herbivores, for whom crowding means MOVE (see {@code HerdMigrationGoal}) rather than not breed.
     */
    public boolean wantsToBreedAsPredator() {
        if (!ConfigGamerules.naturalBreeding.get() || this.age != 0 || this.getHunger() < 80) {
            return false;
        }
        if (this.isSenescent()) {
            return false;
        }
        // Zoo mode falls back to the ORIGINAL predicate rather than skipping the check -- returning
        // true here would leave zoo predators breeding more freely than before any of this existed.
        if (!EcologyMode.usesConditionForBreeding(this)) {
            return true;
        }
        return this.isInGoodCondition() && !this.isLocallyOvercrowded();
    }

    /**
     * The shared herbivore breeding rule.
     * <p>
     * Forage stress -- hungry, with nothing edible within GrazeGoal's search radius -- is the carrying
     * capacity signal, and it now suppresses births on its own. It previously only did so in
     * conjunction with {@code herd.migrationFailed} ({@link #isRangeExhausted}), which almost never
     * became true, so herds bred without limit on ground they had already eaten to dirt.
     * <p>
     * This does not reintroduce a birth cap and does not contradict the migration design: ordinary
     * crowding still means move, and animals still migrate first. They simply do not calve on ground
     * that cannot feed a calf.
     */
    public boolean wantsToBreedAsHerbivore() {
        if (!ConfigGamerules.naturalBreeding.get() || this.age != 0 || this.getHunger() < 80) {
            return false;
        }
        if (this.isSenescent()) {
            return false;
        }
        if (!EcologyMode.usesConditionForBreeding(this)) {
            return !this.isRangeExhausted();
        }
        return !this.isForageStressed() && !this.isRangeExhausted();
    }

    /**
     * Hunger a single mouthful restores, scaled by how much forage is left within reach.
     * <p>
     * Grazing paid a flat 16 on any ground at all, which made forage an infinite resource: one mouthful
     * per ~1600 ticks sustained an animal indefinitely no matter how bare its range was, so herds had
     * no carrying capacity and simply ate the landscape to dirt and kept breeding. Called by
     * {@code GrazeGoal} with the count from the search it already runs, so this costs no extra scan.
     */
    public int getForageYield(int nearbyForage) {
        int max = ConfigGamerules.forageYieldMax.get();
        if (!EcologyMode.allowsForageDepletion(this)) {
            return max;
        }
        int min = Math.min(ConfigGamerules.forageYieldMin.get(), max);
        int rich = ConfigGamerules.forageRichDensity.get();
        float fill = rich <= 0 ? 1F : Mth.clamp((float) nearbyForage / rich, 0F, 1F);
        return Math.max(min, Math.round(min + (max - min) * fill));
    }

    @Override
    public void die(DamageSource cause) {
        if (!this.level.isClientSide) {
            /* A death is a loss the whole group feels. Two inside pack_rout_window and the survivors
             * break off -- which is the behaviour that was missing: a pack that had lost half its
             * members kept fighting because break-off only ever consulted each animal's own health. */
            if (this.herd != null) {
                this.herd.reportLoss();
            }
            /* Anything that dies is food. Restricting scavenging to predator KILLS meant a hyena clan
             * could only eat by hunting, which is the opposite of how real scavengers make a living --
             * most of what they eat simply died. Deliberately below the 60 a stolen kill pays. */
            int carrion = ConfigGamerules.carrionHunger.get();
            if (carrion > 0 && !this.isBaby()) {
                this.feedScavengers(this, carrion);
            }
        }
        super.die(cause);
    }

    public boolean isNotMoving(){
        return this.getDeltaMovement().x == 0 && this.getDeltaMovement().z == 0;
    }
    public boolean canBeTargeted() { return true; }
    public double getCurrentSpeed() { return Math.sqrt(this.getDeltaMovement().x * this.getDeltaMovement().x + this.getDeltaMovement().z * this.getDeltaMovement().z); }

    public int getAmbientSoundInterval() {
        //return Integer.MAX_VALUE;
        return 300;
    }

    protected int getExperienceReward(Player p_27590_) {
        int xp = Math.max(getEcoLevel(this) / 2, 1);
        return xp + this.level.random.nextInt(xp);
    }

    public int getVariant(){ return (this.entityData.get(VARIANT)); }
    /* Applying the species speed here rather than only from updateAttributes() is deliberate: bison,
     * camel, hippo, aardvark and opossum do NOT implement INeedsPostUpdate, so they have no
     * per-species hook at all and would silently ignore their JSON "speed". setVariant is the one
     * funnel every path shares -- finalizeSpawn, breed, egg hatching, spawn eggs and world load
     * (readAdditionalSaveData) all pass through it -- so this also retro-applies the new speeds to
     * animals already saved in an existing world. */
    public void setVariant(int variant){
        this.entityData.set(VARIANT, variant);
        if (!this.level.isClientSide) {
            this.applySpeciesSpeed();
        }
    }
    public int getSkin(){ return (this.entityData.get(SKIN)); }
    public void setSkin(int skin){ this.entityData.set(SKIN, skin); }
    public <T extends ComplexMob> void chooseSkinForSpecies(T entityIn, boolean allowRares) {
        if (entityIn.getType().getRegistryName() != null && this instanceof INewSkins && !this.level.isClientSide) {
            String name = entityIn.getType().getRegistryName().getPath();
            if (!TEXTURES_COMMON.get(name).isEmpty()) {
                boolean isRare = allowRares && TEXTURES_RARE.get(name).containsKey(this.getVariant()) && this.random.nextFloat() < ConfigGamerules.rareSkinChance.get();
                int skin = this.random.nextInt(isRare ? TEXTURES_RARE.get(name).get(this.getVariant()).size() : TEXTURES_COMMON.get(name).get(this.getVariant()).size()) + (isRare ? 100 : 0);
                this.setSkin(skin);
            }
        }
    }

    public float getModelScale() { return getEntityData(this.getType()).getScale(this.getVariant()); }
    public float getMobSize(){ return (this.entityData.get(SIZE)); }
    public void setMobSize(float size){ this.entityData.set(SIZE, size); }
    public void setRandomMobSize(){ this.entityData.set(SIZE, this.getModelScale() + ((float)this.random.nextGaussian() * 0.1F)); }

    public void setGender(int gender){ this.entityData.set(GENDER, gender); }
    public int getGender(){	return (this.entityData.get(GENDER)); }
    public boolean isMale() { return this.getGender() == 0; }
    public String getGenderString() {
        return this.isMale() ? "male" : "female";
    }

    public boolean wantsToBreed() {
        return this.isBreedingAge();
    }

    /**
     * The single breeding rule for the four land predator classes (big cat, bear, dire wolf, hyena).
     * <p>
     * They used to differ: cats and bears needed the season, to be awake and to be at 60% health, while
     * wolves and hyenas needed only body condition. The awake check alone failed about half the time for
     * anything that sleeps through the day, since the breeding check only runs every 600 ticks -- and with
     * pack mates always at hand besides, canids out-bred everything else.
     */
    public boolean wantsToBreedAsLandPredator() {
        return this.isBreedingAge() && EntityUtils.hasHealthFraction(this, 0.6F) && this.wantsToBreedAsPredator();
    }

    /* The body of the base wantsToBreed(), callable where a subclass has overridden it. */
    protected final boolean isBreedingAge() {
        /* Sits on the base predicate so every species that chains through super.wantsToBreed() --
         * bear, big cat, boar, aardvark, opossum, spitter, baleen whale -- inherits it without a
         * per-species edit. Returns false outright in Zoo mode and for tamed animals. */
        if (this.isSenescent()) {
            return false;
        }
        if (ConfigGamerules.naturalBreeding.get()) {
            if (CompatBridge.SereneSeasons) {
                return CompatSereneSeasons.isCurrentSeason(this.level, this.getBreedingSeason());
            }
            return true;
            //return this.isInLove();
        }
        return false;
    }

    @SuppressWarnings("unchecked") // Don't use this outside ComplexMobs
    public <T extends ComplexMob> void breed() {
        int bound = 1 + (this.getOffspring() > 0 ? this.random.nextInt(this.getOffspring() + 1) : 0);
        for (int i = 0; i < bound; i++) {
            T child = (T) this.getBreedOffspring((ServerLevel) this.level, this);
            if (child != null) {
                child.setVariant(this.getVariant());
                child.setAge(this.getAdulthoodTime() * -1);
                child.setGender(this.random.nextInt(2));
                child.setRandomMobSize();
                child.setBaby(true);
                child.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
                if (this.getOwner() != null)
                    child.tame((Player) this.getOwner());
                if (this instanceof INeedsPostUpdate) {
                    ((INeedsPostUpdate) child).updateAttributes();
                }
                if (TEXTURES_COMMON.containsKey(child.getType().getRegistryName().getPath())) {
                    chooseSkinForSpecies(child, true);
                }
                //((ServerLevel)this.level).addFreshEntityWithPassengers(child);
                this.level.addFreshEntity(child);
                this.level.broadcastEntityEvent(this, (byte)18);
            }
        }
    }

    protected <T extends ComplexMob> T create_offspring(T entity) {
        entity.setGender(this.random.nextInt(2));
        entity.setRandomMobSize();
        entity.setVariant(this.getVariant());
        entity.chooseSkinForSpecies(this, true);
        if (entity instanceof INeedsPostUpdate) {
            ((INeedsPostUpdate) entity).updateAttributes();
        }
        return entity;
    }

    public String getBreedingSeason() {
        return getEntityData(this.getType()).getBreedingSeason(this.getVariant());
    }

    /* Gestation and maturity are the real brake on population growth -- at multiplier 1.0 a Mammoth is
     * pregnant for 16 in-game days and its calf needs 32 more, so a herd that loses animals to
     * predators never recovers. cycle_length already existed as a global lever but moves both periods
     * together and also affects egg-layers, so these two scalars split them apart.
     * Both clamp to a minimum of 1 tick: at cycle_length 0 the products collapse to zero, and a
     * pregnancy of 0 makes setAge(0) skip the i > 0 && age <= 0 edge in setAge() and never give birth. */
    /* The two multipliers above move every animal in the mod together, which helps prey more than
     * predators: a herd of twenty breeding faster outpaces a pack of eight doing the same. Carnivores are
     * sparse, slow to mature, and the only animals that can die doing their job, so at the shared rate
     * their populations only ever shrink. The predator_* scalars stack on top for them alone.
     * Gated on the carnivore ecology TAG rather than on EcologyTags.isPredator(), which also returns true
     * for anything that merely declares a diet and would therefore catch herbivores too. */
    // Adulthood time is twice the Growing time, and only used for mobs that do not lay eggs
    public int getAdulthoodTime() {
        int base = getEntityData(this.getType()).getGrowingTime(this.getVariant()) * ConfigGamerules.cycleLength.get() * 2;
        double scale = ConfigGamerules.maturityMultiplier.get() * EcologyMode.maturityScale(this);
        if (this.isCarnivore()) {
            scale *= ConfigGamerules.predatorMaturityMultiplier.get();
        }
        return Math.max(1, (int) (base * scale));
    }

    public int getPregnancyTime() {
        int base = getEntityData(this.getType()).getGrowingTime(this.getVariant()) * ConfigGamerules.cycleLength.get();
        double scale = ConfigGamerules.gestationMultiplier.get() * EcologyMode.gestationScale(this);
        if (this.isCarnivore()) {
            scale *= ConfigGamerules.predatorGestationMultiplier.get();
        }
        return Math.max(1, (int) (base * scale));
    }

    /* Consumed by breed() as 1 + random.nextInt(offspring + 1), so the realised litter is 1..offspring+1
     * and the bonus widens the top of that range. Worth knowing that spotted hyena ships with offspring:1,
     * the smallest litter of any predator in the mod despite a clan size of 20. */
    public int getOffspring() {
        int offspring = getEntityData(this.getType()).getOffspring(this.getVariant());
        if (this.isCarnivore()) {
            offspring += EcologyMode.litterBonus(this);
        }
        return offspring;
    }

    /* Applies the species' real-world-calibrated MOVEMENT_SPEED. registerAttributes() is static per
     * TYPE, so it cannot tell a pronghorn (88 km/h) from a springbok, nor a ground sloth from the
     * bison body it was forked off -- every one of the ten new Ice Age types shipped at a copy-pasted
     * 0.22, which had a sloth outrunning a bear. Called from each updateAttributes(), the same
     * per-species hook that already sets ATTACK_DAMAGE and MAX_HEALTH. A JSON with no "speed" field
     * yields -1 and leaves the registerAttributes() value untouched, so untagged types are unaffected. */
    public void applySpeciesSpeed() {
        EntityDataHolder data = getEntityData(this.getType());
        // Guarded because setVariant() calls this during construction/world-load, before the datapack
        // species list is necessarily populated, and a stale NBT Variant can outrun a shortened list.
        if (data == null || this.getVariant() >= data.getSpeciesData().size()) {
            return;
        }
        float speed = data.getSpeed(this.getVariant());
        if (speed > 0 && this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }
    }

    public boolean isFood(ItemStack stack) {
        if (getEntityData(this.getType()).getFavouriteFood(this.getVariant()).getItem().equals(Blocks.AIR.asItem()))
            return false;
        return stack.getItem().equals(getEntityData(this.getType()).getFavouriteFood(this.getVariant()).getItem());
    }
    public boolean canTakeItem(ItemStack stack) {
        return false;
    }

    private boolean isBlinking() {
        return this.tickCount % 60 > 53;
    }

    public boolean shouldRenderEyes() { return !this.isSleeping() && !this.dead && !this.isBlinking() && this.hurtTime == 0; }

    public boolean canMove() { return !this.isSitting() && !this.isSleeping() && !this.isVehicle(); }

    public void setHome(BlockPos position) {
        this.entityData.set(HOME_POS, position);
    }
    public BlockPos getHome() { return this.entityData.get(HOME_POS); }
    public Vec3 getHomeAsVec() {
        BlockPos home = this.getHome();
        return new Vec3(home.getX(), home.getY(), home.getZ());
    }

    public void setAge(int age) {
        int i = this.age;
        super.setAge(age);
        this.age = age;
        if (!this.isMale() && !(this instanceof INestingMob nestingMob && nestingMob.isEggLayer()) && !ConfigGamerules.easyBreeding.get()) {
            if (i > 0 && age <= 0) {
                this.breed();
            }
        }
    }

    public ResourceLocation getTexture() {
        return EntityUtils.getSkinFromEntity(this);
    }

    // Returns the ecological level of an entity. Values are dynamically calculated based on current HP, Attack and Herd size (if any)
    public static int getEcoLevel(LivingEntity entity) {
        return computeEcoLevel(entity, true);
    }

    /**
     * The eco level a predator should weigh this animal at when deciding whether it can take it. Identical
     * to {@link #getEcoLevel} except for a herd animal that has strayed well away from its herd, which is
     * scored without the herd term: the herd is not there to protect it. Without this a lone predator could
     * almost never hunt anything -- a lone jaguar could take no adult grazer in the mod -- because every
     * herd animal carried its whole herd's weight wherever it wandered. Ambushing the straggler is how
     * solitary predators actually hunt herd prey.
     */
    public static int getEcoLevelAsPrey(LivingEntity entity) {
        if (entity instanceof ComplexMob mob && mob.herd != null && mob.herd.creatureList.size() > 1) {
            ComplexMob leader = mob.herd.getLeader();
            double stray = mob.herd.getRadius() * STRAY_RADIUS_FACTOR;
            if (leader != null && leader != mob && mob.distanceToSqr(leader) > stray * stray) {
                return computeEcoLevel(entity, false);
            }
        }
        return computeEcoLevel(entity, true);
    }

    /** How many herd radii from its leader an animal must be before it counts as a straggler to a hunter. */
    private static final double STRAY_RADIUS_FACTOR = 2.0D;

    private static int computeEcoLevel(LivingEntity entity, boolean includeHerd) {
        if (entity instanceof Player) {
            return (int) (4 + (entity.getHealth() / 6));
        }
        int attack = (int) Math.max(entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? entity.getAttribute(Attributes.ATTACK_DAMAGE).getValue() : 1, 4);
        int level = (int) (Math.sqrt(entity.getHealth() * attack) / 2.5F);
        if (includeHerd && entity instanceof ComplexMob mob && mob.herd != null) {
            /* Herd support fades with the animal's own condition. As a flat bonus it was what made every
             * megaherbivore permanently unkillable: a mammoth at 15% health still carried its full +10 for
             * a herd of ten, so it out-scored a dire wolf pack no matter how close to death it was. Ten
             * mammoths do not shield one of their own that is already down. */
            int members = mob.herd.creatureList.size();
            /* A predator pack is only as strong as its hunters. Pups counted here too, so every litter
             * made the whole pack more dangerous -- a pack of eight with its young scored like twenty,
             * reached bison and mammoth herds, and pushed every other predator off the map. Capped at
             * the species' pack size, so an overgrown pack does not keep climbing either. */
            if (mob.isCarnivore()) {
                members = Math.min(mob.herd.getAdultCount(), mob.herd.getMaxSize());
            }
            level += Math.round(members * healthFraction(entity));
        }
        if (entity.isBaby()) {
            /* A calf is not a scaled-down adult in this mod -- updateAttributes() sets MAX_HEALTH and
             * ATTACK_DAMAGE from the species JSON with no baby branch, so a mammoth calf scored exactly
             * what its mother did and nothing could ever hunt one. The herd's protection of its young is
             * already enacted at runtime by ProtectChildrenTarget and the flight goals; it must not ALSO
             * be a targeting veto, or calf predation -- the natural check on a population -- cannot happen. */
            level = Math.round(level * BABY_ECO_FACTOR);
        }
        return level;
    }

    /** Current health as a 0-1 fraction of maximum, guarding the degenerate max-health-of-zero case. */
    private static float healthFraction(LivingEntity entity) {
        float max = entity.getMaxHealth();
        return max <= 0 ? 1F : Mth.clamp(entity.getHealth() / max, 0F, 1F);
    }

    /**
     * Feeds nearby scavengers from a carcass this animal brought down. Runs after the killer and its own
     * pack have eaten, so a scavenger never takes precedence over the animal that did the work.
     * <p>
     * Note carefully that this is driven by the KILLER and tested against the RECIPIENT. The whole point of
     * kleptoparasitism is a hyena eating a lion's kill, so a hook overridden on the hyena would never fire
     * for the case it exists to model -- it would only run when a hyena had already made the kill itself.
     * The "can this animal scavenge" question therefore lives on the recipient, in
     * {@link #canScavengeCarcass()}.
     */
    protected void shareCarcassWithScavengers(Entity target) {
        // Less than a pack-mate's 90: they are getting the scraps, not the carcass.
        this.feedScavengers(target, 60);
    }

    /**
     * Feeds every scavenger near a carcass. Split out of {@link #shareCarcassWithScavengers} so that a
     * death from ANY cause -- old age at the hands of a player, a fall, another predator's kill -- can
     * pay out as well, at the smaller {@code carrion_hunger}.
     *
     * @param owner the animal the carcass "belongs" to, whose own pack has already eaten; pass the
     *              carcass itself for an unowned death, in which case nothing is excluded but the
     *              carcass.
     */
    protected void feedScavengers(Entity target, int amount) {
        if (this.level.isClientSide() || amount <= 0) {
            return;
        }
        for (ComplexMob scavenger : this.level.getEntitiesOfClass(ComplexMob.class, new AABB(target.blockPosition()).inflate(16D))) {
            /* The killer itself and its own pack-mates have already eaten above, at the full 120/90. Feeding
             * them again here would hand a hyena clan that made its own kill a third helping. */
            if (scavenger == this || scavenger == target || !scavenger.isAlive() || !scavenger.canScavengeCarcass()
                    || (this.herd != null && this.herd.creatureList.contains(scavenger))) {
                continue;
            }
            scavenger.addHunger(amount);
            scavenger.huntingCooldown = ConfigGamerules.predatorKillCooldown.get();
            scavenger.endHuntCommitment();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Interference competition between predators
    // ---------------------------------------------------------------------------------------------

    /** How far a rival will come to take a kill off its owner. */
    private static final double DISPLACEMENT_RADIUS = 12D;

    /**
     * How many of this species must be present to drive a rival off its kill. 0 means this animal never
     * takes anyone else's.
     * <p>
     * The hyena's number is {@code kleptoparasitism_clan_size}; the bear's is one, because a bear does
     * not need help. This is strength expressed as economy rather than as damage: a clan that eats off
     * other predators' kills passes the hunger gate less often, so it hunts less often, so it spends
     * less time exposed to the only thing in the mod that kills a predator for hunting.
     */
    public int displacementGroupSize() {
        return 0;
    }

    /** Whether this animal gives up a kill to {@code rival} rather than fighting over it. */
    public boolean yieldsCarcassTo(ComplexMob rival) {
        return false;
    }

    /** The rival, if any, that is about to take this animal's kill. */
    @Nullable
    private ComplexMob findCarcassThief() {
        if (ConfigGamerules.kleptoparasitismClanSize.get() <= 0) {
            return null;
        }
        ComplexMob best = null;
        int bestCount = 0;
        List<ComplexMob> nearby = this.level.getEntitiesOfClass(ComplexMob.class, this.getBoundingBox().inflate(DISPLACEMENT_RADIUS));
        // Count only the rivals actually standing over the carcass, not the whole clan's roster. Counted
        // once from the one scan -- this used to re-run the identical scan for every rival in it.
        Map<EntityType<?>, Integer> present = new HashMap<>();
        for (ComplexMob mate : nearby) {
            if (mate.isAlive() && !mate.isBaby()) {
                present.merge(mate.getType(), 1, Integer::sum);
            }
        }
        for (ComplexMob rival : nearby) {
            int needed = rival.displacementGroupSize();
            if (rival == this || !rival.isAlive() || rival.isBaby() || needed <= 0 || !this.yieldsCarcassTo(rival)
                    || (this.herd != null && this.herd.creatureList.contains(rival))) {
                continue;
            }
            int count = present.getOrDefault(rival.getType(), 0);
            if (count >= needed && count > bestCount) {
                bestCount = count;
                best = rival;
            }
        }
        return best;
    }

    /**
     * Hands the carcass to {@code thief} and its group, leaving the animal that did the work with
     * scraps. It still pays the kill cooldown -- the hunt happened, the meal largely did not, and that
     * asymmetry is exactly what makes a scavenger's living cheaper than a hunter's.
     */
    private void loseCarcassTo(ComplexMob thief, Entity target) {
        this.addHunger(40);
        this.huntingCooldown = ConfigGamerules.predatorKillCooldown.get();
        this.endHuntCommitment();
        this.routFromCombat();
        thief.addHunger(120);
        thief.huntingCooldown = ConfigGamerules.predatorKillCooldown.get();
        if (thief.herd != null) {
            for (ComplexMob member : thief.herd.creatureList) {
                if (member != thief && member.isAlive() && member.distanceToSqr(target) < 256D) {
                    member.addHunger(90);
                    member.huntingCooldown = ConfigGamerules.predatorKillCooldown.get();
                    member.endHuntCommitment();
                }
            }
        }
        if (UntamedWilds.DEBUG) {
            UntamedWilds.LOGGER.info("{} was driven off its kill by {}", this.getName().getString(), thief.getName().getString());
        }
    }

    /**
     * True for animals that will eat a carcass they did not bring down. Only the hyena says yes.
     * <p>
     * This is strength as economy rather than as damage, and it is what finally makes a hyena a different
     * animal from a dire wolf rather than the same file with smaller numbers: a clan that eats off other
     * predators' kills passes the hunger gate in HuntMobTarget less often, so it hunts less often, so it
     * spends less time exposed to predator_hunt_risk_chance -- the only thing in the mod that kills a
     * predator for hunting.
     */
    public boolean canScavengeCarcass() {
        return false;
    }

    /** True when this animal's species JSON declares the carnivore ecology tag. */
    public boolean isCarnivore() {
        return EcologyTags.getTags(this).contains(EcologyTags.CARNIVORE);
    }

    /* INERT, deliberately (2026-09-30). Upstream's "retaliation strike" only fires when the INCOMING
     * damage is thorns, which nothing deals, so all 16 callers do nothing -- the check was probably
     * meant to be !isThorns() (the thorns source it deals is what stops two animals retaliating
     * forever). Re-enabling it hands every herbivore a free counter-hit on predators, so it is a
     * balance decision, taken with the prey-side ecology pass rather than as a bug fix. */
    protected void performRetaliation(DamageSource damageSource, float health, float damage, boolean needsActiveTarget) {
        if (needsActiveTarget && this.getTarget() != damageSource.getDirectEntity())
            return;
        if (this.retaliationCooldown == 0 && !this.isNoAi() && this.getTarget() != null && damage < health && !damageSource.isProjectile() && damageSource.getDirectEntity() instanceof LivingEntity && !(damageSource.getDirectEntity() instanceof Player) && !(damageSource.getDirectEntity() instanceof TamableAnimal tamable && tamable.getOwner() != null)) {
            if ((damageSource instanceof EntityDamageSource && ((EntityDamageSource)damageSource).isThorns()) && this.hasLineOfSight(damageSource.getDirectEntity())) {
                damageSource.getDirectEntity().hurt(DamageSource.thorns(this), (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                this.retaliationCooldown = 10;
            }
        }
    }

    protected void setAngry(boolean isAngry) { this.entityData.set(IS_ANGRY, isAngry); }
    public boolean isAngry() { return (this.entityData.get(IS_ANGRY)); }

    // Commands:
    // 0 - Wander: The mob wanders around naturally
    // 1 - Follow: The mob will follow its owner, occasionally teleporting
    // 2 - Sit: The mob will sit in place
    // 3 - Guard: The mob will sit in place and attack nearby mobs (NIY)
    public void setCommandInt(int command) { this.entityData.set(COMMAND, command % 3); }
    public int getCommandInt() { return (this.entityData.get(COMMAND)); }

    public boolean shouldDespawn() { return this instanceof ISpecies && this.getHome() != BlockPos.ZERO; }
    @Override
    public void checkDespawn() {
        super.checkDespawn();
        if (this.shouldDespawn()) {
            if (!this.level.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), ConfigMobControl.critterSpawnRange.get())) {
                if (this instanceof ISpecies && this.getHome() != BlockPos.ZERO) {
                    BlockEntity burrow = this.level.getBlockEntity(this.getHome());
                    if (burrow instanceof CritterBurrowBlockEntity) {
                        ((CritterBurrowBlockEntity)burrow).tryEnterBurrow(this);
                        burrow.setChanged();
                    }
                }
            }
        }
    }

    public void addAdditionalSaveData(CompoundTag compound){
        super.addAdditionalSaveData(compound);
        if (this.getHome() != BlockPos.ZERO) {
            compound.putInt("HomePosX", this.getHome().getX());
            compound.putInt("HomePosY", this.getHome().getY());
            compound.putInt("HomePosZ", this.getHome().getZ());
        }
        if (this.isTame()) {
            compound.putInt("Command", this.getCommandInt());
        }
        compound.putInt("Variant", this.getVariant());
        compound.putInt("Skin", this.getSkin());
        compound.putFloat("Size", this.getMobSize());
        compound.putInt("Gender", this.getGender());
        compound.putBoolean("isAngry", this.isAngry());
        compound.putInt("PeacefulTicks", this.huntingCooldown);
        compound.putInt("Hunger", this.getHunger());
        /* Written in both modes on purpose. These are the only two demographic counters that must
         * survive a reload -- an animal whose age reset every time its chunk unloaded would never grow
         * old, and one whose condition reset would breed off a single kill again. */
        compound.putFloat("Condition", this.condition);
        compound.putInt("LifeTicks", this.lifeTicks);
        compound.putInt("StarveTicks", this.starveTicks);
    }

    public void readAdditionalSaveData(CompoundTag compound){
        super.readAdditionalSaveData(compound);
        if (compound.contains("HomePosX")) {
            int i = compound.getInt("HomePosX");
            int j = compound.getInt("HomePosY");
            int k = compound.getInt("HomePosZ");
            this.setHome(new BlockPos(i, j, k));
        }
        if (compound.contains("OwnerUUID")) {
            this.setCommandInt(compound.getInt("Command"));
        }
        this.setVariant(EntityUtils.getClampedNumberOfSpecies(compound.getInt("Variant"), this.getType()));
        //this.setVariant(compound.getInt("Variant"));
        this.setSkin(compound.getInt("Skin"));
        this.setMobSize(compound.getFloat("Size"));
        this.setGender(compound.getInt("Gender"));
        this.setAngry(compound.getBoolean("isAngry"));
        this.huntingCooldown = compound.getInt("PeacefulTicks");
        // Entities saved before Hunger moved up to ComplexMob have no tag on aquatic species; default
        // them to the same 79 defineSynchedData uses rather than to a starving 0.
        this.setHunger(compound.contains("Hunger") ? compound.getInt("Hunger") : 79);
        /* Animals saved before any of this existed have no tags. Default them to a healthy, prime-aged
         * animal rather than to zero -- an existing world must not have its entire fauna wake up
         * starving-weak and, at lifeTicks 0, all the same age. */
        this.condition = compound.contains("Condition") ? compound.getFloat("Condition") : Mth.clamp(this.getHunger() / CONDITION_FULL_HUNGER, 0F, 1F);
        this.lifeTicks = compound.contains("LifeTicks") ? compound.getInt("LifeTicks") : this.random.nextInt(Math.max(1, this.getLifespan() / 2));
        this.starveTicks = compound.getInt("StarveTicks");
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        if (reason != MobSpawnType.DISPENSER && reason != MobSpawnType.BUCKET && reason != MobSpawnType.BREEDING) {
            if (this instanceof ISpecies) {
                Holder<Biome> optional = worldIn.getBiome(new BlockPos(this.position()));
                int i = ((ISpecies)this).setSpeciesByBiome(optional, reason);
                this.setVariant(i);
                if (i == 99) {
                    this.remove(RemovalReason.DISCARDED);
                    return null;
                }
            }
            this.setGender(this.random.nextInt(2));
            this.setRandomMobSize();
            /* Give a naturally-spawned animal an age. Without this every animal a chunk generates is
             * newborn, so a whole region reaches senescence on the same day and dies off together --
             * a synchronised die-off is exactly the boom-and-bust this pass exists to remove. Capped
             * below the senescence threshold so worldgen never places an animal that is already old. */
            this.lifeTicks = this.random.nextInt(Math.max(1, (int) (this.getLifespan() * ConfigGamerules.senescenceFraction.get())));
            if (TEXTURES_COMMON.containsKey(this.getType().getRegistryName().getPath())) {
                chooseSkinForSpecies(this, ConfigGamerules.wildRareSkins.get());
            }
            if (this instanceof INeedsPostUpdate) {
                ((INeedsPostUpdate) this).updateAttributes();
            }

            this.setAge(0);
        }
        if (this instanceof IPackEntity) {
            IPackEntity.initPack(this);
        }
        return spawnDataIn;
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level.isClientSide()) {
            if (!CompatBridge.Patchouli) {
                ModAdvancementTriggers.NO_PATCHOULI_LOADED.trigger((ServerPlayer) player);
            }
            ItemStack itemstack = player.getItemInHand(hand);

            // Highlight mobs in the same pack if Player is in Creative mode
            if (player.isCreative() && itemstack.isEmpty() && this instanceof IPackEntity && this.herd != null) {
                for (int i = 0; i < this.herd.creatureList.size(); ++i) {
                    ComplexMob creature = this.herd.creatureList.get(i);
                    creature.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
                }
            }

            // Command handler for tamed mobs, includes Food/Potion consumption
            if (this.isTame() && this.getOwner() == player) {
                if (itemstack.isEmpty()) {
                    this.setCommandInt(this.getCommandInt() + 1);
                    player.sendMessage(new TranslatableComponent("entity.untamedwilds.command." + this.getCommandInt()), Util.NIL_UUID);
                    if (this.getCommandInt() > 1) {
                        this.getNavigation().stop();
                        this.setSitting(true);
                    } else if (this.getCommandInt() <= 1 && this.isSitting()) {
                        this.setSitting(false);
                    }
                }
                else {
                    EntityUtils.consumeItemStack(this, itemstack);
                }
            }
            return super.mobInteract(player, hand);
        }
        return InteractionResult.PASS;
    }
}
