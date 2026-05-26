package net.ironhalo.originssecundus.mixin.client;

import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ElytraModel.class)
public interface ElytraModelAccessor {
    @Accessor("leftWing")
    ModelPart originssecundus$leftWing();

    @Accessor("rightWing")
    ModelPart originssecundus$rightWing();
}
