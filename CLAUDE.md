# Untamed Wilds Plus — build guide

This repo is the mod **`untamedwildsplus`** (Minecraft **1.18.2 / Forge**, depends on **Citadel**).
The mod root is the **repository root**; `./gradlew` runs from here. This guide is the authoritative
reference for adding new megafauna (Holocene & Pleistocene) without introducing crashes. Textures are
produced separately via Claude Design — this guide covers everything *except* the final art, but names
every texture file each animal needs.

## ▶ REMODELLING SOP — read before any model work

Any time you remodel an animal, follow the standing 9-phase SOP at
`~/.claude/projects/C--Users-josht-OneDrive-Desktop-untamed-wilds-plus/memory/reference_remodel_sop.md`
(indexed first in `MEMORY.md`, so it loads every session). Summary of the phase order:

0. Read the model/entity/JSON **contract** first (animations, per-species flags, scales).
1. Write a `<animal>_spec.py` and validate it with a **forward-kinematics checker** — ground contact for
   every weight-bearing part, size in blocks, truthful volume-sampled silhouette — *before* Blockbench.
2. Massing: **few, big, NESTED** boxes; angle major masses 13–21°; overlap chain joints ~1 unit.
   If two visual passes fail to converge, **stop and ask** rather than guessing more coordinates.
3. Build from the spec, preview with a **flat grey** texture, screenshot 4 angles.
4. Programmatic **z-fighting sweep**; eyes need ≥0.3 units clearance from the skull face.
5. UV: shelf-pack the true `2*(w+d) × (h+d)` footprint, assert 0 overlaps, then a flat-colour checker.
6. Port from `export_model("modded_entity")` **verbatim** (never pass `path`; never re-derive the Y-flip).
7. **Solve** sit/sleep poses by grid search against real geometry — never hand-guess them.
8. Skins one at a time, approval before each; change the surface *treatment*, not just the palette;
   nothing written into `assets/` before the user says yes.
9. `./gradlew build` then `runClient` with output **redirected to a file, not piped through `tail`**.

## ▶ Start here (session handoff — read this first)

**Where things stand (2026-10-01, version 2.6.0):**
- Branch **`ice-age-megafauna`**, committed and pushed to `origin`
  (`github.com/joshteng03-creator/untamedwildsplus`). The release PR targets **`1.18.2`**.
- **Content:** 43 entity types, every one with a model, renderer, species JSON, loot table and spawn
  entries. The ice-age types (mammoth, deer, ground_sloth, glyptodont, dire_wolf, equid, giraffid,
  antelope, toxodon, macrauchenia) and the tapir are all **sculpted** in Blockbench and ported to
  Java, and every skin is painted and user-approved.
- **Ecology:** hunting, herds, demography, ecosystem mode (`/gamerule untamedwildsEcosystemMode`),
  predator balance and prey-side defences — see the Progress log and the Phase 1–8 sections below.
- **Roadmap:** `docs/plans/2026-09-30-status-audit-and-roadmap.md`. Phases 0, 1 and 3–8 are done.
  Future species start in `docs/plans/future_species.md`.

**The one big gap: almost nothing since 2026-07-21 has been seen in game.** Every change builds
(`./gradlew build` → BUILD SUCCESSFUL), but runClient is outstanding (roadmap Phase 2). Do that first:
1. `./gradlew runClient > scratch/runclient.log 2>&1` (redirect, never pipe through `tail`), then grep
   the log for `ERROR` / `Exception`.
2. Smoke test per type: spawn egg, renders with no pink/black, every species variant, sit/sleep poses,
   blink, per-species toggles (`longHorns`, `earSize`, `flatBack`, tusks, antler/horn families,
   glyptodont club), the mammoth's new walk / threaten / gore / graze animations.
3. Ecology: `/untamedwilds census` (and `census <species>`, `census auto <minutes>`) in an Ecosystem-mode
   world over a few in-game days — every predator type should persist and herds should oscillate, not
   hit 0. Check the AnalyzerItem eco level is unchanged right after a hunting blow and a bison charge.
4. Multi-mod run with JEI, Citadel and Patchouli, then merge the PR.

## ▶ Blockbench MCP — how models get made this session

A live **Blockbench ⇄ Claude Code** bridge is set up so geometry can be sculpted with AI help:
- **Connector:** `jasonjgardner/blockbench-mcp-plugin`. The plugin runs an MCP server **inside Blockbench**
  over HTTP at **`http://localhost:3000/bb-mcp`**. It is registered in Claude Code at **user scope**
  (in `~/.claude.json`), so it's available in every project — no per-project setup.
- **To use it in a session (order matters):**
  1. Launch **Blockbench (desktop)** and confirm the MCP plugin is enabled (it listens on port 3000).
  2. **Then** start Claude Code from this repo folder — MCP tools are loaded at session start, so
     Blockbench must already be up. (If you started Claude Code first, restart it.)
  3. Confirm with `claude mcp list` → `blockbench … ✔ Connected`, and ask the agent to "list your
     Blockbench tools" or "screenshot the current Blockbench view" to prove the tools are callable.
- **The workflow loop for THIS mod:** models here are hand-written Citadel `AdvancedEntityModel` **Java**
  classes — there are **no `.bbmodel` files**, and Blockbench cannot open the Java models directly. So:
  Blockbench is the **sculpting surface** (build/preview geometry, export the UV template for skinning),
  and the `Model<Name>.java` file is the **build target** (the agent translates the sculpted boxes into
  the `AdvancedModelBox` code). Keep `client/model/ModelBison.java` open as the format gold standard.

## ▶ Blockbench hard-won lessons (read before touching any model — these caused real bugs)

These were each discovered by shipping a visibly broken mammoth and having to root-cause it. Follow
them from the start on every future animal to avoid repeating the same debugging sessions.

1. **Never hand-copy coordinates off the Blockbench UI.** Blockbench's internal viewport is
   **Y-up**; Minecraft's `ModelPart`/`AdvancedModelBox` convention is **Y-down**. The
   `modded_entity` export (Java Class export) correctly performs this flip for you — raw
   `Group.origin`/`Cube.from`/`Cube.to` values do not match what ends up in-game and will render the
   model **upside-down**. Always call `export_model(codec_id: "modded_entity")`, read the emitted
   `PartPose.offset(...)`/`addBox(...)` values, and transcribe those into `AdvancedModelBox`
   verbatim (same numeric literals, same parent/child structure) — never re-derive them by
   subtracting raw Blockbench values yourself, even if you worked out a "flip formula" once; it does
   not generalize across scales or hierarchy depths, and re-deriving it a second time introduced a
   fresh inversion bug.
2. **A part's UV footprint is fixed by its own box dimensions — `texOffs` only changes the corner,
   not the size.** Minecraft's box-UV auto-layout always reserves a footprint of
   `2×(width+depth)` px wide by `(height+depth)` px tall starting at `texOffs`, no matter how small a
   region you intend to give it. Picking an arbitrary small "UV budget" for a part (instead of its
   real footprint) does **not** confine sampling to that budget — the renderer still lays out the
   full-size cross starting there, spilling into whatever's next to it (another part's paint, or
   blank canvas). This caused two real bugs: features rendering on the wrong body part, and a large
   flank face sampling into unpainted background and rendering solid black.
   - **The fix:** for every `AdvancedModelBox`, compute its real footprint —
     `w = 2 * (round(width) + round(depth))`, `h = round(height) + round(depth)` — and shelf-pack
     those exact sizes onto the texture canvas with zero overlap. Do not invent smaller sizes to
     save space.
   - Sum the footprints before picking a canvas size. A model scaled up from its original design
     (e.g. after a manual resize pass) can easily need 4×+ the pixel budget of a 128×64 canvas —
     check the total against `texWidth × texHeight` before assuming it fits, and bump to 256×256 (or
     whatever fits) rather than cramming.
3. **`texOffs` in the Java file and the live Blockbench project's per-cube `uv_offset` are one
   source of truth — keep them identical, always.** Any time geometry is rescaled/resculpted and the
   UV layout is recomputed, push the new values to **both places in the same pass** via
   `modify_cube(uv_offset: [...], mirror_uv: ...)` for each cube (get UUIDs from `list_outline`).
   Never edit the Java file's `texOffs` without also syncing Blockbench, or vice versa — they will
   silently diverge and the next painting session will paint the wrong regions.
4. **Verify UV alignment with a flat-color checker pass before investing in real painting.** After
   any geometry/UV change, fill each region with a distinct solid color via `risky_eval`
   (`ctx.fillStyle=...; ctx.fillRect(x,y,w,h)`) and screenshot the 3D view. If every part shows one
   clean, correctly-shaped block of color with no bleeding into neighboring parts, the UV is safe to
   paint for real. This is cheap and catches both of the bugs in point 2 immediately, instead of
   discovering them after a full paint pass.
5. **Blockbench's viewport never runs the mod's Java code.** Per-species toggles
   (`hasWoollyCoat()`-gated `showModel`/`setScale` calls in `setupAnim`, or any other entity-driven
   conditional) only execute inside a running Minecraft client, via `RendererX` calling
   `Model.setupAnim(entity, ...)` every frame. A Blockbench screenshot is a static preview of the
   raw box hierarchy with whatever texture is currently loaded — it has no entity, no species, and
   never calls `setupAnim`. **Every species will look identical in Blockbench regardless of the
   toggle**; this is expected, not a bug. The only way to see a toggle actually work is
   `./gradlew runClient` and spawning the different species in-game.
6. **Blockbench's directional preview lighting can make a correctly-painted face look near-black.**
   Faces angled away from the viewport's implied light source render much darker than their actual
   hex value, especially on large flank faces. Don't conclude a color is wrong from how dark a
   screenshot looks — check the actual painted hex, and lean toward brighter base tones for large
   surfaces so they still read clearly under this shading.
7. **`export_model` with a `path` argument hangs the whole Blockbench MCP connection** (triggers an
   unanswerable native filesystem-permission dialog, freezing every subsequent call including
   read-only ones). Never pass `path`. To get a texture out of Blockbench, use `risky_eval` to call
   `Texture.all[0].canvas.toDataURL()`, then base64-decode and write the PNG yourself via Bash —
   never rely on `create_texture`'s width/height parameters sticking either (they default to 16×16
   regardless of what's passed); if you need a specific resolution, recreate the canvas manually in
   `risky_eval` (new `<canvas>`, reassign `tex.canvas`/`tex.ctx`/`tex.width`/`tex.height`, set
   `Project.texture_width`/`texture_height`, call `tex.updateSource(...)`).
8. **Frame `capture_screenshot` wide enough to see the whole model.** A camera `position` too close
   to `target` just shows one giant clipped face, not the animal. For a large (mammoth-scale) model
   centered near the origin, a position offset of roughly `±90, 55, ±100` with `target: [0, 15, 0]`
   gives a usable full-body 3/4 view; adjust proportionally for other sizes.
9. **You CAN rebuild an existing `Model*.java` as a Blockbench rig to preview skins in 3D.** This was
   previously written off as too error-prone because of the Y-flip — it isn't, provided you
   **calibrate instead of guessing**. Point 1 still stands for the Blockbench→Java direction; this is
   the reverse direction, used for *skinning an already-shipped model*, not for authoring geometry.
   - **Calibrate first.** Create one group + one cube with known values, `export_model(codec_id:
     "modded_entity")`, and read the emitted Java. Derived transform (24-unit world):
     `bbOrigin = (-javaPivotX, 24 - javaPivotY, javaPivotZ)` using **absolute** pivots (accumulate
     `setRotationPoint` down the parent chain — both engines apply rotation after hierarchy offsets,
     so naive addition is correct); `bbTo.x = -(absX + addBoxX)`, `bbFrom.x = bbTo.x - width`;
     `bbTo.y = 24 - (absY + addBoxY)`, `bbFrom.y = bbTo.y - height`; `bbFrom.z = absZ + addBoxZ`,
     `bbTo.z = bbFrom.z + depth`; rotation `bbRot = [-rxDeg, -ryDeg, +rzDeg]`.
   - **Prove it every time:** after building, `export_model` again and diff against the original
     `.java`. For `ModelRhino` every `addBox`/`texOffs`/`PartPose`/`mirror` matched exactly. Do not
     paint against an unverified rig.
   - Build it in one `risky_eval` from a SPEC array of
     `[name, parent, origin, rotation, from, to, uv_offset, mirror]` → `new Group(...)` +
     `new Cube({... box_uv:true, uv_offset, mirror_uv})` → `c.applyTexture(tex, true)`. **Omit parts
     the species hides** (e.g. skip `horn_front`/`horn_back` when `stubHorn=1`) so the preview equals
     what the player actually sees. Point 5 still applies: Blockbench never runs `setupAnim`, so bake
     the toggle into the rig by hand.
