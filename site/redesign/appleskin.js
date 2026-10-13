// AppleSkin's HUD overlays (3.0.9, HUDOverlayHandler), restyled to match our food row. AppleSkin blits from
// appleskin:textures/icons.png with UVs over 256 x 256, so an R x sheet works:
//   saturation outlines at v 0, u 0 / 9 / 18 / 27 for a quarter / half / three quarters / full, drawn over the
//   food icons (from the right, like food_half); the same in hunger colours at v 9;
//   the exhaustion underlay at v 18, 81 x 9, cropped from the right (u = 81 - w) at 75 % alpha.
// The rest of the sheet (tooltip icons) is kept, upscaled.
const SAT_FRACS = [0.25, 0.5, 0.75, 1];

/** A binary mask of img's alpha, grown (r > 0) or shrunk (r < 0) by |r| texels (square kernel). */
function alphaMask(img, r) {
  const w = img.width, h = img.height, c = document.createElement('canvas');
  c.width = w; c.height = h;
  const g = c.getContext('2d'); g.drawImage(img, 0, 0);
  const a = g.getImageData(0, 0, w, h).data, m = new Uint8Array(w * h), out = new Uint8Array(w * h), k = Math.abs(r);
  for (let i = 0; i < w * h; i++) m[i] = a[i * 4 + 3] > 96 ? 1 : 0;
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    let any = 0, all = 1;
    for (let dy = -k; dy <= k; dy++) for (let dx = -k; dx <= k; dx++) {
      const xx = x + dx, yy = y + dy, v = xx >= 0 && yy >= 0 && xx < w && yy < h ? m[yy * w + xx] : 0;
      any |= v; all &= v;
    }
    out[y * w + x] = r > 0 ? any : all;
  }
  return out;
}

/** A saturation outline: a thin band on the drumstick's rim (0.5 logical px: out 1 texel, in 3), the right
 *  frac of the icon, in colour (brass gold for saturation, rust for hunger). */
function saturationOutline(foodFull, frac, colour) {
  const [c, g] = hires(9, 9), w = c.width, h = c.height;
  const outer = alphaMask(foodFull, 1), inner = alphaMask(foodFull, -3), d = g.createImageData(w, h);
  const [r, gg, b] = colour, from = Math.round(w * (1 - frac));
  for (let i = 0; i < w * h; i++) {
    if (!outer[i] || inner[i] || i % w < from) continue;
    d.data.set([r, gg, b, 217], i * 4);
  }
  g.putImageData(d, 0, 0);
  return c;
}

/** The exhaustion underlay: a smoked band behind the food row, inset 1 px top and bottom (no checker). */
function exhaustionBand() {
  const [c, g] = hires(81, 9);
  g.fillStyle = 'rgba(14,11,9,0.59)'; g.fillRect(0, 1, 81, 7);
  return c;
}

/** AppleSkin's pieces from a sheet: our sprites, or crops of the original (img, 256 x 256) for "before". */
function appleskinParts(foodFull, original) {
  if (original) {
    const crop = (u, v, w, h) => {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      c.getContext('2d').drawImage(original, u, v, w, h, 0, 0, w, h); return c;
    };
    return { sat: SAT_FRACS.map((_, k) => crop(9 * k, 0, 9, 9)), exhaustion: crop(0, 18, 81, 9) };
  }
  return { sat: SAT_FRACS.map(f => saturationOutline(foodFull, f, [222, 176, 82])), exhaustion: exhaustionBand(),
    hunger: SAT_FRACS.map(f => saturationOutline(foodFull, f, [178, 86, 58])) };
}

/** appleskin:textures/icons.png at R x: the original upscaled, our regions painted in. */
function appleskinSheet(original, parts) {
  const [c, g] = hires(256, 256);
  g.imageSmoothingEnabled = false;
  if (original) g.drawImage(original, 0, 0, 256, 256);
  g.clearRect(0, 0, 36, 18); g.clearRect(0, 18, 81, 9);
  parts.sat.forEach((s, k) => g.drawImage(s, 9 * k, 0, 9, 9));
  parts.hunger.forEach((s, k) => g.drawImage(s, 9 * k, 9, 9, 9));
  g.drawImage(parts.exhaustion, 0, 18, 81, 9);
  return c;
}
