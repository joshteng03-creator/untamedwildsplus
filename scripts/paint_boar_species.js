(function () {
  var SPECIES = {
    // Hylochoerus meinertzhageni -- the biggest wild pig alive: near-black, very shaggy,
    // with a heavy crest and big pale facial warts under the eyes.
    giant_forest_hog: {
      hasCrest: true, hasTusks: true,
      back: '#33291f', flank: '#463a2d', belly: '#5c4d3d',
      // crest LIGHTER than the body, or a black mane on a black hog reads as nothing
      crestCol: '#6b5b45',
      snout: '#6b5847', snoutAmt: 0.55,
      paleCol: '#a3937b', cheekAmt: 0.85,
      legCol: '#241d16', legAmt: 0.40,
      shadeLo: 0.10, shadeHi: 0.85,
      hairScale: 0.9, hairAmt: 0.28, grizzleAmt: 0.17,
      mudCol: '#4a3a24', mudAmt: 0.30
    },
    // Babyrousa -- famously almost hairless, so NO crest and the identity is bare
    // wrinkled hide. Its upward-curling tusks are not in this rig; the forward tusk
    // cubes are the closest the geometry allows, and that is a stated approximation.
    babirusa: {
      hasCrest: false, hasTusks: true,
      back: '#6e6157', flank: '#7d7166', belly: '#8d8175',
      snout: '#9a8b7d', snoutAmt: 0.45,
      legCol: '#5c5148', legAmt: 0.25,
      shadeLo: 0.15, shadeHi: 0.90,
      hairScale: 2.4, hairAmt: 0.05, grizzleAmt: 0.03,
      bareCol: '#95867a', bareAmt: 0.35, bareScale: 1.1,
      foldAmt: 0.085,
      mudCol: '#6b5a3e', mudAmt: 0.22
    },
    // Kubanochoerus -- a huge Miocene suid. Its frontal horn cannot be modelled without
    // new geometry, so the skin carries the identity instead: a pale grizzled forehead
    // shield where the horn boss sat, on a warm tawny coat.
    kubanochoerus: {
      hasCrest: true, hasTusks: true,
      back: '#6b4d24', flank: '#84632f', belly: '#a58551',
      crestCol: '#4a3418',
      snout: '#4c3b28', snoutAmt: 0.50,
      paleCol: '#cbb98f', cheekAmt: 0.40,
      bossCol: '#d8c79f', bossAmt: 0.85,
      legCol: '#553d1c', legAmt: 0.30,
      shadeLo: 0.12, shadeHi: 0.88,
      hairScale: 1.3, hairAmt: 0.16, grizzleAmt: 0.14,
      mudCol: '#a8905f', mudAmt: 0.20
    }
  };

  var name = window.SPEC_NAME;
  var S = SPECIES[name];
  var c = BOAR.paint(S);
  var stamped = BOAR.stamp(c, window.OUT +
    '../src/main/resources/assets/untamedwilds/textures/entity/boar/eurasian_1.png', S);
  MM.bleed(c, 3, MM.faceRects(BOAR.cleared(S)));
  MM.show(c);

  var d = c.getContext('2d').getImageData(0, 0, c.width, c.height).data;
  var op = 0, sum = [0, 0, 0];
  for (var i = 0; i < d.length; i += 4) {
    if (d[i + 3] > 0) { op++; sum[0] += d[i]; sum[1] += d[i + 1]; sum[2] += d[i + 2]; }
  }
  return JSON.stringify({
    species: name, opaque: op, stamped: stamped, bled: MM.bledCount,
    mean: [Math.round(sum[0] / op), Math.round(sum[1] / op), Math.round(sum[2] / op)]
  });
})()
