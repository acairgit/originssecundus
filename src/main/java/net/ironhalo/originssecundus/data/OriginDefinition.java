package net.ironhalo.originssecundus.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.ironhalo.originssecundus.OriginsSecundus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record OriginDefinition(
        ResourceLocation id,
        String name,
        String description,
        ResourceLocation iconItem,
        int order,
        int impact,
        int accentColor,
        String modelHint,
        boolean hidden,
        List<ResourceLocation> powers,
        List<CustomizationOption> uniqueCustomization
) {
    private static final List<CustomizationOption> COMMON_CUSTOMIZATION = List.of(
            new CustomizationOption("height", "Height", "slider", "1.0", 0.85D, 1.15D, 0.01D, List.of(), false),
            new CustomizationOption("eye_type", "Eye type", "choice", "soft", 0, 0, 0, List.of("soft", "sharp", "round"), false),
            new CustomizationOption("eye_color", "Eye color", "choice", "green", 0, 0, 0, List.of("green", "blue", "amber", "violet"), false),
            new CustomizationOption("mouth", "Mouth", "choice", "neutral", 0, 0, 0, List.of("neutral", "smile", "stern"), false),
            new CustomizationOption("hair", "Hairstyle", "choice", "short", 0, 0, 0, List.of("short", "braid", "wild", "hooded"), false),
            new CustomizationOption("skin_tone", "Skin tone", "choice", "natural", 0, 0, 0, List.of("natural", "warm", "ashen", "deep"), false),
            new CustomizationOption("outfit", "Starting outfit", "choice", "traveler", 0, 0, 0, List.of("traveler", "robes", "scout", "worker"), false)
    );

    public static OriginDefinition parse(ResourceLocation id, JsonObject json) {
        List<ResourceLocation> powers = new ArrayList<>();
        if (json.has("powers")) {
            JsonArray array = GsonHelper.getAsJsonArray(json, "powers");
            array.forEach(element -> powers.add(ResourceLocation.parse(element.getAsString())));
        }

        List<CustomizationOption> customization = new ArrayList<>();
        if (json.has("customization")) {
            JsonArray array = GsonHelper.getAsJsonArray(json, "customization");
            array.forEach(element -> customization.add(CustomizationOption.parse(element.getAsJsonObject())));
        }

        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("minecraft", "player_head");
        if (json.has("icon")) {
            JsonObject iconObject = GsonHelper.getAsJsonObject(json, "icon");
            icon = ResourceLocation.parse(GsonHelper.getAsString(iconObject, "item", "minecraft:player_head"));
        }

        return new OriginDefinition(
                id,
                GsonHelper.getAsString(json, "name", id.getPath()),
                GsonHelper.getAsString(json, "description", ""),
                icon,
                GsonHelper.getAsInt(json, "order", 0),
                GsonHelper.getAsInt(json, "impact", 0),
                parseColor(GsonHelper.getAsString(json, "accent_color", "#b48a18")),
                GsonHelper.getAsString(json, "model_hint", "humanoid"),
                GsonHelper.getAsBoolean(json, "hidden", false),
                List.copyOf(powers),
                List.copyOf(customization)
        );
    }

    public List<CustomizationOption> customization() {
        List<CustomizationOption> merged = new ArrayList<>(COMMON_CUSTOMIZATION);
        merged.addAll(uniqueCustomization);
        return merged;
    }

    public Map<String, String> defaultCustomizationValues() {
        return customization().stream()
                .sorted(Comparator.comparing(CustomizationOption::key))
                .collect(java.util.stream.Collectors.toMap(
                        CustomizationOption::key,
                        CustomizationOption::normalizedDefault,
                        (left, right) -> right,
                        java.util.LinkedHashMap::new
                ));
    }

    public static OriginDefinition fallbackHuman() {
        return new OriginDefinition(
                OriginsSecundus.id("human"),
                "Human",
                "A regular human. Your ordinary Minecraft experience awaits.",
                ResourceLocation.fromNamespaceAndPath("minecraft", "player_head"),
                0,
                0,
                0x8a8a8a,
                "humanoid",
                false,
                List.of(),
                List.of()
        );
    }

    private static int parseColor(String value) {
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        try {
            return Integer.parseUnsignedInt(normalized, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return 0xB48A18;
        }
    }
}
