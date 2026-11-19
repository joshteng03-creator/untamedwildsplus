## Untamed Wilds Plus 2.9.0

### 35 new species and 4 new kinds of animal (345 species, 55 types)
- **New kinds of animal**, each with its own model, walk, rest poses, skins, encyclopedia pages and icons:
  - **Toothed Whales:** the **orca** and the **false killer whale**. Pods hunt fish, seals and sharks together and breach. Male orcas carry the tall dorsal fin; females and false killer whales have smaller ones.
  - **Mustelids:** the **wolverine** and the **honey badger**. Small, fearless, solitary hunters that also scavenge other predators' kills.
  - **Giant Ground Birds:** the **South Island giant moa**, the **elephant bird** and ***Genyornis***. Flightless browsers that run from predators and kick when cornered. They sit on their hocks and sleep with their necks laid along the ground.
  - **Great Apes:** ***Gigantopithecus*** and the **Bornean orangutan**. Knuckle-walking fruit-eaters that threaten before they fight and feed with their hands.
- **New species on existing animals:**
  - **Proboscideans:** **Stegodon** (oriental and Flores), with long, close-set tusks that converge in front of the trunk.
  - **Tortoises:** ***Meiolania***, the giant horned turtle with a spiked club on its tail. Also the **Aldabra** and **Galápagos** giant tortoises.
  - **Ground sloths:** *Lestodon*, Harlan's ground sloth and the Shasta ground sloth.
  - **Glyptodonts:** *Panochthus*, with a shell of small tubercled plates.
  - **Bison:** the ancient bison and the helmeted musk ox.
  - **Hippos:** the European, pygmy and Malagasy dwarf hippopotamus.
  - **Horses:** the kiang and the stilt-legged horse.
  - **Canids:** the Ethiopian wolf and the golden jackal.
  - **Big cats:** the clouded leopard.
- **Art already in the mod, now in game:**
  - Five catfish: the tiger shovelnose, goonch, piraiba, Mekong giant catfish and wels.
  - The banded sea krait.
  - The crocea giant clam.
- **Renamed:** the "Dire Wolf" type is now **"Canid"**. Saved worlds, commands and spawn tables are unchanged.
- **Encyclopedia:** a new **Birds** taxon. The *Naturalist* advancement now asks for a bird as well.

### Notes for modpacks
- Four new entity types: `untamedwilds:orca`, `mustelid`, `ground_bird` and `great_ape`. Each has a spawn egg, a loot table and a spawn-table entry: orcas in `water_ocean`, mustelids in `predators`, birds and apes in `herbivores`.
- New species are appended after the existing ones, so saved animals keep their species.
- The new animals reuse sounds that are already registered:
  - orcas: vanilla dolphin;
  - mustelids: vanilla fox;
  - great apes: vanilla polar bear;
  - ground birds: the mod's crocodilian bellows and hisses, so their subtitles read "Crocodilian…".

## Untamed Wilds Plus 2.8.0

### Animals that sound like themselves
- **Real recordings:** 153 recordings, all CC0 or public domain, give 21 animal types their own voices. Another 46 clips derived from them (see below) cover the animals with none of their own. Sources are the US National Park Service, NOAA, Wikimedia Commons and CC0 uploads on Freesound. Every clip is credited in `assets/untamedwilds/sounds/CREDITS.md`.
  - Proboscideans rumble and trumpet, deer bugle and bark alarms, dire wolves howl and growl, and crocodilians hiss and bellow.
  - Whales sing, sea lions bark, bison bellow, and horses whinny and snort.
- **New kinds of sound:**
  - Alarm calls when a herd spots a predator.
  - Long-range calls from herd leaders and lone animals (bugles, howls, trumpets) that carry about 50 blocks.
  - Grazing, drinking and butchering sounds.
  - Heavy footsteps for the biggest animals.
- **Pitch follows size:** a dwarf elephant sounds higher than a mammoth.
- **Voices for animals with no free recording of their own:** these are built from the recordings above by pitch and speed changes, cutting, repeating and filtering. Nothing is synthesised, and every derived file lists its sources in `CREDITS.md`.
  - **Giraffids** hum low and snort.
  - **Tapirs** whistle and huff.
  - **Wildebeest** give the nasal "gnu" honk in bouts, and snort in alarm.
  - **Kangaroos** cluck, cough-grunt and growl when squaring up to fight or spar.
  - **Tortoises** hiss as they pull into their shells. **Softshell turtles** hiss when attacked, but never while hunting.
  - **Added hurt or death sounds:** diprotodonts, antelope, macrauchenia and large rodents.
  - Giant salamanders and newts stay silent, as the real animals almost are.
