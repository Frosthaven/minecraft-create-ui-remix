// Compact HUD concept (preview only, not packed): one iron action bar instead of the floating bits. Shipping it needs
// haven-create-ui-ux to cancel the vanilla and mod HUD layers and draw this layout itself (RenderGuiLayerEvent).
// Logical units, x centred on the screen (0), the action bar's bottom at y 50. Every meter is the same size, with the
// HUD's own sprite as its icon (heart, drumstick, shield, porthole bubble). The always-on meters sit in the wings
// (health and food left, the two manas right); armor and breath, which the game shows only when they apply (armor
// worn; under water), extend the raised hotbar band sideways over their wing only then, in a darker iron, as slim
// pipes level with the experience pipe. Uses sprites.js, meters.js (LIQUID),
// modhud.js (MANA, rollIcon, rollTier), mcfont.js.
const CX = 222, BOTTOM = 50, TOPM = 90;
/** The effects' numbers: clear light tints (buff green, debuff warm red, gear cyan), drawn inside a solid black
 *  outline so they read on any background (a coloured shadow blurred them together in game). The export's digit rows. */
const CC_EFFECT_INK = { buff: '#8ef08a', debuff: '#ff8a80', gear: '#7fe8ff' };
/** The liquids a step brighter, to sit with Minecraft's lighter look: the mid tone toward white, the dark tone toward
 *  the mid (hue and the light-to-dark depth kept). The mana pools more, up to the cast bar's overall value. */
const ccLift = (liq, mid = 0.1, dark = 0.3) => [liq[0], mixHex(liq[1], '#ffffff', mid), mixHex(liq[2], liq[1], dark)];
for (const k of Object.keys(LIQUID)) LIQUID[k] = ccLift(LIQUID[k]);
MANA.irons = ccLift(MANA.irons, 0.16, 0.5); MANA.ars = ccLift(MANA.ars, 0.16, 0.5);
const CC_XP = ccLift(['#d8ff9a', '#5fd02a', '#1d5a08']), CC_TANK_RING = ccLift(['#ffd9b0', '#c98a5a', '#5a2814']);
const CC_TANK = ['#c98a5a', '#7a3e1c', '#2e1206'];   // Create's backtank: copper   // TOPM: room above the bar for the game's text (item name, action bar)
// Three floating plates: the centre (hotbar and XP band, -91..91) and a wing plate each side (±96..±165), 5 px apart. In a wing, out from the gap: meter (32), 2, icon (6), 4, the offhand
// socket / the dodge roll (18), 4, the plate's edge.
const GAP = 4;
/** Bar mode's meter blocks drawn S x around (±anchor, by) (twice the hotbar's scale, like the orbs): the left block
 *  spans -165..-101 and 13.8..45.8 (mid 29.8); each wing is one plate at top's height out to its edge, the block
 *  centred in it; outer: the blocks' outer edge plus 2. The end items (offhand, dial) centre on mid. */
