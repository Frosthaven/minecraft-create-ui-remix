// Compact HUD statuses: every state the game shows around the hotbar, mapped onto the compact action bar (preview).
// Uses sprites.js, meters.js (LIQUID), hud-compact.js (ccMeter). s.t is the time in ms (animations).
//   Hearts: poison / wither / frozen recolour the health liquid; hardcore adds a crimson trim; at 4 health or less the
//     meter shakes; damage flashes it white; regeneration sweeps a light along it.
//   Food: the Hunger effect turns the broth spoiled olive; holding food shows what it would restore (AppleSkin), a
//     pale pulsing band past the fill.
//   Riding: the mount's health replaces food (vanilla hides food then); charging a jump turns the XP pipe into the
//     jump gauge (vanilla swaps the XP bar for the jump bar).
//   Casting (Iron's): a thin mana pipe over the sockets while a spell charges. Attack cooldown (hotbar mode): a shade
//   over the selected socket that lifts as the swing recharges. Creative: health, food, armor and air hide.

const CC_JUMP = ['#fff2a8', '#e0a020', '#5a3808'];
const CC_MOUNT = ['#f0c890', '#a8682e', '#3e200a'];

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
  const since = s.t - (s.hurtAt || -1e9);
  if (since < 400) { g.fillStyle = `rgba(255,255,255,${0.7 * (1 - since / 400)})`; g.fillRect(x, y, w, h); }
  if (s.regen) {
    const p = ((s.t / 1400) % 1) * (w + 8) - 4;
    g.fillStyle = g.createLinearGradient(x + p - 4, 0, x + p + 4, 0);
    g.fillStyle.addColorStop(0, 'rgba(255,255,255,0)'); g.fillStyle.addColorStop(0.5, 'rgba(255,230,220,0.6)'); g.fillStyle.addColorStop(1, 'rgba(255,255,255,0)');
    g.fillRect(x + p - 4, y, 8, h);
  }
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
/** Iron's cast progress: a thin glowing pipe across the sockets' top while a spell charges. */
function ccCastPipe(g, s) {
  if (s.cast == null) return;
  ccMeter(g, -84, 25.6, 168, 2, s.cast, MANA.irons, 0);
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
