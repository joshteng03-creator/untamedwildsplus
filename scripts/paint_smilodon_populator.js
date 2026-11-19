// Smilodon populator -- the South American sabertooth (big_cat "sabertooth"), on ModelBigCat (128x64).
// Replaces the upstream skin (2026-10-09, Phase 3.5). Built from paint_smilodon_fatalis.js so the sabre
// conventions match: sabertooth.png's alpha is the mask (bobtail, no mane, lower fangs clear), eyes, sabres
// and mouth stamped verbatim, face = lioness face structure recoloured through this palette.
// Reads apart from the other sabre cats:
//   smilodon_fatalis  dark cinnamon-russet, faint flank BARRING
//   homotherium       plain pale sand-cream
//   cave lion         grey-sand, faint stripes (not a sabre cat, but nearby in colour space)
//   populator         GOLDEN-OCHRE coat with BOLD dappled spots. The big-cat census is saturated (28 skins;
//                     shipped pairs already sit at 1.2-5): yellow-ochre landed 7.4 from the snow leopard and
//                     dark tawny 5.3 from the marsupial lion, so the spots + sabres carry the identity and the
//                     user judges by eye (nearest: caspian tiger 7.2 -- striped orange vs spotted ochre)
//                     flanks and upper legs (reconstructions of the open-country Pleistocene cat; keeps the
//                     spotted idea of the old skin), dark lip line, dark bobtail tip
(function () {
  var C = {
    back   : MM.hex('#7a5426'),
    flank  : MM.hex('#b0803e'),
    belly  : MM.hex('#e2cfa8'),
    bar    : MM.hex('#4a2e14'),
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
  var RAMP = [[0.05, MM.hex('#1c120c')], [0.35, MM.hex('#7a5426')], [0.55, MM.hex('#b0803e')],
              [0.70, MM.hex('#cba76e')], [0.85, MM.hex('#e8d8b4')]];
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
    var LIMB_TOP = n.indexOf('_upper') > 0 || /^(arm|leg)_(left|right)(_1)?$/.test(n);
    if ((TORSO[n] || LIMB_TOP) && o.face !== 'up' && o.face !== 'down') {
      var sp = MM.smooth(0.54, 0.64, MM.fbm(p, 1.7, 2, 71)) * MM.smooth(0.25, 0.45, MM.fbm(p, 0.45, 2, 83));
      var mid = MM.smooth(0.12, 0.38, t) * (1 - MM.smooth(0.82, 1.0, t));
      col = MM.mix(col, C.bar, 0.8 * sp * mid);
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
  MM.save(window.OUT + 'skins/sabertooth.png', cv);
  return 'sabertooth (S. populator) painted: masked ' + cut + ', stamped ' + st;
})();
