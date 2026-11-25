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
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Contextual key hints from data: assets/haven_create_ui_ux/key_hints/blocks.json, made from an audit of every block class
 * in the pack with its own use code (each action read from the mod's tooltips, ponder text or bytecode; see mod/CLAUDE.md).
 * An entry matches a block by id (exact, or a "ns:*_x" / "ns:x_*" pattern), else by its class (instanceof, the nearest
 * class wins; a class that is not loaded, its mod absent, never matches). Each row needs its hand ("empty", "any",
 * "nonempty", "item", "tag", "class", "block_item", "same_block", "not_same_block", "fluid_container"; "Fill or empty" rows take their words from the held container) and every "when" ("face", "state",
 * "has_items", "has_fluid", "held_full", "creative", "id"); a "when" it cannot check on the client fails, so no row is ever shown on a guess. "skip" =
 * "none" or "trap": nothing shows on that block, not even the fallback (a trap: SecurityCraft's disguised mines).
 */
final class KeyData {
    /** SPECIFIC: a row for this held item matched, so its click takes the use key (no Open fallback) */
    enum Result { NO_ENTRY, SKIP, ADDED, SPECIFIC, NOTHING }

    private record Hand(String type, List<String> values) {}
    private record Row(String label, boolean sneak, Hand hand, List<JsonObject> when) {}
    private record Entry(String cls, String skip, List<Row> rows) {}

    private static final String PATH = "/assets/haven_create_ui_ux/key_hints/blocks.json";
    private static Map<String, Entry> byId;
    private static List<Map.Entry<Pattern, Entry>> byPattern;
    private static List<Entry> byClass;
    private static final Map<Block, Object> RESOLVED = new ConcurrentHashMap<>();
    private static final Object NONE = new Object();

    private KeyData() {}

    private static synchronized void load() {
        if (byId != null) return;
        byId = new HashMap<>();
        byPattern = new ArrayList<>();
        byClass = new ArrayList<>();
        try (var in = KeyData.class.getResourceAsStream(PATH)) {
            if (in == null) { HudLog.once("keydata-missing", "key hints: {} not in the jar (no data-driven block hints)", PATH); return; }
            JsonArray all = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
            int rows = 0;
            for (JsonElement el : all) {
                JsonObject o = el.getAsJsonObject();
                List<Row> rs = new ArrayList<>();
                for (JsonElement r : o.getAsJsonArray("rows")) {
                    JsonObject ro = r.getAsJsonObject(), h = ro.getAsJsonObject("hand");
                    List<String> vals = new ArrayList<>();
                    for (JsonElement v : h.getAsJsonArray("values")) vals.add(v.getAsString());
                    List<JsonObject> when = new ArrayList<>();
                    for (JsonElement w : ro.getAsJsonArray("when")) when.add(w.getAsJsonObject());
                    rs.add(new Row(ro.get("label").getAsString(), ro.get("sneak").getAsBoolean(), new Hand(h.get("type").getAsString(), vals), when));
                }
                rows += rs.size();
                Entry e = new Entry(o.get("class").getAsString(), o.get("skip").isJsonNull() ? null : o.get("skip").getAsString(), rs);
                boolean any = false;
                for (JsonElement b : o.getAsJsonArray("blocks")) {
                    String id = b.getAsString();
                    any = true;
                    if (id.contains("*")) byPattern.add(Map.entry(Pattern.compile(Pattern.quote(id).replace("*", "\\E.*\\Q")), e));
                    else byId.put(id, e);
                }
                if (!any) byClass.add(e);
            }
            HudLog.LOG.info("[compact HUD] key hints: {} block entries ({} rows) from {}", all.size(), rows, PATH);
        } catch (Exception ex) {
            HudLog.error("keydata-load", "key hints: reading " + PATH + " failed (no data-driven block hints)", ex);
        }
    }

    /** The entry for this block: by id, a pattern, else the nearest loaded class it is an instance of; null for none. */
    private static Entry entry(Block b) {
        load();
        Object r = RESOLVED.computeIfAbsent(b, k -> {
            String id = BuiltInRegistries.BLOCK.getKey(k).toString();
            Entry e = byId.get(id);
            if (e == null) for (var p : byPattern) if (p.getKey().matcher(id).matches()) { e = p.getValue(); break; }
            if (e == null) {
                int best = Integer.MAX_VALUE;
                for (Entry c : byClass) {
                    Class<?> cls = Reflect.cls(c.cls());
                    if (cls == null || !cls.isInstance(k)) continue;
                    int depth = 0;
                    for (Class<?> t = k.getClass(); t != null && t != cls; t = t.getSuperclass()) depth++;
                    if (depth < best) { best = depth; e = c; }
                }
            }
            return e == null ? NONE : e;
        });
        return r == NONE ? null : (Entry) r;
    }

    static Result rows(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, ItemStack hand, BlockState state, BlockHitResult bh) {
        Entry e = entry(state.getBlock());
        if (e == null) return Result.NO_ENTRY;
        if (e.skip() != null) return Result.SKIP;
        boolean handsEmpty = hand.isEmpty() && p.getOffhandItem().isEmpty();
        int before = rows.size();
        List<Row> hit = new java.util.ArrayList<>();
        for (Row r : e.rows()) {
            if (r.sneak() && !hand.isEmpty()) continue;                         // sneaking with an item skips block use
            if (!r.sneak() && p.isSecondaryUseActive() && !handsEmpty) continue;
            if (!hand(r.hand(), hand, state) || !when(r.when(), mc, state, bh)) continue;
            hit.add(r);
        }
        // an item's own use (useItemOn, or an event for that item) takes the click before the block's use without an
        // item: a row for this item replaces the rows for any hand on the same key (toolbox Dye, not Dye and Open)
        boolean specific = hit.stream().anyMatch(r -> !r.sneak() && SPECIFIC.contains(r.hand().type()));
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Row r : hit) {
            if (specific && !r.sneak() && !SPECIFIC.contains(r.hand().type())) continue;
            String label = r.label().equals("Fill or empty") ? fillOrEmpty(hand) : r.label();
            if (!seen.add((r.sneak() ? "s:" : "u:") + label)) continue;      // one row per label and key
            if (r.sneak()) KeyRules.add(rows, label, "key.sneak", "key.use");
            else KeyRules.add(rows, label, "key.use");
        }
        return rows.size() > before ? (specific ? Result.SPECIFIC : Result.ADDED) : Result.NOTHING;
    }

    /** "Fill or empty" rows in the short words: a full container empties into the block, an empty one fills from it
     *  (buckets: Fill bucket / Empty bucket; other fluid containers: Fill tank / Empty tank). */
    private static String fillOrEmpty(ItemStack hand) {
        var fh = hand.getCapability(Capabilities.FluidHandler.ITEM);
        boolean full = fh != null && fh.getTanks() > 0 && !fh.getFluidInTank(0).isEmpty();
        boolean bucket = hand.getItem() instanceof net.minecraft.world.item.BucketItem;
        return full ? (bucket ? "Empty bucket" : "Empty tank") : (bucket ? "Fill bucket" : "Fill tank");
    }

    private static final java.util.Set<String> SPECIFIC = java.util.Set.of("item", "tag", "class", "same_block", "fluid_container");

    private static boolean hand(Hand h, ItemStack s, BlockState state) {
        return switch (h.type()) {
            case "empty" -> s.isEmpty();
            case "any" -> true;
            case "nonempty" -> !s.isEmpty();
            case "item" -> h.values().contains(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
            case "tag" -> h.values().stream().anyMatch(t -> s.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(t))));
            case "not_tag" -> h.values().stream().noneMatch(t -> s.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(t))));
            case "class" -> h.values().stream().anyMatch(c -> Reflect.is(s.getItem(), c));
            case "block_item" -> s.getItem() instanceof BlockItem;
            case "same_block" -> s.getItem() instanceof BlockItem bi && bi.getBlock() == state.getBlock();
            case "not_same_block" -> !(s.getItem() instanceof BlockItem bi && bi.getBlock() == state.getBlock());
            case "fluid_container" -> !s.isEmpty() && s.getCapability(Capabilities.FluidHandler.ITEM) != null;
            default -> false;
        };
    }

    private static boolean when(List<JsonObject> when, Minecraft mc, BlockState state, BlockHitResult bh) {
        for (JsonObject w : when) {
            if (w.has("face")) {
                Direction d = bh.getDirection();
                boolean ok = switch (w.get("face").getAsString()) {
                    case "up" -> d == Direction.UP;
                    case "down" -> d == Direction.DOWN;
                    case "side" -> d.getAxis().isHorizontal();
                    case "front" -> facing(state) == d;
                    default -> false;
                };
                if (!ok) return false;
            } else if (w.has("state")) {
                for (var kv : w.getAsJsonObject("state").entrySet()) {
                    Property<?> prop = state.getBlock().getStateDefinition().getProperty(kv.getKey());
                    if (prop == null || !name(state, prop).equals(kv.getValue().getAsString())) return false;
                }
            } else if (w.has("has_items")) {
                var cap = mc.level.getCapability(Capabilities.ItemHandler.BLOCK, bh.getBlockPos(), null);
                if (cap == null) return false;
                boolean any = false;
                for (int i = 0; i < cap.getSlots() && !any; i++) any = !cap.getStackInSlot(i).isEmpty();
                if (any != w.get("has_items").getAsBoolean()) return false;
            } else if (w.has("has_fluid")) {
                var cap = mc.level.getCapability(Capabilities.FluidHandler.BLOCK, bh.getBlockPos(), null);
                if (cap == null) return false;
                boolean any = false;
                for (int i = 0; i < cap.getTanks() && !any; i++) any = !cap.getFluidInTank(i).isEmpty();
                if (any != w.get("has_fluid").getAsBoolean()) return false;
            } else if (w.has("creative")) {   // the player is in creative mode (some uses work only there)
                if (mc.player.isCreative() != w.get("creative").getAsBoolean()) return false;
            } else if (w.has("id")) {          // this exact block (one block of a class shared by several)
                if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(w.get("id").getAsString())) return false;
            } else if (w.has("held_full")) {   // the held fluid container holds fluid (true) or is empty (false)
                var fh = mc.player.getMainHandItem().getCapability(Capabilities.FluidHandler.ITEM);
                boolean full = fh != null && fh.getTanks() > 0 && !fh.getFluidInTank(0).isEmpty();
                if (fh == null || full != w.get("held_full").getAsBoolean()) return false;
            } else return false;   // a condition this reader does not know: never guess
        }
        return true;
    }

    private static Direction facing(BlockState s) {
        for (Property<?> p : s.getProperties())
            if ((p.getName().equals("facing") || p.getName().equals("horizontal_facing")) && s.getValue(p) instanceof Direction d) return d;
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String name(BlockState s, Property p) { return p.getName(s.getValue(p)); }
}
