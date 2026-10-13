// Compact HUD textures for the game (createhud:textures/gui/compact/*), drawn by the same functions as the page so
// the game shows exactly the preview. Create Dark Inventory's CompactHud.java draws from them in 1/8 GUI px (one
// texel), so every position here is a whole multiple of 1/8. KEEP IN STEP with CompactAtlas.java.
//   plate_{none,left,right,both}: 332 x 34 at (-166, 17) of the bar (bottom at 50): the plate with no, the left
//     (armor), the right (breath) or both band extensions.
//   parts (256 x 256):
//     (0, 0) socket 18x18; (20, 0) selection 24x23; (46, 0) offhand ghost 18x18; (66, 0) level window 10x5;
//     (80 + 18 k, 0) dash icons 18x18: green, yellow, orange, red, grey.
//     icons in 8x8 cells, centred: row 24 at wing size (5.6), row 32 at extension size (4.4), row 40 at window size.
//     casings (empty meters): wing (0, 50) 38x4, extension (40, 50) 38x3.25, XP (0, 56) 157x3.25.
//     full meters per liquid (the game crops their inner 0.5-inset area): wing (0, 62 + 5 k), extension
//     (40, 62 + 4 k), XP (0, 130 + 4 k).
//     digits "0123456789/:+" at 0.625 size, 2.5 apart, rows 170 + 4 r: white, dim, level green, shadow black.
//     ghost plate (0, 190) 24x9 (the follower count's digits are drawn after the ghost); fillers: gear row centred
//     at (46, 196), groove (80, 190) 38x4, dash cog centred at (129, 199); effect frames 11x11 at (140|152|164, 190)
//     (buff, debuff, ambient); 0.75 digits 3 apart, rows 212 white, 216 red, 220 black.
const CCX = {
  wingIcons: ['heart', 'heart_poison', 'heart_wither', 'heart_frozen', 'drumstick', 'horse', 'camel', 'flask', 'gem'],
  extIcons: ['armor', 'air', 'air_burst', 'backtank', 'bolt'],
  winIcons: ['flask', 'horse', 'camel', 'train', 'engine'],
  wingLiquids: ['blood', 'poison', 'wither', 'frozen', 'absorb', 'broth', 'hunger', 'gilded', 'saturation', 'irons', 'ars', 'mount'],
  extLiquids: ['armor', 'air', 'tank', 'meka', 'flamingo', 'hull'],
  xpLiquids: ['xp', 'jump', 'cooldown', 'cast', 'train', 'engine'],
  digits: '0123456789/:+', digitRows: ['#ffffff', '#9aa3ad', '#80ff20', '#000000'],
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
    default: return ccModIcon(name);   // bolt, train, engine
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
  ccMeter(g, 0, 50, 38, 4, 0, CCX_LIQ.blood, 10);
  ccMeter(g, 40, 50, 38, 3.25, 0, CCX_LIQ.armor, 10);
  ccMeter(g, 0, 56, 157, 3.25, 0, CCX_LIQ.xp, 18);
  CCX.wingLiquids.forEach((n, k) => ccMeter(g, 0, 62 + 5 * k, 38, 4, 1, CCX_LIQ[n], 10));
  CCX.extLiquids.forEach((n, k) => ccMeter(g, 40, 62 + 4 * k, 38, 3.25, 1, CCX_LIQ[n], 10));
  CCX.xpLiquids.forEach((n, k) => ccMeter(g, 0, 130 + 4 * k, 157, 3.25, 1, CCX_LIQ[n], 18));
  CCX.digitRows.forEach((col, r) => [...CCX.digits].forEach((ch, k) => ccNumber(g, ch, 2.5 * k, 170 + 4 * r, col, null, 0.625)));
  // The follower plate: the bar's plate style, a ghost at its left.
  const plate = () => rrect(g, 0, 190, 24, 9, CC.plate.r);
  plate(); g.fillStyle = iron(g, 190, 199); g.fill(); ironSurface(g, plate); rim(g, plate, 190, 199, P.brassHi, 0.35);
  ccInstruments.ghost(g, 6, 194.5);
  // Pressed-in fillers for empty spots: the gear row (centred at (46, 196)), an empty groove (80, 190) 38x4, the
  // dash's cog (centred at (129, 199)).
  ccGearRow(g, 46, 196);
  ccGroove(g, 80, 190, 38, 4);
  ccDashFiller(g, 129, 199);
  // Effect frames at the bar's 11 x 11 (no icon, no time border: the game draws those): buff (140, 190), debuff
  // (152, 190), ambient (164, 190). Digits at 0.75 for the effects' time, 3 apart: rows 212 white, 216 red, 220 black.
  for (const [kind, ux, amb] of [['buff', 140, false], ['debuff', 152, false], ['buff', 164, true]]) {
    g.save(); g.translate(ux, 190); g.scale(11 / 24, 11 / 24); ccEffect(g, 0, 0, kind, null, 1, amb, true); g.restore();
  }
  ['#ffffff', '#ffb0a0', '#000000'].forEach((col, r) => [...CCX.digits].forEach((ch, k) => ccNumber(g, ch, 3 * k, 212 + 4 * r, col, null, 0.75)));
  files.push(['createhud: compact/parts', c]);
  return files;
}
