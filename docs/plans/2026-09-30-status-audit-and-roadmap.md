# Untamed Wilds Plus — project status audit + roadmap (2026-09-30)

## Context

The user asked for a full state-of-the-project review: what is implemented, what isn't, what works as
intended, what doesn't, what is still to build — and a plan to tackle it, including an optimization/speed
phase. Sources: `CLAUDE.md` (1006 lines), all 38 memory notes, the code/asset tree, build logs
(`scratch/build_*.log`), the last in-game logs (`run/logs/latest.log`, 2026-07-31), and git state.
User also reported from real play: **wolves (dire_wolf type) out-breed and wipe out herds; big cats,
bears and hyenas are underpowered and die out or get killed.**

Scope decisions (user, 2026-09-30): include remaining skins, the 4 parked ecology root causes, the
mammoth animation revamp, upstream TODO cleanup, and predator balance as real phases; optimization =
server hotspots + cheap client wins (no geometry changes); **commit + push first** (authorised).

---

## Part 1 — Status snapshot

### Implemented AND verified
- Latest build `scratch/build_tier3.log` (2026-08-12): **BUILD SUCCESSFUL**, 0 compiler warnings; no source
  newer than it. Jar `build/libs/untamedwilds-1.18.2-2.5.0.jar`; version matches `mods.toml` (2.5.0).
- Content audit is clean: 43 entity types, every variant index contiguous, **no missing textures, lang
  keys, spawn-table entries or spawn-egg models**.
- Mammoth remodel is the only feature ever seen in-game (runClient 2026-07-21, which found the
  sleep/eye bugs below).

### Implemented, builds, but NEVER run in-game (runClient pending)
Everything since 2026-07-21. The last game log (07-31) predates all ecology passes.
- **Models/skins:** deer (59 boxes), dire_wolf (40), equid (31), antelope (56, 9 spp), toxodon (34),
  macrauchenia (35), glyptodont (28), ground_sloth (32), giraffid (50), tapir (new type, 7 spp),
  mammoth ear sizes, 14 reskins, 20 Tier-3 reskins, aurochs/giant_buffalo/elasmotherium realism pass.
- **Ecology:** hunt commitment + flee stamina (07-28), herbivore-massacre fix + herd flight (07-27),
  predator strength & class identities (08-01), ecosystem sim + `/gamerule untamedwildsEcosystemMode`
  (08-02), demography: starvation/condition/senescence/forage regrowth (08-10), pack burst sharing +
  `DefendHerdMateTarget` + herbivore charge ×2 (08-11).
- Specific unverified checks listed in the notes: eco-level leak after a hunting blow **and** after a
  bison charge; migrating herd arrives together; packs follow; glyptodont club toggle; `longHorns`,
  `earSize`, `flatBack`, tusk toggles; dire wolf/deer/equid poses + blink; multi-mod run.

### NOT working as intended (confirmed bugs)
| # | Bug | Evidence |
|---|---|---|
| B1 | **Giraffid skins scrambled:** okapi, sivatherium, samotherium, palaeotragus, helladotherium, bramatherium are byte-identical copies of `bison/plains.png` (128×64) on a 256×128 model. Paint scripts exist (`scripts/paint_*.js`, 2026-08-08) but the session ended before approval; nothing in memory records it. | MD5 + file sizes |
| B2 | **`HerdEntity.tick()` runs N× per herd:** 16 land species call it from every member (`EntityBison.java:136` etc.); fish guard it with a leader check (`EntityTrevally.java:54`). Herd timers run N× fast → migration cooldown ~300 ticks instead of 6000, pack losses/pressure decay so fast **packs never rout**, plus N× entity scans. | `HerdEntity.java:352` |
| B3 | **Per-frame ERROR log spam:** `EntityDataHolder.getFlags` (`:214`) logs when a flag is absent; `ModelBison.setupAnim:261-265` reads `longHorns`/`lessHair` every frame. 12,372 error lines in one session. Also `big_cat.shortSabres` (15/16 missing), `rhino.stubHorn` (7/8). | `run/logs/latest.log` |
| B4 | **Deer tail 100% buried** in `body_croup` (all 8 corners of `tail_dock`+`tail_tip` inside). | `ModelDeer` computed |
| B5 | **Wolves dominate, other predators die out** (user-observed); root causes below in Phase 4. | code analysis |
| B6 | Spawn-table `size_min/size_max` ignored (`FaunaSpawn.java:75/142`); random offset discarded (`FaunaSpawn.java:85`); `FeatureUndergroundFaunaLarge:53-65` loops up to 625 spawns at one fixed pos. | code |
| B7 | Mammoth: sleep-pose legs float, eyes don't close; leg/toe see-through spots (8 skins never re-run through `MM.bleed`); `flatBack` dome floats on cuvieronius/mastodon. | memory notes |
| B8 | Dead code: `ComplexMob.performRetaliation` (thorns only, 16 callers); `EntityBison/EntityRhino.CHARGING` never set. | code |
| B9 | `PredatorRelocateGoal.nearbyPrey` ignores eco level → a starving tiger next to un-huntable deer never relocates. | `:152-155` |

