package dev.frosthaven.havencreateuiux.hud;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * The compact HUD's textures with mipmaps and linear filtering. They are 8 texels a GUI pixel, drawn at 1/8: with
 * vanilla's nearest sampling (no mipmaps) a GUI scale of 2 to 4 skips texels, so thin bright lines (rims, rivets, the
 * glass highlight) flicker in and out as white specks. Here each texture gets {@link #LEVELS} halvings, alpha-weighted
 * (no dark edges), with each level's transparent texels taking their neighbours' colour (no fringes when filtering
 * blends across an edge). Reloads with the resource packs (TextureManager resets registered textures).
 */
final class MipTexture extends AbstractTexture {
    static final int LEVELS = 3;   // 8x down to 1x
    private final ResourceLocation location;

    private MipTexture(ResourceLocation location) { this.location = location; }

    /** Puts each location in the TextureManager as a MipTexture (once; after that the manager keeps and reloads it). */
    static void install(Collection<ResourceLocation> locations) {
        var tm = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation loc : locations) {
            if (!(tm.getTexture(loc, null) instanceof MipTexture)) tm.register(loc, new MipTexture(loc));
        }
    }

    @Override
    public void load(ResourceManager rm) throws IOException {
        NativeImage base;
        try (InputStream in = rm.getResourceOrThrow(location).open()) { base = NativeImage.read(in); }
        CompactAtlas.checkPieces(location, base);
        NativeImage[] levels = new NativeImage[LEVELS + 1];
        levels[0] = base;
        for (int i = 1; i <= LEVELS; i++) levels[i] = half(levels[i - 1]);
        for (NativeImage l : levels) bleed(l);
        HudLog.once("mip:" + location, "texture {} ({}x{}): {} mip levels, linear filtering", location, base.getWidth(), base.getHeight(), LEVELS);
        if (RenderSystem.isOnRenderThreadOrInit()) upload(levels);
        else RenderSystem.recordRenderCall(() -> upload(levels));
    }

    private void upload(NativeImage[] levels) {
        TextureUtil.prepareImage(getId(), LEVELS, levels[0].getWidth(), levels[0].getHeight());
        for (int i = 0; i < levels.length; i++) {
            levels[i].upload(i, 0, 0, 0, 0, levels[i].getWidth(), levels[i].getHeight(), true, false, true, true);
        }
        setFilter(true, true);
    }

    /** Half size, each texel the alpha-weighted mean of its 2x2 block (packed ABGR, as NativeImage stores it). */
    private static NativeImage half(NativeImage src) {
        int w = src.getWidth() / 2, h = src.getHeight() / 2;
        NativeImage out = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                long a = 0, c0 = 0, c1 = 0, c2 = 0;
                for (int k = 0; k < 4; k++) {
                    int p = src.getPixelRGBA(2 * x + (k & 1), 2 * y + (k >> 1)), pa = p >>> 24;
                    a += pa; c0 += (long) (p & 0xFF) * pa; c1 += (long) ((p >> 8) & 0xFF) * pa; c2 += (long) ((p >> 16) & 0xFF) * pa;
                }
                int oa = (int) (a / 4);
                out.setPixelRGBA(x, y, a == 0 ? 0 : oa << 24 | (int) (c2 / a) << 16 | (int) (c1 / a) << 8 | (int) (c0 / a));
            }
        }
        return out;
    }

    /** Transparent texels next to visible ones take their mean colour (alpha stays 0): filtering reads one texel out. */
    private static void bleed(NativeImage img) {
        int w = img.getWidth(), h = img.getHeight();
        {
            int[] next = new int[w * h];
            boolean[] set = new boolean[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = img.getPixelRGBA(x, y);
                    if (p >>> 24 != 0 || (p & 0xFFFFFF) != 0) continue;   // visible, or already coloured
                    int n = 0, c0 = 0, c1 = 0, c2 = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = x + dx, ny = y + dy;
                            if ((dx | dy) == 0 || nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                            int q = img.getPixelRGBA(nx, ny);
                            if (q >>> 24 == 0 && (q & 0xFFFFFF) == 0) continue;
                            n++; c0 += q & 0xFF; c1 += (q >> 8) & 0xFF; c2 += (q >> 16) & 0xFF;
                        }
                    }
                    if (n == 0) continue;
                    next[y * w + x] = c2 / n << 16 | c1 / n << 8 | c0 / n;
                    set[y * w + x] = true;
                }
            }
            for (int i = 0; i < next.length; i++) if (set[i]) img.setPixelRGBA(i % w, i / w, next[i]);
        }
    }
}
