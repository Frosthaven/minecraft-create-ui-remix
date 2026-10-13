// Status effects (buffs and debuffs), kept where vanilla puts them (the screen's top-right: beneficial in the top row,
// harmful in the second, newest leftmost, 25 px apart; Gui.renderEffects) and restyled to match the action bar: each
// effect a small iron socket, brass-bezeled for a buff, rust for a debuff, dimmer for an ambient (beacon) effect.
// Instead of vanilla's blinking under 10 s, a bright border drains clockwise from the top as the effect runs out.
// Uses sprites.js. Shipping it: Create Dark Inventory replaces the MOB_EFFECTS layer (the frame from the pack, the
// border drawn as fills).

// Buffs read green, debuffs red: the draining border (and its glow) in that colour, on a brass or rust bezel.
const CC_EFFECT = {
  buff: { bezel: ['#f4d898', '#b8893f', '#6e4f22'], track: 'rgba(20,60,26,0.95)', glow: '#62e07a' },
  debuff: { bezel: ['#e8a080', '#9a4a28', '#4a1c0a'], track: 'rgba(70,14,12,0.95)', glow: '#ff4e44' },
};

/** One effect at (x, y), 24 x 24: socket, icon (18 x 18 at +3), the draining border. frac: time left / starting time
 *  (1 for infinite), ambient: a beacon's (dimmer). */
function ccEffect(g, x, y, kind, icon, frac, ambient = false) {
  const k = CC_EFFECT[kind], r = 2.2;
  rrect(g, x + 0.5, y + 0.9, 23, 23, r); g.fillStyle = 'rgba(0,0,0,0.5)'; g.fill();          // shadow
  rrect(g, x + 0.5, y + 0.5, 23, 23, r);
  g.fillStyle = vgrad(g, y, y + 24, [[0, k.bezel[0]], [0.25, k.bezel[1]], [1, k.bezel[2]]]); g.fill();
  rrect(g, x + 1.6, y + 1.6, 20.8, 20.8, r - 0.8);
  g.fillStyle = vgrad(g, y + 1.6, y + 22.4, [[0, '#08090a'], [1, '#1a1a1c']]); g.fill();
  if (icon) { g.save(); if (ambient) g.globalAlpha = 0.7; g.imageSmoothingEnabled = false; g.drawImage(icon, x + 3, y + 3, 18, 18); g.restore(); }
  if (ambient) { rrect(g, x + 0.5, y + 0.5, 23, 23, r); g.fillStyle = 'rgba(10,12,16,0.35)'; g.fill(); return; }
  // The time border: a dark track, then the time left drawn clockwise from the top centre.
  const w = 21.6, rr = r - 0.4, per = 4 * (w - 2 * rr) + 2 * Math.PI * rr;
  const border = () => {
    const x0 = x + 1.2, y0 = y + 1.2;
    g.beginPath(); g.moveTo(x0 + w / 2, y0);
    g.arcTo(x0 + w, y0, x0 + w, y0 + w, rr); g.arcTo(x0 + w, y0 + w, x0, y0 + w, rr);
    g.arcTo(x0, y0 + w, x0, y0, rr); g.arcTo(x0, y0, x0 + w, y0, rr); g.lineTo(x0 + w / 2, y0);
  };
  g.lineWidth = 0.7; g.lineCap = 'butt';
  border(); g.strokeStyle = k.track; g.stroke();
  g.save(); g.setLineDash([per * Math.max(0, Math.min(1, frac)), per + 1]);
  g.shadowColor = k.glow; g.shadowBlur = 1.2;
  border(); g.strokeStyle = k.glow; g.stroke();
  g.restore();
}

/** The screen's top-right corner with the given effects ({id, kind, frac, ambient}), w logical px wide. */
function ccEffectsCorner(effects, icons, w = 180) {
  const [c, g] = hires(w, 56);
  const rows = { buff: [], debuff: [] };
  for (const e of effects) rows[e.kind].push(e);
  for (const [kind, y] of [['buff', 1], ['debuff', 26]]) {
    rows[kind].forEach((e, i) => ccEffect(g, w - 25 * (i + 1), y, kind, icons[e.id], e.frac, e.ambient));
  }
  return c;
}

/** Effects in the action bar area: two floating rows above the bar's right wing (no plate), buffs on top and debuffs
 *  below as vanilla stacks them, right-aligned to the bar's edge (newest at the right); each effect an 11 x 11 socket
 *  with the draining border and its time left under it in the bar's digits (seconds, or m:ss from a minute; red
 *  under 10 s; none when infinite). effects: [{id, kind: 'buff' | 'debuff', secs, total, ambient}], icons by id. */
function ccEffectsRail(g, s, icons) {
  const list = s.effects || [];
  const size = 11, cell = 15, rowH = 16.5, k = 0.7, right = CC.plate.x1;
  const rows = { buff: list.filter(e => e.kind === 'buff'), debuff: list.filter(e => e.kind === 'debuff') };
  const debuffTop = CC.plate.mid - 2 - rowH, buffTop = rows.debuff.length ? debuffTop - rowH : debuffTop;
  for (const [kind, top] of [['buff', buffTop], ['debuff', debuffTop]]) {
    rows[kind].forEach((e, i) => {
      const x = right - size - 1 - cell * i - (cell - size) / 2, y = top;
      g.save(); g.translate(x, y); g.scale(size / 24, size / 24);
      ccEffect(g, 0, 0, kind, icons[e.id], e.total ? e.secs / e.total : 1, e.ambient);
      g.restore();
      CC.hit['effect:' + e.id] = [x, y, size, size];
      if (!e.total) return;
      const sec = Math.ceil(e.secs), t = sec >= 60 ? `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}` : String(sec);
      ccNumber(g, t, x + size / 2 - ccNumberWidth(t, k) / 2, y + size + 1.2, sec <= 10 ? '#ffb0a0' : '#ffffff', 'rgba(0,0,0,0.95)', k);
    });
  }
}

/** An effect's name as the game shows it: words capitalised, the level in Roman numerals from II. */
function ccEffectName(e) {
  const name = e.id.split('_').map(w => w[0].toUpperCase() + w.slice(1)).join(' ');
  return e.amp ? `${name} ${['II', 'III', 'IV', 'V'][e.amp - 1] || e.amp + 1}` : name;
}
