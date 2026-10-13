// The player inventory panel (container/inventory, 176x166 of a 256x256 texture) in the HUD's dark
// steampunk theme. Uses the helpers in sprites.js and material.js. Layout from InventoryMenu and
// InventoryScreen (1.21.1) and the pack's mod buttons; keep these exact.
// Spirit of the current panel (Colourful Containers), in our style: its scrolls become Create schematics
// (blueprints on wooden rollers) for the armor column and for the offhand and recipe book, and its crafting
// table becomes a workbench top (see craftingTable). Storage slots are thin sunken cells as in vanilla;
// brass only as small accents (roller caps, a hairline on the hotbar row that echoes the HUD).
const INV = {
  w: 176, h: 166,
  armor: [0, 1, 2, 3].map(i => [8, 8 + i * 18]),
  offhand: [77, 62],
  craft: [0, 1].flatMap(i => [0, 1].map(j => [98 + j * 18, 18 + i * 18])),
  result: [154, 28],
  main: [0, 1, 2].flatMap(i => [...Array(9).keys()].map(j => [8 + j * 18, 84 + i * 18])),
  hotbar: [...Array(9).keys()].map(j => [8 + j * 18, 142]),
  model: [26, 8, 75, 78],        // the game draws the player centred in this box (x 50.5)
  window: [28.5, 8, 72.5, 78],    // the viewport we draw: narrower, symmetric about the player, so the
                                  // scrolls on either side can centre on their slots
  title: [97, 8],                 // "Crafting", drawn by the game in #404040
  book: [104, 61, 20, 18],        // recipe book button
  curios: [26, 8, 10, 10],        // Curios button (TOP_LEFT corner, offsets 0)
  cosmetic: [65, 67, 10, 10],     // Cosmetic Armor Reworked: open cosmetic armor
  cosToggle: [59, 72, 5, 5],      // Cosmetic Armor Reworked: toggle cosmetics on the client
};
/** Every item position (top left of the 16 x 16 item). */
const invSlots = () => [...INV.armor, INV.offhand, ...INV.craft, INV.result, ...INV.main, ...INV.hotbar];

/** The player viewport: a recessed window in a thin steel lip, lit faintly from below by a furnace glow. */
function viewport(g, [x0, y0, x1, y1]) {
  const x = x0, y = y0, w = x1 - x0, h = y1 - y0;
  rrect(g, x - 0.6, y - 0.6, w + 1.2, h + 1.2, 1); g.fillStyle = ironBezel(g, y, y + h); g.fill();
  rrect(g, x, y, w, h, 0.6);
  g.fillStyle = vgrad(g, y, y + h, [[0, '#050506'], [0.7, '#0c0b0a'], [1, '#22140a']]); g.fill();
}

/** The crafting table, a workbench top: oak planks in an L (the leg fills the space beside the offhand
 *  scroll, as the pack's table does), a flush title plank, an inlaid octagonal steel tray centred on the
 *  2x2 grid, a burnt-in arrow centred between grid and result, an iron-framed result, burnt-in tools
 *  centred on the leg, iron corner brackets. Decoration is flat or inlaid: only slots look pressable. */
