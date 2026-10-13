// Compact HUD textures for the game (createhud:textures/gui/compact/*), drawn by the same functions as the page so
// the game shows exactly the preview. Create Dark Inventory's hud/ classes draw from them in 1/8 GUI px (one texel), so
// every position here is a whole multiple of 1/8. KEEP IN STEP with CompactAtlas.java.
//   plate_{bars,bars_meka,orbs,orbs_meka}: 332 x 34 at (-166, 17) of the bar (bottom at 50). Orbs: with the housings.
//     _meka: the left wing widened for the MekaSuit gauge's slot (right-handed only; ccEnds).
//   parts (256 x 256):
//     (0, 0) socket 18x18; (20, 0) selection 24x23; (46, 0) offhand ghost 18x18; (66, 0) the hardcore skull plate 9x10
//     (shadow included); (78, 0) the level window 10 x 4.6 (caps 2 | middle | 2); (90, 0) the icon window 9x7;
//     (100 + 7 k, 0) window icons 7x7 (icon 5 tall, centred): flask, horse, camel, train, engine;
//     (136, 0) MekaSuit gauge casing 2.4 x 18, (140, 0) its fill.
//     (0, 20) wing casing 32x4; (0, 26) XP casing 177x3.
//     (0, 30 + 5 k) full wing meters (the game crops their inner 0.5-inset area), WING_LIQUIDS;
//     (0, 112 + 4 k) full XP pipes, XP_LIQUIDS.
//     (0, 140 + 4 r) digits "0123456789/:+smh" at 0.625, 3.5 apart (m is 5 wide): white, dim, green, black, red.
//     (180, 20 + 3.5 r) digits "0123456789/:+" at 0.6, 3 apart, TINTS rows (meter and orb numbers, the tank time).
//     (180, 76 + 3.5 r) the same at 0.55 (the level): green, dim, black.
//     (0, 190) follower plate 24x9; gear row centred (46, 196); groove (80, 190) 32x4; dash cog centred (129, 199);
//     effect frames 10x10 at (140|152|164, 190); overlays at y 210 (see compactExports).
//   orbs (384 x 320): orb pieces centred in cells. Small cells (32 x 32): cell n centred at (28 + 32 (n % 11),
//     34.5 + 32 floor(n / 11)), ORB_CELLS. Ring cells (48 x 48): centred at (28 + 64 m, 290.5), RING_CELLS. Centres are
//     28 and 2.5 modulo 32, so the material textures (256 px tiles) line up with the page's.
const CCX = {
  winIcons: ['flask', 'horse', 'camel', 'train', 'engine'],
  wingLiquids: ['blood', 'poison', 'wither', 'frozen', 'absorb', 'broth', 'hunger', 'saturation', 'mount', 'irons', 'ars',
    'armor', 'air', 'tank', 'flamingo', 'hull'],
  xpLiquids: ['xp', 'jump', 'cooldown', 'cast', 'train', 'engine'],
  digits: '0123456789/:+smh', digitRows: ['#ffffff', '#9aa3ad', '#80ff20', '#000000', '#ffb0a0'],
  glyphs: '0123456789/:+',
  tints: ['blood', 'poison', 'wither', 'frozen', 'broth', 'hunger', 'mount', 'irons', 'ars', 'armor', 'air', 'tank', 'flamingo', 'hull', 'black'],
  levelRows: ['#80ff20', '#3a5a2a', '#000000'],
  dashTiers: ['green', 'yellow', 'orange', 'red', 'grey'], dashMaxN: 6,
};
const CCX_LIQ = {
  blood: LIQUID.blood, poison: LIQUID.poison, wither: LIQUID.wither, frozen: LIQUID.frozen, absorb: LIQUID.absorb,
  broth: LIQUID.broth, hunger: LIQUID.hunger, saturation: ['#fff4e2', '#e9c79c', '#6e4e30'],
  irons: MANA.irons, ars: MANA.ars, mount: CC_MOUNT, armor: ['#eef1f4', '#9aa0a6', '#3a3d41'], air: LIQUID.air,
  tank: CC_TANK, flamingo: CC_FLAMINGO, hull: CC_HULL,
  xp: ['#d8ff9a', '#5fd02a', '#1d5a08'], jump: CC_JUMP, cooldown: CC_COOLDOWN, cast: MANA.irons, train: CC_TRAIN, engine: CC_ENGINE,
};
/** The liquid style a meter uses (bar mode, the XP pipe): health and the mount billow, food is broth, mana a galaxy. */
const CCX_STYLE = { blood: 'cloud', poison: 'cloud', wither: 'cloud', frozen: 'cloud', absorb: 'cloud', mount: 'cloud',
  broth: 'broth', hunger: 'broth', irons: 'mana', ars: 'mana', xp: 'cloud', cast: 'mana' };
/** Text tints (ccTint of the liquid's middle colour); black: the shadow. */
const ccxTint = n => n === 'black' ? 'rgba(0,0,0,0.9)' : n === 'tank' ? ccTint('#d9874a') : ccTint(CCX_LIQ[n][1]);

