package dev.frosthaven.havencreateuiux;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The client config (config/haven_create_ui_ux-client.toml, and Mods > haven-create-ui-ux > Config in game).
 * Read the values each frame: NeoForge reloads them when the file or the config screen changes them.
 */
public final class UiConfig {
    public enum MeterDisplay { ORBS, BARS }
    public enum PrimaryMana { IRONS, ARS }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<MeterDisplay> METER_DISPLAY;
    public static final ModConfigSpec.EnumValue<PrimaryMana> PRIMARY_MANA;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.BooleanValue MOB_BARS;
    public static final ModConfigSpec.IntValue MOB_BAR_RANGE;
    public static final ModConfigSpec.BooleanValue MOB_BARS_HOSTILE;
    public static final ModConfigSpec.BooleanValue MOB_BARS_NOT_HOSTILE;
    public static final ModConfigSpec.BooleanValue MOB_BARS_PLAYERS;
    public static final ModConfigSpec.BooleanValue MOB_BARS_AGGRESSIVE;

    static {
        var b = new ModConfigSpec.Builder();
        b.push("hud");
        METER_DISPLAY = b.comment("Health, food and mana as two orbs beside the hotbar, or as rows of bars on the wings.")
                .translation("haven_create_ui_ux.config.meter_display").defineEnum("meterDisplay", MeterDisplay.ORBS);
        PRIMARY_MANA = b.comment("With both Iron's Spellbooks and Ars Nouveau installed: the mana pool shown first (the orb's left half, the top bar).")
                .translation("haven_create_ui_ux.config.primary_mana").defineEnum("primaryMana", PrimaryMana.IRONS);
        HUD_SCALE = b.comment("The action bar's size, relative to the GUI scale.")
                .translation("haven_create_ui_ux.config.hud_scale").defineInRange("hudScale", 1.1, 0.5, 2.0);
        b.pop();
        b.push("mobBars");
        MOB_BARS = b.comment("Floating health bars above nearby mobs.")
                .translation("haven_create_ui_ux.config.mob_bars").define("enabled", true);
        MOB_BAR_RANGE = b.comment("How far away (blocks) a mob still gets a bar.")
                .translation("haven_create_ui_ux.config.mob_bar_range").defineInRange("range", 10, 2, 64);
        MOB_BARS_HOSTILE = b.comment("Bars on hostile mobs (red).")
                .translation("haven_create_ui_ux.config.mob_bars_hostile").define("hostile", true);
        MOB_BARS_NOT_HOSTILE = b.comment("Bars on every other mob: animals, villagers, pets, golems, neutral mobs (green).")
                .translation("haven_create_ui_ux.config.mob_bars_not_hostile").define("notHostile", true);
        MOB_BARS_PLAYERS = b.comment("Bars on other players who have not hit a player in the last 30 seconds (blue).")
                .translation("haven_create_ui_ux.config.mob_bars_players").define("friendlyPlayers", true);
        MOB_BARS_AGGRESSIVE = b.comment("Bars on players who hit any player in the last 30 seconds (magenta), whatever the friendly players setting.")
                .translation("haven_create_ui_ux.config.mob_bars_aggressive").define("hostilePlayers", true);
        b.pop();
        SPEC = b.build();
    }

    private UiConfig() {}
}
