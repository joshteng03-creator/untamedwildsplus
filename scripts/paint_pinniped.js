// Pinniped skins (Phase 4.4, 2026-10-10) -- ONE painter, five species + two white-coat pup skins. Set
// window.PIN_SPECIES first; saves scratch/skins/pinniped_<species>.png ("<species>_pup" for the pup coats).
//
// Shading is keyed on each body box's LOCAL faces (down = belly), as on the kangaroo: the sea lion's raised stance
// tilts the chest, so world-y countershading would put the pale belly on its chest front.
// Patterns use texel-space cells, never regular per-texel alternation (the giant-monitor "chessboard" lesson).
(function () {
  var SPECIES = {
    // Odobenus rosmarus -- cinnamon-pink wrinkled skin, sparse hair, a cream bristle pad, ivory tusks.
    walrus: { back: '#86563f', side: '#9c6650', belly: '#b47d66', head: '#946049', flipper: '#6a4434',
              whisker: '#e2d3b4', grain: 0.07, folds: 0.10 },
    // Erignathus barbatus -- grey-brown, darker back, a paler face, the long pale beard bristles.
    bearded_seal: { back: '#5e5650', side: '#7a716a', belly: '#9c948a', head: '#8a8178', flipper: '#3f3934',
                    whisker: '#ddd4c4', grain: 0.06 },
    // Pusa hispida -- dark grey back covered in pale-RINGED dark spots, a silvery belly.
    ringed_seal: { back: '#4c4f52', side: '#62666a', belly: '#b9bcbc', head: '#55585b', flipper: '#35383a',
                   ring: '#b0b4b4', spot: '#2c2e30', grain: 0.05 },
    // Pagophilus groenlandicus -- silver-grey with the black HARP saddle (two flank bands meeting over the shoulders)
    // and a black face.
    harp_seal: { back: '#a9adad', side: '#b9bcbb', belly: '#cfd1cf', head: '#1f2022', flipper: '#3a3b3c',
                 harp: '#202123', grain: 0.05 },
    // Eumetopias jubatus -- tawny golden-buff, a paler chest, dark flippers.
    steller_sea_lion: { back: '#8c6a43', side: '#a37d52', belly: '#c09a6c', head: '#9a774c', flipper: '#3b2e24',
                        grain: 0.06 },
    // white-coat pups (lanugo): near-white fluffy coat, faint warm-grey shading, dark eyes and nose
    harp_seal_pup: { back: '#e3e1da', side: '#ebe9e3', belly: '#f2f0ea', head: '#e6e4dd', flipper: '#cfccc4',
                     grain: 0.04 },
    ringed_seal_pup: { back: '#dad8d2', side: '#e4e2dc', belly: '#efede7', head: '#dedcd5', flipper: '#c7c4bc',
                       grain: 0.04 }
  };
  var name = window.PIN_SPECIES || 'walrus';
  var S = SPECIES[name];
  if (!S) return 'unknown species ' + name;
  var C = {};
  for (var k in S) if (typeof S[k] === 'string') C[k] = MM.hex(S[k]);
  C.nose = MM.hex('#1d1a18');
  C.tusk = MM.hex('#e6dcc2');
  C.lid = MM.hex('#2a2622');
  C.pupil = MM.hex('#0c0b0a');
  C.glint = MM.hex('#86806e');
  C.ear = C.flipper;

  MM.bounds();
  var em = MM.map['eye_left'];
  var eyeY = (em.lo.y + em.hi.y) / 2, eyeZ = (em.lo.z + em.hi.z) / 2;
  var BODY = { body_chest: 1, body_belly: 1, body_hips: 1, neck: 1 };
  function is(n, s) { return n.indexOf(s) >= 0; }
  function side(f) { return f === 'side_l' || f === 'side_r'; }
  function spotAt(u, v) { return MM.h(u, v, 0, 61) > 0.94; }

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
    if (is(n, 'tusk')) return MM.cl(MM.mul(C.tusk, 0.9 + 0.12 * MM.smooth(0, 1, jj) + 0.04 * MM.dith(o.u, o.v, 4)));
    if (n === 'whiskers') {
      var wc = C.whisker || C.head;
      // bristle rows: short vertical strokes in texel columns, low contrast
      col = MM.mix(wc, MM.mul(wc, 0.8), (o.u % 2 === 0 && MM.dith(o.u, 0, 11) > 0.4) ? 0.5 : 0.0);
      return MM.cl(MM.mul(col, 0.97 + 0.06 * MM.dith(o.u, o.v, 5)));
    }
    if (is(n, 'ear_')) return MM.cl(MM.mul(C.ear, 0.95 + 0.1 * MM.dith(o.u, o.v, 5)));

    if (is(n, 'fore_') || is(n, 'hind_')) {
      col = C.flipper;
      if (f === 'down') col = MM.mix(col, C.belly, 0.25);
    } else if (n === 'head' || n === 'muzzle') {
      col = C.head;
      if (f === 'down') col = MM.mix(col, C.belly, S.harp ? 0.15 : 0.5);
    } else {
      var ven = (f === 'down') ? 1.0 : side(f) ? MM.smooth(0.55, 0.95, jj) : 0.0;
      col = MM.mix(C.side, C.back, f === 'up' ? 0.75 : 0.2);
      col = MM.mix(col, C.belly, ven);

      // ringed seal: the real pattern is pale, wavy, LOOP-like rings marbled over a dark back. Drawn as contour lines of
      // a soft noise field (pale where the field crosses a few iso-levels), which gives irregular closed loops of every
      // size. (v1 grid squares read as polka dots; v2 dark dots with pale halos "looked off" -- user.)
      if (S.ring && BODY[n] && ven < 0.6) {
        var fld = MM.fbm({ x: p.x * 1.0, y: p.y * 1.0, z: p.z * 1.0 }, 0.55, 2, 37) * 4.0;
        var dist = Math.abs(fld - Math.round(fld));
        if (dist < 0.11) col = MM.mix(col, C.ring, 0.7 * (1 - ven));
      }
      // harp seal: the harp/lyre is seen from ABOVE -- two dark lines that meet over the shoulders, spread back along the
      // top of the body and run down onto the upper flanks toward the hips. (v1/v2 were bars on the sides only.)
      if (S.harp && (n === 'body_chest' || n === 'body_belly' || n === 'body_hips')) {
        var t = Math.min(1, Math.max(0, (p.z + 4.0) / 12.0));          // 0 at the shoulders .. 1 at mid-body
        var hw = MM.map[n].hi.x;                                       // this box's half-width
        var xc = 0.6 + (hw - 0.9) * Math.sin(t * Math.PI / 2);         // the two lines diverge toward the flanks
        var onTop = f === 'up' && p.z > -4.5 && p.z < 9.0 && Math.abs(Math.abs(p.x) - xc) < 1.35;
        var bc = 0.08 + 0.30 * Math.min(1, Math.max(0, (p.z - 6.0) / 9.0));   // ...then down the upper flank
        var onFlank = side(f) && p.z > 5.0 && p.z < 16.0 && Math.abs(jj - bc) < 0.16;
        if (onTop || onFlank) col = MM.mix(col, C.harp, 0.9);
      }
      // walrus: skin folds -- short irregular creases (a crease runs a few texels along a row, then breaks), never a
      // fixed every-Nth-row stripe (the first pass read as regular stripes)
      if (S.folds && MM.h(Math.floor(o.u / 4), o.v, 0, 71) > 0.72) col = MM.mul(col, 1 - S.folds);
    }

    col = MM.mul(col, 1 + (MM.dith(o.u, o.v, 13) - 0.5) * S.grain);
    col = MM.mul(col, 1 + (MM.fbm(p, 1.2, 2, 5) - 0.5) * 0.10);
    if (f === 'up') col = MM.mul(col, 0.82);
    if (f === 'down') col = MM.mul(col, 1.12);
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/pinniped_' + name + '.png', cv);
  return name + ' painted, bled ' + MM.bledCount;
})();
