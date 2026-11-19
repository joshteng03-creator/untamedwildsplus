"""Build scratch/audio_review/index.html from scratch/audio_stage/credits.json.

python -I scripts/audio/review_page.py

Self-contained local page (open it from disk): grouped by batch -> type -> key, every
clip with an <audio> player (relative path to the staged ogg), a waveform PNG,
duration / peak / RMS, and source link, author and licence.
"""
import html
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dsp  # noqa: E402  (also restores the user site-packages path)
import uwaudio as U  # noqa: E402
import numpy as np  # noqa: E402

BATCH_NAMES = {"B1": "B1 - megaherbivores", "B2": "B2 - ungulates", "B3": "B3 - predators & reptiles",
               "B4": "B4 - aquatic & other", "B5": "B5 - shared sounds",
               "D1": "D1 - DERIVED voices (2026-11-18): built from the recordings above for animals with none of their own -- not heard by the designer, listen first"}
KEY_ORDER = ["ambient", "call", "alarm", "threat", "hurt", "death", "step",
             "grazer_eat", "animal_drink", "megafauna_step", "carcass_butcher"]


def waveform_png(ogg, png):
    x = dsp.load(ogg)
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        fig, ax = plt.subplots(figsize=(4.2, 0.9), dpi=100)
        n = len(x)
        step = max(1, n // 1500)
        seg = x[: (n // step) * step].reshape(-1, step)
        t = np.arange(seg.shape[0]) * step / dsp.SR
        ax.fill_between(t, seg.min(axis=1), seg.max(axis=1), color="#3d7a5a", linewidth=0)
        ax.set_ylim(-1, 1)
        ax.set_xlim(0, max(n / dsp.SR, 0.01))
        ax.axhline(10 ** (-1 / 20), color="#c44", lw=0.5, ls=":")
        ax.axhline(-(10 ** (-1 / 20)), color="#c44", lw=0.5, ls=":")
        ax.set_yticks([])
        ax.tick_params(axis="x", labelsize=6, length=2, pad=1)
        for s in ax.spines.values():
            s.set_visible(False)
        fig.subplots_adjust(left=0.01, right=0.99, top=0.98, bottom=0.22)
        fig.savefig(png, transparent=True)
        plt.close(fig)
    except ImportError:
        from PIL import Image, ImageDraw
        W, H = 420, 80
        im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        step = max(1, len(x) // W)
        for i in range(W):
            seg = x[i * step:(i + 1) * step]
            if len(seg):
                d.line([(i, H / 2 - seg.max() * H / 2), (i, H / 2 - seg.min() * H / 2)], fill="#3d7a5a")
        im.save(png)


CSS = """
:root{--bg:#f6f4ef;--card:#fff;--ink:#1f2420;--mute:#6b716c;--line:#ddd8cc;--acc:#2f6b4f}
@media (prefers-color-scheme:dark){:root{--bg:#161a17;--card:#1f2521;--ink:#e6e8e3;--mute:#9aa29b;--line:#333b35;--acc:#7cc39c}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font:14px/1.45 system-ui,Segoe UI,sans-serif}
header{padding:20px 16px 8px;max-width:1200px;margin:auto}h1{margin:0 0 4px;font-size:22px}
.sub{color:var(--mute)}nav{position:sticky;top:0;background:var(--bg);border-bottom:1px solid var(--line);
padding:8px 16px;z-index:2;display:flex;gap:10px;flex-wrap:wrap}nav a{color:var(--acc);text-decoration:none;font-size:13px}
main{max-width:1200px;margin:auto;padding:0 16px 60px}h2{margin:28px 0 6px;font-size:19px;border-bottom:2px solid var(--acc);padding-bottom:4px}
h3{margin:18px 0 4px;font-size:16px}h4{margin:10px 0 6px;font-size:13px;color:var(--mute);text-transform:uppercase;letter-spacing:.05em}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:10px}
.clip{background:var(--card);border:1px solid var(--line);border-radius:8px;padding:10px}
.clip .name{font-weight:600;font-family:ui-monospace,Consolas,monospace;font-size:13px}
.clip img{width:100%;height:auto;display:block;margin:6px 0}.clip audio{width:100%;height:32px}
.meta{font-size:12px;color:var(--mute)}.meta a{color:var(--acc)}.lic{display:inline-block;padding:0 6px;border-radius:4px;
background:color-mix(in srgb,var(--acc) 18%,transparent);color:var(--ink);font-size:11px}
.stand{font-size:12px;margin-top:2px}.gap{color:#b5651d}table{border-collapse:collapse;font-size:13px}
td,th{border-bottom:1px solid var(--line);padding:3px 8px;text-align:left}
"""


def main():
    credits = json.load(open(os.path.join(U.STAGE, "credits.json"), encoding="utf-8"))
    wf_dir = os.path.join(U.REVIEW, "wave")
    os.makedirs(wf_dir, exist_ok=True)
    plan = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "voice_plan.json"), encoding="utf-8"))
    by = {}
    for e in credits:
        by.setdefault(e["batch"], {}).setdefault(e["type"], {}).setdefault(e["key"], []).append(e)
    total = sum(e["bytes"] for e in credits)
    parts = [f"<!doctype html><html lang=en><head><meta charset=utf-8>"
             f"<meta name=viewport content='width=device-width,initial-scale=1'>"
             f"<title>Sound Review</title><style>{CSS}</style></head><body>"
             f"<header><h1>Untamed Wilds Plus - P6a sound review</h1>"
             f"<div class=sub>{len(credits)} staged clips, {total/1024/1024:.2f} MB total. "
             f"Every clip is CC0 or public domain; nothing here is in the mod yet. "
             f"Approve or reject per clip. Gaps: <a href='GAPS.md'>GAPS.md</a>.</div></header><nav>"]
    for b in sorted(plan):
        if b.startswith("_"):
            continue
        parts.append(f"<a href='#{b}'>{html.escape(BATCH_NAMES.get(b, b))}</a>")
    parts.append("</nav><main>")
    for b in sorted(plan):
        if b.startswith("_"):
            continue
        parts.append(f"<h2 id='{b}'>{html.escape(BATCH_NAMES.get(b, b))}</h2>")
        for typ in plan[b]:
            keys = by.get(b, {}).get(typ, {})
            planned = plan[b][typ]
            have = sum(len(v) for v in keys.values())
            parts.append(f"<h3 id='{b}-{typ}'>{html.escape(typ)} <span class=sub>({have} clips)</span></h3>")
            if not keys:
                parts.append("<div class='gap'>No clips staged yet - see GAPS.md.</div>")
                continue
            for key in sorted(set(planned) | set(keys), key=lambda k: KEY_ORDER.index(k) if k in KEY_ORDER else 99):
                clips = keys.get(key, [])
                want = planned.get(key, {}).get("wanted", "?")
                stand = planned.get(key, {}).get("stand_in", "")
                parts.append(f"<h4>{html.escape(key)} - {len(clips)}/{want} &nbsp;<span style='text-transform:none'>"
                             f"stand-in: {html.escape(stand)}</span></h4>")
                if not clips:
                    parts.append("<div class='gap'>gap</div>")
                    continue
                parts.append("<div class=grid>")
                for e in clips:
                    ogg = os.path.join(U.STAGE, e["file"])
                    png_rel = "wave/" + e["file"].replace("/", "__").replace(".ogg", ".png")
                    waveform_png(ogg, os.path.join(U.REVIEW, png_rel))
                    src = "../audio_stage/" + e["file"]
                    parts.append(
                        f"<div class=clip><div class=name>{html.escape(e['file'])}</div>"
                        f"<audio controls preload=none src='{html.escape(src)}'></audio>"
                        f"<img src='{html.escape(png_rel)}' alt='waveform'>"
                        f"<div class=meta>{e['dur']:.2f} s &middot; peak {e['peak_db']:.1f} dBFS &middot; "
                        f"RMS {e['rms_db']:.1f} (active {e['active_rms_db']:.1f}) dBFS &middot; {e['bytes']/1024:.0f} KB</div>"
                        f"<div class=meta><a href='{html.escape(e['source_url'])}' target=_blank>"
                        f"{html.escape(e['source'])}: {html.escape(e['source_title'][:70])}</a><br>"
                        f"by {html.escape(e['author'][:60])} &middot; <span class=lic>{html.escape(e['licence'])}</span><br>"
                        f"evidence: <a href='../../{html.escape(e['licence_evidence'])}'>{html.escape(os.path.basename(e['licence_evidence']))}</a></div>"
                        f"<div class=meta>{html.escape(e['processing'][:260])}</div></div>")
                parts.append("</div>")
    parts.append("</main></body></html>")
    with open(os.path.join(U.REVIEW, "index.html"), "w", encoding="utf-8") as f:
        f.write("\n".join(parts))
    print("wrote", os.path.join(U.REVIEW, "index.html"))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main()
