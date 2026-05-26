package net.ironhalo.originssecundus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ironhalo.originssecundus.mixin.client.ElytraModelAccessor;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class AvianWingsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final int PREVIEW_ANIMATION_TICKS = 200;
    private final ElytraModel<AbstractClientPlayer> wingsModel;

    public AvianWingsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.wingsModel = new ElytraModel<>(modelSet.bakeLayer(ModelLayers.ELYTRA));
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        ResourceLocation texture = ClientCustomSkin.wingsTextureFor(player);
        if (texture == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        this.getParentModel().copyPropertiesTo(this.wingsModel);
        this.wingsModel.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        applyPreviewOpening(player, partialTicks);
        VertexConsumer vertices = buffer.getBuffer(RenderType.armorCutoutNoCull(texture));
        this.wingsModel.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private void applyPreviewOpening(AbstractClientPlayer player, float partialTicks) {
        if (!(player instanceof EditorPreviewPlayer)) {
            return;
        }
        float progress = previewOpenAmount(player.tickCount + partialTicks);
        ElytraModelAccessor accessor = (ElytraModelAccessor) this.wingsModel;
        ModelPart left = accessor.originssecundus$leftWing();
        ModelPart right = accessor.originssecundus$rightWing();
        float xRot = Mth.lerp(progress, (float) (Math.PI / 12.0D), (float) (Math.PI / 9.0D));
        float zRot = Mth.lerp(progress, (float) (-Math.PI / 12.0D), (float) (-Math.PI / 2.0D));
        float yRot = Mth.lerp(progress, 0.0F, 0.08F);
        left.xRot = xRot;
        left.yRot = yRot;
        left.zRot = zRot;
        right.xRot = xRot;
        right.yRot = -yRot;
        right.zRot = -zRot;
    }

    private static float previewOpenAmount(float ticks) {
        float cycle = ticks % PREVIEW_ANIMATION_TICKS;
        if (cycle < 25.0F) {
            return smooth(cycle / 25.0F);
        }
        if (cycle < 70.0F) {
            return 1.0F;
        }
        if (cycle < 100.0F) {
            return 1.0F - smooth((cycle - 70.0F) / 30.0F);
        }
        return 0.0F;
    }

    private static float smooth(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
}
