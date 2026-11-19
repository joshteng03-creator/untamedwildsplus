// Giant monitor skins (Phase 4.3, 2026-10-10) -- ONE painter, two species. Set window.GM_SPECIES first; saves
// scratch/skins/giant_monitor_<species>.png.
//
// Varanid skin is small bead-like SCALES: random low-contrast texel speckle + soft mottling (never a regular pattern), darker along the back,
// paler on the throat and belly, darker and rougher on the legs, with dark claws at the toe ends.
//   megalania  dark olive with pale ochre CROSS-BANDS over the back and tail (the banding many large varanids keep)
//   komodo     plain dusky grey-brown (adult Komodos lose their juvenile pattern), a yellow tongue
// Bands are keyed on WORLD z and painted only on the torso and tail tops/sides -- constant-z bands checkerboard on
// limbs (skin-painting lesson), so the legs stay plain.
(function () {
  var SPECIES = {
    megalania: { back: '#4a4a36', side: '#5a573f', belly: '#b9ad84', leg: '#3f3e30', head: '#4e4c39',
                 band: '#a89a62', bandAmt: 0.75, tongue: '#2b2626', lip: '#8f8463', scale: 0.12 },
    komodo:    { back: '#5f574b', side: '#6e6557', belly: '#9c917c', leg: '#544c41', head: '#625a4d',
                 band: null, bandAmt: 0.0, tongue: '#d8b030', lip: '#8a7f6c', scale: 0.10 }
  };
  var name = window.GM_SPECIES || 'megalania';
  var S = SPECIES[name];
  if (!S) return 'unknown species ' + name;
  var C = {};
  for (var k in S) if (typeof S[k] === 'string') C[k] = MM.hex(S[k]);
  C.claw = MM.hex('#1f1c18');
  C.lid = MM.hex('#2c2820');
  C.pupil = MM.hex('#0e0c0a');
  C.glint = MM.hex('#8a7a52');

  MM.bounds();
  var em = MM.map['eye_left'];
  var eyeY = (em.lo.y + em.hi.y) / 2, eyeZ = (em.lo.z + em.hi.z) / 2;
  var TORSO = { body_belly: 1, body_chest: 1, body_hips: 1, neck: 1 };
  function is(n, s) { return n.indexOf(s) >= 0; }

  var cv = MM.paint(function (o) {
    var p = o.wp, n = o.part, f = o.face, col;
    var jj = o.h > 1 ? o.j / (o.h - 1) : 0.5;

    if (n === 'eye_left' || n === 'eye_right') {
      var outward = (p.x < 0) ? (o.wn[0] < 0) : (o.wn[0] > 0);
      if (!outward) return [0, 0, 0, 0];
      if (p.y > eyeY) return C.lid.slice();
      return (p.z > eyeZ) ? C.glint.slice() : C.pupil.slice();
    }
    if (n === 'tongue') return MM.cl(MM.mul(C.tongue, 0.92 + 0.12 * MM.dith(o.u, o.v, 3)));

    if (is(n, '_foot')) {
      // claws at the toe end (front 30% of the foot), rough dark scales behind
      var m = MM.map[n];
      var front = MM.smooth(m.lo.z + (m.hi.z - m.lo.z) * 0.32, m.lo.z, p.z);
      col = MM.mix(C.leg, C.claw, front > 0.5 ? 0.85 : 0.1);
    } else if (is(n, 'arm_') || is(n, 'leg_')) {
      col = MM.mix(C.leg, C.side, is(n, 'upper') ? 0.35 : 0.1);
      if (f === 'down') col = MM.mix(col, C.belly, 0.35);
    } else if (n === 'head' || n === 'snout' || n === 'jaw') {
      col = C.head;
      if (n === 'jaw') col = MM.mix(C.head, C.belly, f === 'down' ? 0.75 : 0.35);
      // pale lip line along the lower edge of the head and snout sides
      if ((n === 'head' || n === 'snout') && (f === 'side_l' || f === 'side_r') && jj > 0.75) col = MM.mix(col, C.lip, 0.7);
      if (f === 'down') col = MM.mix(col, C.belly, 0.6);
    } else {
      // torso + tail: dark back, sides, pale belly (local faces, the body is level so world-y would also do)
      var ven = (f === 'down') ? 1.0 : (f === 'side_l' || f === 'side_r') ? MM.smooth(0.6, 1.0, jj) : 0.0;
      col = MM.mix(C.side, C.back, f === 'up' ? 0.75 : 0.2);
      col = MM.mix(col, C.belly, ven);
      if (S.band && f !== 'down') {
        // cross-bands every 6u along the body/tail, ~2u wide, fading down the sides
        var ph = ((p.z + 40.0) / 6.0) % 1.0;
        var b = (ph > 0.62 && ph < 0.95) ? 1.0 : 0.0;
        var side = (f === 'side_l' || f === 'side_r') ? (1 - MM.smooth(0.35, 0.7, jj)) : 1.0;
        col = MM.mix(col, C.band, S.bandAmt * b * side * (TORSO[n] || is(n, 'tail') ? 1 : 0));
      }
    }

    // scales: RANDOM per-texel speckle plus soft mottling. (The first pass alternated light/dark on every texel --
    // a regular two-tone bead pattern -- and the user rightly said both skins "look like a chessboard".)
    col = MM.mul(col, 1 + (MM.dith(o.u, o.v, 13) - 0.5) * S.scale);
    col = MM.mul(col, 1 + (MM.fbm(p, 1.6, 2, 5) - 0.5) * 0.14);
    col = MM.mul(col, 1 + (MM.fbm(p, 0.5, 3, 7) - 0.5) * 0.12);
    if (f === 'up') col = MM.mul(col, 0.80);
    if (f === 'down') col = MM.mul(col, 1.15);
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/giant_monitor_' + name + '.png', cv);
  return name + ' painted, bled ' + MM.bledCount;
})();
