package dev.frosthaven.havencreateuiux.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Key hints from the audit's entity and item data (assets/haven_create_ui_ux/key_hints/entities.json, items.json; see
 * mod/CLAUDE.md): an entity's own interact code (or an event handler on it), and an item's own use code: in the air
 * (stateful, while held), on a block or an entity (contextual). Matching as KeyData: by id or "*" pattern, else the
 * nearest loaded class; a missing mod's class never matches. Every "when" must hold; one this reader does not know fails.
 */
final class KeyThingData {
    record Row(String label, boolean sneak, JsonObject hand, String target, JsonObject on, List<JsonObject> when) {}
    record Entry(String cls, String skip, List<Row> rows) {}

    private static final class Table {
        final Map<String, Entry> byId = new HashMap<>();
        final List<Map.Entry<Pattern, Entry>> byPattern = new ArrayList<>();
        final List<Entry> byClass = new ArrayList<>();
        final Map<Object, Object> resolved = new ConcurrentHashMap<>();
    }

    private static Table entities, items;
    private static final Object NONE = new Object();

    private KeyThingData() {}

    private static Table load(String file, String idsKey) {
        Table t = new Table();
        String path = "/assets/haven_create_ui_ux/key_hints/" + file;
        try (var in = KeyThingData.class.getResourceAsStream(path)) {
            if (in == null) { HudLog.once("keydata-missing:" + file, "key hints: {} not in the jar", path); return t; }
            JsonArray all = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement el : all) {
                JsonObject o = el.getAsJsonObject();
                List<Row> rows = new ArrayList<>();
                for (JsonElement r : o.getAsJsonArray("rows")) {
                    JsonObject ro = r.getAsJsonObject();
                    List<JsonObject> when = new ArrayList<>();
                    if (ro.has("when")) for (JsonElement w : ro.getAsJsonArray("when")) when.add(w.getAsJsonObject());
                    rows.add(new Row(ro.get("label").getAsString(), ro.has("sneak") && ro.get("sneak").getAsBoolean(),
                            ro.has("hand") ? ro.getAsJsonObject("hand") : null, ro.has("target") ? ro.get("target").getAsString() : null,
                            ro.has("on") ? ro.getAsJsonObject("on") : null, when));
                }
                Entry e = new Entry(o.get("class").getAsString(), o.has("skip") && !o.get("skip").isJsonNull() ? o.get("skip").getAsString() : null, rows);
                boolean any = false;
                if (o.has(idsKey)) for (JsonElement b : o.getAsJsonArray(idsKey)) {
                    String id = b.getAsString();
                    any = true;
                    if (id.contains("*")) t.byPattern.add(Map.entry(Pattern.compile(Pattern.quote(id).replace("*", "\\E.*\\Q")), e));
                    else t.byId.put(id, e);
                }
                if (!any) t.byClass.add(e);
            }
            HudLog.LOG.info("[compact HUD] key hints: {} entries from {}", all.size(), path);
        } catch (Exception ex) {
            HudLog.error("keydata-load:" + file, "key hints: reading " + path + " failed", ex);
        }
        return t;
    }

    private static Entry find(Table t, Object thing, String id) {
        // by type and class: vehicle parts (Ad Astra's, NeoForge PartEntity) share their parent's type with another class
        Object r = t.resolved.computeIfAbsent(thing instanceof Entity e ? List.of(e.getType(), e.getClass()) : thing, k -> {
            Entry e = t.byId.get(id);
            if (e == null) for (var p : t.byPattern) if (p.getKey().matcher(id).matches()) { e = p.getValue(); break; }
            if (e == null) {
                int best = Integer.MAX_VALUE;
                for (Entry c : t.byClass) {
                    Class<?> cls = Reflect.cls(c.cls());
                    if (cls == null || !cls.isInstance(thing)) continue;
                    int depth = 0;
                    for (Class<?> x = thing.getClass(); x != null && x != cls; x = x.getSuperclass()) depth++;
                    if (depth < best) { best = depth; e = c; }
                }
            }
            return e == null ? NONE : e;
        });
        return r == NONE ? null : (Entry) r;
    }

    private static synchronized Table entities() { if (entities == null) entities = load("entities.json", "entities"); return entities; }
    private static synchronized Table items() { if (items == null) items = load("items.json", "items"); return items; }

    /** Entity rows for the entity under the crosshair; SKIP for a trap or no hand action. */
    static KeyData.Result entity(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, Entity target, ItemStack hand) {
        Entry e = find(entities(), target, BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
        if (e == null) return KeyData.Result.NO_ENTRY;
        if (e.skip() != null) return KeyData.Result.SKIP;
        int before = rows.size();
        for (Row r : e.rows()) {
            if (r.hand() != null && !hand(r.hand(), hand)) continue;
            if (!entityWhen(r.when(), p, target)) continue;
            if (r.sneak()) KeyRules.add(rows, r.label(), "key.sneak", "key.use");
            else KeyRules.add(rows, r.label(), "key.use");
        }
        return rows.size() > before ? KeyData.Result.ADDED : KeyData.Result.NOTHING;
    }

    /** Item rows of the held item on this block (contextual). */
    static void itemOnBlock(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, ItemStack hand, BlockState state) {
        Entry e = item(hand);
        if (e == null || e.skip() != null) return;
        for (Row r : e.rows()) if ("block".equals(r.target()) && onBlock(r.on(), state) && itemWhen(r.when(), p, hand)) add(rows, r);
    }

    /** Item rows of the held item on this entity (contextual). */
    static void itemOnEntity(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, ItemStack hand, Entity target) {
        Entry e = item(hand);
        if (e == null || e.skip() != null) return;
        for (Row r : e.rows()) if ("entity".equals(r.target()) && onEntity(r.on(), target) && itemWhen(r.when(), p, hand)) add(rows, r);
    }

    /** Item rows of the held item in the air (stateful, while held). */
    static void itemInAir(List<KeyRules.Row> rows, LocalPlayer p, ItemStack hand) {
        Entry e = item(hand);
        if (e == null || e.skip() != null) return;
        for (Row r : e.rows()) if ("air".equals(r.target()) && itemWhen(r.when(), p, hand)) add(rows, r);
    }

    private static void add(List<KeyRules.Row> rows, Row r) {
        if (r.sneak()) KeyRules.add(rows, r.label(), "key.sneak", "key.use");
        else KeyRules.add(rows, r.label(), "key.use");
    }

    private static Entry item(ItemStack s) {
        return s.isEmpty() ? null : find(items(), s.getItem(), BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
    }

    private static boolean hand(JsonObject h, ItemStack s) {
        List<String> v = new ArrayList<>();
        if (h.has("values")) for (JsonElement x : h.getAsJsonArray("values")) v.add(x.getAsString());
        return switch (h.get("type").getAsString()) {
            case "empty" -> s.isEmpty();
            case "any" -> true;
            case "nonempty" -> !s.isEmpty();
            case "item" -> v.contains(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
            case "tag" -> v.stream().anyMatch(t -> s.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(t))));
            case "class" -> v.stream().anyMatch(c -> Reflect.is(s.getItem(), c));
            case "fluid_container" -> !s.isEmpty() && s.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null;
            default -> false;
        };
    }

    private static boolean entityWhen(List<JsonObject> when, LocalPlayer p, Entity e) {
        for (JsonObject w : when) {
            if (w.has("owner")) {
                boolean mine = e instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID());
                if (mine != w.get("owner").getAsBoolean()) return false;
            } else if (w.has("baby")) {
                if (!(e instanceof AgeableMob a) || a.isBaby() != w.get("baby").getAsBoolean()) return false;
            } else if (w.has("vehicle")) {
                if (e.isVehicle() != w.get("vehicle").getAsBoolean()) return false;
            } else if (w.has("tag")) {
                if (!e.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(w.get("tag").getAsString())))) return false;
            } else if (w.has("not_tag")) {
                if (e.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(w.get("not_tag").getAsString())))) return false;
            } else if (w.has("health_full")) {
                boolean full = e instanceof LivingEntity l ? l.getHealth() >= l.getMaxHealth() : Reflect.num(Reflect.call(e, "getHealth"), 1f) >= 1f;
                if (full != w.get("health_full").getAsBoolean()) return false;
            } else return false;
        }
        return true;
    }

    private static boolean itemWhen(List<JsonObject> when, LocalPlayer p, ItemStack s) {
        for (JsonObject w : when) {
            if (w.has("has_component")) {
                var type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(w.get("has_component").getAsString()));
                if (type == null || !s.has(type)) return false;
            } else if (w.has("cooldown")) {
                if (p.getCooldowns().isOnCooldown(s.getItem()) != w.get("cooldown").getAsBoolean()) return false;
            } else if (w.has("flying")) {
                if (p.isFallFlying() != w.get("flying").getAsBoolean()) return false;
            } else if (w.has("on_ground")) {
                if (p.onGround() != w.get("on_ground").getAsBoolean()) return false;
            } else return false;
        }
        return true;
    }

    private static boolean onBlock(JsonObject on, BlockState state) {
        if (on == null) return false;
        List<String> v = new ArrayList<>();
        if (on.has("values")) for (JsonElement x : on.getAsJsonArray("values")) v.add(x.getAsString());
        return switch (on.get("type").getAsString()) {
            case "any" -> true;
            case "block" -> v.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            case "block_tag" -> v.stream().anyMatch(t -> state.is(TagKey.create(Registries.BLOCK, ResourceLocation.parse(t))));
            case "block_class" -> v.stream().anyMatch(c -> Reflect.is(state.getBlock(), c));
            default -> false;
        };
    }

    private static boolean onEntity(JsonObject on, Entity e) {
        if (on == null) return false;
        List<String> v = new ArrayList<>();
        if (on.has("values")) for (JsonElement x : on.getAsJsonArray("values")) v.add(x.getAsString());
        return switch (on.get("type").getAsString()) {
            case "any" -> true;
            case "entity" -> v.contains(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
            case "entity_class" -> v.stream().anyMatch(c -> Reflect.is(e, c));
            default -> false;
        };
    }
}
