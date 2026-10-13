// Status effects (buffs and debuffs), kept where vanilla puts them (the screen's top-right: beneficial in the top row,
// harmful in the second, newest leftmost, 25 px apart; Gui.renderEffects) and restyled to match the action bar: each
// effect a small iron socket, brass-bezeled for a buff, rust for a debuff, dimmer for an ambient (beacon) effect.
// Instead of vanilla's blinking under 10 s, a bright border drains clockwise from the top as the effect runs out.
// Uses sprites.js. Shipping it: haven-create-ui-ux replaces the MOB_EFFECTS layer (the frame from the pack, the
// border drawn as fills).

// Buffs read green, debuffs red: the draining border (and its glow) in that colour, on a brass or rust bezel.
/** Outlined effect numbers (CC_EFFECT_INK, hud-compact.js): a solid 1 font px black outline, then the tint. */
function ccOutlinedNumber(g, t, x, y, col, k) {
  for (const [dx, dy] of [[-1, -1], [0, -1], [1, -1], [-1, 0], [1, 0], [-1, 1], [0, 1], [1, 1]]) ccNumber(g, t, x + dx * k, y + dy * k, '#000000', null, k);
  ccNumber(g, t, x, y, col, null, k);
}
const CC_EFFECT = {
  buff: { bezel: ['#f4d898', '#b8893f', '#6e4f22'], track: 'rgba(20,60,26,0.95)', glow: '#62e07a' },
  debuff: { bezel: ['#e8a080', '#9a4a28', '#4a1c0a'], track: 'rgba(70,14,12,0.95)', glow: '#ff4e44' },
  // A worn MekaSuit's energy: a cyan status in the buffs line (after the always-on buffs), border = energy left.
  suit: { bezel: ['#bdf2ff', '#3f9fb8', '#123a46'], track: 'rgba(10,40,50,0.95)', glow: '#3fe0ff' },
};
/** Fuelled gear, one status per resource after the MekaSuit's, the same cyan kind ('suit': cyan border and number),
 *  told apart by the worn item's icon (the game draws the item) and the tooltip's name. Units as each mod shows them. */
const CC_GEAR = {
  mekJetpack: { name: 'Jetpack hydrogen', unit: 'mB', max: 24000, icon: 'jetpack' },
  jetSuitEnergy: { name: 'Jet Suit energy', unit: 'E', max: 1000000, icon: 'jetsuit' },
};
/** The preview's gear icons (the game draws the worn item): 16x16 pixel drawings. */
const CC_GEAR_ICONS = {};
function ccGearIcon(name) {
  if (CC_GEAR_ICONS[name]) return CC_GEAR_ICONS[name];
  const c = document.createElement('canvas'); c.width = c.height = 16;
  const g = c.getContext('2d'), px = (col, ...r) => { g.fillStyle = col; for (const [x, y, w, h] of r) g.fillRect(x, y, w, h); };
  if (name === 'jetpack' || name === 'jetsuit') {   // two tanks on a harness, nozzles below
    px('#2a2f36', [2, 2, 5, 11], [9, 2, 5, 11], [6, 4, 4, 3]);
    px(name === 'jetsuit' ? '#e8edf2' : '#9aa3ad', [3, 3, 3, 9], [10, 3, 3, 9]);
    px('#6b7380', [7, 5, 2, 1]); px('#ffb24a', [3, 13, 3, 2], [10, 13, 3, 2]);
    if (name === 'jetsuit') px('#ffe14a', [4, 6, 1, 3], [11, 6, 1, 3]);
  } else if (name === 'scuba' || name === 'o2tank') {   // an upright tank with a valve
    px('#2a2f36', [4, 2, 8, 13], [6, 0, 4, 3]);
    px(name === 'o2tank' ? '#c98a5a' : '#e8edf2', [5, 3, 6, 11]);
    px('#8fc4ff', [6, 6, 4, 2]); px('#6b7380', [7, 1, 2, 2]);
  } else {   // space suit: a white chestplate with a visor-blue panel
    px('#2a2f36', [2, 2, 4, 3], [10, 2, 4, 3], [1, 4, 14, 3], [2, 7, 12, 7], [3, 14, 10, 1]);
    px('#f2f2ee', [3, 3, 2, 2], [11, 3, 2, 2], [2, 5, 12, 2], [3, 7, 10, 6]);
    px('#8fc4ff', [6, 7, 4, 3]); px('#c9c9c2', [3, 11, 10, 2]);
  }
  return (CC_GEAR_ICONS[name] = c);
}
/** The MekaSuit icon in the preview (the game draws the worn piece's item): a white chestplate with cyan trim. 16x16. */
let CC_SUIT_ICON = null;
function ccSuitIcon() {
  if (CC_SUIT_ICON) return CC_SUIT_ICON;
  const c = document.createElement('canvas'); c.width = c.height = 16;
  const g = c.getContext('2d'), px = (col, ...r) => { g.fillStyle = col; for (const [x, y, w, h] of r) g.fillRect(x, y, w, h); };
  px('#2a2f36', [2, 2, 4, 3], [10, 2, 4, 3], [1, 4, 14, 3], [2, 7, 12, 7], [3, 14, 10, 1]);   // outline
  px('#e8edf2', [3, 3, 2, 2], [11, 3, 2, 2], [2, 5, 12, 2], [3, 7, 10, 6]);                     // shell
  px('#b6bec8', [3, 11, 10, 2], [7, 5, 2, 8]);                                                  // shade, centre seam
  px('#3fe0ff', [6, 7, 4, 2], [4, 10, 1, 2], [11, 10, 1, 2]);                                   // cyan trim and core
  return (CC_SUIT_ICON = c);
}

