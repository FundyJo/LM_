package wily.legacy.minigame;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Lädt externe Welten (z.B. Battle-Arena) für Minigames
 */
public class ExternalWorldLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("ExternalWorldLoader");

    // Battle-Welt Pfad (kann über Config angepasst werden)
    private static final String BATTLE_WORLD_PATH = "C:\\Users\\timos\\curseforge\\minecraft\\Instances\\l4j\\saves\\Battle";

    /**
     * Lädt die Battle-Welt und gibt das ServerLevel zurück.
     *
     * @param server Der MinecraftServer
     * @param worldName Der Name der Welt (z.B. "Battle")
     * @param sourcePath Der Quellpfad der Welt (optional, wenn null wird der Standard-Pfad verwendet)
     * @return Das geladene ServerLevel oder empty wenn fehlgeschlagen
     */
    public static Optional<ServerLevel> loadWorld(MinecraftServer server, String worldName, Path sourcePath) {
        LOGGER.info("🔄 Loading external world: {}", worldName);

        try {
            // Erstelle einen ResourceKey für die neue Dimension
            ResourceLocation dimensionId = ResourceLocation.fromNamespaceAndPath("legacy", worldName.toLowerCase());
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);

            // Prüfe ob die Dimension bereits geladen ist
            ServerLevel existingLevel = server.getLevel(dimensionKey);
            if (existingLevel != null) {
                LOGGER.info("✅ World {} already loaded", worldName);
                return Optional.of(existingLevel);
            }

            // Für Battle-Welt: Verwende den konfigurierten Pfad
            Path battleSource = sourcePath;
            if (battleSource == null && "Battle".equalsIgnoreCase(worldName)) {
                battleSource = Paths.get(BATTLE_WORLD_PATH);
                LOGGER.info("📂 Using Battle world from: {}", battleSource);
            }

            // Kopiere die Welt-Daten in den Server-Dimensions-Ordner
            if (battleSource != null && Files.exists(battleSource)) {
                Path serverWorldPath = server.storageSource.getDimensionPath(dimensionKey);
                LOGGER.info("📂 Target dimension path: {}", serverWorldPath);

                // Kopiere region, entities, poi Ordner
                copyWorldData(battleSource, serverWorldPath);
            } else if (battleSource != null) {
                LOGGER.warn("⚠️ External world not found at: {}", battleSource);
            }

            // Hole die Overworld als Template für die neue Dimension
            ServerLevel overworld = server.overworld();
            var registryAccess = server.registries().compositeAccess();
            var levelStemRegistry = registryAccess.lookupOrThrow(Registries.LEVEL_STEM);

            // Verwende Overworld LevelStem als Basis
            LevelStem overworldStem = levelStemRegistry.getValue(LevelStem.OVERWORLD.location());
            if (overworldStem == null) {
                LOGGER.error("❌ Could not get overworld level stem");
                return Optional.empty();
            }

            // Hole WorldData
            var worldData = server.getWorldData();
            var serverLevelData = worldData.overworldData();

            // Erstelle das neue ServerLevel
            ServerLevel newLevel = new ServerLevel(
                server,
                net.minecraft.Util.backgroundExecutor(),
                server.storageSource,
                serverLevelData,
                dimensionKey,
                overworldStem,
                worldData.isDebugWorld(),
                net.minecraft.world.level.biome.BiomeManager.obfuscateSeed(worldData.worldGenOptions().seed()),
                com.google.common.collect.ImmutableList.of(),
                false, // shouldTickTime
                null   // RandomSequences
            );

            // Füge Level zur Server-Map hinzu
            if (server instanceof IMinecraftServerLevels serverLevels) {
                serverLevels.legacy$getLevels().put(dimensionKey, newLevel);
                LOGGER.info("✅ Successfully loaded world: {} as dimension {}", worldName, dimensionId);
                return Optional.of(newLevel);
            } else {
                LOGGER.error("❌ Server does not implement IMinecraftServerLevels");
                return Optional.empty();
            }

        } catch (Exception e) {
            LOGGER.error("❌ Failed to load world: {}", worldName, e);
            return Optional.empty();
        }
    }

    /**
     * Kopiert die Welt-Daten (region, entities, poi) in den Ziel-Ordner
     */
    private static void copyWorldData(Path source, Path destination) {
        try {
            // Erstelle Ziel-Verzeichnis
            Files.createDirectories(destination);

            // Kopiere region Ordner (enthält die Chunk-Daten)
            Path sourceRegion = source.resolve("region");
            Path destRegion = destination.resolve("region");
            if (Files.exists(sourceRegion)) {
                copyDirectory(sourceRegion, destRegion);
                LOGGER.info("✅ Copied region data");
            }

            // Kopiere entities Ordner
            Path sourceEntities = source.resolve("entities");
            Path destEntities = destination.resolve("entities");
            if (Files.exists(sourceEntities)) {
                copyDirectory(sourceEntities, destEntities);
                LOGGER.info("✅ Copied entities data");
            }

            // Kopiere poi Ordner (Points of Interest)
            Path sourcePoi = source.resolve("poi");
            Path destPoi = destination.resolve("poi");
            if (Files.exists(sourcePoi)) {
                copyDirectory(sourcePoi, destPoi);
                LOGGER.info("✅ Copied POI data");
            }

            LOGGER.info("✅ World data copied successfully to {}", destination);

        } catch (Exception e) {
            LOGGER.error("❌ Failed to copy world data", e);
        }
    }

    /**
     * Kopiert ein Verzeichnis rekursiv
     */
    private static void copyDirectory(Path source, Path destination) throws IOException {
        if (!Files.exists(destination)) {
            Files.createDirectories(destination);
        }

        Files.walk(source).forEach(sourcePath -> {
            try {
                Path destPath = destination.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(destPath)) {
                        Files.createDirectories(destPath);
                    }
                } else {
                    // Überschreibe existierende Dateien nicht
                    if (!Files.exists(destPath)) {
                        Files.copy(sourcePath, destPath);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to copy: {}", sourcePath, e);
            }
        });
    }

    /**
     * Kopiert eine Welt von einem externen Pfad in den saves-Ordner
     */
    public static boolean copyWorld(Path source, Path destination) {
        try {
            if (!Files.exists(source)) {
                LOGGER.error("❌ Source world does not exist: {}", source);
                return false;
            }

            if (Files.exists(destination)) {
                LOGGER.info("ℹ️ Destination already exists, skipping copy: {}", destination);
                return true;
            }

            LOGGER.info("📋 Copying world from {} to {}", source, destination);

            // Kopiere rekursiv
            Files.walk(source).forEach(sourcePath -> {
                try {
                    Path destPath = destination.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(destPath);
                    } else {
                        Files.copy(sourcePath, destPath);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to copy: {}", sourcePath, e);
                }
            });

            LOGGER.info("✅ World copied successfully");
            return true;

        } catch (Exception e) {
            LOGGER.error("❌ Failed to copy world", e);
            return false;
        }
    }
}

