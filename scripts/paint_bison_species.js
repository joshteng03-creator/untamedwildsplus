(function () {
  var SPECIES = {
    // Bison latifrons -- the giant long-horned bison of Pleistocene North America (horn span
    // ~2 m; longHorns doubles the horn cubes in-game). Shipped as a hue shift of plains, sitting
    // 4.8 from it. Designed against its two nearest siblings on STRUCTURE:
    //   plains  = dark brown with a WARM ochre hump
    //   steppe  = black front cape over a RED body
    //   latifrons = near-black chocolate body under a COOL frost-bleached buff cape across the
    //   hump top, forelock and upper neck, with the face and beard left dark, and pale ivory
    //   horns darkening to the tips so the enormous span reads at distance.
    long_horned: {
      // COOL grey-umber body, not warm brown: at warm brown the census measured 8.5 from
      // plains -- the body is most of the animal, so the cape and horns cannot move the mean.
      back: '#28231f', flank: '#36302a', belly: '#463e35',
      maneCol: '#a59a84', maneAmt: 0.80,
      maneParts: [], maneTop: true, maneTopLo: 15.5, maneTopHi: 20.0,   // forelock stays dark: a pale one read as a blond fringe
      muzzle: '#1f1914', muzzleAmt: 0.35,
      hoof: '#1a1612',
      horn: '#d6ccb4', hornTip: '#2e2a25', hornRidge: 0.05,
      legCol: '#1b1511', legAmt: 0.70,
      shadeLo: 0.15, shadeHi: 0.95,
      hairScale: 0.9, hairAmt: 0.20, grizzleAmt: 0.10,
      strandAmt: 0.30, clumpCol: '#4d453c', clumpAmt: 0.30, clumpScale: 0.9,
      tufts: true
    },
    // Bison priscus -- the steppe bison of the Ice Age, shipped as a hue shift of plains
    // (its identity was just "plains in another colour"). Painted from the animal the Ice Age
    // artists actually drew: Altamira and Lascaux show it TWO-TONE, a red-brown body under a
    // black head, beard, shoulder mane and legs, and the Blue Babe mummy is a deep reddish
    // brown. So the treatment is the split, not the colour: plains is one dark brown all
    // over; this is rufous-chestnut behind a black cape. Hue pushed to ~14 (redder than
    // banteng's 21) and the black extremities pull its mean well away from banteng too.
    steppe: {
      back: '#4a2214', flank: '#6e2f1a', belly: '#8a4a2c',
      maneCol: '#1c1512', maneAmt: 0.85,
      muzzle: '#231c18', muzzleAmt: 0.40,
      hoof: '#1a1613',
      horn: '#2a2622', hornTip: '#6a6258', hornRidge: 0.05,
      legCol: '#1d1714', legAmt: 0.80,
      shadeLo: 0.15, shadeHi: 0.95,
      // user feedback 2026-09-30: the coat read as a flat red gradient and the fringes as
      // flat rectangles -- pixel-fur strands, hue-shifted clumps and the plains tuft cut-outs
      hairScale: 0.9, hairAmt: 0.20, grizzleAmt: 0.10,
      strandAmt: 0.30, clumpCol: '#9a4d29', clumpAmt: 0.35, clumpScale: 0.9,
      tufts: true
    },
    // Bubalus arnee. The whole shipped bison set is brown at hue 17-29 / L 0.13-0.23,
    // so the open slot is COOL: a blue-slate hide with almost no red in it. Treatment
    // is the identity as much as the colour -- sparse hair over bare grey skin, deep
    // neck and shoulder folds, and a heavy wallowing mud crust that goes over the back
    // as well as up the legs.
    water_buffalo: {
      back: '#3d424b', flank: '#4e535c', belly: '#5f6167',
      muzzle: '#8a827c', muzzleAmt: 0.42,
      hoof: '#26282e',
      // huge corrugated crescents, dark at the base and worn pale at the tips --
      // the single most recognisable thing about a wild Asian buffalo
      horn: '#3c3a33', hornTip: '#a8a294', hornRidge: 0.20, hornRidgeFreq: 3.4,
      legCol: '#2e3138', legAmt: 0.34,
      shadeLo: 0.10, shadeHi: 0.95,
      hairScale: 1.25, hairAmt: 0.12, grizzleAmt: 0.06,
      bareCol: '#7d6f6c', bareAmt: 0.24, bareScale: 1.6,
      foldAmt: 0.07,
      // dirty-cream stockings below knee and hock, and the pale throat chevron
      sockCol: '#b9b5a6', sockAmt: 0.80, sockTop: 5.4,
      bibCol: '#b0ada0', bibAmt: 0.55,
      mudCol: '#6d5b41', mudAmt: 0.42, mudLo: 7.0,
      noForelock: true
    },
    // Bos javanicus. Rufous-chestnut cow, and the diagnostic is not the body colour at
    // all -- it is the four white stockings and the white rump patch, so those carry the
    // identity. Sits lighter and far more saturated than any shipped bison.
    banteng: {
      back: '#5a3521', flank: '#75452b', belly: '#8b5c3e',
      // pale muzzle ring, white stockings and white rump: banteng carry three white
      // marks on a red-brown cow, and those marks ARE the species
      muzzle: '#c6b9a4', muzzleAmt: 0.50,
      horn: '#2b2620', hornTip: '#4c443a', hornRidge: 0.10, hornRidgeFreq: 2.2,
      hoof: '#efebe1',
      legCol: '#6b3c22', legAmt: 0.18,
      shadeLo: 0.05, shadeHi: 0.90,
      hairScale: 1.7, hairAmt: 0.085, grizzleAmt: 0.0,
      crown: '#3f2314', crownAmt: 0.40,
      sockCol: '#f2eee4', sockAmt: 0.92, sockTop: 5.6,
      rumpAmt: 0.85,
      noForelock: true
    },
    // Pelorovis oldowayensis. A dry-country savanna bovid, so the open slot is PALE and
    // yellow: nothing shipped is above L 0.24. Short sun-bleached coat, dark face and
    // dock, pale dust on the legs rather than mud.
    pelorovis: {
      back: '#5c4a2c', flank: '#7b653e', belly: '#9f8a5c',
      muzzle: '#3b2f24', muzzleAmt: 0.50,
      // an African buffalo's horns meet in a heavy keratin BOSS across the forehead,
      // so the base is pale and worn and the sweep darkens toward the tips
      horn: '#8d8168', hornTip: '#3a3229', hornRidge: 0.14, hornRidgeFreq: 2.0,
      hoof: '#2c2620',
      legCol: '#6d5836', legAmt: 0.30,
      shadeLo: 0.08, shadeHi: 0.92,
      hairScale: 2.1, hairAmt: 0.115, grizzleAmt: 0.10,
      crown: '#3e2f1c', crownAmt: 0.55,
      bossCol: '#b9ac8b', bossAmt: 0.80,
      bareCol: '#8e7a54', bareAmt: 0.22, bareScale: 1.3,
      mudCol: '#cdb98f', mudAmt: 0.26, mudLo: 3.0,
      noForelock: true
    }
  };

  var name = window.SPEC_NAME;
  var S = SPECIES[name];
  var c = BISON.paint(S);
  var eyed = BISON.stampEyes(c, window.OUT +
    '../src/main/resources/assets/untamedwilds/textures/entity/bison/plains.png');
  MM.bleed(c, 3, MM.faceRects(BISON.cleared(S)));
  // After the bleed, or the bleed would refill the cut-outs.
  var tufted = S.tufts ? BISON.stampTufts(c, window.OUT +
    '../src/main/resources/assets/untamedwilds/textures/entity/bison/plains.png', S) : 0;
  MM.show(c);

  var d = c.getContext('2d').getImageData(0, 0, c.width, c.height).data;
  var op = 0, sum = [0, 0, 0];
  for (var i = 0; i < d.length; i += 4) {
    if (d[i + 3] > 0) { op++; sum[0] += d[i]; sum[1] += d[i + 1]; sum[2] += d[i + 2]; }
  }
  return JSON.stringify({
    species: name, opaque: op, eyeTexels: eyed, bled: MM.bledCount, tufted: tufted,
    mean: [Math.round(sum[0] / op), Math.round(sum[1] / op), Math.round(sum[2] / op)]
  });
})()
