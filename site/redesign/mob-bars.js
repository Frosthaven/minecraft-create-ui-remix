// Floating mob health bars: a flat, dim bar above each mob within 10 blocks, its colour by the mob's kind. This page
// draws a night scene (a zombie from the vanilla skin, in an orthographic 3/4 view) to judge the bar in context.
// Uses sprites.js (vgrad), meters.js (LIQUID, mixHex).

/** The kinds, each with its bar colour: dimmed and desaturated so the bars read at night without glaring.
 *  on: the default config. */
const MOB_KINDS = {
  hostile: { name: 'Hostile', col: '#a83a3a', on: true, test: 'Enemy', ex: 'zombies, skeletons, creepers, slimes, bosses' },
  friendly: { name: 'Not hostile', col: '#5c9a46', on: true, test: 'every other mob', ex: 'animals, villagers, pets, golems, wolves, endermen' },
  player: { name: 'Friendly players', col: '#4a76b8', on: true, test: 'Player', ex: 'players who have not hit a player in 30 s' },
  attacker: { name: 'Hostile players', col: '#a8409a', on: true, test: 'hit any player in the last 30 s', ex: 'any PvP, you or anyone near; shown even with friendly players off' },
};
const MOB_BAR = { w: 14, h: 1.4, lift: 6, back: 'rgba(10,11,14,0.62)', edge: 'rgba(0,0,0,0.7)' };   // model px (16 = a block)

// --- An orthographic 3/4 camera over model space (x right, y up, z toward the viewer; 16 units = a block). ---
const CAM = { yaw: -32 * Math.PI / 180, pitch: 14 * Math.PI / 180, s: 6.5, ox: 430, oy: 400 };
function project([x, y, z]) {
  const xr = x * Math.cos(CAM.yaw) + z * Math.sin(CAM.yaw), zr = -x * Math.sin(CAM.yaw) + z * Math.cos(CAM.yaw);
  return { x: CAM.ox + xr * CAM.s, y: CAM.oy + (-y * Math.cos(CAM.pitch) + zr * Math.sin(CAM.pitch)) * CAM.s,
           depth: zr * Math.cos(CAM.pitch) + y * Math.sin(CAM.pitch) };
}
const add = (a, b, k = 1) => a.map((v, i) => v + b[i] * k);
/** Rotation about the x axis through a pivot (an arm raised forward). */
const rotX = (p, pivot, a) => {
  const y = p[1] - pivot[1], z = p[2] - pivot[2];
  return [p[0], pivot[1] + y * Math.cos(a) - z * Math.sin(a), pivot[2] + y * Math.sin(a) + z * Math.cos(a)];
};

/** A skin box's visible faces: [x0,y0,z0] its low corner, [w,h,d] its size, [u,v] its skin offset (vanilla layout). */
function boxFaces(img, [x0, y0, z0], [w, h, d], [u, v], rot = p => p) {
  const x1 = x0 + w, y1 = y0 + h, z1 = z0 + d;
  // [corner at the texture's top left, its u direction, its v direction, texture rect, light]
  const F = [
    [[x0, y1, z1], [1, 0, 0], [0, -1, 0], [u + d, v + d, w, h], 0.8],            // front (+z)
    [[x1, y1, z0], [-1, 0, 0], [0, -1, 0], [u + 2 * d + w, v + d, w, h], 0.8],   // back
    [[x0, y1, z0], [0, 0, 1], [0, -1, 0], [u, v + d, d, h], 0.6],                // the mob's right (-x)
    [[x1, y1, z1], [0, 0, -1], [0, -1, 0], [u + d + w, v + d, d, h], 0.6],       // the mob's left (+x)
    [[x0, y1, z0], [1, 0, 0], [0, 0, 1], [u + d, v, w, d], 1],                   // top
  ];
  const out = [];
  for (const [p0, du, dv, t, light] of F) {
    const [a, b, c] = [p0, add(p0, du, t[2]), add(p0, dv, t[3])].map(rot);
    const n = cross(sub(c, a), sub(b, a));   // v x u points out of the face
    const pa = project(a), pb = project(b), pc = project(c), pn = project(add(a, n)), facing = pn.depth - pa.depth;
    if (facing <= 0) continue;
    const centre = project(add(a, add(sub(b, a), sub(c, a)), 0.5));
    out.push({ img, t, pa, pb, pc, depth: centre.depth, light });
  }
  return out;
}
const sub = (a, b) => a.map((v, i) => v - b[i]);
const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];

/** Draws a face: the texture rect mapped onto the projected parallelogram (exact for an orthographic view), shaded. */
function drawFace(g, f) {
  const [sx, sy, sw, sh] = f.t;
  g.save();
  g.setTransform((f.pb.x - f.pa.x) / sw, (f.pb.y - f.pa.y) / sw, (f.pc.x - f.pa.x) / sh, (f.pc.y - f.pa.y) / sh, f.pa.x, f.pa.y);
  g.imageSmoothingEnabled = false;
  const e = 0.06;   // a hair of overlap hides the seams between faces
  g.drawImage(f.img, sx, sy, sw, sh, -e, -e, sw + 2 * e, sh + 2 * e);
  g.globalCompositeOperation = 'source-atop';
  g.fillStyle = `rgba(0,0,0,${1 - f.light})`; g.fillRect(-e, -e, sw + 2 * e, sh + 2 * e);
  g.restore();
}

