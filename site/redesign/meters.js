// Health, food and air as meters: each 9 x 9 cell is one segment of a steel pipe with a glass window, in the
// experience pipe's style, holding a glowing liquid (blood, broth, air).
// Joints: cells sit 8 px apart (columns 8..9 of a cell overlap 0..1 of its right neighbour), and the game draws
// hearts, food and air right to left, so each cell's right edge always lies over its neighbour. A cell's content
// is x 0.5 to 8.5; its right edge (8.5..9) is the separator, 0.5 wide, a darker shade of the liquid on that side
// (dark grey where empty); its left edge (0..0.5) shows only at the row's left end. Corners are rounded 0.5, the
// most that stays hidden at joints, so only the row's ends are rounded (the end caps). Every sprite draws its whole
// segment, so full or half sprites over the container, or air with no container, look the same.
// Rows: every meter is the same pipe, 3 to 8, window 4 to 7. Under the food pipe, AppleSkin's exhaustion meter is a bare
// line (rows 8 to 9, 1 px above the experience bar: the game's boxes leave that gap), no chrome. The pipe is plain
// shaded steel, no texture: a texture repeats every 8 px and reads as tiles. Game behaviour: hearts shake 1 px each at 4 health or less, and
// Regeneration lifts one heart 2 px in a wave; food shakes when saturation is out. Segments part for a moment.
const MT = { top: 3, bot: 8, win: [4, 7], line: [8, 9], r: 0.5 };

// Liquids: light top, mid, dark bottom (the experience pipe's lighting).
const LIQUID = {
  blood: ['#ff9a8a', '#c8202a', '#4a0610'], poison: ['#e4ec80', '#8a9a20', '#2a3408'], wither: ['#9a8e98', '#544a52', '#1e181c'],
  frozen: ['#e4f8ff', '#7ab8e0', '#2a4a6a'], absorb: ['#fff0a0', '#e0b030', '#6a4a08'],
  broth: ['#ffd08a', '#d07a2a', '#5a2a08'], hunger: ['#b4a274', '#6e6236', '#2a2410']   /* spoiled: muddy olive-brown, apart from poison's lime */, air: ['#e4fbff', '#5ec8e8', '#0a4a6a'],
};

/** The colour between two #rrggbb colours (t from a to b). */
function mixHex(a, b, t = 0.5) {
  const p = h => [1, 3, 5].map(i => parseInt(h.slice(i, i + 2), 16));
  const [x, y] = [p(a), p(b)];
  return '#' + x.map((v, i) => Math.round(v + (y[i] - v) * t).toString(16).padStart(2, '0')).join('');
}
const meterBody = g => rrect(g, 0, MT.top, 9, MT.bot - MT.top, MT.r);
/** Liquid in the window rows w0..w1 from x0 to x1: flat-ish (the ends pulled toward the middle tone), a faint core line. */
function meterFill(g, x0, x1, w0, w1, col) {
  g.fillStyle = vgrad(g, w0, w1, [[0, mixHex(col[0], col[1], 0.55)], [0.45, col[1]], [1, mixHex(col[1], col[2], 0.45)]]);
  g.fillRect(x0, w0, x1 - x0, w1 - w0);
  g.fillStyle = 'rgba(255,255,255,0.14)'; g.fillRect(x0, w0 + 0.5, x1 - x0, 0.5);
}
const meterDark = col => mixHex(col[1], col[2], 0.55);   // separators: a darker shade of the liquid
const lighter = col => [mixHex(col[0], '#ffffff', 0.5), mixHex(col[1], '#fff0c8', 0.5), mixHex(col[2], col[1], 0.6)];

