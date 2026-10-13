// Compact HUD: mod elements that sit at the hotbar or change the vanilla rows in this pack (read from each mod's
// bytecode and the instance config; see CLAUDE.md), mapped onto the compact action bar. Uses sprites.js, meters.js,
// hud-compact.js (ccMeter, ccIcon, CC). Elsewhere-on-screen mod HUDs (corners, crosshair, tooltips) are untouched.
//   Farmer's Delight: Nourishment gilds the food broth (it draws gilded shanks over vanilla's food); Comfort runs a slow
//     warm sheen along health (its sheen over the hearts: saturation 0, hurt, no Regeneration).
//   Mekanism: the MekaSuit energy bar (above armor in vanilla's left column) joins the armor extension as a second pipe.
//   Immersive Aircraft: the vehicle's health (a heart row on the air row) takes the right extension; the engine gauge
//     (a dial over the XP bar) takes the XP pipe, an engine icon in the level window.
//   Create: the train HUD's speed bar (over the XP bar) takes the XP pipe; toolbox-linked slots get red liquid corner chips (top right, bottom left),
//     the schematic tool's slot a cyan frame (both draw over vanilla's slots, so our sockets keep vanilla's positions).
//   Artifacts: Helium Flamingo charge (a row on the right column) takes the right extension; item cooldown icons (right
//     of the hotbar in vanilla, over our wing) and Immersive Armors' steampunk clock and compass (either side of the
//     hotbar, over our wings) move to a small instrument plate above the bar, shown only while there is one.
//   Haven Link (our server mod): web map viewers following you; Haven Link tells you in chat ("[Web] Name is following
//     you on the web map" / "... stopped following ..."); the client counts them: a ghost and the count on the plate.

