package untamedwilds.entity.ai;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.INestingMob;

import java.util.EnumSet;
import java.util.List;

public class SmartMateGoal extends Goal {
    /** Distance at which a pair is close enough to court, squared. */
    private static final double MATING_DIST_SQR = 9.0D;
    /** Ticks allowed to REACH a mate, separate from the time spent courting one. */
    private static final int TRAVEL_BUDGET = 600;
    /** Travel budget for a mate found by the wide solitary search. */
    private static final int LONG_RANGE_TRAVEL_BUDGET = 2400;

    private final ComplexMob taskOwner;
    private final Level world;
    private final int executionChance;
    private final Class<? extends ComplexMob> mateClass;
    private ComplexMob targetMate;
    private int spawnBabyDelay;
    /* Travel is timed separately from courtship. spawnBabyDelay used to start counting the moment the
     * goal began and canContinueToUse gave up at 200 ticks, so a pair more than a few blocks apart
     * always abandoned the approach part-way -- which is why simply widening the search below would not
     * have been enough on its own. */
    private int travelTicks;
    /* Set when the mate was found by the wide solitary search: the walk is longer, so it gets a longer
     * budget and re-paths on a timer rather than every tick. */
    private boolean longRange;
    private int repathDelay;
    private final double moveSpeed;

    public SmartMateGoal(ComplexMob entityIn, double speedIn) {
        this(entityIn, speedIn, 120, entityIn.getClass());
    }

    private SmartMateGoal(ComplexMob entityIn, double speedIn, int chance, Class<? extends ComplexMob> mateClass) {
        this.taskOwner = entityIn;
        this.world = entityIn.level;
        this.mateClass = mateClass;
        this.executionChance = chance;
        this.moveSpeed = speedIn;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.taskOwner.isInLove() || this.taskOwner.getAge() != 0 || this.taskOwner.getRandom().nextInt(this.executionChance) != 0) {
            return false;
        }
        this.targetMate = this.getNearbyMate();
        return this.targetMate != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetMate.isAlive() && this.taskOwner.getAge() == 0 && this.spawnBabyDelay < 200
                && this.travelTicks < (this.longRange ? LONG_RANGE_TRAVEL_BUDGET : TRAVEL_BUDGET);
    }

    @Override
    public void stop() {
        this.targetMate = null;
        this.spawnBabyDelay = 0;
        this.travelTicks = 0;
        this.longRange = false;
        this.repathDelay = 0;
    }

    @Override
    public void tick() {
        this.taskOwner.getLookControl().setLookAt(this.targetMate, 10.0F, (float) this.taskOwner.getHeadRotSpeed());
        if (!this.longRange || --this.repathDelay <= 0 || this.taskOwner.getNavigation().isDone()) {
            this.repathDelay = 10;
            this.taskOwner.getNavigation().moveTo(this.targetMate.getX(), this.targetMate.getY(), this.targetMate.getZ(), this.moveSpeed);
        }

        /* Only the time spent actually together counts toward breeding; walking there burns the travel
         * budget instead. Both partners run this goal, so each closes half the gap, and the budget is
         * what releases the pair when a mate turns out to be across a ravine. */
        if (this.taskOwner.distanceToSqr(this.targetMate) < MATING_DIST_SQR) {
            ++this.spawnBabyDelay;
        }
        else {
            ++this.travelTicks;
            return;
        }

        if (this.spawnBabyDelay >= 100) {
            this.taskOwner.resetLove();
            this.targetMate.resetLove();
            if (this.world.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                this.world.addFreshEntity(new ExperienceOrb(this.world, this.taskOwner.getX(), this.taskOwner.getY(), this.taskOwner.getZ(), this.taskOwner.getRandom().nextInt(7) + 1));
            }
            // Positive Growing Age is used as pregnancy counter (handled in ComplexMob)
            this.taskOwner.setAge(this.taskOwner.getPregnancyTime());
            this.targetMate.setAge(this.taskOwner.getPregnancyTime());
            if (this.taskOwner instanceof INestingMob nestingMob && nestingMob.isEggLayer()) {
                if (!this.taskOwner.isMale() || !ConfigGamerules.genderedBreeding.get())
                    ((INestingMob)this.taskOwner).setEggStatus(true);
                else
                    ((INestingMob)this.targetMate).setEggStatus(true);
            }
            else if (ConfigGamerules.easyBreeding.get()) {
                if (!this.taskOwner.isMale() || !ConfigGamerules.genderedBreeding.get())
                    this.taskOwner.breed();
                else
                    this.targetMate.breed();
            }
        }
    }

    private ComplexMob getNearbyMate() {
        this.longRange = false;
        ComplexMob mate = this.findMate(ConfigGamerules.mateSearchRadius.get().doubleValue());
        /* Pack animals always have a partner at hand; a solitary one almost never does. A lone tiger or
         * bear sixty blocks from the nearest partner simply never bred, which is a large part of why the
         * solitary predators died out while wolf packs grew. So a solitary animal that finds nobody close
         * looks much further. This only runs while in season, after the near search failed, and on the
         * goal's own 1-in-120 roll, so the wide scan is rare. */
        int wide = ConfigGamerules.solitaryMateSearchRadius.get();
        boolean solitary = this.taskOwner.herd == null || this.taskOwner.herd.getMaxSize() <= 1;
        if (mate == null && solitary && wide > ConfigGamerules.mateSearchRadius.get()) {
            mate = this.findMate(wide);
            this.longRange = mate != null;
        }
        return mate;
    }

    private ComplexMob findMate(double radius) {
        /* The old 8-block box meant a species had to be crowded to breed at all. Predators are sparse and
         * wander widely, so two lone dire wolves thirty blocks apart never saw each other and the
         * population could only ever shrink -- and nothing anywhere made an animal in season go looking. */
        List<? extends ComplexMob> list = this.world.getEntitiesOfClass(mateClass, this.taskOwner.getBoundingBox().inflate(radius));
        list.remove(this.taskOwner);
        double d0 = Double.MAX_VALUE;
        ComplexMob entityanimal = null;
        for (ComplexMob potentialMates : list) {
            if (canMateWith(this.taskOwner, potentialMates) && this.taskOwner.distanceToSqr(potentialMates) < d0) {
                entityanimal = potentialMates;
                d0 = this.taskOwner.distanceToSqr(potentialMates);
            }
        }
        return entityanimal;
    }

    private boolean canMateWith(ComplexMob father, ComplexMob mother) {
        if ((ConfigGamerules.genderedBreeding.get() && father.getGender() == mother.getGender()) || father.getVariant() != mother.getVariant()) {
            return false;
        }
        else if (father instanceof INestingMob nesting && (nesting.wantsToLayEggs() || ((INestingMob)mother).wantsToLayEggs())) {
            return false;
        }
        return ConfigGamerules.playerBreeding.get() || (father.wantsToBreed() && mother.wantsToBreed());
    }
}