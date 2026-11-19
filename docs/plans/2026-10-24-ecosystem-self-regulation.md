# Untamed Wilds Plus: a self-regulating ecosystem (predator numbers follow prey numbers)

## Context

In a 2-hour-20-minute playtest, the populations in loaded chunks went far out of balance:

| | Adults / juveniles |
|---|---|
| Short-faced bears | 268 / 334 |
| Brown bears | 89 / 86 |
| Big cats | 122 / 46 |
| All herbivores combined | about 330 |

Large herbivores crashed: bison, caprines, glyptodonts and tetrameryx. Bears were well fed (condition 0.77) despite the shortage of prey.

**The goal.** A wild ecosystem that behaves naturally:
- predators settle at about **1 per 30–50 prey** by head count (user choice);
- predator and prey numbers cycle;
- predators move toward prey-rich ground instead of eating an area out;
- nothing goes permanently extinct.

**User decisions (2026-10-24):**
- Predators breed **realistically**, but this must **not** cause the predator crash that the old fast-breeding multipliers were added to prevent.
- **Slow immigration** brings back species that are locally absent.
- The **9 hunters that never get hungry are deferred** to a later pass.

### What three code audits found

**Predators out-breed their prey several times over, and nothing ties a litter to how much prey exists.**
- `predator_gestation_multiplier` and `predator_maturity_multiplier` are both 0.33, and `predator_litter_bonus` is 2 (`config/ConfigGamerules.java:229-234`). A bear is pregnant for 0.92 days and an adult at 2.6 days, against 3.5 and 10 days for red deer.
- A mother can conceive again the moment she gives birth (`entity/ComplexMob.java:1405-1414`). That works out to about 35 cubs per short-faced bear female.
- Breeding needs only hunger ≥ 80 and condition ≥ 0.6 (`ComplexMob.wantsToBreedAsPredator`, `:1109-1122`). One kill every 3–4 days keeps a bear permanently able to breed.

**Cubs cost nothing.**
- Babies are exempt from starvation (`tickStarvation`, `ComplexMob.java:1022-1037`).
- The mother pays no feeding cost.
- Nothing hunts cubs, because a predator with a declared diet won't hunt anything tagged `carnivore` (`EcologyTags:127-129`).

**Carcass food is unlimited.**
- Every predator kill gives +60 to every bear or hyena within 16 blocks, with no cap on how many (`shareCarcassWithScavengers`, `:1507-1510`).
- Every non-baby death gives +40 carrion to all of them (`die()`, `:1177-1183`). The killer collects that carrion on its own kill too, because `die()` runs before `satiateFromKill`, so its kill is worth +160.
- Bears steal kills from cats, wolves and hyenas (`:1565-1619`).
- Bears eat any dropped edible item (`FindItemsGoal` in `EntityBear:68`).

**Bears and solitary cats have no density limit.** `isLocallyOvercrowded()` returns false whenever `herd == null` (`:859-861`). Bears never have a herd. Solitary cats lose theirs after a reload or at birth.

**Predators only move when prey is completely gone.** `PredatorRelocateGoal` waits for zero huntable prey within 32 blocks for 2400 ticks (`entity/ai/PredatorRelocateGoal.java:77-84`). It also ignores the herd floor, so a predator standing next to prey it isn't allowed to hunt never moves.

**Prey-side bugs:**
- **1-HP antelope:** tetrameryx, blackbuck, giant_eland, gemsbok, sable and bluebuck have no health or attack values in `antelope.json`. The codec defaults them to -1, `EntityAntelope.updateAttributes` (`:215-217`) applies that, and they spawn with **1 HP**. Each kill is then worth only 8 food, so predators stay hungry and hunt at the cooldown limit.
- **Bison:** `EntityBison` lacks `INeedsPostUpdate`, so every variant has 35 HP instead of the 60–85 in its data.
- **Males:** both mates are locked out for the whole gestation (`SmartMateGoal.java:102-103`), so births are capped at the number of males.
- **Calves:** they get hungry but can't graze (`GrazeGoal.java:67`), and their starvation clock runs while they're young. Every calf reaches adulthood starving.
- **Glyptodonts:** the herd floor never protects them, because species with groupCount ≤ 2 are exempt (`HuntMobTarget.java:92-121`).
- **Flight stamina:** one panic empties it, and a full refill takes 400 ticks.

**Repopulation:** spawning happens only at world generation (`world/UntamedWildsGenerator.java`, `FaunaSpawn`).

## Approach

