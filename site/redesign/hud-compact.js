// Compact HUD concept (preview only, not packed): one iron action bar instead of the floating bits. Shipping it needs
// Create Dark Inventory to cancel the vanilla and mod HUD layers and draw this layout itself (RenderGuiLayerEvent).
// Logical units, x centred on the screen (0), the action bar's bottom at y 50. Every meter is the same size, with the
// HUD's own sprite as its icon (heart, drumstick, shield, porthole bubble). The always-on meters sit in the wings
// (health and food left, the two manas right); armor and breath, which the game shows only when they apply (armor
// worn; under water), extend the raised hotbar band sideways over their wing only then, in a darker iron, as slim
// pipes level with the experience pipe. Uses sprites.js, meters.js (LIQUID),
// modhud.js (MANA, rollIcon, rollTier), mcfont.js.
const CX = 172, BOTTOM = 50, TOPM = 30;   // TOPM: room above the bar for the game's text (item name, action bar)
// Horizontal rhythm: one gap (GAP) between every group, out from the centred hotbar (sockets x -89..89): meters
// (icon 6, 2, meter 38: 46 wide), then the offhand socket / the dodge roll (18 wide), then the plate's edge.
const GAP = 4;
const CC = {
  plate: { x0: -165, x1: 165, wing: 26, mid: 19.4, step: 8, r: 2.5, sec: 85 },   // sec: the raised section's top edge, ±x
  slot: i => [-89 + 20 * i, 29],          // socket top left (item at +1); centred on x 0
  offhand: [-161, 29],
  rows: [32.5, 39.5], meterH: 4, meterW: 38, icon: 5.6,   // centred on the sockets' middle (y 38)
  left: { iconC: -136, x: -131 },         // vitals: health, food (armor extends the band above)
  right: { iconC: 136, x: 93 },           // magic: Iron's mana, Ars mana (breath extends the band above)
  ext: { l: [-140, -85], r: [85, 140], y: 21, h: 3.2, icon: 4.4 },   // band extensions; the outer S ends 1 px before the icon
  roll: [139.5, 29],                      // the dash's chevrons start at 143 (its art is inset 3.5)
  hit: {},                                // meter boxes by id, for the page's hover
  xp: { y: 21, h: 3.2, x0: -84, x1: 84 },   // 1.6 under the band's top, 1.8 above the wings   // the level window, then the pipe (inside the section)
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
const ccNumberWidth = (s, k = 1) => (s.length * 4 - 1) * k;
/** s in the 3 x 5 digits at (x, y), each pixel k units; col a colour or a function of the character index (to colour
 *  parts); shadow: a dark copy offset by half a pixel first (readable over liquid). */
function ccNumber(g, s, x, y, col, shadow, k = 1) {
  if (shadow) ccNumber(g, s, x + 0.5 * k, y + 0.5 * k, shadow, null, k);
  for (const [i, ch] of [...s].entries()) {
    const bits = CC_DIGITS[ch in CC_DIGITS ? ch : +ch];
    g.fillStyle = typeof col === 'function' ? col(i) : col;
    for (let q = 0; q < 15; q++) if (bits[q] === '1') g.fillRect(x + (i * 4 + (q % 3)) * k, y + Math.floor(q / 3) * k, k, k);
  }
}

/** The action bar plate: blackened iron, wings either side of a raised hotbar section. Rounded everywhere: the outer
 *  corners by r, the climb from each wing to the section as a smooth S curve over `step`. */
function ccPlate(g, ears = {}) {
  const { x0, x1, wing, mid, step, r } = CC.plate, b = BOTTOM;
  // The top edge as level runs [x from, x to, y], joined by S curves: the raised band (the hotbar section, plus any
  // extension over a wing) between the two wing runs.
  const E = CC.ext, sec = CC.plate.sec, b0 = ears.left ? E.l[0] : -sec, b1 = ears.right ? E.r[1] : sec;
  const runs = [[x0, b0 - step, wing], [b0, b1, mid], [b1 + step, x1, wing]];
  const top = Math.min(...runs.map(q => q[2]));
  const path = () => {
    const [, , y0] = runs[0], yl = runs[runs.length - 1][2];
    g.beginPath(); g.moveTo(x0, b - r); g.lineTo(x0, y0 + r); g.quadraticCurveTo(x0, y0, x0 + r, y0);
    runs.forEach(([, xe, y], i) => {
      if (i === runs.length - 1) return;
      const [xs, , ny] = runs[i + 1], k = (xs - xe) / 2;
      g.lineTo(xe, y); g.bezierCurveTo(xe + k, y, xs - k, ny, xs, ny);
    });
    g.lineTo(x1 - r, yl); g.quadraticCurveTo(x1, yl, x1, yl + r); g.lineTo(x1, b - r);
    g.quadraticCurveTo(x1, b, x1 - r, b); g.lineTo(x0 + r, b); g.quadraticCurveTo(x0, b, x0, b - r); g.closePath();
  };
  g.save(); g.translate(0, -0.6); path(); g.fillStyle = 'rgba(0,0,0,0.55)'; g.fill(); g.restore();
  path(); g.fillStyle = iron(g, top, b); g.fill(); ironSurface(g, path);
  // Extensions in a darker tone, bounded on the inside by the hotbar section's own S step (kept as the divider
  // leading into the experience band), on the outside by the extension's S down to the wing.
  const k = step / 2, ext = (o0, i0, dir) => {   // o0: outer run end, i0: the section's edge (±sec), dir: -1 left, 1 right
    const oW = o0 + dir * step, iW = i0 + dir * step;
    g.beginPath(); g.moveTo(oW, wing); g.bezierCurveTo(oW - dir * k, wing, o0 + dir * k, mid, o0, mid);
    g.lineTo(i0, mid); g.bezierCurveTo(i0 + dir * k, mid, iW - dir * k, wing, iW, wing); g.closePath();
    g.fillStyle = 'rgba(0,0,0,0.38)'; g.fill();
    g.beginPath(); g.moveTo(i0, mid); g.bezierCurveTo(i0 + dir * k, mid, iW - dir * k, wing, iW, wing);
    g.strokeStyle = 'rgba(0,0,0,0.7)'; g.lineWidth = 0.5; g.stroke();
    g.save(); g.translate(-dir * 0.4, 0); g.strokeStyle = 'rgba(244,216,152,0.35)'; g.lineWidth = 0.25; g.stroke(); g.restore();
  };
  g.save(); path(); g.clip();
  if (ears.left) ext(E.l[0], -sec, -1);
  if (ears.right) ext(E.r[1], sec, 1);
  g.restore();
  rim(g, path, top, b, P.brassHi, 0.35);
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
  ccPlate(g, { left: showArmor, right: showBreath });
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
    : s.riding && s.jump != null ? [s.jump, CC_JUMP, mounted] : null;
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
      // Dim the liquid behind the digits.
      rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.fillStyle = 'rgba(4,5,6,0.6)'; g.fill();
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
  if (!s.creative) {
  // Health against the real maximum (Health Boost and gear raise it; each segment is a tenth of it); absorption in
  // gold continues past the health, AppleSkin's regeneration preview (held food) pulses past that.
  const maxH = s.maxHealth || 20, hpNum = s.absorb ? `${Math.round(s.health)}+${Math.round(s.absorb)}` : s.health;
  row(L, 0, ccHeartIcon(s), s.health / maxH, ccHealthLiquid(s), 10, hpNum, maxH, { shake: ccShake(s),
    after: (g2, s2, box) => { ccAbsorb(g2, s2, box, maxH); ccHealthPreview(g2, s2, box, maxH); ccHealthFx(g2, s2, box); } });
  if (s.riding) row(L, 1, s.riding === 'camel' ? ccCamel() : ccHorse(), s.mount / s.mountMax, CC_MOUNT, 10, s.mount, s.mountMax);
  else row(L, 1, ccDrumstick(), s.food / 20, s.hunger ? LIQUID.hunger : LIQUID.broth, 10, s.food, 20,
    { after: (g2, s2, box) => { ccSaturation(g2, s2, box); ccFoodPreview(g2, s2, box); ccExhaustion(g2, s2, box); } });
  }
  // Armor: the meter shows up to 20 (as vanilla's ten icons); the number gives the real value (up to 30).
  if (showArmor) row(L, 'ear', armor('full'), Math.min(1, s.armor / 20), ['#eef1f4', '#9aa0a6', '#3a3d41'], 10, s.armor, s.armor > 20 ? 30 : 20);
  row(Rt, 0, ccFlask(MANA.irons), s.irons, MANA.irons, 10, s.irons * 200, 200);
  row(Rt, 1, ccGem(MANA.ars), s.ars, MANA.ars, 10, s.ars * 150, 150);
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
  // Dodge roll: our dash icon, its count in the tier colour with the mod's black outline.
  const [rx, ry] = s.leftHanded ? [-164.5, CC.roll[1]] : CC.roll, tier = rollTier(s.rolls);
  g.drawImage(rollIcon(tier), rx, ry, 18, 18);
  if (font) {
    const t = String(Math.floor(s.rolls)), tx = rx + 17, ty = ry + 9 - 3.5;   // beside the dash, on its centre line
    for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) mcText(g, font, t, tx + dx, ty + dy, 1, '#000000');
    mcText(g, font, t, tx, ty, 1, ROLL_TIERS[tier]);
  }
  const [sx, sy] = CC.slot(s.slot);
  ccAttackShade(g, s, [sx, sy]);
  g.drawImage(selection(), sx - 3, sy - 3, 24, 23);
  return c;
}
