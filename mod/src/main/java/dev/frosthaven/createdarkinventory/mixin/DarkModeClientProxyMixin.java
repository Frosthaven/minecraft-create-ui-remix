package dev.frosthaven.createdarkinventory.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No dark mode on the title screen: the pack's FancyMenu layout is its own design. DME asks
 * this for every draw (shader and text color), so answering "blacklisted" there covers both.
 */
@Mixin(targets = "com.buuz135.darkmodeeverywhere.ClientProxy", remap = false)
public abstract class DarkModeClientProxyMixin {
    @Inject(method = "isElementNameBlacklisted", at = @At("HEAD"), cancellable = true)
    private static void createdarkinventory$titleScreen(String name, CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().screen instanceof TitleScreen) cir.setReturnValue(true);
    }
}
