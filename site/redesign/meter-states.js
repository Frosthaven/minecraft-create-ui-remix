// The meters in every state the game draws, placed by its rules (Gui 1.21.1: renderHearts, renderFood,
// renderVehicleHealth, renderAirLevel; all right to left). Each row returns [sprite, x, y] in draw order, x and y
// relative to the row's top left. Needs meters.js (meterSprites) and the page's make() and nearest flag.

/** Hearts as Gui.renderHearts draws them: container, absorption, the flash of lost health, then health. */
function heartRow(M, { health, max = 20, absorb = 0, type = '', hardcore = false, flash = 0, shake = null, regen = -1 }) {
  const V = M.variants, hc = hardcore ? 'hardcore_' : '';
  const heart = (t, half, blink) => V[`hud/heart/${t}${hc}${half ? 'half' : 'full'}${blink ? '_blinking' : ''}`];
  const container = V[`hud/heart/container${hardcore ? '_hardcore' : ''}${flash ? '_blinking' : ''}`];
  const n = Math.ceil(max / 2), a = Math.ceil(absorb / 2), rows = Math.ceil((n + a) / 10), rh = Math.max(10 - (rows - 2), 3);
  const shown = health + flash, out = [];
  for (let l = n + a - 1; l >= 0; l--) {
    const x = (l % 10) * 8;
    let y = 2 + (rows - 1 - Math.floor(l / 10)) * rh;
    if (shake) y += shake[l % shake.length];
    if (l < n && l === regen) y -= 2;
    out.push([container, x, y]);
    const q = l * 2;
    if (l >= n && q - n * 2 < absorb) out.push([heart(type === 'withered_' ? type : 'absorbing_', q - n * 2 + 1 === absorb, false), x, y]);
    if (flash && q < shown) out.push([heart(type, q + 1 === shown, true), x, y]);
    if (q < health) out.push([heart(type, q + 1 === health, false), x, y]);
  }
  return out;
}
/** Food as Gui.renderFood draws it (right to left), plain or with the Hunger effect. */
function foodRow(M, food, hunger) {
  const V = M.variants, out = [];
  for (let i = 0; i < 10; i++) {
    const x = 72 - 8 * i;
    out.push([hunger ? V['hud/food_empty_hunger'] : M.foodEmpty, x, 2]);
    if (i * 2 + 1 < food) out.push([hunger ? V['hud/food_full_hunger'] : M.foodFull, x, 2]);
    else if (i * 2 + 1 === food) out.push([hunger ? V['hud/food_half_hunger'] : M.foodHalf, x, 2]);
  }
  return out;
}
/** A mount's health in place of the food row (renderVehicleHealth: right to left, 10 per row). */
function vehicleRow(M, hp, max) {
  const V = M.variants, out = [], n = Math.ceil(max / 2), rows = Math.ceil(n / 10);
  for (let l = 0; l < n; l++) {
    const x = 72 - 8 * (l % 10), y = 2 + (rows - 1 - Math.floor(l / 10)) * 10;
    out.push([V['hud/heart/vehicle_container'], x, y]);
    if (l * 2 + 1 < hp) out.push([V['hud/heart/vehicle_full'], x, y]);
    else if (l * 2 + 1 === hp) out.push([V['hud/heart/vehicle_half'], x, y]);
  }
  return out;
}
/** Air as Gui.renderAirLevel draws it: full bubbles from the right, then the one bursting; nothing where it is gone. */
function airRow(M, air, max = 300) {
  const full = Math.ceil((air - 2) * 10 / max), burst = Math.ceil(air * 10 / max) - full, out = [];
  for (let i = 0; i < full + burst; i++) out.push([i < full ? M.air : M.airBurst, 72 - 8 * i, 2]);
  return out;
}

/** A row's canvas at scale, on the given background. */
function stateCanvas(items, scale, bg) {
  const h = Math.max(...items.map(([, , y]) => y)) + 11;
  const [c, g] = make(85 * scale, h * scale);
  g.fillStyle = bg; g.fillRect(0, 0, c.width, c.height);
  for (const [img, x, y] of items) {
    const k = img.width / img.lw;
    g.imageSmoothingEnabled = !nearest; g.imageSmoothingQuality = 'high';
    g.drawImage(img, 0, 0, 9 * k, 9 * k, (2 + x) * scale, y * scale, 9 * scale, 9 * scale);
  }
  return c;
}

/** Every state, labelled. */
function meterStates(M) {
  const low = [1, 0, 1, 1, 0, 0, 1, 0, 1, 0];   // a sample of the shake at 4 health or less (random 0 or 1 px each)
  return [
    ['Health 15 of 20', heartRow(M, { health: 15 })],
    ['Poison', heartRow(M, { health: 15, type: 'poisoned_' })],
    ['Wither', heartRow(M, { health: 15, type: 'withered_' })],
    ['Freezing (powder snow)', heartRow(M, { health: 15, type: 'frozen_' })],
    ['Absorption (8 extra: a row above)', heartRow(M, { health: 20, absorb: 8 })],
    ['Hardcore world', heartRow(M, { health: 15, hardcore: true })],
    ['Damage flash (lost 4, blinking)', heartRow(M, { health: 11, flash: 4 })],
    ['Low health (3): shakes', heartRow(M, { health: 3, shake: low })],
    ['Regeneration: one segment lifted, in a wave', heartRow(M, { health: 15, regen: 4 })],
    ['Food 15 of 20', foodRow(M, 15, false)],
    ['Hunger effect', foodRow(M, 15, true)],
    ['Riding: horse health 23 of 30 (replaces food)', vehicleRow(M, 23, 30)],
    ['Air running out (about 40 %)', airRow(M, 120)],
  ];
}
