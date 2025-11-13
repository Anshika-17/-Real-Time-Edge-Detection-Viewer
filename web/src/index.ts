const imgEl = document.getElementById('frame') as HTMLImageElement;
const fpsEl = document.getElementById('fps') as HTMLSpanElement;
const resEl = document.getElementById('res') as HTMLSpanElement;

// Dummy base64 image placeholder (1x1 black pixel)
const base64 = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8Xw8AAoMBj1JY1yQAAAAASUVORK5CYII=';

let lastTime = performance.now();
let frames = 0;
let fps = 0;

function update() {
  frames++;
  const now = performance.now();
  if (now - lastTime >= 1000) {
    fps = frames;
    frames = 0;
    lastTime = now;
    fpsEl.textContent = fps.toString();
  }
  requestAnimationFrame(update);
}

function main() {
  imgEl.src = base64;
  imgEl.onload = () => {
    resEl.textContent = `${imgEl.naturalWidth}x${imgEl.naturalHeight}`;
  };
  update();
}

main();