10. **The viewport pins the texture image — you must replace the `Texture` object to reload a skin.**
    `updateSource` / `updateImageFromCanvas` / `updateMaterial` / `refresh` all fail to refresh what
    is rendered, which makes you chase phantom bugs. Define one helper and reuse it:
    `window.reloadSkin(path)` = read file → base64 → `new Texture().fromDataURL(...).add()` →
    reassign every `cube.faces[f].texture` to the new uuid → `.remove(false)` the old ones. Then force
    a fresh frame by calling `set_camera_angle` with a slightly changed position (it returns the
    image); `capture_screenshot` on its own can return a stale frame.
11. **Probe UV face orientation before painting — it is cheap and catches invisible mistakes.** Paint
    each face of a part a distinct flat colour / directional ramp and screenshot (an extension of
    point 4). Established for this codebase's box-UV layout:
    - Row-1 first quad `A` = **UP** face, second quad `B` = **DOWN**; `front` = −Z, `back` = +Z.
    - On **UP** faces, `j=0` is the **+Z (rear)** end and `j=max` is the front.
    - **`side1` and `side2` run in OPPOSITE directions along `i`.** A "rear-only" feature painted
      naively lands on opposite ends of the two flanks. Use a `rearness()` helper that flips for one
      side, or keep side features symmetric along `i`.
    - **Check whether the region you are painting is buried inside another box.** In `ModelRhino`,
      `head_face`'s rear ~3px of its UP face sit inside `head_neck` and never render — the first
      Elasmotherium frontal dome was painted there and was completely invisible.
12. **Alpha-0 pixels delete a part, with no Java change.** `MobRenderer` draws with
    `RenderType.entityCutoutNoCull`, which discards fully transparent fragments. Clearing exactly a
    part's six face rects makes it vanish in-game — this is how the bison forelock (`head_hair`) is
    removed on the aurochs. Only clear that part's own face rects; the surrounding dead zone often
    holds *another* part's faces (see the packing notes in "Skin painting"), and those must stay
    opaque or you will punch a hole in the mob.

## Skin painting — making a variant look like its real animal, not a recolour

Learned while redoing `bison:aurochs`, `bison:giant_buffalo` and `rhino:elasmotherium`, which had all
shipped as hue-shifted copies of their siblings and read as "the same animal in a different colour".

- **Recolouring is not enough.** A luminance-preserving hue shift keeps the *donor's* texture
  signature — the bison's dense wool dithering, the woolly rhino's shaggy ginger nap — which is
  exactly what makes the variant read as a reskin. Replace the **surface treatment**, not just the
  palette.
- **Design each variant against its sibling, deliberately opposite.** Pick 4–6 diagnostic cues and
  invert them where the real animals differ. Shipped examples:
  aurochs = glossy short cattle coat, smooth gradients, **pale** mealy muzzle ring, cream horns;
  giant buffalo = matte slate hide, sparse hair with worn bare patches, wrinkle folds, **black**
  muzzle, heavy dried ochre mud, keratin boss;
  elasmotherium = cool ash-grey coarse **grizzled/agouti** pelage (vs the woolly rhino's warm ginger
  shag), a large pale domed frontal boss, stiff dark neck mane, light dry steppe dust on long legs.
- **When a species flag hides its signature feature, the skin has to carry the identity.**
  `elasmotherium` has `stubHorn:1`, so the horn is gone — the painted frontal dome is what makes it
  recognisable.
- **Keep procedural noise LOW-contrast.** The first Elasmotherium pass used ±0.26 luminance speckle
  and read as TV static / camo, not fur. Roughly ±0.13 with mild clumping reads as coarse hair.
- **Fine detail must respect how few pixels a face gets.** A 1px highlight on a 2px-wide eye covers
  half the eye and reads as a giant block — use a dim warm catchlight, not white. Likewise, at ~6px
  across, concentric rings alias into random mottling; transverse bands stay legible.
- **Do not paint an eye socket or ring onto a cheek face.** The eye is its own zero-width plane cube;
  anything painted around it on `head_main`/`head_face` renders as a stray blotch *beside* the eye.
- **Markings must be blended, dithered and broken, never hard lines.** A hard pale dorsal stripe on
  the aurochs was rejected in-game as "an ugly white line"; the accepted version is a gaussian blend
  toward a dun tone with hash-dithered edges that fade out at both ends of each face. Same for the
  buffalo's mud tide line — offset it per column so it does not read as a straight horizontal band.
- **Iterate in the 3D rig (point 9), not on the flat PNG.** Flat previews hid every one of the issues
  above. Get per-animal approval before moving to the next skin.

## Progress log

**Branch `ice-age-megafauna` (committed and pushed to `origin`, in sync).** First PR slice done:

- **Part 1 — data-only variants (done):** `bear:short_faced`, `big_cat:american_lion`,
  `big_cat:homotherium`, `bison:{steppe,long_horned,aurochs,giant_buffalo}`, `rhino:elasmotherium`,
  `hyena:cave_hyena`, `camel:western`, `boar:giant_warthog`. Each has a species object, lang name +
  sciname, and a **placeholder** skin (a copy of an existing same-type texture — real art still needed).
- **Sabre-fang flag (done):** `hasSabreFangs` on `big_cat`, set on `sabertooth` (=Smilodon) and
  `homotherium`. `EntityBigCat` mirrors the `dimorphism`/`fluffyTail` synced-flag pattern; `ModelBigCat`
  scales the existing upper canines (`teeth_right`/`teeth_left`) — no new texture region required.
  `ModelBigCatCub` has no tooth cubes, so cubs show no fangs (intended).
- **`dire_wolf` new type (done):** `EntityDireWolf`/`ModelDireWolf`/`RendererDireWolf` forked from the
  hyena trio; all three `ModEntity` hooks; `entities/dire_wolf.json` (`dire_wolf` + `pleistocene_wolf`);
  `dire_wolf_spawn_egg.json`; predators spawn-table entry; bones loot table; lang; placeholder skins.
  Uses vanilla wolf sounds (a non-null `threat` sound is required).
- **`mammoth` new type / proboscideans (done):** `EntityMammoth` (herd herbivore modeled on bison, minus
  the bison-only coupled goals `BisonTerritorialityFight`/`RodeoGoal`; keeps the generic
  `MeleeAttackCharger`; adds a `hasWoollyCoat` flag). `ModelMammoth` forks `ModelBison` and adds a
  3-segment trunk + forward tusks, hides the inherited bison horns, and shows the shaggy fur parts only
  when `hasWoollyCoat`. `RendererMammoth` uses a single model. 8 species: woolly / columbian /
  steppe_mammoth / mastodon / straight_tusked / cuvieronius / african_elephant / asian_elephant
  (`hasWoollyCoat=1` on woolly + steppe_mammoth). Reuses vanilla ravager sounds; loot mirrors the bison
  table with larger yields. **Geometry is placeholder** (bison body + trunk/tusks) pending a Blockbench
  pass, same status as dire_wolf.
- **8 more herd-herbivore types (done):** deer, equid, giraffid, antelope, toxodon, macrauchenia,
  glyptodont, ground_sloth. Each is a lean `ComplexMobTerrestrial` herbivore (declaring
  `ATTACK_THREATEN`+`ATTACK_GORE`) with a `ModelBison` fork that hides the inherited bison horns and
  adds feature cubes: deer antlers, giraffid ossicones, antelope horns + toggled bulbous nose
  (`hasBulbousNose`, saiga), macrauchenia proboscis, glyptodont carapace + toggled tail club
  (`hasTailClub`, Doedicurus); equid/toxodon/ground_sloth are horn-hidden bison bodies. 20 species
  total, vanilla sounds, bison-style loot, all three ModEntity hooks, herbivores spawn-table entries,
  placeholder 128x64 skins. Geometry is placeholder pending Blockbench, same status as mammoth.
- **Part 3 — predator balance (done):** `EntityMonitor` now uses the hunger-gated `HuntMobTarget`
  constructor (threshold 30); `ComplexMobTerrestrial.satiateFromKill(+120)` is called from
  bear/big_cat/hyena/dire_wolf `doHurtTarget`.
- **Skin realism pass — `bison:aurochs`, `bison:giant_buffalo`, `rhino:elasmotherium` (done,
  approved):** all three had shipped as hue-shifted copies of a sibling and read as reskins. Repainted
  from real-animal cues against verified Blockbench preview rigs for `ModelBison` and `ModelRhino`
  (see Blockbench lessons 9–12 and "Skin painting"). The aurochs additionally has the inherited bison
  forelock (`head_hair`) **deleted via alpha-0 UV rects** — no Java change — and a soft dithered dun
  eel-stripe. Only data change: **`longHorns:1` added to `giant_buffalo`** in `bison.json` (correct for
  *Syncerus antiquus*; it now shares that flag with `long_horned`). `elasmotherium` keeps
  `stubHorn:1`, so its painted frontal dome carries the identity instead of a horn.
  **Not yet verified in-game:** the new `longHorns` toggle needs a `runClient` look, since per-species
  flags never execute in Blockbench (lesson 5).
- **Left untouched on purpose (already shipped):** `big_cat:cave_lion`, `big_cat:sabertooth`,
  `rhino:wooly`, `bear:cave`, `hyena:shortface`, `manatee:steller`.

- **`deer` remodel + all 5 skins (done, 2026-07-28):** full 9-phase SOP pass. 59 boxes on 256×128
  replacing the 28-box bison fork; `scripts/deer_spec.py` holds the spec, FK checker, z-fight sweep,
  UV pack and the phase-7 pose solver, and `scripts/gen_model_deer.py` transcribes the Blockbench
  export into `ModelDeer.java` (verified by diffing the emitted Java back against the export).
  Two antler families on a shared pedicle toggled by `getVariant()` — PALMATE (megaloceros /
  stag_moose / moose), BRANCHED (wapiti / red_deer) — each sized by one `setScale` +
  `setShouldScaleChildren(true)`; families and `throat_bell` hidden with `showModel`. All five skins
  painted from real-animal cues and user-approved. `./gradlew build` **SUCCESSFUL**; **runClient not
  yet run** — that is the only outstanding item.

