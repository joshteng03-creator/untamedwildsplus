#!/usr/bin/env python3
"""Offline census of a SAVED world (2026-10-24): counts every Untamed Wilds animal in the entity region files, by
type and species, adults vs juveniles, with mean hunger and body condition -- so a soak test can be read after the
game is closed even if /untamedwilds census was never run. No external libraries (a minimal NBT + Anvil reader).

Usage:  python scripts/world_census.py "<world dir>" [--radius BLOCKS] [--at X Z]
  --radius  only count animals within this many blocks of the player (default 192, about the simulated area)
  --at      centre on these coordinates instead of the player's saved position
"""
import gzip
import json
import math
import pathlib
import struct
import sys
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
ENT = ROOT / "src/main/resources/data/untamedwilds/entities"


class Reader:
    def __init__(self, data):
        self.d, self.i = data, 0

    def take(self, n):
        v = self.d[self.i:self.i + n]
        self.i += n
        return v

    def u(self, fmt):
        size = struct.calcsize(fmt)
        return struct.unpack(">" + fmt, self.take(size))[0]

    def string(self):
        return self.take(self.u("H")).decode("utf-8", "replace")

    def payload(self, tag):
        if tag == 1: return self.u("b")
        if tag == 2: return self.u("h")
        if tag == 3: return self.u("i")
        if tag == 4: return self.u("q")
        if tag == 5: return self.u("f")
        if tag == 6: return self.u("d")
        if tag == 7: n = self.u("i"); return self.take(n)
        if tag == 8: return self.string()
        if tag == 9:
            t, n = self.u("b"), self.u("i")
            return [self.payload(t) for _ in range(n)]
        if tag == 10:
            out = {}
            while True:
                t = self.u("b")
                if t == 0:
                    return out
                name = self.string()
                out[name] = self.payload(t)
        if tag == 11: n = self.u("i"); return list(struct.unpack(">%di" % n, self.take(4 * n)))
        if tag == 12: n = self.u("i"); return list(struct.unpack(">%dq" % n, self.take(8 * n)))
        raise ValueError(f"bad tag {tag}")

    def root(self):
        t = self.u("b")
        self.string()
        return self.payload(t)


def region_chunks(path):
    data = path.read_bytes()
    for k in range(1024):
        off = int.from_bytes(data[4 * k:4 * k + 3], "big")
        if off == 0:
            continue
        start = off * 4096
        length = struct.unpack(">i", data[start:start + 4])[0]
        comp = data[start + 4]
        raw = data[start + 5:start + 4 + length]
        try:
            blob = zlib.decompress(raw) if comp == 2 else gzip.decompress(raw) if comp == 1 else raw
            yield Reader(blob).root()
        except Exception:
            continue


def species_names():
    names = {}
    for f in ENT.glob("*.json"):
        d = json.loads(f.read_text(encoding="utf-8-sig"))
        names[f.stem] = {sp["variant"]: sp["name"] for sp in d["species"]}
        names[f.stem]["_carnivore"] = "carnivore" in d.get("ecology", {}).get("tags", [])
    return names


def player_pos(world):
    for f in (world / "playerdata").glob("*.dat"):
        try:
            p = Reader(gzip.decompress(f.read_bytes())).root()["Pos"]
            return p[0], p[2]
        except Exception:
            continue
    return None


def main():
    world = pathlib.Path(sys.argv[1])
    radius = float(sys.argv[sys.argv.index("--radius") + 1]) if "--radius" in sys.argv else 192.0
    centre = (float(sys.argv[sys.argv.index("--at") + 1]), float(sys.argv[sys.argv.index("--at") + 2])) if "--at" in sys.argv else player_pos(world)
    names = species_names()
    rows = {}
    for region in sorted((world / "entities").glob("r.*.mca")):
        rx, rz = (int(v) for v in region.stem.split(".")[1:3])
        if centre and (abs(rx * 512 + 256 - centre[0]) > radius + 400 or abs(rz * 512 + 256 - centre[1]) > radius + 400):
            continue
        for chunk in region_chunks(region):
            for e in chunk.get("Entities", []):
                eid = e.get("id", "")
                if not eid.startswith("untamedwilds:"):
                    continue
                pos = e.get("Pos", [0, 0, 0])
                if centre and math.hypot(pos[0] - centre[0], pos[2] - centre[1]) > radius:
                    continue
                typ = eid.split(":")[1]
                var = e.get("Variant", 0)
                sp = names.get(typ, {}).get(var, f"v{var}")
                r = rows.setdefault((typ, sp), [0, 0, 0.0, 0.0])
                r[1 if e.get("Age", 0) < 0 else 0] += 1
                r[2] += e.get("Hunger", 0)
                r[3] += e.get("Condition", 0.0)
    print(f"centre {centre}, radius {radius:.0f} blocks")
    print(f"{'type':<16}{'species':<22}{'adults':>7}{'young':>7}{'hunger':>8}{'cond':>7}")
    tot = {True: [0, 0], False: [0, 0]}
    for (typ, sp), (a, j, h, c) in sorted(rows.items()):
        n = max(1, a + j)
        carn = names.get(typ, {}).get("_carnivore", False)
        tot[carn][0] += a
        tot[carn][1] += j
        print(f"{typ:<16}{sp:<22}{a:7d}{j:7d}{h / n:8.0f}{c / n:7.2f}")
    pa, pj = tot[True]
    ha, hj = tot[False]
    print(f"\npredators {pa}/{pj}  other {ha}/{hj}  prey per predator (adults): {ha / max(1, pa):.1f}")


if __name__ == "__main__":
    main()
