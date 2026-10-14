package dev.frosthaven.havencreateuiux;

import dev.frosthaven.havencreateuiux.hud.HudLog;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * The haven-create-hud resource pack, built into this jar (the build copies resourcepacks/haven-create-hud to
 * {@link #PATH}) and registered as a required pack fixed at the top of the list: the mod and its art always come and
 * go together, and players cannot disable or reorder it separately. Its id contains "create_hud", so
 * {@link HudLayers#isCreateHudPack} recognises it like the old standalone pack.
 */
public final class BuiltInPack {
    public static final String PATH = "resourcepacks/haven_create_hud";
    public static final String ID = "mod/" + HavenCreateUiUx.ID + ":" + PATH;

    private static final HudLog.Channel LOG = new HudLog.Channel("[built-in pack] ");

    private BuiltInPack() {}

    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        var info = ModList.get().getModContainerById(HavenCreateUiUx.ID).orElseThrow().getModInfo();
        var root = info.getOwningFile().getFile().findResource(PATH);
        var pack = Pack.readMetaAndCreate(
                new PackLocationInfo(ID, Component.literal("Haven Create HUD"), PackSource.BUILT_IN,
                        Optional.of(new KnownPack("neoforge", ID, info.getVersion().toString()))),
                BuiltInPackSource.fromName(path -> new PathPackResources(path, root)),
                PackType.CLIENT_RESOURCES,
                new PackSelectionConfig(true, Pack.Position.TOP, true));   // required, top, fixed position
        if (pack == null) {
            LOG.error("read", PATH + " could not be read from the jar; the action bar art is missing", null);
            return;
        }
        event.addRepositorySource(consumer -> consumer.accept(pack));
        LOG.once("registered", "registered {} (required, fixed at the top)", ID);
    }
}
