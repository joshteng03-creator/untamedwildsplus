(function () {
  var D = {};

  // ModelDeer hides the family a species does not use with showModel (now driven by the
  // `palmate` JSON flag, not variant indices), so BOTH antler families are painted on
  // every skin -- red_deer.png paints all 20 antler cubes solid. Nothing here is alpha-0'd
  // except the eyes' inward quad.
  D.PALM = ['palm_beam_left', 'palm_beam_right', 'palm_brow_left', 'palm_brow_right',
            'palm_lower_left', 'palm_lower_right', 'palm_upper_left', 'palm_upper_right',
            'palm_digA_left', 'palm_digA_right', 'palm_digB_left', 'palm_digB_right',
            'palm_digC_left', 'palm_digC_right'];
  D.BEAM = ['beam_low_left', 'beam_low_right', 'beam_up_left', 'beam_up_right',
            'br_brow_left', 'br_brow_right', 'br_trez_left', 'br_trez_right',
            'br_crownA_left', 'br_crownA_right', 'br_crownB_left', 'br_crownB_right'];
  D.PEDICLE = ['pedicle_left', 'pedicle_right'];
  D.EYE = ['eye_left', 'eye_right'];
  D.HEAD = ['head_skull', 'head_muzzle', 'ear_left', 'ear_right'];
  D.NECK = ['neck_lower', 'neck_upper', 'throat_bell'];
  D.BODY = ['body_barrel', 'body_chest', 'body_croup', 'body_withers'];
  D.TAIL = ['tail_dock', 'tail_tip'];
  D.LIMB = ['fore_left_shoulder', 'fore_left_forearm', 'fore_left_cannon', 'fore_left_hoof',
            'fore_right_shoulder', 'fore_right_forearm', 'fore_right_cannon', 'fore_right_hoof',
            'hind_left_thigh', 'hind_left_gaskin', 'hind_left_cannon', 'hind_left_hoof',
            'hind_right_thigh', 'hind_right_gaskin', 'hind_right_cannon', 'hind_right_hoof'];
  D.HOOF = ['fore_left_hoof', 'fore_right_hoof', 'hind_left_hoof', 'hind_right_hoof'];

  D.has = function (a, n) { return a.indexOf(n) >= 0; };
  D.cleared = function () { return D.EYE; };

  D.Y0 = 0; D.Y1 = 59.87;
  D.ZNOSE = -36.45; D.ZTAIL = 22.87;

  D.paint = function (S) {
    var back = MM.hex(S.back), flank = MM.hex(S.flank), belly = MM.hex(S.belly);
    var antler = MM.hex(S.antler), antlerTip = MM.hex(S.antlerTip || S.antler);
    var hoof = MM.hex(S.hoof), muzzle = MM.hex(S.muzzle);
    var pale = MM.hex(S.paleCol || '#e8e2d2');

    return MM.paint(function (o) {
      var n = o.part, p = o.wp;
      if (D.has(D.EYE, n)) return [0, 0, 0, 0];

      // ---- antlers: velvet-free bone, dark at the pedicle, polished pale at the tips ----
      if (D.has(D.PALM, n) || D.has(D.BEAM, n) || D.has(D.PEDICLE, n)) {
        // distance out from the skull midline is the base->tip axis for a rack; using
        // height instead bands a palmate sheet the wrong way
        var out = MM.smooth(2.0, S.rackSpan || 20.0, Math.abs(p.x));
        var col2 = MM.mix(antler, antlerTip, out);
        col2 = MM.mul(col2, 0.93 + 0.14 * MM.fbm(p, 1.3, 3, 17));
        // pearling: the knobbly texture round the burr, strongest low
        var pearl = (MM.dith(o.u, o.v, 9) - 0.5) * (1 - out);
        return MM.cl(MM.mul(col2, 1 + 0.16 * pearl));
      }

      var col;
      var limb = D.has(D.LIMB, n);

      if (limb) {
        var m = MM.map[n], seg = (p.y - m.lo.y) / Math.max(0.001, m.hi.y - m.lo.y);
        col = MM.mix(MM.mul(flank, 0.94), flank, seg);
        var down = 1 - MM.smooth(1.0, 20.0, p.y);
        col = MM.mix(col, MM.hex(S.legCol), S.legAmt * down);
      } else {
        var t = MM.smooth(S.shadeLo, S.shadeHi, (p.y - D.Y0) / (D.Y1 - D.Y0));
        col = MM.mix(belly, flank, MM.smooth(0.0, 0.55, t));
        col = MM.mix(col, back, MM.smooth(0.55, 1.0, t));
      }

      // pale neck cape -- the caribou's most recognisable mark, and the wapiti's in reverse
      if (S.capeAmt && (D.has(D.NECK, n) || n === 'body_chest')) {
        var cp = MM.smooth(S.capeZ1 || -2.0, S.capeZ0 || -18.0, p.z);
        cp *= 0.75 + 0.5 * MM.fbm(p, 0.9, 2, 23);
        col = MM.mix(col, pale, S.capeAmt * Math.max(0, Math.min(1, cp)));
      }

      // Neck mane. ModelDeer deliberately has NO mane box -- a mane cube was the equid's
      // worst z-fight source -- so on this rig a mane is PAINTED, as the wapiti and red
      // deer already do it. Vertical streaks, heaviest on the lower neck and throat.
      if (S.maneAmt && (D.has(D.NECK, n) || n === 'body_chest')) {
        var mstr = MM.fbm({ x: p.x * 2.6, y: p.y * 0.30, z: p.z * 2.6 }, 1.0, 3, 41);
        var mm = MM.smooth(0.40, 0.72, mstr)
                 * MM.smooth(-2.0, -12.0, p.z) * (1 - MM.smooth(28.0, 36.0, p.y));
        col = MM.mix(col, MM.hex(S.maneCol), S.maneAmt * Math.max(0, Math.min(1, mm)));
      }

      // dark face mask over the bridge of the nose, with a pale eye surround
      if (S.maskAmt && D.has(D.HEAD, n)) {
        var mk = MM.smooth(-24.0, -32.0, p.z) * MM.smooth(0.35, 0.75,
                 1 - Math.abs(p.x) / 3.4);
        col = MM.mix(col, MM.hex(S.maskCol), S.maskAmt * Math.max(0, Math.min(1, mk)));
      }

      // white throat patch / bib
      if (S.bibAmt && (D.has(D.NECK, n) || D.has(D.HEAD, n))) {
        var bb = (1 - MM.smooth(26.0, 32.0, p.y)) * MM.smooth(-14.0, -24.0, p.z);
        col = MM.mix(col, pale, S.bibAmt * Math.max(0, Math.min(1, bb)));
      }

      // white belly and inner leg
      if (S.bellyAmt) {
        var bl = 1 - MM.smooth(S.bellyY0 || 14.0, S.bellyY1 || 19.0, p.y);
        // On a limb the belly rule must ALSO be gated on height: the belly tone belongs to
        // the upper inner thigh, and without this every deer gets white legs to the hoof --
        // the same "a leg sits low in world Y" trap that gave the canids white socks.
        if (limb) {
          bl *= MM.smooth(0.0, 0.45, 1 - Math.abs(p.x) / 7.0)
                * MM.smooth(S.bellyLegY0 || 8.0, S.bellyLegY1 || 15.0, p.y);
        }
        col = MM.mix(col, pale, S.bellyAmt * Math.max(0, Math.min(1, bl)));
      }

      // pale socks above the hoof (caribou, and most deer show some of this)
      if (S.sockAmt && limb) {
        var sk = 1 - MM.smooth(S.sockTop || 4.0, (S.sockTop || 4.0) + 3.5, p.y);
        sk *= 0.85 + 0.3 * MM.dith(o.u, o.v, 11);
        col = MM.mix(col, pale, S.sockAmt * Math.max(0, Math.min(1, sk)));
      }

      // white rump / tail flag, the white-tailed deer's signature
      if (S.rumpAmt && !limb) {
        var rr = MM.smooth(14.0, 20.0, p.z) * MM.smooth(17.0, 22.0, p.y);
        col = MM.mix(col, pale, S.rumpAmt * Math.max(0, Math.min(1, rr)));
      }
      if (S.rumpAmt && D.has(D.TAIL, n)) col = MM.mix(col, pale, 0.55 * S.rumpAmt);

      // dorsal spot rows: fallow and sika carry discrete round spots along the flank, NOT
      // thresholded noise -- Worley cells give separated dots where a noise threshold
      // gives smears. Damped on the limbs, where the faces sit at an angle to the rows.
      if (S.spotAmt && !D.has(D.HEAD, n)) {
        var sp = S.spotSpacing || 5.0;
        var cx = Math.round(p.z / sp), cy = Math.round(p.y / sp);
        var best = 9;
        for (var a = -1; a <= 1; a++) for (var b = -1; b <= 1; b++) {
          var jz = (cx + a + MM.h(cx + a, cy + b, 3.1, 4) * 0.7 - 0.35) * sp;
          var jy = (cy + b + MM.h(cx + a, cy + b, 7.7, 5) * 0.7 - 0.35) * sp;
          var dd = Math.sqrt((p.z - jz) * (p.z - jz) + (p.y - jy) * (p.y - jy));
          if (dd < best) best = dd;
        }
        var dot = 1 - MM.smooth(sp * 0.16, sp * 0.30, best);
        // spots fade out on the belly and off the rump, as they do on a real fawn coat
        dot *= MM.smooth(15.0, 20.0, p.y) * (1 - MM.smooth(12.0, 20.0, p.z));
        if (limb) dot *= 0.25;
        col = MM.mix(col, pale, S.spotAmt * Math.max(0, Math.min(1, dot)));
      }

      // dark dorsal eel stripe (sika, and the fallow's spine line)
      if (S.dorsalAmt && !limb) {
        var ds = MM.smooth(0.60, 0.85, (p.y - D.Y0) / (D.Y1 - D.Y0))
                 * (1 - MM.smooth(2.5, 5.5, Math.abs(p.x)));
        col = MM.mix(col, MM.hex(S.dorsalCol), S.dorsalAmt * Math.max(0, Math.min(1, ds)));
      }

      if (D.has(D.HEAD, n) && S.muzzleAmt) {
        var fr = MM.smooth(-27.0, -34.0, p.z);
        col = MM.mix(col, muzzle, S.muzzleAmt * fr);
      }
      if (D.has(D.HOOF, n)) {
        col = MM.mix(col, hoof, 0.8 * (1 - MM.smooth(0.0, 2.6, p.y)));
      }

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

  // deer eyes are the 9-painted / 9-alpha-0 pattern (the inward quad is cleared so the
  // zero-width plane cannot z-fight itself) -- stamped verbatim from a shipped skin.
  D.stampEyes = function (c, refPath) {
    var ref = MM.load(refPath);
    var rd = ref.getContext('2d').getImageData(0, 0, ref.width, ref.height);
    var cx = c.getContext('2d');
    var img = cx.getImageData(0, 0, c.width, c.height);
    var n = 0;
    D.EYE.forEach(function (name) {
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

  window.DEER = D;
  return 'deerlib ready';
})()
