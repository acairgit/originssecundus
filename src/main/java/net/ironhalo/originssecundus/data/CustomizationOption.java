package net.ironhalo.originssecundus.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public record CustomizationOption(
        String key,
        String label,
        String type,
        String defaultValue,
        double min,
        double max,
        double step,
        List<String> choices,
        boolean raceUnique
) {
    public static CustomizationOption parse(JsonObject json) {
        List<String> choices = new ArrayList<>();
        if (json.has("choices")) {
            JsonArray array = GsonHelper.getAsJsonArray(json, "choices");
            array.forEach(element -> choices.add(element.getAsString()));
        }
        return new CustomizationOption(
                GsonHelper.getAsString(json, "key"),
                GsonHelper.getAsString(json, "label"),
                GsonHelper.getAsString(json, "type", "choice"),
                GsonHelper.getAsString(json, "default", choices.isEmpty() ? "" : choices.getFirst()),
                GsonHelper.getAsDouble(json, "min", 0.0D),
                GsonHelper.getAsDouble(json, "max", 1.0D),
                GsonHelper.getAsDouble(json, "step", 0.05D),
                List.copyOf(choices),
                GsonHelper.getAsBoolean(json, "race_unique", false)
        );
    }

    public String normalizedDefault() {
        if ("slider".equals(type)) {
            return Double.toString(defaultValue.isBlank() ? min : Double.parseDouble(defaultValue));
        }
        if (!choices.isEmpty() && (defaultValue == null || defaultValue.isBlank())) {
            return choices.getFirst();
        }
        return defaultValue;
    }
}
