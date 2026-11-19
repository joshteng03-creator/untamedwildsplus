"""Walk a Commons category tree and list every audio file with its licence verdict.

python -I scripts/audio/commons_inventory.py "Category:Audio files of Mammalia" [depth] > out.tsv
"""
import json
import os
import sys
import urllib.parse

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import uwaudio as U  # noqa: E402


def members(cat, cmtype):
    cont = {}
    while True:
        q = {"action": "query", "format": "json", "list": "categorymembers", "cmtitle": cat,
             "cmtype": cmtype, "cmlimit": "500", **cont}
        d = json.loads(U.http_get(U.COMMONS_API + "?" + urllib.parse.urlencode(q)))
        for m in d["query"]["categorymembers"]:
            yield m["title"]
        if "continue" not in d:
            break
        cont = {"cmcontinue": d["continue"]["cmcontinue"]}


def info(titles):
    out = []
    for i in range(0, len(titles), 50):
        q = {"action": "query", "format": "json", "titles": "|".join(titles[i:i + 50]),
             "prop": "imageinfo", "iiprop": "extmetadata|mime|size",
             "iiextmetadatafilter": "LicenseShortName|License|Artist|ImageDescription"}
        d = json.loads(U.http_get(U.COMMONS_API + "?" + urllib.parse.urlencode(q)))
        for p in d["query"]["pages"].values():
            ii = (p.get("imageinfo") or [{}])[0]
            em = ii.get("extmetadata", {}) or {}
            ok, short, lic = U.commons_licence_ok(em)
            out.append((ok, ii.get("duration") or 0, short, p["title"],
                        U.strip_tags((em.get("Artist") or {}).get("value", ""))[:40]))
    return out


def main():
    root = sys.argv[1]
    depth = int(sys.argv[2]) if len(sys.argv) > 2 else 3
    seen, files = set(), []
    frontier = [(root, 0)]
    while frontier:
        cat, dep = frontier.pop()
        if cat in seen:
            continue
        seen.add(cat)
        for f in members(cat, "file"):
            files.append((cat, f))
        if dep < depth:
            for c in members(cat, "subcat"):
                frontier.append((c, dep + 1))
    titles = sorted({f for _, f in files})
    catof = {}
    for c, f in files:
        catof.setdefault(f, c)
    for ok, dur, short, t, art in sorted(info(titles), key=lambda r: catof.get(r[3], "")):
        print(f"{'OK' if ok else '--'}\t{dur:7.1f}\t{short[:14]}\t{catof.get(t,'')[9:60]}\t{t}\t{art}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main()
