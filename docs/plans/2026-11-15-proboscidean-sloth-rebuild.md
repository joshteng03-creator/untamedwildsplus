# Proboscidean + ground sloth: full rebuild through the remodelling SOP

User decision 2026-11-15, after an assessment ("something's off but I can't pinpoint what"):
- **Full rebuild of both.**
- **Proboscideans first.**
- The mammoth spawn egg and type now display as **"Proboscidean"**. That rename is lang-only: the registry IDs `untamedwilds:mammoth` and `mammoth_spawn_egg` stay, so existing worlds keep their animals.

## Why: what the assessment found

Both were the first two remodels (July 2026). They were sculpted by eye before the remodelling SOP existed, so they never got life-size calibration, nested massing, the numerical FK / z-fight / burial checks, or the world-space painter.

Renders of the assessment are in `scratch/assess_proboscidean.png`, `assess_ground_sloth.png`, `bigview_mammoth.png` and `bigview_ground_sloth.png`.

### Proboscideans (12 species on `ModelMammoth`, 47 boxes, 256x256)

1. **Ears effectively absent.**
   - Each ear is a 1.1-thick slab flat against the head side, standing 0.4u (2.5 cm) proud.
   - On the coat species it is buried inside `fur_neck`.
   - On the rest it reads as a dark rectangle painted on the head; from the front, no species has ears.
   - The African elephant's earSize 2 makes the ear 34% of shoulder height (real: ~55%), still flat to the skull.
2. **One mammoth body for every species.**
   - Every species shares the domed skull, the shoulder hump and the sloping back.
   - All share the same tusks, curling through 185°, so even the *straight-tusked* elephant has spiral tusks.
   - Flags only vary coat, tusk size, ear size and a squashed dome/hump.
3. **Boxy build.**
   - The body is 0.60 as wide as long (elephants ~0.40-0.45), with no taper and a squared rump.
   - The legs are 12-deep x 7.6-wide planks of three stacked boxes, ending in a wider "boot".
4. **Textures.**
   - The coat fur is straight parallel streaks that read as wood grain.
   - Bare hide has horizontal bands that make the legs look like stacked blocks.
   - All 12 skins sit in one narrow grey-brown band (several pairs under dE 5); Notiomastodon is olive green.
   - The steppe mammoth and straight-tusked elephant collide (6.8).

### Ground sloth (5 species on `ModelGroundSloth`, 32 boxes, 256x256)

1. **Stair-stepped body.** It is three overlapping slabs at different tilts (hips 0°, arch -16°, chest -12°), so the outline is stair-stepped and the 3/4 view reads as a crumpled crate.
2. **Too tall.** At Megatherium x1.40 it is ~6 m long (right) but ~2.7 m at the hips (real ~2 m): length:height 2.1, where the real animal is ~3. Width:length is 0.6.
3. **Head.** It is a small bear-like box low on the chest and vanishes into the body from the front. Megatherium had a tall, narrow skull with a very deep jaw.
4. **Claws.** They are ivory-white and read as white mittens; real sloth claws were dark horn.
5. **One shape for five species.** Megalocnus (~90 kg, Caribbean) through Megatherium (~4 t) share it, and only skin and scale differ.
6. **Textures.** A per-box top-to-bottom gradient plus vertical streaks makes every box read as a separate wooden panel, and three of the five skins are flat recolours.

## Proboscidean rebuild: design

The new 256x256 rig is calibrated by **shoulder height** (1 block = 1 m).

### Body

- **Massing.**
  - A barrel narrower than now (width:length ~0.45), tapering at the shoulders and the hips, with a rounded rump.
  - The withers and topline come from a tall box buried in the barrel (SOP massing rule).
- **Legs.**
  - Straight columnar legs of near-square section: an upper segment partly inside the body, a column, and a pad foot only slightly wider.
  - Lateral faces step monotonically inboard down each leg.
- **Ears.**
  - A true flap hinged at the back of the skull, yawed out so it stands off the head and the neck fur.
  - It scales by `earSize`: 0 small (extinct species), 1 Asian, 2 African, where size 2 reaches ~55% of shoulder height and covers the shoulder.
  - Solved against the head, neck and fur planes.
- **Trunk.** Keep the 6-segment chain with 1u overlaps; the root flares into the face.
- **Eye.** The standard eye plane with a socket ridge (the crocodilian lesson: no bumps, at least 3x2 texels, 0.3u clearance from every ancestor).

### Per-species families

These are selected by flags in `mammoth.json`. Each family is a set of `showModel`-gated boxes plus per-family segment angles, built like the antelope horn families. **No shipped flag changes meaning.**

