// Crocuta spelaea -- the cave hyena, on ModelHyena (64x32, 19 parts). Shipped as a recolour of
// shortface.png: literally the short-faced hyena's banding in another colour. The Chauvet and
// Lascaux artists drew the cave hyena SPOTTED, so it goes back to spots -- but separated from the
// living spotted hyena on every axis:
//   spotted_1   brown coat, DENSE small spots all over
//   cave_hyena  cold-steppe pale ASH-BUFF woolly coat, FEW LARGE discrete spots, bold on the
//               haunches and legs and fading out toward the shoulders, a DARK dorsal crest,
//               dark muzzle, pale throat and cheeks
// Spots are Worley-style discrete blobs (distance to the nearest jittered cell centre), never a
// thresholded noise, which smears (reskin lessons). Eyes are stamped and the alpha is taken from
// spotted_1.png, so the hyena conventions carry over unchanged.
(function () {
  var C = {
    back   : MM.hex('#8d8676'),
    flank  : MM.hex('#aea58f'),
    belly  : MM.hex('#d2cbb8'),
    spot   : MM.hex('#3a3029'),
    crest  : MM.hex('#2f2721'),
    muzzle : MM.hex('#2e2620'),
    throat : MM.hex('#ddd6c4'),
    leg    : MM.hex('#7d7465'),
    foot   : MM.hex('#3a322b')
  };
  var REF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/hyena/spotted_1.png';
  var ref = MM.load(REF).getContext('2d').getImageData(0, 0, 64, 32).data;
  var EYE = ['eye_left', 'eye_right'];
  var LIMB = { leg_left_upper: 1, leg_left_lower: 1, leg_right_upper: 1, leg_right_lower: 1,
               arm_left_upper: 1, arm_left_lower: 1, arm_right_upper: 1, arm_right_lower: 1 };
  var snout = MM.map['head_snout'];

  // Worley spots: the nearest jittered cell centre in world space, a spot where it is close.
  var spotAt = function (p, cell, seed) {
    var gx = Math.floor(p.x / cell), gy = Math.floor(p.y / cell), gz = Math.floor(p.z / cell), best = 1e9;
    for (var dx = -1; dx <= 1; dx++) for (var dy = -1; dy <= 1; dy++) for (var dz = -1; dz <= 1; dz++) {
      var cx = gx + dx, cy = gy + dy, cz = gz + dz;
      var sx = (cx + 0.2 + 0.6 * MM.h(cx, cy, cz, seed)) * cell;
      var sy = (cy + 0.2 + 0.6 * MM.h(cx, cy, cz, seed + 7)) * cell;
      var sz = (cz + 0.2 + 0.6 * MM.h(cx, cy, cz, seed + 13)) * cell;
      var d = Math.sqrt((p.x - sx) * (p.x - sx) + (p.y - sy) * (p.y - sy) + (p.z - sz) * (p.z - sz));
      if (d < best) best = d;
    }
    return best;
  };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, col;
    if (EYE.indexOf(n) >= 0) return [0, 0, 0, 0];
    if (n === 'hair') {   // the dorsal crest
      return MM.cl(MM.mul(C.crest, 0.9 + 0.2 * MM.dith(o.u, o.v, 4)));
    }
    if (n === 'head_snout' || n === 'head_jaw') {
      col = MM.mix(C.flank, C.muzzle, n === 'head_jaw' ? 0.5 : 0.35 + 0.55 * MM.smooth(snout.hi.z - 1.0, snout.lo.z + 0.8, p.z));
    } else if (n === 'head_main' || n === 'ear_left' || n === 'ear_right') {
      col = MM.mix(C.flank, C.throat, 0.45 * (1 - MM.smooth(8.0, 11.0, p.y)));
      if (n !== 'head_main') col = MM.mix(col, C.crest, 0.55);
    } else if (LIMB[n]) {
      col = MM.mix(C.leg, C.foot, 0.8 * (1 - MM.smooth(0.5, 3.5, p.y)));
    } else {
      var t = MM.smooth(4.0, 12.0, p.y);
      col = MM.mix(C.belly, C.flank, MM.smooth(0.0, 0.5, t));
      col = MM.mix(col, C.back, MM.smooth(0.6, 1.0, t) * 0.7);
      if (n === 'head_neck') col = MM.mix(col, C.throat, 0.5 * (1 - MM.smooth(6.0, 9.0, p.y)));
      // bushy tail, darkening to a black tip (it painted pale belly-cream by height at first)
      if (n === 'tail_1') { var tm = MM.map[n]; col = MM.mix(C.back, C.crest, 0.35 + 0.6 * MM.smooth(tm.hi.y, tm.lo.y, p.y)); }
    }
    // spots: rear-weighted, strong on the haunches and legs, gone by the shoulders and head
    if (n !== 'head_main' && n !== 'head_snout' && n !== 'head_jaw' && n.indexOf('ear') < 0) {
      // Over the whole body, heavier toward the rear (0.5 at the shoulders -> 1 on the haunch):
      // sparse rear-only spots read as random dark blocks at this resolution, not as a hyena.
      var amt = 0.5 + 0.5 * MM.smooth(-4.0, 4.0, p.z);
      if (LIMB[n]) amt *= MM.smooth(1.2, 3.0, p.y);   // spots stop above the dark feet
      if (n === 'tail_1') amt = 0;
      var d = spotAt(p, 2.2, 17);
      var s = 1 - MM.smooth(0.55, 0.85, d);
      col = MM.mix(col, C.spot, s * Math.min(1, amt) * 0.9);
    }
    // thick cold-steppe coat: blocky 2x2 mottle, the shipped hyena pixel style
    col = MM.mul(col, 1 + 0.14 * (MM.fbm(p, 0.9, 3, 2) - 0.5));
    col = MM.mul(col, 1 + 0.16 * (Math.floor(MM.h(Math.floor(o.u / 2), Math.floor(o.v / 2), 0, 11) * 4) / 3 - 0.5));
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(EYE));

  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, 64, 32), cut = 0, st = 0;
  for (var i = 0; i < img.data.length; i += 4) {
    if (ref[i + 3] === 0 && img.data[i + 3] !== 0) { img.data[i + 3] = 0; cut++; }
  }
  EYE.forEach(function (name) {
    var F = MM.map[name].faces;
    for (var k in F) {
      var f = F[k];
      for (var y = f.v0; y < f.v1; y++) for (var x = f.u0; x < f.u1; x++) {
        var j = (y * 64 + x) * 4;
        img.data[j] = ref[j]; img.data[j + 1] = ref[j + 1]; img.data[j + 2] = ref[j + 2]; img.data[j + 3] = ref[j + 3];
        st++;
      }
    }
  });
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/cave_hyena.png', cv);
  return 'cave_hyena painted: masked ' + cut + ', eyes ' + st;
})();