function craftingTable(g) {
  const top = 3.6, bot = 56.6, legX = 126.6, legBot = 82.4, left = 93.5, right = 173;   // top level with the scrolls
  const board = () => {
    g.beginPath(); g.moveTo(left, top); g.lineTo(right, top); g.lineTo(right, legBot); g.lineTo(legX, legBot);
    g.lineTo(legX, bot); g.lineTo(left, bot); g.closePath();
  };
  g.save(); g.translate(0, 0.6); board(); g.fillStyle = 'rgba(0,0,0,0.6)'; g.fill(); g.restore();
  planks(g, board, left, top, right - left, legBot - top, 9.4);
  board(); g.strokeStyle = 'rgba(40,22,8,0.8)'; g.lineWidth = 0.5; g.stroke();
  // The title plank, flush: lighter wood under the game's grey title (text spans y 8 to 16).
  const plank = () => { g.beginPath(); g.rect(left + 0.5, top + 0.5, right - left - 1, 16.2 - top - 0.5); };
  plank(); g.fillStyle = vgrad(g, top, 16.2, [[0, '#b8915f'], [1, '#a07b4d']]); g.fill();
  surface(g, plank, TEX.brushed, 0.12);
  // The octagonal tray, inlaid and centred on the grid (cells 97..133 x 17..53): 0.8 px on every side, the
  // most the title allows above it, with chamfered corners.
  craftTray(g, 96.2, 16.2, 37.6, 37.6);
  cells(g, INV.craft);
  // The arrow centred between the tray (x 133.8) and the result frame (x 150.7), on the result's centre line.
  craftArrow(g, 133.8 + (150.7 - 133.8 - 9.5) / 2, 31.5, 9.5, 9);
  resultFrame(g, INV.result[0] - 2.3, INV.result[1] - 2.3, 20.6);
  // Tools burnt into the leg, centred on it.
  burnt(g, b => tools(b, (legX + right) / 2, (bot + legBot) / 2, 1.1));
  // Corner brackets, small enough (3.4 px arms) to stay clear of the title at x 97.
  for (const [x, y, sx, sy] of [[left, top, 1, 1], [right, top, -1, 1], [left, bot, 1, -1], [right, legBot, -1, -1], [legX, legBot, 1, -1]]) {
    bracket(g, x, y, sx, sy, 3.4, 1.3);
  }
}

/** container/inventory: the whole panel at logical 176 x 166. */
function inventoryPanel() {
  const [c, g] = hires(INV.w, INV.h);
  panelBody(g, 0, 0, INV.w, INV.h);

  viewport(g, INV.window);
  // Schematic scrolls, each centred on its slots (paper 2 px past the armor column, 1.6 px past the offhand
  // slot and the recipe book; rollers 1 px clear above and below). Empty-slot silhouettes come from the
  // game (container/slot/*, blueprint lines), so only the slot marks are baked in.
  scroll(g, 4, 3.6, 24, 78.8, 2.4);
  for (const [x, y] of INV.armor) draftMark(g, x - 1, y - 1);
  scroll(g, 73.4, 57.6, 53.2, 24.8, 2.4);
  draftMark(g, INV.offhand[0] - 1, INV.offhand[1] - 1);
  // The strip between the player and the table: a flat engraved cog, surface only (nothing to press).
  engrave(g, () => cogPath(g, 83.5, 30, 6, 4.6, 10));
  engrave(g, () => circle(g, 83.5, 30, 1.8));
  craftingTable(g);
  cells(g, INV.main);
  cells(g, INV.hotbar);
  // 0.5 wide on the half-pixel grid, so nearest sampling keeps it at GUI scale 2.
  g.strokeStyle = 'rgba(214,170,90,0.55)'; g.lineWidth = 0.5; g.strokeRect(6.25, 140.25, 163.5, 19.5);
  return c;
}

const GLYPHS = {
  // Curios: a pendant on a chain.
  curios: (g, w, h) => { g.beginPath(); g.arc(w / 2, h * 0.32, w * 0.24, Math.PI * 0.15, Math.PI * 0.85, true); g.stroke(); circle(g, w / 2, h * 0.68, w * 0.16); g.fill(); },
  // Cosmetic armor: a coat hanger.
  cosmetic: (g, w, h) => {
    g.beginPath(); g.moveTo(w * 0.2, h * 0.72); g.lineTo(w / 2, h * 0.42); g.lineTo(w * 0.8, h * 0.72); g.closePath(); g.stroke();
    g.beginPath(); g.arc(w / 2, h * 0.3, w * 0.1, Math.PI, Math.PI * 2.4); g.stroke();
  },
  toggle: (g, w, h) => { circle(g, w / 2, h / 2, w * 0.18); g.fill(); },
};

