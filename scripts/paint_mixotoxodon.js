// Mixotoxodon larensis -- the larger northern species, savanna/plains.
//
// Designed AGAINST toxodon.png on six diagnostic cues, deliberately inverted, so the two
// read as different animals rather than a hue shift:
//    hue       warm grey-brown      ->  cool desaturated slate
//    value     mid                  ->  dark
//    muzzle    PALE worn            ->  DARK, with a pale mealy ring only at the lip
//    grime     dark ochre river silt, low  ->  PALE savanna dust, carried high
//    hide      even, sparsely bristled     ->  deep folds + bare worn patches
//    dorsum    plain                ->  sun-bleached along the topline
// The grime inversion is the big one: toxodon is a swamp animal booted in wet dark mud,
// this one is a dry-country grazer powdered pale to the belly.
(function () {
  var C = {
    dorsal : MM.hex('#3a3e43'),
    flank  : MM.hex('#535960'),
    ventral: MM.hex('#767b83'),
    bleach : MM.hex('#7d7a70'),   // sun-worn topline
    muzzle : MM.hex('#2c2f34'),
    lip    : MM.hex('#8d8478'),   // pale mealy ring at the mouth only
    jaw    : MM.hex('#33373c'),
    ivory  : MM.hex('#cabfa4'),
    horn   : MM.hex('#212325'),
    dust   : MM.hex('#9c9382'),
    bare   : MM.hex('#5a544c'),   // rubbed-bare hide
    earin  : MM.hex('#4e4a48'),
    pupil  : MM.hex('#0d0f11'),
    glint  : MM.hex('#6f7278')
  };

  MM.bounds();
  var bb = MM.map['body_barrel'];
  var yb0 = bb.lo.y, yb1 = bb.hi.y;

  var BODYCLS = { body_barrel:1, body_withers:1, body_chest:1, body_croup:1, neck:1,
                  throat:1, head_skull:1, head_brow:1,
                  fore_left_shoulder:1, fore_right_shoulder:1,
                  hind_left_thigh:1, hind_right_thigh:1 };
  var DISTAL  = { fore_left_forearm:1, fore_right_forearm:1, fore_left_cannon:1,
                  fore_right_cannon:1, hind_left_gaskin:1, hind_right_gaskin:1,
                  hind_left_cannon:1, hind_right_cannon:1 };
  var FOOT    = { fore_left_foot:1, fore_right_foot:1, hind_left_foot:1, hind_right_foot:1 };

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    // ---------- eyes ----------
    if (o.part === 'eye_left' || o.part === 'eye_right') {
      var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
      if (!outward) return [0, 0, 0, 0];              // inward quad deleted, not painted
      var hide = MM.mix(C.flank, C.dorsal, 0.6);
      if (o.j === 0) return MM.cl(hide);
      if (o.j === 2) return MM.cl(MM.mul(hide, 0.85));
      return (o.i === 2) ? MM.cl(C.glint) : MM.cl(C.pupil);
    }

    // ---------- incisors ----------
    if (o.part === 'incisor_lower' || o.part === 'incisor_upper') {
      var wear = MM.dith(o.u, o.v, 5) * 0.16;
      col = MM.mix(C.ivory, C.bare, wear + 0.18 * MM.smooth(0, 1, o.j / Math.max(1, o.h - 1)));
      return MM.cl(col);
    }

    // ---------- feet: near-black horn under pale dust ----------
    if (FOOT[o.part]) {
      col = MM.mix(C.horn, C.dust, 0.34 + 0.26 * MM.fbm(p, 0.55, 3, 11));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }

    // ---------- base hide ----------
    var t;
    if (BODYCLS[o.part]) {
      t = MM.smooth(yb0, yb1, p.y);
      col = MM.mix(C.ventral, C.dorsal, t);
      col = MM.mix(col, C.flank, 0.40 * (1 - Math.abs(t - 0.5) * 2));
    } else if (DISTAL[o.part]) {
      t = 0.45;
      col = MM.mix(C.ventral, C.flank, 0.75);
    } else if (o.part === 'head_muzzle') {
      // DARK muzzle -- the inversion of toxodon's pale one -- with a pale mealy ring
      // confined to the very front of the lip so it does not become a blocky mask.
      t = 0.5;
      col = MM.mix(C.muzzle, C.lip, 0.55 * MM.smooth(-24.0, -27.5, p.z));
    } else if (o.part === 'head_jaw') {
      t = 0.35; col = C.jaw.slice();
    } else if (o.part === 'ear_left' || o.part === 'ear_right') {
      t = 0.8;
      col = (o.face === 'down') ? C.earin.slice() : MM.mix(C.flank, C.dorsal, 0.6);
    } else {
      t = MM.smooth(yb0, yb1, p.y);
      col = MM.mix(C.ventral, C.dorsal, t);
    }

    // Skull darkens toward the muzzle rather than paling, so the whole face reads as one
    // dark mask -- the opposite direction to toxodon's forward-paling head.
    if (o.part === 'head_skull') {
      col = MM.mix(col, C.muzzle, 0.50 * MM.smooth(-13, -21, p.z));
    }

    // ---------- sun-bleached topline ----------
    if (BODYCLS[o.part]) {
      var sun = MM.smooth(0.72, 1.0, t) * (0.45 + 0.55 * MM.fbm(p, 0.26, 3, 21));
      col = MM.mix(col, C.bleach, 0.58 * sun);
    }

    // ---------- treatment: deep folds + bare worn patches ----------
    var mot = MM.fbm(p, 0.30, 3, 2) - 0.5;
    col = MM.mul(col, 1 + mot * 0.18);

    // Rubbed-bare patches: broad, soft-edged, only on the big flank masses.
    if (BODYCLS[o.part]) {
      var bp = MM.fbm(p, 0.17, 2, 31);
      col = MM.mix(col, C.bare, 0.34 * MM.smooth(0.60, 0.80, bp));
    }

    // Folds are DEEPER and tighter-pitched than toxodon's (0.13 vs 0.085, 3.8 vs 3.0).
    var fold = Math.sin((o.j / Math.max(1, o.h)) * Math.PI * 3.8
                        + MM.fbm(p, 0.22, 2, 4) * 5.0);
    var depth = BODYCLS[o.part] ? 0.13 : 0.07;
    col = MM.mul(col, 1 + fold * depth);

    if (o.part === 'fore_left_shoulder' || o.part === 'fore_right_shoulder' ||
        o.part === 'hind_left_thigh' || o.part === 'hind_right_thigh' ||
        o.part === 'neck' || o.part === 'throat') {
      var cr = Math.sin((o.i / Math.max(1, o.w)) * Math.PI * 2.8 + MM.fbm(p, 0.3, 2, 6) * 4);
      col = MM.mul(col, 1 + cr * 0.09);
    }

    // ---------- pale savanna dust ----------
    // Carried HIGH (line ~13 vs toxodon's ~8.5) and PALE, so at a glance the two species
    // differ most exactly where they touch the ground.
    var line = 10.5 + 3.0 * (MM.fbm(p, 0.35, 2, 9) - 0.5) * 2;
    if (p.y < line + 6) {
      var m = MM.smooth(line + 6, line - 5, p.y);
      if (DISTAL[o.part]) m = Math.min(1, m * 1.10 + 0.14);
      else m *= 0.42;
      m *= 0.55 + 0.45 * MM.fbm(p, 0.5, 3, 12);
      col = MM.mix(col, C.dust, Math.max(0, Math.min(0.60, m)));
    }

    // ---------- MC face lighting compensation ----------
    if (o.face === 'up')   col = MM.mix(col, C.dorsal, 0.46);
    if (o.face === 'down') col = MM.mix(col, C.ventral, 0.30);

    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  return 'mixotoxodon painted, bled ' + MM.bledCount + ' seam texels';
})();
