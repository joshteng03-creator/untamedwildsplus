// Propagate eremotherium's approved countershade (2026-07-22) to another ground-sloth skin,
// ON TOP of that skin's existing art -- so megatherium / megalonyx keep their own shaggy streak
// treatment and palette, and gain the same diverging dorsal/ventral structure:
//   ventral  -> a pale cream of the species' OWN coat colour, strength 0.55
//   dorsal   -> a light sun-bleach of it, strength 0.22
//   flanks   unchanged
// Per-part weights exactly as the eremotherium pass: torso 1.0, tail 0.6, head 0.5, limbs ~0.2 (no
// white socks); claws and eyes untouched. window.CS_SPECIES names the skin to process.
(function () {
  var SP = window.CS_SPECIES;
  var PATH = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/ground_sloth/' + SP + '.png';
  var base = MM.load(PATH).getContext('2d').getImageData(0, 0, 256, 256).data;
  var W = { body_hips: 1.0, body_arch: 1.0, body_chest: 1.0, neck: 0.8,
            tail_1: 0.6, tail_2: 0.6, tail_3: 0.6, head: 0.5, snout: 0.5, jaw: 0.5, ear_left: 0.4, ear_right: 0.4,
            arm_left_1: 0.25, arm_right_1: 0.25, leg_left_thigh: 0.25, leg_right_thigh: 0.25,
            arm_left_2: 0.15, arm_right_2: 0.15, leg_left_shank: 0.15, leg_right_shank: 0.15 };
  // species coat colour = mean of the torso's opaque texels
  var sum = [0, 0, 0], cnt = 0;
  ['body_hips', 'body_arch', 'body_chest'].forEach(function (n) {
    var F = MM.map[n].faces;
    for (var k in F) {
      var f = F[k];
      for (var y = f.v0; y < f.v1; y++) for (var x = f.u0; x < f.u1; x++) {
        var i = (y * 256 + x) * 4;
        if (base[i + 3] > 0) { sum[0] += base[i]; sum[1] += base[i + 1]; sum[2] += base[i + 2]; cnt++; }
      }
    }
  });
  var avg = [sum[0] / cnt, sum[1] / cnt, sum[2] / cnt];
  var CREAM = MM.mix(avg, [236, 226, 206], 0.62);
  var SAND = MM.mix(avg, [214, 198, 170], 0.40);
  // dorsal (high y) -> ventral (low y) over the torso's height
  var Y0 = 14.0, Y1 = 32.0;

  var cv = MM.paint(function (o) {
    var i = (o.v * 256 + o.u) * 4;
    if (base[i + 3] === 0) return [0, 0, 0, 0];
    var col = [base[i], base[i + 1], base[i + 2]];
    var w = W[o.part];
    if (!w) return col;   // claws, eyes, hands, feet: untouched
    var t = (o.wp.y - Y0) / (Y1 - Y0);   // 0 ventral .. 1 dorsal
    var vent = MM.smooth(0.45, 0.0, t) * (o.face === 'down' ? 1.0 : 0.85);
    var dors = MM.smooth(0.65, 1.0, t);
    col = MM.mix(col, MM.mul(CREAM, 0.9 + 0.2 * (col[0] + col[1] + col[2]) / (avg[0] + avg[1] + avg[2]) - 0.1), 0.55 * w * vent);
    col = MM.mix(col, SAND, 0.22 * w * dors);
    return MM.cl(col);
  });
  // keep every texel the reference had, exactly where the paint did not reach (dead zones etc.)
  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, 256, 256);
  for (var j = 0; j < img.data.length; j += 4) {
    if (img.data[j + 3] === 0 && base[j + 3] !== 0) {
      img.data[j] = base[j]; img.data[j + 1] = base[j + 1]; img.data[j + 2] = base[j + 2]; img.data[j + 3] = base[j + 3];
    }
  }
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/' + SP + '.png', cv);
  return SP + ' countershaded; coat mean ' + avg.map(Math.round).join(',');
})();
