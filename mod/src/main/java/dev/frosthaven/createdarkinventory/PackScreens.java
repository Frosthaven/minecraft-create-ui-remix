package dev.frosthaven.createdarkinventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * Screens that keep their own look while a Create HUD pack is on: Sophisticated Backpacks screens
 * and the player's inventory. Matched by DME's caller name, so only the screen's own drawing is
 * skipped and side panels such as JEI stay dark.
 */
public final class PackScreens {
    private static final String SOPHISTICATED = "net.p3pp3rf1y.sophisticated";
    private static final String BACKPACK_SCREENS = "net.p3pp3rf1y.sophisticatedbackpacks.";
    private static final String VANILLA_INVENTORY = "net.minecraft.client.gui.screens.inventory.";
    private static final String VANILLA_WIDGETS = "net.minecraft.client.gui.components.";

    private PackScreens() {}

    public static boolean skipDarkMode(String caller) {
        var screen = Minecraft.getInstance().screen;
        if (screen == null) return false;
        boolean backpack = screen.getClass().getName().startsWith(BACKPACK_SCREENS);
        boolean inventory = screen instanceof InventoryScreen;
        if (!backpack && !inventory) return false;
        boolean own = caller.startsWith(VANILLA_INVENTORY) || caller.startsWith(VANILLA_WIDGETS)
                || (backpack && caller.startsWith(SOPHISTICATED));
        return own && HudLayers.createHudPack();
    }
}
