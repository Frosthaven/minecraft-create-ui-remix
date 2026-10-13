// Orbs (after Path of Exile, in our style) in place of the wing meters: a glass orb in a brass bezel on each wing.
// Left orb, split down the middle: health in the left half (absorption in gold above it; the effects recolour it), food
// in the right half (riding: the mount's health instead; saturation is not shown); armor takes over the brass bezel in ten segments, only while worn.
// Right orb: the primary mana pool, split with the other mana mod's pool on the right when both are installed (one: the
// whole orb);
// which is primary is a setting. Breath takes over the right orb's bezel the same way, only under water (bubbles, or a Create
// backtank in copper with its time left). Uses sprites.js, meters.js (LIQUID), hud-compact.js.
const ORB = { tex: 0.67, housing: 15.5,   // housing: the housing's radius (r + bezel + 1.4: a dome hugging the orb)
    // tex: the orbs' texture strength (a third less than first drawn)
  r: 13, y: 32.5, lx: -112, rx: 112, innerEnd: 97.6, pull: 8,   // innerEnd: the wing plates' inner ends, past the
  // housings (5 px from the centre plate); pull: how far the wings moved in from their meter-bar layout
  bezel: 1.1, band: 0.45, bow: 0.22 };   // band: a full layer's share of the width; bow: its inner edge's bulge (x r)   // band: a full crescent's share of the width   // band: a full crescent's share of the width

/** One orb at (cx, cy): fill {frac, liq, style, wave, t, sat} (sat: the saturation level, food only), over {from, to,
 *  liq} (absorption, above the fill), crescent (the right half's pool, as fill), ring {frac, on, smooth}. Drawn from
 *  pieces (the game draws the same pieces from the export, hud-compact-export.js): bezel, ring, glass, liquids, divider,
 *  glass front, glow. */
function ccOrb(g, cx, cy, { fill, over, crescent, ring }) {
  const r = ORB.r, split = crescent != null;
  ccOrbBezel(g, cx, cy);
  if (ring) ccOrbRing(g, cx, cy, ring);
  ccOrbGlassBack(g, cx, cy);
  g.save(); circle(g, cx, cy, r); g.clip();
  // With a secondary the orb splits down the middle (as Path of Exile does for life and mana): the primary fills the
  // left half, the secondary the right, each rising from the bottom on its own, a thin dark divider between them.
  ccOrbHalf(g, cx, cy, cx - r, split ? r : 2 * r, fill, over);
  if (split) { ccOrbHalf(g, cx, cy, cx, r, crescent, null); ccOrbDivider(g, cx, cy); }
  ccOrbGlassFront(g, cx, cy);
  g.restore();
  // The liquids cast a slight glow onto the stone rim and anything on it (armor plates, rings).
  if (fill && fill.frac > 0) ccOrbGlow(g, cx, cy, cx - ORB.housing, split ? ORB.housing : 2 * ORB.housing, fill.liq, ccOrbLevel(cy, fill.frac));
  if (split && crescent.frac > 0) ccOrbGlow(g, cx, cy, cx, ORB.housing, crescent.liq, ccOrbLevel(cy, crescent.frac));
}
/** The y of a liquid's surface at frac (0 empty .. 1 full). */
const ccOrbLevel = (cy, f) => cy + ORB.r - 2 * ORB.r * Math.max(0, Math.min(1, f));
/** The wave surface at x (low health or food): the liquid sloshes, the orb stays put. */
const ccOrbWave = (top, A, t, x) => top + A * Math.sin(x * 0.9 + t / 180) + A * 0.5 * Math.sin(x * 2.1 - t / 180 * 1.7);

/** The stone bezel: dark grey, a light brushed finish, its lit edge, and dark rivets at the ring's segment gaps. */
function ccOrbBezel(g, cx, cy) {
  const r = ORB.r, B = ORB.bezel, R1 = r + B;
  circle(g, cx, cy, R1); g.fillStyle = vgrad(g, cy - R1, cy + R1, [[0, '#55575b'], [0.35, '#36383c'], [1, '#1b1c1f']]); g.fill();
  surface(g, () => circle(g, cx, cy, R1), TEX.brushed, 0.08 * ORB.tex);
  rim(g, () => circle(g, cx, cy, R1), cy - R1, cy + R1, '#8e9196', 0.3);
  for (let k = 0; k < 10; k++) {
    const a = -Math.PI / 2 + k * Math.PI / 5, x = cx + Math.cos(a) * (r + B / 2), y = cy + Math.sin(a) * (r + B / 2);
    circle(g, x, y, 0.42); g.fillStyle = '#121315'; g.fill();
    circle(g, x - 0.1, y - 0.12, 0.16); g.fillStyle = 'rgba(160,165,170,0.5)'; g.fill();
  }
}
/** The ring on the bezel: armor as ten bolted plates (only the full ones; a half one for an odd value), or one smooth
 *  arc (breath, the backtank, Helium Flamingo, aircraft). half: every plate drawn half (the export's partial set). */
