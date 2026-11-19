#!/usr/bin/env python3
"""Write the SOLVED EAT / DRINK / ALERT poses (scratch/poses/<type>.json, scripts/pose_reach.py) into the shipped models
and declare the animations on the entities (2.8.0, P3c). Idempotent: rerun after re-solving.

  model:  a `// <gen:idle-poses>` ... `// </gen:idle-poses>` region right after `animator.update(...)` in animate()
  entity: EAT (40t, only if the type has none) / DRINK (100t) / ALERT (40t) statics, APPENDED to getAnimations()
          (Citadel syncs animations by index, so existing indices never move), the getAnimationEat/Drink/Alert hooks,
          and getDrinkReach() from the solved DRINK muzzle position.

Only poses a type actually uses are written: EAT if it grazes (GrazeGoal) and has no eat animation of its own, DRINK if
it registers DrinkGoal, ALERT if it registers AlertGoal.

    python scripts/gen_keyframes.py --all          python scripts/gen_keyframes.py deer equid
"""
import json
import pathlib
import re
import sys

import numpy as np

from fk_rig import Rig, pose_over_move
from pose_targets import T

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java/untamedwilds"
POSES = ROOT / "scratch" / "poses"
BEGIN, END = "// <gen:idle-poses>", "// </gen:idle-poses>"


def entity_file(name):
    hits = list((SRC / "entity").rglob(name + ".java"))
    if len(hits) != 1:
        raise SystemExit(f"cannot find {name}.java")
    return hits[0]


def java_rot(d):
    """BB degree deltas -> Java degree arguments for AdvancedEntityModel.rotate (x and y flip)."""
    dx, dy, dz = d
    return (-dx, -dy, dz)


def fmt(v):
    v = 0.0 if abs(v) < 0.05 else v
    return f"{v:.1f}F"


def nod_index(chain):
    """The joint a cropping nod / swallow moves: the head itself, not a tiny nose box at the end of the chain."""
    for i, n in enumerate(chain):
        if n == "head" or n in ("head_main", "head_skull", "head_face"):
            return i
    return max(0, len(chain) - 2) if len(chain) > 2 else len(chain) - 1


def pose_lines(res, pose, extra_last=0.0, ears=True):
    r = res[pose]
    out = []
    chain, deltas = r["chain"], list(r["deltas_bb"])
    if extra_last:
        deltas[nod_index(chain)] += extra_last
    for part, d in zip(chain, deltas):
        if abs(d) >= 0.05:
            x, y, z = java_rot((d, 0.0, 0.0))
            out.append(f"this.rotate(animator, {part}, {fmt(x)}, {fmt(y)}, {fmt(z)});")
    mv = r.get("neck_move_bb")
    if mv and (abs(mv[1]) > 0.05 or abs(mv[2]) > 0.05):
        out.append(f"animator.move({chain[0]}, {fmt(-mv[0])}, {fmt(-mv[1])}, {fmt(mv[2])});")
    # the body under a splayed drink: every keyframe is an absolute pose, so the body has to be in each of them
    for part, d in (r.get("body_rot_bb") or {}).items():
        if any(abs(v) >= 0.05 for v in d):
            x, y, z = java_rot(d)
            out.append(f"this.rotate(animator, {part}, {fmt(x)}, {fmt(y)}, {fmt(z)});")
    for part, d in (r.get("body_move_bb") or {}).items():
        if any(abs(v) > 0.05 for v in d):
            out.append(f"animator.move({part}, {fmt(-d[0])}, {fmt(-d[1])}, {fmt(d[2])});")
    if ears:
        for ear, d in r.get("ears", {}).items():
            d = d if isinstance(d, (list, tuple)) else (d, 0.0, 0.0)
            x, y, z = java_rot(d)
            out.append(f"this.rotate(animator, {ear}, {fmt(x)}, {fmt(y)}, {fmt(z)});")
    return out


def keyframe(ind, dur, lines):
    s = [f"{ind}animator.startKeyframe({dur});"]
    s += [ind + ln for ln in lines]
    s.append(f"{ind}animator.endKeyframe();")
    return s


