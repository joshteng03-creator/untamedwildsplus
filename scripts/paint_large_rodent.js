// Large rodent skins (Phase 4.6, 2026-10-10) -- ONE painter, four species. Set window.ROD_SPECIES first; saves
// scratch/skins/large_rodent_<species>.png. The paddle tail and the bird are painted in every skin (shown per species / per
// entity in game, but an unpainted rect would let MM.bleed smear into the atlas).
//
// Coats: capybaras = sparse, COARSE, bristly hair (short vertical strands); beavers = dense, glossy, finer fur. The paddle
// tail is bare scaly skin -- random dark-edged scales, never a regular grid (the giant-monitor chessboard lesson).
// Incisors: rodent ORANGE (iron in the enamel). The bird: a grey cattle-tyrant with a pale yellow belly and a dark beak.
(function () {
  var SPECIES = {
    capybara:    { back: '#7a5536', side: '#8a6440', belly: '#a5825a', face: '#6e4c30', muzzle: '#5a3c26', leg: '#5e4129',
                   tail: '#2e2824', strand: 0.20, grain: 0.06 },
    neochoerus:  { back: '#5c4a38', side: '#6a5643', belly: '#8a755d', face: '#54432f', muzzle: '#463727', leg: '#463729',
                   tail: '#2e2824', strand: 0.20, grain: 0.06 },
    castoroides: { back: '#4a3324', side: '#583d2b', belly: '#6e5039', face: '#4e3626', muzzle: '#3e2a1d', leg: '#3a281c',
                   tail: '#262220', strand: 0.10, grain: 0.07 },
    beaver:      { back: '#6e4528', side: '#7e5232', belly: '#946a48', face: '#6a4428', muzzle: '#4e3220', leg: '#4a3020',
                   tail: '#2a2624', strand: 0.10, grain: 0.06 }
  };
  var name = window.ROD_SPECIES || 'capybara';
  var S = SPECIES[name];
  if (!S) return 'unknown species ' + name;
  var C = {};
  for (var k in S) if (typeof S[k] === 'string') C[k] = MM.hex(S[k]);
  C.nose = MM.hex('#201a16');
  C.tooth = MM.hex('#d8892a');
  C.lid = MM.hex('#2a221c');
  C.pupil = MM.hex('#0e0b09');
  C.glint = MM.hex('#7d6c55');
  C.claw = MM.hex('#2a221c');
  C.birdBody = MM.hex('#7c7a74');
  C.birdBelly = MM.hex('#e2d27a');
  C.birdBeak = MM.hex('#1e1c1a');

  MM.bounds();
  var em = MM.map['eye_left'];
  var eyeY = (em.lo.y + em.hi.y) / 2, eyeZ = (em.lo.z + em.hi.z) / 2;
  function is(n, s) { return n.indexOf(s) >= 0; }
  function side(f) { return f === 'side_l' || f === 'side_r'; }

  var cv = MM.paint(function (o) {
    var p = o.wp, n = o.part, f = o.face, col;
    var jj = o.h > 1 ? o.j / (o.h - 1) : 0.5;

    if (n === 'eye_left' || n === 'eye_right') {
      var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
      if (!outward) return [0, 0, 0, 0];
      if (p.y > eyeY) return C.lid.slice();
      return (p.z > eyeZ) ? C.glint.slice() : C.pupil.slice();
    }
    if (n === 'nose') return MM.cl(MM.mul(C.nose, 0.95 + 0.1 * MM.dith(o.u, o.v, 3)));
    if (n === 'incisors') return MM.cl(MM.mul(C.tooth, 0.92 + 0.12 * MM.dith(o.u, o.v, 4)));
    if (is(n, 'bird_')) {
      if (n === 'bird_beak') return MM.cl(C.birdBeak);
      col = (f === 'down' || (n === 'bird_body' && side(f) && jj > 0.55)) ? C.birdBelly : C.birdBody;
      if (n === 'bird_tail') col = MM.mul(C.birdBody, 0.8);
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 6)));
    }
    if (is(n, 'tail_')) {
      // bare scaly skin: random dark-edged scales (a per-texel hash, NOT a grid)
      col = C.tail;
      if (n === 'tail_base') col = MM.mix(C.back, C.tail, 0.35);
      else if (MM.h(o.u, o.v, 0, 83) > 0.62) col = MM.mul(col, 1.35);
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 8)));
    }
    if (is(n, '_foot')) {
      col = MM.mix(C.leg, C.claw, jj > 0.6 ? 0.5 : 0.15);
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    }
    if (is(n, 'ear_')) return MM.cl(MM.mul(MM.mix(C.face, C.nose, 0.3), 0.95 + 0.1 * MM.dith(o.u, o.v, 5)));

    if (n === 'head' || n === 'muzzle') {
      col = (n === 'muzzle') ? C.muzzle : C.face;
      if (f === 'down') col = MM.mix(col, C.belly, 0.4);
    } else if (is(n, 'fore_') || is(n, 'hind_')) {
      col = (is(n, 'upper') || is(n, 'thigh')) ? MM.mix(C.side, C.leg, 0.4 * jj) : C.leg;
    } else {
      var ven = (f === 'down') ? 1.0 : side(f) ? MM.smooth(0.6, 0.98, jj) : 0.0;
      col = MM.mix(C.side, C.back, f === 'up' ? 0.7 : 0.2);
      col = MM.mix(col, C.belly, ven);
    }

    // coat: coarse vertical strands (capybaras) or fine dense grain (beavers), plus soft mottling
    var strand = MM.h(o.u, Math.floor((o.v + 3 * MM.h(o.u, 1, 0, 9)) / 3), 0, 13);
    col = MM.mul(col, 1 + (strand - 0.5) * S.strand);
    col = MM.mul(col, 1 + (MM.dith(o.u, o.v, 21) - 0.5) * S.grain);
    col = MM.mul(col, 1 + (MM.fbm(p, 0.8, 3, 2) - 0.5) * 0.12);
    if (f === 'up') col = MM.mul(col, 0.82);
    if (f === 'down') col = MM.mul(col, 1.12);
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/large_rodent_' + name + '.png', cv);
  return name + ' painted, bled ' + MM.bledCount;
})();
