// Crocodilian skins (Phase 4.8, 2026-11-14) -- ONE painter, six species. Set window.CROC_SPECIES first; saves
// scratch/skins/crocodilian_<species>.png. Voay's horns are painted in every skin (shown per species in game, but an
// unpainted rect would let MM.bleed smear into the atlas).
//
// Crocodile skin, from real animals:
//   * the BACK is armour: rows of rectangular scutes. Painted as cells keyed on WORLD x/z with a dark groove round each
//     and a RANDOM shade per cell (cobbled, never an alternating two-tone grid -- the giant-monitor chessboard lesson);
//   * the FLANKS are small rounded scales: random low-contrast speckle plus soft blotches and dark spots;
//   * the BELLY is pale, with faint transverse lines (the rectangular belly scales);
//   * the TAIL carries dark CROSS-BANDS (world z, noise-warped) round its top and sides; the crest scutes are dark-tipped;
//   * the mouth lining (snout underside, jaw top: only seen with the mouth open) is pale yellow, the teeth ivory.
//   nile          olive-bronze back over khaki flanks, dark spots, banded tail, cream-yellow belly
//   saltwater     dark grey-olive back, yellowish flanks with dark streaks, black tail bands, pale belly
//   mugger        plain grey-brown, low contrast, faint bands
//   black_caiman  near-black, with PALE grey bands on the flanks, tail and lower jaw (inverse banding), pale grey belly
//   quinkana      (unknown) a dusty red-brown land hunter, dark bands
//   voay          (unknown) muddy brown-grey, heavily mottled, pale bony horns
(function () {
  var SPECIES = {
    nile:         { back: '#6a5d36', side: '#917e4a', belly: '#dccda0', head: '#6c5e3a', band: '#2f2f20', bandAmt: 0.70,
                    spot: '#2c2b1e', spotAmt: 0.55, streak: 0.0, jawBand: null, grain: 0.10, mottle: 0.14 },
    saltwater:    { back: '#33382f', side: '#787650', belly: '#e0d8b6', head: '#383d34', band: '#1e201a', bandAmt: 0.80,
                    spot: '#23251e', spotAmt: 0.35, streak: 0.55, jawBand: null, grain: 0.10, mottle: 0.12 },
    mugger:       { back: '#5c584c', side: '#6c6656', belly: '#cec4a4', head: '#605b4e', band: '#46423a', bandAmt: 0.40,
                    spot: '#45413a', spotAmt: 0.20, streak: 0.0, jawBand: null, grain: 0.09, mottle: 0.10 },
    black_caiman: { back: '#232320', side: '#2e2e2a', belly: '#b6b09a', head: '#272724', band: '#8a866e', bandAmt: 0.75,
                    spot: '#1a1a18', spotAmt: 0.0, streak: 0.0, jawBand: '#9a967e', grain: 0.12, mottle: 0.10 },
    quinkana:     { back: '#6a4e30', side: '#86663f', belly: '#cbb287', head: '#704f31', band: '#43301c', bandAmt: 0.65,
                    spot: '#3e2c1a', spotAmt: 0.30, streak: 0.0, jawBand: null, grain: 0.10, mottle: 0.12 },
    voay:         { back: '#4e4030', side: '#665440', belly: '#c4b08a', head: '#544433', band: '#35302a', bandAmt: 0.45,
                    spot: '#2e2a22', spotAmt: 0.45, streak: 0.0, jawBand: null, grain: 0.11, mottle: 0.22 }
  };
  var name = window.CROC_SPECIES || 'nile';
  var S = SPECIES[name];
  if (!S) return 'unknown species ' + name;
  var C = {};
  for (var k in S) if (typeof S[k] === 'string') C[k] = MM.hex(S[k]);
  C.tooth = MM.hex('#e6dcc0');
  C.mouth = MM.hex('#d8c890');
  C.claw = MM.hex('#1c1a16');
  C.iris = MM.hex('#bba63a');
  C.pupil = MM.hex('#0c0b09');
  C.horn = MM.hex('#b4a686');
  C.nostril = MM.hex('#1e1c18');

  MM.bounds();
  function is(n, s) { return n.indexOf(s) >= 0; }
  function side(f) { return f === 'side_l' || f === 'side_r'; }
  var ARMOUR = { scutes_back: 1, scutes_hips: 1, scutes_neck: 1 };
  var SNOUT = { head: 1, snout_base: 1, snout_tip: 1 };

  // the dorsal scute cell: dark groove on the cell edges, a random shade per cell
  function scute(col, p, cw, cl) {
    var gx = (p.x + 50) / cw, gz = (p.z + 100) / cl;
    var fx = gx - Math.floor(gx), fz = gz - Math.floor(gz);
    var shade = 0.86 + 0.24 * MM.h(Math.floor(gx), Math.floor(gz), 3, 41);
    col = MM.mul(col, shade);
    if (fz < 0.2 || fx < 0.12) col = MM.mul(col, 0.72);
    return col;
  }

  var cv = MM.paint(function (o) {
    var p = o.wp, n = o.part, f = o.face, col;
    var jj = o.h > 1 ? o.j / (o.h - 1) : 0.5;

    // EYE (v4): the zero-width plane is the eye, 3 x 2 texels facing out -- a gold iris with a black vertical slit pupil
    // down the middle, the top row shaded by the brow. The inward half is cut away (a width-0 box z-fights itself).
    if (n === 'eye_left' || n === 'eye_right') {
      var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
      if (!outward) return [0, 0, 0, 0];
      var ep = MM.map[n];
      if (Math.abs(p.z - (ep.lo.z + ep.hi.z) / 2) < 0.5) return C.pupil.slice();
      var shade = p.y > (ep.lo.y + ep.hi.y) / 2 ? 0.78 : 1.0;
      return MM.cl(MM.mul(C.iris, shade * (0.95 + 0.1 * MM.dith(o.u, o.v, 19))));
    }
    // the eye SOCKET: plain skin, a little darker on its outer face (the brow's shadow), knobbly on top
    if (is(n, 'eye_bump_')) {
      col = (f === 'up') ? scute(C.head, p, 1.1, 1.0) : MM.mul(C.head, side(f) ? 0.85 : 1.0);
      return MM.cl(MM.mul(col, 0.92 + 0.12 * MM.dith(o.u, o.v, 21)));
    }
    if (is(n, 'teeth')) {
      // individual teeth: every other texel along the row is cut away (a solid row read as a white stripe)
      if (f !== 'up' && Math.floor((p.z + 100) / 0.9) % 2 === 1) return [0, 0, 0, 0];
      return MM.cl(MM.mul(C.tooth, 0.92 + 0.12 * MM.dith(o.u, o.v, 5)));
    }
    if (is(n, 'horn_')) return MM.cl(MM.mul(C.horn, 0.9 + 0.18 * MM.dith(o.u, o.v, 7)));
    if (n === 'nose_knob') {
      col = (f === 'up') ? MM.mix(C.head, C.nostril, 0.55) : C.head;
      return MM.cl(MM.mul(col, 0.92 + 0.14 * MM.dith(o.u, o.v, 9)));
    }
    // inside the mouth: the snout underside and the jaw top. The face's EDGE texels keep the outside colour: box faces
    // with fractional depths share a texel column with their neighbour, and a pale mouth edge bled onto the snout top.
    var edge = o.i === 0 || o.j === 0 || o.i === o.w - 1 || o.j === o.h - 1;
    if (!edge && ((SNOUT[n] && n !== 'head' && f === 'down') || ((n === 'jaw' || n === 'jaw_tip') && f === 'up'))) {
      return MM.cl(MM.mul(C.mouth, 0.94 + 0.1 * MM.dith(o.u, o.v, 11)));
    }
    if (is(n, '_foot')) {
      var m = MM.map[n];
      var front = MM.smooth(m.lo.z + (m.hi.z - m.lo.z) * 0.3, m.lo.z, p.z);
      col = MM.mix(MM.mix(C.side, C.back, 0.5), C.claw, front > 0.55 ? 0.85 : 0.05);
      if (f === 'down') col = MM.mix(col, C.belly, 0.4);
    } else if (is(n, 'arm_') || is(n, 'leg_')) {
      col = MM.mix(C.side, C.back, f === 'up' || f === 'front' ? 0.5 : 0.2);
      if (f === 'down' || f === 'back') col = MM.mix(col, C.belly, 0.45);
      if (S.spotAmt > 0 && MM.h(Math.floor(p.x * 0.7 + 50), Math.floor(p.y * 0.7), Math.floor(p.z * 0.7 + 100), 17) > 0.78)
        col = MM.mix(col, C.spot, S.spotAmt);
    } else if (SNOUT[n] || n === 'jaw' || n === 'jaw_tip') {
      col = C.head;
      if (n === 'jaw' || n === 'jaw_tip') {
        // jaw sides stay close to the head colour (pale only underneath), so the ivory teeth stand out against them
        col = MM.mix(C.head, C.belly, f === 'down' ? 0.85 : (side(f) ? 0.15 + 0.35 * MM.smooth(0.5, 1.0, jj) : 0.3));
        // black caiman: pale grey bars on the lower jaw
        if (S.jawBand && side(f) && ((p.z + 100) % 2.6) < 1.1) col = MM.mix(col, C.jawBand, 0.8);
        else if (side(f) && MM.h(o.u, o.v, 0, 23) > 0.8) col = MM.mul(col, 0.7);   // dark speckles along the jaw
      } else {
        if (side(f) && jj > 0.7) col = MM.mix(col, C.belly, 0.35);                 // paler lip line
        if (f === 'up') col = scute(col, p, 1.1, 1.0);                             // knobbly skull and snout top
      }
    } else if (ARMOUR[n]) {
      col = MM.mix(C.back, C.side, side(f) ? 0.4 : 0.0);
      if (f === 'up') col = scute(col, p, 1.3, 1.6);
    } else if (is(n, '_crest')) {
      col = MM.mix(C.back, C.band, jj < 0.4 ? 0.55 : 0.0);                       // dark-tipped crest scutes
      col = MM.mul(col, 0.86 + 0.26 * MM.h(Math.floor((p.z + 100) / 1.4), 1, 0, 43));
    } else {
      // trunk, neck and tail: armoured back, scaly flanks, pale belly
      var ven = (f === 'down') ? 1.0 : side(f) ? MM.smooth(0.62, 0.98, jj) : 0.0;
      col = MM.mix(C.side, C.back, f === 'up' ? 0.85 : MM.smooth(0.6, 0.0, jj) * 0.7);
      col = MM.mix(col, C.belly, ven);
      if (f === 'up') col = scute(col, p, 1.4, 1.7);
      if (f === 'down' && ((p.z + 100) % 1.8) < 0.35) col = MM.mul(col, 0.9);    // belly scale rows
      // dark spots on the flanks (and the saltie's dark streaks)
      if (side(f) && ven < 0.5) {
        if (S.spotAmt > 0 && MM.fbm({x: p.x * 0.9, y: p.y * 1.4, z: p.z * 0.9}, 1.0, 2, 29) > 0.66) col = MM.mix(col, C.spot, S.spotAmt);
        if (S.streak > 0 && Math.abs(Math.sin(p.z * 0.9 + 2.0 * MM.fbm(p, 0.4, 2, 31))) > 0.93) col = MM.mix(col, C.spot, S.streak);
      }
      // tail cross-bands, wrapping the top and the sides, noise-warped so they are not ruler-straight
      if (is(n, 'tail') && f !== 'down') {
        var warp = 1.4 * (MM.fbm({x: p.x * 0.3, y: p.y * 0.3, z: p.z * 0.2}, 1.0, 2, 37) - 0.5);
        var ph = ((p.z + 100 + warp) / 4.4) % 1.0;
        var b = MM.smooth(0.52, 0.62, ph) * (1 - MM.smooth(0.88, 0.98, ph));
        col = MM.mix(col, C.band, S.bandAmt * b * (side(f) ? 1 - MM.smooth(0.55, 0.9, jj) : 1));
      } else if (S.jawBand && side(f) && ven < 0.6) {
        // black caiman: pale flank bars too
        var ph2 = ((p.z + 100) / 5.0) % 1.0;
        if (ph2 > 0.7 && ph2 < 0.86) col = MM.mix(col, C.band, 0.35);
      }
    }

    col = MM.mul(col, 1 + (MM.dith(o.u, o.v, 13) - 0.5) * S.grain);
    col = MM.mul(col, 1 + (MM.fbm(p, 1.2, 2, 5) - 0.5) * S.mottle);
    if (f === 'up') col = MM.mul(col, 0.9);
    if (f === 'down') col = MM.mul(col, 1.08);
    return MM.cl(col);
  });

  // protect the eyes AND the cut-away teeth, or the bleed refills the gaps (reference_bleed_protect_cleared_parts)
  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right', 'teeth_left', 'teeth_right', 'teeth_tip_left', 'teeth_tip_right']));
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/crocodilian_' + name + '.png', cv);
  return name + ' painted, bled ' + MM.bledCount;
})();
