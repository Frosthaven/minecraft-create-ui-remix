package dev.frosthaven.createdarkinventory.hud;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Where things are in the compact HUD's textures (createhud:textures/gui/compact/*, exported by the redesign page's
 * hud-compact-export.js). Everything is in texels: the textures are 8x, and CompactHud draws at 1/8 GUI px.
 * KEEP IN STEP with hud-compact-export.js.
 */
final class CompactAtlas {
    static final int K = 8;   // texels per GUI pixel
    static final ResourceLocation PARTS = tex("parts");
    static final int PARTS_SIZE = 256 * K;
    static final int PLATE_W = 332 * K, PLATE_H = 34 * K, PLATE_X = -166 * K, PLATE_Y = (17 - 50) * K;

    static final List<String> WING_ICONS = List.of("heart", "heart_poison", "heart_wither", "heart_frozen", "drumstick", "horse", "camel", "flask", "gem");
    static final List<String> EXT_ICONS = List.of("armor", "air", "air_burst", "backtank", "bolt");
    static final List<String> WIN_ICONS = List.of("flask", "horse", "camel", "train", "engine");
    static final List<String> WING_LIQUIDS = List.of("blood", "poison", "wither", "frozen", "absorb", "broth", "hunger", "gilded", "saturation", "irons", "ars", "mount");
    static final List<String> EXT_LIQUIDS = List.of("armor", "air", "tank", "meka", "flamingo", "hull");
    static final List<String> XP_LIQUIDS = List.of("xp", "jump", "cooldown", "cast", "train", "engine");
    static final String DIGITS = "0123456789/:+";
    static final int WHITE = 0, DIM = 1, GREEN = 2, BLACK = 3;   // digit rows

    // Parts, in texels.
    static final int SOCKET_U = 0, SELECTION_U = 20 * K, GHOST_U = 46 * K, WINDOW_U = 66 * K, DASH_U = 80 * K;
    static final int WING_W = 32 * K, WING_H = 4 * K, EXT_W = 32 * K, EXT_H = 26, XP_W = 157 * K, XP_H = 26;
    static final int WING_CASE_V = 50 * K, EXT_CASE_U = 40 * K, XP_CASE_V = 56 * K;
    static final int INSET = K / 2;   // the glass inside a casing

    private CompactAtlas() {}

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("createhud", "textures/gui/compact/" + name + ".png");
    }

    static ResourceLocation plate(boolean left, boolean right) {
        return tex("plate_" + (left && right ? "both" : left ? "left" : right ? "right" : "none"));
    }

    static int dashU(int tier) { return DASH_U + 18 * K * tier; }
    static int wingIconU(String n) { return 8 * K * WING_ICONS.indexOf(n); }
    static int extIconU(String n) { return 8 * K * EXT_ICONS.indexOf(n); }
    static int winIconU(String n) { return 8 * K * WIN_ICONS.indexOf(n); }
    static final int WING_ICON_V = 24 * K, EXT_ICON_V = 32 * K, WIN_ICON_V = 40 * K, ICON = 8 * K;
    static int wingLiquidV(String n) { return (62 + 5 * WING_LIQUIDS.indexOf(n)) * K; }
    static int extLiquidV(String n) { return (62 + 4 * EXT_LIQUIDS.indexOf(n)) * K; }
    static int xpLiquidV(String n) { return (130 + 4 * XP_LIQUIDS.indexOf(n)) * K; }
    static int digitU(char c) { return 20 * DIGITS.indexOf(c); }
    static int digitV(int row) { return (170 + 4 * row) * K; }
    static final int DIGIT_W = 15, DIGIT_H = 25, DIGIT_ADV = 20;
    static final int GHOST_PLATE_V = 190 * K, GHOST_PLATE_W = 24 * K, GHOST_PLATE_H = 9 * K;
    // Pressed-in fillers: the gear row (centre (46, 196), drawn as a 32x12 cell), an empty groove, the dash's cog.
    static final int GEARS_U = 30 * K, GEARS_V = 190 * K, GEARS_W = 32 * K, GEARS_H = 12 * K;
    static final int GROOVE_U = 80 * K, GROOVE_V = 190 * K;
    static final int DASH_COG_U = 120 * K, DASH_COG_V = 190 * K, DASH_COG = 18 * K;
}
