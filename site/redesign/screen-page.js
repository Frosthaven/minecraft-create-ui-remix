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
