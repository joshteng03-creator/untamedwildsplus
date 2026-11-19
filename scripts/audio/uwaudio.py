"""Shared helpers for the P6a sound-sourcing pipeline.

Everything that touches the network or a downloaded file lives here so the
licence rules are enforced in one place:

  * Wikimedia Commons: keep only CC0 / public-domain files, judged from the
    file's own extmetadata (LicenseShortName AND License).
  * Freesound: keep only sounds whose own page links the CC0 deed and says
    "Creative Commons 0"; we take the public HQ preview MP3.
  * Federal pages (NPS / NOAA / USFWS / USGS): the caller passes the page URL
    and the file URL; the page HTML is saved as evidence and the caller must
    state the public-domain wording found on it.

Downloads are untrusted data.  Each goes into its own new folder
scratch/audio_sources/raw/<type>/<source-id>/ together with its licence
evidence and a meta.json written by this module (not by the download).
Run every script that imports this with `python -I`.
"""
from __future__ import annotations

import datetime as _dt
import html as _html
import json
import os
import re
import time
import urllib.parse
import urllib.request

UA = "uw-audio-sourcing/1.0"
REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RAW = os.path.join(REPO, "scratch", "audio_sources", "raw")
STAGE = os.path.join(REPO, "scratch", "audio_stage")
REVIEW = os.path.join(REPO, "scratch", "audio_review")
SOURCES = os.path.join(REPO, "scratch", "audio_sources")

COMMONS_API = "https://commons.wikimedia.org/w/api.php"
OK_SHORT = {"cc0", "public domain", "pd"}
OK_LICENSE = {"cc0", "pd", "public domain"}


def today() -> str:
    return _dt.date.today().isoformat()


def http_get(url: str, binary: bool = False, tries: int = 5, timeout: int = 60):
    last = None
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=timeout) as r:
                data = r.read()
            time.sleep(0.8)  # be polite
            return data if binary else data.decode("utf-8", "replace")
        except Exception as e:  # noqa: BLE001
            last = e
            wait = 15 * (i + 1) if "429" in str(e) else 2 * (i + 1)
            time.sleep(wait)
    raise RuntimeError(f"GET failed {url}: {last}")


def slug(s: str, n: int = 60) -> str:
    s = re.sub(r"[^A-Za-z0-9]+", "-", s).strip("-").lower()
    return s[:n] or "x"


def strip_tags(s: str) -> str:
    return _html.unescape(re.sub(r"<[^>]+>", "", s or "")).strip()


def new_folder(typ: str, source_id: str) -> str:
    d = os.path.join(RAW, typ, source_id)
    if os.path.isdir(d) and os.listdir(d):
        raise FileExistsError(d)
    os.makedirs(d, exist_ok=True)
    return d


# ---------------------------------------------------------------- Commons
def commons_licence_ok(em: dict) -> tuple[bool, str, str]:
    short = (em.get("LicenseShortName", {}) or {}).get("value", "")
    lic = (em.get("License", {}) or {}).get("value", "")
    s = strip_tags(short).strip().lower()
    l = strip_tags(lic).strip().lower()
    ok = s in OK_SHORT and (l in OK_LICENSE or l == "")
    # A file can be "Public domain" in short name with a License like "pd";
    # anything mentioning cc-by / sa / nc is rejected outright.
    if re.search(r"\bby\b|cc-by|-sa|-nc|gfdl|gpl", l):
        ok = False
    return ok, strip_tags(short), strip_tags(lic)


LANG_PREFIX = re.compile(r"^File:(LL-Q\d+|[A-Z][a-z]{1,2}(-[A-Za-z]{2,4})*-)", re.I)


def commons_search(terms: str, limit: int = 50) -> list[dict]:
    q = {
        "action": "query", "format": "json", "generator": "search",
        "gsrnamespace": "6", "gsrsearch": f"{terms} filetype:audio",
        "gsrlimit": str(limit), "prop": "imageinfo",
        "iiprop": "url|extmetadata|mime|size",
        "iiextmetadatafilter": "LicenseShortName|License|Artist|ImageDescription|Credit|UsageTerms|Copyrighted",
    }
    d = json.loads(http_get(COMMONS_API + "?" + urllib.parse.urlencode(q)))
    out = []
    for p in (d.get("query", {}) or {}).get("pages", {}).values():
        ii = (p.get("imageinfo") or [{}])[0]
        em = ii.get("extmetadata", {}) or {}
        ok, short, lic = commons_licence_ok(em)
        out.append({
            "title": p.get("title", ""), "pageid": p.get("pageid"),
            "mime": ii.get("mime"), "duration": ii.get("duration"),
            "size": ii.get("size"), "licence_ok": ok, "licence": short,
            "license_code": lic,
            "artist": strip_tags((em.get("Artist") or {}).get("value", ""))[:80],
            "desc": strip_tags((em.get("ImageDescription") or {}).get("value", ""))[:160],
            "pronunciation": bool(LANG_PREFIX.match(p.get("title", ""))),
        })
    out.sort(key=lambda r: (not r["licence_ok"], r["pronunciation"], r["title"]))
    return out


