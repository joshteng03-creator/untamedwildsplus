// Shared tapir skin conventions. Every paint_tapir_*.js evals this first.
//
// THE EYE IS THE USER'S ART, CAPTURED FROM THEIR HAND-PAINTED TEXTURE, AND IS STAMPED
// VERBATIM ON ALL SEVEN SPECIES. Do not re-derive it and do not "improve" it.
//
// How it was recovered, because the same thing will happen again: the user edited the eye
// by PAINTING THE TEXTURE, not by changing the model, and a repaint silently overwrote it
// (MM.show replaces the whole Texture object). A full model diff -- geometry, origin,
// rotation, inflate, uv_offset, mirror, visibility, autouv and all six face UVs, across
// both open projects -- came back identical, which is what proved the change had to be in
// the texture. Diffing the live texture against MM.canvas found exactly 6 differing texels.
// This is the SOP's "capture their edited region and re-stamp it as the painter's LAST
// step" rule; the mod's own convention is that eye art is standardised per TYPE and copied,
// never reasoned about.
//
// The design, stated in WORLD space:
//     upper row (both texels) ... brow / lid, a mid hide tone
//     lower row, FRONT ......... dark pupil
//     lower row, REAR .......... a bright WHITE catchlight
//
// KEYED ON WORLD POSITION, NEVER ON FACE-LOCAL i/j. side_l and side_r run in OPPOSITE
// directions along i, so an index-based rule puts the catchlight on the front of one eye
// and the rear of the other -- and the user's own texture has it on the REAR of both,
// which is the tell that it must be a world-space rule. -Z is the nose, so rear = larger z.
//
// Note this deliberately overrides the "dim warm catchlight, never white" guidance used on
// the other rigs in this mod. At 2x2 a white texel is a quarter of the eye, which is the
// reason for that guidance -- but it is the user's explicit art direction for this type.
window.TAPIR_EYE_LID   = [59, 53, 48];
window.TAPIR_EYE_PUPIL = [18, 15, 12];
window.TAPIR_EYE_GLINT = [255, 255, 255];

// Call ONCE per paint, before MM.paint, to fix the thresholds off the real eye geometry.
window.tapirEyeInit = function () {
  var m = MM.map['eye_left'];
  window._eyeMidY = (m.lo.y + m.hi.y) / 2;
  window._eyeMidZ = (m.lo.z + m.hi.z) / 2;
};

// Returns the colour for an eye texel, or null if `o` is not an eye part.
window.tapirEye = function (o) {
  if (o.part !== 'eye_left' && o.part !== 'eye_right') return null;
  var p = o.wp;
  // A zero-width box draws TWO coplanar quads sampling opposite UV halves, so they z-fight
  // each other. Alpha-0 the inward one; entityCutoutNoCull discards it entirely.
  var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
  if (!outward) return [0, 0, 0, 0];
  if (p.y > window._eyeMidY) return window.TAPIR_EYE_LID.slice();
  return (p.z > window._eyeMidZ) ? window.TAPIR_EYE_GLINT.slice()
                                 : window.TAPIR_EYE_PUPIL.slice();
};
'tapir common ready';
