package wily.legacy.mixin.base.minigame;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.minigame.IMinecraftServer;
import wily.legacy.minigame.IResettableLevel;
import wily.legacy.minigame.MinigamesController;

import java.util.ArrayList;
import java.util.List;

/**
 * Deaktiviert Auto-Saving für Minigame-Server Levels und fügt Reset-Funktionalität hinzu
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements IResettableLevel {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("MinigameServerLevel");

    @Shadow
    @Final
    private MinecraftServer server;

    /**
     * Verhindert das Speichern von Levels auf Minigame-Servern
     */
    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void onSave(ProgressListener progressListener, boolean flush, boolean skipSave, CallbackInfo ci) {
        // Prüfe ob es ein Minigame-Server ist
        if (server instanceof IMinecraftServer minigameServer && minigameServer.isMinigameServer()) {
            ServerLevel level = (ServerLevel)(Object)this;

            // Optional: Prüfe ob das Level einen aktiven Minigame-Controller hat
            MinigamesController controller = MinigamesController.getMinigameController(level);

            if (controller != null && controller.isActive()) {
                LOGGER.debug("🚫 Skipping auto-save for minigame level: {}", level.dimension().location());
                ci.cancel(); // Verhindere das Speichern
            }
        }
    }

    /**
     * Verhindert das Speichern von Player-Daten auf Minigame-Servern
     * (Optional - kann auch aktiviert werden wenn gewünscht)
     */
    @Inject(method = "saveLevelData", at = @At("HEAD"), cancellable = true)
    private void onSaveLevelData(CallbackInfo ci) {
        if (server instanceof IMinecraftServer minigameServer && minigameServer.isMinigameServer()) {
            ServerLevel level = (ServerLevel)(Object)this;
            MinigamesController controller = MinigamesController.getMinigameController(level);

            if (controller != null && controller.isActive()) {
                LOGGER.debug("🚫 Skipping level data save for minigame level: {}", level.dimension().location());
                ci.cancel();
            }
        }
    }

    // ==================== IResettableLevel Implementation ====================

    @Override
    @Unique
    public boolean legacy$resetAllChunks() {
        ServerLevel level = (ServerLevel)(Object)this;
        LOGGER.info("🔄 Resetting all chunks for level: {}", level.dimension().location());

        try {
            ServerChunkCache chunkSource = level.getChunkSource();

            // Sammle alle geladenen Chunks
            List<LevelChunk> chunksToReset = new ArrayList<>();
            chunkSource.chunkMap.forEachReadyToSendChunk(chunksToReset::add);

            LOGGER.info("📦 Found {} chunks to reset", chunksToReset.size());

            // Markiere alle Chunks als gespeichert (damit Änderungen verworfen werden)
            for (LevelChunk chunk : chunksToReset) {
                // tryMarkSaved() markiert den Chunk als gespeichert und gibt true zurück
                // Dies verhindert, dass Änderungen beim Entladen gespeichert werden
                chunk.tryMarkSaved();
                chunk.setLoaded(false);
            }

            // Flush ohne zu speichern - das leert die Caches
            chunkSource.save(false);

            LOGGER.info("✅ All chunks marked for reset");
            return true;

        } catch (Exception e) {
            LOGGER.error("❌ Failed to reset chunks", e);
            return false;
        }
    }

    @Override
    @Unique
    public void legacy$unloadAllChunksWithoutSaving() {
        ServerLevel level = (ServerLevel)(Object)this;
        LOGGER.info("📦 Unloading all chunks without saving for level: {}", level.dimension().location());

        try {
            ServerChunkCache chunkSource = level.getChunkSource();

            // Sammle alle geladenen Chunks
            List<LevelChunk> chunksToUnload = new ArrayList<>();
            chunkSource.chunkMap.forEachReadyToSendChunk(chunksToUnload::add);

            // Markiere als gespeichert und entlade
            for (LevelChunk chunk : chunksToUnload) {
                chunk.tryMarkSaved();
                chunk.setLoaded(false);
                // Entlade den Chunk aus dem Level
                level.unload(chunk);
            }

            LOGGER.info("✅ {} chunks unloaded without saving", chunksToUnload.size());

        } catch (Exception e) {
            LOGGER.error("❌ Failed to unload chunks", e);
        }
    }

    @Override
    @Unique
    public boolean legacy$isMinigameLevel() {
        if (server instanceof IMinecraftServer minigameServer) {
            return minigameServer.isMinigameServer();
        }
        return false;
    }

    @Override
    @Unique
    public void legacy$reset() {
        ServerLevel level = (ServerLevel)(Object)this;
        LOGGER.info("🔄 Resetting chunk cache for level: {}", level.dimension().location());
        try {
            ServerChunkCache chunkSource = level.getChunkSource();
            ChunkMapAccessor accessor = (ChunkMapAccessor) chunkSource.chunkMap;

            // Leert die Maps, die die geladenen Chunks im Speicher halten
            accessor.getUpdatingChunkMap().clear();
            accessor.getVisibleChunkMap().clear();

            // Setzt das modified-Flag, damit die leere Map als die neue "sichtbare" Map übernommen wird
            accessor.setModified(true);
            accessor.invokePromoteChunkMap();

            LOGGER.info("✅ Chunk cache cleared. Chunks will be reloaded from disk on next access.");
        } catch (Exception e) {
            LOGGER.error("❌ Failed to reset chunk cache", e);
        }
    }
}
