package net.ironhalo.originssecundus.mixin.client;

import net.ironhalo.originssecundus.client.ClientCustomSkin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void originssecundus$getSkin(CallbackInfoReturnable<PlayerSkin> callback) {
        PlayerSkin skin = ClientCustomSkin.skinFor((AbstractClientPlayer) (Object) this);
        if (skin != null) {
            callback.setReturnValue(skin);
        }
    }
}
