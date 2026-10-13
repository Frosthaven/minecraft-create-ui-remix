// Shared driver for the before/after screen pages (inventory, backpack): the manifest loader, a canvas
// with a put() that draws pack art crisp and our art smooth (or nearest, as the game samples), GUI scale
// buttons, toggles, and the file list. A page supplies compose(set, scale) and its before/after sets.

const pageQuery = new URLSearchParams(location.search);
const SCREEN = { scale: +(pageQuery.get('scale') ?? 4), nearest: pageQuery.has('nearest'), font: null, render: () => {} };

const loadImage = src => new Promise(r => { const i = new Image(); i.onload = () => r(i); i.onerror = () => r(null); i.src = src; });
/** Loads the almanac's manifest; returns tex(path, ns) giving the extracted image, or null. */
async function packTextures() {
  const manifest = await fetch('../manifest.json').then(r => r.json());
  const tex = async (p, ns = 'minecraft') => { const e = manifest.find(m => m.ns === ns && m.path === p); return e ? loadImage('../tex/' + e.file) : null; };
  const fontImg = await tex('textures/font/ascii');
  if (fontImg) SCREEN.font = mcFont(fontImg);
  return tex;
}
/** A w x h logical canvas at scale. put(img, x, y, w, h, sx, sy, sw, sh) draws (part of) an image. */
function screenCanvas(w, h, scale) {
  const c = document.createElement('canvas');
  c.width = w * scale; c.height = h * scale;
  const g = c.getContext('2d');
  const put = (img, x, y, dw, dh, sx = 0, sy = 0, sw = dw, sh = dh) => {
    if (!img) return;
    const k = img.lw ? img.width / img.lw : 1;
    g.imageSmoothingEnabled = !!img.lw && !SCREEN.nearest; g.imageSmoothingQuality = 'high';
    g.drawImage(img, sx * k, sy * k, sw * k, sh * k, x * scale, y * scale, dw * scale, dh * scale);
  };
  const text = (s, x, y, color = '#404040') => { if (SCREEN.font) mcText(g, SCREEN.font, s, x, y, scale, color); };
  return { c, g, put, text };
}
/** Wires the page: compose(set, scale) draws one view; sets() returns { before, after }. */
function screenPage(compose, sets) {
  SCREEN.render = () => {
    for (const [id, set] of Object.entries(sets())) {
      const c = compose(set, SCREEN.scale);
      c.id = id; if (id === 'before') c.style.imageRendering = 'pixelated';
      document.getElementById(id).replaceWith(c);
    }
  };
  for (const b of document.querySelectorAll('[data-scale]')) {
    b.setAttribute('aria-pressed', String(+b.dataset.scale === SCREEN.scale));
    b.addEventListener('click', () => {
      SCREEN.scale = +b.dataset.scale;
      for (const o of document.querySelectorAll('[data-scale]')) o.setAttribute('aria-pressed', String(o === b));
      SCREEN.render();
    });
  }
  pageToggle('nearest', on => { SCREEN.nearest = on; }, SCREEN.nearest);
}
/** A toggle button by id: flips aria-pressed, calls fn(on), re-renders. */
function pageToggle(id, fn, initial = false) {
  const el = document.getElementById(id);
  if (!el) return;
  el.setAttribute('aria-pressed', String(initial));
  el.addEventListener('click', () => {
    const on = el.getAttribute('aria-pressed') !== 'true';
    el.setAttribute('aria-pressed', String(on));
    fn(on); SCREEN.render();
  });
}
/** A texture sheet of size logical x logical holding img at its top left, at R x. */
function sheetTexture(img, size = 256) {
  const c = document.createElement('canvas');
  c.width = c.height = size * R;
  c.getContext('2d').drawImage(img, 0, 0);
  return c;
}
/** Lists [name, canvas] pairs with PNG download links. */
function fileList(files) {
  const el = document.getElementById('sheet');
  for (const [name, img] of files) {
    el.insertAdjacentHTML('beforeend', `<div class="sprite"><div class="meta"><b>${name}</b> (texture ${img.width}x${img.height}) ·
      <a download="${name.split('/').pop()}.png" href="${img.toDataURL()}">PNG</a></div></div>`);
  }
}

// --- Dark mode split (Dark Mode Everywhere, DME). DME's shader divides a pixel by its divide factor (5.5 "perfect
// dark") where the pixel is grey, less where it is saturated. Our panels are already dark, so the panel greys ship a
// second time as "<name>_grey": the same greys, neutral and 5.5 x lighter (vanilla-like light grey). The mod draws the
// full art with DME skipped, then the grey layer through DME: DME off gives a light panel, perfect dark gives back
// today's greys exactly, the lighter settings a lighter grey. Leather, wood, blueprints, brass and steel objects
// (buttons, frames, gauges) are not in the layer and never change.
const DME_MODES = [['none', 'Dark mode: no DME'], ['off', 'Dark mode: off'], ['perfect', 'Dark mode: perfect dark', 5.5],
  ['less', 'Dark mode: less perfect dark', 3.5], ['toasted', 'Dark mode: toasted light', 2]];
