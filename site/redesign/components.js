// Shared UI components: parts that recur across the pack's screens, drawn once so every screen reads as
// one system. The crafting set (tray, arrow, result frame), steel buttons and switches, and gauges (liquid
// tanks, power cells, in the glass-tube style of the HUD's experience pipe). Uses sprites.js, material.js
// and panel-art.js. Rule: decoration is flat or inlaid; only controls (buttons, switches) look raised.

/** A rectangle with chamfered corners (the crafting table's octagon motif). */
function chamfer(g, x, y, w, h, c) {
  g.beginPath();
  g.moveTo(x + c, y); g.lineTo(x + w - c, y); g.lineTo(x + w, y + c); g.lineTo(x + w, y + h - c);
  g.lineTo(x + w - c, y + h); g.lineTo(x + c, y + h); g.lineTo(x, y + h - c); g.lineTo(x, y + c); g.closePath();
}

/** Crafting: an inlaid steel tray under a grid, chamfered; pass the grid's outer box plus a margin. */
function craftTray(g, x, y, w, h, c = 1.8) {
  const p = () => chamfer(g, x, y, w, h, c);
  p(); g.fillStyle = vgrad(g, y, y + h, [[0, '#3a3d41'], [1, '#2a2c2f']]); g.fill();
  inlay(g, p, y, y + h);
}
/** Crafting: a burnt-in arrow filling a w x h box, pointing right, or down (dir 'down': a broad head, as
 *  screens with the result below the grid use). */
function craftArrow(g, x, y, w, h, color = 'rgba(40,22,8,0.8)', dir = 'right') {
  g.beginPath();
  if (dir === 'down') {
    const cx = x + w / 2, sy = y + h * 0.45, shaft = w * 0.14;
    g.moveTo(cx - shaft, y); g.lineTo(cx + shaft, y); g.lineTo(cx + shaft, sy); g.lineTo(x + w, sy); g.lineTo(cx, y + h);
    g.lineTo(x, sy); g.lineTo(cx - shaft, sy); g.closePath();
  } else {
    const sx = w * 0.53, cy = y + h / 2, shaft = h * 0.22;
    g.moveTo(x, cy - shaft); g.lineTo(x + sx, cy - shaft); g.lineTo(x + sx, y); g.lineTo(x + w, cy); g.lineTo(x + sx, y + h);
    g.lineTo(x + sx, cy + shaft); g.lineTo(x, cy + shaft); g.closePath();
  }
  g.fillStyle = color; g.fill();
}
/** Crafting: the result slot, an iron frame with chamfered corners and a faint ember glow around a cell.
 *  (x, y, s) is the frame's box; the cell (s - 4.6, centred) holds the item. glow: false on a sprite sheet the mod
 *  blits unblended (the glow spills past the box and its soft pixels show at full colour). */
function resultFrame(g, x, y, s, c = 2.2, glow = true) {
  g.save();
  if (glow) { g.shadowColor = 'rgba(255,140,30,0.55)'; g.shadowBlur = 1.6; }
  chamfer(g, x, y, s, s, c); g.fillStyle = '#2b2d30'; g.fill();
  g.restore();
  const inner = s - 4.6;
  cell(g, x + 2.3, y + 2.3, undefined, inner);
}

/** A raised steel push button (the only raised things on a panel are controls). Everything stays inside
 *  its w x h box, shadow included: mods place buttons 1 px from slots. */
function steelButton(g, x, y, w, h, hover, r = Math.min(w, h) * 0.12) {
  const b = () => rrect(g, x + 0.3, y + 0.2, w - 0.6, h - 0.9, r);
  rrect(g, x + 0.3, y + 0.5, w - 0.6, h - 0.8, r); g.fillStyle = 'rgba(0,0,0,0.45)'; g.fill();
  b(); g.fillStyle = hover ? vgrad(g, y, y + h, [[0, '#7d8288'], [1, '#2c2f33']]) : ironBezel(g, y, y + h); g.fill();
  rim(g, b, y + 0.2, y + h - 0.7, 'rgba(230,234,238,0.6)', 0.3);
  if (hover) { b(); g.strokeStyle = 'rgba(255,190,90,0.7)'; g.lineWidth = 0.3; g.stroke(); }
}
/** A switch slot: a sunken steel channel a lever or icon sits in. */
function switchSlot(g, x, y, w, h, hover) {
  rrect(g, x + 0.3, y + 0.3, w - 0.6, h - 0.6, 0.8); g.fillStyle = hover ? '#4a4e53' : ironBezel(g, y, y + h); g.fill();
  rrect(g, x + 0.9, y + 0.9, w - 1.8, h - 1.8, 0.5); g.fillStyle = '#0d0e10'; g.fill();
  g.fillStyle = 'rgba(0,0,0,0.6)'; g.fillRect(x + 0.9, y + 0.9, w - 1.8, 0.5);
}

