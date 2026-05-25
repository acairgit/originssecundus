package net.ironhalo.originssecundus;

import net.ironhalo.originssecundus.power.PowerEngine;
import net.ironhalo.originssecundus.origin.PlayerOrigin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
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
            enforceEquipmentRestrictions(player);
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        PowerEngine.modifyIncomingDamage(event);
        PowerEngine.modifyOutgoingDamage(event);
    }

    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && PowerEngine.hasPower(player, "fall_immunity")) {
            event.setDamageMultiplier(0.0F);
            event.setDistance(0.0F);
        }
    }

    @SubscribeEvent
    public void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || event.getItem().get(DataComponents.FOOD) == null) {
            return;
        }
        boolean meat = isMeat(event.getItem());
        if ((PowerEngine.hasPower(player, "vegetarian") && meat) || (PowerEngine.hasPower(player, "carnivore") && !meat)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("Your origin cannot eat this."), true);
        }
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (PowerEngine.hasPower(player, "aqua_affinity") && player.isUnderWater()) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), event.getOriginalSpeed()));
        }
        if (PowerEngine.hasPower(player, "strong_arms") && event.getState().is(BlockTags.BASE_STONE_OVERWORLD)) {
            event.setNewSpeed(event.getNewSpeed() * 3.0F);
        }
        if (PowerEngine.hasPower(player, "weak_arms") && event.getState().is(BlockTags.BASE_STONE_OVERWORLD)) {
            event.setNewSpeed(event.getNewSpeed() * 0.45F);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (!PowerEngine.hasPower(event.getEntity(), "webbing") && !PowerEngine.hasPower(event.getEntity(), "master_of_webs")) {
            return;
        }
        if (event.getTarget() instanceof LivingEntity target && !event.getEntity().level().isClientSide()) {
            BlockPos pos = target.blockPosition();
            if (target.level().getBlockState(pos).isAir()) {
                target.level().setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
            }
        }
    }

    @SubscribeEvent
    public void onCanSleep(CanPlayerSleepEvent event) {
        if (PowerEngine.hasPower(event.getEntity(), "fresh_air") && event.getPos().getY() < 86) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            event.getEntity().displayClientMessage(Component.literal("You need fresh air to sleep."), true);
        }
    }

    private static void enforceEquipmentRestrictions(ServerPlayer player) {
        if (PowerEngine.hasPower(player, "no_shield") && player.getOffhandItem().is(Items.SHIELD)) {
            player.drop(player.getOffhandItem().copy(), false);
            player.getOffhandItem().setCount(0);
        }
        if (!PowerEngine.hasPower(player, "light_armor")) {
            return;
        }
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem armor && armor.getDefense() > 5) {
                player.drop(stack.copy(), false);
                stack.setCount(0);
                player.displayClientMessage(Component.literal("Your origin cannot wear heavy armor."), true);
            }
        }
    }

    private static boolean isMeat(ItemStack stack) {
        String itemId = stack.getItemHolder().unwrapKey().map(key -> key.location().getPath()).orElse("");
        if (itemId.contains("beef") || itemId.contains("pork") || itemId.contains("chicken") || itemId.contains("mutton")
                || itemId.contains("rabbit") || itemId.contains("cod") || itemId.contains("salmon") || itemId.contains("rotten_flesh")) {
            return true;
        }
        return stack.getTags().anyMatch(tag -> {
            String path = tag.location().getPath();
            return path.contains("meat") || path.contains("fish");
        });
    }
}