function ccOrbRing(g, cx, cy, ring) {
  const r = ORB.r, R1 = r + ORB.bezel, n = ring.smooth ? 1 : 10, gap = ring.smooth ? 0 : 0.07;
  for (let k = 0; k < n; k++) {
    const lit = ring.half ? 0.5 : ring.frac * n - k;   // >= 1 full, 0..1 partial
    if (lit <= 0) break;
    const a0 = -Math.PI / 2 + (k / n) * Math.PI * 2 + gap, a1 = -Math.PI / 2 + ((k + 1) / n) * Math.PI * 2 - gap;
    const a2 = a0 + (a1 - a0) * Math.min(1, lit);
    // Segmented (armor): thick iron plates reaching from the rim across the seat into the housing; smooth: on the rim.
    const p0 = r - 0.35, p1 = ring.smooth ? R1 + 0.45 : ORB.housing - 0.35;
    const seg = () => { g.beginPath(); g.arc(cx, cy, p1, a0, a2); g.arc(cx, cy, p0, a2, a0, true); g.closePath(); };
    if (!ring.smooth) { g.save(); g.translate(0.35, 0.55); seg(); g.fillStyle = 'rgba(0,0,0,0.55)'; g.fill(); g.restore(); }
    g.save(); g.shadowColor = ring.on[1]; g.shadowBlur = 2.2;
    if (ring.smooth) { seg(); g.fillStyle = vgrad(g, cy - R1, cy + R1, [[0, ring.on[0]], [0.45, ring.on[1]], [1, ring.on[2]]]); g.fill(); }
    else {   // a curved strap: a crest just outside the rim, bending back down onto the housing (shaded across its width)
      const q = t => (p0 + (p1 - p0) * t) / p1;
      seg(); g.fillStyle = radial(g, cx, cy, p1, [[q(0), ring.on[1]], [q(0.3), ring.on[0]], [q(0.55), ring.on[1]], [q(1), ring.on[2]]]); g.fill();
      g.globalCompositeOperation = 'multiply';   // keep the top-lit feel: darker toward the bottom of the orb
      seg(); g.fillStyle = vgrad(g, cy - p1, cy + p1, [[0, '#ffffff'], [1, '#8a8e94']]); g.fill();
      g.globalCompositeOperation = 'source-over';
    }
    g.restore();
    surface(g, seg, TEX.brushed, 0.15 * ORB.tex);
    seg(); g.strokeStyle = 'rgba(0,0,0,0.85)'; g.lineWidth = 0.3; g.stroke();
    const crest = ring.smooth ? p1 - 0.25 : p0 + (p1 - p0) * 0.3;   // the highlight along the crest
    g.beginPath(); g.arc(cx, cy, crest, a0 + 0.02, a2 - 0.02); g.strokeStyle = 'rgba(255,255,255,0.55)'; g.lineWidth = 0.2; g.stroke();
    if (!ring.smooth && lit >= 1) {   // two hex bolts holding the plate to the housing
      const rb = (R1 + 0.6 + p1) / 2;
      for (const t of [0.25, 0.75]) { const a = a0 + (a1 - a0) * t; hexBolt(g, cx + Math.cos(a) * rb, cy + Math.sin(a) * rb, 0.42); }
    }
  }
}
/** The empty glass: smoky dark. */
function ccOrbGlassBack(g, cx, cy) {
  const r = ORB.r;
  circle(g, cx, cy, r); g.fillStyle = radial(g, cx, cy - r * 0.3, r * 1.2, [[0, '#1a1c22'], [1, '#050608']]); g.fill();
  surface(g, () => circle(g, cx, cy, r), TEX.rock, 0.12 * ORB.tex);
}
/** The glass over the liquids: a soft dark rim and a highlight top-left. */
function ccOrbGlassFront(g, cx, cy) {
  const r = ORB.r;
  circle(g, cx, cy, r); g.fillStyle = radial(g, cx, cy, r, [[0.7, 'rgba(0,0,0,0)'], [1, 'rgba(0,0,0,0.55)']]); g.fill();
  g.beginPath(); g.ellipse(cx - r * 0.32, cy - r * 0.45, r * 0.42, r * 0.24, -0.5, 0, Math.PI * 2);
  g.fillStyle = radial(g, cx - r * 0.32, cy - r * 0.45, r * 0.42, [[0, 'rgba(255,255,255,0.45)'], [1, 'rgba(255,255,255,0)']]); g.fill();
}
/** The thin dark divider between the halves. */
function ccOrbDivider(g, cx, cy) {
  const r = ORB.r;
  g.fillStyle = vgrad(g, cy - r, cy + r, [[0, '#5a5c60'], [1, '#1c1d20']]); g.fillRect(cx - 0.35, cy - r, 0.7, 2 * r);
}
/** A liquid over the whole disc (the caller clips it to its half and level): its gradient, its style ('mana' a galaxy:
 *  nebula clouds in its colour and fixed stars; 'cloud' billows; 'broth' the same), and depth (darker toward the
 *  bottom and the sides, a little light from within). */
