// Dark steampunk HUD sprites, vector art at R x the vanilla size (Minecraft draws GUI sprites at their
// logical size whatever the texture resolution, so the detail survives). Coordinates are logical.
// Value plan: near-black iron bodies, mid bronze, crisp bright brass edges where the light hits, and the
// brightest values only in things that give light (lamp, fluids, porthole glass).
const R = 8;
const P = {
  void: '#090706', iron0: '#16120f', iron1: '#2a241e', iron2: '#4a4036',
  bronze: '#6e4f22', brass: '#b8893f', brassHi: '#f4d898', copper: '#9a4a28', copperHi: '#f2a676',
};

/** A canvas for a sprite of logical size w x h, drawn at R x; g works in logical units. */
function hires(w, h) {
  const c = document.createElement('canvas');
  c.width = w * R; c.height = h * R;
  c.lw = w; c.lh = h;
  const g = c.getContext('2d');
  g.scale(R, R);
  g.lineJoin = 'round'; g.lineCap = 'round';
  return [c, g];
}
function rrect(g, x, y, w, h, r) { g.beginPath(); g.roundRect(x, y, w, h, r); }
function circle(g, x, y, r) { g.beginPath(); g.arc(x, y, r, 0, Math.PI * 2); }
function vgrad(g, y0, y1, stops) {
  const gr = g.createLinearGradient(0, y0, 0, y1);
  for (const [t, col] of stops) gr.addColorStop(t, col);
  return gr;
}
function radial(g, x, y, r, stops, fx = x, fy = y) {
  const gr = g.createRadialGradient(fx, fy, 0, x, y, r);
  for (const [t, col] of stops) gr.addColorStop(t, col);
  return gr;
}
/** Lit brass, top to bottom: a hot edge, the body, a dark underside. */
const brass = (g, y0, y1) => vgrad(g, y0, y1, [[0, P.brassHi], [0.18, P.brass], [0.6, P.bronze], [1, '#2a1c0a']]);
const copper = (g, y0, y1) => vgrad(g, y0, y1, [[0, P.copperHi], [0.2, P.copper], [0.65, '#5a2814'], [1, '#220d05']]);
const iron = (g, y0, y1) => vgrad(g, y0, y1, [[0, P.iron2], [0.12, P.iron1], [0.7, P.iron0], [1, P.void]]);
/** A thin highlight on the upper half of a shape's outline (light from above). */
function rim(g, path, y0, y1, col = P.brassHi, lw = 0.3) {
  g.save(); g.beginPath(); g.rect(-1, y0, 999, (y1 - y0) / 2); g.clip();
  path(); g.strokeStyle = vgrad(g, y0, y0 + (y1 - y0) / 2, [[0, col], [1, 'rgba(0,0,0,0)']]); g.lineWidth = lw; g.stroke();
  g.restore();
}
/** A domed brass rivet with a hard specular point. */
function rivet(g, x, y, r) {
  circle(g, x, y + r * 0.25, r * 1.1); g.fillStyle = 'rgba(0,0,0,0.6)'; g.fill();
  circle(g, x, y, r); g.fillStyle = radial(g, x, y, r, [[0, '#fff3cc'], [0.25, P.brass], [0.7, P.bronze], [1, '#24180a']], x - r * 0.4, y - r * 0.45); g.fill();
}
const steel = (g, y0, y1) => vgrad(g, y0, y1, [[0, '#d6dade'], [0.15, '#8b9198'], [0.6, '#41464c'], [1, '#121417']]);
function steelSurface(g, path) { surface(g, path, TEX.brushed, 0.2); surface(g, path, TEX.blotch, 0.1); }
/** A steel hex bolt head with a lit upper face. */
function hexBolt(g, x, y, r) {
  const hex = (rr, dy = 0) => {
    g.beginPath();
    for (let i = 0; i < 6; i++) { const t = Math.PI / 6 + i * Math.PI / 3; g.lineTo(x + Math.cos(t) * rr, y + dy + Math.sin(t) * rr); }
    g.closePath();
  };
  hex(r * 1.1, r * 0.25); g.fillStyle = 'rgba(0,0,0,0.6)'; g.fill();
  hex(r); g.fillStyle = radial(g, x, y, r, [[0, '#eef1f4'], [0.3, '#8b9198'], [1, '#24272b']], x - r * 0.4, y - r * 0.5); g.fill();
  circle(g, x, y, r * 0.3); g.fillStyle = 'rgba(0,0,0,0.45)'; g.fill();
}
/** A cogwheel path: trapezoid teeth around a rim. */
function cogPath(g, cx, cy, rOut, rIn, teeth) {
  g.beginPath();
  const step = Math.PI * 2 / teeth;
  for (let i = 0; i < teeth; i++) {
    const a = i * step;
    for (const [r, t] of [[rIn, a - step * 0.5], [rIn, a - step * 0.22], [rOut, a - step * 0.14], [rOut, a + step * 0.14], [rIn, a + step * 0.22]]) {
      g.lineTo(cx + Math.cos(t) * r, cy + Math.sin(t) * r);
    }
  }
  g.closePath();
}
/** A light source: a soft halo and a hot core. */
function glow(g, x, y, r, col, core) {
  circle(g, x, y, r); g.fillStyle = radial(g, x, y, r, [[0, col], [1, 'rgba(0,0,0,0)']]); g.fill();
  circle(g, x, y, r * 0.35); g.fillStyle = core; g.fill();
}

