package dev.frosthaven.havencreateuiux.mixin;

import dev.frosthaven.havencreateuiux.hud.CompactHud;
import dev.frosthaven.havencreateuiux.hud.HudLog;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ad Astra draws its space suit's oxygen tanks and battery at the top left (OverlayScreen.render); while the compact
 * action bar is active they are ours (the oxygen on the breathing meter, AirGear; the Jet Suit's energy as a cyan
 * status and on its Fly key hint), so the overlay is left out. In a rocket it stays: its fuel and altitude bar has no replacement here.
 */
@Mixin(targets = "earth.terrarium.adastra.client.screens.player.OverlayScreen", remap = false)
public abstract class AdAstraOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void haven_create_ui_ux$ours(CallbackInfo ci) {
        var p = Minecraft.getInstance().player;
        boolean rocket = p != null && p.getVehicle() != null && p.getVehicle().getClass().getName().contains("Rocket");
        if (CompactHud.active() && !rocket) {
            ci.cancel();
            HudLog.once("adastra-overlay", "Ad Astra's space suit overlay left out (oxygen on the breathing meter, the battery a status and a key hint tag)");
        }
    }
}
