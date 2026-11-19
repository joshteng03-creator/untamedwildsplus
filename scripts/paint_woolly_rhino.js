// Coelodonta antiquitatis -- the woolly rhinoceros (rhino "wooly"), on ModelRhino (128x64). Replaces the flat
// upstream skin (2026-10-09, Phase 3.5), which read as a short-haired Sumatran rhino (census 12.9).
// Evidence: the permafrost calf "Sasha" has reddish-ginger hair; adults are reconstructed darker brown with a
// long shaggy coat, a heavy shoulder hump and a long flattened front horn of dark keratin.
//   SHAGGY CHESTNUT coat, ginger-frosted tips, DARKER shoulder hump, dark RAGGED FRINGES along the lower
//   flanks and belly (long hair hanging), darker shaggy legs, dark brown head, DARK KERATIN horns paling
//   slightly toward the tips (shading taken from the shipped horns' own brightness).
// Conventions from wooly.png: its alpha is the mask, the eyes are stamped verbatim.
(function () {
  var C = {
    back   : MM.hex('#4e301b'),
    coat   : MM.hex('#6e4527'),
    ginger : MM.hex('#ad7440'),
    fringe : MM.hex('#2e1c12'),
    leg    : MM.hex('#2a1c14'),
    head   : MM.hex('#43301f'),
    muzzle : MM.hex('#5a4430'),
    hornDk : MM.hex('#2a221c'),
    hornLt : MM.hex('#6e6250')
  };
  var REF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/rhino/wooly.png';
  var ref = MM.load(REF).getContext('2d').getImageData(0, 0, 128, 64).data;
  var lum = function (u, v) { var i = (v * 128 + u) * 4; return (ref[i] * 0.299 + ref[i + 1] * 0.587 + ref[i + 2] * 0.114) / 255; };
  var STAMP = ['eye_left', 'eye_right'];
  var BODY = { body_front: 1, body_belly: 1, head_neck: 1 };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, col, m = MM.map[n];
    if (STAMP.indexOf(n) >= 0) return [0, 0, 0, 0];
    if (n.indexOf('horn') === 0) {
      col = MM.mix(C.hornDk, C.hornLt, MM.smooth(0.25, 0.85, lum(o.u, o.v)));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 7)));
    }
    if (n === 'head_face') {
      // the original mouth (user: "give it a mouth like the original model"): wooly.png draws the lips and
      // nostrils as low-saturation dark greys on the face, the fur around them is saturated brown -- keep
      // every such texel verbatim
      var ri = (o.v * 128 + o.u) * 4, mx = Math.max(ref[ri], ref[ri + 1], ref[ri + 2]), mn = Math.min(ref[ri], ref[ri + 1], ref[ri + 2]);
      if (ref[ri + 3] > 0 && mx - mn < 28 && mx < 150) return [ref[ri], ref[ri + 1], ref[ri + 2]];
    }
    if (n === 'head_face' || n === 'ear_left' || n === 'ear_right') {
      col = MM.mix(C.head, C.muzzle, 0.5 * MM.smooth(m.hi.z - 4.0, m.hi.z, p.z) * (n === 'head_face' ? 1 : 0));
      col = MM.mul(col, 0.9 + 0.2 * MM.fbm(p, 1.1, 2, 17));
      var hs = MM.h(o.u, Math.floor((o.v + 5 * MM.h(o.u, 1, 0, 3)) / 3), 0, 19);
      return MM.cl(MM.mul(col, 1 + 0.2 * (Math.floor(hs * 4) / 3 - 0.5)));
    } else {
      // MAMMOTH FUR LANGUAGE (scripts/mammoth_fur.js, user-approved on all 8 mammoths), adapted: the rhino has
      // no separate fur boxes, so the ragged hem cannot be alpha -- each lock's end is painted as shadow.
      var hang = o.face !== 'up' && o.face !== 'down';
      var leg = !BODY[n];
      var mean = leg ? MM.mix(C.coat, C.leg, 0.45) : MM.mix(C.coat, C.back, n === 'body_front' ? MM.smooth(m.lo.y, m.hi.y, p.y) * 0.8 : 0.15);
      var root = MM.mul(mean, 0.72), tip = MM.mix(mean, [214, 170, 110], 0.28);
      var up = p.y - m.lo.y, hgt = m.hi.y - m.lo.y;
      if (o.face === 'down') {
        return MM.cl(MM.mul(root, 0.62 + 0.30 * (Math.floor(MM.h(o.u, Math.floor(o.v / 3), 0, 19) * 4) / 3)));
      }
      // locks hang from the top of each face and lighten toward their tips
      var along = hang ? 1 - MM.smooth(0, hgt, up) : 0.5;
      col = MM.mix(root, tip, 0.25 + 0.6 * along);
      if (hang && !leg) {
        var cut = Math.floor(MM.h(o.u, 3, 0, 61) * 5);          // 0..4 texels of hem per column
        if (up < cut) col = MM.mul(C.fringe, 0.9 + 0.2 * MM.h(o.u, o.v, 0, 3));   // lock ended: shadow
      }
      var run = Math.floor((o.v + 7 * MM.h(o.u, 2, 0, 5)) / 4);
      var tone = Math.floor(MM.h(o.u, run, 0, 19) * 4) / 3 - 0.5;
      col = MM.mul(col, 1 + 0.36 * tone);
      if (MM.h(o.u, 9, 0, 71) < 0.22) col = MM.mul(col, 0.78);   // parting line between locks
      col = MM.mul(col, 1 + 0.10 * (MM.fbm(p, 0.6, 2, 3) - 0.5));
      return MM.cl(col);
    }
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(STAMP));

  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, 128, 64), cut = 0, st = 0;
  for (var i = 0; i < img.data.length; i += 4) {
    if (ref[i + 3] === 0 && img.data[i + 3] !== 0) { img.data[i + 3] = 0; cut++; }
  }
  STAMP.forEach(function (name) {
    var F = MM.map[name].faces;
    for (var k in F) {
      var f = F[k];
      for (var y = f.v0; y < f.v1; y++) for (var x = f.u0; x < f.u1; x++) {
        var j = (y * 128 + x) * 4;
        img.data[j] = ref[j]; img.data[j + 1] = ref[j + 1]; img.data[j + 2] = ref[j + 2]; img.data[j + 3] = ref[j + 3];
        st++;
      }
    }
  });
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/wooly.png', cv);
  return 'woolly rhino painted: masked ' + cut + ', stamped ' + st;
})();
