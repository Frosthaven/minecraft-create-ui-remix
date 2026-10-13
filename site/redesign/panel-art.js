// Reusable illustrations for panel redesigns: schematic scrolls (Create's blueprints on wooden rollers),
// oak planks, iron straps and brackets, small tools, leather, stitching, canvas labels, buckled straps. Uses the helpers in sprites.js and material.js.
// Coordinates are logical (hires canvases).

const hgrad = (g, x0, x1, stops) => {
  const gr = g.createLinearGradient(x0, 0, x1, 0);
  for (const [t, col] of stops) gr.addColorStop(t, col);
  return gr;
};
const BLUEPRINT = { top: '#2a3d52', bottom: '#1b2836', grid: 'rgba(190,210,230,0.08)', line: 'rgba(214,226,240,0.8)' };
const WOOD = [[0, '#9c774b'], [0.12, '#86653f'], [0.85, '#6a4c2c'], [1, '#45301a']];

/** Blueprint paper: a blue sheet with a faint drafting grid and darkened edges. */
function blueprint(g, x, y, w, h) {
  const sheet = () => { g.beginPath(); g.rect(x, y, w, h); };
  sheet(); g.fillStyle = vgrad(g, y, y + h, [[0, BLUEPRINT.top], [1, BLUEPRINT.bottom]]); g.fill();
  surface(g, sheet, TEX.blotch, 0.18);
  g.save(); sheet(); g.clip();
  g.strokeStyle = BLUEPRINT.grid; g.lineWidth = 0.15;
  for (let gx = x + 1.5; gx < x + w; gx += 3) { g.beginPath(); g.moveTo(gx, y); g.lineTo(gx, y + h); g.stroke(); }
  for (let gy = y + 1.5; gy < y + h; gy += 3) { g.beginPath(); g.moveTo(x, gy); g.lineTo(x + w, gy); g.stroke(); }
  g.strokeStyle = 'rgba(8,18,32,0.55)'; g.lineWidth = 1; g.strokeRect(x, y, w, h);
  g.restore();
}
/** A slot drawn on paper: a darker sunken square with a solid light edge, so it reads as a slot. */
function draftMark(g, x, y, s = 18) {
  g.fillStyle = 'rgba(5,10,18,0.45)'; g.fillRect(x + 0.5, y + 0.5, s - 1, s - 1);
  g.fillStyle = 'rgba(0,0,0,0.5)'; g.fillRect(x + 0.5, y + 0.5, s - 1, 0.6);
  g.strokeStyle = 'rgba(214,226,240,0.45)'; g.lineWidth = 0.5; g.strokeRect(x + 0.5, y + 0.5, s - 1, s - 1);
}
/** A wooden roller with brass end caps; horizontal from (x, y) along len, thick across. */
function roller(g, x, y, len, thick) {
  rrect(g, x + 0.6, y + 0.4, len - 1.2, thick, thick / 2); g.fillStyle = 'rgba(0,0,0,0.5)'; g.fill();
  rrect(g, x + 0.6, y, len - 1.2, thick, thick / 2); g.fillStyle = vgrad(g, y, y + thick, [[0, '#d9aa70'], [0.3, '#a8743f'], [0.75, '#6e4520'], [1, '#3a220c']]); g.fill();
  for (const cx of [x, x + len - 1.6]) {
    rrect(g, cx, y - 0.45, 1.6, thick + 0.9, 0.5); g.fillStyle = brass(g, y - 0.45, y + thick + 0.45); g.fill();
  }
}
/** A schematic scroll: blueprint paper between two horizontal rollers (top and bottom). The paper spans
 *  px to px + pw (default: 1 inside the rollers), so it can be narrower where a neighbour is close. */
function scroll(g, x, y, w, h, thick = 3, px = x + 1, pw = w - 2) {
  blueprint(g, px, y + thick / 2, pw, h - thick);
  // The paper curls into the rollers: a soft shadow under the top one, above the bottom one.
  g.fillStyle = vgrad(g, y + thick / 2, y + thick + 1.5, [[0, 'rgba(0,0,0,0.5)'], [1, 'rgba(0,0,0,0)']]); g.fillRect(px, y + thick / 2, pw, thick);
  g.fillStyle = vgrad(g, y + h - thick - 1.5, y + h - thick / 2, [[0, 'rgba(0,0,0,0)'], [1, 'rgba(0,0,0,0.45)']]); g.fillRect(px, y + h - thick - 1.5, pw, thick);
  roller(g, x, y, w, thick);
  roller(g, x, y + h - thick, w, thick);
}

