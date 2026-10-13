// Compact HUD concept (preview only, not packed): one iron console instead of the floating bits. Shipping it needs
// Create Dark Inventory to cancel the vanilla and mod HUD layers and draw this layout itself (RenderGuiLayerEvent).
// Logical units, x centred on the screen (0), the console's bottom at y 50. Every meter is the same size, with the
// HUD's own sprite as its icon (heart, drumstick, shield, porthole bubble). Uses sprites.js, meters.js (LIQUID),
// modhud.js (MANA, rollIcon, rollTier), mcfont.js.
const CX = 172, BOTTOM = 50;
const CC = {
  plate: { x0: -172, x1: 172, wing: 26, mid: 21, step: 5, r: 2 },
  slot: i => [-90 + 20 * i, 29],          // socket top left (item at +1)
  offhand: [-168, 29],
  rows: [28, 35, 42], meterH: 5, meterW: 38, icon: 7,
  left: { icon: -147, x: -138 },          // vitals: health, food, armor
  right: { icon: 140, x: 100 },           // magic: Iron's mana, Ars mana; air under water
  roll: [148, 29],
  xp: { y: 22.6, h: 3.2, x0: -90, x1: 90, gap: 9 },   // the pipe, split around the level window
};

/** A path through pts with every corner rounded by r (the plate's single corner style). */
function roundedPoly(g, pts, r) {
  const n = pts.length, mid = (a, b) => [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];
  g.beginPath(); g.moveTo(...mid(pts[n - 1], pts[0]));
  for (let i = 0; i < n; i++) g.arcTo(...pts[i], ...pts[(i + 1) % n], r);
  g.closePath();
}
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

/** The console plate: blackened iron, wings either side of a raised hotbar section, one corner radius throughout. */
function ccPlate(g) {
  const { x0, x1, wing, mid, step, r } = CC.plate, b = BOTTOM;
  const path = () => roundedPoly(g, [[x0, b], [x0, wing], [-92 - step, wing], [-92, mid], [92, mid], [92 + step, wing], [x1, wing], [x1, b]], r);
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
  // Experience: the glass pipe along the section's top edge, split around a small level window (the same pill shape).
  const X = CC.xp, half = (X.x1 - X.x0 - 2 * X.gap) / 2, fill = Math.max(0, Math.min(1, s.xp)) * 2;
  const xpLiq = ['#d8ff9a', '#5fd02a', '#1d5a08'];
  ccMeter(g, X.x0, X.y, half, X.h, Math.min(1, fill), xpLiq, 9);
  ccMeter(g, X.gap, X.y, half, X.h, Math.max(0, fill - 1), xpLiq, 9);
  if (s.level > 0) {
    const txt = String(s.level), tw = txt.length * 4 - 1, wy = X.y + X.h / 2 - 3.4, ww = Math.max(tw + 4, 2 * X.gap - 2);
    rrect(g, -ww / 2, wy, ww, 6.8, 3.4); g.fillStyle = steel(g, wy, wy + 6.8); g.fill();
    rrect(g, -ww / 2 + 0.5, wy + 0.5, ww - 1, 5.8, 2.9); g.fillStyle = '#060708'; g.fill();
    ccNumber(g, txt, -tw / 2, wy + 0.9, '#80ff20');
  } else {
    rrect(g, -X.gap + 1, X.y, 2 * X.gap - 2, X.h, X.h / 2); g.fillStyle = steel(g, X.y, X.y + X.h); g.fill();
  }
  // Meters: every one the same size, with the HUD's own sprite as its icon.
  const { rows, meterH: mh, meterW: mw, icon: is } = CC, L = CC.left, Rt = CC.right;
  const row = (side, k, iconDraw, frac, liq, segs, from) => {
    const y = rows[k];
    iconDraw(side.icon, y + mh / 2 - is / 2);
    ccMeter(g, side.x, y, mw, mh, frac, liq, segs, from);
  };
  const sprite = img => (x, y) => g.drawImage(img, x, y, is, is);
  row(L, 0, sprite(heart(false)), s.health / 20, LIQUID.blood, 10);
  if (s.absorb) ccMeter(g, L.x, rows[0], mw * s.absorb / 20, mh, 1, LIQUID.absorb, 0);
  row(L, 1, (x, y) => g.drawImage(food('full'), x - 1.5, y - 1.8, is + 3, is + 3), s.food / 20, LIQUID.broth, 10);   // its drawing fills less of its cell
  row(L, 2, sprite(armor('full')), s.armor / 20, ['#eef1f4', '#9aa0a6', '#3a3d41'], 10);
  row(Rt, 0, (x, y) => ccDrop(g, x, y, MANA.irons), s.irons, MANA.irons, 0);
  row(Rt, 1, (x, y) => ccDrop(g, x, y, MANA.ars), s.ars, MANA.ars, 0);
  if (s.air < 1) row(Rt, 2, sprite(air(false)), s.air, LIQUID.air, 10);
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
