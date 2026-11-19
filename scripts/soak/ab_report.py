#!/usr/bin/env python3
"""Compare two soak runs' tick times:  python scripts/soak/ab_report.py <labelA> <labelB>"""
import csv
import pathlib
import statistics
import sys

OUT = pathlib.Path(__file__).resolve().parents[2] / "scratch" / "soak"


def load(label):
    with open(OUT / f"{label}_mspt.csv", encoding="utf-8") as f:
        vals = [float(r["mspt"]) for r in csv.DictReader(f) if r["mspt"] not in ("", "None")]
    # drop the first minute (world warm-up, initial pathfinding burst)
    return vals[6:] if len(vals) > 12 else vals


def main():
    a, b = sys.argv[1], sys.argv[2]
    va, vb = load(a), load(b)
    ma, mb = statistics.mean(va), statistics.mean(vb)
    pa = sorted(va)[int(len(va) * 0.95) - 1]
    pb = sorted(vb)[int(len(vb) * 0.95) - 1]
    print(f"{a}: mean {ma:.2f} ms  p95 {pa:.2f} ms  ({len(va)} samples)")
    print(f"{b}: mean {mb:.2f} ms  p95 {pb:.2f} ms  ({len(vb)} samples)")
    print(f"{b} vs {a}: {100 * (mb - ma) / ma:+.1f}% mean, {100 * (pb - pa) / pa:+.1f}% p95")


if __name__ == "__main__":
    main()
