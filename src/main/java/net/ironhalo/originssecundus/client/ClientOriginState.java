package net.ironhalo.originssecundus.client;

import net.ironhalo.originssecundus.network.OriginSyncPayload;
import net.ironhalo.originssecundus.origin.PlayerOrigin;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public final class ClientOriginState {
    private static boolean receivedServerState;
    private static boolean selectionScreenOpened;
    private static ResourceLocation originId;
    private static Map<String, String> customization = Map.of();

    private ClientOriginState() {
    }

    public static void handleSync(OriginSyncPayload payload) {
        receivedServerState = true;
        selectionScreenOpened = false;
        originId = payload.originId().isBlank() ? null : ResourceLocation.parse(payload.originId());
        customization = PlayerOrigin.customizationFromJson(payload.customizationJson());
    }

    public static boolean shouldOpenInitialSelection() {
        return receivedServerState && originId == null && !selectionScreenOpened;
    }

    public static void markSelectionScreenOpened() {
        selectionScreenOpened = true;
    }

    public static Optional<ResourceLocation> originId() {
        return Optional.ofNullable(originId);
    }

    public static Map<String, String> customization() {
        return customization;
    }

    public static void resetConnectionState() {
        receivedServerState = false;
        selectionScreenOpened = false;
        originId = null;
        customization = Map.of();
    }
}
