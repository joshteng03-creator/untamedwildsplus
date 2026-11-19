(function () {
  var fs = require('fs');
  var pv = Preview.selected || Preview.all[0];
  var shots = window.SHOTS || [];
  var done = [];
  var oldPos = pv.camera.position.clone(), oldTgt = pv.controls.target.clone();
  shots.forEach(function (s) {
    pv.camera.position.set(s.pos[0], s.pos[1], s.pos[2]);
    pv.controls.target.set(s.tgt[0], s.tgt[1], s.tgt[2]);
    pv.camera.lookAt(pv.controls.target);
    pv.controls.update();
    pv.renderer.render(pv.scene || scene, pv.camera);
    var url = pv.canvas.toDataURL('image/png');
    fs.writeFileSync(window.OUT + s.file, Buffer.from(url.split(',')[1], 'base64'));
    done.push(s.file);
  });
  pv.camera.position.copy(oldPos);
  pv.controls.target.copy(oldTgt);
  pv.controls.update();
  return done.join(', ');
})()
