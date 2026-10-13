// Orbs (after Path of Exile, in our style) in place of the wing meters: a glass orb in a brass bezel on each wing.
// Left orb: health rises as the liquid (absorption in gold above it; the effects recolour it); food is the orb's
// secondary layer, a crescent sweeping in from the right as PoE's energy shield does (saturation the lighter part of
// it; riding: the mount's health instead); armor is a ring of ten segments around the bezel, only while worn.
// Right orb: the primary mana pool as the liquid, the other mana mod's pool (when both are installed) the crescent;
// which is primary is a setting. Breath is a ring around the right orb, only under water (bubbles, or a Create
// backtank in copper with its time left). Uses sprites.js, meters.js (LIQUID), hud-compact.js.
const ORB = { r: 13.5, y: 32.5, lx: -120, rx: 120, ring: [14.9, 16.6], band: 0.36 };   // band: a full crescent's share of the width

/** One orb at (cx, cy): fill {frac, liq}, over {from, to, liq} (absorption, above the fill), crescent {frac, liq,
 *  light: {frac, liq}} (the secondary layer), ring {frac, on, off} (segments around the bezel). */
function ccOrb(g, cx, cy, { fill, over, crescent, ring, shake = 0 }) {
  const r = ORB.r;
  cy += shake;
  // The ring first (outside the bezel): ten segments, lit from the top clockwise.
  if (ring) {
    const [r0, r1] = ORB.ring, n = 10, gap = 0.07;
    for (let k = 0; k < n; k++) {
      const a0 = -Math.PI / 2 + (k / n) * Math.PI * 2 + gap, a1 = -Math.PI / 2 + ((k + 1) / n) * Math.PI * 2 - gap;
      g.beginPath(); g.arc(cx, cy, r1, a0, a1); g.arc(cx, cy, r0, a1, a0, true); g.closePath();
      const lit = ring.frac * n - k;   // >= 1 full, 0..1 partial
      g.fillStyle = ring.off; g.fill();
      if (lit > 0) {
        g.save(); g.clip();
        const a2 = a0 + (a1 - a0) * Math.min(1, lit);
        g.beginPath(); g.moveTo(cx, cy); g.arc(cx, cy, r1 + 1, a0, a2); g.closePath();
        g.fillStyle = radial(g, cx, cy, r1, [[0, ring.on[2]], [0.85, ring.on[1]], [1, ring.on[0]]]); g.fill();
        g.restore();
      }
    }
  }
  // Brass bezel, then the dark glass.
  circle(g, cx, cy + 0.6, r + 1.3); g.fillStyle = 'rgba(0,0,0,0.55)'; g.fill();
  circle(g, cx, cy, r + 1.3); g.fillStyle = brass(g, cy - r, cy + r); g.fill();
  circle(g, cx, cy, r); g.fillStyle = radial(g, cx, cy - r * 0.3, r * 1.2, [[0, '#1a1c22'], [1, '#050608']]); g.fill();
  g.save(); circle(g, cx, cy, r); g.clip();
  const level = f => cy + r - 2 * r * Math.max(0, Math.min(1, f));
  const liquid = (liq, y0, y1) => { g.fillStyle = vgrad(g, cy - r, cy + r, [[0, liq[0]], [0.4, liq[1]], [1, liq[2]]]); g.fillRect(cx - r, y0, 2 * r, y1 - y0); };
  if (fill && fill.frac > 0) {
    const top = level(fill.frac);
    liquid(fill.liq, top, cy + r);
    g.fillStyle = 'rgba(255,255,255,0.35)'; g.fillRect(cx - r, top, 2 * r, 0.4);   // the surface catching the light
  }
  if (over && over.to > over.from) liquid(over.liq, level(over.to), level(over.from));
  // The secondary layer: a crescent from the right, the orb minus a circle shifted left by 2r * frac.
  // At most ORB.band of the orb's width, so the primary stays readable behind it; a little see-through.
  const moon = (frac) => { g.beginPath(); g.arc(cx, cy, r + 0.1, 0, Math.PI * 2); g.arc(cx - 2 * r * ORB.band * frac, cy, r, 0, Math.PI * 2); };
  if (crescent && crescent.frac > 0) {
    g.globalAlpha = 0.88; moon(Math.min(1, crescent.frac)); g.fillStyle = radial(g, cx + r * 0.5, cy, r * 1.4, [[0, crescent.liq[0]], [0.5, crescent.liq[1]], [1, crescent.liq[2]]]); g.fill('evenodd');
    if (crescent.light && crescent.light.frac > 0) {
      g.save(); moon(Math.min(1, crescent.frac)); g.clip('evenodd');
      moon(Math.min(crescent.frac, crescent.light.frac)); g.fillStyle = radial(g, cx + r * 0.5, cy, r * 1.4, [[0, crescent.light.liq[0]], [1, crescent.light.liq[1]]]); g.fill('evenodd');
      g.restore();
    }
    g.globalAlpha = 1;
    {   // its inner edge, lit
      g.beginPath(); g.arc(cx - 2 * r * ORB.band * Math.min(1, crescent.frac), cy, r, -Math.PI / 2.2, Math.PI / 2.2); g.strokeStyle = 'rgba(255,255,255,0.35)'; g.lineWidth = 0.35; g.stroke();
    }
  }
  // Glass: a soft dark rim and a highlight top-left.
  circle(g, cx, cy, r); g.fillStyle = radial(g, cx, cy, r, [[0.7, 'rgba(0,0,0,0)'], [1, 'rgba(0,0,0,0.55)']]); g.fill();
  g.beginPath(); g.ellipse(cx - r * 0.32, cy - r * 0.45, r * 0.42, r * 0.24, -0.5, 0, Math.PI * 2);
  g.fillStyle = radial(g, cx - r * 0.32, cy - r * 0.45, r * 0.42, [[0, 'rgba(255,255,255,0.45)'], [1, 'rgba(255,255,255,0)']]); g.fill();
  g.restore();
  rim(g, () => circle(g, cx, cy, r + 1.3), cy - r, cy + r, P.brassHi, 0.3);
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
  // Left: health, food (or the mount), armor.
  if (!s.creative) {
    const max = s.maxHealth || 20, hp = s.health / max, liq = ccHealthLiquid(s);
    const riding = !!s.riding;
    const food = riding ? { frac: s.mount / s.mountMax, liq: CC_MOUNT }
      : { frac: s.food / 20, liq: s.hunger ? LIQUID.hunger : s.nourished ? CC_GILDED : LIQUID.broth,
          light: { frac: Math.min(s.saturation, s.food) / 20, liq: ['#ffd9a8', '#e09a52'] } };
    ccOrb(g, ORB.lx, ORB.y, { fill: { frac: hp, liq }, over: s.absorb ? { from: hp, to: hp + s.absorb / max, liq: LIQUID.absorb } : null,
      crescent: food, ring: s.armor > 0 ? { frac: Math.min(1, s.armor / 20), on: ['#eef1f4', '#9aa0a6', '#3a3d41'], off: 'rgba(20,22,25,0.9)' } : null,
      shake: ccShake(s)[1] });
    CC.hit.orbL = [ORB.lx - ORB.r, ORB.y - ORB.r, 2 * ORB.r, 2 * ORB.r];
    if (showNums) ccOrbNumbers(g, ORB.lx, ORB.y, [[Math.round(s.health), max, '#ffffff'], riding ? [s.mount, s.mountMax, '#f0c890'] : [s.food, 20, '#ffd08a']]);
  } else ccGearRow(g, ORB.lx, ORB.y);
  // Right: the mana pools (primary the liquid, the other the crescent), breath as the ring under water.
  const pools = [['irons', s.irons, MANA.irons, 200], ['ars', s.ars, MANA.ars, 150]].filter(p => p[1] != null);
  if (s.manaPrimary === 'ars') pools.reverse();
  const tank = s.diving ? Math.max(0, s.tank) : 0, breath = !s.creative && (s.underwater || s.air < 1);
  const ring = !breath ? null : tank > 0 ? { frac: tank / s.tankMax, on: ['#ffd9b0', '#c98a5a', '#5a2814'], off: 'rgba(46,18,6,0.9)' }
    : { frac: s.air, on: LIQUID.air, off: 'rgba(10,30,44,0.9)' };
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
    ccNumber(g, t, ORB.rx - ccNumberWidth(t, 0.6) / 2, ORB.y + ORB.ring[1] - 3.5, '#ffffff', 'rgba(0,0,0,0.95)', 0.6);
  }
}
