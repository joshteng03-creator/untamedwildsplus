// Toothed whale skins (2.9.0) on the orca rig (17 boxes, 256x128). window.ORCA_SPECIES picks the species.
// The orca's identity IS its pattern, so it is painted CRISP (hard black/white edges, only a faint tone drift) and keyed
// on WORLD position, so one rule runs continuously across the stacked boxes:
//   * white eye patch: an oval above and behind the eye, on the head's side faces only;
//   * white throat + belly: from the lower jaw back along the underside, NARROW between the flippers, then sweeping up
//     the flanks behind the dorsal fin into the flank patches, closing to a thin stripe under the tail stock;
//   * grey saddle patch on the back just behind the dorsal fin; the flukes white underneath with dark rims;
//   * fins, dorsal, back: black.
// The false killer whale is near-uniform dark grey-black with a pale grey throat blaze between the flippers.
// The inward half of each eye plane is cut AFTER the bleed (it z-fights its twin otherwise).
(function () {
  var SP = window.ORCA_SPECIES || 'orca';
  var H = MM.hex;
  var BLACK = H('#17181c'), WHITE = H('#e6e4dd'), SADDLE = H('#7c7f86'), RIM = H('#24252a');
  var FKW = H('#3a3b41'), FKW_BELLY = H('#4a4c53'), FKW_BLAZE = H('#8a8c92');   // #2b2c31 read as a black void
  var EYE = H('#0c0c0e');
  var CUT = {};
  var TW = 256;

  // underside white boundary: how high (world y) the white reaches at body position z
  var bellyTop = function (z, x) {
    var ax = Math.abs(x);
    if (z < -11) return 3.4;                                   // throat and lower jaw
    if (z < 7) return ax < 3.0 ? 2.7 : 1.4;                    // narrow ventral band between the flippers
    if (z < 18) {                                              // the flank patches sweep up behind the dorsal
      var t = (z - 7) / 11;
      return 2.6 + 4.6 * Math.sin(Math.PI * Math.min(1, t * 1.25));
    }
    return 4.4;                                                // thin stripe under the tail stock (tail sits higher)
  };
  var isWhite = function (o) {
    var p = o.wp, n = o.part;
    if (n.indexOf('fin_') === 0 || n.indexOf('dorsal') === 0) return false;
    if (n.indexOf('fluke_') === 0) {
      if (o.wn[1] > -0.5) return false;
      var m = MM.map[n];
      return Math.abs(p.x) < Math.abs(m.hi.x > 0 ? m.hi.x : m.lo.x) - 1.2 && p.z < m.hi.z - 0.6;   // dark rim
    }
    if (n === 'jaw') return o.wn[1] < 0.5 || p.y < 2.6;
    if (n === 'tail_2') return o.wn[1] < -0.5 && Math.abs(p.x) < 1.0;
    if (n === 'tail_1') return o.wn[1] < -0.5 || (Math.abs(o.wn[0]) > 0.5 && p.y < 4.2 && p.z < 21.5);
    if (o.wn[1] < -0.5) return n !== 'head_melon' && (p.z < 7 ? Math.abs(p.x) < 3.2 : true);
    return p.y < bellyTop(p.z, p.x);
  };
  var eyePatch = function (o) {
    if (o.part !== 'head' || Math.abs(o.wn[0]) < 0.5) return false;
    var dz = (o.wp.z + 12.6) / 2.4, dy = (o.wp.y - 7.4) / 1.15;
    return dz * dz + dy * dy < 1.0;
  };
  var saddle = function (o) {
    var p = o.wp;
    if (!(o.part === 'body_main' || o.part === 'body_ridge' || o.part === 'body_rear')) return false;
    if (o.wn[1] < 0.3 && p.y < 10.6) return false;
    var dz = (p.z - 7.0) / 3.6, dx = p.x / 3.2;
    return dz > -1 && dz * dz + dx * dx < 1.0;
  };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, m = MM.map[n], col;
    var i0 = o.v * TW + o.u;
    if (n.indexOf('eye_') === 0) {
      var out = o.wn[0] * (p.x < 0 ? -1 : 1) > 0;
      if (!out) { CUT[i0] = true; return [0, 0, 0, 255]; }
      return EYE;
    }
    if (SP === 'orca') {
      if (eyePatch(o)) col = WHITE;
      else if (isWhite(o)) col = WHITE;
      else if (saddle(o)) col = SADDLE;
      else col = BLACK;
      if (n.indexOf('fluke_') === 0 && col === BLACK && o.wn[1] < -0.5) col = RIM;
    } else {
      col = FKW;
      if (o.wn[1] < -0.5 || p.y < 2.2) col = FKW_BELLY;
      if (p.z > -12 && p.z < -3 && (o.wn[1] < -0.5 || p.y < 1.8) && Math.abs(p.x) < 2.6 - Math.abs(p.z + 7.5) * 0.25) col = FKW_BLAZE;
      if (n === 'jaw' && o.wn[1] < -0.5) col = MM.mix(FKW_BELLY, FKW_BLAZE, 0.4);
    }
    // a faint broad tone drift so large faces are not flat plastic -- kept tiny: the pattern must stay crisp
    col = MM.mul(col, 0.97 + 0.06 * MM.fbm(p, 0.25, 2, 7));
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, TW, 128), cut = 0;
  for (var key in CUT) {
    img.data[key * 4 + 3] = 0;
    cut++;
  }
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/orca_' + SP + '.png', cv);
  return SP + ' painted, ' + cut + ' texels cut';
})();
