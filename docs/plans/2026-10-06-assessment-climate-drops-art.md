# Untamed Wilds Plus — assessment + plan: climate/weather → drops → skins & models → new models (2026-10-06)

## Context

The user asked what is still missing (ecosystem, zoo side, species, drops, skins/models) and for
recommended changes. Three read-only audits (2026-10-06) produced the assessment below. The user then
set priorities (rejection note on the first plan):
- **Defer the ecosystem gaps (1b), EXCEPT climate & weather** — tied to Minecraft's own weather
  system and to Serene Seasons when it is in the modpack.
- **Then focus on drops, then skins & models, then new models.**
- **Approval gate: for every new visible asset (item texture, skin, model, icon, block) ask for the
  user's approval BEFORE implementing it** (proposal first), on top of the existing Blockbench
  sign-off for finished skins/models.
Standing rules still apply: species theme Middle–Late Pleistocene + Holocene for new species; never
delete/reorder species (integer variant indices; use `scripts/append_species.py`); edge bleed on
every skin; tune via `ConfigGamerules`; Zoo mode (`config/EcologyMode.java`) keeps consequences off.

---

## Part 1 — Assessment (summary of the audits)

**Ecosystem — works:** diets + eco levels, hunt commitment, pack bursts, cat ambush, instant
scavenging + kleptoparasitism, grazing depletion/regrowth, starvation/condition/senescence,
dispersal, migration, capped herd defence, mixed-species alarm flight, territoriality, day/night
activity, vanilla animals tagged as prey (`util/EcologyTags.VANILLA_TAGS`).
**Ecosystem — gaps (DEFERRED by the user except climate/weather):** 9 hunters never get hungry
(shark, monitor, snake, anaconda, softshell turtle, tarantula, opossum, football fish, salamander —
only bear/big_cat/dire_wolf/hyena/spitter call `ComplexMob.drainHuntingHunger`) — **a bug, flagged
for a later decision**; no ongoing repopulation; no persistent carcasses; browsers graze grass;
no water/thirst; no predator-vs-predator suppression; aquatic web mostly inert; no world effects or
disease. **In scope:** seasons (Serene Seasons stub — `compat/CompatSereneSeasons.isCurrentSeason`
returns true; real code commented out against the 1.16 API) and weather (no effects today).

