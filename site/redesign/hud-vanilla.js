// Vanilla layout (preview, the compact page's Layout toggle; not packed): the pieces at vanilla's places (Gui.java 1.21,
// the table in CLAUDE.md), floating as vanilla's do (no plate behind them), in the compact HUD's art. The hotbar's
// sockets; the offhand socket at one end and the dodge roll dial in the same 18 x 18 box mirrored at the other
// (left-handed swaps them), hardcore's skull plate above the offhand; vanilla's XP bar as one gauge holding the mana
// channel over the XP channel, the level window on it; hearts and food above the gauge, armor and breath above them
// (hud-vanilla-rows.js, by vanilla's rules); the instrument plate and the effects rail above the stack. World units as
// compactHud: x from the screen centre, the screen bottom at y BOTTOM; drawn at R x.

const VAN = {
  end: [100, BOTTOM - 20],          // the right end box's top left (18 x 18); the left one is its mirror (x -118):
                                    // vanilla's offhand item at (cx - 117, bottom - 19)
  x0: -89, x1: 89,                  // the sockets' span (no hotbar frame): no row or bar reaches past it
  gaugeBottom: BOTTOM - 24,         // the gauge's lower edge: it is vanilla's XP bar (bottom - 29, 5 tall)
  gap: 3, rowW: 81,                 // the icon rows: 3 px over the gauge, 10 icons (81 px) wide; rows 10 apart, as vanilla
};

/** The gauge's parts for a state: the frame [x, y, w, h] (vanilla's XP bar, 5 tall), the mana pools in the order shown
 *  ([frac, liq, max]) with their channels [x, y, w, h], the XP channel with what fills it (experience, or ccXpTake's
 *  takeover) and the level window [x, y, w, h]. Like a meter: a 0.5 steel casing around 4 of glass, mana (2) straight
 *  over XP (2), no divider; no mana: XP fills the 4. Two pools split at the window (primary left, filling from the
 *  left; the other right, from the right), each running exactly 1 px under the window's border (no gap, nothing more
 *  hidden); one fills the channel. The window is the compact HUD's (CC_LEVEL), centred
 *  on the gauge, larger while a takeover's icon shows. */
function vanGauge(s) {
  const V = VAN, W = V.x1 - V.x0, pools = [[s.irons, MANA.irons, 200], [s.ars, MANA.ars, 150]].filter(p => p[0] != null);
  if (s.manaPrimary === 'ars') pools.reverse();
  const T = V.gaugeBottom - 5, ix = V.x0 + 0.5, iw = W - 1, take = ccXpTake(s);
  const tw = ccNumberWidth(String(Math.max(0, s.level)), CC_LEVEL.k);
  const [ww, wh] = take ? [CC_LEVEL.iconW, CC_LEVEL.iconH] : [Math.max(tw + 3.6, 8.5), CC_LEVEL.h];
  const win = [-ww / 2, T + 2.5 - wh / 2, ww, wh], wl = win[0] + 1, wr = win[0] + ww - 1;   // two pools: each runs 1 px under the window's border
  const mana = pools.length === 2 ? [[ix, T + 0.5, wl - ix, 2], [wr, T + 0.5, ix + iw - wr, 2]] : pools.length ? [[ix, T + 0.5, iw, 2]] : [];
  return { frame: [V.x0, T, W, 5], pools, mana, xp: pools.length ? [ix, T + 2.5, iw, 2] : [ix, T + 0.5, iw, 4], take, win };
}
/** The gauge: the meters' steel casing as one box (small corners, the channels flush in it), the glass channels
 *  (ccMeterFill, near-square ends), the level window with the level or the takeover's icon. */
function ccGauge(g, s, G) {
  const [fx, fy, fw, fh] = G.frame, R0 = 1, inner = () => rrect(g, fx + 0.5, fy + 0.5, fw - 1, fh - 1, R0 - 0.5);
  rrect(g, fx, fy, fw, fh, R0); g.fillStyle = vgrad(g, fy, fy + fh, [[0, '#9ea4aa'], [0.25, '#6c7278'], [0.65, '#3a3e43'], [1, '#141619']]); g.fill();
  inner(); g.fillStyle = '#060708'; g.fill();
  g.save(); inner(); g.clip();
  G.mana.forEach((r, k) => ccMeterFill(g, ...r, 0.2, G.pools[k][0], G.pools[k][1], G.mana.length === 2 ? 10 : 20, k ? 'right' : 'left', 'mana'));
  const [xf, xl] = G.take || [s.xp, CC_XP];
  ccMeterFill(g, ...G.xp, 0.2, xf, xl, 18, 'left', !G.take ? 'cloud' : s.cast != null ? 'mana' : undefined);
  g.restore();
  const [wx, wy, ww, wh] = G.win, txt = String(Math.max(0, s.level)), LK = CC_LEVEL.k;
  ccLevelWindow(g, wx, wy, ww, wh);
  if (G.take) ccIcon(g, G.take[2](), 0, wy + wh / 2, CC_LEVEL.icon);
  else ccNumber(g, txt, -ccNumberWidth(txt, LK) / 2, wy + wh / 2 - 2.5 * LK, s.level > 0 ? '#80ff20' : '#3a5a2a', 'rgba(0,0,0,0.9)', LK);
  G.mana.forEach((r, k) => {   // numbers: the channel's liquid dimmed behind them, as the meters'
    if (s.numbers || s.hover === 'mana' + k) { g.fillStyle = 'rgba(4,5,6,0.6)'; g.fillRect(...r); }
    vanNumbers(g, s, 'mana' + k, r, `${Math.round(G.pools[k][0] * G.pools[k][2])}/${G.pools[k][2]}`, G.pools[k][1][1]);
  });
}
/** The dodge roll in an end box (top left x, y; 18 x 18, the offhand socket's size): the dial scaled to fill it, or
 *  without the mod its pressed-in cog. */