def model_region(typ, res, ent, use, ind):
    L = [f"{ind}{BEGIN} 2.8.0: SOLVED by scripts/pose_reach.py against this model's own boxes (muzzle to the grass / the",
         f"{ind}// water line beyond the bank, every head part and horn family clear of the ground, nothing through the torso).",
         f"{ind}// Regenerate, never hand-edit: python scripts/gen_keyframes.py {typ}"]
    if "eat" in use:
        L.append(f"{ind}// EAT (40t, GrazeGoal's mouthful): head down to the grass, a cropping lift, back down")
        L.append(f"{ind}animator.setAnimation({ent}.EAT);")
        L += keyframe(ind, 10, pose_lines(res, "eat", ears=False))
        L += keyframe(ind, 5, pose_lines(res, "eat", extra_last=6.0, ears=False))
        L += keyframe(ind, 5, pose_lines(res, "eat", ears=False))
        L.append(f"{ind}animator.setStaticKeyframe(10);")
        L.append(f"{ind}animator.resetKeyframe(10);")
    if "drink" in use and T[typ].get("drink_style") == "trunk":
        curl = [f"this.rotate(animator, {part}, {fmt(v)}, 0.0F, 0.0F);" for part, v in T[typ]["trunk_curl_java"].items()]
        reach = pose_lines(res, "drink", ears=False)
        L.append(f"{ind}// DRINK (100t, DrinkGoal), the way elephants drink: the trunk tip down into the water beyond the bank (SOLVED,")
        L.append(f"{ind}// one smooth curve), a hold while it draws water, then the trunk coiled up to the mouth (the approved EAT")
        L.append(f"{ind}// coil, pose_targets trunk_curl_java), a hold while it pours -- twice")
        L.append(f"{ind}animator.setAnimation({ent}.DRINK);")
        L += keyframe(ind, 15, reach)
        L.append(f"{ind}animator.setStaticKeyframe(15);")
        L += keyframe(ind, 12, curl)
        L.append(f"{ind}animator.setStaticKeyframe(10);")
        L += keyframe(ind, 12, reach)
        L.append(f"{ind}animator.setStaticKeyframe(15);")
        L += keyframe(ind, 12, curl)
        L.append(f"{ind}animator.resetKeyframe(9);")
    elif "drink" in use:
        L.append(f"{ind}// DRINK (100t, DrinkGoal): muzzle to the water past the bank edge, two swallows lifting the head")
        L.append(f"{ind}animator.setAnimation({ent}.DRINK);")
        L += keyframe(ind, 15, pose_lines(res, "drink", ears=False))
        L.append(f"{ind}animator.setStaticKeyframe(20);")
        L += keyframe(ind, 5, pose_lines(res, "drink", extra_last=5.0, ears=False))
        L += keyframe(ind, 5, pose_lines(res, "drink", ears=False))
        L.append(f"{ind}animator.setStaticKeyframe(20);")
        L += keyframe(ind, 5, pose_lines(res, "drink", extra_last=5.0, ears=False))
        L += keyframe(ind, 5, pose_lines(res, "drink", ears=False))
        L.append(f"{ind}animator.setStaticKeyframe(10);")
        L.append(f"{ind}animator.resetKeyframe(15);")
    if "alert" in use:
        L.append(f"{ind}// ALERT (40t, AlertGoal): head raised, ears up, staring at the threat; repeats while the alert lasts")
        L.append(f"{ind}animator.setAnimation({ent}.ALERT);")
        L += keyframe(ind, 8, pose_lines(res, "alert"))
        L.append(f"{ind}animator.setStaticKeyframe(24);")
        L.append(f"{ind}animator.resetKeyframe(8);")
    L.append(f"{ind}{END}")
    return "\n".join(L)


def drink_reach_units(typ, res):
    """How far in front of the model origin (entity centre) the muzzle reaches in the DRINK pose, BB units."""
    r = res["drink"]
    rig = Rig(T[typ]["model"])
    over, move = pose_over_move(rig, r, ears=False)
    zs = [rig.world(n, rig.vol(n, 3), over, move)[:, 2].min() for n in T[typ]["muzzle"] if n in rig.P]
    return max(0.0, -min(zs))


def patch_model(typ, res, ent, use):
    path = SRC / "client/model" / (T[typ]["model"] + ".java")
    s = path.read_text(encoding="utf-8")
    head = s[:s.index("public " + T[typ]["model"] + "()")]
    extra = list(T[typ].get("trunk_curl_java", {}).keys()) if "drink" in use else []
    for pose in use:
        for part in (res[pose]["chain"] + list(res[pose].get("ears", {}).keys()) + list((res[pose].get("body_rot_bb") or {}).keys())
                     + list((res[pose].get("body_move_bb") or {}).keys()) + (extra if pose == "drink" else [])):
            if not re.search(r"\b" + re.escape(part) + r"\b", head):
                raise SystemExit(f"{typ}: {part} is not a field of {T[typ]['model']}")
    # drop any previous region
    s = re.sub(r"\n[ \t]*" + re.escape(BEGIN) + r".*?" + re.escape(END), "", s, flags=re.S)
    m = re.search(r"void animate\(IAnimatedEntity \w+\)\s*\{[^\n]*\n(?:[^\n]*\n)*?(?P<ind>[ \t]*)animator\.update\([^)]*\);[^\n]*\n", s)
    if not m:
        raise SystemExit(f"{typ}: no animator.update() in animate()")
    region = model_region(typ, res, ent, use, m.group("ind"))
    s = s[:m.end()] + region + "\n" + s[m.end():]
    path.write_text(s, encoding="utf-8")


