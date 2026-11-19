#!/usr/bin/env python3
"""Life-size audit (2026-10-09; user: "use life size, but also scale up the older models to that standard").

The newer rigs (antelope, deer, diprotodont, equid, giraffid, macrauchenia, tapir, toxodon) are calibrated
1 block = 1 m at the shoulder: scale = real shoulder metres x 16 / mesh shoulder units. The legacy rigs were
never calibrated and run ~0.8-0.9x life size. This measures each legacy model's shoulder height with exact
forward kinematics (rotations included -- java2bb's unrotated boxes are only a lower bound), compares every
species' RENDERED shoulder (mesh x scale) with its real one, and proposes the life-size scale.

    python scripts/life_size_audit.py              # report
    python scripts/life_size_audit.py --parts T    # every part's world top/bottom for type T (to pick SHOULDER)
    python scripts/life_size_audit.py --json       # proposals as JSON (scratch/life_size_proposal.json)
"""
import json
import math
import pathlib
import sys

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from java2bb import parse

ROOT = pathlib.Path(__file__).resolve().parent.parent
ENT = ROOT / "src/main/resources/data/untamedwilds/entities"

# type -> (model, parts whose TOP at the shoulder defines withers height, foot parts defining the ground)
# Shoulder = the topline over the forelegs, never a hump (camel) or head; for bison and the proboscideans
# the quoted shoulder height IS the top of the shoulder hump / withers mass, so those parts are included.
TYPES = {
    "aardvark": ("ModelAardvark", ["body_main"]),
    "bear": ("ModelBear", ["body_torso"]),                    # the shoulder hump, not the higher rump box
    "big_cat": ("ModelBigCat", ["body_main"]),                # not the mane
    "bison": ("ModelBison", ["body_torso"]),                  # the shoulder hump IS the quoted height
    "boar": ("ModelBoar", ["main_body"]),                     # not the bristle crest (shape15)
    "camel": ("ModelCamel", ["body_main"]),                   # shoulder, below the humps
    "dire_wolf": ("ModelDireWolf", ["body_main"]),
    "glyptodont": ("ModelGlyptodont", ["shell_crown"]),      # carapace top = quoted height
    "ground_sloth": ("ModelGroundSloth", ["body_chest"]),    # on all fours, at the shoulder (not the mid-back arch)
    "hippo": ("ModelHippo", ["body_main"]),
    "hyena": ("ModelHyena", ["body_main"]),                   # not the mane
    "mammoth": ("ModelMammoth", ["body_hump"]),               # not the woolly fur_mane
    "opossum": ("ModelOpossum", ["body_main"]),
    "rhino": ("ModelRhino", ["body_front"]),
}

# Real shoulder height in metres, typical adult (sexes averaged, leaning male for dimorphic species).
# KEEP = scale the user set by hand on 2026-10-09 (body-misfit fixes); reported, never changed.
REAL = {
    "aardvark": {"common": 0.62},
    "bear": {"black": 0.85, "blind": 1.0, "brown": 1.1, "cave": 1.25, "panda": 0.75, "polar": 1.3, "spectacled": 0.75,
             "sun": 0.65, "short_faced": 1.6, "arctotherium": 1.7},
    "big_cat": {"cave_lion": 1.25, "jaguar": 0.7, "leopard": 0.65, "lion": 1.15, "marsupial_lion": 0.7, "puma": 0.7,
                "sabertooth": 1.2, "snow_leopard": 0.6, "tiger": 1.0, "american_lion": 1.25, "homotherium": 1.1,
                "cheetah": 0.8, "american_cheetah": 0.85, "barbary_lion": 1.15, "caspian_tiger": 1.0,
                "thylacosmilus": 0.65, "smilodon_fatalis": 1.0},
    "bison": {"plains": 1.75, "wisent": 1.8, "steppe": 2.0, "long_horned": 2.3, "aurochs": 1.7, "giant_buffalo": 1.85,
              "gaur": 1.85, "wild_yak": 1.9, "african_buffalo": 1.5, "musk_ox": 1.35, "water_buffalo": 1.65,
              "banteng": 1.6, "pelorovis": 1.85},
    "boar": {"eurasian": 0.85, "peccary": 0.5, "red_river": 0.7, "warthog": 0.7, "giant_warthog": 1.0,
             "giant_forest_hog": 0.95, "babirusa": 0.75, "kubanochoerus": 1.1, "platygonus": 0.75},
    "camel": {"bactrian": 1.8, "dromedary": 1.9, "wild_bactrian": 1.75, "western": 2.2, "titanotylopus": 3.0,
              "guanaco": 1.1, "vicuna": 0.85, "paleolama": 1.2, "camelus_knoblochi": 2.4, "llama": 1.15,
              "alpaca": 0.9, "hemiauchenia": 1.3},
    "dire_wolf": {"dire_wolf": 0.85, "pleistocene_wolf": 0.85, "dhole": 0.5, "african_wild_dog": 0.7,
                  "protocyon": 0.6, "thylacine": 0.6, "gray_wolf": 0.8, "coyote": 0.6, "maned_wolf": 0.9,
                  "xenocyon": 0.7, "bush_dog": 0.25},
    "glyptodont": {"glyptodon": 1.5, "doedicurus": 1.5, "pampatherium": "KEEP", "glyptotherium": 1.4},
    "ground_sloth": {"megatherium": 2.0, "eremotherium": 2.0, "megalonyx": 1.2, "megalocnus": "KEEP",
                     "glossotherium": 1.2},
    "hippo": {"common": 1.5},
    "hyena": {"aardwolf": 0.47, "brown": 0.75, "spotted": 0.8, "striped": 0.7, "shortface": 1.0, "cave_hyena": 0.95},
    "mammoth": {"woolly": 3.0, "columbian": 3.7, "steppe_mammoth": 4.0, "mastodon": 2.6, "straight_tusked": 3.8,
                "cuvieronius": 2.3, "african_elephant": 3.2, "asian_elephant": 2.6, "notiomastodon": 2.5,
                "paleoloxodon_namadicus": 4.5, "pygmy_mammoth": 1.6, "cretan_dwarf": 0.9},
    "opossum": {"virginia": 0.22},
    "rhino": {"black": 1.5, "indian": 1.75, "javan": 1.6, "sumatran": 1.3, "white": 1.8, "wooly": 1.8,
              "elasmotherium": 2.2, "mercks": 1.9},
}
LEG_DROP = 13.18     # ModelDireWolf: foreleg pivot-to-sole; legLength lifts the body by (legLength-1) x this


