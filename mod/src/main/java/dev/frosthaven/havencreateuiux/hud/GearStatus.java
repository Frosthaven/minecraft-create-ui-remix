package dev.frosthaven.havencreateuiux.hud;

import java.lang.reflect.Constructor;
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
 *   <li>Mekanism jetpack (also a MekaSuit's jetpack unit) and Ad Astra's Jet Suit: their fuel is a tag on their key
 *   hint ({@link #jetpack}, {@link #jetSuit}), next to the key that flies them, and a cyan status as well.</li>
 *   <li>Create Jetpack in flight: the backtank air it drains out of water ({@link HudStats#jetpackFlight}).</li>
 * </ul>
 * Breathing gear (the Mekanism scuba tank, Ad Astra space suit oxygen, Creating Space O2 backtanks) is not a status: it
 * takes the right orb's ring like the Create backtank ({@link AirGear}).
 * Every reader returns nothing when its mod is absent (logged once).
 */
final class GearStatus {
    /** A status: the fraction left, the name and the amount text for the tooltip, and the worn item for the icon. */
    record Gear(String id, float frac, Component name, Component text, ItemStack icon) {}

    private GearStatus() {}

    /** The worn gear's statuses, the MekaSuit's energy first; flamingo / hull: the Helium Flamingo's charge and an
     *  aircraft's hull (HudExtras), or null. */
    static List<Gear> read(Player p, Float flamingo, Float hull) {
        List<Gear> out = new ArrayList<>();
        HudExtras.Suit suit = HudExtras.mekaSuit(p);
        if (suit != null) out.add(new Gear("mekasuit", suit.frac(), Component.literal("MekaSuit energy"), suit.text(), suit.icon()));
        // Fuel for flying gear: a cyan status here, and a tag on its key hint (KeyRules) while those show
        keyedFuel(p, out);
        // A Create Jetpack in flight: the backtank's air it drains (out of water; under water the tank is breathing, on
        // the ring). Create's own counter reads seconds.
        HudStats.Flight fl = HudStats.jetpackFlight(p, p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER));
        if (fl != null) out.add(new Gear("create_jetpack", fl.air() / fl.cap(), Component.literal("Jetpack air (backtank)"),
                Component.literal((int) fl.air() + " s of " + (int) fl.cap() + " s"), fl.icon()));
        if (flamingo != null) out.add(new Gear("flamingo", flamingo, Component.literal("Helium Flamingo charge"),
                Component.literal(Math.round(flamingo * 100) + "%"), item("artifacts:helium_flamingo")));
        if (hull != null && p.getVehicle() != null) out.add(new Gear("aircraft_hull", hull, Component.literal("Aircraft hull"),
                Component.literal(Math.round(hull * 100) + "%"), p.getVehicle().getPickResult() != null ? p.getVehicle().getPickResult() : ItemStack.EMPTY));
        return out;
    }

    // --- Mekanism -------------------------------------------------------------------------------------------------

    /** The worn Mekanism jetpack (a jetpack, or a MekaSuit chestplate with its jetpack unit on): its mode's letter
     *  (D disabled, V vector, N normal, H hover), Mekanism's own name for the mode, and the hydrogen left, for the
     *  stateful key hints; null without one. */
    record Jet(char mode, float frac, String name) {}

    static Jet jetpack(Player p) {
        try {
            if (!(Reflect.callStatic("mekanism.common.item.interfaces.IJetpackItem", "getPrimaryJetpack", p) instanceof ItemStack s) || s.isEmpty()) return null;
            Object mode = Reflect.call(s.getItem(), "getJetpackMode", s);
            long[] c = chemical(s);
            if (!(mode instanceof Enum<?> m) || c == null) return null;
            char letter = switch (m.name()) { case "DISABLED" -> 'D'; case "VECTOR" -> 'V'; case "HOVER" -> 'H'; default -> 'N'; };
            String name = Reflect.call(m, "getTextComponent") instanceof Component t ? t.getString() : m.name();
            HudLog.changed("jetpack", s.getItem() + " " + letter, "jetpack: {} in mode {} ({})", s.getItem(), m.name(), name);
            return new Jet(letter, (float) c[0] / c[1], name);
        } catch (RuntimeException | LinkageError e) {
            HudLog.error("gear-jetpack", "reading the Mekanism jetpack failed (no mode in the key hints)", e);
            return null;
        }
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

    /** Ad Astra's Jet Suit (full set): its energy left (0..1) for the Suit flight key hint's tag, or null. */
    static Float jetSuit(Player p) {
        try {
            return jetSuitEnergy(p);
        } catch (RuntimeException | LinkageError | ReflectiveOperationException e) {
            HudLog.error("gear-adastra", "reading the Jet Suit failed (no energy tag)", e);
            return null;
        }
    }

    private static Float jetSuitEnergy(Player p) throws ReflectiveOperationException {
        Class<?> suit = Reflect.cls("earth.terrarium.adastra.common.items.armor.SpaceSuitItem");
        if (suit == null) return null;
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        Object item = chest.getItem();
        if (!suit.isInstance(item) || !Reflect.bool(Reflect.callStatic(suit.getName(), "hasFullSet", p))) return null;
        // The Jet Suit's energy (getEnergy(stack, new ModifyOnlyContext(stack)): a ValueStorage).
        Class<?> jet = Reflect.cls("earth.terrarium.adastra.common.items.armor.JetSuitItem");
        if (jet == null || !jet.isInstance(item)) return null;
        Object storage = Reflect.call(item, "getEnergy", chest, context(chest));
        if (storage == null) return null;
        long e = ((Number) Reflect.call(storage, "getStoredAmount")).longValue(), cap = ((Number) Reflect.call(storage, "getCapacity")).longValue();
        return cap > 0 ? (float) e / cap : null;
    }

    private static Constructor<?> modifyOnly;

    private static Object context(ItemStack s) throws ReflectiveOperationException {
        if (modifyOnly == null) modifyOnly = Class.forName("earth.terrarium.common_storage_lib.context.impl.ModifyOnlyContext").getConstructor(ItemStack.class);
        return modifyOnly.newInstance(s);
    }

    /** A MekaSuit module's readout, as Mekanism's MekaSuit HUD has it: the module's name, its text upper case, a warning
     *  (Mekanism's red), and the fraction when the text is a percent (else null). */
    record Readout(String name, String text, boolean warning, Float frac) {}

    /** A MekaSuit piece's enabled modules with a readout (ICustomModule.addHUDElements: nutritional paste %, vision and
     *  gravitational modulation on or off, the dosimeter's and Geiger counter's levels). The jetpack unit's readout is
     *  the key hints' Fly row and chest mode (its HUD element is left out, MekanismJetpackUnitHudMixin). */
    static List<Readout> mekaModules(Player p, ItemStack s) {
        List<Readout> out = new ArrayList<>();
        if (s.isEmpty() || !Reflect.is(s.getItem(), "mekanism.common.content.gear.IModuleContainerItem")) return out;
        Object container = Reflect.call(Reflect.callStatic("mekanism.common.content.gear.ModuleHelper", "get"), "getModuleContainer", s);
        if (!(Reflect.call(container, "modules") instanceof java.util.Collection<?> modules)) return out;
        for (Object m : modules) {
            if (!Reflect.bool(Reflect.call(m, "isEnabled"))) continue;
            List<Object> elements = new ArrayList<>();
            Reflect.call(Reflect.call(m, "getCustomInstance"), "addHUDElements", m, container, s, p, (java.util.function.Consumer<Object>) elements::add);
            if (elements.isEmpty()) continue;
            Object data = Reflect.call(m, "getData");
            String name = Reflect.call(data, "getTextComponent") instanceof Component c ? c.getString() : String.valueOf(data);
            for (Object e : elements) {
                String text = Reflect.call(e, "getText") instanceof Component t ? t.getString().toUpperCase(java.util.Locale.ROOT) : null;
                int color = (int) Reflect.num(Reflect.call(e, "getColor"), 0xFFFFFF), r = color >> 16 & 0xFF, g = color >> 8 & 0xFF;
                Float frac = null;
                if (text != null && text.endsWith("%")) try { frac = Float.parseFloat(text.substring(0, text.length() - 1).trim()) / 100f; } catch (NumberFormatException ignored) {}
                out.add(new Readout(name, text, r > 2 * g + 40, frac));
            }
        }
        return out;
    }

    /** The Mekanism jetpack's hydrogen (a MekaSuit's jetpack unit too), the Jet Suit's energy and the MekaSuit modules'
     *  readouts as cyan statuses (a percent by its level, ON / OFF full or empty; their legend rows are in the key hints). */
    private static void keyedFuel(Player p, List<Gear> out) {
        Jet jet = jetpack(p);
        // always the Jetpack item's icon: a MekaSuit's jetpack unit is in the bodyarmor, whose icon the suit's energy uses
        if (jet != null) out.add(new Gear("mek_jetpack", jet.frac(), Component.literal("Jetpack hydrogen"), Component.literal(Math.round(jet.frac() * 100) + "%"),
                item("mekanism:jetpack")));
        Float energy = jetSuit(p);
        if (energy != null) out.add(new Gear("adastra_energy", energy, Component.literal("Jet Suit energy"),
                Component.literal(Math.round(energy * 100) + "%"), p.getItemBySlot(EquipmentSlot.CHEST)));
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack piece = p.getItemBySlot(slot);
            try {
                for (Readout r : mekaModules(p, piece)) {   // a percent fills by its level; ON full, OFF empty; other readings full
                    float frac = r.frac() != null ? r.frac() : "OFF".equals(r.text()) ? 0f : 1f;
                    out.add(new Gear("meka_module_" + r.name(), frac, Component.literal(r.name()), Component.literal(r.text() == null ? "" : r.text()), piece));
                }
            } catch (RuntimeException | LinkageError e) {
                HudLog.error("gear-meka-modules", "reading the MekaSuit's modules failed (their statuses left out)", e);
            }
        }
    }

    private static ItemStack item(String id) {
        var key = net.minecraft.resources.ResourceLocation.tryParse(id);
        var it = key == null ? null : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key);
        return it == null ? ItemStack.EMPTY : new ItemStack(it);
    }

}
