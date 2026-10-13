package dev.frosthaven.createdarkinventory.hud;

import static dev.frosthaven.createdarkinventory.hud.CompactAtlas.*;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import java.util.List;
import java.util.Set;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
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
 */
public final class CompactHud {
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath("createdarkinventory", "compact_hud");
    public static final KeyMapping NUMBERS = new KeyMapping("key.createdarkinventory.hud_numbers",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.createdarkinventory");
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
            id("farmersdelight", "comfort"), id("farmersdelight", "nourishment"));
    private static final ResourceLocation COMFORT = id("farmersdelight", "comfort");
    private static final ResourceLocation NOURISHMENT = id("farmersdelight", "nourishment");

    /** The bar's size in game: 1.1x its designed (vanilla-matched) size reads better on large screens. */
    public static final float SCALE = 1.1f;

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
                : "inactive: {} not found (is the Create-HUD resource pack enabled?)", PARTS);
        return present;
    }

    // ---- Drawing, in texels relative to (screen centre, screen bottom) -------------------------------------------

    private static int x(double l) { return (int) Math.round(l * K); }
    private static int y(double l) { return (int) Math.round((l - 50) * K); }

    static void part(GuiGraphics g, int u, int v, int w, int h, int x, int y) {
        g.blit(PARTS, x, y, u, v, w, h, PARTS_SIZE, PARTS_SIZE);
    }

    /** A meter: casing, then the liquid's inner strip cropped to frac. ext: the slim size. */
    private static void meter(GuiGraphics g, int x, int y, boolean ext, String liquid, double frac) {
        int w = ext ? EXT_W : WING_W, h = ext ? EXT_H : WING_H, u = ext ? EXT_CASE_U : 0;
        part(g, u, WING_CASE_V, w, h, x, y);
        span(g, x, y, ext, liquid, 0, frac);
    }
    /** The liquid's inner strip from frac a to b (absorption, saturation, partial overlays). */
    private static void span(GuiGraphics g, int x, int y, boolean ext, String liquid, double a, double b) {
        int w = (ext ? EXT_W : WING_W) - 2 * INSET, h = (ext ? EXT_H : WING_H) - 2 * INSET, u = (ext ? EXT_CASE_U : 0) + INSET;
        int v = (ext ? extLiquidV(liquid) : wingLiquidV(liquid)) + INSET;
        int from = (int) Math.round(w * clamp(a)), to = (int) Math.round(w * clamp(b));
        if (to > from) part(g, u + from, v, to - from, h, x + INSET + from, y + INSET);
    }
    private static double clamp(double f) { return Math.max(0, Math.min(1, f)); }

    /** Digits in the small set: colour row per character (value bright, the rest dim), a shadow under them. */
    static void digits(GuiGraphics g, String s, int x, int y, int brightChars, int bright, int rest) {
        for (int pass = 0; pass < 2; pass++) {
            int cx = x;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                int row = pass == 0 ? BLACK : i < brightChars ? bright : rest;
                // The shadow a full font pixel (5 texels) down-right: half a pixel let it eat the white strokes in game.
                part(g, digitU(c), digitV(row), digitW(c), DIGIT_H, cx + (pass == 0 ? 5 : 0), y + (pass == 0 ? 5 : 0));
                cx += digitAdv(c);
            }
        }
    }
    static int digitsWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += digitAdv(s.charAt(i));
        return w - 5;
    }

    /** Value / max centred on a meter box, the liquid dimmed behind. */
    private static void numbers(GuiGraphics g, int x, int y, boolean ext, String value, String max) {
        int w = ext ? EXT_W : WING_W, h = ext ? EXT_H : WING_H;
        if (!ext) g.fill(x + INSET, y + INSET, x + w - INSET, y + h - INSET, 0x99040506);   // not on slim pipes: the shadow does
        String t = max == null ? value : value + "/" + max;
        digits(g, t, x + w / 2 - digitsWidth(t) / 2, y + h / 2 - DIGIT_H / 2, value.length(), WHITE, DIM);
    }

    private static void icon(GuiGraphics g, int u, int v, int cx, int cy) { part(g, u, v, ICON, ICON, cx - ICON / 2, cy - ICON / 2); }

    /** A pale fill whose alpha pulses (AppleSkin's preview flash), as ARGB. */
    private static int pulse(int rgb) {
        double a = 0.3 + 0.25 * Math.sin(System.currentTimeMillis() / 160.0);
        return ((int) (a * 255) << 24) | rgb;
    }

    /** A soft light band sweeping along a wing meter every periodMs (regeneration; Farmer's Delight Comfort). */
    private static void sweep(GuiGraphics g, int x, int y, long periodMs, int rgb) {
        int iw = WING_W - 2 * INSET, band = 8 * K;
        double pos = (System.currentTimeMillis() % periodMs) / (double) periodMs * (iw + band) - band / 2.0;
        for (int i = 0; i < 8; i++) {
            int x0 = (int) (pos - band / 2.0 + i * band / 8.0), x1 = x0 + band / 8;
            int a = (int) (153 * (1 - Math.abs(i - 3.5) / 4));
            int c0 = Math.max(0, x0), c1 = Math.min(iw, x1);
            if (c1 > c0) g.fill(x + INSET + c0, y + INSET, x + INSET + c1, y + WING_H - INSET, (a << 24) | rgb);
        }
    }

    /** A thin outline around a meter (hardcore: crimson). */
    private static void frame(GuiGraphics g, int x, int y, int w, int h, int argb) {
        int t = 4;
        g.fill(x - t, y - t, x + w + t, y, argb); g.fill(x - t, y + h, x + w + t, y + h + t, argb);
        g.fill(x - t, y, x, y + h, argb); g.fill(x + w, y, x + w + t, y + h, argb);
    }

    /** The instrument plate above the bar: web followers (ghost and count), the steampunk armor's clock and compass, the
     *  worn Artifacts on cooldown (their item, shaded by the time left). Only while there is one. */
    private static void instruments(GuiGraphics g, Minecraft mc, LocalPlayer p, float delta) {
        int followers = Followers.count();
        boolean steam = HudExtras.steampunk(p);
        var cooldowns = HudExtras.artifactCooldowns(p, delta);
        String fol = String.valueOf(followers);
        int w = (followers > 0 ? 9 * K + digitsWidth(fol) + K : 0) + (steam ? 18 * K : 0) + cooldowns.size() * 9 * K;
        if (w == 0) return;
        w += 3 * K;
        int x0 = -w / 2, y0 = y(10), cap = 4 * K;
        g.blit(PARTS, x0, y0, cap, IPLATE_H, IPLATE_U, OVER_V, cap, IPLATE_H, PARTS_SIZE, PARTS_SIZE);
        g.blit(PARTS, x0 + cap, y0, w - 2 * cap, IPLATE_H, IPLATE_U + cap, OVER_V, IPLATE_W - 2 * cap, IPLATE_H, PARTS_SIZE, PARTS_SIZE);
        g.blit(PARTS, x0 + w - cap, y0, cap, IPLATE_H, IPLATE_U + IPLATE_W - cap, OVER_V, cap, IPLATE_H, PARTS_SIZE, PARTS_SIZE);
        int cur = x0 + 2 * K;
        if (followers > 0) {
            part(g, 2 * K, GHOST_PLATE_V, 8 * K, GHOST_PLATE_H, cur, y0);   // the ghost from the follower plate
            digits(g, fol, cur + 9 * K, y0 + 4 * K - DIGIT_H / 2, 9, WHITE, WHITE);
            cur += 9 * K + digitsWidth(fol) + K;
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
            g.fill(cur + 6, y0 + 6, cur + 9 * K - 6, y0 + 9 * K - 6, 0xFF060708);
            pose.pushPose();
            pose.translate(cur + K, y0 + K, 0);
            pose.scale(K * 7f / 16, K * 7f / 16, 1);
            g.renderItem((ItemStack) cd[0], 0, 0);
            pose.popPose();
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
        g.fill(-2, -DIAL / 2 + 10, 3, 0, argb);
        pose.popPose();
    }

    /** The pressed-in gear row, centred on (x, y). */
    private static void gears(GuiGraphics g, int x, int y) { part(g, GEARS_U, GEARS_V, GEARS_W, GEARS_H, x - GEARS_W / 2, y - GEARS_H / 2); }

    private static String fmt(double v) { return String.valueOf((int) Math.round(v)); }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (!active()) return;
        var pose = g.pose();
        pose.pushPose();
        try {
            // The whole bar, items included, at SCALE around the screen's bottom centre (the hotbar stays centred).
            float cx = g.guiWidth() / 2f, bottom = g.guiHeight();
            pose.translate(cx, bottom, 0);
            pose.scale(SCALE, SCALE, 1);
            pose.translate(-cx, -bottom, 0);
            draw(g, delta.getGameTimeDeltaPartialTick(false));
        } catch (Throwable t) {
            HudLog.error("render:" + t, "drawing the bar failed (skipping this frame; logged once per kind)", t);
        } finally {
            pose.popPose();
        }
    }

    private static void draw(GuiGraphics g, float delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        int cx = g.guiWidth() / 2, bottom = g.guiHeight();
        boolean hurtable = mc.gameMode != null && mc.gameMode.canHurtPlayer();
        boolean underwater = p.isEyeInFluid(FluidTags.WATER);
        float[] tank = HudStats.backtank(p, underwater);
        int air = p.getAirSupply(), maxAir = p.getMaxAirSupply();
        boolean showArmor = hurtable && p.getArmorValue() > 0;
        boolean showBreath = hurtable && (underwater || air < maxAir || tank != null);
        boolean lefty = p.getMainArm() == HumanoidArm.LEFT;
        boolean nums = NUMBERS.isDown() || mc.screen != null;
        HudLog.changed("mode", (hurtable ? "survival" : "creative") + (lefty ? ", left-handed" : ""), "mode: {}{}", hurtable ? "survival" : "creative (vitals hidden)", lefty ? ", left-handed (offhand on the right)" : "");
        // Mod extras, each read as the mod's own HUD reads it (HudExtras); null / -1 when absent or not showing.
        Float cast = HudExtras.ironsCast(), meka = hurtable ? HudExtras.mekaSuit(p) : null, hull = HudExtras.aircraftHealth(p);
        Float engine = HudExtras.aircraftEngine(p), train = HudExtras.trainSpeed(), flamingo = hurtable ? HudExtras.flamingo(p) : null;
        Float attack = HudExtras.attackCharge(mc, p);
        float[] apple = hurtable ? HudExtras.appleSkin(mc, p) : null;
        boolean[] toolbox = HudExtras.toolboxSlots(p);
        int schematic = HudExtras.schematicSlot();
        boolean leftExt = showArmor || meka != null;
        String rightExt = showBreath ? "breath" : flamingo != null ? "flamingo" : hull != null ? "hull" : null;
        HudLog.changed("extras", (cast != null) + "/" + (meka != null) + "/" + (hull != null) + "/" + (engine != null) + "/" + (train != null) + "/" + (flamingo != null) + "/" + (apple != null) + "/" + schematic,
                "extras: cast {}, MekaSuit {}, aircraft {}, engine {}, train {}, Helium Flamingo {}, AppleSkin food {}, schematic slot {}",
                cast != null, meka != null, hull != null, engine != null, train != null, flamingo != null, apple != null, schematic);
        HudLog.changed("ext", showArmor + "/" + showBreath + "/" + (tank != null), "extensions: armor {}, breath {}{}", showArmor ? "shown" : "hidden", showBreath ? "shown" : "hidden", tank != null ? " (Create backtank: " + (int) tank[0] + "s of " + (int) tank[1] + "s)" : "");

        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, bottom, 0);
        pose.scale(1f / K, 1f / K, 1);
        RenderSystem.enableBlend();
        g.blit(plate(leftExt, rightExt != null), PLATE_X, PLATE_Y, 0, 0, PLATE_W, PLATE_H, PLATE_W, PLATE_H);

        // Hotbar sockets, the offhand socket (its shield ghost while empty), the selection frame.
        for (int i = 0; i < 9; i++) part(g, SOCKET_U, 0, 18 * K, 18 * K, x(-89 + 20 * i), y(30));
        int ox = lefty ? 143 : -161;
        part(g, SOCKET_U, 0, 18 * K, 18 * K, x(ox), y(30));
        if (p.getOffhandItem().isEmpty()) part(g, GHOST_U, 0, 18 * K, 18 * K, x(ox), y(30));
        // Create's slot marks (toolbox chips, the schematic tool's frame), the attack cooldown shade, then the selection
        // (a toolbox slot's in red, its chips lit over the ring).
        int sel = p.getInventory().selected;
        for (int i = 0; i < 9; i++) if (toolbox[i] && i != sel) part(g, CHIPS_U, OVER_V, 18 * K, 18 * K, x(-89 + 20 * i), y(30));
        if (schematic >= 0) part(g, SCHEMATIC_U, OVER_V, 18 * K, 18 * K, x(-89 + 20 * schematic), y(30));
        if (attack != null) {
            int sx = x(-88 + 20 * sel), sy = y(31), h = (int) (16 * K * (1 - attack));
            g.fill(sx, sy, sx + 16 * K, sy + h, 0x8C000000);
            g.fill(sx, sy + h - 3, sx + 16 * K, sy + h, 0xCCFFBE5A);
        }
        if (toolbox[sel]) part(g, TOOLBOX_SEL_U, OVER_V, 24 * K, 23 * K, x(-92 + 20 * sel), y(27));
        else part(g, SELECTION_U, 0, 24 * K, 23 * K, x(-92 + 20 * sel), y(27));

        // The shared pipe: a mount's jump charge or dash cooldown takes it over (its icon in the window), else XP.
        var jumpable = p.jumpableVehicle();
        String take = null, takeIcon = null;
        double takeFrac = 0;
        HudLog.changed("mount", p.getVehicle() == null ? "none" : p.getVehicle().getType().toString(), "vehicle: {}", p.getVehicle() == null ? "none" : p.getVehicle().getType().toShortString() + (jumpable != null ? " (jump bar takes the XP pipe while charging)" : ""));
        // Takeovers, first wins: a charging spell (Iron's), a mount's dash cooldown or jump, a train's speed, an engine.
        String mountIcon = p.getVehicle() instanceof Camel ? "camel" : "horse";
        if (cast != null) { take = "cast"; takeIcon = "flask"; takeFrac = cast; }
        else if (jumpable != null && jumpable.getJumpCooldown() > 0) { take = "cooldown"; takeIcon = mountIcon; takeFrac = 1; }
        else if (jumpable != null && p.getJumpRidingScale() > 0) { take = "jump"; takeIcon = mountIcon; takeFrac = p.getJumpRidingScale(); }
        else if (train != null) { take = "train"; takeIcon = "train"; takeFrac = train; }
        else if (engine != null) { take = "engine"; takeIcon = "engine"; takeFrac = engine; }
        part(g, WINDOW_U, 0, 10 * K, 5 * K, x(-84), y(22.125));
        part(g, 0, XP_CASE_V, XP_W, XP_H, x(-73), y(23));
        int xpInner = XP_W - 2 * INSET, xpFill = (int) Math.round(xpInner * clamp(take != null ? takeFrac : p.experienceProgress));
        if (xpFill > 0) part(g, INSET, xpLiquidV(take != null ? take : "xp") + INSET, xpFill, XP_H - 2 * INSET, x(-73) + INSET, y(23) + INSET);
        if (takeIcon != null) icon(g, winIconU(takeIcon), WIN_ICON_V, x(-79), y(24.625));
        else {
            String lvl = String.valueOf(p.experienceLevel);
            digits(g, lvl, x(-79) - digitsWidth(lvl) / 2, y(23.0625), lvl.length(), GREEN, GREEN);
        }

        // Left wing: health (effects, absorption, damage flash, shake at 4 or less), then food or the mount's health.
        int lx = x(-131), rx = x(99), row0 = y(33.5), row1 = y(40.5);
        if (!hurtable) gears(g, x(-119), y(39));   // creative: the vitals wing's pressed-in gears
        if (hurtable) {
            float max = p.getMaxHealth(), hp = p.getHealth(), abs = p.getAbsorptionAmount();
            String eff = p.hasEffect(MobEffects.POISON) ? "poison" : p.hasEffect(MobEffects.WITHER) ? "wither" : p.isFullyFrozen() ? "frozen" : null;
            int shake = hp <= 4 ? (((p.tickCount * 7919) % 3) - 1) * K : 0;
            icon(g, wingIconU(eff == null ? "heart" : "heart_" + eff), WING_ICON_V, x(-136), y(35.5) + shake);
            meter(g, lx, row0 + shake, false, eff == null ? "blood" : eff, hp / max);
            if (abs > 0) span(g, lx, row0 + shake, false, "absorb", hp / max, (hp + abs) / max);
            // AppleSkin: the health the held food would regenerate, pale and pulsing past the health.
            if (apple != null && apple[1] > 0) {
                int iw = WING_W - 2 * INSET, a0 = (int) (iw * clamp(hp / max)), a1 = (int) (iw * clamp((hp + apple[1]) / max));
                g.fill(lx + INSET + a0, row0 + shake + INSET, lx + INSET + a1, row0 + shake + WING_H - INSET, pulse(0xFFC8C8));
            }
            if (p.hasEffect(MobEffects.REGENERATION)) sweep(g, lx, row0 + shake, 1400, 0xFFE6DC);
            else if (p.getFoodData().getSaturationLevel() <= 0 && hp < max && p.getActiveEffects().stream().anyMatch(e -> e.getEffect().is(COMFORT))) sweep(g, lx, row0 + shake, 2600, 0xFFECBE);
            if (mc.level != null && mc.level.getLevelData().isHardcore()) frame(g, lx, row0 + shake, WING_W, WING_H, 0xFF8A1018);
            if (p.hurtTime > 0) {
                int a = (int) (p.hurtTime / 10f * 0.7f * 255) << 24;
                g.fill(lx + INSET, row0 + shake + INSET, lx + WING_W - INSET, row0 + shake + WING_H - INSET, a | 0xFFFFFF);
            }
            if (nums) numbers(g, lx, row0 + shake, false, abs > 0 ? fmt(hp) + "+" + fmt(abs) : fmt(hp), fmt(max));

            if (p.getVehicle() instanceof LivingEntity mount && mount.showVehicleHealth()) {
                icon(g, wingIconU(mount instanceof Camel ? "camel" : "horse"), WING_ICON_V, x(-136), y(42.5));
                meter(g, lx, row1, false, "mount", mount.getHealth() / mount.getMaxHealth());
                if (nums) numbers(g, lx, row1, false, fmt(mount.getHealth()), fmt(mount.getMaxHealth()));
            } else {
                var food = p.getFoodData();
                boolean nourished = p.getActiveEffects().stream().anyMatch(e -> e.getEffect().is(NOURISHMENT));
                icon(g, wingIconU("drumstick"), WING_ICON_V, x(-136), y(42.5));
                meter(g, lx, row1, false, p.hasEffect(MobEffects.HUNGER) ? "hunger" : nourished ? "gilded" : "broth", food.getFoodLevel() / 20.0);
                span(g, lx, row1, false, "saturation", 0, Math.min(food.getSaturationLevel(), food.getFoodLevel()) / 20.0);
                // AppleSkin: what the held food would restore, pale and pulsing past the food.
                if (apple != null && apple[0] > 0) {
                    int iw = WING_W - 2 * INSET, a0 = (int) (iw * food.getFoodLevel() / 20.0), a1 = (int) (iw * Math.min(20, food.getFoodLevel() + apple[0]) / 20.0);
                    g.fill(lx + INSET + a0, row1 + INSET, lx + INSET + a1, row1 + WING_H - INSET, pulse(0xFFE6B4));
                }
                // Exhaustion (a food point lost at 4): its own hairline track just under the food meter.
                float ex = food.getExhaustionLevel();
                {
                    int tx = lx + 12, ty = row1 + WING_H + 6, tw = WING_W - 24;
                    g.fill(tx, ty, tx + tw, ty + 4, 0x8C000000);
                    g.fill(tx, ty, tx + (int) (tw * Math.min(1f, ex / 4f)), ty + 4, 0xD9F0EBE1);
                }
                if (nums) numbers(g, lx, row1, false, String.valueOf(food.getFoodLevel()), "20");
            }
        }

        // Right wing: Iron's mana, Ars mana.
        float[] irons = HudStats.irons(p), ars = HudStats.ars(p);
        HudLog.changed("mana", (irons != null) + "/" + (ars != null), "mana mods: Iron's Spellbooks {}, Ars Nouveau {}",
                irons != null ? "found (" + (int) irons[0] + "/" + (int) irons[1] + ")" : "not found", ars != null ? "found (" + (int) ars[0] + "/" + (int) ars[1] + ")" : "not found");
        // No mana mod: gears pressed into the wing; one: the other row is an empty groove.
        if (irons == null && ars == null) gears(g, x(119), y(39));
        if (irons != null) {
            icon(g, wingIconU("flask"), WING_ICON_V, x(136), y(35.5));
            meter(g, rx, row0, false, "irons", irons[0] / Math.max(1, irons[1]));
            if (nums) numbers(g, rx, row0, false, fmt(irons[0]), fmt(irons[1]));
        } else if (ars != null) part(g, GROOVE_U, GROOVE_V, WING_W, WING_H, rx, row0);
        if (ars != null) {
            icon(g, wingIconU("gem"), WING_ICON_V, x(136), y(42.5));
            meter(g, rx, row1, false, "ars", ars[0] / Math.max(1, ars[1]));
            if (nums) numbers(g, rx, row1, false, fmt(ars[0]), fmt(ars[1]));
        } else if (irons != null) part(g, GROOVE_U, GROOVE_V, WING_W, WING_H, rx, row1);

        // Band extensions: armor (left), breath (right): Create's backtank alone while it holds air, else the bubbles.
        int ey = y(23), eiy = y(24.625);
        if (meka != null) {   // a MekaSuit: two half-height pipes, armor above, suit energy below
            int armor = p.getArmorValue();
            icon(g, extIconU("armor"), EXT_ICON_V, x(-136), ey + EXT_H / 4);
            icon(g, extIconU("bolt"), EXT_ICON_V, x(-136), ey + EXT_H * 3 / 4);
            for (int half = 0; half < 2; half++) {
                var pose2 = g.pose();
                pose2.pushPose();
                pose2.translate(lx, ey + half * (EXT_H / 2 + 1), 0);
                pose2.scale(1, 0.48f, 1);
                meter(g, 0, 0, true, half == 0 ? "armor" : "meka", half == 0 ? armor / 20.0 : meka);
                pose2.popPose();
            }
        } else if (showArmor) {
            int armor = p.getArmorValue();
            icon(g, extIconU("armor"), EXT_ICON_V, x(-136), eiy);
            meter(g, lx, ey, true, "armor", armor / 20.0);
            if (nums) numbers(g, lx, ey, true, String.valueOf(armor), armor > 20 ? "30" : "20");
        }
        if (showBreath) {
            if (tank != null && tank[0] > 0) {
                int sec = (int) Math.ceil(tank[0]);
                String time = sec / 60 + ":" + String.format("%02d", sec % 60);
                icon(g, extIconU("backtank"), EXT_ICON_V, x(136), eiy);
                meter(g, rx, ey, true, "tank", tank[0] / Math.max(1, tank[1]));
                digits(g, time, rx + EXT_W / 2 - digitsWidth(time) / 2, ey + EXT_H / 2 - DIGIT_H / 2, time.length(), WHITE, WHITE);
            } else {
                double f = air / (double) maxAir;
                icon(g, extIconU(f < 0.15 ? "air_burst" : "air"), EXT_ICON_V, x(136), eiy);
                meter(g, rx, ey, true, "air", f);
                if (nums) numbers(g, rx, ey, true, String.valueOf((int) Math.ceil(f * 10)), "10");
            }
        } else if (flamingo != null) {   // Artifacts' Helium Flamingo: its swim-in-air charge
            icon(g, extIconU("flamingo"), EXT_ICON_V, x(136), eiy);
            meter(g, rx, ey, true, "flamingo", flamingo);
            if (nums) numbers(g, rx, ey, true, String.valueOf(Math.round(flamingo * 10)), "10");
        } else if (hull != null) {   // Immersive Aircraft: the vehicle's health
            icon(g, extIconU("plane"), EXT_ICON_V, x(136), eiy);
            meter(g, rx, ey, true, "hull", hull);
            if (nums) numbers(g, rx, ey, true, String.valueOf(Math.round(hull * 20)), "20");
        }

        // The dodge roll: our dash icon by readiness (the mod's tiers), its count after it.
        float[] roll = HudStats.roll(p);
        HudLog.changed("roll", roll == null ? "none" : String.valueOf((int) roll[1]), "dodge roll: {}", roll == null ? "not found (a pressed-in cog takes its place)" : "found, " + (int) roll[1] + " charges");
        int tier = -1;
        double rollX = lefty ? -164.5 : 139.5;
        if (roll != null && roll[1] > 0) {
            float ready = roll[0], n = roll[1];
            tier = ready - 1 >= (n - 1) * 0.75f ? 0 : ready - 1 >= (n - 1) * 0.5f ? 1 : ready - 1 >= (n - 1) * 0.25f ? 2 : ready >= 1 ? 3 : 4;
            part(g, dashU(tier), 0, 18 * K, 18 * K, x(rollX), y(30));
        } else {   // no dodge roll: a cog pressed into the plate in the dash's place
            part(g, DASH_COG_U, DASH_COG_V, DASH_COG, DASH_COG, x(lefty ? -152 : 152) - DASH_COG / 2, y(39) - DASH_COG / 2);
        }

        instruments(g, mc, p, delta);
        HudEffects.draw(g, p, cx, bottom, SCALE);
        pose.popPose();

        // Items, at vanilla's exact spots (mods that mark hotbar slots line up), then the roll count in the game font.
        for (int i = 0; i < 9; i++) slotItem(g, mc, p, p.getInventory().getItem(i), cx - 88 + 20 * i, bottom - 19, i + 1);
        slotItem(g, mc, p, p.getOffhandItem(), cx + (lefty ? 144 : -160), bottom - 19, 10);
        if (tier >= 0) {
            String t = String.valueOf((int) roll[0]);
            int color = new int[] {0x74CD00, 0xE8B500, 0xE84D00, 0xD90003, 0x353535}[tier];
            pose.pushPose();
            pose.translate(cx + rollX + 17, bottom - 14.5, 0);
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) g.drawString(mc.font, t, d[0], d[1], 0xFF000000, false);
            g.drawString(mc.font, t, 0, 0, 0xFF000000 | color, false);
            pose.popPose();
        }
        // The bar is about 31 px tall: later HUD layers (mod texts, Create's prompts) stack above it.
        mc.gui.leftHeight = Math.max(mc.gui.leftHeight, (int) Math.ceil(33 * SCALE));
        mc.gui.rightHeight = Math.max(mc.gui.rightHeight, (int) Math.ceil(33 * SCALE));
    }

    private static void slotItem(GuiGraphics g, Minecraft mc, LocalPlayer p, ItemStack stack, int x, int y, int seed) {
        if (stack.isEmpty()) return;
        g.renderItem(p, stack, x, y, seed);
        g.renderItemDecorations(mc.font, stack, x, y);
    }
}
