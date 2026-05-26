package net.ironhalo.originssecundus.mixin;

import net.ironhalo.originssecundus.power.PowerEngine;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityElytraMixin extends Entity {
    protected LivingEntityElytraMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void originssecundus$keepOriginElytraFlying(CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player) || !PowerEngine.hasPower(player, "elytra")) {
            return;
        }
        if (this.getSharedFlag(7)
                && !entity.onGround()
                && !entity.isPassenger()
                && !entity.hasEffect(MobEffects.LEVITATION)) {
            if (!entity.level().isClientSide()) {
                this.setSharedFlag(7, true);
            }
            callback.cancel();
        }
    }
}
