#!/usr/bin/env python3
"""Offline ecosystem estimator (2026-10-24) -- predicts, from the mod's own formulas and species data, whether the
self-regulating ecosystem (docs/plans/2026-10-24-ecosystem-self-regulation.md) can hold predators near the target
prey-per-predator ratio without crashing either side.

For every species it reports, from ComplexMob's real timing formulas and the entity JSONs:
  * gestation, maturity, first birth, inter-birth interval, breeding span (to senescence), mean litter;
  * r_max: the intrinsic growth rate with no deaths (Euler-Lotka on a female's birth schedule), per in-game day.
For each predator x prey pairing it then asks the question that decides stability:
  * kills/day per predator (hunger drain / food per kill, the carcass pool split across the pack);
  * at the set point (predator_breed_ratio prey per predator) predation removes kills/R of the prey per day --
    that must stay BELOW the prey's growth rate or the prey declines toward the floor the gate defends;
  * a Lotka-Volterra-style cycle period estimate, 2*pi / sqrt(r_prey * m_pred).

Run:  python scripts/eco_equilibrium.py [--ratio 30]
All tunables mirror ConfigGamerules defaults; change them here to try a setting before a soak test.
"""
import json
import math
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
ENT = ROOT / "src/main/resources/data/untamedwilds/entities"

DAY = 24000
CYCLE = 24000                       # cycle_length
GESTATION_MULT = 0.35               # gestation_multiplier
MATURITY_MULT = 0.5                 # maturity_multiplier
PRED_GESTATION_MULT = 1.0           # predator_gestation_multiplier (was 0.33)
PRED_MATURITY_MULT = 1.0            # predator_maturity_multiplier (was 0.33)
PRED_LITTER_BONUS = 0               # predator_litter_bonus (was 2)
LIFESPAN_MULT = 8.0                 # lifespan_multiplier
PRED_LIFESPAN_MULT = 5.0            # predator_lifespan_multiplier
SENESCENCE = 0.75                   # senescence_fraction
REARING_FRACTION = 0.5              # predator_rearing_fraction
MALE_REFRACTORY = 1200              # male_refractory_ticks (no longer the whole gestation)
CARCASS_PER_HP = 8                  # carcass_food_per_health
LACTATION = 1.5                     # lactation_drain_factor
RATIO = float(sys.argv[sys.argv.index("--ratio") + 1]) if "--ratio" in sys.argv else 30.0

# per-1000-tick hunger drain of each predator class (EntityBear / BigCat / DireWolf / Hyena aiStep)
DRAIN = {"bear": 2, "big_cat": 2, "dire_wolf": 4, "hyena": 4}
PREDATORS = [("bear", "brown"), ("bear", "short_faced"), ("big_cat", "lion"), ("big_cat", "american_lion"),
             ("big_cat", "smilodon_fatalis"), ("dire_wolf", "dire_wolf"), ("dire_wolf", "gray_wolf"), ("hyena", "spotted")]
PREY = [("bison", "plains"), ("deer", "red_deer"), ("antelope", "tetrameryx"), ("wildebeest", "blue_wildebeest"),
        ("caprine", "argali"), ("glyptodont", "glyptodon"), ("equid", None), ("mammoth", "columbian")]


def load(typ):
    return json.loads((ENT / f"{typ}.json").read_text(encoding="utf-8-sig"))


def species(typ, name):
    d = load(typ)
    for sp in d["species"]:
        if name is None or sp["name"] == name:
            out = dict(d)
            out.update(sp)
            return out
    return None


