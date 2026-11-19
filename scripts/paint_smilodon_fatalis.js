// Smilodon fatalis -- the North American sabertooth, on ModelBigCat (128x64). A new species
// (quick-win batch, 2026-10-02), built to read apart from every sabre cat already shipped:
//   sabertooth (S. populator)  SPOTTED olive-tawny body, GREY head and neck, ringed dark tail
//   homotherium                PLAIN pale sand-cream, tawny saddle
//   thylacosmilus              cold grey-brown
//   smilodon_fatalis           DARK CINNAMON-RUSSET woodland coat, chestnut back, cream belly and
//                              throat, faint broken dark BARRING on the flanks (no rosettes), head
//                              the body's colour, dark lip line, dark bobtail tip
// The colour aims at an empty corner of the big-cat census (hue ~22, dark, mid-saturation): the
// tawny cats are crowded at hue 30-45 / light 0.4-0.6.
//
// Sabre-cat conventions come from sabertooth.png, not invention: its alpha is the mask (short
// bobtail, no mane, lower fangs clear) and the eyes, sabres and mouth are stamped verbatim. The
// face structure is lion_1_female.png's, recoloured through this palette (see paint_homotherium.js:
// a smooth face block reads as a dog, a cat face lives in its pixel structure).
(function () {
  var C = {
    back   : MM.hex('#4f2616'),   // chestnut back
    flank  : MM.hex('#7c3c22'),   // cinnamon-russet
    belly  : MM.hex('#c2a07e'),   // cream
    bar    : MM.hex('#341a10'),   // faint flank barring
    ear    : MM.hex('#2a1d16'),
    lip    : MM.hex('#2e211a'),
    nose   : MM.hex('#6b463b'),
    tailTip: MM.hex('#2a1d16'),
    toe    : MM.hex('#221a15')
  };
  var REF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/big_cat/sabertooth.png';
  var ref = MM.load(REF).getContext('2d').getImageData(0, 0, 128, 64).data;
  var LREF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/big_cat/lion_1_female.png';
  var lref = MM.load(LREF).getContext('2d').getImageData(0, 0, 128, 64).data;
  var RAMP = [[0.05, MM.hex('#1c120c')], [0.35, MM.hex('#4f2616')], [0.55, MM.hex('#7c3c22')],
              [0.70, MM.hex('#b48462')], [0.85, MM.hex('#dcc2a4')]];
  var ramp = function (L) {
    if (L <= RAMP[0][0]) return RAMP[0][1];
    for (var r = 1; r < RAMP.length; r++) {
      if (L <= RAMP[r][0]) return MM.mix(RAMP[r - 1][1], RAMP[r][1], (L - RAMP[r - 1][0]) / (RAMP[r][0] - RAMP[r - 1][0]));
    }
    return RAMP[RAMP.length - 1][1];
  };
  var faceFromLion = function (u, v) {
    var i = (v * 128 + u) * 4, r = lref[i], g = lref[i + 1], b = lref[i + 2], a = lref[i + 3];
    if (a === 0) return null;
    if (r - g > 60 && r - b > 60) return [r, g, b];   // true reds/pinks only (nose, mouth, inner ears)
    return ramp((r * 0.299 + g * 0.587 + b * 0.114) / 255);
  };
  var FACE = { head_main: 1, head_snout: 1, head_jaw: 1, head_cheek_left: 1, head_cheek_right: 1, ear_left: 1, ear_right: 1 };
  var lum = function (u, v) { var i = (v * 128 + u) * 4; return (ref[i] * 0.299 + ref[i + 1] * 0.587 + ref[i + 2] * 0.114) / 255; };
  var STAMP = ['eye_right', 'eye_right_1', 'teeth_right', 'teeth_left', 'head_snout_teeth'];
  var PAW = { arm_left_paw: 1, arm_right_paw: 1, leg_left_paw: 1, leg_right_paw: 1 };
  var HEAD = { head_main: 1, head_snout: 1, head_jaw: 1, head_cheek_left: 1, head_cheek_right: 1, head_neck: 1 };
  // Barring on the TORSO only: bands keyed on z checkerboard on limbs (skin-painting traps note).
  var TORSO = { body_main: 1, body_torso: 1, body_abdomen: 1 };
  var t2 = MM.map['tail_2'], snout = MM.map['head_snout'];

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, col;
    if (STAMP.indexOf(n) >= 0) return [0, 0, 0, 0];
    if (PAW[n] && lum(o.u, o.v) < 0.18) return MM.cl(C.toe);
    if (FACE[n]) {
      var fc = faceFromLion(o.u, o.v);
      if (fc) return MM.cl(MM.mul(fc, 0.97 + 0.06 * MM.dith(o.u, o.v, 9)));
    }
    if (n === 'ear_left' || n === 'ear_right') {
      col = o.face === 'back' ? C.ear : MM.mix(C.flank, C.ear, 0.35);
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 5)));
    }
    // countershaded base: cream belly -> cinnamon flank -> chestnut back along the topline
    var t = MM.smooth(7.0, 18.5, p.y);
    col = MM.mix(C.belly, C.flank, MM.smooth(0.0, 0.32, t));
    col = MM.mix(col, C.back, MM.smooth(0.6, 1.0, t) * (0.5 + 0.4 * (1 - MM.smooth(2.0, 5.0, Math.abs(p.x)))));
    if (HEAD[n]) {
      col = MM.mix(C.flank, C.belly, 0.4 * (1 - MM.smooth(12.5, 15.5, p.y)));
      if (n === 'head_snout') {
        col = MM.mix(col, C.belly, 0.35);
        col = MM.mix(col, C.lip, 0.8 * (1 - MM.smooth(snout.lo.y, snout.lo.y + 0.9, p.y)));
        if (o.face === 'front' && p.y > snout.hi.y - 1.2 && Math.abs(p.x) < 1.2) col = C.nose;
      }
      if (n === 'head_jaw') col = MM.mix(C.belly, C.lip, 0.2);
    }
    // faint broken vertical barring on the flanks: a stripe phase along the body, wobbled by
    // noise and broken up by a second field, fading out on the belly and the back
    if (TORSO[n] && o.face !== 'up' && o.face !== 'down') {
      var ph = p.z * 0.9 + 1.6 * MM.fbm(p, 0.35, 2, 41) + 0.25 * p.y;
      var band = MM.smooth(0.45, 0.80, 0.5 + 0.5 * Math.sin(ph));
      var brk = MM.smooth(0.34, 0.58, MM.fbm(p, 0.7, 2, 57));
      var mid = MM.smooth(0.15, 0.4, t) * (1 - MM.smooth(0.8, 1.0, t));
      col = MM.mix(col, C.bar, 0.68 * band * brk * mid);
    }
    if (n === 'tail_2') col = MM.mix(col, C.tailTip, MM.smooth(t2.lo.z + 2.0, t2.hi.z, p.z));
    // fur, not a flat ramp: clumps, broader mottle, four-tone pixel strands like the shipped cats
    col = MM.mix(col, C.back, 0.25 * MM.smooth(0.48, 0.74, MM.fbm(p, 0.9, 3, 27)));
    col = MM.mul(col, 1 + 0.20 * (MM.fbm(p, 0.8, 3, 2) - 0.5));
    var sh = MM.h(o.u, Math.floor((o.v + 5 * MM.h(o.u, 1, 0, 3)) / 3), 0, 19);
    col = MM.mul(col, 1 + 0.24 * (Math.floor(sh * 4) / 3 - 0.5));
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(STAMP));

  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, 128, 64), cut = 0, st = 0;
  for (var i = 0; i < img.data.length; i += 4) {
    if (ref[i + 3] === 0 && img.data[i + 3] !== 0) { img.data[i + 3] = 0; cut++; }
  }
  STAMP.forEach(function (name) {
    var F = MM.map[name].faces;
    for (var k in F) {
      var f = F[k];
      for (var y = f.v0; y < f.v1; y++) for (var x = f.u0; x < f.u1; x++) {
        var j = (y * 128 + x) * 4;
        img.data[j] = ref[j]; img.data[j + 1] = ref[j + 1]; img.data[j + 2] = ref[j + 2]; img.data[j + 3] = ref[j + 3];
        st++;
      }
    }
  });
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/smilodon_fatalis.png', cv);
  return 'smilodon_fatalis painted: masked ' + cut + ', stamped ' + st;
})();
