// Wildebeest skins (Phase 4.7, 2026-10-23) -- ONE painter, four species. Set window.WB_SPECIES first; saves
// scratch/skins/wildebeest_<species>.png. Every horn family, the mane, beard, chest fringe and nose tuft are painted in each
// skin (only the species' own show in game, but an unpainted rect would let MM.bleed smear into the atlas).
//
// Real-animal cues:
//   blue wildebeest   slate blue-grey, dark irregular BRINDLE bars over the neck and forequarters fading toward the rump,
//                     black face, mane, beard and tail tassel; dark grey cow-like horns
//   black wildebeest  dark brown-black all over, a CREAM mane with dark roots, the WHITE tail, black beard/fringe/nose tuft
//   megalotragus      (coat unknown -- a plain tawny grey-brown) paler belly, darker face, brown mane, pale ringed horns
//   hartebeest        red-fawn, paler hindquarters and belly, a dark BLAZE down the face, dark fronts to the forelegs,
//                     black tassel, black heavily ringed horns
// Brindle bars are a noise-warped sine along world z (never a regular per-texel pattern -- the chessboard lesson), and
// they are kept OFF the legs (constant-z bands checkerboard on limbs).
(function () {
  var SPECIES = {
    blue_wildebeest:  { back: '#42464a', side: '#4c5154', belly: '#6c7072', leg: '#4e4a46', face: '#2c2a28', muzzle: '#1e1c1b',
                        mane: '#1a1918', maneTip: null, beard: '#1a1918', tassel: '#151413', dock: '#4a4c4e', horn: '#3a3a3a',
                        ear: '#3c3e40', brindle: '#2e2c2a', blaze: null, legFront: null, rumpPale: null, rings: 0.10, grain: 0.07 },
    black_wildebeest: { back: '#2e2620', side: '#352c25', belly: '#4a3e34', leg: '#2a231e', face: '#231d19', muzzle: '#1a1613',
                        mane: '#1e1915', maneTip: '#e2d8c2', beard: '#171310', tassel: '#ece6d8', dock: '#3a322a', horn: '#2a2622',
                        ear: '#2e2620', brindle: null, blaze: null, legFront: null, rumpPale: null, rings: 0.10, grain: 0.08 },
    megalotragus:     { back: '#7e6e58', side: '#8c7b63', belly: '#b8a88c', leg: '#5e5242', face: '#5a4e40', muzzle: '#3e362e',
                        mane: '#4a3e32', maneTip: null, beard: '#4a3e32', tassel: '#2e2822', dock: '#7a6a54', horn: '#9a8a70',
                        ear: '#6e604c', brindle: null, blaze: null, legFront: null, rumpPale: null, rings: 0.22, grain: 0.07 },
    hartebeest:       { back: '#9c6440', side: '#a8714a', belly: '#d2b48c', leg: '#946042', face: '#9a6644', muzzle: '#7a5036',
                        mane: '#8a5a3a', maneTip: null, beard: '#8a5a3a', tassel: '#1a1614', dock: '#a8714a', horn: '#1e1b18',
                        ear: '#a0704c', brindle: null, blaze: '#2e221a', legFront: '#2e241c', rumpPale: '#c69a6c', rings: 0.28,
                        grain: 0.06 }
  };
  var name = window.WB_SPECIES || 'blue_wildebeest';
  var S = SPECIES[name];
  if (!S) return 'unknown species ' + name;
  var C = {};
  for (var k in S) if (typeof S[k] === 'string') C[k] = MM.hex(S[k]);
  C.nose = MM.hex('#141210');
  C.lid = MM.hex('#1c1916');
  C.pupil = MM.hex('#0c0a09');
  C.glint = MM.hex('#5a4a3a');
  C.hoof = MM.hex('#1c1916');
  C.earIn = MM.hex('#d8ccb8');

  MM.bounds();
  var em = MM.map['eye_left'];
  var eyeY = (em.lo.y + em.hi.y) / 2, eyeZ = (em.lo.z + em.hi.z) / 2;
  var rumpLo = MM.map['body_rump'].lo.z;
  function is(n, s) { return n.indexOf(s) >= 0; }
  function side(f) { return f === 'side_l' || f === 'side_r'; }
  var HORN = ['gnu_', 'gnou_', 'megalo_', 'hart_'];
  function horn(n) { for (var i = 0; i < HORN.length; i++) if (n.indexOf(HORN[i]) === 0) return true; return false; }
  var BRINDLE_PARTS = { neck_base: 1, neck: 1, body_chest: 1, body_withers: 1, body_barrel: 1 };

  var cv = MM.paint(function (o) {
    var p = o.wp, n = o.part, f = o.face, col;
    var jj = o.h > 1 ? o.j / (o.h - 1) : 0.5;

    if (n === 'eye_left' || n === 'eye_right') {
      var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
      if (!outward) return [0, 0, 0, 0];
      if (p.y > eyeY) return C.lid.slice();
      return (p.z > eyeZ) ? C.glint.slice() : C.pupil.slice();
    }
    if (n === 'nose') return MM.cl(MM.mul(C.nose, 0.94 + 0.12 * MM.dith(o.u, o.v, 3)));
    if (horn(n)) {
      // growth rings: a darker band every 3rd texel along the length; ringing strength per species
      col = C.horn;
      if ((side(f) || f === 'front' || f === 'back') && o.j % 3 === 0) col = MM.mul(col, 1 - S.rings);
      if (n === 'hart_pedicle') col = MM.mix(C.face, C.horn, 0.25);   // the pedicle is bone under skin and hair
      if (f === 'up') col = MM.mul(col, 0.92);
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 9)));
    }
    if (n === 'mane' || n === 'mane_base') {
      // hair runs up the face: dark roots, the black wildebeest's pale cream tips
      col = (S.maneTip && jj < 0.55) ? C.maneTip : C.mane;
      return MM.cl(MM.mul(col, 0.88 + 0.24 * MM.h(o.u, Math.floor(o.v / 2), 0, 31)));
    }
    if (n === 'beard' || n === 'chest_fringe' || n === 'nose_tuft') {
      col = (n === 'nose_tuft') ? C.muzzle : C.beard;
      return MM.cl(MM.mul(col, 0.86 + 0.28 * MM.h(o.u, Math.floor(o.v / 2), 0, 37)));
    }
    if (n === 'tail' || n === 'tail_tassel') {
      col = (n === 'tail') ? MM.mix(C.dock, C.tassel, MM.smooth(0.55, 1.0, jj) * 0.7) : C.tassel;
      // long hanging hair: vertical strands
      return MM.cl(MM.mul(col, 0.88 + 0.24 * MM.h(o.u, Math.floor(o.v / 4), 0, 41)));
    }
    if (is(n, '_hoof')) {
      col = MM.mix(C.hoof, C.leg, jj < 0.3 ? 0.35 : 0.0);
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 3)));
    }
    if (is(n, 'ear_')) {
      col = (f === 'down') ? MM.mix(C.ear, C.earIn, 0.55) : C.ear;
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 5)));
    }

    if (n === 'head' || n === 'muzzle') {
      col = (n === 'muzzle') ? C.muzzle : C.face;
      if (f === 'down') col = MM.mix(col, C.side, 0.3);
      // hartebeest blaze: the forward-facing (local up) face of head and muzzle, a dark band down the middle
      if (S.blaze && f === 'up' && Math.abs(p.x) < 1.15) col = MM.mix(col, C.blaze, 0.85);
      if (S.blaze && side(f) && jj < 0.25) col = MM.mix(col, C.blaze, 0.35);
    } else if (is(n, 'fore_') || is(n, 'hind_')) {
      var upperSeg = is(n, 'upper') || is(n, 'thigh');
      col = upperSeg ? MM.mix(C.side, C.leg, 0.4 * jj) : C.leg;
      if (S.rumpPale && is(n, 'hind_') && upperSeg) col = MM.mix(col, C.rumpPale, 0.5);
      if (S.legFront && f === 'front' && (is(n, 'upper') || is(n, 'forearm') || is(n, 'gaskin'))) col = MM.mix(col, C.legFront, 0.8);
    } else {
      var ven = (f === 'down') ? 1.0 : side(f) ? MM.smooth(0.62, 0.98, jj) : 0.0;
      col = MM.mix(C.side, C.back, f === 'up' ? 0.75 : 0.2);
      col = MM.mix(col, C.belly, ven);
      if (S.rumpPale && n === 'body_rump') col = MM.mix(col, C.rumpPale, f === 'up' ? 0.3 : 0.55);
      if (S.rumpPale && n === 'body_barrel' && side(f)) col = MM.mix(col, C.rumpPale, 0.4 * MM.smooth(-2, 4.6, p.z));
      // the blue wildebeest's brindle: irregular dark vertical bars, strongest on the neck and shoulders, gone by the rump
      if (S.brindle && BRINDLE_PARTS[n] && side(f)) {
        var warp = 2.6 * (MM.fbm({x: p.x * 0.2, y: p.y * 0.45, z: p.z * 0.25}, 1.0, 3, 17) - 0.5);
        var s = Math.sin(p.z * 2.1 + warp * 2.4 + (p.x < 0 ? 0.9 : 0.0));
        var fade = MM.smooth(5.0, -4.0, p.z) * (1 - 0.6 * ven);
        if (s > 0.5) col = MM.mix(col, C.brindle, fade * MM.smooth(0.5, 0.8, s) * 0.8);
      }
    }

    // coat: short fine grain plus soft mottling
    col = MM.mul(col, 1 + (MM.dith(o.u, o.v, 13) - 0.5) * S.grain);
    col = MM.mul(col, 1 + (MM.fbm(p, 1.1, 2, 5) - 0.5) * 0.10);
    if (f === 'up') col = MM.mul(col, 0.86);
    if (f === 'down') col = MM.mul(col, 1.08);
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/wildebeest_' + name + '.png', cv);
  return name + ' painted, bled ' + MM.bledCount;
})();
