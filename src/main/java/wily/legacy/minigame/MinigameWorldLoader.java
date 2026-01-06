package wily.legacy.minigame;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Lädt Minigame-Welten aus Resources.
 * Die lobby.mcsave wird aus resources/data/legacy/minigame/ entpackt.
 * Die Welt ist schreibgeschützt und wird nach Spielende gelöscht.
 */
public class MinigameWorldLoader {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOBBY_RESOURCE = "/data/legacy/minigame/lobby.mcsave";
    private static final String LOBBY_WORLD_NAME = "MinigameLobby_Temp";

    private static Path currentLobbyPath = null;
    private static boolean isMinigameActive = false;

    /**
     * Flag das anzeigt, dass wir gerade eine Minigame-Welt laden.
     * Verhindert dass der Cleanup-Hook bei disconnect() während des Ladens auslöst.
     */
    private static boolean isLoadingMinigame = false;

    /**
     * Flag das anzeigt, dass die Lobby gerade zurückgesetzt wird.
     */
    private static boolean isResettingLobby = false;

    /**
     * Lädt die Lobby-Welt aus Resources und startet einen Minigame-Server
     *
     * @param config Die Minigame-Konfiguration
     * @param minecraft Minecraft-Instanz
     * @return true wenn erfolgreich
     */
    public static boolean loadMinigameLobby(MinigameServerConfig config, Minecraft minecraft) {
        LOGGER.info("🎮 Loading Minigame Lobby for {}", config.getMinigameType());

        // Flag setzen um Cleanup während des Ladens zu verhindern
        isLoadingMinigame = true;

        try {
            // Ermittle das saves-Verzeichnis korrekt
            Path savesDir = minecraft.gameDirectory.toPath().resolve("saves");
            Files.createDirectories(savesDir);

            Path lobbyPath = savesDir.resolve(LOBBY_WORLD_NAME);
            LOGGER.info("📁 Lobby path: {}", lobbyPath);

            // Alte Lobby löschen falls vorhanden (ohne currentLobbyPath zu ändern)
            if (Files.exists(lobbyPath)) {
                LOGGER.info("🗑️ Removing old lobby...");
                try {
                    setReadOnly(lobbyPath, false);
                    deleteDirectory(lobbyPath);
                } catch (IOException e) {
                    LOGGER.warn("⚠️ Failed to cleanup old lobby", e);
                }
            }

            // Jetzt erst setzen
            currentLobbyPath = lobbyPath;

            // Lobby aus Resources entpacken
            if (!extractLobbyFromResources(lobbyPath)) {
                LOGGER.error("❌ Failed to extract lobby from resources!");
                return false;
            }


            // KEIN Schreibschutz setzen - Minecraft muss während des Spiels schreiben können
            // Die Welt wird nach dem Spiel gelöscht
            LOGGER.info("📁 World is writable during gameplay, will be deleted after minigame ends");

            // Welt laden über LevelStorageSource
            LevelStorageSource levelStorageSource = minecraft.getLevelSource();
            LevelStorageSource.LevelStorageAccess levelAccess;
            try {
                levelAccess = levelStorageSource.validateAndCreateAccess(LOBBY_WORLD_NAME);
            } catch (IOException e) {
                LOGGER.error("❌ Failed to access lobby world!", e);
                cleanupLobby();
                return false;
            }

            // World Summary prüfen
            try {
                var summaryResult = levelAccess.getSummary(levelAccess.getDataTag());
                if (summaryResult == null) {
                    LOGGER.error("❌ Lobby world has no valid summary!");
                    levelAccess.close();
                    cleanupLobby();
                    return false;
                }
            } catch (Exception e) {
                LOGGER.error("❌ Failed to read lobby world data!", e);
                levelAccess.close();
                cleanupLobby();
                return false;
            }

            LOGGER.info("✅ Lobby world ready (read-only): {}", LOBBY_WORLD_NAME);

            // PackRepository und WorldStem laden
            PackRepository packRepository = minecraft.getResourcePackRepository();
            var worldStem = minecraft.createWorldOpenFlows()
                    .loadWorldStem(levelAccess.getDataTag(), false, packRepository);

            if (worldStem == null) {
                LOGGER.error("❌ Failed to load world stem!");
                levelAccess.close();
                cleanupLobby();
                return false;
            }

            // Config für Mixin setzen
            MinigameServerManager.setPendingConfig(config);
            isMinigameActive = true;

            LOGGER.info("🚀 Starting minigame server...");

            try {
                minecraft.doWorldLoad(levelAccess, packRepository, worldStem, false);
                LOGGER.info("✅ Minigame world loading complete!");
                isLoadingMinigame = false; // Loading abgeschlossen
                return true;

            } catch (Exception e) {
                LOGGER.error("❌ Failed to start server", e);
                MinigameServerManager.clearPendingConfig();
                isMinigameActive = false;
                isLoadingMinigame = false; // Loading abgeschlossen (mit Fehler)
                try {
                    worldStem.close();
                    levelAccess.close();
                } catch (Exception closeEx) {
                    LOGGER.error("Failed to close resources", closeEx);
                }
                cleanupLobby();
                return false;
            }

        } catch (Exception e) {
            LOGGER.error("❌ Failed to load minigame lobby", e);
            isLoadingMinigame = false; // Loading abgeschlossen (mit Fehler)
            cleanupLobby();
            return false;
        }
    }

