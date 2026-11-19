#!/usr/bin/env python3
"""Install the staged CC0 / public-domain voices (scratch/audio_stage, built by process.py) into the mod (2.8.0, P6).

  assets/untamedwilds/sounds/<type>/<key>_<n>.ogg     copies (never overwrites an existing file)
  assets/untamedwilds/sounds.json                     entity.<type>.<key> events; calls stream if long and carry 48 blocks
  assets/untamedwilds/sounds/CREDITS.md + credits.json  every clip's source, author, licence and processing
  data/untamedwilds/entities/<type>.json               type-level "sounds" keys pointed at the new events (keys with no
                                                       clip keep their current vanilla sound); "step" for the big animals
  assets/untamedwilds/lang/en_us.json                  subtitles
  init/ModSounds.java                                  the // <gen:voices> registration block

Idempotent. Run with plain `python` (needs soundfile for durations):  python scripts/audio/install_voices.py
"""
import collections
import hashlib
import json
import pathlib
import re
import shutil

ROOT = pathlib.Path(__file__).resolve().parents[2]
STAGE = ROOT / "scratch" / "audio_stage"
RES = ROOT / "src" / "main" / "resources"
SOUNDS = RES / "assets" / "untamedwilds" / "sounds"
SOUNDS_JSON = RES / "assets" / "untamedwilds" / "sounds.json"
LANG = RES / "assets" / "untamedwilds" / "lang" / "en_us.json"
ENTITIES = RES / "data" / "untamedwilds" / "entities"
MODSOUNDS = ROOT / "src" / "main" / "java" / "untamedwilds" / "init" / "ModSounds.java"

SHARED = {"grazer_eat": "entity.grazer.eat", "animal_drink": "entity.animal.drink",
          "megafauna_step": "entity.megafauna.step", "carcass_butcher": "entity.carcass.butcher"}
STEP_TYPES = ["mammoth", "rhino", "ground_sloth", "giraffid", "toxodon", "glyptodont", "diprotodont", "bison"]

VERB = {"ambient": "grunts", "hurt": "hurts", "death": "dies", "threat": "threatens", "alarm": "sounds an alarm", "call": "calls"}
SUBTITLE = {
    ("deer", "ambient"): "Deer bleats", ("deer", "call"): "Stag bugles", ("deer", "alarm"): "Deer barks an alarm",
    ("dire_wolf", "ambient"): "Wolf whines", ("dire_wolf", "call"): "Wolf howls", ("dire_wolf", "threat"): "Wolf growls",
    ("dire_wolf", "alarm"): "Wolf barks",
    ("mammoth", "ambient"): "Proboscidean rumbles", ("mammoth", "call"): "Proboscidean trumpets",
    ("mammoth", "threat"): "Proboscidean trumpets in anger", ("mammoth", "alarm"): "Proboscidean trumpets an alarm",
    ("equid", "ambient"): "Horse snorts", ("equid", "call"): "Horse whinnies", ("equid", "alarm"): "Horse snorts in alarm",
    ("crocodilian", "ambient"): "Crocodilian grumbles", ("crocodilian", "call"): "Crocodilian bellows",
    ("crocodilian", "threat"): "Crocodilian hisses",
    ("baleen_whale", "ambient"): "Whale blows", ("baleen_whale", "call"): "Whale sings",
    ("pinniped", "ambient"): "Pinniped grunts", ("pinniped", "call"): "Pinniped barks", ("pinniped", "threat"): "Pinniped roars",
    ("giant_monitor", "threat"): "Giant monitor hisses", ("monitor", "threat"): "Monitor hisses",
    ("bison", "call"): "Bison bellows", ("rhino", "ambient"): "Rhino snorts", ("rhino", "alarm"): "Rhino puffs an alarm",
    ("caprine", "ambient"): "Goat bleats", ("caprine", "alarm"): "Goat whistles an alarm",
    ("antelope", "ambient"): "Antelope grunts", ("antelope", "alarm"): "Antelope snorts an alarm",
    ("macrauchenia", "alarm"): "Macrauchenia calls an alarm", ("macrauchenia", "ambient"): "Macrauchenia hums",
    ("kangaroo", "alarm"): "Kangaroo thumps", ("large_rodent", "alarm"): "Capybara barks an alarm",
    ("large_rodent", "ambient"): "Capybara chirps",
    ("ground_sloth", "ambient"): "Ground sloth huffs", ("glyptodont", "ambient"): "Glyptodont snuffles",
    ("tortoise", "ambient"): "Tortoise grunts", ("toxodon", "ambient"): "Toxodon grunts", ("diprotodont", "ambient"): "Diprotodont grunts",
    # derived voices (derive_voices.py) for the animals with no recording of their own
    ("giraffid", "ambient"): "Giraffe hums", ("giraffid", "alarm"): "Giraffe snorts an alarm",
    ("tapir", "call"): "Tapir whistles", ("tapir", "ambient"): "Tapir huffs",
    ("wildebeest", "call"): "Wildebeest honks", ("wildebeest", "ambient"): "Wildebeest grunts",
    ("wildebeest", "alarm"): "Wildebeest snorts an alarm",
    ("softshell_turtle", "threat"): "Turtle hisses", ("tortoise", "threat"): "Tortoise hisses",
    ("kangaroo", "ambient"): "Kangaroo clucks", ("kangaroo", "threat"): "Kangaroo growls",
    ("shared", "grazer_eat"): "Animal grazes", ("shared", "animal_drink"): "Animal drinks",
    ("shared", "megafauna_step"): "Heavy footsteps", ("shared", "carcass_butcher"): "Carcass butchered",
}


