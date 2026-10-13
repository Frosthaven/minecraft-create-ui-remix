package dev.frosthaven.havencreateuiux.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Where things are in the compact HUD's textures (createhud:textures/gui/compact/*, exported by the redesign page's
 * hud-compact-export.js). Everything is in texels: the textures are 8x, and the HUD draws at 1/8 GUI px.
 * KEEP IN STEP with hud-compact-export.js.
 */
final class CompactAtlas {
    static final int K = 8;   // texels per GUI pixel
    static final ResourceLocation PARTS = tex("parts"), ORBS = tex("orbs");
    static final int PARTS_SIZE = 256 * K, ORBS_W = 384 * K, ORBS_H = 320 * K;
    static final int PLATE_W = 424 * K, PLATE_H = 66 * K, PLATE_X = -212 * K, PLATE_Y = (-15 - 50) * K;

    static final List<String> WIN_ICONS = List.of("flask", "horse", "camel", "train", "engine");
    static final List<String> WING_LIQUIDS = List.of("blood", "poison", "wither", "frozen", "absorb", "broth", "hunger", "saturation",
            "mount", "irons", "ars", "armor", "air", "tank", "flamingo", "hull");
    static final List<String> XP_LIQUIDS = List.of("xp", "jump", "cooldown", "cast", "train", "engine");
    static final String DIGITS = "0123456789/:+smh";
    static final int WHITE = 0, DIM = 1, GREEN = 2, BLACK = 3, RED = 4;   // 0.625 digit rows (effects)
    static final String GLYPHS = "0123456789/:+";
    static final List<String> TINTS = List.of("blood", "poison", "wither", "frozen", "broth", "hunger", "mount", "irons", "ars",
            "armor", "air", "tank", "flamingo", "hull", "black");
    static final int LEVEL_GREEN = 0, LEVEL_DIM = 1, LEVEL_BLACK = 2;
    static final List<String> DASH_TIERS = List.of("green", "yellow", "orange", "red", "grey");
    static final int DASH_MAX_N = 6;

    // Parts, in texels.
    static final int SOCKET_U = 0, SELECTION_U = 20 * K, GHOST_U = 46 * K;
    static final int SKULL_U = 66 * K, SKULL_W = 9 * K, SKULL_H = 10 * K;
    static final int WINDOW_U = 78 * K, WINDOW_W = 10 * K, WINDOW_CAP = 2 * K, ICON_WIN_U = 90 * K;
    static final int WIN_ICON_U = 100 * K, WIN_ICON = 7 * K;
    static final int MEKA_CASE_U = 136 * K, MEKA_FILL_U = 140 * K, MEKA_W = 19, MEKA_H = 18 * K;   // 2.4 x 18
    // The dash dial's housing (orb mode): 42 x 38 at (182, 92), drawn at (dash - 21, 12.5) of the bar.
    static final int DOME_U = 182 * K, DOME_V = 92 * K, DOME_W = 42 * K, DOME_H = 38 * K;
    static final double DOME_HALF = 21, DOME_TOP = 12.5;
    // Bar mode's exhaustion glow band (on black, screen-blended): 34 x 4 at (182, 132), drawn at (meter x - 1, y + h - 1.5).
    static final int EXH_U = 182 * K, EXH_V = 132 * K, EXH_W = 34 * K, EXH_H = 4 * K;
    static final int WING_CASE_V = 236 * K, XP_CASE_V = 26 * K;   // the wing casing below everything: in rows 20..24 it
    // sat inside the selection cell (20, 0, 24 x 23), which drew a piece of it under the selected item
    static final int WING_W = 32 * K, WING_H = 4 * K, XP_W = 177 * K, XP_H = 3 * K;
    static final int INSET = K / 2;   // the glass inside a casing

    private CompactAtlas() {}

    /** Every piece of the parts atlas {x, y, w, h} (units), checked once for overlaps: a cell that reaches into its
     *  neighbour draws a stray bit of it in game. */
    private static final Object[][] PARTS_PIECES = {
            {"socket", 0, 0, 18, 18}, {"selection", 20, 0, 24, 23}, {"offhand ghost", 46, 0, 18, 18}, {"skull plate", 66, 0, 9, 10},
            {"level window", 78, 0, 10, 5}, {"icon window", 90, 0, 9, 7}, {"window icons", 100, 0, 35, 7}, {"MekaSuit casing", 136, 0, 3, 18},
            {"MekaSuit fill", 140, 0, 3, 18}, {"XP casing", 0, 26, 177, 3}, {"wing liquids", 0, 30, 32, 80}, {"XP liquids", 0, 112, 177, 24},
            {"digits 0.625", 0, 140, 56, 20}, {"glyphs 0.6", 180, 20, 39, 53}, {"level glyphs 0.85", 180, 76, 39, 15},
            {"follower plate", 0, 190, 24, 9}, {"gear row", 30, 190, 32, 12}, {"groove", 80, 190, 32, 4}, {"dash cog", 120, 190, 18, 18},
            {"effect frames", 140, 190, 34, 10}, {"toolbox chips", 0, 210, 18, 18}, {"toolbox chips lit", 20, 210, 18, 18},
            {"toolbox selection", 40, 210, 24, 23}, {"schematic frame", 66, 210, 18, 18}, {"instrument plate", 86, 210, 24, 9},
            {"clock", 112, 210, 9, 9}, {"compass", 122, 210, 9, 9}, {"wing casing", 0, 236, 32, 4},
            {"dial housing", 182, 92, 42, 38}, {"exhaustion band", 182, 132, 34, 4}};

