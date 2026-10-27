// Sophisticated Backpacks textures (sophisticatedcore:textures/gui/*, each 256x256), redesigned as a
// leather pack in our style. The mod cuts these textures at positions that depend on the backpack (see
// backpack.js), so the art obeys those cuts:
// - storage_background_*: the screen is the texture's top 17 + h rows glued to its bottom 97 + h rows
//   (h = half the visible storage height). Rows 17 to 159 vary only across x, or the join shows (only
//   the body's rounded lower corners break that, and rows past 150 are only seen from the bottom slice);
//   rows 159 to 256 are the fixed player pocket.
// - gui_controls tab art (x 128 to 256): a tab is the top half of the art glued to the bottom half, cut
//   from the right edge, so the body is flat and the borders run along the top, right and bottom.
// - gui_controls upgrade column (x 0 to 26): rows 4 to 198 are cropped to the slot count, period 16.


/** Panel icons are flat: one solid light steel tone (rust for "not" marks), no gradient or shadow. */
const ICON_FLAT = '#d4d8dc';

/** Variant geometry: width W and the player pocket's x offset. */
function bpVariant(cols, wider) {
  return { W: cols * 18 + 14 + (wider ? 6 : 0), off: cols === 9 ? (wider ? 3 : 0) : (wider ? 30 : 27) };
}

/** Makes rows y0..y1 of a hires canvas vary only across x: each column becomes its own average over those
 *  rows (logical units). Averaging, not copying one row, so grain does not turn into hard stripes. */
function xOnly(c, y0, y1) {
  const g = c.getContext('2d'), r0 = Math.round(y0 * R), r1 = Math.round(y1 * R), n = r1 - r0;
  const img = g.getImageData(0, r0, c.width, n), d = img.data, w = c.width, sum = new Float64Array(w * 4);
  for (let y = 0; y < n; y++) for (let x = 0; x < w; x++) for (let k = 0; k < 4; k++) sum[x * 4 + k] += d[(y * w + x) * 4 + k];
  for (let y = 0; y < n; y++) for (let x = 0; x < w; x++) for (let k = 0; k < 4; k++) d[(y * w + x) * 4 + k] = sum[x * 4 + k] / n;
  g.putImageData(img, 0, r0);
}

/** storage_background_{9,12}[_wider]: a stitched leather body holding the storage, and the player's inventory
 *  below it in the inventory screen's design: its iron plate, thin sunken cells, the hotbar's brass outline,
 *  a steel nameplate for "Inventory". The plate starts where storage ends (row 159); on 12 column and wider
 *  packs it steps in to 176 wide at row 172, as the original does. No straps or buckles on the side edges:
 *  the upgrade pouch and the tab strips cover them. */
function storageBackground(cols, wider) {
  const { W, off } = bpVariant(cols, wider), join = 159.4, step = 172;
  const [c, g] = hires(256, 256);
  // The leather body, rows 0 to 159, flat colour (rows 17 to 159 are averaged into x-only art and must meet
  // the rows below without a step). Its seam is one closed loop 3 px in, its bottom hidden under the last
  // storage row except where it turns at the corners. Dots one logical px apart stay continuous at any join.
  const body = () => rrect(g, 0.4, 0.4, W - 0.8, join - 0.4, [2.4, 2.4, 0, 0]);
  rrect(g, 0.4, 1, W - 0.8, join, 2.4); g.fillStyle = 'rgba(0,0,0,0.5)'; g.fill();
  body(); g.fillStyle = '#3a2619'; g.fill();
  surface(g, body, TEX.grain, 0.12); surface(g, body, TEX.blotch, 0.18);
  xOnly(c, 17, join);
  rim(g, body, 0.4, join, 'rgba(255,220,180,0.35)', 0.3);
  stitch(g, () => rrect(g, 3, 1.8, W - 6, 155.2, 1));
  // No label behind the title (chosen): the mod's title sits on the leather itself.
  // Framed like the sort buttons beside it, so the mod's flat grey square reads as one of that row.
  steelButton(g, W - 43, 4, 12, 12, false, 1.2);
  // The player's inventory: the inventory screen's iron plate, full width through the "Inventory" row,
  // stepping in to the 176 wide part at row 172 when the pack is wider.
  const r = 2.4, R0 = off + 0.4, R1 = off + 175.6;
  const plate = () => {
    g.beginPath(); g.moveTo(0.4, join); g.lineTo(W - 0.4, join);
    if (off) { g.lineTo(W - 0.4, step - r); g.arcTo(W - 0.4, step, W - 0.4 - r, step, r); g.lineTo(R1, step); }
    g.lineTo(R1, 255.6 - r); g.arcTo(R1, 255.6, R1 - r, 255.6, r); g.lineTo(R0 + r, 255.6); g.arcTo(R0, 255.6, R0, 255.6 - r, r);
    if (off) { g.lineTo(R0, step); g.lineTo(0.4 + r, step); g.arcTo(0.4, step, 0.4, step - r, r); }
    g.closePath();
  };
  ironPlate(g, plate, join, 256);
  playerInventory(g, off + 8, 162, { plate: false });   // the label's position (H - 94 on screen); no plate (chosen)
  solidAlpha(c);   // drawn unblended: the rim's and shadow's soft edges showed as bright clumps on the corners
  return c;
}

