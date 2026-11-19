#!/usr/bin/env python3
"""Generate the Encyclopedia of Life (Patchouli) from the species data (2026-10-08).

* NEW entries for every species without one: description page (text drafted per species in
  scratch/encyclopedia_text/text_*.json), a live entity page, and a stats page.
* NEW categories for the types that had none (description from the same drafts).
* The STATS page of every entry -- old and new -- regenerated from data/untamedwilds/entities/*.json, so
  the book can never drift from the game again (the hand-written pages showed raw item ids, including a
  broken one, and their size stars followed no single rule):
    - Relative Size: 1-5 stars from the species' typical adult MASS (<20 kg, <100, <500, <2000, more).
      Hitbox volume was tried first and rejected: several types carry oversized hitboxes (a peccary
      out-ranked a red deer, a plains bison tied a minke whale). Masses: the per-species drafts plus
      scratch/encyclopedia_text/masses_existing.json for species that already had entries.
    - activity from activityType; colours from the `skins` field (tens = common, units = rare -- the same
      decoding EntityUtils.buildSkinArrays uses); favourite food by its in-game NAME; offspring; growth.
  Only a page whose text starts with "Relative Size" is ever replaced; description pages are untouched.

    python scripts/gen_encyclopedia.py            # dry run: report
    python scripts/gen_encyclopedia.py --write    # write entries / categories / stats
"""
import json
import math
import pathlib
import re
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ENT = RES / "data/untamedwilds/entities"
BOOK = RES / "data/untamedwilds/patchouli_books/encyclopedia_of_life/en_us"
LANG = RES / "assets/untamedwilds/lang/en_us.json"
TEXT = ROOT / "scratch/encyclopedia_text"
MODENTITY = ROOT / "src/main/java/untamedwilds/init/ModEntity.java"
VANILLA_JAR = pathlib.Path.home() / ".gradle/caches/forge_gradle/minecraft_repo/versions/1.18.2/client-extra.jar"

ACTIVITY = {"diurnal": "Active during Daytime", "nocturnal": "Active during Nighttime",
            "crepuscular": "Most active during Dawn/Dusk", "cathemeral": "Active at irregular hours",
            "insomniac": "Always active"}
# adult mass (kg) edges for 1..5 stars
STAR_EDGES = [20, 100, 500, 2000]
TAXON = "untamedwilds:taxon_mammal"


def load_json(p):
    return json.loads(pathlib.Path(p).read_text(encoding="utf-8-sig"))


def hitboxes():
    """registry name -> (width, height) from ModEntity.createEntity(..., "name", w, h, ...)."""
    src = MODENTITY.read_text(encoding="utf-8")
    out = {}
    for m in re.finditer(r'createEntity\(\w+::new,\s*(?:MobCategory\.\w+,\s*)?"([a-z_]+)",\s*(?:\d+,\s*\d+,\s*\w+,\s*)?([\d.]+)[Ff]?,\s*([\d.]+)[Ff]?', src):
        out[m.group(1)] = (float(m.group(2)), float(m.group(3)))
    return out


def item_names():
    names = {}
    with zipfile.ZipFile(VANILLA_JAR) as z:
        van = json.loads(z.read("assets/minecraft/lang/en_us.json").decode("utf-8"))
    for k, v in van.items():
        m = re.fullmatch(r"(?:item|block)\.minecraft\.([a-z0-9_]+)", k)
        if m:
            names["minecraft:" + m.group(1)] = v
    for k, v in load_json(LANG).items():
        m = re.fullmatch(r"(?:item|block)\.untamedwilds\.([a-z0-9_]+)", k)
        if m:
            names["untamedwilds:" + m.group(1)] = v
    return names


def stars(n):
    return "★" * n + "☆" * (5 - n)


def colours(skins):
    common, rare = skins // 10, skins % 10
    s = "%d Color%s" % (common, "" if common == 1 else "s")
    if rare:
        s += " ($(o)%d Rare Color%s$())" % (rare, "" if rare == 1 else "s")
    return s