/** One effect at (x, y), 24 x 24: socket, icon (14 x 14 at +5), the draining border (thick, ~1 px when drawn at 11). frac: time left / starting time
 *  (1 for infinite), ambient: a beacon's (dimmer). */
function ccEffect(g, x, y, kind, icon, frac, ambient = false, frameOnly = false) {
  const k = CC_EFFECT[kind], r = 2.2;
  rrect(g, x + 0.5, y + 0.9, 23, 23, r); g.fillStyle = 'rgba(0,0,0,0.5)'; g.fill();          // shadow
  rrect(g, x + 0.5, y + 0.5, 23, 23, r);
  g.fillStyle = vgrad(g, y, y + 24, [[0, k.bezel[0]], [0.25, k.bezel[1]], [1, k.bezel[2]]]); g.fill();
  rrect(g, x + 1.6, y + 1.6, 20.8, 20.8, r - 0.8);
  g.fillStyle = vgrad(g, y + 1.6, y + 22.4, [[0, '#08090a'], [1, '#1a1a1c']]); g.fill();
  if (icon) { g.save(); if (ambient) g.globalAlpha = 0.7; g.imageSmoothingEnabled = false; g.drawImage(icon, x + 5, y + 5, 14, 14); g.restore(); }
  if (ambient) { rrect(g, x + 0.5, y + 0.5, 23, 23, r); g.fillStyle = 'rgba(10,12,16,0.35)'; g.fill(); return; }
  if (frameOnly) return;   // the game draws the time border itself (fills), over this frame
  // The time border: a dark track, then the time left drawn clockwise from the top centre.
  const w = 20.4, rr = r - 0.4, per = 4 * (w - 2 * rr) + 2 * Math.PI * rr;
  const border = () => {
    const x0 = x + 1.8, y0 = y + 1.8;
    g.beginPath(); g.moveTo(x0 + w / 2, y0);
    g.arcTo(x0 + w, y0, x0 + w, y0 + w, rr); g.arcTo(x0 + w, y0 + w, x0, y0 + w, rr);
    g.arcTo(x0, y0 + w, x0, y0, rr); g.arcTo(x0, y0, x0 + w, y0, rr); g.lineTo(x0 + w / 2, y0);
  };
  g.lineWidth = 2.2; g.lineCap = 'butt';   // about 1 px at the bar's 11 px size
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

/** Effects in the action bar area, floating above the bar's left wing (no plate), left-aligned to the bar's edge,
 *  in two lines: buffs nearest the bar (green), debuffs above them (red, at 1.5x), each always-on first. Each effect a
 *  10 x 10 socket with the draining border and its time left under it in the bar's small digits (45, 3m, 1h;
 *  none when infinite) in the kind's tint, which does not change as it runs out. At most PER_ROW in a row (the right wing's width, clear of the centre),
 *  more wrap onto another row above, so a long list grows upward instead of sideways into anything.
 *  effects: [{id, kind: 'buff' | 'debuff', secs, total, ambient}], icons by id. */
const CC_EFFECTS = { size: 10, cell: 13, rowH: 19.5, k: 0.625, tk: 1.5, perRow: 10 };   // tk: the times' digits scale (the bigger bar)   // a line: the bar's left edge to x -32
/** Time left, short: plain seconds under a minute ("45"), minutes under an hour ("3m"), else hours ("1h"). */
function ccEffectTime(secs) {
  const sec = Math.ceil(secs);
  return sec < 60 ? String(sec) : sec < 3600 ? Math.floor(sec / 60) + 'm' : Math.floor(sec / 3600) + 'h';
}
function ccEffectsRail(g, s, icons) {
  const list = s.effects || [], { size, cell, rowH, k, tk, perRow } = CC_EFFECTS, left = ccSideX((CC.lay || CC.plate).x0, ccSideK(s));   // the bar's left edge
  // Two lines: buffs nearest the bar, debuffs above them at 1.5x. A line that runs out of room wraps upward.
  // Each line starts with its always-on effects (infinite or a beacon's), then the timed ones.
  const line = kind => list.filter(e => e.kind === kind && !e.total).concat(list.filter(e => e.kind === kind && e.total));
  const green = line('buff'), red = line('debuff');
  // A worn MekaSuit: one more socket after the always-on buffs, before the timed ones.
  // Then one socket per fuelled gear resource worn (CC_GEAR), in the same group.
  const worn = s.creative ? [] : [...(s.mekasuit ? [{ id: 'mekasuit', kind: 'suit', suit: s.mekasuit }] : []),
    ...Object.entries(s.gear || {}).map(([id, frac]) => ({ id, kind: 'suit', gear: CC_GEAR[id], frac }))];
  green.splice(green.filter(e => !e.total).length, 0, ...worn);
  // Debuffs at 1.5x (socket, spacing, digits): the ones to act on stand out.
  const draw = (e, x, y, m) => {
    const sz = size * m, kk = k * m * tk;
    g.save(); g.translate(x, y); g.scale(sz / 24, sz / 24);
    ccEffect(g, 0, 0, e.kind, e.suit ? ccSuitIcon() : e.gear ? ccGearIcon(e.gear.icon) : icons[e.id], e.suit ? e.suit : e.gear ? e.frac : e.total ? e.secs / e.total : 1, e.ambient);
    g.restore();
    CC.hit['effect:' + e.id] = [x, y, sz, sz];
    if (e.gear) {   // the amount left (percent, no sign), cyan as the MekaSuit's
      const t = String(Math.round(e.frac * 100));
      ccOutlinedNumber(g, t, x + sz / 2 - ccNumberWidth(t, kk) / 2, y + sz + m, CC_EFFECT_INK.gear, kk);
      return;
    }
    if (e.suit) {   // the energy left (percent), always cyan
      const t = String(Math.round(e.suit * 100));   // percent, no sign
      ccOutlinedNumber(g, t, x + sz / 2 - ccNumberWidth(t, kk) / 2, y + sz + m, CC_EFFECT_INK.gear, kk);
      return;
    }
    if (!e.total) return;
    const t = ccEffectTime(e.secs);
    // The kind's tint all the way down: the colour does not change as the time runs out.
    ccOutlinedNumber(g, t, x + sz / 2 - ccNumberWidth(t, kk) / 2, y + sz + m, e.kind === 'buff' ? CC_EFFECT_INK.buff : CC_EFFECT_INK.debuff, kk);
  };
  let top = ccSideY(s.orbs ? ORB.y - ORB_H() : CC_WING.top - (s.hardcore ? CC_SKULL_PLATE.h + CC_SKULL_PLATE.lift + 1 : 0), ccSideK(s)) - 2;   // above the domes / the wings (and the skull), at the side size
  for (const [all, m] of [[green, 1], [red, 1.5]]) {   // from the bar up: buffs (always-on first), then debuffs on top
    const per = Math.floor(perRow / m);
    for (let r = 0; r * per < all.length; r++) {
      top -= rowH * m;
      all.slice(r * per, (r + 1) * per).forEach((e, i) => draw(e, left + 1 + cell * m * i, top, m));
    }
  }
}
/** An effect's name as the game shows it: words capitalised, the level in Roman numerals from II. */
function ccEffectName(e) {
  const name = e.id.split('_').map(w => w[0].toUpperCase() + w.slice(1)).join(' ');
  return e.amp ? `${name} ${['II', 'III', 'IV', 'V'][e.amp - 1] || e.amp + 1}` : name;
}
