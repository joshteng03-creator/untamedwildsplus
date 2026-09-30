// Sivatherium giganteum -- the heaviest giraffid that ever lived, and the one whose
// silhouette is already unmistakable because of the PALMATE ossicones. That means the skin's
// job is not to carry the identity but to avoid contradicting it: this has to look like a
// massive, thick-hided, short-necked browser, not like a giraffe in a different colour.
//
// Designed deliberately AGAINST the giraffe on every axis:
//   * NO patch network at all. The reticulation is the giraffe's; a faint large-scale
//     mottling is all this gets, and even that stays under 0.10 contrast.
//   * DARK slate-brown, where the giraffe is pale cream and chestnut.
//   * a HEAVY-HIDE treatment: coarse grizzle plus deep horizontal wrinkle folds over the
//     shoulder and haunch, where the giraffe's hide is smooth and sun-bleached.
//   * a pale grey FACE MASK and throat -- the one bright thing on the animal, so the head
//     still reads at distance against a dark body.
//   * the palmate ossicones painted as pale bone-keratin PLATES with dark rims, so the palms
//     read as flat spans rather than as two dark slabs lost against the skull.
(function () {
  var C = {
    // TRUE COOL SLATE, not the grey-BROWN of the first pass, and the reason is arithmetic.
    // Against the okapi this pair is the darkest in the set and it measured dE 7.3, then 8.7
    // after the okapi was darkened -- darkening the okapi barely moved its mean, because a
    // third of its opaque area is white bands, white socks and a pale face, which pin the
    // average up no matter what the body does. So the separation had to come from CHROMA
    // instead: the okapi is a warm chocolate (a* ~+11, b* ~+14) and this is now genuinely
    // neutral (a* ~0, b* ~+3), which opens the pair up without touching either lightness.
    // Lightening this species was the obvious alternative and was checked and rejected -- it
    // lands on palaeotragus at L* 40.2 (dE 6.3).
    //
    // It is also the better animal. Sivatherium is the most massive giraffid that ever lived,
    // and thick-hided megafauna read grey; this now sits beside helladotherium's pale grey-dun
    // as the second desaturated species, which is safe because the two are 22 apart in
    // lightness. Sharing a treatment axis is only a problem when lightness is shared too.
    dorsal : MM.hex('#33383a'),   // near-black cool slate along the topline
    flank  : MM.hex('#474e50'),
    ventral: MM.hex('#5e6668'),
    mask   : MM.hex('#9aa0a0'),   // pale grey face mask + throat
    muzzle : MM.hex('#3d4244'),
    mane   : MM.hex('#24292b'),   // dark crest on the short thick neck
    palm   : MM.hex('#b9a888'),   // bone-keratin ossicone plate
    palmRim: MM.hex('#5c4c3a'),
    tassel : MM.hex('#241d18'),
    hoof   : MM.hex('#241f1b')
  };

  eval(require('fs').readFileSync(window.SP + 'paint_giraffid_common.js', 'utf8'));
  MM.bounds();
  girEyeInit();
  var mz = MM.map['head_muzzle'], noseZ = mz.lo.z;
  var tas = MM.map['tail_tassel'];

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    var ec = window.girEye(o);
    if (ec) return ec;

    if (window.GIR_FOOT[o.part]) {
      col = MM.mix(C.hoof, C.flank, 0.20 * MM.fbm(p, 0.6, 3, 11));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }

    if (o.part === 'tail_tassel') {
      col = MM.mix(C.tassel, C.flank, 0.28 * MM.smooth(tas.lo.y, tas.hi.y, p.y));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 6)));
    }

    if (window.GIR_MANE[o.part]) {
      col = MM.mix(C.mane, C.dorsal, 0.24 * MM.dith(o.u, o.v, 9));
      return MM.cl(window.girFaceLight(col, o.face));
    }

    if (window.GIR_OSS[o.part]) {
      // The PALMS are this species' whole identity, so they get their own treatment: a pale
      // keratin plate with a darker rim. The rim is keyed on distance from the plate's own
      // centre in its OWN local box, not on a face index -- the palm carries 22 degrees of
      // roll and 14 of yaw, so no index axis runs along it on every face.
      var m = MM.map[o.part];
      var isPalm = (o.part === 'oss_palm_left' || o.part === 'oss_palm_right');
      var cy = (m.lo.y + m.hi.y) / 2, cz = (m.lo.z + m.hi.z) / 2;
      var ry = Math.abs(p.y - cy) / Math.max(0.6, (m.hi.y - m.lo.y) / 2);
      var rz = Math.abs(p.z - cz) / Math.max(0.6, (m.hi.z - m.lo.z) / 2);
      var rim = MM.smooth(0.55, 0.96, Math.max(ry, rz));
      col = isPalm ? MM.mix(C.palm, C.palmRim, rim)
                   : MM.mix(MM.mix(C.palm, C.palmRim, 0.45), C.dorsal, 0.30);
      return MM.cl(MM.mul(window.girFaceLight(col, o.face),
                          0.95 + 0.10 * MM.dith(o.u, o.v, 5)));
    }

    if (window.GIR_EAR[o.part]) {
      if (o.face === 'down') return MM.cl(MM.mul(C.mask, 0.92));
      col = MM.mix(C.flank, C.dorsal, 0.40);
      return MM.cl(MM.mul(window.girFaceLight(col, o.face),
                          0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- base coat: countershaded, dark ----------
    if (window.GIR_HEAD[o.part]) {
      // The pale mask sits over the cheeks and bridge and fades out toward the muzzle,
      // which stays dark. Keyed on world height so the jaw and the skull agree.
      var mask = MM.smooth(33.0, 39.5, p.y) * MM.smooth(noseZ - 1.0, noseZ + 7.0, p.z);
      col = MM.mix(C.flank, C.mask, 0.72 * mask);
      if (o.part === 'head_muzzle') {
        col = MM.mix(col, C.muzzle, 0.55 + 0.35 * MM.smooth(noseZ + 5.0, noseZ, p.z));
      }
    } else if (window.GIR_DISTAL[o.part]) {
      col = MM.mix(C.flank, C.dorsal, 0.30 + 0.40 * MM.smooth(16.0, 3.0, p.y));
    } else if (window.GIR_NECK[o.part]) {
      col = MM.mix(C.flank, C.dorsal, MM.smooth(34.0, 52.0, p.y));
      col = MM.mix(col, C.mask, 0.30 * MM.smooth(-8.0, -20.0, p.z) * MM.smooth(50.0, 38.0, p.y));
    } else {
      col = MM.mix(C.ventral, C.dorsal, MM.smooth(23.0, 37.0, p.y));
    }

    // ---------- treatment: thick hide, coarse grizzle + wrinkle folds ----------
    // The grizzle is agouti-style per-texel variance; the FOLDS are the part that makes this
    // read as a heavy animal rather than a dark one. Note the lesson from the tapir batch:
    // grizzling adds variance but does NOT move the mean, so the separation from the giraffe
    // is carried by the GROUND colour, and the treatment only has to stop it reading flat.
    var fold = Math.sin(p.y * 0.72 + MM.fbm(p, 0.30, 2, 23) * 3.4);
    col = MM.mul(col, 1 + 0.085 * fold * MM.smooth(21.0, 34.0, p.y));
    col = MM.mul(col, 1 + (MM.fbm(p, 1.05, 3, 2) - 0.5) * 0.15);
    col = MM.mul(col, 0.97 + 0.06 * MM.dith(o.u, o.v, 7));
    // Pin the saturation DOWN after all the mixing. Repeated mixes toward the warm keratin of
    // the ossicone plates creep the hue back up, and neutrality is now this species' separation
    // axis against the okapi -- so it has to be enforced at the end, not just chosen at the
    // start. Stronger than helladotherium's 0.72 because this animal is darker, and chroma
    // reads more strongly at low lightness.
    col = MM.sat(col, 0.55);

    return MM.cl(window.girFaceLight(col, o.face));
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.save(window.GIR_SCRATCH + 'sivatherium.png', cv);
  return 'sivatherium painted, bled ' + MM.bledCount + ' seam texels';
})();
