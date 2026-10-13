// Mod HUD elements near the hotbar, placed by each mod's code (1.21.1, read from the bytecode) and drawn in our style.
// Positions use cx (screen centre x) and bottom (screen height) in GUI pixels; UVs are over each texture's logical
// 256 x 256 (an R x sheet works: the mods blit with fixed texture sizes). Needs sprites.js, meters.js, mcfont.js.
//
// Block Factory's Bosses dodge roll (ClientEvents.renderGUI, RenderGuiEvent.Pre, so under the hotbar). Instance
// config HOTBAR_RIGHT, offsets 0: icon 18 x 18 at (cx + 92, bottom - 19), textures/gui/roll/dash_<tier>.png; the
// count (int) t at (cx + 110, bottom - 10), outlined 1 px in black, in the tier colour. t is the charges available
// (n = 2): t - 1 >= 0.75 (n - 1) green, >= 0.5 yellow, >= 0.25 orange, t >= 1 red, else grey.
// Iron's Spells 'n Spellbooks mana (ManaBarOverlay, above the air layer). Instance: Contextual (a staff or wand in
// hand, or mana not full), anchor Hunger: x = cx + 1, y = bottom - 57, or bottom - 67 with the air row; textures/gui/
// icons.png background UV (0, 0) 98 x 21, fill UV (0, 21) cropped to (int)(98 * mana / max); text "mana/max" in
// #55FFFF with shadow at (x + 49 - (int)((digits + 0.5) * 6), y + 11). The XP anchor uses UV (68, 40) / (68, 61), 188 x 21.
// Ours hides the bar (empty regions): the text alone shows the mana.
// Ars Nouveau mana (GuiManaHUD, above the crosshair layer: under the hotbar). Fixed to the bottom-left corner: border
// manabar_gui_border UV (0, 0) 108 x 18 at (10, bottom - 23); fill manabar_gui_mana UV (0, 6 frame) at (19, bottom - 14),
// (int)(96 * mana / max) x 6, frame = (ticks / 3) % 33; frame overlay UV (0, 18) 108 x 20 at (10, bottom - 22).
// Rare extras: reserved mana (the mana texture UV (0, 0), r x 8 at (116 - r, bottom - 15), tinted dark) and the
// "not enough mana" flash (manabar_gui_grayscale, 98 x 8 at (18, bottom - 15), tinted red).

const ROLL_TIERS = { green: '#74cd00', yellow: '#e8b500', orange: '#e84d00', red: '#d90003', grey: '#353535' };
function rollTier(t, n = 2) {
  if (t - 1 >= 0.75 * (n - 1)) return 'green';
  if (t - 1 >= 0.5 * (n - 1)) return 'yellow';
  if (t - 1 >= 0.25 * (n - 1)) return 'orange';
  return t >= 1 ? 'red' : 'grey';
}
const MANA = { irons: ['#b8dcff', '#3c7ce8', '#10306a'], ars: ['#ecc8ff', '#9b4ad8', '#3a1060'] };

/** Draws img's UV rectangle (over a logical size tex) at logical (x, y). */
function blitUV(g, img, x, y, u, v, w, h, scale, smooth, tex = 256) {
  if (!img || w <= 0) return;
  const k = img.width / tex;
  g.imageSmoothingEnabled = smooth; g.imageSmoothingQuality = 'high';
  g.drawImage(img, u * k, v * k, w * k, h * k, x * scale, y * scale, w * scale, h * scale);
}

// ---- Our textures --------------------------------------------------------------------------------------------

/** dash_<tier> 18 x 18: no plate or frame, just a flat double chevron in the tier colour (the back one darker, the
 *  colour the mod also gives the count), with a thin dark outline so it reads over bright scenery (as the mod's
 *  outlined number). Grey (no charge) is a dim steel. */
function rollIcon(tier) {
  const [c, g] = hires(18, 18), col = tier === 'grey' ? '#6a6e73' : ROLL_TIERS[tier];
  const chevron = x0 => { g.beginPath(); g.moveTo(x0, 4.5); g.lineTo(x0 + 3, 4.5); g.lineTo(x0 + 6.5, 9); g.lineTo(x0 + 3, 13.5); g.lineTo(x0, 13.5); g.lineTo(x0 + 3.5, 9); g.closePath(); };
  g.lineJoin = 'miter';
  for (const [x0, fill] of [[3.5, mixHex(col, '#000000', 0.4)], [8, col]]) {
    chevron(x0); g.strokeStyle = 'rgba(0,0,0,0.85)'; g.lineWidth = 1; g.stroke();   // 0.5 outside the shape
    chevron(x0); g.fillStyle = fill; g.fill();
  }
  return c;
}

