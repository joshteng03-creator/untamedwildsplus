#!/usr/bin/env python3
"""Headless server soak for 2.8.0: tick time, census and log errors, without anyone in game.

Boots the DEV server (`gradlew runServer`, working dir run-server) with RCON on, applies a scenario, samples
`/forge tps` every --sample seconds into scratch/soak/<label>_mspt.csv, runs the mod's census every minute (it writes
run-server/logs/uw_census.csv), greps the server log for errors, then stops the server.

A/B: run twice with a different --config override, e.g. one with the 2.8.0 features off:
    python scripts/soak/soak.py --label off --minutes 20 --set performance.shared_herd_threat_scan=false
    python scripts/soak/soak.py --label on  --minutes 20 --set performance.shared_herd_threat_scan=true
    python scripts/soak/ab_report.py off on
(A comparison against the 2.7.0 jar itself needs a production Forge server; see the plan, P0.)

Scenario files (scripts/soak/scenarios/*.txt) are plain server commands, one per line; `# ...` comments; `@wait N`
pauses N seconds. They run once the world is up.
"""
import argparse
import csv
import os
import pathlib
import re
import shutil
import subprocess
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from rcon import Rcon  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parents[2]
RUN = ROOT / "run-server"
OUT = ROOT / "scratch" / "soak"
PASSWORD = "uwsoak"
PORT = 25575
ERR = re.compile(r"Exception|ERROR|NullPointer|Can't keep up|Unable to play")


def write_server_files(seed):
    RUN.mkdir(exist_ok=True)
    (RUN / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    props = {
        "enable-rcon": "true", "rcon.port": str(PORT), "rcon.password": PASSWORD, "online-mode": "false",
        "view-distance": "10", "simulation-distance": "10", "level-seed": str(seed), "spawn-protection": "0",
        "level-name": "soak_world", "motd": "uw soak",
    }
    (RUN / "server.properties").write_text("".join(f"{k}={v}\n" for k, v in props.items()), encoding="utf-8")


def apply_config_overrides(sets):
    """Applies --set overrides and returns the file's original text (restored after the run), or None."""
    cfg = RUN / "config" / "untamedwilds-common.toml"
    if not sets:
        return None
    if not cfg.exists():
        print("config not generated yet; overrides apply from the second run on", file=sys.stderr)
        return None
    text = cfg.read_text(encoding="utf-8")
    original = text
    for kv in sets:
        key, val = kv.split("=", 1)
        leaf = key.split(".")[-1]
        text, n = re.subn(r"(?m)^(\s*" + re.escape(leaf) + r"\s*=\s*).*$", lambda m: m.group(1) + val, text)
        if not n:
            # a key added in a newer build is not in the file until the server has run once with it: insert it under
            # its [section] header (Forge keeps a value it finds for a known key)
            section = key.split(".")[0]
            text, n = re.subn(r"(?m)^(\[" + re.escape(section) + r"\][ \t]*\r?\n)",
                              lambda m: m.group(1) + "\t" + leaf + " = " + val + "\n", text, count=1)
        print(f"config {leaf} = {val}" + ("" if n else "  (KEY NOT FOUND)"))
    cfg.write_text(text, encoding="utf-8")
    return original


def wait_for(log_path, pattern, timeout):
    t0 = time.time()
    rx = re.compile(pattern)
    while time.time() - t0 < timeout:
        if log_path.exists() and rx.search(log_path.read_text(encoding="utf-8", errors="replace")):
            return True
        time.sleep(2)
    return False


def keep_awake(on):
    """Windows: hold off system sleep while a soak runs (one soak slept through its middle and came back 11 hours
    behind). SetThreadExecutionState(ES_CONTINUOUS | ES_SYSTEM_REQUIRED); released when the process ends anyway."""
    if os.name != "nt":
        return
    try:
        import ctypes
        ctypes.windll.kernel32.SetThreadExecutionState(0x80000000 | (0x00000001 if on else 0))
    except Exception:
        pass


def main():
    keep_awake(True)
    ap = argparse.ArgumentParser()
    ap.add_argument("--label", default="run")
    ap.add_argument("--minutes", type=float, default=10)
    ap.add_argument("--sample", type=int, default=10)
    ap.add_argument("--seed", default="2808")
    ap.add_argument("--scenario", default="s1_flat_herds")
    ap.add_argument("--set", action="append", default=[])
    ap.add_argument("--keep-world", action="store_true",
                    help="reuse the last run's world (default: every run starts from a fresh world off --seed, "
                         "or an A/B compares two different populations)")
    a = ap.parse_args()

    OUT.mkdir(parents=True, exist_ok=True)
    write_server_files(a.seed)
    if not a.keep_world and (RUN / "soak_world").exists():
        shutil.rmtree(RUN / "soak_world")
    original_cfg = apply_config_overrides(a.set)
    log = OUT / f"{a.label}_server.log"
    env = dict(os.environ)
    env.setdefault("JAVA_HOME", r"C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot")
    gradlew = str(ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    with open(log, "w", encoding="utf-8") as lf:
        proc = subprocess.Popen([gradlew, "runServer", "--console=plain"], cwd=ROOT, stdout=lf, stderr=subprocess.STDOUT, env=env)
    try:
        if not wait_for(log, r"Done \(\d", 1500):
            raise SystemExit("server did not start; see " + str(log))
        with Rcon("127.0.0.1", PORT, PASSWORD) as r:
            r.cmd("gamerule untamedwildsEcosystemMode true")
            r.cmd("untamedwilds census auto 1")
            scen = ROOT / "scripts" / "soak" / "scenarios" / f"{a.scenario}.txt"
            for line in scen.read_text(encoding="utf-8").splitlines():
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                if line.startswith("@wait"):
                    time.sleep(float(line.split()[1]))
                    continue
                r.cmd(line)
            rows = []
            t_end = time.time() + a.minutes * 60
            while time.time() < t_end:
                txt = r.cmd("forge tps overworld")
                m = re.search(r"Mean tick time: ([\d.]+) ms", txt)
                tps = re.search(r"Mean TPS: ([\d.]+)", txt)
                rows.append((round(time.time()), float(m.group(1)) if m else None, float(tps.group(1)) if tps else None))
                time.sleep(a.sample)
            r.cmd("stop")
        with open(OUT / f"{a.label}_mspt.csv", "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["time", "mspt", "tps"])
            w.writerows(rows)
        proc.wait(timeout=300)
    finally:
        if proc.poll() is None:
            proc.kill()
        keep_awake(False)
        if original_cfg is not None:
            # --set is for THIS run only: a later run without it must see the defaults again
            (RUN / "config" / "untamedwilds-common.toml").write_text(original_cfg, encoding="utf-8")
    errors = [ln for ln in log.read_text(encoding="utf-8", errors="replace").splitlines() if ERR.search(ln)]
    vals = [m for _, m, _ in rows if m is not None]
    print(f"{a.label}: {len(rows)} samples, mean MSPT {sum(vals) / max(1, len(vals)):.2f}, log error lines {len(errors)}")
    for e in errors[:15]:
        print("   ", e[:200])


if __name__ == "__main__":
    main()
