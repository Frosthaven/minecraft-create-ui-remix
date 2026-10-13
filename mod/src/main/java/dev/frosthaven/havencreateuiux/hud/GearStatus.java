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
 *   <li>Mekanism jetpack: hydrogen, its chemical tank 0 (Capabilities.CHEMICAL's item capability; its HUD's rule).</li>
 *   <li>MekaSuit chestplate with the jetpack unit enabled: hydrogen, tank 0 (ModuleJetpackUnit's HUD element).</li>
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
            mekanism(p, out);
        } catch (RuntimeException | LinkageError e) {
            HudLog.error("gear-mekanism", "reading Mekanism gear failed (its statuses left out)", e);
        }
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

    private static void mekanism(Player p, List<Gear> out) {
        Class<?> jetpack = Reflect.cls("mekanism.common.item.gear.ItemJetpack"),
                suit = Reflect.cls("mekanism.common.item.gear.ItemMekaSuitArmor");
        if (jetpack == null) return;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD}) {
            ItemStack s = p.getItemBySlot(slot);
            Object item = s.getItem();
            if (jetpack.isInstance(item)) tank(s, "mek_jetpack", "Jetpack hydrogen", out);
            else if (suit.isInstance(item) && slot == EquipmentSlot.CHEST && jetpackUnit(item, s))
                tank(s, "meka_jetpack", "MekaSuit jetpack hydrogen", out);
        }
    }

    /** The MekaSuit chestplate's jetpack unit is installed and enabled (isModuleEnabled(stack, JETPACK_UNIT)). */
    private static boolean jetpackUnit(Object item, ItemStack s) {
        Object unit = Reflect.getStatic("mekanism.common.registries.MekanismModules", "JETPACK_UNIT");
        return unit != null && Reflect.bool(Reflect.call(item, "isModuleEnabled", s, unit));
    }

    /** A Mekanism item's chemical tank 0 (amount / capacity), as its HUD reads it. */
    private static void tank(ItemStack s, String id, String name, List<Gear> out) {
        long[] c = chemical(s);
        if (c != null) out.add(new Gear(id, (float) c[0] / c[1], Component.literal(name), amount(c[0], c[1], "mB"), s));
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
