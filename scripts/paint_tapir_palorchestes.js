// Palorchestes azael -- the "marsupial tapir". Pleistocene Australia, ~1000 kg, extinct
// ~40 ka. NOT a tapir at all: a diprotodontid marsupial that converged on the same body
// plan, grouped here exactly the way thylacine is grouped under dire_wolf elsewhere in
// this mod.
//
// It is the only species in the set that is not a Tapirus, and the skin has to say so.
// Every genus-wide convention the other six share is deliberately BROKEN here:
//
//   * NO white ear rim. That mark is the tapir field mark; a diprotodontid has no business
//     with it, and dropping it is the clearest possible signal that this is a different
//     animal. The ears get a plain dark back and a rufous inner instead.
//   * NO crest (showModel-gated off, variant 6) -- so the neck reads bare where five of
//     the others carry a mane.
//   * CLAWS, shown on this species alone. Palorchestes had huge clawed forelimbs, and they
//     are modelled rather than painted, so the silhouette itself differs. They are the one
//     thing a shared mesh would otherwise erase.
//
// TREATMENT: a coarse SHAGGY marsupial pelage -- long directional hair with visible
// strand breakup, closer to a wombat's or a koala's than to any tapir's short tropical
// coat. Combined with a smoky charcoal-brown ground and a rufous shoulder wash, which is a
// very Australian-mammal palette and one no other species here occupies.
(function () {
  var C = {
    dorsal : MM.hex('#4f4339'),   // smoky charcoal-brown
    flank  : MM.hex('#6d5a48'),
    ventral: MM.hex('#806c57'),
    rufous : MM.hex('#8d5e33'),   // rufous shoulder + haunch wash
    shag   : MM.hex('#a08b75'),   // the lit edge of a hair strand
    face   : MM.hex('#413830'),   // dark blunt face
    nose   : MM.hex('#241f1b'),
    earin  : MM.hex('#6d4c33'),   // rufous inner ear -- NOT the tapir white rim
    hoof   : MM.hex('#241e19'),
    claw   : MM.hex('#c3b79c'),   // pale horn -- these are meant to be SEEN
    clawtip: MM.hex('#6e6352')
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

  // Shag: long DIRECTIONAL strands, not the isotropic clumping the other coats use. The
  // anisotropy is what separates it -- a hair lying along the body reads differently from
  // a clump of wool, and it is the cue that says "long coarse pelage" at this resolution.
  // Sampled with p.y scaled up so the noise is stretched vertically = strands hang down.
  function shag(p) {
    var q = { x: p.x * 1.0, y: p.y * 0.30, z: p.z * 1.0 };
    var strand = MM.fbm(q, 0.85, 3, 4) - 0.5;
    var coarse = MM.fbm(p, 0.30, 2, 9) - 0.5;
    return strand * 0.72 + coarse * 0.42;
  }

  var cv = MM.paint(function (o) {
    var p = o.wp, col;

    var ec = window.tapirEye(o);
    if (ec) return ec;

    // ---------- THE CLAWS ----------
    // Visible on this species ALONE. Pale horn so they read against a dark coat, graded
    // darker toward the tip the way real keratin wears, and given a lengthwise grain.
    // Kept low-contrast per the standing rule that a claw painted with bright alternating
    // pixels reads as painted white stripes rather than as keratin.
    if (o.part === 'claw_left' || o.part === 'claw_right') {
      var t = MM.smooth(-6.6, -9.4, p.z);                 // 0 at the base, 1 at the point
      col = MM.mix(C.claw, C.clawtip, 0.30 + 0.55 * t);
      col = MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 3));
      return MM.cl(col);
    }

    if (FOOT[o.part]) {
      return MM.cl(MM.mul(C.hoof, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }
    if (o.part === 'proboscis_1' || o.part === 'proboscis_2') {
      col = MM.mix(C.face, C.nose, 0.45 + 0.55 * MM.smooth(-19, -24, p.z));
      return MM.cl(MM.mul(col, 1 + (MM.fbm(p, 0.9, 2, 17) - 0.5) * 0.12));
    }
    if (o.part === 'crest') {
      // Hidden at runtime on this variant, but painted so the rect is never transparent
      // (an unpainted rect lets MM.bleed pull a neighbour's colour into it).
      return MM.cl(MM.mul(C.dorsal, 0.96 + 0.08 * MM.dith(o.u, o.v, 9)));
    }
    if (o.part === 'ear_left' || o.part === 'ear_right') {
      // NO WHITE RIM. This is the deliberate break with the other six.
      if (o.face === 'down') return MM.cl(C.earin);
      col = MM.mix(C.dorsal, C.rufous, 0.25);
      return MM.cl(MM.mul(col, 0.96 + 0.10 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- base coat ----------
    if (HEAD[o.part]) {
      col = MM.mix(C.face, C.flank, 0.35 * MM.smooth(19.0, 13.0, p.y));
    } else if (DISTAL[o.part]) {
      col = MM.mix(C.flank, C.dorsal, 0.45 + 0.35 * MM.smooth(11.0, 2.0, p.y));
    } else {
      col = MM.mix(C.ventral, C.dorsal, MM.smooth(yb0 - 1.0, yb1 + 0.5, p.y));
      col = MM.mix(col, C.flank, 0.35);
    }

    // ---------- rufous shoulder and haunch wash ----------
    // Concentrated over the two limb girdles rather than spread along the topline, which
    // is where a heavy digging animal actually carries its muscle mass -- and it reads as
    // "marsupial" rather than as another dorsal cape.
    var sh = Math.exp(-Math.pow((p.z + 2.0) / 5.5, 2))
           + Math.exp(-Math.pow((p.z - 13.0) / 5.5, 2));
    sh *= MM.smooth(yb0 - 1.0, yb1 - 2.0, p.y) * (0.7 + 0.3 * MM.fbm(p, 0.4, 2, 15));
    col = MM.mix(col, C.rufous, 0.52 * Math.min(1, sh));

    // ---------- treatment: SHAGGY ----------
    var sg = shag(p);
    col = MM.mix(col, C.shag, Math.max(0, sg) * 0.75);
    col = MM.mul(col, 1 + sg * 0.30);
    col = MM.mul(col, 0.975 + 0.05 * MM.dith(o.u, o.v, 7));

    if (o.face === 'up')   col = MM.mul(col, 0.76);
    if (o.face === 'down') col = MM.mul(col, 1.18);

    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

  MM.show(cv);
  return 'palorchestes painted ' + Project.texture_width + 'x' + Project.texture_height
       + ', bled ' + MM.bledCount + ' seam texels';
})();
