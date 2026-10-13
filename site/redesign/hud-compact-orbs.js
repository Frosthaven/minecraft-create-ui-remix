// Orbs (after Path of Exile, in our style) in place of the wing meters: a glass orb in a brass bezel on each wing.
// Left orb: health rises as the liquid (absorption in gold above it; the effects recolour it); food is the orb's
// secondary layer, a crescent sweeping in from the right as PoE's energy shield does (riding: the mount's health
// instead; saturation is not shown); armor takes over the brass bezel in ten segments, only while worn.
// Right orb: the primary mana pool as the liquid, the other mana mod's pool (when both are installed) the crescent;
// which is primary is a setting. Breath takes over the right orb's bezel the same way, only under water (bubbles, or a Create
// backtank in copper with its time left). Uses sprites.js, meters.js (LIQUID), hud-compact.js.
const ORB = { tex: 0.67,   // tex: the orbs' texture strength (a third less than first drawn)
  r: 13, y: 32.5, lx: -112, rx: 112, innerEnd: 96.6, pull: 8,   // innerEnd: the wing plates' inner ends, past the
  // housings (5 px from the centre plate); pull: how far the wings moved in from their meter-bar layout
  bezel: 1.1, band: 0.45, bow: 0.22 };   // band: a full layer's share of the width; bow: its inner edge's bulge (x r)   // band: a full crescent's share of the width   // band: a full crescent's share of the width

/** One orb at (cx, cy): fill {frac, liq}, over {from, to, liq} (absorption, above the fill), crescent {frac, liq,
 *  light: {frac, liq}} (the secondary layer), ring {frac, on, off} (segments around the bezel). */
