package dev.frosthaven.createdarkinventory;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Locale;
import java.util.Set;

/**
 * The main HUD (hotbar, hearts, armor, food, air, experience, action bar) keeps its own look
 * while a Create HUD resource pack is on: its sprites are already dark. Set while those layers
 * draw; {@code DarkModeClientProxyMixin} then skips DME's shader and text recolor.
 * With ImmediatelyFast's HUD batching the layers only queue their draws and the whole HUD is
 * drawn at once ({@code BatchingBuffersMixin}); the bars cannot be told apart there, so the
 * whole HUD keeps its look while the pack is on.
 */
public final class HudLayers {
    private static final Set<ResourceLocation> MAIN = Set.of(
            VanillaGuiLayers.HOTBAR,
            VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.ARMOR_LEVEL,
            VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.AIR_LEVEL,
            VanillaGuiLayers.EXPERIENCE_BAR,
            VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.OVERLAY_MESSAGE,
            ResourceLocation.fromNamespaceAndPath("create", "remaining_air"));
    private static boolean drawing;
    private static boolean flushing;

    private HudLayers() {}

    /** True while a main HUD layer draws and a Create HUD pack is selected. */
    public static boolean skipDarkMode() {
        return drawing || (flushing && createHudPack());
    }

    /** ImmediatelyFast is drawing the batched HUD. */
    public static void flushing(boolean value) {
        flushing = value;
    }

    public static void pre(RenderGuiLayerEvent.Pre event) {
        drawing = MAIN.contains(event.getName()) && createHudPack();
    }

    public static void post(RenderGuiLayerEvent.Post event) {
        drawing = false;
    }

    /** A selected resource pack named like "Create-HUD" / "Create HUD". */
    private static boolean createHudPack() {
        for (String id : Minecraft.getInstance().getResourcePackRepository().getSelectedIds()) {
            String name = id.toLowerCase(Locale.ROOT).replace(' ', '-').replace('_', '-');
            if (name.contains("create-hud")) return true;
        }
        return false;
    }
}
