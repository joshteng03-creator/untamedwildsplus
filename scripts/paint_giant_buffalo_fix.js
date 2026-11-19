// Giant buffalo (Pelorovis-like Bubalus/"giant_buffalo"), ModelBison (128x64), lessHair + longHorns.
// User 2026-10-09: aurochs and giant buffalo "too hairy". The wool PARTS were already hidden by lessHair;
// what still read hairy was the giant buffalo's PAINT: a pale, mottled, ragged lower body and legs like a
// shaggy fringe. This keeps every head/horn/eye/ear/tail texel of the shipped skin and repaints only:
//   body + shoulders + thighs  smooth dark slate, a touch darker toward the belly (no pale mottling)
//   lower legs                 dark down to a CLEAN pale band above dark hooves
(function () {
  var TW = 128, TH = 64;
  var REF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/bison/giant_buffalo.png';
  var base = MM.load(REF).getContext('2d').getImageData(0, 0, TW, TH).data;
  var C = {
    slate : MM.hex('#3c3c41'),
    dark  : MM.hex('#2a2a2e'),
    light : MM.hex('#4c4c52'),
    band  : MM.hex('#b6ae9f'),
    hoof  : MM.hex('#26221f')
  };
  var BODY = { body_main: 1, body_torso: 1, leg_left_thigh: 1, leg_right_thigh: 1, arm_left_1: 1, arm_right_1: 1 };
  var SHIN = { arm_left_2: 1, arm_right_2: 1, leg_left_calf: 1, leg_right_calf: 1 };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, i = (o.v * TW + o.u) * 4, col;
    if (!BODY[n] && !SHIN[n]) {
      return base[i + 3] ? [base[i], base[i + 1], base[i + 2]] : [0, 0, 0, 0];
    }
    if (BODY[n]) {
      var m = MM.map[n];
      var t = MM.smooth(m.lo.y, m.hi.y, p.y);
      col = MM.mix(C.dark, C.slate, MM.smooth(0.0, 0.6, t));
      col = MM.mix(col, C.light, 0.25 * MM.smooth(0.7, 1.0, t));
    } else {
      if (p.y < 1.0) col = C.hoof;
      else if (p.y < 3.4) col = C.band;
      else col = MM.mix(C.dark, C.slate, 0.4);
    }
    // the shipped bovid pixel style: soft 2x2 mottle + four-tone short strands, kept low so it reads sleek
    col = MM.mul(col, 1 + 0.10 * (MM.fbm(p, 0.9, 3, 2) - 0.5));
    var sh = MM.h(o.u, Math.floor((o.v + 5 * MM.h(o.u, 1, 0, 3)) / 3), 0, 19);
    col = MM.mul(col, 1 + 0.12 * (Math.floor(sh * 4) / 3 - 0.5));
    return MM.cl(col);
  });
  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, TW, TH), cut = 0;
  for (var j = 0; j < img.data.length; j += 4) {
    if (base[j + 3] === 0 && img.data[j + 3] !== 0) { img.data[j + 3] = 0; cut++; }
  }
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/giant_buffalo.png', cv);
  return 'giant_buffalo repainted: masked ' + cut;
})();
