// Orbs (after Path of Exile, in our style) in place of the wing meters: a glass orb in a brass bezel on each wing.
// Left orb, split down the middle: health in the left half (absorption in gold above it; the effects recolour it), food
// in the right half (riding: the mount's health instead; saturation is not shown); armor takes over the brass bezel in ten segments, only while worn.
// Right orb: the primary mana pool, split with the other mana mod's pool on the right when both are installed (one: the
// whole orb);
// which is primary is a setting. Breath takes over the right orb's bezel the same way, only under water (bubbles, or a Create
// backtank in copper with its time left). Uses sprites.js, meters.js (LIQUID), hud-compact.js.
const ORB = { tex: 0.67, housing: 16.5,   // housing: the housing's radius (r + bezel + 2.4)
    // tex: the orbs' texture strength (a third less than first drawn)
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
  // The bezel: dark stone grey (not brass), a little lighter where the light falls.
  circle(g, cx, cy, R1); g.fillStyle = vgrad(g, cy - R1, cy + R1, [[0, '#55575b'], [0.35, '#36383c'], [1, '#1b1c1f']]); g.fill();
  surface(g, () => circle(g, cx, cy, R1), TEX.brushed, 0.08 * ORB.tex);   // a light brushed finish
  rim(g, () => circle(g, cx, cy, R1), cy - R1, cy + R1, '#8e9196', 0.3);   // its lit edge, under anything on the rim
  // Dark rivets around the stone rim, at the gaps between the ring's segments.
  for (let k = 0; k < 10; k++) {
    const a = -Math.PI / 2 + k * Math.PI / 5, x = cx + Math.cos(a) * (r + B / 2), y = cy + Math.sin(a) * (r + B / 2);
    circle(g, x, y, 0.42); g.fillStyle = '#121315'; g.fill();
    circle(g, x - 0.1, y - 0.12, 0.16); g.fillStyle = 'rgba(160,165,170,0.5)'; g.fill();
  }
  if (ring) {
    // Only the full segments show (bright plates proud of the rim); empty ones leave the plain rim. A smooth ring
    // (breath, the tank, Helium Flamingo, aircraft) is one continuous arc instead.
    const n = ring.smooth ? 1 : 10, gap = ring.smooth ? 0 : 0.07;
    for (let k = 0; k < n; k++) {
      const lit = ring.frac * n - k;   // >= 1 full, 0..1 partial
      if (lit <= 0) break;
      const a0 = -Math.PI / 2 + (k / n) * Math.PI * 2 + gap, a1 = -Math.PI / 2 + ((k + 1) / n) * Math.PI * 2 - gap;
      const a2 = a0 + (a1 - a0) * Math.min(1, lit);
      // A lit segment stands proud of the rim (wider than the bezel, dark outline) and glows in its colour.
      // Segmented (armor): thick iron plates reaching from the rim across the seat into the housing; smooth: on the rim.
      const p0 = r - 0.35, p1 = ring.smooth ? R1 + 0.45 : R1 + 2.1;
      const seg = () => { g.beginPath(); g.arc(cx, cy, p1, a0, a2); g.arc(cx, cy, p0, a2, a0, true); g.closePath(); };
      if (!ring.smooth) {   // a plate casts a soft shadow onto the housing it is bolted to
        g.save(); g.translate(0.35, 0.55); seg(); g.fillStyle = 'rgba(0,0,0,0.55)'; g.fill(); g.restore();
      }
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
  circle(g, cx, cy, r); g.fillStyle = radial(g, cx, cy - r * 0.3, r * 1.2, [[0, '#1a1c22'], [1, '#050608']]); g.fill();
  surface(g, () => circle(g, cx, cy, r), TEX.rock, 0.12 * ORB.tex);   // the empty glass: smoky
  g.save(); circle(g, cx, cy, r); g.clip();
  const level = f => cy + r - 2 * r * Math.max(0, Math.min(1, f));
  // A liquid: its gradient, then slow swirls (broad blotches) and a fine shimmer, clipped to its level.
  // A liquid with depth: its gradient darkening toward the bottom, a soft inner glow, then its style: 'mana' a galaxy
  // (deep space, nebula clouds in its colour, fine stars), 'cloud' billowing lighter and darker layers (health).
  const liquid = (liq, y0, y1, style) => {
    const area = () => { g.beginPath(); g.rect(cx - r, y0, 2 * r, y1 - y0); };
    g.save(); area(); g.clip();
    if (style === 'mana') {
      // A galaxy: the pool's own colour where the nebula clouds are, darkening toward deep space everywhere else
      // (a multiply mask from the cloud texture, at two scales).
      g.fillStyle = vgrad(g, cy - r, cy + r, [[0, liq[0]], [0.4, liq[1]], [1, liq[2]]]); area(); g.fill();
      g.globalCompositeOperation = 'multiply';
      const pat = g.createPattern(ccNebula(), 'repeat');
      for (const k of [0.07, 0.16]) {
        pat.setTransform(new DOMMatrix().translateSelf(cx * 3, cy * 5).rotateSelf(k * 900).scaleSelf(k, k));
        g.fillStyle = pat; area(); g.fill();
      }
      g.globalCompositeOperation = 'screen';
      // Stars: fixed points (seeded by the orb's position), a few brighter.
      for (let i = 0; i < 26; i++) {
        const h = Math.sin((cx + i * 12.9898) * 78.233) * 43758.5453, u = h - Math.floor(h), v = (h * 7.13) - Math.floor(h * 7.13);
        const x = cx - r + u * 2 * r, y = cy - r + v * 2 * r, big = i % 7 === 0;
        g.fillStyle = big ? 'rgba(255,255,255,0.95)' : 'rgba(220,230,255,0.7)';
        g.fillRect(x, y, big ? 0.45 : 0.25, big ? 0.45 : 0.25);
      }
      g.globalCompositeOperation = 'source-over';
    } else {
      g.fillStyle = vgrad(g, cy - r, cy + r, [[0, liq[0]], [0.4, liq[1]], [1, liq[2]]]); area(); g.fill();
      if (style === 'cloud' || style === 'broth') {   // billows: lighter clouds rising, darker ones settling, at two scales
        g.save(); g.translate(cx, cy); g.scale(0.55, 0.55); g.translate(-cx, -cy);
        surface(g, () => { g.beginPath(); g.rect(cx - 3 * r, cy - 3 * r, 6 * r, 6 * r); }, TEX.blotch, 0.55 * ORB.tex, 'soft-light');
        g.restore();
        surface(g, area, TEX.blotch, 0.45 * ORB.tex, 'overlay');
        surface(g, area, TEX.rock, 0.25 * ORB.tex, 'soft-light');
        if (style === 'broth') for (let i = 0; i < 7; i++) {   // a few rising bubbles
          const h = Math.sin((cx + i * 9.17) * 33.1) * 43758.5453, u = h - Math.floor(h), v = h * 5.3 - Math.floor(h * 5.3);
          const by = y0 + 1 + v * (y1 - y0 - 2);
          circle(g, cx - r + u * 2 * r, by, 0.35 + (i % 3) * 0.15); g.strokeStyle = 'rgba(255,230,190,0.55)'; g.lineWidth = 0.15; g.stroke();
        }
      } else {
        surface(g, area, TEX.blotch, 0.35 * ORB.tex); surface(g, area, TEX.brushed, 0.12 * ORB.tex, 'soft-light');
      }
    }
    // Depth: darker toward the bottom and the sides, a little light from within.
    g.fillStyle = vgrad(g, y0, cy + r, [[0, 'rgba(0,0,0,0)'], [1, 'rgba(0,0,0,0.35)']]); area(); g.fill();
    g.fillStyle = radial(g, cx, cy + r * 0.1, r, [[0, 'rgba(255,255,255,0.08)'], [0.6, 'rgba(0,0,0,0)'], [1, 'rgba(0,0,0,0.3)']]); area(); g.fill();
    g.restore();
  };
  // With a secondary the orb splits down the middle (as Path of Exile does for life and mana): the primary fills the
  // left half, the secondary the right, each rising from the bottom on its own, a thin dark divider between them.
  const half = (x0, w, f, liq, overTo, style) => {
    if (!f || f.frac <= 0) return;
    g.save(); g.beginPath(); g.rect(x0, cy - r, w, 2 * r); g.clip();
    const top = level(f.frac);
    liquid(liq, top, cy + r, style);
    if (overTo && overTo.to > overTo.from) {   // absorption: golden cloud with a few sparkles
      const ya = level(overTo.to), yb = level(overTo.from);
      liquid(overTo.liq, ya, yb, 'cloud');
      for (let i = 0; i < 9; i++) {
        const h = Math.sin((cx + i * 7.31) * 51.7) * 43758.5453, u = h - Math.floor(h), v = h * 3.7 - Math.floor(h * 3.7);
        const x = x0 + u * w, y = ya + v * (yb - ya);
        g.fillStyle = i % 3 ? 'rgba(255,250,210,0.8)' : 'rgba(255,255,255,0.95)'; g.fillRect(x, y, i % 3 ? 0.3 : 0.45, i % 3 ? 0.3 : 0.45);
      }
    }
    const surfaceY = overTo && overTo.to > overTo.from ? level(overTo.to) : top;
    g.fillStyle = 'rgba(255,255,255,0.35)'; g.fillRect(x0, surfaceY, w, 0.4);   // the surface catching the light
    g.restore();
  };
  const split = crescent != null;
  half(cx - r, split ? r : 2 * r, fill, fill && fill.liq, over, fill && fill.style);
  if (split) {
    half(cx, r, crescent, crescent.liq, null, crescent.style);
    g.fillStyle = vgrad(g, cy - r, cy + r, [[0, '#5a5c60'], [1, '#1c1d20']]); g.fillRect(cx - 0.35, cy - r, 0.7, 2 * r);
  }
  // Glass: a soft dark rim and a highlight top-left.
  circle(g, cx, cy, r); g.fillStyle = radial(g, cx, cy, r, [[0.7, 'rgba(0,0,0,0)'], [1, 'rgba(0,0,0,0.55)']]); g.fill();
  g.beginPath(); g.ellipse(cx - r * 0.32, cy - r * 0.45, r * 0.42, r * 0.24, -0.5, 0, Math.PI * 2);
  g.fillStyle = radial(g, cx - r * 0.32, cy - r * 0.45, r * 0.42, [[0, 'rgba(255,255,255,0.45)'], [1, 'rgba(255,255,255,0)']]); g.fill();
  g.restore();
}

/** The health orb's status effects (the meters had them; same rules): the damage flash, Regeneration's rising light,
 *  Farmer's Delight Comfort (saturation 0, hurt, no Regeneration) as a slow warm sheen, the hardcore trim (crimson on
 *  the bezel), and AppleSkin's previews of what held food restores: pale pulsing bands above the health and food
 *  levels in their halves. */
function ccOrbFx(g, s, cx, cy, hp, max, riding) {
  const r = ORB.r, level = f => cy + r - 2 * r * Math.max(0, Math.min(1, f));
  g.save(); circle(g, cx, cy, r); g.clip();
  const leftHalf = () => { g.beginPath(); g.rect(cx - r, cy - r, r, 2 * r); };
  const since = s.t - (s.hurtAt || -1e9);
  if (since < 400) { g.fillStyle = `rgba(255,255,255,${0.6 * (1 - since / 400)})`; leftHalf(); g.fill(); }
  const sweep = (period, col) => {   // a soft band of light rising through the health half
    const y = level(hp) + (1 - (s.t / period) % 1) * (cy + r - level(hp) + 6) - 3;
    g.save(); leftHalf(); g.clip(); g.beginPath(); g.rect(cx - r, level(hp), r, cy + r - level(hp)); g.clip();
    g.fillStyle = vgrad(g, y - 3, y + 3, [[0, col + '00'], [0.5, col + '99'], [1, col + '00']]); g.fillRect(cx - r, y - 3, r, 6);
    g.restore();
  };
  if (s.regen) sweep(1400, '#ffe6dc');
  else if (s.comfort && s.saturation <= 0 && hp < 1) sweep(2600, '#ffecbe');
  const pulse = 0.3 + 0.25 * Math.sin(s.t / 160);
  if (s.holdFood) {
    if (s.holdHeal) { g.fillStyle = `rgba(255,200,200,${pulse})`; g.fillRect(cx - r, level(hp + s.holdHeal / max), r, level(hp) - level(hp + s.holdHeal / max)); }
    if (!riding) { const a = s.food / 20, b = Math.min(20, s.food + s.holdFood) / 20; g.fillStyle = `rgba(255,230,180,${pulse})`; g.fillRect(cx, level(b), r, level(a) - level(b)); }
  }
  g.restore();
  if (s.hardcore) { circle(g, cx, cy, r + ORB.bezel / 2); g.strokeStyle = 'rgba(160,20,28,0.9)'; g.lineWidth = 0.6; g.stroke(); }
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
  // The plate's own gradient (shared, from the dome's peak), plus a soft light on the dome's crown so it reads round.
  outline(true); g.fillStyle = iron(g, cy - R, b); g.fill(); ironSurface(g, () => outline(true));
  g.save(); outline(true); g.clip();
  circle(g, cx, cy - R * 0.55, R * 0.75); g.fillStyle = radial(g, cx, cy - R * 0.7, R * 0.75, [[0, 'rgba(255,240,220,0.10)'], [1, 'rgba(255,240,220,0)']]); g.fill();
  g.restore();
  // The rim along the smooth outline only (not the closing sides inside the plate).
  outline(false); g.strokeStyle = 'rgba(0,0,0,0.6)'; g.lineWidth = 0.6; g.stroke();   // an even dark edge (no shifted shadow: it read as a flat top)
  rim(g, () => outline(false), cy - R, cy + R, P.brassHi, 0.35);
  // The seat: a recessed groove the orb's bezel drops into.
  circle(g, cx, cy, ORB.r + ORB.bezel + 0.55); g.strokeStyle = 'rgba(0,0,0,0.75)'; g.lineWidth = 0.9; g.stroke();
  g.save(); g.translate(0, 0.45); circle(g, cx, cy, ORB.r + ORB.bezel + 1.05); g.strokeStyle = 'rgba(220,225,230,0.12)'; g.lineWidth = 0.3; g.stroke(); g.restore();
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
    ccNumber(g, t, cx - ccNumberWidth(t, k) / 2, y, j => j < String(v).length ? ccTint(col) : ccTintDim(col), 'rgba(0,0,0,0.9)', k);
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
      : { frac: s.food / 20, liq: s.hunger ? LIQUID.hunger : s.nourished ? CC_GILDED : LIQUID.broth, style: 'broth' };
    ccOrb(g, ORB.lx, ORB.y, { fill: { frac: hp, liq, style: 'cloud' }, over: s.absorb ? { from: hp, to: hp + s.absorb / max, liq: LIQUID.absorb } : null,
      crescent: food, ring: s.armor > 0 ? { frac: Math.min(1, s.armor / 20), on: ['#eef1f4', '#9aa0a6', '#3a3d41'] } : null,
      shake: ccShake(s)[1] });
    ccOrbFx(g, s, ORB.lx, ORB.y + ccShake(s)[1], hp, max, riding);
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
  // MekaSuit energy: a slim upright gauge between the offhand slot and the health orb, filling from the bottom.
  if (!s.creative && s.mekasuit) {
    const gx = ORB.lx - ORB.housing - 3.4 - 1.2, gy = 30, gh = 18;   // left of the health orb's housing
    rrect(g, gx, gy, 2.4, gh, 1.2); g.fillStyle = vgrad(g, gy, gy + gh, [[0, '#9ea4aa'], [1, '#141619']]); g.fill();
    rrect(g, gx + 0.4, gy + 0.4, 1.6, gh - 0.8, 0.8); g.fillStyle = '#060708'; g.fill();
    const fh = (gh - 0.8) * s.mekasuit;
    rrect(g, gx + 0.4, gy + gh - 0.4 - fh, 1.6, fh, 0.8); g.fillStyle = vgrad(g, gy, gy + gh, [[0, CC_MEKA[0]], [0.5, CC_MEKA[1]], [1, CC_MEKA[2]]]); g.fill();
  }
  // The backtank's time, always on (players rely on it), just above the right orb's housing.
  if (breath && tank > 0) {
    const t = `${Math.floor(Math.ceil(tank) / 60)}:${String(Math.ceil(tank) % 60).padStart(2, '0')}`;
    ccNumber(g, t, ORB.rx - ccNumberWidth(t, 0.6) / 2, ORB.y - ORB.housing - 4.2, ccTint('#d9874a'), 'rgba(0,0,0,0.95)', 0.6);   // above the housing's dome
  }
}
