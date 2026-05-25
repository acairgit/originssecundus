package net.ironhalo.originssecundus.power;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.ironhalo.originssecundus.OriginsSecundus;
import net.ironhalo.originssecundus.data.OriginDataManager;
import net.ironhalo.originssecundus.data.OriginDefinition;
import net.ironhalo.originssecundus.data.PowerDefinition;
import net.ironhalo.originssecundus.origin.PlayerOrigin;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class PowerEngine {
    private static final ResourceLocation CUSTOM_HEIGHT_MODIFIER = OriginsSecundus.id("customization/height");

    private PowerEngine() {
    }

    public static void tick(ServerPlayer player) {
        Optional<OriginDefinition> selected = PlayerOrigin.selectedOrigin(player);
        clearLoadedAttributeModifiers(player);
        clearCustomizationModifiers(player);
        if (selected.isEmpty()) {
            return;
        }

        applyCustomization(player);
        List<PowerDefinition> powers = OriginDataManager.powersFor(selected.get());
        for (PowerDefinition power : powers) {
            if (power.is("attribute")) {
                applyAttribute(player, power);
            } else if (power.is("status_effect")) {
                applyStatusEffect(player, power);
            } else if (power.is("effect_immunity")) {
                applyEffectImmunity(player, power);
            } else if (power.is("damage_over_time")) {
                applyDamageOverTime(player, power);
            } else if (power.is("fire_immunity")) {
                player.clearFire();
            } else if (power.is("climbing")) {
                applySimpleClimbing(player);
            }
        }
    }

    public static void modifyIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        PlayerOrigin.selectedOrigin(player).ifPresent(origin -> {
            for (PowerDefinition power : OriginDataManager.powersFor(origin)) {
                if (power.is("fire_immunity") && matchesDamage(event.getSource(), "fire")) {
                    event.setAmount(0.0F);
                } else if (power.is("damage_immunity") && matchesDamage(event.getSource(), GsonHelper.getAsString(power.data(), "damage", ""))) {
                    event.setAmount(0.0F);
                } else if (power.is("damage_modifier") && matchesDamage(event.getSource(), GsonHelper.getAsString(power.data(), "damage", ""))) {
                    event.setAmount((float) (event.getAmount() * GsonHelper.getAsDouble(power.data(), "multiplier", 1.0D)));
                }
            }
        });
    }

    public static void modifyOutgoingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        PlayerOrigin.selectedOrigin(attacker).ifPresent(origin -> {
            for (PowerDefinition power : OriginDataManager.powersFor(origin)) {
                if (power.is("damage_dealt_modifier") && matchesCondition(attacker, GsonHelper.getAsString(power.data(), "condition", ""))) {
                    event.setAmount((float) (event.getAmount() * GsonHelper.getAsDouble(power.data(), "multiplier", 1.0D)));
                }
            }
        });
    }

    public static void activate(ServerPlayer player, String key) {
        if (!"primary".equals(key)) {
            return;
        }
        PlayerOrigin.selectedOrigin(player).ifPresent(origin -> {
            for (PowerDefinition power : OriginDataManager.powersFor(origin)) {
                if (power.is("active_launch")) {
                    launch(player, power);
                    return;
                }
            }
        });
    }

    private static void launch(ServerPlayer player, PowerDefinition power) {
        String cooldownKey = "OriginsSecundusCooldown_" + power.id().toString();
        long now = player.level().getGameTime();
        long readyAt = player.getPersistentData().getLong(cooldownKey);
        if (readyAt > now) {
            long seconds = Math.max(1L, (readyAt - now) / 20L);
            player.displayClientMessage(Component.literal("Power cooldown: " + seconds + "s"), true);
            return;
        }
        double velocity = GsonHelper.getAsDouble(power.data(), "velocity", 1.5D);
        int cooldown = GsonHelper.getAsInt(power.data(), "cooldown_ticks", 600);
        player.push(0.0D, velocity, 0.0D);
        player.hurtMarked = true;
        player.getPersistentData().putLong(cooldownKey, now + cooldown);
    }

    private static void applyAttribute(ServerPlayer player, PowerDefinition power) {
        Holder<Attribute> attribute = resolveAttribute(GsonHelper.getAsString(power.data(), "attribute", ""));
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        ResourceLocation modifierId = modifierId(power);
        AttributeModifier modifier = new AttributeModifier(
                modifierId,
                GsonHelper.getAsDouble(power.data(), "amount", 0.0D),
                parseOperation(GsonHelper.getAsString(power.data(), "operation", "add_value"))
        );
        instance.addOrUpdateTransientModifier(modifier);
        if (attribute == Attributes.MAX_HEALTH && player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void applyCustomization(ServerPlayer player) {
        double height = PlayerOrigin.customization(player).containsKey("height")
                ? parseHeight(PlayerOrigin.customization(player).get("height"))
                : 1.0D;
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale != null && Math.abs(height - 1.0D) > 0.001D) {
            scale.addOrUpdateTransientModifier(new AttributeModifier(
                    CUSTOM_HEIGHT_MODIFIER,
                    height - 1.0D,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    private static void clearCustomizationModifiers(ServerPlayer player) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.removeModifier(CUSTOM_HEIGHT_MODIFIER);
        }
    }

    private static double parseHeight(String rawHeight) {
        try {
            return Math.max(0.85D, Math.min(1.15D, Double.parseDouble(rawHeight)));
        } catch (NumberFormatException ignored) {
            return 1.0D;
        }
    }

    private static void clearLoadedAttributeModifiers(ServerPlayer player) {
        for (PowerDefinition power : OriginDataManager.allPowers()) {
            if (!power.is("attribute")) {
                continue;
            }
            Holder<Attribute> attribute = resolveAttribute(GsonHelper.getAsString(power.data(), "attribute", ""));
            if (attribute == null) {
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(modifierId(power));
            }
        }
    }

    private static void applyStatusEffect(ServerPlayer player, PowerDefinition power) {
        if (!matchesCondition(player, GsonHelper.getAsString(power.data(), "condition", ""))) {
            return;
        }
        if (power.data().has("effects")) {
            JsonArray effects = GsonHelper.getAsJsonArray(power.data(), "effects");
            for (JsonElement element : effects) {
                applyOneEffect(player, element.getAsJsonObject());
            }
            return;
        }
        applyOneEffect(player, power.data());
    }

    private static void applyOneEffect(ServerPlayer player, JsonObject data) {
        ResourceLocation effectId = ResourceLocation.parse(GsonHelper.getAsString(data, "effect", "minecraft:luck"));
        Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.getHolder(effectId);
        effect.ifPresent(holder -> player.addEffect(new MobEffectInstance(
                holder,
                GsonHelper.getAsInt(data, "duration", 60),
                GsonHelper.getAsInt(data, "amplifier", 0),
                true,
                GsonHelper.getAsBoolean(data, "show_particles", false),
                GsonHelper.getAsBoolean(data, "show_icon", true)
        )));
    }

    private static void applyEffectImmunity(ServerPlayer player, PowerDefinition power) {
        if (!power.data().has("effects")) {
            return;
        }
        JsonArray effects = GsonHelper.getAsJsonArray(power.data(), "effects");
        for (JsonElement element : effects) {
            ResourceLocation id = ResourceLocation.parse(element.getAsString());
            BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(player::removeEffect);
        }
    }

    private static void applyDamageOverTime(ServerPlayer player, PowerDefinition power) {
        if (!matchesCondition(player, GsonHelper.getAsString(power.data(), "condition", ""))) {
            return;
        }
        int interval = GsonHelper.getAsInt(power.data(), "interval_ticks", 40);
        if (interval <= 0 || player.tickCount % interval != 0) {
            return;
        }
        player.hurt(player.damageSources().generic(), GsonHelper.getAsFloat(power.data(), "amount", 1.0F));
    }

    private static void applySimpleClimbing(ServerPlayer player) {
        if (player.horizontalCollision && player.zza > 0.0F && !player.isShiftKeyDown()) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.22D, player.getDeltaMovement().z);
            player.resetFallDistance();
        }
    }

    private static boolean matchesCondition(Player player, String condition) {
        return switch (condition) {
            case "", "always" -> true;
            case "not_sneaking" -> !player.isShiftKeyDown();
            case "in_water" -> player.isInWaterOrBubble();
            case "not_in_water" -> !player.isInWaterOrBubble();
            case "touching_water" -> player.isInWaterRainOrBubble();
            case "low_ceiling" -> !player.level().noCollision(player, player.getBoundingBox().move(0.0D, 1.35D, 0.0D));
            case "on_fire" -> player.isOnFire();
            case "sprinting" -> player.isSprinting();
            case "fall_flying" -> player.isFallFlying();
            default -> false;
        };
    }

    private static boolean matchesDamage(DamageSource source, String damage) {
        return switch (damage) {
            case "fire" -> source.is(DamageTypeTags.IS_FIRE);
            case "fall" -> source.is(DamageTypeTags.IS_FALL);
            case "kinetic" -> source.is(DamageTypeTags.IS_FALL) || source.getMsgId().contains("flyIntoWall");
            case "water" -> source.getMsgId().contains("drown");
            case "" -> false;
            default -> source.getMsgId().equals(damage);
        };
    }

    private static Holder<Attribute> resolveAttribute(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "minecraft:generic.movement_speed", "generic.movement_speed", "movement_speed" -> Attributes.MOVEMENT_SPEED;
            case "minecraft:generic.max_health", "generic.max_health", "max_health" -> Attributes.MAX_HEALTH;
            case "minecraft:generic.armor", "generic.armor", "armor" -> Attributes.ARMOR;
            case "minecraft:generic.attack_damage", "generic.attack_damage", "attack_damage" -> Attributes.ATTACK_DAMAGE;
            case "minecraft:generic.scale", "generic.scale", "scale" -> Attributes.SCALE;
            default -> null;
        };
    }

    private static AttributeModifier.Operation parseOperation(String operation) {
        return switch (operation.toLowerCase(Locale.ROOT)) {
            case "add_multiplied_base", "multiply_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total", "multiply_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    private static ResourceLocation modifierId(PowerDefinition power) {
        return OriginsSecundus.id("power/" + power.id().getNamespace() + "/" + power.id().getPath().replace('/', '_'));
    }
}
