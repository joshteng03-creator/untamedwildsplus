# Future species

Working list for roadmap Phase 10 (future mob additions). **Theme (user, 2026-10-02): strictly the
Middle–Late Pleistocene and Holocene** (~774,000 years ago to today), built as a semi-realistic
predator–prey ecosystem for Ecosystem mode; Zoo mode stays the default.

## Ready-made art: entity PNGs no species uses yet

Kept on purpose (user decision, 2026-09-30). None of these load today:
`EntityUtils.buildSkinArrays` builds texture paths only from each species' `name` and `skins` in
`data/untamedwilds/entities/<type>.json`. Wiring one up means adding a species entry to that file,
plus lang keys and a spawn-table entry, and checking the art against the current model's UV layout
(most of these predate the remodels).

### Unfinished upstream species

| Type | File | Species | Notes |
|---|---|---|---|
| large_snake | `titanoboa.png` (64×32) | Titanoboa | **out of the time window** (Paleocene, ~60 Ma): do not wire |
| monitor | `komodo.png` (64×32) | Komodo dragon | lang keys already exist |
| catfish | `shovelnose.png` (64×32) | Shovelnose catfish | lang keys already exist |
| catfish | `goonch.png` (64×32) | Goonch | |
| catfish | `piraiba.png` (64×32) | Piraiba | |
| catfish | `catfish_mekong.png` (64×32) | Mekong giant catfish | |
| catfish | `catfish_wels.png` (64×32) | Wels catfish | |
| snake | `sea_krait.png` (32×32) | Sea krait | item model `snake_sea_krait` exists |
| giant_clam | `crocea.png` (64×64) | Tridacna crocea | lang keys marked `nyi_` |

### Leftovers (probably superseded)

| Type | File | Likely origin |
|---|---|---|
| bear | `skeleton.png` (128×64) | debug / carcass idea |
| big_cat | `great_lion_female.png` (128×64) | a lion species with no male skin and no JSON entry |
| boar | `peccary_old.png` (64×64) | pre-remodel peccary |
| camel | `dromedary_3.png` (128×64) | third dromedary skin; the species declares 2 |
| monitor | `bengal_3.png` (64×32) | third Bengal monitor skin; the species declares 2 + 1 rare |
| tarantula | `sling.png` (64×32) | juvenile ("sling") texture idea |

## Roster sweep (2026-10-02): species already in the mod that fall outside the window

**User decision (2026-10-02): nothing is removed or stopped from spawning.** Species from outside
the window stay in and keep spawning; where an in-window relative fits the same model and skin,
the species is renamed (display name + scientific name in `en_us.json`; the internal id stays, so
texture paths and saved animals are untouched). The original mod's fantasy mobs stay and spawn.

Never delete a species from the middle of a JSON list: variants are saved as integer indices
(upstream TODO "Move Variants from Int to String"), so it would reassign every saved animal after it.

**Renamed (done 2026-10-02):**

| Internal id | Was | Now | Other changes |
|---|---|---|---|
| giraffid `sivatherium` | *Sivatherium giganteum* (gone by ~1 Ma) | *Sivatherium maurusium* (Africa, into the Middle Pleistocene) | none; display name stays "Sivatherium" |
| deer `eucladoceros` | *Eucladoceros ctenoides* (Early Pleistocene) | **Schomburgk's deer**, *Rucervus schomburgki* (Holocene, extinct 1938) | resized to the real animal: scale 0.95 → 0.65, health 38 → 30, attack 5 → 4, herd 8 → 6, antlerScale 1037 → 800; spawns plains + swamp (was plains + forest). Same many-tined branched antlers, so no model or skin change. |
| mammoth `cretan_dwarf` | *Mammuthus creticus* (Early Pleistocene) | **Sicilian dwarf elephant**, *Palaeoloxodon falconeri* (Middle–Late Pleistocene) | none; it was already sized as the smallest dwarf (scale 0.45) |

