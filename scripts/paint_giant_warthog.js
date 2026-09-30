// Metridiochoerus -- the giant warthog of Pleistocene Africa, on ModelWarthog (64x64, 22 parts).
// Shipped as a straight recolour of warthog.png (identical alpha, colours shifted). Redone as a
// different animal, designed against the common warthog on every visible axis:
//   warthog        warm olive-brown mottled coat | DARK crest     | brown cheek pads | plain legs
//   giant_warthog  cool SLATE-GREY sparse hide   | PALE tawny mane| cream WHISKERS   | laterite mud
// Cool grey is the one empty slot in the boar set (every shipped suid sits at hue 25-37).
//
// ModelWarthog's conventions are taken from warthog.png rather than reasoned about: its alpha
// (partial tail rect, tusk and eye shapes, crest cut-outs) is applied as a mask after painting,
// and the eye texels are stamped verbatim. cheek_* shares UV [0,0] with main_body's corner; that
// overlap is the rig's, and the shipped skins live with it too.
//
// Style matches the shipped suids: blocky 2x2-texel mottling in four tones plus broad
// world-space patches, not a smooth ramp.
(function () {
  var C = {
    back   : MM.hex('#3b3e41'),
    flank  : MM.hex('#575a5c'),
    belly  : MM.hex('#6e6b66'),
    snout  : MM.hex('#34302e'),
    mouth  : MM.hex('#7a6862'),
    mane   : MM.hex('#c9b48a'),   // long pale tawny crest
    maneDk : MM.hex('#8f7a55'),
    whisker: MM.hex('#ddd3bf'),   // cream cheek whiskers
    tusk   : MM.hex('#e3d9c1'),
    tuskTip: MM.hex('#b7aa8c'),
    mud    : MM.hex('#8a5a3a'),   // dry laterite
    hoof   : MM.hex('#26221f'),
    tail   : MM.hex('#2b2522')
  };
  var REF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/boar/warthog.png';
  var HEAD = { head_main: 1, head_snout: 1, head_mouth: 1, ear_left: 1, ear_right: 1 };
  var LIMB = { arm_left_1: 1, arm_left_2: 1, arm_right_1: 1, arm_right_2: 1,
               leg_left_1: 1, leg_left_2: 1, leg_right_1: 1, leg_right_2: 1 };

  var block = function (o, s) {   // four-tone 2x2 texel block, the shipped suid pixel style
    return Math.floor(MM.h(Math.floor(o.u / 2), Math.floor(o.v / 2), 0, s) * 4) / 3 - 0.5;
  };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, col;
    if (n === 'tusk_left' || n === 'tusk_right') {
      var tm = MM.map[n];
      return MM.cl(MM.mix(C.tusk, C.tuskTip, MM.smooth(tm.lo.y + 0.5, tm.hi.y, p.y) * 0.8));
    }
    if (n === 'shape15') {   // the crest
      col = MM.mix(C.mane, C.maneDk, 0.75 * (block(o, 5) + 0.5));
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 3)));
    }
    if (n === 'shape14') return MM.cl(C.tail);
    if (n === 'cheek_left' || n === 'cheek_right') {
      return MM.cl(MM.mul(C.whisker, 0.92 + 0.16 * (block(o, 8) + 0.5)));
    }
    if (LIMB[n]) {
      var m = MM.map[n], seg = (p.y - m.lo.y) / Math.max(0.001, m.hi.y - m.lo.y);
      col = MM.mix(MM.mul(C.flank, 0.9), C.flank, seg);
      // laterite mud up the legs from a wobbling tide line, then dark hooves
      // mud SOCKS on the lower third -- at 3.5-6.5 it swallowed every visible leg
      var tide = 1 - MM.smooth(1.6 + 1.2 * MM.fbm(p, 0.6, 2, 9), 3.8, p.y);
      col = MM.mix(col, C.mud, 0.75 * tide);
      col = MM.mix(col, C.hoof, 0.85 * (1 - MM.smooth(-0.1, 1.2, p.y)));
    } else if (HEAD[n]) {
      col = MM.mix(C.flank, C.back, 0.35);
      if (n === 'head_snout') col = MM.mix(col, C.snout, 0.7);
      if (n === 'head_mouth') col = MM.mix(col, C.mouth, 0.6);
    } else {
      var t = MM.smooth(0.2, 1.0, (p.y - 1.0) / 14.0);
      col = MM.mix(C.belly, C.flank, MM.smooth(0.0, 0.55, t));
      col = MM.mix(col, C.back, MM.smooth(0.55, 1.0, t));
      // a dusting of the same mud on the flanks, where it wallows
      col = MM.mix(col, C.mud, 0.22 * MM.smooth(0.55, 0.8, MM.fbm(p, 0.5, 3, 4)) * (1 - MM.smooth(6.0, 12.0, p.y)));
    }
    // sparse bristle over bare skin: broad patches plus the blocky pixel mottle
    // Mottle strength matched to the shipped warthog (2026-09-30: at 0.16 it read flat).
    col = MM.mul(col, 1 + 0.26 * (MM.fbm(p, 0.7, 3, 2) - 0.5));
    col = MM.mix(col, MM.mul(col, 0.72), MM.smooth(0.5, 0.62, MM.fbm(p, 0.45, 2, 14)) * 0.8);
    col = MM.mul(col, 1 + 0.34 * block(o, 11));
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

  // Apply warthog.png's alpha as the mask and stamp its eyes verbatim.
  var ref = MM.load(REF).getContext('2d').getImageData(0, 0, cv.width, cv.height).data;
  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, cv.width, cv.height), cut = 0, eye = 0;
  for (var i = 0; i < img.data.length; i += 4) {
    if (ref[i + 3] === 0 && img.data[i + 3] !== 0) { img.data[i + 3] = 0; cut++; }
  }
  ['eye_left', 'eye_right'].forEach(function (name) {
    var F = MM.map[name].faces;
    for (var k in F) {
      var f = F[k];
      for (var y = f.v0; y < f.v1; y++) for (var x = f.u0; x < f.u1; x++) {
        var j = (y * cv.width + x) * 4;
        img.data[j] = ref[j]; img.data[j + 1] = ref[j + 1]; img.data[j + 2] = ref[j + 2]; img.data[j + 3] = ref[j + 3];
        eye++;
      }
    }
  });
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/giant_warthog.png', cv);
  return 'giant_warthog painted: masked ' + cut + ' texels to warthog alpha, eyes ' + eye;
})();
