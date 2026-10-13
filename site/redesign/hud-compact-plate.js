// The compact action bar's plate (hud-compact.js draws the rest): one iron bar, the centre section and the two wings at
// their side size, their joints, the shared plate iron. Uses sprites.js, material.js, hud-compact.js (CC, CC_SIDE,
// ccSideY), hud-compact-orbs.js (ORB, ORB_H).

/** The action bar plate: one blackened iron bar. The centre section (the hotbar) runs to ±CC_SIDE, where each wing joins
 *  it: the wings are drawn at the side bars' size around that point, so the joint stays closed at any size, as a
 *  square step (the taller part's edge rises from the shorter, with a rim seam down the joint). The wings' outer ends
 *  slope from the top edge (at x0 / x1, past everything on the wing) down and out to the bar's bottom (CC_TAPER). */
const CC_TAPER = 8;
/** The plate's iron, a moderate step lighter than the generic iron (it read far too dark in daylight play); shared by
 *  the centre plate, the wings, the orb and dial housings and the joints, so they stay one casting. Sockets and wells
 *  keep their own dark fills. */
const CC_PLATE_IRON = ['#66584a', '#3f362d', '#2a231c', '#141010'];
const plateIron = (g, y0, y1) => vgrad(g, y0, y1, [[0, CC_PLATE_IRON[0]], [0.12, CC_PLATE_IRON[1]], [0.7, CC_PLATE_IRON[2]], [1, CC_PLATE_IRON[3]]]);
/** Where the plate's gradient starts: one value for the whole bar (orbs: the housings' crown; bars: the wings' top). */
const ccPlateGradTop = s => s.orbs ? ORB.y - ORB_H() : CC_WING.top;
function ccPlate(g, ears = {}) {
  const x0 = ears.x0 ?? CC.plate.x0, x1 = ears.x1 ?? CC.plate.x1;   // the wings' outer ends at their top edge (ccEnds)
  const { wing, mid, r } = CC.plate, b = BOTTOM, A = CC_SIDE, T = CC_TAPER;
  // A plate: a polygon [x, y, corner radius] with its shadow, iron and surface. Returns [path, gradient top].
  // skip: the index of the edge (pts[skip] to the next) that is a joint: no rim along it (the joint draws the rim
  // where the outlines meet), the rim path ending at that corner's arc.
  const plate = (pts, gradTop, skip = -1) => {
    const top = Math.min(...pts.map(q => q[1])), gtop = gradTop ?? top;
    const path = () => {
      const n = pts.length, m = (i, j) => [(pts[i][0] + pts[j][0]) / 2, (pts[i][1] + pts[j][1]) / 2];
      g.beginPath(); g.moveTo(...m(n - 1, 0));
      for (let i = 0; i < n; i++) {
        const [x, y, rad] = pts[i], nx = pts[(i + 1) % n];
        if (rad > 0) g.arcTo(x, y, nx[0], nx[1], rad); else g.lineTo(x, y);
      }
      g.closePath();
    };
    g.save(); g.translate(0, -0.6); path(); g.fillStyle = 'rgba(0,0,0,0.55)'; g.fill(); g.restore();
    path(); g.fillStyle = plateIron(g, gtop, b); g.fill(); ironSurface(g, path);
    const rimPath = skip < 0 ? path : () => {   // open: from the skipped edge's far end round to its near end
      const n = pts.length, s1 = (skip + 1) % n, [sx, sy, sr] = pts[s1], [px, py] = pts[skip], d = Math.hypot(px - sx, py - sy) || 1;
      g.beginPath(); g.moveTo(sx + (px - sx) / d * sr, sy + (py - sy) / d * sr);   // the corner arc's start on that edge
      for (let k = 0; k < n; k++) {
        const [x, y, rad] = pts[(s1 + k) % n], nx = pts[(s1 + k + 1) % n];
        if (rad > 0) g.arcTo(x, y, nx[0], nx[1], rad); else g.lineTo(x, y);
      }
    };
    return [rimPath, top];
  };
  // ears.only: 'wings' or 'centre' (each is drawn in its own space: a part's square joint edge must not show in the
  // other's); both by default (the export's plate texture).
  const wy = ears.raise ?? wing, only = ears.only;   // the wings' top edge (bar mode: the tall wing plates)
  const parts = [];
  if (only !== 'centre') {
    parts.push(plate([[x0 - T, b, r], [x0, wy, r], [-A, wy, CC_JOINT.corner], [-A, b, 0]], ears.gradTop, 2));
    parts.push(plate([[A, b, 0], [A, wy, CC_JOINT.corner], [x1, wy, r], [x1 + T, b, r]], ears.gradTop, 0));
  }
  if (only !== 'wings') {   // the centre: its rim along the flat top only (both ends are joints)
    const [, top] = plate([[-A, b, 0], [-A, mid, 0], [A, mid, 0], [A, b, 0]], ears.gradTop);
    parts.push([() => { g.beginPath(); g.moveTo(-A, mid); g.lineTo(A, mid); }, top]);
  }
  g.save(); g.lineCap = 'butt';   // an open rim ending at a joint: no round cap poking past the outline
  for (const [path, top] of parts) rim(g, path, top, b, P.brassHi, 0.35);
  g.restore();
}