- **Ecology pass — wolf scale, megaherbivore predation, hunt mortality, grazing, mate-finding (done
  2026-07-29, `./gradlew build` SUCCESSFUL, runClient pending):** `dire_wolf.json` scales corrected to
  the remodel spec (1.1→**0.90**, 1.0→**0.82**; `scripts/dire_wolf_spec.py` calibrates 16u = 1 block
  withers, so `scale` is literally the species' shoulder height in metres). Predation rules 1–4 above,
  plus three new `ConfigGamerules`. **`GrazeGoal` rewritten** — its own `// TODO: Fix this shit so mobs
  actually walk to a grazeable pos` was real: `locateGrazeables` tested `testpos` (the spot in front of
  the animal) instead of the candidate it had just generated, so it only succeeded when the animal did
  not need it; its `getWalkTargetValue < 0` clause demanded a *penalised* block and was never true; and
  `canUse` issued a `moveTo` then returned false, so the goal never held MOVE and `SmartWanderGoal`
  re-pathed the animal immediately. Herbivores on sand starved where they stood. Now SEEKING/EATING
  phases, a ±16 search, and `isGrazeable(BlockPos)` is the overridable form (`EntitySpitter`'s anonymous
  subclass was moved to it, or spitters would have walked toward grass they do not eat).

- **14 new reskin-only species (done 2026-07-30, `./gradlew build` SUCCESSFUL, runClient pending):**
  variants added to the four already-modelled rigs, so each is pure Mechanism A (species object + skin +
  lang, no Java). `bear:arctotherium`; `mammoth:notiomastodon`/`paleoloxodon_namadicus`/`pygmy_mammoth`/
  `cretan_dwarf`; `dire_wolf:dhole`/`african_wild_dog`/`protocyon`/`thylacine`; `big_cat:cheetah`/
  `american_cheetah`/`barbary_lion` (dimorphic, 2 PNGs)/`caspian_tiger`/`thylacosmilus`. 15 PNGs, each
  user-approved before being written to `assets/`.
  - Painted against Blockbench rigs rebuilt from the Java per lesson 9 and **verified exact by
    re-export diff**: bear 23/23, mammoth 47/47, dire_wolf 39/39, big_cat 35/35 parts.
  - **Conventions derived from the shipped PNGs, not invented** — worth re-reading before any skin work:
    `ModelBigCat` renders all four fang cubes at full size regardless of `hasSabreFangs`, and every
    non-sabre skin **alpha-0s** them (only `sabertooth` paints the two uppers); `dimorphism` is
    texture-only, so a lioness's mane is deleted by clearing `neck_mane`'s six rects (male 632/632
    opaque, female 0/632); eye art and `head_snout_teeth` are standardised per type and are stamped
    verbatim. Mirrored L/R parts frequently share one UV rect.
  - **Two dwarf mammoths have an intentional hitbox mismatch.** `getMobSize()` scales only the rendered
    model; `EntityMammoth` has no `getDimensions`/`getScale` override, so every mammoth species keeps the
    fixed 2.6x2.6 hitbox from `ModEntity.createEntity`. At scale 0.55/0.45 `pygmy_mammoth` and
    `cretan_dwarf` are much smaller than their hitbox. Fixing it means overriding `getScale()` on the
    entity, which changes collision and combat reach for **all** proboscideans - deliberately not done.

- **`rhino:mercks` + `camel:titanotylopus` repaint (done 2026-07-30, build SUCCESSFUL):**
  `rhino.json` variant 7 `mercks` (*Stephanorhinus kirchbergensis*, scale 1.3, forest/jungle, no
  `stubHorn` so both horns show). Every dark slot in the rhino set was taken, so it is a **pale warm dun**
  hide (L 0.40 at real saturation — a combination no sibling occupies) carrying its identity on treatment:
  sparse coarse bristle, deep folds, and heavy dark forest-floor leaf mud booting the legs.
  `titanotylopus.png` **repainted** — it had shipped as a luminance-preserving recolour of `western.png`
  (both exactly 4622 opaque px). Now a two-tone animal: deep rufous body, dark chocolate shaggy wool,
  cream cannons (H34/L0.52 → H24/L0.37, 5455 opaque px).
  - **`ModelCamel`'s UV forces all wool overlays to ONE tone.** `neck_hair_1` `[89,56,101,64]` overlaps
    `arm_left_hair` `[89,55,96,64]`, and all four limb skirts share a single rect set. Two attempts at a
    dark neck cape over body-coloured limb wool failed because the cape simply overwrote the skirts —
    only the skirts' `up` faces (non-overlapping rects) kept their colour, which is what exposed it.
    Before designing any per-part colour split, check the UV rects for sharing.
  - The camel eye convention is the **inverse** of the rhino's: the camel paints the INWARD quad and
    clears the outward one, and the two eyes share both rects. Both work because
    `entityCutoutNoCull` does not backface-cull, so copy the reference verbatim rather than reasoning
    about which half faces out.

- **`toxodon` remodel + `mixotoxodon` + both skins (done 2026-07-31, `./gradlew build` SUCCESSFUL,
  runClient pending):** full 9-phase SOP pass. **34 boxes on 256x128** replacing the 19-box / 128x128
  bison fork, whose own comment admitted its sit/sleep fold angles were inherited and never re-derived.
  `scripts/toxodon_spec.py` holds the spec, FK checker, z-fight sweep, UV pack and pose solver;
  `scripts/gen_model_toxodon.py` transcribes the Blockbench export into `ModelToxodon.java` (verified
  by a re-export diff: 34/34 parts, every `texOffs`/`addBox`/`PartPose`/parent link identical).
  Both species share ONE mesh and differ only by JSON `scale`, so there is deliberately **no
  `getVariant()` switch** in the model.
  - **Scale recalibrated to life size** (1 block = 1 m at the shoulder): `toxodon` **1.5 -> 0.87**
    (2.34 -> 1.50 blocks tall) and `mixotoxodon` 1.014. The old rig overflowed the registered
    1.7x1.5 hitbox by 56%; the new one matches it exactly and correctly reads *shorter* than a bison.
  - Four defects the numbers caught that renders hid: the ear roll sign was inverted (`s*-48` rolled
    both ear tips down INTO the skull); the tail was 64% buried inside `body_croup` and invisible from
    behind; five z-fight patches, the worst 36u2 where the chest's -6 tilt lifted its top plane into
    the barrel's; and head+neck at 42% of body length (real: 22%) rendering as one slab from rump to
    nose. **`ModelDeer`'s tail has the same latent burial bug at -62/-78.**

- **Edge bleed is now MANDATORY for every skin (`MM.bleed` in `paintlib.js`).** MC computes a box's UV
  footprint `2*(w+d) x (h+d)` from the RAW FLOAT sizes, so any non-integer box - which is all of them
  on the sculpted rigs - samples a fractional texel region. The painter can only fill whole texels, so
  the outermost fraction of every face edge lands on an unpainted, fully transparent texel, and
  `entityCutoutNoCull` DISCARDS those fragments: visible holes along the seams, worst on small parts
  (`incisor_lower` had 87 transparent texels in a 150-texel edge halo, which is what made the toxodon's
  mouth look punctured). `MM.bleed(canvas, passes, protect)` dilates painted colour outward; pass
  `MM.faceRects(['eye_left','eye_right'])` as `protect` so the eyes' deliberately alpha-0'd inward quad
  is not refilled. **This is the same root cause as the deferred "mammoth leg/toe invisible texture
  spots" item.**

- **`antelope` extended to 9 species + 6 skins (done 2026-07-31, build SUCCESSFUL, runClient pending):**
  `ModelAntelope` went 36 -> **56 boxes**, EXTENDED not rebuilt. The original 36 are byte-identical
  (`scripts/gen_model_antelope.py` re-verifies texOffs/addBox/rotationPoint/rotation/parent against the
  file on disk and **refuses to write** if any moved), so `saiga`/`pronghorn`/`springbok` were untouched
  — confirmed by loading the shipped `springbok.png` onto the extended rig and rendering it correctly.
  New boxes are packed into UV rows **53–65**; the shipped footprints stop at v=52, so nothing moved.
  - **Five horn families on one skull**, selected by `showModel` on the chain ROOT (ModelPart.render
    returns before recursing into children when `visible` is false): GAZELLE (springbok/pronghorn),
    FORKED (tetrameryx, +rear prong), SPIRAL (giant_eland/blackbuck), RAPIER (gemsbok),
    SCIMITAR (sable/bluebuck). New species: tetrameryx, blackbuck, giant_eland, gemsbok, sable, bluebuck.
  - **Segment-to-segment TWIST is load-bearing.** A box's X-face normal is untouched by `rx`, so a chain
    that only varies its backsweep leaves consecutive side planes near-parallel and the sweep found
    1.0–1.4u2 at every joint. The shipped gazelle horn does not flag because base and tip differ by 20
    degrees of rz. Every new joint now turns >8 degrees of yaw or roll — which doubles as the spiral cue.
  - **Heavy build scales the torso AND moves the limb roots.** `AdvancedModelBox` does not scale a
    child's rotation point, so a wider chest alone swallows the legs (chest 4.15 -> 4.65 against a
    shoulder whose outer face is at 5.00). Limb roots go to 1.16x, which keeps the shoulder 0.37 clear
    of the barrel and the thigh 0.78.
  - `EntityAntelope.java` unchanged: `hasBulbousNose` is the only flag, everything else reads
    `getVariant()`. giant_eland (1.60 m) and sable (1.35) exceed the fixed 1.0x1.2 hitbox — accepted and
    documented, exactly as the dwarf mammoths ship.

- **`scripts/java2bb.py` — parse ONLY the constructor.** It rebuilds a shipped `Model*.java` as a
  Blockbench rig. A file-wide regex keeps the LAST `setRotationPoint` per part, and `setupAnim` re-issues
  one on the eye planes for the blink — so the eyes come back at their hidden-inside-the-skull position.
  **A round-trip diff will NOT catch this**, because both sides of the comparison come from the same bad
  parse. It reported "EXACT MATCH" while `eye_left` sat at x 1.0 instead of 2.6.

- **Release 2.4.0 jar built** (`build/libs/untamedwilds-1.18.2-2.4.0.jar`). Version bumped in BOTH
  `gradle.properties` (drives the jar name) and `mods.toml` (what Forge reports in-game) — they are not
  linked. Modpack-safe: `forge [38,)` and `citadel [1.10,)` have no upper pins.

- **`macrauchenia` remodel + `xenorhinotherium`, and `tapir` as a NEW TYPE with 7 species
  (done 2026-08-01, `./gradlew build` SUCCESSFUL + jar verified, runClient pending):**
  macrauchenia was the LAST raw `ModelBison` fork -- 26 boxes / 128x64 still declaring bison
  horns, beard, forelock and limb wool -- replaced by a **35-box 256x128** sculpt
  (`scripts/macrauchenia_spec.py`, `gen_model_macrauchenia.py`). Hitbox raised 1.3x1.9 ->
  **1.4x2.4**; scales recalibrated to 1 block = 1 m at the withers (1.15 -> **1.018**, new
  `xenorhinotherium` **0.876**). `tapir` is a full Mechanism B type built from scratch:
  **34 boxes**, `EntityTapir`/`RendererTapir`/all three `ModEntity` hooks/`tapir.json`/loot/
  spawn entry/egg model/16 lang keys, hitbox 1.3x1.2. Seven species off one mesh with two
  `showModel` gates on `getVariant()`: `crest` on lowland/bairds/mountain/megatapirus/
  vero_tapir, and `claw_left/right` on **palorchestes alone** (a diprotodontid marsupial
  grouped by convergence, as thylacine is under dire_wolf -- its clawed forelimbs are
  MODELLED, not left to the skin). All 9 skins user-approved one at a time.
  - **Blockbench MCP tools missing from a session no longer requires a restart.**
    `scripts/bb.py` drives the plugin directly over HTTP JSON-RPC at localhost:3000/bb-mcp.
    `claude mcp list` reporting "Connected" is NOT evidence the tools are loaded -- it runs a
    fresh subprocess check. Note `risky_eval` REFUSES any payload containing `console.`,
    `//` or `/* */`, and paths must use forward slashes.
  - **`gen_bb_build.py` WIPES the outliner**, so a user's hand edit in Blockbench is
    destroyed by the next rebuild. It now snapshots to `scratch/rig_prewipe.json` first, and
    `scripts/read_back.py <spec_module>` diffs the live rig against the spec. **Run read_back
    before rebuilding if the user has touched Blockbench.** This was learned by destroying a
    hand-painted eye and having to recover it by diffing the live TEXTURE against MM.canvas.
  - **When a head will not read as a separate object, measure how far it projects past the
    box behind it BEFORE touching the head.** Two tapir passes failed by retuning the head;
    the cause was a 13u `body_chest` reaching to z=-14, leaving the muzzle only 9u of
    projection on a 47u animal. Cutting the chest to 7u fixed it at once.
  - **A treatment change does not move a skin's MEAN colour.** megatapirus measured 16.8
    from lowland (inside the 10-20 "hue shift" band) despite a full agouti-grizzle treatment
    no other species uses. Two species sharing a treatment also cannot share a lightness
    (palorchestes vs mountain, 16.6 apart at L 0.275 vs 0.282). Measure each new skin against
    ALL shipped siblings, not just the last one.
  - MC face-lighting compensation must **MULTIPLY**, never mix toward a fixed body colour:
    mixing drags every part's hue toward it and turned macrauchenia's grey muzzle brown.

**Not yet done / next up:**
- Replace all placeholder skins with real art (Track B). The repurposed variant is still displayed as
  "Sabertooth" (sciname already *Smilodon populator*) — rename to "Smilodon" only if desired.
- Audit the remaining variant skins for the "reskin" problem fixed on aurochs / giant_buffalo /
  elasmotherium — any variant whose art was produced by recolouring a sibling is a candidate
  (see "Skin painting" for the method). The `glyptodont` migration was paused mid-way for this pass
  and should be resumed.
- All Part-2 new types are now implemented (dire_wolf, mammoth, deer, equid, giraffid, antelope,
  toxodon, macrauchenia, glyptodont, ground_sloth). Remaining: replace placeholder skins with real
  art and resculpt the placeholder geometry in Blockbench.
- Resculpt the placeholder geometry for `dire_wolf` and `mammoth` in Blockbench (currently the hyena and
  bison bodies with additions).

**Verification status / environment caveats:**
- Verified **statically only** (JSON validity, contiguous `variant` indices, texture-filename resolution,
  lang-key coverage, symbol existence, brace balance). A real `gradle build`/`runClient` was **not** run:
  this environment's egress policy returns 403 for `maven.minecraftforge.net` and `repo.spongepowered.org`,
  so the ForgeGradle toolchain can't be resolved here. Run the compile + in-game smoke tests where those
  Maven repos are reachable.
