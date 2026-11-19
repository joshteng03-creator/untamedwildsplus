// Caprine skins (Phase 4.5, 2026-10-10) -- ONE painter, six species. Set window.CAP_SPECIES first; saves
// scratch/skins/caprine_<species>.png. Every horn family is painted in each skin (only the species' own shows in game, but
// an unpainted rect would let MM.bleed smear into the atlas).
//
// Coats: short, fine grain (the mountain goat: longer, shaggier, white). Horns: the species' horn colour with GROWTH RIDGES
// -- a darker ring every 3rd texel along each segment's length (ibex: raised knobs; sheep: annual rings). Hooves dark.
// Goat/sheep eyes are amber. Countershading on LOCAL faces (down = belly), as on the other new rigs.
(function () {
  var SPECIES = {
    // Capra ibex -- grey-brown, paler belly, darker legs and a dark flank line, dark beard, grey-brown knobbed horns
    alpine_ibex: { back: '#6f6250', side: '#7f705c', belly: '#c4b8a1', leg: '#4a4034', face: '#6a5d4c', muzzle: '#8a7c66',
                   beard: '#3a322a', horn: '#7a6c58', flank: '#4a4034', rump: null, hoof: '#2a2522', grain: 0.07 },
    // Capra sibirica -- warm tawny with a PALE SADDLE over the mid-back and a dark dorsal stripe; long dark beard; dark horns
    siberian_ibex: { back: '#8c7350', side: '#9c815c', belly: '#d2c3a4', leg: '#5a4935', face: '#806a4c', muzzle: '#a08a68',
                     beard: '#2e271f', horn: '#5a4c3c', saddle: '#c8b48c', dorsal: '#4a3c2c', rump: null, hoof: '#2a2522', grain: 0.07 },
    // Ovis ammon -- grey-brown with a WHITE rump patch, white belly and pale legs, a pale ruff; massive tan horns
    argali: { back: '#7a6650', side: '#8a755d', belly: '#e6e0d2', leg: '#cfc6b4', face: '#7e6c56', muzzle: '#d8d0c0',
              beard: null, horn: '#b49a74', rump: '#ece6da', ruff: '#d4cab6', hoof: '#3a332c', grain: 0.06 },
    // Ovis canadensis -- rich brown with a white rump patch and a WHITE MUZZLE; brown-tan horns
    bighorn: { back: '#6a4c34', side: '#7a5a3e', belly: '#d8cdb8', leg: '#5a4430', face: '#6e5038', muzzle: '#e2dacb',
               beard: null, horn: '#9a7e58', rump: '#e8e0d0', hoof: '#2e2722', grain: 0.06 },
    // Rupicapra rupicapra -- summer tan, a black dorsal stripe, the WHITE FACE with a black stripe through each eye,
    // black hooked horns
    chamois: { back: '#8a6a44', side: '#9c7a50', belly: '#c8b08a', leg: '#3e3024', face: '#e0d2b4', muzzle: '#e6dac0',
               beard: null, horn: '#1c1916', dorsal: '#2e241a', eyeStripe: '#2c2219', rump: null, hoof: '#1e1a16', grain: 0.06 },
    // Oreamnos harringtoni -- (like the living mountain goat) a shaggy WHITE coat, a white beard, black horns and hooves
    harrington_goat: { back: '#e6e2d8', side: '#ece8de', belly: '#f2efe8', leg: '#e2ddd2', face: '#ece8de', muzzle: '#e8e4da',
                       beard: '#f0ece4', horn: '#1a1816', rump: null, hoof: '#1a1816', grain: 0.10, shag: true }
  };
  var name = window.CAP_SPECIES || 'alpine_ibex';
  var S = SPECIES[name];
  if (!S) return 'unknown species ' + name;
  var C = {};
  for (var k in S) if (typeof S[k] === 'string') C[k] = MM.hex(S[k]);
  C.nose = MM.hex('#1f1b18');
  C.lid = MM.hex('#2a241e');
  C.iris = MM.hex('#b08a3a');
  C.pupil = MM.hex('#0e0c0a');

  MM.bounds();
  var em = MM.map['eye_left'];
  var eyeY = (em.lo.y + em.hi.y) / 2, eyeZ = (em.lo.z + em.hi.z) / 2;
  var rumpLo = MM.map['body_rump'].lo.z;
  var rumpHi = MM.map['body_rump'].hi.z;
  function is(n, s) { return n.indexOf(s) >= 0; }
  function side(f) { return f === 'side_l' || f === 'side_r'; }

  var cv = MM.paint(function (o) {
    var p = o.wp, n = o.part, f = o.face, col;
    var jj = o.h > 1 ? o.j / (o.h - 1) : 0.5;

    if (n === 'eye_left' || n === 'eye_right') {
      var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
      if (!outward) return [0, 0, 0, 0];
      if (p.y > eyeY) return (S.eyeStripe ? C.eyeStripe : C.lid).slice();
      return (p.z > eyeZ) ? C.pupil.slice() : C.iris.slice();
    }
    if (n === 'nose') return MM.cl(MM.mul(C.nose, 0.95 + 0.1 * MM.dith(o.u, o.v, 3)));
    if (n === 'beard') {
      col = C.beard || C.muzzle;
      return MM.cl(MM.mul(col, 0.9 + 0.2 * MM.dith(o.u, Math.floor(o.v / 2), 7)));
    }
    if (is(n, 'ibex_') || is(n, 'ram_') || is(n, 'chamois_') || is(n, 'dagger_')) {
      // growth ridges: a darker ring every 3rd texel along the length (side faces run along the segment)
      col = C.horn;
      if (side(f) || f === 'front' || f === 'back') {
        if (o.j % 3 === 0) col = MM.mul(col, 0.78);
      }
      if (f === 'up') col = MM.mul(col, 0.9);
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 9)));
    }
    if (is(n, '_hoof')) {
      col = MM.mix(C.hoof, C.leg, jj < 0.3 ? 0.4 : 0.0);
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 3)));
    }
    if (is(n, 'ear_')) {
      col = (f === 'down') ? MM.mix(C.face, C.belly, 0.6) : C.face;
      if (S.eyeStripe) col = (f === 'down') ? MM.mix(C.face, C.belly, 0.4) : MM.mix(C.side, C.eyeStripe, 0.35);
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 5)));
    }

    if (n === 'head' || n === 'muzzle') {
      col = n === 'muzzle' ? C.muzzle : MM.mix(C.face, C.muzzle, 0.25);
      if (f === 'down') col = MM.mix(col, C.belly, 0.5);
      // chamois: a DIAGONAL dark stripe from the horn base, through the eye, down to the corner of the mouth, on a
      // creamy face with a pale nose bridge. (v1: a stark white mask crossed by a flat horizontal black band -- "looks
      // weird", user.) The stripe's height on each side face falls toward the front: high at the back of the head,
      // through the eye, low at the muzzle.
      if (S.eyeStripe && side(f)) {
        var hm = MM.map[n];
        var t = MM.smooth(hm.hi.z, hm.lo.z, p.z);                      // 0 = rear of this box, 1 = its front
        var c0 = (n === 'head') ? 0.15 + 0.45 * t : 0.55 + 0.35 * t;  // the head carries it down to the muzzle
        if (Math.abs(jj - c0) < 0.17) col = C.eyeStripe;
      }
    } else if (is(n, 'fore_') || is(n, 'hind_')) {
      var upperSeg = is(n, 'upper') || is(n, 'thigh');
      col = upperSeg ? MM.mix(C.side, C.leg, 0.35 * jj) : C.leg;
      if (S.rump && is(n, 'thigh') && f === 'back') col = C.rump;
    } else if (n === 'tail') {
      col = S.rump ? C.rump : C.back;
      if (f === 'up' && !S.rump) col = MM.mul(col, 0.85);
    } else {
      var ven = (f === 'down') ? 1.0 : side(f) ? MM.smooth(0.6, 0.98, jj) : 0.0;
      col = MM.mix(C.side, C.back, f === 'up' ? 0.75 : 0.2);
      col = MM.mix(col, C.belly, ven);
      if (S.saddle && f === 'up' && n === 'body_barrel') col = MM.mix(col, C.saddle, 0.7);
      if (S.saddle && side(f) && n === 'body_barrel' && jj < 0.25) col = MM.mix(col, C.saddle, 0.45);
      if (S.dorsal && f === 'up' && Math.abs(p.x) < 0.8) col = C.dorsal;
      if (S.flank && side(f) && Math.abs(jj - 0.62) < 0.06 && n === 'body_barrel') col = MM.mix(col, C.flank, 0.6);
      if (S.ruff && n === 'neck' && f !== 'up') col = MM.mix(col, C.ruff, 0.55);
      // the sheep's white RUMP PATCH: the rear face of the rump and the rear of its sides
      if (S.rump && n === 'body_rump' && (f === 'back' || (side(f) && p.z > rumpHi - 2.2))) col = C.rump;
    }

    // coat: fine grain; the mountain goat's shag adds vertical hanging locks
    col = MM.mul(col, 1 + (MM.dith(o.u, o.v, 13) - 0.5) * S.grain);
    col = MM.mul(col, 1 + (MM.fbm(p, 1.1, 2, 5) - 0.5) * 0.10);
    if (S.shag) col = MM.mul(col, 1 + (MM.h(o.u, Math.floor(o.v / 3), 0, 19) - 0.5) * 0.10);
    if (f === 'up') col = MM.mul(col, 0.84);
    if (f === 'down') col = MM.mul(col, 1.10);
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/caprine_' + name + '.png', cv);
  return name + ' painted, bled ' + MM.bledCount;
})();
