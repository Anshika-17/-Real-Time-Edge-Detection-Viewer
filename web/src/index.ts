const imgEl = document.getElementById('frame') as HTMLImageElement;
const fpsEl = document.getElementById('fps') as HTMLSpanElement;
const resEl = document.getElementById('res') as HTMLSpanElement;
const inputEl = document.getElementById('input') as HTMLTextAreaElement;
const loadBtn = document.getElementById('load') as HTMLButtonElement;
const sampleBtn = document.getElementById('useSample') as HTMLButtonElement;

const sampleBase64 = 'iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAABgUlEQVR42r2TsUoDURSGv7sUVaERIWsXCwkRKZAsbCytLC2clC5EJTh0JLCyMrKwQFstiJCLYgWkBSWUilpKKSk1sZCn3HPmbnZ2bnZnRvZOcnM7cOefMnDs4xsVL7jANpnf4AigCfAaMf+uMX8bLQhgYxV1McMfX2a7X5B+rExZ5Z7CjF0Cc1zspU62A02qgXwA8mmoHU4AOwF0cSGwzAnAwgG5dtJMzep6FRnxJ0n2BHyw7spYhPuvJbNrT2Sxffd/gcW2N2N6gbOizmAsFOZ4GNlDnaFxObMvkialhQfXqHkz4CLae1U7hkS+JzIsgfS9og32cx48F6gNoj2T5yacyFp/6mYWcphD2TR4ogp4+Q500O1lAj5c86X+w7LBYMxrkb82VN4kZVlSLMVsIrIkh6X5AlW8o0j6zD7FgQjagdaJKnN3hrgvWg5nFvZ9wjeKBdkuJ3gxelVjUOShEpX7DTMYpJnkru4gk3PXcv6135I+cFMwRWuGzzatkzJkYP+oK1OgVBvwI4D+pEYCdbzBz5bJNwW1EkqBkfZt+3MoSP6b9Z0l7AgnL1h9I/6MaWJ2EjyjAAAAAElFTkSuQmCC';

let lastTime = performance.now();
let frames = 0;
let fps = 0;

function ensureDataUri(base64: string): string {
  const trimmed = base64.trim();
  if (trimmed.startsWith('data:image')) {
    return trimmed;
  }
  return `data:image/png;base64,${trimmed}`;
}

function loadFrame(base64: string) {
  const dataUri = ensureDataUri(base64);
  imgEl.src = dataUri;
}

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
  inputEl.value = sampleBase64;
  sampleBtn.addEventListener('click', () => {
    inputEl.value = sampleBase64;
    loadFrame(sampleBase64);
  });
  loadBtn.addEventListener('click', () => {
    loadFrame(inputEl.value);
  });
  imgEl.onload = () => {
    resEl.textContent = `${imgEl.naturalWidth}x${imgEl.naturalHeight}`;
  };
  loadFrame(sampleBase64);
  update();
}

main();
