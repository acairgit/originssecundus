package net.ironhalo.originssecundus.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.ironhalo.originssecundus.OriginsSecundus;
import net.ironhalo.originssecundus.data.OriginDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

final class HumanEditorSkin {
    static final String ORIGIN = "editor_origin";
    static final String GENDER = "human_gender";
    static final String HEIGHT = "height";
    static final String WIDTH = "human_width";
    static final String DEPTH = "human_depth";
    static final String ARM_MUSCLES = "human_arm_muscles";
    static final String LEG_MUSCLES = "human_leg_muscles";
    static final String ARM_LENGTH = "human_arm_length";
    static final String LEG_LENGTH = "human_leg_length";
    static final String SKIN = "human_skin";
    static final String SKIN_COLOR_X = "human_skin_color_x";
    static final String SKIN_COLOR_Y = "human_skin_color_y";
    static final String EYES = "human_eyes";
    static final String EYE_COLOR_X = "human_eye_color_x";
    static final String EYE_COLOR_Y = "human_eye_color_y";
    static final String LEFT_EYE_COLOR_X = "human_left_eye_color_x";
    static final String LEFT_EYE_COLOR_Y = "human_left_eye_color_y";
    static final String RIGHT_EYE_COLOR_X = "human_right_eye_color_x";
    static final String RIGHT_EYE_COLOR_Y = "human_right_eye_color_y";
    static final String EYELASHES = "human_eyelashes";
    static final String EYELASHES_BRIGHTNESS = "human_eyelashes_brightness";
    static final String HAIR = "human_hair";
    static final String HAIR_COLOR_X = "human_hair_color_x";
    static final String HAIR_COLOR_Y = "human_hair_color_y";
    static final String EYEBROWS = "human_eyebrows";
    static final String EYEBROWS_BRIGHTNESS = "human_eyebrows_brightness";
    static final String BEARD = "human_beard";
    static final String SCARS = "human_scars";
    static final String TATTOO = "human_tattoo";
    static final String CLOTHES = "human_clothes";
    static final String WINGS = "avian_wings";

    private static final String MALE = "male";
    private static final String FEMALE = "female";
    private static final ResourceLocation SKIN_COLORMAP = editorTexture("human/colormap/villager_skin");
    private static final ResourceLocation HAIR_COLORMAP = editorTexture("human/colormap/villager_hair");
    private static final ResourceLocation EYES_COLORMAP = editorTexture("human/colormap/villager_eyes");
    private static String cachedSignature = "";
    private static PlayerSkin cachedSkin;

    private HumanEditorSkin() {
    }

    static boolean isHuman(OriginDefinition origin) {
        return origin.id().equals(OriginsSecundus.id("human"));
    }

    static boolean isAvian(OriginDefinition origin) {
        return origin.id().equals(OriginsSecundus.id("avian"));
    }

    static boolean isEditable(OriginDefinition origin) {
        return isHuman(origin) || isAvian(origin);
    }

    static boolean isEditable(ResourceLocation originId) {
        return originId.equals(OriginsSecundus.id("human")) || originId.equals(OriginsSecundus.id("avian"));
    }

    static void ensureDefaults(Map<String, String> values) {
        values.putIfAbsent(ORIGIN, "human");
        values.putIfAbsent(GENDER, MALE);
        values.putIfAbsent(HEIGHT, "1.0");
        values.putIfAbsent(WIDTH, "1.0");
        values.putIfAbsent(DEPTH, "1.0");
        values.putIfAbsent(ARM_MUSCLES, "0");
        values.putIfAbsent(LEG_MUSCLES, "0");
        values.putIfAbsent(ARM_LENGTH, "0");
        values.putIfAbsent(LEG_LENGTH, "0");
        values.putIfAbsent(SKIN, "1");
        values.putIfAbsent(SKIN_COLOR_X, "0.22");
        values.putIfAbsent(SKIN_COLOR_Y, "0.16");
        values.putIfAbsent(EYES, "0");
        values.putIfAbsent(EYE_COLOR_X, "0.5");
        values.putIfAbsent(EYE_COLOR_Y, "0.5");
        values.putIfAbsent(LEFT_EYE_COLOR_X, "0.5");
        values.putIfAbsent(LEFT_EYE_COLOR_Y, "0.5");
        values.putIfAbsent(RIGHT_EYE_COLOR_X, "0.5");
        values.putIfAbsent(RIGHT_EYE_COLOR_Y, "0.5");
        values.putIfAbsent(EYELASHES, "0");
        values.putIfAbsent(EYELASHES_BRIGHTNESS, "0");
        values.putIfAbsent(HAIR, "1");
        values.putIfAbsent(HAIR_COLOR_X, "0.5");
        values.putIfAbsent(HAIR_COLOR_Y, "0.5");
        values.putIfAbsent(EYEBROWS, "1");
        values.putIfAbsent(EYEBROWS_BRIGHTNESS, "0");
        values.putIfAbsent(BEARD, "0");
        values.putIfAbsent(SCARS, "0");
        values.putIfAbsent(TATTOO, "0");
        values.putIfAbsent(CLOTHES, "0");
        if ("avian".equals(values.get(ORIGIN))) {
            values.putIfAbsent(WINGS, "1");
        }
    }

