package dev.frosthaven.havencreateuiux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The stock keeper's texts: the "Stock Keeper" header (#714A40), the search box and the delivery address (#4A2D31)
 * and the faded hint (#8C5D4B). Dark Mode Everywhere turns text light only when red, green and blue are all under 65,
 * so these browns stayed dark on its darkened panel. The same hue scaled under 65 lets DME's own rule (and its toggle)
 * handle them, as with the frogport title ({@link PackagePortScreenMixin}).
 */
@Mixin(targets = "com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen", remap = false)
public abstract class StockKeeperRequestScreenMixin {
    @ModifyConstant(method = {"init", "renderBg"}, constant = @Constant(intValue = 0x4A2D31), require = 0)
    private int haven_create_ui_ux$textColor(int color) {
        return 0x402730;
    }

    @ModifyConstant(method = "renderBg", constant = @Constant(intValue = 0xFF4A2D31), require = 0)
    private int haven_create_ui_ux$opaqueTextColor(int color) {
        return 0xFF402730;
    }

    @ModifyConstant(method = {"init", "renderBg"}, constant = @Constant(intValue = 0x714A40), require = 0)
    private int haven_create_ui_ux$headerColor(int color) {
        return 0x402A24;
    }

    @ModifyConstant(method = "renderBg", constant = @Constant(intValue = 0x8C5D4B), require = 0)
    private int haven_create_ui_ux$hintColor(int color) {
        return 0x402A22;
    }
}
