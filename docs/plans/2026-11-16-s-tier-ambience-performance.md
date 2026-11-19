# Untamed Wilds Plus 2.8.0: voices, eat/drink/alert animations, waterholes, carcasses, mob performance

## Context

The UX review ranked four "S tier" gaps. You asked for those first, with new species parked.

1. **Voices.** 25 types borrow vanilla sounds and 11 aquatic types are silent. Three sound ids are broken: `entity.big_cat.ambient_meow`, `entity.big_cat.warning`, and the manatee's `untamedwilds:entity.cow.*`.
2. **Eat, drink and alert animations.** Only mammoth, wildebeest and hippo have a real EAT animation. Nothing has drink or alert.
3. **Drinking.** Animals never visit water.
4. **Carcasses.** Kills vanish. Scavengers are paid food instantly within 16 blocks.

You also asked for:
- a plan you don't need to play to verify;
- performance fixes, because these features add animations, packets and AI goals;
- everything to stay modpack-compatible.

**Your decisions:**
- **Sounds:** real recordings, CC0 or public domain only, licences verified, converted to `.ogg`.
- **Thirst:** cosmetic visits only. There is no thirst stat and no consequence.
- **Carcasses:** the carcass holds the loot.
  - A predator kill or natural death leaves a carcass that fades to bones.
  - Scavengers feed from it over time.
  - A player can butcher it once with an axe, sword or spear for the normal drops.
  - Player kills stay exactly as now.

**Defaults I chose for the open questions.** Override any of them when you approve.

| Question | Default |
|---|---|
| A sound has no CC0 or public-domain recording | It keeps its current vanilla sound and goes on a "gaps" list. No CC-BY. |
| Butchered meat after scavengers have fed | Shrinks in proportion to the food left (meat only). |
| Shared herd threat scan | Default ON only if the equivalence GameTest and the census soak both pass. Otherwise it ships OFF. This meets your 2026-09-30 condition: "only if a profile says it matters". |
| Fish sounds | Keep vanilla fish sounds. |
| Drinking visits | Herbivores only. Predators drinking can come later. |
| Fly particles over carcasses | Vanilla particles. A custom sprite would need your approval. |

**Two facts verified this session that shape the design:**
- **Citadel lets us take over its animation packets.** Its `AnimationEvent.Start` is `@Cancelable` (checked in the cached 3681528 jar). We can cancel it for our own mobs and send the packet only to players who can see the animal.
- **Our mobs are tracked too far away.** `ComplexMob.removeWhenFarAway()` already returns false, so wild animals never despawn. Separately, every UW mob type registers `clientTrackingRange(64)` chunks (`init/ModEntity.java:158`), so it is tracked out to the server's full view distance. Vanilla animals use 10 chunks.

## Modpack-compatibility rules (apply to every phase)

- **No new dependencies or engine patches.**
  - No mixins, no new access transformers, no new hard dependencies.
  - Citadel stays the only required mod. Serene Seasons and Patchouli stay optional.
- **Every behaviour change has a config toggle.**
  - Performance changes must give identical results, proven by equivalence GameTests.
  - Vanilla mob-cap counting is unchanged by default: `count_toward_vanilla_mob_cap = true`.
- **Citadel interop:** the animation-event cancel only touches our own mobs, runs at LOWEST priority, and is guarded by `isCancelable()`.
- **Network:** protocol "1" → "2", so a client and server on mismatched versions refuse to connect cleanly.
- **Death and loot events:**
  - `LivingDeathEvent` and `LivingDropsEvent` still fire.
  - Carcasses only appear when the death wasn't cancelled.
  - Butcher loot rolls through `LootTable.getRandomItems(LootContext)`, so global loot modifiers and datapacks still apply.
  - `doMobLoot` is respected.
- **Dedicated servers:** the client crash on dedicated servers gets fixed in P1. This is the biggest modpack bug found.

## Phase order

```
P0 harness + baseline ─┬─> P1 crash fixes + safe perf + tracking-only animation packets ─> P2 parity / shared scan / panic spread ─┐
                       ├─> P3a animation plumbing ──────────────────────────────────────────────────────────────────────────────┤─> P4 alert ─┐
                       ├─> P3b offline pose solving + render sheets (YOUR APPROVAL, long lead) ─> P3c keyframes ─> P5 drink ─────┘            ├─> P7 carcasses ─> P8 client LOD (optional) ─> P9 release check
                       └─> P6a sound sourcing + audio review page (YOUR APPROVAL, long lead) ─> P6b sound wiring ──────────────────────────────┘
```

- P3b and P6a start on day one, because they wait on your approval.
- P1 lands before any new animation ships, so new animation packets only reach players who can see the animal.

---

## P0: Test harness and baseline (no behaviour change)

**Setup:**
- Save this plan to `docs/plans/2026-11-16-s-tier-ambience-performance.md`, because `~/.claude/plans` gets auto-cleaned.
- Copy `build/libs/untamedwilds-1.18.2-2.7.0.jar` to `scratch/baseline/untamedwilds-2.7.0-A.jar` before any code changes. It is the "A" side of every comparison.
- Recommend committing 2.7.0 first. I'll only do that if you say so.

**`build.gradle`:**
- a `gameTestServer` run (`forge.enabledGameTestNamespaces=untamedwilds`, working directory `run-gametest`);
- the server run with `--nogui` in `run-server`;
- a `clientSmoke` run (`--server localhost --port 25565 --username SmokeBot`, `-Duntamedwilds.smoke=true`);
- JUnit 5 for pure-logic classes;
- `jar { exclude 'untamedwilds/gametest/**' }`.

**Test code and structures:**
- New package `untamedwilds/gametest/`: `UWGameTests` (`@GameTestHolder`), `CarcassTests`, `DrinkTests`, `AlertTests`, `PerfTests`, `DataTests`, `GTUtil`.
  - `GTUtil` covers spawn at a fixed spot, force noon, set the gamerule, count sound events, and a FakePlayer.
- `scripts/gen_gametest_structures.py` writes `platform.nbt` (32×8×32 grass) and `waterhole.nbt` (the same with a 6×6 pond and banks) as gzip NBT, with no extra Python libraries.

**Measurement tools:**
- `util/PerfCounters` keeps per-feature counters on the server thread. It is gated by `debug_perf_counters`, so when off it costs one boolean check.
- `CensusCommand` gains `/untamedwilds perf [reset|dump|auto <s>]` (writes `logs/uw_perf.csv`) and op-only `debug anim|carcass|shot` for the harnesses.
- `network/DebugScreenshotPacket` is acted on only when the client has `-Duntamedwilds.smoke=true`. It freezes a mob at a given animation and tick, then grabs a screenshot.

**Scripts:**
- `scripts/validate/`: `validate_sounds.py`, `validate_static.py`, `validate_keyframes.py`.
- `scripts/soak/`: `rcon.py`, `soak.py`, `client_smoke.py`, `ab_report.py`, `scenarios/`.
- `scripts/verify_all.py` runs validators → `./gradlew build` (with JUnit) → `runGameTestServer`, plus optional `--soak` and `--smoke`.

**Pass:**
- a trivial GameTest passes headless;
- the soak against jar A writes an MSPT CSV;
- the client smoke against a dedicated server running jar A **reproduces** the dedicated-server crash, which proves the harness catches it.

## P1: Crash fixes, low-risk performance, tracking-only animation packets

**1. Dedicated-server crash.**
- *Cause:* species data is null on remote clients.
  - `EntityDataListenerEvent` registers only 32 of 51 types eagerly.
  - Skins and names are synced once at login.
  - Models and `SaddleLayer` call the server-only `ComplexMob.getEntityData`.