/** The zombie (vanilla humanoid model, arms raised forward), as faces. */
function zombieFaces(skin) {
  const arm = a => p => rotX(p, [0, 22, 0], a);
  return [
    ...boxFaces(skin, [-4, 0, -2], [4, 12, 4], [0, 16]),     // right leg
    ...boxFaces(skin, [0, 0, -2], [4, 12, 4], [16, 48]),     // left leg
    ...boxFaces(skin, [-4, 12, -2], [8, 12, 4], [16, 16]),   // body
    ...boxFaces(skin, [-4, 24, -4], [8, 8, 8], [0, 0]),      // head
    ...boxFaces(skin, [-8, 12, -2], [4, 12, 4], [40, 16], arm(-Math.PI * 0.47)),   // right arm
    ...boxFaces(skin, [4, 12, -2], [4, 12, 4], [32, 48], arm(-Math.PI * 0.5)),     // left arm
  ];
}

/** Grass: the grey top tinted with plains' grass colour (as the game tints it). */
function tintedGrass(img) {
  const c = document.createElement('canvas'); c.width = c.height = 16;
  const g = c.getContext('2d');
  g.drawImage(img, 0, 0); g.globalCompositeOperation = 'multiply'; g.fillStyle = '#91bd59'; g.fillRect(0, 0, 16, 16);
  g.globalCompositeOperation = 'destination-in'; g.drawImage(img, 0, 0);
  return c;
}

/** The night scene: sky, moon and stars, then the lit world (ground and mob) darkened to night, then the bar. */
function mobScene(canvas, { skin, grass, kind, hp, max, trail }) {
  const g = canvas.getContext('2d'), W = canvas.width, H = canvas.height;
  g.fillStyle = vgrad(g, 0, H, [[0, '#04060d'], [0.55, '#0c1226'], [1, '#141c36']]); g.fillRect(0, 0, W, H);
  for (let i = 0; i < 70; i++) {   // stars: fixed points, a few brighter
    const h = Math.sin(i * 91.7) * 43758.5453, u = h - Math.floor(h), v = h * 3.7 - Math.floor(h * 3.7);
    g.fillStyle = i % 6 ? 'rgba(200,210,235,0.55)' : 'rgba(240,245,255,0.9)'; g.fillRect(u * W, v * H * 0.55, i % 6 ? 2 : 3, i % 6 ? 2 : 3);
  }
  g.fillStyle = 'rgba(230,236,250,0.08)'; g.fillRect(W - 190, 40, 96, 96);   // the moon, square as in the game
  g.fillStyle = '#e6ecf6'; g.fillRect(W - 170, 60, 56, 56);
  g.fillStyle = '#c8d0de'; g.fillRect(W - 160, 72, 14, 10); g.fillRect(W - 138, 92, 10, 12);

  // The world, lit, on its own layer; then darkened to moonlight (a blue multiply) and laid over the sky.
  const wc = document.createElement('canvas'); wc.width = W; wc.height = H;
  const w = wc.getContext('2d');
  // The ground: one plane filled with the grass texture as a repeating pattern (tiles would leave seams).
  const [gx0, gz0, gw, gd] = [-14 * 16, -9 * 16, 28 * 16, 18 * 16];
  const p0 = project([gx0, 0, gz0]), pu = project([gx0 + 1, 0, gz0]), pv = project([gx0, 0, gz0 + 1]);
  w.save(); w.setTransform(pu.x - p0.x, pu.y - p0.y, pv.x - p0.x, pv.y - p0.y, p0.x, p0.y);
  w.imageSmoothingEnabled = false; w.fillStyle = w.createPattern(grass, 'repeat'); w.fillRect(0, 0, gw, gd); w.restore();
  const far = project([0, 0, -9 * 16]).y;   // distant ground fades into the night, hiding its far edge
  w.fillStyle = vgrad(w, 0, H, [[far / H, 'rgba(12,18,38,1)'], [Math.min(1, (far + 140) / H), 'rgba(12,18,38,0)']]);
  w.globalCompositeOperation = 'source-atop'; w.fillRect(0, 0, W, H); w.globalCompositeOperation = 'source-over';
  zombieFaces(skin).sort((a, b) => a.depth - b.depth).forEach(f => drawFace(w, f));
  const night = document.createElement('canvas'); night.width = W; night.height = H;
  const n = night.getContext('2d');
  n.drawImage(wc, 0, 0); n.globalCompositeOperation = 'multiply'; n.fillStyle = '#4a5478'; n.fillRect(0, 0, W, H);
  n.globalCompositeOperation = 'destination-in'; n.drawImage(wc, 0, 0);
  g.drawImage(night, 0, 0);

  // The bar faces the camera, half a block above the head (where the game puts name tags).
  const top = project([0, 32 + MOB_BAR.lift, 0]);
  mobBar(g, top.x, top.y, MOB_BAR.w * CAM.s, MOB_BAR.h * CAM.s, kind, hp, max, trail);
}

/** The bar: a dim backing, the health in the kind's flat colour, recent damage as a trail of the same colour at half brightness (mixed with black) that drains away. */
function mobBar(g, cx, cy, w, h, kind, hp, max, trail = hp) {
  const x = Math.round(cx - w / 2), y = Math.round(cy - h / 2), col = MOB_KINDS[kind].col;
  g.fillStyle = MOB_BAR.edge; g.fillRect(x - 1, y - 1, w + 2, h + 2);
  g.fillStyle = MOB_BAR.back; g.fillRect(x, y, w, h);
  const fw = w * Math.max(0, hp) / max, tw = w * Math.max(0, trail) / max;
  if (tw > fw) { g.fillStyle = mixHex(col, '#000000', 0.5); g.fillRect(x + fw, y, tw - fw, h); }
  g.fillStyle = col; g.fillRect(x, y, fw, h);
  g.fillStyle = 'rgba(255,255,255,0.08)'; g.fillRect(x, y, fw, 1);   // one faint top line, so the flat fill still reads as a bar
}
