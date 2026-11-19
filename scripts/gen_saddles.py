#!/usr/bin/env python3
"""Saddle overlays v2 (2026-10-08): painted in MODEL SPACE, so the saddle is one continuous piece.

v1 placed the blanket by fractions of each box separately; stacked boxes (hump on chest, hump on body) have
different lengths and centres, so the bands landed offset with bare hide between -- the user: "unattached".
v2 projects every texel of every drape-part face to its 3D position (vanilla ModelPart.Cube UV mapping +
the part's FK chain) and colours it from ONE saddle design defined in model space:

  seat     leather on TOP faces of the seat parts only, within |z - zc| <= seatL and |x| <= seatW
  blanket  any face texel within |z - zc| <= blanketL and above the drop line y <= yDrop (Java y is DOWN):
           drapes continuously over humps, chest, fur -- including the front/back faces inside the zone
  girth    side and bottom faces at |z - zc| <= 1, below the blanket, down to the belly

Camelids: humps are hidden per species by transparent skin texels, and an overlay is a separate texture,
so anything painted on a hidden hump would float. Each camelid species therefore gets its own
saddle_<species>.png: humps the species' skin hides are skipped, and the seat goes in the largest stretch
of back NOT covered by a visible hump (dromedary: beside the hump; Bactrian: between the humps; llama: mid-back).

    python scripts/gen_saddles.py            # drafts -> scratch/saddles/<type>/ (+ preview composites)
    python scripts/gen_saddles.py --install  # textures/entity/<type>/saddle[_<species>].png
"""
import json
import math
import pathlib
import shutil
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from java2bb import parse, canvas

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/untamedwilds/textures/entity"
ENT = ROOT / "src/main/resources/data/untamedwilds/entities"
OUT = ROOT / "scratch/saddles"

LEATHER = [(62, 38, 20), (107, 68, 40), (138, 90, 52)]
BLANKET = [(110, 34, 30), (140, 47, 42)]
BORDER = (217, 164, 65)
GIRTH = (52, 34, 22)
BUCKLE = (186, 176, 150)

# type: model, seat parts, drape parts, base part (sets the saddle position), (seatL, seatW, blanketL) as
# fractions of the base part's length/width, drop as a fraction of the base part's height below its top
SPEC = {
    "equid": dict(model="ModelEquid", seat=["body_barrel"], drape=["body_barrel", "body_chest", "body_croup"], base="body_barrel",
                  frac=(0.22, 0.32, 0.36), drop=0.45, preview="equid/przewalskis_horse"),
    "deer": dict(model="ModelDeer", seat=["body_barrel"], drape=["body_barrel", "body_withers", "body_croup", "body_chest"], base="body_barrel",
                 frac=(0.24, 0.32, 0.38), drop=0.45, preview="deer/red_deer"),
    "bison": dict(model="ModelBison", seat=["body_main"], drape=["body_main"], base="body_main",
                  frac=(0.24, 0.32, 0.36), drop=0.4, preview="bison/plains"),
    # humps stay bare hide: a half-draped hump read as patches (user), and hidden humps must never be painted
    "camel": dict(model="ModelCamel", seat=["body_main"], drape=["body_main"],
                  base="body_main", frac=(0.2, 0.34, 0.32), drop=0.42, preview="camel/dromedary", per_species=True,
                  humps=["body_hump_front", "body_hump_centre", "body_hump_back"]),
    # rebuilt rig (2026-11-15): the seat sits on the MID-BACK, behind a mammoth's hump and over every family's back box
    "mammoth": dict(model="ModelMammoth", seat=["body_back", "fur_mane"],
                    drape=["body_back", "body_withers", "body_croup", "body_mid", "body_chest", "body_hips", "fur_mane",
                           "fur_skirt_left", "fur_skirt_right"],
                    base="body_back", frac=(0.26, 0.34, 0.42), drop=1.25, preview="mammoth/asian_elephant"),
    # one mesh for all five species; only diprotodon / zygomaturus / nototherium can be saddled
    "diprotodont": dict(model="ModelDiprotodont", seat=["body_barrel"],
                        drape=["body_barrel", "body_shoulders", "body_chest", "body_rump"], base="body_barrel",
                        frac=(0.24, 0.30, 0.36), drop=0.45, preview="diprotodont/diprotodon"),
    # only argali + bighorn can be saddled; one texture for the type
    "caprine": dict(model="ModelCaprine", seat=["body_barrel"], drape=["body_barrel", "body_chest", "body_rump"],
                    base="body_barrel", frac=(0.26, 0.30, 0.38), drop=0.45, preview="caprine/argali"),
}
EXTRA_PREVIEWS = [("mammoth", "mammoth/woolly", []), ("camel", "camel/bactrian", []), ("camel", "camel/llama", []),
                  ("diprotodont", "diprotodont/zygomaturus", []), ("caprine", "caprine/bighorn", [])]
