package dev.frosthaven.createdarkinventory.mixin;

import dev.frosthaven.createdarkinventory.SplitBlit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every texture blit ends in innerBlit: after it, {@link SplitBlit} draws the texture's grey layer, if it has one. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsSplitMixin {
    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At("TAIL"))
    private void createdarkinventory$grey(ResourceLocation texture, int x1, int x2, int y1, int y2, int z,
                                          float u0, float u1, float v0, float v1, CallbackInfo ci) {
        SplitBlit.after((GuiGraphics) (Object) this, texture, x1, x2, y1, y2, z, u0, u1, v0, v1, null);
    }

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At("TAIL"))
    private void createdarkinventory$greyTinted(ResourceLocation texture, int x1, int x2, int y1, int y2, int z,
                                                float u0, float u1, float v0, float v1, float r, float g, float b, float a, CallbackInfo ci) {
        SplitBlit.after((GuiGraphics) (Object) this, texture, x1, x2, y1, y2, z, u0, u1, v0, v1, new float[] {r, g, b, a});
    }
}
