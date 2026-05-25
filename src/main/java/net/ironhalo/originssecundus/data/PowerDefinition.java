package net.ironhalo.originssecundus.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public record PowerDefinition(
        ResourceLocation id,
        String type,
        String name,
        String description,
        JsonObject data
) {
    public static PowerDefinition parse(ResourceLocation id, JsonObject json) {
        return new PowerDefinition(
                id,
                GsonHelper.getAsString(json, "type", "originssecundus:passive"),
                GsonHelper.getAsString(json, "name", id.getPath()),
                GsonHelper.getAsString(json, "description", ""),
                json.deepCopy()
        );
    }

    public boolean is(String path) {
        return type.equals("originssecundus:" + path) || type.equals(path);
    }
}
