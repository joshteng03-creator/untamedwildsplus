// Proboscidean skins for the REBUILT rig (57 boxes, 256x256) -- 2026-11-15. window.PB_SPECIES picks the species.
// One painter for twelve species; what separates them is the SURFACE TREATMENT, not just the palette:
//   coat    long shaggy hair over everything, ragged hems on the coat boxes (woolly, steppe mammoth)
//   hairy   a shorter dense coat with the hide showing through (American mastodon)
//   sparse  bare hide with scattered short hair, thicker on the crown and back (Columbian, pygmy, gomphotheres)
//   bare    wrinkled hide only (elephants and Palaeoloxodon)
// Fixes the assessment found in the July skins:
//   * fur read as WOOD GRAIN -- straight full-height columns with a continuous dark parting line. Hair is now LOCKS:
//     2-texel cells 4-8 long, staggered so no two neighbours end together, each darkening at its root and shadowed
//     only where it ends;
//   * bare legs read as STACKED BLOCKS -- regular horizontal bands. Hide is now a low-contrast crack network in world
//     space with only faint, warped creases on the legs; the fine regular rings are kept to the trunk, where they belong.
// Every rule keys on WORLD normals (o.wn), never the box-local face name: the mane and skirts are authored rotated.
// Alpha cuts (coat hems, tail-tuft ends, the inward half of each eye plane) are applied AFTER the edge bleed, which
// would otherwise refill them (see the bleed lesson).
(function () {
  var SP = window.PB_SPECIES || 'woolly';
  var H = MM.hex;
  var P = {
    woolly:            { t: 'coat',   hide: H('#3a2a20'), root: H('#4a2c18'), mid: H('#8a5a32'), tip: H('#c9955a') },
    steppe_mammoth:    { t: 'coat',   hide: H('#2c2019'), root: H('#2c1a10'), mid: H('#5a3824'), tip: H('#8c5c36') },
    columbian:         { t: 'sparse', hide: H('#7a6a58'), hair: H('#55402e'), dens: 0.32 },
    pygmy_mammoth:     { t: 'sparse', hide: H('#7c4f35'), hair: H('#4e2d19'), dens: 0.36 },   // warm ginger: 8.9 from the Columbian on the same shape
    mastodon:          { t: 'hairy',  hide: H('#3c2c22'), root: H('#26170e'), mid: H('#4a2f1d'), tip: H('#76502f') },
    cuvieronius:       { t: 'sparse', hide: H('#8c7a66'), hair: H('#5c4836'), dens: 0.24 },
    notiomastodon:     { t: 'sparse', hide: H('#6a6158'), hair: H('#463d34'), dens: 0.2 },
    african_elephant:  { t: 'bare',   hide: H('#77716a'), dust: H('#9c6c4a') },
    asian_elephant:    { t: 'bare',   hide: H('#55504e'), pink: H('#c9a397') },
    straight_tusked:   { t: 'bare',   hide: H('#706e6a') },   // mid cool grey
    paleoloxodon_namadicus: { t: 'bare', hide: H('#4d4743') },   // dark charcoal grey (brown read as a mammoth), dL ~15 from straight_tusked
    cretan_dwarf:      { t: 'bare',   hide: H('#a69a89') },  // light warm buff, apart from straight_tusked (same shape)
    // 2.9.0 Stegodon (scratch/new_species_research.json, reconstructions): orientalis a warm brown-grey a shade darker and
    // browner than the Asian elephant, sparse dark bristle, dark forest-soil staining low on the legs; florensis lighter,
    // warmer, reddish-grey from Flores volcanic soil, darker along the back (the sparse patches sit on the crown and back)
    stegodon_orientalis: { t: 'sparse', hide: H('#68543f'), hair: H('#2c231c'), dens: 0.16, dust: H('#3a2c21') },   // #5a4b3f sat 10.0 from P. namadicus
    stegodon_florensis:  { t: 'sparse', hide: H('#86695a'), hair: H('#4e382c'), dens: 0.22, dust: H('#80574a') }   // dust #9c5a3c read salmon-pink
  }[SP];
  var IVORY = H('#d8cbaa'), IVORY_TIP = H('#eee6d2'), IVORY_STAIN = H('#9a7f5c');
  var NAIL = H('#c4b8a2'), SOLE = H('#37302b');
  var EYE_LID = H('#231c18'), EYE_IRIS = H('#5c3d22'), EYE_PUPIL = H('#100b08');
  var CUT = {};          // texel index -> true: made transparent after the bleed
  var TW = 256;

  var cls = function (n) {
    if (n.indexOf('tusk_sheath') === 0) return 'head';
    if (n.indexOf('tusk_') === 0) return 'tusk';
    if (n.indexOf('eye_') === 0) return 'eye';
    if (n.indexOf('ear_') === 0) return 'ear';
    if (n.indexOf('trunk_') === 0) return 'trunk';
    if (n.indexOf('fur_') === 0) return 'fur';
    if (n.indexOf('foot_') === 0) return 'foot';
    if (n.indexOf('leg_') === 0) return 'leg';
    if (n === 'tail') return 'tail';
    if (n === 'tail_tuft') return 'tuft';
    if (n === 'neck' || n === 'jaw' || n.indexOf('head') === 0) return 'head';
    return 'body';
  };
  var upw = function (o) { return o.wn[1]; };

  // ---- hide: low-contrast crack network + broad mottle (world space) ----
  // first pass read as pixel NOISE, not skin: the cracks were too fine and the dither too strong. Now broad warped FOLDS
  // (long, mostly horizontal on a hanging face) carry the wrinkle, a fainter fine crack network sits between them, and
  // each fold gets a lit texel above it so it reads as a ridge rather than a scratch.
  var hide = function (o, base, strength) {
    var p = o.wp, col = base;
    col = MM.mul(col, 1 + 0.12 * (MM.fbm(p, 0.28, 3, 11) - 0.5));
    var warp = 2.2 * (MM.fbm(p, 0.22, 2, 31) - 0.5);
    var fold = (p.y + warp + 0.35 * p.z * 0.0) / 2.6;
    var ff = fold - Math.floor(fold);
    var folds = MM.smooth(0.35, 0.75, MM.fbm(p, 0.18, 2, 37));          // folds come and go across the hide
    if (Math.abs(o.wn[1]) < 0.7) {
      if (ff < 0.13) col = MM.mul(col, 1 - 0.20 * strength * folds);
      else if (ff < 0.22) col = MM.mul(col, 1 + 0.07 * strength * folds);
    }
    var c1 = Math.abs(MM.fbm(p, 0.8, 3, 23) - 0.5);
    if (c1 < 0.03) col = MM.mul(col, 1 - 0.11 * strength);
    col = MM.mul(col, 0.975 + 0.05 * MM.dith(o.u, o.v, 5));
    return col;
  };

  // ---- hair locks (texel space: v runs DOWN a hanging face, so locks hang) ----
  // user 2026-11-15, first pass: "the mammoth skin is quite noisy". Locks are now WIDER (3 texels) and LONGER, the per-lock
  // tone swing is small, the root-to-tip ramp narrower, and the variation that remains is broad, soft clumping in world
  // space rather than texel-scale speckle.
  var lock = function (o, root, mid, tip, lenMin, lenSpan, contrast) {
    root = MM.mix(mid, root, 0.55);
    tip = MM.mix(mid, tip, 0.6);
    var cu = Math.floor(o.u / 3);
    var len = lenMin + Math.floor(MM.h(cu, 7, 0, 41) * lenSpan);
    var off = Math.floor(MM.h(cu, 3, 0, 43) * len);
    var cv = Math.floor((o.v + off) / len);
    var t = ((o.v + off) % len) / Math.max(1, len - 1);         // 0 at the root of this lock, 1 at its tip
    var col = t < 0.5 ? MM.mix(root, mid, t * 2) : MM.mix(mid, tip, (t - 0.5) * 2);
    col = MM.mul(col, 1 + contrast * (MM.h(cu, cv, 0, 47) - 0.5));
    if (t > 0.9) col = MM.mul(col, 0.92);                       // the lock ends: a soft shadow under its tip
    col = MM.mul(col, 1 + 0.16 * (MM.fbm(o.wp, 0.22, 2, 53) - 0.5));   // broad clumps
    return col;
  };
  // on a face that looks UP the hair lies along the back: clumped mottle, no hanging streaks
  var lying = function (o, mid, tip) {
    var c = MM.mix(mid, tip, 0.3 * MM.fbm(o.wp, 0.35, 2, 59));
    c = MM.mul(c, 1 + 0.07 * (Math.floor(MM.h(Math.floor(o.u / 3), Math.floor(o.v / 3), 0, 61) * 4) / 3 - 0.5));
    return c;
  };

  // 2.8.0 review: the coat rule above tiled into shingles ("they look like rugs"). WOOL_MODE 'old' reproduces the shipped
  // coat exactly (used to mask which texels the coat owns); otherwise the messy wool of woollib.js.
  var NEW = window.WOOL_MODE !== 'old' && !!window.WOOL;
  var hair = function (o, root, mid, tip, lenMin, lenSpan, contrast) {
    return NEW ? window.WOOL.hang(o, root, mid, tip, lenMin, lenSpan * 2, contrast) : lock(o, root, mid, tip, lenMin, lenSpan, contrast);
  };
  var lie = function (o, mid, tip, root) { return NEW ? window.WOOL.hang(o, root, mid, tip, 6, 8, 0.08) : lying(o, mid, tip); };

  var skinOf = function (o, k) {
    var p = o.wp, base = P.hide;
    if (P.t === 'bare' || P.t === 'sparse') {
      if (P.dust) {                                              // African: red dust-bath earth low on the body and legs
        var dust = (1 - MM.smooth(8, 26, p.y)) * 0.55 + 0.25 * MM.smooth(0.55, 0.8, MM.fbm(p, 0.25, 2, 71));
        base = MM.mix(base, P.dust, Math.min(0.7, dust));
      }
      if (P.pink && (k === 'head' || k === 'trunk' || k === 'ear')) {
        // Asian elephant depigmentation: pink FRECKLES ON GREY, confined to the lower forehead, the trunk base and the ear
        // margins. (First pass inverted it -- the whole head and ears pink with grey blotches.)
        var zone, mm = MM.map[o.part];
        if (k === 'head') zone = MM.smooth(-39.0, -42.0, p.z) * (1 - MM.smooth(38.0, 43.0, p.y));
        else if (k === 'trunk') zone = MM.smooth(17.0, 27.0, p.y);
        else zone = Math.max(1 - MM.smooth(0.0, 3.0, mm.hi.y - p.y), 1 - MM.smooth(0.0, 3.0, p.z - mm.lo.z));
        if (zone > 0.05 && MM.fbm(p, 1.1, 2, 73) > 1.0 - 0.5 * zone) base = MM.mix(base, P.pink, 0.6);
      }
    }
    var col = hide(o, base, P.t === 'bare' ? 1.0 : 0.7);
    if (P.t === 'sparse' && k !== 'ear') {
      // First pass scattered dark texel dashes over the whole hide and read as SPECKLE (the noise the user had just
      // rejected on the woolly). Sparse hair is now broad, soft darker PATCHES where hair grows thickest -- the crown,
      // the back and the shoulders -- with only a few faint strands, and none on the (shrunk, fine-texel) ears.
      var where = (k === 'head' ? 1.3 : 1.0) * MM.smooth(26, 46, p.y);
      var patch = MM.smooth(0.42, 0.72, MM.fbm(p, 0.32, 3, 77)) * where;
      col = MM.mix(col, P.hair, 0.42 * patch * Math.min(1, P.dens * 3));
      if (MM.h(o.u, Math.floor(o.v / 3), 0, 79) < P.dens * where * 0.18) col = MM.mix(col, P.hair, 0.3);
    }
    return col;
  };

  var cv = MM.paint(function (o) {
    var n = o.part, k = cls(n), p = o.wp, m = MM.map[n], col;
    var i0 = o.v * TW + o.u;
    if (k === 'eye') {
      // the inward half of a zero-width plane is cut (it z-fights its twin); the outward half is a 3x2 eye
      var out = o.wn[0] * (p.x < 0 ? -1 : 1) > 0;
      if (!out) { CUT[i0] = true; return [0, 0, 0, 255]; }
      var fz = (p.z - m.lo.z) / Math.max(0.01, m.hi.z - m.lo.z), fy = (p.y - m.lo.y) / Math.max(0.01, m.hi.y - m.lo.y);
      if (fy > 0.5) return EYE_LID;
      return (fz > 0.33 && fz < 0.67) ? EYE_PUPIL : EYE_IRIS;
    }
    if (k === 'tusk') {
      var seg = parseInt(n.charAt(n.length - 1), 10);
      col = MM.mix(IVORY, IVORY_TIP, (seg - 1) / 4);
      if (seg === 1) col = MM.mix(col, IVORY_STAIN, 0.55 * MM.smooth(m.lo.y + 3.0, m.hi.y, p.y));   // stained where it leaves the lip
      col = MM.mul(col, 1 + 0.06 * (MM.h(o.u, 0, 0, 83) - 0.5));                                   // faint lengthwise growth lines
      return MM.cl(MM.mul(col, 0.97 + 0.06 * MM.dith(o.u, o.v, 85)));
    }
    if (k === 'foot') {
      if (upw(o) < -0.7) return MM.cl(MM.mul(SOLE, 0.95 + 0.1 * MM.dith(o.u, o.v, 87)));
      col = skinOf(o, 'leg');
      var front = o.wn[2] < -0.6, low = p.y - m.lo.y;
      if (front && low < 1.9 && (o.i % 3) !== 2) col = MM.mix(NAIL, col, 0.15 * MM.h(o.u, o.v, 0, 89));   // toenails
      if (P.t === 'coat' || P.t === 'hairy') col = MM.mix(col, P.root, 0.5);
      return MM.cl(col);
    }
    if (k === 'tuft') {
      if (upw(o) < -0.7) return MM.cl(MM.mul(H('#1c1612'), 1));
      var lowT = p.y - m.lo.y;
      if (lowT < Math.floor(MM.h(o.u, 5, 0, 91) * 3)) CUT[i0] = true;
      return MM.cl(MM.mul(H('#241b15'), 0.85 + 0.3 * MM.h(o.u, Math.floor(o.v / 2), 0, 93)));
    }
    if (k === 'trunk') {
      col = skinOf(o, 'trunk');
      if (Math.abs(upw(o)) < 0.7 && (o.j % 2) === 0) col = MM.mul(col, 0.9 + 0.05 * MM.h(o.u, o.v, 0, 95));   // fine rings
      if (o.wn[2] > 0.6) col = MM.mul(col, 1.06);                                                            // inner side paler
      if (P.t === 'coat') col = MM.mix(col, hair(o, P.root, P.mid, P.tip, 3, 3, 0.25), n === 'trunk_1' ? 0.7 : 0.25);
      return MM.cl(col);
    }
    if (k === 'ear') {
      col = skinOf(o, 'ear');
      var outer = o.wn[0] * (p.x < 0 ? -1 : 1) > 0.3;
      if (!outer && Math.abs(upw(o)) < 0.7) {                     // inner face: darker, pinker, branching veins
        col = MM.mix(MM.mul(col, 0.84), [150, 112, 104], P.t === 'bare' ? 0.18 : 0.0);
        if (Math.abs(MM.fbm(p, 0.9, 3, 97) - 0.5) < 0.045) col = MM.mul(col, 0.82);
      }
      // the big African ear's trailing edge is tattered and torn: notch the last texels of the back edge
      if (SP === 'african_elephant') {
        var back = MM.map[n].hi.z - p.z;
        if (back < 1.6 && MM.h(Math.floor(p.y), 0, 0, 99) < 0.35) CUT[o.v * TW + o.u] = true;
      }
      // a small ear is the big African-sized box SHRUNK (its texels ~3.4x finer than the rest), so busy locks on it read
      // as a noisy patch: coated ears are plain dark hair with only a soft mottle
      if (P.t === 'coat' || P.t === 'hairy') col = MM.mul(MM.mix(P.root, P.mid, 0.35), 0.92 + 0.12 * MM.fbm(p, 0.6, 2, 101));
      return MM.cl(col);
    }
    if (k === 'tail') {
      col = skinOf(o, 'body');
      if (P.t === 'coat' || P.t === 'hairy') col = hair(o, P.root, P.mid, P.tip, 4, 3, 0.3);
      return MM.cl(col);
    }

    // ---- body, head, legs, coat boxes ----
    if (P.t === 'coat' || P.t === 'hairy') {
      var shag = P.t === 'coat';
      if (upw(o) > 0.7) col = lie(o, P.mid, P.tip, P.root);
      else if (upw(o) < -0.7) {
        col = NEW ? window.WOOL.under(o, P.root) : MM.mul(P.root, 0.75 + 0.3 * (Math.floor(MM.h(o.u, Math.floor(o.v / 3), 0, 19) * 4) / 3));
        if (k === 'fur' && (n === 'fur_skirt_left' || n === 'fur_skirt_right')) CUT[i0] = true;   // a curtain from below
      }
      else col = hair(o, P.root, P.mid, P.tip, shag ? 7 : 4, shag ? 5 : 3, shag ? 0.12 : 0.12);
      if (!shag) col = MM.mix(col, hide(o, P.hide, 0.5), 0.22);                 // the mastodon's hide shows through
      if (k === 'head') col = MM.mix(col, P.root, n === 'jaw' ? 0.5 : 0.12);   // shorter, darker hair on the face
      if (k === 'leg' && shag) col = MM.mix(col, P.root, 0.2);
      // ragged hem: every hanging coat face ends in locks of their own length
      if (k === 'fur' && Math.abs(upw(o)) < 0.7) {
        var up = p.y - m.lo.y;
        if (up < (NEW ? window.WOOL.hem(o, 6) : Math.floor(MM.h(Math.floor(o.u / 3), 3, 0, 61) * 5))) CUT[i0] = true;
      }
      return MM.cl(col);
    }
    col = skinOf(o, k);
    if (k === 'leg') {
      // faint, warped creases -- not the July bands
      var cr = (p.y + 1.4 * MM.fbm(p, 0.4, 2, 33)) / 2.3;
      if (cr - Math.floor(cr) < 0.14) col = MM.mul(col, 0.9);
    }
    if (n === 'jaw') col = MM.mul(col, 0.9);
    if (k === 'head' && n.indexOf('tusk_sheath') === 0) col = MM.mul(col, 0.94);
    return MM.cl(col);
  });

  var eyes = MM.faceRects(['eye_left', 'eye_right']);
  MM.bleed(cv, 3, eyes);
  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, TW, TW), cut = 0;
  for (var key in CUT) {
    img.data[key * 4 + 3] = 0;
    cut++;
  }
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/mammoth_' + SP + '.png', cv);
  return SP + ' painted (' + P.t + '), ' + cut + ' texels cut';
})();