**Zoo side (not scheduled — awaiting the user's call):** no Patchouli entries/icons for the 11
ice-age types, opossum and ~38 newer species; 8 advancements, discovery hardcoded to the spitter; no
steerable mounts; no zoo blocks; Analyzer debug-only; **bug:** favourite foods broken for aardvark
(`untamedwilds:aardvark_cucumber` → `food_aardvark_cucumber`) and manatee/spadefish
(`minecraft:sea_grass` → `seagrass`).

**Drops:** bear/big_cat variant→hide mapping wrong or missing (bear v0 drops black+white, v8–9 none;
big_cat v9–16 none, marsupial lion→puma, sabertooth→snow leopard); panther hide never dropped;
snow-leopard hide missing from `forge:hide`/`forge:leather`; 11 ice-age types share one generic
beef/fat/bone/leather table; dire wolf bones only; big_cat no meat; shark rotten flesh only; textures
with no item (`material/horn`, `material/shark_tooth`, `food/meat_cetacean_raw`, 9 generic hides);
blocks with no loot table (`block_lard`, `block_pearl`, `nest_reptile`, `block_burrow`, 2 flora).

**Skins & models (ranked):** (1) **bug — juvenile skins unpainted**: cubs/calves/piglets read their
own region of the adult texture (e.g. `ModelBigCatCub` u 94–128); ≈0% painted for 15 species
(big_cat ×8, bear ×2, boar ×5), partial for bison Tier-3/Phase-6 calves (62–64%) and rhino mercks
(55%) → babies render see-through; (2) dire wolf walk/run is the hyena's cycle on the new legs;
(3) 7 skins never audited for the UV-resize bug (caribou, white_tailed, fallow, sika, eucladoceros
antlers; maned_wolf, bush_dog legs); (4) renamed species still wear the old animal (Schomburgk's
deer, Sicilian dwarf elephant); (5) ~60 flat upstream mammal skins beside the painted ones (most
visible: cave lion, S. populator, cave bear, woolly rhino, shortface hyena; aurochs/giant_buffalo
"too hairy"); (6) body misfits (pampathere on the domed shell, megalocnus on the giant sloth body,
thylacine on the canid rig); (7) slit-eye blink only on mammoth/sloth.

**Species missing (new models):** Diprotodon, kangaroos, Megalania, crocodilians, pinnipeds,
wildebeest (+Megalotragus), wild goats/sheep, capybara/Neochoerus/Castoroides
(`docs/plans/future_species.md`).

---

## Part 2 — Plan (in the user's order)

### Phase 1 — Climate & weather (Serene Seasons + vanilla weather)
1. **Serene Seasons adapter, no compile dependency.** Rewrite `compat/CompatSereneSeasons.java` to
   call `sereneseasons.api.season.SeasonHelper.getSeasonState(Level)` by reflection (method handles
   cached once; any failure → log once, behave as "no seasons"). Expose `season(level)`,
   `subSeason(level)`, `tropicalSeason(level)`. `CompatBridge.SereneSeasons` already detects the mod.
   - **Breeding stays all year, with or without Serene Seasons (user decision, 2026-10-06 — a
     seasonal gate could drop reproduction too low and crash populations).** `isCurrentSeason`
     keeps returning true; the species `breeding` field stays unused; `ComplexMob.java:1242` is not
     touched. Seasons drive only the forage / cold / herd effects below.
   - Save this as a feedback memory when implementation starts.
2. **Vanilla weather effects** (always available; Ecosystem mode only, each a `ConfigGamerules`
   toggle/strength):
   - **Rain** boosts forage regrowth (`world/ForageRegrowthHandler` attempt rate ×N while raining
     in that biome).
   - **Snow / cold** (raining in a cold biome, or Serene Seasons winter): extra hunger drain for
     species not cold-adapted (cold-adapted = `hasWoollyCoat` flag or spawnBiomes in icy/taiga),
     forage regrowth paused.
   - **Thunderstorms**: herd animals bunch up (herd radius shrinks) and an occasional thunder clap
     triggers the existing herd flight (`HerdFleeGoal`) — no new AI.
   - **Seasonal forage**: Serene Seasons winter halves regrowth; spring boosts it.
3. Census/spyglass readout shows the current season when Serene Seasons is present.
- Files: `compat/CompatSereneSeasons.java`, `compat/CompatBridge.java`, `world/ForageRegrowthHandler.java`,
  `entity/ComplexMob.java` (hunger tick hook), `config/ConfigGamerules.java`, `config/EcologyMode.java`.
- No visible assets → no approval gate needed beyond the plan.

### Phase 2 — Drops
1. **Loot bug fixes (data only):** bear and big_cat variant→hide conditions (every variant mapped,
   new species included — mapping table shown to the user first), panther hide drop, snow-leopard
   hide in `forge:hide`/`forge:leather`, whale pool rename, loot tables for `block_lard`,
   `block_pearl`, `nest_reptile`, `block_burrow`, the 2 flora.
2. **Per-type drops for the generic-template types** — proposal first (which material per type,
   counts, which recipe uses it), then build:
   - Reuse existing unregistered textures where possible: `material_horn` (rhino, bison, antelope),
     `material_shark_tooth` (sharks), `food_cetacean_raw/cooked` (baleen whale; lang already exists),
     the 9 generic hide textures (`hide_*` for types without a species hide).
   - **New visible items (each needs approval before painting):** ivory/tusk (mammoth), antler
     (deer), scute (glyptodont), woolly fur (woolly mammoth/rhino), big-cat meat (raw/cooked).
   - Recipes so the materials do something (decor/tools/armour/food) — listed in the proposal.
- Files: `data/untamedwilds/loot_tables/entities/*.json`, `init/ModItems.java`, `data/.../recipes`,
  `data/forge/tags/items`, `assets/.../models/item`, lang.

### Phase 3 — Skins & models
1. **Juvenile skin regions (bug fix).** `scripts/paint_juvenile.py`: read each juvenile model's face
   rects via `scripts/java2bb.py` (`ModelBigCatCub`, `ModelBearCub`, `ModelBoarPiglet`,
   `ModelBisonCalf`, `ModelRhinoCalf`); fill them by colour-transferring the species' own adult
   palette onto the upstream reference juvenile region (luminance → species ramp, the technique of
   `scripts/paint_glossotherium.js`), keep reference alpha, stamp reference cub eyes, then
   `MM.bleed`. Preview per type on the juvenile rig (`gen_bb_rig.py Model…Cub`) for sign-off.
   Patch painters so future skins also fill the juvenile rect.
2. **Dire wolf walk/run** fitted with the gait optimiser approach used for the mammoth.
3. **UV-resize audit** of the 7 unaudited skins (`reference_rig_resize_uv_bug.md`); repaint any
   misaligned one (approval gate).
4. **Repaint the renamed species** — Schomburgk's deer, Sicilian dwarf elephant (proposal → approval
   → Blockbench sign-off).