- The branch **is now pushed** to `origin` (`github.com/joshteng03-creator/untamedwildsplus`) and local is
  in sync (0 ahead / 0 behind). Next: run the compile + in-game smoke tests, then open a PR against `1.18.2`.

## Two mechanisms for adding animals

### Mechanism A — new *variant* of an existing type (data-only, near-zero risk)
Append one object to the `species[]` array in `src/main/resources/data/untamedwilds/entities/<type>.json`.
No Java. Per variant you touch:
1. `data/untamedwilds/entities/<type>.json` — one species object.
2. `assets/untamedwilds/textures/entity/<type>/<species>.png` — texture (see skin-encoding below).
3. `assets/untamedwilds/lang/en_us.json` — `entity.untamedwilds.<type>_<species>` **and** `.sciname`.
4. *(optional)* a Patchouli entry.

Species schema (see `util/EntityDataHolder.java`, `util/SpeciesDataHolder.java`; template
`data/untamedwilds/entities/rhino.json`). Fields may live at the **type top level** *or* be overridden
**per-species** (e.g. `rhino.json` sets `activityType`/`favourite_food`/`sounds` at top level;
`big_cat.json` overrides them per species):

```jsonc
{
  "name":"steppe", "variant":<next unused int>, "scale":1.4, "rarity":3,
  "attack":8, "health":70,            // optional; fall back to registerAttributes() defaults
  "activityType":"cathemeral",        // optional
  "favourite_food":"minecraft:hay_block",  // optional
  "flags": { "groupCount":6 },        // optional; per-variant model/behaviour toggles
  "spawnBiomes": [ ["category|taiga"], ["category|icy"] ]
}
```

At spawn, `entity/ISpecies.java::setSpeciesByBiome` collects every species whose `spawnBiomes` matches the
biome and weights each by `rarity` (**`rarity:0` = egg-only, never spawns naturally**). Biome tokens:
`category|<biomeCategory>` (`taiga`, `icy`, `extreme_hills`, `savanna`, `forest`, `jungle`, `plains`,
`swamp`, `desert`, `mesa`) or `resource|minecraft:<biome_id>`.

**Flag pattern** (verified in `EntityBear`/`EntityBigCat`): declare an int flag in the JSON `flags` map,
read it in the entity's `updateAttributes()` via
`getEntityData(this.getType()).getFlags(this.getVariant(), "name") == 1`, mirror it to a synced
`EntityDataAccessor<Boolean>` (define in ctor, getter/setter, persist in `add/readAdditionalSaveData`).
The model reads the getter in `setupAnim` to show/hide/scale cubes. Reusing an existing cube and scaling it
(as the sabre-fangs flag does with the big-cat canines) avoids needing new UV regions in the texture.

### Mechanism B — new *entity type* (new body plan; Java + model + renderer)
Required pieces per type (all verified against the codebase):
1. `entity/mammal/Entity<Name>.java` — copy-adapt a template class (table below).
2. `client/model/Model<Name>.java` — hand-written Citadel `AdvancedEntityModel<Entity<Name>>` of
   `AdvancedModelBox` cubes. A separate cub model is **optional** — `RendererHyena` reuses one model for
   adult and baby, so a fork of it can too.
3. `client/render/Renderer<Name>.java` — copy `RendererHyena.java` / `RendererBison.java`, swap the model.
4. `init/ModEntity.java` — **three mandatory hooks**:
   - a `createEntity(Entity<Name>::new, "<type>", sizeX, sizeY, mainColor, backColor)` field (this also
     auto-registers the `<type>_spawn_egg` item),
   - `event.put(<TYPE>.get(), Entity<Name>.registerAttributes().build())` in `bakeAttributes`,
   - `event.registerEntityRenderer(ModEntity.<TYPE>.get(), Renderer<Name>::new)` in `onRegisterRenderer`.
   Missing attributes → crash on spawn; missing renderer → crash on render.
5. `data/untamedwilds/entities/<type>.json` — type + species. `"name"` must equal the registry id.
6. `assets/untamedwilds/models/item/<type>_spawn_egg.json` — one line:
   `{ "parent": "untamedwilds:item/template_spawn_egg_mammal" }`.
7. `data/untamedwilds/spawn_tables/herbivores.json` **or** `predators.json` — add
   `{ "type":"untamedwilds:<type>", "weight":N, "size_min":a, "size_max":b }` or it never spawns wild.
8. `data/untamedwilds/loot_tables/entities/<type>.json` — copy an analogous table (a missing table only
   logs a warning; hyena ships without one).
9. `assets/untamedwilds/lang/en_us.json` — base `entity.untamedwilds.<type>`, each variant + `.sciname`,
   and `item.untamedwilds.<type>_spawn_egg`.
10. `assets/untamedwilds/textures/entity/<type>/<species>.png` placeholders.

**Sounds:** entity classes call `getThreatSound()`/`getAmbientSound()` etc.; if the type's aiStep plays a
threat sound, the JSON **must** define a `"threat"` sound (a null sound NPEs in `playSound`). Reuse vanilla
events (e.g. `minecraft:entity.wolf.growl`) or the mod's own registered events.

### Reuse templates (copy-adapt, do not write from scratch)

| New type | Template class | Why it fits |
|---|---|---|
| `dire_wolf` | `EntityHyena` | pack predator (`IPackEntity`, `INeedsPostUpdate`, `HuntPackMobTarget`) — **done** |
| `mammoth` | `EntityBison` | herd herbivore, large, defends young |
| `ground_sloth` | `EntityBear` | large, slow, rears (`IDLE_STAND`), defensive |
| `deer` | `EntityBison` | herd; antlers via variant model parts |
| `glyptodont` | `EntityTortoise`/`EntityRhino` | armored, slow, defensive |
| `equid` | `EntityBison` | herd, skittish grazer (register `untamedwilds:equid`, never `horse`) |
| `giraffid` | `EntityCamel` | tall browser, long neck + ossicones |
| `antelope` | `EntityBison` | fast skittish herd; horns/nose via flags |
| `toxodon` | `EntityHippo` | bulky semi-aquatic notoungulate |
| `macrauchenia` | `EntityCamel` | llama/camel body + short proboscis |

## Bug-avoidance checklist

1. `variant` indices must be unique and contiguous, appended after the current max (texture arrays are
   keyed by the int).
2. **Skin encoding** (`util/EntityUtils.buildSkinArrays`): single texture → omit `skins`, file
   `<species>.png`; N commons → `skins=N*10`, files `<species>_1..N.png`; +M rares → `skins=N*10+M`, extra
   `<species>_1r..Mr.png`. `dimorphism:1` variants resolve to `<species>_male.png`/`<species>_female.png`.
   A wrong count renders pink/black (not a crash).
3. Every species needs both `entity.untamedwilds.<type>_<species>` and `.sciname`. New types also need base
   `entity.untamedwilds.<type>` and `item.untamedwilds.<type>_spawn_egg`. Missing = raw string, not a crash.
4. New types: all three `ModEntity` hooks are mandatory.
5. New types: the `<type>_spawn_egg.json` item model must exist.
6. New types spawn wild only if added to a `spawn_tables/<category>.json`.
7. Biome coherence: cold fauna → `taiga`/`icy`/`extreme_hills`; steppe → `plains`/`savanna`. An unmatched
   set just yields no natural spawn, never a crash.
8. Stay on the MC 1.18.2 / Forge / Citadel / Java 17 APIs; copy method signatures verbatim from templates.

## Predator hunt balance (implemented)

`entity/ai/target/HuntMobTarget.java::canUse()` refuses to hunt while `getHunger() > threshold`. Hunger is
an int capped at 200 (`ComplexMob.addHunger`).
- All predators pass `threshold = 30` (only hunt when hungry). The no-threshold constructor defaults to 30;
  `EntityMonitor` was fixed to 30. (`EntityGiantSalamander`/`EntityFootballFish` remain intentionally
  opportunistic aquatic ambushers.)
- `ComplexMob.satiateFromKill(Entity)` restores +120 hunger when a predator lands a killing blow (+90 to
  pack-mates within 16 blocks, who share the carcass), called from each predator's `doHurtTarget`. This
  stops predators from thinning whole herds one kill per cooldown. Canids reuse `HuntPackMobTarget` (shared
  pack target) so a pack takes one animal, not one each.
  Do not strip `SmartAvoidGoal`/`ProtectChildrenTarget` from herbivore templates.

### The hunt commitment — read before touching any break-off or flight code

Between them, three mechanics made it impossible for a predator to ever land a killing blow. Each was
individually reasonable and all three had to be fixed together, so **do not "simplify" any one of them
back**:

1. **`activeHunt` cannot carry the hunt exemption on its own.** It lives for exactly as long as the hunt
   goal owns the `TARGET` flag, and every break-off goal (`SmartHurtByTargetGoal`, `HurtPackByTargetGoal`,
   `ProtectChildrenTarget`, …) is registered at a *higher* target priority. The instant prey bites back,
   the `GoalSelector` stops the hunt goal, `activeHunt` hits 0, and the "spare a foe below 10% health"
   clause released the prey. `ComplexMob.huntCommitTicks`/`huntVictim` is a timer on the *animal*, so it
   survives being preempted; `ComplexMob.tryBreakOff()` is now the **single** implementation of the
   break-off rule and the only place that knows about the exception. Six goals had copies of it.
2. **The cooldown is paid on the kill, not on the attempt.** `huntingCooldown = 6000` at hunt *start* meant
   one interruption locked a predator out for five minutes while the animal it had nearly killed healed.
   `start()` now charges `predator_failed_hunt_cooldown`; `satiateFromKill` charges
   `predator_kill_cooldown`. `HuntMobTarget.canUse()` also re-acquires a still-committed victim while
   ignoring the cooldown, or the handover back from a retaliation goal drops the chase for good.
3. **Fleeing prey was strictly faster than every predator.** `HerdFleeGoal` runs prey at `2.0×`; a deer at
   speed 0.25 flees at 0.50 while a dire wolf pursues at 0.24 × 1.6 = 0.384. The gap never closed, so the
   fix is *not* to slow prey down or speed predators up across the board: prey keeps its full sprint and
   loses stamina (`ComplexMob.fleeStamina`/`getFleeSpeedFactor()`, applied in `SmartAvoidGoal.fleeSpeed`),
   and the predator gets `predator_chase_burst` **only** while committed. Prey wins the sprint, loses the
   marathon, and genuinely fast species (pronghorn) still get away.

Overkill is bounded by, in order: the hunger gate, `predator_kill_cooldown`, the ~10 in-game minutes of
hunger a kill buys, one 30 s attempt per `predator_failed_hunt_cooldown`, the shared pack target, and
`predator_herd_floor` (predators will not touch a herd of ≤ 2 unless starving). All eight numbers are
`ConfigGamerules` entries — tune there, not in the goals.

### Who can be hunted, and what hunting costs (2026-07-29)

Four more rules joined that set. They are also mutually load-bearing — read them together.

1. **`getEcoLevel` weights condition and age.** The herd bonus is now scaled by the animal's health
   fraction and the whole level is multiplied by `BABY_ECO_FACTOR` (0.3) for a juvenile. Before this,
   *nothing in the game could kill a megaherbivore*: `updateAttributes()` gives a calf the full adult
   `MAX_HEALTH`/`ATTACK_DAMAGE`, so a mammoth calf scored 22 exactly like its mother against a dire wolf
   pack's 14, and the flat `+ herdSize` meant even an adult at 15% health scored 14. A healthy adult
   still scores 22 and is still untouchable, which is correct — only calves (7) and the dying (6) opened
   up. **Do not re-flatten the herd term**: it is the piece that was doing the blocking.
2. **`predator_herd_floor` exempts calves and solitary species.** Every `IPackEntity` gets a herd object
   even at `groupCount` 1, so `ground_sloth` (1) and `glyptodont` (2) sat permanently at or under the
   floor and were literally unhuntable by anything. The floor protects breeding stock, not the part of a
   herd predation is meant to take; it compares against `herd.getMaxSize()`, not current membership, so a
   thinned real herd keeps its protection.
3. **`predator_hunger_drain`** scales carnivore hunger decay only (herbivores refill by grazing, so their
   rate barely matters). Canids had been on the herbivore rate of −10, which had a pack killing about
   twice an in-game day — faster than any herd breeds back. Now −4 for canids, −2 for big cats/bears, all
   via `ComplexMob.drainHuntingHunger`. Note this also keeps predators above the hunger ≥ 80 breeding
   gate longer, so it *raises* predator birth rates; rule 4 is the counterweight.
