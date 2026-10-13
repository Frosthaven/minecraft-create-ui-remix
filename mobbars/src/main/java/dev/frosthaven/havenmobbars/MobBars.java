package dev.frosthaven.havenmobbars;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

/**
 * Floating health bars (designed on the redesign page mob-bars.html): a flat, dim bar above every living entity within
 * {@link MobBarsConfig#RANGE} blocks, facing the camera, coloured by kind (hostile = {@link Enemy}, players, every
 * other mob), with recent damage as a pale trail that waits, then drains.
 *
 * Drawn from {@link RenderLivingEvent.Post} into the entity pass's own buffers with the name tag background's render
 * type, like a name tag: Iris shader packs and Sodium handle it as they handle name tags, it is depth-tested (walls
 * hide it, so no line-of-sight raycast), unlit (full brightness: the colours are already dim) and outside the GUI, so
 * Dark Mode Everywhere never touches it. Skipped in Iris's shadow pass, and with F1 (hidden GUI).
 */
public final class MobBars {
    private static final Log LOG = new Log();
    // Sizes in model pixels (16 = a block), as on the page.
    private static final float W = 14, H = 1.4f, EDGE = 0.15f, LIFT = 0.5f, NAME_LIFT = 0.3f;
    private static final int BACK = argb(158, 10, 11, 14), EDGE_COL = argb(179, 0, 0, 0);
    private static final int HOSTILE = 0xa83a3a, NOT_HOSTILE = 0x5c9a46, PLAYER = 0x4a76b8, ATTACKER = 0xa8409a;
    private static final long ATTACKER_MS = 30_000;   // a player who hit any player stays magenta this long
    private static final Map<Player, Long> ATTACKERS = new WeakHashMap<>();
    private static final java.util.Set<Integer> LOGGED_KINDS = new java.util.HashSet<>();   // a first-draw line per colour
    private static final long TRAIL_WAIT = 400, TRAIL_DRAIN = 600;   // ms
    private static final Map<LivingEntity, Trail> TRAILS = new WeakHashMap<>();
    private static Method shadowPass;   // Iris: IrisApi.getInstance().isRenderingShadowPass(), when Iris is installed
    private static Object irisApi;
    private static boolean irisChecked;

    private MobBars() {}

    /** The health last seen and the trail behind it (recent damage). */
    private static final class Trail {
        float hp, from;
        long hitAt;
    }

    public static void onRender(RenderLivingEvent.Post<?, ?> event) {
        try {
            render(event);
        } catch (RuntimeException e) {
            LOG.error("render", "drawing a bar failed; bars stay off for that entity this frame", e);
        }
    }

    private static void render(RenderLivingEvent.Post<?, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        LivingEntity e = event.getEntity();
        boolean on = MobBarsConfig.ENABLED.get();
        LOG.changed("enabled", on, "mob health bars {} (range {} blocks; hostile {}, other mobs {}, friendly players {}, hostile players {})",
                on ? "on" : "off", MobBarsConfig.RANGE.get(), MobBarsConfig.HOSTILE.get(),
                MobBarsConfig.NOT_HOSTILE.get(), MobBarsConfig.FRIENDLY_PLAYERS.get(), MobBarsConfig.HOSTILE_PLAYERS.get());
        if (!on || mc.player == null || mc.options.hideGui || e == mc.player || e instanceof ArmorStand) return;
        if (e.isInvisibleTo(mc.player) || e.isDeadOrDying() || inShadowPass()) return;
        int range = MobBarsConfig.RANGE.get();
        if (e.distanceToSqr(mc.player) > (double) range * range) return;
        int col;
        if (e instanceof Player) {
            if (isWebGhost(e)) return;   // Haven Link's web map viewers: never a bar
            // Hostile players (hit any player in the last 30 s) and friendly ones (everyone else) have separate toggles.
            Long hit = ATTACKERS.get(e);
            boolean hostile = hit != null && System.currentTimeMillis() - hit < ATTACKER_MS;
            if (!(hostile ? MobBarsConfig.HOSTILE_PLAYERS.get() : MobBarsConfig.FRIENDLY_PLAYERS.get())) return;
            col = hostile ? ATTACKER : PLAYER;
        } else if (e instanceof Enemy) {
            if (!MobBarsConfig.HOSTILE.get()) return;
            col = HOSTILE;
        } else {
            if (!MobBarsConfig.NOT_HOSTILE.get()) return;
            col = NOT_HOSTILE;
        }
        float max = Math.max(1, e.getMaxHealth()), hp = Math.max(0, Math.min(max, e.getHealth()));
        float trail = trail(e, hp);

        if (LOGGED_KINDS.add(col)) LOG.once("drawn:" + col, "first bar of colour #{} drawn: {} ({} of {} hp, {} blocks away)",
                Integer.toHexString(col), e.getType().getDescriptionId(), hp, max, String.format("%.1f", Math.sqrt(e.distanceToSqr(mc.player))));
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // Half a block over the head (a name tag sits there: then above it), turned to face the camera, in model px.
        boolean named = e instanceof Player || e.shouldShowName();
        pose.translate(0, e.getBbHeight() + LIFT + (named ? NAME_LIFT : 0), 0);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(1 / 16f, -1 / 16f, 1 / 16f);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = event.getMultiBufferSource().getBuffer(RenderType.textBackground());
        int light = LightTexture.FULL_BRIGHT;
        float x0 = -W / 2, x1 = W / 2, y0 = -H / 2, y1 = H / 2;
        float fx = x0 + W * hp / max, tx = x0 + W * Math.max(hp, trail) / max;
        // Coplanar pieces that never overlap (no z-fighting, no doubled alpha): the edge as four strips, then the
        // fill (its top strip a little lighter), the trail and the empty backing side by side.
        quad(vc, m, x0 - EDGE, y0 - EDGE, x1 + EDGE, y0, EDGE_COL, light);
        quad(vc, m, x0 - EDGE, y1, x1 + EDGE, y1 + EDGE, EDGE_COL, light);
        quad(vc, m, x0 - EDGE, y0, x0, y1, EDGE_COL, light);
        quad(vc, m, x1, y0, x1 + EDGE, y1, EDGE_COL, light);
        if (fx > x0) {
            quad(vc, m, x0, y0, fx, y0 + EDGE, 0xff000000 | mix(col, 0xffffff, 0.08f), light);
            quad(vc, m, x0, y0 + EDGE, fx, y1, 0xff000000 | col, light);
        }
        if (tx > fx) quad(vc, m, fx, y0, tx, y1, 0xaa000000 | mix(col, 0xc8c0b0, 0.55f), light);
        if (x1 > tx) quad(vc, m, tx, y0, x1, y1, BACK, light);
        pose.popPose();
    }