/** An item socket: a lit brass bezel around a black well (item area 16 x 16 at x+1, y+1). */
function socket(g, x, y) {
  // The brass bezel is 0.5 wide (x + 0.5 .. x + 1), around the 16 x 16 well where the game draws the item.
  const bezel = () => rrect(g, x + 0.5, y + 0.5, 17, 17, 1);
  bezel(); g.fillStyle = brass(g, y, y + 18); g.fill();
  brassSurface(g, bezel);
  rrect(g, x + 1, y + 1, 16, 16, 0.6); g.fillStyle = vgrad(g, y + 1, y + 17, [[0, '#030202'], [1, '#141009']]); g.fill();
  // The well's upper wall in shadow, its lower lip catching the light.
  g.save(); rrect(g, x + 1, y + 1, 16, 16, 0.6); g.clip();
  g.fillStyle = 'rgba(0,0,0,0.7)'; g.fillRect(x + 1, y + 1, 16, 1.2);
  g.strokeStyle = 'rgba(244,216,152,0.35)'; g.lineWidth = 0.3;
  g.beginPath(); g.moveTo(x + 1.6, y + 16.8); g.lineTo(x + 16.4, y + 16.8); g.stroke();
  g.restore();
  rim(g, () => rrect(g, x + 0.6, y + 0.6, 16.8, 16.8, 0.9), y, y + 18, P.brassHi, 0.2);
}

/** hud/hotbar 182x22: a blackened iron plate in a brushed steel frame with gear-rack teeth along its
 *  edges, nine brass sockets, hex bolts between them. */
function hotbar() {
  const [c, g] = hires(182, 22);
  const plate = () => rrect(g, 0.4, 0.4, 181.2, 21.2, 1.2);
  plate(); g.fillStyle = iron(g, 0, 22); g.fill();
  ironSurface(g, plate);
  // The steel frame, 0.5 wide (0.4 .. 0.9), its outer edge lit; rack teeth notched into it. No bolts between sockets.
  const frame = () => rrect(g, 0.65, 0.65, 180.7, 20.7, 1.15);
  frame(); g.strokeStyle = steel(g, 0, 22); g.lineWidth = 0.5; g.stroke();
  rim(g, () => rrect(g, 0.5, 0.5, 181, 21, 1.25), 0, 22, '#eef1f4', 0.25);
  g.fillStyle = 'rgba(8,9,10,0.85)';
  for (let x = 3; x < 179; x += 2) { g.fillRect(x, 0.4, 0.9, 0.3); g.fillRect(x, 21.3, 0.9, 0.3); }
  for (let i = 0; i < 9; i++) socket(g, 2 + i * 20, 2);
  return c;
}

