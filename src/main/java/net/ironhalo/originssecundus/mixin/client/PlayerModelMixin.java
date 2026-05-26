package net.ironhalo.originssecundus.mixin.client;

import net.ironhalo.originssecundus.client.ClientCustomSkin;
import net.ironhalo.originssecundus.client.EditorPreviewPlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
    private PlayerModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void originssecundus$scaleCustomLimbs(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback
    ) {
        float armMuscle = 1.0F;
        float legMuscle = 1.0F;
        float armLength = 1.0F;
        float legLength = 1.0F;
        if (entity instanceof EditorPreviewPlayer previewPlayer) {
            armMuscle = previewPlayer.armMuscleScale();
            legMuscle = previewPlayer.legMuscleScale();
            armLength = previewPlayer.armLengthScale();
            legLength = previewPlayer.legLengthScale();
        } else if (entity instanceof Player player && ClientCustomSkin.shouldForceOverlays(player)) {
            armMuscle = ClientCustomSkin.armMuscleScaleFor(player);
            legMuscle = ClientCustomSkin.legMuscleScaleFor(player);
            armLength = ClientCustomSkin.armLengthScaleFor(player);
            legLength = ClientCustomSkin.legLengthScaleFor(player);
        }

        this.leftArm.xScale = armMuscle;
        this.rightArm.xScale = armMuscle;
        this.leftArm.zScale = armMuscle;
        this.rightArm.zScale = armMuscle;
        this.leftArm.yScale = armLength;
        this.rightArm.yScale = armLength;
        this.leftLeg.xScale = legMuscle;
        this.rightLeg.xScale = legMuscle;
        this.leftLeg.zScale = legMuscle;
        this.rightLeg.zScale = legMuscle;
        this.leftLeg.yScale = legLength;
        this.rightLeg.yScale = legLength;

        PlayerModel<?> playerModel = (PlayerModel<?>) (Object) this;
        playerModel.leftSleeve.copyFrom(this.leftArm);
        playerModel.rightSleeve.copyFrom(this.rightArm);
        playerModel.leftPants.copyFrom(this.leftLeg);
        playerModel.rightPants.copyFrom(this.rightLeg);
    }
}
