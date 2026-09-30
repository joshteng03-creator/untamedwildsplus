// Seal the sub-texel seam holes in every shipped mammoth skin (the leg/toe "invisible spots" from
// the 2026-07-21 smoke test -- sub-texel UV sampling of unpainted texels, NOT mipmapping; see
// reference_uv_edge_bleed). The art is untouched: MM.bleed only fills TRANSPARENT texels.
//
// Protected, so bleed cannot refill them (reference_bleed_protect_cleared_parts):
//   * the eye planes (their inward half is alpha-0 on purpose)
//   * any part the skin deliberately clears -- detected as a part whose face rects are more than
//     half transparent (e.g. the woolly fur on non-woolly species)
// Reports, per skin, the transparent texels in the 1-texel halo around every unprotected face rect
// before and after. Writes to scratch/skins_bleed/.
(function () {
  var SPECIES = window.MB_SPECIES;
  var TEX = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/mammoth/';
  var TW = Project.texture_width, TH = Project.texture_height;
  var names = Object.keys(MM.map);
  var out = [];
  SPECIES.forEach(function (sp) {
    var cv = MM.load(TEX + sp + '.png');
    var cx = cv.getContext('2d');
    var d = cx.getImageData(0, 0, TW, TH).data;
    var alpha = function (x, y) { return (x < 0 || y < 0 || x >= TW || y >= TH) ? 255 : d[(y * TW + x) * 4 + 3]; };
    var protect = ['eye_left', 'eye_right'];
    names.forEach(function (n) {
      if (protect.indexOf(n) >= 0) return;
      var F = MM.map[n].faces, tot = 0, clear = 0;
      for (var k in F) {
        var f = F[k];
        for (var y = f.v0; y < f.v1; y++) for (var x = f.u0; x < f.u1; x++) { tot++; if (alpha(x, y) === 0) clear++; }
      }
      if (tot && clear / tot > 0.5) protect.push(n);
    });
    var halo = function () {
      var holes = 0;
      names.forEach(function (n) {
        if (protect.indexOf(n) >= 0) return;
        var F = MM.map[n].faces;
        for (var k in F) {
          var f = F[k];
          for (var x = f.u0 - 1; x <= f.u1; x++) { if (alpha(x, f.v0 - 1) === 0) holes++; if (alpha(x, f.v1) === 0) holes++; }
          for (var y = f.v0; y < f.v1; y++) { if (alpha(f.u0 - 1, y) === 0) holes++; if (alpha(f.u1, y) === 0) holes++; }
        }
      });
      return holes;
    };
    var before = halo();
    MM.bleed(cv, 3, MM.faceRects(protect));
    d = cx.getImageData(0, 0, TW, TH).data;
    var after = halo();
    MM.save(window.OUT + 'skins_bleed/' + sp + '.png', cv);
    out.push(sp + ': halo holes ' + before + ' -> ' + after + ', protected ' + (protect.length - 2) + ' cleared parts');
  });
  return out.join(' | ');
})();