- *Fix:*
  - Register every type from the JSON loader's map, and sync on `OnDatapackSyncEvent` (fires at login and on `/reload`).
  - `SyncTextureData` also carries per-species flags.
  - New `ComplexMob.speciesFlag(entity, name)` reads the server holder, then the client map, then 0 (warning logged once). It replaces every `getEntityData(` under `client/`: ModelBison, ModelDeer, ModelDireWolf, ModelGlyptodont, SaddleLayer, plus anything grep finds.
  - `EntityUtils.getSkinFromEntity` falls back to the missing texture instead of throwing.

**2. Despawn and home check (`ComplexMob.checkDespawn:1969`).**
- *Today:* any animal with a home and no nearby player calls `getBlockEntity(home)` every tick. For a migrated herd that can load chunks synchronously.
- *Fix:* the check only runs for burrow users, every 20 ticks, staggered by entity id, and only `if level.isLoaded(home)`.
- Add a `requiresCustomPersistence()` override behind `count_toward_vanilla_mob_cap` (default true = unchanged).

**3. AI caches and throttles.** All behaviour-preserving.

| Change | What it does |
|---|---|
| Eco-level cache | `getEcoLevel` is cached per entity per tick. It is invalidated by `applyAttackBonus`, `clearAttackBonus` and `hurt`, and never cached while the attack-bonus UUID is present. |
| `HerdEntity` counts | `adultCount()` is cached per tick. The combine/rejoin scan runs every 40 ticks, staggered by leader id. |
| Breeding | `setInLove` only fires if the animal isn't already in love, which ends a hearts packet every 600 ticks. `ratioCheckedAt` starts staggered by id. |
| `AngrySleeperTarget` (currently O(N³)), `DefendHerdMateTarget`, `DontThreadOnMeTarget` | `reducedTickDelay` throttles. The owner's eco level is computed once per check. |
| `HuntMobTarget` | Exponential backoff after an empty scan: 10 → 200 ticks (`hunt_scan_backoff_max_ticks`). It resets on a kill or on starving. |
| `SmartMateGoal` (solitary search) | Searches only the animal's own class instead of a 193-block cube of everything, and backs off 240 → 2400 ticks after a failure. |
| `PredatorRelocateGoal` | At most 4 candidate scans, reusing the cached ratio. |
| Crocodile basking | The disturbance scan runs every 10 ticks instead of every tick. |
| Whale, whale-shark and anaconda multipart collision | Every 2 ticks on the server. On the client, only within 8 blocks of the player. |
| Beaver alarm | Uses the new `util/WaterFinder` sampler with a cache. |
| `GrazeGoal` | Drops the unhandled entity-event-10 broadcast. Adds `isLoaded` guards in `locateGrazeables` and `sampleForageDensity`. |

**4. Client allocations.** The 8 models that build a new part list every frame cache `getAllParts`, the same way ModelBison does: Caprine, Crocodilian, Diprotodont, GiantMonitor, Kangaroo, LargeRodent, Pinniped, Wildebeest.

**5. Tracking ranges** (`ModEntity.createEntity`), set by size class instead of 64 chunks, times `tracking_range_multiplier`:

| Size class | Examples | Chunks |
|---|---|---|
| Tiny | critters, newt, snake, opossum | 6 |
| Fish | small fish, clams | 4 |
| Land | most land animals | 10 |
| Huge | mammoth, giraffid, baleen whale, whale shark | 14 |

**6. Animation packets.**
- `util/AnimationNetworkEvents` cancels Citadel's `AnimationEvent.Start` for `ComplexMob` on the server.
- In its place it sends `network/AnimationStartPacket(id, index, tick)` to `TRACKING_ENTITY` only. Today Citadel sends to every player on the server.
- The client handler bounds-checks the animation index.
- `startSeenByPlayer` sends the current animation and tick, so players who start watching mid-animation are in sync.
- `ComplexMobTerrestrial.setAnimation` resets the tick when the animation changes. This fixes the existing "switch mid-animation never syncs" bug.
- Toggle: `tracking_only_animation_packets`.

**7. Broken sound ids** (data only).
- Register `entity.big_cat.ambient_meow` and `entity.big_cat.warning`, temporarily pointing at existing files.
- The manatee uses `untamedwilds:entity.manatee.*`, whose subtitles already exist.

**New config class:** `config/ConfigPerformance` (common), wired in `ConfigBase`.

**Tests:**
- GameTests:
  - `despawn_never_loads_far_chunk`: home 2000 blocks away; after 200 ticks the chunk is still unloaded.
  - `burrow_control`: a tarantula still enters its burrow.
  - `eco_invariant`: a wolf's eco level is the same at rest and immediately after a hunting blow. This automates CLAUDE.md's top in-game check.
  - `hunt_backoff`: at most 12 scans in 600 ticks with no prey.
  - `anim_event_cancel`: the animation tick still advances and the animation completes.
  - `data_all_types_registered`.
- Static check: no `getEntityData(` under `client/`.
- Client smoke against a dedicated server running the B jar: zero exceptions, and every type × species renders.
- Soak S1 (A vs B): B's mean MSPT ≤ A's, and zero new log errors.

## P2: Parity fix, shared herd threat scan, panic spread (changes behaviour, census-gated)

- **Parity bug.**
  - *Cause:* vanilla only runs a goal's `tick()` on every other tick, by (server tick + entity id) parity. So a `% n` check inside a goal never fires for half the mobs.
  - *Fix:* a new `util/GoalTimer` counts down in goal evaluations, with a random start value.
  - *Applied to:* SmartAvoidGoal (and so HerdFleeGoal), HerdMigrationGoal, PredatorRelocateGoal, TortoiseHideInShellGoal, `EntityWildebeest` GreatMigration, EntityOpossum, and the `tick()` checks in SmartSwimGoal_Land, MeleeAttackCircleHerd, MeleeAttackCircle (`% 400`, `% 10`), SmartMeleeAttackGoal and SpitterAttackGoal.
- **Shared herd threat scan** (`HerdEntity.threatsFor`).
  - One entity query per herd every 10 ticks, over the herd's area padded by the detection range × `alert_range_scale`.
  - Each member still applies its own range, selector and line-of-sight tests, so detection is unchanged.
  - Herds spread over more than 48 blocks fall back to per-member scans.
  - Toggle: `shared_herd_threat_scan`.
- **Panic spread.**
  - *Today:* a herd panic issues every member's flee pathfind in the same tick.
  - *Fix:* each member's heading and cooldown are set at once, but the pathfinds are spread over `panic_path_spread_ticks` (8; 0 = old behaviour).
- **Tests:**
  - `parity_both_flee`: two rhinos with consecutive entity ids, plus a frozen wolf behind glass; both flee. This fails on A and passes on B.
  - `shared_scan_equivalence`: the same first spotters with the toggle on and off.
  - `panic_spread`: at most ceil(12/8)+1 flee pathfinds in any one tick, and the whole herd is moving by tick 15.
  - Census soak S2, at least 3 in-game days: no species that started at ≥ 5 hits 0; every predator type persists; MSPT ≤ A.

## P3: Eat, drink and alert animations

**P3a, plumbing (Java):**
- `ComplexMobTerrestrial.tryStartAnimation(anim, priority)`:
  - refuses NO_ANIMATION, or any animation not in `getAnimations()` (logged once);
  - refuses if a higher-priority animation is playing (ATTACK 4 > THREATEN 3 > ALERT 2 > EAT/DRINK 1);
  - otherwise resets the tick to 0.
