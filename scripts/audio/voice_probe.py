#!/usr/bin/env python3
"""Measure clips for voice design (2.8.0 derived voices): duration, voiced fundamental (median f0 of voiced frames,
autocorrelation), spectral centroid, and how much of the clip is voiced. Numbers, because the designer cannot listen.

    python scripts/audio/voice_probe.py mammoth/ambient_1 equid/alarm_1 ...   (paths under scratch/audio_stage, no .ogg)
"""
import pathlib
import sys

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import dsp  # noqa: E402

STAGE = pathlib.Path(__file__).resolve().parents[2] / "scratch" / "audio_stage"


def f0_track(x, fmin=40.0, fmax=1500.0, frame=0.04, hop=0.01):
    n, h = int(dsp.SR * frame), int(dsp.SR * hop)
    lo, hi = int(dsp.SR / fmax), int(dsp.SR / fmin)
    out = []
    for i in range(0, max(1, len(x) - n), h):
        w = x[i:i + n] * np.hanning(n)
        if np.sqrt(np.mean(w ** 2)) < 10 ** (-40 / 20):
            out.append((np.nan, 0.0))
            continue
        ac = np.correlate(w, w, "full")[n - 1:]
        ac /= ac[0] + 1e-12
        seg = ac[lo:min(hi, len(ac) - 1)]
        if len(seg) < 3:
            out.append((np.nan, 0.0))
            continue
        k = int(np.argmax(seg)) + lo
        out.append((dsp.SR / k, float(ac[k])))
    return out


def probe(x):
    tr = f0_track(x)
    voiced = [f for f, c in tr if c > 0.55 and not np.isnan(f)]
    spec = np.abs(np.fft.rfft(x * np.hanning(len(x))))
    freqs = np.fft.rfftfreq(len(x), 1 / dsp.SR)
    cen = float((spec * freqs).sum() / (spec.sum() + 1e-12))
    return {"dur": round(len(x) / dsp.SR, 2), "f0": round(float(np.median(voiced)), 0) if voiced else None,
            "voiced": round(len(voiced) / max(1, len(tr)), 2), "centroid": round(cen, 0)}


if __name__ == "__main__":
    for a in sys.argv[1:]:
        x = dsp.load(str(STAGE / (a + ".ogg")))
        print(f"{a:<30}", probe(x))
