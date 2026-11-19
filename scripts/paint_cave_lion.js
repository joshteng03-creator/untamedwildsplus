// Panthera spelaea -- the cave lion, on ModelBigCat (128x64), both sexes: window.CL_SEX is 'male' or
// 'female' and each writes its own PNG. Replaces the flat upstream skin (2026-10-09, Phase 3.5).
// Evidence: permafrost cubs (Boris, Sparta) have light, greyish-sandy fur; Chauvet and other cave art show
// MANELESS males and, on some figures, faint flank stripes. So:
//   a GREY-SAND coat (first pass was too pale -- read as a polar cat; warmed and darkened) (no shipped cat holds it: lions are tawny, the American lion rufous, the snow
//   leopard grey with rosettes), cream belly, a faint darker dorsal line, FAINT VERTICAL FLANK STRIPES,
//   dark ear backs, dark tail tuft, dense winter-fur texture. No mane on either sex (the male's mane box is
//   alpha-cleared exactly like the lioness's).
// Conventions from the shipped lions (see paint_american_lion.js): alpha mask from lion_1_male /
// lion_1_female, eyes and mouth stamped verbatim, fangs cleared, face = lioness face recoloured by
// brightness into this palette.
(function () {
  var SEX = window.CL_SEX || 'female';
  var C = {
    back   : MM.hex('#8a7a5e'),
    flank  : MM.hex('#a8977a'),
    belly  : MM.hex('#d8ccb0'),
    dorsal : MM.hex('#6b5c44'),
    stripe : MM.hex('#76654a'),
    ear    : MM.hex('#3a3028'),
    tuft   : MM.hex('#2e241c'),
    toe    : MM.hex('#2a221c')
  };
  var TEX = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/big_cat/';
  var ref = MM.load(TEX + (SEX === 'male' ? 'lion_1_male.png' : 'lion_1_female.png')).getContext('2d').getImageData(0, 0, 128, 64).data;
  var lref = MM.load(TEX + 'lion_1_female.png').getContext('2d').getImageData(0, 0, 128, 64).data;
  var lum = function (d, u, v) { var i = (v * 128 + u) * 4; return (d[i] * 0.299 + d[i + 1] * 0.587 + d[i + 2] * 0.114) / 255; };
  var RAMP = [[0.05, MM.hex('#2a221a')], [0.35, MM.hex('#6e5f48')], [0.55, MM.hex('#8f7f63')],
              [0.70, MM.hex('#b0a084')], [0.85, MM.hex('#d8ccb0')]];
  var ramp = function (L) {
    if (L <= RAMP[0][0]) return RAMP[0][1];
    for (var r = 1; r < RAMP.length; r++) {
      if (L <= RAMP[r][0]) return MM.mix(RAMP[r - 1][1], RAMP[r][1], (L - RAMP[r - 1][0]) / (RAMP[r][0] - RAMP[r - 1][0]));
    }
    return RAMP[RAMP.length - 1][1];
  };
  var face = function (u, v) {
    var i = (v * 128 + u) * 4, r = lref[i], g = lref[i + 1], b = lref[i + 2];
    if (lref[i + 3] === 0) return null;
    if (r - g > 60 && r - b > 60) return [r, g, b];   // nose, mouth, pink ear insides
    return ramp(lum(lref, u, v));
  };
  var STAMP = ['eye_right', 'eye_right_1', 'head_snout_teeth'];
  var FACE = { head_main: 1, head_snout: 1, head_jaw: 1, head_cheek_left: 1, head_cheek_right: 1, ear_left: 1, ear_right: 1 };
  var PAW = { arm_left_paw: 1, arm_right_paw: 1, leg_left_paw: 1, leg_right_paw: 1 };
  var TAIL = ['tail_1', 'tail_2', 'tail_3', 'tail_4', 'tail_5'];
  var BODY = { body_main: 1, body_torso: 1, body_tail: 1 };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, col;
    if (STAMP.indexOf(n) >= 0) return [0, 0, 0, 0];
    if (n === 'neck_mane') return [0, 0, 0, 0];          // maneless, both sexes
    if (PAW[n] && lum(ref, o.u, o.v) < 0.18) return MM.cl(C.toe);
    if (FACE[n]) {
      var fc = face(o.u, o.v);
      if (fc) {
        if ((n === 'ear_left' || n === 'ear_right') && o.face === 'back') fc = C.ear;
        return MM.cl(MM.mul(fc, 0.97 + 0.06 * MM.dith(o.u, o.v, 9)));
      }
    }
    var t = MM.smooth(7.0, 18.5, p.y);
    col = MM.mix(C.belly, C.flank, MM.smooth(0.0, 0.5, t));
    col = MM.mix(col, C.back, MM.smooth(0.6, 1.0, t) * 0.75);
    // faint dorsal line
    col = MM.mix(col, C.dorsal, 0.35 * (1 - MM.smooth(0.6, 1.8, Math.abs(p.x))) * MM.smooth(0.8, 1.0, t)
                 * (0.8 + 0.4 * MM.dith(o.u, o.v, 31)));
    // faint vertical flank stripes (cave art): wavy bands along the body, only on the upper flank, never the belly
    if (BODY[n] || (n.indexOf('leg_') === 0 && n.indexOf('_1') > 0) || (n.indexOf('arm_') === 0 && n.indexOf('_1') > 0)) {
      var wob = 1.6 * (MM.fbm(p, 0.35, 2, 13) - 0.5);
      var band = Math.cos((p.z + wob) * 0.95);
      var s = MM.smooth(0.55, 0.9, band) * MM.smooth(0.35, 0.65, t) * (1 - MM.smooth(0.92, 1.0, t));
      col = MM.mix(col, C.stripe, 0.42 * s * (0.75 + 0.5 * MM.dith(o.u, o.v, 53)));
    }
    var ti = TAIL.indexOf(n);
    if (ti >= 2) col = MM.mix(col, C.tuft, ti === 2 ? MM.smooth(MM.map[n].lo.z + 2.5, MM.map[n].hi.z, p.z) : 1);
    // dense winter fur: clumps, broad mottle, four-tone pixel strands (a touch stronger than the lions')
    col = MM.mix(col, C.back, 0.22 * MM.smooth(0.48, 0.74, MM.fbm(p, 0.9, 3, 27)));
    col = MM.mul(col, 1 + 0.18 * (MM.fbm(p, 0.8, 3, 2) - 0.5));
    var sh = MM.h(o.u, Math.floor((o.v + 5 * MM.h(o.u, 1, 0, 3)) / 3), 0, 19);
    col = MM.mul(col, 1 + 0.26 * (Math.floor(sh * 4) / 3 - 0.5));
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(STAMP.concat(['neck_mane'])));

  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, 128, 64), cut = 0, st = 0;
  var maneRects = MM.faceRects(['neck_mane']);
  var inMane = function (x, y) {
    for (var q = 0; q < maneRects.length; q++) {
      var R = maneRects[q];
      if (x >= R[0] && x < R[2] && y >= R[1] && y < R[3]) return true;
    }
    return false;
  };
  for (var i = 0; i < img.data.length; i += 4) {
    var px = (i / 4) % 128, py = Math.floor(i / 4 / 128);
    if (inMane(px, py)) { if (img.data[i + 3] !== 0) { img.data[i + 3] = 0; cut++; } continue; }
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
  MM.save(window.OUT + 'skins/cave_lion_' + SEX + '.png', cv);
  return 'cave_lion_' + SEX + ' painted: masked ' + cut + ', stamped ' + st;
})();
