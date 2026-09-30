package untamedwilds.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.config.EcologyMode;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HerdEntity {
    private int maxHerdSize;
    private float radius = 8.0F;
    private boolean openToCombine;
    private ComplexMob leader;
    private final Random rand;
    public final List<ComplexMob> creatureList = new ArrayList<>();
    public double splitOffDistance = 1024D;

    /* How often the herd's own bookkeeping runs. The land species call tick() from EVERY member's
     * aiStep (fish call it from the leader only), and a `leader.tickCount % HERD_TICK` gate does not
     * stop that: every member sees the same leader tickCount in the same server tick, so the block ran
     * N times in an N-strong herd -- N entity scans, and every counter below decaying N times too
     * fast (a twenty-strong herd's 6000-tick migration cooldown lasted ~300 ticks, and pack losses
     * and pressure evaporated before they could ever rout a pack). lastTickedGameTime makes tick()
     * run once per herd per server tick no matter how many members call it. */
    private static final int HERD_TICK = 10;
    private long lastTickedGameTime = Long.MIN_VALUE;
    /* Distance beyond which a member is dropped from the herd, while a migration is under way. The
     * normal 1024 (32 blocks) shreds a herd that is crossing a hundred blocks in single file: the
     * stragglers are removed mid-journey and each founds its own herd where it stands. */
    private static final double MIGRATION_SPLIT_DISTANCE = 65536D; // 256 blocks

    /* Who is currently fighting what, on behalf of this herd. Group defence used to be unbounded --
     * ProtectChildrenTarget runs independently on every adult, so in a twenty-strong herd a dozen
     * mothers engaged the same wolf at once and killed it in about two seconds regardless of how
     * strong it was. Real herd defence puts a few animals into a front and bunches the rest. */
    private final Map<ComplexMob, LivingEntity> defenders = new HashMap<>();

    /* Group morale. Break-off used to be per-individual and health-based only, so a wolf at full
     * health standing beside three dead pack-mates had no reason to leave and packs fought until the
     * last member fell. Both counters decay over pack_rout_window. */
    private int recentLosses;
    private int pressure;

    /* Range shifting. A herd that has eaten its patch down, or that is standing in an overcrowded
     * neighbourhood, moves rather than being told to stop breeding. */
    private boolean migrating;
    private int migrationCooldown;
    /** Set when a migration search found nowhere better. The ONLY thing that suppresses breeding. */
    public boolean migrationFailed;

    public HerdEntity(ComplexMob creature, int maxSize) {
        this.openToCombine = true;
        this.rand = new Random();
        this.maxHerdSize = maxSize;
        this.radius = radiusForSize(maxSize);
        this.setLeader(creature);
    }

    /* The radius was a flat 8.0 regardless of herd size, which was fine for the old maximum of 8 but
     * packs 20 bison into a 16-block circle -- they shove each other constantly and the herd reads as
     * a scrum rather than a herd. Scaled so a solitary animal keeps a tight leash and a large herd
     * gets room to spread. */
    private static float radiusForSize(int maxSize) {
        return Math.min(24.0F, 6.0F + maxSize * 0.6F);
    }

    public void setLeader(ComplexMob creature) {
        this.leader = creature;
        if (!this.containsCreature(this.leader)) {
            this.addCreature(this.leader);
        }
    }

    public void chooseRandomLeader() {
        this.setLeader(this.creatureList.get(this.rand.nextInt(this.creatureList.size())));
    }

    public ComplexMob getLeader() {
        return this.leader;
    }

    public void addCreature(ComplexMob creature) {
        if (!this.creatureList.contains(creature)) {
            this.creatureList.add(creature);
        }
    }

    public boolean containsCreature(ComplexMob creature) {
        return this.creatureList.contains(creature);
    }

    public void removeCreature(HerdEntity herd, ComplexMob creature) {
        herd.creatureList.remove(creature);
        if (herd.creatureList.size() > 0 && herd.getLeader() == creature) {
            herd.chooseRandomLeader();
        }
        if (creature instanceof IPackEntity) {
            IPackEntity.initPack(creature);
            //creature.herd.setOpenToCombine(false);
        }
    }

    public void setMaxSize(int maxSchoolSize) {
        this.maxHerdSize = maxSchoolSize;
        this.radius = radiusForSize(maxSchoolSize);
    }
    public int getMaxSize() {
        return this.maxHerdSize;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
    public float getRadius() {
        return this.radius;
    }

    public void setOpenToCombine(boolean openToCombine) {
        this.openToCombine = openToCombine;
    }
    private boolean isOpenToCombine() {
        return this.openToCombine;
    }

    /* Only ADULTS count against maxHerdSize. A calf is born outside any herd -- breed() just calls
     * addFreshEntity, and the baby's own aiStep then gives it a fresh one-member herd -- so with a raw
     * size check a herd sitting at its cap could never absorb its own young. The calf was locked out
     * permanently, wandering alone, and since getEcoLevel() scales with herd size that also made it
     * the easiest target on the map. Excluding babies means breeding still works at a maxed herd,
     * which is the whole point of raising the sizes. */
    private int adultCount() {
        int adults = 0;
        for (ComplexMob creature : this.creatureList) {
            if (!creature.isBaby()) {
                adults++;
            }
        }
        return adults;
    }

    // ---------------------------------------------------------------------------------------------
    // Bounded group defence
    // ---------------------------------------------------------------------------------------------

    /**
     * Asks to join the animals fighting {@code threat} on the herd's behalf. False means the front is
     * already full and this animal should back away instead -- which it does for free, since the HEAVY
     * herbivores all register a {@code SmartAvoidGoal} keyed on predators that is suppressed only while
     * they hold a combat target.
     */
    public boolean tryClaimDefenderSlot(ComplexMob member, LivingEntity threat) {
        if (this.defenders.get(member) == threat) {
            return true;
        }
        if (member.defendCooldown > 0) {
            return false;
        }
        if (this.defenderCount(threat) >= EcologyMode.defenderCap(member)) {
            return false;
        }
        this.defenders.put(member, threat);
        return true;
    }

    public void releaseDefenderSlot(ComplexMob member) {
        if (this.defenders.remove(member) != null) {
            member.defendCooldown = ConfigGamerules.herdDefenderEngageCooldown.get();
        }
    }

    /**
     * Whether this animal currently holds a defender slot.
     * <p>
     * Read by {@link untamedwilds.entity.ai.HerdFleeGoal} so that a herd panic does not disarm the very
     * animals the herd just committed to the fight: {@code panic()} clears every member's combat target
     * and re-clears it for the whole 200-tick window, which would have wiped a defender's target within
     * ten ticks of it claiming one. Non-defenders still panic and run exactly as before -- that flight
     * IS the "the rest back away" half of the defender cap, and it is deliberately left alone.
     */
    public boolean isDefender(ComplexMob member) {
        return this.defenders.containsKey(member);
    }

    public int defenderCount(LivingEntity threat) {
        int count = 0;
        for (Map.Entry<ComplexMob, LivingEntity> entry : this.defenders.entrySet()) {
            if (entry.getValue() == threat && entry.getKey().isAlive()) {
                count++;
            }
        }
        return count;
    }

    /* A defender that died, lost its target or wandered off never calls stop() reliably, so slots are
     * re-validated here rather than trusted to the goal's lifecycle. */
    private void pruneDefenders() {
        this.defenders.entrySet().removeIf(entry -> {
            ComplexMob member = entry.getKey();
            LivingEntity threat = entry.getValue();
            return !member.isAlive() || threat == null || !threat.isAlive() || member.getTarget() != threat;
        });
    }

    // ---------------------------------------------------------------------------------------------
    // Group morale
    // ---------------------------------------------------------------------------------------------

    /** Called from {@link ComplexMob#die} for any member. Two losses inside the window rout the group. */
    public void reportLoss() {
        this.recentLosses++;
    }

    /**
     * Damage taken from an animal the group is NOT hunting -- a herd member defending its own. Enough
     * of it means the herd is simply too well defended and the hunt is not worth continuing, which is
     * the case the old code had exactly backwards: being gored by a defender used to make the whole
     * pack drop the hunt and pile onto the defender instead.
     */
    public void addPressure(int amount) {
        this.pressure += amount;
    }

    private boolean shouldRout() {
        int lossLimit = ConfigGamerules.packRoutLossThreshold.get();
        if (lossLimit > 0 && this.recentLosses >= lossLimit) {
            return true;
        }
        int pressureLimit = ConfigGamerules.packRoutPressure.get();
        return pressureLimit > 0 && this.pressure >= pressureLimit;
    }

    /**
     * The whole group gives up and runs. Tears the hunt commitment down explicitly -- letting the
     * animals merely be "routed" is not enough, because {@code HuntMobTarget.canContinueToUse}
     * re-acquires a still-committed victim while ignoring both the cooldown and follow range.
     */
    private void broadcastRout() {
        for (ComplexMob member : this.creatureList) {
            if (!member.isAlive()) {
                continue;
            }
            member.rememberDefeat(member.getTarget());
            member.endHuntCommitment();
            member.routFromCombat();
        }
        this.defenders.clear();
        this.recentLosses = 0;
        this.pressure = 0;
    }

    // ---------------------------------------------------------------------------------------------
    // Range shifting
    // ---------------------------------------------------------------------------------------------

    /**
     * True when this herd should look for somewhere else to be: it has run out of food where it stands,
     * or the neighbourhood holds more of its own species than the range will support.
     * <p>
     * Note this is a reason to MOVE, not a reason to stop breeding. Emigration is the primary
     * density-dependent response in real ungulates; reduced fecundity is what happens only when there
     * is nowhere left to go, which is {@link ComplexMob#isRangeExhausted()}.
     */
    public boolean wantsToMigrate() {
        if (this.migrationCooldown > 0 || this.leader == null || !EcologyMode.allowsMigration(this.leader)) {
            return false;
        }
        return this.leader.isForageStressed() || this.leader.isLocallyOvercrowded();
    }

    public boolean isMigrating() {
        return this.migrating;
    }

    public void beginMigration() {
        this.migrating = true;
        this.migrationFailed = false;
    }

    /**
     * Ends a migration. {@code destination} is null when the search or the journey failed, which starts
     * the cooldown but leaves {@link #migrationFailed} set so the breeding clause can see it.
     */
    public void endMigration(@Nullable BlockPos destination) {
        this.migrating = false;
        this.migrationCooldown = ConfigGamerules.migrationCooldown.get();
        if (destination == null) {
            this.migrationFailed = true;
            return;
        }
        this.migrationFailed = false;
        /* Home has to move with the herd. ComplexMobTerrestrial.aiStep walks an animal back to
         * getHome() to sleep, so a migration that left home behind would be undone every night. */
        for (ComplexMob member : this.creatureList) {
            member.setHome(destination);
            member.clearForageStress();
        }
    }

    /** Members are not dropped for distance while the herd is strung out on the move. */
    public double effectiveSplitOffDistance() {
        return this.migrating ? MIGRATION_SPLIT_DISTANCE : this.splitOffDistance;
    }

    /**
     * Sends one young adult out of a full herd to found its own somewhere else. This is the valve that
     * lets the population keep growing while local density self-limits: births are never blocked by
     * crowding, the surplus simply leaves.
     */
    @Nullable
    public ComplexMob pickDisperser() {
        if (this.adultCount() < this.getMaxSize() || this.leader == null || !EcologyMode.allowsMigration(this.leader)) {
            return null;
        }
        if (!this.leader.isLocallyOvercrowded()) {
            return null;
        }
        for (ComplexMob member : this.creatureList) {
            // Never the leader, never a calf, and never an animal already committed to something else.
            if (member != this.leader && member.isAlive() && !member.isBaby() && !member.isTame()
                    && member.getTarget() == null && member.dispersalLockout == 0) {
                return member;
            }
        }
        return null;
    }

    /** Detaches a disperser into a herd of its own, closed to re-merging until its lockout expires. */
    public void detachDisperser(ComplexMob disperser) {
        this.creatureList.remove(disperser);
        this.defenders.remove(disperser);
        if (this.leader == disperser && !this.creatureList.isEmpty()) {
            this.chooseRandomLeader();
        }
        IPackEntity.initPack(disperser);
        disperser.dispersalLockout = ConfigGamerules.dispersalLockout.get();
        if (disperser.herd != null) {
            disperser.herd.setOpenToCombine(false);
        }
    }

    public void tick() {
        long now = this.getLeader().getLevel().getGameTime();
        if (now == this.lastTickedGameTime) {
            return;
        }
        this.lastTickedGameTime = now;
        if (this.adultCount() >= this.getMaxSize()) {
            this.setOpenToCombine(false);
        }
        else if (this.rand.nextInt(1800) == 0) {
            this.setOpenToCombine(!this.isOpenToCombine());
        }

        if (this.getLeader().tickCount % HERD_TICK == 0) {
            /* Counted down in steps of HERD_TICK because this block runs once per HERD_TICK ticks, not
             * once per tick. */
            if (this.migrationCooldown > 0) {
                this.migrationCooldown = Math.max(0, this.migrationCooldown - HERD_TICK);
            }
            this.pruneDefenders();
            /* Losses and combat pressure fade over pack_rout_window, so a group is routed by a fight it
             * is losing NOW rather than by an accumulated tally of everything that ever happened to it. */
            int window = Math.max(HERD_TICK, ConfigGamerules.packRoutWindow.get());
            if (this.recentLosses > 0 && this.rand.nextInt(Math.max(1, window / HERD_TICK)) == 0) {
                this.recentLosses--;
            }
            if (this.pressure > 0) {
                this.pressure = Math.max(0, this.pressure - Math.max(1, ConfigGamerules.packRoutPressure.get() * HERD_TICK / window));
            }
            if (this.shouldRout()) {
                this.broadcastRout();
            }

            List<ComplexMob> toRemove = new ArrayList<>();
            // Babies always rejoin, even when the herd is full and therefore closed to combining.
            List<ComplexMob> list = this.getLeader().getLevel().getEntitiesOfClass(ComplexMob.class, this.getLeader().getBoundingBox().inflate(16.0D, 12.0D, 16.0D));
            for (ComplexMob creature : list) {
                if (creature.isBaby() && !this.containsCreature(creature) && creature.herd != null
                        && creature.herd != this && creature.getClass().equals(this.getLeader().getClass())) {
                    combineHerds(this, creature.herd);
                }
            }
            if (this.isOpenToCombine()) {
                for (ComplexMob creature : list) {
                    if (!this.containsCreature(creature) && creature.herd != null && canCombineHerds(this, creature.herd)) {
                        int netSize = this.adultCount() + creature.herd.adultCount();
                        if (creature.herd.isOpenToCombine() && creature.getClass().equals(this.getLeader().getClass()) && netSize <= this.getMaxSize() && netSize <= creature.herd.getMaxSize()) {
                            combineHerds(this, creature.herd);
                        }
                    }
                }
            }

            ComplexMob creature;
            for (ComplexMob complexMob : this.creatureList) {
                creature = complexMob;
                /*if (creature instanceof IPackEntity) {
                    IPackEntity packCreature = (IPackEntity)creature;
                    if (packCreature.shouldLeavePack()) {
                        toRemove.add(creature);
                        continue;
                    }
                }*/
                // effectiveSplitOffDistance, not splitOffDistance: a migrating herd is strung out over
                // far more than 32 blocks and would otherwise shed its stragglers mid-journey, each of
                // whom then founds its own herd wherever it happened to be dropped.
                if (creature.isAlive() && creature.distanceToSqr(this.leader) <= this.effectiveSplitOffDistance()) {
                    if (creature != this.leader) {
                        if (creature.distanceToSqr(this.leader) <= (double) (this.radius * this.radius)) {
                            Vec3 vec = this.leader.getLookAngle();
                            creature.getLookControl().setLookAt(creature.getX() + vec.x, creature.getY() + vec.y, creature.getZ() + vec.z, 6.0F, 85.0F);
                        }
                        /* On the move, followers are pulled along actively rather than left to
                         * SmartWanderGoal, which only re-paths on a 1-in-120 roll once its navigation
                         * has finished -- far too slow to keep up with a leader crossing a hundred
                         * blocks. Without this the herd arrives as one animal with a long tail of
                         * stragglers still standing on the range it was supposed to have left. */
                        else if (this.migrating && creature.getTarget() == null && creature.fleeCooldown <= 0 && creature.canMove()) {
                            creature.getNavigation().moveTo(this.leader.getX(), this.leader.getY(), this.leader.getZ(), 1.1D);
                        }
                    }
                } else {
                    toRemove.add(creature);
                }
            }

            for (ComplexMob mob : toRemove) {
                removeCreature(this, mob);
            }
        }
    }

    static boolean canCombineHerds(HerdEntity thisPack, HerdEntity otherPack) {
        return thisPack.adultCount() + otherPack.adultCount() <= thisPack.getMaxSize();
    }

    public static void combineHerds(HerdEntity herd1, HerdEntity herd2) {
        // Merging a herd into itself would copy its list onto itself and then clear it, wiping the herd.
        if (herd1 == herd2) {
            return;
        }
        if (herd2.creatureList.size() > herd1.creatureList.size()) {
            herd1.setLeader(herd2.getLeader());
        }
        if (herd2.getMaxSize() < herd1.getMaxSize()) {
            herd1.setMaxSize(herd2.getMaxSize());
        }
        if (herd2.getRadius() < herd1.getRadius()) {
            herd1.setRadius(herd2.getRadius());
        }
        /* Repoint the absorbed animals at the herd that absorbed them. Without this the merge only
         * copied them into herd1's LIST while each still pointed at herd2, and SmartWanderGoal reads
         * creature.herd.getLeader() -- so a "merged" animal kept following its old leader, and a
         * newly absorbed calf (sole member of its own herd, hence its own leader) just followed
         * itself and never actually joined. Skips duplicates so the list cannot gain the same animal
         * twice when two herds partially overlap. */
        for (ComplexMob creature : herd2.creatureList) {
            if (!herd1.creatureList.contains(creature)) {
                herd1.creatureList.add(creature);
            }
            creature.herd = herd1;
        }
        herd2.creatureList.clear();
    }
}