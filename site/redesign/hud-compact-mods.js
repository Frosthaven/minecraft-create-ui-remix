// Compact HUD: mod elements that sit at the hotbar or change the vanilla rows in this pack (read from each mod's
// bytecode and the instance config; see CLAUDE.md), mapped onto the compact action bar. Uses sprites.js, meters.js,
// hud-compact.js (ccMeter, ccIcon, CC). Elsewhere-on-screen mod HUDs (corners, crosshair, tooltips) are untouched.
//   Farmer's Delight: Nourishment gilds the food broth (it draws gilded shanks over vanilla's food); Comfort runs a slow
//     warm sheen along health (its sheen over the hearts: saturation 0, hurt, no Regeneration).
//   Mekanism: the MekaSuit energy bar (above armor in vanilla's left column) is a cyan status in the effects rail
//   (hud-compact-effects.js): the worn piece's icon, the border draining with the energy, the percent under it.
//   Immersive Aircraft: the vehicle's health (a heart row on the air row) takes the right extension; the engine gauge
//     (a dial over the XP bar) takes the XP pipe, an engine icon in the level window.
//   Create: the train HUD's speed bar (over the XP bar) takes the XP pipe; toolbox-linked slots get red liquid corner chips (top right, bottom left),
//     the schematic tool's slot a cyan frame (both draw over vanilla's slots, so our sockets keep vanilla's positions).
//   Artifacts: Helium Flamingo charge (a row on the right column) takes the right extension; item cooldown icons (right
//     of the hotbar in vanilla, over our wing) and Immersive Armors' steampunk clock and compass (either side of the
//     hotbar, over our wings) move to a small instrument plate above the bar, shown only while there is one.
//   Haven Link (our server mod): web map viewers following you; Haven Link tells you in chat ("[Web] Name is following
//     you on the web map" / "... stopped following ..."); the client counts them: a ghost and the count on the plate.

const CC_HULL = ['#e8eef4', '#8a9aa8', '#2a323a'];        // aircraft hull
const CC_ENGINE = ccLift(['#ffd2a0', '#e0782a', '#5a2408']);      // engine power
const CC_TRAIN = ccLift(['#cfe8ff', '#4a8ad0', '#12305a']);       // train speed
const CC_FLAMINGO = ['#ffd6e6', '#f06aa0', '#6a1a3a'];    // Helium Flamingo
const CC_GILDED = ['#fff2b0', '#e0b040', '#6a4a08'];      // Nourishment

/** Small flat icons in the HUD's style (9 x 9 canvases; ccIcon trims and scales them). */
function ccModIcon(kind) {
  const [c, g] = hires(9, 9);
  const fill = (col) => { g.fillStyle = col; g.fill(); g.strokeStyle = 'rgba(0,0,0,0.6)'; g.lineWidth = 0.35; g.stroke(); };
  if (kind === 'bolt') { g.beginPath(); g.moveTo(5.4, 0.6); g.lineTo(2, 5); g.lineTo(4.4, 5); g.lineTo(3.4, 8.4); g.lineTo(7, 3.8); g.lineTo(4.6, 3.8); g.closePath(); fill('#5fe08e'); }
  if (kind === 'plane') {
    g.beginPath(); g.moveTo(4.5, 0.6); g.lineTo(5.2, 3.2); g.lineTo(8.6, 4.6); g.lineTo(8.6, 5.4); g.lineTo(5.2, 5); g.lineTo(5, 7.2);
    g.lineTo(6.4, 8.2); g.lineTo(6.4, 8.6); g.lineTo(2.6, 8.6); g.lineTo(2.6, 8.2); g.lineTo(4, 7.2); g.lineTo(3.8, 5); g.lineTo(0.4, 5.4);
    g.lineTo(0.4, 4.6); g.lineTo(3.8, 3.2); g.closePath(); fill('#c9ccd0');
  }
  if (kind === 'engine') { cogPath(g, 4.5, 4.5, 4, 3, 8); fill('#e0782a'); circle(g, 4.5, 4.5, 1.4); g.fillStyle = '#2a1206'; g.fill(); }
  if (kind === 'train') {
    rrect(g, 1, 1.4, 7, 5.6, 1.2); fill('#4a8ad0');
    g.fillStyle = '#cfe8ff'; g.fillRect(2, 2.4, 2, 1.8); g.fillRect(5, 2.4, 2, 1.8);
    circle(g, 2.8, 7.8, 0.9); g.fillStyle = '#c9ccd0'; g.fill(); circle(g, 6.2, 7.8, 0.9); g.fill();
  }
  if (kind === 'flamingo') {
    g.beginPath(); g.ellipse(5.6, 6.4, 2.8, 1.8, -0.2, 0, Math.PI * 2); fill('#f06aa0');
    g.beginPath(); g.moveTo(3.2, 6); g.quadraticCurveTo(1.4, 3.2, 2.6, 1.4); g.strokeStyle = '#f06aa0'; g.lineWidth = 0.9; g.stroke();
    circle(g, 2.8, 1.4, 0.9); fill('#f06aa0');
  }
  return c;
}

