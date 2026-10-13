package dev.frosthaven.createdarkinventory;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client only. The texture work is in the mixins; here the Dark Mode Everywhere (DME) settings
 * this pack needs, so they travel with the jar instead of each player's config:
 *  - the Stock Keeper's item counts are blacklisted (they blit directly; DME inverted them on
 *    Create's light panel);
 *  - DME's inventory button moves right of the bottom-left icon buttons it overlapped, only
 *    while it sits at DME's default spot (a player's own X wins).
 */
@Mod(value = CreateDarkInventory.ID, dist = Dist.CLIENT)
public final class CreateDarkInventory {
    public static final String ID = "createdarkinventory";
    private static final String DME = "darkmodeeverywhere";
    private static final int DME_DEFAULT_X = 32;
    private static final int BUTTON_X = 56;

    public CreateDarkInventory(IEventBus modBus) {
        modBus.addListener(InterModEnqueueEvent.class, event ->
                InterModComms.sendTo(DME, "dme-shaderblacklist", () -> "StockKeeperRequestScreen:drawItemCount"));
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, CreateDarkInventory::moveDarkModeButton);
    }

    private static void moveDarkModeButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        for (var child : event.getListenersList()) {
            if (child instanceof Button button && button.getX() == DME_DEFAULT_X
                    && button.getMessage().getContents() instanceof TranslatableContents text
                    && text.getKey().equals("gui.darkmodeeverywhere.dark_mode")) {
                button.setX(BUTTON_X);
            }
        }
    }
}
