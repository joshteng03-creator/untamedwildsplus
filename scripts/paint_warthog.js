// Phacochoerus africanus -- the common warthog, on ModelWarthog (64x64, 22 parts). Older-skin pass 2026-11-15.
// The shipped warthog.png was a brown, boar-blotched coat. A real warthog is GREY, nearly bare skin with a
// long dark mane down the spine, white whisker bristles along the jaw, facial warts and a thin tail with a
// dark tuft. The giant warthog (paint_giant_warthog.js) already took grey, so the two are kept apart on
// every axis the real animals give (and lighter than the babirusa, the other grey suid: at a mid grey
// the census put it 3.6 from it and 8.5 from the giant warthog):
//   warthog        LIGHT WARM dust-grey    | BLACKISH-BROWN mane | white jaw whiskers | faint buff dust on the legs
//   giant_warthog  DARK COOL slate grey     | PALE TAWNY mane     | cream whiskers     | laterite mud socks
// Conventions as the giant warthog: warthog.png's alpha is the mask (tail rect, tusk and eye shapes, crest
// cut-outs), the eyes are stamped verbatim from it, and the style is the shipped suids' blocky 2x2-texel
// four-tone mottle plus broad world-space patches. The ORIGINAL warthog.png is read from scratch/orig_warthog.png
// (saved before this repaint) so a re-run never masks against its own output.
(function () {
  var C = {
    back   : MM.hex('#7a6f62'),
    flank  : MM.hex('#9a8f80'),
    belly  : MM.hex('#a69c8d'),
    snout  : MM.hex('#5d554e'),
    mouth  : MM.hex('#806c64'),
    wart   : MM.hex('#62574d'),
    mane   : MM.hex('#2a221d'),   // long blackish-brown crest
    maneLt : MM.hex('#4e4136'),
    whisker: MM.hex('#d4cdc1'),   // off-white bristles along the jaw (pure white merged with the tusks)
    tusk   : MM.hex('#ebe2cc'),
    tuskTip: MM.hex('#c2b597'),
    dust   : MM.hex('#a08468'),   // pale dry dust, not mud
    hoof   : MM.hex('#26221f'),
    tail   : MM.hex('#4a433d'),
    tuft   : MM.hex('#1f1a17')
  };
  var REF = window.OUT + 'orig_warthog.png';
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
    if (n === 'shape15') {   // the crest: long dark strands with a few lighter hairs
      col = MM.mix(C.mane, C.maneLt, 0.6 * (block(o, 5) + 0.5));
      if (MM.h(o.u, Math.floor(o.v / 3), 0, 61) < 0.18) col = MM.mix(col, C.maneLt, 0.6);
      return MM.cl(MM.mul(col, 0.95 + 0.1 * MM.dith(o.u, o.v, 3)));
    }
    if (n === 'shape14') {   // thin tail, dark tuft at the end
      var sm = MM.map[n];
      col = MM.mix(C.tail, C.tuft, MM.smooth(sm.lo.y + (sm.hi.y - sm.lo.y) * 0.45, sm.lo.y + (sm.hi.y - sm.lo.y) * 0.2, p.y));
      return MM.cl(col);
    }
    if (n === 'cheek_left' || n === 'cheek_right') {
      return MM.cl(MM.mul(C.whisker, 0.86 + 0.22 * (block(o, 8) + 0.5)));
    }
    if (LIMB[n]) {
      var m = MM.map[n], seg = (p.y - m.lo.y) / Math.max(0.001, m.hi.y - m.lo.y);
      col = MM.mix(MM.mul(C.flank, 0.9), C.flank, seg);
      // a faint buff dust on the lower legs (dry wallow), then dark hooves
      var tide = 1 - MM.smooth(1.4 + 1.0 * MM.fbm(p, 0.6, 2, 9), 3.4, p.y);
      col = MM.mix(col, C.dust, 0.4 * tide);
      col = MM.mix(col, C.hoof, 0.85 * (1 - MM.smooth(-0.1, 1.2, p.y)));
    } else if (HEAD[n]) {
      col = MM.mix(C.flank, C.back, 0.3);
      if (n === 'head_snout') col = MM.mix(col, C.snout, 0.6);
      if (n === 'head_mouth') col = MM.mix(col, C.mouth, 0.55);
      // the warts: a few darker 1-2 texel lumps on the face, each with a lit texel above it
      if ((n === 'head_main' || n === 'head_snout') && o.face !== 'up' && o.face !== 'down') {
        var w = MM.h(Math.floor(o.u / 2), Math.floor(o.v / 2), 0, 71);
        if (w < 0.16) col = MM.mix(col, C.wart, 0.75);
        else if (w > 0.93) col = MM.mul(col, 1.12);
      }
    } else {
      var t = MM.smooth(0.2, 1.0, (p.y - 1.0) / 14.0);
      col = MM.mix(C.belly, C.flank, MM.smooth(0.0, 0.55, t));
      col = MM.mix(col, C.back, MM.smooth(0.6, 1.0, t) * 0.8);
      col = MM.mix(col, C.dust, 0.16 * MM.smooth(0.55, 0.8, MM.fbm(p, 0.5, 3, 4)) * (1 - MM.smooth(5.0, 10.0, p.y)));
    }
    // sparse bristle over bare skin: broad patches plus the blocky pixel mottle (strength matched to the
    // shipped suids; at 0.16 the giant warthog read flat)
    col = MM.mul(col, 1 + 0.22 * (MM.fbm(p, 0.7, 3, 2) - 0.5));
    col = MM.mix(col, MM.mul(col, 0.8), MM.smooth(0.5, 0.62, MM.fbm(p, 0.45, 2, 14)) * 0.7);
    col = MM.mul(col, 1 + 0.26 * block(o, 11));
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));

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
  MM.save(window.OUT + 'skins/warthog.png', cv);
  return 'warthog painted: masked ' + cut + ' texels to the original warthog alpha, eyes ' + eye;
})();
