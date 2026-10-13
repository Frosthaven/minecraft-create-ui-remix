package dev.frosthaven.havencreateuiux.hud;

import static dev.frosthaven.havencreateuiux.hud.CompactAtlas.*;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import static dev.frosthaven.havencreateuiux.hud.HudDraw.*;

import com.mojang.math.Axis;
import dev.frosthaven.havencreateuiux.UiConfig;
import net.minecraft.util.Mth;
import java.util.List;
import java.util.Set;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.glfw.GLFW;

/**
 * The compact action bar (designed on the redesign page hud-compact.html): one iron bar in place of vanilla's
 * hotbar, hearts, food, armor, air, XP, mount health and jump bar, and the mod meters next to them (Iron's and Ars
 * mana, AppleSkin, Create's backtank counter, the dodge roll). Active while the pack's textures are present
 * (createhud:textures/gui/compact/*); drawn at 1/8 GUI px from them, so it matches the page texel for texel.
 * Positions are the page's layout (logical units, x from the screen centre, the bar's bottom at 50) times 8.
 * Here: the plate, hotbar, XP pipe and level, the dash, the floating plates, the items; the vitals
 * and mana by the config's meter display, {@link OrbHud} (orbs) or {@link BarHud} (bars).
 */
public final class CompactHud {
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath("haven_create_ui_ux", "compact_hud");
    public static final KeyMapping NUMBERS = new KeyMapping("key.haven_create_ui_ux.hud_numbers",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.haven_create_ui_ux");
    /** Layers the bar replaces: hidden while it is active. */
    private static final Set<ResourceLocation> REPLACED = Set.of(
            VanillaGuiLayers.HOTBAR, VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH, VanillaGuiLayers.AIR_LEVEL, VanillaGuiLayers.EXPERIENCE_BAR,
            VanillaGuiLayers.EXPERIENCE_LEVEL, VanillaGuiLayers.JUMP_METER,
            id("irons_spellbooks", "mana_overlay"), id("ars_nouveau", "mana_hud"), id("create", "remaining_air"),
            id("appleskin", "health_restored"), id("appleskin", "hunger_restored"), id("appleskin", "saturation_level"),
            id("appleskin", "exhaustion_level"), VanillaGuiLayers.EFFECTS, id("irons_spellbooks", "cast_bar"), id("create", "toolbox"),
            id("create", "train_hud"), id("mekanism", "energy_level"), id("immersive_aircraft", "ia_overlay"),
            id("immersive_armors", "ia_overlay"), id("artifacts", "helium_flamingo_charge"), id("artifacts", "artifact_cooldowns"),
            id("farmersdelight", "comfort"), id("farmersdelight", "nourishment"),
            id("creatingspace", "remaining_oxygen"));   // its O2 dial: a gear status now (GearStatus)

    /** The bar's size in game (the config's "action bar size", 1.0 by default: the hotbar at vanilla's size). */
    public static float scale() { return (float) (double) UiConfig.HUD_SCALE.get(); }

    /** Everything the bar draws from (mipmapped, {@link MipTexture}). */
    private static final List<ResourceLocation> TEXTURES = List.of(PARTS, ORBS, plate(true), plate(false));
    static { CompactAtlas.checkLayout(); }
    private static List<String> packIds = List.of();
    private static boolean present;

    private CompactHud() {}

    private static ResourceLocation id(String ns, String path) { return ResourceLocation.fromNamespaceAndPath(ns, path); }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER, CompactHud::render);
        HudLog.once("registered", "layer {} registered above {}; replaces {} layers while active", LAYER, VanillaGuiLayers.HOTBAR, REPLACED.size());
    }
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(NUMBERS);
        HudLog.once("key", "key '{}' registered (default Left Alt; Options > Controls > Create UI Remix)", NUMBERS.getName());
    }

    /** While a screen is open: the hovered buff or debuff's tooltip, over the screen. */
    public static void onScreenRender(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
        if (active()) HudEffects.onScreenRender(event);
    }

    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        if (REPLACED.contains(event.getName()) && active()) {
            event.setCanceled(true);
            HudLog.once("hide:" + event.getName(), "hiding layer {} (the bar draws it)", event.getName());
        }
    }

    /** The bar draws (and hides what it replaces) while its textures are present, outside spectator mode. */
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        if (mc.player.isSpectator()) {
            HudLog.changed("active", "spectator", "inactive: spectator mode (vanilla's spectator HUD shows)");
            return false;
        }
        List<String> ids = List.copyOf(mc.getResourcePackRepository().getSelectedIds());
        if (!ids.equals(packIds)) {
            present = mc.getResourceManager().getResource(PARTS).isPresent();
            packIds = ids;
            HudLog.LOG.info("[compact HUD] resource packs changed ({} selected); {} {}", ids.size(), PARTS, present ? "found" : "MISSING");
        }
        HudLog.changed("active", present, present ? "active: drawing the compact action bar"
                : "inactive: {} not found (is the haven-create-hud resource pack enabled?)", PARTS);
        return present;
    }

    // ---- Drawing, in texels relative to (screen centre, screen bottom) -------------------------------------------

    /** The instrument plate above the bar: web followers (ghost and count), the steampunk armor's clock and compass, the
     *  worn Artifacts on cooldown (their item, shaded by the time left). Only while there is one. */
    private static void instruments(GuiGraphics g, Minecraft mc, LocalPlayer p, float delta) {
        restoreBlend();
        int followers = Followers.count();
        boolean steam = HudExtras.steampunk(p);
        var cooldowns = HudExtras.artifactCooldowns(p, delta);
        String fol = String.valueOf(followers);
        int fw = (int) Math.round(digitsWidth(fol) * 1.5);   // the count at 1.5x
        int w = (followers > 0 ? 9 * K + fw + K : 0) + (steam ? 18 * K : 0) + cooldowns.size() * 9 * K;
        if (w == 0) return;
        w += 3 * K;
        int x0 = -w / 2, y0 = y(MID - 9 - 2.5), cap = 4 * K;
        use(g, PARTS);
        g.blit(PARTS, x0, y0, cap, IPLATE_H, IPLATE_U, OVER_V, cap, IPLATE_H, PARTS_SIZE, PARTS_SIZE);
        g.blit(PARTS, x0 + cap, y0, w - 2 * cap, IPLATE_H, IPLATE_U + cap, OVER_V, IPLATE_W - 2 * cap, IPLATE_H, PARTS_SIZE, PARTS_SIZE);
        g.blit(PARTS, x0 + w - cap, y0, cap, IPLATE_H, IPLATE_U + IPLATE_W - cap, OVER_V, cap, IPLATE_H, PARTS_SIZE, PARTS_SIZE);
        int cur = x0 + 2 * K;
        if (followers > 0) {
            part(g, 2 * K, GHOST_PLATE_V, 8 * K, GHOST_PLATE_H, cur, y0);   // the ghost from the follower plate
            var pose = g.pose();
            pose.pushPose();
            pose.translate(cur + 9 * K, y0 + 4 * K, 0);
            pose.scale(1.5f, 1.5f, 1);
            digits(g, fol, 0, -DIGIT_H / 2, 9, WHITE, WHITE);
            pose.popPose();
            cur += 9 * K + fw + K;
        }
        if (steam && mc.level != null) {
            float day = mc.level.getTimeOfDay(delta);
            dial(g, CLOCK_U, cur, y0, day * Mth.TWO_PI, 0xFFFFF3C8);
            var spawn = mc.level.getSharedSpawnPos();
            double dx = spawn.getX() + 0.5 - p.getX(), dz = spawn.getZ() + 0.5 - p.getZ(), yaw = Math.toRadians(p.getViewYRot(delta));
            double fwd = -Math.sin(yaw) * dx + Math.cos(yaw) * dz, right = -Math.cos(yaw) * dx - Math.sin(yaw) * dz;
            dial(g, COMPASS_U, cur + 9 * K, y0, (float) Math.atan2(right, fwd), 0xFFD84030);
            cur += 18 * K;
        }
        for (Object[] cd : cooldowns) {
            var pose = g.pose();
            use(g, FILL);
            g.fill(cur + 6, y0 + 6, cur + 9 * K - 6, y0 + 9 * K - 6, 0xFF060708);
            pose.pushPose();
            pose.translate(cur + K, y0 + K, 0);
            pose.scale(K * 7f / 16, K * 7f / 16, 1);
            use(g, ITEM);
            g.renderItem((ItemStack) cd[0], 0, 0);
            pose.popPose();
            use(g, FILL);
            g.fill(cur + K, y0 + K, cur + 8 * K, y0 + K + (int) (7 * K * (float) cd[1]), 0x99000000);
            cur += 9 * K;
        }
    }

    /** A dial face from the atlas with its hand at angle a (radians, clockwise from up). */
    private static void dial(GuiGraphics g, int u, int x, int y, float a, int argb) {
        part(g, u, OVER_V, DIAL, DIAL, x, y);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x + DIAL / 2f, y + DIAL / 2f, 0);
        pose.mulPose(Axis.ZP.rotation(a));
        use(g, FILL);
        g.fill(-2, -DIAL / 2 + 10, 3, 0, argb);
        pose.popPose();
    }

    /** The centre plate's top edge, the XP pipe and the level window (the page's CC.plate.mid, CC.xp, CC_LEVEL). */
    static final double MID = 24.4, XP_Y = 25.6, XP_H_U = 3, XP_X0 = -88.5, XP_X1 = 88.5;
    static final double LEVEL_K = 0.85, LEVEL_H = 6.6, ICON_W = 9, ICON_H = 7;   // readable at GUI scale 2-3

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (!active()) return;
        MipTexture.install(TEXTURES);
        beginOrder();
        var pose = g.pose();
        pose.pushPose();
        try {
            // The whole bar, items included, at the configured size around the screen's bottom centre.
            float cx = g.guiWidth() / 2f, bottom = g.guiHeight(), s = scale();
            pose.translate(cx, bottom, 0);
            pose.scale(s, s, 1);
            pose.translate(-cx, -bottom, 0);
            draw(g, delta.getGameTimeDeltaPartialTick(false), s);
        } catch (Throwable t) {
            HudLog.error("render:" + t, "drawing the bar failed (skipping this frame; logged once per kind)", t);
        } finally {
            pose.popPose();
        }
    }

    private static void draw(GuiGraphics g, float delta, float scale) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        int cx = g.guiWidth() / 2, bottom = g.guiHeight();
        HudFrame f = new HudFrame(mc, p, delta, NUMBERS.isDown());
        HudLog.changed("config", f.orbs + "/" + f.primaryArs + "/" + scale + "/" + f.side, "config: meter display {}, primary mana {}, size {}, side bars size {} (wing scale {})",
                f.orbs ? "orbs" : "bars", f.primaryArs ? "Ars" : "Iron's", scale, UiConfig.SIDE_SCALE.get(), f.side);
        HudLog.changed("mode", (f.hurtable ? "survival" : "creative") + (f.lefty ? ", left-handed" : "") + (f.hardcore ? ", hardcore" : ""),
                "mode: {}{}{}", f.hurtable ? "survival" : "creative (vitals hidden)", f.lefty ? ", left-handed (offhand on the right)" : "", f.hardcore ? ", hardcore (skull)" : "");
        // Mod extras, each read as the mod's own HUD reads it (HudExtras); null / -1 when absent or not showing.
        Float cast = HudExtras.ironsCast(), engine = HudExtras.aircraftEngine(p), train = HudExtras.trainSpeed();
        Float attack = HudExtras.attackCharge(mc, p, delta);
        boolean[] toolbox = HudExtras.toolboxSlots(p);
        int schematic = HudExtras.schematicSlot();
        HudLog.changed("extras", (cast != null) + "/" + (!f.gear.isEmpty()) + "/" + (f.hull != null) + "/" + (engine != null) + "/" + (train != null) + "/" + (f.flamingo != null) + "/" + (f.apple != null) + "/" + schematic,
                "extras: cast {}, gear statuses {}, aircraft {}, engine {}, train {}, Helium Flamingo {}, AppleSkin food {}, schematic slot {}",
                cast != null, f.gear.size(), f.hull != null, engine != null, train != null, f.flamingo != null, f.apple != null, schematic);
        HudLog.changed("ext", f.showArmor + "/" + f.showBreath + "/" + (f.tank != null), "armor {}, breath {}{}", f.showArmor ? "shown" : "hidden",
                f.showBreath ? "shown" : "hidden", f.tank != null ? " (Create backtank: " + (int) f.tank[0] + "s of " + (int) f.tank[1] + "s)" : "");
        HudLog.changed("mana", (f.irons != null) + "/" + (f.ars != null), "mana mods: Iron's Spellbooks {}, Ars Nouveau {}",
                f.irons != null ? "found (" + (int) f.irons[0] + "/" + (int) f.irons[1] + ")" : "not found", f.ars != null ? "found (" + (int) f.ars[0] + "/" + (int) f.ars[1] + ")" : "not found");

        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, bottom, 0);
        pose.scale(1f / K, 1f / K, 1);
        checkBlend("frame start"); restoreBlend();
        // The plate in three parts: the centre, and each wing at the side bars' size (attached at WING_ANCHOR).
        ResourceLocation plate = plate(f.orbs);
        int cut = x(-WING_ANCHOR) - PLATE_X, mid = PLATE_W - 2 * cut;   // each wing part: the plate's edge to the anchor
        use(g, plate);
        g.blit(plate, x(-WING_ANCHOR), PLATE_Y, cut, 0, mid, PLATE_H, PLATE_W, PLATE_H);
        side(g, -1, f.side, () -> g.blit(plate, PLATE_X, PLATE_Y, 0, 0, cut, PLATE_H, PLATE_W, PLATE_H));
        side(g, 1, f.side, () -> g.blit(plate, x(WING_ANCHOR), PLATE_Y, cut + mid, 0, cut, PLATE_H, PLATE_W, PLATE_H));
        HudJoint.draw(g, f.orbs, f.side);   // the fillets joining the centre to each wing at its size
        double offSide = f.lefty ? 1 : -1;   // the offhand's wing; the dash takes the other

        // Hotbar sockets, the offhand socket (its shield ghost while empty), the selection frame.
        for (int i = 0; i < 9; i++) part(g, SOCKET_U, 0, 18 * K, 18 * K, x(-89 + 20 * i), y(30));
        double oy = offhandY(f);
        side(g, offSide, f.side, () -> {
            part(g, SOCKET_U, 0, 18 * K, 18 * K, x(f.ends.offhand), y(oy));
            if (p.getOffhandItem().isEmpty()) part(g, GHOST_U, 0, 18 * K, 18 * K, x(f.ends.offhand), y(oy));
        });
        // Create's slot marks (toolbox chips, the schematic tool's frame), the attack cooldown shade, then the selection
        // (a toolbox slot's in red, its chips lit over the ring).
        int sel = p.getInventory().selected;
        for (int i = 0; i < 9; i++) if (toolbox[i] && i != sel) part(g, CHIPS_U, OVER_V, 18 * K, 18 * K, x(-89 + 20 * i), y(30));
        if (schematic >= 0) part(g, SCHEMATIC_U, OVER_V, 18 * K, 18 * K, x(-89 + 20 * schematic), y(30));
        if (attack != null) {
            int sx = x(-88 + 20 * sel), sy = y(31), h = (int) (16 * K * (1 - attack));
            use(g, FILL);
            g.fill(sx, sy, sx + 16 * K, sy + h, 0x8C000000);
            g.fill(sx, sy + h - 3, sx + 16 * K, sy + h, 0xCCFFBE5A);
        }
        if (toolbox[sel]) part(g, TOOLBOX_SEL_U, OVER_V, 24 * K, 23 * K, x(-92 + 20 * sel), y(27));
        else part(g, SELECTION_U, 0, 24 * K, 23 * K, x(-92 + 20 * sel), y(27));

        xpPipe(g, p, cast, train, engine);
        if (f.orbs) OrbHud.draw(g, f); else BarHud.draw(g, f);
        side(g, -offSide, f.side, () -> dash(g, f));
        instruments(g, mc, p, delta);
        double wingTop = f.orbs ? 28 : BarHud.TOP;   // the plate's top over the offhand
        if (f.hardcore) side(g, offSide, f.side, () -> part(g, SKULL_U, 0, SKULL_W, SKULL_H, x(f.ends.offhand + 9 - 4.5), y(wingTop - 9 - 2.5)));   // a skull above the offhand
        // The effects rail: above the orbs' domes; bar mode: above the tall wings (and the skull); following the side size.
        double railTop = f.orbs ? OrbHud.OY - OrbHud.HOUSING * OrbHud.S : BarHud.TOP - (f.hardcore ? 9 + 2.5 + 1 : 0);
        HudEffects.draw(g, p, f.gear, cx, bottom, scale, sideX(f.ends.x0, f.side) + 1, sideY(railTop, f.side) - 2);
        pose.popPose();

        // Items, at vanilla's exact spots (mods that mark hotbar slots line up); the offhand's in its socket.
        for (int i = 0; i < 9; i++) slotItem(g, mc, p, p.getInventory().getItem(i), cx - 88 + 20 * i, bottom - 19, i + 1);
        pose.pushPose();
        // In its socket (bars: centred on the meter block), at the side bars' size.
        if (f.side != 1) {
            double a = offSide * WING_ANCHOR;
            pose.translate(cx + a, bottom, 0);
            pose.scale((float) f.side, (float) f.side, 1);
            pose.translate(-(cx + a), -bottom, 0);
        }
        double ox = f.ends.offhand + 1, oyItem = offhandY(f) + 1 - 50;
        pose.translate(ox - Math.floor(ox), oyItem - Math.floor(oyItem), 0);
        slotItem(g, mc, p, p.getOffhandItem(), cx + (int) Math.floor(ox), bottom + (int) Math.floor(oyItem), 10);
        pose.popPose();
        // The bar is about 31 px tall: later HUD layers (mod texts, Create's prompts) stack above it.
        mc.gui.leftHeight = Math.max(mc.gui.leftHeight, (int) Math.ceil(33 * scale));
        mc.gui.rightHeight = Math.max(mc.gui.rightHeight, (int) Math.ceil(33 * scale));
    }

    /** The offhand socket's top (units): orbs: level with the hotbar; bars: centred on the wing's meter block. */
    private static double offhandY(HudFrame f) { return f.orbs ? 30 : BarHud.MID - 9; }

    /** The shared pipe: XP, or a takeover (first wins: a charging spell (Iron's), a mount's dash cooldown or jump, a
     *  train's speed, an engine) with its icon in a larger window. The level in a small window centred on the pipe. */
    private static void xpPipe(GuiGraphics g, LocalPlayer p, Float cast, Float train, Float engine) {
        restoreBlend();   // the level window and digits have transparent cells
        var jumpable = p.jumpableVehicle();
        HudLog.changed("mount", p.getVehicle() == null ? "none" : p.getVehicle().getType().toString(), "vehicle: {}",
                p.getVehicle() == null ? "none" : p.getVehicle().getType().toShortString() + (jumpable != null ? " (jump bar takes the XP pipe while charging)" : ""));
        String mountIcon = p.getVehicle() instanceof Camel ? "camel" : "horse", take = null, takeIcon = null;
        double takeFrac = 0;
        if (cast != null) { take = "cast"; takeIcon = "flask"; takeFrac = cast; }
        else if (jumpable != null && jumpable.getJumpCooldown() > 0) { take = "cooldown"; takeIcon = mountIcon; takeFrac = 1; }
        else if (jumpable != null && p.getJumpRidingScale() > 0) { take = "jump"; takeIcon = mountIcon; takeFrac = p.getJumpRidingScale(); }
        else if (train != null) { take = "train"; takeIcon = "train"; takeFrac = train; }
        else if (engine != null) { take = "engine"; takeIcon = "engine"; takeFrac = engine; }
        double frac = clamp(take != null ? takeFrac : p.experienceProgress);
        part(g, 0, XP_CASE_V, XP_W, XP_H, x(XP_X0), y(XP_Y));
        int inner = XP_W - 2 * INSET, fill = (int) Math.round(inner * frac);
        part(g, INSET, xpLiquidV(take != null ? take : "xp") + INSET, fill, XP_H - 2 * INSET, x(XP_X0) + INSET, y(XP_Y) + INSET);
        if ("cast".equals(take)) BarHud.stars(g, XP_X0, XP_Y, XP_X1 - XP_X0, XP_H_U, (XP_X1 - XP_X0 - 1) * frac);
        double mid = XP_Y + XP_H_U / 2;
        if (takeIcon != null) {
            part(g, ICON_WIN_U, 0, (int) (ICON_W * K), (int) (ICON_H * K), x(-ICON_W / 2), y(mid - ICON_H / 2));
            part(g, winIconU(takeIcon), 0, WIN_ICON, WIN_ICON, x(-3.5), y(mid - 3.5));
        } else {
            String lvl = String.valueOf(Math.max(0, p.experienceLevel));
            double tw = textWidth(lvl, LEVEL_K) / K, ww = Math.max(tw + 3.6, 8.5);
            int wx = x(-ww / 2), wy = y(mid - LEVEL_H / 2), w = x(ww / 2) - wx, h = (int) Math.round(LEVEL_H * K);
            // The window in three slices (caps and a stretched middle), as wide as the number needs.
            use(g, PARTS);
            g.blit(PARTS, wx, wy, WINDOW_CAP, h, WINDOW_U, 0, WINDOW_CAP, h, PARTS_SIZE, PARTS_SIZE);
            g.blit(PARTS, wx + WINDOW_CAP, wy, w - 2 * WINDOW_CAP, h, WINDOW_U + WINDOW_CAP, 0, WINDOW_W - 2 * WINDOW_CAP, h, PARTS_SIZE, PARTS_SIZE);
            g.blit(PARTS, wx + w - WINDOW_CAP, wy, WINDOW_CAP, h, WINDOW_U + WINDOW_W - WINDOW_CAP, 0, WINDOW_CAP, h, PARTS_SIZE, PARTS_SIZE);
            text(g, lvl, -tw / 2 * K, (mid - 2.5 * LEVEL_K - 50) * K, LEVEL_K, levelV(p.experienceLevel > 0 ? LEVEL_GREEN : LEVEL_DIM), levelV(LEVEL_BLACK));
        }
    }

    /** The dodge roll dial (stone rim, the chevrons in the tier colour, a segment per charge lit as charges are ready),
     *  centred in its space at the wing's end; a pressed-in cog without the mod. */
    private static void dash(GuiGraphics g, HudFrame f) {
        restoreBlend();
        float[] roll = f.roll;
        HudLog.changed("roll", roll == null ? "none" : String.valueOf((int) roll[1]), "dodge roll: {}", roll == null ? "not found (a pressed-in cog takes its place)" : "found, " + (int) roll[1] + " charges");
        // Orbs: in its own housing (a parts piece over the plate); bars: centred on the meter block. Drawn DIAL_S x.
        double dy = f.orbs ? HudFrame.DIAL_Y : BarHud.MID, cx = f.ends.dash * K, cy = (dy - 50) * K;
        if (f.orbs) part(g, DOME_U, DOME_V, DOME_W, DOME_H, x(f.ends.dash - DOME_HALF), y(DOME_TOP));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0);
        pose.scale((float) HudFrame.DIAL_S, (float) HudFrame.DIAL_S, 1);
        pose.translate(-cx, -cy, 0);
        try { dial(g, roll, cx, cy); } finally { pose.popPose(); }
    }
    private static void dial(GuiGraphics g, float[] roll, double cx, double cy) {
        if (roll == null || roll[1] <= 0) {
            part(g, DASH_COG_U, DASH_COG_V, DASH_COG, DASH_COG, (int) Math.round(cx) - DASH_COG / 2, (int) Math.round(cy) - DASH_COG / 2);
            return;
        }
        float ready = roll[0];
        int n = (int) roll[1];
        String tier = ready - 1 >= 0.75 * (n - 1) ? "green" : ready - 1 >= 0.5 * (n - 1) ? "yellow" : ready - 1 >= 0.25 * (n - 1) ? "orange" : ready >= 1 ? "red" : "grey";
        orbCell(g, "dash_" + tier, cx, cy);
        double[] c = cell("ring_" + tier + "_" + Math.min(n, DASH_MAX_N));
        double gap = 0.12, top = -Math.PI / 2, step = 2 * Math.PI / n;
        for (int k = 0; k < n; k++) {
            double lit = ready - k;
            if (lit <= 0) break;
            double a0 = top + k * step + gap, a1 = top + (k + 1) * step - gap;
            sector(g, ORBS, ORBS_W, ORBS_H, c[0], c[1], cx, cy, CELL_HALF, top + k * step, lit >= 1 ? top + (k + 1) * step : a0 + (a1 - a0) * lit);
        }
    }

    private static void slotItem(GuiGraphics g, Minecraft mc, LocalPlayer p, ItemStack stack, int x, int y, int seed) {
        if (stack.isEmpty()) return;
        use(g, ITEM);
        g.renderItem(p, stack, x, y, seed);
        g.renderItemDecorations(mc.font, stack, x, y);
    }
}
