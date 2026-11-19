#!/usr/bin/env python3
"""Encyclopedia icons (2026-10-08): 16x16 front-facing head portraits in the style of the shipped icons
(flat, outline-free, 2-3 tone blocks, 1px eyes with an outer highlight, darker muzzle).

One hand-drawn TEMPLATE per body type (ASCII, below), plus FEATURE overlays (antlers, horns, tusks,
ossicones, stripes) drawn on top. Each species gets its colours from its OWN skin: the opaque texels of
its PNG sorted by luminance give dark / base / light; feature colours are per material (antler bone,
keratin horn, ivory). PALETTE_OVERRIDE fixes species whose skin quantiles mislead (zebras, piebalds).

Keys:  . clear   B base   D dark   L light (muzzle, cheeks)   M nose/mouth   E eye   W eye highlight
       K inner ear   H feature   h feature highlight   S pattern dark   P pattern pale

    python scripts/gen_icons.py --sample             # review sheet -> scratch/icon_sample.png
    python scripts/gen_icons.py --all                # every missing icon -> scratch/icons/
    python scripts/gen_icons.py --install            # copy scratch/icons into assets
"""
import json
import pathlib
import shutil
import sys

from PIL import Image, ImageDraw

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
TEX = RES / "assets/untamedwilds/textures/entity"
ICON = RES / "assets/untamedwilds/textures/icon"
ENT = RES / "data/untamedwilds/entities"
OUT = ROOT / "scratch/icons"


def hx(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


# ------------------------------------------------------------------ templates (16 rows x 16 cols)
T = {}
T["deer"] = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......DBBD......",
    "BBK..DBBBBD..KBB",
    ".BKKDBBBBBBDKKB.",
    "..BBBBBBBBBBBB..",
    "...WEBBLLBBEW...",
    ".....BBLLBB.....",
    ".....BLLLLB.....",
    ".....DLLLLD.....",
    "......LLLL......",
    "......MMMM......",
    ".......MM.......",
]
T["equid"] = [
    "................",
    "....B......B....",
    "....BB....BB....",
    "....BKB..BKB....",
    ".....BBSSBB.....",
    ".....BSSSSB.....",
    "....BBBSSBBB....",
    "....BBBBBBBB....",
    "...WEBBBBBBEW...",
    "....DBBBBBBD....",
    ".....BBBBBB.....",
    ".....BBLLBB.....",
    ".....BLLLLB.....",
    ".....LLLLLL.....",
    ".....LMLLML.....",
    "......LLLL......",
]
T["giraffid"] = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......BBBB......",
    "KKB..BBBBBB..BKK",
    ".KBBBBBBBBBBBBK.",
    "...WEBBBBBBEW...",
    "....DBBBBBBD....",
    ".....BBBBBB.....",
    ".....BBBBBB.....",
    ".....BLLLLB.....",
    ".....LLLLLL.....",
    ".....LMLLML.....",
    "......LLLL......",
]
T["mammoth"] = [
    "................",
    "......DDDD......",
    ".....DBBBBD.....",
    "..BBBBBBBBBBBB..",
    ".BBBBBBBBBBBBBB.",
    ".BDBBBBBBBBBBDB.",
    ".BDBWEBBBBEWBDB.",
    ".BDDBBBBBBBBDDB.",
    "..BDDBBBBBBDDB..",
    "...BDBBBBBBDB...",
    ".....DBBBBD.....",
    "......BBBB......",
    "......DBBD......",
    "......BBBB......",
    "......DBBD......",
    ".......BB.......",
]
T["ground_sloth"] = [
    "................",
    "................",
    "....DDBBBBDD....",
    "...DBBBBBBBBD...",
    "..DBBBBBBBBBBD..",
    "..BBBBBBBBBBBB..",
    "..BBWEBBBBEWBB..",
    "..DBBBBBBBBBBD..",
    "...BBBBLLBBBB...",
    "...DBBLLLLBBD...",
    "....BLLLLLLB....",
    "....BLLMMLLB....",
    ".....LLMMLL.....",
    ".....DLLLLD.....",
    "......DDDD......",
    "................",
]
T["glyptodont"] = [
    "................",
    "................",
    "...HHHHHHHHHH...",
    "..HhHhHhHhHhHH..",
    ".HHhHHhHHhHHhHH.",
    ".HhHHhHHhHHhHhH.",
    ".HHHHHHHHHHHHHH.",
    "HHhHHHhhhhHHHhHH",
    "HHHHHHhHHhHHHHHH",
    "HhHH.BHHHHB.HHhH",
    "HHH..BBBBBB..HHH",
    ".....WEBBEW.....",
    ".....BBBBBB.....",
    ".....BLLLLB.....",
    "......LMML......",
    "................",
]
T["toxodon"] = [
    "................",
    "................",
    "................",
    "...K........K...",
    "..BKB......BKB..",
    "..BBBBBBBBBBBB..",
    ".BBBBBBBBBBBBBB.",
    ".BBWEBBBBBBEWBB.",
    ".DBBBBBBBBBBBBD.",
    ".DBBBBBBBBBBBBD.",
    "..BBBLLLLLLBBB..",
    "..BLLLLLLLLLLB..",
    "..LLLMLLLLMLLL..",
    "..DLLLLLLLLLLD..",
    "...DDLLLLLLDD...",
    "................",
]
T["macrauchenia"] = [
    "................",
    "................",
    "....BK....KB....",
    "....BB....BB....",
    ".....BBBBBB.....",
    "....BBBBBBBB....",
    "...WEBBBBBBEW...",
    "....DBBBBBBD....",
    ".....BBBBBB.....",
    ".....BBDDBB.....",
    ".....BBBBBB.....",
    ".....BLLLLB.....",
    "......LLLL......",
    "......DMMD......",
    ".......DD.......",
    ".......DD.......",
]
T["tapir"] = [
    "................",
    "................",
    "..PB........BP..",
    "..BKB......BKB..",
    "...BBBBBBBBBB...",
    "..BBBBBBBBBBBB..",
    "..BBWEBBBBEWBB..",
    "..DBBBBBBBBBBD..",
    "...DBBBBBBBBD...",
    "....BBBBBBBB....",
    ".....BBBBBB.....",
    ".....BBBBBB.....",
    "......BBBB......",
    "......DBBD......",
    "......DMMD......",
    ".......DD.......",
]
T["opossum"] = [
    "................",
    "................",
    "..DD........DD..",
    "..DKD......DKD..",
    "..DDLLLLLLLLDD..",
    "...LLLLLLLLLL...",
    "...LSLLLLLLSL...",
    "...LEWLLLLWEL...",
    "....LLLLLLLL....",
    ".....LLLLLL.....",
    "......LLLL......",
    "......LLLL......",
    ".......LL.......",
    ".......MM.......",
    ".......MM.......",
    "................",
]
T["dire_wolf"] = [
    "................",
    "..B..........B..",
    "..BB........BB..",
    "..BKB......BKB..",
    "..BKBBBBBBBBKB..",
    "..BBBBBBBBBBBB..",
    "..DBBBBBBBBBBD..",
    "..DBWEBBBBEWBD..",
    "...DBBBBBBBBD...",
    "...DBBBLLBBBD...",
    "....BBLLLLBB....",
    "....DLLLLLLD....",
    ".....LLLLLL.....",
    ".....LLMMLL.....",
    "......LMML......",
    "................",
]

# ------------------------------------------------------------------ feature overlays (H/h/S/P only)
F = {}
F["antler_beam"] = [
    "H..H........H..H",
    ".H.H...hh...H.H.",
    ".HHH..H..H..HHH.",
    "...HHH....HHH...",
    "....HH....HH....",
    ".....H....H.....",
]
F["antler_palm"] = [
    "HhHhH......HhHhH",
    "HHHHHH....HHHHHH",
    ".HHHHHH..HHHHHH.",
    "...HHHH..HHHH...",
    "......H..H......",
    "......H..H......",
]
F["antler_spike"] = [
    "................",
    "................",
    "................",
    "......H..H......",
    "......H..H......",
    "......H..H......",
]
F["antler_deer_spotted"] = F["antler_beam"]
F["ossicones"] = [
    "................",
    "................",
    "......h..h......",
    "......H..H......",
    "......H..H......",
    "................",
]
F["ossicones_sivatherium"] = [
    "..h..........h..",
    "..HH........HH..",
    "...HHh....hHH...",
    "....HHH..HHH....",
    "......H..H......",
    "................",
]
F["tusks_curved"] = [
    "................", "................", "................", "................", "................",
    "................", "................", "................", "................", "................",
    "................",
    ".....H....H.....",
    "....H......H....",
    "...H........H...",
    "...hH......Hh...",
    "....hH....Hh....",
]
F["tusks_straight"] = [
    "................", "................", "................", "................", "................",
    "................", "................", "................", "................", "................",
    "................",
    ".....H....H.....",
    ".....H....H.....",
    ".....H....H.....",
    ".....h....h.....",
    "................",
]
F["tusks_small"] = [
    "................", "................", "................", "................", "................",
    "................", "................", "................", "................", "................",
    "................",
    ".....H....H.....",
    ".....h....h.....",
    "................", "................", "................",
]
F["zebra_stripes"] = [
    "................", "................", "................", "................",
    "................", "................", "....S......S....", "................",
    "....S......S....", ".....S....S.....", "................", ".....S....S.....",
    "................", "................", "................", "................",
]
F["giraffe_patches"] = [
    "................", "................", "................", "................", "................",
    "................", "................", "................", "................",
    ".....S....S.....", "......S..S......", ".....S....S.....",
    "................", "................", "................", "................",
]
F["tapir_saddle"] = [
    "................", "................", "..P..........P..", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
]
F["opossum_ears"] = F["tapir_saddle"]
F["mammoth_wool"] = [
    "................", "......SSSS......", ".....SSSSSS.....", "..SS........SS..",
    ".S............S.", ".S............S.", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
]
F["canid_mask"] = [
    "................", "................", "................", "................",
    "................", "................", "................", "................",
    "................", ".......SS.......", "......S..S......", "................",
    "................", "................", "................", "................",
]
F["thylacine_stripes"] = [
    "................", "................", "................", "................",
    "................", "....S.S..S.S....", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
]

