package dev.frosthaven.createdarkinventory.mixin;

import dev.frosthaven.createdarkinventory.HudLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ImmediatelyFast's HUD batching collects the HUD and draws it all in endHudBatching, after the
 * HUD layers ran: DME then sees ImmediatelyFast as the caller, so neither its own HUD blacklist
 * nor the layer check in {@link HudLayers} matches. Flag the flush instead.
 */
@Mixin(targets = "net.raphimc.immediatelyfast.feature.batching.BatchingBuffers", remap = false)
public abstract class BatchingBuffersMixin {
    @Inject(method = "endHudBatching", at = @At("HEAD"))
    private static void createdarkinventory$flushStart(CallbackInfo ci) {
        HudLayers.flushing(true);
    }

    @Inject(method = "endHudBatching", at = @At("RETURN"))
    private static void createdarkinventory$flushEnd(CallbackInfo ci) {
        HudLayers.flushing(false);
    }
}
