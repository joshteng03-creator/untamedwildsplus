// Palaeotragus rouenii -- a Late Miocene giraffid, okapi-like in build and one of the
// ancestral stock the whole family radiates from. Medium neck, small backswept spike
// ossicones (SPIKE @0.75).
//
// THIS IS SPECIES SEVEN, AND IT GOES STRUCTURAL. That is not a stylistic choice, it is forced
// arithmetic, and the tapir batch learned it the hard way: by the sixth species on a shared rig
// the plain-brown niche is FULL, and vero_tapir could not be separated on colour from any
// sibling at all -- 22.1 from lowland, 28.5 from bairds, everything inside the 10-20 "this is
// just a hue shift" band or barely outside it. It had to be given a different STRUCTURE.
// Palaeotragus is in exactly that position here, and worse: it shares the spike ossicone
// family with okapi AND samotherium, so it cannot lean on headgear either.
//
// So the identity is a hard TWO-TONE SPLIT along the body: dark chocolate-grey forequarters --
// head, neck, shoulders, chest -- handing over across the barrel to a pale sandy-buff rear.
// No sibling has a front-to-back division of any kind, which means this one is separable at a
// glance even if its mean colour lands between two of them, and it survives being 30 pixels
// tall because the boundary is the largest feature on the animal.
//
// It is also a real pattern rather than an invention: contrasting fore and hind quarters occur
// in bontebok, blesbok, gemsbok and gerenuk, and Palaeotragus reconstructions frequently show
// a darker neck and forehand than body.
//
// TREATMENT: medium-length coat with soft clumping -- between the okapi's gloss and
// bramatherium's shag -- plus a faint mottle on the dark forehand only.
(function () {
  var C = {
    fore   : MM.hex('#4e3a2c'),   // dark chocolate-grey forequarters
    foreDk : MM.hex('#382920'),
    rear   : MM.hex('#bda175'),   // pale sandy-buff hindquarters
    rearDk : MM.hex('#96794f'),
    ventral: MM.hex('#cdb994'),
    face   : MM.hex('#9c8b74'),   // pale face against the dark neck
    muzzle : MM.hex('#332820'),
    mane   : MM.hex('#2a1e17'),
    ossicone: MM.hex('#7d6a52'),
    tuft   : MM.hex('#2e231a'),
    tassel : MM.hex('#2c2119'),
    hoof   : MM.hex('#2a231d')
  };

  eval(require('fs').readFileSync(window.SP + 'paint_giraffid_common.js', 'utf8'));
  MM.bounds();
  girEyeInit();
  var mz = MM.map['head_muzzle'], noseZ = mz.lo.z;
  var tas = MM.map['tail_tassel'];

  // THE SPLIT. `foreness` is 1 on the forequarters and 0 on the hindquarters, keyed on world z
  // so it crosses chest -> withers -> barrel -> croup as one continuous boundary regardless of
  // which box a texel belongs to. The transition is 9 units wide and dithered, because a hard
  // line here would be the single most conspicuous "ugly line" the codebase has ever been
  // asked to reject -- it runs right across the widest part of the animal.
  var foreness = function (p, u, v) {
    var t = MM.smooth(5.5, -3.5, p.z);
    var d = (MM.dith(u, v, 21) - 0.5) * 0.22 + (MM.fbm(p, 0.55, 2, 41) - 0.5) * 0.30;
    return Math.max(0, Math.min(1, t + d * MM.smooth(0.02, 0.5, t * (1 - t) * 4)));
  };

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    var ec = window.girEye(o);
    if (ec) return ec;

    if (window.GIR_FOOT[o.part]) {
      col = MM.mix(C.hoof, C.ventral, 0.18 * MM.fbm(p, 0.6, 3, 11));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }

    if (o.part === 'tail_tassel') {
      col = MM.mix(C.tassel, C.rearDk, 0.30 * MM.smooth(tas.lo.y, tas.hi.y, p.y));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 6)));
    }

    if (window.GIR_MANE[o.part]) {
      col = MM.mix(C.mane, C.foreDk, 0.28 * MM.dith(o.u, o.v, 9));
      return MM.cl(window.girFaceLight(col, o.face));
    }

    if (window.GIR_OSS[o.part]) {
      var m = MM.map[o.part];
      var up = MM.smooth(m.lo.y + (m.hi.y - m.lo.y) * 0.40, m.hi.y, p.y);
      col = MM.mix(C.ossicone, C.tuft, up);
      return MM.cl(MM.mul(window.girFaceLight(col, o.face),
                          0.95 + 0.10 * MM.dith(o.u, o.v, 5)));
    }

    if (window.GIR_EAR[o.part]) {
      if (o.face === 'down') return MM.cl(MM.mul(C.face, 0.96));
      col = MM.mix(C.fore, C.foreDk, 0.35);
      return MM.cl(MM.mul(window.girFaceLight(col, o.face),
                          0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- base coat ----------
    if (window.GIR_HEAD[o.part]) {
      // A PALE face on a dark neck -- the second half of the two-tone idea, and what keeps the
      // head from disappearing into the forehand at distance.
      col = MM.mix(C.fore, C.face, 0.80 * MM.smooth(33.0, 39.0, p.y));
      if (o.part === 'head_muzzle') {
        col = MM.mix(col, C.muzzle, 0.55 + 0.30 * MM.smooth(noseZ + 5.0, noseZ, p.z));
      }
      if (o.part === 'head_jaw') col = MM.mix(col, C.face, 0.40);
    } else if (window.GIR_NECK[o.part]) {
      col = MM.mix(C.fore, C.foreDk, 0.40 * MM.smooth(36.0, 56.0, p.y));
    } else if (window.GIR_DISTAL[o.part]) {
      // The forelegs belong to the dark half and the hind legs to the pale half, which makes
      // the split read from the front and the rear as well as from the side.
      var fl = (o.part.indexOf('fore_') === 0) ? 1 : 0;
      col = fl ? MM.mix(C.fore, C.foreDk, 0.30 + 0.35 * MM.smooth(15.0, 4.0, p.y))
               : MM.mix(C.rear, C.ventral, 0.35 + 0.25 * MM.smooth(15.0, 4.0, p.y));
    } else {
      var fw = foreness(p, o.u, o.v);
      var rearCol = MM.mix(C.ventral, C.rearDk, MM.smooth(23.5, 35.0, p.y));
      rearCol = MM.mix(rearCol, C.rear, 0.45);
      var foreCol = MM.mix(C.fore, C.foreDk, MM.smooth(24.0, 36.0, p.y));
      col = MM.mix(rearCol, foreCol, fw);
    }

    // ---------- treatment: medium coat, mottle on the dark half only ----------
    // Restricting the mottle to the forehand is deliberate: it gives the two halves different
    // SURFACE as well as different tone, so the split survives even in shadow where the
    // lightness difference compresses.
    var fw2 = window.GIR_NECK[o.part] || window.GIR_HEAD[o.part] ? 1 : foreness(p, o.u, o.v);
    col = MM.mul(col, 1 + (MM.fbm(p, 0.80, 3, 7) - 0.5) * (0.09 + 0.09 * fw2));
    col = MM.mul(col, 0.98 + 0.04 * MM.dith(o.u, o.v, 7));

    return MM.cl(window.girFaceLight(col, o.face));
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.save(window.GIR_SCRATCH + 'palaeotragus.png', cv);
  return 'palaeotragus painted, bled ' + MM.bledCount + ' seam texels';
})();
