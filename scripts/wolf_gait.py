#!/usr/bin/env python3
"""Fit the dire wolf's walk and run cycles to its rebuilt legs (2026-10-07, roadmap Phase 3.2).

The canid rig (2026-07-29) kept the old hyena-derived walk()/bob() calls, which were never fitted to
the new four-segment legs. This reproduces the exact maths the game runs and fits it:

  * Citadel  walk(part, speed, degree, invert, offset, weight, f, amt):
        rotateAngleX += s * (cos(f*speed + offset) * degree * amt + weight * amt),  s = -1 if invert
  * Citadel  bob(part, speed, degree, bounce=False, f, amt):
        rotationPointY += sin(f*speed) * amt * degree - amt * degree
  * ModelPart transform: translate(rotationPoint) . Rz . Ry . Rx  (Java model space, Y DOWN,
    ground at y = 24, the wolf faces -Z).

PHASE vs DISTANCE. Vanilla advances limbSwing by min(4 * blocks moved, 1) per tick. The new Java uses
f = limbSwing_raw / mobSize and a per-gait cadence c, so while the swing counter is not clamped a planted
paw must slide back relative to the body at K = 4 / c model units per radian -- independent of speed AND
of mob size (the maned wolf at 0.55 and the dire wolf at 0.9 both stay planted). Above 0.25 blocks/tick
the counter clamps at 1/tick, so the run is fitted for the hunting speed (attack goal x1.6).

    python scripts/wolf_gait.py --check    # metrics of the CURRENT (inherited) cycles for comparison
    python scripts/wolf_gait.py --fit2     # THE fit used (two harmonics, target path, joint limits)
    python scripts/wolf_gait.py --java     # print the Java walk() block from the saved fit
    (running with no flag runs the first, single-harmonic attempt -- kept for comparison only)
"""
import math
import pathlib
import sys

import numpy as np
from scipy.optimize import minimize

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from java2bb import parse

ORDER, UV, RP, BOX, ROT, PAR, MIRROR = parse("ModelDireWolf")
GROUND = 24.0
N = 96                                     # samples per cycle
TH = np.linspace(0, 2 * math.pi, N, endpoint=False)

SEG = ["upper", "lower", "foot", "paw"]
LIMBS = {"arm_left": "fore", "arm_right": "fore", "leg_left": "hind", "leg_right": "hind"}


def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def chain(n):
    c = [n]
    while PAR.get(c[-1]):
        c.append(PAR[c[-1]])
    return c[::-1]


def world(part, dx_rot, dy_pivot):
    """4x4 transform of `part` with extra X rotations / pivot-Y offsets on any ancestors."""
    M = np.eye(4)
    for n in chain(part):
        x, y, z = RP[n]
        y += dy_pivot.get(n, 0.0)
        rx, ry, rz = ROT.get(n) or (0.0, 0.0, 0.0)
        rx += dx_rot.get(n, 0.0)
        T = np.eye(4)
        T[:3, :3] = rot(rx, ry, rz)
        T[:3, 3] = (x, y, z)
        M = M @ T
    return M


def sole(limb, dx_rot, dy_pivot):
    """Lowest bottom corner of the paw (max y), and the paw's world pitch."""
    paw = limb + "_paw"
    M = world(paw, dx_rot, dy_pivot)
    bx, by, bz, w, h, d = BOX[paw]
    pts = [M @ np.array([x, by + h, z, 1.0]) for x in (bx, bx + w) for z in (bz, bz + d)]
    low = max(pts, key=lambda p: p[1])
    fwd = M[:3, :3] @ np.array([0, 0, -1.0])
    pitch = math.atan2(fwd[1], -fwd[2])
    return low[:3], pitch


# ---------------------------------------------------------------- gait model
class Gait:
    def __init__(self, name, K, duty, phases, bob_deg, bob_mult, pitch_deg, pitch_off, clearance):
        self.name, self.K, self.duty, self.phases = name, K, duty, phases
        self.bob_deg, self.bob_mult, self.pitch_deg, self.pitch_off, self.clearance = bob_deg, bob_mult, pitch_deg, pitch_off, clearance


WALK = Gait("walk", K=2.1, duty=0.62, phases={"leg_left": 0.0, "arm_left": 0.25, "leg_right": 0.5, "arm_right": 0.75},
            bob_deg=0.2, bob_mult=2, pitch_deg=0.0, pitch_off=0.0, clearance=1.6)
