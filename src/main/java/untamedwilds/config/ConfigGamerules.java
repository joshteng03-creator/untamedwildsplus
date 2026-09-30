package untamedwilds.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigGamerules {

    public static ForgeConfigSpec.BooleanValue naturalBreeding;
    public static ForgeConfigSpec.BooleanValue hardcoreBreeding;
    public static ForgeConfigSpec.BooleanValue easyBreeding;
    public static ForgeConfigSpec.BooleanValue genderedBreeding;
    public static ForgeConfigSpec.BooleanValue hardcoreDeath;
    public static ForgeConfigSpec.BooleanValue playerBreeding;
    public static ForgeConfigSpec.BooleanValue randomSpecies;
    public static ForgeConfigSpec.BooleanValue scientificNames;
    public static ForgeConfigSpec.BooleanValue grazerGriefing;
    public static ForgeConfigSpec.BooleanValue mobGriefing;
    public static ForgeConfigSpec.BooleanValue angrySleepers;
    public static ForgeConfigSpec.BooleanValue contactAgression;
    public static ForgeConfigSpec.BooleanValue mobsLayEggs;
    public static ForgeConfigSpec.BooleanValue spyglassBehaviorChange;
    public static ForgeConfigSpec.IntValue spyglassCheckRange;
    public static ForgeConfigSpec.BooleanValue sleepBehaviour;
    public static ForgeConfigSpec.BooleanValue easyMobCapturing;
    public static ForgeConfigSpec.IntValue cycleLength;
    public static ForgeConfigSpec.DoubleValue gestationMultiplier;
    public static ForgeConfigSpec.DoubleValue maturityMultiplier;
    public static ForgeConfigSpec.DoubleValue rareSkinChance;
    public static ForgeConfigSpec.BooleanValue wildRareSkins;
    public static ForgeConfigSpec.BooleanValue attackUndead;
    public static ForgeConfigSpec.IntValue predatorHuntCommitTicks;
    public static ForgeConfigSpec.IntValue predatorFailedHuntCooldown;
    public static ForgeConfigSpec.IntValue predatorKillCooldown;
    public static ForgeConfigSpec.DoubleValue predatorChaseBurst;
    public static ForgeConfigSpec.IntValue preyFleeStaminaTicks;
    public static ForgeConfigSpec.DoubleValue preyExhaustedFleeFactor;
    public static ForgeConfigSpec.DoubleValue preyWoundedFleeFactor;
    public static ForgeConfigSpec.IntValue predatorHerdFloor;
    public static ForgeConfigSpec.DoubleValue predatorHungerDrain;
    public static ForgeConfigSpec.DoubleValue predatorHuntRiskChance;
    public static ForgeConfigSpec.DoubleValue predatorHuntAbandonHealth;
    public static ForgeConfigSpec.IntValue mateSearchRadius;
    public static ForgeConfigSpec.DoubleValue predatorHuntDamageBurst;
    public static ForgeConfigSpec.DoubleValue packBurstSharing;
    public static ForgeConfigSpec.DoubleValue herbivoreChargeMultiplier;
    public static ForgeConfigSpec.IntValue herdDefendRadius;
    public static ForgeConfigSpec.DoubleValue bigCatAmbushMultiplier;
    public static ForgeConfigSpec.DoubleValue bigCatCorneredReduction;
    public static ForgeConfigSpec.DoubleValue packHunterDamageReduction;
    public static ForgeConfigSpec.DoubleValue loneHunterDamageReduction;
    public static ForgeConfigSpec.IntValue carcassFoodPerHealth;
    public static ForgeConfigSpec.IntValue solitaryMateSearchRadius;
    public static ForgeConfigSpec.IntValue bearStaggerTicks;
    public static ForgeConfigSpec.IntValue canidStaminaDrain;
    public static ForgeConfigSpec.DoubleValue hyenaPackDamageStep;
    public static ForgeConfigSpec.DoubleValue predatorGestationMultiplier;
    public static ForgeConfigSpec.DoubleValue predatorMaturityMultiplier;
    public static ForgeConfigSpec.IntValue predatorLitterBonus;
    public static ForgeConfigSpec.BooleanValue defaultEcosystemMode;
    public static ForgeConfigSpec.IntValue herdDefenderCap;
    public static ForgeConfigSpec.IntValue herdDefenderEngageCooldown;
    public static ForgeConfigSpec.DoubleValue predatorOddsRatio;
    public static ForgeConfigSpec.IntValue packRoutLossThreshold;
    public static ForgeConfigSpec.IntValue packRoutWindow;
    public static ForgeConfigSpec.IntValue packRoutPressure;
    public static ForgeConfigSpec.IntValue predatorRoutAvoidTicks;
    public static ForgeConfigSpec.IntValue crowdingRadius;
    public static ForgeConfigSpec.DoubleValue localCapacityFactor;
    public static ForgeConfigSpec.IntValue forageStressTicks;
    public static ForgeConfigSpec.IntValue migrationMinDistance;
    public static ForgeConfigSpec.IntValue migrationMaxDistance;
    public static ForgeConfigSpec.IntValue migrationCooldown;
    public static ForgeConfigSpec.IntValue dispersalLockout;
    public static ForgeConfigSpec.IntValue predatorRelocateTicks;
    public static ForgeConfigSpec.DoubleValue preySwitchingStrength;
    public static ForgeConfigSpec.IntValue carrionHunger;
    public static ForgeConfigSpec.IntValue kleptoparasitismClanSize;
    public static ForgeConfigSpec.IntValue alarmNetworkRadius;
    public static ForgeConfigSpec.DoubleValue sentinelDetectBonus;
    public static ForgeConfigSpec.DoubleValue stragglerBias;
    public static ForgeConfigSpec.IntValue starvationGraceTicks;
    public static ForgeConfigSpec.IntValue starvationDamageInterval;
    public static ForgeConfigSpec.DoubleValue starvationDamage;
    public static ForgeConfigSpec.IntValue conditionHalflifeTicks;
    public static ForgeConfigSpec.DoubleValue conditionWeakThreshold;
    public static ForgeConfigSpec.DoubleValue breedingConditionThreshold;
    public static ForgeConfigSpec.DoubleValue starvedSpeedFactor;
    public static ForgeConfigSpec.DoubleValue lifespanMultiplier;
    public static ForgeConfigSpec.DoubleValue senescenceFraction;
    public static ForgeConfigSpec.DoubleValue oldAgeMortalityChance;
    public static ForgeConfigSpec.IntValue forageYieldMax;
    public static ForgeConfigSpec.IntValue forageYieldMin;
    public static ForgeConfigSpec.IntValue forageRichDensity;
    public static ForgeConfigSpec.IntValue forageRegrowthInterval;
    public static ForgeConfigSpec.IntValue forageRegrowthAttempts;

    ConfigGamerules(final ForgeConfigSpec.Builder builder) {
        builder.comment("Options pertaining to global Gamerules");

        naturalBreeding = builder.comment("Defines whether animals should breed without Player intervention.").define("gamerules.natural_breeding", true);
        hardcoreBreeding = builder.comment("Adds additional restrictions to mob breeding, including Biome/Temperature requirements and Overcrowding.").define("gamerules.hardcore_breeding", false);
        easyBreeding = builder.comment("Pregnancy time is only used as a cooldown, babies pop out instantly like in Vanilla.").define("gamerules.easy_breeding", false);
        genderedBreeding = builder.comment("Whether breeding requires a Male and a Female to produce offspring/eggs. (Warning: may lead to uncontrolled spawns of eggs)").define("gamerules.gendered_breeding", true);
        playerBreeding = builder.comment("Defines whether players can trigger breeding by feeding a creature's favourite item, like in vanilla.").define("gamerules.player_breeding", false);

        randomSpecies = builder.comment("Allows mobs to spawn as fully random species, ignoring Biomes and Rarity.").define("gamerules.random_species", false);
        rareSkinChance = builder.comment("Chance for a mob, out of 1, to have it's Skin replaced by a Rare skin (if any are defined through assets)").defineInRange("gamerules.rare_skin_chance", 0.05, 0, 1);
        wildRareSkins = builder.comment("Should mobs with Rare skins generate in the wild (if defined through assets)").define("gamerules.wild_rare_skins", true);
        spyglassBehaviorChange = builder.comment("Should the vanilla Spyglass display information when a player looks at a mob. Set to false to disable.").define("gamerules.spyglass_behavior_change", true);
        spyglassCheckRange = builder.comment("Range up to which the Spyglass will identify mobs and give information, this length is not in blocks, and is roughly equivalent to a Render distance of 12.").defineInRange("gamerules.spyglass_range", 5000, 0, Integer.MAX_VALUE);

        hardcoreDeath = builder.comment("Disable this option to have tamed mobs respawn in their home with half a Heart if they were to 'die' (IMPORTANT: This gamerule is NOT fully functional and using it as a free get-out-of-jail card is bound to be disappointing, use at your own risk).").define("gamerules.hardcore_death", true);
        scientificNames = builder.comment("Features scientific names in various descriptions (eg. for mobs inside Cage Traps).").define("gamerules.scientific_names", true);
        grazerGriefing = builder.comment("Should 'Grazing' mobs destroy Tall Grass and/or turn Grass into dirt blocks (like Vanilla Sheep do).").define("gamerules.grazer_griefing", true);
        mobGriefing = builder.comment("Should mobs potentially destroy the terrain? Keep in mind 'mobGriefing' is still required").define("gamerules.mob_griefing", false);
        mobsLayEggs = builder.comment("If set to false, prevents mobs from dropping eggs").define("gamerules.mobs_drop_eggs", false);

        angrySleepers = builder.comment("Defines whether certain large predators will be angered if a player approaches them while they are sleeping.").define("gamerules.angry_sleepers", true);
        contactAgression = builder.comment("Defines whether certain critters will become angry if a mob/player 'steps' on them, by coming too close.").define("gamerules.contact_agression", true);
        attackUndead = builder.comment("Defines whether animals should actively target and hunt Undead mobs. Disabling this option should make mobs less prone to dying due to angering a Zombie horde").define("gamerules.attack_undead", true);

        sleepBehaviour = builder.comment("Should the 'Sleeping' behaviour run? Disabling this option also disables the activity").define("gamerules.mob_sleeping", true);
        easyMobCapturing = builder.comment("If set to false, makes mobs a lot harder to catch by preventing the capture of hostile mobs").define("gamerules.easy_mob_capture", true);

        cycleLength = builder.comment("Defines how long a cycle should last, cycles are used to scale the gestation and breeding periods",
                "Example values: 24000 - Day, 168000 - Week, 720000 - Month, 8760000 - Year").defineInRange("gamerules.cycle_length", 24000, 0, 8760000);

        gestationMultiplier = builder.comment("Scales how long pregnancy lasts, on top of cycle_length. Lower values mean animals reproduce faster.",
                "At 1.0 a Mammoth is pregnant for 16 in-game days, which means most populations never recover from predation.").defineInRange("gamerules.gestation_multiplier", 0.35, 0.01, 10.0);
        maturityMultiplier = builder.comment("Scales how long babies take to reach adulthood, on top of cycle_length. Lower values mean herds replenish faster.",
                "Adulthood is twice the gestation period before this multiplier is applied.").defineInRange("gamerules.maturity_multiplier", 0.5, 0.01, 10.0);

        /* Predation balance. These exist as a group because they only make sense against each other:
         * raising the chase burst without shortening the commitment window, or lowering the kill
         * cooldown without raising the herd floor, tips predation from "never lands a kill" straight
         * into "thins the herd faster than it breeds". Read the comments as a set. */
        predatorHuntCommitTicks = builder.comment("How long, in ticks, a predator presses a single hunt to the kill. While committed it ignores the low-health break-off, keeps its target out of sight, and gets the chase burst.",
                "Lower values mean more prey escape. 600 is 30 seconds.").defineInRange("gamerules.predator_hunt_commit_ticks", 600, 20, 24000);
        predatorFailedHuntCooldown = builder.comment("Ticks a predator waits before trying again after a hunt that did NOT end in a kill. At 1200 a hungry predator gets roughly one attempt per minute.").defineInRange("gamerules.predator_failed_hunt_cooldown", 1200, 0, 72000);
        predatorKillCooldown = builder.comment("Ticks a predator (and every pack-mate that shared the carcass) waits before hunting again after a successful kill. This is the main brake on overkill, on top of the hunger gate.").defineInRange("gamerules.predator_kill_cooldown", 6000, 0, 72000);
        predatorChaseBurst = builder.comment("Pursuit speed multiplier applied only while a predator is committed to a hunt. Fleeing prey is faster than every predator at rest, so without some burst a chase can never be won; too much and nothing ever escapes.").defineInRange("gamerules.predator_chase_burst", 1.15, 1.0, 2.0);

        preyFleeStaminaTicks = builder.comment("How long, in ticks, prey can run flat out before tiring. Exhausted animals flee at prey_exhausted_flee_factor until they have recovered. 200 is 10 seconds.").defineInRange("gamerules.prey_flee_stamina_ticks", 200, 0, 24000);
        preyExhaustedFleeFactor = builder.comment("Flight speed multiplier for prey that has run itself out of breath. This, and not predator damage, is what decides whether a chase can ever be won.").defineInRange("gamerules.prey_exhausted_flee_factor", 0.6, 0.1, 1.0);
        preyWoundedFleeFactor = builder.comment("Additional flight speed multiplier for prey below a quarter of its health. Stacks with the exhaustion penalty.").defineInRange("gamerules.prey_wounded_flee_factor", 0.8, 0.1, 1.0);
        predatorHerdFloor = builder.comment("Predators will not hunt an animal whose herd is down to this many living members or fewer, unless they are starving. A hard floor against a herd being wiped out locally. Set to 0 to disable.",
                "Calves are exempt -- the floor protects breeding stock, not the part of a herd predation is meant to take -- as are species that are solitary by design, which would otherwise be unhuntable by anything.").defineInRange("gamerules.predator_herd_floor", 2, 0, 64);

        predatorHungerDrain = builder.comment("Scales how fast carnivores get hungry, and so how often they hunt. Herbivore hunger is unaffected; they refill it by grazing.",
                "Lower values mean fewer kills per herd per day. Raise this only if predators are starving, not if they simply look idle.").defineInRange("gamerules.predator_hunger_drain", 1.0, 0.1, 3.0);
        predatorHuntRiskChance = builder.comment("Chance, rolled once when a predator commits to a hunt, that the quarry will fight to finish it rather than sparing it at low health. Doubled when the quarry is megafauna.",
                "This is the only way a predator dies of hunting. The share of hunts that actually kill the hunter is far LOWER than this number, because a risky hunt still only turns fatal if the predator genuinely loses the fight. Set to 0 to make hunting consequence-free again.").defineInRange("gamerules.predator_hunt_risk_chance", 0.15, 0, 1);
        predatorHuntAbandonHealth = builder.comment("Fraction of its health at which a predator gives up a hunt it was not committed to dying for, and runs. Deliberately well above the 10% at which ordinary fights break off, so a hunter that loses walks away wounded rather than at death's door.",
                "Only hunts NOT rolled risky by predator_hunt_risk_chance can be abandoned. Lower this if predators give up too easily; set it to 0 to restore the old behaviour where every hunt was fought to the death.").defineInRange("gamerules.predator_hunt_abandon_health", 0.35, 0, 1);

        mateSearchRadius = builder.comment("How far, in blocks, an animal in season will look for a mate and then walk to reach one. The old 8-block search meant scattered species -- predators especially -- could almost never find a partner.",
                "Costs a periodic entity scan of this radius per animal in season, so raising it a long way has a performance price.").defineInRange("gamerules.mate_search_radius", 24, 8, 64);
        solitaryMateSearchRadius = builder.comment("Second, wider mate search for SOLITARY animals (no herd, or a herd of one) when the normal search finds nobody. Pack animals always have mates at hand; a lone tiger or bear 60 blocks from the nearest partner never bred, which is a large part of why solitary predators died out.",
                "Only runs occasionally, while in season and after the normal search failed. Set to 0 to disable.").defineInRange("gamerules.solitary_mate_search_radius", 96, 0, 160);

        /* Predator strength and class identity. Every one of these is deliberately a channel that
         * ComplexMob.getEcoLevel() cannot see -- it reads MAX_HEALTH and the ATTACK_DAMAGE attribute, and
         * nothing else. That is why the damage bonuses below are transient modifiers torn down inside the
         * attack that created them, and why durability is expressed as damage reduction rather than as raw
         * health: raising a dire wolf from 34 to 55 HP lifts its pack's eco level from 15.0 to 16.9 and
         * brings seven new prey species into range past the diet filter (giraffe and cuvieronius among
         * them). Reduction achieves the same survivability and re-sorts nothing.
         * Note also that ARMOR is NOT a lever here and should not be added as one: MC's
         * CombatRules.getDamageAfterAbsorb reduces by max(armor/5, armor - damage/2)/25, which for the
         * bear's 4 armor against an 8-damage bite is 3.2%. Nor is the attack cooldown -- the extra
         * fast-swing clause in SmartMeleeAttackGoal only fires when the mob's animation has ended, and
         * every bear attack animation but BITE is longer than the 20-tick cooldown. */
        predatorHuntDamageBurst = builder.comment("Damage multiplier applied to a predator's melee attack, but ONLY while it is committed to a hunt for food. The exact counterpart of predator_chase_burst, which does the same for pursuit speed.",
                "Zero effect in a brawl, in defence of young, or in retaliation, so raising it does not make predators more dangerous to anything that is not already their prey. It also cannot change what a predator hunts: the bonus is a transient modifier that exists only for the duration of one attack, so getEcoLevel never observes it. Set to 1.0 to disable.").defineInRange("gamerules.predator_hunt_damage_burst", 2.0, 1.0, 3.0);
        /* The burst above is granted to each hunter INDEPENDENTLY, and HuntPackMobTarget gives a whole pack
         * one shared victim, so eight dire wolves on one bison delivered eight times it: 8 x (9 x 2.0) =
         * 144 damage per second against a herd that can answer with at most herd_defender_cap x 7 = 21.
         * Sharing it across the animals actually in the fight leaves a lone hunter untouched -- a solitary
         * cat or bear was never the problem -- and takes the multiplication out of mobbing. */
        packBurstSharing = builder.comment("How strongly predator_hunt_damage_burst is divided among pack-mates committed to the SAME animal. 0 gives every member the full burst (the original behaviour); 1.0 divides it evenly, so a pack of eight does no more burst damage in total than a lone hunter.",
                "0.5 is the square-root middle ground: a solitary predator keeps the full x2.0, four wolves get x1.5 each and eight get x1.35 each. This only ever applies while committed to a food hunt, and only counts pack-mates within 12 blocks of the victim, so it never touches a solitary hunter, a brawl or defence of young.").defineInRange("gamerules.pack_burst_sharing", 0.5, 0.0, 1.0);
        herbivoreChargeMultiplier = builder.comment("Damage multiplier on a connected herbivore charge -- the exact counterpart of predator_hunt_damage_burst, and the herbivore answer to it. A bison charge telegraphs for 50 ticks and then landed a bare 7 damage, against a wolf bite of 18 with no wind-up at all.",
                "Delivered as a transient modifier torn down inside the attack, so getEcoLevel never observes it and nothing changes about what hunts a bison or what flees one. Only the charge gets it; an ordinary bite does not. Set to 1.0 to disable.").defineInRange("gamerules.herbivore_charge_multiplier", 2.0, 1.0, 4.0);
        herdDefendRadius = builder.comment("How far a herd member will look for a herd-mate under attack before coming to help. This is the trigger for the only group defence a herd has that does not require a calf to be present: ProtectChildrenTarget needs a same-variant baby within 8 blocks, so a herd with no calves nearby previously had NO group defence whatsoever.",
                "Bounded by herd_defender_cap exactly as calf defence is -- it claims from the same pool of defender slots -- so widening it brings help from further away, never more of it. Set to 0 to disable.").defineInRange("gamerules.herd_defend_radius", 12, 0, 32);

        bigCatAmbushMultiplier = builder.comment("Damage multiplier on a big cat's opening strike of a hunt. Fires once per hunt, and only against quarry that has not already engaged it -- a cat that is being fought back does not get it. Sabre-toothed species add a further +0.5.",
                "This is the big cats' signature, the counterpart of the bear's low-health Strength buff. It STACKS ADDITIVELY with predator_hunt_damage_burst, because both are amounts on the same MULTIPLY_TOTAL modifier: at the defaults a committed lion opens at x2.75 and a sabertooth at x3.25. Set to 1.0 to disable.").defineInRange("gamerules.bigcat_ambush_multiplier", 1.75, 1.0, 4.0);
        bigCatCorneredReduction = builder.comment("Fraction of incoming damage a big cat below half health ignores. The mirror of the bear's Bearserk: a wounded bear gets more dangerous, a wounded cat gets harder to finish.",
                "Deliberately damage reduction and not a Strength-style buff. MobEffects.DAMAGE_BOOST is attribute-backed, so it raises ATTACK_DAMAGE and therefore the animal's eco level mid-fight, changing what it hunts and what flees it. Set to 0 to disable.").defineInRange("gamerules.bigcat_cornered_reduction", 0.20, 0.0, 0.75);
        packHunterDamageReduction = builder.comment("Fraction of incoming damage hyenas and dire wolves ignore. They are the thinnest-skinned predators in the mod -- a dire wolf is 34 HP and a spotted hyena 20 -- and a wooly rhino hits for 10, so they died in four bites and two respectively while hunting animals their pack eco level said they could take.",
                "Expressed as reduction rather than as extra health on purpose; see the note above this block. At 0.3 a dire wolf's effective health is 48.6 and a rhino needs six hits instead of four.",
                "Applies only against prey, defenders and players -- not in fights with other predators and not to starvation or other damage with no attacker, which is where it used to make packs win every predator brawl and outlast every famine. Set to 0 to disable.").defineInRange("gamerules.pack_hunter_damage_reduction", 0.30, 0.0, 0.75);
        loneHunterDamageReduction = builder.comment("Fraction of prey/defender damage a SOLITARY carnivore ignores while committed to a hunt. A pack spreads the herd's three defender slots across eight attackers; a lone cat or bear takes all three itself, and was the predator that kept dying. Never stacks past the best single reduction.",
                "Set to 0 to disable.").defineInRange("gamerules.lone_hunter_damage_reduction", 0.30, 0.0, 0.75);
        carcassFoodPerHealth = builder.comment("Hunger a kill provides per point of the prey's max health, split between everyone who eats from it (the killer, plus pack-mates within 16 blocks). The killer still caps at +120 and each pack-mate at +90, so a lone hunter or small pack on decent prey eats exactly as before.",
                "What changes is big packs: one carcass used to feed ANY number of wolves in full, so pack size never cost anything and packs grew without limit. Now food per wolf falls as the pack grows, the condition gate stops breeding and starvation trims the surplus. Set to 0 for the old unlimited sharing.").defineInRange("gamerules.carcass_food_per_health", 8, 0, 100);
        bearStaggerTicks = builder.comment("Ticks of Slowness a bear's blow inflicts. Bears are the slowest predators in the mod (0.15-0.24) so anything they pick a fight with simply walks away; this is what makes them inescapable at close range rather than making them faster.",
                "Pairs with the ATTACK_KNOCKBACK of 1.0 they already carry, the highest of any mob here. Set to 0 to disable.").defineInRange("gamerules.bear_stagger_ticks", 40, 0, 200);
        canidStaminaDrain = builder.comment("Flight stamina a dire wolf's bite takes out of its quarry. The canid signature: a wolf pack does not win the sprint, it wins the marathon, and prey_flee_stamina_ticks already models the marathon.",
                "Measured against that budget (200 by default), so at 40 five landed bites blow the quarry outright and it drops to prey_exhausted_flee_factor. Set to 0 to disable.").defineInRange("gamerules.canid_stamina_drain", 40, 0, 200);
        hyenaPackDamageStep = builder.comment("Extra damage fraction a hyena gains per clan-mate engaged on the same animal, capped at +50%. The hyena signature: individually the weakest large carnivore in the mod at attack 5, and dangerous only as a mob.",
                "Self-limiting, since it requires clan-mates to be on the same target and within 8 blocks of it, which the circling attack goal only makes true intermittently. Set to 0 to disable.").defineInRange("gamerules.hyena_pack_damage_step", 0.10, 0.0, 0.5);

        /* Carnivore-only breeding. gestation_multiplier and maturity_multiplier above move every animal in
         * the mod together, which helps prey more than predators -- a herd of twenty breeding faster
         * outpaces a pack of eight doing the same. These two stack on top of those for carnivores only.
         * This is the one part of the predator rebalance that can genuinely destabilise a world: nothing
         * else here raises the KILL RATE, which is bounded by the hunger gate and the kill cooldown, but
         * more predators reaching adulthood means more animals each running their own hunger clock. If
         * herds start shrinking, pull predator_hunger_drain down first, then raise predator_herd_floor. */
        predatorGestationMultiplier = builder.comment("Scales carnivore pregnancy length on top of gestation_multiplier. Lower values mean predators reproduce faster.",
                "Predators are sparse, slow to mature and the only animals that can die doing their job, so at the shared rate their populations shrink to nothing over a long world.").defineInRange("gamerules.predator_gestation_multiplier", 0.33, 0.01, 2.0);
        predatorMaturityMultiplier = builder.comment("Scales how long carnivore young take to reach adulthood, on top of maturity_multiplier. Lower values mean packs and prides replenish faster.",
                "Cubs count for only BABY_ECO_FACTOR of an adult's eco level and are barred from every attack goal, so a long juvenile period is time a pack spends carrying members that cannot hunt.").defineInRange("gamerules.predator_maturity_multiplier", 0.33, 0.01, 2.0);
        predatorLitterBonus = builder.comment("Added to a carnivore's litter size from the species JSON. The realised litter is 1 to (offspring + 1), so this widens the top of that range.",
                "Worth knowing that the spotted hyena ships with offspring:1, the smallest litter of any predator in the mod despite a clan size of 20, which is much of why they vanish from old worlds. Set to 0 to disable.").defineInRange("gamerules.predator_litter_bonus", 2, 0, 6);

        /* ---------------------------------------------------------------------------------------------
         * Ecosystem simulation. Everything below exists to answer one structural problem: predators were
         * dying to PREY, not to other predators. Group defence had no cap, so every adult standing near
         * any calf piled onto the same hunter; a dozen bison at attack 7 kill a dire wolf in two seconds
         * no matter how strong the wolf is. Nothing in the mod assessed odds or casualties either, so a
         * pack that had lost half its members kept fighting until the last one fell.
         * And prey could never resolve its own density: herds bred in place and never moved, so a range
         * that was eaten out stayed occupied and the local prey count only ever climbed.
         * The three fixes are a defender cap, group morale, and MOVEMENT as the density-dependent
         * response -- crowded or starved herds shift ground and surplus young disperse, rather than
         * births being capped. They are mutually load-bearing: raising the defender cap without also
         * raising predator_odds_ratio puts predators straight back into fights they cannot win. */
        defaultEcosystemMode = builder.comment("Default value of the untamedwildsEcosystemMode gamerule for NEW worlds. True is Ecosystem (a self-regulating predator/prey simulation), false is Zoo (slow breeding, calm animals, predators that hunt only when starving and never brawl).",
                "This is only the default. The live setting is the per-world gamerule, which is saved in level.dat and can be flipped mid-game with /gamerule untamedwildsEcosystemMode <true|false>.").define("gamerules.default_ecosystem_mode", true);

        herdDefenderCap = builder.comment("How many members of one herd may fight the same threat at once. The rest bunch and back away, which is what real herd defence looks like -- bison and muskox put a few animals into a front, they do not all charge.",
                "This is THE number that decides whether predators survive. It was effectively unlimited: ProtectChildrenTarget runs independently on every adult, so in a 20-strong herd a dozen mothers engaged one wolf simultaneously. Raise it only alongside predator_odds_ratio. Zoo mode forces 1.").defineInRange("gamerules.herd_defender_cap", 3, 1, 32);
        herdDefenderEngageCooldown = builder.comment("Ticks a herd member waits before it may take a defender slot again after giving one up. Without it the same fight simply cycles a fresh trio of defenders through the cap and the total damage is unchanged.").defineInRange("gamerules.herd_defender_engage_cooldown", 600, 0, 24000);

        predatorOddsRatio = builder.comment("Defenders per hunter a predator will accept before it refuses to start a hunt at all. At 1.5 a lone hyena will not commit with two bison already squared up, but a clan of twelve still will.",
                "Checked only when a hunt STARTS -- a predator already committed is governed by the commitment timer and the morale rules below, not by this. Set to 0 to disable the check (and the defender scan it costs).").defineInRange("gamerules.predator_odds_ratio", 1.5, 0.0, 10.0);
        packRoutLossThreshold = builder.comment("Members a pack may lose within pack_rout_window before the whole pack breaks off and runs. Set to 0 to disable group morale entirely.",
                "This is the fix for packs fighting to the last animal: break-off used to be per-individual and health-based only, so a wolf at full health next to three dead pack-mates had no reason to leave.").defineInRange("gamerules.pack_rout_loss_threshold", 2, 0, 16);
        packRoutWindow = builder.comment("Ticks over which pack losses and combat pressure decay. Losses older than this no longer count toward a rout.").defineInRange("gamerules.pack_rout_window", 600, 20, 24000);
        packRoutPressure = builder.comment("Accumulated hits from animals a pack is NOT hunting -- herd members defending their own -- that will rout the pack even if nobody has died. This is what makes a pack give up on a herd that is simply too well defended.").defineInRange("gamerules.pack_rout_pressure", 12, 0, 200);
        predatorRoutAvoidTicks = builder.comment("Ticks a routed predator refuses to target anything from the herd that beat it. Without this the pack re-acquires the same herd seconds later and routing achieves nothing.",
                "Same lesson as the hunt commitment: a decision that lives only on a goal does not survive the goal being preempted, so it has to be a timer on the animal.").defineInRange("gamerules.predator_rout_avoid_ticks", 2400, 0, 72000);

        /* Density dependence, expressed as MOVEMENT. A birth cap would stop the population growing at
         * all; what real ungulates do instead is leave. Herds shift range when the forage runs out or
         * the neighbourhood fills up, and surplus young adults disperse to found herds elsewhere, so the
         * population keeps growing and simply spreads. Reduced fecundity is the last resort, and only
         * when there is genuinely nowhere better to go (see forage_stress_ticks).
         * Note this finally gives grazer_griefing a purpose beyond cosmetics: a herd eats its patch down
         * to dirt, moves on, and the old range regrows behind it. */
        crowdingRadius = builder.comment("Radius, in blocks, over which an animal counts its own species when deciding whether the neighbourhood is full.").defineInRange("gamerules.crowding_radius", 48, 8, 128);
        localCapacityFactor = builder.comment("Local carrying capacity, as a multiple of the species' own herd size. Above this many conspecific adults within crowding_radius, herds start looking for somewhere else and surplus young adults disperse.",
                "This does NOT stop anything breeding. Lower it to spread populations out more thinly; raise it to let animals pack in.").defineInRange("gamerules.local_capacity_factor", 1.5, 0.5, 8.0);
        forageStressTicks = builder.comment("How long a herbivore may fail to find anything to eat before it counts its range as exhausted. Reaching this is what triggers a migration; it only suppresses breeding when a migration has ALSO failed to find anywhere better.").defineInRange("gamerules.forage_stress_ticks", 1200, 0, 24000);
        migrationMinDistance = builder.comment("Nearest a herd will consider relocating to, in blocks. Too small and the herd shuffles inside the range it just ate out.").defineInRange("gamerules.migration_min_distance", 64, 16, 192);
        migrationMaxDistance = builder.comment("Furthest a herd will relocate, in blocks. Minecraft only ticks entities in loaded chunks, so this is bounded by simulation distance (10 chunks, ~160 blocks, by default) -- this is range SHIFTING, not continental migration, and a herd in unloaded chunks does not move at all.").defineInRange("gamerules.migration_max_distance", 128, 24, 256);
        migrationCooldown = builder.comment("Ticks before a herd that has just relocated will consider moving again, so herds settle instead of ping-ponging between two patches.").defineInRange("gamerules.migration_cooldown", 6000, 0, 72000);
        dispersalLockout = builder.comment("Ticks a dispersing young adult stays closed to rejoining a herd. Without it, it merges straight back into the herd it just left and nothing disperses.").defineInRange("gamerules.dispersal_lockout", 2400, 0, 72000);
        predatorRelocateTicks = builder.comment("Ticks a hungry predator may find no prey in range before it relocates to look elsewhere. If herds migrate and packs do not, the packs simply starve where they stand -- this is what keeps the two coupled.").defineInRange("gamerules.predator_relocate_ticks", 2400, 0, 72000);

        /* Species interactions. These are the mechanisms that keep a multi-species system from collapsing
         * to one prey animal and one predator, which is what an unweighted "nearest thing I can eat"
         * targeting rule always converges to. */
        preySwitchingStrength = builder.comment("How strongly a predator prefers whichever of its prey species is locally COMMON. At 1.0 a species that has been hunted down to a few animals is largely passed over in favour of an abundant one, and recovers.",
                "This is the single most important stabiliser here: without it predators keep taking whatever is nearest and remove their prey species one at a time. Set to 0 for the old flat preference.").defineInRange("gamerules.prey_switching_strength", 1.0, 0.0, 2.0);
        carrionHunger = builder.comment("Hunger a scavenger gains from a carcass it did not bring down and that nobody hunted -- an animal that died of anything at all. Deliberately less than the 60 a stolen kill gives.",
                "Real scavengers live mostly off animals that simply died. Set to 0 to restrict scavenging to predator kills.").defineInRange("gamerules.carrion_hunger", 40, 0, 200);
        kleptoparasitismClanSize = builder.comment("Hyenas that must be present within 12 blocks to drive a big cat off its kill. Bears displace canids at any number.",
                "Set to 0 to disable displacement between predators. Default lowered from 4 to 3 (2026-09-30): hyenas are meant to live off stolen kills, and at 4 most clans rarely qualified.").defineInRange("gamerules.kleptoparasitism_clan_size", 3, 0, 32);
        alarmNetworkRadius = builder.comment("Radius over which a fleeing herbivore alarms OTHER species, not just its own herd. Mixed-species alarm networks are real and they are most of why a plains full of animals feels alive.").defineInRange("gamerules.alarm_network_radius", 24, 0, 64);
        sentinelDetectBonus = builder.comment("Multiplier on a giraffid's predator-detection distance. Tall browsers see furthest and are what the rest of the plains listens to.").defineInRange("gamerules.sentinel_detect_bonus", 1.5, 1.0, 3.0);
        stragglerBias = builder.comment("How strongly predators prefer prey that is wounded, blown, or separated from its herd. This is what makes a big herd protective WITHOUT the herd having to fight, which is the ecological answer to predators being mobbed to death.",
                "Set to 0 to have predators take whatever is nearest regardless of condition.").defineInRange("gamerules.straggler_bias", 2.0, 0.0, 8.0);

        /* ---------------------------------------------------------------------------------------------
         * Demography: the numbers that decide whether a population can STOP growing.
         *
         * Everything above this block models BEHAVIOUR -- who hunts what, who runs, who moves where.
         * None of it could ever bound a population, because the mod had no death that was not inflicted
         * by another animal and no birth rate that depended on how much food there actually was:
         *   - isStarving() was read in ~25 places and its only consequence anywhere was skipping a heal,
         *     so hunger floored at 0 and the animal lived forever. With removeWhenFarAway() false and no
         *     despawn, NOTHING removed a wild adult from the world except a player. Pack hunters in
         *     particular accumulated without limit.
         *   - Predator breeding was a bare getHunger() >= 80. A single kill pays +120 to the killer and
         *     +90 to every pack-mate within 16 blocks, so one carcass put an entire eight-wolf pack over
         *     that threshold at once -- births driven by EVENTS rather than by prey density, which is
         *     exponential growth by construction.
         *   - Grazing yielded a flat +16 against a -10/1000-tick drain on ground that was never modelled
         *     as finite, so forage was an infinite resource and herds had no carrying capacity.
         *   - Nothing aged. A cohort that reached adulthood was immortal.
         * The three answers are density-dependent MORTALITY (starvation), a LAGGED numerical response
         * (breeding on sustained condition, not on a momentary full belly), and a real K (forage that
         * depletes and regrows). They are mutually load-bearing: starvation without the condition gate
         * just kills the surplus after it has already been born, and the condition gate without
         * depletable forage never has anything to respond to.
         * ALL of this is Ecosystem-mode only. Zoo mode keeps every one of the old behaviours. */
        starvationGraceTicks = builder.comment("Ticks an animal may sit at zero hunger before starvation starts doing damage. This is the entire grace period between 'hungry' and 'dying', so it wants to be long enough that a bad afternoon's hunting is survivable. 2400 is two minutes.",
                "Set to a very large number to make starvation effectively non-lethal while keeping the condition penalties.").defineInRange("gamerules.starvation_grace_ticks", 2400, 0, 72000);
        starvationDamageInterval = builder.comment("Ticks between starvation damage ticks once the grace period has elapsed. At 200 with starvation_damage 1.0 a 34 HP dire wolf takes about six minutes to die from the moment it starts starving.").defineInRange("gamerules.starvation_damage_interval", 200, 20, 24000);
        starvationDamage = builder.comment("Damage per starvation tick. Raise this to make marginal habitat lethal faster; the interval above is usually the better knob because it does not scale with the animal's size.",
                "Babies and tamed animals never take starvation damage at all -- a calf cannot use GrazeGoal, so without that exemption every calf in the world would starve.").defineInRange("gamerules.starvation_damage", 1.0, 0.0, 20.0);

        conditionHalflifeTicks = builder.comment("How long, in ticks, it takes an animal's body condition to move halfway toward its current hunger. Condition is a slow average of hunger and is what breeding is actually gated on.",
                "This number IS the lag in the predator numerical response, and the lag is what turns runaway growth into a bounded oscillation. Short values make predators breed off a single good kill again; long values make packs slow to recover after a lean spell. 6000 is five in-game hours.").defineInRange("gamerules.condition_halflife_ticks", 6000, 200, 72000);
        conditionWeakThreshold = builder.comment("Body condition below which an animal is visibly in poor shape and moves at starved_speed_factor. Expressed as a fraction of a full belly.",
                "Deliberately a SPEED penalty and nothing else. Weakness must never be expressed through MAX_HEALTH or ATTACK_DAMAGE: both are live inputs to ComplexMob.getEcoLevel, so either would silently rewrite every targeting predicate in the mod. Starvation damage lowers health on its own, which makes a starving animal huntable through the straggler bias -- that part is intended.").defineInRange("gamerules.condition_weak_threshold", 0.30, 0.0, 1.0);
        breedingConditionThreshold = builder.comment("Body condition an animal must have SUSTAINED before it will breed. This is the single most important number for stopping predator overpopulation.",
                "Because condition is a long average, one carcass can no longer flip a whole pack into breeding at once; a pack only produces cubs after a genuinely successful run of hunting, which is what couples predator births to prey density. Lower it if predators die out, raise it if they still climb.").defineInRange("gamerules.breeding_condition_threshold", 0.60, 0.0, 1.0);
        starvedSpeedFactor = builder.comment("Movement speed multiplier for an animal in poor condition or past its prime. Applied as a transient MOVEMENT_SPEED modifier, which getEcoLevel cannot see.").defineInRange("gamerules.starved_speed_factor", 0.75, 0.1, 1.0);

        lifespanMultiplier = builder.comment("An animal's natural lifespan, as a multiple of the time it takes to reach adulthood. Derived rather than authored per species, so it automatically inherits cycle_length, maturity_multiplier and the carnivore predator_maturity_multiplier and keeps the same maturity-to-lifespan ratio for every animal in the mod.",
                "At the default 8.0 a dire wolf lives roughly eight times as long as it took to grow up. Set to a very large number to make animals effectively immortal again.").defineInRange("gamerules.lifespan_multiplier", 8.0, 1.0, 100.0);
        senescenceFraction = builder.comment("Fraction of its lifespan at which an animal becomes old: it stops breeding and slows to starved_speed_factor. Predators also start preferring it as prey, through the same straggler bias that picks out wounded animals.",
                "Senescence is what guarantees turnover. Without it a cohort that survives to adulthood in a safe biome never dies of anything.").defineInRange("gamerules.senescence_fraction", 0.75, 0.1, 1.0);
        oldAgeMortalityChance = builder.comment("Chance per 1000 ticks that an animal past its full lifespan dies of old age. Scales from zero at the lifespan up to this value at 1.5x it, so deaths spread out instead of a whole cohort dropping at once.",
                "Old-age deaths go through the normal death path, so the carcass feeds scavengers -- which is most of how a hyena clan or a bear makes a living without hunting. Set to 0 to disable death by old age while keeping the senescent breeding stop.").defineInRange("gamerules.old_age_mortality_chance", 0.02, 0.0, 1.0);

        forageYieldMax = builder.comment("Hunger a mouthful restores on rich, ungrazed ground. This was a flat 16 everywhere, which is what made forage an infinite resource: one mouthful per ~1600 ticks sustained an animal indefinitely no matter how bare the range was.").defineInRange("gamerules.forage_yield_max", 16, 1, 200);
        forageYieldMin = builder.comment("Hunger a mouthful restores on ground that has been grazed out. The gap between this and forage_yield_max is the whole carrying-capacity mechanism: a herd that has eaten its range down can no longer feed itself there, accrues forage stress, and migrates.",
                "Do not set this equal to forage_yield_max -- that restores the old infinite-forage behaviour.").defineInRange("gamerules.forage_yield_min", 4, 0, 200);
        forageRichDensity = builder.comment("How many grazeable blocks within GrazeGoal's search radius count as fully rich ground. Below this the mouthful yield falls off toward forage_yield_min in proportion.",
                "Reuses the search the goal already runs, so this costs nothing extra.").defineInRange("gamerules.forage_rich_density", 12, 1, 128);
        forageRegrowthInterval = builder.comment("Ticks between passes of the forage regrowth handler. Grazed ground has to come back on an ecological timescale or the world converts to dirt permanently -- vanilla grass spread is far slower than a herd eats.",
                "This is the other half of grazer_griefing: a herd eats a patch to dirt, moves on, and the range regrows behind it. Set to 0 to disable regrowth entirely.").defineInRange("gamerules.forage_regrowth_interval", 200, 0, 24000);
        forageRegrowthAttempts = builder.comment("Regrowth attempts per player per pass. Each attempt probes one random surface column near a player: dirt beside grass becomes grass again, and bare grass regrows short grass or fern.",
                "Sampling follows players because Minecraft only ticks loaded chunks, so that is the only ground where a herd could have eaten anything in the first place. At the defaults this restores roughly what one large herd consumes; raise it if ranges stay bare, lower it if grazing leaves no visible mark.").defineInRange("gamerules.forage_regrowth_attempts", 32, 0, 512);
    }
}