def load_json(p):
    return json.loads(p.read_text(encoding="utf-8"), object_pairs_hook=collections.OrderedDict)


def duration(path):
    try:
        import soundfile
        return soundfile.info(str(path)).duration
    except Exception:
        return 0.0


def main():
    credits = json.loads((STAGE / "credits.json").read_text(encoding="utf-8"))
    by_event = collections.OrderedDict()
    installed = []
    for c in credits:
        src = STAGE / c["file"]
        typ, key = c["type"], c["key"]
        if typ == "shared":
            event = SHARED[key]
            dest_rel = f"shared/{pathlib.Path(c['file']).name}"
        else:
            event = f"entity.{typ}.{key}"
            dest_rel = f"{typ}/{pathlib.Path(c['file']).name}"
        dest = SOUNDS / dest_rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        if dest.exists() and hashlib.sha256(dest.read_bytes()).hexdigest() != hashlib.sha256(src.read_bytes()).hexdigest():
            raise SystemExit(f"refusing to overwrite a different existing file: {dest}")
        shutil.copyfile(src, dest)
        dur = duration(dest)
        entry = {"name": "untamedwilds:" + dest_rel[:-4]}
        if dur > 5.0:
            entry["stream"] = True
        if key == "call":
            entry["attenuation_distance"] = 48
        by_event.setdefault((typ, key, event), []).append(entry)
        installed.append(dict(c, installed=f"assets/untamedwilds/sounds/{dest_rel}",
                              sha256=hashlib.sha256(dest.read_bytes()).hexdigest(), seconds=round(dur, 2)))

    # sounds.json -- merge (an existing event keeps its sounds; ours are appended)
    sj = load_json(SOUNDS_JSON)
    new_events = []
    for (typ, key, event), entries in by_event.items():
        sub = "untamedwilds.subtitles." + ("shared." + key if typ == "shared" else f"{typ}.{key}")
        if event in sj:
            have = {s if isinstance(s, str) else s.get("name") for s in sj[event].get("sounds", [])}
            sj[event]["sounds"] = sj[event].get("sounds", []) + [e if len(e) > 1 else e["name"] for e in entries if e["name"] not in have]
        else:
            sj[event] = {"sounds": [e if len(e) > 1 else e["name"] for e in entries], "subtitle": sub}
            new_events.append(event)
    SOUNDS_JSON.write_text(json.dumps(sj, indent="	", ensure_ascii=False) + "\n", encoding="utf-8")

    # ModSounds registration block
    ms = MODSOUNDS.read_text(encoding="utf-8")
    registered = set(re.findall(r'registerSound\("([^"]+)"\)', re.sub(r"// <gen:voices>.*?// </gen:voices>", "", ms, flags=re.S)))
    to_register = sorted(e for e in {ev for (_, _, ev) in by_event} if e not in registered)
    fields = []
    for key, ev in SHARED.items():
        if ev in to_register:
            fields.append(f'    public static final SoundEvent {ev.upper().replace(".", "_")} = registerSound("{ev}");')
    plain = [e for e in to_register if e not in SHARED.values()]
    block = ["    // <gen:voices> 2.8.0 CC0 / public-domain voices (scripts/audio/install_voices.py; credits in sounds/CREDITS.md)"]
    block += fields
    block.append("    public static final String[] VOICE_EVENTS = {")
    for i in range(0, len(plain), 4):
        block.append("            " + ", ".join(f'"{e}"' for e in plain[i:i + 4]) + ("," if i + 4 < len(plain) else ""))
    block.append("    };")
    block.append("    static {")
    block.append("        for (String event : VOICE_EVENTS) {")
    block.append("            registerSound(event);")
    block.append("        }")
    block.append("    }")
    block.append("    // </gen:voices>")
    gen = "\n".join(block)
    if "// <gen:voices>" in ms:
        ms = re.sub(r"[ \t]*// <gen:voices>.*?// </gen:voices>", gen, ms, flags=re.S)
    else:
        anchor = "    private static SoundEvent registerSound(String soundName) {"
        ms = ms.replace(anchor, gen + "\n\n" + anchor, 1)
    MODSOUNDS.write_text(ms, encoding="utf-8")

    # entity JSON: type-level sounds -> the new events (only keys we have a clip for); step for the big animals
    per_type = collections.defaultdict(set)
    for (typ, key, _) in by_event:
        if typ != "shared":
            per_type[typ].add(key)
    for typ, keys in sorted(per_type.items()):
        p = ENTITIES / f"{typ}.json"
        text = p.read_text(encoding="utf-8")
        d = load_json(p)
        sounds = collections.OrderedDict(d.get("sounds") or {})
        for key in ("ambient", "hurt", "death", "threat", "alarm", "call"):
            if key in keys and not (typ == "bison" and key == "ambient"):
                sounds[key] = f"untamedwilds:entity.{typ}.{key}"
        if typ in STEP_TYPES:
            sounds["step"] = "untamedwilds:entity.megafauna.step"
        block = '"sounds":{\n' + ",\n".join(f'    "{k}":"{v}"' for k, v in sounds.items()) + "\n  }"
        m = re.search(r'"sounds"\s*:\s*\{[^{}]*\}', text)
        if m:
            text = text[:m.start()] + block + text[m.end():]
        else:
            m = re.search(r'("name"\s*:\s*"[^"]+"\s*,)', text)
            text = text[:m.end()] + "\n  " + block + "," + text[m.end():]
        json.loads(text)   # still valid
        p.write_text(text, encoding="utf-8")
    for typ in STEP_TYPES:
        if typ in per_type:
            continue
        p = ENTITIES / f"{typ}.json"
        text = p.read_text(encoding="utf-8")
        d = load_json(p)
        sounds = collections.OrderedDict(d.get("sounds") or {})
        sounds["step"] = "untamedwilds:entity.megafauna.step"
        block = '"sounds":{\n' + ",\n".join(f'    "{k}":"{v}"' for k, v in sounds.items()) + "\n  }"
        m = re.search(r'"sounds"\s*:\s*\{[^{}]*\}', text)
        text = text[:m.start()] + block + text[m.end():]
        json.loads(text)
        p.write_text(text, encoding="utf-8")

    # subtitles
    lang = load_json(LANG)
    for (typ, key, event) in by_event:
        sub = "untamedwilds.subtitles." + ("shared." + key if typ == "shared" else f"{typ}.{key}")
        if sub in lang:
            continue
        if (typ, key) in SUBTITLE:
            lang[sub] = SUBTITLE[(typ, key)]
        else:
            base = lang.get(f"entity.untamedwilds.{typ}", typ.replace("_", " ").title())
            lang[sub] = f"{base} {VERB.get(key, 'calls')}"
    LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    # credits
    (SOUNDS / "credits.json").write_text(json.dumps(installed, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    legacy = sorted(str(p.relative_to(SOUNDS)).replace("\\", "/") for p in SOUNDS.rglob("*.ogg")
                    if str(p.relative_to(SOUNDS)).replace("\\", "/") not in {i["installed"][len("assets/untamedwilds/sounds/"):] for i in installed})
    md = ["# Untamed Wilds sound credits", "",
          "Every sound added in 2.8.0 comes from a real animal recording released under **CC0 1.0** or in the **public domain**",
          "(works of US federal agencies such as the National Park Service and NOAA). Each was trimmed, filtered, converted to",
          "mono Ogg Vorbis and normalised (`scripts/audio/process.py`). Licence evidence for every source was saved at download.",
          "Where an animal had no CC0 / public-domain recording of its own (giraffids, tapirs, wildebeest, turtles, kangaroos and",
          "a few single sounds), its voice is DERIVED from these same recordings of other animals -- pitch and speed shifts,",
          "a falling glide, cutting, repeating and filtering (`scripts/audio/derive_voices.py`); nothing is synthesised. Those",
          "rows say 'Derived from' and list every source clip, so the licence stays CC0 / public domain.",
          "Giant salamanders and newts stay silent: the real animals barely make a sound.", "",
          "| File | Source | Author | Licence | Link |", "|---|---|---|---|---|"]
    for i in installed:
        md.append(f"| `{i['installed'][len('assets/untamedwilds/sounds/'):]}` | {i.get('source_title', '').replace('|', '/')} ({i['source']}) "
                  f"| {i.get('author', '').replace('|', '/')} | {i['licence']} | {i.get('source_url', '')} |")
    md += ["", "## Earlier sounds (shipped before 2.8.0, from the original Untamed Wilds mod)", ""]
    md += [f"- `{x}`" for x in legacy]
    (SOUNDS / "CREDITS.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    total = sum(p.stat().st_size for p in SOUNDS.rglob("*.ogg"))
    print(f"installed {len(installed)} clips, {len(new_events)} new events, sounds folder {total / 1e6:.2f} MB")


if __name__ == "__main__":
    main()