The central change gives predators a **numerical response**: a predator may breed only while local prey per predator is above the target ratio. Predators then increase until they reach about 30:1 and stop there. The delay from gestation and maturity is what creates the predator–prey cycles. Realistic life cycles, food that runs out, and prey-seeking movement keep the system from overshooting. The safety measures in Phase D keep predators from dying out.

Every new mechanic:
- is gated by `EcologyMode` (Zoo mode keeps consequences off, as now);
- has a `ConfigGamerules` option;
- runs its entity scans no more than once per animal per 600 ticks, or once per herd leader.

### Phase A: prey bugs (data and small code fixes)

1. **Antelope health and attack.**
   - Add health and attack to the six species in `antelope.json`: tetrameryx 16/3, blackbuck 16/3, giant_eland 60/7, gemsbok 40/7, sable 42/7, bluebuck 30/5.
   - Guard every `updateAttributes` so a species value below 1 falls back to the type default instead of being applied. Put the helper in `ComplexMob` and reuse it in the entities that hand-copy the pattern.
2. **Bison** implement `INeedsPostUpdate` and add `updateAttributes()`, using the `EntityAntelope` pattern, so the data's 60–85 HP applies.
3. **Male lockout** (`SmartMateGoal`): only the female gets the pregnancy timer. The male gets a short refractory period (new `male_refractory_ticks`, default 1200).
4. **Weaning, for every type.** The first 40% of babyhood is **nursing**: no hunger drain and no starvation. After that the young are **weaned**:
   - Herbivore calves may use `GrazeGoal`; remove the blanket `isBaby()` exclusion.
   - Hunger drains normally.
   - Starvation applies to predator cubs only. Herbivore calves stay exempt but can now eat.
   - Reset `starveTicks` at weaning and again at maturity.
   - Hook: `ComplexMob` aiStep / `tickStarvation`, with a new `nursing_fraction` option (default 0.4).
5. **Glyptodonts.** Change the herd-floor exemption from groupCount ≤ 2 to ≤ 1. Add a carapace damage reduction of 0.35 against carnivores for the glyptodont type (new `carapace_damage_reduction`), next to the solitary-megafauna reduction (`ComplexMob.java:492-496`).
6. **Flee stamina** refills at 1 per tick instead of 1 per 2 (`ComplexMob.java:658-670`).

### Phase B: realistic predator life cycle (no crash)

1. **Breeding speed and litters.**
   - Set the defaults for `predator_gestation_multiplier` and `predator_maturity_multiplier` to **1.0** and `predator_litter_bonus` to **0**. `growing_time` already encodes body size: bears 8 against bison 10, lions 6 against wildebeest 7.
   - Litter size follows condition: 1 cub below condition 0.7; at 0.7 and above, `1 + nextInt(offspring + 1)`.
2. **Cub-rearing interval.**
   - At birth a predator mother gets an age timer equal to 50% of her species' maturity time. While it runs she can't conceive. This uses the existing `age != 0` gate, set in `breed()` / `ComplexMob:1405-1414`.
   - While she is rearing, her hunger drain is ×1.5 (lactation; new `lactation_drain_factor`).
3. **Cubs eat at kills.** `satiateFromKill` also feeds same-type young within 16 blocks and counts them as eaters. Solitary bears' cubs have no herd, and this is how they get fed. Weaned cubs that don't eat starve, which is the realistic way predator numbers get trimmed.
4. **Checked to grow at abundant prey.** Brown bear: first litter at about 10.8 days, then a litter about every 6.8 days, with about 2 cubs per litter at good condition. That gives a lifetime output of about 5.5 daughters with no deaths, so predators grow quickly whenever food is plentiful. Write out the derivation in the plan log.

### Phase C: carcasses become a finite resource

1. **Kill pool (rewrite of `satiateFromKill` + `shareCarcassWithScavengers`).**
   - Food pool = prey max HP × `carcass_food_per_health` (8).
   - The killer and its pack and cubs take their shares first. The existing caps stay: 120 for the killer, 90 for each other eater.
   - Whatever is left goes to at most 3 scavengers (new `max_scavengers`), up to 60 each.
   - When the pool is empty, nobody else eats.
2. **Carrion (`die()`).**
   - Pool = HP × 4, shared by at most 3 scavengers.
   - Not paid out for deaths of the scavenger's own type, or for babies.
   - Not paid out at all when the death was a predator kill. The death is flagged so the killer can't double-dip.
