(function () {
  var SPECIES = {
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
  MM.show(c);

  var d = c.getContext('2d').getImageData(0, 0, c.width, c.height).data;
  var op = 0, sum = [0, 0, 0];
  for (var i = 0; i < d.length; i += 4) {
    if (d[i + 3] > 0) { op++; sum[0] += d[i]; sum[1] += d[i + 1]; sum[2] += d[i + 2]; }
  }
  return JSON.stringify({
    species: name, opaque: op, eyeTexels: eyed, bled: MM.bledCount,
    mean: [Math.round(sum[0] / op), Math.round(sum[1] / op), Math.round(sum[2] / op)]
  });
})()
