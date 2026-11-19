// Puma concolor -- the puma, on ModelBigCat (128x64). Older-skin pass 2026-11-15.
// The shipped puma.png was lion_2_female with a handful of texels changed (dE 0.6 in the census), so
// in game a puma was a lioness. Repainted as the real animal:
//   a PLAIN BUFF-TAWNY coat, between the plain grey-sand cats (cave lion, American cheetah: a grey puma
//   sat 5.5 from them) and the lions (16-20 now). Its neighbours there are patterned or sabre-toothed,
//   so structure tells them apart,
//   cream-white belly, throat and inner legs, a faint darker dorsal line;
//   the puma face: white muzzle, upper lip and chin, a BLACK MOUSTACHE PATCH at the base of the whiskers
//   on each side of the snout, pale patches under the eyes;
//   blackish ear backs; a long tail ending in a DARK TIP.
// Conventions from the shipped cats (see paint_cave_lion.js): alpha mask and the eye / mouth texels from
// lion_1_female (no mane: the mane box stays alpha-0), face structure recoloured by brightness, fangs cleared.
// Limbs are coloured by facing, not by height: a height rule gives every cat white socks.
(function () {
  var C = {
    back   : MM.hex('#87633f'),
    flank  : MM.hex('#ad8658'),
    belly  : MM.hex('#e4d6c0'),
    dorsal : MM.hex('#664a30'),
    white  : MM.hex('#ece6dc'),
    moust  : MM.hex('#221c18'),
    ear    : MM.hex('#2f2722'),
    tip    : MM.hex('#241d18'),
    toe    : MM.hex('#2a221c')
  };
  var TEX = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/big_cat/';
  var ref = MM.load(TEX + 'lion_1_female.png').getContext('2d').getImageData(0, 0, 128, 64).data;
  var lum = function (d, u, v) { var i = (v * 128 + u) * 4; return (d[i] * 0.299 + d[i + 1] * 0.587 + d[i + 2] * 0.114) / 255; };
  var RAMP = [[0.05, MM.hex('#2a221c')], [0.35, MM.hex('#664a30')], [0.55, MM.hex('#87633f')],
              [0.70, MM.hex('#ad8658')], [0.85, MM.hex('#d6cab6')]];
  var ramp = function (L) {
    if (L <= RAMP[0][0]) return RAMP[0][1];
    for (var r = 1; r < RAMP.length; r++) {
      if (L <= RAMP[r][0]) return MM.mix(RAMP[r - 1][1], RAMP[r][1], (L - RAMP[r - 1][0]) / (RAMP[r][0] - RAMP[r - 1][0]));
    }
    return RAMP[RAMP.length - 1][1];
  };
  var face = function (u, v) {
    var i = (v * 128 + u) * 4, r = ref[i], g = ref[i + 1], b = ref[i + 2];
    if (ref[i + 3] === 0) return null;
    if (r - g > 60 && r - b > 60) return [r, g, b];   // nose pad, mouth, pink ear insides
    return ramp(lum(ref, u, v));
  };
  var STAMP = ['eye_right', 'eye_right_1', 'head_snout_teeth'];
  var FACE = { head_main: 1, head_snout: 1, head_jaw: 1, head_cheek_left: 1, head_cheek_right: 1, ear_left: 1, ear_right: 1 };
  var PAW = { arm_left_paw: 1, arm_right_paw: 1, leg_left_paw: 1, leg_right_paw: 1 };
  var TAIL = ['tail_1', 'tail_2', 'tail_3', 'tail_4', 'tail_5'];
  var fur = function (o, col, k) {
    col = MM.mix(col, C.back, 0.16 * k * MM.smooth(0.5, 0.75, MM.fbm(o.wp, 0.9, 3, 27)));
    col = MM.mul(col, 1 + 0.14 * k * (MM.fbm(o.wp, 0.8, 3, 2) - 0.5));
    var sh = MM.h(o.u, Math.floor((o.v + 5 * MM.h(o.u, 1, 0, 3)) / 3), 0, 19);
    return MM.mul(col, 1 + 0.2 * k * (Math.floor(sh * 4) / 3 - 0.5));
  };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, col;
    if (STAMP.indexOf(n) >= 0 || n === 'neck_mane') return [0, 0, 0, 0];
    if (PAW[n] && lum(ref, o.u, o.v) < 0.18) return MM.cl(C.toe);
    var pink = null;
    if (FACE[n]) {
      var fc = face(o.u, o.v);
      if (!fc) return null;
      var i0 = (o.v * 128 + o.u) * 4;
      if (ref[i0] - ref[i0 + 1] > 60 && ref[i0] - ref[i0 + 2] > 60) pink = fc;
      col = fc;
      if ((n === 'ear_left' || n === 'ear_right') && o.face === 'back') col = C.ear;
      if (n === 'head_jaw') col = MM.mix(C.white, col, 0.15);
      if (n === 'head_snout') {
        if (o.face === 'down') col = C.white;
        else if (o.face === 'front' && p.y < 15.0) col = C.white;                 // upper lip under the nose
        else if (o.face === 'side_l' || o.face === 'side_r') {
          if (p.y < 15.0) col = C.white;                                          // whisker pad
          var m = MM.smooth(14.6, 15.0, p.y) * (1 - MM.smooth(15.9, 16.4, p.y))   // the black moustache patch
                * (1 - MM.smooth(-18.4, -17.4, p.z));
          col = MM.mix(col, C.moust, 0.92 * m);
        }
      }
      if (n === 'head_main' && o.face === 'front') {
        var ux = Math.abs(p.x);
        var under = MM.smooth(1.0, 1.5, ux) * (1 - MM.smooth(3.0, 3.5, ux)) * MM.smooth(15.4, 15.9, p.y) * (1 - MM.smooth(16.5, 16.8, p.y));
        col = MM.mix(col, C.white, 0.6 * under);                                  // pale patch under each eye
      }
      if ((n === 'head_cheek_left' || n === 'head_cheek_right') && p.y < 14.4) col = MM.mix(col, C.white, 0.7 * (1 - MM.smooth(13.2, 14.4, p.y)));
      if (pink) col = pink;
      return MM.cl(MM.mul(col, 0.97 + 0.06 * MM.dith(o.u, o.v, 9)));
    }
    var limb = n.indexOf('arm_') === 0 || n.indexOf('leg_') === 0;
    if (limb) {
      var inner = o.wn[0] * (p.x < 0 ? -1 : 1) < -0.5;                         // the face turned toward the belly
      col = inner ? MM.mix(C.flank, C.belly, 0.7) : (o.face === 'back' ? MM.mix(C.flank, C.belly, 0.25) : C.flank);
      if (PAW[n]) col = MM.mix(col, C.belly, 0.25);
      return MM.cl(fur(o, col, 0.9));
    }
    var t = MM.smooth(8.5, 17.5, p.y);
    col = MM.mix(C.belly, C.flank, MM.smooth(0.0, 0.28, t));   // cream only along the underside, not a bib up the flank
    col = MM.mix(col, C.back, MM.smooth(0.6, 1.0, t) * 0.8);
    if (n === 'head_neck' && p.y < 14.0) col = MM.mix(col, C.white, 0.75 * (1 - MM.smooth(12.4, 14.0, p.y)));   // white throat
    col = MM.mix(col, C.dorsal, 0.3 * (1 - MM.smooth(0.6, 1.8, Math.abs(p.x))) * MM.smooth(0.8, 1.0, t)
                 * (0.8 + 0.4 * MM.dith(o.u, o.v, 31)));
    var ti = TAIL.indexOf(n);
    if (ti >= 0) {
      col = MM.mix(C.flank, C.belly, o.face === 'down' ? 0.45 : 0.0);
      if (ti === 4) col = C.tip;
      if (ti === 3) col = MM.mix(col, C.tip, MM.smooth(MM.map[n].hi.z - 2.6, MM.map[n].hi.z - 0.6, p.z));
    }
    return MM.cl(fur(o, col, 1.0));
  });

  MM.bleed(cv, 3, MM.faceRects(STAMP.concat(['neck_mane'])));

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
  MM.save(window.OUT + 'skins/puma.png', cv);
  return 'puma painted: masked ' + cut + ', stamped ' + st;
})();
