package wily.legacy.minigame;

import net.minecraft.core.RegistryAccess;
import net. minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys. AABB;
import wily.factoryapi.base. network.CommonNetwork;
import wily.legacy.network.S2CDisplayTextPayload;

import java.util. Collections;
import java.util.List;

/**
 * Abstrakte Basisklasse für Minigame-Controller
 */
public abstract class AbstractMinigameController<T extends AbstractMinigameController<T>> {

    protected MinigamesController controller;

    private MinigameData minigameData;

    public AbstractMinigameController(MinigamesController controller) {
        this.controller = controller;
    }

    /**
     * Schreibt den Zustand dieses Controllers in ein NBT-Tag.
     * Muss von Subklassen implementiert werden, um ihren spezifischen Zustand zu speichern.
     */
    public void writeNbt(CompoundTag tag) {
    }

    /**
     * Liest den Zustand dieses Controllers aus einem NBT-Tag.
     * Muss von Subklassen implementiert werden, um ihren spezifischen Zustand zu laden.
     */
    public void readNbt(CompoundTag tag) {
    }


    public abstract Minigame<T> getMinigame();

    public abstract void sendToMap(RegistryAccess access, ServerPlayer player, boolean late);

    public void sendToMap(RegistryAccess access, ServerPlayer player) {
        sendToMap(access, player, false);
    }


    /**
     * Gibt die Spawn-Konfiguration für diesen Controller zurück.
     *
     * Beispiele:
     * <pre>
     * // Universal spawn - alle Spieler spawnen am gleichen Ort
     * return SpawnPosition.SpawnPositions.builder()
     *     .universal(-315. 5, 64.0, -340.5, 90.0f)
     *     .build();
     *
     * // Spezifische Spawn-Positionen pro Spieler
     * return SpawnPosition.SpawnPositions. builder()
     *     .player(0, -315.5, 64.0, -340.5, 90.0f)  // Spieler 1
     *     .player(1, -315.5, 64.0, -338.5, 90.0f)  // Spieler 2
     *     .fallback(-315.5, 64.0, -336.5, 90.0f)   // Alle anderen
     *     .build();
     * </pre>
     */
    public abstract SpawnPosition. SpawnPositions getSpawnPositions();

    /**
     * Gibt die Spawn-Position für einen bestimmten Spieler-Index zurück.
     *
     * @param playerIndex Der Index des Spielers (0-basiert)
     * @return Die Spawn-Position für diesen Spieler
     */
    public SpawnPosition getSpawnPosition(int playerIndex) {
        SpawnPosition.SpawnPositions positions = getSpawnPositions();
        if (positions == null) {
            return SpawnPosition.DEFAULT;
        }
        return positions.get(playerIndex);
    }

    /**
     * Gibt die aktiven Chunk-Bereiche für dieses Minigame zurück.
     * Nur Chunks innerhalb dieser Bereiche werden geladen/aktiv gehalten.
     *
     * Die AABB verwendet Chunk-Koordinaten (nicht Block-Koordinaten! ):
     * - minX/maxX = Chunk X-Koordinaten
     * - minZ/maxZ = Chunk Z-Koordinaten
     * - minY/maxY werden ignoriert (auf 0 setzen)
     *
     * Beispiel für Chunks von (-27, -27) bis (-15, -18):
     * <pre>
     * return List.of(new AABB(-27, 0, -27, -15, 0, -18));
     * </pre>
     *
     * @return Liste von Chunk-Bereichen, oder leere Liste wenn alle Chunks erlaubt sind
     */
    public List<AABB> getActiveChunkAreas() {
        return Collections.emptyList(); // Default: alle Chunks erlaubt
    }

    /**
     * Prüft ob ein Chunk innerhalb der aktiven Bereiche liegt.
     *
     * @param chunkX Chunk X-Koordinate
     * @param chunkZ Chunk Z-Koordinate
     * @return true wenn der Chunk aktiv sein soll
     */
    public boolean isChunkActive(int chunkX, int chunkZ) {
        List<AABB> areas = getActiveChunkAreas();
        if (areas.isEmpty()) {
            return true; // Keine Einschränkung
        }

        for (AABB area : areas) {
            if (chunkX >= area.minX && chunkX <= area.maxX &&
                    chunkZ >= area.minZ && chunkZ <= area.maxZ) {
                return true;
            }
        }
        return false;
    }

    public void playerLoadedIn(ServerPlayer player) {

    }


    public void acceptMinigameData(MinigameData data) {
        this.minigameData = data;
    }

    public MinigameData getMinigameData() {
        return this.minigameData;
    }


    public void playerReady(ServerPlayer player, boolean ready) {

    }

    public void playerVoted(ServerPlayer player, net.minecraft.resources.ResourceLocation resourceLocation) {

    }

    public void tick() {

    }

    public boolean canAcceptNewPlayers() {
        return true;
    }

    public boolean pvpEnabled() {
        return false;
    }

    public boolean isSmallInventory() {
        return true;
    }

    public boolean hideNearbyPlayers() {
        return false;
    }