function ccOrbLiquid(g, cx, cy, liq, style) {
  const r = ORB.r, area = () => { g.beginPath(); g.rect(cx - r, cy - r, 2 * r, 2 * r); };
  g.save(); area(); g.clip();
  g.fillStyle = vgrad(g, cy - r, cy + r, [[0, liq[0]], [0.4, liq[1]], [1, liq[2]]]); area(); g.fill();
  if (style === 'mana') {
    g.globalCompositeOperation = 'multiply';
    const pat = g.createPattern(ccNebula(), 'repeat');
    for (const k of [0.07, 0.16]) {
      pat.setTransform(new DOMMatrix().translateSelf(cx * 3, cy * 5).rotateSelf(k * 900).scaleSelf(k, k));
      g.fillStyle = pat; area(); g.fill();
    }
    g.globalCompositeOperation = 'screen';
    for (let i = 0; i < 26; i++) {   // stars: fixed points (seeded by the orb's position), a few brighter
      const h = Math.sin((cx + i * 12.9898) * 78.233) * 43758.5453, u = h - Math.floor(h), v = (h * 7.13) - Math.floor(h * 7.13);
      const x = cx - r + u * 2 * r, y = cy - r + v * 2 * r, big = i % 7 === 0;
      g.fillStyle = big ? 'rgba(255,255,255,0.95)' : 'rgba(220,230,255,0.7)';
      g.fillRect(x, y, big ? 0.45 : 0.25, big ? 0.45 : 0.25);
    }
    g.globalCompositeOperation = 'source-over';
  } else if (style === 'cloud' || style === 'broth') {   // billows: lighter clouds rising, darker ones settling
    g.save(); g.translate(cx, cy); g.scale(0.55, 0.55); g.translate(-cx, -cy);
    surface(g, () => { g.beginPath(); g.rect(cx - 3 * r, cy - 3 * r, 6 * r, 6 * r); }, TEX.blotch, 0.55 * ORB.tex, 'soft-light');
    g.restore();
    surface(g, area, TEX.blotch, 0.45 * ORB.tex, 'overlay');
    surface(g, area, TEX.rock, 0.25 * ORB.tex, 'soft-light');
  } else { surface(g, area, TEX.blotch, 0.35 * ORB.tex); surface(g, area, TEX.brushed, 0.12 * ORB.tex, 'soft-light'); }
  g.fillStyle = vgrad(g, cy - r, cy + r, [[0, 'rgba(0,0,0,0)'], [1, 'rgba(0,0,0,0.35)']]); area(); g.fill();
  g.fillStyle = radial(g, cx, cy + r * 0.1, r, [[0, 'rgba(255,255,255,0.08)'], [0.6, 'rgba(0,0,0,0)'], [1, 'rgba(0,0,0,0.3)']]); area(); g.fill();
  g.restore();
}
/** Saturation over the broth: paler and creamier, its texture showing straight through (the caller clips it). */
function ccOrbSatTint(g, cx, cy) {
  const r = ORB.r;
  g.save();
  g.globalCompositeOperation = 'soft-light'; g.fillStyle = 'rgba(255,240,210,0.35)'; g.fillRect(cx - r, cy - r, 2 * r, 2 * r);
  g.globalCompositeOperation = 'screen'; g.fillStyle = 'rgba(255,225,180,0.06)'; g.fillRect(cx - r, cy - r, 2 * r, 2 * r);
  g.restore();
}
/** The broth's rising bubbles, seeded by the orb's position, spread over the liquid from y0 to y1. */
function ccOrbBubbles(cx, cy, y0, y1) {
  const r = ORB.r, out = [];
  for (let i = 0; i < 7; i++) {
    const h = Math.sin((cx + i * 9.17) * 33.1) * 43758.5453, u = h - Math.floor(h), v = h * 5.3 - Math.floor(h * 5.3);
    out.push([cx - r + u * 2 * r, y0 + 1 + v * (y1 - y0 - 2), 0.35 + (i % 3) * 0.15]);
  }
  return out;
}
/** Absorption's sparkles in its band (x0..x0 + w, ya..yb). */
function ccOrbSparkles(cx, x0, w, ya, yb) {
  const out = [];
  for (let i = 0; i < 9; i++) {
    const h = Math.sin((cx + i * 7.31) * 51.7) * 43758.5453, u = h - Math.floor(h), v = h * 3.7 - Math.floor(h * 3.7);
    out.push([x0 + u * w, ya + v * (yb - ya), i % 3 ? 0.3 : 0.45, i % 3 ? 'rgba(255,250,210,0.8)' : 'rgba(255,255,255,0.95)']);
  }
  return out;
}
/** One half (x0, width w) of an orb, clipped to the glass by the caller: the liquid to its level (a rolling wave at low
 *  health or food), saturation's tint, bubbles, absorption above it, the surface catching the light. */
