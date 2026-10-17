// Vanilla layout: hearts, drumsticks and breathing tanks in the style of the HUD redesign's shields and bubbles
// (sprites.js armor, air): a bright fill with a lit left facet, a thin dark outline, a light rim and a glint, low in the
// 9 x 9 cell. Empty: a dark silhouette with a thin edge; half: the empty icon with one side filled over it (hearts the
// left, food the right, as vanilla's rows empty; tanks the lower half). Looks for the game's states (effects,
// absorption, Hunger) and overlays (saturation outline, glows). Every icon is cached.
// Paths from sprites.js (heartPath, meatPath, bone).

const VAN_ICONS = new Map();
/** A cached icon: key, and draw(g) on a fresh 9 x 9 hires canvas. */
function vanIcon(key, draw) {
  if (!VAN_ICONS.has(key)) { const [c, g] = hires(9, 9); draw(g); VAN_ICONS.set(key, c); }
  return VAN_ICONS.get(key);
}
const VAN_HEART = ['#ff9a90', '#e0313c', '#7c0d16'];
const vanHeartShape = g => { g.save(); g.translate(4.5, 2.6); g.scale(0.72, 0.72); g.translate(-4.5, -0.4); heartPath(g); g.restore(); };
const vanMeatFit = g => { g.translate(4.5, 1.9); g.scale(0.84, 0.84); g.translate(-4.35, -0.6); };   // drumsticks at 0.84: the hearts' weight
const vanMeatShape = g => { g.save(); vanMeatFit(g); meatPath(g); g.restore(); };
/** A heart: kind 'full', 'half' or 'empty'; look { fill: [light, mid, dark] (effects, absorption, the mount) }. */
function vanHeart(kind, look = {}) {
  const fill = look.fill || VAN_HEART;
  return vanIcon(`heart:${kind}:${fill}`, g => {
    const top = 2.6, bot = 8.2, edge = '#5a3c40', shape = () => vanHeartShape(g);
    shape(); g.fillStyle = 'rgba(12,8,8,0.6)'; g.fill();
    if (kind !== 'full') { shape(); g.strokeStyle = edge; g.lineWidth = 0.35; g.stroke(); }   // empty (half: the missing side)
    if (kind === 'empty') return;
    g.save();
    if (kind === 'half') { g.beginPath(); g.rect(0, 0, 4.5, 9); g.clip(); }
    shape(); g.fillStyle = vgrad(g, top, bot, [[0, fill[0]], [0.45, fill[1]], [1, fill[2]]]); g.fill();
    g.save(); shape(); g.clip(); g.fillStyle = 'rgba(255,255,255,0.16)'; g.fillRect(0, 0, 4.5, 9); g.restore();   // lit facet
    g.strokeStyle = 'rgba(255,255,255,0.85)'; g.lineWidth = 0.35;                                               // glint on the left lobe
    g.beginPath(); g.arc(3.3, 4.2, 0.9, Math.PI * 1.05, Math.PI * 1.5); g.stroke();
    shape(); g.strokeStyle = 'rgba(10,11,12,0.7)'; g.lineWidth = 0.3; g.stroke();
    rim(g, shape, top, bot, mixHex(fill[0], '#ffffff', 0.6), 0.3);
    g.restore();
  });
}
/** A drumstick: kind 'full', 'half' (the right side) or 'empty'; meat: [light, mid, dark] (the Hunger effect spoils it). */
function vanFood(kind, meat = ['#f2a060', '#c2622a', '#5a2208']) {
  return vanIcon(`food:${kind}:${meat}`, g => {
    const top = 1.9, shape = () => vanMeatShape(g);
    if (kind !== 'full') {   // empty (half: the missing side)
      g.save(); vanMeatFit(g); bone(g, '#3a3128'); g.restore();
      shape(); g.fillStyle = 'rgba(12,10,8,0.6)'; g.fill(); g.strokeStyle = '#5a4c3e'; g.lineWidth = 0.35; g.stroke();
    }
    if (kind === 'empty') return;
    g.save();
    if (kind === 'half') { g.beginPath(); g.rect(4.5, 0, 4.5, 9); g.clip(); }
    g.save(); vanMeatFit(g); bone(g, vgrad(g, 5, 8.6, [[0, '#fbf4e4'], [0.5, '#d6c8a8'], [1, '#7a6c52']])); g.restore();
    shape(); g.fillStyle = vgrad(g, top, 7, [[0, meat[0]], [0.5, meat[1]], [1, meat[2]]]); g.fill();
    g.save(); shape(); g.clip(); g.fillStyle = 'rgba(255,255,255,0.16)'; g.fillRect(0, 0, 5.4, 9); g.restore();   // lit facet
    g.strokeStyle = 'rgba(255,235,210,0.85)'; g.lineWidth = 0.35;                                               // glaze glint
    g.beginPath(); g.arc(5.6, 4, 1.5, Math.PI * 1.1, Math.PI * 1.55); g.stroke();
    shape(); g.strokeStyle = 'rgba(10,11,12,0.7)'; g.lineWidth = 0.3; g.stroke();
    rim(g, shape, top, 7, mixHex(meat[0], '#ffffff', 0.55), 0.3);
    g.restore();
  });
}
/** AppleSkin's saturation on a drumstick: a cream outline around the meat (kind 'full', or 'half': the right side). */
function vanSatRing(kind) {
  return vanIcon(`sat:${kind}`, g => {
    if (kind === 'half') { g.beginPath(); g.rect(4.5, 0, 4.5, 9); g.clip(); }
    vanMeatShape(g); g.strokeStyle = '#fff0c8'; g.lineWidth = 0.55; g.stroke();
  });
}
/** A flat silhouette for glows and pulses: shape 'heart' or 'food', side 'full', 'left' or 'right', colour col. */
function vanGlow(shape, side, col) {
  return vanIcon(`glow:${shape}:${side}:${col}`, g => {
    if (side !== 'full') { g.beginPath(); g.rect(side === 'left' ? 0 : 4.5, 0, 4.5, 9); g.clip(); }
    if (shape === 'heart') vanHeartShape(g); else vanMeatShape(g);
    g.fillStyle = col; g.fill();
  });
}
/** A soft halo around an icon (shape 'heart' or 'food', side 'full', 'left' or 'right'): its outline in col, blurred
 *  outward, with a light wash of col over the icon (Nourishment's gold on the saturated drumsticks). */
