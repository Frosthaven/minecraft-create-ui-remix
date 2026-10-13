package dev.frosthaven.createdarkinventory.hud;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Small cached reflection helpers for reading other mods (absent mod or member: null, logged once). */
final class Reflect {
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();
    private static final Object MISSING = new Object();

    private Reflect() {}

    static Class<?> cls(String name) {
        Object c = CACHE.computeIfAbsent("c:" + name, k -> {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException | LinkageError e) {
                HudLog.once("absent:" + name, "{} not present: its part of the bar is left out", name);
                return MISSING;
            }
        });
        return c == MISSING ? null : (Class<?>) c;
    }

    /** A method by name and parameter count on a class or its supertypes (public or declared). */
    static Method method(Class<?> c, String name, int params) {
        if (c == null) return null;
        Object m = CACHE.computeIfAbsent("m:" + c.getName() + "#" + name + "/" + params, k -> {
            for (Class<?> t = c; t != null; t = t.getSuperclass()) {
                for (Method x : t.getDeclaredMethods()) {
                    if (x.getName().equals(name) && x.getParameterCount() == params) { x.setAccessible(true); return x; }
                }
            }
            for (Method x : c.getMethods()) if (x.getName().equals(name) && x.getParameterCount() == params) return x;
            HudLog.once("nomethod:" + k, "{}.{} ({} params) not found", c.getName(), name, params);
            return MISSING;
        });
        return m == MISSING ? null : (Method) m;
    }

    static Field field(Class<?> c, String name) {
        if (c == null) return null;
        Object f = CACHE.computeIfAbsent("f:" + c.getName() + "#" + name, k -> {
            for (Class<?> t = c; t != null; t = t.getSuperclass()) {
                try {
                    Field x = t.getDeclaredField(name);
                    x.setAccessible(true);
                    return x;
                } catch (NoSuchFieldException e) {
                    // try the superclass
                }
            }
            HudLog.once("nofield:" + k, "{}.{} not found", c.getName(), name);
            return MISSING;
        });
        return f == MISSING ? null : (Field) f;
    }

    /** Calls a static method of class name; null when missing or it throws (logged once). */
    static Object callStatic(String cls, String name, Object... args) {
        return invoke(method(cls(cls), name, args.length), null, args);
    }

    /** Calls an instance method by name on target's class. */
    static Object call(Object target, String name, Object... args) {
        return target == null ? null : invoke(method(target.getClass(), name, args.length), target, args);
    }

    static Object getStatic(String cls, String name) {
        return read(field(cls(cls), name), null);
    }

    static Object get(Object target, String name) {
        return target == null ? null : read(field(target.getClass(), name), target);
    }

    private static Object read(Field f, Object target) {
        if (f == null) return null;
        try {
            return f.get(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            HudLog.error("get:" + f, "reading " + f + " failed", e);
            return null;
        }
    }

    private static Object invoke(Method m, Object target, Object[] args) {
        if (m == null) return null;
        try {
            return m.invoke(target, args);
        } catch (ReflectiveOperationException | RuntimeException e) {
            HudLog.error("call:" + m, "calling " + m + " failed", e);
            return null;
        }
    }

    static boolean is(Object o, String cls) {
        Class<?> c = cls(cls);
        return c != null && c.isInstance(o);
    }

    static boolean bool(Object o) { return o instanceof Boolean b && b; }
    static float num(Object o, float dflt) { return o instanceof Number n ? n.floatValue() : dflt; }
}
