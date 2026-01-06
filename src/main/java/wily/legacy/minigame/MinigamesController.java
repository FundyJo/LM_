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
    private static final Map<Level, FactoryConfig<MinigamesController>> LEVEL_CONFIGS = new HashMap<>();

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
        Legacy4J.LOGGER.info("🔄 MinigamesController cleaned up");
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

        this.config = FactoryConfig. create(
                levelKey,
                null,
                this,
                Bearer.of(this),
                new MinigameConfigControl(),
                value -> {
                    if (value != null && value != this) {
                        this.activeMinigame = value. activeMinigame;
                        this.minigameController = value. minigameController;
                        if (this.minigameController != null) {
                            this.minigameController.controller = this;
                        }
                    }
                },
                STORAGE // Nutze den globalen Storage
        );

        // Registriere im globalen Storage
        STORAGE.register(this.config);

        // Merke dir die Config für dieses Level
        LEVEL_CONFIGS.put(level, this.config);

        Legacy4J.LOGGER. debug("✅ Config created for level: {}", levelKey);
    }

    private static String getLevelId(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.dimension().location().toString().replace(":", "_").replace("/", "_");
        }
        return "unknown";
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

        FactoryConfig<MinigamesController> config = LEVEL_CONFIGS.get(level);

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

        // Nur Config initialisieren wenn wir in einem Minigame-Server sind
        MinecraftServer server = level.getServer();
        if (server instanceof IMinecraftServer minigameServer && minigameServer.isMinigameServer()) {
            controller.initConfig(level);
        }

        return controller;
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