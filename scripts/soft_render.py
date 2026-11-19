#!/usr/bin/env python3
"""Pose sheets without Blockbench (2.8.0, P3b): rest / EAT / DRINK / ALERT for a model, side + 3/4 views.

Flat-shaded boxes (painter's algorithm, back faces culled), each part in the average colour of ITS OWN box-UV
footprint in a real skin, so the animal reads as itself. Ground line at y=0; a water band below the bank for DRINK.
Parts whose footprint is fully transparent in that skin (deleted via alpha-0, e.g. a lioness mane) are not drawn;
parts named in pose_targets.T[type]["hide"] (non-default horn / antler families) neither.

    python scripts/soft_render.py deer            # -> scratch/poses/sheet_deer.png
    python scripts/soft_render.py --all
"""
import json
import math
import pathlib
import sys

import numpy as np
from PIL import Image, ImageDraw

from fk_rig import Rig, pose_over_move
from pose_targets import T

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/untamedwilds/textures/entity"
POSES = ROOT / "scratch" / "poses"
QUADS = [(0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1), (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3)]
LIGHT = np.array([0.35, 0.85, -0.4]) / np.linalg.norm([0.35, 0.85, -0.4])


def skin_for(typ):
    folder = TEX / typ
    pngs = sorted(p for p in folder.glob("*.png") if "saddle" not in p.name and "_pup" not in p.name)
    pref = [p for p in pngs if not p.stem.endswith(("_female", "_1r", "_2r"))]
    return (pref or pngs or [None])[0]


def part_colours(rig, skin_path):
    cols = {}
    if skin_path is None:
        return cols
    img = np.asarray(Image.open(skin_path).convert("RGBA")).astype(float)
    H, W = img.shape[:2]
    for n, p in rig.P.items():
        w = abs(p["t"][0] - p["f"][0]); h = abs(p["t"][1] - p["f"][1]); d = abs(p["t"][2] - p["f"][2])
        u, v = p["uv"]
        u1 = int(min(W, u + math.ceil(2 * (w + d)))); v1 = int(min(H, v + math.ceil(h + d)))
        reg = img[int(v):max(int(v) + 1, v1), int(u):max(int(u) + 1, u1)].reshape(-1, 4)
        reg = reg[reg[:, 3] > 0]
        cols[n] = None if len(reg) == 0 else reg[:, :3].mean(0)
    return cols


def project(pts, yaw, pitch):
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rx = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    return (Rx @ Ry @ np.asarray(pts, float).T).T


def draw_view(img, rig, over, cols, hide, yaw, pitch, scale, origin, water_z=None, label="", move=None):
    dr = ImageDraw.Draw(img, "RGBA")
    ox, oy = origin
    # ground (y=0) as a projected slab outline
    g = np.array([[x, 0, z] for x in (-30, 30) for z in (-60, 40)], float)
    gp = project(g, yaw, pitch)
    poly = [(ox + gp[i, 0] * scale, oy - gp[i, 1] * scale) for i in (0, 1, 3, 2)]
    dr.polygon(poly, fill=(120, 150, 90, 70), outline=(70, 90, 50, 160))
    if water_z is not None:
        wpts = np.array([[x, -2.0, z] for x in (-30, 30) for z in (water_z - 40, water_z)], float)
        wp = project(wpts, yaw, pitch)
        dr.polygon([(ox + wp[i, 0] * scale, oy - wp[i, 1] * scale) for i in (0, 1, 3, 2)], fill=(60, 110, 200, 110))
    faces = []
    for n in rig.order:
        if n in hide or cols.get(n, 0) is None:
            continue
        c = rig.world(n, rig.corners(n), over, move)
        cp_ = project(c, yaw, pitch)
        base = cols.get(n)
        base = np.array([150, 130, 110]) if base is None or isinstance(base, int) else base
        for q in QUADS:
            w = c[list(q)]
            nrm = np.cross(w[1] - w[0], w[2] - w[0])
            ln = np.linalg.norm(nrm)
            if ln < 1e-9:
                continue
            nrm /= ln
            pq = cp_[list(q)]
            sn = np.cross(pq[1] - pq[0], pq[2] - pq[0])
            if sn[2] > 0:          # facing away from the camera (camera looks along -Z of view space)
                continue
            shade = 0.45 + 0.55 * max(0.0, float(nrm @ LIGHT))
            col = tuple(int(min(255, x * shade)) for x in base) + (255,)
            faces.append((pq[:, 2].mean(), [(ox + x * scale, oy - y * scale) for x, y, _ in pq], col))
    faces.sort(key=lambda f: f[0], reverse=True)
    for _, poly, col in faces:
        dr.polygon(poly, fill=col, outline=(0, 0, 0, 90))
    if label:
        dr.text((ox - 150, oy + 18), label, fill=(20, 20, 20, 255))