/** An instrument dial (steampunk armor's clock or compass): brass bezel, dark face, a hand at angle a (radians). */
function ccDial(g, cx, cy, r, a, kind) {
  circle(g, cx, cy, r); g.fillStyle = brass(g, cy - r, cy + r); g.fill();
  circle(g, cx, cy, r - 0.7); g.fillStyle = kind === 'clock' ? vgrad(g, cy - r, cy + r, [[0, '#2a3a5a'], [1, '#e8c070']]) : '#151210'; g.fill();
  if (a == null) return;   // the face alone (the game draws the hand)
  g.strokeStyle = kind === 'clock' ? '#fff3c8' : '#d84030'; g.lineWidth = 0.6;
  g.beginPath(); g.moveTo(cx, cy); g.lineTo(cx + Math.sin(a) * (r - 1.2), cy - Math.cos(a) * (r - 1.2)); g.stroke();
  if (kind === 'compass') { g.strokeStyle = '#e8e8e8'; g.beginPath(); g.moveTo(cx, cy); g.lineTo(cx - Math.sin(a) * (r - 1.6), cy + Math.cos(a) * (r - 1.6)); g.stroke(); }
}
/** Every chip's plate width (the steampunk armor's: two dials and margins). */
const CC_CHIP_W = 21;
/** The chips above the bar, each on a floating plate of its own, in a row centred over the bar (2 apart): web
 *  followers, the steampunk clock and compass, the Mekanism jetpack, RELOAD (the held gun empty, red; a low gun only its
 *  orange count; game: SlotRead.reloadRow), the held gun's FIRE MODE and AMMO (GunRead), the Artifacts on cooldown (one chip). Only when needed.
 *  top: the plates' top (default: over the centre plate). Returns it, or null when there is none. */
