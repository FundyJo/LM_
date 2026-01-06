package wily.legacy.minigame.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import wily.legacy.minigame.*;

/**
 * Commands für Minigame-Management
 */
public class MinigameCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("minigame")
                .requires(source -> source.hasPermission(2)) // OP-Level 2

                // /minigame setup <type> <maxPlayers> <minPlayers>
                .then(Commands.literal("setup")
                    .then(Commands.argument("type", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            builder.suggest("TUMBLE");
                            builder.suggest("BATTLE");
                            builder.suggest("GLIDE");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("maxPlayers", IntegerArgumentType.integer(1, 100))
                            .then(Commands.argument("minPlayers", IntegerArgumentType.integer(1, 100))
                                .executes(MinigameCommands::setupMinigame)
                            )
                        )
                    )
                )

                // /minigame ready
                .then(Commands.literal("ready")
                    .executes(MinigameCommands::toggleReady)
                )

                // /minigame status
                .then(Commands.literal("status")
                    .executes(MinigameCommands::showStatus)
                )
        );
    }

    private static int setupMinigame(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        String type = StringArgumentType.getString(context, "type");
        int maxPlayers = IntegerArgumentType.getInteger(context, "maxPlayers");
        int minPlayers = IntegerArgumentType.getInteger(context, "minPlayers");

        context.getSource().sendSuccess(() ->
            Component.literal("§6Setting up minigame server..."), false);
        context.getSource().sendSuccess(() ->
            Component.literal("§7Type: §f" + type), false);
        context.getSource().sendSuccess(() ->
            Component.literal("§7Max Players: §f" + maxPlayers), false);
        context.getSource().sendSuccess(() ->
            Component.literal("§7Min Players: §f" + minPlayers), false);

        try {
            // Erstelle Config
            MinigameServerConfig config = new MinigameServerConfig(type, maxPlayers, minPlayers);

            // Konfiguriere Server als Minigame-Server
            if (server instanceof IMinecraftServer minigameServer) {
                minigameServer.setMinigameServer(true);
                minigameServer.setMinigameServerConfig(config);
                minigameServer.setMaxPlayers(maxPlayers);

                // Erstelle Level Manager wenn noch nicht vorhanden
                if (minigameServer.getMinigameLevelManager() == null) {
                    MinigameLevelManager levelManager = new MinigameLevelManager(server);
                    minigameServer.setMinigameLevelManager(levelManager);

                    // Registriere Dimensionen
                    levelManager.registerDimension("lobby", net.minecraft.world.level.Level.OVERWORLD, Minigame.LOBBY);
                }

                context.getSource().sendSuccess(() ->
                    Component.literal("§a✅ Minigame server configured!"), false);
                context.getSource().sendSuccess(() ->
                    Component.literal("§7Players will spawn in lobby when joining"), false);
            } else {
                context.getSource().sendFailure(
                    Component.literal("§c❌ Server does not support minigames!"));
            }
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c❌ Failed to setup minigame server: " + e.getMessage()));
            e.printStackTrace();
        }

        return 1;
    }

    private static int toggleReady(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can use this command!"));
            return 0;
        }

        MinecraftServer server = context.getSource().getServer();
        if (!(server instanceof IMinecraftServer minigameServer) || !minigameServer.isMinigameServer()) {
            context.getSource().sendFailure(Component.literal("§cNot a minigame server!"));
            return 0;
        }

        MinigamesController controller = MinigamesController.getMinigameController(player.level());

        // Toggle ready status - in der echten Implementierung würde man den Status tracken
        // Für jetzt simulieren wir einfach "ready"
        controller.playerReady(player, true);

        context.getSource().sendSuccess(() ->
            Component.literal("§aYou are now READY!"), false);

        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();

        if (!(server instanceof IMinecraftServer minigameServer)) {
            context.getSource().sendFailure(Component.literal("§cServer does not support minigames!"));
            return 0;
        }

        boolean isMinigame = minigameServer.isMinigameServer();
        context.getSource().sendSuccess(() ->
            Component.literal("§6=== Minigame Server Status ==="), false);
        context.getSource().sendSuccess(() ->
            Component.literal("§7Minigame Mode: " + (isMinigame ? "§aENABLED" : "§cDISABLED")), false);

        if (isMinigame) {
            var config = minigameServer.getMinigameServerConfig();
            if (config != null) {
                context.getSource().sendSuccess(() ->
                    Component.literal("§7Type: §f" + config.getMinigameType()), false);
                context.getSource().sendSuccess(() ->
                    Component.literal("§7Max Players: §f" + config.getMaxPlayers()), false);
                context.getSource().sendSuccess(() ->
                    Component.literal("§7Min to Start: §f" + config.getMinPlayersToStart()), false);
            }

            var levelManager = minigameServer.getMinigameLevelManager();
            if (levelManager != null) {
                context.getSource().sendSuccess(() ->
                    Component.literal("§7Level Manager: §aInitialized"), false);
            }
        }


        return 1;
    }
}

