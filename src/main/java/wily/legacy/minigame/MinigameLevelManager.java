package wily.legacy.minigame;

import net.minecraft.Util;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Verwaltet das Laden und Entladen von Minigame-Levels
 */
public class MinigameLevelManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinigameLevelManager.class);

    // Registrierte Minigame-Dimensionen
    private final Map<String, ResourceKey<Level>> registeredDimensions = new HashMap<>();

    // Welche Dimension hat welchen Controller
    private final Map<ResourceKey<Level>, Minigame<?>> dimensionControllers = new HashMap<>();

    private final MinecraftServer server;

    public MinigameLevelManager(MinecraftServer server) {
        this.server = server;
    }

    /**
     * Registriert eine Dimension für ein Minigame
     *
     * @param name Name der Dimension (z.B. "lobby", "battle_arena")
     * @param dimensionType Typ (z.B. minecraft:overworld, minecraft:the_nether)
     * @param minigame Das Minigame für diese Dimension
     */
    public void registerDimension(String name, ResourceKey<Level> dimensionType, Minigame<?> minigame) {
        registeredDimensions.put(name, dimensionType);
        dimensionControllers.put(dimensionType, minigame);
        LOGGER.info("📝 Registered minigame dimension: {} -> {} ({})", name, dimensionType.location(), minigame.getName());
    }

    /**
     * Lädt eine Minigame-Dimension zur Laufzeit
     */
    public Optional<ServerLevel> loadDimension(String name) {
        ResourceKey<Level> dimensionKey = registeredDimensions.get(name);
        if (dimensionKey == null) {
            LOGGER.warn("❌ Unknown dimension: {}", name);
            return Optional.empty();
        }

        ServerLevel level = server.getLevel(dimensionKey);
        if (level != null) {
            LOGGER.info("✅ Dimension {} already loaded", name);

            // Setze den richtigen Controller für dieses Level
            Minigame<?> minigame = dimensionControllers.get(dimensionKey);
            if (minigame != null) {
                MinigamesController controller = MinigamesController.getMinigameController(level);
                if (controller.getActiveMinigame() == null) {
                    controller.setActiveMinigame(minigame);
                    LOGGER.info("✅ Set minigame {} for dimension {}", minigame.getName(), name);
                }
            }

            return Optional.of(level);
        }

        LOGGER.info("🔄 Loading dimension: {} dynamically", name);

        try {
            // Hole die benötigten Registry-Daten
            var registryAccess = server.registries().compositeAccess();
            var levelStemRegistry = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.LEVEL_STEM);
            LevelStem levelStem = levelStemRegistry.getValue(dimensionKey.location());

            if (levelStem == null) {
                LOGGER.error("❌ LevelStem not found for dimension: {}", name);
                return Optional.empty();
            }

            // Hole WorldData
            var worldData = server.getWorldData();
            var serverLevelData = worldData.overworldData();

            // Erstelle das neue ServerLevel
            ServerLevel newLevel = new ServerLevel(
                server,
                Util.backgroundExecutor(),
                server.storageSource,
                serverLevelData,
                dimensionKey,
                levelStem,
                worldData.isDebugWorld(),
                net.minecraft.world.level.biome.BiomeManager.obfuscateSeed(worldData.worldGenOptions().seed()),
                com.google.common.collect.ImmutableList.of(), // Keine Custom Spawner
                false, // shouldTickTime - Arena-Welten ticken nicht die Zeit
                null // RandomSequences
            );

            // Füge Level zur Server-Map hinzu über Accessor
            if (server instanceof IMinecraftServerLevels serverLevels) {
                serverLevels.legacy$getLevels().put(dimensionKey, newLevel);

                // Setze den richtigen Controller für dieses Level
                Minigame<?> minigame = dimensionControllers.get(dimensionKey);
                if (minigame != null) {
                    MinigamesController controller = MinigamesController.getMinigameController(newLevel);
                    controller.setActiveMinigame(minigame);
                    LOGGER.info("✅ Set minigame {} for dimension {}", minigame.getName(), name);
                }

                LOGGER.info("✅ Successfully loaded dimension: {}", name);
                return Optional.of(newLevel);
            } else {
                LOGGER.error("❌ Server does not implement IMinecraftServerLevels - cannot add to levels map");
                return Optional.empty();
            }

        } catch (Exception e) {
            LOGGER.error("❌ Failed to load dimension: {}", name, e);
            return Optional.empty();
        }
    }

    /**
     * Entlädt eine Minigame-Dimension zur Laufzeit (nur wenn keine Spieler drin sind)
     * WICHTIG: Chunks werden NICHT gespeichert!
     */
    public void unloadDimension(String name) {
        ResourceKey<Level> dimensionKey = registeredDimensions.get(name);
        if (dimensionKey == null) {
            return;
        }

        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null) {
            LOGGER.debug("Dimension {} is not loaded", name);
            return;
        }

        // Prüfe ob Spieler im Level sind
        if (!level.players().isEmpty()) {
            LOGGER.warn("⚠️ Cannot unload dimension {} - players still inside", name);
            return;
        }

        LOGGER.info("🔄 Unloading dimension: {} (without saving)", name);

        try {
            // Entferne alle Entities (außer Spieler, die sind bereits geprüft)
            level.getAllEntities().forEach(entity -> {
                if (!(entity instanceof net.minecraft.world.entity.player.Player)) {
                    entity.discard();
                }
            });

            // Schließe die ChunkSource ohne zu speichern
            // (ServerLevelMixin verhindert bereits das Speichern)
            try {
                level.getChunkSource().close();
            } catch (Exception e) {
                LOGGER.warn("⚠️ Error closing chunk source for {}: {}", name, e.getMessage());
            }

            // Entferne Level aus der Server-Map über Accessor
            if (server instanceof IMinecraftServerLevels serverLevels) {
                serverLevels.legacy$getLevels().remove(dimensionKey);
                LOGGER.info("✅ Successfully unloaded dimension: {} (no data saved)", name);
            } else {
                LOGGER.error("❌ Server does not implement IMinecraftServerLevels - cannot remove from levels map");
            }

        } catch (Exception e) {
            LOGGER.error("❌ Failed to unload dimension: {}", name, e);
        }
    }

    /**
     * Holt den Controller für eine Dimension
     */
    public Optional<Minigame<?>> getMinigameForDimension(ResourceKey<Level> dimension) {
        return Optional.ofNullable(dimensionControllers.get(dimension));
    }

    /**
     * Holt ein Level anhand des Namens
     */
    public Optional<ServerLevel> getLevel(String name) {
        ResourceKey<Level> dimensionKey = registeredDimensions.get(name);
        if (dimensionKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(server.getLevel(dimensionKey));
    }

    /**
     * Standard-Setup für Minigame-Server
     * Lobby = Overworld, Arena = Separate Dimension
     */
    public void setupStandardDimensions(Minigame<?> arenaMinigame) {
        // Lobby ist immer die Overworld
        registerDimension("lobby", Level.OVERWORLD, Minigame.LOBBY);

        // Arena kann eine separate Dimension sein oder auch Overworld
        // Für den Anfang nutzen wir Nether als Arena
        registerDimension("arena", Level.NETHER, arenaMinigame);


        LOGGER.info("✅ Standard minigame dimensions configured");
    }
}

