package dev.frosthaven.havencreateuiux.hud;

import static dev.frosthaven.havencreateuiux.hud.CompactAtlas.*;
import static dev.frosthaven.havencreateuiux.hud.HudDraw.*;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The instrument chips that show words (the Mekanism jetpack, RELOAD): text in the digits' pixel font at the follower count's size (1.5x), one plate
 * cell tall. Positions are texels; x is the content's left edge, y the plate's top.
 */
final class WordChips {
    private static final float S = 1.5f;
    private static final int GAP = 2 * K;   // between the word and the number

    private WordChips() {}

    /** s at 1.5x in digit row `row`, its left edge at x, centred on the plate cell. Returns its width. */
    static int word(GuiGraphics g, String s, int row, int x, int y) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y + 4 * K, 0);
        pose.scale(S, S, 1);
        digits(g, s, 0, -DIGIT_H / 2, s.length(), row, row);
        pose.popPose();
        return width(s);
    }

    static int width(String s) { return (int) Math.round(digitsWidth(s) * S); }

    // ---- Mekanism jetpack: the mode's word in its colour (NORMAL green, VECTOR orange, HOVER cyan, DISABLED red), then
    // the hydrogen left (percent, no sign) in white.

    private static final String[] JET_WORDS = {"NORMAL", "VECTOR", "HOVER", "DISABLED"};
    private static String jetWord(GearStatus.Jet jet) { return JET_WORDS[Math.max(0, "NVHD".indexOf(jet.mode()))]; }
    private static String jetPercent(GearStatus.Jet jet) { return String.valueOf(Math.round(jet.frac() * 100)); }

    static int jetWidth(GearStatus.Jet jet) { return width(jetWord(jet)) + GAP + width(jetPercent(jet)); }

    static void jet(GuiGraphics g, GearStatus.Jet jet, int x, int y) {
        x += word(g, jetWord(jet), jetRow(jet.mode()), x, y) + GAP;
        word(g, jetPercent(jet), WHITE, x, y);
    }

    // ---- Word chips (CompactHud.wordChip: RELOAD, the held NTGL gun empty, red; SlotRead.reloadRow):
    // words split at spaces (the strip has no space glyph), GAP apart.

    static int wordsWidth(String s) {
        int w = -GAP;
        for (String part : s.split(" ")) w += width(part) + GAP;
        return w;
    }

    static void words(GuiGraphics g, String s, int row, int x, int y) {
        for (String part : s.split(" ")) x += word(g, part, row, x, y) + GAP;
    }

    /** The jetpack's mode switch key, a tab on the chip's top edge while the slot numbers show: in the slot numbers' style
     *  (white in the digits' black outline, CompactHud.SLOT_NUM_K), starting at the text's left and running rightward, its
     *  glyphs' bottom 0.75 px below the plate's top (the outline straddles the border, clear of the text below). The chip
     *  keeps its size and place. */
    private static final double KEY_S = CompactHud.SLOT_NUM_K / 0.625;

    /** The key from x (the chip's text left), plate top y. */
    static void key(GuiGraphics g, String key, int x, int y) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y + 0.75 * K - DIGIT_H * KEY_S, 0);
        pose.scale((float) KEY_S, (float) KEY_S, 1);
        digits(g, key, 0, 0, key.length(), WHITE, WHITE);
        pose.popPose();
    }

    /** A key's name in at most 3 characters of the digit strip (0-9, A-Z, +), from its locale-free name
     *  (key.keyboard.left.alt, key.mouse.4); null when unbound. Letters, digits, F keys as they are; punctuation as itself
     *  (; ' , . - = [ ] \ ` /, CompactAtlas.KEY_GLYPHS); modifiers ALT, SHF, CTL, WIN; keypad KP and its key; mouse LMB,
     *  RMB, MMB, M4; others the first 3 letters of the name. */
    static String keyLabel(KeyMapping k) {
        if (k == null || k.isUnbound()) return null;
        String n = k.getKey().getName();
        if (n.startsWith("key.mouse.")) {
            String b = n.substring(10);
            return switch (b) { case "left" -> "LMB"; case "right" -> "RMB"; case "middle" -> "MMB"; default -> "M" + b; };
        }
        String s = n.substring(n.indexOf('.', 4) + 1);   // after key.keyboard.
        String[] parts = s.split("\\.");
        String last = parts[parts.length - 1];
        if (s.startsWith("keypad.")) return "KP" + (last.length() == 1 ? last.toUpperCase() : last.equals("add") ? "+" : "");
        String out = switch (last) {
            case "alt" -> "ALT"; case "shift" -> "SHF"; case "control" -> "CTL"; case "win" -> "WIN"; case "lock" -> s.equals("caps.lock") ? "CAP" : parts[0].toUpperCase();   // NUM, SCR
            case "semicolon" -> ";"; case "apostrophe" -> "'"; case "comma" -> ","; case "period" -> "."; case "minus" -> "-";
            case "equal" -> "="; case "bracket" -> s.startsWith("left") ? "[" : "]"; case "backslash" -> "\\"; case "accent" -> "`"; case "slash" -> "/";
            case "space" -> "SPC"; case "enter" -> "ENT"; case "backspace" -> "BKS"; case "escape" -> "ESC"; case "tab" -> "TAB";
            case "up" -> s.equals("page.up") ? "PGU" : "UP"; case "down" -> s.equals("page.down") ? "PGD" : "DN"; case "left" -> "LFT"; case "right" -> "RGT";
            default -> last.toUpperCase().replaceAll("[^A-Z0-9]", "");
        };
        out = out.substring(0, Math.min(3, out.length()));
        return out.isEmpty() ? null : out;
    }
}