WALK.drop, WALK.bob_off = 0.85, 0.0
# rotary gallop: hind pair, then fore pair (left lead)
RUN = Gait("run", K=6.0, duty=0.36, phases={"leg_right": 0.0, "leg_left": 0.12, "arm_right": 0.45, "arm_left": 0.57},
           bob_deg=0.9, bob_mult=1, pitch_deg=5.0, pitch_off=1.2, clearance=2.6)
RUN.drop, RUN.bob_deg, RUN.bob_off = 1.7, 0.7, 0.0


def unpack(p):
    A = p[0:4]
    D = np.concatenate([[0.0], p[4:7]])
    W = p[7:11]
    return A, D, W


def body_motion(g, th):
    dy = math.sin(th * g.bob_mult) * g.bob_deg - g.bob_deg            # Citadel bob, amt folded into degree
    pitch = math.radians(g.pitch_deg) * math.cos(th + g.pitch_off)
    return dy, pitch


def batch_T(rp, rx):
    """(N,4,4) transforms: translate(rp) . Rx(rx), rp (3,) or (N,3), rx (N,)."""
    n = len(rx)
    T = np.zeros((n, 4, 4))
    c, sn = np.cos(rx), np.sin(rx)
    T[:, 0, 0] = 1
    T[:, 1, 1], T[:, 1, 2], T[:, 2, 1], T[:, 2, 2] = c, -sn, sn, c
    T[:, :3, 3] = rp
    T[:, 3, 3] = 1
    return T


def trajectory(g, limb, p):
    """Vectorised over the cycle. Every joint in these chains rotates about X only (asserted)."""
    A, D, W = unpack(p)
    ph = 2 * math.pi * g.phases[limb]
    dy = np.sin(TH * g.bob_mult) * g.bob_deg - g.bob_deg
    pitch = math.radians(g.pitch_deg) * np.cos(TH + g.pitch_off)
    links = chain(limb + "_paw")
    M = None
    for n in links:
        x, y, z = RP[n]
        rx, ry, rz = ROT.get(n) or (0.0, 0.0, 0.0)
        assert ry == 0 and rz == 0, n
        ang = np.full(N, rx)
        rp = np.tile([x, y, z], (N, 1)).astype(float)
        if n == "body_main":
            rp[:, 1] += dy
            ang = ang + pitch
        seg = n[len(limb) + 1:] if n.startswith(limb + "_") else None
        if seg in SEG:
            k = SEG.index(seg)
            ang = ang + A[k] * np.cos(TH - ph + D[k]) + W[k]
        T = batch_T(rp, ang)
        M = T if M is None else M @ T
    bx, by, bz, w, h, d = BOX[limb + "_paw"]
    corners = np.array([[x, by + h, z, 1.0] for x in (bx, bx + w) for z in (bz, bz + d)])   # (4,4)
    P = np.einsum("nij,kj->nki", M, corners)                                                # (N,4,4)
    idx = np.argmax(P[:, :, 1], axis=1)
    low = P[np.arange(N), idx]
    fwd = M[:, :3, :3] @ np.array([0, 0, -1.0])
    pp = np.arctan2(fwd[:, 1], -fwd[:, 2])
    return np.stack([low[:, 1], low[:, 2], pp], axis=1)


def cost(p, g, limb, verbose=False):
    tr = trajectory(g, limb, p)
    y, z, pp = tr[:, 0], tr[:, 1], tr[:, 2]
    lift = GROUND - y                                                  # >0 above ground
    n_st = int(round(g.duty * N))
    stance = np.argsort(lift)[:n_st]                                    # the duty-fraction closest to the floor
    dz = (np.roll(z, -1) - np.roll(z, 1)) / (2 * (2 * math.pi / N))     # dz/dtheta
    pen = np.sum(np.clip(-lift, 0, None) ** 2) * 200                    # never through the floor
    hover = np.sum(lift[stance] ** 2) * 6                               # planted = on the floor
    slide = np.sum((dz[stance] - g.K) ** 2) * 0.6                       # moving back at body speed
    swing = np.setdiff1d(np.arange(N), stance)
    clear = max(0.0, g.clearance - lift[swing].max()) ** 2 * 40 if len(swing) else 0.0
    back = np.sum(np.clip(dz[swing], 0, None) ** 2) * 0.05             # swing foot travels forward
    flat = np.sum(pp[stance] ** 2) * 2                                  # paw flat while planted
    reg = np.sum(p[7:11] ** 2) * 0.5 + np.sum(p[0:4] ** 2) * 0.1
    total = pen + hover + slide + clear + back + flat + reg
    if verbose:
        return dict(total=total, dig=float(max(0, -lift.min())), hover=float(np.abs(lift[stance]).max()),
                    slide=float(np.abs(dz[stance] - g.K).mean()), clear=float(lift[swing].max()) if len(swing) else 0,
                    paw_tilt=float(np.degrees(np.abs(pp[stance]).max())))
    return total