function ccInstruments(g, s, top) {
  const h = 9, y = top ?? CC.plate.mid - h - 2.5, cy = y + h / 2, chips = [];
  const W = CC_CHIP_W;   // every chip the steampunk chip's width, its content centred
  if (s.followers) chips.push([W, x => {   // a pale ghost (web map viewers) and how many
    const at = x + (W - 9 - ccNumberWidth(String(s.followers), 0.94)) / 2;
    ccInstruments.ghost(g, at + 4, cy);
    ccNumber(g, String(s.followers), at + 8, cy - 2.35, ccTint('#9ac8ff'), 'rgba(0,0,0,0.8)', 0.94);
  }]);
  if (s.steampunk) chips.push([W, x => {
    ccDial(g, x + 6, cy, 3.6, (s.t / 4000) % (Math.PI * 2), 'clock');
    ccDial(g, x + 15, cy, 3.6, Math.sin(s.t / 1500) * 0.6, 'compass');
  }]);
  if (s.jetpack) {   // the mode switch key in a key cap before the text
    const tw = ccChipKey.width(CC_JET_KEY) + ccJetChip.width(s.jetpack), jw = Math.max(W, tw + 8);
    chips.push([jw, x => ccJetChip(g, x + (jw - tw) / 2 + ccChipKey(g, CC_JET_KEY, x + (jw - tw) / 2, cy), cy, s.jetpack)]);
  }
  const held = (s.slotBadges || [])[s.slot];
  const wordChip = (words, col, key) => {   // its key cap, then words in the follower count's digits; widened to fit (4 each side)
    const tw = ccChipKey.width(key) + ccWords.width(words), w = Math.max(W, tw + 8);
    chips.push([w, x => ccWords(g, words, x + (w - tw) / 2 + ccChipKey(g, key, x + (w - tw) / 2, cy), cy, col)]);
  };
  if (held && held.gun && held.row === 'red') wordChip('RELOAD', CC_SLOT_INK.red, CC_RELOAD_KEY);   // the held gun empty
  if (held && held.fire) wordChip(held.fire, '#f2f2f2', CC_FIRE_KEY);   // FIRE MODE: a gun with several fire modes (game: GunRead)
  if (held && held.ammo) wordChip(held.ammo, '#f2f2f2', CC_AMMO_KEY);   // AMMO: a gun taking several ammo kinds
  const cds = s.artifactCooldowns || [];
  if (cds.length) {   // every Artifact on cooldown in one chip: its icon (placeholder gem), shaded by what is left
    const cw = cds.length * 9, w = Math.max(W, cw + 3);
    chips.push([w, x => cds.forEach((it, k) => {
      const cx = x + (w - cw) / 2 + 9 * k + 4.5;
      g.beginPath(); g.moveTo(cx, cy - 2.6); g.lineTo(cx + 2.4, cy); g.lineTo(cx, cy + 2.6); g.lineTo(cx - 2.4, cy); g.closePath();
      g.fillStyle = it.color; g.fill();
      g.fillStyle = 'rgba(0,0,0,0.6)'; g.fillRect(cx - 3.5, cy - 3.5, 7, 7 * it.left);
    })]);
  }
  if (!chips.length) return null;
  let x = -(chips.reduce((a, [w]) => a + w + 2, 0) - 2) / 2;
  for (const [w, draw] of chips) { ccFloatPlate(g, x, y, w, h); draw(x); x += w + 2; }
  return y;
}
/** The Mekanism jetpack's chip (a jetpack, or a MekaSuit's jetpack unit): the mode's word in its colour (Normal green,
 *  Vector orange, Hover cyan, Disabled red, in capitals), then the hydrogen left (percent, no sign) in white, both in
 *  the follower count's digits; the chip widens to fit, 4 px each side. (x, cy): text left, centre line. */
const CC_JET = { D: '#ff5a4e', V: '#ffa040', N: '#62e07a', H: '#3fe0ff' };
const CC_JET_WORD = { N: 'NORMAL', V: 'VECTOR', H: 'HOVER', D: 'DISABLED' };
/** A chip's hotkey inside the chip before its text (the game: WordChips.keyCap): a small key cap (dark, a light edge)
 *  holding the key in the slot numbers' white digits, centred on the words' line (cy - 0.35: their glyphs' centre). From
 *  x; returns its width and the gap after it (0 without a key). Same on every chip. */