HIDE_DIP = ["nasal_boss", "joey_head", "joey_snout"]
HIDE_CAP = ["beard"] + ["%s_%s_%d" % (f, sd, k) for f in ("ibex", "chamois", "dagger") for sd in ("left", "right") for k in range(1, 5)]
HIDE = {"mammoth/asian_elephant": ["fur_mane", "fur_neck", "fur_chest", "fur_rump", "fur_skirt_left", "fur_skirt_right"],
        "diprotodont/diprotodon": HIDE_DIP, "diprotodont/zygomaturus": ["joey_head", "joey_snout"],
        "caprine/argali": HIDE_CAP, "caprine/bighorn": HIDE_CAP}


# ------------------------------------------------------------------ geometry
def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


class Model:
    def __init__(self, name):
        self.order, self.uv, self.rp, self.box, self.rot, self.par, self.mirror = parse(name)
        self.size = canvas(name)

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

    def world_box(self, part):
        bx, by, bz, w, h, d = self.box[part]
        M = self.matrix(part)
        pts = np.array([M @ np.array([x, y, z, 1.0]) for x in (bx, bx + w) for y in (by, by + h) for z in (bz, bz + d)])[:, :3]
        return pts.min(axis=0), pts.max(axis=0)

    def faces(self, part):
        """Yield (face, tx, ty, world_xyz) for every texel of the part's six faces (vanilla Cube mapping)."""
        u, v = self.uv[part]
        bx, by, bz, w, h, d = self.box[part]
        w, h, d = abs(w), abs(h), abs(d)
        x0, y0, z0 = bx, by, bz
        x1, y1, z1 = bx + w, by + h, bz + d
        M = self.matrix(part)

        def emit(face, rx0, ry0, rw, rh, fn):
            for ty in range(int(math.floor(ry0)), int(math.ceil(ry0 + rh))):
                for tx in range(int(math.floor(rx0)), int(math.ceil(rx0 + rw))):
                    a = min(max(tx + 0.5 - rx0, 0), rw)
                    b = min(max(ty + 0.5 - ry0, 0), rh)
                    p = M @ np.array(list(fn(a, b)) + [1.0])
                    yield face, tx, ty, p[:3]

        yield from emit("top", u + d, v, w, d, lambda a, b: (x0 + a, y0, z1 - b))            # DOWN polygon = visual top
        yield from emit("bottom", u + d + w, v, w, d, lambda a, b: (x0 + a, y1, z1 - b))     # UP polygon = visual bottom
        yield from emit("west", u, v + d, d, h, lambda a, b: (x0, y0 + b, z1 - a))
        yield from emit("north", u + d, v + d, w, h, lambda a, b: (x1 - a, y0 + b, z0))
        yield from emit("east", u + d + w, v + d, d, h, lambda a, b: (x1, y0 + b, z0 + a))
        yield from emit("south", u + 2 * d + w, v + d, w, h, lambda a, b: (x0 + a, y0 + b, z1))


