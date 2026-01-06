package wily.legacy.minigame;

/**
 * Interface für MinecraftServer (IntegratedServer/DedicatedServer) mit Minigame-Unterstützung.
 *
 * Wird via Mixin in MinecraftServer implementiert.
 *
 * Verwendung:
 * <pre>
 * if (server instanceof IMinecraftServer minigameServer) {
 *     if (minigameServer.isMinigameServer()) {
 *         MinigameServerConfig config = minigameServer.getMinigameServerConfig();
 *         // ...
 *     }
 * }
 * </pre>
 *
 * @see MinigameServerManager für Config-Management
 * @see MinigamesController für Spiellogik
 */
public interface IMinecraftServer {

    /**
     * Prüft ob dieser Server ein Minigame-Server ist.
     */
    boolean isMinigameServer();

    /**
     * Markiert diesen Server als Minigame-Server.
     */
    void setMinigameServer(boolean value);

    /**
     * Maximale Spieleranzahl für diesen Server.
     * @return Max. Spieler, oder -1 wenn kein Limit gesetzt
     */
    int getMaxPlayers();

    /**
     * Setzt die maximale Spieleranzahl.
     */
    void setMaxPlayers(int value);

    /**
     * Gibt die Minigame-Server-Konfiguration zurück.
     * @return Config, oder null wenn kein Minigame-Server
     */
    MinigameServerConfig getMinigameServerConfig();

    /**
     * Setzt die Minigame-Server-Konfiguration (nur einmal während Initialisierung).
     */
    void setMinigameServerConfig(MinigameServerConfig config);

    /**
     * Gibt den Level-Manager für diesen Minigame-Server zurück.
     * @return Manager, oder null wenn kein Minigame-Server
     */
    MinigameLevelManager getMinigameLevelManager();

    /**
     * Setzt den Level-Manager.
     */
    void setMinigameLevelManager(MinigameLevelManager manager);
}
