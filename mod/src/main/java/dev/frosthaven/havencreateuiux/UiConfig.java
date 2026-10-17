package dev.frosthaven.havencreateuiux;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;

/**
 * The client config (config/haven_create_ui_ux-client.toml, and Mods > haven-create-ui-ux > Config in game).
 * Read the values each frame: NeoForge reloads them when the file or the config screen changes them.
 */
public final class UiConfig {
    /** How the vitals and mana show: Almost Vanilla (vanilla's places, no plate), or the one iron action bar
     *  with orbs or with rows of bars on its wings (Bars, the default). */
    public enum MeterDisplay implements TranslatableEnum {
        ALMOST_VANILLA, ORBS, BARS;
        @Override public Component getTranslatedName() { return Component.translatable("haven_create_ui_ux.config.meter_display." + name().toLowerCase(java.util.Locale.ROOT)); }
    }
    public enum PrimaryMana { IRONS, ARS }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<MeterDisplay> METER_DISPLAY;
    public static final ModConfigSpec.EnumValue<PrimaryMana> PRIMARY_MANA;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.DoubleValue SIDE_SCALE;

    static {
        var b = new ModConfigSpec.Builder();
        b.push("hud");
        METER_DISPLAY = b.comment("Almost Vanilla: hearts, food and the XP bar at vanilla's places (mana stacked in the XP bar). Orbs or Bars: one iron action bar, health, food and mana as two orbs beside the hotbar or as rows of bars on its wings.")
                .translation("haven_create_ui_ux.config.meter_display").defineEnum("meterDisplay", MeterDisplay.BARS);
        PRIMARY_MANA = b.comment("With both Iron's Spellbooks and Ars Nouveau installed: the mana pool shown first (the orb's left half, the top bar).")
                .translation("haven_create_ui_ux.config.primary_mana").defineEnum("primaryMana", PrimaryMana.IRONS);
        HUD_SCALE = b.comment("The action bar's size, relative to the GUI scale.")
                .translation("haven_create_ui_ux.config.hud_scale").defineInRange("hudScale", 1.0, 0.5, 2.0);
        SIDE_SCALE = b.comment("The side bars' size (orbs or meters, the offhand, the dash), relative to their design; they stay attached to the action bar.")
                .translation("haven_create_ui_ux.config.side_scale").defineInRange("sideScale", 1.0, 0.6, 2.0);
        b.pop();
        SPEC = b.build();
    }

    private UiConfig() {}
}
