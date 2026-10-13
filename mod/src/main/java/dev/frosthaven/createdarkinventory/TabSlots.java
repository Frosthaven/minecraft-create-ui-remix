package dev.frosthaven.createdarkinventory;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Sophisticated Core draws every slot grid with GuiHelper.renderSlotsBackground from one texture, slots_background.png:
 * the storage grid and the slot groups in upgrade tabs alike, cut to exactly the cells. A resource pack can give the
 * tab groups their own look with upgrade_slots_background.png: each is drawn as a schematic scroll, blueprint paper
 * 1 px past the cells with a wooden roller above and below (layout: upgradeSlotsBackground in the redesign's
 * backpack-art.js). Without that file nothing changes. The mod leaves 2 to 3 px around a group; this uses 2.
 */
public final class TabSlots {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "textures/gui/upgrade_slots_background.png");
    private static final String GUI_HELPER = "net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper";
    /** Callers that draw the storage grid itself, not a tab. */
    private static final List<String> STORAGE_GRID = List.of(
            "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase",
            "net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen");
    private static final StackWalker STACK = StackWalker.getInstance();

    private static List<String> packIds = List.of();
    private static boolean present;

    private TabSlots() {}

    /** Draws a tab's slot group as a scroll; false when the game should draw it as usual. */
    public static boolean draw(GuiGraphics graphics, int x, int y, int columns, int rows) {
        if (columns > 12 || rows > 12 || !present() || storageGrid()) return false;   // the sheet holds 12 x 12
        int w = columns * 18, h = rows * 18;
        graphics.blit(TEXTURE, x - 1, y - 1, 0, 0, w + 1, h + 1);   // paper and cells, left and top edges
        graphics.blit(TEXTURE, x + w, y - 1, 0, 0, 1, h + 2);       // right edge (the sheet's left column)
        graphics.blit(TEXTURE, x - 1, y + h, 0, 0, w + 2, 1);       // bottom edge (the sheet's top row)
        roller(graphics, x, y - 2, w);
        roller(graphics, x, y + h, w);
        return true;
    }

    private static void roller(GuiGraphics graphics, int x, int y, int w) {
        graphics.blit(TEXTURE, x - 2, y, 0, 220, 1, 2);
        graphics.blit(TEXTURE, x - 1, y, 1, 220, w + 2, 2);
        graphics.blit(TEXTURE, x + w + 1, y, 235, 220, 1, 2);
    }

    private static boolean storageGrid() {
        return STACK.walk(frames -> frames
                .map(StackWalker.StackFrame::getClassName)
                .filter(name -> name.startsWith("net.p3pp3rf1y.") && !name.equals(GUI_HELPER))
                .findFirst()
                .map(STORAGE_GRID::contains)
                .orElse(true));
    }

    /** The texture is in a selected pack; checked again when the pack selection changes. */
    private static boolean present() {
        var minecraft = Minecraft.getInstance();
        List<String> ids = List.copyOf(minecraft.getResourcePackRepository().getSelectedIds());
        if (!ids.equals(packIds)) {
            present = minecraft.getResourceManager().getResource(TEXTURE).isPresent();
            packIds = ids;
        }
        return present;
    }
}