def fit(g, limb, starts=10, seed=1):
    rng = np.random.default_rng(seed)
    best = None
    for i in range(starts):
        x0 = np.concatenate([rng.uniform(0.15, 0.6, 4), rng.uniform(-1.5, 1.5, 3), rng.uniform(-0.2, 0.2, 4)])
        r = minimize(cost, x0, args=(g, limb), method="Powell", options={"maxiter": 6000, "xtol": 1e-4, "ftol": 1e-6})
        if best is None or r.fun < best.fun:
            best = r
    return best.x


# ---------------------------------------------------------------- the inherited cycles, for comparison
def inherited(gname, limb):
    """Metrics of the shipped calls (hyena-derived), evaluated with the same cost at K for the CURRENT cadence."""
    # (speed factor relative to limbSwing_raw, per-segment (degree, invert, offset, weight)), amt = 0.6
    if gname == "walk":
        c, table = 0.5 * 0.5 * 2.8, {
            "arm_right": [(0.6, False, 2.0, 0.0), (0.6 * 1.2, False, 0.0, -0.8 * 1.2), (0.35, False, 5.4, 0.0), None],
            "arm_left": [(0.6, False, 5.0, 0.0), (0.6 * 1.2, False, 3.0, -0.8 * 1.2), (0.35, False, 2.2, 0.0), None],
            "leg_right": [(0.8, True, 1.0, 0.0), (0.4, True, 1.2, 0.2), (0.3, True, 1.4, 0.1), None],
            "leg_left": [(0.8, True, 3.4, 0.0), (0.4, True, 3.6, 0.2), (0.3, True, 3.8, 0.1), None]}
    else:
        c, table = 0.5 * 0.3 * 2.8, {
            "arm_right": [(1.0, True, 0.0, 0.0), (0.6, True, 0.2, 0.2), (0.4, True, 0.4, 0.2), None],
            "arm_left": [(1.0, True, 0.6, 0.0), (0.6, True, 0.8, 0.2), (0.4, True, 1.0, 0.2), None],
            "leg_right": [(1.0, True, 1.4, 0.0), (0.6, True, 1.6, 0.2), (0.4, True, 1.8, 0.2), None],
            "leg_left": [(1.0, True, 2.0, 0.0), (0.6, True, 2.2, 0.2), (0.4, True, 2.4, 0.2), None]}
    amt = 0.6
    out = []
    for th in TH:
        dx = {}
        for k, s in enumerate(SEG):
            t = table[limb][k]
            if t is None:
                continue
            deg, inv, off, wt = t
            v = math.cos(th + off) * deg * amt + wt * amt
            dx[limb + "_" + s] = -v if inv else v
        pos, ppitch = sole(limb, dx, {})
        out.append((pos[1], pos[2]))
    tr = np.array(out)
    lift = GROUND - tr[:, 0]
    # stride per radian of THIS phase variable, in model units at mob size 0.9
    return dict(dig=float(max(0, -lift.min())), clear=float(lift.max()), c=c)


def report():
    for gname in ("walk", "run"):
        for limb in LIMBS:
            print("inherited %-4s %-9s" % (gname, limb), {k: round(v, 2) for k, v in inherited(gname, limb).items()})


if __name__ == "__main__" and "--java" not in sys.argv and "--fit2" not in sys.argv:
    rest = {l: GROUND - sole(l, {}, {})[0][1] for l in LIMBS}
    print("rest-pose sole clearance:", {k: round(v, 3) for k, v in rest.items()})
    if "--check" in sys.argv:
        report()
        sys.exit()
    results = {}
    for g in (WALK, RUN):
        for cls, limb in (("fore", "arm_left"), ("hind", "leg_left")):
            x = fit(g, limb)
            results[(g.name, cls)] = x
            print("%-4s %-4s" % (g.name, cls), {k: round(v, 3) for k, v in cost(x, g, limb, verbose=True).items()})
            print("      A=%s D=%s W=%s" % tuple(np.round(a, 4).tolist() for a in unpack(x)))
            for other in [l for l, c in LIMBS.items() if c == cls and l != limb]:
                print("      check %-9s" % other, {k: round(v, 3) for k, v in cost(x, g, other, verbose=True).items()})
    np.save(pathlib.Path(__file__).resolve().parent.parent / "scratch/wolf_gait_fit.npy", results, allow_pickle=True)