*Sinomegaceros* was the first idea for `eucladoceros`, but it had palmate antlers: switching the
`palmate` flag changes the antler boxes and would need skin work.

**Kept as they are and still spawning (no in-window relative fits the same body):**
`thylacosmilus` (Miocene–Pliocene), `kubanochoerus` (Miocene), `samotherium`, `bramatherium`,
`helladotherium`, `palaeotragus` (Miocene giraffids).

**Original mod's fantasy mobs, kept and spawning:** blind cave bear (`bear` `blind`, cave biomes;
`EntityBear.finalizeSpawn` also turns any bear spawning below y 53 into one) and the spitter (lush
caves). Both come from the `underground` spawn table, placed in lush and dripstone caves.

**Borderline** (Early Pleistocene species that survived into the start of the window, about
0.8–0.4 Ma), kept: `arctotherium` (*A. angustidens*), `pelorovis` (*P. oldowayensis*), `xenocyon`,
`shortface` hyena (*Pachycrocuta*), `titanotylopus`, `giant_warthog` (*Metridiochoerus*).

**Wildebeest is not in the mod** (checked 2026-10-02): no entity, species entry, model or skin. Only
stale upstream Chinese lang keys (`entity.untamedwilds.wildebeest*` in `zh_cn.json`) and a code
comment remain, which is why it can look implemented.

## Where the food web has holes

Land carnivores are only four types (bear, big_cat, dire_wolf, hyena) plus snakes and monitors that
take `small_game`. By region:

- **Sahul (Australia/New Guinea): broken.** Thylacoleo and the thylacine have almost nothing to eat;
  the only large herbivore is *Palorchestes*. No Diprotodon, no kangaroos, no Megalania.
- **Rivers everywhere:** no crocodilians at all, so the river-crossing ambush, the classic hazard for
  migrating herds, is missing on every continent.
- **Mountains:** no wild goats or sheep, so the snow leopard has no natural prey.
- **Arctic coast and ocean:** no seals or walrus, so the polar bear (and great white shark) has no
  natural prey.
- **Africa:** no wildebeest, the defining migratory grazer for its lions, hyenas and wild dogs.
- **Mid-size prey everywhere:** almost nothing between a tortoise and a deer, so coyotes, jackals,
  cheetahs and dholes have little that suits them.
- **South America:** no capybara, the jaguar and anaconda staple.
- **North America:** its own sabertooth (*Smilodon fatalis*) is missing; only the South American
  *S. populator* is in.

## Recommended additions (all in-window, none already in the mod)

### Priority: each one closes a hole above

