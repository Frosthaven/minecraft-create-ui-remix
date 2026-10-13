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
    static int y(double l) { return (int) Math.round((l - 50) * K); }
    static double clamp(double f) { return Math.max(0, Math.min(1, f)); }

    static void part(GuiGraphics g, int u, int v, int w, int h, int x, int y) {
        if (w > 0 && h > 0) g.blit(PARTS, x, y, u, v, w, h, PARTS_SIZE, PARTS_SIZE);
    }
    static void orbPart(GuiGraphics g, int u, int v, int w, int h, int x, int y) {
        if (w > 0 && h > 0) g.blit(ORBS, x, y, u, v, w, h, ORBS_W, ORBS_H);
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
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
        try { draw.run(); } finally { RenderSystem.defaultBlendFunc(); }
    }

    /** A textured sector: the texture around (u, v) (texels) drawn around (cx, cy) out to radius r, from angle a0 to a1
     *  (radians, clockwise from +x, as the page's canvas arcs), so a ring reveals by angle. */
    static void sector(GuiGraphics g, ResourceLocation tex, int texW, int texH, double u, double v, double cx, double cy, double r, double a0, double a1) {
        if (a1 <= a0) return;
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

    /** The 0.625 set (effects): colour row per character (value bright, the rest dim), a shadow under them. */
    static void digits(GuiGraphics g, String s, int x, int y, int brightChars, int bright, int rest) {
        for (int pass = 0; pass < 2; pass++) {
            int cx = x;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                int row = pass == 0 ? BLACK : i < brightChars ? bright : rest;
                // The shadow a full font pixel (5 texels) down-right: half a pixel let it eat the white strokes in game.
                part(g, digitU(c), digitV(row), digitW(c), DIGIT_H, cx + (pass == 0 ? 5 : 0), y + (pass == 0 ? 5 : 0));
                cx += digitAdv(c);
            }
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
