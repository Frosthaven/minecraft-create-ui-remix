package dev.frosthaven.havencreateuiux.hud;

import static dev.frosthaven.havencreateuiux.hud.CompactAtlas.*;
import static dev.frosthaven.havencreateuiux.hud.HudDraw.*;

import dev.frosthaven.havencreateuiux.UiConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Contextual and stateful key hints (KeyRules decides the rows): no plate, words in the game font with a black outline, keys in
 * small caps (the chips' dark cap with a grey edge, the key in the bar's digit glyphs: SHIFT, CTRL, ALT, M1 to M5).
 * Contextual key hints: one line centerd over the bar's top (above the chips), "[keys] words" per action. Stateful key hints: bottom right,
 * a heading per thing (vehicle, held item, worn gear), then rows "[state] words" right-aligned against one column of keys
 * at the edge. GUI px. Only while the bar is active, no screen is open and the HUD is shown; each part has its client config
 * switch (UiConfig.KEY_PROMPTS, KEY_HINTS).
 */
public final class KeyHintsHud {
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath("haven_create_ui_ux", "key_hints");
    /** The bar's top (GUI px, the chips' top when there are chips), set by CompactHud.instruments each frame. */
    static float barTop = Float.NaN;

    /** Cap glyph scale (texels of the digit strip: 25 tall, so 5 GUI px), padding and edge (texels). */
    private static final float GS = 1.6f;
    private static final int PAD = 2 * K, EDGE = K;
    private static final int CAP_H = Math.round((DIGIT_H * GS + 2 * PAD) / (float) K);   // 7 GUI px
    private static final int ROW = 10, GAP = 2, PROMPT_GAP = 12, SPLIT = 4;
    private static final int TEXT = 0xFFF4EFE2, BRASS = 0xFFEAD092, DIM = 0xFFA99F8A, OUTLINE = 0xFF000000;

    private KeyHintsHud() {}

    private static int cachedTick = -1;
    private static List<KeyRules.Row> cachedPrompts;
    private static List<KeyRules.Group> cachedHints;