function ccOrbHalf(g, cx, cy, x0, w, f, over) {
  if (!f || f.frac <= 0) return;
  const r = ORB.r, top = ccOrbLevel(cy, f.frac);
  g.save(); g.beginPath(); g.rect(x0, cy - r, w, 2 * r); g.clip();
  const surf = x => f.wave ? ccOrbWave(top, f.wave, f.t || 0, x) : top;
  g.save(); g.beginPath(); g.moveTo(x0, cy + r);
  for (let x = x0; x <= x0 + w + 0.01; x += 0.5) g.lineTo(x, surf(x));
  g.lineTo(x0 + w, cy + r); g.closePath(); g.clip();
  ccOrbLiquid(g, cx, cy, f.liq, f.style);
  if (f.sat > 0) {   // saturation: the broth below its level (never past the food) tinted a lighter cream
    const ys = ccOrbLevel(cy, f.sat);
    g.save(); g.beginPath(); g.rect(x0, ys, w, cy + r - ys); g.clip(); ccOrbSatTint(g, cx, cy); g.restore();
  }
  if (f.style === 'broth') for (const [x, y, br] of ccOrbBubbles(cx, cy, top, cy + r)) {
    circle(g, x, y, br); g.strokeStyle = 'rgba(255,230,190,0.55)'; g.lineWidth = 0.15; g.stroke();
  }
  g.restore();
  const absorbing = over && over.to > over.from;
  if (absorbing) {   // absorption: golden cloud with a few sparkles
    const ya = ccOrbLevel(cy, over.to), yb = ccOrbLevel(cy, over.from);
    g.save(); g.beginPath(); g.rect(x0, ya, w, yb - ya); g.clip(); ccOrbLiquid(g, cx, cy, over.liq, 'cloud'); g.restore();
    for (const [x, y, sz, col] of ccOrbSparkles(cx, x0, w, ya, yb)) { g.fillStyle = col; g.fillRect(x, y, sz, sz); }
    g.fillStyle = 'rgba(255,255,255,0.35)'; g.fillRect(x0, ya, w, 0.4);
  } else if (f.wave) {   // the moving surface catching the light
    g.beginPath(); for (let x = x0; x <= x0 + w + 0.01; x += 0.5) x === x0 ? g.moveTo(x, surf(x)) : g.lineTo(x, surf(x));
    g.strokeStyle = 'rgba(255,255,255,0.35)'; g.lineWidth = 0.4; g.stroke();
  } else { g.fillStyle = 'rgba(255,255,255,0.35)'; g.fillRect(x0, top, w, 0.4); }
  g.restore();
}
/** The liquid's glow on the rim's inner edge (x0..x0 + w, below the level less 1.5): its colour, equal strength for
 *  every colour (dim ones get more alpha, by luminance), fading out within about a pixel; 'screen' over the rim. */
function ccOrbGlow(g, cx, cy, x0, w, liq, levelY) {
  const r = ORB.r;
  g.save();
  g.beginPath(); g.arc(cx, cy, ORB.housing, 0, Math.PI * 2); g.arc(cx, cy, r - 0.2, 0, Math.PI * 2, true); g.clip('evenodd');
  g.beginPath(); g.rect(x0, levelY - 1.5, w, cy + r + 4 - levelY); g.clip();
  g.globalCompositeOperation = 'screen';
  const [cr, cg, cb] = [1, 3, 5].map(i => parseInt(liq[1].slice(i, i + 2), 16) / 255);
  const lum = 0.3 * cr + 0.59 * cg + 0.11 * cb, a = Math.max(0.2, Math.min(0.75, 0.16 / Math.max(lum, 0.05)));
  const hex = Math.round(a * 255).toString(16).padStart(2, '0');
  g.fillStyle = radial(g, cx, cy, r + 1.4, [[(r - 0.2) / (r + 1.4), liq[1] + hex], [1, liq[1] + '00']]);
  g.fillRect(cx - ORB.housing, cy - ORB.housing, 2 * ORB.housing, 2 * ORB.housing);
  g.restore();
}

/** Exhaustion (0 to 4; a food point or saturation is taken at 4) as hairline cracks in the food half's glass: three
 *  cracks from the rim, a segment more of each per stage (ceil of the exhaustion), thin and low contrast. */
const CC_CRACKS = [   // each: an angle on the rim (from the right, clockwise) and its inward steps [dx, dy]
  [-0.75, [[-1.3, 0.9], [-1.1, 0.4], [-1.2, 1.1], [-0.9, 0.5]]],
  [0.1, [[-1.5, -0.3], [-1.0, 0.6], [-1.3, -0.2], [-1.1, 0.5]]],
  [0.9, [[-1.0, -1.1], [-1.2, -0.5], [-0.8, -1.0], [-1.2, -0.4]]],
];
function ccOrbCrackPoints(cx, cy, k, upto) {
  const [a, steps] = CC_CRACKS[k], r = ORB.r - 0.1;
  const pts = [[cx + Math.cos(a) * r, cy + Math.sin(a) * r]];
  for (let i = 0; i < upto; i++) { const [px, py] = pts[pts.length - 1]; pts.push([px + steps[i][0] * 1.3, py + steps[i][1] * 1.3]); }
  return pts;
}
function ccOrbCracks(g, cx, cy, stage) {
  if (stage <= 0) return;
  g.save(); g.beginPath(); g.rect(cx, cy - ORB.r, ORB.r, 2 * ORB.r); g.clip(); circle(g, cx, cy, ORB.r); g.clip();
  for (let k = 0; k < CC_CRACKS.length; k++) {
    const pts = ccOrbCrackPoints(cx, cy, k, stage);
    for (const [dy, col, lw] of [[0.15, 'rgba(0,0,0,0.35)', 0.22], [0, 'rgba(225,235,245,0.32)', 0.16]]) {
      g.beginPath(); pts.forEach(([x, y], i) => i ? g.lineTo(x, y + dy) : g.moveTo(x, y + dy));
      g.strokeStyle = col; g.lineWidth = lw; g.stroke();
    }
  }
  g.restore();
}
/** When exhaustion wraps (a point taken), glass flakes fall from the cracks' ends and fade (since: ms since). */
function ccOrbFlakes(cx, cy, since) {
  if (since < 0 || since > 900) return [];
  const k = since / 900, out = [];
  for (let c = 0; c < CC_CRACKS.length; c++) {
    const pts = ccOrbCrackPoints(cx, cy, c, 4);
    for (const j of [2, 4]) {
      const [x, y] = pts[j];
      out.push([x + (c - 1) * 0.6 * k, y + 9 * k * k, `rgba(225,235,245,${0.6 * (1 - k)})`]);
    }
  }
  return out;
}

