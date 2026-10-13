// The enchanting table screen (EnchantmentScreen, EnchantmentMenu; 1.21.1, read from the NeoForge client
// jar's bytecode). Layout and the sprites it draws, and its art in our style. Uses components.js,
// panel-art.js, sprites.js, material.js.
// Direction (chosen): an arcane study, one concept with one focus. A dark walnut desk; a plum leather desk mat;
// on it an inscribed rune circle in brass inlay, centred where the game floats the book, the two input slots at
// its foot; the open book in the same plum leather, its pages the options; the player's inventory below, the
// shared flat section. Brass only for the inlay and the book's corners; violet light only from the circle.
const ENCH = {
  w: 176, h: 166,
  item: [15, 47], lapis: [35, 47],
  main: [0, 1, 2].flatMap(i => [...Array(9).keys()].map(j => [8 + j * 18, 84 + i * 18])),
  hotbar: [...Array(9).keys()].map(j => [8 + j * 18, 142]),
  option: k => [60, 14 + 19 * k, 108, 19],     // enchantment_slot sprites
  level: k => [61, 15 + 19 * k, 16, 16],       // level_1..3 sprites
  text: k => [80, 16 + 19 * k],                // the enchantment's name, enchanting font, wrapped to 86 - cost width
  cost: k => [166, 23 + 19 * k],               // the cost, right-aligned to x 166, with a shadow
  book: [33, 31],                              // the 3D book is drawn centred here
  title: [8, 6], invLabel: [8, 72],
  // Fixed text colours (drawn by the game).
  color: { text: '#685e4a', disabled: '#342f25', hover: '#ffff80', cost: '#80ff20', costNo: '#407f10' },
};
const enchSlots = () => [ENCH.item, ENCH.lapis, ...ENCH.main, ...ENCH.hotbar];

// The lower section rises to y 67.6 on the left, up to the book's edge (x 59.2), to hold the "Inventory"
// label (50 px at x 8, so it ends at 57; measured with the game font), and starts at y 75.6 under the book.
const ENCH_LOW = { rise: 67.6, under: 75.6, step: 59.2 };
const ENCH_ART = { leather: '#2a1724', inlay: '#a8843f', walnut: '#271b12' };   // plum leather: it harmonises with the violet magic