/** The joint between the centre plate and a wing. The centre keeps its flat top up to ±CC_SIDE and each wing starts
 *  there (its inner top corner rounded, CC_JOINT.corner): the wings scale around (±CC_SIDE, bottom) with the side bars'
 *  size, so the two outlines meet at heights that change with it. The joint fills the outline's concave corners there
 *  with one smooth fillet: the union's top edge (centre, wing, orb housing) is "closed" with a disk of radius
 *  CC_JOINT.radius (dilate, then erode), which lays a concave fillet into every notch narrower than the disk (the centre's
 *  flat top running into the rising wing or dome). Where the wing is the lower one and no dome rises near, a ramp first
 *  carries the centre's top down onto the wing in an S, so no convex step shows. Only material above the existing
 *  outline is added (nothing on the wing or housing is covered but their top rim and shadow, under the band's feathered
 *  bottom), in the plate's iron (the centre's gradient left of the anchor, the wing's right of it), with the rim and
 *  the shadow along the new outline. Computed once per (mode, size) for the right side; the left mirrors it. The game
 *  draws the same (HudJoint). */
const CC_JOINT = { radius: 6, corner: 3, ramp: 0.6, rampMax: 10, step: 0.05, span: 18, feather: 1.2, cross: 2 };
/** The top (local, unscaled) of what stands on the right wing at x: the orb housing (inner fillet and dome), or
 *  Infinity where nothing does. */
function ccWingStand(s, x) {
  if (!s.orbs) return Infinity;
  // ccHousing's inner side (toward the hotbar): the dome on a vertical skirt, no fillet (the joint's fillet runs into it).
  const R = ORB_H(), cx = ORB.rx, cy = ORB.y;
  return Math.abs(x - cx) <= R ? cy - Math.sqrt(R * R - (x - cx) ** 2) : Infinity;
}
const CC_JOINT_CACHE = new Map();
/** The joint for side size k, right side, screen units: {x0, h, f, T, from, to}: sample i at x0 + i h; f the existing
 *  outline's top, T the joint's (T < f where the joint adds material); from..to the samples it adds. */
