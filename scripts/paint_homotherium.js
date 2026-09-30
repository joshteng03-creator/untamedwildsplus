// Homotherium latidens -- the scimitar-toothed cat, on ModelBigCat (128x64). Shipped as a 0.98
// recolour of sabertooth.png (8.4 apart in the census). Redone against the sabertooth on every mark:
//   sabertooth   SPOTTED olive-tawny body, GREY head and neck, ringed dark tail
//   homotherium  PLAIN pale sand-cream cold-steppe coat, a darker tawny SADDLE down the sloping
//                back, head the body's colour, dark ear backs and lip line, a dark tail tip
//
// Sabre-cat conventions come from sabertooth.png, not invention: its alpha is the mask (short
// bobtail = tail_3..5 clear, no mane, lower fangs clear), and the eyes, the upper fangs and the
// mouth (head_snout_teeth) are stamped verbatim. Paw toe lines follow the reference's dark texels.
(function () {
  var C = {
    back   : MM.hex('#9b7a4f'),   // tawny saddle
    flank  : MM.hex('#c8ad84'),   // pale sand-cream
    belly  : MM.hex('#e3d5bd'),
    ear    : MM.hex('#3a2c22'),   // dark ear backs
    lip    : MM.hex('#40302a'),
    nose   : MM.hex('#6e4c42'),
    tailTip: MM.hex('#3a2c22'),
    toe    : MM.hex('#2a221c')
  };
  var REF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/big_cat/sabertooth.png';
  var ref = MM.load(REF).getContext('2d').getImageData(0, 0, 128, 64).data;
  // FACE: the lioness face from lion_1_female.png as the STRUCTURE (forehead blaze, pale eye
  // corners, dark nose/mouth, pale whisker pads, banded cheeks, pink inner ears), recoloured into
  // this palette by brightness. Homotherium was lion-like, and a cat face lives in its pixel
  // structure -- a smooth sand block with fur strands read as a striped dog face (user, 2026-09-30).
  var LREF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/big_cat/lion_1_female.png';
  var lref = MM.load(LREF).getContext('2d').getImageData(0, 0, 128, 64).data;
  var RAMP = [[0.05, MM.hex('#1e1812')], [0.35, MM.hex('#7a6146')], [0.55, MM.hex('#b39a74')],
              [0.70, MM.hex('#d8c7a6')], [0.85, MM.hex('#efe6d4')]];
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
    if (r - g > 60 && r - b > 60) return [r, g, b];   // only true reds/pinks (nose, mouth, ear insides); tawny goes to the ramp
    return ramp((r * 0.299 + g * 0.587 + b * 0.114) / 255);
  };
  var FACE = { head_main: 1, head_snout: 1, head_jaw: 1, head_cheek_left: 1, head_cheek_right: 1, ear_left: 1, ear_right: 1 };
  var lum = function (u, v) { var i = (v * 128 + u) * 4; return (ref[i] * 0.299 + ref[i + 1] * 0.587 + ref[i + 2] * 0.114) / 255; };
  var STAMP = ['eye_right', 'eye_right_1', 'teeth_right', 'teeth_left', 'head_snout_teeth'];
  var PAW = { arm_left_paw: 1, arm_right_paw: 1, leg_left_paw: 1, leg_right_paw: 1 };
  var HEAD = { head_main: 1, head_snout: 1, head_jaw: 1, head_cheek_left: 1, head_cheek_right: 1, head_neck: 1 };
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
    // countershaded base keyed on height, with the saddle following the sloping topline
    var t = MM.smooth(7.0, 18.5, p.y);
    col = MM.mix(C.belly, C.flank, MM.smooth(0.0, 0.5, t));
    // saddle over the top AND down the upper flanks, strongest on the midline
    col = MM.mix(col, C.back, MM.smooth(0.55, 1.0, t) * (0.55 + 0.35 * (1 - MM.smooth(2.0, 5.0, Math.abs(p.x)))));
    if (HEAD[n]) {
      col = MM.mix(C.flank, C.belly, 0.35 * (1 - MM.smooth(12.5, 15.5, p.y)));
      if (n === 'head_snout') {
        col = MM.mix(col, C.belly, 0.3);
        // dark lip line along the bottom edge of the upper lip, and a darker nose leather
        col = MM.mix(col, C.lip, 0.8 * (1 - MM.smooth(snout.lo.y, snout.lo.y + 0.9, p.y)));
        if (o.face === 'front' && p.y > snout.hi.y - 1.2 && Math.abs(p.x) < 1.2) col = C.nose;
      }
      if (n === 'head_jaw') col = MM.mix(C.belly, C.lip, 0.25);
    }
    if (n === 'tail_2') col = MM.mix(col, C.tailTip, MM.smooth(t2.lo.z + 2.0, t2.hi.z, p.z));
    // short dense cold-steppe coat: soft mottle + fine pixel grain, no spots
    // Fur, not a flat ramp (user's standing note from the steppe bison): tawny clumps, a
    // broader mottle, and the four-tone pixel strands the shipped cats are drawn with.
    col = MM.mix(col, C.back, 0.30 * MM.smooth(0.48, 0.74, MM.fbm(p, 0.9, 3, 27)));
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
  MM.save(window.OUT + 'skins/homotherium.png', cv);
  return 'homotherium painted: masked ' + cut + ', stamped ' + st;
})();
