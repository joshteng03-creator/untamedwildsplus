# New animals (2.9.0) — plan of record

Approved by the user 2026-11-19: "do B onwards in that order and change dire_wolf name to canid". The order is **B → A →
C → D** (the order proposed). Sign-off mode was not specified, so the 2.8.0 mode applies: build, then review sheets plus
one verification list. Per-skin sign-off is used instead if the user asks for it. Nothing is committed unless the user
says so (the last commit is still `20754df`; 2.7.0 and 2.8.0 are uncommitted).

Theme rule (2026-10-02): Middle–Late Pleistocene and Holocene only. Never insert into the middle of a species list,
because variants are saved as integer indices.

## Done first: canid rename

The `dire_wolf` type is shown as **Canid**: the type name, spawn egg and subtitles changed (lang only). The registry ID
stays `untamedwilds:dire_wolf`, so saved worlds, commands, loot and spawn tables keep working. This is the same call the
user made for the proboscideans. The encyclopedia category was already "Canids".

## B — new species on existing bodies (16)

| Type | Species (internal id) | Notes |
|---|---|---|
| ground_sloth | lestodon, paramylodon (MYLO head); nothrotheriops (small, narrow skull) | painter `paint_ground_sloth.js` + woollib |
| glyptodont | panochthus | rosette shell, spiked tail tube (no club) |
| bison | bootherium (helmeted musk ox), antiquus | musk-ox / horn flags |
| hippo | european, pygmy, malagasy_dwarf | the hippo model is upstream (never rebuilt) |
| equid | haringtonhippus (stilt-legged), kiang | |
| dire_wolf (Canid) | ethiopian_wolf, golden_jackal | canid leg-proportion flags |
| big_cat | clouded_leopard | |
| tortoise | aldabra, galapagos | giant scale |

## A — upstream art already in the repo (7)

catfish shovelnose, goonch, piraiba, mekong, wels; snake sea_krait; giant_clam crocea. Each needs a UV-fit check against
the current model, then data.

## C — existing body plus a new feature

- mammoth: *Stegodon orientalis*, *S. florensis*: a new long, close-set tusk family on the rebuilt proboscidean rig.
- tortoise: *Meiolania platyceps*: horned skull and clubbed tail.

## D — new body types (one at a time, full remodelling SOP)

Orca (toothed whale), wolverine (mustelid), ground birds (moa, *Aepyornis*, *Genyornis*), *Gigantopithecus* (great ape).

## Steps for every species

1. Species entry appended to the type JSON: life-size scale, health/attack, flags, diet and ecology tags, group size,
   rarity, spawn biomes.
2. Lang (name and scientific name).
3. Skin painted on the real rig in Blockbench, using woollib for fur. Juvenile regions are filled where the type has a
   separate baby model (bison, boar, rhino, big_cat, bear).
4. Encyclopedia entry (`gen_encyclopedia.py --write`), icon (`gen_icons.py`) and discovery advancements.
5. Eco-level check, so the newcomer does not re-sort who can hunt whom.
6. Voices: the type's existing sounds, with pitch following size automatically.
7. For C/D only:
   - re-solve EAT/DRINK/ALERT (`pose_reach.py` → `gen_keyframes.py`) and the carcass pose (`gen_carcass_pose.py`);
   - GameTests;
   - an ecology soak with `--set performance.debug_regrow_forced_chunks=true`.
8. Release 2.9.0: CHANGELOG, build, install into Test and Planet Wild, verification list.

## Progress log

