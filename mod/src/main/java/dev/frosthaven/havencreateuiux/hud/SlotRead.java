package dev.frosthaven.havencreateuiux.hud;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ChargedProjectiles;

/**
 * What a hotbar or offhand item shows in its slot ({@link SlotBadges}): a weapon's ammo and a Mekanism mode item's mode,
 * read without a compile dependency (data components, reflection; an absent mod reads nothing, logged once).
 * <ul>
 * <li>NTGL weapons (NukaTeam's Gun Lib, the base of Create Gunsmithing): loaded rounds from the {@code ntgl:ammo_count}
 * component over {@code WeaponModifierHelper.getMaxAmmo(WeaponData)}; the ammo's counter type ({@code ntgl:bar},
 * {@code ntgl:percent}: Blazegun, Pneumatic Hammer) as a percent without sign; thrown weapons (grenades: the stack
 * count is the ammo) and weapons without a magazine are left out. Low (a quarter or less, above 0) is orange, empty red. An
 * empty held gun shows LOAD in red on its Reload key hint ({@link #empty}, KeyRules).</li>
 * <li>Crossbows: loaded (the {@code charged_projectiles} component) over 1.</li>
 * <li>Every other projectile weapon (bows, the Electric Bow, Create's and TFMG's potato cannons, Cataclysm's Cursed Bow):
 * the inventory's items its {@code getAllSupportedProjectiles()} accepts (arrows, potato cannon ammo). Power Grid's
 * Electro Zapper accepts none (it runs on its battery): left out.</li>
 * <li>Mekanism mode items in a hand slot ({@code IAttachmentBasedModeItem.getMode}): the mode, abbreviated ({@link #MODES});
 * the Electric Bow shows only its arrows.
 * The Meka-Tool's modes are per module (radial), not one mode: none.</li>
 * </ul>
 */
public final class SlotRead {
    /** A slot's one line (its ammo, else its Mekanism mode), its digit row, and whether it is a Mekanism mode item (held,
     *  its slot shows Mekanism's item mode key). An item with ammo and a mode (the Electric Bow) shows only its ammo. A
     *  gun's fire mode and ammo choice are chips (GunRead). */
    record Badge(String text, int row) {}

    private SlotRead() {}

    static Badge read(Player p, ItemStack s) {
        if (s.isEmpty()) return null;
        try {
            String[] ammo = ammo(p, s);   // {text, "low" | "empty" | null}
            String[] mode = mode(s);      // {text, "green" | "dim" | null}
            if (ammo != null) return new Badge(ammo[0], row(ammo[1]));
            if (mode == null) return null;
            int modeRow = mode[1] == null ? CompactAtlas.WHITE : mode[1].equals("green") ? CompactAtlas.jetRow('N') : CompactAtlas.DIM;
            return new Badge(mode[0], modeRow);
        } catch (RuntimeException | LinkageError e) {
            HudLog.error("slot-read", "reading a slot's ammo or mode failed (" + s.getItem() + ")", e);
            return null;
        }
    }

    // ---- Ammo ----

    /** Each number of a badge at most 999 (1200/2000 shows 999/999), so "999/999" is the widest badge. */
    private static final int CAP = 999;

    static final String NTGL = "com.nukateam.ntgl.common.";
    static final String MODIFIERS = NTGL + "util.util.WeaponModifierHelper", STATE = NTGL + "util.util.WeaponStateHelper";
    private static Optional<DataComponentType<?>> ammoCount;
    private static Constructor<?> weaponData;
    /** Per stack (ItemStack has identity equality): {read at (ms), capacity, 1 when the counter is a bar or percent}. */
    private static final Map<ItemStack, long[]> NTGL_CACHE = new WeakHashMap<>();

    /** An ammo state's digit row: low orange (the jetpack's VECTOR row), empty red (its DISABLED row), else white. */
    private static int row(String state) {
        return state == null ? CompactAtlas.WHITE : CompactAtlas.jetRow(state.equals("empty") ? 'D' : 'V');
    }

    /** True while this NTGL gun is empty (its Reload key hint shows LOAD in red; a low gun shows only its orange slot count). */
    static boolean empty(Player p, ItemStack s) {
        try {
            if (!isNtgl(s)) return false;
            String[] a = ntgl(p, s);
            return a != null && "empty".equals(a[1]);
        } catch (RuntimeException | LinkageError e) {
            HudLog.error("reload-state", "reading a gun's ammo failed (no LOAD state)", e);
            return false;
        }
    }

    static boolean isNtgl(ItemStack s) { return Reflect.is(s.getItem(), NTGL + "foundation.item.interfaces.IWeapon"); }

