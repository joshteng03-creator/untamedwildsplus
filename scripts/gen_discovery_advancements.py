#!/usr/bin/env python3
"""Discovery advancements (2026-10-08), all on the `untamedwilds:discovered_animal` trigger (fired when a
player watches one of the mod's animals through a spyglass, or reads it with the Field Guide):

  first_sighting  -- any animal                                        (task)
  naturalist      -- one animal of each taxon (mammal, fish, reptile,  (goal)
                     amphibian, arthropod, mollusk); a type's taxon is its
                     encyclopedia category's parent, so the two never disagree
  zoologist       -- every animal type; Minecraft lists the missing      (challenge, 100 xp)
                     ones in the tooltip, like "A Balanced Diet"

Types = every data/untamedwilds/entities/*.json (the file stem is the entity registry name).
Idempotent: rewrites the three files and the lang keys.

    python scripts/gen_discovery_advancements.py
"""
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ENT = RES / "data/untamedwilds/entities"
CAT = RES / "data/untamedwilds/patchouli_books/encyclopedia_of_life/en_us/categories"
ADV = RES / "data/untamedwilds/advancements/untamedwilds"
LANG = RES / "assets/untamedwilds/lang/en_us.json"
TAXA = ["mammal", "bird", "fish", "reptile", "amphibian", "arthropod", "mollusk"]   # bird: 2.9.0 ground birds


def taxon(typ):
    c = CAT / (typ + "_category.json")
    if not c.exists():
        # some types file their entries under another type's category (whale shark -> sharks)
        for e in (CAT.parent / "entries" / typ).glob("*.json"):
            cat = json.loads(e.read_text(encoding="utf-8-sig")).get("category", "")
            c = CAT / (cat.split(":")[-1] + ".json")
            break
    if c.exists():
        parent = json.loads(c.read_text(encoding="utf-8-sig")).get("parent", "")
        m = re.search(r"taxon_(\w+)", parent)
        if m:
            return m.group(1)
    return "mammal"


def adv(key, icon, frame, criteria, requirements, parent="untamedwilds:untamedwilds/root", xp=None, hidden=False):
    a = {"parent": parent,
         "display": {"icon": {"item": icon}, "title": {"translate": "advancements.untamedwilds.%s.title" % key},
                     "description": {"translate": "advancements.untamedwilds.%s.description" % key},
                     "frame": frame, "show_toast": True, "announce_to_chat": True, "hidden": hidden},
         "criteria": criteria, "requirements": requirements}
    if xp:
        a["rewards"] = {"experience": xp}
    (ADV / (key + ".json")).write_text(json.dumps(a, indent=2) + "\n", encoding="utf-8")


def crit(typ=None):
    c = {"trigger": "untamedwilds:discovered_animal"}
    if typ:
        c["conditions"] = {"entity_type": "untamedwilds:" + typ}
    return c


def main():
    types = sorted(f.stem for f in ENT.glob("*.json"))
    adv("first_sighting", "minecraft:spyglass", "task", {"any_animal": crit()}, [["any_animal"]])
    groups = {t: [x for x in types if taxon(x) == t] for t in TAXA}
    groups = {t: v for t, v in groups.items() if v}
    adv("naturalist", "minecraft:writable_book", "goal", {x: crit(x) for x in types if taxon(x) in groups},
        [groups[t] for t in groups], parent="untamedwilds:untamedwilds/first_sighting")
    adv("zoologist_complete", "untamedwilds:mammoth_spawn_egg", "challenge", {x: crit(x) for x in types},
        [[x] for x in types], parent="untamedwilds:untamedwilds/naturalist", xp=100)
    names = {
        "advancements.untamedwilds.first_sighting.title": "First Sighting",
        "advancements.untamedwilds.first_sighting.description": "Watch a wild animal through a Spyglass",
        "advancements.untamedwilds.naturalist.title": "Naturalist",
        "advancements.untamedwilds.naturalist.description": "Observe a mammal, a bird, a fish, a reptile, an amphibian, an arthropod and a mollusk",
        "advancements.untamedwilds.zoologist_complete.title": "Encyclopedic Knowledge",
        "advancements.untamedwilds.zoologist_complete.description": "Observe every kind of animal in the Encyclopedia of Life",
    }
    raw = LANG.read_text(encoding="utf-8")
    add = "".join("  %s: %s,\n" % (json.dumps(k), json.dumps(v)) for k, v in names.items() if json.dumps(k) not in raw)
    if add:
        m = re.search(r'  "advancements\.untamedwilds\.discovered_spitter\.description": "[^"]*",\n', raw)
        raw = raw[:m.end()] + add + raw[m.end():]
        json.loads(raw)
        LANG.write_text(raw, encoding="utf-8")
    print("types %d, taxa %s, lang +%d" % (len(types), {t: len(v) for t, v in groups.items()}, add.count("\n")))


if __name__ == "__main__":
    main()
