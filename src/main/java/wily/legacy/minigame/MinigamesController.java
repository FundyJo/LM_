package wily.legacy. minigame;

import com.mojang.serialization. Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft. server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net. minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigControl;
import wily.factoryapi.base.config.FactoryConfigDisplay;
import wily.factoryapi.base.network.CommonConfigSyncPayload;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// attached to dimensions
public class MinigamesController {
    public static final Codec<MinigamesController> CODEC = CompoundTag.CODEC. xmap(a -> {
        MinigamesController minigamesController = new MinigamesController();
        minigamesController.readNbt(a);
        return minigamesController;
    }, a -> {
        CompoundTag compoundTag = new CompoundTag();
        a.writeNbt(compoundTag);
        return compoundTag;
    });
    public static final StreamCodec<ByteBuf, MinigamesController> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    // ===== GLOBALER STORAGE =====
    public static final FactoryConfig. StorageHandler STORAGE = new FactoryConfig.StorageHandler(true);
    private static final ResourceLocation STORAGE_LOCATION = Legacy4J.createModLocation("minigame_controller");

    // ===== PRO-LEVEL CONFIGS =====
    // Use dimension key (String) instead of Level object to support both client and server levels
    private static final Map<String, FactoryConfig<MinigamesController>> LEVEL_CONFIGS = new HashMap<>();
    
    // Client-side flag indicating connection to a minigame server (set via sync from server)
    private static boolean clientConnectedToMinigameServer = false;

    /**
     * Initialisiert das MinigamesController System.
     * Wird EINMAL in Legacy4J.init() aufgerufen.
     */
    public static void init() {
        // Registriere globalen Storage
        FactoryConfig.registerCommonStorage(STORAGE_LOCATION, STORAGE);

        // ✅ SEHR SICHTBARES LOGGING
        System.out.println("===========================================");
        System.out.println("🎮 MinigamesController.init() CALLED!");
        System.out.println("🎮 Storage Location: " + STORAGE_LOCATION);
        System.out.println("🎮 Is Client: " + FactoryAPI.isClient());
        System.out.println("===========================================");

        Legacy4J. LOGGER.info("✅ MinigamesController system initialized on {}",
                FactoryAPI.isClient() ? "CLIENT" : "SERVER");
    }

    /**
     * Konfiguriert den Storage mit Server-File.
     * Wird vom IntegratedServerMixin beim Server-Start aufgerufen.
     */
    public static void configureServerFile(MinecraftServer server) {
        STORAGE.withServerFile(server, "minigame_controller. json");
        STORAGE.load(); // Lade existierende Daten
        Legacy4J. LOGGER.info("✅ MinigamesController storage configured and loaded");
    }

    /**
     * Cleanup - wird beim Server-Stop aufgerufen
     */
    public static void cleanup() {
        STORAGE.save();
        LEVEL_CONFIGS.clear();
        clientConnectedToMinigameServer = false;
        Legacy4J.LOGGER.info("🔄 MinigamesController cleaned up");
    }
    
    /**
     * Markiert den Client als mit einem Minigame-Server verbunden.
     * Wird aufgerufen wenn der Client Minigame-Configs vom Server empfängt.
     */
    public static void setClientConnectedToMinigameServer(boolean connected) {
        clientConnectedToMinigameServer = connected;
        if (connected) {
            Legacy4J.LOGGER.info("🎮 Client marked as connected to minigame server");
        }
    }
    
    /**
     * Prüft ob der Client als mit einem Minigame-Server verbunden markiert ist.
     */
    public static boolean isClientMarkedAsMinigameConnected() {
        return clientConnectedToMinigameServer;
    }
    
    /**
     * Initialisiert den Client-Config für ein Level.
     * Wird aufgerufen wenn der Client einem Server beitritt, um sicherzustellen
     * dass Sync-Pakete empfangen werden können.
     */
    public static void initClientConfigForLevel(Level level) {
        if (level == null) {
            Legacy4J.LOGGER.debug("🎮 initClientConfigForLevel called with null level");
            return;
        }
        if (!level.isClientSide()) {
            Legacy4J.LOGGER.debug("🎮 initClientConfigForLevel called on server-side level, skipping");
            return;
        }
        
        String levelKey = "level_" + getLevelId(level);
        
        // Nur initialisieren wenn noch nicht vorhanden
        if (LEVEL_CONFIGS.containsKey(levelKey)) {
            Legacy4J.LOGGER.debug("🎮 Config already exists for level: {}", levelKey);
            return;
        }
        
        // Erstelle Controller und initialisiere Config
        // Note: initConfig() stores the controller in LEVEL_CONFIGS
        MinigamesController controller = new MinigamesController();
        controller.level = level;
        controller.initConfig(level);
        
        // Verify the config was stored
        if (LEVEL_CONFIGS.containsKey(levelKey)) {
            Legacy4J.LOGGER.info("🎮 Client config initialized for level: {}", levelKey);
        } else {
            Legacy4J.LOGGER.warn("⚠️ Failed to initialize client config for level: {}", levelKey);
        }
    }