# ================================================================= v2: two harmonics, explicit target path
# Each joint: A1*cos(th - ph + D1) + A2*cos(2*(th - ph) + D2) + W  -- in Java that is two walk() calls
# (the second at twice the cadence), which Citadel simply adds. One cosine per joint cannot hold a paw
# flat on the floor for 60% of the cycle and still lift it; the second harmonic can.
from scipy.optimize import least_squares

REST_PITCH = {}


def trajectory2(g, limb, q):
    A1, D1, A2, D2, W = q[0:4], q[4:8], q[8:12], q[12:16], q[16:20]
    ph = 2 * math.pi * g.phases[limb]
    # body SETTLES while moving (rotationPointY += drop, Y down): a straight rest-pose leg cannot reach the
    # floor at the ends of its stride, which lie farther from the hip than straight down
    dy = g.drop + g.bob_deg * np.sin(TH * g.bob_mult + g.bob_off)
    pitch = math.radians(g.pitch_deg) * np.cos(TH + g.pitch_off)
    M = None
    for n in chain(limb + "_paw"):
        x, y, z = RP[n]
        rx = (ROT.get(n) or (0.0, 0.0, 0.0))[0]
        ang = np.full(N, rx)
        rp = np.tile([x, y, z], (N, 1)).astype(float)
        if n == "body_main":
            rp[:, 1] += dy
            ang = ang + pitch
        seg = n[len(limb) + 1:] if n.startswith(limb + "_") else None
        if seg in SEG:
            k = SEG.index(seg)
            t = TH - ph
            ang = ang + A1[k] * np.cos(t + D1[k]) + A2[k] * np.cos(2 * t + D2[k]) + W[k]
        T = batch_T(rp, ang)
        M = T if M is None else M @ T
    bx, by, bz, w, h, d = BOX[limb + "_paw"]
    corners = np.array([[x, by + h, z, 1.0] for x in (bx, bx + w) for z in (bz, bz + d)])
    P = np.einsum("nij,kj->nki", M, corners)
    # height from the LOWEST corner (catches a toe or heel digging in); fore-aft from the sole CENTRE,
    # because the lowest corner hops heel<->toe as the paw rocks and fakes a 3-unit slide
    y = P[:, :, 1].max(axis=1)
    z = P[:, :, 2].mean(axis=1)
    fwd = M[:, :3, :3] @ np.array([0, 0, -1.0])
    return y, z, np.arctan2(fwd[:, 1], -fwd[:, 2])


def target(g, z0):
    """Planted for the first `duty` of the leg's own cycle (sliding back at K), then a lifted arc forward."""
    t = (TH / (2 * math.pi)) % 1.0
    S = g.K * 2 * math.pi * g.duty
    y = np.full(N, GROUND)
    z = np.empty(N)
    st = t < g.duty
    z[st] = z0 - S / 2 + S * (t[st] / g.duty)
    u = (t[~st] - g.duty) / (1 - g.duty)                     # 0..1 through swing
    z[~st] = z0 + S / 2 - S * (0.5 - 0.5 * np.cos(math.pi * u))
    y[~st] = GROUND - g.clearance * np.sin(math.pi * u)
    return y, z, st


def residuals(x, g, limb):
    q, z0 = x[:20], x[20]
    y, z, pp = trajectory2(g, limb, q)
    # the target is in the LEG's phase: shift by the leg's own phase
    k = int(round(g.phases[limb] * N)) % N
    ty, tz, st = (np.roll(a, k) for a in target(g, z0))
    r = [np.where(st, 3.0, 2.0) * (y - ty),
         np.where(st, 1.5, 0.6) * (z - tz),
         12.0 * np.clip(y - GROUND, 0, None),
         np.where(st, 4.0, 0.0) * (pp - REST_PITCH[limb]),
         0.08 * q[0:16], 0.25 * q[16:20]]
    return np.concatenate(r)


def metrics2(g, limb, x):
    y, z, pp = trajectory2(g, limb, x[:20])
    k = int(round(g.phases[limb] * N)) % N
    st = np.roll(target(g, x[20])[2], k)
    lift = GROUND - y
    dz = (np.roll(z, -1) - np.roll(z, 1)) / (2 * (2 * math.pi / N))
    return dict(dig=round(float(max(0, -lift.min())), 3), hover=round(float(np.abs(lift[st]).max()), 3),
                slide=round(float(np.abs(dz[st] - g.K).mean() / g.K), 3), clear=round(float(lift[~st].max()), 2),
                tilt=round(float(np.degrees(np.abs(pp[st] - REST_PITCH[limb]).max())), 1),
                maxang=round(float(np.degrees(np.abs(x[:4]).max() + np.abs(x[8:12]).max())), 1))


