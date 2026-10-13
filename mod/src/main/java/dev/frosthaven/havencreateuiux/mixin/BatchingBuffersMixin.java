package dev.frosthaven.havencreateuiux.mixin;

import dev.frosthaven.havencreateuiux.HudLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ImmediatelyFast's HUD batching collects the HUD and draws it in endHudBatching, and early in
 * tryForceDrawHudBuffers (AppleSkin and minimap compat call it mid-HUD). DME then sees
 * ImmediatelyFast or a RenderType as the caller, so neither its own HUD blacklist nor the layer
 * check in {@link HudLayers} matches. Flag both draws instead.
 */
@Mixin(targets = "net.raphimc.immediatelyfast.feature.batching.BatchingBuffers", remap = false)
public abstract class BatchingBuffersMixin {
    @Inject(method = {"endHudBatching", "tryForceDrawHudBuffers"}, at = @At("HEAD"))
    private static void haven_create_ui_ux$flushStart(CallbackInfo ci) {
        HudLayers.flushing(true);
    }

    @Inject(method = {"endHudBatching", "tryForceDrawHudBuffers"}, at = @At("RETURN"))
    private static void haven_create_ui_ux$flushEnd(CallbackInfo ci) {
        HudLayers.flushing(false);
    }
}