- 2026-11-19: canid rename done. Research and encyclopedia drafts for B/A/C delegated (`scratch/new_species_research.json`, `scratch/encyclopedia_text/text_M.json`).
- 2026-11-19: **B and A DONE** (16 + 7 species, 333 total, `validate_species.py` OK).
  - Batches are in `scripts/species_batches/` (a, b1–b5), applied by `scripts/add_species_batch.py`.
  - **Skins:**
    - canids, the 3 sloths (woollib), bison and clouded leopard are painted on their rigs;
    - hippos, equids and tortoises are luminance-rank recolours (`paint_recolour_b.py`);
    - Panochthus (`paint_panochthus.js`) is an osteoderm mosaic on the Doedicurus club rig.
  - **Juvenile regions:** filled for clouded leopard, antiquus and bootherium.
  - **Zoo side:** 23 encyclopedia entries and 22 icons (`gen_icons` RECOLOR + hand-mapped sea krait). Advancements unchanged (per type).
  - **Food web:** checked with the eco-level formula; nothing re-sorted.
  - **Model:** `ModelEquid` gives Haringtonhippus (variant 10) a slim torso on unchanged legs.
  - **Known limits:**
    - Bootherium uses bison horns.
    - (see the C entry below for Stegodon / Meiolania)
- 2026-11-19: **C DONE** (336 species, build passes).
  - **Stegodon** (mammoth 12 orientalis, 13 florensis):
    - **Tusks:** a new STEGO tusk family (tuskShape 6) in `proboscidean_spec.py`, designed by `scripts/stegodon_tusk_design.py`. Trunk overlap is ≤2% with a 0.25u margin. The inner gap is 4.0u at 1.0× and 0.9u at 1.35×, so the tusks nearly touch but never cross. Segment 3 → 4 turns 9° of yaw (no z-fight).
    - **Java:** `ModelMammoth`'s TUSK_ROT gained one row (reproduced by the generator's own code path; the SMALL row matched byte for byte), and the clamp became `TUSK_ROT.length - 1`. The rest of the generated file was NOT regenerated, which keeps 2.8.0's gen:idle-poses region.
    - **Poses:** rest and sleep lifts are unchanged. The lowest tusk is 14.1u against the 5.9u of the shipped straight-tusked species, so every type-wide pose clears the ground.
    - **Skins:** painted on a species-BAKED rig (`scripts/probo_species_rig.py`). It freezes the UVs of family-scaled boxes; without that the ears painted 40% of their rect. Shown-part coverage equals the Asian elephant's.
  - **Meiolania** (tortoise 8, flag `meiolania`):
    - **Parts:** 9 new boxes on `ModelTortoise` (horns, 2-ring tail, club, 4 spikes), packed into texels transparent in every shipped tortoise skin, and showModel-gated. The stub tail is hidden.
    - **Poses:** the droop is SOLVED (club 0.42 off the ground), as is the sit lift (0.2705 rad), in `scripts/tortoise_meiolania_check.py`. Z-fight is clean.
    - **Skin:** `scripts/paint_meiolania.py`, a recolour plus face-painted new parts.
    - **Carcass pose:** unchanged (`HIDE["tortoise"]`, which `gen_carcass_pose` now reads).
- 2026-11-19: **D1 orca DONE** (NEW TYPE `orca`, displayed as "Toothed Whale"). Build passes, GameTests 22/22, validate_sounds OK.
  - **Species:** orca (scale 1.937, 7 m) and false killer whale (1.383, 5 m).
  - **Pipeline:**
    - `scripts/orca_spec.py`: 17 boxes on 256×128; z-fight clean; fins solved above the hitbox floor; tip directions checked numerically. Both sign errors the grey build showed are fixed (the dorsal leaned forward, the paddles pointed up).
    - Port: `gen_model_orca.py` (verify_port EXACT 17/17).
  - **Java:** `EntityOrca`, a pod predator on ComplexMobAquatic:
    - **Hunting:** `HuntPackMobTarget` with the `aquatic` diet, so it never hunts players (the diet filter excludes them), plus `MeleeAttackCircle` and the pack-hunter damage reduction.
    - **Movement:** school goals keep the pod together; breaching and cosmetic BreathAir.
    - **Breeding:** the predator gate.
    - **Model:** an up-and-down fluke stroke (chainWave); yaw ring-buffer bend; dorsal height per animal (male 1.0, female 0.55, `finScale` 450 for the false killer whale).
  - **Data and assets:**
    - **Data:** `orca.json`, loot (cetacean meat + blubber), `water_ocean` spawn entry (weight 1, pods 3–6), egg, lang; vanilla dolphin sounds (pitch follows size).
    - **Skins:** `scripts/paint_orca.js`, crisp world-space pattern (eye patch, belly to flank patches, saddle, white fluke undersides).
    - **Zoo side:** hand-drawn icons; encyclopedia in `scratch/encyclopedia_text/text_N.json`.
  - **Incident, fixed:** I overwrote `text_D.json` (the October drafts) without reading it. It was restored from the October generator `gen_d.py`, recovered from the session transcript. New drafts go in NEW letters only.
