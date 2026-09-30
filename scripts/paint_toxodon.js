// Toxodon platensis -- swamp/jungle notoungulate.
// Surface TREATMENT (not just palette): thick sparsely-bristled rhino-like hide, broad
// shallow shoulder/haunch folds, dried ochre river silt booting the legs and splashed up
// the belly, a pale worn muzzle and jaw, cream chisel incisors.
(function () {
  var C = {
    dorsal : MM.hex('#574839'),
    flank  : MM.hex('#786752'),
    ventral: MM.hex('#92836f'),
    muzzle : MM.hex('#a3947e'),
    jaw    : MM.hex('#9a8b75'),
    ivory  : MM.hex('#d9ccb1'),
    horn   : MM.hex('#443a31'),
    silt   : MM.hex('#6d5a38'),
    earin  : MM.hex('#8a7460'),
    lid    : MM.hex('#6d5f4e'),
    pupil  : MM.hex('#191410'),
    glint  : MM.hex('#8e7f68')
  };

  MM.bounds();
  var LO = MM.LO, HI = MM.HI;

  // Countershading is stated over the BODY's vertical range, not the whole model's:
  // measured off the barrel so the legs cannot drag the gradient.
  var bb = MM.map['body_barrel'];
  var yb0 = bb.lo.y, yb1 = bb.hi.y;

  // SOP: the proximal limb segments take the BODY rule. On the leg rule the shoulder and
  // thigh -- the two biggest boxes on the flank -- paint out pale and the animal reads
  // two-tone.
  var BODYCLS = { body_barrel:1, body_withers:1, body_chest:1, body_croup:1, neck:1,
                  throat:1, head_skull:1, head_brow:1,
                  fore_left_shoulder:1, fore_right_shoulder:1,
                  hind_left_thigh:1, hind_right_thigh:1 };
  var DISTAL  = { fore_left_forearm:1, fore_right_forearm:1, fore_left_cannon:1,
                  fore_right_cannon:1, hind_left_gaskin:1, hind_right_gaskin:1,
                  hind_left_cannon:1, hind_right_cannon:1 };
  var FOOT    = { fore_left_foot:1, fore_right_foot:1, hind_left_foot:1, hind_right_foot:1 };

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    // ---------- eyes: zero-width planes ----------
    // A width-0 box draws TWO coplanar quads sampling opposite UV halves, so they z-fight
    // each other. Alpha-0 the inward one; entityCutoutNoCull discards it entirely.
    if (o.part === 'eye_left' || o.part === 'eye_right') {
      var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
      if (!outward) return [0, 0, 0, 0];
      // Pixel INDICES, not fractions: on a 3x3 face a fraction test overlaps and the
      // glint overwrites the pupil.
      // ONLY THE MIDDLE ROW IS DARK. A first pass darkened rows 1 AND 2 and the eye
      // rendered as a black slab covering a third of the skull -- the plane is 3u on an
      // 8u-deep skull, so all but one row of it has to read as ordinary hide.
      var hide = MM.mix(C.flank, C.dorsal, 0.55);
      if (o.j === 0) return MM.cl(hide);                       // brow skin above the eye
      if (o.j === 2) return MM.cl(MM.mul(hide, 0.86));         // shadow under the eye
      // Dim WARM catchlight, never white: at 1px on a 3px eye a white glint covers a
      // third of it and reads as a giant block.
      return (o.i === 2) ? MM.cl(C.glint) : MM.cl(C.pupil);
    }

    // ---------- incisors ----------
    if (o.part === 'incisor_lower' || o.part === 'incisor_upper') {
      var wear = MM.dith(o.u, o.v, 5) * 0.12;
      col = MM.mix(C.ivory, C.jaw, wear + 0.10 * MM.smooth(0, 1, o.j / Math.max(1, o.h - 1)));
      return MM.cl(col);
    }

    // ---------- feet: dark horn, mud-caked ----------
    if (FOOT[o.part]) {
      col = MM.mix(C.horn, C.silt, 0.35 + 0.25 * MM.fbm(p, 0.55, 3, 11));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }

    // ---------- base hide ----------
    var t;                                   // 0 = ventral, 1 = dorsal
    if (BODYCLS[o.part]) {
      t = MM.smooth(yb0, yb1, p.y);
      col = MM.mix(C.ventral, C.dorsal, t);
      col = MM.mix(col, C.flank, 0.45 * (1 - Math.abs(t - 0.5) * 2));
    } else if (DISTAL[o.part]) {
      t = 0.45;
      col = MM.mix(C.ventral, C.flank, 0.6);
    } else if (o.part === 'head_muzzle') {
      t = 0.5; col = C.muzzle.slice();
    } else if (o.part === 'head_jaw') {
      t = 0.35; col = C.jaw.slice();
    } else if (o.part === 'ear_left' || o.part === 'ear_right') {
      t = 0.8;
      col = (o.face === 'down') ? C.earin.slice() : MM.mix(C.flank, C.dorsal, 0.5);
    } else {
      t = MM.smooth(yb0, yb1, p.y);
      col = MM.mix(C.ventral, C.dorsal, t);
    }

    // Head blends toward the pale muzzle as it runs forward, so the skull does not end in
    // a hard colour seam at the muzzle joint.
    if (o.part === 'head_skull') {
      col = MM.mix(col, C.muzzle, 0.42 * MM.smooth(-14, -22, p.z));
    }

    // ---------- treatment: hide mottling + sparse bristle ----------
    // Low contrast on purpose. +-0.26 luminance speckle reads as TV static, not hide.
    var mot = MM.fbm(p, 0.30, 3, 2) - 0.5;
    col = MM.mul(col, 1 + mot * 0.20);
    var br = MM.dith(o.u, o.v, 7);
    if (br > 0.86) col = MM.mix(col, C.ventral, 0.22 * (br - 0.86) / 0.14);

    // ---------- broad shallow folds ----------
    // Keyed off face-local j, NOT global texture y: at constant world y the bands land at
    // a different height on every limb and read as haphazard.
    var fold = Math.sin((o.j / Math.max(1, o.h)) * Math.PI * 3.0
                        + MM.fbm(p, 0.22, 2, 4) * 5.0);
    var depth = BODYCLS[o.part] ? 0.085 : 0.05;
    col = MM.mul(col, 1 + fold * depth);

    // Heavier creasing over the shoulder and haunch, where a thick hide actually bunches.
    if (o.part === 'fore_left_shoulder' || o.part === 'fore_right_shoulder' ||
        o.part === 'hind_left_thigh' || o.part === 'hind_right_thigh' ||
        o.part === 'neck' || o.part === 'throat') {
      var cr = Math.sin((o.i / Math.max(1, o.w)) * Math.PI * 2.4 + MM.fbm(p, 0.3, 2, 6) * 4);
      col = MM.mul(col, 1 + cr * 0.06);
    }

    // ---------- river silt ----------
    // Height threshold jittered per texel column so the tide line is never a straight
    // horizontal band; strongest on the distal limbs, a light splash on the belly.
    var line = 8.5 + 2.6 * (MM.fbm(p, 0.35, 2, 9) - 0.5) * 2;
    if (p.y < line + 3) {
      var m = MM.smooth(line + 3, line - 3, p.y);
      if (DISTAL[o.part]) m = Math.min(1, m * 1.35 + 0.20);
      else m *= 0.45;
      m *= 0.55 + 0.45 * MM.fbm(p, 0.5, 3, 12);
      col = MM.mix(col, C.silt, Math.max(0, Math.min(0.8, m)));
    }

    // ---------- MC face lighting compensation ----------
    // MC bakes directional shading at render time (up brightest, down darkest), so a
    // topline painted merely "dark" still renders LIGHTER than the flanks it sits above.
    // Over-correct at the source rather than trusting the eye.
    if (o.face === 'up')   col = MM.mix(col, C.dorsal, 0.46);
    if (o.face === 'down') col = MM.mix(col, C.ventral, 0.30);

    return MM.cl(col);
  });

  // Seal the fractional-footprint seams. Protect the eye planes so their alpha-0'd
  // inward quad stays deleted.
  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

  MM.show(cv);
  return 'toxodon painted ' + Project.texture_width + 'x' + Project.texture_height
       + ', bled ' + MM.bledCount + ' seam texels';
})();
