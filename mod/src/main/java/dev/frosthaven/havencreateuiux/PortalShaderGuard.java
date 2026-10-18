package dev.frosthaven.havencreateuiux;

import java.lang.reflect.Field;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import dev.frosthaven.havencreateuiux.hud.HudLog;

/**
 * Immersive Portals' shader path (every Iris renderer it has, {@code IrisCompatibilityPortalRenderer} in the end) copies
 * the depth buffer with {@code glCopyImageSubData}, an OpenGL 4.3 call, and never checks that the driver has it
 * (upstream issue 1792). macOS stops at OpenGL 4.1, so turning shaders on near a portal there aborts the JVM:
 * "No context is current or a function that is not available in the current context was called". On a machine without
 * that call this holds Immersive Portals' render mode at {@code none} while Iris shaders are on (portals still teleport,
 * they are just not drawn through), tells the player once, and gives the mode back when shaders go off. Where the call
 * exists nothing happens. Reflection only: Immersive Portals or Iris absent, or their internals moved, means no guard
 * (logged once), never a crash of its own.
 */
public final class PortalShaderGuard {
    private static final HudLog.Channel LOG = new HudLog.Channel("[portal guard] ");
    private static final String IP_GLOBAL = "qouteall.imm_ptl.core.IPGlobal", IRIS = "net.irisshaders.iris.Iris";
    private static Boolean needed;      // null until checked on the render thread with a context
    private static Object forcedFrom;   // the render mode replaced by "none" while shaders are on
    private static boolean told, broken;
    private static int tick;

    private PortalShaderGuard() {}

    public static void register(IEventBus bus) {
        if (!ModList.get().isLoaded("immersive_portals_core") || !ModList.get().isLoaded("iris")) return;
        bus.addListener(ClientTickEvent.Post.class, PortalShaderGuard::onTick);
    }

    private static void onTick(ClientTickEvent.Post event) {
        if (broken || ++tick % 10 != 0) return;   // twice a second is plenty: the renderer is re-chosen every frame
        try {
            if (needed == null) {
                GLCapabilities caps = GL.getCapabilities();
                needed = !(caps.OpenGL43 || caps.GL_ARB_copy_image);
                LOG.once("check", "glCopyImageSubData {}: Immersive Portals' shader path {}",
                        needed ? "missing (no OpenGL 4.3 or ARB_copy_image)" : "available",
                        needed ? "is held off while shaders are on" : "needs no guard here");
            }
            if (!needed) return;
            Field mode = Class.forName(IP_GLOBAL).getField("renderMode");
            Object current = mode.get(null), none = constant(mode.getType(), "none");
            if (shadersOn()) {
                if (current != none) {
                    if (forcedFrom == null) forcedFrom = current;
                    mode.set(null, none);
                    LOG.once("forced", "shaders on: Immersive Portals render mode {} -> none (no OpenGL 4.3 here)", current);
                }
                Minecraft mc = Minecraft.getInstance();
                if (!told && mc.player != null) {
                    mc.gui.getChat().addMessage(Component.translatable("haven_create_ui_ux.portal_guard.shaders"));
                    told = true;
                }
            } else if (forcedFrom != null) {
                if (current == none) mode.set(null, forcedFrom);
                LOG.once("restored:" + forcedFrom, "shaders off: Immersive Portals render mode back to {}", forcedFrom);
                forcedFrom = null;
                told = false;
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            broken = true;
            LOG.error("guard", "could not reach Immersive Portals' render mode or Iris; the portal shader guard is off", e);
        }
    }

    /** Iris's own answer: a shader pack is in use (what Immersive Portals checks before taking its shader path). */
    private static boolean shadersOn() throws ReflectiveOperationException {
        Object pack = Class.forName(IRIS).getMethod("getCurrentPack").invoke(null);
        return pack instanceof Optional<?> o && o.isPresent();
    }

    private static Object constant(Class<?> type, String name) {
        for (Object c : type.getEnumConstants()) if (((Enum<?>) c).name().equals(name)) return c;
        throw new IllegalStateException(type.getName() + " has no constant " + name);
    }
}
