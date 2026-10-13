package dev.frosthaven.havencreateuiux.hud;

import static dev.frosthaven.havencreateuiux.hud.CompactAtlas.*;
import static dev.frosthaven.havencreateuiux.hud.HudDraw.*;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Orb mode (the page's hud-compact-orbs.js): two glass orbs in the wing plates' housings. Left: health (absorption above
 * it) and food (or the mount's health) split down the middle, armor as bolted plates on the bezel, the statuses as
 * effects in the glass, exhaustion as a soft crescent glow on the food half's edge. Right: the mana pools (the primary on the left half, one
 * mod fills the orb), breath / the backtank / Helium Flamingo / an aircraft as a smooth ring. Drawn from the orbs atlas
 * in the page's order: bezel, ring, glass, liquids, divider, glass front, glow, effects, numbers.
 */
final class OrbHud {
    /** The orb in its own units (r, housing...), drawn S x around (LX | RX, OY): twice the hotbar's scale. */
    static final double R = 13, LX = -127.5, RX = 127.5, OY = 17, HOUSING = 15.5, S = 2;
    private static final double TAU = Math.PI * 2, TOP = -Math.PI / 2;

    private OrbHud() {}

    /** A liquid filling part of a half: frac of the orb, its atlas name, a wave amplitude (0: still), saturation. */
    record Fill(String liquid, double frac, double wave, double sat) {}

    private static double tx(double u) { return u * K; }
    private static double ty(double v) { return (v - 50) * K; }
    static double level(double frac) { return OY + R - 2 * R * clamp(frac); }
    private static double wave(double top, double a, long t, double x) {
        return top + a * Math.sin(x * 0.9 + t / 180.0) + a * 0.5 * Math.sin(x * 2.1 - t / 180.0 * 1.7);
    }

    static void draw(GuiGraphics g, HudFrame f) {
        side(g, -1, f.side, () -> scaled(g, LX, () -> left(g, f)));
        side(g, 1, f.side, () -> scaled(g, RX, () -> right(g, f)));
        // Numbers and the tank's time doubled with the orbs (readable at the bigger size).
        if (f.nums && f.hurtable) side(g, -1, f.side, () -> scaled(g, LX, () -> numbers(g, LX, new String[][] {
                {Math.round(f.hp) + "/" + Math.round(f.maxHp), f.healthLiquid},
                f.mount != null ? new String[] {Math.round(f.mountHp) + "/" + Math.round(f.mountMax), "mount"} : new String[] {f.food + "/20", "broth"}})));
        var pools = f.pools();
        if (f.nums && !pools.isEmpty()) {
            String[][] lines = new String[pools.size()][];
            for (int i = 0; i < pools.size(); i++) {
                var p = pools.get(i);
                lines[i] = new String[] {Math.round(p.cur()) + "/" + Math.round(p.max()), p.name()};
            }
            side(g, 1, f.side, () -> scaled(g, RX, () -> numbers(g, RX, lines)));
        }
        // The backtank's time, always on (players rely on it), above the right orb's housing.
        if (f.showBreath && f.tank != null && f.tank[0] > 0) {
            String t = HudFrame.mmss(f.tank[0]);
            side(g, 1, f.side, () -> scaled(g, RX, () -> text(g, t, tx(RX) - textWidth(t, 0.6) / 2, ty(OY - HOUSING - 4.2), 0.6, tintV("tank"), tintV("black"))));
        }
    }

    /** Draws an orb S x around its centre (ox, OY). */
    private static void scaled(GuiGraphics g, double ox, Runnable draw) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(tx(ox), ty(OY), 0);
        pose.scale((float) S, (float) S, 1);
        pose.translate(-tx(ox), -ty(OY), 0);
        try { draw.run(); } finally { pose.popPose(); }
    }

    // ---- Left: health, food (or the mount), armor ----
    private static void left(GuiGraphics g, HudFrame f) {
        double cx = tx(LX), cy = ty(OY);
        if (!f.hurtable) { orbCell(g, "cog", cx, cy); return; }   // creative: a cog pressed into the housing's face
        double hp = f.hp / f.maxHp;
        Fill health = new Fill(f.healthLiquid, hp, f.hp <= 4 ? 0.35 : 0, 0);
        Fill food = f.mount != null ? new Fill("mount", f.mountHp / f.mountMax, 0, 0)
                : new Fill(f.hunger ? "hunger" : "broth", f.food / 20.0, f.food <= 6 ? 0.35 : 0,
                        f.food > 0 ? Math.min(f.saturation, f.food) / 20.0 : 0);
        Fill over = f.absorb > 0 ? new Fill("absorb", hp + f.absorb / f.maxHp, 0, hp) : null;   // sat: absorption's start
        orbCell(g, "bezelL", cx, cy);
        if (f.showArmor) armorRing(g, cx, cy, Math.min(1, f.armor / 20.0));
        orbCell(g, "glassL", cx, cy);
        half(g, f, LX, LX - R, R, health, over);
        half(g, f, LX, LX, R, food, null);
        orbCell(g, "divider", cx, cy);
        orbCell(g, "front", cx, cy);
        glow(g, LX, LX - HOUSING, HOUSING, health);
        glow(g, LX, LX, HOUSING, food);
        effects(g, f, hp, f.mount != null);
    }

    // ---- Right: the mana pools, the ring ----
    private static void right(GuiGraphics g, HudFrame f) {
        double cx = tx(RX), cy = ty(OY);
        String ring = f.showBreath ? (f.tank != null && f.tankShown > 0 ? "tank" : "air") : f.flamingo != null ? "flamingo" : f.hull != null ? "hull" : null;
        double ringFrac = ring == null ? 0 : switch (ring) {
            case "tank" -> f.tankShown / Math.max(1, f.tank[1]);
            case "air" -> f.airShown;
            case "flamingo" -> f.flamingo;
            default -> f.hull;
        };
        var pools = f.pools();
        if (pools.isEmpty() && ring == null) { orbCell(g, "cog", cx, cy); return; }
        orbCell(g, "bezelR", cx, cy);
        if (ring != null) {
            double[] c = CompactAtlas.ring(ring);
            sector(g, ORBS, ORBS_W, ORBS_H, c[0], c[1], cx, cy, RING_HALF, TOP, TOP + TAU * clamp(ringFrac));
        }
        orbCell(g, "glassR", cx, cy);
        if (!pools.isEmpty()) {
            boolean split = pools.size() > 1;
            Fill a = new Fill(pools.get(0).name(), pools.get(0).frac(), 0, 0);
            half(g, f, RX, RX - R, split ? R : 2 * R, a, null);
            if (split) half(g, f, RX, RX, R, new Fill(pools.get(1).name(), pools.get(1).frac(), 0, 0), null);
            if (split) orbCell(g, "divider", cx, cy);
            orbCell(g, "front", cx, cy);
            glow(g, RX, RX - HOUSING, split ? HOUSING : 2 * HOUSING, a);
            if (split) glow(g, RX, RX, HOUSING, new Fill(pools.get(1).name(), pools.get(1).frac(), 0, 0));
        } else orbCell(g, "front", cx, cy);
    }

    /** Armor: ten bolted plates, the full ones (and a half one for an odd value), revealed by angle. */
    private static void armorRing(GuiGraphics g, double cx, double cy, double frac) {
        double lit = frac * 10;
        for (int k = 0; k < 10 && lit - k > 0; k++) {
            double[] c = CompactAtlas.ring(lit - k >= 1 ? "armor" : "armorHalf");
            sector(g, ORBS, ORBS_W, ORBS_H, c[0], c[1], cx, cy, RING_HALF, TOP + k * TAU / 10, TOP + (k + 1) * TAU / 10);
        }
    }

    /** One half (x0, width w, units) of the orb at ox: the liquid to its level (a rolling wave at low health or food),
     *  saturation, bubbles, absorption above it, the surface catching the light. */
    private static void half(GuiGraphics g, HudFrame f, double ox, double x0, double w, Fill fill, Fill over) {
        if (fill.frac() <= 0) return;
        double cx = tx(ox), cy = ty(OY), r = R * K, top = level(fill.frac());
        double[] cell = cell(fill.liquid());
        boolean broth = fill.liquid().equals("broth") || fill.liquid().equals("hunger");
        if (fill.wave() > 0) {
            for (double x = x0; x < x0 + w - 1e-6; x += 0.25) {
                double s = wave(top, fill.wave(), f.now, x + 0.125);
                orbPiece(g, cell, cx, cy, tx(x) - cx, ty(s) - cy, tx(Math.min(x + 0.25, x0 + w)) - cx, r);
            }
        } else orbPiece(g, cell, cx, cy, tx(x0) - cx, ty(top) - cy, tx(x0 + w) - cx, r);
        if (fill.sat() > 0) {
            double ys = level(fill.sat());
            orbPiece(g, cell(fill.liquid() + "Sat"), cx, cy, tx(x0) - cx, ty(ys) - cy, tx(x0 + w) - cx, r);
        }
        if (broth) {   // the broth's rising bubbles (seeded by the orb's position), under its surface
            for (int i = 0; i < 7; i++) {
                double h = Math.sin((ox + i * 9.17) * 33.1) * 43758.5453, u = h - Math.floor(h), v = h * 5.3 - Math.floor(h * 5.3);
                double bx = ox - R + u * 2 * R, by = top + 1 + v * (OY + R - top - 2), br = 0.35 + (i % 3) * 0.15;
                double surf = fill.wave() > 0 ? wave(top, fill.wave(), f.now, bx) : top;
                if (bx < x0 || bx > x0 + w || by < surf || Math.hypot(bx - ox, by - OY) > R - br) continue;
                ring(g, tx(bx), ty(by), br * K, argb(0.55, 0xFFE6BE));
            }
        }
        boolean absorbing = over != null && over.frac() > over.sat();
        if (absorbing) {   // absorption: golden cloud from the health's level up, a few sparkles
            double ya = level(over.frac()), yb = level(over.sat());
            orbPiece(g, cell("absorb"), cx, cy, tx(x0) - cx, ty(ya) - cy, tx(x0 + w) - cx, ty(yb) - cy);
            for (int i = 0; i < 9; i++) {
                double h = Math.sin((ox + i * 7.31) * 51.7) * 43758.5453, u = h - Math.floor(h), v = h * 3.7 - Math.floor(h * 3.7);
                double sz = i % 3 != 0 ? 0.3 : 0.45, sx = x0 + u * w, sy = ya + v * (yb - ya);
                discFill(g, cx, cy, r, tx(sx), tx(sx + sz), ty(sy), ty(sy + sz), i % 3 != 0 ? argb(0.8, 0xFFFAD2) : argb(0.95, 0xFFFFFF));
            }
            discFill(g, cx, cy, r, tx(x0), tx(x0 + w), ty(ya), ty(ya + 0.4), argb(0.35, 0xFFFFFF));
        } else if (fill.wave() > 0) {   // the moving surface catching the light
            for (double x = x0; x < x0 + w - 1e-6; x += 0.25) {
                double s = wave(top, fill.wave(), f.now, x + 0.125);
                discFill(g, cx, cy, r, tx(x), tx(Math.min(x + 0.25, x0 + w)), ty(s - 0.2), ty(s + 0.2), argb(0.35, 0xFFFFFF));
            }
        } else discFill(g, cx, cy, r, tx(x0), tx(x0 + w), ty(top), ty(top + 0.4), argb(0.35, 0xFFFFFF));
    }

    /** The liquid's glow on the rim's inner edge, below its level less 1.5 (screen blended). */
    private static void glow(GuiGraphics g, double ox, double x0, double w, Fill fill) {
        if (fill.frac() <= 0) return;
        double cx = tx(ox), cy = ty(OY), y0 = Math.max(-CELL_HALF, ty(level(fill.frac()) - 1.5) - cy);
        String name = "glow_" + fill.liquid();
        screen(() -> orbPiece(g, cell(name), cx, cy, tx(x0) - cx, y0, tx(x0 + w) - cx, CELL_HALF));
    }

    /** The health orb's statuses (the page's ccOrbFx), in the glass. */
    private static void effects(GuiGraphics g, HudFrame f, double hp, boolean riding) {
        double cx = tx(LX), cy = ty(OY), r = R * K, gl = LX - R, gm = LX, gr = LX + R, gt = OY - R, gb = OY + R;
        long t = f.now;
        double warn = 0.12 + 0.12 * Math.sin(t / 220.0);   // one pulse for both warnings, so they flash in sync
        if (!riding && f.food <= 6) discFill(g, cx, cy, r, tx(gm), tx(gr), ty(gt), ty(level(f.food / 20.0)), argb(warn, 0xDC7814));
        if (f.hp <= 4) discFill(g, cx, cy, r, tx(gl), tx(gm), ty(gt), ty(level(hp)), argb(warn, 0xC8141E));
        if (f.hurtSince < 400) discFill(g, cx, cy, r, tx(gl), tx(gm), ty(gt), ty(gb), argb(0.55 * (1 - f.hurtSince / 400.0), 0xFF2828));
        if (f.regen && hp > 0) {   // Regeneration: pink motes rising through the health liquid, fading at its surface
            double top = level(hp), depth = gb - top;
            for (int i = 0; i < 10; i++) {
                double h = Math.sin((i + 1) * 91.7) * 43758.5453, u = h - Math.floor(h), sp = 0.6 + (h * 3.1 - Math.floor(h * 3.1)) * 0.6;
                double k = ((t / 1600.0) * sp + u * 3.7) % 1, my = gb - k * depth, mx = gl + 1 + u * (R - 2) + Math.sin(t / 300.0 + i) * 0.4;
                if (Math.hypot(mx - LX, my - OY) < R) dot(g, tx(mx), ty(my), (0.35 + (i % 3) * 0.12) * K, argb(Math.sin(Math.PI * k) * 0.9, 0xFF78AF));
            }
        }
        if (!riding && f.nourished && f.food > 0) {   // Nourishment: the broth glazed gold with slow sparkles, a gold surface
            double top = level(f.food / 20.0);
            discGradient(g, cx, cy, r, tx(gm), tx(gr), ty(top), ty(gb), ty(top), argb(0.28, 0xFFD75A), ty(gb), argb(0.1, 0xFFC83C));
            for (int i = 0; i < 8; i++) {
                double h = Math.sin((i + 3) * 47.3) * 43758.5453, u = h - Math.floor(h), v = h * 2.9 - Math.floor(h * 2.9);
                double sx = gm + 0.6 + u * (R - 1.2), sy = top + 0.8 + v * (gb - top - 1.6);
                discFill(g, cx, cy, r, tx(sx), tx(sx + 0.4), ty(sy), ty(sy + 0.4), argb(0.45 + 0.45 * Math.sin(t / 260.0 + i * 1.7), 0xFFF0A0));
            }
            discFill(g, cx, cy, r, tx(gm), tx(gr), ty(top - 0.15), ty(top + 0.35), argb(0.95, 0xFFDC6E));
        }
        if (f.comfort && f.saturation <= 0 && hp < 1) {   // Comfort: a slow pink glow rising through the health half's glass
            double y = gb + 4 - ((t / 2600.0) % 1) * (2 * R + 8);
            discGradient(g, cx, cy, r, tx(gl), tx(gm), ty(y - 5), ty(y), ty(y - 5), argb(0, 0xFF9EC4), ty(y), argb(0x88 / 255.0, 0xFF9EC4));
            discGradient(g, cx, cy, r, tx(gl), tx(gm), ty(y), ty(y + 5), ty(y), argb(0x88 / 255.0, 0xFF9EC4), ty(y + 5), argb(0, 0xFF9EC4));
        }
        double pulse = (0.3 + 0.25 * Math.sin(t / 160.0)) / 2;   // half the bars' preview: the orb's large area glared
        if (f.apple != null) {   // AppleSkin: what the held food restores, pale and pulsing above the levels
            if (f.apple[1] > 0) discFill(g, cx, cy, r, tx(gl), tx(gm), ty(level(hp + f.apple[1] / f.maxHp)), ty(level(hp)), argb(pulse, 0xFFC8C8));
            if (!riding && f.apple[0] > 0)
                discFill(g, cx, cy, r, tx(gm), tx(gr), ty(level(Math.min(20, f.food + f.apple[0]) / 20.0)), ty(level(f.food / 20.0)), argb(pulse, 0xFFE6B4));
        }
        if (!riding) {   // exhaustion: a soft crescent glow on the food half's edge, full at 0, draining from the top (screen)
            double fr = 1 - clamp(f.exhaustShown);
            double[] c = cell("exhaust");
            if (fr > 0.001) screen(() -> sector(g, ORBS, ORBS_W, ORBS_H, c[0], c[1], cx, cy, CELL_HALF, Math.PI / 2 - Math.PI * fr, Math.PI / 2));
        }
    }

    /** Value / max centred on an orb (two lines when split), the 0.6 digits tinted by liquid. */
    private static void numbers(GuiGraphics g, double ox, String[][] lines) {
        for (int i = 0; i < lines.length; i++) {
            double y = OY - lines.length * 4 / 2.0 + i * 4.2;
            String s = lines[i][0];
            text(g, s, tx(ox) - textWidth(s, 0.6) / 2, ty(y), 0.6, tintV(lines[i][1]), tintV("black"));
        }
    }
}