| # | Creature | Fills | Build |
|---|---|---|---|
| 1 | **Diprotodon** (*D. optatum*) | Sahul's missing megaherbivore; prey for Thylacoleo | Tier 1: new body |
| 2 | **Kangaroos**: *Procoptodon goliah*, *Protemnodon*, red kangaroo, eastern grey | Sahul's main grazers; prey for thylacine and Thylacoleo | Tier 1: first hopping body |
| 3 | **Megalania** (*Varanus priscus*) | Sahul apex reptile | Tier 2: scaled monitor |
| 4 | **Crocodilians**: Nile crocodile, saltwater crocodile, mugger, black caiman, *Quinkana* (Sahul), *Voay* (Madagascar) | river ambush predator on every continent | Tier 1: one crocodilian body, many species |
| 5 | **Pinnipeds**: walrus, bearded seal, ringed seal, Steller sea lion | polar bear, orca and great white prey | Tier 1: one pinniped body |
| 6 | **Wildebeest**, plus *Megalotragus* (giant wildebeest, Late Pleistocene), hartebeest, impala | Africa's migratory grazers | Tier 2: antelope body + new horn shapes |
| 7 | **Wild goats and sheep**: Alpine ibex, Siberian ibex, argali, bighorn, chamois, *Oreamnos harringtoni* (Harrington's mountain goat) | mountain prey for the snow leopard and wolves | Tier 2: antelope body + curled/backswept horn families |
| 8 | **Large rodents**: capybara, *Neochoerus* (giant capybara), *Castoroides* (giant beaver) | South American and North American wetland prey | Tier 1: one rodent body (beaver tail as a flag) |
| 9 | **Smilodon fatalis** | North America's own sabertooth | Tier 3: big_cat species |

### Cheap completions (Tier 3: new species on an existing body, no model work)

| Body | Species | Fills |
|---|---|---|
| deer | chital, sambar | tiger and dhole staple prey (Asia) |
| ground_sloth | *Glossotherium*, *Lestodon*, *Paramylodon*, *Nothrotheriops* | the grazing sloths of both Americas |
| glyptodont | *Glyptotherium* (North America), *Panochthus* | North America has no glyptodont |
| boar | *Platygonus* (flat-headed peccary) | North American mid-size prey |
| bison | *Bootherium* (helmeted musk ox), *Bison antiquus* | North American grazers |
| hippo | *Hippopotamus antiquus* (European hippo), pygmy hippo, Malagasy dwarf hippo | the hippo type has a single species |
| equid | *Haringtonhippus* (stilt-legged horse), kiang | North American and Tibetan horses |
| dire_wolf | Ethiopian wolf, golden jackal | small canids for small prey |
| big_cat | clouded leopard | South-East Asian forests |

### Later: bigger builds with a narrower payoff

| Creature | Note |
|---|---|
| **Stegodon** (*S. orientalis*, *S. florensis*) | Asia's long-tusked proboscidean; mammoth body + tusk flag (Tier 2) |
| **Ground birds**: moa, *Aepyornis* (elephant bird), *Genyornis* | island and Sahul herbivores; first bird body (Tier 1) |
| **Orca** | marine apex predator that pairs with the pinnipeds; toothed-whale body (Tier 1) |
| **Komodo dragon** | Flores; art already in the repo (monitor species) |
| **Wolverine** | Ice Age scavenger/mesopredator (new small body) |
| **Gigantopithecus** | Middle Pleistocene great ape (to ~215 ka); primate body |
| **Giant tortoises**: *Megalochelys*, *Meiolania* | tortoise species, scaled |

### Removed from the earlier list (outside the window or already in the mod)

- **Outside the window:** *Platybelodon*, *Deinotherium*, *Gomphotherium*, entelodont/*Daeodon*,
  *Aepycamelus*, *Teleoceras*, *Hyaenodon*, *Amphicyon*, megalodon, chalicotheres, *Paraceratherium*,
  *Megacerops*, *Titanis*/phorusrhacids, *Hipparion*, *Chilotherium*, *Agriotherium*, *Dinofelis*,
  *Xenosmilus*, *Megantereon*, *Chasmaporthetes*, *Trigodon*, *Promacrauchenia*, southern mammoth
  (*M. meridionalis*), *Hippopotamus gorgops*, titanoboa.
- **Already in the mod:** *Thylacoleo* (big_cat `marsupial_lion`), *Pachycrocuta* (hyena
  `shortface`), *Equus scotti* (equid `western_horse`).
- **Folded into the roster sweep instead:** the Sicilian dwarf elephant (now `cretan_dwarf`).
  *Sinomegaceros* stays a possible new species (it needs the palmate antler skin work).

### What every pick goes through
1. Ecology tags + diet so it slots into the food web; re-run the eco-level check from CLAUDE.md.
2. Tier 1: the full remodelling SOP (spec + FK, Blockbench massing, z-fight sweep, UV, verbatim port,
   solved poses). Tier 2: the flag is solved against the rig like the earlier toggles.
3. Every model and skin signed off in Blockbench before the next one starts.
4. A census soak after each new predator or large herbivore, so the Phase 4–5 balance still holds.
