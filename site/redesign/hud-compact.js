// Compact HUD concept (preview only, not packed): one iron console instead of the floating bits. Shipping it needs
// Create Dark Inventory to cancel the vanilla and mod HUD layers and draw this layout itself (RenderGuiLayerEvent).
// Logical units, x centred on the screen (0), the console's bottom at y 50. Every meter is the same size, with the
// HUD's own sprite as its icon (heart, drumstick, shield, porthole bubble). Uses sprites.js, meters.js (LIQUID),
// modhud.js (MANA, rollIcon, rollTier), mcfont.js.
const CX = 172, BOTTOM = 50;
const CC = {
  plate: { x0: -172, x1: 172, wing: 26, mid: 21, step: 8, r: 2.5 },
  slot: i => [-90 + 20 * i, 29],          // socket top left (item at +1)
  offhand: [-168, 29],
  rows: [28, 35, 42], meterH: 5, meterW: 38, icon: 6,
  left: { icon: -147, x: -138 },          // vitals: health, food, armor
  right: { icon: 140, x: 100 },           // magic: Iron's mana, Ars mana, then air
  roll: [148, 29],
  xp: { y: 22.6, h: 3.2, x0: -90, x1: 90 },   // the level window, then the pipe
};

/** A pill-shaped glass meter: steel casing, dark glass, liquid (light top, dark bottom) to frac, segment lines. */
function ccMeter(g, x, y, w, h, frac, liq, segs = 0, from = 'left') {
  const r = h / 2;
  rrect(g, x, y, w, h, r); g.fillStyle = steel(g, y, y + h); g.fill();
  rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, r - 0.5); g.fillStyle = '#060708'; g.fill();
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, r - 0.5); g.clip();
  const fw = (w - 1) * Math.max(0, Math.min(1, frac)), fx = from === 'left' ? x + 0.5 : x + w - 0.5 - fw;
  g.fillStyle = vgrad(g, y + 0.5, y + h - 0.5, [[0, liq[0]], [0.45, liq[1]], [1, liq[2]]]); g.fillRect(fx, y, fw, h);
  g.fillStyle = 'rgba(0,0,0,0.55)';
  for (let k = 1; k < segs; k++) g.fillRect(x + 0.5 + (w - 1) * k / segs - 0.15, y, 0.3, h);
  g.restore();
}
/** Draws a sprite canvas trimmed to its visible pixels, scaled to height h and centred on (cx, cy). */
function ccIcon(g, img, cx, cy, h) {
  const d = img.getContext('2d').getImageData(0, 0, img.width, img.height).data;
  let x0 = img.width, y0 = img.height, x1 = -1, y1 = -1;
  for (let y = 0; y < img.height; y++) for (let x = 0; x < img.width; x++) {
    if (d[(y * img.width + x) * 4 + 3] > 24) { x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y); }
  }
  if (x1 < 0) return;
  const bw = x1 - x0 + 1, bh = y1 - y0 + 1, w = h * bw / bh;
  g.drawImage(img, x0, y0, bw, bh, cx - w / 2, cy - h / 2, w, h);
}
/** A mana drop icon (7 x 7 cell at x, y) in the mana's colours. */
function ccDrop(g, x, y, liq) {
  const cx = x + 3.5;
  g.beginPath(); g.moveTo(cx, y + 0.3); g.quadraticCurveTo(cx + 3.2, y + 3.8, cx, y + 6.7); g.quadraticCurveTo(cx - 3.2, y + 3.8, cx, y + 0.3);
  g.fillStyle = vgrad(g, y, y + 7, [[0, liq[0]], [0.5, liq[1]], [1, liq[2]]]); g.fill();
  g.strokeStyle = 'rgba(0,0,0,0.7)'; g.lineWidth = 0.35; g.stroke();
}
// The level in a 3 x 5 pixel digit set (crisp at any GUI scale; the game font is 7 tall and would cover the pipe).
const CC_DIGITS = ['111101101101111', '010110010010111', '111001111100111', '111001111001111', '101101111001001',
  '111100111001111', '111100111101111', '111001010010010', '111101111101111', '111101111001111'];
function ccNumber(g, s, x, y, col) {
  for (const [i, ch] of [...s].entries()) {
    const bits = CC_DIGITS[+ch];
    for (let k = 0; k < 15; k++) if (bits[k] === '1') { g.fillStyle = col; g.fillRect(x + i * 4 + (k % 3), y + Math.floor(k / 3), 1, 1); }
  }
}

/** The console plate: blackened iron, wings either side of a raised hotbar section. Rounded everywhere: the outer
 *  corners by r, the climb from each wing to the section as a smooth S curve over `step`. */
