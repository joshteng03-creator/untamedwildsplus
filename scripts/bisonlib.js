(function () {
  var B = {};

  B.BODY = ['body_main', 'body_torso', 'body_hair'];
  B.HEAD = ['head_neck', 'head_main', 'head_hair', 'head_beard', 'head_ear_left', 'head_ear_right'];
  B.HORN = ['head_horn_left', 'head_horn_right'];
  B.EYE = ['eye_left', 'eye_right'];
  B.LIMB = ['arm_left_1', 'arm_left_2', 'arm_left_fur', 'arm_right_1', 'arm_right_2',
            'arm_right_fur', 'leg_left_thigh', 'leg_left_calf', 'leg_right_thigh',
            'leg_right_calf'];
  B.TAIL = ['tail'];

  B.has = function (arr, n) { return arr.indexOf(n) >= 0; };

  // Every part this species deliberately alpha-0s. MM.bleed MUST be given all of these
  // as `protect`, not just the eyes: bleed dilates paint into transparent neighbours, so
  // a "deleted" part gets its edges refilled and renders as a sliver of geometry. That is
  // how a forelock the aurochs trick had removed came back on the first three skins.
  B.cleared = function (S) {
    return B.EYE.concat(S.noForelock ? ['head_hair'] : []);
  };

  B.Y0 = 0; B.Y1 = 22.53;
  B.ZNOSE = -16.46; B.ZTAIL = 18;

  B.paint = function (S) {
    var back = MM.hex(S.back), flank = MM.hex(S.flank), belly = MM.hex(S.belly);
    var muzzle = MM.hex(S.muzzle), horn = MM.hex(S.horn), hoof = MM.hex(S.hoof);
    var legcol = MM.hex(S.legCol), sockcol = MM.hex(S.sockCol || '#ffffff');
    var mudcol = MM.hex(S.mudCol || '#6b5a3e');

    return MM.paint(function (o) {
      var n = o.part, p = o.wp;

      if (B.has(B.EYE, n)) return [0, 0, 0, 0];
      if (S.noForelock && n === 'head_hair') return [0, 0, 0, 0];

      if (B.has(B.HORN, n)) {
        // Outward along the sweep, not up: these horns run in X, so |x| is the
        // base-to-tip axis and a y-based gradient would band them the wrong way.
        var tip = MM.smooth(1.6, 6.3, Math.abs(p.x));
        var hc = MM.mix(horn, MM.hex(S.hornTip || S.horn), tip);
        hc = MM.mul(hc, 0.94 + 0.12 * MM.fbm(p, 1.4, 2, 11));
        // transverse growth ridges: the wild buffalo's horn is corrugated along its
        // whole length, and at ~1px/unit the ridge period has to be measured, not guessed
        var rid = S.hornRidge || 0.06;
        var band = rid * Math.sin(Math.abs(p.x) * (S.hornRidgeFreq || 2.4));
        return MM.cl(MM.mul(hc, 1 + band * (1 - 0.4 * tip)));
      }

      var col;
      var limb = B.has(B.LIMB, n);

      if (limb) {
        // A leg sits LOW in world Y, so the body's height rule would paint it with the
        // pale belly tone and give the animal white socks. Own flank-toned base, then one
        // wash graded down the WHOLE leg column in absolute Y so it stays continuous
        // across thigh / calf / hoof instead of restarting per segment.
        var m = MM.map[n], seg = (p.y - m.lo.y) / Math.max(0.001, m.hi.y - m.lo.y);
        col = MM.mix(MM.mul(flank, 0.93), flank, seg);
        var down = 1 - MM.smooth(0.5, 12.0, p.y);
        col = MM.mix(col, legcol, S.legAmt * down);
        col = MM.mix(col, hoof, 0.75 * (1 - MM.smooth(0.0, 1.6, p.y)));
      } else {
        var t = MM.smooth(S.shadeLo, S.shadeHi, (p.y - B.Y0) / (B.Y1 - B.Y0));
        col = MM.mix(belly, flank, MM.smooth(0.0, 0.55, t));
        col = MM.mix(col, back, MM.smooth(0.55, 1.0, t));
      }

      if (B.has(B.HEAD, n)) {
        // Frontness alone paints the whole face -- head_main runs z -16.5..-8.3, so more
        // than half of it counts as "front" and the animal ends up wearing a pale mask.
        // Gate it on the LOWER face as well: a muzzle is the nose and lips, not the brow.
        var fr = MM.smooth(-11.5, -15.8, p.z) * (1 - MM.smooth(9.5, 13.5, p.y));
        col = MM.mix(col, muzzle, S.muzzleAmt * fr);
        // keratin boss: the fused horn bases that armour an African buffalo's forehead.
        // Painted on the head, not the horns -- the horn cubes start outboard of it.
        if (S.bossAmt) {
          var bs = MM.smooth(13.5, 16.0, p.y) * MM.smooth(-8.0, -13.0, p.z);
          bs *= 0.85 + 0.3 * MM.fbm(p, 1.6, 2, 31);
          col = MM.mix(col, MM.hex(S.bossCol || '#b3a686'),
                       S.bossAmt * Math.max(0, Math.min(1, bs)));
        }
        if (S.crown) {
          var cr = MM.smooth(15.0, 17.2, p.y) * MM.smooth(-6.0, -12.0, p.z);
          col = MM.mix(col, MM.hex(S.crown), S.crownAmt * cr);
        }
      }

      // white stockings: banteng's diagnostic, and they must start at a hard-ish but
      // dithered line or they read as a painted band
      if (S.sockAmt && limb) {
        var s = MM.smooth(S.sockTop + 1.2, S.sockTop - 1.2, p.y);
        s *= 0.85 + 0.3 * MM.dith(o.u, o.v, 5);
        col = MM.mix(col, sockcol, S.sockAmt * Math.max(0, Math.min(1, s)));
      }

      // pale throat/chest chevron -- the wild Asian buffalo's other diagnostic besides
      // the stockings. Placed on the underside of the neck and the front of the brisket,
      // and broken with noise so it reads as worn hair rather than a painted bib.
      if (S.bibAmt && !limb) {
        var bz = MM.smooth(-4.0, -11.0, p.z);
        var by = 1 - MM.smooth(9.5, 13.5, p.y);
        var bib = bz * by * (0.6 + 0.7 * MM.fbm(p, 1.1, 2, 21));
        col = MM.mix(col, MM.hex(S.bibCol || '#c9c6bb'),
                     S.bibAmt * Math.max(0, Math.min(1, bib)));
      }

      // White rump patch. Kept OFF the limbs and to the rearmost slice of the barrel:
      // running it from z 9 and letting it onto the thighs turned the entire
      // hindquarters white, which reads as a nappy, not as a banteng.
      if (S.rumpAmt && !limb) {
        var rr = MM.smooth(13.0, 16.5, p.z)
                 * MM.smooth(8.0, 11.0, p.y) * (1 - MM.smooth(15.5, 18.5, p.y));
        rr *= 0.75 + 0.5 * MM.fbm(p, 1.2, 2, 3);
        col = MM.mix(col, sockcol, S.rumpAmt * Math.max(0, Math.min(1, rr)));
      }

      // dried mud, heaviest low and offset per column so the tide line is not a
      // straight horizontal band; a dorsal term too, since a wallowing bovid throws
      // mud over its own back
      if (S.mudAmt) {
        var wob = 1.6 * (MM.fbm(p, 0.5, 2, 9) - 0.5);
        var mlo = (S.mudLo === undefined ? 4.0 : S.mudLo);
        var tide = 1 - MM.smooth(mlo + wob, mlo + 6.5 + wob, p.y);
        var dors = MM.smooth(0.72, 1.0, (p.y - B.Y0) / (B.Y1 - B.Y0)) * 0.55;
        var mk = Math.max(tide, dors) * (0.55 + 0.9 * MM.fbm(p, 0.85, 3, 4));
        col = MM.mix(col, mudcol, S.mudAmt * Math.max(0, Math.min(1, mk)));
      }

      // sparse hide: worn bare patches, not speckle
      if (S.bareAmt) {
        var bp = MM.smooth(0.58, 0.86, MM.fbm(p, S.bareScale || 0.55, 3, 7));
        col = MM.mix(col, MM.hex(S.bareCol), S.bareAmt * bp);
      }

      // wrinkle folds across the barrel and neck
      if (S.foldAmt && !limb) {
        var ph = p.z * 0.55 + p.y * 0.22 + 1.4 * MM.fbm(p, 0.4, 2, 13);
        var fl = Math.sin(ph);
        col = MM.mul(col, 1 + S.foldAmt * fl * (0.5 + 0.5 * MM.smooth(0.2, 0.8,
                      (p.y - B.Y0) / (B.Y1 - B.Y0))));
      }

      // coat texture. Kept low-contrast: +-0.26 luminance reads as TV static, ~+-0.13
      // with mild clumping reads as coarse hair.
      var hair = MM.fbm(p, S.hairScale, 3, 1) - 0.5;
      col = MM.mul(col, 1 + S.hairAmt * hair);
      if (S.grizzleAmt) {
        var gz = MM.dith(o.u, o.v, 2) - 0.5;
        var cl = MM.smooth(0.35, 0.75, MM.fbm(p, 1.9, 2, 6));
        col = MM.mul(col, 1 + S.grizzleAmt * gz * (0.35 + 0.65 * cl));
      }

      return MM.cl(col);
    });
  };

  // The eye art is standardised per type and gets STAMPED, not reasoned about: the bison
  // paints the outward quad and leaves the inward one alpha-0, which is what stops the
  // zero-width plane z-fighting against itself.
  B.stampEyes = function (c, refPath) {
    var ref = MM.load(refPath);
    var rd = ref.getContext('2d').getImageData(0, 0, ref.width, ref.height);
    var cx = c.getContext('2d');
    var img = cx.getImageData(0, 0, c.width, c.height);
    var n = 0;
    B.EYE.forEach(function (name) {
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

  window.BISON = B;
  return 'bisonlib ready';
})()
