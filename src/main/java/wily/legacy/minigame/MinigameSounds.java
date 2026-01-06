package wily.legacy.minigame;

import net.minecraft.sounds.SoundEvent;
import wily.legacy.Legacy4J;

/**
 * Sound-Events für das Minigame-System.
 * Diese werden NICHT im Registry registriert, um Server-Client-Sync-Probleme zu vermeiden.
 * Die sounds.json Datei definiert die Sounds, und sie werden direkt erstellt.
 */
public final class MinigameSounds {

    /**
     * Countdown-Sound für die letzten Sekunden in der Lobby
     */
    public static final SoundEvent LOBBY_COUNTDOWN = SoundEvent.createVariableRangeEvent(
        Legacy4J.createModLocation("minigame.lobby_countdown")
    );

    private MinigameSounds() {
        // Utility class
    }
}