    // ===== INSTANCE =====
    private Minigame<? > activeMinigame = Minigame.NONE;
    private AbstractMinigameController minigameController = Minigame.NONE.newController(this);
    private FactoryConfig<MinigamesController> config;
    private Level level;

    public MinigamesController() {

    }

    private void initConfig(Level level) {
        if (this.config != null || level == null) {
            return;
        }

        String levelKey = "level_" + getLevelId(level);
        
        // Check if config already exists for this dimension (e.g., client synced from server)
        FactoryConfig<MinigamesController> existingConfig = LEVEL_CONFIGS.get(levelKey);
        if (existingConfig != null) {
            this.config = existingConfig;
            // Update this instance with the existing config's values
            MinigamesController existingController = existingConfig.get();
            if (existingController != null && existingController != this) {
                this.activeMinigame = existingController.activeMinigame;
                this.minigameController = existingController.minigameController;
                if (this.minigameController != null) {
                    this.minigameController.controller = this;
                }
            }
            Legacy4J.LOGGER.debug("✅ Reusing existing config for level: {}", levelKey);
            return;
        }

        this.config = FactoryConfig.create(
                levelKey,
                null,
                this,
                Bearer.of(this),
                new MinigameConfigControl(),
                value -> {
                    if (value != null && value != this) {
                        this.activeMinigame = value.activeMinigame != null ? value.activeMinigame : Minigame.NONE;
                        this.minigameController = value.minigameController;
                        if (this.minigameController != null) {
                            this.minigameController.controller = this;
                        }
                        // Mark client as connected to minigame server when receiving sync
                        if (level.isClientSide()) {
                            setClientConnectedToMinigameServer(true);
                            Legacy4J.LOGGER.info("🎮 Client received minigame sync for level: {} - Active: {}", 
                                levelKey, this.activeMinigame != null ? this.activeMinigame.getName() : "None");
                        }
                    }
                },
                STORAGE // Nutze den globalen Storage
        );

        // Registriere im globalen Storage
        STORAGE.register(this.config);

        // Merke dir die Config mit dimension key
        LEVEL_CONFIGS.put(levelKey, this.config);

        Legacy4J.LOGGER.debug("✅ Config created for level: {}", levelKey);
    }

    private static String getLevelId(Level level) {
        return level.dimension().location().toString().replace(":", "_").replace("/", "_");
    }

    public void writeNbt(CompoundTag tag) {
        tag.putInt("activeMinigame", activeMinigame.getId());
        CompoundTag compoundTag = new CompoundTag();
        minigameController.writeNbt(compoundTag);
        tag.put("minigameController", compoundTag);
    }

    public void readNbt(CompoundTag tag) {
        this.activeMinigame = Minigame.fromId(tag.getInt("activeMinigame").orElse(0));
        CompoundTag compoundTag = tag.getCompound("minigameController").orElseThrow();
        minigameController = this.activeMinigame.newController(this);
        minigameController. readNbt(compoundTag);
    }

    @Nullable
    public <T extends AbstractMinigameController<T>> T getController(Minigame<T> minigame) {
        if (activeMinigame != minigame) return null;
        return (T) minigameController;
    }

    public Minigame<?> getActiveMinigame() {
        return activeMinigame;
    }

    public boolean isBattle() {
        return getActiveMinigame() == Minigame.BATTLE;
    }

    public boolean isLobby() {
        return getActiveMinigame() == Minigame.LOBBY;
    }

    public boolean isActive() {
        Minigame<?> activeMinigame = getActiveMinigame();
        return activeMinigame != null && activeMinigame != Minigame.NONE;
    }

