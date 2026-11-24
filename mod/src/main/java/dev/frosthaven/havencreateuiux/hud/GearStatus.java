package dev.frosthaven.havencreateuiux.hud;

import java.lang.reflect.Constructor;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;

/**
 * Fuelled gear: one status per worn resource in the effects rail, with the MekaSuit's energy (the page's CC_GEAR): the
 * same cyan kind, told apart by the worn item's icon and the tooltip's name. Each is
 * read the way the mod's own HUD or item bar reads it (bytecode of the 1.21.1 jars in the pack):
 * <ul>
 *   <li>Mekanism jetpack (also a MekaSuit's jetpack unit): not a status; its mode and hydrogen are the instrument
 *   plate's chip ({@link #jetpack}).</li>
 *   <li>Ad Astra Jet Suit (full set): its energy (JetSuitItem.getEnergy). The Jet Suit has no separate fuel.</li>
 *   <li>Create Jetpack in flight: the backtank air it drains out of water ({@link HudStats#jetpackFlight}).</li>
 * </ul>
 * Breathing gear (the Mekanism scuba tank, Ad Astra space suit oxygen, Creating Space O2 backtanks) is not a status: it
 * takes the right orb's ring like the Create backtank ({@link AirGear}).
 * Every reader returns nothing when its mod is absent (logged once).
 */
final class GearStatus {
    /** A status: the fraction left, the name and the amount text for the tooltip, and the worn item for the icon. */
    record Gear(String id, float frac, Component name, Component text, ItemStack icon) {}
    private static final NumberFormat NUM = NumberFormat.getIntegerInstance();

    private GearStatus() {}

