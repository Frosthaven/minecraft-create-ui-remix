package dev.frosthaven.havencreateuiux.mixin;

import dev.frosthaven.havencreateuiux.hud.CompactHud;
import dev.frosthaven.havencreateuiux.hud.HudLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mekanism's jetpack adds "Jetpack Mode" and "Stored" lines to Mekanism's top-left HUD; while the compact action bar
 * is active its instrument plate shows both as the jetpack chip, so the lines are left out. The rest of that HUD stays.
 */
@Mixin(targets = "mekanism.common.item.gear.ItemJetpack", remap = false)
public abstract class MekanismJetpackHudMixin {
    @Inject(method = "addHUDStrings", at = @At("HEAD"), cancellable = true)
    private void haven_create_ui_ux$chip(CallbackInfo ci) {
        if (CompactHud.active()) {
            ci.cancel();
            HudLog.once("jetpack-hud", "Mekanism's jetpack HUD lines left out (the instrument plate's chip shows them)");
        }
    }
}
