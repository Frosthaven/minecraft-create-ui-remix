package dev.frosthaven.havencreateuiux.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * What the contextual key hints (center, the crosshair's target) and the stateful key hints (bottom right: what the player rides, holds and
 * wears) show. Every row names real key mappings by their id (`key.ntgl.reload`), looked up in the game's key list: a
 * mapping that is missing or unbound drops its row (logged once). Each rule is the mod's own condition for that key or
 * click, read from its code (see mod/CLAUDE.md "Contextual and stateful key hints"). No compile dependency on any mod.
 */
final class KeyRules {
    /** One action: its keys pressed together (in order), its words, and a state tag (fire mode, ammo, mode) or null. */
    /** off: a control shown although it cannot act now (its words red), so every control of a device stays in view. */
    record Row(List<KeyMapping> keys, String label, String state, boolean red, boolean either, int tint, boolean off) {
        Row(List<KeyMapping> keys, String label, String state, boolean red, boolean either, int tint) { this(keys, label, state, red, either, tint, false); }
        Row(List<KeyMapping> keys, String label, String state, boolean red, boolean either) { this(keys, label, state, red, either, -1, false); }
        Row(List<KeyMapping> keys, String label, String state, boolean red) { this(keys, label, state, red, false, -1, false); }
    }
    /** A heading (the item's or vehicle's name) and its rows. */
    record Group(String title, List<Row> rows) {}

    private static final Map<String, KeyMapping> BY_NAME = new HashMap<>();
    private static int known;

    private KeyRules() {}

    /** A key mapping by id, or null (missing or unbound: the row is dropped, logged once). */
    static KeyMapping key(String id) {
        KeyMapping[] all = Minecraft.getInstance().options.keyMappings;
        if (all.length != known) {
            BY_NAME.clear();
            for (KeyMapping k : all) BY_NAME.put(k.getName(), k);
            known = all.length;
        }
        KeyMapping k = BY_NAME.get(id);
        if (k == null || k.isUnbound()) {
            HudLog.once("keyrow:" + id, "key hints: {} is {}: its row is left out", id, k == null ? "not a key mapping in this game" : "unbound");
            return null;
        }
        return k;
    }

    static void add(List<Row> rows, String label, String state, boolean red, String... ids) {
        List<KeyMapping> keys = new ArrayList<>();
        for (String id : ids) {
            KeyMapping k = key(id);
            if (k == null) return;
            keys.add(k);
        }
        rows.add(new Row(keys, label, state, red));
    }
    static void add(List<Row> rows, String label, String... ids) { add(rows, label, null, false, ids); }

    static String namespace(net.minecraft.world.level.block.state.BlockState s) { return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getNamespace(); }

    static String id(ItemStack s) { return BuiltInRegistries.ITEM.getKey(s.getItem()).toString(); }

    // ---- Contextual key hints: the crosshair's target, with what the main hand holds ----

