"""Inspect downloaded recordings.  `python -I scripts/audio/analyze.py [--spec out.png] [--hp HZ] <raw-folder>...`

Prints duration, noise floor and the energy events (candidate calls) with
peak / SNR / spectral centroid, so segments can be chosen for selections.json.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dsp  # noqa: E402


def src_file(folder):
    meta = json.load(open(os.path.join(folder, "meta.json"), encoding="utf-8"))
    return os.path.join(folder, meta["file"]), meta


def spectrogram(x, path, events, title):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from scipy.signal import spectrogram as sg
    f, t, S = sg(x, fs=dsp.SR, nperseg=1024, noverlap=768)
    keep = f <= 8000
    fig, ax = plt.subplots(figsize=(14, 3.2), dpi=70)
    ax.pcolormesh(t, f[keep], 10 * __import__("numpy").log10(S[keep] + 1e-14), shading="auto",
                  cmap="magma", vmin=-110, vmax=-30)
    for k, e in enumerate(events):
        ax.axvspan(e["start"], e["end"], color="cyan", alpha=0.12)
        ax.text(e["start"], 7600, str(k), color="cyan", fontsize=9)
    ax.set_title(title[:110], fontsize=9)
    ax.set_ylabel("Hz")
    fig.tight_layout()
    fig.savefig(path)
    plt.close(fig)


def sheet(items, path):
    """items: list of (x, events, title, t0) -> one stacked spectrogram image."""
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    import numpy as np
    from scipy.signal import spectrogram as sg
    n = len(items)
    fig, axes = plt.subplots(n, 1, figsize=(15, 1.9 * n), dpi=62, squeeze=False)
    for ax, (x, ev, title, t0) in zip(axes[:, 0], items):
        f, t, S = sg(x, fs=dsp.SR, nperseg=1024, noverlap=512)
        keep = f <= 6000
        ax.pcolormesh(t + t0, f[keep], 10 * np.log10(S[keep] + 1e-14), shading="auto", cmap="magma",
                      vmin=-115, vmax=-35)
        for k, e in enumerate(ev):
            if t0 <= e["start"] <= t0 + len(x) / dsp.SR:
                ax.axvline(e["start"], color="cyan", lw=0.6)
                ax.text(e["start"], 5300, str(k), color="cyan", fontsize=8)
        ax.set_title(title[:120], fontsize=9, loc="left")
        ax.set_yticks([500, 2000, 4000])
        ax.tick_params(labelsize=7)
    fig.tight_layout()
    fig.savefig(path)
    plt.close(fig)


def main(argv):
    if argv and argv[0] == "--sheet":
        # --sheet out.png folder[@start-end] ...  (hp 60; window optional)
        out = argv[1]
        items = []
        for spec_ in argv[2:]:
            fo, _, win = spec_.partition("@")
            path, meta = src_file(fo)
            x = dsp.highpass(dsp.load(path), 40)
            ev, _, _ = dsp.detect_events(x)
            t0 = 0.0
            if win:
                a, b = (float(v) for v in win.split("-"))
                x = x[int(a * dsp.SR):int(b * dsp.SR)]
                t0 = a
            items.append((x, ev, f"{os.path.basename(fo)}  {meta.get('title','')[:70]}", t0))
        sheet(items, out)
        print("wrote", out)
        return
    spec = None
    hp = 60
    rel = 12.0
    folders = []
    i = 0
    while i < len(argv):
        if argv[i] == "--spec":
            spec = argv[i + 1]; i += 2
        elif argv[i] == "--hp":
            hp = float(argv[i + 1]); i += 2
        elif argv[i] == "--rel":
            rel = float(argv[i + 1]); i += 2
        else:
            folders.append(argv[i]); i += 1
    for fo in folders:
        path, meta = src_file(fo)
        try:
            x = dsp.load(path)
        except Exception as e:  # noqa: BLE001
            print(f"## {fo}: DECODE FAIL {e}")
            continue
        x = dsp.highpass(x, hp)
        ev, floor, thr = dsp.detect_events(x, rel_db=rel)
        print(f"## {os.path.basename(os.path.dirname(fo))}/{os.path.basename(fo)}  "
              f"{len(x)/dsp.SR:.1f}s floor={floor:.1f}dB thr={thr:.1f}  '{meta.get('title','')[:60]}'")
        for k, e in enumerate(ev[:40]):
            s, t = int(e["start"] * dsp.SR), int(e["end"] * dsp.SR)
            c = dsp.centroid(x[s:t])
            print(f"  [{k:2}] {e['start']:7.2f}-{e['end']:7.2f} ({e['dur']:5.2f}s) pk={e['peak_db']:6.1f} "
                  f"snr={e['snr']:5.1f} cen={c:6.0f}Hz")
        if len(ev) > 40:
            print(f"  ... {len(ev)-40} more events")
        if spec:
            out = spec if len(folders) == 1 else spec.replace(".png", f"_{os.path.basename(fo)}.png")
            spectrogram(x, out, ev, meta.get("title", ""))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main(sys.argv[1:])