5. **Upstream flat-skin repaints**, most visible first (cave lion, S. populator, cave bear, woolly
   rhino, shortface hyena, then aurochs/giant_buffalo) — one at a time, proposal + sign-off each.
6. Slit-eye blink on the remaining remodelled models; body misfits (banded pampathere shell flag,
   island-sloth proportions, thylacine tail) as model proposals.

### Phase 4 — New models (Phase 10 species)
Order: Diprotodon → kangaroo body → Megalania → crocodilian body → pinnipeds → wildebeest / wild
goats (antelope horn families) → large-rodent body. Each: proposal (silhouette, size, species list,
ecology placement) → approval → full remodelling SOP → Blockbench sign-off per model and skin.

### Future phase (user-added 2026-10-09) — Sounds & ambience
Animal vocalisations and ambient soundscapes: per-species calls (the mod still shares a handful of upstream
sounds across many species), herd/pack ambience, weather- and season-aware ambience. Scope to be proposed
before any work (new sound assets need approval like visible assets). Slots in after the art phases.

### Deferred / awaiting a decision
- Ecosystem gaps 1b (repopulation, carcasses, browsing, water, intraguild predation, aquatic web,
  world effects, disease) — **including the 9-hunter hunger bug**, flagged because it is a bug, not a
  feature.
- Zoo side (encyclopedia/icons, advancements, mounts, zoo blocks, survival analyzer) — **including
  the 3 broken favourite-food ids** (one-line data fixes).

---

## Verification
- Every phase: `./gradlew build` → BUILD SUCCESSFUL; jar contents checked; then installed in the
  user's Test CurseForge instance (has Serene Seasons 7.0.0.13, Citadel, JEI, Patchouli).
- Phase 1: with Serene Seasons — breeding continues in every season (census over a full year shows
  births in winter); winter slows regrowth and adds cold hunger only to non-cold-adapted species;
  rain visibly speeds grass regrowth; a thunderstorm bunches herds. Without Serene Seasons — no
  errors, weather effects still work, seasonal effects simply absent.
- Phase 2: kill one of every bear/big_cat variant (and each re-tabled type) → correct drops; JEI
  shows new items + recipes; broken blocks drop themselves.
- Phase 3: juvenile coverage ≥95% of each juvenile rect opaque (script assertion) and babies
  summoned in game look right; dire wolf feet don't slide (simulation metrics like the mammoth);
  every repaint signed off in Blockbench.
- Phase 4: per-model SOP checks + Blockbench sign-off + in-game look.

---

## Decisions log

