// Compact HUD concept (preview only, not packed): one iron action bar instead of the floating bits. Shipping it needs
// Create Dark Inventory to cancel the vanilla and mod HUD layers and draw this layout itself (RenderGuiLayerEvent).
// Logical units, x centred on the screen (0), the action bar's bottom at y 50. Every meter is the same size, with the
// HUD's own sprite as its icon (heart, drumstick, shield, porthole bubble). The always-on meters sit in the wings
// (health and food left, the two manas right); armor and breath, which the game shows only when they apply (armor
// worn; under water), extend the raised hotbar band sideways over their wing only then, in a darker iron, as slim
// pipes level with the experience pipe. Uses sprites.js, meters.js (LIQUID),
// modhud.js (MANA, rollIcon, rollTier), mcfont.js.
const CX = 172, BOTTOM = 50, TOPM = 66;   // TOPM: room above the bar for the game's text (item name, action bar)
// Three plates bolted together: the centre (hotbar and XP band, -91..91), a wing plate each side (±96..±165), the
// 5 px gaps bridged by riveted steel straps. In a wing, out from the gap: meter (32), 2, icon (6), 4, the offhand
// socket / the dodge roll (18), 4, the plate's edge.
const GAP = 4;
const CC = {
  plate: { x0: -165, x1: 165, wing: 28, mid: 21.4, step: 8, r: 2.5, sec: 91, inner: 96 },   // sec: the centre plate's half width; inner: the wings' inner edges
  slot: i => [-89 + 20 * i, 30],          // socket top left; items at (-88 + 20 i, 31): vanilla's exact item spots
  offhand: [-161, 30],
  rows: [33.5, 40.5], meterH: 4, meterW: 32, icon: 5.6,   // centred on the sockets' middle (y 39)
  left: { iconC: -136, x: -131 },         // vitals: health, food (armor extends the band above)
  right: { iconC: 136, x: 99 },           // magic: Iron's mana, Ars mana (breath extends the band above)
  ext: { l: [-140, -96], r: [96, 140], y: 23, h: 3.2, icon: 4.4 },   // a wing's raised tab: icon to the plate's inner edge   // band extensions; the outer S ends 1 px before the icon
  roll: [139.5, 30],                      // the dash's chevrons start at 143 (its art is inset 3.5)
  hit: {},                                // meter boxes by id, for the page's hover
  xp: { y: 23, h: 3.2, x0: -84, x1: 84 },   // 1.6 under the band's top, 1.8 above the wings   // the level window, then the pipe (inside the section)
};