/** The health orb's status effects (the meters had them; same rules): the damage flash, Regeneration's rising light,
 *  Farmer's Delight Comfort (saturation 0, hurt, no Regeneration) as a slow warm sheen, (hardcore: ccHardcore, a skull
 *  the bezel), and AppleSkin's previews of what held food restores: pale pulsing bands above the health and food
 *  levels in their halves. */
function ccOrbFx(g, s, cx, cy, hp, max, riding) {
  const r = ORB.r, level = f => cy + r - 2 * r * Math.max(0, Math.min(1, f));
  g.save(); circle(g, cx, cy, r); g.clip();
  const leftHalf = () => { g.beginPath(); g.rect(cx - r, cy - r, r, 2 * r); };
  const warn = 0.12 + 0.12 * Math.sin(s.t / 220);   // one pulse for both warnings, so they flash in sync
  if (!riding && s.food <= 6) {   // low food: the empty glass above the broth pulses a faint amber
    const a = warn;
    g.fillStyle = `rgba(220,120,20,${a})`; g.fillRect(cx, cy - r, r, level(s.food / 20) - (cy - r));
  }
  if (s.health <= 4) {   // low health: the empty glass above the liquid pulses a faint red
    const a = warn;
    g.fillStyle = `rgba(200,20,30,${a})`; g.fillRect(cx - r, cy - r, r, level(hp) - (cy - r));
  }
  const since = s.t - (s.hurtAt || -1e9);
  if (since < 400) { g.fillStyle = `rgba(255,40,40,${0.55 * (1 - since / 400)})`; leftHalf(); g.fill(); }   // damage: a red flash
  if (s.regen && hp > 0) {   // Regeneration: small bright motes rising through the health liquid, fading at its surface
    const top = level(hp), depth = cy + r - top;
    for (let i = 0; i < 10; i++) {
      const h = Math.sin((i + 1) * 91.7) * 43758.5453, u = h - Math.floor(h), sp = 0.6 + (h * 3.1 - Math.floor(h * 3.1)) * 0.6;
      const k = ((s.t / 1600) * sp + u * 3.7) % 1, y = cy + r - k * depth, x = cx - r + 1 + u * (r - 2) + Math.sin(s.t / 300 + i) * 0.4;
      const a = Math.sin(Math.PI * k) * 0.9;
      circle(g, x, y, 0.35 + (i % 3) * 0.12); g.fillStyle = `rgba(255,120,175,${a})`; g.fill();   // pink
    }
  }
  if (!riding && s.nourished && s.food > 0) {   // Nourishment: the broth keeps its level, glazed gold with slow sparkles
    const top = level(s.food / 20);
    g.save(); g.beginPath(); g.rect(cx, top, r, cy + r - top); g.clip();
    g.fillStyle = vgrad(g, top, cy + r, [[0, 'rgba(255,215,90,0.28)'], [1, 'rgba(255,200,60,0.1)']]); g.fillRect(cx, top, r, cy + r - top);
    for (let i = 0; i < 8; i++) {
      const h = Math.sin((i + 3) * 47.3) * 43758.5453, u = h - Math.floor(h), v = h * 2.9 - Math.floor(h * 2.9);
      const a = 0.45 + 0.45 * Math.sin(s.t / 260 + i * 1.7);
      g.fillStyle = `rgba(255,240,160,${a})`; g.fillRect(cx + 0.6 + u * (r - 1.2), top + 0.8 + v * (cy + r - top - 1.6), 0.4, 0.4);
    }
    g.restore();
    g.fillStyle = 'rgba(255,220,110,0.95)'; g.fillRect(cx, top - 0.15, r, 0.5);   // a gold surface line
  }
  if (s.comfort && s.saturation <= 0 && hp < 1) {   // shows alongside Regeneration's motes   // Comfort: a slow pink glow rising through the health half's whole glass
    const y = cy + r + 4 - ((s.t / 2600) % 1) * (2 * r + 8);
    g.fillStyle = vgrad(g, y - 5, y + 5, [[0, '#ff9ec400'], [0.5, '#ff9ec488'], [1, '#ff9ec400']]); g.fillRect(cx - r, y - 5, r, 10);
  }
  const pulse = (0.3 + 0.25 * Math.sin(s.t / 160)) / 2;   // half the bars' preview: the orb's large area glared
  if (s.holdFood) {
    if (s.holdHeal) { g.fillStyle = `rgba(255,200,200,${pulse})`; g.fillRect(cx - r, level(hp + s.holdHeal / max), r, level(hp) - level(hp + s.holdHeal / max)); }
    if (!riding) { const a = s.food / 20, b = Math.min(20, s.food + s.holdFood) / 20; g.fillStyle = `rgba(255,230,180,${pulse})`; g.fillRect(cx, level(b), r, level(a) - level(b)); }
  }
  if (!riding && s.exhaustion != null) {   // exhaustion: cracks in the food half's glass; flakes fall when it wraps
    ccOrbCracks(g, cx, cy, Math.min(4, Math.ceil(s.exhaustion)));
    for (const [x, y, col] of ccOrbFlakes(cx, cy, s.t - (s.flakeAt ?? -1e9))) { g.fillStyle = col; g.fillRect(x, y, 0.45, 0.45); }
  }
  g.restore();
}

