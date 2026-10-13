package dev.frosthaven.havencreateuiux.hud;

import static dev.frosthaven.havencreateuiux.hud.CompactAtlas.*;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Drawing helpers for the compact HUD, in texels relative to (screen centre, screen bottom) under a 1/8 pose scale.
 * Positions on the page (logical units, x from the centre, the bar's bottom at 50) convert with {@link #x} and {@link #y}.
 * Fills clipped to a circle or a pill (the page clips its effects to the glass) go in narrow columns.
 */
final class HudDraw {
    private HudDraw() {}

    static int x(double l) { return (int) Math.round(l * K); }
    /** Where the side bars attach (units, ± from the centre): between the centre plate (±91) and the wings. */
    static final double WING_ANCHOR = 93.5;
    /** Draws a side bar's part at the config's side size: scaled around (sign WING_ANCHOR, the bar's bottom), so the
     *  wing stays attached to the centre and grows outward and upward. */
    static void side(GuiGraphics g, double sign, double s, Runnable draw) {
        if (s == 1) { draw.run(); return; }
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x(sign * WING_ANCHOR), 0, 0);
        pose.scale((float) s, (float) s, 1);
        pose.translate(-x(sign * WING_ANCHOR), 0, 0);
        try { draw.run(); } finally { pose.popPose(); }
    }
    /** A point (units) of a side bar after its scaling (side()): x and y. */
    static double sideX(double ux, double s) { return Math.signum(ux) * WING_ANCHOR + (ux - Math.signum(ux) * WING_ANCHOR) * s; }
    static double sideY(double uy, double s) { return 50 + (uy - 50) * s; }
    static int y(double l) { return (int) Math.round((l - 50) * K); }
    static double clamp(double f) { return Math.max(0, Math.min(1, f)); }

    // ---- Paint order under ImmediatelyFast ----
    // ImmediatelyFast's HUD batching collects GuiGraphics blits and fills per render layer (one per texture, one for fills)
    // and draws them layer by layer when something forces it, not in call order. So a piece of one texture drawn over
    // another texture's (the dash dial's chevrons over its housing, from the orbs atlas over parts) came out under it.
    // Every draw names its kind; a change of kind draws what is batched so far, so the screen keeps the call order.
    static final Object FILL = "fill", ITEM = "item", EFFECT = "mob effect";
    private static Object lastKind;
    private static final String IF_BATCHING = "net.raphimc.immediatelyfast.feature.batching.BatchingBuffers";

    /** Call at the start of a frame. */
    static void beginOrder() { lastKind = null; }
    /** After an immediate draw: nothing is batched, the next draw of any kind needs no flush. */
    static void reset() { lastKind = null; }
    /** Before a draw of the given kind (a texture, FILL, ITEM, EFFECT): draws the batch first when the kind changes. */
    static void use(GuiGraphics g, Object kind) {
        if (lastKind != null && !lastKind.equals(kind)) drawBatched(g);
        lastKind = kind;
    }
    /** Draws everything batched so far (vanilla's buffer source and ImmediatelyFast's HUD batch, when it batches), then
     *  restores the HUD's blending. Drawing a batched layer ends with its render type's clearRenderState, which turns
     *  blending off; ImmediatelyFast records the blend state with each later blit, so every blit after a flush came out
     *  opaque (the orbs' glass and glow as solid white and black, the digits' cells as blocks, the selection frame's
     *  transparent margin as a second border). */
    static void drawBatched(GuiGraphics g) {
        g.flush();
        if (Reflect.cls(IF_BATCHING) != null && Reflect.bool(Reflect.callStatic(IF_BATCHING, "isHudBatching")))
            Reflect.callStatic(IF_BATCHING, "tryForceDrawHudBuffers");
        restoreBlend();
    }
    /** The HUD's blend state: alpha blending on, the default function (what every piece is drawn with). Enable LAST:
     *  while ImmediatelyFast batches, a blend change first draws what is batched, and that draw ends by turning blending
     *  off, so a blendFunc after enableBlend left it off. */
    static void restoreBlend() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
    }
    /** Logs once if blending is off when a frame starts drawing (the state every texture here needs). */
    static void checkBlend(String where) {
        if (!org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND))
            HudLog.once("blend-off:" + where, "blending was off at {} (restored): something before it left the GL state changed", where);
    }

    static void part(GuiGraphics g, int u, int v, int w, int h, int x, int y) {
        if (w > 0 && h > 0) { use(g, PARTS); g.blit(PARTS, x, y, u, v, w, h, PARTS_SIZE, PARTS_SIZE); }
    }
    static void orbPart(GuiGraphics g, int u, int v, int w, int h, int x, int y) {
        if (w > 0 && h > 0) { use(g, ORBS); g.blit(ORBS, x, y, u, v, w, h, ORBS_W, ORBS_H); }
    }
    /** A rect (x0..x1, y0..y1, texels relative to the piece's centre) of an orb-atlas piece centred on (cx, cy). */
    static void orbPiece(GuiGraphics g, double[] cell, double cx, double cy, double x0, double y0, double x1, double y1) {
        int ax = (int) Math.round(cx + x0), ay = (int) Math.round(cy + y0), bx = (int) Math.round(cx + x1), by = (int) Math.round(cy + y1);
        orbPart(g, (int) Math.round(cell[0] + ax - cx), (int) Math.round(cell[1] + ay - cy), bx - ax, by - ay, ax, ay);
    }
    /** The whole cell. */
    static void orbCell(GuiGraphics g, String name, double cx, double cy) {
        orbPiece(g, cell(name), cx, cy, -CELL_HALF, -CELL_HALF, CELL_HALF, CELL_HALF);
    }
    /** A screen-blended piece (glows on black: result = src + dst (1 - src)). */
    static void screen(Runnable draw) {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
        RenderSystem.enableBlend();   // after the function: see restoreBlend
        try { draw.run(); } finally { restoreBlend(); }
    }

    /** A textured sector: the texture around (u, v) (texels) drawn around (cx, cy) out to radius r, from angle a0 to a1
     *  (radians, clockwise from +x, as the page's canvas arcs), so a ring reveals by angle. */
    static void sector(GuiGraphics g, ResourceLocation tex, int texW, int texH, double u, double v, double cx, double cy, double r, double a0, double a1) {
        if (a1 <= a0) return;
        use(g, "sector");   // an immediate draw: what is batched must be on screen first
        drawBatched(g);
        int steps = Math.max(1, (int) Math.ceil((a1 - a0) / (Math.PI / 48)));
        double rr = r / Math.cos((a1 - a0) / steps / 2);   // the polygon's edges outside the circle
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        for (int i = 0; i < steps; i++) {
            double t0 = a0 + (a1 - a0) * i / steps, t1 = a0 + (a1 - a0) * (i + 1) / steps;
            vertex(b, m, texW, texH, u, v, cx, cy, 0, 0);
            vertex(b, m, texW, texH, u, v, cx, cy, Math.cos(t1) * rr, Math.sin(t1) * rr);
            vertex(b, m, texW, texH, u, v, cx, cy, Math.cos(t0) * rr, Math.sin(t0) * rr);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
        restoreBlend();
        lastKind = null;
    }
    private static void vertex(BufferBuilder b, Matrix4f m, int texW, int texH, double u, double v, double cx, double cy, double dx, double dy) {
        b.addVertex(m, (float) (cx + dx), (float) (cy + dy), 0).setUv((float) ((u + dx) / texW), (float) ((v + dy) / texH));
    }

    // ---- Fills ----
    static int argb(double a, int rgb) { return ((int) Math.round(clamp(a) * 255) << 24) | (rgb & 0xFFFFFF); }

    /** The part of the rect inside the circle (cx, cy, r), in columns of 2 texels. */
    static void discFill(GuiGraphics g, double cx, double cy, double r, double x0, double x1, double y0, double y1, int argb) {
        discGradient(g, cx, cy, r, x0, x1, y0, y1, y0, argb, y1, argb);
    }
    /** As discFill, a vertical gradient from colour top at y ga to bottom at gb. */
    static void discGradient(GuiGraphics g, double cx, double cy, double r, double x0, double x1, double y0, double y1,
                             double ga, int top, double gb, int bottom) {
        use(g, FILL);
        for (double x = Math.floor(x0); x < x1; x += 2) {
            double xa = Math.max(x, x0), xb = Math.min(x + 2, x1), xm = (xa + xb) / 2 - cx;
            if (Math.abs(xm) >= r) continue;
            double h = Math.sqrt(r * r - xm * xm), a = Math.max(y0, cy - h), b = Math.min(y1, cy + h);
            if (b <= a) continue;
            int ia = (int) Math.round(a), ib = (int) Math.round(b);
            if (ib <= ia) continue;
            if (top == bottom) g.fill((int) Math.round(xa), ia, (int) Math.round(xb), ib, top);
            else g.fillGradient((int) Math.round(xa), ia, (int) Math.round(xb), ib, mix(top, bottom, (a - ga) / (gb - ga)), mix(top, bottom, (b - ga) / (gb - ga)));
        }
    }
    /** The part of the rect inside a pill (a meter's glass: x, y, w, h with round ends), in columns of 2 texels. */
    static void pillFill(GuiGraphics g, double px, double py, double pw, double ph, double x0, double x1, double y0, double y1, int argb) {
        use(g, FILL);
        double r = ph / 2, cy = py + r;
        for (double x = Math.floor(Math.max(x0, px)); x < Math.min(x1, px + pw); x += 2) {
            double xa = Math.max(x, x0), xb = Math.min(Math.min(x + 2, x1), px + pw), xm = (xa + xb) / 2;
            double d = xm < px + r ? px + r - xm : xm > px + pw - r ? xm - (px + pw - r) : 0;
            if (d >= r) continue;
            double h = Math.sqrt(r * r - d * d), a = Math.max(y0, cy - h), b = Math.min(y1, cy + h);
            int ia = (int) Math.round(a), ib = (int) Math.round(b);
            if (ib > ia) g.fill((int) Math.round(xa), ia, (int) Math.round(xb), ib, argb);
        }
    }
    /** A small round dot (motes, sparkles), in 1-texel columns. */
    static void dot(GuiGraphics g, double cx, double cy, double r, int argb) {
        use(g, FILL);
        for (double x = Math.floor(cx - r); x < cx + r; x++) {
            double xm = x + 0.5 - cx;
            if (Math.abs(xm) >= r) continue;
            double h = Math.sqrt(r * r - xm * xm);
            int ia = (int) Math.round(cy - h), ib = (int) Math.round(cy + h);
            if (ib > ia) g.fill((int) x, ia, (int) x + 1, ib, argb);
        }
    }
    /** A thin circle outline (bubbles): points around it. */
    static void ring(GuiGraphics g, double cx, double cy, double r, int argb) {
        use(g, FILL);
        int n = Math.max(8, (int) (r * 3));
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / n;
            int px = (int) Math.floor(cx + Math.cos(a) * r), py = (int) Math.floor(cy + Math.sin(a) * r);
            g.fill(px, py, px + 1, py + 1, argb);
        }
    }
    static int mix(int c0, int c1, double t) {
        t = clamp(t);
        int out = 0;
        for (int s = 0; s < 32; s += 8) {
            int a = (c0 >>> s) & 0xFF, b = (c1 >>> s) & 0xFF;
            out |= ((int) Math.round(a + (b - a) * t) & 0xFF) << s;
        }
        return out;
    }

    // ---- Digits ----

    /** The 0.625 set (effects): colour row per character (value bright, the rest dim), inside a solid black outline one
     *  font pixel (5 texels) wide, so they read on any background (a shadow down-right blurred them together). */
    static void digits(GuiGraphics g, String s, int x, int y, int brightChars, int bright, int rest) {
        for (int dy = -5; dy <= 5; dy += 5) for (int dx = -5; dx <= 5; dx += 5) {
            if (dx == 0 && dy == 0) continue;
            int cx = x;
            for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); part(g, digitU(c), digitV(BLACK), digitW(c), DIGIT_H, cx + dx, y + dy); cx += digitAdv(c); }
        }
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            part(g, digitU(c), digitV(i < brightChars ? bright : rest), digitW(c), DIGIT_H, cx, y);
            cx += digitAdv(c);
        }
    }
    static int digitsWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += digitAdv(s.charAt(i));
        return w - 5;
    }

    /** Width (texels) of s in a small set of k units a font pixel (glyphs 3 wide, 1 apart). */
    static double textWidth(String s, double k) { return (4 * s.length() - 1) * k * K; }
    /** s at (x, y) (texels, top left) from a glyph row of the 0.6 or 0.55 set, with the black shadow row one font
     *  pixel down-right first. k: the set's size; v / shadowV: the rows. */
    static void text(GuiGraphics g, String s, double x, double y, double k, int v, int shadowV) {
        int cw = (int) Math.ceil(3 * k * K) + 1, ch = (int) Math.ceil(5 * k * K) + 1;
        for (int pass = 0; pass < 2; pass++) {
            double off = pass == 0 ? k * K : 0;
            for (int i = 0; i < s.length(); i++) {
                int u = glyphU(s.charAt(i));
                part(g, u, pass == 0 ? shadowV : v, cw, ch, (int) Math.round(x + off + i * 4 * k * K), (int) Math.round(y + off));
            }
        }
    }
    /** Tinted text at 0.6 (meter and orb numbers, the tank time) centred on cx, its middle at cy. */
    static void tinted(GuiGraphics g, String s, double cx, double cy, String tint) {
        text(g, s, cx - textWidth(s, 0.6) / 2, cy - 2.5 * 0.6 * K, 0.6, tintV(tint), tintV("black"));
    }
}
