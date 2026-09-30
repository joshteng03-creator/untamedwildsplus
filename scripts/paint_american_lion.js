// Panthera atrox -- the American lion, on ModelBigCat (128x64), dimorphic: window.AL_SEX is
// 'male' or 'female' and each writes its own PNG. Shipped as a 0.97 recolour of cave_lion that sat
// 3.3 from the puma in the census. Redone into a colour slot no shipped cat holds:
//   a RICH, DARK RUFOUS-TAWNY coat (lions/pumas are mid tawny, cave lion is pale sand), a faint
//   darker dorsal line, cream belly, dark ear backs, dark tail tuft.
//   The MALE gets a SHORT DARK RUFF, not a full mane: the fossil and cave-art evidence points to
//   sparse or absent manes in the Pleistocene lions, so the ruff is shortened by alpha-clearing the
//   lower rows of the mane box -- the same alpha trick the mod uses to delete a lioness's mane.
//
// Conventions from the shipped lions: alpha mask from lion_1_male / lion_1_female, eyes and the
// mouth (head_snout_teeth) stamped verbatim, fangs cleared (non-sabre). The face is the lioness
// face structure recoloured by brightness into this palette, as on homotherium.
(function () {
  var SEX = window.AL_SEX || 'female';
  var C = {
    // redder and darker than first painted: at #9a5a31/#dbc2a2 the census put it 6.9 from the
    // shipped lion_2_male -- the cream belly dragged the mean back to ordinary lion tawny
    back   : MM.hex('#5c2c15'),
    flank  : MM.hex('#8c4523'),
    belly  : MM.hex('#c99e7c'),
    dorsal : MM.hex('#4a2814'),
    ear    : MM.hex('#2c1c14'),
    tuft   : MM.hex('#2a1b12'),
    ruff   : MM.hex('#4a2a18'),
    ruffDk : MM.hex('#2e1b10'),
    toe    : MM.hex('#241a14')
  };
  var TEX = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/big_cat/';
  var ref = MM.load(TEX + (SEX === 'male' ? 'lion_1_male.png' : 'lion_1_female.png')).getContext('2d').getImageData(0, 0, 128, 64).data;
  var lref = MM.load(TEX + 'lion_1_female.png').getContext('2d').getImageData(0, 0, 128, 64).data;
  var lum = function (d, u, v) { var i = (v * 128 + u) * 4; return (d[i] * 0.299 + d[i + 1] * 0.587 + d[i + 2] * 0.114) / 255; };
  var RAMP = [[0.05, MM.hex('#1c120c')], [0.35, MM.hex('#5e3319')], [0.55, MM.hex('#8c4523')],
              [0.70, MM.hex('#b77f5a')], [0.85, MM.hex('#dcc0a2')]];
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

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, col;
    if (STAMP.indexOf(n) >= 0) return [0, 0, 0, 0];
    if (n === 'neck_mane') {
      if (SEX !== 'male') return [0, 0, 0, 0];
      // short ruff: keep the upper collar, cut the rest away along a ragged edge
      // A flat dark slab read as a hood (first pass). Now: coat-coloured hair darkening toward
      // its tips, ending in a fringe of alternating one-texel tufts like the bison skirts.
      var edge = 13.0 + 2.2 * (MM.fbm(p, 0.7, 2, 7) - 0.5);
      if (p.y < edge - 2.6) return [0, 0, 0, 0];
      if (p.y < edge && (o.u + Math.floor(MM.h(o.u, 0, 0, 41) * 2)) % 2 === 0) return [0, 0, 0, 0];
      var tip = 1 - MM.smooth(edge - 2.6, edge + 4.0, p.y);
      col = MM.mix(C.flank, C.ruffDk, 0.30 + 0.55 * tip);
      var rs = MM.h(o.u, Math.floor((o.v + 5 * MM.h(o.u, 1, 0, 3)) / 3), 0, 29);
      return MM.cl(MM.mul(col, 1 + 0.30 * (Math.floor(rs * 4) / 3 - 0.5)));
    }
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
    col = MM.mix(col, C.back, MM.smooth(0.6, 1.0, t) * 0.8);
    // faint dorsal line down the spine, broken edges
    col = MM.mix(col, C.dorsal, 0.45 * (1 - MM.smooth(0.6, 1.8, Math.abs(p.x))) * MM.smooth(0.8, 1.0, t)
                 * (0.8 + 0.4 * MM.dith(o.u, o.v, 31)));
    // dark tail tuft on the last painted segment
    var ti = TAIL.indexOf(n);
    if (ti >= 2) col = MM.mix(col, C.tuft, ti === 2 ? MM.smooth(MM.map[n].lo.z + 2.5, MM.map[n].hi.z, p.z) : 1);
    // fur: clumps, broad mottle, four-tone pixel strands
    col = MM.mix(col, C.back, 0.25 * MM.smooth(0.48, 0.74, MM.fbm(p, 0.9, 3, 27)));
    col = MM.mul(col, 1 + 0.20 * (MM.fbm(p, 0.8, 3, 2) - 0.5));
    var sh = MM.h(o.u, Math.floor((o.v + 5 * MM.h(o.u, 1, 0, 3)) / 3), 0, 19);
    col = MM.mul(col, 1 + 0.24 * (Math.floor(sh * 4) / 3 - 0.5));
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(STAMP.concat(['neck_mane'])));

  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, 128, 64), cut = 0, st = 0;
  var maneRects = MM.faceRects(['neck_mane']);
  var inMane = function (x, y) {
    for (var q = 0; q < maneRects.length; q++) {
      var R = maneRects[q];
      if (x >= R[0] && x < R[2] && y >= R[1] && y < R[3]) return true;   // [u0, v0, u1, v1]
    }
    return false;
  };
  for (var i = 0; i < img.data.length; i += 4) {
    var px = (i / 4) % 128, py = Math.floor(i / 4 / 128);
    // the male's ruff decides its own alpha; everything else follows the reference
    if (SEX === 'male' && inMane(px, py)) continue;
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
  MM.save(window.OUT + 'skins/american_lion_' + SEX + '.png', cv);
  return 'american_lion_' + SEX + ' painted: masked ' + cut + ', stamped ' + st;
})();
