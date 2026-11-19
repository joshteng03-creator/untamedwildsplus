"""List the audio files on NPS sound pages with the credit line printed after each.

python -I scripts/audio/nps_scan.py <page_url>...
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import uwaudio as U  # noqa: E402

AUD = re.compile(r'(https?://[^"\'\s>]+\.(?:mp3|wav|m4a|ogg))', re.I)


def scan(url):
    h = U.http_get(url)
    auds = []
    for m in AUD.finditer(h):
        a = m.group(1)
        if a not in auds and not a.startswith("sound-"):
            auds.append(a)
    h2 = re.sub(r"<script.*?</script>|<style.*?</style>", " ", h, flags=re.S)
    txt = re.sub(r"\s+", " ", U.strip_tags(h2))
    print("=====", url)
    for a in auds:
        print("  AUDIO", a)
    for m in re.finditer(r"Credit / Author:\s*(.{0,90}?)\s*(?:Date created:\s*(\S+)|Audio Transcript)", txt):
        before = txt[max(0, m.start() - 170):m.start()]
        before = re.sub(r".*(Audio Transcript|More Information|\])", "", before)
        print(f"  CREDIT {m.group(1)} | {m.group(2)}  ctx: {before.strip()[-150:]}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    for u in sys.argv[1:]:
        try:
            scan(u)
        except Exception as e:  # noqa: BLE001
            print("FAIL", u, e)
