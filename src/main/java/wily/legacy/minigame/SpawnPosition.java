package wily.legacy.minigame;

import java.util.HashMap;
import java.util.Map;

/**
 * Repräsentiert eine Spawn-Position für Spieler in einem Minigame
 */
public record SpawnPosition(double x, double y, double z, float yaw, float pitch) {

    /**
     * Standard-Spawn (0, 64, 0)
     */
    public static final SpawnPosition DEFAULT = new SpawnPosition(0.5, 64.0, 0.5, 0.0f, 0.0f);

    /**
     * Erstellt eine SpawnPosition nur mit Koordinaten (yaw/pitch = 0)
     */
    public static SpawnPosition of(double x, double y, double z) {
        return new SpawnPosition(x, y, z, 0.0f, 0.0f);
    }

    /**
     * Erstellt eine SpawnPosition mit Koordinaten und Rotation
     */
    public static SpawnPosition of(double x, double y, double z, float yaw, float pitch) {
        return new SpawnPosition(x, y, z, yaw, pitch);
    }

    /**
     * Erstellt eine SpawnPosition nur mit Koordinaten und Yaw (pitch = 0)
     */
    public static SpawnPosition of(double x, double y, double z, float yaw) {
        return new SpawnPosition(x, y, z, yaw, 0.0f);
    }

    /**
     * Builder für einfache Spawn-Konfiguration.
     *
     * Beispiel:
     * <pre>
     * SpawnPositions.builder()
     *     .universal(-315.5, 64.0, -340.5, 90.0f)  // Alle Spieler spawnen hier
     *     .build();
     *
     * SpawnPositions.builder()
     *     .player(0, -315.5, 64.0, -340.5, 90.0f)  // Spieler 1
     *     .player(1, -315.5, 64.0, -338.5, 90.0f)  // Spieler 2
     *     .fallback(-315.5, 64.0, -336.5, 90.0f)   // Alle anderen
     *     .build();
     * </pre>
     */
    public static class SpawnPositions {
        private SpawnPosition universalSpawn = null;
        private SpawnPosition fallbackSpawn = DEFAULT;
        private final Map<Integer, SpawnPosition> playerSpawns = new HashMap<>();

        private SpawnPositions() {}

        public static SpawnPositions builder() {
            return new SpawnPositions();
        }

        /**
         * Setzt eine universelle Spawn-Position für ALLE Spieler
         */
        public SpawnPositions universal(double x, double y, double z, float yaw) {
            this.universalSpawn = SpawnPosition.of(x, y, z, yaw);
            return this;
        }

        public SpawnPositions universal(double x, double y, double z, float yaw, float pitch) {
            this.universalSpawn = SpawnPosition.of(x, y, z, yaw, pitch);
            return this;
        }

        public SpawnPositions universal(double x, double y, double z) {
            this.universalSpawn = SpawnPosition.of(x, y, z);
            return this;
        }

        public SpawnPositions universal(SpawnPosition pos) {
            this.universalSpawn = pos;
            return this;
        }

        /**
         * Setzt Spawn-Position für einen bestimmten Spieler (0 = Spieler 1, 1 = Spieler 2, usw.)
         */
        public SpawnPositions player(int index, double x, double y, double z, float yaw) {
            playerSpawns.put(index, SpawnPosition.of(x, y, z, yaw));
            return this;
        }

        public SpawnPositions player(int index, double x, double y, double z, float yaw, float pitch) {
            playerSpawns.put(index, SpawnPosition.of(x, y, z, yaw, pitch));
            return this;
        }

        public SpawnPositions player(int index, double x, double y, double z) {
            playerSpawns.put(index, SpawnPosition.of(x, y, z));
            return this;
        }

        public SpawnPositions player(int index, SpawnPosition pos) {
            playerSpawns.put(index, pos);
            return this;
        }

        /**
         * Setzt Fallback-Position für Spieler ohne spezifische Position
         */
        public SpawnPositions fallback(double x, double y, double z, float yaw) {
            this.fallbackSpawn = SpawnPosition.of(x, y, z, yaw);
            return this;
        }

        public SpawnPositions fallback(double x, double y, double z, float yaw, float pitch) {
            this.fallbackSpawn = SpawnPosition.of(x, y, z, yaw, pitch);
            return this;
        }

        public SpawnPositions fallback(SpawnPosition pos) {
            this.fallbackSpawn = pos;
            return this;
        }

        /**
         * Gibt die Spawn-Position für den angegebenen Spieler-Index zurück
         */
        public SpawnPosition get(int playerIndex) {
            // Universal hat höchste Priorität
            if (universalSpawn != null) {
                return universalSpawn;
            }
            // Spezifische Spieler-Position
            if (playerSpawns.containsKey(playerIndex)) {
                return playerSpawns.get(playerIndex);
            }
            // Fallback
            return fallbackSpawn;
        }

        public SpawnPositions build() {
            return this;
        }
    }
}


