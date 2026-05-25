package net.ironhalo.originssecundus.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.ironhalo.originssecundus.data.OriginDataManager;
import net.ironhalo.originssecundus.origin.PlayerOrigin;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class OriginsCommands {
    private OriginsCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("originssecundus")
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getOrigin(context.getSource(), EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("origin", StringArgumentType.word())
                                        .executes(context -> setOrigin(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                StringArgumentType.getString(context, "origin"))))))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> resetOrigin(context.getSource(), EntityArgument.getPlayer(context, "target"))))));
    }

    private static int getOrigin(CommandSourceStack source, ServerPlayer target) {
        String origin = PlayerOrigin.selectedOriginId(target).map(ResourceLocation::toString).orElse("none");
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " origin: " + origin), false);
        return 1;
    }

    private static int setOrigin(CommandSourceStack source, ServerPlayer target, String rawOrigin) {
        ResourceLocation origin = rawOrigin.contains(":")
                ? ResourceLocation.parse(rawOrigin)
                : ResourceLocation.fromNamespaceAndPath("originssecundus", rawOrigin);
        if (OriginDataManager.origin(origin).isEmpty()) {
            source.sendFailure(Component.literal("Unknown origin: " + origin));
            return 0;
        }
        PlayerOrigin.setOrigin(target, origin, OriginDataManager.origin(origin).orElseThrow().defaultCustomizationValues());
        source.sendSuccess(() -> Component.translatable("message.originssecundus.origin_set", target.getGameProfile().getName(), origin.toString()), true);
        return 1;
    }

    private static int resetOrigin(CommandSourceStack source, ServerPlayer target) {
        PlayerOrigin.clear(target);
        source.sendSuccess(() -> Component.translatable("message.originssecundus.origin_reset", target.getGameProfile().getName()), true);
        return 1;
    }
}
