#!/usr/bin/env python3
"""Static checks for the mod's sounds (2.8.0). Exit code 1 on any failure.

    python scripts/validate/validate_sounds.py
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
SOUNDS_DIR = RES / "assets/untamedwilds/sounds"
ALLOWED = ("cc0", "public domain", "creative commons 0")
BUDGET_MB = 8.0

errors, warnings = [], []


def err(msg):
    errors.append(msg)


def main():
    ms = (ROOT / "src/main/java/untamedwilds/init/ModSounds.java").read_text(encoding="utf-8")
    registered = set(re.findall(r'registerSound\("([^"]+)"\)', ms))
    m = re.search(r"VOICE_EVENTS = \{(.*?)\};", ms, re.S)
    if m:
        registered |= set(re.findall(r'"([^"]+)"', m.group(1)))
    sj = json.loads((RES / "assets/untamedwilds/sounds.json").read_text(encoding="utf-8"))
    lang = json.loads((RES / "assets/untamedwilds/lang/en_us.json").read_text(encoding="utf-8"))

    # sounds.json <-> ModSounds
    for event, body in sj.items():
        if event not in registered:
            warnings.append(f"sounds.json event {event} is not registered in ModSounds (only playable by id from vanilla code)")
        sub = body.get("subtitle")
        if sub and sub not in lang:
            err(f"{event}: subtitle key {sub} missing from en_us.json")
        for s in body.get("sounds", []):
            name = s if isinstance(s, str) else s.get("name")
            if name.startswith("untamedwilds:"):
                path = SOUNDS_DIR / (name.split(":", 1)[1] + ".ogg")
                if not path.exists():
                    err(f"{event}: missing file {path.relative_to(ROOT)}")
    for event in registered:
        if event not in sj:
            err(f"ModSounds registers {event} but sounds.json has no entry for it")

    # entity JSON ids
    for f in sorted((RES / "data/untamedwilds/entities").glob("*.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        blocks = [("type", d.get("sounds") or {})] + [(s.get("name"), s.get("sounds") or {}) for s in d.get("species", [])]
        for who, block in blocks:
            for key, sid in block.items():
                ns, _, path = sid.partition(":") if ":" in sid else ("minecraft", ":", sid)
                if ns == "untamedwilds" and path not in registered:
                    err(f"{f.stem} ({who}) {key}: {sid} is not a registered event")

    # files: referenced, credited, format
    referenced = set()
    for body in sj.values():
        for s in body.get("sounds", []):
            name = s if isinstance(s, str) else s.get("name")
            if name.startswith("untamedwilds:"):
                referenced.add(name.split(":", 1)[1] + ".ogg")
    credits = {}
    cj = SOUNDS_DIR / "credits.json"
    if cj.exists():
        for c in json.loads(cj.read_text(encoding="utf-8")):
            credits[c["installed"][len("assets/untamedwilds/sounds/"):]] = c
    try:
        import soundfile
        import numpy as np
    except Exception:
        soundfile = None
        warnings.append("soundfile/numpy not available: audio format checks skipped")
    total = 0
    for p in sorted(SOUNDS_DIR.rglob("*.ogg")):
        rel = str(p.relative_to(SOUNDS_DIR)).replace("\\", "/")
        total += p.stat().st_size
        if rel not in referenced:
            err(f"orphan sound file {rel} (not used by sounds.json)")
        c = credits.get(rel)
        if c is None:
            continue    # legacy upstream file (listed in CREDITS.md)
        if not any(a in c.get("licence", "").lower() for a in ALLOWED):
            err(f"{rel}: licence '{c.get('licence')}' is not CC0 / public domain")
        if soundfile:
            info = soundfile.info(str(p))
            if info.channels != 1:
                err(f"{rel}: {info.channels} channels (positional audio needs mono)")
            if info.samplerate not in (44100, 48000):
                err(f"{rel}: {info.samplerate} Hz")
            data, _ = soundfile.read(str(p))
            peak = float(np.max(np.abs(data))) if len(data) else 0.0
            if peak > 10 ** (-0.5 / 20):
                err(f"{rel}: peak {20 * np.log10(max(peak, 1e-9)):.2f} dBFS (> -0.5)")
            limit = 8.5 if "/call_" in rel else 3.5
            if info.duration > limit:
                err(f"{rel}: {info.duration:.1f} s (> {limit} s)")
    if total / 1e6 > BUDGET_MB:
        err(f"sounds total {total / 1e6:.2f} MB > {BUDGET_MB} MB budget")
    for w in warnings:
        print("WARN ", w)
    for e in errors:
        print("FAIL ", e)
    print(f"{len(registered)} registered events, {len(referenced)} referenced files, {total / 1e6:.2f} MB, "
          f"{len(credits)} credited clips: {'OK' if not errors else str(len(errors)) + ' FAILURES'}")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
