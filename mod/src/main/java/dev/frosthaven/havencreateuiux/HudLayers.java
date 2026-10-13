package dev.frosthaven.havencreateuiux;

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
 * whole HUD keeps its look while the pack is on. Mod HUD parts keep their look when the pack
 * retextures their mod (see {@link PackScreens#retexturedCaller}).
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
            ResourceLocation.fromNamespaceAndPath("create", "remaining_air"),
            dev.frosthaven.havencreateuiux.hud.CompactHud.LAYER);   // the compact action bar: no dark mode on it
    private static boolean drawing;
    private static boolean anyLayer;
    private static boolean flushing;

    private HudLayers() {}

    private static final String BATCHING_LAYERS = "net.raphimc.immediatelyfast.feature.batching.BatchingRenderLayers";

    /**
     * True while a main HUD layer draws, or ImmediatelyFast draws the batched HUD, and a Create
     * HUD pack is selected. {@code caller} is DME's element name: ImmediatelyFast's own HUD render
     * types pick their shader there, which can happen outside the flagged draws.
     */
    public static boolean skipDarkMode(String caller) {
        if (drawing) return true;
        if (!createHudPack()) return false;
        if (flushing || caller.startsWith(BATCHING_LAYERS)) return true;
        // Mod HUD parts the pack retextures (Ars Nouveau mana bar, Iron's Spellbooks bars).
        boolean hud = anyLayer || Minecraft.getInstance().screen == null;
        return hud && PackScreens.retexturedCaller(caller);
    }

    /** ImmediatelyFast is drawing the batched HUD. */
    public static void flushing(boolean value) {
        flushing = value;
    }

    public static void pre(RenderGuiLayerEvent.Pre event) {
        anyLayer = true;
        drawing = MAIN.contains(event.getName()) && createHudPack();
    }

    public static void post(RenderGuiLayerEvent.Post event) {
        drawing = false;
        anyLayer = false;
    }

    /** A selected resource pack named like "haven-create-hud" / "Create HUD". */
    public static boolean createHudPack() {
        for (String id : Minecraft.getInstance().getResourcePackRepository().getSelectedIds()) {
            if (isCreateHudPack(id)) return true;
        }
        return false;
    }

    public static boolean isCreateHudPack(String id) {
        return id.toLowerCase(Locale.ROOT).replace(' ', '-').replace('_', '-').contains("create-hud");
    }
}