/** The search box's hint glyph, U+1F50D (the mod writes "🔍" centred on its grey fill, in #BBBBBB with a shadow, from
 *  the game's font): a magnifier in an 8 x 8 font cell (ascent 7) drawn at R x (64 px): a bitmap provider scales a glyph
 *  by height / image height, so it keeps the 8 px cell and stays smooth. White, so the mod's grey tints it. Shipped as
 *  createhud:font/search.png with a bitmap provider in minecraft:font/default.json. */
function searchGlyph() {
  const [c, g] = hires(8, 8);
  g.strokeStyle = '#ffffff'; g.lineCap = 'round';
  circle(g, 2.9, 2.9, 2.1); g.lineWidth = 0.9; g.stroke();
  g.beginPath(); g.moveTo(4.6, 4.6); g.lineTo(6.6, 6.6); g.lineWidth = 1.3; g.stroke();
  return c;
}

/** slots_background: a 12 x 12 grid of 18 px sunken cells; the mod blits the part it needs. */
function slotsBackground() {
  const [c, g] = hires(256, 256);
  for (let r = 0; r < 12; r++) for (let q = 0; q < 12; q++) cell(g, q * 18, r * 18);
  return c;
}

/** upgrade_slots_background (ours): haven-create-ui-ux draws every upgrade tab's slot group from it as a schematic
 *  scroll (TabSlots.java; tabScroll() mirrors it). Layout, logical:
 *    (0, 0) 218 x 218: blueprint paper, its 1 px edge all round, slot marks at (1 + 18 c, 1 + 18 r) for 12 x 12 cells.
 *      The group is cut from (0, 0) at w + 1 x h + 1 (left and top edges included), its right and bottom edges from the
 *      left column and top row (both plain edge).
 *    (0, 220) 236 x 2: a wooden roller: brass cap (0..1), body (1..235), cap (235..236); one above and one below.
 *  Sizes are whole GUI pixels (the game blits in them): the mod leaves only 2 to 3 px around a group. */
function upgradeSlotsBackground() {
  const [c, g] = hires(256, 256);
  blueprint(g, 0, 0, 218, 218);
  for (let r = 0; r < 12; r++) for (let q = 0; q < 12; q++) draftMark(g, 1 + q * 18, 1 + r * 18);
  g.fillStyle = vgrad(g, 220, 222, [[0, '#d9aa70'], [0.3, '#a8743f'], [0.75, '#6e4520'], [1, '#3a220c']]); g.fillRect(1, 220, 234, 2);
  g.fillStyle = brass(g, 220, 222); g.fillRect(0, 220, 1, 2); g.fillRect(235, 220, 1, 2);
  solidAlpha(c);
  return c;
}

