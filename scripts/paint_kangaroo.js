// Kangaroo skins (Phase 4.2, 2026-10-09) -- ONE painter, five species. Set window.KANG_SPECIES before evaluating;
// saves scratch/skins/kangaroo_<species>.png. Paint the sthenurines (procoptodon, sthenurus) on the sthenurine rig
// build (short muzzle, long arms) so the world-keyed rules land where the game will draw them.
//
// The posture is UPRIGHT, so "ventral" cannot be world-low: the belly and chest face forward-down. Countershading
// is therefore keyed on each torso box's LOCAL faces -- its 'down' face is the belly/throat and the lower band of
// its side faces fades pale -- not on world y. Macropod fur is short, soft and dense: fine low-contrast grain, no
// strands, no clumps.
//
// Conventions carried from the other new types: face lighting compensated by multiplying (up 0.78, down 1.18); the
// zero-width eye planes alpha-0 their INWARD quad and MM.bleed protects them; hidden parts (claws on the three
// non-sthenurines, the joey) are painted anyway so the atlas has no transparent rect for the bleed to smear into.
(function () {
  var SPECIES = {
    // Osphranter rufus -- the arid-zone giant. Rufous-red above, cream below; a white whisker stripe with a dark
    // spot on the muzzle; pale tail tip; dark paws and toes.
    red: { back: '#9a4a28', side: '#ad5a33', belly: '#e2d3b8', face: '#a5603e', muzzle: '#c9a68a',
           whisker: '#efe8dc', spot: '#2e2420', paw: '#3a2a22', foot: '#7a4a32', toe: '#2e231d',
           earBack: '#7e3c22', earIn: '#d8b8a4', tailTip: '#d9c7ad', tailTipAmt: 0.6, grain: 0.07 },
    // Macropus giganteus -- soft cool grey-brown, paler below, dark paws, feet and tail tip; plain face with a
    // pale eye-ring area; ears pale inside.
    eastern_grey: { back: '#7a7067', side: '#8d847a', belly: '#d6d0c4', face: '#857b71', muzzle: '#b2a99d',
                    whisker: null, spot: null, paw: '#2c2724', foot: '#5e554d', toe: '#2a2522',
                    earBack: '#5e554c', earIn: '#c9beb2', tailTip: '#2c2724', tailTipAmt: 0.75, grain: 0.06 },
    // Procoptodon goliah -- arid inland giant. Pale ochre-buff, sand-pale below; a dark short-faced MASK (the
    // species' shape cue, given colour too); dark claws and paws.
    procoptodon: { back: '#a8875a', side: '#ba9b6c', belly: '#e6d8bb', face: '#6a5038', muzzle: '#4d3a2a',
                   whisker: null, spot: null, paw: '#2b221b', foot: '#8a6c48', toe: '#2b221b',
                   earBack: '#7e6242', earIn: '#d9c6a6', tailTip: '#7e6242', tailTipAmt: 0.45, grain: 0.08 },
    // Sthenurus stirlingi -- the dark end of the set: deep chocolate brown, warm tan below, darker face.
    sthenurus: { back: '#4a3426', side: '#5a4030', belly: '#a88a6c', face: '#3f2c20', muzzle: '#33251b',
                 whisker: null, spot: null, paw: '#211813', foot: '#3e2d22', toe: '#211813',
                 earBack: '#3a2a1f', earIn: '#8f725a', tailTip: '#2a1f18', tailTipAmt: 0.5, grain: 0.07 },
    // Protemnodon anak -- heavy forest giant wallaby. Grizzled dark grey-brown with a RUFOUS neck-and-shoulder
    // mantle (the red-necked-wallaby pattern) and a pale cheek stripe.
    protemnodon: { back: '#56514c', side: '#67615a', belly: '#bdb3a4', face: '#5a544e', muzzle: '#8a8176',
                   whisker: '#cfc6b8', spot: null, paw: '#24201d', foot: '#4a4540', toe: '#24201d',
                   earBack: '#4a4540', earIn: '#b0a497', tailTip: '#2a2622', tailTipAmt: 0.55, grain: 0.09,
                   mantle: '#9a5a36' }
  };
  var name = window.KANG_SPECIES || 'red';
  var S = SPECIES[name];
  if (!S) return 'unknown species ' + name;
  var C = {};
  for (var k in S) if (typeof S[k] === 'string') C[k] = MM.hex(S[k]);
  C.lid = MM.hex('#2a221d');
  C.pupil = MM.hex('#0f0c0a');
  C.glint = MM.hex('#7d6f58');
  C.nose = MM.hex('#1e1a18');
  C.claw = MM.hex('#2a241f');

  MM.bounds();
  var em = MM.map['eye_left'];
  var eyeY = (em.lo.y + em.hi.y) / 2, eyeZ = (em.lo.z + em.hi.z) / 2;

  var TORSO = { body_hips: 1, body_chest: 1, neck: 1 };
  var HEAD = { head: 1, muzzle: 1 };
  function is(n, s) { return n.indexOf(s) >= 0; }

  var cv = MM.paint(function (o) {
    var p = o.wp, n = o.part, col, f = o.face;
    var jj = o.h > 1 ? o.j / (o.h - 1) : 0.5;           // 0 at a side face's local top, 1 at its bottom

    if (n === 'eye_left' || n === 'eye_right') {
      var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
      if (!outward) return [0, 0, 0, 0];
      if (p.y > eyeY) return C.lid.slice();
      return (p.z > eyeZ) ? C.glint.slice() : C.pupil.slice();
    }
    if (n === 'nose') return MM.cl(MM.mul(C.nose, 0.95 + 0.1 * MM.dith(o.u, o.v, 3)));
    if (is(n, 'claw')) return MM.cl(MM.mul(C.claw, 0.92 + 0.14 * MM.dith(o.u, o.v, 3)));
    if (n === 'joey_head' || n === 'joey_snout') {
      col = n === 'joey_snout' ? MM.mix(C.muzzle, C.belly, 0.4) : MM.mix(C.side, C.belly, 0.35);
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    if (is(n, 'ear_')) {
      // inner ear on the FRONT face (pale), dark back and a darker tip
      col = (f === 'front') ? MM.mix(C.earIn, C.earBack, 0.25 * jj) : C.earBack;
      if (f !== 'front') col = MM.mix(col, C.toe, 0.35 * (1 - jj));
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 5)));
    }

    if (TORSO[n]) {
      var ven = (f === 'down') ? 1.0 : (f === 'side_l' || f === 'side_r') ? MM.smooth(0.55, 0.95, jj) : 0.0;
      col = MM.mix(C.side, C.back, f === 'up' ? 0.7 : 0.25);
      col = MM.mix(col, C.belly, ven);
      if (S.mantle && (n === 'neck' || n === 'body_chest')) col = MM.mix(col, C.mantle, 0.55 * (1 - ven));
    } else if (HEAD[n]) {
      col = C.face;
      if (n === 'muzzle') col = MM.mix(C.face, C.muzzle, 0.6);
      if (f === 'down') col = MM.mix(col, C.belly, 0.7);                 // pale chin and throat
      // whisker stripe along the side of the muzzle (red: white with a dark spot; protemnodon: pale cheek stripe)
      if (S.whisker && (f === 'side_l' || f === 'side_r') && (n === 'muzzle' || p.z < (MM.map.head.lo.z + 1.5))) {
        var band = 1 - Math.min(1, Math.abs(jj - 0.6) / 0.22);
        col = MM.mix(col, C.whisker, 0.85 * Math.max(0, band));
        if (S.spot && n === 'muzzle' && jj > 0.35 && jj < 0.6 && o.i % 3 === 1) col = MM.mix(col, C.spot, 0.6);
      }
    } else if (is(n, 'arm_')) {
      col = is(n, 'hand') ? C.paw : MM.mix(C.side, C.belly, is(n, 'upper') ? 0.15 : 0.3);
      if (is(n, 'fore')) col = MM.mix(col, C.paw, 0.35 * jj);
    } else if (is(n, 'leg_')) {
      if (is(n, 'toe')) col = C.toe;
      else if (is(n, 'foot')) col = MM.mix(C.foot, C.toe, 0.3);
      else if (is(n, 'shin')) col = MM.mix(C.side, C.foot, 0.45 + 0.4 * jj);
      else {
        // the haunch: body coat, the inner face and lower band pale like the belly
        var vt = (f === 'side_l' || f === 'side_r') ? ((p.x < 0) === (f === 'side_r') ? 0.6 : 0.0) : 0.0;
        col = MM.mix(MM.mix(C.side, C.back, 0.3), C.belly, Math.max(vt, f === 'down' ? 0.8 : 0.0));
      }
    } else if (is(n, 'tail_')) {
      col = MM.mix(C.side, C.back, f === 'up' ? 0.5 : 0.15);
      if (f === 'down') col = MM.mix(col, C.belly, 0.5);
      if (n === 'tail_3') col = MM.mix(col, C.tailTip, S.tailTipAmt);
      if (n === 'tail_2') col = MM.mix(col, C.tailTip, S.tailTipAmt * 0.25 * jj);
    } else {
      col = C.side;
    }

    // fine, dense, soft fur: low-contrast grain only
    col = MM.mul(col, 1 + (MM.fbm(p, 1.1, 3, 2) - 0.5) * 2 * S.grain);
    col = MM.mul(col, 1 + (MM.dith(o.u, o.v, 9) - 0.5) * S.grain);
    if (f === 'up') col = MM.mul(col, 0.78);
    if (f === 'down') col = MM.mul(col, 1.18);
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/kangaroo_' + name + '.png', cv);
  return name + ' painted, bled ' + MM.bledCount;
})();