    static List<Row> prompts(Minecraft mc, LocalPlayer p) {
        List<Row> rows = new ArrayList<>();
        ItemStack hand = p.getMainHandItem();
        if (KeyAddons.stunned(p)) return rows;                       // Cataclysm's stun cancels every click
        KeyCreate.Hit moving = KeyCreate.contraptionHit(mc, p);   // a moving contraption's block, closer than the world's
        if (KeyCreate.elevator(rows, mc, p, moving)) return rows;     // Create elevator controls
        if (KeyCreate.moving(rows, p, hand, moving)) return rows;     // contraption seats, train controls, moving doors
        if (KeyCreate.picked(rows, p, hand)) return rows;            // curved tracks, super glue (Create picks them itself)
        if (KeyCreate.chain(rows, p)) return rows;                   // a chain conveyor's chain (Create selects it over blocks)
        if (KeyAddons.rope(rows, mc, p)) return rows;                // Climbable Ropes: the strand looked at (its own ray)
        if (mc.hitResult instanceof BlockHitResult vb && vb.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && KeyCreate.valueBox(rows, mc, p, hand, vb)) {
            // Create's boxes and slots take the plain click (never a sneaking one): the block's own sneak rows still apply
            List<Row> block = new ArrayList<>();
            KeyData.rows(block, mc, p, hand, mc.level.getBlockState(vb.getBlockPos()), vb);
            for (Row r : block) if (r.keys().stream().anyMatch(k -> k.getName().equals("key.sneak"))) rows.add(r);
            return rows;
        }
        // hatches, tanks, buckets (KeyFluids); a wrench rotates hatches (ItemHatchBlock / FluidHatchBlock pass the wrench on),
        // except Create's wrench while sneaking: the hatch's deposit-all runs before the wrench
        boolean wrenchUse = KeyWrench.isWrench(hand) && !(id(hand).equals("create:wrench") && p.isSecondaryUseActive());
        if (mc.hitResult instanceof BlockHitResult eb && eb.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && KeyAddons.block(rows, mc, p, hand, mc.level.getBlockState(eb.getBlockPos()), eb)) return rows;   // event uses (KeyAddons)
        if (!wrenchUse && KeyFluids.rows(rows, mc, p, hand)) return rows;
        if (mc.hitResult instanceof EntityHitResult eh) {
            Entity e = eh.getEntity();
            String type = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
            if (KeyCreate.mob(rows, p, e, hand)) return rows;   // Create's handlers run first: schedules, shopkeepers
            if (KeyColonies.entity(rows, p, e, hand)) { }                       // MineColonies citizens and visitors
            else if (type.equals("rancher_companion:drekki")) KeyMods.drekki(rows, p, e, hand);
            else if (KeyAddons.entity(rows, p, e, hand)) { }                    // elephant, conductor (KeyAddons)
            else if (e instanceof Villager v) villager(rows, v, hand);
            else if (e instanceof net.minecraft.world.entity.vehicle.Boat b && (e.getClass() == net.minecraft.world.entity.vehicle.Boat.class
                    || e.getClass() == net.minecraft.world.entity.vehicle.ChestBoat.class)) {   // modded boats (storage boats): their data rows
                // vanilla: a boat seats 2 (a chest boat 1); sneak-use opens a chest boat's chest
                int seats = b instanceof net.minecraft.world.entity.vehicle.ChestBoat ? 1 : 2;
                if (!p.isSecondaryUseActive() && b.getPassengers().size() < seats) add(rows, "Board", "key.use");
                if (b instanceof net.minecraft.world.entity.vehicle.ChestBoat) add(rows, "Open chest", "key.sneak", "key.use");
            } else if (e instanceof net.minecraft.world.entity.vehicle.Minecart m && !Reflect.is(m, "com.minecolonies.api.entity.other.MinecoloniesMinecart")
                    && !m.isVehicle() && !p.isSecondaryUseActive()) add(rows, "Ride", "key.use");   // MineColonies' cart refuses riders (interact: FAIL)
            else if (type.equals("haven_backports:cushion") && !e.isVehicle()) add(rows, "Sit", "key.use");   // Haven Backports Cushion.interact
            else if (Reflect.is(e, "mekanism.common.entity.EntityRobit")) {
                // Mekanism's Robit (EntityRobit.interactAt): sneaking with the Configurator picks it up, else its screen
                if (id(hand).equals("mekanism:configurator")) add(rows, "Pick up", "key.sneak", "key.use");
                add(rows, "Open", "key.use");
            }
            else {
                // the audited entity data (its own interact code or an event handler), else vanilla's checks; a trap or a
                // mob with no hand action shows nothing
                KeyData.Result data = KeyThingData.entity(rows, mc, p, e, hand);
                if (data == KeyData.Result.SKIP) return rows;
                if (rows.isEmpty()) KeyVanilla.entity(rows, p, e, hand);
            }
            if (rows.isEmpty()) KeyThingData.itemOnEntity(rows, mc, p, hand, e);   // the held item's own use on it
            KeyMods.entity(rows, mc, p, e);   // Carry On: carry it (both hands empty)
        } else if (mc.hitResult instanceof BlockHitResult bh && mc.level != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            var state = mc.level.getBlockState(bh.getBlockPos());
            String block = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if (KeyCreate.valve(rows, p, hand, state)) {
                // Create valve handles: turned by an event with any item (KeyCreate.valve)
            } else if (KeyColonies.block(rows, mc, p, hand, state, bh)) {
                // MineColonies and Structurize: huts, barrel, crops, their tools on blocks (KeyColonies)
            } else if (KeyMods.cuttingBoard(rows, mc, p, hand, state, bh)) {
                // never: the sneak row is added and the board's other rules follow
            } else if (KeyMods.sophisticatedFront(rows, hand, state, bh)) {
                // Sophisticated Storage fronts: upgrades in, limited barrel deposits (KeyMods.sophisticatedFront)
            } else if (KeyWrench.rows(rows, mc, p, hand, state, bh)) {
                // wrenches: Create's and every c:tools/wrench, AE2, Refined Storage, Mekanism, FramedBlocks, Pipez (KeyWrench)
            } else if (block.equals("ae2:cable_bus") && Reflect.is(hand.getItem(), "appeng.api.parts.IPartItem")) {
                // AE2 part placement: the opposite-side key held flips the part to the cable's far side (PartPlacement).
                add(rows, "Place part", "key.use");
                add(rows, "Place on far side", "key.ae2.part_placement_opposite", "key.use");
            } else if (KeyCreate.crafter(rows, mc, p, hand, state, bh)) {
                // Create's mechanical crafter (KeyCreate.crafter)
            } else if (Reflect.is(state.getBlock(), "dev.frosthaven.ranchercompanion.block.HatcheryBlock")) {
                KeyBlocks.hatchery(rows, mc, hand, state, bh);
            } else if (Reflect.is(state.getBlock(), "dev.frosthaven.ranchercompanion.block.TombstoneBlock")) {
                // Drekki Rancher's tombstone: its own data rows only (Pay Respects with flint and steel, Put out candle,
                // Open), so the general flint and steel words ("Start fire") never take its place
                KeyData.rows(rows, mc, p, hand, state, bh);
            } else if (KeyBlocks.farm(rows, mc, p, hand, state, bh)) {
                // farm and tool words; on a block with a screen the item acts only while sneaking (KeyBlocks.farm)
            } else if (KeyBlocks.seatWord(mc, p, hand, state, bh) instanceof String seatWord) {
                if (!seatWord.isEmpty()) add(rows, seatWord, "key.use");   // seats (KeyBlocks.seatWord)
            } else if (!(p.isSecondaryUseActive() && !(hand.isEmpty() && p.getOffhandItem().isEmpty())) && KeyVanilla.block(rows, mc, p, hand, state, bh)) {
                // vanilla blocks with item-dependent uses (KeyVanilla.block)
            } else if (KeyBlocks.useWord(state) != null && !(p.isSecondaryUseActive() && !(hand.isEmpty() && p.getOffhandItem().isEmpty()))) {
                add(rows, KeyBlocks.useWord(state), "key.use");
            } else {
                // General rules first-class, the audited data adds to them: data rows, then Open when the block opens a
                // screen and no data row said Open, then the fallback Use only when nothing applied and the data does not
                // mark the block as a trap or as having no hand action.
                boolean sneakingWithItem = p.isSecondaryUseActive() && !(hand.isEmpty() && p.getOffhandItem().isEmpty());
                KeyData.Result data = KeyData.rows(rows, mc, p, hand, state, bh);
                if (data != KeyData.Result.SKIP && !sneakingWithItem) {
                    if (data != KeyData.Result.SPECIFIC && KeyBlocks.opensScreen(mc, state, bh.getBlockPos()) && rows.stream().noneMatch(r -> r.label().equals("Open"))) add(rows, "Open", "key.use");
                    if (rows.isEmpty() && data == KeyData.Result.NO_ENTRY && KeyBlocks.fallbackUse(state.getBlock(), hand)) add(rows, "Use", "key.use");
                }
            }
            // Diagonal Blocks toggles per hand (DiagonalBlockHandler.onUseBlock: sneaking, that hand empty): the main hand, or
            // the offhand when the main hand's item does nothing there (a wrench on a fence)
            String angles = hand.isEmpty() || p.getOffhandItem().isEmpty() ? KeyBlocks.diagonal(state.getBlock()) : null;
            if (angles != null) add(rows, angles, "key.sneak", "key.use");
            if (rows.isEmpty()) KeyCreate.itemApplication(rows, mc, hand, state);    // Create item application recipes
            if (rows.isEmpty() && !KeyMods.powerWire(rows, mc, p, hand, state, bh))   // Power Grid wires: terminals only
                KeyThingData.itemOnBlock(rows, mc, p, hand, state);   // the held item's own use on it
            KeyMods.block(rows, mc, p, state, bh);   // Carry On: carry it (both hands empty)
            KeyMods.backpackOnBlock(rows, mc, p, hand, bh);   // a held backpack: sneak-use deposits, restocks or places
        }
        return rows;
    }