/** container/enchanting_table: the panel at logical 176 x 166. sga: the enchanting font, for the circle's runes. */
function enchantingPanel(sga) {
  const [c, g] = hires(ENCH.w, ENCH.h);
  const { rise, under, step } = ENCH_LOW, r = 2.4, A = ENCH_ART;
  // The desk: dark walnut, one tone with a faint grain, a darker edge.
  const top = () => rrect(g, 0.4, 0.4, 175.2, under + 4, [r, r, 0, 0]);
  rrect(g, 0.4, 1, 175.2, 165, r); g.fillStyle = 'rgba(0,0,0,0.5)'; g.fill();
  top(); g.fillStyle = A.walnut; g.fill();
  surface(g, top, TEX.brushed, 0.12);
  top(); g.strokeStyle = 'rgba(0,0,0,0.6)'; g.lineWidth = 0.6; g.stroke();
  // The desk mat: plum leather inlaid in the desk (no tooled border: it would run under the title plate).
  const mat = () => rrect(g, 2.8, 2.8, 53.2, rise - 4, 1.2);
  mat(); g.fillStyle = A.leather; g.fill();
  surface(g, mat, TEX.grain, 0.1);
  inlay(g, mat, 2.8, rise - 1.2);
  // The rune circle, centred on the book (33, 30.5): violet light, two brass rings, runes between them, six
  // points on the inner ring. Its foot (y 44.1) clears the input slots' rims (from y 45.4).
  const cx = 33, cy = 30.5, R1 = 13.6, R2 = 10.6;
  circle(g, cx, cy, R1 + 3);
  g.fillStyle = radial(g, cx, cy, R1 + 3, [[0, 'rgba(170,110,240,0.3)'], [0.75, 'rgba(170,110,240,0.12)'], [1, 'rgba(170,110,240,0)']]); g.fill();
  g.strokeStyle = A.inlay; g.lineWidth = 0.5;
  for (const rr of [R1, R2]) { circle(g, cx, cy, rr); g.stroke(); }
  if (sga) {
    const glyphs = 'abcdefghijkl', k = sga.cell, t = document.createElement('canvas');
    t.width = sga.img.width; t.height = sga.img.height;
    const tg = t.getContext('2d'); tg.drawImage(sga.img, 0, 0);
    tg.globalCompositeOperation = 'source-in'; tg.fillStyle = A.inlay; tg.fillRect(0, 0, t.width, t.height);
    for (let i = 0; i < glyphs.length; i++) {
      const a = (i / glyphs.length) * Math.PI * 2 - Math.PI / 2, rm = (R1 + R2) / 2, ch = glyphs.charCodeAt(i);
      g.save(); g.translate(cx + Math.cos(a) * rm, cy + Math.sin(a) * rm); g.rotate(a + Math.PI / 2);
      g.drawImage(t, (ch % 16) * k, Math.floor(ch / 16) * k, k, k, -1.25, -1.25, 2.5, 2.5); g.restore();
    }
  }
  g.fillStyle = A.inlay;
  for (let i = 0; i < 6; i++) { const a = (i / 6) * Math.PI * 2 - Math.PI / 2; circle(g, cx + Math.cos(a) * R2, cy + Math.sin(a) * R2, 0.7); g.fill(); }
  // The title on a steel nameplate (the game draws it in #404040 at (8, 6)), on the mat.
  steelNameplate(g, 5.6, 3.8, 48.4, 11.4);
  // The open book in the same leather: a page stack showing round the option pages (60..168 x 14..71),
  // the spine binding above and below, brass corner protectors.
  const bx0 = 59.2, by0 = 10.6, bx1 = 171.2, by1 = 75;
  rrect(g, bx0, by0, bx1 - bx0, by1 - by0, 1.2); g.fillStyle = A.leather; g.fill();
  g.strokeStyle = 'rgba(0,0,0,0.6)'; g.lineWidth = 0.4; g.stroke();
  g.fillStyle = '#cdbd93'; g.fillRect(59.8, 11.6, 109.8, 62.2);
  g.fillStyle = 'rgba(90,70,40,0.35)';
  for (const y of [12.4, 13.4, 72, 73]) g.fillRect(59.8, y, 109.8, 0.5);              // page edges, top and bottom
  for (const x of [168.6, 169.2]) g.fillRect(x, 11.6, 0.4, 62.2);                       // and the fore edge
  g.fillStyle = '#170b13'; g.fillRect(112.8, by0, 2.4, 3.6); g.fillRect(112.8, 71, 2.4, by1 - 71);   // spine binding
  for (const [x, y, sx, sy] of [[bx0, by0, 1, 1], [bx1, by0, -1, 1], [bx0, by1, 1, -1], [bx1, by1, -1, -1]]) {
    g.beginPath(); g.moveTo(x, y); g.lineTo(x + sx * 4.4, y); g.lineTo(x, y + sy * 4.4); g.closePath();
    g.fillStyle = '#9a7536'; g.fill();
  }
  // The input slots at the circle's foot: obsidian with a brass rim (the item), a lapis-blue rim (lapis).
  const [ix, iy] = ENCH.item;
  rrect(g, ix - 1.6, iy - 1.6, 19.2, 19.2, 0.8); g.fillStyle = A.inlay; g.fill();
  g.fillStyle = '#120c18'; g.fillRect(ix - 1, iy - 1, 18, 18);
  g.fillStyle = 'rgba(0,0,0,0.75)'; g.fillRect(ix - 1, iy - 1, 18, 0.6); g.fillRect(ix - 1, iy - 1, 0.6, 18);
  const [lx, ly] = ENCH.lapis;
  rrect(g, lx - 1.6, ly - 1.6, 19.2, 19.2, 0.8); g.fillStyle = '#284a9c'; g.fill();
  cells(g, [ENCH.lapis]);
  // The player's inventory: the shared flat iron section, identical on every screen.
  const low = () => {
    g.beginPath(); g.moveTo(0.4, rise); g.lineTo(step - r, rise); g.arcTo(step, rise, step, rise + r, r); g.lineTo(step, under);
    g.lineTo(175.6, under); g.lineTo(175.6, 165.6 - r); g.arcTo(175.6, 165.6, 175.6 - r, 165.6, r);
    g.lineTo(0.4 + r, 165.6); g.arcTo(0.4, 165.6, 0.4, 165.6 - r, r); g.closePath();
  };
  ironPlate(g, low, rise, 166);
  playerInventory(g, ...ENCH.invLabel);
  return c;
}