- 2026-11-19: **D2 mustelid DONE** (NEW TYPE `mustelid`). Build passes.
  - **Species:** wolverine (scale 0.80) and honey badger (0.495).
  - **Pipeline:**
    - `scripts/mustelid_spec.py`: 26 boxes on 128×64; z-fight clean.
    - **Solved poses:** SIT pitches 18° with the rump on the ground, forepaws planted and the tail lifted; SLEEP is belly-down, legs tucked, chin and tail on the ground.
    - **Walk:** checked, with no digging.
    - Port: `gen_model_mustelid.py` (EXACT 26/26).
  - **Java:** `EntityMustelid`, a solitary predator on the bear pattern.
    - Diet: small game, plus grazer/browser calves via the eco level.
    - Behaviour: scavenges with FeedOnCarcassGoal but no kill-stealing; the land-predator breeding gate.
  - **Data and assets:**
    - **Data:** predators spawn table (weight 3), leather loot, vanilla fox sounds.
    - **Skins:** `scripts/paint_mustelid.js` with woollib short locks. Wolverine: flank stripes meeting over the rump, silver brow band. Honey badger: silver mantle.
    - **Zoo side:** icons, encyclopedia (`text_O.json`).
    - **Carcass poses:** mustelid and orca rows added.
- 2026-11-19: **D3 ground birds DONE** (NEW TYPE `ground_bird`, "Giant Ground Bird"). Build passes, GameTests 22/22.
  - **Species:** South Island giant moa (scale 0.886), elephant bird (1.0), Genyornis (0.715, `deepBeak` flag).
  - **Spec:** `scripts/ground_bird_spec.py`, 19 boxes on 128×128.
    - The legs are authored by world direction per segment: thigh forward-down, drumstick down-back, tarsus down-forward, flat foot. The hip height is solved so the feet stand on the ground.
    - **Walk:** FITTED by random search (`--walk-fit`, `scratch/ground_bird_walk.json`): 0.04 dig, 0.84 lift, 8.2u stride, 51% planted. The hand-set gait dug 0.96 and was planted 24%.
    - **SIT:** SOLVED as a ratite rests, hocks behind with the tarsi flat FORWARD under the belly. Two failed fits are documented in the spec.
    - **SLEEP:** the neck laid forward along the ground with the head level. The first try hung the head like an anteater's.
  - **Port:** `gen_model_ground_bird.py` (EXACT 19/19).
  - **EAT/DRINK/ALERT:** via the 2.8.0 pipeline (`pose_targets["ground_bird"]` → `pose_reach.py` → `gen_keyframes.py`). gen_keyframes does not add `getAnimationEat()`; it was added by hand.
  - **Java:** `EntityGroundBird` (new package `entity/bird`): a herd herbivore with flight, alert, graze, drink, migration, a kick that throws, and a threat display. Chicks are born live (an egg item would be a new asset).
  - **Data:** herbivores spawn table (weight 2); chicken and feather loot; crocodilian sounds, since birds are archosaurs and the bellow and hiss fit the boom and hiss of ratites (the subtitles say "Crocodilian").
  - **Skins and icons:** `scripts/paint_ground_bird.js` (woollib plumage, scaly tarsi); hand-drawn icons.
  - **NEW bird taxon:**
    - `taxon_bird.json` (the upstream `category_bird.png` icon); `ground_bird_category` reparented to it.
    - `gen_discovery_advancements.TAXA` gained "bird", so Naturalist now needs a bird (lang updated).
