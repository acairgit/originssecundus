package net.ironhalo.originssecundus.origin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.ironhalo.originssecundus.OriginsSecundus;
import net.ironhalo.originssecundus.data.OriginDataManager;
import net.ironhalo.originssecundus.data.OriginDefinition;
import net.ironhalo.originssecundus.network.OriginSyncPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PlayerOrigin {
    private static final Gson GSON = new Gson();
    private static final String ROOT = "OriginsSecundus";
    private static final String ORIGIN = "Origin";
    private static final String CUSTOMIZATION = "Customization";

    private PlayerOrigin() {
    }

    public static Optional<ResourceLocation> selectedOriginId(Player player) {
        CompoundTag root = root(player);
        if (!root.contains(ORIGIN)) {
            return Optional.empty();
        }
        try {
            return Optional.of(ResourceLocation.parse(root.getString(ORIGIN)));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<OriginDefinition> selectedOrigin(Player player) {
        return selectedOriginId(player).flatMap(OriginDataManager::origin);
    }

    public static boolean hasSelectedOrigin(Player player) {
        return selectedOriginId(player).isPresent();
    }

    public static void setOrigin(ServerPlayer player, ResourceLocation originId, Map<String, String> customization) {
        CompoundTag root = root(player);
        root.putString(ORIGIN, originId.toString());
        CompoundTag values = new CompoundTag();
        customization.forEach(values::putString);
        root.put(CUSTOMIZATION, values);
        syncToClient(player);
    }

    public static void clear(ServerPlayer player) {
        player.getPersistentData().remove(ROOT);
        syncToClient(player);
    }

    public static Map<String, String> customization(Player player) {
        CompoundTag root = root(player);
        Map<String, String> values = new LinkedHashMap<>();
        if (root.contains(CUSTOMIZATION)) {
            CompoundTag tag = root.getCompound(CUSTOMIZATION);
            for (String key : tag.getAllKeys()) {
                values.put(key, tag.getString(key));
            }
        }
        return values;
    }

    public static void copyPersistentData(Player oldPlayer, Player newPlayer) {
        CompoundTag oldRoot = root(oldPlayer);
        if (!oldRoot.isEmpty()) {
            newPlayer.getPersistentData().put(ROOT, oldRoot.copy());
        }
    }

    public static void syncToClient(ServerPlayer player) {
        String origin = selectedOriginId(player).map(ResourceLocation::toString).orElse("");
        PacketDistributor.sendToPlayer(player, new OriginSyncPayload(origin, customizationToJson(customization(player))));
    }

    public static Map<String, String> customizationFromJson(String json) {
        Map<String, String> values = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return values;
        }
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            object.entrySet().forEach(entry -> values.put(entry.getKey(), entry.getValue().getAsString()));
        } catch (RuntimeException exception) {
            OriginsSecundus.LOGGER.warn("Invalid customization json from client: {}", json);
        }
        return values;
    }

    public static String customizationToJson(Map<String, String> customization) {
        JsonObject object = new JsonObject();
        customization.forEach(object::addProperty);
        return GSON.toJson(object);
    }

    private static CompoundTag root(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }
}