    /**
     * Ob Spieler Blöcke abbauen dürfen.
     * @return true wenn Block-Abbau erlaubt ist, false wenn nicht
     */
    public boolean allowBlockBreaking() {
        return false; // Default:  Keine Block-Zerstörung in Minigames
    }

    /**
     * Ob Spieler Blöcke platzieren dürfen.
     * @return true wenn Block-Platzierung erlaubt ist, false wenn nicht
     */
    public boolean allowBlockPlacing() {
        return false; // Default: Keine Block-Platzierung in Minigames
    }

    /**
     * Ob Spieler Schaden nehmen können.
     * @return true wenn Schaden erlaubt ist, false wenn nicht
     */
    public boolean allowDamage() {
        return true; // Default: Schaden ist erlaubt (außer in Lobby)
    }

    /**
     * Ob Spieler Hunger verlieren können.
     * @return true wenn Hunger-Verlust erlaubt ist, false wenn nicht
     */
    public boolean allowHunger() {
        return true; // Default: Hunger ist erlaubt (außer in Lobby)
    }

    /**
     * Berechtigungen für Item-Nutzung (UsePermissions).
     * @return Permission-Objekt das definiert welche Items benutzt werden dürfen
     */
    public MinigamePermissions getUsePermissions() {
        return MinigamePermissions.DENY_ALL; // Default: Keine Items nutzbar
    }

    /**
     * Berechtigungen für Block-Interaktion (BlockUsePermissions).
     * @return Permission-Objekt das definiert welche Blöcke interagiert werden dürfen
     */
    public MinigamePermissions getBlockUsePermissions() {
        return MinigamePermissions.DENY_ALL; // Default: Keine Block-Interaktion
    }

    /**
     * Berechtigungen für Block-Zerstörung (DestroyPermissions).
     * @return Permission-Objekt das definiert welche Blöcke zerstört werden dürfen
     */
    public MinigamePermissions getDestroyPermissions() {
        return MinigamePermissions.DENY_ALL; // Default: Keine Block-Zerstörung
    }

    /**
     * Berechtigungen für Block-Platzierung (PlacePermissions).
     * @return Permission-Objekt das definiert welche Blöcke platziert werden dürfen
     */
    public MinigamePermissions getPlacePermissions() {
        return MinigamePermissions.DENY_ALL; // Default: Keine Block-Platzierung
    }

    /**
     * Gibt die Gamerules für dieses Minigame zurück.
     * Wird automatisch angewendet wenn das Minigame startet.
     * @return MinigameGamerules oder null wenn keine speziellen Regeln
     */
    public MinigameGamerules getGamerules() {
        return null; // Default: Keine speziellen Gamerules
    }

    /**
     * Wendet die Gamerules auf das Level an.
     * Wird automatisch beim Start des Minigames aufgerufen.
     */
    public void applyGamerules() {
        MinigameGamerules gamerules = getGamerules();
        if (gamerules != null && controller. getLevel() != null) {
            gamerules.apply(controller.getLevel());
        }
    }

    public static final class NoneMinigameController extends AbstractMinigameController<NoneMinigameController> {
        public NoneMinigameController(MinigamesController controller) {
            super(controller);
        }

        @Override
        public void writeNbt(CompoundTag tag) {
            // NoneMinigameController hat keinen Zustand zum Speichern
        }

        @Override
        public void readNbt(CompoundTag tag) {
            // NoneMinigameController hat keinen Zustand zum Laden
        }

        @Override
        public Minigame<NoneMinigameController> getMinigame() {
            return Minigame.NONE;
        }

        @Override
        public void sendToMap(RegistryAccess access, ServerPlayer player, boolean late) {}

        @Override
        public SpawnPosition.SpawnPositions getSpawnPositions() {
            return SpawnPosition.SpawnPositions.builder()
                    .universal(SpawnPosition.DEFAULT)
                    .build();
        }

        @Override
        public boolean pvpEnabled() {
            return true;
        }

        @Override
        public boolean isSmallInventory() {
            return false;
        }

        @Override
        public boolean allowBlockBreaking() {
            return true; // Normale Welten erlauben Block-Abbau
        }

        @Override
        public boolean allowBlockPlacing() {
            return true; // Normale Welten erlauben Block-Platzierung
        }
    }

    public static void sendTopMessage(ServerPlayer player, Component message) {
        CommonNetwork.sendToPlayer(player, new S2CDisplayTextPayload(message));
    }

    /**
     * Ob das Minigame eine "bare" Hotbar nutzt (z.B. reduzierte HUD-Elemente / eigener Bar-Renderer).
     *
     * Default: false.
     *
     * Hinweis: Das ist absichtlich Controller-getrieben (nicht Minigame-getrieben),
     * damit einzelne Controller-Varianten das Verhalten unabhängig vom Minigame-Typ wählen können.
     */
    public boolean hasBareHotbar() {
        return false;
    }

    /**
     * Ob zusätzlich zur (ggf. reduzierten) Hotbar auch die Vanilla-Dekorationen gerendert werden sollen.
     * Default: true.
     */
    public boolean hasHotbarDecorations() {
        return true;
    }
}