    static void checkLayout() {
        for (int i = 0; i < PARTS_PIECES.length; i++) {
            for (int j = i + 1; j < PARTS_PIECES.length; j++) {
                Object[] a = PARTS_PIECES[i], b = PARTS_PIECES[j];
                int ax = (int) a[1], ay = (int) a[2], bx = (int) b[1], by = (int) b[2];
                if (ax < bx + (int) b[3] && bx < ax + (int) a[3] && ay < by + (int) b[4] && by < ay + (int) a[4]) {
                    HudLog.once("overlap:" + a[0] + "/" + b[0], "atlas pieces overlap in parts.png: {} and {} (one draws a bit of the other)", a[0], b[0]);
                }
            }
        }
    }

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("createhud", "textures/gui/compact/" + name + ".png");
    }

    static ResourceLocation plate(boolean orbs, boolean wide) {
        return tex("plate_" + (orbs ? "orbs" : "bars") + (wide ? "_meka" : ""));
    }

    static int winIconU(String n) { return WIN_ICON_U + WIN_ICON * WIN_ICONS.indexOf(n); }
    static int wingLiquidV(String n) { return (30 + 5 * WING_LIQUIDS.indexOf(n)) * K; }
    static int xpLiquidV(String n) { return (112 + 4 * XP_LIQUIDS.indexOf(n)) * K; }
    static int digitU(char c) { return 28 * DIGITS.indexOf(c); }   // cells 3.5 px apart
    static int digitW(char c) { return (c == 'm' ? 5 : 3) * 5; }   // glyphs 3 font px wide (m: 5), 5 texels a font px
    static int digitAdv(char c) { return digitW(c) + 5; }
    static int digitV(int row) { return (140 + 4 * row) * K; }
    static final int DIGIT_H = 25;
    /** The 0.6 (k = 0.6) and level (0.85) glyph sets: a glyph's cell (3 units apart), its row by tint. */
    static int glyphU(char c) { return (180 + 3 * GLYPHS.indexOf(c)) * K; }
    static int tintV(String tint) { return (int) Math.round((20 + 3.5 * TINTS.indexOf(tint)) * K); }
    static int levelV(int row) { return (int) Math.round((76 + 5 * row) * K); }

    static final int GHOST_PLATE_V = 190 * K, GHOST_PLATE_W = 24 * K, GHOST_PLATE_H = 9 * K;
    // Effect frames (10x10): buff, debuff, ambient.
    static final int EFFECT_BUFF_U = 140 * K, EFFECT_DEBUFF_U = 152 * K, EFFECT_AMBIENT_U = 164 * K, EFFECT_V = 190 * K;
    // Overlays at y 210: toolbox chips (18x18) and lit, the toolbox slot's selection (24x23 cell), the schematic frame,
    // a blank instrument plate (24x9, sliced 4 | middle | 4), clock and compass faces (9x9).
    static final int OVER_V = 210 * K, CHIPS_U = 0, CHIPS_LIT_U = 20 * K, TOOLBOX_SEL_U = 40 * K, SCHEMATIC_U = 66 * K;
    static final int IPLATE_U = 86 * K, IPLATE_W = 24 * K, IPLATE_H = 9 * K, CLOCK_U = 112 * K, COMPASS_U = 122 * K, DIAL = 9 * K;
    // Pressed-in fillers: the gear row (centre (46, 196), drawn as a 32x12 cell), an empty groove, the dash's cog.
    static final int GEARS_U = 30 * K, GEARS_V = 190 * K, GEARS_W = 32 * K, GEARS_H = 12 * K;
    static final int GROOVE_U = 80 * K, GROOVE_V = 190 * K;
    static final int DASH_COG_U = 120 * K, DASH_COG_V = 190 * K, DASH_COG = 18 * K;

    // ---- Orbs atlas: pieces centred in cells (texel centres). ----
    static final List<String> ORB_CELLS = orbCells();
    static final List<String> RING_CELLS = List.of("armor", "armorHalf", "tank", "air", "flamingo", "hull");
    static final int CELL_HALF = 16 * K, RING_HALF = 24 * K;

    private static List<String> orbCells() {
        List<String> l = new ArrayList<>(List.of("blood", "poison", "wither", "frozen", "broth", "hunger", "brothSat", "hungerSat", "mount",
                "absorb", "irons", "ars", "glassL", "glassR", "front", "divider", "bezelL", "bezelR", "cog", "exhaust", "glow_blood", "glow_poison", "glow_wither", "glow_frozen", "glow_broth", "glow_hunger", "glow_mount", "glow_irons",
                "glow_ars"));
        for (String t : DASH_TIERS) l.add("dash_" + t);
        for (String t : DASH_TIERS) for (int n = 1; n <= DASH_MAX_N; n++) l.add("ring_" + t + "_" + n);
        return List.copyOf(l);
    }

    /** A small cell's centre in texels {u, v}. */
    static double[] cell(String name) {
        int n = ORB_CELLS.indexOf(name);
        if (n < 0) throw new IllegalArgumentException("no orb cell " + name);
        return new double[] {(28 + 32 * (n % 11)) * K, (34.5 + 32 * (n / 11)) * K};
    }
    /** A ring cell's centre in texels {u, v}. */
    static double[] ring(String name) {
        int m = RING_CELLS.indexOf(name);
        if (m < 0) throw new IllegalArgumentException("no ring cell " + name);
        return new double[] {(28 + 64 * m) * K, 290.5 * K};
    }
}