# ------------------------------------------------------------------ painting
def paint(model, spec, skip=(), zc=None, seat_len=None, blanket_len=None):
    """blanket_len limits the blanket on TOP faces only (camel: the hump-free stretch); flanks keep the full drape."""
    img = Image.new("RGBA", tuple(model.size), (0, 0, 0, 0))
    lo, hi = model.world_box(spec["base"])
    length, width, height = hi[2] - lo[2], hi[0] - lo[0], hi[1] - lo[1]
    fs, fw, fb = spec["frac"]
    zc = (lo[2] + hi[2]) / 2 if zc is None else zc
    xc = (lo[0] + hi[0]) / 2
    seatL = length * fs if seat_len is None else seat_len
    seatW, blanketL = width * fw, max(length * fb, seatL + 2)
    topL = blanketL if blanket_len is None else min(blanketL, blanket_len)
    ydrop = lo[1] + height * spec["drop"]
    for part in spec["drape"]:
        if part in skip or part not in model.box:
            continue
        for face, tx, ty, p in model.faces(part):
            if not (0 <= tx < img.width and 0 <= ty < img.height):
                continue
            x, y, z = p
            dz, dx = abs(z - zc), abs(x - xc)
            c = None
            if face == "top" and part in spec["seat"] and dz <= seatL and dx <= seatW:
                rim = seatL - dz < 1.0 or seatW - dx < 1.0
                c = LEATHER[0] if rim else LEATHER[2] if seatL - dz < 2.0 else LEATHER[1]
            elif dz <= (topL if face == "top" else blanketL) and y <= ydrop:
                edge = (topL if face == "top" else blanketL) - dz < 1.0 or (face not in ("top", "bottom") and ydrop - y < 1.0)
                c = BORDER if edge else (BLANKET[0] if int(z + 100) % 3 == 0 and face in ("west", "east") else BLANKET[1])
            elif face in ("west", "east", "bottom") and dz <= 1.0 and part == spec["base"]:
                c = BUCKLE if face != "bottom" and abs(y - (ydrop + (hi[1] - ydrop) * 0.5)) < 0.6 else GIRTH
            if c:
                img.putpixel((tx, ty), c + (255,))
    return img


def opaque_fraction(model, part, skin):
    u, v = model.uv[part]
    w, h, d = (abs(a) for a in model.box[part][3:6])
    tot = op = 0
    for ty in range(int(v), int(math.ceil(v + d + h))):
        for tx in range(int(u), int(math.ceil(u + 2 * d + 2 * w))):
            if 0 <= tx < skin.width and 0 <= ty < skin.height:
                tot += 1
                op += skin.getpixel((tx, ty))[3] > 0
    return op / max(1, tot)


def skin_file(key):
    typ, sp = key.split("/")
    for n in (sp + ".png", sp + "_1.png"):
        p = TEX / typ / n
        if p.exists():
            return p
    raise FileNotFoundError(key)


def camel_variants(model, spec):
    """species -> (hidden humps, seat centre z, seat half-length): the seat in the longest hump-free stretch of back."""
    out = {}
    species = [s["name"] for s in json.loads((ENT / "camel.json").read_text(encoding="utf-8-sig"))["species"]]
    lo, hi = model.world_box(spec["base"])
    for sp in species:
        skin = Image.open(skin_file("camel/" + sp)).convert("RGBA")
        hidden = [h for h in spec["humps"] if opaque_fraction(model, h, skin) < 0.5]
        spans = sorted(model.world_box(h)[0][2::3].tolist() + model.world_box(h)[1][2::3].tolist() for h in spec["humps"] if h not in hidden)
        free, cur = [], lo[2]
        for a, b in spans:
            if a > cur:
                free.append((cur, a))
            cur = max(cur, b)
        free.append((cur, hi[2]))
        a, b = max(free, key=lambda ab: ab[1] - ab[0])
        out[sp] = (hidden, (a + b) / 2, max(1.5, min((b - a) / 2 - 0.5, (hi[2] - lo[2]) * spec["frac"][0])),
                   min((hi[2] - lo[2]) * spec["frac"][2], (b - a) / 2 + 1.0))
    return out


def main():
    install = "--install" in sys.argv
    for typ, spec in SPEC.items():
        model = Model(spec["model"])
        d = OUT / typ
        d.mkdir(parents=True, exist_ok=True)
        textures = {}
        if spec.get("per_species"):
            for sp, (hidden, zc, sl, bl) in camel_variants(model, spec).items():
                textures["saddle_%s" % sp] = (paint(model, spec, skip=hidden, zc=zc, seat_len=sl, blanket_len=bl), sp, hidden)
            textures["saddle"] = textures["saddle_" + spec["preview"].split("/")[1]]
        else:
            textures["saddle"] = (paint(model, spec), None, [])
        for name, (img, sp, hidden) in textures.items():
            img.save(d / (name + ".png"))
            if install:
                shutil.copy(d / (name + ".png"), TEX / typ / (name + ".png"))
        # preview composites
        previews = [spec["preview"]] + [p for t, p, _ in EXTRA_PREVIEWS if t == typ]
        for key in previews:
            sp = key.split("/")[1]
            ov = textures.get("saddle_" + sp, textures["saddle"])[0]
            comp = Image.open(skin_file(key)).convert("RGBA")
            comp.alpha_composite(ov)
            comp.save(d / ("preview_%s.png" % sp))
        print(typ, sorted(textures), "(installed)" if install else "")


if __name__ == "__main__":
    main()