/** One segment: fill 'full', 'left' or 'right' (half), or null (empty); a liquid; options: hardcore (a rust-tinted pipe), blink (the white flash after damage), bubbles (air), fizz (air bursting: bubbles only). */
function meterSegment(fill, liquid, opt = {}) {
  const [c, g] = hires(9, 9), col = LIQUID[liquid], [w0, w1] = MT.win, bot = MT.bot;
  const steel = opt.hardcore ? ['#7a625e', '#4e3c39', '#302523', '#1a1312'] : ['#6a7076', '#46494e', '#2c2f33', '#18191b'];   // hardcore: rust-tinted
  meterBody(g); g.fillStyle = vgrad(g, MT.top, bot, [[0, steel[0]], [0.25, steel[1]], [0.75, steel[2]], [1, steel[3]]]); g.fill();
  g.fillStyle = '#050605'; g.fillRect(0.5, w0, 8, w1 - w0);
  const span = { full: [0.5, 8.5], left: [0.5, 4.5], right: [4.5, 8.5] }[fill];
  if (span) meterFill(g, span[0], span[1], w0, w1, col);
  // Separators: a darker shade of the liquid on that side, dark grey where empty.
  const dark = meterDark(col), empty = '#1e2023';
  g.fillStyle = fill === 'full' || fill === 'left' ? dark : empty; g.fillRect(0, w0, 0.5, w1 - w0);
  g.fillStyle = fill === 'full' || fill === 'right' ? dark : empty; g.fillRect(8.5, w0, 0.5, w1 - w0);
  if (opt.bubbles || opt.fizz) {
    g.fillStyle = 'rgba(240,252,255,0.85)';
    for (const [x, y, r] of opt.fizz ? [[2.5, 5.9, 0.5], [4.5, 4.9, 0.55], [6.3, 6, 0.45]] : [[3, 6, 0.4], [6, 5, 0.35]]) { circle(g, x, y, r); g.fill(); }
  }
  if (opt.blink) { meterBody(g); g.fillStyle = 'rgba(255,255,255,0.4)'; g.fill(); }
  return c;
}

/** AppleSkin's pieces for the meters: saturation as a lighter broth over the full window height, filling from the
 *  right (a quarter to all of the segment), with its own darker separator on the right edge; drawn over the food
 *  (right to left too), so it leaves the left edge (0..0.5) alone, which lies over the next segment. The hunger
 *  version is lighter hunger broth. Exhaustion grows from the right, cut from this fixed band: two flat tones, the
 *  top half black and the bottom half white. */
function meterAppleskin() {
  const glaze = (f, liquid) => {
    const [c, g] = hires(9, 9), x0 = 0.5 + 8 * (1 - f), col = lighter(LIQUID[liquid]), [w0, w1] = MT.win;
    meterFill(g, x0, 8.5, w0, w1, col);
    g.fillStyle = meterDark(col); g.fillRect(8.5, w0, 0.5, w1 - w0);
    return c;
  };
  // Exactly two tones, split: the top half black, the bottom half white (each 0.5: thinner would vanish at GUI scale 2).
  const [c, g] = hires(81, 9), [l0, l1] = MT.line, mid = (l0 + l1) / 2;
  g.fillStyle = '#000000'; g.fillRect(0, l0, 81, mid - l0);
  g.fillStyle = '#ffffff'; g.fillRect(0, mid, 81, l1 - mid);
  return { sat: SAT_FRACS.map(f => glaze(f, 'broth')), hunger: SAT_FRACS.map(f => glaze(f, 'hunger')), exhaustion: c };
}

/** The meter set: the sprites the HUD page shows, plus every heart variant the game picks (for the pack). */
function meterSprites() {
  const set = {
    heartContainer: meterSegment(null, 'blood'), heartFull: meterSegment('full', 'blood'), heartHalf: meterSegment('left', 'blood'),
    foodEmpty: meterSegment(null, 'broth'), foodFull: meterSegment('full', 'broth'),
    foodHalf: meterSegment('right', 'broth'),
    air: air(false), airBurst: air(true),   // air stays an icon (sprites.js), in the shields' style
    as: meterAppleskin(), variants: {},
  };
  const v = set.variants;
  for (const hc of ['', 'hardcore_']) for (const b of ['', '_blinking']) {
    v[`hud/heart/container${hc ? '_hardcore' : ''}${b}`] = meterSegment(null, 'blood', { blink: !!b, hardcore: !!hc });
    for (const [type, liquid] of [['', 'blood'], ['poisoned_', 'poison'], ['withered_', 'wither'], ['frozen_', 'frozen'], ['absorbing_', 'absorb']]) {
      v[`hud/heart/${type}${hc}full${b}`] = meterSegment('full', liquid, { blink: !!b, hardcore: !!hc });
      v[`hud/heart/${type}${hc}half${b}`] = meterSegment('left', liquid, { blink: !!b, hardcore: !!hc });
    }
  }
  for (const k of ['empty', 'full', 'half']) v[`hud/food_${k}_hunger`] = meterSegment({ empty: null, full: 'full', half: 'right' }[k], 'hunger');
  // Mount health replaces the food row while riding (drawn right to left there too).
  Object.assign(v, { 'hud/heart/vehicle_container': meterSegment(null, 'blood'), 'hud/heart/vehicle_full': meterSegment('full', 'blood'),
    'hud/heart/vehicle_half': meterSegment('right', 'blood') });   // that row fills from the right, as food
  return set;
}
