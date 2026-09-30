// Helladotherium duvernoyi -- a large HORNLESS giraffid from the Late Miocene of Greece and
// Asia. It is the only species on this rig with no ossicones at all (family NONE), which makes
// its silhouette the cleanest and its skin the most exposed: there is no headgear to look at,
// so if the hide is dull the whole animal is dull.
//
// Diagnostic cues, chosen to hold a colour slot no sibling occupies:
//   * COOL GREY-DUN, and the only DESATURATED animal in the set. Every other giraffid here is
//     a warm colour -- chestnut, slate-brown, chocolate, sandy, rufous -- so pulling the
//     saturation right down is a separation axis of its own rather than another hue shift.
//   * a pale FACE and throat with a distinctly DARKER CROWN, which is what gives a hornless
//     skull something to read as. This is the same trick elasmotherium uses in this mod: when
//     the species flag removes the signature feature, the paint has to carry the identity.
//   * a soft dark shoulder shadow and a faint dark dorsal wash
//   * pale legs with clean dark hooves
//
// TREATMENT: short smooth hide with fine, almost invisible grain, plus broad soft tonal
// blocking. Deliberately the LEAST textured skin in the set -- bramatherium is the shaggiest,
// this is the smoothest, and the two are the pair most at risk of colliding on lightness.
(function () {
  var C = {
    body   : MM.hex('#8d8677'),   // cool grey-dun
    dorsal : MM.hex('#655f54'),
    crown  : MM.hex('#4b463e'),   // the darker crown -- the identity on a hornless skull
    ventral: MM.hex('#bdb6a4'),
    face   : MM.hex('#b6afa0'),
    muzzle : MM.hex('#57514a'),
    mane   : MM.hex('#4f483f'),
    ossicone: MM.hex('#8b8474'),  // never rendered on this species; painted for atlas hygiene
    tassel : MM.hex('#2f2b26'),
    hoof   : MM.hex('#2b2823')
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
      col = MM.mix(C.hoof, C.ventral, 0.20 * MM.fbm(p, 0.6, 3, 11));
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

    // showModel hides every ossicone on this species, so none of these texels ever render.
    // They are painted anyway: an unpainted rect is fully transparent and MM.bleed would
    // dilate a neighbour's colour into it, corrupting whichever part is packed alongside.
    if (window.GIR_OSS[o.part]) {
      col = MM.mix(C.ossicone, C.dorsal, 0.35);
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    if (window.GIR_EAR[o.part]) {
      if (o.face === 'down') return MM.cl(MM.mul(C.ventral, 0.95));
      col = MM.mix(C.body, C.dorsal, 0.30);
      return MM.cl(MM.mul(window.girFaceLight(col, o.face),
                          0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- base coat: pale face, dark crown ----------
    if (window.GIR_HEAD[o.part]) {
      col = MM.mix(C.face, C.body, 0.30);
      // The crown darkens sharply above the eye line. On a hornless skull this is the only
      // thing giving the head structure, so it is stronger than a countershading gradient
      // would be -- 0.85 rather than the ~0.4 used elsewhere.
      col = MM.mix(col, C.crown, 0.85 * MM.smooth(37.5, 40.5, p.y));
      if (o.part === 'head_muzzle') {
        col = MM.mix(col, C.muzzle, 0.50 + 0.30 * MM.smooth(noseZ + 5.0, noseZ, p.z));
      }
      if (o.part === 'head_jaw') col = MM.mix(col, C.ventral, 0.45);
    } else if (window.GIR_DISTAL[o.part]) {
      col = MM.mix(C.body, C.ventral, 0.45 + 0.30 * MM.smooth(16.0, 5.0, p.y));
    } else if (window.GIR_NECK[o.part]) {
      col = MM.mix(C.body, C.dorsal, 0.30 * MM.smooth(36.0, 54.0, p.y));
      col = MM.mix(col, C.ventral, 0.30 * MM.smooth(-6.0, -18.0, p.z));
    } else {
      col = MM.mix(C.ventral, C.dorsal, MM.smooth(23.0, 36.5, p.y));
      col = MM.mix(col, C.body, 0.42);
      // Soft shoulder shadow: broad, low-contrast tonal blocking rather than a marking.
      col = MM.mul(col, 1 - 0.07 * MM.smooth(2.0, -9.0, p.z) * MM.smooth(22.0, 30.0, p.y));
    }

    // ---------- treatment: smooth short hide, minimal grain ----------
    // The lowest-contrast treatment in the set on purpose. Keep the saturation pinned DOWN
    // after all the mixing, because repeated mixes toward warm tones creep the hue back up and
    // the cool desaturation is this animal's only separation axis.
    col = MM.sat(col, 0.72);
    col = MM.mul(col, 1 + (MM.fbm(p, 0.75, 3, 2) - 0.5) * 0.065);
    col = MM.mul(col, 0.99 + 0.02 * MM.dith(o.u, o.v, 7));

    return MM.cl(window.girFaceLight(col, o.face));
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.save(window.GIR_SCRATCH + 'helladotherium.png', cv);
  return 'helladotherium painted, bled ' + MM.bledCount + ' seam texels';
})();
