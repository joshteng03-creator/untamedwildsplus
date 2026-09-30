(function () {
  var SPECIES = {
    // Rangifer tarandus. The pale neck cape over a dark body is the mark, and it is the
    // one deer that keeps its rack into winter -- palmate, nearly as big as megaloceros.
    caribou: {
      back: '#413a32', flank: '#524a40', belly: '#6e675b',
      paleCol: '#ddd7c7', capeAmt: 0.92, capeZ0: -22.0, capeZ1: -3.0,
      bellyAmt: 0.40, bellyY0: 13.0, bellyY1: 17.5,
      sockAmt: 0.65, sockTop: 5.0,
      rumpAmt: 0.45,
      muzzle: '#cfc8b6', muzzleAmt: 0.45,
      antler: '#6b5c46', antlerTip: '#b9ae95', rackSpan: 24.0,
      legCol: '#3d352b', legAmt: 0.30, hoof: '#22201c',
      shadeLo: 0.10, shadeHi: 0.62,
      hairScale: 1.4, hairAmt: 0.11, grizzleAmt: 0.10
    },
    // Odocoileus virginianus -- warm tan, and everything diagnostic is WHITE: throat,
    // eye ring, belly, inner legs and the raised tail flag it is named for.
    white_tailed: {
      back: '#8a6134', flank: '#a2763f', belly: '#c9ab7e',
      paleCol: '#f0ece0', bibAmt: 0.75, bellyAmt: 0.80, bellyY0: 14.5, bellyY1: 19.0,
      rumpAmt: 0.80,
      muzzle: '#e6dfcd', muzzleAmt: 0.55,
      antler: '#6f6047', antlerTip: '#c2b79c', rackSpan: 12.0,
      legCol: '#8a6538', legAmt: 0.18, hoof: '#24211c',
      shadeLo: 0.12, shadeHi: 0.72,
      hairScale: 1.7, hairAmt: 0.09, grizzleAmt: 0.05
    },
    // Dama dama. Palmate rack on a small deer, chestnut with bold white spot rows and a
    // dark spine line -- the most strongly patterned deer in the set.
    fallow: {
      back: '#7a3a0c', flank: '#a55214', belly: '#d09a55',
      paleCol: '#f4ecd8', spotAmt: 0.90, spotSpacing: 4.2,
      dorsalCol: '#3a2412', dorsalAmt: 0.70,
      bellyAmt: 0.65, bellyY0: 14.0, bellyY1: 18.5, rumpAmt: 0.55,
      muzzle: '#e0d6c0', muzzleAmt: 0.45,
      antler: '#6a5b45', antlerTip: '#b8ad93', rackSpan: 14.0,
      legCol: '#8a5c2c', legAmt: 0.16, hoof: '#22201b',
      shadeLo: 0.14, shadeHi: 0.74,
      hairScale: 1.9, hairAmt: 0.08, grizzleAmt: 0.0
    },
    // Cervus nippon -- also spotted, so it MUST separate from fallow on tone and spacing:
    // darker, cooler, finer and sparser spots, no palmate rack.
    sika: {
      back: '#3e2410', flank: '#5c3617', belly: '#87603a',
      paleCol: '#e6dcc6', spotAmt: 0.62, spotSpacing: 6.8,
      dorsalCol: '#2c1d12', dorsalAmt: 0.55,
      bellyAmt: 0.40, bellyY0: 14.0, bellyY1: 18.0, rumpAmt: 0.60,
      muzzle: '#cdc2ab', muzzleAmt: 0.40,
      antler: '#5f5240', antlerTip: '#a89e86', rackSpan: 11.0,
      legCol: '#3f2e1f', legAmt: 0.22, hoof: '#201d19',
      shadeLo: 0.12, shadeHi: 0.70,
      hairScale: 2.1, hairAmt: 0.10, grizzleAmt: 0.07
    },
    // Eucladoceros -- the Pliocene comb-antlered deer, all tines and no palm. Plain
    // grizzled grey-brown so the enormous rack is what reads.
    eucladoceros: {
      // hard countershading: a dark grizzled saddle over a pale flank and near-white
      // underside, so the animal reads in three bands instead of one flat grey
      back: '#4f4a3c', flank: '#8b8574', belly: '#c3bda8',
      paleCol: '#ded8c4', bibAmt: 0.55, bellyAmt: 0.45, bellyY0: 15.0, bellyY1: 20.0,
      bellyLegY0: 9.0, bellyLegY1: 16.0, rumpAmt: 0.40,
      // painted neck mane -- the rig has no mane box by design
      maneCol: '#3b3529', maneAmt: 0.75,
      maskCol: '#463f31', maskAmt: 0.55,
      muzzle: '#cec6ae', muzzleAmt: 0.50,
      antler: '#75664e', antlerTip: '#c6bba0', rackSpan: 26.0,
      legCol: '#4e4536', legAmt: 0.30, hoof: '#232019',
      shadeLo: 0.10, shadeHi: 0.68,
      hairScale: 1.5, hairAmt: 0.15, grizzleAmt: 0.20
    }
  };

  var name = window.SPEC_NAME;
  var S = SPECIES[name];
  var c = DEER.paint(S);
  var eyed = DEER.stampEyes(c, window.OUT +
    '../src/main/resources/assets/untamedwilds/textures/entity/deer/red_deer.png');
  MM.bleed(c, 3, MM.faceRects(DEER.cleared(S)));
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