/** hud/hotbar_selection 24x23: a steel frame with an ember halo and an amber indicator lamp. */
function selection() {
  const [c, g] = hires(24, 23);
  g.save(); g.shadowColor = 'rgba(255,140,30,1)'; g.shadowBlur = 3.5;
  g.strokeStyle = steel(g, 1, 22); g.lineWidth = 0.65;   // half the old 1.3, as the hotbar's borders
  for (let k = 0; k < 2; k++) { rrect(g, 1.4, 1.4, 21.2, 20.2, 1.4); g.stroke(); }
  g.restore();
  rim(g, () => rrect(g, 0.8, 0.8, 22.4, 21.4, 1.8), 0.8, 22.2, '#f4f6f8', 0.35);
  // Lamplight spilling onto the socket bezel.
  g.strokeStyle = 'rgba(255,200,110,0.95)'; g.lineWidth = 0.4; rrect(g, 2.35, 2.35, 19.3, 18.3, 0.9); g.stroke();
  // The indicator lamp: a steel cap and a glowing bulb.
  rrect(g, 9.6, 0, 4.8, 1.3, 0.4); g.fillStyle = steel(g, 0, 1.3); g.fill();
  glow(g, 12, 1.9, 3.2, 'rgba(255,170,50,0.9)', '#fff1b0');
  return c;
}

/** hud/hotbar_offhand_left 29x24: an iron bracket in a steel frame, a steel shaft into the hotbar with a
 *  bolted brass coupling. */
function offhand() {
  const [c, g] = hires(29, 24);
  const shaft = () => rrect(g, 21, 10.5, 8, 3, 0.3);
  shaft(); g.fillStyle = steel(g, 10.5, 13.5); g.fill(); steelSurface(g, shaft);
  const collar = () => rrect(g, 24, 9.2, 3, 5.6, 0.5);
  collar(); g.fillStyle = brass(g, 9.2, 14.8); g.fill(); brassSurface(g, collar);
  g.strokeStyle = 'rgba(0,0,0,0.5)'; g.lineWidth = 0.2;
  g.beginPath(); g.moveTo(25.5, 9.4); g.lineTo(25.5, 14.6); g.stroke();
  rim(g, collar, 9.2, 14.8);
  hexBolt(g, 25.5, 10.5, 0.45); hexBolt(g, 25.5, 13.5, 0.45);
  const plate = () => rrect(g, 0.5, 1.5, 21, 21, 1.2);
  plate(); g.fillStyle = iron(g, 1.5, 22.5); g.fill(); ironSurface(g, plate);
  g.strokeStyle = steel(g, 1.5, 22.5); g.lineWidth = 0.5; rrect(g, 0.75, 1.75, 20.5, 20.5, 1.1); g.stroke();   // 0.5, as the hotbar's
  rim(g, () => rrect(g, 0.55, 1.55, 20.9, 20.9, 1.2), 1.4, 22.6, '#eef1f4', 0.25);
  socket(g, 2, 3);
  return c;
}

/** hud/experience_bar_background 182x5: a steel pipe with a dark glass window, bolted clamp bands,
 *  brass end caps. */
/** The experience pipe in the meters' style (meters.js): plain shaded steel, rounded ends, a window 3 px tall (rows 1 to
 *  4, as the meters), and 0.5 px separators lined up with the meters' above it: the hearts' (cells 8 apart from x 0,
 *  separator at 8.5 in each) on the left half, the food's (cells from x 173 leftward) on the right half, one in the
 *  middle gap. Dark grey on the empty glass, a darker green over the liquid. No texture or clamps, and no end caps:
 *  like the meters, the window runs to the ends, edged only by a 0.5 px separator and the 0.5 rounded corners. */
