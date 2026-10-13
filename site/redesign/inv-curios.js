// Curios (9.5.1) screen, cut by CuriosScreen.renderBg, PageButton and RenderButton (from the bytecode), drawn in our style.
// Uses sprites.js, panel-art.js (panelBody, cells), components.js (steelButton), inventory.js (solidAlpha).
//
// curios:textures/gui/curios/inventory.png (256 x 256), all pieces placed relative to the screen's left/top:
//   side panel: column 0 is 25 wide from (91, 0), later columns 25 wide from (98, 0), each 7 + 18 rows high, then its
//   7 high bottom from v 159; columns at x = -33 - 18 (n - 1) (+25, then +18 each). Slot cells from (7, 7), 18 wide;
//   items at (x0 + 8 + 18 c, 8 + 18 r) with x0 the first column's x. With several pages everything moves 8 down.
//   cosmetics tab (32, 0) 28 x 24 at (-31, -23); cosmetic slot overlay (32, 50) 18 x 18 (drawn blended);
//   page buttons 11 x 12 at (-28, 2) previous and (-17, 2) next: UV (32 | 43, 25), hovered u + 22, inactive v 37;
//   render toggles 8 x 8 at (item x + 12, item y - 1): UV (75, 0) shown, (83, 0) hidden.
// Sprites (gui atlas, Curios' gui/curios folder): cosmetic_on | cosmetic_off [_highlighted], 20 x 17, in the tab.
// Empty slots: curios:textures/slot/*.png, 16 x 16 on the item atlas (drawn unblended).
const CURIOS = {
  tab: [-31, -23, 28, 24],
  cosmetic: [-27, -19, 20, 17],
};
// The player's slots in this pack (data/*/curios/slots and curios/entities in the mod jars; sizes merged, sorted by
// "order", then name): 19 slots, filled column by column into 7, 7, 5 (as in game). Each with the icon it names.
const CURIO_TYPES = {   // type: [icon ns, icon path, our drawing], what the slot holds
  an_focus: ['curios', 'slot/an_focus_slot', 'focus'],          // Ars Nouveau spell focus (Ars Elemental's icon)
  head: ['curios', 'slot/empty_head_slot', 'hat'],               // hats, goggles, circlets
  necklace: ['curios', 'slot/empty_necklace_slot', 'necklace'],
  back: ['curios', 'slot/empty_back_slot', 'cape'],              // capes, elytra, backpacks
  rings: ['cataclysm', 'item/empty_ring_slot', 'ring'],          // L_Ender's Cataclysm rings
  bracelet: ['curios', 'slot/bangle_slot', 'bangle'],            // Ars Elemental bangles (its icon wins)
  hands: ['curios', 'slot/empty_hands_slot', 'glove'],
  ring: ['curios', 'slot/empty_ring_slot', 'ring'],
  waist: ['cataclysm', 'item/empty_waist_slot', 'pouch'],        // Cataclysm waist items: a belt with a pouch
  belt: ['curios', 'slot/empty_belt_slot', 'belt'],
  feet: ['minecraft', 'item/empty_armor_slot_boots', null],      // Artifacts' and Cataclysm's: the armor boots icon
  charm: ['curios', 'slot/empty_charm_slot', 'charm'],           // magnets and other trinkets
  spellbook: ['curios', 'slot/spellbook_slot', 'spellbook'],     // Iron's Spellbooks
  bundle: ['curios', 'slot/empty_curio_slot', 'sack'],           // Ars Elemental's bundle slot (no icon: Curios' default)
};
CURIOS.slots = ['an_focus', 'head', 'head', 'necklace', 'back', 'rings', 'rings', 'bracelet', 'bracelet', 'hands', 'hands',
  'ring', 'ring', 'waist', 'belt', 'feet', 'charm', 'spellbook', 'bundle'];
CURIOS.grid = [7, 7, 5];
// Icon files we ship that no player slot shows here, kept matching: [ns, path, drawing].
const CURIO_EXTRA = [['curios', 'slot/empty_bracelet_slot', 'bangle'], ['curios', 'slot/empty_body_slot', 'chestplate'],
  ['curios', 'slot/empty_cosmetic_slot', 'butterfly']];