| species | head | back | tusks | coat | ears |
|---|---|---|---|---|---|
| woolly | DOME | SLOPE (hump) | SPIRAL, large | yes | 0 |
| columbian | DOME | SLOPE | SPIRAL, large | sparse (no) | 0 |
| steppe_mammoth | DOME | SLOPE | SPIRAL, large | yes | 0 |
| mastodon | FLAT | LEVEL long, low | UPCURVE, large | (no) | 0 |
| straight_tusked | CREST | LEVEL, tall | STRAIGHT, large | no | 1 |
| cuvieronius | FLAT | LEVEL | TWIST (helical) | no | 0 |
| african_elephant | SLOPE (no dome) | DIP (concave) | FORWARD | no | 2 |
| asian_elephant | TWIN dome | ARCH (mid-back high) | SMALL forward | no | 1 |
| notiomastodon | FLAT | LEVEL | UPCURVE, short | no | 0 |
| paleoloxodon_namadicus | CREST (big) | LEVEL, tall | STRAIGHT, large | no | 1 |
| pygmy_mammoth | DOME | SLOPE | SPIRAL | no | 0 |
| cretan_dwarf (Sicilian dwarf) | CREST | LEVEL | STRAIGHT, small | no | 1 |

New int flags are `headShape`, `backShape` and `tuskShape`. `flatBack` stays readable, for old saves and data packs, and maps onto FLAT/LEVEL.

### Size calibration

Shoulder height (m), approximate:

| species | m | species | m | species | m |
|---|---|---|---|---|---|
| woolly | 3.0 | columbian | 3.8 | steppe | 4.0 |
| mastodon | 2.7 | straight-tusked | 4.0 | cuvieronius | 2.3 |
| African | 3.2 | Asian | 2.75 | Notiomastodon | 2.5 |
| namadicus | 4.5 | pygmy | 1.7 | Sicilian dwarf | 1.0 |

The **grow-only rule applies**: any species whose new scale would *shrink* it is flagged to the user before shipping. The Sicilian dwarf will (current 1.26 m vs real ~1.0 m).

### Also rebuilt on the new UV

- All 12 skins: world-space MM painter, a different *treatment* per family (shaggy coat, sparse hair, wrinkled bare hide), each separated by dE >= 15 or by structure, and the steppe coat warmed off the straight-tusked.
- `mammoth/saddle.png` (SaddleLayer draws on the same model).
- The animations are carried over and re-solved: THREATEN (ears flare), GORE, EAT, walk, the solved rest/sleep poses and the eye slit.

## Ground sloth rebuild: design (after the proboscideans)

- **Body.** Continuous nested massing: a heavy, wide pelvis and hindquarters, a narrower chest, a short thick neck, and a heavy tail as the third leg of a tripod. Calibrated to ~2 m hip height for Megatherium.
- **Heads by family (flags):**
  - MEGATHERIID: tall, narrow, deep-jawed (Megatherium, Eremotherium).
  - MYLODONTID: broad square muzzle (Glossotherium).
  - MEGALONYCHID: short and blunt (Megalonyx, Megalocnus).
- **Limbs.**
  - Dark horn claws, curled.
  - Forefeet walking on the knuckles/outer edge.
  - Hind feet turned in (pedolateral).
- **Skins.** 5 repainted with the world-space painter.

## Phases (SOP order; approval gates in bold)

1. Phase 0: contract (done 2026-11-15). Animations ATTACK_THREATEN(50), ATTACK_GORE(14), EAT(40); flags hasWoollyCoat, hasLargeTusks, flatBack, earSize; SaddleLayer; hitbox 2.6 x 2.6.
2. Phase 1/1b: `scripts/proboscidean_spec.py`. FK, silhouette and size calibration per species.
3. Phase 2/3: grey massing renders of every family. **User approves the shapes.**
4. Phase 4: z-fight and burial sweeps.
5. Phase 5: UV shelf-pack and position-ramp check.
6. Phase 6: verbatim Java port (`gen_model_mammoth.py`), `verify_port` EXACT, the new flags in EntityMammoth and `mammoth.json`.
7. Phase 7: solved rest/sleep and animations.
8. Phase 8: 12 skins and the saddle, **one family at a time with approval**, then encyclopedia icons if the silhouettes change.
9. Phase 9: build, jar to Test, runClient pending.

Then the same for the ground sloth.

## Log

- 2026-11-15:
  - Phase 0 done.
  - Phase 1–3: `scripts/proboscidean_spec.py`, 57 boxes; width:length 0.42; skyline UV pack 215/256 rows.
    - The fur skirts and mane are authored with their thin axis as box depth, so they fit on the canvas.
    - Three grey passes: (1) baseline; (2) stronger back families, belly sag, mastodon/Cuvieronius tusks forward instead of handlebars, straight tusks converging; (3) barrel tops lowered to ~41 so the topline boxes alone shape the back.
  - **User approved the shapes.**
  - **User approved shrinking the Sicilian dwarf to its real 1.0 m** (grow-only exception). The mastodon (2.8 m) and Notiomastodon (2.7 m) are held at the top of their real ranges, so they don't shrink.