// The bar (window) is half the meters' height, rows 2.5 to 4; the pipe gives it 1 px of steel above and below, as the
// health and food meters have (rows 1.5 to 5; the sprite's top 1.5 rows stay empty).
const XP_ROWS = [1.5, 5], XP_WIN = [2.5, 4];
const XP_SEPS = [...Array.from({ length: 10 }, (_, k) => 8.5 + 8 * k), 90.75, ...Array.from({ length: 10 }, (_, i) => 101.5 + 8 * i)];
function xpBack() {
  const [c, g] = hires(182, 5);
  rrect(g, 0, XP_ROWS[0], 182, XP_ROWS[1] - XP_ROWS[0], 0.5);
  g.fillStyle = vgrad(g, XP_ROWS[0], XP_ROWS[1], [[0, '#6a7076'], [0.25, '#46494e'], [0.75, '#2c2f33'], [1, '#18191b']]); g.fill();
  g.fillStyle = '#050605'; g.fillRect(0.5, XP_WIN[0], 181, XP_WIN[1] - XP_WIN[0]);
  g.fillStyle = '#1e2023'; for (const x of [0, ...XP_SEPS, 181.5]) g.fillRect(x, XP_WIN[0], 0.5, XP_WIN[1] - XP_WIN[0]);
  return c;
}
/** hud/experience_bar_progress 182x5: the liquid experience in the window (drawn cropped to the fill width). */
function xpFill() {
  const [c, g] = hires(182, 5), col = ['#bfe07a', '#6aa83e', '#1f4a1f'];
  meterFill(g, 0.5, 181.5, XP_WIN[0], XP_WIN[1], col);
  g.fillStyle = meterDark(col); for (const x of [0, ...XP_SEPS, 181.5]) g.fillRect(x, XP_WIN[0], 0.5, XP_WIN[1] - XP_WIN[0]);
  return c;
}

/** A heart path within a 9 x 9 cell. */
function heartPath(g) {
  g.beginPath();
  g.moveTo(4.5, 8.1);
  g.bezierCurveTo(1.2, 5.9, 0.4, 4.1, 0.6, 2.9);
  g.bezierCurveTo(0.8, 1.1, 3.4, 0.4, 4.5, 2.4);
  g.bezierCurveTo(5.6, 0.4, 8.2, 1.1, 8.4, 2.9);
  g.bezierCurveTo(8.6, 4.1, 7.8, 5.9, 4.5, 8.1);
  g.closePath();
}
function heartFrame(g) {
  // A thin window reflection on the glass.
  g.strokeStyle = 'rgba(255,240,230,0.55)'; g.lineWidth = 0.3;
  g.beginPath(); g.moveTo(1.6, 4.2); g.quadraticCurveTo(1.4, 2.2, 3, 1.9); g.stroke();
  heartPath(g); g.strokeStyle = '#1e140a'; g.lineWidth = 0.5; g.stroke();
  rim(g, () => heartPath(g), 0.4, 8.4, P.brassHi, 0.35);
}
/** hud/heart/container 9x9: an empty vial of smoked glass in an iron frame. */
function heartContainer() {
  const [c, g] = hires(9, 9);
  heartPath(g); g.fillStyle = 'rgba(14,8,8,0.85)'; g.fill();
  heartFrame(g);
  return c;
}
/** hud/heart/full and half 9x9: the vial filled with glowing blood (half: the left side). */
function heart(half) {
  const [c, g] = hires(9, 9);
  heartPath(g); g.fillStyle = 'rgba(14,8,8,0.85)'; g.fill();
  g.save();
  if (half) { g.beginPath(); g.rect(0, 0, 4.5, 9); g.clip(); }
  heartPath(g); g.fillStyle = vgrad(g, 1, 8.2, [[0, '#d9564a'], [0.4, '#a3202a'], [1, '#3e070c']]); g.fill();
  // Light through the liquid gathers low on the far side.
  circle(g, 5.6, 5.6, 1.6); g.fillStyle = radial(g, 5.6, 5.6, 1.6, [[0, 'rgba(255,120,90,0.55)'], [1, 'rgba(255,90,70,0)']]); g.fill();
  g.restore();
  heartFrame(g);
  return c;
}

