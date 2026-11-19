#!/usr/bin/env python3
"""Static species check (2.9.0): for every type JSON, every species must have
  * contiguous variant indices 0..N-1 (saved animals are tied to the index),
  * every texture its `skins` code resolves to (EntityUtils.buildSkinArrays: tens = commons, >1 -> _1.._N, else
    <species>.png; units = rares _1r.._Nr; flags.dimorphism 1 -> each path as _male / _female),
  * its name and scientific name in en_us.json.
Exit code 1 on any failure.   python scripts/validate/validate_species.py
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
ENT = RES / "data/untamedwilds/entities"
TEX = RES / "assets/untamedwilds/textures/entity"
LANG = json.loads((RES / "assets/untamedwilds/lang/en_us.json").read_text(encoding="utf-8"))


def textures(t, sp):
    skins = sp.get("skins", 10)
    commons, rares = skins // 10, skins % 10
    base = [f"{sp['name']}_{i}" for i in range(1, commons + 1)] if commons >= 2 else [sp["name"]]
    base += [f"{sp['name']}_{i}r" for i in range(1, rares + 1)]
    if (sp.get("flags") or {}).get("dimorphism") == 1:
        base = [f"{b}_{s}" for b in base for s in ("male", "female")]
    return [TEX / t / f"{b}.png" for b in base]


def main():
    errors, count = [], 0
    for f in sorted(ENT.glob("*.json")):
        t = f.stem
        d = json.loads(f.read_text(encoding="utf-8"))
        variants = [s.get("variant") for s in d.get("species", [])]
        if variants != list(range(len(variants))):
            errors.append(f"{t}: variants not contiguous: {variants}")
        for sp in d.get("species", []):
            count += 1
            for p in textures(t, sp):
                if not p.exists():
                    errors.append(f"{t}:{sp['name']}: missing texture {p.relative_to(TEX)}")
            for k in (f"entity.untamedwilds.{t}_{sp['name']}", f"entity.untamedwilds.{t}_{sp['name']}.sciname"):
                if k not in LANG:
                    errors.append(f"{t}:{sp['name']}: missing lang {k}")
    for e in errors:
        print("FAIL ", e)
    print(f"{count} species checked: {'OK' if not errors else str(len(errors)) + ' FAILURES'}")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