const DME_P = [0.299, 0.587, 0.114], DME_LIGHT = 5.5;
/** The grey layer of a texture: its opaque, near-neutral pixels dark enough to lighten 5.5 x (luma <= 255 / 5.5),
 *  as neutral grey at 5.5 x luma; everything else transparent. rects (logical units) limit it to those areas.
 *  Returns null when nothing is grey. */
function greyLayer(src, rects = null) {
  const w = src.width, h = src.height, k = src.lw ? w / src.lw : R;
  const d = src.getContext('2d').getImageData(0, 0, w, h), px = d.data;
  const inside = rects && new Uint8Array(w * h);
  if (rects) for (const [x, y, rw, rh] of rects) {
    for (let j = Math.max(0, Math.round(y * k)); j < Math.min(h, Math.round((y + rh) * k)); j++)
      inside.fill(1, j * w + Math.max(0, Math.round(x * k)), j * w + Math.min(w, Math.round((x + rw) * k)));
  }
  let any = false;
  for (let i = 0, p = 0; i < px.length; i += 4, p++) {
    const r = px[i], g = px[i + 1], b = px[i + 2], lum = r * DME_P[0] + g * DME_P[1] + b * DME_P[2];
    const grey = px[i + 3] >= 128 && Math.max(r, g, b) - Math.min(r, g, b) <= 12 && lum * DME_LIGHT <= 255 && (!inside || inside[p]);
    if (grey) { const v = Math.round(lum * DME_LIGHT); px[i] = px[i + 1] = px[i + 2] = v; px[i + 3] = 255; any = true; }
    else px[i + 3] = 0;
  }
  if (!any) return null;
  const c = document.createElement('canvas'); c.width = w; c.height = h; c.lw = src.lw; c.lh = src.lh;
  c.getContext('2d').putImageData(d, 0, 0);
  return c;
}
/** DME's dark_position_tex shader on an image (divide: the mode's factor). */
function dmeShade(src, divide) {
  const c = document.createElement('canvas'); c.width = src.width; c.height = src.height; c.lw = src.lw; c.lh = src.lh;
  const g = c.getContext('2d'); g.drawImage(src, 0, 0);
  const d = g.getImageData(0, 0, c.width, c.height), px = d.data;
  for (let i = 0; i < px.length; i += 4) {
    if (!px[i + 3]) continue;
    const col = [px[i] / 255, px[i + 1] / 255, px[i + 2] / 255], grey = col[0] * DME_P[0] + col[1] * DME_P[1] + col[2] * DME_P[2];
    const sat = Math.hypot(...col.map((v, j) => Math.abs(v - grey) ** (1 - DME_P[j])));
    const div = divide + (1 - divide) * sat ** 0.35;
    for (let j = 0; j < 3; j++) px[i + j] = Math.round(col[j] / div * 255);
  }
  g.putImageData(d, 0, 0);
  return c;
}
/** What the game shows for a split texture in a dark mode (DME_MODES id): the full art, then its grey layer, plain
 *  (DME off) or through DME's shader. 'none' (no DME installed: the mod draws no layer) is the full art. Cached. */
const DARK_VIEWS = new WeakMap();
function darkView(full, grey, mode) {
  if (!grey || mode === 'none') return full;
  const cache = DARK_VIEWS.get(full) || {}; DARK_VIEWS.set(full, cache);
  if (cache[mode]) return cache[mode];
  const divide = DME_MODES.find(m => m[0] === mode)[2];
  const c = document.createElement('canvas'); c.width = full.width; c.height = full.height; c.lw = full.lw; c.lh = full.lh;
  const g = c.getContext('2d'); g.drawImage(full, 0, 0); g.drawImage(divide ? dmeShade(grey, divide) : grey, 0, 0);
  return (cache[mode] = c);
}
/** A button cycling DME_MODES; fn(mode) then re-render. */
function darkModeButton(id, fn, initial = 'perfect') {
  const el = document.getElementById(id);
  if (!el) return initial;
  let i = DME_MODES.findIndex(m => m[0] === initial);
  el.textContent = DME_MODES[i][1];
  el.addEventListener('click', () => { i = (i + 1) % DME_MODES.length; el.textContent = DME_MODES[i][1]; fn(DME_MODES[i][0]); SCREEN.render(); });
  return initial;
}
