package dev.frosthaven.havencreateuiux.hud;

import java.util.Locale;
import java.util.Map;

/**
 * The words in the chips row, at most {@link #MAX} characters each (the key cap in front does not count). Pure: no game
 * classes, so the table can be checked outside the game. {@link WordChips} cuts any longer text with {@link #cut} and logs it.
 */
final class ChipText {
    /** The longest text a chip shows. */
    static final int MAX = 4;

    /** The RELOAD chip's word (the held gun is empty). */
    static final String RELOAD = "LOAD";

    /** Mekanism jetpack modes by their letter (N normal, V vector, H hover, D disabled). */
    private static final Map<Character, String> JET = Map.of('N', "REG", 'V', "VECT", 'H', "HOVR", 'D', "OFF");

    /** NTGL fire mode id paths that do not read well as their first 4 letters. */
    private static final Map<String, String> FIRE = Map.of("multi", "MULT");

    /** Create Gunsmithing ammo items (the weapon data's "ammo" list, AmmoHolder ids). */
    private static final Map<String, String> AMMO = Map.ofEntries(
            Map.entry("cgs:round_revolver", "RND"), Map.entry("cgs:round_gatling", "RND"), Map.entry("cgs:round_shotgun", "RND"),
            Map.entry("cgs:round_revolver_piercing", "AP"), Map.entry("cgs:round_gatling_piercing", "AP"),
            Map.entry("cgs:round_shotgun_flechette", "FLEC"), Map.entry("cgs:round_shotgun_flechette_steel", "STFL"),
            Map.entry("cgs:round_shotgun_incendiary", "FIRE"),
            Map.entry("cgs:round_shotgun_blank", "BLNK"), Map.entry("cgs:round_revolver_blank", "BLNK"), Map.entry("cgs:round_gatling_blank", "BLNK"),
            Map.entry("cgs:nail", "NAIL"), Map.entry("cgs:nail_steel", "STEL"),
            Map.entry("cgs:burnable", "FUEL"), Map.entry("cgs:blaze_cake", "CAKE"),
            Map.entry("cgs:rocket", "RCKT"), Map.entry("cgs:rocket_small", "MINI"));

    private ChipText() {}

    static String jet(char mode) { return JET.getOrDefault(mode, JET.get('N')); }

    /** A fire mode by its id's path ("auto", "semi", "multi"). */
    static String fireMode(String path) { return FIRE.getOrDefault(path, letters(path)); }

    /** An ammo by its item id; unknown: the first 4 letters of its display name (else of the id's path). */
    static String ammo(String id, String name) {
        String w = AMMO.get(id);
        if (w != null) return w;
        w = letters(name);
        return w.isEmpty() ? letters(id == null ? "" : id.substring(id.indexOf(':') + 1)) : w;
    }

    /** s cut to {@link #MAX} characters (null stays null). */
    static String cut(String s) { return s == null || s.length() <= MAX ? s : s.substring(0, MAX); }

    /** The first 4 letters and digits of s in capitals (the digit strip has A-Z and 0-9). */
    private static String letters(String s) {
        String up = s == null ? "" : s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return cut(up);
    }
}
