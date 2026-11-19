// Panochthus tuberculatus (glyptodont "panochthus", 2.9.0), on ModelGlyptodont (256x256), with hasTailClub 1.
// Derived from paint_glyptotherium.js. Its identity is the TREATMENT again, against the three plate/rosette shells:
// a DARK SLATE-BROWN carapace pebbled with MANY SMALL, slightly paler TUBERCLES ("tuberculatus") instead of rosettes
// or big plates, a darker band along the lower edge, the same pebbling on the head shield and tail armour, grey-brown
// leathery skin. The spiked caudal tube uses the Doedicurus club parts, so the alpha mask comes from doedicurus.png
// (the only shipped skin with the club rects painted). Colours: scratch/new_species_research.json (a reconstruction).
(function () {
  var C = {
    shell : MM.hex('#3e3833'),
    tuber : MM.hex('#5e554c'),
    grout : MM.hex('#2a2521'),
    rim   : MM.hex('#26211d'),
    boss  : MM.hex('#6e6458'),
    skin  : MM.hex('#575049'),
    skinDk: MM.hex('#38322d'),
    nail  : MM.hex('#211c18')
  };
  var REF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/glyptodont/doedicurus.png';
  var ref = MM.load(REF).getContext('2d').getImageData(0, 0, 256, 256).data;
  var SHELL = { body_core: 1, shell_crown: 1, shell_front: 1, shell_rear: 1, shell_flank_left: 1, shell_flank_right: 1, head_cap: 1 };
  var TAIL = { tail_1: 1, tail_2: 1, tail_3: 1 };
  var CLUB = { tail_club: 1, club_spike_left: 1, club_spike_right: 1, club_spike_top: 1, club_spike_rear: 1 };
  var LEGS = { arm_left_1: 1, arm_left_2: 1, arm_right_1: 1, arm_right_2: 1, leg_left_1: 1, leg_left_2: 1, leg_right_1: 1, leg_right_2: 1 };
  var STAMP = ['eye_left', 'eye_right'];

  // osteoderm mosaic: a 4-texel cell grid in the face's own texel space, rows staggered by half a cell; every cell is a
  // small polygonal plate with ONE centred tubercle and a soft groove on its far edges. Pass 1 (2-texel cells, random
  // half raised, a hard dark grout texel) read as speckle -- the user's "noisy" failure -- so contrast stays low and
  // the variety comes from per-plate tone, not per-texel noise.
  var pebble = function (o) {
    var cj = Math.floor(o.j / 4);
    var ii = o.i + (cj % 2) * 2;
    var ci = Math.floor(ii / 4);
    var li = ii % 4, lj = o.j % 4;
    var tone = 0.92 + 0.16 * MM.h(ci, cj, o.part.length, 31);
    var col;
    if (li === 3 || lj === 3) col = MM.mix(C.shell, C.grout, 0.4);
    else if (li === 1 && lj === 1) col = MM.mix(C.shell, C.tuber, MM.h(ci, cj, o.part.length, 37) > 0.75 ? 1.0 : 0.75);
    else if (li === 1 || lj === 1) col = MM.mix(C.shell, C.tuber, 0.3);
    else col = C.shell;
    return MM.mul(col, tone);
  };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, col;
    if (STAMP.indexOf(n) >= 0) return [0, 0, 0, 0];
    if (SHELL[n]) {
      col = o.face === 'down' ? C.rim : pebble(o);
      // the darker band along the carapace's lower edge, its upper limit jittered per column so it is not a ruled line
      if (o.face !== 'up' && o.face !== 'down') {
        var edge = 15.0 + 1.6 * (MM.h(Math.floor(o.u / 2), 3, 0, 39) - 0.5);
        col = MM.mix(col, C.rim, 0.55 * (1 - MM.smooth(edge - 2.5, edge, p.y)));
      }
    } else if (TAIL[n]) {
      // movable armour rings: pebbled bands with a dark seam between them
      var inBand = (p.z / 2.2) - Math.floor(p.z / 2.2);
      col = inBand < 0.2 ? C.grout : pebble(o);
    } else if (CLUB[n]) {
      // the rigid caudal tube and its spiked bosses: pebbled tube, pale worn bosses
      col = n === 'tail_club' ? pebble(o) : MM.mix(C.boss, C.tuber, 0.3 * MM.h(o.i, o.j, 5, 35));
    } else {
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
  MM.save(window.OUT + 'skins/panochthus.png', cv);
  return 'panochthus painted: masked ' + cut + ', stamped ' + st;
})();
