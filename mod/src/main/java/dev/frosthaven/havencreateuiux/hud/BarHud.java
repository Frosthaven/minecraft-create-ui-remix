package dev.frosthaven.havencreateuiux.hud;

import static dev.frosthaven.havencreateuiux.hud.CompactAtlas.*;
import static dev.frosthaven.havencreateuiux.hud.HudDraw.*;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Bar mode (the page's hud-compact.js meters): three rows of glass meters on each wing, no icons (their colours and
 * order say what they are). Left: health (absorption, the statuses along it), food (or the mount's health; saturation,
 * Nourishment, exhaustion's hairline), armor. Right: breath (the backtank while it holds air) / Helium Flamingo / an
 * aircraft's health, or a groove; then the mana pools, the primary first; one pool: the last row a groove.
 */
final class BarHud {
    static final double[] ROWS = {31.4, 37.4, 43.4};
    static final double MW = 32, MH = 4, LEFT_X = -131, RIGHT_X = 99;
    private static final double EXH_GAP = 0.5, EXH_H_L = 0.6;   // the exhaustion strip under food (CC_EXH_STRIP)
    private static final int EXH_TRACK = 0xFF15120F, EXH_FILL = 0xFF8A7458;
    /** The meter blocks are drawn S x around (±ANCHOR, BY) (the page's CC_WING): the left spans -165..-101 and 13.8..45.8,
     *  centred (MID) in the wing plate (top TOP); OUTER: the blocks' outer edge plus 2. */
    static final double S = 2, ANCHOR = 97, BY = 49, TOP = 9.6, MID = 29.8, OUTER = 167;
    private static final java.util.Set<String> MANA = java.util.Set.of("irons", "ars"), BROTH = java.util.Set.of("broth", "hunger");

    private BarHud() {}

    static void draw(GuiGraphics g, HudFrame f) {
        checkBlend("bars"); restoreBlend();
        side(g, -1, f.side, () -> scaled(g, -ANCHOR, () -> {
            if (!f.hurtable) gears(g, LEFT_X + MW / 2, (ROWS[0] + ROWS[2] + MH) / 2);   // the empty vitals wing
            else left(g, f);
        }));
        side(g, 1, f.side, () -> scaled(g, ANCHOR, () -> right(g, f)));
    }

    /** A wing's meter block, S x around (ax, BY). */
    private static void scaled(GuiGraphics g, double ax, Runnable draw) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x(ax), y(BY), 0);
        pose.scale((float) S, (float) S, 1);
        pose.translate(-x(ax), -y(BY), 0);
        try { draw.run(); } finally { pose.popPose(); }
    }

    private static void left(GuiGraphics g, HudFrame f) {
        double max = f.maxHp, hp = f.hp / max, y0 = ROWS[0];
        meter(g, f, LEFT_X, y0, f.healthLiquid, hp);
        if (f.absorb > 0) span(g, LEFT_X, y0, "absorb", hp, (f.hp + f.absorb) / max);
        long t = f.now;
        double ix = LEFT_X + 0.5, iw = MW - 1, fill = iw * clamp(hp);
        if (f.apple != null && f.apple[1] > 0)   // AppleSkin: what the held food would heal, pale and pulsing
            inner(g, LEFT_X, y0, ix + fill, ix + iw * clamp((f.hp + f.apple[1]) / max), argb(0.3 + 0.25 * Math.sin(t / 160.0), 0xFFC8C8));
        if (f.hurtSince < 400) inner(g, LEFT_X, y0, LEFT_X, LEFT_X + MW, argb(0.55 * (1 - f.hurtSince / 400.0), 0xFF2828));
        if (f.hp <= 4) inner(g, LEFT_X, y0, ix + fill, ix + iw, argb(warn(t), 0xC8141E));
        if (f.regen && fill > 0) {   // Regeneration: pink motes drifting along the health liquid
            for (int i = 0; i < 9; i++) {
                double hh = Math.sin((i + 1) * 91.7) * 43758.5453, u = hh - Math.floor(hh), k = ((t / 1500.0) * (0.6 + u * 0.6) + u * 3.7) % 1;
                double py = y0 + 0.9 + (MH - 1.8) * (0.5 + 0.45 * Math.sin(i * 2.3 + t / 300.0));
                dot(g, x(ix + k * fill), (py - 50) * K, (0.42 + (i % 3) * 0.1) * K, argb(Math.sin(Math.PI * k) * 0.95, 0xFF78AF));
            }
        }
        if (f.comfort && f.saturation <= 0 && f.hp < max) {   // Farmer's Delight Comfort: a pink sheen along the glass
            double p = ((t / 2600.0) % 1) * (MW + 12) - 6;
            for (double x = LEFT_X + p - 6; x < LEFT_X + p + 6; x += 0.25)
                inner(g, LEFT_X, y0, x, x + 0.25, argb(0.55 * (1 - Math.abs(x + 0.125 - (LEFT_X + p)) / 6), 0xFF9EC4));
        }
        if (f.nums) numbers(g, LEFT_X, y0, f.absorb > 0 ? Math.round(f.hp) + "+" + Math.round(f.absorb) + "/" + Math.round(max)
                : Math.round(f.hp) + "/" + Math.round(max), f.healthLiquid);

        double y1 = ROWS[1];
        if (f.mount != null) {
            meter(g, f, LEFT_X, y1, "mount", f.mountHp / f.mountMax);
            if (f.nums) numbers(g, LEFT_X, y1, Math.round(f.mountHp) + "/" + Math.round(f.mountMax), "mount");
        } else {
            String liq = f.hunger ? "hunger" : "broth";
            double ff = f.food / 20.0, ffill = iw * ff;
            meter(g, f, LEFT_X, y1, liq, ff);
            span(g, LEFT_X, y1, "saturation", 0, Math.min(f.saturation, f.food) / 20.0);
            if (f.food <= 6) inner(g, LEFT_X, y1, ix + ffill, ix + iw, argb(warn(t), 0xDC7814));
            if (f.nourished && ffill > 0) {   // Nourishment: a gold glaze, sparkles, a gold edge
                inner(g, LEFT_X, y1, ix, ix + ffill, argb(0.28, 0xFFD75A));
                for (int i = 0; i < 6; i++) {
                    double hh = Math.sin((i + 3) * 47.3) * 43758.5453, u = hh - Math.floor(hh);
                    double sx = ix + u * (ffill - 0.4), sy = y1 + MH * (0.25 + 0.5 * ((i * 0.37) % 1));
                    use(g, FILL);
                    g.fill(x(sx), (int) Math.round((sy - 50) * K), x(sx + 0.4), (int) Math.round((sy + 0.4 - 50) * K), argb(0.45 + 0.45 * Math.sin(t / 260.0 + i * 1.7), 0xFFF0A0));
                }
                inner(g, LEFT_X, y1, ix + ffill - 0.25, ix + ffill + 0.25, argb(0.95, 0xFFDC6E));
            }
            if (f.apple != null && f.apple[0] > 0)   // AppleSkin: what the held food restores
                inner(g, LEFT_X, y1, ix + ffill, ix + iw * Math.min(20, f.food + f.apple[0]) / 20.0, argb(0.35 + 0.25 * Math.sin(t / 160.0), 0xFFE6B4));
            // Exhaustion: a slim solid strip under the food meter (the page's CC_EXH_STRIP), full at 0, draining from the right.
            double sy = y1 + MH + EXH_GAP, frac = 1 - clamp(f.exhaustShown);
            use(g, FILL);
            g.fill(x(LEFT_X), y(sy), x(LEFT_X + MW), y(sy + EXH_H_L), EXH_TRACK);
            if (frac > 0.001) g.fill(x(LEFT_X), y(sy), x(LEFT_X + MW * frac), y(sy + EXH_H_L), EXH_FILL);
            if (f.nums) numbers(g, LEFT_X, y1, f.food + "/20", liq);
        }
        if (f.showArmor) {   // armor: the meter shows up to 20 (as vanilla's ten icons); the number the real value
            meter(g, f, LEFT_X, ROWS[2], "armor", Math.min(1, f.armor / 20.0));
            if (f.nums) numbers(g, LEFT_X, ROWS[2], f.armor + "/" + (f.armor > 20 ? 30 : 20), "armor");
        }
    }

    private static void right(GuiGraphics g, HudFrame f) {
        double y0 = ROWS[0];
        if (f.showBreath && f.tank != null && f.tankShown > 0) {
            meter(g, f, RIGHT_X, y0, "tank", f.tankShown / Math.max(1, f.tank[1]));
            tinted(g, HudFrame.mmss(f.tank[0]), (RIGHT_X + MW / 2) * K, (y0 + MH / 2 - 50) * K, "tank");
        } else if (f.showBreath) {
            meter(g, f, RIGHT_X, y0, "air", f.airShown);
            if (f.nums) numbers(g, RIGHT_X, y0, (int) Math.ceil(f.airShown * 10) + "/10", "air");
        } else groove(g, RIGHT_X, y0);
        var pools = f.pools();
        if (pools.isEmpty()) { gears(g, RIGHT_X + MW / 2, (ROWS[1] + ROWS[2] + MH) / 2); return; }
        for (int k = 1; k <= 2; k++) {
            if (k - 1 >= pools.size()) { groove(g, RIGHT_X, ROWS[k]); continue; }
            var pool = pools.get(k - 1);
            meter(g, f, RIGHT_X, ROWS[k], pool.name(), pool.frac());
            if (f.nums) numbers(g, RIGHT_X, ROWS[k], Math.round(pool.cur()) + "/" + Math.round(pool.max()), pool.name());
        }
    }

    private static double warn(long t) { return 0.12 + 0.12 * Math.sin(t / 220.0); }

    /** A meter at (x, y) units: casing, the liquid's inner strip to frac, then what moves with the fill (mana stars,
     *  broth bubbles). */
    static void meter(GuiGraphics g, HudFrame f, double mx, double my, String liquid, double frac) {
        part(g, 0, WING_CASE_V, WING_W, WING_H, x(mx), y(my));
        span(g, mx, my, liquid, 0, frac);
        double fx = mx + 0.5, fw = (MW - 1) * clamp(frac);
        if (fw <= 0) return;
        if (MANA.contains(liquid)) stars(g, mx, my, MW, MH, fw);
        else if (BROTH.contains(liquid)) {
            for (int i = 0; i < Math.round(fw / 5); i++) {
                double hh = Math.sin((fx + i * 9.17) * 33.1) * 43758.5453, u = hh - Math.floor(hh);
                ring(g, (fx + u * fw) * K, (my + MH / 2 - 50) * K, 0.35 * K, argb(0.55, 0xFFE6BE));
            }
        }
    }
    /** The galaxy's stars over a mana fill (they spread with the fill, as on the page). */
    static void stars(GuiGraphics g, double mx, double my, double w, double h, double fw) {
        double fx = mx + 0.5;
        for (int i = 0; i < Math.round(fw / 3); i++) {
            double hh = Math.sin((fx + i * 12.9898) * 78.233) * 43758.5453, u = hh - Math.floor(hh), v = hh * 7.13 - Math.floor(hh * 7.13);
            double sx = fx + u * fw, sy = my + v * h;
            pillFill(g, x(mx + 0.5), y(my + 0.5), (w - 1) * K, (h - 1) * K, sx * K, (sx + 0.25) * K, (sy - 50) * K, (sy + 0.25 - 50) * K,
                    i % 5 != 0 ? argb(0.7, 0xDCE6FF) : argb(0.95, 0xFFFFFF));
        }
    }
    /** The liquid's inner strip from frac a to b. */
    static void span(GuiGraphics g, double mx, double my, String liquid, double a, double b) {
        int w = WING_W - 2 * INSET, h = WING_H - 2 * INSET, v = wingLiquidV(liquid) + INSET;
        int from = (int) Math.round(w * clamp(a)), to = (int) Math.round(w * clamp(b));
        if (to > from) part(g, INSET + from, v, to - from, h, x(mx) + INSET + from, y(my) + INSET);
    }
    /** A fill over the meter's glass (x0..x1 units), clipped to its rounded inner shape. */
    private static void inner(GuiGraphics g, double mx, double my, double x0, double x1, int argb) {
        pillFill(g, x(mx + 0.5), y(my + 0.5), (MW - 1) * K, (MH - 1) * K, x0 * K, x1 * K, y(my), y(my + MH), argb);
    }
    /** Value / max centred on the meter in its liquid's tint, the liquid dimmed behind. */
    private static void numbers(GuiGraphics g, double mx, double my, String s, String tint) {
        inner(g, mx, my, mx, mx + MW, argb(0.6, 0x040506));
        tinted(g, s, (mx + MW / 2) * K, (my + MH / 2 - 50) * K, tint);
    }
    private static void groove(GuiGraphics g, double mx, double my) { part(g, GROOVE_U, GROOVE_V, WING_W, WING_H, x(mx), y(my)); }
    static void gears(GuiGraphics g, double cx, double cy) { part(g, GEARS_U, GEARS_V, GEARS_W, GEARS_H, x(cx) - GEARS_W / 2, y(cy) - GEARS_H / 2); }
}
