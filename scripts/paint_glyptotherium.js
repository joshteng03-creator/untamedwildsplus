// Glyptotherium -- North America's glyptodont (quick-win batch, 2026-10-02), on ModelGlyptodont
// (256x256). The three shipped glyptodonts sit in one tight grey-brown band (hue 33-37, light
// 0.33-0.41, sat 0.12-0.22) and share the glyptodon's big rectangular plates. This one changes the
// TREATMENT, not just the palette: Glyptotherium's carapace osteoderms are ROSETTES -- a raised
// central disc ringed by a darker collar -- painted per plate on a ~6-texel grid in each face's own
// texel space, on a warm ochre-tan shell. Tail in banded rings; head, neck and legs dark
// reddish-brown pebbled skin. Eyes and the alpha mask come from glyptodon.png verbatim.
(function () {
  var C = {
    shell : MM.hex('#9c7850'),   // ochre-tan plate field
    disc  : MM.hex('#d4b286'),   // raised rosette centre
    collar: MM.hex('#7b5a3a'),   // ring around the centre
    grout : MM.hex('#4c392a'),   // seams where rosettes meet
    rim   : MM.hex('#644a34'),   // shell underside / lower edge
    skin  : MM.hex('#5e4334'),
    skinDk: MM.hex('#3d2b20'),
    nail  : MM.hex('#2a2018')
  };
  var REF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/glyptodont/glyptodon.png';
  var ref = MM.load(REF).getContext('2d').getImageData(0, 0, 256, 256).data;
  var SHELL = { body_core: 1, shell_crown: 1, shell_front: 1, shell_rear: 1, shell_flank_left: 1, shell_flank_right: 1, head_cap: 1 };
  var TAIL = { tail_1: 1, tail_2: 1, tail_3: 1 };
  var LEGS = { arm_left_1: 1, arm_left_2: 1, arm_right_1: 1, arm_right_2: 1, leg_left_1: 1, leg_left_2: 1, leg_right_1: 1, leg_right_2: 1 };
  var STAMP = ['eye_left', 'eye_right'];
  var CELL = 4;

  // one rosette per CELL x CELL block of the face's own texel grid, centred so partial cells
  // fall evenly on both edges
  var rosette = function (o) {
    // rows staggered by half a cell so the plates interlock like a honeycomb instead of
    // sitting on a printed grid
    var oy = (o.h % CELL) / 2;
    var cj = Math.floor((o.j + 0.5 - oy) / CELL);
    var ox = (o.w % CELL) / 2 + (cj % 2 ? CELL / 2 : 0);
    var ci = Math.floor((o.i + 0.5 - ox) / CELL);
    var cx = ox + (ci + 0.5) * CELL, cy = oy + (cj + 0.5) * CELL;
    var dx = o.i + 0.5 - cx, dy = o.j + 0.5 - cy;
    var r = Math.sqrt(dx * dx + dy * dy);
    var col = C.shell;
    // even cell -> the centre sits on a texel corner: a solid 2x2 stud (r 0.71), its collar the
    // 8 edge-adjacent texels (r 1.58), shell in the 4 corners (r 2.12)
    if (r < 1.0) col = C.disc;
    else if (r < 1.9) col = C.collar;
    else col = MM.mix(C.shell, C.grout, 0.35);
    // per-plate tone so the shell is not a printed pattern
    return MM.mul(col, 0.92 + 0.16 * MM.h(ci, cj, o.part.length, 7));
  };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, col;
    if (STAMP.indexOf(n) >= 0) return [0, 0, 0, 0];
    if (SHELL[n]) {
      col = o.face === 'down' ? C.rim : rosette(o);
      // the lowest band of the skirt darkens toward the rim
      if (o.face !== 'up' && o.face !== 'down') col = MM.mix(col, C.rim, 0.35 * (1 - MM.smooth(9.0, 13.0, p.y)));
    } else if (TAIL[n]) {
      // banded rings of osteoderms down the tail, with a small knob row in each band
      var band = Math.floor(p.z / 2.2);
      var inBand = (p.z / 2.2) - band;
      col = inBand < 0.22 ? C.grout : (band % 2 ? C.shell : MM.mix(C.shell, C.collar, 0.5));
      if (inBand > 0.4 && inBand < 0.75 && MM.h(Math.floor(o.i / 2), band, 3, 9) > 0.55) col = C.disc;
    } else {
      // pebbled skin: a cell texture of small scales, darker toward the feet
      var cell = MM.h(Math.floor(o.i / 2), Math.floor(o.j / 2), n.length, 13);
      col = MM.mix(C.skin, C.skinDk, 0.35 + 0.35 * cell);
      if (LEGS[n]) col = MM.mix(col, C.skinDk, 0.5 * (1 - MM.smooth(1.0, 8.0, p.y)));
      if (LEGS[n] && p.y < 1.4) col = C.nail;
      if (n === 'snout' && o.face === 'front') col = MM.mix(col, C.skinDk, 0.5);
    }
    col = MM.mul(col, 0.96 + 0.08 * MM.dith(o.u, o.v, 7));
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(STAMP));

  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, 256, 256), cut = 0, st = 0;
  for (var i = 0; i < img.data.length; i += 4) {
    if (ref[i + 3] === 0 && img.data[i + 3] !== 0) { img.data[i + 3] = 0; cut++; }
  }
  STAMP.forEach(function (name) {
    var F = MM.map[name].faces;
    for (var k in F) {
      var f = F[k];
      for (var y = f.v0; y < f.v1; y++) for (var x = f.u0; x < f.u1; x++) {
        var j = (y * 256 + x) * 4;
        img.data[j] = ref[j]; img.data[j + 1] = ref[j + 1]; img.data[j + 2] = ref[j + 2]; img.data[j + 3] = ref[j + 3];
        st++;
      }
    }
  });
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/glyptotherium.png', cv);
  return 'glyptotherium painted: masked ' + cut + ', stamped ' + st;
})();