    /** The worn gear's statuses, the MekaSuit's energy first; flamingo / hull: the Helium Flamingo's charge and an
     *  aircraft's hull (HudExtras), or null. */
    static List<Gear> read(Player p, Float flamingo, Float hull) {
        List<Gear> out = new ArrayList<>();
        HudExtras.Suit suit = HudExtras.mekaSuit(p);
        if (suit != null) out.add(new Gear("mekasuit", suit.frac(), Component.literal("MekaSuit energy"), suit.text(), suit.icon()));
        // A Create Jetpack in flight: the backtank's air it drains (out of water; under water the tank is breathing, on
        // the ring). Create's own counter reads seconds.
        HudStats.Flight fl = HudStats.jetpackFlight(p, p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER));
        if (fl != null) out.add(new Gear("create_jetpack", fl.air() / fl.cap(), Component.literal("Jetpack air (backtank)"),
                Component.literal((int) fl.air() + " s of " + (int) fl.cap() + " s"), fl.icon()));
        try {
            adAstra(p, out);
        } catch (RuntimeException | LinkageError | ReflectiveOperationException e) {
            HudLog.error("gear-adastra", "reading Ad Astra suits failed (their statuses left out)", e);
        }
        if (flamingo != null) out.add(new Gear("flamingo", flamingo, Component.literal("Helium Flamingo charge"),
                Component.literal(Math.round(flamingo * 100) + "%"), item("artifacts:helium_flamingo")));
        if (hull != null && p.getVehicle() != null) out.add(new Gear("aircraft_hull", hull, Component.literal("Aircraft hull"),
                Component.literal(Math.round(hull * 100) + "%"), p.getVehicle().getPickResult() != null ? p.getVehicle().getPickResult() : ItemStack.EMPTY));
        return out;
    }

    // --- Mekanism -------------------------------------------------------------------------------------------------

    /** The worn Mekanism jetpack (a jetpack, or a MekaSuit chestplate with its jetpack unit on): its mode's letter
     *  (D disabled, V vector, N normal, H hover) and the hydrogen left, for the instrument plate's chip; null without one. */
    record Jet(char mode, float frac) {}

    static Jet jetpack(Player p) {
        try {
            if (!(Reflect.callStatic("mekanism.common.item.interfaces.IJetpackItem", "getPrimaryJetpack", p) instanceof ItemStack s) || s.isEmpty()) return null;
            Object mode = Reflect.call(s.getItem(), "getJetpackMode", s);
            long[] c = chemical(s);
            if (!(mode instanceof Enum<?> m) || c == null) return null;
            char letter = switch (m.name()) { case "DISABLED" -> 'D'; case "VECTOR" -> 'V'; case "HOVER" -> 'H'; default -> 'N'; };
            HudLog.changed("jetpack", s.getItem() + " " + letter, "jetpack chip: {} in mode {} ({})", s.getItem(), m.name(), letter);
            return new Jet(letter, (float) c[0] / c[1]);
        } catch (RuntimeException | LinkageError e) {
            HudLog.error("gear-jetpack", "reading the Mekanism jetpack failed (no chip)", e);
            return null;
        }
    }

    /** The short name of Mekanism's chest mode switch key (the jetpack's and the MekaSuit jetpack unit's mode), or null. */
    static String jetKey() { return modeKey("chestModeSwitchKey", "jetpack chip"); }

    /** The short name of Mekanism's item mode switch key (key.mekanism.mode: a held mode item, main hand, else offhand), or
     *  null; a mode item's slot shows it (SlotBadges). Sneak + mouse wheel also changes the mode (main hand, client
     *  config allowModeScroll). */
    static String handModeKey() { return modeKey("handModeSwitchKey", "slot modes"); }

    private static String modeKey(String field, String user) {
        Object k = Reflect.getStatic("mekanism.client.key.MekanismKeyHandler", field);
        String label = WordChips.keyLabel(k instanceof net.minecraft.client.KeyMapping km ? km : null);
        HudLog.changed(field, String.valueOf(label), "{}: mode switch key {}", user, label);
        return label;
    }

    /** A Mekanism item's chemical tank 0: {amount, capacity} (mB), or null. */
    static long[] chemical(ItemStack s) {
        Object multi = Reflect.getStatic("mekanism.common.capabilities.Capabilities", "CHEMICAL");
        if (!(Reflect.call(multi, "item") instanceof ItemCapability<?, ?> cap)) return null;
        @SuppressWarnings("unchecked") Object handler = s.getCapability((ItemCapability<Object, Void>) cap);
        if (handler == null || ((Number) Reflect.call(handler, "getChemicalTanks")).intValue() <= 0) return null;
        long max = ((Number) Reflect.call(handler, "getChemicalTankCapacity", 0)).longValue();
        Object stack = Reflect.call(handler, "getChemicalInTank", 0);
        long amount = stack == null ? 0 : ((Number) Reflect.call(stack, "getAmount")).longValue();
        return max > 0 ? new long[] {amount, max} : null;
    }

    // --- Ad Astra -------------------------------------------------------------------------------------------------

    private static void adAstra(Player p, List<Gear> out) throws ReflectiveOperationException {
        Class<?> suit = Reflect.cls("earth.terrarium.adastra.common.items.armor.SpaceSuitItem");
        if (suit == null) return;
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        Object item = chest.getItem();
        if (!suit.isInstance(item) || !Reflect.bool(Reflect.callStatic(suit.getName(), "hasFullSet", p))) return;
        // The Jet Suit's energy (getEnergy(stack, new ModifyOnlyContext(stack)): a ValueStorage).
        Class<?> jet = Reflect.cls("earth.terrarium.adastra.common.items.armor.JetSuitItem");
        if (jet == null || !jet.isInstance(item)) return;
        Object storage = Reflect.call(item, "getEnergy", chest, context(chest));
        if (storage == null) return;
        long e = ((Number) Reflect.call(storage, "getStoredAmount")).longValue(), cap = ((Number) Reflect.call(storage, "getCapacity")).longValue();
        if (cap > 0) out.add(new Gear("adastra_energy", (float) e / cap,
                Component.literal("Jet Suit energy"), amount(e, cap, "E"), chest));
    }

    private static Constructor<?> modifyOnly;

    private static Object context(ItemStack s) throws ReflectiveOperationException {
        if (modifyOnly == null) modifyOnly = Class.forName("earth.terrarium.common_storage_lib.context.impl.ModifyOnlyContext").getConstructor(ItemStack.class);
        return modifyOnly.newInstance(s);
    }

    private static ItemStack item(String id) {
        var key = net.minecraft.resources.ResourceLocation.tryParse(id);
        var it = key == null ? null : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key);
        return it == null ? ItemStack.EMPTY : new ItemStack(it);
    }

    private static Component amount(long amount, long max, String unit) {
        return Component.literal(NUM.format(amount) + "/" + NUM.format(max) + " " + unit);
    }
}