def stats_text(typ, sp, top, mass, foods):
    n = 1 + sum(mass >= e for e in STAR_EDGES)
    vol = mass
    food = sp.get("favourite_food", top.get("favourite_food", ""))
    food_name = foods.get(food, food.split(":")[-1].replace("_", " ").title())
    act = ACTIVITY.get(sp.get("activityType", top.get("activityType", "insomniac")), "Always active")
    off = sp.get("offspring", top.get("offspring", 1))
    grow = sp.get("growing_time", top.get("growing_time", 1))
    return ("Relative Size: %s$(br2)%s$(br2)%s$(br2)Favourite Food: %s$(br2)Number of Offspring: %d$(br2)Growth time: %d Day%s"
            % (stars(n), act, colours(sp.get("skins", top.get("skins", 10))), food_name, off, grow, "" if grow == 1 else "s")), vol, n


def drafts():
    out = {"species": {}, "categories": {}, "mass": {}}
    for f in sorted(TEXT.glob("text_*.json")):
        d = load_json(f)
        out["species"].update(d.get("species", {}))
        out["categories"].update(d.get("categories", {}))
        out["mass"].update({k: v["mass_kg"] for k, v in d.get("species", {}).items()})
    m = TEXT / "masses_existing.json"
    if m.exists():
        out["mass"].update({k: v["mass_kg"] for k, v in load_json(m).items()})
    return out


def main(write):
    lang = load_json(LANG)
    boxes = hitboxes()
    foods = item_names()
    text = drafts()
    report = {"new_entries": 0, "new_categories": 0, "stats_rewritten": 0, "no_text": [], "no_box": [], "vols": []}
    for f in sorted(ENT.glob("*.json")):
        typ = f.stem
        top = load_json(f)
        box = boxes.get(typ)
        if box is None:
            report["no_box"].append(typ)
            continue
        cat = BOOK / "categories" / (typ + "_category.json")
        if not cat.exists():
            c = text["categories"].get(typ)
            if c is None:
                report["no_text"].append("category:" + typ)
            elif write:
                cat.write_text(json.dumps({"name": c["name"], "description": c["description"],
                                           "icon": "untamedwilds:textures/icon/%s/category.png" % typ, "parent": TAXON},
                                          indent=4, ensure_ascii=False) + "\n", encoding="utf-8")
                report["new_categories"] += 1
        for sp in top["species"]:
            name = sp["name"]
            mass = text["mass"].get("%s/%s" % (typ, name))
            if mass is None:
                report["no_text"].append("mass:%s/%s" % (typ, name))
                continue
            stats, vol, n = stats_text(typ, sp, top, mass, foods)
            report["vols"].append((round(math.log10(max(vol, 1e-3)), 2), n, typ + "/" + name))
            path = BOOK / "entries" / typ / (name + ".json")
            if path.exists():
                entry = load_json(path)
                changed = False
                for page in entry.get("pages", []):
                    if page.get("type") == "text" and page.get("text", "").startswith("Relative Size") and page["text"] != stats:
                        page["text"] = stats
                        changed = True
                if changed:
                    report["stats_rewritten"] += 1
                    if write:
                        path.write_text(json.dumps(entry, indent=4, ensure_ascii=False) + "\n", encoding="utf-8")
                continue
            draft = text["species"].get("%s/%s" % (typ, name))
            if draft is None:
                report["no_text"].append(typ + "/" + name)
                continue
            title = lang.get("entity.untamedwilds.%s_%s" % (typ, name), name.replace("_", " ").title())
            entry = {"name": title, "icon": "untamedwilds:textures/icon/%s/%s.png" % (typ, name),
                     "category": "untamedwilds:%s_category" % typ,
                     "pages": [{"type": "text", "title": title, "text": draft["text"]},
                               {"type": "entity", "entity": "untamedwilds:%s{Variant: %d, Size: 1}" % (typ, sp["variant"]), "text": ""},
                               {"type": "text", "title": title, "text": stats}]}
            report["new_entries"] += 1
            if write:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(json.dumps(entry, indent=4, ensure_ascii=False) + "\n", encoding="utf-8")
    return report


if __name__ == "__main__":
    r = main("--write" in sys.argv)
    vols = sorted(r.pop("vols"))
    print({k: (v if not isinstance(v, list) else len(v)) for k, v in r.items()})
    if r["no_box"]:
        print("no hitbox parsed for:", r["no_box"])
    if r["no_text"]:
        print("no draft text yet for %d:" % len(r["no_text"]), " ".join(r["no_text"][:12]), "...")
    hist = {i: sum(1 for v in vols if v[1] == i) for i in range(1, 6)}
    print("stars histogram:", hist)
    if "--report" in sys.argv:
        for v in vols:
            print(v)