4. **`predator_hunt_risk_chance`** is the only way a predator dies of hunting. `tryBreakOff` makes any
   animal spare a foe below `ROUT_THRESHOLD`, and a hunter is already exempt from being routed itself
   while committed — so hunting carried no risk at all; a predator could lose every fight it picked and
   walk away from all of them. `commitToHunt` now rolls `huntIsRisky` once per hunt (doubled vs
   megafauna) and `tryBreakOff` will not spare a hunter mid-commitment on a risky hunt. Rolling **once,
   at commit** is deliberate — a per-tick roll makes one fight flicker between mercy and none. The share
   of hunts that actually kill the hunter is far below this number, since it still has to lose.

### Predator strength and class identity (2026-08-01)

Predators were weak and interchangeable — `EntityDireWolf` was a near-verbatim copy of `EntityHyena`, and
only the bear had a signature mechanic at all. Every mechanic added here is deliberately built from a
channel **`getEcoLevel` cannot see**, because health and attack are its inputs and changing either in a
species JSON re-sorts the entire food web.

1. **`predator_hunt_damage_burst` (2.0)** is the counterpart of `predator_chase_burst`: a damage
   multiplier applied *only* while committed to a hunt for food, and zero in a brawl, cub defence or
   retaliation. Delivered by `ComplexMob.applyAttackBonus`/`clearAttackBonus`, a transient ATTACK_DAMAGE
   modifier under a fixed UUID that **must** be torn down in a `finally` — if it survives the call,
   `getEcoLevel` reads it and the animal silently changes what it hunts. Bonuses **sum**, because
   `MULTIPLY_TOTAL` adds its amounts: a committed lion landing an ambush is `x(1 + 1.00 + 0.75)`.
2. **Four identities, on four axes.** Big cat = burst (`bigcat_ambush_multiplier`, opening strike of a
   stalk only, once per hunt, and never against quarry already fighting back; plus
   `bigcat_cornered_reduction` below half health, the mirror of Bearserk). Bear = control
   (`bear_stagger_ticks`; bears are the slowest predators in the mod, so the fix is to make them
   inescapable, not faster). Dire wolf = endurance (`canid_stamina_drain`, straight out of the existing
   `fleeStamina` budget). Hyena = economy (`hyena_pack_damage_step` capped at +50%, plus
   `shareCarcassWithScavengers`, which lets a clan eat off other predators' kills so it hunts — and so
   risks — less often).
   **Kleptoparasitism is driven by the KILLER and tested against the RECIPIENT**, via
   `ComplexMob.shareCarcassWithScavengers` calling the recipient's `canScavengeCarcass()`. Overriding the
   sharing method on `EntityHyena` looks natural and is wrong: it would only ever fire when a hyena had
   already made the kill itself, which is the exact opposite of stealing a lion's.