- `stopAnimation(onlyIf)` sends index −1.
- New hooks `getAnimationDrink()` and `getAnimationAlert()`. `GrazeGoal.beginEating` now uses `tryStartAnimation`.
- About 20 herbivore entity classes get `static final` EAT (40 ticks, where missing), DRINK (100) and ALERT (40), **appended** to `getAnimations()`.
  - No DRINK for hippo, large_rodent or manatee. Aardvark gets ALERT only.
- Models suppress head-tracking during EAT and DRINK. ALERT keeps it, so the animal stares at the threat.
- Mounted specimens gain these poses automatically.

**P3b, offline solving.** No Java, render sheets only.
- **`scripts/fk_rig.py`:** generic forward kinematics over `java2bb.convert()` output, reusing the spec scripts' `rmat`.
  - *Self-test:* ModelWildebeest's world corners must equal `wildebeest_spec.world()` to within 1e-6.
- **`scripts/pose_targets.py`:** per model, the neck → head chain, the muzzle part and the head/body parts. It covers families: giraffid necks, deer antlers, kangaroo crawl.
- **`scripts/pose_reach.py`:** generalises `wildebeest_spec.graze_solve`. It grid-searches then refines, minimising total bend under these constraints:
  - **EAT:** muzzle 0.1–0.6 above the ground, nothing below ground, no neck through the torso.
  - **DRINK:** muzzle reaches water level past the front of the hitbox. The reach is saved as `DRINK_REACH_UNITS`.
    - **Giraffe:** foreleg splay plus a body drop, with the feet planted.
    - **Mammoth:** the trunk curls to the water and back to the mouth, never through the skull or tusks.
  - **ALERT:** head high, ears forward, no self-intersection.
- **`scripts/gen_keyframes.py`:** writes `// <gen:EAT>…</gen:EAT>` regions into `animate()`, idempotently, with per-family branches.
- **`scripts/pose_sheet.py`:** Blockbench rig with ground and water planes, side and 3/4 views, rest / EAT / DRINK / ALERT, batched 3–4 models per sheet.
  - If Blockbench isn't running, `scripts/soft_render.py` (numpy z-buffer, box-UV textured) renders instead.

**P3c:** after your approval, the generated regions are written into the models. `validate_keyframes.py` then re-parses the Java and checks every keyframe plus 5 interpolated samples per transition, with a 0.1-block margin for idle breathing.

**Tests:**
- `animation_index_sync` GameTest (every type): no duplicates, fewer than 127 animations, and the new animations are present.
- Static check: every declared animation has an `animator.setAnimation` block in its model.
- JUnit: keyframe round-trip.
- Client smoke: `debug anim` cycles every animation on every type with zero exceptions, and takes screenshot sheets at the EAT, DRINK and ALERT midpoints.

## P4: Alert state (vigilance before flight)

- **New state on `ComplexMob`:** `alertTicks`, `alertSource`, `raiseAlert(src, spread)`, plus a herd-level `lastAlarmTick`.
- **Where alerts come from:** the existing flee scan in `SmartAvoidGoal` / `HerdFleeGoal` is split in two.
  - Inside the detection range: flee exactly as now.
  - From the detection range out to detection × `alert_range_scale` (1.5): `raiseAlert`.
    - The spotter makes an `alarm` call once per herd per cooldown.
    - Herd-mates and other species within `alarm_network_radius` get alert ticks only, with no pathfinding.
- **New `entity/ai/AlertGoal`** (MOVE + LOOK, the same priority as flight):
  - stops moving and plays ALERT;
  - watches the threat;
  - steps aside the moment the animal flees, gets a target, or the threat leaves.
- GrazeGoal, DrinkGoal and SmartWanderGoal won't start while an animal is alert.
- **Tests:**
  - `alert_precedes_flee`: 3 deer and a frozen wolf at 1.3× detection range. All alert, none flee, all face the wolf within 30°, ALERT is seen, and the alarm sounds exactly once. Move the wolf inside the range and all flee within 30 ticks.
  - `alert_disabled_config` behaves like P2.
  - Census soak: less grazing time doesn't push any herd species to zero.

## P5: Cosmetic drinking and waterholes (no thirst stat)

- **`util/WaterFinder.sampleBank(level, origin, r, n)`:**
  - takes n random columns;
  - checks `isLoaded` first;
  - reads the heightmap surface;
  - looks for a water source with air above, beside a sturdy standing block with 2 air above it.

  The cost is bounded; there is no cube scan.
- **`entity/ai/DrinkGoal`:**
  - **Gate:** a constant-time time check. It skips animals in water, asleep, hunting, fleeing, alert, commanded, leashed, ridden, and nursing young.
  - **Water choice:** the herd's remembered waterhole, sampled ±6 so the herd spreads along the bank. Otherwise `sampleBank(24, 24)`. Retry in 2400–4800 ticks on failure.
  - **Approach:** stops back from the edge by the solved drink reach × size. Travel budget is 600 ticks.
  - **Drinking:** faces the water, plays DRINK, with the drink sound and a few splash particles at ticks 30, 50 and 70.
- **Waterhole gatherings:** when the leader starts drinking, herd-mates get their next drink time pulled to 20–200 ticks away. With probability `drink_after_wake_chance`, animals also drink soon after waking.
- **State:**
  - `ComplexMob.nextDrinkTime` (NBT "NextDrink"). Existing animals get a random value, so an updated world doesn't drink all at once.
  - `HerdEntity.waterhole`, revalidated every 2400 ticks while loaded.
- **Registered on:** bison, deer, equid, antelope, camel, giraffid, rhino, tapir, toxodon, macrauchenia, glyptodont, ground_sloth, diprotodont, kangaroo, caprine, wildebeest, mammoth, boar.
- **Tests:**
  - `drink_visit_reaches_bank` (waterhole structure): reaches the bank within 400 ticks, faces the water within 30°, DRINK plays, the drink sound fires, and the next visit is rescheduled.
  - `drink_no_water`: never starts, at most 24 samples, retry scheduled.
  - `herd_gathering`: 4 deer reach the bank within 600 ticks.
  - JUnit: scheduler jitter bounds.
  - Census soak: crocodile predation at waterholes doesn't zero any prey species.

## P6: Voice sets from real recordings

**P6a, sourcing** (`scripts/audio/`):
- **Plan file:** `voice_plan.json` lists, per type, each sound key (ambient, hurt, death, threat, alarm, call, step), the stand-in species recorded, search terms, clip count and maximum length.
- **Sources, in order, with the licence evidence saved:**
  1. US government public-domain recordings: NPS sound galleries, NOAA, USFWS.
  2. Wikimedia Commons, accepting only CC0 or public domain in the file's licence metadata.
  3. Freesound filtered to CC0, using the public HQ preview MP3 when the original needs a login.
  4. If none of those has it: a `MANUAL_DOWNLOADS.md` listing exact URLs.
  - Macaulay Library and xeno-canto are excluded because their licences aren't CC0.
  - Downloads go into fresh folders and are processed with `python -I`.
- **Stand-in species, examples:**
  - elephant rumble/trumpet → mammoth; alligator hiss/bellow → crocodilian; grey wolf → dire wolf;
  - elk bugle and red-deer roar → deer; zebra bark → equid; tapir whistle; capybara bark;
  - sea lion/walrus → pinniped; humpback (NOAA, streamed) → baleen whale; wombat → diprotodont.
  - Shared sounds: grazing bite, drink, heavy footstep, butcher.
