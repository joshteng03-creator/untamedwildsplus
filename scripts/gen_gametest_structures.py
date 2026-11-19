"""Writes the GameTest structure templates (gzip NBT, DataVersion 2975 = MC 1.18.2) with no third-party libraries.

    python scripts/gen_gametest_structures.py

Outputs to src/main/resources/data/untamedwilds/structures/gametest/ (excluded from the shipped jar by build.gradle):
  platform.nbt  -- 32x6x32: dirt floor (y0), grass (y1), a 3-high barrier ring (y2..4) so test animals cannot wander off
  waterhole.nbt -- the same with a 6x6 water-source pond set into the grass layer at the centre (banks are grass, 2 air above)
"""
import gzip
import io
import os
import struct

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "data", "untamedwilds", "structures", "gametest")

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


class Int(int):
    pass


def _name(buf, name):
    b = name.encode("utf-8")
    buf.write(struct.pack(">H", len(b)))
    buf.write(b)


def _tag_type(v):
    if isinstance(v, dict):
        return TAG_COMPOUND
    if isinstance(v, list):
        return TAG_LIST
    if isinstance(v, str):
        return TAG_STRING
    if isinstance(v, int):
        return TAG_INT
    raise TypeError(type(v))


def _payload(buf, v):
    t = _tag_type(v)
    if t == TAG_INT:
        buf.write(struct.pack(">i", v))
    elif t == TAG_STRING:
        _name(buf, v)
    elif t == TAG_LIST:
        et = _tag_type(v[0]) if v else TAG_END
        buf.write(struct.pack(">bi", et, len(v)))
        for e in v:
            _payload(buf, e)
    elif t == TAG_COMPOUND:
        for k, e in v.items():
            buf.write(struct.pack(">b", _tag_type(e)))
            _name(buf, k)
            _payload(buf, e)
        buf.write(struct.pack(">b", TAG_END))


def write_nbt(path, root):
    buf = io.BytesIO()
    buf.write(struct.pack(">b", TAG_COMPOUND))
    _name(buf, "")
    _payload(buf, root)
    with gzip.open(path, "wb") as f:
        f.write(buf.getvalue())


def structure(sx, sy, sz, block_at):
    palette, index, blocks = [], {}, []
    for y in range(sy):
        for z in range(sz):
            for x in range(sx):
                b = block_at(x, y, z)
                if b is None:
                    continue
                if b not in index:
                    index[b] = len(palette)
                    palette.append({"Name": b})
                blocks.append({"pos": [x, y, z], "state": index[b]})
    return {"DataVersion": 2975, "size": [sx, sy, sz], "palette": palette, "blocks": blocks, "entities": []}


S, H = 32, 6


def base(x, y, z):
    edge = x in (0, S - 1) or z in (0, S - 1)
    if y == 0:
        return "minecraft:dirt"
    if y == 1:
        return "minecraft:grass_block"
    if 2 <= y <= 4 and edge:
        return "minecraft:barrier"
    return None


def pond(x, y, z):
    if y == 1 and 13 <= x <= 18 and 13 <= z <= 18:
        return "minecraft:water"
    return base(x, y, z)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    write_nbt(os.path.join(OUT, "platform.nbt"), structure(S, H, S, base))
    write_nbt(os.path.join(OUT, "waterhole.nbt"), structure(S, H, S, pond))
    print("wrote", os.listdir(OUT))
