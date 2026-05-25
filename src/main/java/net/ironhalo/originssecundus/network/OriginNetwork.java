package net.ironhalo.originssecundus.network;

import net.ironhalo.originssecundus.client.ClientOriginState;
import net.ironhalo.originssecundus.data.OriginDataManager;
import net.ironhalo.originssecundus.origin.PlayerOrigin;
import net.ironhalo.originssecundus.power.PowerEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Map;

public final class OriginNetwork {
    private OriginNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(OriginSelectPayload.TYPE, OriginSelectPayload.STREAM_CODEC, OriginNetwork::handleSelectOrigin);
        registrar.playToServer(ActivePowerPayload.TYPE, ActivePowerPayload.STREAM_CODEC, OriginNetwork::handleActivePower);
        registrar.playToClient(OriginSyncPayload.TYPE, OriginSyncPayload.STREAM_CODEC, OriginNetwork::handleSyncOrigin);
    }

    private static void handleSelectOrigin(OriginSelectPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        ResourceLocation originId;
        try {
            originId = ResourceLocation.parse(payload.originId());
        } catch (RuntimeException exception) {
            return;
        }

        if (OriginDataManager.origin(originId).isEmpty()) {
            return;
        }

        Map<String, String> customization = PlayerOrigin.customizationFromJson(payload.customizationJson());
        PlayerOrigin.setOrigin(player, originId, customization);
        player.sendSystemMessage(Component.translatable("message.originssecundus.origin_selected", originId.toString()));
    }

    private static void handleActivePower(ActivePowerPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PowerEngine.activate(player, payload.key());
        }
    }

    private static void handleSyncOrigin(OriginSyncPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist.isClient()) {
            ClientOriginState.handleSync(payload);
        }
    }
}
