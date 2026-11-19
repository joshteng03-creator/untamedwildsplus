"""P6a sourcing CLI.  Always run with `python -I scripts/audio/fetch.py ...`.

  cs  <terms>                     Commons search (licence verdict per hit)
  cg  <type> <File:title>         Commons download + extmetadata evidence
  fs  <terms> [maxdur] [page]     Freesound CC0 search
  fp  <url>                       Freesound page peek (licence, description, tags)
  fg  <type> <url>                Freesound download (CC0-verified page + HQ preview)
  fed <type> <agency> <sid> <page_url> <file_url> <author> <pd_quote> <title>
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import uwaudio as U  # noqa: E402


def main(a):
    cmd = a[0]
    if cmd == "cs":
        for r in U.commons_search(a[1]):
            if r["pronunciation"]:
                continue
            flag = "OK " if r["licence_ok"] else "-- "
            d = r["duration"] or 0
            print(f"{flag}{d:7.1f}s {r['licence'][:14]:14} {r['title'][:80]} | {r['desc'][:90]}")
    elif cmd == "cg":
        print(U.commons_fetch(a[2], a[1]))
    elif cmd == "fs":
        maxdur = float(a[2]) if len(a) > 2 else None
        page = int(a[3]) if len(a) > 3 else 1
        for r in U.fs_search(a[1], page, maxdur):
            print(f"{r['id']:>7} {r['duration']:6.1f}s dl={r['downloads']:>5} {r['licence'][:6]} "
                  f"{r['title'][:60]:60} {r['url']}")
    elif cmd == "fu":  # fu <user> <terms|-> [pages]
        pages = int(a[3]) if len(a) > 3 else 1
        terms = "" if a[2] == "-" else a[2]
        for p in range(1, pages + 1):
            rows = U.fs_search(terms, p, None, a[1])
            if not rows:
                break
            for r in rows:
                print(f"{r['id']:>7} {r['duration']:6.1f}s {r['title'][:75]:75} {r['url']}")
    elif cmd == "fp":
        for u in a[1:]:
            try:
                info = U.fs_fetch(u, "_peek", peek=True)
            except Exception as e:  # noqa: BLE001
                print("FAIL", u, e)
                continue
            info["url"] = u
            print(json.dumps({k: info[k] for k in ("url", "title", "licence_ok", "tags", "description")},
                             ensure_ascii=True)[:520])
    elif cmd == "fg":
        for u in a[2:]:
            try:
                print(U.fs_fetch(u, a[1]))
            except (SystemExit, FileExistsError) as e:
                print("SKIP", e)
    elif cmd == "fed":
        print(U.federal_fetch(a[4], a[5], a[1], a[2], a[3], a[6], a[7], a[8]))
    else:
        print(__doc__)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main(sys.argv[1:])
