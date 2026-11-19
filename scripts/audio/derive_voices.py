#!/usr/bin/env python3
"""Derived voices for the animals with no CC0 / public-domain recording of their own (2.8.0, scratch/audio_review/GAPS.md).

Every clip here is built ONLY from recordings already staged and credited (scratch/audio_stage, all CC0 or public
domain): pitch / speed shifts (tape-style resampling -- a bigger animal is slower and lower), a falling pitch glide for
death calls, the loudest window cut out, repeats into a call series, filtering, layering. Nothing is synthesised. Each
recipe names the real sound it imitates, and its credits record lists every source clip (author, URL, licence), so
the licence chain stays CC0 / PD and CREDITS.md can say exactly what each file was made from.

    python scripts/audio/derive_voices.py            # (re)build every derived clip + their credits records
    python scripts/audio/derive_voices.py --dry      # print the plan only

Idempotent: earlier derived clips (credits records with "derived": true) are removed first. Then run
install_voices.py and validate_sounds.py as usual. The designer could not listen to any of these: the numbers in the
log (pitch, brightness, length) are what was checked, so the review page marks every one as DERIVED.
"""
import hashlib
import json
import pathlib
import sys
from fractions import Fraction

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import dsp  # noqa: E402
from scipy.signal import resample_poly  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parents[2]
STAGE = ROOT / "scratch" / "audio_stage"
CREDITS = STAGE / "credits.json"
TARGET = {"ambient": -20, "call": -14, "hurt": -16, "threat": -16, "alarm": -16, "death": -16}
MAX_LEN = {"call": 8.0}            # validator: call <= 8.5 s, everything else <= 3.5 s
SR = dsp.SR


# ---------------------------------------------------------------- operations
def src(name):
    return dsp.load(str(STAGE / (name + ".ogg")))


def shift(x, semis):
    """Tape-style pitch shift: +semis = higher AND shorter, -semis = lower AND longer (a bigger animal)."""
    r = 2.0 ** (semis / 12.0)
    f = Fraction(1.0 / r).limit_denominator(240)
    return resample_poly(x, f.numerator, f.denominator)


def glide(x, s0, s1):
    """Pitch falling (or rising) from s0 to s1 semitones across the clip -- a dying call sags."""
    n = len(x)
    pos, out, p = [], 0.0, 0.0
    while p < n - 1:
        u = p / n
        pos.append(p)
        p += 2.0 ** ((s0 + (s1 - s0) * u) / 12.0)
    return np.interp(np.asarray(pos), np.arange(n), x)


def loudest(x, seconds):
    """The loudest window of `seconds`."""
    n = int(seconds * SR)
    if len(x) <= n:
        return x
    e = np.convolve(x ** 2, np.ones(int(0.02 * SR)), "same")
    c = np.cumsum(e)
    s = c[n:] - c[:-n]
    i = int(np.argmax(s))
    return x[i:i + n]


def cut(x, t0, t1=None):
    return x[int(t0 * SR): (int(t1 * SR) if t1 is not None else None)]


def series(parts, gaps):
    out = [parts[0]]
    for p, g in zip(parts[1:], gaps):
        out += [np.zeros(int(g * SR)), p]
    return np.concatenate(out)


def mix(a, b, gb=1.0, at=0.0):
    n = max(len(a), int(at * SR) + len(b))
    y = np.zeros(n)
    y[:len(a)] += a
    y[int(at * SR):int(at * SR) + len(b)] += gb * b
    return y


def env_fade_out(x, frac=0.45):
    n = int(len(x) * frac)
    y = x.copy()
    y[-n:] *= np.linspace(1.0, 0.0, n) ** 1.6
    return y


def hp(x, hz):
    return dsp.highpass(x, hz)


def lp(x, hz):
    return dsp.lowpass(x, hz)


# ---------------------------------------------------------------- recipes
# (type, key, imitates, sources, builder, processing text)
def R(typ, key, imitates, sources, fn, how):
    return dict(type=typ, key=key, imitates=imitates, sources=sources, fn=fn, how=how)