const CC_MEKA = ['#c8ffd8', '#3fcf7a', '#0e4a26'];       // Mekanism energy
const CC_HULL = ['#e8eef4', '#8a9aa8', '#2a323a'];        // aircraft hull
const CC_ENGINE = ['#ffd2a0', '#e0782a', '#5a2408'];      // engine power
const CC_TRAIN = ['#cfe8ff', '#4a8ad0', '#12305a'];       // train speed
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
  g.strokeStyle = kind === 'clock' ? '#fff3c8' : '#d84030'; g.lineWidth = 0.6;
  g.beginPath(); g.moveTo(cx, cy); g.lineTo(cx + Math.sin(a) * (r - 1.2), cy - Math.cos(a) * (r - 1.2)); g.stroke();
  if (kind === 'compass') { g.strokeStyle = '#e8e8e8'; g.beginPath(); g.moveTo(cx, cy); g.lineTo(cx - Math.sin(a) * (r - 1.6), cy + Math.cos(a) * (r - 1.6)); g.stroke(); }
}
/** The instrument plate above the bar: steampunk clock and compass, then Artifacts cooldown icons. Only when needed. */
function ccInstruments(g, s) {
  const items = [];
  if (s.followers) items.push('ghost');
  if (s.steampunk) items.push('clock', 'compass');
  for (const cd of s.artifactCooldowns || []) items.push(cd);
  if (!items.length) return;
  const cellW = it => it === 'ghost' ? 9 + ccNumberWidth(String(s.followers), 0.625) + 1 : 9;
  const cell = 9, w = items.reduce((a, it) => a + cellW(it), 0) + 3, h = 9, x = -w / 2, y = CC.plate.mid - h - 2.5, r = CC.plate.r;
  const plate = () => rrect(g, x, y, w, h, r);
  g.save(); g.translate(0, 0.6); plate(); g.fillStyle = 'rgba(0,0,0,0.5)'; g.fill(); g.restore();
  plate(); g.fillStyle = iron(g, y, y + h); g.fill(); ironSurface(g, plate); rim(g, plate, y, y + h, P.brassHi, 0.35);
  let cur = x + 2;
  items.forEach((it) => {
    const cx = cur + cell / 2 - 0.5, cy = y + h / 2; cur += cellW(it);
    if (it === 'ghost') {   // a pale ghost (web map viewers) and how many
      ccInstruments.ghost(g, cx, cy);
      return ccNumber(g, String(s.followers), cx + 4, cy - 1.6, '#dcebff', 'rgba(0,0,0,0.8)', 0.625);
    }
    if (it === 'clock') return ccDial(g, cx, cy, 3.6, (s.t / 4000) % (Math.PI * 2), 'clock');
    if (it === 'compass') return ccDial(g, cx, cy, 3.6, Math.sin(s.t / 1500) * 0.6, 'compass');
    // An artifact on cooldown: its icon (placeholder gem) in a small well, shaded by what is left of the cooldown.
    rrect(g, cx - 3.6, cy - 3.6, 7.2, 7.2, 0.8); g.fillStyle = '#060708'; g.fill();
    g.beginPath(); g.moveTo(cx, cy - 2.6); g.lineTo(cx + 2.4, cy); g.lineTo(cx, cy + 2.6); g.lineTo(cx - 2.4, cy); g.closePath();
    g.fillStyle = it.color; g.fill();
    g.fillStyle = 'rgba(0,0,0,0.6)'; g.fillRect(cx - 3.6, cy - 3.6, 7.2, 7.2 * it.left);
  });
}
/** Create's marks over hotbar slots: toolbox-linked slots (red liquid corner chips), the schematic tool's slot (cyan). */
function ccSlotMarks(g, s) {
  // Toolbox-linked: triangular chips in the top-right and bottom-left corners, red liquid (the toolbox's red) in a
  // brass casing that grows out of the socket's bezel.
  for (const i of s.toolboxSlots || []) {
    const [x, y] = CC.slot(i), L = 7, r = 0.8;
    const chip = (ax, ay, sx, sy, inset) => {   // right angle at (ax, ay), legs along +sx (x) and +sy (y)
      const a = [ax + sx * inset, ay + sy * inset], b = [ax + sx * (L - inset * 2.4), ay + sy * inset], c = [ax + sx * inset, ay + sy * (L - inset * 2.4)];
      g.beginPath(); g.moveTo(...a); g.arcTo(...b, ...c, r * (1 - inset)); g.arcTo(...c, ...a, r * (1 - inset)); g.arcTo(...a, ...b, r * (1 - inset)); g.closePath();
    };
    for (const [ax, ay, sx, sy] of [[x + 18, y, -1, 1], [x, y + 18, 1, -1]]) {
      // Both chips alike and quiet: bronze casing (no lit rim), a deep red with a soft core, not lit from above.
      chip(ax, ay, sx, sy, 0.3); g.fillStyle = P.bronze; g.fill();
      chip(ax, ay, sx, sy, 0.95);
      g.fillStyle = radial(g, ax + sx * 2.2, ay + sy * 2.2, 4.5, [[0, '#b02028'], [1, '#4a0a10']]); g.fill();
    }
  }
  if (s.schematicSlot != null) {
    const [x, y] = CC.slot(s.schematicSlot);
    rrect(g, x + 1, y + 1, 16, 16, 0.6); g.strokeStyle = 'rgba(110,220,255,0.9)'; g.lineWidth = 0.6; g.stroke();
  }
}
/** Farmer's Delight Comfort: a slow warm sheen along the health meter (Comfort, saturation 0, hurt, no Regeneration). */
function ccComfortFx(g, s, [x, y, w, h]) {
  if (!s.comfort || s.saturation > 0 || s.regen || s.health >= (s.maxHealth || 20)) return;
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.clip();
  const p = ((s.t / 2600) % 1) * (w + 12) - 6, gr = g.createLinearGradient(x + p - 6, 0, x + p + 6, 0);
  gr.addColorStop(0, 'rgba(255,240,200,0)'); gr.addColorStop(0.5, 'rgba(255,236,190,0.45)'); gr.addColorStop(1, 'rgba(255,240,200,0)');
  g.fillStyle = gr; g.fillRect(x + p - 6, y, 12, h);
  g.restore();
}
/** The follower ghost centred on (cx, cy), about 5 wide and 6 tall. */
ccInstruments.ghost = (g, cx, cy) => {
  g.beginPath(); g.moveTo(cx - 2.6, cy + 3); g.lineTo(cx - 2.6, cy - 0.6); g.arc(cx, cy - 0.6, 2.6, Math.PI, 0); g.lineTo(cx + 2.6, cy + 3);
  for (let k = 0; k < 3; k++) { const bx = cx + 2.6 - (k + 0.5) * (5.2 / 3); g.lineTo(bx, cy + 2.1); g.lineTo(bx - 5.2 / 6, cy + 3); }
  g.closePath(); g.fillStyle = 'rgba(220,235,255,0.92)'; g.fill();
  g.fillStyle = '#152030'; g.fillRect(cx - 1.4, cy - 1.2, 0.8, 1.1); g.fillRect(cx + 0.6, cy - 1.2, 0.8, 1.1);
};