3. **Durability is damage reduction, never health.** `ComplexMob.getIncomingDamageFactor` +
   `ComplexMob.hurt`. Pack hunters were dying to the animals their pack eco level said they could take: a
   dire wolf is 34 HP against a wooly rhino's 10 attack, so four bites. Raising it to 55 HP instead would
   lift the pack from eco 15.0 to 16.9 and bring **seven** prey species into range past the diet filter
   (mixotoxodon, vicuna, giraffe, pronghorn, cuvieronius, grevy's zebra, titanotylopus). Measured, not
   guessed — re-run the check before changing any predator's health.
4. **`predator_gestation_multiplier` / `predator_maturity_multiplier` / `predator_litter_bonus`** stack on
   top of the global breeding scalars for carnivores only, gated on the `carnivore` ecology **tag** —
   *not* `EcologyTags.isPredator()`, which also returns true for anything declaring a diet and so catches
   herbivores. Eight types carry the tag and are therefore affected: `bear`, `big_cat`, `dire_wolf`,
   `hyena`, plus `football_fish`, `large_snake`, `monitor` and `shark`. The last four are egg-layers, so
   `predator_litter_bonus` widens their clutches through `getOffspring()` → `EntityUtils.dropEggs` as
   well — intended, but check `monitor` (offspring 5 → 7) if lizards start piling up.
   This is the only part of the set that can destabilise a world: nothing else raises the kill *rate*, but
   more predators reaching adulthood means more hunger clocks. If herds shrink, pull
   `predator_hunger_drain` down first, then raise `predator_herd_floor`.

**`EntityBigCat` was wired wrong and is now fixed.** It registered the *pack* attack goal
`MeleeAttackCircleHerd` — with a comment claiming `HuntPackMobTarget` accompanied it — while its target
goals were the *solitary* `HuntMobTarget`/`SmartHurtByTargetGoal`. A pride of eight therefore acquired
eight separate targets, each lion committed alone, seven orbited at six blocks contributing nothing, and
nothing came when one was attacked. Both are now the pack versions, matching `EntityHyena`. Note this
*lowers* predation pressure: a pride now takes one animal instead of up to eight.

**Two levers that look obvious and are not — do not re-attempt either:**
- **ARMOR.** MC's `CombatRules.getDamageAfterAbsorb` reduces by `max(armor/5, armor - damage/2)/25`, so
  the bear's 4 armor against an 8-damage bite is worth **3.2%**. The big cat's 0 versus the bear's 4 is
  almost entirely illusory; closing it fixes nothing.
- **Attack cooldown.** `SmartMeleeAttackGoal` has an extra fast-swing clause that
  `MeleeAttackCircleHerd` lacks, but it only fires once the mob's animation has ended, and every bear
  attack animation except BITE (18) is longer than the 20-tick cooldown. Worth ~10%, one time in four.

`mate_search_radius` (24) is in the same family: `SmartMateGoal` searched an 8-block box and nothing made
a lone animal in season go looking, so sparse species — predators especially — could only shrink. Widening
it alone would **not** have worked: `spawnBabyDelay` counted from the moment the goal started and
`canContinueToUse` gave up at 200 ticks, so a distant pair always abandoned the approach part-way. Travel
time now burns a separate `TRAVEL_BUDGET` and only time spent within mating range counts toward breeding.

### Ecosystem simulation: bounded mobbing, group morale, and range shifting (2026-08-02)

Predators were still dying out even after the strength pass. They were not losing to other predators —
they were being **killed by prey**. Three structural faults, all of which had to be fixed together.

**1. Group defence was unbounded.** `ProtectChildrenTarget` runs independently on *every* adult: each
one looks for a same-variant calf within 8 blocks of itself and a threat within `DEFEND_RADIUS` of that
calf. In a `groupCount:20` bison herd with calves scattered through it, a dozen adults satisfy that at
once. Bison are attack 7; a dire wolf is 34 HP × 0.30 reduction = 48.6 effective, so ten defenders land
~49 damage per round and kill it in about two seconds *regardless of how strong it is*. `HerdEntity`
now hands out **defender slots** (`tryClaimDefenderSlot`/`releaseDefenderSlot`, cap
`herd_defender_cap` = 3, plus a per-animal `defendCooldown` so one fight cannot cycle a fresh trio
through the cap). Nothing extra was needed to make the rest back off: the HEAVY herbivores already
carry a `SmartAvoidGoal` keyed on predators that is suppressed only while they hold a target, so
**refusing the target is the backing away**.

**2. Being defended against ESCALATED the fight.** `HurtPackByTargetGoal.alertOthers()` set the target
on every pack member, so one bison goring one wolf handed the entire pack that bison — the hunt was
dropped and a pack-vs-herd brawl started in its place, which the pack always lost. That is now
inverted: a **hunting** predator hit by anything that is not its `huntVictim` declines the retaliation
and credits `reportDefenderPressure` instead, and the alert never overrides a live hunt and carries
only 12 blocks. Same clause added to `SmartHurtByTargetGoal`.
Group morale is new: `HerdEntity.recentLosses` (from `ComplexMob.die`) and accumulated pressure both
decay over `pack_rout_window`; past `pack_rout_loss_threshold` (2) or `pack_rout_pressure` the whole
group calls `endHuntCommitment` + `routFromCombat`. **`ComplexMob.rememberDefeat`/`isAvoiding` is
load-bearing** — without a rout *memory* (`predator_rout_avoid_ticks`) `HuntMobTarget` re-acquires the
same herd within seconds and routing is cosmetic. Same lesson as `activeHunt`: a decision that lives on
a goal does not survive the goal being stopped. `ComplexMob.oddsAreAcceptable()` also refuses to *start*
a hunt at worse than `predator_odds_ratio` defenders per hunter — checked at start only, or a predator
would abandon every hunt the moment the first defender arrived.

**3. Prey had no way to resolve its own density — and the fix is NOT a birth cap.** Herds bred in place
and never moved (`SmartWanderGoal` picks positions within 10–15 blocks, or 7 of the leader), so an
eaten-out range stayed occupied and local prey only ever climbed. Capping births would stop the
population growing at all; what real ungulates do is **leave**. So:
- `ComplexMob.forageStress`, driven by `GrazeGoal` (up when its 16-block search finds nothing, down on
  every mouthful) is the habitat-quality signal, and it finally gives `grazer_griefing` a purpose —
  a herd eats a patch to dirt, moves on, and the range regrows behind it.
- `HerdMigrationGoal` (new) runs on the **herd leader only**, because `SmartWanderGoal.getPosition()`
  already paths every other member to within 7 blocks of the leader — *move the leader and the herd
  follows*. It scores candidate ranges 64–128 blocks out on food minus conspecifics.
- **`HerdEntity.effectiveSplitOffDistance()` is not optional.** `splitOffDistance` is 32 blocks and
  `tick()` drops anyone past it; a migrating herd would shed every straggler mid-journey, each founding
  its own herd where it was dropped. Followers are also pathed at the leader actively during a
  migration, since `SmartWanderGoal`'s 1-in-120 re-path cannot keep up.
- Arrival moves `setHome()` for every member, or `GotoSleepGoal` walks them all the way back overnight.
- Natal **dispersal** (`pickDisperser`/`detachDisperser`) sends one young adult out of a full, crowded
  herd to found its own. This is the growth valve: births are never blocked by crowding, the surplus
  leaves. It is also the only mechanism in the mod for colonising new ground after initial spawning.
- `ComplexMob.isRangeExhausted()` — starving **and** `herd.migrationFailed` — is the *only* thing that
  suppresses breeding, and it is the last resort. Ordinary crowding deliberately does not.
- `PredatorRelocateGoal` is the counterweight and is equally load-bearing: if herds move and packs do
  not, packs starve on empty ground and predator extinction returns by another route.

**Engine constraint, stated in the config comments too:** MC only ticks loaded chunks, so this is range
*shifting* bounded by simulation distance (~160 blocks), not continental migration.

**Four species interactions**, all of which stop the food web collapsing to one predator and one prey:
- **Prey switching** (`EcologyTags.preference` × `ComplexMob.censusOf`, cached per tick): a predator
  prefers whichever prey is locally *common*. Without it, "nearest thing I can eat" removes prey
  species one at a time. This is the single most important stabiliser in the set.
- **Straggler bias + dilution** (`HuntMobTarget.Sorter.conditionBias`, `SmartAvoidGoal.detectDistance`
  overridden in `HerdFleeGoal`): wounded, blown and herd-separated animals are taken first, and bigger
  herds spot predators sooner. Together these are what make herd size protective **without** the herd
  having to mob the predator — the ecological answer to fault 1.
- **Carrion + kleptoparasitism**: `ComplexMob.die` pays `carrion_hunger` to scavengers for *any* death,
  and `findCarcassThief`/`loseCarcassTo` (resolved at the kill, since a carcass is not an entity here)
  lets a hyena clan of `kleptoparasitism_clan_size` or a single bear take a kill outright. Bears now
  scavenge too. `displacementGroupSize()` is on the **displacer**, `yieldsCarcassTo()` on the loser.
- **Mixed-species alarm network**: `HerdFleeGoal.alertNeighbours` panics other herbivore species within
  `alarm_network_radius`; giraffids carry `sentinel_detect_bonus` and are the early-warning system.
  Predators are deliberately excluded, or prey would get a free escape from anything hunting it.

**Ecosystem vs Zoo** is a real vanilla gamerule, `/gamerule untamedwildsEcosystemMode <true|false>` —
per-world, saved in `level.dat`, flippable mid-game, which a config entry cannot be. Registered in the
`UntamedWilds` **constructor** (vanilla builds each world's `GameRules` from the static type map, so a
rule added after the first world loads is absent from it) and it needs two lines in
`META-INF/accesstransformer.cfg`: `GameRules.register` (`m_46189_`) and `GameRules$BooleanValue.create`
(`m_46250_`) are private/package-private in vanilla. `config/EcologyMode.java` is the single read point.
Zoo: gestation ×2.5, maturity ×2.0, no litter bonus, hunger drain ×0.4, hunt threshold 30→8 (starving
only), hunt risk 0, defender cap 1, no migration or dispersal, and **no brawls** — retaliation, pack
call-for-help, `BisonTerritorialityFight` and `AngrySleeperTarget` all decline unless a *player* started
it. Defence of young still runs; a mother should still see off a wolf in a zoo.
**Deliberately not a rewrite of every config call site** — only the ~15 numbers the modes disagree about
route through `EcologyMode`, so existing tuning stays valid.

Twenty-one new `ConfigGamerules` entries carry every number; tune there, never in the goals.
`./gradlew build` **SUCCESSFUL**; **runClient not yet run**.

### Demography: starvation, senescence and carrying capacity (2026-08-10)

Predators — wolves, dire wolves, american lions especially — still overpopulated and drove prey
locally extinct, pack hunters almost never died, and prey-rich areas were grazed to bare dirt.
Everything above this section models **behaviour**; none of it could ever bound a population,
because the mod had **no demography at all**. Four structural gaps, and the fixes are mutually
load-bearing in the same way the ecosystem set above is.

**1. There was no density-dependent mortality.** `isStarving()` was read in ~25 places and its only
consequence anywhere was skipping a `heal()` call. Hunger floored at 0 and the animal lived forever;
a starving predator even *bypassed* `predator_herd_floor`, so an over-dense pack hunted a herd past
the point it should have been left alone. With `removeWhenFarAway()` false and no despawn, nothing in
the world removed a wild adult except a player — which is the entire reason pack hunters accumulated.
`ComplexMob.tickStarvation` now applies real damage after `starvation_grace_ticks`.
**Babies and tamed animals are exempt and must stay exempt**: `GrazeGoal.canUse()` refuses to run for
`isBaby()`, so a calf physically cannot feed itself and without the exemption every calf in the world
starves. The resulting health loss lowers `getEcoLevel` and feeds `Sorter.conditionBias`, so animals
in poor condition get taken first through machinery that already existed — intended, not incidental.

**2. Predator breeding had no prey term.** It was a bare `getHunger() >= 80`, and `satiateFromKill`
pays +120 to the killer plus +90 to every pack-mate within 16 blocks, so **one carcass flipped an
entire eight-wolf pack into breeding simultaneously** — with gestation ×0.33, maturity ×0.33 and
litter +2 stacked on top. Births tracked kill *events* instead of prey *density*: exponential growth
by construction. `ComplexMob.condition` is a slow exponential average of hunger
(`condition_halflife_ticks`), and `wantsToBreedAsPredator()` gates on **sustained** condition plus
`isLocallyOvercrowded()`. **The lag is the mechanism, not a side effect** — it is what turns runaway
growth into a bounded oscillation, so do not shorten the halflife to make predators "responsive".
Note the deliberate asymmetry: for herbivores crowding means *move* (`HerdMigrationGoal`), for
carnivores it means *do not breed*, because territoriality is the real brake on carnivore density.

**3. Forage was infinite.** Grazing paid a flat +16 on any ground at all against a −10/1000-tick
drain, so one mouthful per ~1600 ticks sustained an animal no matter how bare its range was.
`GrazeGoal.sampleForageDensity()` now probes 16 **heightmap columns** — a naive 3D probe lands in open
air or inside the ground and reads intact grassland as barren — and `ComplexMob.getForageYield` scales
the mouthful from `forage_yield_max` to `forage_yield_min`. `world/ForageRegrowthHandler` is the other
half and is **not optional**: `GrazeGoal` destroys plants and converts grass_block to dirt, vanilla
only restores either through `SpreadingSnowyDirtBlock`'s random tick, so without regrowth the map is a
one-way ratchet to bare dirt and carrying capacity only ever falls. It samples around **players**,
because MC only ticks loaded chunks, and it requires a living neighbour to spread from so an isolated
dirt patch in a desert cannot spontaneously turn green.
Herbivore breeding moved from `!isRangeExhausted()` (starving **and** `migrationFailed`, a conjunction
that almost never became true) to `!isForageStressed()`. **This is not a birth cap** and does not
contradict the migration design — herds still migrate first, they simply do not calve on ground that
cannot feed a calf. This also fixed `EntityRhino` and `EntityHippo`, which had no guard at all.

**4. Nothing aged.** Any cohort that reached adulthood was immortal. `ComplexMob.lifeTicks` is
persisted and `getLifespan()` is **derived as `getAdulthoodTime() × lifespan_multiplier`**, not
authored per species — so it inherits `cycle_length`, `maturity_multiplier` and
`predator_maturity_multiplier` for free, keeps the maturity:lifespan ratio identical across the
roster, and needed **zero edits to the ~40 entity JSONs**. `isSenescent()` stops breeding (checked on
the *base* `wantsToBreed()`, so all seven `super.wantsToBreed()` callers inherit it) and adds a
straggler-bias term so predators take the old. Old-age death ramps from 0 at the lifespan to
`old_age_mortality_chance` at 1.5×, and kills by **damage rather than `discard()`** so `die()` runs and
the carcass feeds scavengers — exactly the case `feedScavengers`' comment anticipated and nothing
could previously produce. `finalizeSpawn` randomises `lifeTicks` below the senescence threshold, or
every animal a chunk generates is newborn and a whole region dies off on the same day.

**Two traps this pass had to work around, both worth re-reading before touching it:**
- **Weakness is expressed as MOVEMENT_SPEED and nothing else.** `MAX_HEALTH` and `ATTACK_DAMAGE` are
  the two inputs to `getEcoLevel`, so a "sickly" or "elderly" penalty on either would silently re-sort
  every targeting predicate in the mod. Same rule as the predator-strength pass; same fixed-UUID
  transient-modifier pattern (`CONDITION_PENALTY_UUID`).
- **Zoo mode gates CONSEQUENCES, not bookkeeping.** `condition` and `lifeTicks` accrue and persist in
  both modes. If they did not, flipping `untamedwildsEcosystemMode` to true mid-world would hand every
  existing animal a condition of 0 (instantly starving-weak) and a lifeTicks of 0 (a fresh lifespan).
  Equally, the five new `EcologyMode` gates make the breeding predicates **fall back to the original
  ones** rather than skipping the check — a gate returning `true` would leave zoo animals breeding
  *more* freely than before any of this existed.

Everything here is Ecosystem-mode only: `allowsStarvationDeath`, `allowsConditionPenalty`,
`allowsSenescence`, `allowsForageDepletion`, `usesConditionForBreeding`. Fifteen new
`ConfigGamerules` entries; tune there, never in the goals. The `AnalyzerItem` now prints condition,
age against lifespan, and starvation state — tuning these gamerules is guesswork without it.
`./gradlew build` **SUCCESSFUL** and the new class is jar-verified; **runClient not yet run**, and for
this pass in particular the acceptance test is a long session watching predator and prey counts
oscillate within bounds rather than one going to zero.

### Pack burst sharing and the herd's answer to it (2026-08-11)

Pack hunters were still overpowering herbivores. Not tuning drift — a **collision between the two
passes above**, which were designed independently and pull in opposite directions. The 2026-08-01 pass
gave each predator a ×2 damage burst; the 2026-08-02 pass capped herd defence at 3 and made a hunting
predator decline retaliation. Together they doubled the pack's output and capped the herd's answer.

**The arithmetic that made the case.** A dire wolf pack of 8 against a steppe bison (70 HP, attack 7):
each wolf lands 9 × 2.0 = **18**, all eight share one victim via `HuntPackMobTarget`, and every one of
them swings each 20 ticks — `MeleeAttackCircleHerd` has no fast-swing clause and its
`CLOSE_IN_THRESHOLD` (0.35) makes the whole pack stop circling and close at once. **144 damage per
second.** The herd answers with at most `herd_defender_cap` 3 × 7 = **21**, against wolves at 34 / 0.7
= 48.6 effective. Bison dies in half a second; the pack takes nineteen. **And 21 is the best case** —
see the second fault.

**1. The burst was per-animal, so pack damage multiplied while herd defence was capped.**
`ComplexMob.getHuntDamageBonus` handed the full `+1.00` to every member independently.
`pack_burst_sharing` (**0.5**) now divides it by `pow(engaged, sharing)`, counted by
`countPackCommittedTo(victim)` — pack-mates committed to **the same victim** and within
`ENGAGEMENT_RADIUS` of **it**, not of the hunter, because what is being divided is the number of mouths
on one carcass. A **lone hunter is untouched at ×2.0**: a solitary cat or bear was never the problem,
and the 2.0 was a deliberate choice (see the memory note) that this preserves. Eight wolves get ×1.35
each, so pack DPS falls 144 → 97. Set to 0 to restore the old behaviour exactly.
Both per-class bonuses are added *after* `super` and stay unshared, correctly: the **big cat ambush**
is by definition one cat's opening pounce, and the **hyena clan step** is an explicit pro-pack bonus —
a clan of 8 now runs ×1.85 against a lone hyena's ×2.00, so the clan identity finally registers instead
of being swamped by an unshared burst.

**2. Herbivores had no way to help each other at all.** Predators call for help
(`EntityDireWolf` registers `HurtPackByTargetGoal.setAlertOthers(...)`); every herbivore registered a
bare `SmartHurtByTargetGoal`, which **never enables vanilla `alertSameType`**, so an attacked adult
never told its herd. The only group-defence goal in the mod was `ProtectChildrenTarget`, which needs a
**same-variant baby within 8 blocks** — a herd with no calf nearby had **zero** group defence, and the
seven light herbivores (deer, antelope, equid, tapir, macrauchenia, giraffid, camel) had no defence
goal and no charge whatsoever. New `entity/ai/target/DefendHerdMateTarget.java` answers an attack on
any herd-mate within `herd_defend_radius` (12), triggered off vanilla's
`getLastHurtByMob`/`getLastHurtByMobTimestamp` so it needs no new bookkeeping. Registered on all
thirteen herd herbivores at targetSelector 3, below `ProtectChildrenTarget` so calf defence keeps first
call on the slots. **It claims from `HerdEntity`'s existing defender pool**, inheriting the cap, the
re-engage cooldown, `pruneDefenders` and group rout unchanged — it changes *what* brings defenders out,
never *how many*, so it cannot recreate the unbounded mobbing the defender cap exists to stop. Gated by
`EcologyMode.allowsBrawls` (a call-for-help, unlike defence of young), so zoo herds do not rally.

**3. The panic was disarming the defenders — this fix is not optional.** `HerdFleeGoal.panic()` clears
every member's target and `canUse()` re-clears it for the whole 200-tick window, rescanned every ten
ticks. A predator is *seen* before it bites, so the panic always came first and a defender's target was
wiped within ten ticks of it claiming a slot. `HerdEntity.isDefender` now exempts slot-holders from
`panic()`, from `canUse()`'s clear, from `shouldAbandonTarget()`, and from `canContinueToUse()` (which
`canUse` cannot reach once the goal has started). **Without this the new goal is inert.** Non-defenders
still panic and run exactly as before — that flight *is* the "the rest back away" half of the cap.

**4. The herbivore's weapon was strictly worse than a bite.** `MeleeAttackCharger` telegraphed for 50
ticks, rolled 1-in-3, and then dealt a bare `doHurtTarget` — **no multiplier at all**. A bison charge
was 7 after a 2.5 s wind-up against a wolf's 18 with no tell. `herbivore_charge_multiplier` (**2.0**)
is the counterpart of `predator_hunt_damage_burst`, delivered by new `ComplexMob.doChargeHurtTarget`.
That lives on `ComplexMob` **so `applyAttackBonus`/`clearAttackBonus` stay package-private** to the
entity classes — the `finally` pairing is the one thing here that must never be got wrong. Also:
`start()` picked the wind-up with two hardcoded `instanceof` checks for bison and rhino, so the
**mammoth charged with no telegraph at all**; replaced by a `getChargeAnimation()` hook on
`ComplexMobTerrestrial`. The charger was extended to `EntityToxodon` and **deliberately not** to
glyptodont or ground sloth, whose motion is a club swing and a rearing swipe, not an overshoot run.

