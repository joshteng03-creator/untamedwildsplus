(function () {
  var SPECIES = {
    // Canis lupus -- the conspicuous absence from the roster. Agouti grizzle with a dark
    // saddle, pale mask, throat and legs. It has to stay clearly APART from the shipped
    // pleistocene_wolf and protocyon, which are the other two grey canids.
    gray_wolf: {
      back: '#6b6355', flank: '#867c6b', belly: '#b0a692',
      saddleCol: '#3f3a33', saddleAmt: 0.70, saddleLo: 0.60, saddleHi: 0.84,
      ruffCol: '#9a9078', ruffAmt: 0.45,
      maskCol: '#4a4238', maskAmt: 0.45, paleCol: '#e2dac6', lipAmt: 0.70,
      throatAmt: 0.60, earCol: '#4e453a', earAmt: 0.55,
      legCol: '#9c9280', legAmt: 0.35,
      tailTipCol: '#3a352e', tailTipAmt: 0.55,
      noseCol: '#211c19',
      shadeLo: 0.15, shadeHi: 0.86,
      hairScale: 1.5, hairAmt: 0.15, agoutiAmt: 0.16
    },
    // Canis latrans -- smaller, warmer and redder than the wolf, big pale-backed ears,
    // rufous legs and a black-tipped tail.
    coyote: {
      back: '#7a5730', flank: '#96703f', belly: '#c2a97e',
      saddleCol: '#4a3d2c', saddleAmt: 0.70, saddleLo: 0.58, saddleHi: 0.86,
      ruffCol: '#b09a72', ruffAmt: 0.30,
      maskCol: '#6e5636', maskAmt: 0.40, paleCol: '#eee4cc', lipAmt: 0.75,
      throatAmt: 0.65, earCol: '#8a5f2e', earAmt: 0.60,
      legCol: '#b56a26', legAmt: 0.55,
      tailTipCol: '#2c2620', tailTipAmt: 0.85,
      noseCol: '#241e1a',
      shadeLo: 0.16, shadeHi: 0.88,
      hairScale: 1.8, hairAmt: 0.13, agoutiAmt: 0.14
    },
    // Chrysocyon brachyurus. The rig cannot give it the real stilt legs, so the COLOUR
    // has to: vivid rufous body, hard black stockings, white throat blaze and tail tip,
    // and a black dorsal mane -- the four marks that make a maned wolf recognisable.
    maned_wolf: {
      back: '#a34a12', flank: '#bd6420', belly: '#d99a52',
      saddleCol: '#1e1611', saddleAmt: 0.20, saddleLo: 0.76, saddleHi: 0.94,
      ruffCol: '#171210', ruffAmt: 0.95,
      maskCol: '#2a1d14', maskAmt: 0.60, paleCol: '#f4ece0', lipAmt: 0.55,
      throatAmt: 0.95, earCol: '#8a3c0e', earAmt: 0.45,
      legCol: '#191310', legAmt: 0.92,
      tailTipCol: '#f2ebdd', tailTipAmt: 0.90,
      noseCol: '#1a1512',
      shadeLo: 0.20, shadeHi: 0.92,
      hairScale: 1.6, hairAmt: 0.11, agoutiAmt: 0.05
    },
    // Xenocyon lycaonoides, ancestor of the African wild dog -- deliberately NOT
    // patchworked, since african_wild_dog already ships with that treatment. A dark, cold
    // grizzled canid with a pale mask.
    xenocyon: {
      back: '#403c38', flank: '#575048', belly: '#7d7469',
      saddleCol: '#26231f', saddleAmt: 0.60, saddleLo: 0.58, saddleHi: 0.84,
      ruffCol: '#6b6459', ruffAmt: 0.40,
      maskCol: '#2e2a25', maskAmt: 0.55, paleCol: '#ccc3b0', lipAmt: 0.60,
      throatAmt: 0.45, earCol: '#242120', earAmt: 0.70,
      legCol: '#332f2b', legAmt: 0.40,
      tailTipCol: '#221f1c', tailTipAmt: 0.70,
      noseCol: '#1c1917',
      shadeLo: 0.14, shadeHi: 0.84,
      hairScale: 1.4, hairAmt: 0.16, agoutiAmt: 0.18
    },
    // Speothos venaticus -- tiny, uniform, almost otter-like: dark chocolate body with a
    // markedly PALER head and shoulders, which is the whole pattern.
    bush_dog: {
      back: '#3a2416', flank: '#4a2f1c', belly: '#5c3d26',
      saddleCol: '#2a1a10', saddleAmt: 0.35, saddleLo: 0.55, saddleHi: 0.85,
      ruffCol: '#8a6a44', ruffAmt: 0.75,
      maskCol: '#8f6f48', maskAmt: 0.85, paleCol: '#b89a70', lipAmt: 0.55,
      throatAmt: 0.35, earCol: '#7a5c3a', earAmt: 0.40,
      legCol: '#2a1a10', legAmt: 0.45,
      tailTipCol: '#1f140d', tailTipAmt: 0.70,
      noseCol: '#1a120d',
      shadeLo: 0.18, shadeHi: 0.90,
      hairScale: 2.2, hairAmt: 0.09, agoutiAmt: 0.06
    }
  };

  var name = window.SPEC_NAME;
  var S = SPECIES[name];
  var c = CANID.paint(S);
  var eyed = CANID.stampEyes(c, window.OUT +
    '../src/main/resources/assets/untamedwilds/textures/entity/dire_wolf/dire_wolf.png');
  MM.bleed(c, 3, MM.faceRects(CANID.cleared(S)));
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
