package net.ironhalo.originssecundus.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.ironhalo.originssecundus.OriginsSecundus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OriginDataManager {
    private static final Gson GSON = new Gson();
    private static final OriginReloadListener ORIGINS_RELOAD_LISTENER = new OriginReloadListener();
    private static final PowerReloadListener POWERS_RELOAD_LISTENER = new PowerReloadListener();
    private static Map<ResourceLocation, OriginDefinition> origins = Map.of();
    private static Map<ResourceLocation, PowerDefinition> powers = Map.of();

    private OriginDataManager() {
    }

    public static SimpleJsonResourceReloadListener originsReloadListener() {
        return ORIGINS_RELOAD_LISTENER;
    }

    public static SimpleJsonResourceReloadListener powersReloadListener() {
        return POWERS_RELOAD_LISTENER;
    }

    public static List<OriginDefinition> origins() {
        if (origins.isEmpty()) {
            return List.of(OriginDefinition.fallbackHuman());
        }
        return origins.values().stream()
                .filter(origin -> !origin.hidden())
                .sorted(Comparator.comparingInt(OriginDefinition::order).thenComparing(origin -> origin.id().toString()))
                .toList();
    }

    public static Optional<OriginDefinition> origin(ResourceLocation id) {
        if (origins.isEmpty() && id.equals(OriginsSecundus.id("human"))) {
            return Optional.of(OriginDefinition.fallbackHuman());
        }
        return Optional.ofNullable(origins.get(id));
    }

    public static Optional<PowerDefinition> power(ResourceLocation id) {
        return Optional.ofNullable(powers.get(id));
    }

    public static List<PowerDefinition> powersFor(OriginDefinition origin) {
        List<PowerDefinition> result = new ArrayList<>();
        for (ResourceLocation powerId : origin.powers()) {
            power(powerId).ifPresent(result::add);
        }
        return result;
    }

    public static List<PowerDefinition> allPowers() {
        return List.copyOf(powers.values());
    }

    private static final class OriginReloadListener extends SimpleJsonResourceReloadListener {
        private OriginReloadListener() {
            super(GSON, "origins");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, OriginDefinition> loaded = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
                try {
                    JsonObject json = entry.getValue().getAsJsonObject();
                    loaded.put(entry.getKey(), OriginDefinition.parse(entry.getKey(), json));
                } catch (RuntimeException exception) {
                    OriginsSecundus.LOGGER.error("Failed to load origin {}", entry.getKey(), exception);
                }
            }
            origins = Map.copyOf(loaded);
            OriginsSecundus.LOGGER.info("Loaded {} Origins Secundus origins", origins.size());
        }
    }

    private static final class PowerReloadListener extends SimpleJsonResourceReloadListener {
        private PowerReloadListener() {
            super(GSON, "powers");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, PowerDefinition> loaded = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
                try {
                    JsonObject json = entry.getValue().getAsJsonObject();
                    loaded.put(entry.getKey(), PowerDefinition.parse(entry.getKey(), json));
                } catch (RuntimeException exception) {
                    OriginsSecundus.LOGGER.error("Failed to load power {}", entry.getKey(), exception);
                }
            }
            powers = Map.copyOf(loaded);
            OriginsSecundus.LOGGER.info("Loaded {} Origins Secundus powers", powers.size());
        }
    }
}
