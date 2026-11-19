package untamedwilds.entity.ai;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import untamedwilds.config.ConfigAmbience;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.entity.EntityCarcass;
import untamedwilds.util.EntityUtils;
import untamedwilds.util.GoalTimer;
import untamedwilds.util.PerfCounters;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * A scavenger walks to a carcass and feeds on it over time (2.8.0, P7) -- what {@code feedScavengers} used to do as an
 * instant payout to every bear and hyena within 16 blocks. Same ceilings: at most carcass_max_feeders at once and at
 * most the carcass's per-scavenger cap each (60 on a predator's kill, carrion_hunger on a natural death), so the food
 * a scavenger can get is unchanged; it now has to go and get it, and the kill site becomes a place things happen.
 */
public class FeedOnCarcassGoal extends Goal {

    private static final double SEARCH_RADIUS = 24.0D;
    private static final int TRAVEL_BUDGET = 900;
    private static final int BITE_INTERVAL = 20;
    private static final int BITE = 15;

    private final ComplexMobTerrestrial mob;
    private final GoalTimer searchTimer = new GoalTimer(60, true);
    private int emptySearches;
    private long nextSearch;
    @Nullable
    private EntityCarcass carcass;
    private int ticks;
    private boolean ate;

    public FeedOnCarcassGoal(ComplexMobTerrestrial mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean busy() {
        return this.mob.getTarget() != null || this.mob.fleeCooldown > 0 || this.mob.isSleeping() || this.mob.isTame()
                || !this.mob.canMove() || this.mob.isBaby();
    }

    @Override
    public boolean canUse() {
        if (!ConfigAmbience.carcasses.get() || this.mob.getHunger() > 150 || this.busy()) {
            return false;
        }
        if (!this.searchTimer.ready(this.mob) || this.mob.level.getGameTime() < this.nextSearch) {
            return false;
        }
        PerfCounters.inc(PerfCounters.Counter.CARCASS_SCAN);
        List<EntityCarcass> near = this.mob.level.getEntitiesOfClass(EntityCarcass.class,
                this.mob.getBoundingBox().inflate(SEARCH_RADIUS, 6.0D, SEARCH_RADIUS), c -> c.canFeed(this.mob));
        if (near.isEmpty()) {
            // back off: 60 ticks, then 2, 4, 8 minutes... a scavenger in an empty landscape stops looking every 3 seconds
            this.emptySearches = Math.min(this.emptySearches + 1, 4);
            this.nextSearch = this.mob.level.getGameTime() + (60L << this.emptySearches);
            return false;
        }
        this.emptySearches = 0;
        near.sort(Comparator.comparingDouble(c -> c.distanceToSqr(this.mob)));
        for (EntityCarcass c : near) {
            if (c.claimFeeder(this.mob)) {
                this.carcass = c;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.ate = false;
        this.pathTo();
    }

    private void pathTo() {
        if (this.carcass != null) {
            this.mob.getNavigation().moveTo(this.carcass.getX(), this.carcass.getY(), this.carcass.getZ(), 1.0D);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.carcass != null && this.carcass.isAlive() && this.carcass.canFeed(this.mob) && !this.busy()
                && this.mob.getHunger() < 195 && this.ticks < TRAVEL_BUDGET;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.carcass == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(this.carcass, 30.0F, 30.0F);
        double reach = this.mob.getBbWidth() * 0.5D + this.carcass.getBbWidth() * 0.5D + 0.9D;
        double d2 = this.mob.distanceToSqr(this.carcass);
        if (d2 > reach * reach) {
            if (d2 < (reach + 2.0D) * (reach + 2.0D)) {
                // last stretch walked straight: a wide animal's path can end ~1.8 blocks short of its target, and a
                // re-path from there is already "done" (the same freeze DrinkGoal had)
                this.mob.getNavigation().stop();
                this.mob.getMoveControl().setWantedPosition(this.carcass.getX(), this.carcass.getY(), this.carcass.getZ(), 1.0D);
            } else if (this.mob.getNavigation().isDone() && this.ticks % 20 == 0) {
                this.pathTo();
            }
            return;
        }
        this.mob.getNavigation().stop();
        if (this.ticks % BITE_INTERVAL == 0) {
            int bite = this.carcass.bite(this.mob, BITE);
            if (bite > 0) {
                this.mob.addHunger(bite);
                this.ate = true;
                this.mob.tryStartAnimation(this.mob.getAnimationEat());
                SoundEvent eat = EntityUtils.getSound(this.mob.getType(), this.mob.getVariant(), "eat");
                this.mob.playSound(eat != null ? eat : SoundEvents.GENERIC_EAT, 0.6F, this.mob.voicePitch());
            }
        }
    }

    @Override
    public void stop() {
        if (this.carcass != null) {
            this.carcass.releaseFeeder(this.mob);
        }
        if (this.ate) {
            // fed: the same lockout a kill-site payout used to charge
            this.mob.huntingCooldown = ConfigGamerules.predatorKillCooldown.get();
            this.mob.endHuntCommitment();
        }
        this.carcass = null;
    }
}
