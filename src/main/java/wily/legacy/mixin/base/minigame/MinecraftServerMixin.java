package wily.legacy.mixin.base.minigame;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.minigame.*;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

/**
 * Implementiert IMinecraftServer für den MinecraftServer
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements IMinecraftServer {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("MinecraftServerMixin");

    @Shadow
    @Final
    private Map<net.minecraft.resources.ResourceKey<Level>, ServerLevel> levels;

    @Shadow
    @Final
    private Executor executor;

    @Shadow
    @Final
    public LevelStorageSource.LevelStorageAccess storageSource;


    @Shadow
    @Final
    private net.minecraft.core.LayeredRegistryAccess<net.minecraft.server.RegistryLayer> registries;

    @Unique
    private boolean legacy$isMinigameServer = false;

    @Unique
    private int legacy$maxPlayers = -1;

    @Unique
    private MinigameServerConfig legacy$config = null;

    @Unique
    private MinigameLevelManager legacy$levelManager = null;

    @Override
    public boolean isMinigameServer() {
        return legacy$isMinigameServer;
    }

    @Override
    public void setMinigameServer(boolean value) {
        this.legacy$isMinigameServer = value;
    }

    @Override
    public int getMaxPlayers() {
        return legacy$maxPlayers;
    }

    @Override
    public void setMaxPlayers(int value) {
        this.legacy$maxPlayers = value;
    }

    @Override
    public MinigameServerConfig getMinigameServerConfig() {
        return legacy$config;
    }

    @Override
    public void setMinigameServerConfig(MinigameServerConfig config) {
        if (this.legacy$config != null) {
            throw new IllegalStateException("Minigame server config can only be set once!");
        }
        this.legacy$config = config;
        this.legacy$maxPlayers = config.getMaxPlayers();
    }

    @Override
    public MinigameLevelManager getMinigameLevelManager() {
        return legacy$levelManager;
    }

    @Override
    public void setMinigameLevelManager(MinigameLevelManager manager) {
        this.legacy$levelManager = manager;
    }

    /**
     * Überschreibt die Level-Erstellung für Minigame-Server:
     * - Entfernt alle Standard-Welten (Overworld, Nether, End)
     * - Lädt nur die Lobby als Overworld
     */
    @Inject(method = "createLevels", at = @At("HEAD"), cancellable = true)
    private void legacy$onCreateLevels(CallbackInfo ci) {
        if (!MinigameServerManager.hasPendingConfig()) {
            return; // Normaler Server, keine Änderungen
        }

        LOGGER.info("🎮 ========================================");
        LOGGER.info("🎮 Creating Minigame Server Levels");
        LOGGER.info("🎮 ========================================");

        // Verhindere normale Level-Erstellung
        ci.cancel();

        MinecraftServer server = (MinecraftServer)(Object)this;

        // Lade nur die Lobby als Overworld
        ServerLevelData serverLevelData = server.getWorldData().overworldData();
        Registry<LevelStem> registry = this.registries.compositeAccess().lookupOrThrow(Registries.LEVEL_STEM);
        WorldOptions worldOptions = server.getWorldData().worldGenOptions();
        long seed = worldOptions.seed();
        long obfuscatedSeed = BiomeManager.obfuscateSeed(seed);

        // Lade Lobby-Level als Overworld (OHNE Custom Spawner - keine Monster in der Lobby!)
        LevelStem levelStem = registry.getValue(LevelStem.OVERWORLD);
        ServerLevel lobbyLevel = new ServerLevel(
            server,
            this.executor,
            this.storageSource,
            serverLevelData,
            Level.OVERWORLD,
            levelStem,
            server.getWorldData().isDebugWorld(),
            obfuscatedSeed,
            ImmutableList.of(),
            true,
            null
        );

        this.levels.put(Level.OVERWORLD, lobbyLevel);

        LOGGER.info("✅ Loaded Lobby as Overworld");
        LOGGER.info("   Dimension: {}", Level.OVERWORLD.location());
        LOGGER.info("   Seed: {}", seed);
        LOGGER.info("🎮 ========================================");
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    public void legacy$tickMinigames(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer)(Object)this;
        if (!(server instanceof IMinecraftServer minigameServer) || !minigameServer.isMinigameServer()) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            MinigamesController controller = MinigamesController.getMinigameController(level);
            if (controller.getActiveMinigame() != Minigame.NONE) {
                controller.getActiveController().tick();
            }
        }
    }

    @Inject(method = "saveAllChunks", at = @At("HEAD"), cancellable = true)
    private void onSaveAllChunks(boolean suppressLogs, boolean flush, boolean forced, CallbackInfoReturnable<Boolean> cir) {
        if (legacy$isMinigameServer) {
            LOGGER.debug("🚫 Skipping saveAllChunks for minigame server");
            cir.setReturnValue(true); // Gebe vor dass erfolgreich gespeichert wurde
        }
    }
}