MATERIAL = {"antler": ("#b49c78", "#e4d8bf"), "keratin": ("#3e342a", "#6e6254"), "ivory": ("#d9cbaa", "#f2ead6"),
            "ossicone": ("#4a3a2a", "#2a2018"), "shell": None}

# per type: template, default features, material; per species overrides below
TYPE = {
    "deer": ("deer", ["antler_beam"], "antler"),
    "equid": ("equid", [], None),
    "giraffid": ("giraffid", ["ossicones"], "ossicone"),
    "mammoth": ("mammoth", ["tusks_straight"], "ivory"),
    "ground_sloth": ("ground_sloth", [], None),
    "glyptodont": ("glyptodont", [], "shell"),
    "toxodon": ("toxodon", [], None),
    "macrauchenia": ("macrauchenia", [], None),
    "tapir": ("tapir", [], None),
    "opossum": ("opossum", ["opossum_ears"], None),
    "dire_wolf": ("dire_wolf", [], None),
}
SPECIES = {
    "deer/megaloceros": ["antler_palm"], "deer/stag_moose": ["antler_palm"], "deer/moose": ["antler_palm"],
    "deer/fallow": ["antler_palm"], "deer/pudu": ["antler_spike"],
    "equid/grevys_zebra": ["zebra_stripes"], "equid/quagga": ["zebra_stripes"],
    "giraffid/giraffe": ["ossicones", "giraffe_patches"], "giraffid/sivatherium": ["ossicones_sivatherium"],
    "giraffid/okapi": ["ossicones"],
    "mammoth/woolly": ["tusks_curved", "mammoth_wool"], "mammoth/columbian": ["tusks_curved"],
    "mammoth/steppe_mammoth": ["tusks_curved", "mammoth_wool"], "mammoth/pygmy_mammoth": ["tusks_small", "mammoth_wool"],
    "mammoth/cretan_dwarf": ["tusks_small"], "mammoth/cuvieronius": ["tusks_curved"],
    "tapir/malayan": ["tapir_saddle"],
    "dire_wolf/thylacine": ["thylacine_stripes"], "dire_wolf/african_wild_dog": ["canid_mask"],
    "dire_wolf/maned_wolf": ["canid_mask"], "dire_wolf/dhole": [],
}
PALETTE_OVERRIDE = {
    "equid/grevys_zebra": {"B": "#e8e4dc", "D": "#c8c2b6", "L": "#d8d2c6", "S": "#1e1c1a", "M": "#1e1c1a", "K": "#2a2622"},
    "equid/quagga": {"S": "#2a2018"},
    "tapir/malayan": {"P": "#e8e4dc"},
}


def skin_path(typ, sp):
    for name in (sp + ".png", sp + "_1.png"):
        p = TEX / typ / name
        if p.exists():
            return p
    return None


