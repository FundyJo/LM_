package wily.legacy.minigame;

import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;

/**
 * Interface für Minecraft Client mit Minigame-Unterstützung.
 *
 * Wird via Mixin in Minecraft implementiert.
 * Ermöglicht das Laden von Minigame-Welten mit spezieller Konfiguration.
 *
 * @see MinigameWorldLoader für das Laden von Minigame-Lobbies
 */
public interface IMinecraft {

    /**
     * Lädt eine Minigame-Welt (Alternative zu doWorldLoad für Minigames).
     */
    void doMinigameLoad(
        LevelStorageSource.LevelStorageAccess levelStorageAccess,
        PackRepository packRepository,
        WorldStem worldStem,
        boolean newWorld
    );
}
