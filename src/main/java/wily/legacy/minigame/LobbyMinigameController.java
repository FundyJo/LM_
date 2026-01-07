package wily.legacy.minigame;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net. minecraft.server.level.ServerPlayer;
import net. minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block. Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wily.factoryapi. base.network.CommonNetwork;
import wily.legacy.network. S2CPlaySoundPayload;

import java.util.*;

/**
 * Controller für die Lobby - verwaltet Spieler-Wartebereich und Minigame-Start
 */
public class LobbyMinigameController extends AbstractMinigameController<LobbyMinigameController> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyMinigameController.class);
    private static final int COUNTDOWN_TICKS = 1200; // 60 sec (1200 ticks)

    private int waitingTicks = COUNTDOWN_TICKS;
    private final List<UUID> readiedPlayers = new ArrayList<>();
    private boolean minigameStarted = false; // Verhindert dass der Timer erneut startet

    // Standard Spawn für neue Spieler
    private static final SpawnPosition DEFAULT_SPAWN = SpawnPosition. of(-315.5, 66.0, -341.5, 90.0f);

    // Sieger-Spawn (Spieler die die letzte Runde gewonnen haben spawnen hier)
    public static final SpawnPosition WINNER_SPAWN = SpawnPosition.of(-356.5, 71.0, -340.5, -90.0f);

    // Random Spawns für Spieler die nach dem Standard-Spawn joinen
    private static final List<SpawnPosition> RANDOM_SPAWNS = List.of(
            SpawnPosition.of(-342.5, 59.0, -340.5, -90.0f),
            SpawnPosition.of(-346.5, 52.0, -356.5, -90.0f),
            SpawnPosition.of(-353.5, 54.0, -330.5, 0.0f),
            SpawnPosition.of(-316.5, 64.0, -332.5, 90.0f),
            SpawnPosition.of(-316.5, 64.0, -348.5, 90.0f),
            SpawnPosition.of(-343.5, 65.0, -364.5, -90.0f),
            SpawnPosition.of(-340.5, 64.0, -315.5, -90.0f),
            SpawnPosition.of(-302.5, 64.0, -321.5, 90.0f),
            SpawnPosition.of(-303.5, 64.0, -365.5, 45.0f),
            SpawnPosition.of(-311.5, 72.0, -351.5, 0.0f),
            SpawnPosition.of(-311.5, 72.0, -329.5, 180.0f),
            SpawnPosition.of(-315.5, 76.0, -340.5, 90.0f),
            SpawnPosition.of(-358.5, 57.0, -309.5, 180.0f),
            SpawnPosition.of(-358.5, 57.0, -371.5, 0.0f),
            SpawnPosition.of(-326.5, 59.0, -356.5, 0.0f),
            SpawnPosition.of(-357.5, 52.0, -358.5, -45.0f)
    );

    // Lobby Spawn Konfiguration
    private static final SpawnPosition. SpawnPositions LOBBY_SPAWNS = SpawnPosition.SpawnPositions.builder()
            .player(0, DEFAULT_SPAWN) // Erster Spieler spawnt am Standard-Spawn
            .fallback(DEFAULT_SPAWN)  // Fallback für weitere Spieler (wird überschrieben durch getRandomSpawn)
            .build();

    // Aktive Chunk-Bereiche für die Lobby (Chunk-Koordinaten, nicht Block-Koordinaten!)
    // Bereich:  Chunk (-27, -27) bis (-15, -18)
    private static final List<net.minecraft.world.phys.AABB> LOBBY_CHUNK_AREAS = List.of(
            new net.minecraft.world.phys.AABB(-27, 0, -27, -15, 0, -18)
    );

    // UsePermissions - Welche Items in der Lobby benutzt werden dürfen
    private static final MinigamePermissions USE_PERMISSIONS = MinigamePermissions.builder()
            .whitelist()
            .addItem(Items.BOW)
            .addItem(Items.ARROW)
            .addItem(Items.FISHING_ROD)
            .addItem(Items.SNOWBALL)
            .addItem(Items.ELYTRA)
            // Music Discs
            .addItem(Items. MUSIC_DISC_13)
            .addItem(Items.MUSIC_DISC_CAT)
            .addItem(Items.MUSIC_DISC_BLOCKS)
            .addItem(Items.MUSIC_DISC_CHIRP)
            .addItem(Items.MUSIC_DISC_FAR)
            .addItem(Items.MUSIC_DISC_MALL)
            .addItem(Items.MUSIC_DISC_MELLOHI)
            .addItem(Items.MUSIC_DISC_STAL)
            .addItem(Items.MUSIC_DISC_STRAD)
            .addItem(Items.MUSIC_DISC_WARD)
            .addItem(Items.MUSIC_DISC_11)
            .addItem(Items.MUSIC_DISC_WAIT)
            .build();

    // BlockUsePermissions - Welche Blöcke in der Lobby interagiert werden dürfen
    private static final MinigamePermissions BLOCK_USE_PERMISSIONS = MinigamePermissions.builder()
            .whitelist()
            // Türen
            .addBlock(Blocks.OAK_DOOR)
            .addBlock(Blocks. IRON_DOOR)
            .addBlock(Blocks.SPRUCE_DOOR)
            .addBlock(Blocks.BIRCH_DOOR)
            .addBlock(Blocks.JUNGLE_DOOR)
            .addBlock(Blocks.ACACIA_DOOR)
            .addBlock(Blocks.DARK_OAK_DOOR)
            .addBlock(Blocks.MANGROVE_DOOR)
            .addBlock(Blocks. CHERRY_DOOR)
            .addBlock(Blocks.BAMBOO_DOOR)
            .addBlock(Blocks.CRIMSON_DOOR)
            .addBlock(Blocks.WARPED_DOOR)
            // Falltüren
            .addBlock(Blocks.OAK_TRAPDOOR)
            .addBlock(Blocks.IRON_TRAPDOOR)
            .addBlock(Blocks.SPRUCE_TRAPDOOR)
            .addBlock(Blocks.BIRCH_TRAPDOOR)
            .addBlock(Blocks.JUNGLE_TRAPDOOR)
            .addBlock(Blocks. ACACIA_TRAPDOOR)
            .addBlock(Blocks.DARK_OAK_TRAPDOOR)
            // Buttons
            .addBlock(Blocks.STONE_BUTTON)
            .addBlock(Blocks.OAK_BUTTON)
            .addBlock(Blocks. SPRUCE_BUTTON)
            .addBlock(Blocks.BIRCH_BUTTON)
            .addBlock(Blocks.JUNGLE_BUTTON)
            .addBlock(Blocks.ACACIA_BUTTON)
            .addBlock(Blocks.DARK_OAK_BUTTON)
            // Hebel
            .addBlock(Blocks.LEVER)
            // Truhen
            .addBlock(Blocks.CHEST)
            .addBlock(Blocks. ENDER_CHEST)
            .addBlock(Blocks.TRAPPED_CHEST)
            // Musik
            .addBlock(Blocks.NOTE_BLOCK)
            .addBlock(Blocks.JUKEBOX)
            // Fence Gates
            .addBlock(Blocks.OAK_FENCE_GATE)
            .addBlock(Blocks.SPRUCE_FENCE_GATE)
            .addBlock(Blocks.BIRCH_FENCE_GATE)
            .addBlock(Blocks. JUNGLE_FENCE_GATE)
            .addBlock(Blocks.ACACIA_FENCE_GATE)
            .addBlock(Blocks.DARK_OAK_FENCE_GATE)
            .build();

    public LobbyMinigameController(MinigamesController controller) {
        super(controller);
    }

    @Override
    public void writeNbt(CompoundTag tag) {
        //super.writeNbt(tag);
        //tag.putInt("waitingTicks", waitingTicks);
        //tag.putBoolean("minigameStarted", minigameStarted);
        //// Save readied players
        //CompoundTag readiedTag = new CompoundTag();
        //for (int i = 0; i < readiedPlayers.size(); i++) {
        //    readiedTag.putUUID("player_" + i, readiedPlayers.get(i));
        //}
        //readiedTag.putInt("count", readiedPlayers.size());
        //tag.put("readiedPlayers", readiedTag);
    }

    @Override
    public void readNbt(CompoundTag tag) {
        //super.readNbt(tag);
        //waitingTicks = tag.getInt("waitingTicks");
        //minigameStarted = tag.getBoolean("minigameStarted");
        //// Load readied players
        //readiedPlayers.clear();
        //if (tag.contains("readiedPlayers")) {
        //    CompoundTag readiedTag = tag.getCompound("readiedPlayers");
        //    int count = readiedTag.getInt("count");
        //    for (int i = 0; i < count; i++) {
        //        if (readiedTag.hasUUID("player_" + i)) {
        //            readiedPlayers.add(readiedTag. getUUID("player_" + i));
        //        }
        //    }
        //}
    }

    @Override
    public Minigame<LobbyMinigameController> getMinigame() {
        return Minigame. LOBBY;
    }

    @Override
    public SpawnPosition.SpawnPositions getSpawnPositions() {
        return LOBBY_SPAWNS;
    }

    /**
     * Gibt die Spawn-Position für einen Spieler zurück.
     * - Spieler 0: Standard-Spawn
     * - Alle anderen:  Zufälliger Spawn aus RANDOM_SPAWNS
     */
    @Override
    public SpawnPosition getSpawnPosition(int playerIndex) {
        if (playerIndex == 0) {
            return DEFAULT_SPAWN;
        }
        // Zufälliger Spawn für alle anderen Spieler
        return getRandomSpawn();
    }

    /**
     * Gibt einen zufälligen Spawn aus der RANDOM_SPAWNS Liste zurück
     */
    public static SpawnPosition getRandomSpawn() {
        int index = new Random().nextInt(RANDOM_SPAWNS.size());
        return RANDOM_SPAWNS. get(index);
    }

    /**
     * Gibt den Sieger-Spawn zurück (für Spieler die die letzte Runde gewonnen haben)
     */
    public static SpawnPosition getWinnerSpawn() {
        return WINNER_SPAWN;
    }

    @Override
    public List<net.minecraft.world.phys.AABB> getActiveChunkAreas() {
        return LOBBY_CHUNK_AREAS;
    }

    @Override
    public boolean allowDamage() {
        return false; // Kein Schaden in der Lobby
    }

    @Override
    public boolean allowHunger() {
        return false; // Kein Hunger in der Lobby
    }

    @Override
    public MinigamePermissions getUsePermissions() {
        return USE_PERMISSIONS;
    }

    @Override
    public MinigamePermissions getBlockUsePermissions() {
        return BLOCK_USE_PERMISSIONS;
    }

    @Override
    public MinigamePermissions getDestroyPermissions() {
        return MinigamePermissions.DENY_ALL; // Keine Block-Zerstörung in der Lobby
    }

    @Override
    public MinigamePermissions getPlacePermissions() {
        return MinigamePermissions.DENY_ALL; // Keine Block-Platzierung in der Lobby
    }

    @Override
    public MinigameGamerules getGamerules() {
        return MinigameGamerules.LOBBY;
    }

    @Override
    public void sendToMap(RegistryAccess access, ServerPlayer player, boolean late) {
        // Inventar leeren
        player.getInventory().clearContent();

        // Hole Spieler-Index basierend auf aktuellen Spielern
        ServerLevel level = controller.getLevel();
        if (level == null) return;

        ServerPlayer[] players = controller.getPlayersFor(level);
        int playerIndex = 0;
        for (int i = 0; i < players.length; i++) {
            if (players[i]. getUUID().equals(player.getUUID())) {
                playerIndex = i;
                break;
            }
        }

        // Hole Spawn-Position für diesen Spieler
        SpawnPosition spawn = getSpawnPosition(playerIndex);

        // Teleportiere zur Lobby-Spawn
        player.teleportTo(level, spawn. x(), spawn.y(), spawn.z(), Set.of(), spawn.yaw(), spawn.pitch(), true);
        LOGGER.info("✅ Teleported player {} to lobby spawn #{}:  {} {} {}",
                player.getName().getString(), playerIndex + 1, spawn.x(), spawn.y(), spawn.z());
    }


    @Override
    public void tick() {
        ServerLevel level = controller.getLevel();
        if (level == null) return;

        MinecraftServer server = level.getServer();
        if (!(server instanceof IMinecraftServer minigameServer)) return;

        MinigameServerConfig config = minigameServer.getMinigameServerConfig();
        if (config == null) return;

        // Wenn ein Minigame bereits gestartet wurde, nicht ticken
        if (minigameStarted) {
            return;
        }

        ServerPlayer[] players = controller.getPlayersFor(level);
        int playerCount = players.length;
        int minPlayers = config.getMinPlayersToStart();

        // Solo Mode: Nur 1 Spieler nötig (z.B. Glide Solo)
        boolean isSoloMode = minPlayers == 1;

        // Prüfe ob genug Spieler da sind
        boolean enoughPlayers = (isSoloMode && playerCount > 0) || (playerCount >= minPlayers);

        if (enoughPlayers) {
            // Zeige Countdown-Nachricht jede Sekunde
            if (waitingTicks % 20 == 0) {
                int secondsLeft = waitingTicks / 20;
                for (ServerPlayer player : players) {
                    sendTopMessage(player, Component.literal("§fTime to start: " + secondsLeft + " seconds"));
                }

                // Spiele Sound in den letzten 5 Sekunden (5, 4, 3, 2, 1)
                if (secondsLeft <= 5 && secondsLeft >= 1) {
                    for (ServerPlayer player :  players) {
                        CommonNetwork.sendToPlayer(player, S2CPlaySoundPayload. of(
                                MinigameSounds.LOBBY_COUNTDOWN,
                                1.0f
                        ));
                    }
                }
            }

            // Wenn alle bereit sind, überspringe zum Start (setze ticks auf -9999)
            if (allPlayersReady() && ! isSoloMode) {
                LOGGER.info("✅ All players ready - skipping countdown");
                waitingTicks = -9999;
            }

            // Countdown läuft runter
            if (waitingTicks <= 0) {
                startMinigame(config, players);
                waitingTicks = COUNTDOWN_TICKS; // Reset für nächste Runde
            }

            waitingTicks--;

        } else if (! isSoloMode && playerCount == 1) {
            // Nur 1 Spieler aber kein Solo-Mode - warte auf mehr Spieler
            waitingTicks = COUNTDOWN_TICKS;
            for (ServerPlayer player : players) {
                sendTopMessage(player, Component.literal("§f1 or more additional players are required to start the round... "));
            }
        } else {
            // Nicht genug Spieler - reset countdown
            if (waitingTicks < COUNTDOWN_TICKS) {
                waitingTicks = COUNTDOWN_TICKS;
                LOGGER.info("⏸️ Lobby countdown reset - not enough players");
            }
        }

        // Wenn keine Spieler mehr da sind, könnte die Lobby entladen werden
        // (wird vom Server automatisch gehandhabt wenn alle Spieler weg sind)
        if (waitingTicks <= 0 && playerCount <= 0) {
            LOGGER.info("📦 Lobby is empty - ready for cleanup");
        }
    }

    private void startMinigame(MinigameServerConfig config, ServerPlayer[] players) {
        LOGGER.info("🚀 Starting minigame:  {}", config.getMinigameType());

        // Markiere dass ein Minigame gestartet wurde - verhindert weiteres Ticken des Timers
        minigameStarted = true;
        controller.dirty(); // Speichere Zustand

        MinecraftServer server = controller.getLevel().getServer();
        if (!(server instanceof IMinecraftServer)) return;

        // === Markiere Lobby für Reset ===
        // Die Lobby-Chunks werden automatisch entladen wenn keine Spieler mehr da sind
        // Da Speichern deaktiviert ist, werden sie beim nächsten Laden frisch geladen
        MinigameWorldLoader. markLobbyForReset();
        LOGGER.info("🔄 Lobby marked for reset - will be fresh when players return");

        // === Lade Battle-Welt ===
        ServerLevel arena;

        // Versuche zuerst die bereits geladene Battle-Dimension zu holen
        var battleKey = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacy", "battle")
        );
        arena = server.getLevel(battleKey);

        // Wenn nicht geladen, lade sie
        if (arena == null) {
            LOGGER.info("🔄 Battle dimension not loaded, loading now...");
            var loadedArena = ExternalWorldLoader.loadWorld(server, "Battle", null);
            if (loadedArena. isPresent()) {
                arena = loadedArena.get();
            }
        }

        if (arena == null) {
            LOGGER. error("❌ Battle dimension not available!");
            broadcastMessage(Component.literal("§cFehler: Battle-Arena konnte nicht geladen werden!"));
            return;
        }

        LOGGER.info("✅ Battle arena ready - teleporting {} players now", players.length);

        // === Aktiviere BattleMinigameController für die Arena ===
        MinigamesController arenaController = MinigamesController.getMinigameController(arena);
        if (arenaController == null) {
            LOGGER. error("❌ No MinigamesController for arena!");
            return;
        }

        // Setze Battle als aktives Minigame
        arenaController.setActiveMinigame(Minigame.BATTLE);
        BattleMinigameController battleController = arenaController.getController(Minigame.BATTLE);

        if (battleController == null) {
            LOGGER.error("❌ BattleMinigameController not available!");
            return;
        }

        // Teleportiere alle Spieler zur Battle-Arena
        for (int i = 0; i < players.length; i++) {
            ServerPlayer player = players[i];

            // Hole Spawn-Position vom BattleController
            SpawnPosition spawn = battleController.getSpawnPosition(i);

            // Teleportiere zum Arena-Spawn
            player.teleportTo(arena, spawn.x(), spawn.y(), spawn.z(), Set.of(), spawn.yaw(), spawn.pitch(), true);

            // Registriere Spieler beim Battle-Controller
            battleController.sendToMap(server. registryAccess(), player, false);

            // Sync controller state to player after teleport (so client has correct state for new level)
            arenaController.syncToPlayer(player);

            // Setze Spielmodus auf Adventure
            player.setGameMode(GameType.ADVENTURE);

            // Leere Inventar (Items werden vom Battle-Controller vergeben)
            player.getInventory().clearContent();

            // Heile Spieler
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0f);

            player.sendSystemMessage(Component.literal("§a=== " + config.getMinigameType() + " BEGINNT!  ==="));

            LOGGER.info("✅ Teleported {} to Battle arena at {}, {}, {}",
                    player.getName().getString(), spawn.x(), spawn.y(), spawn.z());
        }

        // Starte das Battle-Spiel
        battleController.startGame();

        LOGGER.info("✅ Minigame started successfully with {} players", players.length);
    }

    private void broadcastMessage(Component message) {
        for (ServerPlayer player : controller.getPlayersFor(controller.getLevel())) {
            player.sendSystemMessage(message);
        }
    }

    private boolean allPlayersReady() {
        ServerPlayer[] players = controller.getPlayersFor(controller.getLevel());
        if (players.length == 0) return false;

        return Arrays.stream(players)
                .map(Entity::getUUID)
                .allMatch(readiedPlayers::contains);
    }

    @Override
    public void playerReady(ServerPlayer player, boolean ready) {
        if (ready) {
            if (readiedPlayers.contains(player.getUUID())) return;
            readiedPlayers.add(player.getUUID());
            broadcastMessage(Component.literal("§a" + player.getName().getString() + " ist bereit!"));
            LOGGER.info("✅ Player {} is ready", player.getName().getString());
        } else {
            readiedPlayers.remove(player. getUUID());
            broadcastMessage(Component.literal("§c" + player.getName().getString() + " ist nicht mehr bereit"));
            LOGGER.info("⏸️ Player {} is not ready", player.getName().getString());
        }
        controller.dirty(); // Speichere Änderungen
    }

    @Override
    public boolean hasBareHotbar() {
        return true; // Lobby zeigt bare hotbar (keine Herzen)
    }

    @Override
    public boolean canAcceptNewPlayers() {
        return true; // Lobby kann immer neue Spieler akzeptieren
    }

    @Override
    public boolean pvpEnabled() {
        return false; // Kein PvP in der Lobby
    }

    @Override
    public boolean isSmallInventory() {
        return true; // Kleines Inventar in der Lobby
    }

    @Override
    public boolean hideNearbyPlayers() {
        return false; // Spieler sollen sich in der Lobby sehen können
    }

    /**
     * Setzt den Lobby-Zustand zurück, wenn Spieler von einem Minigame zurückkehren.
     * Wird aufgerufen wenn das Battle/Minigame endet und Spieler zur Lobby teleportiert werden.
     */
    public void resetLobbyState() {
        LOGGER.info("🔄 Resetting lobby state for new round.. .");
        minigameStarted = false;
        waitingTicks = COUNTDOWN_TICKS;
        readiedPlayers.clear();
        controller.dirty(); // Speichere Reset
        LOGGER.info("✅ Lobby ready for new round!");
    }

    /**
     * Prüft ob ein Minigame bereits gestartet wurde
     */
    public boolean isMinigameStarted() {
        return minigameStarted;
    }
}