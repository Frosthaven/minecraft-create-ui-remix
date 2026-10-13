package dev.frosthaven.havencreateuiux.hud;

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

    /** When the backtanks' total air last fell (ms), and the total then: a Create Jetpack drains it in flight. */
    private static long tankDrainAt = Long.MIN_VALUE / 2;
    private static int tankLast = -1;
    /** How long the tank stays shown after its air last fell out of the water (a jetpack flight, a pause between bursts). */
    private static final long TANK_DRAIN_SHOW = 3000;

    /** Create backtank air in seconds: {left, capacity}, or null when none shows on the breathing ring: under water or
     *  in lava (its VisualBacktankAir), as Create's remaining_air layer. A Create Jetpack draining the tanks in flight is
     *  not breathing: {@link #jetpackFlight} gives it as a cyan status. */
    static float[] backtank(Player player, boolean underwater) {
        Tanks t = tanks(player);
        if (t == null) return null;
        boolean wet = (underwater || player.isInLava()) && player.getPersistentData().contains("VisualBacktankAir");
        HudLog.changed("tank-source", wet ? "water" : t.draining() ? "flight" : "none", "backtank: {}",
                wet ? "under water / in lava (Create's counter), on the breathing ring" : t.draining() ? "draining out of the water (a jetpack in flight): a gear status" : "hidden");
        if (!wet) return null;
        int left = player.getPersistentData().getInt("VisualBacktankAir");
        return new float[] {left, Math.max(t.cap, left)};
    }

    /** A Create Jetpack in flight: the tanks' air falling out of the water (and TANK_DRAIN_SHOW after), as {air, capacity}
     *  seconds and the first tank worn (its icon), or null. */
    record Flight(float air, float cap, ItemStack icon) {}
    static Flight jetpackFlight(Player player, boolean underwater) {
        Tanks t = tanks(player);
        boolean wet = (underwater || player.isInLava()) && player.getPersistentData().contains("VisualBacktankAir");
        return t == null || wet || !t.draining() || t.total <= 0 ? null : new Flight(t.total, Math.max(t.cap, t.total), t.icon);
    }

    private record Tanks(int total, int cap, ItemStack icon, boolean draining) {}
    private static long tanksAt = -1;
    private static Tanks tanksLast;

    /** The worn tanks' air (getAir over getAllWithAir, jetpacks included), read once a frame; null without Create. */
    private static Tanks tanks(Player player) {
        if (player.isCreative()) return null;
        long now = net.minecraft.Util.getMillis();
        if (now == tanksAt) return tanksLast;
        int total = 0, cap = 0;
        ItemStack icon = ItemStack.EMPTY;
        try {
            Class<?> util = Class.forName("com.simibubi.create.content.equipment.armor.BacktankUtil");
            Method max = util.getMethod("maxAir", ItemStack.class), air = util.getMethod("getAir", ItemStack.class);
            for (Object s : (List<?>) util.getMethod("getAllWithAir", net.minecraft.world.entity.LivingEntity.class).invoke(null, player)) {
                if (icon.isEmpty() && s instanceof ItemStack st) icon = st;
                cap += ((Number) max.invoke(null, s)).intValue();
                total += ((Number) air.invoke(null, s)).intValue();
            }
        } catch (ClassNotFoundException | LinkageError e) {
            HudLog.once("create-absent", "Create's BacktankUtil not present: no backtank");
            return null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            HudLog.error("backtank", "reading Create's backtanks failed (Create's own counter value still used under water)", e);
        }
        if (tankLast >= 0 && total < tankLast) tankDrainAt = now;
        tankLast = total;
        tanksAt = now;
        return tanksLast = new Tanks(total, cap, icon, now - tankDrainAt < TANK_DRAIN_SHOW);
    }

    /** The backtanks' air fell within TANK_DRAIN_SHOW (AirGear prefers a source whose air is falling). */
    static boolean backtankFalling(long now) { return now - tankDrainAt < TANK_DRAIN_SHOW; }

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
