package dev.frosthaven.havencreateuiux.hud;

import static dev.frosthaven.havencreateuiux.hud.CompactAtlas.*;
import static dev.frosthaven.havencreateuiux.hud.HudDraw.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A slot's ammo or Mekanism mode ({@link SlotRead}) drawn on the item in the slot numbers' style (digits in their black
 * outline): one line at the bottom right, reaching onto the socket's border art, the corner opposite the slot number,
 * always while the item is in the hotbar or the offhand; ammo white, low (a quarter or less) orange, none red; a mode
 * white, the bucket mode ON green, the Disassembler's OFF grey. Keys are key hints (KeyHintsHud), not on the slots. Every badge has the slot numbers' size ({@link #F}); only a text too wide for the socket at it
 * (the gatling's "100/100"; at most "999/999", SlotRead caps each number) shrinks to span it. With a stack count (bottom
 * right), the text sits above it. GUI px; z 300.
 */
final class SlotBadges {
    /** Texels of the digit strip to GUI px at the slot numbers' size. */
    private static final double TS = CompactHud.SLOT_NUM_K / 0.625 / K;
    private static final double LINE = DIGIT_H * TS;
    /** The badges' corner: glyphs to 0.5 px past the item's right and bottom edges, so the outline lies on the socket's
     *  border art (the socket is the item plus 1 px each side). */
    private static final double EDGE = 16.5;
    /** The badges' size: the slot numbers' (whole font px, a crisp outline). A text wider than the socket at it (the
     *  gatling's "100/100") shrinks to span the socket, its outline from the socket's left edge (x - 1) to its right edge. */
    static final double F = 1;

    private SlotBadges() {}

    /** The hotbar's nine slots (items at vanilla's spots: cx - 88 + 20 i, bottom - 19). */
    static void hotbar(GuiGraphics g, Player p, int cx, int bottom) {
        boolean any = false;
        SlotRead.Badge[] b = new SlotRead.Badge[9];
        for (int i = 0; i < 9; i++) any |= (b[i] = SlotRead.read(p, p.getInventory().getItem(i))) != null;
        if (!any) return;
        begin(g);
        for (int i = 0; i < 9; i++) if (b[i] != null) draw(g, b[i], p.getInventory().getItem(i), cx - 88 + 20 * i, bottom - 19);
        g.pose().popPose();
    }

    /** One slot (the offhand), its item's top left at (x, y) in the current pose. */
    static void slot(GuiGraphics g, Player p, ItemStack s, int x, int y) {
        SlotRead.Badge b = SlotRead.read(p, s);
        if (b == null) return;
        begin(g);
        draw(g, b, s, x, y);
        g.pose().popPose();
    }

    private static void begin(GuiGraphics g) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);   // over the item models, their counts (200) and the attack veil (250)
        use(g, PARTS);
        restoreBlend();
    }

    private static void draw(GuiGraphics g, SlotRead.Badge b, ItemStack s, int x, int y) {
        double right = x + EDGE, bottom = s.getCount() > 1 ? y + 8 : y + EDGE;   // above a stack count
        double w = digitsWidth(b.text()) * TS, f = Math.min(F, (right - x + 0.25) / w);   // the slot numbers' size; a wider text shrinks to span the socket
        if (b.total() != null) {   // a gun's inventory total at the bottom, its loaded rounds one line above
            double tw = digitsWidth(b.total()) * TS, tf = Math.min(F, (right - x + 0.25) / tw);
            text(g, b.total(), b.totalRow(), right - tw * tf, bottom - LINE * tf, tf);
            bottom -= LINE * tf + 0.5;
        }
        text(g, b.text(), b.row(), right - w * f, bottom - LINE * f, f);
    }

    private static void text(GuiGraphics g, String s, int row, double x, double y, double f) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale((float) (TS * f), (float) (TS * f), 1);
        digits(g, s, 0, 0, s.length(), row, row);
        pose.popPose();
    }
}
