# Future species

Working list for roadmap Phase 10 (future mob additions). The full candidate list is rebuilt in
Phase 10; this file starts with the art that already exists in the repo.

## Ready-made art: entity PNGs no species uses yet

Kept on purpose (user decision, 2026-09-30). None of these load today:
`EntityUtils.buildSkinArrays` builds texture paths only from each species' `name` and `skins` in
`data/untamedwilds/entities/<type>.json`. Wiring one up means adding a species entry to that file,
plus lang keys and a spawn-table entry, and checking the art against the current model's UV layout
(most of these predate the remodels).

### Unfinished upstream species

| Type | File | Species | Notes |
|---|---|---|---|
| large_snake | `titanoboa.png` (64×32) | Titanoboa | lang keys already exist |
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

## Candidate new body types

To be filled in during Phase 10 (see the roadmap in `2026-09-30-status-audit-and-roadmap.md`).