/** A mana pipe w wide in the meters' style: body 5 tall from row top, window 3, 0.5 separators every 8 from sep0
 *  (dark grey on empty glass), end zones of 0.5. fill: draw the liquid (and darker separators) instead. */
function manaPipe(g, x0, top, w, sep0, liquid, fill, brassy = false, h = 5) {
  const w0 = top + 1, w1 = top + h - 1, seps = [x0, x0 + w - 0.5];
  for (let x = x0 + sep0; x < x0 + w - 1; x += 8) seps.push(x);
  if (fill) {
    meterFill(g, x0 + 0.5, x0 + w - 0.5, w0, w1, liquid);
    g.fillStyle = meterDark(liquid);
  } else {
    const body = brassy ? ['#c9a25a', '#a07a36', '#6e5022', '#3a2a10'] : ['#6a7076', '#46494e', '#2c2f33', '#18191b'];   // flat brass or steel
    rrect(g, x0, top, w, h, 0.5); g.fillStyle = vgrad(g, top, top + h, [[0, body[0]], [0.25, body[1]], [0.75, body[2]], [1, body[3]]]); g.fill();
    g.fillStyle = '#050605'; g.fillRect(x0 + 0.5, w0, w - 1, w1 - w0);
    g.fillStyle = brassy ? '#2e2210' : '#1e2023';
  }
  for (const x of seps) g.fillRect(x, w0, 0.5, w1 - w0);
}

/** irons_spellbooks:textures/gui/icons.png at R x: the original, with the mana bar's four regions (background and fill,
 *  Hunger and XP anchors) left empty: no bar, the mod's "mana/max" text alone shows the mana. The rest of the sheet
 *  (the spell bar's sprites) is kept. */
function ironsSheet(orig) {
  const [c, g] = hires(256, 256);
  g.imageSmoothingEnabled = false;
  if (orig) g.drawImage(orig, 0, 0, 256, 256);
  g.clearRect(0, 0, 98, 42); g.clearRect(68, 40, 188, 42);
  return c;
}

/** ars_nouveau manabar_gui_border at R x: the background is the pipe around the fill (fill at x 9..105, rows 9..15 of
 *  this region; the liquid uses rows 10.5..13.5), the frame region (UV (0, 18)) left empty: no gold scrollwork. */
function arsBorderSheet(orig) {
  const [c, g] = hires(256, 256);
  g.imageSmoothingEnabled = false;
  if (orig) g.drawImage(orig, 0, 0, 256, 256);
  g.clearRect(0, 0, 108, 38);
  // Taller than the other meters: the window is the fill's full 6 rows (9..15), the brass pipe 8..16.
  manaPipe(g, 8.5, 8, 97, 8, MANA.ars, false, true, 8);   // brass: Ars' gold, in our flat brass
  // End caps: one sprite (not tiled cells), so the bar can have real caps: brass collars 2 wide, 1 px taller than the
  // pipe each way, closing the window just outside the fill (x 9..105).
  for (const x of [7, 105]) {
    const cap = () => rrect(g, x, 7, 2, 10, 0.6);
    cap(); g.fillStyle = vgrad(g, 7, 17, [[0, '#e0bc72'], [0.3, '#b08640'], [0.75, '#6e5022'], [1, '#3a2a10']]); g.fill();
    cap(); g.strokeStyle = 'rgba(20,12,4,0.8)'; g.lineWidth = 0.3; g.stroke();
  }
  // The frame overlay (UV (0, 18), drawn 1 px lower than the background and over the fill), kept from the original in
  // our style: a small amethyst on the pipe's top centre, thin brass flourishes running out from it along the pipe's
  // top edge (the pipe itself is brass too). In this region the pipe is at rows 7..15 (x 8.5..105.5); the window
  // (rows 8..14) stays clear.
  const top = 18 + 7, cxA = 57;
  g.strokeStyle = P.brass; g.lineWidth = 0.75; g.lineCap = 'round';   // 0.75: thinner curves break up at GUI scale 2
  for (const s of [-1, 1]) {
    g.beginPath(); g.moveTo(cxA + s * 2.6, top - 1.6);
    g.bezierCurveTo(cxA + s * 7, top - 3.6, cxA + s * 11, top + 0.2, cxA + s * 16, top - 1.4);
    g.quadraticCurveTo(cxA + s * 19, top - 2.4, cxA + s * 20.5, top - 1.2); g.stroke();
  }
  const gem = () => { g.beginPath(); g.moveTo(cxA, top - 3.4); g.lineTo(cxA + 2.4, top - 0.9); g.lineTo(cxA, top + 1.4); g.lineTo(cxA - 2.4, top - 0.9); g.closePath(); };
  gem(); g.fillStyle = vgrad(g, top - 3.4, top + 1.4, [[0, '#f0c8ff'], [0.5, '#b064e0'], [1, '#5a2080']]); g.fill();
  gem(); g.strokeStyle = P.brass; g.lineWidth = 0.5; g.stroke();
  return c;
}
/** ars_nouveau manabar_gui_mana at R x: 33 frames of 96 x 6 (v = 6 frame), the liquid filling each (the pipe's
 *  window), separators as the pipe's, a soft lighter band drifting across the frames. gray: the grayscale texture for
 *  the red "not enough mana" flash (drawn 1 px left and up, 98 x 8): a light band over the window (rows 1.5..6.5;
 *  its 8-row read reaches 0.5 into the next frame's band). */