- **`process.py`** (soundfile, numpy, scipy):
  - mono, 44.1 kHz;
  - high-pass at 60 Hz (30 Hz for elephant rumbles), noise gate, 20 ms fades;
  - onset split into separate calls;
  - per-category loudness (ambient −20, call −14, hurt −16 dBFS RMS; peak ≤ −1);
  - Vorbis q4 into `scratch/audio_stage`.
- **Review:** `review_page.py` builds `scratch/audio_review/index.html` with players, waveforms, source and licence per clip. **You approve each batch** (~6 family batches plus a shared batch).
- **After approval:**
  - clips go to `assets/untamedwilds/sounds/<type>/<key>_<n>.ogg`;
  - `sounds/CREDITS.md` and `credits.json` record URL, author, licence, date, processing and sha256;
  - the existing 48 files are marked as legacy upstream sounds.
- **Limits:** clips longer than 5 s are streamed. The jar budget is ≤ 8 MB.

**P6b, wiring:**
- **Fallback chain in `EntityUtils.getSound`:** species → type → default.
  - `alarm` → `threat`;
  - `eat` → `entity.grazer.eat`;
  - `drink` → `entity.animal.drink`;
  - `step` → the block's step sound;
  - `call` → none.
- **New getters on `ComplexMob`:** `getAlarmSound`, `getCallSound`, `getEatSound`, `getDrinkSound`, `getStepEvent`.
- **Pitch:** `getVoicePitch()` = vanilla (keeps the baby pitch-up) × clamp(sqrt(refScale / size), 0.75, 1.35). `refScale` is the type's median species scale.
- **Volume:** `getSoundVolume()` by size class (1.0 / 1.5 / 2.0).
- **Long-range calls:** played at volume 3–4, because the server only sends sounds out to 16 × volume blocks, giving 48–64 blocks. `attenuation_distance` only shapes the fall-off on the client.
- **Footsteps:** sized steps from the JSON `step` entry. The trivial `RAVAGER_STEP` overrides move into JSON.
- **Calls:** `tickCalls()` every 100 ticks, staggered. Herd leaders and solitary animals only, awake, not fleeing, tuned to about one call per `call_interval_ticks`.
- **Hooks:** eat sound on the GrazeGoal mouthful, drink in DrinkGoal, alarm in `raiseAlert`.
- **Data files:** `ModSounds`, `sounds.json`, `entities/*.json` (new keys), en_us subtitles `untamedwilds.subtitles.<type>.<key>`. **You approve the wording list.**

**Tests:**
- `validate_sounds.py`:
  - every id resolves to ModSounds or vanilla;
  - every file exists and has no orphans;
  - every file is mono, 44.1 or 48 kHz, peak ≤ −0.5 dBFS, within the length limit;
  - every event has a subtitle and an allowed licence in `credits.json`;
  - the size budget holds;
  - every class that plays a threat sound has `threat` defined.
- GameTest `sound_registry_validation`.
- JUnit: pitch and volume bounds.
- Soak: call rate ≤ expected.
- Client smoke: no "Unable to play unknown soundEvent".

## P7: Persistent carcasses

**`entity/EntityCarcass`** follows the `EntityMountedSpecimen` template: a plain MISC entity, not a mob.
- **Synced data:**
  - the stored animal's NBT, stripped as in `MountedSpecimenItem` (keeping Hunger, Condition and LifeTicks);
  - STAGE (fresh, picked, bones);
  - DECAY.
- **Server-only state:** food, food max, age, loot table, butchered, eaten-by, excluded (the killer's pack), feeders.
- **Behaviour:**
  - not pushable, but can be clicked;
  - tracking 6 chunks, update interval 40;
  - `tick()` only ages it and updates the stage every 20 ticks.
- **Caps:**
  - per area (6 within 48 blocks): the oldest carcass jumps to bones;
  - global (64 per dimension): above it, deaths fall back to today's instant behaviour.

**When a carcass spawns** (`ComplexMob.die`). All of these must hold:
- Ecosystem mode is on and `carcasses` is on.
- The animal isn't a baby, tamed, saddled or named.
- It isn't in a fluid.
- The death isn't `/kill`.
- It isn't a player kill (including a player's pet).

The mechanics:
- Set `carcassPending` before `super.die()`, so `dropFromLootTable` and `dropCustomDeathLoot` skip their drops.
- Spawn the carcass after `super.die()`, only if the death wasn't cancelled.
- Food pool: max HP × `carcass_food_per_health` (predated) or × the carrion rate (natural death).

**Feeding:**
- `satiateFromKill` draws the killer's and pack's shares from the carcass, with the same caps as today. The rest stays in the carcass.
- New `entity/ai/FeedOnCarcassGoal` for bear and hyena (`extra_scavengers` is opt-in):
  - searches nearby carcasses with a backoff;
  - up to 3 feeders at once;
  - a bite every 20 ticks with the EAT animation and sound;
  - at most 60 food per scavenger, so payout ceilings are unchanged.
- Kill stealing is unchanged: it is still resolved at the kill.

**Butchering** (`interact`):
- **Tools:** axe, sword (spears included), or the tag `untamedwilds:butchering_tools` (optionally `#forge:tools/knives`).
- **Loot:** an unspawned copy of the animal rolls the normal loot table with full player context, so looting, `killed_by_player` conditions, global loot modifiers and `doMobLoot` all work. Meat scales by the food left.
- **After:** the carcass goes to bones and the tool takes 1 durability. A second use does nothing.

**`client/render/RendererCarcass`:**
- the cached animal model rolled 90° onto its side, with a per-body-plan lying offset (generated by `fk_rig.py` into `CarcassPose`);
- deliberately not `deathTime`, which would tint it red;
- light dims 1.0 → 0.55 with decay;
- the bones stage is 3–5 vanilla bone items laid flat;
- flies use vanilla particles (client toggle).

**Gating:**
- `EcologyMode.allowsCarcasses`: Zoo mode keeps today's drops.
- `carcasses=false` runs the exact old code path.

**Tests:**
- **GameTests:**
  - `carcass_natural_death`: no item drops; food = the carrion pool.
  - `player_kill_unchanged`: drops and no carcass.
  - `predator_kill_split`: carcass food = pool − shares.
  - `scavenger_feeds`: food drops ≤ 60; feeders ≤ the cap.
  - `decay_despawn`, `butcher_once`, `nbt_roundtrip`.
  - `zoo_gate`, in its own batch.
  - `area_cap`.
  - `config_off_legacy`: the hyena is paid instantly.
  - `kill_command_no_carcass`, `death_cancelled_no_carcass`.
- **JUnit:** pool split.
- **Soak:** carcass count ≤ the caps; bears and hyenas persist; MSPT with features on ≤ +5% vs off.
- **Client smoke:** `debug carcass` for every type with zero exceptions, plus a screenshot sheet **for your approval of the look**.

## P8: Client detail LOD (optional, last)

- `client/model/ModelLod`: each model lists detail parts (eyes, teeth, claws, tufts), proposed by `scripts/lod_parts.py`.
  - Beyond `model_detail_lod_distance` (32 blocks; 0 = off), they are hidden at the end of `setupAnim`.
  - **They are restored first thing in the next `setupAnim`**, because models are shared between entities. This also keeps parts hidden by species logic hidden.
- New `config/ConfigClient` holds this and the carcass-flies toggle.
- **Tests:** JUnit state-machine test; an LOD on/off screenshot pair.

## P9: Release check

