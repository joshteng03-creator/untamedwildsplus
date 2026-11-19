// Thylacine (Thylacinus cynocephalus), ModelDireWolf (128x128). User 2026-10-09: the stripes ran over
// the shoulders like a tiger's. A real thylacine's 13-21 dark bands start at mid-back and get LONGER and
// closer toward the rump, running onto the thighs and the base of the tail; the shoulders, neck and
// forelegs are plain. This keeps every head/ear/eye/leg texel of the shipped skin and repaints only the
// coat parts: a plain sandy base (darker along the spine, pale belly) and the bands in world z.
//   bands      z -1 (mid-back) .. 12 (tail base), spaced 2.4u and ~1u wide: one clean texel each, even on the tilted croup
//   depth      ~2.5u down the flank at the front band, ~7u at the rump. Not on the thighs: they are
//              rotated, so world-z bands cross their faces as a diagonal staircase.
(function () {
  var TW = 128, TH = 128;
  var REF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/dire_wolf/thylacine.png';
  var base = MM.load(REF).getContext('2d').getImageData(0, 0, TW, TH).data;
  var C = {
    back  : MM.hex('#8e7046'),
    flank : MM.hex('#a88858'),
    belly : MM.hex('#cdb689'),
    band  : MM.hex('#463620')
  };
  var COAT = { hair: 1, body_main: 1, body_chest: 1, body_loin: 1, body_croup: 1, head_neck: 1,
               tail_1: 1, tail_2: 1, tail_3: 1, leg_left_upper: 1, leg_right_upper: 1 };
  var Z0 = -1.0, Z1 = 12.0, SP = 2.4, TOP = 17.0;

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, i = (o.v * TW + o.u) * 4, col;
    if (!COAT[n]) {
      return base[i + 3] ? [base[i], base[i + 1], base[i + 2]] : [0, 0, 0, 0];
    }
    var t = MM.smooth(9.0, 16.5, p.y);
    col = MM.mix(C.belly, C.flank, MM.smooth(0.0, 0.5, t));
    col = MM.mix(col, C.back, MM.smooth(0.6, 1.0, t));
    if (n === 'tail_2' || n === 'tail_3') col = MM.mix(col, C.back, 0.35 + 0.3 * MM.smooth(12.0, 18.0, p.z));

    // bands: one per SP along z, each with its own jitter, ending lower the further back it sits
    if (p.z > Z0 && p.z < Z1 && n !== 'leg_left_upper' && n !== 'leg_right_upper') {
      var k = Math.floor((p.z - Z0) / SP), f = (p.z - Z0) / SP - k;
      var jit = 0.10 * (MM.h(k, 3, 0, 41) - 0.5);
      var back01 = (p.z - Z0) / (Z1 - Z0);
      var depth = 2.6 + 4.6 * back01 + 0.8 * (MM.h(k, 5, 0, 47) - 0.5);
      var w = 0.40 + 0.10 * back01;
      if (Math.abs(f - 0.5 - jit) < w / 2 + 0.02 && p.y > TOP - depth) {
        var tip = MM.smooth(TOP - depth, TOP - depth + 1.2, p.y);
        col = MM.mix(col, C.band, 0.75 + 0.25 * tip);
      }
    }

    col = MM.mul(col, 1 + 0.10 * (MM.fbm(p, 1.4, 3, 1) - 0.5));
    col = MM.mul(col, 1 + 0.08 * (MM.dith(o.u, o.v, 2) - 0.5));
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
  MM.save(window.OUT + 'skins/thylacine.png', cv);
  return 'thylacine repainted: masked ' + cut;
})();
