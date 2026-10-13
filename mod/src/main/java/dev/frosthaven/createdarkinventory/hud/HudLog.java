package dev.frosthaven.createdarkinventory.hud;

import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/**
 * The compact HUD's log (logger "createdarkinventory/hud" in latest.log). Nothing logs per frame: {@link #once} logs a
 * key the first time, {@link #changed} when a key's value changes, so a session's log reads as a story of what the bar
 * found and did.
 */
public final class HudLog {
    public static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, Object> SEEN = new ConcurrentHashMap<>();

    private HudLog() {}

    /** The compact HUD's channel; other features log through their own {@link Channel} (another prefix). */
    private static final Channel HUD = new Channel("[compact HUD] ");

    /** Logs msg (info) the first time key is seen. */
    public static void once(String key, String msg, Object... args) { HUD.once(key, msg, args); }

    /** Logs msg (info) when key's value differs from the last one logged (also the first time). */
    public static void changed(String key, Object value, String msg, Object... args) { HUD.changed(key, value, msg, args); }

    /** Logs an error with its stack trace the first time key is seen. */
    public static void error(String key, String msg, Throwable t) { HUD.error(key, msg, t); }

    /** The same once / changed / error rules under a prefix of its own (keys are kept per prefix). */
    public static final class Channel {
        private final String prefix;

        public Channel(String prefix) { this.prefix = prefix; }

        public void once(String key, String msg, Object... args) {
            if (SEEN.putIfAbsent(prefix + "once:" + key, Boolean.TRUE) == null) LOG.info(prefix + msg, args);
        }

        public void changed(String key, Object value, String msg, Object... args) {
            Object v = value == null ? "null" : value, old = SEEN.put(prefix + "val:" + key, v);
            if (!Objects.equals(old, v)) LOG.info(prefix + msg, args);
        }

        public void error(String key, String msg, Throwable t) {
            if (SEEN.putIfAbsent(prefix + "err:" + key, Boolean.TRUE) == null) LOG.error(prefix + msg, t);
        }
    }
}