- Run `verify_all.py --soak --smoke`: every validator, the build and JUnit, GameTests 3×, a 60-minute S2 census soak, and the dedicated-server client smoke.
- Bump to 2.8.0 in `gradle.properties` **and** `mods.toml`.
- Update CHANGELOG, CLAUDE.md and memory.
- **Your first time in game** is the runClient visual pass, already outstanding for 2.7.0. Do both together.

---

## Testing without you in game

| Layer | Tool | Time | Catches |
|---|---|---|---|
| Static | `validate_sounds/static/keyframes.py` (`python -I`) | seconds | ids, licences, audio format, client-safe data access, animation coverage, poses through the ground |
| Pure logic | JUnit in `./gradlew build` | seconds | schedulers, backoff, pitch, pool math, LOD state |
| Headless server | `./gradlew runGameTestServer` (exit code = failures; 3 runs for flakiness) | 2–4 min | every behaviour test above |
| Server soak | `scripts/soak/soak.py` | 10–60 min | MSPT A vs B, perf counters, census, log errors |
| Client smoke | `scripts/soak/client_smoke.py` | 5–10 min | dedicated-server crashes, render and animation exceptions, screenshots |

**GameTest rules:**
- One behaviour test per batch, so a predator in one test can't scare deer in another.
- Force noon and set the gamerule explicitly.
- Timers are exposed as settable fields: `nextDrinkTime`, carcass age.
- Wait by polling.
- Config values are restored in `@AfterBatch`.
- Tests run in `run-gametest`, never in your `run/`.

**Soak:**
- **Server:** a standalone Forge 40.1.86 server in `scratch/soak/server` with Citadel; jar A or B is swapped in, and a pristine world copied before each run.
- **Settings:** fixed seed, `online-mode=false`, RCON on, view distance 10.
- **Scenarios:**
  - S1: a flat force-loaded 12×12-chunk area with ~20 herds, 6 packs and 30 critters summoned.
  - S2: a natural force-loaded 16×16-chunk area.
- **Sampling:** `/forge tps` every 10 s, `perf auto 10`, `census auto 1`.
- **Pass:**
  - B ≤ A mean MSPT (target ≤ 0.9 × A on S2);
  - features on ≤ +5% vs off;
  - zero new errors.

**Client smoke:**
1. Start the dedicated server.
2. Write `run-smoke/options.txt` (pause on focus loss off, render distance 8).
3. Launch `runClientSmoke`, which joins as SmokeBot.
4. Over RCON:
   - spectator mode;
   - for each type, summon every variant in a ring;
   - run `debug anim` through every animation and `debug shot` at the midpoints;
   - kill them.
5. Close the client.

A window opens, but nobody touches it.

**Pass:** zero client exceptions, every type × variant logged as rendered, and the EAT, DRINK and ALERT packets received.

## New config keys (all new, so their defaults apply to existing worlds; no existing default changes)

- **ConfigGamerules:**
  - Alert: `herbivore_alert`, `alert_range_scale` 1.5, `alert_ticks` 60, `alarm_call_cooldown_ticks` 300.
  - Drinking: `drink_visits`, `drink_interval_ticks` 24000, `drink_search_radius` 24, `drink_after_wake_chance` 0.5.
  - Carcasses: `carcasses`, `carcass_from_natural_deaths`, `carcass_lifetime_ticks` 12000, `carcass_bones_ticks` 6000, `carcass_area_cap` 6, `carcass_global_cap` 64, `carcass_max_feeders` 3, `carcass_meat_scales_with_food`, `extra_scavengers` [].
  - Sound: `long_range_calls`, `call_interval_ticks` 2400, `footstep_sounds`.
- **ConfigPerformance:**
  - `tracking_only_animation_packets`, `shared_herd_threat_scan` (soak-gated), `panic_path_spread_ticks` 8;
  - `hunt_scan_backoff_max_ticks` 200;
  - `count_toward_vanilla_mob_cap` true;
  - `tracking_range_multiplier` 1.0 (restart);
  - `debug_perf_counters` false.
- **ConfigClient:** `model_detail_lod_distance` 32, `carcass_flies`.

## What you'll be asked to approve (all from pages, sheets or screenshots, never by playing)

- Audio batches: about 7 rounds.
- The subtitle wording list: 1 round.
- EAT / DRINK / ALERT pose sheets: 5–7 rounds, including the giraffe splay and the mammoth trunk.
- The carcass look (lying pose, decay, bones): 1–2 rounds.
- The final screenshot sheets from the client harness: 1–2 rounds.
- The LOD comparison: optional.

## Risks and mitigations

- **Ecology shifts.** The parity fix doubles scans and flight for half the mobs. Alert costs grazing time. Waterholes expose prey to crocodiles. Scavenging becomes delayed. Each lands in its own phase with a toggle and a census soak, and payout caps are unchanged.
- **Shared-scan semantics.** Each member keeps its own line-of-sight test. The equivalence test guards it, and the 48-block fallback covers stretched-out herds.
- **Citadel drift in modpacks.** Guarded cancel at LOWEST priority. A duplicate start packet from another Citadel version is harmless.
- **Death and loot interplay with other mods.** Only non-player UW deaths are affected. Cancelled deaths spawn nothing. `carcasses=false` is the old path.
- **Shared model state (LOD, per-species scale).** Restore-first pattern plus a JUnit test.
- **Pose approval bottleneck.** Sheets are batched by family. The software renderer removes the dependency on Blockbench.
- **Licence ambiguity.** Strict whitelist, saved evidence, a validator gate. Gaps keep vanilla sounds.
- **GameTest flakiness.** Isolated batches, forced timers, polling waits, 3 runs.

## Critical files

- `entity/ComplexMob.java`: despawn, eco cache, alert, drink time, carcass spawn in `die()`, `satiateFromKill`, sound getters, pitch.
- `entity/ComplexMobTerrestrial.java`: `tryStartAnimation`, `setAnimation` tick reset, `startSeenByPlayer`.
- `entity/HerdEntity.java`: shared scan, waterhole, counts.
- `entity/ai/{SmartAvoidGoal,HerdFleeGoal,GrazeGoal}.java`, plus new `AlertGoal`, `DrinkGoal`, `FeedOnCarcassGoal`, `util/GoalTimer`, `util/WaterFinder`.
- `util/EntityDataListenerEvent.java`, `network/{UntamedInstance,SyncTextureData}.java`, plus new `AnimationStartPacket` and `util/AnimationNetworkEvents`.
- `util/EntityUtils.java` (`getSound`, `getSkinFromEntity`), `init/{ModEntity,ModSounds}.java`, `sounds.json`, `entities/*.json`.
- New `entity/EntityCarcass` and `client/render/RendererCarcass`. Templates: `EntityMountedSpecimen`, `RendererMountedSpecimen`, `item/MountedSpecimenItem`.
- `build.gradle`; templates for the forward-kinematics tool and pose solver: `scripts/java2bb.py`, `scripts/wildebeest_spec.py`.

---

## Progress log

### 2026-11-16 (session 1)

- **P0 harness:** done.
  - The baseline jar is at `scratch/baseline/untamedwilds-2.7.0-A.jar`.
  - `build.gradle` gained a `gameTestServer` run, a `clientSmoke` run, the server run with `--nogui` in `run-server`, JUnit, Citadel dev-deobf (runtime only), and jar excludes for the tests.
  - GameTest structures come from `scripts/gen_gametest_structures.py` (`platform`, `waterhole`).
  - `gametest/` holds `UWGameTests`, `PerfTests`, `DataTests`, `EcologyTests`, `AmbienceTests` and `CarcassTests`.
  - **Coordinate trap:** test-relative coordinates sit one block below the template's own. Grass is at helper y 2 and animals stand at helper y 3 (`UWGameTests.FEET_Y`); the harness test asserts this.
  - **1.18 API trap:** `thenWaitUntil(tick, …)` means EXACTLY that tick. For "within N ticks", use the tickless form and rely on the test timeout.
  - **Counters:** worldgen fauna run alongside every test, so tests assert on per-animal counters (`perfHuntScans`, `perfAnimStarts`, `perfFleeScans`, `drinkVisits`), not the global `PerfCounters`.
