package wily.legacy.minigame;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.network.S2CPlaySoundPayload;

import java.util.*;

/**
 * Controller für das Battle Minigame
 */
public class BattleMinigameController extends AbstractMinigameController<BattleMinigameController> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleMinigameController.class);

    private static final int END_GAME_COUNTDOWN_TICKS = 200; // 10 Sekunden (200 ticks)

    private int gameTime = 0;
    private boolean gameStarted = false;
    private boolean gameEnded = false;
    private int endGameCountdown = -1;
    private UUID winnerId = null;
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Map<UUID, Integer> playerKills = new HashMap<>();

    public BattleMinigameController(MinigamesController controller) {
        super(controller);
    }

    @Override
    public Minigame<BattleMinigameController> getMinigame() {
        return Minigame.BATTLE;
    }

    // Battle Spawn Konfiguration - alle Spieler spawnen am gleichen Ort (Arena-Mitte)
    private static final SpawnPosition.SpawnPositions BATTLE_SPAWNS = SpawnPosition.SpawnPositions.builder()
        .universal(0.5, 64.0, 0.5, 0.0f)
        .build();

    @Override
    public SpawnPosition.SpawnPositions getSpawnPositions() {
        return BATTLE_SPAWNS;
    }

    @Override
    public MinigameGamerules getGamerules() {
        return MinigameGamerules.BATTLE;
    }

    @Override
    public void sendToMap(RegistryAccess access, ServerPlayer player, boolean late) {
        // Registriere Spieler als "am Leben"
        alivePlayers.add(player.getUUID());
        playerKills.put(player.getUUID(), 0);
    }

    @Override
    public void tick() {
        // Wenn das Spiel beendet ist, zähle den Countdown runter
        if (gameEnded) {
            tickEndGameCountdown();
            return;
        }

        if (!gameStarted) return;

        gameTime++;

        // Spiel-Timer Logic - zeige jede Minute
        if (gameTime % (20 * 60) == 0) {
            int minutes = gameTime / (20 * 60);
            broadcastMessage(Component.literal("§eSpielzeit: " + minutes + " Minuten"));
        }

        // Prüfe Gewinnbedingungen
        checkWinCondition();
    }

    private void tickEndGameCountdown() {
        if (endGameCountdown < 0) return;

        // Zeige Countdown jede Sekunde
        if (endGameCountdown % 20 == 0) {
            int secondsLeft = endGameCountdown / 20;

            for (ServerPlayer player : controller.getPlayersFor(controller.getLevel())) {
                sendTopMessage(player, Component.literal("§fReturning to lobby in " + secondsLeft + " seconds..."));
            }

            // Spiele Sound in den letzten 5 Sekunden
            if (secondsLeft <= 5 && secondsLeft >= 1) {
                for (ServerPlayer player : controller.getPlayersFor(controller.getLevel())) {
                    CommonNetwork.sendToPlayer(player, S2CPlaySoundPayload.of(
                        MinigameSounds.LOBBY_COUNTDOWN,
                        1.0f
                    ));
                }
            }
        }

        // Countdown abgelaufen - teleportiere alle zur Lobby
        if (endGameCountdown <= 0) {
            teleportAllToLobby();
        }

        endGameCountdown--;
    }

    private void teleportAllToLobby() {
        ServerLevel level = controller.getLevel();
        if (level == null) return;

        MinecraftServer server = level.getServer();
        ServerLevel lobbyLevel = server.getLevel(Level.OVERWORLD);

        if (lobbyLevel == null) {
            LOGGER.error("❌ Lobby level (Overworld) not found!");
            return;
        }

        LOGGER.info("🔄 Teleporting all players back to lobby...");

        // === AKTIVIERE LOBBY-CONTROLLER ZUERST ===
        // Wichtig: Der Lobby-Controller muss VOR dem Teleport aktiv gesetzt werden!
        MinigamesController lobbyController = MinigamesController.getMinigameController(lobbyLevel);
        LobbyMinigameController lobbyMinigameController = null;
        if (lobbyController != null) {
            // Setze Lobby als aktives Minigame - das aktiviert den LobbyMinigameController
            lobbyMinigameController = lobbyController.setActiveMinigame(Minigame.LOBBY);

            if (lobbyMinigameController != null) {
                LOGGER.info("✅ Lobby controller activated");
            } else {
                LOGGER.error("❌ Failed to activate lobby controller!");
            }
        } else {
            LOGGER.error("❌ No MinigamesController found for lobby level!");
        }

        // === LOBBY RESET ===
        // Setzt die Lobby-Welt zurück, indem alle Chunks entladen werden.
        // Beim nächsten Betreten werden sie frisch von der Festplatte geladen.
        if (lobbyLevel instanceof IResettableLevel resettableLobby) {
            LOGGER.info("🔄 Resetting lobby level...");
            resettableLobby.legacy$reset();
            LOGGER.info("✅ Lobby level reset.");
        } else {
            LOGGER.warn("⚠️ Lobby level does not implement IResettableLevel, cannot reset!");
        }

        for (ServerPlayer player : controller.getPlayersFor(level)) {
            SpawnPosition spawn;

            // Sieger bekommt den Winner-Spawn
            if (winnerId != null && player.getUUID().equals(winnerId)) {
                spawn = LobbyMinigameController.getWinnerSpawn();
                LOGGER.info("🏆 Winner {} teleporting to winner spawn", player.getName().getString());
            } else {
                // Alle anderen bekommen einen zufälligen Spawn
                spawn = LobbyMinigameController.getRandomSpawn();
            }

            // Teleportiere zur Lobby
            player.teleportTo(lobbyLevel, spawn.x(), spawn.y(), spawn.z(),
                Set.of(), spawn.yaw(), spawn.pitch(), true);

            // Sync controller state to player after teleport (so client has correct state for new level)
            if (lobbyController != null) {
                lobbyController.syncToPlayer(player);
            }

            // Setze Spielmodus auf Adventure
            player.setGameMode(GameType.ADVENTURE);

            // Leere Inventar
            player.getInventory().clearContent();

            // Heile Spieler
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0f);

            LOGGER.info("✅ Teleported {} to lobby", player.getName().getString());
        }

        // Reset Battle-Controller für nächste Runde
        resetGame();

        // Setze den Lobby-Zustand zurück für neue Runde
        if (lobbyMinigameController != null) {
            lobbyMinigameController.resetLobbyState();
            LOGGER.info("✅ Lobby controller reset for new round");
        }

        LOGGER.info("✅ All players returned to lobby");
    }

    private void checkWinCondition() {
        if (alivePlayers.size() <= 1 && !gameEnded) {
            endGame();
        }
    }

    private void endGame() {
        gameStarted = false;
        gameEnded = true;
        endGameCountdown = END_GAME_COUNTDOWN_TICKS;

        if (alivePlayers.size() == 1) {
            winnerId = alivePlayers.iterator().next();

            // Finde den Sieger-Spieler
            ServerPlayer winner = findPlayerByUUID(winnerId);
            String winnerName = winner != null ? winner.getName().getString() : "Unknown";
            int kills = playerKills.getOrDefault(winnerId, 0);

            LOGGER.info("🏆 Winner: {} with {} kills", winnerName, kills);

            // Broadcast Gewinner-Nachricht
            broadcastMessage(Component.literal(""));
            broadcastMessage(Component.literal("§6§l★ ★ ★ GEWINNER ★ ★ ★"));
            broadcastMessage(Component.literal("§e" + winnerName + " §fhat die Runde gewonnen!"));
            broadcastMessage(Component.literal("§7Kills: §f" + kills));
            broadcastMessage(Component.literal(""));

            // Sieger bekommt Feuerwerk-Effekt (optional)
            if (winner != null) {
                winner.sendSystemMessage(Component.literal("§a§lDu hast gewonnen!"));
            }
        } else {
            winnerId = null;
            LOGGER.info("⚔️ Game ended with no winner (draw)");
            broadcastMessage(Component.literal("§c§lUnentschieden! Keine Überlebenden."));
        }

        broadcastMessage(Component.literal("§7Zurück zur Lobby in 10 Sekunden..."));
    }

    private ServerPlayer findPlayerByUUID(UUID uuid) {
        if (controller.getLevel() == null) return null;
        for (ServerPlayer player : controller.getPlayersFor(controller.getLevel())) {
            if (player.getUUID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    private void resetGame() {
        gameTime = 0;
        gameStarted = false;
        gameEnded = false;
        endGameCountdown = -1;
        winnerId = null;
        alivePlayers.clear();
        playerKills.clear();
        LOGGER.info("🔄 Battle game reset");
    }

    private void broadcastMessage(Component message) {
        if (controller.getLevel() == null) return;
        for (ServerPlayer player : controller.getPlayersFor(controller.getLevel())) {
            player.sendSystemMessage(message);
        }
    }

    public void startGame() {
        gameStarted = true;
        gameEnded = false;
        gameTime = 0;
        endGameCountdown = -1;
        broadcastMessage(Component.literal("§a§l⚔ BATTLE BEGINNT! ⚔"));
        LOGGER.info("⚔️ Battle started with {} players", alivePlayers.size());
    }

    /**
     * Wird aufgerufen wenn ein Spieler stirbt
     */
    public void playerDied(ServerPlayer player) {
        if (!alivePlayers.contains(player.getUUID())) return;

        alivePlayers.remove(player.getUUID());

        int remaining = alivePlayers.size();
        player.sendSystemMessage(Component.literal("§c§lDu bist ausgeschieden!"));
        broadcastMessage(Component.literal("§c" + player.getName().getString() + " §7wurde eliminiert! §f(" + remaining + " verbleibend)"));

        LOGGER.info("💀 Player {} eliminated, {} remaining", player.getName().getString(), remaining);

        // Setze Spieler in Spectator-Modus
        player.setGameMode(GameType.SPECTATOR);

        checkWinCondition();
    }

    /**
     * Wird aufgerufen wenn ein Spieler einen anderen tötet
     */
    public void playerKill(ServerPlayer killer, ServerPlayer victim) {
        playerKills.merge(killer.getUUID(), 1, Integer::sum);
        int kills = playerKills.get(killer.getUUID());

        killer.sendSystemMessage(Component.literal("§a+" + kills + " Kill!"));

        playerDied(victim);
    }

    /**
     * Prüft ob ein Spieler noch am Leben ist
     */
    public boolean isPlayerAlive(ServerPlayer player) {
        return alivePlayers.contains(player.getUUID());
    }

    /**
     * Gibt die Anzahl der verbleibenden Spieler zurück
     */
    public int getRemainingPlayers() {
        return alivePlayers.size();
    }

    /**
     * Gibt die Kills eines Spielers zurück
     */
    public int getPlayerKills(ServerPlayer player) {
        return playerKills.getOrDefault(player.getUUID(), 0);
    }

    @Override
    public boolean pvpEnabled() {
        return true;
    }

    @Override
    public boolean isSmallInventory() {
        return false;
    }

    @Override
    public boolean canAcceptNewPlayers() {
        return !gameStarted && !gameEnded;
    }

    @Override
    public boolean allowDamage() {
        return gameStarted && !gameEnded;
    }
}
