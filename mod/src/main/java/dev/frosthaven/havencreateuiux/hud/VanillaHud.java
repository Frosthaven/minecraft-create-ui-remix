package dev.frosthaven.havencreateuiux.hud;

import static dev.frosthaven.havencreateuiux.hud.CompactAtlas.*;
import static dev.frosthaven.havencreateuiux.hud.HudDraw.*;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Almost Vanilla (the default meter display; the redesign page's hud-vanilla*.js, Layout: vanilla): the pieces at
 * vanilla's places, floating as vanilla's do (no plate). The hotbar's sockets; the offhand socket at one end and the
 * dodge roll dial in the same 18 x 18 box at the other (left-handed swaps them), hardcore's skull plate above the
 * offhand; vanilla's XP bar as one gauge (mana over XP, the level window on it); hearts and food above it, armor and
 * breath above them, by vanilla's rules (Gui.renderHearts, renderFood, renderVehicleHealth), with the action bar's
 * effects (damage flash, Regeneration motes, natural heal, AppleSkin, Comfort, Nourishment). Drawn from the vanilla
 * atlas (createhud:textures/gui/compact/vanilla) and the parts and orbs atlases, at 1/8 GUI px.
 */
final class VanillaHud {
    /** The right end box's left x (the left one is its mirror, -END_X - 18), both ends' top; the sockets' span; the
     *  gauge's lower edge (it is vanilla's XP bar, bottom - 29, 5 tall); the rows: 3 px over the gauge, 81 wide. */
    static final double END_X = 100, END_Y = 30, X0 = -89, X1 = 89, GAUGE_BOTTOM = 26, GAP = 3, ROW_W = 81;
    /** While numbers show, the rows' icons draw at this alpha (the meters dim their liquid behind the digits). */
    private static final double DIM = 0.4;
    private static final int EXH_TRACK = 0xFF15120F, EXH_FILL = 0xFF8A7458;
    private static double dim = 1;

    private VanillaHud() {}

    static void draw(GuiGraphics g, HudFrame f, float delta, float scale, int cx, int bottom,
                     Float cast, Float train, Float engine, Float attack, boolean[] toolbox, int schematic) {
        var mc = f.mc;
        var p = f.p;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, bottom, 0);
        pose.scale(1f / K, 1f / K, 1);
        checkBlend("almost vanilla"); restoreBlend();
        dim = 1;
        double ox = f.lefty ? END_X : -END_X - 18, dx = f.lefty ? -END_X - 18 : END_X;
        CompactHud.hotbar(g, p, toolbox, schematic);
        CompactHud.offhandSocket(g, p, ox, END_Y);
        if (f.hardcore) part(g, SKULL_U, 0, SKULL_W, SKULL_H, x(ox + 9 - 4.5), y(END_Y - 9 - 2.5));   // the skull plate above the offhand
        HudLog.changed("roll", f.roll == null ? "none" : String.valueOf((int) f.roll[1]), "dodge roll: {}",
                f.roll == null ? "not found (a pressed-in cog takes its place)" : "found, " + (int) f.roll[1] + " charges");
        CompactHud.dialAt(g, f.roll, dx + 9, END_Y + 9, 9 / (HudFrame.DIAL_R + HudFrame.DIAL_B + 0.45));   // filling the offhand's box
        double top = gauge(g, f, CompactHud.take(p, cast, train, engine));
        if (f.hurtable) top = rows(g, f, top);   // creative hides health, food, armor and air, as vanilla
        double plate = CompactHud.instruments(g, mc, p, delta, top - 9 - 3);
        if (!Double.isNaN(plate)) top = plate;
        HudEffects.draw(g, p, f.gear, cx, bottom, scale, -END_X - 18, top - 2);   // the first socket on the left end box's edge
        pose.popPose();

        CompactHud.hotbarItems(g, mc, p, attack, cx, bottom);
        CompactHud.slotItem(g, mc, p, p.getOffhandItem(), cx + (int) ox + 1, bottom - 19, 10);   // vanilla's offhand spot
        int h = (int) Math.ceil((50 - top + 1) * scale);   // later HUD layers stack above the rows
        mc.gui.leftHeight = Math.max(mc.gui.leftHeight, h);
        mc.gui.rightHeight = Math.max(mc.gui.rightHeight, h);
    }

    // ---- Icons ----

    /** Cell n of a row showing v half-points: full, half or empty (vanilla: full below 2 i + 1). */
    private static String kind(double v, int i) { return 2 * i + 1 < v ? "full" : 2 * i + 1 == v ? "half" : "empty"; }
    private static double rand(int a, int b) { double v = Math.sin(a * 127.1 + b * 311.7) * 43758.5453; return v - Math.floor(v); }

    /** An icon cell at (ux, uy) (units, its top left) at alpha a, times the row's dimming. A faded icon is drawn on its own
     *  with the shader colour's alpha (flushed before and after, so nothing else takes it). */
    private static void icon(GuiGraphics g, String name, double ux, double uy, double a) {
        a *= dim;
        if (a <= 0.004) return;
        int u = vanCellU(name), v = vanCellV(name), px = x(ux), py = y(uy);
        if (a >= 0.999) { use(g, VANILLA); g.blit(VANILLA, px, py, u, v, VAN_CELL, VAN_CELL, VAN_W, VAN_H); return; }
        drawBatched(g);
        reset();
        RenderSystem.setShaderColor(1, 1, 1, (float) a);
        try {
            use(g, VANILLA);
            g.blit(VANILLA, px, py, u, v, VAN_CELL, VAN_CELL, VAN_W, VAN_H);
            drawBatched(g);
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            reset();
        }
    }

    // ---- The gauge ----

    /** Vanilla's XP bar as one gauge (the page's vanGauge, ccGauge): a steel casing around 4 of glass, mana (2) over XP
     *  (2); no mana: XP fills the 4. Two pools split at the level window (primary left, filling from the left; the other
     *  right, from the right), each 1 px under the window's border; one fills the channel. Returns the gauge's top. */
    private static double gauge(GuiGraphics g, HudFrame f, CompactHud.Take take) {
        restoreBlend();
        var pools = f.pools();
        double t = GAUGE_BOTTOM - 5, ix = X0 + 0.5, iw = X1 - X0 - 1;
        String lvl = String.valueOf(Math.max(0, f.p.experienceLevel));
        double tw = textWidth(lvl, CompactHud.LEVEL_K) / K;
        double ww = take != null ? CompactHud.ICON_W : Math.max(tw + 3.6, 8.5), wh = take != null ? CompactHud.ICON_H : CompactHud.LEVEL_H;
        double wx = -ww / 2, wy = t + 2.5 - wh / 2, wl = wx + 1, wr = wx + ww - 1;
        use(g, VANILLA);
        g.blit(VANILLA, x(X0), y(t), 0, VAN_FRAME_V, VAN_FRAME_W, VAN_FRAME_H, VAN_W, VAN_H);
        double[][] mana = pools.size() == 2 ? new double[][] {{ix, t + 0.5, wl - ix, 2}, {wr, t + 0.5, ix + iw - wr, 2}}
                : pools.size() == 1 ? new double[][] {{ix, t + 0.5, iw, 2}} : new double[0][];
        for (int k = 0; k < mana.length; k++) {
            var pool = pools.get(k);
            channel(g, mana[k], pool.name(), false, pool.frac(), k == 1, mana.length == 2 ? 10 : 20, true);
            if (f.nums) {   // the liquid dimmed behind the digits, as the meters'
                use(g, FILL);
                g.fill(x(mana[k][0]), y(mana[k][1]), x(mana[k][0] + mana[k][2]), y(mana[k][1] + mana[k][3]), argb(0.6, 0x040506));
            }
        }
        boolean tall = mana.length == 0;
        double[] xp = tall ? new double[] {ix, t + 0.5, iw, 4} : new double[] {ix, t + 2.5, iw, 2};
        channel(g, xp, take != null ? take.liquid() : "xp", tall, take != null ? take.frac() : f.p.experienceProgress, false, 18, take != null && "cast".equals(take.liquid()));
        // The level window: the parts atlas's (three slices, as wide as the number needs), or the icon window.
        double mid = wy + wh / 2;
        if (take != null) {
            part(g, ICON_WIN_U, 0, (int) (CompactHud.ICON_W * K), (int) (CompactHud.ICON_H * K), x(wx), y(wy));
            part(g, winIconU(take.icon()), 0, WIN_ICON, WIN_ICON, x(-3.5), y(mid - 3.5));
        } else {
            int px = x(wx), py = y(wy), w = x(-wx) - px, h = (int) Math.round(wh * K);
            use(g, PARTS);
            g.blit(PARTS, px, py, WINDOW_CAP, h, WINDOW_U, 0, WINDOW_CAP, h, PARTS_SIZE, PARTS_SIZE);
            g.blit(PARTS, px + WINDOW_CAP, py, w - 2 * WINDOW_CAP, h, WINDOW_U + WINDOW_CAP, 0, WINDOW_W - 2 * WINDOW_CAP, h, PARTS_SIZE, PARTS_SIZE);
            g.blit(PARTS, px + w - WINDOW_CAP, py, WINDOW_CAP, h, WINDOW_U + WINDOW_W - WINDOW_CAP, 0, WINDOW_CAP, h, PARTS_SIZE, PARTS_SIZE);
            text(g, lvl, -tw / 2 * K, (mid - 2.5 * CompactHud.LEVEL_K - 50) * K, CompactHud.LEVEL_K,
                    levelV(f.p.experienceLevel > 0 ? LEVEL_GREEN : LEVEL_DIM), levelV(LEVEL_BLACK));
        }
        if (f.nums) for (int k = 0; k < mana.length; k++) {
            var pool = pools.get(k);
            tinted(g, Math.round(pool.cur()) + "/" + Math.round(pool.max()), (mana[k][0] + mana[k][2] / 2) * K, (mana[k][1] + 1 - 50) * K, pool.name());
        }
        return t;
    }
    /** A channel (r: x, y, w, h units): its liquid's strip to frac (from the right end with fromRight), the galaxy's
     *  stars over a mana fill (they spread with the fill), the segment lines over the whole channel. */
    private static void channel(GuiGraphics g, double[] r, String strip, boolean tall, double frac, boolean fromRight, int segs, boolean mana) {
        double fw = r[2] * clamp(frac), fx = fromRight ? r[0] + r[2] - fw : r[0];
        int px = x(fx), pw = x(fx + fw) - px, py = y(r[1]), ph = y(r[1] + r[3]) - py;
        if (pw > 0) { use(g, VANILLA); g.blit(VANILLA, px, py, 0, vanStripV(strip, tall), pw, ph, VAN_W, VAN_H); }
        use(g, FILL);
        if (mana) for (int i = 0; i < Math.round(fw / 3); i++) {
            double hh = Math.sin((fx + i * 12.9898) * 78.233) * 43758.5453, u = hh - Math.floor(hh), v = hh * 7.13 - Math.floor(hh * 7.13);
            int sx = x(fx + u * fw), sy = y(r[1] + v * r[3]);
            g.fill(sx, sy, sx + 2, sy + 2, i % 5 != 0 ? argb(0.7, 0xDCE6FF) : argb(0.95, 0xFFFFFF));
        }
        for (int k = 1; k < segs; k++) {
            double sx = r[0] + r[2] * k / segs - 0.15;
            g.fill(x(sx), py, x(sx + 0.3), py + ph, argb(0.55, 0x000000));
        }
    }

    // ---- The rows ----

    /** Hearts and food (or the mount's hearts) over the gauge, armor and breath above them. Returns the stack's top. */
    private static double rows(GuiGraphics g, HudFrame f, double gaugeTop) {
        double y0 = gaugeTop - GAP - 9;
        dim = f.nums ? DIM : 1;
        double left = hearts(g, f, y0), right = y0;
        if (f.mount != null) right = mountRow(g, f, y0);
        else foodRow(g, f, y0);
        if (f.showArmor) {
            for (int i = 0; i < 10; i++) icon(g, "armor_" + kind(f.armor, i), X0 + 8 * i, left - 10, 1);
            left -= 10;
        }
        boolean breath = breathRow(g, f, right - 10);
        dim = 1;
        if (f.nums) {
            double max = f.maxHp;
            numbers(g, X0, left + (f.showArmor ? 10 : 0), y0, f.absorb > 0 ? Math.round(f.hp) + "+" + Math.round(f.absorb) + "/" + Math.round(max)
                    : Math.round(f.hp) + "/" + Math.round(max), f.healthLiquid);
            if (f.mount != null) numbers(g, X1 - ROW_W, right, y0, Math.round(f.mountHp) + "/" + Math.round(f.mountMax), "mount");
            else numbers(g, X1 - ROW_W, y0, y0, f.food + "/20", "broth");
            if (f.showArmor) numbers(g, X0, left, left, f.armor + "/" + (f.armor > 20 ? 30 : 20), "armor");
            if (breath && f.tank == null) numbers(g, X1 - ROW_W, right - 10, right - 10, (int) Math.ceil(f.airShown * 10) + "/10", "air");
        }
        return Math.min(left, breath ? right - 10 : right);
    }
    /** A row's value / max centred on its rows (from the top row's y to the bottom row's), over the dimmed icons. */
    private static void numbers(GuiGraphics g, double x0, double topY, double bottomY, String s, String tint) {
        tinted(g, s, (x0 + ROW_W / 2) * K, ((topY + bottomY + 9) / 2 - 50) * K, tint);
    }

    /** Hearts from X0, the lowest row's top at y0 (the page's vanHearts): ceil(max / 2) containers, then the absorption
     *  hearts, 10 a row, rows rising by max(10 - (rows - 2), 3); the low health shake. Overlays: the damage flash, the
     *  held food preview (AppleSkin), the natural heal pulse, Comfort's sheen, Regeneration's motes. Returns the top row's y. */
    private static double hearts(GuiGraphics g, HudFrame f, double y0) {
        double max = f.maxHp;
        int hp = (int) Math.ceil(f.hp), absorb = (int) Math.ceil(f.absorb);
        int n = (int) Math.ceil(max / 2), total = n + (absorb + 1) / 2, rows = (total + 9) / 10, rh = Math.max(10 - (rows - 2), 3);
        double flash = f.hurtSince < 400 ? 0.55 * (1 - f.hurtSince / 400.0) : 0, heal = f.healPulse();
        int preview = f.apple != null && f.apple[1] > 0 ? (int) Math.min(max, hp + f.apple[1]) : hp;
        boolean comfort = f.comfort && f.saturation <= 0 && f.hp < max;
        double sweep = ((f.now / 2600.0) % 1) * 92 - 6, pulse = 0.35 + 0.25 * Math.sin(f.now / 160.0);
        int tick = f.p.tickCount;
        for (int i = total - 1; i >= 0; i--) {
            double x = X0 + 8 * (i % 10), y = y0 - rh * (i / 10);
            if (f.hp + absorb <= 4) y += Math.floor(rand(i, tick) * 2);
            icon(g, "heart_empty", x, y, 1);
            if (i >= n) {   // absorption: gold hearts after the containers
                icon(g, "heart_absorb_" + (absorb - 2 * (i - n) >= 2 ? "full" : "half"), x, y, 1);
                continue;
            }
            String k = kind(hp, i);
            if (!k.equals("empty")) icon(g, "heart_" + f.healthLiquid + "_" + k, x, y, 1);
            icon(g, "glow_flash", x, y, flash);
            if (preview > hp && !kind(preview, i).equals(k)) icon(g, "heart_" + f.healthLiquid + "_" + kind(preview, i), x, y, pulse);
            if (!k.equals("empty")) {
                String side = k.equals("half") ? "left" : "full";
                icon(g, "glow_heal_" + side, x, y, heal);
                if (comfort) icon(g, "glow_comfort_" + side, x, y, Math.max(0, 1 - Math.abs(x + 4.5 - X0 - sweep) / 8) * 0.55);
            }
        }
        if (f.regen) for (int r = 0; r < rows; r++) regen(g, f.now, y0 - rh * r, Math.min(10, Math.max(0, hp / 2.0 - 10 * r)) * 8, r);
        return y0 - rh * (rows - 1);
    }
    /** Regeneration on a heart row (top y) over w (the filled hearts): the meters' pink motes drifting left to right. */
    private static void regen(GuiGraphics g, long t, double y, double w, int seed) {
        if (w <= 0) return;
        for (int i = 0; i < Math.ceil(w / 9); i++) {
            double hh = Math.sin((i + 1 + seed * 13) * 91.7) * 43758.5453, u = hh - Math.floor(hh), k = ((t / 1500.0) * (0.6 + u * 0.6) + u * 3.7) % 1;
            double py = y + 5.4 + 2.2 * Math.sin(i * 2.3 + t / 300.0);
            dot(g, (X0 + 0.5 + k * (w - 1)) * K, (py - 50) * K, (0.5 + (i % 3) * 0.12) * K, argb(Math.sin(Math.PI * k) * 0.95 * dim, 0xFF78AF));
        }
    }
    /** Food from X1, right to left (the page's vanFoodRow): the Hunger effect's spoiled food, the shake at saturation 0.
     *  Overlays: the held food preview, the saturation outline, Nourishment's halo; AppleSkin's exhaustion strip under it. */
    private static void foodRow(GuiGraphics g, HudFrame f, double y0) {
        String meat = f.hunger ? "_hunger" : "";
        double sat = Math.min(f.saturation, f.food);
        int tick = f.p.tickCount, preview = f.apple != null && f.apple[0] > 0 ? Math.min(20, f.food + (int) f.apple[0]) : f.food;
        boolean shake = f.saturation <= 0 && tick % (f.food * 3 + 1) == 0;
        double pulse = 0.35 + 0.25 * Math.sin(f.now / 160.0), halo = 0.55 + 1.8 * (f.nourishGlow() - 0.16);
        for (int i = 0; i < 10; i++) {
            double x = X1 - 9 - 8 * i, y = y0 + (shake ? Math.floor(rand(i, tick) * 3) - 1 : 0);
            String k = kind(f.food, i);
            icon(g, "food_empty", x, y, 1);
            if (!k.equals("empty")) icon(g, "food_" + k + meat, x, y, 1);
            if (preview > f.food && !kind(preview, i).equals(k)) icon(g, "food_" + kind(preview, i) + meat, x, y, pulse);
            String sk = kind(sat, i);
            if (!sk.equals("empty")) {
                icon(g, "sat_" + sk, x, y, 1);
                if (f.nourished) icon(g, "halo_" + (sk.equals("half") ? "right" : "full"), x, y, halo);
            }
        }
        double sy = y0 + 9 + 0.5, frac = 1 - clamp(f.exhaustShown);   // drains toward the right end
        use(g, FILL);
        g.fill(x(X1 - ROW_W), y(sy), x(X1), y(sy + 0.6), EXH_TRACK);
        if (frac > 0.001) g.fill(x(X1 - ROW_W * frac), y(sy), x(X1), y(sy + 0.6), EXH_FILL);
    }
    /** The mount's hearts in place of food: up to 30, 10 a row from X1, rows 10 apart, in the mount's emerald. */
    private static double mountRow(GuiGraphics g, HudFrame f, double y0) {
        int n = (int) Math.min(30, Math.ceil(f.mountMax / 2)), v = (int) Math.ceil(f.mountHp);
        for (int i = 0; i < n; i++) {
            double x = X1 - 9 - 8 * (i % 10), y = y0 - 10 * (i / 10);
            icon(g, "heart_empty", x, y, 1);
            String k = kind(v, i);
            if (!k.equals("empty")) icon(g, "heart_mount_" + k, x, y, 1);
        }
        return y0 - 10 * ((n + 9) / 10 - 1);
    }
    /** The breath row at y: bubbles by vanilla's rule (full, then the popping one; 0.75 right, so their edge meets the
     *  food's), or while a breathing gear supplies air its copper tanks with the mm:ss time right-aligned to the food's
     *  edge, the tanks to its left after a 3 px gap (as many as fit, filled in halves). Returns true when shown. */
    private static boolean breathRow(GuiGraphics g, HudFrame f, double y) {
        if (!f.showBreath) return false;
        if (f.tank != null && f.tankShown > 0) {
            int s = (int) Math.ceil(f.tank[0]);
            String txt = String.format("%02d:%02d", s / 60, s % 60);
            double tw = 4 * txt.length() - 1, right = X1 - 1 - tw - 3;
            int n = (int) Math.floor((right - (X1 - ROW_W) - 9) / 8) + 1;
            double v = Math.ceil(f.tankShown / Math.max(1, f.tank[1]) * 2 * n);
            for (int i = 0; i < n; i++) icon(g, "tank_" + kind(v, i), right - 9 - 8 * i, y, 1);
            for (int pass = 0; pass < 2; pass++) {   // the shadow one font px down-right, then the tank's tint
                double off = pass == 0 ? 1 : 0;
                for (int i = 0; i < txt.length(); i++) {
                    use(g, VANILLA);
                    g.blit(VANILLA, x(X1 - 1 - tw + 4 * i + off), y(y + 2.75 + off), vanGlyphU(txt.charAt(i)), pass == 0 ? VAN_GLYPH_BLACK_V : VAN_GLYPH_TINT_V,
                            3 * K, 5 * K, VAN_W, VAN_H);
                }
            }
            return true;
        }
        double a = f.airShown * 300;
        int full = (int) Math.ceil((a - 2) * 10 / 300), pop = (int) Math.ceil(a * 10 / 300) - full;
        for (int i = 0; i < full + pop; i++) icon(g, i >= full ? "air_burst" : "air", X1 - 9 - 8 * i + 0.75, y, 1);
        return true;
    }
}