- **Config:**
  - `ambience.long_range_calls`
  - `ambience.call_interval_ticks`
  - `ambience.footstep_sounds`
  - `ambience.pitch_by_size`
- **Fixes:** cheetah and small-cat meows were silent because the sound was never registered. The manatee's broken ambient sound has been removed.

### Eating, drinking and keeping watch
- **New animations:** 19 types have new eat, drink and/or alert animations, each posed against that animal's own model. That's 17 herbivores, plus boars and proboscideans, which now drink too.
  - **Giraffids** drink the way real giraffes do: forelegs splayed wide and chest lowered between them, all four hooves planted.
  - **Proboscideans** drink with the trunk: they dip it in the water, hold while drawing water, then coil it up to the mouth, twice.
  - Eating: head down to the grass, with a cropping lift.
  - Drinking: muzzle down to the water past the bank's edge, with two swallows.
  - Alert: head up, ears forward, staring at the threat.
- **Drinking (cosmetic):** about once a day, animals walk to nearby water and drink from the bank, standing back by their own reach so the muzzle or trunk lands over the water.
  - There is no thirst stat; an animal that finds no water simply tries again later.
  - Herds remember their waterhole and come down to drink together, and animals often drink soon after waking.
  - Config: `ambience.drink_*`.
- **Keeping watch:** a herbivore that sees a predator just beyond fleeing range stops, turns to face it and watches it, and its herd calls an alarm. If the predator comes closer, the herd flees as before.
  - If the predator stays where it is, the animal relaxes after about 10 seconds (4 if it's hungry) and goes back to grazing. It looks up again 20 seconds later. A soak test caught deer starving while they stared at big cats resting just out of flight range.
  - Config: `ambience.herbivore_alert`, `ambience.alert_range_scale`, `ambience.alert_max_ticks`, `ambience.alert_relax_ticks`.

### Fixes from the first in-game look
- **Ground sloths:** a wider barrel and hips, since Megatherium's pelvis was enormous. The Megatherium and Eremotherium lower jaw no longer hangs below the skull like a sagging jowl. All five coats are now long and shaggy.
- **Woolly mammoth, steppe mammoth and mastodon:** the coat no longer tiles into neat rows of identical locks that looked like a rug. It's messy wool now: locks of every length, ragged ends, clumps and matted patches, still low-contrast. Eyes, tusks, ears and feet are unchanged.

### Carcasses
- **What leaves one:** animals killed by predators, and animals that die of hunger or old age, leave a carcass where they fell.
  - The killer and its pack eat their share as before. Bears and hyenas walk over and feed on the rest over time, with the same food limits as before.
  - The carcass darkens as it decays, then only bones are left, then they fade.
- **Butchering:** use an axe, sword or spear on a carcass (or a knife, with any mod that tags knives `forge:tools/knives`) for the animal's normal drops, once. Looting works, and the meat shrinks if scavengers have already eaten.
- **What doesn't:** animals killed by players drop loot exactly as before. There's no carcass in Zoo mode, for babies, tamed, saddled or named animals, for deaths in water or lava, or for `/kill`.
- **Display:** most animals lie on their side; deer and proboscideans lie as if asleep, so their antlers and tusks don't go through the ground.
- **Config:** `ambience.carcass_*` (lifetime, area cap, a cap per dimension, feeders) and `ambience.extra_scavengers` (more animals that feed on carcasses). The client setting `display.carcass_flies` turns off the flies.

### Performance and multiplayer
- **Dedicated-server crash fixed:** clients on a dedicated server crashed on newer animals. They never received those animals' species data, so models and skins had nothing to read. Every type's data is now sent at login and after `/reload`.
- **Animation messages:** animation starts are sent only to players who can see the animal, instead of to every player on the server.
- **Tracking distances:** animals are sent to clients from vanilla-like distances by size (10 chunks for most animals, 14 for the biggest, 4 for small fish) instead of the whole view distance. Clients never drew them further away anyway. Config: `performance.tracking_range_multiplier`.
- **Less background work:** fewer, staggered searches throughout:
  - hungry predators with nothing to hunt search less often, and back off further after each empty search;
  - lone animals looking for a mate back off after a failed wide search;
  - herd merging, crocodile basking checks, sleeping-animal checks and herd-defence checks run less often;
  - animals with a far-away home no longer load that chunk every tick;
  - herd panics spread their pathfinding over a few ticks.
- **Behaviour fix:** about half of all animals never checked for predators themselves, because of how vanilla schedules AI checks. Several other timed AI checks were skipped the same way. Every animal now runs them.
- **Shared herd scan:** a herd can look for predators with one search instead of one per animal. It finds exactly the same threats and is off by default: `performance.shared_herd_threat_scan`.
- **Config files:** new `performance.*` and `ambience.*` sections, and a client config file `untamedwilds-client.toml`.
- **Network version bumped:** client and server must both run 2.8.0.
- **Sleep fix:** an animal woken by a fight could fall straight back asleep.

### Testing
- **Automated tests:** 22 in-game tests run on a headless test server (`./gradlew runGameTestServer`, in `untamedwilds.gametest`; they are not included in the mod jar). They cover the multiplayer data sync, the AI scheduling fix, the shared scan, vigilance and habituation, drinking (incl. the mammoth trunk drink), carcasses, butchering, the animation tables and the "no hidden stat changes during a hunting blow" rule.
- **Soak harness:** `scripts/soak/` runs a dedicated server headless and records tick time and the census.

## Untamed Wilds Plus 2.7.0

### New animals
Eight new animal types and 37 new species, plus a new antelope, all modelled at life size (1 block = 1 metre at the shoulder):
- **Diprotodonts:** Diprotodon, Zygomaturus, Nototherium, Hulitherium and Maokopia. Mothers carry a joey in the pouch, and adults can be ridden with a saddle.
- **Kangaroos:** red and eastern grey kangaroos, Procoptodon, Sthenurus and Protemnodon. They hop, walk slowly on five limbs (forearms and tail) when grazing, carry joeys in the pouch, and kick. Males spar with each other.
- **Giant monitors:** Megalania and the Komodo dragon, venomous ambush predators.
- **Pinnipeds:** walrus, bearded seal, ringed seal, harp seal and Steller sea lion. They haul out in colonies on the shore, walrus bulls fight with their tusks, and harp and ringed seal pups are born in a white coat. Walruses drop ivory.
- **Wild goats and sheep:** Alpine and Siberian ibex, argali, bighorn sheep, chamois and Harrington's mountain goat. They climb cliffs, and rams clash heads. Large rams (argali, bighorn) can be ridden with a saddle. They drop horn.
- **Large rodents:** capybara, Neochoerus (giant capybara), Castoroides (giant beaver) and the North American beaver. They graze on the bank and escape by diving.
  - Birds perch on resting capybaras.
  - Beavers slap their tails to warn every rodent nearby when a predator comes close.
  - Adult beavers build small dams of logs and earth in shallow water. Dams need the vanilla `mobGriefing` gamerule.
- **Wildebeest:** blue and black wildebeest, Megalotragus (the giant wildebeest of the Late Pleistocene) and the hartebeest. Each species has its own horn shape. They graze with their heads down to the grass, and a cornered bull paws the ground and charges.
  - **Great migration:** every few in-game days a herd sets off on a long trek, about 160 blocks, and each trek turns from the last so herds loop round a circuit. With Serene Seasons, treks start when the season changes. Hungry lions, hyenas and wild dogs follow the moving herd. Config: `gamerules.wildebeest_trek_interval`, `gamerules.wildebeest_trek_distance`, `gamerules.migration_predator_follow`.
  - **Territorial bulls:** a settled bull holds a small patch, and a rival bull entering it sets off a kneeling horn clash (no damage) before the loser walks away.
  - They drop beef, leather and horn.
- **Crocodilians:** Nile, saltwater and mugger crocodiles, the black caiman, Quinkana (a long-legged land crocodile of Pleistocene Australia) and Voay (the horned crocodile of Madagascar). Each species has its own snout shape, and Voay has small horns.
  - **River ambush:** a hungry crocodile waits in the water and takes animals that are in it or drinking at its edge, lunging out at them. A bite in the water can become a death roll that holds the prey under. Quinkana hunts on land instead.
  - **Basking:** by day crocodiles lie on the bank with their mouths open, and slide back into the water when a player or predator comes close.
  - **Nests:** females lay their eggs in a reptile nest on land, and the hatchlings grow up from there.
  - They get hungry more slowly than mammal predators. They drop leather and bones.
- **Impala**, a new antelope: the rams carry long, ridged, lyre-shaped horns; the ewes are hornless.

Every new species has an Encyclopedia of Life entry with an icon, a discovery advancement, loot and spawns.

### A self-regulating ecosystem
In a long test, predators took over: hundreds of bears against a few hundred herbivores, while bison, goats, glyptodonts and tetrameryx crashed. Predator numbers now follow prey numbers.
- **Predators breed only when there is enough prey.** A predator breeds while there are at least about 30 prey per predator around it (`gamerules.predator_breed_ratio`), so predator numbers rise and fall with their prey, in cycles.
- **Realistic predator life cycles.** Predators no longer breed three times faster than their prey and have no litter bonus. A mother can't have another litter while she is rearing her cubs, and feeding them costs her extra food. Thin mothers have single cubs.
- **Cubs must eat.** After weaning, predator cubs feed at their mother's kills and can starve in lean times. Calves graze for themselves once weaned and no longer reach adulthood already starving.
- **Carcasses run out.** A kill feeds the hunter, its pack and its cubs, then at most three scavengers, from a fixed amount of food. Animals that die of other causes leave carrion the same way, but nothing eats its own kind.
- **Predators go where the prey is.** Hungry predators leave ground where prey per predator is low and look for better ground. Young solitary predators leave home when they grow up.
- **Safety net against predator die-offs.** Where a predator is the only one of its kind, it breeds more easily.
- **Slow immigration.** Now and then a small group of a locally missing species walks in from beyond the loaded area, so a local extinction is not permanent. Config: `gamerules.immigration_*`.
- **Fixes:** six antelope species (tetrameryx, blackbuck, giant eland, gemsbok, sable, bluebuck) spawned with 1 health; bison ignored the health in their data; males were locked out of mating for the whole of a pregnancy; glyptodont pairs had no protection (now: protected pairs and an armoured shell); prey recover their running stamina faster.
- **Census:** `/untamedwilds census` now reports prey per predator and writes births and deaths by cause (predation, starvation, old age) to `logs/uw_census_v2.csv`.
- Existing worlds: if your config file already has `predator_gestation_multiplier = 0.33`, `predator_maturity_multiplier = 0.33` and `predator_litter_bonus = 2`, set them to `1.0`, `1.0` and `0` to get the new behaviour.

### Climate and weather
- Climate support for Serene Seasons (optional; no hard dependency):
  - Winter slows the regrowth of grazed forage, and spring speeds it up.
  - Rain speeds up regrowth, and snowfall stops it.
  - Animals that aren't adapted to the cold get hungrier in the cold.
  - Thunderstorms make herds bunch up around their leader, and herds near a lightning strike bolt away from it.
- **Breeding is never affected by the season.** It runs all year.
- Every effect has a config option (`gamerules.climate_effects` and its neighbours).

### Drops, materials and gear
- **Hides:**
  - Every bear and big cat species now drops its own hide, each with a matching rug. Every hide crafts into leather.
  - Panther hide drops from black jaguars and leopards.
- **New materials:** horn, shark tooth, ivory, woolly fur, antler, glyptodont scute, and raw/cooked whale meat. Ivory, antler, horn and tooth also make bone meal.
- **Gear:**
  - Fur armour (immune to freezing) and carapace armour (iron-level, with knockback resistance).
  - A carapace shield.
  - Serrated shark-tooth arrows that cause a short bleed.
  - Hunting spears in every vanilla tier: longer reach and throwable, with shark-tooth and antler trims that hit the mod's animals harder.
  - The Herding Horn: tamed animals within 32 blocks follow you; sneak to make them sit.
- **Decor:**
  - The ivory block set.
  - An antler chandelier and wall trophy.
  - A taxidermy kit to mount any animal in a pose of your choice, and a display plinth.
- Loot tables added for blocks that dropped nothing (lard, pearl, reptile nest, burrow, two plants).

### Zoo and taming
- The Encyclopedia of Life covers every species, with regenerated stats and hand-drawn icons.
- Discovery advancements for each animal type, and a Field Guide.
- Mounts: equids (they can jump), elephants and mammoths (two seats), large bovids and deer (they charge) and camelids (fast on sand and snow). Saddles show on the model.
- New blocks: Feeding Trough, Species Placard and Salt Lick.

### Art
- Babies no longer render see-through: 24 cub, calf and piglet skins were painted.
- The dire wolf has a fitted walk and gallop.
- Repainted:
  - Schomburgk's deer and the Sicilian dwarf elephant.
  - The cave lion, Smilodon populator, cave bear, woolly rhino and short-faced hyena.
  - The giant buffalo's lower half.
- Body fixes:
  - Pampatherium has its low banded shell.
  - Megalocnus is island-sloth sized.
  - The thylacine has a stiff tail and stripes only on its rear half.
  - The tapir's head and bust are corrected.
- The fallow deer now has palmate antlers.
- Twelve species that were smaller than real life were raised to life size.

- **Older skins redone:** the puma was a recoloured lioness and is now painted as a real puma (plain buff coat, white muzzle and throat, black moustache marks, dark tail tip, spotted kittens). The common warthog is now grey with a dark spine mane, white jaw whiskers and facial warts instead of a brown boar coat. Both lion males' manes now have real hair: lighter around the face, darker at the back and chest, with a tufted lower edge.
- **Proboscideans rebuilt.** All twelve mammoths, mastodons, gomphotheres and elephants have a new model and new skins:
  - **Ears** are real flaps that stand off the head. Mammoth ears are small and sit in the coat; African elephants have the big fan.
  - **Each species has its own shape.**
    - Heads: the mammoths' single dome, the Asian elephant's twin dome, the African's sloping forehead, the Palaeoloxodon crest, and the mastodons' low, flat skull.
    - Backs: the mammoth hump, the African dip, the Asian arch, and a level back.
    - Tusks: spiral (mammoths), forward-curving (African), short (Asian), long and nearly straight (Palaeoloxodon), upturned (mastodons) and twisted (Cuvieronius).
  - **Body:** narrower, with straight pillar legs instead of stacked blocks. Each species is now life size at the shoulder (most grow slightly; the Sicilian dwarf elephant shrinks to its real 1 m).
  - **Skins:** mammoth coats are soft hanging locks instead of wood-grain streaks. Elephant hide has folds instead of stacked bands. African elephants are dusted with red earth, and Asian elephants have pink freckling on the trunk and face.
  - **Movement:** the walk, the lying-down and sleeping poses, the threat display and grazing were all redone for the new model. Elephants now graze with the trunk instead of dropping their heads.
  - **Riding:** the saddle and rider sit on each species' own back height. The Sicilian dwarf is too small to ride.
  - Existing animals in saved worlds keep their size and take on their species' new shape.
- **Ground sloths rebuilt.** All five ground sloths have a new model and new skins:
  - **Body:** one continuous back, high over the wide hips and sloping down to the shoulders, instead of a pile of tilted slabs. They are now life size, so they are shorter than before; Megatherium is 2 m at the hips and still about 6.5 m long.
  - **Heads by family:** the tall, narrow, deep-jawed head of Megatherium and Eremotherium, the broad square muzzle of Glossotherium, and the short, blunt head of Megalonyx and Megalocnus.
  - **Feet and claws:** they walk on the outer edges of their feet, with dark horn claws turned in.
  - **Skins:** calm shaggy coats in five distinct colours.
  - **Threat display:** they now rear up onto their hind legs and tail and rake with their claws.
  - **Movement:** the walk and the lying-down and sleeping poses were redone.
  - **Icons:** the encyclopedia icons for ground sloths and proboscideans now show each family's head shape.
- The mammoth spawn egg and type are now called **Proboscidean**, since the type also covers elephants, mastodons and gomphotheres. Internal IDs are unchanged, so existing worlds keep their animals.

### Species
- New species follow a Middle–Late Pleistocene and Holocene theme. Three species were renamed in place: same variant slots, so existing worlds are unaffected.

### Bug fixes
- Favourite foods fixed for the aardvark, manatee and spadefish.
- Bear and big cat variants that dropped the wrong hide, or none, now drop their own.
- The snow leopard hide now counts as `forge:hide` / `forge:leather`.
- Babies hatched from nests and eggs took twice as long to grow up as they should, and weren't counted in the census.

---

## UntamedWilds 2.2.0

### General Changes:
Players can now mount wild Boars and Buffalo with an empty hand, there is no point in doing this besides angering the wildlife, but there's advancements!
Added alternative advancement trigger for the root advancement, in case Patchouli is not installed
Allow UntamedWilds Spawn Eggs to work with Spawners (mobs from a Spawner will always be Male, and have no size variation, due to Minecraft limitations)
Added mob spotting functionality to the vanilla Spyglass, giving players a way to identify, and gauge the threat a mob can pose, from a safe distance (distance is configurable)
New Encyclopedia of Life version, still WIP, but all entries now have an extra page with some stats (when is the mob active, relative size, favourite food...)
Removed information that is no longer true from the Encyclopedia of Life
Poisonous Hemlock now only spawns in Forests
Added a couple trades involving Animal Fat and Blubber for the Butcher profession
Removed breaking time from some UntamedWilds flora

### Additions:
Added Whales, with 6 variants that can be rarely found in deep Oceans, they are an ambient mob, but will drop large amounts of food and fuel (you monster)
Added Junegrass, a new plant found in Plains

### Bug fixes:
Fixed Bison sleeping and sitting simultaneously, leading to a broken animation
Fixed Tetra support breaking sockets (credit to Unkownn66)
Fixed mobs not properly dropping Eggs
Fixed rare crash with missing ambient sounds
Fixed Leash duplication when turning a mob into NBT data

---

## UntamedWilds 2.1.3

### General Changes:
Snake hitboxes are slightly larger, and can be killed in one hit by Shovels
Contact aggression (Snakes and Tarantulas) triggers more consistently, but aggression lasts a very short time
Baby protection aggression will stop when you get far away from the baby
Cleaned up the Hunting AI
Removed Config options to disable mobs (they were not functional). The intended way to disable mobs is through datapacks
Added two dimension blacklist config options for Mobs and Features

### Bug fixes:
Fixed empty hands being a valid breeding Item for multiple species
(Potentially) Fixed rare crash during worldgen from Burrow generation
Fixed Cage Traps being weird if they catch an entity riding another entity (eg. Shulkers in Boats) 
Fixed Swimming AI being unable to leave water
Fixed crash when referencing undefined Sounds
Fixed crash when right-clicking mobs with certain Items
Fixed default name of some mobs being undefined (Credit to Phweeeee)

---

## UntamedWilds 2.1.2

### General Changes:
Exposed as a config parameter for second chance spawnings at a higher Y level, and lowered the default value. Fewer mobs should be spawning on trees

### Bug fixes:
Fixed Giant Clams not spawning
Fixed Manatee population explosions
Fixed Manatee glowing when eating Seagrass
Fixed most mobs not recognizing their favourite food

---
## UntamedWilds 2.1.1

### Bug fixes:
Fixed crash when using UntamedWilds on a dedicated server

---

## UntamedWilds 2.1.0

### General Changes:
Updated the mod to 1.18.2 (NOT compatible with 1.18.1, albeit upgrading worlds should be possible)
Features a WIP, and not-yet-documented data-driven system to define which mobs can spawn in each location

### Additions:
Added Manatee, with 5 variants that can be found in multiple Ocean and Jungle biomes, they are mostly an ambient mob, but will occasionally eat seagrass
Added Catfish, with 3 variants that can be found in River and Jungle biomes, again, ambience mob to make Rivers more lively, but they will eat any edible item that falls in the water
Added Yarrow, a thick, white flower that can be found in Forests

### Mobs:
Big Cat: Added a new orange skin for Lions
Bison: Male Bison will now randomly attack each other to assert dominance (they should never kill each other, however)
Sharks: Added turning animation for Sharks
Hyena: Added Laughing sounds to Spotted Hyenas
---

## UntamedWilds 2.0.0
This version has been marked as an alpha, despite being mostly stable, because it will receive neither support nor updates, since I am focusing on updating (and supporting) 1.18.2

### General Changes:
Updated the mod to 1.18.1 (NOT compatible with 1.18.2)
Removed a handful of config parameters relating to extinct mobs. Functionality can be replicated with a datapack
Many small improvements and bug fixes

### Additions:
Added Camels, with 3 variants that can be found in Deserts or any 'Wasteland' biome

### Blocks:
Cage Traps: Rearranged some events to give Cage Traps breaking animations
Burrows: Changed burrow logic to work as a randomly ticking block, rather than a ticking BlockEntity
Burrows: Allow burrows to generate underwater, given that the mob inside can survive underwater

### Mobs:
Amphibious mobs: Updated amphibious mob logic to work based on the vanilla amphibious implementation
Giant Salamander: Gave giant salamanders new textures, and improved animations
Newt: Gave newts new textures, and improved animations
Football Fish: Added some glow particles to the lure
Giant Clams: Will occasionally spawn bubble particles
Tarantula: Given Tarantula the wall climbing ability, and added a very WIP wall attaching animation
Big Cat: Added additional tail segment, and new 'fluffy tail' flag, currently used by Snow Leopards