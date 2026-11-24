package dev.frosthaven.havencreateuiux.mixin;

import dev.frosthaven.havencreateuiux.hud.CompactHud;
import dev.frosthaven.havencreateuiux.hud.HudLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The MekaSuit's jetpack unit adds its mode icon and hydrogen percent to Mekanism's MekaSuit HUD; while the compact
 * action bar is active its key hints show both (the mode in its colour, the hydrogen on Fly) and a cyan status the
 * hydrogen, so that element is left out.
 */
@Mixin(targets = "mekanism.common.content.gear.mekasuit.ModuleJetpackUnit", remap = false)
public abstract class MekanismJetpackUnitHudMixin {
    @Inject(method = "addHUDElements", at = @At("HEAD"), cancellable = true)
    private void haven_create_ui_ux$ours(CallbackInfo ci) {
        if (CompactHud.active()) {
            ci.cancel();
            HudLog.once("jetpack-unit-hud", "the MekaSuit jetpack unit's HUD element left out (the key hints and a status show it)");
        }
    }
}
