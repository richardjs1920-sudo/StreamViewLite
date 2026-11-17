(() => {
  if (window.__fakeCamInstalled) return;
  window.__fakeCamInstalled = true;

  const real = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
  
  navigator.mediaDevices.getUserMedia = async (constraints) => {
    if (constraints?.video) {
      // Create a canvas to draw the animated background
      const canvas = document.createElement('canvas');
      canvas.width = 720;
      canvas.height = 1280;
      const ctx = canvas.getContext('2d');

      let start = Date.now();
      function draw() {
        const t = (Date.now() - start) / 1000;
        const g = ctx.createLinearGradient(0,0,0,canvas.height);
        g.addColorStop(0, `hsl(${(t*30)%360},70%,50%)`);
        g.addColorStop(1, `hsl(${(t*30+180)%360},70%,30%)`);
        ctx.fillStyle = g;
        ctx.fillRect(0,0,canvas.width,canvas.height);

        ctx.fillStyle = '#fff'; ctx.font = 'bold 60px sans-serif'; ctx.textAlign='center';
        ctx.fillText("FAKE CAM", canvas.width/2, canvas.height/2 - 40);
        ctx.font = 'bold 40px monospace';
        ctx.fillText(new Date().toISOString(), canvas.width/2, canvas.height/2 + 40);
        requestAnimationFrame(draw);
      }
      draw();

      return canvas.captureStream(30); // Return this as the camera stream
    }
    return real(constraints);
  };
})();
