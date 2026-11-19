#!/usr/bin/env python3
"""Local review page for the solved EAT / DRINK / ALERT poses: scratch/poses/index.html (2.8.0, P3b approval)."""
import html
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
POSES = ROOT / "scratch" / "poses"

rows = []
for js in sorted(POSES.glob("*.json")):
    res = json.loads(js.read_text(encoding="utf-8"))
    typ = res["type"]
    sheet = POSES / f"sheet_{typ}.png"
    if not sheet.exists():
        continue
    notes = []
    for pose in ("eat", "drink", "alert"):
        r = res.get(pose)
        if not r:
            notes.append(f"<b>{pose.upper()}</b>: not part of this pass (shipped already or not applicable)")
            continue
        flag = "" if r.get("feasible") else " <span class=warn>(constraint marginal: " + html.escape(r.get("why", "")) + ")</span>"
        mv = r.get("neck_move_bb")
        extra = f", neck base lowered {-mv[1]:.1f} / forward {-mv[2]:.1f} units" if mv and (abs(mv[1]) > 0.05 or abs(mv[2]) > 0.05) else ""
        notes.append(f"<b>{pose.upper()}</b>: muzzle {r['muzzle_low']:+.2f} units from the ground{extra}{flag}")
    rows.append(f"""<section><h2>{html.escape(typ)}</h2><p class=meta>{html.escape(res['model'])}</p>
<ul>{''.join('<li>' + n + '</li>' for n in notes)}</ul>
<img src="{sheet.name}" alt="pose sheet {html.escape(typ)}"></section>""")

page = f"""<!doctype html><html><head><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1">
<title>Pose review</title><style>
body{{font:15px/1.45 system-ui,sans-serif;margin:24px auto;max-width:960px;padding:0 16px;background:#f6f4ef;color:#222}}
section{{background:#fff;border:1px solid #ddd;border-radius:8px;padding:12px 16px;margin:18px 0}}
img{{max-width:100%;height:auto;border:1px solid #eee}} .meta{{color:#777;margin:0}} .warn{{color:#a55}}
</style></head><body>
<h1>EAT / DRINK / ALERT poses for approval</h1>
<p>Each sheet: REST, then the solved poses, side view left and 3/4 view right. Ground line = grass; the blue band
under DRINK is the water past the bank edge. Colours are each part's average skin colour; only one horn / antler /
snout family is drawn per type, but every pose was solved to clear <i>all</i> families.</p>
{''.join(rows)}
</body></html>"""
(POSES / "index.html").write_text(page, encoding="utf-8")
print("wrote", POSES / "index.html", len(rows), "types")
