package dev.frosthaven.createdarkinventory.mixin;

import dev.frosthaven.createdarkinventory.DarkBlit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/** Create's player inventory, the frogport and mailbox screens and the clipboard: drawn through {@link DarkBlit}. */
@Mixin(targets = "com.simibubi.create.foundation.gui.AllGuiTextures", remap = false)
public abstract class AllGuiTexturesMixin {
    private static final Set<String> DARK = Set.of(
            "textures/gui/player_inventory.png",
            "textures/gui/clipboard.png",
            "textures/gui/frogport_and_mailbox.png");

    @Shadow @Final public ResourceLocation location;
    @Shadow @Final private int width;
    @Shadow @Final private int height;
    @Shadow @Final private int startX;
    @Shadow @Final private int startY;

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true)
    private void createdarkinventory$render(GuiGraphics graphics, int x, int y, CallbackInfo ci) {
        if (location.getNamespace().equals("create") && DARK.contains(location.getPath())) {
            DarkBlit.blit(graphics, location, x, y, startX, startY, width, height);
            ci.cancel();
        }
    }
}
