// Material surfaces for the sprites: tileable value noise (grain, tarnish, brushing) laid lightly over
// a clipped shape, so metal and stone read as materials and not as flat gradients (stylized, not photoreal).

/** Tileable value noise as a grey canvas (128 is neutral for overlay). sx stretches it along x (brushing). */
function noiseTex(size, cells, octaves, { sx = 1, seed = 1, contrast = 1 } = {}) {
  const hash = (x, y, o) => {
    let h = (x * 374761393 + y * 668265263 + o * 2147483647 + seed * 1442695041) | 0;
    h = Math.imul(h ^ (h >>> 13), 1274126177);
    return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
  };
  const fade = t => t * t * (3 - 2 * t);
  const c = document.createElement('canvas');
  c.width = c.height = size;
  const g = c.getContext('2d'), img = g.createImageData(size, size), d = img.data;
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      let v = 0, amp = 0.5, norm = 0;
      for (let o = 0; o < octaves; o++) {
        const px = Math.max(1, Math.round(cells * (1 << o) / sx)), py = cells * (1 << o);
        const fx = x / size * px, fy = y / size * py, ix = Math.floor(fx), iy = Math.floor(fy);
        const tx = fade(fx - ix), ty = fade(fy - iy);
        const at = (i, j) => hash(((ix + i) % px + px) % px, ((iy + j) % py + py) % py, o);
        const a = at(0, 0) + (at(1, 0) - at(0, 0)) * tx, b = at(0, 1) + (at(1, 1) - at(0, 1)) * tx;
        v += (a + (b - a) * ty) * amp; norm += amp; amp *= 0.5;
      }
      const k = Math.max(0, Math.min(255, 128 + (v / norm - 0.5) * 255 * contrast)), i = (y * size + x) * 4;
      d[i] = d[i + 1] = d[i + 2] = k; d[i + 3] = 255;
    }
  }
  g.putImageData(img, 0, 0);
  return c;
}

const TEX = {
  grain: noiseTex(256, 64, 2, { seed: 3, contrast: 1.6 }),      // pitted cast iron, fine
  blotch: noiseTex(256, 4, 4, { seed: 7, contrast: 2.2 }),       // tarnish and wear, broad
  brushed: noiseTex(256, 96, 3, { sx: 24, seed: 11, contrast: 1.4 }), // brushed metal streaks
  rock: noiseTex(256, 16, 4, { seed: 19, contrast: 2 }),          // coal and stone
};

/** Lays a texture over the shape built by path(), in device pixels, with a blend mode. */
function surface(g, path, tex, alpha, mode = 'overlay') {
  g.save();
  path(); g.clip();
  g.setTransform(1, 0, 0, 1, 0, 0);
  g.globalCompositeOperation = mode; g.globalAlpha = alpha;
  g.fillStyle = g.createPattern(tex, 'repeat');
  g.fillRect(0, 0, g.canvas.width, g.canvas.height);
  g.restore();
}
/** Cast or wrought iron: pitting plus slow tarnish. */
function ironSurface(g, path) { surface(g, path, TEX.grain, 0.175); surface(g, path, TEX.blotch, 0.15); }
/** Aged brass: brushing plus dark tarnish blotches. */
function brassSurface(g, path) { surface(g, path, TEX.brushed, 0.15); surface(g, path, TEX.blotch, 0.2); }
