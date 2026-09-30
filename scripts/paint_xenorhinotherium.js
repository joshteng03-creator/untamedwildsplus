// Xenorhinotherium bahiense -- the northeastern Brazilian sibling, a caatinga animal:
// hot, dry, thorn-scrub, strongly seasonal. Smaller and more gracile than M. patachonica
// (scale 0.876 vs 1.018).
//
// Designed as a DELIBERATELY OPPOSITE animal, not a hue shift. A luminance-preserving
// recolour keeps the donor's texture signature, which is exactly what makes a variant
// read as a reskin (the aurochs / giant_buffalo / titanotylopus lesson). Six diagnostic
// cues are inverted against macrauchenia.png one by one:
//
//   macrauchenia (Patagonian steppe)      xenorhinotherium (caatinga)
//   -------------------------------      --------------------------------------
//   PALE GREY head, desaturated           DARK face MASK, near-black on the muzzle
//   sharp cream guanaco ventral LINE      soft diffuse counterhade, no line at all
//   warm tawny, saturated (L.51 S.32)     BLEACHED bone-sand (L.62 S.19)
//   dense clumped cold-steppe FUR         sleek sparse hot-climate coat + skin creasing
//   no dust                               heavy pale dust carried OVER THE BACK
//   plain topline                         dark dorsal eel-stripe down the spine
(function () {
  var C = {
    dorsal : MM.hex('#9c8b64'),   // olive-sand, dorsal
    flank  : MM.hex('#d3c5a1'),   // pale sand, flank
    ventral: MM.hex('#e9dec8'),   // soft pale belly -- NOT a hard cream line
    mask   : MM.hex('#413428'),   // dark face mask
    maskdk : MM.hex('#291f19'),   // near-black muzzle and proboscis
    stripe : MM.hex('#42341f'),   // dark dorsal eel-stripe
    dust   : MM.hex('#d3c4a6'),   // pale caatinga dust
    earin  : MM.hex('#a8926f'),
    hoof   : MM.hex('#3a332b'),
    pupil  : MM.hex('#140f0b'),
    glint  : MM.hex('#8f7c5c')
  };

  MM.bounds();
  var bb = MM.map['body_barrel'];
  var yb0 = bb.lo.y, yb1 = bb.hi.y;

  // SOP: the proximal limb segments take the BODY rule, or the shoulder and thigh -- the
  // two biggest boxes on the flank -- paint out pale and the animal reads two-tone.
  var BODYCLS = { body_barrel:1, body_withers:1, body_chest:1, body_croup:1,
                  fore_left_shoulder:1, fore_right_shoulder:1,
                  hind_left_thigh:1, hind_right_thigh:1,
                  tail_dock:1, tail_tip:1 };
  var NECK    = { neck_base:1, neck_mid:1, neck_top:1 };
  var HEAD    = { head_skull:1, head_muzzle:1, head_jaw:1, head_nasal:1 };
  var DISTAL  = { fore_left_forearm:1, fore_right_forearm:1, fore_left_cannon:1,
                  fore_right_cannon:1, hind_left_gaskin:1, hind_right_gaskin:1,
                  hind_left_cannon:1, hind_right_cannon:1 };
  var FOOT    = { fore_left_foot:1, fore_right_foot:1, hind_left_foot:1, hind_right_foot:1 };

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    // ---------- eyes ----------
    // Zero-width plane: two coplanar quads sampling opposite UV halves, so they z-fight
    // each other. Alpha-0 the inward one; entityCutoutNoCull discards it.
    // 2x2 texels per outward face, so the whole quad IS the eye. Pixel indices, never
    // fractions. A pale eye-ring would vanish into the dark mask, so the socket is simply
    // the mask itself -- which is how a masked face actually reads.
    if (o.part === 'eye_left' || o.part === 'eye_right') {
      var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
      if (!outward) return [0, 0, 0, 0];
      if (o.i === 1 && o.j === 0) return MM.cl(C.glint);
      return MM.cl(C.pupil);
    }

    if (FOOT[o.part]) {
      col = MM.mix(C.hoof, C.dust, 0.16 + 0.16 * MM.fbm(p, 0.55, 3, 11));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }

    // ---------- proboscis: near-black, the darkest thing on the animal ----------
    if (o.part === 'proboscis_1' || o.part === 'proboscis_2') {
      col = MM.mix(C.mask, C.maskdk, 0.45 + 0.55 * MM.smooth(-30, -35, p.z));
      return MM.cl(MM.mul(col, 1 + (MM.fbm(p, 0.9, 2, 17) - 0.5) * 0.14));
    }

    if (o.part === 'ear_left' || o.part === 'ear_right') {
      col = (o.face === 'down') ? C.earin.slice() : MM.mix(C.mask, C.flank, 0.35);
      return MM.cl(MM.mul(col, 0.95 + 0.10 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- the diagnostic nasal dome ----------
    // Kept PALE against the dark mask -- the inverse of macrauchenia, where it is pale
    // against pale grey. Same character, opposite contrast, so it reads on both animals.
    if (o.part === 'head_nasal') {
      col = MM.mix(C.mask, C.flank, 0.55);
      if (o.face === 'up') col = MM.mix(col, C.dust, 0.45);
      return MM.cl(MM.mul(col, 0.96 + 0.09 * MM.dith(o.u, o.v, 6)));
    }

    // ---------- base coat ----------
    if (HEAD[o.part]) {
      // The mask is heaviest on the muzzle and fades back toward the poll, so the head
      // does not end in a hard seam where it meets the neck.
      var back = MM.smooth(-33.0, -21.0, p.z);          // 1 at the nose, 0 at the poll
      col = MM.mix(MM.mix(C.flank, C.mask, 0.86), C.maskdk, 0.55 * back);
      if (o.part === 'head_jaw') col = MM.mix(col, C.flank, 0.22);
    } else if (NECK[o.part]) {
      // The mask bleeds a little way down the throat, then gives out.
      var g = MM.smooth(33.0, 25.0, p.y);
      col = MM.mix(C.flank, C.dorsal, 0.35);
      col = MM.mix(col, C.mask, 0.62 * g);
    } else if (DISTAL[o.part]) {
      // Legs stay body-toned and only lighten slightly, graded down the whole limb column
      // in world y so it is continuous across forearm -> cannon rather than restarting per
      // segment. Kept close to the flank tone on purpose -- pale SOCKS are
      // macrauchenia's marking, and once this animal's base coat was bleached to
      // L 0.61 the old 0.20+0.30 lightening reproduced them by accident.
      var down = MM.smooth(18.0, 3.0, p.y);
      col = MM.mix(C.flank, C.ventral, 0.08 + 0.14 * down);
    } else {
      col = MM.mix(C.ventral, C.dorsal, MM.smooth(yb0 - 1.5, yb1 + 1.0, p.y));
    }

    // ---------- dark dorsal eel-stripe ----------
    // Narrow, and blended + dithered at the edges. A hard pale dorsal stripe on the
    // aurochs was rejected in-game as "an ugly white line"; the accepted form is a
    // gaussian blend with hash-dithered edges, so this one is built the same way.
    if (BODYCLS[o.part] || NECK[o.part]) {
      var s = Math.exp(-Math.pow(Math.abs(p.x) / 2.1, 2.0))
              * MM.smooth(yb1 - 3.0, yb1 + 0.5, p.y);
      s *= 0.75 + 0.25 * MM.fbm(p, 0.8, 2, 31);
      col = MM.mix(col, C.stripe, 0.78 * s);
    }

    // ---------- treatment: sleek sparse hot-climate coat over creased skin ----------
    // No fur clumping at all -- that is the Patagonian animal's signature. Here the
    // surface is thin coat over skin, so the modulation is CREASES, not hair.
    var crease = Math.sin((o.j / Math.max(1, o.h)) * Math.PI * 4.0
                          + MM.fbm(p, 0.26, 2, 4) * 5.5);
    col = MM.mul(col, 1 + crease * (BODYCLS[o.part] ? 0.075 : 0.045));
    // Sun-bleaching: broad, low-frequency, and only ever lightens.
    col = MM.mix(col, C.dust, 0.08 * MM.smooth(0.45, 1.0, MM.fbm(p, 0.16, 3, 8)));
    col = MM.mul(col, 0.98 + 0.04 * MM.dith(o.u, o.v, 7));

    // ---------- caatinga dust ----------
    // Dust settles on the BACK, not only the belly -- restricting the wash to below the
    // belly line left namadicus with bare hide exactly where dust should be heaviest.
    // So: a dorsal fall plus a low splash, and nothing in between.
    var dorsalFall = MM.smooth(yb1 - 5.0, yb1 + 1.5, p.y) * 0.24;
    var lowSplash  = MM.smooth(14.0, 2.0, p.y) * 0.22;
    var dm = (dorsalFall + lowSplash) * (0.55 + 0.45 * MM.fbm(p, 0.42, 3, 12));
    if (o.face === 'up') dm *= 1.35;
    col = MM.mix(col, C.dust, Math.max(0, Math.min(0.34, dm)));

    // ---------- MC face lighting compensation ----------
    // MULTIPLY, never mix toward a fixed body colour: mixing drags every part's hue
    // toward it, which turned macrauchenia's grey muzzle brown on its top faces.
    if (o.face === 'up')   col = MM.mul(col, 0.74);
    if (o.face === 'down') col = MM.mul(col, 1.20);

    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

  MM.show(cv);
  return 'xenorhinotherium painted ' + Project.texture_width + 'x' + Project.texture_height
       + ', bled ' + MM.bledCount + ' seam texels';
})();
