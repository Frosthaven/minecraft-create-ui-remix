package dev.frosthaven.havencreateuiux.hud;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sable (Aeronautics ships): ship blocks live in plot space far from the world. Clicks on them arrive as normal block
 * hits in plot coordinates, so distances and "the block under the player" need Sable's own transforms
 * (SableCompanion.projectOutOfSubLevel, getTrackingSubLevel + SubLevel.transformPositionInverse). Without Sable both
 * helpers return their input.
 */
final class KeySable {
    private KeySable() {}

    private static final String COMPANION = "dev.ryanhcode.sable.companion.SableCompanion";

    private static Object companion() { return Reflect.getStatic(COMPANION, "INSTANCE"); }

    /** A position in world space: a plot-space point on a ship is moved to where the ship is now. */
    static Vec3 world(Level level, Vec3 at) {
        Object c = companion();
        return c != null && Reflect.call(c, "projectOutOfSubLevel", level, at) instanceof Vec3 v ? v : at;
    }

    /** The block under an entity's feet: on a ship, the ship's block (plot space). */
    static BlockPos below(Entity e) {
        Object c = companion();
        Object ship = c == null ? null : Reflect.call(c, "getTrackingSubLevel", e);
        if (ship != null && Reflect.call(ship, "transformPositionInverse", e.position()) instanceof Vec3 local)
            return BlockPos.containing(local.x, local.y - 0.2, local.z);
        return e.blockPosition().below();
    }
}
