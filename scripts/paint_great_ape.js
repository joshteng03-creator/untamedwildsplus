// Great ape skins (2.9.0) on the great ape rig (25 boxes, 128x128). window.GA_SPECIES picks the species; needs woollib.js.
// Gigantopithecus is reconstructed after its closest living relatives, the orangutans: a long, shaggy reddish coat
// (woollib's messy locks -- the approved hair; LONGER on the arms, the way an orangutan's hangs), bare dark skin on the
// face, muzzle and the palms / soles, the brow's front bare. The orangutan is brighter orange-red with a grey-brown face.
// The eyes face FORWARD: the plane's inward quad (+z, into the skull) is cut after the bleed, the outward quad gets a
// skin-coloured lid row over a dark iris so it reads as an eye under the brow, not a hole.
(function () {
  var SP = window.GA_SPECIES || 'gigantopithecus';
  var H = MM.hex, WOOL = window.WOOL;
  var P = {
    // (first pass #6a3016 / #9a4e24 read as the same animal as the orangutan: darker, browner chestnut)
    gigantopithecus:   { root: H('#22110a'), mid: H('#52301f'), tip: H('#7a4c30'), skin: H('#2a2420'), skinLt: H('#3e3530') },
    bornean_orangutan: { root: H('#4a1c0a'), mid: H('#94401a'), tip: H('#c4672e'), skin: H('#3a3028'), skinLt: H('#544638') }
  }[SP];
  var IRIS = H('#2a1608');
  var CUT = {};
  var TW = 128, TH = 128;

  var hair = function (o, len) {
    if (o.wn[1] < -0.7) return WOOL.under(o, P.root);
    return WOOL.hang(o, P.root, P.mid, P.tip, len, len + 3, 0.09);
  };
  var bare = function (o, lt) {
    return MM.cl(MM.mul(lt ? P.skinLt : P.skin, 0.94 + 0.12 * MM.dith(o.u, o.v, 5)));
  };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, m = MM.map[n];
    var i0 = o.v * TW + o.u;
    if (n.indexOf('eye_') === 0) {
      if (o.wn[2] > 0) { CUT[i0] = true; return [0, 0, 0, 255]; }
      var fy = (p.y - m.lo.y) / Math.max(0.01, m.hi.y - m.lo.y);
      return fy > 0.6 ? P.skinLt : IRIS;
    }
    if (n === 'nose' || n === 'muzzle') return bare(o, n === 'muzzle' && o.wn[1] > 0.6);
    if (n === 'jaw') return o.wn[2] < -0.6 || o.wn[1] < -0.6 ? bare(o, false) : MM.cl(hair(o, 2));
    if (n === 'brow') return o.wn[2] < -0.6 ? bare(o, true) : MM.cl(hair(o, 2));
    if (n === 'head') {
      // the face: the front of the skull bare below the brow, the rest short hair
      if (o.wn[2] < -0.6) return bare(o, false);
      return MM.cl(hair(o, 2));
    }
    if (n.indexOf('ear_') === 0) return bare(o, true);
    if (n.indexOf('hand_') === 0) return o.wn[1] < -0.6 || o.wn[2] < -0.6 ? bare(o, false) : MM.cl(hair(o, 2));
    if (n.indexOf('foot_') === 0) return o.wn[1] < -0.6 ? bare(o, false) : MM.cl(MM.mix(hair(o, 2), P.skin, 0.35));
    if (n.indexOf('arm_') === 0) return MM.cl(hair(o, n.indexOf('upper') > 0 ? 6 : 5));   // long arm hair
    if (n.indexOf('leg_') === 0) return MM.cl(hair(o, 4));
    return MM.cl(hair(o, 5));
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
  MM.save(window.OUT + 'skins/great_ape_' + SP + '.png', cv);
  return SP + ' painted, ' + cut + ' texels cut';
})();