def pose_move(res, pose, rig=None):
    r = res.get(pose) or {}
    if "deltas_bb" not in r:
        return None
    if rig is None:
        rig = Rig(T[res["type"]]["model"])
    return pose_over_move(rig, r)[1]


def pose_over(rig, res, pose):
    r = res.get(pose)
    if not r or "deltas_bb" not in r:
        return None
    return pose_over_move(rig, r)[0]


def sheet(typ):
    cfg = T[typ]
    rig = Rig(cfg["model"])
    res_path = POSES / f"{typ}.json"
    res = json.loads(res_path.read_text(encoding="utf-8")) if res_path.exists() else {}
    skin = skin_for(typ)
    cols = part_colours(rig, skin)
    hide = set(cfg.get("hide", []))
    rows = [("rest", {})]
    for pose in ("eat", "drink", "alert"):
        o = pose_over(rig, res, pose)
        if o is not None:
            rows.append((pose, o))
    # one scale for the whole sheet: fit the rest pose's extent
    allc = np.vstack([rig.world(n, rig.corners(n)) for n in rig.order])
    ext = max(allc[:, 2].max() - allc[:, 2].min(), allc[:, 1].max() + 4, 1.0)
    cell_w, cell_h = 440, 300
    scale = min(cell_w * 0.8 / ext, cell_h * 0.75 / (allc[:, 1].max() + 6))
    img = Image.new("RGBA", (cell_w * 2, cell_h * len(rows) + 24), (245, 242, 235, 255))
    ImageDraw.Draw(img).text((8, 4), f"{typ}  ({cfg['model']}, skin {skin.name if skin else '-'})  -- left: side view, right: 3/4 view",
                             fill=(0, 0, 0, 255))
    front_z = None
    if "drink" in res and "deltas_bb" in res.get("drink", {}):
        fore = cfg.get("fore") or [n for n in rig.order if any(w in n for w in ("fore", "arm", "hand"))]
        if fore:
            # the bank edge is in front of the POSED front feet (a splayed giraffe's hooves move)
            o, m = pose_over_move(rig, res["drink"])
            front_z = min(rig.world(n, rig.corners(n), o, m)[:, 2].min() for n in fore) - 1.0
    for i, (pose, over) in enumerate(rows):
        oy = 24 + cell_h * i + cell_h * 0.78
        info = ""
        mv = pose_move(res, pose, rig) if pose != "rest" else None
        if pose != "rest":
            r = res[pose]
            info = f"{pose.upper()}  " + ", ".join(f"{j} {d:+.0f}" for j, d in zip(r["chain"], r["deltas_bb"]) if abs(d) >= 0.5)
            if r.get("neck_move_bb"):
                info += f", neck base drop {-r['neck_move_bb'][1]:.1f} fwd {-r['neck_move_bb'][2]:.1f}"
            if r.get("body_note"):
                info += f", {r['body_note']}"
            info += f"   muzzle {r['muzzle_low']:+.2f}" + ("" if r.get("feasible") else "  [CONSTRAINTS NOT MET]")
        else:
            info = "REST"
        wz = front_z if pose == "drink" else None
        draw_view(img, rig, over, cols, hide, 90, 0, scale, (cell_w * 0.5, oy), wz, info, mv)
        draw_view(img, rig, over, cols, hide, 130, 18, scale, (cell_w * 1.5, oy), wz, "", mv)
    out = POSES / f"sheet_{typ}.png"
    img.convert("RGB").save(out)
    print("wrote", out)
    return out


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if "--all" in sys.argv:
        args = [t for t in T if (POSES / f"{t}.json").exists()]
    for t in args:
        sheet(t)