**Diagnosed and deliberately left for later** — each is a real cause of prey decline:
- **Herd protection is a cliff with positive feedback.** `getEcoLevel` uses the *current* member count,
  so a bison herd scores 28 at 20 members, 14 at 6, and **13 at 5 — huntable**. Every kill lowers the
  next animal's score and `predator_herd_floor` only catches the last two. Fix is
  `max(living, maxSize × memory)`, the same idea `isHerdTooSmallToHunt` already uses.
- **Calves are unprotected by construction — the quiet extinction mechanism.** `BABY_ECO_FACTOR` 0.3
  plus the explicit baby exemption in `isHerdTooSmallToHunt` means recruitment collapses and a herd
  ages out under senescence **without a single adult being taken**.
- **Solitary megafauna have nothing**: rhino, ground sloth and moose have `herd == null`, so no herd eco
  term, no defender slot, no alert. A wooly rhino scores 9 against a wolf pack's 14.
- **Flight manufactures its own stragglers** — `panic()` disperses each animal individually, creating
  exactly what `straggler_bias` 2.0 then prefers.
- **Dead code**: `ComplexMob.performRetaliation` only fires on thorns damage, which nothing deals;
  `EntityBison.CHARGING` is read but `setCharging` has no caller.

Three new `ConfigGamerules` entries; tune there, never in the goals. `./gradlew build` **SUCCESSFUL**
and `DefendHerdMateTarget` is jar-verified; **runClient not yet run**. Highest-priority in-game checks:
a wolf's eco level immediately after a hunting blow must equal its value at rest, **and the same for a
bison immediately after a connected charge** — that is a brand-new `applyAttackBonus` call site with
the identical leak risk.

### Roadmap + Phase 1 correctness fixes (2026-09-30)

The active plan is `docs/plans/2026-09-30-status-audit-and-roadmap.md` (11 phases: safety net,
correctness, runClient + census baseline, optimization, predator balance, prey ecology, skins,
mammoth animations, upstream cleanup, release, future mobs). **Approved plans now live in
`docs/plans/`** — `~/.claude/plans` is auto-cleaned and every earlier mod plan, including the
future-species list, was lost that way. Phase 1 (build SUCCESSFUL, runClient pending):
- **`HerdEntity.tick()` ran once per MEMBER.** The land species call it from every member's
  `aiStep` (fish guard it with a leader check); the `leader.tickCount % HERD_TICK` gate does not
  dedupe, because every member sees the same leader tickCount in the same tick. Herd timers decayed
  N× too fast — a 20-herd's 6000-tick migration cooldown lasted ~300 ticks, and pack losses/pressure
  evaporated so fast that **packs could never rout**. Now guarded by `lastTickedGameTime`. Any
  rout/migration tuning done before this fix was tuned against the bug.
- **Per-frame ERROR spam**: `ModelBison.setupAnim` reads `longHorns`/`lessHair` every frame and
  `EntityDataHolder.getFlags` logged every miss (12k lines in one session). Type-level defaults added
  to bison/big_cat/rhino; `getFlags` now reports each missing (type, flag) once. **Any flag a model
  reads must have a type-level default.**
- **Deer tail was 100% buried** in `body_croup` (zchain lays a chain forward along −Z, so −62°
  meant down-and-FORWARD). Re-solved by grid search in `deer_spec.py`: pivot on the rear face,
  −110/−115. Box sizes + texOffs unchanged; Java re-verified against the spec via `java2bb.py`
  (59/59 parts). The skins were painted while the tail was invisible — look at it in-game.
- **Worldgen**: `FaunaSpawn` discarded its random offset (`pos.offset(...)` result unused), so every
  surface group spawned on the chunk-corner column; `FeatureUndergroundFaunaLarge` spawned at the
  unchanged origin and could make 625 attempts per placement. Spawn-table `size_min/size_max` is
  still ignored (pack size = species `groupCount`) — deliberately left for Phase 4, since honouring
  it changes spawn density for every solitary species.
- Dead `CHARGING` removed from bison/rhino (never set). `performRetaliation` left **inert on
  purpose** (thorns-only check; enabling it is a balance decision for Phase 5).
- **`/untamedwilds census`** (op 2) + `census auto <minutes>` → `logs/uw_census.csv` with adults,
  juveniles, mean condition, starving, senescent per species. This is the measuring tool for every
  ecology change from here on. **`/untamedwilds census <species>`** prints one species' status in
  chat — population by sex/age with the change since your last check, groups + migration, condition,
  hunger, pregnancies, elderly, eco level range, and the nearest animal (click to fill a `/tp`).
  Accepts `type:species`, a whole `type`, or a unique bare species name; Tab suggestions match
  anywhere in the name (`gray` → `dire_wolf:gray_wolf`) and show the in-game name as a tooltip.
  It is a greedy-string argument beside the `auto` literal (Brigadier prefers the literal), because
  `word()` rejects the colon.

### Phase 3 — optimization (2026-09-30, build SUCCESSFUL, not yet profiled in-game)

Behaviour-preserving: the same decisions, evaluated less often. **Throttling rule learned here:**
vanilla gives goals their full tick only when `(serverTick + entityId)` is even, so a
`tickCount % N` gate inside a goal can sit on the skipped parity and never fire. Goals use a
countdown or `getRandom().nextInt(reducedTickDelay(N))`; `% N` staggers are only safe in `aiStep`.
- `HerdFleeGoal`: only the animal that STARTS a panic alerts the herd and alarm network, and an
  animal already fleeing keeps its path (was 1000+ pathfinds per sighting in a big mixed herd).
- `MeleeAttackCircleHerd`: circle re-path every ~10 ticks or when the path ends (was every tick).
- `HuntMobTarget`: cheap distance test first in `isValidTarget`; fresh searches on vanilla's ~10-tick
  random interval (re-acquiring a committed victim stays immediate); `Sorter.best()` picks the target
  in one pass instead of a sort that recomputed census weights twice per comparison.
  `ProtectChildrenTarget` and `FollowParentGoal` got the same random interval.
- `LookThroughSpyglassEvent`: the raycast could load/generate chunks up to 5000 blocks out — now
  clamped to the view distance. Its entity search passed range where vanilla wants a SQUARED
  distance (effective reach ~158 blocks, box 25,000 blocks long); reach kept, box sized to it.
- `ForageRegrowthHandler`: attempts spread evenly across the interval (was a ~5000-lookup spike every
  200 ticks), and the neighbour sweep checks its chunks are loaded first.
- `HerdEntity` migration followers re-path only when arrived or the leader moved > 8 blocks;
  `isLocallyOvercrowded` cached 100 ticks per animal; `findCarcassThief` scans once, not once per rival;
  `EcologyTags` stops allocating one-element lists per lookup; the 19 `% 1000` hunger/heal blocks are
  staggered by entity id; `predator_odds_ratio` can now be 0 (disables the check and its scan).
- Client: all 52 models build `getAllParts()` once (was a fresh ImmutableList per animal per frame).
  The 70 `Animation` statics are now `static final` — they were recreated in every entity constructor,
  and since models compare them by identity, spawning an animal could drop another's running animation.
- Deliberately not done yet: leader-shared predator scans for herbivores (changes the "many eyes"
  detection design — only if a Spark profile says it matters).

### Phase 4 — predator balance: wolves dominate, other predators die out (2026-09-30)

Reported from real play. Build SUCCESSFUL, **not yet soak-tested** — the acceptance test is a census
run (`/untamedwilds census auto 5`) where every predator type persists and no herd goes to zero.
Nothing here touches JSON `health`/`attack`. Five structural causes, all fixed together:
1. **Packs grew without limit and got stronger as they grew.** One carcass paid +120/+90 to *every*
   pack-mate within 16 blocks however many there were, so pack size cost nothing. Now a kill is a food
   budget of `carcass_food_per_health` (8) × prey max HP, split among the eaters, with the old
   120/90 as caps — lone hunters and small packs on decent prey eat exactly as before. Pups counted in
   the eco herd term (each litter made the pack more dangerous): carnivores now count
   `min(adults, maxSize)`. Pups count toward predator crowding (`countOvercrowded`). Oversized packs
   shed surplus adults 48–96 blocks away (`HerdEntity.dispersePredatorSurplus`; dispersal previously
   only ran from the herbivore migration goal). `dire_wolf` litter `offspring` 5 → 3.
2. **Solitary cats and bears rarely bred.** They spawned alone, mates had to be within 24 blocks,
   and they carried breed gates (season, awake, 60% HP) canids did not. Now: solitary species spawn
   as an opposite-sex pair where the spawn table allows ≥2 (and `getGroupCount` is inclusive — "1–2"
   could never roll 2); a solitary animal whose near search fails searches
   `solitary_mate_search_radius` (96) with a longer travel budget; all four land predators share
   `ComplexMob.wantsToBreedAsLandPredator()` (season + 60% HP + condition, no awake check).
3. **Lone predators had almost nothing they could hunt** (every herd animal carried its whole herd's
   eco weight wherever it wandered). `ComplexMob.getEcoLevelAsPrey` scores an animal more than 2×
   herd radius from its leader without the herd term; the four big-predator hunt filters and
   `PredatorRelocateGoal` use it. Relocation also checks eco now — a starving tiger beside a herd it
   was not allowed to hunt never moved.
4. **Wolves hunted bears and won every predator brawl.** Bears carry `megafauna` + `carnivore`, so
   every megafauna diet included them. `EcologyTags.isPreferredPrey` now rejects carnivore-tagged
   prey unless the diet lists `carnivore` (none do). `isThreatTo` no longer treats a passing predator
   as a threat to another predator — only one targeting it (or, in `ProtectChildrenTarget`, its young).
   `pack_hunter_damage_reduction` applies only vs prey/defenders/players, not other predators and not
   attacker-less damage (starvation). New `lone_hunter_damage_reduction` (0.3) for a solitary
   carnivore committed to a hunt, vs prey/defenders only, never stacking past the best single value.
   `AngrySleeperTarget`: returned true all night even with no one near, kept a stale target, never
   reset its 1000-tick fight timer (so it broke permanently after its first long fight), and attacked
   any predator walking past — all fixed.
5. **Packs never routed** — the Phase 1 herd-tick fix.
Plus hyenas: `kleptoparasitism_clan_size` default 4 → 3; brown/striped hyena +desert,
shortface/cave hyena +plains. **Changed DEFAULTS do not reach existing worlds** — Forge keeps the
old value in each world's `serverconfig`; new gamerules are added with their defaults.

### Phase 5 — prey-side ecology (2026-09-30, build SUCCESSFUL, not yet soak-tested)

The four causes of prey decline diagnosed on 2026-08-11 and parked until now. Test in the same
census soak as Phase 4 — they interact.
1. **Herd protection was a cliff with positive feedback** — eco used the current headcount, so each
   kill made the next easier (bison herd 28 at twenty, 13 at five). Herbivore herds now weigh in at
   `HerdEntity.getProtectiveSize()`: the headcount, or a remembered larger size that fades one member
   per `herd_memory_ticks` (1200). Carnivore packs are unaffected (Phase 4 rule).
2. **Calves were unprotected by construction** — the whole level, herd included, was ×0.3. For
   herbivore calves only the calf's own body is ×0.3 now; the herd term is ×`calf_herd_protection`
   (0.6). Bison calf: 14 in a herd of twenty (dire wolves 14 cannot, wild dogs 16 can), 8 in a herd of
   ten. A calf that strays is scored without the herd term via `getEcoLevelAsPrey`.
3. **Solitary megafauna had nothing** — rhinos, solitary sloths, moose (`herd == null`, no defender
   slots, no alert). A herd-less herbivore of 40+ max health takes
   `solitary_megafauna_damage_reduction` (0.25) less damage from carnivores, in `ComplexMob.hurt`,
   never stacking past the best single reduction.
4. **Flight manufactured its own stragglers** — every panicking animal ran to its own random point.
   `HerdFleeGoal` now gives the herd one heading (away from the threat, from the herd centre,
   `ComplexMob.fleeHeading`) and each animal paths along it (`DefaultRandomPos.getPosTowards`, falling
   back to plain away-from-threat); alarm-network neighbours of other species take their own heading.
`performRetaliation` stays **inert** by decision: herds already have defender slots, the ×2 charge
and `DefendHerdMateTarget`; free counter-hits would stack on all three.

### Phase 6 — remaining skins (2026-09-30, every skin signed off by the user in Blockbench)

- **Giraffid ×6** (okapi, samotherium, bramatherium, helladotherium, palaeotragus, sivatherium),
  on per-species rigs (`gen_bb_rig --giraffid-neck <nf> [--scale oss_*:<k>]`, all ossicone
  families built, non-species ones hidden via `scratch/fam2.js`).
