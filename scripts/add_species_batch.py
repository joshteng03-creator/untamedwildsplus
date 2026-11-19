#!/usr/bin/env python3
"""Add a batch of species (2.9.0): each species object is APPENDED to its type's JSON through append_species.py (next
free variant index only -- saved animals are tied to the index), and its name + scientific name go into en_us.json next
to the type's existing keys. Idempotent: a species that already exists is skipped.

    python scripts/add_species_batch.py scripts/species_batches/a_upstream_art.json
Batch file: [{"type": "catfish", "name": "...", "display": "...", "sciname": "...", "entry": {...without name/variant...}}]
"""
import collections
import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
ENT = ROOT / "src/main/resources/data/untamedwilds/entities"
LANG = ROOT / "src/main/resources/assets/untamedwilds/lang/en_us.json"


def main(batch_path):
    batch = json.loads(pathlib.Path(batch_path).read_text(encoding="utf-8"))
    lang = json.loads(LANG.read_text(encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
    for b in batch:
        t, name = b["type"], b["name"]
        d = json.loads((ENT / f"{t}.json").read_text(encoding="utf-8"))
        if name in [s["name"] for s in d["species"]]:
            print(f"skip {t}:{name} (exists)")
        else:
            obj = collections.OrderedDict([("name", name), ("variant", len(d["species"]))])
            obj.update(b["entry"])
            r = subprocess.run([sys.executable, str(ROOT / "scripts/append_species.py"), t, json.dumps(obj)], cwd=ROOT,
                               capture_output=True, text=True)
            print(r.stdout.strip() or r.stderr.strip())
            if r.returncode:
                raise SystemExit(f"failed on {t}:{name}")
        # lang: insert right after the type's last species key, so the file stays grouped
        keys = {f"entity.untamedwilds.{t}_{name}": b["display"], f"entity.untamedwilds.{t}_{name}.sciname": b["sciname"]}
        items = list(lang.items())
        last = max((i for i, (k, _) in enumerate(items) if k.startswith(f"entity.untamedwilds.{t}_")), default=len(items) - 1)
        new_items = [(k, v) for k, v in keys.items() if k not in lang]
        for k, v in keys.items():
            if k in lang:
                lang[k] = v
        if new_items:
            items = items[:last + 1] + new_items + items[last + 1:]
            lang = collections.OrderedDict(items)
    LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("lang written")


if __name__ == "__main__":
    main(sys.argv[1])
