package dev.frosthaven.havencreateuiux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The frogport and mailbox title (#3D3C48, the edit box and the drawn name). Dark Mode Everywhere
 * turns text light only when red, green and blue are all under 65; blue 72 missed it, so the title
 * stayed dark on the darkened header. Blue 64 lets DME's own rule (and its toggle) handle it.
 */
@Mixin(targets = "com.simibubi.create.content.logistics.packagePort.PackagePortScreen", remap = false)
public abstract class PackagePortScreenMixin {
    @ModifyConstant(method = {"init", "renderBg"}, constant = @Constant(intValue = 0x3D3C48))
    private int haven_create_ui_ux$titleColor(int color) {
        return 0x3D3C40;
    }
}
