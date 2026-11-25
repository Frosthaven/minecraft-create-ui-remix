package dev.frosthaven.havenmobbars;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The client config (config/haven_mob_health_bars-client.toml, and Mods > haven-mob-health-bars > Config in game).
 *  Read each frame: NeoForge reloads the values when the file or the config screen changes them. */
public final class MobBarsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue RANGE;
    public static final ModConfigSpec.BooleanValue LOOKED_AT;
    public static final ModConfigSpec.IntValue LOOK_RANGE;
    public static final ModConfigSpec.BooleanValue HOSTILE;
    public static final ModConfigSpec.BooleanValue NOT_HOSTILE;
    public static final ModConfigSpec.EnumValue<When> NOT_HOSTILE_WHEN;
    public static final ModConfigSpec.BooleanValue FRIENDLY_PLAYERS;
    public static final ModConfigSpec.BooleanValue HOSTILE_PLAYERS;
    public static final ModConfigSpec.BooleanValue ARMOUR;

    static {
        var b = new ModConfigSpec.Builder();
        ENABLED = b.comment("Floating health bars above nearby mobs and players.")
                .translation("haven_mob_health_bars.config.enabled").define("enabled", true);
        RANGE = b.comment("How far away (blocks) a mob still gets a bar.")
                .translation("haven_mob_health_bars.config.range").defineInRange("range", 10, 2, 64);
        LOOKED_AT = b.comment("Also a bar on the entity under the crosshair beyond the range, when no block is in between (only that one).")
                .translation("haven_mob_health_bars.config.looked_at").define("lookedAt", true);
        LOOK_RANGE = b.comment("How far away (blocks) the looked-at entity still gets a bar.")
                .translation("haven_mob_health_bars.config.look_range").defineInRange("lookRange", 64, 8, 128);
        HOSTILE = b.comment("Bars on hostile mobs (red).")
                .translation("haven_mob_health_bars.config.hostile").define("hostile", true);
        NOT_HOSTILE = b.comment("Bars on every other mob: animals, villagers, pets, golems, neutral mobs (green).")
                .translation("haven_mob_health_bars.config.not_hostile").define("notHostile", true);
        NOT_HOSTILE_WHEN = b.comment("When the bars of non-hostile mobs and friendly players show: WHEN_DAMAGED (below full health or worn armour below full durability; the default) or ALWAYS.")
                .translation("haven_mob_health_bars.config.not_hostile_when").defineEnum("notHostileWhen", When.WHEN_DAMAGED);
        FRIENDLY_PLAYERS = b.comment("Bars on other players who have not hit a player in the last 30 seconds (blue).")
                .translation("haven_mob_health_bars.config.friendly_players").define("friendlyPlayers", true);
        HOSTILE_PLAYERS = b.comment("Bars on players who hit any player in the last 30 seconds (magenta), whatever the friendly players setting.")
                .translation("haven_mob_health_bars.config.hostile_players").define("hostilePlayers", true);
        ARMOUR = b.comment("The worn armour's durability left as the bar's bottom strip (armour slots, horse, wolf and Drekki barding), when the mob wears damageable armour. Worn armour counts as damaged for notHostileWhen.")
                .translation("haven_mob_health_bars.config.armour").define("armourDurability", true);
        SPEC = b.build();
    }

    /** When a bar of the other mobs shows. */
    public enum When implements net.neoforged.neoforge.common.TranslatableEnum {
        ALWAYS("Always"), WHEN_DAMAGED("When damaged");
        private final String label;
        When(String label) { this.label = label; }
        @Override public net.minecraft.network.chat.Component getTranslatedName() { return net.minecraft.network.chat.Component.literal(label); }
    }

    private MobBarsConfig() {}
}
