package net.ironhalo.originssecundus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ironhalo.originssecundus.client.AvianWingsLayer;
import net.ironhalo.originssecundus.client.ClientCustomSkin;
import net.ironhalo.originssecundus.client.EditorPreviewPlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void originssecundus$addAvianWingsLayer(
            EntityRendererProvider.Context context,
            boolean useSlimModel,
            CallbackInfo callback
    ) {
        PlayerRenderer renderer = (PlayerRenderer) (Object) this;
        renderer.addLayer(new AvianWingsLayer(renderer, context.getModelSet()));
    }

    @Inject(
            method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void originssecundus$hideEditorPreviewName(
            AbstractClientPlayer player,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float partialTick,
            CallbackInfo callback
    ) {
        if (player instanceof EditorPreviewPlayer) {
            callback.cancel();
        }
    }

    @Inject(
            method = "scale(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
            at = @At("TAIL")
    )
    private void originssecundus$scaleCustomHuman(
            AbstractClientPlayer player,
            PoseStack poseStack,
            float partialTickTime,
            CallbackInfo callback
    ) {
        if (player instanceof EditorPreviewPlayer editorPreviewPlayer) {
            poseStack.scale(
                    editorPreviewPlayer.editorScaleX(),
                    editorPreviewPlayer.editorScaleY(),
                    editorPreviewPlayer.editorScaleZ()
            );
            return;
        }
        if (ClientCustomSkin.shouldForceOverlays(player)) {
            poseStack.scale(
                    ClientCustomSkin.widthScaleFor(player),
                    ClientCustomSkin.heightScaleFor(player),
                    ClientCustomSkin.depthScaleFor(player)
            );
        }
    }
}
