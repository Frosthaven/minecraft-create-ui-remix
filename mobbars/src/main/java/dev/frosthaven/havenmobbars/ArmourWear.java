package dev.frosthaven.havenmobbars;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The worn armour's durability left (0..1) for the bar's bottom strip: sum of (max damage - damage) over sum of max
 * damage of the damageable pieces in the armour slots and the body slot (horse and wolf armour), plus a Drekki's
 * barding (Drekki Rancher keeps it in its own field: read by reflection). {@link #NONE} when nothing damageable is worn.
 *
 * Computed at most once per client tick per entity and cached ({@link WeakHashMap}); a frame that draws the bar only
 * reads the cache. The client sees other entities' armour damage because vanilla sends an equipment packet whenever a
 * worn stack's components change ({@code LivingEntity.equipmentHasChanged}: {@code ItemStack.matches}).
 */
final class ArmourWear {
    static final float NONE = -1;
    private static final Log LOG = new Log();
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.BODY};
    private static final String DREKKI = "dev.frosthaven.ranchercompanion.entity.DrekkiEntity";
    private static final Map<LivingEntity, Entry> CACHE = new WeakHashMap<>();
    private static Class<?> drekki;
    private static MethodHandle barding;   // DrekkiEntity.bardingItem(), as (LivingEntity) -> ItemStack
    private static boolean drekkiChecked;

    private ArmourWear() {}

    private static final class Entry {
        long tick = Long.MIN_VALUE;
        float left = NONE;
    }

    /** The fraction left, or {@link #NONE}; {@code tick} is the client level's game time. */
    static float of(LivingEntity e, long tick) {
        Entry c = CACHE.get(e);
        if (c == null) CACHE.put(e, c = new Entry());
        if (c.tick != tick) {
            c.tick = tick;
            c.left = compute(e);
        }
        return c.left;
    }

    private static float compute(LivingEntity e) {
        long left = 0, max = 0;
        for (EquipmentSlot s : SLOTS) {
            ItemStack st = e.getItemBySlot(s);
            if (st.isDamageableItem()) {
                int m = st.getMaxDamage();
                max += m;
                left += Math.max(0, m - st.getDamageValue());
            }
        }
        ItemStack b = drekkiBarding(e);
        if (b != null && b.isDamageableItem()) {
            int m = b.getMaxDamage();
            max += m;
            left += Math.max(0, m - b.getDamageValue());
        }
        return max == 0 ? NONE : (float) left / max;
    }

    /** A Drekki's barding stack, or null (not a Drekki, Drekki Rancher absent, or the lookup failed). */
    private static ItemStack drekkiBarding(LivingEntity e) {
        if (!drekkiChecked) {
            drekkiChecked = true;
            try {
                Class<?> c = Class.forName(DREKKI, false, ArmourWear.class.getClassLoader());
                barding = MethodHandles.publicLookup().findVirtual(c, "bardingItem", MethodType.methodType(ItemStack.class))
                        .asType(MethodType.methodType(ItemStack.class, LivingEntity.class));
                drekki = c;
                LOG.once("drekki", "Drekki Rancher found: its barding counts as worn armour");
            } catch (ClassNotFoundException ex) {
                LOG.once("drekki", "Drekki Rancher not present");
            } catch (ReflectiveOperationException | LinkageError ex) {
                LOG.error("drekki", "DrekkiEntity.bardingItem() not found; barding not counted", ex);
            }
        }
        if (drekki == null || !drekki.isInstance(e)) return null;
        try {
            return (ItemStack) barding.invokeExact(e);
        } catch (Throwable ex) {
            LOG.error("drekki-call", "DrekkiEntity.bardingItem() failed; barding no longer counted", ex);
            drekki = null;
            return null;
        }
    }
}
