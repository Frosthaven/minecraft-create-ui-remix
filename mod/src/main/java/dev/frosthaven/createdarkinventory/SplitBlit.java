package dev.frosthaven.createdarkinventory;

import com.mojang.logging.LogUtils;
import dev.frosthaven.createdarkinventory.mixin.GuiGraphicsInvoker;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * The dark mode split. Our panels are dark art that DME must not darken as a whole (leather, wood, blueprints, brass
 * and steel keep their look), but their plain greys (panel body, slot wells) should follow DME like vanilla's grey
 * panels. The pack ships those greys again as "&lt;texture&gt;_grey.png": the same pixels, neutral and 5.5 x lighter,
 * transparent elsewhere (the redesign pages' greyLayer). After any blit of a texture that has one, this draws the grey
 * layer over it with the same coordinates, from this class: DME's caller is then SplitBlit:draw, which nothing
 * blacklists, so the layer gets DME's shader while the full art under it keeps its look ({@link PackScreens}).
 * DME off: a light grey panel. "Perfect dark" (divide 5.5): today's dark greys exactly; lighter settings, lighter greys.
 * Without DME nothing is drawn: the full art alone is the pack's look.
 */
public final class SplitBlit {
    private static final Logger LOG = LogUtils.getLogger();
    /** Texture to its grey layer, if the resource packs have one. Cleared on resource reload. */
    private static final Map<ResourceLocation, Optional<ResourceLocation>> GREY = new ConcurrentHashMap<>();
    /** GUI sprite to its grey sprite ("&lt;sprite&gt;_grey" in the GUI atlas), if the packs have one. */
    private static final Map<ResourceLocation, Optional<ResourceLocation>> GREY_SPRITE = new ConcurrentHashMap<>();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static boolean drawing;

    private SplitBlit() {}

    /** After a texture blit (GuiGraphics.innerBlit): its grey layer on top, through DME. color is null when untinted. */
    public static void after(GuiGraphics graphics, ResourceLocation texture, int x1, int x2, int y1, int y2, int z,
                             float u0, float u1, float v0, float v1, float[] color) {
        if (drawing || !DarkMode.installed()) return;
        Optional<ResourceLocation> grey = GREY.computeIfAbsent(texture, SplitBlit::find);
        if (grey.isEmpty()) return;
        drawing = true;
        try {
            draw(graphics, grey.get(), x1, x2, y1, y2, z, u0, u1, v0, v1, color);
        } catch (RuntimeException e) {
            if (LOGGED.add("error:" + texture)) LOG.error("[dark split] grey layer of {} failed; drawing the full art only", texture, e);
            GREY.put(texture, Optional.empty());
        } finally {
            drawing = false;
        }
    }

    /** After a GUI sprite blit (GuiGraphics.blitSprite, e.g. the recipe book button): its grey sprite on top, through DME. */
    public static void afterSprite(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int z, int width, int height) {
        if (drawing || !DarkMode.installed()) return;
        Optional<ResourceLocation> grey = GREY_SPRITE.computeIfAbsent(sprite, SplitBlit::findSprite);
        if (grey.isEmpty()) return;
        drawing = true;
        try {
            drawSprite(graphics, grey.get(), x, y, z, width, height);
        } catch (RuntimeException e) {
            if (LOGGED.add("error:" + sprite)) LOG.error("[dark split] grey sprite of {} failed; drawing the sprite only", sprite, e);
            GREY_SPRITE.put(sprite, Optional.empty());
        } finally {
            drawing = false;
        }
    }

    /** DME's caller for a grey sprite: keep this a separate method of this class. */
    private static void drawSprite(GuiGraphics graphics, ResourceLocation grey, int x, int y, int z, int width, int height) {
        graphics.blitSprite(grey, x, y, z, width, height);
    }

    private static Optional<ResourceLocation> findSprite(ResourceLocation sprite) {
        if (sprite.getPath().endsWith("_grey")) return Optional.empty();
        var grey = ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), sprite.getPath() + "_grey");
        if (!Minecraft.getInstance().getGuiSprites().getSprite(grey).contents().name().equals(grey)) return Optional.empty();
        if (LOGGED.add(grey.toString())) LOG.info("[dark split] sprite {} has a grey layer ({}): its greys follow Dark Mode Everywhere", sprite, grey);
        return Optional.of(grey);
    }

    /** DME's caller for the grey layer: keep this a separate method of this class. */
    private static void draw(GuiGraphics graphics, ResourceLocation grey, int x1, int x2, int y1, int y2, int z,
                             float u0, float u1, float v0, float v1, float[] color) {
        var invoker = (GuiGraphicsInvoker) graphics;
        if (color == null) invoker.createdarkinventory$innerBlit(grey, x1, x2, y1, y2, z, u0, u1, v0, v1);
        else invoker.createdarkinventory$innerBlit(grey, x1, x2, y1, y2, z, u0, u1, v0, v1, color[0], color[1], color[2], color[3]);
    }

    private static Optional<ResourceLocation> find(ResourceLocation texture) {
        String path = texture.getPath();
        if (!path.endsWith(".png") || path.endsWith("_grey.png") || path.startsWith("textures/atlas/")) return Optional.empty();
        var grey = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), path.substring(0, path.length() - 4) + "_grey.png");
        if (Minecraft.getInstance().getResourceManager().getResource(grey).isEmpty()) return Optional.empty();
        if (LOGGED.add(grey.toString())) LOG.info("[dark split] {} has a grey layer ({}): its greys follow Dark Mode Everywhere", texture, grey);
        return Optional.of(grey);
    }

    /** Resource packs changed: look the grey layers up again. */
    public static void reload() {
        GREY.clear();
        GREY_SPRITE.clear();
    }
}
