// Lion male manes, on ModelBigCat (128x64) -- older-skin pass 2026-11-15. window.LION is '1' or '2'.
// The shipped lion_1_male / lion_2_male bodies hold up next to the new art, but the neck_mane box
// (11 x 12 x 8) rendered as a flat dark-brown block: its strands were there in the PNG but too dark and
// too low-contrast to read. So ONLY the mane box is repainted; every other texel is copied from the
// shipped PNG verbatim (the box is integer-sized, so there are no fractional fringes to bleed).
// A real mane is lightest where it frames the face and darkens toward the back and the chest, with long
// downward strands and lighter tips on top. Morph 1 is the classic tawny-blond mane, morph 2 the
// dark-maned lion. The lower edge of the side and back faces is cut into alternating one-texel tufts
// (the alpha trick the bison skirts and the American lion's ruff use) so the mane stops ending in a ruler line.
(function () {
  var M = window.LION === '2' ? '2' : '1';
  var P = M === '1' ? {
    face : MM.hex('#c99a5c'),   // the ruff that frames the face
    mid  : MM.hex('#94683a'),
    back : MM.hex('#5a3c22'),   // back of the neck and the chest
    dark : MM.hex('#352315'),
    tip  : MM.hex('#e0b878')
  } : {
    face : MM.hex('#a06a38'),
    mid  : MM.hex('#5e3b20'),
    back : MM.hex('#2e1d11'),
    dark : MM.hex('#1a100a'),
    tip  : MM.hex('#c48a4e')
  };
  var TEX = window.OUT + '../src/main/resources/assets/untamedwilds/textures/entity/big_cat/';
  var NAME = 'lion_' + M + '_male';
  var orig = MM.load(TEX + NAME + '.png');
  var B = MM.map['neck_mane'], lo = B.lo, hi = B.hi;
  var HC = { x: 0, y: 15.0 };   // where the head pokes through the front of the mane

  var cv = MM.paint(function (o) {
    if (o.part !== 'neck_mane') return null;
    var p = o.wp;
    var tz = (p.z - lo.z) / (hi.z - lo.z);          // 0 front (head side) .. 1 back
    var ty = (p.y - lo.y) / (hi.y - lo.y);          // 0 chest .. 1 crest
    var col;
    if (o.face === 'front') {
      // front face: a light ruff around the head, darkening to the outer rim
      var d = Math.sqrt(p.x * p.x / 30.0 + (p.y - HC.y) * (p.y - HC.y) / 40.0);
      col = MM.mix(P.face, P.mid, MM.smooth(0.45, 1.0, d));
    } else {
      var dark = MM.smooth(0.0, 1.0, 0.6 * tz + 0.55 * (1 - ty));
      col = MM.mix(P.face, P.mid, MM.smooth(0.0, 0.5, dark));
      col = MM.mix(col, P.back, MM.smooth(0.45, 1.0, dark));
    }
    if (o.face === 'down') col = MM.mix(col, P.dark, 0.55);
    // long strands: a per-column groove that runs the full length of the face, plus 4-tone pixel strands
    if (MM.h(o.u, 0, 0, 77) < 0.2) col = MM.mix(col, P.dark, 0.45);
    var rs = MM.h(o.u, Math.floor((o.v + 5 * MM.h(o.u, 1, 0, 3)) / 3), 0, 29);
    col = MM.mul(col, 1 + 0.34 * (Math.floor(rs * 4) / 3 - 0.5));
    // light tips on the crest and around the face
    var lit = o.face === 'up' ? 0.5 : (o.face === 'front' ? 0.35 : 0.25 * MM.smooth(0.7, 1.0, ty));
    if (MM.h(o.u, Math.floor(o.v / 2), 0, 91) < lit * 0.6) col = MM.mix(col, P.tip, 0.5);
    col = MM.mul(col, 1 + 0.12 * (MM.fbm(p, 0.6, 3, 5) - 0.5));
    // ragged lower edge on the sides and back: alternating one-texel tufts in the last row,
    // a sparser cut in the row above
    if (o.face !== 'up' && o.face !== 'down' && o.face !== 'front') {
      var fromBottom = p.y - lo.y;
      if (fromBottom < 1.0 && (o.u + Math.floor(MM.h(o.u, 0, 0, 41) * 2)) % 2 === 0) return [0, 0, 0, 0];
      if (fromBottom < 2.0 && fromBottom >= 1.0 && MM.h(o.u, o.v, 0, 43) < 0.25) return [0, 0, 0, 0];
    }
    return MM.cl(col);
  });

  // composite: the shipped PNG everywhere, the new mane inside the mane box's face rects only
  var out = document.createElement('canvas'); out.width = 128; out.height = 64;
  var cx = out.getContext('2d');
  cx.drawImage(orig, 0, 0);
  var img = cx.getImageData(0, 0, 128, 64), pm = cv.getContext('2d').getImageData(0, 0, 128, 64).data;
  var rects = MM.faceRects(['neck_mane']), n = 0;
  rects.forEach(function (R) {
    for (var y = R[1]; y < R[3]; y++) for (var x = R[0]; x < R[2]; x++) {
      var i = (y * 128 + x) * 4;
      img.data[i] = pm[i]; img.data[i + 1] = pm[i + 1]; img.data[i + 2] = pm[i + 2]; img.data[i + 3] = pm[i + 3];
      n++;
    }
  });
  cx.putImageData(img, 0, 0);
  MM.show(out);
  MM.canvas = out;
  MM.save(window.OUT + 'skins/' + NAME + '.png', out);
  return NAME + ' mane painted: ' + n + ' texels in ' + rects.length + ' rects';
})();
