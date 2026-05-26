package net.ironhalo.originssecundus.client;

import net.ironhalo.originssecundus.OriginsSecundus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientCustomSkin {
    private ClientCustomSkin() {
    }

    public static PlayerSkin skinFor(AbstractClientPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player != player) {
            return null;
        }
        ResourceLocation originId = ClientOriginState.originId().filter(HumanEditorSkin::isEditable).orElse(null);
        if (originId == null) {
            return null;
        }
        Map<String, String> values = new LinkedHashMap<>(ClientOriginState.customization());
        HumanEditorSkin.ensureDefaults(values, originId);
        return HumanEditorSkin.skin(minecraft, originId, values);
    }

    public static boolean shouldForceOverlays(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player == player
                && ClientOriginState.originId().filter(HumanEditorSkin::isEditable).isPresent();
    }

    public static ResourceLocation wingsTextureFor(AbstractClientPlayer player) {
        if (player instanceof EditorPreviewPlayer previewPlayer) {
            return previewPlayer.editorWingsTexture();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player != player) {
            return null;
        }
        ResourceLocation originId = ClientOriginState.originId().orElse(null);
        if (!OriginsSecundus.id("avian").equals(originId)) {
            return null;
        }
        Map<String, String> values = new LinkedHashMap<>(ClientOriginState.customization());
        HumanEditorSkin.ensureDefaults(values, originId);
        return HumanEditorSkin.wingsTexture(values);
    }

    public static float heightScaleFor(Player player) {
        return shouldForceOverlays(player) ? (float) HumanEditorSkin.heightScale(customValues()) : 1.0F;
    }

    public static float widthScaleFor(Player player) {
        return shouldForceOverlays(player) ? (float) HumanEditorSkin.widthScale(customValues()) : 1.0F;
    }

    public static float depthScaleFor(Player player) {
        return shouldForceOverlays(player) ? (float) HumanEditorSkin.depthScale(customValues()) : 1.0F;
    }

    public static float armMuscleScaleFor(Player player) {
        return shouldForceOverlays(player) ? HumanEditorSkin.armMuscleScale(customValues()) : 1.0F;
    }

    public static float legMuscleScaleFor(Player player) {
        return shouldForceOverlays(player) ? HumanEditorSkin.legMuscleScale(customValues()) : 1.0F;
    }

    public static float armLengthScaleFor(Player player) {
        return shouldForceOverlays(player) ? HumanEditorSkin.armLengthScale(customValues()) : 1.0F;
    }

    public static float legLengthScaleFor(Player player) {
        return shouldForceOverlays(player) ? HumanEditorSkin.legLengthScale(customValues()) : 1.0F;
    }

    private static Map<String, String> customValues() {
        Map<String, String> values = new LinkedHashMap<>(ClientOriginState.customization());
        ClientOriginState.originId().ifPresentOrElse(
                originId -> HumanEditorSkin.ensureDefaults(values, originId),
                () -> HumanEditorSkin.ensureDefaults(values)
        );
        return values;
    }
}
