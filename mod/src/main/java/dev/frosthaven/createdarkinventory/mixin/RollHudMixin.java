package dev.frosthaven.createdarkinventory.mixin;

import dev.frosthaven.createdarkinventory.hud.CompactHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block Factory's Bosses draws its dodge roll icon in RenderGuiEvent.Pre (not a layer we can hide), beside the
 * hotbar; the compact action bar draws the roll itself, so skip theirs while it is active.
 */
@Mixin(targets = "net.unusual.block_factorys_bosses.event.ClientEvents", remap = false)
public abstract class RollHudMixin {
    @Inject(method = "renderGUI", at = @At("HEAD"), cancellable = true)
    private static void createdarkinventory$compactHud(CallbackInfo ci) {
        if (CompactHud.active()) ci.cancel();
    }
}
