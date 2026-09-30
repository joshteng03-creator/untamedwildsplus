// Painter framework for untamedwildsplus skins. Loaded into Blockbench with
//   eval(require('fs').readFileSync(window.SP+'paintlib.js','utf8'))
// Requires window.RIGCUBES / window.RIGTEX from buildRig().
//
// Core idea (SOP phase 8): never guess UV face orientation. For every cube face,
// solve the affine map from the 4 (uv, position) corners of Blockbench's own
// geometry, then push each local point through mesh.matrixWorld. Every texel then
// has an exact MODEL-SPACE position, so paint rules are stated in animal terms
// (dorsal/ventral, fore/aft) and all face flips fall out automatically.
window.MM = (function () {
  var MM = {};

  // ---------- texel -> surface map ----------
  MM.build = function () {
    var TW = Project.texture_width, TH = Project.texture_height;
    var map = {};
    for (var name in window.RIGCUBES) {
      var cu = window.RIGCUBES[name], mesh = cu.mesh;
      mesh.updateMatrixWorld(true);
      var a = mesh.geometry.attributes, byN = {};
      for (var i = 0; i < a.position.count; i++) {
        var k = [Math.round(a.normal.getX(i)), Math.round(a.normal.getY(i)),
                 Math.round(a.normal.getZ(i))].join(',');
        (byN[k] = byN[k] || []).push({
          p: new THREE.Vector3(a.position.getX(i), a.position.getY(i), a.position.getZ(i))
              .applyMatrix4(mesh.matrixWorld),
          u: a.uv.getX(i) * TW, v: (1 - a.uv.getY(i)) * TH
        });
      }
      var faces = {}, lo = null, hi = null;
      for (var k2 in byN) {
        var vs = byN[k2];
        if (vs.length !== 4) continue;
        var u0 = Math.round(Math.min(vs[0].u, vs[1].u, vs[2].u, vs[3].u));
        var u1 = Math.round(Math.max(vs[0].u, vs[1].u, vs[2].u, vs[3].u));
        var v0 = Math.round(Math.min(vs[0].v, vs[1].v, vs[2].v, vs[3].v));
        var v1 = Math.round(Math.max(vs[0].v, vs[1].v, vs[2].v, vs[3].v));
        if (u1 - u0 < 1 || v1 - v0 < 1) continue;   // degenerate face of a flat plane cube
        var at = function (uu, vvv) {
          var best = null, bd = 1e9;
          for (var q = 0; q < 4; q++) {
            var d = Math.abs(vs[q].u - uu) + Math.abs(vs[q].v - vvv);
            if (d < bd) { bd = d; best = vs[q]; }
          }
          return best.p;
        };
        var o = at(u0, v0);
        var du = at(u1, v0).clone().sub(o).divideScalar(u1 - u0);
        var dv = at(u0, v1).clone().sub(o).divideScalar(v1 - v0);
        var n = k2.split(',').map(Number);
        var wn = new THREE.Vector3(n[0], n[1], n[2]).transformDirection(mesh.matrixWorld);
        // The FRACTIONAL extents MC actually samples, and a texel->world mapping based on them.
        // The rounded u0..u1 above cut a face short whenever its true edge was fractional, and
        // the texel it straddled was then painted only by the NEIGHBOURING face -- see MM.paint.
        var fu0 = Math.min(vs[0].u, vs[1].u, vs[2].u, vs[3].u), fu1 = Math.max(vs[0].u, vs[1].u, vs[2].u, vs[3].u);
        var fv0 = Math.min(vs[0].v, vs[1].v, vs[2].v, vs[3].v), fv1 = Math.max(vs[0].v, vs[1].v, vs[2].v, vs[3].v);
        var fo = at(fu0, fv0);
        var fdu = at(fu1, fv0).clone().sub(fo).divideScalar(fu1 - fu0);
        var fdv = at(fu0, fv1).clone().sub(fo).divideScalar(fv1 - fv0);
        faces[k2] = { u0: u0, v0: v0, u1: u1, v1: v1, o: o, du: du, dv: dv,
                      fu0: fu0, fu1: fu1, fv0: fv0, fv1: fv1, fo: fo, fdu: fdu, fdv: fdv,
                      n: n, wn: [wn.x, wn.y, wn.z], key: MM.faceName(n) };
        for (var q2 = 0; q2 < 4; q2++) {
          var p = vs[q2].p;
          if (!lo) { lo = p.clone(); hi = p.clone(); }
          lo.min(p); hi.max(p);
        }
      }
      map[name] = { faces: faces, lo: lo, hi: hi,
                    ctr: lo ? lo.clone().add(hi).multiplyScalar(0.5) : null };
    }
    MM.map = map;
    return Object.keys(map).length;
  };

  // Emulate AdvancedModelBox.setScale for a CHILDLESS part: scaling the group's
  // THREE object scales the box about its rotation point exactly as Java does,
  // and unlike editing from/to it leaves the box_uv rects untouched. Only valid
  // where the Java part has no children (Java needs setShouldScaleChildren for
  // that case), so assert it at the call site.
  MM.scalePart = function (name, sx, sy, sz) {
    var g = window.RIGGROUPS[name];
    if (!g) return 'no such part: ' + name;
    if (g.children && g.children.length > 1) return 'REFUSED, ' + name + ' has children';
    g.mesh.scale.set(sx, sy, sz);
    g.mesh.updateMatrixWorld(true);
    return name + ' scaled';
  };

  // Same thing for a part WITH children, i.e. the Java setScale + setShouldScaleChildren(true)
  // case (ModelDeer sizes a whole antler rack from one scale on the shared pedicle). THREE
  // propagates a parent's scale to its children, which is exactly that semantic. Kept
  // separate from MM.scalePart so the childless guard there still means something.
  MM.scalePartTree = function (name, s) {
    var g = window.RIGGROUPS[name];
    if (!g) return 'no such part: ' + name;
    g.mesh.scale.set(s, s, s);
    g.mesh.updateMatrixWorld(true);
    return name + ' tree-scaled ' + s;
  };

  MM.faceName = function (n) {
    if (n[1] > 0) return 'up';
    if (n[1] < 0) return 'down';
    if (n[2] < 0) return 'front';
    if (n[2] > 0) return 'back';
    return n[0] > 0 ? 'side_r' : 'side_l';   // rig +X is the animal's RIGHT
  };

  // ---------- model bounds ----------
  MM.bounds = function () {
    var lo = null, hi = null;
    for (var n in MM.map) {
      var m = MM.map[n];
      if (!m.lo) continue;
      if (!lo) { lo = m.lo.clone(); hi = m.hi.clone(); }
      lo.min(m.lo); hi.max(m.hi);
    }
    MM.LO = lo; MM.HI = hi;
    return { lo: [lo.x, lo.y, lo.z], hi: [hi.x, hi.y, hi.z] };
  };

  // ---------- noise ----------
  MM.h = function (x, y, z, s) {
    var n = Math.sin(x * 127.1 + y * 311.7 + z * 74.7 + (s || 0) * 43.31) * 43758.5453;
    return n - Math.floor(n);
  };
  MM.vnoise = function (p, sc, s) {
    var x = p.x * sc, y = p.y * sc, z = p.z * sc;
    var xi = Math.floor(x), yi = Math.floor(y), zi = Math.floor(z);
    var xf = x - xi, yf = y - yi, zf = z - zi;
    var sm = function (t) { return t * t * (3 - 2 * t); };
    var u = sm(xf), v = sm(yf), w = sm(zf), r = 0;
    for (var dx = 0; dx < 2; dx++) for (var dy = 0; dy < 2; dy++) for (var dz = 0; dz < 2; dz++)
      r += (dx ? u : 1 - u) * (dy ? v : 1 - v) * (dz ? w : 1 - w) * MM.h(xi + dx, yi + dy, zi + dz, s);
    return r;
  };
  MM.fbm = function (p, sc, oct, s) {
    var a = 0.5, t = 0, nn = 0;
    for (var i = 0; i < (oct || 3); i++) {
      t += a * MM.vnoise(p, sc * Math.pow(2, i), (s || 0) + i * 17); nn += a; a *= 0.5;
    }
    return t / nn;
  };
  // stable per-texel dither in [0,1)
  MM.dith = function (u, v, s) { return MM.h(u * 1.13 + 3.7, v * 2.71 + 1.3, 7.1, s || 0); };

  // ---------- colour ----------
  // Validated: a species object that reuses one key for both a colour and an amount
  // (hair / dust / mask) leaves the numeric value here and throws "s.substr is not a
  // function"; a mistyped hex is worse -- it NaNs silently and paints black. Fail loudly.
  MM.hex = function (s) {
    if (typeof s !== 'string' || !/^#[0-9a-fA-F]{6}$/.test(s)) {
      throw new Error('MM.hex: not a #rrggbb colour: ' + JSON.stringify(s));
    }
    return [parseInt(s.substr(1, 2), 16), parseInt(s.substr(3, 2), 16), parseInt(s.substr(5, 2), 16)];
  };
  MM.mix = function (a, b, t) {
    return [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t];
  };
  MM.mul = function (a, f) { return [a[0] * f, a[1] * f, a[2] * f]; };
  MM.cl = function (a) {
    return [Math.max(0, Math.min(255, Math.round(a[0]))),
            Math.max(0, Math.min(255, Math.round(a[1]))),
            Math.max(0, Math.min(255, Math.round(a[2])))];
  };
  MM.sat = function (a, f) {
    var g = a[0] * 0.299 + a[1] * 0.587 + a[2] * 0.114;
    return [g + (a[0] - g) * f, g + (a[1] - g) * f, g + (a[2] - g) * f];
  };
  MM.smooth = function (e0, e1, x) {
    var t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0)));
    return t * t * (3 - 2 * t);
  };

  // ---------- paint ----------
  // cb(info) -> [r,g,b] or [r,g,b,a] or null (leave transparent).
  // info: {part, face, n, wn, i, j, w, h, wp (THREE.Vector3 model space), u, v}
  MM.paint = function (cb) {
    var TW = Project.texture_width, TH = Project.texture_height;
    var c = document.createElement('canvas'); c.width = TW; c.height = TH;
    var ctx = c.getContext('2d');
    var img = ctx.createImageData(TW, TH);
    // Fractional box sizes (every sculpted rig here) put face edges mid-texel, and MC samples
    // every texel a face's FRACTIONAL UV range touches. Painting only the ROUNDED rect left the
    // straddled texel to whichever neighbour's rounded rect happened to include it -- usually
    // the DOWN face, which painters brighten x1.2 against MC's bottom shading, so on a pale
    // species every shoulder and thigh showed a near-white line along its top edge
    // (samotherium, 2026-09-30). Now each face paints every texel its fractional range
    // touches (its "fringe" beyond the rounded core, sampled at the clamped edge), and shared
    // texels go by priority, lowest first: down fringe, down core, other fringe, other core.
    // So a face's majority coverage beats another's sliver, a top edge beats a bottom face
    // (rarely seen on a standing animal), and a neighbouring part's own texels are never taken
    // by a fringe. Equal priority keeps the old last-wins. `i`/`j` passed to the callback stay
    // within the rounded rect so face-local painters behave exactly as before.
    var owner = new Uint8Array(TW * TH);
    for (var name in MM.map) {
      var F = MM.map[name].faces;
      for (var k in F) {
        var f = F[k], w = f.u1 - f.u0, h = f.v1 - f.v0;
        var fw = f.fu1 - f.fu0, fh = f.fv1 - f.fv0;
        var cu0 = Math.floor(f.fu0 + 1e-6), cu1 = Math.ceil(f.fu1 - 1e-6);
        var cv0 = Math.floor(f.fv0 + 1e-6), cv1 = Math.ceil(f.fv1 - 1e-6);
        for (var tv = cv0; tv < cv1; tv++) for (var tu = cu0; tu < cu1; tu++) {
          if (tu < 0 || tv < 0 || tu >= TW || tv >= TH) continue;
          var su = Math.min(Math.max(tu + 0.5 - f.fu0, 0), fw);
          var sv = Math.min(Math.max(tv + 0.5 - f.fv0, 0), fh);
          var p = f.fo.clone().addScaledVector(f.fdu, su).addScaledVector(f.fdv, sv);
          var i = Math.min(Math.max(tu - f.u0, 0), w - 1), j = Math.min(Math.max(tv - f.v0, 0), h - 1);
          var col = cb({ part: name, face: f.key, n: f.n, wn: f.wn, i: i, j: j, w: w, h: h,
                         wp: p, u: tu, v: tv });
          if (!col) continue;
          var tx = tv * TW + tu;
          var core = tu >= f.u0 && tu < f.u1 && tv >= f.v0 && tv < f.v1;
          var prio = (f.key === 'down' ? 1 : 3) + (core ? 1 : 0);
          if (owner[tx] > prio) continue;
          owner[tx] = prio;
          var px = tx * 4;
          img.data[px] = col[0]; img.data[px + 1] = col[1]; img.data[px + 2] = col[2];
          img.data[px + 3] = col.length > 3 ? col[3] : 255;
        }
      }
    }
    ctx.putImageData(img, 0, 0);
    MM.canvas = c;
    return c;
  };

  // ---------- edge bleed ----------
  // MANDATORY on any rig with non-integer box sizes, which is all of them here.
  // MC reserves a UV footprint of 2*(w+d) x (h+d) computed from the RAW float sizes, so a
  // 8.4 x 6.4 x 6.1 muzzle samples a 29 x 12.5 texel region. The painter can only fill
  // whole texels, so the outermost fraction of every face edge samples an unpainted,
  // fully transparent texel -- and entityCutoutNoCull DISCARDS those fragments, punching
  // visible holes along the seams. Worst on small parts: incisor_lower had 87 transparent
  // texels in a 150-texel edge halo.
  // Dilate painted colour outward into transparent neighbours so the fractional edge
  // always lands on something opaque.
  //
  // `protect` rects are left strictly alone in BOTH directions. The eye planes depend on
  // this: their inward quad is deliberately alpha-0'd to stop the two coplanar halves
  // z-fighting, and bleeding would refill it and bring the flicker straight back.
  MM.bleed = function (c, passes, protect) {
    c = c || MM.canvas;
    var TW = c.width, TH = c.height, ctx = c.getContext('2d');
    var img = ctx.getImageData(0, 0, TW, TH), D = img.data;
    var prot = new Uint8Array(TW * TH);
    (protect || []).forEach(function (r) {
      for (var y = r[1]; y < r[3]; y++) for (var x = r[0]; x < r[2]; x++)
        if (x >= 0 && y >= 0 && x < TW && y < TH) prot[y * TW + x] = 1;
    });
    var filled = 0;
    for (var pass = 0; pass < (passes || 2); pass++) {
      var src = new Uint8ClampedArray(D);
      for (var y2 = 0; y2 < TH; y2++) for (var x2 = 0; x2 < TW; x2++) {
        var o = (y2 * TW + x2) * 4;
        if (src[o + 3] !== 0 || prot[y2 * TW + x2]) continue;
        var r2 = 0, g2 = 0, b2 = 0, n2 = 0;
        for (var dy = -1; dy <= 1; dy++) for (var dx = -1; dx <= 1; dx++) {
          var xx = x2 + dx, yy = y2 + dy;
          if (xx < 0 || yy < 0 || xx >= TW || yy >= TH) continue;
          if (prot[yy * TW + xx]) continue;
          var q = (yy * TW + xx) * 4;
          if (src[q + 3] === 0) continue;
          r2 += src[q]; g2 += src[q + 1]; b2 += src[q + 2]; n2++;
        }
        if (!n2) continue;
        D[o] = r2 / n2; D[o + 1] = g2 / n2; D[o + 2] = b2 / n2; D[o + 3] = 255;
        filled++;
      }
    }
    ctx.putImageData(img, 0, 0);
    MM.bledCount = filled;
    return c;
  };

  // Full box-UV footprint rects of the given parts, as [u0,v0,u1,v1].
  MM.footprints = function (names) {
    return names.map(function (n) {
      var cu = window.RIGCUBES[n];
      var w = Math.abs(cu.to[0] - cu.from[0]), h = Math.abs(cu.to[1] - cu.from[1]),
          d = Math.abs(cu.to[2] - cu.from[2]);
      var u = cu.uv_offset[0], v = cu.uv_offset[1];
      return [u, v, u + Math.ceil(2 * (w + d)), v + Math.ceil(h + d)];
    });
  };

  // Just the real FACE rects of the given parts -- the right `protect` argument for
  // MM.bleed. Protecting a whole footprint also freezes its dead zone, and a neighbouring
  // part's sub-texel edge can fall inside that dead zone and be left unsealed.
  MM.faceRects = function (names) {
    var out = [];
    names.forEach(function (n) {
      var F = MM.map[n].faces;
      for (var k in F) out.push([F[k].u0, F[k].v0, F[k].u1, F[k].v1]);
    });
    return out;
  };

  // position ramp: hue from z (nose->tail), brightness from y. One continuous
  // gradient means every face map is right; a mismatched patch = a flipped face.
  MM.ramp = function () {
    MM.bounds();
    var lo = MM.LO, hi = MM.HI;
    return MM.paint(function (o) {
      var tz = (o.wp.z - lo.z) / (hi.z - lo.z);
      var ty = (o.wp.y - lo.y) / (hi.y - lo.y);
      var a = tz * 5;
      var seg = Math.floor(a) % 6, fr = a - Math.floor(a);
      var tab = [[255, 0, 0], [255, 160, 0], [220, 220, 0], [0, 200, 60], [0, 140, 255], [160, 60, 255]];
      var col = MM.mix(tab[seg], tab[(seg + 1) % 6], fr);
      return MM.cl(MM.mul(col, 0.35 + 0.65 * ty));
    });
  };

  // ---------- viewport ----------
  MM.show = function (c) {
    c = c || MM.canvas;
    var old = window.RIGTEX;
    var t = new Texture({ name: 'skin' }).fromDataURL(c.toDataURL()).add();
    for (var n in window.RIGCUBES) {
      var cu = window.RIGCUBES[n];
      for (var f in cu.faces) cu.faces[f].texture = t.uuid;
    }
    window.RIGTEX = t;
    if (old && old.uuid !== t.uuid) old.remove(false);
    t.select();                 // without this the viewport keeps the old image
    Canvas.updateAllFaces();
    Canvas.updateAll();
    return t.uuid;
  };

  // Decoded SYNCHRONOUSLY. This used to hand the bytes to an <img> and drawImage it on the
  // next line, but image decoding is asynchronous, so whenever the decode had not finished
  // the canvas came back blank -- and CAMEL.stampEyes then "stamped" 8 transparent texels,
  // shipping an eyeless western camel (2026-09-30). It only ever worked when the decode
  // happened to win the race. zlib is Node's, so this is exact and needs no event loop.
  MM.decodePNG = function (buf) {
    var zlib = require('zlib');
    var pos = 8, w = 0, h = 0, depth = 0, ctype = 0, idat = [], plte = null, trns = null;
    while (pos < buf.length) {
      var len = buf.readUInt32BE(pos), type = buf.toString('ascii', pos + 4, pos + 8);
      var data = buf.slice(pos + 8, pos + 8 + len);
      if (type === 'IHDR') { w = data.readUInt32BE(0); h = data.readUInt32BE(4); depth = data[8]; ctype = data[9]; }
      else if (type === 'PLTE') plte = data;
      else if (type === 'tRNS') trns = data;
      else if (type === 'IDAT') idat.push(data);
      else if (type === 'IEND') break;
      pos += 12 + len;
    }
    if (depth !== 8) throw new Error('MM.decodePNG: only 8-bit PNGs, got depth ' + depth);
    var bpp = { 6: 4, 2: 3, 0: 1, 4: 2, 3: 1 }[ctype];
    if (!bpp) throw new Error('MM.decodePNG: unsupported colour type ' + ctype);
    var raw = zlib.inflateSync(Buffer.concat(idat)), stride = w * bpp;
    var cur = Buffer.alloc(stride), prev = Buffer.alloc(stride), out = new Uint8ClampedArray(w * h * 4);
    for (var y = 0; y < h; y++) {
      var ft = raw[y * (stride + 1)];
      raw.copy(cur, 0, y * (stride + 1) + 1, (y + 1) * (stride + 1));
      for (var x = 0; x < stride; x++) {
        var a = x >= bpp ? cur[x - bpp] : 0, b2 = prev[x], c2 = x >= bpp ? prev[x - bpp] : 0, v = cur[x];
        if (ft === 1) v += a;
        else if (ft === 2) v += b2;
        else if (ft === 3) v += (a + b2) >> 1;
        else if (ft === 4) { var pp = a + b2 - c2, pa = Math.abs(pp - a), pb = Math.abs(pp - b2), pc = Math.abs(pp - c2);
          v += (pa <= pb && pa <= pc) ? a : (pb <= pc ? b2 : c2); }
        cur[x] = v & 255;
      }
      for (var px = 0; px < w; px++) {
        var o = (y * w + px) * 4, s = px * bpp;
        if (ctype === 6) { out[o] = cur[s]; out[o + 1] = cur[s + 1]; out[o + 2] = cur[s + 2]; out[o + 3] = cur[s + 3]; }
        else if (ctype === 2) { out[o] = cur[s]; out[o + 1] = cur[s + 1]; out[o + 2] = cur[s + 2]; out[o + 3] = 255; }
        else if (ctype === 0) { out[o] = out[o + 1] = out[o + 2] = cur[s]; out[o + 3] = 255; }
        else if (ctype === 4) { out[o] = out[o + 1] = out[o + 2] = cur[s]; out[o + 3] = cur[s + 1]; }
        else { var i3 = cur[s] * 3; out[o] = plte[i3]; out[o + 1] = plte[i3 + 1]; out[o + 2] = plte[i3 + 2];
          out[o + 3] = trns && cur[s] < trns.length ? trns[cur[s]] : 255; }
      }
      var t = prev; prev = cur; cur = t;
    }
    return { w: w, h: h, data: out };
  };

  MM.load = function (path) {
    var png = MM.decodePNG(require('fs').readFileSync(path));
    var c = document.createElement('canvas');
    c.width = Project.texture_width; c.height = Project.texture_height;
    var img = new ImageData(png.data, png.w, png.h);
    c.getContext('2d').putImageData(img, 0, 0);
    return c;
  };

  MM.save = function (path, c) {
    c = c || MM.canvas;
    Blockbench.writeFile(path, { content: c.toDataURL(), savetype: 'image' });
    return path;
  };

  // ---------- introspection (writes to disk; keeps tokens out of the transcript) ----------
  MM.dump = function (file) {
    var out = {};
    for (var n in MM.map) {
      var m = MM.map[n], fs2 = {};
      for (var k in m.faces) {
        var f = m.faces[k];
        fs2[f.key] = { uv: [f.u0, f.v0, f.u1, f.v1], px: [f.u1 - f.u0, f.v1 - f.v0],
                       o: [+f.o.x.toFixed(2), +f.o.y.toFixed(2), +f.o.z.toFixed(2)],
                       du: [+f.du.x.toFixed(3), +f.du.y.toFixed(3), +f.du.z.toFixed(3)],
                       dv: [+f.dv.x.toFixed(3), +f.dv.y.toFixed(3), +f.dv.z.toFixed(3)] };
      }
      out[n] = { lo: m.lo ? [+m.lo.x.toFixed(2), +m.lo.y.toFixed(2), +m.lo.z.toFixed(2)] : null,
                 hi: m.hi ? [+m.hi.x.toFixed(2), +m.hi.y.toFixed(2), +m.hi.z.toFixed(2)] : null,
                 faces: fs2 };
    }
    require('fs').writeFileSync(window.SP + file, JSON.stringify(out, null, 1));
    return Object.keys(out).length + ' parts -> ' + file;
  };

  return MM;
})();
'paintlib ready';
