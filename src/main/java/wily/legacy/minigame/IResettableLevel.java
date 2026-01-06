package wily.legacy.minigame;

/**
 * Interface für ServerLevel mit Minigame-spezifischen Methoden
 */
public interface IResettableLevel {

    /**
     * Setzt alle Chunk-Änderungen im RAM zurück.
     * Die Chunks werden zum Neuladen von der Festplatte markiert.
     *
     * WICHTIG: Alle Spieler sollten vor dem Aufruf aus dem Level teleportiert werden!
     *
     * @return true wenn erfolgreich
     */
    boolean legacy$resetAllChunks();

    /**
     * Entlädt alle Chunks aus dem RAM ohne sie zu speichern.
     * Die Chunks werden beim nächsten Zugriff frisch von der Festplatte geladen.
     */
    void legacy$unloadAllChunksWithoutSaving();

    /**
     * Prüft ob dieses Level zu einem Minigame-Server gehört
     */
    boolean legacy$isMinigameLevel();

    /**
     * Setzt das Level zurück, indem alle Chunks ohne Speichern entladen werden.
     */
    void legacy$reset();
}
