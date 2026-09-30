// Samotherium boissieri -- a Miocene woodland-savanna giraffid with a moderately elongated
// neck and straight backswept ossicones. Known from Samos, Greece and across Eurasia.
//
// This is the PALE one, and that is its separation strategy. By the time six species share a
// rig the plain-brown niche is full (the tapir batch hit exactly this wall at species 6), so
// this one is claimed early: a light sandy-fawn animal in a set whose other members are
// chestnut, near-black, chocolate, rufous and grey-dun.
//
// Diagnostic cues:
//   * SMALL SOFT DAPPLING, not reticulation. Same Voronoi field as the giraffe at a much
//     smaller cell and far lower contrast -- fallow-deer dapples rather than giraffe patches.
//     Sharing the generator but not the parameters is the point: it is a different marking
//     produced by the same mechanism, which is how the two stay related but not confusable.
//   * a dark DORSAL STRIPE from the withers to the tail, a common woodland-ungulate cue and
//     something no other species on this rig has
//   * pale cream underside and inner legs
//   * a pale eye ring and a dark muzzle bridge
//
// TREATMENT: short fine summer coat, faint dust on the lower legs from open woodland.
(function () {
  var C = {
    // PUSHED WARM AND SATURATED from a neutral sandy fawn after measuring. The first pass sat
    // at mean L* 64.6 / a* 4.3 / b* 23.7 against the giraffe's 57.0 / 6.2 / 22.6 -- dE 7.9,
    // i.e. very nearly the same colour, separated only by being a bit lighter. Lightness alone
    // could not fix it: seven species have to pack into roughly L* 22-72, so the average gap
    // is ~8 and there is no room to open a 10-wide hole. The separation therefore has to come
    // out of CHROMA, and going richly golden takes it a* +10 / b* +10 away from the giraffe's
    // desaturated cream while leaving helladotherium's cool grey-dun further away still.
    // Going PALER instead would have worked arithmetically and looked washed out in game.
    ground : MM.hex('#cf9a48'),   // rich golden tawny
    dappleD: MM.hex('#9a6829'),   // the dapples: soft, only moderately darker
    dorsal : MM.hex('#7d5220'),   // the dark dorsal stripe
    belly  : MM.hex('#e8cf9e'),
    face   : MM.hex('#d4a660'),
    ring   : MM.hex('#f0dcb2'),   // pale eye ring
    muzzle : MM.hex('#6f5535'),
    mane   : MM.hex('#6b4520'),
    ossicone: MM.hex('#a8874f'),
    tuft   : MM.hex('#42311f'),
    dust   : MM.hex('#d8c084'),
    tassel : MM.hex('#3a2b1c'),
    hoof   : MM.hex('#332b23')
  };

  eval(require('fs').readFileSync(window.SP + 'paint_giraffid_common.js', 'utf8'));
  MM.bounds();
  girEyeInit();
  var mz = MM.map['head_muzzle'], noseZ = mz.lo.z;
  var tas = MM.map['tail_tassel'];
  var eyeM = MM.map['eye_left'];

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    var ec = window.girEye(o);
    if (ec) return ec;

    if (window.GIR_FOOT[o.part]) {
      col = MM.mix(C.hoof, C.dust, 0.24 * MM.fbm(p, 0.6, 3, 11));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }

    if (o.part === 'tail_tassel') {
      col = MM.mix(C.tassel, C.dorsal, 0.30 * MM.smooth(tas.lo.y, tas.hi.y, p.y));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 6)));
    }

    if (window.GIR_MANE[o.part]) {
      col = MM.mix(C.mane, C.dorsal, 0.28 * MM.dith(o.u, o.v, 9));
      return MM.cl(window.girFaceLight(col, o.face));
    }

    if (window.GIR_OSS[o.part]) {
      var m = MM.map[o.part];
      var up = MM.smooth(m.lo.y + (m.hi.y - m.lo.y) * 0.42, m.hi.y, p.y);
      col = MM.mix(C.ossicone, C.tuft, up);
      return MM.cl(MM.mul(window.girFaceLight(col, o.face),
                          0.95 + 0.10 * MM.dith(o.u, o.v, 5)));
    }

    if (window.GIR_EAR[o.part]) {
      if (o.face === 'down') return MM.cl(MM.mul(C.belly, 0.95));
      col = MM.mix(C.ground, C.dorsal, 0.28);
      return MM.cl(MM.mul(window.girFaceLight(col, o.face),
                          0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- base coat: countershaded sandy ----------
    if (window.GIR_HEAD[o.part]) {
      col = MM.mix(C.ground, C.face, 0.6);
      // Pale eye ring, stamped as a soft halo around the eye plane's own world centre. The
      // SOP forbids painting an eye SOCKET on a cheek face -- that lands a blotch beside the
      // eye -- but a wide soft ring centred on the real eye position is a different thing and
      // is a genuine field mark on most woodland ungulates.
      var dx = Math.abs(Math.abs(p.x) - Math.abs(eyeM.ctr.x));
      var dy = p.y - eyeM.ctr.y, dz = p.z - eyeM.ctr.z;
      var r = Math.sqrt(dy * dy + dz * dz + dx * dx * 0.25);
      col = MM.mix(col, C.ring, 0.55 * MM.smooth(3.4, 1.6, r));
      if (o.part === 'head_muzzle') {
        col = MM.mix(col, C.muzzle, 0.50 + 0.30 * MM.smooth(noseZ + 5.0, noseZ, p.z));
      }
      if (o.part === 'head_jaw') col = MM.mix(col, C.belly, 0.40);
    } else if (window.GIR_DISTAL[o.part]) {
      col = MM.mix(C.ground, C.belly, 0.35);
      col = MM.mix(col, C.dust, 0.45 * MM.smooth(13.0, 3.0, p.y));
    } else {
      col = MM.mix(C.belly, C.ground, MM.smooth(23.5, 30.0, p.y));
    }

    // ---------- dapples ----------
    // Cell 2.6 against the giraffe's 4.4, and mixed in at 0.30 rather than 1.0, so this reads
    // as soft dappling. Suppressed on the belly and below the knee, same as the giraffe's
    // patches -- dapples on a pale sock would just be noise.
    var amt = MM.smooth(11.0, 20.0, p.y) * (1 - 0.85 * MM.smooth(27.0, 23.6, p.y));
    if (window.GIR_HEAD[o.part]) amt *= 0.35;
    if (amt > 0.004) {
      var v = MM.vor(p, 2.6, 19);
      var lo = 0.14 + 0.16 * MM.dith(o.u, o.v, 4);
      var d = MM.smooth(lo, lo + 0.42, v.edge) * amt;
      col = MM.mix(col, MM.mix(C.dappleD, C.ground, 0.35 * v.cell), d * 0.30);
    }

    // ---------- the dorsal stripe ----------
    // Narrow, dithered and fading out at both ends. A hard pale dorsal line was rejected in
    // game once before as "an ugly white line", so this is a soft dark one with broken edges.
    var mid = 1 - MM.smooth(0.0, 2.4, Math.abs(p.x));
    var along = MM.smooth(-6.0, 2.0, p.z) * MM.smooth(24.0, 20.0, p.z);
    col = MM.mix(col, C.dorsal, 0.40 * mid * along
                 * (0.75 + 0.25 * MM.dith(o.u, o.v, 12)));

    // ---------- treatment: short fine coat ----------
    col = MM.mul(col, 1 + (MM.fbm(p, 0.95, 3, 2) - 0.5) * 0.10);
    col = MM.mul(col, 0.98 + 0.04 * MM.dith(o.u, o.v, 7));

    return MM.cl(window.girFaceLight(col, o.face));
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.save(window.GIR_SCRATCH + 'samotherium.png', cv);
  return 'samotherium painted, bled ' + MM.bledCount + ' seam texels';
})();