- 2026-10-06 — Phase 1 (climate & weather) implemented and building; breeding stays all year.
- 2026-10-06 — Drops decisions (user):
  - **Per-species hides + rugs for bears and big cats** (16 new hides, 16 rugs, generated from each
    species' skin; design sheet for approval first). Every hide also crafts 1 → 1 leather.
  - **Build all four drop groups:** horn (+ Herding Horn: tamed animals within 32 blocks Follow;
    sneak = Sit; 7 s cooldown), shark tooth (+ serrated arrows, short bleed), ivory (+ ivory block,
    slab, stairs, wall, chiseled), woolly fur (+ leather-tier fur armour, freeze-immune), antler
    (+ antler chandelier light, wall trophy), scute (+ carapace chestplate: iron armour + 0.1
    knockback resistance; carapace shield 1.5x durability), whale meat raw/cooked (beef values).
    Ivory/antler/horn/tooth also → bone meal.
  - **No big-cat meat** (vanilla predators drop no meat).
  - **Added:** mounted specimens (taxidermy kit → frozen poseable mount of any species reusing its
    model/skin; optional display plinth) and **hunting spears in vanilla tiers** (melee reach +
    throwable; mod materials only as cosmetic/anti-animal trims). Fossil skeletons: not now.
  - Every new visible asset gets a design preview for approval before painting.
- 2026-10-06 — Phase 2 progress: block loot tables, hide tags, whale pool fixed; 17 per-species
  hides + rugs installed (panda/sun/spectacled/cheetah/fatalis revised per the user); bear and big_cat
  loot now drop each species' own hide (panther from Skin:1 jaguars/leopards); build SUCCESSFUL.
- 2026-10-06 — Materials (8 items + loot + recipes) and gear built: fur + carapace armour sets (user
  added the full carapace set), serrated arrow, Herding Horn, ivory block set; build SUCCESSFUL. Next:
  antler chandelier + trophy (Blockbench), carapace shield, taxidermy, vanilla-tier spears.
- 2026-10-06 — Batch 3 approved ("good") and built: hunting spears ×6 tiers (sword −1 damage, 1.2
  speed, +1 attack range, throwable, sword enchants) + shark-tooth/antler trims (+2 vs the mod's
  animals); carapace shield (504 durability, scute repair); taxidermy kit → mounted specimen (a plain
  display entity that draws an un-ticked copy of the animal; stand/sit/sleep/animation poses; turn,
  name, break by players only); display plinth. Build SUCCESSFUL, jar in the Test instance; runClient
  pending. Left in drops: antler chandelier + wall trophy (need Blockbench for design approval). Then
  Phase 3 (skins & models), starting with the juvenile skin regions.
- 2026-10-06 — Antler chandelier + wall trophy approved in Blockbench (after two skin passes: real vanilla
  candles, a continuous bone gradient, and an anatomical deer skull instead of a face) and built. Chandelier
  has 4 arms (block-model rotation limit), recipe 4 antlers + 2 torches; trophy 2 antlers + planks. **Drops
  phase complete.** Next: Phase 3 skins & models, juvenile skin regions first.
- 2026-10-07 — Phase 3.1 juvenile skin regions: 24 skins filled by palette transfer from painted
  relatives, approved by the user from contact sheets, installed, build SUCCESSFUL, jar in Test. Next:
  3.2 dire wolf gait fit, 3.3 UV-resize audit of 7 skins.
- 2026-10-07 — Phase 3.3 UV-resize audit closed: no holes; 3 deer have antler segments showing base-skin
  bone instead of the repaint, visually identical on the fixed rig, so no repaint. Next: 3.2 dire wolf gait.
- 2026-10-07 — Fallow deer switched to palmate antlers (data flag; user-approved).
- 2026-10-07 — Phase 3.2 dire wolf gait fitted (walk = lateral sequence, run = rotary gallop, size-normalised
  cadence, run threshold 0.06 -> 0.1 b/t so wandering wolves walk). Build SUCCESSFUL, jar in Test; needs a
  runClient look. Next: 3.4 repaint the renamed species (Schomburgk's deer, Sicilian dwarf elephant).
- 2026-10-08 — User moved the deferred **zoo side** ahead of more models (zoo side started). Scope chosen:
  encyclopedia (all species), discovery advancements, Field Guide, mounts (equids, camelids, proboscideans,
  large bovids & deer), zoo blocks. Done: favourite-food ids, encyclopedia 272/272 with regenerated stats,
  133 icons (user-approved after a tapir/canid/camelid redraw), 3 discovery advancements. Build SUCCESSFUL.
  Next: proposals for the Field Guide, mounts and zoo blocks.
- 2026-10-08 — zoo side complete (all user-approved): Field Guide, mounts (equids jump, elephants 2
  seats, bovids/deer charge, camelids sand/snow), saddle overlays (redone in model space after the user
  rejected floating camel/mammoth saddles), Feeding Trough, Species Placard, Salt Lick. Build SUCCESSFUL, jar
  in Test, runClient pending. Next: Phase 3.4 renamed-species repaints, then Phase 3.5-3.6, then new models.
- 2026-10-08 — Phase 3.4 done: Schomburgk's deer (chestnut) and Sicilian dwarf elephant (light warm grey,
  from the straight-tusked) repainted + icons, user-approved. Next: 3.5 upstream flat-skin repaints (+ the
  steppe mammoth / straight-tusked colour collision), 3.6 blink + misfits.
- 2026-10-09 — User added a future phase: sounds & ambience. Phase 3.5 started (cave lion first).
- 2026-10-09 — Phase 3.5 done (user-approved): cave lion, S. populator, cave bear, woolly rhino (mammoth fur
  style + original mouth), short-faced hyena, giant buffalo lower half; aurochs already fine. Every baby skin
  re-checked: all live juvenile regions painted. Open: steppe mammoth vs straight-tusked colour collision
  (6.8). Next: Phase 3.6 (slit-eye blink on remodelled models; body misfits).
- 2026-10-09 — Phase 3.6 body misfits done (user-approved): tapir bust, pampatherium lowShell + 0.55,
  megalocnus 0.4, thylacine stiffTail + rear-only bands. Mammoth width analysed, no change (user). Slit-eye
  blink deferred (user). Build passes; runClient pending.
- 2026-10-09 — Phase 4.1 Diprotodon approved (user): new `diprotodont` type, 5 species (diprotodon,
  zygomaturus, nototherium, hulitherium, maokopia), LIFE SIZE (1 block = 1 m at the shoulder), extras: pouch
  joey, rideable (mounts system), marsupial_lion also spawns in savanna + forest. **Queued after the
  Diprotodon model: rescale the older (~0.85x) models up to the life-size standard (user).**
- 2026-10-09 — Phase 4.1 Diprotodon DONE (user-approved model, 5 skins, icons, saddle): new `diprotodont` type,
  encyclopedia category + 5 entries, discovery advancements, spawn (herbivores), loot (beef/fat/bone/leather),
  marsupial_lion range savanna+forest. Build passes, jar in Test, runClient pending. NEXT (user-queued): rescale
  the legacy ~0.85x models to the life-size standard.
- 2026-10-09 — Legacy life-size audit: models are mostly OVERSIZED (median 1.12x), not undersized. User: only grow
  the small ones. 12 species set to life size (data only); 71 oversized left as they are. Next: Phase 4.2 kangaroo.
- 2026-10-09 — Phase 4.2 Kangaroo approved (user): new `kangaroo` type, 5 species (red, eastern grey, Procoptodon
  goliah, Sthenurus stirlingi, Protemnodon anak), MODEL-ONLY hopping (normal ground movement), extras: forward pouch
  joey, slow pentapedal walk (forearms + tail), cosmetic boxing between males; kick attack. Not rideable.
- 2026-10-10 — Phase 4.2 Kangaroo DONE (model, 5 skins, icons; build passes, runClient pending). User order for the rest of
  Phase 4: 4.3 Megalania as a NEW `giant_monitor` type (Megalania + Komodo, own life-size rig, venomous apex
  ambush predator), then pinnipeds, wildebeest-type antelopes, goats/sheep, large rodents; CROCODILIANS LAST.
- 2026-10-10 — Phase 4.3 Giant monitor DONE (Megalania + Komodo; model, 2 skins, icons; build passes, runClient
  pending). Order revised by the user: pinnipeds next, then goats/sheep, rodents, wildebeest-type antelopes;
  crocodilians last.
- 2026-10-10 — Phase 4.4 Pinnipeds DONE (5 species + 2 pup coats, icons; build passes, runClient pending). Next: wild
  goats/sheep (proposal), then rodents, wildebeest-type antelopes, crocodilians last.
- 2026-10-10 — Phase 4.5 Caprines DONE (6 species, icons, saddle; build passes, runClient pending). Next: large
  rodents (proposal), then wildebeest-type antelopes, crocodilians last.
- 2026-10-10 — Phase 4.6 Large rodents DONE (capybara, Neochoerus, Castoroides, beaver; model, 4 skins, icons; bird
  passengers, beaver tail-slap alarm, beaver dams gated on vanilla mobGriefing; not rideable). Build passes.
- 2026-10-10 — **Release 2.7.0 built** (user: "wrap the build for a version"): version bumped, CHANGELOG 2.7.0 section,
  CLAUDE.md handoff refreshed, jar installed in Test (2.6.0 jar removed). runClient still pending for everything since
  2.6.0. Next: Phase 4.7 wildebeest-type antelopes (proposal), crocodilians last.
- 2026-10-23 — Phase 4.7 DONE (user-approved model, 4 skins, impala skin, 5 icons): new `wildebeest` type (blue + black
  wildebeest, Megalotragus, hartebeest; 67-box rig, 4 world-orientation horn families, solved sit/sleep/graze/kneeling clash),
  great migration (periodic treks, season-change treks with Serene Seasons, predators follow) + territorial bulls; impala added
  to the antelope rig as variant 9 (6 lyre boxes spliced in, the 56 shipped boxes verified unchanged). Folded into the
  uncommitted 2.7.0 build; jar in Test, runClient pending. Next: crocodilians (proposal), then sounds & ambience.
- 2026-11-14 — Roadmap set by the user, in this order:
  1. Phase 4.8 crocodilians:
     - New `crocodilian` type: Nile crocodile, saltwater crocodile, mugger, black caiman, Quinkana, Voay.
     - Extras: river ambush with a death roll, nests and hatchlings (the existing reptile nest block), basking.
  2. Older-skin pass: about 70 mammal skins still use the original art. Render sheets and the user picks, most visible mismatches first. Also the steppe mammoth / straight-tusked colour collision, and the possible deer tail burial in sit/sleep.
  3. Cheap new species on existing bodies (future_species.md Tier 3 list).
  4. Bigger new bodies: Stegodon, giant tortoises, wolverine, orca, ground birds, Gigantopithecus.
  5. Sounds and ambience.

  Parked: the ecosystem playtest, the 9 never-hungry hunters, the 2.7.0 commit.

- 2026-11-14 — Phase 4.8 crocodilians DONE (model, 6 skins, eyes and icons approved by the user; build passes, jar in Test, runClient pending). 45-box rig, EXACT 45/45 port, river ambush + death roll, basking, reptile-nest eggs. Fixed on the way: nest/egg hatchlings got double the growing time and weren't counted as census births. Phase 4 is complete; the older-skin pass is next.
- 2026-11-15 — Older-skin pass, review done: all 74 original-art mammal skins rendered on their rigs (`scratch/old_skins_<type>.png`). Most hold up next to the new art (they already carry hand-drawn fur noise and markings). User picked three repaints: (1) lion males' flat manes + the puma (nearly identical to lioness morph 2), (2) the common warthog (brown boar blotches; should be grey with a spine mane), (3) the steppe mammoth coat (grey, same tone as straight-tusked; warm to chestnut). Indian rhino left as is. Deer tail in sit/sleep checked numerically: not buried (only the 6% dock anchor in the croup, tip clear). `big_cat/great_lion_female.png` is an orphan (no species uses it).
- 2026-11-15 — Older-skin repaints DONE and installed (user-approved in Blockbench): lion_1_male + lion_2_male manes only (`scripts/paint_lion_mane.js`, every other texel verbatim), puma full repaint (`scripts/paint_puma.js`; it had been lion_2_female, dE 0.6) + kitten region via paint_juvenile (ref lion_1_female), common warthog full repaint (`scripts/paint_warthog.js`; light dust-grey, dark mane; 16.9 from the giant warthog) + piglet region (ref giant_warthog). Steppe mammoth coat folded into the proboscidean assessment. Mammoth spawn egg / type display names renamed to "Proboscidean" (lang only; registry IDs kept for save compatibility).