- **Recolours redone as different animals:** western camel (ashy fawn + dark ridge saddle),
  steppe bison (Altamira two-tone: black cape over rufous), long-horned bison (cool umber + frosted
  cape, ivory horns), giant warthog (slate + pale mane + cream whiskers + laterite mud),
  short-faced bear (rufous, dark face mask, cream chest band), homotherium (plain sand-cream,
  lioness face structure recoloured), cave hyena (ash-grey, small spots, dark crest).
  **American lion kept as shipped** (user's call). Megatherium + megalonyx got eremotherium's
  countershade (`scripts/countershade_sloth.js`). All 12 mammoths seam-sealed
  (`scripts/mammoth_bleed.js`, 0 painted texels changed); woolly + steppe mammoth fur re-haired
  (`scripts/mammoth_fur.js`: strands, ragged alpha hem, open skirt undersides). The flatBack dome no
  longer floats (the remodel's pivot sits on the dome's base) — no Java change needed.
- **Tooling bugs found and fixed — read before any skin work:**
  1. `gen_bb_rig` resize options moved UV footprints with the geometry → skins misaligned IN GAME
     while the Blockbench preview looked right. UVs are now frozen at the Java box size
     (`uv-frozen (resized) parts: N`). 7 already-saved skins were repainted; the Aug Tier-3 deer
     antlers + maned wolf / bush dog legs are **not yet audited**.
  2. `java2bb` ignored LOCAL-variable parts (`AdvancedModelBox ear_left = new ...`): ModelBear's
     ears, teeth and tail were missing from rigs. Only ModelBear was affected.
  3. `MM.load` decoded PNGs asynchronously, so reference stamps (eyes!) could copy blank pixels —
     the western camel came out eyeless. Now a synchronous zlib PNG decoder.
  4. Paint coverage: faces now paint every texel their FRACTIONAL UV range touches, shared texels
     by priority (down < other; fringe < core) — kills bright seam slivers on pale species.
- New painter options: bisonlib `maneCol/maneAmt/maneParts/maneTop`, `strandAmt`, `clumpCol`,
  `tufts` (stamps plains' fringe cut-outs); camellib `ridgeCol/ridgeAmt`. **User's standing note:
  fur must read as hair (strands, clumps, ragged tufts), never a flat gradient or flat panel.**

### Phase 7 — mammoth animation revamp + sloth poses (2026-09-30, build SUCCESSFUL, runClient pending)

Every pose below was SOLVED numerically (scale-aware FK: Citadel `translate · rotZYX · scale`), then
shown on a posed Blockbench rig for the user's sign-off. Rest poses and the sloth are user-approved;
the mammoth action/walk key poses were built into Java after being shown.
- **Mammoth rest/sleep** (`applyRestingPose(progress, asleep)`): body_hips rp y 14.5 (belly 0.8u off
  the ground — headroom for the 6% breathing scale). At that height a folded knee cannot fit, so the
  legs take the elephant's real lying posture: forelegs forward, hind legs straight back. Tail lies on
  the ground; woolly fur flares and y-scales (setScale written EVERY frame — the model is shared).
  Sit = head up; sleep = the 1.35× tusks rest on the ground. Checked at both tusk scales.
- **Eyes (mammoth + ground sloth):** closed = eye planes `setScale(1, 0.2, 1)`, a slit, instead of
  burying them in the skull. Other models still use the bury trick.
- **Mammoth walk:** lateral-sequence gait (LH, LF, RH, RF) FITTED with scipy to the exact Citadel
  term `±(cos(f·speed+offset)·degree·amt + weight·amt)` (invert negates the weight too). The inherited
  rhino cycle dug 0.9u and slid planted hind feet FORWARD; now planted feet move back, lift ~2u,
  dig ≤0.3u (0.2 of that is the standing pose's own baseline), ≥2 feet down at all times.
- **Mammoth actions:** THREATEN = trumpet (head/trunk up, ears flared, forefoot raised) + stomp ×2;
  GORE = trunk tucked under, lunge + tusk toss; NEW `EntityMammoth.EAT` (40 ticks, the length of
  GrazeGoal's `eatingGrassTimer`): reach ground → grasp → coil trunk to mouth. Nothing below ground,
  no trunk segment through the skull. Trunk idle sway is cut to 30% while lying.
- **Ground sloth rest/sleep:** the old fold pushed shanks/forearms 0.5–0.6u through the ground and
  buried hands + claws in the chest (the forelimbs read as stumps — user). Now forearms lie forward
  on the ground, hands turned in, claws curled forward in front of the chest; hind limbs fold under;
  tail on the ground. Sit = head up; sleep = chin on the ground.

### Phase 8 — upstream TODO cleanup (2026-09-30, build SUCCESSFUL)

- **Loot tables for the 11 types that had none** (hyena, football_fish, spadefish, triggerfish,
  whale_shark, king_crab, newt, giant_salamander, snake, large_snake, spitter), in the style of the
  shipped tables: fish → cod (smelted if on fire) or tropical_fish for reef fish, reptile meat →
  `food_turtle_raw` (as monitor), snakes → `material_snake_skin`. Every type now has a table.
- **`RaidCropsGoal` fixed but still unwired (user's call):** it built a LootContext without ORIGIN/TOOL
  (block loot threw), never reset `continueTask` (one raid per lifetime), and had no give-up timer.
- **Newt `crested`:** crested_2 was a byte copy of crested_1 → collapsed to one skin (`crested.png`,
  skins 20 → 10). Existing skin indices are clamped, so saved newts are safe.
- **`NestReptileBlock`:** the creative sneak-use egg count goes to the player's action bar, not the log.
- **Removed** `GuardPositionTarget` (no callers, no logic). Opossum play-dead comment corrected: mod
  predators respect `canBeTargeted()`; only vanilla attackers re-acquire.
- **Orphan PNGs kept (user's call)** and inventoried in `docs/plans/future_species.md` as ready-made
  art for Phase 10. Skin resolution rule for reference: `skins` tens digit = common count (≥2 →
  `<sp>_N.png`, else `<sp>.png`), ones digit − 1 = rare count (`<sp>_Nr.png`); dimorphic big cats
  append `_male`/`_female`.
- The other TODOs are upstream feature wishes (climbing AI, chest raiding, egg hatching…) — left as-is.

## Models & skins (Claude Design)

Models are hand-written Citadel `AdvancedEntityModel<EntityXxx>` from `AdvancedModelBox` cubes
(Blockbench/Tabula "box" style — per-cube `setRotationPoint`, `addBox`, `setRotateAngle`, `addChild`).
No GeckoLib, no `.bbmodel`/geo-JSON in the repo. Gold-standard examples: `client/model/ModelBison.java` +
`client/render/RendererBison.java`. Atlas size (`texWidth`/`texHeight`): **64×32 for small mobs**
(`ModelHyena`), **128×64 for large mobs** (`ModelBear`, `ModelRhino`, `ModelBison`).

**Model ↔ entity contract:** `setupAnim(entity,…)` calls `animate(entity)`, which must build keyframes for
exactly the `Animation` objects the entity's `getAnimations()` declares (names + tick lengths), and read the
entity's flag getters to toggle/scale parts. A model can only be built after (or alongside) its entity class.

**Two-track split:** Track A (geometry) — author boxes in Blockbench from reference art, port to
`AdvancedEntityModel` using `ModelBison.java` as the pattern; Claude Design may emit a first-draft
`Model<Name>.java` given the spec + example, to be loaded/tested in-game. Track B (skins, Claude Design's
strength) — finish the model → export its UV template PNG (Blockbench: *File ▸ Export ▸ Texture Template*) →
paint each variant onto it at the model's `texWidth×texHeight`, named per the skin encoding above, into
`assets/untamedwilds/textures/entity/<type>/`.

### Per-animal Claude Design brief (reusable template)
For each **new entity type**, hand Claude Design a packet with:
1. **Reference:** the real animal + 2–3 photos/skeletal refs; note the diagnostic silhouette (trunk+tusks,
   antler span, domed shell, sabre fangs, hump).
2. **Target size in blocks** = the entity's hitbox from `ModEntity.createEntity(...)` `sized(x,y)` (proposed:
   mammoth ~2.6×2.6, ground_sloth ~1.6×2.2 rearing, deer ~1.4×1.8, glyptodont ~1.8×1.2, equid ~1.4×1.6,
   dire_wolf 1.0×1.0). Proportions must read correctly at that size.
3. **Base model to fork** (closest existing geometry): mammoth→`ModelRhino`/`ModelHippo`+trunk/tusks;
   ground_sloth→`ModelBear`+arms/claws; deer→`ModelBison` slimmed+antlers; glyptodont→`ModelTortoise`+tail;
   equid→`ModelCamel`/`ModelBison`+horse neck; dire_wolf→`ModelHyena`+wolfier head/tail;
   giraffid→`ModelCamel`+long neck+ossicones; antelope→`ModelBison` slimmed+horns/nose;
   toxodon→`ModelHippo`; macrauchenia→`ModelCamel`+short trunk cube.
4. **The paired entity class** (or its spec): exact `Animation` field names + tick lengths, flag getters,
   `getMobSize()`/`isBaby()` usage — attach the template entity so the contract is visible.
5. **Format constraints:** "Output `public class Model<Name> extends AdvancedEntityModel<Entity<Name>>` in the
   exact style of the attached `ModelBison.java`; set `texWidth/texHeight`; parent parts with `addChild`;
   implement `setupAnim` + `animate` for the listed animations; expose feature cubes and hide/scale them per
   the flag getters." Attach `ModelBison.java` and `RendererBison.java` as gold-standard examples.
6. **Cubs:** request a matching juvenile model only if the renderer swaps on `isBaby()` (hyena/dire_wolf do
   not — they reuse one model).

For **new variants of existing types**, no geometry — hand Claude Design the type's UV template + the new
species' reference; it paints `<species>.png` (plus `_Nr` rares if wanted).

## Full roster (target: extinct + a few living relatives that share a body plan)

Already shipped in-repo (extinct): `rhino:wooly`, `bear:cave`, `hyena:shortface`, `big_cat:cave_lion`,
`big_cat:sabertooth`, `manatee:steller`.

Added by this update — Part 1 variants: `bear:short_faced` (Arctodus), `big_cat:american_lion`,
`big_cat:homotherium` (scimitar cat), `bison:steppe`/`long_horned`/`aurochs`/`giant_buffalo`,
`rhino:elasmotherium`, `hyena:cave_hyena`, `camel:western` (Camelops), `boar:giant_warthog`; plus the
`hasSabreFangs` flag on `big_cat` (set on `sabertooth`=Smilodon and `homotherium`). New type: `dire_wolf`
(`dire_wolf` + `pleistocene_wolf`).

Planned new types (later PRs): `mammoth` (woolly/Columbian/steppe mammoth, mastodon, straight-tusked &
Cuvieronius, + living African/Asian elephants), `ground_sloth` (Megatherium/Eremotherium/Megalonyx),
`deer` (Megaloceros/Cervalces + living moose/wapiti/red deer), `glyptodont` (Glyptodon/Doedicurus),
`equid` (Equus scotti/Hippidion/tarpan), `giraffid` (giraffe/Sivatherium),
`antelope` (saiga/pronghorn/African antelope), `toxodon`, `macrauchenia`.

## Critical files (reference)

- Registration: `src/main/java/untamedwilds/init/ModEntity.java`
- Entity templates: `entity/mammal/EntityBison.java`, `EntityBear.java`, `EntityBigCat.java`,
  `EntityHyena.java`, `EntityDireWolf.java`; `entity/reptile/EntityTortoise.java`
- Base/flags/hunger: `entity/ComplexMobTerrestrial.java`, `entity/ComplexMob.java`, `entity/ISpecies.java`,
  `entity/IPackEntity.java`, `entity/INewSkins.java`
- Data/schema: `util/EntityDataHolder.java`, `util/SpeciesDataHolder.java`,
  `util/EntityUtils.java` (`buildSkinArrays`, `getSkinFromEntity`, `getSound`)
- Hunt AI: `entity/ai/target/HuntMobTarget.java`, `HuntPackMobTarget.java`
- Data: `data/untamedwilds/entities/*.json`, `spawn_tables/*.json`, `loot_tables/entities/*.json`
- Assets: `assets/untamedwilds/lang/en_us.json`, `models/item/template_spawn_egg_mammal.json`,
  `textures/entity/<type>/*.png`
- Models/renderers: `client/model/Model*.java`, `client/render/Renderer*.java`
  (gold standard: `ModelBison`/`RendererBison`, `ModelHyena`/`RendererHyena`)
