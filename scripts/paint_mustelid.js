// Mustelid skins (2.9.0) on the mustelid rig (26 boxes, 128x64). window.MU_SPECIES picks the species; needs woollib.js.
// Fur is woollib's messy hair (the approved coat: low contrast, no repeating locks), SHORT locks for this small model.
// Every marking keys on WORLD position so it runs continuously across the stacked boxes, and its edge is broken by a
// hash so it never reads as a ruled line:
//   wolverine  -- dark chocolate coat; the pale buff STRIPE from each shoulder along the flank, the two meeting over the
//                rump at the tail base; a silver-grey band across the forehead between eyes and ears; dark muzzle, near-
//                black legs and paws, pale claw tips, a small pale throat blaze;
//   honey badger -- short black coat; a crisp silver-white MANTLE from the crown of the head over the neck, back and the
//                top of the tail, its lower edge at mid-flank; everything below it black.
// The inward half of each eye plane is cut AFTER the bleed.
(function () {
  var SP = window.MU_SPECIES || 'wolverine';
  var H = MM.hex, WOOL = window.WOOL;
  var P = {
    wolverine:    { root: H('#1d1510'), mid: H('#34261b'), tip: H('#4a3625'), pale: H('#94744c'), paleTip: H('#b39268'),
                    mask: H('#7a7368'), leg: H('#140f0b'), muzzle: H('#2a1f17') },   // (mask #8c8579 read as a white cap, muzzle #17110d as a hole)
    honey_badger: { root: H('#0e0e0f'), mid: H('#1b1b1d'), tip: H('#262629'), pale: H('#a9a7a0'), paleTip: H('#d4d2ca'),
                    mask: H('#bdbbb3'), leg: H('#0c0c0d'), muzzle: H('#101011') }
  }[SP];
  var CLAW = H('#c4b8a0'), NOSE = H('#0b0908'), EYE = H('#0a0806'), EAR_IN = H('#2a1e16');
  var CUT = {};
  var TW = 128, TH = 64;

  var broken = function (o, edge, soft) {
    // a hash-dithered boundary: positive inside, a soft ragged band of width `soft`
    return edge + soft * (MM.h(Math.floor(o.u / 2), Math.floor(o.v / 2), 0, 51) - 0.5);
  };
  var coat = function (o, root, mid, tip) {
    if (o.wn[1] > 0.7) return WOOL.hang(o, root, mid, tip, 2, 4, 0.06);
    if (o.wn[1] < -0.7) return WOOL.under(o, root);
    return WOOL.hang(o, root, mid, tip, 2, 3, 0.08);
  };
  var inPale = function (o) {
    var p = o.wp, n = o.part;
    if (SP === 'wolverine') {
      if (n.indexOf('leg_') === 0 || n.indexOf('paw_') === 0 || n === 'neck' || n.indexOf('head') === 0 || n === 'snout' || n === 'jaw') return false;
      if (n.indexOf('tail') === 0) return n === 'tail_1' && o.wn[1] > 0.3 && p.z < 7.6;
      // the stripe: a band along each flank rising toward the hips, closing over the rump
      var c = 6.3 + 0.12 * (p.z + 4.0), half = 0.95;
      if (p.z < -4.6) return false;
      if (o.wn[1] > 0.6) return p.z > 3.6 && Math.abs(p.x) < 2.4 + 0.8 * (p.z - 3.6);   // over the rump
      return Math.abs(p.y - c) < broken(o, half, 0.6);
    }
    // honey badger mantle: crown -> neck -> back -> tail top, down to mid-flank
    if (n.indexOf('leg_') === 0 || n.indexOf('paw_') === 0 || n === 'jaw' || n === 'snout' || n === 'nose') return false;
    if (n === 'head') return p.y > broken(o, 7.2, 0.4) && p.z > -11.4;
    if (n.indexOf('ear_') === 0) return false;
    if (n.indexOf('tail') === 0) return o.wn[1] > -0.3 && p.y > broken(o, 6.9, 0.5);
    return o.wn[1] > -0.5 && p.y > broken(o, 6.1, 0.5);
  };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, m = MM.map[n], col;
    var i0 = o.v * TW + o.u;
    if (n.indexOf('eye_') === 0) {
      var out = o.wn[0] * (p.x < 0 ? -1 : 1) > 0;
      if (!out) { CUT[i0] = true; return [0, 0, 0, 255]; }
      // the 2x2 plane read as a hole: the top row is a fur lid, so the eye is a 2x1 dark slit
      var fy = (p.y - m.lo.y) / Math.max(0.01, m.hi.y - m.lo.y);
      return fy > 0.5 ? MM.cl(MM.mix(P.mid, P.root, 0.4)) : EYE;
    }
    if (n === 'nose') return MM.cl(MM.mul(NOSE, 0.95 + 0.1 * MM.dith(o.u, o.v, 3)));
    if (n.indexOf('paw_') === 0) {
      if (o.wn[1] < -0.6) return MM.cl(P.leg);
      col = MM.mul(P.leg, 0.95 + 0.1 * MM.dith(o.u, o.v, 5));
      if (o.wn[2] < -0.6 && p.y < 0.5 && (o.i % 2) === 0) col = CLAW;                 // claw tips at the front
      return MM.cl(col);
    }
    if (n.indexOf('ear_') === 0) {
      col = coat(o, P.root, P.mid, P.tip);
      if (o.wn[2] < -0.6) col = MM.mix(col, EAR_IN, 0.5);
      if (SP === 'wolverine' && p.y > 9.0) col = MM.mix(col, P.mask, 0.5);          // pale-rimmed ears
      return MM.cl(col);
    }
    if (n.indexOf('leg_') === 0) return MM.cl(coat(o, P.leg, MM.mix(P.leg, P.root, 0.5), P.root));
    if (n === 'snout' || n === 'jaw') {
      col = coat(o, P.muzzle, MM.mix(P.muzzle, P.root, 0.5), P.root);
      if (SP === 'wolverine' && n === 'jaw' && o.wn[1] < -0.5) col = MM.mix(col, P.pale, 0.35);
      return MM.cl(col);
    }
    if (inPale(o)) col = coat(o, MM.mix(P.root, P.pale, 0.55), P.pale, P.paleTip);
    else col = coat(o, P.root, P.mid, P.tip);
    if (n === 'head') {
      if (SP === 'wolverine') {
        // silver forehead band between the eyes and the ears
        var band = (o.wn[1] > 0.6 || Math.abs(o.wn[0]) > 0.6) && p.y > broken(o, 7.5, 0.5) && p.z > -11.6 && p.z < -9.3;
        if (band) col = coat(o, MM.mix(P.root, P.mask, 0.5), P.mask, MM.mix(P.mask, P.paleTip, 0.5));
        if (p.y < 6.0) col = MM.mix(col, P.muzzle, 0.5);                            // dark cheeks / lower face
      } else if (p.y < 7.2) {
        col = coat(o, P.root, P.mid, P.tip);
      }
    }
    if (SP === 'wolverine' && n === 'neck' && o.wn[1] < -0.6 && Math.abs(p.x) < 0.9) col = MM.mix(col, P.pale, 0.5);   // throat blaze
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, TW, TH), cut = 0;
  for (var key in CUT) {
    img.data[key * 4 + 3] = 0;
    cut++;
  }
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/mustelid_' + SP + '.png', cv);
  return SP + ' painted, ' + cut + ' texels cut';
})();
