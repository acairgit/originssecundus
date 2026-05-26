package net.ironhalo.originssecundus.mixin.client;

import net.ironhalo.originssecundus.client.ClientCustomSkin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "isModelPartShown", at = @At("HEAD"), cancellable = true)
    private void originssecundus$isModelPartShown(PlayerModelPart part, CallbackInfoReturnable<Boolean> callback) {
        if (ClientCustomSkin.shouldForceOverlays((Player) (Object) this)) {
            callback.setReturnValue(true);
        }
    }
}