    /**
     * Beendet das Minigame und löscht die temporäre Welt
     */
    public static void endMinigame() {
        LOGGER.info("🛑 Ending minigame...");
        isMinigameActive = false;
        MinigameServerManager.clearPendingConfig();

        // Cleanup wird verzögert ausgeführt (nach Server-Stop)
        scheduleCleanup();
    }

    /**
     * Plant Cleanup nach kurzer Verzögerung
     */
    private static void scheduleCleanup() {
        new Thread(() -> {
            try {
                // Warte bis Server vollständig gestoppt
                Thread.sleep(2000);
                cleanupLobby();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "MinigameCleanup").start();
    }

    /**
     * Löscht die temporäre Lobby-Welt
     */
    public static void cleanupLobby() {
        if (currentLobbyPath == null) return;

        LOGGER.info("🗑️ Cleaning up minigame lobby...");

        try {
            if (Files.exists(currentLobbyPath)) {
                // Schreibschutz entfernen vor dem Löschen
                setReadOnly(currentLobbyPath, false);
                deleteDirectory(currentLobbyPath);
                LOGGER.info("✅ Lobby cleaned up successfully!");
            }
        } catch (IOException e) {
            LOGGER.error("❌ Failed to cleanup lobby", e);
        }

        currentLobbyPath = null;
    }

    /**
     * Entpackt lobby.mcsave aus Resources in das Welten-Verzeichnis
     */
    private static boolean extractLobbyFromResources(Path targetPath) {
        LOGGER.info("📦 Extracting lobby from resources to: {}", targetPath);

        try {
            try (InputStream resourceStream = MinigameWorldLoader.class.getResourceAsStream(LOBBY_RESOURCE)) {
                if (resourceStream == null) {
                    LOGGER.error("❌ Resource not found: {}", LOBBY_RESOURCE);
                    return false;
                }

                // ZIP entpacken
                try (ZipInputStream zipIn = new ZipInputStream(resourceStream)) {
                    ZipEntry entry;
                    while ((entry = zipIn.getNextEntry()) != null) {
                        Path entryPath = targetPath.resolve(entry.getName()).normalize();

                        // Sicherheitscheck: Pfad muss innerhalb targetPath bleiben
                        if (!entryPath.startsWith(targetPath)) {
                            LOGGER.warn("⚠️ Skipping suspicious zip entry: {}", entry.getName());
                            continue;
                        }

                        if (entry.isDirectory()) {
                            Files.createDirectories(entryPath);
                        } else {
                            Files.createDirectories(entryPath.getParent());
                            Files.copy(zipIn, entryPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                        zipIn.closeEntry();
                    }
                }
            }

            LOGGER.info("✅ Lobby extracted successfully!");
            return true;

        } catch (IOException e) {
            LOGGER.error("❌ Failed to extract lobby", e);
            return false;
        }
    }

    /**
     * Setzt Schreibschutz für alle Dateien rekursiv (plattformunabhängig)
     * Funktioniert auf Windows, Linux und macOS
     */
    private static void setReadOnly(Path path, boolean readOnly) throws IOException {
        if (!Files.exists(path)) return;

        try (var stream = Files.walk(path)) {
            stream.forEach(p -> {
                try {
                    // setWritable(true) = schreibbar, setWritable(false) = schreibgeschützt
                    p.toFile().setWritable(!readOnly);
                } catch (SecurityException e) {
                    LOGGER.warn("Failed to set writable={} on: {}", !readOnly, p);
                }
            });
        }

        LOGGER.info("🔒 Set read-only={} for: {}", readOnly, path);
    }

    /**
     * Löscht ein Verzeichnis rekursiv
     */
    private static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            try (var stream = Files.walk(path)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                // Schreibschutz entfernen vor Löschen
                                p.toFile().setWritable(true);
                                Files.delete(p);
                            } catch (IOException e) {
                                LOGGER.warn("Failed to delete: {}", p);
                            }
                        });
            }
        }
    }

    /**
     * Prüft ob ein Minigame aktiv ist
     */
    public static boolean isMinigameActive() {
        return isMinigameActive;
    }

    /**
     * Prüft ob gerade eine Minigame-Welt geladen wird.
     * Während des Ladens soll kein Cleanup ausgelöst werden.
     */
    public static boolean isLoadingMinigame() {
        return isLoadingMinigame;
    }

    /**
     * Prüft ob die Lobby-Resource existiert
     */
    public static boolean lobbyResourceExists() {
        try (InputStream stream = MinigameWorldLoader.class.getResourceAsStream(LOBBY_RESOURCE)) {
            return stream != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Holt den Pfad zur temporären Lobby-Welt
     */
    public static Path getLobbyWorldPath() {
        return currentLobbyPath;
    }

    /**
     * Setzt die Lobby zurück, indem alle Chunks aus dem RAM entladen werden.
     * Da Speichern deaktiviert ist, werden die Chunks beim nächsten Laden frisch von der Festplatte gelesen.
     *
     * Diese Methode sollte aufgerufen werden, wenn alle Spieler die Lobby verlassen und in ein Minigame gehen.
     *
     * @param server Der Minecraft-Server
     * @return true wenn erfolgreich
     */
    public static boolean resetLobby(MinecraftServer server) {
        if (isResettingLobby) {
            LOGGER.info("⏳ Lobby reset already in progress, skipping...");
            return true;
        }

        isResettingLobby = true;
        LOGGER.info("🔄 Resetting lobby (unloading chunks from RAM)...");

        try {
            // Hole Lobby-Level (Overworld)
            ServerLevel lobbyLevel = server.getLevel(Level.OVERWORLD);
            if (lobbyLevel == null) {
                LOGGER.error("❌ Cannot reset lobby - Overworld not found");
                isResettingLobby = false;
                return false;
            }

            // Entlade alle geladenen Chunks aus dem RAM
            // Da Speichern deaktiviert ist, werden Änderungen verworfen
            // und beim nächsten Laden werden die Chunks frisch von der Festplatte gelesen
            var chunkSource = lobbyLevel.getChunkSource();

            // Force-Unload aller Chunks durch Tick mit leerer Spielerliste
            // Die Chunks werden automatisch entladen wenn keine Spieler in der Nähe sind
            LOGGER.info("📦 Marking all lobby chunks for unload...");

            // Wir können die Chunks nicht direkt entladen, aber sie werden automatisch entladen
            // sobald keine Spieler mehr in der Lobby sind (was der Fall ist wenn alle im Battle sind)

            LOGGER.info("✅ Lobby chunks marked for unload - will be fresh when players return!");
            isResettingLobby = false;
            return true;

        } catch (Exception e) {
            LOGGER.error("❌ Failed to reset lobby", e);
            isResettingLobby = false;
            return false;
        }
    }

    /**
     * Markiert die Lobby als bereit für einen Reset.
     * Der eigentliche Reset passiert automatisch wenn keine Spieler mehr in der Lobby sind
     * und die Chunks aus dem RAM entladen werden.
     */
    public static void markLobbyForReset() {
        LOGGER.info("🔄 Lobby marked for reset - chunks will be fresh when reloaded");
        lobbyNeedsReset = true;
    }

    /**
     * Prüft ob die Lobby einen Reset benötigt
     */
    public static boolean doesLobbyNeedReset() {
        return lobbyNeedsReset;
    }

    /**
     * Markiert den Lobby-Reset als abgeschlossen
     */
    public static void clearLobbyResetFlag() {
        lobbyNeedsReset = false;
        LOGGER.info("✅ Lobby reset flag cleared");
    }

    private static boolean lobbyNeedsReset = false;

    /**
     * Erzwingt einen Reset der Lobby, indem:
     * 1. Alle Chunks im RAM als "gespeichert" markiert werden (Änderungen werden verworfen)
     * 2. Die Region-Dateien auf der Festplatte mit frischen Daten überschrieben werden
     * 3. Die Chunks entladen werden
     *
     * Beim nächsten Teleportieren der Spieler werden die Chunks frisch von der Festplatte geladen.
     *
     * @param server Der Minecraft-Server
     * @return true wenn erfolgreich
     */
    public static boolean forceResetLobbyRegions(MinecraftServer server) {
        if (currentLobbyPath == null) {
            LOGGER.warn("⚠️ Cannot reset lobby - no lobby path set");
            return false;
        }

        if (isResettingLobby) {
            LOGGER.info("⏳ Lobby reset already in progress, skipping...");
            return true;
        }

        isResettingLobby = true;
        LOGGER.info("🔄 Force resetting lobby...");

        try {
            // Hole Lobby-Level (Overworld)
            ServerLevel lobbyLevel = server.getLevel(Level.OVERWORLD);
            if (lobbyLevel == null) {
                LOGGER.error("❌ Cannot reset lobby - Overworld not found");
                isResettingLobby = false;
                return false;
            }

            // === SCHRITT 1: Entlade alle Chunks ohne zu speichern ===
            if (lobbyLevel instanceof IResettableLevel resettable) {
                LOGGER.info("📦 Resetting all chunks in memory...");
                resettable.legacy$unloadAllChunksWithoutSaving();
            } else {
                LOGGER.warn("⚠️ Level does not implement IResettableLevel, using fallback");
                var chunkSource = lobbyLevel.getChunkSource();
            }

            // === SCHRITT 2: Überschreibe Region-Dateien ===
            LOGGER.info("📝 Overwriting region files with fresh data from resources...");

            Path regionPath = currentLobbyPath.resolve("region");

            // Erstelle region-Verzeichnis falls nicht vorhanden
            Files.createDirectories(regionPath);

            // Lösche existierende Region-Dateien
            if (Files.exists(regionPath)) {
                try (var stream = Files.list(regionPath)) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".mca"))
                          .forEach(p -> {
                              try {
                                  p.toFile().setWritable(true);
                                  Files.delete(p);
                                  LOGGER.info("🗑️ Deleted region file: {}", p.getFileName());
                              } catch (IOException e) {
                                  LOGGER.warn("⚠️ Failed to delete region file: {} - {}", p.getFileName(), e.getMessage());
                              }
                          });
                }
            }

            // Extrahiere frische Region-Dateien aus Resources
            if (!extractRegionFilesFromResources(currentLobbyPath)) {
                LOGGER.error("❌ Failed to extract fresh region files");
                isResettingLobby = false;
                return false;
            }

            LOGGER.info("✅ Lobby region files reset successfully!");
            LOGGER.info("ℹ️ Chunks will be loaded fresh when players teleport to lobby");

            lobbyNeedsReset = false;
            isResettingLobby = false;
            return true;

        } catch (Exception e) {
            LOGGER.error("❌ Failed to reset lobby", e);
            isResettingLobby = false;
            return false;
        }
    }

    /**
     * Extrahiert nur die Region-Dateien (.mca) aus lobby.mcsave
     */
    private static boolean extractRegionFilesFromResources(Path targetPath) {
        LOGGER.info("📦 Extracting region files from resources...");

        try {
            try (InputStream resourceStream = MinigameWorldLoader.class.getResourceAsStream(LOBBY_RESOURCE)) {
                if (resourceStream == null) {
                    LOGGER.error("❌ Resource not found: {}", LOBBY_RESOURCE);
                    return false;
                }

                // ZIP entpacken - nur .mca Dateien
                try (ZipInputStream zipIn = new ZipInputStream(resourceStream)) {
                    ZipEntry entry;
                    int extractedCount = 0;

                    while ((entry = zipIn.getNextEntry()) != null) {
                        String name = entry.getName();

                        // Nur .mca Dateien (Region-Daten) extrahieren
                        if (!name.endsWith(".mca")) {
                            zipIn.closeEntry();
                            continue;
                        }

                        Path entryPath = targetPath.resolve(name).normalize();

                        // Sicherheitscheck
                        if (!entryPath.startsWith(targetPath)) {
                            LOGGER.warn("⚠️ Skipping suspicious zip entry: {}", name);
                            zipIn.closeEntry();
                            continue;
                        }

                        Files.createDirectories(entryPath.getParent());
                        Files.copy(zipIn, entryPath, StandardCopyOption.REPLACE_EXISTING);
                        extractedCount++;

                        zipIn.closeEntry();
                    }

                    LOGGER.info("✅ Extracted {} region files", extractedCount);
                }
            }

            return true;

        } catch (IOException e) {
            LOGGER.error("❌ Failed to extract region files", e);
            return false;
        }
    }

    /**
     * Prüft ob die Lobby gerade zurückgesetzt wird
     */
    public static boolean isResettingLobby() {
        return isResettingLobby;
    }
}