const CC_WING = { S: 2, anchor: 97, by: 49, top: 9.6, mid: 29.8, outer: 167 };
/** Where the side bars attach (± from the centre), between the centre plate (±91) and the wings. */
const CC_SIDE = 93.5;
/** The wings' base size: their designed size times this is Side bars size 1.0 (the config multiplies it). */
const CC_WING_BASE = 0.85;
const ccSideK = s => (s.side || 1) * CC_WING_BASE;
/** Draws f with a side bar's size k, scaled around (sign CC_SIDE, the bar's bottom). */
function ccSide(g, sign, k, f) { if (k === 1) return f(); ccScaled(g, sign * CC_SIDE, BOTTOM, k, f); }
/** A point of a side bar after its scaling: x, y. */
const ccSideX = (x, k) => Math.sign(x) * CC_SIDE + (x - Math.sign(x) * CC_SIDE) * k, ccSideY = (y, k) => BOTTOM + (y - BOTTOM) * k;
/** Draws f clipped to x0..x1 (logical). */
function ccClipX(g, x0, x1, f) { g.save(); g.beginPath(); g.rect(x0, -1e3, x1 - x0, 2e3); g.clip(); f(); g.restore(); }
const CC = {
  plate: { x0: -165, x1: 165, wing: 28, mid: 24.4, step: 8, r: 2.5, sec: 91, inner: 96 },   // sec: the centre plate's half width; inner: the wings' inner edges
  slot: i => [-89 + 20 * i, 30],          // socket top left; items at (-88 + 20 i, 31): vanilla's exact item spots
  offhand: [-161, 30],
  rows: [31.4, 37.4, 43.4], meterH: 4, meterW: 32, icon: 5.6,   // three rows a wing, centred on the sockets' middle (y 39);
  // the gap under food holds AppleSkin's exhaustion line
  left: { iconC: -136, x: -131 },         // vitals: health, food (armor extends the band above)
  right: { iconC: 136, x: 99 },           // magic: Iron's mana, Ars mana (breath extends the band above)
  ext: { l: [-133, -96], r: [96, 133], y: 25, h: 3.2, icon: 4.4 },   // a wing's raised tab: icon to the plate's inner edge   // band extensions; the outer S ends 1 px before the icon
  roll: [139.5, 30],                      // the dash's chevrons start at 143 (its art is inset 3.5)
  hit: {},                                // meter boxes by id, for the page's hover
  xp: { y: 25.6, h: 3, x0: -88.5, x1: 88.5 },   // the pipe across nearly the centre plate's full width (±91), the level
  // window centred on it
};

/** A pill-shaped glass meter: steel casing, dark glass, liquid (light top, dark bottom) to frac, segment lines. */
function ccMeter(g, x, y, w, h, frac, liq, segs = 0, from = 'left', style) {
  const r = h / 2;
  // Satin, not chrome: a darker steel casing, and the liquid's highlight halfway to its body colour.
  rrect(g, x, y, w, h, r); g.fillStyle = vgrad(g, y, y + h, [[0, '#9ea4aa'], [0.25, '#6c7278'], [0.65, '#3a3e43'], [1, '#141619']]); g.fill();
  rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, r - 0.5); g.fillStyle = '#060708'; g.fill();
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, r - 0.5); g.clip();
  const fw = (w - 1) * Math.max(0, Math.min(1, frac)), fx = from === 'left' ? x + 0.5 : x + w - 0.5 - fw;
  g.fillStyle = vgrad(g, y + 0.5, y + h - 0.5, [[0, mixHex(liq[0], liq[1], 0.5)], [0.45, liq[1]], [1, liq[2]]]); g.fillRect(fx, y, fw, h);
  if (style && fw > 0) ccMeterTexture(g, fx, y, fw, h, liq, style);   // the orbs' liquid looks (cloud, broth, galaxy)
  g.fillStyle = 'rgba(0,0,0,0.55)';
  for (let k = 1; k < segs; k++) g.fillRect(x + 0.5 + (w - 1) * k / segs - 0.15, y, 0.3, h);

  g.restore();
}
/** The orbs' liquid looks on a meter's fill (x, y, w, h): 'cloud' billows (health, absorption, the mount), 'broth' the
 *  same with a few bubbles (food), 'mana' a galaxy (nebula mask and stars). */