/** The nebula mask: white in the clouds, falling to deep space (dark blue-black) between them; multiplied over a
 *  liquid it keeps most of the colour in the clouds (the dark glass showing a little through) and darkens the rest.
 *  Cached. */
let NEBULA = null;
function ccNebula() {
  if (NEBULA) return NEBULA;
  const t = TEX.blotch, c = document.createElement('canvas');
  c.width = t.width; c.height = t.height;
  const g = c.getContext('2d');
  g.drawImage(t, 0, 0);
  const d = g.getImageData(0, 0, c.width, c.height), px = d.data, deep = [78, 72, 118];   // deep space, not quite black
  for (let i = 0; i < px.length; i += 4) {
    const v = Math.min(1, Math.max(0, (px[i] / 255 - 0.3) / 0.45));   // 0 deep space .. 1 cloud
    for (let k = 0; k < 3; k++) px[i + k] = deep[k] + (222 - deep[k]) * v;   // the brightest clouds a little see-through
    px[i + 3] = 255;
  }
  g.putImageData(d, 0, 0);
  return (NEBULA = c);
}

/** The orb's housing, part of its wing plate: the plate rises into a round iron boss around the orb (the plate's own
 *  iron and texture, so they read as one casting), its brass rim continuing over the top where it rises above the
 *  plate, and a dark seat line where the orb sits recessed. Drawn after the plate, before the orb. */
function ccHousing(g, cx, cy, inner) {   // inner: +1 when the hotbar is to the right, -1 to the left
  const R = ORB.housing, wing = CC.plate.wing, b = BOTTOM, f = 3;   // f: the fillets' radius
  // The boss's outline above the plate: the plate's top edge, a concave fillet into the circle, over the top, a fillet
  // back down to the edge, so the outline flows from the straight edge into the curve without a corner.
  const fy = wing - f, dy = fy - cy, dx = Math.sqrt((R + f) ** 2 - dy ** 2);
  const tL = Math.atan2(dy, -dx), tR = Math.atan2(dy, dx);              // where the fillets touch the circle
  // Toward the hotbar there is no fillet: the plate ends at the housing's edge (ORB.innerEnd), so the curve runs down
  // to the circle's side and the plate's straight end continues below it. Drawn for inner = +1, mirrored for -1.
  const outline = (close) => {
    g.save(); g.translate(cx, 0); g.scale(inner, 1); g.translate(-cx, 0);
    g.beginPath(); g.moveTo(cx - dx - 0.01, wing);
    g.arc(cx - dx, fy, f, Math.PI / 2, Math.atan2(cy - fy, dx), true);
    g.arc(cx, cy, R, tL, 0, false);
    g.lineTo(cx + R, close ? b : cy);
    if (close) { g.lineTo(cx - dx, b); g.closePath(); }
    g.restore();
  };
  // The plate's own gradient (shared, from the dome's peak), plus a soft light on the dome's crown so it reads round.
  outline(true); g.fillStyle = iron(g, cy - R, b); g.fill(); ironSurface(g, () => outline(true));
  g.save(); outline(true); g.clip();
  circle(g, cx, cy - R * 0.55, R * 0.75); g.fillStyle = radial(g, cx, cy - R * 0.7, R * 0.75, [[0, 'rgba(255,240,220,0.10)'], [1, 'rgba(255,240,220,0)']]); g.fill();
  g.restore();
  // The rim along the smooth outline only (not the closing sides inside the plate).
  outline(false); g.strokeStyle = 'rgba(0,0,0,0.6)'; g.lineWidth = 0.6; g.stroke();   // an even dark edge (no shifted shadow: it read as a flat top)
  rim(g, () => outline(false), cy - R, cy + R, P.brassHi, 0.35);
  // The seat: a recessed groove the orb's bezel drops into.
  circle(g, cx, cy, ORB.r + ORB.bezel + 0.4); g.strokeStyle = 'rgba(0,0,0,0.75)'; g.lineWidth = 0.7; g.stroke();
  g.save(); g.translate(0, 0.45); circle(g, cx, cy, ORB.r + ORB.bezel + 0.8); g.strokeStyle = 'rgba(220,225,230,0.12)'; g.lineWidth = 0.3; g.stroke(); g.restore();
}