/** gui_controls: the pack's sheet with our pieces painted over the regions we restyle; the rest stays. */
function guiControls(orig) {
  const [c, g] = hires(256, 256);
  g.imageSmoothingEnabled = false;
  if (orig) g.drawImage(orig, 0, 0, 256, 256);
  const clear = (x, y, w, h) => g.clearRect(x, y, w, h);
  // Upgrade column (0,0)-(26,204): a leather side pouch sewn onto the pack, 16 px pockets every 16 rows.
  clear(0, 0, 28, 222);
  const pouch = () => rrect(g, 0.4, 0.4, 26, 203.2, [2.4, 0, 0, 2.4]);
  pouch(); g.fillStyle = hgrad(g, 0, 26, [[0, '#3a2619'], [1, '#4a3122']]); g.fill();
  surface(g, pouch, TEX.grain, 0.1);
  // One seam: where the pouch is sewn onto the pack, from its top edge to its bottom edge. No seam on the
  // left: the upgrade switches clip on there.
  stitch(g, () => { g.beginPath(); g.moveTo(24, 0.4); g.lineTo(24, 203.6); });   // panel x 3, in line with the body's seam
  pouch(); g.strokeStyle = 'rgba(0,0,0,0.55)'; g.lineWidth = 0.4; g.stroke();
  for (let i = 0; i < 12; i++) cell(g, 6, 6 + 16 * i, undefined, 16);
  // Switch bracket (0,204,7,18): the pouch's leather bulging out to hold a switch. Drawn at panel x -25, it
  // overlaps the pouch from src x 4 on, so its edge stops there and it reads as one piece with the pouch.
  const tab = () => rrect(g, 0.3, 204.3, 6.7, 17.4, [1.6, 0, 0, 1.6]);
  tab(); g.fillStyle = hgrad(g, 0, 7, [[0, '#33221a'], [1, '#3a2619']]); g.fill();
  g.save(); g.beginPath(); g.rect(0, 204, 4.4, 18); g.clip();
  tab(); g.strokeStyle = 'rgba(0,0,0,0.55)'; g.lineWidth = 0.4; g.stroke();
  g.restore();
  // Switches (65,0) and hover (71,0), 6 x 12.
  clear(65, 0, 12, 12); switchSlot(g, 65, 0, 6, 12, false); switchSlot(g, 71, 0, 6, 12, true);
  // Buttons: default 18 x 18 (29,0) and hover (47,0); small 12 x 12 (29,18) and hover (41,18).
  clear(29, 0, 36, 18); steelButton(g, 29, 0, 18, 18, false, 1.6); steelButton(g, 47, 0, 18, 18, true, 1.6);
  // The small button's face is inset 1.2 px in its 12 px box: the mod sets these 1 px from slot rows, and
  // a full-size face reads as touching them.
  clear(29, 18, 24, 12); steelButton(g, 30.2, 19.2, 9.6, 9.6, false, 1); steelButton(g, 42.2, 19.2, 9.6, 9.6, true, 1);
  // Small 8 x 12 button (53,18) and hover (61,18): plain steel; the mod draws the previous/next icons on top.
  clear(53, 18, 16, 12); steelButton(g, 53, 18, 8, 12, false, 1); steelButton(g, 61, 18, 8, 12, true, 1);
  // Crafting: select result (69,18) and hover (63,30), 16 x 12; the arrow (97,216,15,8) and the result slot
  // (71,216,26,26), shared with the inventory.
  clear(69, 18, 16, 12); steelButton(g, 69, 18, 16, 12, false, 1.2);
  clear(63, 30, 16, 12); steelButton(g, 63, 30, 16, 12, true, 1.2);
  // Both are blitted unblended inside the open tab: no glow (it spilled into the arrow's box as a line and showed
  // as orange caps on the frame's corners), an opaque arrow, and binary alpha (below).
  clear(97, 216, 15, 8); craftArrow(g, 97, 216, 15, 8, '#2a1a0c', 'down');   // the original points down to the result
  clear(71, 216, 26, 26); resultFrame(g, 71, 216, 26, 2.6, false);
  // Gauges: tank and battery bar pieces (29,30 / 29,48 / 29,66, 18 x 18), glass overlays (47,30 / 47,56, 16 x 18).
  clear(29, 30, 18, 54);
  gaugeFrame(g, 29, 30, 18, 18, 'top'); gaugeFrame(g, 29, 48, 18, 18, 'middle'); gaugeFrame(g, 29, 66, 18, 18, 'bottom');
  clear(47, 30, 16, 18); gaugeGlass(g, 47, 30, 16, 18, 'liquid');
  clear(47, 56, 16, 18); gaugeGlass(g, 47, 56, 16, 18, 'power');
  // Tab art (128..256): canvas flaps, raised (they are controls), stitched along the top, right, bottom.
  clear(128, 0, 128, 256);
  tabArt(g);
  // The tabs and the upgrade pouch are blitted unblended too: no soft edges there. Not the whole sheet: the gauge glass
  // (47, 30) and other overlays are drawn blended over fluid and need their translucency.
  solidAlpha(c, [128, 0, 128, 256]); solidAlpha(c, [0, 0, 27, 204]); solidAlpha(c, [0, 204, 7, 18]);
  solidAlpha(c, [71, 216, 26, 26]); solidAlpha(c, [97, 216, 15, 8]);   // the crafting tab's result frame and arrow
  return c;
}