const ORB_CELLS = ['blood', 'poison', 'wither', 'frozen', 'broth', 'hunger', 'brothSat', 'hungerSat', 'mount', 'absorb', 'irons', 'ars',
  'glassL', 'glassR', 'front', 'divider', 'bezelL', 'bezelR', 'cog', 'crack1', 'crack2', 'crack3', 'crack4',
  'glow_blood', 'glow_poison', 'glow_wither', 'glow_frozen', 'glow_broth', 'glow_hunger', 'glow_mount', 'glow_irons', 'glow_ars',
  ...CCX.dashTiers.map(t => 'dash_' + t),
  ...CCX.dashTiers.flatMap(t => Array.from({ length: CCX.dashMaxN }, (_, i) => `ring_${t}_${i + 1}`))];
const RING_CELLS = ['armor', 'armorHalf', 'tank', 'air', 'flamingo', 'hull'];
const ccxCell = n => [28 + 32 * (n % 11), 34.5 + 32 * Math.floor(n / 11)];
const ccxRing = m => [28 + 64 * m, 290.5];
/** Which orb a liquid belongs to (its pieces are drawn at that orb, so the material textures line up). */
const ccxOrbX = name => /^(irons|ars|glassR|bezelR)$/.test(name) ? ORB.rx : ORB.lx;

function ccxIcon(name) {
  switch (name) {
    case 'flask': return ccFlask(MANA.irons);
    case 'horse': return ccHorse();
    case 'camel': return ccCamel();
    default: return ccModIcon(name);   // train, engine
  }
}

/** The orbs atlas. */
function ccxOrbs() {
  const [c, g] = hires(384, 320), r = ORB.r, oy = ORB.y;
  const at = ([ex, ey], half, ox, draw) => {
    g.save(); g.beginPath(); g.rect(ex - half, ey - half, 2 * half, 2 * half); g.clip();
    g.translate(ex - ox, ey - oy); draw(ox); g.restore();
  };
  const disc = (ox, f) => { g.save(); circle(g, ox, oy, r); g.clip(); f(); g.restore(); };
  ORB_CELLS.forEach((name, n) => {
    const ox = ccxOrbX(name), cell = ccxCell(n);
    at(cell, 16, ox, ox => {
      const liq = CCX_LIQ[name.replace(/Sat$/, '')];
      if (/^(blood|poison|wither|frozen|mount|absorb)$/.test(name)) disc(ox, () => ccOrbLiquid(g, ox, oy, liq, 'cloud'));
      else if (/^(broth|hunger)$/.test(name)) disc(ox, () => ccOrbLiquid(g, ox, oy, liq, 'broth'));
      else if (/Sat$/.test(name)) disc(ox, () => { ccOrbLiquid(g, ox, oy, liq, 'broth'); ccOrbSatTint(g, ox, oy); });
      else if (name === 'irons' || name === 'ars') disc(ox, () => ccOrbLiquid(g, ox, oy, liq, 'mana'));
      else if (name === 'glassL' || name === 'glassR') ccOrbGlassBack(g, ox, oy);
      else if (name === 'front') disc(ox, () => ccOrbGlassFront(g, ox, oy));
      else if (name === 'divider') disc(ox, () => ccOrbDivider(g, ox, oy));
      else if (name === 'bezelL' || name === 'bezelR') ccOrbBezel(g, ox, oy);
      else if (name === 'cog') ccHousingCog(g, ox, oy);
      else if (name.startsWith('crack')) ccOrbCracks(g, ox, oy, +name.slice(5));
      else if (name.startsWith('glow_')) {   // on black: the game adds it with a screen blend (ONE, ONE_MINUS_SRC_COLOR)
        g.fillStyle = '#000'; g.fillRect(ox - 16, oy - 16, 32, 32);
        ccOrbGlow(g, ox, oy, ox - ORB.housing, 2 * ORB.housing, CCX_LIQ[name.slice(5)], oy - r - 4);
      } else if (name.startsWith('dash_')) ccDashBase(g, ox, oy, name.slice(5));
      else if (name.startsWith('ring_')) { const [, t, k] = name.split('_'); ccDashRing(g, ox, oy, t, +k, +k); }
    });
  });
  RING_CELLS.forEach((name, m) => {
    const ox = name.startsWith('armor') ? ORB.lx : ORB.rx;
    at(ccxRing(m), 24, ox, ox => {
      if (name === 'armor') ccOrbRing(g, ox, oy, { frac: 1, on: CCX_LIQ.armor });
      else if (name === 'armorHalf') ccOrbRing(g, ox, oy, { frac: 1, on: CCX_LIQ.armor, half: true });
      else ccOrbRing(g, ox, oy, { frac: 1, on: name === 'tank' ? ['#ffd9b0', '#c98a5a', '#5a2814'] : CCX_LIQ[name], smooth: true });
    });
  });
  return c;
}

