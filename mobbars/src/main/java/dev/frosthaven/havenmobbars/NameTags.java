package dev.frosthaven.havenmobbars;

import dev.frosthaven.havenmobbars.mixin.EntityRendererInvoker;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

/**
 * Where vanilla draws an entity's name tag, and whether it draws one this frame, by the renderer's own rules
 * (EntityRenderer.render and renderNameTag, 1.21.1 with NeoForge): the tag is posted as {@link RenderNameTagEvent}
 * (mods may force it on or off), else the renderer's shouldShowName decides; it is drawn within the nameplate distance
 * at the entity's NAME_TAG attachment plus half a block, one font line tall (9 px at 0.025 per px, downward from that
 * point) on a background from 1 px above to 9 px below.
 */
final class NameTags {
    /** The tag's text top above its anchor (the background's top edge: 1 font px at 0.025). */
    static final float BACKGROUND_TOP = 0.025f;
    private static final Map<Entity, TriState> FORCED = new WeakHashMap<>();   // this frame's event result, per entity

    private NameTags() {}

    /** Lowest priority: the result after every other mod has had its say. The name tag event comes before
     *  RenderLivingEvent.Post in the same render call. */
    static void onNameTag(RenderNameTagEvent event) {
        FORCED.put(event.getEntity(), event.canRender());
    }

    /** The tag's anchor in the render's local space (the text's top line), whether or not a tag is drawn. */
    static Vec3 anchor(Entity e, float partialTick) {
        Vec3 v = e.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, e.getViewYRot(partialTick));
        return (v == null ? new Vec3(0, e.getBbHeight(), 0) : v).add(0, 0.5, 0);
    }

    /** True when vanilla draws this entity's name tag this frame. */
    static boolean shown(Entity e, EntityRenderer<?> renderer) {
        TriState forced = FORCED.remove(e);
        if (forced == TriState.FALSE) return false;
        Minecraft mc = Minecraft.getInstance();
        double d = mc.getEntityRenderDispatcher().distanceToSqr(e);
        if (!ClientHooks.isNameplateInRenderDistance(e, d)) return false;
        if (e.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, 0) == null) return false;
        return forced == TriState.TRUE || ((EntityRendererInvoker) renderer).haven_mob_health_bars$shouldShowName(e);
    }
}
