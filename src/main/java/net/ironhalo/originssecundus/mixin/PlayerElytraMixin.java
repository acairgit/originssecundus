package net.ironhalo.originssecundus.mixin;

import net.ironhalo.originssecundus.power.PowerEngine;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerElytraMixin {
    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void originssecundus$tryStartOriginElytra(CallbackInfoReturnable<Boolean> callback) {
        Player player = (Player) (Object) this;
        if (PowerEngine.hasPower(player, "elytra")
                && !player.onGround()
                && !player.isFallFlying()
                && !player.isInWater()
                && !player.hasEffect(MobEffects.LEVITATION)) {
            player.startFallFlying();
            callback.setReturnValue(true);
        }
    }
}