3. **Kill stealing** keeps its behaviour, but the thief draws from the same finite pool.
4. **Bears' item eating (`FindItemsGoal`).**
   - Only when hunger ≤ 60.
   - Nutrition ×5 instead of ×10.
   - Bears may no longer eat meat that dropped from a kill within the last 600 ticks; that food is already counted in the pool.

### Phase D: numerical response, movement, and the no-extinction safety measures

1. **Local prey-per-predator ratio, `ComplexMob.preyPerPredator()`.**
   - Recomputed at most every 600 ticks for each predator, within 64 blocks (`ratio_radius`).
   - Prey = non-tame `ComplexMob` adults the predator can eat (diet tags, reusing the `PredatorRelocateGoal.nearbyPrey` diet and eco-level test), plus 0.5 per juvenile.
   - Predators = adult `EcologyTags.isPredator` mobs whose diet overlaps.
   - Vanilla animals still feed predators, but they don't count toward the ratio, so a respawning subsidy can't license breeding.
2. **Breeding gate (`wantsToBreedAsPredator`).** Also requires ratio ≥ `predator_breed_ratio` (default **30**). This is what replaces the missing density cap for bears and solitary cats.
3. **Safety measures against predator crashes (the user's condition).**
   - **Rare predator:** if no other adult of the same type is within 96 blocks apart from a mate, the condition threshold drops to 0.45 and the ratio requirement to 15.
   - **Wide mate search for solitary predators** stays at 96 blocks.
   - **Predators can immigrate** (Phase E).
   - **Starvation is slower** for adult predators: grace period 4800 ticks for carnivores (`predator_starvation_grace`). Lean periods then cost cubs before they cost adults.
4. **Moving toward prey (`PredatorRelocateGoal`).**
   - Fires when ratio < `predator_relocate_ratio` (default **15**) for 2400 ticks, not only when prey is zero.
   - Scores candidate spots by prey ÷ (predators + 1).
   - Uses the hunting filters: diet, eco level, and the herd floor.
   - Moves the home of the pack or the solitary predator.
   - Also registered for all land predators that have the goal's preconditions.
5. **Dispersal at maturity.** A predator that has just become an adult runs the same search once (64–128 blocks) and settles at the best ratio it finds. This spreads predators toward prey instead of piling them up where they were born.
6. **Wildebeest trek follow-up.** `EntityWildebeest.drawPredators` uses the real hunt threshold instead of `< 150`, and sets the follower's home so the nightly return to its sleeping spot doesn't pull it back.

### Phase E: slow immigration (new `world/ImmigrationHandler.java`)

- **Schedule:** every `immigration_interval` ticks (default 12000) per player, ecosystem mode only.
- **Where:** a random surface point 64–128 blocks from the player in a loaded chunk.
- **Candidates:** species whose `spawnBiomes` match the biome and which have no adults of that species within 128 blocks.
- **Weighting:** herbivores 85%. Predators 15%, and only where the prey-per-predator ratio there is at least 30 (computed as in Phase D).
- **Group size:** herbivores 2–4 (respecting groupCount), predators 1–2 (a male and a female).
- **Spawning:** reuses the existing `FaunaSpawn` group-spawn and `finalizeSpawn` path, so size, gender and age are set correctly.
- **Cap:** nothing spawns if more than `immigration_local_cap` (default 150) mod animals are already within 128 blocks.
- **Config:** `immigration_enabled`, `immigration_interval`, `immigration_predator_share`, `immigration_local_cap`.

### Phase F: measurement

1. **Census additions (`util/CensusCommand.java`).**
   - Per-species counters for births, predation deaths, starvation, old age and other deaths. Increment them in `breed()`, `die()` (cause from the damage source and the predation flag) and `tickOldAge`.
   - Write them as new CSV columns.
   - Add a summary row with the overall prey-per-predator ratio.
2. **Offline estimator (`scripts/eco_equilibrium.py`).** Plugs the real formulas and configs into the numbers for each species: growth rate r from gestation, maturity and lifespan, and predator kills per day from hunger drain and carcass food. From those it predicts the equilibrium ratio and cycle period, so defaults can be tuned before the soak test.

## Files to modify

- **Core:** `entity/ComplexMob.java`
  - breeding gates
  - nursing and weaning
  - starvation grace
  - `satiateFromKill` / scavengers / carrion pool
  - `preyPerPredator`
  - litter size
  - rearing interval
  - attribute fallback helper
- **AI:**
  - `entity/ai/SmartMateGoal.java`: male refractory period
  - `entity/ai/GrazeGoal.java`: weaned calves graze
  - `entity/ai/PredatorRelocateGoal.java`: ratio trigger, filters, maturity dispersal
  - `entity/ai/FindItemsGoal.java`: bear limits
  - `entity/ai/target/HuntMobTarget.java`: herd-floor exemption
- **Entities:**
  - `entity/mammal/EntityBison.java`: `INeedsPostUpdate`
  - `entity/mammal/EntityAntelope.java` and peers: attribute fallback
  - `entity/mammal/EntityGlyptodont.java` or `ComplexMob`'s damage hook: carapace
  - `entity/mammal/EntityWildebeest.java`: `drawPredators`
- **Config:** `config/ConfigGamerules.java` for new options and defaults; `config/EcologyMode.java` for Zoo gates.
- **New:** `world/ImmigrationHandler.java`, registered on the server tick event alongside the existing world handlers.
- **Data:** `data/untamedwilds/entities/antelope.json`.
- **Tools:** `util/CensusCommand.java`; new `scripts/eco_equilibrium.py`.

Reuse rather than duplicate:
- `PredatorRelocateGoal.nearbyPrey` / prey scoring
- `ComplexMob.censusOf` / `countOvercrowded` scan helpers
- `HerdEntity.dispersePredatorSurplus`
- `FaunaSpawn` group spawning
- `EcologyTags.isPredator` / diet tags
- `EcologyMode` gates

## Verification

1. `./gradlew build` passes after each phase; install the jar in Test.
2. **Offline:** `python scripts/eco_equilibrium.py` predicts a prey-per-predator ratio of about 25–50 and a positive predator growth rate when prey is abundant, for bears, big cats, dire wolves and hyenas.
3. **Unit checks in a fresh world:**
   - `/summon` tetrameryx, giant eland and bison and check their health with `/data get entity`.
   - Watch a pregnant female while the male mates again.
   - A weaned calf grazes.
   - A bear kill feeds at most 3 scavengers.
4. **Soak test (`runClient`).**
   - Setup: ecosystem-mode world, `/untamedwilds census auto 5`, at least 10 in-game days (about 3.5 hours) in the same 5k map area.
   - **Pass criteria:**
     - the overall prey-per-predator ratio settles in the 20–60 range;
     - no predator type goes to zero for long, and immigration logs show recolonisation;
     - herbivore counts oscillate rather than trend down;
     - the CSV shows predation deaths ≤ births for every herbivore at equilibrium;
     - starvation hits predator cubs before adults.
5. **Zoo mode:** with `/gamerule untamedwildsEcosystemMode false`, none of the new consequences happen: no ratio gate, no immigration, no cub starvation.

## Out of scope (later, per the user)

- The 9 hunters that never get hungry (shark, monitor, snakes, anaconda, softshell turtle, tarantula, opossum, football fish, giant salamander).
- Persistent carcass entities.
- Water and thirst.
- Disease.
- Crocodilians, which remain the next new-model phase.

## Implementation log

- 2026-10-24: Phases A-F implemented; build passes; jar in Test.
  - The offline estimator (`scripts/eco_equilibrium.py`) gives predator growth rates of 0.06-0.15 a day (4.5-7.5 daughters per female) against prey rates of 0.05-0.14. At the 30:1 set point, predation takes at most about 3% of the prey a day, so every pairing is sustainable.
  - The dev config `run/config/untamedwilds-common.toml` still held the old 0.33 / 0.33 / +2. Forge keeps existing values, so I set it to 1.0 / 1.0 / 0 (backup in `scratch/`). The Test instance's config predates these options and picks up the new defaults.
  - Pending: the soak test (Verification step 4) and the unit checks (step 3).
- 2026-10-24: first soak on the overpopulated 5k map (about 2 hours).
  - **Lag:** heavy from 17:10 to 17:50 as hundreds of bears went hungry at once, then gone after 18:10.
  - **Immigration:** fired once at 18:15 (Maokopia x2).
  - **Census:** `scripts/world_census.py`, a new offline reader for saved worlds, found:
    - near the player, 77 predators (68 short-faced bears), no predator young, 10 herbivores;
    - across the whole save, 796 predators against 1395 herbivores. Predator young fell from 420 to 55, and dead-bear hides and meat show the die-off is under way.
  - **Bug found:** lifespan is derived from maturity, so maturity 1.0 had tripled predator lifespans (bear 21 to 64 days). Added `predator_lifespan_multiplier` = 5, matching real carnivores at 3-5x maturity. The estimator still gives a lifetime output of 2.2-3.8 daughters and every pairing sustainable.