/** A roast drumstick within a 9 x 9 cell: a round head upper right, sides curving in to the bone joint. */
function meatPath(g) {
  g.beginPath(); g.moveTo(2.78, 5.38);
  g.quadraticCurveTo(3.7, 3.9, 3.86, 1.36);
  g.arc(5.7, 3.2, 2.6, Math.PI * 1.25, Math.PI * 2.25);
  g.quadraticCurveTo(5, 5.2, 3.62, 6.22); g.closePath();
}
/** The bone: a shaft and a two-knob end, filled with style. */
function bone(g, style) {
  g.strokeStyle = g.fillStyle = style; g.lineWidth = 1.1;
  g.beginPath(); g.moveTo(3.8, 5.2); g.lineTo(2, 7); g.stroke();
  circle(g, 1.25, 6.65, 0.85); g.fill(); circle(g, 2.35, 7.75, 0.85); g.fill();
}
/** hud/food_* 9x9: a roast drumstick (half: the right side, as the food row empties from the left;
 *  empty: a dark silhouette), fitted into rows 0.3 to 6.7 and centred: rows 7 to 9 belong to the exhaustion
 *  meter (appleskin.js), which nothing may overlap. */
function food(kind) {
  const [c, g] = hires(9, 9);
  const fit = () => { g.translate(1.02, -0.18); g.scale(0.8, 0.8); };   // the drawing spans x 0.4..8.3, y 0.6..8.6
  if (kind === 'empty') {
    fit();
    bone(g, 'rgba(12,10,8,0.8)');
    meatPath(g); g.fillStyle = 'rgba(12,10,8,0.8)'; g.fill(); g.strokeStyle = P.iron2; g.lineWidth = 0.35; g.stroke();
    return c;
  }
  g.save();
  if (kind === 'half') { g.beginPath(); g.rect(4.5, 0, 4.5, 9); g.clip(); }
  fit();
  bone(g, vgrad(g, 5, 8.6, [[0, '#efe4c8'], [0.5, '#c2b08c'], [1, '#6e6048']]));
  meatPath(g); g.fillStyle = radial(g, 5.7, 3.2, 3.6, [[0, '#c8783e'], [0.45, '#8e4320'], [0.85, '#4a1c0a'], [1, '#2a0e04']], 5, 2.3); g.fill();
  surface(g, () => meatPath(g), TEX.blotch, 0.3);
  // A glazed highlight along the upper side of the roast.
  g.strokeStyle = 'rgba(255,215,160,0.55)'; g.lineWidth = 0.35;
  g.beginPath(); g.arc(5.7, 3.2, 1.9, Math.PI * 1.1, Math.PI * 1.55); g.stroke();
  g.restore();
  return c;
}

/** A heater shield within a 9 x 9 cell: an arched top, straight sides, a point at the bottom. */
function shieldPath(g) {
  g.beginPath();
  g.moveTo(0.8, 1.3); g.quadraticCurveTo(4.5, 0.2, 8.2, 1.3); g.lineTo(8.2, 4);
  g.bezierCurveTo(8.2, 6.3, 6.5, 7.7, 4.5, 8.7); g.bezierCurveTo(2.5, 7.7, 0.8, 6.3, 0.8, 4); g.closePath();
}
/** hud/armor_* 9x9: a small heater shield (0.72 of the cell, low in it, near the health meter) in the action bar's
 *  polished steel: its steel gradient and near-white rim, two facets (the left lit, the right shaded). No brass.
 *  Half: the left side; empty: a dark silhouette with a thin steel edge. */
