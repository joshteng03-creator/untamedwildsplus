(function () {
  var C = {};

  // The camel rig carries EVERY camelid body plan at once and the SKIN chooses which,
  // by alpha-0'ing the cubes that species does not have (CLAUDE.md lesson 12). Read off
  // the shipped PNGs, not invented:
  //   bactrian      hump_front + hump_back opaque, hump_centre CLEAR, tail CLEAR, wool on
  //   dromedary     hump_centre opaque, front+back CLEAR, tail CLEAR, wool off
  //   guanaco/vicuna/paleolama   all three humps CLEAR, tail OPAQUE, wool off
  //   western/titanotylopus      all three humps opaque (one long dorsal ridge)
  // So "no hump" and "llama tail" need no Java at all.
  C.HUMP_FRONT = 'body_hump_front';
  C.HUMP_BACK = 'body_hump_back';
  C.HUMP_CENTRE = 'body_hump_centre';
  C.HUMPS = [C.HUMP_FRONT, C.HUMP_BACK, C.HUMP_CENTRE];
  // Every limb skirt and both neck skirts SHARE one rect set, so they cannot differ in
  // colour -- two earlier attempts at a dark neck cape over body-toned limb wool failed
  // exactly here. Treat the whole wool overlay as ONE surface.
  C.WOOL = ['arm_left_hair', 'arm_right_hair', 'leg_left_hair', 'leg_right_hair',
            'neck_hair_1', 'neck_hair_2', 'head_hair'];
  C.EYE = ['eye_left', 'eye_right'];
  C.HEAD = ['head_main', 'head_nose', 'head_nose_1', 'ear_left', 'ear_right'];
  C.NECK = ['neck_1', 'neck_2'];
  C.LIMB = ['arm_left_1', 'arm_left_2', 'arm_right_1', 'arm_right_2',
            'leg_left_thigh', 'leg_left_calf', 'leg_left_calf_1',
            'leg_right_thigh', 'leg_right_calf', 'leg_right_calf_1'];
  C.TAIL = ['tail'];

  C.has = function (a, n) { return a.indexOf(n) >= 0; };

  // Everything this species alpha-0s, which MM.bleed must be told to leave alone. Bleed
  // refills a cleared part's edges otherwise, and on this rig that renders as a slab of
  // leftover hump hanging in the gap between the two real ones.
  C.cleared = function (S) {
    var out = C.EYE.slice();
    C.HUMPS.forEach(function (h) { if (S.humps.indexOf(h) < 0) out.push(h); });
    if (!S.hasTail) out.push('tail');
    if (!S.hasWool) out = out.concat(C.WOOL);
    return out;
  };

  C.Y0 = -0.11; C.Y1 = 38.44;
  C.ZNOSE = -27.59; C.ZTAIL = 18.43;

  C.paint = function (S) {
    var back = MM.hex(S.back), flank = MM.hex(S.flank), belly = MM.hex(S.belly);
    var muzzle = MM.hex(S.muzzle), legcol = MM.hex(S.legCol), hoof = MM.hex(S.hoof);
    var woolcol = MM.hex(S.woolCol || S.flank);
    var patchcol = MM.hex(S.patchCol || '#ffffff');

    return MM.paint(function (o) {
      var n = o.part, p = o.wp;

      if (C.has(C.EYE, n)) return [0, 0, 0, 0];
      if (C.HUMPS.indexOf(n) >= 0 && S.humps.indexOf(n) < 0) return [0, 0, 0, 0];
      if (n === 'tail' && !S.hasTail) return [0, 0, 0, 0];
      if (C.has(C.WOOL, n) && !S.hasWool) return [0, 0, 0, 0];

      var col;
      var limb = C.has(C.LIMB, n);
      var wool = C.has(C.WOOL, n);

      if (limb) {
        var m = MM.map[n], seg = (p.y - m.lo.y) / Math.max(0.001, m.hi.y - m.lo.y);
        col = MM.mix(MM.mul(flank, 0.93), flank, seg);
        // one wash down the WHOLE leg column in absolute Y, so it stays continuous
        // across thigh / calf / cannon instead of restarting per segment
        var down = 1 - MM.smooth(1.0, 18.0, p.y);
        col = MM.mix(col, legcol, S.legAmt * down);
        col = MM.mix(col, hoof, 0.7 * (1 - MM.smooth(-0.1, 2.2, p.y)));
      } else if (wool) {
        col = MM.mul(woolcol, 0.96 + 0.16 * MM.fbm(p, S.woolScale || 1.1, 3, 5));
      } else {
        var t = MM.smooth(S.shadeLo, S.shadeHi, (p.y - C.Y0) / (C.Y1 - C.Y0));
        col = MM.mix(belly, flank, MM.smooth(0.0, 0.55, t));
        col = MM.mix(col, back, MM.smooth(0.55, 1.0, t));
      }

      // a dark dorsal SADDLE over the hump ridge, fading onto the midline of the back. Lets
      // a ridge-backed species make the ridge itself its field mark (western, 2026-09-30).
      if (S.ridgeAmt) {
        // Graded up each hump from its base rather than flat: painted uniformly, the ridge
        // read as a dark saddle blanket strapped on top of the animal, not as coat.
        var hm = MM.map[n];
        var ridge = C.HUMPS.indexOf(n) >= 0
          ? MM.smooth(hm.lo.y + 0.15 * (hm.hi.y - hm.lo.y), hm.hi.y - 0.10 * (hm.hi.y - hm.lo.y), p.y)
          : (n === 'body_main' ? (1 - MM.smooth(1.5, 4.5, Math.abs(p.x))) * MM.smooth(26.0, 30.0, p.y) * 0.5 : 0);
        if (ridge > 0) {
          ridge *= 0.80 + 0.20 * MM.dith(o.u, o.v, 13);
          col = MM.mix(col, MM.hex(S.ridgeCol), S.ridgeAmt * ridge);
        }
      }

      // a camelid's pale muzzle and eye surround, kept to the NOSE cubes and the lower
      // face so it does not wash the whole head into a mask
      if (C.has(C.HEAD, n) && S.muzzleAmt) {
        var fr = MM.smooth(-19.0, -25.0, p.z);
        col = MM.mix(col, muzzle, S.muzzleAmt * fr);
      }

      // pale throat / underside, the llamas' countershading signature
      if (S.throatAmt && (C.has(C.NECK, n) || n === 'body_main')) {
        var th = (1 - MM.smooth(20.0, 26.5, p.y)) * MM.smooth(2.0, -12.0, p.z);
        col = MM.mix(col, muzzle, S.throatAmt * Math.max(0, Math.min(1, th)));
      }

      // domestic piebald: big irregular patches with hard-ish dithered edges. This is
      // what separates a llama from its wild guanaco ancestor at a glance.
      if (S.patchAmt) {
        // TWO octaves, not three: a third octave breaks the patches into speckle, and a
        // llama's markings are a few big blocks of colour with ragged edges.
        var q = MM.fbm(p, S.patchScale || 0.55, 2, 12);
        var pm = MM.smooth(0.47, 0.55, q);
        pm *= 0.9 + 0.2 * MM.dith(o.u, o.v, 8);
        col = MM.mix(col, patchcol, S.patchAmt * Math.max(0, Math.min(1, pm)));
      }

      var hair = MM.fbm(p, S.hairScale, 3, 1) - 0.5;
      col = MM.mul(col, 1 + S.hairAmt * hair);
      if (S.grizzleAmt) {
        var gz = MM.dith(o.u, o.v, 2) - 0.5;
        var cl = MM.smooth(0.35, 0.75, MM.fbm(p, 1.9, 2, 6));
        col = MM.mul(col, 1 + S.grizzleAmt * gz * (0.35 + 0.65 * cl));
      }
      if (S.dustAmt) {
        var du = (1 - MM.smooth(6.0, 20.0, p.y)) * (0.5 + 0.8 * MM.fbm(p, 0.9, 3, 4));
        col = MM.mix(col, MM.hex(S.dustCol), S.dustAmt * Math.max(0, Math.min(1, du)));
      }

      return MM.cl(col);
    });
  };

  // The camel eye convention is the INVERSE of the bison's -- it paints the INWARD quad
  // and clears the outward one, and both eyes share the rects. Stamp it, do not reason
  // about which half faces out; entityCutoutNoCull does no backface culling so both work.
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

  window.CAMEL = C;
  return 'camellib ready';
})()
