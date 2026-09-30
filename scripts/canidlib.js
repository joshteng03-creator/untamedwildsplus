(function () {
  var C = {};

  C.EYE = ['eye_left', 'eye_right'];
  C.NOSE = ['nose'];
  C.HEAD = ['head_main', 'head_face', 'head_snout', 'head_jaw', 'cheek_left', 'cheek_right',
            'ear_left', 'ear_right', 'ear_left_tip', 'ear_right_tip'];
  C.EARTIP = ['ear_left_tip', 'ear_right_tip', 'ear_left', 'ear_right'];
  C.RUFF = ['neck_ruff', 'hair'];
  C.NECK = ['head_neck'];
  C.BODY = ['body_main', 'body_chest', 'body_loin', 'body_croup'];
  C.TAIL = ['tail_1', 'tail_2', 'tail_3'];
  C.LIMB = ['arm_left_upper', 'arm_left_lower', 'arm_left_foot', 'arm_left_paw',
            'arm_right_upper', 'arm_right_lower', 'arm_right_foot', 'arm_right_paw',
            'leg_left_upper', 'leg_left_lower', 'leg_left_foot', 'leg_left_paw',
            'leg_right_upper', 'leg_right_lower', 'leg_right_foot', 'leg_right_paw'];

  C.has = function (a, n) { return a.indexOf(n) >= 0; };
  C.cleared = function () { return C.EYE; };

  C.Y0 = 0; C.Y1 = 19.96;
  C.ZNOSE = -18.74; C.ZTAIL = 17.33;

  C.paint = function (S) {
    var back = MM.hex(S.back), flank = MM.hex(S.flank), belly = MM.hex(S.belly);
    var mask = MM.hex(S.maskCol || S.back), pale = MM.hex(S.paleCol || '#e6ddca');
    var legcol = MM.hex(S.legCol), tailtip = MM.hex(S.tailTipCol || S.back);

    return MM.paint(function (o) {
      var n = o.part, p = o.wp;
      if (C.has(C.EYE, n)) return [0, 0, 0, 0];
      if (C.has(C.NOSE, n)) {
        return MM.cl(MM.mul(MM.hex(S.noseCol || '#231d1a'),
                            0.92 + 0.16 * MM.fbm(p, 3.0, 2, 5)));
      }

      var col;
      var limb = C.has(C.LIMB, n);

      if (limb) {
        // own flank-toned base shaded by the segment, then ONE wash down the whole leg
        // column in absolute Y -- restarting it per segment is what gives white socks
        var m = MM.map[n], seg = (p.y - m.lo.y) / Math.max(0.001, m.hi.y - m.lo.y);
        col = MM.mix(MM.mul(flank, 0.94), flank, seg);
        var down = 1 - MM.smooth(0.5, 11.0, p.y);
        col = MM.mix(col, legcol, S.legAmt * down);
      } else {
        var t = MM.smooth(S.shadeLo, S.shadeHi, (p.y - C.Y0) / (C.Y1 - C.Y0));
        col = MM.mix(belly, flank, MM.smooth(0.0, 0.55, t));
        col = MM.mix(col, back, MM.smooth(0.55, 1.0, t));
      }

      // dark dorsal saddle -- a grey wolf's cape, and the strongest shape cue on a canid
      if (S.saddleAmt && !limb) {
        var sd = MM.smooth(S.saddleLo || 0.62, S.saddleHi || 0.86,
                           (p.y - C.Y0) / (C.Y1 - C.Y0));
        sd *= 1 - MM.smooth(-6.0, -13.0, p.z);
        sd *= 0.75 + 0.5 * MM.fbm(p, 0.9, 3, 19);
        col = MM.mix(col, MM.hex(S.saddleCol), S.saddleAmt * Math.max(0, Math.min(1, sd)));
      }

      // ruff / mane
      if (S.ruffAmt && C.has(C.RUFF, n)) {
        var rf = MM.fbm({ x: p.x * 2.4, y: p.y * 0.35, z: p.z * 2.4 }, 1.0, 3, 27);
        col = MM.mix(col, MM.hex(S.ruffCol), S.ruffAmt * MM.smooth(0.3, 0.75, rf));
      }

      // facial mask: dark over the muzzle bridge, pale round the lips and throat
      if (C.has(C.HEAD, n)) {
        var fm = MM.smooth(-12.0, -17.0, p.z) * MM.smooth(0.30, 0.80,
                 1 - Math.abs(p.x) / 2.2);
        col = MM.mix(col, mask, S.maskAmt * Math.max(0, Math.min(1, fm)));
        var lip = MM.smooth(-13.0, -17.5, p.z) * (1 - MM.smooth(12.0, 14.2, p.y));
        col = MM.mix(col, pale, S.lipAmt * Math.max(0, Math.min(1, lip)));
      }

      // dark ear backs / tips, a real canid cue and cheap to read at distance
      if (S.earAmt && C.has(C.EARTIP, n)) {
        var et = MM.smooth(15.0, 18.5, p.y);
        col = MM.mix(col, MM.hex(S.earCol), S.earAmt * et);
      }

      // pale throat blaze and chest
      if (S.throatAmt && (C.has(C.NECK, n) || n === 'body_chest' || C.has(C.HEAD, n))) {
        var th = (1 - MM.smooth(11.0, 14.0, p.y)) * MM.smooth(-2.0, -11.0, p.z);
        col = MM.mix(col, pale, S.throatAmt * Math.max(0, Math.min(1, th)));
      }

      // tail: graded to the tip, which is where a canid's marking lives
      if (C.has(C.TAIL, n)) {
        var tt = MM.smooth(8.0, 16.5, p.z);
        col = MM.mix(col, tailtip, S.tailTipAmt * tt);
      }

      // agouti: banded hairs. This is what makes a wolf coat read as fur rather than
      // paint -- clumped, low contrast, and it must NOT become a per-texel checkerboard.
      var hair = MM.fbm(p, S.hairScale, 3, 1) - 0.5;
      col = MM.mul(col, 1 + S.hairAmt * hair);
      if (S.agoutiAmt) {
        var ag = MM.dith(o.u, o.v, 2) - 0.5;
        var cl = MM.smooth(0.35, 0.75, MM.fbm(p, 2.1, 2, 6));
        col = MM.mul(col, 1 + S.agoutiAmt * ag * (0.3 + 0.7 * cl));
      }
      return MM.cl(col);
    });
  };

  // dire_wolf eye art is standardised: amber iris over a black pupil on the OUTWARD half,
  // inward half alpha-0 so the zero-width plane cannot z-fight itself. Stamped verbatim.
  C.stampEyes = function (c, refPath) {
    var ref = MM.load(refPath);
    var rd = ref.getContext('2d').getImageData(0, 0, ref.width, ref.height);
    var cx = c.getContext('2d');
    var img = cx.getImageData(0, 0, c.width, c.height);
    var n = 0;
    C.EYE.forEach(function (name) {
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

  window.CANID = C;
  return 'canidlib ready';
})()
