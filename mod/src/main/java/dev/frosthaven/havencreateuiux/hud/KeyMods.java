package dev.frosthaven.havencreateuiux.hud;

import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Pack mechanics that are not one block's or item's own use code, each from its mod's own checks (by name, an absent mod
 * is no rule): Carry On (pick up with both hands empty and its carry key, which falls back to sneak while unbound:
 * CarryOnCommonClient; PickupHandler.canCarryGeneral, ListHandler.isPermitted, its config's settings), and ElevatorID
 * (standing on an elevator: jump goes up, sneak goes down: ElevatorHandler.handleInput).
 */
final class KeyMods {
    private static final String CARRY_DATA = "tschipp.carryon.common.carry.CarryOnDataManager";
    private static final String CARRY_LIST = "tschipp.carryon.common.config.ListHandler";

    private KeyMods() {}

    /** Carry On's carry key: its own binding, else sneak (unbound, as in the pack). */
    private static String[] carryKeys() {
        KeyMapping k = Reflect.getStatic("tschipp.carryon.client.keybinds.CarryOnKeybinds", "carryKey") instanceof KeyMapping m ? m : null;
        return k == null || k.isUnbound() ? new String[] {"key.sneak", "key.use"} : new String[] {"key.carry.desc", "key.use"};
    }

    private static Object settings() {
        Object common = Reflect.getStatic("tschipp.carryon.Constants", "COMMON_CONFIG");
        return common == null ? null : Reflect.get(common, "settings");
    }

    private static boolean carrying(LocalPlayer p) {
        Object data = Reflect.callStatic(CARRY_DATA, "getCarryData", p);
        return data != null && Reflect.bool(Reflect.call(data, "isCarrying"));
    }

    /** Carry On: both hands empty, not carrying, a game mode that may (not spectator, not adventure). */
    private static boolean mayCarry(Minecraft mc, LocalPlayer p) {
        if (Reflect.cls(CARRY_DATA) == null || !p.getMainHandItem().isEmpty() || !p.getOffhandItem().isEmpty() || carrying(p)) return false;
        var mode = mc.gameMode == null ? null : mc.gameMode.getPlayerMode();
        return mode != net.minecraft.world.level.GameType.SPECTATOR && mode != net.minecraft.world.level.GameType.ADVENTURE;
    }

    /** Carry this block (contextual): permitted by its lists, a block entity unless pickupAllBlocks, breakable unless
     *  pickupUnbreakableBlocks (or creative). */
    static void block(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, BlockState state, BlockHitResult bh) {
        if (!mayCarry(mc, p) || !Reflect.bool(Reflect.callStatic(CARRY_LIST, "isPermitted", state.getBlock()))) return;
        Object s = settings();
        boolean all = s != null && Reflect.bool(Reflect.get(s, "pickupAllBlocks")), unbreakable = s != null && Reflect.bool(Reflect.get(s, "pickupUnbreakableBlocks"));
        if (!all && mc.level.getBlockEntity(bh.getBlockPos()) == null) return;
        if (state.getDestroySpeed(mc.level, bh.getBlockPos()) < 0 && !unbreakable && !p.isCreative()) return;
        carry(rows);
    }

    /** Carry On's server handler (CommonEvents, priority HIGH, cancels) runs before a block's use and a mob's own
     *  interact, so on the same keys Carry replaces their row. A click the client cancels first never reaches it: Easy
     *  Villagers' pickup (VillagerEvents.onClick, client side, sends its own packet), so there Carry is no row. */
    private static void carry(List<KeyRules.Row> rows) {
        int before = rows.size();
        KeyRules.add(rows, "Carry", carryKeys());
        if (rows.size() == before) return;
        var keys = rows.get(rows.size() - 1).keys();
        rows.removeIf(r -> r.keys().equals(keys) && !r.label().equals("Carry"));
    }

    /** Carry this entity (contextual): permitted by its lists; hostile mobs and players only when its config allows. */
    static void entity(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, Entity e) {
        if (!mayCarry(mc, p) || !Reflect.bool(Reflect.callStatic(CARRY_LIST, "isPermitted", e))) return;
        Object s = settings();
        if (e instanceof Enemy && !(s != null && Reflect.bool(Reflect.get(s, "pickupHostileMobs")))) return;
        if (e instanceof Player && !(s != null && Reflect.bool(Reflect.get(s, "pickupPlayers")))) return;
        if (e instanceof net.minecraft.world.entity.npc.Villager && rows.stream().anyMatch(r -> r.label().equals("Pick up"))) return;   // Easy Villagers wins (client side)
        carry(rows);
    }

    /** Stateful: while carrying, use places it (PlacementHandler). */
    static void carried(List<KeyRules.Row> rows, LocalPlayer p) {
        if (Reflect.cls(CARRY_DATA) != null && carrying(p)) KeyRules.add(rows, "Place", "key.use");
    }

    /** Stateful: standing on an ElevatorID elevator, jump goes up and sneak goes down. */
    static void elevator(List<KeyRules.Row> rows, LocalPlayer p) {
        if (Reflect.cls("com.vsngarcia.ElevatorBlockBase") == null || !p.onGround()) return;
        var below = p.level().getBlockState(KeySable.below(p));   // on a ship: its block
        if (!Reflect.is(below.getBlock(), "com.vsngarcia.ElevatorBlockBase")) return;
        KeyRules.add(rows, "Up", "key.jump");
        KeyRules.add(rows, "Down", "key.sneak");
    }

    // ---- Axiom (the Offline Editor): only while Axiom says it can be used here (AxiomClient.isAxiomActive: its world,
    // game mode and permissions) and its editor UI is closed (EditorUI.isActive; the editor has its own key list) ----
    private static final String AXIOM = "com.moulberry.axiom.AxiomClient";
    private static final String CAPABILITY = "com.moulberry.axiom.capabilities.Capability";

    private static boolean axiomActive() {
        return Reflect.cls(AXIOM) != null && Reflect.bool(Reflect.callStatic(AXIOM, "isAxiomActive"))
                && !Reflect.bool(Reflect.callStatic("com.moulberry.axiom.editor.EditorUI", "isActive"));
    }

    /** Stateful: the Offline Editor's toggles that are on (Capability.isEnabled), each with its toggle key
     *  (axiom.keybind.toggle_<capability>_cap); an unbound toggle drops its row. */
    static void axiom(List<KeyRules.Row> rows) {
        if (!axiomActive() || !(Reflect.callStatic(CAPABILITY, "values") instanceof Object[] caps)) return;
        for (Object c : caps) {
            if (!Reflect.bool(Reflect.call(c, "isEnabled"))) continue;
            String name = ((Enum<?>) c).name().toLowerCase(java.util.Locale.ROOT);
            Object title = Reflect.get(c, "title");
            String label = title instanceof net.minecraft.network.chat.Component t ? t.getString() : name.replace('_', ' ');
            KeyRules.add(rows, label, "ON", false, "axiom.keybind.toggle_" + name + "_cap");
        }
    }

    /** Escape menu: the Offline Editor's own keys while Axiom can be used here. */
    static void axiomGlobal(List<KeyRules.Row> rows) {
        if (!axiomActive()) return;
        KeyRules.add(rows, "Editor", "axiom.keybind.toggle_editor_ui");
        KeyRules.add(rows, "Editor menu", "axiom.keybind.context");
        KeyRules.add(rows, "Builder tool", "axiom.keybind.builder_tool_slot");
        KeyRules.add(rows, "Replace mode", "axiom.keybind.toggle_replace_mode_cap");
    }

    /** Stateful: Create's toolbox key opens its radial menu while a toolbox is within 8 blocks
     *  (ToolboxHandlerClient.onKeyInput: ToolboxHandler.getNearest(level, player, 8)), or while the selected slot holds
     *  an item taken from a toolbox (persistent "CreateToolboxData" by slot: the menu returns it). Not for spectators. */
    static void toolbox(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p) {
        String h = "com.simibubi.create.content.equipment.toolbox.ToolboxHandler";
        if (Reflect.cls(h) == null || p.isSpectator()) return;
        boolean linked = p.getPersistentData().getCompound("CreateToolboxData").contains(String.valueOf(p.getInventory().selected));
        boolean near = Reflect.callStatic(h, "getNearest", p.level(), p, 8) instanceof java.util.List<?> l && !l.isEmpty();
        if (near || linked) KeyRules.add(rows, linked && !near ? "Return to toolbox" : "Toolbox", "create.keyinfo.toolbelt");
    }

    // ---- Held items with their own rules ----
    private static final String BACKPACK = "net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem";

    /** Stateful: a held backpack opens on use (BackpackItem.use); Create's sandpaper polishes the other hand's item while
     *  held down when that item has a polishing recipe (SandPaperItem.use, SandPaperPolishingRecipe.canPolish). */
    static void held(List<KeyRules.Row> rows, LocalPlayer p, net.minecraft.world.item.ItemStack s) {
        if (Reflect.is(s.getItem(), BACKPACK)) { KeyRules.add(rows, "Open", "key.use"); return; }
        if (Reflect.is(s.getItem(), "com.simibubi.create.content.equipment.sandPaper.SandPaperItem")) {
            var other = p.getMainHandItem() == s ? p.getOffhandItem() : p.getMainHandItem();
            if (!other.isEmpty() && Reflect.bool(Reflect.callStatic("com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe", "canPolish", p.level(), other)))
                KeyRules.add(rows, "Polish " + other.getHoverName().getString() + " (hold)", "key.use");
        }
    }

    /** Contextual: a held backpack on a block, sneaking (BackpackItem.useOn: InventoryInteractionHelper first). On a block
     *  with an item inventory that its server config allows, its Deposit and Restock upgrades run
     *  (IItemHandlerInteractionUpgrade wrappers); otherwise the backpack is placed. */
    static void backpackOnBlock(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, net.minecraft.world.item.ItemStack hand, BlockHitResult bh) {
        if (!Reflect.is(hand.getItem(), BACKPACK)) return;
        var pos = bh.getBlockPos();
        boolean inventory = mc.level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, bh.getDirection()) != null;
        Object cfg = Reflect.get(Reflect.getStatic("net.p3pp3rf1y.sophisticatedbackpacks.Config", "SERVER"), "noInteractionBlocks");
        if (inventory && cfg != null && Reflect.bool(Reflect.call(cfg, "isBlockInteractionDisallowed", mc.level.getBlockState(pos).getBlock()))) inventory = false;
        boolean any = false;
        if (inventory) {
            Object wrapper = Reflect.callStatic("net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper", "fromStack", hand);
            Object upgrades = Reflect.call(wrapper, "getUpgradeHandler");
            Class<?> iface = Reflect.cls("net.p3pp3rf1y.sophisticatedbackpacks.api.IItemHandlerInteractionUpgrade");
            if (upgrades != null && iface != null && Reflect.call(upgrades, "getWrappersThatImplement", iface) instanceof List<?> ws) {
                for (Object w : ws) {
                    String n = w.getClass().getSimpleName();
                    if (n.contains("Deposit")) { KeyRules.add(rows, "Deposit", "key.sneak", "key.use"); any = true; }
                    else if (n.contains("Restock")) { KeyRules.add(rows, "Restock", "key.sneak", "key.use"); any = true; }
                }
            }
        }
        if (!any) KeyRules.add(rows, "Place", "key.sneak", "key.use");
    }

    /** Create: Power Grid's wires (WireItem.useOn, IElectric.onWire): not sneaking, a wire item (IWire.isWire), the
     *  crosshair on a terminal of an electric block (terminalIndexAt on the hit inside the block) that accepts this wire.
     *  A wire already holding a start point (its CONNECTION_DATA component) connects; otherwise the click starts one.
     *  True when the held item is such a wire (its rows decided here, the audit data's looser "Connect" not used). */
    static boolean powerWire(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, net.minecraft.world.item.ItemStack hand, BlockState state, BlockHitResult bh) {
        String iwire = "org.patryk3211.powergrid.electricity.wire.IWire";
        if (Reflect.cls(iwire) == null || hand.isEmpty() || !Reflect.bool(Reflect.callStatic(iwire, "isWire", mc.level, hand.getItem()))) return false;
        if (p.isShiftKeyDown()) return true;
        Object electric = Reflect.callStatic("org.patryk3211.powergrid.electricity.base.IElectric", "getAt", mc.level, bh.getBlockPos());
        if (electric == null) return true;
        var local = bh.getLocation().subtract(bh.getBlockPos().getX(), bh.getBlockPos().getY(), bh.getBlockPos().getZ());
        int index = (int) Reflect.num(Reflect.call(electric, "terminalIndexAt", state, local), -1f);
        if (index < 0 || !Reflect.bool(Reflect.call(electric, "accepts", hand))) return true;
        Object supplier = Reflect.getStatic("org.patryk3211.powergrid.collections.ModdedDataComponents", "CONNECTION_DATA");
        boolean started = Reflect.call(supplier, "get") instanceof net.minecraft.core.component.DataComponentType<?> type && hand.has(type);
        KeyRules.add(rows, started ? "Connect" : "Start wire", "key.use");
        return true;
    }


    /** Sophisticated Storage on its front face (StorageBlockBase.tryAddUpgrade, main hand; sneaking through
     *  CommonEventHandler.onSneakItemBlockInteraction too): an upgrade item goes in. A limited barrel's front takes any
     *  item (deposit) and a dye colours its count (LimitedBarrelBlock). True when it decided. */
    static boolean sophisticatedFront(List<KeyRules.Row> rows, net.minecraft.world.item.ItemStack hand, BlockState state, BlockHitResult bh) {
        Object b = state.getBlock();
        if (hand.isEmpty() || !Reflect.is(b, "net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase")) return false;
        if (Reflect.call(b, "getFacing", state) != bh.getDirection()) return false;
        if (Reflect.is(hand.getItem(), "net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase")) {
            KeyRules.add(rows, "Add upgrade", "key.use");
            KeyRules.add(rows, "Add upgrade", "key.sneak", "key.use");
            return true;
        }
        if (!Reflect.is(b, "net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock")) return false;
        if (hand.getItem() instanceof net.minecraft.world.item.DyeItem) {
            KeyRules.add(rows, "Dye count", "key.use");
            KeyRules.add(rows, "Dye all counts", "key.sneak", "key.use");
        } else KeyRules.add(rows, "Deposit", "key.use");
        return true;
    }

    /** Farmer's Delight cutting board (CuttingBoardBlock$ToolCarvingEvent.onSneakPlaceTool): sneaking with a tool, a
     *  trident or shears in the main hand on an empty board places it there. True when it decided. */
    static boolean cuttingBoard(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, net.minecraft.world.item.ItemStack hand, BlockState state, BlockHitResult bh) {
        if (!Reflect.is(state.getBlock(), "vectorwing.farmersdelight.common.block.CuttingBoardBlock")) return false;
        var item = hand.getItem();
        if (!(item instanceof net.minecraft.world.item.TieredItem || item instanceof net.minecraft.world.item.TridentItem || item instanceof net.minecraft.world.item.ShearsItem)) return false;
        if (!Reflect.bool(Reflect.call(mc.level.getBlockEntity(bh.getBlockPos()), "isEmpty"))) return false;
        KeyRules.add(rows, "Place tool", "key.sneak", "key.use");
        return false;   // the plain click's rows (carving recipes) still come from the data
    }

    /** Drekki Rancher's DrekkiInteract, in its order: food it eats now feeds; at work a sneak click opens it; an empty hand's
     *  sneak click pets; a saddled Mature takes its owner on a plain click; else a plain click opens its screen. */
    static void drekki(List<KeyRules.Row> rows, LocalPlayer p, Entity d, net.minecraft.world.item.ItemStack hand) {
        if (!Reflect.bool(Reflect.call(d, "live"))) return;
        boolean mine = p.getUUID().equals(Reflect.call(d, "owner")), ghost = Reflect.bool(Reflect.call(d, "ghost"));
        int job = Reflect.call(d, "job") instanceof Integer j ? j : 0;
        String interact = "dev.frosthaven.ranchercompanion.entity.DrekkiInteract";
        if (mine && !ghost && Reflect.bool(Reflect.callStatic(interact, "handFeeds", d, hand))) { KeyRules.add(rows, "Feed", "key.use"); return; }
        if (job > 0) { if (mine) KeyRules.add(rows, "Drekki inventory", "key.sneak", "key.use"); return; }
        boolean opens = mine && Reflect.bool(Reflect.callStatic(interact, "opensScreen", hand));
        boolean ride = opens && Reflect.bool(Reflect.call(d, "rideable")) && !d.isVehicle() && !p.isPassenger();
        if (opens) KeyRules.add(rows, ride ? "Ride" : "Drekki inventory", "key.use");
        if (hand.isEmpty() && !ghost) KeyRules.add(rows, "Pet", "key.sneak", "key.use");
        else if (ride) KeyRules.add(rows, "Drekki inventory", "key.sneak", "key.use");   // only when use rides: else use already opens it
    }
}
