// Impala skin on the ANTELOPE rig (Phase 4.7, 2026-10-23) -- saves scratch/skins/antelope_impala.png. The eye rects are
// replaced afterwards with the shipped springbok's (the mod's own eye convention), by the runner, not here.
//
// Aepyceros melampus cues: a TWO-TONE body -- a dark reddish-tan saddle over paler fawn flanks, the line between them
// crisp -- a white belly, chin and throat, a pale eyebrow; a black "M" on the rump (a black vertical line down the back of
// each thigh and a black stripe along the top of the tail, whose underside is white); black ear tips; the black tufts
// over the hind fetlocks; dark ridged lyre horns (rams only in game; painted on every rect so the bleed has nothing to
// smear). All the other horn families' rects are painted too, in the same horn colour.
(function () {
  var C = {
    saddle: MM.hex('#9a5e36'), flank: MM.hex('#c39062'), belly: MM.hex('#ece6da'), face: MM.hex('#a0663c'),
    muzzle: MM.hex('#8a5634'), black: MM.hex('#1c1714'), horn: MM.hex('#2e2a26'), leg: MM.hex('#b88658'),
    hoof: MM.hex('#1e1a16'), earIn: MM.hex('#e2d8c8'), nose: MM.hex('#1a1614')
  };
  MM.bounds();
  function is(n, s) { return n.indexOf(s) >= 0; }
  function side(f) { return f === 'side_l' || f === 'side_r'; }
  var HORNS = ['horn_', 'spiral_', 'rapier_', 'scimitar_', 'lyre_'];
  function horn(n) { for (var i = 0; i < HORNS.length; i++) if (n.indexOf(HORNS[i]) === 0) return true; return false; }

  var cv = MM.paint(function (o) {
    var p = o.wp, n = o.part, f = o.face, col;
    var jj = o.h > 1 ? o.j / (o.h - 1) : 0.5;

    if (n === 'eye_left' || n === 'eye_right') return C.black.slice();      // replaced by the springbok's eye art
    if (horn(n)) {
      col = C.horn;
      if ((side(f) || f === 'front' || f === 'back') && o.j % 2 === 0) col = MM.mul(col, 1.35);   // close ridges
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 9)));
    }
    if (n === 'nose_bulb') return MM.cl(MM.mul(f === 'front' ? C.nose : C.muzzle, 0.95 + 0.1 * MM.dith(o.u, o.v, 3)));
    if (is(n, 'hoof_')) return MM.cl(MM.mul(C.hoof, 0.95 + 0.1 * MM.dith(o.u, o.v, 3)));
    if (is(n, 'ear_')) {
      col = (f === 'down' || f === 'front') ? C.earIn : C.face;
      if (jj < 0.22 && f !== 'down') col = C.black;                         // black ear tips
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 5)));
    }
    if (n === 'tail_dock' || n === 'tail_tuft') {
      col = (f === 'up' || f === 'back') ? C.black : C.belly;              // black stripe on top, white beneath
      return MM.cl(MM.mul(col, 0.92 + 0.16 * MM.h(o.u, Math.floor(o.v / 3), 0, 41)));
    }
    if (n === 'head' || n === 'muzzle') {
      col = n === 'muzzle' ? C.muzzle : C.face;
      if (f === 'down') col = C.belly;                                     // white chin and throat
      if (n === 'muzzle' && side(f) && jj > 0.7) col = C.belly;            // pale lips
      if (n === 'head' && side(f) && jj < 0.3 && p.z > -15.6) col = MM.mix(col, C.belly, 0.7);   // the pale eyebrow
      return MM.cl(MM.mul(col, 0.95 + 0.08 * MM.dith(o.u, o.v, 7)));
    }
    if (n === 'neck_base' || n === 'neck' || n === 'neck_mane' || n === 'dewlap') {
      col = (f === 'down' || (side(f) && jj > 0.7)) ? C.belly : MM.mix(C.saddle, C.flank, 0.35);
    } else if (is(n, 'arm_') || is(n, 'leg_')) {
      col = (is(n, '_1')) ? MM.mix(C.flank, C.leg, 0.4 * jj) : C.leg;
      if (f === 'back' && is(n, '_1') && is(n, 'leg_')) col = C.black;      // the M: black down the back of each thigh
      if (f === 'back' && is(n, 'leg_') && is(n, '_3') && jj < 0.35) col = C.black;   // fetlock tufts
      if (side(f) && is(n, '_1') && jj > 0.8) col = MM.mix(col, C.belly, 0.6);
    } else {
      // body: the crisp two-tone -- saddle above, fawn flank below the line, white belly
      if (f === 'up') col = C.saddle;
      else if (f === 'down') col = C.belly;
      else if (side(f)) col = jj < 0.42 ? C.saddle : (jj < 0.8 ? C.flank : C.belly);
      else col = C.saddle;
      if (n === 'body_croup' && f === 'back') col = jj < 0.2 ? C.saddle : C.belly;   // the white rump between the M
    }
    col = MM.mul(col, 1 + (MM.dith(o.u, o.v, 13) - 0.5) * 0.06);
    col = MM.mul(col, 1 + (MM.fbm(p, 1.1, 2, 5) - 0.5) * 0.08);
    if (f === 'up') col = MM.mul(col, 0.88);
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/antelope_impala.png', cv);
  return 'impala painted, bled ' + MM.bledCount;
})();
