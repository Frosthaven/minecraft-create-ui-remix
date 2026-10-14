package dev.frosthaven.havencreateuiux;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.frosthaven.havencreateuiux.hud.HudLog;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Applies a modpack's recommended defaults once, then leaves the player in charge. Reads
 * {@code config/haven-pack-defaults.json} (absent: does nothing; unknown keys are ignored):
 * <pre>
 * {"resourcePacks": [{"id": "file/FA+All_Extensions-v1.8.1.zip", "above": "file/FreshAnimations_v1.10.4.zip"},
 *                    {"id": "file/Some Pack.zip"}],
 *  "shader": {"default": "ComplementaryUnbound_r5.9.zip", "macos": "BSL_v10.1.1.zip", "enabled": true}}
 * </pre>
 * Resource packs: a listed pack is enabled when it is first seen installed, or when it is installed again after it was
 * removed; "above" / "below" place it right above (higher priority) or below another selected pack. A pack the player
 * turned off while it stayed installed is never turned back on. Shader: on the first run only, Iris gets the
 * OS-specific shader pack (macOS: "macos", else "default") and shaders on or off. State is kept in
 * {@code config/haven-pack-defaults.state.json}. Runs on the first title screen of each session.
 */
public final class PackDefaults {
    private static final HudLog.Channel LOG = new HudLog.Channel("[pack defaults] ");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean done;

    private PackDefaults() {}

    public static void onScreen(ScreenEvent.Init.Post event) {
        if (done || !(event.getScreen() instanceof TitleScreen)) return;
        done = true;
        Path config = FMLPaths.CONFIGDIR.get();
        Path defaults = config.resolve("haven-pack-defaults.json"), statePath = config.resolve("haven-pack-defaults.state.json");
        if (!Files.exists(defaults)) {
            LOG.once("absent", "no {} in config: nothing to apply", defaults.getFileName());
            return;
        }
        try {
            JsonObject spec = JsonParser.parseString(Files.readString(defaults, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject state = Files.exists(statePath)
                    ? JsonParser.parseString(Files.readString(statePath, StandardCharsets.UTF_8)).getAsJsonObject() : new JsonObject();
            if (spec.has("resourcePacks")) resourcePacks(spec.getAsJsonArray("resourcePacks"), obj(state, "seen"));
            if (spec.has("shader") && !state.has("shaderApplied")) {
                shader(spec.getAsJsonObject("shader"));
                state.addProperty("shaderApplied", true);
            }
            Files.writeString(statePath, GSON.toJson(state), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error("apply", "could not apply " + defaults.getFileName() + "; skipped", e);
        }
    }

    private static JsonObject obj(JsonObject parent, String key) {
        if (!parent.has(key)) parent.add(key, new JsonObject());
        return parent.getAsJsonObject(key);
    }

    /** Enables newly seen packs in order, then saves options (Minecraft reloads resources if the list changed). */
    private static void resourcePacks(JsonArray list, JsonObject seen) {
        Minecraft mc = Minecraft.getInstance();
        var repository = mc.getResourcePackRepository();
        repository.reload();
        List<String> selected = new ArrayList<>(repository.getSelectedIds());   // bottom (lowest priority) first
        boolean changed = false;
        for (JsonElement el : list) {
            JsonObject entry = el.getAsJsonObject();
            String id = entry.get("id").getAsString();
            boolean present = repository.getAvailableIds().contains(id);
            boolean wasSeen = seen.has(id) && seen.get(id).getAsBoolean();
            seen.addProperty(id, present);
            if (!present) {
                LOG.once("missing:" + id, "{} is not installed; nothing to do", id);
                continue;
            }
            if (wasSeen) continue;   // installed before: the player's choice stands
            if (selected.contains(id)) {
                LOG.once("already:" + id, "{} is newly installed and already enabled", id);
                continue;
            }
            int at = selected.size();
            String anchor = entry.has("above") ? entry.get("above").getAsString() : entry.has("below") ? entry.get("below").getAsString() : null;
            if (anchor != null && selected.contains(anchor)) at = selected.indexOf(anchor) + (entry.has("above") ? 1 : 0);
            selected.add(at, id);
            changed = true;
            LOG.once("enabled:" + id, "enabled {}{}", id, anchor == null ? "" : (entry.has("above") ? " above " : " below ") + anchor);
        }
        if (changed) {
            repository.setSelected(selected);
            mc.options.updateResourcePacks(repository);   // saves options.txt, reloads resources if the list changed
        }
    }

    /** First run only: sets Iris's shader pack for this OS (reflection; Iris absent or file missing: skipped). */
    private static void shader(JsonObject spec) {
        boolean mac = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
        String name = spec.has(mac ? "macos" : "default") ? spec.get(mac ? "macos" : "default").getAsString()
                : spec.has("default") ? spec.get("default").getAsString() : null;
        boolean enabled = !spec.has("enabled") || spec.get("enabled").getAsBoolean();
        if (name == null) return;
        if (!Files.exists(FMLPaths.GAMEDIR.get().resolve("shaderpacks").resolve(name))) {
            LOG.once("shader-missing", "shader pack {} is not in shaderpacks/; skipped", name);
            return;
        }
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object cfg = iris.getMethod("getIrisConfig").invoke(null);
            cfg.getClass().getMethod("setShaderPackName", String.class).invoke(cfg, name);
            cfg.getClass().getMethod("setShadersEnabled", boolean.class).invoke(cfg, enabled);
            cfg.getClass().getMethod("save").invoke(cfg);
            Method reload = iris.getMethod("reload");
            reload.invoke(null);
            LOG.once("shader", "first run: shader pack {} ({}), shaders {}", name, mac ? "macOS" : "default", enabled ? "on" : "off");
        } catch (ClassNotFoundException e) {
            LOG.once("no-iris", "Iris is not installed; shader default skipped");
        } catch (ReflectiveOperationException | LinkageError e) {
            LOG.error("shader", "setting the shader pack through Iris failed; skipped", e);
        }
    }
}
