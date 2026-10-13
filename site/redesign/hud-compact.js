// Compact HUD concept (preview only, not packed): one iron console instead of the floating bits. Shipping it needs
// Create Dark Inventory to cancel the vanilla and mod HUD layers and draw this layout itself (RenderGuiLayerEvent).
// Logical units, x centred on the screen (0), the console's bottom at y 50. Uses sprites.js, meters.js (LIQUID),
// modhud.js (MANA, ROLL_TIERS), mcfont.js.
const CX = 170, BOTTOM = 50;   // canvas origin: x = CX + layout x
const CC = {
  plate: { x0: -168, x1: 168, wing: 27, mid: 22 },     // wing tops at y 27, the hotbar section from y 22
  slot: i => [-90 + 20 * i, 29],                        // socket top left (item at +1)
  offhand: [-165, 29], roll: [148, 29],
  xp: [-90, 23.6, 180, 3.2], badge: [0, 18],
  left: { icon: -146, x: -139, w: 44 }, right: { icon: 140, x: 95, w: 44 },
  rowMeter: [30.5, 6], rowStrip: [27.8, 1.6], rowMana: [40.5, 4],
};

/** A horizontal glass gauge: steel casing, dark glass, liquid (light top, dark bottom) to frac, segment lines. */
function ccGauge(g, x, y, w, h, frac, liq, segs = 0, from = 'left') {
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
/** A thin plating strip (armor, saturation): pips that light up to n of max. */
function ccStrip(g, x, y, w, h, n, max, on, off, from = 'left') {
  const pw = w / max;
  for (let k = 0; k < max; k++) {
    const px = from === 'left' ? x + k * pw : x + w - (k + 1) * pw;
    rrect(g, px + 0.2, y, pw - 0.4, h, 0.3); g.fillStyle = k < n ? on : off; g.fill();
  }
}
/** Small flat glyphs beside the gauges, centred on (x, y). */
const CC_GLYPHS = {
  heart: (g, x, y, col) => { g.save(); g.translate(x - 2.6, y - 2.4); g.scale(0.58, 0.58); heartPath(g); g.fillStyle = col; g.fill(); g.restore(); },
  food: (g, x, y, col) => { g.save(); g.translate(x - 2.6, y - 2.6); g.scale(0.58, 0.58); meatPath(g); g.fillStyle = col; g.fill(); g.restore(); },
  drop: (g, x, y, col) => { g.beginPath(); g.moveTo(x, y - 2.6); g.quadraticCurveTo(x + 2.4, y + 0.4, x, y + 2.4); g.quadraticCurveTo(x - 2.4, y + 0.4, x, y - 2.6); g.fillStyle = col; g.fill(); },
  bubble: (g, x, y, col) => { circle(g, x, y, 2); g.strokeStyle = col; g.lineWidth = 0.6; g.stroke(); },
};

/** The console plate: blackened iron, wings either side of a raised hotbar section, a lit brass top edge. */
function ccPlate(g) {
  const { x0, x1, wing, mid } = CC.plate, b = BOTTOM, path = () => {
    g.beginPath(); g.moveTo(x0, b); g.lineTo(x0, wing + 3); g.quadraticCurveTo(x0, wing, x0 + 3, wing);
    g.lineTo(-97, wing); g.lineTo(-92, mid); g.lineTo(92, mid); g.lineTo(97, wing);
    g.lineTo(x1 - 3, wing); g.quadraticCurveTo(x1, wing, x1, wing + 3); g.lineTo(x1, b); g.closePath();
  };
  g.save(); g.translate(0, -0.6); path(); g.fillStyle = 'rgba(0,0,0,0.55)'; g.fill(); g.restore();
  path(); g.fillStyle = iron(g, mid, b); g.fill(); ironSurface(g, path);
  rim(g, path, mid, b, P.brassHi, 0.35);
  // Rivets at the wing ends and the section steps.
  for (const x of [x0 + 2.5, x1 - 2.5]) rivet(g, x, b - 3, 0.7);
  for (const x of [-94, 94]) rivet(g, x, b - 3, 0.7);
}

/** The whole console for a state. Returns a canvas (logical 340 x 50). */
function compactHud(s, font) {
  const [c, g] = hires(340, 50);
  g.translate(CX, 0);
  ccPlate(g);
  // Hotbar: nine sockets, the selection frame, the offhand socket on the left wing.
  for (let i = 0; i < 9; i++) socket(g, ...CC.slot(i));
  socket(g, ...CC.offhand);
  // Experience: a glass pipe along the hotbar section's top edge, the level on a brass badge above its middle.
  const [xx, xy, xw, xh] = CC.xp;
  ccGauge(g, xx, xy, xw, xh, s.xp, ['#d8ff9a', '#5fd02a', '#1d5a08'], 18);
  if (s.level > 0) {
    const txt = String(s.level), tw = font ? mcWidth(font, txt) : 6, bw = tw + 6, [bx, by] = CC.badge;
    rrect(g, bx - bw / 2, by, bw, 9, 2); g.fillStyle = brass(g, by, by + 9); g.fill();
    rrect(g, bx - bw / 2 + 0.6, by + 0.6, bw - 1.2, 7.8, 1.6); g.fillStyle = '#0d0e0b'; g.fill();
    if (font) mcText(g, font, txt, bx - tw / 2 + 0.5, by + 1, 1, '#80ff20', true);
  }
  // Left wing: health (blood, absorption in gold over it), armor plating along its top, Iron's mana below.
  const L = CC.left, [my, mh] = CC.rowMeter, [sy, sh] = CC.rowStrip, [ny, nh] = CC.rowMana;
  CC_GLYPHS.heart(g, L.icon + 2.6, my + mh / 2, '#e0404a');
  ccGauge(g, L.x, my, L.w, mh, s.health / 20, LIQUID.blood, 10);
  if (s.absorb) ccGauge(g, L.x, my, L.w * s.absorb / 20, mh, 1, LIQUID.absorb, 0);
  ccStrip(g, L.x + 1, sy, L.w - 2, sh, Math.round(s.armor / 2), 10, '#c9ccd0', 'rgba(60,64,70,0.9)');
  CC_GLYPHS.drop(g, L.icon + 2.6, ny + nh / 2, MANA.irons[1]);
  ccGauge(g, L.x, ny, L.w, nh, s.irons, MANA.irons, 0);
  // Right wing (mirrored, filling from the right like vanilla food): food, AppleSkin saturation on top, Ars mana.
  const Rt = CC.right;
  CC_GLYPHS.food(g, Rt.icon + 2.6, my + mh / 2, '#e08a3a');
  ccGauge(g, Rt.x, my, Rt.w, mh, s.food / 20, LIQUID.broth, 10, 'right');
  ccStrip(g, Rt.x + 1, sy, Rt.w - 2, sh, Math.round(s.saturation / 2), 10, '#ffd36a', 'rgba(60,64,70,0.9)', 'right');
  CC_GLYPHS.drop(g, Rt.icon + 2.6, ny + nh / 2, MANA.ars[1]);
  ccGauge(g, Rt.x, ny, Rt.w, nh, s.ars, MANA.ars, 0, 'right');
  // Air: only under water, a gauge that rises out of the right wing's edge.
  if (s.air < 1) {
    const ay = CC.plate.wing - 5.2;
    rrect(g, Rt.x - 1, ay - 0.6, Rt.w + 2, 5.6, 1.6); g.fillStyle = iron(g, ay, ay + 5); g.fill();
    ccGauge(g, Rt.x, ay, Rt.w, 4, s.air, LIQUID.air, 10, 'right');
    CC_GLYPHS.bubble(g, Rt.icon + 2.6, ay + 2, LIQUID.air[1]);
  }
  // Dodge roll: two charge lamps stacked on the right wing's end.
  const [rx, ry] = CC.roll;
  for (let k = 0; k < 2; k++) {
    const y = ry + 2 + k * 8, on = k < s.rolls;
    rrect(g, rx, y, 16, 6, 1.5); g.fillStyle = steel(g, y, y + 6); g.fill();
    rrect(g, rx + 0.6, y + 0.6, 14.8, 4.8, 1.1); g.fillStyle = '#060708'; g.fill();
    g.beginPath();
    for (const dx of [3.5, 8]) { g.moveTo(rx + dx, y + 1.4); g.lineTo(rx + dx + 3, y + 3); g.lineTo(rx + dx, y + 4.6); }
    g.strokeStyle = on ? ROLL_TIERS.green : ROLL_TIERS.grey; g.lineWidth = 1; g.lineCap = 'round'; g.lineJoin = 'round'; g.stroke();
  }
  // The selected slot last, over its neighbours.
  const [sx, sy2] = CC.slot(s.slot);
  g.drawImage(selection(), sx - 3, sy2 - 3, 24, 23);
  return c;
}