def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    return (np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]) @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
            @ np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]))


class Model:
    def __init__(self, name):
        self.order, self.uv, self.rp, self.box, self.rot, self.par, self.mirror = parse(name)

    def matrix(self, part):
        chain = [part]
        while self.par.get(chain[-1]):
            chain.append(self.par[chain[-1]])
        M = np.eye(4)
        for n in reversed(chain):
            T = np.eye(4)
            T[:3, :3] = rot(*(self.rot.get(n) or (0.0, 0.0, 0.0)))
            T[:3, 3] = self.rp[n]
            M = M @ T
        return M

    def points(self, part):
        """Box corners in model space, Java Y DOWN (ground ~ y 24)."""
        bx, by, bz, w, h, d = self.box[part]
        M = self.matrix(part)
        return np.array([(M @ np.array([x, y, z, 1.0]))[:3] for x in (bx, bx + w) for y in (by, by + h) for z in (bz, bz + d)])


def part_table(typ):
    m = Model(TYPES[typ][0])
    rows = []
    for n in m.order:
        if n not in m.box:
            continue
        p = m.points(n)
        rows.append((n, p[:, 1].min(), p[:, 1].max(), p[:, 2].min(), p[:, 2].max()))
    ground = max(r[2] for r in rows)
    print(f"{typ}: ground (lowest point) y={ground:.2f}   heights are above that, in units")
    for n, ylo, yhi, zlo, zhi in sorted(rows, key=lambda r: r[1]):
        print(f"  {n:<26} top {ground - ylo:6.2f}  bottom {ground - yhi:6.2f}   z {zlo:6.1f}..{zhi:6.1f}")


if __name__ == "__main__" and "--parts" in sys.argv:
    part_table(sys.argv[sys.argv.index("--parts") + 1])


def shoulder_units(typ):
    m = Model(TYPES[typ][0])
    pts = np.vstack([m.points(n) for n in m.box])
    ground = pts[:, 1].max()
    top = min(m.points(n)[:, 1].min() for n in TYPES[typ][1])
    return ground - top


def proposals():
    out = []
    for typ in TYPES:
        u = shoulder_units(typ)
        d = json.loads((ENT / (typ + ".json")).read_text(encoding="utf-8-sig"))
        top_flags = d.get("flags", {})
        for sp in d["species"]:
            real = REAL[typ][sp["name"]]
            su = u
            if typ == "dire_wolf":
                leg = sp.get("flags", {}).get("legLength", top_flags.get("legLength", 1000)) / 1000.0
                su = u + (leg - 1.0) * LEG_DROP
            cur = sp.get("scale", 1.0)
            rendered = su * cur / 16.0
            if real == "KEEP":
                out.append((typ, sp["name"], cur, rendered, None, cur))
            else:
                out.append((typ, sp["name"], cur, rendered, real, round(real * 16.0 / su, 3)))
    return out


def report():
    rows = proposals()
    print(f"{'type/species':<32}{'scale':>7}{'renders':>9}{'real':>7}{'ratio':>7}{'-> scale':>10}{'change':>8}")
    for typ, name, cur, rend, real, new in rows:
        if real is None:
            print(f"{typ + '/' + name:<32}{cur:7.3f}{rend:8.2f}m{'keep':>7}{'':>7}{new:10.3f}{'(user)':>8}")
            continue
        print(f"{typ + '/' + name:<32}{cur:7.3f}{rend:8.2f}m{real:6.2f}m{rend / real:7.2f}{new:10.3f}{(new / cur - 1) * 100:+7.0f}%")
    ratios = sorted(r[3] / r[4] for r in rows if r[4])
    print(f"\n{len(ratios)} species: rendered/real median {ratios[len(ratios) // 2]:.2f}, "
          f"min {ratios[0]:.2f}, max {ratios[-1]:.2f}")


if __name__ == "__main__" and "--parts" not in sys.argv:
    report()
    if "--json" in sys.argv:
        (ROOT / "scratch/life_size_proposal.json").write_text(json.dumps(proposals(), indent=1))
