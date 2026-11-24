package dev.frosthaven.havencreateuiux.hud;

import java.lang.reflect.Method;
import java.util.Collection;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The held NTGL gun's choices for the chips row (main hand, else offhand; no compile dependency):
 * <ul>
 * <li>FIRE MODE chip, while the gun has more than one fire mode ({@code WeaponModifierHelper.getFireModes(data)},
 * attachments included: Create Gunsmithing's nailgun, shotgun, launcher): the current mode
 * ({@code WeaponStateHelper.getFireMode(data)}) as {@link ChipText#fireMode} words it (AUTO, SEMI, MULT), with NTGL's
 * fire select key.</li>
 * <li>AMMO chip, while it accepts more than one ammo ({@code getAmmoItems(data)}: revolver, gatling, nailgun, shotgun,
 * blazegun): the current ammo item ({@code getCurrentAmmo(data).getId()}, an id from the weapon data's "ammo" list such
 * as cgs:round_revolver) as {@link ChipText#ammo} words it (RND, AP, FLEC, NAIL, FUEL; unknown: its display name's first
 * 4 letters), with NTGL's ammo select key.</li>
 * </ul>
 * Each returns {words, key} or null.
 */
final class GunRead {
    private static Method fireMode, fireModes, ammoItems, currentAmmo;
    private static boolean missing;

    private GunRead() {}

    static String[] fireMode(Player p) {
        Object data = heldGun(p);
        if (data == null || !lookup()) return null;
        try {
            if (!(fireModes.invoke(null, data) instanceof Collection<?> modes) || modes.size() < 2) return null;
            String mode = ChipText.fireMode(path(fireMode.invoke(null, data)));
            HudLog.changed("fire-mode", mode, "FIRE MODE chip: {} of {} modes", mode, modes.size());
            return new String[] {mode, SlotRead.ntglKey("KEY_FIRE_SELECT", "FIRE MODE chip: NTGL fire select key")};
        } catch (ReflectiveOperationException | RuntimeException e) {
            HudLog.error("fire-mode-chip", "reading the held gun's fire mode failed (no FIRE MODE chip)", e);
            return null;
        }
    }

    static String[] ammo(Player p) {
        Object data = heldGun(p);
        if (data == null || !lookup()) return null;
        try {
            if (!(ammoItems.invoke(null, data) instanceof Collection<?> kinds) || kinds.size() < 2) return null;
            Object holder = currentAmmo.invoke(null, data);
            if (holder == null) return null;
            String id = String.valueOf(Reflect.call(holder, "getId"));
            String words = ChipText.ammo(id, name(holder));
            HudLog.changed("ammo-kind", words, "AMMO chip: {} ({}) of {} kinds", words, id, kinds.size());
            return new String[] {words, SlotRead.ntglKey("KEY_AMMO_SELECT", "AMMO chip: NTGL ammo select key")};
        } catch (ReflectiveOperationException | RuntimeException e) {
            HudLog.error("ammo-chip", "reading the held gun's ammo failed (no AMMO chip)", e);
            return null;
        }
    }

    /** The held NTGL gun's WeaponData (main hand, else offhand), or null. */
    private static Object heldGun(Player p) {
        for (ItemStack s : new ItemStack[] {p.getMainHandItem(), p.getOffhandItem()}) {
            if (!s.isEmpty() && SlotRead.isNtgl(s)) return SlotRead.weaponData(s, p);
        }
        return null;
    }

    /** A holder's id path ("ntgl:auto" -> "auto"); "" when there is none. */
    private static String path(Object holder) {
        String id = String.valueOf(holder == null ? null : Reflect.call(holder, "getId"));
        return holder == null ? "" : id.substring(id.indexOf(':') + 1);
    }

    /** An AmmoHolder's display name (its description id translated), or null. */
    private static String name(Object holder) {
        Object key = Reflect.call(holder, "getDescriptionId");
        return key instanceof String k && I18n.exists(k) ? I18n.get(k) : null;
    }

    /** The NTGL methods by their exact parameter types (getFireMode has a private ItemStack overload). */
    private static boolean lookup() {
        if (fireMode != null) return true;
        if (missing) return false;
        try {
            Class<?> data = Reflect.cls(SlotRead.NTGL + "data.WeaponData"), state = Reflect.cls(SlotRead.STATE), mods = Reflect.cls(SlotRead.MODIFIERS);
            if (data == null || state == null || mods == null) { missing = true; return false; }
            fireModes = mods.getMethod("getFireModes", data);
            ammoItems = mods.getMethod("getAmmoItems", data);
            currentAmmo = state.getMethod("getCurrentAmmo", data);
            fireMode = state.getMethod("getFireMode", data);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            HudLog.error("gun-read", "NTGL's fire mode and ammo methods not found (no FIRE MODE / AMMO chips)", e);
            fireMode = null;
            missing = true;
            return false;
        }
    }
}
