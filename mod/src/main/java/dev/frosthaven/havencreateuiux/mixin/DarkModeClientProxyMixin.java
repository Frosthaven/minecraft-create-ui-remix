package dev.frosthaven.havencreateuiux.mixin;

import dev.frosthaven.havencreateuiux.HudLayers;
import dev.frosthaven.havencreateuiux.PackScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where DME stays off: the title screen (the pack's FancyMenu layout is its own design) and the
 * main HUD, backpack screens and the inventory while a Create HUD pack is on ({@link HudLayers},
 * {@link PackScreens}). DME asks this for every draw
 * (shader and text color), so answering "blacklisted" there covers both.
 */
@Mixin(targets = "com.buuz135.darkmodeeverywhere.ClientProxy", remap = false)
public abstract class DarkModeClientProxyMixin {
    @Inject(method = "isElementNameBlacklisted", at = @At("HEAD"), cancellable = true)
    private static void haven_create_ui_ux$titleScreen(String name, CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().screen instanceof TitleScreen || HudLayers.skipDarkMode(name) || PackScreens.skipDarkMode(name)) cir.setReturnValue(true);
    }
}
