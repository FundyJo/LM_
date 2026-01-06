package wily.legacy.mixin.base.minigame;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.minigame.*;

/**
 * Initialisiert IntegratedServer als Minigame-Server wenn eine pending Config existiert.
 * Das Publishing wird über MinigameServerManager gehandhabt.
 */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("MinigameIntegratedServer");
    @Inject(method = "initServer", at = @At("RETURN"))
    private void onServerInit(CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer server = (MinecraftServer) (Object) this;

        if (! MinigameServerManager.hasPendingConfig()) {
            return;
        }

        LOGGER.info("🎮 ========================================");
        LOGGER.info("🎮 Initializing Minigame IntegratedServer");
        LOGGER.info("🎮 ========================================");

        MinigameServerConfig config = MinigameServerManager. consumePendingConfig();
        if (config == null) {
            LOGGER.warn("⚠️ Pending config was consumed!");
            return;
        }

        if (!(server instanceof IMinecraftServer minigameServer)) {
            LOGGER.error("❌ Server does not implement IMinecraftServer!");
            return;
        }

        // ✅ Konfiguriere Storage (init() wurde bereits in Legacy4J.init() aufgerufen)
        MinigamesController.configureServerFile(server);

        // Markiere als Minigame-Server
        minigameServer.setMinigameServer(true);
        minigameServer.setMinigameServerConfig(config);
        minigameServer.setMaxPlayers(config.getMaxPlayers());

        LOGGER.info("✅ Server marked as minigame server");
        LOGGER.info("   Type: {}", config.getMinigameType());
        LOGGER.info("   Max Players: {}", config.getMaxPlayers());
        LOGGER.info("   Min Players:  {}", config.getMinPlayersToStart());

        // Erstelle Level Manager
        MinigameLevelManager levelManager = new MinigameLevelManager(server);
        minigameServer. setMinigameLevelManager(levelManager);

        // Registriere Dimensionen
        levelManager. registerDimension("lobby", Level.OVERWORLD, Minigame.LOBBY);
        levelManager.registerDimension("arena", Level.NETHER, config.getMinigame());

        LOGGER.info("✅ Dimensions registered");

        // Initialisiere Lobby-Controller
        ServerLevel overworldLevel = server.getLevel(Level.OVERWORLD);
        if (overworldLevel != null) {
            MinigamesController lobbyController = MinigamesController.getMinigameController(overworldLevel);
            lobbyController.setActiveMinigame(Minigame.LOBBY);

            LOGGER.info("✅ Lobby controller initialized in Overworld");
        } else {
            LOGGER.error("❌ Overworld level not found!");
        }

        // Schedule Server Publishing für LAN-Zugriff
        MinigameServerManager.schedulePublish((IntegratedServer) server);
        LOGGER.info("📡 Server publishing scheduled");

        LOGGER.info("🎮 ========================================");
        LOGGER.info("🎮 Minigame Server Ready!");
        LOGGER.info("🎮 ========================================");
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void onStopServer(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;

        if (server instanceof IMinecraftServer minigameServer && minigameServer.isMinigameServer()) {
            LOGGER.info("🔄 Minigame server stopping - cleanup...");

            // Cleanup
            MinigamesController.cleanup();
            MinigameServerManager.cleanup();
            MinigameWorldLoader.endMinigame();

            LOGGER.info("✅ Cleanup completed");
        }
    }
    /**
     * Tick the MinigameServerManager to check if we can publish.
     */
    @Inject(
        method = "tickServer",
        at = @At("HEAD")
    )
    private void onTickServer(CallbackInfo ci) {
        // Tick the publisher to check if minecraft.player is ready
        MinigameServerManager.tickPublisher();
    }
}
