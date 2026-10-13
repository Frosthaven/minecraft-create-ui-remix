package dev.frosthaven.havenmobbars;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The client config (config/haven_mob_health_bars-client.toml, and Mods > haven-mob-health-bars > Config in game).
 *  Read each frame: NeoForge reloads the values when the file or the config screen changes them. */
public final class MobBarsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue RANGE;
    public static final ModConfigSpec.BooleanValue HOSTILE;
    public static final ModConfigSpec.BooleanValue NOT_HOSTILE;
    public static final ModConfigSpec.BooleanValue FRIENDLY_PLAYERS;
    public static final ModConfigSpec.BooleanValue HOSTILE_PLAYERS;

    static {
        var b = new ModConfigSpec.Builder();
        ENABLED = b.comment("Floating health bars above nearby mobs and players.")
                .translation("haven_mob_health_bars.config.enabled").define("enabled", true);
        RANGE = b.comment("How far away (blocks) a mob still gets a bar.")
                .translation("haven_mob_health_bars.config.range").defineInRange("range", 10, 2, 64);
        HOSTILE = b.comment("Bars on hostile mobs (red).")
                .translation("haven_mob_health_bars.config.hostile").define("hostile", true);
        NOT_HOSTILE = b.comment("Bars on every other mob: animals, villagers, pets, golems, neutral mobs (green).")
                .translation("haven_mob_health_bars.config.not_hostile").define("notHostile", true);
        FRIENDLY_PLAYERS = b.comment("Bars on other players who have not hit a player in the last 30 seconds (blue).")
                .translation("haven_mob_health_bars.config.friendly_players").define("friendlyPlayers", true);
        HOSTILE_PLAYERS = b.comment("Bars on players who hit any player in the last 30 seconds (magenta), whatever the friendly players setting.")
                .translation("haven_mob_health_bars.config.hostile_players").define("hostilePlayers", true);
        SPEC = b.build();
    }

    private MobBarsConfig() {}
}
