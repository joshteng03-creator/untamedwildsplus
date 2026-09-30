(function () {
  var B = {};

  // Conventions read off the shipped PNGs, not invented:
  //   shape15  = the bristly dorsal crest. Opaque on eurasian + warthogs, CLEARED on
  //              peccary and red_river -- so a crestless suid deletes it by alpha.
  //   tusk_*   = 2/4 opaque (ivory) on eurasian/red_river, CLEARED on both warthogs and
  //              the peccaries. Mirrored L/R share one rect.
  //   shape14  = the tail, only 11/42 opaque (22/42 on warthogs) -- a partial rect, so it
  //              is STAMPED from a reference rather than reasoned about.
  B.CREST = 'shape15';
  B.TAIL = 'shape14';
  B.TUSK = ['tusk_left', 'tusk_left_1'];
  B.EYE = ['eye_left', 'eye_right'];
  B.HEAD = ['head_main', 'head_snout', 'head_mouth', 'ear_left', 'ear_right'];
  B.LIMB = ['arm_left_1', 'arm_left_2', 'arm_right_1', 'arm_right_2',
            'leg_left_1', 'leg_left_2', 'leg_right_1', 'leg_right_2'];
  B.BODY = ['main_body'];

  B.has = function (a, n) { return a.indexOf(n) >= 0; };
  B.cleared = function (S) {
    return B.EYE.concat(B.TUSK).concat([B.TAIL]).concat(S.hasCrest ? [] : [B.CREST]);
  };

  B.Y0 = -0.09; B.Y1 = 15.88;
  B.ZNOSE = -15.04; B.ZTAIL = 7.17;

  B.paint = function (S) {
    var back = MM.hex(S.back), flank = MM.hex(S.flank), belly = MM.hex(S.belly);
    var crest = MM.hex(S.crestCol || S.back), snout = MM.hex(S.snout);
    var legcol = MM.hex(S.legCol), pale = MM.hex(S.paleCol || '#e0d6c2');

    return MM.paint(function (o) {
      var n = o.part, p = o.wp;
      if (B.has(B.EYE, n) || B.has(B.TUSK, n) || n === B.TAIL) return [0, 0, 0, 0];
      if (n === B.CREST && !S.hasCrest) return [0, 0, 0, 0];

      var col;
      var limb = B.has(B.LIMB, n);

      if (n === B.CREST) {
        // coarse bristle: high-frequency streaks along the spine, not smooth shading
        var br = MM.fbm({ x: p.x * 3.0, y: p.y * 0.5, z: p.z * 3.0 }, 1.0, 3, 51);
        col = MM.mix(MM.mul(crest, 0.8), crest, MM.smooth(0.35, 0.7, br));
      } else if (limb) {
        var m = MM.map[n], seg = (p.y - m.lo.y) / Math.max(0.001, m.hi.y - m.lo.y);
        col = MM.mix(MM.mul(flank, 0.92), flank, seg);
        var down = 1 - MM.smooth(0.5, 9.0, p.y);
        col = MM.mix(col, legcol, S.legAmt * down);
      } else {
        var t = MM.smooth(S.shadeLo, S.shadeHi, (p.y - B.Y0) / (B.Y1 - B.Y0));
        col = MM.mix(belly, flank, MM.smooth(0.0, 0.55, t));
        col = MM.mix(col, back, MM.smooth(0.55, 1.0, t));
      }

      // the long mobile snout disc, always a different tone from the face
      if (B.has(B.HEAD, n)) {
        var fr = MM.smooth(-10.5, -14.5, p.z);
        col = MM.mix(col, snout, S.snoutAmt * fr);
      }

      // facial warts / a pale cheek whisker streak, the hog-specific marks
      if (S.cheekAmt && B.has(B.HEAD, n)) {
        var ch = MM.smooth(-8.0, -12.5, p.z) * MM.smooth(0.45, 0.85,
                 Math.abs(p.x) / 2.6) * (1 - MM.smooth(9.5, 12.5, p.y));
        col = MM.mix(col, pale, S.cheekAmt * Math.max(0, Math.min(1, ch)));
      }

      // pale forehead shield, centred and high on the skull. Kubanochoerus carried a
      // bony horn there that this rig has no geometry for, so the marking has to carry it
      // -- the same job elasmotherium's painted frontal dome does for its missing horn.
      if (S.bossAmt && B.has(B.HEAD, n)) {
        var bs = MM.smooth(10.5, 13.5, p.y) * MM.smooth(-7.5, -12.0, p.z)
                 * MM.smooth(0.30, 0.80, 1 - Math.abs(p.x) / 2.8);
        bs *= 0.85 + 0.3 * MM.fbm(p, 1.8, 2, 31);
        col = MM.mix(col, MM.hex(S.bossCol || '#cbb98f'),
                     S.bossAmt * Math.max(0, Math.min(1, bs)));
      }

      // wrinkle folds across the shoulder and face
      if (S.foldAmt && !limb) {
        var ph = p.z * 0.85 + 1.3 * MM.fbm(p, 0.5, 2, 13);
        col = MM.mul(col, 1 + S.foldAmt * Math.sin(ph));
      }

      // sparse hide with worn bare patches (babirusa is nearly naked)
      if (S.bareAmt) {
        var bp = MM.smooth(0.55, 0.85, MM.fbm(p, S.bareScale || 1.4, 3, 7));
        col = MM.mix(col, MM.hex(S.bareCol), S.bareAmt * bp);
      }

      // shaggy coat: coarse and high-contrast on a forest hog, absent on a babirusa
      var hair = MM.fbm(p, S.hairScale, 3, 1) - 0.5;
      col = MM.mul(col, 1 + S.hairAmt * hair);
      if (S.grizzleAmt) {
        var gz = MM.dith(o.u, o.v, 2) - 0.5;
        var cl = MM.smooth(0.35, 0.75, MM.fbm(p, 1.9, 2, 6));
        col = MM.mul(col, 1 + S.grizzleAmt * gz * (0.35 + 0.65 * cl));
      }
      if (S.mudAmt) {
        var mk = (1 - MM.smooth(2.0, 8.0, p.y)) * (0.5 + 0.9 * MM.fbm(p, 0.9, 3, 4));
        col = MM.mix(col, MM.hex(S.mudCol), S.mudAmt * Math.max(0, Math.min(1, mk)));
      }
      return MM.cl(col);
    });
  };

  // eyes, ivory tusks and the partial tail rect are all standardised -- stamp them.
  B.stamp = function (c, refPath, S) {
    var ref = MM.load(refPath);
    var rd = ref.getContext('2d').getImageData(0, 0, ref.width, ref.height);
    var cx = c.getContext('2d');
    var img = cx.getImageData(0, 0, c.width, c.height);
    var names = B.EYE.concat([B.TAIL]).concat(S.hasTusks ? B.TUSK : []);
    var n = 0;
    names.forEach(function (name) {
      var F = MM.map[name].faces;
      for (var k in F) {
        var f = F[k];
        for (var y = f.v0; y < f.v1; y++) for (var x = f.u0; x < f.u1; x++) {
          var i = (y * c.width + x) * 4;
          img.data[i] = rd.data[i]; img.data[i + 1] = rd.data[i + 1];
          img.data[i + 2] = rd.data[i + 2]; img.data[i + 3] = rd.data[i + 3];
          n++;
        }
      }
    });
    cx.putImageData(img, 0, 0);
    return n;
  };

  window.BOAR = B;
  return 'boarlib ready';
})()
