// The Sophisticated Backpacks screen, assembled the way the mod does (Sophisticated Core 1.4.90 and
// Backpacks 3.25.78: StorageScreenBase, StorageGuiHelper, GuiHelper, SettingsTabControl,
// CraftingUpgradeTab, PumpUpgradeTab, TankInventoryControl), from whichever texture set is given, so the
// same code shows the pack's art and ours. All positions relative to the panel.

const BP_TIERS = {
  leather: { name: 'Backpack', slots: 27, upgrades: 1 },
  copper: { name: 'Copper Backpack', slots: 45, upgrades: 1 },
  iron: { name: 'Iron Backpack', slots: 54, upgrades: 2 },
  gold: { name: 'Gold Backpack', slots: 81, upgrades: 3 },
  diamond: { name: 'Diamond Backpack', slots: 108, upgrades: 5 },
  netherite: { name: 'Netherite Backpack', slots: 120, upgrades: 7 },
};
/** Upgrade sets to show: items work (crafting, magnet) or fluids (tank, pump). The tank takes two storage
 *  columns (TankInventoryControl, 36 wide with the tank centred) and has no tab. */
const BP_SETS = { items: ['crafting', 'magnet'], fluids: ['pump', 'tank'] };
const BP_TABBED = ['crafting', 'magnet', 'pump'], BP_SWITCHED = ['magnet', 'pump'];

/** Layout for a tier and its installed upgrades; maxRows limits the visible rows (a small screen), which
 *  switches to the wider texture and a scrollbar. */
function bpLayout(tier, set, maxRows = Infinity) {
  const t = BP_TIERS[tier], installed = BP_SETS[set].slice(0, t.upgrades);
  const taken = installed.includes('tank') ? 2 : 0;
  let cols = 9, rows = Math.ceil(t.slots / (9 - taken));
  if (t.slots + taken * rows > 81) { cols = 12; rows = Math.ceil(t.slots / (12 - taken)); }
  const perRow = cols - taken, visRows = Math.min(rows, maxRows), wider = visRows < rows;
  const { W, off } = bpVariant(cols, wider);
  const H = 114 + visRows * 18, S = visRows * 18, h = Math.min(S / 2, 150);
  return { ...t, installed, taken, cols, perRow, rows, visRows, wider, W, H, S, h, off, key: `${cols}${wider ? '_wider' : ''}` };
}

/** A tab background at (x, y), w x h, cut from the tab art as GuiHelper.renderTabBackground does. */
function drawTab(put, controls, x, y, w, h) {
  const top = Math.floor(h / 2), bot = Math.ceil(h / 2);
  if (w <= 128) {
    put(controls, x, y, w, top, 256 - w, 0, w, top);
    put(controls, x, y + top, w, bot, 256 - w, 256 - bot, w, bot);
    put(controls, x - 3, y, 3, h, 128, 256 - h, 3, h);
  } else {
    for (const [sy, dy, hh] of [[0, 0, top], [256 - bot, top, bot]]) {
      put(controls, x, y + dy, 6, hh, 128, sy, 6, hh);
      for (let cx = 6; cx < w - 6; cx += 116) { const cw = Math.min(116, w - 6 - cx); put(controls, x + cx, y + dy, cw, hh, 134, sy, cw, hh); }
      put(controls, x + w - 6, y + dy, 6, hh, 250, sy, 6, hh);
    }
  }
}
/** An 18 x 18 toggle button with its 16 x 16 icon at +1, +1 (ToggleButton over ButtonDefinitions). */
function toggleButton(put, T, x, y, [u, v]) {
  put(T.controls, x, y, 18, 18, 29, 0, 18, 18);
  put(T.iconSheet, x + 1, y + 1, 16, 16, u, v, 16, 16);
}

/** The whole screen for layout L and texture set T at scale.
 *  state: { open: null | 'crafting' | 'pump', results, showSlots }. */
/** A tab's slot group (cols x rows at x, y): before, the mod's cells; after, haven-create-ui-ux's scroll (see
 *  upgradeSlotsBackground). */
