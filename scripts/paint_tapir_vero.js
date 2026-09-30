// Tapirus veroensis -- the Vero tapir, North America, extinct ~11 ka. A temperate-zone
// tapir: it ranged well north of the tropics, through what is now the southeastern USA and
// as far as Pennsylvania, so it lived with a real winter.
//
// The design problem is the same one bairds posed -- another brown tapir -- and it is
// solved on a different axis again. Every other species in this set is separated by a
// MARKING (malayan's saddle, bairds' cream mask, mountain's lip ring) or by a TREATMENT
// (mountain's fleece, megatapirus' grizzle). This one is separated by SEASONAL COAT
// STRUCTURE, which none of the others have:
//
//   * a visible WINTER COAT BREAK -- a longer, paler, slightly shaggy guard layer over the
//     shoulders, neck and rump, against shorter darker hair on the flank and legs. Real
//     temperate ungulates do not wear one uniform coat, and the boundary between the two
//     is a diagnostic nobody else here uses.
//   * a pale FROSTED muzzle and brow, the way temperate mammals grey around the face
//   * a DARK russet ground under a distinctly PALE tan guard layer. The first pass made
//     the break a subtle 0.50 mix on a mid-brown ground and measured only 22.1 from
//     lowland and 28.5 from bairds -- too close on both, because by this point the
//     plain-brown niche is genuinely crowded (lowland cool grey-brown, bairds rufous,
//     mountain near-black, megatapirus pale grey-fawn). The fix was to stop competing
//     on colour and make the coat break STRUCTURAL: dark head, throat, lower flank and
//     legs against a pale shaggy back. That is a light/dark division like malayan's,
//     but horizontal rather than fore-and-aft, and soft-edged rather than knife-sharp.
//   * the genus-wide white ear rim and a moderate crest
(function () {
  var C = {
    dorsal : MM.hex('#4a382a'),   // warm mid russet, dorsal
    flank  : MM.hex('#63503c'),
    ventral: MM.hex('#8a7259'),
    winter : MM.hex('#b79e77'),   // the longer, paler winter guard layer
    frost  : MM.hex('#b6a892'),   // frosted muzzle + brow
    crest  : MM.hex('#33281e'),
    nose   : MM.hex('#3a2e23'),
    earrim : MM.hex('#ded3bd'),
    earin  : MM.hex('#705a45'),
    hoof   : MM.hex('#2b221a'),
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
  var WINTER = { body_withers:1, body_croup:1, neck:1, body_barrel:1,
                 fore_left_shoulder:1, fore_right_shoulder:1,
                 hind_left_thigh:1, hind_right_thigh:1 };

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
      col = MM.mix(C.nose, C.frost, 0.35 * MM.smooth(-19, -24, p.z));
      return MM.cl(MM.mul(col, 1 + (MM.fbm(p, 0.9, 2, 17) - 0.5) * 0.14));
    }
    if (o.part === 'crest') {
      col = MM.mix(C.crest, C.winter, 0.30 * MM.dith(o.u, o.v, 9));
      return MM.cl(col);
    }
    if (o.part === 'ear_left' || o.part === 'ear_right') {
      if (o.face === 'down') return MM.cl(C.earin);
      var tip = MM.smooth(4.4, 5.4, Math.abs(p.x));
      col = MM.mix(MM.mix(C.dorsal, C.crest, 0.35), C.earrim, tip);
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- base coat ----------
    if (HEAD[o.part]) {
      // Frosting: greys the muzzle and the brow, strongest at the front of the face.
      var fr = MM.smooth(-13.5, -20.0, p.z) * 0.75
             + MM.smooth(16.0, 19.0, p.y) * 0.30;
      fr *= 0.7 + 0.3 * MM.fbm(p, 0.55, 2, 27);
      col = MM.mix(C.flank, C.frost, Math.min(0.70, fr));
    } else if (DISTAL[o.part]) {
      // Short dark hair -- the SUMMER layer. The winter coat does not reach the legs, which
      // is what makes the break on the body legible.
      col = MM.mix(C.flank, C.dorsal, 0.40 + 0.35 * MM.smooth(11.0, 2.0, p.y));
    } else {
      col = MM.mix(C.ventral, C.dorsal, MM.smooth(yb0 - 1.0, yb1 + 0.5, p.y));
      col = MM.mix(col, C.flank, 0.38);
    }

    // ---------- the winter coat break ----------
    // A longer, paler guard layer over the shoulders, neck and rump. Its lower edge is a
    // real BOUNDARY, jittered and dithered so it reads as where one coat ends rather than
    // as a gradient -- a smooth fade would just be countershading, which every other
    // species already has.
    if (WINTER[o.part]) {
      var edge = 1.8 * (MM.fbm(p, 0.30, 3, 31) - 0.5) * 2;
      var w = MM.smooth(yb0 + 1.0 + edge, yb0 + 5.5 + edge, p.y);
      w = Math.max(0, Math.min(1, w + (MM.dith(o.u, o.v, 19) - 0.5) * 0.26));
      // The guard hairs are longer, so they also carry a coarser clump than the flank.
      var shag = MM.fbm(p, 0.46, 3, 3) - 0.5;
      col = MM.mix(col, C.winter, 0.82 * w);
      col = MM.mul(col, 1 + shag * 0.22 * w);
    }

    // ---------- treatment ----------
    col = MM.mul(col, 1 + (MM.fbm(p, 0.62, 3, 2) - 0.5) * 0.14);
    col = MM.mul(col, 0.975 + 0.05 * MM.dith(o.u, o.v, 7));

    if (o.face === 'up')   col = MM.mul(col, 0.75);
    if (o.face === 'down') col = MM.mul(col, 1.19);

    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

  MM.show(cv);
  return 'vero tapir painted ' + Project.texture_width + 'x' + Project.texture_height
       + ', bled ' + MM.bledCount + ' seam texels';
})();