    static void ensureDefaults(Map<String, String> values, OriginDefinition origin) {
        ensureDefaults(values, origin.id());
    }

    static void ensureDefaults(Map<String, String> values, ResourceLocation originId) {
        values.put(ORIGIN, originId.equals(OriginsSecundus.id("avian")) ? "avian" : "human");
        ensureDefaults(values);
    }

    static PlayerSkin skin(Minecraft minecraft, Map<String, String> values) {
        ensureDefaults(values);
        return skin(minecraft, OriginsSecundus.id(assetRoot(values)), values);
    }

    static PlayerSkin skin(Minecraft minecraft, OriginDefinition origin, Map<String, String> values) {
        return skin(minecraft, origin.id(), values);
    }

    static PlayerSkin skin(Minecraft minecraft, ResourceLocation originId, Map<String, String> values) {
        ensureDefaults(values, originId);
        clampHumanValues(minecraft, values);
        PlayerSkin.Model model = model(values);
        String signature = signature(values, model);
        if (!signature.equals(cachedSignature) || cachedSkin == null) {
            NativeImage image = compose(minecraft, values);
            ResourceLocation dynamicSkinTexture = dynamicSkinTexture(values);
            minecraft.getTextureManager().register(dynamicSkinTexture, new DynamicTexture(image));
            cachedSkin = new PlayerSkin(dynamicSkinTexture, null, null, null, model, true);
            cachedSignature = signature;
        } else if (cachedSkin.model() != model) {
            cachedSkin = new PlayerSkin(dynamicSkinTexture(values), null, null, null, model, true);
        }
        return cachedSkin;
    }

    static ResourceLocation paletteTexture(String key) {
        return HAIR_COLOR_X.equals(key) || HAIR_COLOR_Y.equals(key) ? HAIR_COLORMAP : SKIN_COLORMAP;
    }

    static String gender(Map<String, String> values) {
        return FEMALE.equals(values.get(GENDER)) ? FEMALE : MALE;
    }

    static PlayerSkin.Model model(Map<String, String> values) {
        return FEMALE.equals(gender(values)) ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE;
    }

    static int minIndex(String folder) {
        if ("eyes".equals(folder)) {
            return 0;
        }
        return hasNoneChoice(folder) ? 0 : firstTextureIndex(folder);
    }

    static int maxIndex(Minecraft minecraft, Map<String, String> values, String folder) {
        if ("eyes".equals(folder)) {
            return 1;
        }
        if ("wings".equals(folder)) {
            return wingsMaxIndex(minecraft, values);
        }
        String gender = gender(values);
        int first = firstTextureIndex(folder);
        int last = first - 1;
        for (int index = first; index < 128; index++) {
            if (!resourceExists(minecraft, texture(values, gender, folder, index))) {
                break;
            }
            last = index;
        }
        if (last < first) {
            return hasNoneChoice(folder) ? 0 : first;
        }
        return switch (folder) {
            case "hair", "eyelashes", "beard" -> last + 1;
            default -> last;
        };
    }

    static boolean hasNoneChoice(String folder) {
        return switch (folder) {
            case "hair", "eyelashes", "eyebrows", "beard", "scars", "tattoo" -> true;
            default -> false;
        };
    }

