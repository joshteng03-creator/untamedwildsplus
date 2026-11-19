"""DSP helpers for P6a: decode, mono, resample, filter, detect, normalise."""
from __future__ import annotations

import math
import site
import sys
from fractions import Fraction

# `python -I` drops the per-user site-packages (where numpy/scipy/soundfile are
# installed); add that one known directory back explicitly - never cwd or the
# downloads folder.
_US = site.getusersitepackages()
if _US not in sys.path:
    sys.path.append(_US)

import numpy as np  # noqa: E402
import soundfile as sf
from scipy.ndimage import minimum_filter1d, uniform_filter1d
from scipy.signal import butter, resample_poly, sosfiltfilt

SR = 44100
FRAME = 0.02  # 20 ms analysis frames


def db(x):
    return 20 * np.log10(np.maximum(np.abs(x), 1e-9))


def load(path: str) -> np.ndarray:
    """Decode any libsndfile-readable file -> mono float64 at 44.1 kHz."""
    x, sr = sf.read(path, dtype="float64", always_2d=True)
    x = x.mean(axis=1)
    if sr != SR:
        fr = Fraction(SR, int(sr)).limit_denominator(1000)
        x = resample_poly(x, fr.numerator, fr.denominator)
    x = x - np.mean(x)
    return x


def highpass(x, hz):
    sos = butter(4, hz, btype="highpass", fs=SR, output="sos")
    return sosfiltfilt(sos, x)


def lowpass(x, hz):
    sos = butter(4, hz, btype="lowpass", fs=SR, output="sos")
    return sosfiltfilt(sos, x)


def frame_rms_db(x, frame=FRAME):
    n = int(SR * frame)
    m = len(x) // n
    if m == 0:
        return np.array([db(np.sqrt(np.mean(x ** 2)) if len(x) else 0)])
    f = x[: m * n].reshape(m, n)
    return 10 * np.log10(np.maximum(np.mean(f ** 2, axis=1), 1e-18))


def detect_events(x, rel_db=12.0, abs_floor=-50.0, gap=0.25, min_len=0.12):
    """Energy events: frames louder than (noise floor + rel_db), gaps < `gap` merged."""
    e = frame_rms_db(x)
    floor = np.percentile(e, 15)
    thr = max(floor + rel_db, abs_floor)
    on = e > thr
    ev = []
    i = 0
    n = len(on)
    while i < n:
        if on[i]:
            j = i
            while j < n and on[j]:
                j += 1
            ev.append([i, j])
            i = j
        else:
            i += 1
    merged = []
    g = int(gap / FRAME)
    for s, t in ev:
        if merged and s - merged[-1][1] <= g:
            merged[-1][1] = t
        else:
            merged.append([s, t])
    out = []
    for s, t in merged:
        if (t - s) * FRAME < min_len:
            continue
        seg = e[s:t]
        out.append({
            "start": round(s * FRAME, 2), "end": round(t * FRAME, 2),
            "dur": round((t - s) * FRAME, 2), "peak_db": round(float(seg.max()), 1),
            "snr": round(float(seg.max() - floor), 1),
        })
    return out, float(floor), float(thr)


def centroid(x):
    if len(x) < 2048:
        return 0.0
    spec = np.abs(np.fft.rfft(x * np.hanning(len(x))))
    f = np.fft.rfftfreq(len(x), 1 / SR)
    return float((spec * f).sum() / max(spec.sum(), 1e-9))


def trim_silence(x, below_peak_db=38.0, abs_db=-55.0, margin=0.04):
    e = frame_rms_db(x, 0.01)
    if len(e) == 0:
        return x
    thr = max(e.max() - below_peak_db, abs_db)
    idx = np.where(e > thr)[0]
    if len(idx) == 0:
        return x
    n = int(SR * 0.01)
    s = max(0, idx[0] * n - int(margin * SR))
    t = min(len(x), (idx[-1] + 1) * n + int(margin * SR))
    return x[s:t]


def active_rms_db(x, gate_db=-45.0):
    """RMS over frames that are not near-silent (so pauses don't drag it down)."""
    e = frame_rms_db(x)
    if len(e) == 0:
        return -120.0
    thr = max(e.max() - 25.0, gate_db)
    act = e[e > thr]
    if len(act) == 0:
        act = e
    return float(10 * np.log10(np.mean(10 ** (act / 10))))


def limiter(x, ceiling_db=-1.0, look=0.005):
    c = 10 ** (ceiling_db / 20)
    a = np.abs(x)
    g = np.minimum(1.0, c / np.maximum(a, 1e-12))
    L = max(3, int(look * SR))
    g = minimum_filter1d(g, size=2 * L + 1)
    g = uniform_filter1d(g, size=L)
    y = x * g
    pk = np.max(np.abs(y)) if len(y) else 0
    if pk > c:  # numerical safety
        y *= c / pk
    return y


def normalise(x, target_db, ceiling_db=-1.0, max_limit_db=6.0):
    """Gain to the active-RMS target; peaks over the ceiling are limited by at
    most `max_limit_db`, beyond that the gain itself is reduced."""
    cur = active_rms_db(x)
    gain_db = target_db - cur
    pk = float(db(np.max(np.abs(x))))
    if pk + gain_db > ceiling_db + max_limit_db:
        gain_db = ceiling_db + max_limit_db - pk
    y = x * 10 ** (gain_db / 20)
    y = limiter(y, ceiling_db)
    return y, gain_db


def gate(x, thr_db=-45.0, floor_db=-20.0, attack=0.005, release=0.08):
    """Smooth downward gate: frames under thr_db are attenuated by floor_db."""
    n = int(SR * 0.005)
    m = int(math.ceil(len(x) / n))
    pad = np.pad(x, (0, m * n - len(x)))
    e = 10 * np.log10(np.maximum(np.mean(pad.reshape(m, n) ** 2, axis=1), 1e-18))
    open_ = (e > thr_db).astype(float)
    # hold the gate open around events (release) and open early (attack)
    rel = max(1, int(release / 0.005))
    open_ = np.maximum.reduce([np.roll(open_, k) for k in range(0, rel)] +
                              [np.roll(open_, -k) for k in range(1, max(2, int(attack / 0.005) + 1))])
    gfr = np.where(open_ > 0, 1.0, 10 ** (floor_db / 20))
    gfr = uniform_filter1d(gfr, size=5)
    g = np.repeat(gfr, n)[: len(x)]
    return x * g


def fades(x, ms=20):
    n = min(int(SR * ms / 1000), len(x) // 2)
    if n <= 0:
        return x
    r = np.linspace(0, 1, n) ** 2
    y = x.copy()
    y[:n] *= r
    y[-n:] *= r[::-1]
    return y


def stats(x):
    pk = float(db(np.max(np.abs(x)))) if len(x) else -120
    rms = float(10 * np.log10(max(np.mean(x ** 2), 1e-18))) if len(x) else -120
    return {"dur": round(len(x) / SR, 3), "peak_db": round(pk, 2), "rms_db": round(rms, 2),
            "active_rms_db": round(active_rms_db(x), 2)}


def write_ogg(path, x, quality=0.4):
    # libsndfile: Vorbis quality = 1 - compression_level
    sf.write(path, x.astype(np.float32), SR, format="OGG", subtype="VORBIS",
             compression_level=1.0 - quality)
