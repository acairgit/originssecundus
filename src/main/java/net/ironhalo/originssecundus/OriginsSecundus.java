package net.ironhalo.originssecundus;

import com.mojang.logging.LogUtils;
import net.ironhalo.originssecundus.command.OriginsCommands;
import net.ironhalo.originssecundus.data.OriginDataManager;
import net.ironhalo.originssecundus.network.OriginNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import net.minecraft.resources.ResourceLocation;

@Mod(OriginsSecundus.MODID)
public final class OriginsSecundus {
    public static final String MODID = "originssecundus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OriginsSecundus(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(OriginNetwork::register);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new CommonEvents());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @SubscribeEvent
    public void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(OriginDataManager.originsReloadListener());
        event.addListener(OriginDataManager.powersReloadListener());
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        OriginsCommands.register(event.getDispatcher());
    }
}