/** recipe_book/button 20x18: a steel push button holding a flat book icon (cover, page edge, spine, cog). */
function bookButton(hover) {
  const [c, g] = hires(20, 18);
  steelButton(g, 0.2, 0.2, 19.6, 17.6, hover, 1.4);
  // The book, flat (solid shapes, no shading): a cover, a page edge, a spine band, a cog on the cover.
  const x0 = 5.6, y0 = 3.8, w = 8.8, h = 10.4;
  g.fillStyle = '#e4dcc4'; g.fillRect(x0 + 0.8, y0 + 0.6, w, h);
  g.fillStyle = '#8c3326'; g.fillRect(x0, y0, w, h);
  g.fillStyle = '#5c1f17'; g.fillRect(x0, y0, 1.6, h);
  cogPath(g, x0 + 1.6 + (w - 1.6) / 2, y0 + h / 2, 2.1, 1.55, 8); g.fillStyle = hover ? '#ffd890' : '#d9b260'; g.fill();
  circle(g, x0 + 1.6 + (w - 1.6) / 2, y0 + h / 2, 0.6); g.fillStyle = '#8c3326'; g.fill();
  return c;
}

/** Empty-slot silhouettes (container/slot/*, 16x16), drawn by the game in empty slots: blueprint lines. */
const SLOT_ICONS = {
  helmet: g => {
    g.beginPath(); g.moveTo(3, 12); g.lineTo(3, 8); g.bezierCurveTo(3, 2.5, 13, 2.5, 13, 8); g.lineTo(13, 12);
    g.lineTo(10.6, 12); g.lineTo(10.6, 9.6); g.lineTo(5.4, 9.6); g.lineTo(5.4, 12); g.closePath();
  },
  chestplate: g => {
    // Breastplate with a rounded belly, a high collar, pauldrons standing off the shoulders.
    g.beginPath(); g.moveTo(4.2, 3.2); g.lineTo(6.4, 2.4); g.quadraticCurveTo(8, 4.4, 9.6, 2.4); g.lineTo(11.8, 3.2);
    g.lineTo(11.8, 10.4); g.quadraticCurveTo(8, 15.2, 4.2, 10.4); g.closePath();
    for (const sx of [1, -1]) {
      const x = sx > 0 ? 3.6 : 12.4;
      g.moveTo(x, 3); g.quadraticCurveTo(x - sx * 3.2, 3, x - sx * 3, 7.2); g.lineTo(x, 6.4); g.closePath();
    }
    g.moveTo(8, 5.6); g.lineTo(8, 12.6);
  },
  leggings: g => {
    g.beginPath(); g.moveTo(3.6, 2.5); g.lineTo(12.4, 2.5); g.lineTo(13, 14); g.lineTo(9.6, 14); g.lineTo(8, 6.8);
    g.lineTo(6.4, 14); g.lineTo(3, 14); g.closePath();
  },
  boots: g => {
    g.beginPath();
    for (const dx of [0, 6.6]) {
      g.moveTo(1.6 + dx, 5); g.lineTo(5.2 + dx, 5); g.lineTo(5.2 + dx, 10.6); g.lineTo(7.6 + dx, 11.4);
      g.lineTo(7.6 + dx, 13.6); g.lineTo(1.6 + dx, 13.6); g.closePath();
    }
  },
  shield: g => { g.save(); g.translate(0.5, 0.6); g.scale(15 / 9, 15 / 9); shieldPath(g); g.restore(); },
};
function slotIcon(kind) {
  const [c, g] = hires(16, 16);
  const path = () => SLOT_ICONS[kind](g);
  // A dotted ghost, so an empty slot never reads as filled. Dots and gaps 0.5 long and the line 0.5 wide:
  // nearest sampling at GUI scale 2 lands one sample on each dot.
  g.setLineDash([0.5, 0.5]); g.lineCap = 'butt';
  path(); g.strokeStyle = 'rgba(200,220,240,0.32)'; g.lineWidth = 0.5; g.stroke();
  return c;
}