function tabSlots(put, T, x, y, cols, rows) {
  const w = cols * 18, h = rows * 18;
  if (!T.tabSlots) { put(T.slots, x, y, w, h, 0, 0, w, h); return; }
  put(T.tabSlots, x - 1, y - 1, w + 1, h + 1, 0, 0, w + 1, h + 1);
  put(T.tabSlots, x + w, y - 1, 1, h + 2, 0, 0, 1, h + 2);
  put(T.tabSlots, x - 1, y + h, w + 2, 1, 0, 0, w + 2, 1);
  for (const ry of [y - 2, y + h]) {
    put(T.tabSlots, x - 2, ry, 1, 2, 0, 220, 1, 2); put(T.tabSlots, x - 1, ry, w + 2, 2, 1, 220, w + 2, 2);
    put(T.tabSlots, x + w + 1, ry, 1, 2, 235, 220, 1, 2);
  }
}
function bpCompose(T, L, scale, state) {
  const OPEN = { crafting: [63, 142], pump: [81, 68] }, open = L.installed.includes(state.open) ? state.open : null;
  const tabW = open ? OPEN[open][0] : 21;
  const ox = 26, oy = 2, cw = ox + L.W + tabW + 4, ch = Math.max(L.H, 4 + 25 * 3 + 142) + oy + 2;
  const { c, g, put, text } = screenCanvas(cw, ch, scale);
  const P = (x, y) => [ox + x, oy + y];
  // Background: the top 17 + h rows, then the bottom 97 + h rows (StorageGuiHelper.renderStorageBackground).
  const bg = T.bg[L.key];
  put(bg, ...P(0, 0), L.W, 17 + L.h, 0, 0, L.W, 17 + L.h);
  put(bg, ...P(0, 17 + L.h), L.W, 97 + L.h, 0, 256 - (97 + L.h), L.W, 97 + L.h);
  // Storage slots from slots_background at (7, 17): full rows, then a partial last row.
  const full = Math.min(L.visRows, Math.floor(L.slots / L.perRow)), extra = L.visRows > full ? L.slots - full * L.perRow : 0;
  put(T.slots, ...P(7, 17), L.perRow * 18, full * 18, 0, 0, L.perRow * 18, full * 18);
  if (extra) put(T.slots, ...P(7, 17 + full * 18), extra * 18, 18, 0, 0, extra * 18, 18);
  // Tank: two columns after the slots, the tank centred (x + 9): top, middles, bottom, fluid, glass overlay.
  if (L.taken) {
    const tx = 7 + L.perRow * 18 + 9, th = L.visRows * 18;
    put(T.controls, ...P(tx, 17), 18, 18, 29, 30, 18, 18);
    for (let i = 0; i < (th - 36) / 18; i++) put(T.controls, ...P(tx, 35 + 18 * i), 18, 18, 29, 48, 18, 18);
    put(T.controls, ...P(tx, 17 + th - 18), 18, 18, 29, 66, 18, 18);
    const lvl = Math.round((th - 2) * 0.6), [fx, fy] = P(tx + 1, 17 + 1 + th - 2 - lvl);
    g.fillStyle = 'rgba(50,100,200,0.85)'; g.fillRect(fx * scale, fy * scale, 16 * scale, lvl * scale);   // water (fluid art is the game's)
    for (let i = 0; i < th / 18; i++) put(T.controls, ...P(tx + 1, 17 + 18 * i), 16, 18, 47, 30, 16, 18);
  }
  // Scrollbar: NeoForge ScrollPanel draws it with colour fills (not restylable by a pack).
  if (L.wider) {
    const [sx, sy] = P(7 + L.cols * 18, 17), bh = L.visRows * 18, th = Math.max(32, bh * bh / (L.rows * 18));
    g.fillStyle = '#000'; g.fillRect(sx * scale, sy * scale, 6 * scale, bh * scale);
    g.fillStyle = '#808080'; g.fillRect(sx * scale, sy * scale, 6 * scale, th * scale);
    g.fillStyle = '#c0c0c0'; g.fillRect(sx * scale, sy * scale, 5 * scale, (th - 1) * scale);
  }
  // Upgrade column at x -21 (SSB.drawUpgradeBackground): top, cropped middle, bottom; switches on toggleables.
  const N = L.upgrades, HwB = 6 + 16 * N;   // the tier's upgrade slot count
  put(T.controls, ...P(-21, 0), 26, 4, 0, 0, 26, 4);
  put(T.controls, ...P(-21, 4), 25, HwB - 4, 0, 4, 25, HwB - 4);
  put(T.controls, ...P(-21, HwB), 25, 6, 0, 198, 25, 6);
  L.installed.forEach((k, i) => {
    put(T.icons[k], ...P(-15, 6 + 16 * i), 16, 16);
    if (BP_SWITCHED.includes(k)) {
      put(T.controls, ...P(-25, 5 + 16 * i), 7, 18, 0, 204, 7, 18);
      put(T.controls, ...P(-22, 8 + 16 * i), 6, 12, 65, 0, 6, 12);
      put(T.iconSheet, ...P(-21, 9 + 16 * i), 4, 10, 4, 128, 4, 10);
    }
  });
  // Tabs at (W, 4 + 25 i): settings, then one per upgrade with settings. Closed ones first, the open one on top.
  const tabs = ['settings', ...L.installed.filter(k => BP_TABBED.includes(k))];
  tabs.forEach((k, i) => {
    if (k === open) return;
    const [x, y] = P(L.W, 4 + 25 * i);
    drawTab(put, T.controls, x, y, 21, 24);
    if (k === 'settings') put(T.iconSheet, x + 1, y + 4, 16, 16, 16, 96, 16, 16); else put(T.icons[k], x + 1, y + 4, 16, 16);
  });
  if (open) {
    const [x, y] = P(L.W, 4 + 25 * tabs.indexOf(open)), [w, h] = OPEN[open];
    drawTab(put, T.controls, x, y, w, h);
    put(T.icons[open], x + 1, y + 4, 16, 16);
    text(open === 'crafting' ? 'Crafting' : 'Pump', x + 20, y + 8);
    if (open === 'crafting') {
      // CraftingUpgradeTab: two toggles (shift-click target, refill grid), the 3x3 grid, arrow, result.
      toggleButton(put, T, x + 3, y + 24, [32, 48]); toggleButton(put, T, x + 21, y + 24, [32, 112]);
      tabSlots(put, T, x + 3, y + 44, 3, 3);
      put(T.controls, x + 22, y + 101, 15, 8, 97, 216, 15, 8);
      put(T.controls, x + 17, y + 111, 26, 26, 71, 216, 26, 26);
      if (state.results) {   // only when the recipe has several results (shouldShowResultSelection)
        put(T.controls, x + 22, y + 99, 16, 12, 69, 18, 16, 12);
        for (const [bx, u] of [[9, 48], [43, 56]]) {
          put(T.controls, x + bx, y + 118, 8, 12, 53, 18, 8, 12); put(T.iconSheet, x + bx, y + 118, 8, 12, u, 144, 8, 12);
        }
      }
    } else {
      // PumpUpgradeTab: input/output, tanks and pipes, world, hand toggles; four fluid filter slots.
      [[144, 0], [0, 112], [176, 0], [208, 0]].forEach((uv, i) => toggleButton(put, T, x + 3 + 18 * i, y + 24, uv));
      tabSlots(put, T, x + 3, y + 44, 4, 1);
    }
  }
  // Title (8, 6) and "Inventory" (8 + off, H - 94), the game's grey, no shadow.
  text(L.name, ...P(8, 6));
  text('Inventory', ...P(8 + L.off, L.H - 94));
  // Search box, collapsed: a solid fill, 10 x 10, right-anchored at x W - 32 (not restylable).
  const [qx, qy] = P(L.W - 42, 5);
  g.fillStyle = '#777777'; g.fillRect(qx * scale, qy * scale, 10 * scale, 10 * scale);
  // Its hint: U+1F50D written centred in #BBBBBB with the game's text shadow. Ours: our glyph (createhud:font/search);
  // before: an approximation of the game's fallback-font magnifier.
  if (T.searchGlyph) {
    const tint = (col, dx) => {
      const t = document.createElement('canvas'); t.width = T.searchGlyph.width; t.height = T.searchGlyph.height;
      const tg = t.getContext('2d'); tg.drawImage(T.searchGlyph, 0, 0); tg.globalCompositeOperation = 'source-in';
      tg.fillStyle = col; tg.fillRect(0, 0, t.width, t.height);
      g.imageSmoothingEnabled = false; g.drawImage(t, (qx + 1 + dx) * scale, (qy + 1 + dx) * scale, 8 * scale, 8 * scale);
    };
    tint('#2e2e2e', 1); tint('#bbbbbb', 0);
  } else {
    g.strokeStyle = '#bbbbbb'; g.lineWidth = 0.8 * scale;
    g.beginPath(); g.arc((qx + 4.4) * scale, (qy + 4.4) * scale, 2 * scale, 0, Math.PI * 2); g.stroke();
    g.beginPath(); g.moveTo((qx + 5.9) * scale, (qy + 5.9) * scale); g.lineTo((qx + 7.8) * scale, (qy + 7.8) * scale); g.stroke();
  }
  // Sort and sort-by (TITLE_LINE_RIGHT), transfer buttons beside the "Inventory" label: small buttons + icons.
  for (const [x, y, ix, iy] of [[L.W - 31, 4, 0, 144], [L.W - 19, 4, 24, 144],
    [8 + L.off + 137, L.H - 96, 0, 144], [8 + L.off + 149, L.H - 96, 12, 156]]) {
    put(T.controls, ...P(x, y), 12, 12, 29, 18, 12, 12);
    put(T.iconSheet, ...P(x, y), 12, 12, ix, iy, 12, 12);
  }
  if (state.showSlots) {
    g.strokeStyle = 'rgba(255,255,255,0.6)'; g.setLineDash([3, 3]);
    const box = (x, y, w = 16, h = 16) => { const [a, b] = P(x, y); g.strokeRect(a * scale + 0.5, b * scale + 0.5, w * scale - 1, h * scale - 1); };
    for (let i = 0; i < Math.min(L.slots, L.visRows * L.perRow); i++) box(8 + 18 * (i % L.perRow), 18 + 18 * Math.floor(i / L.perRow));
    for (let i = 0; i < 3; i++) for (let j = 0; j < 9; j++) box(L.off + 8 + 18 * j, L.H - 82 + 18 * i);
    for (let j = 0; j < 9; j++) box(L.off + 8 + 18 * j, L.H - 24);
    for (let i = 0; i < N; i++) box(-15, 6 + 16 * i);
  }
  return c;
}
