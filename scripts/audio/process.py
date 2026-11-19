"""P6a processing: raw recordings -> Minecraft-ready mono Ogg Vorbis clips.

python -I scripts/audio/process.py [--batch B1,B2]   (default: every clip in selections.json)

Reads scripts/audio/selections.json:
  {"clips": [{"batch": "B1", "type": "mammoth", "key": "call", "src": "mammoth/fs-819668",
              "start": 1.2, "end": 5.0, "hp": 30, "note": "..."}, ...]}
Pipeline per clip: decode -> mono -> 44.1 kHz -> cut -> high-pass -> trim silence
-> normalise (active RMS per key, peak <= -1 dBFS) -> gate (-45 dBFS) -> 20 ms fades
-> Ogg Vorbis q0.4 at scratch/audio_stage/<type>/<key>_<n>.ogg.
Writes scratch/audio_stage/credits.json (one entry per staged clip).
"""
import hashlib
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dsp  # noqa: E402
import uwaudio as U  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
TARGET = {"ambient": -20, "call": -14, "hurt": -16, "threat": -16, "alarm": -16, "death": -16,
          "step": -20}
SHARED_TARGET = -20
MAXLEN = {"call": 8.0}


def target_for(typ, key):
    if typ == "shared":
        return SHARED_TARGET
    return TARGET.get(key, -18)


def process_clip(c):
    folder = os.path.join(U.RAW, c["src"])
    meta = json.load(open(os.path.join(folder, "meta.json"), encoding="utf-8"))
    x = dsp.load(os.path.join(folder, meta["file"]))
    s = int(c.get("start", 0) * dsp.SR)
    t = int(c["end"] * dsp.SR) if c.get("end") else len(x)
    x = x[s:t]
    notes = [f"cut {c.get('start', 0):.2f}-{(t / dsp.SR):.2f}s of source"]
    hp = c.get("hp", 60)
    x = dsp.highpass(x, hp)
    notes.append(f"HP {hp} Hz")
    if c.get("lp"):
        x = dsp.lowpass(x, c["lp"])
        notes.append(f"LP {c['lp']} Hz")
    x = dsp.trim_silence(x)
    mx = MAXLEN.get(c["key"], 3.0)
    if len(x) > mx * dsp.SR:
        x = x[: int(mx * dsp.SR)]
        notes.append(f"truncated to {mx:.0f}s")
    tgt = target_for(c["type"], c["key"]) + c.get("gain_adj", 0)
    x, g = dsp.normalise(x, tgt)
    notes.append(f"normalised to {tgt} dBFS active RMS (gain {g:+.1f} dB), peak<=-1 dBFS")
    x = dsp.gate(x, -45.0)
    x = dsp.fades(x, 20)
    x = dsp.limiter(x, -1.0)
    notes.append("gate -45 dBFS, 20 ms fades, mono 44.1 kHz, Vorbis q0.4")
    if c.get("note"):
        notes.append(c["note"])
    return x, meta, notes


def main(argv):
    only = None
    if "--batch" in argv:
        only = set(argv[argv.index("--batch") + 1].split(","))
    sel = json.load(open(os.path.join(HERE, "selections.json"), encoding="utf-8"))
    # extra pick files (one per sourcing pass), appended in name order
    for fn in sorted(os.listdir(HERE)):
        if fn.startswith("picks_") and fn.endswith(".json"):
            extra = json.load(open(os.path.join(HERE, fn), encoding="utf-8"))
            for row in extra:
                if isinstance(row, list):  # compact form
                    b, ty, k, src, a, e, stand = row[:7]
                    d = {"batch": b, "type": ty, "key": k, "src": src, "start": a, "end": e, "stand_in": stand}
                    d.update(row[7] if len(row) > 7 else {})
                    row = d
                sel["clips"].append(row)
    credits_path = os.path.join(U.STAGE, "credits.json")
    old = []
    if os.path.exists(credits_path):
        old = json.load(open(credits_path, encoding="utf-8"))
    keep = [e for e in old if only is not None and e["batch"] not in only]
    # remove stale staged files of the batches being rebuilt
    for e in old:
        if only is None or e["batch"] in only:
            p = os.path.join(U.STAGE, e["file"])
            if os.path.exists(p):
                os.remove(p)
    counters = {}
    out = []
    for c in sel["clips"]:
        if only is not None and c["batch"] not in only:
            continue
        if c.get("skip"):
            continue
        k = (c["type"], c["key"])
        counters[k] = counters.get(k, 0) + 1
        n = counters[k]
        rel = f"{c['type']}/{c['key']}_{n}.ogg"
        dst = os.path.join(U.STAGE, rel)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        x, meta, notes = process_clip(c)
        # Vorbis can overshoot the pre-encode peak; re-encode until the DECODED
        # file honours peak <= -1 dBFS.
        for _ in range(4):
            dsp.write_ogg(dst, x)
            y = dsp.load(dst)
            st = dsp.stats(y)
            if st["peak_db"] <= -1.0:
                break
            x = x * 10 ** ((-1.1 - st["peak_db"]) / 20)
        sha = hashlib.sha256(open(dst, "rb").read()).hexdigest()
        out.append({
            "batch": c["batch"], "type": c["type"], "key": c["key"], "file": rel,
            "stand_in": c.get("stand_in", ""),
            "source": meta["source"], "source_title": meta.get("title", ""),
            "source_url": meta.get("page_url", ""), "file_url": meta.get("file_url", ""),
            "author": meta.get("author", ""), "licence": meta.get("licence", ""),
            "licence_url": meta.get("licence_url", ""),
            "licence_evidence": f"scratch/audio_sources/raw/{c['src']}/{meta['evidence']}",
            "extra_evidence": [f"scratch/audio_sources/raw/{c['src']}/{p}" for p in meta.get("extra_evidence", [])],
            "retrieved": meta.get("retrieved", ""),
            "processing": "; ".join(notes), "sha256": sha, "bytes": os.path.getsize(dst), **st,
        })
        print(f"{rel:38} {st['dur']:5.2f}s pk={st['peak_db']:6.1f} rms={st['active_rms_db']:6.1f} "
              f"{os.path.getsize(dst)/1024:6.1f}KB  <- {c['src']}")
    allc = keep + out
    allc.sort(key=lambda e: (e["batch"], e["type"], e["key"], e["file"]))
    json.dump(allc, open(credits_path, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
    tot = sum(e["bytes"] for e in allc)
    print(f"staged clips: {len(allc)}  total {tot/1024/1024:.2f} MB")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main(sys.argv[1:])
