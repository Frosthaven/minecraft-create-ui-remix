package dev.frosthaven.havencreateuiux;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.frosthaven.havencreateuiux.hud.HudLog;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
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
 *  "shader": {"default": "ComplementaryUnbound_r5.9.zip", "macos": "BSL_v10.1.1.zip", "enabled": true},
 *  "shaderOptions": {"macos": {"ComplementaryUnbound_r5.9.zip": {"BLOCK_REFLECT_QUALITY": "1"}}}}
 * </pre>
 * Resource packs: a listed pack is enabled when it is first seen installed, or when it is installed again after it was
 * removed; "above" / "below" place it right above (higher priority) or below another selected pack. A pack the player
 * turned off while it stayed installed is never turned back on. Shader: on the first run only, Iris gets the
 * OS-specific shader pack (macOS: "macos", else "default") and shaders on or off. Shader options: per OS ("macos",
 * else "default"), per shader pack, option values written into Iris's per-pack option file
 * ({@code shaderpacks/<pack>.txt}); a key is written when the file lacks it or still holds the value written here
 * before (so a pack change moves it), a value the player changed stays; Iris reloads once when something changed.
 * State is kept in {@code config/haven-pack-defaults.state.json}. Runs on the first title screen of each session.
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
            boolean reload = false;
            if (spec.has("shaderOptions")) reload = shaderOptions(spec.getAsJsonObject("shaderOptions"), obj(state, "shaderOptions"));
            if (spec.has("shader") && !state.has("shaderApplied")) {
                shader(spec.getAsJsonObject("shader"));   // reloads Iris itself
                state.addProperty("shaderApplied", true);
                reload = false;
            }
            if (reload) reloadIris("shader options changed");
            Files.writeString(statePath, GSON.toJson(state), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error("apply", "could not apply " + defaults.getFileName() + "; skipped", e);
        }
    }

    private static JsonObject obj(JsonObject parent, String key) {
        if (!parent.has(key)) parent.add(key, new JsonObject());
        return parent.getAsJsonObject(key);
    }

    private static boolean mac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
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
        boolean mac = mac();
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

    /**
     * Per-OS option values for shader packs, in Iris's per-pack option file (a Java properties file next to the pack).
     * Example: macOS runs out of texture units (Apple's OpenGL stops at 16) with Complementary at "Block Reflection
     * Quality: High" once a mod adds its own sampler, so the pack sets it to Low there. Returns true when a file changed.
     */
    private static boolean shaderOptions(JsonObject spec, JsonObject applied) {
        String os = mac() ? "macos" : "default";
        if (!spec.has(os) || !spec.get(os).isJsonObject()) return false;
        Path dir = FMLPaths.GAMEDIR.get().resolve("shaderpacks");
        boolean changed = false;
        for (Map.Entry<String, JsonElement> pack : spec.getAsJsonObject(os).entrySet()) {
            String name = pack.getKey();
            if (!pack.getValue().isJsonObject()) continue;
            if (!Files.exists(dir.resolve(name))) {
                LOG.once("options-missing:" + name, "shader pack {} is not in shaderpacks/; its options are skipped", name);
                continue;
            }
            Path file = dir.resolve(name + ".txt");
            Properties values = new Properties();
            try {
                if (Files.exists(file)) {
                    try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        values.load(in);
                    }
                }
                JsonObject mine = obj(applied, name);
                boolean packChanged = false;
                for (Map.Entry<String, JsonElement> opt : pack.getValue().getAsJsonObject().entrySet()) {
                    String key = opt.getKey(), want = opt.getValue().getAsString();
                    String have = values.getProperty(key);
                    String before = mine.has(key) ? mine.get(key).getAsString() : null;
                    if (want.equals(have)) {
                        mine.addProperty(key, want);
                        continue;
                    }
                    if (have != null && !have.equals(before)) {
                        LOG.once("options-player:" + name + ":" + key, "{}: {} is the player's choice ({}); left alone", name, key, have);
                        continue;
                    }
                    values.setProperty(key, want);
                    mine.addProperty(key, want);
                    packChanged = true;
                    LOG.once("options:" + name + ":" + key, "{} ({}): {} {} -> {}", name, os, key, have == null ? "unset" : have, want);
                }
                if (packChanged) {
                    try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                        values.store(out, "Shader pack options; Haven pack defaults set some of these once");
                    }
                    changed = true;
                }
            } catch (Exception e) {
                LOG.error("options:" + name, "could not update " + file.getFileName() + "; skipped", e);
            }
        }
        return changed;
    }

    /** Iris reloads its shader pack (reflection; Iris absent: nothing to do). */
    private static void reloadIris(String why) {
        try {
            Class.forName("net.irisshaders.iris.Iris").getMethod("reload").invoke(null);
            LOG.once("reload", "Iris reloaded: {}", why);
        } catch (ClassNotFoundException e) {
            LOG.once("no-iris", "Iris is not installed; nothing to reload");
        } catch (ReflectiveOperationException | LinkageError e) {
            LOG.error("reload", "Iris reload failed", e);
        }
    }
}