/** A pill-shaped glass meter: steel casing, dark glass, liquid (light top, dark bottom) to frac, segment lines. */
function ccMeter(g, x, y, w, h, frac, liq, segs = 0, from = 'left') {
  const r = h / 2;
  // Satin, not chrome: a darker steel casing, and the liquid's highlight halfway to its body colour.
  rrect(g, x, y, w, h, r); g.fillStyle = vgrad(g, y, y + h, [[0, '#9ea4aa'], [0.25, '#6c7278'], [0.65, '#3a3e43'], [1, '#141619']]); g.fill();
  rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, r - 0.5); g.fillStyle = '#060708'; g.fill();
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, r - 0.5); g.clip();
  const fw = (w - 1) * Math.max(0, Math.min(1, frac)), fx = from === 'left' ? x + 0.5 : x + w - 0.5 - fw;
  g.fillStyle = vgrad(g, y + 0.5, y + h - 0.5, [[0, mixHex(liq[0], liq[1], 0.5)], [0.45, liq[1]], [1, liq[2]]]); g.fillRect(fx, y, fw, h);
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
/** Iron's mana: a round glass flask of glowing mana with a brass cap (the hearts' vials, in mana). 9 x 9. */
function ccFlask(liq) {
  const [c, g] = hires(9, 9);
  const body = () => { g.beginPath(); g.moveTo(3.6, 2.6); g.lineTo(3.6, 3.9); g.arc(4.5, 5.9, 2.9, Math.PI * 1.32, Math.PI * 1.68 + Math.PI * 2 - 0.0001, false); g.lineTo(5.4, 2.6); g.closePath(); };
  body(); g.fillStyle = 'rgba(10,12,16,0.9)'; g.fill();
  g.save(); body(); g.clip(); g.fillStyle = vgrad(g, 4.4, 8.8, [[0, liq[0]], [0.4, liq[1]], [1, liq[2]]]); g.fillRect(0, 4.4, 9, 5); g.restore();
  body(); g.strokeStyle = 'rgba(220,230,240,0.55)'; g.lineWidth = 0.35; g.stroke();
  rrect(g, 3.1, 1.2, 2.8, 1.6, 0.4); g.fillStyle = brass(g, 1.2, 2.8); g.fill();
  return c;
}
/** Ars mana: a faceted source gem in a brass bezel. 9 x 9. */
function ccGem(liq) {
  const [c, g] = hires(9, 9);
  const gem = (k) => { g.beginPath(); for (let i = 0; i < 6; i++) { const a = Math.PI / 6 + i * Math.PI / 3; g.lineTo(4.5 + k * Math.cos(a), 4.5 + k * Math.sin(a)); } g.closePath(); };
  gem(4.2); g.fillStyle = brass(g, 0.3, 8.7); g.fill();
  gem(3.3); g.fillStyle = vgrad(g, 1.2, 7.8, [[0, liq[0]], [0.45, liq[1]], [1, liq[2]]]); g.fill();
  g.strokeStyle = 'rgba(255,255,255,0.35)'; g.lineWidth = 0.3;
  g.beginPath(); g.moveTo(4.5, 1.2); g.lineTo(4.5, 4.5); g.lineTo(1.6, 6.2); g.moveTo(4.5, 4.5); g.lineTo(7.4, 6.2); g.stroke();
  return c;
}
/** Create's copper backtank (the breath row while diving): a riveted copper cylinder with a valve. 9 x 9. */
function ccBacktank() {
  const [c, g] = hires(9, 9);
  const body = () => rrect(g, 2.2, 2.4, 4.6, 6.2, 1.6);
  body(); g.fillStyle = vgrad(g, 2.4, 8.6, [[0, P.copperHi], [0.35, P.copper], [1, '#3a1608']]); g.fill();
  g.fillStyle = 'rgba(255,220,180,0.35)'; g.fillRect(3, 3.2, 0.5, 4.6);
  g.fillStyle = 'rgba(40,14,4,0.8)'; g.fillRect(2.2, 5.2, 4.6, 0.4);
  rrect(g, 3.7, 1.1, 1.6, 1.5, 0.3); g.fillStyle = steel(g, 1.1, 2.6); g.fill();
  rrect(g, 2.9, 0.5, 3.2, 0.8, 0.3); g.fillStyle = brass(g, 0.5, 1.3); g.fill();
  return c;
}
/** The compact drumstick: the HUD's roast with a bigger head and a short bone, so it reads at 6 px. 9 x 9. */
function ccDrumstick() {
  const [c, g] = hires(9, 9);
  g.strokeStyle = g.fillStyle = vgrad(g, 5, 8.8, [[0, '#efe4c8'], [0.5, '#c2b08c'], [1, '#6e6048']]);
  g.lineWidth = 1.1; g.lineCap = 'round'; g.beginPath(); g.moveTo(3.4, 5.6); g.lineTo(1.9, 7.1); g.stroke();
  circle(g, 1.2, 7.1, 0.75); g.fill(); circle(g, 1.9, 7.8, 0.75); g.fill();
  const meat = () => { g.beginPath(); g.ellipse(5.3, 3.7, 3.4, 2.7, -Math.PI / 4, 0, Math.PI * 2); };
  meat(); g.fillStyle = radial(g, 5.6, 3.3, 3.8, [[0, '#c8783e'], [0.45, '#8e4320'], [0.85, '#4a1c0a'], [1, '#2a0e04']], 4.8, 2.4); g.fill();
  surface(g, meat, TEX.blotch, 0.3);
  g.strokeStyle = 'rgba(255,215,160,0.55)'; g.lineWidth = 0.35; g.beginPath(); g.ellipse(5.3, 3.7, 2.6, 1.9, -Math.PI / 4, Math.PI * 1.05, Math.PI * 1.6); g.stroke();
  return c;
}
// The level in a 3 x 5 pixel digit set (crisp at any GUI scale; the game font is 7 tall and would cover the pipe).
const CC_DIGITS = ['111101101101111', '010110010010111', '111001111100111', '111001111001111', '101101111001001',
  '111100111001111', '111100111101111', '111001010010010', '111101111101111', '111101111001111'];