/** Planks across a box (oak by default; pass other wood stops): seams every plankH, grain from the brushed texture. */
function planks(g, path, x, y, w, h, plankH, stops = WOOD) {
  path(); g.fillStyle = vgrad(g, y, y + h, stops); g.fill();
  g.save(); path(); g.clip();
  for (let py = y, i = 0; py < y + h; py += plankH, i++) {
    g.fillStyle = vgrad(g, py, py + plankH, [[0, 'rgba(255,230,190,0.18)'], [0.2, 'rgba(0,0,0,0)'], [1, 'rgba(40,20,5,0.25)']]);
    g.fillRect(x, py, w, plankH);
    if (i % 2) { g.fillStyle = 'rgba(60,30,10,0.05)'; g.fillRect(x, py, w, plankH); }
    g.fillStyle = 'rgba(40,22,8,0.6)'; g.fillRect(x, py + plankH - 0.35, w, 0.35);
    // Butt joints, staggered plank to plank.
    const jx = x + (i % 2 ? w * 0.62 : w * 0.3);
    g.fillRect(jx, py, 0.35, plankH);
  }
  g.restore();
  surface(g, path, TEX.brushed, 0.14);
}
/** An iron strap between two points, bolted at both ends. */
function strap(g, x0, y0, x1, y1, width) {
  g.save(); g.lineCap = 'butt';
  g.strokeStyle = 'rgba(0,0,0,0.5)'; g.lineWidth = width + 0.6; g.beginPath(); g.moveTo(x0, y0 + 0.3); g.lineTo(x1, y1 + 0.3); g.stroke();
  g.strokeStyle = '#2b2d30'; g.lineWidth = width; g.beginPath(); g.moveTo(x0, y0); g.lineTo(x1, y1); g.stroke();
  g.restore();
}
/** An L-shaped iron corner bracket with one bolt; sx, sy (+1 or -1) point its arms into the board. */
function bracket(g, x, y, sx, sy, arm = 6, t = 1.8) {
  g.beginPath();
  g.moveTo(x, y); g.lineTo(x + sx * arm, y); g.lineTo(x + sx * arm, y + sy * t); g.lineTo(x + sx * t, y + sy * t);
  g.lineTo(x + sx * t, y + sy * arm); g.lineTo(x, y + sy * arm); g.closePath();
  g.fillStyle = vgrad(g, Math.min(y, y + sy * arm), Math.max(y, y + sy * arm), [[0, '#55595e'], [1, '#26282b']]); g.fill();
  g.strokeStyle = 'rgba(0,0,0,0.5)'; g.lineWidth = 0.25; g.stroke();
  hexBolt(g, x + sx * Math.min(2.2, arm * 0.4), y + sy * Math.min(2.2, arm * 0.4), Math.min(0.5, arm * 0.13));
}
/** A hammer and a wrench, crossed, centred at (x, y), about 12 x k wide. */
function tools(g, x, y, k = 1) {
  g.save(); g.translate(x, y); g.scale(k, k);
  // Wrench: a steel bar with an open jaw.
  g.save(); g.rotate(-Math.PI / 5);
  rrect(g, -5.5, -0.6, 10, 1.2, 0.5); g.fillStyle = vgrad(g, -0.6, 0.6, [[0, '#c7ccd1'], [1, '#55595e']]); g.fill();
  circle(g, 5, 0, 1.6); g.fill();
  g.fillStyle = '#9a7448'; g.beginPath(); g.rect(5.2, -0.55, 1.6, 1.1); g.fill();
  g.restore();
  // Hammer: a wooden handle and a dark iron head.
  g.save(); g.rotate(Math.PI / 5);
  rrect(g, -5.5, -0.55, 10, 1.1, 0.4); g.fillStyle = vgrad(g, -0.55, 0.55, [[0, '#c69a63'], [1, '#6e4520']]); g.fill();
  rrect(g, 3.6, -2.2, 2.2, 4.4, 0.3); g.fillStyle = vgrad(g, -2.2, 2.2, [[0, '#7a7f85'], [1, '#2b2d30']]); g.fill();
  g.restore();
  g.restore();
}