function arsManaSheet(gray) {
  const [c, g] = hires(256, 256), col = gray ? ['#ffffff', '#d8d8d8', '#9a9a9a'] : MANA.ars, dx = gray ? 1 : 0, h = gray ? 5 : 6;
  for (let f = 0; f < 33; f++) {
    const y = 6 * f + (gray ? 1.5 : 0);
    meterFill(g, dx, dx + 96, y, y + h, col);
    g.fillStyle = meterDark(col);
    for (let x = dx + 7.5; x < dx + 96; x += 8) g.fillRect(x, y, 0.5, h);
    if (!gray) {
      g.save(); g.beginPath(); g.rect(dx, y, 96, h); g.clip();
      g.fillStyle = 'rgba(255,255,255,0.12)'; g.fillRect(dx - 12 + (f / 33) * 120, y, 12, h);
      g.restore();
    }
  }
  return c;
}

/** Our set: the textures, by the names the game loads. */
function modHudSprites(orig) {
  const roll = {};
  for (const t of Object.keys(ROLL_TIERS)) roll[t] = rollIcon(t);
  return { roll, irons: ironsSheet(orig.irons), arsBorder: arsBorderSheet(orig.arsBorder), arsMana: arsManaSheet(false), arsGray: arsManaSheet(true) };
}

// ---- Placement, as the mods draw -------------------------------------------------------------------------------

/** The dodge roll counter (before the vanilla layers). */
function drawRoll(g, set, font, cx, bottom, scale, smooth, t) {
  const tier = rollTier(t);
  blitUV(g, set.roll[tier], cx + 92, bottom - 19, 0, 0, 18, 18, scale, smooth, 18);
  if (!font) return;
  const s = String(Math.floor(t)), x = cx + 110, y = bottom - 10;
  for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) mcText(g, font, s, x + dx, y + dy, scale, '#000000');
  mcText(g, font, s, x, y, scale, ROLL_TIERS[tier]);
}
/** Iron's mana bar (after the air layer), Hunger anchor. */
function drawIronsMana(g, set, font, cx, bottom, scale, smooth, mana, max, airRow) {
  const x = cx + 1, y = bottom - (airRow ? 67 : 57);
  blitUV(g, set.irons, x, y, 0, 0, 98, 21, scale, smooth);
  blitUV(g, set.irons, x, y, 0, 21, Math.floor(98 * Math.min(mana / max, 1)), 21, scale, smooth);
  if (!font) return;
  const s = `${mana}/${max}`;
  mcText(g, font, s, x + 49 - Math.floor((String(mana).length + 0.5) * 6), y + 11, scale, '#55ffff', true);
}
/** Ars Nouveau's mana bar at the bottom-left corner (left = the screen's left edge). */
function drawArsMana(g, set, left, bottom, scale, smooth, frac, frame = 0) {
  blitUV(g, set.arsBorder, left + 10, bottom - 23, 0, 0, 108, 18, scale, smooth);
  blitUV(g, set.arsMana, left + 19, bottom - 14, 0, 6 * frame, Math.floor(96 * frac), 6, scale, smooth);
  blitUV(g, set.arsBorder, left + 10, bottom - 22, 0, 18, 108, 20, scale, smooth);
}
