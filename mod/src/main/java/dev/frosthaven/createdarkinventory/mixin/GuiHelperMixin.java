package dev.frosthaven.createdarkinventory.mixin;

import dev.frosthaven.createdarkinventory.TabSlots;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Upgrade tab slots get their own texture when a pack has one ({@link TabSlots}). */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper", remap = false)
public abstract class GuiHelperMixin {
    @Inject(method = "renderSlotsBackground(Lnet/minecraft/client/gui/GuiGraphics;IIII)V", at = @At("HEAD"), cancellable = true)
    private static void createdarkinventory$tabSlots(GuiGraphics graphics, int x, int y, int columns, int rows, CallbackInfo ci) {
        if (TabSlots.draw(graphics, x, y, columns, rows)) ci.cancel();
    }
}
