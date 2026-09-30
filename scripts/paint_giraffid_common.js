// Shared giraffid skin conventions. Every paint_giraffid_*.js evals this first.
//
// ---------------------------------------------------------------------------
// THE EYE
// ---------------------------------------------------------------------------
// Standardised per TYPE and copied verbatim to all seven species, which is this mod's own
// convention (derived from the shipped PNGs, not invented -- see the bear/mammoth/big_cat
// reskin batch). Unlike ModelTapir this follows the general guidance rather than overriding
// it: a DIM WARM catchlight, never white, because at 2x2 texels a white texel is a quarter
// of the whole eye and reads as a headlight.
//
// The plane is a whole-unit 2.0 x 2.0 zero-width box, so each of its two coplanar quads is
// exactly 2x2 texels. Rows, not rings: a "ring" test on a 2x2 face marks every texel.
//     upper row .......... brow / lid, a mid hide tone
//     lower row, FRONT ... dark pupil
//     lower row, REAR .... dim warm catchlight
//
// KEYED ON WORLD POSITION, NEVER ON FACE-LOCAL i/j. side_l and side_r run in OPPOSITE
// directions along i, so an index-based rule puts the catchlight on the front of one eye and
// the rear of the other. -Z is the nose, so rear = larger z.
window.GIR_EYE_LID   = [74, 59, 44];
window.GIR_EYE_PUPIL = [20, 16, 12];
window.GIR_EYE_GLINT = [124, 103, 70];

// Call ONCE per paint, before MM.paint, to fix the thresholds off the real eye geometry.
window.girEyeInit = function () {
  var m = MM.map['eye_left'];
  window._eyeMidY = (m.lo.y + m.hi.y) / 2;
  window._eyeMidZ = (m.lo.z + m.hi.z) / 2;
};

// Returns the colour for an eye texel, or null if `o` is not an eye part.
window.girEye = function (o) {
  if (o.part !== 'eye_left' && o.part !== 'eye_right') return null;
  var p = o.wp;
  // A zero-width box draws TWO coplanar quads sampling opposite UV halves, so they z-fight
  // each other. Alpha-0 the inward one; entityCutoutNoCull discards it entirely. Derived
  // from the world normal rather than reasoned about -- the camel's convention is the
  // inverse of the rhino's and both render correctly, so the sign is not guessable.
  var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
  if (!outward) return [0, 0, 0, 0];
  if (p.y > window._eyeMidY) return window.GIR_EYE_LID.slice();
  return (p.z > window._eyeMidZ) ? window.GIR_EYE_GLINT.slice()
                                 : window.GIR_EYE_PUPIL.slice();
};

// ---------------------------------------------------------------------------
// 3D WORLD-SPACE VORONOI -- the giraffe patch network
// ---------------------------------------------------------------------------
// The reticulated patch network is the most recognisable marking in the animal kingdom and
// it is the entire reason this type needs more than a countershading rule. It is also the
// single best case for the SOP's world-position painting: seeded in WORLD space, one cell
// field spans the whole animal, so a patch that crosses from the barrel to the shoulder to
// the neck stays ONE patch across three separate UV islands with no seam and no bookkeeping.
// Any face-local or per-part scheme would break every patch at every box boundary.
//
// Jittered-grid Voronoi. For a point, find the nearest and second-nearest seed; `edge` =
// d2 - d1 is small exactly on a cell boundary, which is where the pale reticulation lines
// go. `cell` returns a stable hash of the WINNING cell so each patch can carry its own
// darkness -- real patches vary noticeably and a single flat chestnut reads as printed cloth.
//
// Cell size is set by texel density, not by the real animal. Box UV gives exactly 1 texel
// per model unit here, and 1 unit is 8.6 cm, so a true 20 cm patch would be 2.3 texels --
// indistinguishable from noise. The patches are therefore deliberately enlarged to ~4.4
// units so that a patch is ~4 texels across with a ~1 texel line between, which is the
// smallest pattern that still reads AS a pattern at this resolution.
MM.vor = function (p, cell, seed) {
  var gx = Math.floor(p.x / cell), gy = Math.floor(p.y / cell), gz = Math.floor(p.z / cell);
  var d1 = 1e9, d2 = 1e9, bx = 0, by = 0, bz = 0;
  for (var dx = -1; dx <= 1; dx++) {
    for (var dy = -1; dy <= 1; dy++) {
      for (var dz = -1; dz <= 1; dz++) {
        var cx = gx + dx, cy = gy + dy, cz = gz + dz;
        // Near-full-cell jitter (0.10 .. 0.90) so the cells come out as irregular polygons
        // rather than the tidy hexagons a lightly-jittered grid produces.
        var sx = (cx + 0.10 + 0.80 * MM.h(cx, cy, cz, seed)) * cell;
        var sy = (cy + 0.10 + 0.80 * MM.h(cx, cy, cz, seed + 31)) * cell;
        var sz = (cz + 0.10 + 0.80 * MM.h(cx, cy, cz, seed + 67)) * cell;
        var ex = p.x - sx, ey = p.y - sy, ez = p.z - sz;
        var d = Math.sqrt(ex * ex + ey * ey + ez * ez);
        if (d < d1) { d2 = d1; d1 = d; bx = cx; by = cy; bz = cz; }
        else if (d < d2) { d2 = d; }
      }
    }
  }
  return { d1: d1, edge: d2 - d1, cell: MM.h(bx, by, bz, seed + 101) };
};