def palette(typ, sp):
    p = skin_path(typ, sp)
    px = []
    if p:
        im = Image.open(p).convert("RGBA")
        px = sorted((c[:3] for c in im.getdata() if c[3] > 200), key=lum)
    if len(px) < 20:
        px = [(120, 100, 80)] * 20
    n = len(px)
    q = lambda f: px[min(n - 1, int(n * f))]
    avg = lambda a, b: tuple(sum(c[k] for c in px[int(n * a):int(n * b)]) // max(1, len(px[int(n * a):int(n * b)])) for k in range(3))
    base = avg(0.35, 0.65)
    pal = {"B": base, "D": avg(0.08, 0.3), "L": avg(0.72, 0.93), "M": shade(avg(0.0, 0.08), 0.8),
           "E": (22, 20, 18), "W": (232, 230, 226), "K": mix(avg(0.0, 0.2), (190, 130, 120), 0.35),
           "S": shade(avg(0.0, 0.1), 0.85), "P": avg(0.9, 1.0)}
    return pal


def compose(typ, sp, feats=None):
    tname, deff, mat = TYPE[typ]
    feats = SPECIES.get(typ + "/" + sp, deff) if feats is None else feats
    pal = palette(typ, sp)
    if mat and MATERIAL.get(mat):
        pal["H"], pal["h"] = hx(MATERIAL[mat][0]), hx(MATERIAL[mat][1])
    elif mat == "shell":
        pal["H"], pal["h"] = shade(pal["B"], 0.85), shade(pal["L"], 1.0)
    for k, v in PALETTE_OVERRIDE.get(typ + "/" + sp, {}).items():
        pal[k] = hx(v)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    layers = [T[tname]] + [F[f] for f in feats]
    for li, rows in enumerate(layers):
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == ".":
                    continue
                if li > 0 and ch in "SP" and img.getpixel((x, y))[3] == 0:
                    continue   # patterns only paint ON the head
                img.putpixel((x, y), pal[ch] + (255,))
    return img


def species_list():
    out = []
    for f in sorted(ENT.glob("*.json")):
        d = json.loads(f.read_text(encoding="utf-8-sig"))
        out += [(f.stem, s["name"]) for s in d["species"]]
    return out


def sheet(items, path, S=8, cols=8):
    W = 16 * S + 12
    rows = -(-len(items) // cols)
    sh = Image.new("RGBA", (cols * W, rows * (16 * S + 22)), (64, 64, 64, 255))
    d = ImageDraw.Draw(sh)
    for k, (label, img) in enumerate(items):
        x, y = (k % cols) * W + 6, (k // cols) * (16 * S + 22) + 4
        sh.alpha_composite(img.resize((16 * S, 16 * S), Image.NEAREST), (x, y))
        d.text((x, y + 16 * S + 3), label[:22], fill=(235, 235, 235, 255))
    sh.save(path)
    print(path)


def sample():
    if True:
        picks = ["deer/red_deer", "deer/moose", "deer/pudu", "equid/przewalskis_horse", "equid/grevys_zebra", "giraffid/giraffe",
                 "giraffid/okapi", "giraffid/sivatherium", "mammoth/woolly", "mammoth/african_elephant", "mammoth/cretan_dwarf",
                 "ground_sloth/megatherium", "glyptodont/glyptodon", "toxodon/toxodon", "macrauchenia/macrauchenia",
                 "tapir/malayan", "tapir/lowland", "opossum/virginia", "dire_wolf/dire_wolf", "dire_wolf/maned_wolf",
                 "dire_wolf/thylacine", "dire_wolf/african_wild_dog"]
        picks += ["big_cat/caspian_tiger", "big_cat/cheetah", "bison/gaur", "bison/musk_ox", "camel/llama", "antelope/gemsbok",
                  "rhino/elasmotherium", "boar/babirusa", "hyena/cave_hyena", "bear/short_faced"]
        sheet([(p.split("/")[1], make(*p.split("/"))) for p in picks], ROOT / "scratch/icon_sample.png")


# ================================================================== additions after the first sample
F["ears_large"] = [   # flaps framing the face, painted OVER the head's edge (the head already spans 14 px)
    "................", "................", "................", ".DDD........DDD.",
    "DDDD........DDDD", "DDD..........DDD", "DDD..........DDD", "DDD..........DDD",
    "DDDD........DDDD", ".DD..........DD.", "..D..........D..", "................",
    "................", "................", "................", "................",
]
SPECIES["mammoth/african_elephant"] = ["ears_large", "tusks_curved"]
PALETTE_OVERRIDE["opossum/virginia"] = {"L": "#ece8e0", "S": "#4a4440", "M": "#d99a9a", "D": "#1e1c1a", "K": "#3a3430"}


def compose_with_ears(typ, sp):
    """Ear flaps sit BEHIND the head: head pixels win wherever the head template has paint."""
    feats = SPECIES.get(typ + "/" + sp, TYPE[typ][1])
    if True:   # flaps now paint over the head edge like any overlay
        return compose(typ, sp)
    ears = compose(typ, sp, ["ears_large"])
    head = compose(typ, sp, [f for f in feats if f != "ears_large"])
    out = head.copy()
    for y in range(16):
        for x in range(16):
            if F["ears_large"][y][x] != "." and T[TYPE[typ][0]][y][x] == ".":
                out.putpixel((x, y), ears.getpixel((x, y)))
    return out


# --- existing body types: recolour the closest hand-drawn icon into the species' own skin palette
RECOLOR = {
    "antelope/pronghorn": "antelope/thomson", "antelope/springbok": "antelope/thomson", "antelope/tetrameryx": "antelope/thomson",
    "antelope/giant_eland": "antelope/blackbuck", "antelope/gemsbok": "antelope/impala", "antelope/sable": "antelope/impala",
    "antelope/bluebuck": "antelope/impala",
    "bear/short_faced": "bear/brown", "bear/arctotherium": "bear/spectacled",
    "big_cat/american_lion": "big_cat/cave_lion", "big_cat/homotherium": "big_cat/cave_lion", "big_cat/cheetah": "big_cat/leopard",
    "big_cat/american_cheetah": "big_cat/puma", "big_cat/barbary_lion": "big_cat/lion", "big_cat/caspian_tiger": "big_cat/tiger",
    "big_cat/thylacosmilus": "big_cat/marsupial_lion", "big_cat/smilodon_fatalis": "big_cat/sabertooth",
    "boar/giant_warthog": "boar/warthog", "boar/giant_forest_hog": "boar/red_river", "boar/babirusa": "boar/peccary",
    "boar/kubanochoerus": "boar/warthog", "boar/platygonus": "boar/peccary",
    "hyena/cave_hyena": "hyena/spotted", "rhino/elasmotherium": "rhino/wooly", "rhino/mercks": "rhino/sumatran",
    "camel/camelus_knoblochi": "camel/wild_bactrian",
}
for _s in ("steppe", "long_horned", "wild_yak", "musk_ox"):
    RECOLOR["bison/" + _s] = "bison/plains"
for _s in ("aurochs", "gaur", "banteng", "water_buffalo", "african_buffalo", "giant_buffalo", "pelorovis"):
    RECOLOR["bison/" + _s] = "bison/wisent"
for _s in ("western", "titanotylopus", "guanaco", "vicuna", "paleolama", "llama", "alpaca", "hemiauchenia"):
    RECOLOR["camel/" + _s] = "camel/dromedary"
# 2.9.0 new species on existing bodies (hippo/pigmy.png is an orphan upstream icon: the right shape for the dwarfs)
RECOLOR.update({
    "big_cat/clouded_leopard": "big_cat/leopard",
    "bison/antiquus": "bison/plains", "bison/bootherium": "bison/musk_ox",
    "catfish/shovelnose": "catfish/channel", "catfish/goonch": "catfish/channel", "catfish/wels": "catfish/channel",
    "catfish/piraiba": "catfish/blue", "catfish/mekong": "catfish/blue",
    "hippo/european": "hippo/common", "hippo/pygmy": "hippo/pigmy", "hippo/malagasy_dwarf": "hippo/pigmy",
    "snake/sea_krait": "snake/coral_blue",
    "tortoise/aldabra": "tortoise/sulcata", "tortoise/galapagos": "tortoise/sulcata",
})


def skin_pixels(key):
    typ, sp = key.split("/")
    p = skin_path(typ, sp)
    if p is None:
        cands = sorted((TEX / typ).glob(sp + "*.png"))
        p = cands[0] if cands else None
    if p is None:
        return None
    im = Image.open(p).convert("RGBA")
    return sorted((c[:3] for c in im.getdata() if c[3] > 200), key=lum)


def recolor(target_key):
    import bisect
    ref_key = RECOLOR[target_key]
    icon = Image.open(ICON / (ref_key + ".png")).convert("RGBA")
    ref = skin_pixels(ref_key)
    tgt = skin_pixels(target_key)
    if not tgt:
        return None
    if not ref:
        # icons of species that no longer exist (impala, thomson, dama): rank within the icon itself
        ref = sorted((c[:3] for c in icon.getdata() if c[3] > 0 and 28 <= lum(c) <= 228), key=lum)
    ref_l = [lum(c) for c in ref]
    out = icon.copy()
    for y in range(16):
        for x in range(16):
            c = icon.getpixel((x, y))
            if c[3] == 0:
                continue
            L = lum(c)
            if L < 28 or L > 228 or (c[0] - c[2] > 90 and c[0] > 150 and c[1] < 110):   # eyes, highlights, red mouths
                continue
            q = bisect.bisect_left(ref_l, L) / max(1, len(ref_l) - 1)
            i0, i1 = max(0, int((q - 0.02) * len(tgt))), min(len(tgt), int((q + 0.02) * len(tgt)) + 1)
            chunk = tgt[i0:i1] or [tgt[min(len(tgt) - 1, int(q * len(tgt)))]]
            out.putpixel((x, y), tuple(sum(cc[k] for cc in chunk) // len(chunk) for k in range(3)) + (255,))
    return out


def make(typ, sp):
    key = typ + "/" + sp
    if key in RECOLOR:
        return recolor(key)
    if typ in TYPE:
        return compose_with_ears(typ, sp)
    return None


CATEGORY_FROM = {"antelope": "gemsbok", "deer": "red_deer", "dire_wolf": "dire_wolf", "equid": "przewalskis_horse",
                 "giraffid": "giraffe", "glyptodont": "glyptodon", "ground_sloth": "megatherium", "macrauchenia": "macrauchenia",
                 "mammoth": "woolly", "opossum": "virginia", "tapir": "lowland", "toxodon": "toxodon"}


def build_all():
    OUT.mkdir(parents=True, exist_ok=True)
    made, skipped = [], []
    for typ, sp in species_list():
        if (ICON / typ / (sp + ".png")).exists():
            continue
        img = make(typ, sp)
        if img is None:
            skipped.append(typ + "/" + sp)
            continue
        (OUT / typ).mkdir(exist_ok=True)
        img.save(OUT / typ / (sp + ".png"))
        made.append((typ + "/" + sp, img))
    for typ, sp in CATEGORY_FROM.items():
        if not (ICON / typ / "category.png").exists():
            p = OUT / typ / (sp + ".png")
            src = p if p.exists() else ICON / typ / (sp + ".png")
            if src.exists():
                (OUT / typ).mkdir(exist_ok=True)
                shutil.copy(src, OUT / typ / "category.png")
    return made, skipped





# ================================================================== redraw 2026-10-08 (user: tapir, wolf, camelid "look weird")
T["tapir"] = [   # rounded head, round rimmed ears, small wide-set eyes, pale cheeks/lip, trunk hanging over it
    "................",
    "................",
    "..PPB......BPP..",
    "..PKB......BKP..",
    "...BBBBBBBBBB...",
    "..BBBBBBBBBBBB..",
    "..BBWEBBBBEWBB..",
    "..BBBBBBBBBBBB..",
    "...BBBBBBBBBB...",
    "...LBBBBBBBBL...",
    "....LLBBBBLL....",
    ".....LLBBLL.....",
    ".......BB.......",
    ".......BB.......",
    ".......DB.......",
    ".......DD.......",
]
F["tapir_crest"] = [
    "................", "................", "................", ".......SS.......",
    ".......SS.......", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
]
T["dire_wolf"] = [   # broad skull, upright ears, wide cheek ruff, narrower pale muzzle
    "................",
    "..BB........BB..",
    "..BKB......BKB..",
    "..BKKB....BKKB..",
    "..BBBBBBBBBBBB..",
    ".DBBBBBBBBBBBBD.",
    ".DBBWEBBBBEWBBD.",
    "DBBBBBBBBBBBBBBD",
    "DBLBBBBBBBBBBLBD",
    ".DLLBBBLLBBBLLD.",
    "..DLLBLLLLBLLD..",
    "...DLLLLLLLLD...",
    ".....LLLLLL.....",
    ".....LLMMLL.....",
    "......LMML......",
    ".......LL.......",
]
T["camel"] = [   # true camels: ears out sideways, heavy brow, long face, big drooping lips
    "................",
    "....BBBBBBBB....",
    ".DDBBBBBBBBBBDD.",
    ".DKBBBBBBBBBBKD.",
    "....BBBBBBBB....",
    "....DBBBBBBD....",
    "...WEBBBBBBEW...",
    "....BBBBBBBB....",
    ".....BBBBBB.....",
    ".....BBBBBB.....",
    ".....BBLLBB.....",
    ".....BLLLLB.....",
    "....BLLLLLLB....",
    "....LMLLLLML....",
    "....LLMMMMLL....",
    ".....LLLLLL.....",
]
T["lama"] = [    # lamines: tall banana ears curving in, narrow face, big eyes, split lip
    "....B......B....",
    "...BK......KB...",
    "...BK......KB...",
    "...BB......BB...",
    "....BBBBBBBB....",
    "...BBBBBBBBBB...",
    "...WEBBBBBBEW...",
    "...DBBBBBBBBD...",
    "....BBBBBBBB....",
    ".....BBBBBB.....",
    ".....BBBBBB.....",
    ".....BLLLLB.....",
    ".....LLLLLL.....",
    ".....LMLLML.....",
    "......LMML......",
    "......LLLL......",
]
F["forelock"] = [
    "................", "....SSSSSSSS....", "....SSSSSSSS....", ".....SSSSSS.....",
    "......SSSS......", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
]
F["topknot"] = [
    "................", "................", "................", ".....PPPPPP.....",
    "......qqqq......", "......qqqq......", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
]
TYPE["camel"] = ("camel", [], None)
TEMPLATE_FOR = {"camel/" + s: "lama" for s in ("guanaco", "vicuna", "llama", "alpaca", "paleolama", "hemiauchenia")}
for _s in ("western", "titanotylopus", "guanaco", "vicuna", "paleolama", "llama", "alpaca", "hemiauchenia", "camelus_knoblochi"):
    RECOLOR.pop("camel/" + _s, None)
SPECIES.update({"tapir/lowland": ["tapir_crest"], "tapir/malayan": [], "tapir/palorchestes": [],
                "camel/camelus_knoblochi": ["forelock"], "camel/alpaca": ["topknot"]})
PALETTE_OVERRIDE.update({
    "tapir/malayan": {"P": "#e8e4dc", "L": "#3a3632", "K": "#5a5450"},
    "tapir/bairds": {"L": "#cfc3ae"},
    "tapir/palorchestes": {"P": "#6a5848"},
    "camel/llama": {"B": "#e6dccb", "D": "#bfae94", "L": "#efe8dc", "K": "#8a6a4a", "M": "#4a3a2e"},
    "camel/alpaca": {"B": "#d9c29c", "D": "#b39a72", "L": "#e8dcc4", "P": "#e8dcc4", "K": "#8a6a4a", "M": "#4a3a2e"},
    "camel/guanaco": {"L": "#e9e1d3", "K": "#5a4a3c"},
    "camel/vicuna": {"L": "#f0e8dc"},
})
_compose_orig = compose


def compose(typ, sp, feats=None):
    tname = TEMPLATE_FOR.get(typ + "/" + sp)
    if not tname:
        return _compose_orig(typ, sp, feats)
    saved = TYPE[typ]
    TYPE[typ] = (tname, saved[1], saved[2])
    try:
        return _compose_orig(typ, sp, feats)
    finally:
        TYPE[typ] = saved


T["tapir"] = [   # v2: fuller round head, big oval rimmed ears, dark trunk from mid-face over a pale lip, curled tip
    "................",
    ".PPP........PPP.",
    ".PKB........BKP.",
    ".PBBB......BBBP.",
    "...BBBBBBBBBB...",
    "..BBBBBBBBBBBB..",
    ".BBWEBBBBBBEWBB.",
    ".BBBBBBBBBBBBBB.",
    ".BBBBBBBBBBBBBB.",
    "..BBBBBDDBBBBB..",
    "...LBBBDDBBBL...",
    "....LLBDDBLL....",
    "......LDDL......",
    ".......DD.......",
    ".......DD.......",
    "......DD........",
]
F["tapir_crest"] = [
    "................", "................", "................", "......SSSS......",
    ".......SS.......", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
]
SPECIES["dire_wolf/thylacine"] = []
PALETTE_OVERRIDE["tapir/malayan"] = {"P": "#e8e4dc", "L": "#3a3632", "K": "#5a5450", "D": "#4a4642"}
PALETTE_OVERRIDE["camel/alpaca"]["P"] = "#f4eedf"


def redo_sheet():
    picks = ["tapir/lowland", "tapir/malayan", "tapir/bairds", "tapir/mountain", "tapir/megatapirus", "tapir/vero_tapir",
             "tapir/palorchestes",
             "dire_wolf/dire_wolf", "dire_wolf/pleistocene_wolf", "dire_wolf/gray_wolf", "dire_wolf/coyote", "dire_wolf/dhole",
             "dire_wolf/african_wild_dog", "dire_wolf/maned_wolf", "dire_wolf/xenocyon", "dire_wolf/bush_dog", "dire_wolf/protocyon",
             "dire_wolf/thylacine",
             "camel/western", "camel/titanotylopus", "camel/camelus_knoblochi", "camel/guanaco", "camel/vicuna", "camel/llama",
             "camel/alpaca", "camel/paleolama", "camel/hemiauchenia"]
    sheet([(p.split("/")[1], make(*p.split("/"))) for p in picks], ROOT / "scratch/icon_redo.png", S=8, cols=7)


# ================================================================== diprotodont (Phase 4.1, 2026-10-09)
T["diprotodont"] = [   # broad wombat face: small round ears, wide cheeks, small high eyes, big dark nose pad, incisors
    "................",
    "................",
    "..DDD......DDD..",
    "..DKBBBBBBBBKD..",
    ".BBBBBBBBBBBBBB.",
    ".BBBBBBBBBBBBBB.",
    "BBBWEBBBBBBEWBBB",
    "BBBBBBBBBBBBBBBB",
    "DBBBBBBBBBBBBBBD",
    ".DBBBLLLLLLBBBD.",
    "..BBLLMMMMLLBB..",
    "..BLLMMMMMMLLB..",
    "..BLLLMMMMLLLB..",
    "...BLLLLLLLLB...",
    "....DLLHHLLD....",
    "................",
]   # v2: the first nose pad (rows 10-12 at 6-8 wide over the teeth) read as an open mouth
F["dip_boss"] = [      # zygomaturus: the raised bare nasal area above the nose pad
    "................", "................", "................", "................",
    "................", "................", "................", "................",
    "......SSSS......", ".....SSSSSS.....", "................", "................",
    "................", "................", "................", "................",
]
F["dip_mask"] = [      # nototherium: dark face mask round the eyes and down the muzzle bridge
    "................", "................", "................", "................",
    "................", "..SSSS....SSSS..", "..S..S....S..S..", "..SSSSSSSSSSSS..",
    "....SSSSSSSS....", ".....SSSSSS.....", "................", "................",
    "................", "................", "................", "................",
]
TYPE["diprotodont"] = ("diprotodont", [], "ivory")
SPECIES.update({"diprotodont/zygomaturus": ["dip_boss"], "diprotodont/nototherium": ["dip_mask"]})
PALETTE_OVERRIDE.update({
    # the skin's luminance quantiles describe the near-black BODY; the icon is the pale HEAD
    "diprotodont/hulitherium": {"B": "#857b6d", "D": "#3a3530", "L": "#9a9080", "K": "#2a2622"},
    "diprotodont/nototherium": {"S": "#5f4a35"},
})
CATEGORY_FROM["diprotodont"] = "diprotodon"


def dip_sheet():
    picks = ["diprotodont/" + s for s in ("diprotodon", "zygomaturus", "nototherium", "hulitherium", "maokopia")]
    picks += ["tapir/lowland", "glyptodont/glyptodon", "ground_sloth/megatherium"]
    sheet([(p.split("/")[1], make(*p.split("/"))) for p in picks], ROOT / "scratch/icon_dip.png", S=8, cols=8)


if __name__ == "__main__" and "--dip" in sys.argv:
    dip_sheet()

# ================================================================== kangaroo (Phase 4.2, 2026-10-10)
T["kangaroo"] = [   # v2: long PARALLEL round-tipped ears (v1's short flared ears read as a fox), long tapering muzzle, small high nose
    "...DD......DD...",
    "..DKKD....DKKD..",
    "..DKKD....DKKD..",
    "..DKKD....DKKD..",
    "..DKKB....BKKD..",
    "...BBBBBBBBBB...",
    "..BBBBBBBBBBBB..",
    "..BWEBBBBBBEWB..",
    "..BBBBBBBBBBBB..",
    "...BBBBBBBBBB...",
    "....BBBBBBBB....",
    "....BLLLLLLB....",
    ".....LMMMML.....",
    "......LMML......",
    ".......LL.......",
    "................",
]
F["roo_whisker"] = [   # red kangaroo / protemnodon: pale cheek stripe
    "................", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "....P......P....",
    ".....P....P.....", "................", "................", "................",
]
TYPE["kangaroo"] = ("kangaroo", [], None)
SPECIES.update({"kangaroo/red": ["roo_whisker"], "kangaroo/protemnodon": ["roo_whisker"]})
PALETTE_OVERRIDE.update({
    # the skins' luminance quantiles are dominated by the pale belly; pin the head colours to the painted face
    "kangaroo/red": {"B": "#a5603e", "D": "#7e3c22", "L": "#c9a68a", "P": "#efe8dc", "K": "#d8b8a4", "M": "#1e1a18"},
    "kangaroo/eastern_grey": {"B": "#857b71", "D": "#5e554c", "L": "#b2a99d", "K": "#c9beb2", "M": "#1e1a18"},
    "kangaroo/procoptodon": {"B": "#a8875a", "D": "#7e6242", "L": "#4d3a2a", "K": "#d9c6a6", "M": "#1e1a18"},
    "kangaroo/sthenurus": {"B": "#4a3426", "D": "#3a2a1f", "L": "#33251b", "K": "#8f725a", "M": "#141110"},
    "kangaroo/protemnodon": {"B": "#5a544e", "D": "#4a4540", "L": "#8a8176", "P": "#cfc6b8", "K": "#b0a497", "M": "#1e1a18"},
})
CATEGORY_FROM["kangaroo"] = "red"


def roo_sheet():
    picks = ["kangaroo/" + s for s in ("red", "eastern_grey", "procoptodon", "sthenurus", "protemnodon")]
    picks += ["diprotodont/diprotodon", "tapir/lowland", "deer/red_deer"]
    sheet([(p.split("/")[1], make(*p.split("/"))) for p in picks], ROOT / "scratch/icon_roo.png", S=8, cols=8)


if __name__ == "__main__" and "--roo" in sys.argv:
    roo_sheet()

# ================================================================== giant monitor (Phase 4.3, 2026-10-10)
# recoloured from the hand-drawn monitor icons, so the new type sits in the same style as its small relatives
RECOLOR.update({"giant_monitor/megalania": "monitor/water", "giant_monitor/komodo": "monitor/rock"})
CATEGORY_FROM["giant_monitor"] = "megalania"
# the reference icons' PINK tongue is recoloured to khaki by the luminance ranking; paint it back in the species' own
# tongue colour (the skins: komodo yellow, megalania dark)
GM_TONGUE = {"giant_monitor/megalania": (74, 44, 44), "giant_monitor/komodo": (216, 176, 48)}
_make_orig = make


def make(typ, sp):
    img = _make_orig(typ, sp)
    key = typ + "/" + sp
    if img is not None and key in GM_TONGUE:
        ref = Image.open(ICON / (RECOLOR[key] + ".png")).convert("RGBA")
        for y in range(16):
            for x in range(16):
                r, g, b, a = ref.getpixel((x, y))
                if a and r - b > 35 and r > 150 and r >= g:          # the pink tongue in the hand-drawn icon
                    img.putpixel((x, y), GM_TONGUE[key] + (255,))
    return img


def gm_sheet():
    picks = ["giant_monitor/megalania", "giant_monitor/komodo", "monitor/water", "monitor/rock", "monitor/nile"]
    items = []
    for p in picks:
        t, n = p.split("/")
        img = make(t, n) if t == "giant_monitor" else Image.open(ICON / (p + ".png")).convert("RGBA")
        items.append((n, img))
    sheet(items, ROOT / "scratch/icon_gm.png", S=8, cols=5)


if __name__ == "__main__" and "--gm" in sys.argv:
    gm_sheet()

# ================================================================== pinniped (Phase 4.4, 2026-10-10)
T["pinniped"] = [   # round head, big dark eyes, pale muzzle with a small dark nose
    "................",
    "................",
    "................",
    ".....BBBBBB.....",
    "...BBBBBBBBBB...",
    "..BBBBBBBBBBBB..",
    "..BEEBBBBBBEEB..",
    "..BEWBBBBBBWEB..",
    "..BBBBBBBBBBBB..",
    "..BBBBLLLLBBBB..",
    "..BBBLLMMLLBBB..",
    "...BLLLLLLLLB...",
    "...BLLLLLLLLB...",
    "....BBLLLLBB....",
    "......BBBB......",
    "................",
]
F["pin_whiskers"] = [   # walrus / bearded seal: the bristle pad
    "................", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "...P..PP..P.....".replace("P..PP..P", "PP....PP"), "..PPPPPPPPPPPP..",
    "...PPPPPPPPPP...", "................", "................", "................",
]
F["pin_tusks"] = [      # walrus
    "................", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "......H..H......", "......H..H......", "......h..h......",
]
F["pin_ears"] = [       # sea lion ear flaps
    "................", "................", "................", "...D........D...",
    "..DD........DD..", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
]
TYPE["pinniped"] = ("pinniped", [], "ivory")
SPECIES.update({"pinniped/walrus": ["pin_whiskers", "pin_tusks"], "pinniped/bearded_seal": ["pin_whiskers"],
                "pinniped/steller_sea_lion": ["pin_ears"]})
PALETTE_OVERRIDE.update({
    # pinned to the painted HEAD colours (the skins' luminance quantiles describe the body)
    "pinniped/walrus": {"B": "#946049", "D": "#6a4434", "L": "#b47d66", "P": "#e2d3b4", "M": "#1d1a18"},
    "pinniped/bearded_seal": {"B": "#7a716a", "D": "#5e5650", "L": "#9c948a", "P": "#ddd4c4", "M": "#1d1a18"},
    "pinniped/ringed_seal": {"B": "#55585b", "D": "#3a3c3e", "L": "#8a8e90", "M": "#141516"},
    "pinniped/harp_seal": {"B": "#232426", "D": "#161718", "L": "#3a3b3d", "E": "#060606", "W": "#cfd1cf", "M": "#060606"},
    "pinniped/steller_sea_lion": {"B": "#9a774c", "D": "#6e5436", "L": "#c09a6c", "M": "#1d1a18"},
})
CATEGORY_FROM["pinniped"] = "harp_seal"


def pin_sheet():
    picks = ["pinniped/" + s for s in ("walrus", "bearded_seal", "ringed_seal", "harp_seal", "steller_sea_lion")]
    picks += ["kangaroo/red", "giant_monitor/komodo"]
    sheet([(q.split("/")[1], make(*q.split("/"))) for q in picks], ROOT / "scratch/icon_pin.png", S=8, cols=7)


if __name__ == "__main__" and "--pin" in sys.argv:
    pin_sheet()

# ================================================================== caprine (Phase 4.5, 2026-10-10)
T["caprine"] = [   # narrow goat face, ears out to the sides, amber eyes, pale muzzle, small dark nose
    "................",
    "................",
    "................",
    "................",
    ".....BBBBBB.....",
    "DDD.BBBBBBBB.DDD",
    ".DDBBBBBBBBBBDD.",
    "...BEWBBBBWEB...",
    "...BBBBBBBBBB...",
    "....BBBBBBBB....",
    "....BBBLLBBB....",
    ".....BLLLLB.....",
    ".....LLLLLL.....",
    ".....LLMMLL.....",
    "......LLLL......",
    "................",
]
F["cap_ibex"] = [      # v3: seen from the FRONT an ibex's horns rise close together, near-parallel and knobbed (v1 thin
    #                      and v2 a wide V both read as ANTLERS beside the ears)
    "....HH....HH....", "....hH....Hh....", ".....HH..HH.....", ".....hH..Hh.....",
    ".....HH..HH.....", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
]
F["cap_ram"] = [       # heavy curls framing the face
    "................", "................", "....HHH..HHH....", "..HHhHH..HHhHH..",
    ".HHH........HHH.", ".HH..........HH.", ".HHh........hHH.", "..HH........HH..",
    "...H........H...", "................", "................", "................",
    "................", "................", "................", "................",
]
F["cap_chamois"] = [   # short hooks
    "................", "................", "......H..H......", ".....HH..HH.....",
    "......H..H......", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
]
F["cap_dagger"] = [    # short black daggers
    "................", "................", "......H..H......", "......H..H......",
    "......H..H......", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
]
F["cap_beard"] = [
    "................", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "................", "................",
    "................", "................", "......SSSS......", ".......SS.......",
]
F["cap_stripe"] = [    # chamois: dark stripe through each eye on the pale face
    "................", "................", "................", "................",
    "................", "................", "................", "...S......S.....".replace("S......S", "SS....SS"),
    "....S......S....", ".....S....S.....", "................", "................",
    "................", "................", "................", "................",
]
TYPE["caprine"] = ("caprine", [], "keratin")
SPECIES.update({"caprine/alpine_ibex": ["cap_ibex", "cap_beard"], "caprine/siberian_ibex": ["cap_ibex", "cap_beard"],
                "caprine/argali": ["cap_ram"], "caprine/bighorn": ["cap_ram"],
                "caprine/chamois": ["cap_chamois", "cap_stripe"], "caprine/harrington_goat": ["cap_dagger", "cap_beard"]})
PALETTE_OVERRIDE.update({
    "caprine/alpine_ibex": {"B": "#6a5d4c", "D": "#4a4034", "L": "#8a7c66", "H": "#7a6c58", "h": "#9a8a72", "S": "#3a322a",
                            "E": "#b08a3a", "W": "#0e0c0a", "M": "#1f1b18"},
    "caprine/siberian_ibex": {"B": "#806a4c", "D": "#5a4935", "L": "#a08a68", "H": "#5a4c3c", "h": "#7a6a56", "S": "#2e271f",
                              "E": "#b08a3a", "W": "#0e0c0a", "M": "#1f1b18"},
    "caprine/argali": {"B": "#7e6c56", "D": "#5a4a3a", "L": "#d8d0c0", "H": "#b49a74", "h": "#d0bc98",
                       "E": "#b08a3a", "W": "#0e0c0a", "M": "#1f1b18"},
    "caprine/bighorn": {"B": "#6e5038", "D": "#4e3826", "L": "#e2dacb", "H": "#9a7e58", "h": "#b89c74",
                        "E": "#b08a3a", "W": "#0e0c0a", "M": "#1f1b18"},
    "caprine/chamois": {"B": "#e0d2b4", "D": "#9c7a50", "L": "#e6dac0", "H": "#1c1916", "h": "#3a3530", "S": "#2c2219",
                        "E": "#b08a3a", "W": "#0e0c0a", "M": "#1f1b18"},
    "caprine/harrington_goat": {"B": "#ece8de", "D": "#d0cbc0", "L": "#e8e4da", "H": "#1a1816", "h": "#3a3634", "S": "#f2efe8",
                                "E": "#b08a3a", "W": "#0e0c0a", "M": "#1f1b18"},
})
CATEGORY_FROM["caprine"] = "alpine_ibex"


def cap_sheet():
    picks = ["caprine/" + s for s in ("alpine_ibex", "siberian_ibex", "argali", "bighorn", "chamois", "harrington_goat")]
    picks += ["deer/red_deer"]
    sheet([(q.split("/")[1], make(*q.split("/"))) for q in picks], ROOT / "scratch/icon_cap.png", S=8, cols=7)


if __name__ == "__main__" and "--cap" in sys.argv:
    cap_sheet()

# ================================================================== large rodent (Phase 4.6, 2026-10-10)
T["large_rodent"] = [   # capybara: a tall blunt box of a face, tiny round ears up top, eyes set high, deep square muzzle
    "................",
    "................",
    "...DD......DD...",
    "...DDBBBBBBDD...",
    "....BBBBBBBB....",
    "...BBBBBBBBBB...",
    "...BEWBBBBWEB...",
    "...BBBBBBBBBB...",
    "....BBBBBBBB....",
    "....BBLLLLBB....",
    "....BLLLLLLB....",
    "....LLMMMMLL....",
    "....LLLMMLLL....",
    "....LLLLLLLL....",
    ".....LLLLLL.....",
    "................",
]
F["rod_beaver"] = [     # beavers: rounder, wider cheeks and the orange incisors showing below the lip
    "................", "................", "................", "................",
    "................", "................", "................", "..B..........B..",
    "..BB........BB..", "...B........B...", "................", "................",
    "................", "................", "................", "......TTTT......",
]
F["rod_castoroides"] = [  # the giant beaver: the same face with long incisors
    "................", "................", "................", "................",
    "................", "................", "................", "..B..........B..",
    "..BB........BB..", "...B........B...", "................", "................",
    "................", "................", "......TTTT......", "......TTTT......",
]
TYPE["large_rodent"] = ("large_rodent", [], None)
SPECIES.update({"large_rodent/beaver": ["rod_beaver"], "large_rodent/castoroides": ["rod_castoroides"]})
_ROD_EYE = {"E": "#0e0b09", "W": "#7d6c55", "T": "#d8892a"}
PALETTE_OVERRIDE.update({
    "large_rodent/capybara": dict(_ROD_EYE, B="#8a6440", D="#5e4129", L="#6e4c30", M="#201a16"),
    "large_rodent/neochoerus": dict(_ROD_EYE, B="#6a5643", D="#463729", L="#54432f", M="#201a16"),
    "large_rodent/castoroides": dict(_ROD_EYE, B="#583d2b", D="#3a281c", L="#4e3626", M="#201a16"),
    "large_rodent/beaver": dict(_ROD_EYE, B="#7e5232", D="#4a3020", L="#946a48", M="#201a16"),
})
CATEGORY_FROM["large_rodent"] = "capybara"


def rod_sheet():
    picks = ["large_rodent/" + s for s in ("capybara", "neochoerus", "castoroides", "beaver")]
    picks += ["tapir/lowland", "caprine/alpine_ibex"]
    sheet([(q.split("/")[1], make(*q.split("/"))) for q in picks], ROOT / "scratch/icon_rod.png", S=8, cols=6)


if __name__ == "__main__" and "--rod" in sys.argv:
    rod_sheet()

# ================================================================== wildebeest (Phase 4.7, 2026-10-23)
T["wildebeest"] = [   # a long boxy face seen from the front, ears out sideways BELOW the horns, a broad dark muzzle
    "................",
    "................",
    "................",
    "................",
    "......BBBB......",
    ".....BBBBBB.....",
    "DD..BBBBBBBB..DD",
    ".DDDBEWBBWEBDDD.",
    "....BBBBBBBB....",
    ".....BBBBBB.....",
    ".....BBBBBB.....",
    ".....LLLLLL.....",
    "....LLLLLLLL....",
    "....LMMLLMML....",
    "....LLLLLLLL....",
    ".....LLLLLL.....",
]
_E = "................"
F["wb_gnu"] = [        # blue wildebeest: a boss across the brow, the horns out, DOWN, then up -- cow-like
    _E, "H..............H", "H..............H", "H..............H", "HH.HHHHHHHHHH.HH", ".HHH........HHH.",
    _E, _E, _E, _E, _E, _E, _E, _E, _E, _E]
F["wb_gnou"] = [       # black wildebeest: hooks running down beside the face, then up
    _E, _E, "...H........H...", "..HH........HH..", "..H.HHHHHHHH.H..", "..H..........H..", "..HH........HH..",
    _E, _E, _E, _E, _E, _E, _E, _E, _E]
F["wb_mane_tuft"] = [  # the black wildebeest's upright cream mane, seen over the brow
    _E, _E, _E, "......CCCC......", _E, _E, _E, _E, _E, _E, _E, _E, _E, _E, _E, _E]
F["wb_nose_tuft"] = [  # and its bristly nose tuft
    _E, _E, _E, _E, _E, _E, _E, _E, _E, _E, _E, ".....KKKKKK.....", _E, _E, _E, _E]
F["wb_megalo"] = [     # Megalotragus: very long, THICK horns sweeping out and up (1 px wide read as antlers)
    "HH............HH", "HHh..........hHH", ".HHh........hHH.", "..HHh......hHH..", "...HHHHHHHHHH...",
    _E, _E, _E, _E, _E, _E, _E, _E, _E, _E, _E]
F["wb_hart"] = [       # hartebeest: THICK bracket horns on the bony pedicle
    "...HH......HH...", "....HH....HH....", ".....HH..HH.....", "......qqqq......", "......qqqq......",
    _E, _E, _E, _E, _E, _E, _E, _E, _E, _E, _E]
F["wb_blaze"] = [      # the dark blaze down the hartebeest's face
    _E, _E, _E, _E, _E, ".......SS.......", ".......SS.......", _E, ".......SS.......", ".......SS.......",
    ".......SS.......", _E, _E, _E, _E, _E]
TYPE["wildebeest"] = ("wildebeest", [], None)
SPECIES.update({"wildebeest/blue_wildebeest": ["wb_gnu"],
                "wildebeest/black_wildebeest": ["wb_gnou", "wb_mane_tuft", "wb_nose_tuft"],
                "wildebeest/megalotragus": ["wb_megalo"], "wildebeest/hartebeest": ["wb_hart", "wb_blaze"]})
_WB_EYE = {"E": "#0e0c0a", "W": "#5a4a3a"}
PALETTE_OVERRIDE.update({
    "wildebeest/blue_wildebeest": dict(_WB_EYE, B="#3c3f42", D="#4c5154", L="#232221", M="#121110", H="#8a8a86", h="#a8a8a2"),
    "wildebeest/black_wildebeest": dict(_WB_EYE, B="#2a231e", D="#352c25", L="#1c1815", M="#0e0c0a", H="#2e2a26",
                                        h="#4a4440", C="#e2d8c2", K="#141210"),
    "wildebeest/megalotragus": dict(_WB_EYE, B="#7e6e58", D="#8c7b63", L="#4a4036", M="#1e1a16", H="#a89a80", h="#c8baa0"),
    "wildebeest/hartebeest": dict(_WB_EYE, B="#a8714a", D="#b07a52", L="#8a5a3a", M="#1e1814", S="#3a2a1e", H="#1e1b18",
                                  h="#3a3530", q="#8a5a3a"),
})
CATEGORY_FROM["wildebeest"] = "blue_wildebeest"


def wb_sheet():
    picks = ["wildebeest/" + s for s in ("blue_wildebeest", "black_wildebeest", "megalotragus", "hartebeest")]
    items = [(q.split("/")[1], make(*q.split("/"))) for q in picks]
    items.append(("impala (existing)", Image.open(ICON / "antelope/impala.png").convert("RGBA")))
    items.append(("argali", make("caprine", "argali")))
    items.append(("bison (existing)", Image.open(ICON / "bison/plains.png").convert("RGBA")))
    sheet(items, ROOT / "scratch/icon_wb.png", S=8, cols=7)


if __name__ == "__main__" and "--wb" in sys.argv:
    wb_sheet()

# ================================================================== crocodilian (Phase 4.8, 2026-11-14)
# TOP-DOWN head pointing down, like the hand-drawn monitor / snake icons: wide skull with the gold eyes on top, a long
# tapering snout with the teeth showing along its edges, the nostrils at the tip.
T["crocodilian"] = [
    "................",
    "......DDDD......",
    ".....DBBBBD.....",
    "....DBBBBBBD....",
    "....BEQBBQEB....",
    "....DBBBBBBD....",
    ".....BBBBBB.....",
    ".....BBBBBB.....",
    ".....BBDDBB.....",
    ".....BBBBBB.....",
    ".....BBBBBB.....",
    "......BBBB......",
    "......BBBB......",
    "......BNNB......",
    ".......BB.......",
    "................",
]
_E = "................"
F["croc_broad"] = [    # a broad snout (mugger, black caiman, Voay): one more column on each side down the snout
    _E, _E, _E, _E, _E, _E, "....B......B....", "....B......B....", "....B......B....", "....B......B....",
    "....B......B....", ".....B....B.....", ".....B....B.....", ".....B....B.....", _E, _E]
F["croc_teeth"] = [    # tooth tips at the very edge of the snout only (the top view shows little of them)
    _E, _E, _E, _E, _E, _E, _E, _E, ".....T....T.....", _E, ".....T....T.....", _E, _E, _E, _E, _E]
F["croc_teeth_broad"] = [
    _E, _E, _E, _E, _E, _E, _E, _E, "....T......T....", _E, "....T......T....", _E, _E, _E, _E, _E]
F["croc_horns"] = [    # Voay's horns at the back corners of the skull
    _E, "...H........H...", "...hH......Hh...", _E, _E, _E, _E, _E, _E, _E, _E, _E, _E, _E, _E, _E]
TYPE["crocodilian"] = ("crocodilian", [], None)
SPECIES.update({"crocodilian/nile": ["croc_teeth"], "crocodilian/saltwater": ["croc_teeth"],
                "crocodilian/quinkana": ["croc_teeth"],
                "crocodilian/mugger": ["croc_broad", "croc_teeth_broad"], "crocodilian/black_caiman": ["croc_broad", "croc_teeth_broad"],
                "crocodilian/voay": ["croc_broad", "croc_teeth_broad", "croc_horns"]})
_CROC = {"E": "#bba63a", "Q": "#0c0b09", "T": "#e6dcc0", "N": "#141210"}
PALETTE_OVERRIDE.update({
    "crocodilian/nile": dict(_CROC, B="#6c5e3a", D="#4a4128"),
    "crocodilian/saltwater": dict(_CROC, B="#383d34", D="#24271f"),
    "crocodilian/mugger": dict(_CROC, B="#605b4e", D="#46423a"),
    "crocodilian/black_caiman": dict(_CROC, B="#272724", D="#3e3d36"),
    "crocodilian/quinkana": dict(_CROC, B="#704f31", D="#4a3420"),
    "crocodilian/voay": dict(_CROC, B="#544433", D="#3a3026", H="#b4a686", h="#8c8068"),
})
CATEGORY_FROM["crocodilian"] = "nile"
_make_before_croc = make


def make(typ, sp):
    img = _make_before_croc(typ, sp)
    if img is not None and typ + "/" + sp == "crocodilian/quinkana":
        # Quinkana's NARROW snout: cut the outer column of the snout (rows 7-13) back to one tooth row wide
        for y in range(7, 14):
            row = [x for x in range(16) if img.getpixel((x, y))[3] > 0]
            if len(row) > 4:
                img.putpixel((row[0], y), (0, 0, 0, 0))
                img.putpixel((row[-1], y), (0, 0, 0, 0))
                t = (230, 220, 192, 255) if y in (8, 10) else img.getpixel((row[1], y))
                img.putpixel((row[1], y), t)
                img.putpixel((row[-2], y), t)
    return img


def croc_sheet():
    picks = ["crocodilian/" + s for s in ("nile", "saltwater", "mugger", "black_caiman", "quinkana", "voay")]
    items = [(q.split("/")[1], make(*q.split("/"))) for q in picks]
    items.append(("monitor (existing)", Image.open(ICON / "monitor/nile.png").convert("RGBA")))
    sheet(items, ROOT / "scratch/icon_croc.png", S=8, cols=7)


if __name__ == "__main__" and "--croc" in sys.argv:
    croc_sheet()

# ================================================================== proboscideans, rebuilt rig (2026-11-16)
# Front-view heads built per species from the SAME families the model uses (proboscidean_spec.py): head top, ear size,
# tusk shape and size, coat. The July icons were one head with three tusk overlays; the African's big ears vanished
# into its dark grey (ear flaps are now outlined and lit so they read on any skin).
_PB_TOP = {
    "DOME":  ["................", "......DDDD......", ".....DBBBBD....."],
    "TWIN":  ["................", ".....DD..DD.....", "....DBBDDBBD...."],
    "SLOPE": ["................", "................", "......DDDD......"],
    "CREST": ["................", "....DDDDDDDD....", "....DLLLLLLD...."],
    "FLAT":  ["................", "................", "................"],
}
_PB_FACE = [
    "...DBBBBBBBBD...",
    "...BBBBBBBBBB...",
    "...BDBBBBBBDB...",
    "...BWEBBBBEWB...",
    "...BBBBBBBBBB...",
    "....BDBBBBDB....",
    ".....DBBBBD.....",
    "......BBBB......",
    "......DBBD......",
    "......BBBB......",
    "......DBBD......",
    "......BBBB......",
    ".......BB.......",
]
_PB_EARS = {   # left half only (cols 0-2), mirrored; rows 3..12
    0: ["...", "..D", "..B", "..D", "...", "...", "...", "...", "...", "..."],
    1: ["..D", ".DB", ".DL", ".DB", ".DB", "..D", "...", "...", "...", "..."],
    2: [".DD", "DLB", "DLB", "DLB", "DLB", "DLB", "DBB", ".DB", "..D", "..."],
}
_PB_TUSK = {   # rows 10..15, left half cols 0-7 (mirrored); (large, small) variants
    "SPIRAL":   ([".......", ".....H.", "....H..", "...H...", "...hH..", "....hH."],
                 [".......", ".....H.", "....H..", "....hH.", ".......", "......."]),
    "FORWARD":  ([".....H.", ".....H.", "....H..", "....h..", ".......", "......."],
                 [".....H.", ".....H.", "....H..", "....h..", ".......", "......."]),
    "STRAIGHT": ([".....H.", ".....H.", ".....H.", ".....H.", ".....H.", ".....h."],
                 [".....H.", ".....H.", ".....h.", ".......", ".......", "......."]),
    "UPCURVE":  ([".....H.", ".....H.", "....H..", "....H..", "...H...", "...h..."],
                 [".....H.", "....H..", "....H..", "...h...", ".......", "......."]),
    "TWIST":    ([".....H.", "....H..", "...H...", "..H....", "..hH...", "...h..."],
                 [".....H.", "....H..", "...H...", "..hH...", ".......", "......."]),
    "SMALL":    ([".....H.", ".....h.", ".......", ".......", ".......", "......."],
                 [".....H.", ".....h.", ".......", ".......", ".......", "......."]),
    # Stegodon: close beside the trunk, then converging IN FRONT of it (col 6 overdraws the trunk's edge)
    "STEGO":    ([".....H.", ".....H.", ".....H.", ".....h.", "......H", "......h"],
                 [".....H.", ".....H.", "......H", "......h", ".......", "......."]),
}
_PB_FAMILY = {}
try:
    import importlib as _il
    sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
    _PS = _il.import_module("proboscidean_spec")
    for _sp in _PS.SPECIES:
        _PB_FAMILY[_sp[0]] = dict(head=_sp[3], tusk=_sp[5], large=_sp[6], coat=_sp[7], ear=_sp[8])
except Exception as _e:   # the spec is the source of truth; without it the July icon path is used
    print("proboscidean_spec unavailable:", _e)


def _pb_grid(fam):
    g = [list(r) for r in _PB_TOP[fam["head"]] + _PB_FACE]
    for k, row in enumerate(_PB_EARS[fam["ear"]]):
        y = 3 + k
        for x, ch in enumerate(row):
            if ch != ".":
                g[y][x] = ch
                g[y][15 - x] = ch
    tk = _PB_TUSK[fam["tusk"]][0 if fam["large"] else 1]
    for k, row in enumerate(tk):
        y = 10 + k
        for x, ch in enumerate(row):
            if ch != ".":
                g[y][x] = ch
                g[y][15 - x] = ch
    return ["".join(r) for r in g]


def probo_icon(sp):
    fam = _PB_FAMILY[sp]
    grid = _pb_grid(fam)
    pal = palette("mammoth", sp)
    pal["H"], pal["h"] = hx(MATERIAL["ivory"][0]), hx(MATERIAL["ivory"][1])
    if sp == "asian_elephant":
        pal["P"] = hx("#c9a397")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), pal[ch] + (255,))
    if fam["coat"]:
        for y, row in enumerate(F["mammoth_wool"]):
            for x, ch in enumerate(row):
                if ch != "." and img.getpixel((x, y))[3]:
                    img.putpixel((x, y), pal["S"] + (255,))
    if sp == "asian_elephant":   # pink freckles on the trunk base and lower face
        for x, y in ((6, 10), (9, 11), (7, 12), (5, 8), (10, 8), (8, 9)):
            if img.getpixel((x, y))[3]:
                img.putpixel((x, y), pal["P"] + (255,))
    return img


_make_before_probo = make


def make(typ, sp):
    if typ == "mammoth" and sp in _PB_FAMILY:
        return probo_icon(sp)
    return _make_before_probo(typ, sp)


def probo_sheet():
    items = [(sp, make("mammoth", sp)) for sp in _PB_FAMILY]
    sheet(items, ROOT / "scratch/icon_probo.png", S=8, cols=6)


if __name__ == "__main__" and "--probo" in sys.argv:
    probo_sheet()

# ================================================================== ground sloths, rebuilt rig (2026-11-16)
# Front-view heads per HEAD FAMILY (ground_sloth_spec.py headShape), colours from the new skins. N = bare dark nose skin.
_GS_HEAD = {
    "MEGA": [          # Megatherium / Eremotherium: tall narrow skull, prehensile lip, very deep jaw
        "................",
        "......DDDD......",
        ".....DBBBBD.....",
        "...DDBBBBBBDD...",
        "....BBBBBBBB....",
        "...DBWEBBEWBD...",
        "....BBBBBBBB....",
        "....DBBBBBBD....",
        ".....BBBBBB.....",
        ".....BBBBBB.....",
        ".....DBBBBD.....",
        "......NNNN......",
        "......NMMN......",
        ".....DBBBBD.....",
        ".....DBBBBD.....",
        "......DDDD......",
    ],
    "MYLO": [          # Glossotherium: broad head, square dark muzzle
        "................",
        "................",
        "..DD.DBBBBD.DD..",
        "..DBBBBBBBBBBD..",
        "..BBBBBBBBBBBB..",
        "..BBWEBBBBEWBB..",
        "..DBBBBBBBBBBD..",
        "..BBBBBBBBBBBB..",
        "..DBBBBBBBBBBD..",
        "...BBNNNNNNBB...",
        "...BBBBMMBBBB...",
        "...DBBBBBBBBD...",
        "....DBBBBBBD....",
        ".....DDDDDD.....",
        "................",
        "................",
    ],
    "MEGALONYCH": [    # Megalonyx / Megalocnus: short, blunt, round
        "................",
        "................",
        "...DD.DDDD.DD...",
        "...DBBBBBBBBD...",
        "..DBBBBBBBBBBD..",
        "..BBBBBBBBBBBB..",
        "..BBWEBBBBEWBB..",
        "..DBBBBBBBBBBD..",
        "...BBBNNNNBBB...",
        "...DBBNMMNBBD...",
        "....DBBBBBBD....",
        ".....DDDDDD.....",
        "................",
        "................",
        "................",
        "................",
    ],
}
_GS_FAMILY = {}
try:
    import importlib as _il2
    sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
    _GSS = _il2.import_module("ground_sloth_spec")
    for _sp in _GSS.SPECIES:
        _GS_FAMILY[_sp[0]] = _sp[3]
except Exception as _e2:
    print("ground_sloth_spec unavailable:", _e2)


def sloth_icon(sp):
    grid = _GS_HEAD[_GS_FAMILY[sp]]
    pal = palette("ground_sloth", sp)
    pal["N"] = hx("#2c2420")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), pal[ch] + (255,))
    return img


_make_before_sloth = make


def make(typ, sp):
    if typ == "ground_sloth" and sp in _GS_FAMILY:
        return sloth_icon(sp)
    return _make_before_sloth(typ, sp)


# ================================================================== sea krait (2.9.0)
# Drawn from the blue coral snake icon by a HAND colour map, not the luminance recolour: that keeps the coral snake's
# red head and tail (the red-mouth guard) and turns the body near-black. Laticauda: blue-grey with black rings, a
# black head with a yellow snout/lip, a paddle tail.
_KRAIT = {"282835": "7f93ab", "21212a": "6a7d95", "131319": "16161c", "0d0d11": "16161c",
          "5399e9": "d8d6b8", "3e80d1": "c4c3a2", "2c58b7": "a5a688", "000000": "000000"}
_KRAIT_RINGS = {(6, 5), (3, 7), (6, 8), (11, 8), (11, 10)}


def krait_icon():
    src = Image.open(ICON / "snake/coral_blue.png").convert("RGBA")
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            c = src.getpixel((x, y))
            if c[3] == 0:
                continue
            h = "%02x%02x%02x" % c[:3]
            if h in ("8e0b0b", "d31919"):          # the coral snake's red head (rows 0-3) and tail (rows 12-15)
                if y <= 3:
                    n = "16161c" if h == "8e0b0b" else ("c9b75a" if y == 3 else "3b3e48")
                else:
                    n = "16161c" if (h == "8e0b0b" and y == 15) or y == 13 else ("6a7d95" if h == "8e0b0b" else "7f93ab")
            else:
                n = "16161c" if (x, y) in _KRAIT_RINGS else _KRAIT.get(h, h)
            out.putpixel((x, y), hx(n) + (255,))
    return out


_make_before_krait = make


# Meiolania: the tortoise icons are top-down shells with no head, so the recoloured sulcata gets the two features that
# identify it drawn on -- a head with outswept horns at the front, the ringed tail and its club at the back
RECOLOR["tortoise/meiolania"] = "tortoise/sulcata"
_MEIO = {"K": "#3a3730", "H": "#b9aa86", "h": "#6e6248", "C": "#a1916c", "c": "#6c5f45"}
_MEIO_PIX = [(6, 1, "K"), (7, 1, "K"), (8, 1, "K"), (9, 1, "K"), (7, 0, "K"), (8, 0, "K"),
             (5, 1, "h"), (4, 0, "H"), (10, 1, "h"), (11, 0, "H"),
             (7, 13, "c"), (8, 13, "c"), (7, 14, "C"), (8, 14, "C"), (6, 15, "C"), (7, 15, "c"), (8, 15, "c"), (9, 15, "C")]


def meiolania_icon():
    img = recolor("tortoise/meiolania")
    for x, y, k in _MEIO_PIX:
        img.putpixel((x, y), hx(_MEIO[k]) + (255,))
    return img


# Toothed whales (2.9.0, new type): a front-view head like every other portrait, drawn by hand because the orca's
# identity is a TWO-TONE pattern the luminance-rank palette cannot carry. W = white patch, E = eye.
_ORCA_GRID = [
    "................",
    "................",
    ".....DDDDDD.....",
    "...DDDDDDDDDD...",
    "..DDDDDDDDDDDD..",
    "..DWWDDDDDDWWD..",
    ".DWWWDDDDDDWWWD.",
    ".DDWEDDDDDDEWDD.",
    ".DDDDDDDDDDDDDD.",
    ".DDDDDDDDDDDDDD.",
    "..DDDDDDDDDDDD..",
    "..WWWWWWWWWWWW..",
    "...WWWWWWWWWW...",
    "....WWWWWWWW....",
    "......WWWW......",
    "................",
]
_ORCA_PAL = {"orca": {"D": "#17181c", "W": "#e6e4dd", "E": "#050506"},
             "false_killer_whale": {"D": "#3a3b41", "W": "#5c5e65", "E": "#0c0c0e"}}
CATEGORY_FROM["orca"] = "orca"


def orca_icon(sp):
    pal = dict(_ORCA_PAL.get(sp, _ORCA_PAL["orca"]))
    if sp != "orca":
        pal["P"] = pal["D"]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(_ORCA_GRID):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if sp != "orca" and ch == "W" and y < 9:
                ch = "D"                                   # the false killer whale has no eye patch
            img.putpixel((x, y), hx(pal[ch]) + (255,))
    return img


# Mustelids (2.9.0, new type): broad flat head, small round ears, short dark muzzle. P = the pale marking: the
# wolverine's silver forehead band, the honey badger's mantle (which covers the whole crown).
_MUST_GRID = [
    "................",
    "................",
    "...KK......KK...",
    "..KBBBBBBBBBBK..",
    "..BPPPPPPPPPPB..",
    "..BPPPPPPPPPPB..",
    "..BBBEBBBBEBBB..",
    "..BBBBBBBBBBBB..",
    "...BBBMMMMBBB...",
    "...BBMMNNMMBB...",
    "....BMMMMMMB....",
    ".....BBBBBB.....",
    "................",
    "................",
    "................",
    "................",
]
_MUST_PAL = {"wolverine": {"K": "#1d1510", "B": "#3a2a1e", "P": "#7a7368", "M": "#2a1f17", "N": "#0b0908", "E": "#050403"},
             "honey_badger": {"K": "#a9a7a0", "B": "#1b1b1d", "P": "#c9c7c0", "M": "#141416", "N": "#060606", "E": "#020202"}}
CATEGORY_FROM["mustelid"] = "wolverine"


def mustelid_icon(sp):
    pal = _MUST_PAL.get(sp, _MUST_PAL["wolverine"])
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(_MUST_GRID):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if sp == "honey_badger" and ch == "B" and y <= 3:
                ch = "P"                                   # the mantle covers the crown to the ears
            img.putpixel((x, y), hx(pal[ch]) + (255,))
    return img


# Giant ground birds (2.9.0, new type): a small head on the top of a long neck, front view; Genyornis' deep beak.
_GB_GRID = [
    "................",
    ".....DDDDDD.....",
    "....DBBBBBBD....",
    "....BWEBBEWB....",
    "....BBBBBBBB....",
    ".....BHHHHB.....",
    "......HhhH......",
    "......BBBB......",
    "......BNNB......",
    "......BNNB......",
    ".....BBNNBB.....",
    "....BBBNNBBB....",
    "...PPBBBBBBPP...",
    "..PPPPPPPPPPPP..",
    "..PPPPPPPPPPPP..",
    "................",
]
_GB_PAL = {"giant_moa": {"D": "#3a2316", "B": "#5e3c26", "N": "#7a5034", "P": "#6e4128", "H": "#c8b88c", "h": "#a49268", "E": "#0c0806", "W": "#2a1c12"},
           "elephant_bird": {"D": "#1c1612", "B": "#4a4540", "N": "#5e5852", "P": "#352a22", "H": "#9a8f7c", "h": "#7c7262", "E": "#0c0806", "W": "#2a241e"},
           "genyornis": {"D": "#2e2a26", "B": "#9a968e", "N": "#8a8680", "P": "#544c44", "H": "#3a3835", "h": "#5a5650", "E": "#0c0806", "W": "#5c5850"}}
CATEGORY_FROM["ground_bird"] = "giant_moa"


def ground_bird_icon(sp):
    pal = _GB_PAL.get(sp, _GB_PAL["giant_moa"])
    grid = [list(r) for r in _GB_GRID]
    if sp == "genyornis":                       # the deep beak: two more beak rows
        grid[7] = list("......HhhH......")
        grid[8] = list("......hHHh......")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), hx(pal[ch]) + (255,))
    return img


