package dev.frosthaven.havenmobbars;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Client only: floating health bars over nearby mobs and players ({@link MobBars}), with its config screen. */
@Mod(value = HavenMobHealthBars.ID, dist = Dist.CLIENT)
public final class HavenMobHealthBars {
    public static final String ID = "haven_mob_health_bars";

    public HavenMobHealthBars(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, MobBarsConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RenderNameTagEvent.class, NameTags::onNameTag);   // before Post, same render call
        NeoForge.EVENT_BUS.addListener(RenderLivingEvent.Post.class, MobBars::onRender);
    }
}
