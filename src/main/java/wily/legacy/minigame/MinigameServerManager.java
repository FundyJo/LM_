package wily.legacy.minigame;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zentraler Manager für Minigame-Server.
 *
 * <h2>Verantwortlichkeiten:</h2>
 * <ul>
 *   <li><b>Config Management:</b> Hält die Config bis der Server startet</li>
 *   <li><b>Server Publishing:</b> Published IntegratedServer für LAN-Zugriff</li>
 * </ul>
 *
 * <h2>Ablauf:</h2>
 * <pre>
 * 1. CreateMinigameScreen → setPendingConfig()
 * 2. Minecraft.doWorldLoad() → Server erstellt
 * 3. IntegratedServerMixin.onServerInit() → consumePendingConfig()
 * 4. schedulePublish() → wartet auf minecraft.player
 * 5. tickPublisher() → publishServer() wenn bereit
 * </pre>
 *
 * @see MinigameServerConfig für Konfigurationsoptionen
 * @see MinigameWorldLoader für Welt-Laden
 */
public class MinigameServerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("MinigameServerManager");

    // ==================== CONFIG MANAGEMENT ====================

    private static MinigameServerConfig pendingConfig = null;

    /**
     * Setzt die Config die beim nächsten Server-Start verwendet werden soll
     */
    public static void setPendingConfig(MinigameServerConfig config) {
        LOGGER.info("📝 Setting pending minigame config: {}", config.getMinigameType());
        pendingConfig = config;
    }

    /**
     * Holt und konsumiert die pending Config.
     * Nach dem Aufruf ist pendingConfig wieder null.
     */
    public static MinigameServerConfig consumePendingConfig() {
        MinigameServerConfig config = pendingConfig;
        pendingConfig = null;

        if (config != null) {
            LOGGER.info("✅ Consumed pending config: {}", config.getMinigameType());
        }

        return config;
    }

    /**
     * Prüft ob eine pending Config existiert
     */
    public static boolean hasPendingConfig() {
        return pendingConfig != null;
    }

    /**
     * Holt die pending Config ohne sie zu konsumieren.
     */
    public static MinigameServerConfig getPendingConfig() {
        return pendingConfig;
    }

    /**
     * Löscht die pending Config ohne sie zu konsumieren
     */
    public static void clearPendingConfig() {
        if (pendingConfig != null) {
            LOGGER.info("🗑️ Cleared pending config: {}", pendingConfig.getMinigameType());
            pendingConfig = null;
        }
    }

    // ==================== SERVER PUBLISHING ====================

    private static IntegratedServer pendingServer = null;
    private static boolean publishScheduled = false;

    /**
     * Schedule a server for publishing once minecraft.player is ready.
     */
    public static void schedulePublish(IntegratedServer server) {
        if (server.isPublished()) {
            LOGGER.info("ℹ️ Server is already published on port {}", server.getPort());
            return;
        }

        pendingServer = server;
        publishScheduled = true;
        LOGGER.info("📡 Minigame server publish scheduled - waiting for player connection...");
    }

    /**
     * Called from server tick to check if we can publish.
     */
    public static void tickPublisher() {
        if (!publishScheduled || pendingServer == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        // Wait for player to be ready
        if (minecraft.player == null) {
            return;
        }

        // Player is ready, publish now!
        publishScheduled = false;
        IntegratedServer server = pendingServer;
        pendingServer = null;

        if (server.isPublished()) {
            LOGGER.info("ℹ️ Server was already published on port {}", server.getPort());
            return;
        }

        LOGGER.info("🎮 Publishing Minigame server for LAN...");

        // Publish with port fallback
        int[] portsToTry = {25565, 25566, 25567, 25568, 25569, 25570, 0};
        int publishedPort = -1;

        for (int port : portsToTry) {
            try {
                if (server.publishServer(GameType.ADVENTURE, false, port)) {
                    publishedPort = port == 0 ? server.getPort() : port;
                    LOGGER.info("✅ Minigame server published on port {}", publishedPort);
                    break;
                } else {
                    LOGGER.warn("⚠️ publishServer returned false for port {}", port);
                }
            } catch (Exception e) {
                LOGGER.warn("⚠️ Port {} failed: {}", port, e.getMessage());
            }
        }

        if (publishedPort == -1) {
            LOGGER.warn("⚠️ Could not publish server on any port, running in singleplayer mode");
        }
    }

    /**
     * Cancel any pending publish.
     */
    public static void cancelPublish() {
        publishScheduled = false;
        pendingServer = null;
    }

    // ==================== CLEANUP ====================

    /**
     * Cleanup alles (Config + Publishing)
     */
    public static void cleanup() {
        clearPendingConfig();
        cancelPublish();
    }
}

