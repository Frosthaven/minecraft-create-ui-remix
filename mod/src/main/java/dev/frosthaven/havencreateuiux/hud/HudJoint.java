package dev.frosthaven.havencreateuiux.hud;

import static dev.frosthaven.havencreateuiux.hud.CompactAtlas.*;
import static dev.frosthaven.havencreateuiux.hud.HudDraw.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * The joints between the centre plate and the wings (the page's ccJoint, ccJointProfile; keep in step). The centre
 * keeps its flat top to ±WING_ANCHOR, each wing starts there at its side size, so the outlines meet at heights that
 * change with it. Each joint closes the union's top edge with a disk of {@link #RADIUS} (a concave fillet into the
 * rising wing or the orb's dome), after an S ramp down onto a lower wing when no dome is near, and draws only what
 * that adds: the plate's iron (the centre's gradient, crossfading into the wing's) from the new top down past the old
 * one (its bottom feathered), the shadow above it and the rim along it. Drawn right after the plate, immediately (one
 * mesh per pass), so the batches before it are flushed first.
 */
final class HudJoint {
    static final double RADIUS = 6, CORNER = 3, RAMP = 0.6, RAMP_MAX = 10, STEP = 0.05, SPAN = 18, FEATHER = 1.2, CROSS = 2;
    private static final double YC = 24.4, ORB_WING = 28, ORB_R = OrbHud.HOUSING * OrbHud.S, ORB_CX = OrbHud.RX, ORB_CY = OrbHud.OY;
    private static final int SHADOW = 0x8C000000, RIM = 0xF4D898;   // the plates' shadow and rim (P.brassHi)
    private static final Map<String, Profile> CACHE = new HashMap<>();

    private HudJoint() {}

    /** The right joint (screen units): sample i at x0 + i h, f the old top, t the new; samples from..to are added. */
    record Profile(double x0, double h, int n, double[] f, double[] t, int from, int to) {}

    static Profile profile(boolean orbs, double k) {
        return CACHE.computeIfAbsent((orbs ? "o" : "b") + k, key -> build(orbs, k));
    }

    /** What stands on the right wing at local x: the orb housing's dome (its inner side is a vertical skirt). */
    private static double stand(boolean orbs, double x) {
        if (!orbs || Math.abs(x - ORB_CX) > ORB_R) return Double.POSITIVE_INFINITY;
        return ORB_CY - Math.sqrt(ORB_R * ORB_R - (x - ORB_CX) * (x - ORB_CX));
    }

    private static Profile build(boolean orbs, double k) {
        double a = WING_ANCHOR, wy = orbs ? ORB_WING : BarHud.TOP, yw = 50 + (wy - 50) * k, h = STEP, x0 = a - 2 * RADIUS, rc = CORNER * k;
        int n = (int) Math.round((SPAN + 2 * RADIUS) / h) + 1;
        double[] f = new double[n], t0 = new double[n], t = new double[n], dil = new double[n];
        for (int i = 0; i < n; i++) {
            double x = x0 + i * h;
            if (x < a) { f[i] = YC; continue; }
            double y = x < a + rc ? yw + rc - Math.sqrt(Math.max(0, rc * rc - (a + rc - x) * (a + rc - x))) : yw;   // the wing's rounded inner corner
            double st = stand(orbs, a + (x - a) / k);
            if (st != Double.POSITIVE_INFINITY) y = Math.min(y, 50 - (50 - st) * k);
            f[i] = y;
        }
        double dome = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) if (x0 + i * h >= a && f[i] <= YC) { dome = x0 + i * h; break; }
        System.arraycopy(f, 0, t0, 0, n);
        if (yw > YC && !(dome - a < 2 * RADIUS)) {   // a lower wing with no dome near: an S ramp down from the centre's top
            double w = Math.min(RAMP_MAX, Math.max(RADIUS, (yw - YC) * RAMP));
            for (int i = 0; i < n; i++) {
                double x = x0 + i * h;
                if (x >= a && x <= a + w) t0[i] = Math.min(t0[i], YC + (yw - YC) * (1 - Math.cos(Math.PI * (x - a) / w)) / 2);
            }
        }
        int m = (int) Math.round(RADIUS / h);
        for (int i = 0; i < n; i++) {   // closing: dilate, then erode, the material by the disk
            double v = Double.POSITIVE_INFINITY;
            for (int u = -m; u <= m; u++) { int q = i + u; if (q >= 0 && q < n) v = Math.min(v, t0[q] - Math.sqrt(Math.max(0, RADIUS * RADIUS - u * h * u * h))); }
            dil[i] = v;
        }
        int from = -1, to = -1;
        for (int i = 0; i < n; i++) {
            double v = Double.NEGATIVE_INFINITY;
            for (int u = -m; u <= m; u++) { int q = i + u; if (q >= 0 && q < n) v = Math.max(v, dil[q] + Math.sqrt(Math.max(0, RADIUS * RADIUS - u * h * u * h))); }
            t[i] = Math.min(v, t0[i]);
            if (t[i] < f[i] - 0.02) { if (from < 0) from = i; to = i; }
        }
        HudLog.once("joint:" + (orbs ? "o" : "b") + k, "plate joint ({}, side scale {}): adds {} units from x {} to {}", orbs ? "orbs" : "bars",
                String.format("%.3f", k), String.format("%.2f", from < 0 ? 0 : (to - from) * h), String.format("%.2f", x0 + from * h), String.format("%.2f", x0 + to * h));
        return new Profile(x0, h, n, f, t, from, to);
    }

    /** Both joints, in the plate's space (texels under the 1/K pose, x from the centre, y from the bar's bottom). */
    static void draw(GuiGraphics g, boolean orbs, double k) {
        Profile p = profile(orbs, k);
        use(g, "joint");
        drawBatched(g);
        Matrix4f mat = g.pose().last().pose();
        int u0 = (orbs ? 228 : 242) * K;
        // Full height either side of the anchor the tones cross over (the wing's gradient scales with it): the wing's
        // fades in over [A - CROSS, A] on the centre, the centre's out over [A, A + r2] on the wing (r2 short of an orb
        // housing's seat groove), so both sides show the same mix at the anchor; from just under the outline down.
        double a = WING_ANCHOR, r2 = orbs ? Math.min(CROSS, (ORB_CX - (OrbHud.R + 1.1 + 0.4 + 0.35) * OrbHud.S - a) * k - 0.3) : CROSS;
        for (int sign = -1; sign <= 1; sign += 2) {
            cross(mat, p, sign, k, u0, a - CROSS, a, true, r2);
            if (r2 > 0) cross(mat, p, sign, k, u0, a, a + r2, false, r2);
            if (p.from >= 0) side(mat, p, sign, k, u0);
        }
        HudDraw.reset();
    }

    private static double ramp(double x, double r2) {
        double t = HudDraw.clamp((x - (WING_ANCHOR - CROSS)) / (CROSS + Math.max(r2, 0.01)));
        return t * t * (3 - 2 * t);
    }

    /** One crossover strip [xa, xb]: the wing's tone (wing) fading in, or the centre's fading out. */
    private static void cross(Matrix4f mat, Profile p, int sign, double k, int u0, double xa, double xb, boolean wing, double r2) {
        BufferBuilder m = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int ia = (int) Math.round((xa - p.x0) / p.h), ib = Math.min(p.n - 2, (int) Math.round((xb - p.x0) / p.h) - 1);
        for (int i = Math.max(0, ia); i <= ib; i++) {
            double x = p.x0 + (i + 0.5) * p.h, alpha = wing ? ramp(x, r2) : 1 - ramp(x, r2);
            if (alpha > 0.002) iron(m, mat, sign, p, i, p.t[i] + 0.4, 50, 1, 1, alpha, wing, k, u0);
        }
        RenderSystem.setShaderTexture(0, PARTS);
        draw(m, GameRenderer::getPositionTexColorShader);
    }

    private static void side(Matrix4f mat, Profile p, int sign, double k, int u0) {
        int i0 = Math.max(0, p.from - 2), i1 = Math.min(p.n - 2, p.to + 2);
        double top = Double.POSITIVE_INFINITY;
        for (int i = i0; i <= i1; i++) top = Math.min(top, p.t[i]);
        // Shadow above the new top, then the iron (two passes: the centre's gradient, the wing's over it), then the rim.
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = i0; i <= i1; i++) if (p.t[i] < p.f[i] - 0.02) quad(b, mat, sign, p, i, p.t[i] - 0.6, p.t[i], SHADOW, SHADOW);
        draw(b, GameRenderer::getPositionColorShader);
        for (int pass = 0; pass < 2; pass++) {
            BufferBuilder m = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int i = i0; i <= i1; i++) {
                double x = p.x0 + i * p.h, low = low(p, i);
                double wing = pass == 0 ? 1 : HudDraw.clamp((x - (WING_ANCHOR - 1)) / 2);   // the wing's gradient: from A - 1 to A + 1
                if (wing <= 0) continue;
                iron(m, mat, sign, p, i, p.t[i], low, 1, 1, wing, pass == 1, k, u0);
                iron(m, mat, sign, p, i, low, low + FEATHER, 1, 0, wing, pass == 1, k, u0);
            }
            RenderSystem.setShaderTexture(0, PARTS);
            draw(m, GameRenderer::getPositionTexColorShader);
        }
        BufferBuilder r = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        double span = (50 - top) / 2;
        for (int i = i0; i <= i1; i++) {
            double a0 = HudDraw.clamp(1 - (p.t[i] - top) / span), a1 = HudDraw.clamp(1 - (p.t[i + 1] - top) / span);
            quad(r, mat, sign, p, i, p.t[i] - 0.175, p.t[i] + 0.175, argb(a0, RIM), argb(a1, RIM));
        }
        draw(r, GameRenderer::getPositionColorShader);
    }

    /** The old top's lowest point nearby (a jump in it, a dome's side, leaves no seam). */
    private static double low(Profile p, int i) {
        double v = Double.NEGATIVE_INFINITY;
        for (int q = Math.max(0, i - 3); q <= Math.min(p.n - 1, i + 3); q++) v = Math.max(v, p.f[q]);
        return v;
    }

    private static void draw(BufferBuilder b, java.util.function.Supplier<net.minecraft.client.renderer.ShaderInstance> shader) {
        MeshData mesh = b.build();
        if (mesh == null) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(shader);
        BufferUploader.drawWithShader(mesh);
    }

    /** Column i (to i + 1) from y0 to y1 (units), a colour at each end (x). */
    private static void quad(BufferBuilder b, Matrix4f m, int sign, Profile p, int i, double y0, double y1, int c0, int c1) {
        double xa = sign * (p.x0 + i * p.h), xb = sign * (p.x0 + (i + 1) * p.h);
        int ca = sign > 0 ? c0 : c1, cb = sign > 0 ? c1 : c0;
        float l = (float) (Math.min(xa, xb) * K), rr = (float) (Math.max(xa, xb) * K), t = (float) ((y0 - 50) * K), bt = (float) ((y1 - 50) * K);
        b.addVertex(m, l, t, 0).setColor(ca);
        b.addVertex(m, l, bt, 0).setColor(ca);
        b.addVertex(m, rr, bt, 0).setColor(cb);
        b.addVertex(m, rr, t, 0).setColor(cb);
    }

    /** The joint iron over column i from y0 to y1, alpha a0 at y0 to a1 at y1, times alpha; wing: the wing's gradient
     *  (the strip's v at the point's local y), else the centre's (its screen y). The strip is the whole bar's gradient at
     *  world y -40..50; u wraps every 12 units. */
    private static void iron(BufferBuilder b, Matrix4f m, int sign, Profile p, int i, double y0, double y1, double a0, double a1,
                             double alpha, boolean wing, double k, int u0) {
        if (y1 <= y0) return;
        double xa = sign * (p.x0 + i * p.h), xb = sign * (p.x0 + (i + 1) * p.h);
        float l = (float) (Math.min(xa, xb) * K), rr = (float) (Math.max(xa, xb) * K), t = (float) ((y0 - 50) * K), bt = (float) ((y1 - 50) * K);
        double cu = ((Math.abs(p.x0 + i * p.h) % 11.5) + 0.25) * K;
        float ua = (float) ((u0 + cu) / PARTS_SIZE), ub = (float) ((u0 + cu + p.h * K) / PARTS_SIZE);
        float v0 = (float) (strip(y0, wing, k) * K / PARTS_SIZE), v1 = (float) (strip(y1, wing, k) * K / PARTS_SIZE);
        int top = argb(a0 * alpha, 0xFFFFFF), bot = argb(a1 * alpha, 0xFFFFFF);
        b.addVertex(m, l, t, 0).setUv(ua, v0).setColor(top);
        b.addVertex(m, l, bt, 0).setUv(ua, v1).setColor(bot);
        b.addVertex(m, rr, bt, 0).setUv(ub, v1).setColor(bot);
        b.addVertex(m, rr, t, 0).setUv(ub, v0).setColor(top);
    }

    /** The strip's row (units, 0..90) for screen y: the wing's gradient scales with it around the bar's bottom. */
    private static double strip(double y, boolean wing, double k) {
        double world = wing ? 50 - (50 - y) / k : y;
        return Math.max(0, Math.min(90, world + 40));
    }
}
