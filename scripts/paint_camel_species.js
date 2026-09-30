(function () {
  var F = 'body_hump_front', B = 'body_hump_back', M = 'body_hump_centre';

  var SPECIES = {
    // Camelus knoblochi -- a giant cold-steppe Bactrian relative, so TWO humps and the
    // full wool overlay, read straight off bactrian.png's alpha pattern. Colour has to
    // clear bactrian (H32/L0.29) and wild_bactrian: goes paler and greyer, a
    // frost-bleached winter coat rather than the domestic camel's warm sand.
    camelus_knoblochi: {
      humps: [F, B], hasTail: false, hasWool: true,
      back: '#61573f', flank: '#7a7053', belly: '#928768',
      woolCol: '#3b3428', woolScale: 0.9,
      muzzle: '#9a9179', muzzleAmt: 0.45,
      legCol: '#463f31', legAmt: 0.28, hoof: '#2a251e',
      shadeLo: 0.30, shadeHi: 0.98,
      hairScale: 1.1, hairAmt: 0.13, grizzleAmt: 0.14
    },
    // Domestic llama: no hump, llama tail, and the diagnostic is the PIEBALD -- big
    // irregular cream patches on brown that no wild camelid in the set has.
    llama: {
      humps: [], hasTail: true, hasWool: false,
      back: '#33200f', flank: '#472c1a', belly: '#5f3f27',
      patchCol: '#ece1cb', patchAmt: 0.95, patchScale: 0.20,
      muzzle: '#e2d7c0', muzzleAmt: 0.55, throatAmt: 0.45,
      legCol: '#5a3b27', legAmt: 0.20, hoof: '#2e2721',
      shadeLo: 0.28, shadeHi: 0.95,
      hairScale: 1.5, hairAmt: 0.09, grizzleAmt: 0.0
    },
    // Alpaca: no hump, and the fleece IS the animal, so this is the one llama-type that
    // turns the wool overlay ON -- a dense uniform crimped fleece in undyed cream.
    alpaca: {
      humps: [], hasTail: true, hasWool: true,
      // silver-cream, an undyed fleece: the low-saturation slot nothing else occupies
      back: '#b4ab97', flank: '#c4bba6', belly: '#d2cab6',
      woolCol: '#c9c0aa', woolScale: 2.4,
      muzzle: '#e4dccb', muzzleAmt: 0.35,
      legCol: '#a49a86', legAmt: 0.16, hoof: '#3a332a',
      shadeLo: 0.25, shadeHi: 0.95,
      hairScale: 2.6, hairAmt: 0.14, grizzleAmt: 0.05
    },
    // Hemiauchenia -- the long-legged Pleistocene llama of open country. No hump, no
    // wool, and a dry sandy dun with heavy dust up the very long legs.
    hemiauchenia: {
      humps: [], hasTail: true, hasWool: false,
      // hard countershading is the treatment here -- a dark dorsal saddle over a near-white
      // belly and throat, which is what an open-country animal actually looks like and what
      // separates it from the flat-toned alpaca
      back: '#6b5228', flank: '#9a7f4c', belly: '#ded0ac',
      muzzle: '#eae0c6', muzzleAmt: 0.55, throatAmt: 0.70,
      legCol: '#7d6438', legAmt: 0.26, hoof: '#332c24',
      shadeLo: 0.18, shadeHi: 0.90,
      hairScale: 1.8, hairAmt: 0.12, grizzleAmt: 0.11,
      dustCol: '#d6c9a8', dustAmt: 0.38
    }
  };

  var name = window.SPEC_NAME;
  var S = SPECIES[name];
  var c = CAMEL.paint(S);
  var eyed = CAMEL.stampEyes(c, window.OUT +
    '../src/main/resources/assets/untamedwilds/textures/entity/camel/bactrian.png');
  MM.bleed(c, 3, MM.faceRects(CAMEL.cleared(S)));
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