### Still to be implemented
- 6 giraffid skins (B1) + ~10 "recolour" variant skins on hold: `camel/western`, `bison/steppe`,
  `bison/long_horned`, `boar/giant_warthog`, `bear/short_faced`, `big_cat/homotherium`,
  `american_lion_male/_female`, `hyena/cave_hyena` (+ ground-sloth countershade on 2 species).
- 4 parked ecology root causes: herd eco-level cliff, unprotected calves, solitary megafauna with no
  defence, flight manufacturing stragglers.
- Mammoth animation revamp (+ ground sloth sit/sleep pose re-derivation).
- Mammoth deferred toggles (3-way tusks, JSON torso Y) — backlog only.
- Upstream gaps: 11 types with no loot table (incl. hyena), orphan PNGs, `RaidCropsGoal` "Broken",
  `GuardPositionTarget` unimplemented, `newt/crested_1 == crested_2`.
- Release: PR `ice-age-megafauna` → `1.18.2`.

### Prior plan .md files — how many are accessible
**None of the earlier plan files for this mod survive.** `~/.claude/plans/` now holds 9 files, all
for other projects (procurement, EvoSim, Medzone, Greenleaf, WC Telehealth, flower…). A listing
captured on 2026-09-07 still showed at least these mod plans, since deleted (Claude Code's plan/
transcript cleanup): `i-want-you-to-fizzy-gray.md` (**the future-species suggestion list, ~45
names**), `predators-like-dire-wolves-idempotent-sketch.md`, `the-ecosystem-part-of-lively-bird.md`,
plus possibly `my-session-crashed-where-delightful-wind.md` / `where-did-the-project-stateless-dewdrop.md`
/ `look-at-the-currently-parallel-rocket.md`. Old session transcripts are gone too (only this
session's remains). What survives is what was distilled into **CLAUDE.md + the 38 memory notes**,
which cover the ecology plans in full. The suggestion list survives only partially: 14 species
(07-30 batch) + 20 Tier-3 species (08-12, "Stage 1 / Tier 3 variant-only cheap wins") were
implemented from it; its **Tier 1/2 (new body types) contents are lost**. Phase 0 adds a guard so
this doesn't recur: copy every approved plan into the repo at `docs/plans/`.

### Future mob additions — where things stand
Roster today: 43 types, 266 species. No candidate new body types are in-repo — checked absent:
diprotodon, megalania, chalicothere, entelodont/daeodon, paraceratherium, stegodon/deinotherium/
platybelodon, castoroides, phorusrhacid, moa/elephant bird, thylacoleo, amphicyon, hyaenodon,
brontotherium, procoptodon. Phase 10 rebuilds the list and adds them.

### Repo hygiene
238 uncommitted entries (147 modified, 90 untracked incl. the whole tapir type and every new ecology
class, 1 deleted) — ~2 months of work with no backup. Untracked 733 MB `java_pid29508.hprof` (Gradle
daemon OOM; `gradle.properties` has no `org.gradle.jvmargs`) and an installer log; `.gitignore` covers
neither. Several memory notes/index lines are stale (tapir "ACTIVE", macrauchenia "not started",
mammoth ears "deferred", giraffid session unrecorded).

---

## Part 2 — Plan

Standing rules from memory apply throughout: skins one at a time with approval before writing to
`assets/`; one animal at a time; stop after 1–2 failed visual passes; load the `3d-modeling` skill for
model work; follow `reference_remodel_sop.md`; never buff predators via JSON `health`/`attack`
(eco-level inputs); tune via `ConfigGamerules`, never inside goals; runClient output redirected to a
file. Each phase ends with `./gradlew build` and a CLAUDE.md progress-log + memory update.

### Phase 0 — Safety net (first, before any code change)
1. `.gitignore`: add `*.hprof`, `*.jar.log`, `scratch/`, `bin/`, `run/`. Delete `java_pid29508.hprof`.
2. `gradle.properties`: add `org.gradle.jvmargs=-Xmx3G` and `org.gradle.daemon=false` (MDK default)
   to stop daemon OOMs.
3. Commit the 238 changes in logical chunks on `ice-age-megafauna` (models+skins per animal; tapir
   type; ecology/AI classes; config/gamerules; scripts; CLAUDE.md), then push. Ends with the required
   Co-Authored-By line.
4. Memory hygiene: fix stale index lines, add a `project_giraffid_model.md` note recording the
   2026-08-08 remodel + the 6 unpainted skins.
5. Create `docs/plans/` in the repo and save this plan there (plus every future approved plan), so
   plans are versioned with the code instead of living only in `~/.claude/plans/`. Add a feedback
   memory: "Blockbench model+skin sign-off before every next skin" (see Phase 6).

### Phase 1 — Correctness quick fixes (low risk, unblock testing)
- **B2** herd tick: guard `HerdEntity.tick()` with a `lastTickedGameTime` check (one line, covers all
  16 callers) and fix the wrong comment at `:25-28`.
- **B3** flag spam: add type-level `"flags"` defaults (`longHorns:0, lessHair:0` in `bison.json`,
  `shortSabres:0` in `big_cat.json`, `stubHorn:0` in `rhino.json`, `legLength/legThickness` default in
  `dire_wolf.json`), and make `EntityDataHolder.getFlags` warn **once per (type, flag)** via a static
  Set instead of every call.
- **B4** deer tail: re-solve `tail_dock`/`tail_tip` rotation against `body_croup` with the
  `scripts/deer_spec.py` FK/burial checker (SOP phase 7 style), not by hand.
- **B6** spawn bugs: honour `groupSize`/size_min–max in `FaunaSpawn`, apply the discarded offset, fix
  `FeatureUndergroundFaunaLarge` to use the loop position (or exit after first success).
  *Note: honouring size_max changes spawn density — do it here so Phase 2 baselines include it.*
- **B8** dead code: delete `CHARGING`/`setCharging` on bison+rhino (the charger already uses
  `getChargeAnimation()`), and make `performRetaliation` either a real hook or remove the 16 calls.

### Phase 2 — In-game verification + measurement baseline (needs the user at the GUI)
**Status: COMPLETE (marked complete by the user, 2026-10-01).** The census command (item 1) shipped in
Phase 1; the in-game checks (items 2–4) were run by the user.

1. Add a lightweight **census logger**: a `/untamedwilds census` command (+ optional every-N-minutes
   CSV to `run/logs/uw_census.csv`) counting adults/juveniles per type & species in loaded chunks,
   plus mean condition. Phases 4–5 are unmeasurable without it (acceptance = bounded oscillation).
2. `./gradlew runClient > scratch/runclient.log 2>&1`. Smoke checklist per new type: spawn egg,
   render (no pink/black), all species variants, sit/sleep poses, blink, per-species toggles
   (`longHorns`, `earSize`, `flatBack`, tusks, antler families, horn families, crest/claws, glyptodont
   club). Grep the log for ERROR/Exception.
3. Ecology checks from the notes: `AnalyzerItem` eco level identical at rest vs right after a hunting
   blow and after a bison charge (modifier leak); migrating herd arrives together; pack follows.
4. **Baseline soak**: a fresh Ecosystem-mode world, `/tick`-style long session (or `randomTickSpeed`
   untouched, AFK 2–3 in-game days), census CSV captured. This reproduces the wolf dominance with
   numbers before Phase 4 changes anything.

### Phase 3 — Optimization & speed (server + cheap client)
Ordered by impact; each change is behaviour-preserving (same decisions, fewer evaluations).
- **High**
  - B2 fix above (removes N× 33×25×33 scans, N× `pruneDefenders`, O(N²) `adultCount`).
  - `HerdFleeGoal.panic()` (`:128-192`): skip animals already fleeing (`fleeCooldown > 0`); only the
    first detector alerts the herd/alarm network → kills the N×M pathfind burst.
  - `MeleeAttackCircleHerd.tick()` (`:180-185`): re-path every 10 ticks or when the circle point moved
    > 2 blocks / navigation done.
  - `HuntMobTarget.canUse()` (`:116-157`): vanilla-style `reducedTickDelay(10)` random interval;
    cheap distance test before the diet/herd-floor filter; precompute sort keys once per candidate
    (Schwartzian) instead of recomputing `preference`/`conditionBias` per comparison.
  - `ProtectChildrenTarget.canUse()` (`:65-102`) and `FollowParentGoal.canUse()`: throttle to every 10
    ticks.
  - `LookThroughSpyglassEvent` (`:36-66`): clamp `level.clip` range to server view distance and use
    `hasChunkAt` checks — currently a 5000-block raycast can **generate chunks** on the main thread.
- **Medium**
  - `ForageRegrowthHandler` (`:46-113`): `hasChunkAt` on neighbour sweeps; spread the per-player 32
    attempts across ticks instead of one burst.
  - `HerdEntity:417-418` follower re-path only when navigation done or leader moved > 8 blocks.
  - Cache `isLocallyOvercrowded` per herd for ≥ 100 ticks (`ComplexMob:802-818`).
  - `SmartAvoidGoal`/`HerdFleeGoal` predator scan: leader scans, followers reuse (or 20-tick interval);
    avoid `List.of` allocation in `EcologyTags.isPredator` (`:78-93`).
  - Stagger the 19 `getGameTime() % 1000` hunger/heal blocks by `tickCount` (packet bursts).
  - `findCarcassThief` duplicate 12-radius scan inside its loop (`ComplexMob:1410/1418`).
  - Add a `predator_odds_ratio`-independent kill switch for `countEngagedDefenders`.
- **Client (cheap)**
  - Cache `getAllParts()` `ImmutableList` in a final field in every `Model*.java` (currently rebuilt per
    entity per frame).
  - B3 log spam removal (string concat + file I/O per frame).
  - Move `Animation` static-field creation out of entity constructors (`EntityDireWolf:40-42`,
    `EntityAntelope:55-56`).
- **Measure**: before/after with Forge's `/forge tps` and a Spark profile (spark mod in `run/mods`)
  on the Phase-2 soak world with ~200 animals loaded.

### Phase 4 — Predator balance (user-reported: wolves dominate, others die out)
Ranked root causes and fixes (none touch JSON health/attack):
1. **Wolf packs grow without bound and get stronger as they grow.** One carcass pays +120/+90 to every
   member regardless of pack size (`ComplexMob:298-321`); pups add +1 eco each (`:1317`) but don't
   count toward crowding (`:810`); predators never disperse; wolves have the biggest litters (mean 4.5).
   Fixes: carcass = fixed food budget ∝ prey max HP **split among eaters**; carnivore herd eco term =
   `min(adults, maxSize)`, juveniles excluded (re-run the food-web check CLAUDE.md requires);
   juveniles count in `isLocallyOvercrowded`; call `detachDisperser` when a predator herd exceeds
   maxSize; dire_wolf `offspring` 5 → 3.
2. **Solitary cats/bears almost never breed.** Worldgen spawns one animal (B6), mates must be within 24
   blocks, and cats/bears carry extra breed gates (season, not sleeping, ≥60% HP) wolves don't. Fixes:
   solitary species spawn as an opposite-sex pair; long-range mate-seek (96–128 blocks every 200 ticks)
   for groupCount 1 / no-herd animals; unify breed gates across predator classes.
3. **Lone predators have almost nothing to hunt** (no herd eco term + strict `eco < own`; e.g. lone
   jaguar can take no adult mod grazer). Fixes: in `HuntMobTarget`, a herd member strayed beyond
   `herd.getRadius()` from its leader is scored without its herd term (ambush-on-straggler); fix B9.
4. **Wolves hunt bears and win every brawl.** Bears are tagged `megafauna` and dire wolves eat
   megafauna; `pack_hunter_damage_reduction` ×0.7 applies to *all* damage incl. brawls and starvation.
   Fixes: reject carnivore-tagged targets unless the diet lists `carnivore`; restrict pack reduction to
   food hunts vs non-carnivores and exclude starvation; give lone hunters facing defenders their own
   reduction; stop wolf `ProtectChildrenTarget`/`AngrySleeperTarget` attacking predators that aren't
   threatening the pups; clear `AngrySleeperTarget`'s stale cached target.
5. **Packs never rout** — fixed by B2; re-verify rout thresholds with the census soak.
6. **Hyenas:** absent from 6/10 biome categories; 3 of 6 species can hunt no adult prey. Make
   kleptoparasitism the real food source (`kleptoparasitism_clan_size` 4 → 3, share across clan) and
   widen spawn biomes where historically plausible.
All new numbers as `ConfigGamerules` entries, Zoo-mode behaviour unchanged via `EcologyMode`.
Acceptance: census soak shows each predator type persisting and prey oscillating, none → 0.

### Phase 5 — Prey-side ecology root causes (parked from 2026-08-11)
Done right after Phase 4 and tested in the same soak, since they interact.
1. **Herd eco cliff**: herd term = `max(living, maxSize × memory)` (reuse `isHerdTooSmallToHunt`'s idea).
2. **Unprotected calves**: raise `BABY_ECO_FACTOR` and/or drop the baby exemption from
   `isHerdTooSmallToHunt` so recruitment survives.
3. **Solitary megafauna** (rhino, ground sloth, moose; `herd == null`): a solitary-defence bonus
   through damage reduction / charge, not health.
4. **Flight manufactures stragglers**: `panic()` flees as a group vector away from the threat
   (herd centroid) instead of per-animal random positions (also cheaper — pairs with Phase 3).

### Phase 6 — Remaining skins (one at a time, Blockbench sign-off gated)
**Skin workflow (user rule, 2026-09-30) — applies to every skin in this and later phases:**
1. Load the species' real model into Blockbench as a rig (`scripts/gen_bb_rig.py` / `java2bb.py`,
   constructor-only parse, verified by re-export diff), with any per-species `showModel`/scale
   toggles baked in so it matches what the player sees.
2. Paint the skin natively on that rig in Blockbench (paintlib + `MM.bleed`, eye rects protected).
3. **Stop and hand over**: the user inspects model + skin in Blockbench (orbit, zoom) and either signs
   off or requests changes. No PNG is written to `assets/` and no next skin starts until sign-off.
4. After sign-off: write the PNG, run `skin_census.py` vs all siblings, then move on.
Blockbench must be running with the MCP plugin on port 3000 (it failed to connect this session);
fall back to the `scripts/bb.py` HTTP bridge. Run `read_back.py` before any rig rebuild in case the
user hand-edited in Blockbench.

1. Giraffid ×6 using the existing `scripts/paint_{okapi,samotherium,bramatherium,helladotherium,
   palaeotragus,sivatherium}.js` + `paint_giraffid_common.js` on the 50-box giraffid rig.
2. Recolour variants: western camel (user wants redone), steppe/long_horned bison, giant_warthog,
   short_faced bear, homotherium, american lion M/F, cave hyena; ground-sloth countershade ×2.
3. Mammoth: re-run all 8 skins through `MM.bleed` with eye rects protected (fixes leg/toe holes);
   hide the `flatBack` dome (`showModel=false`) on cuvieronius/mastodon.
Every skin: `MM.bleed`, `skin_census.py` distance vs ALL siblings, approval before `assets/`.

### Phase 7 — Mammoth animation revamp (+ sloth poses)
Per `project_mammoth_animation_revamp.md`: numerically solve sleep/sit poses so legs contact the ground
(SOP phase 7 grid search), real eyelid close instead of the reposition trick, then trunk/walk/idle/
graze/attack revamp. Then re-derive the ground sloth's simplified sit/sleep folds the same way.
Each solved pose is shown on the Blockbench rig for user sign-off first (static poses only —
Blockbench can't run `setupAnim`), then checked in motion with runClient.

### Phase 8 — Upstream TODO cleanup
Loot tables for hyena + 10 others (football_fish, giant_salamander, king_crab, large_snake, newt,
snake, spadefish, spitter, triggerfish, whale_shark — copy analogous tables); decide on orphan PNGs
(wire or delete); fix `newt/crested_2`; `RaidCropsGoal` "Broken"; remove `NestReptileBlock:118` debug
log; triage the remaining TODOs (list in audit) as fix / leave / delete.

### Phase 9 — Release
Bump version in **both** `gradle.properties` and `mods.toml`; build jar; multi-mod runClient (JEI,
Citadel, Patchouli); final CLAUDE.md "Start here" rewrite (it still says 2026-07-20); open PR
`ice-age-megafauna` → `1.18.2`.

### Phase 10 — Future mob additions (after the ecosystem is stable)
Adding animals before Phases 4–5 would add more food-web entries to a balance that doesn't hold yet,
so this comes last (or in parallel with Phase 9 as data-only work).
1. **Rebuild the lost suggestion list** as `docs/plans/future_species.md`: the current 266-species
   roster, then candidates in three tiers:
   - *Tier 3 (variant-only, Mechanism A)* on existing rigs — e.g. more deer/antelope/bison/big_cat/
     canid species. Each one = JSON object + skin + 2 lang keys.
   - *Tier 2 (existing type + model flag)* — e.g. an entelodont/daeodon on the boar rig, stegodon/
     platybelodon/deinotherium on the mammoth rig (tusk/jaw flags), megalania as a monitor species.
   - *Tier 1 (new body type, Mechanism B)* — e.g. diprotodon, chalicothere, castoroides, thylacoleo,
     phorusrhacid/moa (would need the mod's first bird rig), paraceratherium.
   The user picks what goes in; each pick also gets ecology tags + diet placed so it slots into the
   food web (re-run the eco-level check from CLAUDE.md).
2. Build each pick with the full remodel SOP (spec + FK → Blockbench massing → z-fight → UV → verbatim
   port → solved poses) and the Phase 6 Blockbench sign-off loop for its model and every skin.
3. Census soak after each new predator or large herbivore type, to confirm it doesn't break Phases 4–5.

---

## Verification (per phase)
- Every phase: `./gradlew build` → BUILD SUCCESSFUL, log saved in `scratch/`.
- Phase 1/3: `grep -c "Couldn't find" run/logs/latest.log` = 0; no new ERROR lines; Spark profile
  shows the listed goals dropped in the server-tick breakdown; `/forge tps` ≥ 19.5 with ~200 animals.
- Phase 2: checklist ticked per type; eco-level leak check passes.
- Phases 4–5: census CSV over ≥ 3 in-game days — every predator type > 0, wolf count bounded, herds
  oscillate rather than hit 0; Zoo mode unchanged (spot check).
- Phases 6–7, 10: user sign-off per model/skin/pose in Blockbench before the next one starts, then an
  in-game look.
- Phase 9: jar loads in a multi-mod instance with no errors.
