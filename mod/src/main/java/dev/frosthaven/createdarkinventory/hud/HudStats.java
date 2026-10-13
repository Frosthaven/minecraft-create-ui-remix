package dev.frosthaven.createdarkinventory.hud;

import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Values from other mods for the compact HUD, read by reflection so each mod is optional (absent: null / -1, and its
 * row hides). Names from each mod's bytecode (1.21.1 builds in this pack).
 */
final class HudStats {
    private HudStats() {}

    /** Iron's Spellbooks mana: {current, max}, or null. */
    static float[] irons(Player player) {
        Method get = method("io.redspace.ironsspellbooks.player.ClientMagicData", "getPlayerMana");
        if (get == null) return null;
        var max = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "max_mana"));
        if (max.isEmpty()) return null;
        try {
            return new float[] {((Number) get.invoke(null)).floatValue(), (float) player.getAttributeValue(max.get())};
        } catch (ReflectiveOperationException | RuntimeException e) {
            HudLog.error("irons", "reading Iron's Spellbooks mana failed (row hidden)", e);
            return null;
        }
    }

    /** Ars Nouveau mana: {current, max}, or null. */
    static float[] ars(Player player) {
        Method get = method("com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry", "getMana");
        if (get == null) return null;
        try {
            Object cap = get.invoke(null, player);
            if (cap == null) return null;
            double cur = ((Number) cap.getClass().getMethod("getCurrentMana").invoke(cap)).doubleValue();
            int max = ((Number) cap.getClass().getMethod("getMaxMana").invoke(cap)).intValue();
            return max > 0 ? new float[] {(float) cur, max} : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            HudLog.error("ars", "reading Ars Nouveau mana failed (row hidden)", e);
            return null;
        }
    }

    /** Block Factory's Bosses dodge roll: {charges ready (fractional), count}, or null. */
    static float[] roll(Player player) {
        Method from = method("net.unusual.block_factorys_bosses.attachment.entity.RollAttachment", "fromPlayer");
        if (from == null) return null;
        try {
            Object a = from.invoke(null, player);
            int count = ((Number) a.getClass().getMethod("rollCount").invoke(a)).intValue();
            Method cd = a.getClass().getMethod("getCooldown", int.class);
            float ready = 0;
            for (int i = 0; i < count; i++) ready += 1 - ((Number) cd.invoke(a, i)).floatValue();
            return new float[] {ready, count};
        } catch (ReflectiveOperationException | RuntimeException e) {
            HudLog.error("roll", "reading the dodge roll failed (cog shown instead)", e);
            return null;
        }
    }

    /** Create backtank air in seconds: {left, capacity}, or null when Create shows none (its remaining_air rule). */
    static float[] backtank(Player player, boolean underwater) {
        if (player.isCreative() || !player.getPersistentData().contains("VisualBacktankAir")) return null;
        if (!underwater && !player.isInLava()) return null;
        int left = player.getPersistentData().getInt("VisualBacktankAir");
        int cap = 0;
        try {
            Class<?> util = Class.forName("com.simibubi.create.content.equipment.armor.BacktankUtil");
            Method max = util.getMethod("maxAir", ItemStack.class);
            for (Object s : (List<?>) util.getMethod("getAllWithAir", net.minecraft.world.entity.LivingEntity.class).invoke(null, player)) {
                cap += ((Number) max.invoke(null, s)).intValue();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            HudLog.error("backtank", "reading Create's backtank capacity failed (time still shown)", e);
            cap = 0;
        }
        return new float[] {left, Math.max(cap, left)};
    }

    private static final java.util.Map<String, Method> METHODS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Method NONE;
    static {
        try { NONE = Object.class.getMethod("hashCode"); } catch (NoSuchMethodException e) { throw new IllegalStateException(e); }
    }

    /** A static method of a class by name (first one with that name), cached; null if the mod is absent. */
    private static Method method(String cls, String name) {
        Method m = METHODS.computeIfAbsent(cls + "#" + name, k -> {
            try {
                for (Method x : Class.forName(cls).getMethods()) if (x.getName().equals(name)) return x;
            } catch (ClassNotFoundException | LinkageError e) {
                HudLog.once("absent:" + cls, "{} not present ({}): its part of the bar is left out", cls, e.getClass().getSimpleName());
            }
            return NONE;
        });
        return m == NONE ? null : m;
    }
}
