package dev.frosthaven.createdarkinventory.mixin;

import dev.frosthaven.createdarkinventory.DarkMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The clipboard's mid-line cursor is a black fill, which DME does not touch (no texture, no
 * text); on the darkened paper it would vanish. White while dark mode is on.
 */
@Mixin(targets = "com.simibubi.create.content.equipment.clipboard.ClipboardScreen", remap = false)
public abstract class ClipboardScreenMixin {
    @ModifyConstant(method = "renderCursor", constant = @Constant(intValue = 0xFF000000))
    private int createdarkinventory$cursorColor(int color) {
        return DarkMode.on() ? 0xFFFFFFFF : color;
    }
}
