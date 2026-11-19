#!/usr/bin/env python3
"""A Blockbench rig of ONE proboscidean species as it looks in game (2.9.0).

Blockbench never runs setupAnim, so the family tables (tusk angles, topline scales, ear yaw, hidden parts) are invisible
on the plain gen_bb_rig ModelMammoth rig. This takes that rig's loader and swaps in the species' BAKED geometry from
proboscidean_spec.emit_json(species), after checking that every part's texOffs equals the shipped Java's (the bake comes
from the spec's UV pack; a mismatch would paint the wrong rects).

hasLargeTusks is NOT baked (--keep-tusk-size, the default): Blockbench's box UV sizes each footprint from the box, so a
1.35x tusk would paint a larger footprint than the game samples. The tusk paint is a lengthwise ramp, so a 1.0 bake
paints the right texels for both sizes.

    python scripts/gen_bb_rig.py ModelMammoth scratch/rig_mammoth_full.js --skin mammoth/asian_elephant.png
    python scripts/probo_species_rig.py stegodon_orientalis scratch/rig_mammoth_full.js scratch/rig_stego.js
"""
import io
import json
import pathlib
import sys
from contextlib import redirect_stdout

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import proboscidean_spec as S  # noqa: E402

species, src, dst = sys.argv[1], pathlib.Path(sys.argv[2]), pathlib.Path(sys.argv[3])
sp = next(s for s in S.SPECIES if s[0] == species)
if "--large" not in sys.argv:
    sp = sp[:6] + (False,) + sp[7:]
    S.SPECIES = [sp if s[0] == species else s for s in S.SPECIES]
buf = io.StringIO()
with redirect_stdout(buf):
    S.emit_json(species)
baked = json.loads(buf.getvalue())
text = src.read_text(encoding="utf-8")
i = text.index("const SPEC = ") + len("const SPEC = ")
j = text.index("};", i) + 1
shipped = {p["n"]: p for p in json.loads(text[i:j])["parts"]}
bad = [p["n"] for p in baked["parts"] if shipped[p["n"]]["uv"] != p["uv"]]
if bad:
    raise SystemExit("texOffs differ from the shipped Java for: " + ", ".join(bad))
frozen = 0
for p in baked["parts"]:
    q = shipped[p["n"]]
    p["m"] = q.get("m", False)
    # a family scale (ear size, topline height) resizes the box but never its UV in game (setScale): freeze the face
    # UVs at the Java box size, or the ears paint 40% of their rect and the topline spills onto its neighbours
    size, orig = [p["t"][k] - p["f"][k] for k in range(3)], [q["t"][k] - q["f"][k] for k in range(3)]
    if any(abs(size[k] - orig[k]) > 1e-6 for k in range(3)):
        p["os"] = orig
        frozen += 1
print(f"uv-frozen (resized) parts: {frozen}")
dst.write_text(text[:i] + json.dumps(baked, separators=(",", ":")) + text[j:], encoding="utf-8")
print(f"{species}: {len(baked['parts'])} of {len(shipped)} parts shown, texOffs identical to the Java -> {dst}")
