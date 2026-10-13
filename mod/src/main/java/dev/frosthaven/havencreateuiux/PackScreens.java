package dev.frosthaven.havencreateuiux;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.neoforged.fml.ModList;

/**
 * Screens that keep their own look while a Create HUD pack is on: the player's inventory,
 * Sophisticated Backpacks screens, and any screen of a mod the pack retextures. Matched by DME's
 * caller name ("class:method"): only drawing by vanilla screen code or by mods the pack retextures
 * (trash slot, Curios buttons, ...) is skipped, so side panels such as JEI stay dark.
 */
public final class PackScreens {
    private static final String SOPHISTICATED = "net.p3pp3rf1y.sophisticated";
    private static final String BACKPACK_SCREENS = "net.p3pp3rf1y.sophisticatedbackpacks.";
    private static final String VANILLA_INVENTORY = "net.minecraft.client.gui.screens.inventory.";
    private static final String VANILLA_WIDGETS = "net.minecraft.client.gui.components.";

    /** Class name to the ids of the mods in its jar. */
    private static final Map<String, Set<String>> MODS = new ConcurrentHashMap<>();
    private static List<String> packIds = List.of();
    private static Set<String> packNamespaces = Set.of();

    private PackScreens() {}

    public static boolean skipDarkMode(String caller) {
        var screen = Minecraft.getInstance().screen;
        if (screen == null || !HudLayers.createHudPack()) return false;
        String screenClass = screen.getClass().getName();
        boolean backpack = screenClass.startsWith(BACKPACK_SCREENS);
        if (!backpack && !(screen instanceof InventoryScreen) && !retextured(screenClass)) return false;
        return caller.startsWith(VANILLA_INVENTORY) || caller.startsWith(VANILLA_WIDGETS)
                || (backpack && (caller.startsWith(SOPHISTICATED) || caller.startsWith(TabSlots.class.getName())))
                || retexturedCaller(caller);
    }

    /** DME's caller ("class:method") comes from a mod whose textures the Create HUD pack replaces. */
    public static boolean retexturedCaller(String caller) {
        return retextured(caller.substring(0, Math.max(caller.indexOf(':'), 0)));
    }

    /** The class comes from a mod (not vanilla) whose textures the Create HUD pack replaces. */
    private static boolean retextured(String className) {
        if (className.isEmpty() || className.startsWith("net.minecraft.")) return false;
        Set<String> namespaces = namespaces();
        for (String mod : MODS.computeIfAbsent(className, PackScreens::modsOf)) {
            if (namespaces.contains(mod)) return true;
        }
        return false;
    }

    private static Set<String> modsOf(String className) {
        try {
            String module = Class.forName(className, false, PackScreens.class.getClassLoader()).getModule().getName();
            if (module == null) return Set.of();
            Set<String> ids = new java.util.HashSet<>();
            for (var file : ModList.get().getModFiles()) {
                if (module.equals(file.moduleName())) file.getMods().forEach(mod -> ids.add(mod.getModId()));
            }
            return ids;
        } catch (ReflectiveOperationException | LinkageError e) {
            return Set.of();
        }
    }

    /** Asset namespaces of the selected Create HUD pack, read again when the selection changes. */
    private static Set<String> namespaces() {
        var repository = Minecraft.getInstance().getResourcePackRepository();
        List<String> ids = List.copyOf(repository.getSelectedIds());
        if (!ids.equals(packIds)) {
            Set<String> found = new java.util.HashSet<>();
            for (Pack pack : repository.getSelectedPacks()) {
                if (!HudLayers.isCreateHudPack(pack.getId())) continue;
                try (PackResources resources = pack.open()) {
                    found.addAll(resources.getNamespaces(PackType.CLIENT_RESOURCES));
                }
            }
            packNamespaces = Set.copyOf(found);
            packIds = ids;
        }
        return packNamespaces;
    }
}
