// Messy wool / shaggy hair for the coat painters (2.8.0 review, 2026-11-18). eval'd after paintlib.js; exposes window.WOOL.
//
// Why: the shipped coats used ONE lock rule -- 3-texel columns, every lock in a column the SAME length, a root-to-tip ramp
// ending in a hard tip line. Each column became a repeating sawtooth and the face read as shingles / a knitted rug
// (user: woolly mammoth and mastodon "look like rugs", "much too patterned"). Before that the complaint was the opposite
// ("quite noisy"), so this keeps the LOW contrast and big shapes and only removes the regularity:
//   * columns drift (world-space noise) and vary 1.5-4.5 texels wide -- no straight full-height grooves;
//   * every lock draws its OWN length, so no two rows of tips line up -- no tiers;
//   * sub-columns inside a lock start 0-2 texels apart -- ragged tips, never a flat line;
//   * a lock's tip is blended over the next lock's root -- no hard light/dark seam;
//   * locks gather in clumps (shared phase, faint parting between clumps);
//   * broad matted patches (flatter, darker) and a broad tone drift carry the large-scale variation.
// Works in WORLD space (hair hangs along -y whatever the box's UV orientation), like the painters' other rules.
(function () {
  var MM = window.MM;
  var W = {};

  // world-space axes of a face: s runs across the hair, d runs along it (down a hanging face)
  W.axes = function (o) {
    var n = o.wn, p = o.wp;
    if (Math.abs(n[1]) > 0.7) {
      // a face looking up / down: the hair lies along the body (front to back), parted across it
      return { s: p.x, d: p.z, lying: true };
    }
    var tx = n[2], tz = -n[0], l = Math.sqrt(tx * tx + tz * tz) || 1;
    return { s: (p.x * tx + p.z * tz) / l, d: -p.y, lying: false };
  };

  // column of texel position s (after drift): boundaries every ~w with per-boundary jitter
  W.column = function (s, w, seed) {
    var k0 = Math.floor(s / w), c = k0 - 2;
    for (var k = k0 - 2; k <= k0 + 2; k++) {
      var b = k * w + 1.6 * (MM.h(k, 1, 0, seed + 3) - 0.5) * (w / 3);
      if (b <= s) c = k;
    }
    var b0 = c * w + 1.6 * (MM.h(c, 1, 0, seed + 3) - 0.5) * (w / 3);
    return { c: c, q: Math.floor(s - b0) };
  };

  W.last = { c: 0, k: 0, t: 0 };
  // phase of a lock column = CLUMP (shared by the ~3 columns of a clump) + INDIV (its own) + RAGGED (per texel sub-column)
  W.CLUMP = 9; W.INDIV = 2.5; W.RAGGED = 2.5;

  // tone of one hanging (or lying) lock texel. lenMin/lenSpan in texels; contrast = per-lock tone swing (keep ~0.1)
  W.hang = function (o, root, mid, tip, lenMin, lenSpan, contrast, opt) {
    opt = opt || {};
    var seed = opt.seed || 211;
    var ax = W.axes(o);
    var width = opt.width || 3;
    var drift = 2.6 * (MM.fbm(o.wp, 0.085, 2, seed + 1) - 0.5) + 0.9 * (MM.fbm(o.wp, 0.21, 1, seed + 2) - 0.5);
    var col = W.column(ax.s + drift, width, seed);
    var c = col.c, q = col.q;
    var group = Math.floor((c + 7) / 3);
    // clumped phase: columns of one clump nearly agree, clumps disagree; each sub-column starts 0-2 texels apart
    var d = ax.d + 400 + W.CLUMP * MM.h(group, 2, 0, seed + 5) + W.INDIV * MM.h(c, 3, 0, seed + 6) + Math.floor(MM.h(c, q, 0, seed + 8) * W.RAGGED);
    // walk the column's locks, each with its own length
    var acc = 0, k = 0, L = lenMin;
    while (k < 400) {
      L = lenMin + Math.floor(MM.h(c, k, 0, seed + 7) * (lenSpan + 1));
      if (acc + L > d) break;
      acc += L;
      k++;
    }
    var pos = d - acc;
    var t = pos / Math.max(1, L - 1);
    if (ax.lying) t = 0.25 + 0.5 * t;                        // lying hair: a flatter ramp (no hanging tips)
    var r = MM.mix(mid, root, 0.5), tp = MM.mix(mid, tip, 0.55);
    var out = t < 0.5 ? MM.mix(r, mid, t * 2) : MM.mix(mid, tp, (t - 0.5) * 2);
    // the previous lock's tip hangs over this lock's root: no hard seam where locks meet
    if (pos < 1.5 && !ax.lying) out = MM.mix(out, tp, 0.5 * (1.5 - pos) / 1.5);
    out = MM.mul(out, 1 + contrast * (MM.h(c, k, 0, seed + 9) - 0.5));
    out = MM.mul(out, 1 + 0.06 * (MM.h(group, 9, 0, seed + 10) - 0.5));
    // a faint parting where one clump meets the next
    if (q === 0 && (c + 7) % 3 === 0 && MM.h(c, 4, 0, seed + 11) < 0.6) out = MM.mul(out, 0.95);
    // matted patches: flatter and a little darker, the lock structure half lost
    // (rarer and softer than first tried: at 0.45 they read as hard camouflage blocks on the darker sloth palette)
    var mat = MM.smooth(0.6, 0.86, MM.fbm(o.wp, 0.065, 2, seed + 12));
    out = MM.mix(out, MM.mul(mid, 0.94), 0.3 * mat);
    // broad drift in tone across the animal
    out = MM.mul(out, 1 + 0.11 * (MM.fbm(o.wp, 0.12, 3, seed + 13) - 0.5));
    W.last.c = c; W.last.k = k; W.last.t = t;
    return out;
  };

  // the underside (a face looking down): dark roots, broad soft mottle, no texel blocks
  W.under = function (o, root) {
    return MM.mul(root, 0.78 + 0.22 * MM.fbm(o.wp, 0.3, 2, 223));
  };

  // a ragged hem for a hanging coat box: how many texels above the box's bottom edge this column's lock ends
  W.hem = function (o, maxLen) {
    var ax = W.axes(o);
    var drift = 2.6 * (MM.fbm(o.wp, 0.085, 2, 212) - 0.5);
    var col = W.column(ax.s + drift, 3, 211);
    return Math.floor(MM.h(col.c, col.q, 0, 231) * 0.5 * (maxLen + 1) + MM.h(col.c, 0, 0, 232) * 0.5 * (maxLen + 1));
  };

  window.WOOL = W;
  return 'woollib ok';
})();