def commons_fetch(title: str, typ: str) -> str:
    q = {
        "action": "query", "format": "json", "titles": title,
        "prop": "imageinfo", "iiprop": "url|extmetadata|mime|size|sha1|user|timestamp",
    }
    raw_json = http_get(COMMONS_API + "?" + urllib.parse.urlencode(q))
    d = json.loads(raw_json)
    page = next(iter(d["query"]["pages"].values()))
    ii = page["imageinfo"][0]
    em = ii.get("extmetadata", {})
    ok, short, lic = commons_licence_ok(em)
    if not ok:
        raise SystemExit(f"REJECT licence {short!r}/{lic!r}: {title}")
    folder = new_folder(typ, f"commons-{page['pageid']}")
    with open(os.path.join(folder, "evidence_commons_api.json"), "w", encoding="utf-8") as f:
        f.write(raw_json)
    url = ii["url"]
    ext = os.path.splitext(urllib.parse.urlparse(url).path)[1].lower() or ".bin"
    fn = "source" + ext
    with open(os.path.join(folder, fn), "wb") as f:
        f.write(http_get(url, binary=True))
    meta = {
        "source": "Wikimedia Commons", "title": title,
        "page_url": ii.get("descriptionurl"), "file_url": url, "file": fn,
        "author": strip_tags((em.get("Artist") or {}).get("value", "")) or ii.get("user", ""),
        "licence": short, "licence_code": lic,
        "licence_url": strip_tags((em.get("LicenseUrl") or {}).get("value", "")),
        "credit": strip_tags((em.get("Credit") or {}).get("value", "")),
        "description": strip_tags((em.get("ImageDescription") or {}).get("value", ""))[:600],
        "evidence": "evidence_commons_api.json", "retrieved": today(),
    }
    with open(os.path.join(folder, "meta.json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)
    return folder


# ---------------------------------------------------------------- Freesound
FS = "https://freesound.org"
PLAYER = re.compile(r'<div\s+class="bw-player"(.*?)>', re.S)
ATTR = re.compile(r'data-([a-z-]+)="([^"]*)"')


def fs_search(terms: str, page: int = 1, maxdur: float | None = None, user: str | None = None) -> list[dict]:
    f = 'license:"Creative Commons 0"'
    if maxdur:
        f += f" duration:[0 TO {maxdur}]"
    if user:
        f += f' username:"{user}"'
    q = {"q": terms, "f": f, "page": str(page), "g": "0"}  # g=0: do not collapse packs
    h = http_get(FS + "/search/?" + urllib.parse.urlencode(q))
    out = []
    # split by result blocks so the per-result licence title stays with it
    for block in h.split('class="bw-search__result"')[1:]:
        m = PLAYER.search(block)
        if not m:
            continue
        a = dict(ATTR.findall(m.group(1)))
        lic = re.search(r'title="License: ([^"]+)"', block)
        out.append({
            "id": a.get("sound-id"), "user": a.get("username"),
            "title": _html.unescape(a.get("title", "")),
            "duration": float(a.get("duration") or 0),
            "sr": a.get("samplerate"), "downloads": a.get("num-downloads"),
            "licence": lic.group(1) if lic else "?",
            "url": f"{FS}/people/{a.get('username')}/sounds/{a.get('sound-id')}/",
        })
    return out


def fs_page_info(h: str) -> dict:
    def meta(prop):
        m = re.search(r'<meta (?:property|name)="%s" content="([^"]*)"' % re.escape(prop), h)
        return _html.unescape(m.group(1)) if m else ""

    lic_a = re.search(r'<a title="Go to the full license text" href="([^"]+)"[^>]*>([^<]+)</a>', h)
    hq = re.search(r'https://cdn\.freesound\.org/previews/\d+/\d+_\d+-hq\.mp3', h)
    tags = sorted(set(re.findall(r'href="/browse/tags/([^/"]+)/"', h)))
    return {
        "title": meta("og:audio:title"), "author": meta("og:audio:artist"),
        "page_url": meta("og:url"), "description": meta("twitter:description"),
        "licence_href": lic_a.group(1) if lic_a else "",
        "licence_text": lic_a.group(2).strip() if lic_a else "",
        "hq": hq.group(0) if hq else "", "tags": tags,
    }


def fs_fetch(url: str, typ: str, peek: bool = False) -> str | dict:
    m = re.search(r"/sounds/(\d+)/?", url)
    if not m:
        raise SystemExit("not a freesound sound url: " + url)
    sid = m.group(1)
    h = http_get(url)
    info = fs_page_info(h)
    ok = ("publicdomain/zero/1.0" in info["licence_href"]
          and info["licence_text"] == "Creative Commons 0")
    if peek:
        info["licence_ok"] = ok
        return info
    if not ok:
        raise SystemExit(f"REJECT licence {info['licence_text']!r} {info['licence_href']!r}: {url}")
    if not info["hq"]:
        raise SystemExit("no HQ preview on page: " + url)
    folder = new_folder(typ, f"fs-{sid}")
    with open(os.path.join(folder, "evidence_freesound_page.html"), "w", encoding="utf-8") as f:
        f.write(h)
    with open(os.path.join(folder, "source.mp3"), "wb") as f:
        f.write(http_get(info["hq"], binary=True))
    meta = {
        "source": "Freesound", "title": info["title"], "page_url": info["page_url"] or url,
        "file_url": info["hq"], "file": "source.mp3", "author": info["author"],
        "licence": "CC0 1.0 (Creative Commons 0)", "licence_url": info["licence_href"],
        "description": info["description"][:600], "tags": info["tags"],
        "evidence": "evidence_freesound_page.html", "retrieved": today(),
        "note": "public HQ preview MP3 (original needs login)",
    }
    with open(os.path.join(folder, "meta.json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)
    return folder


# ---------------------------------------------------------------- federal
POLICY = {
    "NPS": ("https://www.nps.gov/aboutus/disclaimer.htm",
            "material created by the National Park Service and presented on this website, "
            "unless otherwise indicated, is generally considered in the public domain"),
}


def federal_fetch(page_url: str, file_url: str, typ: str, agency: str, sid: str,
                  author: str, pd_quote: str, title: str) -> str:
    """Save a federal page + file.  `pd_quote` (e.g. the page's own
    "Credit / Author: NPS/<name>" line) is checked against the saved HTML, and the
    agency's public-domain policy page is saved beside it as extra evidence."""
    h = http_get(page_url)
    norm = lambda s: re.sub(r"\s+", " ", strip_tags(s)).lower()
    found = pd_quote.lower() in norm(h)
    if not found:
        raise SystemExit(f"REJECT: credit/PD wording not found on page: {pd_quote!r}")
    folder = new_folder(typ, f"{agency.lower()}-{sid}")
    with open(os.path.join(folder, "evidence_page.html"), "w", encoding="utf-8") as f:
        f.write(h)
    extra = []
    if agency in POLICY:
        purl, pq = POLICY[agency]
        ph = http_get(purl)
        if pq.lower() not in norm(ph).replace("’", "'"):
            raise SystemExit("policy wording not found on " + purl)
        with open(os.path.join(folder, "evidence_policy.html"), "w", encoding="utf-8") as f:
            f.write(ph)
        extra.append("evidence_policy.html")
    ext = os.path.splitext(urllib.parse.urlparse(file_url).path)[1].lower() or ".bin"
    fn = "source" + ext
    with open(os.path.join(folder, fn), "wb") as f:
        f.write(http_get(file_url, binary=True))
    meta = {
        "source": agency, "title": title, "page_url": page_url, "file_url": file_url,
        "file": fn, "author": author, "licence": "Public domain (US federal government work)",
        "licence_url": POLICY.get(agency, ("",))[0], "pd_quote": pd_quote,
        "pd_quote_found_on_page": found, "evidence": "evidence_page.html",
        "extra_evidence": extra, "retrieved": today(),
    }
    with open(os.path.join(folder, "meta.json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)
    return folder
