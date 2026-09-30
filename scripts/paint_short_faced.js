// Arctodus simus -- the giant short-faced bear, on ModelBear (128x64, 19 parts, hasHump).
// Shipped as a ~0.99-similar recolour of cave.png. Redone as a different animal, designed
// against the two bears it could be mistaken for:
//   cave          uniform dark chocolate shag, lighter muzzle
//   arctotherium  pale tawny back fading to dark legs, BUFF face (its tremarctine cousin)
//   short_faced   rich rufous-cinnamon shag all over, a DARK chocolate face mask (the inverse
//                 of arctotherium's), pale muzzle tip, and the cream CHEST CRESCENT that marks
//                 the tremarctines (the spectacled bear's family) -- plus darker lower legs.
//
// Conventions come from cave.png, not invention: its alpha is applied as a mask, head_eyes is
// stamped verbatim, and the claws are wherever cave.png's foot texels are pale.
(function () {
  var C = {
    back   : MM.hex('#5a2c17'),
    flank  : MM.hex('#7e4024'),
    belly  : MM.hex('#6a3a22'),
    mask   : MM.hex('#2e1a12'),   // dark chocolate face mask
    muzzle : MM.hex('#9a7b5c'),   // tan muzzle (a flat pale one read as a beak)
    nose   : MM.hex('#1c1411'),
    blaze  : MM.hex('#e2d3b5'),   // tremarctine chest crescent
    leg    : MM.hex('#3a2016'),
    clump  : MM.hex('#a0582f'),   // lighter cinnamon clumps
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
      // the chest crescent: front of the torso, low, broken edges
      if (n === 'body_torso') {
        // keyed on the torso's own FRONT face, not world z: the tilt and hump scale moved it
        var bl = (o.face === 'front' ? 1 : 0) * MM.smooth(7.0, 8.2, p.y) * (1 - MM.smooth(11.5, 13.2, p.y))
                 * (1 - MM.smooth(2.6, 4.4, Math.abs(p.x)));
        bl *= 0.8 + 0.4 * MM.dith(o.u, o.v, 21);
        col = MM.mix(col, C.blaze, Math.max(0, Math.min(1, bl)));
      }
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
  MM.save(window.OUT + 'skins/short_faced.png', cv);
  return 'short_faced painted: masked ' + cut + ', eye texels ' + eye;
})();