function ccJointProfile(s, k) {
  const key = (s.orbs ? 'o' : 'b') + k;
  if (CC_JOINT_CACHE.has(key)) return CC_JOINT_CACHE.get(key);
  const A = CC_SIDE, J = CC_JOINT, yc = CC.plate.mid, wy = s.orbs ? CC.plate.wing : CC_WING.top, yW = ccSideY(wy, k);
  const h = J.step, x0 = A - 2 * J.radius, n = Math.round((J.span + 2 * J.radius) / h) + 1, rc = J.corner * k;
  const f = new Float64Array(n);
  for (let i = 0; i < n; i++) {
    const x = x0 + i * h;
    if (x < A) { f[i] = yc; continue; }
    let y = x < A + rc ? yW + rc - Math.sqrt(Math.max(0, rc * rc - (A + rc - x) ** 2)) : yW;   // the wing's rounded inner corner
    const st = ccWingStand(s, A + (x - A) / k);
    if (st !== Infinity) y = Math.min(y, BOTTOM - (BOTTOM - st) * k);
    f[i] = y;
  }
  // A dome that rises above the centre's top within the disk's reach is joined by the fillet alone; otherwise a lower
  // wing gets the ramp down from the centre's top.
  let dome = Infinity;
  for (let i = 0; i < n; i++) if (x0 + i * h >= A && f[i] <= yc) { dome = x0 + i * h; break; }
  const T0 = Float64Array.from(f);
  if (yW > yc && !(dome - A < 2 * J.radius)) {
    const w = Math.min(J.rampMax, Math.max(J.radius, (yW - yc) * J.ramp));
    for (let i = 0; i < n; i++) {
      const x = x0 + i * h; if (x < A || x > A + w) continue;
      T0[i] = Math.min(T0[i], yc + (yW - yc) * (1 - Math.cos(Math.PI * (x - A) / w)) / 2);
    }
  }
  // Closing (material below the top): dilate, then erode, by a disk of the radius.
  const m = Math.round(J.radius / h), dil = new Float64Array(n), T = new Float64Array(n);
  for (let i = 0; i < n; i++) {
    let v = Infinity;
    for (let u = -m; u <= m; u++) { const q = i + u; if (q < 0 || q >= n) continue; v = Math.min(v, T0[q] - Math.sqrt(Math.max(0, J.radius ** 2 - (u * h) ** 2))); }
    dil[i] = v;
  }
  for (let i = 0; i < n; i++) {
    let v = -Infinity;
    for (let u = -m; u <= m; u++) { const q = i + u; if (q < 0 || q >= n) continue; v = Math.max(v, dil[q] + Math.sqrt(Math.max(0, J.radius ** 2 - (u * h) ** 2))); }
    T[i] = Math.min(v, T0[i]);   // only ever adds material
  }
  let from = -1, to = -1;
  for (let i = 0; i < n; i++) if (T[i] < f[i] - 0.02) { if (from < 0) from = i; to = i; }
  const out = { x0, h, n, f, T, from, to, A };
  CC_JOINT_CACHE.set(key, out);
  return out;
}
function ccJoint(g, s, sign, k, gradTop) {
  const P0 = ccJointProfile(s, k), { x0, h, f, T, A, n } = P0, b = BOTTOM, J = CC_JOINT, X = i => sign * (x0 + i * h);
  const idx = x => Math.max(0, Math.min(n - 1, Math.round((x - x0) / h)));
  const tr = g.getTransform(), px = x => tr.transformPoint(new DOMPoint(sign * x, 0)).x;
  // Draws a shape on its own canvas (the same transform), masked by a horizontal alpha profile, then lays it over.
  const layer = (shape, fill, alphaAt, xa, xb) => {
    const [cc, cg] = hires(2 * CX, BOTTOM + TOPM); cg.setTransform(tr); fill(cg, shape);
    cg.save(); cg.setTransform(1, 0, 0, 1, 0, 0); cg.globalCompositeOperation = 'destination-in';
    const gx = cg.createLinearGradient(px(xa), 0, px(xb), 0);
    for (let q = 0; q <= 16; q++) gx.addColorStop(q / 16, `rgba(0,0,0,${alphaAt(xa + (xb - xa) * q / 16)})`);
    cg.fillStyle = gx; cg.fillRect(0, 0, cc.width, cc.height); cg.restore();
    g.save(); g.setTransform(1, 0, 0, 1, 0, 0); g.drawImage(cc, 0, 0); g.restore();
  };
  const iron = gt => (c, shape) => { shape(c); c.fillStyle = plateIron(c, gt, b); c.fill(); ironSurface(c, () => shape(c)); };
  // 1. Full height either side of the anchor the tones cross over (the wing's gradient scales with it): the wing's tone
  //    fades in over [A - cross, A] on the centre, the centre's fades out over [A, A + R2] on the wing (R2 short of an orb
  //    housing's seat groove), so both sides of the anchor show the same mix and no vertical seam shows.
  const R2 = s.orbs ? Math.min(J.cross, (ORB.rx - (ORB.r + ORB.bezel + 0.4 + 0.35) * ORB.S - A) * k - 0.3) : J.cross;
  const ramp = x => { const t = Math.max(0, Math.min(1, (x - (A - J.cross)) / (J.cross + Math.max(R2, 0.01)))); return t * t * (3 - 2 * t); };
  // From just under the outline (the plates' own rims and the band's stay as they are) to the bottom.
  const full = (xa, xb) => c => { c.beginPath(); for (let i = idx(xa); i <= idx(xb); i++) c.lineTo(X(i), T[i] + 0.4); c.lineTo(sign * xb, b); c.lineTo(sign * xa, b); c.closePath(); };
  layer(full(A - J.cross, A), iron(ccSideY(gradTop, k)), ramp, A - J.cross, A);
  if (R2 > 0) layer(full(A, A + R2), iron(gradTop), x => 1 - ramp(x), A, A + R2);
  // 2. The added band (where the fillet adds material): from the new top down past the old top (covering its rim and
  //    shadow); its bottom is the old top's lowest point nearby plus a feather, so a jump in the old outline (a dome's
  //    side) leaves no seam. Its shadow above it; its iron the centre's gradient crossing into the wing's at the anchor.
  if (P0.from < 0) return;
  {
    const i0 = Math.max(0, P0.from - 2), i1 = Math.min(n - 1, P0.to + 2), r0 = i0, r1 = i1;
    const low = i => { let v = -Infinity; for (let q = Math.max(0, i - 3); q <= Math.min(n - 1, i + 3); q++) v = Math.max(v, f[q]); return v; };
    const band = c => { c.beginPath(); for (let i = i0; i <= i1; i++) c.lineTo(X(i), T[i]); for (let i = i1; i >= i0; i--) c.lineTo(X(i), low(i) + J.feather); c.closePath(); };
    const [oc, o] = hires(2 * CX, BOTTOM + TOPM); o.setTransform(tr);
    o.save(); o.translate(0, -0.6); band(o); o.fillStyle = 'rgba(0,0,0,0.55)'; o.fill(); o.restore();
    iron(gradTop)(o, band);
    { const [wc, wg] = hires(2 * CX, BOTTOM + TOPM); wg.setTransform(tr); iron(ccSideY(gradTop, k))(wg, band);
      wg.save(); wg.setTransform(1, 0, 0, 1, 0, 0); wg.globalCompositeOperation = 'destination-in';
      const gw = wg.createLinearGradient(px(A - 1), 0, px(A + 1), 0); gw.addColorStop(0, 'rgba(0,0,0,0)'); gw.addColorStop(1, '#000');
      wg.fillStyle = gw; wg.fillRect(0, 0, wc.width, wc.height); wg.restore();
      o.save(); o.setTransform(1, 0, 0, 1, 0, 0); o.drawImage(wc, 0, 0); o.restore(); }
    o.save(); o.globalCompositeOperation = 'destination-out';   // the feather: opaque to the old top, fading below it
    for (let i = i0; i <= i1; i++) {
      const lo = low(i), gr = o.createLinearGradient(0, lo, 0, lo + J.feather); gr.addColorStop(0, 'rgba(0,0,0,0)'); gr.addColorStop(1, '#000');
      o.fillStyle = gr; o.fillRect(Math.min(X(i), X(i) + sign * h), lo, h + 0.01, J.feather + 0.01);
    }
    o.restore();
    g.save(); g.setTransform(1, 0, 0, 1, 0, 0); g.drawImage(oc, 0, 0); g.restore();
    // 3. The rim along the band's top (it covered the old rims there), meeting them tangentially at both ends.
    let top = Infinity; for (let i = r0; i <= r1; i++) top = Math.min(top, T[i]);
    g.save(); g.lineCap = 'butt';
    rim(g, () => { g.beginPath(); for (let i = r0; i <= r1; i++) g.lineTo(X(i), T[i]); }, top - 1, b, P.brassHi, 0.35);
    g.restore();
  }
}
