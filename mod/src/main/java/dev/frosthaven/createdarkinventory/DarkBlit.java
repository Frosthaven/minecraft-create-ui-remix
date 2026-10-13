package dev.frosthaven.createdarkinventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a Create texture from a class that Dark Mode Everywhere does not blacklist.
 *
 * Dark Mode Everywhere picks its shader by the first caller outside GuiGraphics and blacklists
 * Create's AllGuiTextures by default, so every Create panel stays light. Drawn from here, a
 * texture gets the dark shader again, and so follows the dark mode toggle and shader choice.
 */
public final class DarkBlit {
    private DarkBlit() {}

    public static void blit(GuiGraphics graphics, ResourceLocation location, int x, int y, int u, int v, int width, int height) {
        graphics.blit(location, x, y, u, v, width, height);
    }
}
