package dev.frosthaven.createdarkinventory.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** GuiGraphics' package-private innerBlit, for {@link dev.frosthaven.createdarkinventory.SplitBlit}. */
@Mixin(GuiGraphics.class)
public interface GuiGraphicsInvoker {
    @Invoker("innerBlit")
    void createdarkinventory$innerBlit(ResourceLocation texture, int x1, int x2, int y1, int y2, int z,
                                       float u0, float u1, float v0, float v1);

    @Invoker("innerBlit")
    void createdarkinventory$innerBlit(ResourceLocation texture, int x1, int x2, int y1, int y2, int z,
                                       float u0, float u1, float v0, float v1, float r, float g, float b, float a);
}
