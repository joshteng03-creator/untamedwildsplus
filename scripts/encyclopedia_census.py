#!/usr/bin/env python3
"""Which species have no Encyclopedia of Life entry / icon / category (2026-10-08).

Walks data/untamedwilds/entities/*.json (every type and its species list, in variant order) against the
Patchouli book (categories/<type>_category.json, entries/<type>/<species>.json) and the entry icons
(textures/icon/<type>/<species>.png).

    python scripts/encyclopedia_census.py
"""
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources"
ENT = ROOT / "data/untamedwilds/entities"
BOOK = ROOT / "data/untamedwilds/patchouli_books/encyclopedia_of_life/en_us"
ICON = ROOT / "assets/untamedwilds/textures/icon"


def species_by_type():
    out = {}
    for f in sorted(ENT.glob("*.json")):
        d = json.loads(f.read_text(encoding="utf-8-sig"))
        sp = d.get("species") or d.get("species_data") or []
        out[f.stem] = [s["name"] for s in sp]
    return out


def main():
    total = missing_entry = missing_icon = 0
    no_cat = []
    rows = []
    for typ, names in species_by_type().items():
        cat = (BOOK / "categories" / (typ + "_category.json")).exists() or any(
            json.loads(e.read_text(encoding="utf-8-sig")).get("category") for e in (BOOK / "entries" / typ).glob("*.json"))
        if not cat:
            no_cat.append(typ)
        for n in names:
            total += 1
            ep = BOOK / "entries" / typ / (n + ".json")
            e = ep.exists()
            i = (ICON / typ / (n + ".png")).exists()
            if e:   # an entry may point at an icon elsewhere (whale shark -> icon/shark/whale.png)
                icon = json.loads(ep.read_text(encoding="utf-8-sig")).get("icon", "")
                i = i or (ROOT / "assets" / icon.replace(":", "/", 1)).exists()
            missing_entry += not e
            missing_icon += not i
            if not (e and i):
                rows.append((typ, n, e, i))
    print("species: %d   missing entry: %d   missing icon: %d" % (total, missing_entry, missing_icon))
    print("types with no category (%d): %s" % (len(no_cat), ", ".join(no_cat)))
    cur = None
    for typ, n, e, i in rows:
        if typ != cur:
            print("\n%s:" % typ, end="")
            cur = typ
        print(" %s%s%s" % (n, "" if e else "[E]", "" if i else "[I]"), end="")
    print()


if __name__ == "__main__":
    main()