    /** The Escape menu: the global keys in the stateful key hints' place, over the menu. */
    public static void onScreenRender(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(event.getScreen() instanceof net.minecraft.client.gui.screens.PauseScreen) || mc.player == null || !UiConfig.KEY_HINTS.get() || !CompactHud.active()) return;
        try {
            dev.frosthaven.havencreateuiux.HudLayers.drawingOwn(true);   // Dark Mode Everywhere darkens screen draws: not ours
            beginOrder();
            List<KeyRules.Group> global = KeyRules.global();
            HudLog.changed("key-pause", global.stream().map(h -> describe(h.rows())).toList().toString(), "key hints (Escape menu): {}", global.stream().map(h -> describe(h.rows())).toList());
            hints(event.getGuiGraphics(), mc.font, global);
            List<KeyRules.Group> left = KeyRules.globalLeft();
            HudLog.changed("key-pause-left", left.stream().map(h -> describe(h.rows())).toList().toString(), "key hints (Escape menu, left): {}", left.stream().map(h -> describe(h.rows())).toList());
            hintsLeft(event.getGuiGraphics(), mc.font, left);
            use(event.getGuiGraphics(), FILL);
            drawBatched(event.getGuiGraphics());
        } catch (Throwable t) {
            HudLog.error("key-hints-pause:" + t, "drawing the Escape menu's key hints failed (logged once per kind)", t);
        } finally {
            dev.frosthaven.havencreateuiux.HudLayers.drawingOwn(false);
        }
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null || !CompactHud.active()) return;
        try {
            beginOrder();
            // the rules (reflection, entity scans) once per client tick: what they read changes at most that often
            int tick = mc.gui.getGuiTicks();
            if (tick != cachedTick || cachedPrompts == null) {
                cachedTick = tick;
                cachedPrompts = UiConfig.KEY_PROMPTS.get() ? KeyRules.prompts(mc, mc.player) : List.of();
                cachedHints = UiConfig.KEY_HINTS.get() ? KeyRules.hints(mc, mc.player) : List.of();
                List<KeyRules.Row> pr = cachedPrompts; List<KeyRules.Group> hi = cachedHints;
                HudLog.changed("key-prompts", describe(pr), "key prompts: {}", describe(pr));
                HudLog.changed("key-hints", hi.stream().map(h -> h.title() + " " + describe(h.rows())).toList().toString(), "key hints: {}",
                        hi.stream().map(h -> h.title() + " " + describe(h.rows())).toList());
            }
            List<KeyRules.Row> prompts = cachedPrompts;
            List<KeyRules.Group> hints = cachedHints;
            float top = Float.isNaN(barTop) ? g.guiHeight() - 40 : barTop;
            if (!prompts.isEmpty()) prompts(g, mc.font, prompts, top);
            if (!hints.isEmpty()) hints(g, mc.font, hints);
            use(g, FILL);
            drawBatched(g);
        } catch (Throwable t) {
            HudLog.error("key-hints:" + t, "drawing the key prompts or hints failed (skipping this frame; logged once per kind)", t);
        }
    }

    private static String describe(List<KeyRules.Row> rows) {
        List<String> out = new ArrayList<>();
        for (KeyRules.Row r : rows) out.add(String.join(r.either() ? "/" : "+", caps(r)) + " " + r.label() + (r.off() ? " (off)" : "")
                + (r.state() == null ? "" : " [" + r.state() + "]"));
        return out.toString();
    }

    /** One line centered over the bar: each action's caps, then its words; SPLIT * 4 px between actions. */
    private static void prompts(GuiGraphics g, Font font, List<KeyRules.Row> rows, float top) {
        int total = -SPLIT * 4;
        for (KeyRules.Row r : rows) total += capsWidth(caps(r)) + 3 + font.width(r.label()) + SPLIT * 4;
        int x = (g.guiWidth() - total) / 2, y = Math.round(top) - PROMPT_GAP - ROW;
        // drawn at hintsScale() about the line's bottom centre (just above the bar), like the legend about its corner
        var pose = g.pose();
        pose.pushPose();
        float cx = g.guiWidth() / 2f, by = Math.round(top) - PROMPT_GAP;
        pose.translate(cx, by, 0);
        pose.scale(hintsScale(), hintsScale(), 1);
        pose.translate(-cx, -by, 0);
        for (KeyRules.Row r : rows) {
            x += drawCaps(g, caps(r), x, y + (ROW - CAP_H) / 2, r.either()) + 3;
            text(g, font, r.label(), x, y + 1, TEXT);
            x += font.width(r.label()) + SPLIT * 4;
        }
        pose.popPose();
    }

    /** Bottom right: the keys column at the right edge, the words right-aligned before it, a dim heading per group. */
    private static void hints(GuiGraphics g, Font font, List<KeyRules.Group> groups) {
        int capsCol = 0, lines = 0;
        for (KeyRules.Group gr : groups) {
            lines++;
            for (KeyRules.Row r : gr.rows()) { capsCol = Math.max(capsCol, capsWidth(caps(r))); lines++; }
        }
        // Above the action bar's right wing (orbs, bars); in Almost Vanilla the corner is free.
        boolean wings = UiConfig.METER_DISPLAY.get() != UiConfig.MeterDisplay.ALMOST_VANILLA;
        int right = g.guiWidth() - 6, bottom = g.guiHeight() - (wings ? (int) Math.ceil(36 * CompactHud.scale()) : 6);
        int y = bottom - lines * (ROW + GAP) + GAP - GROUP_GAP * Math.max(0, groups.size() - 1), wordsRight = right - capsCol - 4;
        // drawn at HINTS_SCALE about the bottom-right corner; top: the stack's top on screen
        int top = Math.round(bottom - (bottom - y) * hintsScale());
        // too many rows for the screen: the stack's top runs past the top edge (logged on change, so tests and players
        // can tell; the rows still draw)
        HudLog.changed("key-hints-tall", lines + " " + (top < 4), top < 4 ? "key hints: {} lines reach past the top of the screen ({} px tall, {} px free)"
                : "key hints: {} lines fit ({} px tall, {} px free)", lines, bottom - top, bottom);
        int mapBottom = minimapBottom(g, right);   // JourneyMap's minimap over the same corner: report an overlap (no layout change)
        if (mapBottom >= 0) HudLog.changed("key-hints-minimap", lines + " " + (top < mapBottom), top < mapBottom
                ? "key hints: the stack's top ({}) overlaps the JourneyMap minimap (its bottom at {}) by {} px"
                : "key hints: the stack's top ({}) is clear of the JourneyMap minimap (its bottom at {}, {} px)", top, mapBottom, Math.abs(mapBottom - top));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(right, bottom, 0);
        pose.scale(hintsScale(), hintsScale(), 1);
        pose.translate(-right, -bottom, 0);
        boolean first = true;
        for (KeyRules.Group gr : groups) {
            if (!first) y += GROUP_GAP;   // a little space above each group but the first
            first = false;
            String title = gr.title().toUpperCase();
            text(g, font, title, right - font.width(title), y + 1, DIM);
            y += ROW + GAP;
            for (KeyRules.Row r : gr.rows()) {
                List<String> c = caps(r);
                drawCaps(g, c, right - capsWidth(c), y + (ROW - CAP_H) / 2, r.either());
                int wx = wordsRight - font.width(r.label());
                text(g, font, r.label(), wx, y + 1, r.off() ? OFF : BRASS);   // red words: shown, cannot act now
                if (r.state() != null) tag(g, r.state(), wx - 3, y + (ROW - CAP_H) / 2, r.red(), r.tint());
                y += ROW + GAP;
            }
        }
        pose.popPose();
    }

    /** The right-side legend's size (the stateful key hints and the Escape menu's keys): 84 % of the prompts' size, so a
     *  full set of worn and held gear stays clear of the screen's top corner. */
    private static final float HINTS_SCALE = 0.84f;

    /** Both key hint parts' size: HINTS_SCALE times the player's "keyHintScale" (1 = the default). */
    static float hintsScale() { return HINTS_SCALE * UiConfig.KEY_HINT_SCALE.get().floatValue(); }
    /** The space above each group of the right-side legend but the first (GUI px, before HINTS_SCALE). */
    private static final int GROUP_GAP = 10;

    /** The bottom edge (GUI units) of JourneyMap's minimap and its info lines when the minimap is shown in a top corner
     *  over the hints' column (its DisplayVars: window pixels / GUI scale), else -1. For the overlap report only. */
    private static int minimapBottom(GuiGraphics g, int right) {
        Object ui = Reflect.getStatic("journeymap.client.ui.UIManager", "INSTANCE");
        Object mini = Reflect.call(ui, "getMinimap");
        Object dv = Reflect.call(mini, "getDisplayVars");
        if (dv == null || !(Reflect.get(Reflect.call(mini, "getCurrentMinimapProperties"), "enabled") instanceof Object en)) return -1;
        if (Reflect.call(en, "get") instanceof Boolean on && !on) return -1;
        String pos = String.valueOf(Reflect.get(dv, "position"));
        if (!pos.startsWith("Top")) return -1;
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        double left = Reflect.num(Reflect.get(dv, "textureX"), 0) / scale, bottom = (Reflect.num(Reflect.get(dv, "textureY"), 0) + Reflect.num(Reflect.get(dv, "minimapHeight"), 0)) / scale;
        if (Reflect.get(dv, "labels") instanceof List<?> labels) for (Object t : labels) {
            Object lv = Reflect.call(t, "a");   // JourneyMap's Tuple2 record: (LabelVars, InfoSlot)
            double ly = Reflect.num(Reflect.get(lv, "y"), 0), fs = Reflect.num(Reflect.get(lv, "fontScale"), 1);
            bottom = Math.max(bottom, (ly + 11 * fs) / scale);
        }
        return left < right ? (int) Math.ceil(bottom) : -1;
    }

    /** The bottom-left mirror of hints(): keys in one column at the left edge, words after them (no state tags). */
    private static void hintsLeft(GuiGraphics g, Font font, List<KeyRules.Group> groups) {
        if (groups.isEmpty()) return;
        int capsCol = 0, lines = 0;
        for (KeyRules.Group gr : groups) {
            lines++;
            for (KeyRules.Row r : gr.rows()) { capsCol = Math.max(capsCol, capsWidth(caps(r))); lines++; }
        }
        boolean wings = UiConfig.METER_DISPLAY.get() != UiConfig.MeterDisplay.ALMOST_VANILLA;
        int left = 6, bottom = g.guiHeight() - (wings ? (int) Math.ceil(36 * CompactHud.scale()) : 6);
        int y = bottom - lines * (ROW + GAP) + GAP, wordsLeft = left + capsCol + 4;
        for (KeyRules.Group gr : groups) {
            text(g, font, gr.title().toUpperCase(), left, y + 1, DIM);
            y += ROW + GAP;
            for (KeyRules.Row r : gr.rows()) {
                drawCaps(g, caps(r), left, y + (ROW - CAP_H) / 2, r.either());
                text(g, font, r.label(), wordsLeft, y + 1, r.off() ? OFF : BRASS);
                y += ROW + GAP;
            }
        }
    }

    // ---- Caps and text ----

    static List<String> caps(KeyRules.Row r) { return caps(r.keys()); }

    /** Each key's cap labels in order: a NeoForge modifier first (ALT, CTRL, SHIFT), then the key (WordChips.keyName). */
    static List<String> caps(List<KeyMapping> keys) {
        List<String> out = new ArrayList<>();
        for (KeyMapping k : keys) {
            if (k == KeyCreate.WHEEL) { out.add("WHEEL"); continue; }
            switch (k.getKeyModifier()) {
                case ALT -> out.add("ALT");
                case CONTROL -> out.add("CTRL");
                case SHIFT -> out.add("SHIFT");
                default -> { }
            }
            String n = WordChips.keyName(k.getKey().getName(), true);
            out.add(n == null ? "?" : n);
        }
        return out;
    }

    private static int capW(String s) { return Math.round((digitsWidth(s) * GS + 2 * PAD) / (float) K); }
    private static final int PLUS = 5;

    static int capsWidth(List<String> caps) {
        if (caps.isEmpty()) return 0;   // a readout row: no key
        int w = 0;
        for (String c : caps) w += capW(c);
        return w + PLUS * (caps.size() - 1);
    }

    /** The caps from (x, y) (GUI px, top left), a small "+" between them (keys held together) or "/" (either key); returns
     *  their width. */
    static int drawCaps(GuiGraphics g, List<String> caps, int x, int y, boolean either) {
        int at = x;
        for (int i = 0; i < caps.size(); i++) {
            if (i > 0) { glyphs(g, either ? "/" : "+", at + 1, y + 1, WHITE, 1.2f); at += PLUS; }
            box(g, caps.get(i), at, y, 0xFF5A5E66, 0xFF000000, WHITE);
            at += capW(caps.get(i));
        }
        return at - x;
    }

    /** A state tag (fire mode, ammo, mode) left of x: brass edge on black, white (red edge and glyphs when red). */
    /** The words of a control that cannot act now (the red of the red state tags). */
    private static final int OFF = 0xFFFF5050;

    /** A state tag: white, red (a warning), or a given atlas digit row (a jetpack mode in its colour). */
    private static void tag(GuiGraphics g, String s, int xRight, int y, boolean red, int tint) {
        int w = capW(s);
        box(g, s, xRight - w, y, red ? 0xFFFF5050 : 0xFF8A7550, 0xFF0A0805, red ? jetRow('D') : tint >= 0 ? tint : WHITE);
    }

    private static void box(GuiGraphics g, String s, int x, int y, int edge, int fill, int row) {
        int w = capW(s);
        use(g, FILL);
        g.fill(x, y, x + w, y + CAP_H, edge);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(1f / K, 1f / K, 1);
        g.fill(EDGE, EDGE, w * K - EDGE, CAP_H * K - EDGE, fill);
        pose.popPose();
        glyphs(g, s, x + (w * K - Math.round(digitsWidth(s) * GS)) / 2f / K, y + (CAP_H * K - DIGIT_H * GS) / 2f / K, row, GS);
    }

    /** Digit-strip glyphs at (x, y) GUI px, scale k texels. */
    private static void glyphs(GuiGraphics g, String s, float x, float y, int row, float k) {
        use(g, PARTS);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(k / K, k / K, 1);
        digits(g, s, 0, 0, s.length(), row, row);
        pose.popPose();
    }

    /** The game font with a black outline all round (the sign glow outline), so it reads on sky and snow. */
    private static void text(GuiGraphics g, Font font, String s, int x, int y, int color) {
        use(g, "font");
        font.drawInBatch8xOutline(Component.literal(s).getVisualOrderText(), x, y, color, OUTLINE, g.pose().last().pose(), g.bufferSource(), 0xF000F0);
    }
}
