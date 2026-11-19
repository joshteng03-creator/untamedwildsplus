// Diprotodont skins (Phase 4.1, 2026-10-09) -- ONE painter, five species. Set window.DIP_SPECIES
// to the species name before evaluating; the painter saves scratch/skins/<species>.png.
//
// The body plan is a wombat's, so the coat language is a wombat's: COARSE, short, grizzled hair
// that lies in short vertical strands, near-naked dark skin on the nose pad and soles, darker legs.
// Species are separated by GROUND colour and TREATMENT, not by noise (tapir lesson: grizzling adds
// per-texel variance but does not move the mean, so two species with the same ground collide).
//
// Rules carried over from the other new types:
//   * proximal limb segments (upper, thigh) take the BODY rule, or the biggest flank boxes paint
//     pale and the animal reads two-tone;
//   * face lighting is compensated by MULTIPLYING (up 0.74, down 1.20), never by mixing;
//   * the zero-width eye planes alpha-0 their INWARD quad, and MM.bleed protects them;
//   * hidden parts (the nasal boss on four species, the joey) are still painted, so the atlas has
//   no transparent rect for MM.bleed to smear into.
(function () {
  var SPECIES = {
    // Diprotodon optatum -- the giant of the open woodland and saltbush plains. Warm wombat
    // brown, darker along the back, paler and dustier below, dark legs. (First pass: a cool grey
    // with grizzle 0.20 read as pale SPOTS, not hair tips.)
    diprotodon: {
      dorsal: '#5c4836', flank: '#715a43', ventral: '#8a7457', muzzle: '#9e886a',
      leg: '#3f3329', skin: '#2f2925', nose: '#221d1a', ear: '#3e3329',
      grizzle: 0.08, strand: 0.24, clump: 0.14, dust: 0.10
    },
    // Zygomaturus trilobus -- wet forest and swamp margins. Deliberately the OPPOSITE ground to
    // Diprotodon: a dark, rich chocolate with a russet cast, no dust at all, and wet dark MUD
    // caked up the legs to the knee instead. The raised nasal area is bare dark skin, so the
    // one thing that makes this species different in shape also reads in colour.
    zygomaturus: {
      dorsal: '#3b2a20', flank: '#4c3627', ventral: '#5d4433', muzzle: '#6e5440',
      leg: '#2e231c', skin: '#241e1b', nose: '#1b1715', ear: '#2c211a',
      grizzle: 0.0, strand: 0.22, clump: 0.12, dust: 0.0,
      mud: '#2e281f', mudTop: 7.5, boss: '#2a2320'
    },
    // Nototherium mitchelli -- stocky and short-faced, of open eastern woodland. The PALE end of
    // the set: a dusty sandy fawn, with a darker brown face MASK over the short broad muzzle (the
    // species' one shape cue, given colour too) and dark lower legs.
    nototherium: {
      dorsal: '#8a7350', flank: '#a08a64', ventral: '#b8a682', muzzle: '#c3b18e',
      leg: '#5a4934', skin: '#332c26', nose: '#241f1b', ear: '#5e4c37',
      grizzle: 0.04, strand: 0.20, clump: 0.10, dust: 0.16,
      mask: '#5f4a35'
    },
    // Hulitherium tomasettii -- New Guinea highland forest. By the fourth species the brown range
    // is full (mid / dark / pale), so this one separates STRUCTURALLY: a cool, near-black dense
    // coat with a clearly PALER grey-buff head and throat -- a two-tone animal, which is also the
    // nearest honest nod to the "panda-like" comparison its build gets.
    hulitherium: {
      dorsal: '#262423', flank: '#312e2b', ventral: '#3d3934', muzzle: '#9a9080',
      leg: '#1e1c1b', skin: '#1f1c1a', nose: '#171513', ear: '#1f1d1c',
      grizzle: 0.03, strand: 0.20, clump: 0.10, dust: 0.0,
      headPale: '#857b6d'
    },
    // Maokopia ronaldi -- the smallest, from subalpine grassland high in the New Guinea
    // mountains. A different TREATMENT, not just a colour: a thick woolly coat, cool SLATE silver-grey
    // (a warm grey measured 17.8 from nototherium -- hue, not lightness, is what separates it),
    // grizzled with frosted tips that get heavier along the back (frost), softer strands and
    // bigger clumps -- the cold-climate coat. (First pass: frost 0.35 + grizzle 0.30 read as gravel.)
    maokopia: {
      dorsal: '#5f6366', flank: '#6f7376', ventral: '#838688', muzzle: '#94979a',
      leg: '#3e4043', skin: '#272829', nose: '#1b1c1d', ear: '#3d3f42',
      grizzle: 0.12, strand: 0.12, clump: 0.16, dust: 0.0,
      frost: 0.16
    }
  };
  var name = window.DIP_SPECIES || 'diprotodon';
  var S = SPECIES[name];
  if (!S) return 'unknown species ' + name;
  var C = {};
  ['dorsal', 'flank', 'ventral', 'muzzle', 'leg', 'skin', 'nose', 'ear'].forEach(function (k) { C[k] = MM.hex(S[k]); });
  C.tooth = MM.hex('#d9cba6');
  C.lid = MM.hex('#3b322a');
  C.pupil = MM.hex('#120e0b');
  C.glint = MM.hex('#7d6f58');
  C.dust = MM.hex('#b8a888');

  MM.bounds();
  var em = MM.map['eye_left'];
  var eyeY = (em.lo.y + em.hi.y) / 2, eyeZ = (em.lo.z + em.hi.z) / 2;
  var bb = MM.map['body_barrel'];
  var yb0 = bb.lo.y, yb1 = bb.hi.y;

  var BODY = { body_barrel: 1, body_shoulders: 1, body_chest: 1, body_rump: 1, tail: 1,
               fore_left_upper: 1, fore_right_upper: 1, hind_left_thigh: 1, hind_right_thigh: 1 };
  var HEAD = { neck: 1, head_skull: 1, head_muzzle: 1, head_jaw: 1, nasal_boss: 1 };
  var LEG = { fore_left_forearm: 1, fore_right_forearm: 1, hind_left_shank: 1, hind_right_shank: 1 };
  var FOOT = { fore_left_foot: 1, fore_right_foot: 1, hind_left_foot: 1, hind_right_foot: 1 };

  var cv = MM.paint(function (o) {
    var p = o.wp, n = o.part, col;

    // ---------- eyes: lid row above, pupil front + dim warm glint rear (world-keyed) ----------
    if (n === 'eye_left' || n === 'eye_right') {
      var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
      if (!outward) return [0, 0, 0, 0];
      if (p.y > eyeY) return C.lid.slice();
      return (p.z > eyeZ) ? C.glint.slice() : C.pupil.slice();
    }
    if (n === 'incisors') {
      return MM.cl(MM.mul(C.tooth, 0.92 + 0.12 * MM.dith(o.u, o.v, 3)));
    }
    if (n === 'nose') {
      // naked, slightly glossy rhinarium: near-black with a faint cool sheen on top
      col = MM.mix(C.nose, C.skin, 0.25 * MM.fbm(p, 1.2, 2, 4));
      if (o.face === 'up') col = MM.mul(col, 1.18);
      return MM.cl(col);
    }
    if (FOOT[n]) {
      // broad plantigrade foot: hair ends at the ankle, dark naked skin below
      col = MM.mix(C.skin, C.leg, MM.smooth(0.9, 1.9, p.y));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }
    if (n === 'joey_head' || n === 'joey_snout') {
      // a joey's coat is shorter and paler, its nose pinker
      col = (n === 'joey_snout') ? MM.mix(C.skin, MM.hex('#7d6460'), 0.6) : MM.mix(C.flank, C.ventral, 0.6);
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }
    if (n === 'nasal_boss' && S.boss) {
      // the raised nasal area: thick bare skin, darker at the top where it is most exposed
      col = MM.mix(MM.hex(S.boss), C.muzzle, 0.25 * MM.smooth(21.0, 18.0, p.y));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }
    if (n === 'ear_left' || n === 'ear_right') {
      if (o.face === 'down') return MM.cl(MM.mix(C.skin, C.ear, 0.5));
      col = MM.mix(C.ear, C.dorsal, 0.3 * MM.dith(o.u, o.v, 5));
      return MM.cl(col);
    }

    // ---------- base coat ----------
    if (HEAD[n]) {
      // a big plain head; the muzzle and lips pale and dusty, darkening toward the nose pad
      col = MM.mix(C.flank, C.dorsal, 0.4 * MM.smooth(13.0, 20.0, p.y));
      if (n === 'head_muzzle' || n === 'head_jaw' || n === 'nasal_boss') {
        col = MM.mix(col, C.muzzle, 0.55 * MM.smooth(-22.0, -28.0, p.z));
      }
      if (n === 'head_jaw') col = MM.mix(col, C.muzzle, 0.25);
      // two-tone head: pale from the ears forward, the break running down behind the jaw
      if (S.headPale) {
        var hp = (n === 'neck') ? 0.55 * MM.smooth(-15.0, -19.0, p.z) : 0.9;
        col = MM.mix(col, MM.hex(S.headPale), hp);
      }
      // face mask: dark over the muzzle bridge and round the eyes, fading back onto the skull
      if (S.mask && n !== 'neck' && n !== 'head_jaw') {
        var mk = MM.smooth(-21.5, -24.5, p.z) * MM.smooth(12.0, 13.5, p.y);
        col = MM.mix(col, MM.hex(S.mask), 0.8 * mk);
      }
    } else if (LEG[n]) {
      col = MM.mix(C.flank, C.leg, 0.45 + 0.5 * MM.smooth(9.0, 2.0, p.y));
    } else {
      col = MM.mix(C.ventral, C.dorsal, MM.smooth(yb0 - 1.0, yb1 + 0.5, p.y));
      col = MM.mix(col, C.flank, 0.35);
      if (BODY[n] && (n.indexOf('upper') >= 0 || n.indexOf('thigh') >= 0)) {
        col = MM.mix(col, C.leg, 0.35 * MM.smooth(11.0, 5.0, p.y));
      }
    }

    // ---------- treatment: coarse short wombat hair ----------
    // clumps (low-frequency), short VERTICAL strands (u-columns broken every few rows), and a
    // grizzle of pale hair tips -- all kept low so the large faces read as fur, not pattern.
    col = MM.mul(col, 1 + (MM.fbm(p, 0.55, 3, 2) - 0.5) * 2 * S.clump);
    var strand = MM.h(o.u, Math.floor((o.v + 3 * MM.h(o.u, 1, 0, 9)) / 3), 0, 13);
    col = MM.mul(col, 1 + (strand - 0.5) * S.strand);
    // tips are a PALER version of the local coat, never a fixed colour: on a dark ground a fixed
    // pale tip reads as a spot (Zygomaturus' first pass)
    if (S.grizzle && MM.dith(o.u, o.v, 21) > 1 - S.grizzle * 0.5) col = MM.mul(col, 1.22);
    // frosted tips: more of them, and paler, the higher up the animal (cold-climate wool)
    if (S.frost && !HEAD[n] && MM.dith(o.u, o.v, 23) < S.frost * MM.smooth(10.0, 20.0, p.y)) {
      col = MM.mix(col, MM.hex('#c8cacc'), 0.32);
    }
    // dust on the belly and lower legs
    col = MM.mix(col, C.dust, S.dust * MM.smooth(11.0, 4.0, p.y));
    // wet mud caked up the legs, with a ragged top edge (swamp species)
    if (S.mud && (LEG[n] || BODY[n])) {
      var top = S.mudTop + 1.2 * (MM.fbm(p, 1.6, 2, 31) - 0.5);
      if (p.y < top) col = MM.mix(col, MM.hex(S.mud), 0.75 * MM.smooth(top, top - 1.0, p.y));
    }

    if (o.face === 'up') col = MM.mul(col, 0.74);
    if (o.face === 'down') col = MM.mul(col, 1.20);
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/' + name + '.png', cv);
  return name + ' painted, bled ' + MM.bledCount + ' seam texels';
})();