/** Engraved into the surface: a dark cut with a faint lit lower edge. Flat, so it never reads as a control. */
function engrave(g, path, lw = 0.5) {
  g.save(); g.translate(0, 0.4); path(); g.strokeStyle = 'rgba(220,225,230,0.1)'; g.lineWidth = lw; g.stroke(); g.restore();
  path(); g.strokeStyle = 'rgba(0,0,0,0.6)'; g.lineWidth = lw; g.stroke();
}
/** Inlaid: set into the surface (dark upper wall, faint lit lower lip), never raised. */
function inlay(g, path, y0, y1) {
  g.save(); path(); g.clip();
  path(); g.strokeStyle = 'rgba(0,0,0,0.7)'; g.lineWidth = 1.2; g.stroke();
  g.restore();
  g.save(); g.beginPath(); g.rect(-1, (y0 + y1) / 2, 999, 999); g.clip();
  path(); g.strokeStyle = 'rgba(230,220,200,0.18)'; g.lineWidth = 0.5; g.stroke();
  g.restore();
}
/** Draws art as a flat burnt-in mark: everything drawn by fn() becomes one dark brown silhouette. */
function burnt(g, fn) {
  const c = document.createElement('canvas');
  c.width = g.canvas.width; c.height = g.canvas.height;
  const b = c.getContext('2d');
  b.setTransform(g.getTransform());
  fn.call(null, b);
  b.setTransform(1, 0, 0, 1, 0, 0);
  b.globalCompositeOperation = 'source-in'; b.fillStyle = 'rgba(40,22,8,0.75)'; b.fillRect(0, 0, c.width, c.height);
  g.save(); g.setTransform(1, 0, 0, 1, 0, 0); g.drawImage(c, 0, 0); g.restore();
}

const ironBezel = (g, y0, y1) => vgrad(g, y0, y1, [[0, '#5d6166'], [0.3, '#3a3d41'], [1, '#1a1b1d']]);

/** A slot as vanilla draws it: an 18 x 18 sunken cell, dark upper and left edges, a light lower and
 *  right lip. Neighbours share edges, so a grid reads as one surface. */
function cell(g, x, y, lip = 'rgba(200,205,210,0.2)', size = 18) {
  g.fillStyle = vgrad(g, y, y + size, [[0, '#0b0c0d'], [1, '#17181a']]); g.fillRect(x, y, size, size);
  g.fillStyle = 'rgba(0,0,0,0.75)'; g.fillRect(x, y, size, 0.6); g.fillRect(x, y, 0.6, size);
  g.fillStyle = lip; g.fillRect(x + 0.6, y + size - 0.45, size - 0.6, 0.45); g.fillRect(x + size - 0.45, y + 0.6, 0.45, size - 0.6);
}
/** A block of cells set into the plate: a thin dark recess line around it. */
function cells(g, list, lip) {
  for (const [x, y] of list) cell(g, x - 1, y - 1, lip);
  const xs = list.map(p => p[0] - 1), ys = list.map(p => p[1] - 1);
  const x0 = Math.min(...xs), y0 = Math.min(...ys), x1 = Math.max(...xs) + 18, y1 = Math.max(...ys) + 18;
  g.strokeStyle = 'rgba(0,0,0,0.6)'; g.lineWidth = 0.5; g.strokeRect(x0 - 0.25, y0 - 0.25, x1 - x0 + 0.5, y1 - y0 + 0.5);
}

/** A small steel push button (Curios, Cosmetic Armor), with a glyph drawn by glyph(g, w, h). */
function miniButton(w, h, glyph, hover) {
  const [c, g] = hires(w, h);
  steelButton(g, 0, 0, w, h, hover, Math.min(w, h) * 0.18);
  g.drawImage(glyphLayer(w, h, gg => { gg.fillStyle = gg.strokeStyle = hover ? '#ffd9a0' : '#c9ccd0'; gg.lineWidth = 0.6; glyph(gg, w, h); }), 0, 0, w, h);
  return c;
}
/** A button glyph on its own w x h layer with a thin dark outline: steel buttons follow dark mode (their face turns
 *  light grey with DME off or light), and the outline keeps the light glyph readable on a light face too. */
function glyphLayer(w, h, draw, outline = 0.35) {
  const [c, g] = hires(w, h);
  draw(g);
  outlineCanvas(c, outline);
  return c;
}
/** A dark outline around the opaque pixels of c, behind them (only inside rects, logical units, when given). */
function outlineCanvas(c, width = 0.5, rects = null) {
  const k = c.lw ? c.width / c.lw : R, s = document.createElement('canvas');
  s.width = c.width; s.height = c.height;
  const sg = s.getContext('2d');
  sg.drawImage(c, 0, 0); sg.globalCompositeOperation = 'source-in'; sg.fillStyle = '#0b0c0d'; sg.fillRect(0, 0, s.width, s.height);
  const g = c.getContext('2d');
  g.save(); g.setTransform(1, 0, 0, 1, 0, 0);
  if (rects) { g.beginPath(); for (const [x, y, rw, rh] of rects) g.rect(x * k, y * k, rw * k, rh * k); g.clip(); }
  g.globalCompositeOperation = 'destination-over';
  for (let a = 0; a < 8; a++) g.drawImage(s, Math.round(Math.cos(a * Math.PI / 4) * width * k), Math.round(Math.sin(a * Math.PI / 4) * width * k));
  g.restore();
}

