package net.ironhalo.originssecundus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.ironhalo.originssecundus.OriginsSecundus;
import net.ironhalo.originssecundus.network.ActivePowerPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@Mod(value = OriginsSecundus.MODID, dist = Dist.CLIENT)
public final class OriginsSecundusClient {
    public static KeyMapping primaryActiveKey;
    public static KeyMapping openOriginScreenKey;

    public OriginsSecundusClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.register(ClientGameEvents.class);
    }

    @EventBusSubscriber(modid = OriginsSecundus.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ClientModEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            primaryActiveKey = new KeyMapping(
                    "key.originssecundus.primary_active",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_G,
                    "category.originssecundus"
            );
            openOriginScreenKey = new KeyMapping(
                    "key.originssecundus.open_origin_screen",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_O,
                    "category.originssecundus"
            );
            event.register(primaryActiveKey);
            event.register(openOriginScreenKey);
        }
    }

    public static final class ClientGameEvents {
        @SubscribeEvent
        public static void clientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) {
                return;
            }

            while (primaryActiveKey != null && primaryActiveKey.consumeClick()) {
                PacketDistributor.sendToServer(new ActivePowerPayload("primary"));
            }

            while (openOriginScreenKey != null && openOriginScreenKey.consumeClick()) {
                minecraft.setScreen(new OriginSelectionScreen(false));
            }

            if (ClientOriginState.shouldOpenInitialSelection() && minecraft.screen == null) {
                ClientOriginState.markSelectionScreenOpened();
                minecraft.setScreen(new OriginSelectionScreen(true));
            }
        }

        @SubscribeEvent
        public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            ClientOriginState.resetConnectionState();
        }
    }
}
