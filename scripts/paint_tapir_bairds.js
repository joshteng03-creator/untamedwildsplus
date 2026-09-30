// Tapirus bairdii -- Baird's tapir. The largest land mammal in Central America.
//
// The hard case of this set: it is a plain brown tapir sharing a rig with another plain
// brown tapir (lowland). A hue shift would be indefensible here, so it is separated on
// treatment and on markings that lowland deliberately does NOT have:
//
//   lowland (T. terrestris)             bairds (T. bairdii)
//   --------------------------------    ------------------------------------------
//   cool dark GREY-brown (H27)          warm RUFOUS brown (H21), clearly warmer
//   paler cheeks, no sharp boundary     a broad CREAM MASK over cheeks, throat and
//                                       lips, with a definite edge -- the field mark
//   tall black crest                    a LOW crest, dark brown rather than black
//   uniform low-contrast grain          coarser, clumpier hair with a dark dorsal
//                                       saddle shading the topline
//
// The cream face is the real diagnostic: Baird's tapir has a pale creamy-white mask over
// the cheeks, jaw and throat that stops in a fairly clean line behind the eye, and a dark
// spot behind each eye. That is what makes it identifiable in a photograph, so it is what
// has to carry at 20 pixels.
(function () {
  var C = {
    dorsal : MM.hex('#54402f'),   // warm rufous, dorsal
    flank  : MM.hex('#6d543d'),
    ventral: MM.hex('#7d6349'),
    cream  : MM.hex('#c9b795'),   // THE cream face mask
    creamd : MM.hex('#a8967a'),   // its shaded side
    spot   : MM.hex('#3a2c20'),   // the dark spot behind the eye
    crest  : MM.hex('#3d2e21'),   // low, dark BROWN crest -- not lowland's black
    nose   : MM.hex('#33271d'),
    earrim : MM.hex('#ddd2ba'),
    earin  : MM.hex('#6f5741'),
    hoof   : MM.hex('#2c221a'),
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

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    var ec = window.tapirEye(o);
    if (ec) return ec;

    if (o.part === 'claw_left' || o.part === 'claw_right') {
      return MM.cl(MM.mul(C.claw, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }
    if (FOOT[o.part]) {
      col = MM.mix(C.hoof, C.flank, 0.16 * MM.fbm(p, 0.6, 3, 11));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }
    if (o.part === 'proboscis_1' || o.part === 'proboscis_2') {
      // The pale mask runs onto the upper lip, so the trunk is NOT uniformly dark the way
      // lowland's is -- it grades from dark at the base to cream at the lip.
      col = MM.mix(C.nose, C.cream, 0.55 * MM.smooth(-19, -24, p.z));
      return MM.cl(MM.mul(col, 1 + (MM.fbm(p, 0.9, 2, 17) - 0.5) * 0.14));
    }
    if (o.part === 'crest') {
      // Low and brown. Graded down toward the base so it reads as a ridge of hair rather
      // than the hard black blade lowland carries.
      col = MM.mix(C.dorsal, C.crest, MM.smooth(0, 1, o.j / Math.max(1, o.h - 1)));
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 9)));
    }
    if (o.part === 'ear_left' || o.part === 'ear_right') {
      if (o.face === 'down') return MM.cl(C.earin);
      var tip = MM.smooth(4.4, 5.4, Math.abs(p.x));
      col = MM.mix(MM.mix(C.dorsal, C.crest, 0.4), C.earrim, tip);
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- base coat ----------
    if (HEAD[o.part]) {
      // THE CREAM MASK. Bounded in z (it stops behind the eye) and jittered, so it reads
      // as a marking with an edge rather than as a gradient -- that boundary is the whole
      // point, since a soft fade would just look like lowland's paler cheeks.
      var jit = 1.2 * (MM.fbm(p, 0.5, 3, 19) - 0.5) * 2;
      var mask = MM.smooth(-14.5 + jit, -18.0 + jit, p.z);
      mask *= MM.smooth(18.8, 15.5, p.y);           // top of the skull stays brown
      col = MM.mix(C.flank, C.cream, 0.92 * mask);
      if (o.part === 'head_jaw') col = MM.mix(col, C.cream, 0.55);
      // Dark spot behind the eye -- small, and only where the mask has already stopped.
      var d = Math.exp(-Math.pow((p.z + 13.6) / 1.9, 2)) * Math.exp(-Math.pow((p.y - 16.6) / 1.9, 2));
      col = MM.mix(col, C.spot, 0.60 * d * (1 - mask));
    } else if (DISTAL[o.part]) {
      col = MM.mix(C.flank, C.dorsal, 0.30 + 0.40 * MM.smooth(11.0, 2.0, p.y));
    } else {
      col = MM.mix(C.ventral, C.dorsal, MM.smooth(yb0 - 1.0, yb1 + 0.5, p.y));
      col = MM.mix(col, C.flank, 0.38);
      // Dark dorsal saddle: shades the topline so the back reads rounder than lowland's.
      col = MM.mix(col, C.dorsal, 0.45 * MM.smooth(yb1 - 4.0, yb1 + 1.0, p.y));
    }

    // ---------- treatment: coarser, clumpier hair than lowland ----------
    // Same family of noise, roughly double the amplitude and a coarser scale. This is the
    // axis that stops two brown tapirs reading as one animal recoloured.
    col = MM.mul(col, 1 + (MM.fbm(p, 0.40, 3, 2) - 0.5) * 0.22);
    var hair = MM.dith(o.u, o.v, 7);
    if (hair > 0.80) col = MM.mix(col, C.dorsal, 0.20 * (hair - 0.80) / 0.20);
    col = MM.mul(col, 0.975 + 0.05 * MM.dith(o.u, o.v, 11));

    if (o.face === 'up')   col = MM.mul(col, 0.74);
    if (o.face === 'down') col = MM.mul(col, 1.20);

    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

  MM.show(cv);
  return 'bairds tapir painted ' + Project.texture_width + 'x' + Project.texture_height
       + ', bled ' + MM.bledCount + ' seam texels';
})();
