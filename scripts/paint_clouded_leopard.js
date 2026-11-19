// Clouded leopard -- Neofelis nebulosa (big_cat "clouded_leopard", 2.9.0), on ModelBigCat (128x64).
// Conventions from paint_smilodon_populator.js: the shipped leopard_1.png's alpha is the mask (no mane, lower fangs
// clear, long tail), eyes/teeth stamped verbatim from it, face = the lioness face structure recoloured.
// What makes it a clouded leopard and not the leopard / jaguar / snow leopard beside it:
//   * CLOUDS: large irregular blotches on the shoulders and flanks, each a dark broken RIM around a smoky centre a little
//     darker than the ground, separated by narrow pale ground lanes -- cellular (Worley) noise in WORLD space, so a cloud
//     runs across box seams instead of restarting on every face;
//   * small solid spots on the head and lower legs, broken bold stripes down the neck, dark rings on the thick tail;
//   * a cool grey-ochre ground (the leopard is warm gold, the snow leopard pale grey with rosettes).
(function () {
  var C = {
    back   : MM.hex('#6f5f40'),
    flank  : MM.hex('#a08a5c'),
    belly  : MM.hex('#d8cbb0'),
    rim    : MM.hex('#18130f'),
    smoke  : MM.hex('#4e4230'),
    ear    : MM.hex('#211a15'),
    lip    : MM.hex('#2a221b'),
    nose   : MM.hex('#6e4f45'),
    toe    : MM.hex('#211a15')
  };
  var REF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/big_cat/leopard_1.png';
  var ref = MM.load(REF).getContext('2d').getImageData(0, 0, 128, 64).data;
  var LREF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/big_cat/lion_1_female.png';
  var lref = MM.load(LREF).getContext('2d').getImageData(0, 0, 128, 64).data;
  var RAMP = [[0.05, MM.hex('#16110d')], [0.35, MM.hex('#6e6248')], [0.55, MM.hex('#9c8a64')],
              [0.70, MM.hex('#b8a882')], [0.85, MM.hex('#ddd3bc')]];
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
    if (r - g > 60 && r - b > 60) return [r, g, b];
    return ramp((r * 0.299 + g * 0.587 + b * 0.114) / 255);
  };
  // cellular noise: distances to the nearest and second-nearest jittered feature point, cell size s
  var worley = function (p, s, seed) {
    var x = p.x / s, y = p.y / s, z = p.z / s;
    var xi = Math.floor(x), yi = Math.floor(y), zi = Math.floor(z), f1 = 9, f2 = 9, id = 0;
    for (var dx = -1; dx <= 1; dx++) for (var dy = -1; dy <= 1; dy++) for (var dz = -1; dz <= 1; dz++) {
      var cx = xi + dx, cy = yi + dy, cz = zi + dz;
      var fx = cx + MM.h(cx, cy, cz, seed), fy = cy + MM.h(cx, cy, cz, seed + 1), fz = cz + MM.h(cx, cy, cz, seed + 2);
      var d = Math.sqrt((fx - x) * (fx - x) + (fy - y) * (fy - y) * 1.6 + (fz - z) * (fz - z));
      if (d < f1) { f2 = f1; f1 = d; id = cx * 73 + cy * 37 + cz * 11; } else if (d < f2) { f2 = d; }
    }
    return { f1: f1, f2: f2, id: id };
  };
  var FACE = { head_main: 1, head_snout: 1, head_jaw: 1, head_cheek_left: 1, head_cheek_right: 1, ear_left: 1, ear_right: 1 };
  var lum = function (u, v) { var i = (v * 128 + u) * 4; return (ref[i] * 0.299 + ref[i + 1] * 0.587 + ref[i + 2] * 0.114) / 255; };
  var STAMP = ['eye_right', 'eye_right_1', 'teeth_right', 'teeth_left', 'head_snout_teeth'];
  var PAW = { arm_left_paw: 1, arm_right_paw: 1, leg_left_paw: 1, leg_right_paw: 1 };
  var HEAD = { head_main: 1, head_snout: 1, head_jaw: 1, head_cheek_left: 1, head_cheek_right: 1 };
  var TORSO = { body_main: 1, body_torso: 1, body_abdomen: 1 };
  var snout = MM.map['head_snout'];
  var isTail = function (n) { return n.indexOf('tail') === 0; };
  var isLimb = function (n) { return /^(arm|leg)_/.test(n) && !PAW[n]; };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, col;
    if (STAMP.indexOf(n) >= 0) return [0, 0, 0, 0];
    if (PAW[n] && lum(o.u, o.v) < 0.18) return MM.cl(C.toe);
    if (FACE[n]) {
      var fc = faceFromLion(o.u, o.v);
      if (fc) {
        col = MM.mul(fc, 0.97 + 0.06 * MM.dith(o.u, o.v, 9));
        // small solid spots on the crown and cheeks (not the muzzle)
        if (n !== 'head_snout' && n !== 'head_jaw' && n.indexOf('ear') < 0) {
          var hs = worley(p, 1.6, 401);
          if (hs.f1 < 0.32) col = MM.mix(col, C.rim, 0.85);
        }
        return MM.cl(col);
      }
    }
    if (n === 'ear_left' || n === 'ear_right') {
      col = o.face === 'back' ? C.ear : MM.mix(C.flank, C.ear, 0.35);
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 5)));
    }
    var t = MM.smooth(8.5, 17.0, p.y);
    // the pale ground is confined to the underside (pass 2 read two-tone: cream lower half, ochre upper)
    col = MM.mix(C.belly, C.flank, MM.smooth(0.0, 0.12, t));
    if (isLimb(n) || PAW[n]) col = MM.mix(C.flank, C.belly, o.face === 'down' ? 0.6 : 0.2);
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
    var top = o.face === 'up', under = o.face === 'down';
    if ((TORSO[n] || n.indexOf('_upper') > 0 || /^(arm|leg)_(left|right)(_1)?$/.test(n)) && !under) {
      // the clouds: rim where the point is near its cell's edge-ish ring, smoky centre inside, pale lane at the edge
      // (first pass: 4.2-unit cells with a 0.08-wide rim drew rims thinner than a texel -- they rendered as scattered
      // black squares, a speckled snow leopard. Now 5.5-unit clouds with 1-2 texel rims, darker centres, narrow lanes.)
      var w = worley(p, 5.5, 211);
      var edge = w.f2 - w.f1;                                  // small at a cell boundary
      var lane = 1 - MM.smooth(0.04, 0.09, edge);              // pale ground between clouds
      var rim = MM.smooth(0.08, 0.14, edge) * (1 - MM.smooth(0.30, 0.38, edge));
      var broken = MM.smooth(0.22, 0.42, MM.fbm(p, 0.9, 2, 223));   // rims are broken, not drawn rings
      var inside = MM.smooth(0.30, 0.40, edge);
      var onFlank = MM.smooth(0.05, 0.25, t) * (1 - MM.smooth(0.92, 1.0, t) * 0.4);
      col = MM.mix(col, C.smoke, 0.65 * inside * onFlank * (0.8 + 0.4 * MM.h(w.id, 1, 0, 227)));
      col = MM.mix(col, C.rim, 0.9 * rim * broken * onFlank);
      col = MM.mix(col, C.belly, 0.30 * lane * onFlank);
      if (top) {
        // along the spine: two broken dark lines
        var spine = 1 - MM.smooth(0.25, 0.55, Math.abs(Math.abs(p.x) - 1.4));
        col = MM.mix(col, C.rim, 0.7 * spine * MM.smooth(0.4, 0.6, MM.fbm(p, 0.9, 2, 229)));
      }
    } else if (n === 'head_neck' && !under) {
      // broken bold stripes running down the neck
      var st = 1 - MM.smooth(0.3, 0.7, Math.abs(((Math.abs(p.x) + 0.6 * MM.fbm(p, 0.8, 2, 233)) % 2.6) - 1.3));
      col = MM.mix(col, C.rim, 0.8 * st * MM.smooth(0.35, 0.6, MM.fbm(p, 1.1, 2, 235)));
    } else if (isLimb(n) && !under) {
      var ls = worley(p, 1.8, 241);
      if (ls.f1 < 0.34) col = MM.mix(col, C.rim, 0.8);
    } else if (isTail(n)) {
      // thick tail ringed in dark bands along its length
      var ring = (p.z + 0.4 * MM.fbm(p, 0.7, 2, 251)) / 2.4;
      if (ring - Math.floor(ring) < 0.36) col = MM.mix(col, C.rim, 0.75);
    }
    if (under) {
      var us = worley(p, 2.2, 261);
      if (us.f1 < 0.30) col = MM.mix(col, C.rim, 0.55);
    }
    col = MM.mul(col, 1 + 0.16 * (MM.fbm(p, 0.8, 3, 2) - 0.5));
    var sh = MM.h(o.u, Math.floor((o.v + 5 * MM.h(o.u, 1, 0, 3)) / 3), 0, 19);
    col = MM.mul(col, 1 + 0.16 * (Math.floor(sh * 4) / 3 - 0.5));
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
  MM.save(window.OUT + 'skins/clouded_leopard.png', cv);
  return 'clouded leopard painted: masked ' + cut + ', stamped ' + st;
})();
