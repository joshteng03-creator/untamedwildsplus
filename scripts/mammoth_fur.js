// Re-hair the six woolly-coat parts (fur_mane, fur_neck, fur_chest, fur_rump, fur_skirt_left/right)
// on a mammoth skin, leaving every other texel exactly as it is. User, 2026-09-30: "the furs at the
// side read like flat rectangles, make that and all other fur parts look more like hair".
//
//   * STRANDS: long vertical streaks down each texel column (UV v runs down a side face), four
//     tones, darker at the roots and lighter at the tips -- the pixel-hair language of the shipped
//     bison and camel wool, not horizontal banding.
//   * A RAGGED HEM: on every side/front/back face each column ends at its own random length (0-4
//     texels up from the bottom edge) via alpha, so the bottom hangs as separate locks instead of a
//     ruler-straight panel edge. Top faces keep full coverage (hair lying along the back).
// Colour is the species' own: the mean of its existing fur texels.
// Input: scratch/skins_bleed/<sp>.png (already seam-sealed). Output: scratch/skins_fur/<sp>.png.
(function () {
  var FUR = ['fur_mane', 'fur_neck', 'fur_chest', 'fur_rump', 'fur_skirt_left', 'fur_skirt_right'];
  var TW = Project.texture_width, TH = Project.texture_height;
  var out = [];
  window.MF_SPECIES.forEach(function (sp) {
    var base = MM.load(window.OUT + 'skins_bleed/' + sp + '.png').getContext('2d').getImageData(0, 0, TW, TH).data;
    var sum = [0, 0, 0], cnt = 0;
    FUR.forEach(function (n) {
      var F = MM.map[n].faces;
      for (var k in F) {
        var f = F[k];
        for (var y = f.v0; y < f.v1; y++) for (var x = f.u0; x < f.u1; x++) {
          var i = (y * TW + x) * 4;
          if (base[i + 3] > 0) { sum[0] += base[i]; sum[1] += base[i + 1]; sum[2] += base[i + 2]; cnt++; }
        }
      }
    });
    var mean = [sum[0] / cnt, sum[1] / cnt, sum[2] / cnt];
    var root = MM.mul(mean, 0.72), tip = MM.mix(mean, [214, 170, 110], 0.28);

    var cv = MM.paint(function (o) {
      var i = (o.v * TW + o.u) * 4;
      if (FUR.indexOf(o.part) < 0) {
        return base[i + 3] ? [base[i], base[i + 1], base[i + 2]] : [0, 0, 0, 0];
      }
      var m = MM.map[o.part], p = o.wp;
      var hang = o.face !== 'up' && o.face !== 'down';
      // how far up from this part's lowest point, 0 = bottom edge
      var up = p.y - m.lo.y;
      if (hang) {
        var cut = Math.floor(MM.h(o.u, 3, 0, 61) * 5);   // 0..4 texels of hem per column
        if (up < cut) return [0, 0, 0, 0];
      }
      // The underside is cleared: a solid bottom face showed as a flat plank under the hanging
      // locks. MC draws both sides of every face (entityCutoutNoCull), so the hair reads as a
      // curtain from below as well.
      // Thin skirts only: a deep part (chest, neck, rump) with no underside read as a hollow cage
      // from below, and scattered gaps read as a grate, so its underside is solid dark streaked hair.
      if (o.face === 'down') {
        if (o.part === 'fur_skirt_left' || o.part === 'fur_skirt_right') return [0, 0, 0, 0];
        // solid, dark and streaked: random gaps read as a grate from below
        return MM.cl(MM.mul(root, 0.62 + 0.30 * (Math.floor(MM.h(o.u, Math.floor(o.v / 3), 0, 19) * 4) / 3)));
      }
      // root -> tip along the hang, so each lock lightens toward its end
      var along = hang ? 1 - MM.smooth(0, m.hi.y - m.lo.y, up) : 0.5;
      var col = MM.mix(root, tip, 0.25 + 0.6 * along);
      // strands: a tone per ~4-texel run down each column, staggered per column
      var run = Math.floor((o.v + 7 * MM.h(o.u, 2, 0, 5)) / 4);
      var tone = Math.floor(MM.h(o.u, run, 0, 19) * 4) / 3 - 0.5;
      col = MM.mul(col, 1 + 0.36 * tone);
      // a dark parting line between some columns, which is what separates locks
      if (MM.h(o.u, 9, 0, 71) < 0.22) col = MM.mul(col, 0.78);
      col = MM.mul(col, 1 + 0.10 * (MM.fbm(p, 0.6, 2, 3) - 0.5));
      return MM.cl(col);
    });
    // everything outside the fur faces (dead zones included) exactly as it was
    var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, TW, TH);
    var furRects = MM.faceRects(FUR);
    var inFur = function (x, y) {
      for (var q = 0; q < furRects.length; q++) {
        var R = furRects[q];
        if (x >= R[0] && x < R[2] && y >= R[1] && y < R[3]) return true;
      }
      return false;
    };
    var cut = 0;
    for (var j = 0; j < img.data.length; j += 4) {
      var px = (j / 4) % TW, py = Math.floor(j / 4 / TW);
      if (!inFur(px, py)) {
        img.data[j] = base[j]; img.data[j + 1] = base[j + 1]; img.data[j + 2] = base[j + 2]; img.data[j + 3] = base[j + 3];
      } else if (img.data[j + 3] === 0) cut++;
    }
    cx.putImageData(img, 0, 0);
    MM.show(cv);
    MM.canvas = cv;
    MM.save(window.OUT + 'skins_fur/' + sp + '.png', cv);
    out.push(sp + ': fur mean ' + mean.map(Math.round).join(',') + ', hem cut-outs ' + cut);
  });
  return out.join(' | ');
})();
