// Compact HUD statuses: every state the game shows around the hotbar, mapped onto the compact action bar (preview).
// Uses sprites.js, meters.js (LIQUID), hud-compact.js (ccMeter). s.t is the time in ms (animations).
//   Hearts: poison / wither / frozen recolour the health liquid; hardcore shows a skull above the offhand (ccHardcore); at 4 health or less the
//     meter shakes; damage flashes it white; regeneration sweeps a light along it.
//   Food: saturation (vanilla's yellow outline, AppleSkin's overlay) fills the first segments, up to its level (never
//     past the food), in light cream against the orange broth; the Hunger effect turns the broth spoiled olive; holding food shows
//     what it would restore (AppleSkin), a pale pulsing band past the fill.
//   The heart icon follows the effect (poisoned, withered, frozen), as vanilla's hearts do.
//   Riding: the mount's health replaces food (vanilla hides food then). The XP pipe is shared: a charging jump, a
//     camel's dash cooldown and a charging spell (Iron's) take it over, the level window showing their icon.
//   Attack cooldown (hotbar mode): a shade
//   over the selected socket that lifts as the swing recharges. Creative: health, food, armor and air hide.

const CC_JUMP = ccLift(['#fff2a8', '#e0a020', '#5a3808']);
const CC_MOUNT = ccLift(['#b8ffe0', '#2fbf8a', '#0b4a34']);   // the mount's health: emerald, clearly apart from food's orange
const CC_COOLDOWN = ccLift(['#b8bcc2', '#6a6e74', '#26282b']);

/** The health liquid for the current effect. */
function ccHealthLiquid(s) {
  return { poison: LIQUID.poison, wither: LIQUID.wither, frozen: LIQUID.frozen }[s.effect] || LIQUID.blood;
}
/** A shake offset at low health (the game jitters hearts at 4 health or less). */
function ccShake(s) {
  if (s.health > 4) return [0, 0];
  const k = Math.floor(s.t / 60);
  return [0, ((k * 7919) % 3) - 1];
}
/** Effects over the health meter at box: damage flash, low-health pulse, regeneration motes. */
/** Natural healing (food regeneration, not the Regeneration effect): each heal a short soft brightening of the health
 *  liquid, eased in and out over CC_HEAL.ms, warm pink-white at a low alpha (quieter than Regeneration's motes). The
 *  game: HudFrame.healAt, a rise in health without Regeneration. Returns the alpha now (0 when idle). */
const CC_HEAL = { ms: 350, alpha: 0.22, rgb: '255,214,222' };
function ccHealPulse(s) {
  const since = s.t - (s.healAt ?? -1e9);
  return since >= 0 && since < CC_HEAL.ms ? Math.sin(Math.PI * since / CC_HEAL.ms) ** 2 * CC_HEAL.alpha : 0;
}
function ccHealthFx(g, s, [x, y, w, h]) {
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.clip();
  // The orbs' effects, laid along the meter: the damage flash red, Regeneration's pink motes drifting along the
  // liquid, and at 4 health or less the empty glass pulsing red (in sync with low food) instead of a shake.
  const fill = (w - 1) * Math.max(0, Math.min(1, s.health / (s.maxHealth || 20)));
  const since = s.t - (s.hurtAt || -1e9);
  if (since < 400) { g.fillStyle = `rgba(255,40,40,${0.55 * (1 - since / 400)})`; g.fillRect(x, y, w, h); }
  if (s.health <= 4) { g.fillStyle = `rgba(200,20,30,${ccWarn(s)})`; g.fillRect(x + 0.5 + fill, y, w - 1 - fill, h); }
  const heal = ccHealPulse(s);
  if (heal > 0 && fill > 0) { g.fillStyle = `rgba(${CC_HEAL.rgb},${heal})`; g.fillRect(x + 0.5, y, fill, h); }   // natural heal
  if (s.regen && fill > 0) {   // Regeneration: the orb's pink particles, drifting left to right along the health liquid
    // and fading as they reach its end; a few more and a little larger than the orb's, so they read on the thin meter.
    for (let i = 0; i < 9; i++) {
      const hh = Math.sin((i + 1) * 91.7) * 43758.5453, u = hh - Math.floor(hh), k = ((s.t / 1500) * (0.6 + u * 0.6) + u * 3.7) % 1;
      const py = y + 0.9 + (h - 1.8) * (0.5 + 0.45 * Math.sin(i * 2.3 + s.t / 300));
      circle(g, x + 0.5 + k * fill, py, 0.42 + (i % 3) * 0.1); g.fillStyle = `rgba(255,120,175,${Math.sin(Math.PI * k) * 0.95})`; g.fill();
    }
  }
  g.restore();
}
/** The shared warning pulse (low health, low food): one timing so they flash in sync. */
function ccWarn(s) { return 0.12 + 0.12 * Math.sin(s.t / 220); }
/** Farmer's Delight Nourishment: a gold glow over the saturated broth (what Nourishment protects), ending at
 *  saturation's own cut line, breathing slowly (period ms) at a low alpha so it is plain where it is. Returns the alpha
 *  now. The game: HudFrame.nourishGlow. */