    static double heightScale(Map<String, String> values) {
        return scaleValue(values, HEIGHT, 0.94D, 1.05D);
    }

    static double widthScale(Map<String, String> values) {
        return scaleValue(values, WIDTH, 0.93D, 1.06D);
    }

    static double depthScale(Map<String, String> values) {
        return scaleValue(values, DEPTH, 0.92D, 1.06D);
    }

    static int scaleOffset(Map<String, String> values, String key) {
        double scale = scaleValue(values, key, minScale(key), maxScale(key));
        if (scale < 1.0D) {
            return (int) Math.round((scale - 1.0D) / Math.max(0.0001D, 1.0D - minScale(key)) * 5.0D);
        }
        return (int) Math.round((scale - 1.0D) / Math.max(0.0001D, maxScale(key) - 1.0D) * 5.0D);
    }

    static double scaleFromOffset(String key, int offset) {
        int clamped = clamp(offset, -5, 5);
        if (clamped < 0) {
            return 1.0D + (1.0D - minScale(key)) * clamped / 5.0D;
        }
        return 1.0D + (maxScale(key) - 1.0D) * clamped / 5.0D;
    }

    static int offsetValue(Map<String, String> values, String key) {
        return clamp(parseInt(values.get(key), 0), -5, 5);
    }

    static float armMuscleScale(Map<String, String> values) {
        return offsetScale(values, ARM_MUSCLES, 0.96F, 1.12F);
    }

    static float legMuscleScale(Map<String, String> values) {
        return offsetScale(values, LEG_MUSCLES, 0.96F, 1.11F);
    }

    static float armLengthScale(Map<String, String> values) {
        return offsetScale(values, ARM_LENGTH, 0.96F, 1.08F);
    }

    static float legLengthScale(Map<String, String> values) {
        return offsetScale(values, LEG_LENGTH, 0.96F, 1.08F);
    }

    static ResourceLocation skinColormap() {
        return SKIN_COLORMAP;
    }

    static ResourceLocation hairColormap() {
        return HAIR_COLORMAP;
    }

    static ResourceLocation eyesColormap() {
        return EYES_COLORMAP;
    }

    static ResourceLocation wingsTexture(Map<String, String> values) {
        ensureDefaults(values);
        if (!"avian".equals(assetRoot(values))) {
            return null;
        }
        int wing = clamp(parseInt(values.get(WINGS), 1), 1, 128);
        return editorTexture("avian/wings/textures/" + wing);
    }

