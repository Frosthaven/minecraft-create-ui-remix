package dev.frosthaven.havenmobbars.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The renderer's own name tag rule (EntityRenderer.shouldShowName, overridden by LivingEntityRenderer and MobRenderer:
 *  custom name visible, or a custom name under the crosshair, teams, distance), so bars know when a tag is drawn. */
@Mixin(EntityRenderer.class)
public interface EntityRendererInvoker {
    @Invoker("shouldShowName")
    boolean haven_mob_health_bars$shouldShowName(Entity entity);
}
