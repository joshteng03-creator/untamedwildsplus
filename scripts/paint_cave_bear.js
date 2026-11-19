// Ursus spelaeus -- the cave bear, on ModelBear (128x64). Replaces the flat upstream skin (2026-10-09,
// Phase 3.5), which sat 7.2 from brown_2 in the census (both flat dark chocolate). Built from
// paint_short_faced.js (same conventions: cave.png's alpha as the mask, head_eyes/head_teeth stamped,
// claws where cave.png's feet are pale). Coat aimed at the free slot of the bear census (cool, low
// saturation, mid-dark):
//   a COOL DARK UMBER shag, darker legs, GREY-FROSTED guard-hair tips over the back and shoulders (a
//   cold-steppe bear), head the body colour with NO face mask, fawn muzzle, dark nose, no chest crescent
//   (that marks the tremarctines -- short_faced/spectacled -- not Ursus).
(function () {
  var C = {
    back   : MM.hex('#3c3229'),
    flank  : MM.hex('#4a3e33'),
    belly  : MM.hex('#382e26'),
    mask   : MM.hex('#40352b'),
    muzzle : MM.hex('#8a7660'),
    nose   : MM.hex('#18130f'),
    frost  : MM.hex('#a39886'),
    leg    : MM.hex('#221b16'),
    clump  : MM.hex('#5c4e40'),
    claw   : MM.hex('#d8ccb0')
  };

  var REF = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/bear/cave.png';
  var refImg = MM.load(REF).getContext('2d').getImageData(0, 0, 128, 64).data;
  var refLum = function (u, v) {
    var i = (v * 128 + u) * 4;
    return (refImg[i] * 0.299 + refImg[i + 1] * 0.587 + refImg[i + 2] * 0.114) / 255;
  };
  var HEAD = { head_face: 1, head_snout: 1, head_jaw: 1 };
  var FOOT = { arm_left_foot: 1, arm_right_foot: 1, leg_left_foot: 1, leg_right_foot: 1 };
  var LIMB = { arm_left_1: 1, arm_left_2: 1, arm_right_1: 1, arm_right_2: 1,
               leg_left_1: 1, leg_left_2: 1, leg_right_1: 1, leg_right_2: 1 };
  var snoutM = MM.map['head_snout'];
  var body_frost_ok = function (n) { return n === 'body_main' || n === 'body_torso' || n === 'body_hump' || n === 'head_neck' || n.indexOf('_1') > 0; };

  var cv = MM.paint(function (o) {
    var n = o.part, p = o.wp, col;
    if (n === 'head_eyes' || n === 'head_teeth') return [0, 0, 0, 0];   // stamped from cave.png below
    // Ears (locals in ModelBear -- missed by the rig until java2bb was fixed): rufous with a
    // darker tip, so they read against the dark face mask instead of vanishing into it.
    if (n === 'ear_left' || n === 'ear_right') {
      var em = MM.map[n];
      col = MM.mix(C.flank, C.mask, 0.25 + 0.45 * MM.smooth(em.lo.y, em.hi.y, p.y));
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 5)));
    }
    // cave.png's claws are three mid-tone stripes (luminance 0.35-0.5) on each foot front
    if (FOOT[n] && refLum(o.u, o.v) > 0.35) return MM.cl(C.claw);

    if (HEAD[n]) {
      col = C.mask;
      if (n === 'head_snout') {
        // pale muzzle tip toward the nose, dark bridge behind it
        col = MM.mix(C.mask, C.muzzle, 0.80 * MM.smooth(snoutM.hi.z - 0.5, snoutM.lo.z + 2.0, p.z));
        col = MM.mul(col, 0.92 + 0.16 * MM.fbm(p, 1.2, 2, 44));
        // dark nose pad on the front face, upper centre
        if (o.face === 'front' && Math.abs(p.x) < 1.3 && p.y > (snoutM.lo.y + snoutM.hi.y) / 2 - 0.3) col = C.nose;
      }
      if (n === 'head_jaw') col = MM.mix(C.mask, C.muzzle, 0.35);
    } else if (LIMB[n] || FOOT[n]) {
      col = MM.mix(C.flank, C.leg, 0.35 + 0.55 * (1 - MM.smooth(1.0, 11.0, p.y)));
    } else {
      var t = MM.smooth(8.0, 22.0, p.y);
      col = MM.mix(C.belly, C.flank, MM.smooth(0.0, 0.55, t));
      col = MM.mix(col, C.back, MM.smooth(0.55, 1.0, t));
      // grey-frosted guard-hair tips over the back and shoulders: the brightest strand tone only
      var fr = MM.smooth(0.55, 0.9, t) * (body_frost_ok(n) ? 1 : 0);
      var sh2 = MM.h(o.u, Math.floor((o.v + 5 * MM.h(o.u, 1, 0, 3)) / 3), 0, 19);
      if (sh2 > 0.66 && MM.h(o.u, o.v, 0, 61) > 0.55) col = MM.mix(col, C.frost, 0.5 * fr);   // short flecks, not streaks
    }
    // shaggy pixel fur, the shipped bear style: clumps + short vertical strands in four tones
    if (!HEAD[n]) {
      col = MM.mix(col, C.clump, 0.35 * MM.smooth(0.45, 0.72, MM.fbm(p, 0.8, 3, 27)));
    }
    col = MM.mul(col, 1 + 0.18 * (MM.fbm(p, 0.9, 3, 1) - 0.5));
    var sh = MM.h(o.u, Math.floor((o.v + 5 * MM.h(o.u, 1, 0, 3)) / 3), 0, 19);
    col = MM.mul(col, 1 + 0.26 * (Math.floor(sh * 4) / 3 - 0.5));
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['head_eyes', 'head_teeth']));

  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, 128, 64), cut = 0, eye = 0;
  for (var i = 0; i < img.data.length; i += 4) {
    if (refImg[i + 3] === 0 && img.data[i + 3] !== 0) { img.data[i + 3] = 0; cut++; }
  }
  ['head_eyes', 'head_teeth'].forEach(function (name) {
    var F = MM.map[name].faces;
    for (var k in F) {
      var f = F[k];
      for (var y = f.v0; y < f.v1; y++) for (var x = f.u0; x < f.u1; x++) {
        var j = (y * 128 + x) * 4;
        img.data[j] = refImg[j]; img.data[j + 1] = refImg[j + 1]; img.data[j + 2] = refImg[j + 2]; img.data[j + 3] = refImg[j + 3];
        eye++;
      }
    }
  });
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/cave.png', cv);
  return 'cave bear painted: masked ' + cut + ', eye texels ' + eye;
})();