    private static void clampHumanValues(Minecraft minecraft, Map<String, String> values) {
        values.put(GENDER, gender(values));
        values.put(HEIGHT, Double.toString(heightScale(values)));
        values.put(WIDTH, Double.toString(widthScale(values)));
        values.put(DEPTH, Double.toString(depthScale(values)));
        values.put(ARM_MUSCLES, Integer.toString(offsetValue(values, ARM_MUSCLES)));
        values.put(LEG_MUSCLES, Integer.toString(offsetValue(values, LEG_MUSCLES)));
        values.put(ARM_LENGTH, Integer.toString(offsetValue(values, ARM_LENGTH)));
        values.put(LEG_LENGTH, Integer.toString(offsetValue(values, LEG_LENGTH)));
        clampIndexValue(minecraft, values, SKIN, "skin");
        clampIndexValue(minecraft, values, EYES, "eyes");
        clampIndexValue(minecraft, values, EYELASHES, "eyelashes");
        clampIndexValue(minecraft, values, HAIR, "hair");
        clampIndexValue(minecraft, values, EYEBROWS, "eyebrows");
        clampIndexValue(minecraft, values, BEARD, "beard");
        clampIndexValue(minecraft, values, TATTOO, "tattoo");
        clampIndexValue(minecraft, values, CLOTHES, "clothes");
        if ("avian".equals(assetRoot(values))) {
            values.put(WINGS, Integer.toString(clamp(parseInt(values.get(WINGS), 1), 1, wingsMaxIndex(minecraft, values))));
        }
        values.put(SCARS, Integer.toString(clamp(parseInt(values.get(SCARS), 0), 0, maxIndex(minecraft, values, "scars"))));
        values.put(SKIN_COLOR_X, Double.toString(clamp(parseDouble(values.get(SKIN_COLOR_X), 0.22D), 0.0D, 1.0D)));
        values.put(SKIN_COLOR_Y, Double.toString(clamp(parseDouble(values.get(SKIN_COLOR_Y), 0.16D), 0.0D, 1.0D)));
        values.put(HAIR_COLOR_X, Double.toString(clamp(parseDouble(values.get(HAIR_COLOR_X), 0.5D), 0.0D, 1.0D)));
        values.put(HAIR_COLOR_Y, Double.toString(clamp(parseDouble(values.get(HAIR_COLOR_Y), 0.5D), 0.0D, 1.0D)));
        values.put(EYE_COLOR_X, Double.toString(clamp(parseDouble(values.get(EYE_COLOR_X), 0.5D), 0.0D, 1.0D)));
        values.put(EYE_COLOR_Y, Double.toString(clamp(parseDouble(values.get(EYE_COLOR_Y), 0.5D), 0.0D, 1.0D)));
        values.put(LEFT_EYE_COLOR_X, Double.toString(clamp(parseDouble(values.get(LEFT_EYE_COLOR_X), 0.5D), 0.0D, 1.0D)));
        values.put(LEFT_EYE_COLOR_Y, Double.toString(clamp(parseDouble(values.get(LEFT_EYE_COLOR_Y), 0.5D), 0.0D, 1.0D)));
        values.put(RIGHT_EYE_COLOR_X, Double.toString(clamp(parseDouble(values.get(RIGHT_EYE_COLOR_X), 0.5D), 0.0D, 1.0D)));
        values.put(RIGHT_EYE_COLOR_Y, Double.toString(clamp(parseDouble(values.get(RIGHT_EYE_COLOR_Y), 0.5D), 0.0D, 1.0D)));
        values.put(EYEBROWS_BRIGHTNESS, Integer.toString(offsetValue(values, EYEBROWS_BRIGHTNESS)));
        values.put(EYELASHES_BRIGHTNESS, Integer.toString(offsetValue(values, EYELASHES_BRIGHTNESS)));
    }

    private static void clampIndexValue(Minecraft minecraft, Map<String, String> values, String key, String folder) {
        int min = minIndex(folder);
        int max = maxIndex(minecraft, values, folder);
        values.put(key, Integer.toString(clamp(parseInt(values.get(key), min), min, max)));
    }

    private static String signature(Map<String, String> values, PlayerSkin.Model model) {
        return model.id()
                + "|" + values.get(ORIGIN)
                + "|" + values.get(GENDER)
                + "|" + values.get(SKIN)
                + "|" + values.get(SKIN_COLOR_X)
                + "|" + values.get(SKIN_COLOR_Y)
                + "|" + values.get(EYES)
                + "|" + values.get(EYE_COLOR_X)
                + "|" + values.get(EYE_COLOR_Y)
                + "|" + values.get(LEFT_EYE_COLOR_X)
                + "|" + values.get(LEFT_EYE_COLOR_Y)
                + "|" + values.get(RIGHT_EYE_COLOR_X)
                + "|" + values.get(RIGHT_EYE_COLOR_Y)
                + "|" + values.get(EYELASHES)
                + "|" + values.get(EYELASHES_BRIGHTNESS)
                + "|" + values.get(HAIR)
                + "|" + values.get(HAIR_COLOR_X)
                + "|" + values.get(HAIR_COLOR_Y)
                + "|" + values.get(EYEBROWS)
                + "|" + values.get(EYEBROWS_BRIGHTNESS)
                + "|" + values.get(BEARD)
                + "|" + values.get(SCARS)
                + "|" + values.get(TATTOO)
                + "|" + values.get(CLOTHES);
    }

