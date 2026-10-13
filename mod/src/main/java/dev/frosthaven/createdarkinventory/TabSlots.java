package dev.frosthaven.createdarkinventory;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Sophisticated Core draws every slot grid from one texture, slots_background.png: the storage
 * grid and the slots inside upgrade tabs alike. A resource pack can give the tabs their own look
 * with upgrade_slots_background.png (same layout); without that file nothing changes.
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

    /** Draws the tab slot grid like GuiHelper.renderSlotsBackground; false when it should not. */
    public static boolean draw(GuiGraphics graphics, int x, int y, int columns, int rows) {
        if (!present() || storageGrid()) return false;
        // Same split as GuiHelper: at most 12 rows per blit, 216 px apart.
        for (int left = rows; left > 0; left -= Math.min(rows, 12)) {
            graphics.blit(TEXTURE, x, y, 0, 0, columns * 18, left * 18);
            y += 216;
        }
        return true;
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