- 2026-11-19: **D4 great ape DONE** (NEW TYPE `great_ape`). Build passes, GameTests 22/22.
  - **Species:** Gigantopithecus (scale 0.865) and Bornean orangutan (0.432).
  - **Spec:** `scripts/great_ape_spec.py`, 25 boxes on 128×128.
    - **Body:** a knuckle-walker with the torso pitched 39°, solved from the arm and leg heights. Limbs are placed by world joint (`to_local`) and world direction. The head is the NECK's child (re-parented, because the eat/drink solver moves the neck subtree) and has forward-facing eye planes.
    - **Walk:** fitted per limb pair.
    - **SIT:** solved. SLEEP is the sit slumped (a belly-down solve buried the jaw 2.4u).
  - **Port:** `gen_model_great_ape.py` (EXACT 25/25).
  - **EAT / DRINK / ALERT:** pose_reach could not bring an ape's mouth to the ground (the head ended 9u inside the torso), so EAT is a hand-made hand-to-mouth, and the ape registers no DrinkGoal and has no DRINK. Only ALERT comes from gen_keyframes (`pose_targets` `eat=False, drink=False`).
  - **Data:** herbivores spawn table (weight 1); vanilla polar-bear sounds; leather loot.
  - **Skins:** `scripts/paint_great_ape.js`. Gigantopithecus is darker chestnut, 29.6 apart from the orangutan.
- 2026-11-19: **RELEASE 2.9.0 BUILT** (`build/libs/untamedwilds-1.18.2-2.9.0.jar`).
  - **Status:** 345 species, 55 types. GameTests 22/22, validate_species and validate_sounds OK.
  - **Docs:** version bumped in `gradle.properties` and `mods.toml`; CHANGELOG written.
  - **Installed** in CurseForge Test and GDLauncher Planet Wild. The old 2.8.0 jars were kept as `.bak`.
  - Nothing is committed (the last commit is still `20754df`).
  - **Not yet done:** runClient, together with 2.7.0 and 2.8.0.
- 2026-11-19: **COMMITTED** as `8def9aa` (2.7.0 + 2.8.0 + 2.9.0, 1,736 files; not pushed). `.gitignore` gained `run-*/`.
- 2026-11-19: **SOAK s4_290**: 60 min, 3 in-game days. Scenario `scripts/soak/scenarios/s4_290.txt` is S2 plus the 4 new types; the orca pod is in a built 25×25×8 pool with trevally and seals. Run with `debug_regrow_forced_chunks=true`.
  - **Clean:** no errors from any animal, and no species went extinct.
    - Orcas 3/3; they killed both seals and 3 trevally (hunting works).
    - Ground birds 5/5 (condition 0.79–0.94); great apes 2/2.
    - Mustelids: 1 of 2 starved (it started at Hunger 12; vanilla rabbits, chickens and pigs ARE small_game prey).
  - **Tick time:** mean 7.96 ms. The FIRST 15 min averaged 19.4 ms (p95 39, one 73 ms sample) against 7.4 ms in the s3grass soak; the rest averaged 4.5 ms against 6.3. The hump is unexplained: candidates are the pool fill and the orcas in a cramped pool. A `debug_perf_counters` run would attribute it.
  - **Pre-existing, not 2.9.0:** 8 "other" deaths (both tapirs, 2 boar, 1 each of big cat, caprine, dire wolf and equid), as in the S3 soak. The scenario summons at the console's y, so possibly suffocation. There were also herbivore starvation deaths on a patch that is ~70% non-grass ground.

    - Galápagos is domed, not saddleback.
    - The giant tortoises' hitbox is smaller than the model.
    - Panochthus uses the Doedicurus club, not a spiked tube.
