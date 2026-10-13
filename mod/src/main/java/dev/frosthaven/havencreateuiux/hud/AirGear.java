package dev.frosthaven.havencreateuiux.hud;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Breathing gear on the right orb's ring (bar mode: its row 1), in the backtank's copper, with its time left: one
 * source at a time, as seconds {left, capacity}. Each is shown while in use as the Create backtank is: under water,
 * without breathable air, or while its stored air falls (and {@link #DRAIN_SHOW} after). The drain rates, from the
 * 1.21.1 jars' bytecode, turn the amount into time:
 * <ul>
 *   <li>Create backtank under water: its air is seconds ({@link HudStats#backtank}). A Create Jetpack in flight is a
 *       gear status, not breathing.</li>
 *   <li>Creating Space O2 backtank (copper, netherite) as the chestplate: 1 O2 a second (CSEventHandler, every 20 ticks;
 *       under water its helmet uses it as Create's backtank does).</li>
 *   <li>Mekanism scuba tank with the scuba mask on (CommonPlayerTickHandler.isScubaMaskOn): it refills the air the
 *       player loses, 1 mB a tick under water.</li>
 *   <li>Ad Astra space suit (full set): 1 mB every 12 ticks without breathable air (OxygenApi.hasOxygen), another
 *       every 12 ticks with the eyes in water (SpaceSuitItem.inventoryTick).</li>
 * </ul>
 * Several in use: the one whose air is falling now, else Create backtank, Creating Space, Mekanism scuba, Ad Astra.
 */
final class AirGear {
    static final long DRAIN_SHOW = 3000;
    private static final Map<String, long[]> LAST = new HashMap<>();   // id -> {amount, when it last fell (ms)}
    private static Method csMax, csGet;

    private AirGear() {}

    private record Source(String id, String name, float left, float cap, boolean inUse, boolean falling) {}

    /** The ring's source this frame as {seconds left, capacity}, or null. */
    static float[] read(Player p, boolean underwater) {
        if (p.isCreative()) return null;
        long now = net.minecraft.Util.getMillis();
        Source best = null;
        Source[] all = {create(p, underwater, now), creatingSpace(p, underwater, now), scuba(p, underwater, now), adAstra(p, underwater, now)};
        for (Source s : all) if (s != null && s.inUse && s.falling && best == null) best = s;
        if (best == null) for (Source s : all) if (s != null && s.inUse) { best = s; break; }
        HudLog.changed("air-gear", best == null ? "none" : best.id, "breathing ring: {}", best == null ? "hidden"
                : best.name + (best.falling ? " (its air falling)" : " (in use)") + ", " + (int) best.left + " s of " + (int) best.cap + " s");
        return best == null ? null : new float[] {best.left, Math.max(best.cap, best.left)};
    }

    /** Remembers the amount; true while it fell within DRAIN_SHOW. */
    private static boolean falling(String id, long amount, long now) {
        long[] l = LAST.computeIfAbsent(id, k -> new long[] {amount, Long.MIN_VALUE / 2});
        if (amount < l[0]) l[1] = now;
        l[0] = amount;
        return now - l[1] < DRAIN_SHOW;
    }

    private static Source create(Player p, boolean underwater, long now) {
        float[] t = HudStats.backtank(p, underwater);
        return t == null ? null : new Source("create", "Create backtank", t[0], t[1], true, HudStats.backtankFalling(now));
    }

    private static Source creatingSpace(Player p, boolean underwater, long now) {
        try {
            Class<?> tank = Reflect.cls("com.rae.creatingspace.content.life_support.spacesuit.OxygenBacktankItem");
            ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
            if (tank == null || !tank.isInstance(chest.getItem())) return null;
            if (csMax == null) {   // typed: maxOxygen has an (int) overload too
                Class<?> util = Class.forName("com.rae.creatingspace.content.life_support.spacesuit.OxygenBacktankUtil");
                csMax = util.getMethod("maxOxygen", ItemStack.class);
                csGet = util.getMethod("getOxygen", ItemStack.class);
            }
            int max = (int) csMax.invoke(null, chest), o2 = (int) csGet.invoke(null, chest);
            boolean fell = falling("creatingspace", o2, now);
            return max <= 0 ? null : new Source("creatingspace", "Creating Space O2 backtank", o2, max, underwater || fell, fell);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            HudLog.error("air-creatingspace", "reading the Creating Space O2 backtank failed (left off the ring)", e);
            return null;
        }
    }

    private static Source scuba(Player p, boolean underwater, long now) {
        try {
            Class<?> scuba = Reflect.cls("mekanism.common.item.gear.ItemScubaTank");
            ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
            if (scuba == null || !scuba.isInstance(chest.getItem())) return null;
            long[] c = GearStatus.chemical(chest);
            if (c == null) return null;
            boolean mask = Reflect.bool(Reflect.callStatic("mekanism.common.CommonPlayerTickHandler", "isScubaMaskOn", p, chest));
            boolean fell = falling("scuba", c[0], now);
            return new Source("scuba", "Mekanism scuba tank", c[0] / 20f, c[1] / 20f, (mask && underwater) || fell, fell);
        } catch (RuntimeException | LinkageError e) {
            HudLog.error("air-scuba", "reading the Mekanism scuba tank failed (left off the ring)", e);
            return null;
        }
    }

    private static Source adAstra(Player p, boolean underwater, long now) {
        try {
            Class<?> suit = Reflect.cls("earth.terrarium.adastra.common.items.armor.SpaceSuitItem");
            ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
            if (suit == null || !suit.isInstance(chest.getItem()) || !Reflect.bool(Reflect.callStatic(suit.getName(), "hasFullSet", p))) return null;
            long amount = ((Number) Reflect.callStatic(suit.getName(), "getOxygenAmount", p)).longValue();
            long size = ((Number) Reflect.get(chest.getItem(), "tankSize")).longValue();
            long max = ((Number) Reflect.callStatic("earth.terrarium.common_storage_lib.resources.fluid.util.FluidAmounts", "toPlatformAmount", size)).longValue();
            Object api = Reflect.getStatic("earth.terrarium.adastra.api.systems.OxygenApi", "API");
            boolean airless = api != null && !Reflect.bool(Reflect.call(api, "hasOxygen", p));
            boolean eyes = p.isEyeInFluid(FluidTags.WATER), fell = falling("adastra", amount, now);
            // 1 mB per 12 ticks each for no air and for water: seconds = amount * 12 / 20 / uses (at least one use).
            int uses = Math.max(1, (airless ? 1 : 0) + (eyes ? 1 : 0));
            float k = 12f / 20f / uses;
            return max <= 0 ? null : new Source("adastra", "Ad Astra space suit", amount * k, max * k, airless || eyes || fell, fell);
        } catch (RuntimeException | LinkageError e) {
            HudLog.error("air-adastra", "reading the Ad Astra space suit's oxygen failed (left off the ring)", e);
            return null;
        }
    }
}
