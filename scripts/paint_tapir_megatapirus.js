// Megatapirus augustus -- the giant Pleistocene tapir of southern China. Roughly a third
// larger than any living tapir (1.30 m at the withers here) and the extinct headliner of
// the type.
//
// Nothing is known about its colour, so the design is reasoned from what IS known: a
// large-bodied tapir in cool subtropical woodland, with a notably heavier, deeper skull
// than the living species. That points at a temperate-forest animal rather than a
// rainforest one, so it gets the treatment no living tapir here has:
//
//   * GRIZZLED / AGOUTI pelage -- individual hairs banded light and dark, which reads as a
//     salt-and-pepper shimmer rather than a solid tone. This is the same cue that
//     distinguishes elasmotherium from the woolly rhino elsewhere in the mod, and it is
//     the one surface treatment none of the six other tapirs uses.
//   * a PALE COLD grey-fawn ground (L 0.46, S 0.07 -- nearly neutral). The first pass
//     sat at L 0.357 / S 0.087, and measured only 16.8 mean RGB distance from
//     lowland.png -- squarely inside the 10-20 band that means "hue shift", i.e.
//     exactly the reskin problem this whole set is built to avoid. Grizzling adds
//     per-texel variance but does not move the MEAN, so the ground itself had to go.
//   * a dark dorsal cape over the shoulders and back, fading out on the flank -- mass
//     reads as bulk, and this is what makes it look like the big one
//   * heavier, blunter extremities: a broad dark muzzle and thick dark legs
//   * the genus-wide white ear rim, kept, plus a modest crest
(function () {
  var C = {
    dorsal : MM.hex('#605a50'),   // cold grey-fawn, dorsal
    flank  : MM.hex('#8c8477'),
    ventral: MM.hex('#a49a8c'),
    grizz  : MM.hex('#b2a999'),   // the pale band of an agouti hair
    grizzd : MM.hex('#4b463f'),   // its dark band
    cape   : MM.hex('#332f2a'),   // dark shoulder/back cape
    cheek  : MM.hex('#a89e90'),
    crest  : MM.hex('#2f2b26'),
    nose   : MM.hex('#312d28'),
    earrim : MM.hex('#e0d8ca'),
    earin  : MM.hex('#6b6157'),
    hoof   : MM.hex('#2a2622'),
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

  // Agouti banding. HIGH-frequency and PER-TEXEL, because the effect is individual hairs
  // banded along their length -- a low-frequency term would give patches, which is the
  // wrong cue entirely (that is mountain's fleece). Two uncorrelated hashes are combined so
  // the result does not fall into the visible diagonal lattice a single dither produces.
  function agouti(o, p) {
    var a = MM.dith(o.u, o.v, 3);
    var b = MM.dith(o.u * 1.7 + 11.3, o.v * 0.61 + 5.9, 17);
    var band = (a * 0.62 + b * 0.38);
    // Modulate the banding density slowly over the body so it is not uniform static.
    var dens = 0.55 + 0.45 * MM.fbm(p, 0.22, 2, 41);
    return (band - 0.5) * dens;
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
    if (o.part === 'proboscis_1' || o.part === 'proboscis_2') {
      // Broad and blunt: dark and nearly ungrizzled, so the trunk reads as bare skin
      // against a grizzled coat rather than as more fur.
      col = MM.mix(C.dorsal, C.nose, 0.55 + 0.45 * MM.smooth(-19, -24, p.z));
      return MM.cl(MM.mul(col, 1 + (MM.fbm(p, 0.9, 2, 17) - 0.5) * 0.12));
    }
    if (o.part === 'crest') {
      return MM.cl(MM.mul(C.crest, 0.96 + 0.10 * MM.dith(o.u, o.v, 9)));
    }
    if (o.part === 'ear_left' || o.part === 'ear_right') {
      if (o.face === 'down') return MM.cl(C.earin);
      var tip = MM.smooth(4.4, 5.4, Math.abs(p.x));
      col = MM.mix(C.dorsal, C.earrim, tip);
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- base coat ----------
    if (HEAD[o.part]) {
      col = MM.mix(C.flank, C.cheek, 0.40 * MM.smooth(17.5, 12.0, p.y));
      if (o.part === 'head_muzzle') col = MM.mix(col, C.nose, 0.45 * MM.smooth(-17, -23, p.z));
    } else if (DISTAL[o.part]) {
      // Thick and dark: heavy limbs are part of reading as the big species.
      col = MM.mix(C.flank, C.dorsal, 0.45 + 0.35 * MM.smooth(11.0, 2.0, p.y));
    } else {
      col = MM.mix(C.ventral, C.dorsal, MM.smooth(yb0 - 1.0, yb1 + 0.5, p.y));
      col = MM.mix(col, C.flank, 0.35);
    }

    // ---------- the dark cape ----------
    // Over the shoulders and back, fading out down the flank and back along the body. Bulk
    // is the whole point of this species, and a dark dorsal mass is what reads as bulk.
    var cape = MM.smooth(yb1 - 6.0, yb1 + 1.0, p.y) * MM.smooth(12.0, -2.0, p.z);
    cape *= 0.85 + 0.15 * MM.fbm(p, 0.22, 2, 13);   // smoother: at 0.35/0.25 it blotched
    col = MM.mix(col, C.cape, 0.72 * cape);

    // ---------- treatment: GRIZZLED ----------
    // Applied as a mix toward two BANDED colours rather than as a brightness wobble --
    // agouti hair is light-and-dark along its length, so the pale and dark bands are
    // different colours, not one colour at two exposures.
    // CAP 0.18, NOT 0.42, and the two band colours sit close to the ground rather than
    // far from it. At 0.42 with widely-separated bands this rendered as blocky CAMO,
    // not fur -- the same failure the first elasmotherium pass hit at +-0.26 luminance
    // speckle. Grizzling has to be a shimmer you notice second, not a pattern first.
    var g = agouti(o, p);
    col = MM.mix(col, g > 0 ? C.grizz : C.grizzd, Math.min(0.18, Math.abs(g) * 0.7));
    col = MM.mul(col, 1 + (MM.fbm(p, 0.5, 3, 2) - 0.5) * 0.10);

    if (o.face === 'up')   col = MM.mul(col, 0.76);
    if (o.face === 'down') col = MM.mul(col, 1.18);

    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

  MM.show(cv);
  return 'megatapirus painted ' + Project.texture_width + 'x' + Project.texture_height
       + ', bled ' + MM.bledCount + ' seam texels';
})();
