package dev.frosthaven.havencreateuiux.mixin;

import dev.frosthaven.havencreateuiux.hud.CompactHud;
import dev.frosthaven.havencreateuiux.hud.HudLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mekanism's MekaSuit HUD draws each worn piece's battery percent at the top left; while the compact action bar is
 * active the suit's energy is ours (a cyan status, and a tag on the MekaSuit's Modules key hint), and so are its
 * modules' readouts (rows in the MekaSuit's key hints), so both icon rows are left out; its compass too (JourneyMap
 * shows the way and the coordinates).
 */
@Mixin(targets = "mekanism.client.render.HUDRenderer", remap = false)
public abstract class MekanismSuitEnergyHudMixin {
    @Inject(method = "renderMekaSuitEnergyIcons", at = @At("HEAD"), cancellable = true)
    private void haven_create_ui_ux$ours(CallbackInfo ci) {
        if (CompactHud.active()) {
            ci.cancel();
            HudLog.once("mekasuit-energy-hud", "the MekaSuit HUD's battery icons left out (the suit's energy is a status and a key hint tag)");
        }
    }

    @Inject(method = "renderMekaSuitModuleIcons", at = @At("HEAD"), cancellable = true)
    private void haven_create_ui_ux$oursModules(CallbackInfo ci) {
        if (CompactHud.active()) {
            ci.cancel();
            HudLog.once("mekasuit-module-hud", "the MekaSuit HUD's module icons left out (the key hints list each module's readout)");
        }
    }

    @Inject(method = "renderCompass", at = @At("HEAD"), cancellable = true)
    private void haven_create_ui_ux$noCompass(CallbackInfo ci) {
        if (CompactHud.active()) {
            ci.cancel();
            HudLog.once("mekasuit-compass-hud", "the MekaSuit HUD's compass left out (JourneyMap shows the way)");
        }
    }
}