/** A container panel: dark iron, quiet tarnish, a thin iron frame lit along its top. */
function panelBody(g, x, y, w, h) {
  const body = () => rrect(g, x + 0.4, y + 0.4, w - 0.8, h - 0.8, 2.4);
  rrect(g, x + 0.4, y + 1, w - 0.8, h - 0.8, 2.4); g.fillStyle = 'rgba(0,0,0,0.5)'; g.fill();
  body(); g.fillStyle = '#232426'; g.fill();   // flat, as every inventory section
  g.strokeStyle = ironBezel(g, y, y + h); g.lineWidth = 0.9; rrect(g, x + 0.85, y + 0.85, w - 1.7, h - 1.7, 2); g.stroke();
  rim(g, () => rrect(g, x + 0.3, y + 0.3, w - 0.6, h - 0.6, 2.5), y, y + h, 'rgba(220,225,230,0.5)', 0.3);
}

/** Dark oiled leather filling path(): a warm brown body, soft grain, a worn lighter top. */
function leather(g, path, y0, y1) {
  path(); g.fillStyle = vgrad(g, y0, y1, [[0, '#4a3122'], [0.5, '#3a2619'], [1, '#291a10']]); g.fill();
  surface(g, path, TEX.grain, 0.12); surface(g, path, TEX.blotch, 0.18);
}
/** A stitched seam along path(): dashes 0.5 long with 0.5 gaps (they survive nearest sampling at GUI
 *  scale 2), a dark needle-hole line beside them. */
function stitch(g, path, color = 'rgba(214,196,160,0.26)') {
  // Translucent thread, well below the panel's lit edges so it reads as texture, not as a highlight.
  g.save(); g.lineCap = 'butt';
  g.translate(0, 0.35); path(); g.strokeStyle = 'rgba(0,0,0,0.25)'; g.lineWidth = 0.5; g.setLineDash([0.5, 0.5]); g.stroke();
  g.translate(0, -0.35); path(); g.strokeStyle = color; g.stroke();
  g.restore();
}
/** A stitched-on canvas label: light enough for the game's grey title text, seamed all round. */
function canvasLabel(g, x, y, w, h) {
  const p = () => rrect(g, x, y, w, h, 0.6);
  rrect(g, x, y + 0.4, w, h, 0.6); g.fillStyle = 'rgba(0,0,0,0.4)'; g.fill();
  p(); g.fillStyle = vgrad(g, y, y + h, [[0, '#c9bb9c'], [1, '#ae9f80']]); g.fill();   // flat: every label matches
  stitch(g, () => rrect(g, x + 1, y + 1, w - 2, h - 2, 0.4), 'rgba(80,60,35,0.55)');
}
/** A leather strap from (x, y) of length len (horizontal), with a steel buckle at bx along it. */
function buckleStrap(g, x, y, len, w, bx) {
  const s = () => rrect(g, x, y, len, w, 0.4);
  rrect(g, x, y + 0.4, len, w, 0.4); g.fillStyle = 'rgba(0,0,0,0.45)'; g.fill();
  s(); g.fillStyle = vgrad(g, y, y + w, [[0, '#5a3c28'], [1, '#332115']]); g.fill();
  stitch(g, () => { g.beginPath(); g.moveTo(x + 0.8, y + 0.7); g.lineTo(x + len - 0.8, y + 0.7); g.moveTo(x + 0.8, y + w - 0.7); g.lineTo(x + len - 0.8, y + w - 0.7); }, 'rgba(214,196,160,0.4)');
  if (bx != null) {
    rrect(g, x + bx, y - 0.6, 3, w + 1.2, 0.5); g.strokeStyle = steel(g, y - 0.6, y + w + 0.6); g.lineWidth = 0.6; g.stroke();
    g.fillStyle = '#c9ccd0'; g.fillRect(x + bx + 1.25, y + w / 2 - 0.25, 1.6, 0.5);
  }
}