/** The tab art: the upgrade pouch's design (grain, dark edge, seam along the top, right and bottom, ending
 *  where the tab meets the panel) in tan leather. Tan, not the pouch's dark brown: the game draws open tab
 *  titles at (+20, +8) in fixed #404040, and a separate light title band cannot work, because the mod builds
 *  a closed tab from the top 12 and bottom 12 rows of this same art and would cut the band in half behind
 *  its icon. The mod glues the halves at any height and cuts any width, so the body is flat with grain
 *  along x only. A lit top and shaded bottom make it read raised (tabs are controls). */
function tabArt(g) {
  // The tabs in the side pouch's dark leather and style (same fill, grain, dark edge, stitching); no bright rim
  // (it bunched into a white clump where the edge rounds at the top right).
  const x0 = 128, w = 128;
  const body = () => rrect(g, x0, 0, w, 256, [0, 2.4, 2.4, 0]);
  body(); g.fillStyle = hgrad(g, x0, 256, [[0, '#3a2619'], [1, '#4a3122']]); g.fill();
  surface(g, body, TEX.grain, 0.1);
  g.strokeStyle = 'rgba(0,0,0,0.55)'; g.lineWidth = 0.4;
  g.beginPath(); g.moveTo(x0 + 3, 0.2); g.lineTo(256 - 2.4, 0.2); g.arcTo(255.8, 0.2, 255.8, 2.4, 2.2); g.lineTo(255.8, 253.6);
  g.arcTo(255.8, 255.8, 253.6, 255.8, 2.2); g.lineTo(x0 + 3, 255.8); g.stroke();
  stitch(g, () => {
    g.beginPath(); g.moveTo(x0, 2.2); g.lineTo(252.8, 2.2); g.arcTo(253.8, 2.2, 253.8, 3.2, 1); g.lineTo(253.8, 252.8);
    g.arcTo(253.8, 253.8, 252.8, 253.8, 1); g.lineTo(x0, 253.8);
  });
  // The strip the mod lays over the panel edge to join a tab (x 128 to 131): transparent, so the tab sits
  // flush against the panel's own edge.
  g.clearRect(x0, 0, 3, 256);
}

/** icons: the pack's sheet with the icons this screen uses redrawn flat (one solid tone, no shading)
 *  (transfer and sort arrows, filtered arrows with a dotted stem, sort-by letters in the game's font,
 *  the settings cog, the linked storage markers) and the upgrade switch as a lever that glows ember when on. */