- 2026-11-16 — **Proboscidean rebuild DONE** (build passes, jar in Test, runClient pending).
  - **Rig:** 57 boxes. Final spec changes after grey approval:
    - The ear is authored at African size and shrunk for smaller ears (stretched texels read 3.6x coarse).
    - The croup is tilted and shortened and the rump lowered (user: the African's "butt looks like a bump up"). The fix also removed a smaller rear bump on the Asian.
  - **Port:** `scripts/gen_model_mammoth.py`, verify_port EXACT 57/57.
  - **Poses:** solved rest/sleep (drop 20.7, fore 85/-23/26, hind -71/1/-15, tail swung back 50, coat squash, head lift per head shape). Walk fitted with zero planted-foot slide by `scripts/proboscidean_walk_fit.py`. The spec's `--walk` is report-only, because it once overwrote the fit.
  - **Skins:** 12 via `scripts/paint_proboscidean.js` (coat / hairy / sparse / bare). User iterations:
    - The coat was "quite noisy", so locks became wider and longer with a low tone swing.
    - The sparse hair was speckle, so it became soft patches.
    - The Asian freckling was inverted, so it became pink on grey, confined to the face, trunk base and ear edges.
    - Namadicus was brown, so it became charcoal.
  - All 12 skins approved; the July skins are backed up in `scratch/mammoth_july_skins/`.
  - **Saddle:** regenerated by gen_saddles.py on the new parts (seat on body_back).
  - **EntityMammoth:**
    - headShape/backShape/tuskShape are synced flags, loaded from species data so old saves get their shape.
    - Pre-rebuild saves rescale Size by new/old species scale.
    - Seat height comes from the back shape (+ coat).
    - The Sicilian dwarf is not rideable (scale < 0.45).
  - **Preview pipeline traps:** Blockbench switches tabs ASYNCHRONOUSLY (a build or boot right after select() hit the wrong tab and once wiped cubes); it CACHES textures by path; and a baked resize must freeze box UVs at the authored size.
  - **Open:** encyclopedia icons still show the old tusk shapes (optional); runClient visual check.
- 2026-11-16 — Proboscidean encyclopedia icons rebuilt from the model families (gen_icons.py `--probo`, user-approved) and installed.
- 2026-11-16 — **Ground sloth Phase 1-3:**
  - Spec: `scripts/ground_sloth_spec.py`, 43 boxes, headShape 0 MEGA / 1 MYLO / 2 MEGALONYCH.
  - Construction: a single sloping topline box over a nested barrel; a world-direction tail; forefoot claws turned in; pedolateral hind feet.
  - Two grey passes:
    - The tail was authored in the rump's tilted frame, so it ran 6u underground; it is now authored in world directions.
    - The head was too small: scaled 1.25 and carried forward. The chest bottom was raised to show the forelimbs.
  - Length:hip 3.0 (the July rig was 2.1).
  - **User approved the shapes and LIFE SIZE** (all five shrink in height: Megatherium 2.9 -> 2.0 m at the hips, length ~6.5 m).
- 2026-11-16 — **Ground sloth rebuild DONE** (build passes, jar in Test, runClient pending).
  - **Port:** `scripts/gen_model_ground_sloth.py`, verify_port EXACT 43/43.
  - **Poses:**
    - Solved rest/sleep (drop 11.55, head lift per family).
    - REAR-UP threat solved: pitch 44 deg about the hip joint, root move (0, 8.8, 3.65) keeps the hip fixed, tail lifted 38 deg so its tip rests on the ground, head 3.7 m at Megatherium scale. It is done procedurally in setupAnim (exact), with claw-rake keyframes on top.
    - Walk fitted by `scripts/ground_sloth_walk_fit.py`: zero slide; the flex bias is capped at ~1, because >1 bought zero slide with a permanently bent knee.
  - **EntityGroundSloth:** headShape is a synced flag loaded from species data; pre-rebuild saves rescale Size; life-size scales in ground_sloth.json.
  - **Skins:** five via `scripts/paint_ground_sloth.js` (the calm lock coat, one world-space colour rule, dark horn claws). Glossotherium was warmed to reddish tan (it sat 8.6 from Megalocnus). All approved; the July skins are backed up in `scratch/sloth_july_skins/`.
  - **Icons:** per head family (gen_icons `--sloth`), approved and installed.
  - Both rebuilds are COMPLETE.
