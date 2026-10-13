// Compact HUD textures for the game (createhud:textures/gui/compact/*), drawn by the same functions as the page so
// the game shows exactly the preview. Create Dark Inventory's CompactHud.java draws from them in 1/8 GUI px (one
// texel), so every position here is a whole multiple of 1/8. KEEP IN STEP with CompactAtlas.java.
//   plate_{none,left,right,both}: 332 x 34 at (-166, 17) of the bar (bottom at 50): the plate with no, the left
//     (armor), the right (breath) or both band extensions.
//   parts (256 x 256):
//     (0, 0) socket 18x18; (20, 0) selection 24x23; (46, 0) offhand ghost 18x18; (66, 0) level window 10x5;
//     (80 + 18 k, 0) dash icons 18x18: green, yellow, orange, red, grey.
//     icons in 8x8 cells, centred: row 24 at wing size (5.6), row 32 at extension size (4.4), row 40 at window size.
//     casings (empty meters): wing (0, 50) 32x4, extension (40, 50) 32x3.25, XP (0, 56) 157x3.25.
//     full meters per liquid (the game crops their inner 0.5-inset area): wing (0, 62 + 5 k), extension
//     (40, 62 + 4 k), XP (0, 130 + 4 k).
//     digits "0123456789/:+smh" at 0.625 size, 3.5 apart (m is 5 wide), rows 170 + 4 r: white, dim, level green, shadow black, red.
//     ghost plate (0, 190) 24x9 (the follower count's digits are drawn after the ghost); fillers: gear row centred
//     at (46, 196), groove (80, 190) 32x4, dash cog centred at (129, 199); effect frames 10x10 at (140|152|164, 190)
//     (buff, debuff, ambient).
const CCX = {
  wingIcons: ['heart', 'heart_poison', 'heart_wither', 'heart_frozen', 'drumstick', 'horse', 'camel', 'flask', 'gem'],
  extIcons: ['armor', 'air', 'air_burst', 'backtank', 'bolt', 'plane', 'flamingo'],
  winIcons: ['flask', 'horse', 'camel', 'train', 'engine'],
  wingLiquids: ['blood', 'poison', 'wither', 'frozen', 'absorb', 'broth', 'hunger', 'gilded', 'saturation', 'irons', 'ars', 'mount'],
  extLiquids: ['armor', 'air', 'tank', 'meka', 'flamingo', 'hull'],
  xpLiquids: ['xp', 'jump', 'cooldown', 'cast', 'train', 'engine'],
  digits: '0123456789/:+smh', digitRows: ['#ffffff', '#9aa3ad', '#80ff20', '#000000', '#ffb0a0'],
  dashTiers: ['green', 'yellow', 'orange', 'red', 'grey'],
};
const CCX_LIQ = {
  blood: LIQUID.blood, poison: LIQUID.poison, wither: LIQUID.wither, frozen: LIQUID.frozen, absorb: LIQUID.absorb,
  broth: LIQUID.broth, hunger: LIQUID.hunger, gilded: CC_GILDED, saturation: ['#fff4e2', '#e9c79c', '#6e4e30'],
  irons: MANA.irons, ars: MANA.ars, mount: CC_MOUNT, armor: ['#eef1f4', '#9aa0a6', '#3a3d41'], air: LIQUID.air,
  tank: ['#c98a5a', '#7a3e1c', '#2e1206'], meka: CC_MEKA, flamingo: CC_FLAMINGO, hull: CC_HULL,
  xp: ['#d8ff9a', '#5fd02a', '#1d5a08'], jump: CC_JUMP, cooldown: CC_COOLDOWN, cast: MANA.irons, train: CC_TRAIN, engine: CC_ENGINE,
};
function ccxIcon(name) {
  switch (name) {
    case 'heart': return heart(false);
    case 'heart_poison': return ccHeartIcon({ effect: 'poison' });
    case 'heart_wither': return ccHeartIcon({ effect: 'wither' });
    case 'heart_frozen': return ccHeartIcon({ effect: 'frozen' });
    case 'drumstick': return ccDrumstick();
    case 'horse': return ccHorse();
    case 'camel': return ccCamel();
    case 'flask': return ccFlask(MANA.irons);
    case 'gem': return ccGem(MANA.ars);
    case 'armor': return armor('full');
    case 'air': return air(false);
    case 'air_burst': return air(true);
    case 'backtank': return ccBacktank();
    default: return ccModIcon(name);   // bolt, train, engine, plane, flamingo
  }
}
/** [[file name, canvas]] for the pack. */
function compactExports() {
  const files = [];
  for (const [name, ears] of [['none', {}], ['left', { left: true }], ['right', { right: true }], ['both', { left: true, right: true }]]) {
    const [c, g] = hires(332, 34);
    g.translate(166, -17); ccPlate(g, ears);
    files.push(['createhud: compact/plate_' + name, c]);
  }
  const [c, g] = hires(256, 256);
  socket(g, 0, 0);
  ccSelection(g, 23, 3);   // snug on a socket at (23, 3): the game draws this 24x23 cell at the socket - 3
  g.save(); g.translate(46 + 9, 9.4); g.scale(1.25, 1.25); g.translate(-4.5, -4.5);
  shieldPath(g); g.setLineDash([0.4, 0.4]); g.strokeStyle = '#3a414b'; g.lineWidth = 0.4; g.stroke(); g.restore();
  rrect(g, 66, 0, 10, 5, 1.2); g.fillStyle = steel(g, 0, 5); g.fill();
  rrect(g, 66.5, 0.5, 9, 4, 0.8); g.fillStyle = '#060708'; g.fill();
  CCX.dashTiers.forEach((t, k) => g.drawImage(rollIcon(t), 80 + 18 * k, 0, 18, 18));
  CCX.wingIcons.forEach((n, k) => ccIcon(g, ccxIcon(n), 8 * k + 4, 28, 5.6));
  CCX.extIcons.forEach((n, k) => ccIcon(g, ccxIcon(n), 8 * k + 4, 36, 4.4));
  CCX.winIcons.forEach((n, k) => ccIcon(g, ccxIcon(n), 8 * k + 4, 44, 3.8));
  const MW = CC.meterW;   // wing and extension meters share the width
  ccMeter(g, 0, 50, MW, 4, 0, CCX_LIQ.blood, 10);
  ccMeter(g, 40, 50, MW, 3.25, 0, CCX_LIQ.armor, 10);
  ccMeter(g, 0, 56, 157, 3.25, 0, CCX_LIQ.xp, 18);
  CCX.wingLiquids.forEach((n, k) => ccMeter(g, 0, 62 + 5 * k, MW, 4, 1, CCX_LIQ[n], 10));
  CCX.extLiquids.forEach((n, k) => ccMeter(g, 40, 62 + 4 * k, MW, 3.25, 1, CCX_LIQ[n], 10));
  CCX.xpLiquids.forEach((n, k) => ccMeter(g, 0, 130 + 4 * k, 157, 3.25, 1, CCX_LIQ[n], 18));
  CCX.digitRows.forEach((col, r) => [...CCX.digits].forEach((ch, k) => ccNumber(g, ch, 3.5 * k, 170 + 4 * r, col, null, 0.625)));
  // The follower plate: the bar's plate style, a ghost at its left.
  const plate = () => rrect(g, 0, 190, 24, 9, CC.plate.r);
  plate(); g.fillStyle = iron(g, 190, 199); g.fill(); ironSurface(g, plate); rim(g, plate, 190, 199, P.brassHi, 0.35);
  ccInstruments.ghost(g, 6, 194.5);
  // Pressed-in fillers for empty spots: the gear row (centred at (46, 196)), an empty groove (80, 190) 32x4, the
  // dash's cog (centred at (129, 199)).
  ccGearRow(g, 46, 196);
  ccGroove(g, 80, 190, MW, 4);
  ccDashFiller(g, 129, 199);
  // Effect frames at 10 x 10 (no icon, no time border: the game draws those): buff (140, 190), debuff (152, 190),
  // ambient (164, 190).
  for (const [kind, ux, amb] of [['buff', 140, false], ['debuff', 152, false], ['buff', 164, true]]) {
    g.save(); g.translate(ux, 190); g.scale(10 / 24, 10 / 24); ccEffect(g, 0, 0, kind, null, 1, amb, true); g.restore();
  }

  // Overlays and pieces for the mods' extras (y 210): toolbox chips on a socket (0, 210) and lit (20, 210), each 18x18;
  // the toolbox slot's red selection snug on a socket at (43, 213) (a 24x23 cell at (40, 210)); the schematic slot's
  // frame (66, 210) 18x18; a blank instrument plate (86, 210) 24x9 (sliced 4 | middle | 4); clock and compass faces
  // (112 | 122, 210) 9x9 (the game draws the hands).
  ccToolboxChips(g, 0, false, [0, 210]); ccToolboxChips(g, 0, true, [20, 210]);
  ccSelection(g, 43, 213, true); ccToolboxChips(g, 0, true, [43, 213]);
  rrect(g, 67, 211, 16, 16, 0.6); g.strokeStyle = 'rgba(110,220,255,0.9)'; g.lineWidth = 0.6; g.stroke();
  { const ip = () => rrect(g, 86, 210, 24, 9, CC.plate.r); ip(); g.fillStyle = iron(g, 210, 219); g.fill(); ironSurface(g, ip); rim(g, ip, 210, 219, P.brassHi, 0.35); }
  ccDial(g, 116.5, 214.5, 3.6, null, 'clock'); ccDial(g, 126.5, 214.5, 3.6, null, 'compass');
  files.push(['createhud: compact/parts', c]);
  return files;
}
