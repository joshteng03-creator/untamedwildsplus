// 2.8.0 review driver: repaint ONLY the coat of a shipped skin with woollib.js. Set window.WOOL_RUN =
// {painter, specVar, species, tex, out} and eval after boot_paint.js + woollib.js on the species' rig.
// The painter runs twice on the same rig -- WOOL_MODE 'old' (the shipped coat rule) and 'new' (messy wool). The texels
// the two runs disagree on are exactly the coat's (plus its hem cuts and edge bleed); the shipped PNG keeps every other
// texel -- eyes, tusks, claws, toenails, ears, soles -- byte for byte. reproMismatch counts texels OUTSIDE that mask
// where the 'old' run differs from the shipped PNG: ~0 proves the rig is the one the skin was painted on.
(function () {
  var fs = require('fs');
  var R = window.WOOL_RUN, MM = window.MM;
  var src = fs.readFileSync(window.SP + R.painter, 'utf8');
  window[R.specVar] = R.species;
  var TW = Project.texture_width, TH = Project.texture_height, N = TW * TH * 4;
  window.WOOL_MODE = 'old';
  eval(src);
  var A = MM.canvas.getContext('2d').getImageData(0, 0, TW, TH).data.slice();
  window.WOOL_MODE = 'new';
  eval(src);
  var B = MM.canvas.getContext('2d').getImageData(0, 0, TW, TH).data;
  var O = MM.load(R.tex), ox = O.getContext('2d'), od = ox.getImageData(0, 0, TW, TH);
  var changed = 0, repro = 0, kept = 0;
  for (var i = 0; i < N; i += 4) {
    if (A[i] !== B[i] || A[i + 1] !== B[i + 1] || A[i + 2] !== B[i + 2] || A[i + 3] !== B[i + 3]) {
      od.data[i] = B[i]; od.data[i + 1] = B[i + 1]; od.data[i + 2] = B[i + 2]; od.data[i + 3] = B[i + 3];
      changed++;
    } else {
      kept++;
      var dd = Math.abs(A[i] - od.data[i]) + Math.abs(A[i + 1] - od.data[i + 1]) + Math.abs(A[i + 2] - od.data[i + 2]);
      if ((od.data[i + 3] > 0 || A[i + 3] > 0) && (dd > 6 || A[i + 3] !== od.data[i + 3])) repro++;
    }
  }
  ox.putImageData(od, 0, 0);
  var dir = window.OUT + 'wool2';
  if (!fs.existsSync(dir)) fs.mkdirSync(dir);
  fs.writeFileSync(R.out, Buffer.from(O.toDataURL('image/png').split(',')[1], 'base64'));
  MM.show(O);
  return JSON.stringify({ species: R.species, coatTexels: changed, keptTexels: kept, reproMismatch: repro });
})()