/** Blueprint line drawings (16 x 16, drawn by slotIcon as dotted ghosts at half size), one per slot meaning. */
Object.assign(SLOT_ICONS, {
  focus: g => {   // a cut crystal
    g.beginPath(); g.moveTo(8, 1.5); g.lineTo(12.5, 6); g.lineTo(8, 14.5); g.lineTo(3.5, 6); g.closePath();
    g.moveTo(3.5, 6); g.lineTo(12.5, 6); g.moveTo(6, 6); g.lineTo(8, 14.5); g.lineTo(10, 6);
  },
  hat: g => {     // a brimmed hat
    g.beginPath(); g.moveTo(1.5, 11.5); g.lineTo(14.5, 11.5);
    g.moveTo(4.5, 11.5); g.lineTo(5, 6); g.quadraticCurveTo(8, 3, 11, 6); g.lineTo(11.5, 11.5); g.moveTo(4.8, 9); g.lineTo(11.2, 9);
  },
  necklace: g => {   // a chain and a pendant
    g.beginPath(); g.moveTo(3, 2); g.quadraticCurveTo(3.5, 8.5, 8, 9); g.quadraticCurveTo(12.5, 8.5, 13, 2);
    g.moveTo(8, 9.5); g.lineTo(10, 12); g.lineTo(8, 14.5); g.lineTo(6, 12); g.closePath();
  },
  cape: g => {    // a cape from the shoulders
    g.beginPath(); g.moveTo(5.5, 2); g.lineTo(10.5, 2); g.lineTo(13.5, 14); g.quadraticCurveTo(8, 12.5, 2.5, 14); g.closePath();
    g.moveTo(5.5, 2); g.quadraticCurveTo(8, 4.5, 10.5, 2);
  },
  ring: g => {    // a band with a stone
    g.beginPath(); g.arc(8, 10, 4.2, 0, Math.PI * 2);
    g.moveTo(8, 3); g.lineTo(10, 5); g.lineTo(8, 7); g.lineTo(6, 5); g.closePath();
  },
  bangle: g => {  // a wide band seen at an angle
    g.beginPath(); g.ellipse(8, 8, 6.5, 4, 0, 0, Math.PI * 2); g.moveTo(12.5, 8); g.ellipse(8, 8, 4.5, 2.2, 0, 0, Math.PI * 2);
  },
  glove: g => {   // a glove: cuff, palm, fingers, thumb
    g.beginPath(); g.rect(4.5, 12, 6.5, 2.8);
    g.moveTo(4.5, 12); g.lineTo(4.5, 3.5); g.moveTo(6.6, 12); g.lineTo(6.6, 2.5); g.moveTo(8.8, 9); g.lineTo(8.8, 2.5);
    g.moveTo(11, 12); g.lineTo(11, 3.5); g.moveTo(4.5, 3.5); g.lineTo(11, 3.5);
    g.moveTo(11, 10); g.lineTo(13.5, 7); g.lineTo(14, 8.5); g.lineTo(11, 12);
  },
  pouch: g => {   // a belt with a pouch hanging from it
    g.beginPath(); g.rect(1.5, 4.5, 13, 3); g.rect(8.5, 7.5, 5, 6); g.moveTo(8.5, 9.5); g.lineTo(13.5, 9.5);
  },
  belt: g => {    // a belt with a buckle
    g.beginPath(); g.rect(1.5, 6, 13, 4); g.rect(6, 4.5, 4, 7); g.moveTo(8, 6.5); g.lineTo(8, 9.5);
  },
  charm: g => {   // a trinket on a loop
    g.beginPath(); g.arc(8, 3, 1.3, 0, Math.PI * 2); g.moveTo(8, 4.3); g.lineTo(8, 6);
    g.moveTo(8, 6); g.lineTo(12, 10); g.lineTo(8, 14.5); g.lineTo(4, 10); g.closePath(); g.moveTo(8, 8.5); g.arc(8, 10, 1.5, -Math.PI / 2, Math.PI * 1.5);
  },
  spellbook: g => {   // a book with a rune circle
    g.beginPath(); g.rect(3.5, 2, 9.5, 12.5); g.moveTo(5.3, 2); g.lineTo(5.3, 14.5);
    g.moveTo(11.4, 8.2); g.arc(9.2, 8.2, 2.2, 0, Math.PI * 2);
  },
  sack: g => {    // a tied bundle
    g.beginPath(); g.moveTo(6.5, 4.5); g.quadraticCurveTo(1.5, 9, 3, 14.5); g.lineTo(13, 14.5); g.quadraticCurveTo(14.5, 9, 9.5, 4.5); g.closePath();
    g.moveTo(6.5, 4.5); g.lineTo(5, 1.8); g.moveTo(9.5, 4.5); g.lineTo(11, 1.8); g.moveTo(6.5, 4.5); g.lineTo(9.5, 4.5);
  },
  butterfly: g => {   // cosmetics (the tab's butterfly)
    g.beginPath(); g.ellipse(5, 6, 3, 2.6, -0.5, 0, Math.PI * 2); g.moveTo(14, 6); g.ellipse(11, 6, 3, 2.6, 0.5, 0, Math.PI * 2);
    g.moveTo(7.4, 11); g.ellipse(5.8, 11, 1.8, 1.5, 0.5, 0, Math.PI * 2); g.moveTo(12, 11); g.ellipse(10.2, 11, 1.8, 1.5, -0.5, 0, Math.PI * 2);
    g.moveTo(8, 3.5); g.lineTo(8, 13);
  },
});

