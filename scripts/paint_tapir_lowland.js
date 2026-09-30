// Tapirus terrestris -- lowland / Brazilian tapir. The type species, and the baseline the
// other six are designed AGAINST.
//
// Diagnostic cues, all of which have to survive being ~20 pixels tall:
//   * a near-uniform dark grey-brown coat -- adults are famously plain (the stripes and
//     spots belong to calves), so the identity has to come from the extremities, not the flank
//   * WHITE-RIMMED EARS. This is the single most reliable tapir field mark and it is shared
//     by the whole genus, so every Tapirus skin here paints it and palorchestes does not.
//   * a black erect CREST running the neck
//   * paler grey cheeks and throat
//   * darker legs and a dark proboscis
//
// TREATMENT: short dense rainforest coat -- fine tight grain, very low contrast, faint
// clumping. No dust, no bleaching, no wrinkles: this animal lives wet.
//
// Palette lifted once after measuring: the first pass sat at median L 0.32 and read as
// a black silhouette in the viewport. Blockbench's preview lighting runs dark, so the
// check has to be a numeric sample of the canvas, not the render -- three species in an
// earlier batch were repainted for exactly this reason.
(function () {
  var C = {
    dorsal : MM.hex('#5b5148'),   // dark grey-brown, dorsal
    flank  : MM.hex('#6f6359'),   // barely lighter -- the coat really is near-uniform
    ventral: MM.hex('#7e7167'),
    cheek  : MM.hex('#8f8377'),   // paler grey cheeks + throat
    crest  : MM.hex('#2b2521'),   // black erect mane
    nose   : MM.hex('#2a2521'),   // dark proboscis
    earrim : MM.hex('#ddd6ca'),   // THE white ear rim
    earin  : MM.hex('#6e6157'),
    hoof   : MM.hex('#2e2823'),
    claw   : MM.hex('#b7ab95'),
    pupil  : MM.hex('#120f0c'),
    glint  : MM.hex('#7d6f58')
  };

  eval(require('fs').readFileSync(window.SP + 'paint_tapir_common.js', 'utf8'));
  MM.bounds();
  tapirEyeInit();
  var bb = MM.map['body_barrel'];
  var yb0 = bb.lo.y, yb1 = bb.hi.y;

  // SOP: the proximal limb segments take the BODY rule, or the shoulder and thigh -- the
  // two biggest boxes on the flank -- paint out pale and the animal reads two-tone.
  var BODYCLS = { body_barrel:1, body_withers:1, body_chest:1, body_croup:1,
                  fore_left_shoulder:1, fore_right_shoulder:1,
                  hind_left_thigh:1, hind_right_thigh:1, tail:1 };
  var HEAD    = { head_skull:1, head_muzzle:1, head_jaw:1 };
  var DISTAL  = { fore_left_forearm:1, fore_right_forearm:1, fore_left_cannon:1,
                  fore_right_cannon:1, hind_left_gaskin:1, hind_right_gaskin:1,
                  hind_left_cannon:1, hind_right_cannon:1 };
  var FOOT    = { fore_left_foot:1, fore_right_foot:1, hind_left_foot:1, hind_right_foot:1 };

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    // ---------- eyes: the user's art, stamped verbatim ----------
    var ec = window.tapirEye(o);
    if (ec) return ec;

    // ---------- palorchestes claws ----------
    // showModel-gated OFF for this species, so these texels never render -- but they are
    // painted anyway. An unpainted rect is fully transparent, and MM.bleed would then
    // dilate neighbouring colour into it and quietly corrupt whichever part is packed
    // alongside; filling it keeps the atlas well-defined.
    if (o.part === 'claw_left' || o.part === 'claw_right') {
      return MM.cl(MM.mul(C.claw, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }

    if (FOOT[o.part]) {
      col = MM.mix(C.hoof, C.flank, 0.18 * MM.fbm(p, 0.6, 3, 11));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }

    if (o.part === 'proboscis_1' || o.part === 'proboscis_2') {
      col = MM.mix(C.dorsal, C.nose, 0.5 + 0.5 * MM.smooth(-19, -24, p.z));
      return MM.cl(MM.mul(col, 1 + (MM.fbm(p, 0.9, 2, 17) - 0.5) * 0.14));
    }

    if (o.part === 'crest') {
      col = MM.mix(C.crest, C.dorsal, 0.22 * MM.dith(o.u, o.v, 9));
      return MM.cl(col);
    }

    // ---------- ears: the white TIP ----------
    // A white RIM is what the real animal has, but it does not survive this resolution: an
    // ear side face is only about 3x2 texels, so an "edge ring" test marks EVERY texel on
    // it and the whole ear paints out white. Grade the distal end instead -- at 20 pixels
    // a white-tipped ear is what a white-rimmed ear looks like anyway.
    // Keyed on |p.x| (distance out from the midline) rather than face-local indices,
    // because the ear carries 38 degrees of roll and 14 of yaw, so no single index axis
    // points along the ear on all of its faces.
    if (o.part === 'ear_left' || o.part === 'ear_right') {
      if (o.face === 'down') return MM.cl(C.earin);
      var tip = MM.smooth(4.4, 5.4, Math.abs(p.x));
      col = MM.mix(MM.mix(C.dorsal, C.crest, 0.35), C.earrim, tip);
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- base coat ----------
    if (HEAD[o.part]) {
      // Cheeks and throat lighten toward the jaw; the muzzle darkens toward the trunk.
      var low = MM.smooth(17.5, 11.5, p.y);
      col = MM.mix(C.flank, C.cheek, 0.55 * low);
      if (o.part === 'head_muzzle') col = MM.mix(col, C.nose, 0.40 * MM.smooth(-17, -23, p.z));
      if (o.part === 'head_jaw')    col = MM.mix(col, C.cheek, 0.35);
    } else if (DISTAL[o.part]) {
      // Legs stay DARK and get darker downward -- the opposite of the macrauchenia's pale
      // guanaco socks, and correct for a forest tapir.
      col = MM.mix(C.flank, C.dorsal, 0.35 + 0.45 * MM.smooth(11.0, 2.0, p.y));
    } else {
      col = MM.mix(C.ventral, C.dorsal, MM.smooth(yb0 - 1.0, yb1 + 0.5, p.y));
      col = MM.mix(col, C.flank, 0.40);
    }

    // ---------- treatment: short dense wet-forest coat ----------
    // Deliberately very low contrast. This animal is near-uniform in life, so the job of
    // the treatment is to stop large faces reading as flat plastic, not to add pattern.
    col = MM.mul(col, 1 + (MM.fbm(p, 0.65, 3, 2) - 0.5) * 0.13);
    col = MM.mul(col, 0.975 + 0.05 * MM.dith(o.u, o.v, 7));

    // ---------- MC face lighting compensation ----------
    // MULTIPLY, never mix toward a fixed body colour: mixing drags every part's hue toward
    // it, which turned a grey muzzle brown on the macrauchenia.
    if (o.face === 'up')   col = MM.mul(col, 0.74);
    if (o.face === 'down') col = MM.mul(col, 1.20);

    return MM.cl(col);
  });

  // Seal the fractional-footprint seams. Protect the eye planes so their deliberately
  // alpha-0'd inward quad is not refilled, which would bring the z-fight straight back.
  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

  MM.show(cv);
  return 'lowland tapir painted ' + Project.texture_width + 'x' + Project.texture_height
       + ', bled ' + MM.bledCount + ' seam texels';
})();