    private static String[] ammo(Player p, ItemStack s) {
        if (isNtgl(s)) return ntgl(p, s);
        if (s.getItem() instanceof CrossbowItem) {
            ChargedProjectiles c = s.get(DataComponents.CHARGED_PROJECTILES);
            boolean loaded = c != null && !c.isEmpty();
            return new String[] {loaded ? "1/1" : "0/1", loaded ? null : "empty"};
        }
        if (s.getItem() instanceof ProjectileWeaponItem w) {
            if (w.getClass().getName().endsWith("ElectroZapperItem")) return null;   // a battery, no projectiles
            Predicate<ItemStack> ok = w.getAllSupportedProjectiles();
            int n = 0;
            Inventory inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) { ItemStack it = inv.getItem(i); if (!it.isEmpty() && ok.test(it)) n += it.getCount(); }
            HudLog.changed("ammo:" + s.getItem(), n == 0, "slot ammo: {} reads its projectiles from the inventory ({} now)", s.getItem(), n);
            return new String[] {String.valueOf(Math.min(n, CAP)), n == 0 ? "empty" : null};
        }
        return null;
    }

    private static String[] ntgl(Player p, ItemStack s) {
        if (ammoCount == null) {
            ammoCount = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath("ntgl", "ammo_count"));
            HudLog.once("ntgl-ammo", "slot ammo: ntgl:ammo_count {}", ammoCount.isPresent() ? "found" : "absent (no NTGL ammo)");
        }
        if (ammoCount.isEmpty()) return null;
        long now = System.currentTimeMillis();
        long[] c = NTGL_CACHE.get(s);
        if (c == null || now - c[0] > 500) {   // capacity and counter change only with attachments and the ammo kind
            c = ntglInfo(p, s, now);
            NTGL_CACHE.put(s, c);
        }
        if (c[1] <= 0) return null;
        int count = s.get(ammoCount.get()) instanceof Integer i ? i : 0;
        String state = count == 0 ? "empty" : count <= c[1] * 0.25 ? "low" : null;
        String text = c[2] == 1 ? String.valueOf(Math.round(100.0 * count / c[1])) : Math.min(count, CAP) + "/" + Math.min(c[1], CAP);
        return new String[] {text, state};
    }

    /** {now, capacity (0: no ammo shown), 1 when the counter is a bar or percent}. */
    private static long[] ntglInfo(Player p, ItemStack s, long now) {
        Object data = weaponData(s, p);
        if (data == null) return new long[] {now, 0, 0};
        if (Reflect.callStatic(MODIFIERS, "getWeaponAction", data) instanceof Object action
                && String.valueOf(Reflect.call(action, "getId")).endsWith(":throw")) return new long[] {now, 0, 0};   // a grenade: its count
        long max = Reflect.callStatic(MODIFIERS, "getMaxAmmo", data) instanceof Integer m ? m : 0;
        Object holder = Reflect.callStatic(STATE, "getCurrentAmmo", data);
        Object config = holder == null ? null : Reflect.callStatic(MODIFIERS, "getAmmoConfig", Reflect.call(holder, "getId"), data);
        String counter = String.valueOf(config == null ? null : Reflect.call(Reflect.call(config, "getCounter"), "getId"));
        long pct = counter.endsWith(":bar") || counter.endsWith(":percent") ? 1 : 0;
        HudLog.changed("ntgl:" + s.getItem(), max + " " + counter, "slot ammo: {} holds {} (counter {})", s.getItem(), max, counter);
        return new long[] {now, max, pct};
    }

    static Object weaponData(ItemStack s, Player p) {
        try {
            if (weaponData == null) {
                Class<?> c = Reflect.cls(NTGL + "data.WeaponData");
                if (c == null) return null;
                weaponData = c.getConstructor(ItemStack.class, LivingEntity.class);
            }
            return weaponData.newInstance(s, p);
        } catch (ReflectiveOperationException e) {
            HudLog.error("ntgl-data", "NTGL WeaponData could not be made (no ammo shown)", e);
            return null;
        }
    }

    // ---- Mekanism modes ----

    /** Mode names (enum constants) to at most 4 glyphs of the digit strip. Unknown ones: their first 4 letters. */
    static final Map<String, String> MODES = Map.ofEntries(
            // Configurator
            Map.entry("CONFIGURATE_ITEMS", "ITEM"), Map.entry("CONFIGURATE_FLUIDS", "FLUD"), Map.entry("CONFIGURATE_CHEMICALS", "CHEM"),
            Map.entry("CONFIGURATE_ENERGY", "ENRG"), Map.entry("CONFIGURATE_HEAT", "HEAT"), Map.entry("EMPTY", "EMTY"),
            Map.entry("ROTATE", "ROT"), Map.entry("WRENCH", "WRCH"),
            // Atomic Disassembler
            Map.entry("NORMAL", "NORM"), Map.entry("SLOW", "SLOW"), Map.entry("FAST", "FAST"), Map.entry("VEIN", "VEIN"), Map.entry("OFF", "OFF"),
            // Flamethrower
            Map.entry("COMBAT", "CMBT"), Map.entry("INFERNO", "INFR"));
    private static final String MODE_ITEM = "mekanism.common.item.interfaces.IModeItem$IAttachmentBasedModeItem";

    /** {text, colour: "green" (the portable fluid tank's bucket mode ON, the jetpack's NORMAL green; OFF white), "dim"
     *  (an enum's OFF: the Atomic Disassembler switched off), else null (white)}, or null. */
    private static String[] mode(ItemStack s) {
        Object item = s.getItem();
        if (!Reflect.is(item, MODE_ITEM) || !Reflect.bool(Reflect.call(item, "supportsSlotType", s, EquipmentSlot.MAINHAND))) return null;
        Object m = Reflect.call(item, "getMode", s);
        String text;
        if (m instanceof Boolean on) text = on ? "ON" : "OFF";
        else if (m instanceof Enum<?> e) {
            String letters = e.name().replaceAll("[^A-Z]", "");
            text = MODES.getOrDefault(e.name(), letters.substring(0, Math.min(4, letters.length())));
        } else return null;
        HudLog.changed("mode:" + s.getItem(), text, "slot mode: {} in mode {}", s.getItem(), text);
        return new String[] {text, m instanceof Boolean on ? (on ? "green" : null) : text.equals("OFF") ? "dim" : null};
    }

    /** While the bar draws, Mekanism's top-left HUD leaves out the hand slots' mode items (their mode is on the slot) and
     *  the Meka-Tool (MekanismHandHudMixin); armor lines stay. */
    public static boolean hideHandHud(ItemStack s) {
        Object item = s.getItem();
        return Reflect.is(item, "mekanism.common.item.interfaces.IModeItem") || Reflect.is(item, "mekanism.common.content.gear.IModuleContainerItem");
    }
}