/** Column x positions and item positions for a grid. */
function curiosLayout(grid = CURIOS.grid) {
  const x0 = -33 - 18 * (grid.length - 1);
  const items = grid.flatMap((rows, c) => [...Array(rows).keys()].map(r => [x0 + 8 + 18 * c, 8 + 18 * r]));
  return { x0, items };
}

/** curios:textures/gui/curios/inventory.png at R x. */
function curiosSheet() {
  const [c, g] = hires(256, 256);
  panelBody(g, 91, 0, 32, 166);                       // the side panel, cut into columns by the game
  g.fillStyle = '#232426'; g.fillRect(7, 7, 18, 144);  // slot strip: cells on the panel's iron
  cells(g, [...Array(8).keys()].map(r => [8, 8 + 18 * r]));
  // The cosmetics tab: the panel's top, open at the bottom where it overlaps the panel by 1.
  g.save(); g.beginPath(); g.rect(32, 0, 28, 24); g.clip(); panelBody(g, 32, 0, 28, 34); g.restore();
  // Page buttons: steel, a flat light arrow; hovered amber; inactive dark.
  for (const [u, prev] of [[32, true], [43, false]]) {
    for (const [du, v, state] of [[0, 25, 'on'], [22, 25, 'hover'], [0, 37, 'off']]) {
      const x = u + du;
      steelButton(g, x, v, 11, 12, state === 'hover', 1.6);
      if (state === 'off') { rrect(g, x + 0.3, v + 0.2, 10.4, 11.1, 1.6); g.fillStyle = 'rgba(10,11,12,0.6)'; g.fill(); }
      const cx = x + 5.5, cy = v + 5.6, w = 3, h = 5;
      g.beginPath();
      if (prev) { g.moveTo(cx + w / 2, cy - h / 2); g.lineTo(cx + w / 2, cy + h / 2); g.lineTo(cx - w / 2, cy); }
      else { g.moveTo(cx - w / 2, cy - h / 2); g.lineTo(cx - w / 2, cy + h / 2); g.lineTo(cx + w / 2, cy); }
      g.closePath(); g.fillStyle = { on: '#c9ccd0', hover: '#ffd9a0', off: '#4a4d52' }[state]; g.fill();
    }
  }
  // Cosmetic slots: a faint violet wash over the cell (the game blends this one).
  rrect(g, 32.5, 50.5, 17, 17, 1); g.fillStyle = 'rgba(150,100,210,0.22)'; g.fill();
  rrect(g, 32.8, 50.8, 16.4, 16.4, 0.8); g.strokeStyle = 'rgba(180,130,240,0.45)'; g.lineWidth = 0.4; g.stroke();
  // Render toggles: a small steel bezel around a dark well, an amber dot while the curio is shown.
  for (const [u, on] of [[75, true], [83, false]]) {
    rrect(g, u + 0.4, 0.4, 7.2, 7.2, 1.2); g.fillStyle = vgrad(g, 0, 8, [[0, '#8a8f95'], [1, '#3e4146']]); g.fill();
    rrect(g, u + 0.4, 0.4, 7.2, 7.2, 1.2); g.strokeStyle = 'rgba(0,0,0,0.9)'; g.lineWidth = 0.5; g.stroke();
    rrect(g, u + 1.6, 1.6, 4.8, 4.8, 0.6); g.fillStyle = '#0d0e10'; g.fill();
    if (on) { circle(g, u + 4, 4, 1.5); g.fillStyle = '#ffb347'; g.fill(); }
  }
  // Partial alpha would show at full strength where the game blits unblended; keep the cosmetic wash translucent.
  const wash = g.getImageData(32 * R, 50 * R, 18 * R, 18 * R);
  solidAlpha(c);
  g.putImageData(wash, 32 * R, 50 * R);
  return c;
}

/** A cosmetics toggle sprite (20 x 17): a steel button with a butterfly, amber while cosmetics are on. */
function curiosCosmeticButton(on, hover) {
  const [c, g] = hires(20, 17);
  steelButton(g, 0, 0, 20, 17, hover, 2.4);
  g.fillStyle = on ? '#ffb347' : hover ? '#ffd9a0' : '#c9ccd0';
  const cx = 10, cy = 8;
  for (const s of [1, -1]) {
    g.beginPath(); g.ellipse(cx + s * 3, cy - 1.6, 2.8, 2.2, s * -0.5, 0, Math.PI * 2); g.fill();
    g.beginPath(); g.ellipse(cx + s * 2.3, cy + 2.4, 1.9, 1.6, s * 0.5, 0, Math.PI * 2); g.fill();
  }
  g.fillRect(cx - 0.4, cy - 3.6, 0.8, 7.6);
  return c;
}