# Great apes (2.9.0, new type): front view -- hair crown, heavy brow, bare dark face, eyes under the brow, muzzle.
_GA_GRID = [
    "................",
    "....HHHHHHHH....",
    "...HHHHHHHHHH...",
    "..HHSSSSSSSSHH..",
    "..HSSSSSSSSSSH..",
    "..HSESSSSSSESH..",
    "..HSSSSSSSSSSH..",
    "..HHSSMMMMSSHH..",
    "...HSMMNNMMSH...",
    "...HSMMMMMMSH...",
    "....HSSSSSSH....",
    ".....HHHHHH.....",
    "................",
    "................",
    "................",
    "................",
]
_GA_PAL = {"gigantopithecus": {"H": "#52301f", "S": "#2a2420", "M": "#3e3530", "N": "#100c0a", "E": "#2a1608"},
           "bornean_orangutan": {"H": "#94401a", "S": "#3a3028", "M": "#544638", "N": "#140f0c", "E": "#2a1608"}}
CATEGORY_FROM["great_ape"] = "gigantopithecus"


def great_ape_icon(sp):
    pal = _GA_PAL.get(sp, _GA_PAL["gigantopithecus"])
    grid = [list(r) for r in _GA_GRID]
    if sp == "bornean_orangutan":                 # the male's broad cheek pads
        for y in (5, 6, 7):
            grid[y][1] = "S"
            grid[y][14] = "S"
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), hx(pal[ch]) + (255,))
    return img


