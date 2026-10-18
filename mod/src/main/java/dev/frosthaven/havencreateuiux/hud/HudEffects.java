package dev.frosthaven.havencreateuiux.hud;

import static dev.frosthaven.havencreateuiux.hud.CompactAtlas.*;
import static dev.frosthaven.havencreateuiux.hud.HudDraw.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ScreenEvent;

import dev.frosthaven.havencreateuiux.UiConfig;

/**
 * Buffs and debuffs (the page's hud-compact-effects.js), in place of vanilla's top-right icons, where the config's
 * "Buffs and debuffs" puts them ({@link UiConfig.EffectsAnchor}): the screen's top left (the default; {@link #corner}),
 * buffs in the first row and debuffs in the row below as vanilla orders them, at twice the rail's size, following the
 * screen and not the bar's size; or the action bar's area ({@link #draw}): two floating lines left-aligned to the bar's
 * edge, buffs nearest the bar and debuffs (1.5x) above. Each line always-on effects first; a socket per effect with a
 * border draining clockwise from the top (green buff, red debuff) and the time left under it (45, 3m, 1h) in the kind's
 * tint (it does not change as it runs down). Hovering one while a screen is open shows its name and time.
 * Worn gear (GearStatus: MekaSuit energy, jetpack hydrogen, scuba / space suit oxygen, Jet Suit energy, O2 backtanks)
 * adds sockets to the buffs line after the always-on buffs: all cyan, each the worn item as its icon, the border draining
 * with what is left and the percent under it (no sign), cyan; the tooltip names the resource and its amount.
 * The rail is drawn in texels relative to (bar centre, screen bottom); CompactHud's pose maps them to the screen.
 */
final class HudEffects {
    private static final int SIZE = 10 * K, CELL = 13 * K, PER_ROW = 10;
    private static final double ROW_H = 19.5, TIME_K = 1.5;   // TIME_K: the times' digits scale (the bigger bar)
    /** The top-left corner: both kinds at this scale, from (CORNER_X, CORNER_Y) GUI px; a row wraps at half the screen. */
    private static final double CORNER_K = 2, CORNER_X = 2, CORNER_Y = 2;
    /** The longest duration seen per effect: the border shows time left of that. */
    private static final Map<Holder<MobEffect>, Integer> START = new HashMap<>();
    /** Effect boxes on screen (GUI px) from the last frame, for the hover tooltip. */
    private static final List<Object[]> BOXES = new ArrayList<>();

    private HudEffects() {}

    /** The config: buffs and debuffs at the screen's top left (else in the action bar's area). */
    static boolean atCorner() { return UiConfig.EFFECTS_ANCHOR.get() == UiConfig.EffectsAnchor.TOP_LEFT; }

    /** The buffs line (always-on first, then the worn gear, then the timed ones) and the debuffs line. */
    private static List<List<Object>> lines(Player p, List<GearStatus.Gear> gear) {
        List<MobEffectInstance> all = new ArrayList<>();
        for (MobEffectInstance e : p.getActiveEffects()) if (e.showIcon()) all.add(e);
        all.sort((a, b) -> b.compareTo(a));
        START.keySet().removeIf(h -> all.stream().noneMatch(e -> e.getEffect().equals(h)));
        List<Object> green = new ArrayList<>(line(all, false)), red = new ArrayList<>(line(all, true));
        green.addAll(alwaysOn(green), gear);   // after the always-on buffs, before the timed ones
        HudLog.changed("effects", green.size() + "/" + red.size(), "effects: {} buffs, {} debuffs", green.size(), red.size());
        HudLog.changed("gear", gear.stream().map(GearStatus.Gear::id).toList(), "gear statuses (cyan, in the buffs line): {}",
                gear.isEmpty() ? "none" : String.join(", ", gear.stream().map(GearStatus.Gear::id).toList()));
        return List.of(green, red);
    }

    /** The rail in the action bar's area (nothing when the config anchors the effects to the corner).
     *  left: the bar's left edge + 1, top: where the lines start (units; with orbs, clear of the housings). */
    static void draw(GuiGraphics g, Player p, List<GearStatus.Gear> gear, int cx, int bottom, float scale, double left, double startTop) {
        if (atCorner()) return;
        restoreBlend();
        BOXES.clear();
        List<List<Object>> lines = lines(p, gear);
        double top = startTop;
        for (int kind = 0; kind < 2; kind++) {   // from the bar up: buffs, then debuffs at 1.5x on top
            List<Object> list = lines.get(kind);
            double m = kind == 0 ? 1.0 : 1.5;
            int per = (int) (PER_ROW / m);
            for (int r = 0; r * per < list.size(); r++) {
                top -= ROW_H * m;
                for (int i = 0; i < per && r * per + i < list.size(); i++) {
                    Object e = list.get(r * per + i);
                    int x = (int) Math.round(left * K) + (int) (CELL * m * i), y = (int) Math.round((top - 50) * K);
                    socket(g, e, x, y, m);
                    double s = scale / K;
                    BOXES.add(new Object[] {e, cx + x * s, bottom + y * s, SIZE * m * s});
                }
            }
        }
    }

