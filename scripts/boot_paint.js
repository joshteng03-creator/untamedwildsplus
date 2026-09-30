(function () {
  window.SP = 'C:/Users/josht/OneDrive/Desktop/untamed wilds plus/untamedwildsplus/scripts/';
  window.OUT = 'C:/Users/josht/OneDrive/Desktop/untamed wilds plus/untamedwildsplus/scratch/';

  window.RIGCUBES = {};
  Cube.all.forEach(function (c) { window.RIGCUBES[c.name] = c; });
  window.RIGGROUPS = {};
  Group.all.forEach(function (g) { window.RIGGROUPS[g.name] = g; });
  window.RIGTEX = Texture.all[0];

  eval(require('fs').readFileSync(window.SP + 'paintlib.js', 'utf8'));

  MM.build();
  MM.bounds();

  var nf = 0;
  for (var n in MM.map) nf += Object.keys(MM.map[n].faces).length;

  return JSON.stringify({
    cubes: Object.keys(window.RIGCUBES).length,
    mapped: Object.keys(MM.map).length,
    faces: nf,
    lo: [+MM.LO.x.toFixed(2), +MM.LO.y.toFixed(2), +MM.LO.z.toFixed(2)],
    hi: [+MM.HI.x.toFixed(2), +MM.HI.y.toFixed(2), +MM.HI.z.toFixed(2)]
  });
})()