const CC_NOURISH = { ms: 2000, base: 0.16, amp: 0.08, rgb: '255,210,90' };
function ccNourishGlow(s) { return CC_NOURISH.base + CC_NOURISH.amp * Math.sin(2 * Math.PI * s.t / CC_NOURISH.ms); }
/** The food meter's effects, as on the orb: Nourishment's breathing gold glow over the saturated part, and at 6 food or
 *  less the empty glass pulsing amber. */
function ccFoodFx(g, s, [x, y, w, h]) {
  const fill = (w - 1) * Math.max(0, Math.min(1, s.food / 20));
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.clip();
  if (s.food <= 6) { g.fillStyle = `rgba(220,120,20,${ccWarn(s)})`; g.fillRect(x + 0.5 + fill, y, w - 1 - fill, h); }
  const sat = (w - 1) * Math.max(0, Math.min(s.saturation, s.food)) / 20;
  if (s.nourished && sat > 0) { g.fillStyle = `rgba(${CC_NOURISH.rgb},${ccNourishGlow(s)})`; g.fillRect(x + 0.5, y, sat, h); }
  g.restore();
}
/** The heart icon for the current effect: the HUD's vial heart, its liquid in the effect's colours. 9 x 9. */
function ccHeartIcon(s) {
  if (!s.effect) return heart(false);
  const liq = ccHealthLiquid(s), [c, g] = hires(9, 9);
  heartPath(g); g.fillStyle = vgrad(g, 1, 8.2, [[0, liq[0]], [0.4, liq[1]], [1, liq[2]]]); g.fill();
  heartFrame(g);
  return c;
}
/** Saturation over the food meter: the segments up to saturation / 20 (never past the food) in their own colour, a
 *  light cream against the orange broth, full height, the segment lines kept. */
function ccSaturation(g, s, [x, y, w, h]) {
  const sat = Math.min(s.saturation, s.food);
  if (sat <= 0) return;
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.clip();
  g.fillStyle = vgrad(g, y + 0.5, y + h - 0.5, [[0, '#fff4e2'], [0.45, '#e9c79c'], [1, '#6e4e30']]);
  g.fillRect(x + 0.5, y, (w - 1) * sat / 20, h);
  g.fillStyle = 'rgba(0,0,0,0.55)';
  for (let k = 1; k < 10; k++) g.fillRect(x + 0.5 + (w - 1) * k / 10 - 0.15, y, 0.3, h);
  g.restore();
}
/** AppleSkin's restore preview over the food meter: from the fill to fill + restore, pale and pulsing. */
function ccFoodPreview(g, s, [x, y, w, h]) {
  if (!s.holdFood) return;
  const from = (w - 1) * s.food / 20, to = (w - 1) * Math.min(20, s.food + s.holdFood) / 20;
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.clip();
  g.fillStyle = `rgba(255,230,180,${0.35 + 0.25 * Math.sin(s.t / 160)})`; g.fillRect(x + 0.5 + from, y, to - from, h);
  g.restore();
}
/** The mount icon: a horse's head in profile, chestnut with a dark mane. 9 x 9. */
function ccHorse() {
  const [c, g] = hires(9, 9);
  const head = () => {
    g.beginPath(); g.moveTo(3.2, 8.6); g.lineTo(3.4, 4.2); g.quadraticCurveTo(3.6, 1.6, 5.6, 1.2); g.lineTo(6, 0.4); g.lineTo(6.6, 1.4);
    g.quadraticCurveTo(7.6, 2.4, 8.4, 5.6); g.quadraticCurveTo(8.6, 6.6, 7.6, 6.7); g.quadraticCurveTo(6.4, 6.2, 5.8, 5.2);
    g.quadraticCurveTo(5.6, 7, 6.4, 8.6); g.closePath();
  };
  head(); g.fillStyle = vgrad(g, 0.4, 8.6, [[0, '#c8844a'], [0.5, '#8e4f22'], [1, '#4a2410']]); g.fill();
  g.strokeStyle = 'rgba(0,0,0,0.6)'; g.lineWidth = 0.35; g.stroke();
  g.beginPath(); g.moveTo(3.2, 8.4); g.lineTo(3.3, 4.3); g.quadraticCurveTo(3.5, 2, 5.4, 1.4); g.strokeStyle = '#2a160a'; g.lineWidth = 0.9; g.stroke();
  circle(g, 6.6, 3.2, 0.4); g.fillStyle = '#0a0604'; g.fill();
  return c;
}
/** Attack cooldown: a translucent cool grey veil over the selected item (the game draws it after the items, so the held
 *  weapon dims), hanging from the slot's top and shrinking upward as the swing recharges, a faint light line and the
 *  amber edge at its bottom (game: CompactHud.attackVeil, 1 px lines). */
