// Tapirus indicus -- Malayan / Asian tapir. The largest living tapir and, by a wide
// margin, the most recognisable animal in this set.
//
// THE SADDLE IS THE WHOLE DESIGN. Front third (head, neck, shoulders, forelegs) BLACK;
// a broad pale saddle over the barrel, belly and rump; hind legs black again. It is
// disruptive camouflage -- in moonlight the animal reads as a rock rather than a body --
// and it is the one tapir pattern that survives at any distance or resolution.
//
// Deliberately opposite to lowland.png on every axis that matters:
//   lowland                          malayan
//   -----------------------------    -------------------------------------
//   near-uniform dark grey-brown     two-tone, near-black against near-white
//   erect black CREST                NO crest (showModel-gated off, variant 1)
//   uniform low-contrast treatment   a hard-edged, high-contrast marking
//
// The saddle edge is the one place in this whole batch where a HARD boundary is correct --
// on a real Malayan tapir it is knife-sharp. It still gets fbm jitter and dither breakup,
// though, because a mathematically straight edge across a blocky mesh reads as a decal.
// (A hard pale dorsal stripe on the aurochs was rejected in-game as "an ugly white line".)
(function () {
  var C = {
    black  : MM.hex('#241f1d'),   // front + limbs
    blackhi: MM.hex('#3a3330'),   // its lit faces, so the black is not a dead flat mass
    saddle : MM.hex('#cfc8bd'),   // the pale saddle
    saddled: MM.hex('#a49c92'),   // its shaded underside
    cheek  : MM.hex('#4a423e'),
    nose   : MM.hex('#1a1614'),
    earrim : MM.hex('#e2dbd0'),   // white ear rim -- shared by the whole genus
    earin  : MM.hex('#4e4642'),
    hoof   : MM.hex('#1c1817'),
    claw   : MM.hex('#b7ab95')
  };

  eval(require('fs').readFileSync(window.SP + 'paint_tapir_common.js', 'utf8'));
  MM.bounds();
  tapirEyeInit();

  var LEGS   = { fore_left_forearm:1, fore_right_forearm:1, fore_left_cannon:1,
                 fore_right_cannon:1, hind_left_gaskin:1, hind_right_gaskin:1,
                 hind_left_cannon:1, hind_right_cannon:1,
                 fore_left_shoulder:1, fore_right_shoulder:1 };
  var FOOT   = { fore_left_foot:1, fore_right_foot:1, hind_left_foot:1, hind_right_foot:1 };
  var HEADCL = { head_skull:1, head_muzzle:1, head_jaw:1, neck:1, body_chest:1 };

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    var ec = window.tapirEye(o);
    if (ec) return ec;

    // Hidden on this species (showModel off) but painted anyway: an unpainted rect is
    // fully transparent, and MM.bleed would dilate a neighbour's colour into it.
    if (o.part === 'claw_left' || o.part === 'claw_right') {
      return MM.cl(MM.mul(C.claw, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }
    if (o.part === 'crest') {
      return MM.cl(MM.mul(C.black, 0.96 + 0.08 * MM.dith(o.u, o.v, 9)));
    }

    if (FOOT[o.part]) {
      return MM.cl(MM.mul(C.hoof, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }
    if (o.part === 'proboscis_1' || o.part === 'proboscis_2') {
      return MM.cl(MM.mul(C.nose, 1 + (MM.fbm(p, 0.9, 2, 17) - 0.5) * 0.12));
    }
    if (o.part === 'ear_left' || o.part === 'ear_right') {
      if (o.face === 'down') return MM.cl(C.earin);
      var tip = MM.smooth(4.4, 5.4, Math.abs(p.x));
      col = MM.mix(C.black, C.earrim, tip);
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- the saddle mask ----------
    // Two independent terms, because the real marking is bounded BOTH ways:
    //   * fore edge, in z -- everything ahead of the shoulder is black
    //   * height, in y -- the legs are black even where they hang below a pale flank
    // The rump has no rear cutoff: on a Malayan tapir the pale runs all the way back over
    // the croup and only the hind legs go dark again.
    var edge = 1.6 * (MM.fbm(p, 0.42, 3, 23) - 0.5) * 2;      // jitter the boundary
    var fore = MM.smooth(-4.0 + edge, 1.0 + edge, p.z);
    var high = MM.smooth(7.0, 10.5, p.y);
    var pale = fore * high;
    if (HEADCL[o.part]) pale = 0;                             // head/neck/chest always dark
    if (LEGS[o.part])   pale = Math.min(pale, 0.10 * high);   // limbs stay dark

    // Dither the transition so the edge breaks up at texel scale instead of stepping.
    pale = Math.max(0, Math.min(1, pale + (MM.dith(o.u, o.v, 13) - 0.5) * 0.22));

    var dark  = MM.mix(C.black, C.blackhi, 0.35 + 0.30 * MM.fbm(p, 0.55, 3, 5));
    var light = MM.mix(C.saddled, C.saddle, MM.smooth(9.0, 19.0, p.y));
    col = MM.mix(dark, light, pale);

    if (o.part === 'head_jaw') col = MM.mix(col, C.cheek, 0.40);

    // ---------- treatment ----------
    // Low-contrast grain only. On a two-tone animal any real noise fights the marking,
    // which is the thing carrying the identity.
    col = MM.mul(col, 1 + (MM.fbm(p, 0.7, 3, 2) - 0.5) * 0.09);
    col = MM.mul(col, 0.98 + 0.04 * MM.dith(o.u, o.v, 7));

    // MULTIPLY, never mix toward a fixed colour -- mixing would drag the pale saddle
    // toward the black and destroy the contrast the whole design depends on.
    if (o.face === 'up')   col = MM.mul(col, 0.80);
    if (o.face === 'down') col = MM.mul(col, 1.16);

    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

  MM.show(cv);
  return 'malayan tapir painted ' + Project.texture_width + 'x' + Project.texture_height
       + ', bled ' + MM.bledCount + ' seam texels';
})();