    private static NativeImage compose(Minecraft minecraft, Map<String, String> values) {
        String gender = gender(values);
        NativeImage target = new NativeImage(64, 64, true);
        int skinColor = samplePalette(minecraft, colormap(values, "villager_skin"), values.get(SKIN_COLOR_X), values.get(SKIN_COLOR_Y), 0xFFD8B89A);
        int hairColor = samplePalette(minecraft, colormap(values, "villager_hair"), values.get(HAIR_COLOR_X), values.get(HAIR_COLOR_Y), 0xFF5B3A26);
        int eyeColor = vividEyeColor(samplePalette(minecraft, colormap(values, "villager_eyes"), values.get(EYE_COLOR_X), values.get(EYE_COLOR_Y), 0xFF70553A));
        int leftEyeColor = vividEyeColor(samplePalette(minecraft, colormap(values, "villager_eyes"), values.get(LEFT_EYE_COLOR_X), values.get(LEFT_EYE_COLOR_Y), 0xFF70553A));
        int rightEyeColor = vividEyeColor(samplePalette(minecraft, colormap(values, "villager_eyes"), values.get(RIGHT_EYE_COLOR_X), values.get(RIGHT_EYE_COLOR_Y), 0xFF70553A));

        applyTintedLayer(minecraft, target, texture(values, gender, "skin", parseInt(values.get(SKIN), 1)), skinColor);
        ResourceLocation scarsTexture = textureForChoice(values, gender, "scars", parseInt(values.get(SCARS), 0));
        if (scarsTexture != null) {
            applyRawLayer(minecraft, target, scarsTexture);
        }
        ResourceLocation tattooTexture = textureForChoice(values, gender, "tattoo", parseInt(values.get(TATTOO), 0));
        if (tattooTexture != null) {
            applyRawLayer(minecraft, target, tattooTexture);
        }
        applyRawLayer(minecraft, target, texture(values, gender, "clothes", parseInt(values.get(CLOTHES), 0)));
        applyRawLayer(minecraft, target, namedTexture(values, gender, "eyes", "eye_base"));
        if (parseInt(values.get(EYES), 0) == 0) {
            applyTintedLayer(minecraft, target, namedTexture(values, gender, "eyes", "left_eye"), eyeColor);
            applyTintedLayer(minecraft, target, namedTexture(values, gender, "eyes", "right_eye"), eyeColor);
        } else {
            applyTintedLayer(minecraft, target, namedTexture(values, gender, "eyes", "left_eye"), leftEyeColor);
            applyTintedLayer(minecraft, target, namedTexture(values, gender, "eyes", "right_eye"), rightEyeColor);
        }
        ResourceLocation eyelashesTexture = textureForChoice(values, gender, "eyelashes", parseInt(values.get(EYELASHES), 0));
        if (eyelashesTexture != null) {
            applyTintedLayer(
                    minecraft,
                    target,
                    eyelashesTexture,
                adjustColor(hairColor, offsetValue(values, EYELASHES_BRIGHTNESS), 0.50D, 1.12D)
            );
        }
        ResourceLocation eyebrowsTexture = textureForChoice(values, gender, "eyebrows", parseInt(values.get(EYEBROWS), 1));
        if (eyebrowsTexture != null) {
            applyTintedLayer(minecraft, target, eyebrowsTexture, adjustColor(hairColor, offsetValue(values, EYEBROWS_BRIGHTNESS), 0.55D, 1.25D));
        }
        ResourceLocation hairTexture = textureForChoice(values, gender, "hair", parseInt(values.get(HAIR), 1));
        if (hairTexture != null) {
            applyTintedLayer(minecraft, target, hairTexture, hairColor);
        }
        if (MALE.equals(gender)) {
            ResourceLocation beardTexture = textureForChoice(values, gender, "beard", parseInt(values.get(BEARD), 0));
            if (beardTexture != null) {
                applyTintedLayer(minecraft, target, beardTexture, hairColor);
            }
        }
        return target;
    }

    private static void applyRawLayer(Minecraft minecraft, NativeImage target, ResourceLocation texture) {
        try (NativeImage layer = readTexture(minecraft, texture)) {
            if (layer == null) {
                return;
            }
            for (int y = 0; y < Math.min(target.getHeight(), layer.getHeight()); y++) {
                for (int x = 0; x < Math.min(target.getWidth(), layer.getWidth()); x++) {
                    int source = layer.getPixelRGBA(x, y);
                    if (FastColor.ABGR32.alpha(source) > 0) {
                        target.blendPixel(x, y, source);
                    }
                }
            }
        }
    }