function ccPlate(g) {
  const { x0, x1, wing, mid, step, r } = CC.plate, b = BOTTOM, k = step / 2;
  const path = () => {
    g.beginPath(); g.moveTo(x0, b - r); g.lineTo(x0, wing + r); g.quadraticCurveTo(x0, wing, x0 + r, wing);
    g.lineTo(-92 - step, wing); g.bezierCurveTo(-92 - k, wing, -92 - k, mid, -92, mid);
    g.lineTo(92, mid); g.bezierCurveTo(92 + k, mid, 92 + k, wing, 92 + step, wing);
    g.lineTo(x1 - r, wing); g.quadraticCurveTo(x1, wing, x1, wing + r); g.lineTo(x1, b - r);
    g.quadraticCurveTo(x1, b, x1 - r, b); g.lineTo(x0 + r, b); g.quadraticCurveTo(x0, b, x0, b - r); g.closePath();
  };
  g.save(); g.translate(0, -0.6); path(); g.fillStyle = 'rgba(0,0,0,0.55)'; g.fill(); g.restore();
  path(); g.fillStyle = iron(g, mid, b); g.fill(); ironSurface(g, path);
  rim(g, path, mid, b, P.brassHi, 0.35);
}

/** The whole console for a state; a canvas of logical 344 x 50. */
function compactHud(s, font) {
  const [c, g] = hires(344, 50);
  g.translate(CX, 0);
  ccPlate(g);
  for (let i = 0; i < 9; i++) socket(g, ...CC.slot(i));
  // The offhand socket with a faint dotted shield, so the empty slot reads as the offhand.
  const [ox, oy] = CC.offhand;
  socket(g, ox, oy);
  g.save(); g.translate(ox + 9, oy + 9.4); g.scale(1.25, 1.25); g.translate(-4.5, -4.5);
  shieldPath(g); g.setLineDash([0.4, 0.4]); g.strokeStyle = '#3a414b'; g.lineWidth = 0.4; g.stroke(); g.restore();
  // Experience: one glass pipe along the section's top edge (0 to 100 % in one run), the level in a small window at
  // its left end (the same pill shape, a little taller).
  const X = CC.xp, txt = String(Math.max(0, s.level)), tw = txt.length * 4 - 1, ww = Math.max(tw + 5, 12);
  const wy = X.y + X.h / 2 - 3.4;
  ccMeter(g, X.x0 + ww + 1, X.y, X.x1 - X.x0 - ww - 1, X.h, s.xp, ['#d8ff9a', '#5fd02a', '#1d5a08'], 18);
  rrect(g, X.x0, wy, ww, 6.8, 3.4); g.fillStyle = steel(g, wy, wy + 6.8); g.fill();
  rrect(g, X.x0 + 0.5, wy + 0.5, ww - 1, 5.8, 2.9); g.fillStyle = '#060708'; g.fill();
  ccNumber(g, txt, X.x0 + ww / 2 - tw / 2, wy + 0.9, s.level > 0 ? '#80ff20' : '#3a5a2a');
  // Meters: every one the same size, with the HUD's own sprite as its icon.
  const { rows, meterH: mh, meterW: mw, icon: is } = CC, L = CC.left, Rt = CC.right;
  // Icons: each sprite trimmed to its drawing, one height (is), centred on its meter's middle.
  const row = (side, k, img, frac, liq, segs, from) => {
    const y = rows[k];
    ccIcon(g, img, side.icon + is / 2, y + mh / 2, is);
    ccMeter(g, side.x, y, mw, mh, frac, liq, segs, from);
  };
  const drop = liq => { const [c2, g2] = hires(7, 7); ccDrop(g2, 0, 0, liq); return c2; };
  row(L, 0, heart(false), s.health / 20, LIQUID.blood, 10);
  if (s.absorb) ccMeter(g, L.x, rows[0], mw * s.absorb / 20, mh, 1, LIQUID.absorb, 0);
  row(L, 1, food('full'), s.food / 20, LIQUID.broth, 10);
  row(L, 2, armor('full'), s.armor / 20, ['#eef1f4', '#9aa0a6', '#3a3d41'], 10);
  row(Rt, 0, drop(MANA.irons), s.irons, MANA.irons, 0);
  row(Rt, 1, drop(MANA.ars), s.ars, MANA.ars, 0);
  row(Rt, 2, air(s.air < 0.15), s.air, LIQUID.air, 10);   // always shown: full above water, draining under it
  // Dodge roll: our dash icon, its count in the tier colour with the mod's black outline.
  const [rx, ry] = CC.roll, tier = rollTier(s.rolls);
  g.drawImage(rollIcon(tier), rx, ry, 18, 18);
  if (font) {
    const t = String(Math.floor(s.rolls)), tx = rx + 16, ty = ry + 10;
    for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) mcText(g, font, t, tx + dx, ty + dy, 1, '#000000');
    mcText(g, font, t, tx, ty, 1, ROLL_TIERS[tier]);
  }
  const [sx, sy] = CC.slot(s.slot);
  g.drawImage(selection(), sx - 3, sy - 3, 24, 23);
  return c;
}