function vanDial(g, s, x, y) {
  const cx = x + 9, cy = y + 9, S = 9 / (CC_DASH.r + CC_DASH.B + 0.45);   // the rim and its recess just inside the box
  ccScaled(g, cx, cy, S, () => s.rolls == null ? ccDashFiller(g, cx, cy) : ccDashDial(g, cx, cy, s.rolls, s.rollCount || 2));
}
/** The vanilla-layout HUD for a state s (the compact page's state fields); a canvas of logical 2 CX x ccCanvasH(s), as
 *  compactHud's. */
function vanillaHud(s, font) {
  const H = ccCanvasH(s), [c, g] = hires(2 * CX, H), V = VAN, G = vanGauge(s);
  g.translate(CX, H - BOTTOM);
  if ((s.barScale || 1) !== 1) { g.translate(0, BOTTOM); g.scale(s.barScale, s.barScale); g.translate(0, -BOTTOM); }   // the action bar size
  ccGameText(g, s, font);
  if (s.spectator) return c;
  CC.hit = {};
  for (let i = 0; i < 9; i++) socket(g, ...CC.slot(i));
  // The end boxes: the offhand at the off hand's side (vanilla moves it right for left-handed players), the dial opposite.
  const [ex, ey] = V.end, [ox, dx] = s.leftHanded ? [ex, -ex - 18] : [-ex - 18, ex];
  socket(g, ox, ey);
  g.save(); g.translate(ox + 9, ey + 9.4); g.scale(1.25, 1.25); g.translate(-4.5, -4.5);
  shieldPath(g); g.setLineDash([0.4, 0.4]); g.strokeStyle = '#3a414b'; g.lineWidth = 0.4; g.stroke(); g.restore();
  vanDial(g, s, dx, ey);
  if (s.hardcore) {   // hardcore: the action bar's skull plate, floating above the offhand socket
    const { w, h, lift } = CC_SKULL_PLATE, px = ox + 9 - w / 2, py = ey - h - lift;
    ccFloatPlate(g, px, py, w, h); ccSkull(g, px + w / 2, py + h / 2);
  }
  ccGauge(g, s, G);
  let top = G.frame[1];
  if (!s.creative) {   // creative hides health, food, armor and air, as vanilla
    const y0 = G.frame[1] - V.gap - 9, max = s.maxHealth || 20, liq = ccHealthLiquid(s);
    vanShown(s, 'hearts');
    let left = vanHearts(g, s, V.x0, y0), right = y0;
    vanNumbers(g, s, 'hearts', [V.x0, left, V.rowW, y0 + 9 - left], s.absorb ? `${Math.round(s.health)}+${s.absorb}/${max}` : `${Math.round(s.health)}/${max}`, liq[1]);
    vanShown(s, 'food');
    if (s.riding) {
      right = vanMountRow(g, s, V.x1, y0);
      vanNumbers(g, s, 'food', [V.x1 - V.rowW, right, V.rowW, y0 + 9 - right], `${s.mount}/${s.mountMax}`, CC_MOUNT[1]);
    } else {
      vanFoodRow(g, s, V.x1, y0);
      g.save(); g.translate(2 * V.x1 - V.rowW, 0); g.scale(-1, 1); ccExhaustion(g, s, [V.x1 - V.rowW, y0, V.rowW, 9]); g.restore();   // drains toward the right end
      vanNumbers(g, s, 'food', [V.x1 - V.rowW, y0, V.rowW, 9], `${s.food}/20`, LIQUID.broth[1]);
    }
    if (s.armor) {
      vanShown(s, 'armor');
      for (let i = 0; i < 10; i++) vanAt(g, armor(vanKind(s.armor, i)), V.x0 + 8 * i, left - 10);
      vanNumbers(g, s, 'armor', [V.x0, left - 10, V.rowW, 9], `${s.armor}/${s.armor > 20 ? 30 : 20}`, '#9aa0a6');
      left -= 10;
    }
    vanShown(s, 'air');
    if (vanBreathRow(g, s, V.x1, right - 10, V.rowW)) {
      if (!ccTankLeft(s)) vanNumbers(g, s, 'air', [V.x1 - V.rowW, right - 10, V.rowW, 9], `${Math.ceil(s.air * 10)}/10`, LIQUID.air[1]);
      right -= 10;
    }
    vanDim = 1;   // the tank row sets no numbers
    top = Math.min(left, right);
  }
  const plate = ccInstruments(g, s, top - 9 - 3);   // the instrument plate above the stack
  if (plate != null) top = plate;
  ccEffects(g, s, s.effectIcons || {}, { left: -V.end[0] - 18 - 1, top: top - 2 });   // the rail: the first socket on the left end box's edge; or the screen's top left corner
  ccSlotMarks(g, s);
  const [sx, sy] = CC.slot(s.slot), toolboxSel = (s.toolboxSlots || []).includes(s.slot);
  ccSelection(g, sx, sy, toolboxSel);
  if (toolboxSel) ccToolboxChips(g, s.slot, true);
  ccAttackShade(g, s, [sx, sy]);
  return c;
}
