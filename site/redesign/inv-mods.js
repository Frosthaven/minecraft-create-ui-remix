// Mod pieces on the inventory screen, placed and cut by each mod's code (read from the bytecode), drawn in our style.
// Uses sprites.js, panel-art.js (panelBody's iron and bezel), components.js.
//
// Cosmetic Armor Reworked: cosmeticarmorreworked:textures/gui/cosarmorinventory.png (256 x 256; also the cosmetic
// screen's panel at (0, 0) 176 x 166, kept). Open button 10 x 10 at (65, 67): UV (0, 166), hovered (10, 166); its
// hover text "CosmeticArmor" is code. Toggle 5 x 5 at (59, 72): UV (0, 176) cosmetics shown, (5, 176) hidden.
// TrashSlot: trashslot:textures/gui/slot.png (256 x 256), grey at u 0 (undo possible: survival here), red at u 64
// (instant deletion). Pieces: bottom 32 x 25 (0, 0), top 32 x 25 (32, 0), right 25 x 31 (0, 25), left 25 x 31 (25, 25),
// lone 28 x 28 (0, 56); 4 x 4 join patches at (50|54, 25|29|33|37). On the inventory (TrashSlotSaveState.json) the
// style is bottom-right: the bottom piece at (144, 162), the patch (50, 29) over its top-left corner, the slot at
// (152, 165). Empty: trashslot:item/trashcan (item atlas, 16 x 16).

/** The cosmetic armor sheet at R x: the original kept, with our cosmetic screen panel (0, 0, 176 x 166), our button
 *  (a coat hanger, normal and hovered) and toggle painted in. */
function cosSheet(orig) {
  const [c, g] = hires(256, 256);
  g.imageSmoothingEnabled = false;
  if (orig) g.drawImage(orig, 0, 0, 256, 256);
  g.clearRect(0, 0, 176, 166); g.drawImage(inventoryPanel(true), 0, 0, 176, 166);
  g.clearRect(0, 166, 20, 10); g.clearRect(0, 176, 10, 5);
  g.drawImage(miniButton(10, 10, GLYPHS.cosmetic), 0, 166, 10, 10);
  g.drawImage(miniButton(10, 10, GLYPHS.cosmetic, true), 10, 166, 10, 10);
  // The toggle: a tiny steel bezel; cosmetics shown, a lit amber dot; hidden, dark.
  for (const [x, on] of [[0, true], [5, false]]) {
    // A clear border: a bright steel bezel with a dark outline, around a dark well.
    const bez = () => rrect(g, x + 0.3, 176.3, 4.4, 4.4, 0.8);
    bez(); g.fillStyle = vgrad(g, 176, 181, [[0, '#eef1f4'], [1, '#9aa0a6']]); g.fill();
    bez(); g.strokeStyle = 'rgba(0,0,0,0.9)'; g.lineWidth = 0.5; g.stroke();
    rrect(g, x + 1.1, 177.1, 2.8, 2.8, 0.4); g.fillStyle = '#0d0e10'; g.fill();
    if (on) { circle(g, x + 2.5, 178.5, 1); g.fillStyle = '#ffb347'; g.fill(); }
  }
  return c;
}

// ---- TrashSlot -------------------------------------------------------------------------------------------------
// A piece is drawn in (u, v): u along the panel edge it joins (0..L), v away from the panel (0..D); the first 4 of v
// overlap the panel. The panel's frame (panelBody: bezel 0.9 wide, centred 0.85 inside its edge) lies at v = 3.15.
const TRASH_JOIN = 4, TRASH_FRAME = 3.15;
/** Maps (u, v) to the piece's box (w x h) for the side it joins: 'top' (a bottom tab), 'bottom', 'left', 'right'. */
function trashFrame(g, x, y, w, h, join) {
  g.translate(x, y);
  if (join === 'bottom') g.transform(1, 0, 0, -1, 0, h);
  else if (join === 'left') g.transform(0, 1, 1, 0, 0, 0);
  else if (join === 'right') g.transform(0, 1, -1, 0, w, 0);
}
/** A tab: iron, its frame turning into the panel's border at both ends (the join patch's shape, built in). The mod
 *  places a piece flush with the panel's edge only in theory: in game the bottom-right piece sits a few px inside, so
 *  a frame running straight up into the panel showed over it. */