function ccOrb(g, cx, cy, { fill, over, crescent, ring, shake = 0 }) {
  const r = ORB.r;
  cy += shake;
  // The brass bezel; a ring (armor, breath) takes it over where its segments are lit: ten segments clockwise from the
  // top, the unlit ones staying plain brass.
  const B = ORB.bezel, R1 = r + B;
  circle(g, cx, cy, R1); g.fillStyle = brass(g, cy - R1, cy + R1); g.fill();
  surface(g, () => circle(g, cx, cy, R1), TEX.brushed, 0.08 * ORB.tex);   // a light brushed finish
  if (ring) {
    // Only the full segments show (bright plates proud of the rim); empty ones leave the plain rim.
    const n = 10, gap = 0.07;
    for (let k = 0; k < n; k++) {
      const lit = ring.frac * n - k;   // >= 1 full, 0..1 partial
      if (lit <= 0) break;
      const a0 = -Math.PI / 2 + (k / n) * Math.PI * 2 + gap, a1 = -Math.PI / 2 + ((k + 1) / n) * Math.PI * 2 - gap;
      const a2 = a0 + (a1 - a0) * Math.min(1, lit);
      // A lit segment stands proud of the rim (wider than the bezel, dark outline) and glows in its colour.
      const p0 = r - 0.35, p1 = R1 + 0.45;
      const seg = () => { g.beginPath(); g.arc(cx, cy, p1, a0, a2); g.arc(cx, cy, p0, a2, a0, true); g.closePath(); };
      g.save(); g.shadowColor = ring.on[1]; g.shadowBlur = 2.2;
      seg(); g.fillStyle = vgrad(g, cy - R1, cy + R1, [[0, ring.on[0]], [0.45, ring.on[1]], [1, ring.on[2]]]); g.fill();
      g.restore();
      surface(g, seg, TEX.brushed, 0.15 * ORB.tex);
      seg(); g.strokeStyle = 'rgba(0,0,0,0.85)'; g.lineWidth = 0.3; g.stroke();
      g.beginPath(); g.arc(cx, cy, p1 - 0.25, a0 + 0.02, a2 - 0.02); g.strokeStyle = 'rgba(255,255,255,0.55)'; g.lineWidth = 0.2; g.stroke();
    }
  }
  circle(g, cx, cy, r); g.fillStyle = radial(g, cx, cy - r * 0.3, r * 1.2, [[0, '#1a1c22'], [1, '#050608']]); g.fill();
  surface(g, () => circle(g, cx, cy, r), TEX.rock, 0.12 * ORB.tex);   // the empty glass: smoky
  g.save(); circle(g, cx, cy, r); g.clip();
  const level = f => cy + r - 2 * r * Math.max(0, Math.min(1, f));
  // A liquid: its gradient, then slow swirls (broad blotches) and a fine shimmer, clipped to its level.
  const liquid = (liq, y0, y1) => {
    const area = () => { g.beginPath(); g.rect(cx - r, y0, 2 * r, y1 - y0); };
    g.fillStyle = vgrad(g, cy - r, cy + r, [[0, liq[0]], [0.4, liq[1]], [1, liq[2]]]); area(); g.fill();
    surface(g, area, TEX.blotch, 0.35 * ORB.tex); surface(g, area, TEX.brushed, 0.12 * ORB.tex, 'soft-light');
  };
  if (fill && fill.frac > 0) {
    const top = level(fill.frac);
    liquid(fill.liq, top, cy + r);
    g.fillStyle = 'rgba(255,255,255,0.35)'; g.fillRect(cx - r, top, 2 * r, 0.4);   // the surface catching the light
  }
  if (over && over.to > over.from) liquid(over.liq, level(over.to), level(over.from));
  // The secondary layer: a crescent from the right, the orb minus a circle shifted left by 2r * frac.
  // At most ORB.band of the orb's width, so the primary stays readable behind it; a little see-through. The inner edge
  // is one fixed curve (bowing ORB.bow * r toward the orb's edge at mid-height) at any fill, as PoE's shield layer.
  const edge = (frac, y) => { const t = (y - cy) / r; return cx + r - 2 * r * ORB.band * frac + ORB.bow * r * (1 - t * t) - ORB.bow * r; };
  const edgePath = (frac, close) => {
    g.beginPath();
    for (let i = 0; i <= 24; i++) { const y = cy - r + (2 * r * i) / 24; i ? g.lineTo(edge(frac, y), y) : g.moveTo(edge(frac, y), y); }
    if (close) { g.lineTo(cx + r + 1, cy + r); g.lineTo(cx + r + 1, cy - r); g.closePath(); }
  };
  // The layer: inside the orb, right of the edge. ('evenodd' is kept for the callers: one closed shape fills alike.)
  const moon = (frac) => edgePath(frac, true);
  if (crescent && crescent.frac > 0) {
    g.globalAlpha = 0.88; moon(Math.min(1, crescent.frac)); g.fillStyle = radial(g, cx + r * 0.5, cy, r * 1.4, [[0, crescent.liq[0]], [0.5, crescent.liq[1]], [1, crescent.liq[2]]]); g.fill('evenodd');
    g.save(); moon(Math.min(1, crescent.frac)); g.clip('evenodd'); surface(g, () => circle(g, cx, cy, r), TEX.blotch, 0.3 * ORB.tex); g.restore();
    if (crescent.light && crescent.light.frac > 0) {
      g.save(); moon(Math.min(1, crescent.frac)); g.clip('evenodd');
      moon(Math.min(crescent.frac, crescent.light.frac)); g.fillStyle = radial(g, cx + r * 0.5, cy, r * 1.4, [[0, crescent.light.liq[0]], [1, crescent.light.liq[1]]]); g.fill('evenodd');
      g.restore();
    }
    g.globalAlpha = 1;
    {   // its inner edge, lit
      edgePath(Math.min(1, crescent.frac), false); g.strokeStyle = 'rgba(255,255,255,0.35)'; g.lineWidth = 0.35; g.stroke();
    }
  }
  // Glass: a soft dark rim and a highlight top-left.
  circle(g, cx, cy, r); g.fillStyle = radial(g, cx, cy, r, [[0.7, 'rgba(0,0,0,0)'], [1, 'rgba(0,0,0,0.55)']]); g.fill();
  g.beginPath(); g.ellipse(cx - r * 0.32, cy - r * 0.45, r * 0.42, r * 0.24, -0.5, 0, Math.PI * 2);
  g.fillStyle = radial(g, cx - r * 0.32, cy - r * 0.45, r * 0.42, [[0, 'rgba(255,255,255,0.45)'], [1, 'rgba(255,255,255,0)']]); g.fill();
  g.restore();
  rim(g, () => circle(g, cx, cy, R1), cy - R1, cy + R1, P.brassHi, 0.3);
}

/** The orb's housing, part of its wing plate: the plate rises into a round iron boss around the orb (the plate's own
 *  iron and texture, so they read as one casting), its brass rim continuing over the top where it rises above the
 *  plate, and a dark seat line where the orb sits recessed. Drawn after the plate, before the orb. */
