package dev.frosthaven.havencreateuiux;

import java.lang.reflect.Field;

/** Whether Dark Mode Everywhere's shader is on (its toggle). False without DME. */
public final class DarkMode {
    private static final Field SELECTED = find();

    private DarkMode() {}

    /** DME is installed (its toggle exists, on or off). */
    public static boolean installed() {
        return SELECTED != null;
    }

    public static boolean on() {
        try {
            return SELECTED != null && SELECTED.get(null) != null;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private static Field find() {
        try {
            return Class.forName("com.buuz135.darkmodeeverywhere.ClientProxy").getField("SELECTED_SHADER_VALUE");
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
