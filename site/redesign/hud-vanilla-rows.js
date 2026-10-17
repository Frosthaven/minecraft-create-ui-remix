// Vanilla layout: the icon rows by vanilla's rules (Gui.java 1.21: renderHearts, renderFood, renderVehicleHealth, the
// armor and air rows), with the icons of hud-vanilla-icons.js and our overlays: AppleSkin (saturation outline, held
// food preview, exhaustion strip), natural heal, Farmer's Delight Comfort and Nourishment. Each row returns its top y.

const vanRand = (a, b) => { const v = Math.sin(a * 127.1 + b * 311.7) * 43758.5453; return v - Math.floor(v); };
const vanTick = s => Math.floor(s.t / 50);   // the game's 20 ticks a second
/** Cell i of a row showing v half-points: 'full', 'half' or 'empty' (vanilla: full below 2 i + 1). */
const vanKind = (v, i) => 2 * i + 1 < v ? 'full' : 2 * i + 1 === v ? 'half' : 'empty';
const vanPulse = s => 0.35 + 0.25 * Math.sin(s.t / 160);   // AppleSkin's preview flash
/** While a row's numbers show, its icons draw at this alpha (the meters dim their liquid behind the digits). */
const VAN_DIM = 0.4;
let vanDim = 1;
/** Draws img at (x, y) at alpha a (0: nothing), times the row's dimming. */
function vanAt(g, img, x, y, a = 1) { a *= vanDim; if (a <= 0) return; g.globalAlpha = a; g.drawImage(img, x, y, 9, 9); g.globalAlpha = 1; }
/** Whether a row's numbers show (hovered, or All numbers); sets the dimming for the icons drawn next. */
const vanShown = (s, id) => { const on = !!(s.numbers || s.hover === id); vanDim = on ? VAN_DIM : 1; return on; };

/** Hearts from x0, the lowest row's top at y0. As vanilla: ceil(max / 2) containers, then the absorption hearts, 10 a
 *  row, rows rising by max(10 - (rows - 2), 3); the low health shake (health + absorption 4 or less). Overlays: the
 *  damage flash (the meters' red, fading over 400 ms, on every heart), AppleSkin's held food preview, natural heal's
 *  pulse, Comfort's sheen, Regeneration's pink motes (vanRegen, the meters' and orbs' look in place of vanilla's wave). Returns
 *  the top row's y. */
function vanHearts(g, s, x0, y0) {
  const max = s.maxHealth || 20, hp = Math.ceil(s.health), absorb = Math.ceil(s.absorb || 0);
  const n = Math.ceil(max / 2), total = n + Math.ceil(absorb / 2), rows = Math.ceil(total / 10), rh = Math.max(10 - (rows - 2), 3);
  const since = s.t - (s.hurtAt ?? -1e9), flash = since < 400 ? 0.55 * (1 - since / 400) : 0;   // as ccHealthFx
  const look = { fill: s.effect ? ccHealthLiquid(s) : null };
  const preview = s.holdFood && s.holdHeal ? Math.min(max, hp + s.holdHeal) : hp, heal = ccHealPulse(s);
  const comfort = s.comfort && s.saturation <= 0 && s.health < max, sweep = ((s.t / 2600) % 1) * 92 - 6;
  for (let i = total - 1; i >= 0; i--) {
    const x = x0 + 8 * (i % 10);
    let y = y0 - rh * Math.floor(i / 10);
    if (s.health + absorb <= 4) y += Math.floor(vanRand(i, vanTick(s)) * 2);
    vanAt(g, vanHeart('empty', look), x, y);
    if (i >= n) {   // absorption: gold hearts after the containers
      const a = absorb - 2 * (i - n);
      vanAt(g, vanHeart(a >= 2 ? 'full' : 'half', { fill: LIQUID.absorb }), x, y);
      continue;
    }
    const k = vanKind(hp, i);
    if (k !== 'empty') vanAt(g, vanHeart(k, look), x, y);
    vanAt(g, vanGlow('heart', 'full', '#ff2828'), x, y, flash);   // the damage flash, over the whole row
    if (preview > hp && vanKind(preview, i) !== k) vanAt(g, vanHeart(vanKind(preview, i), look), x, y, vanPulse(s));
    if (k !== 'empty') {
      const side = k === 'half' ? 'left' : 'full';
      vanAt(g, vanGlow('heart', side, `rgb(${CC_HEAL.rgb})`), x, y, heal);   // natural heal
      if (comfort) vanAt(g, vanGlow('heart', side, '#ff9ec4'), x, y, Math.max(0, 1 - Math.abs(x + 4.5 - x0 - sweep) / 8) * 0.55);
    }
  }
  if (s.regen) for (let r = 0; r < rows; r++) vanRegen(g, s, x0, y0 - rh * r, Math.min(10, Math.max(0, hp / 2 - 10 * r)) * 8, r);
  return y0 - rh * (rows - 1);
}
/** Regeneration on a heart row (top y) from x0 over w (the filled hearts): the meters' pink motes drifting left to right
 *  and fading at the ends, a few to a heart (ccHealthFx's look at the hearts' size). */