- **P1:** done and passing.
  - Species-data sync: full `EntityDataHolder` NBT per type on `OnDatapackSyncEvent`; all 51 types round-trip.
  - Null-safe client helpers: `ComplexMob.speciesFlag()`, `ISpecies.getRawSpeciesName()`, `getSkinFromEntity()`.
  - Despawn home check: every 20 ticks, never on an unloaded chunk.
  - Eco-level per-tick memo: invalidated on every bonus apply/clear and on `setHealth`. Test `eco_level_unchanged_after_hunting_blow` passes.
  - Throttles and backoffs; tracking ranges by size class; `getAllParts` cached in 8 models plus the generators.
  - Tracking-only animation packets (Citadel's `AnimationEvent.Start` cancelled for our mobs, `AnimationStartPacket`), plus `startSeenByPlayer` catch-up and `setAnimation` resetting the tick on a change.
  - The unregistered `entity.big_cat.ambient_meow` is now registered.
  - The manatee's broken ambient id was removed; it stays silent until P6.
  - `playSound` is null-safe.
  - Protocol version is now "2".
  - Dropped from the plan:
    - **The `setInLove` change.** Love lasts exactly 600 ticks, so its refresh is needed every 600 ticks anyway.
    - **The HerdEntity adult-count memo.** `isBaby()` flips outside the herd, and with the eco memo in place the hot caller is gone.
- **P2:** done.
  - `util/GoalTimer` fires on the first call in each N-tick window. Twelve goal sites were patched; windows paired with a `% N` window check stay aligned, the rest are staggered by entity id.
  - **Parity confirmed:** deer with consecutive ids now scan 12 and 12 times in 120 ticks. Before the fix, one of them scanned 0 times.
  - Shared herd threat scan (`HerdEntity.sharedThreatCandidates`, toggle, default OFF): the equivalence test passes, with 6 of 6 deer agreeing with their own scans.
  - Panic pathfinding is spread over `panic_path_spread_ticks`.
  - Census soak still to do.
- **P3a:** done.
  - Plumbing: `tryStartAnimation` with priorities, `stopAnimation`, the drink/alert hooks, and `cancelIdleAnimationIfBusy`.
  - GrazeGoal and feeding call `tryStartAnimation`.
- **P3b:** done; awaiting the user's approval.
  - `scripts/fk_rig.py` reproduces `wildebeest_spec` to 8.8e-4.
  - `scripts/pose_reach.py` matches the hand-solved wildebeest graze to within a few degrees. The neck root may drop by up to 6 units, which the equid's withers-high neck needs.
  - `scripts/soft_render.py` renders the sheets and `scripts/pose_review_page.py` builds `scratch/poses/index.html`.
- **P4:** done. `AlertGoal`, plus alert raised from the flee scan's outer band (one search). Grazing, wandering and drinking stand down while alert.
- **P5:** done.
  - `util/WaterFinder.findBank` probes a 3-block grid, because random samples missed a 6×6 pond 70% of the time.
  - `DrinkGoal` handles waterhole memory and herd gathering. The drink sound stays silent until P6.
- **P6a:** done by a subagent; awaiting the user's approval.
  - 153 CC0/PD clips, 3.3 MB, at `scratch/audio_review/index.html`; credits in `scratch/audio_stage/credits.json`.
  - Gaps: giraffid, tapir, wildebeest, softshell turtle, kangaroo (except a stand-in alarm), plus a few individual keys.
- **P7:** code done; tests running.
  - `EntityCarcass` and `RendererCarcass` hold the loot and the food pool and track who has eaten.
  - `FeedOnCarcassGoal` is registered on bears and hyenas, plus any `extra_scavengers`.
  - Butchering uses the `untamedwilds:butchering_tools` tag.
  - `satiateFromKill` and `loseCarcassTo` draw from the carcass.
- **Waiting on the user:** pose approval (gates P3c keyframes) and audio approval (gates P6b wiring and asset install).
- **End of session 1:** all 19 GameTests pass (`All 19 required tests passed`). Gradle then reports "daemon disappeared", because the test server calls `System.exit`; treat the log line as the result. `./gradlew build` is SUCCESSFUL and the jar contains no gametest classes.
  - **Bug fixed on the way:** `ComplexMobTerrestrial.aiStep`'s 200-tick "go to sleep near home" check ignored a negative `forceSleep` (keep-awake). An animal woken by a fight (`forceSleep` −300) could fall straight back asleep.
  - **WaterFinder:** probes a vertical window around the animal's own height instead of reading the heightmap. GameTest-world heightmaps lag behind placed templates (y 63 over a pond at −58), and water far above or below is unreachable anyway.
  - **Not installed** in the Test instance: the jar is still versioned 2.7.0 and lacks the P3c animations and P6b voices.
  - **Still to do:** P3c and P6b after approval; carcass-look screenshots (client smoke harness); census soak (`scripts/soak/soak.py`, written but not yet run); P8; P9.

### 2026-11-16 (session 2): "continue all in one pass"

The user asked for everything finished in one go. The staged poses, voices and carcass look were installed **without** a separate approval round. All of it is uncommitted and reversible: re-run the generators against older inputs, or delete the generated regions.

- **P3c:** done. `scripts/gen_keyframes.py` writes a `// <gen:idle-poses>` region after `animator.update(...)` in each model, and patches each entity (`EAT`/`DRINK`/`ALERT` statics appended to `getAnimations()`, the getters, `getDrinkReach()`).
  - **17 types** got poses. Mammoth, boar and hippo got none: none of the three registered the drink or alert goals, mammoth and hippo keep their hand-made EAT, and the boar's "eat" is its rooting dig.
    - The alert goal is only on the 12 light flee-first herbivores, matching the design: heavy herbivores confront instead.
    - The boar's DRINK pose is solved, so boar drinking was wired at the end of the session (see below).
    - The mammoth's trunk-curl drink was never solved and stays open.
  - **Solver changes:**
    - The neck root may drop 0–6 and slide 0–3 units.
    - Hair counts as soft, overlap is tolerated (at most 8 points inside the torso), and `EAT_MAX` is 0.7.
    - These were needed because the hand-solved wildebeest graze was infeasible under the first constraints. The solver now reproduces it (neck_base −77 against the hand-solved −82).
  - **ALERT ears** flare by yaw, not pitch. With pitch, the mammoth's ears covered its face.
  - **Drink reach:** each entity's reach = its solved muzzle reach × `getMobSize()`. DrinkGoal stops at that reach, and the GameTest asserts against it.
  - **New GameTest `animation_tables_are_consistent`** covers all 39 types with animations.
  - **Giraffe:** drinks with a neck-only pose. The leg splay was dropped from the solver and is still open.
- **P6b:** done. `scripts/audio/install_voices.py` installs the 153 clips (77 new events, 4.35 MB) and updates:
  - the `sounds.json` merge (tab indent kept, additions only);
  - the `ModSounds` `// <gen:voices>` block;
  - each type's entity JSON `sounds`;
  - en_us subtitles;
  - `sounds/CREDITS.md` and `credits.json`.
  - **Validator:** `scripts/validate/validate_sounds.py` passes (117 events, 201 files, every clip credited).
  - **Bug caught:** the validator found `entity.big_cat.warning` was never registered (big_cat threat); it now points at ambient.
  - **ComplexMob:**
    - getters `getAlarmSound` (falls back to threat), `getCallSound`, `getEatSound`, `getDrinkSound`;
    - `playFootstep` for 8 megafauna;
    - size-scaled pitch (`ambience.pitch_by_size`) and volume;
    - `tickCalls()`: herd leaders and lone animals, volume 3, so calls carry ~48 blocks.
  - **Hooks:** GrazeGoal plays the eat sound at its mouthful, DrinkGoal the drink sound, the carcass the butcher sound.
- **P7 look:** `scripts/gen_carcass_pose.py` solves a lift/shift for 48 types so the torso and legs rest on the ground after the 90° roll.
  - Deer and mammoth sink their antlers or tusks more than 0.25 blocks when rolled, so they lie in their solved sleep pose instead.
  - Preview: `scratch/poses/sheet_carcass.png`.
- **Tests:** all 20 GameTests pass (`gametest_p6b.log`).
- **Boar:** gets DrinkGoal (priority 4, below sleep) plus its solved DRINK, via `gen_keyframes.py boar` (reach 14.9u).
- **DrinkGoal freeze fixed:** a re-run failed `drink_visit_reaches_the_bank`. The bison stood still 2.5 blocks from the pond for the rest of the test.
  - **Cause:** `moveTo(x, y, z)` plans to block accuracy 1, and a 1.7-wide animal reaches a waypoint within half its width. So the path ended up to ~1.8 blocks short, outside the 1.2 fallback.
  - Every re-path from there returned an already-finished path, and it re-pathed EVERY tick for 600 ticks. The test only passed before because the random bank choice happened to suit it.
  - **Fix:**
    - The last 2.5 blocks are walked straight with `MoveControl.setWantedPosition`.
    - Re-pathing is limited to once every 20 ticks.
    - After 60 ticks of final approach the animal drinks if within 1.2 blocks, and otherwise gives up and retries later.
  - 20/20 pass after the fix (`gametest_p9b.log`).
  - `FeedOnCarcassGoal` got the same final approach (it had more slack, since its reach is half of each width plus 0.9), plus a re-path throttle. 20/20 still pass (`gametest_p9c.log`).
- **Installed:** `untamedwilds-1.18.2-2.8.0.jar` is in the Test CurseForge instance, and the 2.7.0 jar was renamed to `.disabled`.
  - The jar has no gametest classes, 201 .ogg files and `CREDITS.md`.
- **First A/B soak was INVALID.** The "on" run measured +97% mean MSPT, but `soak.py` reused the previous run's world, so the "on" run carried the "off" run's animals plus a second set of summons. VS Code's Java indexers also started between the runs.
  - **The harness also leaked config:** `--set` wrote the config in place and never reverted it.
  - **Fixed:** each run now deletes `run-server/soak_world` (pass `--keep-world` to opt out) and restores the config after the run.
  - The "off" run on its own is a valid smoke soak: 28 summoned animals, no mod errors, mean 2.43 ms/tick.
- **Soak (fresh worlds):** see the next entry.
- **Release:** version 2.8.0 in `gradle.properties` and `mods.toml`; CHANGELOG entry written.

### 2026-11-18: fresh-world A/B soak and release state

- **Soak, both runs from a fresh seed-2808 world, 15 minutes each, the S1 scenario (28 summoned animals plus worldgen fauna):**

  | Run | Mean MSPT | p95 MSPT | Animals (start → end) |
  |---|---|---|---|
  | Shared scan OFF | 8.36 | 12.19 | 105 → 104 |
  | Shared scan ON | 8.38 | 11.57 | 93 → 89 |

  - **Difference:** +0.2% mean, −5% p95.
  - Both sit above the earlier 2.4 ms because a fresh world pays chunk generation over the force-loaded 192×192 area.
  - Both servers stopped cleanly. There were no mod errors and no crash reports, and no type went to zero.
- **Decision: `performance.shared_herd_threat_scan` ships OFF.** At this scale it buys nothing measurable. The plan's condition for turning it on also required a multi-day census soak, which was not run.
- **What the soak did NOT test:** there were no births, deaths or kills in either run. 15 minutes is under one in-game day, and the predators were summoned well fed. Hunting and carcass behaviour are covered by the GameTests only.
  - **Still open:** the S2 multi-day census soak, e.g. `soak.py --minutes 60` with a predator-hungry scenario.
- **Release state:**
  - 2.8.0 is built, passes 20/20 GameTests, and is installed in the Test instance.
  - Not committed; the last commit is still `20754df`.
  - runClient is not done, for 2.7.0 and 2.8.0 together.

### 2026-11-18 (later): missing drink poses, derived voices, LOD analysis

- **Giraffe splay drink:** `pose_reach.solve_body` handles the body; the neck is then solved on top of it (`Problem.set_body`).
  - **Body stage:** forelegs splayed a fixed 30°; the hooves are counter-rotated so the soles stay level. The body pitch, a foreleg pitch and three hind-leg pitches are searched.
  - **The body drop is derived, not searched:** it is a pure vertical shift, so it is set to put the lowest hoof exactly on the ground. As a free variable the search left the hind hooves 0.13 underground.
  - **Result:** drop 1.8u, pitch −8°, front hooves 14u out each side, every hoof within 0.1 of the ground. The neck base now bends −61° instead of −82°. Reach 31.2u.
  - EAT/ALERT for giraffe and mammoth, and a bison control re-solve, are byte-identical to before the refactor.
- **Mammoth trunk drink:** `drink_style="trunk"`. The trunk tip is SOLVED into the water.
  - `smooth` makes the six trunk joints one linear ramp; independent joints zig-zagged (+12/−14/+11/−10). `drink_band` (−3.2, −2.2) keeps the tip submerged.
  - Then the approved EAT coil (`trunk_curl_java`) brings it to the mouth: reach / hold / coil / hold, twice, 100 ticks.
  - DrinkGoal is now registered on the mammoth. Reach 44.1u.
- **`fk_rig.pose_over_move`** is now the single reader of a pose record, used by the solver, `soft_render` and `gen_keyframes`.
- **DrinkGoal:** the stand point aims the muzzle 0.35 past the bank edge, and the arrival tolerance is 0.3, reachable because the last stretch is steered straight.
  - **Why:** the new mammoth GameTest showed the trunk tip landing 0.41 blocks short, on the bank. That test now asserts dist ≤ reach + 0.15.
- **Derived voices** (`scripts/audio/derive_voices.py`): 46 clips built only from staged CC0/PD recordings, using tape-style pitch shifts, pitch glides, loudest-window cuts, honk series and filters. Nothing is synthesised.
  - **Coverage:** giraffid, tapir, wildebeest, softshell and tortoise threat, kangaroo, plus the missing keys for diprotodont, antelope, macrauchenia and large rodent.
  - **Credits:** each record lists its sources (`derived_from`) and carries batch D1. The review page shows them as a separate "DERIVED, not heard by the designer" section.
  - **Validator:** `validate_sounds` OK, 199 clips, 4.98 MB. The derived clips needed peak headroom of −1.4 dBFS, because Vorbis overshoots.
  - **Not done, on purpose:** the beaver tail slap keeps the vanilla splash, and the salamander and newt stay silent.
- **Threat hooks:** only bear, boar and the canids ever played a "threat" sound, so the new hisses and growls would have been silent.
  - New `ComplexMob.threatCallOnNewTarget`: once per new target, at most every 200 ticks, never from a baby.
  - **Kangaroo:** on `setTarget` and at the start of a spar.
  - **Softshell:** on `setTarget`, only against its attacker or a player.
  - **Tortoise:** in `TortoiseHideInShellGoal.start()`.
- **P8 LOD analysis:** NOT built (see the session report).
  - **Gain:** named detail parts are 8.4% of 1425 boxes overall, and only 3–4% on the herd models that make up most of the screen.
  - **Visibility:** eyes are still ~3 px at 32 blocks on 1080p.
  - **Risks:** the spyglass magnifies exactly what a distance LOD hides, shared-model state, and shader shadow passes.

### 2026-11-18 (evening): first in-game feedback, done without sign-off (user: "do the rest of the build without my signoff")

- **Ground sloth "far too thin" and "the sag at the bottom of the face".** The user didn't say body or coat, so both were done.
  - **Body (`ModelGroundSloth`, UV-safe):**
    - per-box `setScale` widths: hips/rump 1.18, mid 1.16, back 1.14, chest 1.12 (multiplied into the breathing scale);
    - limb roots pushed out by the same factors, so the legs stay outside the wider body;
    - the torso is now 26.4u wide (was 22.4), width/length 0.52.
  - **Jaw:** `jaw_mega` setScale Y 0.55 about its top pivot. It was a 6.9u box hanging 5.6u below the skull; it now hangs 2.8u.
  - **`fk_rig` learned `pose_targets.STATICS`** (per-part scale plus `pivot_x`), so the pose and carcass solvers see the rendered shape.
    - Re-solved: EAT neck −12 → −20 to reach the grass with the shallower jaw; DRINK reach 48.8u; carcass lift 11.6 → 13.22.
    - The `fk_rig` self-test still passes.
- **Coats ("look like rugs", "much too patterned"):**
  - **Cause:** the shared `lock()` made every lock in a 3-texel column the same length, so each column was a repeating sawtooth of identical small gradient rectangles.
  - **Fix:** new `scripts/woollib.js`:
    - world-space drifting columns of varied width;
    - a random length for every lock;
    - ragged sub-column starts, and tips blended over the next root;
    - clumps (`CLUMP`/`INDIV`/`RAGGED`), softened matted patches and a broad tone drift.
  - **Method (`scripts/wool_repaint.js`):** run the painter on the same rig in `WOOL_MODE` 'old' and 'new'. The texels that differ are the coat; only those replace texels in the shipped PNG.
  - **Proof the rig matches:** outside the coat, 'old' reproduces every shipped skin exactly (`reproMismatch` 0; the mastodon has 2 edge texels).
  - **Repainted:** woolly, steppe mammoth, mastodon, and all five sloths. Backups are in `scratch/wool_backup_2026-11-18/`.
  - **Measured:** texel-to-texel noise unchanged or lower (woolly 7.8 → 7.4).
    - A "tip alignment" proxy rose (0.12 → 0.24) even with clumps off. It rewards long smooth ramps, not tiers, so the side-by-side crops decided instead: the old coat is a mosaic of identical rectangles, the new reads as hair. The clumped variant was kept.
- **Blockbench trap seen again:** a second `MM.show` in one project can leave a cached texture on screen. Use the `showFresh` recipe (remove every Texture, add a new one) before trusting a preview.
- **Ecology soak S2** (`scenarios/s2_ecology.txt`): 75 minutes, 33 animals plus hungry predators. `soak.py` now keeps Windows awake while it runs. Results are in the next entry.

### 2026-11-18 (late): S2 ecology soak, a vigilance bug, and what a player-less soak can and cannot show

- **Before run** (stopped at day 1.85 once the evidence was in): 2 predator kills, 2 other deaths, 6 starvation deaths.
  - Four were alert-capable herbivores: a moose, a Megaloceros, a sika and a tetrameryx. That was 3 of the 5 deer.
  - The deer herd spawned ~30 blocks from two big cats: outside the deer's 20.6-block flight range, inside the 31-block alert band.
- **BUG:** `SmartAvoidGoal` re-raised the alert on every scan while a predator stayed in the band, and GrazeGoal never starts while alert. Herds stared at a resting predator until they starved.
  - **Fix: habituation.** `ComplexMob.alertStreak` / `vigilanceRelaxUntil` relax the animal after `ambience.alert_max_ticks` (200; 40% of that when hungry) for `ambience.alert_relax_ticks` (400). Spread alerts respect the relax window. Flight is unchanged.
  - **New GameTest `alert_habituates_so_prey_can_feed`:** a hungry deer with a frozen wolf in the band relaxes and feeds. 22/22 pass.
- **After run** (45 minutes, mean 3.56 MSPT, no mod errors): 6 predator kills and 7 other deaths (mostly canids killed by defending herds); deer starvation fell from 3 to 1.
  - Four Macrauchenia starved late (days 1.3–2). They were natural worldgen spawns, not the scenario's.
- **Limitation:** forage regrowth (`ForageRegrowthHandler`) samples around PLAYERS, and vanilla grass-spreading random ticks also need a nearby player. A player-less soak eats its force-loaded area bare, so herbivore starvation in a soak is NOT a balance signal.
  - **Next step if wanted:** an off-by-default debug flag that also regrows around force-loaded chunks, then a multi-day soak.
- Runs differ in species and worldgen fauna (12 vs 28 predators), so only like-for-like signals are compared.

### 2026-11-19: ecology soak WITH grass regrowth (S3)

- **New test hook `performance.debug_regrow_forced_chunks`** (default false): with no player in the dimension, `ForageRegrowthHandler` uses one virtual player at the centre of the force-loaded area (same radius and rate). Every half day it logs `[soak] forage day N: regrown ...; ground cover plant/bare grass/dirt/other` (400 samples).
  - `soak.py --set` now inserts a key that is missing from the config file.
- **Run:** 90 minutes, 4.5 in-game days, the S2 scenario (hungry predators). Mean 6.52 MSPT, no mod errors.
- **Forage held:**
  - plant-covered ground 35 → 58–72 of 400; dirt never above 5;
  - ~270–420 plants and up to 24 grass blocks regrown per half day;
  - ~70% of this seed's spawn area is not grass at all ("other" ≈ 270/400).
- **Population:** herbivores 78 → 62, predators 15 → 13. The first births of any soak: 4 black wildebeest calves.
  - 12 predator kills: boar 3, deer 3, wildebeest 2, equid 2, antelope 1, king crab 1.
- **Starvation:** 7 in total.
  - Two lone small canids early on (maned wolf, grey wolf).
  - Five herbivores: onager (day 1.3), black wildebeest (1.9 and 3.3), Macrauchenia (3.2 and 3.7).
  - Herd condition dipped while forage was at its minimum (black wildebeest 0.84 → 0.55 at days 1.4–3), then RECOVERED as grass regrew (0.79–0.81 at days 3.4–4.5; Macrauchenia 0.45 → 0.81).
  - **Reading:** ~60 grazers start above this forage-poor area's carrying capacity, then settle. A density response, not an AI lock. No deer starved (3 did before the habituation fix).
- **Open:** 7 "other" deaths (wildebeest 5, deer 1, antelope 1). The census lumps every non-carnivore, non-starvation death together (falls, drowning, suffocation, herbivore defenders, vanilla mobs).
  - Territorial clashes are cosmetic, so not those.
  - **Next step if wanted:** log the damage source of "other" deaths when debug_perf_counters is on, then rerun.