/** The dodge roll dial at (cx, cy): a stone-rimmed glass with the dash chevrons in the tier colour, and around the rim a
 *  segment per charge, each lit (glowing, within the rim) once that charge is ready (partial while recharging).
 *  ready: charges ready (fractional), n: the charge count. Drawn as its base (by tier) and ring (by tier and n). */
function ccDashDial(g, cx, cy, ready, n) {
  const tier = rollTier(ready, n);
  ccDashBase(g, cx, cy, tier);
  ccDashRing(g, cx, cy, tier, n, ready);
}
const ccDashColour = tier => tier === 'grey' ? '#5a5e64' : ROLL_TIERS[tier];
const CC_DASH = { r: 6.2, B: 1.1, gap: 0.12 };
/** The dial without its charge ring: recess, stone rim, glass, the chevrons glowing in the tier colour. */
function ccDashBase(g, cx, cy, tier) {
  const { r, B } = CC_DASH, R1 = r + B, col = ccDashColour(tier);
  circle(g, cx, cy, R1 + 0.25); g.strokeStyle = 'rgba(0,0,0,0.7)'; g.lineWidth = 0.4; g.stroke();   // the recess, thin
  circle(g, cx, cy, R1); g.fillStyle = vgrad(g, cy - R1, cy + R1, [[0, '#55575b'], [0.35, '#36383c'], [1, '#1b1c1f']]); g.fill();
  rim(g, () => circle(g, cx, cy, R1), cy - R1, cy + R1, '#8e9196', 0.3);
  circle(g, cx, cy, r); g.fillStyle = radial(g, cx, cy - r * 0.3, r * 1.2, [[0, '#1a1c22'], [1, '#050608']]); g.fill();
  g.save(); circle(g, cx, cy, r); g.clip();
  circle(g, cx, cy, r); g.fillStyle = radial(g, cx, cy, r, [[0, col + '55'], [1, col + '00']]); g.fill();
  const chev = x0 => { g.beginPath(); g.moveTo(x0, cy - 2.6); g.lineTo(x0 + 1.7, cy - 2.6); g.lineTo(x0 + 3.7, cy); g.lineTo(x0 + 1.7, cy + 2.6); g.lineTo(x0, cy + 2.6); g.lineTo(x0 + 2, cy); g.closePath(); };
  // The pair spans cx - 3.2 .. cx + 3.2, centred.
  for (const [x0, a] of [[cx - 3.2, 0.55], [cx - 0.5, 1]]) { chev(x0); g.fillStyle = mixHex(col, '#000000', a < 1 ? 0.4 : 0); g.fill(); }
  g.beginPath(); g.ellipse(cx - r * 0.3, cy - r * 0.45, r * 0.42, r * 0.24, -0.5, 0, Math.PI * 2);
  g.fillStyle = radial(g, cx - r * 0.3, cy - r * 0.45, r * 0.42, [[0, 'rgba(255,255,255,0.4)'], [1, 'rgba(255,255,255,0)']]); g.fill();
  g.restore();
}
/** The segment angles of charge k of n: [start, end]. */
function ccDashSeg(k, n) {
  const a0 = -Math.PI / 2 + (k / n) * Math.PI * 2 + CC_DASH.gap, a1 = -Math.PI / 2 + ((k + 1) / n) * Math.PI * 2 - CC_DASH.gap;
  return [a0, a1];
}
/** The charge ring: a segment per charge on the rim, lit up to ready. */
function ccDashRing(g, cx, cy, tier, n, ready) {
  const { r, B } = CC_DASH, R1 = r + B, col = ccDashColour(tier);
  for (let k = 0; k < n; k++) {
    const lit = ready - k;
    if (lit <= 0) break;
    const [a0, a1] = ccDashSeg(k, n), a2 = a0 + (a1 - a0) * Math.min(1, lit), p0 = r, p1 = R1;
    const seg = () => { g.beginPath(); g.arc(cx, cy, p1, a0, a2); g.arc(cx, cy, p0, a2, a0, true); g.closePath(); };
    g.save(); g.shadowColor = col; g.shadowBlur = 1.6; seg(); g.fillStyle = vgrad(g, cy - R1, cy + R1, [[0, mixHex(col, '#ffffff', 0.4)], [1, mixHex(col, '#000000', 0.3)]]); g.fill(); g.restore();
    seg(); g.strokeStyle = 'rgba(0,0,0,0.8)'; g.lineWidth = 0.25; g.stroke();
  }
}

/** An empty housing (creative's vitals, no mana mod): a cog pressed into its face. */
function ccHousingCog(g, cx, cy) {
  ccEngrave(g, () => cogPath(g, cx, cy, ORB.r * 0.9, ORB.r * 0.7, 12), 0.6);
  ccEngrave(g, () => circle(g, cx, cy, ORB.r * 0.3), 0.6);
}

