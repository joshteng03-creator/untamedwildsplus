// Okapia johnstoni -- the forest giraffe, and the only living giraffid other than Giraffa.
// The one species on this rig whose markings are STRUCTURALLY different from everything else:
// horizontal white BANDS, not patches. That makes it the easiest of the seven to separate and
// the most important not to get lazy with.
//
// Diagnostic cues:
//   * deep chocolate body with a faint purplish-red sheen (real okapi hide is oily and almost
//     iridescent in sunlight)
//   * WHITE HORIZONTAL STRIPES on the hindquarters and the tops of all four legs -- the field
//     mark. Near-vertical over the haunch, turning to horizontal rings down the legs.
//   * WHITE lower legs below the stripes, with a dark ring just above the hoof
//   * a pale grey-buff FACE with large dark eyes and a black muzzle
//   * short dark ossicones (male-only in life; modelled at SPIKE @0.50 for all of them)
//
// TREATMENT: short, dense, sleek rainforest coat. Almost no grain -- this animal is glossy,
// not shaggy -- so the surface interest is entirely the band field plus a soft sheen.
(function () {
  var C = {
    // DARKENED from #4b2a1c after measuring. The first pass landed at mean L* 29.0 against
    // sivatherium's 31.8 -- dE 7.3, inside the "same animal in two files" band -- and the two
    // are the darkest pair in the set, so one of them had to move. It is this one, for two
    // reasons: a real okapi is very dark, nearly black-chocolate in shade, so darkening is a
    // move toward accuracy rather than away from it; and lightening sivatherium instead would
    // have walked it straight into palaeotragus at L* 40.2 (dE 6.3). Colour space on a shared
    // rig is a PACKING problem -- moving one species has to be checked against all the others,
    // not just against the one it collided with.
    body   : MM.hex('#351b11'),   // very deep chocolate
    dorsal : MM.hex('#20100a'),
    sheen  : MM.hex('#5c2f1e'),   // the reddish sheen on lit surfaces
    stripe : MM.hex('#e6ded0'),   // the white bands
    sock   : MM.hex('#ded5c4'),   // white lower legs
    ankle  : MM.hex('#33241c'),   // dark ring above the hoof
    face   : MM.hex('#a9977e'),   // pale grey-buff face
    muzzle : MM.hex('#2a211b'),   // black muzzle
    mane   : MM.hex('#2d1d15'),
    ossicone: MM.hex('#4a3729'),
    tuft   : MM.hex('#241a13'),
    tassel : MM.hex('#241a13'),
    hoof   : MM.hex('#241f1b')
  };

  eval(require('fs').readFileSync(window.SP + 'paint_giraffid_common.js', 'utf8'));
  MM.bounds();
  girEyeInit();
  var mz = MM.map['head_muzzle'], noseZ = mz.lo.z;
  var tas = MM.map['tail_tassel'];

  // The banded regions: hindquarters and the PROXIMAL limb segments. The forequarters, barrel
  // and neck stay plain chocolate, which is correct and is also what keeps the stripes reading
  // as a marking rather than as all-over camouflage.
  var HAUNCH = { body_croup: 1, hind_left_thigh: 1, hind_right_thigh: 1 };
  var RINGED = { hind_left_gaskin: 1, hind_right_gaskin: 1,
                 fore_left_shoulder: 1, fore_right_shoulder: 1,
                 fore_left_forearm: 1, fore_right_forearm: 1 };

  // TWO KEYS, NOT ONE, and this is the whole trick. A single band axis cannot serve both
  // regions: bands perpendicular to z read as near-vertical stripes over the haunch (right),
  // but on a vertical leg the same axis produces stripes running DOWN the leg (wrong -- leg
  // markings are rings). So the haunch keys on z with a slight y lean, and the limbs key on
  // world y, which gives true horizontal rings on every limb at the same heights.
  //
  // Keying the rings on WORLD y rather than face-local j is deliberate and is the opposite of
  // the SOP's warning, which is about global TEXTURE y (the UV row) -- that lands the bands at
  // a different height on every limb. World y IS height, so it lands them all at the same one.
  //
  // Period 4.0 units. The real bands are 4-8 cm and one mesh unit is 3.9 cm at this species'
  // scale, so a truthful band would be under 2 texels including its gap and would alias into
  // grey mush. 4.0 gives a ~2-texel band and a ~2-texel gap, the tightest that still reads.
  var band = function (t, u, v) {
    var s = 0.5 + 0.5 * Math.sin(t * 2 * Math.PI / 4.0);
    return MM.smooth(0.40 + 0.12 * MM.dith(u, v, 4), 0.78, s);
  };

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    var ec = window.girEye(o);
    if (ec) return ec;

    if (window.GIR_FOOT[o.part]) {
      col = MM.mix(C.hoof, C.sock, 0.18 * MM.fbm(p, 0.6, 3, 11));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }

    if (o.part === 'tail_tassel') {
      col = MM.mix(C.tassel, C.body, 0.26 * MM.smooth(tas.lo.y, tas.hi.y, p.y));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 6)));
    }

    if (window.GIR_MANE[o.part]) {
      col = MM.mix(C.mane, C.dorsal, 0.24 * MM.dith(o.u, o.v, 9));
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
      // Okapi ears are enormous and pale inside -- a real and very visible feature.
      if (o.face === 'down') return MM.cl(MM.mul(C.face, 0.96));
      col = MM.mix(C.body, C.dorsal, 0.35);
      return MM.cl(MM.mul(window.girFaceLight(col, o.face),
                          0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- base coat ----------
    if (window.GIR_HEAD[o.part]) {
      col = MM.mix(C.body, C.face, 0.78 * MM.smooth(32.5, 39.0, p.y));
      if (o.part === 'head_muzzle') {
        col = MM.mix(col, C.muzzle, 0.60 + 0.35 * MM.smooth(noseZ + 5.5, noseZ, p.z));
      }
      if (o.part === 'head_jaw') col = MM.mix(col, C.muzzle, 0.45);
    } else if (window.GIR_DISTAL[o.part]) {
      // White socks below the band field, with a dark ankle ring above the hoof.
      var sock = MM.smooth(11.5, 6.5, p.y);
      col = MM.mix(C.body, C.sock, sock);
      col = MM.mix(col, C.ankle, 0.75 * MM.smooth(6.0, 3.4, p.y));
    } else {
      col = MM.mix(C.body, C.dorsal, MM.smooth(26.0, 40.0, p.y));
    }

    // ---------- the band field ----------
    if (HAUNCH[o.part]) {
      // Keyed on HEIGHT with a slight rearward droop, not on z: a real okapi's hindquarter
      // stripes run near-horizontally across the rump and thigh, and the first z-keyed pass
      // came out as diagonal slashes. The top face stays dark -- the rump top is plain in life,
      // and a constant-y face would otherwise just be one flat stripe or one flat gap.
      if (o.face !== 'up') {
        col = MM.mix(col, C.stripe, band(p.y + 0.10 * p.z, o.u, o.v) * 0.92);
      }
    } else if (RINGED[o.part]) {
      // Rings fade out downward so they hand over to the white sock rather than fighting it.
      col = MM.mix(col, C.stripe, band(p.y, o.u, o.v) * 0.88 * MM.smooth(12.0, 18.0, p.y));
    }

    // ---------- treatment: sleek glossy forest coat ----------
    // Very low grain. The sheen is a broad low-frequency lift toward the reddish tone on
    // upward-facing surfaces, which is what makes the hide look oiled rather than matte.
    col = MM.mix(col, C.sheen, 0.16 * MM.smooth(0.1, 0.9, MM.fbm(p, 0.35, 2, 13)));
    col = MM.mul(col, 1 + (MM.fbm(p, 0.85, 3, 2) - 0.5) * 0.085);
    col = MM.mul(col, 0.985 + 0.03 * MM.dith(o.u, o.v, 7));

    return MM.cl(window.girFaceLight(col, o.face));
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.save(window.GIR_SCRATCH + 'okapi.png', cv);
  return 'okapi painted, bled ' + MM.bledCount + ' seam texels';
})();
