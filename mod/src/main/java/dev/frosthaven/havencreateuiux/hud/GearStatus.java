package dev.frosthaven.havencreateuiux.hud;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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
 *   <li>Mekanism scuba tank: oxygen, tank 0. (The MekaSuit's breathing unit makes oxygen from energy: no tank.)</li>
 *   <li>Ad Astra space suits with the full set worn: oxygen (SpaceSuitItem.getOxygenAmount over the chestplate's tank,
 *       its overlay's rule); the Jet Suit also its energy (JetSuitItem.getEnergy). The Jet Suit has no separate fuel.</li>
 *   <li>Creating Space oxygen backtanks (copper, netherite) as the chestplate: O2 (OxygenBacktankUtil, its
 *       remaining_oxygen layer's rule).</li>
 * </ul>
 * Every reader returns nothing when its mod is absent (logged once).
 */
final class GearStatus {
    /** A status: the fraction left, the name and the amount text for the tooltip, and the worn item for the icon. */
    record Gear(String id, float frac, Component name, Component text, ItemStack icon) {}
    private static final NumberFormat NUM = NumberFormat.getIntegerInstance();

    private GearStatus() {}

    /** The worn gear's statuses, the MekaSuit's energy first. */
    static List<Gear> read(Player p) {
        List<Gear> out = new ArrayList<>();
        HudExtras.Suit suit = HudExtras.mekaSuit(p);
        if (suit != null) out.add(new Gear("mekasuit", suit.frac(), Component.literal("MekaSuit energy"), suit.text(), suit.icon()));
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
        try {
            creatingSpace(p, out);
        } catch (RuntimeException | LinkageError | ReflectiveOperationException e) {
            HudLog.error("gear-creatingspace", "reading Creating Space oxygen backtanks failed (its status left out)", e);
        }
        return out;
    }

    // --- Mekanism -------------------------------------------------------------------------------------------------

    private static void mekanism(Player p, List<Gear> out) {
        Class<?> jetpack = Reflect.cls("mekanism.common.item.gear.ItemJetpack"), scuba = Reflect.cls("mekanism.common.item.gear.ItemScubaTank"),
                suit = Reflect.cls("mekanism.common.item.gear.ItemMekaSuitArmor");
        if (jetpack == null) return;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD}) {
            ItemStack s = p.getItemBySlot(slot);
            Object item = s.getItem();
            if (jetpack.isInstance(item)) tank(s, "mek_jetpack", "Jetpack hydrogen", out);
            else if (scuba.isInstance(item)) tank(s, "mek_scuba", "Scuba tank oxygen", out);
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
        Object multi = Reflect.getStatic("mekanism.common.capabilities.Capabilities", "CHEMICAL");
        if (!(Reflect.call(multi, "item") instanceof ItemCapability<?, ?> cap)) return;
        @SuppressWarnings("unchecked") Object handler = s.getCapability((ItemCapability<Object, Void>) cap);
        if (handler == null || ((Number) Reflect.call(handler, "getChemicalTanks")).intValue() <= 0) return;
        long max = ((Number) Reflect.call(handler, "getChemicalTankCapacity", 0)).longValue();
        Object stack = Reflect.call(handler, "getChemicalInTank", 0);
        long amount = stack == null ? 0 : ((Number) Reflect.call(stack, "getAmount")).longValue();
        if (max <= 0) return;
        out.add(new Gear(id, (float) amount / max, Component.literal(name), amount(amount, max, "mB"), s));
    }

    // --- Ad Astra -------------------------------------------------------------------------------------------------

    private static void adAstra(Player p, List<Gear> out) throws ReflectiveOperationException {
        Class<?> suit = Reflect.cls("earth.terrarium.adastra.common.items.armor.SpaceSuitItem");
        if (suit == null) return;
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        Object item = chest.getItem();
        if (!suit.isInstance(item) || !Reflect.bool(Reflect.callStatic(suit.getName(), "hasFullSet", p))) return;
        // Oxygen: the chestplate's tank (its overlay: getOxygenAmount over the limit of FluidAmounts.toPlatformAmount(tankSize)).
        long amount = ((Number) Reflect.callStatic(suit.getName(), "getOxygenAmount", p)).longValue();
        long size = ((Number) Reflect.get(item, "tankSize")).longValue();
        long max = ((Number) Reflect.callStatic("earth.terrarium.common_storage_lib.resources.fluid.util.FluidAmounts", "toPlatformAmount", size)).longValue();
        if (max > 0) out.add(new Gear("adastra_oxygen", (float) amount / max,
                Component.literal("Space suit oxygen"), amount(amount, max, "mB"), chest));
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

    // --- Creating Space -------------------------------------------------------------------------------------------

    private static Method maxOxygen, getOxygen;

    private static void creatingSpace(Player p, List<Gear> out) throws ReflectiveOperationException {
        Class<?> tank = Reflect.cls("com.rae.creatingspace.content.life_support.spacesuit.OxygenBacktankItem");
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (tank == null || !tank.isInstance(chest.getItem())) return;
        if (maxOxygen == null) {   // typed: maxOxygen has an (int) overload too
            Class<?> util = Class.forName("com.rae.creatingspace.content.life_support.spacesuit.OxygenBacktankUtil");
            maxOxygen = util.getMethod("maxOxygen", ItemStack.class);
            getOxygen = util.getMethod("getOxygen", ItemStack.class);
        }
        int max = (int) maxOxygen.invoke(null, chest), o2 = (int) getOxygen.invoke(null, chest);
        if (max > 0) out.add(new Gear("cs_oxygen", (float) o2 / max,
                Component.literal("O2 backtank"), amount(o2, max, "O2"), chest));
    }

    private static Component amount(long amount, long max, String unit) {
        return Component.literal(NUM.format(amount) + "/" + NUM.format(max) + " " + unit);
    }
}