function armor(kind) {
  const [c, g] = hires(9, 9);
  const shield = () => { g.save(); g.translate(4.5, 2.4); g.scale(0.72, 0.72); g.translate(-4.5, -0.2); shieldPath(g); g.restore(); };
  const top = 2.4, bot = 8.5;
  shield(); g.fillStyle = 'rgba(12,10,8,0.6)'; g.fill();
  if (kind === 'empty') { shield(); g.strokeStyle = '#4a4f55'; g.lineWidth = 0.35; g.stroke(); return c; }
  g.save();
  if (kind === 'half') { g.beginPath(); g.rect(0, 0, 4.5, 9); g.clip(); }
  shield(); g.fillStyle = steel(g, top, bot); g.fill();
  g.save(); shield(); g.clip();
  g.fillStyle = 'rgba(255,255,255,0.3)'; g.fillRect(0, 0, 4.5, 9);    // lit facet
  g.fillStyle = 'rgba(255,255,255,0.1)'; g.fillRect(4.5, 0, 4.5, 9);   // shaded facet (still brighter than the bare gradient)
  g.restore();
  shield(); g.strokeStyle = 'rgba(10,11,12,0.7)'; g.lineWidth = 0.3; g.stroke();
  rim(g, shield, top, bot, '#eef1f4', 0.3);
  g.restore();
  return c;
}

/** hud/air 9x9: a water bubble in the shields' size and place (0.65 of the cell, low in it): a translucent ultramarine
 *  sphere (deep blue, not cyan: Iron's mana text above it is a fixed aqua), a periwinkle rim, a highlight; bright like the filled shields. Bursting: the rim broken into arcs, droplets flying. */
function air(burst) {
  const [c, g] = hires(9, 9), x = 4.5, y = 5.5, r = 2.9;
  if (burst) {
    g.strokeStyle = 'rgba(150,170,255,0.9)'; g.lineWidth = 0.5;
    for (const [a0, a1] of [[0.15, 0.55], [0.8, 1.15], [1.35, 1.7]]) { g.beginPath(); g.arc(x, y, r - 0.3, a0 * Math.PI, a1 * Math.PI); g.stroke(); }
    g.fillStyle = 'rgba(80,110,230,0.9)';
    for (const [dx, dy, dr] of [[-3.4, -2.6, 0.5], [3.3, -2.9, 0.45], [3.6, 1.6, 0.4], [-3.5, 2, 0.35]]) { circle(g, x + dx, y + dy, dr); g.fill(); }
    return c;
  }
  circle(g, x, y, r);
  g.fillStyle = radial(g, x, y, r, [[0, 'rgba(90,120,235,0.6)'], [0.7, 'rgba(45,70,195,0.82)'], [1, 'rgba(25,40,145,0.96)']], x - 0.8, y - 0.9);
  g.fill();
  circle(g, x, y, r - 0.25); g.strokeStyle = 'rgba(150,170,255,0.85)'; g.lineWidth = 0.5; g.stroke();   // bright rim
  g.strokeStyle = 'rgba(255,255,255,0.95)'; g.lineWidth = 0.55;                                        // highlight
  g.beginPath(); g.arc(x, y, r - 1, Math.PI * 1.1, Math.PI * 1.45); g.stroke();
  circle(g, x + 1.2, y + 1.1, 0.35); g.fillStyle = 'rgba(255,255,255,0.7)'; g.fill();
  return c;
}

/** Every sprite, by the keys the page uses. */
function sprites() {
  return {
    hotbar: hotbar(), selection: selection(), offhand: offhand(), xpBack: xpBack(), xpFill: xpFill(),
    heartContainer: heartContainer(), heartFull: heart(false), heartHalf: heart(true),
    foodEmpty: food('empty'), foodFull: food('full'), foodHalf: food('half'),
    armorFull: armor('full'), armorHalf: armor('half'), armorEmpty: armor('empty'), air: air(false), airBurst: air(true),
  };
}