CC_DIGITS['/'] = '001001010100100';
CC_DIGITS[':'] = '000010000010000';
CC_DIGITS['+'] = '000010111010000';
// Time units, lowercase on the baseline (x-height 3): s and h 3 wide, m 5 wide (two arches).
CC_DIGITS.s = '000000011010110'; CC_DIGITS.h = '100100110101101'; CC_DIGITS.m = '0000000000111101010110101';
const CC_GLYPH_W = { m: 5 };
const ccGlyphW = ch => CC_GLYPH_W[ch] || 3;
const ccNumberWidth = (s, k = 1) => ([...s].reduce((a, ch) => a + ccGlyphW(ch) + 1, 0) - 1) * k;
/** s in the small font at (x, y) (glyphs 5 tall, 3 wide but m 5, 1 apart), each pixel k units; col a colour or a
 *  function of the character index (to colour parts); shadow: a dark copy one font pixel down-right, first. */
function ccNumber(g, s, x, y, col, shadow, k = 1) {
  if (shadow) ccNumber(g, s, x + k, y + k, shadow, null, k);   // a full font pixel down-right (half a pixel ate the strokes)
  let cx = 0;
  for (const [i, ch] of [...s].entries()) {
    const bits = CC_DIGITS[ch in CC_DIGITS ? ch : +ch], w = ccGlyphW(ch);
    g.fillStyle = typeof col === 'function' ? col(i) : col;
    for (let q = 0; q < w * 5; q++) if (bits[q] === '1') g.fillRect(x + (cx + (q % w)) * k, y + Math.floor(q / w) * k, k, k);
    cx += w + 1;
  }
}

/** The selected slot, snug on its socket: a steel ring over the brass bezel with an ember glow, and a small amber
 *  lamp on its top edge (the HUD's selection, without the frame around the socket). */
function ccSelection(g, x, y, toolbox = false) {
  // A toolbox-linked slot's selection glows the toolbox's red instead of amber (ring glow, inner line, lamp).
  const [halo, line, lamp, core] = toolbox
    ? ['rgba(230,40,40,1)', 'rgba(255,120,110,0.9)', 'rgba(240,60,50,0.85)', '#ffd0c8']
    : ['rgba(255,140,30,1)', 'rgba(255,200,110,0.9)', 'rgba(255,170,50,0.85)', '#fff1b0'];
  g.save(); g.shadowColor = halo; g.shadowBlur = 2.4;
  rrect(g, x + 0.75, y + 0.75, 16.5, 16.5, 1); g.strokeStyle = steel(g, y, y + 18); g.lineWidth = 0.9; g.stroke();
  g.restore();
  rim(g, () => rrect(g, x + 0.3, y + 0.3, 17.4, 17.4, 1.3), y, y + 18, '#f4f6f8', 0.3);
  rrect(g, x + 1.35, y + 1.35, 15.3, 15.3, 0.7); g.strokeStyle = line; g.lineWidth = 0.3; g.stroke();
  rrect(g, x + 7.2, y - 0.1, 3.6, 1.2, 0.4); g.fillStyle = steel(g, y - 0.1, y + 1.1); g.fill();
  glow(g, x + 9, y + 0.5, 1.8, lamp, core);
}
/** Pressed into the plate: a dark line with a faint lit lip under it (the inventory's engraved style). */
function ccEngrave(g, path, lw = 0.5) {
  g.save(); g.translate(0, 0.4); path(); g.strokeStyle = 'rgba(220,225,230,0.1)'; g.lineWidth = lw; g.stroke(); g.restore();
  path(); g.strokeStyle = 'rgba(0,0,0,0.6)'; g.lineWidth = lw; g.stroke();
}
/** A wing with nothing to show (vitals in creative, magic without mana mods): three meshing cogs pressed into the
 *  plate, centred on (cx, cy). */
