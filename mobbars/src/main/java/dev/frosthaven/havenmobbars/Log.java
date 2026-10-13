package dev.frosthaven.havenmobbars;

import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/** The mod's log in latest.log, prefixed "[mob bars]". Nothing per frame: {@link #once} logs a key the first time,
 *  {@link #changed} when its value changes, {@link #error} once per key with the stack trace. */
final class Log {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String PREFIX = "[mob bars] ";
    private final Map<String, Object> seen = new ConcurrentHashMap<>();

    void once(String key, String msg, Object... args) {
        if (seen.putIfAbsent("once:" + key, Boolean.TRUE) == null) LOG.info(PREFIX + msg, args);
    }

    void changed(String key, Object value, String msg, Object... args) {
        Object v = value == null ? "null" : value, old = seen.put("val:" + key, v);
        if (!Objects.equals(old, v)) LOG.info(PREFIX + msg, args);
    }

    void error(String key, String msg, Throwable t) {
        if (seen.putIfAbsent("err:" + key, Boolean.TRUE) == null) LOG.error(PREFIX + msg, t);
    }
}
