package dev.frosthaven.havenmobbars;

import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * The living entity under the crosshair up to {@link MobBarsConfig#LOOK_RANGE} blocks, when no block is in between: it
 * gets a bar beyond the normal range ({@link MobBars}). One pick per client tick, the way vanilla picks (each entity's
 * box inflated by its pick radius), then one block raycast from the eye to the hit point. A multipart mob (Iron's Ice
 * Spider, the Ender Dragon) is picked through its parts, so a hit part counts as its living parent.
 */
final class LookTarget {
    private static final Log LOG = new Log();
    private static WeakReference<LivingEntity> target = new WeakReference<>(null);

    private LookTarget() {}

    /** The entity looked at this tick, or null. */
    static LivingEntity get() { return target.get(); }

    static void onTick(ClientTickEvent.Post event) {
        try {
            target = new WeakReference<>(pick());
        } catch (RuntimeException e) {
            target = new WeakReference<>(null);
            LOG.error("look", "picking the looked-at entity failed; only the normal range applies this tick", e);
        }
    }

    private static LivingEntity pick() {
        Minecraft mc = Minecraft.getInstance();
        boolean on = MobBarsConfig.ENABLED.get() && MobBarsConfig.LOOKED_AT.get();
        int range = MobBarsConfig.LOOK_RANGE.get();
        LOG.changed("looked-at", on + ":" + range, "bar on the looked-at entity {} (up to {} blocks, no block in between)", on ? "on" : "off", range);
        Entity cam = mc.getCameraEntity();
        if (!on || mc.level == null || mc.player == null || cam == null) return null;
        Vec3 eye = cam.getEyePosition(1f), look = cam.getViewVector(1f), end = eye.add(look.scale(range));
        AABB box = cam.getBoundingBox().expandTowards(look.scale(range)).inflate(1);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(cam, eye, end, box,
                e -> owner(e) != null && owner(e) != mc.player && !e.isSpectator() && e.isPickable(), (double) range * range);
        if (hit == null) return null;
        Vec3 at = hit.getLocation();
        BlockHitResult wall = mc.level.clip(new ClipContext(eye, at, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, cam));
        if (wall.getType() != HitResult.Type.MISS && wall.getLocation().distanceToSqr(eye) < at.distanceToSqr(eye)) return null;
        return owner(hit.getEntity());
    }

    /** The living entity itself, or a part's living parent; else null. */
    private static LivingEntity owner(Entity e) {
        if (e instanceof PartEntity<?> part) e = part.getParent();
        return e instanceof LivingEntity living ? living : null;
    }
}
