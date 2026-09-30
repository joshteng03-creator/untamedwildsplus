// Tapirus pinchaque -- mountain / woolly tapir. Andean paramo and cloud forest, 2000-4500 m,
// and the only tapir adapted to real cold. Smallest of the seven at 0.80 m.
//
// The identity is the COAT, not the colour. This animal is the only one in the set with a
// thick woolly pelage -- the others are all short-haired tropical or temperate species --
// so the treatment carries the species and the palette only supports it:
//
//   * WOOL, not hair. Big soft clumps at low frequency plus a fine curl at high frequency,
//     roughly triple the amplitude of lowland's flat rainforest coat. Deliberately the
//     coarsest surface in the batch.
//     NB the ground had to be LIFTED from L 0.23 to ~0.30 for any of this to be
//     visible: a real mountain tapir is nearly black, but at that value the fleece
//     clumps carry no contrast and the whole treatment -- the thing that IS the
//     species -- simply disappears into a flat dark mass.
//   * a near-black brown ground, so individual clumps catch light and read as fleece
//   * THE WHITE LIP RING -- a clean pale band right around the mouth, the field mark that
//     names the animal in every guide
//   * white ear rims, larger and more contrasty than the other species carry
//   * NO warm tone at all (S 0.10, H shoved cold): against bairds' rufous this is the
//     coldest, darkest animal here, which is also what a high-altitude coat actually looks
//     like
(function () {
  var C = {
    dorsal : MM.hex('#3f3733'),   // near-black brown
    flank  : MM.hex('#564b45'),
    ventral: MM.hex('#665a52'),
    wool   : MM.hex('#83766c'),   // the lit face of a wool clump
    lip    : MM.hex('#ded6c8'),   // THE white lip ring
    crest  : MM.hex('#241f1c'),
    nose   : MM.hex('#241e1b'),
    earrim : MM.hex('#e6ded1'),
    earin  : MM.hex('#4f453f'),
    hoof   : MM.hex('#221c19'),
    claw   : MM.hex('#b7ab95')
  };

  eval(require('fs').readFileSync(window.SP + 'paint_tapir_common.js', 'utf8'));
  MM.bounds();
  tapirEyeInit();
  var bb = MM.map['body_barrel'];
  var yb0 = bb.lo.y, yb1 = bb.hi.y;

  var HEAD   = { head_skull:1, head_muzzle:1, head_jaw:1 };
  var DISTAL = { fore_left_forearm:1, fore_right_forearm:1, fore_left_cannon:1,
                 fore_right_cannon:1, hind_left_gaskin:1, hind_right_gaskin:1,
                 hind_left_cannon:1, hind_right_cannon:1 };
  var FOOT   = { fore_left_foot:1, fore_right_foot:1, hind_left_foot:1, hind_right_foot:1 };

  // Wool: two octave bands multiplied together. One alone gives either blotches (low) or
  // static (high); the product gives clumps WITH internal texture, which is what fleece
  // actually looks like. Kept as a single helper so the body, head and legs all wear the
  // same coat rather than three different ones.
  function fleece(p) {
    var clump = MM.fbm(p, 0.34, 3, 2) - 0.5;      // big soft masses
    var curl  = MM.fbm(p, 1.35, 2, 6) - 0.5;      // fine crimp inside them
    return clump * 0.30 + curl * 0.16 + clump * curl * 0.5;
  }

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    var ec = window.tapirEye(o);
    if (ec) return ec;

    if (o.part === 'claw_left' || o.part === 'claw_right') {
      return MM.cl(MM.mul(C.claw, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }
    if (FOOT[o.part]) {
      return MM.cl(MM.mul(C.hoof, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }
    if (o.part === 'crest') {
      col = MM.mix(C.crest, C.wool, 0.30 * (0.5 + fleece(p)));
      return MM.cl(col);
    }
    if (o.part === 'ear_left' || o.part === 'ear_right') {
      if (o.face === 'down') return MM.cl(C.earin);
      // Wider rim than the other species: the mountain tapir's ear edging is broad and
      // very high-contrast against a near-black coat.
      var tip = MM.smooth(4.0, 5.2, Math.abs(p.x));
      col = MM.mix(C.dorsal, C.earrim, tip);
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- the white lip ring ----------
    // A BAND round the mouth, not a patch on one face -- so it is defined by distance from
    // the mouth opening in world space and applies to the muzzle, jaw and trunk tip alike.
    // A face-local rule would break the ring at every box seam.
    var lipd = Math.sqrt(Math.pow(p.z + 23.6, 2) + Math.pow((p.y - 12.6) * 1.15, 2));
    var lip = MM.smooth(4.8, 2.2, lipd);   // radius: at 3.4/1.4 the ring was a
                                       // 3-texel smudge hidden behind the trunk
    lip *= 0.80 + 0.20 * MM.fbm(p, 0.9, 2, 29);          // break the ring up slightly

    if (o.part === 'proboscis_1' || o.part === 'proboscis_2') {
      col = MM.mix(C.nose, C.lip, 0.90 * lip);
      return MM.cl(MM.mul(col, 1 + fleece(p) * 0.5));
    }

    // ---------- base coat ----------
    if (HEAD[o.part]) {
      col = MM.mix(C.flank, C.dorsal, 0.35);
      col = MM.mix(col, C.lip, 0.92 * lip);
    } else if (DISTAL[o.part]) {
      col = MM.mix(C.flank, C.dorsal, 0.40 + 0.35 * MM.smooth(11.0, 2.0, p.y));
    } else {
      col = MM.mix(C.ventral, C.dorsal, MM.smooth(yb0 - 1.0, yb1 + 0.5, p.y));
      col = MM.mix(col, C.flank, 0.35);
    }

    // ---------- treatment: WOOL ----------
    // The identity of this species. Amplitude is roughly 3x lowland's, and clumps are
    // lightened toward C.wool rather than just brightened, so the fleece has its own
    // colour rather than reading as noise on a flat ground.
    var fl = fleece(p);
    col = MM.mix(col, C.wool, Math.max(0, fl) * 1.25);
    col = MM.mul(col, 1 + fl * 0.55);
    col = MM.mul(col, 0.97 + 0.06 * MM.dith(o.u, o.v, 7));

    if (o.face === 'up')   col = MM.mul(col, 0.76);
    if (o.face === 'down') col = MM.mul(col, 1.18);

    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

  MM.show(cv);
  return 'mountain tapir painted ' + Project.texture_width + 'x' + Project.texture_height
       + ', bled ' + MM.bledCount + ' seam texels';
})();