def patch_entity(path, use, reach_blocks_per_size):
    s = path.read_text(encoding="utf-8")
    cls = path.stem
    wanted = []
    if "eat" in use:
        wanted.append(("EAT", 40))
    if "drink" in use:
        wanted.append(("DRINK", 100))
    if "alert" in use:
        wanted.append(("ALERT", 40))
    # statics
    decl = list(re.finditer(r"^[ \t]*public static final Animation \w+ = Animation\.create\(\d+\);[^\n]*\n", s, re.M))
    add = [f"    public static final Animation {n} = Animation.create({t});   // 2.8.0 idle pose (gen_keyframes.py)\n"
           for n, t in wanted if not re.search(rf"\bAnimation {n}\b\s*=", s)]
    if add:
        at = decl[-1].end() if decl else s.index("{", s.index("public class " + cls)) + 2
        s = s[:at] + "".join(add) + s[at:]
    # getAnimations: append (indices never move)
    m = re.search(r"(public Animation\[\] getAnimations\(\)\s*\{\s*return new Animation\[\]\s*\{)(?P<body>.*?)(\}\s*;)", s, re.S)
    if not m:
        raise SystemExit(f"{cls}: getAnimations() not in the expected form")
    present = {x.strip() for x in m.group("body").split(",")}
    extra = [n for n, _ in wanted if n not in present]
    if extra:
        body = m.group("body").rstrip()
        s = s[:m.start("body")] + body + ", " + ", ".join(extra) + s[m.end("body"):]
    # hooks
    if "eat" in use:
        s, _ = re.subn(r"public Animation getAnimationEat\(\) \{ return NO_ANIMATION; \}", "public Animation getAnimationEat() { return EAT; }", s)
    hooks = []
    if "drink" in use and "getAnimationDrink()" not in s:
        hooks.append("    @Override\n    public Animation getAnimationDrink() { return DRINK; }\n")
    if "alert" in use and "getAnimationAlert()" not in s:
        hooks.append("    @Override\n    public Animation getAnimationAlert() { return ALERT; }\n")
    if reach_blocks_per_size is not None:
        s = re.sub(r"\n    // 2\.8\.0 solved DRINK reach.*?\n    public double getDrinkReach\(\) \{[^\n]*\}\n", "\n", s, flags=re.S)
        hooks.append(f"    // 2.8.0 solved DRINK reach (gen_keyframes.py): muzzle {reach_blocks_per_size * 16:.1f} units in front of the centre at size 1\n"
                     f"    @Override\n    public double getDrinkReach() {{ return {reach_blocks_per_size:.3f}D * this.getMobSize(); }}\n")
    if hooks:
        # right before getAnimations() (and its @Override, if it has one on the line above)
        anchor = re.search(r"^(?:[ \t]*@Override[ \t]*\n)?[ \t]*public Animation\[\] getAnimations\(\)", s, re.M)
        s = s[:anchor.start()] + "".join(hooks) + "\n" + s[anchor.start():]
    path.write_text(s, encoding="utf-8")
    return extra


def used_poses(typ, epath, res):
    src = epath.read_text(encoding="utf-8")
    cfg = T[typ]
    use = []
    if "new GrazeGoal(" in src and cfg.get("eat", True) is not False and "eat" in res and "deltas_bb" in res["eat"]:
        use.append("eat")
    if "new DrinkGoal(" in src and "drink" in res and "deltas_bb" in res["drink"]:
        use.append("drink")
    if "new AlertGoal(" in src and "alert" in res and "deltas_bb" in res["alert"]:
        use.append("alert")
    return use


def run(typ):
    res_path = POSES / f"{typ}.json"
    if not res_path.exists():
        print(f"  {typ}: no solved poses, skipped")
        return
    res = json.loads(res_path.read_text(encoding="utf-8"))
    ent = T[typ]["entity"]
    epath = entity_file(ent)
    use = used_poses(typ, epath, res)
    if not use:
        print(f"  {typ}: no pose is used by its goals, skipped")
        return
    reach = drink_reach_units(typ, res) / 16.0 if "drink" in use else None
    patch_model(typ, res, ent, use)
    extra = patch_entity(epath, use, reach)
    print(f"  {typ}: {'+'.join(use)} written" + (f", reach {reach * 16:.1f}u" if reach else "") + (f", appended {extra}" if extra else ""))


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if "--all" in sys.argv:
        args = list(T)
    for t in args:
        run(t)
