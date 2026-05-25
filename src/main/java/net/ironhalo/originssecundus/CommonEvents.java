package net.ironhalo.originssecundus;

import net.ironhalo.originssecundus.power.PowerEngine;
import net.ironhalo.originssecundus.origin.PlayerOrigin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.server.level.ServerPlayer;

public final class CommonEvents {
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerOrigin.syncToClient(player);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        PlayerOrigin.copyPersistentData(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerEngine.tick(player);
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        PowerEngine.modifyIncomingDamage(event);
        PowerEngine.modifyOutgoingDamage(event);
    }
}