function ccGearRow(g, cx, cy) {
  for (const [dx, r, t] of [[-11.2, 3.6, 8], [0, 5, 10], [11.2, 3.6, 8]]) {
    const x = cx + dx, a = dx ? Math.PI / t : 0;
    g.save(); g.translate(x, cy); g.rotate(a); g.translate(-x, -cy);
    ccEngrave(g, () => cogPath(g, x, cy, r, r * 0.76, t));
    g.restore();
    ccEngrave(g, () => circle(g, x, cy, r * 0.32));
  }
}
/** An empty meter-shaped groove pressed into the plate (a row with no meter). */
function ccGroove(g, x, y, w, h) {
  rrect(g, x, y, w, h, h / 2); g.fillStyle = 'rgba(0,0,0,0.22)'; g.fill();
  ccEngrave(g, () => rrect(g, x, y, w, h, h / 2), 0.4);
}
/** No dodge roll in the pack: one cog with a hex hub pressed into the plate where the dash would be. */
function ccDashFiller(g, cx, cy) {
  ccEngrave(g, () => cogPath(g, cx, cy, 6.2, 4.8, 10));
  ccEngrave(g, () => { g.beginPath(); for (let i = 0; i < 6; i++) { const a = Math.PI / 6 + i * Math.PI / 3; g.lineTo(cx + 2 * Math.cos(a), cy + 2 * Math.sin(a)); } g.closePath(); });
}
/** The action bar plate: blackened iron, wings either side of a raised hotbar section. Rounded everywhere: the outer
 *  corners by r, the climb from each wing to the section as a smooth S curve over `step`. */
