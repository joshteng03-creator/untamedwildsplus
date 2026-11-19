// Ground sloth skins for the REBUILT rig (43 boxes, 256x256) -- 2026-11-16. window.SL_SPECIES picks the species.
// Coat = the proboscidean CALM LOCK language the user approved after "the mammoth skin is quite noisy": 3-texel locks
// 7-11 long, staggered, a small per-lock tone swing, root-to-tip lightening, broad soft clumping in world space.
// What the July skins got wrong and this fixes:
//   * a per-box top-to-bottom gradient made every box a separate wooden panel -> colour comes from ONE world-space rule
//     (countershade by world height, darker legs), so overlapping boxes read as one animal;
//   * ivory "mitten" claws -> dark horn, paling only at the tips;
//   * three of five skins were flat recolours -> each species differs in coat colour AND treatment (Eremotherium
//     countershaded, Glossotherium coarse and pale, Megalonyx dark and dense, Megalocnus short-haired).
// Every rule keys on WORLD normals (o.wn). The inward half of each eye plane is cut AFTER the bleed.
(function () {
  var SP = window.SL_SPECIES || 'megatherium';
  var H = MM.hex;
  var P = {
    megatherium:   { root: H('#3a2717'), mid: H('#64452a'), tip: H('#8f6b45'), belly: 0.10, len: 7 },
    eremotherium:  { root: H('#5a4530'), mid: H('#8e7050'), tip: H('#bc9d74'), belly: 0.45, len: 7 },
    megalonyx:     { root: H('#2a221b'), mid: H('#4c3f33'), tip: H('#736152'), belly: 0.08, len: 8 },
    megalocnus:    { root: H('#4a3f33'), mid: H('#776753'), tip: H('#a2907a'), belly: 0.2, len: 4 },
    glossotherium: { root: H('#5e3d29'), mid: H('#94674a'), tip: H('#bf9272'), belly: 0.25, len: 6 },  // reddish tan (preserved mylodontid hair); pale buff sat 8.6 from Megalocnus
    // 2.9.0 (scratch/new_species_research.json): Lestodon pale straw-buff after Mylodon's preserved blond hair;
    // Paramylodon cool grizzled grey-brown (kept apart from the warm Glossotherium it resembles); Nothrotheriops the
    // rufous brown of its own preserved hair (Rampart Cave), redder than Glossotherium
    lestodon:       { root: H('#6a5a3e'), mid: H('#a8925e'), tip: H('#d2bd8a'), belly: 0.35, len: 7 },
    paramylodon:    { root: H('#2a2724'), mid: H('#524a41'), tip: H('#7e7568'), belly: 0.30, len: 6 },   // first pass sat 7.1 from Megalocnus: darker, duskier
    nothrotheriops: { root: H('#4a2416'), mid: H('#844a2c'), tip: H('#b0714a'), belly: 0.30, len: 6 }
  }[SP];
  var BELLY = H('#d6c6a6');
  var CLAW = H('#2a241f'), CLAW_TIP = H('#5e554b'), SKIN = H('#2c2420'), SOLE = H('#2a2420');
  var EYE_LID = H('#1e1814'), EYE_IRIS = H('#4a3220'), EYE_PUPIL = H('#0e0a08');
  var CUT = {};
  var TW = 256;

  var cls = function (n) {
    if (n.indexOf('claw_') === 0) return 'claw';
    if (n.indexOf('eye_') === 0) return 'eye';
    if (n.indexOf('ear_') === 0) return 'ear';
    if (n === 'hand_left' || n === 'hand_right' || n === 'foot_left' || n === 'foot_right') return 'foot';
    if (n.indexOf('arm_') === 0 || n.indexOf('leg_') === 0) return 'leg';
    if (n.indexOf('tail_') === 0) return 'tail';
    if (n === 'lip_mega' || n.indexOf('snout') === 0) return 'snout';
    if (n === 'head' || n === 'neck' || n.indexOf('jaw') === 0 || n.indexOf('cheek') === 0) return 'head';
    return 'body';
  };

  var lock = function (o, root, mid, tip, lenMin, lenSpan, contrast) {
    root = MM.mix(mid, root, 0.55);
    tip = MM.mix(mid, tip, 0.6);
    var cu = Math.floor(o.u / 3);
    var len = lenMin + Math.floor(MM.h(cu, 7, 0, 41) * lenSpan);
    var off = Math.floor(MM.h(cu, 3, 0, 43) * len);
    var cv = Math.floor((o.v + off) / len);
    var t = ((o.v + off) % len) / Math.max(1, len - 1);
    var col = t < 0.5 ? MM.mix(root, mid, t * 2) : MM.mix(mid, tip, (t - 0.5) * 2);
    col = MM.mul(col, 1 + contrast * (MM.h(cu, cv, 0, 47) - 0.5));
    if (t > 0.9) col = MM.mul(col, 0.92);
    col = MM.mul(col, 1 + 0.16 * (MM.fbm(o.wp, 0.22, 2, 53) - 0.5));
    return col;
  };
  var lying = function (o, mid, tip) {
    var c = MM.mix(mid, tip, 0.3 * MM.fbm(o.wp, 0.35, 2, 59));
    return MM.mul(c, 1 + 0.07 * (Math.floor(MM.h(Math.floor(o.u / 3), Math.floor(o.v / 3), 0, 61) * 4) / 3 - 0.5));
  };

  // 2.8.0 review: the coat rule above tiled into shingles ("they look like rugs"). WOOL_MODE 'old' reproduces the shipped
  // coat exactly (used to mask which texels the coat owns); otherwise the messy wool of woollib.js.
  var NEW = window.WOOL_MODE !== 'old' && !!window.WOOL;
  var hair = function (o, root, mid, tip, lenMin, lenSpan, contrast) {
    return NEW ? window.WOOL.hang(o, root, mid, tip, lenMin, lenSpan * 2, contrast) : lock(o, root, mid, tip, lenMin, lenSpan, contrast);
  };
  var lie = function (o, mid, tip, root) { return NEW ? window.WOOL.hang(o, root, mid, tip, 6, 8, 0.08) : lying(o, mid, tip); };
  // ONE world-space colour rule for the whole animal: countershade by world height (belly pale on Eremotherium),
  // legs a touch darker -- never a per-box gradient
  var coatTone = function (o, k) {
    var p = o.wp;
    var mid = P.mid, tip = P.tip, root = P.root;
    var under = 1 - MM.smooth(10.0, 22.0, p.y);
    if (k === 'leg' || k === 'foot') { mid = MM.mix(mid, root, 0.3); tip = MM.mix(tip, mid, 0.3); under *= 0.3; }
    mid = MM.mix(mid, BELLY, P.belly * under);
    tip = MM.mix(tip, BELLY, P.belly * under);
    return [root, mid, tip];
  };

  var cv = MM.paint(function (o) {
    var n = o.part, k = cls(n), p = o.wp, m = MM.map[n], col;
    var i0 = o.v * TW + o.u;
    if (k === 'eye') {
      var out = o.wn[0] * (p.x < 0 ? -1 : 1) > 0;
      if (!out) { CUT[i0] = true; return [0, 0, 0, 255]; }
      var fz = (p.z - m.lo.z) / Math.max(0.01, m.hi.z - m.lo.z), fy = (p.y - m.lo.y) / Math.max(0.01, m.hi.y - m.lo.y);
      if (fy > 0.6) return EYE_LID;
      return (fz > 0.33 && fz < 0.67) ? EYE_PUPIL : EYE_IRIS;
    }
    if (k === 'claw') {
      var tipT = n.charAt(n.length - 1) === '2' ? 1 : 0;   // the second segment is the claw's end
      col = MM.mix(CLAW, CLAW_TIP, tipT * 0.7);
      return MM.cl(MM.mul(col, 0.94 + 0.12 * MM.dith(o.u, o.v, 7)));
    }
    var tones = coatTone(o, k);
    if (k === 'foot') {
      if (o.wn[1] < -0.6) return MM.cl(MM.mul(SOLE, 0.95 + 0.1 * MM.dith(o.u, o.v, 9)));
      // the hind foot's sole faces INWARD (pedolateral): bare dark skin on the face turned toward the midline
      if ((n === 'foot_left' || n === 'foot_right') && o.wn[0] * (p.x < 0 ? -1 : 1) < -0.6) return MM.cl(SOLE);
      col = hair(o, tones[0], tones[1], tones[2], 3, 2, 0.12);
      return MM.cl(MM.mul(col, 0.88));
    }
    if (k === 'snout') {
      // bare dark skin at the muzzle tip (nose pad / prehensile lip), hair behind it
      var front = o.wn[2] < -0.6 || n === 'lip_mega';
      col = front ? MM.mul(SKIN, 0.95 + 0.1 * MM.dith(o.u, o.v, 11))
                  : MM.mix(hair(o, tones[0], tones[1], tones[2], 3, 2, 0.1), SKIN, 0.35 * MM.smooth(m.hi.z - 2.0, m.lo.z, p.z));
      return MM.cl(col);
    }
    if (o.wn[1] > 0.7) col = lie(o, tones[1], tones[2], tones[0]);
    else if (o.wn[1] < -0.7) col = NEW ? window.WOOL.under(o, tones[0]) : MM.mul(tones[0], 0.8 + 0.25 * (Math.floor(MM.h(o.u, Math.floor(o.v / 3), 0, 19) * 4) / 3));
    else col = hair(o, tones[0], tones[1], tones[2], P.len, k === 'head' || k === 'ear' ? 2 : 4, 0.12);
    if (k === 'head' || k === 'ear') col = MM.mix(col, tones[0], 0.12);
    if (n.indexOf('jaw') === 0) col = MM.mul(col, 0.92);
    return MM.cl(col);
  });

  MM.bleed(cv, 3, MM.faceRects(['eye_left', 'eye_right']));
  var cx = cv.getContext('2d'), img = cx.getImageData(0, 0, TW, TW), cut = 0;
  for (var key in CUT) {
    img.data[key * 4 + 3] = 0;
    cut++;
  }
  cx.putImageData(img, 0, 0);
  MM.show(cv);
  MM.canvas = cv;
  MM.save(window.OUT + 'skins/sloth_' + SP + '.png', cv);
  return SP + ' painted, ' + cut + ' texels cut';
})();