function vanHalo(shape, side, col) {
  return vanIcon(`halo:${shape}:${side}:${col}`, g => {
    if (side !== 'full') { g.beginPath(); g.rect(side === 'left' ? 0 : 4.5, 0, 4.5, 9); g.clip(); }
    const path = () => shape === 'heart' ? vanHeartShape(g) : vanMeatShape(g);
    g.save(); g.shadowColor = col; g.shadowBlur = 1.4 * R; path(); g.strokeStyle = col; g.lineWidth = 0.6; g.stroke(); g.restore();
    g.globalAlpha = 0.3; path(); g.fillStyle = col; g.fill();
  });
}
/** A breathing tank (Create's copper backtank, the breath row while it supplies air): kind 'full', 'half' (filled in its
 *  lower half, over the empty icon) or 'empty'. A copper capsule with a steel valve. */
function vanTank(kind) {
  return vanIcon(`tank:${kind}`, g => {
    const top = 3.1, bot = 8.5, body = () => rrect(g, 2.4, top, 4.2, bot - top, 1.5);
    const valve = col => { rrect(g, 3.6, 2, 1.8, 1.4, 0.3); g.fillStyle = col; g.fill(); };
    if (kind !== 'full') {   // empty (half: the drained top)
      valve('#3a3128'); body(); g.fillStyle = 'rgba(12,10,8,0.6)'; g.fill(); g.strokeStyle = '#5a3a28'; g.lineWidth = 0.35; g.stroke();
    }
    if (kind === 'empty') return;
    g.save();
    if (kind === 'half') { g.beginPath(); g.rect(0, (top + bot) / 2, 9, 9); g.clip(); }
    else valve(steel(g, 2, 3.4));
    body(); g.fillStyle = vgrad(g, top, bot, [[0, '#ffc89a'], [0.45, '#c9783e'], [1, '#5a2410']]); g.fill();
    g.save(); body(); g.clip(); g.fillStyle = 'rgba(255,255,255,0.16)'; g.fillRect(0, 0, 4.5, 9);   // lit facet
    g.fillStyle = 'rgba(40,14,4,0.7)'; g.fillRect(2.4, 5.6, 4.2, 0.4); g.restore();                   // the tank's seam band
    g.strokeStyle = 'rgba(255,240,225,0.85)'; g.lineWidth = 0.35;                                       // glint
    g.beginPath(); g.moveTo(3.3, 4.1); g.lineTo(3.3, 5.1); g.stroke();
    body(); g.strokeStyle = 'rgba(10,11,12,0.7)'; g.lineWidth = 0.3; g.stroke();
    rim(g, body, top, bot, '#ffe0c0', 0.3);
    g.restore();
  });
}