    private static void applyTintedLayer(Minecraft minecraft, NativeImage target, ResourceLocation texture, int tintAbgr) {
        try (NativeImage layer = readTexture(minecraft, texture)) {
            if (layer == null) {
                return;
            }
            int tintR = FastColor.ABGR32.red(tintAbgr);
            int tintG = FastColor.ABGR32.green(tintAbgr);
            int tintB = FastColor.ABGR32.blue(tintAbgr);
            for (int y = 0; y < Math.min(target.getHeight(), layer.getHeight()); y++) {
                for (int x = 0; x < Math.min(target.getWidth(), layer.getWidth()); x++) {
                    int source = layer.getPixelRGBA(x, y);
                    int alpha = FastColor.ABGR32.alpha(source);
                    if (alpha == 0) {
                        continue;
                    }
                    int shade = (FastColor.ABGR32.red(source) + FastColor.ABGR32.green(source) + FastColor.ABGR32.blue(source)) / 3;
                    int red = tintR * shade / 255;
                    int green = tintG * shade / 255;
                    int blue = tintB * shade / 255;
                    target.blendPixel(x, y, FastColor.ABGR32.color(alpha, blue, green, red));
                }
            }
        }
    }

    private static int samplePalette(Minecraft minecraft, ResourceLocation texture, String xValue, String yValue, int fallbackArgb) {
        try (NativeImage palette = readTexture(minecraft, texture)) {
            if (palette == null) {
                return FastColor.ABGR32.fromArgb32(fallbackArgb);
            }
            double xNorm = clamp(parseDouble(xValue, 0.2D), 0.0D, 1.0D);
            double yNorm = clamp(parseDouble(yValue, 0.16D), 0.0D, 1.0D);
            int x = clamp((int) Math.round(xNorm * (palette.getWidth() - 1)), 0, palette.getWidth() - 1);
            int y = clamp((int) Math.round(yNorm * (palette.getHeight() - 1)), 0, palette.getHeight() - 1);
            int sampled = palette.getPixelRGBA(x, y);
            return FastColor.ABGR32.alpha(sampled) == 0 ? FastColor.ABGR32.fromArgb32(fallbackArgb) : sampled;
        }
    }

