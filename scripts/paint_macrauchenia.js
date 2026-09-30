// Macrauchenia patachonica -- Patagonian steppe litoptern.
//
// IDENTITY: the guanaco pattern, which is the right call twice over -- it is the living
// large ungulate of exactly this landscape, and it gives the animal a cue that survives
// being 28 pixels tall. Three bands, hard-edged rather than graded:
//   warm tawny above  ->  sharp cream ventral line  ->  a distinctly GREY head and neck.
// The grey head is the diagnostic. It is desaturated hard against the tawny body (S 0.09
// vs 0.42), so the head reads as a separate object at any distance -- which matters on a
// long-necked animal whose head is the thing you actually look at.
//
// Surface TREATMENT (not just palette): a SHORT DENSE cold-steppe coat -- fine tight
// grain, low-amplitude clumping, and wind-ruffled patches lying along the flank. That is
// deliberately the opposite of the toxodon's sparse-bristled swamp hide on the same
// codebase, so the two never read as one animal recoloured.
(function () {
  var C = {
    dorsal : MM.hex('#6f4f2e'),   // warm tawny brown, dorsal
    flank  : MM.hex('#96703f'),   // lighter tawny, flank
    ventral: MM.hex('#ddd0b6'),   // cream, the guanaco underside
    greyhd : MM.hex('#8b817a'),   // head + upper neck grey -- THE identity cue
    greydk : MM.hex('#5f5852'),   // darker grey, muzzle and ear backs
    nose   : MM.hex('#3b3531'),   // charcoal proboscis tip
    nasal  : MM.hex('#a29890'),   // pale crown on the retracted-nasal dome
    earin  : MM.hex('#bcab92'),
    hoof   : MM.hex('#3f382f'),
    lid    : MM.hex('#7d736b'),
    pupil  : MM.hex('#17130f'),
    glint  : MM.hex('#8a7a63')
  };

  MM.bounds();

  // Countershading is stated over the BODY's own vertical range, measured off the barrel,
  // so the legs cannot drag the gradient (the model spans y 0..40 but the torso only
  // 16.4..26.6, and using the model range washes the whole trunk to one tone).
  var bb = MM.map['body_barrel'];
  var yb0 = bb.lo.y, yb1 = bb.hi.y;

  // SOP: the proximal limb segments take the BODY rule. On the leg rule the shoulder and
  // thigh -- the two biggest boxes on the flank -- paint out pale and the animal reads
  // two-tone.
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

    // ---------- eyes: zero-width planes ----------
    // A width-0 box draws TWO coplanar quads sampling opposite UV halves, so they z-fight
    // each other. Alpha-0 the inward one; entityCutoutNoCull discards it entirely.
    // The plane is a whole-unit 3x3, so the two halves land on disjoint texel rects and
    // clearing one cannot eat a column of the other.
    if (o.part === 'eye_left' || o.part === 'eye_right') {
      var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
      if (!outward) return [0, 0, 0, 0];
      // The plane is a 2x2, so each outward face is exactly 2x2 texels: the whole quad IS
      // the eye and the surrounding head grey supplies the socket. Pixel INDICES, never
      // fractions -- at this size a fraction test overlaps and the glint lands on top of
      // the pupil instead of beside it.
      // Dim WARM catchlight, never white: on a 2px eye a white pixel is a quarter of it.
      if (o.i === 1 && o.j === 0) return MM.cl(C.glint);
      return MM.cl(C.pupil);
    }

    // ---------- feet: dark horn ----------
    if (FOOT[o.part]) {
      col = MM.mix(C.hoof, C.ventral, 0.10 + 0.10 * MM.fbm(p, 0.55, 3, 11));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }

    // ---------- proboscis: soft dark skin, darkening to the tip ----------
    if (o.part === 'proboscis_1' || o.part === 'proboscis_2') {
      var tip = MM.smooth(-30, -35, p.z);
      col = MM.mix(C.greydk, C.nose, 0.35 + 0.65 * tip);
      col = MM.mul(col, 1 + (MM.fbm(p, 0.9, 2, 17) - 0.5) * 0.16);
      return MM.cl(col);
    }

    // ---------- ears ----------
    if (o.part === 'ear_left' || o.part === 'ear_right') {
      col = (o.face === 'down') ? C.earin.slice() : MM.mix(C.greyhd, C.greydk, 0.45);
      return MM.cl(MM.mul(col, 0.95 + 0.10 * MM.dith(o.u, o.v, 5)));
    }

    // ---------- the diagnostic nasal dome ----------
    // Paler than the surrounding skull so the retracted nasal opening actually reads as a
    // feature rather than disappearing into the head grey.
    if (o.part === 'head_nasal') {
      col = MM.mix(C.greyhd, C.nasal, 0.85 + 0.15 * MM.smooth(0, 1, o.j / Math.max(1, o.h - 1)));
      // The crown of the dome is lifted further still. At 0.55 the feature was a barely
      // perceptible tint and the character it exists to carry did not read at all.
      if (o.face === 'up') col = MM.mix(col, C.nasal, 0.75);
      return MM.cl(MM.mul(col, 0.96 + 0.09 * MM.dith(o.u, o.v, 6)));
    }

    // ---------- base coat ----------
    var t;                                   // 0 = ventral, 1 = dorsal
    if (HEAD[o.part]) {
      t = 0.65;
      col = C.greyhd.slice();
      // The muzzle and the underside of the jaw go darker, as a guanaco's do.
      if (o.part === 'head_muzzle') col = MM.mix(col, C.greydk, 0.45);
      if (o.part === 'head_jaw')    col = MM.mix(col, C.greydk, 0.28);
    } else if (NECK[o.part]) {
      // The tawny body climbs INTO the grey head over the neck's own world-y span, so
      // there is no hard colour seam at either joint. Stated in world y rather than
      // per-part so all three segments share one continuous ramp.
      var g = MM.smooth(22.0, 33.0, p.y);
      t = MM.smooth(yb0, yb1, p.y);
      col = MM.mix(C.flank, C.dorsal, 0.45);
      col = MM.mix(col, C.greyhd, g);
    } else if (DISTAL[o.part]) {
      // Guanaco legs go PALE downward -- so here the usual "don't let the body rule give
      // it white socks" warning is inverted: the pale lower leg is the intended marking,
      // and it is graded down the whole limb column in world y so it stays continuous
      // across forearm -> cannon instead of restarting per segment.
      t = 0.4;
      var down = MM.smooth(18.0, 3.0, p.y);
      col = MM.mix(C.flank, C.ventral, 0.25 + 0.60 * down);
    } else {
      t = MM.smooth(yb0, yb1, p.y);
      col = MM.mix(C.ventral, C.dorsal, t);
      col = MM.mix(col, C.flank, 0.45 * (1 - Math.abs(t - 0.5) * 2));
    }

    // ---------- the guanaco line ----------
    // A comparatively SHARP ventral boundary is the whole point of the pattern, but a
    // clean horizontal edge reads as an ugly painted line (that is how the aurochs' first
    // dorsal stripe was rejected). Jitter the threshold per texel with fbm and keep the
    // transition ~2u wide so it stays a crisp but broken natural edge.
    if (BODYCLS[o.part] || NECK[o.part]) {
      var line = yb0 + 2.6 + 1.5 * (MM.fbm(p, 0.34, 3, 21) - 0.5) * 2;
      var m = MM.smooth(line + 1.0, line - 1.0, p.y);
      col = MM.mix(col, C.ventral, 0.88 * m);
    }

    // ---------- treatment: short dense cold-steppe coat ----------
    // Fine tight grain + low-amplitude clumping. Kept low contrast on purpose: +-0.26
    // luminance speckle reads as TV static, not fur.
    var clump = MM.fbm(p, 0.62, 3, 2) - 0.5;
    col = MM.mul(col, 1 + clump * 0.15);
    col = MM.mul(col, 0.97 + 0.06 * MM.dith(o.u, o.v, 7));

    // Wind-ruffled patches lying ALONG the flank. Keyed off face-local i/j, never global
    // texture coords, or the ruffle lands at a different height on every part and reads
    // as haphazard rather than as a coat lying one way.
    if (BODYCLS[o.part] || NECK[o.part]) {
      var ruffle = Math.sin((o.i / Math.max(1, o.w)) * Math.PI * 2.2
                            + MM.fbm(p, 0.28, 2, 4) * 4.5);
      col = MM.mul(col, 1 + ruffle * 0.055);
    }

    // ---------- MC face lighting compensation ----------
    // MC bakes directional shading at render time (up brightest, down darkest), so a
    // topline painted merely "dark" still renders LIGHTER than the flanks it sits above.
    // Over-correct at the source rather than trusting the eye.
    //
    // MULTIPLY, do not mix toward C.dorsal/C.ventral. Mixing toward a fixed body colour
    // drags every part's HUE toward it, and on a two-tone animal that is a visible bug:
    // mixing the head's top faces 0.44 toward the tawny dorsal turned the grey muzzle
    // brown, so the skull rendered grey on the sides and tan on top. Scaling luminance
    // keeps each part's own hue and corrects exactly the thing being corrected.
    if (o.face === 'up')   col = MM.mul(col, 0.72);
    if (o.face === 'down') col = MM.mul(col, 1.22);

    return MM.cl(col);
  });

  // Seal the fractional-footprint seams. Protect the eye planes so their alpha-0'd
  // inward quad stays deleted -- bleeding would refill it and bring the z-fight back.
  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

  MM.show(cv);
  return 'macrauchenia painted ' + Project.texture_width + 'x' + Project.texture_height
       + ', bled ' + MM.bledCount + ' seam texels';
})();