/** [[file name, canvas]] for the pack. */
function compactExports() {
  const files = [];
  for (const [name, orbs, meka] of [['bars', false, false], ['bars_meka', false, true], ['orbs', true, false], ['orbs_meka', true, true]]) {
    const [c, g] = hires(332, 34);
    g.translate(166, -17);
    const ends = ccEnds({ orbs, mekasuit: meka ? 1 : 0 });
    ccPlate(g, { inner: orbs ? ORB.innerEnd : 0, x0: ends.x0, x1: ends.x1, gradTop: orbs ? ORB.y - ORB.housing : undefined });
    if (orbs) { ccHousing(g, ORB.lx, ORB.y, 1); ccHousing(g, ORB.rx, ORB.y, -1); }
    files.push(['createhud: compact/plate_' + name, c]);
  }
  const [c, g] = hires(256, 256);
  socket(g, 0, 0);
  ccSelection(g, 23, 3);   // snug on a socket at (23, 3): the game draws this 24x23 cell at the socket - 3
  g.save(); g.translate(46 + 9, 9.4); g.scale(1.25, 1.25); g.translate(-4.5, -4.5);
  shieldPath(g); g.setLineDash([0.4, 0.4]); g.strokeStyle = '#3a414b'; g.lineWidth = 0.4; g.stroke(); g.restore();
  ccFloatPlate(g, 66, 0, CC_SKULL_PLATE.w, CC_SKULL_PLATE.h); ccSkull(g, 66 + CC_SKULL_PLATE.w / 2, CC_SKULL_PLATE.h / 2);
  ccLevelWindow(g, 78, 0, 10, CC_LEVEL.h);
  ccLevelWindow(g, 90, 0, CC_LEVEL.iconW, CC_LEVEL.iconH);
  CCX.winIcons.forEach((n, k) => ccIcon(g, ccxIcon(n), 100 + 7 * k + 3.5, 3.5, CC_LEVEL.icon));
  ccMekaCasing(g, 136, 0); ccMekaFill(g, 140, 0);
  const MW = CC.meterW;
  ccMeter(g, 0, 20, MW, 4, 0, CCX_LIQ.blood, 10);
  ccMeter(g, 0, 26, CC.xp.x1 - CC.xp.x0, CC.xp.h, 0, CCX_LIQ.xp, 18);
  // Full meters: the liquid with its style but without the parts that move with the level (mana stars, broth
  // bubbles: the game draws those), the segment lines over it.
  const fullMeter = (x, y, w, h, n, segs) => {
    ccMeter(g, x, y, w, h, 1, CCX_LIQ[n], segs, 'left', CCX_STYLE[n] === 'cloud' ? 'cloud' : undefined);
    const st = CCX_STYLE[n];
    if (st === 'broth' || st === 'mana') {   // the style's texture alone (no bubbles or stars), then the lines again
      g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.clip();
      if (st === 'mana') {
        g.globalCompositeOperation = 'multiply'; g.globalAlpha = 0.5;
        const pat = g.createPattern(ccNebula(), 'repeat');
        pat.setTransform(new DOMMatrix().translateSelf((x + 0.5) * 3, y * 5).rotateSelf(60).scaleSelf(0.05, 0.05));
        g.fillStyle = pat; g.fillRect(x, y, w, h);
      } else {
        const area = () => { g.beginPath(); g.rect(x + 0.5, y, w - 1, h); };
        surface(g, area, TEX.blotch, 0.45 * ORB.tex, 'overlay'); surface(g, area, TEX.rock, 0.25 * ORB.tex, 'soft-light');
      }
      g.restore();
      g.fillStyle = 'rgba(0,0,0,0.55)';
      for (let k = 1; k < segs; k++) g.fillRect(x + 0.5 + (w - 1) * k / segs - 0.15, y, 0.3, h);
    }
  };
  CCX.wingLiquids.forEach((n, k) => fullMeter(0, 30 + 5 * k, MW, 4, n, 10));
  CCX.xpLiquids.forEach((n, k) => fullMeter(0, 112 + 4 * k, CC.xp.x1 - CC.xp.x0, CC.xp.h, n, 18));
  CCX.digitRows.forEach((col, r) => [...CCX.digits].forEach((ch, k) => ccNumber(g, ch, 3.5 * k, 140 + 4 * r, col, null, 0.625)));
  CCX.tints.forEach((n, r) => [...CCX.glyphs].forEach((ch, k) => ccNumber(g, ch, 180 + 3 * k, 20 + 3.5 * r, ccxTint(n), null, 0.6)));
  CCX.levelRows.forEach((col, r) => [...CCX.glyphs].forEach((ch, k) => ccNumber(g, ch, 180 + 3 * k, 76 + 3.5 * r, col === '#000000' ? 'rgba(0,0,0,0.9)' : col, null, CC_LEVEL.k)));
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
  files.push(['createhud: compact/orbs', ccxOrbs()]);
  return files;
}