def make(typ, sp):
    if typ == "great_ape":
        return great_ape_icon(sp)
    if typ == "ground_bird":
        return ground_bird_icon(sp)
    if typ == "snake" and sp == "sea_krait":
        return krait_icon()
    if typ == "tortoise" and sp == "meiolania":
        return meiolania_icon()
    if typ == "orca":
        return orca_icon(sp)
    if typ == "mustelid":
        return mustelid_icon(sp)
    return _make_before_krait(typ, sp)


def sloth_sheet():
    items = [(sp, make("ground_sloth", sp)) for sp in _GS_FAMILY]
    items += [("OLD " + sp, Image.open(ICON / "ground_sloth" / (sp + ".png")).convert("RGBA")) for sp in _GS_FAMILY
              if (ICON / "ground_sloth" / (sp + ".png")).exists()]
    sheet(items, ROOT / "scratch/icon_sloth.png", S=8, cols=5)


if __name__ == "__main__" and "--sloth" in sys.argv:
    sloth_sheet()

if __name__ == "__main__" and "--redo" in sys.argv:
    redo_sheet()

if __name__ == "__main__" and "--sample" in sys.argv:
    sample()

if __name__ == "__main__" and "--all" in sys.argv:
    made, skipped = build_all()
    print("made %d icons; skipped %s" % (len(made), skipped))
    sheet(made, ROOT / "scratch/icon_all.png", S=5, cols=12)

if __name__ == "__main__" and "--install" in sys.argv:
    n = 0
    for p in OUT.rglob("*.png"):
        dst = ICON / p.relative_to(OUT)
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(p, dst)
        n += 1
    print("installed", n)