function ccPlate(g, ears = {}) {
  const { x0, x1, wing, mid, step, r, sec, inner } = CC.plate, b = BOTTOM, E = CC.ext, k = step / 2;
  // One plate: a top edge of level runs [x from, x to, y] joined by S curves, rounded corners, its shadow, iron, a
  // lit brass rim. Returns its path.
  const plate = (runs) => {
    const xa = runs[0][0], xb = runs[runs.length - 1][1], top = Math.min(...runs.map(q => q[2]));
    const path = () => {
      const y0 = runs[0][2], yl = runs[runs.length - 1][2];
      g.beginPath(); g.moveTo(xa, b - r); g.lineTo(xa, y0 + r); g.quadraticCurveTo(xa, y0, xa + r, y0);
      runs.forEach(([, xe, y], i) => {
        if (i === runs.length - 1) return;
        const [xs, , ny] = runs[i + 1], kk = (xs - xe) / 2;
        g.lineTo(xe, y); g.bezierCurveTo(xe + kk, y, xs - kk, ny, xs, ny);
      });
      g.lineTo(xb - r, yl); g.quadraticCurveTo(xb, yl, xb, yl + r); g.lineTo(xb, b - r);
      g.quadraticCurveTo(xb, b, xb - r, b); g.lineTo(xa + r, b); g.quadraticCurveTo(xa, b, xa, b - r); g.closePath();
    };
    g.save(); g.translate(0, -0.6); path(); g.fillStyle = 'rgba(0,0,0,0.55)'; g.fill(); g.restore();
    path(); g.fillStyle = iron(g, top, b); g.fill(); ironSurface(g, path);
    return [path, top];
  };
  // A wing's raised tab (armor, breath) in a darker tone: from its outer S to the plate's inner edge.
  const tab = (path, o0, edge, dir) => {
    g.save(); path(); g.clip();
    const oW = o0 + dir * step;
    g.beginPath(); g.moveTo(oW, wing); g.bezierCurveTo(oW - dir * k, wing, o0 + dir * k, mid, o0, mid);
    g.lineTo(edge, mid); g.lineTo(edge, wing); g.closePath(); g.fillStyle = 'rgba(0,0,0,0.38)'; g.fill();
    g.restore();
  };
  const left = plate(ears.left ? [[x0, E.l[0] - step, wing], [E.l[0], -inner, mid]] : [[x0, -inner, wing]]);
  if (ears.left) tab(left[0], E.l[0], -inner, -1);
  const right = plate(ears.right ? [[inner, E.r[1], mid], [E.r[1] + step, x1, wing]] : [[inner, x1, wing]]);
  if (ears.right) tab(right[0], E.r[1], inner, 1);
  const centre = plate([[-sec, sec, mid]]);
  for (const [path, top] of [left, right, centre]) rim(g, path, top, b, P.brassHi, 0.35);
  // The joints: riveted steel straps across each gap (a third at band height where a raised tab meets the centre).
  for (const dir of [-1, 1]) {
    const xs = [dir * (sec - 2.5), dir * (inner + 2)], xa = Math.min(...xs), xb = Math.max(...xs);   // clear of the meters
    const ys = [34, 43.5].concat((dir < 0 ? ears.left : ears.right) ? [22.6] : []);
    for (const y of ys) {
      rrect(g, xa, y + 0.4, xb - xa, 3, 0.8); g.fillStyle = 'rgba(0,0,0,0.5)'; g.fill();
      const strap = () => rrect(g, xa, y, xb - xa, 3, 0.8);
      strap(); g.fillStyle = vgrad(g, y, y + 3, [[0, '#8b9198'], [0.3, '#4a4f55'], [1, '#1c1e21']]); g.fill();
      rim(g, strap, y, y + 3, '#d6dade', 0.25);
      for (const bx of [xa + 1.5, xb - 1.5]) hexBolt(g, bx, y + 1.5, 0.95);
    }
  }
}