/** A gauge's glass tube in a steel frame (the HUD's experience pipe, upright): the frame piece of a
 *  tiled bar; part is 'top', 'middle' or 'bottom'. */
function gaugeFrame(g, x, y, w, h, part) {
  const top = part === 'top' ? 1.2 : 0, bot = part === 'bottom' ? 1.2 : 0;
  g.fillStyle = hgrad(g, x, x + w, [[0, '#26282b'], [0.25, '#7d8288'], [0.6, '#45484c'], [1, '#1a1b1d']]);
  g.fillRect(x, y, w, h);
  g.fillStyle = '#060708'; g.fillRect(x + 1, y + top, w - 2, h - top - bot);
  if (part === 'top') { g.fillStyle = 'rgba(230,234,238,0.5)'; g.fillRect(x, y, w, 0.4); }
  if (part === 'bottom') { g.fillStyle = 'rgba(0,0,0,0.6)'; g.fillRect(x, y + h - 0.5, w, 0.5); }
}
/** A gauge's glass over the fill: tick marks down both sides (0.5 wide, so they
 *  survive nearest sampling); kind 'liquid' or 'power' (power adds copper bands, like a cell). */
function gaugeGlass(g, x, y, w, h, kind) {
  // No reflection stripe: a thin bright line beside the frame reads as a defect, not as glass.
  // Ticks down both sides, long and short alternating (as the original gauge).
  g.fillStyle = 'rgba(220,226,232,0.45)';
  for (let ty = y + 2; ty < y + h; ty += 4) {
    const len = ty % 8 < 4 ? 3 : 1.5;
    g.fillRect(x + w - 1 - len, ty, len, 0.5); g.fillRect(x + 1, ty, len, 0.5);
  }
  if (kind === 'power') {
    g.fillStyle = 'rgba(184,110,60,0.55)';
    for (const ty of [y + 0.5, y + h - 1]) g.fillRect(x, ty, w, 0.5);
  }
}

/** A brushed steel nameplate for a title on an iron panel: mid grey, light enough for the game's fixed #404040
 *  titles without glaring; a soft top edge; no bolts, since titles start about 2 px into the plate. */
function steelNameplate(g, x, y, w, h) {
  const p = () => rrect(g, x, y, w, h, 0.8);
  p(); g.fillStyle = vgrad(g, y, y + h, [[0, '#a7acb0'], [1, '#92979b']]); g.fill();
  surface(g, p, TEX.brushed, 0.15);
  p(); g.strokeStyle = 'rgba(20,22,24,0.55)'; g.lineWidth = 0.4; g.stroke();
  rim(g, p, y, y + h, 'rgba(255,255,255,0.25)', 0.3);
}

/** The inventory's iron plate filling path() (y0..y1 its extent): flat dark iron, an edge a little lighter than
 *  the plate (no bright fade, no texture). The player's inventory section of every screen sits on it. */
function ironPlate(g, path, y0, y1) {
  path(); g.fillStyle = '#232426'; g.fill();   // flat: one tone, no texture (the rule for inventory sections)
  path(); g.strokeStyle = '#3a3c3f'; g.lineWidth = 0.8; g.stroke();
  rim(g, path, y0, y1, 'rgba(220,225,230,0.12)', 0.4);
}

/** The hotbar's brass outline (it echoes the HUD's), for a hotbar whose first slot's item sits at (x, y): 1 px wide (a
 *  whole pixel renders the same width on every side at any GUI scale; 0.5 came out 1 or 2 screen px by position),
 *  1 px clear of the cells, corners rounded 1.5. */
function hotbarOutline(g, x, y) {
  rrect(g, x - 2.5, y - 2.5, 165, 21, 1.5);
  g.strokeStyle = 'rgba(214,170,90,0.45)'; g.lineWidth = 1; g.stroke();
}

/** The player's inventory as every screen shows it, placed from its label's position (lx, ly), which the game
 *  sets (vanilla (8, imageHeight - 94)): a steel nameplate behind "Inventory" (50 px in the game font), the
 *  3 x 9 grid 12 px below the label, the hotbar 70 px below it in the brass outline that echoes the HUD.
 *  Draw it on the iron plate (ironPlate). One component, so every screen's inventory is identical. */
function playerInventory(g, lx, ly, { plate = true } = {}) {
  if (plate) steelNameplate(g, lx - 2.4, ly - 2, 53, 11);   // plate: false leaves "Inventory" on the iron itself
  cells(g, [0, 1, 2].flatMap(i => [...Array(9).keys()].map(j => [lx + 18 * j, ly + 12 + 18 * i])));
  cells(g, [...Array(9).keys()].map(j => [lx + 18 * j, ly + 70]));
  hotbarOutline(g, lx, ly + 70);
}
