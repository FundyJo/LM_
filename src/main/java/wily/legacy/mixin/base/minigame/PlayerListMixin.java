package wily.legacy.mixin.base.minigame;

import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.minigame.AbstractMinigameController;
import wily.legacy.minigame.IMinecraftServer;
import wily.legacy.minigame.MinigamesController;
import wily.legacy.minigame.SpawnPosition;

import java.util.Set;

/**
 * Setzt Spieler-Spawn-Position für Minigame-Lobby direkt beim Platzieren.
 * Liest die Spawn-Positionen aus dem aktiven Controller.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("MinigamePlayerList");

    @Shadow
    @Final
    private MinecraftServer server;

    /**
     * Teleportiert den Spieler zur korrekten Minigame-Spawn-Position NACH dem vollständigen Platzieren.
     * Dies ist notwendig weil Minecraft's teleport() in placeNewPlayer die Position überschreibt.
     */
    @Inject(
        method = "placeNewPlayer",
        at = @At("TAIL")
    )
    private void legacy$onPlaceNewPlayerEnd(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        // Prüfe ob es ein Minigame-Server ist
        if (!(server instanceof IMinecraftServer minigameServer)) {
            return;
        }

        if (!minigameServer.isMinigameServer()) {
            return;
        }

        // Hole die Lobby (Overworld)
        ServerLevel lobbyLevel = server.getLevel(Level.OVERWORLD);
        if (lobbyLevel == null) {
            LOGGER.error("❌ Lobby level (Overworld) not found!");
            return;
        }

        // Hole den aktiven Controller
        MinigamesController controller = MinigamesController.getMinigameController(lobbyLevel);
        if (controller == null || !controller.isActive()) {
            LOGGER.warn("⚠️ No active minigame controller found, using default spawn");
            return;
        }

        // Hole den aktuellen Minigame-Controller
        AbstractMinigameController<?> activeController = controller.getActiveController();
        if (activeController == null) {
            LOGGER.warn("⚠️ No active controller, using default spawn");
            return;
        }

        // Zähle aktuelle Spieler für die Spawn-Index-Berechnung (ohne den neuen Spieler)
        ServerPlayer[] existingPlayers = controller.getPlayersFor(lobbyLevel);
        int playerIndex = 0;
        for (int i = 0; i < existingPlayers.length; i++) {
            if (existingPlayers[i].getUUID().equals(player.getUUID())) {
                playerIndex = i;
                break;
            }
        }

        // Hole Spawn-Position für diesen Spieler-Index aus dem Controller
        SpawnPosition spawn = activeController.getSpawnPosition(playerIndex);

        LOGGER.info("🎮 Teleporting player {} to minigame spawn (index {}): {}, {}, {} (yaw: {})",
            player.getName().getString(), playerIndex, spawn.x(), spawn.y(), spawn.z(), spawn.yaw());

        // Teleportiere den Spieler zur korrekten Position
        // Wir müssen teleportTo verwenden, da der Spieler bereits platziert wurde
        player.teleportTo(lobbyLevel, spawn.x(), spawn.y(), spawn.z(), Set.of(), spawn.yaw(), spawn.pitch(), true);

        // Inventar leeren
        player.getInventory().clearContent();

        // Setze volle Gesundheit und Hunger
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0f);

        LOGGER.info("✅ Player {} spawned at minigame position from controller: {}",
            player.getName().getString(), activeController.getMinigame().getName());
    }
}