    public static MinigamesController getMinigameController(Level level) {
        if (level == null) {
            Legacy4J.LOGGER.error("Level is null in getMinigameController!");
            return new MinigamesController();
        }

        String levelKey = "level_" + getLevelId(level);
        FactoryConfig<MinigamesController> config = LEVEL_CONFIGS.get(levelKey);

        if (config != null) {
            MinigamesController controller = config.get();
            if (controller != null) {
                controller.level = level;
                return controller;
            }
        }

        // Erstelle neuen Controller
        MinigamesController controller = new MinigamesController();
        controller.level = level;

        // Config initialisieren wenn wir in einem Minigame-Server sind
        MinecraftServer server = level.getServer();
        if (server instanceof IMinecraftServer minigameServer && minigameServer.isMinigameServer()) {
            controller.initConfig(level);
        } else if (level.isClientSide() && isClientConnectedToMinigameServer()) {
            // Auch auf Client-Seite initialisieren wenn verbunden mit Minigame-Server
            controller.initConfig(level);
        }

        return controller;
    }

    /**
     * Prüft ob der Client mit einem Minigame-Server verbunden ist.
     * Prüft sowohl den IntegratedServer (für Host) als auch den client-seitigen Flag
     * (für Remote-Clients die Sync-Pakete vom Server empfangen haben).
     */
    private static boolean isClientConnectedToMinigameServer() {
        if (!FactoryAPI.isClient()) {
            return false;
        }
        // Check both: integrated server (for host) and client flag (for remote LAN clients)
        return clientConnectedToMinigameServer || isClientConnectedToMinigameServerInternal();
    }

    /**
     * Interne Methode - nur auf Client-Seite aufrufen!
     * Prüft ob der IntegratedServer ein Minigame-Server ist.
     */
    private static boolean isClientConnectedToMinigameServerInternal() {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        MinecraftServer server = minecraft.getSingleplayerServer();
        return server instanceof IMinecraftServer minigameServer && minigameServer.isMinigameServer();
    }

    public void dirty() {
        if (level == null) {
            Legacy4J.LOGGER.warn("No level when calling dirty()!");
            return;
        }

        if (config != null) {
            config.save(); // Speichert den globalen STORAGE
            config.sync(); // Synchronisiert zu allen Clients

            Legacy4J.LOGGER.debug("💾 Saved and synced for level: {}", getLevelId(level));
        }
    }

    /**
     * Synchronisiert den Controller-Zustand zu einem spezifischen Spieler.
     * Wird beim Player-Join aufgerufen.
     */
    public void syncToPlayer(ServerPlayer player) {
        if (config == null) {
            Legacy4J.LOGGER.debug("No config to sync for level: {}", getLevelId(level));
            return;
        }

        // Sende nur diesen Config-Eintrag zum Spieler
        CommonNetwork.sendToPlayer(player,
                CommonConfigSyncPayload.of(
                        CommonConfigSyncPayload.ID_S2C,
                        STORAGE,
                        config
                )
        );

        Legacy4J.LOGGER.debug("🔄 Synced to player {} for level: {}",
                player.getName().getString(), getLevelId(level));
    }

    public <T extends AbstractMinigameController<T>> T setActiveMinigame(Minigame<T> minigame) {
        this.activeMinigame = minigame;
        this.minigameController = minigame.newController(this);
        dirty();
        return (T) minigameController;
    }

    public AbstractMinigameController<?> getActiveController() {
        return minigameController;
    }

    public ServerLevel getLevel() {
        return (ServerLevel) level;
    }

    public ServerPlayer[] getPlayersFor(ServerLevel level) {
        ArrayList<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level) players.add(player);
        }
        return players.toArray(ServerPlayer[]::new);
    }

    public void playerLoadedIn(ServerPlayer player) {
        minigameController.playerLoadedIn(player);
    }

    public boolean hideNearbyPlayers() {
        return minigameController.hideNearbyPlayers();
    }

    public boolean pvpEnabled() {
        return minigameController.pvpEnabled();
    }

    public MinigameData getMinigameData() {
        return minigameController.getMinigameData();
    }

    public boolean canAcceptNewPlayers() {
        return minigameController.canAcceptNewPlayers();
    }

    public boolean isSmallInventory() {
        return minigameController.isSmallInventory();
    }

    public boolean hasPlayers() {
        return getPlayersFor(getLevel()).length > 0;
    }

    public void playerReady(ServerPlayer player, boolean ready) {
        minigameController.playerReady(player, ready);
    }

    public void playerVoted(ServerPlayer player, ResourceLocation resourceLocation) {
        minigameController. playerVoted(player, resourceLocation);
    }

    public boolean isClient() {
        return level. isClientSide();
    }

    // Custom ConfigControl for MinigamesController
    private static class MinigameConfigControl implements FactoryConfigControl<MinigamesController> {
        @Override
        public Codec<MinigamesController> codec() {
            return MinigamesController. CODEC;
        }
    }
}