/** Value / max centred on an orb (two lines when a secondary shows), the bar's small digits. */
function ccOrbNumbers(g, cx, cy, lines) {
  const k = 0.6;
  lines.forEach(([v, max, col], i) => {
    const t = `${v}/${max}`, y = cy - (lines.length * 4) / 2 + i * 4.2;
    ccNumber(g, t, cx - ccNumberWidth(t, k) / 2, y, ccTint(col), 'rgba(0,0,0,0.9)', k);
  });
}

/** Both orbs for a state (in place of the wing meters and the armor / breath tabs). */
function ccOrbs(g, s) {
  const showNums = s.numbers || s.hover === 'orbL' || s.hover === 'orbR';
  ccHousing(g, ORB.lx, ORB.y, 1); ccHousing(g, ORB.rx, ORB.y, -1);
  // Left: health, food (or the mount), armor.
  if (!s.creative) {
    const max = s.maxHealth || 20, hp = s.health / max, liq = ccHealthLiquid(s);
    const riding = !!s.riding;
    const food = riding ? { frac: s.mount / s.mountMax, liq: CC_MOUNT, style: 'cloud' }
      : { frac: s.food / 20, liq: s.hunger ? LIQUID.hunger : LIQUID.broth, style: 'broth',
          wave: s.food <= 6 ? 0.35 : 0, t: s.t, sat: s.food > 0 ? Math.min(s.saturation, s.food) / 20 : 0 };   // low food (no sprinting): the broth sloshes too
    const low = s.health <= 4;   // vanilla shakes the hearts here; the orb's liquid sloshes instead
    ccOrb(g, ORB.lx, ORB.y, { fill: { frac: hp, liq, style: 'cloud', wave: low ? 0.35 : 0, t: s.t }, over: s.absorb ? { from: hp, to: hp + s.absorb / max, liq: LIQUID.absorb } : null,
      crescent: food, ring: s.armor > 0 ? { frac: Math.min(1, s.armor / 20), on: ['#eef1f4', '#9aa0a6', '#3a3d41'] } : null,
    });
    ccOrbFx(g, s, ORB.lx, ORB.y, hp, max, riding);
    CC.hit.orbL = [ORB.lx - ORB.r, ORB.y - ORB.r, 2 * ORB.r, 2 * ORB.r];
    if (showNums) ccOrbNumbers(g, ORB.lx, ORB.y, [[Math.round(s.health), max, liq[1]], riding ? [s.mount, s.mountMax, CC_MOUNT[1]] : [s.food, 20, LIQUID.broth[1]]]);
  } else ccHousingCog(g, ORB.lx, ORB.y);   // creative: a cog pressed into the housing's face
  // Right: the mana pools (primary the liquid, the other the crescent), breath as the ring under water.
  const pools = [['irons', s.irons, MANA.irons, 200], ['ars', s.ars, MANA.ars, 150]].filter(p => p[1] != null);
  if (s.manaPrimary === 'ars') pools.reverse();
  const tank = s.diving ? Math.max(0, s.tank) : 0, breath = !s.creative && (s.underwater || s.air < 1);
  // The right ring shows one thing: breath (or the backtank), else the Helium Flamingo's charge, else an aircraft's health.
  const ring = breath ? (tank > 0 ? { frac: tank / s.tankMax, on: ['#ffd9b0', '#c98a5a', '#5a2814'], smooth: true } : { frac: s.air, on: LIQUID.air, smooth: true })
    : s.flamingo != null ? { frac: s.flamingo, on: CC_FLAMINGO, smooth: true }
    : s.aircraft ? { frac: s.hull, on: CC_HULL, smooth: true } : null;
  if (pools.length) {
    ccOrb(g, ORB.rx, ORB.y, { fill: { frac: pools[0][1], liq: pools[0][2], style: 'mana' }, crescent: pools[1] ? { frac: pools[1][1], liq: pools[1][2], style: 'mana' } : null, ring });
    CC.hit.orbR = [ORB.rx - ORB.r, ORB.y - ORB.r, 2 * ORB.r, 2 * ORB.r];
    if (showNums) ccOrbNumbers(g, ORB.rx, ORB.y, pools.map(p => [Math.round(p[1] * p[3]), p[3], p[2][1]]));
  } else if (ring) ccOrb(g, ORB.rx, ORB.y, { ring });   // no mana mod: an empty orb still carries the ring
  else ccHousingCog(g, ORB.rx, ORB.y);                 // no mana mod: a cog pressed into the housing's face
  // The backtank's time, always on (players rely on it), just above the right orb's housing.
  if (breath && tank > 0) {
    const t = `${Math.floor(Math.ceil(tank) / 60)}:${String(Math.ceil(tank) % 60).padStart(2, '0')}`;
    ccNumber(g, t, ORB.rx - ccNumberWidth(t, 0.6) / 2, ORB.y - ORB.housing - 4.2, ccTint('#d9874a'), 'rgba(0,0,0,0.95)', 0.6);   // above the housing's dome
  }
}