function ccChipKey(g, key, x, cy) {
  if (!key) return 0;
  const k = CC_SLOT_NUM.k, kw = ccNumberWidth(key, k), kh = 5 * k, c = cy - 0.35;
  g.fillStyle = '#7a808a'; g.fillRect(x, c - kh / 2 - 1, kw + 2, kh + 2);
  g.fillStyle = '#1c1f24'; g.fillRect(x + 0.5, c - kh / 2 - 0.5, kw + 1, kh + 1);
  ccOutlinedNumber(g, key, x + 1, c - kh / 2, '#ffffff', k);
  return ccChipKey.width(key);
}
ccChipKey.width = key => key ? ccNumberWidth(key, CC_SLOT_NUM.k) + 2 + 2 : 0;
/** NTGL's fire select and ammo select keys (defaults B, N; the game: the player's bindings). */
const CC_FIRE_KEY = 'B', CC_AMMO_KEY = 'N';
/** The RELOAD chip (the held gun empty, red): NTGL's reload key (default R; the game: the binding). */
const CC_RELOAD_KEY = 'R';
/** Words in the follower count's digits split at spaces (no space glyph), 2 apart; (x, cy): left, centre line. */
function ccWords(g, words, x, cy, col) {
  for (const w of words.split(' ')) { ccNumber(g, w, x, cy - 2.35, col, 'rgba(0,0,0,0.8)', 0.94); x += ccNumberWidth(w, 0.94) + 2; }
}
ccWords.width = words => words.split(' ').reduce((a, w) => a + ccNumberWidth(w, 0.94) + 2, -2);
/** Mekanism's chest mode switch key, in the game its bound key's short name (at most 3: G, ALT, SHF, M4; WordChips.keyLabel). */
const CC_JET_KEY = 'G';   // Mekanism's default
function ccJetChip(g, x, cy, jet) {
  const word = CC_JET_WORD[jet.mode];
  ccNumber(g, word, x, cy - 2.35, CC_JET[jet.mode], 'rgba(0,0,0,0.8)', 0.94);
  ccNumber(g, String(Math.round(jet.fuel * 100)), x + ccNumberWidth(word, 0.94) + 2, cy - 2.35, '#f2f2f2', 'rgba(0,0,0,0.8)', 0.94);
}
/** The content's width: the word, 2, the number. */
ccJetChip.width = jet => ccNumberWidth(CC_JET_WORD[jet.mode], 0.94) + 2 + ccNumberWidth(String(Math.round(jet.fuel * 100)), 0.94);
/** Create's marks over hotbar slots: toolbox-linked slots (red liquid corner chips), the schematic tool's slot (cyan). */
function ccSlotMarks(g, s) {
  for (const i of s.toolboxSlots || []) ccToolboxChips(g, i, false);
  if (s.schematicSlot != null) {
    const [x, y] = CC.slot(s.schematicSlot);
    rrect(g, x + 1, y + 1, 16, 16, 0.6); g.strokeStyle = 'rgba(110,220,255,0.9)'; g.lineWidth = 0.6; g.stroke();
  }
}
/** A toolbox-linked slot's marks: triangular chips in the top-right and bottom-left corners, red liquid (the
 *  toolbox's red) in quiet bronze tucked inside the bezel's rounded corners. lit: the selected slot's, drawn over its
 *  selection ring with a brighter, glowing red. */
