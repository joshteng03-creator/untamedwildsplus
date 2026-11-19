// Glossotherium robustum -- the broad-muzzled grazing ground sloth of Late Pleistocene South
// America (quick-win batch, 2026-10-02), on ModelGroundSloth (256x256).
// Shipped sloths: eremotherium dark rufous, megatherium mid tawny, megalonyx and megalocnus
// grey-browns. This one takes the free PALE BUFF-GREY corner.
// The fur structure is megatherium.png's own shaggy strands, recoloured through this palette by
// brightness (so the hand-drawn hair survives), and then given Glossotherium's own marks:
//   * a pale, BROAD muzzle (the genus is named for its wide grazing mouth)
//   * dark lower-leg "stockings" fading up the limbs
//   * a slightly darker saddle along the back
// Claws, eyes and the alpha mask come from megatherium.png verbatim.
(function () {
  var REF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/ground_sloth/megatherium.png';
  var ref = MM.load(REF).getContext('2d').getImageData(0, 0, 256, 256).data;
  var RAMP = [[0.06, MM.hex('#2a241d')], [0.25, MM.hex('#5f5444')], [0.40, MM.hex('#968568')],
              [0.55, MM.hex('#c2b297')], [0.75, MM.hex('#e0d4bd')]];
  var ramp = function (L) {
    if (L <= RAMP[0][0]) return RAMP[0][1];
    for (var r = 1; r < RAMP.length; r++) {
      if (L <= RAMP[r][0]) return MM.mix(RAMP[r - 1][1], RAMP[r][1], (L - RAMP[r - 1][0]) / (RAMP[r][0] - RAMP[r - 1][0]));
    }
    return RAMP[RAMP.length - 1][1];
  };
  var KEEP = function (n) { return n.indexOf('claw') === 0 || n.indexOf('eye') === 0; };
  var LIMB = function (n) { return /^(arm|leg|hand|foot)_/.test(n); };
  var stocking = MM.hex('#3a3128'), saddle = MM.hex('#7d6d58'), muzzle = MM.hex('#e6dccb');
  var lumAt = function (u, v) {
    var i = (v * 256 + u) * 4;
    return { a: ref[i + 3], L: (ref[i] * 0.299 + ref[i + 1] * 0.587 + ref[i + 2] * 0.114) / 255 };
  };
  // megatherium's own brightness range, so the ramp uses the whole palette
  var lo = 1, hi = 0;
  for (var k = 0; k < ref.length; k += 4) {
    if (!ref[k + 3]) continue;
    var L0 = (ref[k] * 0.299 + ref[k + 1] * 0.587 + ref[k + 2] * 0.114) / 255;
    if (L0 < lo) lo = L0; if (L0 > hi) hi = L0;
  }
  var snout = MM.map['snout'];

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp;
    if (KEEP(n)) return [0, 0, 0, 0];
    var t = lumAt(o.u, o.v);
    if (!t.a) return [0, 0, 0, 0];
    var col = ramp(0.04 + 0.80 * (t.L - lo) / Math.max(0.01, hi - lo));
    if (LIMB(n)) col = MM.mix(col, stocking, 0.75 * (1 - MM.smooth(2.0, 13.0, p.y)));
    if (!LIMB(n) && n !== 'snout' && n !== 'jaw' && n !== 'head') {
      col = MM.mix(col, saddle, 0.70 * MM.smooth(0.45, 0.85, (p.y - MM.LO.y) / (MM.HI.y - MM.LO.y)) * (1 - MM.smooth(5.0, 10.0, Math.abs(p.x))));
    }
    if (n === 'snout' || n === 'jaw') col = MM.mix(col, muzzle, n === 'snout' ? 0.55 : 0.35);
    if (n === 'snout' && o.face === 'front' && Math.abs(p.x) < 1.6 && p.y > snout.hi.y - 1.6) col = MM.hex('#3a3029');
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(Object.keys(MM.map).filter(KEEP)));

  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, 256, 256), cut = 0, st = 0;
  var keepRects = MM.faceRects(Object.keys(MM.map).filter(KEEP));
  for (var i = 0; i < img.data.length; i += 4) {
    if (ref[i + 3] === 0 && img.data[i + 3] !== 0) { img.data[i + 3] = 0; cut++; }
  }
  keepRects.forEach(function (R) {
    for (var y = R[1]; y < R[3]; y++) for (var x = R[0]; x < R[2]; x++) {
      var j = (y * 256 + x) * 4;
      img.data[j] = ref[j]; img.data[j + 1] = ref[j + 1]; img.data[j + 2] = ref[j + 2]; img.data[j + 3] = ref[j + 3];
      st++;
    }
  });
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/glossotherium.png', cv);
  return 'glossotherium painted: masked ' + cut + ', kept ' + st + ' claw/eye texels';
})();
