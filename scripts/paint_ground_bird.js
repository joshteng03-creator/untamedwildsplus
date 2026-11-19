// Giant ground bird skins (2.9.0) on the ground bird rig (19 boxes, 128x128). window.GB_SPECIES picks the species;
// needs woollib.js. Ratite plumage is hair-like and shaggy, so the body, tail and thighs wear woollib's messy locks
// (the approved coat: low contrast, no repeating rows); the neck wears SHORTER locks, the head shorter still. The
// drumstick is feathered down from the hip and bare toward the ankle; the tarsus and foot are SCALY (a soft scute
// band every texel row, not a checker), the toes clawed dark at the front.
//   giant moa      reddish-brown plumage with buff tips; pale horn beak;
//   elephant bird  dark umber-black plumage, a greyer neck; grey-horn beak;
//   Genyornis      emu-like grey-brown, darker body, pale grey head and neck; the big deep beak dark slate.
// Colours: reconstructions (scratch/new_species_research.json style). The inward half of each eye plane is cut after
// the bleed; its top row is a lid so the eye is a slit, not a hole.
(function () {
  var SP = window.GB_SPECIES || 'giant_moa';
  var H = MM.hex, WOOL = window.WOOL;
  var P = {
    giant_moa:     { root: H('#3a2316'), mid: H('#6e4128'), tip: H('#9c6a44'), neck: H('#7a5034'), head: H('#5e3c26'),
                     beak: H('#c8b88c'), beakTip: H('#a49268'), leg: H('#5a5248'), scute: H('#463f37') },
    elephant_bird: { root: H('#1c1612'), mid: H('#352a22'), tip: H('#55463a'), neck: H('#5e5852'), head: H('#4a4540'),
                     beak: H('#9a8f7c'), beakTip: H('#7c7262'), leg: H('#55504a'), scute: H('#403c37') },
    genyornis:     { root: H('#2e2a26'), mid: H('#544c44'), tip: H('#7c7266'), neck: H('#8a8680'), head: H('#9a968e'),
                     beak: H('#3a3835'), beakTip: H('#5a5650'), leg: H('#5c5852'), scute: H('#45423d') }
  }[SP];
  var CLAW = H('#1e1b18'), EYE = H('#120c08'), EYE_RING = H('#3a2a1c');
  var CUT = {};
  var TW = 128, TH = 128;

  var coat = function (o, root, mid, tip, len) {
    if (o.wn[1] < -0.7) return WOOL.under(o, root);
    return WOOL.hang(o, root, mid, tip, len, len + 2, 0.08);
  };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, m = MM.map[n], col;
    var i0 = o.v * TW + o.u;
    if (n.indexOf('eye_') === 0) {
      var out = o.wn[0] * (p.x < 0 ? -1 : 1) > 0;
      if (!out) { CUT[i0] = true; return [0, 0, 0, 255]; }
      var fy = (p.y - m.lo.y) / Math.max(0.01, m.hi.y - m.lo.y);
      return fy > 0.55 ? MM.cl(MM.mix(P.head, EYE_RING, 0.5)) : EYE;
    }
    if (n === 'beak') {
      var fz = (p.z - m.lo.z) / Math.max(0.01, m.hi.z - m.lo.z);      // 0 at the tip (-z)
      col = MM.mix(P.beakTip, P.beak, Math.min(1, fz * 1.6));
      return MM.cl(MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 3)));
    }
    if (n.indexOf('foot_') === 0) {
      if (o.wn[1] < -0.6) return MM.cl(MM.mul(P.scute, 0.8));
      col = (o.j % 2 === 0) ? P.leg : MM.mix(P.leg, P.scute, 0.6);
      if (o.wn[2] < -0.6 && (o.i % 2) === 0) col = CLAW;                 // three clawed toes at the front
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 5)));
    }
    if (n.indexOf('leg_tarsus') === 0) {
      col = (Math.floor(p.y) % 2 === 0) ? P.leg : MM.mix(P.leg, P.scute, 0.55);   // soft scute bands
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 7)));
    }
    if (n.indexOf('leg_shin') === 0) {
      // feathered down from the hip, bare and scaly toward the ankle
      var fh = (p.y - m.lo.y) / Math.max(0.01, m.hi.y - m.lo.y);
      col = coat(o, P.root, P.mid, P.tip, 3);
      var bare = MM.smooth(0.15, 0.4, 1 - fh + 0.15 * (MM.h(o.u, 2, 0, 41) - 0.5));
      return MM.cl(MM.mix(col, P.leg, bare));
    }
    if (n === 'head') return MM.cl(coat(o, MM.mix(P.head, P.root, 0.4), P.head, MM.mix(P.head, P.tip, 0.3), 1));
    if (n.indexOf('neck') === 0) {
      var t = n === 'neck_1' ? 0.0 : (n === 'neck_2' ? 0.5 : 1.0);   // the plumage colour gives way to the neck colour
      var mid = MM.mix(P.mid, P.neck, 0.3 + 0.7 * t), tip = MM.mix(P.tip, P.neck, 0.3 + 0.6 * t);
      return MM.cl(coat(o, MM.mix(P.root, P.neck, 0.4 * t), mid, tip, n === 'neck_1' ? 4 : 2));
    }
    // body, rump, breast, tail plume, thighs: the long shaggy plumage
    col = coat(o, P.root, P.mid, P.tip, n === 'tail' ? 5 : 4);
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
  MM.save(window.OUT + 'skins/ground_bird_' + SP + '.png', cv);
  return SP + ' painted, ' + cut + ' texels cut';
})();