function bpIcons(orig, font) {
  const [c, g] = hires(256, 256);
  g.imageSmoothingEnabled = false;
  if (orig) g.drawImage(orig, 0, 0, 256, 256);
  const glyph = (x, y, draw) => {
    g.clearRect(x, y, 12, 12);
    g.save(); g.translate(x, y); draw(); g.fillStyle = ICON_FLAT; g.fill(); g.restore();
  };
  const arrow = (up, dotted) => () => {
    g.beginPath();
    // 4.4 x 5.6, centred in the 12 px cell, so it sits inside the inset button face with room around it.
    const head = up ? [[3.8, 5.4], [6, 3.2], [8.2, 5.4]] : [[3.8, 6.6], [6, 8.8], [8.2, 6.6]];
    for (const [px, py] of head) g.lineTo(px, py);
    g.closePath();
    if (dotted) for (const y of up ? [6, 7.2, 8.2] : [3.2, 4.2, 5.4]) g.rect(5.4, y, 1.2, 0.6);
    else g.rect(5.4, up ? 5.2 : 3.2, 1.2, 3.6);
  };
  glyph(0, 144, arrow(true, false));     // sort, and transfer to storage
  glyph(36, 156, arrow(true, true));     // transfer to storage, filtered
  glyph(12, 156, arrow(false, false));   // transfer to inventory
  glyph(24, 156, arrow(false, true));    // transfer to inventory, filtered
  // Sort-by letters: name A, tags T, count 1, mod M, in the game's font, steel.
  if (font) {
    for (const [x, y, ch] of [[24, 144, 'A'], [12, 144, 'T'], [36, 144, '1'], [0, 156, 'M']]) {
      g.clearRect(x, y, 12, 12);
      // At 0.75 size (caps 5.25 tall), centred, to sit inside the inset button face.
      const k = font.cell, i = ch.charCodeAt(0), sx = (i % 16) * k, sy = Math.floor(i / 16) * k, dx = x + (12 - (font.adv[i] - 1) * 0.75) / 2;
      for (const [ox, oy, col] of [[0, 0, ICON_FLAT]]) {
        const t = document.createElement('canvas'); t.width = t.height = k;
        const tg = t.getContext('2d'); tg.drawImage(font.img, sx, sy, k, k, 0, 0, k, k);
        tg.globalCompositeOperation = 'source-in'; tg.fillStyle = col; tg.fillRect(0, 0, k, k);
        g.drawImage(t, dx + ox * 0.8, y + 3.4 + oy * 0.8, 6, 6);
      }
    }
  }
  // Tab toggles (16 x 16) and result arrows (8 x 12), redrawn as flat vector shapes with the originals'
  // meanings: crafting shift-click target (into the backpack / inventory), refill grid (yes / no); pump
  // input / output, tanks and pipes, world, hand (each with a "not" sign when off); previous / next result.
  const RUST = '#b0503a';
  const icon16 = (x, y, draw, not) => {
    g.clearRect(x, y, 16, 16);
    g.save(); g.translate(x, y); g.fillStyle = g.strokeStyle = ICON_FLAT; g.lineCap = 'butt'; draw();
    if (not) {
      g.strokeStyle = RUST; g.lineWidth = 1.3;
      g.beginPath(); g.arc(8, 8, 5.6, 0, Math.PI * 2); g.stroke();
      g.beginPath(); g.moveTo(4, 12); g.lineTo(12, 4); g.stroke();
    }
    g.restore();
  };
  const arrowL = () => { g.beginPath(); g.moveTo(2, 8); g.lineTo(6.5, 3.5); g.lineTo(6.5, 6.8); g.lineTo(13.5, 6.8); g.lineTo(13.5, 9.2); g.lineTo(6.5, 9.2); g.lineTo(6.5, 12.5); g.closePath(); g.fill(); };
  const arrowR = () => { g.save(); g.translate(16, 0); g.scale(-1, 1); arrowL(); g.restore(); };
  const arrowD = () => { g.save(); g.translate(8, 8); g.rotate(-Math.PI / 2); g.translate(-8, -8); arrowL(); g.restore(); };
  const grid = () => { for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) g.fillRect(8 + 2.2 * i, 4.7 + 2.2 * j, 1.6, 1.6); };
  const pipe = () => { g.fillRect(2, 4, 1.6, 8); g.fillRect(3.6, 7, 3.4, 2); };
  icon16(32, 48, arrowL); icon16(48, 48, arrowD);
  icon16(32, 112, () => { g.beginPath(); g.moveTo(7, 8); g.lineTo(4.4, 5.6); g.lineTo(4.4, 7.2); g.lineTo(1.8, 7.2); g.lineTo(1.8, 8.8); g.lineTo(4.4, 8.8); g.lineTo(4.4, 10.4); g.closePath(); g.fill(); grid(); });
  icon16(48, 112, () => { grid(); g.strokeStyle = RUST; g.lineWidth = 1.3; g.beginPath(); g.moveTo(6.5, 12); g.lineTo(15, 3.5); g.stroke(); });
  icon16(144, 0, arrowL); icon16(160, 0, arrowR);
  const tank = () => { pipe(); g.lineWidth = 1; g.strokeRect(8.5, 3.5, 5, 9); g.fillRect(9.5, 8.5, 3, 3); };
  const world = () => { pipe(); for (const [cx, cy] of [[10, 6.5], [12.6, 9.5], [9.6, 11.4]]) { g.beginPath(); g.arc(cx, cy, 1.1, 0, Math.PI * 2); g.fill(); } };
  const hand = () => { pipe(); g.lineWidth = 1; g.strokeRect(8.5, 5, 5, 6); };
  icon16(0, 112, tank); icon16(16, 112, tank, true);
  icon16(176, 0, world); icon16(192, 0, world, true);
  icon16(208, 0, hand); icon16(224, 0, hand, true);
  // Linked storage markers (Sophisticated Core 1.5.7, the Ender Linker; LinkedStorageEndpointRoleRenderer), 16 x 16:
  // primary endpoint (112,112), secondary (128,112), and the broken link the sheet carries at (144,112) (no code
  // draws it in 1.5.7). An ender orb: the primary inside a ring (the hub), the secondary alone, the broken one cracked.
  const orb = r => { circle(g, 8, 8, r); g.fill(); };
  icon16(112, 112, () => { orb(3.2); g.lineWidth = 0.9; circle(g, 8, 8, 5.6); g.stroke(); });
  icon16(128, 112, () => orb(3.8));
  icon16(144, 112, () => {
    orb(3.8);
    g.save(); g.globalCompositeOperation = 'destination-out'; g.lineWidth = 1; g.lineJoin = 'miter';
    g.beginPath(); g.moveTo(7, 3.4); g.lineTo(9.2, 6.6); g.lineTo(6.8, 9.2); g.lineTo(9, 12.6); g.stroke(); g.restore();
  });
  g.clearRect(48, 144, 16, 12);
  g.fillStyle = ICON_FLAT;
  g.beginPath(); g.moveTo(53.6, 3 + 144); g.lineTo(50.4, 6 + 144); g.lineTo(53.6, 9 + 144); g.closePath(); g.fill();
  g.beginPath(); g.moveTo(58.4, 3 + 144); g.lineTo(61.6, 6 + 144); g.lineTo(58.4, 9 + 144); g.closePath(); g.fill();
  // Settings (16,96), 16 x 16: a flat cog with a cut-out hub.
  g.clearRect(16, 96, 16, 16);
  cogPath(g, 24, 104, 6.6, 5, 10); g.fillStyle = ICON_FLAT; g.fill();
  g.save(); g.globalCompositeOperation = 'destination-out'; circle(g, 24, 104, 2); g.fill(); g.restore();
  // Upgrade switch, 4 x 10: off (0,128), on (4,128), inactive (8,128). A lever knob: down and dark when off,
  // up and lit ember when on, down and grey when inactive.
  g.clearRect(0, 128, 12, 10);
  for (const [x, up, col] of [[0, false, '#5a2a1e'], [4, true, '#ffb040'], [8, false, '#5d6166']]) {
    const ky = up ? 128.6 : 133.4;
    rrect(g, x + 0.4, ky, 3.2, 4, 0.8); g.fillStyle = col; g.fill();
  }
  // A thin dark outline on every icon: the buttons under them follow dark mode, so a face can be light grey.
  outlineCanvas(c, 0.4, [[0, 144, 48, 24], [48, 144, 16, 12], [32, 48, 32, 16], [0, 112, 64, 16], [144, 0, 96, 16], [16, 96, 16, 16], [112, 112, 48, 16]]);
  return c;
}