# Anatomical excursion limits (radians) per joint: upper = shoulder/hip, lower = elbow/stifle,
# foot = carpus/hock (flexes most in swing), paw = digits. Without them the solver "cheats" with
# 100-degree paw flips. A1 = main swing, A2 = second-harmonic shaping, W = mean offset.
LIMITS = {"walk": dict(A1=[0.35, 0.40, 0.85, 0.30], A2=[0.12, 0.15, 0.30, 0.15], W=0.30),
          "run": dict(A1=[0.60, 0.60, 0.80, 0.40], A2=[0.20, 0.25, 0.30, 0.20], W=0.35)}


def bounds(g):
    L = LIMITS[g.name]
    hi = np.concatenate([L["A1"], np.full(4, np.inf), L["A2"], np.full(4, np.inf), np.full(4, L["W"]), [np.inf]])
    return -hi, hi


def fit2(g, limb, starts=12, seed=3):
    rng = np.random.default_rng(seed)
    z_rest = sole(limb, {}, {})[0][2]
    best = None
    for i in range(starts):
        x0 = np.concatenate([rng.uniform(0.1, 0.5, 4), rng.uniform(-3, 3, 4), rng.uniform(0, 0.15, 4),
                             rng.uniform(-3, 3, 4), rng.uniform(-0.1, 0.1, 4), [z_rest]])
        LO, HI = bounds(g)
        x0 = np.clip(x0, LO + 1e-6, HI - 1e-6)
        r = least_squares(residuals, x0, args=(g, limb), method="trf", bounds=(LO, HI), max_nfev=4000)
        if best is None or r.cost < best.cost:
            best = r
    return best.x


def main2():
    for l in LIMBS:
        REST_PITCH[l] = sole(l, {}, {})[1]
    out = {}
    for g in (WALK, RUN):
        for limb in LIMBS:
            x = fit2(g, limb)
            out[(g.name, limb)] = x
            print("%-4s %-9s" % (g.name, limb), metrics2(g, limb, x))
    np.save(pathlib.Path(__file__).resolve().parent.parent / "scratch/wolf_gait_fit2.npy", out, allow_pickle=True)
    return out


# ================================================================= Java emission
RUN_SPEED = 0.355          # blocks/tick at the hunt (attack goal x1.6, attribute 0.22): the clamped regime


def cadence(g):
    """Phase speed c in walk(part, c, ...) with f = limbSwing_raw / renderScale.
    Unclamped (walk): K = 4 / c. Clamped (run, limbSwing advances 1/tick): K = 16 v / c."""
    return 4.0 / g.K if g.name == "walk" else 16.0 * RUN_SPEED / g.K


def emit_java(fits):
    lines = []
    for g in (WALK, RUN):
        c = cadence(g)
        lines.append("            " + ("} else {" if g is RUN else "if (!running) {"))
        lines.append("                // %s: fitted by scripts/wolf_gait.py (K=%.2f units/rad, duty %.2f)" % (g.name, g.K, g.duty))
        lines.append("                this.body_main.rotationPointY += (%.3fF + %.3fF * Mth.sin(gait * %.4fF + %.4fF)) * stride;"
                     % (g.drop, g.bob_deg, c * g.bob_mult, g.bob_off))
        if g.pitch_deg:
            lines.append("                walk(body_main, %.4fF, %.4fF, false, %.4fF, 0F, gait, stride);" % (c, math.radians(g.pitch_deg), g.pitch_off))
        for limb in ("arm_left", "arm_right", "leg_left", "leg_right"):
            x = fits[(g.name, limb)]
            A1, D1, A2, D2, W = x[0:4], x[4:8], x[8:12], x[12:16], x[16:20]
            ph = 2 * math.pi * g.phases[limb]
            for k, s in enumerate(SEG):
                part = "%s_%s" % (limb, s)
                lines.append("                walk(%s, %.4fF, %.4fF, false, %.4fF, %.4fF, gait, stride);"
                             % (part, c, A1[k], (D1[k] - ph) % (2 * math.pi), W[k]))
                lines.append("                walk(%s, %.4fF, %.4fF, false, %.4fF, 0F, gait, stride);"
                             % (part, 2 * c, A2[k], (D2[k] - 2 * ph) % (2 * math.pi)))
    lines.append("            }")
    return "\n".join(lines)


if __name__ == "__main__" and "--java" in sys.argv:
    fits = np.load(pathlib.Path(__file__).resolve().parent.parent / "scratch/wolf_gait_fit2.npy", allow_pickle=True).item()
    print(emit_java(fits))


if __name__ == "__main__" and "--fit2" in sys.argv:
    main2()
