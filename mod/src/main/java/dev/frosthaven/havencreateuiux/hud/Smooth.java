package dev.frosthaven.havencreateuiux.hud;

/**
 * A shown value that eases to its target over the time the target last took to change, so a value the game updates in
 * steps (vanilla air each tick, Create's backtank air about once a second) moves smoothly, draining or refilling.
 */
final class Smooth {
    private double from = Double.NaN, to;
    private long t0, lastChange;
    private double dur = 50;

    /** expectedMs: the usual update interval (used for the first step after a pause). */
    double get(double target, long now, long expectedMs) {
        if (Double.isNaN(from)) { from = to = target; t0 = lastChange = now; return target; }
        if (target != to) {
            from = current(now);
            to = target;
            long seen = now - lastChange;
            dur = seen <= expectedMs * 3 / 2 ? Math.max(30, seen) : expectedMs;
            lastChange = t0 = now;
        }
        return current(now);
    }
    private double current(long now) {
        return from + (to - from) * Math.min(1, (now - t0) / dur);
    }
}
