package dev.frosthaven.havencreateuiux.hud;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * MineColonies and Structurize (contextual and held key hints), from their own interaction code:
 * citizens (EntityCitizen.checkAndHandleImportantInteractions), visitors (VisitorCitizen), huts
 * (AbstractColonyBlock.useItemOn), the compost barrel (TileEntityBarrel.useBarrel), crops, and the tools that act on
 * blocks (build, scan, tag and shape tools, clipboard, colony map, quest log, resource scroll, assistant hammer, rally
 * banner, forester's scepter, scan analyzer). True when a rule decided. An absent mod's classes never match.
 */
final class KeyColonies {
    private KeyColonies() {}

    private static final String MC = "com.minecolonies.", ST = "com.ldtteam.structurize.items.";
    private static final TagKey<Item> EXCLUDED_FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecolonies", "excluded_food"));

    /** Citizens and visitors. */
    static boolean entity(List<KeyRules.Row> rows, LocalPlayer p, Entity e, ItemStack hand) {
        boolean citizen = Reflect.is(e, MC + "core.entity.citizen.EntityCitizen"), visitor = Reflect.is(e, MC + "core.entity.visitor.VisitorCitizen");
        if (!citizen && !visitor) return false;
        boolean food = hand.has(DataComponents.FOOD) && !hand.is(EXCLUDED_FOOD);
        if (visitor) {   // VisitorCitizen: food is eaten sneaking or not; sneak opens its inventory; else the tavern talk
            if (food) { KeyRules.add(rows, "Feed", "key.use"); return true; }
            KeyRules.add(rows, "Talk", "key.use");
            KeyRules.add(rows, "Inventory", "key.sneak", "key.use");
            return true;
        }
        boolean baby = e instanceof net.minecraft.world.entity.LivingEntity l && l.isBaby();
        String word = "Talk";
        if (hand.is(Items.GOLDEN_APPLE) && sick(e)) word = "Cure";
        else if (food && (!baby || hand.getHoverName().getString().toLowerCase().contains("cookie"))) word = "Feed";
        else if (baby && hand.is(Items.BOOK)) word = "Teach";
        else if (hand.is(Items.CACTUS)) word = "Poke";
        else if (hand.is(Items.GLOWSTONE_DUST)) word = "Make glow";
        KeyRules.add(rows, word, "key.use");
        KeyRules.add(rows, "Inventory", "key.sneak", "key.use");
        return true;
    }

    /** The citizen's visible status is the sick one (ICitizenDataView.getVisibleStatus). */
    private static boolean sick(Entity e) {
        Object status = Reflect.call(Reflect.call(e, "getCitizenDataView"), "getVisibleStatus");
        return status != null && (String.valueOf(Reflect.call(status, "getId")) + status).toLowerCase().contains("sick");
    }

    /** Blocks: huts, the barrel, crops, and the MineColonies / Structurize tools used on a block. */
    static boolean block(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, ItemStack hand, BlockState state, BlockHitResult bh) {
        Object b = state.getBlock(), item = hand.getItem();
        boolean hut = Reflect.is(b, MC + "api.blocks.AbstractBlockHut");
        // tools first: their own useOn runs before the block's use
        if (Reflect.is(item, ST + "ItemBuildTool") || Reflect.is(item, ST + "ItemShapeTool")) { KeyRules.add(rows, "Open", "key.use"); return true; }
        if (Reflect.is(item, ST + "ItemScanTool") || Reflect.is(item, MC + "core.items.ItemScanAnalyzer") || Reflect.is(item, MC + "core.items.ItemScepterLumberjack")) {
            KeyRules.add(rows, "Set corner 1", "key.attack");
            KeyRules.add(rows, "Set corner 2", "key.use");
            if (Reflect.is(item, ST + "ItemScanTool")) KeyRules.add(rows, hut ? "Select hut" : "Set anchor", "key.sneak", "key.attack");
            return true;
        }
        if (Reflect.is(item, ST + "ItemTagTool")) {   // ItemTagTool: sneak-use anchors on a hut, decoration controller or field; attack tags
            KeyRules.add(rows, "Add or remove tag", "key.attack");
            String n = b.getClass().getSimpleName();
            if (hut || n.contains("DecorationController") || n.contains("PlantationField")) KeyRules.add(rows, "Set anchor", "key.sneak", "key.use");
            return true;
        }
        if (Reflect.is(item, MC + "core.items.ItemAssistantHammer")) { KeyRules.add(rows, "Help build", "key.use"); return true; }
        if (Reflect.is(item, MC + "core.items.ItemBannerRallyGuards") && hut) {
            String n = b.getClass().getSimpleName();
            if (n.contains("GuardTower") || n.contains("BarracksTower")) { KeyRules.add(rows, "Add or remove tower", "key.sneak", "key.use"); return true; }
        }
        if (!hut && (Reflect.is(item, MC + "core.items.ItemClipboard") || Reflect.is(item, MC + "core.items.ItemColonyMap")
                || Reflect.is(item, MC + "core.items.ItemQuestLog") || Reflect.is(item, MC + "core.items.ItemResourceScroll"))) {
            KeyRules.add(rows, "Open", "key.use");   // on a hut they link (sneak, the data rows); elsewhere their window
            return true;
        }
        if (hut) {   // AbstractColonyBlock.useItemOn: a plain click opens the hut, sneak-use with empty hands its inventory
            if (p.isSecondaryUseActive() && !(hand.isEmpty() && p.getOffhandItem().isEmpty())) return false;
            KeyRules.add(rows, Reflect.is(b, MC + "core.blocks.huts.BlockHutGateHouse") ? "Connections" : "Open", "key.use");
            if (hand.isEmpty()) KeyRules.add(rows, "Open inventory", "key.sneak", "key.use");
            return true;
        }
        if (Reflect.is(b, MC + "core.blocks.BlockBarrel")) {   // TileEntityBarrel.useBarrel: a done barrel gives compost
            String v = state.getProperties().stream().filter(pr -> pr.getName().equals("variant")).findFirst().map(pr -> String.valueOf(state.getValue(pr))).orElse("");
            if (v.contains("done")) KeyRules.add(rows, "Collect compost", "key.use");
            else if (!v.contains("100") && !v.contains("working") && !hand.isEmpty() && compostable(hand)) KeyRules.add(rows, "Add", "key.use");
            return true;
        }
        if (Reflect.is(b, MC + "core.blocks.MinecoloniesCropBlock") && hand.getItem() instanceof net.minecraft.world.item.HoeItem
                && Reflect.bool(Reflect.call(b, "isMaxAge", state))) { KeyRules.add(rows, "Harvest", "key.use"); return true; }
        return false;
    }

    /** MineColonies' compost recipes (IMinecoloniesAPI -> colony manager -> compatibility manager), keyed by item. */
    private static boolean compostable(ItemStack s) {
        Object api = Reflect.callStatic(MC + "api.IMinecoloniesAPI", "getInstance");
        Object compat = Reflect.call(Reflect.call(api, "getColonyManager"), "getCompatibilityManager");
        return Reflect.call(compat, "getCopyOfCompostRecipes") instanceof java.util.Map<?, ?> m && m.containsKey(s.getItem());
    }

    /** Held (stateful): hut items in the air open the building browser (ClientEventHandler.onUseItem); scrolls work on
     *  a held use (AbstractItemScroll, no use animation); the assistant hammer builds ahead. */
    static boolean held(List<KeyRules.Row> rows, ItemStack s) {
        Object item = s.getItem();
        if (Reflect.is(item, MC + "api.items.ItemBlockHut")) { KeyRules.add(rows, "Building info", "key.use"); return true; }
        if (Reflect.is(item, MC + "core.items.ItemAssistantHammer")) { KeyRules.add(rows, "Help build", "key.use"); return true; }
        if (Reflect.is(item, MC + "core.items.AbstractItemScroll")) {
            String n = item.getClass().getSimpleName();
            String w = n.contains("Buff") ? "Heal" : n.contains("AreaTP") ? "Teleport area" : n.contains("TP") ? "Teleport home"
                    : n.contains("GuardHelp") ? "Call guards" : n.contains("Highlight") ? "Highlight" : "Use";
            KeyRules.add(rows, w + " (hold)", "key.use");
            return true;
        }
        return false;
    }
}
