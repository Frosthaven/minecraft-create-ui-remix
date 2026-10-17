// Almost Vanilla's texture for the game (createhud:textures/gui/compact/vanilla, 224 x 96 at 8x), drawn by the same
// functions as the page (hud-vanilla-icons.js, ccMeterFill's liquids, ccNumber). haven-create-ui-ux's VanillaHud draws
// from it; the sockets, selection, level window, dial and skull come from the parts and orbs atlases.
// KEEP IN STEP with CompactAtlas.java (VAN_*).
//   (10 (n % 16), 10 floor(n / 16)) icon cell n, 9 x 9: VANX.cells.
//   (0, 32) the gauge's frame 178 x 5 (casing and empty glass).
//   (0, 40 + 3 k) liquid strips 177 x 2 (the gauge's channels with mana): VANX.strips2.
//   (0, 64 + 5 k) liquid strips 177 x 4 (the XP channel alone): VANX.strips4.
//   (164 + 4 i, 0 | 6) the tank time's glyphs "0123456789:/+" at 1 (3 x 5): the tank's tint, then the black shadow.
const VANX = {
  cells: ['heart_empty', ...['blood', 'poison', 'wither', 'frozen', 'absorb', 'mount'].flatMap(f => [`heart_${f}_full`, `heart_${f}_half`]),
    'food_empty', 'food_full', 'food_half', 'food_full_hunger', 'food_half_hunger', 'sat_full', 'sat_half',
    'glow_heal_full', 'glow_heal_left', 'glow_comfort_full', 'glow_comfort_left', 'glow_flash', 'halo_full', 'halo_right',
    'tank_full', 'tank_half', 'tank_empty', 'armor_full', 'armor_half', 'armor_empty', 'air', 'air_burst'],
  strips2: ['irons', 'ars', 'xp', 'jump', 'cooldown', 'cast', 'train', 'engine'],
  strips4: ['xp', 'jump', 'cooldown', 'cast', 'train', 'engine'],
  glyphs: '0123456789:/+',
};
/** A heart fill's colours by name (the page's looks: blood is the icons' own red). */
const vanxFill = n => ({ blood: VAN_HEART, poison: LIQUID.poison, wither: LIQUID.wither, frozen: LIQUID.frozen, absorb: LIQUID.absorb, mount: CC_MOUNT })[n];
/** An icon cell's canvas by name. */
function vanxCell(name) {
  const [kind, a, b] = name.split('_');
  if (kind === 'heart') return a === 'empty' ? vanHeart('empty') : vanHeart(b, { fill: vanxFill(a) });
  if (kind === 'food') return a === 'empty' ? vanFood('empty') : vanFood(a, b === 'hunger' ? LIQUID.hunger : undefined);
  if (kind === 'sat') return vanSatRing(a);
  if (kind === 'glow') return vanGlow('heart', b || 'full', { heal: `rgb(${CC_HEAL.rgb})`, comfort: '#ff9ec4', flash: '#ff2828' }[a]);
  if (kind === 'halo') return vanHalo('food', a, `rgb(${CC_NOURISH.rgb})`);
  if (kind === 'tank') return vanTank(a);
  if (kind === 'armor') return armor(a);
  return air(name === 'air_burst');
}
/** A liquid strip without what moves with the fill (the game draws the mana stars and the segment lines). */
function vanxStrip(g, y, h, n) {
  const liq = CCX_LIQ[n], style = CCX_STYLE[n], area = () => { g.beginPath(); g.rect(0, y, 177, h); };
  g.fillStyle = vgrad(g, y, y + h, [[0, mixHex(liq[0], liq[1], 0.5)], [0.45, liq[1]], [1, liq[2]]]); g.fillRect(0, y, 177, h);
  g.save(); area(); g.clip();
  if (style === 'mana') {
    g.globalCompositeOperation = 'multiply'; g.globalAlpha = 0.5;
    const pat = g.createPattern(ccNebula(), 'repeat');
    pat.setTransform(new DOMMatrix().translateSelf(0, y * 5).rotateSelf(60).scaleSelf(0.05, 0.05)); g.fillStyle = pat; area(); g.fill();
  } else if (style) { surface(g, area, TEX.blotch, 0.45 * ORB.tex, 'overlay'); surface(g, area, TEX.rock, 0.25 * ORB.tex, 'soft-light'); }
  g.restore();
}
/** The atlas. */
function vanillaAtlas() {
  const [c, g] = hires(224, 96);
  VANX.cells.forEach((n, i) => g.drawImage(vanxCell(n), 10 * (i % 16), 10 * Math.floor(i / 16), 9, 9));
  rrect(g, 0, 32, 178, 5, 1); g.fillStyle = vgrad(g, 32, 37, [[0, '#9ea4aa'], [0.25, '#6c7278'], [0.65, '#3a3e43'], [1, '#141619']]); g.fill();
  rrect(g, 0.5, 32.5, 177, 4, 0.5); g.fillStyle = '#060708'; g.fill();
  VANX.strips2.forEach((n, k) => vanxStrip(g, 40 + 3 * k, 2, n));
  VANX.strips4.forEach((n, k) => vanxStrip(g, 64 + 5 * k, 4, n));
  [...VANX.glyphs].forEach((ch, i) => {
    ccNumber(g, ch, 164 + 4 * i, 0, ccTint('#d9874a'), null);
    ccNumber(g, ch, 164 + 4 * i, 6, 'rgba(0,0,0,0.9)', null);
  });
  return c;
}