// ---------------------------------------------------------------------------
// PART CLASSES
// ---------------------------------------------------------------------------
// SOP: the proximal limb segments (shoulder, thigh) take the BODY rule, or the two biggest
// boxes on the flank paint out pale and the animal reads two-tone. tail_dock is body too --
// it is patched at the base on a real giraffe.
window.GIR_BODY = { body_barrel: 1, body_withers: 1, body_chest: 1, body_croup: 1,
                    fore_left_shoulder: 1, fore_right_shoulder: 1,
                    hind_left_thigh: 1, hind_right_thigh: 1, tail_dock: 1 };
window.GIR_NECK = { neck_1: 1, neck_2: 1, neck_3: 1, neck_4: 1 };
window.GIR_HEAD = { head_skull: 1, head_muzzle: 1, head_jaw: 1 };
window.GIR_DISTAL = { fore_left_forearm: 1, fore_right_forearm: 1,
                      fore_left_cannon: 1, fore_right_cannon: 1,
                      hind_left_gaskin: 1, hind_right_gaskin: 1,
                      hind_left_cannon: 1, hind_right_cannon: 1 };
window.GIR_FOOT = { fore_left_foot: 1, fore_right_foot: 1,
                    hind_left_foot: 1, hind_right_foot: 1 };
window.GIR_MANE = { mane_1: 1, mane_2: 1, mane_3: 1, mane_4: 1 };
window.GIR_EAR = { ear_left: 1, ear_right: 1 };
// Every ossicone box of every family. ALL of them are painted on every species, even the
// ones showModel hides: an unpainted rect is fully transparent, and MM.bleed would then
// dilate a neighbour's colour into it and quietly corrupt whichever part is packed
// alongside. Filling them keeps the atlas well-defined.
window.GIR_OSS = { oss_taper_left: 1, oss_taper_right: 1, oss_boss: 1,
                   oss_palm_base_left: 1, oss_palm_base_right: 1,
                   oss_palm_left: 1, oss_palm_right: 1,
                   oss_spike_left: 1, oss_spike_right: 1,
                   oss_quad_left: 1, oss_quad_right: 1,
                   oss_quad_front_left: 1, oss_quad_front_right: 1 };

// MC bakes face lighting into the render (top 1.16, bottom 0.6, ...), so a topline painted
// merely "dark" still comes out LIGHTER than the flanks. Compensate by MULTIPLYING, never by
// mixing toward a fixed body colour -- mixing drags every part's hue toward it, which turned
// the macrauchenia's grey muzzle brown.
window.girFaceLight = function (col, face) {
  if (face === 'up') return MM.mul(col, 0.74);
  if (face === 'down') return MM.mul(col, 1.20);
  return col;
};
'giraffid common ready';