    /** The screen's top left (the config's default; nothing otherwise): buffs in the first row, debuffs in the row below,
     *  both at CORNER_K, the time under each socket; a row longer than half the screen wraps downward. Drawn in GUI px
     *  (the bar's size undone: the corner follows the screen); the caller's pose is CompactHud's sized one. */
    static void corner(GuiGraphics g, Player p, List<GearStatus.Gear> gear, int cx, int bottom, float scale, int guiWidth) {
        if (!atCorner()) return;
        restoreBlend();
        BOXES.clear();
        List<List<Object>> lines = lines(p, gear);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, bottom, 0);
        pose.scale(1 / scale, 1 / scale, 1);
        pose.translate(-cx, -bottom, 0);
        pose.scale(1f / K, 1f / K, 1);
        double m = CORNER_K, top = CORNER_Y;
        int per = Math.max(1, (int) ((guiWidth / 2.0 - CORNER_X) / (CELL / (double) K * m)));
        for (List<Object> list : lines) {   // from the top down: buffs, then debuffs
            for (int r = 0; r * per < list.size(); r++) {
                for (int i = 0; i < per && r * per + i < list.size(); i++) {
                    Object e = list.get(r * per + i);
                    int x = (int) Math.round(CORNER_X * K + CELL * m * i), y = (int) Math.round(top * K);
                    socket(g, e, x, y, m);
                    BOXES.add(new Object[] {e, x / (double) K, y / (double) K, SIZE * m / K});
                }
                top += ROW_H * m;
            }
        }
        pose.popPose();
    }

    /** One socket at (x, y) texels, m times the rail's size: a gear status or an effect. */
    private static void socket(GuiGraphics g, Object e, int x, int y, double m) {
        if (e instanceof GearStatus.Gear su) suit(g, su, x, y, m);
        else effect(g, (MobEffectInstance) e, x, y, m);
    }

    /** How many of a buffs line are always-on (they lead it). */
    private static int alwaysOn(List<Object> line) {
        int n = 0;
        for (Object o : line) if (o instanceof MobEffectInstance e && (e.isInfiniteDuration() || e.isAmbient())) n++;
        return n;
    }

    /** A gear socket: cyan frame, the worn item's icon, the border draining with what is left, the percent. */
    private static void suit(GuiGraphics g, GearStatus.Gear su, int x, int y, double m) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale((float) m, (float) m, 1);
        use(g, PARTS);
        g.blit(PARTS, 0, 0, EFFECT_SUIT_U, EFFECT_V, SIZE, SIZE, PARTS_SIZE, PARTS_SIZE);
        if (su.icon() != null) {   // the item, 16 px, in the effect icon's 47-texel box at (17, 17)
            pose.pushPose();
            pose.translate(17, 17, 0);
            pose.scale(47 / 16f, 47 / 16f, 1);
            use(g, ITEM);
            g.renderItem(su.icon(), 0, 0);
            pose.popPose();
        }
        border(g, su.frac(), 0xFF3FE0FF, 0xF20A2832);
        String t = String.valueOf(Math.round(su.frac() * 100));   // percent, no sign
        pose.pushPose();
        pose.translate(SIZE / 2f, SIZE + 8, 0);
        pose.scale((float) TIME_K, (float) TIME_K, 1);
        HudDraw.digits(g, t, -HudDraw.digitsWidth(t) / 2, 0, t.length(), CYAN, DIM);
        pose.popPose();
        pose.popPose();
    }

    /** A line's effects: always-on first (infinite or a beacon's), then the timed ones. */
    private static List<MobEffectInstance> line(List<MobEffectInstance> all, boolean harmful) {
        List<MobEffectInstance> out = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            for (MobEffectInstance e : all) {
                boolean bad = e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL, always = e.isInfiniteDuration() || e.isAmbient();
                if (bad == harmful && always == (pass == 0)) out.add(e);
            }
        }
        return out;
    }

    private static void effect(GuiGraphics g, MobEffectInstance e, int x, int y, double m) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale((float) m, (float) m, 1);
        boolean bad = e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL, ambient = e.isAmbient();
        use(g, PARTS);
        g.blit(PARTS, 0, 0, ambient ? EFFECT_AMBIENT_U : bad ? EFFECT_DEBUFF_U : EFFECT_BUFF_U, EFFECT_V, SIZE, SIZE, PARTS_SIZE, PARTS_SIZE);
        use(g, EFFECT);
        g.blit(17, 17, 0, 47, 47, Minecraft.getInstance().getMobEffectTextures().get(e.getEffect()));
        if (ambient) { use(g, FILL); g.fill(0, 0, SIZE, SIZE, 0x59101216); }
        else {
            int dur = e.isInfiniteDuration() ? -1 : e.getDuration();
            int start = dur < 0 ? 1 : START.merge(e.getEffect(), dur, Math::max);
            border(g, dur < 0 ? 1 : dur / (double) start, bad ? 0xFFFF4E44 : 0xFF62E07A, bad ? 0xF2460E0C : 0xF2143C1A);
            if (dur >= 0) {
                String t = time(dur / 20);
                pose.pushPose();
                pose.translate(SIZE / 2f, SIZE + 8, 0);
                pose.scale((float) TIME_K, (float) TIME_K, 1);
                HudDraw.digits(g, t, -HudDraw.digitsWidth(t) / 2, 0, t.length(), bad ? DEBUFF : BUFF, DIM);   // the kind's tint all the way down
                pose.popPose();
            }
        }
        pose.popPose();
    }

    /** The time border: a dark track around the socket, then the time left clockwise from the top centre. */
    private static void border(GuiGraphics g, double frac, int glow, int track) {
        int a = 6, b = SIZE - 6, t = 7, mid = SIZE / 2;
        int[][] segs = {{mid, a, b, a}, {b, a, b, b}, {b, b, a, b}, {a, b, a, a}, {a, a, mid, a}};   // clockwise
        for (int pass = 0; pass < 2; pass++) {
            double left = pass == 0 ? Double.MAX_VALUE : frac * 4 * (b - a);
            for (int[] s : segs) {
                double len = Math.abs(s[2] - s[0]) + Math.abs(s[3] - s[1]);
                double d = Math.min(len, left);
                if (d <= 0) break;
                int x0 = s[0], y0 = s[1], x1 = (int) (s[0] + Math.signum(s[2] - s[0]) * d), y1 = (int) (s[1] + Math.signum(s[3] - s[1]) * d);
                use(g, FILL);
                g.fill(Math.min(x0, x1) - t / 2, Math.min(y0, y1) - t / 2, Math.max(x0, x1) + t / 2 + 1, Math.max(y0, y1) + t / 2 + 1, pass == 0 ? track : glow);
                left -= d;
            }
        }
    }

    /** 45 (seconds), 3m (minutes), 1h (hours). */
    static String time(int sec) {
        return sec < 60 ? String.valueOf(sec) : sec < 3600 ? sec / 60 + "m" : sec / 3600 + "h";
    }

    /** While a screen is open: the hovered effect's name, level and time (drawn after the screen, on top). */
    static void onScreenRender(ScreenEvent.Render.Post event) {
        double mx = event.getMouseX(), my = event.getMouseY();
        for (Object[] box : BOXES) {
            double x = (double) box[1], y = (double) box[2], s = (double) box[3];
            if (mx < x || mx > x + s || my < y || my > y + s) continue;
            var mc = Minecraft.getInstance();
            if (box[0] instanceof GearStatus.Gear su) {
                event.getGuiGraphics().renderComponentTooltip(mc.font, List.of(su.name(), su.text()), (int) mx, (int) my);
                return;
            }
            MobEffectInstance e = (MobEffectInstance) box[0];
            Component name = e.getEffect().value().getDisplayName().copy();
            if (e.getAmplifier() > 0) name = name.copy().append(" ").append(Component.translatable("enchantment.level." + (e.getAmplifier() + 1)));
            Component time = e.isInfiniteDuration() ? Component.translatable("effect.duration.infinite")
                    : MobEffectUtil.formatDuration(e, 1.0f, mc.level == null ? 20f : mc.level.tickRateManager().tickrate());
            event.getGuiGraphics().renderComponentTooltip(mc.font, List.of(name, time), (int) mx, (int) my);
            return;
        }
    }
}