/** The whole action bar for a state; a canvas of logical 344 x 50. */
function compactHud(s, font) {
  const [c, g] = hires(344, BOTTOM + TOPM);
  g.translate(CX, TOPM);
  ccGameText(g, s, font);   // the game's own text above the bar (not ours; shown for spacing)
  if (s.spectator) return c;   // spectator: vanilla draws no hotbar or vitals (only its own menu when opened)
  CC.hit = {};
  // Breath shows as the game's air row does: while under water (eyes in water) or while air is still below full
  // (catching your breath after surfacing). Not from the tank or air values alone: when the backtank empties the air
  // is still full for a moment, and the segment must not blink out.
  const tank = s.diving ? Math.max(0, s.tank) : 0, showArmor = !s.creative && s.armor > 0, showBreath = !s.creative && (s.underwater || s.air < 1);
  // The right extension: breath under water, else the Helium Flamingo charge, else an aircraft's health.
  const rightExt = showBreath ? 'breath' : s.flamingo != null ? 'flamingo' : s.aircraft ? 'hull' : null;
  const leftExt = showArmor || (!s.creative && s.mekasuit);
  ccPlate(g, { left: leftExt, right: !!rightExt });
  for (let i = 0; i < 9; i++) socket(g, ...CC.slot(i));
  // The offhand socket with a faint dotted shield, so the empty slot reads as the offhand.
  // Left-handed (Main Hand: Left): vanilla moves the offhand to the hotbar's right; the dash takes the left end.
  const [ox, oy] = s.leftHanded ? [143, CC.offhand[1]] : CC.offhand;
  socket(g, ox, oy);
  g.save(); g.translate(ox + 9, oy + 9.4); g.scale(1.25, 1.25); g.translate(-4.5, -4.5);
  shieldPath(g); g.setLineDash([0.4, 0.4]); g.strokeStyle = '#3a414b'; g.lineWidth = 0.4; g.stroke(); g.restore();
  // Experience: one glass pipe along the section's top edge (0 to 100 % in one run), the level in a small window at
  // its left end: the meters' small digits, a rounded rectangle like the plate (not a pill), inside the section's edge.
  const X = CC.xp, LK = 0.6, txt = String(Math.max(0, s.level)), tw = ccNumberWidth(txt, LK), ww = Math.max(tw + 3, 8);
  const wh = 5, wy = X.y + X.h / 2 - wh / 2;
  // The pipe is shared: casting a spell (Iron's), a camel's dash cooldown and a mount's jump charge take it over (vanilla
  // already swaps the XP bar for the jump bar), and the level window shows that action's icon instead of the number.
  const mounted = s.riding === 'camel' ? ccCamel : ccHorse;
  const take = s.cast != null ? [s.cast, MANA.irons, () => ccFlask(MANA.irons)]
    : s.riding === 'camel' && s.dash != null ? [s.dash, CC_COOLDOWN, ccCamel]
    : s.riding && s.jump != null ? [s.jump, CC_JUMP, mounted]
    : s.train != null ? [s.train, CC_TRAIN, () => ccModIcon('train')]      // Create's train HUD speed
    : s.aircraft && s.engine != null ? [s.engine, CC_ENGINE, () => ccModIcon('engine')] : null;   // aircraft engine
  const [xf, xl] = take ? take : [s.xp, ['#d8ff9a', '#5fd02a', '#1d5a08']];
  ccMeter(g, X.x0 + ww + 1, X.y, X.x1 - X.x0 - ww - 1, X.h, xf, xl, 18);
  rrect(g, X.x0, wy, ww, wh, 1.2); g.fillStyle = steel(g, wy, wy + wh); g.fill();
  rrect(g, X.x0 + 0.5, wy + 0.5, ww - 1, wh - 1, 0.8); g.fillStyle = '#060708'; g.fill();
  if (take) ccIcon(g, take[2](), X.x0 + ww / 2, wy + wh / 2, wh - 1.2);
  else ccNumber(g, txt, X.x0 + ww / 2 - tw / 2, wy + wh / 2 - 2.5 * LK, s.level > 0 ? '#80ff20' : '#3a5a2a', null, LK);
  // Meters: every one the same size, with the HUD's own sprite as its icon.
  const { rows, meterH: mh, meterW: mw, icon: is } = CC, L = CC.left, Rt = CC.right;
  // Icons: each sprite trimmed to its drawing, one height (is), centred on its meter's middle.
  // Numbers: value / max in the small digits at 0.6 size, centred in the glass; the value bright, "/max" dim. Shown for
  // the meter under the pointer (s.hover = 'L0', 'L1', 'Lear', 'R0', 'R1', 'Rear'), or all with s.numbers.
  const NK = 0.6;
  // A meter with its icon: box [x, y, w, h], icon centred at ix (height ih). The band extensions use slim pipes.
  const meterAt = (id, ix, ih, [x, y0, w, h], img, frac, liq, segs, value, max, opt = {}) => {
    const y = y0 + (opt.shake ? opt.shake[1] : 0);
    ccIcon(g, img, ix, y + h / 2, ih);
    ccMeter(g, x, y, w, h, frac, liq, segs);
    if (opt.after) opt.after(g, s, [x, y, w, h]);
    CC.hit[id] = [x, y, w, h];
    if ((s.numbers || s.hover === id) && value != null) {
      // Dim the liquid behind the digits; not on the slim extension pipes (thinner than the digits: the shadow does).
      if (h >= 4) { rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.fillStyle = 'rgba(4,5,6,0.6)'; g.fill(); }
      const v = typeof value === 'string' ? value : String(Math.round(value)), t = max == null ? v : `${v}/${max}`;
      ccNumber(g, t, x + w / 2 - ccNumberWidth(t, NK) / 2, y + h / 2 - 2.5 * NK, i => i < v.length ? '#ffffff' : '#9aa3ad',
        'rgba(0,0,0,0.85)', NK);
    }
  };
  // Extension pipes match the wing meters below them (same x, width, icon centre); the section's S divider runs
  // between their inner ends (±93 at the wing's level) and the XP band (±85).
  const E = CC.ext, extL = [L.x, E.y, mw, E.h], extR = [Rt.x, E.y, mw, E.h];
  const row = (side, k, img, frac, liq, segs, value, max, opt) => {
    const id = (side === L ? 'L' : 'R') + k;
    if (k === 'ear') return meterAt(id, side.iconC, E.icon, side === L ? extL : extR, img, frac, liq, segs, value, max, opt);
    meterAt(id, side.iconC, is, [side.x, rows[k], mw, mh], img, frac, liq, segs, value, max, opt);
  };
  // Vitals hide in creative (as vanilla). Health carries the heart statuses; riding swaps food for the mount's health.
  if (s.creative) ccGearRow(g, (CC.left.iconC - 3 + CC.left.x + CC.meterW) / 2, 39);   // the empty vitals wing
  if (!s.creative) {
  // Health against the real maximum (Health Boost and gear raise it; each segment is a tenth of it); absorption in
  // gold continues past the health, AppleSkin's regeneration preview (held food) pulses past that.
  const maxH = s.maxHealth || 20, hpNum = s.absorb ? `${Math.round(s.health)}+${Math.round(s.absorb)}` : s.health;
  row(L, 0, ccHeartIcon(s), s.health / maxH, ccHealthLiquid(s), 10, hpNum, maxH, { shake: ccShake(s),
    after: (g2, s2, box) => { ccAbsorb(g2, s2, box, maxH); ccHealthPreview(g2, s2, box, maxH); ccHealthFx(g2, s2, box); ccComfortFx(g2, s2, box); } });
  if (s.riding) row(L, 1, s.riding === 'camel' ? ccCamel() : ccHorse(), s.mount / s.mountMax, CC_MOUNT, 10, s.mount, s.mountMax);
  else row(L, 1, ccDrumstick(), s.food / 20, s.hunger ? LIQUID.hunger : s.nourished ? CC_GILDED : LIQUID.broth, 10, s.food, 20,
    { after: (g2, s2, box) => { ccSaturation(g2, s2, box); ccFoodPreview(g2, s2, box); ccExhaustion(g2, s2, box); } });
  }
  // Armor: the meter shows up to 20 (as vanilla's ten icons); the number gives the real value (up to 30).
  // With a MekaSuit the extension holds two half-height pipes: armor above, suit energy below (Mekanism's bar).
  if (leftExt && s.mekasuit) {
    const [ex, ey, ew, eh] = extL, hh = (eh - 0.4) / 2;
    ccIcon(g, armor('full'), L.iconC, ey + hh / 2, hh + 0.6); ccIcon(g, ccModIcon('bolt'), L.iconC, ey + eh - hh / 2, hh + 0.6);
    ccMeter(g, ex, ey, ew, hh, Math.min(1, s.armor / 20), ['#eef1f4', '#9aa0a6', '#3a3d41'], 10);
    ccMeter(g, ex, ey + hh + 0.4, ew, hh, s.mekasuit, CC_MEKA, 10);
    CC.hit.Lear = [ex, ey, ew, eh];
  } else if (showArmor) row(L, 'ear', armor('full'), Math.min(1, s.armor / 20), ['#eef1f4', '#9aa0a6', '#3a3d41'], 10, s.armor, s.armor > 20 ? 30 : 20);
  // Magic: a mana row per mod present (null: the mod is not in the pack). None: engraved gears fill the wing; one: the
  // other row is an empty groove, so the wing still reads as complete.
  const hasIrons = s.irons != null, hasArs = s.ars != null;
  if (!hasIrons && !hasArs) ccGearRow(g, (Rt.x + Rt.iconC + 3) / 2, 39);   // centred on the wing's meter + icon span
  else {
    if (hasIrons) row(Rt, 0, ccFlask(MANA.irons), s.irons, MANA.irons, 10, s.irons * 200, 200); else ccGroove(g, Rt.x, rows[0], mw, mh);
    if (hasArs) row(Rt, 1, ccGem(MANA.ars), s.ars, MANA.ars, 10, s.ars * 150, 150); else ccGroove(g, Rt.x, rows[1], mw, mh);
  }
  // Breath: while a Create backtank holds air, the segment is the tank alone (a darker copper meter, tank / capacity)
  // with its time left (m:ss, as Create's counter) always on it; the vanilla air stays full meanwhile. When the tank
  // is empty, the bubbles take over (draining under water, refilling after).
  const mmss = t => `${Math.floor(t / 60)}:${String(t % 60).padStart(2, '0')}`;
  if (showBreath && tank > 0) {
    const [ex, ey, ew, eh] = extR, tankTime = mmss(Math.ceil(tank));
    ccIcon(g, ccBacktank(), Rt.iconC, ey + eh / 2, E.icon);
    ccMeter(g, ex, ey, ew, eh, tank / s.tankMax, ['#c98a5a', '#7a3e1c', '#2e1206'], 10);
    CC.hit.Rear = [ex, ey, ew, eh];
    ccNumber(g, tankTime, ex + ew / 2 - ccNumberWidth(tankTime, NK) / 2, ey + eh / 2 - 2.5 * NK, '#ffffff', 'rgba(0,0,0,0.9)', NK);
  } else if (showBreath) row(Rt, 'ear', air(s.air < 0.15), s.air, LIQUID.air, 10, Math.ceil(s.air * 10), 10);
  else if (rightExt === 'flamingo') row(Rt, 'ear', ccModIcon('flamingo'), s.flamingo, CC_FLAMINGO, 10, Math.round(s.flamingo * 10), 10);
  else if (rightExt === 'hull') row(Rt, 'ear', ccModIcon('plane'), s.hull, CC_HULL, 10, Math.round(s.hull * 20), 20);
  // Dodge roll: our dash icon, its count in the tier colour with the mod's black outline.
  if (s.rolls == null) ccDashFiller(g, s.leftHanded ? -152 : 152, 39);   // no dodge roll mod: a pressed-in cog
  else {
  const [rx, ry] = s.leftHanded ? [-164.5, CC.roll[1]] : CC.roll, tier = rollTier(s.rolls);
  g.drawImage(rollIcon(tier), rx, ry, 18, 18);
  if (font) {
    const t = String(Math.floor(s.rolls)), tx = rx + 17, ty = ry + 9 - 3.5;   // beside the dash, on its centre line
    for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) mcText(g, font, t, tx + dx, ty + dy, 1, '#000000');
    mcText(g, font, t, tx, ty, 1, ROLL_TIERS[tier]);
  }
  }
  const [sx, sy] = CC.slot(s.slot);
  ccSlotMarks(g, s);
  ccAttackShade(g, s, [sx, sy]);
  const toolboxSel = (s.toolboxSlots || []).includes(s.slot);
  ccSelection(g, sx, sy, toolboxSel);
  if (toolboxSel) ccToolboxChips(g, s.slot, true);   // its chips over the ring, glowing
  ccInstruments(g, s);
  if (s.effects && s.effects.length) ccEffectsRail(g, s, s.effectIcons || {});
  return c;
}
