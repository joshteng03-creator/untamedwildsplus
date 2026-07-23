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

**Where things stand (2026-07-20):**
- You are on branch **`ice-age-megafauna`**, which is **committed and pushed** to
  `origin` (`github.com/joshteng03-creator/untamedwildsplus`) — local and remote are in sync
  (verify with `git status` → "up to date with 'origin/ice-age-megafauna'").
- **All 10 new entity types are already implemented and wired** end-to-end: `mammoth, deer,
  ground_sloth, glyptodont, dire_wolf, equid, giraffid, antelope, toxodon, macrauchenia`. Each has an
  `Entity*.java`, `Model*.java`, `Renderer*.java`, full `ModEntity` registration (all three hooks), a
  species JSON, a loot table, and spawn-table entries. Part-1 variants, the `hasSabreFangs` flag, and the
  predator-balance work are also in. See the Progress log below for specifics.
- **Two things are deliberately placeholder:** (1) every new texture is a stub PNG, and (2) the new-type
  **geometry** is a fork of an existing body (bison/hyena/etc.) with feature cubes bolted on — not yet
  sculpted to the real animal.

**The remaining work ("the rest of the mod creation process"), in recommended order:**
1. **Verify it compiles first.** Run `./gradlew build` from the repo root, then `./gradlew runClient`
   for an in-game smoke test. The new-type code was AI-generated and verified *statically only* (see
   caveats below), so a real compile may surface issues — fix those before investing in art/geometry.
   Smoke test per type: obtain the `<type>_spawn_egg`, `/summon untamedwilds:<type>`, confirm it renders
   with no missing-texture (pink/black) or console errors, spawns in its biomes, drops loot, breeds, and
   that predators only hunt when hungry (Part 3).
2. **Resculpt geometry in Blockbench (Track A)** — use the Blockbench MCP connection (next section) to
   shape each new type to its real silhouette, then translate the geometry into the matching
   `client/model/Model<Name>.java`. Priority: `mammoth` (currently a bison body + trunk/tusks) and
   `dire_wolf` (a hyena body), then the 8 herbivores (all bison-body forks).
3. **Paint real skins (Track B, Claude Design)** — replace the placeholder PNGs. See "Models & skins".
4. **Open a PR** from `ice-age-megafauna` against `1.18.2` once it builds and looks right.

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

`entity/ai/target/HuntMobTarget.java::canUse()` refuses to hunt while `getHunger() > threshold` and sets
`huntingCooldown = 6000` ticks on start. Hunger is an int capped at 200 (`ComplexMobTerrestrial.addHunger`).
- All predators pass `threshold = 30` (only hunt when hungry). The no-threshold constructor defaults to 200
  (always hunts); `EntityMonitor` was fixed to 30. (`EntityGiantSalamander`/`EntityFootballFish` remain
  intentionally opportunistic aquatic ambushers.)
- `ComplexMobTerrestrial.satiateFromKill(Entity)` restores +120 hunger when a predator lands a killing blow,
  called from each predator's `doHurtTarget`. This stops predators from thinning whole herds one kill per
  cooldown. Canids reuse `HuntPackMobTarget` (shared pack target) so a pack takes one animal, not one each.
  Do not strip `SmartAvoidGoal`/`ProtectChildrenTarget` from herbivore templates.

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
