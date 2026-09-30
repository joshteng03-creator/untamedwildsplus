// Bramatherium perimense -- a heavy, short-necked Siwalik giraffid (Late Miocene, India) and
// the only species on this rig with FOUR ossicones: a massive pair set close on the midline
// and angled hard outward, plus a small supraorbital pair in front. The head silhouette alone
// identifies it, so the skin's job is to look like a stocky woodland browser and to take a
// colour slot nobody else holds.
//
// Diagnostic cues:
//   * WARM RUFOUS RED-BROWN, the only genuinely red animal in the set. The hue is the
//     separation: chestnut (giraffe), slate (sivatherium), chocolate (okapi), sandy
//     (samotherium) and grey-dun (helladotherium) all sit elsewhere on the wheel.
//   * a dark NECK CAPE -- a blackish shawl over the neck and shoulders fading into the body.
//     Extant heavy bovids and Siwalik reconstructions both carry this, and it makes the short
//     thick neck read as deliberately short rather than as a giraffe's neck cut off.
//   * pale keratin on all four ossicones, so the paired horns read against a dark crown
//   * pale ventral and inner legs, dark cannons
//
// TREATMENT: coarse dense hair with visible clumping -- a much harder, shaggier grain than
// the giraffe's smooth hide or the okapi's gloss. No spots, no stripes, no dapples: this is
// the plain one, and it is allowed to be plain because its head is not.
(function () {
  var C = {
    body   : MM.hex('#8a4520'),   // warm rufous
    dorsal : MM.hex('#653015'),
    cape   : MM.hex('#3a2117'),   // dark neck/shoulder cape
    ventral: MM.hex('#b98253'),
    face   : MM.hex('#9e5b32'),
    muzzle : MM.hex('#4a3226'),
    mane   : MM.hex('#2e1a12'),
    ossicone: MM.hex('#bfa77f'),  // pale keratin
    tuft   : MM.hex('#5a4530'),
    tassel : MM.hex('#2a1a12'),
    hoof   : MM.hex('#2b221c')
  };

  eval(require('fs').readFileSync(window.SP + 'paint_giraffid_common.js', 'utf8'));
  MM.bounds();
  girEyeInit();
  var mz = MM.map['head_muzzle'], noseZ = mz.lo.z;
  var tas = MM.map['tail_tassel'];

  // The cape covers the neck and the front of the barrel. Keyed on world z so it crosses the
  // neck/withers/chest boundary as one continuous shawl instead of stopping at a box edge --
  // which is exactly what world-space painting is for.
  var capeAt = function (p) {
    return MM.smooth(4.0, -10.0, p.z) * MM.smooth(24.0, 32.0, p.y);
  };

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    var ec = window.girEye(o);
    if (ec) return ec;

    if (window.GIR_FOOT[o.part]) {
      col = MM.mix(C.hoof, C.body, 0.18 * MM.fbm(p, 0.6, 3, 11));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }

    if (o.part === 'tail_tassel') {
      col = MM.mix(C.tassel, C.dorsal, 0.28 * MM.smooth(tas.lo.y, tas.hi.y, p.y));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 6)));
    }

    if (window.GIR_MANE[o.part]) {
      col = MM.mix(C.mane, C.cape, 0.30 * MM.dith(o.u, o.v, 9));
      return MM.cl(window.girFaceLight(col, o.face));
    }

    if (window.GIR_OSS[o.part]) {
      // Pale keratin along the shaft with a duller base. The small anterior pair is left
      // paler overall so the FOUR read as four rather than merging into the crown.
      var m = MM.map[o.part];
      var up = MM.smooth(m.lo.y, m.hi.y, p.y);
      var front = (o.part.indexOf('front') >= 0);
      col = MM.mix(MM.mix(C.tuft, C.ossicone, front ? 0.85 : 0.55), C.ossicone, up * 0.6);
      return MM.cl(MM.mul(window.girFaceLight(col, o.face),
                          0.95 + 0.10 * MM.dith(o.u, o.v, 5)));
    }

    if (window.GIR_EAR[o.part]) {
      if (o.face === 'down') return MM.cl(MM.mul(C.ventral, 0.94));
      col = MM.mix(C.body, C.cape, 0.45);
      return MM.cl(MM.mul(window.girFaceLight(col, o.face),
                          0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- base coat ----------
    if (window.GIR_HEAD[o.part]) {
      col = MM.mix(C.face, C.cape, 0.35 * MM.smooth(36.0, 41.0, p.y));
      if (o.part === 'head_muzzle') {
        col = MM.mix(col, C.muzzle, 0.55 + 0.30 * MM.smooth(noseZ + 5.0, noseZ, p.z));
      }
      if (o.part === 'head_jaw') col = MM.mix(col, C.ventral, 0.30);
    } else if (window.GIR_DISTAL[o.part]) {
      col = MM.mix(C.body, C.ventral, 0.30);
      col = MM.mix(col, C.dorsal, 0.45 * MM.smooth(14.0, 3.0, p.y));
    } else if (window.GIR_NECK[o.part]) {
      col = MM.mix(C.body, C.cape, 0.80);
    } else {
      col = MM.mix(C.ventral, C.dorsal, MM.smooth(23.5, 36.0, p.y));
      col = MM.mix(col, C.body, 0.45);
      col = MM.mix(col, C.cape, 0.70 * capeAt(p));
    }

    // ---------- treatment: coarse clumped hair ----------
    // Two octaves at different scales: a broad clump field plus a tight grain inside it. This
    // is the hardest, shaggiest treatment in the set and it is what stops a plain rufous animal
    // reading as moulded plastic across the big flank faces.
    var clump = MM.fbm(p, 0.45, 2, 31);
    col = MM.mul(col, 1 + (clump - 0.5) * 0.16);
    col = MM.mul(col, 1 + (MM.fbm(p, 1.6, 2, 5) - 0.5) * 0.13 * (0.5 + clump));
    col = MM.mul(col, 0.97 + 0.06 * MM.dith(o.u, o.v, 7));

    return MM.cl(window.girFaceLight(col, o.face));
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.save(window.GIR_SCRATCH + 'bramatherium.png', cv);
  return 'bramatherium painted, bled ' + MM.bledCount + ' seam texels';
})();