RECIPES = [
    # ---- giraffid: giraffes are near-silent -- a low nocturnal hum (~90-100 Hz), snorts when alarmed, grunts/bellows
    R("giraffid", "ambient", "giraffe nocturnal hum (~110 Hz)", ["macrauchenia/ambient_1"],
      lambda: lp(cut(shift(src("macrauchenia/ambient_1"), -11), 0, 3.2), 2000),
      "alpaca moaning hum (f0 213 Hz) pitched down 11 semitones to ~112 Hz, low-passed 2 kHz, cut to 3.2 s "
      "(a real giraffe hums at ~92 Hz; a touch higher so it is audible on small speakers)"),
    R("giraffid", "ambient", "giraffe hum, short", ["macrauchenia/ambient_3"],
      lambda: lp(shift(src("macrauchenia/ambient_3"), -12), 2000),
      "alpaca moan (f0 261 Hz) pitched down 12 semitones to ~130 Hz, low-passed 2 kHz"),
    R("giraffid", "ambient", "giraffe soft snort", ["equid/alarm_1"],
      lambda: shift(src("equid/alarm_1"), -4),
      "horse snort pitched down 4 semitones (a larger nose)"),
    R("giraffid", "alarm", "giraffe alarm snort", ["equid/alarm_1"],
      lambda: shift(src("equid/alarm_1"), -2),
      "horse snort pitched down 2 semitones"),
    R("giraffid", "alarm", "giraffe alarm snort, double", ["equid/alarm_2"],
      lambda: shift(src("equid/alarm_2"), -3),
      "donkey snort pitched down 3 semitones"),
    R("giraffid", "hurt", "giraffe distress grunt", ["bison/ambient_2"],
      lambda: loudest(shift(src("bison/ambient_2"), 2), 0.9),
      "bison grunt pitched up 2 semitones, loudest 0.9 s"),
    R("giraffid", "hurt", "giraffe distress squeal-grunt", ["equid/hurt_1"],
      lambda: shift(src("equid/hurt_1"), -4),
      "horse squeal pitched down 4 semitones"),
    R("giraffid", "death", "giraffe dying groan", ["rhino/death_1"],
      lambda: env_fade_out(glide(src("rhino/death_1"), 1.0, -3.0)),
      "rhino groan with a pitch glide from +1 to -3 semitones and a long fade"),

    # ---- tapir: high thin whistle-squeals (contact/alarm), snorts, huffs and clicks
    R("tapir", "call", "tapir whistle-squeal", ["large_rodent/hurt_1"],
      lambda: loudest(shift(src("large_rodent/hurt_1"), -3), 1.4),
      "guinea pig wheek-squeal (f0 873 Hz) pitched down 3 semitones (~735 Hz), loudest 1.4 s"),
    R("tapir", "call", "tapir two-note whistle", ["macrauchenia/hurt_1"],
      lambda: (lambda w: series([w, shift(src("macrauchenia/hurt_1"), 8)], [0.16]))(shift(src("macrauchenia/hurt_1"), 7)),
      "alpaca high moan (f0 441 Hz) pitched up 7 then 8 semitones (~660-700 Hz), twice with a 0.16 s gap"),
    R("tapir", "ambient", "tapir huff", ["equid/ambient_2"],
      lambda: shift(src("equid/ambient_2"), 2),
      "horse soft nicker/huff pitched up 2 semitones"),
    R("tapir", "ambient", "tapir snuffle", ["glyptodont/ambient_3"],
      lambda: shift(src("glyptodont/ambient_3"), 3),
      "pig low grunt pitched up 3 semitones"),
    R("tapir", "ambient", "tapir clicks", ["large_rodent/ambient_2"],
      lambda: cut(shift(src("large_rodent/ambient_2"), -6), 0, 2.2),
      "guinea pig bubbling clicks pitched down 6 semitones, cut to 2.2 s"),
    R("tapir", "hurt", "tapir pain squeal", ["glyptodont/hurt_2"],
      lambda: shift(src("glyptodont/hurt_2"), 2),
      "pig squeal pitched up 2 semitones"),
    R("tapir", "hurt", "tapir short squeal", ["large_rodent/hurt_1"],
      lambda: loudest(shift(src("large_rodent/hurt_1"), -1), 0.7),
      "guinea pig squeal pitched down 1 semitone, loudest 0.7 s"),
    R("tapir", "death", "tapir dying squeal", ["glyptodont/hurt_2"],
      lambda: env_fade_out(glide(src("glyptodont/hurt_2"), -1.0, -7.0)),
      "pig squeal with a pitch glide from -1 to -7 semitones and a long fade"),

    # ---- wildebeest: the nasal, metallic "gnu" honk, repeated in bouts; snorts in alarm; bawls when hurt
    R("wildebeest", "call", "wildebeest honk series (ka-gnu, gnu, gnu)", ["bison/ambient_1"],
      lambda: hp(series([loudest(shift(src("bison/ambient_1"), s), 0.42) for s in (4.0, 5.0, 3.5)], [0.2, 0.24]), 160),
      "bison grunt-bellow pitched up 4 / 5 / 3.5 semitones, loudest 0.42 s of each, three honks 0.2-0.24 s apart, HP 160 Hz"),
    R("wildebeest", "call", "wildebeest honk series, long bout", ["bison/ambient_2"],
      lambda: hp(series([loudest(shift(src("bison/ambient_2"), s), 0.38) for s in (5.0, 4.0, 5.5, 4.5)], [0.18, 0.22, 0.2]), 160),
      "bison grunt pitched up 5 / 4 / 5.5 / 4.5 semitones, loudest 0.38 s of each, four honks ~0.2 s apart, HP 160 Hz"),
    R("wildebeest", "ambient", "wildebeest single grunt-honk", ["bison/ambient_1"],
      lambda: hp(loudest(shift(src("bison/ambient_1"), 5), 0.6), 160),
      "bison grunt-bellow pitched up 5 semitones, loudest 0.6 s, HP 160 Hz"),
    R("wildebeest", "ambient", "wildebeest low grunt", ["antelope/ambient_2"],
      lambda: shift(src("antelope/ambient_2"), -3),
      "scimitar oryx grunt pitched down 3 semitones"),
    R("wildebeest", "ambient", "wildebeest nasal huffs", ["bison/ambient_3"],
      lambda: cut(shift(src("bison/ambient_3"), 3), 0, 2.4),
      "bison nasal huffs pitched up 3 semitones, cut to 2.4 s"),
    R("wildebeest", "alarm", "wildebeest alarm snorts", ["antelope/alarm_1"],
      lambda: shift(src("antelope/alarm_1"), -3),
      "blackbuck double alarm snort pitched down 3 semitones (a heavier animal)"),
    R("wildebeest", "alarm", "wildebeest single alarm snort", ["antelope/alarm_2"],
      lambda: shift(src("antelope/alarm_2"), -4),
      "blackbuck alarm snort pitched down 4 semitones"),
    R("wildebeest", "hurt", "wildebeest bawl", ["caprine/hurt_1"],
      lambda: shift(src("caprine/hurt_1"), -7),
      "goat kid bleat (f0 371 Hz) pitched down 7 semitones (~248 Hz)"),
    R("wildebeest", "hurt", "wildebeest sharp grunt", ["bison/ambient_2"],
      lambda: hp(loudest(shift(src("bison/ambient_2"), 6), 0.4), 160),
      "bison grunt pitched up 6 semitones, loudest 0.4 s, HP 160 Hz"),
    R("wildebeest", "death", "wildebeest dying bellow", ["bison/call_2"],
      lambda: env_fade_out(glide(src("bison/call_2"), 2.0, -2.0)),
      "bison bellow with a pitch glide from +2 to -2 semitones and a long fade"),

    # ---- turtles: a short forced-exhale hiss, brighter for the small softshell, lower for the tortoise
    R("softshell_turtle", "threat", "softshell turtle hiss", ["crocodilian/threat_1"],
      lambda: hp(env_fade_out(loudest(shift(src("crocodilian/threat_1"), 5), 0.7), 0.5), 900),
      "alligator hiss pitched up 5 semitones, loudest 0.7 s, HP 900 Hz, faded out"),
    R("softshell_turtle", "threat", "softshell turtle hiss, sharp", ["crocodilian/threat_2"],
      lambda: hp(env_fade_out(loudest(shift(src("crocodilian/threat_2"), 6), 0.55), 0.5), 1000),
      "alligator hiss-growl pitched up 6 semitones, loudest 0.55 s, HP 1 kHz, faded out"),
    R("tortoise", "threat", "tortoise hiss", ["crocodilian/threat_1"],
      lambda: hp(env_fade_out(loudest(shift(src("crocodilian/threat_1"), 2), 0.9), 0.5), 500),
      "alligator hiss pitched up 2 semitones, loudest 0.9 s, HP 500 Hz, faded out"),
    R("tortoise", "threat", "tortoise hiss, short", ["crocodilian/threat_2"],
      lambda: hp(env_fade_out(loudest(shift(src("crocodilian/threat_2"), 3), 0.6), 0.5), 600),
      "alligator hiss-growl pitched up 3 semitones, loudest 0.6 s, HP 600 Hz, faded out"),

    # ---- kangaroo: soft clucks and cough-grunts, a guttural growl-cough + hiss when squaring up, grunts when hurt
    R("kangaroo", "ambient", "kangaroo cough-grunt", ["glyptodont/ambient_3"],
      lambda: shift(src("glyptodont/ambient_3"), 4),
      "pig low grunt pitched up 4 semitones"),
    R("kangaroo", "ambient", "kangaroo clucking (mother to joey)", ["large_rodent/ambient_2"],
      lambda: cut(shift(src("large_rodent/ambient_2"), -4), 0, 1.4),
      "guinea pig bubbling clicks pitched down 4 semitones, cut to 1.4 s"),
    R("kangaroo", "ambient", "kangaroo short grunt", ["glyptodont/ambient_1"],
      lambda: loudest(shift(src("glyptodont/ambient_1"), 5), 0.45),
      "pig grunt pitched up 5 semitones, loudest 0.45 s"),
    R("kangaroo", "threat", "kangaroo growl-cough", ["ground_sloth/threat_2"],
      lambda: loudest(shift(src("ground_sloth/threat_2"), 5), 1.3),
      "black bear growl pitched up 5 semitones, loudest 1.3 s"),
    R("kangaroo", "threat", "kangaroo cough + hiss", ["glyptodont/ambient_1", "crocodilian/threat_1"],
      lambda: (lambda g: mix(g, hp(env_fade_out(loudest(shift(src("crocodilian/threat_1"), 6), 0.45), 0.6), 1200), 0.8, at=len(g) / SR * 0.7))(
          loudest(shift(src("glyptodont/ambient_1"), 3), 0.45)),
      "pig grunt pitched up 3 semitones, then an alligator hiss (+6 semitones, HP 1.2 kHz) overlapping its tail"),
    R("kangaroo", "hurt", "kangaroo pain grunt", ["glyptodont/hurt_1"],
      lambda: shift(src("glyptodont/hurt_1"), 5),
      "pig sharp grunt pitched up 5 semitones"),
    R("kangaroo", "hurt", "kangaroo short squeal", ["large_rodent/hurt_1"],
      lambda: loudest(src("large_rodent/hurt_1"), 0.6),
      "guinea pig squeal, loudest 0.6 s"),
    R("kangaroo", "death", "kangaroo dying grunt", ["glyptodont/death_1"],
      lambda: env_fade_out(glide(src("glyptodont/death_1"), 3.0, -1.0)),
      "pig long grunt with a pitch glide from +3 to -1 semitones and a long fade"),

    # ---- single missing keys
    R("diprotodont", "hurt", "diprotodont pain bellow", ["diprotodont/threat_1"],
      lambda: loudest(shift(src("diprotodont/threat_1"), 2), 0.8),
      "the diprotodont's own threat burst pitched up 2 semitones, loudest 0.8 s"),
    R("diprotodont", "hurt", "diprotodont pain grunt", ["glyptodont/hurt_1"],
      lambda: shift(src("glyptodont/hurt_1"), -3),
      "pig sharp grunt pitched down 3 semitones (a far bigger animal)"),
    R("diprotodont", "death", "diprotodont dying groan", ["diprotodont/ambient_1"],
      lambda: env_fade_out(cut(glide(src("diprotodont/ambient_1"), -2.0, -5.0), 0, 3.3)),
      "the diprotodont's own ambient bellow with a pitch glide from -2 to -5 semitones, long fade, cut to 3.3 s"),
    R("antelope", "hurt", "antelope pain bleat", ["caprine/hurt_2"],
      lambda: shift(src("caprine/hurt_2"), -2),
      "goat kid bleat pitched down 2 semitones"),
    R("antelope", "hurt", "antelope pain bleat, short", ["caprine/hurt_1"],
      lambda: loudest(shift(src("caprine/hurt_1"), -1), 0.8),
      "goat kid bleat pitched down 1 semitone, loudest 0.8 s"),
    R("antelope", "death", "antelope dying bleat", ["caprine/death_1"],
      lambda: env_fade_out(glide(src("caprine/death_1"), -1.0, -5.0)),
      "goat bleat with a pitch glide from -1 to -5 semitones and a long fade"),
    R("macrauchenia", "death", "macrauchenia dying moan", ["macrauchenia/ambient_3"],
      lambda: env_fade_out(glide(src("macrauchenia/ambient_3"), -1.0, -6.0)),
      "the macrauchenia's own alpaca moan with a pitch glide from -1 to -6 semitones and a long fade"),
    R("large_rodent", "death", "capybara dying squeal", ["large_rodent/hurt_1"],
      lambda: env_fade_out(cut(glide(src("large_rodent/hurt_1"), -3.0, -8.0), 0, 2.4)),
      "the large rodent's own squeal with a pitch glide from -3 to -8 semitones, long fade, cut to 2.4 s"),
]