function ccMeterTexture(g, x, y, w, h, liq, style) {
  const area = () => { g.beginPath(); g.rect(x, y, w, h); };
  g.save(); area(); g.clip();
  if (style === 'mana') {
    g.globalCompositeOperation = 'multiply'; g.globalAlpha = 0.5;   // half the orbs' mask: a thin meter reads as noise at full contrast
    const pat = g.createPattern(ccNebula(), 'repeat');
    pat.setTransform(new DOMMatrix().translateSelf(x * 3, y * 5).rotateSelf(60).scaleSelf(0.05, 0.05)); g.fillStyle = pat; area(); g.fill();
    g.globalCompositeOperation = 'source-over'; g.globalAlpha = 1;
    for (let i = 0; i < Math.round(w / 3); i++) {
      const hh = Math.sin((x + i * 12.9898) * 78.233) * 43758.5453, u = hh - Math.floor(hh), v = hh * 7.13 - Math.floor(hh * 7.13);
      g.fillStyle = i % 5 ? 'rgba(220,230,255,0.7)' : 'rgba(255,255,255,0.95)'; g.fillRect(x + u * w, y + v * h, 0.25, 0.25);
    }
  } else {
    surface(g, area, TEX.blotch, 0.45 * ORB.tex, 'overlay'); surface(g, area, TEX.rock, 0.25 * ORB.tex, 'soft-light');
    if (style === 'broth') for (let i = 0; i < Math.round(w / 5); i++) {
      const hh = Math.sin((x + i * 9.17) * 33.1) * 43758.5453, u = hh - Math.floor(hh);
      circle(g, x + u * w, y + h / 2, 0.35); g.strokeStyle = 'rgba(255,230,190,0.55)'; g.lineWidth = 0.15; g.stroke();
    }
  }
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
/** Text colours by hue: bright, leaning toward a hue (value and max alike). */
const ccTint = hue => mixHex(hue, '#ffffff', 0.6);
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
function ccGroove(g, x, y, w, h) {   // a quiet recess (an empty row), darker, no lit rim
  rrect(g, x, y, w, h, h / 2); g.fillStyle = 'rgba(0,0,0,0.32)'; g.fill();
  rrect(g, x, y, w, h, h / 2); g.strokeStyle = 'rgba(0,0,0,0.35)'; g.lineWidth = 0.3; g.stroke();
}
/** No dodge roll in the pack: one cog with a hex hub pressed into the plate where the dash would be. */
function ccDashFiller(g, cx, cy) {
  ccEngrave(g, () => cogPath(g, cx, cy, 6.2, 4.8, 10));
  ccEngrave(g, () => { g.beginPath(); for (let i = 0; i < 6; i++) { const a = Math.PI / 6 + i * Math.PI / 3; g.lineTo(cx + 2 * Math.cos(a), cy + 2 * Math.sin(a)); } g.closePath(); });
}
/** Where each wing ends: out from the orb's housing (or the meter icons) a 4 px gap, the end item (the offhand slot, 18
 *  wide, or the dash dial / its cog, 14.6), then 4 px of plate. Left-handed mirrors which end holds which. Returns
 *  { x0, x1, offhand (slot's left x), dash (dial centre x) }. */
function ccEnds(s) {
  // Orb mode: the item tucks in close to the housing's curve (1.5 px) with 3 px of plate past it; meter mode: 4 and 4.
  // The dial's width: its housing's reach at the plate's edge (the dial and its housing scale with the orbs).
  const G = s.orbs ? 1.5 : 4, E = s.orbs ? 3 : 4, inner = s.orbs ? ORB.rx + ORB_H() : CC_WING.outer, slotW = 18;
  // Orbs: the dial's housing reach at the plate's edge; bars: the dial alone (inside the wing's plate).
  const dialW = s.orbs ? 2 * ccDomeHalf(ccDialR(), CC_DIAL.y) : 2 * (CC_DASH.r + CC_DASH.B) * CC_DIAL.S;
  const left = inner + G, right = inner + G;
  const leftW = s.leftHanded ? dialW : slotW, rightW = s.leftHanded ? slotW : dialW;
  const span = Math.max(left + leftW, right + rightW);   // each wing's extent (the wider one's): the dial centres in its space
  // Each end item centred in its wing's end space (the wider wing's), so neither end leaves dead plate.
  const leftItem = -(left + span) / 2, rightItem = (right + span) / 2;
  const dialC = s.leftHanded ? -(left + span) / 2 : (right + span) / 2;
  return {
    // Both wings the same width (the wider one's), so the bar reads symmetric about the hotbar.
    x0: -span - E, x1: span + E,
    offhand: (s.leftHanded ? rightItem : leftItem) - slotW / 2,
    dash: dialC,
  };
}
/** The level window: k the digits' size, h its height for digits; iconW x iconH while an icon (size icon) shows. */
const CC_LEVEL = { k: 0.85, h: 6.6, iconW: 9, iconH: 7, icon: 5 };   // k: readable at GUI scale 2-3
function ccLevelWindow(g, x, y, w, h) {
  rrect(g, x, y, w, h, 1.2); g.fillStyle = steel(g, y, y + h); g.fill();
  rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, 0.8); g.fillStyle = '#060708'; g.fill();
}
/** The whole action bar for a state; a canvas of logical 2 CX x (BOTTOM + TOPM). */
function compactHud(s, font) {
  const [c, g] = hires(2 * CX, BOTTOM + TOPM);
  g.translate(CX, TOPM);
  if ((s.barScale || 1) !== 1) { g.translate(0, BOTTOM); g.scale(s.barScale, s.barScale); g.translate(0, -BOTTOM); }   // the action bar size
  ccGameText(g, s, font);   // the game's own text above the bar (not ours; shown for spacing)
  if (s.spectator) return c;   // spectator: vanilla draws no hotbar or vitals (only its own menu when opened)
  CC.hit = {};
  // Breath shows as the game's air row does: while under water (eyes in water) or while air is still below full
  // (catching your breath after surfacing). Not from the tank or air values alone: when the backtank empties the air
  // is still full for a moment, and the segment must not blink out.
  const tank = ccTankLeft(s), showArmor = !s.creative && s.armor > 0, showBreath = ccBreathShown(s);
  const ends = CC.lay = ccEnds(s);
  // The side bars at their own size (CC_WING_BASE x the config's "side bars size", s.side): scaled around (±CC_SIDE, the bottom), so
  // they stay attached to the centre plate. The plate is drawn per part, each clipped to its side of the anchor.
  const offSide = s.leftHanded ? 1 : -1, side = (sign, f) => ccSide(g, sign, ccSideK(s), f);
  const plateAll = only => () => {
    ccPlate(g, { x0: ends.x0, x1: ends.x1, gradTop: ccPlateGradTop(s), raise: s.orbs ? null : CC_WING.top, only });
    if (only === 'wings' && s.orbs) { ccHousing(g, ORB.lx, ORB.y, 1); ccHousing(g, ORB.rx, ORB.y, -1); }   // as the plate texture has them; the joint fillets their inner sides
  };
  // The centre up to the joints, each wing from the anchor at its size, then the joints closing them into one casting.
  ccClipX(g, -CC_SIDE, CC_SIDE, plateAll('centre'));
  side(-1, () => ccClipX(g, -1e3, -CC_SIDE, plateAll('wings')));
  side(1, () => ccClipX(g, CC_SIDE, 1e3, plateAll('wings')));
  for (const sign of [-1, 1]) ccJoint(g, s, sign, ccSideK(s), ccPlateGradTop(s));
  // Orbs: the dash dial in its own housing at the wing's end, the plate's gradient (bars: inside the tall wing plate).
  if (s.orbs) side(-offSide, () => ccHousing(g, ends.dash, CC_DIAL.y, 0, ccDialR(), ORB.y - ORB_H(), (CC_DASH.r + CC_DASH.B) * CC_DIAL.S, CC_DIAL.S));
  for (let i = 0; i < 9; i++) socket(g, ...CC.slot(i));
  // The offhand socket with a faint dotted shield, so the empty slot reads as the offhand.
  // Left-handed (Main Hand: Left): vanilla moves the offhand to the hotbar's right; the dash takes the left end.
  const [ox, oy] = [ends.offhand, s.orbs ? CC.offhand[1] : CC_WING.mid - 9];   // bars: centred on the meter block
  side(offSide, () => {
    socket(g, ox, oy);
    g.save(); g.translate(ox + 9, oy + 9.4); g.scale(1.25, 1.25); g.translate(-4.5, -4.5);
    shieldPath(g); g.setLineDash([0.4, 0.4]); g.strokeStyle = '#3a414b'; g.lineWidth = 0.4; g.stroke(); g.restore();
  });
  // Experience: one glass pipe along the section's top edge (0 to 100 % in one run, behind the window); the level in a
  // small window centred on the pipe.
  // The pipe is shared: casting a spell (Iron's), a camel's dash cooldown and a mount's jump charge take it over (vanilla
  // already swaps the XP bar for the jump bar), and the level window shows that action's icon instead of the number.
  const X = CC.xp, mounted = s.riding === 'camel' ? ccCamel : ccHorse;
  const take = s.cast != null ? [s.cast, MANA.irons, () => ccFlask(MANA.irons)]
    : s.riding === 'camel' && s.dash != null ? [s.dash, CC_COOLDOWN, ccCamel]
    : s.riding && s.jump != null ? [s.jump, CC_JUMP, mounted]
    : s.train != null ? [s.train, CC_TRAIN, () => ccModIcon('train')]      // Create's train HUD speed
    : s.aircraft && s.engine != null ? [s.engine, CC_ENGINE, () => ccModIcon('engine')] : null;   // aircraft engine
  const [xf, xl] = take ? take : [s.xp, CC_XP];
  ccMeter(g, X.x0, X.y, X.x1 - X.x0, X.h, xf, xl, 18, 'left', !take ? 'cloud' : s.cast != null ? 'mana' : undefined);   // XP: health's billows; a cast is mana: the galaxy
  // The window, centred on the pipe (which runs on behind it): small for the level's digits, larger while an icon
  // shows (so the icon reads).
  const LK = CC_LEVEL.k, txt = String(Math.max(0, s.level)), tw = ccNumberWidth(txt, LK);
  const [ww, wh] = take ? [CC_LEVEL.iconW, CC_LEVEL.iconH] : [Math.max(tw + 3.6, 8.5), CC_LEVEL.h];
  const wy = X.y + X.h / 2 - wh / 2, mid = wy + wh / 2;
  ccLevelWindow(g, -ww / 2, wy, ww, wh);
  if (take) ccIcon(g, take[2](), 0, mid, CC_LEVEL.icon);
  else ccNumber(g, txt, -tw / 2, mid - 2.5 * LK, s.level > 0 ? '#80ff20' : '#3a5a2a', 'rgba(0,0,0,0.9)', LK);
  // Meters: every one the same size, with the HUD's own sprite as its icon.
  const { rows, meterH: mh, meterW: mw, icon: is } = CC, L = CC.left, Rt = CC.right;
  // Icons: each sprite trimmed to its drawing, one height (is), centred on its meter's middle.
  // Numbers: value / max in the small digits at 0.6 size, centred in the glass; the value bright, "/max" dim. Shown for
  // the meter under the pointer (s.hover = 'L0', 'L1', 'Lear', 'R0', 'R1', 'Rear'), or all with s.numbers.
  const NK = 0.6;
  // A meter with its icon: box [x, y, w, h], icon centred at ix (height ih). The band extensions use slim pipes.
  const meterAt = (id, ix, ih, [x, y0, w, h], img, frac, liq, segs, value, max, opt = {}) => {
    const y = y0 + (opt.shake ? opt.shake[1] : 0);
    // Meter mode carries no icons (space): the meters' colours and their order say what they are.
    ccMeter(g, x, y, w, h, frac, liq, segs, 'left', opt.style);
    if (opt.after) opt.after(g, s, [x, y, w, h]);
    const T = CC.hitT;   // drawn scaled (bar mode's wings): the hover box on the page in bar units
    CC.hit[id] = T ? [T.ax + (x - T.ax) * T.S, T.ay + (y - T.ay) * T.S, w * T.S, h * T.S] : [x, y, w, h];
    if ((s.numbers || s.hover === id) && value != null) {
      // Dim the liquid behind the digits; not on the slim extension pipes (thinner than the digits: the shadow does).
      if (h >= 4) { rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.fillStyle = 'rgba(4,5,6,0.6)'; g.fill(); }
      const v = typeof value === 'string' ? value : String(Math.round(value)), t = max == null ? v : `${v}/${max}`;
      ccNumber(g, t, x + w / 2 - ccNumberWidth(t, NK) / 2, y + h / 2 - 2.5 * NK, ccTint(liq[1]),
        'rgba(0,0,0,0.85)', NK);
    }
  };
  const row = (side, k, img, frac, liq, segs, value, max, opt) =>
    meterAt((side === L ? 'L' : 'R') + k, side.iconC, is, [side.x, rows[k], mw, mh], img, frac, liq, segs, value, max, opt);
  if (s.orbs) {   // orbs in place of the wing meters (hud-compact-orbs.js), each side at the side bars' size
    side(-1, () => ccClipX(g, -1e3, -CC_SIDE, () => ccOrbs(g, s)));
    side(1, () => ccClipX(g, CC_SIDE, 1e3, () => ccOrbs(g, s)));
  }
  else {
  const wing = (sign, f) => { const ax = sign * CC_WING.anchor, ay = CC_WING.by, S = CC_WING.S;   // a wing's meter block, S x
    CC.hitT = { ax, ay, S }; side(sign, () => ccScaled(g, ax, ay, S, f)); CC.hitT = null; };
  wing(-1, () => {
  // Vitals hide in creative (as vanilla). Health carries the heart statuses; riding swaps food for the mount's health.
  if (s.creative) ccGearRow(g, L.x + mw / 2, (rows[0] + rows[2] + mh) / 2);   // the empty vitals wing: centred on its three rows
  if (!s.creative) {
  // Health against the real maximum (Health Boost and gear raise it; each segment is a tenth of it); absorption in
  // gold continues past the health, AppleSkin's regeneration preview (held food) pulses past that.
  const maxH = s.maxHealth || 20, hpNum = s.absorb ? `${Math.round(s.health)}+${Math.round(s.absorb)}` : s.health;
  row(L, 0, ccHeartIcon(s), s.health / maxH, ccHealthLiquid(s), 10, hpNum, maxH, { style: 'cloud',
    after: (g2, s2, box) => { ccAbsorb(g2, s2, box, maxH); ccHealthPreview(g2, s2, box, maxH); ccHealthFx(g2, s2, box); ccComfortFx(g2, s2, box); } });
  if (s.riding) row(L, 1, s.riding === 'camel' ? ccCamel() : ccHorse(), s.mount / s.mountMax, CC_MOUNT, 10, s.mount, s.mountMax, { style: 'cloud' });
  else row(L, 1, ccDrumstick(), s.food / 20, s.hunger ? LIQUID.hunger : LIQUID.broth, 10, s.food, 20,
    { style: 'broth', after: (g2, s2, box) => { ccSaturation(g2, s2, box); ccFoodFx(g2, s2, box); ccFoodPreview(g2, s2, box); ccExhaustion(g2, s2, box); } });
  }
  // Armor (row 2): the meter shows up to 20 (as vanilla's ten icons); the number gives the real value (up to 30).
  if (showArmor) row(L, 2, armor('full'), Math.min(1, s.armor / 20), ['#eef1f4', '#9aa0a6', '#3a3d41'], 10, s.armor, s.armor > 20 ? 30 : 20);
  });
  wing(1, () => {
  // The right wing: row 0 breath (the backtank while it holds air), else the Helium Flamingo's charge, else an
  // aircraft's health, else an empty groove; rows 1 and 2 the mana pools, the primary (the setting) on row 1. One mana
  // mod: its pool on row 1, row 2 a groove; none: engraved gears fill rows 1 and 2.
  const pools = [['irons', s.irons, MANA.irons, 200, ccFlask], ['ars', s.ars, MANA.ars, 150, ccGem]].filter(p => p[1] != null);
  if (s.manaPrimary === 'ars') pools.reverse();
  if (!pools.length) ccGearRow(g, Rt.x + mw / 2, (rows[1] + rows[2] + mh) / 2);   // centred on the two mana rows
  else for (const k of [1, 2]) {
    const p = pools[k - 1];
    if (p) row(Rt, k, p[4](p[2]), p[1], p[2], 10, p[1] * p[3], p[3], { style: 'mana' }); else ccGroove(g, Rt.x, rows[k], mw, mh);
  }
  // Breath: while a Create backtank holds air, the row is the tank alone (a darker copper meter, tank / capacity) with
  // its time left (m:ss, as Create's counter) always on it; the vanilla air stays full meanwhile. When the tank is
  // empty, the bubbles take over (draining under water, refilling after).
  const mmss = t => `${Math.floor(t / 60)}:${String(t % 60).padStart(2, '0')}`;
  if (showBreath && tank > 0) {
    const tankTime = mmss(Math.ceil(tank));
    row(Rt, 0, ccBacktank(), tank / s.tankMax, CC_TANK, 10, null, null);
    ccNumber(g, tankTime, Rt.x + mw / 2 - ccNumberWidth(tankTime, NK) / 2, rows[0] + mh / 2 - 2.5 * NK, ccTint('#d9874a'), 'rgba(0,0,0,0.9)', NK);
  } else if (showBreath) row(Rt, 0, air(s.air < 0.15), s.air, LIQUID.air, 10, Math.ceil(s.air * 10), 10);
  else if (s.flamingo != null) row(Rt, 0, ccModIcon('flamingo'), s.flamingo, CC_FLAMINGO, 10, Math.round(s.flamingo * 10), 10);
  else if (s.aircraft) row(Rt, 0, ccModIcon('plane'), s.hull, CC_HULL, 10, Math.round(s.hull * 20), 20);
  else ccGroove(g, Rt.x, rows[0], mw, mh);
  });
  }
  // Dodge roll: a small dial like the orbs (stone rim, dark rivets, glass) with the dash chevrons behind the glass in
  // the tier colour and a ring segment per charge, lit as charges are ready. Centred in its space at the wing's end.
  // No dodge roll mod: a pressed-in cog there instead.
  const dy = s.orbs ? CC_DIAL.y : CC_WING.mid;   // orbs: in its housing; bars: centred on the meter block
  side(-offSide, () => ccScaled(g, ends.dash, dy, CC_DIAL.S, () => {   // the dial at the orbs' scale
    if (s.rolls == null) ccDashFiller(g, ends.dash, dy);
    else ccDashDial(g, ends.dash, dy, s.rolls, s.rollCount || 2);
  }));
  const [sx, sy] = CC.slot(s.slot);
  ccSlotMarks(g, s);
  ccAttackShade(g, s, [sx, sy]);
  const toolboxSel = (s.toolboxSlots || []).includes(s.slot);
  ccSelection(g, sx, sy, toolboxSel);
  if (toolboxSel) ccToolboxChips(g, s.slot, true);   // its chips over the ring, glowing
  ccInstruments(g, s);
  side(offSide, () => ccHardcore(g, s));
  if ((s.effects && s.effects.length) || (!s.creative && s.mekasuit)) ccEffectsRail(g, s, s.effectIcons || {});
  return c;
}

/** Create backtank air shown (seconds): while diving, or while a Create Jetpack drains it in flight (the game: tank air
 *  fell in the last 3 s), else 0. */
function ccTankLeft(s) { return s.diving || s.jetpack ? Math.max(0, s.tank) : 0; }
/** The breath / tank segment shows as the game's air row (under water, or air below full) and while a jetpack drains
 *  the backtank. */
function ccBreathShown(s) { return !s.creative && (s.underwater || s.air < 1 || (s.jetpack && s.tank > 0)); }
