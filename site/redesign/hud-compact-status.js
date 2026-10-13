// Compact HUD statuses: every state the game shows around the hotbar, mapped onto the compact action bar (preview).
// Uses sprites.js, meters.js (LIQUID), hud-compact.js (ccMeter). s.t is the time in ms (animations).
//   Hearts: poison / wither / frozen recolour the health liquid; hardcore adds a crimson trim; at 4 health or less the
//     meter shakes; damage flashes it white; regeneration sweeps a light along it.
//   Food: saturation (vanilla's yellow outline, AppleSkin's overlay) fills the first segments, up to its level (never
//     past the food), in light cream against the orange broth; the Hunger effect turns the broth spoiled olive; holding food shows
//     what it would restore (AppleSkin), a pale pulsing band past the fill.
//   The heart icon follows the effect (poisoned, withered, frozen), as vanilla's hearts do.
//   Riding: the mount's health replaces food (vanilla hides food then). The XP pipe is shared: a charging jump, a
//     camel's dash cooldown and a charging spell (Iron's) take it over, the level window showing their icon.
//   Attack cooldown (hotbar mode): a shade
//   over the selected socket that lifts as the swing recharges. Creative: health, food, armor and air hide.

const CC_JUMP = ['#fff2a8', '#e0a020', '#5a3808'];
const CC_MOUNT = ['#b8ffe0', '#2fbf8a', '#0b4a34'];   // the mount's health: emerald, clearly apart from food's orange
const CC_COOLDOWN = ['#b8bcc2', '#6a6e74', '#26282b'];

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
/** Effects over the health meter at box: hardcore trim, damage flash, regeneration sweep. */
function ccHealthFx(g, s, [x, y, w, h]) {
  if (s.hardcore) { rrect(g, x - 0.3, y - 0.3, w + 0.6, h + 0.6, h / 2 + 0.3); g.strokeStyle = '#8a1018'; g.lineWidth = 0.6; g.stroke(); }
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.clip();
  // The orbs' effects, laid along the meter: the damage flash red, Regeneration's pink motes drifting along the
  // liquid, and at 4 health or less the empty glass pulsing red (in sync with low food) instead of a shake.
  const fill = (w - 1) * Math.max(0, Math.min(1, s.health / (s.maxHealth || 20)));
  const since = s.t - (s.hurtAt || -1e9);
  if (since < 400) { g.fillStyle = `rgba(255,40,40,${0.55 * (1 - since / 400)})`; g.fillRect(x, y, w, h); }
  if (s.health <= 4) { g.fillStyle = `rgba(200,20,30,${ccWarn(s)})`; g.fillRect(x + 0.5 + fill, y, w - 1 - fill, h); }
  if (s.regen && fill > 0) {   // Regeneration (the meter is too thin for the orb's motes): a bright pink band sweeping
    // along the health toward its end, and a soft pink glow at the leading edge where health is added.
    const bx = x + 0.5 + ((s.t / 1100) % 1) * (fill + 8) - 4;
    const gr = g.createLinearGradient(bx - 4, 0, bx + 4, 0);
    gr.addColorStop(0, 'rgba(255,120,175,0)'); gr.addColorStop(0.5, 'rgba(255,150,195,0.85)'); gr.addColorStop(1, 'rgba(255,120,175,0)');
    g.save(); g.beginPath(); g.rect(x + 0.5, y, fill, h); g.clip(); g.fillStyle = gr; g.fillRect(bx - 4, y, 8, h); g.restore();
    const ex = x + 0.5 + fill, eg = g.createRadialGradient(ex, y + h / 2, 0, ex, y + h / 2, 3);
    eg.addColorStop(0, `rgba(255,140,190,${0.55 + 0.25 * Math.sin(s.t / 180)})`); eg.addColorStop(1, 'rgba(255,140,190,0)');
    g.fillStyle = eg; g.fillRect(ex - 3, y, 6, h);
  }
  g.restore();
}
/** The shared warning pulse (low health, low food): one timing so they flash in sync. */
function ccWarn(s) { return 0.12 + 0.12 * Math.sin(s.t / 220); }
/** The food meter's effects, as on the orb: Nourishment's gold glaze, surface line and sparkles over the broth, and at
 *  6 food or less the empty glass pulsing amber. */
function ccFoodFx(g, s, [x, y, w, h]) {
  const fill = (w - 1) * Math.max(0, Math.min(1, s.food / 20));
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.clip();
  if (s.food <= 6) { g.fillStyle = `rgba(220,120,20,${ccWarn(s)})`; g.fillRect(x + 0.5 + fill, y, w - 1 - fill, h); }
  if (s.nourished && fill > 0) {
    g.fillStyle = 'rgba(255,215,90,0.28)'; g.fillRect(x + 0.5, y, fill, h);
    for (let i = 0; i < 6; i++) {
      const hh = Math.sin((i + 3) * 47.3) * 43758.5453, u = hh - Math.floor(hh);
      g.fillStyle = `rgba(255,240,160,${0.45 + 0.45 * Math.sin(s.t / 260 + i * 1.7)})`; g.fillRect(x + 0.5 + u * (fill - 0.4), y + h * (0.25 + 0.5 * ((i * 0.37) % 1)), 0.4, 0.4);
    }
    g.fillStyle = 'rgba(255,220,110,0.95)'; g.fillRect(x + 0.5 + fill - 0.25, y, 0.5, h);   // the gold surface edge
  }
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
/** Attack cooldown (hotbar mode): a shade over the selected socket's well, lifting as the swing recharges. */
function ccAttackShade(g, s, [sx, sy]) {
  if (s.attack == null || s.attack >= 1) return;
  const h = 16 * (1 - s.attack);
  g.save(); rrect(g, sx + 1, sy + 1, 16, 16, 0.6); g.clip();
  g.fillStyle = 'rgba(0,0,0,0.55)'; g.fillRect(sx + 1, sy + 1, 16, h);
  g.fillStyle = 'rgba(255,190,90,0.8)'; g.fillRect(sx + 1, sy + 1 + h - 0.3, 16, 0.3);
  g.restore();
}

/** Absorption: gold segments from the health's end, as far as they fit (the number gives the rest). */
function ccAbsorb(g, s, [x, y, w, h], max) {
  if (!s.absorb) return;
  const a0 = (w - 1) * Math.min(1, s.health / max), a1 = (w - 1) * Math.min(1, (s.health + s.absorb) / max);
  g.save(); rrect(g, x + 0.5, y + 0.5, w - 1, h - 1, h / 2 - 0.5); g.clip();
  g.fillStyle = vgrad(g, y + 0.5, y + h - 0.5, [[0, LIQUID.absorb[0]], [0.45, LIQUID.absorb[1]], [1, LIQUID.absorb[2]]]);
  g.fillRect(x + 0.5 + a0, y, a1 - a0, h);
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
/** AppleSkin's exhaustion (0 to 4, a food point lost at 4): its own hairline track just under the food meter. */
function ccExhaustion(g, s, [x, y, w, h]) {
  if (s.exhaustion == null) return;
  const ly = y + h + 0.8, lw = w - 3;
  g.fillStyle = 'rgba(0,0,0,0.55)'; g.fillRect(x + 1.5, ly, lw, 0.5);
  g.fillStyle = 'rgba(240,235,225,0.85)'; g.fillRect(x + 1.5, ly, lw * Math.min(1, s.exhaustion / 4), 0.5);
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