def finish(x, key):
    x = dsp.highpass(x, 40)
    x = dsp.gate(x)
    x, _ = dsp.normalise(x, TARGET[key])
    x = dsp.fades(x, 20)
    lim = MAX_LEN.get(key, 3.4)
    if len(x) > lim * SR:
        x = dsp.fades(x[:int(lim * SR)], 60)
    pk = float(np.max(np.abs(x))) if len(x) else 0.0
    if pk > 0.85:                     # -1.4 dBFS: Vorbis overshoots the PCM peak by a few tenths of a dB
        x = x * (0.85 / pk)
    return x


def main():
    dry = "--dry" in sys.argv
    credits = json.loads(CREDITS.read_text(encoding="utf-8"))
    # idempotent: drop earlier derived clips
    for c in [c for c in credits if c.get("derived")]:
        f = STAGE / c["file"]
        if f.exists() and not dry:
            f.unlink()
    credits = [c for c in credits if not c.get("derived")]
    by_file = {c["file"][:-4]: c for c in credits}
    taken = {c["file"] for c in credits}
    import voice_probe as VP
    new = []
    for r in RECIPES:
        srcs = [by_file[s] for s in r["sources"]]
        n = 1
        while f"{r['type']}/{r['key']}_{n}.ogg" in taken:
            n += 1
        rel = f"{r['type']}/{r['key']}_{n}.ogg"
        taken.add(rel)
        if dry:
            print(f"  {rel:<34} <- {', '.join(r['sources'])}: {r['how']}")
            continue
        x = finish(r["fn"](), r["key"])
        out = STAGE / rel
        out.parent.mkdir(parents=True, exist_ok=True)
        dsp.write_ogg(str(out), x)
        pr = VP.probe(dsp.load(str(out)))
        lic = sorted({s["licence"] for s in srcs})
        pd = any("public domain" in l.lower() for l in lic)
        cc0 = any("cc0" in l.lower() or "creative commons 0" in l.lower() for l in lic)
        licence = ("CC0 1.0 / public domain" if pd and cc0 else "public domain" if pd else "CC0 1.0 (Creative Commons 0)") + " -- derived"
        rec = {
            "batch": "D1", "type": r["type"], "key": r["key"], "file": rel, "derived": True,
            "stand_in": f"DERIVED: {r['imitates']}",
            "source": "derived from staged CC0 / PD recordings",
            "source_title": "Derived from: " + "; ".join(s.get("source_title", s["file"]) for s in srcs),
            "source_url": srcs[0].get("source_url", ""),
            "author": "; ".join(sorted({s.get("author", "") for s in srcs})),
            "licence": licence, "licence_url": srcs[0].get("licence_url", ""),
            "licence_evidence": [s.get("licence_evidence", "") for s in srcs],
            "derived_from": [{"file": s["file"], "source_url": s.get("source_url", ""), "author": s.get("author", ""),
                              "licence": s["licence"]} for s in srcs],
            "retrieved": "2026-11-18",
            "processing": r["how"] + "; HP 40 Hz, gate -45 dBFS, normalised to " + f"{TARGET[r['key']]} dBFS active RMS, 20 ms fades, mono 44.1 kHz, Vorbis q0.4",
            "sha256": hashlib.sha256(out.read_bytes()).hexdigest(),
            "bytes": out.stat().st_size,
            "probe": pr,
        }
        new.append(rec)
        print(f"  {rel:<34} {pr['dur']:4.1f}s f0 {str(pr['f0']):>6} centroid {pr['centroid']:>6.0f}  {r['imitates']}")
    if not dry:
        CREDITS.write_text(json.dumps(credits + new, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"{len(new)} derived clips staged, {sum(c['bytes'] for c in new) / 1e6:.2f} MB")


if __name__ == "__main__":
    main()
