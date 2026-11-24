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
    /** Where buffs and debuffs show: the screen's top left (vanilla's rows: buffs first, debuffs below; the default), or the
     *  action bar's area (a rail above its left wing, debuffs above buffs). */
    public enum EffectsAnchor implements TranslatableEnum {
        TOP_LEFT, ACTION_BAR;
        @Override public Component getTranslatedName() { return Component.translatable("haven_create_ui_ux.config.effects_anchor." + name().toLowerCase(java.util.Locale.ROOT)); }
    }

    /** When the hotbar's slot numbers (1 to 9, top left of each socket) show: while the numbers key is held (and while a
     *  screen is open, as the meter numbers; the default), or always. */
    public enum HotbarNumbers implements TranslatableEnum {
        HOLD, ALWAYS;
        @Override public Component getTranslatedName() { return Component.translatable("haven_create_ui_ux.config.hotbar_numbers." + name().toLowerCase(java.util.Locale.ROOT)); }
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<MeterDisplay> METER_DISPLAY;
    public static final ModConfigSpec.EnumValue<PrimaryMana> PRIMARY_MANA;
    public static final ModConfigSpec.EnumValue<EffectsAnchor> EFFECTS_ANCHOR;
    public static final ModConfigSpec.EnumValue<HotbarNumbers> HOTBAR_NUMBERS;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.DoubleValue SIDE_SCALE;
    public static final ModConfigSpec.BooleanValue KEY_PROMPTS;
    public static final ModConfigSpec.BooleanValue KEY_HINTS;

    static {
        var b = new ModConfigSpec.Builder();
        b.push("hud");
        METER_DISPLAY = b.comment("Almost Vanilla: hearts, food and the XP bar at vanilla's places (mana stacked in the XP bar). Orbs or Bars: one iron action bar, health, food and mana as two orbs beside the hotbar or as rows of bars on its wings.")
                .translation("haven_create_ui_ux.config.meter_display").defineEnum("meterDisplay", MeterDisplay.BARS);
        PRIMARY_MANA = b.comment("With both Iron's Spellbooks and Ars Nouveau installed: the mana pool shown first (the orb's left half, the top bar).")
                .translation("haven_create_ui_ux.config.primary_mana").defineEnum("primaryMana", PrimaryMana.IRONS);
        EFFECTS_ANCHOR = b.comment("Where buffs and debuffs show: Top left of the screen (buffs in the first row, debuffs in the row below, as vanilla orders them), or Action bar (a rail above its left wing, debuffs above buffs).")
                .translation("haven_create_ui_ux.config.effects_anchor").defineEnum("effectsAnchor", EffectsAnchor.TOP_LEFT);
        HOTBAR_NUMBERS = b.comment("The hotbar's slot numbers (1 to 9, top left of each socket): Hold (while the HUD numbers key is held or a screen is open) or Always.")
                .translation("haven_create_ui_ux.config.hotbar_numbers").defineEnum("hotbarNumbers", HotbarNumbers.HOLD);
        HUD_SCALE = b.comment("The action bar's size, relative to the GUI scale.")
                .translation("haven_create_ui_ux.config.hud_scale").defineInRange("hudScale", 1.0, 0.5, 2.0);
        SIDE_SCALE = b.comment("The side bars' size (orbs or meters, the offhand, the dash), relative to their design; they stay attached to the action bar.")
                .translation("haven_create_ui_ux.config.side_scale").defineInRange("sideScale", 1.0, 0.6, 2.0);
        b.pop();
        b.push("keyHints");
        KEY_PROMPTS = b.comment("Contextual key hints: the keys for what you look at (ride, trade, rotate with a wrench), one line above the action bar.")
                .translation("haven_create_ui_ux.config.key_prompts").define("keyPrompts", true);
        KEY_HINTS = b.comment("Stateful key hints: the keys for what you ride, hold and wear (a gun's reload and ammo, a MekaSuit's modes), bottom right of the screen.")
                .translation("haven_create_ui_ux.config.key_hints").define("keyHints", true);
        b.pop();
        SPEC = b.build();
    }

    private UiConfig() {}
}
