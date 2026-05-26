package net.ironhalo.originssecundus.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;

public final class EditorPreviewPlayer extends RemotePlayer {
    private PlayerSkin editorSkin;
    private float editorScaleX = 1.0F;
    private float editorScaleY = 1.0F;
    private float editorScaleZ = 1.0F;
    private float armMuscleScale = 1.0F;
    private float legMuscleScale = 1.0F;
    private float armLengthScale = 1.0F;
    private float legLengthScale = 1.0F;
    private ResourceLocation editorWingsTexture;

    EditorPreviewPlayer(ClientLevel level, GameProfile profile) {
        super(level, profile);
        this.noPhysics = true;
    }

    void setEditorSkin(PlayerSkin editorSkin) {
        this.editorSkin = editorSkin;
    }

    void setEditorScale(float scaleX, float scaleY, float scaleZ) {
        this.editorScaleX = scaleX;
        this.editorScaleY = scaleY;
        this.editorScaleZ = scaleZ;
    }

    void setEditorLimbScale(float armMuscleScale, float legMuscleScale, float armLengthScale, float legLengthScale) {
        this.armMuscleScale = armMuscleScale;
        this.legMuscleScale = legMuscleScale;
        this.armLengthScale = armLengthScale;
        this.legLengthScale = legLengthScale;
    }

    void setEditorWingsTexture(ResourceLocation editorWingsTexture) {
        this.editorWingsTexture = editorWingsTexture;
    }

    public float editorScaleX() {
        return editorScaleX;
    }

    public float editorScaleY() {
        return editorScaleY;
    }

    public float editorScaleZ() {
        return editorScaleZ;
    }

    public float armMuscleScale() {
        return armMuscleScale;
    }

    public float legMuscleScale() {
        return legMuscleScale;
    }

    public float armLengthScale() {
        return armLengthScale;
    }

    public float legLengthScale() {
        return legLengthScale;
    }

    public ResourceLocation editorWingsTexture() {
        return editorWingsTexture;
    }

    @Override
    public PlayerSkin getSkin() {
        return editorSkin != null ? editorSkin : super.getSkin();
    }

    @Override
    public boolean isModelPartShown(PlayerModelPart part) {
        return editorSkin != null || super.isModelPartShown(part);
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }
}