def timing(data, carnivore):
    gt = data.get("growing_time", 6)
    gest = gt * CYCLE * GESTATION_MULT * (PRED_GESTATION_MULT if carnivore else 1.0)
    mature = gt * CYCLE * 2 * MATURITY_MULT * (PRED_MATURITY_MULT if carnivore else 1.0)
    life = mature * (PRED_LIFESPAN_MULT if carnivore else LIFESPAN_MULT)
    offspring = data.get("offspring", 1) + (PRED_LITTER_BONUS if carnivore else 0)
    litter = 1 + offspring / 2.0                      # 1 + nextInt(offspring + 1), mean
    interbirth = gest + (mature * REARING_FRACTION if carnivore else 0)
    first = mature + gest
    span_end = life * SENESCENCE
    return dict(gest=gest, mature=mature, life=life, litter=litter, interbirth=interbirth, first=first, end=span_end)


def r_max(t):
    """Euler-Lotka for daughters: sum_k (litter/2) e^{-r (first + k*interbirth)} = 1."""
    ages = []
    a = t["first"]
    while a <= t["end"]:
        ages.append(a / DAY)
        a += t["interbirth"]
    if not ages:
        return -1.0, 0.0
    b = t["litter"] / 2.0
    r0 = b * len(ages)
    f = lambda r: sum(b * math.exp(-r * x) for x in ages) - 1.0
    lo, hi = -1.0, 2.0
    for _ in range(100):
        mid = (lo + hi) / 2
        if f(mid) > 0:
            lo = mid
        else:
            hi = mid
    return (lo + hi) / 2, r0


def main():
    print(f"=== species life histories (days); set point = {RATIO:.0f} prey per predator ===")
    print(f"{'species':<28}{'gest':>6}{'mature':>8}{'1st':>6}{'every':>7}{'span':>7}{'litter':>7}{'R0':>7}{'r_max/d':>9}")
    rows = {}
    for typ, name in PREDATORS + PREY:
        data = species(typ, name)
        if data is None:
            continue
        carn = typ in DRAIN
        t = timing(data, carn)
        r, r0 = r_max(t)
        rows[(typ, data["name"])] = (data, t, r)
        print(f"{typ + ':' + data['name']:<28}{t['gest'] / DAY:6.1f}{t['mature'] / DAY:8.1f}{t['first'] / DAY:6.1f}"
              f"{t['interbirth'] / DAY:7.1f}{(t['end'] - t['first']) / DAY:7.1f}{t['litter']:7.2f}{r0:7.1f}{r:9.3f}")

    print("\n=== predator x prey: is the set point sustainable? ===")
    print(f"{'predator':<22}{'prey':<26}{'pack':>5}{'kills/d/pred':>13}{'pred.rate':>10}{'prey r':>8}{'verdict':>10}{'cycle d':>9}")
    for (ptyp, pname) in PREDATORS:
        key = next((k for k in rows if k[0] == ptyp and k[1] == pname), None)
        if key is None:
            continue
        pdata, pt, pr = rows[key]
        pack = max(1, min(int(pdata.get("flags", {}).get("groupCount", 1)), 8))
        drain_day = DRAIN[ptyp] * 24 * (1 + (LACTATION - 1) * 0.5)    # about half the adult time is rearing
        m_pred = 1.0 / (pt["life"] / DAY)                               # background death rate, per day
        for (qtyp, qname) in PREY:
            qkey = next((k for k in rows if k[0] == qtyp and (qname is None or k[1] == qname)), None)
            if qkey is None:
                continue
            qdata, qt, qr = rows[qkey]
            hp = qdata.get("health", 30) or 30
            per_eater = min(120.0, hp * CARCASS_PER_HP / pack)
            # kills per predator per day: each eater needs drain_day; one kill feeds the whole pack per_eater each
            kills = drain_day / max(1.0, per_eater) / pack
            pred_rate = kills / RATIO                                       # fraction of the prey taken per day
            verdict = "OK" if pred_rate < 0.6 * qr else ("tight" if pred_rate < qr else "DECLINE")
            cycle = 2 * math.pi / math.sqrt(max(1e-6, qr * m_pred))
            print(f"{pname:<22}{qkey[1]:<26}{pack:>5}{kills:13.3f}{pred_rate:10.4f}{qr:8.3f}{verdict:>10}{cycle:9.0f}")


if __name__ == "__main__":
    main()