    /** A villager with trades (an adult, not unemployed, not a nitwit) trades on use; Easy Villagers' pick up key picks it
     *  up when its own conditions allow (VillagerEvents.arePickupConditionsMet). */
    private static void villager(List<Row> rows, Villager v, ItemStack hand) {
        var prof = v.getVillagerData().getProfession();
        if (!v.isBaby() && prof != VillagerProfession.NONE && prof != VillagerProfession.NITWIT) add(rows, "Trade", "key.use");
        // Guard Villagers (HandlerEvents.onEntityInteract): sneaking with a guard weapon on a jobless or nitwit adult
        if (!v.isBaby() && (prof == VillagerProfession.NONE || prof == VillagerProfession.NITWIT) && Reflect.cls("tallestegg.guardvillagers.GuardVillagers") != null
                && hand.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("guardvillagers", "convertible_guard_items")))) {
            add(rows, "Make guard", "key.sneak", "key.use");
            return;
        }
        if (!Reflect.bool(Reflect.callStatic("de.maxhenkel.easyvillagers.events.VillagerEvents", "arePickupConditionsMet", v))) return;
        // Its sneak + use pickup when the client config allows it (villager.sneak_pick_up, VillagerEvents), else its key.
        Object cfg = Reflect.getStatic("de.maxhenkel.easyvillagers.Main", "CLIENT_CONFIG");
        Object sneak = cfg == null ? null : Reflect.get(cfg, "enableRightClickPickup");
        if (sneak instanceof net.neoforged.neoforge.common.ModConfigSpec.BooleanValue b && b.get()) add(rows, "Pick up", "key.sneak", "key.use");
        else add(rows, "Pick up", "key.easy_villagers.pick_up");
    }

    /** The Escape menu's global keys (stateful key hints' place): the map (JourneyMap's key or Haven Compat's second
     *  one, either), a waypoint, chat, the social menu, the inventory, swap offhand, Block Factory's dodge roll, and
     *  the worn backpack while one is worn. */
    static List<Group> global() {
        List<Row> rows = new ArrayList<>();
        List<KeyMapping> map = new ArrayList<>();
        for (String id : new String[] {"key.journeymap.map_toggle_alt", "key.haven_compat.open_map"}) { KeyMapping k = key(id); if (k != null) map.add(k); }
        if (!map.isEmpty()) rows.add(new Row(map, "Map", null, false, true));
        add(rows, "Add waypoint", "key.journeymap.create_waypoint");
        add(rows, "Chat", "key.chat");
        add(rows, "Social menu", "key.socialInteractions");
        add(rows, "Inventory", "key.inventory");
        add(rows, "Swap offhand", "key.swapOffhand");
        add(rows, "Dodge roll", "key.block_factorys_bosses.dodge_roll");   // Block Factory's Bosses (no row without it)
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null && backpack(p)) add(rows, "Worn backpack", "key.sophisticatedbackpacks.open_backpack");
        if (dev.frosthaven.havencreateuiux.UiConfig.HOTBAR_NUMBERS.get() == dev.frosthaven.havencreateuiux.UiConfig.HotbarNumbers.HOLD)
            add(rows, "Hotbar numbers (hold)", "key.haven_create_ui_ux.hud_numbers");   // our own key, while the config says Hold
        List<Group> out = new ArrayList<>();
        group(out, "Keys", rows);
        return out;
    }

    /** The Escape menu's bottom left: the Offline Editor's keys while Axiom can be used here (KeyMods.axiomGlobal). */
    static List<Group> globalLeft() {
        List<Row> axiom = new ArrayList<>();
        KeyMods.axiomGlobal(axiom);
        List<Group> out = new ArrayList<>();
        group(out, "Offline Editor", axiom);
        return out;
    }

    // ---- Stateful key hints: what the player rides, holds and wears ----

    static List<Group> hints(Minecraft mc, LocalPlayer p) {
        List<Group> out = new ArrayList<>();
        Entity v = p.getVehicle();
        if (v != null) KeyRiding.riding(out, p, v);
        else KeyRiding.travel(out, p);
        held(out, p);
        offhand(out, p);
        worn(out, p);
        List<Row> carry = new ArrayList<>();
        KeyMods.carried(carry, p);
        group(out, "Carrying", carry);
        List<Row> elevator = new ArrayList<>();
        KeyMods.elevator(elevator, p);
        group(out, "Elevator", elevator);
        List<Row> toolbox = new ArrayList<>();
        KeyMods.toolbox(toolbox, mc, p);
        group(out, "Nearby", toolbox);
        List<Row> axiom = new ArrayList<>();
        KeyMods.axiom(axiom);
        group(out, "Offline Editor", axiom);
        return out;
    }

    static void group(List<Group> out, String title, List<Row> rows) { if (!rows.isEmpty()) out.add(new Group(title, rows)); }

    /** The offhand's keys: each needs sneak held. NTGL's InputHandler picks the hand by sneaking (fire select, ammo select,
     *  reload, unload, attachments, inspect; melee and alt are main hand only); Mekanism's item mode key reaches the offhand
     *  only while sneaking (haven-compat ModeHand). */
    private static void offhand(List<Group> out, LocalPlayer p) {
        ItemStack s = p.getOffhandItem();
        if (s.isEmpty()) return;
        List<Row> rows = new ArrayList<>();
        if (SlotRead.isNtgl(s)) {
            String fire = GunRead.fireMode(p, s), ammo = GunRead.ammo(p, s);
            if (fire != null) add(rows, "Fire mode", fire, false, "key.sneak", "key.ntgl.fire_select");
            if (ammo != null) add(rows, "Ammo", ammo, false, "key.sneak", "key.ntgl.ammo_select");
            boolean empty = SlotRead.empty(p, s);
            add(rows, "Reload", empty ? ChipText.RELOAD : null, empty, "key.sneak", "key.ntgl.reload");
            add(rows, "Unload", "key.sneak", "key.ntgl.unload");
            add(rows, "Attachments", "key.sneak", "key.ntgl.attachments");
            add(rows, "Inspect", "key.sneak", "key.ntgl.inspect");
        } else if (mekanismMode(s, EquipmentSlot.OFFHAND)) {
            SlotRead.Badge b = SlotRead.read(p, s);
            add(rows, "Mode", b == null ? null : b.text(), false, "key.sneak", "key.mekanism.mode");
        }
        group(out, s.getHoverName().getString() + " (offhand)", rows);
    }

    private static void held(List<Group> out, LocalPlayer p) {
        ItemStack s = p.getMainHandItem();
        List<Row> rows = new ArrayList<>();
        if (SlotRead.isNtgl(s)) {
            String fire = GunRead.fireMode(p, s), ammo = GunRead.ammo(p, s);
            if (fire != null) add(rows, "Fire mode", fire, false, "key.ntgl.fire_select");
            if (ammo != null) add(rows, "Ammo", ammo, false, "key.ntgl.ammo_select");
            boolean empty = SlotRead.empty(p, s);
            add(rows, "Reload", empty ? ChipText.RELOAD : null, empty, "key.ntgl.reload");
            add(rows, "Melee", "key.ntgl.melee");
            add(rows, "Alt attack", "key.ntgl.alt");
            add(rows, "Attachments", "key.ntgl.attachments");
            add(rows, "Inspect", "key.ntgl.inspect");
            add(rows, "Unload", "key.ntgl.unload");
        } else if (!s.isEmpty()) {
            String id = id(s);
            if (mekanismMode(s, EquipmentSlot.MAINHAND)) {
                SlotRead.Badge b = SlotRead.read(p, s);
                add(rows, "Mode", b == null ? null : b.text(), false, "key.mekanism.mode");
            }
            if (Reflect.is(s.getItem(), "mekanism.common.content.gear.IModuleContainerItem")) add(rows, "Modules", "key.mekanism.module_tweaker");
            if (id.startsWith("ars_nouveau:") && id.endsWith("_spell_book")) {
                add(rows, "Previous spell", "key.ars_nouveau.previous_slot");
                add(rows, "Next spell", "key.ars_nouveau.next_slot");
                add(rows, "Spell HUD", "key.ars_nouveau.selection_hud");
                add(rows, "Open book", "key.ars_nouveau.open_book");
            } else if (id.startsWith("buildinggadgets2:gadget_")) {
                add(rows, "Settings", "key.buildinggadgets2.settings_menu");
                add(rows, "Anchor", "key.buildinggadgets2.anchor");
                add(rows, "Undo", "key.buildinggadgets2.undo");
            } else if (id.startsWith("chiselsandbits:chisel") || id.startsWith("chiselsandbits:bitsaw")) {
                add(rows, "Tool menu", "mod.chiselsandbits.keys.key.modded-tool.open");
                add(rows, "Undo", "mod.chiselsandbits.keys.key.undo");
                add(rows, "Redo", "mod.chiselsandbits.keys.key.redo");
            } else if (id.equals("chiselsandbits:measuring_tape")) {
                add(rows, "Clear tape", "mod.chiselsandbits.keys.key.measuring-tape.reset");
            } else if (id.equals("create:railway_casing")) {
                add(rows, "Bogey style", "railways.keyinfo.bogey_menu");   // BogeyMenuEventsHandler: holding Create's railway casing
            } else if (id.equals("rancher_companion:drekki_whistle")) {
                add(rows, "Stay / follow", "key.use");   // WhistleItem.use
                String order = whistleOrder(p);
                if (order != null) add(rows, order, "key.sneak", "key.use");
            }
            if (rows.isEmpty()) KeyMods.held(rows, p, s);       // Sophisticated backpack, Create sandpaper
            KeyColonies.held(rows, s);                           // MineColonies: hut items, scrolls (held use), the hammer
            if (rows.isEmpty() && KeyAddons.curioEquip(p, s)) add(rows, "Equip", "key.use");   // Curios: equips on use
            if (rows.isEmpty()) KeyVanilla.held(rows, s, p);    // eat, drink, block, draw, throw... by the item's use type
            if (rows.isEmpty()) KeyThingData.itemInAir(rows, p, s);   // the audited item data: its own use in the air
        }
        group(out, s.getHoverName().getString(), rows);
    }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] ARMOR_KEY = {"key.mekanism.head_mode", "key.mekanism.chest_mode", "key.mekanism.legs_mode", "key.mekanism.feet_mode"};
    private static final String[] ARMOR_WORD = {"Helmet mode", "Chest mode", "Legs mode", "Boots mode"};

    /** Worn gear's keys: Mekanism's per-slot mode keys for each piece that takes them (MekanismKeyHandler's own check:
     *  IModeItem.supportsSlotType), the jetpack's mode as the state; the module tweaker for a module item worn;
     *  Ad Astra's suit flight with the Jet Suit on. */
    private static void worn(List<Group> out, LocalPlayer p) {
        List<Row> rows = new ArrayList<>();
        String title = null;
        boolean modules = false;
        for (int i = 0; i < 4; i++) {
            ItemStack s = p.getItemBySlot(ARMOR[i]);
            if (s.isEmpty()) continue;
            // A MekaSuit piece always shows its mode key (red words while none of its modules has a mode); other Mekanism
            // gear while it takes the key (IModeItem.supportsSlotType: Free Runners, the jetpack)
            boolean meka = Reflect.is(s.getItem(), "mekanism.common.item.gear.ItemMekaSuitArmor"), hasMode = mekanismMode(s, ARMOR[i]);
            if (hasMode || meka) {
                String state = null;
                GearStatus.Jet jet = ARMOR[i] == EquipmentSlot.CHEST ? GearStatus.jetpack(p) : null;
                if (jet != null) state = jet.name().toUpperCase(java.util.Locale.ROOT);   // Mekanism's own word for the mode
                else if (hasMode) state = modeName(s);   // Free Runners and other mode gear: the mode it is in
                int before = rows.size();
                add(rows, ARMOR_WORD[i], state, false, ARMOR_KEY[i]);
                if (rows.size() > before && (jet != null || !hasMode)) {   // the jetpack's mode word in its colour; a piece without a mode: red words
                    Row r = rows.remove(rows.size() - 1);
                    rows.add(new Row(r.keys(), r.label(), r.state(), false, r.either(), jet != null ? CompactAtlas.jetRow(jet.mode()) : -1, !hasMode));
                }
                if (ARMOR[i] == EquipmentSlot.CHEST) jetpackFlight(rows, p);
                if (meka) title = "MekaSuit";   // the suit, not its chest piece's name ("MekaSuit Bodyarmor")
                else if (title == null || ARMOR[i] == EquipmentSlot.CHEST) title = s.getHoverName().getString();
            }
            modules |= Reflect.is(s.getItem(), "mekanism.common.content.gear.IModuleContainerItem");
            if (ARMOR[i] == EquipmentSlot.CHEST && id(s).equals("ad_astra:jet_suit")) {
                // its flight toggle, then flying on jump with the energy left (red EMPTY and red words at none)
                Float energy = GearStatus.jetSuit(p);
                add(rows, "Suit flight", "key.ad_astra.toggle_suit_flight");
                boolean dry = energy != null && energy <= 0f;
                control(rows, "Fly (hold)", energy == null ? null : fuel(energy), dry, dry, "key.jump");
                title = s.getHoverName().getString();
            }
        }
        if (modules) {   // a MekaSuit's energy on its Modules row: what its modules and modes run on (red EMPTY at none)
            HudExtras.Suit suit = HudExtras.mekaSuit(p);
            add(rows, "Modules", suit == null ? null : fuel(suit.frac()), suit != null && suit.frac() <= 0f, "key.mekanism.module_tweaker");
        }
        for (int i = 0; i < 4; i++) moduleReadouts(rows, p, p.getItemBySlot(ARMOR[i]));
        // elytra: jump while falling starts gliding (LocalPlayer.aiStep: tryToStartFallFlying)
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (!p.onGround() && !p.isFallFlying() && !p.getAbilities().flying && chest.canElytraFly(p)) {
            add(rows, "Glide", "key.jump");
            if (title == null) title = chest.getHoverName().getString();
        }
        group(out, title == null ? "Worn" : title, rows);
    }

    /** A fuel or charge left as a state tag: "73%", or EMPTY (shown red) at none. */
    static String fuel(float frac) { return frac <= 0f ? "EMPTY" : Math.max(1, Math.round(frac * 100)) + "%"; }

    /** The worn Mekanism jetpack's flight (JetpackMode), both controls always shown with the hydrogen left on the first:
     *  jump flies (Normal, Vector, and rises in Hover), sneak sinks (Hover only). A control the mode or an empty tank
     *  stops is shown with red words (Row.off), so the controls and the fuel stay in view in every mode. */
    private static void jetpackFlight(List<Row> rows, LocalPlayer p) {
        GearStatus.Jet jet = GearStatus.jetpack(p);
        if (jet == null) return;
        boolean empty = jet.frac() <= 0f, disabled = jet.mode() == 'D';
        control(rows, "Fly (hold)", fuel(jet.frac()), empty, disabled || empty, "key.jump");
        control(rows, "Sink (hold)", null, false, jet.mode() != 'H' || empty, "key.sneak");
    }

    /** A row that may be off (its words red: the control cannot act now). */
    private static void control(List<Row> rows, String label, String state, boolean red, boolean off, String... ids) {
        int before = rows.size();
        add(rows, label, state, red, ids);
        if (off && rows.size() > before) {
            Row r = rows.remove(rows.size() - 1);
            rows.add(new Row(r.keys(), r.label(), r.state(), r.red(), r.either(), r.tint(), true));
        }
    }

    private static boolean isBackpack(ItemStack s) { return Reflect.is(s.getItem(), "net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem"); }

    /** A worn Sophisticated backpack (the chest slot or a Curios slot): its open key works only then. */
    private static boolean backpack(LocalPlayer p) {
        if (isBackpack(p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST))) return true;
        Object handler = Reflect.callStatic("top.theillusivec4.curios.api.CuriosApi", "getCuriosInventory", p);
        if (handler instanceof java.util.Optional<?> o && o.isPresent())
            return Reflect.bool(Reflect.call(o.get(), "isEquipped", (java.util.function.Predicate<ItemStack>) KeyRules::isBackpack));
        return false;
    }

    /** The whistle's sneak order, as GoHere.command decides it: a hostile looked at within GoHere.RANGE, in front of the
     *  block behind it (Enemy, alive, not invulnerable, not a creeper: CombatGoals.fair's rule) is an attack, which only a
     *  drekki of stage 2 or more takes (none when the player's drekki here is younger); else Go here. */
    private static String whistleOrder(LocalPlayer p) {
        double range = Reflect.num(Reflect.getStatic("dev.frosthaven.ranchercompanion.entity.GoHere", "RANGE"), 64f);
        var eye = p.getEyePosition();
        var look = p.getViewVector(1f);
        var hit = p.level().clip(new net.minecraft.world.level.ClipContext(eye, eye.add(look.scale(range)), net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        double dist = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? range : hit.getLocation().distanceTo(eye);
        var mob = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p, eye, eye.add(look.scale(dist)),
                p.getBoundingBox().expandTowards(look.scale(dist)).inflate(1), e -> e instanceof net.minecraft.world.entity.monster.Enemy
                        && e instanceof net.minecraft.world.entity.LivingEntity l && l.isAlive() && !l.isInvulnerable()
                        && !(e instanceof net.minecraft.world.entity.monster.Creeper), dist * dist);
        if (mob != null) {
            for (Entity d : p.level().getEntitiesOfClass(Entity.class, p.getBoundingBox().inflate(range),
                    e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals("rancher_companion:drekki") && p.getUUID().equals(Reflect.call(e, "owner"))))
                if (Reflect.num(Reflect.call(d, "stage"), 3f) < 2) return null;   // "too young": the order does nothing
            return "Attack";
        }
        // a Create seat (Drekki Rancher's CreateSeats): one on a contraption first (its own look-ray walk), else the seat
        // block looked at. A job seat (a train's: conductor; beside a Stock Ticker: stock keeper) hires; a taken one: nothing
        Object seat = Reflect.callStatic("dev.frosthaven.ranchercompanion.compat.create.CreateSeats", "onContraption", p, Math.min(range, dist));
        if (seat != null) {
            if (!Reflect.bool(Reflect.call(seat, "free"))) return null;
            return Reflect.num(Reflect.call(seat, "role"), 0f) > 0 ? "Hire" : "Sit";
        }
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            var pos = hit.getBlockPos();
            var block = p.level().getBlockState(pos).getBlock();
            if (Reflect.is(block, "com.simibubi.create.content.contraptions.actors.seat.SeatBlock")) {
                if (Reflect.bool(Reflect.callStatic("com.simibubi.create.content.contraptions.actors.seat.SeatBlock", "isSeatOccupied", p.level(), pos))) return null;
                for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) for (int y = 0; y <= 1; y++)
                    if (Reflect.is(p.level().getBlockState(pos.relative(d).above(y)).getBlock(), "com.simibubi.create.content.logistics.stockTicker.StockTickerBlock")) return "Hire";
                return "Sit";
            }
        }
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? null : "Go here";
    }

    /** Drekki Rancher: whether this drekki holds a trait (Riding.holds(DrekkiEntity, Trait)). */
    static boolean trait(Entity d, String name) {
        Class<?> t = Reflect.cls("dev.frosthaven.ranchercompanion.trait.Trait");
        if (t == null || !t.isEnum()) return false;
        try {
            @SuppressWarnings({"unchecked", "rawtypes"}) Object v = Enum.valueOf((Class) t, name);
            return Reflect.bool(Reflect.callStatic("dev.frosthaven.ranchercompanion.entity.Riding", "holds", d, v));
        } catch (IllegalArgumentException e) {
            HudLog.once("trait:" + name, "key hints: Drekki Rancher has no trait {}", name);
            return false;
        }
    }

    /** Mekanism's mode key condition: an IModeItem that supports this slot. */
    /** A MekaSuit piece's module readouts (GearStatus.mekaModules): a row each, the module's name and its readout as a
     *  tag (red when Mekanism shows it as a warning), on the Module Tweaker's key: modules have no key of their own, they
     *  are switched there. */
    private static void moduleReadouts(List<Row> rows, LocalPlayer p, ItemStack s) {
        for (GearStatus.Readout r : GearStatus.mekaModules(p, s)) add(rows, r.name(), r.text(), r.warning(), "key.mekanism.module_tweaker");
    }

    /** A Mekanism mode item's current mode by Mekanism's own name (IAttachmentBasedModeItem.getMode: an enum with
     *  getTextComponent), upper case; null when it has none to read. */
    private static String modeName(ItemStack s) {
        Object m = Reflect.call(s.getItem(), "getMode", s);
        if (m == null) return null;
        Object t = Reflect.call(m, "getTextComponent");
        String n = t instanceof net.minecraft.network.chat.Component c ? c.getString() : m instanceof Enum<?> e ? e.name() : null;
        return n == null ? null : n.toUpperCase(java.util.Locale.ROOT);
    }

    private static boolean mekanismMode(ItemStack s, EquipmentSlot slot) {
        return Reflect.is(s.getItem(), "mekanism.common.item.interfaces.IModeItem") && Reflect.bool(Reflect.call(s.getItem(), "supportsSlotType", s, slot));
    }
}