    private static NativeImage readTexture(Minecraft minecraft, ResourceLocation texture) {
        Optional<Resource> resource = minecraft.getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            return null;
        }
        try (InputStream stream = resource.get().open()) {
            return NativeImage.read(stream);
        } catch (IOException exception) {
            OriginsSecundus.LOGGER.warn("Failed to read editor texture {}", texture, exception);
            return null;
        }
    }

    private static ResourceLocation texture(String gender, String folder, int index) {
        return editorTexture("human/" + gender + "/" + folder + "/" + fileName(gender, folder, index));
    }

    private static ResourceLocation texture(Map<String, String> values, String gender, String folder, int index) {
        return editorTexture(assetRoot(values) + "/" + gender + "/" + folder + "/" + fileName(gender, folder, index));
    }

    private static ResourceLocation textureForChoice(String gender, String folder, int choice) {
        if (hasNoneChoice(folder) && choice <= 0) {
            return null;
        }
        int index = switch (folder) {
            case "hair", "eyelashes", "beard" -> choice - 1;
            default -> choice;
        };
        return texture(gender, folder, index);
    }

    private static ResourceLocation textureForChoice(Map<String, String> values, String gender, String folder, int choice) {
        if (hasNoneChoice(folder) && choice <= 0) {
            return null;
        }
        int index = switch (folder) {
            case "hair", "eyelashes", "beard" -> choice - 1;
            default -> choice;
        };
        return texture(values, gender, folder, index);
    }

    private static int firstTextureIndex(String folder) {
        return switch (folder) {
            case "clothes" -> 0;
            case "skin", "scars", "eyebrows", "tattoo" -> 1;
            default -> 0;
        };
    }

    private static String fileName(String gender, String folder, int index) {
        return switch (folder) {
            case "skin" -> "skin_" + index;
            case "scars" -> "scar_" + index;
            case "tattoo" -> "tattoo_" + index;
            case "eyebrows" -> "brows_" + index;
            case "eyelashes" -> "eyelashes_" + index;
            case "beard" -> "beard_" + index;
            case "hair" -> MALE.equals(gender) ? "hair_" + index : Integer.toString(index);
            default -> Integer.toString(index);
        };
    }

    private static ResourceLocation namedTexture(String gender, String folder, String name) {
        return editorTexture("human/" + gender + "/" + folder + "/" + name);
    }

    private static ResourceLocation namedTexture(Map<String, String> values, String gender, String folder, String name) {
        return editorTexture(assetRoot(values) + "/" + gender + "/" + folder + "/" + name);
    }

    private static ResourceLocation colormap(Map<String, String> values, String name) {
        return editorTexture(assetRoot(values) + "/colormap/" + name);
    }

    private static int wingsMaxIndex(Minecraft minecraft, Map<String, String> values) {
        int last = 0;
        for (int index = 1; index < 128; index++) {
            if (!resourceExists(minecraft, editorTexture(assetRoot(values) + "/wings/textures/" + index))) {
                break;
            }
            last = index;
        }
        return Math.max(1, last);
    }

    private static String assetRoot(Map<String, String> values) {
        return "avian".equals(values.get(ORIGIN)) ? "avian" : "human";
    }

    private static ResourceLocation dynamicSkinTexture(Map<String, String> values) {
        return OriginsSecundus.id("dynamic/" + assetRoot(values) + "_editor_skin");
    }

    private static boolean resourceExists(Minecraft minecraft, ResourceLocation texture) {
        return minecraft != null && minecraft.getResourceManager().getResource(texture).isPresent();
    }

    private static ResourceLocation editorTexture(String path) {
        return OriginsSecundus.id("textures/editor/" + path + ".png");
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double scaleValue(Map<String, String> values, String key, double min, double max) {
        return clamp(parseDouble(values.get(key), 1.0D), min, max);
    }

    private static double minScale(String key) {
        if (WIDTH.equals(key)) {
            return 0.93D;
        }
        if (DEPTH.equals(key)) {
            return 0.92D;
        }
        return 0.94D;
    }

    private static double maxScale(String key) {
        if (HEIGHT.equals(key)) {
            return 1.05D;
        }
        return 1.06D;
    }

    private static float offsetScale(Map<String, String> values, String key, float min, float max) {
        int offset = offsetValue(values, key);
        if (offset < 0) {
            return 1.0F + (1.0F - min) * offset / 5.0F;
        }
        return 1.0F + (max - 1.0F) * offset / 5.0F;
    }

    private static int adjustColor(int abgr, int offset, double darkMultiplier, double lightMultiplier) {
        double multiplier;
        if (offset < 0) {
            multiplier = 1.0D + (1.0D - darkMultiplier) * offset / 5.0D;
        } else {
            multiplier = 1.0D + (lightMultiplier - 1.0D) * offset / 5.0D;
        }
        int alpha = FastColor.ABGR32.alpha(abgr);
        int red = clamp((int) Math.round(FastColor.ABGR32.red(abgr) * multiplier), 0, 255);
        int green = clamp((int) Math.round(FastColor.ABGR32.green(abgr) * multiplier), 0, 255);
        int blue = clamp((int) Math.round(FastColor.ABGR32.blue(abgr) * multiplier), 0, 255);
        return FastColor.ABGR32.color(alpha, blue, green, red);
    }

    private static int vividEyeColor(int abgr) {
        int alpha = FastColor.ABGR32.alpha(abgr);
        int red = FastColor.ABGR32.red(abgr);
        int green = FastColor.ABGR32.green(abgr);
        int blue = FastColor.ABGR32.blue(abgr);
        double gray = red * 0.299D + green * 0.587D + blue * 0.114D;
        double saturation = 1.45D;
        double brightness = 1.30D;
        int vividRed = clamp((int) Math.round((gray + (red - gray) * saturation) * brightness), 0, 255);
        int vividGreen = clamp((int) Math.round((gray + (green - gray) * saturation) * brightness), 0, 255);
        int vividBlue = clamp((int) Math.round((gray + (blue - gray) * saturation) * brightness), 0, 255);
        return FastColor.ABGR32.color(alpha, vividBlue, vividGreen, vividRed);
    }
}
