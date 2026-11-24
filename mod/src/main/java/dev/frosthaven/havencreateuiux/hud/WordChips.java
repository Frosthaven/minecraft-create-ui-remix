package dev.frosthaven.havencreateuiux.hud;

import static dev.frosthaven.havencreateuiux.hud.CompactAtlas.*;
import static dev.frosthaven.havencreateuiux.hud.HudDraw.*;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Chip words and key names for the key hint caps: text in the digits' pixel font at the follower count's size (1.5x), one plate
 * cell tall. Positions are texels; x is the content's left edge, y the plate's top. Every chip's text is at most
 * {@link ChipText#MAX} characters (the key cap does not count): {@link #fit} cuts a longer one and logs it once.
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

    /** s cut to ChipText.MAX characters, logged once per text that was too long. */
    static String fit(String s) {
        String out = ChipText.cut(s);
        if (out != null && !out.equals(s)) HudLog.once("chip-cut:" + s, "chip text \"{}\" is longer than {} characters, shown as \"{}\"", s, ChipText.MAX, out);
        return out;
    }

    /** A key's short name in the digit strip (0-9, A-Z, +), from its locale-free name (key.keyboard.left.alt,
     *  key.mouse.4); null when unbound: letters, digits, F keys as they are; punctuation as itself (; ' , . - = [ ] \\ ` /,
     *  CompactAtlas.KEY_GLYPHS); modifiers ALT, SHF, CTL, WIN; keypad KP and its key; mouse M1 (left), M2 (right), M3
     *  (middle), M4, M5; others the first 3 letters of the name. KeyHintsHud draws them on its caps. */
    /** full: the key hints' longer names (SHIFT, CTRL, SPACE, ENTER), never cut to 3 characters. */
    static String keyName(String n, boolean full) {
        if (n.startsWith("key.mouse.")) {
            String b = n.substring(10);
            return switch (b) { case "left" -> "M1"; case "right" -> "M2"; case "middle" -> "M3"; default -> "M" + b; };
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
        if (full) out = switch (out) { case "SHF" -> "SHIFT"; case "CTL" -> "CTRL"; case "SPC" -> "SPACE"; case "ENT" -> "ENTER"; default -> out; };
        else out = out.substring(0, Math.min(3, out.length()));
        return out.isEmpty() ? null : out;
    }
}