/** Placeholder upgrade item icons (item art is not part of these textures): the same in both views. */
function upgradeIcon(kind) {
  const [c, g] = hires(16, 16);
  if (kind === 'crafting') {
    rrect(g, 2, 3, 12, 11, 1); g.fillStyle = '#8a6a44'; g.fill();
    g.fillStyle = '#3a2a1a'; for (const k of [0, 1, 2]) g.fillRect(4 + k * 3, 5, 2.4, 7);
  } else if (kind === 'magnet') {
    g.beginPath(); g.arc(8, 7, 4.5, Math.PI, 0); g.lineTo(12.5, 13); g.lineTo(10, 13); g.lineTo(10, 7);
    g.arc(8, 7, 2, 0, Math.PI, true); g.lineTo(6, 13); g.lineTo(3.5, 13); g.closePath(); g.fillStyle = '#b03030'; g.fill();
    g.fillStyle = '#d8d8d8'; g.fillRect(3.5, 11, 2.5, 2); g.fillRect(10, 11, 2.5, 2);
  } else if (kind === 'pump') {
    rrect(g, 3, 4, 8, 9, 1); g.fillStyle = '#7d8288'; g.fill();
    g.fillStyle = '#4a4e53'; g.fillRect(11, 6, 3, 2); g.fillRect(12.5, 6, 1.5, 5);
    g.fillStyle = '#3c78d8'; g.fillRect(12.6, 11.5, 1.3, 2);
  } else {
    rrect(g, 4, 2, 8, 12, 1.5); g.fillStyle = '#9aa0a6'; g.fill();
    g.fillStyle = '#3c78d8'; g.fillRect(5.2, 7, 5.6, 5.8);
    g.fillStyle = 'rgba(255,255,255,0.45)'; g.fillRect(5.6, 3.4, 1, 9);
  }
  return c;
}