function trashTab(g, x, y, w, h, join) {
  const side = join === 'top' || join === 'bottom', L = side ? w : h, D = side ? h : w, r = 2.4;
  g.save(); trashFrame(g, x, y, w, h, join);
  rrect(g, 0.4, 0, L - 0.8, D - 0.4, [0, 0, r, r]); g.fillStyle = '#232426'; g.fill();
  g.beginPath(); g.moveTo(0.85, 0); g.lineTo(0.85, D - 0.85 - r); g.arcTo(0.85, D - 0.85, 0.85 + r, D - 0.85, r);
  g.lineTo(L - 0.85 - r, D - 0.85); g.arcTo(L - 0.85, D - 0.85, L - 0.85, D - 0.85 - r, r); g.lineTo(L - 0.85, 0);
  g.strokeStyle = '#3a3c3f'; g.lineWidth = 0.9; g.stroke();
  patchShape(g); g.save(); g.transform(-1, 0, 0, 1, L, 0); patchShape(g); g.restore();
  g.restore();
}
/** The join patch's drawing in its piece's (u, v) frame, corner at u = 0. */
function patchShape(g) {
  g.fillStyle = '#232426';
  g.fillRect(0, 0, 4, TRASH_FRAME + 0.45); g.fillRect(0.4, TRASH_FRAME, 3.6, 4 - TRASH_FRAME);
  g.beginPath(); g.moveTo(0, TRASH_FRAME); g.lineTo(0.85, TRASH_FRAME); g.lineTo(0.85, 4);
  g.strokeStyle = '#3a3c3f'; g.lineWidth = 0.9; g.lineJoin = 'miter'; g.stroke();
}
/** A 4 x 4 join patch: the panel's border turning into the tab's side (corner at u = 0, or mirrored). */
function trashPatch(g, x, y, join, mirror) {
  g.save(); trashFrame(g, x, y, 4, 4, join);
  if (mirror) g.transform(-1, 0, 0, 1, 4, 0);
  patchShape(g);
  g.restore();
}
/** The slot's socket: an 18 x 18 bezel 0.5 wide (brass, or rust for instant deletion) around the 16 x 16 well. */
function trashSocket(g, x, y, rust) {
  rrect(g, x + 0.5, y + 0.5, 17, 17, 1); g.fillStyle = rust ? vgrad(g, y, y + 18, [[0, '#c8603a'], [1, '#6a2a14']]) : brass(g, y, y + 18); g.fill();
  rrect(g, x + 1, y + 1, 16, 16, 0.6); g.fillStyle = vgrad(g, y + 1, y + 17, [[0, '#030202'], [1, '#141009']]); g.fill();
}
/** trashslot:textures/gui/slot.png at R x: every piece and patch, grey (u 0) and red (u 64) variants. */
function trashSheet() {
  const [c, g] = hires(256, 256);
  for (const [u, rust] of [[0, false], [64, true]]) {
    // [x, y, w, h, join, slot local x, y]
    for (const [x, y, w, h, join, sx, sy] of [[u, 0, 32, 25, 'top', 8, 3], [u + 32, 0, 32, 25, 'bottom', 8, 6],
      [u, 25, 25, 31, 'left', 3, 7], [u + 25, 25, 25, 31, 'right', 6, 7]]) {
      trashTab(g, x, y, w, h, join);
      trashSocket(g, x + sx - 1, y + sy - 1, rust);
    }
    // Lone: a framed square on its own.
    rrect(g, u + 0.4, 56.4, 27.2, 27.2, 2.4); g.fillStyle = '#232426'; g.fill();
    rrect(g, u + 0.85, 56.85, 26.3, 26.3, 2); g.strokeStyle = '#3a3c3f'; g.lineWidth = 0.9; g.stroke();
    trashSocket(g, u + 5, 61, rust);
    // Join patches: bottom pieces' top corners, top pieces' bottom corners, left pieces' right corners, right pieces'
    // left corners.
    for (const [x, y, join, mirror] of [[50, 29, 'top', false], [54, 29, 'top', true], [50, 25, 'bottom', false], [54, 25, 'bottom', true],
      [50, 33, 'right', false], [50, 37, 'right', true], [54, 33, 'left', false], [54, 37, 'left', true]]) trashPatch(g, u + x, y, join, mirror);
  }
  solidAlpha(c);   // blitted unblended: soft edges would show at full strength
  return c;
}