function ccAttackShade(g, s, [sx, sy]) {
  if (s.attack == null || s.attack >= 1) return;
  const h = Math.round(16 * (1 - s.attack)), x = sx + 1, y = sy + 1;
  if (h <= 0) return;
  g.fillStyle = 'rgba(20,22,28,0.6)'; g.fillRect(x, y, 16, h);
  if (h >= 2) { g.fillStyle = 'rgba(220,230,240,0.2)'; g.fillRect(x, y + h - 2, 16, 1); }
  g.fillStyle = 'rgba(255,190,90,0.8)'; g.fillRect(x, y + h - 1, 16, 1);
}

/** Absorption: gold segments from the health's end, as far as they fit (the number gives the rest). */
function ccAbsorb(g, s, [x, y, w, h], max) {
  if (!s.absorb) return;
  const a0 = (w - 1) * Math.min(1, s.health / max), a1 = (w - 1) * Math.min(1, (s.health + s.absorb) / max);
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.clip();
  g.fillStyle = vgrad(g, y + 0.5, y + h - 0.5, [[0, LIQUID.absorb[0]], [0.45, LIQUID.absorb[1]], [1, LIQUID.absorb[2]]]);
  g.fillRect(x + 0.5 + a0, y, a1 - a0, h);
  ccMeterTexture(g, x + 0.5 + a0, y, a1 - a0, h, LIQUID.absorb, 'cloud');
  g.fillStyle = 'rgba(0,0,0,0.55)';
  for (let k = 1; k < 10; k++) g.fillRect(x + 0.5 + (w - 1) * k / 10 - 0.15, y, 0.3, h);
  g.restore();
}
/** AppleSkin's health preview: what the held food would regenerate, pale and pulsing past the health. */
function ccHealthPreview(g, s, [x, y, w, h], max) {
  if (!s.holdFood || !s.holdHeal) return;
  const from = (w - 1) * Math.min(1, s.health / max), to = (w - 1) * Math.min(1, (s.health + s.holdHeal) / max);
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.clip();
  g.fillStyle = `rgba(255,200,200,${0.3 + 0.25 * Math.sin(s.t / 160)})`; g.fillRect(x + 0.5 + from, y, to - from, h);
  g.restore();
}
/** AppleSkin's exhaustion (0 to 4, a food point lost at 4), bar mode: a slim solid strip directly under the food
 *  meter (the old hairline's height, the meter's length, crisp edges, a dim muted tan on a dark track), full at 0 and
 *  draining from the right end as exhaustion rises (as food empties), refilling smoothly when the game takes the point
 *  (s.exhShown eases). The game draws the same strip with fills (CC_EXH_STRIP). */
const CC_EXH_STRIP = { gap: 0.5, h: 0.6, track: '#15120f', fill: '#8a7458' };
function ccExhaustion(g, s, [x, y, w, h]) {
  if (s.exhaustion == null) return;
  const frac = 1 - Math.min(1, (s.exhShown ?? s.exhaustion) / 4), sy = y + h + CC_EXH_STRIP.gap;
  g.fillStyle = CC_EXH_STRIP.track; g.fillRect(x, sy, w, CC_EXH_STRIP.h);
  if (frac > 0.001) { g.fillStyle = CC_EXH_STRIP.fill; g.fillRect(x, sy, w * frac, CC_EXH_STRIP.h); }
}
/** The mount icon for a camel: a head on a long neck, sandy. 9 x 9. */
function ccCamel() {
  const [c, g] = hires(9, 9);
  const head = () => {
    g.beginPath(); g.moveTo(2.4, 8.6); g.quadraticCurveTo(2.2, 4.6, 4, 2.6); g.quadraticCurveTo(5.2, 1.2, 7, 1.6);
    g.quadraticCurveTo(8.6, 2.1, 8.4, 3.3); g.quadraticCurveTo(7.4, 3.8, 6, 3.8); g.quadraticCurveTo(4.6, 5.6, 5, 8.6); g.closePath();
  };
  head(); g.fillStyle = vgrad(g, 1.2, 8.6, [[0, '#e8c48a'], [0.5, '#b88a50'], [1, '#6a4a24']]); g.fill();
  g.strokeStyle = 'rgba(0,0,0,0.6)'; g.lineWidth = 0.35; g.stroke();
  circle(g, 6.2, 2.4, 0.35); g.fillStyle = '#1a1006'; g.fill();
  return c;
}
/** The game's text above the bar, where vanilla draws it: the held item's name (bottom - 59, 14 lower when the player
 *  cannot be hurt; it fades after about 2 s) and the action bar message (bottom - 68). Shown for spacing only. */
function ccGameText(g, s, font) {
  if (!font) return;
  const cx = (t) => -mcWidth(font, t) / 2;
  if (s.itemName && s.t - (s.nameAt || -1e9) < 2000) {
    const y = BOTTOM - 59 + (s.creative || s.spectator ? 14 : 0);
    mcText(g, font, s.itemName, cx(s.itemName), y, 1, '#ffffff', true);
  }
  if (s.actionBar) mcText(g, font, s.actionBar, cx(s.actionBar), BOTTOM - 68, 1, '#ffffff', true);
}
