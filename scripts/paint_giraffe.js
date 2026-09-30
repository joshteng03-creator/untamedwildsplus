// Giraffa camelopardalis -- the type species, and the baseline the other six are designed
// AGAINST.
//
// Diagnostic cues, all of which have to survive being ~90 pixels tall:
//   * THE RETICULATED PATCH NETWORK. Irregular chestnut polygons separated by narrow cream
//     lines, running from the head down the neck and body onto the upper legs. Nothing else
//     about the skin matters as much; if this reads, the animal reads.
//   * PATCHES STOP AT THE KNEE. Below roughly the carpus and hock a giraffe's legs are plain
//     pale cream. This is a real and very visible break, and it is what stops the patch field
//     turning the legs into noise at this resolution.
//   * A WHITE BELLY, unpatterned.
//   * a short dark chestnut MANE along the dorsal neck crest
//   * dark tufted OSSICONE tips -- bare-topped in old bulls, tufted otherwise
//   * a long black TAIL TASSEL
//   * a pale grey-tan muzzle and pale eye surrounds
//
// TREATMENT: a smooth, tight, sun-bleached hide. Very little hair texture -- a giraffe is
// not a shaggy animal -- so the surface interest comes almost entirely from the patch field
// and from per-patch tone variation, plus a faint dorsal sun-bleach. Keep the added noise
// LOW: hard speckle over a hard-edged pattern reads as TV static, and the patch edges are
// already doing all the high-frequency work this skin can afford.
(function () {
  var C = {
    patch  : MM.hex('#7d4a1e'),   // chestnut patch interior
    patchLo: MM.hex('#5c3112'),   // the darkest patches
    cream  : MM.hex('#e2d3ac'),   // the reticulation lines and the ground colour
    lower  : MM.hex('#ded0ae'),   // plain pale lower legs, below the knee
    belly  : MM.hex('#e6dabb'),   // white belly
    mane   : MM.hex('#5a3417'),   // short dark chestnut crest
    muzzle : MM.hex('#a4917a'),   // pale grey-tan muzzle
    ossicone: MM.hex('#ab9370'),
    tuft   : MM.hex('#33251a'),   // the dark ossicone tuft
    tassel : MM.hex('#241b13'),   // black tail tassel
    hoof   : MM.hex('#2c2521')
  };

  eval(require('fs').readFileSync(window.SP + 'paint_giraffid_common.js', 'utf8'));
  MM.bounds();
  girEyeInit();

  var mz = MM.map['head_muzzle'], msk = MM.map['head_skull'];
  var noseZ = mz.lo.z, skullZ = msk.hi.z;
  var tas = MM.map['tail_tassel'];

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    // ---------- eyes: the type's standard art ----------
    var ec = window.girEye(o);
    if (ec) return ec;

    // ---------- the patch field ----------
    // patchAmt is how strongly the reticulation shows at this point. It is a pure function
    // of WORLD HEIGHT, which is what makes one rule cover the knee break, the belly and the
    // face at once.
    //   * fades out between y 19 and y 9 -- the carpus/hock break. Below it the leg is plain.
    //   * fades out on the underside of the barrel -- the white belly.
    var patchAmt = MM.smooth(9.0, 19.0, p.y);
    if (window.GIR_BODY[o.part]) {
      patchAmt *= 1 - 0.90 * MM.smooth(26.8, 23.4, p.y);
    }
    // The face carries only faint patching; a giraffe's head is much plainer than its neck.
    if (window.GIR_HEAD[o.part]) patchAmt *= 0.42;
    if (window.GIR_FOOT[o.part] || window.GIR_MANE[o.part] || window.GIR_OSS[o.part]) {
      patchAmt = 0;
    }

    var patch = 0, tone = 0;
    if (patchAmt > 0.004) {
      var v = MM.vor(p, 4.4, 5);
      // THE THRESHOLD IS A NARROW BAND JUST ABOVE ZERO, and getting this wrong the first
      // time inverted the whole animal. `edge` = d2 - d1 is ZERO exactly on a cell boundary
      // and grows toward the cell CENTRE, peaking around 2.0 for this cell size. The first
      // pass ramped patch over 0.30..1.45, which made only the small core of each cell
      // chestnut and left everything else cream -- so the giraffe came out as sparse isolated
      // spots on a pale ground, the exact inverse of the real animal, which is chestnut
      // almost everywhere with NARROW cream lines between. Ramping over a ~0.55-wide band
      // just above 0 puts cream only within about half a unit of a boundary, i.e. a 1-texel
      // line, and leaves the rest of the cell as patch.
      //
      // The band is DITHERED rather than fixed. A constant threshold gives a vector-crisp
      // boundary, and hard-edged markings have been rejected on this codebase before ("an
      // ugly white line"); jittering it by a fraction of a texel breaks the edge up so it
      // reads as a hide marking rather than as printed geometry.
      var lo = 0.18 + 0.20 * MM.dith(o.u, o.v, 4);
      patch = MM.smooth(lo, lo + 0.55, v.edge) * patchAmt;
      tone = v.cell;
    }

    // ---------- per-part base ----------
    if (window.GIR_FOOT[o.part]) {
      col = MM.mix(C.hoof, C.lower, 0.22 * MM.fbm(p, 0.6, 3, 11));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }

    if (o.part === 'tail_tassel') {
      // The tassel is black and it is a real field mark, so it is painted flat and dark with
      // only enough variation to stop it reading as a hole. Graded at the very top so it
      // does not meet the patched dock as a hard line.
      col = MM.mix(C.tassel, C.patchLo, 0.30 * MM.smooth(tas.lo.y, tas.hi.y, p.y));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 6)));
    }

    if (window.GIR_MANE[o.part]) {
      col = MM.mix(C.mane, C.patchLo, 0.26 * MM.dith(o.u, o.v, 9));
      return MM.cl(window.girFaceLight(col, o.face));
    }

    if (window.GIR_OSS[o.part]) {
      // Pale shaft, dark tuft on the distal end. Keyed on world height rather than on a
      // face-local index: the ossicones of the four families carry up to 24 degrees of roll
      // and 16 of yaw, so no single index axis points along the horn on all of its faces,
      // and the palmate palm is a different shape from the rest entirely.
      var m = MM.map[o.part];
      var up = MM.smooth(m.lo.y + (m.hi.y - m.lo.y) * 0.45, m.hi.y, p.y);
      col = MM.mix(C.ossicone, C.tuft, up);
      col = MM.mix(col, C.patch, 0.22 * (1 - up));
      return MM.cl(MM.mul(window.girFaceLight(col, o.face),
                          0.95 + 0.10 * MM.dith(o.u, o.v, 5)));
    }

    if (window.GIR_EAR[o.part]) {
      // Pale inner surface, patched outer. `down` is the inward-facing quad on this rig.
      if (o.face === 'down') return MM.cl(MM.mul(C.cream, 0.94));
      col = MM.mix(C.cream, MM.mix(C.patch, C.patchLo, tone), patch * 0.7);
      return MM.cl(MM.mul(window.girFaceLight(col, o.face),
                          0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // Ground colour under the patches.
    if (window.GIR_DISTAL[o.part]) {
      col = MM.mix(C.lower, C.cream, 0.5);
    } else if (window.GIR_HEAD[o.part]) {
      col = MM.mix(C.cream, C.muzzle, 0.34);
      // The muzzle and the lower jaw go pale grey-tan, and the very tip paler still.
      if (o.part === 'head_muzzle') {
        col = MM.mix(col, C.muzzle, 0.55 + 0.35 * MM.smooth(noseZ + 5.0, noseZ, p.z));
      }
      if (o.part === 'head_jaw') col = MM.mix(col, C.muzzle, 0.42);
    } else if (window.GIR_BODY[o.part]) {
      col = MM.mix(C.cream, C.belly, MM.smooth(26.8, 23.2, p.y));
    } else {
      col = C.cream;
    }

    // Lay the patches over the ground.
    col = MM.mix(col, MM.mix(C.patch, C.patchLo, tone), patch);

    // ---------- treatment: smooth sun-bleached hide ----------
    // Low contrast on purpose. The patch field is already the high-frequency content; adding
    // real hair grain on top of it turns the whole flank into speckle at this texel density.
    col = MM.mul(col, 1 + (MM.fbm(p, 0.55, 3, 2) - 0.5) * 0.075);
    col = MM.mul(col, 0.985 + 0.03 * MM.dith(o.u, o.v, 7));
    // Faint dorsal bleach -- the topline takes the sun on an animal this tall.
    col = MM.mul(col, 1 + 0.05 * MM.smooth(30.0, 44.0, p.y));

    return MM.cl(window.girFaceLight(col, o.face));
  });

  // Seal the fractional-footprint seams. Protect the eye planes so their deliberately
  // alpha-0'd inward quad is not refilled, which would bring the z-fight straight back.
  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

  MM.show(cv);
  return 'giraffe painted ' + Project.texture_width + 'x' + Project.texture_height
       + ', bled ' + MM.bledCount + ' seam texels';
})();