function vanRegen(g, s, x0, y, w, seed) {
  if (w <= 0) return;
  for (let i = 0; i < Math.ceil(w / 9); i++) {
    const hh = Math.sin((i + 1 + seed * 13) * 91.7) * 43758.5453, u = hh - Math.floor(hh), k = ((s.t / 1500) * (0.6 + u * 0.6) + u * 3.7) % 1;
    const py = y + 5.4 + 2.2 * Math.sin(i * 2.3 + s.t / 300);
    circle(g, x0 + 0.5 + k * (w - 1), py, 0.5 + (i % 3) * 0.12); g.fillStyle = `rgba(255,120,175,${Math.sin(Math.PI * k) * 0.95})`; g.fill();
  }
}
/** Food from the right end x1, the row's top at y0. As vanilla: right to left, the Hunger effect's spoiled food, the
 *  shake at saturation 0 (a tick in food * 3 + 1). Overlays: AppleSkin's held food preview and saturation outline,
 *  Nourishment's breathing gold halo on the saturated cells. */
function vanFoodRow(g, s, x1, y0) {
  const meat = s.hunger ? LIQUID.hunger : undefined, sat = Math.min(s.saturation, s.food), tick = vanTick(s);
  const shake = s.saturation <= 0 && tick % (s.food * 3 + 1) === 0, preview = Math.min(20, s.food + (s.holdFood || 0));
  for (let i = 0; i < 10; i++) {
    const x = x1 - 9 - 8 * i, y = y0 + (shake ? Math.floor(vanRand(i, tick) * 3) - 1 : 0), k = vanKind(s.food, i);
    vanAt(g, vanFood('empty'), x, y);
    if (k !== 'empty') vanAt(g, vanFood(k, meat), x, y);
    if (preview > s.food && vanKind(preview, i) !== k) vanAt(g, vanFood(vanKind(preview, i), meat), x, y, vanPulse(s));
    const sk = vanKind(sat, i);
    if (sk !== 'empty') {
      vanAt(g, vanSatRing(sk), x, y);
      if (s.nourished) vanAt(g, vanHalo('food', sk === 'half' ? 'right' : 'full', `rgb(${CC_NOURISH.rgb})`), x, y, 0.55 + 1.8 * (ccNourishGlow(s) - CC_NOURISH.base));   // breathing 0.4 to 0.7
    }
  }
}
/** The mount's hearts in place of food (riding), from the right end x1, the lowest row's top at y0: as vanilla, up to 30
 *  hearts, 10 a row, rows 10 apart, in the mount's emerald. Returns the top row's y. */
function vanMountRow(g, s, x1, y0) {
  const n = Math.min(30, Math.ceil(s.mountMax / 2)), v = Math.ceil(s.mount), look = { fill: CC_MOUNT };
  for (let i = 0; i < n; i++) {
    const x = x1 - 9 - 8 * (i % 10), y = y0 - 10 * Math.floor(i / 10), k = vanKind(v, i);
    vanAt(g, vanHeart('empty', look), x, y);
    if (k !== 'empty') vanAt(g, vanHeart(k, look), x, y);
  }
  return y0 - 10 * (Math.ceil(n / 10) - 1);
}
/** The breath row from the right end x1 at y: bubbles by vanilla's rule (full, then the popping one), or while a
 *  breathing gear supplies air its copper tanks, the mm:ss time right-aligned to the food's edge (x1 - 1, its shadow on x1), the tanks to its left
 *  after a 3 px gap (as many as fit beside the time, filled in halves). Returns true when shown. */
function vanBreathRow(g, s, x1, y, rowW) {
  if (!ccBreathShown(s)) return false;
  const tank = ccTankLeft(s);
  if (tank > 0) {
    const t = Math.ceil(tank), txt = `${String(Math.floor(t / 60)).padStart(2, '0')}:${String(t % 60).padStart(2, '0')}`;
    const right = x1 - 1 - ccNumberWidth(txt) - 3, n = Math.floor((right - (x1 - rowW) - 9) / 8) + 1, v = Math.ceil(tank / s.tankMax * 2 * n);
    for (let i = 0; i < n; i++) vanAt(g, vanTank(vanKind(v, i)), right - 9 - 8 * i, y);
    ccNumber(g, txt, x1 - 1 - ccNumberWidth(txt), y + 5.25 - 2.5, ccTint('#d9874a'), 'rgba(0,0,0,0.9)');   // centred on the tanks
    return true;
  }
  const a = s.air * 300, full = Math.ceil((a - 2) * 10 / 300), pop = Math.ceil(a * 10 / 300) - full;
  for (let i = 0; i < full + pop; i++) vanAt(g, air(i >= full), x1 - 9 - 8 * i + 0.75, y);   // the bubble is centred in its cell: 0.75 right puts its edge on the food's
  return true;
}
/** A row's value / max, shown while hovered (or with All numbers): the meters' 0.6 digits in the row's tint with their
 *  shadow, centred on box, over the dimmed icons (vanShown; the game: the parts atlas's tinted glyph rows). */
function vanNumbers(g, s, id, box, txt, col) {
  CC.hit[id] = box;
  vanDim = 1;
  if (!(s.numbers || s.hover === id)) return;
  const k = 0.6, w = ccNumberWidth(txt, k), [x, y, bw, bh] = box, cx = x + bw / 2, cy = y + bh / 2;
  ccNumber(g, txt, cx - w / 2, cy - 2.5 * k, ccTint(col), 'rgba(0,0,0,0.9)', k);
}