function ccToolboxChips(g, i, lit, at) {
  const [x, y] = at || CC.slot(i), L = 7, r = 0.8;   // at: a socket's top left instead of hotbar slot i
  const chip = (ax, ay, sx, sy, inset) => {   // right angle at (ax, ay), legs along +sx (x) and +sy (y)
    const a = [ax + sx * inset, ay + sy * inset], b = [ax + sx * (L - inset * 2.4), ay + sy * inset], c = [ax + sx * inset, ay + sy * (L - inset * 2.4)];
    g.beginPath(); g.moveTo(...a); g.arcTo(...b, ...c, r * (1 - inset)); g.arcTo(...c, ...a, r * (1 - inset)); g.arcTo(...a, ...b, r * (1 - inset)); g.closePath();
  };
  for (const [ax, ay, sx, sy] of [[x + 17.5, y + 0.5, -1, 1], [x + 0.5, y + 17.5, 1, -1]]) {
    chip(ax, ay, sx, sy, 0.3); g.fillStyle = P.bronze; g.fill();
    if (lit) { g.save(); g.shadowColor = 'rgba(230,40,40,0.9)'; g.shadowBlur = 1.6; }
    chip(ax, ay, sx, sy, 0.95);
    g.fillStyle = radial(g, ax + sx * 2.2, ay + sy * 2.2, 4.5, lit ? [[0, '#ff4a48'], [1, '#7a1018']] : [[0, '#b02028'], [1, '#4a0a10']]); g.fill();
    if (lit) g.restore();
  }
}
/** Farmer's Delight Comfort: a slow warm sheen along the health meter (Comfort, saturation 0, hurt, no Regeneration). */
function ccComfortFx(g, s, [x, y, w, h]) {
  if (!s.comfort || s.saturation > 0 || s.health >= (s.maxHealth || 20)) return;   // shows alongside Regeneration
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.clip();
  const p = ((s.t / 2600) % 1) * (w + 12) - 6, gr = g.createLinearGradient(x + p - 6, 0, x + p + 6, 0);
  gr.addColorStop(0, 'rgba(255,158,196,0)'); gr.addColorStop(0.5, 'rgba(255,158,196,0.55)'); gr.addColorStop(1, 'rgba(255,158,196,0)');   // pink, the whole glass
  g.fillStyle = gr; g.fillRect(x + p - 6, y, 12, h);
  g.restore();
}
/** Hardcore: a pale skull on a small floating iron plate above the offhand socket (the follower ghost's style). */
const CC_SKULL_PLATE = { w: 9, h: 9, lift: 2.5 };
function ccHardcore(g, s) {
  if (!s.hardcore) return;
  const { w, h, lift } = CC_SKULL_PLATE, x = CC.lay.offhand + 9 - w / 2, y = (s.orbs ? CC.plate.wing : CC_WING.top) - h - lift;
  ccFloatPlate(g, x, y, w, h);
  ccSkull(g, x + w / 2, y + h / 2);
}
/** A small iron plate floating above the bar (the instrument plate's style): shadow, iron, brass rim. */
function ccFloatPlate(g, x, y, w, h) {
  const plate = () => rrect(g, x, y, w, h, CC.plate.r);
  g.save(); g.translate(0, 0.6); plate(); g.fillStyle = 'rgba(0,0,0,0.5)'; g.fill(); g.restore();
  plate(); g.fillStyle = iron(g, y, y + h); g.fill(); ironSurface(g, plate); rim(g, plate, y, y + h, P.brassHi, 0.35);
}
/** A skull centred on (cx, cy), about 5 wide and 6 tall, pale as the follower ghost. */
function ccSkull(g, cx, cy) {
  g.fillStyle = 'rgba(220,235,255,0.92)';
  circle(g, cx, cy - 0.7, 2.5); g.fill();
  rrect(g, cx - 1.6, cy + 0.6, 3.2, 2.2, 0.5); g.fill();
  g.fillStyle = '#152030';
  circle(g, cx - 1, cy - 0.5, 0.75); g.fill(); circle(g, cx + 1, cy - 0.5, 0.75); g.fill();
  g.beginPath(); g.moveTo(cx, cy + 0.4); g.lineTo(cx - 0.45, cy + 1.1); g.lineTo(cx + 0.45, cy + 1.1); g.closePath(); g.fill();
  for (const dx of [-0.8, 0, 0.8]) g.fillRect(cx + dx - 0.12, cy + 1.7, 0.24, 1.1);
}
/** The follower ghost centred on (cx, cy), about 5 wide and 6 tall. */
ccInstruments.ghost = (g, cx, cy) => {
  g.beginPath(); g.moveTo(cx - 2.6, cy + 3); g.lineTo(cx - 2.6, cy - 0.6); g.arc(cx, cy - 0.6, 2.6, Math.PI, 0); g.lineTo(cx + 2.6, cy + 3);
  for (let k = 0; k < 3; k++) { const bx = cx + 2.6 - (k + 0.5) * (5.2 / 3); g.lineTo(bx, cy + 2.1); g.lineTo(bx - 5.2 / 6, cy + 3); }
  g.closePath(); g.fillStyle = 'rgba(220,235,255,0.92)'; g.fill();
  g.fillStyle = '#152030'; g.fillRect(cx - 1.4, cy - 1.2, 0.8, 1.1); g.fillRect(cx + 0.6, cy - 1.2, 0.8, 1.1);
};