/** enchantment_slot sprites, 108 x 19: pages of the book. Enabled: parchment (the game's #685E4A text reads
 *  on it), raised (a control). Highlighted: deep violet, lit, for the game's #FFFF80 text. Disabled: dim,
 *  flat (not pressable). The spine fold runs down the middle of each. */
function optionSprite(kind) {
  const [c, g] = hires(108, 19);
  const face = { enabled: '#d6c7a0', highlighted: '#3d2a55', disabled: '#5b5142' }[kind];
  const p = () => rrect(g, 0.3, 0.3, 107.4, 18.4, 1);
  p(); g.fillStyle = face; g.fill();
  if (kind !== 'disabled') {
    g.fillStyle = 'rgba(0,0,0,0.22)'; g.fillRect(0.6, 17.6, 106.8, 1);              // shaded lower edge
    rim(g, p, 0.3, 18.7, kind === 'enabled' ? 'rgba(255,250,235,0.55)' : 'rgba(200,170,255,0.4)', 0.3);
  }
  if (kind === 'highlighted') { g.save(); p(); g.clip(); g.fillStyle = 'rgba(160,110,230,0.18)'; g.fillRect(0, 0, 108, 6); g.restore(); }
  p(); g.strokeStyle = 'rgba(0,0,0,0.55)'; g.lineWidth = 0.4; g.stroke();
  g.fillStyle = kind === 'highlighted' ? 'rgba(0,0,0,0.35)' : 'rgba(80,60,35,0.3)'; g.fillRect(53.75, 0.6, 0.5, 17.8);
  return c;
}

/** level_1..3 sprites, 16 x 16, flat: one to three experience gems in a row (the cost tier), green when
 *  affordable, grey when not. */
function levelSprite(n, disabled) {
  const [c, g] = hires(16, 16);
  const col = disabled ? '#5c6654' : '#8bd448';
  const xs = { 1: [8], 2: [5.4, 10.6], 3: [3.4, 8, 12.6] }[n];
  for (const x of xs) {
    g.beginPath(); g.moveTo(x, 4.5); g.lineTo(x + 2.4, 8); g.lineTo(x, 11.5); g.lineTo(x - 2.4, 8); g.closePath();
    g.fillStyle = col; g.fill();
  }
  return c;
}

/** item/empty_slot_lapis_lazuli (1.21.1's item atlas), 16 x 16: a dotted ghost of a lapis gem (empty-slot hint, never filled). */
function lapisGhost() {
  const [c, g] = hires(16, 16);
  g.setLineDash([0.5, 0.5]); g.lineCap = 'butt';
  g.beginPath(); g.moveTo(8, 3); g.lineTo(12.5, 7); g.lineTo(10.5, 13); g.lineTo(5.5, 13); g.lineTo(3.5, 7); g.closePath();
  g.moveTo(3.5, 7); g.lineTo(12.5, 7);
  g.strokeStyle = 'rgba(220,226,240,0.4)'; g.lineWidth = 0.5; g.stroke();
  return c;
}