    /** The trail's current end: on damage it holds the health before the hit, waits, then drains to the health. */
    /** Haven Link's web map ghosts: client-only players on its "haven_web_ghosts" team, with name-based (version 3)
     *  UUIDs, where real accounts have version 4. Either marks one. */
    private static boolean isWebGhost(LivingEntity e) {
        var team = e.getTeam();
        return team != null && "haven_web_ghosts".equals(team.getName()) || e.getUUID().version() == 3;
    }

    /** A player hit a player (RecentAttackerMixin, any PvP): their bar is magenta for {@link #ATTACKER_MS}. */
    public static void attackedBy(Player attacker) {
        if (ATTACKERS.put(attacker, System.currentTimeMillis()) == null) LOG.once("attacker:" + attacker.getUUID(), "recently aggressive: " + attacker.getName().getString());
    }

    private static float trail(LivingEntity e, float hp) {
        long now = Util.getMillis();
        Trail t = TRAILS.computeIfAbsent(e, k -> {
            Trail n = new Trail();
            n.hp = n.from = hp;
            return n;
        });
        if (hp < t.hp) {   // hit: the trail starts from what is still showing
            t.from = Math.max(t.hp, current(t, now));
            t.hitAt = now;
        } else if (hp > t.hp) {
            t.from = hp;   // healed: no trail
        }
        t.hp = hp;
        return current(t, now);
    }

    private static float current(Trail t, long now) {
        float k = Math.max(0, Math.min(1, (now - t.hitAt - TRAIL_WAIT) / (float) TRAIL_DRAIN));
        return t.hp + (t.from - t.hp) * (1 - k);
    }

    /** True while Iris renders its shadow map (entities render twice; the bar would cast a shadow). */
    private static boolean inShadowPass() {
        if (!irisChecked) {
            irisChecked = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                shadowPass = api.getMethod("isRenderingShadowPass");
                LOG.once("iris", "Iris found: bars skip its shadow pass");
            } catch (ReflectiveOperationException | LinkageError ex) {
                LOG.once("iris", "Iris not present");
            }
        }
        if (shadowPass == null) return false;
        try {
            return (boolean) shadowPass.invoke(irisApi);
        } catch (ReflectiveOperationException ex) {
            LOG.error("iris-call", "IrisApi.isRenderingShadowPass failed; no longer asking", ex);
            shadowPass = null;
            return false;
        }
    }

    /** A quad facing the camera. textBackground() culls back faces, and the -y scale flips the winding, so the
     *  vertices go bottom edge first (as the name tag background's effect quad: y0 is its bottom): wound the other
     *  way the bar faced away from the camera and was culled. */
    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, int argb, int light) {
        vc.addVertex(m, x0, y1, 0).setColor(argb).setLight(light);
        vc.addVertex(m, x1, y1, 0).setColor(argb).setLight(light);
        vc.addVertex(m, x1, y0, 0).setColor(argb).setLight(light);
        vc.addVertex(m, x0, y0, 0).setColor(argb).setLight(light);
    }

    private static int argb(int a, int r, int g, int b) { return a << 24 | r << 16 | g << 8 | b; }

    /** The colour between two 0xRRGGBB colours (t from a to b). */
    private static int mix(int a, int b, float t) {
        int r = 0;
        for (int s = 0; s <= 16; s += 8) {
            int x = a >> s & 0xff, y = b >> s & 0xff;
            r |= Math.round(x + (y - x) * t) << s;
        }
        return r;
    }
}
