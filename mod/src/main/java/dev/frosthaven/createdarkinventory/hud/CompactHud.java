package dev.frosthaven.createdarkinventory.hud;

import static dev.frosthaven.createdarkinventory.hud.CompactAtlas.*;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
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
            id("appleskin", "exhaustion_level"));
    private static final ResourceLocation NOURISHMENT = id("farmersdelight", "nourishment");

    private static List<String> packIds = List.of();
    private static boolean present;

    private CompactHud() {}

    private static ResourceLocation id(String ns, String path) { return ResourceLocation.fromNamespaceAndPath(ns, path); }

    public static void register(RegisterGuiLayersEvent event) { event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER, CompactHud::render); }
    public static void registerKeys(RegisterKeyMappingsEvent event) { event.register(NUMBERS); }

    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        if (REPLACED.contains(event.getName()) && active()) event.setCanceled(true);
    }

    /** The bar draws (and hides what it replaces) while its textures are present, outside spectator mode. */
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator()) return false;
        List<String> ids = List.copyOf(mc.getResourcePackRepository().getSelectedIds());
        if (!ids.equals(packIds)) {
            present = mc.getResourceManager().getResource(PARTS).isPresent();
            packIds = ids;
        }
        return present;
    }

    // ---- Drawing, in texels relative to (screen centre, screen bottom) -------------------------------------------

    private static int x(double l) { return (int) Math.round(l * K); }
    private static int y(double l) { return (int) Math.round((l - 50) * K); }

    private static void part(GuiGraphics g, int u, int v, int w, int h, int x, int y) {
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
    private static void digits(GuiGraphics g, String s, int x, int y, int brightChars, int bright, int rest) {
        for (int pass = 0; pass < 2; pass++) {
            int cx = x;
            for (int i = 0; i < s.length(); i++) {
                int row = pass == 0 ? BLACK : i < brightChars ? bright : rest;
                part(g, digitU(s.charAt(i)), digitV(row), DIGIT_W, DIGIT_H, cx + (pass == 0 ? 3 : 0), y + (pass == 0 ? 3 : 0));
                cx += DIGIT_ADV;
            }
        }
    }
    private static int digitsWidth(String s) { return DIGIT_ADV * s.length() - (DIGIT_ADV - DIGIT_W); }

    /** Value / max centred on a meter box, the liquid dimmed behind. */
    private static void numbers(GuiGraphics g, int x, int y, boolean ext, String value, String max) {
        int w = ext ? EXT_W : WING_W, h = ext ? EXT_H : WING_H;
        g.fill(x + INSET, y + INSET, x + w - INSET, y + h - INSET, 0x99040506);
        String t = max == null ? value : value + "/" + max;
        digits(g, t, x + w / 2 - digitsWidth(t) / 2, y + h / 2 - DIGIT_H / 2, value.length(), WHITE, DIM);
    }

    private static void icon(GuiGraphics g, int u, int v, int cx, int cy) { part(g, u, v, ICON, ICON, cx - ICON / 2, cy - ICON / 2); }

    /** The plate between texel x0 and x1: the texture's middle, then its own end caps (the last 3.5 px at each side,
     *  rounded corner and shadow) copied onto the new ends. */
    private static void plateTrimmed(GuiGraphics g, ResourceLocation tex, int x0, int x1) {
        int cap = 28, edge0 = -165 * K, edge1 = 165 * K;   // a cap: 2.5 px corner + the texture's 1 px margin
        int from = x0 + (x0 > edge0 ? 20 : -8), to = x1 - (x1 < edge1 ? 20 : -8);
        g.blit(tex, from, PLATE_Y, from - PLATE_X, 0, to - from, PLATE_H, PLATE_W, PLATE_H);
        if (x0 > edge0) g.blit(tex, x0 - 8, PLATE_Y, 0, 0, cap, PLATE_H, PLATE_W, PLATE_H);
        if (x1 < edge1) g.blit(tex, x1 - 20, PLATE_Y, PLATE_W - cap, 0, cap, PLATE_H, PLATE_W, PLATE_H);
    }

    /** The pressed-in gear row, centred on (x, y). */
    private static void gears(GuiGraphics g, int x, int y) { part(g, GEARS_U, GEARS_V, GEARS_W, GEARS_H, x - GEARS_W / 2, y - GEARS_H / 2); }

    private static String fmt(double v) { return String.valueOf((int) Math.round(v)); }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (!active()) return;
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

        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, bottom, 0);
        pose.scale(1f / K, 1f / K, 1);
        RenderSystem.enableBlend();
        // The bar's width for what the pack has (the page's ccLayout): no mana mod drops the right wing, no dodge roll the
        // dash's spot; with no right wing it grows back out while breath shows. The plate textures are full width:
        // drawn trimmed to [x0, x1], with their rounded end caps copied to the new ends.
        float[] irons = HudStats.irons(p), ars = HudStats.ars(p), roll = HudStats.roll(p);
        boolean mana = irons != null || ars != null, hasRoll = roll != null && roll[1] > 0;
        double rightStart = mana ? 143 : 93;
        boolean rightEnd = lefty || hasRoll, leftEnd = !lefty || hasRoll;
        double x1 = rightEnd ? rightStart + 22 : mana ? 152 : 97;
        if (showBreath) x1 = Math.max(x1, 152);
        double x0 = leftEnd ? -165 : -152;
        double ox = lefty ? rightStart : -161, rollX = lefty ? -164.5 : rightStart - 3.5;
        plateTrimmed(g, plate(showArmor, showBreath), x(x0), x(x1));

        // Hotbar sockets, the offhand socket (its shield ghost while empty), the selection frame.
        for (int i = 0; i < 9; i++) part(g, SOCKET_U, 0, 18 * K, 18 * K, x(-89 + 20 * i), y(30));
        part(g, SOCKET_U, 0, 18 * K, 18 * K, x(ox), y(30));
        if (p.getOffhandItem().isEmpty()) part(g, GHOST_U, 0, 18 * K, 18 * K, x(ox), y(30));
        part(g, SELECTION_U, 0, 24 * K, 23 * K, x(-92 + 20 * p.getInventory().selected), y(27));

        // The shared pipe: a mount's jump charge or dash cooldown takes it over (its icon in the window), else XP.
        var jumpable = p.jumpableVehicle();
        String take = null, takeIcon = null;
        double takeFrac = 0;
        if (jumpable != null) {
            String mount = p.getVehicle() instanceof Camel ? "camel" : "horse";
            if (jumpable.getJumpCooldown() > 0) { take = "cooldown"; takeIcon = mount; takeFrac = 1; }
            else if (p.getJumpRidingScale() > 0) { take = "jump"; takeIcon = mount; takeFrac = p.getJumpRidingScale(); }
        }
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
        int lx = x(-131), rx = x(93), row0 = y(33.5), row1 = y(40.5);
        if (!hurtable) gears(g, x(-116), y(39));   // creative: the vitals wing's pressed-in gears
        if (hurtable) {
            float max = p.getMaxHealth(), hp = p.getHealth(), abs = p.getAbsorptionAmount();
            String eff = p.hasEffect(MobEffects.POISON) ? "poison" : p.hasEffect(MobEffects.WITHER) ? "wither" : p.isFullyFrozen() ? "frozen" : null;
            int shake = hp <= 4 ? (((p.tickCount * 7919) % 3) - 1) * K : 0;
            icon(g, wingIconU(eff == null ? "heart" : "heart_" + eff), WING_ICON_V, x(-136), y(35.5) + shake);
            meter(g, lx, row0 + shake, false, eff == null ? "blood" : eff, hp / max);
            if (abs > 0) span(g, lx, row0 + shake, false, "absorb", hp / max, (hp + abs) / max);
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
                if (nums) numbers(g, lx, row1, false, String.valueOf(food.getFoodLevel()), "20");
            }
        }

        // Right wing: Iron's mana, Ars mana.
        // One mana mod: the other row is an empty groove (none: no right wing at all, above).
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
        if (showArmor) {
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
        }

        // The dodge roll: our dash icon by readiness (the mod's tiers), its count after it.
        int tier = -1;
        if (hasRoll) {
            float ready = roll[0], n = roll[1];
            tier = ready - 1 >= (n - 1) * 0.75f ? 0 : ready - 1 >= (n - 1) * 0.5f ? 1 : ready - 1 >= (n - 1) * 0.25f ? 2 : ready >= 1 ? 3 : 4;
            part(g, dashU(tier), 0, 18 * K, 18 * K, x(rollX), y(30));
        }

        // Web map followers (Haven Link): the ghost plate above the bar.
        int followers = Followers.count();
        if (followers > 0) {
            part(g, 0, GHOST_PLATE_V, GHOST_PLATE_W, GHOST_PLATE_H, x(-12), y(10));
            digits(g, String.valueOf(followers), x(-2), y(12.9), 9, WHITE, WHITE);
        }
        pose.popPose();

        // Items at vanilla's exact spots (mods that mark hotbar slots line up), then the roll count in the game font.
        int bx = cx;   // the hotbar is always centred on the screen (vanilla's spots); a narrower bar is lopsided
        for (int i = 0; i < 9; i++) slotItem(g, mc, p, p.getInventory().getItem(i), bx - 88 + 20 * i, bottom - 19, i + 1);
        slotItem(g, mc, p, p.getOffhandItem(), bx + (int) ox + 1, bottom - 19, 10);
        if (tier >= 0) {
            String t = String.valueOf((int) roll[0]);
            int color = new int[] {0x74CD00, 0xE8B500, 0xE84D00, 0xD90003, 0x353535}[tier];
            pose.pushPose();
            pose.translate(bx + rollX + 17, bottom - 14.5, 0);
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) g.drawString(mc.font, t, d[0], d[1], 0xFF000000, false);
            g.drawString(mc.font, t, 0, 0, 0xFF000000 | color, false);
            pose.popPose();
        }
        // The bar is about 31 px tall: later HUD layers (mod texts, Create's prompts) stack above it.
        mc.gui.leftHeight = Math.max(mc.gui.leftHeight, 33);
        mc.gui.rightHeight = Math.max(mc.gui.rightHeight, 33);
    }

    private static void slotItem(GuiGraphics g, Minecraft mc, LocalPlayer p, ItemStack stack, int x, int y, int seed) {
        if (stack.isEmpty()) return;
        g.renderItem(p, stack, x, y, seed);
        g.renderItemDecorations(mc.font, stack, x, y);
    }
}