function ccHousing(g, cx, cy, inner) {   // inner: +1 when the hotbar is to the right, -1 to the left
  const R = ORB.r + ORB.bezel + 2.4, wing = CC.plate.wing, b = BOTTOM, f = 3;   // f: the fillets' radius
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
  g.save(); g.translate(0, -0.6); outline(true); g.fillStyle = 'rgba(0,0,0,0.55)'; g.save(); g.beginPath(); g.rect(cx - dx - 1, cy - R - 2, 2 * dx + 2, wing - (cy - R - 2)); g.clip(); outline(true); g.fill(); g.restore(); g.restore();
  outline(true); g.fillStyle = iron(g, wing, b); g.fill(); ironSurface(g, () => outline(true));
  // The rim along the smooth outline only (not the closing sides inside the plate).
  rim(g, () => outline(false), cy - R, cy + R, P.brassHi, 0.35);
  // The seat: a recessed groove the orb's bezel drops into.
  circle(g, cx, cy, ORB.r + ORB.bezel + 0.55); g.strokeStyle = 'rgba(0,0,0,0.75)'; g.lineWidth = 0.9; g.stroke();
  g.save(); g.translate(0, 0.45); circle(g, cx, cy, ORB.r + ORB.bezel + 1.05); g.strokeStyle = 'rgba(220,225,230,0.12)'; g.lineWidth = 0.3; g.stroke(); g.restore();
}

/** Value / max centred on an orb (two lines when a secondary shows), the bar's small digits. */
function ccOrbNumbers(g, cx, cy, lines) {
  const k = 0.6;
  lines.forEach(([v, max, col], i) => {
    const t = `${v}/${max}`, y = cy - (lines.length * 4) / 2 + i * 4.2;
    ccNumber(g, t, cx - ccNumberWidth(t, k) / 2, y, j => j < String(v).length ? col : '#c4c9cf', 'rgba(0,0,0,0.9)', k);
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
    const food = riding ? { frac: s.mount / s.mountMax, liq: CC_MOUNT }
      : { frac: s.food / 20, liq: s.hunger ? LIQUID.hunger : s.nourished ? CC_GILDED : LIQUID.broth };
    ccOrb(g, ORB.lx, ORB.y, { fill: { frac: hp, liq }, over: s.absorb ? { from: hp, to: hp + s.absorb / max, liq: LIQUID.absorb } : null,
      crescent: food, ring: s.armor > 0 ? { frac: Math.min(1, s.armor / 20), on: ['#eef1f4', '#9aa0a6', '#3a3d41'] } : null,
      shake: ccShake(s)[1] });
    CC.hit.orbL = [ORB.lx - ORB.r, ORB.y - ORB.r, 2 * ORB.r, 2 * ORB.r];
    if (showNums) ccOrbNumbers(g, ORB.lx, ORB.y, [[Math.round(s.health), max, '#ffffff'], riding ? [s.mount, s.mountMax, '#f0c890'] : [s.food, 20, '#ffd08a']]);
  } else ccGearRow(g, ORB.lx, ORB.y);
  // Right: the mana pools (primary the liquid, the other the crescent), breath as the ring under water.
  const pools = [['irons', s.irons, MANA.irons, 200], ['ars', s.ars, MANA.ars, 150]].filter(p => p[1] != null);
  if (s.manaPrimary === 'ars') pools.reverse();
  const tank = s.diving ? Math.max(0, s.tank) : 0, breath = !s.creative && (s.underwater || s.air < 1);
  const ring = !breath ? null : tank > 0 ? { frac: tank / s.tankMax, on: ['#ffd9b0', '#c98a5a', '#5a2814'] }
    : { frac: s.air, on: LIQUID.air };
  if (pools.length) {
    ccOrb(g, ORB.rx, ORB.y, { fill: { frac: pools[0][1], liq: pools[0][2] }, crescent: pools[1] ? { frac: pools[1][1], liq: pools[1][2] } : null, ring });
    CC.hit.orbR = [ORB.rx - ORB.r, ORB.y - ORB.r, 2 * ORB.r, 2 * ORB.r];
    if (showNums) ccOrbNumbers(g, ORB.rx, ORB.y, pools.map(p => [Math.round(p[1] * p[3]), p[3], '#ffffff']));
  } else {
    ccGearRow(g, ORB.rx, ORB.y);
    if (ring) ccOrb(g, ORB.rx, ORB.y, { ring });   // breath still needs its ring: an empty orb carries it
  }
  // The backtank's time, always on (players rely on it), under the right orb's ring.
  if (breath && tank > 0) {
    const t = `${Math.floor(Math.ceil(tank) / 60)}:${String(Math.ceil(tank) % 60).padStart(2, '0')}`;
    ccNumber(g, t, ORB.rx - ccNumberWidth(t, 0.6) / 2, ORB.y + ORB.r + ORB.bezel - 3.5, '#ffffff', 'rgba(0,0,0,0.95)', 0.6);
  }
}
